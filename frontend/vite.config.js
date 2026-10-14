/**
 * vite.config.js：前端应用模块的应用配置与入口。
 * 职责范围：路由、应用装配、共享组件或样式能力。
 * 组织前端装配、构建或入口行为，具体依赖和导出入口见下文。
 * 注册 Vue 编译插件；开发环境的 /api 代理目标读取 STAR_RAIN_API_TARGET，否则使用本机默认后端地址。
 * 该开发代理配置不替代生产环境的 Nginx 同源反向代理。
 * 主要依赖：vite、@vitejs/plugin-vue。
 */

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { execFileSync } from 'node:child_process'
import { vditorAssets } from './vditorAssets'

const buildIdentity = {
  name: 'build-identity',
  generateBundle() {
    this.emitFile({ type: 'asset', fileName: 'build-info.json', source: JSON.stringify({
      batch: 'V2.2', commit: execFileSync('git', ['rev-parse', 'HEAD'], { encoding: 'utf8' }).trim(),
      dirty: Boolean(execFileSync('git', ['status', '--porcelain'], { encoding: 'utf8' }).trim()),
    }) })
  },
}

/*
 * 单域名 + 两条路径树之后，dev server 不再需要按 Host 分流：
 *   - server.allowedHosts 已删除：它原本是为 user.localhost / admin.localhost 这类
 *     多域名本地验证放行的，现在只有 localhost / 127.0.0.1，Vite 默认就允许；
 *   - proxy.changeOrigin 恢复为 true（引入域名方案之前的值）：
 *     它当时被改成 false，是为了让原始 Host 透传到后端做入口判定；
 *     路径方案下后端完全不读 Host，透传原始 Host 只会让后端日志与 Spring 的
 *     host/baseUrl 推导里出现前端 dev server 的端口，没有收益。
 */
export default defineConfig({
  plugins: [vue(), buildIdentity, vditorAssets()],
  server: {
    port: 5174,
    strictPort: true,
    proxy: {
      '/api': {
        target: process.env.STAR_RAIN_API_TARGET || 'http://127.0.0.1:8088',
        changeOrigin: true,
      },
    },
  },
})
