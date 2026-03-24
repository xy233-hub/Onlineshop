<template>
  <div class="purchase-intents-page">
    <div class="header">
      <h2>购买意向记录</h2>
      <div style="color:#666">查看别人购买您商品的记录</div>
    </div>

    <el-table :data="intents" v-loading="loading" stripe>
      <el-table-column prop="purchase_id" label="意向 ID" width="100" />
      <el-table-column prop="product_id" label="商品 ID" width="100">
        <template #default="{ row }">
          <el-link 
            v-if="row.product_id" 
            type="primary" 
            @click="$router.push({ path: `/product/${row.product_id}` })"
          >
            {{ row.product_id }}
          </el-link>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="product_name" label="商品名称" />
      <el-table-column label="卖家" width="120">
        <template #default="{ row }">
          <span>{{ sellerMap[row.seller_id] || row.seller_id || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="customer_name" label="顾客姓名" width="120" />
      <el-table-column prop="customer_phone" label="联系电话" width="140" />
      <el-table-column prop="customer_address" label="收货地址" width="200" />
      <el-table-column prop="quantity" label="数量" width="80" />
      <el-table-column prop="total_amount" label="总额" width="120">
        <template #default="{ row }">¥{{ row.total_amount }}</template>
      </el-table-column>
      <el-table-column prop="purchase_status" label="状态" width="140">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.purchase_status)">
            {{ getStatusText(row.purchase_status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="created_at" label="提交时间" width="180">
        <template #default="{ row }">{{ formatTime(row.created_at) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="280">
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

          <!-- 开始发货 -->
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
              type="warning"
              size="small"
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

    <div class="pagination" v-if="total > size" style="margin-top:16px; text-align:right;">
      <el-pagination
          background
          :current-page="page"
          :page-size="size"
          :total="total"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
          :page-sizes="[10,20,50,100]"
          layout="sizes, prev, pager, next, jumper"
      />
    </div>

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
              :rows="3">
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

    <!-- 发货对话框 -->
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
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { logisticsAPI, purchaseAPI, sellerProductAPI, sellerAPI } from '@/api'
import { formatTime } from '@/utils'

const intents = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const sellerMap = ref({}) // 存储seller_id到username的映射

// 取消订单相关
const cancelDialogVisible = ref(false)
const cancelForm = ref({
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
const addTrackFormRef = ref()
const addTrackForm = reactive({
  purchase_id: null,
  track_content: '',
  track_location: '',
  track_status: '运输中'
})

const addTrackRules = {
  track_content: [{ required: true, message: '请填写轨迹内容', trigger: 'blur' }]
}

const extractData = (res) => res?.data?.data ?? null

const loadProviders = async () => {
  const res = await logisticsAPI.getProviders({ type: 'EXPRESS' })
  logisticsProviders.value = Array.isArray(res?.data?.data) ? res.data.data : []
}

const fetchIntents = async () => {
  loading.value = true
  try {
    // 获取当前登录买家的 ID
    const customerInfo = JSON.parse(localStorage.getItem('customer_info') || '{}')
    const customerId = customerInfo.customer_id
    
    if (!customerId) {
      ElMessage.error('未找到买家 ID，请重新登录')
      return
    }
    
    // 调用卖家接口，但传递买家 ID 作为筛选条件
    // 后端需要支持按 seller_id 筛选（买家发布的商品，seller_id = customer_id）
    const res = await purchaseAPI.getSellerPurchaseIntents({ 
      page: page.value, 
      size: size.value,
      seller_id: customerId  // 传递买家 ID 作为 seller_id
    })
    const d = extractData(res)
    if (!d) {
      intents.value = []
      total.value = 0
      return
    }
    intents.value = Array.isArray(d.items) ? d.items : (Array.isArray(d) ? d : [])
    total.value = Number(d.total ?? intents.value.length)
    
    // 提取所有唯一的 seller_id
    const sellerIds = [...new Set(intents.value.map(item => item.seller_id).filter(Boolean))]
    
    // 批量获取卖家信息
    for (const sellerId of sellerIds) {
      try {
        const sellerResponse = await sellerAPI.getSellerById(sellerId)
        if (sellerResponse.data.data?.username) {
          sellerMap.value[sellerId] = sellerResponse.data.data.username
        }
      } catch (sellerError) {
        console.error(`获取卖家 ${sellerId} 信息失败:`, sellerError)
      }
    }
  } catch (err) {
    console.error(err)
    ElMessage.error('获取购买意向失败')
    intents.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

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
      await fetchIntents()
    } else {
      ElMessage.error(res.data?.message || '更新失败')
      await fetchIntents()
    }
  } catch (err) {
    console.error(err)
    ElMessage.error('更新失败')
    await fetchIntents()
  }
}

const openShipDialog = (row) => {
  shipForm.purchase_id = row.purchase_id
  shipForm.logistics_provider_id = null
  shipForm.tracking_no = ''
  shipDialogVisible.value = true
  updateOrderStatus(row, 'SHIPPING_STARTED')
}

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
    await fetchIntents()
  } catch (e) {
    const msg = e?.response?.data?.message || '发货失败'
    ElMessage.error(msg)
  } finally {
    shippingSubmitting.value = false
  }
}

const openAddTrackDialog = (row) => {
  addTrackForm.purchase_id = row?.purchase_id ?? null
  addTrackForm.track_content = ''
  addTrackForm.track_location = ''
  addTrackForm.track_status = '运输中'
  addTrackDialogVisible.value = true
}

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
    await fetchIntents()
  } catch (e) {
    const msg = e?.response?.data?.message || '添加失败'
    ElMessage.error(msg)
  } finally {
    addTrackSubmitting.value = false
  }
}

const showCancelDialog = (row) => {
  cancelForm.value.purchaseId = row.purchase_id
  cancelForm.value.cancelReason = ''
  cancelForm.value.cancelNotes = ''
  cancelDialogVisible.value = true
}

const confirmCancelOrder = async () => {
  if (!cancelForm.value.cancelReason) {
    ElMessage.warning('请选择取消原因')
    return
  }

  try {
    const res = await purchaseAPI.updatePurchaseIntentStatus(
        cancelForm.value.purchaseId,
        {
          new_status: 'SELLER_CANCELLED',
          cancel_reason: cancelForm.value.cancelReason,
          cancel_notes: cancelForm.value.cancelNotes
        }
    )

    if (res.data && res.data.code === 200) {
      ElMessage.success('订单取消成功')
      cancelDialogVisible.value = false
      await fetchIntents()
    } else {
      ElMessage.error(res.data?.message || '取消订单失败')
      await fetchIntents()
    }
  } catch (err) {
    console.error('取消订单失败', err)
    ElMessage.error('取消订单失败')
    await fetchIntents()
  }
}

const handlePageChange = (p) => {
  page.value = p
  fetchIntents()
}

const handleSizeChange = (s) => {
  size.value = s
  page.value = 1
  fetchIntents()
}

const getStatusType = (status) => {
  const types = {
    'CUSTOMER_ORDERED': 'warning',
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
    'SELLER_CONFIRMED': '商家确认',
    'STOCK_PREPARED': '备货完成',
    'SHIPPING_STARTED': '开始发货',
    'COMPLETED': '交易完成',
    'CUSTOMER_CANCELLED': '客户取消',
    'SELLER_CANCELLED': '商家取消'
  }
  return texts[status] || (status || '')
}

onMounted(() => {
  loadProviders()
  fetchIntents()
})
</script>

<style scoped>
.purchase-intents-page {
  padding: 20px;
  background: #fff;
  border-radius: 6px;
}

.header {
  margin-bottom: 20px;
}

.header h2 {
  margin: 0 0 8px 0;
  font-size: 20px;
  color: #333;
}
</style>