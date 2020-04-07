package com.sellinall.shopify.util;

import java.util.Arrays;
import java.util.List;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.sellinall.util.enums.SIAOrderCancelReasons;

public class ShopifyUtil {

	private static final List<SIAOrderCancelReasons> cancelReasonList = Arrays.asList(
			SIAOrderCancelReasons.CUSTOMER_CHANGED_OR_CANCELED_ORDER, SIAOrderCancelReasons.FRAUDULENT_ORDER,
			SIAOrderCancelReasons.OUT_OF_STOCK, SIAOrderCancelReasons.PAYMENT_DECLINED, SIAOrderCancelReasons.OTHERS,
			SIAOrderCancelReasons.REFUND_WITH_AMOUNT);

	public static final List<SIAOrderCancelReasons> getCancelReasonList() {
		return cancelReasonList;
	}

	public static String getPageInfoLink(JSONObject headers) throws JSONException {
		if (headers.has("Link")) {
			if (headers.getString("Link").toLowerCase().contains("next")) {
				String[] splittedLink = headers.getString("Link").split(",");
				for (String splitLink : splittedLink) {
					if (splitLink.toLowerCase().contains("next")) {
						String[] pageInfoArray = splitLink.split(";");
						return pageInfoArray[0].substring(pageInfoArray[0].indexOf("<") + 1,
								pageInfoArray[0].indexOf(">"));
					}
				}
			}
		}
		return null;
	}
}
