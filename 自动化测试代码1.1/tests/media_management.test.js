// tests/media_management.test.js - 修复版
const TestHelper = require('./test_helper');
const { test, expect, beforeAll, afterAll, beforeEach } = require('@jest/globals');

describe('媒体资源管理测试', () => {
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

    test('TC-MEDIA-001: 商品图片上传测试', async () => {
        const imageData = {
            product_id: 1, // 使用基础数据中的商品
            image_url: 'https://example.com/image1.jpg',
            image_order: 0
        };

        const [result] = await helper.connection.execute(
            `INSERT INTO product_images (product_id, image_url, image_order) 
             VALUES (?, ?, ?)`,
            [imageData.product_id, imageData.image_url, imageData.image_order]
        );
        
        expect(result.affectedRows).toBe(1);
        
        console.log('✅ TC-MEDIA-001: 商品图片上传测试通过');
    });

    test('TC-MEDIA-002: 图片排序功能测试', async () => {
        // 插入多张图片
        const images = [
            { product_id: 1, image_url: 'image1.jpg', image_order: 2 },
            { product_id: 1, image_url: 'image2.jpg', image_order: 1 },
            { product_id: 1, image_url: 'image3.jpg', image_order: 0 }
        ];

        for (const image of images) {
            await helper.connection.execute(
                'INSERT INTO product_images (product_id, image_url, image_order) VALUES (?, ?, ?)',
                [image.product_id, image.image_url, image.image_order]
            );
        }

        const [rows] = await helper.connection.execute(
            `SELECT * FROM product_images 
             WHERE product_id = ? ORDER BY image_order`,
            [1]
        );
        
        // 验证排序正确性
        for (let i = 0; i < rows.length - 1; i++) {
            expect(rows[i].image_order).toBeLessThanOrEqual(rows[i + 1].image_order);
        }
        
        console.log('✅ TC-MEDIA-002: 图片排序功能测试通过');
    });

    test('TC-MEDIA-003: 媒体资源上传测试', async () => {
        const mediaData = {
            product_id: 1,
            media_type: 'image',
            media_url: 'https://example.com/media1.jpg',
            file_name: 'product_image.jpg',
            file_size: 1024000,
            mime_type: 'image/jpeg',
            display_order: 1,
            is_embedded: true
        };

        const [result] = await helper.connection.execute(
            `INSERT INTO media_resources 
             (product_id, media_type, media_url, file_name, file_size, mime_type, display_order, is_embedded) 
             VALUES (?, ?, ?, ?, ?, ?, ?, ?)`,
            [mediaData.product_id, mediaData.media_type, mediaData.media_url,
             mediaData.file_name, mediaData.file_size, mediaData.mime_type,
             mediaData.display_order, mediaData.is_embedded]
        );
        
        expect(result.affectedRows).toBe(1);
        
        console.log('✅ TC-MEDIA-003: 媒体资源上传测试通过');
    });
});