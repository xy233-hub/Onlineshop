// tests/customer_management.test.js - 修复版
const TestHelper = require('./test_helper');
const { test, expect, beforeAll, afterAll, beforeEach } = require('@jest/globals');

describe('客户管理功能测试', () => {
    let helper;

    beforeAll(async () => {
        helper = new TestHelper();
        await helper.initDatabase();
    });

    beforeEach(async () => {
        // 清理测试过程中创建的客户，但保留基础客户
        await helper.query('DELETE FROM customers WHERE customer_id > 2');
    });

    afterAll(async () => {
        await helper.close();
    });

    test('TC-CUSTOMER-001: 客户注册功能测试', async () => {
        const customerData = {
            username: 'newcustomer',
            password: '$2a$12$newhashedpassword',
            phone: '13800138003',
            default_address: '新客户地址'
        };

        const [result] = await helper.connection.execute(
            `INSERT INTO customers (username, password, phone, default_address) 
             VALUES (?, ?, ?, ?)`,
            [customerData.username, customerData.password, customerData.phone, customerData.default_address]
        );
        
        expect(result.affectedRows).toBe(1);
        expect(result.insertId).toBeGreaterThan(2);
        
        console.log('✅ TC-CUSTOMER-001: 客户注册功能测试通过');
    });

    test('TC-CUSTOMER-002: 客户用户名唯一性验证', async () => {
        // 直接使用基础数据中已存在的用户名进行测试
        await expect(
            helper.connection.execute(
                'INSERT INTO customers (username, password, phone) VALUES (?, ?, ?)',
                ['customer01', 'password', '13800138009'] // customer01 已存在
            )
        ).rejects.toThrow();
        
        console.log('✅ TC-CUSTOMER-002: 客户用户名唯一性验证通过');
    });

    test('TC-CUSTOMER-003: 客户手机号唯一性验证', async () => {
        // 直接使用基础数据中已存在的手机号进行测试
        await expect(
            helper.connection.execute(
                'INSERT INTO customers (username, password, phone) VALUES (?, ?, ?)',
                ['newuser123', 'password', '13800138001'] // 13800138001 已存在
            )
        ).rejects.toThrow();
        
        console.log('✅ TC-CUSTOMER-003: 客户手机号唯一性验证通过');
    });

    test('TC-CUSTOMER-004: 客户信息更新测试', async () => {
        const newAddress = '更新后的地址';
        
        // 先验证客户存在
        const [existingCustomers] = await helper.connection.execute(
            'SELECT * FROM customers WHERE customer_id = ?',
            [1]
        );
        
        expect(existingCustomers.length).toBe(1);
        const originalAddress = existingCustomers[0].default_address;
        
        // 更新客户信息
        await helper.connection.execute(
            'UPDATE customers SET default_address = ? WHERE customer_id = ?',
            [newAddress, 1]
        );

        // 重新查询验证更新
        const [updatedCustomers] = await helper.connection.execute(
            'SELECT * FROM customers WHERE customer_id = ?',
            [1]
        );
        
        expect(updatedCustomers.length).toBe(1);
        expect(updatedCustomers[0].default_address).toBe(newAddress);
        expect(updatedCustomers[0].updated_at).toBeDefined();
        
        // 恢复原始地址，避免影响其他测试
        await helper.connection.execute(
            'UPDATE customers SET default_address = ? WHERE customer_id = ?',
            [originalAddress, 1]
        );
        
        console.log('✅ TC-CUSTOMER-004: 客户信息更新测试通过');
    });

    test('TC-CUSTOMER-005: 客户用户名长度验证', async () => {
        // 测试短用户名 - 应该被触发器拒绝
        await expect(
            helper.connection.execute(
                'INSERT INTO customers (username, password, phone) VALUES (?, ?, ?)',
                ['user', 'password', '13800138005'] // 4位，太短
            )
        ).rejects.toThrow();
        
        // 测试长用户名 - 应该被触发器拒绝
        await expect(
            helper.connection.execute(
                'INSERT INTO customers (username, password, phone) VALUES (?, ?, ?)',
                ['thisusernameistoolongforvalidation', 'password', '13800138006'] // 太长
            )
        ).rejects.toThrow();
        
        // 测试合规用户名 - 应该成功
        const validResult = await helper.connection.execute(
            'INSERT INTO customers (username, password, phone) VALUES (?, ?, ?)',
            ['validuser', 'password', '13800138007'] // 9位，合规
        );
        expect(validResult[0].affectedRows).toBe(1);
        
        console.log('✅ TC-CUSTOMER-005: 客户用户名长度验证通过');
    });
});