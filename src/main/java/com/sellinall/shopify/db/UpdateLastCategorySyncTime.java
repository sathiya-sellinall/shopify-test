package com.sellinall.shopify.db;

import java.util.Date;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.bson.types.ObjectId;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.sellinall.database.DbUtilities;

public class UpdateLastCategorySyncTime implements Processor {
	static Logger log = Logger.getLogger(UpdateLastCategorySyncTime.class.getName());

	public void process(Exchange exchange) throws Exception {
		DBCollection table = DbUtilities.getDBCollection("accounts");
		ObjectId objid = new ObjectId((String) exchange.getProperty("accountNumber"));
		String nickNameID = exchange.getProperty("nickNameID", String.class);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("_id", objid);
		searchQuery.put("shopify.nickName.id", nickNameID);

		BasicDBObject updateFields = new BasicDBObject();
		updateFields.put("shopify.$.lastSyncTime", new Date().getTime() / 1000);

		DBObject updateQuery = new BasicDBObject();
		updateQuery.put("$set", updateFields);
		log.debug("Updated last category sync time");
		table.update(searchQuery, updateQuery);
	}
}
