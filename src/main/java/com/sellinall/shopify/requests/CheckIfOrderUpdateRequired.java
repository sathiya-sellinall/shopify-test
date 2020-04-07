package com.sellinall.shopify.requests;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.sellinall.util.enums.SIAOrderStatus;

public class CheckIfOrderUpdateRequired implements Processor {
	static Logger log = Logger.getLogger(CheckIfOrderUpdateRequired.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject order = (JSONObject) exchange.getProperty("order");
		boolean updateOrderStatus = order.getBoolean("needToUpdateOrder");
		exchange.getOut().setHeader("isOrderUpdateRequired", updateOrderStatus);
		exchange.getOut().setBody(order);
	}
}
