package com.seckill.order.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CreateOrderRequest {
    private String orderNo;
    private Long userId;
    private Long productId;
    private Integer quantity;
    private BigDecimal totalPrice;
}
