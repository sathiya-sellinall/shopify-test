package com.sellinall.shopify.message;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.codehaus.jettison.json.JSONObject;

/**
 * 
 * @author Raguvaran
 *
 */

public class ConstructPagingMessage implements Processor {

	public void process(Exchange exchange) throws Exception {

		JSONObject message = new JSONObject();
		message.put("numberOfRecords", exchange.getProperty("numberOfRecords", Integer.class));
		message.put("noOfItemCompleted", exchange.getProperty("noOfItemCompleted"));
		message.put("noOfItemLinked", exchange.getProperty("noOfItemLinked"));
		message.put("noOfItemUnLinked", exchange.getProperty("noOfItemUnLinked"));
		message.put("noOfItemSkipped", exchange.getProperty("noOfItemSkipped"));
		message.put("accountNumber", exchange.getProperty("accountNumber"));
		message.put("importRecordObjectId", exchange.getProperty("importRecordObjectId"));
		message.put("pageNumber", exchange.getProperty("pageNumber", Integer.class));
		message.put("numberOfPages", exchange.getProperty("numberOfPages", Integer.class));
		message.put("pageInfo", exchange.getProperty("pageInfo", String.class));
		message.put("nickNameID", exchange.getProperty("nickNameID", String.class));
		message.put("requestType", "processPullInventoryByPage");
		exchange.getOut().setBody(message);
	}
}
