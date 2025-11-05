-- 创建测试数据库
CREATE DATABASE IF NOT EXISTS `seller_management_test` 
DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `seller_management_test`;

-- 1. 卖家表（seller）：存储卖家账号信息
CREATE TABLE `seller` (
  `seller_id` INT NOT NULL AUTO_INCREMENT COMMENT '卖家唯一ID',
  `username` VARCHAR(50) NOT NULL COMMENT '登录用户名',
  `password` VARCHAR(100) NOT NULL COMMENT '加密后的密码',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '账号创建时间',
  PRIMARY KEY (`seller_id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. 商品表（products）：存储商品信息
CREATE TABLE `products` (
  `product_id` INT NOT NULL AUTO_INCREMENT COMMENT '商品唯一ID',
  `seller_id` INT NOT NULL COMMENT '所属卖家ID',
  `product_name` VARCHAR(100) NOT NULL COMMENT '商品名称',
  `product_desc` TEXT NOT NULL COMMENT '商品详细描述',
  `image_url` VARCHAR(255) NOT NULL COMMENT '商品图片路径/链接',
  `price` DECIMAL(10,2) NOT NULL COMMENT '商品价格',
  `product_status` ENUM('online', 'frozen', 'sold') NOT NULL DEFAULT 'online' COMMENT '商品状态',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上架时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (`product_id`),
  KEY `idx_seller_id` (`seller_id`),
  KEY `idx_product_status` (`product_status`),
  CONSTRAINT `fk_product_seller` FOREIGN KEY (`seller_id`) REFERENCES `seller` (`seller_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. 购买意向表（purchase_intents）：记录顾客购买意向
CREATE TABLE `purchase_intents` (
  `purchase_id` INT NOT NULL AUTO_INCREMENT COMMENT '购买意向ID',
  `product_id` INT NOT NULL COMMENT '关联的商品ID',
  `customer_name` VARCHAR(50) NOT NULL COMMENT '顾客姓名',
  `customer_phone` VARCHAR(20) NOT NULL COMMENT '顾客联系电话',
  `customer_address` VARCHAR(255) NOT NULL COMMENT '收货地址',
  `purchase_status` ENUM('pending', 'success', 'failed') NOT NULL DEFAULT 'pending' COMMENT '交易状态',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '意向提交时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (`purchase_id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_purchase_status` (`purchase_status`),
  CONSTRAINT `fk_intent_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`product_id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;