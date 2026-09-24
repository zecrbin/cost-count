import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

const backend = { target: 'http://127.0.0.1:8081', changeOrigin: true };

export default defineConfig({
  plugins: [react()],
  server: {
    host: '127.0.0.1',
    port: 5173,
    // 接口和图标（系统生成的银行卡图标、内置机构图标）都由后端提供。
    proxy: { '/api': backend, '/icons': backend },
  },
});
