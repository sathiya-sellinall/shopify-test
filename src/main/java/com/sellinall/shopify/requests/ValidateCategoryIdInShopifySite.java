package com.sellinall.shopify.requests;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.sellinall.config.Config;
import com.sellinall.shopify.util.ShopifyConnectionUtil;
import com.sellinall.util.enums.SIAInventoryStatus;
import com.sellinall.util.enums.SIAInventoryUpdateStatus;

public class ValidateCategoryIdInShopifySite implements Processor {
	static Logger log = Logger.getLogger(ValidateCategoryIdInShopifySite.class.getName());

	public void process(Exchange exchange) {
		String categoryID = exchange.getProperty("categoryId", String.class);
		BasicDBObject userShopify = (BasicDBObject) exchange.getProperty("userShopify");
		BasicDBObject postHelper = (BasicDBObject) userShopify.get("postHelper");
		try {
			String responseString = ShopifyConnectionUtil.doGetWithAuthorization(postHelper.getString("apiKey"),
					postHelper.getString("pass"), createShopifyURL(userShopify, exchange) + "/" + categoryID + ".json");
			log.debug("Shopify Response=" + responseString);
			JSONObject response = new JSONObject(responseString);
			if (response.get("httpCode").equals(404)) {
				exchange.setProperty("isValidCategoryId", false);
				exchange.setProperty("failureReason", "The given category doesn't exist.");
				BasicDBObject updateInventoryObject = new BasicDBObject();
				String requestType = exchange.getProperty("requestType",String.class);
				if (requestType.equals("addItem") || requestType.equals("batchAddItem")) {
					updateInventoryObject.put("shopify.$.status", SIAInventoryStatus.FAILED.toString());
				} else if (requestType.equals("batchEditItem")) {
					updateInventoryObject.put("shopify.$.updateStatus", SIAInventoryUpdateStatus.FAILED.toString());
				}
				updateInventoryObject.put("shopify.$.failureReason", "The given category doesn't exist.");
				updateInventoryObject.put("SKU", exchange.getProperty("SKU"));
				exchange.getOut().setBody(updateInventoryObject);

			}
		} catch (Exception e) {
			log.error("Internal server error, while validating this given category");
			exchange.setProperty("failureReason", "Internal error");
		}
	}

	private String createShopifyURL(BasicDBObject shopify, Exchange exchange) {
		BasicDBObject postHelper = (BasicDBObject) shopify.get("postHelper");
		String url = postHelper.getString("URL");
		url = url + "/admin/api/" + Config.getConfig().getApiVersion() + "/custom_collections";
		log.debug("Shopify url=" + url);
		return url;
	}
}
