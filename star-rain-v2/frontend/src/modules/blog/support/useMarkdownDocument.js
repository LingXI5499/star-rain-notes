import { nextTick, ref, watch } from 'vue'
import { enhanceMarkdownDom, renderMarkdownDocument, typesetMarkdownMath } from './markdown'

/*
 * 「把一段 Markdown 渲染进一个容器」这件事的唯一定义。
 *
 * BlogProse（公开阅读页）与 BlogPreview（后台预览）都调它，两边的差别只剩外层类名
 * 与要不要上报大纲 —— 不允许各自实现一遍，否则「预览好看、发布跑版」会再次出现。
 *
 * 三步的顺序是硬约束（理由见 support/markdown.js 的文件头）：
 *   1. 渲染 + 消毒 → 写进 v-html；
 *   2. await nextTick() → DOM 真的落地了，才能建代码页签（否则 querySelector 拿到空）；
 *   3. 排版公式 —— 必须在消毒之后。
 *
 * 公式排版失败被吞掉是有意的：MathJax 资源加载不了时，正文里其它内容（代码、表格、
 * 标题）必须照常显示，只有公式退化成红色原始 TeX。把异常抛给组件只会整页白屏。
 */
export function useMarkdownDocument(rootRef, source, options = {}) {
  const html = ref('')

  watch(source, async (value) => {
    const rendered = renderMarkdownDocument(value)
    html.value = rendered.html
    options.onOutline?.(rendered.outline)
    await nextTick()
    /* 只对「本次真正渲染出来的元素」做增强：未标记的子元素查询是幂等的 */
    enhanceMarkdownDom(rootRef.value)
    try {
      await typesetMarkdownMath(rootRef.value)
    } catch {
      /* 资源加载失败：保留正文，公式显示为源码 */
    }
  }, { immediate: true })

  return { html }
}
