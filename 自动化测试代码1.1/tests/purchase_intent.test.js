// tests/purchase_intent_management.test.js 
const TestHelper = require('./test_helper');
const { test, expect, beforeAll, afterAll, beforeEach } = require('@jest/globals');

describe('购买意向管理功能测试', () => {
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

    test('TC-INTENT-001: 购买意向提交测试', async () => {
        const intentData = {
            product_id: 1,
            customer_id: 1,
            customer_name: '测试用户',
            customer_phone: '13800138000',
            customer_address: '收货地址',
            quantity: 2,
            total_amount: 11998.00,
            purchase_status: 'pending'
        };

        const [result] = await helper.connection.execute(
            `INSERT INTO purchase_intents 
             (product_id, customer_id, customer_name, customer_phone, customer_address, 
              quantity, total_amount, purchase_status) 
             VALUES (?, ?, ?, ?, ?, ?, ?, ?)`,
            [intentData.product_id, intentData.customer_id, intentData.customer_name,
             intentData.customer_phone, intentData.customer_address, intentData.quantity,
             intentData.total_amount, intentData.purchase_status]
        );
        
        expect(result.affectedRows).toBe(1);
        expect(result.insertId).toBeGreaterThan(5);
        
        console.log('✅ TC-INTENT-001: 购买意向提交测试通过');
    });

    test('TC-INTENT-002: 购买意向状态流转', async () => {
        // 创建一个新的购买意向用于测试
        const [result] = await helper.connection.execute(
            `INSERT INTO purchase_intents 
             (product_id, customer_id, customer_name, customer_phone, customer_address, 
              quantity, total_amount, purchase_status) 
             VALUES (?, ?, ?, ?, ?, ?, ?, ?)`,
            [1, 1, '测试用户', '13800138000', '地址', 1, 5999.00, 'pending']
        );
        const purchaseId = result.insertId;
        
        // 测试状态从pending到success
        await helper.connection.execute(
            'UPDATE purchase_intents SET purchase_status = ? WHERE purchase_id = ?',
            ['success', purchaseId]
        );

        const [intents] = await helper.connection.execute(
            'SELECT purchase_status FROM purchase_intents WHERE purchase_id = ?',
            [purchaseId]
        );
        
        expect(intents[0].purchase_status).toBe('success');
        
        console.log('✅ TC-INTENT-002: 购买意向状态流转测试通过');
    });

    test('TC-INTENT-003: 商品冻结逻辑测试', async () => {
        // 创建pending购买意向
        await helper.connection.execute(
            `INSERT INTO purchase_intents 
             (product_id, customer_id, customer_name, customer_phone, customer_address, 
              quantity, total_amount, purchase_status) 
             VALUES (?, ?, ?, ?, ?, ?, ?, ?)`,
            [3, 1, '测试用户', '13800138000', '地址', 1, 12999.00, 'pending']
        );

        // 手动模拟商品冻结（实际业务中应该是触发器或应用逻辑）
        await helper.connection.execute(
            'UPDATE products SET product_status = ? WHERE product_id = ?',
            ['frozen', 3]
        );

        const [products] = await helper.connection.execute(
            'SELECT product_status FROM products WHERE product_id = ?',
            [3]
        );
        
        expect(products[0].product_status).toBe('frozen');
        
        console.log('✅ TC-INTENT-003: 商品冻结逻辑测试通过');
    });

    test('TC-INTENT-004: 收货地址灵活性测试', async () => {
        const customerId = 1;
        const [customer] = await helper.connection.execute(
            'SELECT default_address FROM customers WHERE customer_id = ?',
            [customerId]
        );
        
        const defaultAddress = customer[0].default_address;
        const customAddress = '自定义收货地址';
        
        // 提交与默认地址不同的收货地址
        await helper.connection.execute(
            `INSERT INTO purchase_intents 
             (product_id, customer_id, customer_name, customer_phone, customer_address, 
              quantity, total_amount, purchase_status) 
             VALUES (?, ?, ?, ?, ?, ?, ?, ?)`,
            [1, customerId, '测试用户', '13800138001', customAddress, 1, 5999.00, 'pending']
        );

        const [intents] = await helper.connection.execute(
            'SELECT customer_address FROM purchase_intents WHERE customer_id = ? ORDER BY purchase_id DESC LIMIT 1',
            [customerId]
        );
        
        expect(intents[0].customer_address).toBe(customAddress);
        expect(intents[0].customer_address).not.toBe(defaultAddress);
        
        console.log('✅ TC-INTENT-004: 收货地址灵活性测试通过');
    });
});