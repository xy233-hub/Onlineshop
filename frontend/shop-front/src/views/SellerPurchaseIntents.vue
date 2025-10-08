<template>
  <div class="seller-purchase-intents-wrapper">
    <div class="seller-purchase-intents-card">
      <h2>购买意向管理</h2>
      <table>
        <thead>
        <tr>
          <th>意向ID</th><th>商品ID</th><th>顾客姓名</th><th>电话</th><th>地址</th><th>状态</th><th>操作</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="item in intents" :key="item.purchase_id">
          <td>{{ item.purchase_id }}</td>
          <td>{{ item.product_id }}</td>
          <td>{{ item.customer_name }}</td>
          <td>{{ item.customer_phone }}</td>
          <td>{{ item.customer_address }}</td>
          <td>
            <span :class="['status', item.purchase_status]">
              {{ item.purchase_status === 'pending' ? '待处理' : item.purchase_status === 'success' ? '成功' : '失败' }}
            </span>
          </td>
          <td>
            <button v-if="item.purchase_status==='pending'" class="success-btn" @click="updateStatus(item.purchase_id, 'success')">交易成功</button>
            <button v-if="item.purchase_status==='pending'" class="fail-btn" @click="updateStatus(item.purchase_id, 'failed')">交易失败</button>
          </td>
        </tr>
        </tbody>
      </table>
      <div v-if="error" class="error">{{ error }}</div>
      <button class="back-btn" @click="$router.back()">返回</button>
    </div>
  </div>
</template>

<style scoped>
.seller-purchase-intents-wrapper {
  width: 100vw;
  min-height: 100vh;
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 48px;
}
.seller-purchase-intents-card {
  background: #fff;
  border-radius: 24px;
  box-shadow: 0 4px 24px rgba(0,0,0,0.08);
  padding: 40px 48px;
  min-width: 900px;
  display: flex;
  flex-direction: column;
  align-items: center;
}
table { width: 100%; border-collapse: collapse; margin-top: 16px; }
th, td { border: 1px solid #eee; padding: 8px; text-align: center; }
.status { padding: 4px 12px; border-radius: 12px; font-size: 14px; }
.status.pending { background: #fffbe6; color: #e6a23c; }
.status.success { background: #e6ffed; color: #67c23a; }
.status.failed { background: #ffeaea; color: #f56c6c; }
.success-btn {
  background: #67c23a;
  color: #fff;
  border: none;
  border-radius: 16px;
  padding: 6px 18px;
  font-size: 14px;
  cursor: pointer;
  margin-right: 8px;
  transition: background 0.2s;
}
.success-btn:hover { background: #85ce61; }
.fail-btn {
  background: #f56c6c;
  color: #fff;
  border: none;
  border-radius: 16px;
  padding: 6px 18px;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.2s;
}
.fail-btn:hover { background: #fa8686; }
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
  name: 'SellerPurchaseIntents',
  data() {
    return {
      intents: [],
      error: ''
    }
  },
  mounted() {
    this.fetchIntents()
  },
  methods: {
    async fetchIntents() {
      this.error = ''
      const token = localStorage.getItem('seller_token')
      const res = await fetch('/api/seller/purchase-intents', {
        headers: { Authorization: `Bearer ${token}` }
      })
      const data = await res.json()
      if (data.code === 200) this.intents = data.data
      else this.error = data.message
    },
    async updateStatus(id, status) {
      const token = localStorage.getItem('seller_token')
      await fetch('/api/seller/purchase-intents/update-status', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify({ purchase_id: id, new_status: status })
      })
      this.fetchIntents()
    }
  }
}
</script>


