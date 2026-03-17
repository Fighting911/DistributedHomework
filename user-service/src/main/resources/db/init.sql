CREATE DATABASE IF NOT EXISTS seckill DEFAULT CHARACTER SET utf8mb4;
USE seckill;

-- 用户表
CREATE TABLE IF NOT EXISTS t_user (
    id          BIGINT          NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    username    VARCHAR(50)     NOT NULL COMMENT '用户名',
    password    VARCHAR(100)    NOT NULL COMMENT '密码（BCrypt加密）',
    email       VARCHAR(100)    DEFAULT NULL COMMENT '邮箱',
    phone       VARCHAR(20)     DEFAULT NULL COMMENT '手机号',
    status      TINYINT         NOT NULL DEFAULT 1 COMMENT '1正常 0禁用',
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 商品表（加 description、image_url）
CREATE TABLE IF NOT EXISTS t_product (
    id          BIGINT          NOT NULL AUTO_INCREMENT COMMENT '商品ID',
    name        VARCHAR(100)    NOT NULL COMMENT '商品名称',
    description VARCHAR(500)    DEFAULT NULL COMMENT '商品描述',
    image_url   VARCHAR(300)    DEFAULT NULL COMMENT '商品图片URL',
    price       DECIMAL(10,2)   NOT NULL COMMENT '商品价格',
    status      TINYINT         NOT NULL DEFAULT 1 COMMENT '1上架 0下架',
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- 库存表（加 locked_stock，更贴近真实场景）
CREATE TABLE IF NOT EXISTS t_inventory (
    id           BIGINT  NOT NULL AUTO_INCREMENT,
    product_id   BIGINT  NOT NULL COMMENT '商品ID',
    stock        INT     NOT NULL COMMENT '可用库存',
    locked_stock INT     NOT NULL DEFAULT 0 COMMENT '锁定库存（已下单未支付）',
    version      INT     NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    updated_at   DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_id (product_id),
    KEY idx_stock (stock)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存表';

-- 订单表（加 quantity、total_price）
CREATE TABLE IF NOT EXISTS t_order (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    order_no    VARCHAR(32)     NOT NULL COMMENT '订单号',
    user_id     BIGINT          NOT NULL COMMENT '用户ID',
    product_id  BIGINT          NOT NULL COMMENT '商品ID',
    quantity    INT             NOT NULL DEFAULT 1 COMMENT '购买数量',
    total_price DECIMAL(10,2)   NOT NULL COMMENT '订单总价',
    status      TINYINT         NOT NULL DEFAULT 0 COMMENT '0待支付 1已支付 2已取消 3已完成',
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_id (user_id),
    KEY idx_product_id (product_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 示例数据
INSERT IGNORE INTO t_product (id, name, description, image_url, price, status) VALUES
(1, 'iPhone 16 Pro',  '最新款苹果旗舰手机，A18 Pro芯片',   'https://example.com/iphone16pro.jpg', 9999.00, 1),
(2, 'MacBook Pro M4', '14英寸专业笔记本，M4芯片',           'https://example.com/macbookm4.jpg',   14999.00, 1),
(3, 'PS5 Pro',        '索尼次世代游戏主机升级版',            'https://example.com/ps5pro.jpg',      4299.00, 1);

INSERT IGNORE INTO t_inventory (product_id, stock, locked_stock, version) VALUES
(1, 100, 0, 0),
(2, 50,  0, 0),
(3, 200, 0, 0);
