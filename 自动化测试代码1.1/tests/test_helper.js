// tests/test_helper.js - 修复数据库名称
const mysql = require('mysql2/promise');
const bcrypt = require('bcrypt');
const axios = require('axios');

class TestHelper {
    constructor() {
        this.dbConfig = {
            host: process.env.DB_HOST || 'localhost',
            user: process.env.DB_USER || 'root',
            password: process.env.DB_PASSWORD || '12345',
            database: process.env.DB_NAME || 'onlineshop_test' // 确保这里是正确的数据库名
        };
        
        this.apiBaseUrl = process.env.API_BASE_URL || 'http://localhost:3000/api';
        this.authToken = null;
        this.connection = null;
    }

    async initDatabase() {
        try {
            this.connection = await mysql.createConnection(this.dbConfig);
            console.log('✅ 数据库连接成功');
            return this.connection;
        } catch (error) {
            console.error('❌ 数据库连接失败:', error.message);
            throw error;
        }
    }

   async cleanupTestData() {
    if (!this.connection) return;
    
    try {
        // 按依赖关系顺序清理，但保留基础数据
        await this.query('DELETE FROM media_resources WHERE media_id > 0');
        await this.query('DELETE FROM product_images WHERE image_id > 0');
        await this.query('DELETE FROM purchase_intents WHERE purchase_id > 5');
        await this.query('DELETE FROM products WHERE product_id > 5');
        await this.query('DELETE FROM customers WHERE customer_id > 2'); // 保留2个基础客户
        await this.query('DELETE FROM categories WHERE category_id > 6'); // 保留6个基础类别
        // seller 表保留基础数据 (seller_id = 1)
        
        console.log('✅ 测试数据清理完成');
    } catch (error) {
        console.error('❌ 数据清理失败:', error.message);
    }
}

    async close() {
        if (this.connection) {
            await this.connection.end();
            console.log('✅ 数据库连接已关闭');
        }
    }

    async query(sql, params = []) {
        if (!this.connection) {
            throw new Error('数据库未连接');
        }
        try {
            const [rows] = await this.connection.execute(sql, params);
            return rows;
        } catch (error) {
            console.error(`❌ SQL执行失败: ${sql}`, error.message);
            throw error;
        }
    }

    async verifyPassword(plainPassword, hashedPassword) {
        return await bcrypt.compare(plainPassword, hashedPassword);
    }

    
}

module.exports = TestHelper;