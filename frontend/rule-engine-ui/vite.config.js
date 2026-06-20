import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

function vendorChunk(id) {
  if (!id.includes('node_modules')) {
    return undefined
  }
  const normalizedId = id.replace(/\\/g, '/')
  if (normalizedId.includes('/element-plus/')) {
    return 'element-plus'
  }
  if (normalizedId.includes('/lucide-vue-next/')) {
    return 'icons'
  }
  if (normalizedId.includes('/axios/')) {
    return 'http'
  }
  if (
    normalizedId.includes('/vue/') ||
    normalizedId.includes('/@vue/')
  ) {
    return 'vue-vendor'
  }
  return undefined
}

export default defineConfig({
  plugins: [vue()],
  build: {
    rollupOptions: {
      output: {
        manualChunks: vendorChunk
      }
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/rule-engine': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
