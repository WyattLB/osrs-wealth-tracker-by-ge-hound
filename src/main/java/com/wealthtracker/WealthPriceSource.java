package com.wealthtracker;

public enum WealthPriceSource
{
	GE("GE Price"),
	HIGH_ALCH("High Alch Value");

	private final String displayName;

	WealthPriceSource(String displayName)
	{
		this.displayName = displayName;
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
