package com.sellinall.shopify.message;

import java.util.ArrayList;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;

public class PrepareBatchMessage implements Processor {
	static Logger log = Logger.getLogger(PrepareBatchMessage.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject SKUMap = exchange.getProperty("SKUMap", JSONObject.class);
		String failureReason = "";
		String requestType = "";
		ArrayList<JSONObject> SKUMaps = new ArrayList<JSONObject>();
		if (exchange.getProperties().containsKey("failureReason")) {
			failureReason = exchange.getProperty("failureReason", String.class);
			requestType = exchange.getProperty("requestType", String.class);
		}
		if (SKUMap.getString("SKU").contains("-")) {
			ArrayList<BasicDBObject> inventoryDetails = (ArrayList<BasicDBObject>) exchange
					.getProperty("inventoryDetails");
			// we need only number of child records so have to ignore parent
			// record
			JSONObject rowIdentifier = SKUMap.getJSONObject("rowIdentifier");
			String parentSKU = SKUMap.getString("SKU").split("-")[0];
			int rowId = rowIdentifier.getInt("rowId");
			for (int i = 0; i < inventoryDetails.size(); i++) {
				JSONObject batchMessage = new JSONObject();
				JSONObject formRowIdentifier = new JSONObject();
				formRowIdentifier.put("docId", rowIdentifier.getString("docId"));
				formRowIdentifier.put("documentObjectId", rowIdentifier.getString("documentObjectId"));
				formRowIdentifier.put("sheetId", rowIdentifier.getString("sheetId"));
				formRowIdentifier.put("rowId", rowId);
				batchMessage.put("SKU", parentSKU + "-" + String.format("%02d", i+1));
				batchMessage.put("rowIdentifier", formRowIdentifier);
				if (!failureReason.isEmpty()) {
					batchMessage.put("status", "failure");
					batchMessage.put("failureReason", failureReason.replaceAll(requestType + " ", ""));
				} else {
					batchMessage.put("status", "success");
					if (exchange.getProperties().containsKey("isQuantityUpdated")
							&& !exchange.getProperty("isQuantityUpdated", boolean.class)) {
						batchMessage.put("warningMessage",
								"warningMessage : " + exchange.getProperty("quantityUpdateMessage"));
					}
				}
				rowId++;
				SKUMaps.add(batchMessage);
			}
		} else {
			if (!failureReason.isEmpty()) {
				SKUMap.put("status", "failure");
				SKUMap.put("failureReason", failureReason.replaceAll(requestType + " ", ""));
			} else {
				SKUMap.put("status", "success");
				if (exchange.getProperties().containsKey("isQuantityUpdated")
						&& !exchange.getProperty("isQuantityUpdated", boolean.class)) {
					SKUMap.put("warningMessage", "warningMessage : " + exchange.getProperty("quantityUpdateMessage"));
				}
			}
			SKUMaps.add(SKUMap);
		}
		log.debug("SKUMaps :" + SKUMaps);
		exchange.getOut().setBody(SKUMaps);
	}
}
