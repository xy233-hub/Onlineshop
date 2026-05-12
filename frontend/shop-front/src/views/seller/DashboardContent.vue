<template>
  <div class="dashboard-content">
    <h2>数据概览</h2>

    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-item">
            <div class="stat-icon online">
              <el-icon><Goods /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.onlineProducts }}</div>
              <div class="stat-label">在线商品</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-item">
            <div class="stat-icon total">
              <el-icon><Box /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalProducts }}</div>
              <div class="stat-label">总商品数</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-item">
            <div class="stat-icon pending">
              <el-icon><Clock /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.pendingOrders }}</div>
              <div class="stat-label">待处理意向</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-item">
            <div class="stat-icon sold">
              <el-icon><SuccessFilled /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.soldProducts }}</div>
              <div class="stat-label">已售出商品</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="charts-row">
      <el-col :span="12">
        <el-card header="最近商品" shadow="hover">
          <div v-if="recentProducts.length > 0">
            <el-table :data="recentProducts" size="small" :show-header="false">
              <el-table-column prop="product_name" label="商品名称" show-overflow-tooltip />
              <el-table-column prop="price" label="价格" width="100">
                <template #default="{ row }">¥{{ row.price }}</template>
              </el-table-column>
              <el-table-column prop="product_status" label="状态" width="80">
                <template #default="{ row }">
                  <el-tag :type="getStatusType(row.product_status)" size="small">
                    {{ getStatusText(row.product_status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="created_at" label="上架时间" width="120">
                <template #default="{ row }">{{ formatTime(row.created_at) }}</template>
              </el-table-column>
            </el-table>
            <div class="view-more">
              <el-button text @click="$router.push('/seller/dashboard/products')">查看更多商品</el-button>
            </div>
          </div>
          <el-empty v-else description="暂无商品" :image-size="80" />
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card header="最新购买意向" shadow="hover">
          <div v-if="recentOrders.length > 0">
            <el-table :data="recentOrders" size="small" :show-header="false">
              <el-table-column prop="customer_name" label="顾客" width="80" />
              <el-table-column prop="product_name" label="商品" show-overflow-tooltip />
              <el-table-column prop="purchase_status" label="状态" width="80">
                <template #default="{ row }">
                  <el-tag :type="getOrderStatusType(row.purchase_status)" size="small">
                    {{ getOrderStatusText(row.purchase_status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="created_at" label="时间" width="120">
                <template #default="{ row }">{{ formatTime(row.created_at) }}</template>
              </el-table-column>
            </el-table>
            <div class="view-more">
              <el-button text @click="$router.push('/seller/dashboard/orders')">查看更多意向</el-button>
            </div>
          </div>
          <el-empty v-else description="暂无购买意向" :image-size="80" />
        </el-card>
      </el-col>
    </el-row>


  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { sellerProductAPI, purchaseAPI, dashboardAPI } from '@/api'
import { formatTime } from '@/utils'

const stats = ref({
  onlineProducts: 0,
  totalProducts: 0,
  pendingOrders: 0,
  soldProducts: 0
})

const recentProducts = ref([])
const recentOrders = ref([])
const loading = ref(false)

// 辅助：从统一响应中提取 data
const extractData = (res) => res?.data?.data ?? null

const fetchDashboardData = async () => {
  loading.value = true
  try {
    // 并行请求：仪表盘统计、卖家商品（尽量获取较多记录用于计算/展示）、卖家收到的购买意向
    const [statsRes, productsRes, ordersRes] = await Promise.all([
      dashboardAPI.getStats().catch(() => null),
      sellerProductAPI.getProducts({ page: 1, size: 100 }).catch(() => null),
      purchaseAPI.getSellerPurchaseIntents({ page: 1, size: 100 }).catch(() => null)
    ])

    const statsData = extractData(statsRes)
    const productsData = extractData(productsRes)
    const ordersData = extractData(ordersRes)

    // 优先使用后端统计（若存在），否则从列表计算
    if (statsData && typeof statsData === 'object') {
      stats.value.onlineProducts = Number(statsData.onlineProducts ?? statsData.online_products ?? 0)
      stats.value.totalProducts = Number(statsData.totalProducts ?? statsData.total_products ?? 0)
      stats.value.pendingOrders = Number(statsData.pendingOrders ?? statsData.pending_orders ?? 0)
      stats.value.soldProducts = Number(statsData.soldProducts ?? statsData.sold_products ?? 0)
    } else {
      const allProducts = Array.isArray(productsData?.items) ? productsData.items : (Array.isArray(productsData) ? productsData : [])
      const allOrders = Array.isArray(ordersData?.items) ? ordersData.items : (Array.isArray(ordersData) ? ordersData : [])

      stats.value.onlineProducts = allProducts.filter(p => p.product_status === 'online').length
      stats.value.totalProducts = allProducts.length
      stats.value.pendingOrders = allOrders.filter(o => o.purchase_status === 'pending').length
      stats.value.soldProducts = allProducts.filter(p => p.product_status === 'sold').length
    }

    // 最近5个商品：优先使用 items 数组
    const productsItems = Array.isArray(productsData?.items) ? productsData.items : (Array.isArray(productsData) ? productsData : [])
    recentProducts.value = productsItems
        .slice()
        .sort((a, b) => new Date(b.created_at) - new Date(a.created_at))
        .slice(0, 5)

    // 最近5个购买意向
    const ordersItems = Array.isArray(ordersData?.items) ? ordersData.items : (Array.isArray(ordersData) ? ordersData : [])
    recentOrders.value = ordersItems
        .slice()
        .sort((a, b) => new Date(b.created_at) - new Date(a.created_at))
        .slice(0, 5)

  } catch (error) {
    console.error('获取仪表板数据失败:', error)
  } finally {
    loading.value = false
  }
}

const getStatusType = (status) => {
  const types = { online: 'success', frozen: 'warning', sold: 'info' }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = { online: '在售', frozen: '冻结', sold: '已售' }
  return texts[status] || status
}

const getOrderStatusType = (status) => {
  const types = { pending: 'warning', success: 'success', failed: 'danger' }
  return types[status] || 'info'
}

const getOrderStatusText = (status) => {
  const texts = { pending: '待处理', success: '成功', failed: '失败' }
  return texts[status] || status
}

onMounted(() => {
  fetchDashboardData()
})
</script>

<style scoped>
.dashboard-content h2 {
  margin-bottom: 28px;
  color: #1e293b;
  font-size: 28px;
  font-weight: 700;
  letter-spacing: -0.02em;
  background: linear-gradient(135deg, #1e293b, #475569);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.stats-row {
  margin-bottom: 32px;
}

.stat-card {
  margin-bottom: 0;
  border-radius: 24px;
  border: 1px solid rgba(224, 230, 237, 0.5);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  overflow: hidden;
}

.stat-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, transparent, rgba(59, 130, 246, 0.3), transparent);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-8px) scale(1.02);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.12);
}

.stat-card:hover::before {
  opacity: 1;
}

.stat-item {
  display: flex;
  align-items: center;
  padding: 28px 24px;
}

.stat-icon {
  width: 76px;
  height: 76px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 24px;
  font-size: 32px;
  color: #fff;
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);
  position: relative;
}

.stat-icon::before {
  content: '';
  position: absolute;
  inset: -6px;
  border-radius: 50%;
  opacity: 0;
  transition: opacity 0.4s ease;
}

.stat-card:hover .stat-icon {
  transform: scale(1.15) rotate(8deg);
}

.stat-card:hover .stat-icon::before {
  opacity: 0.4;
}

.stat-icon.online {
  background: linear-gradient(135deg, #10b981 0%, #06b6d4 100%);
  box-shadow: 0 12px 32px rgba(16, 185, 129, 0.4),
              inset 0 2px 4px rgba(255, 255, 255, 0.4);
}
.stat-icon.online::before {
  background: linear-gradient(135deg, #10b981 0%, #06b6d4 100%);
}

.stat-icon.total {
  background: linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%);
  box-shadow: 0 12px 32px rgba(59, 130, 246, 0.4),
              inset 0 2px 4px rgba(255, 255, 255, 0.4);
}
.stat-icon.total::before {
  background: linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%);
}

.stat-icon.pending {
  background: linear-gradient(135deg, #f59e0b 0%, #ec4899 100%);
  box-shadow: 0 12px 32px rgba(245, 158, 11, 0.4),
              inset 0 2px 4px rgba(255, 255, 255, 0.4);
}
.stat-icon.pending::before {
  background: linear-gradient(135deg, #f59e0b 0%, #ec4899 100%);
}

.stat-icon.sold {
  background: linear-gradient(135deg, #ef4444 0%, #f97316 100%);
  box-shadow: 0 12px 32px rgba(239, 68, 68, 0.4),
              inset 0 2px 4px rgba(255, 255, 255, 0.4);
}
.stat-icon.sold::before {
  background: linear-gradient(135deg, #ef4444 0%, #f97316 100%);
}

.stat-value {
  font-size: 44px;
  font-weight: 800;
  color: #1e293b;
  margin-bottom: 8px;
  letter-spacing: -3px;
  background: linear-gradient(135deg, #1e293b 0%, #475569 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.stat-label {
  color: #64748b;
  font-size: 14px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 1.5px;
}

.charts-row {
  margin-bottom: 32px;
}

.charts-row .el-card {
  min-height: 340px;
  border-radius: 24px;
  border: 1px solid rgba(224, 230, 237, 0.5);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(12px);
}

:deep(.charts-row .el-card__header) {
  background: rgba(248, 250, 252, 0.95);
  border-radius: 24px 24px 0 0;
  padding: 20px 24px;
  font-weight: 600;
  color: #1e293b;
  font-size: 16px;
  border-bottom: 1px solid rgba(224, 230, 237, 0.5);
}

.view-more {
  text-align: center;
  padding: 20px 0;
  border-top: 1px solid #e2e8f0;
  margin-top: 12px;
}

.view-more :deep(.el-button) {
  color: #3b82f6;
  font-weight: 500;
}

.view-more :deep(.el-button:hover) {
  color: #2563eb;
}

:deep(.el-table) {
  border: none;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 20px;
  overflow: hidden;
}

:deep(.el-table th) {
  background: rgba(248, 250, 252, 0.95);
  color: #64748b;
  font-weight: 600;
  font-size: 13px;
  padding: 16px 20px;
  border-bottom: 1px solid #e2e8f0;
}

:deep(.el-table td) {
  padding: 16px 20px;
  border-bottom: 1px solid #f1f5f9;
}

:deep(.el-table__row:hover) {
  background: #f8fafc;
}

:deep(.el-tag) {
  border-radius: 12px;
  padding: 6px 14px;
  font-size: 12px;
  font-weight: 600;
}

:deep(.el-tag--success) {
  background: linear-gradient(135deg, #dcfce7, #bbf7d0);
  color: #16a34a;
  border: none;
}

:deep(.el-tag--warning) {
  background: linear-gradient(135deg, #fef3c7, #fde68a);
  color: #d97706;
  border: none;
}

:deep(.el-tag--info) {
  background: linear-gradient(135deg, #dbeafe, #bfdbfe);
  color: #2563eb;
  border: none;
}

:deep(.el-tag--danger) {
  background: linear-gradient(135deg, #fee2e2, #fecaca);
  color: #dc2626;
  border: none;
}

:deep(.el-card) {
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(12px);
  border-radius: 24px;
  border: 1px solid rgba(224, 230, 237, 0.5);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}
</style>
