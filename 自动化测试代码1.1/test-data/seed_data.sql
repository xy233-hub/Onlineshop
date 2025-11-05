-- seed_data.sql - 更新版
USE `onlineshop`;

-- 清空现有数据（按依赖关系顺序）
DELETE FROM media_resources;
DELETE FROM product_images;
DELETE FROM purchase_intents;
DELETE FROM products;
DELETE FROM customers;
DELETE FROM categories;
DELETE FROM seller;

-- 插入测试商家 (密码: Admin123456)
INSERT INTO `seller` (`seller_id`, `username`, `password`) 
VALUES (
  1, 
  'admin', 
  '$2a$12$ycJEdpJ1TYxBzDCmuGju2eUbTP.vm0yNYYyFpK3ahxsCLq9d2Iq32'
);

-- 插入商品类别数据（支持2级类别）
INSERT INTO `categories` (`category_id`, `parent_id`, `category_name`, `category_level`) VALUES
(1, NULL, '电子产品', 1),
(2, NULL, '服装服饰', 1),
(3, 1, '手机', 2),
(4, 1, '笔记本电脑', 2),
(5, 1, '平板电脑', 2),
(6, 2, '男装', 2),
(7, 2, '女装', 2),
(8, 2, '配饰', 2);

-- 插入测试客户 (密码都是: Customer123)
INSERT INTO `customers` (`customer_id`, `username`, `password`, `phone`, `default_address`) VALUES
(1, 'customer1', '$2a$12$customerhashedpassword1', '13800138001', '北京市朝阳区建国路100号'),
(2, 'customer2', '$2a$12$customerhashedpassword2', '13800138002', '上海市浦东新区陆家嘴'),
(3, 'customer3', '$2a$12$customerhashedpassword3', '13800138003', '广州市天河区珠江新城'),
(4, 'customer4', '$2a$12$customerhashedpassword4', '13800138004', '深圳市南山区科技园'),
(5, 'customer5', '$2a$12$customerhashedpassword5', '13800138005', '杭州市西湖区文三路');

-- 插入测试商品
INSERT INTO `products` (
  `product_id`, `seller_id`, `category_id`, `product_name`, `product_desc`, 
  `price`, `stock_quantity`, `product_status`, `search_keywords`
) VALUES 
(1, 1, 3, 'iPhone 15 Pro', '最新款苹果手机，A17 Pro芯片，钛金属设计', 7999.00, 50, 'online', '苹果,iphone,手机,智能手机'),
(2, 1, 3, '小米14 Ultra', '徕卡影像旗舰，骁龙8 Gen3处理器', 5999.00, 30, 'online', '小米,手机,安卓,徕卡'),
(3, 1, 4, 'MacBook Pro 16寸', 'M3 Max芯片，专业级笔记本电脑', 18999.00, 10, 'online', '苹果,macbook,笔记本,电脑'),
(4, 1, 4, 'ThinkPad X1 Carbon', '商务轻薄本，英特尔酷睿i7', 12999.00, 0, 'outOfStock', 'thinkpad,商务,笔记本'),
(5, 1, 5, 'iPad Pro 12.9', 'M2芯片，Liquid视网膜XDR显示屏', 9299.00, 15, 'online', '苹果,ipad,平板'),
(6, 1, 6, '男士商务衬衫', '100%棉质，经典商务款式', 299.00, 100, 'online', '男装,衬衫,商务'),
(7, 1, 7, '女士连衣裙', '夏季新款，雪纺材质', 399.00, 0, 'sold', '女装,连衣裙,夏季'),
(8, 1, 8, '真皮钱包', '头层牛皮，多卡位设计', 199.00, 25, 'frozen', '配饰,钱包,真皮');

-- 插入商品图片数据
INSERT INTO `product_images` (`product_id`, `image_url`, `image_order`) VALUES
(1, 'https://picsum.photos/400/300?product=1-1', 0),
(1, 'https://picsum.photos/400/300?product=1-2', 1),
(1, 'https://picsum.photos/400/300?product=1-3', 2),
(2, 'https://picsum.photos/400/300?product=2-1', 0),
(2, 'https://picsum.photos/400/300?product=2-2', 1),
(3, 'https://picsum.photos/400/300?product=3-1', 0),
(6, 'https://picsum.photos/400/300?product=6-1', 0),
(7, 'https://picsum.photos/400/300?product=7-1', 0),
(8, 'https://picsum.photos/400/300?product=8-1', 0);

-- 插入媒体资源数据（商品描述中的富媒体）
INSERT INTO `media_resources` (`product_id`, `media_type`, `media_url`, `file_name`, `file_size`, `mime_type`, `display_order`, `is_embedded`) VALUES
(1, 'image', 'https://picsum.photos/800/600?media=1-1', 'product_detail_1.jpg', 2048000, 'image/jpeg', 1, TRUE),
(1, 'video', 'https://example.com/videos/iphone15.mp4', 'product_video_1.mp4', 15728640, 'video/mp4', 2, TRUE),
(3, 'image', 'https://picsum.photos/800/600?media=3-1', 'macbook_detail_1.jpg', 2457600, 'image/jpeg', 1, TRUE),
(6, 'image', 'https://picsum.photos/800/600?media=6-1', 'shirt_detail_1.jpg', 1835008, 'image/jpeg', 1, TRUE);

-- 插入购买意向数据
INSERT INTO `purchase_intents` (
  `purchase_id`, `product_id`, `customer_id`, `customer_name`, `customer_phone`, 
  `customer_address`, `quantity`, `total_amount`, `purchase_status`, `seller_notes`
) VALUES
(1, 1, 1, '张三', '13800138001', '北京市朝阳区建国路100号', 1, 7999.00, 'pending', NULL),
(2, 1, 2, '李四', '13800138002', '上海市浦东新区世纪大道200号', 2, 15998.00, 'success', '客户已付款，安排发货'),
(3, 3, 3, '王五', '13800138003', '广州市天河区珠江新城', 1, 18999.00, 'pending', NULL),
(4, 2, 4, '赵六', '13800138004', '深圳市南山区科技园', 1, 5999.00, 'failed', '库存不足，联系客户取消'),
(5, 6, 5, '钱七', '13800138005', '杭州市西湖区文三路', 3, 897.00, 'pending', NULL),
(6, 5, 1, '张三', '13800138001', '北京市海淀区中关村', 1, 9299.00, 'success', '教育优惠订单'),
(7, 8, 2, '李四', '13800138002', '上海市徐汇区漕河泾', 2, 398.00, 'pending', NULL),
(8, 7, 3, '王五', '13800138003', '广州市越秀区北京路', 1, 399.00, 'failed', '商品已售罄');

-- 更新序列（如果需要的话）
-- ALTER TABLE seller AUTO_INCREMENT = 2;
-- ALTER TABLE customers AUTO_INCREMENT = 6;
-- ALTER TABLE categories AUTO_INCREMENT = 9;
-- ALTER TABLE products AUTO_INCREMENT = 9;
-- ALTER TABLE product_images AUTO_INCREMENT = 10;
-- ALTER TABLE media_resources AUTO_INCREMENT = 5;
-- ALTER TABLE purchase_intents AUTO_INCREMENT = 9;

-- 验证数据插入
SELECT 
  (SELECT COUNT(*) FROM seller) as seller_count,
  (SELECT COUNT(*) FROM customers) as customer_count,
  (SELECT COUNT(*) FROM categories) as category_count,
  (SELECT COUNT(*) FROM products) as product_count,
  (SELECT COUNT(*) FROM product_images) as image_count,
  (SELECT COUNT(*) FROM media_resources) as media_count,
  (SELECT COUNT(*) FROM purchase_intents) as intent_count;