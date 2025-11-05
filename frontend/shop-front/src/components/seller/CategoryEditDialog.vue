```vue
<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { categoryAPI } from '@/api'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  category: { type: Object, default: null },
  parents: { type: Array, default: () => [] }
})
const emit = defineEmits(['update:modelValue', 'saved'])

const visibleLocal = ref(props.modelValue)
watch(() => props.modelValue, v => (visibleLocal.value = v))
watch(visibleLocal, v => emit('update:modelValue', v))

const formRef = ref(null)
const form = ref({
  category_name: '',
  parent_id: null
})
const loading = ref(false)

const isEdit = computed(() => !!(props.category && props.category.category_id))
const originalId = computed(() => props.category?.category_id ?? null)

const rules = {
  category_name: [
    { required: true, message: '请输入分类名称', trigger: 'blur' },
    { min: 1, max: 64, message: '长度 1-64 个字符', trigger: 'blur' }
  ]
}

const resetForm = () => {
  if (isEdit.value) {
    form.value.category_name = props.category.category_name ?? ''
    form.value.parent_id = props.category.parent_id ?? null
  } else {
    form.value.category_name = ''
    form.value.parent_id = null
  }
  formRef.value && formRef.value.clearValidate()
}

watch(() => props.category, () => {
  if (visibleLocal.value) resetForm()
})

watch(() => visibleLocal.value, v => {
  if (v) resetForm()
})

const onClose = () => {
  visibleLocal.value = false
  // 显式通知父组件（watch 也会触发，但这里确保同步）
  emit('update:modelValue', false)
}

const onSubmit = () => {
  formRef.value.validate(async valid => {
    if (!valid) return
    loading.value = true
    try {
      let res
      const payload = {
        parent_id: form.value.parent_id,
        category_name: form.value.category_name
      }
      if (isEdit.value) {
        res = await categoryAPI.updateCategory(props.category.category_id, payload)
      } else {
        res = await categoryAPI.createCategory(payload)
      }
      const data = res?.data?.data ?? res?.data
      ElMessage.success('保存成功')
      emit('saved', data)
      onClose()
    } catch (e) {
      console.error(e)
      ElMessage.error('保存失败，请重试')
    } finally {
      loading.value = false
    }
  })
}
</script>

<template>
  <el-dialog
      :title="isEdit ? '编辑分类' : '新建分类'"
      v-model="visibleLocal"
      width="480px"
      @close="onClose"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
      <el-form-item label="父分类" prop="parent_id">
        <el-select v-model="form.parent_id" placeholder="请选择父分类（可选）" clearable>
          <el-option :label="'— 无父级 —'" :value="null" />
          <el-option
              v-for="p in parents"
              :key="p.category_id"
              :label="`${p.category_name} (${p.category_level === 1 ? '一级' : '二级'})`"
              :value="p.category_id"
              :disabled="p.category_level === 2 && isEdit && p.category_id === originalId"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="分类名称" prop="category_name">
        <el-input v-model="form.category_name" maxlength="64" show-word-limit />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="onClose">取消</el-button>
      <el-button type="primary" :loading="loading" @click="onSubmit">{{ isEdit ? '保存' : '创建' }}</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.el-dialog__body { padding-top: 6px; }
</style>
