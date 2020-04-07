<#assign payloadIndex = exchange.properties.payloadIndex?number>
<#assign singlePayload = exchange.properties.stockUpdatePayloadList[payloadIndex]>
{
	"inventory_item_id":<#setting number_format="0" />${singlePayload.inventoryItemID}<#setting number_format="" />,
	"location_id":<#setting number_format="0" />${singlePayload.locationID}<#setting number_format="" />,
	"available":${singlePayload.available}
}
