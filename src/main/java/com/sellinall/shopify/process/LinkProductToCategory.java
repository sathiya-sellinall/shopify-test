package com.sellinall.shopify.process;

import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.jetty.http.HttpStatus;

import com.mongodb.BasicDBObject;
import com.sellinall.config.Config;
import com.sellinall.util.HttpsURLConnectionUtil;

public class LinkProductToCategory implements Processor {
	static Logger log = Logger.getLogger(LinkProductToCategory.class.getName());

	public void process(Exchange exchange) throws Exception {
		@SuppressWarnings("unchecked")
		List<BasicDBObject> updateObjectList = exchange.getProperty("updateObjectList", List.class);
		exchange.getOut().setBody(processLinkProductToCategory(exchange, updateObjectList));

	}

	private List<BasicDBObject> processLinkProductToCategory(Exchange exchange, List<BasicDBObject> updateObjectList) {
		try {
			JSONObject payload = new JSONObject();
			JSONObject collect = new JSONObject();
			collect.put("product_id", exchange.getProperty("productId", String.class));
			collect.put("collection_id", exchange.getProperty("categoryId", String.class));
			payload.put("collect", collect);
			BasicDBObject userShopify = (BasicDBObject) exchange.getProperty("userShopify");
			BasicDBObject postHelper = (BasicDBObject) userShopify.get("postHelper");
			JSONObject responseString = HttpsURLConnectionUtil.doPostWithAuth(createShopifyURL(userShopify, exchange),
					postHelper.getString("apiKey"), postHelper.getString("pass"), payload);
			log.debug("Shopify Response=" + responseString);
			if (responseString.getInt("httpCode") != HttpStatus.CREATED_201) {
				log.error("Failed to link this product to the given category");
				exchange.setProperty("failureReason", "Link category failed");
				for (int i = 0; i < updateObjectList.size(); i++) {
					BasicDBObject updateInventoryObject = updateObjectList.get(i);
					updateInventoryObject.put("shopify.$.failureReason", "Link category failed");
				}
			}
		} catch (Exception e) {
			log.error("Internal server error, while linking this product to the given category");
			exchange.setProperty("failureReason", "Intenal server error");
			for (int i = 0; i < updateObjectList.size(); i++) {
				BasicDBObject updateInventoryObject = updateObjectList.get(i);
				updateInventoryObject.put("shopify.$.failureReason", "Intenal server error");
			}
		}
		return updateObjectList;
	}

	private String createShopifyURL(BasicDBObject shopify, Exchange exchange) {
		BasicDBObject postHelper = (BasicDBObject) shopify.get("postHelper");
		String apiVersion = Config.getConfig().getApiVersion();
		String url = postHelper.getString("URL");
		url = url + "/admin/api/" + apiVersion + "/collects.json";
		log.debug("Shopify url=" + url);
		return url;
	}

}