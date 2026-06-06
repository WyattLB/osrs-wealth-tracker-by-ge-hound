package com.wealthtracker;

import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.ItemID;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
	name = "Wealth Tracker",
	description = "Track your total OSRS net worth over time",
	tags = {"wealth", "net worth", "bank", "tracker", "history", "money"}
)
public class WealthTrackerPlugin extends Plugin
{
	@Inject private Client client;
	@Inject private ClientThread clientThread;
	@Inject private ItemManager itemManager;
	@Inject private ClientToolbar clientToolbar;
	@Inject private OverlayManager overlayManager;
	@Inject private WealthTrackerConfig config;
	@Inject private WealthDataManager dataManager;
	@Inject private WealthTrackerOverlay overlay;

	private WealthTrackerPanel panel;
	private NavigationButton navButton;
	private ScheduledExecutorService executor;

	@Override
	protected void startUp() throws Exception
	{
		log.debug("WealthTracker: startUp()");

		executor = Executors.newSingleThreadScheduledExecutor(r ->
		{
			Thread t = new Thread(r, "WealthTracker-worker");
			t.setDaemon(true);
			return t;
		});

		panel = new WealthTrackerPanel(dataManager, config);

		BufferedImage icon = loadIcon();
		navButton = NavigationButton.builder()
			.tooltip("Wealth Tracker")
			.icon(icon)
			.priority(7)
			.panel(panel)
			.build();

		clientToolbar.addNavigation(navButton);
		overlayManager.add(overlay);

		log.debug("WealthTracker: navigation button and overlay registered");

		SwingUtilities.invokeLater(() ->
		{
			if (panel != null)
			{
				panel.refresh();
			}
		});

		if (client.getGameState() == GameState.LOGGED_IN && config.snapshotOnLogin())
		{
			log.debug("WealthTracker: already logged in at startUp, scheduling snapshot");
			scheduleLoginSnapshot();
		}
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.debug("WealthTracker: shutDown()");

		clientToolbar.removeNavigation(navButton);
		overlayManager.remove(overlay);

		if (executor != null && !executor.isShutdown())
		{
			executor.shutdownNow();
			log.debug("WealthTracker: executor shut down");
		}

		panel = null;
		navButton = null;
		executor = null;
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (!config.snapshotOnBankOpen()) return;
		if (event.getContainerId() != InventoryID.BANK.getId()) return;

		log.debug("WealthTracker: bank container changed (id={}), taking snapshot",
			event.getContainerId());
		takeSnapshot("bank_open");
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		GameState state = event.getGameState();
		log.debug("WealthTracker: game state changed to {}", state);

		if (state == GameState.LOGGED_IN && config.snapshotOnLogin())
		{
			log.debug("WealthTracker: scheduling login snapshot in 3 seconds");
			scheduleLoginSnapshot();
		}

		if (state == GameState.LOGIN_SCREEN || state == GameState.HOPPING)
		{
			dataManager.clearCache();
		}
	}

	private void scheduleLoginSnapshot()
	{
		executor.schedule(
			() -> clientThread.invokeLater(this::takeLoginSnapshot),
			3, TimeUnit.SECONDS
		);
	}

	private void takeLoginSnapshot()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			log.debug("WealthTracker: login snapshot aborted — state is {}", client.getGameState());
			return;
		}
		takeSnapshot("login");
	}

	private void takeSnapshot(String trigger)
	{
		log.debug("WealthTracker: takeSnapshot() trigger={}", trigger);

		long bankValue = 0;
		long inventoryValue = 0;
		long equipmentValue = 0;
		long coinsInHand = 0;
		long lootingBagValue = 0;
		long seedVaultValue = 0;
		long groupStorageValue = 0;
		Map<Integer, ItemSnapshot> breakdown = new HashMap<>();

		if (config.includeBank())
		{
			ItemContainer bank = client.getItemContainer(InventoryID.BANK);
			if (bank != null)
			{
				for (Item item : bank.getItems())
				{
					if (item == null || item.getId() <= 0) continue;
					long price = resolvePrice(item.getId());
					if (price < config.minItemValue()) continue;
					long total = price * item.getQuantity();
					bankValue += total;
					accumulateBreakdown(breakdown, item, price, total);
				}
			}
			else
			{
				log.debug("WealthTracker: bank container null — not opened yet this session");
			}
			log.debug("WealthTracker: bankValue={}", WealthUtils.formatGp(bankValue));
		}

		if (config.includeInventory())
		{
			ItemContainer inv = client.getItemContainer(InventoryID.INVENTORY);
			if (inv != null)
			{
				for (Item item : inv.getItems())
				{
					if (item == null || item.getId() <= 0) continue;
					if (item.getId() == ItemID.COINS_995)
					{
						coinsInHand += item.getQuantity();
					}
					long price = resolvePrice(item.getId());
					if (item.getId() != ItemID.COINS_995 && price < config.minItemValue()) continue;
					long total = price * item.getQuantity();
					inventoryValue += total;
					accumulateBreakdown(breakdown, item, price, total);
				}
			}
			log.debug("WealthTracker: inventoryValue={} coinsInHand={}",
				WealthUtils.formatGp(inventoryValue), coinsInHand);
		}

		if (config.includeEquipment())
		{
			ItemContainer equip = client.getItemContainer(InventoryID.EQUIPMENT);
			if (equip != null)
			{
				for (Item item : equip.getItems())
				{
					if (item == null || item.getId() <= 0) continue;
					long price = resolvePrice(item.getId());
					if (price < config.minItemValue()) continue;
					long total = price * item.getQuantity();
					equipmentValue += total;
					accumulateBreakdown(breakdown, item, price, total);
				}
			}
			log.debug("WealthTracker: equipmentValue={}", WealthUtils.formatGp(equipmentValue));
		}

		if (config.includeLootingBag())
		{
			// Looting bag container (id 516) is not on net.runelite.api.InventoryID.
			ItemContainer lootingBag = client.getItemContainer(516);
			if (lootingBag != null)
			{
				for (Item item : lootingBag.getItems())
				{
					if (item == null || item.getId() <= 0) continue;
					long price = resolvePrice(item.getId());
					if (price < config.minItemValue()) continue;
					long total = price * item.getQuantity();
					lootingBagValue += total;
					accumulateBreakdown(breakdown, item, price, total);
				}
			}
			else
			{
				log.debug("WealthTracker: looting bag container null — bag not open or not in inventory");
			}
			log.debug("WealthTracker: lootingBagValue={}", WealthUtils.formatGp(lootingBagValue));
		}

		if (config.includeSeedVault())
		{
			ItemContainer seedVault = client.getItemContainer(InventoryID.SEED_VAULT);
			if (seedVault != null)
			{
				for (Item item : seedVault.getItems())
				{
					if (item == null || item.getId() <= 0) continue;
					long price = resolvePrice(item.getId());
					if (price < config.minItemValue()) continue;
					long total = price * item.getQuantity();
					seedVaultValue += total;
					accumulateBreakdown(breakdown, item, price, total);
				}
			}
			else
			{
				log.debug("WealthTracker: seed vault container null — vault not open this session");
			}
			log.debug("WealthTracker: seedVaultValue={}", WealthUtils.formatGp(seedVaultValue));
		}

		if (config.includeGroupStorage())
		{
			ItemContainer groupStorage = client.getItemContainer(InventoryID.GROUP_STORAGE);
			if (groupStorage != null)
			{
				for (Item item : groupStorage.getItems())
				{
					if (item == null || item.getId() <= 0) continue;
					long price = resolvePrice(item.getId());
					if (price < config.minItemValue()) continue;
					long total = price * item.getQuantity();
					groupStorageValue += total;
					accumulateBreakdown(breakdown, item, price, total);
				}
			}
			else
			{
				log.debug("WealthTracker: group storage container null — not a GIM account or storage not opened");
			}
			log.debug("WealthTracker: groupStorageValue={}", WealthUtils.formatGp(groupStorageValue));
		}

		WealthSnapshot snapshot = WealthSnapshot.create(
			bankValue, inventoryValue, equipmentValue, coinsInHand,
			lootingBagValue, seedVaultValue, groupStorageValue,
			breakdown);

		log.info("WealthTracker: snapshot — total={} (bank={}, inv={}, equip={}, bag={}, seeds={}, gim={}) trigger={}",
			WealthUtils.formatGp(snapshot.getTotalNetWorth()),
			WealthUtils.formatGp(bankValue),
			WealthUtils.formatGp(inventoryValue),
			WealthUtils.formatGp(equipmentValue),
			WealthUtils.formatGp(lootingBagValue),
			WealthUtils.formatGp(seedVaultValue),
			WealthUtils.formatGp(groupStorageValue),
			trigger);

		executor.submit(() ->
		{
			try
			{
				dataManager.saveSnapshot(snapshot, config.dataRetentionDays());
				log.debug("WealthTracker: snapshot saved successfully");
			}
			catch (Exception e)
			{
				log.warn("WealthTracker: failed to save snapshot", e);
			}
			SwingUtilities.invokeLater(() ->
			{
				if (panel != null)
				{
					panel.refresh();
					log.debug("WealthTracker: panel refreshed on EDT");
				}
			});
		});
	}

	private long resolvePrice(int itemId)
	{
		try
		{
			if (config.priceSource() == WealthPriceSource.HIGH_ALCH)
			{
				return itemManager.getItemComposition(itemId).getHaPrice();
			}
			return itemManager.getItemPrice(itemId);
		}
		catch (Exception e)
		{
			log.debug("WealthTracker: could not get price for itemId={}: {}", itemId, e.getMessage());
			return 0;
		}
	}

	private void accumulateBreakdown(
		Map<Integer, ItemSnapshot> breakdown,
		Item item,
		long priceEach,
		long totalValue)
	{
		String name;
		try
		{
			name = itemManager.getItemComposition(item.getId()).getName();
		}
		catch (Exception e)
		{
			name = "Item #" + item.getId();
		}

		breakdown.merge(item.getId(),
			new ItemSnapshot(item.getId(), name, item.getQuantity(), priceEach, totalValue),
			(existing, incoming) -> new ItemSnapshot(
				existing.getItemId(),
				existing.getItemName(),
				existing.getQuantity() + incoming.getQuantity(),
				existing.getPriceEach(),
				existing.getTotalValue() + incoming.getTotalValue())
		);
	}

	private BufferedImage loadIcon()
	{
		try
		{
			return ImageUtil.loadImageResource(getClass(), "/com/wealthtracker/icon.png");
		}
		catch (Exception e)
		{
			log.warn("WealthTracker: icon not found, generating placeholder");
			BufferedImage placeholder = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
			java.awt.Graphics2D pg = placeholder.createGraphics();
			pg.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
				java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
			pg.setColor(new java.awt.Color(255, 180, 0));
			pg.fillOval(1, 1, 14, 14);
			pg.setColor(new java.awt.Color(200, 140, 0));
			pg.setStroke(new java.awt.BasicStroke(1f));
			pg.drawOval(1, 1, 13, 13);
			pg.setColor(new java.awt.Color(120, 80, 0));
			pg.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 9));
			pg.drawString("G", 4, 11);
			pg.dispose();
			return placeholder;
		}
	}

	@Provides
	WealthTrackerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(WealthTrackerConfig.class);
	}
}
