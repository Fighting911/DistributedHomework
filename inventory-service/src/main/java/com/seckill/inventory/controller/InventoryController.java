package com.seckill.inventory.controller;

import com.seckill.inventory.common.Result;
import com.seckill.inventory.dto.SeckillRequest;
import com.seckill.inventory.service.impl.InventoryServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    @Autowired
    private InventoryServiceImpl inventoryService;

    @Value("${instance.id:unknown}")
    private String instanceId;

    @PostMapping("/seckill")
    public Result<?> seckill(@RequestBody SeckillRequest req) {
        try {
            return Result.success(inventoryService.seckill(req));
        } catch (Exception e) {
            return Result.error(400, e.getMessage());
        }
    }

    @GetMapping("/stock/{productId}")
    public Result<Integer> getStock(@PathVariable Long productId) {
        return Result.success(inventoryService.getStock(productId));
    }

    @GetMapping("/ping")
    public Result<String> ping() {
        return Result.success("pong from " + instanceId);
    }
}
