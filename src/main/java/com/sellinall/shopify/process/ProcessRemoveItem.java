package com.sellinall.shopify.process;

import java.util.ArrayList;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.sellinall.config.Config;
import com.sellinall.util.HttpsURLConnectionUtil;

public class ProcessRemoveItem implements Processor {
	static Logger log = Logger.getLogger(ProcessRemoveItem.class.getName());

	public void process(Exchange exchange) throws Exception {
		log.debug("Inside Shopify Remove item");
		BasicDBObject inventory = (BasicDBObject) exchange.getIn().getBody();
		BasicDBObject inventoryShopify=(BasicDBObject) inventory.get("shopify");
		//No need to remove all post 
		//Required post only will remove
		boolean needToRemoveItem=needToRemoveThisItem(exchange, inventoryShopify.getString("nickNameID"));
		if(!needToRemoveItem){
			exchange.setProperty("itemRemoved", false);
			return;
		}
		BasicDBObject postHelper = getUserShopifyPostHelper(exchange, inventoryShopify.getString("nickNameID"));
		String userName = postHelper.getString("apiKey");
		String password = postHelper.getString("pass");
		String url = postHelper.getString("URL");
		String refrenceId = inventoryShopify.getString("refrenceID");
		JSONObject request = (JSONObject) exchange.getProperty("request");
		String apiVersion = Config.getConfig().getApiVersion();
		if (request.getBoolean("needToRemoveParent")) {
			url = url + "/admin/api/" + apiVersion + "/products/" + refrenceId + ".json";

		} else {
			url = url + "/admin/api/" + apiVersion + "/products/" + refrenceId + "/variants/"
					+ inventoryShopify.getString("variantRefrenceId") + ".json";
		}
		HttpsURLConnectionUtil.doDeleteWithAuth(url, userName, password);
		log.debug("Removed to shopify");
		exchange.setProperty("nickNameID", inventoryShopify.getString("nickNameID"));
		exchange.setProperty("itemRemoved", true);
	}

	@SuppressWarnings("unchecked")
	private BasicDBObject getUserShopifyPostHelper(Exchange exchange, String nickName) {
		BasicDBObject user = (BasicDBObject) exchange.getProperty("UserDetails");
		ArrayList<BasicDBObject> usershopifyList = (ArrayList<BasicDBObject>) user.get("shopify");
		for(int i=0;i<usershopifyList.size();i++){
			BasicDBObject shopify=usershopifyList.get(i);
			BasicDBObject userNickName=(BasicDBObject) shopify.get("nickName");
			if(userNickName.getString("id").equals(nickName)){
				return (BasicDBObject) shopify.get("postHelper");
			}
				
		}
		return new BasicDBObject();
	}
	
	//Remove item request we have NickName List 
	//Which are all post need to remove 
	//Based on the nickNameList only we will remove that item
	private boolean needToRemoveThisItem(Exchange exchange,String inventoryNickNameID) throws JSONException{
		JSONArray nickNameList=(JSONArray) exchange.getProperty("siteNicknames");
		for(int i=0;i<nickNameList.length();i++){
			if(nickNameList.getString(i).equals(inventoryNickNameID)){
				return true;
			}
		}
		return false;
	}

}