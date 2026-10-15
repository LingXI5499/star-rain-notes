import { it, expect } from 'vitest'
import { inflateRawSync } from 'node:zlib'
import { createWordBlob } from './wordExport'
function zipEntries(bytes) {
  const data = Buffer.from(bytes),
    entries = {}
  for (let offset = data.indexOf(Buffer.from([0x50, 0x4b, 0x01, 0x02])); offset >= 0;) {
    const compressed = data.readUInt32LE(offset + 20),
      length = data.readUInt16LE(offset + 28),
      extra = data.readUInt16LE(offset + 30),
      comment = data.readUInt16LE(offset + 32),
      local = data.readUInt32LE(offset + 42),
      name = data.subarray(offset + 46, offset + 46 + length).toString(),
      start = local + 30 + data.readUInt16LE(local + 26) + data.readUInt16LE(local + 28),
      content = data.subarray(start, start + compressed)
    entries[name] = (
      data.readUInt16LE(offset + 10) === 8 ? inflateRawSync(content) : content
    ).toString()
    offset += 46 + length + extra + comment
    if (data.readUInt32LE(offset) !== 0x02014b50) break
  }
  return entries
}
it('produces real editable OOXML with headings, both code panels, tables and safe image links', async () => {
  const source =
    '# 中文标题\n\n**加粗**\n\n| A | B |\n| --- | --- |\n| 1 | 2 |\n\n::: code-group Java|Python\n\n```java\nclass Test {}\n```\n\n```python\nprint("ok")\n```\n\n:::\n\n![图](/api/media/assets/19/content)\n\n$$\nx^2\n$$'
  const blob = await createWordBlob(source),
    bytes = await blob.arrayBuffer(),
    entries = zipEntries(bytes)
  expect(new Uint8Array(bytes).slice(0, 2)).toEqual(new Uint8Array([0x50, 0x4b]))
  expect(entries['[Content_Types].xml']).toContain('wordprocessingml')
  const xml = entries['word/document.xml']
  expect(xml).toContain('中文标题')
  expect(xml).toContain('class Test')
  expect(xml).toContain('print(')
  expect(xml).toContain('<w:tbl>')
  expect(xml).toContain('x^2')
  expect(xml).toContain('/api/media/assets/19/content')
  expect(xml).toContain('Heading1')
})
