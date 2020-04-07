/**
 * 
 */
package com.sellinall.shopify.init;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * @author vikraman
 * 
 */
public class InitializeRoute implements Processor {
	static Logger log = Logger.getLogger(InitializeRoute.class.getName());

	@SuppressWarnings("unchecked")
	public void process(Exchange exchange) throws Exception {
		BasicDBObject inventory = exchange.getIn().getBody(BasicDBObject.class);
		log.debug("InitializePostRoute inbody: " + inventory);
		exchange.getOut().setBody(inventory);
		BasicDBObject inventoryShopify = (BasicDBObject) exchange.getProperty("shopifyInstance");
		BasicDBObject shopify = (BasicDBObject) inventoryShopify.get("shopify");
		exchange.setProperty("productId", null);
		exchange.setProperty("categoryId", null);
		exchange.setProperty("isValidCategoryId", true);
		if (shopify.containsField("categoryID") && !shopify.getString("categoryID").equals("0")) {
			exchange.setProperty("categoryId", shopify.getString("categoryID"));
		}
		if (shopify.containsField("parentShopify")) {
			BasicDBObject parentShopify = (BasicDBObject) shopify.get("parentShopify");
			if (parentShopify.containsField("categoryID")) {
				exchange.setProperty("categoryId", parentShopify.getString("categoryID"));
			}
		}
		// Get User for Auth token details
		DBObject userDetails = (DBObject) exchange.getProperty("UserDetails");
		ArrayList<BasicDBObject> userShopifyList = (ArrayList<BasicDBObject>) userDetails.get("shopify");
		Map<String, BasicDBObject> warehouseLocationMap = new HashMap<String, BasicDBObject>();
		BasicDBObject userShopify = new BasicDBObject();
		for (int j = 0; j < userShopifyList.size(); j++) {
			userShopify = userShopifyList.get(j);
			String inventoryNickID = shopify.getString("nickNameID");
			BasicDBObject userNickName = (BasicDBObject) userShopify.get("nickName");
			String userNickID = userNickName.getString("id");
			if (inventoryNickID.equals(userNickID)) {
				exchange.setProperty("postHelper", (BasicDBObject) userShopify.get("postHelper"));
				exchange.setProperty("nickNameID", userNickID);
				exchange.setProperty("userShopify", userShopify);
				if (userShopify.containsField("storePickUpDetails")) {
					BasicDBObject storePickUpDetails = (BasicDBObject) userShopify.get("storePickUpDetails");
					if (storePickUpDetails.containsField("pickUpAddressDetails")) {
						List<BasicDBObject> warehouseLocationList = (List<BasicDBObject>) storePickUpDetails
								.get("pickUpAddressDetails");
						for (BasicDBObject warehouse : warehouseLocationList) {
							warehouseLocationMap.put(warehouse.getString("locationId"), warehouse);
						}
					}
				}
				break;
			}
		}
		exchange.setProperty("warehouseLocationMap", warehouseLocationMap);
		exchange.getOut().setBody((BasicDBObject) exchange.getProperty("parentInventory"));
	}
}