/**
 * 
 */
package com.sellinall.shopify.db;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.bson.types.ObjectId;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.sellinall.database.DbUtilities;

/**
 * @author vikraman
 *
 */
public class UserDBQuery implements Processor {

	public void process(Exchange exchange) throws Exception {
		JSONObject inBody = exchange.getIn().getBody(JSONObject.class);
		exchange.getOut().setBody(inBody);
		DBObject outBody = createBody(exchange);
		exchange.setProperty("merchantID", outBody.get("merchantID"));
		exchange.setProperty("UserDetails", outBody);
	}

	private DBObject createBody(Exchange exchange) {
		String accountNumber = (String) exchange.getProperty("accountNumber");
		DBCollection table = DbUtilities.getDBCollection("accounts");
		BasicDBObject searchQuery = new BasicDBObject();
		ObjectId objid = new ObjectId(accountNumber);
		searchQuery.put("_id", objid);
		DBObject object = table.findOne(searchQuery);
		return object;
	}
}