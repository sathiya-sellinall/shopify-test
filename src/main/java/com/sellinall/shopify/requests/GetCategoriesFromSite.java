package com.sellinall.shopify.requests;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.sellinall.config.Config;
import com.sellinall.shopify.util.ShopifyUtil;
import com.sellinall.util.AuthConstant;
import com.sellinall.util.HttpsURLConnectionUtil;

public class GetCategoriesFromSite implements Processor {

	public void process(Exchange exchange) throws Exception {
		BasicDBObject userDetails = exchange.getProperty("UserDetails", BasicDBObject.class);
		String apiVersion = Config.getConfig().getApiVersion();
		String apikey = "";
		String password = "";
		String shopUrl = "";
		String nickNameID = exchange.getProperty("nickNameID", String.class);
		String channelName = nickNameID.split("-")[0];
		ArrayList<DBObject> channelDetails = (ArrayList<DBObject>) userDetails.get(channelName);
		String countryCode = "";
		for (DBObject channelDetail : channelDetails) {
			String siteNickId = (String) ((DBObject) channelDetail.get("nickName")).get("id");
			countryCode = "";
			if (channelDetail.containsField("countryCode")) {
				countryCode = (String) channelDetail.get("countryCode");
			}
			if (nickNameID.equals(siteNickId)) {
				countryCode = (String) channelDetail.get("countryCode");
				BasicDBObject postHelper = (BasicDBObject) channelDetail.get("postHelper");
				apikey = postHelper.getString("apiKey");
				password = postHelper.getString("pass");
				shopUrl = postHelper.getString("URL");
				break;
			}
		}
		int defaultPageLimit = Config.getConfig().getRecordsPerPage();
		ArrayList<String> shopifyCategorySiaFormat = new ArrayList<String>();
		String pageInfoLink = null;
		do {
			String url = shopUrl + "/admin/api/" + apiVersion + "/custom_collections.json?limit=" + defaultPageLimit;
			JSONObject response = new JSONObject();
			if (pageInfoLink != null) {
				url = pageInfoLink;
			}
			response = HttpsURLConnectionUtil.doGetWithAuth(apikey, password, url);
			JSONObject categoryParseToJson = new JSONObject(response.getString("payload"));
			if (categoryParseToJson.has("custom_collections")) {
				JSONArray categoryList = categoryParseToJson.getJSONArray("custom_collections");
				parseCategoryToSIAFormat(categoryList, shopifyCategorySiaFormat);
			}
			pageInfoLink = ShopifyUtil.getPageInfoLink(new JSONObject(response.getString("headers")));

		} while (pageInfoLink != null);
		callCategoriesUpload(shopifyCategorySiaFormat, userDetails.getString("merchantID"), nickNameID, countryCode);
	}

	private void parseCategoryToSIAFormat(JSONArray categoryList, ArrayList<String> shopifyCategorySiaFormat)
			throws JSONException {
		for (int j = 0; j < categoryList.length(); j++) {
			JSONObject category = (JSONObject) categoryList.get(j);
			shopifyCategorySiaFormat.add(category.getString("title") + " ## " + category.getString("id"));
		}
	}

	private void callCategoriesUpload(ArrayList<String> categoryList, String merchantID, String nickNameID,
			String countryCode) throws JSONException, IOException {
		JSONObject categoryUploadPayload = new JSONObject();
		categoryUploadPayload.put("merchantID", merchantID);
		categoryUploadPayload.put("categoryList", categoryList);
		categoryUploadPayload.put("channelName", "shopify");
		categoryUploadPayload.put("nickNameId", nickNameID);
		categoryUploadPayload.put("countryCode", countryCode);
		Map<String, String> config = new HashMap<String, String>();
		config.put("Content-Type", "application/json");
		config.put(AuthConstant.RAGASIYAM_KEY, Config.getConfig().getRagasiyam());
		HttpsURLConnectionUtil.doPut(Config.getConfig().getUploadCategories(), categoryUploadPayload.toString(), config);
	}
}
