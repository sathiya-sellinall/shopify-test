package com.sellinall.shopify.requests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.jetty.http.HttpStatus;

import com.mongodb.BasicDBObject;
import com.sellinall.config.Config;
import com.sellinall.util.HttpsURLConnectionUtil;

public class PullCategoryID implements Processor {
	static Logger log = Logger.getLogger(PullCategoryID.class.getName());

	public void process(Exchange exchange) throws Exception {
		BasicDBObject channelDBObj = (BasicDBObject) exchange.getProperty("userShopify");
		BasicDBObject postHelper = (BasicDBObject) channelDBObj.get("postHelper");
		String url = postHelper.getString("URL");
		String apiVersion = Config.getConfig().getApiVersion();
		int recordsPerPage = Config.getConfig().getRecordsPerPage();
		String apiEndPoint = url + "/admin/api/" + apiVersion + "/collects.json?limit=" + recordsPerPage;
		ArrayList<JSONObject> arrayList = new ArrayList<JSONObject>();
		boolean nextCycleAvailable = true;
		while (nextCycleAvailable) {
			log.debug("apiEndPoint " + apiEndPoint);
			JSONObject response = HttpsURLConnectionUtil.doGetWithAuth(postHelper.getString("apiKey"),
					postHelper.getString("pass"), apiEndPoint);
			log.debug("Shopify Response=" + response);
			if (response.getInt("httpCode") != HttpStatus.OK_200) {
				exchange.setProperty("failureReason", "Internal error");
				log.error("Failed to get category List from shopify and reponse is : " + response.toString());
				return;
			}
			JSONObject responseFromSite = new JSONObject(response.getString("payload"));
			if (!responseFromSite.has("collects")) {
				exchange.setProperty("failureReason", responseFromSite.getString("errors"));
				log.error(
						"Failed to get category List  becasue collects key is not avilable in response and reposne is "
								+ responseFromSite.toString());
				return;
			}
			JSONArray collects = responseFromSite.getJSONArray("collects");
			for (int i = 0; i < collects.length(); i++) {
				arrayList.add(collects.getJSONObject(i));
			}
			apiEndPoint = getNextPageURL(new JSONObject(response.getString("headers")));
			if (apiEndPoint == null) {
				nextCycleAvailable = false;
			}
		}
		exchange.setProperty("categoryMap", parseCategoryMap(arrayList));
	}

	private HashMap<String, String> parseCategoryMap(ArrayList<JSONObject> arrayList) throws JSONException {
		HashMap<String, String> categoryMap = new HashMap<String, String>();
		for (JSONObject category : arrayList) {
			categoryMap.put("" + category.get("product_id"), "" + category.get("collection_id"));
		}
		return categoryMap;
	}

	private String getNextPageURL(JSONObject headers) throws JSONException {
		if (headers.has("Link")) {
			String linkValue = headers.getString("Link");
			String[] pageInfo = linkValue.split(",");
			String nextPageInfo = "";
			if (pageInfo.length > 1) {
				nextPageInfo = pageInfo[1];
			} else {
				nextPageInfo = pageInfo[0];
			}
			String pageDirection = nextPageInfo.split(";")[1];
			if (pageDirection.contains("next")) {
				Pattern p = Pattern.compile("(?<=<)(.+)(?=>)");
				Matcher m = p.matcher(nextPageInfo.split(";")[0]);
				while (m.find()) {
					return m.group().trim();
				}
			}
		}

		return null;
	}
}