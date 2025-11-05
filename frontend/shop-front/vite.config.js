import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'
import path from 'path'

export default defineConfig({
    plugins: [
        vue(),
        vueDevTools(),
    ],
    resolve: {
        alias: {
            '@': fileURLToPath(new URL('./src', import.meta.url)),
            // 强制使用项目根目录 node_modules 中的 quill，避免重复副本
            quill: path.resolve(__dirname, 'node_modules/quill')
        }
    },
    optimizeDeps: {
        // 确保 quill 在 dev 预打包中被处理，减少运行时导入顺序问题
        include: ['quill']
    },
    server: {
        proxy: {
            '/api': {
                target: 'http://localhost:8080',
                changeOrigin: true,
                rewrite: path => path.replace(/^\/api/, '/api')
            }
        }
    }
})
