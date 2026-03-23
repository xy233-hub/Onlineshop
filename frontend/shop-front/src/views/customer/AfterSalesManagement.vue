<template>
  <div class="customer-after-sales-management">
    <el-card>
      <template #header>
        <h2>售后申请处理</h2>
      </template>

      <!-- 筛选栏 -->
      <div class="filter-bar">
        <el-select
          v-model="searchForm.service_status"
          placeholder="售后状态"
          clearable
          style="width: 150px"
          @change="handleSearch"
        >
          <el-option label="待处理" value="PENDING" />
          <el-option label="已同意" value="APPROVED" />
          <el-option label="已拒绝" value="REJECTED" />
          <el-option label="已完成" value="COMPLETED" />
          <el-option label="已取消" value="CANCELLED" />
        </el-select>
        <el-button type="primary" style="margin-left: 10px" @click="handleSearch">查询</el-button>
      </div>

      <!-- 售后列表 -->
      <el-table
        v-loading="loading"
        :data="afterSalesList"
        stripe
        style="width: 100%; margin-top: 20px"
      >
        <el-table-column prop="service_id" label="售后 ID" width="80" />
        <el-table-column prop="service_type" label="类型" width="100">
          <template #default="{ row }">
            {{ getServiceTypeText(row.service_type) }}
          </template>
        </el-table-column>
        <el-table-column prop="service_title" label="标题" min-width="200" />
        <el-table-column prop="customer_username" label="申请人" width="120" />
        <el-table-column prop="refund_amount" label="退款金额" width="100">
          <template #default="{ row }">¥{{ row.refund_amount }}</template>
        </el-table-column>
        <el-table-column prop="service_status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.service_status)">
              {{ getStatusText(row.service_status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="created_at" label="申请时间" width="160">
          <template #default="{ row }">{{ formatTime(row.created_at) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="viewDetail(row)">详情</el-button>
            <el-button
              v-if="row.service_status === 'PENDING'"
              size="small"
              type="primary"
              @click="handleDecision(row)"
            >处理</el-button>
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

    <!-- 售后详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="售后详情"
      width="700px"
    >
      <div v-if="currentService" class="detail-content">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="售后 ID">{{ currentService.service_id }}</el-descriptions-item>
          <el-descriptions-item label="订单 ID">{{ currentService.purchase_id }}</el-descriptions-item>
          <el-descriptions-item label="商品 ID">{{ currentService.product_id }}</el-descriptions-item>
          <el-descriptions-item label="商品名称">{{ currentService.product_info?.product_name || '-' }}</el-descriptions-item>
          <el-descriptions-item label="售后类型">{{ getServiceTypeText(currentService.service_type) }}</el-descriptions-item>
          <el-descriptions-item label="退款金额">¥{{ currentService.refund_amount }}</el-descriptions-item>
          <el-descriptions-item label="申请时间">{{ formatTime(currentService.created_at) }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(currentService.service_status)">
              {{ getStatusText(currentService.service_status) }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <el-divider />
        <h4>问题描述</h4>
        <p>{{ currentService.problem_description || '-' }}</p>

        <div v-if="currentService.evidence_images && currentService.evidence_images.length">
          <h4>凭证图片</h4>
          <div class="image-list">
            <el-image
              v-for="(img, idx) in currentService.evidence_images"
              :key="idx"
              :src="img"
              :preview-src-list="currentService.evidence_images"
              :initial-index="idx"
              fit="cover"
              style="width: 100px; height: 100px; margin-right: 10px"
            />
          </div>
        </div>

        <div v-if="currentService.seller_response">
          <el-divider />
          <h4>卖家回复</h4>
          <p>{{ currentService.seller_response }}</p>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button
          v-if="currentService?.service_status === 'PENDING'"
          type="primary"
          @click="handleDecision(currentService)"
        >处理</el-button>
      </template>
    </el-dialog>

    <!-- 处理对话框 -->
    <el-dialog
      v-model="decisionDialogVisible"
      title="处理售后申请"
      width="500px"
    >
      <el-form :model="decisionForm" label-width="100px">
        <el-form-item label="处理结果" required>
          <el-radio-group v-model="decisionForm.decision">
            <el-radio label="approved">同意</el-radio>
            <el-radio label="rejected">拒绝</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="回复内容" required>
          <el-input
            v-model="decisionForm.response_message"
            type="textarea"
            :rows="4"
            placeholder="请输入回复内容"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="decisionDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitDecision">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { customerAfterSalesHandleAPI } from '@/api'

const searchForm = reactive({
  service_status: ''
})

const page = ref(1)
const size = ref(20)
const total = ref(0)
const afterSalesList = ref([])
const loading = ref(false)

const detailDialogVisible = ref(false)
const decisionDialogVisible = ref(false)
const currentService = ref(null)

const decisionForm = reactive({
  service_id: null,
  decision: 'approved',
  response_message: ''
})

const extractData = (res) => res?.data?.data ?? res?.data ?? null

const fetchAfterSalesList = async (p = page.value, s = size.value) => {
  loading.value = true
  try {
    const params = { page: p, size: s }
    if (searchForm.service_status) params.service_status = searchForm.service_status
    
    const res = await customerAfterSalesHandleAPI.getAfterSalesList(params)
    const d = extractData(res)
    const items = Array.isArray(d.items) ? d.items : []
    
    afterSalesList.value = items
    page.value = Number(d?.page ?? p)
    size.value = Number(d?.size ?? s)
    total.value = Number(d?.total ?? items.length)
  } catch (err) {
    console.error(err)
    ElMessage.error('获取售后列表失败')
    afterSalesList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  page.value = 1
  fetchAfterSalesList()
}

const handlePageChange = (p) => {
  page.value = p
  fetchAfterSalesList()
}

const handleSizeChange = (s) => {
  size.value = s
  page.value = 1
  fetchAfterSalesList()
}

const viewDetail = async (service) => {
  try {
    const res = await customerAfterSalesHandleAPI.getAfterSalesDetail(service.service_id)
    const d = extractData(res)
    currentService.value = d
    detailDialogVisible.value = true
  } catch (err) {
    ElMessage.error('获取详情失败')
  }
}

const handleDecision = (service) => {
  currentService.value = service
  decisionForm.service_id = service.service_id
  decisionForm.decision = 'approved'
  decisionForm.response_message = ''
  decisionDialogVisible.value = true
}

const submitDecision = async () => {
  if (!decisionForm.response_message) {
    ElMessage.error('请填写回复内容')
    return
  }
  
  try {
    await customerAfterSalesHandleAPI.handleAfterSales(decisionForm.service_id, {
      decision: decisionForm.decision,
      response_message: decisionForm.response_message
    })
    ElMessage.success('处理成功')
    decisionDialogVisible.value = false
    fetchAfterSalesList()
  } catch (err) {
    ElMessage.error('处理失败')
  }
}

const getServiceTypeText = (type) => {
  const map = {
    REFUND_ONLY: '仅退款',
    RETURN_AND_REFUND: '退货退款',
    EXCHANGE: '换货'
  }
  return map[type] || type
}

const getStatusType = (status) => {
  const map = {
    PENDING: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger',
    COMPLETED: 'info',
    CANCELLED: 'info'
  }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = {
    PENDING: '待处理',
    APPROVED: '已同意',
    REJECTED: '已拒绝',
    COMPLETED: '已完成',
    CANCELLED: '已取消'
  }
  return map[status] || status
}

const formatTime = (time) => {
  if (!time) return '-'
  const date = new Date(time)
  return date.toLocaleString('zh-CN', { hour12: false })
}

onMounted(() => {
  fetchAfterSalesList()
})
</script>

<style scoped>
.customer-after-sales-management {
  padding: 20px;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 10px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.detail-content h4 {
  margin: 16px 0 8px;
  font-size: 14px;
  color: #606266;
}

.detail-content p {
  margin: 8px 0;
  font-size: 14px;
  line-height: 1.6;
}

.image-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
</style>