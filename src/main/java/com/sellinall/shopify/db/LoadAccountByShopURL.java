package com.sellinall.shopify.db;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.sellinall.database.DbUtilities;

public class LoadAccountByShopURL implements Processor {
	static Logger log = Logger.getLogger(LoadAccountByShopURL.class.getName());

	public void process(Exchange exchange) throws Exception {
		DBCollection table = DbUtilities.getDBCollection("accounts");
		exchange.setProperty("hasShippingCarrier", false);
		BasicDBObject searchQuery = new BasicDBObject();
		JSONObject inBody = new JSONObject(exchange.getProperty("rawData").toString());
		exchange.setProperty("hasUserDBRecord", true);
		if(exchange.getProperty("shopUrl") == null){
			log.info("Shop URL not found, find the root cause for this record below");
			exchange.setProperty("hasUserDBRecord", false);
			return;
		}
		searchQuery.put("shopify.shop.myshopify_domain", exchange.getProperty("shopUrl").toString());
		log.info("shopUrl in searchQuery " + exchange.getProperty("shopUrl").toString());
		BasicDBObject field = new BasicDBObject("shopify.$", 1);
		DBObject object = table.findOne(searchQuery, field);
		if (object != null) {
			String accountNumber = ((ObjectId) object.get("_id")).toString();
			log.info("account Number: " + accountNumber);
			List<BasicDBObject> shopifyList = (List<BasicDBObject>) object.get("shopify");
			exchange.setProperty("accountNumber", accountNumber);
			BasicDBObject shopify = (BasicDBObject) shopifyList.get(0);
			BasicDBObject nickNameId = (BasicDBObject) shopify.get("nickName");
			exchange.setProperty("nickNameID", nickNameId.getString("id"));
			if (shopify.containsField("shippingCarrier")) {
				checkChannelHasShippingCarrier(exchange, shopify);
			}
		} else {
			log.info("User doesn't exists in our database");
			return;
		}
	}

	private void checkChannelHasShippingCarrier(Exchange exchange, BasicDBObject shopify) {
		boolean hasShippingCarrier = false;
		List<String> shippingCarriers = (ArrayList<String>) shopify.get("shippingCarrier");
		for (String shippingCarrier : shippingCarriers) {
			if (!shippingCarrier.isEmpty()) {
				hasShippingCarrier = true;
				break;
			}
		}
		if (hasShippingCarrier) {
			exchange.setProperty("stopProcess", true);
			if (exchange.getProperty("actionName").equals("products")
					|| (exchange.getProperties().containsKey("actionType")
							&& (exchange.getProperty("actionType").equals("create")
									|| exchange.getProperty("actionType").equals("paid")))) {
				exchange.setProperty("stopProcess", false);
			}
		}
		exchange.setProperty("hasShippingCarrier", hasShippingCarrier);
	}
}