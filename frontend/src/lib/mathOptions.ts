import type { KatexOptions } from 'katex'

/** Shared limits for published content and the editor's formula preview. */
export const mathOptions: KatexOptions = {
  throwOnError: false,
  trust: false,
  output: 'htmlAndMathml',
  maxExpand: 1000,
  maxSize: 20,
  strict: 'ignore',
}
