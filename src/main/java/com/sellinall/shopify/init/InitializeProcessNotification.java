/**
 * 
 */
package com.sellinall.shopify.init;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

/**
 * @author Senthil
 *
 */
public class InitializeProcessNotification implements Processor {
	static Logger log = Logger.getLogger(InitializeProcessNotification.class.getName());

	public void process(Exchange exchange) throws Exception {

		String inBody = exchange.getIn().getBody(String.class);
		log.debug("InBody: " + inBody);
		exchange.getOut().setBody(inBody);
		exchange.setProperty("rawData", inBody);
		JSONObject payload = new JSONObject(inBody);
		exchange.setProperty("actionName", payload.getString("actionName"));
		exchange.setProperty("shopUrl", null);
		if(payload.has("actionType")) {
			exchange.setProperty("actionType", payload.getString("actionType"));
		}
		if(payload.has("shopUrl")){
			exchange.setProperty("shopUrl", payload.getString("shopUrl"));
		}
	}

}