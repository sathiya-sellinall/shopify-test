package com.sellinall.shopify.response;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.sellinall.config.Config;
import com.sellinall.util.AuthConstant;
import com.sellinall.util.HttpsURLConnectionUtil;

public class ProcessImageURL implements Processor {
	static Logger log = Logger.getLogger(ProcessImageURL.class.getName());

	public void process(Exchange exchange) throws Exception {
		String uploadImageToSellInAllURL = Config.getConfig().getUploadImageToSellInAllUrl();
		String merchantID = exchange.getProperty("merchantID", String.class);
		log.debug("merchantID: " + merchantID);
		Set<String> imageSet = exchange.getProperty("imageSet", Set.class);
		JSONObject imagesPayload = new JSONObject();
		imagesPayload.put("merchantID", merchantID);
		imagesPayload.put("uniqueUploadID", exchange.getProperty("unlinkedInventorySKU", String.class));
		imagesPayload.put("url", imageSet);
		log.debug("imagesPayload:" + imagesPayload);
		Map<String, String> config = new LinkedHashMap<String, String>();
		config.put("Content-Type", "application/json");
		config.put(AuthConstant.RAGASIYAM_KEY, Config.getConfig().getRagasiyam());
		boolean isNewRecord = exchange.getProperty("isNewItem", Boolean.class);
		if (!isNewRecord) {
			// If it is already exist then we will delete existing image and re
			// upload new images
			imagesPayload.put("reset", 1);
		}
		String uploadResponsePayload = "";
		exchange.setProperty("isImageUploadedSuccessfully", true);
		try {
			JSONObject imageUploadResponse = HttpsURLConnectionUtil.doPut(uploadImageToSellInAllURL, imagesPayload.toString(),
					config);
			uploadResponsePayload = imageUploadResponse.has("payload")
					? imageUploadResponse.getString("payload") : "";
		} catch (Exception e) {
			exchange.setProperty("isImageUploadedSuccessfully", false);
			log.error("Invalid image URL : " + imageSet);
			return;
		}

		if (uploadResponsePayload.isEmpty()) {
			exchange.setProperty("isImageUploadedSuccessfully", false);
			log.error("Upload image reponse empty for " + imagesPayload);
			return;
		}
		Map<String, String> imageMap = new LinkedHashMap<String, String>();
		try {
			JSONObject uploadResponse = new JSONObject(uploadResponsePayload);
			log.debug("uploadResponse:" + uploadResponse);
			JSONArray imageArray = uploadResponse.getJSONArray("response");
			for (int i = 0; i < imageArray.length(); i++) {
				JSONObject imageItem = imageArray.getJSONObject(i);
				imageMap.put(imageItem.getString("input"), imageItem.getString("output"));
			}
		} catch (Exception e) {
			exchange.setProperty("isImageUploadedSuccessfully", false);
			log.error("Upload image reponse : " + imagesPayload);
			log.error(e);
		}
		log.debug("imageMap:" + imageMap);
		exchange.setProperty("imageMap", imageMap);
	}
}