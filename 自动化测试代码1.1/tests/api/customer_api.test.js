const request = require('supertest');
const { test, expect } = require('@jest/globals');

const app = require('../../scripts/mock_api_server');

describe('顾客API接口测试', () => {
    test('TC-API-CUSTOMER-001: 获取在售商品API测试', async () => {
        const response = await request(app)
            .get('/api/product')
            .expect(200);

        expect(response.body.code).toBe(200);
        expect(Array.isArray(response.body.data)).toBe(true);
        
        const products = response.body.data;
        if (products.length > 0) {
            products.forEach(product => {
                expect(product.product_status).toBe('online');
            });
        }
        console.log(' TC-API-CUSTOMER-001: 获取在售商品API测试通过');
    });

    test('TC-API-CUSTOMER-002: 提交购买意向API测试', async () => {
        const purchaseData = {
            product_id: 1,
            customer_name: 'API测试客户',
            customer_phone: '13800138000',
            customer_address: 'API测试地址',
            message: '通过API测试提交'
        };

        const response = await request(app)
            .post('/api/product/purchase')
            .send(purchaseData)
            .expect(200);

        expect(response.body.code).toBe(200);
        expect(response.body.message).toContain('成功');
        expect(response.body.data.purchase_id).toBeDefined();
        console.log(' TC-API-CUSTOMER-002: 提交购买意向API测试通过');
    });

    test('TC-API-CUSTOMER-003: 提交意向验证测试 - 必填字段', async () => {
        const response = await request(app)
            .post('/api/product/purchase')
            .send({
                product_id: 1,
                customer_name: '', // 空姓名
                customer_phone: '13800138000',
                customer_address: '地址'
            })
            .expect(400);

        expect(response.body.code).toBe(400);
        expect(response.body.message).toContain('必填');
        console.log(' TC-API-CUSTOMER-003: 提交意向验证测试通过');
    });

    test('TC-API-CUSTOMER-004: 提交意向验证测试 - 手机号格式', async () => {
        const response = await request(app)
            .post('/api/product/purchase')
            .send({
                product_id: 1,
                customer_name: '测试客户',
                customer_phone: '1380013', // 错误格式
                customer_address: '地址'
            })
            .expect(400);

        expect(response.body.code).toBe(400);
        expect(response.body.message).toContain('手机号');
        console.log(' TC-API-CUSTOMER-004: 手机号格式验证测试通过');
    });

    test('TC-API-CUSTOMER-005: 提交意向验证测试 - 商品不可购买', async () => {
        // 测试冻结或售出的商品
        const response = await request(app)
            .post('/api/product/purchase')
            .send({
                product_id: 999, // 不存在的商品
                customer_name: '测试客户',
                customer_phone: '13800138000',
                customer_address: '地址'
            })
            .expect(400);

        expect(response.body.code).toBe(400);
        expect(response.body.message).toContain('商品不存在');
        console.log(' TC-API-CUSTOMER-005: 商品不可购买验证测试通过');
    });
});