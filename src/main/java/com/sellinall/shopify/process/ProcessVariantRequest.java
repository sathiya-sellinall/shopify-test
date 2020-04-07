package com.sellinall.shopify.process;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.sellinall.util.HttpsURLConnectionUtil;

public class ProcessVariantRequest implements Processor {
	static Logger log = Logger.getLogger(ProcessVariantRequest.class.getName());

	public void process(Exchange exchange) throws Exception {
		BasicDBObject userShopify = (BasicDBObject) exchange.getProperty("userShopify");
		BasicDBObject postHelper = (BasicDBObject) userShopify.get("postHelper");
		String ftlRequest = exchange.getIn().getBody(String.class);
		String url = createShopifyURL(userShopify, exchange);
		String responseString = HttpsURLConnectionUtil.doPostWithAuth(url, postHelper.getString("apiKey"),
				postHelper.getString("pass"), new JSONObject(ftlRequest)).getString("payload");
		log.debug("Shopify Response=" + responseString);
		exchange.getOut().setBody(responseString);
	}

	private String createShopifyURL(BasicDBObject shopify, Exchange exchange) {
		BasicDBObject postHelper = (BasicDBObject) shopify.get("postHelper");
		String url = postHelper.getString("URL");
		url = url + getVariantPostURL(exchange);
		log.debug("Shopify url=" + url);
		return url;
	}

	private String getVariantPostURL(Exchange exchange) {
		String url = "/admin/products/";
		BasicDBObject inventory = (BasicDBObject) exchange.getProperty("inventory");
		BasicDBObject inventoryShopify = (BasicDBObject) inventory.get("shopify");
		BasicDBObject parentInventory = (BasicDBObject) inventoryShopify.get("parentShopify");
		url = url + parentInventory.getString("refrenceID");
		return url + "/variants.json";
	}
}