package com.flashscale.inventory.entity.dtos;

public record InventoryRequest(
        String skuCode,
        String productName,
        Integer quantity,
        Double price
) {
}
