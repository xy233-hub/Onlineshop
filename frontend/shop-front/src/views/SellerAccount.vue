<template>
  <div class="seller-account-wrapper">
    <div class="seller-account-card">
      <h2>账户管理</h2>
      <form @submit.prevent="changePassword">
        <input v-model="old_password" type="password" placeholder="原密码" required />
        <input v-model="new_password" type="password" placeholder="新密码" required />
        <button type="submit" class="submit-btn">修改密码</button>
      </form>
      <div v-if="msg" :class="msgType">{{ msg }}</div>
      <button class="back-btn" @click="$router.back()">返回</button>
    </div>
  </div>
</template>


<style scoped>
.seller-account-wrapper {
  width: 100vw;
  min-height: 100vh;
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 48px;
}
.seller-account-card {
  background: #fff;
  border-radius: 24px;
  box-shadow: 0 4px 24px rgba(0,0,0,0.08);
  padding: 40px 48px;
  min-width: 340px;
  display: flex;
  flex-direction: column;
  align-items: center;
}
form {
  display: flex;
  flex-direction: column;
  gap: 16px;
  width: 260px;
}
input {
  padding: 10px 14px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  font-size: 15px;
  outline: none;
  transition: border 0.2s;
}
input:focus {
  border-color: #409eff;
}
.submit-btn {
  background: #409eff;
  color: #fff;
  border: none;
  border-radius: 20px;
  padding: 10px 0;
  font-size: 16px;
  cursor: pointer;
  margin-top: 8px;
  transition: background 0.2s;
}
.submit-btn:hover {
  background: #66b1ff;
}
.success { color: #2ecc71; margin-top: 16px; }
.error { color: #e74c3c; margin-top: 16px; }
.back-btn {
  background: #f0f1f5;
  color: #409eff;
  border: none;
  border-radius: 20px;
  padding: 8px 24px;
  font-size: 15px;
  cursor: pointer;
  margin-top: 24px;
  transition: background 0.2s, color 0.2s;
}
.back-btn:hover {
  background: #409eff;
  color: #fff;
}
</style>


<script>
export default {
  name: 'SellerAccount',
  data() {
    return {
      old_password: '',
      new_password: '',
      msg: '',
      msgType: ''
    }
  },
  methods: {
    async changePassword() {
      this.msg = ''
      const token = localStorage.getItem('seller_token')
      const res = await fetch('/api/seller/password', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify({
          old_password: this.old_password,
          new_password: this.new_password
        })
      })
      const data = await res.json()
      if (data.code === 200) {
        this.msg = '修改成功'
        this.msgType = 'success'
        this.old_password = ''
        this.new_password = ''
      } else {
        this.msg = data.message
        this.msgType = 'error'
      }
    }
  }
}
</script>