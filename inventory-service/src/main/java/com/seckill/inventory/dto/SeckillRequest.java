package com.seckill.inventory.dto;

import lombok.Data;

@Data
public class SeckillRequest {
    private Long userId;
    private Long productId;
    private Integer quantity;
}
