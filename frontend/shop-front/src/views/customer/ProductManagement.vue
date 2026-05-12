<template>
  <div class="product-management">
    <div style="display:flex; gap:12px; margin-bottom:12px; align-items:center;">
      <el-button type="primary" @click="showCreate = true">创建新商品</el-button>

      <el-button
          type="success"
          :disabled="selectedProducts.length === 0"
          @click="handleBatchUnfreeze"
      >
        批量上架
      </el-button>

      <div style="margin-left:auto; color:#888">共 {{ total }} 条</div>
    </div>

    <el-table :data="products" v-loading="loading" stripe style="width:100%" @selection-change="handleSelectionChange">
      <el-table-column prop="product_id" label="ID" width="90" type="selection"/>
      <el-table-column label="商品">
        <template #default="{ row }">
          <div style="display:flex; gap:12px; align-items:center">
            <el-image :src="row.image_url" style="width:72px; height:54px; object-fit:cover" />
            <div>
              <div style="font-weight:600">{{ row.product_name }}</div>
              <div style="color:#999; font-size:12px; max-width:380px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap">
                {{ row.search_keywords || '' }}
              </div>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="price" label="价格" width="110">
        <template #default="{ row }">¥{{ row.price }}</template>
      </el-table-column>
      <el-table-column prop="stock_quantity" label="库存" width="100" />
      <el-table-column prop="product_status" label="状态" width="140">
        <template #default="{ row }">
          <el-tag :type="row.product_status === 'online' ? 'success' : (row.product_status === 'frozen' ? 'warning' : 'info')">
            {{ row.product_status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="created_at" label="创建时间" width="180">
        <template #default="{ row }">{{ new Date(row.created_at).toLocaleString() }}</template>
      </el-table-column>

      <el-table-column label="操作" width="460">
        <template #default="{ row }">
          <el-button size="small" type="primary" @click="goToDetail(row.product_id)">查看</el-button>

          <el-button
              size="small"
              type="warning"
              v-if="row.product_status !== 'frozen'"
              @click="handleFreeze(row)"
          >下架</el-button>

          <el-button
              size="small"
              type="success"
              v-else
              @click="handleUnfreeze(row)"
          >上架</el-button>

          <el-button
              size="small"
              type="info"
              :disabled="row.product_status === 'sold'"
              @click="handleMarkSold(row)"
          >标记已售</el-button>

          <el-button
              size="small"
              type="danger"
              @click="openPriceDialog(row)"
          >修改价格</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div style="margin-top:16px; display:flex; justify-content:flex-end; align-items:center;">
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

    <ProductForm v-model:visible="showCreate" @created="onProductCreated" publisher-role="customer" />

    <!-- 修改价格对话框 -->
    <el-dialog
        v-model="priceDialogVisible"
        title="修改商品价格"
        width="500px"
        :close-on-click-modal="false"
    >
      <el-form :model="priceForm" :rules="priceRules" ref="priceFormRef" label-width="100px">
        <el-form-item label="商品名称">
          <span>{{ currentProduct?.product_name }}</span>
        </el-form-item>
        <el-form-item label="当前价格">
          <span style="color: #f56c6c; font-weight: 600;">¥{{ currentProduct?.price }}</span>
        </el-form-item>
        <el-form-item label="新价格" prop="newPrice">
          <el-input-number
              v-model="priceForm.newPrice"
              :min="0.01"
              :precision="2"
              :step="1"
              style="width: 100%;"
              placeholder="请输入新价格"
          />
        </el-form-item>
        <el-form-item label="修改原因" prop="reason">
          <el-input
              v-model="priceForm.reason"
              type="textarea"
              :rows="3"
              placeholder="请输入价格修改原因（可选）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="priceDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="priceUpdating" @click="handleUpdatePrice">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { customerProductAPI, categoryAPI } from '@/api'
import ProductForm from '@/components/seller/ProductForm.vue'

/** 状态 */
const q = ref('')
const categoryId = ref(null as number | null)
const status = ref('')
const page = ref(1)
const size = ref(20)
const total = ref(0)
const products = ref<any[]>([])
const loading = ref(false)
const showCreate = ref(false)
const categories = ref<any[]>([])
const router = useRouter()
const selectedProducts = ref<any[]>([])

// 价格修改相关
const priceDialogVisible = ref(false)
const priceUpdating = ref(false)
const priceFormRef = ref(null)
const currentProduct = ref<any>(null)
const priceForm = reactive({
  newPrice: 0,
  reason: ''
})

const priceRules = {
  newPrice: [
    { required: true, message: '请输入新价格', trigger: 'blur' },
    { type: 'number', min: 0.01, message: '价格必须大于0', trigger: 'blur' }
  ]
}

const extractData = (res: any) => res?.data?.data ?? res?.data ?? null

const handleSelectionChange = (val: any[]) => {
  selectedProducts.value = val || []
}

const handleBatchUnfreeze = async () => {
  if (!selectedProducts.value.length) return
  try {
    await ElMessageBox.confirm(`确认将选中的 ${selectedProducts.value.length} 个商品上架？`, '确认', { type: 'warning' })
    const targets = selectedProducts.value.filter(p => p.product_status !== 'online')
    if (!targets.length) {
      ElMessage.info('所选商品均已为在线状态')
      return
    }
    const promises = targets.map(p => customerProductAPI.unfreezeProduct(p.product_id))
    const results = await Promise.allSettled(promises)
    const successCount = results.filter(r => r.status === 'fulfilled').length
    const failCount = results.length - successCount
    if (successCount) ElMessage.success(`成功上架 ${successCount} 个商品`)
    if (failCount) ElMessage.error(`${failCount} 个商品上架失败`)
    await fetchProducts()
    selectedProducts.value = []
  } catch (e) {
    // 取消或错误，不额外提示
  }
}

const fetchCategories = async () => {
  try {
    const res = await categoryAPI.getCategories({ tree: false, size: 100 })
    const d = extractData(res)
    if (Array.isArray(d.items)) categories.value = d.items
    else if (Array.isArray(d)) categories.value = d
    else categories.value = []
  } catch (e) {
    categories.value = []
  }
}

const normalizeItems = (items: any[]) => {
  return items.map(item => {
    const copy = { ...item }
    let img = ''
    if (Array.isArray(copy.images) && copy.images.length) {
      const first = copy.images[0]
      if (typeof first === 'string') img = first
      else if (first && (first.image_url || first.url)) img = first.image_url || first.url
    } else if (copy.image_url) {
      img = copy.image_url
    } else if (copy.images && typeof copy.images === 'string') {
      img = copy.images
    }
    copy.image_url = img || ''
    copy.product_desc = copy.product_desc ?? ''
    copy.price = copy.price ?? 0
    return copy
  })
}

const fetchProducts = async (p = page.value, s = size.value) => {
  loading.value = true
  try {
    const params: any = { page: p, size: s }
    if (q.value) params.q = q.value
    if (categoryId.value) params.category_id = categoryId.value
    if (status.value) params.status = status.value

    const res = await customerProductAPI.getMyProducts(params)
    const d = extractData(res)
    let items: any[] = []
    if (!d) items = []
    else if (Array.isArray(d.items)) items = d.items
    else if (Array.isArray(d)) items = d
    else if (d && (d.product_id || d.product_id === 0)) items = [d]
    else items = []

    items = normalizeItems(items)
    products.value = items
    page.value = Number(d?.page ?? p)
    size.value = Number(d?.size ?? s)
    total.value = Number(d?.total ?? items.length)
  } catch (err) {
    console.error(err)
    ElMessage.error('获取商品列表失败')
    products.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchCategories()
  fetchProducts()
})

const handleSearch = () => {
  page.value = 1
  fetchProducts()
}

const handlePageChange = (p: number) => {
  page.value = p
  fetchProducts()
}

const handleSizeChange = (s: number) => {
  size.value = s
  page.value = 1
  fetchProducts()
}

const goToDetail = (id: number) => {
  router.push({ path: `/product/${id}` })
}

const handleFreeze = async (product: any) => {
  try {
    await ElMessageBox.confirm('确认冻结（下架）此商品？', '确认', { type: 'warning' })
    await customerProductAPI.freezeProduct(product.product_id)
    ElMessage.success('商品已下架')
    fetchProducts()
  } catch (e) {
    // 取消或错误
  }
}

const handleUnfreeze = async (product: any) => {
  try {
    await ElMessageBox.confirm('确认重新上架此商品？', '确认', { type: 'warning' })
    await customerProductAPI.unfreezeProduct(product.product_id)
    ElMessage.success('商品已上架')
    fetchProducts()
  } catch (e) {}
}

const handleMarkSold = async (product: any) => {
  try {
    await ElMessageBox.confirm('确认将此商品标记为已售出？', '确认', { type: 'warning' })
    await customerProductAPI.markSold(product.product_id, { sold_quantity: 1 })
    ElMessage.success('商品已标记为已售出')
    fetchProducts()
  } catch (e) {}
}

const onProductCreated = (payload: any) => {
  fetchProducts()
  ElMessage.success('商品创建成功')
}

// 打开价格修改对话框
const openPriceDialog = (product: any) => {
  currentProduct.value = product
  priceForm.newPrice = product.price
  priceForm.reason = ''
  priceDialogVisible.value = true
}

// 处理价格修改
const handleUpdatePrice = async () => {
  if (!priceFormRef.value) return
  
  try {
    await priceFormRef.value.validate()
    
    await ElMessageBox.confirm(
        `确认将商品"${currentProduct.value?.product_name}"的价格从 ¥${currentProduct.value?.price} 修改为 ¥${priceForm.newPrice}？`,
        '确认修改',
        { type: 'warning' }
    )
    
    priceUpdating.value = true
    
    const payload = {
      new_price: priceForm.newPrice,
      change_reason: priceForm.reason || '卖家手动修改'
    }
    
    const res = await customerProductAPI.updatePrice(currentProduct.value.product_id, payload)
    
    if (res?.data?.code === 200) {
      ElMessage.success('价格修改成功')
      priceDialogVisible.value = false
      await fetchProducts()
    } else {
      ElMessage.error(res?.data?.message || '价格修改失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('修改价格失败:', error)
      ElMessage.error(error?.response?.data?.message || '修改价格失败')
    }
  } finally {
    priceUpdating.value = false
  }
}
</script>

<style scoped>
.product-management {
  padding: 12px;
  background: #fff;
  border-radius: 6px;
}
</style>