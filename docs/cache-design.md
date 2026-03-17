# 分布式缓存设计说明

## 1. 缓存架构

```
Client
  │
  ▼
Nginx (80)
  │
  ▼
product-service (8082)
  │
  ├─ Redis CACHE HIT  →  直接返回（< 1ms）
  │
  └─ Redis CACHE MISS
         │
         ├─ 获取分布式锁（SETNX）
         │     ├─ 成功 → 查 MySQL → 写缓存 → 释放锁
         │     └─ 失败 → 等待50ms → 重试
         │
         └─ 返回结果
```

---

## 2. 三大缓存问题及解决方案

### 2.1 缓存穿透（Cache Penetration）

**问题描述：** 恶意或错误请求查询不存在的商品ID（如 id=999999），
每次都绕过缓存直接打到 MySQL，可能导致数据库压力过大。

**解决方案：空值缓存**

```
查询 product:detail:999999
    → Redis: MISS
    → MySQL: 查询结果为 null
    → 写入 Redis: SET product:detail:999999 "NULL" EX 60
    → 下次相同请求命中 "NULL"，直接返回"商品不存在"
```

**代码关键点：**
```java
if (NULL_VALUE.equals(cached)) {
    return null;  // 命中空值，直接返回
}
// ...
if (product == null) {
    redisTemplate.opsForValue().set(cacheKey, "NULL", 60, TimeUnit.SECONDS);
}
```

**权衡：** 空值TTL不宜过长（设60s），否则商品新上架后用户会持续看到"不存在"。

---

### 2.2 缓存击穿（Cache Breakdown）

**问题描述：** 某个热点商品的缓存恰好过期，此时大量并发请求同时发现缓存MISS，
全部打到 MySQL，瞬间产生巨大压力（"狗群效应"）。

**解决方案：Redis 分布式互斥锁**

```
线程1: SETNX product:lock:1 → 成功 → 查DB → 写缓存 → 释放锁
线程2: SETNX product:lock:1 → 失败 → sleep(50ms) → 重试 → 命中缓存
线程3: SETNX product:lock:1 → 失败 → sleep(50ms) → 重试 → 命中缓存
```

**代码关键点：**
```java
Boolean locked = redisTemplate.opsForValue()
    .setIfAbsent(lockKey, instanceId, 10, TimeUnit.SECONDS);  // SETNX + EX

if (Boolean.TRUE.equals(locked)) {
    // 双重检查（Double Check）
    String recheck = redisTemplate.opsForValue().get(cacheKey);
    if (recheck != null) return parse(recheck);  // 其他线程已写入

    Product p = productMapper.selectByIdWithStock(id);
    redisTemplate.opsForValue().set(cacheKey, serialize(p), ttl, TimeUnit.SECONDS);
    return p;
} else {
    Thread.sleep(50);
    return getProductById(id);  // 重试
}
```

**双重检查的必要性：** 加锁后必须再查一次缓存，因为在等待锁的过程中，
持锁线程可能已经把数据写入缓存了。

---

### 2.3 缓存雪崩（Cache Avalanche）

**问题描述：** 系统启动或缓存重建时，大量key设置了相同的TTL，
导致同一时刻集体过期，瞬间大量请求全部打到数据库。

**解决方案：TTL 随机偏移**

```java
// 基础TTL=3600s，加上 0~600s 随机偏移
long ttl = baseTtl + random.nextInt(600);
redisTemplate.opsForValue().set(cacheKey, value, ttl, TimeUnit.SECONDS);
```

**效果：**
- 商品1: TTL = 3600 + 342 = 3942s
- 商品2: TTL = 3600 + 187 = 3787s  
- 商品3: TTL = 3600 + 571 = 4171s

各商品错开过期时间，避免同时过期。

---

## 3. Redis Key 设计

| Key 格式                    | 含义         | TTL          |
|---------------------------|--------------|--------------|
| `product:detail:{id}`     | 商品详情缓存  | 3600~4200s   |
| `product:detail:{id}=NULL`| 空值缓存      | 60s          |
| `product:lock:{id}`       | 防击穿互斥锁  | 10s（自动释放）|
| `seckill:stock:{id}`      | 秒杀库存      | 永久（手动维护）|
| `user:token:{token}`      | 用户登录Token | 7200s        |

---

## 4. 验证方法

```bash
# 1. 第一次请求（冷启动，查 DB）
curl http://localhost/api/product/1
# → "fromCache": false

# 2. 第二次请求（命中缓存）
curl http://localhost/api/product/1
# → "fromCache": true

# 3. 查看缓存内容
docker-compose exec redis redis-cli GET "product:detail:1"

# 4. 测试穿透保护（查询不存在的商品）
curl http://localhost/api/product/9999
# → 404，且 Redis 中写入了 "NULL" 值

# 5. 查看空值缓存
docker-compose exec redis redis-cli GET "product:detail:9999"
# → "NULL"
```
