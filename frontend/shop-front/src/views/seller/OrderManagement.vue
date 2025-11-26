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
      <el-table-column prop="cancel_info" label="取消原因" width="150">
        <template #default="{ row }">
          <div v-if="row.purchase_status.includes('CANCELLED') || row.purchase_status === 'CUSTOMER_CANCELLED' || row.purchase_status === 'SELLER_CANCELLED'">
            <el-text size="small">{{ row.cancel_reason || '-' }}</el-text>
            <div v-if="row.cancel_notes || row.cancel_notes === 0">
              <el-text type="info" size="small">备注：{{ row.cancel_notes || '-' }}</el-text>
            </div>
          </div>
          <el-text type="info" size="small" v-else>-</el-text>
        </template>
      </el-table-column>
      <el-table-column prop="created_at" label="提交时间" width="180">
        <template #default="{ row }">{{ formatTime(row.created_at) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="240">
        <template #default="{ row }">
          <!-- 商家确认订单 -->
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
              @click="updateOrderStatus(row, 'SHIPPING_STARTED')"
          >
            开始发货
          </el-button>
          
          <!-- 商家取消订单 -->
          <el-button
              v-if="['CUSTOMER_ORDERED', 'SELLER_CONFIRMED', 'STOCK_PREPARED', 'SHIPPING_STARTED'].includes(row.purchase_status)"
              size="small"
              type="danger"
              @click="showSellerCancelDialog(row)"
          >
            取消订单
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
    
    <!-- 商家取消订单对话框 -->
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
          <el-button type="primary" @click="confirmSellerCancelOrder">确认取消</el-button>
        </span>
      </template>
    </el-dialog>
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

// 取消订单相关
const cancelDialogVisible = ref(false)
const cancelForm = ref({
  purchaseId: null,
  cancelReason: '',
  cancelNotes: ''
})

const extractData = (res) => res?.data?.data ?? null

const fetchOrders = async () => {
  loading.value = true
  try {
    const res = await purchaseAPI.getSellerPurchaseIntents({ page: page.value, size: size.value })
    const d = extractData(res)
    if (!d) {
      ElMessage.error(res?.message || '获取购买意向失败')
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

const updateOrderStatus = async (order, newStatus) => {
  try {
    let sellerNotes = null;
    
    // 如果是特定状态转换，可能需要添加备注
    if (newStatus === 'SELLER_CONFIRMED') {
      sellerNotes = '商家已确认订单';
    } else if (newStatus === 'STOCK_PREPARED') {
      sellerNotes = '备货已完成';
    } else if (newStatus === 'SHIPPING_STARTED') {
      sellerNotes = '已开始发货';
    }
    
    const payload = { new_status: newStatus };
    if (sellerNotes) payload.seller_notes = sellerNotes;
    
    const res = await purchaseAPI.updatePurchaseIntentStatus(order.purchase_id, payload);
    // 修复响应数据访问方式
    if (res.data && res.data.code === 200) {
      ElMessage.success('状态更新成功');
      await fetchOrders();
    } else {
      ElMessage.error(res.data?.message || '更新失败');
      // 即使提示失败，也刷新数据以确保界面与数据库一致
      await fetchOrders();
    }
  } catch (err) {
    console.error(err);
    ElMessage.error('更新失败');
    // 即使提示失败，也刷新数据以确保界面与数据库一致
    await fetchOrders();
  }
};
const handleUpdateStatus = async (order, newStatus, sellerNotes = null) => {
  try {
    const payload = { new_status: newStatus }
    if (sellerNotes) payload.seller_notes = sellerNotes
    const res = await purchaseAPI.updatePurchaseIntentStatus(order.purchase_id, payload)
    if (res.code === 200) {
      ElMessage.success('状态更新成功')
      await fetchOrders()
    } else {
      ElMessage.error(res.message || '更新失败')
    }
  } catch (err) {
    console.error(err)
    ElMessage.error('更新失败')
  }
}

// 显示商家取消订单对话框
const showSellerCancelDialog = (row) => {
  cancelForm.value.purchaseId = row.purchase_id
  cancelForm.value.cancelReason = ''
  cancelForm.value.cancelNotes = ''
  cancelDialogVisible.value = true
}

// 确认商家取消订单
const confirmSellerCancelOrder = async () => {
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
    
    // 修复响应数据访问方式
    if (res.data && res.data.code === 200) {
      ElMessage.success('订单取消成功')
      cancelDialogVisible.value = false
      await fetchOrders() // 重新加载订单列表
    } else {
      ElMessage.error(res.data?.message || '取消订单失败')
      // 即使提示失败，也刷新数据以确保界面与数据库一致
      await fetchOrders()
    }
  } catch (err) {
    console.error('取消订单失败', err)
    ElMessage.error('取消订单失败')
    // 即使提示失败，也刷新数据以确保界面与数据库一致
    await fetchOrders()
  }
}
const handlePageChange = (p) => {
  page.value = p
  fetchOrders()
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

onMounted(() => {
  fetchOrders()
})
</script>