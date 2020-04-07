package com.sellinall.shopify.message;

import java.util.HashMap;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.sellinall.database.DbUtilities;
import com.sellinall.shopify.common.ShopifyFinancialStatus;
import com.sellinall.util.CurrencyUtil;
import com.sellinall.util.DateUtil;
import com.sellinall.util.enums.SIAOrderStatus;
import com.sellinall.util.enums.SIAPaymentStatus;
import com.sellinall.util.enums.SIAShippingStatus;

/**
 * 
 * @author Raju
 *
 */

public class ConstructMessageForPNQ implements Processor {
	static Logger log = Logger.getLogger(ConstructMessageForPNQ.class.getName());

	public void process(Exchange exchange) throws Exception {
		exchange.getOut().setBody(createMessage(exchange));
	}

	private JSONObject createMessage(Exchange exchange) throws JSONException {
		JSONObject rawDataJson = new JSONObject(exchange.getProperty("rawData").toString());
		String accountNumber = exchange.getProperty("accountNumber",String.class);
		String nickNameID = exchange.getProperty("nickNameID",String.class);
		JSONObject siaNotificationMessage = new JSONObject();
		String orderID = null;
		if (rawDataJson.getString("actionName").equals("fulfillmentEvents")) {
			orderID = rawDataJson.getString("order_id");
			siaNotificationMessage.put("orderID", orderID);
		} else {
			orderID = rawDataJson.getString("id");
			siaNotificationMessage.put("orderID", orderID);
			BasicDBObject existingOrderDetails = checkIfOrderAlreadyExists(orderID, accountNumber, nickNameID);
			if (rawDataJson.has("name")) {
				siaNotificationMessage.put("orderNumber", rawDataJson.getString("name"));
			}
			if (rawDataJson.has("created_at")) {
				long timeOrderCreated = DateUtil.getUnixTimestamp(rawDataJson.getString("created_at"),
						"yyyy-MM-dd'T'HH:mm:ssXXX", "GMT");
				siaNotificationMessage.put("timeOrderCreated", timeOrderCreated);
			}
			String transactionAmount = rawDataJson.getString("total_price");
			long amount = CurrencyUtil.convertAmountToSIAFormat(Double.parseDouble(transactionAmount));
			String currency = rawDataJson.getString("currency");
			siaNotificationMessage.put("orderAmount", CurrencyUtil.getJSONAmountObject(Long.valueOf(amount), currency));
			JSONObject buyerDetails = getBuyerDetails(rawDataJson, accountNumber);
			siaNotificationMessage.put("buyerDetails", buyerDetails);
			boolean hasShippingCarrier = (exchange.getProperties().containsKey("hasShippingCarrier")
					&& exchange.getProperty("hasShippingCarrier", Boolean.class));
			JSONObject shippingDetails = getShippingDetails(rawDataJson, existingOrderDetails, hasShippingCarrier);
			siaNotificationMessage.put("shippingDetails", shippingDetails);
			String paymentMethod = rawDataJson.getString("gateway");
			JSONArray paymentMethods = new JSONArray();
			paymentMethods.put(paymentMethod);
			siaNotificationMessage.put("paymentMethods", paymentMethods);
			JSONArray orderItems = getOrderItem(rawDataJson, exchange);
			siaNotificationMessage.put("orderItems", orderItems);
			siaNotificationMessage.put("paymentStatus", setPaymentStatus(rawDataJson));

			JSONArray shippingLinesArray = rawDataJson.getJSONArray("shipping_lines");
			if (shippingLinesArray.length() > 0) {
				JSONObject shippingLinesObject = shippingLinesArray.getJSONObject(0);
				String shippingPrice = shippingLinesObject.getString("price");
				long shippingAmount = CurrencyUtil.convertAmountToSIAFormat(Double.parseDouble(shippingPrice));
				siaNotificationMessage.put("shippingAmount",
						CurrencyUtil.getJSONAmountObject(shippingAmount, currency));
				if (shippingLinesArray.length() > 1) {
					log.warn("shipping_lines Array is having more than one object, which is not handled for orderId: "
							+ rawDataJson.getString("id") + " nickNameID: " + nickNameID + "  for accountNumber: "
							+ accountNumber + " and shipping_lines: " + shippingLinesArray);
				}
			}
		}
		siaNotificationMessage.put("accountNumber", accountNumber);
		siaNotificationMessage.put("orderStatus", setOrderStatus(rawDataJson));
		siaNotificationMessage.put("shippingStatus", setShippingStatus(rawDataJson));
		siaNotificationMessage.put("site", "shopify");
		siaNotificationMessage.put("nickNameID", nickNameID);
		String notificationID = (String) exchange.getProperty("notificationID");
		siaNotificationMessage.put("notificationID", notificationID);
		log.info(siaNotificationMessage);
		return siaNotificationMessage;
	}

	private BasicDBObject checkIfOrderAlreadyExists(String orderID, String accountNumber, String nickNameID) {
		DBCollection table = DbUtilities.getOrderDBCollection("order");
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("accountNumber", accountNumber);
		searchQuery.put("orderID", orderID);
		searchQuery.put("site.nickNameID", nickNameID);
		BasicDBObject projection = new BasicDBObject("shippingDetails", 1);
		return (BasicDBObject) table.findOne(searchQuery, projection);
	}

	private String setOrderStatus(JSONObject rawData) throws JSONException {
		if (rawData.getString("actionName").equals("orders")) {
			if (!rawData.isNull("cancel_reason")) {
				return SIAOrderStatus.CANCELLED.toString();
			}
			if (rawData.getJSONArray("fulfillments").length() != 0) {
				JSONObject fulfillments = rawData.getJSONArray("fulfillments").getJSONObject(0);
				if (fulfillments.getString("status").equals("success")) {
					return SIAOrderStatus.DISPATCHED.toString();
				} else if (fulfillments.getString("status").equals("cancelled")) {
					return SIAOrderStatus.INITIATED.toString();
				}
			}
			return SIAOrderStatus.INITIATED.toString();
		} else if (rawData.getString("actionName").equals("fulfillmentEvents")) {
			if (rawData.getString("status").equals("confirmed")) {
				return SIAOrderStatus.PROCESSING.toString();
			} else if (rawData.getString("status").equals("in_transit")
					|| rawData.getString("status").equals("out_for_delivery")) {
				return SIAOrderStatus.DISPATCHED.toString();
			} else if (rawData.getString("status").equals("delivered")) {
				return SIAOrderStatus.DELIVERED.toString();
			} else if (rawData.getString("status").equals("failure")) {
				return SIAOrderStatus.DELIVERY_FAILED.toString();
			}
		}
		return SIAOrderStatus.UNKNOWN.toString();
	}

	private String setPaymentStatus(JSONObject rawData) throws JSONException {
		if (rawData.getString("actionName").equals("orders")) {
			String receivedPaymentStatus = rawData.getString("financial_status");
			if (ShopifyFinancialStatus.PENDING.equalsName(receivedPaymentStatus)) {
				return SIAPaymentStatus.INITIATED.toString();
			} else if (ShopifyFinancialStatus.AUTHORIZED.equalsName(receivedPaymentStatus)) {
				return SIAPaymentStatus.AUTHORIZED.toString();
			} else if (ShopifyFinancialStatus.PAID.equalsName(receivedPaymentStatus)) {
				return SIAPaymentStatus.COMPLETED.toString();
			} else if (ShopifyFinancialStatus.REFUNDED.equalsName(receivedPaymentStatus)) {
				return SIAPaymentStatus.REFUNDED.toString();
			}
		}
		return SIAPaymentStatus.UNKNOWN.toString();
	}

	private String setShippingStatus(JSONObject rawData) throws JSONException {
		if (rawData.getString("actionName").equals("orders")) {
			return SIAShippingStatus.NOT_SHIPPED.toString();
		} else if (rawData.getString("actionName").equals("fulfillmentEvents")) {
			if (rawData.getString("status").equals("confirmed")) {
				return SIAShippingStatus.READY_TO_SHIP.toString();
			} else if (rawData.getString("status").equals("in_transit")
					|| rawData.getString("status").equals("out_for_delivery")) {
				return SIAShippingStatus.SHIPPED.toString();
			} else if (rawData.getString("status").equals("delivered")) {
				return SIAShippingStatus.DELIVERED.toString();
			} else if (rawData.getString("status").equals("failure")) {
				return SIAShippingStatus.RETURN_SHIPPED.toString();
			}
		}
		return SIAShippingStatus.UNKNOWN.toString();
	}

	private JSONObject getBuyerDetails(JSONObject rawData, String accountNumber) throws JSONException {
		JSONObject buyerDetails = new JSONObject();
		if (rawData.has("customer")) {
			JSONObject customer = rawData.getJSONObject("customer");
			if (!customer.isNull("email")) {
				buyerDetails.put("email", customer.getString("email"));
			} else {
				log.warn("buyer emailId is null for orderId: " + rawData.getString("id") + " and accountNumber: "
						+ accountNumber);
			}
			JSONObject defaultAddress = customer.getJSONObject("default_address");
			String name = defaultAddress.getString("name");
			buyerDetails.put("name", name);
		}
		return buyerDetails;
	}
	private JSONObject getShippingDetails(JSONObject rawData, BasicDBObject existingOrderDetails,
			Boolean hasShippingCarrier) throws JSONException {
		JSONObject shippingDetails = new JSONObject();
		JSONObject shippingAddress = null;
		JSONObject address = new JSONObject();
		if (rawData.getJSONArray("shipping_lines").length() != 0) {
			JSONObject shippingLine = rawData.getJSONArray("shipping_lines").getJSONObject(0);
			String shippingMethod = shippingLine.getString("code");
			shippingDetails.put("method", shippingMethod);
		}
		if (rawData.has("shipping_address") && rawData.getJSONObject("shipping_address") != null) {
			shippingAddress = rawData.getJSONObject("shipping_address");
			String name = shippingAddress.getString("name");
			String street1 = shippingAddress.getString("address1");
			if (!shippingAddress.isNull("address2")) {
				String street2 = shippingAddress.getString("address2");
				address.put("street2", street2);
			}
			String city = shippingAddress.getString("city");
			String province = shippingAddress.getString("province");
			String country = shippingAddress.getString("country");
			String postalCode = shippingAddress.getString("zip");
			address.put("name", name);
			address.put("street1", street1);
			address.put("city", city);
			address.put("state", province);
			address.put("country", country);
			address.put("postalCode", postalCode);
			shippingDetails.put("address", address);
			if (shippingAddress.has("phone")) {
				address.put("phone", shippingAddress.getString("phone"));
			}
		}
		if (rawData.getJSONArray("fulfillments").length() != 0) {
			JSONObject shippingTrackingDetails = new JSONObject();
			JSONObject fulfillments = rawData.getJSONArray("fulfillments").getJSONObject(0);
			if (!fulfillments.isNull("tracking_company")) {
				shippingTrackingDetails.put("courierName", fulfillments.getString("tracking_company"));
			}
			if (!fulfillments.isNull("tracking_number")) {
				shippingTrackingDetails.put("airwayBill", fulfillments.getString("tracking_number"));
			}
			if (!rawData.isNull("note")) {
				shippingTrackingDetails.put("remarks", rawData.getString("note"));
			}
			shippingDetails.put("shippingTrackingDetails", shippingTrackingDetails);
			shippingDetails.put("fulfillmentID", fulfillments.getLong("id"));
		}
		if (hasShippingCarrier && existingOrderDetails != null
				&& existingOrderDetails.containsField("shippingDetails")) {
			//if order is already exists and has shipping carrier enabled, then copy shippingTracking information if exists
			BasicDBObject existingShippingDetails = (BasicDBObject) existingOrderDetails.get("shippingDetails");
			if (existingShippingDetails.containsField("shippingTrackingDetails")) {
				shippingDetails.put("shippingTrackingDetails", (BasicDBObject)existingShippingDetails.get("shippingTrackingDetails"));
			}
		}
		return shippingDetails;
	}

	private JSONArray getOrderItem(JSONObject rawData, Exchange exchange) throws JSONException {
		HashMap<String, String> SKUMap = exchange.getProperty("SKUMap", HashMap.class);
		JSONArray orderItems = new JSONArray();
		JSONArray line_items = rawData.getJSONArray("line_items");
		int numberOfItems = line_items.length();
		for (int index = 0; index < numberOfItems; index++) {
			JSONObject orderItem = new JSONObject();
			JSONObject line = line_items.getJSONObject(index);
			String itemID = line.getString("variant_id");
			orderItem.put("orderItemID", line.getString("id"));

			if (SKUMap.containsKey(itemID) && !SKUMap.get(itemID).equals("")) {
				orderItem.put("SKU", SKUMap.get(itemID));
			}
			orderItem.put("itemTitle", line.getString("title"));
			String customSKU = line.getString("sku");
			if (!customSKU.isEmpty()) {
				orderItem.put("customSKU", customSKU);
			}
			int quantity = line.getInt("quantity");
			orderItem.put("quantity", quantity);

			String itemPrice = line.getString("price");
			long itemAmount = CurrencyUtil.convertAmountToSIAFormat(Double.parseDouble(itemPrice));
			String currency = rawData.getString("currency");
			orderItem.put("itemAmount", CurrencyUtil.getJSONAmountObject(Long.valueOf(itemAmount), currency));
			orderItems.put(orderItem);
		}
		return orderItems;
	}

}