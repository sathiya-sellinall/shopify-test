package com.sellinall.shopify.process;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.sellinall.config.Config;
import com.sellinall.util.HttpsURLConnectionUtil;

public class ProcessQuantityUpdateRequest implements Processor {

	static Logger log = Logger.getLogger(ProcessQuantityUpdateRequest.class.getName());

	public void process(Exchange exchange) throws Exception {
		BasicDBObject inventory = exchange.getProperty("inventory", BasicDBObject.class);
		BasicDBObject postHelper = exchange.getProperty("postHelper", BasicDBObject.class);
		String apiVersion = Config.getConfig().getApiVersion();
		String url = postHelper.getString("URL") + "/admin/api/" + apiVersion + "/inventory_levels/set.json";
		String ftlRequest = exchange.getIn().getBody(String.class);
		try {
			JSONObject responseObject = HttpsURLConnectionUtil.doPostWithAuth(url, postHelper.getString("apiKey"),
					postHelper.getString("pass"), new JSONObject(ftlRequest));
			log.debug("Shopify Update Item Response=" + responseObject);
			exchange.getOut().setBody(responseObject);
		} catch (Exception exception) {
			log.error("Quantity update failure for : " + inventory.getString("SKU"));
			exception.printStackTrace();
		}
	}

}