package com.seckill.order.mq;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seckill.order.dto.CreateOrderRequest;
import com.seckill.order.service.impl.OrderServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 消费 inventory-service 发来的秒杀消息，创建订单
 */
@Component
public class OrderKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderKafkaConsumer.class);

    @Autowired private OrderServiceImpl orderService;
    @Autowired private ObjectMapper objectMapper;

    @KafkaListener(
        topics = "seckill-order",
        groupId = "order-service-group",
        concurrency = "3"
    )
    public void onSeckillMessage(String message, Acknowledgment ack) {
        try {
            Map<String, Object> map = objectMapper.readValue(
                message, new TypeReference<Map<String, Object>>() {});

            CreateOrderRequest req = new CreateOrderRequest();
            req.setOrderNo((String) map.get("orderNo"));
            req.setUserId(Long.valueOf(map.get("userId").toString()));
            req.setProductId(Long.valueOf(map.get("productId").toString()));
            req.setQuantity(Integer.valueOf(map.get("quantity").toString()));
            Object price = map.get("totalPrice");
            req.setTotalPrice(price != null ? new BigDecimal(price.toString()) : BigDecimal.ZERO);

            orderService.createOrder(req);
            ack.acknowledge();

        } catch (Exception e) {
            log.error("Failed to process seckill message: {}", message, e);
            // 不 ack → Kafka 重投
        }
    }
}
