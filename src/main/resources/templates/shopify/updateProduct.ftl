{
	"product" : {
		"id" : ${exchange.properties.parentInventory.shopify.refrenceID?j_string},
		<#if exchange.properties.shopifyInstance.shopify.itemTitle?? && exchange.properties.shopifyInstance.shopify.itemTitle?trim?has_content>
			"title" : "${exchange.properties.shopifyInstance.shopify.itemTitle?j_string}",
		<#elseif exchange.properties.shopifyInstance.itemTitle?? && exchange.properties.shopifyInstance.itemTitle?trim?has_content>
			"title" : "${exchange.properties.shopifyInstance.itemTitle?j_string}",
		<#elseif exchange.properties.shopifyInstance.shopify.parentShopify?? && exchange.properties.shopifyInstance.shopify.parentShopify.itemTitle?? &&
						exchange.properties.shopifyInstance.shopify.parentShopify.itemTitle?j_string?trim?has_content>
			"title" : "${exchange.properties.shopifyInstance.shopify.parentShopify.itemTitle?j_string}",
		<#else>
			"title" : "${exchange.properties.parentInventory.itemTitle?j_string}",
		</#if>
		<#if exchange.properties.shopifyInstance.shopify.itemDescription?? && exchange.properties.shopifyInstance.shopify.itemDescription?trim?has_content>
			"body_html" : "${exchange.properties.shopifyInstance.shopify.itemDescription?replace("•","*")?js_string}",
		<#elseif exchange.properties.shopifyInstance.itemDescription?? && exchange.properties.shopifyInstance.itemDescription?trim?has_content>
			"body_html" : "${exchange.properties.shopifyInstance.itemDescription?replace("•","*")?js_string}",
		<#elseif exchange.properties.shopifyInstance.shopify.parentShopify?? && exchange.properties.shopifyInstance.shopify.parentShopify.itemDescription?? &&
							exchange.properties.shopifyInstance.shopify.parentShopify.itemDescription?trim?has_content>
			"body_html" : "${exchange.properties.shopifyInstance.shopify.parentShopify.itemDescription?replace("•","*")?js_string}",
		<#elseif exchange.properties.parentInventory?? && exchange.properties.parentInventory.itemDescription??>
			"body_html" : "${exchange.properties.parentInventory.itemDescription?replace("•","*")?js_string}",
		</#if>
		<#if exchange.properties.shopifyInstance.shopify.productType?? && exchange.properties.shopifyInstance.shopify.productType?trim?has_content>
			"product_type" : "${exchange.properties.shopifyInstance.shopify.productType}",
		<#elseif exchange.properties.parentInventory.shopify.productType?? && exchange.properties.parentInventory.shopify.productType?trim?has_content>
			"product_type" : "${exchange.properties.parentInventory.shopify.productType?j_string}",
		</#if>
		<#if exchange.properties.shopifyInstance.shopify.vendor?? && exchange.properties.shopifyInstance.shopify.vendor?trim?has_content>
			"vendor" : "${exchange.properties.shopifyInstance.shopify.vendor}",
		<#elseif exchange.properties.parentInventory.shopify.vendor?? && exchange.properties.parentInventory.shopify.vendor?trim?has_content>
			"vendor" : "${exchange.properties.parentInventory.shopify.vendor?j_string}",
		</#if>
		<#if exchange.properties.shopifyInstance.shopify.tags?? && exchange.properties.shopifyInstance.shopify.tags?has_content>
			"tags" : "${exchange.properties.shopifyInstance.shopify.tags?join(", ")}",
		<#elseif exchange.properties.parentInventory.shopify.tags?? && exchange.properties.parentInventory.shopify.tags?has_content>
			"tags" : "${exchange.properties.parentInventory.shopify.tags?join(", ")}",
		</#if>

		<#if exchange.properties.hasVariants == true >
			"variants" : ${processListForVariants()}
		<#else>
			"variants" : ${processListForNonVariants()}
		</#if>
		<#if exchange.properties.parentInventory.shopify.imageURI??>
		,"images" : [
			<#list exchange.properties.parentInventory.shopify.imageURI as imageUri>
					{
						"src" : "${exchange.properties.parentInventory.imageURL+imageUri}"
					}<#if imageUri_has_next>,</#if>
			</#list>
		]
		</#if>
		<#if exchange.properties.hasVariants == true >
		,"options" : [
			<#list exchange.properties.inventoryDetails[0].shopify.variantDetails as variant>
				{"name" : "${variant.title}"}<#if variant_has_next>,</#if>
			</#list>
		]
		</#if>
	}
}

<#function processListForNonVariants>
<#assign variants = "[">
	<#assign variants = variants + "{">
			<#assign variants = variants + "\"price\":\""+(exchange.properties.shopifyInstance.shopify.itemAmount.amount/100)?string("0.00")+"\",">
			<#assign variants = variants + "\"compare_at_price\":\""><#if exchange.properties.shopifyInstance.shopify.retailAmount??><#assign variants = variants + (exchange.properties.shopifyInstance.shopify.retailAmount.amount/100)?string("0.00")+"\","><#else><#assign variants = variants + "\","></#if>
			<#assign variants = variants + "\"sku\" : \""><#if exchange.properties.shopifyInstance.customSKU??><#assign variants = variants + exchange.properties.shopifyInstance.customSKU+"\","><#else><#assign variants = variants + exchange.properties.shopifyInstance.SKU+"\","></#if>
			<#assign variants = variants + "\"taxable\":\""><#if exchange.properties.shopifyInstance.shopify.taxable??><#assign variants = variants + (exchange.properties.shopifyInstance.shopify.taxable)?c+"\","><#else><#assign variants = variants + "\","></#if>
			<#assign variants = variants + "\"inventory_management\" : \"shopify\"">
	<#assign variants = variants + "}]">
	<#return variants>
</#function>

<#function processListForVariants>
<#assign variants = "[">
	<#list exchange.properties.inventoryDetails as inventory>
		<#assign shopify = inventory.shopify>
		<#assign variants = variants + "{">
		<#assign variants = variants + "\"id\":"+(shopify.variantRefrenceId)?j_string +",">
		<#assign variants = variants + "\"price\":\""+(shopify.itemAmount.amount/100)?string("0.00") +"\",">
		<#assign variants = variants + "\"weight\" : \""><#if shopify.weight??><#assign variants = variants + shopify.weight+"\","><#else><#assign variants = variants + "\","></#if>
		<#assign variants = variants + "\"compare_at_price\":\""><#if shopify.retailAmount??><#assign variants = variants + (shopify.retailAmount.amount/100)?string("0.00")+"\","><#else><#assign variants = variants + "\","></#if>
		<#list shopify.variantDetails as variantDetail>
				<#assign variants = variants + "\"option"+(variantDetail_index+1)+"\": \""+ variantDetail.name +"\",">
		</#list>
		<#assign variants = variants + "\"sku\" : \""><#if inventory.customSKU??><#assign variants = variants + inventory.customSKU+"\","><#else><#assign variants = variants + inventory.SKU+"\","></#if>
		<#assign variants = variants + "\"taxable\":\""><#if shopify.taxable??><#assign variants = variants + (shopify.taxable)?c+"\","><#else><#assign variants = variants + "\","></#if>
		<#assign variants = variants + "\"inventory_management\" : \"shopify\"">
		<#assign variants = variants + "}">
		<#if inventory_has_next>
			<#assign variants = variants + ",">	
		</#if>
	</#list>
<#assign variants = variants + "]">
<#return variants>
</#function>