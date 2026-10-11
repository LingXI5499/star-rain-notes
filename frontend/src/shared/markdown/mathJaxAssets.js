/**
 * mathJaxAssets.js：跨模块基础模块的共享工具。
 * 职责范围：路由、应用装配、共享组件或样式能力。
 * 将跨模块使用的行为收敛到一个实现，调用方通过导出函数或组合式函数使用。
 * 主要依赖：mathjax-full/es5/tex-svg-full.js?url。
 */

/*
 * MathJax 脚本地址的唯一出口。
 *
 * 单独一个模块的理由：`?url` 必须写成静态 import 才由 Vite 在构建期处理
 * （把 mathjax-full 的 es5 全量包原样拷进 dist/assets，只产出一个字符串常量，
 * 不会把 ~1.5MB 的脚本打进 JS 包）。而主包又不能出现这个字符串常量背后的依赖图 ——
 * 因此让 mathJax.js 用 `await import('./mathJaxAssets.js')` 延迟取这个地址：
 * 构建后它是一个几十字节的独立 chunk，只有真的需要排版公式时才会被请求。
 */
import mathJaxScriptUrl from 'mathjax-full/es5/tex-svg-full.js?url'

export default mathJaxScriptUrl
