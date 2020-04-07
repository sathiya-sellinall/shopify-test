package com.sellinall.shopify.message;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

public class ConstructProcessNotificationMsg implements Processor {
	static Logger log = Logger.getLogger(ConstructProcessNotificationMsg.class.getName());

	public void process(Exchange exchange) throws Exception {

		JSONObject publishMessage =  new JSONObject();
		publishMessage.put("notificationID", exchange.getProperty("notificationID"));
		publishMessage.put("requestType", "processNotification");
		exchange.getOut().setBody(publishMessage);
	}
}
