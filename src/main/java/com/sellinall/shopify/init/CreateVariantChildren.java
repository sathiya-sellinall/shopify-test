package com.sellinall.shopify.init;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.sellinall.config.Config;
import com.sellinall.util.CurrencyUtil;
import com.sellinall.util.enums.SIAInventoryStatus;
import com.sellinall.util.enums.SIAUnlinkedInventoryStatus;

public class CreateVariantChildren implements Processor {

	static Logger log = Logger.getLogger(CreateVariantChildren.class.getName());

	public void process(Exchange exchange) throws Exception {
		exchange.getOut().setBody(processVariants(exchange));
	}

	private ArrayList<BasicDBObject> processVariants(Exchange exchange) throws JSONException {
		JSONArray variantDetailsFromSite = exchange.getProperty("variantsDetails", JSONArray.class);
		HashMap<String, String> categoryMap = exchange.getProperty("categoryMap", HashMap.class);
		String SKU = exchange.getProperty("unlinkedInventorySKU", String.class);
		String[] unlinkedSKU = SKU.split("-"); // U-AUB0009-01
		SKU = unlinkedSKU[0] + "-" + unlinkedSKU[1];
		String merchantID = exchange.getProperty("merchantID", String.class);
		ArrayList<BasicDBObject> inventoryList = new ArrayList<BasicDBObject>();
		for (int i = 0; i < variantDetailsFromSite.length(); i++) {
			String variantSKU = SKU + "-" + String.format("%02d", i + 1);
			JSONObject variant = variantDetailsFromSite.getJSONObject(i);
			ArrayList<BasicDBObject> channelObj = new ArrayList<BasicDBObject>();
			BasicDBList variantDetails = constructVariantDetails(exchange, variant);
			String categoryID = "";
			if (categoryMap.containsKey("" + variant.get("product_id"))) {
				categoryID = categoryMap.get("" + variant.get("product_id"));
			}
			channelObj.add(0,
					constructChannel(variant, exchange, variantDetails, merchantID, variantSKU, categoryID));
			BasicDBObject inventory = getInventoryDetails(exchange, SKU, variantSKU);
			inventory.put("variantDetails", variantDetails);
			if (variant.has("inventory_item_id") && exchange.getProperty("inventoryIdAndQuantityMap") != null) {
				Map<String, Integer> inventoryIdAndQuantityMap = (Map<String, Integer>) exchange
						.getProperty("inventoryIdAndQuantityMap");
				if (inventoryIdAndQuantityMap.containsKey(variant.getString("inventory_item_id"))) {
					int quantity = inventoryIdAndQuantityMap.get(variant.getString("inventory_item_id"));
					inventory.put("noOfItem", quantity);
				}
			} else {
				inventory.put("noOfItem", variant.getInt("inventory_quantity"));
			}
			inventory.put("shopify", channelObj);
			inventory.put("imageURI", channelObj.get(0).get("imageURI"));
			if (channelObj.get(0).containsField("refrenceSKU")) {
				inventory.put("customSKU", channelObj.get(0).get("refrenceSKU"));
			}
			inventoryList.add(inventory);
		}
		return inventoryList;
	}

	private BasicDBObject getInventoryDetails(Exchange exchange, String parentSKU, String variantSKU)
			throws JSONException {
		BasicDBObject unlinkedInventory = new BasicDBObject();
		String merchantID = exchange.getProperty("merchantID", String.class);
		unlinkedInventory.put("SKU", variantSKU);
		unlinkedInventory.put("accountNumber", exchange.getProperty("accountNumber"));
		unlinkedInventory.put("merchantId", merchantID);
		unlinkedInventory.put("status", SIAUnlinkedInventoryStatus.UNLINKED.toString());
		unlinkedInventory.put("date", System.currentTimeMillis() / 1000L);
		unlinkedInventory.put("noOfItemsold", 0);
		unlinkedInventory.put("noOfItemPending", 0);
		unlinkedInventory.put("noOfItemRefunded", 0);
		unlinkedInventory.put("noOfItemShipped", 0);
		unlinkedInventory.put("sync", true);
		unlinkedInventory.put("itemTitle", exchange.getProperty("itemTitle"));
		unlinkedInventory.put("imageURL", Config.getConfig().getUploadImageUri() + merchantID + "/");
		
		unlinkedInventory.put("itemDescription", exchange.getProperty("itemDescription"));
		unlinkedInventory.put("site", "shopify");
		return unlinkedInventory;
	}

	private BasicDBList getImageURIs(String image, String SKU) throws JSONException {
		BasicDBList imageURIList = new BasicDBList();
		if (!image.isEmpty()) {
			imageURIList.add("Shinmudra-" + SKU.split("-")[0] + "-" + SKU.split("-")[1] + "/" + image);
		}
		return imageURIList;
	}

	private BasicDBList constructVariantDetails(Exchange exchange, JSONObject variant) throws JSONException {
		Map<Integer, String> varaintsNamesMap = exchange.getProperty("variantNamesMap", Map.class);
		BasicDBList variantDetails = new BasicDBList();
		Iterator it = varaintsNamesMap.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pair = (Map.Entry) it.next();
			BasicDBObject variantOption = new BasicDBObject();
			variantOption.put("title", pair.getValue());
			variantOption.put("name", variant.getString("option" + pair.getKey()));
			variantDetails.add(variantOption);
		}
		return variantDetails;
	}

	private BasicDBObject constructChannel(JSONObject variant, Exchange exchange, BasicDBList variantDetails,
			String merchantID, String SKU, String categoryID) throws JSONException {
		BasicDBObject channel = new BasicDBObject();
		channel.put("refrenceID", variant.getString("product_id"));
		channel.put("variantRefrenceId", variant.getString("id"));
		if (variant.has("inventory_item_id")) {
			channel.put("inventoryItemID", variant.getString("inventory_item_id"));
		}
		channel.put("nickNameID", exchange.getProperty("nickNameID", String.class));
		if (!variant.isNull("sku") && !variant.getString("sku").isEmpty()) {
			channel.put("refrenceSKU", variant.getString("sku"));
		}
		float itemAmount = Float.parseFloat(variant.getString("price"));
		BasicDBObject userShopify = (BasicDBObject) exchange.getProperty("userShopify");
		long amount = CurrencyUtil.convertAmountToSIAFormat(itemAmount);
		channel.put("itemAmount", CurrencyUtil.getAmountObject(amount, userShopify.getString("currencyCode")));
		if (!variant.isNull("compare_at_price")) {
			float retailAmount = Float.parseFloat(variant.getString("compare_at_price"));
			channel.put("retailAmount", CurrencyUtil.getAmountObject(
					CurrencyUtil.convertAmountToSIAFormat(retailAmount), userShopify.getString("currencyCode")));
		}
		channel.put("variantDetails", variantDetails);
		if (variant.has("inventory_item_id") && exchange.getProperty("inventoryIdAndQuantityMap") != null) {
			Map<String, Integer> inventoryIdAndQuantityMap = (Map<String, Integer>) exchange
					.getProperty("inventoryIdAndQuantityMap");
			if (inventoryIdAndQuantityMap.containsKey(variant.getString("inventory_item_id"))) {
				int quantity = inventoryIdAndQuantityMap.get(variant.getString("inventory_item_id"));
				channel.put("noOfItem", quantity);
			}
		} else {
			channel.put("noOfItem", variant.getInt("inventory_quantity"));
		}
		channel.put("taxable", variant.getBoolean("taxable"));
		String barcode = variant.getString("barcode");
		if (barcode != null && !barcode.isEmpty()) {
			channel.put("barcode", barcode);
		}

		String weight = variant.getString("weight");
		if (!weight.isEmpty()) {
			channel.put("weight", weight);
		}

		channel.put("imageURL", Config.getConfig().getUploadImageUri() + merchantID + "/");
		if (categoryID != null && !categoryID.trim().isEmpty()) {
			channel.put("categoryID", categoryID);
		}
		if (exchange.getProperties().containsKey("imageMap")) {
			BasicDBList imageURIList = getImageURIs(getVariantImage(variant.getString("id"), exchange), SKU);
			channel.put("imageURI", imageURIList);
		}
		channel.put("status", SIAInventoryStatus.ACTIVE.toString());
		String siteURL = exchange.getProperty("shopifyURL", String.class);
		siteURL = siteURL + "/products/" + exchange.getProperty("url_key", String.class);
		if (siteURL.equals("")) {
			channel.put("itemUrl", "NA");
			// Item Waiting for Approval
			channel.put("status", SIAInventoryStatus.INITIATED.toString());
		} else {
			channel.put("itemUrl", siteURL);
		}
		channel.remove("variants");
		return channel;
	}
	
	private String getVariantImage(String variantID, Exchange exchange) {
		HashMap<String, JSONArray> variantsImageMap = exchange.getProperty("variantImageMap", HashMap.class);
		Map<String, String> SIAUploadedImageMap = exchange.getProperty("imageMap", HashMap.class);
		Iterator it = variantsImageMap.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pair = (Map.Entry)it.next();
			JSONArray variantIDs = (JSONArray) pair.getValue();
			if(variantIDs.toString().contains(variantID)){
				return SIAUploadedImageMap.get((String) pair.getKey());
			}
		}
		return "";
	}

}