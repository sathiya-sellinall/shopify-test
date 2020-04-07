package com.sellinall.shopify.db;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;

import com.mongodb.BasicDBObject;

public class ProcessInventoryDBQueryResult implements Processor {
	static Logger log = Logger.getLogger(ProcessInventoryDBQueryResult.class
			.getName());

	@SuppressWarnings("unchecked")
	public void process(Exchange exchange) throws Exception {
		ArrayList<BasicDBObject> inventoryList = (ArrayList<BasicDBObject>) exchange
				.getIn().getBody();
		String SKU = exchange.getProperty("SKU").toString();
		String parentSKU = SKU.split("-")[0];
		
		// Initialize assuming the request is without variance
		BasicDBObject inventory = inventoryList.get(0);
		BasicDBObject parentInventory = inventoryList.get(0);
		exchange.setProperty("parentInventory", parentInventory); // without variance
		
		// Look for variance if any
		for (int index=0; index < inventoryList.size() ; index++) {
			if (inventoryList.get(index).getString("SKU").equals(parentSKU)) {
				parentInventory = inventoryList.get(index);
			} 
			if (inventoryList.get(index).getString("SKU").equals(SKU)) {
				inventory = inventoryList.get(index);
			}
		}
		// Does the request only for specific siteNicknames ?
		if (exchange.getProperty("siteNicknames") != null) {
			JSONArray siteNicknames = (JSONArray) exchange.getProperty("siteNicknames");
			List<String> siteNicknameList = new ArrayList<String>();
			for (int i = 0; i < siteNicknames.length(); i++) {
				siteNicknameList.add(siteNicknames.getString(i));
			}
			processRequestedNickNames(siteNicknameList, inventory, parentInventory);
		}
		exchange.setProperty("inventoryList", inventoryList);
		exchange.setProperty("parentInventory", parentInventory);
		exchange.getOut().setBody(inventory);
	}

	@SuppressWarnings("unchecked")
	private void processRequestedNickNames(List<String> siteNicknameList, BasicDBObject inventory, BasicDBObject parentInventory) {
		ArrayList<BasicDBObject> shopifyList = (ArrayList<BasicDBObject>) inventory.get("shopify");
		ArrayList<BasicDBObject> parentShopifyList = (ArrayList<BasicDBObject>) parentInventory.get("shopify");
		ArrayList<BasicDBObject> newShopifyList = new ArrayList<BasicDBObject>();
		for (BasicDBObject shopify : shopifyList) {
			if (siteNicknameList.contains(shopify.getString("nickNameID"))) {
				if ( ! inventory.getString("SKU").equals(parentInventory.getString("SKU"))) {
					// insert parent shopify record to its corresponding child record
					for (BasicDBObject parentShopify : parentShopifyList) {
						if (siteNicknameList.contains(parentShopify.getString("nickNameID"))) {
							shopify.put("parentShopify", parentShopify);
						}
					}
				}
				newShopifyList.add(shopify);
			}
		}
		inventory.put("shopify", newShopifyList);
	}

}
