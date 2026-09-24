import { readFileSync } from 'node:fs';
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

const backend = { target: 'http://127.0.0.1:8081', changeOrigin: true };
// 接口和图标（系统生成的银行卡图标、内置机构图标）都由后端提供。
const proxy = { '/api': backend, '/icons': backend };

// 按需引入的图标路径直接从 src/lib/icons.js 读取，避免新增图标时漏配。
const iconModules = [...readFileSync(new URL('./src/lib/icons.js', import.meta.url), 'utf8')
  .matchAll(/from '(lucide-react\/[^']+)'/g)].map((match) => match[1]);

export default defineConfig({
  plugins: [react()],
  // 启动时预先打包全部依赖（含懒加载的图表），否则首次打开页面时 Vite 才发现依赖，
  // 会重新打包并强制整页刷新一次。
  optimizeDeps: {
    include: [
      'react', 'react-dom/client', '@mantine/core', '@mantine/hooks', '@mantine/notifications', '@mantine/charts',
      ...iconModules,
    ],
  },
  server: { host: '127.0.0.1', port: 5173, proxy },
  preview: { host: '127.0.0.1', port: 4173, proxy },
});
