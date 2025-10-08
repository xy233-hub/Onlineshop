<template>
  <div class="home-container">
    <button class="seller-login-btn" @click="goToSellerLogin">商家登录</button>
    <div class="center-content">
      <div v-if="product" class="product-card">
        <img :src="product.image_url" alt="商品图片" />
        <h2>{{ product.product_name }}</h2>
        <p class="desc">{{ product.product_desc }}</p>
        <p class="price">价格：￥{{ product.price }}</p>
        <button v-if="product.product_status === 'online'" class="buy-btn" @click="showForm = true">我要购买</button>
        <span v-else class="unavailable">商品暂不可购买</span>
        <PurchaseForm v-if="showForm" :productId="product.product_id" @close="showForm = false" />
      </div>
      <div v-else>
        <p>暂无商品信息</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.home-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #f0f4ff 0%, #e0e7ff 100%);
  position: relative;
}
.seller-login-btn {
  position: fixed;
  top: 32px;
  right: 48px;
  z-index: 100;
  padding: 10px 28px;
  background: linear-gradient(90deg, #409eff 60%, #66b1ff 100%);
  color: #fff;
  border: none;
  border-radius: 24px;
  font-size: 16px;
  font-weight: bold;
  box-shadow: 0 2px 12px rgba(64,158,255,0.15);
  cursor: pointer;
  transition: background 0.2s;
}
.seller-login-btn:hover {
  background: linear-gradient(90deg, #66b1ff 60%, #409eff 100%);
}
.center-content {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
}
.product-card {
  background: linear-gradient(135deg, #fff 80%, #f6faff 100%);
  border: 2px solid #e3e8f7;
  border-radius: 32px;
  box-shadow: 0 8px 32px rgba(64,158,255,0.10), 0 1.5px 6px rgba(0,0,0,0.04);
  padding: 48px 56px 40px 56px;
  min-width: 380px;
  max-width: 420px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 18px;
  transition: box-shadow 0.2s;
}
.product-card img {
  width: 220px;
  height: 220px;
  object-fit: cover;
  border-radius: 16px;
  margin-bottom: 18px;
  box-shadow: 0 2px 12px rgba(64,158,255,0.08);
  border: 1.5px solid #e3e8f7;
}
.product-card h2 {
  font-size: 24px;
  font-weight: 700;
  margin: 0 0 6px 0;
  color: #222;
}
.product-card .desc {
  color: #666;
  font-size: 16px;
  text-align: center;
  margin-bottom: 8px;
}
.product-card .price {
  color: #409eff;
  font-size: 20px;
  font-weight: bold;
  margin-bottom: 12px;
}
.buy-btn {
  background: linear-gradient(90deg, #67c23a 60%, #95d475 100%);
  color: #fff;
  border: none;
  border-radius: 20px;
  padding: 10px 36px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  margin-top: 10px;
  box-shadow: 0 2px 8px rgba(103,194,58,0.10);
  transition: background 0.2s;
}
.buy-btn:hover {
  background: linear-gradient(90deg, #95d475 60%, #67c23a 100%);
}
.unavailable {
  color: #bbb;
  font-size: 15px;
  margin-top: 12px;
}
</style>


<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import PurchaseForm from '../components/PurchaseForm.vue'
import { getProduct } from '../api/product'

const product = ref(null)
const showForm = ref(false)
const router = useRouter()

const goToSellerLogin = () => {
  router.push('/seller/login')
}

onMounted(async () => {
  const res = await getProduct()
  if (res.code === 200) product.value = res.data
})
</script>


