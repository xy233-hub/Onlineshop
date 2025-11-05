const mysql = require('mysql2/promise');

async function cleanDatabase() {
    const password = '12345';
    let connection;

    try {
        connection = await mysql.createConnection({
            host: 'localhost',
            user: 'root',
            password: password
        });

        // 直接删除数据库
        await connection.query('DROP DATABASE IF EXISTS seller_management_test');
        console.log(' 数据库清理完成');

    } catch (error) {
        console.error(' 数据库清理失败:', error.message);
    } finally {
        if (connection) {
            await connection.end();
        }
    }
}

cleanDatabase();