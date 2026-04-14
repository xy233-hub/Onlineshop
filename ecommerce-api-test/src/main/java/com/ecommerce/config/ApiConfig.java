// src/main/java/com/ecommerce/config/ApiConfig.java
package com.ecommerce.config;

public class ApiConfig {
    // 基础配置 - 使用 8081 端口
    public static final String BASE_URL = "http://123.56.161.48:8081";
    public static final int DEFAULT_TIMEOUT = 10000;
    
    // API端点
    public static class Endpoints {
        // 认证相关
        public static final String CUSTOMER_LOGIN = "/api/customers/login";
        public static final String SELLER_LOGIN = "/api/seller/login";
        public static final String CUSTOMER_REGISTER = "/api/customers/register";
        
        // 商品相关
        public static final String PRODUCTS = "/api/products";
        public static final String PRODUCT_DETAIL = "/api/products/{productId}";
        
        // 地址相关
        public static final String ADDRESSES = "/api/customers/addresses";
        public static final String ADDRESS_DETAIL = "/api/customers/addresses/{addressId}";
        public static final String ADDRESS_DEFAULT = "/api/customers/addresses/{addressId}/default";
        
        // 订单/购买意向相关
        public static final String PURCHASE_INTENTS = "/api/products/purchase-intents";
        public static final String CUSTOMER_PURCHASE_INTENTS = "/api/customers/{customerId}/purchase-intents";
        public static final String CANCEL_ORDER = "/api/customers/{customerId}/purchase-intents/{purchaseId}/cancel";
        public static final String CONFIRM_RECEIVED = "/api/customers/{customerId}/purchase-intents/{purchaseId}/confirm-received";
        
        // 购物车相关
        public static final String CART_ITEMS = "/api/customers/cart/items";
        public static final String CART_BATCH_PURCHASE = "/api/customers/cart/batch-purchase";
        public static final String CART_BATCH_CONVERT_FAVORITE = "/api/customers/cart/batch-convert-favorite";
        
        // 收藏夹相关
        public static final String FAVORITES = "/api/customers/favorites";
        public static final String FAVORITE_DETAIL = "/api/customers/favorites/{favoriteId}";
        
        // 支付相关
        public static final String PAYMENT_CREATE = "/api/payments/create";
        public static final String PAYMENT_PAY = "/api/payments/{paymentId}/pay";
        public static final String PAYMENT_VERIFY = "/api/payments/verify";
        public static final String PAYMENT_CUSTOMER = "/api/payments/customer";
        public static final String PAYMENT_BY_PURCHASE = "/api/payments/purchase/{purchaseId}";
        public static final String PAYMENT_REFUND_APPLY = "/api/payments/{paymentId}/refund/apply";
        
        // 物流相关
        public static final String ORDER_LOGISTICS = "/api/customers/orders/{purchaseId}/logistics";
        public static final String LOGISTICS_PROVIDERS = "/api/logistics/providers";
        
        
        // 售后相关
        public static final String AFTER_SALES = "/api/customers/after-sales";
        public static final String AFTER_SALE_DETAIL = "/api/customers/after-sales/{serviceId}";
        public static final String AFTER_SALE_CANCEL = "/api/customers/after-sales/{serviceId}/cancel";
        public static final String AFTER_SALE_RETURN_SHIP = "/api/customers/after-sales/{serviceId}/return-ship";
        
        // 卖家端
        public static final String SELLER_PRODUCTS = "/api/seller/products";
        public static final String SELLER_PRODUCT_FREEZE = "/api/seller/products/{productId}/freeze";
        public static final String SELLER_PRODUCT_UNFREEZE = "/api/seller/products/{productId}/unfreeze";
        public static final String SELLER_PRODUCT_MARK_SOLD = "/api/seller/products/{productId}/mark-sold";
        public static final String SELLER_PURCHASE_INTENTS = "/api/seller/purchase-intents";
        public static final String SELLER_PURCHASE_INTENT_STATUS = "/api/seller/purchase-intents/{purchaseId}/status";
        public static final String SELLER_CUSTOMERS = "/api/seller/customers";
        public static final String SELLER_CUSTOMER_PURCHASE_HISTORY = "/api/seller/customers/{customerId}/purchase-history";
        public static final String SELLER_PASSWORD = "/api/seller/password";
        public static final String SELLER_SHIP_ORDER = "/api/seller/purchase-intents/{purchaseId}/ship";
        public static final String SELLER_AFTER_SALES = "/api/seller/after-sales";
        public static final String SELLER_AFTER_SALE_HANDLE = "/api/seller/after-sales/{serviceId}/handle";
        public static final String SELLER_AFTER_SALE_CONFIRM_RETURN = "/api/seller/after-sales/{serviceId}/confirm-return";
        public static final String SELLER_ADD_LOGISTICS_TRACK = "/api/seller/orders/{purchaseId}/logistics/tracks";
        
        // 分类相关
        public static final String CATEGORIES = "/api/categories";
        public static final String CATEGORY_DETAIL = "/api/categories/{categoryId}";
        public static final String SELLER_CATEGORIES = "/api/seller/categories";
        public static final String SELLER_CATEGORIES_TREE = "/api/seller/categories/tree";
        
        // 媒体上传
        public static final String MEDIA_UPLOAD = "/api/media/upload";
        
        // 卖家支付相关
        public static final String SELLER_PAYMENT_REFUND = "/api/seller/payments/{paymentId}/refund";
        public static final String SELLER_PAYMENTS_EXPIRING = "/api/seller/payments/expiring";
    }
    
    // 响应状态码
    public static class StatusCode {
        public static final int OK = 200;
        public static final int CREATED = 201;
        public static final int BAD_REQUEST = 400;
        public static final int UNAUTHORIZED = 401;
        public static final int NOT_FOUND = 404;
        public static final int INTERNAL_ERROR = 500;
    }
}