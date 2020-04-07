/**
 * 
 */
package com.sellinall.shopify.db;

import java.util.regex.Pattern;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.mongodb.MongoDbConstants;
import org.apache.log4j.Logger;

import com.mongodb.BasicDBObject;
import com.mongodb.WriteConcern;

/**
 * @author Malli
 * 
 */
public class UpdateVariantParentSKUDBQuery implements Processor {
	static Logger log = Logger.getLogger(UpdateVariantParentSKUDBQuery.class.getName());

	public void process(Exchange exchange) throws Exception {
		Object[] outBody = createBody(exchange);
		exchange.getOut().setBody(outBody);
	}

	private Object[] createBody(Exchange exchange) {
		BasicDBObject updateObject = (BasicDBObject) exchange.getIn().getBody();
		String SKU = updateObject.getString("SKU");
		updateObject.remove("SKU");
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("accountNumber", exchange.getProperty("accountNumber"));
		if (exchange.getProperties().containsKey("failureReason")
				&& !exchange.getProperty("failureReason", String.class).isEmpty()) {
			searchQuery.put("SKU", Pattern.compile(SKU.split("-")[0] + ".*"));
		} else {
			searchQuery.put("SKU", SKU);
		}
		searchQuery.put("shopify.nickNameID", exchange.getProperty("nickNameID"));
		exchange.getOut().setHeader(MongoDbConstants.WRITECONCERN, WriteConcern.ACKNOWLEDGED);
		exchange.getOut().setHeader(MongoDbConstants.MULTIUPDATE, true);
		if (exchange.getProperties().containsKey("isQuantityUpdated")
				&& !exchange.getProperty("isQuantityUpdated", boolean.class)) {
			updateObject.put("shopify.$.warningMessage",
					"warningMessage : " + exchange.getProperty("quantityUpdateMessage"));
		}
		BasicDBObject setUpdateObject = new BasicDBObject("$set", updateObject);
		log.debug("DB query = " + searchQuery.toString() + " updateData = " + setUpdateObject.toString());
		return new Object[] { searchQuery, setUpdateObject };
	}
}