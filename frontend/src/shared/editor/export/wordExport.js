import { renderMarkdownDocument } from '../../markdown/markdown'

/** Real OOXML package. No remote image fetch or permission changes are performed. */
export async function createWordBlob(source) {
  const {
    Document,
    Paragraph,
    TextRun,
    Table,
    TableRow,
    TableCell,
    ExternalHyperlink,
    HeadingLevel,
    Packer,
    WidthType,
  } = await import('docx')
  const root = document.createElement('div')
  root.innerHTML = renderMarkdownDocument(source).html
  const runs = (node, style = {}) => {
    if (node.nodeType === 3) return [new TextRun({ text: node.textContent, ...style })]
    if (node.nodeType !== 1) return []
    const tag = node.tagName
    if (tag === 'BR') return [new TextRun({ break: 1 })]
    if (tag === 'INPUT' && node.getAttribute('type') === 'checkbox')
      return [new TextRun({ text: node.hasAttribute('checked') ? '[x] ' : '[ ] ' })]
    if (tag === 'IMG')
      return [
        new TextRun({
          text: `[图片：${node.getAttribute('alt') || '未提供替代文本'}] ${node.getAttribute('src') || ''}`,
        }),
      ]
    if (node.classList.contains('math-source'))
      return [
        new TextRun({
          text: `${node.classList.contains('math-source--display') ? '$$' : '$'}${node.textContent}${node.classList.contains('math-source--display') ? '$$' : '$'}`,
          font: 'Consolas',
        }),
      ]
    const next = {
      ...style,
      ...(['STRONG', 'B'].includes(tag) ? { bold: true } : {}),
      ...(['EM', 'I'].includes(tag) ? { italics: true } : {}),
      ...(['DEL', 'S'].includes(tag) ? { strike: true } : {}),
      ...(tag === 'CODE' ? { font: 'Consolas' } : {}),
    }
    const children = [...node.childNodes].flatMap((child) => runs(child, next))
    if (tag === 'A' && /^https?:\/\//i.test(node.getAttribute('href') || ''))
      return [new ExternalHyperlink({ link: node.getAttribute('href'), children })]
    return children
  }
  const paragraph = (node, options = {}) =>
    new Paragraph({ children: runs(node), spacing: { after: 140 }, ...options })
  const blocks = (node) => {
    if (node.nodeType === 3)
      return node.textContent.trim() ? [new Paragraph({ text: node.textContent })] : []
    if (node.nodeType !== 1) return []
    const tag = node.tagName
    if (/^H[1-6]$/.test(tag))
      return [paragraph(node, { heading: HeadingLevel[`HEADING_${tag[1]}`] })]
    if (tag === 'TABLE')
      return [
        new Table({
          width: { size: 100, type: WidthType.PERCENTAGE },
          rows: [...node.querySelectorAll('tr')].map(
            (row) =>
              new TableRow({
                children: [...row.children]
                  .filter((cell) => ['TD', 'TH'].includes(cell.tagName))
                  .map((cell) => new TableCell({ children: [paragraph(cell)] })),
              }),
          ),
        }),
      ]
    if (tag === 'PRE')
      return node.textContent
        .replace(/\n$/, '')
        .split('\n')
        .map(
          (line) =>
            new Paragraph({
              children: [new TextRun({ text: line, font: 'Consolas', size: 19 })],
              spacing: { after: 0 },
            }),
        )
    if (tag === 'UL' || tag === 'OL')
      return [...node.children].flatMap((item, index) => {
        const clone = item.cloneNode(true)
        ;[...clone.children]
          .filter((child) => ['UL', 'OL'].includes(child.tagName))
          .forEach((child) => child.remove())
        return [
          new Paragraph({
            children: [
              new TextRun({ text: tag === 'OL' ? `${index + 1}. ` : '• ' }),
              ...runs(clone),
            ],
            spacing: { after: 100 },
          }),
          ...[...item.children]
            .filter((child) => ['UL', 'OL'].includes(child.tagName))
            .flatMap(blocks),
        ]
      })
    if (tag === 'BLOCKQUOTE')
      return [...node.children].map((child) => paragraph(child, { indent: { left: 400 } }))
    if (tag === 'P' || node.classList.contains('math-source')) return [paragraph(node)]
    if (node.dataset.codeGroup) {
      let labels = []
      try {
        labels = JSON.parse(node.dataset.codeLabels || '[]')
      } catch {}
      return [...node.children].flatMap((child, index) => [
        new Paragraph({
          text:
            labels[index] ||
            child.querySelector('code')?.className.replace('language-', '') ||
            '代码',
          heading: HeadingLevel.HEADING_6,
        }),
        ...blocks(child),
      ])
    }
    if (tag === 'HR') return [new Paragraph({ text: '────────────────' })]
    return [...node.childNodes].flatMap(blocks)
  }
  const children = [...root.childNodes].flatMap(blocks)
  children.push(
    new Paragraph({
      children: [
        new TextRun({
          text: '导出说明：公式保留 TeX 源码；图片保留替代文本与链接，受保护图片需登录访问。',
          italics: true,
          size: 18,
        }),
      ],
    }),
  )
  return Packer.toBlob(
    new Document({
      creator: '星雨笔录',
      sections: [{ children }],
      styles: { default: { document: { run: { font: 'Microsoft YaHei', size: 22 } } } },
    }),
  )
}
