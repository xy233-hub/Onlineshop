```vue
<template>
  <div class="product-detail">
    <el-container>
      <el-header class="header">
        <div class="header-inner">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item @click="$router.push('/')">首页</el-breadcrumb-item>
            <el-breadcrumb-item>商品详情</el-breadcrumb-item>
            <el-breadcrumb-item>{{ product?.product_name || '加载中' }}</el-breadcrumb-item>
          </el-breadcrumb>

          <el-button @click="$router.push('/')" type="text" plain>
            返回
          </el-button>
        </div>
      </el-header>

      <el-main>
        <div v-if="product" class="detail-wrap">
          <el-row :gutter="24" class="main-row">
            <!-- 左侧图片与详情（宽） -->
            <el-col :xs="24" :sm="24" :md="14" :lg="16">
              <div class="visual-area">
                <div class="image-panel">
                  <div class="main-image" @click="openImageDialog(currentImageIndex)">
                    <img v-if="imageList.length" :src="imageList[currentImageIndex]" alt="主图" />
                    <div v-else class="no-image">暂无图片</div>
                    <div class="zoom-hint">点击放大</div>
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

                  <el-tabs class="desc-tabs" type="card">
                    <el-tab-pane label="商品描述">
                      <div class="description" v-html="safeDescription"></div>
                    </el-tab-pane>
                    <el-tab-pane label="媒体资源" v-if="hasMedia">
                      <div class="media-section">
                        <h4>媒体资源</h4>
                        <div class="media-grid">
                          <div
                              class="media-card"
                              v-for="(m, idx) in product.media_resources || []"
                              :key="m.media_id || m.temp_key || idx"
                          >
                            <template v-if="isImage(m)">
                              <img :src="m.media_url || m.image_url || m.url" alt="媒体图片" style="width:100%; height:140px; object-fit:cover; border-radius:6px;" />
                              <div class="media-footer">
                                <div class="name">{{ m.file_name || m.media_url?.split('/').pop() }}</div>
                                <div class="meta">{{ m.mime_type || m.media_type || '' }}</div>
                              </div>
                            </template>

                            <template v-else-if="isVideo(m)">
                              <video :src="m.media_url" controls style="width:100%; height:140px; background:#000; border-radius:6px;" />
                              <div class="media-footer">
                                <div class="name">{{ m.file_name || '视频' }}</div>
                                <div class="meta">{{ m.mime_type || m.media_type || '' }}</div>
                              </div>
                            </template>

                            <template v-else-if="isAudio(m)">
                              <div class="audio-card">
                                <div style="flex:1">
                                  <div class="audio-info">
                                    <div class="name">{{ m.file_name || '音频' }}</div>
                                    <div class="meta">{{ m.mime_type || m.media_type || '' }}</div>
                                  </div>
                                  <audio :src="m.media_url" controls style="width:100%; margin-top:8px;" />
                                </div>
                              </div>
                            </template>

                            <template v-else>
                              <div style="height:140px; display:flex; align-items:center; justify-content:center; color:#888; background:#fafafa; border-radius:6px;">
                                无法预览
                              </div>
                              <div class="media-footer">
                                <div class="name">{{ m.file_name || m.media_url }}</div>
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

            <!-- 右侧购买卡（吸附） -->
            <el-col :xs="24" :sm="24" :md="10" :lg="8">
              <div class="purchase-card">
                <div class="card-inner">
                  <h2 class="title">{{ product.product_name }}</h2>

                  <div class="price-row">
                    <div class="price">¥{{ product.price != null ? product.price.toFixed(2) : '--' }}</div>
                    <el-tag :type="getStatusType(product.product_status)">{{ getStatusText(product.product_status) }}</el-tag>
                  </div>

                  <div class="stock">库存：{{ product.stock_quantity ?? 0 }}</div>

                  <div class="actions">
                    <el-button
                        type="primary"
                        :disabled="product.product_status !== 'online' || (product.stock_quantity != null && product.stock_quantity <= 0)"
                        @click="showPurchaseDialog = true"
                    >
                      {{ getButtonText(product.product_status) }}
                    </el-button>

                    <el-button type="default" @click="$router.push({ path: '/seller', query: {} })" plain>联系卖家</el-button>
                  </div>

                  <div class="seller-tip">
                    类别：{{ product.category?.category_name || '-' }} • 发布时间：{{ formatTime(product.created_at) }}
                  </div>
                </div>
              </div>
            </el-col>
          </el-row>
        </div>

        <div v-else-if="loading" class="loading-state">
          <el-skeleton :rows="6" animated />
        </div>

        <div v-else class="error-state">
          <el-empty description="商品不存在或已被删除">
            <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
          </el-empty>
        </div>
      </el-main>
    </el-container>

    <!-- 图片弹窗 -->
    <el-dialog :visible.sync="showImageDialog" width="80%" center>
      <div class="dialog-image-wrap">
        <el-image :src="imageList[currentImageIndex]" fit="contain" style="max-height:70vh" />
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
import { productAPI } from '@/api'
import PurchaseDialog from '@/components/buyer/PurchaseDialog.vue'
import DOMPurify from 'dompurify'

const route = useRoute()
const product = ref(null)
const loading = ref(true)
const showPurchaseDialog = ref(false)
const showImageDialog = ref(false)
const currentImageIndex = ref(0)

const productId = computed(() => route.params.id)

let DeltaToHtmlConverter = null

const fetchProduct = async () => {
  loading.value = true
  try {
    const response = await productAPI.getProductDetail(productId.value)
    product.value = response?.data?.data ?? response?.data ?? null
    currentImageIndex.value = 0
  } catch (error) {
    console.error('获取商品详情失败:', error)
    product.value = null
  } finally {
    loading.value = false
  }
}

const handlePurchaseSuccess = () => {
  fetchProduct()
}

const getStatusType = (status) => {
  const types = { online: 'success', frozen: 'warning', sold: 'info' }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = { online: '在售', frozen: '交易中', sold: '已售出' }
  return texts[status] || status
}

const getButtonText = (status) => {
  const texts = {
    online: '立即购买',
    frozen: '商品交易中',
    sold: '商品已售出'
  }
  return texts[status] || '暂不可用'
}

const formatTime = (time) => {
  return time ? new Date(time).toLocaleString('zh-CN') : ''
}

// 图片列表优先 product.images -> image_url
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

// 是否有媒体
const hasMedia = computed(() => {
  return !!(product.value && Array.isArray(product.value.media_resources) && product.value.media_resources.length)
})

// 媒体类型判断
const isImage = (m) => {
  if (!m) return false
  const t = (m.media_type || m.mime_type || '').toLowerCase()
  return t.startsWith('image') || t === 'image'
}
const isVideo = (m) => {
  if (!m) return false
  const t = (m.media_type || m.mime_type || '').toLowerCase()
  return t.startsWith('video') || t === 'video' || (m.media_url && /\.(mp4|webm|ogg)(\?.*)?$/.test(m.media_url))
}
const isAudio = (m) => {
  if (!m) return false
  const t = (m.media_type || m.mime_type || '').toLowerCase()
  return t.startsWith('audio') || t === 'audio' || (m.media_url && /\.(mp3|wav|ogg)(\?.*)?$/.test(m.media_url))
}

// 富媒体描述转换与清理（复用已有逻辑）
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

// 缩略图切换与弹大图
const showImage = (idx) => {
  if (idx >= 0 && idx < imageList.value.length) {
    currentImageIndex.value = idx
  }
}
const openImageDialog = (idx) => {
  showImage(idx)
  showImageDialog.value = true
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
.product-detail { min-height: 100vh; background: #fafafa; }
.header { background: #fff; border-bottom: 1px solid #eef0f3; display:flex; align-items:center; padding:12px 24px; }
.header-inner { width:100%; display:flex; justify-content:space-between; align-items:center; gap:12px; }
.detail-wrap { max-width:1200px; margin:24px auto; padding:20px; background:#fff; border-radius:10px; box-shadow: 0 6px 18px rgba(18,26,40,0.06); }

.visual-area { padding:8px; }
.image-panel { display:flex; flex-direction:column; gap:12px; }
.main-image { width:100%; height:480px; background:#f7f7f7; display:flex; align-items:center; justify-content:center; position:relative; border-radius:8px; overflow:hidden; cursor:zoom-in; }
.main-image img { max-width:100%; max-height:100%; display:block; }
.main-image .no-image { color:#999; }
.zoom-hint { position:absolute; right:12px; bottom:12px; background:rgba(0,0,0,0.45); color:#fff; padding:4px 8px; font-size:12px; border-radius:4px; }

.thumbs { display:flex; gap:10px; margin-top:6px; overflow:auto; }
.thumb { width:80px; height:64px; border-radius:6px; overflow:hidden; cursor:pointer; border:2px solid transparent; flex:0 0 auto; }
.thumb.active { border-color:#409EFF; box-shadow:0 6px 18px rgba(64,158,255,0.12); }
.thumb img { width:100%; height:100%; object-fit:cover; }

.desc-tabs { margin-top:18px; }
.description { color:#444; line-height:1.8; font-size:15px; padding:8px 2px; }

.media-section h4 { margin:6px 0 12px; color:#333; }
.media-grid { display:flex; gap:12px; flex-wrap:wrap; }
.media-card { width:220px; background:#fff; border:1px solid #eef2f6; border-radius:8px; overflow:hidden; padding:8px; box-shadow:0 4px 14px rgba(16,24,40,0.04); display:flex; flex-direction:column; }
.media-footer { margin-top:8px; font-size:12px; color:#888; text-align:left; }
.audio-card { display:flex; gap:8px; align-items:center; height:140px; }
.audio-info .name { font-weight:600; color:#333; margin-bottom:6px; }

.meta { margin-top:14px; color:#9aa0a6; display:flex; gap:16px; font-size:13px; }

.purchase-card { position:sticky; top:24px; }
.card-inner { border:1px solid #eef2f6; padding:18px; border-radius:8px; background:linear-gradient(180deg,#fff,#fcfcff); box-shadow:0 8px 28px rgba(16,24,40,0.04); }
.title { font-size:20px; margin:0 0 12px; color:#222; line-height:1.3; }
.price-row { display:flex; align-items:center; gap:12px; margin-bottom:8px; }
.price { color:#f56c6c; font-size:28px; font-weight:700; }
.stock { color:#666; margin-bottom:12px; }

.actions { display:flex; flex-direction:column; gap:8px; margin-bottom:10px; }
.seller-tip { margin-top:8px; color:#99a0a6; font-size:12px; }

.loading-state, .error-state { display:flex; justify-content:center; align-items:center; min-height:320px; }

.dialog-image-wrap { display:flex; justify-content:center; align-items:center; padding:12px 6px; background:#111; }
@media (max-width: 768px) {
  .main-image { height:320px; }
  .thumb { width:60px; height:48px; }
  .purchase-card { position:relative; top:auto; margin-top:16px; }
}
</style>
