<!-- File: src/views/customer/Dashboard.vue -->
<template>
  <div class="customer-dashboard-page">
    <div class="top-bar">
      <el-button type="primary" @click="goHome">返回商城首页</el-button>
    </div>
    <el-container>
      <el-aside width="220px" class="side">
        <div class="side-title">我的中心</div>
        <el-menu :default-active="activeKey" router class="side-menu" @select="onSelect">
          <el-menu-item index="/customer/dashboard/orders">历史下单</el-menu-item>
          <el-menu-item index="/customer/dashboard/favorites">我的收藏</el-menu-item>
          <el-menu-item index="/customer/dashboard/cart">我的购物车</el-menu-item>
          <el-menu-item index="/customer/dashboard/after-sales">售后申请</el-menu-item>
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

const route = useRoute()
const router = useRouter()

const activeKey = computed(() => {
  // 侧栏高亮基于当前路径
  const p = route.path
  if (p.startsWith('/dashboard/favorites')) return '/dashboard/favorites'
  if (p.startsWith('/dashboard/cart')) return '/dashboard/cart'
  if (p.startsWith('/dashboard/after-sales')) return '/dashboard/after-sales'
  return '/dashboard/orders'
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

/* 顶部工具栏样式，可按需调整 */
.top-bar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}
</style>
