<!-- vue -->
<template>
  <div class="order-management">
    <el-table :data="orders" v-loading="loading" stripe>
      <el-table-column prop="purchase_id" label="意向ID" width="100" />
      <el-table-column prop="product_id" label="商品ID" width="120">
        <template #default="{ row }">
          <el-link type="primary" @click="$router.push({ path: `/product/${row.product_id}` })">
            {{ row.product_id }}
          </el-link>
        </template>
      </el-table-column>
      <el-table-column prop="customer_name" label="顾客姓名" width="120" />
      <el-table-column prop="customer_phone" label="联系电话" width="140" />
      <el-table-column prop="customer_address" label="收货地址" />
      <el-table-column prop="quantity" label="数量" width="80" />
      <el-table-column prop="total_amount" label="总额" width="120">
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
      <el-table-column label="操作" width="240">
        <template #default="{ row }">
          <el-button
              size="small"
              type="success"
              @click="confirmUpdateStatus(row, 'success')"
              :disabled="row.purchase_status !== 'pending'"
          >
            交易成功
          </el-button>
          <el-button
              size="small"
              type="danger"
              @click="promptFailAndUpdate(row)"
              :disabled="row.purchase_status !== 'pending'"
          >
            交易失败
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 简单分页（如需） -->
    <div class="pagination" v-if="total > size" style="margin-top:16px; text-align:right;">
      <el-pagination
          background
          :current-page="page"
          :page-size="size"
          :total="total"
          @current-change="handlePageChange"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { purchaseAPI } from '@/api'
import { formatTime } from '@/utils'

const orders = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)

const extractData = (res) => res?.data?.data ?? null

const fetchOrders = async () => {
  loading.value = true
  try {
    const res = await purchaseAPI.getSellerPurchaseIntents({ page: page.value, size: size.value })
    const d = extractData(res)
    if (!d) {
      ElMessage.error(res?.data?.message || '获取购买意向失败')
      orders.value = []
      total.value = 0
      return
    }
    // 支持 data 为分页对象 { page,size,total,items }
    orders.value = Array.isArray(d.items) ? d.items : (Array.isArray(d) ? d : [])
    total.value = Number(d.total ?? orders.value.length)
  } catch (err) {
    console.error(err)
    ElMessage.error('获取购买意向失败')
    orders.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const handleUpdateStatus = async (order, newStatus, sellerNotes = null) => {
  try {
    const payload = { new_status: newStatus }
    if (sellerNotes) payload.seller_notes = sellerNotes
    const res = await purchaseAPI.updatePurchaseIntentStatus(order.purchase_id, payload)
    const d = res?.data
    if (d?.code === 200) {
      ElMessage.success('状态更新成功')
      await fetchOrders()
    } else {
      ElMessage.error(d?.message || '更新失败')
    }
  } catch (err) {
    console.error(err)
    ElMessage.error('更新失败')
  }
}

const confirmUpdateStatus = async (order, status) => {
  try {
    await ElMessageBox.confirm('确认将此意向标记为成功？', '确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await handleUpdateStatus(order, status)
  } catch (e) {
    // 取消或错误
  }
}

const promptFailAndUpdate = async (order) => {
  try {
    const { value } = await ElMessageBox.prompt('填写失败备注（可选）', '交易失败', {
      confirmButtonText: '提交',
      cancelButtonText: '取消',
      inputPlaceholder: '输入备注',
      inputPattern: /[\s\S]*/,
      inputErrorMessage: ''
    })
    await handleUpdateStatus(order, 'failed', value || null)
  } catch (e) {
    // 取消或错误
  }
}

const handlePageChange = (p) => {
  page.value = p
  fetchOrders()
}

const getStatusType = (status) => {
  const types = { pending: 'warning', success: 'success', failed: 'danger' }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = { pending: '待处理', success: '成功', failed: '失败' }
  return texts[status] || status
}

onMounted(() => {
  fetchOrders()
})
</script>
