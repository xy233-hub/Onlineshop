```vue
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

    <el-row :gutter="20" class="quick-actions">
      <el-col :span="24">
        <el-card header="快捷操作" shadow="hover">
          <el-space wrap>
            <el-button type="primary" @click="$router.push('/seller/dashboard/products')">
              <el-icon><Plus /></el-icon>
              发布新商品
            </el-button>
            <el-button @click="$router.push('/seller/dashboard/products')">
              <el-icon><Edit /></el-icon>
              管理商品
            </el-button>
            <el-button @click="$router.push('/seller/dashboard/orders')">
              <el-icon><View /></el-icon>
              查看购买意向
            </el-button>
            <el-button @click="$router.push('/seller/dashboard/password')">
              <el-icon><Lock /></el-icon>
              修改密码
            </el-button>
          </el-space>
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
  margin-bottom: 20px;
  color: #303133;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  margin-bottom: 0;
  border-radius: 8px;
}

.stat-item {
  display: flex;
  align-items: center;
  padding: 10px 0;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 15px;
  font-size: 24px;
  color: #fff;
}

.stat-icon.online { background: #67c23a; }
.stat-icon.total { background: #409eff; }
.stat-icon.pending { background: #e6a23c; }
.stat-icon.sold { background: #f56c6c; }

.stat-value {
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 5px;
}

.stat-label {
  color: #909399;
  font-size: 14px;
}

.charts-row {
  margin-bottom: 20px;
}

.charts-row .el-card {
  min-height: 300px;
}

.view-more {
  text-align: center;
  padding: 10px 0;
  border-top: 1px solid #f0f0f0;
}

.quick-actions .el-card {
  min-height: 100px;
}

.quick-actions .el-button {
  padding: 20px 24px;
  font-size: 14px;
}
</style>
