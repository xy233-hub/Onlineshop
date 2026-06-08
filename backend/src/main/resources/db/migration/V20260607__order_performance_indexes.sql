-- 下单功能性能优化索引
-- 执行前请确保表存在

-- 1. purchase_intents 表索引
-- 用于按客户ID查询订单（高频查询）
CREATE INDEX IF NOT EXISTS idx_purchase_customer_time 
ON purchase_intents(customer_id, created_at DESC);

-- 用于按订单状态查询
CREATE INDEX IF NOT EXISTS idx_purchase_status 
ON purchase_intents(purchase_status);

-- 用于按卖家ID关联查询（通过 purchase_intent_items）
-- 注意：此索引在 purchase_intent_items 表上

-- 2. purchase_intent_items 表索引
-- 用于按订单ID查询商品项（每次下单都会查询）
CREATE INDEX IF NOT EXISTS idx_item_purchase_id 
ON purchase_intent_items(purchase_id);

-- 用于按商品ID查询（统计商品销量等）
CREATE INDEX IF NOT EXISTS idx_item_product_id 
ON purchase_intent_items(product_id);

-- 3. products 表索引
-- 用于按卖家ID查询商品
CREATE INDEX IF NOT EXISTS idx_product_seller 
ON products(seller_id);

-- 用于按状态查询商品
CREATE INDEX IF NOT EXISTS idx_product_status 
ON products(product_status);

-- 用于按分类查询商品
CREATE INDEX IF NOT EXISTS idx_product_category 
ON products(category_id);

-- 4. shopping_cart_items 表索引
-- 用于按客户ID查询购物车（高频查询）
CREATE INDEX IF NOT EXISTS idx_cart_customer 
ON shopping_cart_items(customer_id);

-- 用于按客户和商品查询（添加购物车时检查是否存在）
CREATE INDEX IF NOT EXISTS idx_cart_customer_product 
ON shopping_cart_items(customer_id, product_id);

-- 5. product_promotions 表索引（如果存在）
-- 用于按商品ID查询促销信息
CREATE INDEX IF NOT EXISTS idx_promotion_product 
ON product_promotions(product_id);

-- 用于按促销活动ID查询
CREATE INDEX IF NOT EXISTS idx_promotion_active 
ON product_promotions(promotion_id, is_active);