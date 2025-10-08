import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

export default defineConfig({
    plugins: [
        vue(),
        vueDevTools(),
    ],
    resolve: {
        alias: {
            '@': fileURLToPath(new URL('./src', import.meta.url))
        },
    },
    server: {
        proxy: {
            '/api': {
                target: 'http://localhost:8080', // 替换为你的后端地址和端口
                changeOrigin: true,
                rewrite: path => path.replace(/^\/api/, '/api')
            }
        }
    }
})

