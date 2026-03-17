# 商品库存与秒杀系统

> 武汉大学 · 分布式系统课程作业

## 项目简介

基于微服务架构的商品库存与秒杀系统，覆盖分布式系统核心能力：

- ✅ 服务拆分（用户、商品、库存、订单服务）
- ✅ 高并发防超卖（Redis预减 + Redisson分布式锁 + 乐观锁三层）
- ✅ 负载均衡（Nginx 轮询 / IP Hash / 最少连接）
- ✅ 动静分离（Nginx 静态文件直出 vs 动态接口代理）
- ✅ 分布式缓存（Redis 缓存穿透/击穿/雪崩处理）
- ✅ JWT 认证（无状态 Token）
- ✅ 容器化部署（Docker + Docker Compose）

## 系统架构

```
Client
  │
  ▼
Nginx (80)  ← 负载均衡 + 动静分离
  ├── /static/*      → 直接返回静态文件
  ├── /api/user/*    → user-service-1 / user-service-2（轮询）
  ├── /api/product/* → product-service-1 / product-service-2（轮询）
  └── /api/inventory/* → inventory-service（秒杀核心）
         │
         ├── Redis 7.x（缓存 + 分布式锁）
         └── MySQL 8.0（持久化）
```

## 技术栈

| 层次 | 技术选型 |
|------|---------|
| 后端框架 | Spring Boot 3.2 |
| 持久层 | MyBatis |
| 数据库 | MySQL 8.0 |
| 缓存/限流 | Redis 7.x |
| 分布式锁 | Redisson 3.27 |
| 认证 | JWT (jjwt 0.12) |
| 容器化 | Docker + Docker Compose |
| 负载均衡 | Nginx 1.25 |
| 构建工具 | Maven（多模块） |

## 快速启动

### 前置条件
- Docker + Docker Compose
- Java 17+（本地开发）

### 一键启动（推荐）

```bash
docker-compose up -d --build
```

启动后容器列表：

| 容器 | 宿主机端口 | 说明 |
|------|-----------|------|
| seckill-nginx | 80 | 统一入口 |
| user-service-1 | 8081 | 用户服务实例1 |
| user-service-2 | 8082 | 用户服务实例2 |
| product-service-1 | 8083 | 商品服务实例1 |
| product-service-2 | 8086 | 商品服务实例2 |
| inventory-service | 8084 | 库存/秒杀服务 |
| seckill-mysql | 3307 | MySQL（宿主机3307避免冲突）|
| seckill-redis | 6379 | Redis |

### 停止服务

```bash
docker-compose down
```

## 接口文档

所有请求通过 Nginx（端口80）统一入口。

### 用户服务

```bash
# 注册
curl -X POST http://localhost/api/user/register \
  -H "Content-Type: application/json" \
  -d '{"username":"max","password":"123456","email":"max@test.com"}'

# 登录（返回 JWT Token）
curl -X POST http://localhost/api/user/login \
  -H "Content-Type: application/json" \
  -d '{"username":"max","password":"123456"}'

# 查询用户信息
curl http://localhost/api/user/info/1

# 负载均衡验证
curl http://localhost/api/user/ping
```

### 商品服务

```bash
# 商品详情（Redis缓存，返回 fromCache 字段）
curl http://localhost/api/product/1

# 商品列表
curl http://localhost/api/product/list

# 负载均衡验证
curl http://localhost/api/product/ping
```

### 库存/秒杀服务

```bash
# 查询库存
curl http://localhost/api/inventory/1

# 秒杀（三层防超卖）
curl -X POST "http://localhost/api/inventory/seckill?productId=1&userId=1"

# 回滚库存
curl -X POST "http://localhost/api/inventory/rollback?productId=1&quantity=1"
```

## 目录结构

```
DistributedHomework/
├── pom.xml                    # 父 POM（Maven 多模块）
├── docker-compose.yml         # 全服务编排
├── nginx/
│   ├── Dockerfile
│   └── nginx.conf             # 负载均衡 + 动静分离
├── frontend/                  # 静态资源（动静分离）
│   ├── index.html
│   ├── css/style.css
│   └── js/app.js
├── user-service/              # 用户服务（JWT认证）
├── product-service/           # 商品服务（Redis缓存）
├── inventory-service/         # 库存服务（Redisson秒杀）
└── docs/
    ├── architecture.md        # 架构设计
    ├── er-diagram.md          # ER图
    ├── tech-stack.md          # 技术选型
    ├── cache-design.md        # 缓存三大问题
    ├── jmeter-guide.md        # 压测指南
    └── api/user-api.md        # 接口文档
```

## 作业完成清单

### 作业一：商品库存与秒杀系统设计

- ✅ 系统架构图（服务拆分）
- ✅ RESTful API 接口定义
- ✅ 数据库 ER 图（用户/商品/库存/订单四表）
- ✅ 技术栈选型说明
- ✅ Git 仓库初始化
- ✅ Spring Boot + MyBatis + MySQL 环境搭建
- ✅ 用户注册/登录功能（JWT认证）

### 作业二：高并发读

- ✅ Dockerfile + Docker Compose 容器化部署
- ✅ Nginx 负载均衡（轮询/IP Hash/最少连接三种算法）
- ✅ 动静分离（静态文件直出，动态接口代理）
- ✅ Redis 商品详情缓存
- ✅ 缓存穿透处理（空值缓存）
- ✅ 缓存击穿处理（Redisson 分布式锁）
- ✅ 缓存雪崩处理（TTL 随机偏移）
- ⏳ JMeter 压力测试（参见 docs/jmeter-guide.md）

### 进行中

- ⏳ RabbitMQ 异步创建订单
- ⏳ order-service 订单服务

## 常见问题

**端口冲突**：MySQL 默认映射到宿主机 3307，如有冲突修改 `docker-compose.yml`

**JWT Token 无效**：检查 `jwt.secret` 长度是否 >= 64 字节

**Redis 连接失败**：确认 `seckill-redis` 容器已启动：`docker ps | grep redis`

**查看实时日志**：`docker-compose logs -f user-service-1`
