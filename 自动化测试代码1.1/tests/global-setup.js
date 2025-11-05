
const TestHelper = require('./test_helper');

module.exports = async function globalSetup() {
  console.log('🚀 全局测试环境初始化...');
  
  const helper = new TestHelper();
  try {
    await helper.initDatabase();
    console.log('✅ 全局数据库连接建立成功');
  } catch (error) {
    console.error('❌ 全局数据库连接失败:', error.message);
    throw error;
  } finally {
    await helper.close();
  }
};