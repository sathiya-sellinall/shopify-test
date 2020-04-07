package com.sellinall.shopify.message;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.codehaus.jettison.json.JSONObject;

/**
 * 
 * @author Raguvaran
 *
 */
public class ConstructResponseMessage implements Processor {

	public void process(Exchange exchange) throws Exception {
		JSONObject response = new JSONObject();
		response.put("response", "success");
		response.put("responseMessage", "success");
		if (exchange.getProperties().containsKey("failureReason")) {
			response.put("response", "failure");
			response.put("responseMessage", exchange.getProperty("failureReason", String.class));
		}
		exchange.getOut().setBody(response);
	}

}
