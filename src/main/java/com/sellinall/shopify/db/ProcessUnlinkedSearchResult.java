package com.sellinall.shopify.db;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import com.mongodb.BasicDBObject;
import com.sellinall.util.InventorySequence;

public class ProcessUnlinkedSearchResult implements Processor {

	public void process(Exchange exchange) throws Exception {
		BasicDBObject unlinkedInventory = exchange.getIn().getBody(BasicDBObject.class);
		if (unlinkedInventory == null) {
			String SKU = InventorySequence.getNextUnlinkedSKU(exchange.getProperty("merchantID", String.class));
			exchange.setProperty("unlinkedInventorySKU", SKU);
			exchange.setProperty("isNewItem", true);
			return;
		}
		exchange.setProperty("isNewItem", false);
		String SKU = unlinkedInventory.getString("SKU");
		exchange.setProperty("unlinkedInventorySKU", SKU);

	}
}
