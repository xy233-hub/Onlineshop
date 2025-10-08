// src/api/seller/index.js

// 登录
export async function sellerLogin(username, password) {
    const res = await fetch('/api/seller/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password })
    })
    return res.json()
}

// 发布新商品
export async function addProduct(product, token) {
    const res = await fetch('/api/seller/product', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify(product)
    })
    return res.json()
}

// 获取历史商品
export async function getProducts(token) {
    const res = await fetch('/api/seller/products', {
        headers: { Authorization: `Bearer ${token}` }
    })
    return res.json()
}

// 冻结商品
export async function freezeProduct(product_id, token) {
    const res = await fetch('/api/seller/product/freeze', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify({ product_id })
    })
    return res.json()
}

// 解冻商品
export async function unfreezeProduct(product_id, token) {
    const res = await fetch('/api/seller/product/unfreeze', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify({ product_id })
    })
    return res.json()
}

// 标记售出
export async function markSold(product_id, token) {
    const res = await fetch('/api/seller/product/mark-sold', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify({ product_id })
    })
    return res.json()
}

// 修改密码
export async function changePassword(old_password, new_password, token) {
    const res = await fetch('/api/seller/password', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify({ old_password, new_password })
    })
    return res.json()
}

// 获取购买意向
export async function getPurchaseIntents(token) {
    const res = await fetch('/api/seller/purchase-intents', {
        headers: { Authorization: `Bearer ${token}` }
    })
    return res.json()
}

// 更新意向状态
export async function updatePurchaseIntentStatus(purchase_id, new_status, token) {
    const res = await fetch('/api/seller/purchase-intents/update-status', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify({ purchase_id, new_status })
    })
    return res.json()
}

