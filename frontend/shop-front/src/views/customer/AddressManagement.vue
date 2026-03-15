<template>
  <div class="address-management">
    <el-card>
      <div class="header-row">
        <h2>地址管理</h2>
        <el-button type="primary" @click="showAddAddressDialog">添加收货地址</el-button>
      </div>

      <div class="address-list" v-if="addresses.length > 0">
        <el-card
          v-for="address in addresses"
          :key="address.address_id"
          class="address-card"
          shadow="hover"
        >
          <div class="address-content">
            <div class="address-header">
              <div class="recipient-info">
                <span class="name">{{ address.recipient_name }}</span>
                <span class="phone">{{ address.recipient_phone }}</span>
                <el-tag v-if="address.is_default" type="primary" size="small">默认</el-tag>
              </div>
              <div class="address-actions">
                <el-button size="small" @click="showEditAddressDialog(address)">编辑</el-button>
                <el-button size="small" type="danger" @click="confirmDeleteAddress(address)">删除</el-button>
                <el-button 
                  v-if="!address.is_default" 
                  size="small" 
                  @click="setDefaultAddress(address.address_id)"
                >
                  设为默认
                </el-button>
              </div>
            </div>
            <div class="address-detail">
              {{ address.province }} {{ address.city }} {{ address.district }} {{ address.detail_address }}
            </div>
            <div class="address-meta">
              <span class="created-at">创建时间：{{ formatTime(address.created_at) }}</span>
            </div>
          </div>
        </el-card>
      </div>

      <div v-else class="empty-addresses">
        <el-empty description="暂无收货地址">
          <el-button type="primary" @click="showAddAddressDialog">添加收货地址</el-button>
        </el-empty>
      </div>
    </el-card>

    <!-- 添加/编辑地址对话框 -->
    <el-dialog
      v-model="addressDialogVisible"
      :title="isEditing ? '编辑收货地址' : '添加收货地址'"
      width="600px"
    >
      <el-form :model="addressForm" :rules="addressRules" ref="addressFormRef" label-width="100px">
        <el-form-item label="收货人姓名" prop="recipientName">
          <el-input v-model="addressForm.recipientName" placeholder="请输入收货人姓名"></el-input>
        </el-form-item>
        <el-form-item label="手机号码" prop="recipientPhone">
          <el-input v-model="addressForm.recipientPhone" placeholder="请输入手机号码"></el-input>
        </el-form-item>
        <el-form-item label="省份" prop="province">
          <el-input v-model="addressForm.province" placeholder="请输入省份"></el-input>
        </el-form-item>
        <el-form-item label="城市" prop="city">
          <el-input v-model="addressForm.city" placeholder="请输入城市"></el-input>
        </el-form-item>
        <el-form-item label="区县" prop="district">
          <el-input v-model="addressForm.district" placeholder="请输入区县"></el-input>
        </el-form-item>
        <el-form-item label="详细地址" prop="detailAddress">
          <el-input
            v-model="addressForm.detailAddress"
            type="textarea"
            :rows="3"
            placeholder="请输入详细地址"
          ></el-input>
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="addressForm.isDefault">设为默认地址</el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="addressDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveAddress">保存</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const addresses = ref([])
const loading = ref(false)
const addressDialogVisible = ref(false)
const isEditing = ref(false)
const addressFormRef = ref(null)
const currentAddressId = ref(null)

const addressForm = ref({
  recipientName: '',
  recipientPhone: '',
  province: '',
  city: '',
  district: '',
  detailAddress: '',
  isDefault: false
})

const addressRules = {
  recipientName: [
    { required: true, message: '请输入收货人姓名', trigger: 'blur' }
  ],
  recipientPhone: [
    { required: true, message: '请输入手机号码', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ],
  province: [
    { required: true, message: '请输入省份', trigger: 'blur' }
  ],
  city: [
    { required: true, message: '请输入城市', trigger: 'blur' }
  ],
  district: [
    { required: true, message: '请输入区县', trigger: 'blur' }
  ],
  detailAddress: [
    { required: true, message: '请输入详细地址', trigger: 'blur' }
  ]
}

const getCustomerIdFromStorage = () => {
  try {
    const raw = localStorage.getItem('customer_info') || localStorage.getItem('customer') || null
    if (raw) {
      try {
        const parsed = JSON.parse(raw)
        if (parsed && parsed.customer_id) return Number(parsed.customer_id)
        if (parsed && (parsed.id || parsed.user_id)) return Number(parsed.id || parsed.user_id)
      } catch (e) {
        const n = Number(raw)
        if (!Number.isNaN(n)) return n
      }
    }
  } catch (e) { /* ignore */ }

  const idStr = localStorage.getItem('customer_id')
  if (idStr) return Number(idStr)
  return null
}

const fetchAddresses = async () => {
  loading.value = true
  try {
    const response = await fetch('/api/customers/addresses', {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json'
      }
    })

    const result = await response.json()
    if (result.code === 200) {
      addresses.value = result.data
    } else {
      ElMessage.error(result.message || '获取地址列表失败')
    }
  } catch (error) {
    console.error('获取地址列表失败:', error)
    ElMessage.error('网络错误，请重试')
  } finally {
    loading.value = false
  }
}

const showAddAddressDialog = () => {
  isEditing.value = false
  currentAddressId.value = null
  addressForm.value = {
    recipientName: '',
    recipientPhone: '',
    province: '',
    city: '',
    district: '',
    detailAddress: '',
    isDefault: false
  }
  addressDialogVisible.value = true
}

const showEditAddressDialog = (address) => {
  isEditing.value = true
  currentAddressId.value = address.address_id
  addressForm.value = {
    recipientName: address.recipient_name,
    recipientPhone: address.recipient_phone,
    province: address.province,
    city: address.city,
    district: address.district,
    detailAddress: address.detail_address,
    isDefault: address.is_default
  }
  addressDialogVisible.value = true
}

// 解析JWT令牌
const parseJWT = (token) => {
  try {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(atob(base64).split('').map(function(c) {
      return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
    }).join(''));
    return JSON.parse(jsonPayload);
  } catch (e) {
    return null;
  }
}

const saveAddress = async () => {
  console.log('saveAddress方法被调用');
  console.log('addressForm.value:', addressForm.value);
  
  try {
    // 确保所有字段都是字符串
    const requestData = {
      recipientName: String(addressForm.value.recipientName),
      recipientPhone: String(addressForm.value.recipientPhone),
      province: String(addressForm.value.province),
      city: String(addressForm.value.city),
      district: String(addressForm.value.district),
      detailAddress: String(addressForm.value.detailAddress),
      isDefault: Boolean(addressForm.value.isDefault)
    }
    console.log('请求体:', requestData);

    let response
    if (isEditing.value && currentAddressId.value) {
      // 编辑地址
      response = await fetch(`/api/customers/addresses/${currentAddressId.value}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(requestData)
      })
    } else {
      // 添加地址
      response = await fetch('/api/customers/addresses', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(requestData)
      })
    }

    console.log('响应状态:', response.status);
    const result = await response.json()
    console.log('响应结果:', result);
    if (result.code === 200) {
      ElMessage.success(isEditing.value ? '地址更新成功' : '地址添加成功')
      addressDialogVisible.value = false
      await fetchAddresses()
    } else {
      ElMessage.error(result.message || (isEditing.value ? '地址更新失败' : '地址添加失败'))
    }
  } catch (error) {
    console.error('保存地址失败:', error)
    ElMessage.error('网络错误，请重试')
  }
}

const confirmDeleteAddress = (address) => {
  ElMessageBox.confirm(
    `确定要删除地址 ${address.recipient_name} (${address.recipient_phone}) 吗？`,
    '删除地址',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      const response = await fetch(`/api/customers/addresses/${address.address_id}`, {
        method: 'DELETE'
      })

      const result = await response.json()
      if (result.code === 200) {
        ElMessage.success('地址删除成功')
        await fetchAddresses()
      } else {
        ElMessage.error(result.message || '地址删除失败')
      }
    } catch (error) {
      console.error('删除地址失败:', error)
      ElMessage.error('网络错误，请重试')
    }
  }).catch(() => {
    // 取消删除
  })
}

const setDefaultAddress = async (addressId) => {
  try {
    const response = await fetch(`/api/customers/addresses/${addressId}/default`, {
      method: 'PATCH'
    })

    const result = await response.json()
    if (result.code === 200) {
      ElMessage.success('默认地址设置成功')
      await fetchAddresses()
    } else {
      ElMessage.error(result.message || '默认地址设置失败')
    }
  } catch (error) {
    console.error('设置默认地址失败:', error)
    ElMessage.error('网络错误，请重试')
  }
}

const formatTime = (t) => {
  if (!t) return ''
  try { return new Date(t).toLocaleString() } catch (e) { return t }
}

onMounted(() => {
  fetchAddresses()
})
</script>

<style scoped>
.address-management {
  padding: 16px;
}

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header-row h2 {
  margin: 0;
  color: #333;
}

.address-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 16px;
}

.address-card {
  padding: 16px;
}

.address-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.address-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.recipient-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.name {
  font-weight: bold;
  font-size: 16px;
}

.phone {
  color: #666;
}

.address-actions {
  display: flex;
  gap: 8px;
}

.address-detail {
  color: #333;
  line-height: 1.5;
}

.address-meta {
  color: #999;
  font-size: 12px;
}

.empty-addresses {
  padding: 40px 0;
  display: flex;
  justify-content: center;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 768px) {
  .address-list {
    grid-template-columns: 1fr;
  }
}
</style>