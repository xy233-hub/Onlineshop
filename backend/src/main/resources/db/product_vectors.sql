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
