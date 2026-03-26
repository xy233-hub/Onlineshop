<template>
  <el-dialog
    v-model="dialogVisible"
    title="订单支付"
    width="500px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div class="payment-container">
      <div class="order-info">
        <h4>订单信息</h4>
        <el-descriptions :column="1" size="small">
          <el-descriptions-item label="订单编号">
            {{ orderInfo.purchaseId }}
            <span v-if="multipleOrders" class="order-count">
              (共 {{ orderCount }} 个订单)
            </span>
          </el-descriptions-item>
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
import { ref, computed, watch, onMounted, onUnmounted } from 'vue';
import { ElMessage } from 'element-plus';
import api from '@/api/index';

const props = defineProps({
  modelValue: Boolean,
  order: {
    type: Object,
    required: true
  }
});

const emit = defineEmits(['update:modelValue', 'payment-success']);

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
});

const selectedPaymentMethod = ref('ALIPAY');
const paying = ref(false);
const isPaid = ref(false);
const isExpired = ref(false);
const showCountdown = ref(true);
const countdownTime = ref('30:00');
const countdownTimer = ref(null);
const paymentId = ref(null);
const paymentExpiryTime = ref(null);

const orderInfo = ref({
  purchaseId: null,
  productId: null,
  productName: null,
  totalAmount: 0
});

const multipleOrders = ref(false);
const orderCount = ref(1);

const paymentExpiryText = computed(() => {
  if (!paymentExpiryTime.value) return '未知';
  const time = new Date(paymentExpiryTime.value);
  return time.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
});

const isExpiring = computed(() => {
  if (!paymentExpiryTime.value) return false;
  const now = new Date().getTime();
  const expiry = new Date(paymentExpiryTime.value).getTime();
  return (expiry - now) < 5 * 60 * 1000;
});

const payButtonText = computed(() => {
  if (isPaid.value) return '已支付';
  if (isExpired.value) return '已过期';
  if (paying.value) return '支付中...';
  return '立即支付';
});

watch(() => props.order, (newOrder) => {
  if (newOrder && (newOrder.purchaseId || newOrder.purchase_id)) {
    orderInfo.value = {
      purchaseId: newOrder.purchaseId || newOrder.purchase_id,
      productId: newOrder.productId || newOrder.product_id,
      productName: newOrder.productName || newOrder.product_name,
      totalAmount: newOrder.totalAmount || newOrder.total_amount || 0
    };
    
    if (newOrder.purchase_ids && newOrder.purchase_ids.length > 1) {
      multipleOrders.value = true;
      orderCount.value = newOrder.purchase_ids.length;
    }
    
    if (newOrder.paymentId) {
      paymentId.value = newOrder.paymentId;
    }
    
    initPaymentInfo();
  }
}, { immediate: true, deep: true });

onMounted(() => {
  startCountdown();
});

onUnmounted(() => {
  if (countdownTimer.value) {
    clearInterval(countdownTimer.value);
  }
});

const initPaymentInfo = async () => {
  try {
    if (paymentId.value) {
      const response = await api.get(`/payments/${paymentId.value}`);
      const result = response.data;
      
      if (result.code === 200 && result.data) {
        handleExistingPayment(result.data);
        return;
      }
    }
    
    const response = await api.get(`/payments/purchase/${orderInfo.value.purchaseId}`);
    const result = response.data;
    
    if (result.code === 200 && result.data) {
      handleExistingPayment(result.data);
    } else {
      await createPayment();
    }
  } catch (error) {
    console.error('初始化支付信息失败:', error);
    const defaultExpiry = new Date();
    defaultExpiry.setMinutes(defaultExpiry.getMinutes() + 30);
    paymentExpiryTime.value = defaultExpiry.toISOString();
    updateCountdown();
  }
};

const handleExistingPayment = (payment) => {
  if (payment.paymentStatus === 'PAID' || payment.payment_status === 'PAID') {
    isPaid.value = true;
    ElMessage.success('该订单已支付');
  } else if (payment.paymentStatus === 'FAILED' || payment.payment_status === 'FAILED') {
    isExpired.value = true;
    ElMessage.warning('该订单支付已失败');
  } else if (payment.paymentExpiry || payment.payment_expiry) {
    const expiryTime = payment.paymentExpiry || payment.payment_expiry;
    const expiryTimestamp = new Date(expiryTime).getTime();
    const nowTimestamp = new Date().getTime();
    
    if (expiryTimestamp <= nowTimestamp) {
      const defaultExpiry = new Date();
      defaultExpiry.setMinutes(defaultExpiry.getMinutes() + 30);
      paymentExpiryTime.value = defaultExpiry.toISOString();
    } else {
      paymentExpiryTime.value = expiryTime;
    }
    
    if (!paymentId.value) {
      paymentId.value = payment.payment_id || payment.id;
    }
    updateCountdown();
  }
};

const createPayment = async () => {
  try {
    const response = await api.post('/payments/create', {
      purchaseId: orderInfo.value.purchaseId,
      amount: orderInfo.value.totalAmount
    });

    const result = response.data;
    
    if (result.code === 200 && result.data) {
      const expiryTime = result.data.paymentExpiry || result.data.payment_expiry;
      const expiryTimestamp = new Date(expiryTime).getTime();
      const nowTimestamp = new Date().getTime();
      
      if (expiryTimestamp <= nowTimestamp) {
        const defaultExpiry = new Date();
        defaultExpiry.setMinutes(defaultExpiry.getMinutes() + 30);
        paymentExpiryTime.value = defaultExpiry.toISOString();
      } else {
        paymentExpiryTime.value = expiryTime;
      }
      
      if (!paymentId.value) {
        paymentId.value = result.data.payment_id || result.data.id;
      }
      updateCountdown();
      ElMessage.success('支付记录创建成功，请尽快完成支付');
    } else {
      const defaultExpiry = new Date();
      defaultExpiry.setMinutes(defaultExpiry.getMinutes() + 30);
      paymentExpiryTime.value = defaultExpiry.toISOString();
      updateCountdown();
    }
  } catch (error) {
    console.error('创建支付记录失败:', error);
    const defaultExpiry = new Date();
    defaultExpiry.setMinutes(defaultExpiry.getMinutes() + 30);
    paymentExpiryTime.value = defaultExpiry.toISOString();
    updateCountdown();
  }
};

const startCountdown = () => {
  if (countdownTimer.value) {
    clearInterval(countdownTimer.value);
  }
  
  countdownTimer.value = setInterval(() => {
    updateCountdown();
  }, 1000);
};

const updateCountdown = () => {
  if (!paymentExpiryTime.value) {
    countdownTime.value = '加载中...';
    return;
  }

  const now = new Date().getTime();
  const expiry = new Date(paymentExpiryTime.value).getTime();
  const diff = expiry - now;

  if (diff <= 0) {
    countdownTime.value = '00:00';
    isExpired.value = true;
    if (countdownTimer.value) {
      clearInterval(countdownTimer.value);
    }
    if (!isPaid.value) {
      ElMessage.warning('支付已过期，订单自动关闭');
    }
  } else {
    const minutes = Math.floor(diff / 60000);
    const seconds = Math.floor((diff % 60000) / 1000);
    countdownTime.value = `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
    
    if (diff < 5 * 60 * 1000) {
      showCountdown.value = true;
    }
  }
};

const handlePayment = async () => {
  if (paying.value || isPaid.value || isExpired.value) return;

  paying.value = true;

  try {
    const payResponse = await api.post(`/payments/${paymentId.value}/pay`, {
      paymentMethod: selectedPaymentMethod.value
    });

    const payResult = payResponse.data;
    
    if (payResult.code !== 200) {
      throw new Error(payResult.message || '发起支付失败');
    }

    await simulateThirdPartyPayment();
    
    ElMessage.success('支付成功');
    isPaid.value = true;
    emit('payment-success', orderInfo.value);
    
    setTimeout(() => {
      handleClose();
    }, 1000);
  } catch (error) {
    console.error('支付失败:', error);
    ElMessage.error('支付失败，请重试');
  } finally {
    paying.value = false;
  }
};

const simulateThirdPartyPayment = () => {
  return new Promise((resolve, reject) => {
    setTimeout(async () => {
      try {
        const transactionId = 'TXN' + Date.now() + Math.random().toString(36).substr(2, 9);
        
        if (!paymentId.value) {
          throw new Error('支付 ID 不存在');
        }

        const response = await api.post(
          `/payments/${paymentId.value}/success`,
          null,
          {
            params: {
              transactionId: transactionId,
              paymentMethod: selectedPaymentMethod.value
            }
          }
        );

        const result = response.data;
        
        if (result.code !== 200) {
          throw new Error(result.message || '支付确认失败');
        }
        
        ElMessage.success(result.message || '支付成功');
        resolve(result.data);
      } catch (error) {
        reject(error);
      }
    }, 2000);
  });
};

const handleClose = () => {
  if (countdownTimer.value) {
    clearInterval(countdownTimer.value);
  }
  emit('update:modelValue', false);
};
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

.order-count {
  color: #409EFF;
  font-weight: bold;
  margin-left: 5px;
}
</style>