import { renderMarkdownDocument, typesetMarkdownMath } from '../../markdown/markdown'
const css = `@page{size:A4;margin:18mm}body{font:12pt/1.7 system-ui,"Microsoft YaHei",sans-serif;color:#111}h1,h2,h3,h4,h5,h6{break-after:avoid}pre{white-space:pre-wrap;overflow-wrap:anywhere;border:1px solid #ddd;padding:10px;break-inside:avoid;font:10pt/1.5 monospace}table{border-collapse:collapse;width:100%}th,td{border:1px solid #bbb;padding:6px;overflow-wrap:anywhere}thead{display:table-header-group}img{max-width:100%;height:auto;break-inside:avoid}a{color:inherit}blockquote{border-left:3px solid #bbb;padding-left:14px}.math-source--display{text-align:center}svg{max-width:100%}.code-group>pre{display:block!important}[hidden]{display:block!important}button,.code-copy,.code-group__tabs{display:none!important}`
export async function printMarkdown(source, onNotice = () => {}, isCurrent = () => true) {
  const frame = document.createElement('iframe')
  frame.title = 'PDF 导出打印页面'
  frame.setAttribute('aria-hidden', 'true')
  frame.style.cssText = 'position:fixed;left:-10000px;width:1000px;height:800px;border:0'
  document.body.append(frame)
  let timer,
    imageTimer,
    closed = false
  const cleanup = () => {
    if (closed) return
    closed = true
    clearTimeout(timer)
    clearTimeout(imageTimer)
    frame.contentWindow?.removeEventListener('afterprint', cleanup)
    frame.remove()
  }
  try {
    const doc = frame.contentDocument,
      style = doc.createElement('style')
    style.textContent = css
    doc.head.append(style)
    doc.title = '星雨笔录文档'
    doc.body.innerHTML = renderMarkdownDocument(source).html
    for (const group of doc.querySelectorAll('[data-code-group]')) {
      let labels = []
      try {
        labels = JSON.parse(group.dataset.codeLabels || '[]')
      } catch {}
      ;[...group.querySelectorAll(':scope > pre')].forEach((pre, index) => {
        const label = doc.createElement('p')
        label.textContent =
          labels[index] || pre.querySelector('code')?.className.replace('language-', '') || '代码'
        pre.before(label)
      })
    }
    await typesetMarkdownMath(doc.body).catch(() =>
      onNotice('公式排版不可用，PDF 中将保留公式源码。'),
    )
    const images = [...doc.querySelectorAll('img')]
    images.forEach((img) => {
      img.loading = 'eager'
    })
    if (images.length)
      await Promise.race([
        Promise.all(
          images.map((img) =>
            img.complete
              ? Promise.resolve()
              : new Promise((resolve) => {
                  img.onload = resolve
                  img.onerror = resolve
                }),
          ),
        ),
        new Promise((resolve) => {
          imageTimer = setTimeout(resolve, 4000)
        }),
      ])
    clearTimeout(imageTimer)
    if (!isCurrent()) {
      cleanup()
      return cleanup
    }
    if (images.some((img) => !img.complete || !img.naturalWidth))
      onNotice('部分图片无法加载，打印前请检查预览；受保护图片需要登录。')
    frame.contentWindow.addEventListener('afterprint', cleanup)
    timer = setTimeout(cleanup, 60000)
    frame.contentWindow.focus()
    frame.contentWindow.print()
    return cleanup
  } catch (error) {
    cleanup()
    throw error
  }
}
