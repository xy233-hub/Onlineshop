<!-- vue -->
<!-- File: 'src/views/customer/Favorites.vue' -->
<template>
  <div>
    <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:12px;">
      <h3>我的收藏</h3>
      <!-- 右上角按钮区域可以先留空或放别的操作 -->
      <div></div>
    </div>

    <el-table
        :data="items"
        style="width:100%"
        row-key="favorite_id"
    >
      <el-table-column prop="product_info.product_name" label="商品" />
      <el-table-column label="当前单价" width="120">
        <template #default="{ row }">
          ¥{{ row.product_info?.price ?? 0 }}
        </template>
      </el-table-column>
      <el-table-column label="查看商品" width="140" align="center">
        <template #default="{ row }">
          <el-button
              type="primary"
              link
              size="small"
              @click="goToProduct(row)"
          >
            查看商品
          </el-button>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <!-- 示例：跳转商品详情 -->
          <!-- <el-button type="primary" link @click="goDetail(row)">查看</el-button> -->
          <el-button type="danger" link @click="removeRow(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div style="display:flex; justify-content:flex-end; margin-top:12px;">
      <el-pagination
          v-if="total > 0"
          :current-page="page"
          :page-size="size"
          :total="total"
          @current-change="onPageChange"
          @size-change="onSizeChange"
          :page-sizes="[10,20,50]"
          layout="sizes, prev, pager, next, jumper"
          background
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { favoritesAPI } from '@/api'
import { useCustomerStore } from '@/stores/customer'
import { useRouter } from 'vue-router'

const store = useCustomerStore()
const router = useRouter()

const items = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const loading = ref(false)

const extractData = r => r?.data?.data ?? r?.data ?? null
const getCustomerId = () =>
    store.info?.customer_id ??
    (localStorage.getItem('customer_id') ? Number(localStorage.getItem('customer_id')) : (() => {
      const raw = localStorage.getItem('customer_info')
      if (!raw) return null
      try { const parsed = JSON.parse(raw); return Number(parsed?.customer_id) || null } catch { return null }
    })())

const fetch = async (p = page.value, s = size.value) => {
  const customerId = getCustomerId()
  if (!customerId) {
    ElMessage.error('未检测到用户信息')
    items.value = []
    total.value = 0
    return
  }
  loading.value = true
  try {
    const res = await favoritesAPI.getFavorites({ customer_id: customerId, page: p, size: s })
    const payload = extractData(res)
    let list = payload?.items ?? payload ?? []
    if (!Array.isArray(list)) list = []
    items.value = list.map(it => ({
      favorite_id: it.favorite_id ?? it.id,
      product_id: it.product_id ?? it.product?.product_id ?? it.product?.id,
      product_info: it.product_info ?? it.product ?? { product_name: it.product_name, price: it.price },
      created_at: it.created_at ?? it.created
    }))
    total.value = Number(payload?.total ?? items.value.length ?? 0)
    page.value = p
    size.value = s
  } catch (e) {
    console.error('获取收藏失败', e)
    ElMessage.error('获取收藏失败')
    items.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const onPageChange = p => { fetch(p, size.value) }
const onSizeChange = s => { fetch(1, s) }

const removeRow = async (row) => {
  const customerId = getCustomerId()
  if (!customerId) {
    ElMessage.error('请先登录客户账号')
    return
  }
  if (!row?.favorite_id) {
    ElMessage.error('记录信息不完整')
    return
  }

  try {
    const res = await favoritesAPI.removeFavorite(row.favorite_id, {
      customer_id: Number(customerId)
    })
    const code = res?.data?.code
    const msg = res?.data?.message

    if (code === 200) {
      await fetch(page.value, size.value)
      ElMessage.success(msg || '取消收藏成功')
    } else {
      ElMessage.error(msg || '取消收藏失败（接口未实现 or 返回非200）')
    }
  } catch (e) {
    console.error('取消收藏失败:', e)
    ElMessage.error(e?.response?.data?.message || '取消收藏失败，接口可能尚未实现')
  }
}

/* 新增：跳转到商品详情 */
const goToProduct = (row) => {
  const id =
      row.product_id
      || row.product_info?.product_id
      || row.product_info?.id

  if (!id) {
    return ElMessage.warning('无可跳转的商品信息')
  }
  router.push({ path: `/product/${id}` })
}

onMounted(() => {
  fetch()
})
</script>

<style scoped></style>
