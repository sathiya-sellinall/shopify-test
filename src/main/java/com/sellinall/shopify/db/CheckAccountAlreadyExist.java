package com.sellinall.shopify.db;

import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.sellinall.database.DbUtilities;

/**
 * 
 * @author Raguvaran
 *
 */
public class CheckAccountAlreadyExist implements Processor {

	public void process(Exchange exchange) throws Exception {
		JSONObject inBody = exchange.getIn().getBody(JSONObject.class);
		exchange.setProperty("accountAlreadyExists", false);
		boolean isAccountExists = checkAccountAlreadyExists(inBody);
		if (isAccountExists) {
			exchange.setProperty("accountAlreadyExists", true);
			exchange.setProperty("failureReason", "Account already exists");
		}
	}

	private static boolean checkAccountAlreadyExists(JSONObject request) throws Exception {
		DBCollection table = DbUtilities.getDBCollection("accounts");
		BasicDBObject searchQuery = new BasicDBObject();
		BasicDBObject elemMatch = new BasicDBObject();
		elemMatch.put("postHelper.apiKey", request.getString("apiKey"));
		elemMatch.put("postHelper.pass", request.getString("password"));
		elemMatch.put("postHelper.URL", request.getString("webUrl"));
		searchQuery.put("shopify", new BasicDBObject("$elemMatch", elemMatch));
		DBCursor object = table.find(searchQuery);
		List<DBObject> accountList = object.toArray();
		if (accountList.size() > 0) {
			return true;
		}
		return false;
	}
}