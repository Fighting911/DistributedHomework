package com.seckill.inventory.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seckill.inventory.dto.SeckillRequest;
import com.seckill.inventory.entity.Inventory;
import com.seckill.inventory.mapper.InventoryMapper;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 秒杀三层防超卖：
 * 1. Redis DECR 预减库存（最快拦截）
 * 2. Redisson 分布式锁（防并发重复）
 * 3. MySQL 乐观锁（version 字段兜底）
 */
@Service
public class InventoryServiceImpl {

    private static final Logger log = LoggerFactory.getLogger(InventoryServiceImpl.class);
    private static final String STOCK_KEY = "inventory:stock:";
    private static final String TOPIC_SECKILL = "seckill-order";

    @Autowired private InventoryMapper inventoryMapper;
    @Autowired private StringRedisTemplate redisTemplate;
    @Autowired private RedissonClient redissonClient;
    @Autowired private KafkaTemplate<String, String> kafkaTemplate;
    @Autowired private ObjectMapper objectMapper;

    @Value("${instance.id:inventory-service-local}")
    private String instanceId;

    // 将 MySQL 库存预热到 Redis
    @EventListener(ApplicationReadyEvent.class)
    public void initRedisStock() {
        for (long productId = 1; productId <= 3; productId++) {
            try {
                Inventory inv = inventoryMapper.selectByProductId(productId);
                if (inv != null) {
                    String key = STOCK_KEY + productId;
                    redisTemplate.opsForValue().set(key, String.valueOf(inv.getStock()));
                    log.info("Init Redis stock: product={}, stock={}", productId, inv.getStock());
                }
            } catch (Exception e) {
                log.warn("Failed to init stock for product {}: {}", productId, e.getMessage());
            }
        }
    }

    @Transactional
    public Map<String, Object> seckill(SeckillRequest req) throws Exception {
        Long userId = req.getUserId();
        Long productId = req.getProductId();
        int qty = req.getQuantity() == null ? 1 : req.getQuantity();

        // ① Redis 预减库存
        String stockKey = STOCK_KEY + productId;
        Long remaining = redisTemplate.opsForValue().decrement(stockKey, qty);
        if (remaining == null || remaining < 0) {
            redisTemplate.opsForValue().increment(stockKey, qty);
            throw new RuntimeException("库存不足，秒杀失败");
        }

        // ② 幂等检查
        int exists = inventoryMapper.countSeckillRecord(userId, productId);
        if (exists > 0) {
            redisTemplate.opsForValue().increment(stockKey, qty);
            throw new RuntimeException("您已参与过该商品的秒杀");
        }

        // ③ Redisson 分布式锁
        RLock lock = redissonClient.getLock("seckill:lock:" + productId);
        boolean locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
        if (!locked) {
            redisTemplate.opsForValue().increment(stockKey, qty);
            throw new RuntimeException("系统繁忙，请稍后重试");
        }

        try {
            // ④ MySQL 乐观锁扣减
            Inventory inv = inventoryMapper.selectByProductId(productId);
            if (inv == null || inv.getStock() < qty) {
                redisTemplate.opsForValue().increment(stockKey, qty);
                throw new RuntimeException("库存不足");
            }
            int rows = inventoryMapper.decreaseStock(productId, qty, inv.getVersion());
            if (rows == 0) {
                redisTemplate.opsForValue().increment(stockKey, qty);
                throw new RuntimeException("并发冲突，请重试");
            }

            // ⑤ 生成订单号，插入幂等记录
            String orderNo = generateOrderNo(userId);
            inventoryMapper.insertSeckillRecord(userId, productId, orderNo);

            // ⑥ 发送 Kafka 消息
            Map<String, Object> msg = new HashMap<>();
            msg.put("orderNo", orderNo);
            msg.put("userId", userId);
            msg.put("productId", productId);
            msg.put("quantity", qty);
            String payload = objectMapper.writeValueAsString(msg);
            kafkaTemplate.send(TOPIC_SECKILL, String.valueOf(productId), payload);
            log.info("Seckill success: orderNo={}, userId={}, productId={}", orderNo, userId, productId);

            Map<String, Object> result = new HashMap<>();
            result.put("orderNo", orderNo);
            result.put("message", "秒杀成功，订单处理中");
            return result;

        } finally {
            if (lock.isHeldByCurrentThread()) lock.unlock();
        }
    }

    public int getStock(Long productId) {
        String val = redisTemplate.opsForValue().get(STOCK_KEY + productId);
        if (val != null) return Integer.parseInt(val);
        Inventory inv = inventoryMapper.selectByProductId(productId);
        return inv == null ? 0 : inv.getStock();
    }

    private String generateOrderNo(Long userId) {
        long ts = System.currentTimeMillis();
        int uid = (int) (userId % 10000);
        int rand = (int) (Math.random() * 10000);
        return String.format("%d%04d%04d", ts, uid, rand);
    }
}
