import { fileURLToPath, URL } from 'node:url'

import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { visualizer } from 'rollup-plugin-visualizer'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const apiTarget = env.VITE_API_TARGET || 'http://localhost:24680'
  const analyze = mode === 'analyze' || env.ANALYZE === '1'

  return {
    plugins: [
      vue(),
      analyze
        ? visualizer({
            filename: 'dist/stats.html',
            gzipSize: true,
            brotliSize: true,
            open: false,
          })
        : null,
    ].filter(Boolean),
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },
    server: {
      port: 5173,
      proxy: {
        '/api': {
          target: apiTarget,
          changeOrigin: true,
        },
        '/actuator': {
          target: apiTarget,
          changeOrigin: true,
        },
        // Public media: dev server must forward /uploads to the backend
        // (the backend serves the local storage dir; nginx does it in prod).
        '/uploads': {
          target: apiTarget,
          changeOrigin: true,
        },
      },
    },
    build: {
      outDir: 'dist',
      sourcemap: false,
      cssCodeSplit: true,
      modulePreload: { polyfill: true },
      rollupOptions: {
        output: {
          chunkFileNames: 'assets/[name]-[hash].js',
          entryFileNames: 'assets/[name]-[hash].js',
          assetFileNames: 'assets/[name]-[hash][extname]',
          /**
           * Keep frequently-changing app code out of heavy vendor buckets so
           * browser caches survive feature deploys. Element Plus / markdown
           * stay on their own chunks (admin vs reading routes).
           */
          manualChunks(id) {
            if (!id.includes('node_modules')) return
            if (
              id.includes('element-plus') ||
              id.includes('@element-plus') ||
              id.includes('@popperjs') ||
              id.includes('@floating-ui') ||
              id.includes('@ctrl/tinycolor') ||
              id.includes('async-validator') ||
              id.includes('lodash-unified') ||
              id.includes('memoize-one') ||
              id.includes('normalize-wheel-es')
            ) {
              return 'element-plus'
            }
            if (id.includes('vditor')) return 'vditor'
            if (
              id.includes('markdown-it') ||
              id.includes('highlight.js') ||
              id.includes('dompurify')
            ) {
              return 'markdown'
            }
            if (
              id.includes('/vue/') ||
              id.includes('/vue-router/') ||
              id.includes('/pinia/') ||
              id.includes('/axios/')
            ) {
              return 'vue-vendor'
            }
          },
        },
      },
    },
  }
})
