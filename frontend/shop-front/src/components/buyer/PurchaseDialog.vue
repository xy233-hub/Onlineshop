vue
<template>
  <el-dialog
      v-model="dialogVisible"
      :title="`购买 ${product?.product_name || ''}`"
      width="500px"
      :before-close="handleClose"
  >
    <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="80px"
        v-loading="submitting"
    >
      <el-form-item label="商品信息">
        <div class="product-summary">
          <el-image
              :src="productImageUrl"
              style="width: 60px; height: 60px; margin-right: 10px;"
              fit="cover"
          >
            <template #error>
              <div style="width:60px;height:60px;display:flex;align-items:center;justify-content:center;background:#f2f3f5;color:#909399;">
                无图
              </div>
            </template>
          </el-image>
          <div>
            <div class="product-name">{{ product?.product_name }}</div>
            <div class="product-price">
              <span class="price-current">¥{{ formatPrice(displayCurrentPrice) }}</span>
              <span v-if="hasPromotion" class="price-original">¥{{ formatPrice(displayOriginalPrice) }}</span>
            </div>
          </div>
        </div>
      </el-form-item>

      <el-form-item label="优惠信息">
        <div class="promotion-box" v-loading="promotionLoading">
          <template v-if="hasPromotion">
            <div class="promotion-line">
              <span class="label">优惠后价格</span>
              <span class="value highlight">¥{{ formatPrice(displayCurrentPrice) }}</span>
            </div>
            <div class="promotion-line">
              <span class="label">原价</span>
              <span class="value">¥{{ formatPrice(displayOriginalPrice) }}</span>
            </div>
            <div class="promotion-line">
              <span class="label">累计优惠</span>
              <span class="value save">省 ¥{{ formatPrice(saveAmount) }}</span>
            </div>
            <div v-if="promotionInfo?.best_promotion" class="promotion-best">
              <div class="best-title">当前优先促销</div>
              <div class="best-content">
                活动ID: {{ promotionInfo.best_promotion.promotion_id }}，
                优惠金额: ¥{{ formatPrice(promotionInfo.best_promotion.save_amount) }}
              </div>
            </div>
            <div v-if="promotionList.length" class="promotion-list">
              <div class="best-title">当前生效促销列表</div>
              <div v-for="item in promotionList" :key="item.promotion_id" class="promotion-item">
                #{{ item.promotion_id }} {{ item.promotion_name || item.promotion_type || '促销活动' }}
                <span class="item-price">¥{{ formatPrice(item.final_price) }}</span>
              </div>
            </div>
          </template>
          <template v-else>
            <div class="no-promo">当前无可用促销，按商品原价下单。</div>
          </template>
        </div>
      </el-form-item>

      <el-form-item label="数量" prop="quantity">
        <el-input-number v-model="form.quantity" :min="1" :controls="true" />
      </el-form-item>

      <el-form-item label="姓名" prop="customer_name">
        <el-input
            v-model="form.customer_name"
            placeholder="请输入您的真实姓名"
            maxlength="50"
        />
      </el-form-item>

      <el-form-item label="电话" prop="customer_phone">
        <el-input
            v-model="form.customer_phone"
            placeholder="请输入联系电话"
            maxlength="20"
        />
      </el-form-item>

      <el-form-item label="收货地址">
        <el-select
            v-model="selectedAddressId"
            placeholder="请选择收货地址"
            style="width: 100%;"
            clearable
            :disabled="addressLoading || addresses.length === 0"
        >
          <el-option
              v-for="a in addresses"
              :key="a.address_id"
              :label="formatAddressOption(a)"
              :value="a.address_id"
          />
        </el-select>
        <div v-if="!addressLoading && addresses.length === 0" style="margin-top: 6px; color: #909399; font-size: 12px;">
          暂无地址，请添加地址后再选择（可在个人中心管理地址）
        </div>
      </el-form-item>



      <el-form-item label="备注">
        <el-input
            v-model="form.remark"
            type="textarea"
            :rows="2"
            placeholder="可选：其他特殊要求或备注"
            maxlength="200"
            show-word-limit
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="handleClose" :disabled="submitting">取消</el-button>
      <el-button
          type="primary"
          @click="handleSubmit"
          :loading="submitting"
      >
        提交购买意向
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch, nextTick, computed } from 'vue'
import { ElMessage } from 'element-plus'
import api, { purchaseAPI } from '@/api'

const props = defineProps({
  modelValue: Boolean,
  product: Object,
  promotionInfo: {
    type: Object,
    default: () => null
  },
  promotionLoading: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'success'])

const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref()

const form = ref({
  quantity: 1,
  customer_name: '',
  customer_phone: '',
  customer_address: '',
  remark: ''
})

/** 地址列表相关 **/
const addresses = ref([])
const addressLoading = ref(false)
const selectedAddressId = ref(null)

const getCustomerIdFromStorage = () => {
  try {
    const raw = localStorage.getItem('customer_info') || localStorage.getItem('customer') || null
    if (raw) {
      try {
        const parsed = JSON.parse(raw)
        if (parsed && parsed.customer_id != null) return Number(parsed.customer_id)
        if (parsed && (parsed.id != null || parsed.user_id != null)) return Number(parsed.id ?? parsed.user_id)
      } catch {
        const n = Number(raw)
        if (!Number.isNaN(n)) return n
      }
    }
  } catch { /* ignore */ }

  const idStr = localStorage.getItem('customer_id')
  if (idStr) return Number(idStr)
  return null
}

const formatAddressOption = (a) => {
  const tag = a.is_default ? '【默认】' : ''
  const head = `${tag}${a.recipient_name} ${a.recipient_phone}`
  const addr = `${a.province || ''}${a.city || ''}${a.district || ''}${a.detail_address || ''}`
  return `${head} - ${addr}`.trim()
}

const buildFullAddressText = (a) => {
  return `${a.province || ''} ${a.city || ''} ${a.district || ''} ${a.detail_address || ''}`.replace(/\s+/g, ' ').trim()
}

const pickDefaultAddressId = (list) => {
  if (!Array.isArray(list) || list.length === 0) return null
  const def = list.find(x => x && x.is_default)
  return (def?.address_id) ?? list[0]?.address_id ?? null
}

const applyAddressToForm = (a) => {
  if (!a) return
  form.value.customer_name = a.recipient_name || form.value.customer_name
  form.value.customer_phone = a.recipient_phone || form.value.customer_phone
  form.value.customer_address = buildFullAddressText(a) || form.value.customer_address
}

const fetchAddresses = async () => {
  const customerId = getCustomerIdFromStorage()
  if (!customerId) {
    addresses.value = []
    selectedAddressId.value = null
    return
  }

  addressLoading.value = true
  try {
    const { data: result } = await api.get('/customers/addresses', { params: { customer_id: customerId } })
    if (result?.code === 200) {
      const list = Array.isArray(result.data) ? result.data : []
      addresses.value = list
      selectedAddressId.value = pickDefaultAddressId(list)
      const selected = list.find(x => x.address_id === selectedAddressId.value) || null
      applyAddressToForm(selected)
    } else {
      addresses.value = []
      selectedAddressId.value = null
    }
  } catch (e) {
    console.error(e)
    addresses.value = []
    selectedAddressId.value = null
  } finally {
    addressLoading.value = false
  }
}

watch(selectedAddressId, (id) => {
  const a = addresses.value.find(x => x.address_id === id) || null
  applyAddressToForm(a)
})

/** 兼容多字段的图片地址（原逻辑保留） **/
const productImageUrl = computed(() => {
  const p = props.product
  if (!p) return ''
  if (Array.isArray(p.images) && p.images.length) {
    const first = p.images[0]
    if (typeof first === 'string') return first
    return first?.image_url || first?.media_url || first?.url || ''
  }
  if (Array.isArray(p.media_resources) && p.media_resources.length) {
    const firstImg = p.media_resources.find(m => {
      if (!m) return false
      const t = String(m.media_type || m.mime_type || '').toLowerCase()
      const u = m.media_url || m.image_url || m.url
      return t.startsWith('image') || t === 'image' || (u && /\.(png|jpe?g|gif|webp|bmp|svg)(\?.*)?$/i.test(u))
    })
    if (firstImg) return firstImg.media_url || firstImg.image_url || firstImg.url || ''
  }
  return p.image_url || p.media_url || p.imageUrl || p.cover_url || p.cover || ''
})

const toNumberPrice = (val) => {
  const n = Number(val)
  return Number.isFinite(n) ? n : null
}

const displayCurrentPrice = computed(() => {
  const p = props.promotionInfo || {}
  return toNumberPrice(p.current_price ?? p.current_promotion_price ?? props.product?.price)
})

const displayOriginalPrice = computed(() => {
  const p = props.promotionInfo || {}
  return toNumberPrice(p.original_price ?? props.product?.original_price ?? props.product?.price)
})

const hasPromotion = computed(() => {
  if (!props.promotionInfo?.has_active_promotion) return false
  if (displayCurrentPrice.value == null || displayOriginalPrice.value == null) return false
  return displayOriginalPrice.value > displayCurrentPrice.value
})

const saveAmount = computed(() => {
  if (!hasPromotion.value) return 0
  return displayOriginalPrice.value - displayCurrentPrice.value
})

const promotionList = computed(() => {
  const list = props.promotionInfo?.promotions
  return Array.isArray(list) ? list : []
})

const formatPrice = (val) => {
  const n = toNumberPrice(val)
  return n == null ? '--' : n.toFixed(2)
}

/** 校验规则（原样保留） **/
const rules = {
  quantity: [
    { required: true, message: '数量不能为空', trigger: 'blur' },
    { type: 'number', min: 1, message: '数量必须 >= 1', trigger: 'blur' }
  ],
  customer_name: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { min: 1, max: 50, message: '长度 1-50', trigger: 'blur' }
  ],
  customer_phone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^[0-9+\-()\s]{6,20}$/, message: '电话格式不正确', trigger: 'blur' }
  ],
  customer_address: [
    { required: true, message: '请输入收货地址', trigger: 'blur' },
    { min: 5, max: 255, message: '地址长度过短或过长', trigger: 'blur' }
  ]
}

watch(() => props.modelValue, async (val) => {
  dialogVisible.value = val
  if (val) {
    await nextTick()
    form.value.quantity = 1
    form.value.remark = ''
    formRef.value?.clearValidate?.()
    await fetchAddresses() // 打开即拉取并选默认地址
  }
})

watch(dialogVisible, (val) => {
  emit('update:modelValue', val)
})

const handleClose = () => {
  if (submitting.value) return
  dialogVisible.value = false
  form.value.quantity = 1
  form.value.remark = ''
  formRef.value?.clearValidate?.()
}

const handleSubmit = async () => {
  if (!formRef.value) return
  formRef.value.validate(async (valid) => {
    if (!valid) return
    if (!props.product) {
      ElMessage.error('商品信息丢失，无法提交')
      return
    }

    submitting.value = true
    try {
      let customerId = null
      try {
        const raw = localStorage.getItem('customer_info') || localStorage.getItem('customer')
        if (raw) {
          const info = JSON.parse(raw)
          customerId = info?.customer_id || info?.id || null
        }
        if (!customerId) {
          const idRaw = localStorage.getItem('customer_id')
          if (idRaw) customerId = Number(idRaw)
        }
      } catch {
        customerId = null
      }

      const payload = {
        product_id: props.product.product_id ?? props.product.id ?? null,
        quantity: Number(form.value.quantity || 1),
        customer_name: form.value.customer_name || undefined,
        customer_phone: form.value.customer_phone || undefined,
        customer_address: form.value.customer_address || undefined,
        note: form.value.remark || undefined
      }
      if (customerId) payload.customer_id = customerId

      if (!payload.product_id) {
        ElMessage.error('商品 ID 缺失，无法提交')
        submitting.value = false
        return
      }

      const res = await purchaseAPI.createPurchaseIntent(payload)
      const code = res?.data?.code
      const data = res?.data?.data
      const msg = res?.data?.message || '提交失败'

      if (code === 200) {
        ElMessage.success('提交成功')
        dialogVisible.value = false
        emit('success', data ?? null)
      } else {
        ElMessage.error(msg)
      }
    } catch (err) {
      console.error(err)
      const errMsg = err?.response?.data?.message || '提交购买意向失败'
      ElMessage.error(errMsg)
    } finally {
      submitting.value = false
    }
  })
}
</script>

<style scoped>
.product-summary {
  display: flex;
  align-items: center;
  padding: 10px;
  background: #f8f9fa;
  border-radius: 4px;
}

.product-name {
  font-weight: bold;
  margin-bottom: 5px;
}

.product-price {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.price-current {
  color: #f56c6c;
  font-size: 16px;
  font-weight: bold;
}

.price-original {
  color: #909399;
  font-size: 13px;
  text-decoration: line-through;
}

.promotion-box {
  width: 100%;
  background: #f8f9fa;
  border-radius: 6px;
  padding: 10px 12px;
}

.promotion-line {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;
}

.promotion-line .label {
  color: #606266;
}

.promotion-line .value {
  color: #303133;
}

.promotion-line .highlight {
  color: #f56c6c;
  font-weight: 700;
}

.promotion-line .save {
  color: #67c23a;
  font-weight: 600;
}

.promotion-best,
.promotion-list {
  margin-top: 8px;
  border-top: 1px dashed #dcdfe6;
  padding-top: 8px;
}

.best-title {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.best-content,
.promotion-item,
.no-promo {
  font-size: 13px;
  color: #606266;
}

.promotion-item {
  display: flex;
  justify-content: space-between;
  padding: 2px 0;
}

.item-price {
  color: #f56c6c;
}
</style>
