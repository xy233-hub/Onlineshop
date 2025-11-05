// scripts/mock_api_server.js
const express = require('express');
const bcrypt = require('bcrypt');
const mysql = require('mysql2/promise');

const app = express();
const PORT = 3000;

app.use(express.json());

/* ================= 模拟数据 ================= */
let mockData = {
  sellers: [
    {
      seller_id: 1,
      username: 'admin',
      // 明文：Admin123456
      password: '$2a$12$ycJEdpJ1TYxBzDCmuGju2eUbTP.vm0yNYYyFpK3ahxsCLq9d2Iq32',
      create_time: new Date()
    }
  ],
  products: [
    {
      product_id: 1,
      seller_id: 1,
      product_name: '笔记本电脑',
      product_desc: '高性能游戏笔记本',
      image_url: 'https://picsum.photos/400/300',
      price: 5999.00,
      product_status: 'online',
      created_at: new Date(),
      updated_at: new Date()
    },
    {
      product_id: 2,
      seller_id: 1,
      product_name: '智能手机',
      product_desc: '最新款5G手机',
      image_url: 'https://picsum.photos/401/300',
      price: 3999.00,
      product_status: 'online',
      created_at: new Date(),
      updated_at: new Date()
    }
  ],
  purchase_intents: [
    {
      purchase_id: 1,
      product_id: 1,
      customer_name: '张三',
      customer_phone: '13800138001',
      customer_address: '北京市朝阳区建国路100号',
      purchase_status: 'pending',
      created_at: new Date(),
      updated_at: new Date()
    }
  ],
  tokens: {}
};

/* ================= 中间件 ================= */
const authenticateToken = (req, res, next) => {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.split(' ')[1];

  if (!token || !mockData.tokens[token]) {
    return res.status(401).json({ code: 401, message: '未授权访问' });
  }
  req.seller = mockData.tokens[token];
  next();
};

/* ================= 路由 ================= */
// 商家登录
app.post('/api/seller/login', async (req, res) => {
  try {
    const { username, password } = req.body;
    if (!username || !password) {
      return res.status(400).json({ code: 400, message: '用户名和密码必填' });
    }
    const seller = mockData.sellers.find(s => s.username === username);
    if (!seller || !(await bcrypt.compare(password, seller.password))) {
      return res.status(400).json({ code: 400, message: '用户名或密码错误' });
    }
    const token = `mock_token_${Date.now()}`;
    mockData.tokens[token] = { seller_id: seller.seller_id, username: seller.username };
    res.json({ code: 200, message: '登录成功', data: { token, seller_id: seller.seller_id } });
  } catch {
    res.status(500).json({ code: 500, message: '服务器错误' });
  }
});

// 获取商家商品列表（需认证）
app.get('/api/seller/products', authenticateToken, (req, res) => {
  const list = mockData.products.filter(p => p.seller_id === req.seller.seller_id);
  res.json({ code: 200, message: '获取成功', data: list });
});

// 发布新商品（需认证）
app.post('/api/seller/product', authenticateToken, (req, res) => {
  const { product_name, product_desc, image_url, price } = req.body;
  if (!product_name || !product_desc || !price) {
    return res.status(400).json({ code: 400, message: '商品名称、描述和价格必填' });
  }
  const newProduct = {
    product_id: mockData.products.length + 1,
    seller_id: req.seller.seller_id,
    product_name,
    product_desc,
    image_url: image_url || 'https://picsum.photos/400/300',
    price: parseFloat(price),
    product_status: 'online',
    created_at: new Date(),
    updated_at: new Date()
  };
  mockData.products.push(newProduct);
  res.json({ code: 200, message: '商品发布成功', data: { product_id: newProduct.product_id } });
});

// 获取购买意向列表（需认证）
app.get('/api/seller/purchase-intents', authenticateToken, (req, res) => {
  const sellerProductIds = mockData.products
    .filter(p => p.seller_id === req.seller.seller_id)
    .map(p => p.product_id);
  const intents = mockData.purchase_intents.filter(pi => sellerProductIds.includes(pi.product_id));
  res.json({ code: 200, message: '获取成功', data: intents });
});

// 顾客获取在售商品
app.get('/api/product', (req, res) => {
  const online = mockData.products.filter(p => p.product_status === 'online');
  res.json({ code: 200, message: '获取成功', data: online });
});

// 顾客提交购买意向
app.post('/api/product/purchase', (req, res) => {
  const { product_id, customer_name, customer_phone, customer_address, message } = req.body;
  if (!product_id || !customer_name || !customer_phone || !customer_address) {
    return res.status(400).json({ code: 400, message: '必填字段不能为空' });
  }
  if (!/^\d{11}$/.test(customer_phone)) {
    return res.status(400).json({ code: 400, message: '手机号格式不正确' });
  }
  const product = mockData.products.find(p => p.product_id === parseInt(product_id));
  if (!product || product.product_status !== 'online') {
    return res.status(400).json({ code: 400, message: '商品不存在或不可购买' });
  }
  const newIntent = {
    purchase_id: mockData.purchase_intents.length + 1,
    product_id: parseInt(product_id),
    customer_name,
    customer_phone,
    customer_address,
    message: message || '',
    purchase_status: 'pending',
    created_at: new Date(),
    updated_at: new Date()
  };
  mockData.purchase_intents.push(newIntent);
  res.json({ code: 200, message: '购买意向提交成功', data: { purchase_id: newIntent.purchase_id } });
});

// 修改密码（需认证）
app.put('/api/seller/password', authenticateToken, async (req, res) => {
  const { old_password, new_password } = req.body;
  if (!old_password || !new_password) {
    return res.status(400).json({ code: 400, message: '原密码和新密码必填' });
  }
  const seller = mockData.sellers.find(s => s.seller_id === req.seller.seller_id);
  const valid = await bcrypt.compare(old_password, seller.password);
  if (!valid) {
    return res.status(400).json({ code: 400, message: '原密码错误' });
  }
  if (new_password.length < 6) {
    return res.status(400).json({ code: 400, message: '新密码长度至少6位' });
  }
  seller.password = bcrypt.hashSync(new_password, 12);
  res.json({ code: 200, message: '密码修改成功' });
});

/* ================= 端口监听 ================= */
// 仅在直接运行本文件时才启动服务器，避免测试时重复监听
if (require.main === module) {
  app.listen(PORT, () => {
    console.log(` 模拟API服务器运行在 http://localhost:${PORT}`);
    console.log(` API文档: http://localhost:${PORT}/api`);
  });
}

/* ================= 导出供测试使用 ================= */
module.exports = app;