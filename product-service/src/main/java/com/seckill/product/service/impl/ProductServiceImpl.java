package com.seckill.product.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seckill.product.entity.Product;
import com.seckill.product.es.ProductDocument;
import com.seckill.product.es.ProductEsRepository;
import com.seckill.product.mapper.ProductMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 商品服务 - 分布式缓存实现 + 读写分离 + ES搜索
 *
 * 三大缓存问题处理：
 * 1. 缓存穿透：查询不存在商品时缓存空值 "NULL"，TTL=60s
 * 2. 缓存击穿：Redis SETNX 互斥锁，只让一个线程查DB
 * 3. 缓存雪崩：TTL = 基础值 + 随机偏移（0-600s）
 *
 * 读写分离：通过 ShardingSphere-JDBC 自动路由
 * - SELECT → slave 数据源
 * - INSERT/UPDATE/DELETE → master 数据源
 */
@Service
public class ProductServiceImpl {

    private static final Logger log = LoggerFactory.getLogger(ProductServiceImpl.class);

    private static final String CACHE_PREFIX = "product:detail:";
    private static final String LOCK_PREFIX  = "product:lock:";
    private static final String NULL_VALUE   = "NULL";

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired(required = false)
    private ProductEsRepository productEsRepository;

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
    // ShardingSphere 自动将此 SELECT 路由到 slave
    // ─────────────────────────────────────────────────────────────
    public Product getProductById(Long id) throws Exception {
        String cacheKey = CACHE_PREFIX + id;

        // Step 1: 查 Redis 缓存
        String cached = redisTemplate.opsForValue().get(cacheKey);

        if (cached != null) {
            log.info("[{}] Cache HIT, key={}", instanceId, cacheKey);
            if (NULL_VALUE.equals(cached)) {
                log.info("[{}] Cache NULL hit (penetration guard), id={}", instanceId, id);
                return null;
            }
            Product product = objectMapper.readValue(cached, Product.class);
            product.setFromCache(true);
            return product;
        }

        log.info("[{}] Cache MISS, key={}", instanceId, cacheKey);
        return queryWithLock(id, cacheKey);
    }

    /**
     * 分布式互斥锁：防止缓存击穿
     */
    private Product queryWithLock(Long id, String cacheKey) throws Exception {
        String lockKey = LOCK_PREFIX + id;

        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, instanceId, lockTtl, TimeUnit.SECONDS);

        if (Boolean.TRUE.equals(locked)) {
            try {
                String cached = redisTemplate.opsForValue().get(cacheKey);
                if (cached != null) {
                    if (NULL_VALUE.equals(cached)) return null;
                    Product p = objectMapper.readValue(cached, Product.class);
                    p.setFromCache(true);
                    return p;
                }

                // 走 slave（ShardingSphere 自动路由）
                log.info("[{}] Querying DB(slave) for product id={}", instanceId, id);
                Product product = productMapper.selectByIdWithStock(id);

                if (product == null) {
                    redisTemplate.opsForValue().set(cacheKey, NULL_VALUE, nullTtl, TimeUnit.SECONDS);
                    log.warn("[{}] Product not found, cached NULL for id={}", instanceId, id);
                    return null;
                }

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
                String lockVal = redisTemplate.opsForValue().get(lockKey);
                if (instanceId.equals(lockVal)) {
                    redisTemplate.delete(lockKey);
                }
            }

        } else {
            log.info("[{}] Lock not acquired, retrying for id={}", instanceId, id);
            Thread.sleep(50);
            return getProductById(id);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 查询商品列表（走 slave）
    // ─────────────────────────────────────────────────────────────
    public List<Product> listProducts() {
        log.info("[{}] List products from DB(slave via ShardingSphere)", instanceId);
        return productMapper.selectAllWithStock();
    }

    // ─────────────────────────────────────────────────────────────
    // ES 全文搜索
    // ─────────────────────────────────────────────────────────────

    /**
     * 全文搜索商品（走 Elasticsearch）
     */
    public List<ProductDocument> searchProducts(String keyword) {
        if (productEsRepository == null) {
            log.warn("ES not available, returning empty result");
            return List.of();
        }
        return productEsRepository
                .findByNameContainingOrDescriptionContaining(
                        keyword, keyword,
                        PageRequest.of(0, 20))
                .getContent();
    }

    /**
     * 同步单个商品到 ES
     */
    public void syncToEs(Product product) {
        if (productEsRepository == null) return;
        ProductDocument doc = new ProductDocument();
        doc.setId(String.valueOf(product.getId()));
        doc.setName(product.getName());
        doc.setDescription(product.getDescription());
        doc.setPrice(product.getPrice());
        doc.setStock(product.getStock());
        doc.setStatus(product.getStatus());
        productEsRepository.save(doc);
        log.info("Synced product {} to ES", product.getId());
    }

    /**
     * 应用启动完成后将全量商品同步到 ES
     */
    @EventListener(ApplicationReadyEvent.class)
    public void syncAllToEs() {
        if (productEsRepository == null) return;
        try {
            List<Product> products = productMapper.selectAllWithStock();
            products.forEach(this::syncToEs);
            log.info("Full sync to ES done, {} products", products.size());
        } catch (Exception e) {
            log.warn("ES sync failed (ES may not be ready): {}", e.getMessage());
        }
    }
}
