package com.sellinall.shopify.requests;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.jetty.http.HttpStatus;

import com.mongodb.BasicDBObject;
import com.sellinall.config.Config;
import com.sellinall.util.HttpsURLConnectionUtil;

public class PullLocationDetailsFromSite implements Processor {
	static Logger log = Logger.getLogger(PullLocationDetailsFromSite.class.getName());

	public void process(Exchange exchange) {
		List<JSONObject> locationList = new ArrayList<JSONObject>();
		try {
			String accountNumber = exchange.getProperty("accountNumber", String.class);
			String nickNameID = exchange.getProperty("nickNameID", String.class);
			String SKU = exchange.getProperty("SKU", String.class);
			BasicDBObject postHelper = exchange.getProperty("postHelper", BasicDBObject.class);
			String url = postHelper.getString("URL");
			String apiVersion = Config.getConfig().getApiVersion();
			String restGetProductEndPoint = url + "/admin/api/" + apiVersion + "/locations.json";
			JSONObject response = HttpsURLConnectionUtil.doGetWithAuth(postHelper.getString("apiKey"),
					postHelper.getString("pass"), restGetProductEndPoint);
			if (response.getInt("httpCode") != HttpStatus.OK_200) {
				exchange.setProperty("failureReason", "Failed to get location details from shopify");
				log.error("Failed to get location details from shopify for  accountNumber-" + accountNumber
						+ ",nickNameID" + nickNameID + ",SKU " + SKU + "and response is - " + response.toString());
			}
			JSONObject responseFromSite = new JSONObject(response.getString("payload"));
			if (!responseFromSite.has("locations")) {
				exchange.setProperty("failureReason",
						"Failed to get location details from shopify because locations key not found");
				log.error(
						"Failed to get location details from shopify because locations key not found for accountNumber-"
								+ accountNumber + ",nickNameID" + nickNameID + ",SKU " + SKU + "and response is - "
								+ response.toString());
			} else {
				JSONArray shopifLocationArray = responseFromSite.getJSONArray("locations");
				for (int i = 0; i < shopifLocationArray.length(); i++) {
					JSONObject location = shopifLocationArray.getJSONObject(i);
					if (location.getBoolean("active")) {
						locationList.add(location);
					}
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
			exchange.setProperty("failureReason", e.getMessage());
		}
		exchange.setProperty("locationsList", locationList);

	}

}