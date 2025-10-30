CREATE DATABASE IF NOT EXISTS onlineshop;
USE onlineshop;

-- 1. 卖家表（seller）：存储卖家账号信息
CREATE TABLE `seller` (
  `seller_id` INT NOT NULL AUTO_INCREMENT COMMENT '卖家唯一ID（自增主键）',
  `username` VARCHAR(50) NOT NULL COMMENT '登录用户名（唯一）',
  `password` VARCHAR(100) NOT NULL COMMENT '加密后的密码（BCrypt加密）',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '账号创建时间',
  PRIMARY KEY (`seller_id`),
  UNIQUE KEY `uk_username` (`username`) COMMENT '用户名唯一，防止重复注册'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '卖家账号表';

-- 2. 客户表（customers）：支持客户自由注册
CREATE TABLE `customers` (
  `customer_id` INT NOT NULL AUTO_INCREMENT COMMENT '客户唯一ID（自增主键）',
  `username` VARCHAR(20) NOT NULL COMMENT '登录用户名（唯一，6-20位）',
  `password` VARCHAR(100) NOT NULL COMMENT '加密密码',
  `phone` VARCHAR(11) NOT NULL COMMENT '联系电话（11位手机号）',
  `default_address` VARCHAR(255) COMMENT '默认交易地点（可选）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (`customer_id`),
  UNIQUE KEY `uk_customer_username` (`username`) COMMENT '客户用户名唯一',
  UNIQUE KEY `uk_customer_phone` (`phone`) COMMENT '手机号唯一'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '客户信息表';

-- 3. 商品类别表（categories）：支持2级类别管理
CREATE TABLE `categories` (
  `category_id` INT NOT NULL AUTO_INCREMENT COMMENT '类别唯一ID',
  `parent_id` INT DEFAULT NULL COMMENT '父类别ID（NULL表示一级类别）',
  `category_name` VARCHAR(100) NOT NULL COMMENT '类别名称',
  `category_level` TINYINT NOT NULL COMMENT '类别级别（1：一级类别，2：二级类别）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`category_id`),
  KEY `idx_parent_id` (`parent_id`) COMMENT '父类别索引',
  KEY `idx_category_level` (`category_level`) COMMENT '类别级别索引',
  CONSTRAINT `fk_category_parent` FOREIGN KEY (`parent_id`) REFERENCES `categories` (`category_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '商品类别表';

-- 4. 商品表（products）：升级商品信息
CREATE TABLE `products` (
  `product_id` INT NOT NULL AUTO_INCREMENT COMMENT '商品唯一ID（自增主键）',
  `seller_id` INT NOT NULL COMMENT '所属卖家ID（外键关联seller表）',
  `category_id` INT NOT NULL COMMENT '商品二级类别ID（外键关联categories表）',
  `product_name` VARCHAR(100) NOT NULL COMMENT '商品名称',
  `product_desc` TEXT NOT NULL COMMENT '商品详细描述（支持富媒体）',
  `price` DECIMAL(10,2) NOT NULL COMMENT '商品价格（保留2位小数）',
  `stock_quantity` INT NOT NULL DEFAULT 0 COMMENT '库存数量',
  `product_status` ENUM('online', 'outOfStock', 'frozen', 'sold') NOT NULL DEFAULT 'outOfStock' COMMENT '商品状态：online=在线，outOfStock=售罄下架，frozen=已冻结，sold=已售出',
  `search_keywords` VARCHAR(255) COMMENT '搜索关键词',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上架时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间（自动刷新）',
  PRIMARY KEY (`product_id`),
  KEY `idx_seller_id` (`seller_id`) COMMENT '按卖家查询商品的索引',
  KEY `idx_category_id` (`category_id`) COMMENT '按类别查询商品的索引',
  KEY `idx_product_status` (`product_status`) COMMENT '按状态筛选商品的索引',
  KEY `idx_stock_quantity` (`stock_quantity`) COMMENT '库存数量索引',
  FULLTEXT KEY `ft_search` (`product_name`, `search_keywords`) COMMENT '全文搜索索引',
  CONSTRAINT `fk_product_seller` FOREIGN KEY (`seller_id`) REFERENCES `seller` (`seller_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_product_category` FOREIGN KEY (`category_id`) REFERENCES `categories` (`category_id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '商品信息表';

-- 5. 商品图片表（product_images）：支持多张图片
CREATE TABLE `product_images` (
  `image_id` INT NOT NULL AUTO_INCREMENT COMMENT '图片唯一ID',
  `product_id` INT NOT NULL COMMENT '关联的商品ID',
  `image_url` VARCHAR(255) NOT NULL COMMENT '图片路径/链接',
  `image_order` TINYINT NOT NULL DEFAULT 0 COMMENT '图片排序（0-4，0为封面图）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  PRIMARY KEY (`image_id`),
  KEY `idx_product_id` (`product_id`) COMMENT '商品ID索引',
  CONSTRAINT `fk_image_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`product_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '商品图片表';

-- 6. 媒体资源表（media_resources）：统一管理富媒体资源
CREATE TABLE `media_resources` (
  `media_id` INT NOT NULL AUTO_INCREMENT COMMENT '媒体资源唯一ID',
  `product_id` INT NOT NULL COMMENT '关联的商品ID',
  `media_type` ENUM('image', 'video', 'audio') NOT NULL DEFAULT 'image' COMMENT '媒体类型：image=图片，video=视频，audio=音频',
  `media_url` VARCHAR(255) NOT NULL COMMENT '媒体文件路径/链接',
  `file_name` VARCHAR(100) NOT NULL COMMENT '原始文件名',
  `file_size` INT NOT NULL COMMENT '文件大小（字节）',
  `mime_type` VARCHAR(50) NOT NULL COMMENT '文件MIME类型',
  `display_order` TINYINT NOT NULL DEFAULT 0 COMMENT '显示顺序',
  `is_embedded` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否嵌入在商品描述中',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  PRIMARY KEY (`media_id`),
  KEY `idx_product_id` (`product_id`) COMMENT '商品ID索引',
  KEY `idx_media_type` (`media_type`) COMMENT '媒体类型索引',
  KEY `idx_display_order` (`display_order`) COMMENT '显示顺序索引',
  KEY `idx_embedded` (`is_embedded`) COMMENT '是否嵌入索引',
  CONSTRAINT `fk_media_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`product_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '媒体资源表';

-- 7. 购买意向表（purchase_intents）：升级购买意向信息
CREATE TABLE `purchase_intents` (
  `purchase_id` INT NOT NULL AUTO_INCREMENT COMMENT '购买意向ID（自增主键）',
  `product_id` INT NOT NULL COMMENT '关联的商品ID（外键）',
  `customer_id` INT NOT NULL COMMENT '客户ID（外键关联customers表）',
  `customer_name` VARCHAR(50) NOT NULL COMMENT '顾客姓名',
  `customer_phone` VARCHAR(20) NOT NULL COMMENT '顾客联系电话',
  `customer_address` VARCHAR(255) NOT NULL COMMENT '收货地址',
  `quantity` INT NOT NULL DEFAULT 1 COMMENT '购买数量',
  `total_amount` DECIMAL(10,2) NOT NULL COMMENT '订单总金额',
  `purchase_status` ENUM('pending', 'success', 'failed') NOT NULL DEFAULT 'pending' COMMENT '交易状态：pending=待处理，success=成功，failed=失败',
  `seller_notes` VARCHAR(500) COMMENT '商家处理备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '意向提交时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间（自动刷新）',
  PRIMARY KEY (`purchase_id`),
  KEY `idx_product_id` (`product_id`) COMMENT '按商品查询意向的索引',
  KEY `idx_customer_id` (`customer_id`) COMMENT '按客户查询意向的索引',
  KEY `idx_purchase_status` (`purchase_status`) COMMENT '按交易状态筛选的索引',
  CONSTRAINT `fk_intent_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`product_id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_intent_customer` FOREIGN KEY (`customer_id`) REFERENCES `customers` (`customer_id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '购买意向记录表';

-- 插入初始数据
INSERT INTO `seller` (`username`, `password`) 
VALUES (
  'admin', 
  '$2a$12$ycJEdpJ1TYxBzDCmuGju2eUbTP.vm0yNYYyFpK3ahxsCLq9d2Iq32'
);

-- 插入示例类别数据
INSERT INTO `categories` (`parent_id`, `category_name`, `category_level`) VALUES
(NULL, '电子产品', 1),
(NULL, '服装服饰', 1),
(1, '手机', 2),
(1, '笔记本电脑', 2),
(2, '男装', 2),
(2, '女装', 2);