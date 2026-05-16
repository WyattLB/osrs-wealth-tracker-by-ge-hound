package com.wealthtracker;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("wealthtracker")
public interface WealthTrackerConfig extends Config
{
	@ConfigItem(
		keyName = "includeBank",
		name = "Include Bank",
		description = "Count bank items in net worth",
		position = 0
	)
	default boolean includeBank()
	{
		return true;
	}

	@ConfigItem(
		keyName = "includeInventory",
		name = "Include Inventory",
		description = "Count inventory items in net worth",
		position = 1
	)
	default boolean includeInventory()
	{
		return true;
	}

	@ConfigItem(
		keyName = "includeEquipment",
		name = "Include Equipment",
		description = "Count worn equipment in net worth",
		position = 2
	)
	default boolean includeEquipment()
	{
		return true;
	}

	@ConfigItem(
		keyName = "snapshotOnBankOpen",
		name = "Snapshot on Bank Open",
		description = "Record net worth each time you open your bank",
		position = 3
	)
	default boolean snapshotOnBankOpen()
	{
		return true;
	}

	@ConfigItem(
		keyName = "snapshotOnLogin",
		name = "Snapshot on Login",
		description = "Record net worth when you log in",
		position = 4
	)
	default boolean snapshotOnLogin()
	{
		return true;
	}

	@ConfigItem(
		keyName = "priceSource",
		name = "Price Source",
		description = "GE price or High Alch value",
		position = 5
	)
	default WealthPriceSource priceSource()
	{
		return WealthPriceSource.GE;
	}

	@Range(min = 0, max = 10_000_000)
	@ConfigItem(
		keyName = "minItemValue",
		name = "Min Item Value (gp)",
		description = "Ignore items worth less than this",
		position = 6
	)
	default int minItemValue()
	{
		return 10000;
	}

	@Range(min = 1, max = 365)
	@ConfigItem(
		keyName = "dataRetentionDays",
		name = "History (days)",
		description = "Days of history to keep (1-365)",
		position = 7
	)
	default int dataRetentionDays()
	{
		return 90;
	}

	@ConfigItem(
		keyName = "showOverlay",
		name = "Show Overlay",
		description = "Show net worth as a small in-game HUD",
		position = 8
	)
	default boolean showOverlay()
	{
		return false;
	}
}
