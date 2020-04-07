package com.sellinall.shopify.response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.util.JSON;
import com.sellinall.config.Config;
import com.sellinall.util.CurrencyUtil;
import com.sellinall.util.enums.SIAInventoryStatus;
import com.sellinall.util.enums.SIAUnlinkedInventoryStatus;

public class ProcessUnlinkedInventory implements Processor {
	static Logger log = Logger.getLogger(ProcessUnlinkedInventory.class.getName());

	public void process(Exchange exchange) throws Exception {

		if (!exchange.getProperty("isImageUploadedSuccessfully", Boolean.class)) {
			int noOfItemSkipped = (Integer) exchange.getProperty("noOfItemSkipped");
			noOfItemSkipped = noOfItemSkipped + 1;
			exchange.setProperty("noOfItemSkipped", noOfItemSkipped);
			return;
		}

		BasicDBObject unlinkedInventory = new BasicDBObject();
		String merchantID = exchange.getProperty("merchantID", String.class);
		String unlinkedInventorySKU = exchange.getProperty("unlinkedInventorySKU", String.class);
		unlinkedInventory.put("SKU", unlinkedInventorySKU);
		unlinkedInventory.put("accountNumber", exchange.getProperty("accountNumber"));
		unlinkedInventory.put("merchantId", merchantID);
		if (exchange.getProperty("unlinkedInventoryStatus") == null) {
			unlinkedInventory.put("status", SIAUnlinkedInventoryStatus.UNLINKED.toString());
		} else {
			unlinkedInventory.put("status", exchange.getProperty("unlinkedInventoryStatus"));
		}
		if (exchange.getProperties().containsKey("autoLinkFailureReason")) {
			unlinkedInventory.put("failureReason", exchange.getProperty("autoLinkFailureReason"));
		}
		unlinkedInventory.put("date", System.currentTimeMillis() / 1000L);
		unlinkedInventory.put("noOfItemsold", 0);
		unlinkedInventory.put("noOfItemPending", 0);
		unlinkedInventory.put("noOfItemRefunded", 0);
		unlinkedInventory.put("noOfItemShipped", 0);
		unlinkedInventory.put("sync", true);
		BasicDBObject channel = new BasicDBObject();
		unlinkedInventory.put("itemTitle", exchange.getProperty("itemTitle"));
		unlinkedInventory.put("imageURL", Config.getConfig().getUploadImageUri() + merchantID + "/");		
		if (exchange.getProperties().containsKey("imageMap")) {
			BasicDBList imageURIList = getImageURIs(unlinkedInventorySKU, exchange);
			unlinkedInventory.put("imageURI", imageURIList);
			channel.put("imageURI", imageURIList);
		}
		unlinkedInventory.put("itemDescription", exchange.getProperty("itemDescription"));
		unlinkedInventory.put("site", "shopify");
		if (!exchange.getProperty("sellerSKU", String.class).isEmpty()
				&& !exchange.getProperty("hasVariations", Boolean.class)) {
			unlinkedInventory.put("customSKU", exchange.getProperty("sellerSKU", String.class));
		}
		channel.put("itemTitle", exchange.getProperty("itemTitle"));
		channel.put("nickNameID", exchange.getProperty("nickNameID", String.class));
		channel.put("status", SIAInventoryStatus.ACTIVE.toString());
		channel.put("itemDescription", exchange.getProperty("itemDescription"));
		BasicDBObject userShopify = (BasicDBObject) exchange.getProperty("userShopify");
		String siteURL = exchange.getProperty("shopifyURL", String.class);
		siteURL = siteURL + "/products/" + exchange.getProperty("url_key", String.class);
		channel.put("itemUrl", siteURL);

		int noOfItem = exchange.getProperty("noOfItem", Integer.class);
		unlinkedInventory.put("noOfItem", noOfItem);
		channel.put("noOfItem", noOfItem);
		if (exchange.getProperties().containsKey("itemAmount")) {
			long itemAmount = exchange.getProperty("itemAmount", Long.class);
			channel.put("itemAmount", CurrencyUtil.getAmountObject(itemAmount, userShopify.getString("currencyCode")));
		} else {
			channel.put("itemAmount", CurrencyUtil.getAmountObject(0, userShopify.getString("currencyCode")));
		}
		if (exchange.getProperties().containsKey("retailAmount")) {
			float retailAmount = exchange.getProperty("retailAmount", Float.class);
			channel.put("retailAmount", CurrencyUtil.getAmountObject(
					CurrencyUtil.convertAmountToSIAFormat(retailAmount), userShopify.getString("currencyCode")));
		}
		if(exchange.getProperties().containsKey("categoryID")){
			channel.put("categoryID", exchange.getProperty("categoryID", String.class));
		}
		if (exchange.getProperties().containsKey("categoryName") && exchange.getProperty("categoryName") != null) {
			channel.put("categoryName", exchange.getProperty("categoryName", String.class));
		}
		if (exchange.getProperties().containsKey("taxable")) {
			channel.put("taxable", exchange.getProperty("taxable", Boolean.class));
		}
		if (exchange.getProperties().containsKey("vendor")) {
			channel.put("vendor", exchange.getProperty("vendor", String.class));
		}
		if (exchange.getProperties().containsKey("productType")) {
			channel.put("productType", exchange.getProperty("productType", String.class));
		}
		if (exchange.getProperties().containsKey("weight")) {
			channel.put("weight", exchange.getProperty("weight", String.class));
		}
		if (exchange.getProperties().containsKey("barcode")) {
			channel.put("barcode", exchange.getProperty("barcode", String.class));
		}
		if (exchange.getProperties().containsKey("tags")) {
			channel.put("tags", JSON.parse(exchange.getProperty("tags", JSONArray.class).toString()));
		}
		channel.put("timeLastUpdated", System.currentTimeMillis() / 1000L);
		channel.put("refrenceID", exchange.getProperty("refrenceID").toString());
		if (!exchange.getProperty("hasVariations", Boolean.class)) {
			//For non-Variant only, we need to add variantRefrenceId & inventoryItemID.
			//For Variant, variantRefrenceId,inventoryItemID should be added only for child records.
			channel.put("variantRefrenceId", exchange.getProperty("variantRefrenceId").toString());
			if (exchange.getProperties().containsKey("inventoryItemID")) {
				String inventoryItemID = exchange.getProperty("inventoryItemID", String.class);
				channel.put("inventoryItemID", inventoryItemID);
				if (exchange.getProperty("inventoryIdAndQuantityMap") != null) {
					Map<String, Integer> inventoryIdAndQuantityMap = (Map<String, Integer>) exchange
							.getProperty("inventoryIdAndQuantityMap");
					if (inventoryIdAndQuantityMap.containsKey(inventoryItemID)) {
						int quantity = inventoryIdAndQuantityMap.get(inventoryItemID);
						unlinkedInventory.put("noOfItem", quantity);
						channel.put("noOfItem", quantity);
					}
				}
			}
		}
		if (!exchange.getProperty("refrenceSKU", String.class).isEmpty()
				&& !exchange.getProperty("hasVariations", Boolean.class)) {
			channel.put("refrenceSKU", exchange.getProperty("refrenceSKU", String.class));
		}
		if (exchange.getProperty("hasVariations", Boolean.class)) {
			List<BasicDBObject> variants = getVariants(exchange);
			channel.put("variants", variants);
			unlinkedInventory.put("variants", variants);
		}
		if (exchange.getProperties().containsKey("itemSpecifics")) {
			channel.put("itemSpecifics", exchange.getProperty("itemSpecifics", BasicDBList.class));
		}
		// To reset the fields
		exchange.removeProperty("sellerSKU");
		exchange.removeProperty("productType");
		exchange.removeProperty("tags");
		exchange.removeProperty("itemSpecifics");
		BasicDBList channelList = new BasicDBList();
		channelList.add(channel);
		unlinkedInventory.put("shopify", channelList);
		log.debug("Unlinked " + unlinkedInventory);
		exchange.getOut().setBody(unlinkedInventory);
	}

	private BasicDBList getImageURIs(String SKU, Exchange exchange) throws JSONException {
		BasicDBList imageURIList = new BasicDBList();
		ArrayList<String> parentImages = exchange.getProperty("parentImages", ArrayList.class);
		Map<String, String> imageMap = exchange.getProperty("imageMap", Map.class);
		for (String image : parentImages) {
			imageURIList.add("Shinmudra-" + SKU + "/" + imageMap.get(image));
		}
		return imageURIList;
	}

	private List<BasicDBObject> getVariants(Exchange exchange) throws JSONException {
		JSONArray variantOptions = exchange.getProperty("variantsOptions", JSONArray.class);
		List<BasicDBObject> variants = new ArrayList<BasicDBObject>();
		Map<Integer, String> variantNames = new HashMap<Integer, String>();
		for (int i = 0; i < variantOptions.length(); i++) {
			JSONObject option = variantOptions.getJSONObject(i);
			BasicDBObject SIAVariants = new BasicDBObject();
			List<String> names = new ArrayList<String>();
			for (int j = 0; j < option.getJSONArray("values").length(); j++) {
				names.add(option.getJSONArray("values").getString(j));
			}
			SIAVariants.put("title", option.getString("name"));
			SIAVariants.put("names", names);
			variants.add(SIAVariants);
			variantNames.put(option.getInt("position"), option.getString("name"));
		}
		exchange.setProperty("variantNamesMap", variantNames);
		return variants;
	}

}