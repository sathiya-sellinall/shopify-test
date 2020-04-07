package com.sellinall.shopify.db;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import com.mongodb.BasicDBObject;

public class ProcessReferenceIDResult implements Processor {
	static Logger log = Logger.getLogger(ProcessReferenceIDResult.class.getName());

	@SuppressWarnings("unchecked")
	public void process(Exchange exchange) throws Exception {
		ArrayList<BasicDBObject> inventoryList = (ArrayList<BasicDBObject>) exchange.getIn().getBody();
		HashMap<String, String> SKUMap = new HashMap<String, String>();
		exchange.setProperty("SKUMap", SKUMap);
		if (inventoryList.isEmpty()) {
			log.info("If no invertory record for this reference ID the serious issue");
			return;
		}
		ArrayList<String> productIdList = exchange.getProperty("productIdList", ArrayList.class);
		
		for (String productId : productIdList) {
			SKUMap.put(productId, getSKUFromInventory(inventoryList, productId));
		}
		exchange.setProperty("SKUMap", SKUMap);
	}

	private String getSKUFromInventory(ArrayList<BasicDBObject> inventoryList, String variantRefrenceId) {
		for (BasicDBObject inventory : inventoryList) {
			ArrayList<BasicDBObject> shopifyList = (ArrayList<BasicDBObject>) inventory.get("shopify");
			// Here always we got single object
			BasicDBObject shopify = shopifyList.get(0);
			if (shopify.getString("variantRefrenceId").equals(variantRefrenceId)) {
				return inventory.getString("SKU");
			}
		}
		return "";
	}
}
