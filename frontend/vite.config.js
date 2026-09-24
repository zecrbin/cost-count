import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

const backend = 'http://127.0.0.1:8081';

export default defineConfig({
  plugins: [react()],
  server: {
    host: '127.0.0.1',
    port: 5173,
    // 接口和账户图标都由后端提供：/icons/accounts 是生成的账户图标，/icons/default 是提供方图标
    proxy: {
      '/api': { target: backend, changeOrigin: true },
      '/icons': { target: backend, changeOrigin: true },
    },
  },
});
