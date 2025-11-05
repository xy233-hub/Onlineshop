const request = require('supertest');
const { test, expect, beforeAll, afterAll } = require('@jest/globals');

// 启动模拟服务器
const app = require('../../scripts/mock_api_server');

describe('商家API接口测试', () => {
    let authToken;

    test('TC-API-LOGIN-001: 商家登录API测试', async () => {
        const response = await request(app)
            .post('/api/seller/login')
            .send({
                username: 'admin',
                password: 'Admin123456'
            })
            .expect(200);

        expect(response.body.code).toBe(200);
        expect(response.body.message).toContain('成功');
        expect(response.body.data.token).toBeDefined();
        expect(response.body.data.seller_id).toBe(1);

        authToken = response.body.data.token;
        console.log(' TC-API-LOGIN-001: 商家登录API测试通过');
    });

    test('TC-API-LOGIN-002: 登录失败测试 - 错误密码', async () => {
        const response = await request(app)
            .post('/api/seller/login')
            .send({
                username: 'admin',
                password: 'wrongpassword'
            })
            .expect(400);

        expect(response.body.code).toBe(400);
        expect(response.body.message).toContain('用户名或密码错误');
        console.log(' TC-API-LOGIN-002: 登录失败测试通过');
    });

    test('TC-API-LOGIN-003: 登录失败测试 - 用户不存在', async () => {
        const response = await request(app)
            .post('/api/seller/login')
            .send({
                username: 'nonexistent',
                password: 'password'
            })
            .expect(400);

        expect(response.body.code).toBe(400);
        expect(response.body.message).toContain('用户名或密码错误');
        console.log(' TC-API-LOGIN-003: 用户不存在测试通过');
    });

    test('TC-API-PRODUCT-001: 获取商品列表API测试', async () => {
        const response = await request(app)
            .get('/api/seller/products')
            .set('Authorization', `Bearer ${authToken}`)
            .expect(200);

        expect(response.body.code).toBe(200);
        expect(Array.isArray(response.body.data)).toBe(true);
        expect(response.body.data.length).toBeGreaterThan(0);

        const product = response.body.data[0];
        expect(product).toHaveProperty('product_id');
        expect(product).toHaveProperty('product_name');
        expect(product).toHaveProperty('price');
        console.log(' TC-API-PRODUCT-001: 获取商品列表API测试通过');
    });

    test('TC-API-PRODUCT-002: 发布新商品API测试', async () => {
        const newProduct = {
            product_name: 'API测试商品',
            product_desc: '通过API测试创建的商品',
            image_url: 'https://picsum.photos/400/300',
            price: 199.99
        };

        const response = await request(app)
            .post('/api/seller/product')
            .set('Authorization', `Bearer ${authToken}`)
            .send(newProduct)
            .expect(200);

        expect(response.body.code).toBe(200);
        expect(response.body.message).toContain('成功');
        expect(response.body.data.product_id).toBeDefined();
        console.log(' TC-API-PRODUCT-002: 发布新商品API测试通过');
    });

    test('TC-API-PRODUCT-003: 发布商品验证测试', async () => {
        const response = await request(app)
            .post('/api/seller/product')
            .set('Authorization', `Bearer ${authToken}`)
            .send({
                product_name: '', // 空名称
                product_desc: '描述',
                price: 100
            })
            .expect(400);

        expect(response.body.code).toBe(400);
        expect(response.body.message).toContain('必填');
        console.log(' TC-API-PRODUCT-003: 发布商品验证测试通过');
    });

    test('TC-API-INTENT-001: 获取购买意向列表API测试', async () => {
        const response = await request(app)
            .get('/api/seller/purchase-intents')
            .set('Authorization', `Bearer ${authToken}`)
            .expect(200);

        expect(response.body.code).toBe(200);
        expect(Array.isArray(response.body.data)).toBe(true);
        console.log(' TC-API-INTENT-001: 获取购买意向列表API测试通过');
    });

    test('TC-API-ACCOUNT-001: 修改密码API测试', async () => {
        const response = await request(app)
            .put('/api/seller/password')
            .set('Authorization', `Bearer ${authToken}`)
            .send({
                old_password: 'Admin123456',
                new_password: 'NewPassword123'
            })
            .expect(200);

        expect(response.body.code).toBe(200);
        expect(response.body.message).toContain('成功');
        console.log(' TC-API-ACCOUNT-001: 修改密码API测试通过');
    });

    test('TC-API-ACCOUNT-002: 修改密码验证测试', async () => {
        const response = await request(app)
            .put('/api/seller/password')
            .set('Authorization', `Bearer ${authToken}`)
            .send({
                old_password: 'WrongPassword', // 错误原密码
                new_password: 'NewPassword123'
            })
            .expect(400);

        expect(response.body.code).toBe(400);
        expect(response.body.message).toContain('原密码错误');
        console.log(' TC-API-ACCOUNT-002: 修改密码验证测试通过');
    });
});