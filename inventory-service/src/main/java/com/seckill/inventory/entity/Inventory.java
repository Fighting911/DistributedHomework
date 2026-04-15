package com.seckill.inventory.entity;

import lombok.Data;

@Data
public class Inventory {
    private Long id;
    private Long productId;
    private Integer stock;
    private Integer lockedStock;
    private Integer version;
}
