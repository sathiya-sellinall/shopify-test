package com.sellinall.shopify.response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.sellinall.config.Config;
import com.sellinall.shopify.util.ShopifyConnectionUtil;

public class ProcessPullInventory implements Processor {
	static Logger log = Logger.getLogger(ProcessPullInventory.class.getName());

	public void process(Exchange exchange) throws Exception {
		clearExchangeProperties(exchange);
		ArrayList<JSONObject> arrayList = (ArrayList<JSONObject>) exchange.getProperty("pulledInventoryList");
		BasicDBObject channelDBObj = (BasicDBObject) exchange.getProperty("userShopify");
		BasicDBObject postHelper = (BasicDBObject) channelDBObj.get("postHelper");
		JSONObject channelItemList = arrayList.get(exchange.getProperty("itemListIndex", Integer.class));
		exchange.setProperty("unlinkedInventory", channelItemList);
		String productId = channelItemList.getString("id");
		JSONObject variants = channelItemList.getJSONArray("variants").getJSONObject(0);
		exchange.setProperty("hasVariations", false);
		if (!variants.getString("title").equals("Default Title")) {
			exchange.setProperty("hasVariations", true);
		}

		JSONArray itemImages = channelItemList.getJSONArray("images");
		Set<String> imageSet = new HashSet<String>();
		ArrayList<String> parentImages = new ArrayList<String>();
		if (itemImages.length() == 0) {
			JSONObject imageResponse = getProductImage(productId, postHelper);
			if (imageResponse.has("payload")) {
				JSONObject imageResponsePayload = imageResponse.getJSONObject("payload");
				if (imageResponsePayload.has("images")) {
					itemImages = imageResponsePayload.getJSONArray("images");
				}
			}
			if (itemImages.length() == 0) {
				String getNoImageURL = Config.getConfig().getNoImageURL();
				imageSet.add(getNoImageURL);
				parentImages.add(getNoImageURL);
			}
		}
		HashMap<String, JSONArray> variantsImageMap = new HashMap<String, JSONArray>();
		String parentImage = "";
		for (int i = 0; i < itemImages.length(); i++) {
			JSONObject itemImage = itemImages.getJSONObject(i);
			log.debug("response images " + itemImage);
			imageSet.add(itemImage.getString("src"));
			JSONArray variantIDS = itemImage.getJSONArray("variant_ids");
			if (i == 0) {
				parentImage = itemImage.getString("src");
			}
			if (variantIDS.length() == 0
					|| (!exchange.getProperty("hasVariations", Boolean.class) && variantIDS.length() > 0)) {
				parentImages.add(itemImage.getString("src"));
			} else {
				variantsImageMap.put(itemImage.getString("src"), variantIDS);
			}
		}
		if (parentImages.isEmpty()) {
			parentImages.add(parentImage);
		}
		log.debug("imageSet" + imageSet.toString());
		exchange.setProperty("imageSet", imageSet);
		exchange.setProperty("variantImageMap", variantsImageMap);
		exchange.setProperty("parentImages", parentImages);
		exchange.getOut().setBody(channelItemList);
	}

	private void clearExchangeProperties(Exchange exchange) {
		exchange.removeProperty("hasVariations");
		exchange.removeProperty("imageSet");
		exchange.removeProperty("variantImageMap");
		exchange.removeProperty("parentImages");
		exchange.removeProperty("variantsOptions");
		exchange.removeProperty("variantsDetails");
		exchange.removeProperty("sellerSKU");
		exchange.removeProperty("refrenceSKU");
		exchange.removeProperty("itemTitle");
		exchange.removeProperty("itemDescription");
		exchange.removeProperty("refrenceID");
		exchange.removeProperty("variantRefrenceId");
		exchange.removeProperty("itemAmount");
		exchange.removeProperty("retailAmount");
		exchange.removeProperty("taxable");
		exchange.removeProperty("vendor");
		exchange.removeProperty("productType");
		exchange.removeProperty("tags");
		exchange.removeProperty("barcode");
		exchange.removeProperty("noOfItem");
		exchange.removeProperty("categoryID");
		exchange.removeProperty("weight");
		exchange.removeProperty("itemSpecifics");
		exchange.removeProperty("categoryName");
		exchange.removeProperty("imageMap");
		exchange.removeProperty("inventoryItemIdListMap");
	}

	public JSONObject getProductImage(String productId, BasicDBObject postHelper) throws JSONException {
		String url = postHelper.getString("URL") + "/admin/products/" + productId + "/images.json";
		String responseString = ShopifyConnectionUtil.doGetWithAuthorization(postHelper.getString("apiKey"),
				postHelper.getString("pass"), url);
		log.debug("Shopify Response=" + responseString);
		return new JSONObject(responseString);
	}
}