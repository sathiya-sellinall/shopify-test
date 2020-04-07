package com.sellinall.shopify.requests;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.jetty.http.HttpStatus;

import com.mongodb.BasicDBObject;
import com.sellinall.config.Config;
import com.sellinall.shopify.util.ShopifyUtil;
import com.sellinall.util.HttpsURLConnectionUtil;

public class PullShopifyInventoryDetails implements Processor {
	static Logger log = Logger.getLogger(PullShopifyInventoryDetails.class.getName());

	public void process(Exchange exchange) throws Exception {
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		String nickNameID = exchange.getProperty("nickNameID", String.class);
		String SKU = exchange.getProperty("SKU", String.class);
		BasicDBObject postHelper = exchange.getProperty("postHelper", BasicDBObject.class);
		String url = postHelper.getString("URL");
		List<String> inventoryItemIdList = new ArrayList<String>();
		if (exchange.getProperty("inventoryItemIdListMap") != null) {
			Map<String, JSONObject> inventoryItemIdListMap = exchange.getProperty("inventoryItemIdListMap", Map.class);
			inventoryItemIdList = new ArrayList<>(inventoryItemIdListMap.keySet());
		} else {
			BasicDBObject shopifyObject = (BasicDBObject) exchange.getProperty("inventory", BasicDBObject.class)
					.get("shopify");
			inventoryItemIdList.add(shopifyObject.getString("inventoryItemID"));
		}
		int recordsPerPage = Config.getConfig().getRecordsPerPage();
		int pageCount = 1;
		if (inventoryItemIdList.size() > recordsPerPage) {
			pageCount = inventoryItemIdList.size() / pageCount;
			if (inventoryItemIdList.size() % pageCount > 0) {
				pageCount++;
			}
		}
		ArrayList<JSONObject> inventoryLevelsDetails = new ArrayList<JSONObject>();
		for (int i = 0; i < pageCount; i = i + recordsPerPage) {
			List<String> subInventoryIdList = inventoryItemIdList.stream().skip(i).limit(recordsPerPage)
					.collect(Collectors.toList());
			String inventoryItemIDs = StringUtils.join(subInventoryIdList, ",");
			String pageInfoLink = null;
			do {
				String apiUrl = url + "/admin/api/" + Config.getConfig().getApiVersion()
						+ "/inventory_levels.json?inventory_item_ids=" + inventoryItemIDs + "&limit=" + recordsPerPage;
				if (pageInfoLink != null) {
					apiUrl = pageInfoLink;
				}

				JSONObject response = HttpsURLConnectionUtil.doGetWithAuth(postHelper.getString("apiKey"),
						postHelper.getString("pass"), apiUrl);
				if (response.getInt("httpCode") != HttpStatus.OK_200) {
					exchange.setProperty("failureReason", "Failed to pull warehouse quantity details  from shopify");
					log.error("Failed to pull warehouse quantity details  from shopify for inventory item id - "
							+ inventoryItemIDs + ",accountNumber-" + accountNumber + ",nickNameID" + nickNameID
							+ ",SKU " + SKU + "and response is - " + response.toString());
				}
				JSONObject responseFromSite = new JSONObject(response.getString("payload"));
				if (!responseFromSite.has("inventory_levels")) {
					exchange.setProperty("failureReason",
							"Failed to pull warehouse quantity details  from shopify beacause inventory_levels key not found");
					log.error(
							"Failed to pull warehouse quantity details  from shopify beacause inventory_levels key not"
									+ " found in response for inventory item id - " + inventoryItemIDs
									+ " ,accountNumber-" + accountNumber + ",nickNameID" + nickNameID + ",SKU " + SKU
									+ "and response is - " + response.toString());
				} else {
					JSONArray inventoryLevel = responseFromSite.getJSONArray("inventory_levels");
					for (int j = 0; j < inventoryLevel.length(); j++) {
						inventoryLevelsDetails.add(inventoryLevel.getJSONObject(j));
					}
					pageInfoLink = ShopifyUtil.getPageInfoLink(new JSONObject(response.getString("headers")));
				}
			} while (pageInfoLink != null);
		}
		exchange.setProperty("inventoryLevelsDetails", inventoryLevelsDetails);
	}
}