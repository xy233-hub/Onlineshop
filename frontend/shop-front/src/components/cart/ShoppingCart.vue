<template>
  <div class="shopping-cart">
    <h2>购物车</h2>
    <div v-if="cartItems.length === 0" class="empty-cart">
      <p>购物车为空</p>
      <el-button type="primary" @click="$router.push('/')">去购物</el-button>
    </div>
    <div v-else>
      <div class="cart-items">
        <el-checkbox 
          v-model="selectAll" 
          @change="handleSelectAll"
          class="select-all"
        >
          全选
        </el-checkbox>
        
        <div 
          v-for="item in cartItems" 
          :key="item.id" 
          class="cart-item"
        >
          <el-checkbox 
            v-model="item.selected" 
            @change="updateSelectAllState"
          />
          <el-image 
            :src="item.product.image_url" 
            class="item-image" 
            fit="cover" 
          />
          <div class="item-details">
            <div class="item-name">{{ item.product.product_name }}</div>
            <div class="item-price">¥{{ item.product.price }}</div>
          </div>
          <div class="item-quantity">
            <el-input-number 
              v-model="item.quantity" 
              :min="1" 
              :max="item.product.stock_quantity"
              @change="updateItemQuantity(item)"
            />
          </div>
          <div class="item-total">¥{{ (item.product.price * item.quantity).toFixed(2) }}</div>
          <el-button 
            type="text" 
            @click="removeFromCart(item)"
            class="remove-btn"
          >
            删除
          </el-button>
          <el-button 
            type="text" 
            @click="moveToFavorites(item)"
            class="favorite-btn"
          >
            移到收藏
          </el-button>
        </div>
      </div>
      
      <div class="cart-summary">
        <div class="summary-row">
          <span>总计:</span>
          <span class="total-amount">¥{{ totalAmount.toFixed(2) }}</span>
        </div>
        <el-button 
          type="primary" 
          :disabled="selectedItems.length === 0"
          @click="checkout"
          class="checkout-btn"
        >
          结算 ({{ selectedItems.length }} 件商品)
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { cartAPI, favoritesAPI } from '@/api'

// 购物车商品数据
const cartItems = ref([])

// 全选状态
const selectAll = ref(false)

// 加载状态
const loading = ref(false)

// 获取路由实例
const router = useRouter()

// 计算属性：选中的商品
const selectedItems = computed(() => {
  return cartItems.value.filter(item => item.selected)
})

// 计算属性：总金额
const totalAmount = computed(() => {
  return selectedItems.value.reduce((total, item) => {
    return total + (item.product.price * item.quantity)
  }, 0)
})

// 全选/取消全选
const handleSelectAll = (val) => {
  cartItems.value.forEach(item => {
    item.selected = val
  })
}

// 更新全选状态
const updateSelectAllState = () => {
  const allSelected = cartItems.value.every(item => item.selected)
  selectAll.value = allSelected
}

// 更新商品数量
const updateItemQuantity = async (item) => {
  try {
    await cartAPI.updateCartItem(item.id, { quantity: item.quantity })
    ElMessage.success('商品数量已更新')
  } catch (error) {
    ElMessage.error('更新商品数量失败')
  }
}

// 从购物车移除商品
const removeFromCart = async (item) => {
  try {
    await ElMessageBox.confirm('确定要从购物车移除此商品吗？', '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await cartAPI.removeCartItem(item.id)
    const index = cartItems.value.findIndex(i => i.id === item.id)
    if (index > -1) {
      cartItems.value.splice(index, 1)
      updateSelectAllState()
      ElMessage.success('商品已移除')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('移除商品失败')
    }
  }
}

// 将商品移到收藏夹
const moveToFavorites = async (item) => {
  try {
    // 先添加到收藏夹
    await favoritesAPI.addToFavorites({ product_id: item.product.product_id })
    
    // 再从购物车移除
    await removeFromCart(item)
    
    ElMessage.success('商品已移到收藏夹')
  } catch (error) {
    ElMessage.error('移动商品失败')
  }
}

// 结算选中的商品
const checkout = () => {
  if (selectedItems.value.length === 0) {
    ElMessage.warning('请选择要结算的商品')
    return
  }
  
  // 跳转到结算页面
  router.push('/checkout')
}

// 初始化购物车数据
const initializeCart = async () => {
  try {
    loading.value = true
    const response = await cartAPI.getCartItems()
    cartItems.value = response.data.data.map(item => ({
      ...item,
      selected: false
    }))
  } catch (error) {
    ElMessage.error('加载购物车数据失败')
    // 使用模拟数据作为后备
    cartItems.value = [
      {
        id: 1,
        product: {
          product_id: 101,
          product_name: '示例商品1',
          price: 99.99,
          image_url: 'https://via.placeholder.com/100x100',
          stock_quantity: 10
        },
        quantity: 1,
        selected: true
      },
      {
        id: 2,
        product: {
          product_id: 102,
          product_name: '示例商品2',
          price: 199.99,
          image_url: 'https://via.placeholder.com/100x100',
          stock_quantity: 5
        },
        quantity: 2,
        selected: false
      }
    ]
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  initializeCart()
  updateSelectAllState()
})
</script>

<style scoped>
.shopping-cart {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.empty-cart {
  text-align: center;
  padding: 50px 0;
}

.cart-items {
  margin: 20px 0;
}

.select-all {
  margin-bottom: 15px;
  font-weight: bold;
}

.cart-item {
  display: flex;
  align-items: center;
  padding: 15px;
  border: 1px solid #eee;
  border-radius: 4px;
  margin-bottom: 10px;
}

.item-image {
  width: 80px;
  height: 80px;
  margin: 0 15px;
}

.item-details {
  flex: 1;
}

.item-name {
  font-weight: bold;
  margin-bottom: 5px;
}

.item-price {
  color: #f56c6c;
  font-size: 16px;
}

.item-quantity {
  margin: 0 15px;
}

.item-total {
  width: 100px;
  font-weight: bold;
  color: #f56c6c;
  margin: 0 15px;
}

.remove-btn, .favorite-btn {
  margin-left: 10px;
}

.cart-summary {
  text-align: right;
  padding: 20px;
  border-top: 1px solid #eee;
}

.summary-row {
  font-size: 18px;
  margin-bottom: 15px;
}

.total-amount {
  font-weight: bold;
  color: #f56c6c;
  font-size: 20px;
}

.checkout-btn {
  width: 200px;
  height: 45px;
  font-size: 16px;
}
</style>