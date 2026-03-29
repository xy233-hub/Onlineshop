<script setup>
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { customerAuthAPI } from '@/api'
import { useCustomerStore } from '@/stores/customer'

const router = useRouter()
const route = useRoute()
const formRef = ref(null)
const loading = ref(false)
const customerStore = useCustomerStore()

const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  formRef.value.validate(async valid => {
    if (!valid) return
    loading.value = true
    try {
      const res = await customerAuthAPI.login({
        username: form.username,
        password: form.password
      })
      const d = res?.data?.data ?? res?.data
      const token = d?.token ?? (res?.data?.token ?? null)
      const info = d?.customer_info ?? d?.customer ?? d?.user ?? null
      if (token) {
        customerStore.login(token, info)
        ElMessage.success('登录成功')
        const redirect = route.query.redirect ?? '/dashboard'
        router.push(String(redirect))
      } else {
        ElMessage.error(res?.data?.message || '登录失败')
      }
    } catch (err) {
      ElMessage.error('登录出错')
    } finally {
      loading.value = false
    }
  })
}

const gotoHome = () => router.push('/')

/* 注册相关 */
const showRegisterDialog = ref(false)
const registerFormRef = ref(null)
const registerLoading = ref(false)
const registerForm = reactive({
  username: '',
  password: '',
  confirm: '',
  phone: '',
  default_address: ''
})

const registerRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ],
  confirm: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== registerForm.password) callback(new Error('两次输入密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
  default_address: [{ required: true, message: '请输入默认地址', trigger: 'blur' }]
}

const openRegister = (e) => {
  if (e && e.preventDefault) e.preventDefault()
  registerForm.username = ''
  registerForm.password = ''
  registerForm.confirm = ''
  registerForm.phone = ''
  registerForm.default_address = ''
  showRegisterDialog.value = true
}

const handleRegister = () => {
  registerFormRef.value.validate(async valid => {
    if (!valid) return
    registerLoading.value = true
    try {
      const payload = {
        username: registerForm.username,
        password: registerForm.password,
        phone: registerForm.phone,
        default_address: registerForm.default_address
      }
      const res = await customerAuthAPI.register(payload)
      const d = res?.data?.data ?? res?.data
      const token = d?.token ?? (res?.data?.token ?? null)
      const info = d?.customer_info ?? d?.customer ?? d?.user ?? null
      if (token) {
        customerStore.login(token, info)
        ElMessage.success('注册并已登录')
        showRegisterDialog.value = false
        const redirect = route.query.redirect ?? '/dashboard'
        router.push(String(redirect))
      } else {
        ElMessage.success('注册成功，请登录')
        showRegisterDialog.value = false
      }
    } catch (err) {
      ElMessage.error(err?.response?.data?.message || '注册失败')
    } finally {
      registerLoading.value = false
    }
  })
}
</script>

<template>
  <div class="customer-login">
    <div class="login-container">
      <div class="login-card">
        <div class="login-header">
          <h2 class="login-title">买家登录</h2>
          <p class="login-subtitle">欢迎回来，继续您的购物之旅</p>
        </div>
        <el-form :model="form" :rules="rules" ref="formRef" label-position="top" class="login-form">
          <el-form-item label="用户名" prop="username">
            <el-input 
              v-model="form.username" 
              autocomplete="username" 
              class="custom-input"
              placeholder="请输入用户名"
            />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input 
              v-model="form.password" 
              type="password" 
              autocomplete="current-password" 
              show-password 
              class="custom-input"
              placeholder="请输入密码"
            />
          </el-form-item>
          <el-form-item class="login-actions">
            <el-button type="primary" :loading="loading" @click="handleLogin" class="login-btn">
              登录
            </el-button>
            <div class="login-links">
              <el-button native-type="button" @click="gotoHome" class="cancel-btn">
                返回首页
              </el-button>
              <el-button type="text" native-type="button" @click="openRegister" class="register-btn">
                新用户注册
              </el-button>
            </div>
          </el-form-item>
        </el-form>
      </div>
    </div>

    <!-- 注册弹窗 -->
    <el-dialog 
      v-model="showRegisterDialog" 
      title="顾客注册" 
      width="520px" 
      :close-on-click-modal="false"
      class="register-dialog"
    >
      <el-form 
        ref="registerFormRef" 
        :model="registerForm" 
        :rules="registerRules" 
        label-position="top"
        class="register-form"
      >
        <el-form-item label="用户名" prop="username">
          <el-input 
            v-model="registerForm.username" 
            class="custom-input"
            placeholder="请输入用户名"
          />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input 
            v-model="registerForm.password" 
            type="password" 
            show-password 
            class="custom-input"
            placeholder="请输入密码（至少6位）"
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirm">
          <el-input 
            v-model="registerForm.confirm" 
            type="password" 
            show-password 
            class="custom-input"
            placeholder="请再次输入密码"
          />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input 
            v-model="registerForm.phone" 
            class="custom-input"
            placeholder="请输入手机号"
          />
        </el-form-item>
        <el-form-item label="默认地址" prop="default_address">
          <el-input 
            v-model="registerForm.default_address" 
            placeholder="请输入默认地址"
            class="custom-input"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button native-type="button" @click="showRegisterDialog = false" class="cancel-btn">
          取消
        </el-button>
        <el-button 
          type="primary" 
          native-type="button" 
          :loading="registerLoading" 
          @click="handleRegister"
          class="register-submit-btn"
        >
          注册并登录
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.customer-login {
  min-height: 100vh;
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  position: relative;
  overflow: hidden;
}

.customer-login::before {
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
  margin-top: 32px;
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

.login-links {
  display: flex;
  justify-content: space-between;
  margin-top: 16px;
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

.register-btn {
  color: #3b82f6;
  font-weight: 500;
  transition: all 0.3s ease;
}

.register-btn:hover {
  color: #2563eb;
  text-decoration: underline;
}

/* 注册弹窗 */
.register-dialog {
  border-radius: 24px !important;
  overflow: hidden !important;
}

.register-dialog .el-dialog__header {
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
  padding: 24px;
}

.register-dialog .el-dialog__title {
  font-size: 24px;
  font-weight: 600;
  color: #1e293b;
}

.register-dialog .el-dialog__body {
  padding: 32px 24px;
}

.register-form {
  width: 100%;
}

.register-submit-btn {
  border-radius: 12px;
  padding: 12px 24px;
  font-weight: 600;
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  border-color: transparent;
  transition: all 0.3s ease;
  box-shadow: 0 4px 16px rgba(59, 130, 246, 0.3);
}

.register-submit-btn:hover {
  background: linear-gradient(135deg, #2563eb, #7c3aed);
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.4);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .login-card {
    padding: 32px 24px;
  }
  
  .login-title {
    font-size: 28px;
  }
  
  .register-dialog {
    width: 90% !important;
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
