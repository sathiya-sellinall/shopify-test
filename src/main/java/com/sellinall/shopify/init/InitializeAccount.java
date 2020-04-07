package com.sellinall.shopify.init;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

public class InitializeAccount implements Processor {
	static Logger log = Logger.getLogger(InitializeAccount.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject inBody = exchange.getIn().getBody(JSONObject.class);
		log.debug("InBody: " + inBody);
		exchange.getOut().setBody(inBody);
		exchange.setProperty("accountNumber", inBody.get("accountNumber"));
		exchange.setProperty("request", inBody);
		exchange.setProperty("needToValidateAccount", false);
		if ((inBody.has("apiKey") && !inBody.getString("apiKey").equals(""))
				&& (inBody.has("password") && !inBody.getString("password").equals(""))) {
			exchange.setProperty("needToValidateAccount", true);
		}
	}
}
