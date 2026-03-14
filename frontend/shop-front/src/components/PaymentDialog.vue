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
import { ref, computed, watch, onMounted, onUnmounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useCustomerStore } from '@/stores/customer';

const props = defineProps({
  modelValue: Boolean,
  order: {
    type: Object,
    required: true
  }
});

const emit = defineEmits(['update:modelValue', 'payment-success']);

const customerStore = useCustomerStore();

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

// 支付过期时间
const paymentExpiryTime = ref(null);

// 订单信息（规范化字段）
const orderInfo = ref({
  purchaseId: null,
  productId: null,
  productName: null,
  totalAmount: 0
});

// 支付过期文本
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

// 是否即将过期
const isExpiring = computed(() => {
  if (!paymentExpiryTime.value) return false;
  const now = new Date().getTime();
  const expiry = new Date(paymentExpiryTime.value).getTime();
  return (expiry - now) < 5 * 60 * 1000; // 少于 5 分钟
});

// 支付按钮文本
const payButtonText = computed(() => {
  if (isPaid.value) return '已支付';
  if (isExpired.value) return '已过期';
  if (paying.value) return '支付中...';
  return '立即支付';
});

// 监听订单变化，初始化支付信息
watch(() => props.order, (newOrder) => {
  if (newOrder && (newOrder.purchaseId || newOrder.purchase_id)) {
    // 规范化订单字段
    orderInfo.value = {
      purchaseId: newOrder.purchaseId || newOrder.purchase_id,
      productId: newOrder.productId || newOrder.product_id,
      productName: newOrder.productName || newOrder.product_name,
      totalAmount: newOrder.totalAmount || newOrder.total_amount || 0
    };
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

// 初始化支付信息
const initPaymentInfo = async () => {
  try {
    // 查询是否已有支付记录
    const response = await fetch(`/api/payments/purchase/${orderInfo.value.purchaseId}`, {
      method: 'GET',
      headers: {
        'Authorization': localStorage.getItem('customer_token')
      }
    });

    const result = await response.json();
    
    if (result.code === 200 && result.data) {
      // 已有支付记录
      handleExistingPayment(result.data);
    } else {
      // 创建新的支付记录
      await createPayment();
    }
  } catch (error) {
    console.error('初始化支付信息失败:', error);
  }
};

// 处理已存在的支付记录
const handleExistingPayment = (payment) => {
  if (payment.paymentStatus === 'PAID' || payment.payment_status === 'PAID') {
    isPaid.value = true;
    ElMessage.success('该订单已支付');
  } else if (payment.paymentStatus === 'FAILED' || payment.payment_status === 'FAILED') {
    isExpired.value = true;
    ElMessage.warning('该订单支付已失败');
  } else if (payment.paymentExpiry || payment.payment_expiry) {
    paymentExpiryTime.value = payment.paymentExpiry || payment.payment_expiry;
    paymentId.value = payment.payment_id || payment.id;
    updateCountdown();
  }
};

// 创建支付记录
const createPayment = async () => {
  try {
    const response = await fetch('/api/payments/create', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': localStorage.getItem('customer_token')
      },
      body: JSON.stringify({
        purchaseId: orderInfo.value.purchaseId,
        amount: orderInfo.value.totalAmount
      })
    });

    const result = await response.json();
    
    if (result.code === 200 && result.data) {
      paymentExpiryTime.value = result.data.paymentExpiry || result.data.payment_expiry;
      paymentId.value = result.data.payment_id || result.data.id;
      updateCountdown();
      ElMessage.success('支付记录创建成功，请尽快完成支付');
    } else {
      ElMessage.error(result.message || '创建支付记录失败');
    }
  } catch (error) {
    console.error('创建支付记录失败:', error);
    ElMessage.error('网络错误，请重试');
  }
};

// 开始倒计时
const startCountdown = () => {
  if (countdownTimer.value) {
    clearInterval(countdownTimer.value);
  }
  
  countdownTimer.value = setInterval(() => {
    updateCountdown();
  }, 1000);
};

// 更新倒计时
const updateCountdown = () => {
  if (!paymentExpiryTime.value) return;

  const now = new Date().getTime();
  const expiry = new Date(paymentExpiryTime.value).getTime();
  const diff = expiry - now;

  if (diff <= 0) {
    countdownTime.value = '00:00';
    isExpired.value = true;
    if (countdownTimer.value) {
      clearInterval(countdownTimer.value);
    }
    ElMessage.warning('支付已过期，订单自动关闭');
  } else {
    const minutes = Math.floor(diff / 60000);
    const seconds = Math.floor((diff % 60000) / 1000);
    countdownTime.value = `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
    
    // 少于 5 分钟显示警告
    if (diff < 5 * 60 * 1000) {
      showCountdown.value = true;
    }
  }
};

// ... existing code ...

// 处理支付
const handlePayment = async () => {
  if (paying.value || isPaid.value || isExpired.value) return;

  paying.value = true;

  try {
    // 先调用后端接口选择支付方式
    const payResponse = await fetch(`/api/payments/${paymentId.value}/pay`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': localStorage.getItem('customer_token')
      },
      body: JSON.stringify({
        paymentMethod: selectedPaymentMethod.value
      })
    });

    const payResult = await payResponse.json();
    
    if (payResult.code !== 200) {
      throw new Error(payResult.message || '发起支付失败');
    }

    // 模拟调用第三方支付接口
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



// 模拟第三方支付（实际项目需要替换为真实支付接口）
const simulateThirdPartyPayment = () => {
  return new Promise((resolve, reject) => {
    // 模拟支付过程
    setTimeout(async () => {
      try {
        // 生成模拟的交易号
        const transactionId = 'TXN' + Date.now() + Math.random().toString(36).substr(2, 9);
        
        // 调用后端支付成功接口 - 确保 paymentId 存在
        if (!paymentId.value) {
          throw new Error('支付 ID 不存在');
        }

        const response = await fetch(`/api/payments/${paymentId.value}/success?transactionId=${encodeURIComponent(transactionId)}&paymentMethod=${encodeURIComponent(selectedPaymentMethod.value)}`, {
          method: 'POST',
          headers: {
            'Authorization': localStorage.getItem('customer_token')
          }
        });

        const result = await response.json();
        
        if (result.code !== 200) {
          throw new Error(result.message || '支付确认失败');
        }
        
        resolve(result.data);
      } catch (error) {
        reject(error);
      }
    }, 2000); // 模拟 2 秒支付延迟
  });
};



// 手动校验支付结果
const verifyPaymentResult = async () => {
  try {
    const response = await fetch('/api/payments/verify', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': localStorage.getItem('customer_token')
      },
      body: JSON.stringify({
        purchaseId: orderInfo.value.purchaseId
      })
    });

    const result = await response.json();
    
    if (result.code === 200) {
      const status = result.data.paymentStatus;
      let message = '';
      
      if (status === 'PAID') {
        message = '支付成功';
        isPaid.value = true;
        emit('payment-success', orderInfo.value);
      } else if (status === 'FAILED') {
        message = '支付失败';
        ElMessage.warning(result.data.message || '支付失败');
        return;
      } else if (status === 'PENDING') {
        message = '待支付';
      } else {
        message = status || '未知状态';
      }
      
      ElMessage.success(`当前支付状态：${message}`);
    } else {
      ElMessage.error(result.message || '校验失败');
    }
  } catch (error) {
    console.error('校验支付结果失败:', error);
    ElMessage.error('网络错误，请重试');
  }
};

// 关闭对话框
const handleClose = () => {
  if (countdownTimer.value) {
    clearInterval(countdownTimer.value);
  }
  emit('update:modelValue', false);
};

// 暴露校验方法给父组件
defineExpose({
  verifyPaymentResult
});
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