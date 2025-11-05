const TestHelper = require('./test_helper');
const { test, expect, beforeAll, afterAll } = require('@jest/globals');

describe('账户管理功能测试', () => {
    let helper;

    beforeAll(async () => {
        helper = new TestHelper();
        await helper.initDatabase();
    });

    afterAll(async () => {
        await helper.close();
    });

    test('TC-ACCOUNT-001: 商家账户数据验证', async () => {
        const sellers = await helper.query('SELECT * FROM seller');
        
        expect(sellers.length).toBe(1);
        
        const seller = sellers[0];
        expect(seller.username).toBe('admin');
        expect(seller.password).toBeDefined();
        expect(seller.create_time).toBeDefined();
        
        console.log(' TC-ACCOUNT-001: 商家账户数据验证通过');
    });

    test('TC-ACCOUNT-002: 密码加密强度验证', async () => {
        const sellers = await helper.query('SELECT * FROM seller WHERE username = ?', ['admin']);
        const seller = sellers[0];
        
        // BCrypt加密格式验证
        expect(seller.password).toMatch(/^\$2[ayb]\$\d+\$.{53}$/);
        
        // 密码长度验证
        expect(seller.password.length).toBe(60);
        
        console.log(' TC-ACCOUNT-002: 密码加密强度验证通过');
    });

    test('TC-ACCOUNT-003: 账户创建时间验证', async () => {
        const sellers = await helper.query('SELECT * FROM seller WHERE username = ?', ['admin']);
        const seller = sellers[0];
          
        // 验证创建时间是有效的日期
        const createTime = new Date(seller.create_time);
        expect(createTime instanceof Date).toBe(true);
        expect(isNaN(createTime.getTime())).toBe(false);
        
        console.log(' TC-ACCOUNT-003: 账户创建时间验证通过');
    });
 
    test('TC-ACCOUNT-004: 账户唯一性验证', async () => {
        const duplicateUsers = await helper.query(`
            SELECT username, COUNT(*) as count   
            FROM seller 
            GROUP BY username    
            HAVING COUNT(*) > 1
        `);
        
        expect(duplicateUsers.length).toBe(0);
        console.log(' TC-ACCOUNT-004: 账户唯一性验证通过');
    });
});
   