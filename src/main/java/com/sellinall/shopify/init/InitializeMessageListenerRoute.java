package com.sellinall.shopify.init;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

public class InitializeMessageListenerRoute implements Processor {
	static Logger log = Logger
			.getLogger(InitializeMessageListenerRoute.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject inBody = new JSONObject(exchange.getIn().getBody(String.class));
		log.info("InitializeMessageListenerRoute " +inBody);
		exchange.setProperty("requestType", inBody.getString("requestType"));
		exchange.getOut().setBody(inBody);
	}
}
