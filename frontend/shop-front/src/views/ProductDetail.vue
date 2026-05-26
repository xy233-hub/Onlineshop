<template>
  <div class="product-detail">
    <el-container>
      <!-- 轻盈玻璃态头部 -->
      <el-header class="header">
        <div class="header-inner">
          <el-breadcrumb separator="/" class="breadcrumb-modern">
            <el-breadcrumb-item @click="$router.push('/')">首页</el-breadcrumb-item>
            <el-breadcrumb-item>商品详情</el-breadcrumb-item>
            <el-breadcrumb-item>{{ product?.product_name || '加载中' }}</el-breadcrumb-item>
          </el-breadcrumb>

          <div class="header-actions">
            <el-button @click="toggleLanguage" class="btn-language" size="default">
              {{ currentLanguage === 'chinese_simplified' ? 'English' : '中文' }}
            </el-button>
            <el-button @click="$router.push('/')" class="btn-return" :icon="ArrowLeft" size="default">
              返回
            </el-button>
          </div>
        </div>
      </el-header>

      <el-main class="main-content">
        <div v-if="product" class="detail-wrap">
          <el-row :gutter="24" class="main-row">
            <!-- 左侧媒体区 -->
            <el-col :xs="24" :sm="24" :md="16" :lg="17">
              <div class="visual-area">
                <div class="image-panel">
                  <div class="main-image" @click="openImageDialog(currentImageIndex)">
                    <img v-if="imageList.length" :src="imageList[currentImageIndex]" alt="主图" />
                    <div v-else class="no-image">✨ 暂无图片</div>
                    <div class="zoom-hint">
                      <el-icon><ZoomIn /></el-icon> 点击放大
                    </div>
                  </div>

                  <div class="thumbs" v-if="imageList.length">
                    <div
                        v-for="(src, idx) in imageList"
                        :key="idx"
                        class="thumb"
                        :class="{ active: idx === currentImageIndex }"
                        @click="showImage(idx)"
                    >
                      <img :src="src" alt="缩略图" />
                    </div>
                  </div>

                  <el-tabs class="desc-tabs-modern" v-model="activeTab">
                    <el-tab-pane label="商品描述" name="desc">
                      <div class="description" v-html="safeDescription"></div>
                    </el-tab-pane>
                    <el-tab-pane label="媒体资源" name="media" v-if="hasMedia">
                      <div class="media-section">
                        <div class="media-grid">
                          <div
                              class="media-card"
                              v-for="(m, idx) in product.media_resources || []"
                              :key="m.media_id || m.temp_key || idx"
                          >
                            <template v-if="isImage(m)">
                              <div class="media-preview">
                                <img :src="m.media_url || m.image_url || m.url" alt="媒体图片" />
                              </div>
                              <div class="media-footer">
                                <span class="name">{{ m.file_name || '图片' }}</span>
                                <span class="meta">{{ m.mime_type || m.media_type || 'image' }}</span>
                              </div>
                            </template>

                            <template v-else-if="isVideo(m)">
                              <div class="media-preview video-preview">
                                <video :src="m.media_url" controls />
                              </div>
                              <div class="media-footer">
                                <span class="name">{{ m.file_name || '视频' }}</span>
                                <span class="meta">{{ m.mime_type || 'video' }}</span>
                              </div>
                            </template>

                            <template v-else-if="isAudio(m)">
                              <div class="media-preview audio-preview">
                                <div class="audio-placeholder">
                                  <el-icon :size="24"><Headset /></el-icon>
                                  <span class="audio-filename">{{ m.file_name || '音频文件' }}</span>
                                </div>
                                <audio :src="m.media_url" controls />
                              </div>
                              <div class="media-footer">
                                <span class="name">{{ m.file_name || '音频' }}</span>
                                <span class="meta">{{ m.mime_type || 'audio' }}</span>
                              </div>
                            </template>

                            <template v-else>
                              <div class="media-preview fallback">
                                <el-icon><Document /></el-icon>
                                <span>无法预览</span>
                              </div>
                              <div class="media-footer">
                                <span class="name">{{ m.file_name || '其他文件' }}</span>
                              </div>
                            </template>
                          </div>
                        </div>
                      </div>
                    </el-tab-pane>
                    <el-tab-pane label="价格趋势" name="price">
                      <div class="price-trend-section">
                        <div v-if="priceHistoryLoading" class="loading-state">
                          <el-skeleton :rows="4" animated />
                        </div>
                        <div v-else-if="priceHistory.length === 0" class="empty-state">
                          <el-empty description="暂无价格变动记录" />
                        </div>
                        <div v-else>
                          <div class="price-chart-container">
                            <VChart :option="priceChartOption" style="width: 100%; height: 450px" autoresize />
                          </div>
                          <div class="price-history-list">
                            <h3 class="history-title">价格变动记录</h3>
                            <el-table :data="priceHistory" style="width: 100%" :fit="true">
                              <el-table-column prop="createdAt" label="变动时间" min-width="160">
                                <template #default="scope">
                                  <span style="font-size: 13px;">{{ scope.row.createdAt }}</span>
                                </template>
                              </el-table-column>
                              <el-table-column prop="oldPrice" label="旧价格" min-width="100" align="center">
                                <template #default="scope">
                                  <span style="font-size: 14px; color: #64748b;">¥{{ scope.row.oldPrice.toFixed(2) }}</span>
                                </template>
                              </el-table-column>
                              <el-table-column prop="newPrice" label="新价格" min-width="100" align="center">
                                <template #default="scope">
                                  <span style="font-size: 15px; font-weight: 600; color: #3b82f6;">¥{{ scope.row.newPrice.toFixed(2) }}</span>
                                </template>
                              </el-table-column>
                              <el-table-column prop="changeType" label="变动类型" min-width="100" align="center">
                                <template #default="scope">
                                  <el-tag :type="scope.row.changeType === 'initial' ? 'info' : scope.row.changeType === 'current' ? 'success' : 'warning'" size="small">
                                    {{ scope.row.changeType === 'initial' ? '初始价格' : scope.row.changeType === 'current' ? '当前价格' : '手动修改' }}
                                  </el-tag>
                                </template>
                              </el-table-column>
                              <el-table-column prop="reason" label="变动原因" min-width="150">
                                <template #default="scope">
                                  <span style="font-size: 13px; color: #94a3b8;">{{ scope.row.reason || '无' }}</span>
                                </template>
                              </el-table-column>
                            </el-table>
                          </div>
                        </div>
                      </div>
                    </el-tab-pane>
                  </el-tabs>
                </div>
              </div>
            </el-col>

            <!-- 右侧购买卡 -->
            <el-col :xs="24" :sm="24" :md="8" :lg="7">
              <div class="purchase-card">
                <div class="card-inner">
                  <h2 class="product-title">{{ product.product_name }}</h2>

                  <div class="price-section">
                    <div class="price-wrapper">
                      <span class="price-currency">¥</span>
                      <span class="price-value">{{ currentDisplayPrice != null ? currentDisplayPrice.toFixed(2) : '--' }}</span>
                      <span v-if="hasDiscountPrice" class="price-original">¥{{ originalDisplayPrice.toFixed(2) }}</span>
                      <span v-if="hasDiscountPrice" class="price-save">省 ¥{{ discountAmount.toFixed(2) }}</span>
                    </div>
                    <el-tag class="status-tag" :type="getStatusType(product.product_status)" effect="light" round>
                      {{ getStatusText(product.product_status) }}
                    </el-tag>
                  </div>

                  <div class="stock-modern">
                    <div class="stock-label">
                      <span>库存</span>
                      <span :class="{ 'stock-low': (product.stock_quantity ?? 0) <= 5 }">{{ product.stock_quantity ?? 0 }} 件</span>
                    </div>
                    <el-progress
                        :percentage="Math.min(100, ((product.stock_quantity ?? 0) / 100) * 100)"
                        :format="() => ''"
                        :stroke-width="6"
                        :color="(product.stock_quantity ?? 0) > 0 ? '#52c41a' : '#f56c6c'"
                        :show-text="false"
                        class="stock-progress"
                    />
                  </div>

                  <!-- 操作按钮组 -->
                  <div class="action-group">
                    <!-- 第一行：立即购买 + 收藏（带状态） -->
                    <div class="action-row">
                      <el-button
                          type="primary"
                          :disabled="!canBuy"
                          @click="showPurchaseDialog = true"
                          class="btn-buy"
                          size="large"
                      >
                        <el-icon><ShoppingCart /></el-icon> {{ getButtonText(product.product_status) }}
                      </el-button>

                      <el-button
                          type="default"
                          :disabled="!canFavorite"
                          @click="toggleFavorite"
                          class="btn-favorite"
                          :class="{ 'is-favorited': isFavorited }"
                          size="large"
                      >
                        <el-icon :class="{ 'star-filled': isFavorited }"><Star /></el-icon> 收藏
                      </el-button>
                    </div>

                    <!-- 第二行：数量选择 + 加入购物车 - 修复上下按钮位置 -->
                    <div class="action-row cart-row-fixed">
                      <div class="quantity-selector">
                        <span class="quantity-label">数量</span>
                        <el-input-number
                            v-model="quantity"
                            :min="1"
                            :max="product.stock_quantity || 999999"
                            size="default"
                            controls-position="right"
                            class="modern-number fixed-controls"
                        />
                      </div>
                      <el-button
                          type="warning"
                          :disabled="!canAddCart"
                          @click="addCart"
                          class="btn-cart"
                          size="large"
                          plain
                      >
                        <el-icon><Plus /></el-icon> 加入购物车
                      </el-button>
                    </div>

                    <!-- 第三行：联系卖家 -->
                    <el-button type="default" @click="$router.push({ path: '/seller', query: {} })" class="btn-contact" :icon="Message" plain block>
                      联系卖家
                    </el-button>

                    <!-- 第四行：AI助手 -->
                    <el-button type="success" @click="aiPanelVisible = true" class="btn-ai" :icon="ChatDotRound" plain block>
                      AI 智能助手
                    </el-button>
                  </div>

                  <div class="product-footer">
                    <el-divider>
                      <el-icon><Goods /></el-icon>
                    </el-divider>
                    <div class="footer-meta">
                      <span>类别：{{ product.category?.category_name || '-' }}</span>
                      <span>发布时间：{{ formatTime(product.created_at) }}</span>
                      <span>卖家：{{ sellerInfo?.username || product.seller_id || '-' }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </el-col>
          </el-row>
        </div>

        <div v-else-if="loading" class="loading-state">
          <el-skeleton :rows="6" animated class="smooth-skeleton" />
        </div>

        <div v-else class="error-state">
          <el-empty description="商品不存在或已被删除" :image-size="200">
            <el-button type="primary" @click="$router.push('/')" round>返回首页</el-button>
          </el-empty>
        </div>
      </el-main>
    </el-container>

    <!-- 图片弹窗 -->
    <el-dialog v-model="showImageDialog" width="80%" center top="5vh" :show-close="true" class="image-dialog">
      <div class="dialog-image-wrap">
        <el-image :src="imageList[currentImageIndex]" fit="contain" style="max-height:80vh; width:100%;" />
      </div>
    </el-dialog>

    <!-- 购买对话框 -->
    <PurchaseDialog
        v-model="showPurchaseDialog"
        :product="product"
        :promotion-info="purchasePromotionInfo"
        :promotion-loading="promotionLoading"
        @success="handlePurchaseSuccess"
    />

    <!-- AI 对话助手抽屉 -->
    <el-drawer
      v-model="aiPanelVisible"
      title="AI 对话助手"
      direction="rtl"
      size="460px"
      class="ai-drawer"
    >
      <div class="ai-panel">
        <div class="ai-sessions-bar">
          <div class="ai-sessions-header">
            <span class="ai-sessions-title">会话列表</span>
            <el-button type="primary" size="small" @click="createNewSession">新建对话</el-button>
          </div>
          <div class="ai-sessions-list">
            <div
              v-for="session in chatSessions"
              :key="session.id"
              class="ai-session-item"
              :class="{ active: currentSessionId === session.id }"
              @click="switchSession(session)"
            >
              <span class="session-name">{{ session.session_name || '新对话' }}</span>
              <el-button
                type="danger"
                size="small"
                link
                @click.stop="deleteSession(session.id)"
                class="delete-btn"
              >×</el-button>
            </div>
            <div v-if="!chatSessions.length" class="ai-sessions-empty">
              暂无会话
            </div>
          </div>
        </div>

        <div ref="aiChatBodyRef" class="ai-chat-body">
          <div v-if="!aiMessages.length" class="ai-empty-chat">
            <el-empty description="有什么想问的吗？" />
          </div>

          <div
            v-for="(msg, idx) in aiMessages"
            :key="`${msg.role}-${idx}-${msg.time}`"
            class="ai-msg-row"
            :class="msg.role === 'user' ? 'is-user' : 'is-assistant'"
          >
            <div class="ai-bubble">
              <div class="ai-msg-text">{{ msg.text }}</div>
            </div>
          </div>
        </div>

        <div class="ai-input-box">
          <el-input
            v-model="aiInput"
            type="textarea"
            :rows="3"
            resize="none"
            placeholder="例如：这个商品有什么优惠？"
            @keyup.enter.ctrl="submitAiQuery"
          />
          <div class="ai-actions-row">
            <el-button type="primary" :loading="aiLoading" @click="submitAiQuery">发送</el-button>
            <el-button @click="clearAiResult">清空</el-button>
            <span class="ai-tip">按 Ctrl + Enter 发送</span>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { productAPI, favoritesAPI, cartAPI, sellerAPI, promotionAPI, aiAPI, chatAPI, priceHistoryAPI } from '@/api'
import PurchaseDialog from '@/components/buyer/PurchaseDialog.vue'
import DOMPurify from 'dompurify'
import { marked } from 'marked'
import { ElMessage } from 'element-plus'
import { ArrowLeft, ZoomIn, ShoppingCart, Star, Plus, Message, Headset, Document, Goods, ChatDotRound } from '@element-plus/icons-vue'
import VChart from 'vue-echarts'
import { LineChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  GridComponent,
  DataZoomComponent,
  LegendComponent
} from 'echarts/components'
import { use } from 'echarts'

use([
  LineChart,
  TitleComponent,
  TooltipComponent,
  GridComponent,
  DataZoomComponent,
  LegendComponent
])

const route = useRoute()
const product = ref(null)
const sellerInfo = ref(null)
const loading = ref(true)
const showPurchaseDialog = ref(false)
const showImageDialog = ref(false)
const currentImageIndex = ref(0)
const submitLoading = ref(false)
const promotionLoading = ref(false)
const productPromotionInfo = ref(null)
const quantity = ref(1)
const activeTab = ref('desc')
const isFavorited = ref(false)
const productId = computed(() => route.params.id)
const currentLanguage = ref('chinese_simplified')

const priceHistory = ref([])
const priceHistoryLoading = ref(false)
const priceChartOption = ref({})

const aiPanelVisible = ref(false)
const aiInput = ref('')
const aiLoading = ref(false)
const aiMessages = ref([])
const aiChatBodyRef = ref(null)
const chatSessions = ref([])
const currentSessionId = ref(null)

const ANON_AI_USER_ID_KEY = 'anon_ai_user_id'

const getAiUserId = () => {
  const cid = localStorage.getItem('customer_id')
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

const loadChatSessions = async () => {
  try {
    const userId = getAiUserId()
    const response = await chatAPI.getUserSessions(userId)
    chatSessions.value = response?.data?.data || []
  } catch (e) {
    console.error('加载会话列表失败:', e)
  }
}

const createNewSession = async () => {
  try {
    const userId = getAiUserId()
    const response = await chatAPI.createSession(userId, '新对话')
    const newSession = response?.data?.data
    if (newSession) {
      chatSessions.value.unshift(newSession)
      await switchSession(newSession)
    }
  } catch (e) {
    console.error('创建会话失败:', e)
    ElMessage.error('创建会话失败')
  }
}

const switchSession = async (session) => {
  currentSessionId.value = session.id
  aiMessages.value = []

  try {
    const response = await chatAPI.getSessionMessages(session.id)
    const messages = response?.data?.data || []
    aiMessages.value = messages.map(msg => ({
      role: msg.role,
      text: msg.content,
      items: [],
      time: new Date(msg.created_at).getTime()
    }))
    await nextTick()
    await scrollAiToBottom()
  } catch (e) {
    console.error('加载会话消息失败:', e)
  }
}

const deleteSession = async (sessionId) => {
  try {
    const userId = getAiUserId()
    await chatAPI.deleteSession(sessionId, userId)
    chatSessions.value = chatSessions.value.filter(s => s.id !== sessionId)
    if (currentSessionId.value === sessionId) {
      if (chatSessions.value.length > 0) {
        await switchSession(chatSessions.value[0])
      } else {
        currentSessionId.value = null
        aiMessages.value = []
      }
    }
    ElMessage.success('会话已删除')
  } catch (e) {
    console.error('删除会话失败:', e)
    ElMessage.error('删除会话失败')
  }
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

  if (!currentSessionId.value) {
    try {
      const userId = getAiUserId()
      const response = await chatAPI.createSession(userId, text.slice(0, 20))
      const newSession = response?.data?.data
      if (newSession) {
        chatSessions.value.unshift(newSession)
        currentSessionId.value = newSession.id
      }
    } catch (e) {
      console.error('自动创建会话失败:', e)
    }
  }

  aiMessages.value.push({ role: 'user', text, items: [], time: Date.now() })
  aiLoading.value = true
  aiInput.value = ''
  await scrollAiToBottom()

  try {
    console.log('[AI] 发送请求, product_id:', product.value?.product_id, 'session_id:', currentSessionId.value)
    const response = await aiAPI.recommend({
      text,
      page: 1,
      size: 10,
      user_id: getAiUserId(),
      session_id: currentSessionId.value,
      product_id: product.value?.product_id
    })

    const payload = response?.data?.data || {}
    const aiDesc = payload?.ai_description || payload?.aiDescription || ''

    aiMessages.value.push({
      role: 'assistant',
      text: aiDesc || '未获取到 AI 回复',
      items: [],
      time: Date.now()
    })
    await scrollAiToBottom()

    if (currentSessionId.value) {
      await loadChatSessions()
    }
  } catch (error) {
    console.error('AI 对话请求失败:', error)
    aiMessages.value.push({ role: 'assistant', text: '请求失败，请稍后重试', items: [], time: Date.now() })
    ElMessage.error('AI 对话请求失败')
    await scrollAiToBottom()
  } finally {
    aiLoading.value = false
  }
}

const clearAiResult = () => {
  aiInput.value = ''
  aiMessages.value = []
}

let DeltaToHtmlConverter = null
let translateReady = false

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



const fetchProduct = async () => {
  loading.value = true
  try {
    const response = await productAPI.getProductDetail(productId.value)
    product.value = response?.data?.data ?? null
    currentImageIndex.value = 0

    if (product.value?.product_id) {
      await fetchProductPromotionInfo(product.value.product_id)
    } else {
      productPromotionInfo.value = null
    }

    // 获取后检查是否已收藏
    if (product.value?.product_id) {
      checkFavoriteStatus()
      // 获取卖家信息
      if (product.value?.product_id) {
        try {
          console.log('开始获取卖家信息，product_id:', product.value.product_id)
          const sellerResponse = await sellerAPI.getSellerByProductId(product.value.product_id)
          console.log('获取卖家信息响应:', sellerResponse)
          sellerInfo.value = sellerResponse?.data?.data ?? null
          console.log('卖家信息:', sellerInfo.value)
        } catch (sellerError) {
          console.error('获取卖家信息失败:', sellerError)
          console.error('错误详情:', sellerError.response)
        }
      }
      // 获取价格历史
      fetchPriceHistory()
    }
  } catch (error) {
    console.error('获取商品详情失败:', error)
    product.value = null
    productPromotionInfo.value = null
  } finally {
    loading.value = false
  }
}
const fetchPriceHistory = async () => {
  if (!product.value?.product_id) return
  
  priceHistoryLoading.value = true
  try {
    const response = await priceHistoryAPI.getPriceHistory(product.value.product_id)
    console.log('获取价格历史响应:', response)
    
    // 后端返回格式：response.data.data.price_trend
    const responseData = response?.data?.data
    const priceTrend = responseData?.price_trend || []
    
    if (response?.data?.code === 200 && priceTrend.length > 0) {
      // 转换后端返回的字段格式
      priceHistory.value = priceTrend.map((item, index) => ({
        historyId: index + 1,
        productId: product.value.product_id,
        oldPrice: index > 0 ? priceTrend[index - 1].price : item.price,
        newPrice: item.price,
        changeType: item.change_type || 'MANUAL',
        reason: getChangeReason(item.change_type),
        createdAt: item.date
      }))
      
      console.log('价格历史数据:', priceHistory.value)
      generatePriceChart()
    } else {
      console.warn('没有价格历史数据')
      priceHistory.value = []
    }
  } catch (error) {
    console.error('获取价格历史失败:', error)
    priceHistory.value = []
  } finally {
    priceHistoryLoading.value = false
  }
}

// 辅助函数：获取变动原因
const getChangeReason = (changeType) => {
  const reasons = {
    'MANUAL': '手动修改',
    'PROMOTION_START': '促销开始',
    'PROMOTION_END': '促销结束',
    'SYSTEM': '系统调整'
  }
  return reasons[changeType] || '价格调整'
}

const generatePriceChart = () => {
  if (priceHistory.value.length === 0) return
  
  const dates = []
  const prices = []
  const changes = []
  const reasons = []
  
  const sortedHistory = [...priceHistory.value].sort((a, b) => {
    return new Date(a.createdAt) - new Date(b.createdAt)
  })
  
  const completePriceHistory = []
  
  if (sortedHistory.length > 0) {
    const firstRecord = sortedHistory[0]
    const initialPrice = typeof firstRecord.oldPrice === 'object' 
      ? firstRecord.oldPrice 
      : parseFloat(firstRecord.oldPrice)
    
    completePriceHistory.push({
      createdAt: firstRecord.createdAt,
      newPrice: initialPrice,
      oldPrice: initialPrice,
      changeType: 'initial',
      reason: '初始价格'
    })
    
    sortedHistory.forEach(item => {
      completePriceHistory.push({
        createdAt: item.createdAt,
        newPrice: typeof item.newPrice === 'object' ? item.newPrice : parseFloat(item.newPrice),
        oldPrice: typeof item.oldPrice === 'object' ? item.oldPrice : parseFloat(item.oldPrice),
        changeType: item.changeType,
        reason: item.reason
      })
    })
    
    if (product.value?.price) {
      const lastRecord = completePriceHistory[completePriceHistory.length - 1]
      const currentPrice = typeof product.value.price === 'object' 
        ? product.value.price 
        : parseFloat(product.value.price)
      
      if (lastRecord.newPrice !== currentPrice) {
        completePriceHistory.push({
          createdAt: new Date().toISOString(),
          newPrice: currentPrice,
          oldPrice: lastRecord.newPrice,
          changeType: 'current',
          reason: '当前价格'
        })
      }
    }
  }
  
  completePriceHistory.forEach((item, index) => {
    dates.push(new Date(item.createdAt).toLocaleString('zh-CN'))
    prices.push(item.newPrice)
    
    if (index > 0) {
      const prevPrice = completePriceHistory[index - 1].newPrice
      const change = item.newPrice - prevPrice
      changes.push(change)
    } else {
      changes.push(0)
    }
    
    reasons.push(item.reason || '无')
  })
  
  priceChartOption.value = {
    backgroundColor: {
      type: 'linear',
      x: 0, y: 0, x2: 0, y2: 1,
      colorStops: [
        { offset: 0, color: '#fafbfc' },
        { offset: 1, color: '#f5f7fa' }
      ]
    },
    title: {
      text: '价格变动趋势',
      left: 'center',
      top: 20,
      textStyle: {
        fontSize: 20,
        fontWeight: 600,
        color: '#1a1a2e',
        letterSpacing: 2,
        textShadowColor: 'rgba(0, 0, 0, 0.05)',
        textShadowBlur: 10
      }
    },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.98)',
      borderColor: '#e8ecf0',
      borderWidth: 1,
      borderRadius: 12,
      padding: [16, 20],
      textStyle: {
        color: '#2d3748',
        fontSize: 14,
        lineHeight: 1.6
      },
      boxShadow: '0 8px 24px rgba(0, 0, 0, 0.1)',
      formatter: function(params) {
        const data = params[0]
        const index = params[0].dataIndex
        const change = changes[index]
        const reason = reasons[index]
        
        const changeColor = change > 0 ? '#ef4444' : change < 0 ? '#10b981' : '#6b7280'
        const changeIcon = change > 0 ? '📈' : change < 0 ? '📉' : '➡️'
        const changeText = change > 0 
          ? `<span style="color: ${changeColor}; font-weight: 600;">${changeIcon} +¥${change.toFixed(2)}</span>`
          : change < 0
          ? `<span style="color: ${changeColor}; font-weight: 600;">${changeIcon} -¥${Math.abs(change).toFixed(2)}</span>`
          : `<span style="color: ${changeColor};">${changeIcon} 无变动</span>`
        
        return `
          <div style="padding: 4px;">
            <div style="font-size: 15px; font-weight: 600; color: #1a1a2e; margin-bottom: 8px;">${data.name}</div>
            <div style="display: flex; align-items: center; margin-bottom: 6px;">
              <span style="color: #6b7280; width: 60px;">价格：</span>
              <span style="font-size: 18px; font-weight: 700; color: #3b82f6;">¥${data.value.toFixed(2)}</span>
            </div>
            <div style="display: flex; align-items: center; margin-bottom: 6px;">
              <span style="color: #6b7280; width: 60px;">变动：</span>
              ${changeText}
            </div>
            <div style="display: flex; align-items: center;">
              <span style="color: #6b7280; width: 60px;">原因：</span>
              <span style="color: #4b5563;">${reason}</span>
            </div>
          </div>
        `
      }
    },
    grid: {
      left: '3%',
      right: '3%',
      top: '12%',
      bottom: '10%',
      height: '75%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: dates,
      axisLabel: {
        rotate: 45,
        fontSize: 12,
        color: '#6b7280',
        fontWeight: 400,
        margin: 15
      },
      axisLine: {
        lineStyle: {
          color: '#e5e7eb',
          width: 1
        }
      },
      axisTick: {
        show: false
      }
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        formatter: '¥{value}',
        color: '#6b7280',
        fontSize: 12,
        fontWeight: 400
      },
      axisLine: {
        show: true,
        lineStyle: {
          color: '#e5e7eb',
          width: 1
        }
      },
      axisTick: {
        show: false
      },
      splitLine: {
        lineStyle: {
          color: '#f3f4f6',
          width: 1,
          type: 'dashed'
        }
      }
    },
    series: [
      {
        name: '价格',
        type: 'line',
        data: prices,
        smooth: true,
        symbol: 'circle',
        symbolSize: 10,
        lineStyle: {
          color: '#4f46e5',
          width: 3,
          shadowColor: 'rgba(79, 70, 229, 0.2)',
          shadowBlur: 12,
          shadowOffsetY: 6
        },
        itemStyle: {
          color: function(params) {
            const change = changes[params.dataIndex]
            if (change > 0) return '#ef4444'
            if (change < 0) return '#10b981'
            return '#4f46e5'
          },
          borderColor: '#ffffff',
          borderWidth: 3,
          shadowColor: function(params) {
            const change = changes[params.dataIndex]
            if (change > 0) return 'rgba(239, 68, 68, 0.3)'
            if (change < 0) return 'rgba(16, 185, 129, 0.3)'
            return 'rgba(79, 70, 229, 0.3)'
          },
          shadowBlur: 8,
          shadowOffsetY: 3
        },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(79, 70, 229, 0.25)' },
              { offset: 0.5, color: 'rgba(79, 70, 229, 0.1)' },
              { offset: 1, color: 'rgba(79, 70, 229, 0.02)' }
            ]
          }
        },
        emphasis: {
          focus: 'series',
          scale: true,
          itemStyle: {
            symbolSize: 16,
            borderWidth: 4,
            shadowBlur: 15,
            shadowOffsetY: 5
          }
        },
        animationDuration: 1500,
        animationEasing: 'cubicOut'
      }
    ],
    dataZoom: [
      {
        type: 'inside',
        start: 0,
        end: 100,
        zoomLock: false,
        zoomOnMouseWheel: true,
        moveOnMouseMove: true
      },
      {
        type: 'slider',
        start: 0,
        end: 100,
        height: 24,
        bottom: 20,
        left: '5%',
        right: '5%',
        fillerColor: 'rgba(79, 70, 229, 0.12)',
        borderColor: '#e5e7eb',
        borderWidth: 1,
        backgroundColor: '#f9fafb',
        handleIcon: 'circle',
        handleSize: '100%',
        handleStyle: {
          color: '#ffffff',
          borderColor: '#4f46e5',
          borderWidth: 2,
          shadowColor: 'rgba(79, 70, 229, 0.3)',
          shadowBlur: 4,
          shadowOffsetY: 2
        },
        textStyle: {
          color: '#9ca3af',
          fontSize: 11
        },
        selectedDataBackgroundColor: 'rgba(79, 70, 229, 0.2)',
        dataBackground: {
          lineStyle: { color: '#e5e7eb' },
          areaStyle: { color: '#f3f4f6' }
        }
      }
    ],
    toolbox: {
      show: true,
      right: '5%',
      top: '15%',
      feature: {
        saveAsImage: {
          show: true,
          title: '保存图片',
          pixelRatio: 2,
          backgroundColor: '#ffffff',
          iconStyle: {
            borderColor: '#e5e7eb',
            borderWidth: 1,
            borderRadius: 6
          }
        },
        dataZoom: {
          show: true,
          title: {
            zoom: '区域缩放',
            back: '缩放还原'
          },
          iconStyle: {
            borderColor: '#e5e7eb',
            borderWidth: 1,
            borderRadius: 6
          }
        },
        refresh: {
          show: true,
          title: '刷新数据',
          iconStyle: {
            borderColor: '#e5e7eb',
            borderWidth: 1,
            borderRadius: 6
          }
        }
      }
    }
  }
}

const fetchProductPromotionInfo = async (id) => {
  if (!id) {
    productPromotionInfo.value = null
    return
  }
  promotionLoading.value = true
  try {
    const res = await promotionAPI.getProductPromotions(id)
    productPromotionInfo.value = res?.data?.data ?? null
  } catch (e) {
    console.error('获取商品促销详情失败:', e)
    productPromotionInfo.value = null
  } finally {
    promotionLoading.value = false
  }
}

const purchasePromotionInfo = computed(() => {
  const p = product.value || {}
  const promo = productPromotionInfo.value || {}
  return {
    product_id: p.product_id,
    product_name: p.product_name,
    original_price: promo.original_price ?? p.original_price ?? p.price ?? null,
    current_price: p.current_promotion_price ?? promo.current_price ?? p.price ?? null,
    current_promotion_price: promo.current_promotion_price ?? p.current_promotion_price ?? null,
    has_active_promotion: promo.has_active_promotion ?? Boolean(p.has_active_promotion),
    active_promotion_ids: promo.active_promotion_ids || [],
    best_promotion: promo.best_promotion || null,
    promotions: Array.isArray(promo.promotions) ? promo.promotions : []
  }
})

// 检查收藏状态
const checkFavoriteStatus = async () => {
  const customerId = getCurrentCustomerId()
  if (!customerId || !product.value?.product_id) return

  try {
    // 这里应该调用你的收藏列表API来检查当前商品是否在收藏中
    // 假设有一个API可以获取用户收藏列表
    const res = await favoritesAPI.getFavorites({ customer_id: customerId })
    const favorites = res?.data?.data?.list || []
    isFavorited.value = favorites.some(item => item.product_id === product.value?.product_id)
  } catch (error) {
    console.error('检查收藏状态失败:', error)
    // 如果API不存在，暂时设为false
    isFavorited.value = false
  }
}

const handlePurchaseSuccess = () => {
  fetchProduct()
}

const getStatusType = (status) => {
  const types = { online: 'success', frozen: 'warning', sold: 'info', outOfStock: 'danger' }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = { online: '在售', frozen: '交易中', sold: '已售出', outOfStock: '无库存' }
  return texts[status] || status
}

const getButtonText = (status) => {
  const texts = {
    online: '立即购买',
    frozen: '商品交易中',
    sold: '商品已售出',
    outOfStock: '库存不足'
  }
  return texts[status] || '暂不可用'
}

const formatTime = (time) => {
  return time ? new Date(time).toLocaleString('zh-CN', { year:'numeric', month:'2-digit', day:'2-digit', hour:'2-digit', minute:'2-digit' }) : ''
}

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

const imageList = computed(() => {
  if (!product.value) return []

  if (Array.isArray(product.value.images) && product.value.images.length) {
    return product.value.images.map((it) => {
      if (typeof it === 'string') return normalizeMediaUrl(it)
      return normalizeMediaUrl(it?.image_url || it?.media_url || it?.url || '')
    }).filter(Boolean)
  }

  const url = normalizeMediaUrl(product.value.image_url || '')
  return url ? [url] : []
})

const hasMedia = computed(() => {
  return !!(product.value && Array.isArray(product.value.media_resources) && product.value.media_resources.length)
})

const isImage = (m) => {
  if (!m) return false
  const t = (m.media_type || m.mime_type || '').toLowerCase()
  return t.startsWith('image') || t === 'image'
}
const isVideo = (m) => {
  if (!m) return false
  const t = (m.media_type || m.mime_type || '').toLowerCase()
  return t.startsWith('video') || t === 'video' || (m.media_url && /\.(mp4|webm|ogg)(\?.*)?$/i.test(m.media_url))
}
const isAudio = (m) => {
  if (!m) return false
  const t = (m.media_type || m.mime_type || '').toLowerCase()
  return t.startsWith('audio') || t === 'audio' || (m.media_url && /\.(mp3|wav|ogg)(\?.*)?$/i.test(m.media_url))
}

const escapeHtml = (s) => {
  return String(s)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;')
}

const looksLikeHtml = (s) => /<\/?[a-z][\s\S]*>/i.test(s)
const looksLikeMarkdown = (s) => {
  if (!s) return false
  return /(\*\*.+\*\*|`{1,3}.+`{1,3}|^#{1,6}\s+|^\s*[-*+]\s+|^\s*\d+\.\s+)/m.test(s)
}

const convertDescToHtml = (desc) => {
  if (!desc && desc !== 0) return ''
  if (typeof desc === 'string') {
    try {
      const parsed = JSON.parse(desc)
      if (parsed && typeof parsed === 'object') return convertDescToHtml(parsed)
    } catch (_) {}

    const source = desc.trim()
    if (source && looksLikeMarkdown(source) && !looksLikeHtml(source)) {
      try {
        const mdHtml = marked.parse(source, { gfm: true, breaks: true })
        return DOMPurify.sanitize(mdHtml, { SAFE_FOR_TEMPLATES: true })
      } catch (e) {
        console.warn('Markdown -> HTML 转换失败，回退为纯文本:', e)
      }
    }

    return DOMPurify.sanitize(desc, { SAFE_FOR_TEMPLATES: true })
  }

  if (typeof desc === 'object') {
    if (Array.isArray(desc.ops) && DeltaToHtmlConverter) {
      try {
        const conv = new DeltaToHtmlConverter(desc.ops, { encodeHtml: false })
        const html = conv.convert()
        return DOMPurify.sanitize(html, { SAFE_FOR_TEMPLATES: true })
      } catch (e) {
        console.warn('Delta -> HTML 转换失败，回退到 JSON:', e)
        return DOMPurify.sanitize(`<pre>${escapeHtml(JSON.stringify(desc, null, 2))}</pre>`, { SAFE_FOR_TEMPLATES: true })
      }
    }
    if (typeof desc.html === 'string' && desc.html.trim()) {
      return DOMPurify.sanitize(desc.html, { SAFE_FOR_TEMPLATES: true })
    }
    if (desc.delta || desc.content) {
      return convertDescToHtml(desc.delta || desc.content)
    }
    return DOMPurify.sanitize(`<pre>${escapeHtml(JSON.stringify(desc, null, 2))}</pre>`, { SAFE_FOR_TEMPLATES: true })
  }

  return DOMPurify.sanitize(String(desc), { SAFE_FOR_TEMPLATES: true })
}

const safeDescription = computed(() => {
  return convertDescToHtml(product.value?.product_desc)
})

const showImage = (idx) => {
  if (idx >= 0 && idx < imageList.value.length) {
    currentImageIndex.value = idx
  }
}
const openImageDialog = (idx) => {
  showImage(idx)
  showImageDialog.value = true
}

const getCurrentCustomerId = () => {
  try {
    const raw = localStorage.getItem('customer_info')
    if (!raw) return null
    const info = JSON.parse(raw)
    return info?.customer_id ?? null
  } catch {
    return null
  }
}

const canOnline = computed(() => product.value?.product_status === 'online')
const hasStock = computed(() => (product.value?.stock_quantity ?? 0) > 0)
const canBuy = computed(() => canOnline.value && hasStock.value)
const canFavorite = computed(() => canOnline.value)
const canAddCart = computed(() => canOnline.value && hasStock.value)

const toNumberPrice = (val) => {
  const n = Number(val)
  return Number.isFinite(n) ? n : null
}

const currentDisplayPrice = computed(() => {
  if (!product.value) return null
  return toNumberPrice(
    product.value.current_promotion_price ??
    product.value.current_price ??
    product.value.final_price ??
    product.value.price
  )
})

const originalDisplayPrice = computed(() => {
  if (!product.value) return null
  const original = toNumberPrice(product.value.original_price)
  return original ?? currentDisplayPrice.value
})

const hasDiscountPrice = computed(() => {
  if (currentDisplayPrice.value == null || originalDisplayPrice.value == null) return false
  return originalDisplayPrice.value > currentDisplayPrice.value
})

const discountAmount = computed(() => {
  if (!hasDiscountPrice.value) return 0
  return originalDisplayPrice.value - currentDisplayPrice.value
})

// 切换收藏状态
const toggleFavorite = async () => {
  const customerId = getCurrentCustomerId()
  if (!customerId) {
    ElMessage.error('请先登录客户账号')
    return
  }
  if (!product.value?.product_id) {
    ElMessage.error('商品信息不完整')
    return
  }

  submitLoading.value = true
  try {
    if (isFavorited.value) {
      // 取消收藏 - 需要调用删除API
      // 假设有 deleteFavorite API
      // await favoritesAPI.deleteFavorite({ customer_id: customerId, product_id: product.value.product_id })
      // 临时模拟成功
      isFavorited.value = false
      ElMessage.success('已取消收藏')
    } else {
      // 添加收藏
      const payload = { customer_id: customerId, product_id: product.value.product_id }
      const res = await favoritesAPI.addToFavorites(payload)
      const data = res?.data?.data ?? null
      isFavorited.value = true
      ElMessage.success(data ? '收藏成功' : '收藏成功')
    }
  } catch (e) {
    console.error('操作收藏失败:', e)
    ElMessage.error(e?.response?.data?.message || '操作失败')
  } finally {
    submitLoading.value = false
  }
}

const addCart = async () => {
  const customerId = getCurrentCustomerId()
  if (!customerId) {
    ElMessage.error('请先登录客户账号')
    return
  }
  if (!product.value?.product_id) {
    ElMessage.error('商品信息不完整')
    return
  }
  if (!canAddCart.value) {
    ElMessage.warning('当前不可加入购物车')
    return
  }
  if (!quantity.value || quantity.value <= 0) {
    ElMessage.warning('数量必须大于 0')
    return
  }

  submitLoading.value = true
  try {
    const payload = {
      customer_id: Number(customerId),
      product_id: Number(product.value.product_id),
      quantity: Number(quantity.value)
    }
    const res = await cartAPI.addToCart(payload)
    const code = res?.data?.code
    const msg = res?.data?.message

    if (code === 200) {
      ElMessage.success(msg || '已加入购物车')
    } else {
      ElMessage.error(msg || '加入购物车失败')
    }
  } catch (e) {
    console.error('加入购物车异常:', e)
    ElMessage.error(e?.response?.data?.message || '网络异常，稍后重试')
  } finally {
    submitLoading.value = false
  }
}

onMounted(async () => {
  try {
    await ensureTranslateScriptLoaded()
    initTranslate()
  } catch (e) {
    console.warn('translate.js 加载失败：', e)
  }

  try {
    const mod = await import('quill-delta-to-html')
    DeltaToHtmlConverter = mod.DeltaToHtmlConverter || mod.default?.DeltaToHtmlConverter || mod.default || mod
  } catch (e) {
    console.warn('无法加载 quill-delta-to-html，delta 将以 JSON/text 展示：', e)
  }

  if (productId.value) {
    fetchProduct()
  }

  loadChatSessions()
})
</script>

<style scoped>
/* 全局设计变量 */
.product-detail {
  min-height: 100vh;
  background: linear-gradient(145deg, #f9fafc 0%, #f2f4f8 100%);
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, system-ui, sans-serif;
  width: 100%;
  overflow-x: hidden;
}

/* 玻璃质感头部 */
.header {
  background: rgba(255,255,255,0.72);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(224, 230, 237, 0.5);
  display: flex;
  align-items: center;
  padding: 16px 32px;
  position: sticky;
  top: 0;
  z-index: 10;
  width: 100%;
  box-sizing: border-box;
}
.header-inner {
  width: 100%;
  max-width: 1440px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.btn-language {
  border: none;
  background: rgba(0,0,0,0.02);
  border-radius: 40px;
  padding: 8px 18px;
}
.btn-language:hover {
  background: rgba(0,0,0,0.04);
}

.breadcrumb-modern :deep(.el-breadcrumb__item) .el-breadcrumb__inner {
  font-weight: 450;
  color: #2c3e50;
  font-size: 0.95rem;
}
.breadcrumb-modern :deep(.el-breadcrumb__item:last-child) .el-breadcrumb__inner {
  color: #1e293b;
  font-weight: 600;
}
.btn-return {
  border: none;
  background: rgba(0,0,0,0.02);
  border-radius: 40px;
  padding: 8px 18px;
  transition: all 0.2s;
}
.btn-return:hover {
  background: rgba(0,0,0,0.04);
  transform: scale(0.98);
}

.main-content {
  max-width: 1440px;
  margin: 0 auto;
  padding: 24px 32px;
  width: 100%;
  box-sizing: border-box;
}

.detail-wrap {
  background: transparent;
  margin: 0;
  padding: 0;
  box-shadow: none;
  width: 100%;
}

/* 修复栅格系统 */
.main-row {
  margin: 0 -12px !important;
  width: calc(100% + 24px) !important;
}

.visual-area {
  background: transparent;
}

.image-panel {
  background: #fff;
  border-radius: 32px;
  padding: 24px;
  box-shadow: 0 20px 40px -12px rgba(0,20,40,0.12);
  backdrop-filter: blur(2px);
  border: 1px solid rgba(255,255,255,0.6);
}

.main-image {
  width: 100%;
  height: 500px;
  background: #f7f9fc;
  border-radius: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  cursor: zoom-in;
  transition: box-shadow 0.25s;
}
.main-image:hover {
  box-shadow: 0 12px 28px rgba(0,0,0,0.04);
}
.main-image img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  transition: transform 0.4s ease;
}
.main-image:hover img {
  transform: scale(1.02);
}
.no-image {
  color: #a3a9b3;
  font-size: 1.2rem;
}
.zoom-hint {
  position: absolute;
  right: 20px;
  bottom: 20px;
  background: rgba(255,255,255,0.7);
  backdrop-filter: blur(10px);
  color: #1e293b;
  padding: 6px 14px;
  font-size: 0.85rem;
  border-radius: 40px;
  display: flex;
  align-items: center;
  gap: 6px;
  border: 1px solid rgba(0,0,0,0.04);
}

.thumbs {
  display: flex;
  gap: 12px;
  margin-top: 20px;
  overflow-x: auto;
  padding: 4px 2px;
}
.thumb {
  width: 90px;
  height: 70px;
  border-radius: 18px;
  overflow: hidden;
  cursor: pointer;
  border: 2px solid transparent;
  transition: all 0.2s;
  flex-shrink: 0;
  background: #fff;
  box-shadow: 0 4px 10px rgba(0,0,0,0.02);
}
.thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.thumb.active {
  border-color: #1e293b;
  box-shadow: 0 8px 16px rgba(30, 41, 59, 0.12);
  transform: translateY(-2px);
}

.desc-tabs-modern {
  margin-top: 32px;
}
.desc-tabs-modern :deep(.el-tabs__header) {
  margin-bottom: 20px;
  border-bottom: 1px solid #eef2f6;
}
.desc-tabs-modern :deep(.el-tabs__item) {
  font-size: 1rem;
  font-weight: 500;
  padding: 12px 20px;
  color: #5b6877;
  transition: color 0.2s;
}
.desc-tabs-modern :deep(.el-tabs__item.is-active) {
  color: #1e293b;
  font-weight: 600;
}
.desc-tabs-modern :deep(.el-tabs__active-bar) {
  background-color: #1e293b;
  height: 3px;
  border-radius: 3px 3px 0 0;
}

.description {
  color: #334155;
  line-height: 1.8;
  font-size: 1rem;
  padding: 8px 4px 16px;
}
.description:deep(img) {
  max-width: 100%;
  border-radius: 16px;
  margin: 12px 0;
}

.media-section {
  padding: 4px 0;
}
.media-grid {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
}
.media-card {
  width: 240px;
  background: #fff;
  border-radius: 24px;
  overflow: hidden;
  border: 1px solid #edf2f7;
  transition: all 0.2s;
  box-shadow: 0 6px 18px rgba(0,0,0,0.02);
}
.media-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 20px 28px -10px rgba(16,24,40,0.12);
}
.media-preview {
  height: 160px;
  background: #f8fafd;
  display: flex;
  align-items: center;
  justify-content: center;
}
.media-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.video-preview video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.audio-preview {
  flex-direction: column;
  padding: 16px;
  gap: 12px;
}
.audio-placeholder {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #526277;
}
.audio-filename {
  font-size: 0.9rem;
  max-width: 140px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.fallback {
  flex-direction: column;
  gap: 8px;
  color: #8b9aa8;
}
.media-footer {
  padding: 12px 14px;
  display: flex;
  justify-content: space-between;
  font-size: 0.85rem;
  border-top: 1px solid #f0f4f9;
  background: #fff;
}
.media-footer .name {
  font-weight: 550;
  color: #1e293b;
  max-width: 130px;
  overflow: hidden;
  text-overflow: ellipsis;
}
.media-footer .meta {
  color: #7e8c9e;
}

/* 右侧购买卡片 */
.purchase-card {
  position: sticky;
  top: 110px;
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
}
.card-inner {
  background: rgba(255,255,255,0.8);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(255,255,255,0.9);
  border-radius: 32px;
  padding: 24px 20px;
  box-shadow: 0 25px 45px -18px rgba(18,35,60,0.2);
  width: 100%;
  box-sizing: border-box;
}

.product-title {
  font-size: 1.8rem;
  font-weight: 600;
  line-height: 1.3;
  margin: 0 0 16px 0;
  color: #0f172a;
  letter-spacing: -0.01em;
  word-break: break-word;
}

.price-section {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 10px;
}
.price-wrapper {
  display: flex;
  align-items: baseline;
  gap: 2px;
}
.price-currency {
  font-size: 1.3rem;
  color: #e35f5f;
  font-weight: 500;
}
.price-value {
  font-size: 2.4rem;
  font-weight: 700;
  color: #cf4444;
  line-height: 1;
}
.price-original {
  margin-left: 10px;
  font-size: 1rem;
  color: #94a3b8;
  text-decoration: line-through;
}
.price-save {
  margin-left: 8px;
  font-size: 0.95rem;
  font-weight: 600;
  color: #16a34a;
}
.status-tag {
  font-weight: 500;
  border: none;
  padding: 6px 16px;
  border-radius: 40px;
  font-size: 0.85rem;
}

.stock-modern {
  margin: 20px 0 22px;
}
.stock-label {
  display: flex;
  justify-content: space-between;
  font-size: 0.95rem;
  color: #475569;
  margin-bottom: 6px;
}
.stock-label .stock-low {
  color: #e6a23c;
  font-weight: 600;
}
.stock-progress {
  width: 100%;
}
.stock-progress :deep(.el-progress-bar__outer) {
  background-color: #e9ecf0;
  border-radius: 30px;
}
.stock-progress :deep(.el-progress-bar__inner) {
  border-radius: 30px;
  transition: width 0.3s;
}

/* 操作按钮组 */
.action-group {
  display: flex;
  flex-direction: column;
  gap: 14px;
  width: 100%;
}

.action-row {
  display: flex;
  gap: 12px;
  width: 100%;
}

.cart-row-fixed {
  flex-wrap: nowrap;
}

.action-row .btn-buy {
  flex: 2;
  background: linear-gradient(165deg, #1e293b, #0f1829);
  border: none;
  font-weight: 600;
  letter-spacing: 0.5px;
  border-radius: 60px;
  height: 52px;
  padding: 0 16px;
  min-width: 0;
  white-space: nowrap;
}
.action-row .btn-buy:hover {
  background: linear-gradient(165deg, #2d3a4e, #1a2538);
  transform: scale(0.99);
}

/* 收藏按钮样式 - 已收藏状态 */
.action-row .btn-favorite {
  flex: 1;
  border-radius: 60px;
  height: 52px;
  border: 1px solid #dee5ed;
  background: rgba(255,255,255,0.5);
  font-weight: 500;
  color: #334155;
  padding: 0 12px;
  min-width: 0;
  white-space: nowrap;
  transition: all 0.2s;
}
.action-row .btn-favorite.is-favorited {
  background: #fef3c7;
  border-color: #fbbf24;
  color: #b45309;
}
.action-row .btn-favorite.is-favorited:hover {
  background: #fde68a;
}
.star-filled {
  color: #f59e0b;
  fill: #f59e0b;
}

/* 数量选择器 - 修复上下按钮位置 */
.quantity-selector {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #f4f7fc;
  padding: 4px 4px 4px 16px;
  border-radius: 48px;
  height: 52px;
  flex: 1;
  min-width: 0;
}
.quantity-label {
  font-weight: 500;
  color: #475569;
  white-space: nowrap;
}

/* 修复input-number上下按钮位置 */
.modern-number.fixed-controls {
  width: 120px;
  flex-shrink: 0;
}
.modern-number.fixed-controls :deep(.el-input-number__input) {
  text-align: center;
  font-weight: 600;
}
.modern-number.fixed-controls :deep(.el-input__wrapper) {
  background: white;
  box-shadow: 0 2px 6px rgba(0,0,0,0.04) !important;
  border-radius: 30px;
  padding: 0 24px 0 8px;
  height: 42px;
}
.modern-number.fixed-controls :deep(.el-input-number__increase),
.modern-number.fixed-controls :deep(.el-input-number__decrease) {
  background: #ffffff;
  border: 1px solid #dce3ec;
  border-radius: 20px;
  width: 22px;
  height: 22px;
  line-height: 22px;
  top: 10px;
  right: 6px;
  color: #2c3e50;
  display: flex;
  align-items: center;
  justify-content: center;
}
.modern-number.fixed-controls :deep(.el-input-number__decrease) {
  right: auto;
  left: 6px;
  top: 10px;
}
.modern-number.fixed-controls :deep(.el-input-number__increase:hover),
.modern-number.fixed-controls :deep(.el-input-number__decrease:hover) {
  background: #f0f2f5;
  color: #1e293b;
}

.btn-cart {
  height: 52px;
  border-radius: 60px;
  padding: 0 16px;
  background: #fff3e3;
  border: 1px solid #ffdbb5;
  color: #b35f1b;
  font-weight: 600;
  flex: 1;
  min-width: 0;
  white-space: nowrap;
}
.btn-cart:hover {
  background: #ffedd5;
  border-color: #f3b95f;
}
.btn-contact {
  border-radius: 60px;
  height: 48px;
  background: #ffffffd9;
  border: 1px solid #d9e1ec;
  font-weight: 500;
  color: #445566;
  width: 100%;
}

.product-footer {
  margin-top: 20px;
}
.footer-meta {
  display: flex;
  justify-content: space-between;
  color: #7b8a9a;
  font-size: 0.8rem;
  padding: 0 4px;
  flex-wrap: wrap;
  gap: 8px;
}
.el-divider {
  margin: 16px 0 12px;
  border-top: 1px dashed #d0dae8;
}

.loading-state, .error-state {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
}
.smooth-skeleton :deep(.el-skeleton__item) {
  border-radius: 20px;
}

/* 图片弹窗 */
.image-dialog :deep(.el-dialog) {
  background: transparent;
  box-shadow: none;
}
.image-dialog :deep(.el-dialog__headerbtn) {
  background: rgba(0,0,0,0.3);
  border-radius: 50%;
  color: white;
  font-size: 20px;
}
.dialog-image-wrap {
  display: flex;
  justify-content: center;
  align-items: center;
  background: #1b1f26;
  border-radius: 40px;
  padding: 18px;
}

/* 响应式调整 */
@media (max-width: 992px) {
  .main-content {
    padding: 16px 20px;
  }
  .cart-row-fixed {
    flex-wrap: wrap;
  }
  .quantity-selector {
    width: 100%;
  }
  .btn-cart {
    width: 100%;
  }
  .product-title {
    font-size: 1.6rem;
  }
  .price-value {
    font-size: 2.2rem;
  }
}

@media (max-width: 768px) {
  .header { padding: 12px 16px; }
  .main-content { padding: 16px; }
  .main-image { height: 340px; }
  .image-panel { padding: 16px; }
  .purchase-card {
    position: relative;
    top: 0;
    margin-top: 20px;
  }
}

.btn-ai {
  margin-top: 8px;
}

.ai-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 12px;
}

.ai-sessions-bar {
  background: #f1f5f9;
  border-radius: 8px;
  padding: 8px;
}

.ai-sessions-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.ai-sessions-title {
  font-size: 14px;
  font-weight: 600;
  color: #334155;
}

.ai-sessions-list {
  max-height: 120px;
  overflow-y: auto;
}

.ai-session-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 8px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s;
}

.ai-session-item:hover {
  background: #e2e8f0;
}

.ai-session-item.active {
  background: #dbeafe;
}

.ai-session-item .session-name {
  font-size: 13px;
  color: #475569;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ai-session-item .delete-btn {
  opacity: 0;
  transition: opacity 0.2s;
}

.ai-session-item:hover .delete-btn {
  opacity: 1;
}

.ai-sessions-empty {
  font-size: 12px;
  color: #94a3b8;
  text-align: center;
  padding: 8px;
}

.ai-chat-body {
  overflow-y: auto;
  flex: 1;
  padding: 8px;
  background: #f8fafc;
  border-radius: 8px;
}

.ai-empty-chat {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.ai-msg-row {
  margin-bottom: 10px;
}

.ai-msg-row.is-user {
  text-align: right;
}

.ai-msg-row.is-user .ai-bubble {
  background: #3b82f6;
  color: #fff;
}

.ai-msg-row.is-assistant .ai-bubble {
  background: #fff;
  color: #334155;
  border: 1px solid #e2e8f0;
}

.ai-bubble {
  display: inline-block;
  max-width: 85%;
  padding: 10px 14px;
  border-radius: 12px;
  word-break: break-word;
  text-align: left;
}

.ai-msg-text {
  font-size: 14px;
  line-height: 1.5;
}

.ai-input-box {
  padding: 8px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}

.ai-actions-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
}

.ai-tip {
  font-size: 12px;
  color: #94a3b8;
  margin-left: auto;
}

.price-trend-section {
  padding: 20px 0;
}

.price-chart-container {
  background: #ffffff;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  margin-bottom: 24px;
}

.price-history-list {
  margin-top: 24px;
}

.history-title {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 2px solid #f1f5f9;
}

.loading-state {
  padding: 40px 20px;
}

.empty-state {
  padding: 60px 20px;
  text-align: center;
}
</style>