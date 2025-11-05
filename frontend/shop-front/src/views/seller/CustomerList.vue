// src/views/seller/CustomerList.vue
<template>
  <div>
    <el-card>
      <div style="display:flex; justify-content:space-between; align-items:center;">
        <h3>顾客列表</h3>
      </div>

      <el-table :data="customers" stripe style="margin-top:16px">
        <el-table-column prop="customer_id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="phone" label="手机号" />
        <el-table-column prop="default_address" label="默认地址" />
        <el-table-column label="最近购买">
          <template #default="{ row }">
            <span v-if="row.recent_purchases && row.recent_purchases.length">
              {{ row.recent_purchases[0].total_amount }}（{{ row.recent_purchases[0].created_at }}）
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button type="primary" size="mini" @click="viewDetail(row.customer_id)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div style="margin-top:16px; display:flex; justify-content:flex-end;">
        <el-pagination
            background
            :current-page="page"
            :page-size="size"
            :total="total"
            @current-change="onPageChange"
            layout="prev, pager, next, total">
        </el-pagination>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { sellerCustomerAPI } from '@/api'

const router = useRouter()
const customers = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)

const load = async () => {
  try {
    const res = await sellerCustomerAPI.getCustomers({ page: page.value, size: size.value })
    // 后端统一返回 ApiResponse: { code, message, data }
    const d = res.data?.data || res.data
    if (d) {
      customers.value = d.items || d.items === undefined ? (d.items || d) : d
      // 若后端返回分页结构
      total.value = d.total || (Array.isArray(customers.value) ? customers.value.length : 0)
    }
  } catch (e) {
    // 简洁处理
    console.error(e)
  }
}

onMounted(() => load())

const viewDetail = (id) => {
  router.push(`/seller/dashboard/customers/${id}`)
}

const onPageChange = (p) => {
  page.value = p
  load()
}
</script>
