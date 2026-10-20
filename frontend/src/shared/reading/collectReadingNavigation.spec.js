import { afterEach, describe, expect, it } from 'vitest'
import { collectReadingNavigation } from './collectReadingNavigation'

function read(html, path = '/tutorials/java/first') {
  document.body.innerHTML = `<main id="reading-test">${html}</main>`
  return collectReadingNavigation(document.getElementById('reading-test'), path)
}
afterEach(() => { document.body.innerHTML = '' })

describe('global reading directory', () => {
  it('collects only marked reading content and assigns stable, distinct anchors', () => {
    const first = read('<h2>List heading</h2><article data-reading-content><h1>Title</h1><h2 id="same">English</h2><h2 id="same">中文</h2><aside data-reading-ignore><h2>Popup</h2></aside></article>')
    expect(first.items.map(item => item.text)).toEqual(['Title', 'English', '中文'])
    expect(new Set(first.items.map(item => item.id)).size).toBe(3)
    for (const item of first.items) expect(document.getElementById(item.id)?.textContent).toBe(item.text)
    expect(collectReadingNavigation(document.getElementById('reading-test'), '/').items).toEqual(first.items)
  })
  it('updates the visible language and excludes collapsed answers and loading content', () => {
    const navigation = read('<article data-reading-content><div style="display:none"><h2>English</h2></div><div><h2>中文</h2></div><details><summary>Answer</summary><h3>Hidden answer</h3></details><div aria-busy="true"><h2>Stale heading</h2></div></article>')
    expect(navigation.items.map(item => item.text)).toEqual(['中文'])
    document.querySelector('details').open = true
    expect(collectReadingNavigation(document.getElementById('reading-test'), '/').items.map(item => item.text)).toEqual(['中文', 'Hidden answer'])
  })
  it('retains hidden desktop course links, account prefixes and the active chapter', () => {
    const navigation = read('<nav data-reading-course style="display:none"><section><h3>Basics</h3><a href="/useradmin/tutorials/java/first?plan=1">01 First</a><a href="/useradmin/tutorials/java/second?plan=1">02 Second</a></section></nav>', '/useradmin/tutorials/java/first')
    expect(navigation.courses).toEqual([{ title: 'Basics', links: [
      { to: '/useradmin/tutorials/java/first?plan=1', text: '01 First', current: true },
      { to: '/useradmin/tutorials/java/second?plan=1', text: '02 Second', current: false },
    ] }])
  })
})
