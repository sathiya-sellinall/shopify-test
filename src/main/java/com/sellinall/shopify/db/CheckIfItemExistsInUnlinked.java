package com.sellinall.shopify.db;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.mongodb.MongoDbConstants;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;

import com.mongodb.BasicDBObject;
import com.sellinall.util.enums.SIAUnlinkedInventoryStatus;

public class CheckIfItemExistsInUnlinked implements Processor {

	static Logger log = Logger.getLogger(CheckIfItemExistsInUnlinked.class.getName());

	public void process(Exchange exchange) throws Exception {
		log.debug("Inside check is item exist in unlinked ");
		buildHeaderAndQuery(exchange);
	}

	private void buildHeaderAndQuery(Exchange exchange) throws JSONException {
		BasicDBObject projection = new BasicDBObject("shopify", 1);
		projection.put("SKU", 1);
		projection.put("customSKU", 1);
		projection.put("itemTitle", 1);
		log.debug("Fields : " + projection.toString());
		exchange.getOut().setHeader(MongoDbConstants.FIELDS_FILTER, projection);
		exchange.getOut().setBody(buildSearchQuery(exchange));
	}

	private BasicDBObject buildSearchQuery(Exchange exchange) throws JSONException {
		BasicDBObject elemMatch = new BasicDBObject();
		BasicDBObject searchQuery = new BasicDBObject();
		elemMatch.put("refrenceID", exchange.getProperty("refrenceID", String.class));
		elemMatch.put("nickNameID", exchange.getProperty("nickNameID", String.class));
		BasicDBObject searchChannel = new BasicDBObject("$elemMatch", elemMatch);
		searchQuery.put("accountNumber", exchange.getProperty("accountNumber", String.class));
		searchQuery.put("shopify", searchChannel);
		searchQuery.put("variantDetails", new BasicDBObject("$exists", false));
		searchQuery.put("status", new BasicDBObject("$ne", SIAUnlinkedInventoryStatus.REMOVED.toString()));
		List<SIAUnlinkedInventoryStatus> exclusions = new ArrayList<SIAUnlinkedInventoryStatus>();
		exclusions.add(SIAUnlinkedInventoryStatus.REMOVED);
		List<String> statusValues = getStatusValues(exclusions);
		searchQuery.put("status", new BasicDBObject("$in", statusValues));
		log.debug("Search query for unlinked : " + searchQuery.toString());
		return searchQuery;
	}

	private static List<String> getStatusValues(List<SIAUnlinkedInventoryStatus> exclusions) {
		SIAUnlinkedInventoryStatus[] a = SIAUnlinkedInventoryStatus.values();
		List<String> statusValues = new ArrayList<String>();
		for (int i = 0; i < a.length; i++) {
			if (!exclusions.contains(a[i])) {
				statusValues.add(a[i].toString());
			}
		}
		return statusValues;
	}
}
