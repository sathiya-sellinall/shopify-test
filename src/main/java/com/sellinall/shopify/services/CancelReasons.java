package com.sellinall.shopify.services;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.sellinall.shopify.util.ShopifyUtil;

@Path("/cancelReasons")
@Produces(MediaType.APPLICATION_JSON)
public class CancelReasons {
	static Logger log = Logger.getLogger(AccountService.class.getName());

	@GET
	public JSONObject getCancelReasons() throws JSONException {
		JSONObject response = new JSONObject();
		response.put("cancelReasonList", ShopifyUtil.getCancelReasonList());
		return response;
	}
}