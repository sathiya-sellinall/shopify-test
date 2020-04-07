package com.sellinall.shopify.message;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

public class ConstructMessageForBatch implements Processor {
	static Logger log = Logger.getLogger(ConstructMessageForBatch.class.getName());

	public void process(Exchange exchange) throws Exception {
		log.debug("Inside ConstructMessageForBatch");

		int noOfItemCompleted = (Integer) exchange.getProperty("noOfItemCompleted");
		int noOfItemLinked = (Integer) exchange.getProperty("noOfItemLinked");
		int noOfItemSkipped = (Integer) exchange.getProperty("noOfItemSkipped");
		JSONObject message = new JSONObject();
		if (exchange.getIn().getHeaders().containsKey("importStatus")) {
			message.put("status", exchange.getIn().getHeader("importStatus", String.class));
		}
		if (exchange.getProperties().containsKey("failureReason")) {
			message.put("failureReason", exchange.getProperty("failureReason", String.class));
		}
		message.put("noOfRecords", exchange.getProperty("numberOfRecords", Integer.class));
		message.put("skipped", noOfItemSkipped);
		message.put("imported", noOfItemCompleted);
		message.put("linked", noOfItemLinked);
		message.put("unLinked", exchange.getProperty("noOfItemUnLinked", Integer.class));
		message.put("accountNumber", exchange.getProperty("accountNumber"));
		message.put("importRecordObjectId", exchange.getProperty("importRecordObjectId"));
		message.put("requestType", "importInventoryUpdate");
		log.debug(message.toString());
		exchange.getOut().setBody(message);
	}

}
