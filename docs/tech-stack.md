# 技术栈选型说明

## 1. 编程语言：Java 17

**选型理由：**
- Spring 生态成熟，微服务开发效率高
- Java 17 LTS 版本，支持 Record、密封类等新特性
- 企业级生产环境主流选择，社区资源丰富

---

## 2. 后端框架：Spring Boot 3.x

**选型理由：**
- 内嵌 Tomcat，开箱即用，无需额外配置容器
- 自动装配大幅降低配置成本
- 与 MyBatis、Redis 等组件集成成熟

**核心依赖：**
- `spring-boot-starter-web`：RESTful 接口开发
- `spring-boot-starter-validation`：参数校验
- `spring-boot-starter-data-redis`：Redis 操作

---

## 3. 持久层框架：MyBatis

**选型理由：**
- SQL 可控性强，适合复杂查询场景（如秒杀库存扣减的 UPDATE + version）
- 相比 JPA/Hibernate 性能更可预测
- XML 与注解两种方式灵活选择

**对比 JPA：** 秒杀场景需要精确控制 SQL（如乐观锁 UPDATE），MyBatis 更直观

---

## 4. 数据库：MySQL 8.0

**选型理由：**
- 主流关系型数据库，事务支持完善（ACID）
- InnoDB 引擎行锁机制，支持并发写入
- 8.0 版本窗口函数、JSON 支持更强

**关键配置：**
- 库存扣减使用乐观锁（`version` 字段）
- 事务隔离级别：READ COMMITTED（减少间隙锁争用）

---

## 5. 缓存中间件：Redis 7.x

**选型理由：**
- 秒杀库存预热到 Redis，`DECR` 原子操作防超卖
- QPS 可达 10万+，远超 MySQL 直接扣减
- Token 存储、接口限流（令牌桶/滑动窗口）

**核心用法：**
```
# 库存预热
SET seckill:stock:{productId} 100

# 原子扣减（返回值<0则秒杀结束）
DECR seckill:stock:{productId}
```

---

## 6. 构建工具：Maven

- 依赖管理清晰，`pom.xml` 便于版本统一
- 多模块项目结构支持良好

---

## 7. 容器化：Docker + Docker Compose

- 本地开发环境一键启动（MySQL + Redis）
- 消除"在我机器上能跑"问题，保证环境一致性

---

## 总结

| 组件    | 选型                  | 核心价值          |
|-------|---------------------|---------------|
| 语言    | Java 17             | 生态成熟，企业主流     |
| 框架    | Spring Boot 3.x     | 开发效率高         |
| ORM   | MyBatis             | SQL可控，适合复杂场景  |
| 数据库  | MySQL 8.0           | 事务可靠          |
| 缓存    | Redis 7.x           | 高并发扣库存核心      |
| 容器    | Docker Compose      | 环境一致性         |
