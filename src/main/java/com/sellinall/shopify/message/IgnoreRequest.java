package com.sellinall.shopify.message;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;

import com.mongodb.BasicDBObject;
import com.sellinall.util.enums.SIAInventoryStatus;
import com.sellinall.util.enums.SIAInventoryUpdateStatus;

public class IgnoreRequest implements Processor {
	static Logger log = Logger.getLogger(IgnoreRequest.class.getName());

	public void process(Exchange exchange) throws Exception {
		List<BasicDBObject> updateObjectList = new ArrayList<BasicDBObject>();
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		String SKU = exchange.getProperty("SKU", String.class);
		String nickNameID = exchange.getProperty("nickNameID", String.class);
		String failureReason = "";
		String requestType = exchange.getProperty("requestType", String.class);
		boolean isItemRequest = requestType.equals("batchAddItem") || requestType.equals("batchEditItem") || requestType.equals("addItem");
		if (isItemRequest) {
			if (exchange.getProperties().containsKey("failureReason")) {
				failureReason = exchange.getProperty("failureReason", String.class);
			} else {
				failureReason = "unable to " + currentProcess(requestType)
						+ ", since account has more than one location ";
				log.error(requestType + " : unable to " + currentProcess(requestType)
						+ " , since account has more than one location for accountNumber - " + accountNumber
						+ ", SKU - " + SKU + ", NickNameID - " + nickNameID);
			}
		} else {
			if (exchange.getProperties().containsKey("failureReason")) {
				failureReason = exchange.getProperty("failureReason", String.class);
			} else {
				if (exchange.getProperties().containsKey("stockUpdatePayloadList")
						&& exchange.getProperty("stockUpdatePayloadList", List.class).size() == 0) {
					failureReason = "Unable to update quantity  because first location inventory count is lessthan zero";
				} else {
					failureReason = "FE update : unable to update quantity since account has more than one location";
					log.error(
							"FE update : unable to update quantity since account has more than one location for accountNumber - "
									+ accountNumber + ", SKU - " + SKU + ", NickNameID - " + nickNameID);
				}
			}
		}
		exchange.setProperty("failureReason", failureReason);
		BasicDBObject updateInventoryObject = new BasicDBObject();
		updateInventoryObject.put("SKU", exchange.getProperty("SKU"));
		if (requestType.equals("batchAddItem") || requestType.equals("addItem")) {
			updateInventoryObject.put("shopify.$.status", SIAInventoryStatus.FAILED.toString());
		} else {
			updateInventoryObject.put("shopify.$.updateStatus", SIAInventoryUpdateStatus.FAILED.toString());
		}
		updateInventoryObject.put("shopify.$.failureReason", failureReason);
		updateObjectList.add(updateInventoryObject);
		exchange.getOut().setBody(updateObjectList);
	}

	public String currentProcess(String requestType) {
		if (requestType.equals("batchAddItem") || requestType.equals("addItem")) {
			return "add";
		} else {
			return "update";
		}
	}

}
