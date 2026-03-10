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

          <el-button @click="$router.push('/')" class="btn-return" :icon="ArrowLeft" size="default">
            返回
          </el-button>
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
                      <span class="price-value">{{ product.price != null ? product.price.toFixed(2) : '--' }}</span>
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
                  </div>

                  <div class="product-footer">
                    <el-divider>
                      <el-icon><Goods /></el-icon>
                    </el-divider>
                    <div class="footer-meta">
                      <span>类别：{{ product.category?.category_name || '-' }}</span>
                      <span>发布时间：{{ formatTime(product.created_at) }}</span>
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
        @success="handlePurchaseSuccess"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { productAPI, favoritesAPI, cartAPI } from '@/api'
import PurchaseDialog from '@/components/buyer/PurchaseDialog.vue'
import DOMPurify from 'dompurify'
import { ElMessage } from 'element-plus'
import { ArrowLeft, ZoomIn, ShoppingCart, Star, Plus, Message, Headset, Document, Goods } from '@element-plus/icons-vue'

const route = useRoute()
const product = ref(null)
const loading = ref(true)
const showPurchaseDialog = ref(false)
const showImageDialog = ref(false)
const currentImageIndex = ref(0)
const submitLoading = ref(false)
const quantity = ref(1)
const activeTab = ref('desc')
const isFavorited = ref(false) // 收藏状态
const productId = computed(() => route.params.id)

let DeltaToHtmlConverter = null

const fetchProduct = async () => {
  loading.value = true
  try {
    const response = await productAPI.getProductDetail(productId.value)
    product.value = response?.data?.data ?? response?.data ?? null
    currentImageIndex.value = 0

    // 获取后检查是否已收藏
    if (product.value?.product_id) {
      checkFavoriteStatus()
    }
  } catch (error) {
    console.error('获取商品详情失败:', error)
    product.value = null
  } finally {
    loading.value = false
  }
}

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

const imageList = computed(() => {
  if (!product.value) return []
  if (Array.isArray(product.value.images) && product.value.images.length) {
    return product.value.images.map(it => {
      if (typeof it === 'string') return it
      return it.image_url || it.media_url || ''
    }).filter(Boolean)
  }
  return product.value.image_url ? [product.value.image_url] : []
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

const convertDescToHtml = (desc) => {
  if (!desc && desc !== 0) return ''
  if (typeof desc === 'string') {
    try {
      const parsed = JSON.parse(desc)
      if (parsed && typeof parsed === 'object') return convertDescToHtml(parsed)
    } catch (_) {}
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
    const mod = await import('quill-delta-to-html')
    DeltaToHtmlConverter = mod.DeltaToHtmlConverter || mod.default?.DeltaToHtmlConverter || mod.default || mod
  } catch (e) {
    console.warn('无法加载 quill-delta-to-html，delta 将以 JSON/text 展示：', e)
  }

  if (productId.value) {
    fetchProduct()
  }
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
</style>