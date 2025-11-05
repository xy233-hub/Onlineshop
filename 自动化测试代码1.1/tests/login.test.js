const TestHelper = require('./test_helper');
const { test, expect, beforeAll, afterAll, beforeEach } = require('@jest/globals');

describe('商家登录功能测试', () => {
    let helper;

    beforeAll(async () => {
        helper = new TestHelper();
        await helper.initDatabase();
    });

    beforeEach(async () => {
        helper.authToken = null; // 重置token
    });

    afterAll(async () => {
        await helper.close();
    });

    test('TC-LOGIN-001: 正常登录测试', async () => {
        // 这里我们会模拟API请求
        // 由于没有真实的后端API，我们先验证数据库中的用户存在
        const sellers = await helper.query('SELECT * FROM seller WHERE username = ?', ['admin']);
        
        expect(sellers.length).toBe(1);
        expect(sellers[0].username).toBe('admin');
        
        // 验证密码加密格式
        expect(sellers[0].password).toMatch(/^\$2[ayb]\$.{56}$/);
        
        console.log(' TC-LOGIN-001: 正常登录测试通过 - 用户存在且密码加密正确');
    });

    test('TC-LOGIN-002: 用户不存在测试', async () => {
        const sellers = await helper.query('SELECT * FROM seller WHERE username = ?', ['nonexistent_user']);
        expect(sellers.length).toBe(0);
        console.log(' TC-LOGIN-002: 用户不存在测试通过');
    });

    test('TC-LOGIN-003: 密码验证测试', async () => {
        const sellers = await helper.query('SELECT * FROM seller WHERE username = ?', ['admin']);
        const seller = sellers[0];
        
        // 测试正确密码
        const isCorrectPasswordValid = await helper.verifyPassword('Admin123456', seller.password);
        expect(isCorrectPasswordValid).toBe(true);
        
        // 测试错误密码
        const isWrongPasswordValid = await helper.verifyPassword('wrongpassword', seller.password);
        expect(isWrongPasswordValid).toBe(false);
        
        console.log(' TC-LOGIN-003: 密码验证测试通过');
    });

    test('TC-LOGIN-004: 用户数据完整性测试', async () => {
        const sellers = await helper.query('SELECT * FROM seller WHERE username = ?', ['admin']);
        const seller = sellers[0];
        
        expect(seller).toHaveProperty('seller_id');
        expect(seller).toHaveProperty('username');
        expect(seller).toHaveProperty('password');
        expect(seller).toHaveProperty('create_time');
        
        expect(seller.seller_id).toBeGreaterThan(0);
        expect(seller.username).toBe('admin');
        expect(seller.password.length).toBeGreaterThan(0);
        
        console.log(' TC-LOGIN-004: 用户数据完整性测试通过');
    });
});