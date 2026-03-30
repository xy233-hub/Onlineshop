<!-- vue -->
<template>
  <div class="seller-login">
    <div class="login-container">
      <div class="login-card">
        <div class="login-header">
          <h2 class="login-title">卖家登录</h2>
          <p class="login-subtitle">欢迎回来，管理您的店铺</p>
        </div>
        <el-form
            ref="loginFormRef"
            :model="loginForm"
            :rules="loginRules"
            label-position="top"
            class="login-form"
        >
          <el-form-item label="用户名" prop="username">
            <el-input
                v-model="loginForm.username"
                placeholder="请输入用户名"
                class="custom-input"
            />
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
                v-model="loginForm.password"
                type="password"
                placeholder="请输入密码"
                show-password
                class="custom-input"
            />
          </el-form-item>
        </el-form>

        <div class="login-actions">
          <el-button
              type="primary"
              @click="handleLogin"
              :loading="loading"
              class="login-btn"
          >
            登录
          </el-button>
          <el-button 
            native-type="button" 
            @click="gotoHome" 
            class="cancel-btn"
          >
            返回首页
          </el-button>
        </div>
      </div>
    </div>
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
    console.log('[auth.login] submitting:', loginForm.value)
    const response = await authAPI.login(loginForm.value)
    console.log('[auth.login] response:', response)

    // 兼容不同封装层级：response.data.data 或 response.data
    const root = response?.data
    const payload = root?.data ?? root

    // 尝试多种可能的 token 字段位置
    const token = payload?.token ?? payload?.data?.token ?? root?.token ?? null
    const seller = payload?.seller ?? payload?.seller_info ?? payload?.seller_info ?? payload?.customer_info ?? null

    console.log('[auth.login] token:', token)
    console.log('[auth.login] seller:', seller)

    if (!token) {
      console.error('[auth.login] token not found, full payload:', payload)
      ElMessage.error('登录成功但未返回 token，无法鉴权')
      return
    }

    // 规范化 token 并写入 localStorage（拦截器读取 seller_token）
    const normToken = (typeof token === 'string' ? token.trim() : String(token)).replace(/^Bearer\s+/i, '')
    localStorage.setItem('seller_token', normToken)
    console.log('[auth.login] token stored:', normToken)

    // 调用 store（若 store 也会写 localStorage，则无害）
    sellerStore.login(normToken, seller)

    ElMessage.success('登录成功')
    console.log('[auth.login] redirecting to dashboard')
    await router.push('/seller/dashboard')
  } catch (error) {
    console.error('[auth.login] error:', error)
    console.error('[auth.login] error.response:', error.response)
    ElMessage.error('登录失败，请检查用户名和密码')
  } finally {
    loading.value = false
  }
}

const gotoHome = () => router.push('/')
</script>

<style scoped>
.seller-login {
  min-height: 100vh;
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  position: relative;
  overflow: hidden;
}

.seller-login::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background-image: 
    radial-gradient(circle at 20% 30%, rgba(59, 130, 246, 0.1) 0%, transparent 50%),
    radial-gradient(circle at 80% 70%, rgba(139, 92, 246, 0.1) 0%, transparent 50%);
  animation: float 6s ease-in-out infinite;
  pointer-events: none;
  z-index: 0;
}

@keyframes float {
  0% {
    transform: translateY(0) rotate(0deg);
  }
  50% {
    transform: translateY(-10px) rotate(5deg);
  }
  100% {
    transform: translateY(0) rotate(0deg);
  }
}

.login-container {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 480px;
}

.login-card {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 24px;
  padding: 40px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(224, 230, 237, 0.5);
  backdrop-filter: blur(12px);
  transition: all 0.3s ease;
}

.login-card:hover {
  box-shadow: 0 24px 48px rgba(0, 0, 0, 0.15);
  transform: translateY(-5px);
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.login-title {
  font-size: 32px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 8px 0;
  background: linear-gradient(135deg, #1e293b, #475569);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.login-subtitle {
  font-size: 16px;
  color: #64748b;
  margin: 0;
}

.login-form {
  width: 100%;
  margin-bottom: 24px;
}

.custom-input {
  border-radius: 12px;
  border: 2px solid #e2e8f0;
  transition: all 0.3s ease;
  font-size: 16px;
  padding: 12px 16px;
}

.custom-input:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.15);
}

.login-actions {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.login-btn {
  width: 100%;
  border-radius: 12px;
  padding: 14px 0;
  font-size: 16px;
  font-weight: 600;
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  border-color: transparent;
  transition: all 0.3s ease;
  box-shadow: 0 4px 16px rgba(59, 130, 246, 0.3);
}

.login-btn:hover {
  background: linear-gradient(135deg, #2563eb, #7c3aed);
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.4);
}

.cancel-btn {
  border-radius: 8px;
  transition: all 0.3s ease;
  color: #64748b;
  border-color: #e2e8f0;
  background: #f8fafc;
}

.cancel-btn:hover {
  border-color: #3b82f6;
  color: #3b82f6;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.2);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .login-card {
    padding: 32px 24px;
  }
  
  .login-title {
    font-size: 28px;
  }
}

@media (max-width: 480px) {
  .login-card {
    padding: 24px 20px;
  }
  
  .login-title {
    font-size: 24px;
  }
  
  .custom-input {
    font-size: 14px;
    padding: 10px 14px;
  }
  
  .login-btn {
    padding: 12px 0;
    font-size: 14px;
  }
}
</style>
