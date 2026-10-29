import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createMemoryHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '../modules/account/stores/authStore'
import HomePage from '../modules/site/pages/HomePage.vue'
import { createAppRouter, routeDeclarations } from './index'

vi.mock('../modules/account/pages/AuthPage.vue', () => ({ default: { template: '<div />' } }))

let router, auth
beforeEach(() => {
  setActivePinia(createPinia())
  auth = useAuthStore()
  auth.initialized = true
  router = createAppRouter(createMemoryHistory())
  vi.spyOn(window, 'scrollTo').mockImplementation(() => {})
})

describe('unified router and access controls', () => {
  it('declares every page once and keeps the existing homepage component', () => {
    const records = router.getRoutes()
    expect(new Set(records.map(r => r.path)).size).toBe(records.length)
    expect(records.length).toBe(routeDeclarations.length + 2)
    expect(router.resolve('/').matched[0].components.default).toBe(HomePage)
    expect(router.resolve('/').meta.console).toBeUndefined()
    expect(records.filter(r => r.path.startsWith('/useradmin'))).toHaveLength(1)
    for (const declaration of routeDeclarations) {
      const example=declaration.path.replace(':category(t[0-9]{2})','t01').replace(':topic(t[0-9]{2}-[0-9]{2})?','t01-01').replace(':category(r[0-9]{2})','r01').replace(':topic(r[0-9]{2}-[0-9]{2})?','r01-01')
      expect(router.resolve(example).matched.at(-1).path).toBe(declaration.path)
    }
  })
  it.each(['/login', '/register', '/forgot-password', '/reset-password'])('opens %s directly', async path => {
    await router.push(path)
    expect(router.currentRoute.value.path).toBe(path)
    expect(router.currentRoute.value.meta.authPage).toBe(true)
  })
  it('initializes the account entry session on the public homepage', async () => {
    auth.initialized = false
    const initialize = vi.spyOn(auth, 'initialize').mockResolvedValue()
    await router.push('/')
    expect(initialize).toHaveBeenCalledOnce()
  })
  it('preserves the complete protected target through login', async () => {
    await router.push('/learning/plans/12?tab=active#task')
    expect(router.currentRoute.value.path).toBe('/login')
    expect(router.currentRoute.value.query.redirect).toBe('/learning/plans/12?tab=active#task')
    auth.currentUser = { id: 7, roles: ['USER'], permissions: [] }
    await router.push(router.currentRoute.value.query.redirect)
    expect(router.currentRoute.value.fullPath).toBe('/learning/plans/12?tab=active#task')
  })
  it.each(['/dashboard', '/accounts', '/invitations', '/audits', '/blog/manage', '/tutorials/manage', '/portfolio/manage'])('denies a normal user %s', async path => {
    auth.currentUser = { id: 7, roles: ['USER'], permissions: [] }
    await router.push(path)
    expect(router.currentRoute.value.path).toBe('/center')
  })
  it('requires SUPER_ADMIN even when another role has the permission', async () => {
    auth.currentUser = { id: 7, roles: ['ADMIN'], permissions: ['account:read', 'media:read'] }
    await router.push('/accounts')
    expect(router.currentRoute.value.path).toBe('/center')
    await router.push('/media')
    expect(router.currentRoute.value.path).toBe('/media')
    auth.currentUser = { id: 8, roles: ['SUPER_ADMIN'], permissions: ['account:read'] }
    await router.push('/accounts')
    expect(router.currentRoute.value.path).toBe('/accounts')
  })
  it.each(['/blog', '/tutorials', '/portfolio', '/english', '/about', '/messages'])('keeps %s public', async path => {
    await router.push(path)
    expect(router.currentRoute.value.path).toBe(path)
    expect(router.currentRoute.value.meta.publicPage).toBe(true)
  })
  it.each([
    ['/useradmin?from=bookmark#top', '/?from=bookmark#top'],
    ['/useradmin/reset-password?token=a%2Bb%3D#reset', '/reset-password?token=a%2Bb=#reset'],
    ['/useradmin/blog?tag=java&page=2#list', '/blog?tag=java&page=2#list'],
  ])('redirects old %s once to %s', async (oldPath, expected) => {
    await router.push(oldPath)
    expect(router.currentRoute.value.fullPath).toBe(expected)
    if (oldPath.includes('token=')) expect(router.currentRoute.value.query.token).toBe('a+b=')
  })
  it('retains an old invitation token inside the login return address', async () => {
    await router.push('/useradmin/invitation/accept?token=a%2Bb%3D#accept')
    expect(router.currentRoute.value.path).toBe('/login')
    expect(router.currentRoute.value.query.redirect).toBe('/invitation/accept?token=a%2Bb=#accept')
  })
  it('preserves repeated and empty query values and a nested legacy return address', async () => {
    await router.push('/useradmin/login?tag=a&tag=b&empty=&flag&redirect=%2Fuseradmin%2Fcenter%3Ftab%3Dprofile%23details#form')
    expect(router.currentRoute.value.path).toBe('/login')
    expect(router.currentRoute.value.query).toEqual({ tag: ['a', 'b'], empty: '', flag: null,
      redirect: '/useradmin/center?tab=profile#details' })
    expect(router.currentRoute.value.hash).toBe('#form')
  })
  it('keeps dynamic management matches, aliases and redirect parameters', async () => {
    auth.currentUser = { id: 8, roles: ['SUPER_ADMIN'], permissions: ['tutorial:edit'] }
    await router.push('/useradmin/tutorials/12/chapters/34/exercises?kind=card#answer')
    expect(router.currentRoute.value.fullPath).toBe('/tutorials/12/chapters/34/cards?kind=card#answer')
    expect(router.currentRoute.value.params).toEqual({ tutorialId: '12', chapterId: '34' })
    await router.push('/useradmin/learning/today?day=1#tasks')
    expect(router.currentRoute.value.fullPath).toBe('/learning?day=1#tasks')
  })
  it('keeps unknown pages on a noindex not-found component', async () => {
    await router.push('/useradmin/does-not-exist?old=1#top')
    expect(router.currentRoute.value.fullPath).toBe('/does-not-exist?old=1#top')
    expect(router.currentRoute.value.meta.noindex).toBe(true)
  })
})
