package com.flashscale.inventory.service;

import com.flashscale.inventory.entity.InventoryItem;
import com.flashscale.inventory.entity.dtos.InventoryRequest;
import com.flashscale.inventory.entity.dtos.InventoryResponse;
import com.flashscale.inventory.repository.InventoryRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class InventoryService {

    private final String STOCK_KEY_PREFIX = "flashscale:stock:";
    private final InventoryRepository inventoryRepository;
    private final StringRedisTemplate redisTemplate;

    public InventoryService(InventoryRepository inventoryRepository, StringRedisTemplate stringRedisTemplate) {
        this.inventoryRepository = inventoryRepository;
        this.redisTemplate = stringRedisTemplate;
    }

    @Transactional
    public InventoryItem addOrUpdateProduct(InventoryRequest request) {
        InventoryItem item = inventoryRepository.findBySkuCode(request.skuCode())
                .map(existing -> {
                    existing.setProductName(request.productName());
                    existing.setAvailableQuantity(request.quantity());
                    existing.setPrice(request.price());
                    log.info("Item already exists and updated");
                    return existing;
                })
                .orElseGet(() -> InventoryItem.builder()
                        .skuCode(request.skuCode())
                        .productName(request.productName())
                        .availableQuantity(request.quantity())
                        .price(request.price())
                        .build());

        InventoryItem savedItem = inventoryRepository.save(item);

        //sync redis cache with exact stock count
        redisTemplate.opsForValue().set(STOCK_KEY_PREFIX+request.skuCode(),String.valueOf(request.quantity()));
        log.info("Item {} has been added to stock and synced to redis cache ",savedItem.getId());
        return savedItem;
    }

    public int getAvailableStock(String skuCode){
        String key = STOCK_KEY_PREFIX+skuCode;
        String cache = redisTemplate.opsForValue().get(key);

        if(cache != null){
            return Integer.parseInt(cache);
        }
        //Cache miss fallback : load from mysql and then load it to cache
        InventoryItem item = inventoryRepository.findBySkuCode(skuCode)
                .orElseThrow(() -> new IllegalArgumentException("SKU Not Found : "+ skuCode));
        redisTemplate.opsForValue().set(key, String.valueOf(item.getAvailableQuantity()));
        log.info("Item {} added to redis cache ", item.getId());
        return item.getAvailableQuantity();
    }

    @Transactional
    public boolean reserveStock(String skuCode, Integer quantity){
        String key = STOCK_KEY_PREFIX+skuCode;

        if(redisTemplate.opsForValue().get(key) == null){
            getAvailableStock(skuCode);
        }

        Long remaining = redisTemplate.opsForValue().decrement(key, quantity);

        if(remaining == null || remaining == 0){
            //Roll back the decrement on the stock
            redisTemplate.opsForValue().increment(key, quantity);
            log.warn("Insufficient stock for SKU {} (requested{})", skuCode, quantity);
            return false;
        }

        //Store the updated count to SQL and Redis
        InventoryItem item = inventoryRepository.findBySkuCode(skuCode)
                .orElseThrow(() -> new IllegalArgumentException("SKU code not found : "+ skuCode));

        item.setAvailableQuantity(remaining.intValue());
        inventoryRepository.save(item);
        log.info("Reserved {} units of SKU {}. Remaining : {} ",quantity, skuCode,remaining);
        return true;
    }
}
