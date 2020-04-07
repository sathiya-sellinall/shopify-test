package com.sellinall.shopify.services;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.camel.ProducerTemplate;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

@Path("/inventory")
@Produces(MediaType.APPLICATION_JSON)
public class ListingServ {
	// This is being set by main function
	static Logger log = Logger.getLogger(ListingServ.class.getName());

	private static ProducerTemplate template;

	public static void setProducerTemplate(ProducerTemplate template1) {
		template = template1;
	}

	@POST
	@Path("/{param}")
	public Object post(@HeaderParam("accountNumber") String accountNumber, @PathParam("param") String sku, JSONObject payload)
			throws Exception {
		JSONObject json = new JSONObject();
		log.debug("sku: " + sku);
		log.debug("payload: " + payload);
		json.put("SKU", sku);
		json.put("accountNumber", accountNumber);
		json.put("requestType", "addItem");
		json.put("siteNicknames", payload.getJSONArray("siteNicknames"));
		json.put("isDeleteOperation", false);
		template.sendBody("direct:startPost", json);
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("response", "success");
		return jsonResponse;
	}

	@PUT
	@Path("/{param}")
	public JSONObject update(@HeaderParam("accountNumber") String accountNumber, @PathParam("param") String sku, JSONObject payload)
			throws Exception {
		JSONObject json = new JSONObject();
		log.debug("sku: " + sku);
		log.debug("payload: " + payload);
		json.put("requestType", "updateItem");
		json.put("SKU", sku);
		json.put("accountNumber", accountNumber);
		if (payload.has("siteNicknames")) {
			json.put("siteNicknames", payload.getJSONArray("siteNicknames"));
		}
		json.put("isDeleteOperation", false);
		template.sendBody("direct:startUpdate", json);
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("response", "success");
		return jsonResponse;
	}

}
