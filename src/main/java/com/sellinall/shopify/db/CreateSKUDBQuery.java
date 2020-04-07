/**
 * 
 */
package com.sellinall.shopify.db;

import java.util.regex.Pattern;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.mongodb.MongoDbConstants;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.sellinall.util.enums.SIAInventoryStatus;

/**
 * @author vikraman
 * 
 */
public class CreateSKUDBQuery implements Processor {
	static Logger log = Logger.getLogger(CreateSKUDBQuery.class.getName());

	public void process(Exchange exchange) throws Exception {

		DBObject outBody = createBody(exchange);
		exchange.getOut().setBody(outBody);
	}

	private DBObject createBody(Exchange exchange) throws JSONException {
		String nickNameID = "";
		JSONObject inBody = exchange.getIn().getBody(JSONObject.class);
		log.debug("CreateSKUDBQuery Received sku: " + inBody);
		String[] SKU = exchange.getProperty("SKU").toString().split("-");
		String accountNumber = (String) exchange.getProperty("accountNumber");

		BasicDBObject searchQuery = new BasicDBObject("accountNumber", accountNumber);
		log.debug("sku = " + SKU[0].toString());
		searchQuery.put("SKU", Pattern.compile(SKU[0] + ".*"));

		BasicDBObject projection = new BasicDBObject("shopify.$", 1);
		projection.put("SKU", 1);
		projection.put("itemTitle", 1);
		projection.put("itemDescription", 1);
		projection.put("imageURL", 1);
		projection.put("customSKU", 1);
		projection.put("variantDetails", 1);
		projection.put("variants", 1);
		log.debug("Fields : " + projection.toString());
		exchange.getOut().setHeader(MongoDbConstants.FIELDS_FILTER, projection);

		if (exchange.getProperties().containsKey("nickNameID")) {
			nickNameID = exchange.getProperty("nickNameID", String.class);
		} else if (exchange.getProperties().containsKey("siteNicknames")) {
			nickNameID = inBody.getString("nickNameID");
		}
		exchange.setProperty("nickNameID", nickNameID);
		BasicDBObject elemMatch = new BasicDBObject();
		elemMatch.put("nickNameID", nickNameID);
		if (exchange.getProperties().containsKey("requestType")
				&& (exchange.getProperty("requestType").equals("batchAddItem")
						|| exchange.getProperty("requestType").equals("batchEditItem"))) {
			elemMatch.put("status", new BasicDBObject("$ne", SIAInventoryStatus.REMOVED.toString()));
		}
		searchQuery.put(nickNameID.split("-")[0], new BasicDBObject("$elemMatch", elemMatch));
		log.debug("searchQuery : " + searchQuery.toString());
		return searchQuery;

	}
}