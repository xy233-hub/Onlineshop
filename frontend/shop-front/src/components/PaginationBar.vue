<template>
  <div class="pagination-bar">
    <el-pagination
        :current-page="localPage"
        :page-size="localSize"
        :total="total"
        :page-sizes="pageSizes"
        :layout="layout"
        @current-change="onPageChange"
        @size-change="onSizeChange"
    />
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  modelValue: { type: Number, default: 1 },      // v-model 页码
  pageSize: { type: Number, default: 8 },       // 初始每页大小
  total: { type: Number, default: 0 },          // 总条数
  pageSizes: { type: Array, default: () => [8, 12, 20, 50] },
  layout: { type: String, default: 'total, sizes, prev, pager, next, jumper' }
})
const emit = defineEmits(['update:modelValue', 'update:pageSize', 'change'])

const localPage = ref(props.modelValue)
const localSize = ref(props.pageSize)

watch(() => props.modelValue, v => (localPage.value = v))
watch(() => props.pageSize, v => (localSize.value = v))

function onPageChange(page) {
  localPage.value = page
  emit('update:modelValue', page)
  emit('change', { page, size: localSize.value })
}

function onSizeChange(size) {
  localSize.value = size
  emit('update:pageSize', size)
  // 切换每页数量时通常回到第一页
  localPage.value = 1
  emit('update:modelValue', 1)
  emit('change', { page: 1, size })
}
</script>

<style scoped>
.pagination-bar {
  display: flex;
  justify-content: center;
  padding: 12px 0;
}
</style>