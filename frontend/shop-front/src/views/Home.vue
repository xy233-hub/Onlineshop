<template>
  <div class="home-container">
    <!-- 头部导航 -->
    <header class="header">
      <div class="header-inner">
        <div class="nav-brand">
          <h1>光盘行动</h1>
        </div>

        <div class="nav-links">
          <a href="/" class="nav-link active">首页</a>
        </div>

        <div class="header-actions">
          <el-button type="text" class="action-btn" @click="goToCart">
            <el-icon><ShoppingCart /></el-icon>
            <span>购物车</span>
          </el-button>
          <template v-if="!isCustomerLogged">
            <el-button type="primary" plain class="login-btn" @click="goBuyerLogin">买家登录</el-button>
            <el-button type="primary" class="seller-btn" @click="goSellerLogin">卖家登录</el-button>
          </template>
          <template v-else>
            <el-dropdown>
              <el-button type="default" class="user-btn">
                <el-icon><User /></el-icon>
                <span>我的中心</span>
                <el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="goDashboard">个人中心</el-dropdown-item>
                  <el-dropdown-item divided @click="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </div>
      </div>
    </header>

    <!-- 轮播图 -->
    <div class="banner-section">
      <el-carousel :interval="5000" type="card" height="400px" class="banner-carousel">
        <el-carousel-item v-for="(item, index) in bannerItems" :key="index">
          <div class="banner-item" :style="{ backgroundImage: `url(${item.image})` }">
            <div class="banner-content">
              <h2>{{ item.title }}</h2>
              <p>{{ item.subtitle }}</p>
            </div>
          </div>
        </el-carousel-item>
      </el-carousel>
    </div>

    <!-- 主要内容：搜索（含分类弹出） + 商品列表 -->
    <main class="main-content">
      <div class="search-section">
        <div class="search-box">
          <!-- 左：分类 -->
          <div class="search-left">
            <el-popover
                v-model:visible="showCategoryPopover"
                placement="bottom-start"
                :width="320"
                trigger="click"
                popper-class="category-popover"
            >
              <template #reference>
                <button class="category-button" type="button">
                  <el-icon><Grid /></el-icon>
                  <span class="cat-label">
                    {{ currentCategoryName || '全部分类' }}
                  </span>
                  <span class="caret" :class="{ open: showCategoryPopover }"><ArrowDown /></span>
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

                  <!-- 兼容树形分类（children） -->
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

                    <el-menu-item
                        v-else
                        :index="String(c.category_id)"
                    >
                      {{ c.category_name }}
                    </el-menu-item>
                  </template>
                </el-menu>
              </el-scrollbar>
            </el-popover>
          </div>

          <!-- 中：搜索输入 -->
          <div class="search-middle">
            <el-input
                v-model="q"
                class="search-input"
                clearable
                placeholder="搜索商品名称"
                @keyup.enter="handleSearch"
                prefix-icon="Search"
            />
          </div>

          <!-- 右：搜索按钮 -->
          <div class="search-right">
            <el-button type="primary" class="search-btn" @click="handleSearch">
              <el-icon><Search /></el-icon>
              搜索
            </el-button>
          </div>
        </div>
      </div>

      <!-- 商品分类标签 -->
      <div class="category-tabs">
        <el-button 
            v-for="tab in categoryTabs" 
            :key="tab.id"
            :type="activeTab === tab.id ? 'primary' : 'default'"
            @click="switchTab(tab.id)"
            class="tab-btn"
        >
          {{ tab.name }}
        </el-button>
      </div>

      <!-- 商品网格 -->
      <div class="section-title">
        <h2>{{ activeTab === 'all' ? '全部商品' : categoryTabs.find(t => t.id === activeTab)?.name }}</h2>
        <el-button type="text" class="view-more">查看更多</el-button>
      </div>

      <div v-if="products.length" class="products-grid">
        <div class="grid-item" v-for="product in products" :key="product.product_id">
          <div class="product-card">
            <div class="product-image">
              <el-image
                  :src="product.image_url || 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=minimal%20product%20placeholder%20white%20background&image_size=square'"
                  :alt="product.product_name"
                  fit="cover"
                  class="product-img"
              />
              <div class="product-badge" v-if="product.product_status === 'online'">
                <span class="badge-text">在售</span>
              </div>
            </div>

            <div class="product-info">
              <h3 class="product-name" :title="product.product_name">{{ product.product_name }}</h3>
              
              <div class="price-section">
                <span class="price">¥{{ product.price ?? 0 }}</span>
                <span class="stock">库存: {{ product.stock_quantity ?? 0 }}</span>
              </div>

              <div class="description">
                <p
                    class="desc-text"
                    :title="stripHtml(product.product_desc)"
                    v-text="stripHtml(product.product_desc)"
                ></p>
              </div>

              <div class="action-section">
                <el-button type="primary" plain class="add-cart-btn" @click="addToCart(product)">
                  <el-icon><Plus /></el-icon>
                  加入购物车
                </el-button>
                <el-button type="default" class="detail-btn" @click="$router.push(`/product/${product.product_id}`)">
                  <el-icon><View /></el-icon>
                  查看详情
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 无商品提示 -->
      <div v-else class="empty-state">
        <el-empty description="暂无商品在售">
          <el-button type="primary" @click="$router.push('/seller')">卖家登录</el-button>
        </el-empty>
      </div>

      <!-- 分页条 -->
      <div v-if="total > 0" class="pagination-section">
        <PaginationBar
            v-model="page"
            :page-size="size"
            :total="total"
            @change="({ page: p, size: s }) => fetchProducts(p, s)"
        />
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { productAPI, categoryAPI, cartAPI } from '@/api'
import PaginationBar from '@/components/PaginationBar.vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useCustomerStore } from '@/stores/customer'
import { ShoppingCart, User, ArrowDown, Grid, Search, Plus, View } from '@element-plus/icons-vue'

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
const activeTab = ref('all')

// 轮播图数据 - 从已上架商品中获取
const bannerItems = ref([])

// 分类标签 - 从后端获取
const categoryTabs = computed(() => {
  const tabs = [{ id: 'all', name: '全部' }]
  if (categories.value && categories.value.length) {
    // 递归遍历分类树，添加所有分类
    const addCategories = (cats) => {
      cats.forEach(cat => {
        tabs.push({ id: String(cat.category_id), name: cat.category_name })
        if (cat.children && cat.children.length) {
          addCategories(cat.children)
        }
      })
    }
    addCategories(categories.value)
  }
  return tabs
})

const isCustomerLogged = computed(() => !!customerStore.token)

/* 导航与会话操作 */
const goBuyerLogin = () => {
  router.push({ path: '/login', query: { redirect: route.fullPath } }).catch(() => {})
}
const goSellerLogin = () => {
  router.push('/seller').catch(() => {})
}
const goDashboard = () => {
  router.push('/customer/dashboard').catch(() => {})
}
const goToCart = () => {
  router.push('/customer/dashboard/cart').catch(() => {})
}
const goToOrders = () => {
  router.push('/customer/dashboard/orders').catch(() => {})
}
const goToFavorites = () => {
  router.push('/customer/dashboard/favorites').catch(() => {})
}

// 跳转到商品详情页
const goToProductDetail = (productId) => {
  if (productId) {
    router.push(`/product/${productId}`).catch(() => {})
  }
}

const LOGOUT_TOAST_KEY = 'post_reload_toast'

const logout = async () => {
  try {
    // 1) 清理状态
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

    // 2) 把提示“带过刷新”
    sessionStorage.setItem(
        LOGOUT_TOAST_KEY,
        JSON.stringify({ type: 'success', message: '已退出登录', ts: Date.now() })
    )

    // 3) 跳转后刷新（让页面状态彻底重置）
    await router.replace({ path: '/' })
    window.location.reload()
  } catch (e) {
    console.error(e)
    ElMessage.error('退出登录失败')
  }
}

// 切换分类标签
const switchTab = (tabId) => {
  activeTab.value = tabId
  // 这里可以根据标签ID筛选商品
  page.value = 1
  fetchProducts()
}

// 加入购物车
const addToCart = async (product) => {
  if (!isCustomerLogged.value) {
    ElMessage.warning('请先登录')
    goBuyerLogin()
    return
  }
  
  try {
    const customerId = customerStore.customer?.customer_id || localStorage.getItem('customer_id')
    if (!customerId) {
      ElMessage.warning('请先登录')
      goBuyerLogin()
      return
    }
    
    const payload = {
      customer_id: Number(customerId),
      product_id: product.product_id,
      quantity: 1
    }
    
    await cartAPI.addToCart(payload)
    ElMessage.success('已加入购物车')
  } catch (error) {
    console.error('加入购物车失败:', error)
    ElMessage.error('加入购物车失败，请稍后重试')
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

const fetchProducts = async (p = page.value, s = size.value) => {
  try {
    const params = { page: p, size: s }
    if (q.value && q.value.toString().trim()) params.q = q.value.toString().trim()
    if (categoryId.value) params.category_id = categoryId.value

    const response = await productAPI.getProducts(params)
    console.log('获取商品列表响应:', response)
    const payload = extractData(response)
    console.log('提取的payload:', payload)
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
        if (typeof first === 'string') {
          // 将远程图片URL替换为本地地址
          img = first.replace('http://120.55.249.112:8081/media', 'http://localhost:8081/media')
        } else if (first && (first.image_url || first.url)) {
          // 将远程图片URL替换为本地地址
          const url = first.image_url || first.url
          img = url.replace('http://120.55.249.112:8081/media', 'http://localhost:8081/media')
        }
      } else if (copy.image_url) {
        // 将远程图片URL替换为本地地址
        img = copy.image_url.replace('http://120.55.249.112:8081/media', 'http://localhost:8081/media')
      } else if (copy.images && typeof copy.images === 'string') {
        // 将远程图片URL替换为本地地址
        img = copy.images.replace('http://120.55.249.112:8081/media', 'http://localhost:8081/media')
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

// 获取轮播图图片
const fetchBannerProducts = () => {
  // 从本地banner目录加载图片
  bannerItems.value = [
    {
      id: 1,
      image: '/banner/u=540345704,1147443840&fm=253&app=138&f=JPEG.jpg',
      title: '西部风格',
      subtitle: '体验狂野西部的魅力',
      buttonText: '探索更多'
    },
    {
      id: 2,
      image: '/banner/43214a876142c1fd472963bac3c9243ca7b8e3ca.jpg',
      title: '新品上市',
      subtitle: '最新款商品等你来选',
      buttonText: '查看详情'
    },
    {
      id: 3,
      image: '/banner/7d097afb3d4d2c1133f58c6824edc3e4.jpeg',
      title: '限时优惠',
      subtitle: '全场商品低至5折',
      buttonText: '立即抢购'
    },
    {
      id: 4,
      image: '/banner/u=1884076161,1553839118&fm=253&fmt=auto&app=138&f=JPEG.webp',
      title: '会员专享',
      subtitle: '会员购物享额外折扣',
      buttonText: '了解会员'
    }
  ]
}

onMounted(() => {
  fetchCategories()
  fetchProducts()
  fetchBannerProducts()
  try {
    const raw = sessionStorage.getItem(LOGOUT_TOAST_KEY)
    if (!raw) return
    sessionStorage.removeItem(LOGOUT_TOAST_KEY)

    const toast = JSON.parse(raw)
    if (toast?.message) {
      ElMessage({
        type: toast.type || 'success',
        message: toast.message,
        duration: 2000
      })
    }
  } catch {
    // ignore
  }
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
*,*::before,
*::after { box-sizing: border-box; }

.home-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fb 0%, #e3e8f0 100%);
  padding-bottom: 60px;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

/* 头部导航 */
.header {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(224, 230, 237, 0.5);
  position: sticky;
  top: 0;
  z-index: 100;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}

.header-inner {
  max-width: 1440px;
  margin: 0 auto;
  padding: 0 32px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.nav-brand h1 {
  margin: 0;
  color: #1e293b;
  font-size: 28px;
  font-weight: 700;
  letter-spacing: -0.02em;
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.nav-links {
  display: flex;
  gap: 32px;
  align-items: center;
}

.nav-link {
  text-decoration: none;
  color: #475569;
  font-weight: 500;
  font-size: 16px;
  transition: all 0.2s ease;
  position: relative;
  padding: 8px 0;
}

.nav-link:hover,
.nav-link.active {
  color: #3b82f6;
}

.nav-link.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: #3b82f6;
  border-radius: 2px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #475569;
  font-weight: 500;
  transition: all 0.2s ease;
}

.action-btn:hover {
  color: #3b82f6;
}

.login-btn {
  border-radius: 8px;
  padding: 8px 16px;
  font-weight: 500;
  transition: all 0.2s ease;
}

.seller-btn {
  border-radius: 8px;
  padding: 8px 16px;
  font-weight: 500;
  background: #1e293b;
  border-color: #1e293b;
  transition: all 0.2s ease;
}

.seller-btn:hover {
  background: #334155;
  border-color: #334155;
}

.user-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  border-radius: 8px;
  padding: 8px 16px;
  transition: all 0.2s ease;
}

/* 轮播图 */
.banner-section {
  max-width: 1440px;
  margin: 0 auto;
  padding: 24px 32px;
}

.banner-carousel {
  border-radius: 24px;
  overflow: hidden;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
}

.banner-item {
  position: relative;
  height: 400px;
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: center;
  padding: 0 64px;
}

.banner-content {
  max-width: 600px;
  color: white;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.3);
}

.banner-content h2 {
  font-size: 48px;
  font-weight: 700;
  margin-bottom: 16px;
  line-height: 1.2;
}

.banner-content p {
  font-size: 20px;
  margin-bottom: 24px;
  opacity: 0.9;
}

.banner-btn {
  border-radius: 12px;
  padding: 12px 24px;
  font-size: 16px;
  font-weight: 600;
  background: rgba(255, 255, 255, 0.95);
  color: #1e293b;
  border: none;
  transition: all 0.2s ease;
}

.banner-btn:hover {
  background: white;
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.2);
}

/* 主要内容 */
.main-content {
  max-width: 1440px;
  margin: 0 auto;
  padding: 0 32px 40px;
}

/* 搜索区 */
.search-section {
  background: white;
  border-radius: 20px;
  padding: 24px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.06);
  margin-bottom: 32px;
}

.search-box {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 16px;
}

.search-left {
  flex: 0 0 auto;
}

.search-middle {
  flex: 1;
}

.search-right {
  flex: 0 0 auto;
}

.category-button {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  border-radius: 12px;
  background: #f8fafc;
  color: #334155;
  border: 1px solid #e2e8f0;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s ease;
}

.category-button:hover {
  background: #f1f5f9;
  border-color: #cbd5e1;
}

.category-button .caret {
  transition: transform 0.2s ease;
}

.category-button .caret.open {
  transform: rotate(180deg);
}

.search-input {
  width: 100%;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  transition: all 0.2s ease;
}

.search-input:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.search-btn {
  border-radius: 12px;
  padding: 10px 24px;
  font-weight: 500;
  background: #3b82f6;
  border-color: #3b82f6;
  transition: all 0.2s ease;
}

.search-btn:hover {
  background: #2563eb;
  border-color: #2563eb;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

/* 分类标签 */
.category-tabs {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  flex-wrap: wrap;
}

.tab-btn {
  border-radius: 12px;
  padding: 8px 16px;
  font-weight: 500;
  transition: all 0.2s ease;
}

.tab-btn:hover {
  transform: translateY(-1px);
}

/* 区块标题 */
.section-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.section-title h2 {
  font-size: 24px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
}

.view-more {
  color: #64748b;
  font-weight: 500;
  transition: all 0.2s ease;
}

.view-more:hover {
  color: #3b82f6;
}

/* 商品网格 */
.products-grid {
  display: grid !important;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)) !important;
  gap: 24px;
  margin-bottom: 40px;
}

.grid-item {
  display: flex;
  align-items: stretch;
}

.product-card {
  width: 100%;
  background: white;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
  display: flex;
  flex-direction: column;
}

.product-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.12);
}

.product-image {
  position: relative;
  height: 200px;
  background: #f8fafc;
  overflow: hidden;
}

.product-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.product-card:hover .product-img {
  transform: scale(1.05);
}

.product-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  background: #3b82f6;
  color: white;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.product-info {
  padding: 20px;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.product-name {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  margin: 0;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.price-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.price {
  font-size: 20px;
  font-weight: 700;
  color: #ef4444;
}

.stock {
  font-size: 14px;
  color: #64748b;
}

.description {
  flex: 1;
}

.desc-text {
  font-size: 14px;
  color: #64748b;
  line-height: 1.4;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.action-section {
  display: flex;
  gap: 8px;
  margin-top: 16px;
}

.add-cart-btn {
  flex: 1;
  border-radius: 8px;
  font-weight: 500;
  transition: all 0.2s ease;
}

.add-cart-btn:hover {
  border-color: #3b82f6;
  color: #3b82f6;
}

.detail-btn {
  flex: 1;
  border-radius: 8px;
  font-weight: 500;
  transition: all 0.2s ease;
}

.detail-btn:hover {
  border-color: #3b82f6;
  color: #3b82f6;
}

/* 分页 */
.pagination-section {
  display: flex;
  justify-content: center;
  margin-top: 40px;
}

/* 无商品状态 */
.empty-state {
  text-align: center;
  padding: 120px 0;
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

/* 响应式 */
@media (max-width: 1024px) {
  .products-grid {
    grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)) !important;
    gap: 20px;
  }
  
  .banner-content h2 {
    font-size: 36px;
  }
  
  .banner-content p {
    font-size: 18px;
  }
}

@media (max-width: 768px) {
  .header-inner {
    padding: 0 20px;
    height: 70px;
  }
  
  .nav-links {
    display: none;
  }
  
  .banner-section {
    padding: 16px 20px;
  }
  
  .banner-item {
    height: 300px;
    padding: 0 32px;
  }
  
  .banner-content h2 {
    font-size: 28px;
  }
  
  .banner-content p {
    font-size: 16px;
  }
  
  .main-content {
    padding: 0 20px 32px;
  }
  
  .search-section {
    padding: 16px;
  }
  
  .search-box {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }
  
  .products-grid {
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr)) !important;
    gap: 16px;
  }
  
  .product-info {
    padding: 16px;
  }
}

@media (max-width: 480px) {
  .header-inner {
    padding: 0 16px;
  }
  
  .nav-brand h1 {
    font-size: 24px;
  }
  
  .header-actions {
    gap: 8px;
  }
  
  .banner-item {
    height: 250px;
    padding: 0 24px;
  }
  
  .banner-content h2 {
    font-size: 24px;
  }
  
  .products-grid {
    grid-template-columns: repeat(auto-fill, minmax(140px, 1fr)) !important;
  }
  
  .product-name {
    font-size: 14px;
  }
  
  .price {
    font-size: 18px;
  }
}

</style>
