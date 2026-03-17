package com.seckill.product.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seckill.product.entity.Product;
import com.seckill.product.mapper.ProductMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 商品服务 - 分布式缓存实现
 *
 * 三大缓存问题处理：
 *
 * 1. 缓存穿透（Cache Penetration）
 *    问题：查询不存在的商品ID，每次都打到数据库
 *    方案：查询结果为空时，缓存空值（"NULL"），TTL=60s
 *
 * 2. 缓存击穿（Cache Breakdown）
 *    问题：热点key过期，大量并发同时打到数据库
 *    方案：Redis SETNX 分布式互斥锁，只让一个线程查DB，其余等待
 *
 * 3. 缓存雪崩（Cache Avalanche）
 *    问题：大量key同时过期，瞬间大量请求打到数据库
 *    方案：TTL = 基础值 + 随机偏移量（0-600s），错开过期时间
 */
@Service
public class ProductServiceImpl {

    private static final Logger log = LoggerFactory.getLogger(ProductServiceImpl.class);

    private static final String CACHE_PREFIX   = "product:detail:";
    private static final String LOCK_PREFIX    = "product:lock:";
    private static final String NULL_VALUE     = "NULL";   // 空值标记

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${cache.product.ttl:3600}")
    private long baseTtl;

    @Value("${cache.product.null-ttl:60}")
    private long nullTtl;

    @Value("${cache.product.lock-ttl:10}")
    private long lockTtl;

    @Value("${instance.id:unknown}")
    private String instanceId;

    private final Random random = new Random();

    // ─────────────────────────────────────────────────────────────
    // 查询商品详情（含三大缓存问题处理）
    // ─────────────────────────────────────────────────────────────
    public Product getProductById(Long id) throws Exception {
        String cacheKey = CACHE_PREFIX + id;

        // Step 1: 查 Redis 缓存
        String cached = redisTemplate.opsForValue().get(cacheKey);

        if (cached != null) {
            log.info("[{}] Cache HIT, key={}", instanceId, cacheKey);

            // 【防穿透】命中空值缓存，直接返回 null
            if (NULL_VALUE.equals(cached)) {
                log.info("[{}] Cache NULL hit (penetration guard), id={}", instanceId, id);
                return null;
            }

            Product product = objectMapper.readValue(cached, Product.class);
            product.setFromCache(true);
            return product;
        }

        log.info("[{}] Cache MISS, key={}", instanceId, cacheKey);

        // Step 2: 【防击穿】获取分布式锁，只允许一个线程查DB
        return queryWithLock(id, cacheKey);
    }

    /**
     * 分布式互斥锁：防止缓存击穿
     * SETNX + EX = 原子设置锁，保证同一时刻只有一个线程查数据库
     */
    private Product queryWithLock(Long id, String cacheKey) throws Exception {
        String lockKey = LOCK_PREFIX + id;

        // 尝试加锁（SETNX）
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, instanceId, lockTtl, TimeUnit.SECONDS);

        if (Boolean.TRUE.equals(locked)) {
            // 抢到锁：查数据库
            try {
                // 双重检查：加锁后再查一次缓存（可能其他线程刚写入）
                String cached = redisTemplate.opsForValue().get(cacheKey);
                if (cached != null) {
                    if (NULL_VALUE.equals(cached)) return null;
                    Product p = objectMapper.readValue(cached, Product.class);
                    p.setFromCache(true);
                    return p;
                }

                log.info("[{}] Querying DB for product id={}", instanceId, id);
                Product product = productMapper.selectByIdWithStock(id);

                if (product == null) {
                    // 【防穿透】数据库也没有，缓存空值
                    redisTemplate.opsForValue().set(cacheKey, NULL_VALUE, nullTtl, TimeUnit.SECONDS);
                    log.warn("[{}] Product not found, cached NULL for id={}", instanceId, id);
                    return null;
                }

                // 【防雪崩】TTL = 基础值 + 随机偏移（0~600s），错开过期时间
                long ttl = baseTtl + random.nextInt(600);
                redisTemplate.opsForValue().set(
                    cacheKey,
                    objectMapper.writeValueAsString(product),
                    ttl,
                    TimeUnit.SECONDS
                );
                log.info("[{}] Cached product id={}, ttl={}s", instanceId, id, ttl);
                return product;

            } finally {
                // 释放锁（只释放自己加的锁）
                String lockVal = redisTemplate.opsForValue().get(lockKey);
                if (instanceId.equals(lockVal)) {
                    redisTemplate.delete(lockKey);
                }
            }

        } else {
            // 未抢到锁：短暂等待后重试（自旋）
            log.info("[{}] Lock not acquired, retrying for id={}", instanceId, id);
            Thread.sleep(50);
            return getProductById(id);  // 重试（最多等锁释放）
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 查询商品列表（不缓存，直接查DB）
    // ─────────────────────────────────────────────────────────────
    public List<Product> listProducts() {
        return productMapper.selectAllWithStock();
    }
}
