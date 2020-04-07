package com.sellinall.shopify.db;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.mongodb.MongoDbConstants;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.sellinall.util.enums.SIAInventoryStatus;

public class LoadInventoryForAutoLinking implements Processor {

	static Logger log = Logger.getLogger(UserDBQuery.class.getName());

	public void process(Exchange exchange) throws Exception {
		exchange.getOut().setBody(exchange.getIn().getBody());
		JSONObject itemFromSite = (JSONObject) exchange.getIn().getBody();
		exchange.setProperty("inBodyResponse", itemFromSite);

		// The JSON Array will contain all variant details
		JSONArray variants = (JSONArray) itemFromSite.get("variants");
		// Now handled only without variant that means first array element of
		// the object
		JSONObject variantsDetails = (JSONObject) variants.get(0);
		BasicDBObject baseQuery = new BasicDBObject("accountNumber", exchange.getProperty("accountNumber", String.class));
		List<SIAInventoryStatus> exclusions = new ArrayList<SIAInventoryStatus>();
		exclusions.add(SIAInventoryStatus.REMOVED);
		List<String> statusValues = getStatusValues(exclusions);
		baseQuery.put("status", new BasicDBObject("$in", statusValues));
		baseQuery.put("variantDetails", new BasicDBObject("$exists", false));
		BasicDBObject customSKUSubQuery = new BasicDBObject();
		List<BasicDBObject> orQuery = new ArrayList<BasicDBObject>();

		// customSKU match subQuery
		String customSKU = "";
		if (!variantsDetails.isNull("sku") && !variantsDetails.getString("sku").isEmpty()) {
			customSKU = variantsDetails.getString("sku");
			customSKUSubQuery = (BasicDBObject) baseQuery.copy();
			customSKUSubQuery.put("customSKU", customSKU);
			orQuery.add(customSKUSubQuery);
		}
		exchange.setProperty("customSKU", customSKU);
		// itemTitle match subQuery
		BasicDBObject itemTitleSubQuery = (BasicDBObject) baseQuery.copy();
		itemTitleSubQuery.put("itemTitle", itemFromSite.getString("title"));
		orQuery.add(itemTitleSubQuery);

		// referenceID match subQuery
		BasicDBObject referenceIdSubQuery = (BasicDBObject) baseQuery.copy();
		String refrenceID = itemFromSite.getString("id");
		exchange.setProperty("refrenceID", refrenceID);
		referenceIdSubQuery.put("shopify.refrenceID", refrenceID);
		referenceIdSubQuery.put("shopify.nickNameID", exchange.getProperty("nickNameID", String.class));
		orQuery.add(referenceIdSubQuery);
		log.debug("orQuery:" + orQuery);

		BasicDBObject projection = new BasicDBObject("shopify", 1);
		projection.put("SKU", 1);
		projection.put("customSKU", 1);
		projection.put("itemTitle", 1);

		exchange.getOut().setHeader(MongoDbConstants.FIELDS_FILTER, projection);
		exchange.getOut().setBody(new BasicDBObject("$or", orQuery));

	}

	private static List<String> getStatusValues(List<SIAInventoryStatus> exclusions) {
		SIAInventoryStatus[] a = SIAInventoryStatus.values();
		List<String> statusValues = new ArrayList<String>();
		for (int i = 0; i < a.length; i++) {
			if (!exclusions.contains(a[i])) {
				statusValues.add(a[i].toString());
			}
		}
		return statusValues;
	}

}
