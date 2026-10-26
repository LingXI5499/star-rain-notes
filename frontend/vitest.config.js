/**
 * vitest.config.js：前端应用模块的应用配置与入口。
 * 职责范围：路由、应用装配、共享组件或样式能力。
 * 组织前端装配、构建或入口行为，具体依赖和导出入口见下文。
 * 主要依赖：vitest/config、@vitejs/plugin-vue。
 */

import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  test: {
    environment: 'happy-dom',
    environmentOptions: {
      happyDOM: {
        settings: {
          // happy-dom 21 起默认禁用 iframe 页面加载，PDF 打印测试需要真实浏览上下文。
          enableIframePageLoading: true,
        },
      },
    },
    include: ['src/**/*.spec.js'],
  },
})
