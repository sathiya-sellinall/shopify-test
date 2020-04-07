package com.sellinall.shopify.db;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import com.sellinall.database.DbUtilities;
import com.sellinall.util.DateUtil;

/**
 * 
 * @author Raguvaran
 *
 */
public class AddAccountDetails implements Processor {
	static Logger log = Logger.getLogger(AddAccountDetails.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject request = exchange.getIn().getBody(JSONObject.class);
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		DBObject searchQuery = new BasicDBObject();
		searchQuery.put("_id", new ObjectId(accountNumber));
		BasicDBObject channel = constructNewChannelObject(request);
		if (channel == null) {
			exchange.setProperty("failureReason", "Add account failed");
			return;
		}
		DBCollection table = DbUtilities.getDBCollection("accounts");
		table.update(searchQuery, new BasicDBObject("$push", channel));
	}

	private static BasicDBObject constructNewChannelObject(JSONObject request) {
		BasicDBObject channel = new BasicDBObject();
		BasicDBObject newChannel = new BasicDBObject();
		BasicDBObject newChannelShop = new BasicDBObject();
		DBObject nickName = new BasicDBObject();
		try {
			newChannelShop = (BasicDBObject) JSON.parse(request.toString());
			newChannel.put("shop", newChannelShop.get("shop"));
			nickName.put("id", request.getString("nickNameID"));
			nickName.put("value", request.getString("nickName"));
			newChannel.put("nickName", nickName);
			newChannel.put("apiKey", request.getString("apiKey"));
			newChannel.put("URL", request.getString("webUrl"));
			newChannel.put("invoiceTemplate", request.getString("invoiceTemplate"));
			JSONObject shop = request.getJSONObject("shop");
			newChannel.put("country", shop.getString("country_name"));
			newChannel.put("countryCode", shop.getString("country_code"));
			newChannel.put("currencyCode", shop.getString("currency"));
			DBObject postHelper = new BasicDBObject();
			postHelper.put("apiKey", request.getString("apiKey"));
			postHelper.put("pass", request.getString("password"));
			postHelper.put("URL", request.getString("webUrl"));
			newChannel.put("postHelper", postHelper);
			newChannel.put("enablePost", request.getBoolean("enablePost"));
			newChannel.put("lastScannedTime", DateUtil.getSIADateFormat());
			channel.put("shopify", newChannel);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("error occurred while creating an account");
			return null;
		}
		return channel;
	}
}