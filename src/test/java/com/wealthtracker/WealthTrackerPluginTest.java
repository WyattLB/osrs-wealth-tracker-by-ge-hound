package com.wealthtracker;

import java.util.List;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WealthTrackerPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(WealthTrackerPlugin.class);
		RuneLite.main(args);
	}

	@Test
	public void formatGp_zero()
	{
		assertEquals("0 gp", WealthUtils.formatGp(0));
	}

	@Test
	public void formatGp_smallValue()
	{
		assertEquals("500 gp", WealthUtils.formatGp(500));
	}

	@Test
	public void formatGp_thousands()
	{
		assertEquals("1,500 gp", WealthUtils.formatGp(1_500));
	}

	@Test
	public void formatGp_justUnderMillion()
	{
		assertEquals("999,999 gp", WealthUtils.formatGp(999_999));
	}

	@Test
	public void formatGp_exactlyOneMillion()
	{
		assertEquals("1.00m", WealthUtils.formatGp(1_000_000));
	}

	@Test
	public void formatGp_millions()
	{
		assertEquals("1.04m", WealthUtils.formatGp(1_042_388));
	}

	@Test
	public void formatGp_billions()
	{
		assertEquals("2.15b", WealthUtils.formatGp(2_147_483_647L));
	}

	@Test
	public void formatGp_negative()
	{
		String result = WealthUtils.formatGp(-500_000);
		assertTrue("Expected negative prefix", result.startsWith("-"));
	}

	@Test
	public void formatDelta_positive()
	{
		String result = WealthUtils.formatDelta(8_200_000);
		assertTrue("Should start with up arrow", result.startsWith("↑"));
		assertTrue("Should contain plus sign", result.contains("+"));
	}

	@Test
	public void formatDelta_negative()
	{
		String result = WealthUtils.formatDelta(-500_000);
		assertTrue("Should start with down arrow", result.startsWith("↓"));
	}

	@Test
	public void formatDelta_zero()
	{
		String result = WealthUtils.formatDelta(0);
		assertTrue("Should start with neutral arrow", result.startsWith("→"));
	}

	@Test
	public void formatPercentage_increase()
	{
		assertEquals("+11.1%", WealthUtils.formatPercentage(900, 1000));
	}

	@Test
	public void formatPercentage_decrease()
	{
		assertEquals("-10.0%", WealthUtils.formatPercentage(1000, 900));
	}

	@Test
	public void formatPercentage_fromZero()
	{
		assertEquals("N/A", WealthUtils.formatPercentage(0, 1000));
	}

	@Test
	public void formatPercentage_noChange()
	{
		assertEquals("+0.0%", WealthUtils.formatPercentage(1000, 1000));
	}

	@Test
	public void filterByDays_allTime_returnsAll()
	{
		WealthSnapshot s = WealthSnapshot.create(100, 0, 0, 0, null);
		List<WealthSnapshot> list = java.util.Arrays.asList(s);
		List<WealthSnapshot> result = WealthUtils.filterByDays(list, Integer.MAX_VALUE);
		assertEquals(1, result.size());
	}

	@Test
	public void snapshot_totalNetWorth_doesNotIncludeCoinsInHandTwice()
	{
		WealthSnapshot s = WealthSnapshot.create(100_000L, 50_000L, 75_000L, 5_000L, null);
		assertEquals(225_000L, s.getTotalNetWorth());
	}

	@Test
	public void snapshot_timestampIsRecent()
	{
		long before = System.currentTimeMillis();
		WealthSnapshot s = WealthSnapshot.create(0, 0, 0, 0, null);
		long after = System.currentTimeMillis();
		assertTrue(s.getTimestamp() >= before);
		assertTrue(s.getTimestamp() <= after);
	}
}
