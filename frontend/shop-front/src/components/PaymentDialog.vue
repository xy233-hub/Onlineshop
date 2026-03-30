<template>
  <el-dialog
    v-model="dialogVisible"
    title="订单支付"
    width="500px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div class="payment-container">
      <!-- 订单信息 -->
      <div class="order-info">
        <h4>订单信息</h4>
        <el-descriptions :column="1" size="small">
          <el-descriptions-item label="订单编号">{{ orderInfo.purchaseId }}</el-descriptions-item>
          <el-descriptions-item label="订单金额">
            <span class="amount">¥{{ orderInfo.totalAmount }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="支付截止">
            <span :class="{ 'warning': isExpiring }">{{ paymentExpiryText }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="商品信息">
            <span>{{ orderInfo.productName || '商品 #' + orderInfo.productId }}</span>
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- 支付方式选择 -->
      <div class="payment-methods">
        <h4>选择支付方式</h4>
        <el-radio-group v-model="selectedPaymentMethod" class="payment-method-group">
          <el-radio-button value="ALIPAY">
            <i class="icon-alipay"></i> 支付宝
          </el-radio-button>
          <el-radio-button value="WECHAT_PAY">
            <i class="icon-wechat"></i> 微信支付
          </el-radio-button>
          <el-radio-button value="BANK_CARD">
            <i class="icon-bank"></i> 银行卡
          </el-radio-button>
          <el-radio-button value="CREDIT_CARD">
            <i class="icon-credit"></i> 信用卡
          </el-radio-button>
        </el-radio-group>
      </div>

      <!-- 支付倒计时 -->
      <div class="countdown" v-if="showCountdown && !isPaid">
        <el-alert
          title="请尽快完成支付"
          type="warning"
          :closable="false"
          show-icon
        >
          <template #default>
            <div>剩余支付时间：<span class="countdown-timer">{{ countdownTime }}</span></div>
          </template>
        </el-alert>
      </div>
    </div>

    <template #footer>
      <span class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button 
          type="primary" 
          @click="handlePayment"
          :loading="paying"
          :disabled="isPaid || isExpired"
        >
          {{ payButtonText }}
        </el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { paymentAPI } from '@/api'

const props = defineProps({
  modelValue: Boolean,
  order: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['update:modelValue', 'payment-success'])

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const selectedPaymentMethod = ref('ALIPAY')
const paying = ref(false)
const isPaid = ref(false)
const isExpired = ref(false)
const showCountdown = ref(true)
const countdownTime = ref('30:00')
const countdownTimer = ref(null)
const paymentId = ref(null)

const paymentExpiryTime = ref(null)

const orderInfo = ref({
  purchaseId: null,
  productId: null,
  productName: null,
  totalAmount: 0
})

const extractData = (res) => res?.data?.data ?? res?.data ?? null
const apiCode = (res) => Number(res?.data?.code ?? 200)
const apiMessage = (res) => res?.data?.message || '请求失败'

const paymentExpiryText = computed(() => {
  if (!paymentExpiryTime.value) return '未知'
  const time = new Date(paymentExpiryTime.value)
  return time.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
})

const isExpiring = computed(() => {
  if (!paymentExpiryTime.value) return false
  const now = new Date().getTime()
  const expiry = new Date(paymentExpiryTime.value).getTime()
  return (expiry - now) < 5 * 60 * 1000
})

const payButtonText = computed(() => {
  if (isPaid.value) return '已支付'
  if (isExpired.value) return '已过期'
  if (paying.value) return '支付中...'
  return '立即支付'
})

watch(() => props.order, (newOrder) => {
  if (newOrder && (newOrder.purchaseId || newOrder.purchase_id)) {
    orderInfo.value = {
      purchaseId: newOrder.purchaseId || newOrder.purchase_id,
      productId: newOrder.productId || newOrder.product_id,
      productName: newOrder.productName || newOrder.product_name,
      totalAmount: newOrder.totalAmount || newOrder.total_amount || 0
    }
    initPaymentInfo()
  }
}, { immediate: true, deep: true })

onMounted(() => {
  startCountdown()
})

onUnmounted(() => {
  if (countdownTimer.value) {
    clearInterval(countdownTimer.value)
  }
})

const initPaymentInfo = async () => {
  try {
    const response = await paymentAPI.getPaymentByPurchaseId(orderInfo.value.purchaseId)
    const code = apiCode(response)
    const data = extractData(response)

    if (code === 200 && data) {
      handleExistingPayment(data)
    } else {
      await createPayment()
    }
  } catch (error) {
    console.error('初始化支付信息失败:', error)
    setDefaultExpiry()
  }
}

const setDefaultExpiry = () => {
  const defaultExpiry = new Date()
  defaultExpiry.setMinutes(defaultExpiry.getMinutes() + 30)
  paymentExpiryTime.value = defaultExpiry.toISOString()
  updateCountdown()
}

const handleExistingPayment = (payment) => {
  const status = payment.paymentStatus || payment.payment_status
  const id = payment.paymentId || payment.payment_id || payment.id
  if (id) {
    paymentId.value = id
  }

  if (status === 'PAID') {
    isPaid.value = true
    ElMessage.success('该订单已支付')
    return
  }

  if (status === 'FAILED') {
    isExpired.value = true
    ElMessage.warning('该订单支付已失败')
    return
  }

  const expiryTime = payment.paymentExpiry || payment.payment_expiry
  if (!expiryTime) {
    setDefaultExpiry()
    return
  }

  const expiryTimestamp = new Date(expiryTime).getTime()
  const nowTimestamp = new Date().getTime()
  paymentExpiryTime.value = expiryTimestamp <= nowTimestamp ? new Date(nowTimestamp + 30 * 60 * 1000).toISOString() : expiryTime
  updateCountdown()
}

const createPayment = async () => {
  try {
    const response = await paymentAPI.createPayment({
      purchaseId: orderInfo.value.purchaseId,
      amount: orderInfo.value.totalAmount
    })

    const code = apiCode(response)
    const result = extractData(response)
    if (code !== 200 || !result) {
      ElMessage.error(apiMessage(response) || '创建支付记录失败')
      setDefaultExpiry()
      return
    }

    const expiryTime = result.paymentExpiry || result.payment_expiry
    const id = result.paymentId || result.payment_id || result.id
    if (id) paymentId.value = id

    if (!expiryTime) {
      setDefaultExpiry()
      return
    }

    const expiryTimestamp = new Date(expiryTime).getTime()
    const nowTimestamp = new Date().getTime()
    paymentExpiryTime.value = expiryTimestamp <= nowTimestamp ? new Date(nowTimestamp + 30 * 60 * 1000).toISOString() : expiryTime
    updateCountdown()
    ElMessage.success('支付记录创建成功，请尽快完成支付')
  } catch (error) {
    console.error('创建支付记录失败:', error)
    ElMessage.error('网络错误，请重试')
    setDefaultExpiry()
  }
}

const startCountdown = () => {
  if (countdownTimer.value) {
    clearInterval(countdownTimer.value)
  }

  countdownTimer.value = setInterval(() => {
    updateCountdown()
  }, 1000)
}

const updateCountdown = () => {
  if (!paymentExpiryTime.value) {
    countdownTime.value = '加载中...'
    return
  }

  const now = new Date().getTime()
  const expiry = new Date(paymentExpiryTime.value).getTime()
  const diff = expiry - now

  if (diff <= 0) {
    countdownTime.value = '00:00'
    isExpired.value = true
    if (countdownTimer.value) {
      clearInterval(countdownTimer.value)
    }
    if (!isPaid.value) {
      ElMessage.warning('支付已过期，订单自动关闭')
    }
  } else {
    const minutes = Math.floor(diff / 60000)
    const seconds = Math.floor((diff % 60000) / 1000)
    countdownTime.value = `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`

    if (diff < 5 * 60 * 1000) {
      showCountdown.value = true
    }
  }
}

const submitAlipayForm = (formHtml) => {
  const parser = new DOMParser()
  const doc = parser.parseFromString(formHtml, 'text/html')
  const sourceForm = doc.querySelector('form')
  if (!sourceForm) {
    throw new Error('支付宝表单格式不正确')
  }

  const action = sourceForm.getAttribute('action') || ''
  const actionUrl = new URL(action, window.location.origin)
  const allowedHosts = ['openapi-sandbox.dl.alipaydev.com', 'openapi.alipay.com']
  if (!allowedHosts.includes(actionUrl.host)) {
    throw new Error('支付宝网关地址不在白名单内')
  }

  const form = document.createElement('form')
  form.method = (sourceForm.getAttribute('method') || 'post').toLowerCase()
  form.action = actionUrl.toString()
  form.style.display = 'none'

  sourceForm.querySelectorAll('input').forEach((node) => {
    const input = document.createElement('input')
    input.type = 'hidden'
    input.name = node.getAttribute('name') || ''
    input.value = node.getAttribute('value') || ''
    form.appendChild(input)
  })

  document.body.appendChild(form)
  form.submit()
}

const handlePayment = async () => {
  if (paying.value || isPaid.value || isExpired.value) return
  if (!paymentId.value) {
    ElMessage.warning('支付记录尚未初始化，请稍后重试')
    return
  }

  paying.value = true

  try {
    const payResponse = await paymentAPI.selectPaymentMethod(paymentId.value, {
      paymentMethod: selectedPaymentMethod.value
    })

    if (apiCode(payResponse) !== 200) {
      throw new Error(apiMessage(payResponse) || '发起支付失败')
    }

    const payData = extractData(payResponse) || {}

    if (selectedPaymentMethod.value === 'ALIPAY') {
      const payForm = payData.pay_form
      if (!payForm) {
        throw new Error('后端未返回支付宝支付表单')
      }

      sessionStorage.setItem('pending_payment_purchase_id', String(orderInfo.value.purchaseId || ''))
      sessionStorage.setItem('pending_payment_id', String(paymentId.value || ''))
      sessionStorage.setItem('pending_payment_method', 'ALIPAY')
      if (payData.out_trade_no) {
        sessionStorage.setItem('pending_out_trade_no', String(payData.out_trade_no))
      }

      ElMessage.success('正在跳转支付宝，请在新页面完成支付')
      submitAlipayForm(payForm)
      return
    }

    await simulateThirdPartyPayment()
    ElMessage.success('支付成功')
    isPaid.value = true
    emit('payment-success', orderInfo.value)

    setTimeout(() => {
      handleClose()
    }, 1000)
  } catch (error) {
    console.error('支付失败:', error)
    ElMessage.error(error?.message || '支付失败，请重试')
  } finally {
    paying.value = false
  }
}

const simulateThirdPartyPayment = async () => {
  const transactionId = 'TXN' + Date.now() + Math.random().toString(36).slice(2, 11)
  const response = await paymentAPI.paymentSuccess(paymentId.value, {
    transactionId,
    paymentMethod: selectedPaymentMethod.value
  })
  if (apiCode(response) !== 200) {
    throw new Error(apiMessage(response) || '支付确认失败')
  }
  return extractData(response)
}

const verifyPaymentResult = async () => {
  try {
    const response = await paymentAPI.verifyPayment({
      purchaseId: orderInfo.value.purchaseId
    })

    if (apiCode(response) === 200) {
      const data = extractData(response) || {}
      const status = data.paymentStatus
      if (status === 'PAID') {
        isPaid.value = true
        emit('payment-success', orderInfo.value)
        ElMessage.success('当前支付状态：支付成功')
        return
      }
      if (status === 'FAILED') {
        ElMessage.warning(data.message || '支付失败')
        return
      }
      ElMessage.info(`当前支付状态：${status || '待支付'}`)
      return
    }

    ElMessage.error(apiMessage(response) || '校验失败')
  } catch (error) {
    console.error('校验支付结果失败:', error)
    ElMessage.error('网络错误，请重试')
  }
}

const handleClose = () => {
  if (countdownTimer.value) {
    clearInterval(countdownTimer.value)
  }
  emit('update:modelValue', false)
}

defineExpose({
  verifyPaymentResult
})
</script>

<style scoped>
.payment-container {
  padding: 10px 0;
}

.payment-container h4 {
  margin-bottom: 15px;
  color: #333;
  font-size: 16px;
}

.order-info {
  margin-bottom: 25px;
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.amount {
  color: #f56c6c;
  font-size: 20px;
  font-weight: bold;
}

.payment-methods {
  margin-bottom: 25px;
}

.payment-method-group {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
}

.payment-method-group .el-radio-button {
  width: 100%;
}

.countdown {
  margin-top: 15px;
}

.countdown-timer {
  font-size: 18px;
  font-weight: bold;
  color: #f56c6c;
}

.warning {
  color: #f56c6c;
  font-weight: bold;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>