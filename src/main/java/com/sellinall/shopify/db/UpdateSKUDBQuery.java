/**
 * 
 */
package com.sellinall.shopify.db;

import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * @author vikraman
 * 
 */
public class UpdateSKUDBQuery implements Processor {
	static Logger log = Logger.getLogger(UpdateSKUDBQuery.class.getName());

	public void process(Exchange exchange) throws Exception {
		String requestType = exchange.getProperty("requestType").toString();
		if (requestType.equals("removeArrayItem")) {
			Object[] outBody = createBodyForRemoveItem(exchange);
			exchange.getOut().setBody(outBody);
		} else {
			Object[] outBody = createBody(exchange);
			exchange.getOut().setBody(outBody);
		}
	}

	private Object[] createBody(Exchange exchange) {
		BasicDBObject inventory = (BasicDBObject) exchange.getProperty("inventory");
		String SKU = (String) exchange.getProperty("SKU");
		log.debug("SKU=" + SKU);
		BasicDBObject shopify = (BasicDBObject) inventory.get("shopify");
		String nickNameID = shopify.getString("nickNameID");
		DBObject filterField1 = new BasicDBObject("SKU", SKU);
		DBObject filterField2 = new BasicDBObject("shopify.nickNameID", nickNameID);
		BasicDBList and = new BasicDBList();
		and.add(filterField1);
		and.add(filterField2);
		DBObject filterField = new BasicDBObject("$and", and);
		//This list will always contains one index.
		BasicDBObject updateObjectMap = ((List<BasicDBObject>) exchange.getIn().getBody()).get(0);
		DBObject updateObject = new BasicDBObject("$set", updateObjectMap);

		return new Object[] { filterField, updateObject };
	}

	private Object[] createBodyForRemoveItem(Exchange exchange) {
		String SKU = (String) exchange.getProperty("SKU");
		log.debug("SKU=" + SKU);
		String nickNameID = (String) exchange.getProperty("nickNameID");
		DBObject filterField1 = new BasicDBObject("SKU", SKU);
		DBObject filterField2 = new BasicDBObject("shopify.nickNameID", nickNameID);
		BasicDBList and = new BasicDBList();
		and.add(filterField1);
		and.add(filterField2);
		DBObject filterField = new BasicDBObject("$and", and);
		DBObject updateObject = new BasicDBObject("$set", new BasicDBObject("shopify.$.status", "X"));
		return new Object[] { filterField, updateObject };
	}
}