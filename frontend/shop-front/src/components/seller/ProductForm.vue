```vue
<template>
  <el-dialog :model-value="visible" title="发布新商品" width="720px" @close="handleClose">
    <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
      <el-form-item label="商品名称" prop="product_name">
        <el-input v-model="form.product_name" maxlength="200" />
      </el-form-item>

      <el-form-item label="所属分类 (二级)" prop="category_id">
        <el-select v-model="form.category_id" placeholder="请选择分类" filterable>
          <el-option
              v-for="c in flatCategories"
              :key="c.category_id"
              :label="`${c.category_name} (${c.category_level})`"
              :value="c.category_id"
          />
        </el-select>
        <div style="color:#999; font-size:12px; margin-top:6px;">
          请确保选择二级分类，上架时后端会校验。
        </div>
      </el-form-item>

      <el-form-item label="价格 (¥)" prop="price">
        <el-input-number v-model="form.price" :min="0" :step="0.01" />
      </el-form-item>

      <el-form-item label="库存" prop="stock_quantity">
        <el-input-number v-model="form.stock_quantity" :min="0" />
      </el-form-item>

      <el-form-item label="关键字" prop="search_keywords">
        <el-input v-model="form.search_keywords" placeholder="用逗号分隔" />
      </el-form-item>

      <el-form-item label="详情描述" prop="product_desc">
        <div class="quill-editor-wrapper">
          <Editor
              v-if="Editor"
              :api-key="tinymceApiKey"
              v-model="form.product_desc"
              :init="tinymceInit"
              @update:modelValue="handleEditorModelUpdate"
              class="quill-editor"
          />
          <el-input
              v-else
              type="textarea"
              v-model="form.product_desc"
              :rows="12"
              class="desc-textarea"
          />
        </div>
      </el-form-item>

      <el-form-item label="商品图片" prop="images">
        <div style="display:flex; gap:8px; flex-wrap:wrap">
          <el-upload
              class="upload-demo"
              :http-request="customHttpRequest"
              :file-list="fileList"
              list-type="picture-card"
              :on-preview="handlePreview"
              :on-remove="handleRemove"
          >
            <i class="el-icon-plus"></i>
          </el-upload>
          <el-button v-if="fileList.length" type="text" @click="clearUploads">清空</el-button>
        </div>
        <div style="color:#999; font-size:12px; margin-top:6px;">支持多张。</div>
      </el-form-item>


    </el-form>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">提交并发布</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { sellerProductAPI, mediaAPI, categoryAPI } from '@/api'
import type { UploadFile } from 'element-plus'
import  Editor  from '@tinymce/tinymce-vue'
const tinymceApiKey = String("l8ol8rrsgk5v4unha10wmxcau3hdf40gu3y6sz23wdoadlxj")

const props = defineProps({
  visible: { type: Boolean, required: true }
})
const emit = defineEmits(['update:visible', 'created'])

const formRef = ref()
const submitting = ref(false)

// 图片/媒体上传相关
type ImageItem = { temp_key?: string; image_url?: string; image_order?: number; file_name?: string; media_type?: string; media_url?: string }
const fileList = ref<UploadFile[]>([])
const uploadingMap = ref<Record<string, boolean>>({})
const fileUidToTempKey = ref(new Map<string, string>())

// 嵌入媒体
const mediaFileList = ref<UploadFile[]>([])
const uploadingMapMedia = ref<Record<string, boolean>>({})
const fileUidToTempKeyMedia = ref(new Map<string, string>())

const rawCategories = ref<any[]>([])
const fetchCategories = async () => {
  try {
    const res = await categoryAPI.getSellerCategories({ size: 500 })
    const d = res?.data?.data ?? res?.data ?? []
    rawCategories.value = Array.isArray(d.items) ? d.items : (Array.isArray(d) ? d : [])
  } catch {
    rawCategories.value = []
  }
}
const flatCategories = computed(() => rawCategories.value)

watch(() => props.visible, v => { if (v) resetForm() })
onMounted(() => { fetchCategories() })

function resetForm() {
  form.product_name = ''
  form.product_desc = ''
  form.category_id = null
  form.price = 0
  form.stock_quantity = 0
  form.search_keywords = ''
  form.images.splice(0)
  form.media_resources.splice(0)
  fileList.value.splice(0)
  mediaFileList.value.splice(0)
  uploadingMap.value = {}
  uploadingMapMedia.value = {}
  fileUidToTempKey.value.clear()
  fileUidToTempKeyMedia.value.clear()
}
async function uploadForTinyMCE(file: File, purpose = 'embedded') {
  const fd = new FormData()
  fd.append('file', file)
  fd.append('purpose', purpose)
  fd.append('file_name', file.name)
  try {
    const res = await mediaAPI.upload(fd)
    const d = res?.data?.data ?? res?.data ?? {}
    return { url: d?.media_url || d?.mediaUrl || '', mime: d?.mime_type || file.type, tempKey: d?.temp_key || null }
  } catch (e) {
    console.error('uploadForTinyMCE error', e)
    return { url: '', mime: file.type, tempKey: null }
  }
}
/* TinyMCE 配置 */
const tinymceInit = {
  height: 360,
  menubar: false,
  plugins: [
    'link', 'lists', 'image', 'media', 'code', 'table', 'paste', 'autoresize'
  ],
  toolbar:
      'undo redo | formatselect | bold italic underline | alignleft aligncenter alignright | bullist numlist outdent indent | link image media | removeformat | code',
  tinymceScriptSrc: '/node_modules/tinymce/tinymce.min.js',
  toolbar_mode: 'wrap',
  fixed_toolbar_container: 'body',
  image_uploadtab: false,
  images_upload_handler: async (blobInfo: any, success: Function, failure: Function) => {
    try {
      const file = new File([blobInfo.blob()], blobInfo.filename())
      const fd = new FormData()
      fd.append('file', file)
      fd.append('purpose', 'embedded')
      fd.append('file_name', file.name)
      const res = await mediaAPI.upload(fd)
      const d = res?.data?.data ?? res?.data ?? res
      const url = d?.media_url || d?.mediaUrl || ''
      if (url) success(url)
      else failure('No url returned')
    } catch (e) {
      console.error('上传图片失败', e)
      failure('Upload failed')
    }
  },
  paste_as_text: false,

  /* 关键：允许通过文件选择器插入图片/媒体 */
  file_picker_types: 'file image media',
  file_picker_callback: async (callback: any, _value: any, meta: any) => {
    const input = document.createElement('input')
    input.setAttribute('type', 'file')
    if (meta.filetype === 'image') input.accept = 'image/*'
    else input.accept = 'image/*,video/*,audio/*'
    input.onchange = async () => {
      const file = input.files && input.files[0]
      if (!file) return
      try {
        const { url, mime } = await uploadForTinyMCE(file, 'embedded')
        if (!url) throw new Error('no url returned from upload')
        // image -> 使用 callback (可让 tinymce 处理 alt 等)
        if (meta.filetype === 'image') {
          callback(url, { alt: file.name })
          return
        }
        // media/file -> 直接向编辑器插入合适的标签（video/audio 或 链接）
        const ed = (window as any).tinymce?.activeEditor
        if (ed) {
          if (mime && mime.startsWith('video')) {
            ed.insertContent(`<video controls src="${url}" style="max-width:100%; display:block;"></video>`)
          } else if (mime && mime.startsWith('audio')) {
            ed.insertContent(`<audio controls src="${url}" style="display:block;"></audio>`)
          } else {
            // 其他文件类型插入下载链接
            ed.insertContent(`<a href="${url}" target="_blank" rel="noopener">${file.name}</a>`)
          }
        } else {
          // 回退：通知 tinymce 使用 url
          callback(url)
        }
      } catch (err) {
        console.error('file_picker_callback error', err)
      }
    }
    input.click()
  }
}

/* 防止把原生 DOM 事件对象写入描述 */
const isDomEvent = (v: any) => {
  if (!v || typeof v !== 'object') return false
  try { if (v instanceof Event) return true } catch (_) {}
  return v.isTrusted === true || Object.prototype.hasOwnProperty.call(v, '_vts')
}

/* 拦截 TinyMCE 的 update:modelValue，忽略事件对象 */
const handleEditorModelUpdate = (val: any) => {
  if (isDomEvent(val)) return
  if (typeof val === 'string') {
    form.product_desc = val
  } else if (val !== undefined && val !== null) {
    try { form.product_desc = String(val) } catch { /* ignore */ }
  }
}

/* 上传处理（gallery）*/
const customHttpRequest = async (options: any) => {
  const file: File = options.file
  const uid = file?.uid || String(Date.now())
  const fd = new FormData()
  fd.append('file', file)
  fd.append('purpose', 'gallery')
  try {
    uploadingMap.value[file.name] = true
    const res = await mediaAPI.upload(fd)
    const d = res?.data?.data ?? res?.data
    const tempKey = d?.temp_key || null
    const mediaUrl = d?.media_url || d?.mediaUrl || null
    if (tempKey) {
      form.images.push({ temp_key: tempKey, image_url: mediaUrl })
      fileUidToTempKey.value.set(uid, tempKey)
      options.onSuccess && options.onSuccess(d)
      ElMessage.success('图片上传成功')
    } else {
      throw new Error('no temp_key')
    }
  } catch (e) {
    options.onError && options.onError(e)
    ElMessage.error('图片上传失败')
  } finally {
    delete uploadingMap.value[file.name]
  }
}

function handleRemove(file: UploadFile) {
  const uid = (file as any).uid
  const tempKey = fileUidToTempKey.value.get(uid)
  if (tempKey) {
    const idx = form.images.findIndex(i => i.temp_key === tempKey)
    if (idx >= 0) form.images.splice(idx, 1)
    fileUidToTempKey.value.delete(uid)
  } else {
    const idx2 = form.images.findIndex(i => i.temp_key && file.name?.includes((i.temp_key).slice(0,6)))
    if (idx2 >= 0) form.images.splice(idx2, 1)
  }
}
function handlePreview(file: UploadFile) { window.open(file.url || '') }

function clearUploads() { fileList.value.splice(0); form.images.splice(0); fileUidToTempKey.value.clear() }


/* 表单与提交 */
interface ProductForm {
  product_name: string
  product_desc: string
  category_id: number | null
  price: number
  stock_quantity: number
  search_keywords: string
  images: ImageItem[]
  media_resources: ImageItem[]
}

const form = reactive<ProductForm>({
  product_name: '',
  product_desc: '',
  category_id: null,
  price: 0,
  stock_quantity: 0,
  search_keywords: '',
  images: [],
  media_resources: []
})

const rules = {
  product_name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  product_desc: [{ required: true, message: '请输入商品描述', trigger: 'change' }],
  category_id: [{ required: true, message: '请选择分类（必须为二级）', trigger: 'change' }],
  price: [{ required: true, validator: (rule: any, value: any, cb: any) => value >= 0 ? cb() : cb(new Error('价格不能为负')) , trigger: 'blur' }]
}

const handleSubmit = async () => {
  if (submitting.value) return
  formRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    const payload: any = {
      product_name: form.product_name,
      product_desc: form.product_desc,
      category_id: form.category_id,
      price: Number(form.price),
      stock_quantity: Number(form.stock_quantity),
      search_keywords: form.search_keywords || undefined,
      images: form.images.map((it, idx) => ({ temp_key: it.temp_key, image_order: idx })),
      media_resources: form.media_resources.map((it, idx) => ({ temp_key: it.temp_key, display_order: idx }))
    }
    submitting.value = true
    try {
      const res = await sellerProductAPI.createProduct(payload)
      const d = res?.data?.data ?? res?.data ?? res
      ElMessage.success('发布成功')
      emit('created', d)
      emit('update:visible', false)
      resetForm()
    } catch (e: any) {
      console.error('发布失败', e)
      ElMessage.error(e?.response?.data?.message || '发布失败')
    } finally {
      submitting.value = false
    }
  })
}

function handleClose() { emit('update:visible', false) }
</script>

<style scoped>
.upload-demo ::v-deep .el-upload {
  display:inline-block;
}

/* 编辑器 wrapper，确保宽度占满 */
.quill-editor-wrapper {
  width: 100%;

}

/* 强制让 editor 有足够高度 */
.quill-editor ::v-deep .tox-tinymce {
  min-height: 360px !important;
}

/* 文本域备用样式 */
.desc-textarea textarea {
  min-height: 360px;
  resize: vertical;
}

.tox-tinymce-aux,
.tox .tox-dialog,
.tox-tinymce{
  z-index: 100000 !important;
}
</style>
