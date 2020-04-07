package com.sellinall.shopify.db;

import java.util.LinkedList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.bson.types.ObjectId;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.sellinall.database.DbUtilities;

/**
 * @author Samy
 * 
 */
public class LoadAccountDataByNicknameId implements Processor {

	@SuppressWarnings("unchecked")
	public void process(Exchange exchange) throws Exception {
		String nickNameId = exchange.getProperty("nickNameID", String.class);
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		DBObject queryResult = runQuery(accountNumber, nickNameId);
		List<BasicDBObject> channelList = (List<BasicDBObject>) queryResult.get("shopify");
		BasicDBObject channelObj = channelList.get(0);
		exchange.setProperty("siteId", channelObj.getString("siteId"));
		exchange.setProperty("merchantID", queryResult.get("merchantID"));
		exchange.setProperty("shopifyURL", channelObj.getString("URL"));
		exchange.setProperty("userShopify", channelObj);
		exchange.setProperty("autoMatch", queryResult.get("autoMatch"));
		if (channelObj.containsField("extractItemSpecificsFromDescription")) {
			exchange.setProperty("extractItemSpecificsFromDescription",
					channelObj.getBoolean("extractItemSpecificsFromDescription"));
		}

		List<String> activeLocationIdList = new  LinkedList<String>();
		if (channelObj.containsField("storePickUpDetails")) {
			BasicDBObject storePickUpDetails = (BasicDBObject) channelObj.get("storePickUpDetails");
			if (storePickUpDetails.containsField("pickUpAddressDetails")) {
				List<BasicDBObject> pickUpAddressDetails = (List<BasicDBObject>) storePickUpDetails
						.get("pickUpAddressDetails");
				for (BasicDBObject pickUpAddress : pickUpAddressDetails) {
					if (pickUpAddress.getBoolean("active")) {
						activeLocationIdList.add(pickUpAddress.getString("locationId"));
					}
				}
			}
		}

		BasicDBObject postHelper = (BasicDBObject) channelObj.get("postHelper");
		exchange.setProperty("apiKey", postHelper.getString("apiKey"));
		exchange.setProperty("postHelper", postHelper);
		exchange.setProperty("password	", postHelper.getString("pass"));
		exchange.setProperty("URL", postHelper.getString("URL"));
		exchange.setProperty("activeLocationIdList", activeLocationIdList);

	}

	private DBObject runQuery(String accountNumber, String nickNameId) {
		BasicDBObject elemMatch = new BasicDBObject();
		elemMatch.put("nickName.id", nickNameId);
		BasicDBObject shopify = new BasicDBObject("$elemMatch", elemMatch);
		ObjectId objId = new ObjectId(accountNumber);
		BasicDBObject searchQuery = new BasicDBObject("_id", objId);
		searchQuery.put("shopify", shopify);

		BasicDBObject projection = new BasicDBObject("shopify.$", 1);
		projection.put("merchantID", 1);
		projection.put("autoMatch", 1);
		DBCollection table = DbUtilities.getDBCollection("accounts");
		BasicDBObject shopifyObj = (BasicDBObject) table.findOne(searchQuery, projection);

		return shopifyObj;
	}
}