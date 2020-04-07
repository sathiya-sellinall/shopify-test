package com.sellinall.shopify.process;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.sellinall.config.Config;
import com.sellinall.util.HttpsURLConnectionUtil;

public class WebHookCreator implements Processor {
	static Logger log = Logger.getLogger(WebHookCreator.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject request = exchange.getIn().getBody(JSONObject.class);
		String shopUrl = request.getString("webUrl");
		String apikey = request.getString("apiKey");
		String password = request.getString("password");
		String webHookTopic = (String) exchange.getIn().getHeader("webHookTopic");
		try {
			JSONObject webhooks = new JSONObject();
			webhooks.put("topic", webHookTopic);
			webhooks.put("format", "json");
			webhooks.put("address", Config.getConfig().getShopifyServerURL() + "notification/" + webHookTopic + "/");
			JSONObject object = new JSONObject();
			object.put("webhook", webhooks);
			String response = HttpsURLConnectionUtil
					.doPostWithAuth(shopUrl + "/admin/api/" + Config.getConfig().getApiVersion() + "/webhooks.json",
							apikey, password, object)
					.getString("payload");
			log.debug("Response String=" + response);
		} catch (Exception e) {
			log.error("Already Web hook created" + e);
		}
	}
}