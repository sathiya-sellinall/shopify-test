package com.sellinall.shopify.services;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.camel.ProducerTemplate;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

@Path("/settings")
@Produces(MediaType.APPLICATION_JSON)
public class AccountService {
	static Logger log = Logger.getLogger(AccountService.class.getName());
	private static ProducerTemplate template;

	public static void setProducerTemplate(ProducerTemplate template1) {
		template = template1;
	}

	@POST
	public JSONObject addLinkedAccount(@HeaderParam("accountNumber") String accountNumber, JSONObject request)
			throws JSONException {
		log.debug("Inside add User" + accountNumber);
		request = request.getJSONObject("data");
		request.put("accountNumber", accountNumber);
		if (request.getString("webUrl").endsWith("/")) {
			String webUrl = request.getString("webUrl");
			request.put("webUrl", webUrl.substring(0, webUrl.length() - 1));
		}
		JSONObject response = template.requestBody("direct:addAccount", request, JSONObject.class);
		if (response.getString("response").equals("failure")) {
			return response;
		}
		template.asyncSendBody("direct:createWebHooksShopify", request);
		if (response.has("isNeedToRefreshCategory")) {
			// While updating existing channel object no need to get categories
			response.remove("isNeedToRefreshCategory");
			return response;
		}
		template.asyncSendBody("direct:syncCategories", request);
		return response;

	}

	@PUT
	public JSONObject updateLinkedAccount(@HeaderParam("accountNumber") String accountNumber, JSONObject request)
			throws JSONException {
		log.debug("Inside Update User" + accountNumber);
		request = request.getJSONObject("data");
		request.put("accountNumber", accountNumber);
		JSONObject response = new JSONObject();
		if (request.has("syncCategories") && request.getBoolean("syncCategories")) {
			// Refreshing the category
			response = template.requestBody("direct:syncCategories", request, JSONObject.class);
			return response;
		}
		response = template.requestBody("direct:updateAccount", request, JSONObject.class);
		return response;
	}
}