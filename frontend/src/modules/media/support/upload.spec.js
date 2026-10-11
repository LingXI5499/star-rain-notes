/**
 * upload.spec.js：媒体模块的自动化测试。
 * 职责范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 使用模拟接口、固定输入或组件挂载验证行为，测试标题与断言说明期望结果。
 * 主要依赖：vitest、./upload。
 * 验证场景：editor image upload；rejects unsupported, empty and oversized files before upload；inserts the server media address and escapes malformed names；never displays private paths from an old storage error response；distinguishes text validation and server failure without exposing response internals。
 */

import { describe, expect, it } from 'vitest'
import { imageMarkdown, mediaUploadError, validateEditorImage } from './upload'

describe('editor image upload', () => {
  it('rejects unsupported, empty and oversized files before upload', () => {
    expect(validateEditorImage(new File(['<svg/>'], 'drawing.svg'))).toContain('请选择')
    expect(validateEditorImage(new File([], 'empty.png'))).toContain('空图片')
    expect(validateEditorImage({ name: 'large.PNG', size: 20 * 1024 * 1024 + 1 })).toContain('20 MB')
    expect(validateEditorImage(new File(['png'], '中文图片.PNG'))).toBe('')
  })
  it('inserts the server media address and escapes malformed names', () => {
    expect(imageMarkdown({ originalName: '截图[1]\\\n.png', contentUrl: '/api/media/assets/17/content' })).toBe('![截图 1   .png](</api/media/assets/17/content>)')
    expect(() => imageMarkdown({ url: 'javascript:alert(1)' })).toThrow()
  })
  it('never displays private paths from an old storage error response', () => {
    const message = mediaUploadError({ response: { status: 500, data: { code: 'MEDIA_STORAGE_WRITE_FAILED', message: '/private/home/secret.png' } } })
    expect(message).toContain('权限')
    expect(message).toContain('MEDIA_STORAGE_WRITE_FAILED')
    expect(message).not.toContain('/private/')
    expect(mediaUploadError({ response: { status: 500, data: { code: 'INTERNAL_ERROR', message: 'SQL password' } } })).not.toContain('SQL')
    expect(mediaUploadError({ code: 'ECONNABORTED' })).toContain('是否已经收到')
  })
  it('distinguishes text validation and server failure without exposing response internals', () => {
    expect(mediaUploadError({ response: { status: 415, data: { code: 'MEDIA_CONTENT_TYPE_MISMATCH' } } })).toContain('UTF-8')
    expect(mediaUploadError({ response: { status: 500, data: { code: 'INTERNAL_ERROR', message: 'SQL password' } } })).toContain('HTTP 500 / INTERNAL_ERROR')
    const unsafe = mediaUploadError({ response: { status: 502, data: { code: '/private/path', message: 'secret' } } })
    expect(unsafe).toContain('HTTP 502')
    expect(unsafe).not.toMatch(/private|secret/)
  })
})
