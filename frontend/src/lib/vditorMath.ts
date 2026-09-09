import katexScriptUrl from 'katex/dist/katex.min.js?url'
import chemistryScriptUrl from 'katex/dist/contrib/mhchem.min.js?url'
import katexStyles from 'katex/dist/katex.min.css?inline'
import { mathOptions } from './mathOptions'

let loading: Promise<void> | undefined

function loadScript(id: string, src: string): Promise<void> {
  return new Promise((resolve, reject) => {
    if (document.getElementById(id)) {
      resolve()
      return
    }
    const script = document.createElement('script')
    script.src = src
    script.onload = () => {
      script.id = id
      resolve()
    }
    script.onerror = () => {
      script.remove()
      reject(new Error('无法加载公式预览资源，请刷新页面重试。'))
    }
    document.head.appendChild(script)
  })
}

/**
 * Vditor 3.11 loads KaTeX through these resource IDs. Preload the same assets
 * from our Vite build before creating the editor, so its internal renderer
 * reuses them instead of requesting its older CDN copy. Vite also rewrites
 * the font URLs in the inline stylesheet for development and production.
 */
export function prepareVditorMath(): Promise<void> {
  loading ??= (async () => {
    if (!document.getElementById('vditorKatexStyle')) {
      const style = document.createElement('style')
      style.id = 'vditorKatexStyle'
      style.textContent = katexStyles
      document.head.appendChild(style)
    }
    await loadScript('vditorKatexScript', katexScriptUrl)
    await loadScript('vditorKatexChemScript', chemistryScriptUrl)
    const engine = (window as Window & { katex: typeof import('katex') }).katex
    const render = engine.renderToString.bind(engine)
    engine.renderToString = (source, options) => render(source, {
      ...options,
      ...mathOptions,
      // Vditor manages selection/copy using its HTML preview and data-math.
      output: 'html',
      macros: { ...options?.macros },
    })
  })().catch((error: unknown) => {
    loading = undefined
    throw error
  })
  return loading
}
