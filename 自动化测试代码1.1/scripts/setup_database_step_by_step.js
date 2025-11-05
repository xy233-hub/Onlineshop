// scripts/setup_database_with_constraints.js
const mysql = require('mysql2');

class DatabaseSetup {
    constructor() {
        this.connection = null;
        this.config = {
            host: process.env.DB_HOST || 'localhost',
            user: process.env.DB_USER || 'root',
            password: process.env.DB_PASSWORD || '12345',
            database: process.env.DB_NAME || 'onlineshop_test',
            multipleStatements: true
        };
    }

    async connectToMySQL() {
        try {
            const connectionConfig = { ...this.config };
            delete connectionConfig.database;
            this.connection = mysql.createConnection(connectionConfig);
            await this.connection.promise().connect();
            console.log('✅ 连接到 MySQL 服务器成功');
            return true;
        } catch (error) {
            console.error('❌ 连接到 MySQL 服务器失败:', error.message);
            return false;
        }
    }

    async createDatabase() {
        try {
            await this.connection.promise().query(`CREATE DATABASE IF NOT EXISTS \`${this.config.database}\``);
            console.log('✅ 数据库创建成功');
            return true;
        } catch (error) {
            console.error('❌ 数据库创建失败:', error.message);
            return false;
        }
    }

    async useDatabase() {
        try {
            await this.connection.promise().query(`USE \`${this.config.database}\``);
            console.log('✅ 切换到数据库成功');
            return true;
        } catch (error) {
            console.error('❌ 切换数据库失败:', error.message);
            return false;
        }
    }

    async dropAllTables() {
        try {
            const [tables] = await this.connection.promise().query(`
                SELECT TABLE_NAME 
                FROM information_schema.TABLES 
                WHERE TABLE_SCHEMA = ?
            `, [this.config.database]);
            
            if (tables.length > 0) {
                await this.connection.promise().query('SET FOREIGN_KEY_CHECKS = 0');
                for (const table of tables) {
                    await this.connection.promise().query(`DROP TABLE IF EXISTS \`${table.TABLE_NAME}\``);
                }
                await this.connection.promise().query('SET FOREIGN_KEY_CHECKS = 1');
                console.log(`✅ 已删除 ${tables.length} 个旧表`);
            }
            return true;
        } catch (error) {
            console.error('❌ 删除旧表失败:', error.message);
            return false;
        }
    }

    async createTablesWithConstraints() {
        try {
            const createTablesSQL = `
-- 1. 卖家表（seller）
CREATE TABLE IF NOT EXISTS \`seller\` (
  \`seller_id\` INT NOT NULL AUTO_INCREMENT COMMENT '卖家唯一ID',
  \`username\` VARCHAR(50) NOT NULL COMMENT '登录用户名',
  \`password\` VARCHAR(100) NOT NULL COMMENT '加密后的密码',
  \`create_time\` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '账号创建时间',
  PRIMARY KEY (\`seller_id\`),
  UNIQUE KEY \`uk_username\` (\`username\`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='卖家账号表';

-- 2. 客户表（customers）- 添加长度约束
CREATE TABLE IF NOT EXISTS \`customers\` (
  \`customer_id\` INT NOT NULL AUTO_INCREMENT COMMENT '客户唯一ID',
  \`username\` VARCHAR(20) NOT NULL COMMENT '登录用户名（6-20位）',
  \`password\` VARCHAR(100) NOT NULL COMMENT '加密密码',
  \`phone\` VARCHAR(11) NOT NULL COMMENT '联系电话',
  \`default_address\` VARCHAR(255) COMMENT '默认交易地点',
  \`created_at\` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  \`updated_at\` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (\`customer_id\`),
  UNIQUE KEY \`uk_customer_username\` (\`username\`),
  UNIQUE KEY \`uk_customer_phone\` (\`phone\`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户信息表';

-- 3. 商品类别表（categories）
CREATE TABLE IF NOT EXISTS \`categories\` (
  \`category_id\` INT NOT NULL AUTO_INCREMENT COMMENT '类别唯一ID',
  \`parent_id\` INT DEFAULT NULL COMMENT '父类别ID',
  \`category_name\` VARCHAR(100) NOT NULL COMMENT '类别名称',
  \`category_level\` TINYINT NOT NULL COMMENT '类别级别',
  \`created_at\` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (\`category_id\`),
  KEY \`idx_parent_id\` (\`parent_id\`),
  KEY \`idx_category_level\` (\`category_level\`),
  CONSTRAINT \`fk_category_parent\` FOREIGN KEY (\`parent_id\`) REFERENCES \`categories\` (\`category_id\`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品类别表';

-- 4. 商品表（products）
CREATE TABLE IF NOT EXISTS \`products\` (
  \`product_id\` INT NOT NULL AUTO_INCREMENT COMMENT '商品唯一ID',
  \`seller_id\` INT NOT NULL COMMENT '所属卖家ID',
  \`category_id\` INT NOT NULL COMMENT '商品二级类别ID',
  \`product_name\` VARCHAR(100) NOT NULL COMMENT '商品名称',
  \`product_desc\` TEXT NOT NULL COMMENT '商品详细描述',
  \`price\` DECIMAL(10,2) NOT NULL COMMENT '商品价格',
  \`stock_quantity\` INT NOT NULL DEFAULT 0 COMMENT '库存数量',
  \`product_status\` ENUM('online', 'outOfStock', 'frozen', 'sold') NOT NULL DEFAULT 'outOfStock' COMMENT '商品状态',
  \`search_keywords\` VARCHAR(255) COMMENT '搜索关键词',
  \`created_at\` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上架时间',
  \`updated_at\` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (\`product_id\`),
  KEY \`idx_seller_id\` (\`seller_id\`),
  KEY \`idx_category_id\` (\`category_id\`),
  KEY \`idx_product_status\` (\`product_status\`),
  KEY \`idx_stock_quantity\` (\`stock_quantity\`),
  FULLTEXT KEY \`ft_search\` (\`product_name\`, \`search_keywords\`),
  CONSTRAINT \`fk_product_seller\` FOREIGN KEY (\`seller_id\`) REFERENCES \`seller\` (\`seller_id\`) ON DELETE CASCADE,
  CONSTRAINT \`fk_product_category\` FOREIGN KEY (\`category_id\`) REFERENCES \`categories\` (\`category_id\`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品信息表';

-- 5. 商品图片表（product_images）
CREATE TABLE IF NOT EXISTS \`product_images\` (
  \`image_id\` INT NOT NULL AUTO_INCREMENT COMMENT '图片唯一ID',
  \`product_id\` INT NOT NULL COMMENT '关联的商品ID',
  \`image_url\` VARCHAR(255) NOT NULL COMMENT '图片路径/链接',
  \`image_order\` TINYINT NOT NULL DEFAULT 0 COMMENT '图片排序',
  \`created_at\` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  PRIMARY KEY (\`image_id\`),
  KEY \`idx_product_id\` (\`product_id\`),
  CONSTRAINT \`fk_image_product\` FOREIGN KEY (\`product_id\`) REFERENCES \`products\` (\`product_id\`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品图片表';

-- 6. 媒体资源表（media_resources）
CREATE TABLE IF NOT EXISTS \`media_resources\` (
  \`media_id\` INT NOT NULL AUTO_INCREMENT COMMENT '媒体资源唯一ID',
  \`product_id\` INT NOT NULL COMMENT '关联的商品ID',
  \`media_type\` ENUM('image', 'video', 'audio') NOT NULL DEFAULT 'image' COMMENT '媒体类型',
  \`media_url\` VARCHAR(255) NOT NULL COMMENT '媒体文件路径/链接',
  \`file_name\` VARCHAR(100) NOT NULL COMMENT '原始文件名',
  \`file_size\` INT NOT NULL COMMENT '文件大小',
  \`mime_type\` VARCHAR(50) NOT NULL COMMENT '文件MIME类型',
  \`display_order\` TINYINT NOT NULL DEFAULT 0 COMMENT '显示顺序',
  \`is_embedded\` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否嵌入在商品描述中',
  \`created_at\` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  PRIMARY KEY (\`media_id\`),
  KEY \`idx_product_id\` (\`product_id\`),
  KEY \`idx_media_type\` (\`media_type\`),
  KEY \`idx_display_order\` (\`display_order\`),
  KEY \`idx_embedded\` (\`is_embedded\`),
  CONSTRAINT \`fk_media_product\` FOREIGN KEY (\`product_id\`) REFERENCES \`products\` (\`product_id\`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='媒体资源表';

-- 7. 购买意向表（purchase_intents）
CREATE TABLE IF NOT EXISTS \`purchase_intents\` (
  \`purchase_id\` INT NOT NULL AUTO_INCREMENT COMMENT '购买意向ID',
  \`product_id\` INT NOT NULL COMMENT '关联的商品ID',
  \`customer_id\` INT NOT NULL COMMENT '客户ID',
  \`customer_name\` VARCHAR(50) NOT NULL COMMENT '顾客姓名',
  \`customer_phone\` VARCHAR(20) NOT NULL COMMENT '顾客联系电话',
  \`customer_address\` VARCHAR(255) NOT NULL COMMENT '收货地址',
  \`quantity\` INT NOT NULL DEFAULT 1 COMMENT '购买数量',
  \`total_amount\` DECIMAL(10,2) NOT NULL COMMENT '订单总金额',
  \`purchase_status\` ENUM('pending', 'success', 'failed') NOT NULL DEFAULT 'pending' COMMENT '交易状态',
  \`seller_notes\` VARCHAR(500) COMMENT '商家处理备注',
  \`created_at\` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '意向提交时间',
  \`updated_at\` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (\`purchase_id\`),
  KEY \`idx_product_id\` (\`product_id\`),
  KEY \`idx_customer_id\` (\`customer_id\`),
  KEY \`idx_purchase_status\` (\`purchase_status\`),
  CONSTRAINT \`fk_intent_product\` FOREIGN KEY (\`product_id\`) REFERENCES \`products\` (\`product_id\`) ON DELETE RESTRICT,
  CONSTRAINT \`fk_intent_customer\` FOREIGN KEY (\`customer_id\`) REFERENCES \`customers\` (\`customer_id\`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购买意向记录表';
            `;

            await this.connection.promise().query(createTablesSQL);
            console.log('✅ 数据表创建成功');

            // 创建触发器
            await this.createTriggers();
            return true;
        } catch (error) {
            console.error('❌ 数据表创建失败:', error.message);
            return false;
        }
    }

    async createTriggers() {
        try {
            // 分别创建每个触发器，不使用 DELIMITER
            const triggers = [
                // 用户名长度验证触发器
                `CREATE TRIGGER IF NOT EXISTS validate_customer_username_length
                 BEFORE INSERT ON customers
                 FOR EACH ROW
                 BEGIN
                     IF CHAR_LENGTH(NEW.username) < 6 OR CHAR_LENGTH(NEW.username) > 20 THEN
                         SIGNAL SQLSTATE '45000' 
                         SET MESSAGE_TEXT = '用户名长度必须在6-20位之间';
                     END IF;
                 END`,

                // 库存自动下架触发器
                `CREATE TRIGGER IF NOT EXISTS update_product_status_on_stock_change 
                 BEFORE UPDATE ON products
                 FOR EACH ROW
                 BEGIN
                     -- 当库存≤0且商品状态为online时，自动改为outOfStock
                     IF NEW.stock_quantity <= 0 AND OLD.product_status = 'online' THEN
                         SET NEW.product_status = 'outOfStock';
                     END IF;
                 END`
            ];

            // 先删除可能存在的旧触发器
            await this.connection.promise().query('DROP TRIGGER IF EXISTS validate_customer_username_length');
            await this.connection.promise().query('DROP TRIGGER IF EXISTS update_product_status_on_stock_change');

            // 创建新触发器
            for (const triggerSQL of triggers) {
                await this.connection.promise().query(triggerSQL);
            }

            console.log('✅ 数据库触发器创建成功');
        } catch (error) {
            console.error('❌ 触发器创建失败:', error.message);
            // 如果触发器创建失败，继续执行，不影响主要功能
        }
    }

    async insertSampleData() {
        try {
            const baseDataSQL = `
-- 插入卖家数据
INSERT IGNORE INTO \`seller\` (\`seller_id\`, \`username\`, \`password\`) VALUES 
(1, 'admin', '$2a$12$ycJEdpJ1TYxBzDCmuGju2eUbTP.vm0yNYYyFpK3ahxsCLq9d2Iq32');

-- 插入类别数据
INSERT IGNORE INTO \`categories\` (\`category_id\`, \`parent_id\`, \`category_name\`, \`category_level\`) VALUES
(1, NULL, '电子产品', 1),
(2, NULL, '服装服饰', 1),
(3, 1, '手机', 2),
(4, 1, '笔记本电脑', 2),
(5, 2, '男装', 2),
(6, 2, '女装', 2);

-- 插入客户数据（确保用户名长度合规）
INSERT IGNORE INTO \`customers\` (\`customer_id\`, \`username\`, \`password\`, \`phone\`, \`default_address\`) VALUES
(1, 'customer01', '$2a$12$customerhashedpassword1', '13800138001', '北京市朝阳区'),
(2, 'customer02', '$2a$12$customerhashedpassword2', '13800138002', '上海市浦东新区');

-- 插入商品数据
INSERT IGNORE INTO \`products\` (\`product_id\`, \`seller_id\`, \`category_id\`, \`product_name\`, \`product_desc\`, \`price\`, \`stock_quantity\`, \`product_status\`) VALUES
(1, 1, 3, 'iPhone 15', '最新款苹果手机', 5999.00, 10, 'online'),
(2, 1, 3, '小米14', '高性能安卓手机', 3999.00, 0, 'outOfStock'),
(3, 1, 4, 'MacBook Pro', '专业笔记本电脑', 12999.00, 5, 'online'),
(4, 1, 5, '男士衬衫', '纯棉商务衬衫', 199.00, 50, 'online'),
(5, 1, 6, '女士连衣裙', '夏季时尚连衣裙', 299.00, 0, 'sold');

-- 插入购买意向数据
INSERT IGNORE INTO \`purchase_intents\` (\`purchase_id\`, \`product_id\`, \`customer_id\`, \`customer_name\`, \`customer_phone\`, \`customer_address\`, \`quantity\`, \`total_amount\`, \`purchase_status\`) VALUES
(1, 1, 1, '张三', '13800138001', '北京市朝阳区', 1, 5999.00, 'pending'),
(2, 3, 2, '李四', '13800138002', '上海市浦东新区', 1, 12999.00, 'success'),
(3, 4, 1, '张三', '13800138001', '北京市朝阳区', 2, 398.00, 'pending'),
(4, 1, 2, '李四', '13800138002', '上海市浦东新区', 1, 5999.00, 'failed'),
(5, 3, 1, '张三', '13800138001', '北京市朝阳区', 1, 12999.00, 'pending');
            `;

            await this.connection.promise().query(baseDataSQL);
            console.log('✅ 示例数据插入成功');
            return true;
        } catch (error) {
            console.error('❌ 示例数据插入失败:', error.message);
            return false;
        }
    }

    async close() {
        if (this.connection) {
            await this.connection.promise().end();
            console.log('✅ 数据库连接已关闭');
        }
    }

    async setup() {
        console.log('🚀 开始初始化数据库（包含约束）...');
        
        try {
            if (!await this.connectToMySQL()) return false;
            if (!await this.createDatabase()) return false;
            if (!await this.useDatabase()) return false;
            await this.dropAllTables();
            if (!await this.createTablesWithConstraints()) return false;
            if (!await this.insertSampleData()) return false;

            console.log('🎉 数据库初始化完成！');
            return true;
        } catch (error) {
            console.error('❌ 数据库初始化过程出错:', error.message);
            return false;
        } finally {
            await this.close();
        }
    }
}

// 执行初始化
const dbSetup = new DatabaseSetup();
dbSetup.setup().then(success => {
    if (success) {
        console.log('✨ 数据库设置流程完成');
        process.exit(0);
    } else {
        console.error('💥 数据库设置流程失败');
        process.exit(1);
    }
}).catch(error => {
    console.error('💥 未预期的错误:', error);
    process.exit(1);
});