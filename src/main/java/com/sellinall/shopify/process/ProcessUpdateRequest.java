package com.sellinall.shopify.process;

import java.util.ArrayList;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.sellinall.config.Config;
import com.sellinall.util.HttpsURLConnectionUtil;


public class ProcessUpdateRequest implements Processor {
	static Logger log = Logger.getLogger(ProcessUpdateRequest.class.getName());

	public void process(Exchange exchange) throws Exception {
		DBObject user = exchange.getProperty("UserDetails", DBObject.class);
		BasicDBObject inventory = exchange.getProperty("inventory", BasicDBObject.class);
		BasicDBObject postHelper = getPostHelper(user, exchange.getProperty("nickNameID", String.class));
		String ftlRequest = exchange.getIn().getBody(String.class);
		String url = createShopifyURL(postHelper, (BasicDBObject) inventory.get("shopify"));
		try {
			JSONObject responseObject = HttpsURLConnectionUtil.doPutWithAuth(url, postHelper.getString("apiKey"),
					postHelper.getString("pass"), new JSONObject(ftlRequest));
			log.debug("Shopify Update Item Response=" + responseObject);
			if (responseObject.has("headers")) {
				JSONObject headers = (JSONObject) responseObject.get("headers");
				if (headers.has("HTTP_X_SHOPIFY_SHOP_API_CALL_LIMIT")) {
					// TODO to clean up and put a permanent fix
					String limit = headers.getString("HTTP_X_SHOPIFY_SHOP_API_CALL_LIMIT").split("/")[0];
					if (Integer.parseInt(limit) >= 39) {
						log.info("Sleep time started for 10 seconds, due to free up the bucket size.");
						Thread.sleep(10000);
					}
				}
			}
			exchange.getOut().setBody(responseObject);
		} catch (Exception exception) {
			exception.printStackTrace();
			log.error("Update Item Failure : " + inventory.getString("SKU"));
		}
	}

	private BasicDBObject getPostHelper(DBObject user, String nickNameId) {
		ArrayList<BasicDBObject> shopifyList = (ArrayList<BasicDBObject>) user.get("shopify");
		for (BasicDBObject shopify : shopifyList) {
			BasicDBObject nickName = (BasicDBObject) shopify.get("nickName");
			if (nickName.getString("id").equals(nickNameId)) {
				return (BasicDBObject) shopify.get("postHelper");
			}
		}
		return null;
	}

	private String createShopifyURL(BasicDBObject shopify, BasicDBObject inventoryShopify) {
		String url = shopify.getString("URL");
		String refrenceID = "";
		String apiVersion = Config.getConfig().getApiVersion();
		if (inventoryShopify.containsKey("variantRefrenceId")) {
			refrenceID = inventoryShopify.getString("variantRefrenceId");
		} else {
			refrenceID = inventoryShopify.getString("refrenceID");
		}
		url = url + "/admin/variants/" + refrenceID + ".json";
		log.debug("Shopify url=" + url);
		return url;
	}
}