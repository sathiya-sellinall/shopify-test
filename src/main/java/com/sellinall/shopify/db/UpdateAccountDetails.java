package com.sellinall.shopify.db;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import com.sellinall.database.DbUtilities;

/**
 * 
 * @author Raguvaran
 *
 */
public class UpdateAccountDetails implements Processor {
	static Logger log = Logger.getLogger(UpdateAccountDetails.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject request = exchange.getIn().getBody(JSONObject.class);
		if (exchange.getProperty("isValidAccount") != null && !exchange.getProperty("isValidAccount", Boolean.class)) {
			return;
		}
		DBObject update = new BasicDBObject();
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		DBObject searchQuery = new BasicDBObject();
		searchQuery.put("_id", new ObjectId(accountNumber));
		searchQuery.put("shopify" + ".nickName.id", request.getString("nickNameID"));
		update = constructUpdateObject(request, "shopify", exchange);
		if (update == null) {
			exchange.setProperty("failureReason", "Update failed");
			return;
		}
		DBCollection table = DbUtilities.getDBCollection("accounts");
		table.update(searchQuery, new BasicDBObject("$set", update));
	}

	private static BasicDBObject constructUpdateObject(JSONObject request, String channelName, Exchange exchange) {
		BasicDBObject update = new BasicDBObject();
		DBObject nickName = new BasicDBObject();
		try {
			update = new BasicDBObject();
			nickName.put("id", request.getString("nickNameID"));
			if (request.has("nickName")) {
				nickName.put("value", request.getString("nickName"));
				update.put(channelName + ".$.nickName", nickName);
			}
			if(request.has("enablePost")) {
			update.put(channelName + ".$.enablePost", request.getBoolean("enablePost"));
			}
			if (request.has("invoiceTemplate")) {
				update.put(channelName + ".$.invoiceTemplate", request.getString("invoiceTemplate"));
			}
			if (request.has("shippingCarrier") && request.get("shippingCarrier") instanceof JSONArray
					&& (request.getJSONArray("shippingCarrier")).length() > 0) {
				update.put(channelName + ".$.shippingCarrier",
						JSON.parse(request.getJSONArray("shippingCarrier").toString()));
			}
			if (request.has("wms") && request.get("wms") instanceof JSONArray
					&& (request.getJSONArray("wms")).length() > 0) {
				update.put(channelName + ".$.wms", JSON.parse(request.getJSONArray("wms").toString()));
			}
			if (request.has("profile") && !request.getString("profile").isEmpty()) {
				update.put(channelName + ".$.profile", request.getString("profile"));
			}
			if (exchange.getProperty("needToValidateAccount", Boolean.class)) {
				update.put(channelName + ".$.postHelper.apiKey", request.getString("apiKey"));
				update.put(channelName + ".$.postHelper.pass", request.getString("password"));
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.error("error occurred while updating an account");
			return null;
		}
		return update;
	}

}