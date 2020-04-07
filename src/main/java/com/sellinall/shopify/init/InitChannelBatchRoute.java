package com.sellinall.shopify.init;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

public class InitChannelBatchRoute implements Processor {
	static Logger log = Logger.getLogger(InitChannelBatchRoute.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject inBody = exchange.getIn().getBody(JSONObject.class);
		log.debug("InitEbayBatchAddItemRoute in body: " + inBody);
		exchange.setProperty("accountNumber", inBody.get("accountNumber"));
		exchange.setProperty("siteNicknames", inBody.get("siteNicknames"));
		JSONArray siteNicknames = inBody.getJSONArray("siteNicknames");
		String channelName = siteNicknames.getString(0).split("-")[0];
		exchange.setProperty("channelName", channelName);
		exchange.setProperty("requestType", inBody.getString("requestType"));
		exchange.setProperty("inputRequest", inBody);
		exchange.getOut().setBody(inBody);
	}
}
