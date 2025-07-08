import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },

  build: {
    outDir: '../medical-qna-system/src/main/resources/static',
    emptyOutDir: true, 
  },
  base: './',


  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8000',
        changeOrigin: true,
        // rewrite: path => path.replace(/^\/api/, '/api'), // 通常不需要重写
      }
    }
  }
})
