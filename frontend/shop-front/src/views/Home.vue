<template>
  <div class="home-container">
    <el-container>
      <!-- 头部导航 -->
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
            <el-button type="primary" @click="goDashboard">历史下单</el-button>
            <el-button type="warning" @click="logout">退出登录</el-button>
          </template>
        </div>
      </el-header>

      <!-- 主要内容：搜索（含分类弹出） + 商品列表 -->
      <el-main class="main-content">
        <div class="search-wrap">
          <div class="search-bar">
            <!-- 搜索输入在前 -->
            <el-input
                v-model="q"
                placeholder="按关键词搜索商品"
                clearable
                @keyup.enter.native="handleSearch"
                class="search-input"
            />

            <!-- 分类弹出按钮 -->
            <el-popover
                v-model:visible="showCategoryPopover"
                placement="bottom-start"
                width="340"
                trigger="click"
            >
              <div class="category-popover">
                <div class="popover-header">
                  <div style="font-weight:600">选择分类</div>
                  <div style="display:flex; gap:8px; align-items:center;">
                    <el-button type="text" size="small" @click="clearCategory">全部</el-button>
                    <el-button type="text" size="small" @click="showCategoryPopover = false">关闭</el-button>
                  </div>
                </div>

                <el-scrollbar style="max-height:360px;">
                  <el-menu
                      class="category-menu"
                      :default-active="String(categoryId ?? '')"
                      @select="onSelectCategoryFromPopover"
                      :router="false"
                  >
                    <template v-for="c in categories" :key="`p-${c.category_id}`">
                      <el-sub-menu v-if="c.children && c.children.length" :index="`p-${c.category_id}`">
                        <template #title>
                          <span>{{ c.category_name }}</span>
                        </template>
                        <el-menu-item
                            v-for="child in c.children"
                            :key="child.category_id"
                            :index="String(child.category_id)"
                        >
                          {{ child.category_name }}
                        </el-menu-item>
                      </el-sub-menu>

                      <el-menu-item v-else :key="c.category_id" :index="String(c.category_id)">
                        {{ c.category_name }}
                      </el-menu-item>
                    </template>
                  </el-menu>
                </el-scrollbar>
              </div>

              <template #reference>
                <button
                    class="category-button"
                    :aria-expanded="String(showCategoryPopover)"
                    @click.stop
                >
                  <span class="cat-label">{{ currentCategoryName || '分类' }}</span>
                  <i class="el-icon-menu" style="font-size:14px;"></i>
                  <i class="caret el-icon-arrow-down" :class="{ open: showCategoryPopover }"></i>
                </button>
              </template>
            </el-popover>

            <el-button type="primary" @click="handleSearch">搜索</el-button>

            <div class="total-text">共 {{ total }} 条</div>
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
                    <!-- 渲染为纯文本、仅一行显示，多出部分省略，title 显示全文 -->
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

        <!-- 无商品提示 -->
        <div v-else class="empty-state">
          <el-empty description="暂无商品在售">
            <el-button type="primary" @click="$router.push('/seller')">卖家登录</el-button>
          </el-empty>
        </div>

        <!-- 分页条 -->
        <div v-if="total > 0" style="margin-top: 12px;">
          <PaginationBar
              v-model="page"
              :page-size="size"
              :total="total"
              @change="({ page: p, size: s }) => fetchProducts(p, s)"
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

const isCustomerLogged = computed(() => !!customerStore.token)

/* 导航与会话操作 */
const goBuyerLogin = () => {
  router.push({ path: '/login', query: { redirect: route.fullPath } }).catch(() => {})
}
const goSellerLogin = () => {
  router.push('/seller').catch(() => {})
}
const goDashboard = () => {
  router.push('/dashboard').catch(() => {})
}
const logout = () => {
  customerStore.logout()
  ElMessage.success('已退出登录')
  router.push('/').catch(() => {})
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

const fetchProducts = async (p = page.value, s = size.value) => {
  try {
    const params = { page: p, size: s }
    if (q.value && q.value.toString().trim()) params.q = q.value.toString().trim()
    if (categoryId.value) params.category_id = categoryId.value

    const response = await productAPI.getProducts(params)
    const payload = extractData(response)
    let items = []

    if (!payload) {
      items = []
    } else if (Array.isArray(payload.items) && payload.items.length) {
      items = payload.items
    } else if (Array.isArray(payload) && payload.length) {
      items = payload
    } else if (payload && (payload.product_id || payload.product_id === 0)) {
      items = [payload]
    } else {
      items = []
    }

    // 规范化字段
    items = items.map(item => {
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

    page.value = p
    size.value = s
    total.value = Number(payload?.total ?? items.length ?? 0)
    products.value = items
  } catch (error) {
    console.error('获取商品失败:', error)
    products.value = []
    total.value = 0
  }
}

const handleSearch = () => {
  page.value = 1
  fetchProducts()
}

const onSelectCategoryFromPopover = (index) => {
  if (index === '' || index == null) categoryId.value = null
  else categoryId.value = Number(index)
  showCategoryPopover.value = false
  page.value = 1
  fetchProducts()
}

const clearCategory = () => {
  categoryId.value = null
  showCategoryPopover.value = false
  page.value = 1
  fetchProducts()
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

onMounted(() => {
  fetchCategories()
  fetchProducts()
})
const stripHtml = (input) => {
  if (input === null || input === undefined) return ''
  let s = input
  if (typeof s !== 'string') {
    try { s = JSON.stringify(s) } catch { s = String(s) }
  }
  // 使用 DOM 去掉 html
  const div = document.createElement('div')
  div.innerHTML = s
  return (div.textContent || div.innerText || '').trim()
}
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

/* 搜索栏与分类弹出 */
.search-wrap { margin-bottom: 12px; }
.search-bar { display:flex; gap:8px; align-items:center; width:100%; }
.search-input { flex:1; max-width:640px; }

/* 美化后的分类按钮 */
.category-button {
  display:inline-flex;
  align-items:center;
  gap:8px;
  padding:6px 12px;
  border-radius:20px;
  background: #f4f8ff;
  color: #2b7cff;
  border: 1px solid #dbeeff;
  box-shadow: 0 1px 0 rgba(66,133,244,0.04);
  cursor: pointer;
  height:36px;
  font-size:14px;
  outline: none;
}
.category-button .cat-label {
  max-width:160px;
  white-space:nowrap;
  overflow:hidden;
  text-overflow:ellipsis;
  display:inline-block;
  vertical-align:middle;
}
.category-button .caret {
  transition: transform .18s ease;
  font-size:12px;
}
.category-button .caret.open {
  transform: rotate(180deg);
}

/* total 文本放右侧 */
.total-text { margin-left:auto; color:#666; }

/* popover 内的分类 */
.category-popover { padding:8px; }
.popover-header { display:flex; justify-content:space-between; align-items:center; margin-bottom:6px; }

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
