/*
 * MathJax 排版 —— 从 V1 `lib/mathJax.ts` 移植。
 *
 * 三件事必须按这个顺序，任何一步提前都会出问题：
 *   1. 先写 window.MathJax 配置（startup.typeset: false），**再**加载脚本；
 *      脚本一执行就会读 config，晚一步配置就被忽略，页面会自动全量 typeset 一遍
 *      （排版编辑器与正文之外的地方，慢且会改变 Vditor 的 DOM）。
 *   2. 加载用的 script id 固定为 protyleMathJaxScript：Vditor 认这个 id，
 *      因此它不会再去请求自己的 CDN 版本（离线 / 内网环境下的关键点）。
 *   3. 排版发生在 DOMPurify 消毒**之后**（typesetMath 由渲染器在 DOM 落地后调用）。
 *
 * 安全：packages 里禁掉 require / configmacros / html 三个扩展 —— 正文里的公式
 * 不能靠 \require 在运行时加载额外模块，也不能用宏改配置或注入 HTML。
 * maxBuffer / maxMacros 兜住「一条公式把浏览器算死」。
 */

/* 载入中/已载入的 promise。失败时清空，让下一次调用可以重试（例如网络瞬断） */
let loading

function loadScript(id, src) {
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
      /* 失败的 script 标签要摘掉：留着会让下一次 loadScript 以为已经加载过 */
      script.remove()
      reject(new Error('无法加载本地公式预览资源。'))
    }
    document.head.appendChild(script)
  })
}

export function prepareMathJax() {
  loading ??= (async () => {
    if (!window.MathJax) {
      window.MathJax = {
        startup: { typeset: false },
        options: { enableMenu: false },
        tex: {
          packages: { '[-]': ['require', 'configmacros', 'html'] },
          maxBuffer: 10000,
          maxMacros: 1000,
        },
      }
    }
    const { default: mathJaxScriptUrl } = await import('./mathJaxAssets.js')
    await loadScript('protyleMathJaxScript', mathJaxScriptUrl)
    await window.MathJax?.startup.promise
  })().catch((error) => {
    loading = undefined
    throw error
  })
  return loading
}

/*
 * 单条公式失败（TeX 语法错）时把元素还原成源码，并标成错误色。
 * 刻意的取舍：宁可显示一行红色的原始 TeX，也不要留一块空白 ——
 * 空白会让读者以为文章少了一段，红色源码能直接看出是公式写错了。
 */
function formulaError(element, source) {
  element.replaceChildren()
  element.textContent = source
  element.classList.add('math-source--error')
  element.setAttribute('title', '公式无法解析')
}

/* 对 root 里的每个 .math-source 占位元素做排版（已排版过的跳过） */
export async function typesetMath(root) {
  const formulas = Array.from(root.querySelectorAll('.math-source'))
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
      })
      /* MathJax 把 TeX 错误包成 merror 节点而不是 reject，需要自己认出来 */
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
