<template>
  <div class="favorites">
    <el-container>
      <el-header class="header">
        <div class="header-inner">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item @click="$router.push('/')">首页</el-breadcrumb-item>
            <el-breadcrumb-item>我的收藏</el-breadcrumb-item>
          </el-breadcrumb>
          <el-button @click="$router.go(-1)" type="text" plain>
            返回
          </el-button>
        </div>
      </el-header>

      <el-main>
        <div class="page-title">
          <h1>我的收藏</h1>
            <el-button 
              v-if="favoritesStore.favoritesList.length > 0"
              type="danger" 
              plain
              @click="clearFavorites"
            >
              <el-icon><Delete /></el-icon>
              清空收藏
            </el-button>
        </div>

        <div v-if="loading || favoritesStore.loading" class="loading-state">
          <el-skeleton :rows="4" animated />
        </div>

        <div v-else-if="favoritesStore.favoritesList.length === 0" class="empty-state">
          <el-empty description="暂无收藏商品">
            <el-button type="primary" @click="$router.push('/')">去购物</el-button>
          </el-empty>
        </div>

        <div v-else class="favorites-grid">
          <el-card 
            v-for="item in favoritesStore.favoritesList" 
            :key="item.product_id" 
            shadow="hover"
            class="favorites-card"
          >
            <div class="card-content">
              <div class="product-image" @click="goToDetail(item.product_id)">
                <img 
                  :src="getImageUrl(item)" 
                  alt="商品图片"
                  loading="lazy"
                  @error="onImageError($event)"
                />
              </div>
              <div class="product-info">
                <h3 class="product-title" @click="goToDetail(item.product_id)">
                  {{ item.product_name }}
                </h3>
                <div class="product-price">¥{{ (item.price || 0).toFixed(2) }}</div>
                <div class="product-status">
                  <el-tag :type="getStatusType(item.product_status)">{{ getStatusText(item.product_status) }}</el-tag>
                </div>
                <div class="product-stock">
                  库存：{{ item.stock_quantity || 0 }}
                </div>
              </div>
              <div class="product-actions">
                <el-button 
                  type="success" 
                  size="small"
                  :disabled="item.product_status !== 'online' || (item.stock_quantity != null && item.stock_quantity <= 0)"
                  @click="addToCart(item)"
                >
                  加入购物车
                </el-button>
                <el-button 
                  type="danger" 
                  size="small"
                  @click="removeFromFavorites(item.product_id)"
                >
                  取消收藏
                </el-button>
              </div>
            </div>
          </el-card>
        </div>
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useFavoritesStore } from '@/stores/favorites'
import { useCartStore } from '@/stores/cart'
import { useCustomerStore } from '@/stores/customer'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import { productAPI } from '@/api'

const router = useRouter()
const favoritesStore = useFavoritesStore()
const cartStore = useCartStore()
const customerStore = useCustomerStore()
const loading = ref(false)

// 获取收藏商品详情
const fetchProductDetails = async () => {
  loading.value = true
  try {
    // 这里可以根据需要从API获取最新的商品信息
    // 例如：检查库存状态、价格变化等
    const favorites = favoritesStore.favoritesList
    if (favorites.length === 0) return
    
    for (const favorite of favorites) {
      try {
        const response = await productAPI.getProductDetail(favorite.product_id)
        const updatedProduct = response?.data?.data || response?.data
        if (updatedProduct) {
          // 更新本地收藏中的商品信息
          Object.assign(favorite, updatedProduct)
        }
      } catch (error) {
        console.error(`获取商品 ${favorite.product_id} 详情失败:`, error)
        // 可以选择移除无法获取的商品
        // favoritesStore.removeFromFavorites(favorite.product_id)
      }
    }
  } catch (error) {
    console.error('获取收藏商品详情失败:', error)
    ElMessage.error('获取收藏商品详情失败')
  } finally {
    loading.value = false
  }
}

// 跳转到商品详情页
const goToDetail = (productId) => {
  router.push(`/product/${productId}`)
}

// 从收藏中移除
const removeFromFavorites = (productId) => {
  ElMessageBox.confirm('确定要取消收藏该商品吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    favoritesStore.removeFromFavorites(productId)
    ElMessage.success('已取消收藏')
  }).catch(() => {
    // 取消操作，不做任何处理
  })
}

// 清空收藏
const clearFavorites = () => {
  ElMessageBox.confirm('确定要清空所有收藏商品吗？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'error'
  }).then(() => {
    favoritesStore.clearFavorites()
    ElMessage.success('已清空所有收藏')
  }).catch(() => {
    // 取消操作，不做任何处理
  })
}

// 加入购物车
const addToCart = async (product) => {
  if (!product || product.product_status !== 'online') {
    ElMessage.error('商品不可购买')
    return
  }

  if (product.stock_quantity != null && product.stock_quantity <= 0) {
    ElMessage.error('商品已售罄')
    return
  }

  try {
    await cartStore.addToCart(
      customerStore.customer?.customer_id || 'local_customer', // 使用实际客户ID或默认值
      product.product_id,
      1, // 默认添加1件
      product // 传递完整商品数据
    )
    ElMessage.success('商品已成功加入购物车')
  } catch (error) {
    console.error('加入购物车失败:', error)
    ElMessage.error('加入购物车失败，请重试')
  }
}

// 获取商品状态类型
const getStatusType = (status) => {
  const types = { online: 'success', frozen: 'warning', sold: 'info' }
  return types[status] || 'info'
}

// 获取商品状态文本
const getStatusText = (status) => {
  const texts = { online: '在售', frozen: '交易中', sold: '已售出' }
  return texts[status] || status
}

// 获取商品图片URL
const getImageUrl = (item) => {
  // 首先尝试从images数组获取（如果存在）
  if (Array.isArray(item.images) && item.images.length > 0) {
    const firstImage = item.images[0]
    if (typeof firstImage === 'string') {
      return firstImage
    } else if (typeof firstImage === 'object') {
      return firstImage.image_url || firstImage.media_url || firstImage.url || '/default-product.jpg'
    }
  }
  
  // 尝试多种可能的图片字段
  const imageField = item.image_url || item.product_image || item.image || item.cover_image
  
  // 如果没有图片字段，返回默认图片
  if (!imageField) {
    return '/default-product.jpg'
  }
  
  // 检查是否是完整URL
  if (typeof imageField === 'string' && (imageField.startsWith('http://') || imageField.startsWith('https://') || imageField.startsWith('data:image/'))) {
    return imageField
  }
  
  // 检查是否是相对路径
  if (typeof imageField === 'string' && imageField.startsWith('/')) {
    return imageField
  }
  
  // 如果是图片ID或相对路径，尝试拼接默认URL
  return `/api/media/${imageField}` || '/default-product.jpg'
}

// 图片加载失败处理
const onImageError = (event) => {
  event.target.src = '/default-product.jpg'
}

onMounted(() => {
  // 加载收藏数据
  favoritesStore.loadFromLocalStorage()
  // 获取最新商品详情
  fetchProductDetails()
})
</script>

<style scoped>
.favorites { min-height: 100vh; background: #fafafa; }
.header { background: #fff; border-bottom: 1px solid #eef0f3; display: flex; align-items: center; padding: 12px 24px; }
.header-inner { width: 100%; display: flex; justify-content: space-between; align-items: center; gap: 12px; }

.page-title { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.page-title h1 { margin: 0; font-size: 24px; color: #222; }

.loading-state { max-width: 1200px; margin: 0 auto; padding: 20px; }
.empty-state { text-align: center; padding: 60px 20px; }

.favorites-grid { max-width: 1200px; margin: 0 auto; display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 20px; }
.favorites-card { height: 100%; display: flex; flex-direction: column; }

.card-content {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.product-image {
  width: 100%;
  height: 200px;
  overflow: hidden;
  cursor: pointer;
  border-radius: 8px;
  margin-bottom: 12px;
}

.product-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.product-image:hover img {
  transform: scale(1.05);
}

.product-info {
  flex: 1;
  margin-bottom: 16px;
}

.product-title {
  font-size: 16px;
  margin: 0 0 8px;
  color: #222;
  line-height: 1.4;
  cursor: pointer;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.product-title:hover {
  color: #409EFF;
}

.product-price {
  font-size: 20px;
  font-weight: 700;
  color: #f56c6c;
  margin-bottom: 8px;
}

.product-status {
  margin-bottom: 8px;
}

.product-stock {
  color: #666;
  font-size: 13px;
}

.product-actions {
  display: flex;
  gap: 8px;
}

.product-actions .el-button {
  flex: 1;
}

@media (max-width: 768px) {
  .favorites-grid {
    grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
    padding: 0 12px;
  }
  
  .product-image {
    height: 180px;
  }
  
  .page-title {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .header {
    padding: 8px 16px;
  }
}

@media (max-width: 480px) {
  .favorites-grid {
    grid-template-columns: 1fr;
    padding: 0 8px;
  }
  
  .product-image {
    height: 200px;
  }
  
  .page-title h1 {
    font-size: 20px;
  }
}
</style>