# 数据库 ER 图

## 1. 实体关系概览

```
t_user ──────────────── t_order
  │ 1                      │ N
  │                        │
  │                   t_order_item
  │                        │ N
  │                        │ 1
t_product ───────────── t_inventory
  1                           1
```

## 2. 表结构详细设计

### 2.1 用户表（t_user）

| 字段名         | 类型           | 约束              | 说明         |
|-------------|--------------|-----------------|------------|
| id          | BIGINT       | PK, AUTO_INCREMENT | 用户ID      |
| username    | VARCHAR(50)  | UNIQUE, NOT NULL  | 用户名       |
| password    | VARCHAR(100) | NOT NULL          | 密码（BCrypt加密） |
| email       | VARCHAR(100) | UNIQUE            | 邮箱         |
| phone       | VARCHAR(20)  |                   | 手机号        |
| status      | TINYINT      | DEFAULT 1         | 状态 1正常 0禁用 |
| created_at  | DATETIME     | NOT NULL          | 创建时间       |
| updated_at  | DATETIME     |                   | 更新时间       |

---

### 2.2 商品表（t_product）

| 字段名          | 类型            | 约束              | 说明            |
|-------------|---------------|-----------------|---------------|
| id          | BIGINT        | PK, AUTO_INCREMENT | 商品ID         |
| name        | VARCHAR(100)  | NOT NULL          | 商品名称          |
| description | TEXT          |                   | 商品描述          |
| price       | DECIMAL(10,2) | NOT NULL          | 商品价格          |
| status      | TINYINT       | DEFAULT 1         | 1上架 0下架       |
| created_at  | DATETIME      | NOT NULL          | 创建时间          |
| updated_at  | DATETIME      |                   | 更新时间          |

---

### 2.3 库存表（t_inventory）

| 字段名          | 类型     | 约束              | 说明          |
|-------------|--------|-----------------|-------------|
| id          | BIGINT | PK, AUTO_INCREMENT | 库存ID       |
| product_id  | BIGINT | FK → t_product.id | 商品ID（唯一）  |
| total       | INT    | NOT NULL          | 总库存         |
| available   | INT    | NOT NULL          | 可用库存（秒杀用） |
| locked      | INT    | DEFAULT 0         | 锁定库存（已下单未付款） |
| version     | INT    | DEFAULT 0         | 乐观锁版本号     |
| updated_at  | DATETIME |               | 更新时间        |

> **乐观锁说明：** 更新时 `WHERE id=? AND version=?`，失败则重试，防止并发超卖（MySQL层兜底）

---

### 2.4 订单表（t_order）

| 字段名          | 类型            | 约束              | 说明                          |
|-------------|---------------|-----------------|-------------------------------|
| id          | BIGINT        | PK, AUTO_INCREMENT | 订单ID                       |
| order_no    | VARCHAR(32)   | UNIQUE, NOT NULL  | 订单号（雪花ID）               |
| user_id     | BIGINT        | FK → t_user.id    | 用户ID                        |
| product_id  | BIGINT        | FK → t_product.id | 商品ID                        |
| quantity    | INT           | NOT NULL          | 购买数量                       |
| total_price | DECIMAL(10,2) | NOT NULL          | 订单总价                       |
| status      | TINYINT       | DEFAULT 0         | 0待支付 1已支付 2已取消 3已完成 |
| created_at  | DATETIME      | NOT NULL          | 创建时间                       |
| updated_at  | DATETIME      |                   | 更新时间                       |

---

## 3. 关键约束说明

- `t_inventory.product_id` → `t_product.id`（一商品只有一条库存记录）
- `t_order.user_id` → `t_user.id`
- `t_order.product_id` → `t_product.id`
- 库存扣减采用**乐观锁（version字段）+ Redis原子操作**双重保障
