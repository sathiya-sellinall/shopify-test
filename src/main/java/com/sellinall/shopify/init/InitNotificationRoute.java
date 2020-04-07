package com.sellinall.shopify.init;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

public class InitNotificationRoute implements Processor {

	@Override
	public void process(Exchange exchange) throws Exception {
		String inBody = exchange.getIn().getBody(String.class);
		exchange.setProperty("rawData", inBody);
	}
}
