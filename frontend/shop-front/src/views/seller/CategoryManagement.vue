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
    <div class="toolbar">
      <div class="toolbar-left">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item>分类管理</el-breadcrumb-item>
        </el-breadcrumb>
      </div>

      <div class="toolbar-actions">
        <el-button @click="refresh" :loading="loading">刷新</el-button>
        <el-button type="primary" @click="createCategory">新建分类</el-button>
        <el-button type="primary" @click="toggleView">
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
            <div class="tree-node-content">
              <div class="tree-node-label">
                <span class="tree-node-name">{{ data.category_name }}</span>
                <span v-if="data.category_level === 2" class="tree-node-level">(二级)</span>
              </div>
              <div class="tree-node-actions">
                <el-button type="text" @click.stop="editCategory(data)">编辑</el-button>
                <el-button type="text" @click.stop="deleteCategory(data)">删除</el-button>
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

        <div class="pagination-wrapper">
          <div class="pagination-info">共 {{ total }} 条</div>
          <el-pagination
              background
              layout="prev, pager, next, jumper"
              :page-size="size"
              :current-page="page"
              :total="total"
              @current-change="onPageChange"
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
.category-management {
  padding: 24px;
  min-height: calc(100vh - 160px);
  position: relative;
  z-index: 1;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

:deep(.el-breadcrumb) {
  font-size: 14px;
  color: #64748b;
}

:deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
  color: #1e293b;
  font-weight: 600;
}

.toolbar-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

:deep(.toolbar-actions .el-button) {
  padding: 10px 20px;
  border-radius: 12px;
  font-weight: 600;
  font-size: 14px;
  transition: all 0.3s ease;
}

:deep(.toolbar-actions .el-button--default) {
  background: rgba(248, 250, 252, 0.95);
  border: 1px solid #e2e8f0;
  color: #475569;
}

:deep(.toolbar-actions .el-button--default:hover) {
  background: #f1f5f9;
  border-color: #cbd5e1;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

:deep(.toolbar-actions .el-button--primary) {
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  border: none;
  box-shadow: 0 4px 16px rgba(59, 130, 246, 0.3);
}

:deep(.toolbar-actions .el-button--primary:hover) {
  background: linear-gradient(135deg, #2563eb, #7c3aed);
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.4);
}

:deep(.el-card) {
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-radius: 20px;
  border: 1px solid rgba(224, 230, 237, 0.5);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

:deep(.el-card__header) {
  background: rgba(248, 250, 252, 0.95);
  border-radius: 20px 20px 0 0;
  padding: 20px 24px;
  font-weight: 600;
  color: #1e293b;
  font-size: 16px;
  border-bottom: 1px solid rgba(224, 230, 237, 0.5);
}

:deep(.el-tree) {
  background: transparent;
}

:deep(.el-tree-node__content) {
  padding: 12px 16px;
  transition: all 0.3s ease;
  border-radius: 12px;
  margin: 4px 8px;
}

:deep(.el-tree-node__content:hover) {
  background: #f8fafc;
}

:deep(.el-tree-node.is-current > .el-tree-node__content) {
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.1), rgba(139, 92, 246, 0.1));
  color: #3b82f6;
}

:deep(.el-tree-node__label) {
  font-size: 14px;
  font-weight: 500;
  color: #1e293b;
}

:deep(.el-tree .el-button) {
  padding: 4px 10px;
  font-size: 12px;
  border-radius: 8px;
  transition: all 0.2s ease;
}

:deep(.el-tree .el-button--text) {
  color: #64748b;
}

:deep(.el-tree .el-button--text:hover) {
  color: #3b82f6;
  background: rgba(59, 130, 246, 0.1);
}

:deep(.el-table) {
  border: none;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 16px;
  overflow: hidden;
}

:deep(.el-table th) {
  background: rgba(248, 250, 252, 0.95);
  color: #64748b;
  font-weight: 600;
  font-size: 13px;
  padding: 16px 20px;
  border-bottom: 1px solid #e2e8f0;
}

:deep(.el-table td) {
  padding: 16px 20px;
  border-bottom: 1px solid #f1f5f9;
}

:deep(.el-table__row:hover) {
  background: #f8fafc;
}

:deep(.el-tag) {
  border-radius: 12px;
  padding: 6px 14px;
  font-size: 12px;
  font-weight: 600;
}

:deep(.el-tag--info) {
  background: linear-gradient(135deg, #dbeafe, #bfdbfe);
  color: #2563eb;
  border: none;
}

:deep(.el-tag--success) {
  background: linear-gradient(135deg, #dcfce7, #bbf7d0);
  color: #16a34a;
  border: none;
}

:deep(.el-table .el-button--text) {
  padding: 4px 10px;
  font-size: 12px;
  border-radius: 8px;
  transition: all 0.2s ease;
  color: #64748b;
}

:deep(.el-table .el-button--text:hover) {
  color: #3b82f6;
  background: rgba(59, 130, 246, 0.1);
}

:deep(.el-pagination) {
  font-size: 13px;
}

:deep(.el-pagination .el-pager li) {
  border-radius: 8px;
  margin: 0 4px;
  padding: 4px 12px;
}

:deep(.el-pagination .el-pager li.is-active) {
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  color: white;
}

:deep(.el-pagination button) {
  border-radius: 8px;
}

.pagination-wrapper {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #f1f5f9;
}

.pagination-info {
  font-size: 14px;
  color: #64748b;
  font-weight: 500;
}

.tree-node-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 8px 0;
}

.tree-node-label {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tree-node-name {
  font-weight: 600;
  color: #1e293b;
}

.tree-node-level {
  color: #94a3b8;
  font-size: 12px;
  padding: 2px 8px;
  background: #f1f5f9;
  border-radius: 8px;
}

.tree-node-actions {
  display: flex;
  gap: 8px;
}
</style>
