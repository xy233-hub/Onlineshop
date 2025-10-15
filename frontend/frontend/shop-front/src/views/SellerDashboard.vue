<template>
  <div class="dashboard">
    <h2>商家功能中心</h2>
    <div class="btn-group">
      <button @click="$router.push('/seller/products')">卖家商品管理</button>
      <button @click="$router.push('/seller/account')">卖家账户</button>
      <button @click="$router.push('/seller/purchase-intents')">购买意向管理</button>
    </div>
  </div>
</template>


<style scoped>
.dashboard {
  min-height: 60vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
h2 {
  margin-bottom: 32px;
  color: #333;
}
.btn-group {
  display: flex;
  flex-direction: column;
  gap: 20px;
  width: 240px;
}
button {
  padding: 14px 0;
  font-size: 17px;
  border: none;
  border-radius: 8px;
  background: #409eff;
  color: #fff;
  cursor: pointer;
  font-weight: 500;
  transition: background 0.2s;
}
button:hover {
  background: #3076c9;
}
</style>


<script>
export default {
  name: 'SellerDashboard'
}
</script>
