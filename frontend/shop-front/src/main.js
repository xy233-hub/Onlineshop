// javascript
// 文件：`main.js`
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './assets/tinymce-fix.css'

import App from '@/App.vue'
import router from '@/router'
import PaginationBar from '@/components/PaginationBar.vue'
import {QuillEditor } from '@vueup/vue-quill'
import '@vueup/vue-quill/dist/vue-quill.snow.css'
// 创建并挂载应用
const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus)
app.use(QuillEditor)
// 注册全局组件（如果需要）
app.component('PaginationBar', PaginationBar)
app.component('QuillEditor', QuillEditor)
app.mount('#app')
