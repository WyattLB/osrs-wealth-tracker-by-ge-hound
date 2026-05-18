package com.wealthtracker;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.List;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.LinkBrowser;

@Slf4j
public class WealthTrackerPanel extends PluginPanel
{
	private static final int[] RANGES = {1, 7, 30, Integer.MAX_VALUE};
	private static final String[] LABELS = {"1D", "7D", "30D", "All"};

	private final WealthDataManager dataManager;
	private final WealthTrackerConfig config;

	private final WealthChartPanel chartPanel = new WealthChartPanel();

	private final JLabel netWorthLabel = new JLabel("Loading...");
	private final JLabel deltaLabel = new JLabel(" ");
	private final JLabel bankLabel = new JLabel("—");
	private final JLabel invLabel = new JLabel("—");
	private final JLabel equipLabel = new JLabel("—");
	private final JLabel lootBagLabel = new JLabel("—");
	private final JLabel seedVaultLabel = new JLabel("—");
	private final JLabel groupStorageLabel = new JLabel("—");
	private final JLabel[] moverLabels = {new JLabel("—"), new JLabel("—"), new JLabel("—")};

	private int selectedRangeIndex = 1;

	public WealthTrackerPanel(WealthDataManager dataManager, WealthTrackerConfig config)
	{
		this.dataManager = dataManager;
		this.config = config;

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(new EmptyBorder(10, 10, 10, 10));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		buildHeader();
		add(Box.createVerticalStrut(6));
		buildNetWorthSection();
		add(Box.createVerticalStrut(4));
		buildRangeTabs();
		add(Box.createVerticalStrut(4));
		add(chartPanel);
		add(Box.createVerticalStrut(8));
		addSeparator();
		add(Box.createVerticalStrut(6));
		buildBreakdownSection();
		add(Box.createVerticalStrut(6));
		addSeparator();
		add(Box.createVerticalStrut(6));
		buildMoversSection();
		add(Box.createVerticalStrut(8));
		addSeparator();
		add(Box.createVerticalStrut(6));
		buildFooter();
	}

	private void buildHeader()
	{
		JPanel headerPanel = new JPanel(new BorderLayout());
		headerPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		headerPanel.setBorder(new EmptyBorder(8, 10, 8, 10));
		headerPanel.setAlignmentX(LEFT_ALIGNMENT);
		headerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

		JLabel header = new JLabel("💰 WEALTH TRACKER");
		header.setFont(FontManager.getRunescapeBoldFont().deriveFont(13f));
		header.setForeground(Color.WHITE);
		header.setHorizontalAlignment(SwingConstants.CENTER);

		headerPanel.add(header, BorderLayout.CENTER);
		add(headerPanel);
	}

	private void buildNetWorthSection()
	{
		netWorthLabel.setFont(FontManager.getRunescapeBoldFont().deriveFont(20f));
		netWorthLabel.setBorder(new EmptyBorder(4, 0, 2, 0));
		netWorthLabel.setForeground(new Color(0, 200, 83));
		netWorthLabel.setAlignmentX(CENTER_ALIGNMENT);
		add(netWorthLabel);

		deltaLabel.setFont(FontManager.getRunescapeSmallFont());
		deltaLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		deltaLabel.setAlignmentX(CENTER_ALIGNMENT);
		add(deltaLabel);
	}

	private void buildRangeTabs()
	{
		JPanel tabPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
		tabPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		tabPanel.setAlignmentX(CENTER_ALIGNMENT);

		ButtonGroup group = new ButtonGroup();
		for (int i = 0; i < LABELS.length; i++)
		{
			final int rangeIndex = i;
			JToggleButton btn = new JToggleButton(LABELS[i]);
			btn.setFont(FontManager.getRunescapeSmallFont());
			btn.setForeground(Color.WHITE);
			btn.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			btn.setBorderPainted(false);
			btn.setFocusPainted(false);
			btn.setSelected(i == selectedRangeIndex);
			btn.addActionListener(e ->
			{
				selectedRangeIndex = rangeIndex;
				refresh();
			});
			group.add(btn);
			tabPanel.add(btn);
		}
		add(tabPanel);
	}

	private void buildBreakdownSection()
	{
		add(sectionTitle("BREAKDOWN"));
		add(Box.createVerticalStrut(4));
		add(buildRow("Bank", bankLabel));
		add(buildRow("Inventory", invLabel));
		add(buildRow("Equipped", equipLabel));
		add(buildRow("Looting Bag", lootBagLabel));
		add(buildRow("Seed Vault", seedVaultLabel));
		add(buildRow("GIM Storage", groupStorageLabel));
	}

	private void buildMoversSection()
	{
		add(sectionTitle("TOP MOVERS"));
		add(Box.createVerticalStrut(4));
		for (JLabel moverLabel : moverLabels)
		{
			moverLabel.setFont(FontManager.getRunescapeSmallFont());
			moverLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			moverLabel.setBorder(new EmptyBorder(2, 0, 2, 0));
			add(moverLabel);
		}
	}

	private void buildFooter()
	{
		JPanel linkPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
		linkPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		linkPanel.setAlignmentX(LEFT_ALIGNMENT);

		JLabel linkPrefix = new JLabel("Powered by ");
		linkPrefix.setFont(FontManager.getRunescapeSmallFont());
		linkPrefix.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

		JLabel link = new JLabel("gehound.com");
		link.setFont(FontManager.getRunescapeSmallFont());
		link.setForeground(new Color(100, 180, 255));
		link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		link.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				LinkBrowser.browse("https://gehound.com");
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				link.setForeground(new Color(150, 210, 255));
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				link.setForeground(new Color(100, 180, 255));
			}
		});

		linkPanel.add(linkPrefix);
		linkPanel.add(link);

		JButton exportBtn = new JButton("Export CSV");
		exportBtn.setFont(FontManager.getRunescapeSmallFont());
		exportBtn.setBackground(new Color(50, 50, 50));
		exportBtn.setForeground(Color.WHITE);
		exportBtn.setBorderPainted(true);
		exportBtn.setFocusPainted(false);
		exportBtn.setAlignmentX(CENTER_ALIGNMENT);
		exportBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
		exportBtn.addActionListener(e -> exportCsv());

		add(linkPanel);
		add(Box.createVerticalStrut(4));
		add(exportBtn);
	}

	private JLabel sectionTitle(String text)
	{
		JLabel label = new JLabel(text);
		label.setFont(FontManager.getRunescapeSmallFont().deriveFont(java.awt.Font.BOLD));
		label.setForeground(new Color(160, 160, 160));
		label.setBorder(new EmptyBorder(6, 0, 2, 0));
		return label;
	}

	private JPanel buildRow(String labelText, JLabel valueLabel)
	{
		JPanel row = new JPanel(new BorderLayout());
		row.setBackground(ColorScheme.DARK_GRAY_COLOR);
		row.setBorder(new EmptyBorder(1, 0, 1, 0));

		JLabel key = new JLabel(labelText);
		key.setFont(FontManager.getRunescapeSmallFont());
		key.setForeground(new Color(170, 170, 170));

		valueLabel.setFont(FontManager.getRunescapeSmallFont());
		valueLabel.setForeground(Color.WHITE);

		row.add(key, BorderLayout.WEST);
		row.add(valueLabel, BorderLayout.EAST);
		return row;
	}

	private String truncateItemName(String name, int maxChars)
	{
		name = name.replaceAll("\\s*\\(Members\\)", "")
			.replaceAll("\\s*\\(members\\)", "")
			.trim();
		if (name.length() <= maxChars) return name;
		return name.substring(0, maxChars - 1) + "…";
	}

	private void addSeparator()
	{
		JSeparator sep = new JSeparator();
		sep.setBackground(ColorScheme.MEDIUM_GRAY_COLOR);
		sep.setForeground(ColorScheme.MEDIUM_GRAY_COLOR);
		sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
		add(sep);
	}

	public void refresh()
	{
		assert SwingUtilities.isEventDispatchThread() : "WealthTrackerPanel.refresh() called off EDT!";

		List<WealthSnapshot> all = dataManager.loadSnapshots();

		if (all.isEmpty())
		{
			netWorthLabel.setText("No data yet");
			netWorthLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			deltaLabel.setText("Open your bank to start tracking");
			chartPanel.setData(null);
			bankLabel.setText("—");
			invLabel.setText("—");
			equipLabel.setText("—");
			lootBagLabel.setText("—");
			seedVaultLabel.setText("—");
			groupStorageLabel.setText("—");
			bankLabel.setForeground(new Color(80, 80, 80));
			invLabel.setForeground(new Color(80, 80, 80));
			equipLabel.setForeground(new Color(80, 80, 80));
			lootBagLabel.setForeground(new Color(80, 80, 80));
			seedVaultLabel.setForeground(new Color(80, 80, 80));
			groupStorageLabel.setForeground(new Color(80, 80, 80));
			for (JLabel ml : moverLabels)
			{
				ml.setText("—");
				ml.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			}
			return;
		}

		WealthSnapshot latest = all.get(all.size() - 1);
		WealthSnapshot prev = all.size() > 1 ? all.get(all.size() - 2) : null;

		netWorthLabel.setText(WealthUtils.formatGp(latest.getTotalNetWorth()));
		netWorthLabel.setForeground(new Color(0, 200, 83));

		if (prev != null)
		{
			long delta = latest.getTotalNetWorth() - prev.getTotalNetWorth();
			String line1Color = delta > 0 ? "#00c853" : delta < 0 ? "#dc3232" : "#aaaaaa";
			String line1 = "<font color='" + line1Color + "'>"
				+ WealthUtils.formatDelta(delta)
				+ " (" + WealthUtils.formatPercentage(prev.getTotalNetWorth(),
					latest.getTotalNetWorth()) + ")</font>";
			String line2 = WealthUtils.formatTimeSince(prev.getTimestamp());
			deltaLabel.setText("<html><center>" + line1 + "<br><font color='#888888'>"
				+ line2 + "</font></center></html>");
		}
		else
		{
			deltaLabel.setText("First snapshot recorded");
			deltaLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		}

		int days = RANGES[selectedRangeIndex];
		List<WealthSnapshot> filtered = WealthUtils.filterByDays(all, days);
		chartPanel.setData(filtered);

		bankLabel.setText(WealthUtils.formatGp(latest.getBankValue()));
		invLabel.setText(WealthUtils.formatGp(latest.getInventoryValue()));
		equipLabel.setText(WealthUtils.formatGp(latest.getEquipmentValue()));
		bankLabel.setForeground(Color.WHITE);
		invLabel.setForeground(Color.WHITE);
		equipLabel.setForeground(Color.WHITE);

		lootBagLabel.setText(
			config.includeLootingBag()
				? WealthUtils.formatGp(latest.getLootingBagValue())
				: "—");
		seedVaultLabel.setText(
			config.includeSeedVault()
				? WealthUtils.formatGp(latest.getSeedVaultValue())
				: "—");
		groupStorageLabel.setText(
			config.includeGroupStorage()
				? WealthUtils.formatGp(latest.getGroupStorageValue())
				: "—");
		lootBagLabel.setForeground(
			config.includeLootingBag() ? Color.WHITE : new Color(80, 80, 80));
		seedVaultLabel.setForeground(
			config.includeSeedVault() ? Color.WHITE : new Color(80, 80, 80));
		groupStorageLabel.setForeground(
			config.includeGroupStorage() ? Color.WHITE : new Color(80, 80, 80));

		if (prev != null
			&& prev.getItemBreakdown() != null
			&& latest.getItemBreakdown() != null)
		{
			List<WealthUtils.ItemDelta> movers =
				WealthUtils.calculateMovers(prev, latest, 3);
			for (int i = 0; i < moverLabels.length; i++)
			{
				if (i < movers.size())
				{
					WealthUtils.ItemDelta m = movers.get(i);
					String itemName = truncateItemName(m.getItemName(), 22);
					String deltaStr = WealthUtils.formatDelta(m.getDelta());
					moverLabels[i].setText("<html><b>" + itemName + "</b>&nbsp;&nbsp;"
						+ "<font color='" + (m.getDelta() >= 0 ? "#00c853" : "#dc3232") + "'>"
						+ deltaStr + "</font></html>");
					moverLabels[i].setForeground(Color.WHITE);
				}
				else
				{
					moverLabels[i].setText("—");
					moverLabels[i].setForeground(ColorScheme.LIGHT_GRAY_COLOR);
				}
			}
		}
		else
		{
			for (JLabel ml : moverLabels)
			{
				ml.setText("—");
				ml.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			}
		}
	}

	private void exportCsv()
	{
		List<WealthSnapshot> all = dataManager.loadSnapshots();
		if (all.isEmpty())
		{
			JOptionPane.showMessageDialog(this,
				"No data to export yet.\nOpen your bank first to record a snapshot.",
				"Nothing to Export", JOptionPane.INFORMATION_MESSAGE);
			return;
		}

		JFileChooser chooser = new JFileChooser(System.getProperty("user.home"));
		chooser.setSelectedFile(new java.io.File(
			System.getProperty("user.home"), "wealth-history.csv"));
		if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

		try (java.io.PrintWriter pw = new java.io.PrintWriter(chooser.getSelectedFile()))
		{
			pw.println("timestamp_ms,date,total_net_worth_gp,bank_gp,inventory_gp,equipment_gp,looting_bag_gp,seed_vault_gp,group_storage_gp");
			for (WealthSnapshot s : all)
			{
				pw.printf("%d,\"%s\",%d,%d,%d,%d,%d,%d,%d%n",
					s.getTimestamp(),
					new java.util.Date(s.getTimestamp()),
					s.getTotalNetWorth(),
					s.getBankValue(),
					s.getInventoryValue(),
					s.getEquipmentValue(),
					s.getLootingBagValue(),
					s.getSeedVaultValue(),
					s.getGroupStorageValue());
			}
			log.debug("WealthTracker: exported {} rows to CSV", all.size());
			JOptionPane.showMessageDialog(this,
				"Exported " + all.size() + " snapshots.",
				"Export Complete", JOptionPane.INFORMATION_MESSAGE);
		}
		catch (Exception e)
		{
			log.warn("WealthTracker: CSV export failed", e);
			JOptionPane.showMessageDialog(this,
				"Export failed: " + e.getMessage(),
				"Export Error", JOptionPane.ERROR_MESSAGE);
		}
	}
}
