package com.sellinall.shopify.common;

public enum ShopifyFullfillmentStatus {
	FULLFILLED("fullfilled"),
	PARTIAL("partial"),
	NULL("null");
	
	private final String name;       

	private ShopifyFullfillmentStatus(String s) {
		name = s;
	}

	public boolean equalsName(String otherName) {
		return (otherName == null) ? false : name.equals(otherName);
	}

	public String toString() {
		return this.name;
	}
}
