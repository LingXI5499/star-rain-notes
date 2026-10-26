import { describe, expect, it } from 'vitest'
import { accountPath, contentPath, canonicalPath } from './viewMode'

describe('canonical navigation', () => {
  it.each([
    [undefined, '/'], ['', '/'], ['/', '/'], ['blog/', '/blog'],
    ['/useradmin', '/'], ['/useradmin/', '/'], ['/useradmin/center', '/center'],
    ['/useradmin/reset-password?token=a%2Bb%3D#reset', '/reset-password?token=a%2Bb%3D#reset'],
    ['/useradmin/login?redirect=%2Fuseradmin%2Fcenter%3Ftab%3Da#form', '/login?redirect=%2Fuseradmin%2Fcenter%3Ftab%3Da#form'],
    ['/useradministrator/blog', '/useradministrator/blog'],
  ])('normalizes %s without changing tokens, query or hash', (input, expected) => {
    expect(canonicalPath(input)).toBe(expected)
    expect(accountPath(input)).toBe(expected)
    expect(contentPath(input)).toBe(expected)
    expect(canonicalPath(expected)).toBe(expected)
  })
})
