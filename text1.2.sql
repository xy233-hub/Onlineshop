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



-- 8. 创建商品收藏表
CREATE TABLE favorites (
    favorite_id INT NOT NULL AUTO_INCREMENT COMMENT '收藏记录唯一ID',
    customer_id INT NOT NULL COMMENT '客户ID（外键关联customers表）',
    product_id INT NOT NULL COMMENT '商品ID（外键关联products表）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',

    PRIMARY KEY (favorite_id),
    UNIQUE KEY uk_customer_product (customer_id, product_id) COMMENT '同一客户不能重复收藏同一商品',
    KEY idx_customer_id (customer_id) COMMENT '按客户查询收藏的索引',
    KEY idx_product_id (product_id) COMMENT '按商品查询收藏的索引',

    CONSTRAINT fk_favorite_customer FOREIGN KEY (customer_id)
        REFERENCES customers (customer_id) ON DELETE CASCADE,
    CONSTRAINT fk_favorite_product FOREIGN KEY (product_id)
        REFERENCES products (product_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '商品收藏表';

-- 9. 创建购物车表
CREATE TABLE shopping_cart (
    cart_item_id INT NOT NULL AUTO_INCREMENT COMMENT '购物车项目唯一ID',
    customer_id INT NOT NULL COMMENT '客户ID（外键关联customers表）',
    product_id INT NOT NULL COMMENT '商品ID（外键关联products表）',
    quantity INT NOT NULL DEFAULT 1 COMMENT '商品数量',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '添加到购物车的时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',

    PRIMARY KEY (cart_item_id),
    UNIQUE KEY uk_customer_product (customer_id, product_id) COMMENT '同一客户不能重复添加同一商品到购物车',
    KEY idx_customer_id (customer_id) COMMENT '按客户查询购物车项目的索引',
    KEY idx_product_id (product_id) COMMENT '按商品查询购物车项目的索引',

    CONSTRAINT fk_cart_customer FOREIGN KEY (customer_id)
        REFERENCES customers (customer_id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_product FOREIGN KEY (product_id)
        REFERENCES products (product_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '购物车表';

-- 10. 创建购买意向商品项表
CREATE TABLE purchase_intent_items (
    item_id INT NOT NULL AUTO_INCREMENT COMMENT '购买意向商品项唯一ID',
    purchase_id INT NOT NULL COMMENT '购买意向ID（外键关联purchase_intents表）',
    product_id INT NOT NULL COMMENT '商品ID（外键关联products表）',
    product_name VARCHAR(100) NOT NULL COMMENT '下单时的商品名称（快照）',
    product_price DECIMAL(10,2) NOT NULL COMMENT '下单时的商品单价（快照）',
    quantity INT NOT NULL DEFAULT 1 COMMENT '购买数量',
    subtotal DECIMAL(10,2) NOT NULL COMMENT '小计金额',

    PRIMARY KEY (item_id),
    KEY idx_purchase_id (purchase_id) COMMENT '按购买意向查询商品项的索引',
    KEY idx_product_id (product_id) COMMENT '按商品查询购买意向项的索引',

    CONSTRAINT fk_item_purchase_intent FOREIGN KEY (purchase_id)
        REFERENCES purchase_intents (purchase_id) ON DELETE CASCADE,
    CONSTRAINT fk_item_product FOREIGN KEY (product_id)
        REFERENCES products (product_id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '购买意向商品项表';

-- 插入初始数据
INSERT INTO `seller` (`username`, `password`)
VALUES (
           'admin',
           '$2a$12$ycJEdpJ1TYxBzDCmuGju2eUbTP.vm0yNYYyFpK3ahxsCLq9d2Iq32'
       );


ALTER TABLE media_resources
    MODIFY COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- 步骤1：添加新字段
ALTER TABLE purchase_intents
    ADD COLUMN cancel_reason VARCHAR(100) DEFAULT NULL COMMENT '取消原因',
ADD COLUMN cancel_notes VARCHAR(255) DEFAULT NULL COMMENT '取消备注';

-- 步骤2：修改 purchase_status 字段的ENUM定义
ALTER TABLE purchase_intents
    MODIFY COLUMN purchase_status ENUM(
    'CUSTOMER_ORDERED',
    'SELLER_CONFIRMED',
    'STOCK_PREPARED',
    'SHIPPING_STARTED',
    'COMPLETED',
    'CUSTOMER_CANCELLED',
    'SELLER_CANCELLED'
    ) NOT NULL DEFAULT 'CUSTOMER_ORDERED' COMMENT '交易状态：CUSTOMER_ORDERED=客户下单, SELLER_CONFIRMED=商家确认, STOCK_PREPARED=备货完成, SHIPPING_STARTED=开始发货, COMPLETED=交易完成, CUSTOMER_CANCELLED=客户取消订单, SELLER_CANCELLED=商家取消订单';

-- 步骤3：数据迁移 - 更新现有状态值
UPDATE purchase_intents
SET purchase_status = 'CUSTOMER_ORDERED'
WHERE purchase_status = 'pending';

UPDATE purchase_intents
SET purchase_status = 'COMPLETED'
WHERE purchase_status = 'success';

-- 对于失败的订单，需要根据具体情况区分取消方
-- 这里假设所有原failed状态都是客户取消（实际情况可能需要业务逻辑判断）

START TRANSACTION;

-- 1) 将 purchase_intents 中存在的 product_id 数据迁移为 purchase_intent_items（仅当还没有对应 item 时）
INSERT INTO purchase_intent_items (purchase_id, product_id, product_name, product_price, quantity, subtotal)
SELECT pi.purchase_id,
       pi.product_id,
       COALESCE(p.product_name, '') AS product_name,
       COALESCE(p.price, 0.00) AS product_price,
       COALESCE(pi.quantity, 1) AS quantity,
       COALESCE(pi.total_amount, COALESCE(p.price,0.00) * COALESCE(pi.quantity,1)) AS subtotal
FROM purchase_intents pi
         LEFT JOIN purchase_intent_items pii ON pi.purchase_id = pii.purchase_id
         LEFT JOIN products p ON p.product_id = pi.product_id
WHERE pi.product_id IS NOT NULL
  AND pii.item_id IS NULL;

-- 2) 删除外键约束（约束名按现有库中实际名称调整）
ALTER TABLE purchase_intents DROP FOREIGN KEY fk_intent_product;

-- 3) 删除冗余列
ALTER TABLE purchase_intents DROP COLUMN product_id;

COMMIT;


-- 11. 创建支付记录表
CREATE TABLE payments (
    payment_id INT NOT NULL AUTO_INCREMENT COMMENT '支付记录唯一 ID',
    purchase_id INT NOT NULL COMMENT '关联的购买意向 ID（外键关联 purchase_intents 表）',
    customer_id INT NOT NULL COMMENT '客户 ID（外键关联 customers 表）',
    payment_method ENUM('BANK_CARD', 'CREDIT_CARD', 'ALIPAY', 'WECHAT_PAY') NOT NULL COMMENT '支付方式：BANK_CARD=银行卡，CREDIT_CARD=信用卡，ALIPAY=支付宝，WECHAT_PAY=微信支付',
    payment_amount DECIMAL(10,2) NOT NULL COMMENT '支付金额',
    payment_status ENUM('PENDING', 'PAID', 'FAILED', 'REFUNDING', 'REFUNDED') NOT NULL DEFAULT 'PENDING' COMMENT '支付状态：PENDING=待支付，PAID=已支付，FAILED=支付失败，REFUNDING=退款中，REFUNDED=已退款',
    transaction_id VARCHAR(100) COMMENT '第三方支付交易号',
    payment_time DATETIME COMMENT '支付成功时间',
    refund_time DATETIME COMMENT '退款时间',
    refund_amount DECIMAL(10,2) COMMENT '退款金额',
    refund_reason VARCHAR(255) COMMENT '退款原因',
    payment_expiry DATETIME NOT NULL COMMENT '支付过期时间（订单创建后 30 分钟）',
    payment_notes VARCHAR(500) COMMENT '支付备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',

    PRIMARY KEY (payment_id),
    UNIQUE KEY uk_purchase_id (purchase_id) COMMENT '一个购买意向对应一个支付记录',
    KEY idx_customer_id (customer_id) COMMENT '按客户查询支付的索引',
    KEY idx_payment_status (payment_status) COMMENT '按支付状态筛选的索引',
    KEY idx_payment_method (payment_method) COMMENT '按支付方式筛选的索引',
    KEY idx_transaction_id (transaction_id) COMMENT '按第三方交易号查询的索引',
    KEY idx_payment_expiry (payment_expiry) COMMENT '支付过期时间索引',

    CONSTRAINT fk_payment_purchase_intent FOREIGN KEY (purchase_id)
        REFERENCES purchase_intents (purchase_id) ON DELETE CASCADE,
    CONSTRAINT fk_payment_customer FOREIGN KEY (customer_id)
        REFERENCES customers (customer_id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '支付记录表';

-- 12. 在 purchase_intents 表中添加支付状态字段
ALTER TABLE purchase_intents
    ADD COLUMN payment_status ENUM('UNPAID', 'PAID', 'REFUNDING', 'REFUNDED') DEFAULT 'UNPAID' COMMENT '支付状态：UNPAID=未支付，PAID=已支付，REFUNDING=退款中，REFUNDED=已退款',
    ADD COLUMN payment_verify_token VARCHAR(100) COMMENT '支付结果校验令牌';

-- 插入示例支付数据
INSERT INTO `payments` (`purchase_id`, `customer_id`, `payment_method`, `payment_amount`, `payment_status`, `payment_expiry`)
SELECT
    pi.purchase_id,
    pi.customer_id,
    'ALIPAY',
    pi.total_amount,
    CASE
        WHEN pi.purchase_status IN ('COMPLETED', 'SHIPPING_STARTED', 'STOCK_PREPARED', 'SELLER_CONFIRMED') THEN 'PAID'
        ELSE 'PENDING'
    END,
    DATE_ADD(pi.created_at, INTERVAL 30 MINUTE)
FROM purchase_intents pi
WHERE NOT EXISTS (
    SELECT 1 FROM payments p WHERE p.purchase_id = pi.purchase_id
);
ALTER TABLE payments MODIFY COLUMN payment_method ENUM('BANK_CARD', 'CREDIT_CARD', 'ALIPAY', 'WECHAT_PAY') NULL COMMENT '支付方式：BANK_CARD=银行卡，CREDIT_CARD=信用卡，ALIPAY=支付宝，WECHAT_PAY=微信支付';

-- 13. 创建收货地址表
CREATE TABLE customer_addresses (
    address_id INT NOT NULL AUTO_INCREMENT COMMENT '地址唯一 ID',
    customer_id INT NOT NULL COMMENT '客户 ID（外键关联 customers 表）',
    recipient_name VARCHAR(50) NOT NULL COMMENT '收货人姓名',
    recipient_phone VARCHAR(20) NOT NULL COMMENT '收货人联系电话',
    province VARCHAR(50) NOT NULL COMMENT '省份/直辖市',
    city VARCHAR(50) NOT NULL COMMENT '城市',
    district VARCHAR(50) NOT NULL COMMENT '区/县',
    detail_address VARCHAR(255) NOT NULL COMMENT '详细地址（街道、门牌号等）',
    is_default BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否为默认地址（true=默认）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',

    PRIMARY KEY (address_id),
    KEY idx_customer_id (customer_id) COMMENT '按客户查询地址的索引',
    KEY idx_is_default (is_default) COMMENT '默认地址索引',

    CONSTRAINT fk_address_customer FOREIGN KEY (customer_id)
        REFERENCES customers (customer_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '客户收货地址表';

-- 14. 创建物流配置表
CREATE TABLE logistics_providers (
    provider_id INT NOT NULL AUTO_INCREMENT COMMENT '物流商唯一 ID',
    provider_name VARCHAR(100) NOT NULL COMMENT '物流商名称（如：顺丰速运、中通快递）',
    provider_code VARCHAR(50) NOT NULL COMMENT '物流商编码（用于对接第三方物流 API）',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序顺序（数字越小越靠前）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    PRIMARY KEY (provider_id),
    UNIQUE KEY uk_provider_code (provider_code) COMMENT '物流商编码唯一',
    KEY idx_enabled (enabled) COMMENT '启用状态索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '物流服务商配置表';

-- 15. 在 purchase_intents 表中添加物流相关字段
ALTER TABLE purchase_intents
    ADD COLUMN logistics_provider_id INT COMMENT '物流商 ID（外键关联 logistics_providers 表）',
    ADD COLUMN logistics_type ENUM('OFFLINE', 'EXPRESS') NOT NULL DEFAULT 'EXPRESS' COMMENT '配送方式：OFFLINE=线下配送，EXPRESS=快递配送',
    ADD COLUMN tracking_no VARCHAR(100) COMMENT '物流单号',
    ADD COLUMN shipped_at DATETIME COMMENT '发货时间',
    ADD COLUMN received_at DATETIME COMMENT '客户确认收货时间';

-- 添加外键约束
ALTER TABLE purchase_intents
    ADD CONSTRAINT fk_intent_logistics FOREIGN KEY (logistics_provider_id)
        REFERENCES logistics_providers (provider_id) ON DELETE SET NULL;

-- 16. 创建物流轨迹表
CREATE TABLE logistics_tracks (
    track_id INT NOT NULL AUTO_INCREMENT COMMENT '物流轨迹唯一 ID',
    purchase_id INT NOT NULL COMMENT '购买意向 ID（外键关联 purchase_intents 表）',
    track_time DATETIME NOT NULL COMMENT '轨迹发生时间',
    track_content VARCHAR(500) NOT NULL COMMENT '轨迹内容描述',
    track_location VARCHAR(255) COMMENT '轨迹发生地点',
    track_status VARCHAR(50) COMMENT '当前物流状态（如：运输中、已签收、派送中）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',

    PRIMARY KEY (track_id),
    KEY idx_purchase_id (purchase_id) COMMENT '按订单查询物流轨迹的索引',
    KEY idx_track_time (track_time) COMMENT '轨迹时间索引',

    CONSTRAINT fk_track_purchase FOREIGN KEY (purchase_id)
        REFERENCES purchase_intents (purchase_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '物流轨迹记录表';

-- 17. 创建售后服务表
CREATE TABLE after_sales_services (
    service_id INT NOT NULL AUTO_INCREMENT COMMENT '售后服务单唯一 ID',
    purchase_id INT NOT NULL COMMENT '关联的购买意向 ID（外键关联 purchase_intents 表）',
    product_id INT NOT NULL COMMENT '商品 ID（外键关联 products 表）',
    customer_id INT NOT NULL COMMENT '客户 ID（外键关联 customers 表）',
    service_type ENUM('REFUND_ONLY', 'RETURN_AND_REFUND', 'EXCHANGE') NOT NULL COMMENT '售后类型：REFUND_ONLY=仅退款，RETURN_AND_REFUND=退货退款，EXCHANGE=换货',
    service_title VARCHAR(200) NOT NULL COMMENT '售后标题',
    problem_description TEXT NOT NULL COMMENT '问题描述（富文本）',
    evidence_images JSON COMMENT '凭证图片 URL 数组',
    refund_amount DECIMAL(10,2) NOT NULL COMMENT '申请退款金额',
    service_status ENUM('PENDING', 'NEGOTIATING', 'AGREED', 'REJECTED', 'COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'PENDING' COMMENT '售后状态：PENDING=待处理，NEGOTIATING=协商中，AGREED=同意，REJECTED=拒绝，COMPLETED=已完成，CANCELLED=已取消',
    seller_response TEXT COMMENT '商家回复',
    seller_decision ENUM('AGREE_REFUND', 'AGREE_RETURN_REFUND', 'AGREE_EXCHANGE', 'REJECT') COMMENT '商家处理决定',
    seller_decision_at DATETIME COMMENT '商家处理决定时间',
    return_tracking_no VARCHAR(100) COMMENT '客户退货物流单号',
    return_logistics_provider VARCHAR(100) COMMENT '客户退货物流公司',
    return_shipped_at DATETIME COMMENT '客户退货发货时间',
    return_received_at DATETIME COMMENT '商家确认收货时间',
    completed_at DATETIME COMMENT '售后完成时间',
    cancelled_at DATETIME COMMENT '售后取消时间',
    cancel_reason VARCHAR(255) COMMENT '取消原因',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',

    PRIMARY KEY (service_id),
    KEY idx_purchase_id (purchase_id) COMMENT '按订单查询售后的索引',
    KEY idx_product_id (product_id) COMMENT '按商品查询售后的索引',
    KEY idx_customer_id (customer_id) COMMENT '按客户查询售后的索引',
    KEY idx_service_status (service_status) COMMENT '售后状态索引',

    CONSTRAINT fk_after_sales_purchase FOREIGN KEY (purchase_id)
        REFERENCES purchase_intents (purchase_id) ON DELETE RESTRICT,
    CONSTRAINT fk_after_sales_product FOREIGN KEY (product_id)
        REFERENCES products (product_id) ON DELETE RESTRICT,
    CONSTRAINT fk_after_sales_customer FOREIGN KEY (customer_id)
        REFERENCES customers (customer_id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '售后服务表';

-- 插入初始物流商数据
INSERT INTO `logistics_providers` (`provider_name`, `provider_code`, `enabled`, `sort_order`) VALUES
('顺丰速运', 'SF', TRUE, 1),
('京东物流', 'JD', TRUE, 2),
('中通快递', 'ZTO', TRUE, 3),
('圆通速递', 'YTO', TRUE, 4),
('申通快递', 'STO', TRUE, 5),
('韵达速递', 'YD', TRUE, 6),
('邮政 EMS', 'EMS', TRUE, 7),
('线下配送', 'OFFLINE', TRUE, 8);

-- 在 payments 表中添加售后相关字段
ALTER TABLE payments
    ADD COLUMN after_sales_service_id INT COMMENT '关联的售后服务单 ID',
    ADD CONSTRAINT fk_payment_after_sales FOREIGN KEY (after_sales_service_id)
        REFERENCES after_sales_services (service_id) ON DELETE SET NULL;


-- 11. 创建支付记录表
CREATE TABLE payments (
    payment_id INT NOT NULL AUTO_INCREMENT COMMENT '支付记录唯一 ID',
    purchase_id INT NOT NULL COMMENT '关联的购买意向 ID（外键关联 purchase_intents 表）',
    customer_id INT NOT NULL COMMENT '客户 ID（外键关联 customers 表）',
    payment_method ENUM('BANK_CARD', 'CREDIT_CARD', 'ALIPAY', 'WECHAT_PAY') NOT NULL COMMENT '支付方式：BANK_CARD=银行卡，CREDIT_CARD=信用卡，ALIPAY=支付宝，WECHAT_PAY=微信支付',
    payment_amount DECIMAL(10,2) NOT NULL COMMENT '支付金额',
    payment_status ENUM('PENDING', 'PAID', 'FAILED', 'REFUNDING', 'REFUNDED') NOT NULL DEFAULT 'PENDING' COMMENT '支付状态：PENDING=待支付，PAID=已支付，FAILED=支付失败，REFUNDING=退款中，REFUNDED=已退款',
    transaction_id VARCHAR(100) COMMENT '第三方支付交易号',
    payment_time DATETIME COMMENT '支付成功时间',
    refund_time DATETIME COMMENT '退款时间',
    refund_amount DECIMAL(10,2) COMMENT '退款金额',
    refund_reason VARCHAR(255) COMMENT '退款原因',
    payment_expiry DATETIME NOT NULL COMMENT '支付过期时间（订单创建后 30 分钟）',
    payment_notes VARCHAR(500) COMMENT '支付备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',

    PRIMARY KEY (payment_id),
    UNIQUE KEY uk_purchase_id (purchase_id) COMMENT '一个购买意向对应一个支付记录',
    KEY idx_customer_id (customer_id) COMMENT '按客户查询支付的索引',
    KEY idx_payment_status (payment_status) COMMENT '按支付状态筛选的索引',
    KEY idx_payment_method (payment_method) COMMENT '按支付方式筛选的索引',
    KEY idx_transaction_id (transaction_id) COMMENT '按第三方交易号查询的索引',
    KEY idx_payment_expiry (payment_expiry) COMMENT '支付过期时间索引',

    CONSTRAINT fk_payment_purchase_intent FOREIGN KEY (purchase_id)
        REFERENCES purchase_intents (purchase_id) ON DELETE CASCADE,
    CONSTRAINT fk_payment_customer FOREIGN KEY (customer_id)
        REFERENCES customers (customer_id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '支付记录表';

-- 12. 在 purchase_intents 表中添加支付状态字段
ALTER TABLE purchase_intents
    ADD COLUMN payment_status ENUM('UNPAID', 'PAID', 'REFUNDING', 'REFUNDED') DEFAULT 'UNPAID' COMMENT '支付状态：UNPAID=未支付，PAID=已支付，REFUNDING=退款中，REFUNDED=已退款',
    ADD COLUMN payment_verify_token VARCHAR(100) COMMENT '支付结果校验令牌';

-- 插入示例支付数据
INSERT INTO `payments` (`purchase_id`, `customer_id`, `payment_method`, `payment_amount`, `payment_status`, `payment_expiry`)
SELECT
    pi.purchase_id,
    pi.customer_id,
    'ALIPAY',
    pi.total_amount,
    CASE
        WHEN pi.purchase_status IN ('COMPLETED', 'SHIPPING_STARTED', 'STOCK_PREPARED', 'SELLER_CONFIRMED') THEN 'PAID'
        ELSE 'PENDING'
    END,
    DATE_ADD(pi.created_at, INTERVAL 30 MINUTE)
FROM purchase_intents pi
WHERE NOT EXISTS (
    SELECT 1 FROM payments p WHERE p.purchase_id = pi.purchase_id
);
ALTER TABLE payments MODIFY COLUMN payment_method ENUM('BANK_CARD', 'CREDIT_CARD', 'ALIPAY', 'WECHAT_PAY') NULL COMMENT '支付方式：BANK_CARD=银行卡，CREDIT_CARD=信用卡，ALIPAY=支付宝，WECHAT_PAY=微信支付';

-- 13. 创建收货地址表
CREATE TABLE customer_addresses (
    address_id INT NOT NULL AUTO_INCREMENT COMMENT '地址唯一 ID',
    customer_id INT NOT NULL COMMENT '客户 ID（外键关联 customers 表）',
    recipient_name VARCHAR(50) NOT NULL COMMENT '收货人姓名',
    recipient_phone VARCHAR(20) NOT NULL COMMENT '收货人联系电话',
    province VARCHAR(50) NOT NULL COMMENT '省份/直辖市',
    city VARCHAR(50) NOT NULL COMMENT '城市',
    district VARCHAR(50) NOT NULL COMMENT '区/县',
    detail_address VARCHAR(255) NOT NULL COMMENT '详细地址（街道、门牌号等）',
    is_default BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否为默认地址（true=默认）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',

    PRIMARY KEY (address_id),
    KEY idx_customer_id (customer_id) COMMENT '按客户查询地址的索引',
    KEY idx_is_default (is_default) COMMENT '默认地址索引',

    CONSTRAINT fk_address_customer FOREIGN KEY (customer_id)
        REFERENCES customers (customer_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '客户收货地址表';

-- 14. 创建物流配置表
CREATE TABLE logistics_providers (
    provider_id INT NOT NULL AUTO_INCREMENT COMMENT '物流商唯一 ID',
    provider_name VARCHAR(100) NOT NULL COMMENT '物流商名称（如：顺丰速运、中通快递）',
    provider_code VARCHAR(50) NOT NULL COMMENT '物流商编码（用于对接第三方物流 API）',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序顺序（数字越小越靠前）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    PRIMARY KEY (provider_id),
    UNIQUE KEY uk_provider_code (provider_code) COMMENT '物流商编码唯一',
    KEY idx_enabled (enabled) COMMENT '启用状态索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '物流服务商配置表';

-- 15. 在 purchase_intents 表中添加物流相关字段
ALTER TABLE purchase_intents
    ADD COLUMN logistics_provider_id INT COMMENT '物流商 ID（外键关联 logistics_providers 表）',
    ADD COLUMN logistics_type ENUM('OFFLINE', 'EXPRESS') NOT NULL DEFAULT 'EXPRESS' COMMENT '配送方式：OFFLINE=线下配送，EXPRESS=快递配送',
    ADD COLUMN tracking_no VARCHAR(100) COMMENT '物流单号',
    ADD COLUMN shipped_at DATETIME COMMENT '发货时间',
    ADD COLUMN received_at DATETIME COMMENT '客户确认收货时间';

-- 添加外键约束
ALTER TABLE purchase_intents
    ADD CONSTRAINT fk_intent_logistics FOREIGN KEY (logistics_provider_id)
        REFERENCES logistics_providers (provider_id) ON DELETE SET NULL;

-- 16. 创建物流轨迹表
CREATE TABLE logistics_tracks (
    track_id INT NOT NULL AUTO_INCREMENT COMMENT '物流轨迹唯一 ID',
    purchase_id INT NOT NULL COMMENT '购买意向 ID（外键关联 purchase_intents 表）',
    track_time DATETIME NOT NULL COMMENT '轨迹发生时间',
    track_content VARCHAR(500) NOT NULL COMMENT '轨迹内容描述',
    track_location VARCHAR(255) COMMENT '轨迹发生地点',
    track_status VARCHAR(50) COMMENT '当前物流状态（如：运输中、已签收、派送中）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',

    PRIMARY KEY (track_id),
    KEY idx_purchase_id (purchase_id) COMMENT '按订单查询物流轨迹的索引',
    KEY idx_track_time (track_time) COMMENT '轨迹时间索引',

    CONSTRAINT fk_track_purchase FOREIGN KEY (purchase_id)
        REFERENCES purchase_intents (purchase_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '物流轨迹记录表';

-- 17. 创建售后服务表
CREATE TABLE after_sales_services (
    service_id INT NOT NULL AUTO_INCREMENT COMMENT '售后服务单唯一 ID',
    purchase_id INT NOT NULL COMMENT '关联的购买意向 ID（外键关联 purchase_intents 表）',
    product_id INT NOT NULL COMMENT '商品 ID（外键关联 products 表）',
    customer_id INT NOT NULL COMMENT '客户 ID（外键关联 customers 表）',
    service_type ENUM('REFUND_ONLY', 'RETURN_AND_REFUND', 'EXCHANGE') NOT NULL COMMENT '售后类型：REFUND_ONLY=仅退款，RETURN_AND_REFUND=退货退款，EXCHANGE=换货',
    service_title VARCHAR(200) NOT NULL COMMENT '售后标题',
    problem_description TEXT NOT NULL COMMENT '问题描述（富文本）',
    evidence_images JSON COMMENT '凭证图片 URL 数组',
    refund_amount DECIMAL(10,2) NOT NULL COMMENT '申请退款金额',
    service_status ENUM('PENDING', 'NEGOTIATING', 'AGREED', 'REJECTED', 'COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'PENDING' COMMENT '售后状态：PENDING=待处理，NEGOTIATING=协商中，AGREED=同意，REJECTED=拒绝，COMPLETED=已完成，CANCELLED=已取消',
    seller_response TEXT COMMENT '商家回复',
    seller_decision ENUM('AGREE_REFUND', 'AGREE_RETURN_REFUND', 'AGREE_EXCHANGE', 'REJECT') COMMENT '商家处理决定',
    seller_decision_at DATETIME COMMENT '商家处理决定时间',
    return_tracking_no VARCHAR(100) COMMENT '客户退货物流单号',
    return_logistics_provider VARCHAR(100) COMMENT '客户退货物流公司',
    return_shipped_at DATETIME COMMENT '客户退货发货时间',
    return_received_at DATETIME COMMENT '商家确认收货时间',
    completed_at DATETIME COMMENT '售后完成时间',
    cancelled_at DATETIME COMMENT '售后取消时间',
    cancel_reason VARCHAR(255) COMMENT '取消原因',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',

    PRIMARY KEY (service_id),
    KEY idx_purchase_id (purchase_id) COMMENT '按订单查询售后的索引',
    KEY idx_product_id (product_id) COMMENT '按商品查询售后的索引',
    KEY idx_customer_id (customer_id) COMMENT '按客户查询售后的索引',
    KEY idx_service_status (service_status) COMMENT '售后状态索引',

    CONSTRAINT fk_after_sales_purchase FOREIGN KEY (purchase_id)
        REFERENCES purchase_intents (purchase_id) ON DELETE RESTRICT,
    CONSTRAINT fk_after_sales_product FOREIGN KEY (product_id)
        REFERENCES products (product_id) ON DELETE RESTRICT,
    CONSTRAINT fk_after_sales_customer FOREIGN KEY (customer_id)
        REFERENCES customers (customer_id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '售后服务表';

-- 插入初始物流商数据
INSERT INTO `logistics_providers` (`provider_name`, `provider_code`, `enabled`, `sort_order`) VALUES
('顺丰速运', 'SF', TRUE, 1),
('京东物流', 'JD', TRUE, 2),
('中通快递', 'ZTO', TRUE, 3),
('圆通速递', 'YTO', TRUE, 4),
('申通快递', 'STO', TRUE, 5),
('韵达速递', 'YD', TRUE, 6),
('邮政 EMS', 'EMS', TRUE, 7),
('线下配送', 'OFFLINE', TRUE, 8);

-- 在 payments 表中添加售后相关字段
ALTER TABLE payments
    ADD COLUMN after_sales_service_id INT COMMENT '关联的售后服务单 ID',
    ADD CONSTRAINT fk_payment_after_sales FOREIGN KEY (after_sales_service_id)
        REFERENCES after_sales_services (service_id) ON DELETE SET NULL;

-- 1. 先查看当前的外键名称（确认一下）
SHOW CREATE TABLE onlineshop.products;

-- 2. 删除外键约束 fk_product_seller
ALTER TABLE onlineshop.products DROP FOREIGN KEY fk_product_seller;

-- 3. 验证是否删除成功
SHOW CREATE TABLE onlineshop.products;

-- 4. 可选：添加索引提高查询性能
CREATE INDEX idx_products_seller_id ON products(seller_id);

ALTER TABLE products
    ADD COLUMN short_desc VARCHAR(300) NULL COMMENT '商品缩减描述（列表页/卡片展示用）'

-- ============================================
-- 18. 商品价格历史记录表
-- ============================================
CREATE TABLE product_price_history (
    history_id INT NOT NULL AUTO_INCREMENT COMMENT '价格历史记录唯一ID',
    product_id INT NOT NULL COMMENT '商品ID（外键关联products表）',
    old_price DECIMAL(10,2) NOT NULL COMMENT '变更前价格',
    new_price DECIMAL(10,2) NOT NULL COMMENT '变更后价格',
    change_type ENUM('MANUAL', 'PROMOTION_START', 'PROMOTION_END', 'SYSTEM') NOT NULL DEFAULT 'MANUAL' COMMENT '变更类型：MANUAL=手动修改，PROMOTION_START=促销开始，PROMOTION_END=促销结束，SYSTEM=系统调整',
    change_reason VARCHAR(255) COMMENT '价格变更原因/备注',
    changed_by INT COMMENT '操作人ID（卖家ID或系统ID=0）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '价格变更时间',
    
    PRIMARY KEY (history_id),
    KEY idx_product_id (product_id) COMMENT '按商品查询价格历史的索引',
    KEY idx_created_at (created_at) COMMENT '按时间查询的索引',
    KEY idx_change_type (change_type) COMMENT '按变更类型筛选的索引',
    
    CONSTRAINT fk_price_history_product FOREIGN KEY (product_id)
        REFERENCES products (product_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '商品价格历史记录表';

-- ============================================
-- 19. 价格变动提醒配置表
-- ============================================
CREATE TABLE price_alert_settings (
    alert_id INT NOT NULL AUTO_INCREMENT COMMENT '提醒配置唯一ID',
    customer_id INT NOT NULL COMMENT '客户ID（外键关联customers表）',
    product_id INT NOT NULL COMMENT '商品ID（外键关联products表）',
    alert_type ENUM('FAVORITE', 'CART') NOT NULL COMMENT '提醒来源：FAVORITE=收藏商品，CART=购物车商品',
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用提醒',
    threshold_percentage DECIMAL(5,2) DEFAULT 5.00 COMMENT '价格变动阈值百分比（默认5%）',
    last_alerted_price DECIMAL(10,2) COMMENT '上次提醒时的价格',
    last_alerted_at DATETIME COMMENT '上次提醒时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    
    PRIMARY KEY (alert_id),
    UNIQUE KEY uk_customer_product_type (customer_id, product_id, alert_type) COMMENT '同一客户对同一商品的同类型提醒唯一',
    KEY idx_customer_id (customer_id) COMMENT '按客户查询提醒配置的索引',
    KEY idx_product_id (product_id) COMMENT '按商品查询提醒配置的索引',
    KEY idx_is_enabled (is_enabled) COMMENT '启用状态索引',
    
    CONSTRAINT fk_alert_customer FOREIGN KEY (customer_id)
        REFERENCES customers (customer_id) ON DELETE CASCADE,
    CONSTRAINT fk_alert_product FOREIGN KEY (product_id)
        REFERENCES products (product_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '价格变动提醒配置表';

-- ============================================
-- 20. 价格变动通知记录表
-- ============================================
CREATE TABLE price_alert_notifications (
    notification_id INT NOT NULL AUTO_INCREMENT COMMENT '通知记录唯一ID',
    alert_id INT NOT NULL COMMENT '提醒配置ID（外键关联price_alert_settings表）',
    customer_id INT NOT NULL COMMENT '客户ID',
    product_id INT NOT NULL COMMENT '商品ID',
    old_price DECIMAL(10,2) NOT NULL COMMENT '变动前价格',
    new_price DECIMAL(10,2) NOT NULL COMMENT '变动后价格',
    change_percentage DECIMAL(5,2) NOT NULL COMMENT '价格变动百分比',
    change_direction ENUM('DECREASE', 'INCREASE') NOT NULL COMMENT '变动方向：DECREASE=降价，INCREASE=涨价',
    is_read BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否已读',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '通知创建时间',
    read_at DATETIME COMMENT '阅读时间',
    
    PRIMARY KEY (notification_id),
    KEY idx_alert_id (alert_id) COMMENT '按提醒配置查询的索引',
    KEY idx_customer_id (customer_id) COMMENT '按客户查询通知的索引',
    KEY idx_is_read (is_read) COMMENT '未读通知索引',
    KEY idx_created_at (created_at) COMMENT '按时间查询的索引',
    
    CONSTRAINT fk_notification_alert FOREIGN KEY (alert_id)
        REFERENCES price_alert_settings (alert_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '价格变动通知记录表';

-- ============================================
-- 21. 促销活动表
-- ============================================
CREATE TABLE promotions (
    promotion_id INT NOT NULL AUTO_INCREMENT COMMENT '促销活动唯一ID',
    promotion_name VARCHAR(100) NOT NULL COMMENT '促销活动名称',
    promotion_type ENUM('DISCOUNT', 'FULL_REDUCTION', 'COUPON', 'FLASH_SALE') NOT NULL COMMENT '促销类型：DISCOUNT=限时折扣，FULL_REDUCTION=满减，COUPON=优惠券，FLASH_SALE=秒杀',
    description TEXT COMMENT '活动描述',
    start_time DATETIME NOT NULL COMMENT '活动开始时间',
    end_time DATETIME NOT NULL COMMENT '活动结束时间',
    status ENUM('DRAFT', 'ACTIVE', 'ENDED', 'CANCELLED') NOT NULL DEFAULT 'DRAFT' COMMENT '活动状态：DRAFT=草稿，ACTIVE=进行中，ENDED=已结束，CANCELLED=已取消',
    discount_value DECIMAL(10,2) COMMENT '折扣值（折扣类型为折扣率，满减类型为减免金额）',
    min_purchase_amount DECIMAL(10,2) COMMENT '最低消费金额（满减活动使用）',
    max_discount_amount DECIMAL(10,2) COMMENT '最大优惠金额（限制优惠上限）',
    applicable_scope ENUM('ALL', 'CATEGORY', 'PRODUCT', 'USER_GROUP') NOT NULL DEFAULT 'ALL' COMMENT '适用范围：ALL=全部商品，CATEGORY=指定分类，PRODUCT=指定商品，USER_GROUP=指定用户群',
    target_ids JSON COMMENT '目标ID列表（分类ID数组/商品ID数组/用户组ID数组，根据applicable_scope决定）',
    priority INT NOT NULL DEFAULT 0 COMMENT '优先级（数字越大优先级越高，用于多个活动重叠时）',
    created_by INT NOT NULL COMMENT '创建人ID（运营人员ID）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    
    PRIMARY KEY (promotion_id),
    KEY idx_status (status) COMMENT '活动状态索引',
    KEY idx_start_time (start_time) COMMENT '开始时间索引',
    KEY idx_end_time (end_time) COMMENT '结束时间索引',
    KEY idx_promotion_type (promotion_type) COMMENT '促销类型索引',
    KEY idx_applicable_scope (applicable_scope) COMMENT '适用范围索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '促销活动表';

-- ============================================
-- 22. 促销规则详情表（支持更复杂的规则配置）
-- ============================================
CREATE TABLE promotion_rules (
    rule_id INT NOT NULL AUTO_INCREMENT COMMENT '规则唯一ID',
    promotion_id INT NOT NULL COMMENT '促销活动ID（外键关联promotions表）',
    rule_type ENUM('TIME_RANGE', 'USER_LEVEL', 'QUANTITY_LIMIT', 'COMBINATION') NOT NULL COMMENT '规则类型：TIME_RANGE=时间段限制，USER_LEVEL=用户等级限制，QUANTITY_LIMIT=数量限制，COMBINATION=组合购买',
    rule_config JSON NOT NULL COMMENT '规则配置JSON（根据不同rule_type存储不同结构）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    PRIMARY KEY (rule_id),
    KEY idx_promotion_id (promotion_id) COMMENT '按促销活动查询规则的索引',
    KEY idx_rule_type (rule_type) COMMENT '规则类型索引',
    
    CONSTRAINT fk_rule_promotion FOREIGN KEY (promotion_id)
        REFERENCES promotions (promotion_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '促销规则详情表';

-- ============================================
-- 23. 商品促销关联表
-- ============================================
CREATE TABLE product_promotions (
    relation_id INT NOT NULL AUTO_INCREMENT COMMENT '关联关系唯一ID',
    product_id INT NOT NULL COMMENT '商品ID',
    promotion_id INT NOT NULL COMMENT '促销活动ID',
    final_price DECIMAL(10,2) NOT NULL COMMENT '促销后的最终价格',
    discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '优惠金额',
    is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否生效',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    
    PRIMARY KEY (relation_id),
    UNIQUE KEY uk_product_promotion (product_id, promotion_id) COMMENT '商品与促销活动唯一关联',
    KEY idx_product_id (product_id) COMMENT '按商品查询促销的索引',
    KEY idx_promotion_id (promotion_id) COMMENT '按促销活动查询商品的索引',
    KEY idx_is_active (is_active) COMMENT '生效状态索引',
    
    CONSTRAINT fk_product_promo_product FOREIGN KEY (product_id)
        REFERENCES products (product_id) ON DELETE CASCADE,
    CONSTRAINT fk_product_promo_promotion FOREIGN KEY (promotion_id)
        REFERENCES promotions (promotion_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '商品促销关联表';

-- ============================================
-- 24. 在products表中添加当前促销价格字段
-- ============================================
ALTER TABLE products
    ADD COLUMN current_promotion_price DECIMAL(10,2) COMMENT '当前促销价格（如有活跃促销）',
    ADD COLUMN original_price DECIMAL(10,2) COMMENT '原价（用于展示折扣）',
    ADD COLUMN has_active_promotion BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否有活跃的促销活动';

-- ============================================
-- 初始化数据：为现有商品设置original_price等于当前price
-- ============================================
UPDATE products 
SET original_price = price,
    current_promotion_price = NULL,
    has_active_promotion = FALSE;

ALTER TABLE products
    ADD COLUMN active_promotion_ids JSON COMMENT '当前生效的促销活动ID数组（按优先级从高到低）';

UPDATE products
SET original_price = price,
    current_promotion_price = NULL,
    has_active_promotion = FALSE,
    active_promotion_ids = JSON_ARRAY();

-- 商品向量存储表
CREATE TABLE IF NOT EXISTS product_vectors (
                                               id INT AUTO_INCREMENT PRIMARY KEY,
                                               product_id INT NOT NULL,
                                               vector_type VARCHAR(20) NOT NULL DEFAULT 'product',
    image_url VARCHAR(1024) NULL,
    vector_text TEXT NULL,
    vector_data LONGTEXT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_product_id (product_id),
    INDEX idx_vector_type (vector_type)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
