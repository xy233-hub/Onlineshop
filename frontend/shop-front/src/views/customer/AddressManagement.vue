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
      width="700px"
    >
      <el-form :model="addressForm" :rules="addressRules" ref="addressFormRef" label-width="100px">
        <el-form-item label="智能识别" v-if="!isEditing">
          <el-input
            v-model="addressText"
            type="textarea"
            :rows="3"
            placeholder="请输入完整地址，例如：张三 13800138000 北京市朝阳区建国路100号"
            style="margin-bottom: 10px;"
          ></el-input>
          <el-button 
            type="success" 
            @click="handleAddressParse" 
            :loading="parsing"
            style="width: 100%;"
          >
            AI智能识别地址
          </el-button>
        </el-form-item>
        <el-divider v-if="!isEditing"></el-divider>
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
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api, { addressAPI } from '@/api/index.js'

const addresses = ref([])
const loading = ref(false)
const addressDialogVisible = ref(false)
const isEditing = ref(false)
const addressFormRef = ref(null)
const currentAddressId = ref(null)
const addressText = ref('')
const parsing = ref(false)

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
  recipientName: [{ required: true, message: '请输入收货人姓名', trigger: 'blur' }],
  recipientPhone: [
    { required: true, message: '请输入手机号码', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ],
  province: [{ required: true, message: '请输入省份', trigger: 'blur' }],
  city: [{ required: true, message: '请输入城市', trigger: 'blur' }],
  district: [{ required: true, message: '请输入区县', trigger: 'blur' }],
  detailAddress: [{ required: true, message: '请输入详细地址', trigger: 'blur' }]
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
    const customerId = getCustomerIdFromStorage()
    if (!customerId) {
      ElMessage.error('未获取到客户信息，请重新登录')
      return
    }

    const { data: result } = await api.get('/customers/addresses', {
      params: { customer_id: customerId }
    })

    if (result.code === 200) {
      addresses.value = Array.isArray(result.data) ? result.data : []
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

const handleAddressParse = async () => {
  if (!addressText.value || !addressText.value.trim()) {
    ElMessage.warning('请输入要识别的地址文本')
    return
  }

  parsing.value = true
  try {
    const { data: result } = await addressAPI.parseAddress({
      address_text: addressText.value
    })

    if (result.code === 200) {
      const parsed = result.data
      addressForm.value.recipientName = parsed.recipient_name || ''
      addressForm.value.recipientPhone = parsed.recipient_phone || ''
      addressForm.value.province = parsed.province || ''
      addressForm.value.city = parsed.city || ''
      addressForm.value.district = parsed.district || ''
      addressForm.value.detailAddress = parsed.detail_address || ''
      
      ElMessage.success('地址识别成功，请检查并确认信息')
    } else {
      ElMessage.error(result.message || '地址识别失败')
    }
  } catch (error) {
    console.error('地址识别失败:', error)
    ElMessage.error('地址识别失败，请手动填写')
  } finally {
    parsing.value = false
  }
}

const showAddAddressDialog = () => {
  isEditing.value = false
  currentAddressId.value = null
  addressText.value = ''
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
  addressText.value = ''
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

const saveAddress = async () => {
  if (!addressFormRef.value) return

  try {
    await addressFormRef.value.validate()
    
    const customerId = getCustomerIdFromStorage()
    if (!customerId) {
      ElMessage.error('未获取到客户信息，请重新登录')
      return
    }

    if (isEditing.value) {
      const { data: result } = await addressAPI.updateAddress(currentAddressId.value, addressForm.value)
      if (result.code === 200) {
        ElMessage.success('地址更新成功')
        addressDialogVisible.value = false
        fetchAddresses()
      } else {
        ElMessage.error(result.message || '地址更新失败')
      }
    } else {
      const { data: result } = await addressAPI.addAddress(customerId, addressForm.value)
      if (result.code === 200) {
        ElMessage.success('地址添加成功')
        addressDialogVisible.value = false
        fetchAddresses()
      } else {
        ElMessage.error(result.message || '地址添加失败')
      }
    }
  } catch (error) {
    if (error !== false) {
      console.error('保存地址失败:', error)
      ElMessage.error('保存失败，请重试')
    }
  }
}

const confirmDeleteAddress = (address) => {
  ElMessageBox.confirm(
    `确定要删除地址「${address.province} ${address.city} ${address.district} ${address.detail_address}」吗？`,
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      const { data: result } = await addressAPI.deleteAddress(address.address_id)
      if (result.code === 200) {
        ElMessage.success('地址删除成功')
        fetchAddresses()
      } else {
        ElMessage.error(result.message || '地址删除失败')
      }
    } catch (error) {
      console.error('删除地址失败:', error)
      ElMessage.error('删除失败，请重试')
    }
  }).catch(() => {})
}

const setDefaultAddress = async (addressId) => {
  try {
    const { data: result } = await addressAPI.setDefaultAddress(addressId)
    if (result.code === 200) {
      ElMessage.success('设置默认地址成功')
      fetchAddresses()
    } else {
      ElMessage.error(result.message || '设置默认地址失败')
    }
  } catch (error) {
    console.error('设置默认地址失败:', error)
    ElMessage.error('设置失败，请重试')
  }
}

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  return date.toLocaleString('zh-CN')
}

onMounted(() => {
  fetchAddresses()
})
</script>

<style scoped>
.address-management {
  padding: 20px;
}

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header-row h2 {
  margin: 0;
}

.address-list {
  display: grid;
  gap: 16px;
}

.address-card {
  transition: all 0.3s;
}

.address-card:hover {
  transform: translateY(-2px);
}

.address-content {
  padding: 10px;
}

.address-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.recipient-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.recipient-info .name {
  font-weight: bold;
  font-size: 16px;
}

.recipient-info .phone {
  color: #606266;
}

.address-actions {
  display: flex;
  gap: 8px;
}

.address-detail {
  color: #303133;
  line-height: 1.6;
  margin-bottom: 8px;
}

.address-meta {
  color: #909399;
  font-size: 12px;
}

.empty-addresses {
  padding: 40px 0;
}
</style>