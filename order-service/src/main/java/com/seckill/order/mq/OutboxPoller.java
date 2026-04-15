package com.seckill.order.mq;

import com.seckill.order.entity.OutboxMessage;
import com.seckill.order.mapper.OrderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 本地消息表轮询器（分布式事务核心组件）
 *
 * 工作流程：
 * 1. payOrder 时：同一本地事务写 t_order（状态=已支付）+ t_outbox_message
 * 2. 轮询器：扫描 outbox 发送到 Kafka（至少一次语义）
 * 3. 下游消费者幂等处理
 *
 * 这种模式保证：即使 Kafka 临时不可用，支付消息也不会丢失。
 */
@Component
public class OutboxPoller {

    private static final Logger log = LoggerFactory.getLogger(OutboxPoller.class);

    @Autowired private OrderMapper orderMapper;
    @Autowired private KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 5000)
    public void poll() {
        List<OutboxMessage> pending = orderMapper.selectPendingOutbox();
        for (OutboxMessage msg : pending) {
            try {
                kafkaTemplate.send(msg.getTopic(), msg.getPayload()).get();
                orderMapper.markOutboxSent(msg.getId());
                log.info("Outbox message sent: id={}, topic={}", msg.getId(), msg.getTopic());
            } catch (Exception e) {
                orderMapper.incrementOutboxRetry(msg.getId());
                log.error("Outbox send failed: id={}, retryCount={}", msg.getId(), msg.getRetryCount(), e);
            }
        }
    }
}
