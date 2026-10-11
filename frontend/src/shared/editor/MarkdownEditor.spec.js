import { describe, expect, it } from 'vitest'
import MarkdownEditor from './MarkdownEditor.vue'
import LegacyMarkdownEditor from '../../modules/blog/components/admin/MarkdownEditor.vue'

describe('shared Markdown editor', () => {
  it('keeps the legacy blog import pointing to the same editor', () => {
    expect(LegacyMarkdownEditor).toBe(MarkdownEditor)
  })
})
