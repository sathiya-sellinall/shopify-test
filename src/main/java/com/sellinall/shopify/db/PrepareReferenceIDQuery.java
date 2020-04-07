package com.sellinall.shopify.db;

import java.util.ArrayList;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.mongodb.MongoDbConstants;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;

public class PrepareReferenceIDQuery implements Processor {
	static Logger log = Logger.getLogger(PrepareReferenceIDQuery.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject inBody = new JSONObject(exchange.getProperty("rawData").toString());
		JSONArray line_items = inBody.getJSONArray("line_items");
		ArrayList<String> productIdList = new ArrayList<String>();
		// Each item in shopify has unique VariantID.
		// Even for variant, each child contains Unique variantID.
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		for (int i = 0; i < line_items.length(); i++) {
			JSONObject line = line_items.getJSONObject(i);
			String variantRefrenceId = line.getString("variant_id");
			productIdList.add(variantRefrenceId);
		}
		exchange.setProperty("productIdList", productIdList);
		BasicDBObject elemMatch = new BasicDBObject();
		elemMatch.put("nickNameID", exchange.getProperty("nickNameID", String.class));
		elemMatch.put("variantRefrenceId", new BasicDBObject("$in", productIdList));
		BasicDBObject query = new BasicDBObject();
		query.put("accountNumber", accountNumber);
		query.put("shopify", new BasicDBObject("$elemMatch", elemMatch));
		BasicDBObject projection = new BasicDBObject("shopify.$", 1);
		projection.put("SKU", 1);
		projection.put("itemTitle", 1);
		projection.put("itemDescription", 1);
		projection.put("imageURL", 1);
		log.debug("Fields : " + projection.toString());
		exchange.getOut().setHeader(MongoDbConstants.FIELDS_FILTER, projection);
		exchange.getOut().setBody(query);
	}
}