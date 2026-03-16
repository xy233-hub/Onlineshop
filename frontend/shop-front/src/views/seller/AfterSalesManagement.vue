<template>
  <div class="after-sales-management-container">
    <el-card class="after-sales-management-card">
      <template #header>
        <div class="after-sales-management-header">
          <h2>售后管理</h2>
          <div class="filter-container">
            <el-select v-model="filter.serviceStatus" placeholder="售后状态">
              <el-option label="全部" value="" />
              <el-option label="待处理" value="PENDING" />
              <el-option label="处理中" value="PROCESSING" />
              <el-option label="已完成" value="COMPLETED" />
              <el-option label="已拒绝" value="REJECTED" />
              <el-option label="已取消" value="CANCELLED" />
            </el-select>
            <el-select v-model="filter.serviceType" placeholder="售后类型">
              <el-option label="全部" value="" />
              <el-option label="仅退款" value="REFUND_ONLY" />
              <el-option label="退货退款" value="RETURN_AND_REFUND" />
              <el-option label="换货" value="EXCHANGE" />
            </el-select>
            <el-button type="primary" @click="loadAfterSalesList">查询</el-button>
          </div>
        </div>
      </template>

      <el-table :data="afterSalesList" style="width: 100%">
        <el-table-column prop="service_id" label="售后ID" width="100" />
        <el-table-column prop="purchase_id" label="订单ID" width="100" />
        <el-table-column label="客户信息" width="180">
          <template #default="scope">
            <div v-if="scope.row.customer_info">
              <div>{{ scope.row.customer_info.username }}</div>
              <div>{{ scope.row.customer_info.phone }}</div>
            </div>
            <div v-else>无</div>
          </template>
        </el-table-column>
        <el-table-column label="商品信息" width="200">
          <template #default="scope">
            <div v-if="scope.row.product_info">
              <div>{{ scope.row.product_info.product_name }}</div>
              <div>¥{{ scope.row.product_info.price }}</div>
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
        <el-table-column prop="refund_amount" label="退款金额" width="120" />
        <el-table-column prop="service_status" label="状态" width="120">
          <template #default="scope">
            <el-tag :type="getStatusTagType(scope.row.service_status)">
              {{ getStatusText(scope.row.service_status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="created_at" label="创建时间" width="180" />
        <el-table-column label="操作" width="150">
          <template #default="scope">
            <el-button size="small" type="primary" @click="viewDetail(scope.row.service_id)">
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
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
    <el-dialog
        v-model="detailDialogVisible"
        title="售后详情"
        width="800px"
    >
      <div v-if="afterSalesDetail" class="after-sales-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="售后ID">{{ afterSalesDetail.service_id }}</el-descriptions-item>
          <el-descriptions-item label="订单ID">{{ afterSalesDetail.purchase_id }}</el-descriptions-item>
          <el-descriptions-item label="客户信息">
            <div v-if="afterSalesDetail.customer_info">
              <div>{{ afterSalesDetail.customer_info.username }}</div>
              <div>{{ afterSalesDetail.customer_info.phone }}</div>
            </div>
            <div v-else>无</div>
          </el-descriptions-item>
          <el-descriptions-item label="订单信息">
            <div v-if="afterSalesDetail.order_info">
              <div>{{ afterSalesDetail.order_info.product_name }}</div>
              <div>¥{{ afterSalesDetail.order_info.order_amount }}</div>
              <div>数量: {{ afterSalesDetail.order_info.quantity }}</div>
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
          <el-descriptions-item label="退款金额">{{ afterSalesDetail.refund_amount }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusTagType(afterSalesDetail.service_status)">
              {{ getStatusText(afterSalesDetail.service_status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="卖家回复" :span="2">{{ afterSalesDetail.seller_response || '无' }}</el-descriptions-item>
          <el-descriptions-item label="卖家决定" :span="2">{{ afterSalesDetail.seller_decision || '无' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ afterSalesDetail.created_at }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ afterSalesDetail.updated_at }}</el-descriptions-item>
        </el-descriptions>

        <!-- 处理售后 -->
        <div v-if="afterSalesDetail.service_status === 'PENDING'" class="handle-section">
          <h3>处理售后</h3>
          <el-form :model="handleForm" label-width="100px">
            <el-form-item label="处理决定">
              <el-select v-model="handleForm.seller_decision" placeholder="请选择处理决定">
                <el-option label="同意退款" value="AGREE_REFUND" />
                <el-option label="同意退货退款" value="AGREE_RETURN_REFUND" />
                <el-option label="同意换货" value="AGREE_EXCHANGE" />
                <el-option label="拒绝售后" value="REJECT" />
              </el-select>
            </el-form-item>
            <el-form-item label="回复内容">
              <el-input
                  v-model="handleForm.seller_response"
                  type="textarea"
                  placeholder="请输入回复内容"
                  :rows="3"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleAfterSales">提交处理</el-button>
            </el-form-item>
          </el-form>
        </div>

        <!-- 确认收货 -->
        <div v-if="afterSalesDetail.service_status === 'AGREED' && afterSalesDetail.service_type === 'RETURN_AND_REFUND'" class="confirm-section">
          <h3>确认收货</h3>
          <el-form :model="confirmForm" label-width="100px">
            <el-form-item label="备注">
              <el-input
                  v-model="confirmForm.remark"
                  type="textarea"
                  placeholder="请输入备注"
                  :rows="2"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="confirmReturn">确认收货并退款</el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { afterSalesAPI } from '@/api'

// 售后列表数据
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

// 加载状态
const loading = ref(false)

// 详情对话框
const detailDialogVisible = ref(false)
const afterSalesDetail = ref(null)

// 处理表单
const handleForm = reactive({
  seller_decision: '',
  seller_response: ''
})

// 确认表单
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
    afterSalesList.value = response.data.data.items
    pagination.total = response.data.data.total
  } catch (error) {
    console.error('获取售后列表失败:', error)
    ElMessage.error('获取售后列表失败: ' + (error.response?.data?.message || error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

// 查看详情
const viewDetail = async (serviceId) => {
  try {
    const response = await afterSalesAPI.getSellerAfterSalesDetail(serviceId)
    afterSalesDetail.value = response.data.data
    detailDialogVisible.value = true
  } catch (error) {
    ElMessage.error('获取售后详情失败')
  }
}

// 处理售后
const handleAfterSales = async () => {
  if (!handleForm.seller_decision) {
    ElMessage.warning('请选择处理决定')
    return
  }
  
  try {
    const response = await afterSalesAPI.handleAfterSales(afterSalesDetail.value.service_id, {
      seller_decision: handleForm.seller_decision,
      seller_response: handleForm.seller_response
    })
    ElMessage.success('处理成功')
    detailDialogVisible.value = false
    loadAfterSalesList()
  } catch (error) {
    ElMessage.error('处理失败')
  }
}

// 确认收货
const confirmReturn = async () => {
  try {
    const response = await afterSalesAPI.confirmReturn(afterSalesDetail.value.service_id, {
      remark: confirmForm.remark
    })
    ElMessage.success('确认收货成功，退款已执行')
    detailDialogVisible.value = false
    loadAfterSalesList()
  } catch (error) {
    ElMessage.error('确认失败')
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

// 初始化
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

.after-sales-management-card {
  max-width: 1200px;
  margin: 0 auto;
}

.after-sales-management-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.filter-container {
  display: flex;
  gap: 10px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.after-sales-detail {
  padding: 20px 0;
}

.evidence-images {
  display: flex;
  flex-wrap: wrap;
}

.handle-section,
.confirm-section {
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #eaeaea;
}

.handle-section h3,
.confirm-section h3 {
  margin-bottom: 20px;
  font-size: 16px;
  font-weight: bold;
}
</style>
