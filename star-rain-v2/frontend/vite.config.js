import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

/*
 * 允许访问 dev server 的 Host。
 *
 * 三入口按域名分流，本地用 user.localhost / admin.localhost 验证分流，
 * 浏览器会把 *.localhost 解析到 127.0.0.1，因此这里必须显式放行；
 * 正式域名一起列上，方便用 Host 头直接测。
 */
const allowedHosts = [
  'localhost',
  'user.localhost',
  'admin.localhost',
  'yulanlin.cn',
  'www.yulanlin.cn',
  'user.yulanlin.cn',
  'admin.yulanlin.cn',
]

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5174,
    strictPort: true,
    allowedHosts,
    proxy: {
      '/api': {
        target: process.env.STAR_RAIN_API_TARGET || 'http://127.0.0.1:8088',
        /*
         * changeOrigin 必须是 false：后端按 Host 判定入口，
         * 改写 Host 会让所有本地请求都变成 127.0.0.1（公开站），
         * 于是在 user.localhost 下连注册都会被 403，本地开发直接不可用。
         */
        changeOrigin: false,
      },
    },
  },
})
