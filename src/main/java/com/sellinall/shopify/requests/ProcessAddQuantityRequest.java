package com.sellinall.shopify.requests;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.sellinall.config.Config;
import com.sellinall.util.HttpsURLConnectionUtil;

public class ProcessAddQuantityRequest implements Processor {
	static Logger log = Logger.getLogger(ProcessAddQuantityRequest.class.getName());

	public void process(Exchange exchange) throws Exception {
		boolean isQuantityUpdated = false;
		String quantityUpdateMessage = "";
		BasicDBObject postHelper = exchange.getProperty("postHelper", BasicDBObject.class);
		String requestType = exchange.getProperty("requestType", String.class);
		String apiVersion = Config.getConfig().getApiVersion();
		String url = postHelper.getString("URL") + "/admin/api/" + apiVersion + "/inventory_levels/set.json";
		String ftlRequest = exchange.getIn().getBody(String.class);
		try {
			JSONObject responseObject = HttpsURLConnectionUtil.doPostWithAuth(url, postHelper.getString("apiKey"),
					postHelper.getString("pass"), new JSONObject(ftlRequest));
			if (responseObject.getInt("httpCode") == HttpStatus.SC_OK) {
				exchange.setProperty("quantityUpdateResponse", responseObject);
				isQuantityUpdated = true;
			} else {
				log.error(requestType + ": Unable to update quantity for SKU "
						+ exchange.getProperty("SKU", String.class) + " and response : " + responseObject.toString());
				quantityUpdateMessage = "Unable to update quantity " + responseObject.getString("payload");
			}
		} catch (Exception exception) {
			log.error(requestType + ": Quantity update failure for SKU " + exchange.getProperty("SKU", String.class)
					+ " and response : " + exception);
			quantityUpdateMessage = "Unable to update quantity " + exception.getMessage();
			exception.printStackTrace();
		}
		exchange.setProperty("isQuantityUpdated", isQuantityUpdated);
		exchange.setProperty("quantityUpdateMessage", quantityUpdateMessage);
	}

}
