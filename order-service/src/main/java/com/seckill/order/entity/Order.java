package com.seckill.order.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Order {
    private Long id;
    private String orderNo;
    private Long userId;
    private Long productId;
    private Integer quantity;
    private BigDecimal totalPrice;
    /** 0待支付 1已支付 2已取消 3已完成 */
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
