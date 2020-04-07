package com.sellinall.shopify.init;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.codehaus.jettison.json.JSONObject;

public class InitializeUpdateOrderRoute implements Processor {

	public void process(Exchange exchange) throws Exception {
		JSONObject order = exchange.getIn().getBody(JSONObject.class);
		exchange.setProperty("inputRequest", order);
		order.remove("requestType");
		exchange.setProperty("order", order);
		exchange.setProperty("accountNumber", order.get("accountNumber"));
		exchange.setProperty("nickNameID", order.getString("nickNameID"));
		exchange.getOut().setBody(order);
	}

}
