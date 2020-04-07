package com.sellinall.shopify.init;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.codehaus.jettison.json.JSONObject;

/**
 * 
 * @author Raguvaran
 *
 */

public class InitializeProcessPullInventoryByPage implements Processor {

	public void process(Exchange exchange) throws Exception {

		JSONObject inBody = exchange.getIn().getBody(JSONObject.class);
		String nickNameID = inBody.getString("nickNameID");
		exchange.setProperty("numberOfRecords", inBody.getInt("numberOfRecords"));
		exchange.setProperty("noOfItemCompleted", inBody.getInt("noOfItemCompleted"));
		exchange.setProperty("noOfItemLinked", inBody.getInt("noOfItemLinked"));
		exchange.setProperty("noOfItemUnLinked", inBody.getInt("noOfItemUnLinked"));
		exchange.setProperty("noOfItemSkipped", inBody.getInt("noOfItemSkipped"));
		exchange.setProperty("accountNumber", inBody.getString("accountNumber"));
		exchange.setProperty("importRecordObjectId", inBody.getString("importRecordObjectId"));
		exchange.setProperty("nickNameID", nickNameID);
		exchange.setProperty("pageNumber", inBody.getInt("pageNumber"));
		exchange.setProperty("pageInfo", inBody.getString("pageInfo"));
		exchange.setProperty("numberOfPages", inBody.getInt("numberOfPages"));
	}
}
