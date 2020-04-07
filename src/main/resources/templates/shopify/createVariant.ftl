{
  "variant": {
  	"sku":"${exchange.properties.SKU}",
  	"inventory_quantity":${exchange.properties.shopifyInstance.shopify.noOfItem},
	<#list exchange.properties.shopifyInstance.variantDetails as variantDetail>
		"option${variantDetail_index+1}": "${variantDetail.name}",
	</#list>
    "price": "${(exchange.properties.shopifyInstance.shopify.itemAmount.amount/100)?string("0.00")}",
    "inventory_management" : "shopify"
  }
}