package com.sellinall.shopify.process;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.sellinall.config.Config;
import com.sellinall.util.ContentChangesUtil;
import com.sellinall.util.HttpsURLConnectionUtil;

public class ProcessAddItemRequest implements Processor {
	static Logger log = Logger.getLogger(ProcessAddItemRequest.class.getName());

	public void process(Exchange exchange) throws Exception {
		BasicDBObject userShopify = (BasicDBObject) exchange.getProperty("userShopify");
		BasicDBObject postHelper = (BasicDBObject) userShopify.get("postHelper");
		String ftlRequest = exchange.getIn().getBody(String.class);
		JSONObject payload = new JSONObject(ftlRequest);
		itemDescriptionContentChanges(payload);
		String referenceId = "";
		String url = "";
		JSONObject response = new JSONObject();
		JSONObject product = payload.getJSONObject("product");
		if (product.has("id")) {
			referenceId = product.getString("id");
			url = createShopifyURL(userShopify, exchange, referenceId);
			response = HttpsURLConnectionUtil
					.doPutWithAuth(url, postHelper.getString("apiKey"), postHelper.getString("pass"), payload);
		} else {
			url = createShopifyURL(userShopify, exchange, referenceId);
			response = HttpsURLConnectionUtil
					.doPostWithAuth(url, postHelper.getString("apiKey"), postHelper.getString("pass"), payload);
		}
		log.debug("Shopify Response=" + response.toString());
		exchange.getOut().setBody(response);
	}

	private String createShopifyURL(BasicDBObject shopify, Exchange exchange, String referenceId) {
		BasicDBObject postHelper = (BasicDBObject) shopify.get("postHelper");
		String url = postHelper.getString("URL");
		String apiVersion = Config.getConfig().getApiVersion();
		if (!referenceId.isEmpty()) {
			url = url + "/admin/api/" + apiVersion + "/products/" + referenceId + ".json";
		} else {
			url = url + "/admin/api/" + apiVersion + "/products.json";
		}
		log.debug("Shopify url=" + url);
		return url;
	}
	
	private void itemDescriptionContentChanges(JSONObject payload){
		try{
			JSONObject product = payload.getJSONObject("product");
			String itemDescription = product.getString("body_html");
			itemDescription = itemDescription.replaceAll("[^\\x00-\\x7F]", "");
			product.put("body_html", itemDescription);
		}catch (Exception e) {
			
		}
	}

}