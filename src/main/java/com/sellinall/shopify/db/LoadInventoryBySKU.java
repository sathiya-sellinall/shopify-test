/**
 * 
 */
package com.sellinall.shopify.db;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.sellinall.database.DbUtilities;

/**
 * @author Ramchdanran.K
 * 
 */
public class LoadInventoryBySKU implements Processor {
	static Logger log = Logger.getLogger(LoadInventoryBySKU.class.getName());

	public void process(Exchange exchange) throws Exception {
		exchange.getOut().setBody(loadInventory(exchange));
	}

	private BasicDBObject loadInventory(Exchange exchange) throws JSONException {
		DBCollection table = DbUtilities.getInventoryDBCollection("inventory");
		String SKU = exchange.getProperty("SKU", String.class);
		String accountNumber = (String) exchange.getProperty("accountNumber");
		DBObject searchQuery = new BasicDBObject("accountNumber", accountNumber);
		BasicDBObject projection = new BasicDBObject();
		searchQuery.put("SKU", SKU);
		//For order sync cases, fieldFilter not required because need to update all shopify object.
		//For customSKU update, fieldFilter not required because need to update all shopify object.
		if (exchange.getProperties().containsKey("siteNicknames")
				&& !exchange.getProperty("isUpdateCustomSKU", Boolean.class)) {
			//For FE update cases we need to fetch only required channel object.
			JSONArray siteNicknames = exchange.getProperty("siteNicknames", JSONArray.class);
			String nickNameId = siteNicknames.getString(0);
			searchQuery.put("shopify.nickNameID", nickNameId);
			exchange.setProperty("nickNameID", nickNameId);
			projection.put("SKU", 1);
			projection.put("customSKU", 1);
			projection.put("shopify.$", 1);
		}
		return (BasicDBObject) table.findOne(searchQuery, projection);
	}
}