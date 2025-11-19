<script setup>
import { ref, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { categoryAPI } from '@/api'
import CategoryEditDialog from '@/components/seller/CategoryEditDialog.vue'

const viewMode = ref('tree') // 'tree' 或 'flat'
const loading = ref(false)

// 树形数据
const treeData = ref([])
const treeProps = { children: 'children', label: 'category_name' }

// 扁平数据 + 分页
const flatItems = ref([])
const page = ref(1)
const size = ref(20)
const total = ref(0)

// 编辑对话框控制
const showEditDialog = ref(false)
const selectedCategory = ref(null)

// 父分类列表（供对话框选择）
const parents = ref([])

// 从统一响应中提取 data
const extractData = (res) => res?.data?.data ?? res?.data ?? null

const fetchTree = async () => {
  loading.value = true
  try {
    const res = await categoryAPI.getSellerCategoryTree()
    const data = extractData(res) || []
    treeData.value = Array.isArray(data) ? data : []
  } catch (e) {
    console.error('fetchTree error', e)
    treeData.value = []
  } finally {
    loading.value = false
  }
}

const fetchFlat = async (p = page.value, s = size.value) => {
  loading.value = true
  try {
    const res = await categoryAPI.getSellerCategories({ page: p, size: s })
    const data = extractData(res)
    if (!data) {
      flatItems.value = []
      total.value = 0
    } else if (Array.isArray(data.items)) {
      flatItems.value = data.items
      total.value = Number(data.total ?? data.items.length ?? 0)
      page.value = Number(data.page ?? p)
      size.value = Number(data.size ?? s)
    } else if (Array.isArray(data)) {
      flatItems.value = data
      total.value = data.length
    } else {
      flatItems.value = []
      total.value = 0
    }
  } catch (e) {
    console.error('fetchFlat error', e)
    flatItems.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 获取用于选择父分类的列表（拉取全部或大页大小）
const fetchParents = async () => {
  try {
    const res = await categoryAPI.getSellerCategories({ page: 1, size: 1000 })
    const data = extractData(res)
    let items = []
    if (!data) items = []
    else if (Array.isArray(data.items)) items = data.items
    else if (Array.isArray(data)) items = data
    else items = []
    parents.value = items.map(i => ({
      category_id: i.category_id,
      category_name: i.category_name,
      category_level: i.category_level ?? 1
    }))
  } catch (e) {
    console.error('fetchParents error', e)
    parents.value = []
  }
}

const toggleView = () => {
  viewMode.value = viewMode.value === 'tree' ? 'flat' : 'tree'
}

const refresh = () => {
  // 刷新数据并更新父分类列表
  if (viewMode.value === 'tree') fetchTree()
  else fetchFlat()
  fetchParents()
}

const onPageChange = (p) => {
  page.value = p
  fetchFlat(p, size.value)
}

/* 编辑/删除实现 */
const createCategory = () => {
  selectedCategory.value = null
  showEditDialog.value = true
}

const editCategory = (row) => {
  // 传入选中的对象给对话框（编辑）
  selectedCategory.value = row
  showEditDialog.value = true
}

const deleteCategory = async (row) => {
  try {
    await ElMessageBox.confirm(`确认删除分类「${row.category_name}」？`, '警告', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await categoryAPI.deleteCategory(row.category_id)
    ElMessage.success('删除成功')
    refresh()
  } catch (err) {
    // 取消或失败，忽略
  }
}

// 保存后的处理：关闭对话框并刷新数据 & 父分类列表
const onSaved = (savedData) => {
  showEditDialog.value = false
  ElMessage.success('保存成功')
  refresh()
}

onMounted(() => {
  if (viewMode.value === 'tree') fetchTree()
  else fetchFlat()
  fetchParents()
})

// 当切换视图时自动加载对应数据
watch(viewMode, (v) => {
  if (v === 'tree') fetchTree()
  else fetchFlat(1, size.value)
})
</script>

<template>
  <div class="category-management">
    <div class="toolbar" style="display:flex; justify-content:space-between; align-items:center; margin-bottom:12px;">
      <div>
        <el-breadcrumb separator="/">
          <el-breadcrumb-item>分类管理</el-breadcrumb-item>
        </el-breadcrumb>
      </div>

      <div style="display:flex; gap:8px; align-items:center;">
        <el-button size="small" @click="refresh" :loading="loading">刷新</el-button>
        <el-button size="small" type="primary" @click="createCategory">新建分类</el-button>
        <el-button size="small" type="primary" @click="toggleView">
          {{ viewMode === 'tree' ? '切换为扁平列表' : '切换为树形结构' }}
        </el-button>
      </div>
    </div>

    <!-- 树形视图 -->
    <div v-if="viewMode === 'tree'">
      <el-card>
        <el-tree
            :data="treeData"
            :props="treeProps"
            node-key="category_id"
            :default-expand-all="false"
            highlight-current
            accordion
            style="max-height: 560px; overflow:auto"
        >
          <template #default="{ node, data }">
            <div style="display:flex; align-items:center; justify-content:space-between; width:100%;">
              <div>
                <span style="font-weight:600">{{ data.category_name }}</span>
                <span v-if="data.category_level === 2" style="margin-left:8px; color:#909399; font-size:12px;">(二级)</span>
              </div>
              <div>
                <el-button type="text" size="mini" @click.stop="editCategory(data)">编辑</el-button>
                <el-button type="text" size="mini" @click.stop="deleteCategory(data)">删除</el-button>
              </div>
            </div>
          </template>
        </el-tree>
      </el-card>
    </div>

    <!-- 扁平列表视图 -->
    <div v-else>
      <el-card>
        <el-table
            :data="flatItems"
            style="width:100%"
            v-loading="loading"
            size="small"
        >
          <el-table-column prop="category_id" label="ID" width="80" />
          <el-table-column prop="category_name" label="名称" />
          <el-table-column label="级别" width="100">
            <template #default="{ row }">
              <el-tag size="small">{{ row.category_level === 1 ? '一级' : '二级' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="parent_id" label="父ID" width="100" />
          <el-table-column label="操作" width="160">
            <template #default="{ row }">
              <el-button type="text" size="mini" @click="editCategory(row)">编辑</el-button>
              <el-button type="text" size="mini" @click="deleteCategory(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div style="margin-top:12px; display:flex; justify-content:space-between; align-items:center;">
          <div>共 {{ total }} 条</div>
          <el-pagination
              background
              layout="prev, pager, next, jumper"
              :page-size="size"
              :current-page="page"
              :total="total"
              @current-change="onPageChange"
              small
          />
        </div>
      </el-card>
    </div>

    <!-- 合并的新建/编辑对话框 -->
    <CategoryEditDialog
        v-model="showEditDialog"
        :category="selectedCategory"
        :parents="parents"
        @saved="onSaved"
    />
  </div>
</template>

<style scoped>
.category-management { padding: 12px; }
.toolbar { margin-bottom: 12px; }
</style>
