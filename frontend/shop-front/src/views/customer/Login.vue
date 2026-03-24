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
  <div class="customer-login" style="max-width:420px; margin:40px auto;">
    <el-card>
      <template #header><h3>买家登录</h3></template>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" autocomplete="current-password" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleLogin">登录</el-button>
          <el-button native-type="button" @click="gotoHome" style="margin-left:8px;">取消</el-button>
          <el-button type="text" native-type="button" @click="openRegister" style="margin-left:8px;">注册</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 注册弹窗 -->
    <el-dialog v-model="showRegisterDialog" title="顾客注册" width="520px" :close-on-click-modal="false">
      <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" label-width="120px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="registerForm.username" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="registerForm.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirm">
          <el-input v-model="registerForm.confirm" type="password" show-password />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="registerForm.phone" />
        </el-form-item>
        <el-form-item label="默认地址" prop="default_address">
          <el-input v-model="registerForm.default_address" placeholder="必填" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button native-type="button" @click="showRegisterDialog = false">取消</el-button>
        <el-button type="primary" native-type="button" :loading="registerLoading" @click="handleRegister">注册并登录</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.customer-login { padding: 16px; }
</style>
