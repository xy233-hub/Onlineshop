// src/api/product.js
import axios from 'axios'

export function getProduct() {
    return axios.get('/api/product').then(res => res.data)
}

export function purchaseProduct(data) {
    return axios.post('/api/product/purchase', data).then(res => res.data)
}
