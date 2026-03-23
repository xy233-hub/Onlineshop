<template>
  <div class="customer-product-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <h2>我的商品</h2>
          <el-button type="primary" @click="showCreateDialog = true">发布商品</el-button>
        </div>
      </template>

      <!-- 筛选栏 -->
      <div class="filter-bar">
        <el-input
          v-model="searchForm.q"
          placeholder="搜索商品名称"
          clearable
          style="width: 200px"
          @clear="handleSearch"
        />
        <el-select
          v-model="searchForm.category_id"
          placeholder="商品分类"
          clearable
          style="width: 150px; margin-left: 10px"
          @change="handleSearch"
        >
          <el-option
            v-for="cat in categories"
            :key="cat.category_id"
            :label="cat.category_name"
            :value="cat.category_id"
          />
        </el-select>
        <el-select
          v-model="searchForm.status"
          placeholder="商品状态"
          clearable
          style="width: 120px; margin-left: 10px"
          @change="handleSearch"
        >
          <el-option label="在线销售" value="online" />
          <el-option label="已下架" value="frozen" />
          <el-option label="已售罄" value="sold" />
          <el-option label="缺货" value="outOfStock" />
        </el-select>
        <el-button type="primary" style="margin-left: 10px" @click="handleSearch">搜索</el-button>
      </div>

      <!-- 商品列表 -->
      <el-table
        v-loading="loading"
        :data="products"
        stripe
        style="width: 100%; margin-top: 20px"
      >
        <el-table-column prop="product_id" label="ID" width="80" />
        <el-table-column label="商品图片" width="100">
          <template #default="{ row }">
            <el-image
              :src="row.image_url || row.cover_image || ''"
              fit="cover"
              style="width: 60px; height: 60px"
            />
          </template>
        </el-table-column>
        <el-table-column prop="product_name" label="商品名称" min-width="200" />
        <el-table-column prop="price" label="价格" width="100">
          <template #default="{ row }">¥{{ row.price }}</template>
        </el-table-column>
        <el-table-column prop="stock_quantity" label="库存" width="80" />
        <el-table-column prop="product_status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.product_status)">
              {{ getStatusText(row.product_status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="created_at" label="发布时间" width="160">
          <template #default="{ row }">{{ formatTime(row.created_at) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button
              v-if="row.product_status === 'frozen'"
              size="small"
              type="success"
              @click="handleUnfreeze(row)"
            >上架</el-button>
            <el-button
              v-else-if="row.product_status === 'online'"
              size="small"
              type="warning"
              @click="handleFreeze(row)"
            >下架</el-button>
            <el-button
              v-if="row.product_status !== 'sold'"
              size="small"
              type="primary"
              @click="handleMarkSold(row)"
            >标记已售</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 发布/编辑商品对话框 -->
    <ProductForm
      v-if="showCreateDialog || showEditDialog"
      :edit-data="editData"
      :categories="categories"
      @close="showCreateDialog = false; showEditDialog = false; editData = null"
      @success="handleProductSuccess"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { customerProductAPI, categoryAPI } from '@/api'
import ProductForm from '@/components/seller/ProductForm.vue'

const searchForm = reactive({
  q: '',
  category_id: null,
  status: ''
})

const page = ref(1)
const size = ref(20)
const total = ref(0)
const products = ref([])
const loading = ref(false)
const categories = ref([])
const showCreateDialog = ref(false)
const showEditDialog = ref(false)
const editData = ref(null)

const extractData = (res) => res?.data?.data ?? res?.data ?? null

const normalizeItems = (items) => {
  return items.map(item => {
    const copy = { ...item }
    let img = ''
    if (Array.isArray(copy.images) && copy.images.length) {
      const first = copy.images[0]
      if (typeof first === 'string') img = first
      else if (first && (first.image_url || first.url)) img = first.image_url || first.url
    } else if (copy.image_url) {
      img = copy.image_url
    } else if (copy.cover_image) {
      img = copy.cover_image
    }
    copy.image_url = img || ''
    return copy
  })
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

const fetchProducts = async (p = page.value, s = size.value) => {
  loading.value = true
  try {
    const params = { page: p, size: s }
    if (searchForm.q) params.q = searchForm.q
    if (searchForm.category_id) params.category_id = searchForm.category_id
    if (searchForm.status) params.status = searchForm.status

    const res = await customerProductAPI.getProducts(params)
    const d = extractData(res)
    let items = []
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

const handleSearch = () => {
  page.value = 1
  fetchProducts()
}

const handlePageChange = (p) => {
  page.value = p
  fetchProducts()
}

const handleSizeChange = (s) => {
  size.value = s
  page.value = 1
  fetchProducts()
}

const handleEdit = (product) => {
  editData.value = product
  showEditDialog.value = true
}

const handleProductSuccess = () => {
  showCreateDialog.value = false
  showEditDialog.value = false
  editData.value = null
  ElMessage.success('操作成功')
  fetchProducts()
}

const handleFreeze = async (product) => {
  try {
    await ElMessageBox.confirm('确认下架此商品？', '提示', { type: 'warning' })
    await customerProductAPI.freezeProduct(product.product_id)
    ElMessage.success('商品已下架')
    fetchProducts()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('下架失败')
  }
}

const handleUnfreeze = async (product) => {
  try {
    await ElMessageBox.confirm('确认上架此商品？', '提示', { type: 'warning' })
    await customerProductAPI.unfreezeProduct(product.product_id)
    ElMessage.success('商品已上架')
    fetchProducts()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('上架失败')
  }
}

const handleMarkSold = async (product) => {
  try {
    await ElMessageBox.confirm('确认标记此商品为已售？', '提示', { type: 'warning' })
    await customerProductAPI.markSold(product.product_id)
    ElMessage.success('商品已标记为已售')
    fetchProducts()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('操作失败')
  }
}

const getStatusType = (status) => {
  const map = {
    online: 'success',
    frozen: 'warning',
    sold: 'info',
    outOfStock: 'danger'
  }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = {
    online: '在线销售',
    frozen: '已下架',
    sold: '已售罄',
    outOfStock: '缺货'
  }
  return map[status] || status
}

const formatTime = (time) => {
  if (!time) return '-'
  const date = new Date(time)
  return date.toLocaleString('zh-CN', { hour12: false })
}

onMounted(() => {
  fetchCategories()
  fetchProducts()
})
</script>

<style scoped>
.customer-product-management {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.filter-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>