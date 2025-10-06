-- 1. 卖家表（seller）：存储卖家账号信息
CREATE TABLE `seller` (
  `seller_id` INT NOT NULL AUTO_INCREMENT COMMENT '卖家唯一ID（自增主键）',
  `username` VARCHAR(50) NOT NULL COMMENT '登录用户名（唯一）',
  `password` VARCHAR(100) NOT NULL COMMENT '加密后的密码（BCrypt加密）',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '账号创建时间',
  PRIMARY KEY (`seller_id`),
  UNIQUE KEY `uk_username` (`username`) COMMENT '用户名唯一，防止重复注册'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '卖家账号表';


-- 2. 商品表（products）：存储商品信息（修正外键语法）
CREATE TABLE `products` (
  `product_id` INT NOT NULL AUTO_INCREMENT COMMENT '商品唯一ID（自增主键）',
  `seller_id` INT NOT NULL COMMENT '所属卖家ID（外键关联seller表）',
  `product_name` VARCHAR(100) NOT NULL COMMENT '商品名称',
  `product_desc` TEXT NOT NULL COMMENT '商品详细描述',
  `image_url` VARCHAR(255) NOT NULL COMMENT '商品图片路径/链接',
  `price` DECIMAL(10,2) NOT NULL COMMENT '商品价格（保留2位小数）',
  `product_status` ENUM('online', 'frozen', 'sold') NOT NULL DEFAULT 'online' COMMENT '商品状态：online=在线，frozen=已冻结，sold=已售出',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上架时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间（自动刷新）',
  PRIMARY KEY (`product_id`),
  KEY `idx_seller_id` (`seller_id`) COMMENT '按卖家查询商品的索引',
  KEY `idx_product_status` (`product_status`) COMMENT '按状态筛选商品的索引',
  -- 移除外键的COMMENT，兼容低版本MySQL
  CONSTRAINT `fk_product_seller` FOREIGN KEY (`seller_id`) REFERENCES `seller` (`seller_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '商品信息表（商品属于某个卖家，卖家删除则商品自动删除）';


-- 3. 购买意向表（purchase_intents）：记录顾客购买意向（修正外键语法）
CREATE TABLE `purchase_intents` (
  `purchase_id` INT NOT NULL AUTO_INCREMENT COMMENT '购买意向ID（自增主键）',
  `product_id` INT NOT NULL COMMENT '关联的商品ID（外键）',
  `customer_name` VARCHAR(50) NOT NULL COMMENT '顾客姓名',
  `customer_phone` VARCHAR(20) NOT NULL COMMENT '顾客联系电话',
  `customer_address` VARCHAR(255) NOT NULL COMMENT '收货地址',
  `purchase_status` ENUM('pending', 'success', 'failed') NOT NULL DEFAULT 'pending' COMMENT '交易状态：pending=待处理，success=成功，failed=失败',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '意向提交时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间（自动刷新）',
  PRIMARY KEY (`purchase_id`),
  KEY `idx_product_id` (`product_id`) COMMENT '按商品查询意向的索引',
  KEY `idx_purchase_status` (`purchase_status`) COMMENT '按交易状态筛选的索引',
  -- 移除外键的COMMENT，兼容低版本MySQL
  CONSTRAINT `fk_intent_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`product_id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '购买意向记录表（有购买意向的商品禁止删除）';


INSERT INTO `seller` (`username`, `password`) 
VALUES (
  'admin', 
  '$2a$12$ycJEdpJ1TYxBzDCmuGju2eUbTP.vm0yNYYyFpK3ahxsCLq9d2Iq32'  -- 明文密码Admin123456的加密结果
);