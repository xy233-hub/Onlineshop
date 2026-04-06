<template>
  <div class="home-container">
    <!-- 头部导航 -->
    <header class="header">
      <div class="header-inner">
        <div class="nav-brand">
          <h1>光盘行动-C版</h1>
        </div>

        <div class="nav-links">
          <a href="/" class="nav-link active">首页</a>
        </div>

        <div class="header-actions">
          <el-button type="text" class="btn-language" @click="toggleLanguage">
            {{ currentLanguage === 'chinese_simplified' ? 'English' : '中文' }}
          </el-button>
          <el-button type="text" class="action-btn" @click="goToCart">
            <el-icon><ShoppingCart /></el-icon>
            <span>购物车</span>
          </el-button>
          <template v-if="!isCustomerLogged">
            <el-dropdown>
              <el-button type="primary" class="login-dropdown-btn">
                <el-icon><User /></el-icon>
                <span>登录</span>
                <el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="goBuyerLogin">
                    <el-icon><User /></el-icon>
                    <span>买家登录</span>
                  </el-dropdown-item>
                  <el-dropdown-item @click="goSellerLogin">
                    <el-icon><Shop /></el-icon>
                    <span>卖家登录</span>
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
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
      <el-carousel :interval="5000" type="card" height="450px" class="banner-carousel" indicator-position="outside">
        <el-carousel-item v-for="(item, index) in bannerItems" :key="index">
          <div class="banner-item" :style="{ backgroundImage: `url(${item.image})` }">
            <div class="banner-overlay"></div>
            <div class="banner-content">
              <h2 class="banner-title">{{ item.title }}</h2>
              <p class="banner-subtitle">{{ item.subtitle }}</p>
              <el-button 
                type="primary" 
                class="banner-btn"
                @click="handleBannerClick(item)"
                :icon="item.buttonIcon || 'ArrowRight'"
              >
                {{ item.buttonText || '了解更多' }}
              </el-button>
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
                :suggestions="searchSuggestions"
                @select-suggestion="handleSuggestionSelect"
                show-word-limit
                maxlength="50"
            >
              <template #suffix>
                <el-button 
                  v-if="q" 
                  icon="Close" 
                  circle 
                  @click="q = ''"
                  class="clear-btn"
                />
              </template>
            </el-input>
            <!-- 搜索建议 -->
            <div v-if="showSuggestions && searchSuggestions.length" class="search-suggestions">
              <div 
                v-for="(suggestion, index) in searchSuggestions" 
                :key="index"
                class="suggestion-item"
                @click="selectSuggestion(suggestion)"
              >
                <el-icon><Search /></el-icon>
                <span>{{ suggestion }}</span>
              </div>
            </div>
          </div>

          <!-- 右：搜索按钮 -->
          <div class="search-right">
            <el-button type="primary" class="search-btn" @click="handleSearch">
              <el-icon><Search /></el-icon>
              搜索
            </el-button>
          </div>
        </div>
        <!-- 热门搜索标签 -->
        <div class="hot-search" v-if="!q">
          <span class="hot-label">热门搜索：</span>
          <el-tag 
            v-for="(tag, index) in hotSearchTags" 
            :key="index"
            class="hot-tag"
            @click="selectHotTag(tag)"
          >
            {{ tag }}
          </el-tag>
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
            :class="{ active: activeTab === tab.id }"
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
              <div class="product-overlay">
                <el-button 
                  type="primary" 
                  circle 
                  class="quick-view-btn"
                  @click="quickViewProduct(product)"
                  :icon="View"
                />
              </div>
            </div>

            <div class="product-info">
              <h3 class="product-name" :title="product.product_name">{{ product.product_name }}</h3>
              
              <div class="price-section">
                <span class="price">¥{{ product.price ?? 0 }}</span>
                <span class="stock" :class="{ 'low-stock': (product.stock_quantity ?? 0) < 10 }">
                  库存: {{ product.stock_quantity ?? 0 }}
                </span>
              </div>

              <div class="description">
                <p
                    class="desc-text"
                    :title="getCardSummary(product)"
                    v-text="getCardSummary(product)"
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

      <!-- 热门游戏推荐 -->
      <div class="featured-games-section">
        <div class="section-title">
          <h2>热门游戏推荐</h2>
          <el-button type="text" class="view-more">查看更多</el-button>
        </div>
        <div class="featured-games-grid">
          <div class="featured-game-item" v-for="(game, index) in featuredGames" :key="index">
            <div class="featured-game-image">
              <el-image
                  :src="game.image"
                  :alt="game.name"
                  fit="cover"
                  class="featured-game-img"
              />
              <div class="featured-game-overlay">
                <el-button type="primary" plain class="featured-game-btn">
                  查看详情
                </el-button>
              </div>
            </div>
            <div class="featured-game-info">
              <h3 class="featured-game-name">{{ game.name }}</h3>
              <p class="featured-game-desc">{{ game.description }}</p>
            </div>
          </div>
        </div>
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

    <!-- 右下角 AI 助手入口 -->
    <el-button class="ai-fab" type="primary" circle @click="aiPanelVisible = true" :icon="ChatDotRound" />

    <!-- AI 助手抽屉 -->
    <el-drawer
      v-model="aiPanelVisible"
      title="AI 对话助手"
      direction="rtl"
      :size="aiDrawerSize"
      class="ai-drawer"
    >
      <div class="ai-panel">
        <div ref="aiChatBodyRef" class="ai-chat-body">
          <div v-if="!aiMessages.length" class="ai-empty-chat">
            <el-empty description="开始聊聊你的需求吧" />
          </div>

          <div
            v-for="(msg, idx) in aiMessages"
            :key="`${msg.role}-${idx}-${msg.time}`"
            class="ai-msg-row"
            :class="msg.role === 'user' ? 'is-user' : 'is-assistant'"
          >
            <div class="ai-bubble">
              <div class="ai-msg-text">{{ msg.text }}</div>
              <div v-if="msg.items && msg.items.length" class="ai-msg-items">
                <div v-for="item in msg.items" :key="item.product_id" class="ai-mini-item">
                  <span class="name">{{ item.product_name }}</span>
                  <span class="price">¥{{ item.price ?? 0 }}</span>
                  <el-button type="primary" link @click="goToProductDetail(item.product_id)">查看详情</el-button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="ai-input-box">
          <el-input
            v-model="aiInput"
            type="textarea"
            :rows="3"
            resize="none"
            :placeholder="aiInputPlaceholder"
            @keyup.enter.ctrl="submitAiQuery"
          />
          <div class="ai-actions-row">
            <el-select v-model="aiScene" size="small" class="ai-scene-select" placeholder="选择模式">
              <el-option v-for="opt in aiSceneOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
            </el-select>
            <el-button type="primary" :loading="aiLoading" @click="submitAiQuery">发送</el-button>
            <el-button @click="clearAiResult">清空</el-button>
            <span class="ai-tip">按 Ctrl + Enter 发送</span>
          </div>
        </div>

        <div class="ai-raw-block">
          <div class="ai-raw-title">原始返回数据</div>
          <pre class="ai-raw-json">{{ aiRawText }}</pre>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, computed, watch, nextTick } from 'vue'
import { productAPI, categoryAPI, cartAPI, aiAPI } from '@/api'
import PaginationBar from '@/components/PaginationBar.vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useCustomerStore } from '@/stores/customer'
import { ShoppingCart, User, ArrowDown, Grid, Search, Plus, View, Shop, ChatDotRound } from '@element-plus/icons-vue'

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
const loading = ref(false)
const currentLanguage = ref('chinese_simplified')
let translateReady = false

// AI 助手
const aiPanelVisible = ref(false)
const aiInput = ref('')
const aiLoading = ref(false)
const aiReply = ref('')
const aiItems = ref([])
const aiMessages = ref([])
const aiChatBodyRef = ref(null)
const aiRawResponse = ref(null)
const aiPage = ref(1)
const aiSize = ref(10)
const aiDrawerSize = ref('460px')
const aiScene = ref('recommend')
const aiSceneOptions = [
  { label: '导购推荐', value: 'recommend' },
  { label: '对话问答', value: 'chat' },
  { label: '条件提取', value: 'extract_query' }
]
const aiInputPlaceholder = computed(() => {
  if (aiScene.value === 'chat') return '例如：预算 3000，主要办公用，帮我选几款'
  if (aiScene.value === 'extract_query') return '例如：帮我筛选 1000-2000 的二手手机，按价格升序'
  return '例如：我想买一台 5000 左右的游戏本'
})
const aiRawText = computed(() => {
  if (!aiRawResponse.value) return '暂无数据'
  try {
    return JSON.stringify(aiRawResponse.value, null, 2)
  } catch {
    return String(aiRawResponse.value)
  }
})

// 轮播图数据 - 从已上架商品中获取
const bannerItems = ref([])

// 搜索相关
const searchSuggestions = ref([])
const showSuggestions = ref(false)
const hotSearchTags = ref(['光盘', '西部风格', '新品', '优惠', '限量版', '收藏'])

// 热门游戏推荐数据
const featuredGames = ref([
  {
    id: 1,
    name: 'GTA 6',
    description: 'Rockstar Games最新力作，体验狂野西部的开放世界冒险',
    image: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=Grand%20Theft%20Auto%206%20GTA%206%20game%20cover%20art%20wild%20west%20style%20high%20quality&image_size=landscape_16_9'
  },
  {
    id: 2,
    name: '艾尔登法环',
    description: 'FromSoftware开发的开放世界动作角色扮演游戏',
    image: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=Elden%20Ring%20game%20cover%20art%20fantasy%20dark%20souls%20high%20quality&image_size=landscape_16_9'
  },
  {
    id: 3,
    name: '塞尔达传说：王国之泪',
    description: '任天堂开发的开放世界动作冒险游戏',
    image: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=The%20Legend%20of%20Zelda%20Tears%20of%20the%20Kingdom%20game%20cover%20art%20fantasy%20adventure%20high%20quality&image_size=landscape_16_9'
  },
  {
    id: 4,
    name: '赛博朋克 2077',
    description: 'CD Projekt Red开发的开放世界角色扮演游戏',
    image: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=Cyberpunk%202077%20game%20cover%20art%20futuristic%20sci-fi%20high%20quality&image_size=landscape_16_9'
  },
  {
    id: 5,
    name: '星空',
    description: 'Bethesda开发的开放世界太空角色扮演游戏',
    image: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=Starfield%20game%20cover%20art%20space%20sci-fi%20futuristic%20high%20quality&image_size=landscape_16_9'
  },
  {
    id: 6,
    name: '战神 诸神黄昏',
    description: 'Santa Monica Studio开发的动作冒险游戏',
    image: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=God%20of%20War%20Ragnarok%20game%20cover%20art%20norse%20mythology%20high%20quality&image_size=landscape_16_9'
  }
])

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

const isCustomerLogged = computed(() => customerStore.isLoggedIn || !!customerStore.customerId)

const getCurrentCustomerId = () => {
  const cid = customerStore.customerId
  if (cid === null || cid === undefined) return null
  const normalized = String(cid).trim()
  return normalized ? normalized : null
}

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

const ensureTranslateScriptLoaded = () => {
  if (window.translate) return Promise.resolve()

  return new Promise((resolve, reject) => {
    const scriptId = 'translate-js-cdn'
    const existing = document.getElementById(scriptId)
    if (existing) {
      existing.addEventListener('load', resolve, { once: true })
      existing.addEventListener('error', reject, { once: true })
      return
    }

    const script = document.createElement('script')
    script.id = scriptId
    script.src = 'https://cdn.staticfile.net/translate.js/3.18.66/translate.js'
    script.async = true
    script.onload = resolve
    script.onerror = reject
    document.head.appendChild(script)
  })
}

const initTranslate = () => {
  if (!window.translate || translateReady) return

  window.translate.language.setLocal('chinese_simplified')
  window.translate.service.use('client.edge')
  window.translate.listener.start()
  window.translate.execute()
  translateReady = true
}

const toggleLanguage = () => {
  if (!window.translate || !translateReady) {
    ElMessage.warning('翻译组件加载中，请稍后重试')
    return
  }

  const target = currentLanguage.value === 'chinese_simplified' ? 'english' : 'chinese_simplified'

  if (typeof window.translate.changeLanguage === 'function') {
    window.translate.changeLanguage(target)
  } else if (typeof window.translate.to === 'function') {
    window.translate.to(target)
  } else {
    ElMessage.warning('当前翻译组件不支持语言切换')
    return
  }

  currentLanguage.value = target
}

// 跳转到商品详情页
const goToProductDetail = (productId) => {
  if (productId) {
    router.push(`/product/${productId}`).catch(() => {})
  }
}

// 处理轮播图按钮点击
const handleBannerClick = (item) => {
  // 根据轮播图项的ID执行不同的操作
  switch (item.id) {
    case 1:
      // 西部风格 - 跳转到相关分类
      router.push('/').catch(() => {})
      break
    case 2:
      // 新品上市 - 跳转到全部商品
      router.push('/').catch(() => {})
      break
    case 3:
      // 限时优惠 - 跳转到全部商品
      router.push('/').catch(() => {})
      break
    case 4:
      // 会员专享 - 跳转到登录页
      router.push('/login').catch(() => {})
      break
    default:
      router.push('/').catch(() => {})
  }
}

// 快速查看商品
const quickViewProduct = (product) => {
  // 这里可以实现快速查看功能，比如弹出一个模态框显示商品详情
  // 暂时先跳转到商品详情页
  router.push(`/product/${product.product_id}`).catch(() => {})
}

// 处理搜索建议选择
const handleSuggestionSelect = (suggestion) => {
  q.value = suggestion
  showSuggestions.value = false
  handleSearch()
}

// 选择搜索建议
const selectSuggestion = (suggestion) => {
  q.value = suggestion
  showSuggestions.value = false
  handleSearch()
}

// 选择热门标签
const selectHotTag = (tag) => {
  q.value = tag
  handleSearch()
}

// 监听搜索输入变化
watch(q, (newValue) => {
  if (newValue) {
    // 模拟搜索建议
    searchSuggestions.value = hotSearchTags.value.filter(tag => 
      tag.includes(newValue)
    )
    showSuggestions.value = true
  } else {
    searchSuggestions.value = []
    showSuggestions.value = false
  }
})

const LOGOUT_TOAST_KEY = 'post_reload_toast'
const ANON_AI_USER_ID_KEY = 'anon_ai_user_id'

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
  const customerId = getCurrentCustomerId()
  if (!isCustomerLogged.value || !customerId) {
    ElMessage.warning('请先登录')
    goBuyerLogin()
    return
  }
  
  try {
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

const mediaBaseUrl = (import.meta.env.VITE_MEDIA_BASE_URL || '').replace(/\/$/, '')
const normalizeMediaUrl = (rawUrl) => {
  const url = String(rawUrl || '').trim()
  if (!url) return ''

  // Relative media path: /media/xxx -> prefer configured media base, fallback current origin.
  if (url.startsWith('/media/')) {
    if (mediaBaseUrl) return `${mediaBaseUrl}${url}`
    return `${window.location.origin}${url}`
  }

  // Absolute URL: keep as-is unless VITE_MEDIA_BASE_URL is provided, then rewrite only /media host.
  if (/^https?:\/\//i.test(url)) {
    if (!mediaBaseUrl) return url
    return url.replace(/https?:\/\/[^/]+\/media/i, `${mediaBaseUrl}/media`)
  }

  return url
}

const normalizeProductItem = (item) => {
  const copy = { ...(item || {}) }
  let img = ''
  if (Array.isArray(copy.images) && copy.images.length) {
    const first = copy.images[0]
    if (typeof first === 'string') {
      img = normalizeMediaUrl(first)
    } else if (first && (first.image_url || first.url)) {
      img = normalizeMediaUrl(first.image_url || first.url)
    }
  } else if (copy.image_url) {
    img = normalizeMediaUrl(copy.image_url)
  } else if (copy.images && typeof copy.images === 'string') {
    img = normalizeMediaUrl(copy.images)
  }
  copy.image_url = img || ''
  copy.short_desc = copy.short_desc ?? ''
  copy.product_desc = copy.product_desc ?? ''
  copy.price = copy.price ?? 0
  copy.stock_quantity = copy.stock_quantity ?? 0
  return copy
}

const getCardSummary = (product) => {
  if (!product) return ''
  const shortDesc = stripHtml(product.short_desc)
  if (shortDesc) return shortDesc
  return stripHtml(product.product_desc)
}

const clearAiResult = () => {
  aiInput.value = ''
  aiReply.value = ''
  aiItems.value = []
  aiMessages.value = []
  aiRawResponse.value = null
}

const buildExtractQueryText = (payload) => {
  const query = payload?.query || {}
  const q = query?.q || ''
  const min = query?.minPrice ?? query?.min_price
  const max = query?.maxPrice ?? query?.max_price
  const status = query?.status || 'online'
  const pageNo = query?.page ?? aiPage.value
  const pageSize = query?.size ?? aiSize.value
  const range = (min != null || max != null) ? `，价格区间：${min ?? '-'} ~ ${max ?? '-'}` : ''
  return `已提取查询条件：关键词「${q || '未识别'}」、状态：${status}${range}，分页：${pageNo}/${pageSize}`
}

const scrollAiToBottom = async () => {
  await nextTick()
  const el = aiChatBodyRef.value
  if (!el) return
  el.scrollTop = el.scrollHeight
}

const submitAiQuery = async () => {
  const text = (aiInput.value || '').trim()
  if (!text) {
    ElMessage.warning('请输入问题后再发送')
    return
  }

  aiMessages.value.push({ role: 'user', text, items: [], time: Date.now() })
  aiLoading.value = true
  aiInput.value = ''
  await scrollAiToBottom()

  try {
    const scene = aiScene.value || 'recommend'
    const response = await aiAPI.recommend({
      text,
      page: aiPage.value,
      size: aiSize.value,
      userId: getAiUserId(),
      scene,
      action: scene
    })
    aiRawResponse.value = response?.data ?? null

    const payload = extractData(response) || {}
    const aiDesc = payload?.ai_description || payload?.aiDescription || ''
    const normalizedItems = Array.isArray(payload?.items) ? payload.items.map(normalizeProductItem) : []

    if (scene === 'extract_query') {
      aiReply.value = aiDesc || buildExtractQueryText(payload)
      aiItems.value = []
    } else if (scene === 'chat') {
      aiReply.value = aiDesc || '已收到你的问题，我再帮你细化一下需求。'
      aiItems.value = []
    } else {
      aiReply.value = aiDesc || '未获取到 AI 回复'
      aiItems.value = normalizedItems
    }

    aiMessages.value.push({
      role: 'assistant',
      text: aiReply.value,
      items: aiItems.value,
      time: Date.now()
    })
    await scrollAiToBottom()
  } catch (error) {
    console.error('AI 对话请求失败:', error)
    aiReply.value = '请求失败，请稍后重试'
    aiItems.value = []
    aiMessages.value.push({ role: 'assistant', text: aiReply.value, items: [], time: Date.now() })
    ElMessage.error('AI 对话请求失败')
    await scrollAiToBottom()
  } finally {
    aiLoading.value = false
  }
}

const getAiUserId = () => {
  const cid = getCurrentCustomerId()
  if (cid) {
    return `customer_${cid}`
  }

  let anonId = localStorage.getItem(ANON_AI_USER_ID_KEY)
  if (!anonId) {
    const randomPart = `${Date.now()}_${Math.random().toString(36).slice(2, 10)}`
    anonId = `anon_${randomPart}`
    localStorage.setItem(ANON_AI_USER_ID_KEY, anonId)
  }
  return anonId
}

const updateAiDrawerSize = () => {
  if (typeof window === 'undefined') return
  aiDrawerSize.value = window.innerWidth <= 768 ? '92%' : '460px'
}

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
    items = items.map(normalizeProductItem)

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
  // 使用热门游戏的图片
  bannerItems.value = [
    {
      id: 1,
      image: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=Grand%20Theft%20Auto%206%20GTA%206%20game%20cover%20art%20wild%20west%20style%20high%20quality&image_size=landscape_16_9',
      title: 'GTA 6',
      subtitle: '体验狂野西部的魅力',
      buttonText: '探索更多'
    },
    {
      id: 2,
      image: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=Elden%20Ring%20game%20cover%20art%20fantasy%20dark%20souls%20high%20quality&image_size=landscape_16_9',
      title: '艾尔登法环',
      subtitle: '最新款商品等你来选',
      buttonText: '查看详情'
    },
    {
      id: 3,
      image: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=The%20Legend%20of%20Zelda%20Tears%20of%20the%20Kingdom%20game%20cover%20art%20fantasy%20adventure%20high%20quality&image_size=landscape_16_9',
      title: '塞尔达传说',
      subtitle: '全场商品低至5折',
      buttonText: '立即抢购'
    },
    {
      id: 4,
      image: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=Cyberpunk%202077%20game%20cover%20art%20futuristic%20sci-fi%20high%20quality&image_size=landscape_16_9',
      title: '赛博朋克 2077',
      subtitle: '会员购物享额外折扣',
      buttonText: '了解会员'
    }
  ]
}

onMounted(async () => {
  try {
    await ensureTranslateScriptLoaded()
    initTranslate()
  } catch (e) {
    console.warn('translate.js 加载失败：', e)
  }

  fetchCategories()
  fetchProducts()
  fetchBannerProducts()
  updateAiDrawerSize()
  window.addEventListener('resize', updateAiDrawerSize)
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

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateAiDrawerSize)
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
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  padding-bottom: 80px;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  color: #1e293b;
  position: relative;
}

.home-container::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: 
    radial-gradient(circle at 20% 30%, rgba(59, 130, 246, 0.1) 0%, transparent 50%),
    radial-gradient(circle at 80% 70%, rgba(139, 92, 246, 0.1) 0%, transparent 50%);
  pointer-events: none;
  z-index: 0;
}

/* 头部导航 */
.header {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(224, 230, 237, 0.8);
  position: sticky;
  top: 0;
  z-index: 100;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
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
  font-size: 32px;
  font-weight: 700;
  letter-spacing: -0.02em;
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.nav-links {
  display: flex;
  gap: 32px;
  align-items: center;
}

.nav-link {
  text-decoration: none;
  color: #64748b;
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

.btn-language {
  color: #64748b;
  font-weight: 500;
  border: 1px solid #e2e8f0;
  border-radius: 999px;
  padding: 6px 14px;
  background: #ffffff;
}

.btn-language:hover {
  color: #3b82f6;
  border-color: #93c5fd;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #64748b;
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
  color: #3b82f6;
  border-color: #93c5fd;
  background: white;
}

.login-btn:hover {
  border-color: #3b82f6;
  color: #3b82f6;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.2);
}

.seller-btn {
  border-radius: 8px;
  padding: 8px 16px;
  font-weight: 500;
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  border-color: transparent;
  transition: all 0.2s ease;
  color: white;
  box-shadow: 0 4px 16px rgba(59, 130, 246, 0.3);
}

.seller-btn:hover {
  background: linear-gradient(135deg, #2563eb, #7c3aed);
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.4);
}

.login-dropdown-btn {
  border-radius: 8px;
  padding: 8px 16px;
  font-weight: 500;
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  border-color: transparent;
  transition: all 0.2s ease;
  color: white;
  box-shadow: 0 4px 16px rgba(59, 130, 246, 0.3);
  display: flex;
  align-items: center;
  gap: 6px;
}

.login-dropdown-btn:hover {
  background: linear-gradient(135deg, #2563eb, #7c3aed);
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.4);
}

.user-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  border-radius: 8px;
  padding: 8px 16px;
  transition: all 0.2s ease;
  color: #1e293b;
  border-color: #e2e8f0;
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.user-btn:hover {
  border-color: #3b82f6;
  color: #3b82f6;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.2);
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
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
}

.banner-item {
  position: relative;
  height: 450px;
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: center;
  padding: 0 64px;
  transition: transform 0.5s ease;
}

.banner-item:hover {
  transform: scale(1.02);
}

.banner-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(to right, rgba(0, 0, 0, 0.7), rgba(0, 0, 0, 0.3));
  z-index: 1;
}

.banner-content {
  max-width: 600px;
  color: white;
  text-shadow: 0 4px 16px rgba(0, 0, 0, 0.4);
  z-index: 2;
  position: relative;
  animation: fadeInUp 1s ease-out;
}

.banner-title {
  font-size: 56px;
  font-weight: 700;
  margin-bottom: 20px;
  line-height: 1.1;
  animation: slideInLeft 0.8s ease-out;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  letter-spacing: -0.02em;
}

.banner-subtitle {
  font-size: 24px;
  margin-bottom: 36px;
  opacity: 0.95;
  animation: slideInLeft 0.8s ease-out 0.2s both;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  font-weight: 400;
}

.banner-btn {
  border-radius: 12px;
  padding: 14px 28px;
  font-size: 16px;
  font-weight: 600;
  background: rgba(255, 255, 255, 0.95);
  color: #1e293b;
  border: none;
  transition: all 0.3s ease;
  animation: slideInLeft 0.8s ease-out 0.4s both;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
}

.banner-btn:hover {
  background: white;
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);
}

/* 轮播图动画 */
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes slideInLeft {
  from {
    opacity: 0;
    transform: translateX(-30px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

/* ��要内容 */
.main-content {
  max-width: 1440px;
  margin: 0 auto;
  padding: 0 32px 40px;
}

/* 搜索区 */
.search-section {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 28px;
  padding: 32px;
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.1);
  margin-bottom: 40px;
  position: relative;
  backdrop-filter: blur(12px);
  border: 1px solid rgba(224, 230, 237, 0.5);
  overflow: hidden;
}

.search-section::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(59, 130, 246, 0.1) 0%, transparent 70%);
  animation: float 6s ease-in-out infinite;
  pointer-events: none;
  z-index: 0;
}

@keyframes float {
  0% {
    transform: translateY(0) rotate(0deg);
  }
  50% {
    transform: translateY(-10px) rotate(5deg);
  }
  100% {
    transform: translateY(0) rotate(0deg);
  }
}

.search-box {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 18px;
  margin-bottom: 20px;
}

.search-left {
  flex: 0 0 auto;
}

.search-middle {
  flex: 1;
  position: relative;
}

.search-right {
  flex: 0 0 auto;
}

.category-button {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  border-radius: 16px;
  background: linear-gradient(135deg, #f8fafc, #e2e8f0);
  color: #334155;
  border: 1px solid #e2e8f0;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  position: relative;
  z-index: 1;
}

.category-button:hover {
  background: linear-gradient(135deg, #f1f5f9, #cbd5e1);
  border-color: #cbd5e1;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.category-button .caret {
  transition: transform 0.3s ease;
}

.category-button .caret.open {
  transform: rotate(180deg);
}

.search-input {
  width: 100%;
  border-radius: 16px;
  border: 2px solid #e2e8f0;
  background: white;
  color: #1e293b;
  transition: all 0.3s ease;
  font-size: 16px;
  padding: 12px 20px;
  position: relative;
  z-index: 1;
}

.search-input:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.15);
}

.clear-btn {
  transition: all 0.2s ease;
}

.clear-btn:hover {
  color: #ef4444;
}

.search-btn {
  border-radius: 16px;
  padding: 12px 28px;
  font-weight: 600;
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  border-color: transparent;
  transition: all 0.3s ease;
  box-shadow: 0 4px 16px rgba(59, 130, 246, 0.3);
}

.search-btn:hover {
  background: linear-gradient(135deg, #2563eb, #7c3aed);
  transform: translateY(-2px);
  box-shadow: 0 6px 24px rgba(59, 130, 246, 0.4);
}

/* 搜索建议 */
.search-suggestions {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: white;
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
  margin-top: 8px;
  z-index: 10;
  max-height: 300px;
  overflow-y: auto;
  border: 1px solid #e2e8f0;
  position: relative;
  z-index: 1;
}

.suggestion-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  cursor: pointer;
  transition: all 0.2s ease;
  border-bottom: 1px solid #f1f5f9;
  color: #1e293b;
}

.suggestion-item:last-child {
  border-bottom: none;
}

.suggestion-item:hover {
  background: #f8fafc;
  padding-left: 20px;
}

.suggestion-item el-icon {
  color: #64748b;
}

/* 热门搜索 */
.hot-search {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  padding-top: 16px;
  border-top: 1px solid #f1f5f9;
  position: relative;
  z-index: 1;
}

.hot-label {
  font-size: 14px;
  font-weight: 600;
  color: #64748b;
  white-space: nowrap;
}

.hot-tag {
  cursor: pointer;
  transition: all 0.3s ease;
  border-radius: 16px;
  padding: 4px 12px;
  font-size: 13px;
  background: #f8fafc;
  color: #334155;
  border: 1px solid #e2e8f0;
}

.hot-tag:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.2);
  background: #3b82f6;
  border-color: #3b82f6;
  color: white;
}

/* 分类标签 */
.category-tabs {
  display: flex;
  gap: 16px;
  margin-bottom: 28px;
  flex-wrap: wrap;
  padding: 16px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 24px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(224, 230, 237, 0.5);
  position: relative;
  z-index: 1;
}

.tab-btn {
  border-radius: 16px;
  padding: 10px 20px;
  font-weight: 600;
  transition: all 0.3s ease;
  border: 2px solid transparent;
  position: relative;
  overflow: hidden;
  background: #f8fafc;
  color: #334155;
  border-color: #e2e8f0;
}

.tab-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.2);
  background: #f1f5f9;
  border-color: #cbd5e1;
}

.tab-btn.active {
  background: linear-gradient(135deg, #2563eb, #7c3aed);
  border-color: transparent;
  color: white;
  box-shadow: 0 8px 24px rgba(37, 99, 235, 0.4);
}

.tab-btn.active:hover {
  background: linear-gradient(135deg, #1d4ed8, #6d28d9);
  box-shadow: 0 12px 32px rgba(37, 99, 235, 0.5);
}

/* 区块标题 */
.section-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  position: relative;
  z-index: 1;
}

.section-title h2 {
  font-size: 32px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  letter-spacing: -0.02em;
  background: linear-gradient(135deg, #1e293b, #475569);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
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
  gap: 28px;
  margin-bottom: 40px;
}

.grid-item {
  display: flex;
  align-items: stretch;
}

.product-card {
  width: 100%;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 24px;
  overflow: hidden;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
  display: flex;
  flex-direction: column;
  position: relative;
  border: 1px solid rgba(224, 230, 237, 0.5);
  backdrop-filter: blur(12px);
  position: relative;
  z-index: 1;
}

.product-card:hover {
  transform: translateY(-10px);
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.15);
  border-color: rgba(59, 130, 246, 0.3);
}

.product-image {
  position: relative;
  height: 220px;
  background: #f8fafc;
  overflow: hidden;
  cursor: pointer;
  border-radius: 24px 24px 0 0;
}

.product-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s ease;
}

.product-card:hover .product-img {
  transform: scale(1.1);
}

.product-badge {
  position: absolute;
  top: 16px;
  right: 16px;
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  color: white;
  padding: 6px 14px;
  border-radius: 16px;
  font-size: 12px;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
  z-index: 2;
}

.product-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: all 0.3s ease;
  z-index: 3;
}

.product-card:hover .product-overlay {
  opacity: 1;
}

.quick-view-btn {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: white;
  color: #3b82f6;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);
  transition: all 0.3s ease;
}

.quick-view-btn:hover {
  transform: scale(1.1);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.4);
}

.product-info {
  padding: 24px;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.product-name {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  margin: 0;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  transition: color 0.2s ease;
  cursor: pointer;
}

.product-name:hover {
  color: #3b82f6;
}

.price-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.price {
  font-size: 24px;
  font-weight: 700;
  color: #ef4444;
  transition: transform 0.2s ease;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.product-card:hover .price {
  transform: scale(1.05);
}

.stock {
  font-size: 14px;
  color: #64748b;
  transition: all 0.2s ease;
}

.low-stock {
  color: #ef4444;
  font-weight: 500;
}

.description {
  flex: 1;
}

.desc-text {
  font-size: 14px;
  color: #64748b;
  line-height: 1.5;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.action-section {
  display: flex;
  gap: 12px;
  margin-top: 20px;
}

.add-cart-btn {
  flex: 1;
  border-radius: 10px;
  font-weight: 600;
  transition: all 0.3s ease;
  padding: 10px 0;
  background: #f8fafc;
  border-color: #e2e8f0;
  color: #334155;
}

.add-cart-btn:hover {
  border-color: #3b82f6;
  color: #3b82f6;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.2);
}

.detail-btn {
  flex: 1;
  border-radius: 10px;
  font-weight: 600;
  transition: all 0.3s ease;
  padding: 10px 0;
  background: #f8fafc;
  border-color: #e2e8f0;
  color: #334155;
}

.detail-btn:hover {
  border-color: #3b82f6;
  color: #3b82f6;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.2);
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
  background: rgba(255, 255, 255, 0.9);
  border-radius: 24px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(224, 230, 237, 0.5);
  position: relative;
  z-index: 1;
}

/* 热门游戏推荐 */
.featured-games-section {
  margin-bottom: 40px;
}

.featured-games-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 24px;
  margin-top: 24px;
}

.featured-game-item {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 24px;
  overflow: hidden;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
  display: flex;
  flex-direction: column;
  border: 1px solid rgba(224, 230, 237, 0.5);
  backdrop-filter: blur(12px);
  position: relative;
  z-index: 1;
}

.featured-game-item:hover {
  transform: translateY(-8px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.15);
  border-color: rgba(59, 130, 246, 0.3);
}

.featured-game-image {
  position: relative;
  height: 180px;
  overflow: hidden;
  cursor: pointer;
}

.featured-game-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s ease;
}

.featured-game-item:hover .featured-game-img {
  transform: scale(1.1);
}

.featured-game-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: all 0.3s ease;
  z-index: 1;
}

.featured-game-item:hover .featured-game-overlay {
  opacity: 1;
}

.featured-game-btn {
  border-radius: 8px;
  font-weight: 600;
  transition: all 0.3s ease;
  color: white;
  border-color: white;
}

.featured-game-btn:hover {
  background: white;
  color: #3b82f6;
  border-color: white;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(255, 255, 255, 0.3);
}

.featured-game-info {
  padding: 20px;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.featured-game-name {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  margin: 0;
  line-height: 1.4;
  transition: color 0.2s ease;
  cursor: pointer;
}

.featured-game-name:hover {
  color: #3b82f6;
}

.featured-game-desc {
  font-size: 14px;
  color: #64748b;
  line-height: 1.5;
  margin: 0;
  flex: 1;
}

/* 响应式调整 */
@media (max-width: 1024px) {
  .featured-games-grid {
    grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
    gap: 20px;
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

  .ai-fab {
    right: 16px;
    bottom: 16px;
    width: 50px;
    height: 50px;
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

/* AI 助手 */
.ai-fab {
  position: fixed;
  right: 24px;
  bottom: 24px;
  width: 56px;
  height: 56px;
  border: none;
  box-shadow: 0 10px 24px rgba(37, 99, 235, 0.35);
  z-index: 1200;
}

:deep(.ai-drawer .el-drawer__body) {
  padding: 0;
}

.ai-panel {
  height: 100%;
  display: grid;
  grid-template-rows: 1fr auto auto;
  gap: 10px;
  padding: 12px;
}

.ai-chat-body {
  overflow-y: auto;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 12px;
}

.ai-empty-chat {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.ai-msg-row {
  display: flex;
  margin-bottom: 10px;
}

.ai-msg-row.is-user {
  justify-content: flex-end;
}

.ai-msg-row.is-assistant {
  justify-content: flex-start;
}

.ai-bubble {
  max-width: 88%;
  border-radius: 12px;
  padding: 10px 12px;
  line-height: 1.55;
  font-size: 14px;
}

.ai-msg-row.is-user .ai-bubble {
  background: #2563eb;
  color: #fff;
}

.ai-msg-row.is-assistant .ai-bubble {
  background: #fff;
  color: #1f2937;
  border: 1px solid #e5e7eb;
}

.ai-msg-items {
  margin-top: 8px;
  display: grid;
  gap: 6px;
}

.ai-mini-item {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 6px 8px;
}

.ai-mini-item .name {
  flex: 1;
  font-weight: 500;
}

.ai-mini-item .price {
  color: #dc2626;
  font-weight: 600;
}

.ai-input-box {
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #fff;
  padding: 10px;
}

.ai-actions-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
}

.ai-scene-select {
  width: 120px;
}

.ai-tip {
  color: #64748b;
  font-size: 12px;
  margin-left: auto;
}

.ai-raw-block {
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #0f172a;
  color: #e2e8f0;
  padding: 10px;
}

.ai-raw-title {
  font-size: 13px;
  margin-bottom: 6px;
}

.ai-raw-json {
  margin: 0;
  max-height: 140px;
  overflow: auto;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
