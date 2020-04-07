package com.sellinall.shopify.requests;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.jetty.http.HttpStatus;

import com.mongodb.BasicDBObject;
import com.sellinall.config.Config;
import com.sellinall.util.HttpsURLConnectionUtil;

public class GetCategoryName implements Processor {
	static Logger log = Logger.getLogger(GetCategoryName.class.getName());

	public void process(Exchange exchange) throws Exception {
		if (!exchange.getProperties().containsKey("categoryID")
				|| exchange.getProperty("categoryID", String.class).isEmpty()) {
			return;
		}
		try {
			String apiVerison = Config.getConfig().getApiVersion();
			String categoryID = exchange.getProperty("categoryID", String.class);
			BasicDBObject userShopify = (BasicDBObject) exchange.getProperty("userShopify");
			BasicDBObject postHelper = (BasicDBObject) userShopify.get("postHelper");
			String url = postHelper.getString("URL");
			url = url + "/admin/api/" + apiVerison + "/custom_collections/" + categoryID + ".json";
			JSONObject responseFromSite = HttpsURLConnectionUtil.doGetWithAuth(postHelper.getString("apiKey"),
					postHelper.getString("pass"), url);
			if (responseFromSite.getInt("httpCode") != HttpStatus.OK_200) {
				exchange.setProperty("failureReason", "Internal error");
				log.error("Failed to get category name from shopify");
				return;
			}
			JSONObject response = new JSONObject(responseFromSite.getString("payload"));
			JSONObject customCollection = response.getJSONObject("custom_collection");
			String categoryName = customCollection.getString("title") + " ## " + categoryID;
			exchange.setProperty("categoryName", categoryName);
		} catch (Exception e) {
			log.error("error while getting categoryName");
		}
	}
}
