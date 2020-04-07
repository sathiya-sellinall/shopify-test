package com.sellinall.config;

import org.springframework.context.ApplicationContext;

public class Config {
	public static ApplicationContext context;

	private String FbTokenExchangeUri;

	private String DbUserName;
	private String DbPassword;
	private String UserCollectionHostName;
	private String UserCollectionPort;
	private String UserCollectionDBName;
	private String InventoryCollectionHostName;
	private String InventoryCollectionPort;
	private String InventoryCollectionDBName;
	private String OrderCollectionHostName;
	private String OrderCollectionPort;
	private String OrderCollectionDBName;
	private int RecordsPerPage;
	private String uploadImageToSellInAllUrl;
	private String uploadImageUri;
	private String noImageURL;
	private String Ragasiyam;
	private String uploadCategories;
	private String shopifyServerURL;
	private String apiVersion;

	public String getShopifyServerURL() {
		return shopifyServerURL;
	}

	public void setShopifyServerURL(String shopifyServerURL) {
		this.shopifyServerURL = shopifyServerURL;
	}

	public String getUploadCategories() {
		return uploadCategories;
	}

	public void setUploadCategories(String uploadCategories) {
		this.uploadCategories = uploadCategories;
	}

	public String getRagasiyam() {
		return Ragasiyam;
	}

	public void setRagasiyam(String ragasiyam) {
		Ragasiyam = ragasiyam;
	}

	public String getNoImageURL() {
		return noImageURL;
	}

	public void setNoImageURL(String noImageURL) {
		this.noImageURL = noImageURL;
	}

	public String getUploadImageUri() {
		return uploadImageUri;
	}

	public void setUploadImageUri(String uploadImageUri) {
		this.uploadImageUri = uploadImageUri;
	}

	public String getUploadImageToSellInAllUrl() {
		return uploadImageToSellInAllUrl;
	}

	public void setUploadImageToSellInAllUrl(String uploadImageToSellInAllUrl) {
		this.uploadImageToSellInAllUrl = uploadImageToSellInAllUrl;
	}

	public int getRecordsPerPage() {
		return RecordsPerPage;
	}

	public void setRecordsPerPage(int recordsPerPage) {
		RecordsPerPage = recordsPerPage;
	}

	public String getFbTokenExchangeUri() {
		return FbTokenExchangeUri;
	}

	public void setFbTokenExchangeUri(String fbTokenExchangeUri) {
		FbTokenExchangeUri = fbTokenExchangeUri;
	}

	public String getDbUserName() {
		return DbUserName;
	}

	public void setDbUserName(String dbUserName) {
		DbUserName = dbUserName;
	}

	public String getDbPassword() {
		return DbPassword;
	}

	public void setDbPassword(String dbPassword) {
		DbPassword = dbPassword;
	}

	public String getUserCollectionHostName() {
		return UserCollectionHostName;
	}

	public void setUserCollectionHostName(String userCollectionHostName) {
		UserCollectionHostName = userCollectionHostName;
	}

	public String getUserCollectionPort() {
		return UserCollectionPort;
	}

	public void setUserCollectionPort(String userCollectionPort) {
		UserCollectionPort = userCollectionPort;
	}

	public String getUserCollectionDBName() {
		return UserCollectionDBName;
	}

	public void setUserCollectionDBName(String userCollectionDBName) {
		UserCollectionDBName = userCollectionDBName;
	}

	public String getInventoryCollectionHostName() {
		return InventoryCollectionHostName;
	}

	public void setInventoryCollectionHostName(
			String inventoryCollectionHostName) {
		InventoryCollectionHostName = inventoryCollectionHostName;
	}

	public String getInventoryCollectionPort() {
		return InventoryCollectionPort;
	}

	public void setInventoryCollectionPort(String inventoryCollectionPort) {
		InventoryCollectionPort = inventoryCollectionPort;
	}

	public String getInventoryCollectionDBName() {
		return InventoryCollectionDBName;
	}

	public void setInventoryCollectionDBName(String inventoryCollectionDBName) {
		InventoryCollectionDBName = inventoryCollectionDBName;
	}

	public static Config getConfig() {
		return (Config) context.getBean("Config");
	}

	public String getOrderCollectionHostName() {
		return OrderCollectionHostName;
	}

	public void setOrderCollectionHostName(String orderCollectionHostName) {
		OrderCollectionHostName = orderCollectionHostName;
	}

	public String getOrderCollectionPort() {
		return OrderCollectionPort;
	}

	public void setOrderCollectionPort(String orderCollectionPort) {
		OrderCollectionPort = orderCollectionPort;
	}

	public String getOrderCollectionDBName() {
		return OrderCollectionDBName;
	}

	public void setOrderCollectionDBName(String orderCollectionDBName) {
		OrderCollectionDBName = orderCollectionDBName;
	}

	public String getApiVersion() {
		return apiVersion;
	}

	public void setApiVersion(String apiVersion) {
		this.apiVersion = apiVersion;
	}
}
