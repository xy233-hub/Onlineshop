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
            :disabled="!selectedIds.length || promoPreviewLoading"
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
          <template v-if="hasPromotionPreview(row) && getSettledUnitPrice(row) < toPrice(row.unit_price)">
            <div class="price-original">￥{{ toPrice(row.unit_price).toFixed(2) }}</div>
            <div class="price-final">￥{{ getSettledUnitPrice(row).toFixed(2) }}</div>
          </template>
          <template v-else>
            ￥{{ toPrice(row.unit_price).toFixed(2) }}
          </template>
        </template>
      </el-table-column>

      <el-table-column label="数量" width="120" align="center">
        <template #default="{ row }">
          {{ row.quantity }}
        </template>
      </el-table-column>

      <el-table-column label="小计" width="140" align="center">
        <template #default="{ row }">
          <template v-if="hasPromotionPreview(row) && getSettledUnitPrice(row) < toPrice(row.unit_price)">
            <div class="price-original">￥{{ (toPrice(row.unit_price) * row.quantity).toFixed(2) }}</div>
            <div class="price-final">￥{{ (getSettledUnitPrice(row) * row.quantity).toFixed(2) }}</div>
          </template>
          <template v-else>
            ￥{{ (toPrice(row.unit_price) * row.quantity).toFixed(2) }}
          </template>
        </template>
      </el-table-column>

      <el-table-column label="优惠信息" min-width="220" align="center">
        <template #default="{ row }">
          <span v-if="!selectedIds.includes(row.cart_item_id)" class="promotion-muted">勾选后计算</span>
          <span v-else-if="promotionPreviewByCartItemId[row.cart_item_id]?.loading" class="promotion-muted">计算中...</span>
          <span v-else-if="promotionPreviewByCartItemId[row.cart_item_id]">
            <el-tag
              :type="promotionPreviewByCartItemId[row.cart_item_id].applied ? 'success' : 'info'"
              size="small"
            >
              {{ promotionPreviewByCartItemId[row.cart_item_id].text }}
            </el-tag>
          </span>
          <span v-else class="promotion-muted">暂无</span>
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
        <span v-if="selectedTotalDiscount > 0" class="discount-amount">（已优惠 ￥{{ selectedTotalDiscount.toFixed(2) }}）</span>
        <span v-if="orderLevelPromotionText" class="order-level-text">（{{ orderLevelPromotionText }}）</span>
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
          <span v-if="selectedTotalDiscount > 0" class="discount-amount">（已优惠 ￥{{ selectedTotalDiscount.toFixed(2) }}）</span>
          <span v-if="orderLevelPromotionText" class="order-level-text">（{{ orderLevelPromotionText }}）</span>
        </el-form-item>
        <el-form-item label="优惠明细" v-if="selectedPreviewItems.length">
          <div class="promotion-lines">
            <div
              v-for="item in selectedPreviewItems"
              :key="item.cart_item_id"
              class="promotion-line"
            >
              <span>{{ item.product_name }}</span>
              <span :class="item.applied ? 'promotion-applied' : 'promotion-muted'">{{ item.text }}</span>
            </div>
          </div>
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
const promotionPreviewByCartItemId = ref({})
const promoPreviewLoading = ref(false)
const promoRequestVersion = ref(0)
const previewSummary = ref({
  selected_count: 0,
  total_amount: null,
  discount_amount: 0,
  order_level_promotions: []
})

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
  if (
    previewSummary.value.selected_count === selectedIds.value.length &&
    typeof previewSummary.value.total_amount === 'number'
  ) {
    return previewSummary.value.total_amount
  }
  const idSet = new Set(selectedIds.value)
  return cartItems.value
      .filter(i => idSet.has(i.cart_item_id))
      .reduce((sum, i) => {
        const price = getSettledUnitPrice(i)
        return sum + price * i.quantity
      }, 0)
})

const selectedTotalDiscount = computed(() => {
  if (
    previewSummary.value.selected_count === selectedIds.value.length &&
    typeof previewSummary.value.discount_amount === 'number'
  ) {
    return previewSummary.value.discount_amount
  }
  const idSet = new Set(selectedIds.value)
  return cartItems.value
      .filter(i => idSet.has(i.cart_item_id))
      .reduce((sum, i) => {
        const original = toPrice(i.unit_price)
        const settled = getSettledUnitPrice(i)
        const discount = Math.max(0, (original - settled) * i.quantity)
        return sum + discount
      }, 0)
})

const orderLevelPromotionText = computed(() => {
  const rows = Array.isArray(previewSummary.value.order_level_promotions)
    ? previewSummary.value.order_level_promotions
    : []
  if (!rows.length) return ''
  const total = rows.reduce((sum, row) => sum + toPrice(row.discount_amount), 0)
  if (total <= 0) return ''
  return `订单级优惠 -￥${total.toFixed(2)}`
})

const selectedPreviewItems = computed(() => {
  const idSet = new Set(selectedIds.value)
  return cartItems.value
      .filter(i => idSet.has(i.cart_item_id))
      .map(i => {
        const preview = promotionPreviewByCartItemId.value[i.cart_item_id]
        return {
          cart_item_id: i.cart_item_id,
          product_name: i.product_name,
          applied: !!preview?.applied,
          text: preview?.text || '待计算'
        }
      })
})

const toPrice = (value) => {
  const n = Number(value)
  return Number.isFinite(n) ? n : 0
}

const getSettledUnitPrice = (row) => {
  const preview = promotionPreviewByCartItemId.value[row.cart_item_id]
  if (preview && typeof preview.final_unit_price === 'number' && preview.final_unit_price >= 0) {
    return preview.final_unit_price
  }
  return toPrice(row.unit_price)
}

const hasPromotionPreview = (row) => {
  return !!promotionPreviewByCartItemId.value[row.cart_item_id]
}

const mapSkippedReasonText = (reason) => {
  switch (reason) {
    case 'PROMOTION_PREVIEW_FAILED':
      return '优惠计算失败'
    case 'PROMOTION_DATA_UNAVAILABLE':
      return '优惠数据不可用'
    case 'SAME_PROMOTION_RULE_ALREADY_APPLIED':
      return '同规则已触发'
    case 'PROMOTION_NOT_BETTER':
      return '无更优优惠'
    case 'NO_ACTIVE_PROMOTION':
      return '当前无活动'
    default:
      return '未命中优惠'
  }
}

const refreshPromotionPreviewForRows = async (rows) => {
  const currentVersion = ++promoRequestVersion.value
  const selectedMap = { ...promotionPreviewByCartItemId.value }

  rows.forEach(row => {
    selectedMap[row.cart_item_id] = {
      loading: true,
      applied: false,
      promotion_id: null,
      final_unit_price: toPrice(row.unit_price),
      discount_amount: 0,
      skipped_reason: null,
      text: '计算中...'
    }
  })
  promotionPreviewByCartItemId.value = selectedMap

  if (!rows.length) {
    promotionPreviewByCartItemId.value = {}
    previewSummary.value = { selected_count: 0, total_amount: null, discount_amount: 0, order_level_promotions: [] }
    return
  }

  promoPreviewLoading.value = true
  try {
    const { data } = await cartAPI.batchPurchasePreview({
      customer_id: customerId,
      cart_item_ids: rows.map(r => r.cart_item_id)
    })

    if (data?.code !== 200 || !data?.data) {
      throw new Error(data?.message || '预结算失败')
    }

    if (currentVersion !== promoRequestVersion.value) return

    const nextMap = {}
    const settlementRows = Array.isArray(data.data.promotion_settlement) ? data.data.promotion_settlement : []
    settlementRows.forEach(item => {
      const discount = toPrice(item.discount_amount)
      const skippedReason = item.skipped_reason || null
      const hasOrderLevel = !!item.order_level_applied
      let text
      if (discount > 0) {
        text = hasOrderLevel ? `已优惠(含订单级) -￥${discount.toFixed(2)}` : `已优惠 -￥${discount.toFixed(2)}`
      } else {
        text = mapSkippedReasonText(skippedReason)
      }

      nextMap[item.cart_item_id] = {
        loading: false,
        applied: !!item.applied || discount > 0,
        promotion_id: item.promotion_id ?? null,
        final_unit_price: toPrice(item.final_unit_price),
        discount_amount: discount,
        skipped_reason: skippedReason,
        order_level_applied: hasOrderLevel,
        text
      }
    })

    for (const row of rows) {
      if (!nextMap[row.cart_item_id]) {
        nextMap[row.cart_item_id] = {
          loading: false,
          applied: false,
          promotion_id: null,
          final_unit_price: toPrice(row.unit_price),
          discount_amount: 0,
          skipped_reason: 'PROMOTION_PREVIEW_FAILED',
          order_level_applied: false,
          text: '优惠计算失败'
        }
      }
    }

    promotionPreviewByCartItemId.value = nextMap
    previewSummary.value = {
      selected_count: Number(data.data.selected_count) || rows.length,
      total_amount: toPrice(data.data.total_amount),
      discount_amount: toPrice(data.data.discount_amount),
      order_level_promotions: Array.isArray(data.data.order_level_promotions) ? data.data.order_level_promotions : []
    }
  } catch (e) {
    if (currentVersion === promoRequestVersion.value) {
      const fallbackMap = {}
      for (const row of rows) {
        fallbackMap[row.cart_item_id] = {
          loading: false,
          applied: false,
          promotion_id: null,
          final_unit_price: toPrice(row.unit_price),
          discount_amount: 0,
          skipped_reason: 'PROMOTION_PREVIEW_FAILED',
          order_level_applied: false,
          text: '优惠计算失败'
        }
      }
      promotionPreviewByCartItemId.value = fallbackMap
      previewSummary.value = {
        selected_count: rows.length,
        total_amount: null,
        discount_amount: 0,
        order_level_promotions: []
      }
    }
  } finally {
    if (currentVersion === promoRequestVersion.value) {
      promoPreviewLoading.value = false
    }
  }
}

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
      promotionPreviewByCartItemId.value = {}
      previewSummary.value = { selected_count: 0, total_amount: null, discount_amount: 0, order_level_promotions: [] }
      selectedIds.value = []
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
  refreshPromotionPreviewForRows(rows)
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
      const settlement = Array.isArray(data?.data?.promotion_settlement) ? data.data.promotion_settlement : []
      const discountTotal = toPrice(data?.data?.discount_amount) || settlement.reduce((sum, item) => sum + toPrice(item.discount_amount), 0)
      ElMessage.success('下单成功')
      if (discountTotal > 0) {
        ElMessage.info(`本次已优惠 ￥${discountTotal.toFixed(2)}`)
      }
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
.discount-amount {
  margin-left: 6px;
  color: #67c23a;
  font-size: 13px;
}
.order-level-text {
  margin-left: 6px;
  color: #409eff;
  font-size: 13px;
}
.price-original {
  color: #909399;
  text-decoration: line-through;
  font-size: 12px;
}
.price-final {
  color: #f56c6c;
  font-weight: 600;
}
.promotion-muted {
  color: #909399;
  font-size: 12px;
}
.promotion-lines {
  width: 100%;
  max-height: 180px;
  overflow-y: auto;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 6px 10px;
}
.promotion-line {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  line-height: 24px;
}
.promotion-applied {
  color: #67c23a;
}
</style>
