package com.sellinall.shopify.requests;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.sellinall.config.Config;
import com.sellinall.util.HttpsURLConnectionUtil;
import com.sellinall.util.enums.OrderUpdateStatus;
import com.sellinall.util.enums.SIAOrderStatus;
import com.sellinall.util.enums.SIAPaymentStatus;
import com.sellinall.util.enums.SIAShippingStatus;

public class SubmitOrderUpdateRequest implements Processor {
	static Logger log = Logger.getLogger(SubmitOrderUpdateRequest.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject responseObj = new JSONObject();
		BasicDBObject postHelper = exchange.getProperty("postHelper", BasicDBObject.class);
		;
		JSONObject order = exchange.getProperty("order", JSONObject.class);
		String orderId = order.getString("orderID");
		SIAOrderStatus orderStatus = SIAOrderStatus.valueOf(order.getString("orderStatus"));
		if (!exchange.getProperties().containsKey("failureReason")) {
			if (orderStatus == SIAOrderStatus.ACCEPTED) {
				// Accepted is only internal state. no need to call api.
				exchange.setProperty("updateStatus", OrderUpdateStatus.COMPLETE.toString());
				return;
			}
			try {
				if (orderStatus == SIAOrderStatus.CANCELLED) {
					responseObj = submitCancelOrderRequest(order, orderId, postHelper, exchange);
				} else if (orderStatus == SIAOrderStatus.DISPATCHED) {
					responseObj = submitProcessingOrderRequest(order, orderId, postHelper, exchange);
				}
			} catch (Exception exception) {
				log.error("Order update failure : " + orderId);
				exchange.setProperty("updateStatus", OrderUpdateStatus.FAILED.toString());
			}
		} else {
			exchange.setProperty("updateStatus", OrderUpdateStatus.FAILED.toString());
		}
		exchange.setProperty("order", responseObj);
	}

	private JSONObject submitCancelOrderRequest(JSONObject order, String orderId, BasicDBObject postHelper,
			Exchange exchange) throws JSONException, IOException {
		String apiVersion = Config.getConfig().getApiVersion();
		String url = postHelper.getString("URL") + "/admin/api/" + apiVersion + "/orders/" + orderId + "/cancel.json";
		JSONObject requestPayload = constructCancelOrderData(order);
		String responseString = HttpsURLConnectionUtil
				.doPostWithAuth(url, postHelper.getString("apiKey"), postHelper.getString("pass"), requestPayload)
				.getString("payload");
		log.debug("Shopify Response=" + responseString);
		JSONObject response = new JSONObject(responseString);
		exchange.setProperty("updateStatus", OrderUpdateStatus.FAILED.toString());
		if (response.has("notice") && response.getString("notice").contains("Order has been canceled")) {
			exchange.setProperty("updateStatus", OrderUpdateStatus.COMPLETE.toString());
			order.put("paymentStatus", SIAPaymentStatus.REFUNDED.toString());
			order.put("shippingStatus", SIAShippingStatus.RETURNED.toString());
		}
		return order;
	}

	private JSONObject constructCancelOrderData(JSONObject order) throws JSONException {
		JSONObject data = new JSONObject();
		if (order.has("refundDetails")) {
			JSONObject refundData = order.getJSONObject("refundDetails");
			if (refundData.has("refundType") && refundData.getString("refundType").equals("REFUND_WITH_AMOUNT")) {
				data.put("note", refundData.getString("note"));
				data.put("amount", refundData.getString("amount"));
			}
		}
		return data;
	}

	private JSONObject submitProcessingOrderRequest(JSONObject order, String orderId, BasicDBObject postHelper,
			Exchange exchange) throws JSONException, IOException {
		JSONObject fulfillment = new JSONObject();
		JSONObject shippingDetails = order.getJSONObject("shippingDetails");
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		JSONArray orderItemDetails = order.getJSONArray("orderItems");
		exchange.setProperty("updateStatus", OrderUpdateStatus.FAILED.toString());
		String apiVersion = Config.getConfig().getApiVersion();
		if (orderItemDetails.length() != 0) {
			Set<String> orderFulfillmentLocation = exchange.getProperty("orderFulfillmentLocation", Set.class);
			for (String locationID : orderFulfillmentLocation) {
				JSONArray orderLineItems = new JSONArray();
				JSONObject fulfillments = new JSONObject();
				for (int i = 0; i < orderItemDetails.length(); i++) {
					JSONObject orderItems = orderItemDetails.getJSONObject(i);
					if (locationID.equals(orderItems.getString("locationID"))) {
						JSONObject ids = new JSONObject();
						long orderIds = Long.parseLong(orderItems.getString("orderItemID"));
						ids.put("id", orderIds);
						orderLineItems.put(ids);
					}
				}
				fulfillment.put("location_id", Long.parseLong(locationID));
				fulfillment.put("line_items", orderLineItems);
				fulfillments.put("fulfillment", fulfillment);
				String url = postHelper.getString("URL") + "/admin/api/" + apiVersion + "/orders/" + orderId
						+ "/fulfillments.json";
				String responseString = HttpsURLConnectionUtil
						.doPostWithAuth(url, postHelper.getString("apiKey"), postHelper.getString("pass"), fulfillments)
						.getString("payload");
				JSONObject response = new JSONObject(responseString);
				log.debug("fulfillment Response=" + responseString.toString());
				JSONObject fulfillmentObj = response.getJSONObject("fulfillment");
				if (fulfillmentObj.has("status") && fulfillmentObj.getString("status").equals("success")) {
					long fulfillmentId = fulfillmentObj.getLong("id");
					shippingDetails.put("fulfillmentID", fulfillmentId);
					if (shippingDetails.has("shippingTrackingDetails")) {
						submitShippingTrackingDetails(shippingDetails, orderId, postHelper);
					}
				} else {
					log.error("Unable to create fulfillment for accountNumber:" + accountNumber + ", orderID:" + orderId
							+ ", and response" + response.toString());
				}
			}
			order.put("orderStatus", SIAOrderStatus.DISPATCHED.toString());
			exchange.setProperty("updateStatus", OrderUpdateStatus.COMPLETE.toString());
		}
		return order;
	}

	private void submitShippingTrackingDetails(JSONObject shippingDetails, String orderId, BasicDBObject postHelper)
			throws JSONException, IOException {
		JSONObject trackingDetails = new JSONObject();
		JSONObject trackingRequestObj = new JSONObject();
		JSONObject shippingTrackingDetails = shippingDetails.getJSONObject("shippingTrackingDetails");
		if (shippingTrackingDetails.has("airwayBill")) {
			trackingDetails.put("tracking_number", shippingTrackingDetails.getString("airwayBill"));
		}
		if (shippingTrackingDetails.has("courierName")) {
			trackingDetails.put("tracking_company", shippingTrackingDetails.getString("courierName"));
		}
		if (shippingTrackingDetails.has("trackingURL")) {
			trackingDetails.put("tracking_url", shippingTrackingDetails.getString("trackingURL"));
		}
		String fullfillmentId = shippingDetails.getString("fulfillmentID");
		trackingDetails.put("id", fullfillmentId);
		trackingRequestObj.put("fulfillment", trackingDetails);
		String url = postHelper.getString("URL") + "/admin/orders/" + orderId + "/fulfillments/" + fullfillmentId
				+ ".json";
		HttpsURLConnectionUtil.doPutWithAuth(url, postHelper.getString("apiKey"), postHelper.getString("pass"),
				trackingRequestObj);
		log.debug("tracking details has been updated for the orderId: " + orderId);
	}

}