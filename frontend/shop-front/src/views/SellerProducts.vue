<template>
  <div class="seller-products-wrapper">
    <div class="seller-products-card">
      <h2>商品管理</h2>
      <button @click="showAdd = true" class="add-btn">发布新商品</button>
      <div v-if="showAdd" class="modal-mask" @click.self="showAdd = false">
        <div class="modal-card">
          <h3>发布新商品</h3>
          <input v-model="newProduct.product_name" placeholder="商品名称" />
          <input v-model="newProduct.product_desc" placeholder="商品描述" />
          <input v-model="newProduct.image_url" placeholder="图片链接" />
          <input v-model.number="newProduct.price" placeholder="价格" type="number" />
          <div class="modal-actions">
            <button class="primary" @click="addProduct">提交</button>
            <button class="cancel" @click="showAdd = false">取消</button>
          </div>
        </div>
      </div>
      <table>
        <thead>
        <tr>
          <th>名称</th><th>价格</th><th>状态</th><th>操作</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="item in products" :key="item.product_id">
          <td>{{ item.product_name }}</td>
          <td>{{ item.price }}</td>
          <td>{{ item.product_status }}</td>
          <td>
            <button v-if="item.product_status==='online'" @click="freeze(item.product_id)">冻结</button>
            <button v-if="item.product_status==='frozen'" @click="unfreeze(item.product_id)">解冻</button>
            <button v-if="item.product_status!=='sold'" @click="markSold(item.product_id)">标记售出</button>
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
.seller-products-wrapper {
  width: 100vw;
  min-height: 100vh;
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
  align-items: center;
  /* 顶部留白，内容靠上 */
  padding-top: 48px;
}
.seller-products-card {
  background: #fff;
  border-radius: 24px;
  box-shadow: 0 4px 24px rgba(0,0,0,0.08);
  padding: 40px 48px;
  min-width: 700px;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.add-btn { margin-bottom: 16px; }
.add-form { margin-bottom: 16px; }
table { width: 100%; border-collapse: collapse; margin-top: 16px; }
th, td { border: 1px solid #eee; padding: 8px; text-align: center; }
.error { color: #e74c3c; margin-top: 16px; }
.back-btn {
  background: #409eff;
  color: #fff;
  border: none;
  border-radius: 20px;
  padding: 8px 24px;
  font-size: 16px;
  cursor: pointer;
  margin: 16px 0;
  transition: background 0.2s;
}
.back-btn:hover {
  background: #66b1ff;
}
.modal-mask {
  position: fixed;
  z-index: 99;
  left: 0; top: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.18);
  display: flex;
  align-items: center;
  justify-content: center;
}
.modal-card {
  background: #fff;
  border-radius: 18px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.13);
  padding: 32px 36px 24px 36px;
  min-width: 340px;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 16px;
  animation: popin 0.2s;
}
@keyframes popin { from { transform: scale(0.95); opacity: 0; } to { transform: scale(1); opacity: 1; } }
.modal-card h3 { margin-bottom: 8px; text-align: center; }
.modal-card input {
  padding: 10px 14px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  font-size: 15px;
  outline: none;
  transition: border 0.2s;
  margin-bottom: 4px;
}
.modal-card input:focus { border-color: #409eff; }
.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 8px;
}
.primary {
  background: #409eff;
  color: #fff;
  border: none;
  border-radius: 18px;
  padding: 8px 24px;
  font-size: 15px;
  cursor: pointer;
  transition: background 0.2s;
}
.primary:hover { background: #66b1ff; }
.cancel {
  background: #f0f1f5;
  color: #409eff;
  border: none;
  border-radius: 18px;
  padding: 8px 24px;
  font-size: 15px;
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
}
.cancel:hover { background: #409eff; color: #fff; }
</style>



<script>
export default {
  name: 'SellerProducts',
  data() {
    return {
      products: [],
      showAdd: false,
      newProduct: { product_name: '', product_desc: '', image_url: '', price: null },
      error: ''
    }
  },
  mounted() {
    this.fetchProducts()
  },
  methods: {
    async fetchProducts() {
      this.error = ''
      const token = localStorage.getItem('seller_token')
      const res = await fetch('/api/seller/products', {
        headers: { Authorization: `Bearer ${token}` }
      })
      const data = await res.json()
      if (data.code === 200) this.products = data.data
      else this.error = data.message
    },
    async addProduct() {
      this.error = ''
      const token = localStorage.getItem('seller_token')
      const res = await fetch('/api/seller/product', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify(this.newProduct)
      })
      const data = await res.json()
      if (data.code === 200) {
        this.showAdd = false
        this.newProduct = { product_name: '', product_desc: '', image_url: '', price: null }
        this.fetchProducts()
      } else this.error = data.message
    },
    async freeze(id) {
      const token = localStorage.getItem('seller_token')
      await fetch('/api/seller/product/freeze', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify({ product_id: id })
      })
      this.fetchProducts()
    },
    async unfreeze(id) {
      const token = localStorage.getItem('seller_token')
      const res = await fetch('/api/seller/product/unfreeze', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify({ product_id: id })
      })
      const data = await res.json()
      if (data.code === 200) {
        this.fetchProducts()
        this.error = ''
      } else if (data.code === 400 && data.message === '已有商品在售，不能解冻') {
        this.error = data.message
      } else {
        this.error = data.message
      }
    },
    async markSold(id) {
      const token = localStorage.getItem('seller_token')
      await fetch('/api/seller/product/mark-sold', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify({ product_id: id })
      })
      this.fetchProducts()
    }
  }
}
</script>

