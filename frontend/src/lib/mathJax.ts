import mathJaxScriptUrl from 'mathjax-full/es5/tex-svg-full.js?url'

type MathJaxNode = Element & { textContent: string | null }

declare global {
  interface Window {
    MathJax?: {
      startup: { promise: Promise<void>; document: { clear: () => void; updateDocument: () => void } }
      tex2svgPromise: (source: string, options: { display: boolean }) => Promise<Element>
      getMetricsFor: (element: Element) => Record<string, unknown>
    }
  }
}

let loading: Promise<void> | undefined

function loadScript(id: string, src: string): Promise<void> {
  return new Promise((resolve, reject) => {
    if (document.getElementById(id)) {
      resolve()
      return
    }
    const script = document.createElement('script')
    script.id = id
    script.src = src
    script.onload = () => resolve()
    script.onerror = () => {
      script.remove()
      reject(new Error('无法加载本地公式预览资源。'))
    }
    document.head.appendChild(script)
  })
}

/**
 * Load the bundled MathJax v3 "full" build once. Vditor recognises this id
 * and therefore does not issue its legacy synchronous CDN request.
 */
export function prepareMathJax(): Promise<void> {
  loading ??= (async () => {
    if (!window.MathJax) {
      window.MathJax = {
        startup: { typeset: false },
        options: { enableMenu: false },
        tex: {
          // Keep the standard TeX/AMS vocabulary while denying runtime module
          // loading and HTML/config macros from authored Markdown.
          packages: { '[-]': ['require', 'configmacros', 'html'] },
          maxBuffer: 10_000,
          maxMacros: 1_000,
        },
      } as never
    }
    await loadScript('protyleMathJaxScript', mathJaxScriptUrl)
    await window.MathJax?.startup.promise
  })().catch((error: unknown) => {
    loading = undefined
    throw error
  })
  return loading
}

function formulaError(element: HTMLElement, source: string): void {
  element.replaceChildren()
  element.textContent = source
  element.classList.add('math-source--error')
  element.setAttribute('title', '公式无法解析')
}

/** Typeset Markdown placeholders after DOMPurify has created the DOM tree. */
export async function typesetMath(root: ParentNode): Promise<void> {
  const formulas = Array.from(root.querySelectorAll<HTMLElement>('.math-source'))
  if (formulas.length === 0) return
  await prepareMathJax()
  const engine = window.MathJax
  if (!engine) throw new Error('MathJax 未初始化。')

  for (const formula of formulas) {
    if (formula.dataset.rendered === 'true') continue
    const source = formula.textContent?.trim() ?? ''
    if (!source) continue
    try {
      const metrics = engine.getMetricsFor(formula)
      const node = await engine.tex2svgPromise(source, {
        ...metrics,
        display: formula.dataset.display === 'true',
      } as { display: boolean }) as MathJaxNode
      const error = node.querySelector('[data-mml-node="merror"]')?.textContent?.trim()
      if (error) {
        formulaError(formula, source)
        continue
      }
      formula.replaceChildren(node)
      formula.dataset.rendered = 'true'
    } catch {
      formulaError(formula, source)
    }
  }
}
