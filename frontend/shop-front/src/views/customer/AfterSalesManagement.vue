<template>
  <div class="after-sales-management-container">
    <el-card class="after-sales-card">
      <template #header>
        <div class="after-sales-header">
          <h2>售后管理</h2>
          <el-button type="primary" @click="loadAfterSalesList" :loading="loading">
            刷新列表
          </el-button>
        </div>
      </template>

      <!-- 筛选条件 -->
      <div class="filter-container" style="margin-bottom: 20px; display: flex; gap: 10px; align-items: center;">
        <el-select v-model="filter.serviceStatus" placeholder="按状态筛选" clearable @change="loadAfterSalesList">
          <el-option label="全部" value="" />
          <el-option label="待处理" value="PENDING" />
          <el-option label="协商中" value="NEGOTIATING" />
          <el-option label="已同意" value="AGREED" />
          <el-option label="已拒绝" value="REJECTED" />
          <el-option label="已完成" value="COMPLETED" />
          <el-option label="已取消" value="CANCELLED" />
        </el-select>
        
        <el-select v-model="filter.serviceType" placeholder="按类型筛选" clearable @change="loadAfterSalesList">
          <el-option label="全部" value="" />
          <el-option label="仅退款" value="REFUND_ONLY" />
          <el-option label="退货退款" value="RETURN_AND_REFUND" />
          <el-option label="换货" value="EXCHANGE" />
        </el-select>
      </div>

      <!-- 售后列表 -->
      <el-table :data="afterSalesList" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="service_id" label="售后单号" width="180" />
        <el-table-column prop="purchase_id" label="订单 ID" width="120" />
        <el-table-column label="商品信息" width="200">
          <template #default="scope">
            <div v-if="scope.row.product_info">
              <div style="font-weight: 600">{{ scope.row.product_info.product_name }}</div>
              <div style="color: #999; font-size: 12px">¥{{ scope.row.product_info.price }}</div>
            </div>
            <div v-else>无</div>
          </template>
        </el-table-column>
        <el-table-column label="客户信息" width="150">
          <template #default="scope">
            <div v-if="scope.row.customer_info">
              <div>{{ scope.row.customer_info.username }}</div>
              <div style="color: #999; font-size: 12px">{{ scope.row.customer_info.phone }}</div>
            </div>
            <div v-else>无</div>
          </template>
        </el-table-column>
        <el-table-column prop="service_type" label="售后类型" width="120">
          <template #default="scope">
            <el-tag :type="getServiceTypeTagType(scope.row.service_type)">
              {{ getServiceTypeText(scope.row.service_type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="service_title" label="申请标题" />
        <el-table-column prop="refund_amount" label="退款金额" width="120">
          <template #default="scope">
            <span style="color: #f56c6c; font-weight: bold">¥{{ scope.row.refund_amount }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="service_status" label="状态" width="120">
          <template #default="scope">
            <el-tag :type="getStatusTagType(scope.row.service_status)">
              {{ getStatusText(scope.row.service_status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="created_at" label="提交时间" width="180" />
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="scope">
            <el-button type="primary" size="small" @click="viewDetail(scope.row)">
              查看详情
            </el-button>
            <el-button 
              v-if="scope.row.service_status === 'PENDING' || scope.row.service_status === 'NEGOTIATING'" 
              type="success" 
              size="small" 
              @click="openHandleDialog(scope.row)"
            >
              处理申请
            </el-button>
            <el-button 
              v-if="scope.row.service_status === 'AGREED' && scope.row.service_type === 'RETURN_AND_REFUND'" 
              type="warning" 
              size="small" 
              @click="openConfirmReturnDialog(scope.row)"
            >
              确认退货
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container" style="margin-top: 20px; display: flex; justify-content: flex-end;">
        <el-pagination
            v-model:current-page="pagination.page"
            v-model:page-size="pagination.size"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="pagination.total"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 售后详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="售后详情" width="800px">
      <div v-if="afterSalesDetail" class="after-sales-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="售后单号">{{ afterSalesDetail.service_id }}</el-descriptions-item>
          <el-descriptions-item label="订单 ID">{{ afterSalesDetail.purchase_id }}</el-descriptions-item>
          
          <el-descriptions-item label="客户信息">
            <div v-if="afterSalesDetail.customer_info">
              <div>用户名：{{ afterSalesDetail.customer_info.username }}</div>
              <div>电话：{{ afterSalesDetail.customer_info.phone }}</div>
            </div>
            <div v-else>无</div>
          </el-descriptions-item>
          
          <el-descriptions-item label="订单信息">
            <div v-if="afterSalesDetail.order_info">
              <div>商品：{{ afterSalesDetail.order_info.product_name }}</div>
              <div>金额：¥{{ afterSalesDetail.order_info.order_amount }}</div>
              <div>数量：{{ afterSalesDetail.order_info.quantity }}</div>
              <div>状态：{{ afterSalesDetail.order_info.order_status }}</div>
            </div>
            <div v-else>无</div>
          </el-descriptions-item>
          
          <el-descriptions-item label="售后类型" :span="2">
            <el-tag :type="getServiceTypeTagType(afterSalesDetail.service_type)">
              {{ getServiceTypeText(afterSalesDetail.service_type) }}
            </el-tag>
          </el-descriptions-item>
          
          <el-descriptions-item label="申请标题" :span="2">{{ afterSalesDetail.service_title }}</el-descriptions-item>
          <el-descriptions-item label="问题描述" :span="2">{{ afterSalesDetail.problem_description }}</el-descriptions-item>
          
          <el-descriptions-item label="凭证图片" :span="2">
            <div class="evidence-images" v-if="afterSalesDetail.evidence_images && afterSalesDetail.evidence_images.length > 0">
              <el-image
                  v-for="(image, index) in afterSalesDetail.evidence_images"
                  :key="index"
                  :src="image"
                  :preview-src-list="afterSalesDetail.evidence_images"
                  style="width: 100px; height: 100px; margin-right: 10px;"
              />
            </div>
            <div v-else>无</div>
          </el-descriptions-item>
          
          <el-descriptions-item label="退款金额">
            <span style="color: #f56c6c; font-weight: bold">¥{{ afterSalesDetail.refund_amount }}</span>
          </el-descriptions-item>
          
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusTagType(afterSalesDetail.service_status)">
              {{ getStatusText(afterSalesDetail.service_status) }}
            </el-tag>
          </el-descriptions-item>
          
          <el-descriptions-item label="卖家回复" :span="2">{{ afterSalesDetail.seller_response || '暂无' }}</el-descriptions-item>
          <el-descriptions-item label="卖家决定" :span="2">{{ afterSalesDetail.seller_decision || '暂无' }}</el-descriptions-item>
          
          <el-descriptions-item label="提交时间">{{ afterSalesDetail.created_at }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ afterSalesDetail.updated_at }}</el-descriptions-item>
        </el-descriptions>
      </div>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="detailDialogVisible = false">关闭</el-button>
          <el-button 
            v-if="afterSalesDetail?.service_status === 'PENDING' || afterSalesDetail?.service_status === 'NEGOTIATING'" 
            type="success" 
            @click="openHandleDialog(afterSalesDetail)"
          >
            处理申请
          </el-button>
          <el-button 
            v-if="afterSalesDetail?.service_status === 'AGREED' && afterSalesDetail?.service_type === 'RETURN_AND_REFUND'" 
            type="warning" 
            @click="openConfirmReturnDialog(afterSalesDetail)"
          >
            确认退货
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 处理售后对话框 -->
    <el-dialog v-model="handleDialogVisible" title="处理售后申请" width="600px">
      <el-form :model="handleForm" label-width="100px">
        <el-form-item label="处理决定">
          <el-select v-model="handleForm.seller_decision" placeholder="请选择处理决定" style="width: 100%;">
            <el-option label="同意仅退款" value="AGREE_REFUND" />
            <el-option label="同意退货退款" value="AGREE_RETURN_REFUND" />
            <el-option label="同意换货" value="AGREE_EXCHANGE" />
            <el-option label="拒绝" value="REJECT" />
          </el-select>
        </el-form-item>
        <el-form-item label="处理说明">
          <el-input
            v-model="handleForm.seller_response"
            type="textarea"
            :rows="4"
            placeholder="请输入处理说明"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="handleDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitHandle">提交</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 确认退货对话框 -->
    <el-dialog v-model="confirmReturnDialogVisible" title="确认收到退货" width="600px">
      <el-form :model="confirmForm" label-width="100px">
        <el-form-item label="备注说明">
          <el-input
            v-model="confirmForm.remark"
            type="textarea"
            :rows="4"
            placeholder="请输入备注说明（可选）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="confirmReturnDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitConfirmReturn">确认收货并退款</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { afterSalesAPI } from '@/api'

// 加载状态
const loading = ref(false)

// 售后列表
const afterSalesList = ref([])

// 筛选条件
const filter = reactive({
  serviceStatus: '',
  serviceType: ''
})

// 分页信息
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

// 售后详情
const afterSalesDetail = ref(null)
const detailDialogVisible = ref(false)

// 处理售后对话框
const handleDialogVisible = ref(false)
const handleForm = reactive({
  seller_decision: '',
  seller_response: ''
})

// 确认退货对话框
const confirmReturnDialogVisible = ref(false)
const confirmForm = reactive({
  remark: ''
})

// 加载售后列表
const loadAfterSalesList = async () => {
  loading.value = true
  try {
    const response = await afterSalesAPI.getSellerAfterSales({
      page: pagination.page,
      size: pagination.size,
      service_status: filter.serviceStatus,
      service_type: filter.serviceType
    })
    
    if (response && response.data && response.data.data) {
      afterSalesList.value = response.data.data.items || []
      pagination.total = response.data.data.total || 0
    } else {
      afterSalesList.value = []
      pagination.total = 0
    }
  } catch (error) {
    console.error('获取售后列表失败:', error)
    ElMessage.error('获取售后列表失败：' + (error.response?.data?.message || '网络错误'))
  } finally {
    loading.value = false
  }
}

// 查看售后详情
const viewDetail = async (row) => {
  try {
    const response = await afterSalesAPI.getSellerAfterSalesDetail(row.service_id)
    afterSalesDetail.value = response.data.data
    detailDialogVisible.value = true
  } catch (error) {
    ElMessage.error('获取售后详情失败：' + (error.response?.data?.message || '网络错误'))
  }
}

// 打开处理对话框
const openHandleDialog = (row) => {
  handleForm.seller_decision = ''
  handleForm.seller_response = ''
  afterSalesDetail.value = row
  handleDialogVisible.value = true
}

// 提交处理
const submitHandle = async () => {
  if (!handleForm.seller_decision) {
    ElMessage.warning('请选择处理决定')
    return
  }
  
  try {
    const serviceId = afterSalesDetail.value.service_id
    const response = await afterSalesAPI.handleAfterSales(serviceId, {
      seller_decision: handleForm.seller_decision,
      seller_response: handleForm.seller_response
    })
    
    ElMessage.success('处理成功')
    handleDialogVisible.value = false
    loadAfterSalesList()
  } catch (error) {
    ElMessage.error('处理失败：' + (error.response?.data?.message || '网络错误'))
  }
}

// 打开确认退货对话框
const openConfirmReturnDialog = (row) => {
  confirmForm.remark = ''
  afterSalesDetail.value = row
  confirmReturnDialogVisible.value = true
}

// 提交确认退货
const submitConfirmReturn = async () => {
  try {
    const serviceId = afterSalesDetail.value.service_id
    const response = await afterSalesAPI.confirmReturn(serviceId, {
      remark: confirmForm.remark || ''
    })
    
    ElMessage.success('确认收货成功，退款已执行')
    confirmReturnDialogVisible.value = false
    loadAfterSalesList()
  } catch (error) {
    ElMessage.error('确认失败：' + (error.response?.data?.message || '网络错误'))
  }
}

// 分页处理
const handleSizeChange = (size) => {
  pagination.size = size
  loadAfterSalesList()
}

const handleCurrentChange = (page) => {
  pagination.page = page
  loadAfterSalesList()
}

// 获取售后类型文本
const getServiceTypeText = (type) => {
  const typeMap = {
    'REFUND_ONLY': '仅退款',
    'RETURN_AND_REFUND': '退货退款',
    'EXCHANGE': '换货'
  }
  return typeMap[type] || type
}

// 获取售后类型标签类型
const getServiceTypeTagType = (type) => {
  const typeMap = {
    'REFUND_ONLY': 'info',
    'RETURN_AND_REFUND': 'warning',
    'EXCHANGE': 'success'
  }
  return typeMap[type] || 'default'
}

// 获取状态文本
const getStatusText = (status) => {
  const statusMap = {
    'PENDING': '待处理',
    'NEGOTIATING': '协商中',
    'AGREED': '同意',
    'REJECTED': '拒绝',
    'COMPLETED': '已完成',
    'CANCELLED': '已取消'
  }
  return statusMap[status] || status
}

// 获取状态标签类型
const getStatusTagType = (status) => {
  const statusMap = {
    'PENDING': 'info',
    'NEGOTIATING': 'warning',
    'AGREED': 'success',
    'REJECTED': 'danger',
    'COMPLETED': 'success',
    'CANCELLED': 'default'
  }
  return statusMap[status] || 'default'
}

// 组件挂载时的操作
onMounted(() => {
  loadAfterSalesList()
})
</script>

<style scoped>
.after-sales-management-container {
  min-height: 100vh;
  padding: 20px;
  background: #f5f5f5;
}

.after-sales-card {
  max-width: 1400px;
  margin: 0 auto;
}

.after-sales-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.after-sales-header h2 {
  margin: 0;
}

.filter-container {
  background: #f9fafc;
  padding: 15px;
  border-radius: 4px;
}

.after-sales-detail {
  padding: 10px 0;
}

.evidence-images {
  display: flex;
  flex-wrap: wrap;
}

.pagination-container {
  margin-top: 20px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>