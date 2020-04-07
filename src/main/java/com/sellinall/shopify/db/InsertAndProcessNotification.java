/**
 * 
 */
package com.sellinall.shopify.db;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import com.sellinall.database.DbUtilities;
import com.sellinall.util.DateUtil;
import com.sellinall.util.enums.NotificationStatus;

/**
 * @author Senthil
 *
 */
public class InsertAndProcessNotification implements Processor {

	public void process(Exchange exchange) throws Exception {
		DBObject notificationObj = createQuery(exchange);
		DBCollection table = DbUtilities.getOrderDBCollection("notification");
		table.insert(notificationObj);

		String notificationID = notificationObj.get("_id").toString();
		exchange.setProperty("notificationID", notificationID);
		exchange.getOut().setBody(exchange.getProperty("orderDetailsResponse"));
	}

	private BasicDBObject createQuery(Exchange exchange) {
		String rawData = exchange.getProperty("rawData", String.class);

		BasicDBObject site = new BasicDBObject();
		site.put("name", "shopify");

		BasicDBObject notification = new BasicDBObject();
		notification.put("site", site);
		notification.put("raw_data", JSON.parse(rawData));
		notification.put("time_received", DateUtil.getSIADateFormat());
		if(exchange.getIn().getHeader("SupportedNotification").toString() == "1"){
			notification.put("status", NotificationStatus.SUPPORTED.name());
		} else if(exchange.getIn().getHeader("SupportedNotification").toString() == "0"){
			notification.put("status", NotificationStatus.UNSUPPORTED.name());
		} else if(exchange.getIn().getHeader("SupportedNotification").toString() == "2"){
			notification.put("status", NotificationStatus.EXCEPTION.name());
		}
		return notification;
	}
}
