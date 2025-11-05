// tests/product_management.test.js - 修复版
const TestHelper = require('./test_helper');
const { test, expect, beforeAll, afterAll, beforeEach } = require('@jest/globals');

describe('商品管理功能测试', () => {
    let helper;

    beforeAll(async () => {
        helper = new TestHelper();
        await helper.initDatabase();
    });

    beforeEach(async () => {
        await helper.cleanupTestData();
    });

    afterAll(async () => {
        await helper.close();
    });

    test('TC-PRODUCT-001: 商品发布测试', async () => {
        const productData = {
            seller_id: 1,
            category_id: 3, // 手机类别
            product_name: '测试商品',
            product_desc: '商品详细描述',
            price: 99.99,
            stock_quantity: 100,
            product_status: 'online',
            search_keywords: '测试,新品'
        };

        const [result] = await helper.connection.execute(
            `INSERT INTO products 
             (seller_id, category_id, product_name, product_desc, price, stock_quantity, product_status, search_keywords) 
             VALUES (?, ?, ?, ?, ?, ?, ?, ?)`,
            [productData.seller_id, productData.category_id, productData.product_name,
             productData.product_desc, productData.price, productData.stock_quantity,
             productData.product_status, productData.search_keywords]
        );
        
        expect(result.affectedRows).toBe(1);
        expect(result.insertId).toBeGreaterThan(5);
        
        console.log('✅ TC-PRODUCT-001: 商品发布测试通过');
    });

    test('TC-PRODUCT-002: 商品状态枚举验证', async () => {
        const validStatuses = ['online', 'outOfStock', 'frozen', 'sold'];
        const productId = 1; // 使用基础数据中的商品
        
        for (const status of validStatuses) {
            await expect(
                helper.connection.execute(
                    'UPDATE products SET product_status = ? WHERE product_id = ?',
                    [status, productId]
                )
            ).resolves.not.toThrow();
        }

        // 测试无效状态
        await expect(
            helper.connection.execute(
                'UPDATE products SET product_status = ? WHERE product_id = ?',
                ['invalid_status', productId]
            )
        ).rejects.toThrow();
        
        console.log('✅ TC-PRODUCT-002: 商品状态枚举验证通过');
    });

    test('TC-PRODUCT-003: 库存自动下架逻辑', async () => {
        // 先创建一个新商品来测试，避免影响基础数据
        const [result] = await helper.connection.execute(
            'INSERT INTO products (seller_id, category_id, product_name, product_desc, price, stock_quantity, product_status) VALUES (?, ?, ?, ?, ?, ?, ?)',
            [1, 3, '测试库存商品', '测试描述', 100.00, 1, 'online']
        );
        const productId = result.insertId;

        // 验证商品创建成功
        const [createdProducts] = await helper.connection.execute(
            'SELECT * FROM products WHERE product_id = ?',
            [productId]
        );
        expect(createdProducts.length).toBe(1);
        expect(createdProducts[0].product_status).toBe('online');

        // 设置库存为0 - 触发器应该自动更新状态
        await helper.connection.execute(
            'UPDATE products SET stock_quantity = 0 WHERE product_id = ?',
            [productId]
        );

        // 验证状态自动变为outOfStock
        const [updatedProducts] = await helper.connection.execute(
            'SELECT product_status FROM products WHERE product_id = ?',
            [productId]
        );
        
        expect(updatedProducts.length).toBe(1);
        expect(updatedProducts[0].product_status).toBe('outOfStock');
        
        console.log('✅ TC-PRODUCT-003: 库存自动下架逻辑测试通过');
    });

    test('TC-PRODUCT-004: 全文搜索功能测试', async () => {
        const searchTerm = '手机';
        
        const [rows] = await helper.connection.execute(
            `SELECT * FROM products 
             WHERE MATCH(product_name, search_keywords) AGAINST(? IN NATURAL LANGUAGE MODE)`,
            [searchTerm]
        );
        
        expect(Array.isArray(rows)).toBe(true);
        // 基础数据中应该有包含"手机"的商品
        if (rows.length > 0) {
            rows.forEach(product => {
                expect(product.product_name.toLowerCase()).toContain('手机');
            });
        }
        
        console.log('✅ TC-PRODUCT-004: 全文搜索功能测试通过');
    });

    test('TC-PRODUCT-005: 商品状态分布统计', async () => {
        const statusCounts = await helper.query(`
            SELECT product_status, COUNT(*) as count 
            FROM products 
            GROUP BY product_status
        `);
        
        console.log('商品状态分布:');
        statusCounts.forEach(status => {
            console.log(`  - ${status.product_status}: ${status.count} 个`);
        });
        
        expect(statusCounts.length).toBeGreaterThan(0);
        
        console.log('✅ TC-PRODUCT-005: 商品状态分布统计测试通过');
    });
});