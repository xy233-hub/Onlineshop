<!-- File: src/views/customer/Dashboard.vue -->
<template>
  <div class="customer-dashboard-page">
    <div class="top-bar">
      <div class="user-info">
        <span class="user-id">用户ID: {{ customerInfo?.customer_id || '未登录' }}</span>
        <span class="user-name">登录名: {{ customerInfo?.username || '未登录' }}</span>
      </div>
      <el-button type="primary" @click="goHome">返回商城首页</el-button>
    </div>
    <el-container>
      <el-aside width="220px" class="side">
        <div class="side-title">我的中心</div>
        <el-menu :default-active="activeKey" router class="side-menu" @select="onSelect">
          <el-menu-item index="/customer/dashboard/info">个人信息</el-menu-item>
          <el-menu-item index="/customer/dashboard/orders">历史下单</el-menu-item>
          <el-menu-item index="/customer/dashboard/favorites">我的收藏</el-menu-item>
          <el-menu-item index="/customer/dashboard/cart">我的购物车</el-menu-item>
          <el-menu-item index="/customer/dashboard/after-sales">售后申请</el-menu-item>
          <el-menu-item index="/customer/dashboard/addresses">地址管理</el-menu-item>
          <el-menu-item index="/customer/dashboard/products">商品管理</el-menu-item>
          <el-menu-item index="/customer/dashboard/purchase-intents">购买意向记录</el-menu-item>
          <el-menu-item index="/customer/dashboard/after-sales-management">售后管理</el-menu-item>
        </el-menu>
      </el-aside>

      <el-main class="content">
        <router-view />
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCustomerStore } from '@/stores/customer'

const route = useRoute()
const router = useRouter()
const customerStore = useCustomerStore()

const customerInfo = computed(() => {
  // 优先从 store 获取用户信息
  if (customerStore.customer) {
    return customerStore.customer
  }
  // 如果 store 中没有，从 localStorage 获取
  try {
    const raw = localStorage.getItem('customer_info')
    if (raw) {
      return JSON.parse(raw)
    }
  } catch (e) {
    console.error('解析用户信息失败:', e)
  }
  return null
})

const activeKey = computed(() => {
  // 侧栏高亮基于当前路径
  const p = route.path
  if (p.startsWith('/dashboard/info')) return '/dashboard/info'
  if (p.startsWith('/dashboard/favorites')) return '/dashboard/favorites'
  if (p.startsWith('/dashboard/cart')) return '/dashboard/cart'
  if (p.startsWith('/dashboard/after-sales')) return '/dashboard/after-sales'
  if (p.startsWith('/dashboard/products')) return '/dashboard/products'
  if (p.startsWith('/dashboard/purchase-intents')) return '/dashboard/purchase-intents'
  if (p.startsWith('/dashboard/after-sales-management')) return '/dashboard/after-sales-management'
  return '/dashboard/info'
})

const onSelect = (index) => {
  router.push(index).catch(()=>{})
}
const goHome = () => {
  router.push('/')
}
</script>

<style scoped>
.customer-dashboard-page { padding: 16px; }
.side { background: #fff; border-right: 1px solid #eef2f7; padding-top: 8px; }
.side-title { font-weight:700; padding: 16px; color:#409eff; }
.content { background: #fff; padding: 20px; min-height: 480px; }
.side-menu { border-right: none; }

/* 移除菜单激活状态的光标显示 */
.side-menu :deep(.el-menu-item.is-active) {
  background-color: #ecf5ff !important;
  color: #409eff !important;
}

.side-menu :deep(.el-menu-item.is-active::before) {
  display: none !important;
}

/* 顶部工具栏样式，可按需调整 */
.top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

/* 用户信息样式 */
.user-info {
  display: flex;
  gap: 20px;
  align-items: center;
  font-size: 14px;
  color: #333;
}

.user-id {
  font-weight: 500;
}

.user-name {
  font-weight: 500;
  color: #409eff;
}
</style>
