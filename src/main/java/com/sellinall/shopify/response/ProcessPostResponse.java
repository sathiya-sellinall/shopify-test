package com.sellinall.shopify.response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.jetty.http.HttpStatus;

import com.mongodb.BasicDBObject;
import com.sellinall.util.enums.SIAInventoryStatus;
import com.sellinall.util.enums.SIAInventoryUpdateStatus;

public class ProcessPostResponse implements Processor {
	static Logger log = Logger.getLogger(ProcessPostResponse.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject responseObj = exchange.getIn().getBody(JSONObject.class);
		exchange.setProperty("status", SIAInventoryStatus.FAILED.toString());
		String requestType = (String) exchange.getProperty("requestType");
		// this filed using quantity update purpose
		boolean isItemPosted = false;
		List<BasicDBObject> updateObjectList = new ArrayList<BasicDBObject>();
		int httpStatusCode = responseObj.getInt("httpCode");
		String failureReason = "";
		if (httpStatusCode != HttpStatus.OK_200 && httpStatusCode != HttpStatus.CREATED_201) {
			failureReason = "Invalid request";
			if (httpStatusCode == HttpStatus.BAD_GATEWAY_502) {
				failureReason = "Retry after some time.";
			}
			if (httpStatusCode == HttpStatus.BAD_REQUEST_400) {
				failureReason = "check your item input details";
			}
			exchange.setProperty("isItemPosted", isItemPosted);
			exchange.setProperty("failureReason", failureReason);
			updateFailureStatus(requestType, exchange, updateObjectList);
			exchange.getOut().setBody(updateObjectList);
			exchange.setProperty("updateObjectList", updateObjectList);
			log.error("Response failed with statuscode: " + httpStatusCode + " for accountNumber: "
					+ exchange.getProperty("accountNumber") + ", SKU: " + exchange.getProperty("SKU") + " and response:"
					+ responseObj.toString());
			return;
		}
		String response = responseObj.getString("payload");
		log.debug("Received Response: " + response);
		JSONObject jsonResponse = new JSONObject(response);
		if (!jsonResponse.has("errors")) {
			isItemPosted = true;
			parseResponse(response, exchange, requestType, updateObjectList);
		} else {
			if (jsonResponse.get("errors") instanceof JSONObject) {
				JSONObject getErrorObject = (JSONObject) jsonResponse.get("errors");
				if (getErrorObject.has("base")) {
					JSONArray array = getErrorObject.getJSONArray("base");
					for (int i = 0; i < array.length(); i++) {
						failureReason += (String) array.get(i) + "\n";
					}
				} else {
					log.error("Response failed with statuscode: " + httpStatusCode + " for accountNumber: "
							+ exchange.getProperty("accountNumber") + ", SKU: " + exchange.getProperty("SKU")
							+ " and response:" + response);
				}
			} else {
				failureReason = jsonResponse.getString("errors");
			}
			exchange.setProperty("failureReason", failureReason);
			updateFailureStatus(requestType, exchange, updateObjectList);
		}
		exchange.setProperty("isItemPosted", isItemPosted);
		exchange.getOut().setBody(updateObjectList);
		exchange.setProperty("updateObjectList", updateObjectList);
	}

	private void parseResponse(String responseString, Exchange exchange, String requestType,
			List<BasicDBObject> updateObjectList) throws JSONException {
		try {
			ArrayList<BasicDBObject> inventoryList = (ArrayList<BasicDBObject>) exchange.getProperty("inventoryList");
			JSONObject response = new JSONObject(responseString);
			BasicDBObject updateInventoryObject = new BasicDBObject();
			Map <String, String> skuInventoryItemIDMap = new HashMap<String, String>();
			if (requestType.equals("updateItem")) {
				updateInventoryObject.put("SKU", exchange.getProperty("SKU", String.class));
				updateInventoryObject.put("shopify.$.failureReason", "");
				updateInventoryObject.put("shopify.$.updateStatus", SIAInventoryUpdateStatus.COMPLETED.toString());
				updateObjectList.add(updateInventoryObject);
			} else if (requestType.equals("batchEditItem")) {
				parseBatchEditResponse(exchange, response, inventoryList, updateObjectList, skuInventoryItemIDMap);
			} else {
				BasicDBObject userShopify = (BasicDBObject) exchange.getProperty("userShopify");
				BasicDBObject postHelper = (BasicDBObject) userShopify.get("postHelper");
				JSONObject product = response.getJSONObject("product");
				exchange.setProperty("productId", product.getString("id"));
				String itemURL = postHelper.getString("URL");
				itemURL = itemURL + "/products/" + product.getString("handle");
				JSONArray shopifyVariantArray = product.getJSONArray("variants");
				BasicDBObject updateObject = new BasicDBObject();
				if (!exchange.getProperty("hasVariants", boolean.class)) {
					String SKU = inventoryList.get(0).getString("SKU");
					updateObject.put("SKU", SKU);
					updateObject.put("shopify.$.itemUrl", itemURL);
					updateObject.put("shopify.$.refrenceID", product.getString("id"));
					JSONObject shopifyVariant = shopifyVariantArray.getJSONObject(0);
					updateObject.put("shopify.$.variantRefrenceId", shopifyVariant.getString("id"));
					if (shopifyVariant.has("inventory_item_id")) {
						skuInventoryItemIDMap.put(SKU, shopifyVariant.getString("inventory_item_id"));
						updateObject.put("shopify.$.inventoryItemID",
								shopifyVariant.getString("inventory_item_id"));
					}
					updateObject.put("shopify.$.status", SIAInventoryStatus.ACTIVE.toString());
					updateObject.put("shopify.$.failureReason", "");
					updateObjectList.add(updateObject);
				} else {
					//For variant each child has unique variantRefrenceID given shopify which is used for update,delete.
					// we need to store each unique variantRefrenceID with respective SKU childs in our system
					for (BasicDBObject inventory : inventoryList) {
						if (!inventory.containsField("variants")) {
							updateObject = new BasicDBObject();
							ArrayList<BasicDBObject> variantDetails = (ArrayList<BasicDBObject>) inventory
									.get("variantDetails");
							String name = "";
							for (BasicDBObject variantDetail : variantDetails) {
								// add "/" if variants has more than one
								name = name + variantDetail.getString("name")
										+ ((variantDetails.indexOf(variantDetail) < variantDetails.size() - 1) ? "/"
												: "");
							}
							updateObject.put("shopify.$.itemUrl", itemURL);
							updateObject.put("shopify.$.refrenceID", product.getString("id"));
							updateObject.put("shopify.$.status", SIAInventoryStatus.ACTIVE.toString());
							updateObject.put("shopify.$.failureReason", "");
							for (int index = 0; index < shopifyVariantArray.length(); index++) {
								// Compare variant details with shopify variant
								// detail which is given in response
								// Example: red/L(SIA) is equal to red/L(shopify)
								JSONObject shopifyVariantDetails = shopifyVariantArray.getJSONObject(index);
								String title = StringEscapeUtils
										.unescapeJava((shopifyVariantDetails.getString("title").replaceAll(" ", "").trim()));
								if (title.equals(name)) {
									String SKU = inventory.getString("SKU");
									updateObject.put("SKU", SKU);
									updateObject.put("shopify.$.variantRefrenceId", shopifyVariantDetails.getString("id"));
									if (shopifyVariantDetails.has("inventory_item_id")) {
										skuInventoryItemIDMap.put(SKU, shopifyVariantDetails.getString("inventory_item_id"));
										updateObject.put("shopify.$.inventoryItemID",
												shopifyVariantDetails.getString("inventory_item_id"));
									}
									updateObjectList.add(updateObject);
								}
							}
						}
					}
					// Update url and refrenceID to parent inventory finally.
					updateObject = new BasicDBObject();
					updateObject.put("SKU", exchange.getProperty("SKU", String.class).split("-")[0]);
					updateObject.put("shopify.$.itemUrl", itemURL);
					updateObject.put("shopify.$.refrenceID", product.getString("id"));
					updateObject.put("shopify.$.status", SIAInventoryStatus.ACTIVE.toString());
					updateObject.put("shopify.$.failureReason", "");
					updateObjectList.add(updateObject);
				}
			}
			exchange.setProperty("skuInventoryItemIDMap", skuInventoryItemIDMap);
		} catch (Exception e) {
			exchange.setProperty("failureReason", "Internal Error");
			log.info("response: "+responseString);
			e.printStackTrace();
			updateFailureStatus(requestType, exchange, updateObjectList);
		}
	}

	private void parseBatchEditResponse(Exchange exchange, JSONObject response, ArrayList<BasicDBObject> inventoryList,
			List<BasicDBObject> updateObjectList, Map <String, String> skuInventoryItemIDMap ) throws JSONException {
		JSONObject product = response.getJSONObject("product");
		BasicDBObject updateObject = new BasicDBObject();
		JSONArray shopifyVariantArray = product.getJSONArray("variants");
		if (!exchange.getProperty("hasVariants", boolean.class)) {
			// Non varaint
			String SKU = inventoryList.get(0).getString("SKU");
			updateObject.put("SKU", SKU);
			JSONObject shopifyVariant = shopifyVariantArray.getJSONObject(0);
			updateObject.put("shopify.$.variantRefrenceId", shopifyVariant.getString("id"));
			if (shopifyVariant.has("inventory_item_id")) {
				updateObject.put("shopify.$.inventoryItemID", shopifyVariant.getString("inventory_item_id"));
				skuInventoryItemIDMap.put(SKU ,shopifyVariant.getString("inventory_item_id"));
			}
			updateObject.put("shopify.$.failureReason", "");
			updateObjectList.add(updateObject);
		} else {
			for (BasicDBObject inventory : inventoryList) {
				if (!inventory.containsField("variants")) {
					updateObject = new BasicDBObject();
					ArrayList<BasicDBObject> variantDetails = (ArrayList<BasicDBObject>) inventory
							.get("variantDetails");
					String name = "";
					for (BasicDBObject variantDetail : variantDetails) {
						name = name + variantDetail.getString("name")
								+ ((variantDetails.indexOf(variantDetail) < variantDetails.size() - 1) ? "/" : "");
					}
					for (int index = 0; index < shopifyVariantArray.length(); index++) {
						JSONObject shopifyVariantDetails = shopifyVariantArray.getJSONObject(index);
						String title = StringEscapeUtils
								.unescapeJava((shopifyVariantDetails.getString("title").replaceAll(" ", "").trim()));
						if (title.equals(name)) {
							String SKU = inventory.getString("SKU");
							updateObject.put("SKU", SKU);
							updateObject.put("shopify.$.variantRefrenceId", shopifyVariantDetails.getString("id"));
							if (shopifyVariantDetails.has("inventory_item_id")) {
								skuInventoryItemIDMap.put(SKU, shopifyVariantDetails.getString("inventory_item_id"));
								updateObject.put("shopify.$.inventoryItemID",
										shopifyVariantDetails.getString("inventory_item_id"));
							}
							updateObject.put("shopify.$.failureReason", "");
							updateObjectList.add(updateObject);
						}
					}
				}
			}
			// update parent inventory
			updateObject = new BasicDBObject();
			updateObject.put("SKU", exchange.getProperty("SKU", String.class).split("-")[0]);
			updateObject.put("shopify.$.failureReason", "");
			updateObjectList.add(updateObject);
		}

	}

	private void updateFailureStatus(String requestType, Exchange exchange, List<BasicDBObject> updateList) {
		BasicDBObject updateInventoryObject = new BasicDBObject();
		updateInventoryObject.put("SKU", exchange.getProperty("SKU"));
		if (requestType.equals("addItem") || requestType.equals("batchAddItem")) {
			updateInventoryObject.put("shopify.$.status", SIAInventoryStatus.FAILED.toString());
		} else {
			updateInventoryObject.put("shopify.$.updateStatus", SIAInventoryUpdateStatus.FAILED.toString());
		}
		updateInventoryObject.put("shopify.$.failureReason", exchange.getProperty("failureReason", String.class));
		updateList.add(updateInventoryObject);
	}
}