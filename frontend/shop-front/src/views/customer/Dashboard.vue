<template>
  <div class="customer-dashboard">
    <el-card>
      <div class="header-row">
        <h2>历史订单</h2>
        <el-button type="primary" @click="$router.push('/')">返回商城首页</el-button>
      </div>

      <div style="margin-top:12px;">
        <div v-if="!items.length && !loading" class="empty-wrap">
          <el-empty description="暂无购买意向">
            <el-button type="primary" @click="$router.push('/')">去逛逛</el-button>
          </el-empty>
        </div>

        <div v-else>
          <div class="intent-list">
            <el-card
                v-for="row in items"
                :key="row.purchase_id"
                class="intent-card"
                shadow="hover"
            >
              <div class="intent-grid">
                <div class="left">
                  <div class="thumb" v-if="row.product_image">
                    <el-image :src="row.product_image" fit="cover" />
                  </div>
                  <div class="meta">
                    <div class="title" @click="goToProduct(row.product_id)">
                      {{ row.product_name || ('商品 #' + (row.product_id ?? '')) }}
                    </div>
                    <div class="muted">购买意向ID: {{ row.purchase_id }}</div>
                  </div>
                </div>

                <div class="center">
                  <div>数量: <strong>{{ row.quantity ?? 0 }}</strong></div>
                  <div>总价: <strong>¥{{ row.total_amount ?? 0 }}</strong></div>
                  <div class="muted">下单时间: {{ formatTime(row.created_at) }}</div>
                </div>

                <div class="right">
                  <el-tag :type="getOrderStatusType(row.purchase_status)" size="medium">
                    {{ getOrderStatusText(row.purchase_status) }}
                  </el-tag>
                  <div class="actions">
                    <el-button size="small" type="primary" @click="goToProduct(row.product_id)">查看商品</el-button>
                    <el-button size="small" @click="viewDetail(row)">详情</el-button>
                    
                    <!-- 客户取消订单按钮 -->
                    <el-button 
                      v-if="canCustomerCancel(row.purchase_status)" 
                      size="small" 
                      type="danger" 
                      @click="showCancelDialog(row)">
                      取消订单
                    </el-button>
                    
                    <!-- 客户确认收货按钮 -->
                    <el-button 
                      v-if="row.purchase_status === 'SHIPPING_STARTED'" 
                      size="small" 
                      type="success" 
                      @click="confirmReceived(row)">
                      确认收货
                    </el-button>
                  </div>
                </div>
              </div>
            </el-card>
          </div>

          <div class="pager-row">
            <el-pagination
                background
                :current-page="page"
                :page-size="size"
                :total="total"
                @current-change="onPageChange"
                @size-change="onSizeChange"
                :page-sizes="[10,20,50]"
                layout="sizes, prev, pager, next, jumper"
            />
          </div>
        </div>

        <el-loading :loading="loading" />
      </div>
    </el-card>
    
    <!-- 取消订单对话框 -->
    <el-dialog v-model="cancelDialogVisible" title="取消订单" width="500px">
      <el-form :model="cancelForm" label-width="100px">
        <el-form-item label="取消原因">
          <el-select v-model="cancelForm.cancelReason" placeholder="请选择取消原因">
            <el-option label="不想买了" value="不想买了"></el-option>
            <el-option label="买错了" value="买错了"></el-option>
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessageBox, ElMessage, ElLoading } from 'element-plus'
import { purchaseAPI } from '@/api'
import { useRouter } from 'vue-router'

const router = useRouter()
const page = ref(1)
const size = ref(10)
const total = ref(0)
const items = ref([])
const loading = ref(false)

// 取消订单相关
const cancelDialogVisible = ref(false)
const cancelForm = ref({
  purchaseId: null,
  cancelReason: '',
  cancelNotes: ''
})

const extractData = (res) => res?.data?.data ?? res?.data ?? null

const getCustomerIdFromStorage = () => {
  try {
    const raw = localStorage.getItem('customer_info') || localStorage.getItem('customer') || null
    if (raw) {
      try {
        const parsed = JSON.parse(raw)
        if (parsed && parsed.customer_id) return Number(parsed.customer_id)
        if (parsed && (parsed.id || parsed.user_id)) return Number(parsed.id || parsed.user_id)
      } catch (e) {
        // raw 可能是字符串 id
        const n = Number(raw)
        if (!Number.isNaN(n)) return n
      }
    }
  } catch (e) { /* ignore */ }

  const idStr = localStorage.getItem('customer_id')
  if (idStr) return Number(idStr)
  return null
}

const fetchItems = async (p = page.value, s = size.value) => {
  const customerId = getCustomerIdFromStorage()
  if (!customerId) {
    ElMessage.error('未检测到用户信息，无法获取购买意向')
    items.value = []
    total.value = 0
    return
  }

  loading.value = true
  try {
    const res = await purchaseAPI.getCustomerPurchaseIntents(customerId, { page: p, size: s })
    const payload = extractData(res)
    let list = []
    if (!payload) list = []
    else if (Array.isArray(payload.items)) list = payload.items
    else if (Array.isArray(payload)) list = payload
    else if (payload && (payload.purchase_id || payload.purchase_id === 0)) list = [payload]
    else list = []

    // 简单规范化常用字段（兼容不同后端返回）
    items.value = list.map(it => ({
      purchase_id: it.purchase_id ?? it.id,
      product_id: it.product_id ?? it.productId ?? it.product?.product_id,
      product_name: it.product_name ?? it.product?.product_name ?? it.product?.name,
      product_image: it.product_image ?? it.product?.image_url ?? it.product?.image,
      quantity: it.quantity ?? it.qty ?? 1,
      total_amount: it.total_amount ?? it.total ?? it.amount,
      purchase_status: it.purchase_status ?? it.status,
      created_at: it.created_at ?? it.createdAt ?? it.created
    }))

    total.value = Number(payload?.total ?? items.value.length ?? 0)
    page.value = p
    size.value = s
  } catch (err) {
    console.error('获取购买意向失败', err)
    items.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const onPageChange = (p) => { page.value = p; fetchItems(p, size.value) }
const onSizeChange = (s) => { size.value = s; page.value = 1; fetchItems(1, s) }

const formatTime = (t) => {
  if (!t) return ''
  try { return new Date(t).toLocaleString() } catch (e) { return t }
}

const getOrderStatusType = (status) => {
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

const getOrderStatusText = (status) => {
  const texts = { 
    'CUSTOMER_ORDERED': '客户下单', 
    'SELLER_CONFIRMED': '商家确认',
    'STOCK_PREPARED': '备货完成',
    'SHIPPING_STARTED': '开始发货',
    'COMPLETED': '交易完成', 
    'CUSTOMER_CANCELLED': '客户取消',
    'SELLER_CANCELLED': '商家取消'
  }
  return texts[status] || (status || '')
}

// 判断客户是否可以取消订单
const canCustomerCancel = (status) => {
  return ['CUSTOMER_ORDERED', 'SELLER_CONFIRMED', 'STOCK_PREPARED'].includes(status)
}

const goToProduct = (id) => {
  if (!id) return ElMessage.warning('无可跳转的商品信息')
  router.push({ path: `/product/${id}` })
}

const viewDetail = (row) => {
  ElMessageBox.alert(
      `
购买意向ID：${row.purchase_id}
商品：${row.product_name || row.product_id}
数量：${row.quantity}
总价：¥${row.total_amount ?? 0}
状态：${getOrderStatusText(row.purchase_status)}
创建时间：${formatTime(row.created_at)}
    `,
      '购买意向详情',
      { confirmButtonText: '关闭' }
  )
}

// 显示取消订单对话框
const showCancelDialog = (row) => {
  cancelForm.value.purchaseId = row.purchase_id
  cancelForm.value.cancelReason = ''
  cancelForm.value.cancelNotes = ''
  cancelDialogVisible.value = true
}

// 确认取消订单

const confirmCancelOrder = async () => {
  if (!cancelForm.value.cancelReason) {
    ElMessage.warning('请选择取消原因')
    return
  }
  
  const customerId = getCustomerIdFromStorage()
  if (!customerId) {
    ElMessage.error('未检测到用户信息')
    return
  }
  
  try {
    const res = await purchaseAPI.customerCancelOrder(
      customerId,
      cancelForm.value.purchaseId,
      {
        cancel_reason: cancelForm.value.cancelReason,
        cancel_notes: cancelForm.value.cancelNotes
      }
    )
    
    // 修复响应数据访问方式
    if (res && res.data && res.data.code === 200) {
      ElMessage.success('订单取消成功')
      cancelDialogVisible.value = false
      await fetchItems() // 重新加载订单列表
    } else {
      ElMessage.error(res?.data?.message || '取消订单失败')
      // 即使提示失败，也刷新数据以确保界面与数据库一致
      await fetchItems()
    }
  } catch (err) {
    console.error('取消订单失败', err)
    ElMessage.error('取消订单失败: ' + (err.message || err))
    // 即使提示失败，也刷新数据以确保界面与数据库一致
    await fetchItems()
  }
}

// 确认收货
const confirmReceived = async (row) => {
  try {
    await ElMessageBox.confirm('确认已收到货物并完成订单?', '确认收货', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const customerId = getCustomerIdFromStorage()
    if (!customerId) {
      ElMessage.error('未检测到用户信息')
      return
    }
    
    const res = await purchaseAPI.customerConfirmReceived(customerId, row.purchase_id)
    
    // 修复响应数据访问方式
    if (res && res.data && res.data.code === 200) {
      ElMessage.success('订单已完成')
      await fetchItems() // 重新加载订单列表
    } else {
      ElMessage.error(res?.data?.message || '确认收货失败')
      // 即使提示失败，也刷新数据以确保界面与数据库一致
      await fetchItems()
    }
  } catch (err) {
    if (err !== 'cancel') {
      console.error('确认收货失败', err)
      ElMessage.error('确认收货失败: ' + (err.message || err))
      // 即使提示失败，也刷新数据以确保界面与数据库一致
      await fetchItems()
    }
  }
}

onMounted(() => {
  fetchItems()
})
</script>

<style scoped>
.customer-dashboard { padding: 16px; }
.header-row { display:flex; justify-content:space-between; align-items:center; gap:12px; }
.intent-list { display:flex; flex-direction:column; gap:12px; margin-top:8px; }
.intent-card { padding:12px; }
.intent-grid { display:flex; gap:16px; align-items:center; }
.left { display:flex; gap:12px; align-items:center; min-width: 280px; }
.thumb { width:80px; height:80px; border-radius:8px; overflow:hidden; background:#fafafa; }
.thumb img, .thumb .el-image__inner { width:100%; height:100%; object-fit:cover; display:block; }
.meta .title { font-weight:700; color:#409eff; cursor:pointer; }
.meta .muted { color:#888; font-size:12px; margin-top:6px; }
.center { flex:1; color:#333; }
.right { display:flex; flex-direction:column; align-items:flex-end; gap:8px; min-width:160px; }
.actions { display:flex; gap:8px; flex-wrap: wrap; }
.muted { color:#999; font-size:12px; }
.pager-row { margin-top:14px; display:flex; justify-content:flex-end; }
.empty-wrap { padding:24px 0; display:flex; justify-content:center; }
@media (max-width:800px) {
  .intent-grid { flex-direction:column; align-items:stretch; }
  .right { align-items:flex-start; }
  .left { min-width: auto; }
}
</style>