<template>
  <div class="personal-info-page">
    <h2>个人信息</h2>
    <el-card class="info-card">
      <template #header>
        <div class="card-header">
          <span>基本信息</span>
        </div>
      </template>
      <div class="info-content">
        <el-form :model="userInfo" label-width="100px">
          <el-form-item label="用户ID">
            <el-input v-model="userInfo.customer_id" disabled />
          </el-form-item>
          <el-form-item label="登录名">
            <el-input v-model="userInfo.username" disabled />
          </el-form-item>
          <el-form-item label="电话">
            <el-input v-model="userInfo.phone" disabled />
          </el-form-item>
          <el-form-item label="注册时间">
            <el-input v-model="userInfo.created_at" disabled />
          </el-form-item>
        </el-form>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const userInfo = ref({
  customer_id: '',
  username: '',
  phone: '',
  created_at: ''
})

onMounted(() => {
  // 从 localStorage 获取用户信息
  try {
    const raw = localStorage.getItem('customer_info')
    if (raw) {
      const info = JSON.parse(raw)
      userInfo.value = {
        customer_id: info.customer_id || '',
        username: info.username || '',
        phone: info.phone || '',
        created_at: info.created_at || ''
      }
    }
  } catch (e) {
    console.error('解析用户信息失败:', e)
  }
})
</script>

<style scoped>
.personal-info-page {
  padding: 20px;
}

.info-card {
  margin-top: 20px;
}

.card-header {
  font-weight: 600;
  font-size: 16px;
}

.info-content {
  padding: 20px 0;
}
</style>