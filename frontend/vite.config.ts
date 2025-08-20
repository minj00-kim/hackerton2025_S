// frontend/vite.config.ts
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    strictPort: true,                 // 5175가 점유되어 있으면 실행 자체를 막음
    proxy: { '/api': { target: 'http://localhost:5050', changeOrigin: true } }
  }
})
