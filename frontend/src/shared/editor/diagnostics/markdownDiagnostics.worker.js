import { diagnoseMarkdown } from './markdownDiagnostics'
self.onmessage = (event) => {
  try {
    self.postMessage({ id: event.data.id, issues: diagnoseMarkdown(event.data.source) })
  } catch {
    self.postMessage({ id: event.data.id, error: '文档检查失败，正文未修改。' })
  }
}
