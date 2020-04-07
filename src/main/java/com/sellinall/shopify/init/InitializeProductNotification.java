package com.sellinall.shopify.init;

import java.util.ArrayList;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

/**
 *
 * @author Raguvaran
 *
 */
public class InitializeProductNotification implements Processor {
	static Logger log = Logger.getLogger(InitializeProductNotification.class.getName());

	public void process(Exchange exchange) throws Exception {
		String inBody = exchange.getProperty("rawData", String.class);
		log.debug("rawData: " + inBody);
		ArrayList<JSONObject> arrayList = new ArrayList<JSONObject>();
		JSONObject payload = new JSONObject(inBody);
		arrayList.add(payload);
		exchange.setProperty("pulledInventoryList", arrayList);
		exchange.setProperty("itemListIndex", 0);
		exchange.setProperty("noOfItemCompleted", 0);
		exchange.setProperty("noOfItemSkipped", 0);
		exchange.setProperty("noOfItemLinked", 0);
		exchange.setProperty("noOfItemUnLinked", 0);
	}

}