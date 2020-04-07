package com.sellinall.shopify.process;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;

public class ProcessQuantityUpdate implements Processor {
	static Logger log = Logger.getLogger(ProcessQuantityUpdate.class.getName());

	@Override
	public void process(Exchange exchange) throws Exception {
		Map<String, String> skuInventoryItemIDMap = exchange.getProperty("skuInventoryItemIDMap", Map.class);
		List<JSONObject> locationList = exchange.getProperty("locationsList", List.class);
		ArrayList<BasicDBObject> inventoryList = (ArrayList<BasicDBObject>) exchange.getProperty("inventoryList");
		JSONObject locationObject = locationList.get(0);
		ArrayList<JSONObject> stockUpdatePayloadList = new ArrayList<JSONObject>();
		for (BasicDBObject inventory : inventoryList) {
			String SKU = inventory.getString("SKU");
			if (skuInventoryItemIDMap.containsKey(SKU)) {
				BasicDBObject shopifyObject = (BasicDBObject) inventory.get("shopify");
				int quantity = shopifyObject.getInt("noOfItem");
				JSONObject payloadObject = new JSONObject();
				payloadObject.put("locationID", locationObject.getLong("id"));
				payloadObject.put("available", quantity);
				payloadObject.put("inventoryItemID", Long.parseLong(skuInventoryItemIDMap.get(SKU)));
				stockUpdatePayloadList.add(payloadObject);
			}

		}
		exchange.setProperty("stockUpdatePayloadList", stockUpdatePayloadList);
	}
}