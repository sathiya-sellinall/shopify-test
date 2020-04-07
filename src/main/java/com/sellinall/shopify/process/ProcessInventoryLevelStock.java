package com.sellinall.shopify.process;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class ProcessInventoryLevelStock implements Processor {

	public void process(Exchange exchange) throws Exception {
		List<JSONObject> inventoryLevelsDetails = (List<JSONObject>) exchange.getProperty("inventoryLevelsDetails");
		List<String> activeLocationIdList = (List<String>) exchange.getProperty("activeLocationIdList");
		Map<String, Integer> inventoryIdAndQuantityMap = new LinkedHashMap<String, Integer>();
		Map<String, JSONObject> inventoryItemIdListMap = exchange.getProperty("inventoryItemIdListMap", Map.class);
		List<String> inventoryItemIdList = new ArrayList<>(inventoryItemIdListMap.keySet());
		for (String inventoryItemId : inventoryItemIdList) {
			int quantity = 0;
			JSONObject varaintObject = inventoryItemIdListMap.get(inventoryItemId);
			if (varaintObject.has("inventory_management") && !varaintObject.isNull("inventory_management")) {
				List<JSONObject> inventorylist = inventoryLevelsDetails.stream().filter(p -> {
					try {
						return p.getString("inventory_item_id").equals(inventoryItemId);
					} catch (JSONException e) {
						return false;
					}
				}).collect(Collectors.toList());
				for (JSONObject inventory : inventorylist) {
					if (activeLocationIdList.contains(inventory.getString("location_id"))) {
						quantity += inventory.getInt("available");
					}
				}
			} else {
				quantity = varaintObject.getInt("inventory_quantity");
			}
			inventoryIdAndQuantityMap.put(inventoryItemId, quantity);
		}
		exchange.setProperty("inventoryIdAndQuantityMap", inventoryIdAndQuantityMap);
	}

}
