package com.sellinall.shopify.init;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

public class InitGetCategories implements Processor {
	static Logger log = Logger.getLogger(InitGetCategories.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject inBody = exchange.getIn().getBody(JSONObject.class);
		log.debug("InBody: " + inBody);
		exchange.getOut().setBody(inBody);
		exchange.setProperty("accountNumber", inBody.get("accountNumber"));
		if (inBody.has("nickNameID")) {
			exchange.setProperty("nickNameID", inBody.get("nickNameID"));
		}
		exchange.getOut().setBody(inBody);
	}
}
