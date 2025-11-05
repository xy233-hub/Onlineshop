-- 简单的数据库初始化脚本
CREATE DATABASE IF NOT EXISTS `seller_management_test`;
USE `seller_management_test`;

-- 卖家表
CREATE TABLE IF NOT EXISTS `seller` (
  `seller_id` INT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(50) NOT NULL,
  `password` VARCHAR(100) NOT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`seller_id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 商品表
CREATE TABLE IF NOT EXISTS `products` (
  `product_id` INT NOT NULL AUTO_INCREMENT,
  `seller_id` INT NOT NULL,
  `product_name` VARCHAR(100) NOT NULL,
  `product_desc` TEXT NOT NULL,
  `image_url` VARCHAR(255) NOT NULL,
  `price` DECIMAL(10,2) NOT NULL,
  `product_status` ENUM('online', 'frozen', 'sold') NOT NULL DEFAULT 'online',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`product_id`),
  KEY `idx_seller_id` (`seller_id`),
  KEY `idx_product_status` (`product_status`),
  CONSTRAINT `fk_product_seller` FOREIGN KEY (`seller_id`) REFERENCES `seller` (`seller_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 购买意向表
CREATE TABLE IF NOT EXISTS `purchase_intents` (
  `purchase_id` INT NOT NULL AUTO_INCREMENT,
  `product_id` INT NOT NULL,
  `customer_name` VARCHAR(50) NOT NULL,
  `customer_phone` VARCHAR(20) NOT NULL,
  `customer_address` VARCHAR(255) NOT NULL,
  `purchase_status` ENUM('pending', 'success', 'failed') NOT NULL DEFAULT 'pending',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`purchase_id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_purchase_status` (`purchase_status`),
  CONSTRAINT `fk_intent_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`product_id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;