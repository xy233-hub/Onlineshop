```vue
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
        >
          <!-- 这里改为与路由一致的完整路径 -->
          <el-menu-item index="/seller/dashboard">
            <el-icon><House /></el-icon>
            <span>首页</span>
          </el-menu-item>
          <el-menu-item index="/seller/dashboard/products">
            <el-icon><Goods /></el-icon>
            <span>商品管理</span>
          </el-menu-item>
          <el-menu-item index="/seller/dashboard/orders">
            <el-icon><Tickets /></el-icon>
            <span>购买意向</span>
          </el-menu-item>
          <el-menu-item index="/seller/dashboard/password">
            <el-icon><Lock /></el-icon>
            <span>修改密码</span>
          </el-menu-item>
          <el-menu-item index="/seller/dashboard/categories">
            <el-icon><Folder /></el-icon>
            <span>分类管理</span>
          </el-menu-item>
          <el-menu-item index="/seller/dashboard/customers">
            <el-icon><Folder /></el-icon>
            <span>顾客详情</span>
          </el-menu-item>
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
              <el-button @click="handleLogout" size="small">退出登录</el-button>
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
/* 与之前相同的样式 */
.dashboard-container {
  height: 100vh;
  display: flex;
}

.sidebar-header {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid #e6e6e6;
  background: #001529;
  color: #fff;
}

.sidebar-header h3 {
  margin: 0;
  font-size: 16px;
}

.sidebar-menu {
  border: none;
  height: calc(100vh - 60px);
}

.header {
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  padding: 0 20px;
}

.header-content {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 15px;
}

.welcome {
  color: #666;
  font-size: 14px;
}

.view-store-btn {
  margin-right: 10px;
}

.main-content {
  background: #f5f5f5;
  padding: 20px;
  overflow-y: auto;
}

.el-aside {
  background: #001529;
}

:deep(.el-menu) {
  background: #001529;
  border: none;
}

:deep(.el-menu-item) {
  color: #bfbfbf;
}

:deep(.el-menu-item.is-active) {
  color: #1890ff;
  background: #1890ff20;
}

:deep(.el-menu-item:hover) {
  background: #1890ff10;
  color: #fff;
}
</style>
