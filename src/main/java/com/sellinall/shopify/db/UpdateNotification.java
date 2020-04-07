package com.sellinall.shopify.db;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.bson.types.ObjectId;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.sellinall.database.DbUtilities;
import com.sellinall.util.enums.NotificationStatus;

/**
 * @author Raju
 *
 */
public class UpdateNotification implements Processor {
	static Logger log = Logger.getLogger(UpdateNotification.class.getName());
	public void process(Exchange exchange) throws Exception {
		BasicDBObject searchQuery = new BasicDBObject();
		// NotificationId should be there in property, if does not exist then DB insertion itself failed
		ObjectId objId = new ObjectId(exchange.getProperty("notificationID").toString());
		searchQuery.put("_id", objId);
		BasicDBObject updateObject = new BasicDBObject();
		if(exchange.getIn().getHeader("isExceptionOccured").toString().equals("1") ){
			updateObject.put("$set", new BasicDBObject("status", NotificationStatus.EXCEPTION.name()));
		}else{
			updateObject.put("$set", new BasicDBObject("status", NotificationStatus.SUPPORTED.name()));
		}
		DBCollection table = DbUtilities.getOrderDBCollection("notification");
		table.update(searchQuery, updateObject);
	}
}
