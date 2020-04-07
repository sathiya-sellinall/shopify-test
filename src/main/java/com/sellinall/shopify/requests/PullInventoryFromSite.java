package com.sellinall.shopify.requests;

import java.util.ArrayList;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.jetty.http.HttpStatus;

import com.mongodb.BasicDBObject;
import com.sellinall.config.Config;
import com.sellinall.util.HttpsURLConnectionUtil;

public class PullInventoryFromSite implements Processor {
	static Logger log = Logger.getLogger(PullInventoryFromSite.class.getName());

	public void process(Exchange exchange) throws Exception {
		BasicDBObject channelDBObj = (BasicDBObject) exchange.getProperty("userShopify");
		BasicDBObject postHelper = (BasicDBObject) channelDBObj.get("postHelper");
		int pageNumber = exchange.getProperty("pageNumber", Integer.class);
		int recordsPerPage = Config.getConfig().getRecordsPerPage();
		String apiVersion = Config.getConfig().getApiVersion();
		int noOfPages = exchange.getProperty("numberOfPages", Integer.class);
		if (noOfPages == pageNumber) {
			int totalNoOfRecord = exchange.getProperty("numberOfRecords", Integer.class);
			int recordslastPage = totalNoOfRecord % recordsPerPage;
			if (recordslastPage != 0) {
				recordsPerPage = recordslastPage;
			}
		}
		boolean stopProcess = false;
		String url = postHelper.getString("URL");
		String restGetProductEndPoint = "";
		if (exchange.getProperty("pageInfo") == null) {
			restGetProductEndPoint = url + "/admin/api/"+apiVersion+"/products.json?limit=" + recordsPerPage
					+ "&order=updated_at+desc";
		} else {
			restGetProductEndPoint = url + "/admin/api/"+apiVersion+"/products.json?limit=" + recordsPerPage + "&page_info="
					+ exchange.getProperty("pageInfo", String.class);
		}
		log.debug("restGetProductEndPoint" + restGetProductEndPoint);
		JSONObject response = HttpsURLConnectionUtil.doGetWithAuth(postHelper.getString("apiKey"),
				postHelper.getString("pass"), restGetProductEndPoint);
		log.debug("Shopify Response=" + response.getInt("httpCode") + " " + response);
		if (response.getInt("httpCode") != HttpStatus.OK_200) {
			exchange.setProperty("failureReason", "Internal error");
			log.error("Failed inventory pull from shopify");
			return;
		}
		JSONObject responseFromSite = new JSONObject(response.getString("payload"));
		if (!responseFromSite.has("products")) {
			exchange.setProperty("failureReason", responseFromSite.getString("errors"));
			log.error("No inventory items found");
			return;
		}
		JSONObject headers = new JSONObject(response.getString("headers"));
		if (headers.has("Link")) {
			if (headers.getString("Link").toLowerCase().contains("next")) {
				String[] splittedLink = headers.getString("Link").split(",");
				for (String splitLink : splittedLink) {
					if (splitLink.toLowerCase().contains("next")) {
						String[] pageInfoArray = splitLink.split(";");
						String pageInfo = pageInfoArray[0].substring(pageInfoArray[0].indexOf("<") + 1,
								pageInfoArray[0].indexOf(">"));
						exchange.setProperty("pageInfo", pageInfo.split("page_info=")[1]);
					}
				}
			} else {
				stopProcess = true;
			}
		}

		ArrayList<JSONObject> arrayList = new ArrayList<JSONObject>();
		JSONArray products = responseFromSite.getJSONArray("products");
		for (int i = 0; i < products.length(); i++) {
			arrayList.add(products.getJSONObject(i));
		}
		exchange.setProperty("pulledInventoryList", arrayList);
		exchange.setProperty("totalItemsInCurrentPage", arrayList.size());
		exchange.setProperty("itemListIndex", 0);
		exchange.setProperty("stopProcess", stopProcess);

	}

}