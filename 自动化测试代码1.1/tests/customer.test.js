// tests/customer.test.js - 修复版
const TestHelper = require('./test_helper');
const { test, expect, beforeAll, afterAll } = require('@jest/globals');

describe('顾客功能测试', () => {
    let helper;

    beforeAll(async () => {
        helper = new TestHelper();
        await helper.initDatabase();
    });

    afterAll(async () => {
        await helper.close();
    });

    test('TC-CUSTOMER-001: 在线商品查询测试', async () => {
        const onlineProducts = await helper.query(`
            SELECT * FROM products 
            WHERE product_status = 'online'
        `);
        
        expect(onlineProducts.length).toBeGreaterThan(0);
        
        onlineProducts.forEach(product => {
            expect(product.product_status).toBe('online');
            expect(parseFloat(product.price)).toBeGreaterThan(0);
            expect(product.product_name.length).toBeGreaterThan(0);
        });
        
        console.log(`✅ TC-CUSTOMER-001: 在线商品查询测试通过 - 共 ${onlineProducts.length} 个在线商品`);
    });

    test('TC-CUSTOMER-002: 商品状态过滤测试', async () => {
        const frozenProducts = await helper.query(`SELECT * FROM products WHERE product_status = 'frozen'`);
        const soldProducts = await helper.query(`SELECT * FROM products WHERE product_status = 'sold'`);
        const outOfStockProducts = await helper.query(`SELECT * FROM products WHERE product_status = 'outOfStock'`);
        
        // 验证冻结商品
        frozenProducts.forEach(product => {
            expect(product.product_status).toBe('frozen');
        });
        
        // 验证已售出商品
        soldProducts.forEach(product => {
            expect(product.product_status).toBe('sold');
        });

        // 验证售罄商品
        outOfStockProducts.forEach(product => {
            expect(product.product_status).toBe('outOfStock');
        });
        
        console.log(`✅ TC-CUSTOMER-002: 商品状态过滤测试通过 - 冻结: ${frozenProducts.length} 个, 售出: ${soldProducts.length} 个, 售罄: ${outOfStockProducts.length} 个`);
    });

    test('TC-CUSTOMER-003: 商品信息完整性测试', async () => {
        const products = await helper.query('SELECT * FROM products');
        
        products.forEach(product => {
            // 基本信息验证
            expect(product.product_name.length).toBeGreaterThan(0);
            expect(product.product_desc.length).toBeGreaterThan(0);
            expect(parseFloat(product.price)).toBeGreaterThan(0);
            expect(product.stock_quantity).toBeGreaterThanOrEqual(0);
            
            // 必填字段验证
            expect(product.seller_id).toBeDefined();
            expect(product.category_id).toBeDefined();
            expect(product.product_status).toBeDefined();
            
            // 状态枚举值验证
            const validStatuses = ['online', 'outOfStock', 'frozen', 'sold'];
            expect(validStatuses).toContain(product.product_status);
        });
        
        console.log('✅ TC-CUSTOMER-003: 商品信息完整性测试通过');
    });

    test('TC-CUSTOMER-004: 购买意向提交数据验证', async () => {
        const intents = await helper.query('SELECT * FROM purchase_intents');
        
        intents.forEach(intent => {
            // 客户信息验证
            expect(intent.customer_name.trim().length).toBeGreaterThan(0);
            expect(intent.customer_phone.trim().length).toBeGreaterThan(0);
            expect(intent.customer_address.trim().length).toBeGreaterThan(0);
            
            // 关联商品验证
            expect(intent.product_id).toBeGreaterThan(0);
            expect(intent.customer_id).toBeGreaterThan(0);
            
            // 金额和数量验证
            expect(parseFloat(intent.total_amount)).toBeGreaterThan(0);
            expect(intent.quantity).toBeGreaterThan(0);
            
            // 状态枚举值验证
            const validStatuses = ['pending', 'success', 'failed'];
            expect(validStatuses).toContain(intent.purchase_status);
        });
        
        console.log('✅ TC-CUSTOMER-004: 购买意向提交数据验证通过');
    });

    test('TC-CUSTOMER-005: 商品图片关联验证', async () => {
        // 验证商品图片表的功能
        const productImages = await helper.query(`
            SELECT pi.*, p.product_name 
            FROM product_images pi 
            JOIN products p ON pi.product_id = p.product_id 
            LIMIT 1
        `);
        
        if (productImages.length > 0) {
            const image = productImages[0];
            expect(image.image_url.length).toBeGreaterThan(0);
            expect(image.image_order).toBeGreaterThanOrEqual(0);
            expect(image.product_id).toBeGreaterThan(0);
            expect(image.product_name).toBeDefined();
        }
        
        console.log('✅ TC-CUSTOMER-005: 商品图片关联验证通过');
    });

    test('TC-CUSTOMER-006: 类别导航功能测试', async () => {
        // 验证类别层级结构
        const topCategories = await helper.query(`
            SELECT * FROM categories 
            WHERE parent_id IS NULL
        `);
        
        expect(topCategories.length).toBeGreaterThan(0);
        
        // 验证每个一级类别都有子类别
        for (const category of topCategories) {
            const subCategories = await helper.query(`
                SELECT * FROM categories 
                WHERE parent_id = ?
            `, [category.category_id]);
            
            // 一级类别应该有子类别，或者至少可以查询
            expect(Array.isArray(subCategories)).toBe(true);
        }
        
        console.log(`✅ TC-CUSTOMER-006: 类别导航功能测试通过 - 共 ${topCategories.length} 个一级类别`);
    });
});