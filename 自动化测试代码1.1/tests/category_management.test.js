// tests/category_management.test.js - 修复版
const TestHelper = require('./test_helper');
const { test, expect, beforeAll, afterAll, beforeEach } = require('@jest/globals');

describe('商品类别管理测试', () => {
    let helper;

    beforeAll(async () => {
        helper = new TestHelper();
        await helper.initDatabase();
    });

    beforeEach(async () => {
        // 清理测试过程中创建的类别，但保留基础类别
        await helper.query('DELETE FROM categories WHERE category_id > 6');
    });

    afterAll(async () => {
        await helper.close();
    });

    test('TC-CATEGORY-001: 一级类别创建测试', async () => {
        const [result] = await helper.connection.execute(
            'INSERT INTO categories (category_name, category_level) VALUES (?, ?)',
            ['家用电器', 1]
        );
        
        expect(result.affectedRows).toBe(1);
        expect(result.insertId).toBeGreaterThan(6);
        
        const [categories] = await helper.connection.execute(
            'SELECT * FROM categories WHERE category_id = ?',
            [result.insertId]
        );
        
        expect(categories[0].parent_id).toBeNull();
        expect(categories[0].category_level).toBe(1);
        
        console.log('✅ TC-CATEGORY-001: 一级类别创建测试通过');
    });

    test('TC-CATEGORY-002: 二级类别创建测试', async () => {
        // 使用基础数据中的父类别ID
        const [result] = await helper.connection.execute(
            'INSERT INTO categories (parent_id, category_name, category_level) VALUES (?, ?, ?)',
            [1, '冰箱', 2] // parent_id=1是电子产品
        );
        
        expect(result.affectedRows).toBe(1);
        
        const [categories] = await helper.connection.execute(
            'SELECT * FROM categories WHERE category_id = ?',
            [result.insertId]
        );
        
        expect(categories[0].parent_id).toBe(1);
        expect(categories[0].category_level).toBe(2);
        
        console.log('✅ TC-CATEGORY-002: 二级类别创建测试通过');
    });

    test('TC-CATEGORY-003: 类别层级关系验证', async () => {
        const [childCategories] = await helper.connection.execute(
            'SELECT * FROM categories WHERE parent_id = ?',
            [1] // 电子产品的子类别
        );
        
        expect(childCategories.length).toBeGreaterThan(0);
        childCategories.forEach(category => {
            expect(category.category_level).toBe(2);
            expect(category.parent_id).toBe(1);
        });
        
        console.log('✅ TC-CATEGORY-003: 类别层级关系验证通过');
    });

    test('TC-CATEGORY-004: 类别删除级联测试', async () => {
        // 创建一个全新的类别层次结构用于测试，确保没有商品引用
        const [parentResult] = await helper.connection.execute(
            'INSERT INTO categories (category_name, category_level) VALUES (?, ?)',
            ['测试父类', 1]
        );
        const parentId = parentResult.insertId;
        
        // 创建子类别
        const [childResult] = await helper.connection.execute(
            'INSERT INTO categories (parent_id, category_name, category_level) VALUES (?, ?, ?)',
            [parentId, '测试子类', 2]
        );
        const childId = childResult.insertId;
        
        // 验证子类别存在
        const [beforeDelete] = await helper.connection.execute(
            'SELECT * FROM categories WHERE category_id = ?',
            [childId]
        );
        expect(beforeDelete.length).toBe(1);
        expect(beforeDelete[0].parent_id).toBe(parentId);
        
        // 删除父类别，应该级联删除子类别
        await helper.connection.execute(
            'DELETE FROM categories WHERE category_id = ?',
            [parentId]
        );
        
        // 验证子类别也被删除
        const [afterDelete] = await helper.connection.execute(
            'SELECT * FROM categories WHERE category_id = ?',
            [childId]
        );
        
        expect(afterDelete.length).toBe(0);
        
        console.log('✅ TC-CATEGORY-004: 类别删除级联测试通过');
    });

    test('TC-CATEGORY-005: 类别被商品引用时无法删除测试', async () => {
        // 测试当类别被商品引用时，由于 ON DELETE RESTRICT 约束，应该无法删除
        await expect(
            helper.connection.execute(
                'DELETE FROM categories WHERE category_id = ?',
                [3] // 手机类别，被商品引用
            )
        ).rejects.toThrow();
        
        console.log('✅ TC-CATEGORY-005: 类别被商品引用时无法删除测试通过');
    });
});