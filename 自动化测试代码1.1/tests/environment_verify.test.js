// tests/environment_verify.test.js - 修复版
const TestHelper = require('./test_helper');
const { test, expect, beforeAll, afterAll } = require('@jest/globals');

describe('环境验证测试', () => {
    let helper;

    beforeAll(async () => {
        helper = new TestHelper();
        await helper.initDatabase();
        // 不再调用 initTestData，因为数据已经在数据库中
    });

    test('TC-ENV-001: 数据库连接测试', async () => {
        const result = await helper.query('SELECT 1 as connection_test');
        expect(result[0].connection_test).toBe(1);
        console.log('✅ TC-ENV-001: 数据库连接测试通过');
    });

    test('TC-ENV-002: 数据库表结构验证', async () => {
        const tables = await helper.query('SHOW TABLES');
        const tableNames = tables.map(row => Object.values(row)[0]);
        
        // 验证所有新表
        const expectedTables = [
            'seller', 'customers', 'categories', 'products', 
            'product_images', 'media_resources', 'purchase_intents'
        ];
        
        expectedTables.forEach(table => {
            expect(tableNames).toContain(table);
        });
        
        console.log('✅ TC-ENV-002: 数据库表结构验证通过');
    });

    test('TC-ENV-003: 测试数据验证', async () => {
        const sellers = await helper.query('SELECT * FROM seller');
        const customers = await helper.query('SELECT * FROM customers');
        const categories = await helper.query('SELECT * FROM categories');
        const products = await helper.query('SELECT * FROM products');
        const intents = await helper.query('SELECT * FROM purchase_intents');
        
        expect(sellers.length).toBeGreaterThan(0);
        expect(customers.length).toBeGreaterThan(0);
        expect(categories.length).toBeGreaterThan(0);
        expect(products.length).toBeGreaterThan(0);
        expect(intents.length).toBeGreaterThan(0);
        
        console.log(`✅ TC-ENV-003: 测试数据验证通过 - 卖家:${sellers.length}, 客户:${customers.length}, 类别:${categories.length}, 商品:${products.length}, 意向:${intents.length}`);
    });

    test('TC-ENV-004: 密码加密验证', async () => {
        const sellers = await helper.query('SELECT * FROM seller WHERE username = ?', ['admin']);
        const seller = sellers[0];
        
        // 验证密码是BCrypt加密格式
        expect(seller.password).toMatch(/^\$2[ayb]\$.{56}$/);
        
        // 验证密码正确性
        const isPasswordValid = await helper.verifyPassword('Admin123456', seller.password);
        expect(isPasswordValid).toBe(true);
        
        console.log('✅ TC-ENV-004: 密码加密验证通过');
    });

    test('TC-ENV-005: 外键约束验证', async () => {
        // 验证商品表的外键约束
        const products = await helper.query(`
            SELECT p.*, s.username, c.category_name 
            FROM products p 
            JOIN seller s ON p.seller_id = s.seller_id 
            JOIN categories c ON p.category_id = c.category_id
            LIMIT 1
        `);
        
        expect(products.length).toBe(1);
        expect(products[0].username).toBeDefined();
        expect(products[0].category_name).toBeDefined();
        
        console.log('✅ TC-ENV-005: 外键约束验证通过');
    });

    test('TC-ENV-006: 数据完整性验证', async () => {
        // 验证卖家数据完整性
        const sellers = await helper.query('SELECT * FROM seller WHERE username = ?', ['admin']);
        expect(sellers[0].username).toBe('admin');
        expect(sellers[0].password).toBeDefined();
        
        // 验证类别层级关系
        const topCategories = await helper.query('SELECT * FROM categories WHERE parent_id IS NULL');
        const subCategories = await helper.query('SELECT * FROM categories WHERE parent_id IS NOT NULL');
        
        expect(topCategories.length).toBeGreaterThan(0);
        expect(subCategories.length).toBeGreaterThan(0);
        
        console.log('✅ TC-ENV-006: 数据完整性验证通过');
    });

    afterAll(async () => {
        await helper.close();
    });
});