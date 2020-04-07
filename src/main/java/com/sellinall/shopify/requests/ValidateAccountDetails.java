package com.sellinall.shopify.requests;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.sellinall.shopify.util.ShopifyConnectionUtil;

public class ValidateAccountDetails implements Processor {
	static Logger log = Logger.getLogger(ValidateAccountDetails.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject request = exchange.getProperty("request", JSONObject.class);
		try {
			exchange.setProperty("isValidAccount", false);
			exchange.setProperty("failureReason", "Authentication failed");
			String apikey = request.getString("apiKey");
			String password = request.getString("password");
			String url = request.getString("webUrl");
			String shopUrl = url;
			String responseString = ShopifyConnectionUtil.doGetWithAuthorization(apikey, password,
					shopUrl + "/admin/shop.json");
			log.debug(responseString);
			JSONObject userResponse = new JSONObject(responseString);
			if (userResponse.has("httpCode") && userResponse.getInt("httpCode") == HttpStatus.SC_OK
					&& userResponse.getJSONObject("payload").has("shop")) {
				exchange.setProperty("isValidAccount", true);
				exchange.removeProperties("failureReason");
				request.put("shop", userResponse.getJSONObject("payload").getJSONObject("shop"));
				exchange.getOut().setBody(request);
				return;
			}
			log.error("Invalid account details");
		} catch (Exception e) {
			log.error("User Auth Failure");
		}
	}
}
