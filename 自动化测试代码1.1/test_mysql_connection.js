const mysql = require('mysql2/promise');

async function testConnection() {
    // 常见的MySQL密码列表
    const passwordsToTry = [
        '',           // 空密码
        'password',   // 常用密码
        'root',       // 常用密码
        '12345',     // 常用密码
        'admin',      // 常用密码
        '1234',       // 常用密码
    ];

    console.log(' 测试MySQL连接...');

    for (const password of passwordsToTry) {
        try {
            const connection = await mysql.createConnection({
                host: 'localhost',
                user: 'root',
                password: password,
                connectTimeout: 2000
            });

            console.log(` 连接成功！密码是: ${password === '' ? '空密码' : password}`);
            await connection.end();
            return password;
        } catch (error) {
            console.log(` 密码 "${password === '' ? '空密码' : password}" 失败: ${error.code}`);
        }
    }

    console.log(' 所有常见密码都失败，请手动输入您的MySQL密码');
    return null;
}

testConnection();