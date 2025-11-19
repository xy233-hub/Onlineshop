<!-- vue -->
<template>
  <div class="login-container">
    <el-card class="login-card">
      <template #header>
        <div class="login-header">
          <h2>卖家登录</h2>
        </div>
      </template>

      <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="loginRules"
          label-width="80px"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
              v-model="loginForm.username"
              placeholder="请输入用户名"
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              show-password
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button
            type="primary"
            @click="handleLogin"
            :loading="loading"
            style="width: 100%"
        >
          登录
        </el-button>
      </template>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useSellerStore } from '@/stores/seller'
import { authAPI } from '@/api'

const router = useRouter()
const sellerStore = useSellerStore()

// 表单模型
const loginForm = ref({
  username: '',
  password: ''
})

// 单独的表单实例引用，避免与模型变量同名冲突
const loginFormRef = ref(null)

const loading = ref(false)

const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  loading.value = true
  try {
    const response = await authAPI.login(loginForm.value)
    console.log('[auth.login] response:', response)

    // 兼容不同封装层级：response.data.data 或 response.data
    const root = response?.data
    const payload = root?.data ?? root

    // 尝试多种可能的 token 字段位置
    const token = payload?.token ?? payload?.data?.token ?? root?.token ?? null
    const seller = payload?.seller ?? payload?.seller_info ?? payload?.seller_info ?? payload?.customer_info ?? null

    if (!token) {
      console.error('[auth.login] token not found, full payload:', payload)
      ElMessage.error('登录成功但未返回 token，无法鉴权')
      return
    }

    // 规范化 token 并写入 localStorage（拦截器读取 seller_token）
    const normToken = (typeof token === 'string' ? token.trim() : String(token)).replace(/^Bearer\s+/i, '')
    localStorage.setItem('seller_token', normToken)

    // 调用 store（若 store 也会写 localStorage，则无害）
    sellerStore.login(normToken, seller)

    ElMessage.success('登录成功')
    await router.push('/seller/dashboard')
  } catch (error) {
    console.error('[auth.login] error:', error)
    ElMessage.error('登录失败，请检查用户名和密码')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
}

/* 针对 Element Plus 内部结构使用深度选择器 */
:deep(.login-card) {
  width: 480px; /* 修改宽度示例 */
}

.login-header {
  text-align: center;
}
</style>
