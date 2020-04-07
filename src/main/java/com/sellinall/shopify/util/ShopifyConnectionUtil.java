package com.sellinall.shopify.util;

import java.io.IOException;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * 
 * @author Raguvaran
 *
 */
public class ShopifyConnectionUtil {
	static Logger log = Logger.getLogger(ShopifyConnectionUtil.class.getName());

	public static String doGetWithAuthorization(String name, String password, String url) throws JSONException {
		JSONObject responseData = new JSONObject();
		DefaultHttpClient httpClient = new DefaultHttpClient();
		try {
			HttpGet getRequest = new HttpGet(url);
			String authString = name + ":" + password;
			log.debug("auth string: " + authString);
			byte[] authEncBytes = Base64.encodeBase64(authString.getBytes());
			String authStringEnc = new String(authEncBytes);
			log.debug("Base64 encoded auth string: " + authStringEnc);
			getRequest.addHeader("Authorization", "Basic " + authStringEnc);
			HttpResponse response = httpClient.execute(getRequest);
			responseData.put("payload", new JSONObject(EntityUtils.toString(response.getEntity())));
			responseData.put("httpCode", response.getStatusLine().getStatusCode());
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			// Release the connection.
			httpClient.getConnectionManager().shutdown();
		}
		// print result
		log.debug(responseData.toString());
		return responseData.toString();
	}

}