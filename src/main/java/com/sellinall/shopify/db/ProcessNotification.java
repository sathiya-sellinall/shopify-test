package com.sellinall.shopify.db;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.sellinall.database.DbUtilities;

public class ProcessNotification implements Processor {
	static Logger log = Logger.getLogger(ProcessNotification.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject inBody = (JSONObject) exchange.getIn().getBody();
		exchange.setProperty("notificationID", inBody.getString("notificationID"));
		ObjectId notificationID = new ObjectId(inBody.getString("notificationID"));
		exchange.setProperty("notificationData", inBody);

		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("_id", notificationID);
		log.debug("searchQuery: " + searchQuery);

		DBCollection table = DbUtilities.getOrderDBCollection("notification");
		DBObject orderDetail = table.findOne(searchQuery);
		if (orderDetail == null) {
			log.error("Notification doesn't exists in our database, find the root cause for this record below");
			log.error("NotificationID: " + exchange.getProperty("notificationID"));
			exchange.setProperty("hasNotificationDBRecord", false);
			return;
		}
		exchange.setProperty("hasNotificationDBRecord", true);
		exchange.getOut().setBody(orderDetail.get("raw_data").toString());
	}
}