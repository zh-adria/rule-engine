import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

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
  plugins: [
    vue(),
    AutoImport({
      resolvers: [ElementPlusResolver()],
      imports: ['vue', 'vue-router', 'pinia'],
      dts: false
    }),
    Components({
      resolvers: [ElementPlusResolver()],
      dts: false
    })
  ],
  test: {
    environment: 'jsdom',
    globals: true,
    include: ['src/**/*.{test,spec}.{js,ts}'],
    deps: {
      inline: [/element-plus/]
    },
    css: false
  },
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
      '/approval': {
        target: 'http://localhost:9000',
        changeOrigin: true
      },
      '/rule-engine': {
        target: 'http://localhost:9000',
        changeOrigin: true
      }
    }
  }
})
