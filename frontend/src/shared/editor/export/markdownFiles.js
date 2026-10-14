export const MAX_MARKDOWN_BYTES = 2 * 1024 * 1024
export function exportName(source, extension = 'md') {
  const heading =
    String(source)
      .split('\n')
      .find((line) => /^#\s+/.test(line))
      ?.replace(/^#\s+/, '') || '未命名文档'
  const name =
    heading
      .replace(/[<>:"/\\|?*\u0000-\u001f]/g, '_')
      .trim()
      .replace(/[. ]+$/, '')
      .slice(0, 80) || '未命名文档'
  return `${name}.${extension}`
}
export async function readMarkdownFile(file) {
  if (!file || !/\.(md|markdown)$/i.test(file.name))
    throw new Error('仅支持 .md 和 .markdown 导入。PDF 与 Word 仅提供导出。')
  if (file.size > MAX_MARKDOWN_BYTES) throw new Error('Markdown 文件不能超过 2 MiB。')
  const bytes = await file.arrayBuffer()
  if (bytes.byteLength > MAX_MARKDOWN_BYTES) throw new Error('Markdown 文件不能超过 2 MiB。')
  let source
  try {
    source = new TextDecoder('utf-8', { fatal: true }).decode(bytes)
  } catch {
    throw new Error('文件不是有效的 UTF-8 文本，请转换编码后再导入。')
  }
  if (source.includes('\0')) throw new Error('文件包含二进制内容，未导入。')
  return source.replace(/\r\n?/g, '\n')
}
export function appendMarkdown(original, incoming) {
  return original ? `${original.replace(/\n*$/, '')}\n\n${incoming}` : incoming
}
export function downloadBlob(blob, name) {
  const url = URL.createObjectURL(blob),
    anchor = document.createElement('a')
  anchor.href = url
  anchor.download = name
  document.body.append(anchor)
  anchor.click()
  anchor.remove()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}
