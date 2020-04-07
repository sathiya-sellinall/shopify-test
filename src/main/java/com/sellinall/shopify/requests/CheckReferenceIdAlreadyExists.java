package com.sellinall.shopify.requests;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.sellinall.database.DbUtilities;

/**
 * 
 * @author Raguvaran
 *
 */

public class CheckReferenceIdAlreadyExists implements Processor {
	static Logger log = Logger.getLogger(CheckReferenceIdAlreadyExists.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject inBody = new JSONObject(exchange.getProperty("rawData").toString());
		String referenceId = inBody.getString("id");
		String nickNameId = exchange.getProperty("nickNameID", String.class);
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		exchange.setProperty("isItemAlreadyPosted", true);
		if (runQuery(referenceId, nickNameId, accountNumber) == null) {
			exchange.setProperty("isItemAlreadyPosted", false);
			log.info("Item not exists in our database");
		}
	}

	public DBObject runQuery(String referenceId, String nickNameId, String accountNumber) {
		DBCollection table = DbUtilities.getInventoryDBCollection("inventory");
		BasicDBObject searchQuery = new BasicDBObject();
		BasicDBObject elemMatch = new BasicDBObject();
		elemMatch.put("nickNameID", nickNameId);
		elemMatch.put("refrenceID", referenceId);
		BasicDBObject shopify = new BasicDBObject("$elemMatch", elemMatch);
		searchQuery.put("accountNumber", accountNumber);
		searchQuery.put("shopify", shopify);
		BasicDBObject filter = new BasicDBObject();
		filter.put("shopify.refrenceID", 1);
		filter.put("_id", 0);
		DBObject result = table.findOne(searchQuery, filter);
		return result;
	}
}
