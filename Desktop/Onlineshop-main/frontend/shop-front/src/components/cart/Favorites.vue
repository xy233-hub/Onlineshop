<template>
  <div class="favorites">
    <h2>我的收藏</h2>
    <div v-if="favoriteItems.length === 0" class="empty-favorites">
      <p>收藏夹为空</p>
      <el-button type="primary" @click="$router.push('/')">去购物</el-button>
    </div>
    <div v-else class="favorites-grid">
      <div 
        v-for="item in favoriteItems" 
        :key="item.id" 
        class="favorite-item"
      >
        <el-image 
          :src="item.product.image_url" 
          class="item-image" 
          fit="cover" 
        />
        <div class="item-details">
          <div class="item-name">{{ item.product.product_name }}</div>
          <div class="item-price">¥{{ item.product.price }}</div>
        </div>
        <div class="item-actions">
          <el-button 
            type="primary" 
            size="small" 
            @click="addToCart(item)"
          >
            加入购物车
          </el-button>
          <el-button 
            type="danger" 
            size="small" 
            @click="removeFromFavorites(item)"
          >
            取消收藏
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { favoritesAPI, cartAPI } from '@/api'

// 收藏夹商品数据
const favoriteItems = ref([])

// 加载状态
const loading = ref(false)

// 获取路由实例
const router = useRouter()

// 添加到购物车
const addToCart = async (item) => {
  try {
    await cartAPI.addToCart({ product_id: item.product.product_id, quantity: 1 })
    ElMessage.success('商品已添加到购物车')
  } catch (error) {
    ElMessage.error('添加商品到购物车失败')
  }
}

// 从收藏夹移除商品
const removeFromFavorites = async (item) => {
  try {
    await ElMessageBox.confirm('确定要取消收藏此商品吗？', '确认取消收藏', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await favoritesAPI.removeFavorite(item.id)
    const index = favoriteItems.value.findIndex(i => i.id === item.id)
    if (index > -1) {
      favoriteItems.value.splice(index, 1)
      ElMessage.success('商品已取消收藏')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('取消收藏失败')
    }
  }
}

// 初始化收藏夹数据
const initializeFavorites = async () => {
  try {
    loading.value = true
    const response = await favoritesAPI.getFavorites()
    favoriteItems.value = response.data.data
  } catch (error) {
    ElMessage.error('加载收藏夹数据失败')
    // 使用模拟数据作为后备
    favoriteItems.value = [
      {
        id: 1,
        product: {
          product_id: 103,
          product_name: '收藏商品1',
          price: 129.99,
          image_url: 'https://via.placeholder.com/100x100'
        }
      },
      {
        id: 2,
        product: {
          product_id: 104,
          product_name: '收藏商品2',
          price: 299.99,
          image_url: 'https://via.placeholder.com/100x100'
        }
      },
      {
        id: 3,
        product: {
          product_id: 105,
          product_name: '收藏商品3',
          price: 89.99,
          image_url: 'https://via.placeholder.com/100x100'
        }
      }
    ]
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  initializeFavorites()
})
</script>

<style scoped>
.favorites {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.empty-favorites {
  text-align: center;
  padding: 50px 0;
}

.favorites-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 20px;
  margin-top: 20px;
}

.favorite-item {
  border: 1px solid #eee;
  border-radius: 8px;
  padding: 15px;
  text-align: center;
  transition: box-shadow 0.3s;
}

.favorite-item:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.item-image {
  width: 100%;
  height: 200px;
  margin-bottom: 15px;
}

.item-details {
  margin-bottom: 15px;
}

.item-name {
  font-weight: bold;
  margin-bottom: 10px;
  height: 40px;
  overflow: hidden;
}

.item-price {
  color: #f56c6c;
  font-size: 18px;
  font-weight: bold;
}

.item-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
</style>