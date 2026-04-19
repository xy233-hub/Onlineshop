<template>
  <div class="dashboard-content">
    <h2>数据概览</h2>

    <el-row :gutter="20" class="stats-row">
      <el-col :span="4">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-item">
            <div class="stat-icon online"><el-icon><Goods /></el-icon></div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.onlineProducts }}</div>
              <div class="stat-label">在线商品</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="4">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-item">
            <div class="stat-icon total"><el-icon><Box /></el-icon></div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalProducts }}</div>
              <div class="stat-label">总商品数</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="4">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-item">
            <div class="stat-icon pending"><el-icon><Clock /></el-icon></div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.pendingOrders }}</div>
              <div class="stat-label">待处理意向</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="4">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-item">
            <div class="stat-icon sold"><el-icon><SuccessFilled /></el-icon></div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.soldProducts }}</div>
              <div class="stat-label">已售出商品</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="4">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-item">
            <div class="stat-icon promo"><el-icon><Discount /></el-icon></div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.activePromotions }}</div>
              <div class="stat-label">进行中促销</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="4">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-item">
            <div class="stat-icon draft"><el-icon><Document /></el-icon></div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.draftPromotions }}</div>
              <div class="stat-label">草稿促销</div>
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
            <el-button type="success" @click="$router.push('/seller/dashboard/promotions')">
              <el-icon><Discount /></el-icon>
              促销管理
            </el-button>
            <el-button @click="$router.push('/seller/dashboard/password')">
              <el-icon><Lock /></el-icon>
              修改密码
            </el-button>
          </el-space>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="charts-row">
      <el-col :span="24">
        <el-card header="最近促销活动" shadow="hover">
          <div v-if="recentPromotions.length > 0">
            <el-table :data="recentPromotions" size="small" :show-header="false">
              <el-table-column prop="promotion_name" label="活动名称" show-overflow-tooltip />
              <el-table-column prop="promotion_type" label="类型" width="120">
                <template #default="{ row }">{{ promotionTypeText(row.promotion_type) }}</template>
              </el-table-column>
              <el-table-column prop="status" label="状态" width="100">
                <template #default="{ row }">
                  <el-tag :type="promotionStatusTagType(row.status)" size="small">{{ promotionStatusText(row.status) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="end_time" label="结束时间" width="180" />
            </el-table>
            <div class="view-more">
              <el-button text @click="$router.push('/seller/dashboard/promotions')">查看更多促销活动</el-button>
            </div>
          </div>
          <el-empty v-else description="暂无促销活动" :image-size="80" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { sellerProductAPI, purchaseAPI, dashboardAPI, promotionAPI } from '@/api'
import { formatTime } from '@/utils'

const stats = ref({
  onlineProducts: 0,
  totalProducts: 0,
  pendingOrders: 0,
  soldProducts: 0,
  activePromotions: 0,
  draftPromotions: 0
})

const recentProducts = ref([])
const recentOrders = ref([])
const recentPromotions = ref([])

const extractData = (res) => res?.data?.data ?? null

const fetchDashboardData = async () => {
  try {
    const [statsRes, productsRes, ordersRes, promotionsRes] = await Promise.all([
      dashboardAPI.getStats().catch(() => null),
      sellerProductAPI.getProducts({ page: 1, size: 100 }).catch(() => null),
      purchaseAPI.getSellerPurchaseIntents({ page: 1, size: 100 }).catch(() => null),
      promotionAPI.getPromotions({ page: 1, size: 50 }).catch(() => null)
    ])

    const statsData = extractData(statsRes)
    const productsData = extractData(productsRes)
    const ordersData = extractData(ordersRes)
    const promotionsData = extractData(promotionsRes)

    const allProducts = Array.isArray(productsData?.items) ? productsData.items : (Array.isArray(productsData) ? productsData : [])
    const allOrders = Array.isArray(ordersData?.items) ? ordersData.items : (Array.isArray(ordersData) ? ordersData : [])
    const allPromotions = Array.isArray(promotionsData?.items) ? promotionsData.items : (Array.isArray(promotionsData) ? promotionsData : [])

    if (statsData && typeof statsData === 'object') {
      stats.value.onlineProducts = Number(statsData.onlineProducts ?? statsData.online_products ?? allProducts.filter(p => p.product_status === 'online').length)
      stats.value.totalProducts = Number(statsData.totalProducts ?? statsData.total_products ?? allProducts.length)
      stats.value.pendingOrders = Number(statsData.pendingOrders ?? statsData.pending_orders ?? allOrders.filter(o => o.purchase_status === 'pending').length)
      stats.value.soldProducts = Number(statsData.soldProducts ?? statsData.sold_products ?? allProducts.filter(p => p.product_status === 'sold').length)
    } else {
      stats.value.onlineProducts = allProducts.filter(p => p.product_status === 'online').length
      stats.value.totalProducts = allProducts.length
      stats.value.pendingOrders = allOrders.filter(o => o.purchase_status === 'pending').length
      stats.value.soldProducts = allProducts.filter(p => p.product_status === 'sold').length
    }

    stats.value.activePromotions = allPromotions.filter(p => p.status === 'ACTIVE').length
    stats.value.draftPromotions = allPromotions.filter(p => p.status === 'DRAFT').length

    recentProducts.value = allProducts.slice().sort((a, b) => new Date(b.created_at) - new Date(a.created_at)).slice(0, 5)
    recentOrders.value = allOrders.slice().sort((a, b) => new Date(b.created_at) - new Date(a.created_at)).slice(0, 5)
    recentPromotions.value = allPromotions.slice().sort((a, b) => new Date(b.created_at) - new Date(a.created_at)).slice(0, 5)
  } catch (error) {
    console.error('获取仪表板数据失败:', error)
  }
}

const getStatusType = (status) => ({ online: 'success', frozen: 'warning', sold: 'info' }[status] || 'info')
const getStatusText = (status) => ({ online: '在售', frozen: '冻结', sold: '已售' }[status] || status)
const getOrderStatusType = (status) => ({ pending: 'warning', success: 'success', failed: 'danger' }[status] || 'info')
const getOrderStatusText = (status) => ({ pending: '待处理', success: '成功', failed: '失败' }[status] || status)
const promotionTypeText = (type) => ({ DISCOUNT: '限时折扣', FULL_REDUCTION: '满减', COUPON: '优惠券', FLASH_SALE: '秒杀' }[type] || type)
const promotionStatusText = (status) => ({ DRAFT: '草稿', ACTIVE: '进行中', ENDED: '已结束', CANCELLED: '已取消' }[status] || status)
const promotionStatusTagType = (status) => ({ DRAFT: 'info', ACTIVE: 'success', ENDED: 'warning', CANCELLED: 'danger' }[status] || 'info')

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
  border-radius: 8px;
}

.stat-item {
  display: flex;
  align-items: center;
  padding: 8px 0;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
  font-size: 20px;
  color: #fff;
}

.stat-icon.online { background: #67c23a; }
.stat-icon.total { background: #409eff; }
.stat-icon.pending { background: #e6a23c; }
.stat-icon.sold { background: #f56c6c; }
.stat-icon.promo { background: #22c55e; }
.stat-icon.draft { background: #909399; }

.stat-value {
  font-size: 22px;
  font-weight: bold;
  margin-bottom: 4px;
}

.stat-label {
  color: #909399;
  font-size: 13px;
}

.charts-row {
  margin-bottom: 20px;
}

.view-more {
  text-align: center;
  padding-top: 10px;
  border-top: 1px solid #f0f0f0;
}

.quick-actions .el-button {
  padding: 16px 20px;
}
</style>
