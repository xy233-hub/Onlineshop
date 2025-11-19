
<script setup>
import { ref, watch, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { purchaseAPI } from '@/api'

const props = defineProps({
  modelValue: Boolean,
  product: Object
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

// 简单校验规则
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

// 打开时尝试从 localStorage 预填客户信息
const tryPrefillCustomer = () => {
  try {
    const raw = localStorage.getItem('customer_info') || localStorage.getItem('customer')
    if (raw) {
      const info = JSON.parse(raw)
      if (info) {
        form.value.customer_name = form.value.customer_name || (info.username || info.contact_name || '')
        form.value.customer_phone = form.value.customer_phone || (info.phone || '')
        form.value.customer_address = form.value.customer_address || (info.default_address || '')
      }
    }
  } catch (e) {
    // ignore
  }
}

watch(() => props.modelValue, async (val) => {
  dialogVisible.value = val
  if (val) {
    // reset 表单并预填
    await nextTick()
    form.value.quantity = 1
    form.value.remark = ''
    // 若组件外传入 product 有默认数量或可用库存，可在此处调整
    tryPrefillCustomer()
    formRef.value?.clearValidate?.()
  }
})

watch(dialogVisible, (val) => {
  emit('update:modelValue', val)
})

const handleClose = () => {
  if (submitting.value) return
  dialogVisible.value = false
  // 清理表单（保留预填项可按需）
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
      // 尝试从 localStorage 获取 customer_id（若后端需强制）
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
      } catch (e) {
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
        // 关闭并通知父组件（返回创建的记录）
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
              :src="product?.image_url"
              style="width: 60px; height: 60px; margin-right: 10px;"
          />
          <div>
            <div class="product-name">{{ product?.product_name }}</div>
            <div class="product-price">¥{{ product?.price }}</div>
          </div>
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

      <el-form-item label="地址" prop="customer_address">
        <el-input
            v-model="form.customer_address"
            type="textarea"
            :rows="3"
            placeholder="请输入详细的收货地址"
            maxlength="255"
            show-word-limit
        />
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
  color: #f56c6c;
  font-size: 16px;
  font-weight: bold;
}
</style>
