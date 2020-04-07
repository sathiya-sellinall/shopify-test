package com.sellinall.shopify.message;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;

public class ConstructStockUpdatePayload implements Processor {
	static Logger log = Logger.getLogger(ConstructStockUpdatePayload.class.getName());

	@Override
	public void process(Exchange exchange) throws Exception {

		String accountNumber = exchange.getProperty("accountNumber", String.class);
		String nickNameID = exchange.getProperty("nickNameID", String.class);
		String SKU = exchange.getProperty("SKU", String.class);
		Map<String, BasicDBObject> warehouseLocationMap = exchange.getProperty("warehouseLocationMap", Map.class);
		// For single warehouse directly set quantity
		BasicDBObject shopifyObject = (BasicDBObject) exchange.getProperty("inventory", BasicDBObject.class)
				.get("shopify");
		ArrayList<JSONObject> stockUpdatePayloadList = new ArrayList<JSONObject>();
		String inventoryItemID = shopifyObject.getString("inventoryItemID");
		if (warehouseLocationMap.size() == 1 || !exchange.getProperty("isQuantityUpdateByNewOrder", Boolean.class)) {
			stockUpdatePayloadList = getSingleWarehousePayload(shopifyObject, inventoryItemID, warehouseLocationMap);
		} else {
			if (exchange.getProperties().containsKey("inventoryLevelsDetails")) {
				List<JSONObject> inventoryList = (List<JSONObject>) exchange.getProperty("inventoryLevelsDetails");
				stockUpdatePayloadList = getMultiWarehousePayload(exchange, inventoryList, shopifyObject,
						inventoryItemID, warehouseLocationMap, accountNumber, nickNameID, SKU);
			}
		}
		exchange.setProperty("stockUpdatePayloadList", stockUpdatePayloadList);
	}

	private ArrayList<JSONObject> getSingleWarehousePayload(BasicDBObject shopifyObject, String inventoryItemID,
			Map<String, BasicDBObject> warehouseLocationMap) throws JSONException {
		ArrayList<JSONObject> arrayList = new ArrayList<JSONObject>();
		JSONObject payloadObject = new JSONObject();
		payloadObject.put("locationID", warehouseLocationMap.keySet().toArray()[0].toString());
		payloadObject.put("available", shopifyObject.getInt("noOfItem"));
		payloadObject.put("inventoryItemID", inventoryItemID);
		arrayList.add(payloadObject);
		return arrayList;
	}

	private ArrayList<JSONObject> getMultiWarehousePayload(Exchange exchange, List<JSONObject> inventoryList,
			BasicDBObject shopifyObject, String inventoryItemID, Map<String, BasicDBObject> warehouseLocationMap,
			String accountNumber, String SKU, String nickNameID) throws JSONException {
		int inventoryCount = 0;
		ArrayList<JSONObject> arrayList = new ArrayList<JSONObject>();
		Map<Integer, JSONObject> shopifyQuantityMap = new TreeMap<Integer, JSONObject>();
		for (JSONObject inventory : inventoryList) {
			String locationId = inventory.getString("location_id");
			if (warehouseLocationMap.containsKey(locationId)) {
				BasicDBObject warehouseObject = warehouseLocationMap.get(locationId);
				if (warehouseObject.containsKey("priority")) {
					inventoryCount = inventoryCount + inventory.getInt("available");
					shopifyQuantityMap.put(warehouseObject.getInt("priority"), inventory);
				}
			}
		}
		int siashopifyQuantity = shopifyObject.getInt("noOfItem");
		int quantityDif = siashopifyQuantity - inventoryCount;
		int index = 0;
		int remainQuantity = 0;
		// decrease quantity
		if (quantityDif <= 0) {
			for (int key : shopifyQuantityMap.keySet()) {
				JSONObject payloadObject = new JSONObject();
				JSONObject inventory = shopifyQuantityMap.get(key);
				int shopifyQuantity = inventory.getInt("available");
				if (index == 0) {
					remainQuantity = shopifyQuantity - Math.abs(quantityDif);
				} else {
					remainQuantity = shopifyQuantity - remainQuantity;
				}
				if (remainQuantity >= 0) {
					payloadObject.put("locationID", inventory.get("location_id"));
					payloadObject.put("locationID", inventory.get("location_id"));
					payloadObject.put("inventoryItemID", inventoryItemID);
					payloadObject.put("available", remainQuantity);
					arrayList.add(payloadObject);
					break;
				} else {
					payloadObject.put("locationID", inventory.get("location_id"));
					payloadObject.put("available", 0);
					payloadObject.put("inventoryItemID", inventoryItemID);
					remainQuantity = Math.abs(remainQuantity);
				}
				arrayList.add(payloadObject);
				index++;
			}
		} else {
			// increase quantity
			for (int key : shopifyQuantityMap.keySet()) {
				JSONObject payloadObject = new JSONObject();
				JSONObject inventory = shopifyQuantityMap.get(key);
				int shopifyQuantity = inventory.getInt("available");
				if (shopifyQuantity > 0) {
					payloadObject.put("locationID", inventory.get("location_id"));
					payloadObject.put("available", shopifyQuantity + quantityDif);
					payloadObject.put("inventoryItemID", inventoryItemID);
					arrayList.add(payloadObject);
				} else {
					log.error(
							"Unable to update quantity  because first location inventory count is lessthan zero for inventoryItemID-"
									+ inventoryItemID + ",accountNumber-" + accountNumber + ",nickNameID-" + nickNameID
									+ ",SKU-" + SKU);
				}
				break;
			}
		}
		return arrayList;
	}

}
