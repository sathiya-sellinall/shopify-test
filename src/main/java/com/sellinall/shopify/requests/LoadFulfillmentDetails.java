package com.sellinall.shopify.requests;

import java.util.HashSet;
import java.util.Set;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.sellinall.config.Config;
import com.sellinall.util.HttpsURLConnectionUtil;

public class LoadFulfillmentDetails implements Processor {
	static Logger log = Logger.getLogger(LoadFulfillmentDetails.class.getName());

	@Override
	public void process(Exchange exchange) throws Exception {
		JSONObject order = exchange.getProperty("order", JSONObject.class);
		String orderID = order.getString("orderID");
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		JSONArray orderItemDetails = order.getJSONArray("orderItems");
		BasicDBObject postHelper = exchange.getProperty("postHelper", BasicDBObject.class);
		String apiVersion = Config.getConfig().getApiVersion();
		Set<String> orderFulfillmentLocation = new HashSet<String>();
		try {
			String url = postHelper.getString("URL") + "/admin/api/" + apiVersion + "/orders/" + orderID
					+ "/fulfillment_orders.json";
			JSONObject responseObject = HttpsURLConnectionUtil.doGetWithAuth(postHelper.getString("apiKey"),
					postHelper.getString("pass"), url);
			if (responseObject.getInt("httpCode") == HttpStatus.SC_OK) {
				String responseString = responseObject.getString("payload");
				JSONObject payloadObject = new JSONObject(responseString);
				JSONArray fulfillmentArray = payloadObject.getJSONArray("fulfillment_orders");
				for (int i = 0; i < orderItemDetails.length(); i++) {
					JSONObject orderItem = orderItemDetails.getJSONObject(i);
					for (int j = 0; j < fulfillmentArray.length(); j++) {
						JSONObject fulfillment = fulfillmentArray.getJSONObject(j);
						String shopifyAssigLocation = fulfillment.getString("assigned_location_id");
						JSONArray lineItemArray = fulfillment.getJSONArray("line_items");
						for (int k = 0; k < lineItemArray.length(); k++) {
							JSONObject lineItemObject = lineItemArray.getJSONObject(k);
							if (lineItemObject.getString("line_item_id").equals(orderItem.getString("orderItemID"))) {
								orderItem.put("locationID", shopifyAssigLocation);
								orderFulfillmentLocation.add(shopifyAssigLocation);
							}
						}

					}
				}
				if (orderFulfillmentLocation.size() == 0) {
					exchange.setProperty("failureReason",
							"Unable to process order beacause fulfillment location not found");
					log.error("Unable to process order beacause fulfillment location not found  for accountNumber : "
							+ accountNumber + ", OrderID :" + orderID + ", reponse " + responseObject.toString());

				}
				exchange.setProperty("orderFulfillmentLocation", orderFulfillmentLocation);
			} else {
				exchange.setProperty("failureReason", "Unable to get fulfillment details");
				log.error("Unable to get fulfillment details for accountNumber : " + accountNumber + ", OrderID :"
						+ orderID + ", reponse " + responseObject.toString());

			}
		} catch (Exception e) {
			e.printStackTrace();
			log.error("Internal error for get fulfillment details on accountNumber : " + accountNumber + ", OrderID :"
					+ orderID + ", error " + e);

			exchange.setProperty("failureReason", "Internal Error: " + e.getMessage());
		}
	}

}
