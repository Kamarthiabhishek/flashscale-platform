package com.flashscale.inventory.entity.dtos;

public record InventoryResponse(
        Long id,
        String skuCode,
        String productName,
        Integer quantity,
        Double price
) {
}
