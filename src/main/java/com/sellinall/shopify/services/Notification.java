package com.sellinall.shopify.services;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.camel.ProducerTemplate;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

@Path("/notification")
@Produces(MediaType.APPLICATION_JSON)
public class Notification {
	static Logger log = Logger.getLogger(Notification.class.getName());

	// This is being set by main function
	private static ProducerTemplate template;

	public static void setProducerTemplate(ProducerTemplate template1) {
		template = template1;
	}

	/**
	 * @param notificationData
	 *            notification received from Shopify.
	 * @throws Exception
	 */
	@POST
	@Path("/orders/{action}")
	public Object receiveOrderNotification(@PathParam("action") String action, String payload,
			@HeaderParam("X-Shopify-Shop-Domain") String shopUrl) throws Exception {
		log.debug("received Order notificationData=" + payload);
		JSONObject receiveOrderNotification = new JSONObject(payload);
		receiveOrderNotification.put("shopUrl", shopUrl);
		receiveOrderNotification.put("actionName", "orders");
		receiveOrderNotification.put("actionType", action);
		if (action.equals("updated")) {
			// order/create, orders/paid, orders/cancelled, orders/fulfilled,
			// orders/updated
			template.sendBody("direct:initNotification", receiveOrderNotification.toString());
		} else {
			// orders/delete, orders/partially_fulfilled
			template.sendBody("direct:processUnsupportedNotification", receiveOrderNotification.toString());
		}
		JSONObject response = new JSONObject();
		response.put("status", "SUCCESS");
		return response;
	}

	@POST
	@Path("/fulfillment_events/{action}")
	public Object receiveFulFillmentEventsNotification(@PathParam("action") String action, String payload,
			@HeaderParam("X-Shopify-Shop-Domain") String shopUrl) throws Exception {
		log.debug("received fulfillmentEvents notificationData=" + payload);
		JSONObject receiveFulFillmentEventsNotification = new JSONObject(payload);
		receiveFulFillmentEventsNotification.put("shopUrl", shopUrl);
		receiveFulFillmentEventsNotification.put("actionName", "fulfillmentEvents");
		if (action.equals("create")) {
			// fulfillment_events/create
			template.sendBody("direct:initNotification", receiveFulFillmentEventsNotification.toString());
		} else {
			// fulfillment_events/delete
			template.sendBody("direct:processUnsupportedNotification", receiveFulFillmentEventsNotification.toString());
		}
		JSONObject response = new JSONObject();
		response.put("status", "SUCCESS");
		return response;
	}

	@POST
	@Path("/fulfillments/{action}")
	public Object receiveFulFillmentsNotification(@PathParam("action") String action, String payload) throws Exception {
		log.debug("received fulfillments notificationData=" + payload);
		JSONObject receiveFulFillmentsNotification = new JSONObject(payload);
		receiveFulFillmentsNotification.put("actionName", "fulfillments");
		// fulfillments/create, fulfillments/update
		template.sendBody("direct:processUnsupportedNotification", receiveFulFillmentsNotification.toString());
		JSONObject response = new JSONObject();
		response.put("status", "SUCCESS");
		return response;
	}

	/**
	 * @param refundnotificationData
	 *            notification received from Shopify.
	 * @throws Exception
	 */
	@POST
	@Path("/refunds/{action}")
	public Object receiveRefundNotification(@PathParam("action") String action, String payload,
			@HeaderParam("X-Shopify-Shop-Domain") String shopUrl) throws Exception {
		log.debug("received Refund notificationData=" + payload);
		JSONObject receiveRefundNotification = new JSONObject(payload);
		receiveRefundNotification.put("shopUrl", shopUrl);
		receiveRefundNotification.put("actionName", "refunds");
		template.sendBody("direct:processUnsupportedNotification", receiveRefundNotification.toString());
		JSONObject response = new JSONObject();
		response.put("status", "SUCCESS");
		return response;
	}

	@POST
	@Path("/products/{action}")
	public Object receiveProductNotification(@PathParam("action") String action, String payload,
			@HeaderParam("X-Shopify-Shop-Domain") String shopUrl) throws Exception {
		log.debug("received Product notificationData=" + payload);
		JSONObject receiveProductNotification = new JSONObject(payload);
		receiveProductNotification.put("shopUrl", shopUrl);
		receiveProductNotification.put("actionName", "products");
		if (action.equals("create")) {
			// products/create
			template.sendBody("direct:initNotification", receiveProductNotification.toString());
		} else {
			// products/update, products/delete
			template.sendBody("direct:processUnsupportedNotification", receiveProductNotification.toString());
		}
		JSONObject response = new JSONObject();
		response.put("status", "SUCCESS");
		return response;
	}
}