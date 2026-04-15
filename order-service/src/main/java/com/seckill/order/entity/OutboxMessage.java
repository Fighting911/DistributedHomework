package com.seckill.order.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 本地消息表实体
 * 用于"支付成功→通知下游"的最终一致性保障
 */
@Data
public class OutboxMessage {
    private Long id;
    private String topic;
    private String payload;
    /** 0待发送 1已发送 2失败 */
    private Integer status;
    private Integer retryCount;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
}
