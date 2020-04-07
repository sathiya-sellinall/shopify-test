package com.sellinall.shopify.db;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.bson.types.ObjectId;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.sellinall.database.DbUtilities;

/**
 * @author Malli
 *
 */
public class RemoveNotificationFromDB implements Processor {
	static Logger log = Logger.getLogger(RemoveNotificationFromDB.class.getName());
	public void process(Exchange exchange) throws Exception {
		// NotificationId should be there in property, if does not exist then DB insertion itself failed
		ObjectId objid = new ObjectId(exchange.getProperty("notificationID").toString());
		BasicDBObject searchQuery = new BasicDBObject("_id", objid);

		DBCollection table = DbUtilities.getOrderDBCollection("notification");
		table.remove(searchQuery);
	}
}
