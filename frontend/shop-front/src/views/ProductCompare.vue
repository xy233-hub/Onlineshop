<template>
  <div class="compare-page">
    <el-container>
      <el-header class="header">
        <div class="header-inner">
          <el-breadcrumb separator="/" class="breadcrumb-modern">
            <el-breadcrumb-item @click="$router.push('/')">首页</el-breadcrumb-item>
            <el-breadcrumb-item @click="$router.back()">商品详情</el-breadcrumb-item>
            <el-breadcrumb-item>商品对比</el-breadcrumb-item>
          </el-breadcrumb>
          <div class="header-actions">
            <el-button @click="$router.back()" class="btn-return" size="default">返回</el-button>
          </div>
        </div>
      </el-header>

      <el-main class="main-content">
        <div v-if="currentProduct" class="current-product-card">
          <h3 class="section-title">🎯 当前商品</h3>
          <div class="product-summary">
            <span class="product-name">{{ currentProduct.product_name }}</span>
            <span class="product-price">¥{{ currentProduct.price.toFixed(2) }}</span>
            <span class="product-date">{{ formatTime(currentProduct.created_at) }}</span>
          </div>
        </div>

        <div class="select-section">
          <h3 class="section-title">📋 选择同类商品进行对比</h3>
          <div v-if="similarProducts.length > 0">
            <el-table :data="similarProducts" :row-key="'product_id'" @selection-change="handleSelectionChange">
              <el-table-column type="selection" width="55"></el-table-column>
              <el-table-column prop="product_name" label="商品名称" min-width="250"></el-table-column>
              <el-table-column prop="price" label="价格" width="120">
                <template #default="scope">¥{{ scope.row.price.toFixed(2) }}</template>
              </el-table-column>
              <el-table-column prop="created_at" label="上架时间" width="180"></el-table-column>
            </el-table>
          </div>
          <div v-else class="empty-state">
            <el-empty description="暂无同类商品" />
          </div>
        </div>

        <div v-if="showResult && compareData.length > 0" class="result-section">
          <h3 class="section-title">📊 对比结果</h3>
          <el-table :data="compareData" border class="compare-table">
            <el-table-column prop="product_name" label="商品名称" min-width="250">
              <template #default="scope">
                <span :class="{ 'current-product': scope.row.price_compare === '当前商品' }">
                  {{ scope.row.product_name }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="price" label="价格" width="120"></el-table-column>
            <el-table-column prop="created_at" label="上架时间" width="180"></el-table-column>
            <el-table-column prop="price_compare" label="价格对比" width="140">
              <template #default="scope">
                <el-tag :type="scope.row.price_compare === '更低' ? 'success' : scope.row.price_compare === '更高' ? 'danger' : 'info'">
                  {{ scope.row.price_compare }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="time_compare" label="上架时间对比" width="140">
              <template #default="scope">
                <el-tag :type="scope.row.time_compare === '更早' ? 'info' : 'warning'">
                  {{ scope.row.time_compare }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="action-bar">
          <el-button @click="compareProducts" type="primary" :disabled="selectedProducts.length === 0">
            开始对比 ({{ selectedProducts.length }}个)
          </el-button>
          <el-button v-if="showResult" @click="resetCompare">重新选择</el-button>
        </div>
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { productAPI } from '@/api'

const router = useRouter()
const currentProduct = ref(null)
const similarProducts = ref([])
const selectedProducts = ref([])
const compareData = ref([])
const showResult = ref(false)

const formatTime = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const handleSelectionChange = (val) => {
  selectedProducts.value = val
}

const compareProducts = () => {
  if (selectedProducts.value.length === 0 || !currentProduct.value) return
  
  const current = {
    product_name: currentProduct.value.product_name,
    price: `¥${currentProduct.value.price.toFixed(2)}`,
    created_at: formatTime(currentProduct.value.created_at),
    price_compare: '当前商品',
    time_compare: '当前商品'
  }
  
  compareData.value = [current, ...selectedProducts.value.map(p => ({
    product_name: p.product_name,
    price: `¥${p.price.toFixed(2)}`,
    created_at: p.created_at,
    price_compare: p.price < currentProduct.value.price ? '更低' : p.price > currentProduct.value.price ? '更高' : '相同',
    time_compare: new Date(p.created_at) < new Date(currentProduct.value.created_at) ? '更早' : '更晚'
  }))]
  
  showResult.value = true
}

const resetCompare = () => {
  selectedProducts.value = []
  compareData.value = []
  showResult.value = false
}

onMounted(() => {
  const data = localStorage.getItem('compareProduct')
  if (data) {
    try {
      currentProduct.value = JSON.parse(data)
      
      if (currentProduct.value.category_id) {
        productAPI.getProducts({ category_id: currentProduct.value.category_id }).then(res => {
          const result = res?.data?.data || {}
          similarProducts.value = (result.items || result.list || []).filter(p => p.product_id !== currentProduct.value.product_id)
        }).catch(error => {
          console.error('获取同类商品失败:', error)
        })
      }
    } catch (e) {
      console.error('解析商品数据失败:', e)
    }
  } else {
    router.push('/')
  }
})
</script>

<style scoped>
.compare-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8ec 100%);
}

.header {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  padding: 0;
}

.header-inner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
}

.breadcrumb-modern {
  font-size: 14px;
}

.btn-return {
  background: #f1f5f9;
  color: #475569;
  border: none;
}

.btn-return:hover {
  background: #e2e8f0;
}

.main-content {
  padding: 32px;
  max-width: 1200px;
  margin: 0 auto;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 16px;
  padding-left: 8px;
  border-left: 4px solid #3b82f6;
}

.current-product-card {
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 24px;
}

.product-summary {
  display: flex;
  align-items: center;
  gap: 20px;
}

.product-summary .product-name {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
}

.product-summary .product-price {
  font-size: 24px;
  font-weight: 700;
  color: #ef4444;
}

.product-summary .product-date {
  font-size: 14px;
  color: #64748b;
}

.select-section {
  background: white;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.result-section {
  background: white;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.compare-table {
  --el-table-row-hover-bg-color: #f0f9ff;
}

.compare-table .current-product {
  font-weight: 600;
  color: #3b82f6;
}

.action-bar {
  display: flex;
  justify-content: center;
  gap: 12px;
  padding: 20px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.empty-state {
  padding: 40px;
  text-align: center;
}
</style>