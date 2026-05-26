// javascript
import axios from 'axios'

const baseURL = import.meta.env.VITE_API_BASE_URL

const api = axios.create({
    baseURL: baseURL,
    timeout: 30000
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

    const explicitRole = (config.headers && (config.headers['X-Auth-Role'] || config.headers['x-auth-role'])) || null

        const url = (config.url || '')
    const path = url.startsWith('http') ? new URL(url).pathname : url
    
    // 特殊处理：/seller/purchase-intents 允许买家调用（查看别人购买自己商品的意向）
    const isSellerPurchaseIntentsPath = path === '/seller/purchase-intents'
    // 特殊处理：/seller/purchase-intents/{id}/status 也允许买家调用（更新备货状态等）
    const isSellerPurchaseIntentStatusPath = /^\/seller\/purchase-intents\/\d+\/status$/.test(path)
    // 特殊处理：/seller/purchase-intents/{id}/ship 也允许买家调用（买家发货）
    const isSellerPurchaseIntentShipPath = /^\/seller\/purchase-intents\/\d+\/ship$/.test(path)
    // 特殊处理：/seller/after-sales 相关接口允许买家调用（处理售后）
    const isSellerAfterSalesPath = /^\/seller\/after-sales(\/.*)?$/.test(path)
    // 特殊处理：/seller/orders/{id}/logistics/tracks 允许买家调用（买家添加物流轨迹）
    const isSellerOrderLogisticsTrackPath = /^\/seller\/orders\/\d+\/logistics\/tracks$/.test(path)
    // 特殊处理：/seller/products 允许买家调用（买家发布商品）
    const isSellerProductsPath = /^\/seller\/products(\/.*)?$/.test(path)
    // 特殊处理：/seller/{seller_id} 允许匿名调用（获取卖家信息）
    const isSellerInfoPath = /^\/seller\/\d+$/.test(path)
    
    const inferredRole = (() => {
        if (/^\/seller(\/|$)/.test(path) && !isSellerPurchaseIntentsPath && !isSellerPurchaseIntentStatusPath && !isSellerPurchaseIntentShipPath && !isSellerAfterSalesPath && !isSellerOrderLogisticsTrackPath && !isSellerProductsPath && !isSellerInfoPath) return 'seller'
        if (/^\/admin(\/|$)/.test(path)) return 'seller'
        // **删除：地址相关的请求不需要 JWT 令牌**
        // if (/^\/customers\/addresses(\/|$)/.test(path)) return null
        if (/^\/customers?(\/|$)/.test(path)) return 'customer'
        if (/^\/products\/purchase-intents(\/|$)/.test(path)) return 'customer'
        if (/^\/payments(\/|$)/.test(path)) return 'customer'
        if (/^\/products(\/|$)/.test(path)) return null
        return null
    })()

    let tokenToUse = null
    if (explicitRole === 'seller') tokenToUse = sellerToken
    else if (explicitRole === 'customer') tokenToUse = customerToken
    else if (inferredRole === 'seller') tokenToUse = sellerToken
    else if (inferredRole === 'customer') tokenToUse = customerToken
    else tokenToUse = sellerToken || customerToken
    
    // 特殊处理：如果是 /seller/purchase-intents、/seller/after-sales、/seller/orders/{id}/logistics/tracks 或 /seller/products 相关接口且没有 seller_token，则使用 customer_token
    if ((isSellerPurchaseIntentsPath || isSellerPurchaseIntentStatusPath || isSellerPurchaseIntentShipPath || isSellerAfterSalesPath || isSellerOrderLogisticsTrackPath || isSellerProductsPath) && !sellerToken && customerToken) {
        tokenToUse = customerToken
    }
    if (tokenToUse) {
        config.headers = config.headers || {}
        config.headers.Authorization = `Bearer ${tokenToUse}`
    }

    if (config.headers) {
        delete config.headers['X-Auth-Role']
        delete config.headers['x-auth-role']
    }

    return config
}, error => Promise.reject(error))

api.interceptors.response.use(
    response => response,
    error => {
        const status = error?.response?.status
        const url = error?.config?.url || ''

        const path = url.startsWith('http') ? new URL(url).pathname : url

        const isLoginApi =
            /^\/seller\/login(\/|$)/.test(path) ||
            /^\/customers\/login(\/|$)/.test(path)

        if (status === 401 && !isLoginApi) {
            const isSellerPath = /^\/(seller|admin)(\/|$)/.test(path)
            const isCustomerPath = /^\/customers?(\/|$)/.test(path) ||
                                   /^\/payments(\/|$)/.test(path) ||
                                   /^\/products\/purchase-intents(\/|$)/.test(path)

            if (isSellerPath) {
                localStorage.removeItem('seller_token')
                localStorage.removeItem('seller_info')
                try { window.location.href = '/seller' } catch (e) {}
            } else if (isCustomerPath) {
                localStorage.removeItem('customer_token')
                localStorage.removeItem('customer_info')
                localStorage.removeItem('customer')
                localStorage.removeItem('customer_id')
            } else {
                const usedToken = error?.config?.headers?.Authorization
                if (usedToken) {
                    const sellerToken = sanitizeToken(localStorage.getItem('seller_token'))
                    if (sellerToken && usedToken.includes(sellerToken)) {
                        localStorage.removeItem('seller_token')
                        localStorage.removeItem('seller_info')
                        try { window.location.href = '/seller' } catch (e) {}
                    } else {
                        localStorage.removeItem('customer_token')
                        localStorage.removeItem('customer_info')
                        localStorage.removeItem('customer')
                        localStorage.removeItem('customer_id')
                    }
                }
            }
        }

        return Promise.reject(error)
    }
)

/**
 * 公共商品接口（文档：GET /api/products, GET /api/products/{product_id}）
 */
export const productAPI = {
    getProducts: (params) => api.get('/products', { params }),
    getProductDetail: (id) => api.get(`/products/${id}`)
}

/**
 * AI 选品助手
 * - POST /api/products/ai-recommend
 */
export const aiAPI = {
    recommend: (data) => api.post('/products/ai-recommend', data),
    searchImage: (data) => api.post('/products/ai-image-search', data),
    getImageSearchStatus: () => api.get('/products/ai-image-search/status'),
    generateVectors: () => api.post('/products/ai-image-search/generate-vectors')
}

/**
 * 聊天会话管理
 */
export const chatAPI = {
    createSession: (userId, name) => api.post('/chat/sessions', null, { params: { userId, name } }),
    getUserSessions: (userId) => api.get('/chat/sessions', { params: { userId } }),
    getSession: (sessionId) => api.get(`/chat/sessions/${sessionId}`),
    getSessionMessages: (sessionId) => api.get(`/chat/sessions/${sessionId}/messages`),
    renameSession: (sessionId, userId, name) => api.put(`/chat/sessions/${sessionId}`, null, { params: { userId, name } }),
    renameSessionWithAi: (sessionId, userId) => api.post(`/chat/sessions/${sessionId}/rename-with-ai`, null, { params: { userId } }),
    deleteSession: (sessionId, userId) => api.delete(`/chat/sessions/${sessionId}`, { params: { userId } })
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
    updatePrice: (productId, payload) => api.put(`/seller/products/${productId}/price`, payload),
    updateProduct: (productId, data) => api.put(`/seller/products/${productId}`, data)
}

/**
 * 促销管理（后端已实现 69-77）
 */
export const promotionAPI = {
    createPromotion: (data) => api.post('/admin/promotions', data),
    getPromotions: (params) => api.get('/admin/promotions', { params }),
    getPromotionDetail: (promotionId) => api.get(`/admin/promotions/${promotionId}`),
    updatePromotion: (promotionId, data) => api.put(`/admin/promotions/${promotionId}`, data),
    activatePromotion: (promotionId) => api.post(`/admin/promotions/${promotionId}/activate`, { confirm: true }),
    endPromotion: (promotionId, data = {}) => api.post(`/admin/promotions/${promotionId}/end`, data),
    cancelPromotion: (promotionId, reason) => api.post(`/admin/promotions/${promotionId}/cancel`, { reason }),
    getRuleDefinitions: () => api.get('/admin/promotions/rule-definitions'),
    getActivePromotions: (params) => api.get('/promotions/active', { params }),
    getProductPromotions: (productId) => api.get(`/products/${productId}/promotions`)
}

export const priceHistoryAPI = {
    getPriceHistory: (productId) => api.get(`/products/${productId}/price-history`)
}

export const sellerProductAIAPI = {
    // AI 生成可能超过默认 10s，这里单独放宽超时。
    generateDescription: (data) => api.post('/seller/products/ai/description', data, { timeout: 120000 }),
    estimatePrice: (data) => api.post('/seller/products/ai/price-estimate', data, { timeout: 120000 })
}
/**
 * 买家商品管理接口
 */
export const customerProductAPI = {
    getMyProducts: (params) => api.get('/customers/products/my-products', { params }),
    createProduct: (data) => {
        // 买家发布商品时需要显式指定使用 customer 角色
        return api.post('/seller/products', data, {
            headers: { 'X-Auth-Role': 'customer' }
        })
    },
    freezeProduct: (productId, payload = {}) => api.put(`/seller/products/${productId}/freeze`, payload),
    unfreezeProduct: (productId, payload = {}) => api.put(`/seller/products/${productId}/unfreeze`, payload),
    markSold: (productId, payload = {}) => api.put(`/seller/products/${productId}/mark-sold`, payload),
    updatePrice: (productId, payload) => api.put(`/seller/products/${productId}/price`, payload)
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
 * 购物车相关接口（已按接口文档统一为 /customers/cart/items）
 * - 获取购物车商品：GET /api/customers/cart/items
 * - 添加商品到购物车（单）：POST /api/customers/cart/items
 * - 更新购物车商品数量：PUT /api/customers/cart/items/{cart_item_id}
 * - 从购物车移除商品：DELETE /api/customers/cart/items/{cart_item_id}
 * - 批量移除购物车商品：DELETE /api/customers/cart/items （请求体包含 customer_id 与 cart_item_ids）
 */
export const cartAPI = {
    // 现保持同一路径，后端已改为与收藏同体：{ customer_id, product_id }
    addToCart: (data) => api.post('/customers/cart/items', data),
    getCartItems: (params) => api.get('/customers/cart/items', { params }),
    updateCartItem: (cartItemId, data) => api.put(`/customers/cart/items/${cartItemId}`, data),
    removeCartItem: (cartItemId) => api.delete(`/customers/cart/items/${cartItemId}`),
    removeCartItems: (payload) => api.delete('/customers/cart/items', { data: payload }),
    batchPurchasePreview: (payload) => api.post('/customers/cart/batch-purchase-preview', payload),
    batchPurchase: (payload) => api.post('/customers/cart/batch-purchase', payload),
    batchConvertToFavorites: (payload) => api.post('/customers/cart/batch-convert-favorite', payload)
}
/**
 * 收藏夹相关接口（与接口文档一致）
 * - 获取收藏商品：GET /api/customers/favorites  (支持 params: customer_id, page, size, sort_by, order)
 * - 添加商品到收藏：POST /api/customers/favorites
 * - 从收藏移除商品：DELETE /api/customers/favorites/{favorite_id}
 */
export const favoritesAPI = {
    getFavorites: (params) => api.get('/customers/favorites', { params }),
    addToFavorites: (data) => api.post('/customers/favorites', data),
    removeFavorite: (favoriteId, params) => api.delete(`/customers/favorites/${favoriteId}`, { params })
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

/**
 * 卖家相关接口
 */
export const sellerAPI = {
    // 根据卖家ID获取卖家信息
    getSellerById: (sellerId) => {
        console.log('调用sellerAPI.getSellerById，sellerId:', sellerId)
        console.log('API路径:', `/sellers/${sellerId}`)
        return api.get(`/sellers/${sellerId}`)
    },
    // 根据商品ID获取商品发布者信息
    getSellerByProductId: (productId) => {
        console.log('调用sellerAPI.getSellerByProductId，productId:', productId)
        console.log('API路径:', `/sellers/product/${productId}`)
        return api.get(`/sellers/product/${productId}`)
    }
}

/**
 * 物流（46-49）
 */
export const logisticsAPI = {
    getProviders: params => api.get('/logistics/providers', { params }),

    // 47. 卖家发货
    shipOrder: (purchaseId, data) =>
        api.post(`/seller/purchase-intents/${purchaseId}/ship`, data),

    // 48. 客户查询订单物流轨迹
    getOrderLogistics: purchaseId => api.get(`/customer/orders/${purchaseId}/logistics`),

    // 49. 卖家手动添加物流轨迹
    addLogisticsTrack: (purchaseId, data) =>
        api.post(`/seller/orders/${purchaseId}/logistics/tracks`, data)
}


/**
 * 售后相关接口
 */
export const afterSalesAPI = {
    // 客户提交售后申请
    createAfterSales: (data) => api.post('/customers/after-sales', data),
    // 客户查询售后列表
    getCustomerAfterSales: (params) => {
      console.log('调用getCustomerAfterSales，参数:', params);
      return api.get('/customers/after-sales', { params })
        .then(response => {
          console.log('getCustomerAfterSales响应:', response);
          return response;
        })
        .catch(error => {
          console.error('getCustomerAfterSales错误:', error);
          console.error('错误响应:', error.response);
          throw error;
        });
    },
    // 客户查询售后详情
    getAfterSalesDetail: (serviceId) => api.get(`/customers/after-sales/${serviceId}`),
    // 客户取消售后
    cancelAfterSales: (serviceId, data) => api.post(`/customers/after-sales/${serviceId}/cancel`, data),
    // 客户填写退货物流信息
    returnShip: (serviceId, data) => api.post(`/customers/after-sales/${serviceId}/return-ship`, data),
    // 卖家查询售后列表
    getSellerAfterSales: (params) => api.get('/seller/after-sales', { params }),
    // 卖家查询售后详情
    getSellerAfterSalesDetail: (serviceId) => api.get(`/seller/after-sales/${serviceId}`),
    // 卖家处理售后
    handleAfterSales: (serviceId, data) => api.post(`/seller/after-sales/${serviceId}/handle`, data),
    // 卖家确认收货
    confirmReturn: (serviceId, data) => api.post(`/seller/after-sales/${serviceId}/confirm-return`, data)
}

/**
 * 支付相关接口
 */
export const paymentAPI = {
    createPayment: (data) => api.post('/payments/create', data),
    
    paymentSuccess: (paymentId, data) => api.post(`/payments/${paymentId}/success`, null, {
        params: {
            transactionId: data.transactionId,
            paymentMethod: data.paymentMethod
        }
    }),
    
    selectPaymentMethod: (paymentId, data) => api.post(`/payments/${paymentId}/pay`, data),
    
    paymentFailure: (paymentId, data) => api.post(`/payments/${paymentId}/failure`, data),
    
    verifyPayment: (data) => api.post('/payments/verify', data),
    
    getCustomerPayments: () => api.get('/payments/customer'),
    
    getPaymentByPurchaseId: (purchaseId) => api.get(`/payments/purchase/${purchaseId}`),

    alipayReturn: (params) => api.get('/payments/alipay/return', { params }),

    // Keep original signed query as-is to avoid signature mismatch after re-encoding.
    alipayReturnRaw: (rawQuery = '') => api.get(`/payments/alipay/return${rawQuery}`)
}

export default api