package com.wealthtracker;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

@Slf4j
public class WealthTrackerOverlay extends Overlay
{
	private static final Color WEALTH_COLOR = new Color(0, 200, 83);

	private final WealthDataManager dataManager;
	private final WealthTrackerConfig config;

	@Inject
	public WealthTrackerOverlay(WealthDataManager dataManager, WealthTrackerConfig config)
	{
		this.dataManager = dataManager;
		this.config = config;

		setPosition(OverlayPosition.TOP_LEFT);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.showOverlay())
		{
			return null;
		}

		WealthSnapshot latest = dataManager.getLatestSnapshot();
		if (latest == null)
		{
			return null;
		}

		PanelComponent panelComponent = new PanelComponent();
		panelComponent.getChildren().add(
			TitleComponent.builder()
				.text("💰 " + WealthUtils.formatGp(latest.getTotalNetWorth()))
				.color(WEALTH_COLOR)
				.build()
		);

		return panelComponent.render(graphics);
	}
}
