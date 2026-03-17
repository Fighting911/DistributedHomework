package com.seckill.product.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Product {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer status;
    private LocalDateTime createdAt;
    // 库存（来自 t_inventory 关联查询）
    private Integer stock;
    // 标识数据来源（缓存 or 数据库），用于前端展示
    private transient Boolean fromCache = false;
}
