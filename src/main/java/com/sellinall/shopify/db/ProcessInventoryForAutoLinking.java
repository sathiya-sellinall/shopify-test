package com.sellinall.shopify.db;

import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.sellinall.util.enums.SIAInventoryStatus;
import com.sellinall.util.enums.SIAUnlinkedInventoryStatus;

public class ProcessInventoryForAutoLinking implements Processor {
	static Logger log = Logger.getLogger(ProcessInventoryForAutoLinking.class.getName());

	public void process(Exchange exchange) throws Exception {
		List<BasicDBObject> queryResults = (List<BasicDBObject>) exchange.getIn().getBody();
		exchange.getOut().setBody(queryResults);
		
		JSONObject itemFromSite = exchange.getProperty("inBodyResponse", JSONObject.class);
		// reset autoLinkFailureReason value
		exchange.setProperty("autoLinkFailureReason", null);
		exchange.setProperty("unlinkedInventoryStatus", null);
		exchange.setProperty("hasItemInInventoryDB", false);

		log.debug("queryResult:" + queryResults);
		String nickNameID = exchange.getProperty("nickNameID", String.class);
		String customSKU = null, refrenceID = null;

		if (!exchange.getProperty("customSKU", String.class).isEmpty()) {
			customSKU = exchange.getProperty("customSKU", String.class);
		}
		if (exchange.getProperties().containsKey("refrenceID")) {
			refrenceID = exchange.getProperty("refrenceID", String.class);
		}

		// refrenceID match
		int matchCounter = getRefrenceIDMatches(exchange, queryResults, nickNameID, refrenceID);
		if (matchCounter != 0) {
			return;
		}
		List<String> autoMatch = (List<String>) exchange.getProperty("autoMatch");
		log.debug("autoMatch values : " + autoMatch);
		String matchKey = "customSKU";
		// customSKU match
		if (autoMatch != null && autoMatch.contains(matchKey)) {
			matchCounter = getMatches(exchange, queryResults, refrenceID, nickNameID, matchKey, customSKU, true/* linkToInventory */);
			if (matchCounter != 0) {
				return;
			}
		}
		matchKey = "title";
		// name match
		JSONObject unlinkedInventory = exchange.getProperty("unlinkedInventory", JSONObject.class);
		if (autoMatch != null && autoMatch.contains(matchKey)) {
			matchCounter = getMatches(exchange, queryResults, refrenceID, nickNameID, matchKey,
					unlinkedInventory.getString(matchKey), true/* linkToInventory */);
		} else {
			matchCounter = getMatches(exchange, queryResults, refrenceID, nickNameID, matchKey,
					unlinkedInventory.getString(matchKey), false/* linkToInventory */);
		}
		if (matchCounter == 0) {
			exchange.setProperty("unlinkedInventoryStatus", SIAUnlinkedInventoryStatus.NO_MATCH_FOUND.toString());
			exchange.setProperty("autoLinkFailureReason", "no match found");
		}

	}

	@SuppressWarnings("unchecked")
	private int getRefrenceIDMatches(Exchange exchange, List<BasicDBObject> queryResults, String nickNameID,
			String refrenceID) {
		int matchCounter = 0;
		if (refrenceID == null) {
			return matchCounter;
		}

		String SKU = null, logSKUs = " ", orphanImageDir = null;
		for (BasicDBObject queryResult : queryResults) {
			if (!queryResult.containsField("shopify")) {
				continue;
			}
			List<BasicDBObject> channelList = (List<BasicDBObject>) queryResult.get("shopify");
			for (BasicDBObject channel : channelList) {
				if (channel.getString("nickNameID").equals(nickNameID)) {
					if (channel.containsField("refrenceID") && channel.getString("refrenceID").equals(refrenceID)) {
						SKU = queryResult.getString("SKU");
						logSKUs = logSKUs + SKU + " ";
						if (channel.containsField("imageURI")) {
							orphanImageDir = channel.getString("imageURI");
						}
						matchCounter++;
					}
				}
			}
		}
		String failureReason = null;
		if (matchCounter == 1) {
			log.info("Match found for refrenceID " + refrenceID + ". To be overwritten " + SKU + " " + nickNameID);
			if (orphanImageDir != null) {
				log.info("potential orphanImageDir analyze for image removal: " + orphanImageDir);
			}
			exchange.setProperty("hasItemInInventoryDB", true);
			exchange.setProperty("linkToSKU", SKU);
			return matchCounter;
		} else if (matchCounter > 1) {
			failureReason = "more than one documents found with refrenceID: " + refrenceID + " " + logSKUs;
			log.error(failureReason);
			exchange.setProperty("unlinkedInventoryStatus", SIAUnlinkedInventoryStatus.MORE_THAN_ONE.toString());
			exchange.setProperty("autoLinkFailureReason", failureReason);
		} else {
			failureReason = "no match found";
			log.debug("no records found with refrenceID: " + refrenceID);
		}
		return matchCounter;
	}

	@SuppressWarnings("unchecked")
	private int getMatches(Exchange exchange, List<BasicDBObject> queryResults, String refrenceID, String nickNameID,
			String matchKey, String matchValue, boolean linkToInventory) {
		int matchCounter = 0;
		if (matchValue == null) {
			return matchCounter;
		}
		BasicDBObject matchedResult = null;
		String logInfo = " ";
		for (int index = 0; index < queryResults.size(); index++) {
			BasicDBObject queryResult = queryResults.get(index);
			logInfo = logInfo + queryResult.getString("SKU");
			String dbValue = (String) queryResult.get(matchKey);
			if (dbValue != null && dbValue.equals(matchValue)) {
				matchedResult = queryResult;
				matchCounter++;
			}
		}
		String failureReason = null;
		if (matchCounter > 1) {
			failureReason = "more than one documents found with " + matchKey + ": " + matchValue + " [list of SKUs:"
					+ logInfo + "]";
			exchange.setProperty("unlinkedInventoryStatus", SIAUnlinkedInventoryStatus.MORE_THAN_ONE.toString());
			exchange.setProperty("autoLinkFailureReason", failureReason);
			return matchCounter;
		} else if (matchCounter == 0) {
			return matchCounter;
		}

		// we know exactly one queryResult
		String SKU = matchedResult.getString("SKU");
		if (!matchedResult.containsField("shopify")) {
			log.info("Match found with no shopify object " + matchKey + ": " + matchValue);
			if (SKU.contains("-")) {
				failureReason = "Inventory matched with child record, matched child SKU : " + SKU;
				exchange.setProperty("hasItemInInventoryDB", false);
				exchange.setProperty("autoLinkFailureReason", failureReason);
				return matchCounter++;
			}
			if (linkToInventory) {
				exchange.setProperty("hasItemInInventoryDB", true);
				exchange.setProperty("linkToSKU", SKU);
				return matchCounter++;
			} else {
				failureReason = "This itemID: " + refrenceID + " inventory has match with " + matchKey + ": "
						+ matchValue + ". SKU: " + SKU;
				exchange.setProperty("unlinkedInventoryStatus", SIAUnlinkedInventoryStatus.UNLINKED.toString());
				exchange.setProperty("autoLinkFailureReason", failureReason);
				// This can be autoLinked but User didn't opted this autoMatch
				// selection
				return matchCounter++;
			}
		}
		List<BasicDBObject> channelList = (List<BasicDBObject>) matchedResult.get("shopify");
		for (BasicDBObject channel : channelList) {
			if (channel.getString("nickNameID").equals(nickNameID)
					&& SIAInventoryStatus.ACTIVE.equalsName(channel.getString("status"))) {
				failureReason = "This itemID: " + refrenceID + " inventory has match with " + matchKey + ": "
						+ matchValue + " to shopify itemID: " + channel.getString("refrenceID") + " SKU: " + SKU
						+ " which has already active inventory. Please cross check";
				exchange.setProperty("unlinkedInventoryStatus", SIAUnlinkedInventoryStatus.ALREADY_ACTIVE.toString());
				exchange.setProperty("autoLinkFailureReason", failureReason);
				return matchCounter++;
			}
		}
		if (linkToInventory) {
			exchange.setProperty("linkToSKU", SKU);
			exchange.setProperty("hasItemInInventoryDB", true);
		} else {
			failureReason = "This itemID: " + refrenceID + " inventory has match with " + matchKey + ": " + matchValue
					+ ". SKU: " + SKU;
			exchange.setProperty("unlinkedInventoryStatus", SIAUnlinkedInventoryStatus.UNLINKED.toString());
			exchange.setProperty("autoLinkFailureReason", failureReason);
		}
		return matchCounter++;
	}
}
