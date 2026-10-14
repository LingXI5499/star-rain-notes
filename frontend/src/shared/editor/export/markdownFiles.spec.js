import { describe, it, expect } from 'vitest'
import { readMarkdownFile, exportName, appendMarkdown, MAX_MARKDOWN_BYTES } from './markdownFiles'
const file = (name, source, size) => ({
  name,
  size: size ?? new TextEncoder().encode(source).byteLength,
  arrayBuffer: async () => new TextEncoder().encode(source).buffer,
})
describe('Markdown-only import and UTF-8 roundtrip', () => {
  it('accepts Markdown and normalizes only line endings', async () => {
    const source = '# 中文\r\n\r\n`代码`\r\n'
    expect(await readMarkdownFile(file('笔记.md', source))).toBe('# 中文\n\n`代码`\n')
  })
  it.each(['sample.pdf', 'sample.docx', 'sample.html'])('rejects %s import', async (name) => {
    await expect(readMarkdownFile(file(name, 'body'))).rejects.toThrow('仅支持')
  })
  it('rejects quota, malformed UTF-8 and binary without applying anything', async () => {
    await expect(readMarkdownFile(file('a.md', 'x', MAX_MARKDOWN_BYTES + 1))).rejects.toThrow(
      '2 MiB',
    )
    await expect(
      readMarkdownFile({
        name: 'a.md',
        size: 2,
        arrayBuffer: async () => new Uint8Array([0xc3, 0x28]).buffer,
      }),
    ).rejects.toThrow('UTF-8')
    await expect(readMarkdownFile(file('a.md', 'a\0b'))).rejects.toThrow('二进制')
  })
  it('has a safe download name and explicit append boundaries', () => {
    expect(exportName('# ../A?<>')).toBe('.._A___.md')
    expect(appendMarkdown('old\n', 'new')).toBe('old\n\nnew')
  })
  it('export text can be read back byte-for-byte when already LF UTF-8', async () => {
    const source = '# A\n\n![图](/api/media/assets/19/content)\n\n$$\nx^2\n$$\n'
    expect(await readMarkdownFile(file(exportName(source), source))).toBe(source)
  })
})
