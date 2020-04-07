package com.sellinall.shopify.message;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

public class ConstructBatchMessage implements Processor {
	static Logger log = Logger.getLogger(ConstructBatchMessage.class.getName());
	
	public void process(Exchange exchange) throws Exception {
		JSONObject SKUMap = exchange.getIn().getBody(JSONObject.class);
		JSONArray SKUMapList = new JSONArray();
		SKUMapList.put(SKUMap);
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		log.debug("SKUMapList: "+SKUMapList);
		JSONObject message = new JSONObject();
		message.put("SKUMap", SKUMapList);
		message.put("accountNumber", accountNumber);
		exchange.getOut().setBody(message);
	}
}
