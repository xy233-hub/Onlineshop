// javascript
import { createRouter, createWebHistory } from 'vue-router'

// 导入组件
import Home from '@/views/Home.vue'
import ProductDetail from '@/views/ProductDetail.vue'
import SellerLogin from '@/views/seller/Login.vue'
import SellerDashboard from '@/views/seller/Dashboard.vue'
import DashboardContent from '@/views/seller/DashboardContent.vue'
import ProductManagement from '@/views/seller/ProductManagement.vue'
import OrderManagement from '@/views/seller/OrderManagement.vue'
import ChangePassword from '@/views/seller/ChangePassword.vue'
import CustomerLogin from '@/views/customer/Login.vue'
import CategoryManagement from '@/views/seller/CategoryManagement.vue'
import CustomerList from '@/views/seller/CustomerList.vue'
import CustomerDetail from '@/views/seller/CustomerDetail.vue'

const routes = [
    { path: '/', name: 'Home', component: Home },
    { path: '/login', name: 'CustomerLogin', component: CustomerLogin, meta: { guestOnly: true } },
    { path: '/product/:id', name: 'ProductDetail', component: ProductDetail, props: true },
    { path: '/seller', name: 'SellerLogin', component: SellerLogin, meta: { guestOnly: true } },
    {
        path: '/dashboard',
        name: 'CustomerDashboard',
        component: () => import('@/views/customer/Dashboard.vue'),
        meta: { requiresAuth: true }
    },
    {
        path: '/seller/dashboard',
        name: 'SellerDashboard',
        component: SellerDashboard,
        meta: { requiresAuth: true },
        // 访问 /seller/dashboard 时加载子路由 path: '' （概览）
        redirect: '/seller/dashboard',
        children: [
            {
                path: '', // 关键：概览使用空路径，访问 /seller/dashboard 时会匹配
                name: 'DashboardOverview',
                component: DashboardContent,
                meta: { requiresAuth: true }
            },
            {
                path: 'products',
                name: 'SellerProducts',
                component: ProductManagement,
                meta: { requiresAuth: true }
            },
            {
                path: 'orders',
                name: 'SellerOrders',
                component: OrderManagement,
                meta: { requiresAuth: true }
            },
            {
                path: 'password',
                name: 'SellerChangePassword',
                component: ChangePassword,
                meta: { requiresAuth: true }
            },
            {
                path: 'categories',
                name: 'SellerCategories',
                component: () => import('@/views/seller/CategoryManagement.vue')
            },{
                path: 'customers',
                name: 'SellerCustomers',
                component: CustomerList,
                meta: { requiresAuth: true }
            },
            {
                path: 'customers/:id',
                name: 'SellerCustomerDetail',
                component: CustomerDetail,
                props: true,
                meta: { requiresAuth: true }
            }
        ]
    },

    // 兜底路由
    { path: '/:pathMatch(.*)*', name: 'NotFound', redirect: '/' }
]

const router = createRouter({
    history: createWebHistory(),
    routes,
    scrollBehavior(to, from, savedPosition) {
        if (savedPosition) return savedPosition
        return { top: 0 }
    }
})

const sanitizeToken = v => {
    if (typeof v !== 'string') return null
    const t = v.trim()
    if (!t || t === 'undefined' || t === 'null') return null
    return t
}

router.beforeEach((to, from, next) => {
    const sellerToken = sanitizeToken(localStorage.getItem('seller_token'))
    const customerToken = sanitizeToken(localStorage.getItem('customer_token'))

    const isSeller = !!sellerToken
    const isCustomer = !!customerToken

    if (to.path.startsWith('/seller')) {
        if (!isSeller) {
            if (to.path !== '/seller') {
                return next({ path: '/seller', query: { redirect: to.fullPath } })
            }
            return next()
        }
        return next()
    }

    if (to.meta?.requiresAuth) {
        if (!(isSeller || isCustomer)) {
            return next({ path: '/login', query: { redirect: to.fullPath } })
        }
    }

    // guestOnly：允许特定 guest 页面即使已登录也能访问（例如客户登录页）
    if (to.meta?.guestOnly) {
        // 允许卖家访问客户登录页以实现切换登录角色
        if (to.name === 'CustomerLogin') {
            return next()
        }
        // 原有行为：如果是卖家则跳到卖家仪表盘
        if (isSeller) {
            if (to.path !== '/seller/dashboard') return next({ path: '/seller/dashboard' })
            return next()
        }
        if (isCustomer) {
            if (to.path !== '/') return next({ path: '/' })
            return next()
        }
    }

    next()
})

export default router
