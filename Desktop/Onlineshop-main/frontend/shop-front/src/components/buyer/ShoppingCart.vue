<template>
  <div class="shopping-cart">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <h3>购物车</h3>
          <el-button 
            type="danger" 
            size="small" 
            link
            :disabled="cartStore.cartItems.length === 0"
            @click="clearCart"
          >
            清空
          </el-button>
        </div>
      </template>

      <div v-if="cartStore.loading" class="loading-state">
        <el-skeleton :rows="4" animated />
      </div>

      <div v-else-if="cartStore.cartItems.length === 0" class="empty-cart">
        <el-empty description="购物车是空的">
          <el-button type="primary" @click="$router.push('/')">去购物</el-button>
        </el-empty>
      </div>

      <div v-else class="cart-items">
        <div 
          v-for="item in cartStore.cartItems" 
          :key="item.cart_item_id || item.product.product_id"
          class="cart-item"
        >
          <div class="item-image">
            <img 
              :src="getImageUrl(item.product)" 
              alt="商品图片"
              @error="onImageError($event)"
            />
          </div>
          <div class="item-info">
            <h4 class="item-name">{{ item.product.product_name }}</h4>
            <div class="item-price">¥{{ (item.product.price || 0).toFixed(2) }}</div>
            <div class="item-actions">
              <el-input-number
                v-model="item.quantity"
                :min="1"
                :max="item.product.stock_quantity || 99"
                size="small"
                @change="updateQuantity(item)"
              />
              <el-button 
                type="danger" 
                size="small"
                icon="Delete"
                @click="removeItem(item)"
              />
            </div>
          </div>
          <div class="item-subtotal">
            ¥{{ (item.product.price * item.quantity).toFixed(2) }}
          </div>
        </div>

        <div class="cart-footer">
          <div class="cart-total">
            <span>总计：</span>
            <span class="total-price">¥{{ cartStore.cartTotal.toFixed(2) }}</span>
          </div>
          <el-button type="primary" size="large">
            结算 ({{ cartStore.cartCount }})
          </el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useCartStore } from '../../stores/cart'
import { useRouter } from 'vue-router'

const cartStore = useCartStore()
const router = useRouter()

// 加载购物车数据
onMounted(() => {
  cartStore.getCartItems()
})

// 更新商品数量
const updateQuantity = async (item) => {
  try {
    await cartStore.updateCartItem(item.cart_item_id, {
      quantity: item.quantity
    })
    ElMessage.success('更新数量成功')
  } catch (error) {
    console.error('更新数量失败:', error)
    ElMessage.error('更新数量失败，请重试')
    // 恢复原来的数量
    item.quantity = item.quantity_prev
  }
}

// 删除商品
const removeItem = async (item) => {
  try {
    await cartStore.removeCartItem(item.cart_item_id)
    ElMessage.success('删除商品成功')
  } catch (error) {
    console.error('删除商品失败:', error)
    ElMessage.error('删除商品失败，请重试')
  }
}

// 清空购物车
const clearCart = async () => {
  try {
    await ElMessageBox.confirm('确定要清空购物车吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await cartStore.clearCart()
    ElMessage.success('清空购物车成功')
  } catch (error) {
    if (error === 'cancel') return
    console.error('清空购物车失败:', error)
    ElMessage.error('清空购物车失败，请重试')
  }
}

// 获取商品图片URL
const getImageUrl = (product) => {
  // 尝试多种可能的图片字段
  if (Array.isArray(product.images) && product.images.length > 0) {
    const firstImage = product.images[0]
    if (typeof firstImage === 'string') {
      return firstImage
    } else if (typeof firstImage === 'object') {
      return firstImage.image_url || firstImage.media_url || firstImage.url || '/default-product.jpg'
    }
  }
  
  const imageField = product.image_url || product.product_image || product.image || product.cover_image
  
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
</script>

<style scoped>
.shopping-cart {
  width: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.loading-state {
  padding: 20px;
}

.empty-cart {
  padding: 40px 20px;
  text-align: center;
}

.cart-items {
  max-height: 400px;
  overflow-y: auto;
}

.cart-item {
  display: flex;
  padding: 12px 0;
  border-bottom: 1px solid #eee;
  align-items: center;
}

.item-image {
  width: 80px;
  height: 80px;
  overflow: hidden;
  border-radius: 8px;
  margin-right: 12px;
}

.item-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.item-info {
  flex: 1;
  min-width: 0;
}

.item-name {
  font-size: 14px;
  margin: 0 0 8px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-price {
  font-size: 16px;
  font-weight: 600;
  color: #f56c6c;
  margin-bottom: 8px;
}

.item-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.item-subtotal {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-left: 16px;
  min-width: 80px;
  text-align: right;
}

.cart-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0 0;
  border-top: 2px solid #eee;
  margin-top: 12px;
}

.cart-total {
  font-size: 18px;
  font-weight: 600;
}

.total-price {
  color: #f56c6c;
  margin-left: 8px;
}
</style>