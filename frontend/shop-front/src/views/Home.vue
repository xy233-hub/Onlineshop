<!-- 文件：`Home.vue` -->
<template>
  <div class="home-container">
    <el-container>
      <el-header class="header">
        <div class="nav-brand">
          <h1>在线购物系统</h1>
        </div>

        <div class="header-actions">
          <template v-if="!isCustomerLogged">
            <el-button type="primary" @click="goBuyerLogin">买家登录</el-button>
            <el-button type="primary" @click="goSellerLogin">卖家登录</el-button>
          </template>
          <template v-else>
            <el-button type="primary" @click="goDashboard">我的中心</el-button>
            <el-button type="warning" @click="logout">退出登录</el-button>
          </template>
        </div>
      </el-header>

      <el-main class="main-content">
        <div class="search-wrap">
          <div class="search-box">
            <!-- 左：普通搜索才显示分类 -->
            <div class="search-left" v-if="searchMode === 'normal'">
              <el-popover
                  v-model:visible="showCategoryPopover"
                  placement="bottom-start"
                  :width="320"
                  trigger="click"
                  popper-class="category-popover"
              >
                <template #reference>
                  <button class="category-button" type="button">
                    <span class="cat-label">
                      {{ currentCategoryName || '全部分类' }}
                    </span>
                    <span class="caret" :class="{ open: showCategoryPopover }">▼</span>
                  </button>
                </template>

                <div class="popover-header">
                  <div style="font-weight:600;">选择分类</div>
                  <el-button size="small" text type="primary" @click="clearCategory">清除</el-button>
                </div>

                <el-scrollbar max-height="260px">
                  <el-menu
                      :default-active="categoryId ? String(categoryId) : ''"
                      @select="onSelectCategoryFromPopover"
                  >
                    <el-menu-item index="">全部分类</el-menu-item>

                    <template v-for="c in categories" :key="c.category_id">
                      <el-sub-menu
                          v-if="c.children && c.children.length"
                          :index="String(c.category_id)"
                      >
                        <template #title>{{ c.category_name }}</template>
                        <el-menu-item
                            v-for="cc in c.children"
                            :key="cc.category_id"
                            :index="String(cc.category_id)"
                        >
                          {{ cc.category_name }}
                        </el-menu-item>
                      </el-sub-menu>

                      <el-menu-item v-else :index="String(c.category_id)">
                        {{ c.category_name }}
                      </el-menu-item>
                    </template>
                  </el-menu>
                </el-scrollbar>
              </el-popover>
            </div>

            <!-- 中：搜索输入 -->
            <div class="search-middle" :class="{ 'ai-mode': searchMode === 'ai' }">
            <div class="search-middle">
              <el-input
                  v-model="q"
                  class="search-input"
                  clearable
                  :placeholder="searchMode === 'ai' ? '对我说：你想买什么' : '搜索商品名称'"
                  @keyup.enter="handleSearch"
              />
            </div>
            </div>
            <!-- 右：模式切换 + 搜索按钮 -->
            <div class="search-right">
              <el-segmented
                  v-model="searchMode"
                  :options="modeOptions"
                  class="mode-switch"
                  @change="onModeChange"
              />
              <el-button type="primary" class="search-btn" @click="handleSearch">
                {{ searchMode === 'ai' ? 'AI 推荐' : '搜索' }}
              </el-button>
            </div>
          </div>

          <!-- AI 文案 -->
          <div v-if="searchMode === 'ai' && aiDescription" class="ai-desc">
            <el-alert
                :title="aiDescription"
                type="info"
                :closable="false"
                show-icon
            />
          </div>
        </div>

        <div v-if="products.length" class="products-grid">
          <div class="grid-item" v-for="product in products" :key="product.product_id">
            <el-card class="product-card">
              <template #header>
                <div class="card-header">
                  <span class="product-name" :title="product.product_name">{{ product.product_name }}</span>
                  <el-tag :type="getStatusType(product.product_status)">
                    {{ getStatusText(product.product_status) }}
                  </el-tag>
                </div>
              </template>

              <div class="product-content">
                <div class="product-image">
                  <el-image
                      :src="product.image_url || ''"
                      :alt="product.product_name"
                      fit="cover"
                      class="thumb"
                  />
                </div>

                <div class="product-info">
                  <div class="price-section">
                    <span class="price">¥{{ product.price ?? 0 }}</span>
                  </div>

                  <div class="description">
                    <p
                        class="desc-text single-line-ellipsis"
                        :title="stripHtml(product.product_desc)"
                        v-text="stripHtml(product.product_desc)"
                    ></p>
                  </div>

                  <div class="action-section">
                    <el-tag type="info">库存: {{ product.stock_quantity ?? 0 }}</el-tag>
                    <el-button type="text" @click="$router.push(`/product/${product.product_id}`)">查看详情</el-button>
                  </div>
                </div>
              </div>
            </el-card>
          </div>
        </div>

        <div v-else class="empty-state">
          <el-empty :description="searchMode === 'ai' ? '暂无推荐结果' : '暂无商品在售'">
            <el-button v-if="searchMode !== 'ai'" type="primary" @click="$router.push('/seller')">卖家登录</el-button>
          </el-empty>
        </div>

        <div v-if="total > 0" style="margin-top: 12px;">
          <PaginationBar
              v-model="page"
              :page-size="size"
              :total="total"
              @change="({ page: p, size: s }) => onPageChange(p, s)"
          />
        </div>
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { productAPI, categoryAPI } from '@/api'
import PaginationBar from '@/components/PaginationBar.vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useCustomerStore } from '@/stores/customer'
import api from '@/api' // 复用同一个 axios 实例（`index.js` 默认导出）

const router = useRouter()
const route = useRoute()
const customerStore = useCustomerStore()

const page = ref(1)
const size = ref(8)
const total = ref(0)
const products = ref([])
const q = ref('')
const categoryId = ref(null)
const categories = ref([])
const showCategoryPopover = ref(false)

const searchMode = ref('normal') // normal | ai
const modeOptions = [
  { label: '普通搜索', value: 'normal' },
  { label: 'AI 推荐', value: 'ai' }
]
const aiDescription = ref('')

const isCustomerLogged = computed(() => !!customerStore.token)

const goBuyerLogin = () => {
  router.push({ path: '/login', query: { redirect: route.fullPath } }).catch(() => {})
}
const goSellerLogin = () => {
  router.push('/seller').catch(() => {})
}
const goDashboard = () => {
  router.push('/customer/dashboard').catch(() => {})
}
const LOGOUT_TOAST_KEY = 'post_reload_toast'
const logout = async () => {
  try {
    if (typeof customerStore.logout === 'function') {
      await customerStore.logout()
    } else {
      customerStore.token = ''
      customerStore.customer = null
      localStorage.removeItem('customer_info')
      localStorage.removeItem('customer')
      localStorage.removeItem('customer_id')
      localStorage.removeItem('token')
    }

    sessionStorage.setItem(
        LOGOUT_TOAST_KEY,
        JSON.stringify({ type: 'success', message: '已退出登录', ts: Date.now() })
    )

    await router.replace({ path: '/' })
    window.location.reload()
  } catch (e) {
    console.error(e)
    ElMessage.error('退出登录失败')
  }
}

const extractData = (res) => res?.data?.data ?? res?.data ?? null

const fetchCategories = async () => {
  try {
    const res = await categoryAPI.getCategories({ tree: true, size: 100 })
    const d = extractData(res) || []
    categories.value = Array.isArray(d) ? d : []
  } catch (e) {
    console.error('获取分类失败', e)
    categories.value = []
  }
}

const normalizeItems = (items) => {
  const list = Array.isArray(items) ? items : []
  return list.map(item => {
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

const fetchProductsNormal = async (p = page.value, s = size.value) => {
  const params = { page: p, size: s }
  if (q.value && q.value.toString().trim()) params.q = q.value.toString().trim()
  if (categoryId.value) params.category_id = categoryId.value

  const response = await productAPI.getProducts(params)
  const payload = extractData(response)

  const items = normalizeItems(payload?.items ?? [])
  page.value = p
  size.value = s
  total.value = Number(payload?.total ?? items.length ?? 0)
  products.value = items
}

const fetchProductsAi = async (p = page.value, s = size.value) => {
  const text = (q.value ?? '').toString().trim()
  const body = { text, page: p, size: s }

  const response = await api.post('/products/ai-recommend', body)
  const payload = extractData(response) || {}

  aiDescription.value = payload.ai_description ?? ''
  const items = normalizeItems(payload.items ?? [])
  page.value = Number(payload.page ?? p)
  size.value = Number(payload.size ?? s)
  total.value = Number(payload.total ?? items.length ?? 0)
  products.value = items
}

const fetchProducts = async (p = page.value, s = size.value) => {
  try {
    if (searchMode.value === 'ai') {
      await fetchProductsAi(p, s)
    } else {
      await fetchProductsNormal(p, s)
    }
  } catch (error) {
    console.error('获取商品失败:', error)
    products.value = []
    total.value = 0
    if (searchMode.value === 'ai') aiDescription.value = ''
  }
}

const handleSearch = () => {
  page.value = 1
  fetchProducts(1, size.value)
}

const onPageChange = (p, s) => {
  fetchProducts(p, s)
}

const onModeChange = () => {
  // 切到 AI 时不需要分类过滤；切回普通保留输入框即可
  page.value = 1
  if (searchMode.value === 'ai') {
    categoryId.value = null
    showCategoryPopover.value = false
  } else {
    aiDescription.value = ''
  }
  fetchProducts(1, size.value)
}

const onSelectCategoryFromPopover = (index) => {
  if (index === '' || index == null) categoryId.value = null
  else categoryId.value = Number(index)
  showCategoryPopover.value = false
  page.value = 1
  fetchProducts(1, size.value)
}

const clearCategory = () => {
  categoryId.value = null
  showCategoryPopover.value = false
  page.value = 1
  fetchProducts(1, size.value)
}

const currentCategoryName = computed(() => {
  if (!categoryId.value) return ''
  const stack = [...categories.value]
  while (stack.length) {
    const n = stack.shift()
    if (!n) continue
    if (n.category_id === categoryId.value) return n.category_name
    if (n.children && n.children.length) stack.push(...n.children)
  }
  return ''
})

const getStatusType = (status) => {
  const types = { online: 'success', frozen: 'warning', sold: 'info' }
  return types[status] || 'info'
}
const getStatusText = (status) => {
  const texts = { online: '在售', frozen: '交易中', sold: '已售出' }
  return texts[status] || status
}

const stripHtml = (input) => {
  if (input === null || input === undefined) return ''
  let s = input
  if (typeof s !== 'string') {
    try { s = JSON.stringify(s) } catch { s = String(s) }
  }
  const div = document.createElement('div')
  div.innerHTML = s
  return (div.textContent || div.innerText || '').trim()
}

onMounted(() => {
  fetchCategories()
  fetchProducts()
  try {
    const raw = sessionStorage.getItem(LOGOUT_TOAST_KEY)
    if (!raw) return
    sessionStorage.removeItem(LOGOUT_TOAST_KEY)
    const toast = JSON.parse(raw)
    if (toast?.message) {
      ElMessage({ type: toast.type || 'success', message: toast.message, duration: 2000 })
    }
  } catch {
    // ignore
  }
})
</script>



<style scoped>
*,
*::before,
*::after { box-sizing: border-box; }

.home-container {
  min-height: 100vh;
  background: #f5f7fb;
  padding-bottom: 40px;
}

.header {
  background: #fff;
  border-bottom: 1px solid #e9eef6;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}
.nav-brand h1 {
  margin: 0;
  color: #409eff;
  font-size: 22px;
  font-weight: 700;
}
.header-actions { display:flex; align-items:center; }

.main-content {
  max-width: 1200px;
  margin: 24px auto;
  padding: 20px;
}

/* 搜索栏整体居中 */
.search-wrap {
  width: 100%;
  max-width: 1440px;
  margin: 0 auto;
  padding: 0 32px;
  box-sizing: border-box;
}

/* 大框：三栏容器 */
.search-box {
  width: 100%;
  max-width: 980px;
  margin: 0 auto;
  padding: 12px;
  border-radius: 14px;
  border: 1px solid #e8eef6;
  background: #fff;
  box-shadow: 0 10px 26px rgba(16, 24, 40, 0.06);
  display: flex;
  align-items: center;
  gap: 10px;
  box-sizing: border-box;
}

/* 左\／中\／右三栏 */
.search-left {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
}
.search-middle {
  flex: 1 1 auto;
  min-width: 0;
}
.search-right {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
}

/* 输入框撑满中间 */
.search-input {
  width: 100%;
}

/* 右侧按钮固定宽度 */
.search-btn {
  height: 36px;
  padding: 0 18px;
  border-radius: 10px;
  white-space: nowrap;
}


/* 分类按钮 */
.category-button {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  border-radius: 20px;
  background: #f4f8ff;
  color: #2b7cff;
  border: 1px solid #dbeeff;
  cursor: pointer;
  height: 36px;
  font-size: 14px;
  outline: none;
}
.category-button .cat-label {
  max-width: 160px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  display: inline-block;
}
.category-button .caret {
  transition: transform 0.18s ease;
  font-size: 12px;
}
.category-button .caret.open {
  transform: rotate(180deg);
}



/* popover 内 */
.category-popover {
  padding: 8px;
}
.popover-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}
/* 网格样式 */
.products-grid {
  display: grid !important;
  grid-template-columns: repeat(4, minmax(180px, 1fr)) !important;
  gap: 20px;
  align-items: start;
  justify-items: stretch;
  width: 100%;
  box-sizing: border-box;
  padding: 8px 0;
}
.grid-item { display:flex; align-items:stretch; width:100%; }

.product-card {
  width: 100%;
  min-height: 320px;
  display: flex;
  flex-direction: column;
  border-radius: 10px;
  border: 1px solid #e8eef6;
  background: #ffffff;
  overflow: visible;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}
.product-card:hover { transform: translateY(-6px); box-shadow: 0 8px 28px rgba(16,24,40,0.08); }

.product-image { width:100%; height:160px; background:#fafbfd; display:flex; align-items:center; justify-content:center; }
.product-image .thumb { width:100%; height:100%; object-fit:cover; display:block; }

.product-content { display:flex; flex-direction:column; padding:12px 14px; gap:8px; flex:1; box-sizing:border-box; }
.card-header { display:flex; align-items:center; justify-content:space-between; gap:8px; padding:0; }
.product-name { font-weight:600; font-size:14px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis; margin-right:8px; max-width:calc(100% - 56px); }

.price { font-size:18px; color:#f56c6c; font-weight:700; }
.desc-text { color:#666; font-size:13px; margin:4px 0; line-height:1.4; }

.action-section { margin-top:auto; display:flex; gap:8px; align-items:center; }
.empty-state { text-align:center; padding:100px 0; }

/* 响应式 */
@media (max-width: 1000px) {
  .products-grid { grid-template-columns: repeat(3, minmax(160px, 1fr)) !important; }
}
@media (max-width: 700px) {
  .products-grid { grid-template-columns: repeat(2, minmax(140px, 1fr)) !important; }
}
@media (max-width: 420px) {
  .products-grid { grid-template-columns: repeat(1, minmax(140px, 1fr)) !important; }
}


</style>
