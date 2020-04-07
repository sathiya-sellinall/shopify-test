package com.sellinall.shopify.response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.sellinall.config.Config;
import com.sellinall.util.AuthConstant;
import com.sellinall.util.HttpsURLConnectionUtil;

public class ParseCategories implements Processor {
	static Logger log = Logger.getLogger(ParseCategories.class.getName());

	public void process(Exchange exchange) throws Exception {
		ArrayList<String> shopifyCategorySiaFormat = new ArrayList<String>();
		JSONObject categoryList = (JSONObject) exchange.getIn().getBody();
		BasicDBObject userDetails = exchange.getProperty("UserDetails", BasicDBObject.class);
		if (!categoryList.get("categories").equals(null)) {
			JSONArray categories = categoryList.getJSONArray("categories");
			for (int i = 0; i < categories.length(); i++) {
				JSONObject childrenCategory = (JSONObject) categories.get(i);
				shopifyCategorySiaFormat
						.add(childrenCategory.getString("name") + " ## " + childrenCategory.getString("category_id"));
				if (!childrenCategory.get("categories").equals(null)) {
					parseChildren(childrenCategory.getJSONArray("categories"), childrenCategory.getString("name"),
							shopifyCategorySiaFormat);
				}
			}
			callCategoriesUpload(shopifyCategorySiaFormat, userDetails.getString("merchantID"),
					exchange.getProperty("nickNameID", String.class),
					exchange.getProperty("countryCode", String.class));
		}
	}

	private void parseChildren(JSONArray childrenCategory, String parentCategory,
			ArrayList<String> shopifyCategorySiaFormat) throws JSONException {
		for (int j = 0; j < childrenCategory.length(); j++) {
			JSONObject childrenSubCategory = (JSONObject) childrenCategory.get(j);
			String categoryName = parentCategory + ">" + childrenSubCategory.getString("name");
			shopifyCategorySiaFormat.add(categoryName + " ## " + childrenSubCategory.getString("category_id"));
			if (!childrenSubCategory.get("categories").equals(null)) {
				parseChildren(childrenSubCategory.getJSONArray("categories"), categoryName,
						shopifyCategorySiaFormat);
			}
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
