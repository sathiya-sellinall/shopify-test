package com.sellinall.shopify.init;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;

public class InitializeUnlinkedInventoryRoute implements Processor {
	static Logger log = Logger.getLogger(InitializeUnlinkedInventoryRoute.class.getName());

	public void process(Exchange exchange) throws Exception {
		String inBody = exchange.getProperty("inBodyResponse").toString();
		JSONObject itemFromSite = new JSONObject(inBody);
		JSONArray variants =  itemFromSite.getJSONArray("variants");
		exchange.setProperty("variantsOptions", itemFromSite.getJSONArray("options"));
		exchange.setProperty("variantsDetails", itemFromSite.getJSONArray("variants"));
		JSONObject variantsDetails = (JSONObject) variants.get(0);
		String customSKU = "";
		if (!variantsDetails.isNull("sku") && !variantsDetails.getString("sku").isEmpty()) {
			customSKU = variantsDetails.getString("sku");
		}
		exchange.setProperty("sellerSKU", customSKU);
		exchange.setProperty("refrenceSKU", customSKU);
		exchange.getOut().setHeader("processItem", true);
		exchange.setProperty("itemTitle", itemFromSite.getString("title"));
		exchange.setProperty("url_key", itemFromSite.getString("handle"));
		String itemDescription = itemFromSite.getString("body_html");
		exchange.setProperty("itemDescription", itemDescription);
		if (exchange.getProperties().containsKey("extractItemSpecificsFromDescription")
				&& exchange.getProperty("extractItemSpecificsFromDescription", boolean.class)) {
			getItemSpecificsFromDescription(exchange, itemDescription);
		}
		exchange.setProperty("refrenceID", itemFromSite.get("id"));
		exchange.setProperty("variantRefrenceId", variantsDetails.getString("id"));
		if (variantsDetails.has("inventory_item_id")) {
			exchange.setProperty("inventoryItemID", variantsDetails.getString("inventory_item_id"));
		}
		String itemAmountStr = variantsDetails.getString("price");
		itemAmountStr = itemAmountStr.substring(0, itemAmountStr.indexOf(".") + 3);
		itemAmountStr = itemAmountStr.replace(".", "");
		exchange.setProperty("itemAmount", itemAmountStr);
		if (!variantsDetails.isNull("compare_at_price")) {
			float retailAmount = Float.parseFloat(variantsDetails.getString("compare_at_price"));
			exchange.setProperty("retailAmount", retailAmount);
		}
		if (!variantsDetails.isNull("taxable")) {
			exchange.setProperty("taxable", variantsDetails.getBoolean("taxable"));
		}
		if (!itemFromSite.isNull("vendor")) {
			exchange.setProperty("vendor", itemFromSite.getString("vendor"));
		}
		if (!itemFromSite.getString("product_type").equals("")) {
			exchange.setProperty("productType", itemFromSite.getString("product_type"));
		}
		String weight = variantsDetails.getString("weight");
		if (!weight.isEmpty()) {
			exchange.setProperty("weight", weight );
		}
		if (!itemFromSite.getString("tags").equals("")) {
			exchange.setProperty("tags", (JSONArray)constructTags(itemFromSite.getString("tags")));
		}
		String barcode = variantsDetails.getString("barcode");
		if (barcode != null && !barcode.isEmpty()) {
			exchange.setProperty("barcode", barcode);
		}
		int noOfItem = 0;
		Map <String,JSONObject> inventoryItemIdListMap = new HashMap<String,JSONObject>();
		for (int i = 0; i < variants.length(); i++) {
			JSONObject variantObj = variants.getJSONObject(i);
			inventoryItemIdListMap.put(variantObj.getString("inventory_item_id"), variantObj);
			noOfItem += variantObj.getInt("inventory_quantity");
		}
		exchange.setProperty("noOfItem", noOfItem);
		exchange.setProperty("inventoryItemIdListMap", inventoryItemIdListMap);
		if (exchange.getProperty("categoryMap") != null) {
			HashMap<String, String> categoryMap = exchange.getProperty("categoryMap", HashMap.class);
			if (categoryMap.containsKey("" + variantsDetails.get("product_id"))) {
				String categoryId = categoryMap.get("" + variantsDetails.get("product_id"));
				exchange.setProperty("categoryID", categoryId);
			}
		}
	}

	private void getItemSpecificsFromDescription(Exchange exchange, String desc) {
		try {
			Document document = Jsoup.parse(desc);
			Elements tableTags = document.getElementsByTag("table");
			BasicDBList itemSpecList = new BasicDBList();
			for (Element tableTag : tableTags) {
				Elements rows = tableTag.select("tr");
				for (Element row : rows) {
					Elements data = row.select("td");
					String title = data.get(0).text();
					String name = data.get(1).text();
					List<String> names = new ArrayList<String>();
					names.add(name);
					BasicDBObject itemSpec = new BasicDBObject();
					itemSpec.put("title", title);
					itemSpec.put("names", names);
					itemSpecList.add(itemSpec);
				}
			}
			if (itemSpecList.size() > 0) {
				exchange.setProperty("itemSpecifics", itemSpecList);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private JSONArray constructTags(String input) {
		String[] splitObj = input.split(",");
		JSONArray result = new JSONArray();
		for (int i = 0; i < splitObj.length; i++) {
			result.put(splitObj[i].trim());
		}
		return result;
	}
}