import { renderMarkdown } from '../../../shared/markdown/markdown'

function mediaKey(url) {
 try { return new URL(url, window.location.origin).href } catch { return url }
}

/** Leave authored content intact; suppress only automatically inserted duplicates. */
export function workVisualMedia(work) {
 const used = new Set()
 const sections = work.hasSections ? (work.sections || []).filter(section => section.visible !== false) : []
 const markdown = work.hasSections
  ? sections.filter(section => ['MARKDOWN', 'CUSTOM'].includes(section.sectionType)).map(section => section.content || '').join('\n\n')
  : work.bodyMarkdown || ''
 const document = new DOMParser().parseFromString(renderMarkdown(markdown), 'text/html')
 document.querySelectorAll('img[src]').forEach(image => used.add(mediaKey(image.getAttribute('src'))))
 for (const section of sections) {
  if (['IMAGE', 'GALLERY'].includes(section.sectionType))
   for (const media of section.media || []) if (media.url) used.add(mediaKey(media.url))
 }
 const cover = work.coverUrl && !used.has(mediaKey(work.coverUrl)) ? work.coverUrl : null
 if (cover) used.add(mediaKey(cover))
 const gallery = work.hasSections ? [] : (work.media || []).filter(media => {
  if (media.usageType !== 'SCREENSHOT' || !media.url || used.has(mediaKey(media.url))) return false
  used.add(mediaKey(media.url))
  return true
 })
 return { cover, gallery }
}
