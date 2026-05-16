package com.wealthtracker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class WealthUtils
{
	private WealthUtils() {}

	public static String formatGp(long value)
	{
		if (value < 0)
		{
			return "-" + formatGp(-value);
		}
		if (value >= 1_000_000_000L)
		{
			return String.format("%.2fb", value / 1_000_000_000.0);
		}
		if (value >= 1_000_000L)
		{
			return String.format("%.2fm", value / 1_000_000.0);
		}
		if (value >= 1_000L)
		{
			return String.format("%,d gp", value);
		}
		return value + " gp";
	}

	public static String formatDelta(long delta)
	{
		if (delta > 0) return "↑ +" + formatGp(delta);
		if (delta < 0) return "↓ " + formatGp(delta);
		return "→ " + formatGp(0);
	}

	public static String formatPercentage(long from, long to)
	{
		if (from == 0) return "N/A";
		double pct = ((double) (to - from) / from) * 100.0;
		return String.format("%+.1f%%", pct);
	}

	public static String formatTimeSince(long epochMs)
	{
		long secondsAgo = (System.currentTimeMillis() - epochMs) / 1000L;
		if (secondsAgo < 60) return "just now";
		if (secondsAgo < 3600) return (secondsAgo / 60) + "m ago";
		if (secondsAgo < 86400) return (secondsAgo / 3600) + "h ago";
		return (secondsAgo / 86400) + "d ago";
	}

	public static List<ItemDelta> calculateMovers(
		WealthSnapshot prev, WealthSnapshot current, int topN)
	{
		if (prev == null || current == null) return Collections.emptyList();
		if (prev.getItemBreakdown() == null || current.getItemBreakdown() == null)
		{
			return Collections.emptyList();
		}

		List<ItemDelta> deltas = new ArrayList<>();
		Map<Integer, ItemSnapshot> prevItems = prev.getItemBreakdown();
		Map<Integer, ItemSnapshot> currItems = current.getItemBreakdown();

		for (Map.Entry<Integer, ItemSnapshot> entry : currItems.entrySet())
		{
			ItemSnapshot currItem = entry.getValue();
			ItemSnapshot prevItem = prevItems.get(entry.getKey());
			long prevTotal = prevItem != null ? prevItem.getTotalValue() : 0;
			long delta = currItem.getTotalValue() - prevTotal;
			if (delta != 0)
			{
				deltas.add(new ItemDelta(currItem.getItemName(), delta));
			}
		}

		for (Map.Entry<Integer, ItemSnapshot> entry : prevItems.entrySet())
		{
			if (!currItems.containsKey(entry.getKey()))
			{
				deltas.add(new ItemDelta(entry.getValue().getItemName(),
					-entry.getValue().getTotalValue()));
			}
		}

		deltas.sort((a, b) -> Long.compare(Math.abs(b.delta), Math.abs(a.delta)));
		return deltas.subList(0, Math.min(topN, deltas.size()));
	}

	public static List<WealthSnapshot> filterByDays(List<WealthSnapshot> snapshots, int days)
	{
		if (days == Integer.MAX_VALUE) return snapshots;
		long cutoff = System.currentTimeMillis() - ((long) days * 86_400_000L);
		List<WealthSnapshot> result = new ArrayList<>();
		for (WealthSnapshot s : snapshots)
		{
			if (s.getTimestamp() >= cutoff)
			{
				result.add(s);
			}
		}
		return result;
	}

	public static final class ItemDelta
	{
		public final String itemName;
		public final long delta;

		public ItemDelta(String itemName, long delta)
		{
			this.itemName = Objects.requireNonNull(itemName, "itemName");
			this.delta = delta;
		}

		public String getItemName() { return itemName; }
		public long getDelta() { return delta; }
	}
}
