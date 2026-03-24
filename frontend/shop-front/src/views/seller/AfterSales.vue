<template>
  <div class="after-sales-container">
    <el-card class="after-sales-card">
      <template #header>
        <div class="after-sales-header">
          <h2>售后管理</h2>
          <el-tabs v-model="activeTab" class="after-sales-tabs" @tab-change="handleTabChange">
            <el-tab-pane label="提交申请" name="apply">
            </el-tab-pane>
            <el-tab-pane label="历史申请" name="history">
            </el-tab-pane>
          </el-tabs>
        </div>
      </template>

      <!-- 提交申请 -->
      <div v-if="activeTab === 'apply'">
        <el-form
            ref="afterSalesFormRef"
            :model="afterSalesForm"
            :rules="afterSalesRules"
            label-width="100px"
        >
          <el-form-item label="订单选择" prop="purchaseId">
            <el-select
                v-model="afterSalesForm.purchaseId"
                placeholder="请选择订单"
                :loading="orderLoading"
                @change="handleOrderChange"
            >
              <el-option
                  v-for="order in orderList"
                  :key="order.purchase_id || order.purchaseId"
                  :label="`订单ID: ${order.purchase_id || order.purchaseId} - 商品: ${getProductName(order)}`"
                  :value="order.purchase_id || order.purchaseId"
              >
                <div>
                  <span>订单ID: {{ order.purchase_id || order.purchaseId }}</span>
                  <span style="margin-left: 20px">商品: {{ getProductName(order) }}</span>
                  <span style="margin-left: 20px">金额: {{ order.total_amount || order.totalAmount || '0' }}</span>
                </div>
              </el-option>
            </el-select>
          </el-form-item>

          <el-form-item label="商品ID" prop="productId" style="display: none;">
            <el-input
                v-model="afterSalesForm.productId"
                placeholder="请输入商品ID"
            />
          </el-form-item>

          <el-form-item label="售后类型" prop="serviceType">
            <el-select
                v-model="afterSalesForm.serviceType"
                placeholder="请选择售后类型"
            >
              <el-option label="仅退款" value="REFUND_ONLY" />
              <el-option label="退货退款" value="RETURN_AND_REFUND" />
              <el-option label="换货" value="EXCHANGE" />
            </el-select>
          </el-form-item>

          <el-form-item label="申请标题" prop="serviceTitle">
            <el-input
                v-model="afterSalesForm.serviceTitle"
                placeholder="请输入申请标题"
            />
          </el-form-item>

          <el-form-item label="问题描述" prop="problemDescription">
            <el-input
                v-model="afterSalesForm.problemDescription"
                type="textarea"
                placeholder="请详细描述您遇到的问题"
                :rows="4"
            />
          </el-form-item>

          <el-form-item label="退款金额" prop="refundAmount">
            <el-input
                v-model="afterSalesForm.refundAmount"
                placeholder="请输入退款金额"
                type="number"
            />
          </el-form-item>

          <el-form-item label="凭证图片">
            <el-upload
                class="upload-demo"
                action=""
                :auto-upload="false"
                :on-change="handleImageChange"
                :file-list="fileList"
                list-type="picture-card"
                :limit="5"
            >
              <template #default>
                <el-icon><Plus /></el-icon>
                <div class="el-upload__text">上传图片</div>
              </template>
              <template #file-list="{ file }">
                <div class="el-upload-list__item">
                  <img :src="file.url" class="el-upload-list__item-thumbnail" />
                  <div class="el-upload-list__item-actions">
                    <span class="el-upload-list__item-preview" @click="handlePictureCardPreview(file)">
                      <el-icon><View /></el-icon>
                    </span>
                    <span class="el-upload-list__item-delete" @click="handleRemove(file)">
                      <el-icon><Delete /></el-icon>
                    </span>
                  </div>
                </div>
              </template>
            </el-upload>
            <el-dialog v-model="dialogVisible">
              <img width="100%" :src="dialogImageUrl" alt="预览" />
            </el-dialog>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" @click="handleSubmit" :loading="loading">
              提交申请
            </el-button>
            <el-button @click="resetForm">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 历史申请 -->
      <div v-else-if="activeTab === 'history'">
        <div class="filter-container" style="margin-bottom: 20px; display: flex; gap: 10px; align-items: center;">
          <el-select v-model="filter.serviceStatus" placeholder="按状态筛选">
            <el-option label="全部" value="" />
            <el-option label="待处理" value="PENDING" />
            <el-option label="协商中" value="NEGOTIATING" />
            <el-option label="已同意" value="AGREED" />
            <el-option label="已拒绝" value="REJECTED" />
            <el-option label="已完成" value="COMPLETED" />
            <el-option label="已取消" value="CANCELLED" />
          </el-select>
          <el-button type="primary" @click="loadAfterSalesList">
            刷新列表
          </el-button>
        </div>

        <el-table :data="afterSalesList" style="width: 100%">
          <el-table-column prop="service_id" label="售后单号" width="180" />
          <el-table-column prop="purchase_id" label="订单ID" width="120" />
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
          <el-table-column prop="created_at" label="提交时间" width="180" />
          <el-table-column label="操作" width="150">
            <template #default="scope">
              <el-button type="primary" size="small" @click="viewDetail(scope.row.service_id)">
                查看详情
              </el-button>
            </template>
          </el-table-column>
        </el-table>

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

        <!-- 售后详情对话框 -->
        <el-dialog v-model="detailDialogVisible" title="售后详情" width="800px">
          <div v-if="afterSalesDetail" class="after-sales-detail">
            <el-descriptions :column="2" border>
              <el-descriptions-item label="售后单号">{{ afterSalesDetail.service_id }}</el-descriptions-item>
              <el-descriptions-item label="订单ID">{{ afterSalesDetail.purchase_id }}</el-descriptions-item>
              <el-descriptions-item label="订单信息">
                <div v-if="afterSalesDetail.order_info">
                  <div>{{ afterSalesDetail.order_info.product_name }}</div>
                  <div>¥{{ afterSalesDetail.order_info.order_amount }}</div>
                  <div>数量: {{ afterSalesDetail.order_info.quantity }}</div>
                </div>
                <div v-else>无</div>
              </el-descriptions-item>
              <el-descriptions-item label="商品信息">
                <div v-if="afterSalesDetail.product_info">
                  <div>{{ afterSalesDetail.product_info.product_name }}</div>
                  <div>¥{{ afterSalesDetail.product_info.price }}</div>
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
              <el-descriptions-item label="提交时间">{{ afterSalesDetail.created_at }}</el-descriptions-item>
              <el-descriptions-item label="更新时间">{{ afterSalesDetail.updated_at }}</el-descriptions-item>
            </el-descriptions>
          </div>
          <div v-else class="loading">
            <el-loading :fullscreen="true" text="加载中..." />
          </div>
        </el-dialog>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { afterSalesAPI, mediaAPI, purchaseAPI } from '@/api'
import { Plus, View, Delete } from '@element-plus/icons-vue'

// 从localStorage中恢复activeTab，或者默认为'apply'
const activeTab = ref(localStorage.getItem('activeTab') || 'apply')

// 监听activeTab变化，保存到localStorage
watch(activeTab, (newValue) => {
  localStorage.setItem('activeTab', newValue)
})

// 表单模型
const afterSalesForm = reactive({
  purchaseId: '',
  productId: '',
  serviceType: '',
  serviceTitle: '',
  problemDescription: '',
  refundAmount: '',
  evidenceImages: []
})

// 订单列表
const orderList = ref([])
const orderLoading = ref(false)

// 表单实例
const afterSalesFormRef = ref(null)

// 加载状态
const loading = ref(false)

// 文件列表
const fileList = ref([])
const dialogVisible = ref(false)
const dialogImageUrl = ref('')

// 历史售后申请
const afterSalesList = ref([])

// 筛选条件
const filter = reactive({
  serviceStatus: ''
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

// 表单验证规则
const afterSalesRules = {
  purchaseId: [{ required: true, message: '请输入订单ID', trigger: 'blur' }],
  productId: [{ required: true, message: '请输入商品ID', trigger: 'blur' }],
  serviceType: [{ required: true, message: '请选择售后类型', trigger: 'change' }],
  serviceTitle: [{ required: true, message: '请输入申请标题', trigger: 'blur' }],
  problemDescription: [{ required: true, message: '请描述问题', trigger: 'blur' }],
  refundAmount: [{ required: true, message: '请输入退款金额', trigger: 'blur' }]
}

// 处理图片上传 
const handleImageChange = async (file) => {
  try {
    // 验证文件是否存在
    if (!file || !file.raw) {
      throw new Error('文件不存在')
    }
    
    const formData = new FormData()
    formData.append('file', file.raw || file)
    formData.append('purpose', 'embedded')
    
    console.log('=== 开始上传图片 ===')
    console.log('文件名:', file.name)
    console.log('文件大小:', file.size)
    console.log('文件类型:', file.type)
    
    const response = await mediaAPI.upload(formData)
    
    console.log('=== 上传响应 ===')
    console.log('response.data:', response.data)
    
    // 确保返回的数据结构正确
    if (response.data && response.data.data) {
      const data = response.data.data
      
      // 注意：后端配置了 SNAKE_CASE 命名策略，所以 mediaUrl 会变成 media_url
      const imageUrl = data.media_url || data.mediaUrl || data.url
      
      if (imageUrl) {
        file.url = imageUrl
        
        // 添加到证据图片数组
        afterSalesForm.evidenceImages.push(imageUrl)
        
        // 同时更新 fileList（如果需要）
        if (!fileList.value.find(f => f.uid === file.uid)) {
          fileList.value.push(file)
        }
        
        ElMessage.success('图片上传成功')
      } else {
        console.error('响应中没有 media_url 或 mediaUrl 或 url 字段:', data)
        throw new Error('上传响应格式不正确：缺少 media_url 字段')
      }
    } else {
      console.error('响应格式不正确:', response)
      throw new Error('上传响应格式不正确')
    }
  } catch (error) {
    console.error('=== 图片上传失败 ===')
    console.error('错误对象:', error)
    console.error('错误响应:', error.response)
    console.error('错误数据:', error.response?.data)
    console.error('错误状态码:', error.response?.status)
    
    const errorMsg = error.response?.data?.message || 
                    error.response?.data || 
                    error.message || 
                    '请重试'
    
    ElMessage.error('图片上传失败：' + errorMsg)
    
    // 从 fileList 中移除失败的文件
    const index = fileList.value.findIndex(f => f.uid === file.uid)
    if (index !== -1) {
      fileList.value.splice(index, 1)
    }
  }
}

// 预览图片
const handlePictureCardPreview = (file) => {
  dialogImageUrl.value = file.url
  dialogVisible.value = true
}

// 移除图片 - 修改：同步移除两个数组中的数据
const handleRemove = (file) => {
  // 从 fileList 中移除
  const index = fileList.value.findIndex(item => item.uid === file.uid)
  if (index !== -1) {
    fileList.value.splice(index, 1)
  }
  
  // 从 evidenceImages 中移除
  const imageIndex = afterSalesForm.evidenceImages.findIndex(url => url === file.url)
  if (imageIndex !== -1) {
    afterSalesForm.evidenceImages.splice(imageIndex, 1)
  }
}
// 提交申请 - 修改：添加数据校验
const handleSubmit = async () => {
  loading.value = true
  
  // 过滤掉 null、undefined 和空字符串
  const validImages = afterSalesForm.evidenceImages.filter(url => 
    url != null && url.toString().trim() !== ''
  )
  
  try {
    // 提取订单 ID 中的数字部分，去除可能的前缀
    let purchaseId = afterSalesForm.purchaseId
    if (typeof purchaseId === 'string') {
      // 提取数字部分
      const match = purchaseId.match(/\d+/)
      if (match) {
        purchaseId = match[0]
      }
    }
    
    // 提取商品 ID 中的数字部分，去除可能的前缀
    let productId = afterSalesForm.productId
    if (typeof productId === 'string') {
      // 提取数字部分
      const match = productId.match(/\d+/)
      if (match) {
        productId = match[0]
      }
    }
    
    // 调试：打印提交的数据
    console.log('=== 提交售后申请 ===')
    console.log('purchase_id:', purchaseId)
    console.log('product_id:', productId)
    console.log('evidence_images:', validImages)
    console.log('原始 evidenceImages:', afterSalesForm.evidenceImages)
    
    const response = await afterSalesAPI.createAfterSales({
      purchase_id: purchaseId,
      product_id: productId,
      service_type: afterSalesForm.serviceType,
      service_title: afterSalesForm.serviceTitle,
      problem_description: afterSalesForm.problemDescription,
      refund_amount: afterSalesForm.refundAmount,
      evidence_images: validImages  // 使用过滤后的有效图片数组
    })
    ElMessage.success('售后申请提交成功')
    resetForm()
  } catch (error) {
    console.error('提交失败:', error)
    ElMessage.error('提交失败：' + (error.response?.data?.message || '网络错误'))
  } finally {
    loading.value = false
  }
}


// 重置表单
const resetForm = () => {
  afterSalesFormRef.value.resetFields()
  fileList.value = []
  afterSalesForm.evidenceImages = []
}

// 加载售后列表
const loadAfterSalesList = async () => {
  console.log('开始加载售后列表...')
  loading.value = true
  try {
    // 获取当前登录客户ID
    let customerId = null
    try {
      const customerInfo = localStorage.getItem('customer_info')
      console.log('customer_info:', customerInfo)
      if (customerInfo) {
        const parsedInfo = JSON.parse(customerInfo)
        console.log('parsedInfo:', parsedInfo)
        customerId = parsedInfo?.customer_id
        console.log('customerId:', customerId)
        // 确保customerId是数字类型
        if (customerId && typeof customerId === 'string') {
          customerId = parseInt(customerId)
          console.log('转换后的customerId:', customerId)
        }
      }
    } catch (e) {
      console.error('解析customer_info失败:', e)
    }
    
    if (!customerId) {
      console.error('没有找到customerId')
      ElMessage.error('请先登录')
      return
    }
    
    // 检查是否有customer_token
    const customerToken = localStorage.getItem('customer_token')
    console.log('customer_token:', customerToken)
    
    console.log('准备调用API，customerId:', customerId, '类型:', typeof customerId)
    console.log('分页参数:', { page: pagination.page, size: pagination.size })
    console.log('筛选条件:', { service_status: filter.serviceStatus })
    
    try {
      console.log('开始调用API...')
      const response = await afterSalesAPI.getCustomerAfterSales({
        customer_id: customerId,
        page: pagination.page,
        size: pagination.size,
        service_status: filter.serviceStatus
      })
      
      console.log('收到响应:', response)
      
      if (response && response.data && response.data.data) {
        afterSalesList.value = response.data.data.items || []
        pagination.total = response.data.data.total || 0
        console.log('售后列表数据:', afterSalesList.value)
        console.log('总数量:', pagination.total)
      } else {
        console.log('响应格式不正确:', response)
        afterSalesList.value = []
        pagination.total = 0
      }
    } catch (apiError) {
      console.error('API调用失败:', apiError)
      console.error('API错误响应:', apiError.response)
      console.error('错误消息:', apiError.message)
      ElMessage.error('获取售后列表失败：' + (apiError.response?.data?.message || '网络错误'))
    }
  } catch (error) {
    console.error('获取售后列表失败:', error)
    console.error('错误堆栈:', error.stack)
    ElMessage.error('获取售后列表失败: ' + (error.message || '未知错误'))
  } finally {
    console.log('加载售后列表完成')
    loading.value = false
  }
}

// 查看售后详情
const viewDetail = async (serviceId) => {
  if (!serviceId) {
    ElMessage.error('无效的售后ID')
    return
  }
  loading.value = true
  try {
    const response = await afterSalesAPI.getAfterSalesDetail(serviceId)
    afterSalesDetail.value = response.data.data
    detailDialogVisible.value = true
  } catch (error) {
    ElMessage.error('获取售后详情失败: ' + (error.response?.data?.message || error.message || '未知错误'))
  } finally {
    loading.value = false
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

// 切换到历史申请标签时加载数据
const handleTabChange = (newTab) => {
  console.log('标签切换:', newTab)
  if (newTab === 'history') {
    console.log('切换到历史申请标签，准备加载售后列表')
    loadAfterSalesList()
  }
}

// 从订单中获取商品名称
const getProductName = (order) => {
  if (order.items && order.items.length > 0) {
    return order.items[0].product_name || order.items[0].productName || '未知商品'
  }
  return '未知商品'
}

// 订单选择变化处理
const handleOrderChange = (purchaseId) => {
  // 根据选择的订单ID找到对应的订单
  const selectedOrder = orderList.value.find(order => 
    (order.purchase_id === purchaseId) || (order.purchaseId === purchaseId)
  )
  if (selectedOrder) {
    // 自动填充商品ID
    if (selectedOrder.items && selectedOrder.items.length > 0) {
      afterSalesForm.productId = selectedOrder.items[0].product_id || selectedOrder.items[0].productId
    } else {
      afterSalesForm.productId = selectedOrder.product_id || selectedOrder.productId
    }
  }
}


// 获取用户订单列表
const fetchOrderList = async () => {
  orderLoading.value = true
  try {
    // 获取当前登录客户ID
    const customerId = JSON.parse(localStorage.getItem('customer_info'))?.customer_id
    if (!customerId) {
      ElMessage.error('请先登录')
      return
    }
    
    const response = await purchaseAPI.getCustomerPurchaseIntents(customerId, {
      page: 1,
      size: 50 // 获取足够多的订单
    })
    
    orderList.value = response.data.data.items || []
  } catch (error) {
    ElMessage.error('获取订单列表失败：' + (error.response?.data?.message || '网络错误'))
  } finally {
    orderLoading.value = false
  }
}

// 组件挂载时的操作
onMounted(() => {
  console.log('组件挂载开始...')
  console.log('当前activeTab:', activeTab.value)
  // 获取订单列表
  fetchOrderList()
  
  // 直接加载售后申请列表，无论当前是哪个标签
  console.log('准备调用loadAfterSalesList...')
  loadAfterSalesList()
  console.log('组件挂载完成')
})

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
</script>

<style scoped>
.after-sales-container {
  min-height: 100vh;
  padding: 20px;
  background: #f5f5f5;
}

.after-sales-card {
  max-width: 1000px;
  margin: 0 auto;
}

.after-sales-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.after-sales-header h2 {
  margin: 0;
}

.after-sales-tabs {
  width: 300px;
}

.upload-demo {
  margin-top: 10px;
}

.after-sales-detail {
  padding: 10px 0;
}

.evidence-images {
  display: flex;
  flex-wrap: wrap;
}

.loading {
  min-height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
