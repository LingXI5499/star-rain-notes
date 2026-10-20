let nextHeadingId = 0

function visible(element, boundary) {
  for (let node = element; node; node = node.parentElement) {
    if (node.hidden || node.getAttribute('aria-hidden') === 'true' || node.getAttribute('aria-busy') === 'true') return false
    const style = getComputedStyle(node)
    if (style.display === 'none' || style.visibility === 'hidden') return false
    if (node.tagName === 'DETAILS' && !node.open && !node.querySelector('summary')?.contains(element)) return false
    if (node === boundary) break
  }
  return true
}

/** Read only explicitly marked reading areas; hidden translations and collapsed answers stay out. */
export function collectReadingNavigation(root, pathname) {
  if (!root) return { items: [], courses: [] }
  const document = root.ownerDocument, seen = new Set(), items = []
  for (const content of root.querySelectorAll('[data-reading-content]')) {
    for (const heading of content.querySelectorAll('h1,h2,h3,h4,h5,h6')) {
      if (seen.has(heading) || heading.closest('[data-reading-ignore]') || !visible(heading, root)) continue
      const text = heading.textContent.trim().replace(/\s+/g, ' ')
      if (!text) continue
      seen.add(heading)
      // Several Markdown instances can emit the same ID, especially in bilingual reading.
      if (!heading.id || document.getElementById(heading.id) !== heading) {
        let id
        do { id = `reading-section-${++nextHeadingId}` } while (document.getElementById(id))
        heading.id = id
      }
      items.push({ id: heading.id, text, level: Number(heading.tagName.slice(1)) })
    }
  }
  const courses = []
  for (const course of root.querySelectorAll('[data-reading-course]')) {
    for (const section of course.querySelectorAll('section')) {
      const links = [...section.querySelectorAll('a[href]')].map(link => ({
        to: link.getAttribute('href'), text: link.textContent.trim().replace(/\s+/g, ' '),
        current: link.getAttribute('aria-current') === 'page' || link.classList.contains('curriculum__chapter--active')
          || link.classList.contains('active') || new URL(link.href, document.baseURI).pathname === pathname,
      }))
      if (links.length) courses.push({ title: section.querySelector('h2,h3')?.textContent.trim() || '章节', links })
    }
  }
  return { items, courses }
}
