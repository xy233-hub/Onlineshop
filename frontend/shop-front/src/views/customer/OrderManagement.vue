<template>
  <div class="customer-order-management">
    <el-card>
      <template #header>
        <h2>购买意向管理</h2>
      </template>

      <!-- 筛选栏 -->
      <div class="filter-bar">
        <el-input
          v-model="searchForm.q"
          placeholder="搜索商品名称或顾客姓名"
          clearable
          style="width: 200px"
          @clear="handleSearch"
        />
        <el-select
          v-model="searchForm.status"
          placeholder="意向状态"
          clearable
          style="width: 150px; margin-left: 10px"
          @change="handleSearch"
        >
          <el-option label="待支付" value="CUSTOMER_ORDERED" />
          <el-option label="已付款" value="PAID" />
          <el-option label="商家确认" value="SELLER_CONFIRMED" />
          <el-option label="备货完成" value="STOCK_PREPARED" />
          <el-option label="运输中" value="SHIPPING_STARTED" />
          <el-option label="已完成" value="COMPLETED" />
          <el-option label="已取消" value="CANCELLED" />
        </el-select>
        <el-button type="primary" style="margin-left: 10px" @click="handleSearch">搜索</el-button>
      </div>

      <!-- 购买意向列表 -->
      <el-table
        v-loading="loading"
        :data="orders"
        stripe
        style="width: 100%; margin-top: 20px"
      >
        <el-table-column prop="purchase_id" label="意向 ID" width="100" />
        <el-table-column label="商品信息" min-width="200">
          <template #default="{ row }">
            <div v-if="row.items && row.items.length">
              <span
                v-for="(it, idx) in row.items"
                :key="it.item_id ?? `${row.purchase_id}-${idx}`"
                style="display:inline-flex; align-items:center; gap:8px; margin-right:8px;"
              >
                <el-link
                  v-if="it.product_id"
                  type="primary"
                  @click="$router.push({ path: `/product/${it.product_id}` })"
                >
                  {{ it.product_name || `商品 #${it.product_id}` }}
                </el-link>
                <el-text v-else size="small">-</el-text>
                <span v-if="idx < row.items.length - 1" style="color:#999">,</span>
              </span>
            </div>
            <div v-else-if="row.product_id">
              <el-link type="primary" @click="$router.push({ path: `/product/${row.product_id}` })">
                {{ row.product_name || `商品 #${row.product_id}` }}
              </el-link>
            </div>
            <div v-else>
              <el-text type="info" size="small">无</el-text>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="customer_username" label="顾客姓名" width="120" />
        <el-table-column prop="customer_phone" label="联系电话" width="140" />
        <el-table-column prop="quantity" label="数量" width="80" />
        <el-table-column prop="total_amount" label="金额" width="120">
          <template #default="{ row }">¥{{ row.total_amount }}</template>
        </el-table-column>
        <el-table-column prop="purchase_status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.purchase_status)">
              {{ getStatusText(row.purchase_status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="created_at" label="提交时间" width="180">
          <template #default="{ row }">{{ formatTime(row.created_at) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <!-- 确认订单 -->
            <el-button
              v-if="row.purchase_status === 'CUSTOMER_ORDERED'"
              size="small"
              type="success"
              @click="updateOrderStatus(row, 'SELLER_CONFIRMED')"
            >
              确认订单
            </el-button>

            <!-- 备货完成 -->
            <el-button
              v-if="row.purchase_status === 'SELLER_CONFIRMED'"
              size="small"
              type="success"
              @click="updateOrderStatus(row, 'STOCK_PREPARED')"
            >
              备货完成
            </el-button>

            <!-- 填写物流并发货 -->
            <el-button
              v-if="row.purchase_status === 'STOCK_PREPARED'"
              size="small"
              type="success"
              @click="openShipDialog(row)"
            >
              填写物流并发货
            </el-button>

            <!-- 添加物流轨迹 -->
            <el-button
              v-if="row.purchase_status === 'SHIPPING_STARTED'"
              size="small"
              type="warning"
              @click="openAddTrackDialog(row)"
            >
              添加物流轨迹
            </el-button>

            <!-- 取消订单 -->
            <el-button
              v-if="['CUSTOMER_ORDERED', 'SELLER_CONFIRMED', 'STOCK_PREPARED'].includes(row.purchase_status)"
              size="small"
              type="danger"
              @click="showCancelDialog(row)"
            >
              取消订单
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 取消订单对话框 -->
    <el-dialog v-model="cancelDialogVisible" title="取消订单" width="500px">
      <el-form :model="cancelForm" label-width="100px">
        <el-form-item label="取消原因">
          <el-select v-model="cancelForm.cancelReason" placeholder="请选择取消原因">
            <el-option label="缺货" value="缺货"></el-option>
            <el-option label="客户信息有误" value="客户信息有误"></el-option>
            <el-option label="价格调整" value="价格调整"></el-option>
            <el-option label="商品停产" value="商品停产"></el-option>
            <el-option label="其他原因" value="其他原因"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="取消备注">
          <el-input
            v-model="cancelForm.cancelNotes"
            type="textarea"
            placeholder="请输入取消备注（可选）"
            :rows="3"
          >
          </el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="cancelDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmCancelOrder">确认取消</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 填写物流对话框 -->
    <el-dialog
      v-model="shipDialogVisible"
      title="填写物流并发货"
      width="520px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="shipFormRef"
        :model="shipForm"
        :rules="shipRules"
        label-width="110px"
      >
        <el-form-item label="订单 ID">
          <el-input v-model="shipForm.purchase_id" disabled />
        </el-form-item>

        <el-form-item label="物流商" prop="logistics_provider_id">
          <el-select v-model="shipForm.logistics_provider_id" placeholder="请选择物流商" filterable>
            <el-option
              v-for="p in logisticsProviders"
              :key="p.provider_id"
              :label="p.provider_name"
              :value="p.provider_id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="运单号" prop="tracking_no">
          <el-input v-model="shipForm.tracking_no" placeholder="请输入运单号" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="shipDialogVisible = false" :disabled="shippingSubmitting">取消</el-button>
        <el-button type="primary" @click="submitShip" :loading="shippingSubmitting">
          确认发货
        </el-button>
      </template>
    </el-dialog>

    <!-- 添加物流轨迹对话框 -->
    <el-dialog
      v-model="addTrackDialogVisible"
      title="添加物流轨迹"
      width="520px"
      destroy-on-close
    >
      <el-form
        ref="addTrackFormRef"
        :model="addTrackForm"
        :rules="addTrackRules"
        label-width="110px"
        @submit.prevent
      >
        <el-form-item label="订单 ID">
          <el-input v-model="addTrackForm.purchase_id" disabled />
        </el-form-item>

        <el-form-item label="轨迹内容" prop="track_content">
          <el-input
            v-model="addTrackForm.track_content"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="例如：快件已从杭州集散中心发出，下一站北京"
          />
        </el-form-item>

        <el-form-item label="发生地点" prop="track_location">
          <el-input
            v-model="addTrackForm.track_location"
            maxlength="50"
            placeholder="例如：杭州市"
          />
        </el-form-item>

        <el-form-item label="物流状态" prop="track_status">
          <el-select v-model="addTrackForm.track_status" placeholder="请选择">
            <el-option label="已揽收" value="已揽收" />
            <el-option label="运输中" value="运输中" />
            <el-option label="派送中" value="派送中" />
            <el-option label="已签收" value="已签收" />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="addTrackDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="addTrackSubmitting" @click="submitAddTrack">
          提交
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { purchaseAPI, logisticsAPI } from '@/api'

const page = ref(1)
const size = ref(20)
const total = ref(0)
const orders = ref([])
const loading = ref(false)

const searchForm = reactive({
  q: '',
  status: ''
})

// 取消订单相关
const cancelDialogVisible = ref(false)
const cancelForm = reactive({
  purchaseId: null,
  cancelReason: '',
  cancelNotes: ''
})

// 发货相关
const shipDialogVisible = ref(false)
const shippingSubmitting = ref(false)
const shipFormRef = ref(null)
const logisticsProviders = ref([])

const shipForm = reactive({
  purchase_id: null,
  logistics_provider_id: null,
  tracking_no: ''
})

const shipRules = {
  logistics_provider_id: [{ required: true, message: '请选择物流商', trigger: 'change' }],
  tracking_no: [{ required: true, message: '请输入运单号', trigger: 'blur' }]
}

// 添加物流轨迹相关
const addTrackDialogVisible = ref(false)
const addTrackSubmitting = ref(false)
const addTrackFormRef = ref(null)

const addTrackForm = reactive({
  purchase_id: null,
  track_content: '',
  track_location: '',
  track_status: '运输中'
})

const addTrackRules = {
  track_content: [{ required: true, message: '请填写轨迹内容', trigger: 'blur' }]
}

const extractData = (res) => res?.data?.data ?? res?.data ?? null

const fetchOrders = async (p = page.value, s = size.value) => {
  loading.value = true
  try {
    const params = { page: p, size: s }
    if (searchForm.q) params.q = searchForm.q
    if (searchForm.status) params.status = searchForm.status

    // 调用卖家购买意向接口，查看别人买自己商品的意向
    const res = await purchaseAPI.getSellerPurchaseIntents(params)
    const d = extractData(res)
    
    if (!d) {
      orders.value = []
      total.value = 0
      return
    }

    orders.value = Array.isArray(d.items) ? d.items : (Array.isArray(d) ? d : [])
    total.value = Number(d.total ?? orders.value.length)
  } catch (err) {
    console.error(err)
    ElMessage.error('获取购买意向列表失败')
    orders.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  page.value = 1
  fetchOrders()
}

const handlePageChange = (p) => {
  page.value = p
  fetchOrders()
}

const handleSizeChange = (s) => {
  size.value = s
  page.value = 1
  fetchOrders()
}

// 更新订单状态
const updateOrderStatus = async (order, newStatus) => {
  try {
    let sellerNotes = null

    if (newStatus === 'SELLER_CONFIRMED') {
      sellerNotes = '商家已确认订单'
    } else if (newStatus === 'STOCK_PREPARED') {
      sellerNotes = '备货已完成'
    }

    const payload = { new_status: newStatus }
    if (sellerNotes) payload.seller_notes = sellerNotes

    const res = await purchaseAPI.updatePurchaseIntentStatus(order.purchase_id, payload)
    
    if (res.data && res.data.code === 200) {
      ElMessage.success('状态更新成功')
      await fetchOrders()
    } else {
      ElMessage.error(res.data?.message || '更新失败')
      await fetchOrders()
    }
  } catch (err) {
    console.error(err)
    ElMessage.error('更新失败')
    await fetchOrders()
  }
}

// 显示取消订单对话框
const showCancelDialog = (row) => {
  cancelForm.purchaseId = row.purchase_id
  cancelForm.cancelReason = ''
  cancelForm.cancelNotes = ''
  cancelDialogVisible.value = true
}

// 确认取消订单
const confirmCancelOrder = async () => {
  if (!cancelForm.cancelReason) {
    ElMessage.warning('请选择取消原因')
    return
  }

  try {
    const res = await purchaseAPI.updatePurchaseIntentStatus(
      cancelForm.purchaseId,
      {
        new_status: 'SELLER_CANCELLED',
        cancel_reason: cancelForm.cancelReason,
        cancel_notes: cancelForm.cancelNotes
      }
    )

    if (res.data && res.data.code === 200) {
      ElMessage.success('订单取消成功')
      cancelDialogVisible.value = false
      await fetchOrders()
    } else {
      ElMessage.error(res.data?.message || '取消订单失败')
      await fetchOrders()
    }
  } catch (err) {
    console.error('取消订单失败', err)
    ElMessage.error('取消订单失败')
    await fetchOrders()
  }
}

// 加载物流商列表
const loadProviders = async () => {
  try {
    const res = await logisticsAPI.getProviders({ type: 'EXPRESS' })
    logisticsProviders.value = Array.isArray(res?.data?.data) ? res.data.data : []
  } catch (e) {
    // 忽略错误
  }
}

// 打开发货对话框
const openShipDialog = (row) => {
  shipForm.purchase_id = row.purchase_id
  shipForm.logistics_provider_id = null
  shipForm.tracking_no = ''
  shipDialogVisible.value = true
}

// 提交发货
const submitShip = async () => {
  if (!shipFormRef.value) return
  await shipFormRef.value.validate()

  shippingSubmitting.value = true
  try {
    const purchaseId = shipForm.purchase_id
    const payload = {
      logistics_provider_id: shipForm.logistics_provider_id,
      tracking_no: shipForm.tracking_no
    }

    const res = await logisticsAPI.shipOrder(purchaseId, payload)
    ElMessage.success(res?.data?.message || '发货成功')
    shipDialogVisible.value = false
    await fetchOrders()
  } catch (e) {
    const msg = e?.response?.data?.message || '发货失败'
    ElMessage.error(msg)
  } finally {
    shippingSubmitting.value = false
  }
}

// 打开添加物流轨迹对话框
const openAddTrackDialog = (row) => {
  addTrackForm.purchase_id = row?.purchase_id ?? null
  addTrackForm.track_content = ''
  addTrackForm.track_location = ''
  addTrackForm.track_status = '运输中'

  addTrackDialogVisible.value = true

  nextTick(() => {
    addTrackFormRef.value?.clearValidate?.()
  })
}

// 提交添加物流轨迹
const submitAddTrack = async () => {
  if (!addTrackForm.purchase_id) {
    ElMessage.error('订单 ID 无效')
    return
  }

  await addTrackFormRef.value?.validate?.()

  addTrackSubmitting.value = true
  try {
    const payload = {
      track_content: addTrackForm.track_content,
      track_location: addTrackForm.track_location || null,
      track_status: addTrackForm.track_status || null
    }

    await logisticsAPI.addLogisticsTrack(addTrackForm.purchase_id, payload)

    ElMessage.success('物流轨迹添加成功')
    addTrackDialogVisible.value = false
    await fetchOrders()
  } catch (e) {
    const msg = e?.response?.data?.message || '添加失败'
    ElMessage.error(msg)
  } finally {
    addTrackSubmitting.value = false
  }
}

const getStatusType = (status) => {
  const types = {
    'CUSTOMER_ORDERED': 'warning',
    'PAID': 'warning',
    'SELLER_CONFIRMED': 'primary',
    'STOCK_PREPARED': 'primary',
    'SHIPPING_STARTED': 'primary',
    'COMPLETED': 'success',
    'CUSTOMER_CANCELLED': 'danger',
    'SELLER_CANCELLED': 'danger'
  }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = {
    'CUSTOMER_ORDERED': '待支付',
    'PAID': '已付款',
    'SELLER_CONFIRMED': '商家确认',
    'STOCK_PREPARED': '备货完成',
    'SHIPPING_STARTED': '运输中',
    'COMPLETED': '已完成',
    'CUSTOMER_CANCELLED': '已取消',
    'SELLER_CANCELLED': '已取消'
  }
  return texts[status] || status
}

const formatTime = (time) => {
  if (!time) return '-'
  const date = new Date(time)
  return date.toLocaleString('zh-CN', { hour12: false })
}

onMounted(() => {
  loadProviders()
  fetchOrders()
})
</script>

<style scoped>
.customer-order-management {
  padding: 20px;
}

.filter-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 20px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>