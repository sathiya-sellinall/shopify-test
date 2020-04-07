package com.sellinall.shopify.db;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;

import com.mongodb.BasicDBObject;

public class ProcessSKUDBQueryResult implements Processor {

	static Logger log = Logger.getLogger(ProcessSKUDBQueryResult.class.getName());

	@SuppressWarnings("unchecked")
	public void process(Exchange exchange) throws Exception {
		ArrayList<BasicDBObject> inventoryList = (ArrayList<BasicDBObject>) exchange.getIn().getBody();
		exchange.setProperty("inventoryList", inventoryList);
		String SKU = exchange.getProperty("SKU").toString();
		String parentSKU = SKU.split("-")[0];

		exchange.setProperty("hasVariants", false);
		if (inventoryList.size() > 1) {
			exchange.setProperty("hasVariants", true);
		}
		// Initialize assuming the request is without variance
		BasicDBObject inventory = inventoryList.get(0);
		BasicDBObject parentInventory = inventoryList.get(0);

		// Look for variance if any
		for (int index = 0; index < inventoryList.size(); index++) {
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
			inventory = processRequestedNickNames(siteNicknameList, inventory, parentInventory);
		}
		processVariantsChannelData(exchange, inventoryList, parentSKU);
		exchange.setProperty("parentInventory", getInventory(parentInventory));
		exchange.setProperty("shopifyInstance", inventory);
		exchange.setProperty("inventory", getInventory(inventory));
	}

	@SuppressWarnings("unchecked")
	private BasicDBObject processRequestedNickNames(List<String> siteNicknameList, BasicDBObject inventory,
			BasicDBObject parentInventory) {
		ArrayList<BasicDBObject> shopifyList = (ArrayList<BasicDBObject>) inventory.get("shopify");
		ArrayList<BasicDBObject> parentShopifyList = (ArrayList<BasicDBObject>) parentInventory.get("shopify");
		ArrayList<BasicDBObject> newShopifyList = new ArrayList<BasicDBObject>();
		for (BasicDBObject shopify : shopifyList) {
			if (siteNicknameList.contains(shopify.getString("nickNameID"))) {
				if (!inventory.getString("SKU").equals(parentInventory.getString("SKU"))) {
					// insert parent shopify record to its corresponding child
					// record
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
		log.debug("inventory manpulated :" + inventory);
		return inventory;
	}

	@SuppressWarnings("unchecked")
	private BasicDBObject getInventory(BasicDBObject inventory) {
		if (inventory.get("shopify") instanceof BasicDBObject) {
			return inventory;
		}
		ArrayList<BasicDBObject> shopify = (ArrayList<BasicDBObject>) inventory.get("shopify");
		inventory.put("shopify", shopify.get(0));
		return inventory;
	}
	
	private void processVariantsChannelData(Exchange exchange, ArrayList<BasicDBObject> inventoryList,
			String parentSKU) {
		// Here we convert arrayList to BasicDbObject
		// query based on nickNameID
		ArrayList<BasicDBObject> list = new ArrayList<BasicDBObject>();
		for (BasicDBObject inventory : inventoryList) {
			if (!inventory.getString("SKU").equals(parentSKU)) {
				inventory.put("shopify", processChannelData((ArrayList<BasicDBObject>) inventory.get("shopify")));
				list.add(inventory);
			}
		}
		exchange.setProperty("inventoryDetails", list);
	}
	
	private BasicDBObject processChannelData(ArrayList<BasicDBObject> channelList) {
		// All ways list have single data only
		// query based on nickNameID
		return (BasicDBObject) channelList.get(0);
	}
}
