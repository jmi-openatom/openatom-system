import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      // 本地开发时把 /api/v1 代理到 Spring Boot，避免浏览器跨域问题。
      '/api/v1': {
        target: 'http://localhost:8921',
        changeOrigin: true
      }
    }
  }
})
