import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

const tokensPath = resolve(import.meta.dirname, 'tokens.css')

function parseVars(block: string): Record<string, string> {
  const vars: Record<string, string> = {}
  for (const match of block.matchAll(/--([a-z0-9-]+)\s*:\s*([^;]+);/gi)) {
    vars[match[1].toLowerCase()] = match[2].trim().toLowerCase()
  }
  return vars
}

describe('tokens.css visual theme V3', () => {
  const css = readFileSync(tokensPath, 'utf8')

  it('maps light palette and EP primary to Star Rain V1.0 hex values', () => {
    const darkStart = css.indexOf("[data-theme='dark']")
    const lightPalette = parseVars(css.slice(0, darkStart))
    // EP primary lives in a later :root block; merge all non-dark declarations
    const rootBlocks = [...css.matchAll(/:root\s*\{([\s\S]*?)\n\}/g)].map((m) => m[1])
    const rootVars = parseVars(rootBlocks.join('\n'))

    expect(lightPalette['bg-page']).toBe('#f8f6f1')
    expect(lightPalette.primary).toBe('#0f3d36')
    expect(lightPalette.accent).toBe('#c46f4e')
    expect(rootVars['el-color-primary']).toBe('#0f3d36')
    expect(lightPalette['bg-page']).not.toBe('#f7f7f4')
    expect(lightPalette.primary).not.toBe('#28564d')
    expect(lightPalette.accent).not.toBe('#b86f47')
  })

  it('maps dark palette and EP primary to Night Ink / Mist Green / Star Orange', () => {
    const darkBlocks = [...css.matchAll(/\[data-theme='dark'\]\s*\{([\s\S]*?)\n\}/g)].map((m) => m[1])
    expect(darkBlocks.length).toBeGreaterThanOrEqual(1)
    const merged = parseVars(darkBlocks.join('\n'))

    expect(merged['bg-page']).toBe('#102c31')
    expect(merged.primary).toBe('#7a9c8f')
    expect(merged.accent).toBe('#c46f4e')
    expect(merged['el-color-primary']).toBe('#7a9c8f')
    expect(merged['text-primary']).toBeTruthy()
  })
})
