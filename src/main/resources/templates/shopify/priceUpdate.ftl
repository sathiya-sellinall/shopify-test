{
	"variant":{
		<#if exchange.properties.isPriceUpdate?? && exchange.properties.isPriceUpdate == true>
            "price":"${(exchange.properties.inventory.shopify.itemAmount.amount/100)?string("0.00")}",
		<#if exchange.properties.inventory.shopify.retailAmount??>
			"compare_at_price":"${(exchange.properties.inventory.shopify.retailAmount.amount/100)?string("0.00")}",
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