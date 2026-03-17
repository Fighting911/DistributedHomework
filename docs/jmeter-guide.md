# JMeter 压力测试指南

## 一、安装 JMeter

从 [jmeter.apache.org](https://jmeter.apache.org/download_jmeter.cgi) 下载最新版（5.6+），解压后运行：

```bash
# macOS / Linux
bin/jmeter

# Windows
bin/jmeter.bat
```

---

## 二、测试一：验证负载均衡

**目标：** 向 Nginx（端口80）发 100 个并发请求，观察日志确认两个实例各自处理约 50% 的请求。

### JMeter 配置

1. 新建测试计划 → 添加**线程组**
   - 线程数：50
   - Ramp-Up：5s
   - 循环次数：2（共 100 次请求）

2. 线程组 → 添加**HTTP请求**
   - 协议：http
   - 服务器：`localhost`
   - 端口：`80`
   - 方法：GET
   - 路径：`/api/user/ping`

3. 添加**察看结果树** + **聚合报告**监听器

4. 运行后在终端验证日志：

```bash
# 统计各实例处理的请求数，数量应接近 1:1
docker logs user-service-1 2>&1 | grep "pong from" | wc -l
docker logs user-service-2 2>&1 | grep "pong from" | wc -l
```

### 切换负载均衡算法

编辑 `nginx/nginx.conf`，修改 `upstream user_backend` 块：

```nginx
# 算法一：轮询（默认）
upstream user_backend {
    server user-service-1:8081;
    server user-service-2:8081;
}

# 算法二：IP Hash（同一IP每次落到同一实例）
upstream user_backend {
    ip_hash;
    server user-service-1:8081;
    server user-service-2:8081;
}

# 算法三：最少连接数
upstream user_backend {
    least_conn;
    server user-service-1:8081;
    server user-service-2:8081;
}
```

修改后重载 Nginx：

```bash
docker-compose exec nginx nginx -s reload
```

---

## 三、测试二：动静分离对比

**目标：** 分别压测静态文件和动态接口，对比响应时间。

### 静态文件压测

- 路径：`/css/style.css` 或 `/js/app.js`
- 预期：响应时间 < 5ms（Nginx 直接返回，无后端开销）

### 动态接口压测

- 路径：`/api/product/1`（触发 Redis 缓存查询）
- 预期：缓存命中后响应时间 < 20ms

### 配置方法

创建两个 HTTP 请求 Sampler，分别测试：

| Sampler        | 路径              | 预期响应时间 |
|----------------|-------------------|-----------|
| 静态-CSS        | /css/style.css    | < 5ms     |
| 静态-JS         | /js/app.js        | < 5ms     |
| 动态-商品详情   | /api/product/1    | < 30ms    |
| 动态-用户ping   | /api/user/ping    | < 30ms    |

---

## 四、测试三：Redis 缓存效果验证

**目标：** 验证缓存命中时响应时间显著低于直接查数据库。

### 步骤

1. 先清空缓存，触发冷启动（直接查 DB）：
```bash
docker-compose exec redis redis-cli FLUSHDB
```

2. JMeter 发送 200 并发请求到 `/api/product/1`，记录平均响应时间 T1

3. 再次发送 200 并发请求（此时缓存已预热），记录平均响应时间 T2

4. 预期：**T2 明显小于 T1**（Redis 缓存 vs MySQL 查询）

### 缓存监控命令

```bash
# 查看缓存 key
docker-compose exec redis redis-cli KEYS "product:*"

# 查看某个 key 的 TTL 和值
docker-compose exec redis redis-cli TTL "product:detail:1"
docker-compose exec redis redis-cli GET "product:detail:1"

# 实时监控 Redis 命令（观察是否命中缓存）
docker-compose exec redis redis-cli MONITOR
```

---

## 五、JMeter 关键指标说明

| 指标              | 含义                       | 参考标准        |
|-----------------|--------------------------|-------------|
| Average         | 平均响应时间（ms）            | 越低越好        |
| 90% Line        | 90% 请求在此时间内完成         | < 200ms 为良好 |
| Throughput      | 每秒处理请求数（TPS）          | 越高越好        |
| Error %         | 错误率                      | 应为 0%       |
