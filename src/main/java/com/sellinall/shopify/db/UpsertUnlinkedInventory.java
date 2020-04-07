package com.sellinall.shopify.db;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.mongodb.MongoDbConstants;
import org.apache.log4j.Logger;

import com.mongodb.BasicDBObject;
import com.mongodb.WriteConcern;

/**
 * @author Samy
 * 
 */
public class UpsertUnlinkedInventory implements Processor {
	static Logger log = Logger.getLogger(UpsertUnlinkedInventory.class.getName());

	@SuppressWarnings("unchecked")
	public void process(Exchange exchange) throws Exception {
		BasicDBObject unlinkedInventory = exchange.getIn().getBody(BasicDBObject.class);
		log.debug(unlinkedInventory.toString());
		exchange.setProperty("unlinkedInventory", unlinkedInventory);

		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("accountNumber", exchange.getProperty("accountNumber", String.class));
		searchQuery.put("shopify.nickNameID", exchange.getProperty("nickNameID", String.class));
		String actionName = exchange.getProperty("actionName", String.class);
		if (actionName != null && actionName.equals("products")) {
			searchQuery.put("shopify.refrenceID", exchange.getProperty("refrenceID").toString());
			if (exchange.getProperty("hasVariations", Boolean.class)) {
				searchQuery.put("variants", new BasicDBObject("$exists", true));
			}
		} else {
			searchQuery.put("SKU", unlinkedInventory.getString("SKU"));
		}
		BasicDBObject updateData = new BasicDBObject("$set", unlinkedInventory);

		exchange.getOut().setHeader(MongoDbConstants.UPSERT, true);
		exchange.getOut().setHeader(MongoDbConstants.WRITECONCERN, WriteConcern.ACKNOWLEDGED);
		log.debug("Variants = " + updateData.toString());
		exchange.getOut().setBody(new Object[] { searchQuery, updateData });
	}

}