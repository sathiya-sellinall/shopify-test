{
	"variant":{
		<#if exchange.properties.isQuantityUpdate?? && exchange.properties.isQuantityUpdate == true>
		    <#if exchange.properties.inventory.shopify.noOfItem < 0>
		        "inventory_quantity": 0,
		    <#else>
		        "inventory_quantity": ${exchange.properties.inventory.shopify.noOfItem},
		    </#if>
		</#if>
		<#if exchange.properties.inventory.customSKU??>
			"sku":"${exchange.properties.inventory.customSKU}",
		</#if>
		<#if exchange.properties.inventory.shopify.variantRefrenceId??>
			"id": "${exchange.properties.inventory.shopify.variantRefrenceId}"
		<#else>
			"id": "${exchange.properties.inventory.shopify.refrenceID}"
		</#if>
		}
}