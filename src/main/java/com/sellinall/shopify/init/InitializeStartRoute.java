/**
 * 
 */
package com.sellinall.shopify.init;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

/**
 * @author vikraman
 * 
 */
public class InitializeStartRoute implements Processor {
	static Logger log = Logger.getLogger(InitializeStartRoute.class.getName());

	public void process(Exchange exchange) throws Exception {

		JSONObject inBody = exchange.getIn().getBody(JSONObject.class);
		log.debug("InBody: " + inBody);
		exchange.getOut().setBody(inBody);
		exchange.setProperty("SKU", inBody.get("SKU"));
		exchange.setProperty("accountNumber", inBody.get("accountNumber"));
		exchange.setProperty("requestType", inBody.getString("requestType"));
		if (inBody.has("siteNicknames")) {
			exchange.setProperty("siteNicknames", inBody.get("siteNicknames"));
		}
		Boolean isUpdateCustomSKU = false;
		Boolean isPriceUpdate=false;
		Boolean isQuantityUpdate= false;
		Boolean isQuantityUpdateByNewOrder = false;
		if (inBody.has("fieldsToUpdate")) {
			JSONArray fieldsToUpdates = inBody.getJSONArray("fieldsToUpdate");
			List<String> fieldsToUpdatesList = new ArrayList<String>();
			for (int i = 0; i < fieldsToUpdates.length(); i++) {
				fieldsToUpdatesList.add(fieldsToUpdates.getString(i));
			}
			if (fieldsToUpdatesList.contains("customSKU")) {
				isUpdateCustomSKU = true;
			}
			if (fieldsToUpdatesList.contains("price")) {
				isPriceUpdate = true;
			}
			if (fieldsToUpdatesList.contains("quantity")) {
				isQuantityUpdate = true;
				if (inBody.has("isQuantityUpdateByNewOrder")) {
					isQuantityUpdateByNewOrder = inBody.getBoolean("isQuantityUpdateByNewOrder");
				}
			}
		}
		exchange.setProperty("isQuantityUpdate",isQuantityUpdate);
		exchange.setProperty("isQuantityUpdateByNewOrder", isQuantityUpdateByNewOrder);
		exchange.setProperty("isPriceUpdate", isPriceUpdate);
		exchange.setProperty("isUpdateCustomSKU", isUpdateCustomSKU);
		exchange.setProperty("request", inBody);
	}

}