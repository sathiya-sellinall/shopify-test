package com.sellinall.shopify.message;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

public class PreparePublishMessageForPNQ implements Processor {
	static Logger log = Logger.getLogger(PreparePublishMessageForPNQ.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject order = (JSONObject) exchange.getProperty("order");
		if (exchange.getProperties().containsKey("updateStatus")) {
			order.put("updateStatus", exchange.getProperty("updateStatus", String.class));
		}
		log.debug("order:" + order);
		exchange.getOut().setBody(order);
	}
}