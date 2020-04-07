package com.sellinall.shopify.common;

public enum ShopifyFinancialStatus {
	PENDING("pending"),
	AUTHORIZED("authorized"),
	PAID("paid"),
	REFUNDED("refunded"),
	VOIDED("voided");
	
	private final String name;       

	private ShopifyFinancialStatus(String s) {
		name = s;
	}

	public boolean equalsName(String otherName) {
		return (otherName == null) ? false : name.equals(otherName);
	}

	public String toString() {
		return this.name;
	}
}
