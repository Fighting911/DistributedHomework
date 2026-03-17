# 用户服务 API 接口文档

**Base URL：** `http://localhost:8081/api/user`

**通用响应格式：**
```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

---

## 1. 用户注册

**POST** `/register`

**Request Body：**
```json
{
  "username": "testuser",
  "password": "123456",
  "email": "test@example.com",
  "phone": "13800138000"
}
```

| 字段       | 类型     | 必填 | 说明              |
|----------|--------|----|--------------------|
| username | String | ✓  | 用户名，4-20位字母数字 |
| password | String | ✓  | 密码，6-20位        |
| email    | String | ✗  | 邮箱格式            |
| phone    | String | ✗  | 11位手机号          |

**Response（成功）：**
```json
{
  "code": 200,
  "message": "注册成功",
  "data": {
    "userId": 1,
    "username": "testuser"
  }
}
```

**错误码：**
| code | message         |
|------|-----------------|
| 400  | 参数校验失败      |
| 409  | 用户名已存在      |

---

## 2. 用户登录

**POST** `/login`

**Request Body：**
```json
{
  "username": "testuser",
  "password": "123456"
}
```

**Response（成功）：**
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "userId": 1,
    "username": "testuser",
    "token": "eyJhbGciOiJIUzI1NiJ9..."
  }
}
```

**错误码：**
| code | message      |
|------|--------------|
| 401  | 用户名或密码错误 |
| 403  | 账号已被禁用   |

---

## 3. 查询用户信息

**GET** `/info/{userId}`

**Headers：** `Authorization: Bearer {token}`

**Response：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "userId": 1,
    "username": "testuser",
    "email": "test@example.com",
    "createdAt": "2026-03-17T12:00:00"
  }
}
```

---

## 其他服务接口（待实现）

| 服务      | 方法   | 路径                        | 说明       |
|---------|------|-----------------------------|----------|
| 商品服务  | GET  | /api/product/list           | 商品列表    |
| 商品服务  | GET  | /api/product/{id}           | 商品详情    |
| 库存服务  | GET  | /api/inventory/{productId}  | 查询库存    |
| 库存服务  | POST | /api/inventory/deduct       | 扣减库存    |
| 订单服务  | POST | /api/order/create           | 创建订单    |
| 订单服务  | GET  | /api/order/list             | 用户订单列表 |
