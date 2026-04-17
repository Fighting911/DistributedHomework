# 商品库存与秒杀系统

基于微服务架构的商品库存与秒杀系统，覆盖分布式系统三大核心作业：高并发读、高并发写、分布式事务。

## 系统架构

```
Client
  │
  ▼
Nginx (:8080)  ← 负载均衡 + 动静分离
  ├── /static/*        → 静态文件直出
  ├── /api/user/*      → user-service-1/2（轮询，8081/8082）
  ├── /api/product/*   → product-service-1/2（轮询，8083/8086）
  ├── /api/inventory/* → inventory-service（8084，秒杀核心）
  └── /api/order/*     → order-service（8085，异步下单）
         │
         ├── MySQL 8.0（主从复制，master:3307 / slave:3308）
         ├── Redis 7.x（缓存 + 分布式锁 + 库存预减）
         ├── Kafka（异步订单消息队列）
         └── Elasticsearch 8.11（商品全文搜索）
```

## 技术栈

| 层次 | 技术选型 |
|------|---------|
| 后端框架 | Spring Boot 3.2 |
| 持久层 | MyBatis |
| 数据库 | MySQL 8.0（主从复制） |
| 缓存 | Redis 7.x |
| 分布式锁 | Redisson 3.27 |
| 消息队列 | Kafka (Confluent 7.4) |
| 搜索引擎 | Elasticsearch 8.11 |
| 读写分离 | AbstractRoutingDataSource + AOP |
| 分表 | ShardingSphere-JDBC 5.4（order-service） |
| 认证 | JWT (jjwt 0.12) |
| 容器化 | Docker + Docker Compose |
| 负载均衡 | Nginx 1.25 |

## 作业完成清单

### 作业一：系统设计与基础实现

- ✅ 微服务拆分（用户、商品、库存、订单四服务）
- ✅ RESTful API 设计
- ✅ MySQL 数据库设计（用户/商品/库存/订单/秒杀记录）
- ✅ Spring Boot + MyBatis + MySQL 环境搭建
- ✅ 用户注册/登录（JWT 认证，Token 存 Redis，TTL 2h）
- ✅ Docker + Docker Compose 容器化部署
- ✅ Nginx 负载均衡（轮询 / IP Hash / 最少连接）
- ✅ 动静分离（静态文件直出，动态接口代理）

### 作业二：高并发读

- ✅ **MySQL 主从复制**（binlog ROW 格式，master→slave 实时同步）
- ✅ **读写分离**（AbstractRoutingDataSource + @ReadOnly AOP，SELECT 路由从库）
- ✅ **Elasticsearch 全文搜索**（商品名称/描述索引，启动时全量同步）
- ✅ **Redis 分布式缓存**，三大问题处理：
  - 缓存穿透：查询不存在商品缓存空值 `"NULL"`，TTL=60s
  - 缓存击穿：Redis SETNX 互斥锁，只让一个线程查 DB
  - 缓存雪崩：TTL = 基础值(3600s) + 随机偏移(0~600s)

### 作业三：高并发写 + 分布式事务

- ✅ **Kafka 异步下单**（秒杀请求发 Kafka，inventory-service 消费处理）
- ✅ **三层防超卖**：
  1. Redis DECR 原子预减库存（最快拦截）
  2. Redisson 分布式锁（防并发重复下单）
  3. MySQL 乐观锁（version 字段，最终一致性兜底）
- ✅ **幂等性**（Redis 秒杀记录去重，同一用户同一商品只能成功一次）
- ✅ **ShardingSphere 分表**（t_order 按 id%4 分为 t_order_0~3）
- ✅ **Outbox Pattern 分布式事务**（本地消息表 t_outbox_message，保证库存扣减与订单创建最终一致）

## 快速启动

```bash
# 一键构建并启动所有服务
docker-compose up -d --build

# 查看服务状态
docker-compose ps

# 停止所有服务
docker-compose down
```

启动后端口汇总：

| 服务 | 宿主机端口 |
|------|-----------|
| Nginx（统一入口） | **8080** |
| user-service-1/2 | 8081 / 8082 |
| product-service-1/2 | 8083 / 8086 |
| inventory-service | 8084 |
| order-service | 8085 |
| MySQL master | 3307 |
| MySQL slave | 3308 |
| Redis | 6379 |
| Kafka | 9092 |
| Elasticsearch | 9200 |

> 注意：Nginx 使用 8080 端口（80 端口可能被其他服务占用）

## 接口示例

所有请求通过 Nginx（端口 8080）统一入口。

```bash
# 注册
curl -X POST http://localhost:8080/api/user/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"123456","email":"test@test.com"}'

# 登录（返回 JWT Token）
curl -X POST http://localhost:8080/api/user/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"123456"}'

# 商品列表（读从库）
curl http://localhost:8080/api/product/list

# 商品详情（Redis 缓存，fromCache=true 表示命中缓存）
curl http://localhost:8080/api/product/1

# ES 全文搜索
curl "http://localhost:8080/api/product/search?keyword=iPhone"

# 秒杀下单（需要 JWT Token）
curl -X POST http://localhost:8080/api/inventory/seckill \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"productId":1,"userId":1}'
```

## 目录结构

```
DistributedHomework/
├── docker-compose.yml
├── nginx/                  # 负载均衡 + 动静分离
├── frontend/               # 静态页面
├── mysql/
│   ├── master/my.cnf       # 主库配置（binlog ROW）
│   └── slave/              # 从库配置 + 初始化脚本
├── user-service/           # 用户服务（JWT 认证）
├── product-service/        # 商品服务（读写分离 + Redis 缓存 + ES）
├── inventory-service/      # 库存服务（Redisson 秒杀 + Outbox）
└── order-service/          # 订单服务（Kafka 消费 + ShardingSphere 分表）
```
