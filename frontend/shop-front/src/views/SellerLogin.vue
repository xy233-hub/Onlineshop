<template>
  <div class="seller-login">
    <div class="login-card">
      <h2>商家登录</h2>
      <form @submit.prevent="handleLogin">
        <div class="form-row">
          <label>用户名</label>
          <input v-model="username" required />
        </div>
        <div class="form-row">
          <label>密码</label>
          <input v-model="password" type="password" required />
        </div>
        <button type="submit" class="login-btn">登录</button>
        <div v-if="error" class="error">{{ error }}</div>
      </form>
    </div>
  </div>
</template>


<style scoped>
.seller-login {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f7f8fa;
}
.login-card {
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 24px rgba(0,0,0,0.10);
  padding: 36px 32px 28px 32px;
  min-width: 320px;
  display: flex;
  flex-direction: column;
  align-items: center;
}
h2 {
  margin-bottom: 24px;
  color: #333;
  font-weight: 600;
  letter-spacing: 1px;
}
.form-row {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 18px;
  width: 220px;
}
label {
  color: #666;
  font-size: 15px;
}
input {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 15px;
  outline: none;
  transition: border 0.2s;
}
input:focus {
  border-color: #409eff;
}
.login-btn {
  width: 100%;
  padding: 10px 0;
  background: #409eff;
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  margin-top: 8px;
  transition: background 0.2s;
}
.login-btn:hover {
  background: #3076c9;
}
.error {
  color: #e74c3c;
  margin-top: 16px;
  text-align: center;
  font-size: 14px;
}
</style>


<script>
export default {
  name: 'SellerLogin',
  data() {
    return {
      username: '',
      password: '',
      error: ''
    }
  },
  methods: {
    async handleLogin() {
      this.error = ''
      try {
        const res = await fetch('/api/seller/login', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            username: this.username,
            password: this.password
          })
        })
        const data = await res.json()
        if (data.code === 200) {
          localStorage.setItem('seller_token', data.data.token)
          this.$router.push('/seller/dashboard')
        } else {
          this.error = data.message
        }
      } catch (e) {
        this.error = '网络错误'
      }
    }
  }
}
</script>
