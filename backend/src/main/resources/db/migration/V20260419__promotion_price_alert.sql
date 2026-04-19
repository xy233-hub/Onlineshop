-- Pricing, alert, and promotion baseline schema for APIs 59-77

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS current_promotion_price DECIMAL(10,2) NULL COMMENT '当前促销价格（如有活跃促销）',
    ADD COLUMN IF NOT EXISTS original_price DECIMAL(10,2) NULL COMMENT '原价（用于展示折扣）',
    ADD COLUMN IF NOT EXISTS has_active_promotion BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否有活跃促销活动';

UPDATE products
SET original_price = COALESCE(original_price, price),
    current_promotion_price = NULL,
    has_active_promotion = COALESCE(has_active_promotion, FALSE);

CREATE TABLE IF NOT EXISTS product_price_history (
    history_id INT NOT NULL AUTO_INCREMENT,
    product_id INT NOT NULL,
    old_price DECIMAL(10,2) NOT NULL,
    new_price DECIMAL(10,2) NOT NULL,
    change_type ENUM('MANUAL', 'PROMOTION_START', 'PROMOTION_END', 'SYSTEM') NOT NULL DEFAULT 'MANUAL',
    change_reason VARCHAR(255) NULL,
    changed_by INT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (history_id),
    KEY idx_price_history_product (product_id),
    KEY idx_price_history_created_at (created_at),
    CONSTRAINT fk_price_history_product FOREIGN KEY (product_id) REFERENCES products (product_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS price_alert_settings (
    alert_id INT NOT NULL AUTO_INCREMENT,
    customer_id INT NOT NULL,
    product_id INT NOT NULL,
    alert_type ENUM('FAVORITE', 'CART') NOT NULL,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    threshold_percentage DECIMAL(5,2) NOT NULL DEFAULT 5.00,
    last_alerted_price DECIMAL(10,2) NULL,
    last_alerted_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (alert_id),
    UNIQUE KEY uk_alert_unique (customer_id, product_id, alert_type),
    KEY idx_alert_customer (customer_id),
    KEY idx_alert_product (product_id),
    KEY idx_alert_enabled (is_enabled),
    CONSTRAINT fk_alert_customer FOREIGN KEY (customer_id) REFERENCES customers (customer_id) ON DELETE CASCADE,
    CONSTRAINT fk_alert_product FOREIGN KEY (product_id) REFERENCES products (product_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS price_alert_notifications (
    notification_id INT NOT NULL AUTO_INCREMENT,
    alert_id INT NOT NULL,
    customer_id INT NOT NULL,
    product_id INT NOT NULL,
    old_price DECIMAL(10,2) NOT NULL,
    new_price DECIMAL(10,2) NOT NULL,
    change_percentage DECIMAL(7,2) NOT NULL,
    change_direction ENUM('DECREASE', 'INCREASE') NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at DATETIME NULL,
    PRIMARY KEY (notification_id),
    KEY idx_notify_customer_read (customer_id, is_read),
    KEY idx_notify_product (product_id),
    CONSTRAINT fk_notify_alert FOREIGN KEY (alert_id) REFERENCES price_alert_settings (alert_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS promotions (
    promotion_id INT NOT NULL AUTO_INCREMENT,
    promotion_name VARCHAR(100) NOT NULL,
    promotion_type ENUM('DISCOUNT', 'FULL_REDUCTION', 'COUPON', 'FLASH_SALE') NOT NULL,
    description TEXT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    status ENUM('DRAFT', 'ACTIVE', 'ENDED', 'CANCELLED') NOT NULL DEFAULT 'DRAFT',
    discount_value DECIMAL(10,2) NOT NULL,
    min_purchase_amount DECIMAL(10,2) NULL,
    max_discount_amount DECIMAL(10,2) NULL,
    applicable_scope ENUM('ALL', 'CATEGORY', 'PRODUCT', 'USER_GROUP') NOT NULL DEFAULT 'ALL',
    target_ids JSON NULL,
    priority INT NOT NULL DEFAULT 0,
    created_by INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (promotion_id),
    KEY idx_promotion_status_time (status, start_time, end_time, priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS promotion_rules (
    rule_id INT NOT NULL AUTO_INCREMENT,
    promotion_id INT NOT NULL,
    rule_type ENUM('TIME_RANGE', 'USER_LEVEL', 'QUANTITY_LIMIT', 'COMBINATION') NOT NULL,
    rule_config JSON NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (rule_id),
    KEY idx_promotion_rule_pid (promotion_id),
    CONSTRAINT fk_rule_promotion FOREIGN KEY (promotion_id) REFERENCES promotions (promotion_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS product_promotions (
    relation_id INT NOT NULL AUTO_INCREMENT,
    product_id INT NOT NULL,
    promotion_id INT NOT NULL,
    final_price DECIMAL(10,2) NOT NULL,
    discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (relation_id),
    UNIQUE KEY uk_product_promotion (product_id, promotion_id),
    KEY idx_product_promotion_active (product_id, is_active),
    CONSTRAINT fk_product_promo_product FOREIGN KEY (product_id) REFERENCES products (product_id) ON DELETE CASCADE,
    CONSTRAINT fk_product_promo_promotion FOREIGN KEY (promotion_id) REFERENCES promotions (promotion_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

