import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path';

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  build: {
    outDir: '../medical-qna-system/src/main/resources/static/', 
    assetsDir: process.env.NODE_ENV === 'production' ? 'static/' : '', 
  },
  base: process.env.NODE_ENV === 'production' ? '/static/' : '/',
})
