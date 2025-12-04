// javascript
import axios from 'axios'

const api = axios.create({
    baseURL: 'http://localhost:8080/api',
    timeout: 10000
})

const sanitizeToken = v => {
    if (typeof v !== 'string') return null
    const t = v.trim()
    if (!t || t === 'undefined' || t === 'null') return null
    return t
}

api.interceptors.request.use(config => {
    const sellerToken = sanitizeToken(localStorage.getItem('seller_token'))
    const customerToken = sanitizeToken(localStorage.getItem('customer_token'))

    // 1. 优先查看是否由调用方显式指定角色（例如：{ headers: { 'X-Auth-Role': 'seller' } }）
    const explicitRole = (config.headers && (config.headers['X-Auth-Role'] || config.headers['x-auth-role'])) || null

    // 2. 根据 URL 推断角色（路径以 /seller 开头则使用 seller token）
    const url = (config.url || '')
    const path = url.startsWith('http') ? new URL(url).pathname : url
    const inferredRole = (() => {
        if (/^\/seller(\/|$)/.test(path)) return 'seller'
        if (/^\/customers?(\/|$)/.test(path)) return 'customer'
        // 特殊接口：客户端提交购买意向使用 customer（示例）
        if (/^\/products\/purchase-intents(\/|$)/.test(path)) return 'customer'
        // 其它情况不强制，可回退
        return null
    })()

    let tokenToUse = null
    if (explicitRole === 'seller') tokenToUse = sellerToken
    else if (explicitRole === 'customer') tokenToUse = customerToken
    else if (inferredRole === 'seller') tokenToUse = sellerToken
    else if (inferredRole === 'customer') tokenToUse = customerToken
    else tokenToUse = sellerToken || customerToken // 兼容旧逻辑

    if (tokenToUse) {
        config.headers = config.headers || {}
        config.headers.Authorization = `Bearer ${tokenToUse}`
    }

    // 清理自定义头，避免泄露给后端（如不想传递可注释掉）
    if (config.headers) {
        delete config.headers['X-Auth-Role']
        delete config.headers['x-auth-role']
    }

    return config
}, error => Promise.reject(error))

// 响应拦截器保留原样...
api.interceptors.response.use(response => response, error => {
    if (error.response && error.response.status === 401) {
        localStorage.removeItem('seller_token')
        localStorage.removeItem('customer_token')
        try { window.location.href = '/seller' } catch (e) {}
        return Promise.reject(error)
    }
    return Promise.reject(error)
})

/**
 * 公共商品接口（文档：GET /api/products, GET /api/products/{product_id}）
 */
export const productAPI = {
    getProducts: (params) => api.get('/products', { params }),
    getProductDetail: (id) => api.get(`/products/${id}`)
}

/**
 * 卖家商品管理（文档：POST /api/seller/products,
 * GET /api/seller/products,
 * PUT /api/seller/products/{product_id}/freeze,
 * PUT /api/seller/products/{product_id}/unfreeze,
 * PUT /api/seller/products/{product_id}/mark-sold）
 */
export const sellerProductAPI = {
    createProduct: (data) => api.post('/seller/products', data),
    getProducts: (params) => api.get('/seller/products', { params }),
    freezeProduct: (productId, payload = {}) => api.put(`/seller/products/${productId}/freeze`, payload),
    unfreezeProduct: (productId, payload = {}) => api.put(`/seller/products/${productId}/unfreeze`, payload),
    markSold: (productId, payload = {}) => api.put(`/seller/products/${productId}/mark-sold`, payload),
    updateProduct: (productId, data) => api.put(`/seller/products/${productId}`, data)
}

/**
 * 购买意向（客户/卖家相关）
 * - 客户提交购买意向：POST /api/products/purchase-intents   （按文档原样）
 * - 查询客户自己的购买意向：GET /api/customers/{customer_id}/purchase-intents
 * - 卖家查询收到的购买意向：GET /api/seller/purchase-intents
 * - 卖家修改购买意向状态：PUT /api/seller/purchase-intents/{purchase_id}/status
 */
export const purchaseAPI = {
    createPurchaseIntent: (data) => api.post('/products/purchase-intents', data),
    getCustomerPurchaseIntents: (customerId, params) => api.get(`/customers/${customerId}/purchase-intents`, { params }),
    getSellerPurchaseIntents: (params) => api.get('/seller/purchase-intents', { params }),
    updatePurchaseIntentStatus: (purchaseId, payload) => api.put(`/seller/purchase-intents/${purchaseId}/status`, payload),
    // 客户取消订单
    customerCancelOrder: (customerId, purchaseId, payload) => api.post(`/customers/${customerId}/purchase-intents/${purchaseId}/cancel`, payload),
    // 客户确认收货
    customerConfirmReceived: (customerId, purchaseId) => api.post(`/customers/${customerId}/purchase-intents/${purchaseId}/confirm-received`)
}

/**
 * 客户相关认证（文档：POST /api/customers/register, POST /api/customers/login）
 */
export const customerAuthAPI = {
    register: (data) => api.post('/customers/register', data),
    login: (data) => api.post('/customers/login', data)
}

/**
 * 卖家认证与管理（文档：POST /api/seller/login, PUT /api/seller/password）
 */
export const authAPI = {
    // 卖家登录
    login: (data) => api.post('/seller/login', data),

    // 修改密码
    changePassword: (data) => api.put('/seller/password', data)
}
/**
 * 媒体上传（文档：POST /api/media/upload multipart/form-data）
 */
export const mediaAPI = {
    upload: (formData) => api.post('/media/upload', formData)
}

/**
 * 类别管理
 * - 公开：GET /api/categories, GET /api/categories/{category_id}
 * - 卖家管理：GET /api/seller/categories, GET /api/seller/categories/tree,
 *   POST /api/seller/categories, PUT /api/seller/categories/{category_id},
 *   DELETE /api/seller/categories/{category_id}, PATCH /api/seller/categories/{category_id}/move
 */
export const categoryAPI = {
    // 公开
    getCategories: (params) => api.get('/categories', { params }),
    getCategory: (id) => api.get(`/categories/${id}`),

    // 卖家管理
    getSellerCategories: (params) => api.get('/seller/categories', { params }),
    getSellerCategoryTree: () => api.get('/seller/categories/tree'),
    createCategory: (data) => api.post('/seller/categories', data),
    updateCategory: (id, data) => api.put(`/seller/categories/${id}`, data),
    deleteCategory: (id) => api.delete(`/seller/categories/${id}`),
    moveCategory: (id, payload) => api.patch(`/seller/categories/${id}/move`, payload)
}

export const sellerCustomerAPI = {
    // 现有方法...
    getCustomers: (params) => api.get('/seller/customers', { params }),

    // 新增：卖家查看某顾客的购买历史（分页/筛选）
    // GET /api/seller/customers/{customer_id}/purchase-history
    getCustomerPurchaseHistory: (customerId, params) =>
        api.get(`/seller/customers/${customerId}/purchase-history`, { params })
};
/**
 * 仪表盘
 */
export const dashboardAPI = {
    getStats: () => api.get('/dashboard/stats')
}

export default api