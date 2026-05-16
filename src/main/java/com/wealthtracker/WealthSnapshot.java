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
	private long totalNetWorth;
	private Map<Integer, ItemSnapshot> itemBreakdown;

	public static WealthSnapshot create(
		long bankValue,
		long inventoryValue,
		long equipmentValue,
		long coinsInHand,
		Map<Integer, ItemSnapshot> breakdown)
	{
		WealthSnapshot s = new WealthSnapshot();
		s.timestamp = System.currentTimeMillis();
		s.bankValue = bankValue;
		s.inventoryValue = inventoryValue;
		s.equipmentValue = equipmentValue;
		s.coinsInHand = coinsInHand;
		s.totalNetWorth = bankValue + inventoryValue + equipmentValue;
		s.itemBreakdown = breakdown;
		return s;
	}
}
