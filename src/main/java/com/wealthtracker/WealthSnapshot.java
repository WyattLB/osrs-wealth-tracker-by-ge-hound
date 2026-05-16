package com.wealthtracker;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WealthSnapshot
{
	private long timestamp;
	private long bankValue;
	private long inventoryValue;
	private long equipmentValue;
	private long coinsInHand;
	private long lootingBagValue;
	private long seedVaultValue;
	private long groupStorageValue;
	private long totalNetWorth;
	private Map<Integer, ItemSnapshot> itemBreakdown;

	public static WealthSnapshot create(
		long bankValue,
		long inventoryValue,
		long equipmentValue,
		long coinsInHand,
		long lootingBagValue,
		long seedVaultValue,
		long groupStorageValue,
		Map<Integer, ItemSnapshot> breakdown)
	{
		WealthSnapshot s = new WealthSnapshot();
		s.timestamp = System.currentTimeMillis();
		s.bankValue = bankValue;
		s.inventoryValue = inventoryValue;
		s.equipmentValue = equipmentValue;
		s.coinsInHand = coinsInHand;
		s.lootingBagValue = lootingBagValue;
		s.seedVaultValue = seedVaultValue;
		s.groupStorageValue = groupStorageValue;
		s.totalNetWorth = bankValue + inventoryValue + equipmentValue
			+ lootingBagValue + seedVaultValue + groupStorageValue;
		s.itemBreakdown = breakdown;
		return s;
	}
}
