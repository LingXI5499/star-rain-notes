/**
 * markdownEditorChrome.js：跨模块基础模块的共享工具。
 * 职责范围：路由、应用装配、共享组件或样式能力。
 * 将跨模块使用的行为收敛到一个实现，调用方通过导出函数或组合式函数使用。
 * 阅读入口：resolveVditorEditorHeight（解析VditorEditor高度）；setVditorFullscreenActive（设置VditorFullscreen有效）。
 */

/*
 * Vditor 编辑器外壳的两个小工具 —— 从 V1 `lib/markdownEditorChrome.ts` 移植。
 *
 * 这两件事都与「编辑器所处的页面外壳」有关，所以单独成文件、并且不做任何 DOM 之外的事，
 * 便于离线断言（见 markdownEditorChrome.spec.js）。
 */

/*
 * 高度必须是数值，不能是 'auto'。
 *
 * Vditor 在大纲点击时，如果 height 是 'auto'，会去 window 上做滚动（scrollTo）；
 * 而 V2 控制台的可滚动容器是 `.admin-shell__content`（不是 window），
 * 于是点了大纲什么也不会发生 —— 这是 V1 踩过的坑，注释原文保留在这里。
 *
 * 下限 640：低于这个高度时 IR 模式的编辑区几乎看不到内容，
 * 600px 高的窗口（笔记本分屏）也要保证可用，所以不做「按比例缩到很小」的处理。
 */
export
  /**
   * 解析VditorEditor高度。
   * @param viewportHeight viewport高度；默认值、解构字段与可选参数见函数签名。
   * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
   */
  function resolveVditorEditorHeight(viewportHeight) {
  return Math.max(640, Number(viewportHeight) - 280)
}

/*
 * 全屏时需要给 <html> 打标记。
 *
 * Vditor 全屏是 position: fixed，但它仍然是 `.admin-shell__body` 的后代，
 * 而侧栏 `.admin-shell__sidebar` 的 z-index 是 30 —— 不把外壳藏起来，
 * 侧栏就会盖在编辑器上面（fixed 元素也不会超出祖先的堆叠上下文）。
 * 样式见 styles/admin.css 的 html.is-vditor-fullscreen 段。
 */
export
  /**
   * 设置VditorFullscreen有效。
   * 主要协作：document.documentElement.classList.toggle、Boolean。
   * @param active 有效；默认值、解构字段与可选参数见函数签名。
   */
  function setVditorFullscreenActive(active) {
  document.documentElement.classList.toggle('is-vditor-fullscreen', Boolean(active))
}
