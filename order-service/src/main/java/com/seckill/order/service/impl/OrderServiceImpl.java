package com.seckill.order.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seckill.order.dto.CreateOrderRequest;
import com.seckill.order.entity.Order;
import com.seckill.order.entity.OutboxMessage;
import com.seckill.order.mapper.OrderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class OrderServiceImpl {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    @Autowired private OrderMapper orderMapper;
    @Autowired private ObjectMapper objectMapper;

    /**
     * 创建订单（幂等，从 Kafka 消费后调用）
     */
    @Transactional
    public void createOrder(CreateOrderRequest req) {
        if (orderMapper.selectByOrderNo(req.getOrderNo()) != null) {
            log.info("Order already exists, skip: {}", req.getOrderNo());
            return;
        }
        Order order = new Order();
        order.setOrderNo(req.getOrderNo());
        order.setUserId(req.getUserId());
        order.setProductId(req.getProductId());
        order.setQuantity(req.getQuantity());
        order.setTotalPrice(req.getTotalPrice() != null ? req.getTotalPrice() : BigDecimal.ZERO);
        order.setStatus(0);
        orderMapper.insertOrder(order);
        log.info("Order created: {}", req.getOrderNo());
    }

    /**
     * 支付订单
     * 核心：更新订单状态 + 写本地消息表（同一个本地事务）
     * OutboxPoller 负责将消息发往 Kafka，保证最终一致性
     */
    @Transactional
    public void payOrder(String orderNo) throws Exception {
        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) throw new RuntimeException("订单不存在: " + orderNo);
        if (order.getStatus() == 1) throw new RuntimeException("订单已支付");
        if (order.getStatus() == 2) throw new RuntimeException("订单已取消");

        orderMapper.updateStatus(orderNo, 1);

        // 写入本地消息表（与订单更新同一事务，保证消息不丢失）
        OutboxMessage msg = new OutboxMessage();
        msg.setTopic("order-paid");
        Map<String, Object> payload = Map.of(
            "orderNo", orderNo,
            "userId", order.getUserId(),
            "productId", order.getProductId()
        );
        msg.setPayload(objectMapper.writeValueAsString(payload));
        msg.setStatus(0);
        orderMapper.insertOutbox(msg);

        log.info("Order paid, outbox message inserted: {}", orderNo);
    }

    public Order getByOrderNo(String orderNo) {
        return orderMapper.selectByOrderNo(orderNo);
    }

    public List<Order> getByUserId(Long userId) {
        return orderMapper.selectByUserId(userId);
    }
}
