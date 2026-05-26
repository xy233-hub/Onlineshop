<template>
  <div class="promotions-page">
    <header class="page-header">
      <div class="header-content">
        <div class="header-left">
          <a href="/" class="logo-link">
            <el-icon class="logo-icon"><Shop /></el-icon>
            <span class="logo-text">在线商城</span>
          </a>
        </div>
        <div class="header-center">
          <h1 class="page-title">
            <el-icon><Discount /></el-icon>
            优惠活动
          </h1>
        </div>
        <div class="header-right">
          <el-button @click="goHome" text>
            <el-icon><HomeFilled /></el-icon>
            返回首页
          </el-button>
        </div>
      </div>
    </header>

    <main class="main-content">
      <div class="loading-container" v-if="loading">
        <el-skeleton :rows="6" animated />
      </div>

      <div class="empty-container" v-else-if="promotions.length === 0">
        <el-empty description="暂无进行中的优惠活动">
          <el-button type="primary" @click="goHome">去逛逛商品</el-button>
        </el-empty>
      </div>

      <div class="promotions-container" v-else>
        <div class="promotions-stats">
          <span class="stats-text">当前共有 <strong>{{ promotions.length }}</strong> 个活动进行中</span>
        </div>

        <div class="promotions-grid">
          <div 
            class="promotion-card" 
            v-for="promo in promotions" 
            :key="promo.promotion_id"
            :class="getPromotionCardClass(promo)"
          >
            <div class="card-decoration"></div>
            <div class="promotion-badge">{{ getPromotionTypeLabel(promo.promotion_type) }}</div>
            <div class="promotion-content">
              <h3 class="promotion-name">{{ promo.promotion_name }}</h3>
              <p class="promotion-desc" v-if="promo.description">{{ promo.description }}</p>
              <div class="promotion-discount">
                <span class="discount-value">{{ formatDiscountValue(promo) }}</span>
                <span class="discount-label">{{ getDiscountLabel(promo.promotion_type) }}</span>
              </div>
              <div class="promotion-details">
                <div class="detail-item">
                  <el-icon><Location /></el-icon>
                  <span>{{ getScopeLabel(promo.applicable_scope) }}</span>
                </div>
                <div class="detail-item" v-if="promo.min_purchase_amount">
                  <el-icon><Wallet /></el-icon>
                  <span>满 ¥{{ promo.min_purchase_amount }} 可用</span>
                </div>
              </div>
              <div class="promotion-time">
                <el-icon><Clock /></el-icon>
                <span>{{ formatTimeRange(promo) }}</span>
              </div>
            </div>
            <div class="promotion-footer">
              <el-button type="primary" @click="viewPromotionProducts(promo)">
                查看参与商品
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Discount, Clock, Shop, HomeFilled, Location, Wallet } from '@element-plus/icons-vue'
import { promotionAPI } from '@/api'

const router = useRouter()
const promotions = ref([])
const loading = ref(true)

const fetchActivePromotions = async () => {
  loading.value = true
  try {
    const response = await promotionAPI.getActivePromotions({ size: 20 })
    const data = response?.data?.data || response?.data || []
    promotions.value = Array.isArray(data) ? data : (data.items || [])
  } catch (e) {
    console.error('获取促销活动失败:', e)
    ElMessage.error('获取促销活动失败')
  } finally {
    loading.value = false
  }
}

const getPromotionTypeLabel = (type) => {
  const labels = {
    'DISCOUNT': '折扣',
    'FULL_REDUCTION': '满减',
    'FIXED_PRICE': '特价',
    'BUY_X_GET_Y': '买赠'
  }
  return labels[type] || '优惠'
}

const getDiscountLabel = (type) => {
  const labels = {
    'DISCOUNT': '折',
    'FULL_REDUCTION': '元减',
    'FIXED_PRICE': '特价',
    'BUY_X_GET_Y': '买赠'
  }
  return labels[type] || ''
}

const formatDiscountValue = (promo) => {
  const type = promo.promotion_type
  const value = promo.discount_value
  if (type === 'DISCOUNT') {
    return (value * 10).toFixed(1)
  }
  return value
}

const getScopeLabel = (scope) => {
  const labels = {
    'ALL': '全场商品',
    'PRODUCT': '指定商品',
    'CATEGORY': '指定分类'
  }
  return labels[scope] || scope
}

const getPromotionCardClass = (promo) => {
  const type = promo.promotion_type
  return {
    'card-discount': type === 'DISCOUNT',
    'card-reduction': type === 'FULL_REDUCTION',
    'card-special': type === 'FIXED_PRICE',
    'card-gift': type === 'BUY_X_GET_Y'
  }
}

const formatTimeRange = (promo) => {
  if (!promo.start_time && !promo.end_time) return '长期有效'
  const start = promo.start_time ? formatDate(promo.start_time) : '即日起'
  const end = promo.end_time ? formatDate(promo.end_time) : '长期'
  return `${start} ~ ${end}`
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getMonth() + 1}/${date.getDate()} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

const viewPromotionProducts = (promo) => {
  if (promo.applicable_scope === 'ALL') {
    router.push('/')
  } else {
    ElMessage.info('请在商品列表中查看参与活动的商品')
    router.push('/')
  }
}

const goHome = () => {
  router.push('/')
}

onMounted(() => {
  fetchActivePromotions()
})
</script>

<style scoped>
.promotions-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.page-header {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  box-shadow: 0 2px 20px rgba(0, 0, 0, 0.1);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-content {
  max-width: 1400px;
  margin: 0 auto;
  padding: 16px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-left {
  display: flex;
  align-items: center;
}

.logo-link {
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
  color: #333;
}

.logo-icon {
  font-size: 28px;
  color: #667eea;
}

.logo-text {
  font-size: 20px;
  font-weight: 600;
}

.header-center {
  flex: 1;
  display: flex;
  justify-content: center;
}

.page-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.page-title .el-icon {
  color: #f5576c;
}

.header-right {
  display: flex;
  align-items: center;
}

.main-content {
  max-width: 1400px;
  margin: 0 auto;
  padding: 40px 24px;
}

.loading-container,
.empty-container {
  background: white;
  border-radius: 16px;
  padding: 60px 40px;
  text-align: center;
}

.promotions-stats {
  text-align: center;
  margin-bottom: 30px;
}

.stats-text {
  color: rgba(255, 255, 255, 0.9);
  font-size: 16px;
}

.stats-text strong {
  color: white;
  font-size: 20px;
}

.promotions-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 24px;
}

.promotion-card {
  background: white;
  border-radius: 20px;
  padding: 24px;
  position: relative;
  overflow: hidden;
  transition: all 0.3s ease;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15);
}

.promotion-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.25);
}

.card-decoration {
  position: absolute;
  top: -50px;
  right: -50px;
  width: 150px;
  height: 150px;
  border-radius: 50%;
  opacity: 0.1;
}

.card-discount .card-decoration {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.card-reduction .card-decoration {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.card-special .card-decoration {
  background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
}

.card-gift .card-decoration {
  background: linear-gradient(135deg, #a8edea 0%, #fed6e3 100%);
}

.promotion-badge {
  display: inline-block;
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 16px;
}

.card-discount .promotion-badge {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  color: white;
}

.card-reduction .promotion-badge {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  color: white;
}

.card-special .promotion-badge {
  background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
  color: white;
}

.card-gift .promotion-badge {
  background: linear-gradient(135deg, #a8edea 0%, #fed6e3 100%);
  color: #333;
}

.promotion-content {
  color: #333;
}

.promotion-name {
  font-size: 20px;
  font-weight: 600;
  margin: 0 0 8px 0;
}

.promotion-desc {
  font-size: 14px;
  color: #666;
  margin: 0 0 16px 0;
  line-height: 1.5;
}

.promotion-discount {
  display: flex;
  align-items: baseline;
  gap: 6px;
  margin-bottom: 16px;
}

.discount-value {
  font-size: 42px;
  font-weight: 700;
  line-height: 1;
}

.card-discount .discount-value {
  color: #f5576c;
}

.card-reduction .discount-value {
  color: #4facfe;
}

.card-special .discount-value {
  color: #fa709a;
}

.card-gift .discount-value {
  color: #667eea;
}

.discount-label {
  font-size: 16px;
  color: #666;
}

.promotion-details {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 12px;
}

.detail-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #666;
  background: #f5f5f5;
  padding: 4px 12px;
  border-radius: 12px;
}

.promotion-time {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #999;
}

.promotion-footer {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #eee;
}

.promotion-footer .el-button {
  width: 100%;
  height: 44px;
  font-size: 16px;
  border-radius: 12px;
}

.card-discount .promotion-footer .el-button {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  border: none;
}

.card-reduction .promotion-footer .el-button {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  border: none;
}

.card-special .promotion-footer .el-button {
  background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
  border: none;
}

.card-gift .promotion-footer .el-button {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
}

@media (max-width: 768px) {
  .promotions-grid {
    grid-template-columns: 1fr;
  }
  
  .header-content {
    flex-direction: column;
    gap: 16px;
  }
  
  .header-center {
    order: -1;
  }
}
</style>
