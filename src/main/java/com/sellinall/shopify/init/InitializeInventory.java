/**
 * 
 */
package com.sellinall.shopify.init;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.sellinall.util.enums.SIAInventoryStatus;

/**
 * @author Ramachandran.K
 * 
 */
public class InitializeInventory implements Processor {
	static Logger log = Logger.getLogger(InitializeInventory.class.getName());

	public void process(Exchange exchange) throws Exception {
		BasicDBObject inventory = exchange.getIn().getBody(BasicDBObject.class);
		BasicDBObject shopify = (BasicDBObject) inventory.get("shopify");
		if (!shopify.getString("status").equals(SIAInventoryStatus.ACTIVE.toString())) {
			exchange.setProperty("isEligibleToProceed", false);
			return;
		}
		exchange.setProperty("isEligibleToProceed", true);
		exchange.setProperty("nickNameID", shopify.getString("nickNameID"));
		exchange.setProperty("inventory", inventory);
		DBObject outBody = exchange.getProperty("UserDetails", DBObject.class);
		Map<String, BasicDBObject> warehouseLocationMap = new HashMap<String, BasicDBObject>();
		if (outBody.containsField("shopify")) {
			BasicDBList shopifyAccountList = (BasicDBList) outBody.get("shopify");
			for (int i = 0; i < shopifyAccountList.size(); i++) {
				BasicDBObject shopifyAccount = (BasicDBObject) shopifyAccountList.get(i);
				BasicDBObject nickNameObject = (BasicDBObject) shopifyAccount.get("nickName");
				if (nickNameObject.getString("id").equals(shopify.getString("nickNameID"))) {
					exchange.setProperty("postHelper", (BasicDBObject) shopifyAccount.get("postHelper"));
					if (shopifyAccount.containsField("storePickUpDetails")) {
						BasicDBObject storePickUpDetails = (BasicDBObject) shopifyAccount.get("storePickUpDetails");
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
		}
		exchange.setProperty("warehouseLocationMap", warehouseLocationMap);
	}
}