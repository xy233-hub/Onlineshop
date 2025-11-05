console.log('=== 所有包安装验证 ===\n');

// 测试异步操作
async function testPackages() {
  try {
    // 1. 测试 Bcrypt
    const bcrypt = require('bcrypt');
    const hash = await bcrypt.hash('test123', 5);
    console.log('✅ Bcrypt: 加密成功');
    
    // 2. 测试 Axios
    const axios = require('axios');
    console.log('✅ Axios: 版本', axios.VERSION);
    
    // 3. 测试 Supertest
    const supertest = require('supertest');
    console.log('✅ Supertest: 函数可用');
    
    // 4. 测试 MySQL2 (不实际连接)
    const mysql = require('mysql2');
    console.log('✅ MySQL2: 加载成功');
    
    // 5. 测试 Jest
    const jest = require('jest');
    console.log('✅ Jest: 加载成功');
    
    // 6. 测试 Puppeteer
    const puppeteer = require('puppeteer');
    console.log('✅ Puppeteer: 加载成功');
    
    console.log('\n🎉 所有包安装成功！可以开始开发了！');
    
  } catch (error) {
    console.log('❌ 错误:', error.message);
  }
}

testPackages();