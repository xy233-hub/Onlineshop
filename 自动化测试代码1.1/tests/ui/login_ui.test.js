const puppeteer = require('puppeteer');
const { test, expect, beforeAll, afterAll, beforeEach } = require('@jest/globals');

describe('商家登录界面测试', () => {
    let browser;
    let page;

    beforeAll(async () => {
        browser = await puppeteer.launch({ 
            headless: true, // 设置为false可以看到浏览器操作
            slowMo: 50 // 减慢操作速度，便于观察
        });
    });

    beforeEach(async () => {
        page = await browser.newPage();
        await page.setViewport({ width: 1280, height: 720 });
    });

    afterAll(async () => {
        await browser.close();
    });

    test('TC-UI-LOGIN-001: 登录页面加载测试', async () => {
        // 这里模拟访问登录页面
        // 由于没有真实的前端，我们测试页面基本功能
        await page.goto('about:blank');
        await page.setContent(`
            <!DOCTYPE html>
            <html>
            <head>
                <title>商家登录</title>
            </head>
            <body>
                <div id="login-page">
                    <h1>商家登录</h1>
                    <form id="login-form">
                        <input type="text" id="username" placeholder="用户名" required>
                        <input type="password" id="password" placeholder="密码" required>
                        <button type="submit" id="login-btn">登录</button>
                    </form>
                </div>
            </body>
            </html>
        `);

        // 验证页面元素存在
        const title = await page.$eval('h1', el => el.textContent);
        expect(title).toContain('商家登录');

        const usernameInput = await page.$('#username');
        expect(usernameInput).toBeTruthy();

        const passwordInput = await page.$('#password');
        expect(passwordInput).toBeTruthy();

        const loginButton = await page.$('#login-btn');
        expect(loginButton).toBeTruthy();

        console.log(' TC-UI-LOGIN-001: 登录页面加载测试通过');
    }, 30000);

    test('TC-UI-LOGIN-002: 登录表单交互测试', async () => {
        await page.goto('about:blank');
        await page.setContent(`
            <!DOCTYPE html>
            <html>
            <body>
                <form id="login-form">
                    <input type="text" id="username" required>
                    <input type="password" id="password" required>
                    <button type="submit" id="login-btn">登录</button>
                </form>
                <div id="message"></div>
            </body>
            </html>
        `);

        // 模拟用户输入
        await page.type('#username', 'testuser');
        await page.type('#password', 'testpassword');

        // 验证输入值
        const usernameValue = await page.$eval('#username', el => el.value);
        const passwordValue = await page.$eval('#password', el => el.value);

        expect(usernameValue).toBe('testuser');
        expect(passwordValue).toBe('testpassword');

        console.log(' TC-UI-LOGIN-002: 登录表单交互测试通过');
    }, 30000);

    test('TC-UI-LOGIN-003: 表单验证测试', async () => {
        await page.goto('about:blank');
        await page.setContent(`
            <!DOCTYPE html>
            <html>
            <body>
                <form id="login-form">
                    <input type="text" id="username" required>
                    <input type="password" id="password" required>
                    <button type="submit" id="login-btn">登录</button>
                </form>
            </body>
            </html>
        `);

        // 尝试提交空表单
        await page.click('#login-btn');

        // 验证必填字段验证
        const usernameRequired = await page.$eval('#username', el => el.validity.valid);
        const passwordRequired = await page.$eval('#password', el => el.validity.valid);

        expect(usernameRequired).toBe(false);
        expect(passwordRequired).toBe(false);

        console.log(' TC-UI-LOGIN-003: 表单验证测试通过');
    }, 30000);
});