import { createRouter, createWebHistory } from 'vue-router'
import Home from '@/views/Home.vue'
const routes = [
    {
        path: '/',
        component: () => import('@/views/Home.vue') // 商品展示页
    },
    {
        path: '/seller/login',
        component: () => import('@/views/SellerLogin.vue')
    },
    {
        path: '/seller/dashboard',
        component: () => import('@/views/SellerDashboard.vue')
    },
    {
        path: '/seller/products',
        component: () => import('@/views/SellerProducts.vue') // 卖家商品管理
    },
    {
        path: '/seller/account',
        component: () => import('@/views/SellerAccount.vue') // 卖家账户
    },
    {
        path: '/seller/purchase-intents',
        component: () => import('@/views/SellerPurchaseIntents.vue') // 购买意向管理
    }
    // 其他路由...
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

export default router
