import axios, { AxiosError, AxiosHeaders } from 'axios'
import type { AxiosRequestConfig } from 'axios'

/**
 * RFC 9457 Problem Details shape returned by the V1 API (04-api-design.md §1).
 */
export interface ProblemDetail {
  type?: string
  title?: string
  status?: number
  detail?: string
  instance?: string
  code?: string
  traceId?: string
  violations?: { field: string; message: string }[]
}

/**
 * Shared Axios client for the V1 REST API.
 *
 * - baseURL /api/v1, session cookies sent (withCredentials)
 * - CSRF: XSRF-TOKEN cookie echoed in the X-XSRF-TOKEN header for
 *   state-changing requests; token refreshed via GET /auth/csrf
 * - 403 CSRF_INVALID: refresh the token once and retry exactly once
 *   (never an infinite retry loop)
 * - 401 (outside auth endpoints): clear the auth store and go to login
 */
export const http = axios.create({
  baseURL: '/api/v1',
  timeout: 10000,
  withCredentials: true,
})

let csrfToken: string | null = null

function readCookie(name: string): string | null {
  const match = document.cookie.split('; ').find((c) => c.startsWith(`${name}=`))
  return match ? decodeURIComponent(match.slice(name.length + 1)) : null
}

/**
 * Fetches the current CSRF token via GET /api/v1/auth/csrf (also refreshes
 * the XSRF-TOKEN cookie). Called after login/logout/password change and on
 * CSRF_INVALID.
 */
export async function refreshCsrf(): Promise<string | null> {
  const res = await http.get<{ csrfToken: string }>('/auth/csrf')
  csrfToken = res.data.csrfToken || readCookie('XSRF-TOKEN')
  return csrfToken
}

function isStateChanging(method?: string): boolean {
  return method === 'post' || method === 'put' || method === 'patch' || method === 'delete'
}

function isAuthUrl(url?: string): boolean {
  return !!url && (url.includes('/auth/login') || url.includes('/auth/session') || url.includes('/auth/csrf'))
}

http.interceptors.request.use((config) => {
  if (isStateChanging(config.method)) {
    const token = csrfToken ?? readCookie('XSRF-TOKEN')
    if (token) {
      config.headers.set('X-XSRF-TOKEN', token)
    }
  }
  return config
})

interface RetryableConfig extends AxiosRequestConfig {
  _csrfRetried?: boolean
}

http.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ProblemDetail>) => {
    const status = error.response?.status
    const code = error.response?.data?.code
    const config = error.config as RetryableConfig | undefined

    // CSRF_INVALID: refresh the token once, then retry exactly once.
    if (status === 403 && code === 'CSRF_INVALID' && config && !config._csrfRetried) {
      config._csrfRetried = true
      const token = await refreshCsrf()
      if (token) {
        const headers = config.headers as AxiosHeaders | undefined
        if (headers) {
          headers.set('X-XSRF-TOKEN', token)
        }
        return http.request(config)
      }
    }

    // Session expired / not authenticated: clear auth state and go to login.
    if (status === 401 && config && !isAuthUrl(config.url)) {
      const { useAuthStore } = await import('@/stores/auth')
      useAuthStore().reset()
      if (!window.location.pathname.startsWith('/admin/login')) {
        window.location.assign('/admin/login')
      }
    }

    return Promise.reject(error)
  },
)
