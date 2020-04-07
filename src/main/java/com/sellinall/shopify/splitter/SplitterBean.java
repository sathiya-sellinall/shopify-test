/**
 * 
 */
package com.sellinall.shopify.splitter;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Body;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultMessage;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;

/**
 * @author vikraman
 *
 */
public class SplitterBean {
	static Logger log = Logger.getLogger(SplitterBean.class.getName());

	@SuppressWarnings("unchecked")
	public List<Message> splitInventory(@Body BasicDBObject body) {
		ArrayList<BasicDBObject> shopifyInventory = (ArrayList<BasicDBObject>) body.get("shopify");
		List<Message> answer = new ArrayList<Message>();
		for (BasicDBObject shopify : shopifyInventory) {
			BasicDBObject splitBody = new BasicDBObject(body.toMap());
			splitBody.put("shopify", shopify);
			DefaultMessage message = new DefaultMessage();
			message.setBody(splitBody);
			answer.add(message);
		}

		return answer;
	}
	
	/*Sample Request*/
/*		{
		"accountNumber" : "56275005e4b03200bcb2152e",
		"SKUList" : [{
			"SKU" : "BQG0000040",
			"needToRemoveParent" : true,
			"siteNicknames" : ["shopify-2"]
			}
		],
		"requestType" : "removeArrayItem"
	}*/
	
	public List<Message> splitSKU(@Body JSONObject inBody) throws JSONException {	
		JSONArray skuList = inBody.getJSONArray("SKUList");
		List<Message> answer = new ArrayList<Message>();
		for (int i = 0; i < skuList.length(); i++) {
			JSONObject splitBody=new JSONObject();
			DefaultMessage message = new DefaultMessage();
			JSONObject skuData=skuList.getJSONObject(i);
			splitBody.put("accountNumber", inBody.getString("accountNumber"));
			splitBody.put("SKU", skuData.getString("SKU"));
			splitBody.put("siteNicknames", skuData.get("siteNicknames"));
			splitBody.put("needToRemoveParent", skuData.getString("needToRemoveParent"));
			splitBody.put("requestType", inBody.getString("requestType"));
			message.setBody(splitBody);
			answer.add(message);
		}
		return answer;
	}

	public List<Message> splitNickNames(@Body JSONObject inBody) throws JSONException {
		log.debug("iniside nickName SplitterBean");
		JSONArray nickNames = inBody.getJSONArray("siteNicknames");
		List<Message> answer = new ArrayList<Message>();
		for (int i = 0; i < nickNames.length(); i++) {
			JSONObject splitBody = new JSONObject();
			DefaultMessage message = new DefaultMessage();
			splitBody.put("nickNameID", nickNames.getString(i));
			message.setBody(splitBody);
			answer.add(message);
		}
		return answer;
	}
}