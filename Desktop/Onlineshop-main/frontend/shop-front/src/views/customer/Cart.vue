<!-- vue -->
<!-- File: 'src/views/customer/Cart.vue' -->
<template>
  <div>
    <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:12px;">
      <h3>我的购物车</h3>
      <!-- 右上角按钮区域 -->
      <div>
        <el-button 
          type="danger" 
          size="small" 
          :disabled="cartItems.length === 0"
          @click="clearCart"
        >
          清空购物车
        </el-button>
      </div>
    </div>

    <el-table
        :data="cartItems"
        style="width:100%"
        row-key="cart_item_id"
    >
      <el-table-column prop="product_info.product_name" label="商品" />
      <el-table-column label="当前单价" width="120">
        <template #default="{ row }">
          ¥{{ row.product_info?.price ?? 0 }}
        </template>
      </el-table-column>
      <el-table-column label="数量" width="180">
        <template #default="{ row }">
          <el-input-number
            v-model="row.quantity"
            :min="1"
            :max="row.product_info?.stock_quantity ?? 99"
            size="small"
            @change="updateQuantity(row)"
          />
        </template>
      </el-table-column>
      <el-table-column label="小计" width="120">
        <template #default="{ row }">
          ¥{{ (row.product_info?.price * row.quantity ?? 0).toFixed(2) }}
        </template>
      </el-table-column>
      <el-table-column label="查看商品" width="140" align="center">
        <template #default="{ row }">
          <el-button
              type="primary"
              link
              size="small"
              @click="goToProduct(row)"
          >
            查看商品
          </el-button>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button type="danger" link @click="removeRow(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div style="display:flex; justify-content:flex-end; margin-top:12px; align-items:center;">
      <div style="margin-right:24px; font-size:16px; font-weight:500;">
        购物车总计：¥{{ cartTotal.toFixed(2) }}
      </div>
      <el-button type="primary" :disabled="cartItems.length === 0">
        结算
      </el-button>
    </div>

    <div style="display:flex; justify-content:flex-end; margin-top:12px;">
      <el-pagination
          v-if="total > 0"
          :current-page="page"
          :page-size="size"
          :total="total"
          @current-change="onPageChange"
          @size-change="onSizeChange"
          :page-sizes="[10,20,50]"
          layout="sizes, prev, pager, next, jumper"
          background
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { cartAPI } from '@/api'
import { useCustomerStore } from '@/stores/customer'
import { useRouter } from 'vue-router'

const store = useCustomerStore()
const router = useRouter()

const cartItems = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const loading = ref(false)

// 计算购物车总价
const cartTotal = computed(() => {
  return cartItems.value.reduce((total, item) => {
    return total + (item.product_info?.price * item.quantity ?? 0)
  }, 0)
})

const extractData = r => r?.data?.data ?? r?.data ?? null
const getCustomerId = () =>
    store.info?.customer_id ??
    (localStorage.getItem('customer_id') ? Number(localStorage.getItem('customer_id')) : (() => {
      const raw = localStorage.getItem('customer_info')
      if (!raw) return null
      try { const parsed = JSON.parse(raw); return Number(parsed?.customer_id) || null } catch { return null }
    })())

const fetch = async (p = page.value, s = size.value) => {
  const customerId = getCustomerId()
  if (!customerId) {
    ElMessage.error('未检测到用户信息')
    cartItems.value = []
    total.value = 0
    return
  }
  loading.value = true
  try {
    const res = await cartAPI.getCartItems({ customer_id: customerId, page: p, size: s })
    const payload = extractData(res)
    let list = payload?.items ?? payload ?? []
    if (!Array.isArray(list)) list = []
    cartItems.value = list.map(it => ({
      cart_item_id: it.cart_item_id ?? it.id,
      product_id: it.product_id ?? it.product?.product_id ?? it.product?.id,
      product_info: it.product_info ?? it.product ?? { product_name: it.product_name, price: it.price },
      quantity: it.quantity ?? 1
    }))
    total.value = Number(payload?.total ?? cartItems.value.length ?? 0)
    page.value = p
    size.value = s
  } catch (e) {
    console.error('获取购物车失败', e)
    ElMessage.error('获取购物车失败')
    cartItems.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const onPageChange = p => { fetch(p, size.value) }
const onSizeChange = s => { fetch(1, s) }

// 删除购物车商品
const removeRow = async (row) => {
  const customerId = getCustomerId()
  if (!customerId) {
    ElMessage.error('请先登录客户账号')
    return
  }
  if (!row?.cart_item_id) {
    ElMessage.error('记录信息不完整')
    return
  }

  try {
    const res = await cartAPI.removeCartItem(row.cart_item_id)
    const code = res?.data?.code
    const msg = res?.data?.message

    if (code === 200) {
      await fetch(page.value, size.value)
      ElMessage.success(msg || '删除购物车商品成功')
    } else {
      ElMessage.error(msg || '删除购物车商品失败')
    }
  } catch (e) {
    console.error('删除购物车商品失败:', e)
    ElMessage.error(e?.response?.data?.message || '删除购物车商品失败，接口可能尚未实现')
  }
}

// 更新购物车商品数量
const updateQuantity = async (row) => {
  const customerId = getCustomerId()
  if (!customerId) {
    ElMessage.error('请先登录客户账号')
    return
  }
  if (!row?.cart_item_id) {
    ElMessage.error('记录信息不完整')
    return
  }

  try {
    const res = await cartAPI.updateCartItem(row.cart_item_id, {
      customer_id: Number(customerId),
      quantity: row.quantity
    })
    const code = res?.data?.code
    const msg = res?.data?.message

    if (code === 200) {
      ElMessage.success(msg || '更新购物车商品数量成功')
    } else {
      ElMessage.error(msg || '更新购物车商品数量失败')
      // 如果更新失败，恢复原来的数量
      fetch(page.value, size.value)
    }
  } catch (e) {
    console.error('更新购物车商品数量失败:', e)
    ElMessage.error(e?.response?.data?.message || '更新购物车商品数量失败，接口可能尚未实现')
    // 如果更新失败，恢复原来的数量
    fetch(page.value, size.value)
  }
}

// 清空购物车
const clearCart = async () => {
  const customerId = getCustomerId()
  if (!customerId) {
    ElMessage.error('请先登录客户账号')
    return
  }

  try {
    await ElMessageBox.confirm('确定要清空购物车吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    // 批量删除购物车商品
    const itemIds = cartItems.value.map(item => item.cart_item_id)
    if (itemIds.length > 0) {
      const res = await cartAPI.removeCartItems({
        customer_id: Number(customerId),
        cart_item_ids: itemIds
      })
      const code = res?.data?.code
      const msg = res?.data?.message

      if (code === 200) {
        await fetch(page.value, size.value)
        ElMessage.success(msg || '清空购物车成功')
      } else {
        ElMessage.error(msg || '清空购物车失败')
      }
    } else {
      ElMessage.info('购物车已空')
    }
  } catch (e) {
    if (e === 'cancel') return
    console.error('清空购物车失败:', e)
    ElMessage.error(e?.response?.data?.message || '清空购物车失败，接口可能尚未实现')
  }
}

// 跳转到商品详情
const goToProduct = (row) => {
  const id = row.product_id || row.product_info?.product_id || row.product_info?.id

  if (!id) {
    return ElMessage.warning('无可跳转的商品信息')
  }
  router.push({ path: `/product/${id}` })
}

onMounted(() => {
  fetch()
})
</script>

<style scoped>
/* 可以根据需要添加样式 */
</style>