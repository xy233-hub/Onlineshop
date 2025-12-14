<!-- language: vue -->
<!-- File: src/views/customer/CustomerCart.vue -->
<template>
  <div class="customer-cart-page">
    <div class="cart-header">
      <h2>我的购物车</h2>
      <div class="cart-actions">
        <!-- 批量下单 -->
        <el-button
            type="primary"
            :disabled="!selectedIds.length"
            @click="onBatchPurchase"
        >
          下单
        </el-button>
        <!-- 批量转收藏 -->
        <el-button
            type="success"
            :disabled="!selectedIds.length"
            @click="onBatchConvertFavorite"
        >
          批量转收藏
        </el-button>
        <!-- 批量删除 -->
        <el-button
            type="danger"
            :disabled="!selectedIds.length"
            @click="onBatchDelete"
        >
          批量删除
        </el-button>
      </div>
    </div>

    <el-table
        v-loading="loading"
        :data="cartItems"
        border
        @selection-change="onSelectionChange"
    >
      <el-table-column type="selection" width="50" />

      <el-table-column prop="product_name" label="商品名称" min-width="220" />

      <el-table-column label="单价" width="120" align="center">
        <template #default="{ row }">
          ￥{{ row.unit_price.toFixed(2) }}
        </template>
      </el-table-column>

      <el-table-column label="数量" width="120" align="center">
        <template #default="{ row }">
          {{ row.quantity }}
        </template>
      </el-table-column>

      <el-table-column label="小计" width="140" align="center">
        <template #default="{ row }">
          ￥{{ (row.unit_price * row.quantity).toFixed(2) }}
        </template>
      </el-table-column>

      <el-table-column label="状态/库存" min-width="160" align="center">
        <template #default="{ row }">
          <div>
            <el-tag
                :type="row.product_status === 'online' ? 'success' : 'info'"
                size="small"
            >
              {{ statusText(row.product_status) }}
            </el-tag>
          </div>
          <div style="margin-top: 4px; font-size: 12px; color: #909399">
            库存：{{ row.stock_quantity }}
          </div>
        </template>
      </el-table-column>

      <el-table-column label="创建时间" min-width="180" align="center">
        <template #default="{ row }">
          {{ row.created_at }}
        </template>
      </el-table-column>

      <el-table-column label="操作" width="120" fixed="right" align="center">
        <template #default="{ row }">
          <el-button
              type="danger"
              link
              size="small"
              @click="onDeleteItem(row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="cart-footer">
      <div class="left">
        已选 {{ selectedIds.length }} 件商品，合计：
        <span class="total-amount">
          ￥{{ selectedTotalAmount.toFixed(2) }}
        </span>
      </div>
      <div class="right">
        <el-pagination
            background
            layout="prev, pager, next, jumper"
            :current-page="pagination.page"
            :page-size="pagination.size"
            :total="pagination.total"
            @current-change="onPageChange"
        />
      </div>
    </div>

    <el-dialog
        v-model="purchaseDialogVisible"
        title="确认下单信息"
        width="480px"
    >
      <el-form :model="purchaseForm" label-width="90px">
        <el-form-item label="收货人">
          <el-input v-model="purchaseForm.contact_name" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="purchaseForm.contact_phone" />
        </el-form-item>
        <el-form-item label="收货地址">
          <el-input v-model="purchaseForm.delivery_address" />
        </el-form-item>
        <el-form-item label="订单备注">
          <el-input
              v-model="purchaseForm.note"
              type="textarea"
              :rows="3"
          />
        </el-form-item>
        <el-form-item label="本次金额">
          <span class="total-amount">
            ￥{{ selectedTotalAmount.toFixed(2) }}
          </span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="purchaseDialogVisible = false">取消</el-button>
        <el-button
            type="primary"
            :loading="purchaseLoading"
            @click="doBatchPurchase"
        >
          提交订单
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { cartAPI } from '@/api/index'

const loading = ref(false)
const cartItems = ref([])
const selectedIds = ref([])

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const purchaseDialogVisible = ref(false)
const purchaseLoading = ref(false)
const purchaseForm = reactive({
  contact_name: '',
  contact_phone: '',
  delivery_address: '',
  note: ''
})

const customerInfo = (() => {
  try {
    return JSON.parse(localStorage.getItem('customer_info') || '{}')
  } catch {
    return {}
  }
})()
const customerId = customerInfo.customer_id

const selectedTotalAmount = computed(() => {
  const idSet = new Set(selectedIds.value)
  return cartItems.value
      .filter(i => idSet.has(i.cart_item_id))
      .reduce((sum, i) => {
        const price = i.unit_price || 0
        return sum + price * i.quantity
      }, 0)
})

const statusText = (status) => {
  switch (status) {
    case 'online': return '在售'
    case 'sold': return '已售出'
    case 'outOfStock': return '缺货'
    case 'frozen': return '已下架'
    default: return status || '未知'
  }
}

const fetchCart = async () => {
  if (!customerId) {
    ElMessage.error('未登录客户，无法加载购物车')
    return
  }
  loading.value = true
  try {
    const { data } = await cartAPI.getCartItems({
      customer_id: customerId,
      page: pagination.page,
      size: pagination.size
    })
    if (data.code === 200 && data.data) {
      const pageData = data.data
      cartItems.value = pageData.items || []
      pagination.page = pageData.page || 1
      pagination.size = pageData.size || 10
      pagination.total = pageData.total || 0
    } else {
      ElMessage.error(data.message || '加载购物车失败')
    }
  } catch (e) {
    ElMessage.error('加载购物车失败')
  } finally {
    loading.value = false
  }
}

const onSelectionChange = (rows) => {
  selectedIds.value = rows.map(r => r.cart_item_id)
}

const onPageChange = (page) => {
  pagination.page = page
  fetchCart()
}


// 单条删除（统一走批量接口）
const onDeleteItem = async (row) => {
  try {
    await cartAPI.removeCartItems({
      customer_id: Number(customerId),
      cart_item_ids: [row.cart_item_id] // 数组，不是字符串
    })
    ElMessage.success('删除成功')
    fetchCart()
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

// 批量删除
const onBatchDelete = async () => {
  if (!selectedIds.value.length) {
    ElMessage.warning('请先选择要删除的商品')
    return
  }
  try {
    await cartAPI.removeCartItems({
      customer_id: Number(customerId),
      cart_item_ids: selectedIds.value // 直接数组
    })
    ElMessage.success('批量删除成功')
    selectedIds.value = []
    fetchCart()
  } catch (e) {
    ElMessage.error('批量删除失败')
  }
}

const onBatchConvertFavorite = async () => {
  if (!selectedIds.value.length) return
  try {
    const { data } = await cartAPI.batchConvertToFavorites({
      customer_id: customerId,
      cart_item_ids: selectedIds.value
    })
    if (data.code === 200) {
      ElMessage.success('转收藏成功')
      selectedIds.value = []
      fetchCart()
    } else {
      ElMessage.error(data.message || '转收藏失败')
    }
  } catch (e) {
    ElMessage.error('转换收藏失败')
  }
}

const onBatchPurchase = () => {
  if (!selectedIds.value.length) {
    ElMessage.warning('请先选择要下单的商品')
    return
  }
  purchaseForm.contact_name = customerInfo.username || ''
  purchaseForm.contact_phone = customerInfo.phone || ''
  purchaseForm.delivery_address = customerInfo.default_address || ''
  purchaseForm.note = ''
  purchaseDialogVisible.value = true
}

const doBatchPurchase = async () => {
  if (!selectedIds.value.length) return
  purchaseLoading.value = true
  try {
    const payload = {
      customer_id: customerId,
      cart_item_ids: selectedIds.value,
      contact_name: purchaseForm.contact_name || undefined,
      contact_phone: purchaseForm.contact_phone || undefined,
      delivery_address: purchaseForm.delivery_address || undefined,
      note: purchaseForm.note || undefined
    }
    const { data } = await cartAPI.batchPurchase(payload)
    if (data.code === 200) {
      ElMessage.success('下单成功')
      purchaseDialogVisible.value = false
      selectedIds.value = []
      fetchCart()
    } else {
      ElMessage.error(data.message || '下单失败')
    }
  } catch (e) {
    ElMessage.error('下单失败')
  } finally {
    purchaseLoading.value = false
  }
}

onMounted(() => {
  fetchCart()
})
</script>

<style scoped>
.customer-cart-page {
  padding: 8px;
}
.cart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.cart-actions > * + * {
  margin-left: 8px;
}
.cart-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16px;
}
.total-amount {
  color: #f56c6c;
  font-weight: 600;
  font-size: 16px;
}
</style>
