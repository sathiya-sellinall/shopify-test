
package com.sellinall.shopify.init;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.sellinall.config.Config;

/**
 * @author Samy
 *
 */
public class InitializePullInventoryRoute implements Processor {
	static Logger log = Logger.getLogger(InitializePullInventoryRoute.class.getName());

	public void process(Exchange exchange) throws Exception {

		JSONObject input = exchange.getIn().getBody(JSONObject.class);
		log.debug("accountNumber: " + input.getString("accountNumber"));

		exchange.setProperty("accountNumber", input.getString("accountNumber"));
		exchange.setProperty("nickNameID", input.getString("nickNameId"));
		exchange.setProperty("importRecordObjectId", input.getString("importRecordObjectId"));
		int numberOfRecords = input.getInt("numberOfRecords");
		exchange.setProperty("numberOfRecords", numberOfRecords);
		int recordsPerPage = Config.getConfig().getRecordsPerPage();
		int numberOfPages = (numberOfRecords / recordsPerPage) + ((numberOfRecords % recordsPerPage > 0) ? 1 : 0);
		exchange.setProperty("numberOfPages", numberOfPages);
		log.debug("No Of Record = " + numberOfRecords);
		exchange.setProperty("noOfItemCompleted", 0);
		exchange.setProperty("noOfItemSkipped", 0);
		exchange.setProperty("noOfItemLinked", 0);
		exchange.setProperty("noOfItemUnLinked", 0);
		exchange.setProperty("pageNumber", 1);
		exchange.setProperty("actionName", null);

	}

}