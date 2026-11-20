package com.flashscale.inventory.controller;

import com.flashscale.inventory.entity.dtos.InventoryRequest;
import com.flashscale.inventory.service.InventoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    public final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/seed")
    public ResponseEntity<?> seedProduct(@RequestBody InventoryRequest request) {
        return ResponseEntity.ok(inventoryService.addOrUpdateProduct(request));
    }

    @GetMapping("/{skuCode}")
    public ResponseEntity<?> getAvailableStock(@PathVariable String skuCode){
        int stock = inventoryService.getAvailableStock(skuCode);
        return ResponseEntity.ok(Map.of("skuCode",skuCode, "availableStock",stock));
    }

    @PostMapping("/{skuCode}/reserve")
    public ResponseEntity<?> reserveStock(@PathVariable String skuCode, @RequestParam(defaultValue = "1") Integer quantity){
        boolean reserved = inventoryService.reserveStock(skuCode,quantity);
        if(!reserved){
            return ResponseEntity.badRequest().body(Map.of("skuCode", skuCode, "reserved",false,"message","Out of Stock"));
        }
        return ResponseEntity.ok(Map.of("skuCode",skuCode,"reserved", true,"quantityReserved",quantity));
    }
}
