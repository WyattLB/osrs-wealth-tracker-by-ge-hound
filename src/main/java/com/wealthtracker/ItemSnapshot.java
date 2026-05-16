package com.wealthtracker;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemSnapshot
{
	private int itemId;
	private String itemName;
	private int quantity;
	private long priceEach;
	private long totalValue;
}
