<template>
  <div>
    <div v-if="showSuccess" class="modal-mask">
      <div class="modal-card">
        <h3>购买意向提交成功！</h3>
        <button class="primary" @click="closeSuccess">确定</button>
      </div>
    </div>
    <form class="purchase-form" @submit.prevent="submit" v-if="!showSuccess">
      <div class="form-row">
        <label>姓名</label>
        <input v-model="name" required />
      </div>
      <div class="form-row">
        <label>电话</label>
        <input v-model="phone" required />
      </div>
      <div class="form-row">
        <label>收货地址</label>
        <input v-model="address" required />
      </div>
      <div class="modal-actions">
        <button class="primary" type="submit">提交购买意向</button>
        <button class="cancel" type="button" @click="$emit('close')">取消</button>
      </div>
      <p v-if="msg" class="error">{{ msg }}</p>
    </form>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { purchaseProduct } from '../api'

const props = defineProps(['productId'])
const emit = defineEmits(['close'])
const name = ref('')
const phone = ref('')
const address = ref('')
const msg = ref('')
const showSuccess = ref(false)

const submit = async () => {
  const res = await purchaseProduct({
    customer_name: name.value,
    customer_phone: phone.value,
    customer_address: address.value,
    product_id: props.productId
  })
  if (res.code === 200) {
    showSuccess.value = true
    msg.value = ''
  } else {
    msg.value = res.message
  }
}
const closeSuccess = () => {
  showSuccess.value = false
  emit('close')
}
</script>

<style scoped>
.modal-mask {
  position: fixed;
  z-index: 99;
  left: 0; top: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.18);
  display: flex;
  align-items: center;
  justify-content: center;
}
.modal-card {
  background: #fff;
  border-radius: 18px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.13);
  padding: 32px 36px 24px 36px;
  min-width: 340px;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 16px;
  animation: popin 0.2s;
}
@keyframes popin { from { transform: scale(0.95); opacity: 0; } to { transform: scale(1); opacity: 1; } }
.modal-card h3 { margin-bottom: 8px; text-align: center; }
.purchase-form {
  background: #fff;
  border-radius: 18px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.13);
  padding: 32px 36px 24px 36px;
  min-width: 340px;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 16px;
  margin: 0 auto;
}
.form-row {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.purchase-form input {
  padding: 10px 14px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  font-size: 15px;
  outline: none;
  transition: border 0.2s;
  margin-bottom: 4px;
}
.purchase-form input:focus { border-color: #409eff; }
.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 8px;
}
.primary {
  background: #409eff;
  color: #fff;
  border: none;
  border-radius: 18px;
  padding: 8px 24px;
  font-size: 15px;
  cursor: pointer;
  transition: background 0.2s;
}
.primary:hover { background: #66b1ff; }
.cancel {
  background: #f0f1f5;
  color: #409eff;
  border: none;
  border-radius: 18px;
  padding: 8px 24px;
  font-size: 15px;
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
}
.cancel:hover { background: #409eff; color: #fff; }
.error { color: #e74c3c; margin-top: 8px; text-align: center; }
</style>

