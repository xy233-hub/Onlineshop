<template>
  <div class="alipay-return-page">
    <el-card class="return-card">
      <h2>支付宝支付结果</h2>
      <el-result :icon="resultIcon" :title="resultTitle" :sub-title="resultSubTitle">
        <template #extra>
          <el-button type="primary" @click="goOrders">返回订单列表</el-button>
          <el-button @click="retryQuery" :loading="loading">重新查询</el-button>
        </template>
      </el-result>

      <el-descriptions v-if="detail" :column="1" border size="small" class="detail-box">
        <el-descriptions-item label="支付单ID">{{ detail.payment_id || '-' }}</el-descriptions-item>
        <el-descriptions-item label="商户订单号">{{ detail.out_trade_no || '-' }}</el-descriptions-item>
        <el-descriptions-item label="支付宝交易号">{{ detail.trade_no || '-' }}</el-descriptions-item>
        <el-descriptions-item label="支付宝状态">{{ detail.trade_status || '-' }}</el-descriptions-item>
        <el-descriptions-item label="系统状态">{{ detail.payment_status || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { paymentAPI } from '@/api'

const router = useRouter()

const loading = ref(false)
const detail = ref(null)
const state = ref('pending')
const message = ref('正在查询支付状态，请稍候...')

const extractData = (res) => res?.data?.data ?? res?.data ?? null
const apiCode = (res) => Number(res?.data?.code ?? 200)
const apiMessage = (res) => res?.data?.message || '请求失败'

const resultIcon = computed(() => {
  if (state.value === 'success') return 'success'
  if (state.value === 'error') return 'error'
  return 'info'
})

const resultTitle = computed(() => {
  if (state.value === 'success') return '支付完成'
  if (state.value === 'error') return '支付未完成'
  return '支付处理中'
})

const resultSubTitle = computed(() => message.value)

const queryReturn = async () => {
  loading.value = true
  try {
    const rawQuery = window.location.search || ''
    const response = await paymentAPI.alipayReturnRaw(rawQuery)
    if (apiCode(response) !== 200) {
      state.value = 'error'
      message.value = apiMessage(response) || '支付宝回跳验签失败'
      return
    }

    const data = extractData(response) || {}
    detail.value = data

    const purchaseId = Number(sessionStorage.getItem('pending_payment_purchase_id') || 0)
    if (purchaseId > 0) {
      const verifyRes = await paymentAPI.verifyPayment({ purchaseId })
      const verifyData = extractData(verifyRes) || {}
      if ((verifyData.paymentStatus || '').toUpperCase() === 'PAID') {
        state.value = 'success'
        message.value = '订单已支付成功'
        sessionStorage.removeItem('pending_payment_purchase_id')
        sessionStorage.removeItem('pending_payment_id')
        sessionStorage.removeItem('pending_payment_method')
        sessionStorage.removeItem('pending_out_trade_no')
        return
      }
      state.value = 'pending'
      message.value = `当前支付状态：${verifyData.paymentStatus || 'PENDING'}`
      return
    }

    if ((data.payment_status || '').toUpperCase() === 'PAID') {
      state.value = 'success'
      message.value = '订单已支付成功'
    } else {
      state.value = 'pending'
      message.value = `当前支付状态：${data.payment_status || 'PENDING'}`
    }
  } catch (error) {
    console.error('查询支付宝回跳结果失败:', error)
    state.value = 'error'
    message.value = '查询失败，请稍后重试'
    ElMessage.error('查询失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const retryQuery = () => {
  queryReturn()
}

const goOrders = () => {
  router.push('/customer/dashboard/orders').catch(() => {})
}

onMounted(() => {
  queryReturn()
})
</script>

<style scoped>
.alipay-return-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: #f5f7fa;
}

.return-card {
  width: min(760px, 100%);
}

.detail-box {
  margin-top: 16px;
}
</style>
