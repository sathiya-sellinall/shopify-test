package com.sellinall.shopify.init;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.sellinall.util.enums.SIAOrderStatus;

public class InitOrderUpdate implements Processor {

	@Override
	public void process(Exchange exchange) throws Exception {
		DBObject outBody = exchange.getProperty("UserDetails", DBObject.class);
		JSONObject order = (JSONObject) exchange.getProperty("order");
		SIAOrderStatus orderStatus = SIAOrderStatus.valueOf(order.getString("orderStatus"));
		exchange.setProperty("orderStatus", orderStatus.toString());
		if (outBody.containsField("shopify")) {
			BasicDBList shopifyAccountList = (BasicDBList) outBody.get("shopify");
			for (int i = 0; i < shopifyAccountList.size(); i++) {
				BasicDBObject shopifyAccount = (BasicDBObject) shopifyAccountList.get(i);
				BasicDBObject nickNameObject = (BasicDBObject) shopifyAccount.get("nickName");
				if (nickNameObject.getString("id").equals(exchange.getProperty("nickNameID", String.class))) {
					exchange.setProperty("postHelper", (BasicDBObject) shopifyAccount.get("postHelper"));
					break;
				}
			}
		}

	}

}
