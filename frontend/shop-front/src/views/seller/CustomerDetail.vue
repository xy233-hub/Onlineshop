// src/views/seller/CustomerDetail.vue
<template>
  <div>
    <el-card>
      <div style="display:flex; justify-content:space-between; align-items:center;">
        <h3>顾客详情：{{ customerInfo.username || customerId }}</h3>
        <el-button @click="$router.back()">返回</el-button>
      </div>

      <el-descriptions :column="2" style="margin-top:16px" border>
        <el-descriptions-item label="ID">{{ customerInfo.customer_id }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ customerInfo.username }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ customerInfo.phone }}</el-descriptions-item>
        <el-descriptions-item label="默认地址">{{ customerInfo.default_address }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ customerInfo.created_at }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ customerInfo.updated_at }}</el-descriptions-item>
      </el-descriptions>

      <h4 style="margin-top:20px">购买历史</h4>
      <el-table :data="purchaseHistory" stripe style="margin-top:8px">
        <el-table-column prop="purchase_id" label="购买ID" width="100" />
        <el-table-column prop="product_id" label="商品ID" width="100" />
        <el-table-column prop="quantity" label="数量" width="80" />
        <el-table-column prop="total_amount" label="金额" width="120" />
        <el-table-column prop="purchase_status" label="状态" width="120" />
        <el-table-column prop="created_at" label="下单时间" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { sellerCustomerAPI } from '@/api'

const route = useRoute()
const customerId = Number(route.params.id)
const purchaseHistory = ref([])
const customerInfo = ref({})

const load = async () => {
  try {
    const res = await sellerCustomerAPI.getCustomerPurchaseHistory(customerId, { page: 1, size: 50 })
    const d = res.data?.data || res.data
    if (d) {
      purchaseHistory.value = d.purchase_history || []
      customerInfo.value = d.customer_info || {}
      // 避免泄露敏感字段
      if (customerInfo.value.password) delete customerInfo.value.password
    }
  } catch (e) {
    console.error(e)
  }
}

onMounted(() => load())
</script>
