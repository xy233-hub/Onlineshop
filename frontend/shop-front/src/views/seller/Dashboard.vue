<template>
  <div class="dashboard-container">
    <el-container>
      <!-- 侧边栏 -->
      <el-aside width="200px">
        <div class="sidebar-header">
          <h3>卖家后台</h3>
        </div>
        <el-menu
            router
            :default-active="activeMenu"
            class="sidebar-menu"
            unique-opened
        >
          <el-menu-item index="/seller/dashboard">
            <el-icon><House /></el-icon>
            <span>首页</span>
          </el-menu-item>

           <el-sub-menu index="product-group">
            <template #title>
              <el-icon><Box /></el-icon>
              <span>商品相关</span>
            </template>
            <el-menu-item index="/seller/dashboard/products">
              <el-icon><Goods /></el-icon>
              <span>商品管理</span>
            </el-menu-item>
            <el-menu-item index="/seller/dashboard/categories">
              <el-icon><Folder /></el-icon>
              <span>分类管理</span>
            </el-menu-item>
          </el-sub-menu>

           <el-sub-menu index="trade-group">
            <template #title>
              <el-icon><ShoppingCart /></el-icon>
              <span>交易相关</span>
            </template>
            <el-menu-item index="/seller/dashboard/orders">
              <el-icon><Tickets /></el-icon>
              <span>购买意向</span>
            </el-menu-item>
            <el-menu-item index="/seller/dashboard/after-sales">
              <el-icon><Service /></el-icon>
              <span>售后管理</span>
            </el-menu-item>
          </el-sub-menu>

            <el-sub-menu index="marketing-group">
            <template #title>
              <el-icon><Promotion /></el-icon>
              <span>营销相关</span>
            </template>
            <el-menu-item index="/seller/dashboard/promotions">
              <el-icon><PriceTag /></el-icon>
              <span>优惠营销管理</span>
            </el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="settings-group">
            <template #title>
              <el-icon><User /></el-icon>
              <span>个人设置</span>
            </template>
            <el-menu-item index="/seller/dashboard/password">
              <el-icon><Lock /></el-icon>
              <span>修改密码</span>
            </el-menu-item>
          </el-sub-menu>
        </el-menu>

      </el-aside>

      <!-- 主要内容 -->
      <el-container>
        <el-header class="header">
          <div class="header-content">
            <span>卖家后台管理系统</span>
            <div class="header-actions">
              <el-button
                  type="primary"
                  text
                  @click="$router.push('/')"
                  class="view-store-btn"
              >
                <el-icon><View /></el-icon>
                查看店铺
              </el-button>
              <span class="welcome">欢迎，{{ sellerStore.seller?.username }}</span>
              <el-button @click="handleLogout" size="small" class="logout-btn">退出登录</el-button>
            </div>
          </div>
        </el-header>

        <el-main class="main-content">
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSellerStore } from '@/stores/seller'
import { ElMessageBox } from 'element-plus'
import { House, Goods, Tickets, Lock, Folder, Box, ShoppingCart, Service, PriceTag, User, View, Promotion } from '@element-plus/icons-vue'


const route = useRoute()
const router = useRouter()
const sellerStore = useSellerStore()

const activeMenu = computed(() => route.path)

const handleLogout = async () => {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    sellerStore.logout()
    router.push('/')
  } catch (error) {
    // 取消
  }
}
</script>

<style scoped>
.dashboard-container {
  min-height: 100vh;
  display: flex;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  position: relative;
}

.dashboard-container::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: 
    radial-gradient(circle at 20% 30%, rgba(59, 130, 246, 0.1) 0%, transparent 50%),
    radial-gradient(circle at 80% 70%, rgba(139, 92, 246, 0.1) 0%, transparent 50%);
  pointer-events: none;
  z-index: 0;
}

.sidebar-header {
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(224, 230, 237, 0.8);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.sidebar-header h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  letter-spacing: -0.02em;
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.sidebar-menu {
  border: none;
  height: calc(100vh - 80px);
}

.header {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(224, 230, 237, 0.8);
  display: flex;
  align-items: center;
  padding: 0 32px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  height: 80px;
}

.header-content {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-content > span {
  font-size: 20px;
  font-weight: 700;
  color: #1e293b;
  letter-spacing: -0.02em;
  background: linear-gradient(135deg, #1e293b, #475569);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 20px;
}

.welcome {
  color: #64748b;
  font-size: 14px;
  font-weight: 500;
}

.view-store-btn {
  margin-right: 10px;
  color: #3b82f6;
  border-color: #93c5fd;
  padding: 8px 20px;
  border-radius: 8px;
  font-weight: 500;
  background: white;
  transition: all 0.2s ease;
}

.view-store-btn:hover {
  background: #3b82f6;
  color: #fff;
  border-color: #3b82f6;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.2);
}

.main-content {
  background: transparent;
  padding: 24px 32px;
  overflow-y: auto;
  flex: 1;
  position: relative;
  z-index: 1;
}

.el-aside {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  width: 260px;
  box-shadow: 4px 0 20px rgba(0, 0, 0, 0.1);
  border-right: 1px solid rgba(224, 230, 237, 0.8);
}

:deep(.el-menu) {
  background: transparent;
  border: none;
}

:deep(.el-menu-item) {
  color: #64748b;
  padding: 14px 24px;
  margin: 6px 12px;
  border-radius: 12px;
  transition: all 0.3s ease;
  font-size: 14px;
  font-weight: 500;
  background: #f8fafc;
  border: 1px solid transparent;
}

:deep(.el-menu-item:hover) {
  background: #f1f5f9;
  color: #3b82f6;
  transform: translateX(4px);
  border-color: #e2e8f0;
}

:deep(.el-menu-item.is-active) {
  color: #fff;
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  box-shadow: 0 4px 16px rgba(59, 130, 246, 0.3);
  border-color: transparent;
}

:deep(.el-menu-item.is-active:hover) {
  transform: translateX(0);
  background: linear-gradient(135deg, #2563eb, #7c3aed);
}

:deep(.el-menu-item i) {
  margin-right: 12px;
  font-size: 16px;
}

:deep(.el-sub-menu .el-sub-menu__title) {
  color: #475569;
  padding: 14px 24px;
  margin: 8px 12px;
  border-radius: 12px;
  transition: all 0.3s ease;
  font-size: 14px;
  font-weight: 600;
  background: transparent;
  border: 1px solid transparent;
}

:deep(.el-sub-menu .el-sub-menu__title:hover) {
  background: #f8fafc;
  color: #3b82f6;
  border-color: #e2e8f0;
}

:deep(.el-sub-menu.is-active .el-sub-menu__title) {
  color: #3b82f6;
  background: rgba(59, 130, 246, 0.1);
}

:deep(.el-sub-menu .el-sub-menu__icon-arrow) {
  color: #94a3b8;
  font-size: 14px;
}

:deep(.el-sub-menu.is-active .el-sub-menu__icon-arrow) {
  color: #3b82f6;
}

:deep(.el-menu--popup) {
  background: rgba(255, 255, 255, 0.98);
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(224, 230, 237, 0.8);
}

:deep(.el-menu--popup .el-menu-item) {
  background: transparent;
  border: none;
  margin: 4px 8px;
  border-radius: 10px;
}

:deep(.el-menu--popup .el-menu-item:hover) {
  background: #f1f5f9;
  color: #3b82f6;
}

.logout-btn {
  background: linear-gradient(135deg, #ef4444, #f97316);
  border-color: transparent;
  color: #fff;
  border-radius: 8px;
  padding: 8px 18px;
  font-weight: 500;
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.3);
  transition: all 0.2s ease;
}

.logout-btn:hover {
  background: linear-gradient(135deg, #dc2626, #ea580c);
  border-color: transparent;
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(239, 68, 68, 0.4);
}
</style>
