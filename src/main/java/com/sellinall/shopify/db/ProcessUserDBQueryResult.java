package com.sellinall.shopify.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

public class ProcessUserDBQueryResult implements Processor {
	static Logger log = Logger.getLogger(ProcessUserDBQueryResult.class.getName());

	@SuppressWarnings("unchecked")
	public void process(Exchange exchange) throws Exception {

		DBObject userDBObject = (DBObject) exchange.getProperty("UserDetails");
		ArrayList<DBObject> shopifyUser = (ArrayList<DBObject>) userDBObject.get("shopify");

		Map<String, BasicDBObject> nicknameToPostHelperMap = new HashMap<String, BasicDBObject>();
		Map<String, String> nicknameToNameMap = new HashMap<String, String>();
		for (DBObject user : shopifyUser) {
			String userNickName = (String) ((DBObject) user.get("nickName")).get("id");
			nicknameToPostHelperMap.put(userNickName, (BasicDBObject) user.get("postHelper"));
			nicknameToNameMap.put(userNickName, ((BasicDBObject) user.get("shop")).getString("name"));
		}

		BasicDBObject invBasicDBObject = (BasicDBObject) exchange.getIn().getBody();
		exchange.getOut().setBody(invBasicDBObject);

	}

}
