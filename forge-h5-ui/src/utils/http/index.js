import axios from 'axios'
import { setupInterceptors } from './interceptors'
import { createUniRequestAdapter, resolvePlatformRequestBaseURL } from './uni-adapter'

export function createAxios(options = {}) {
  let platformBaseURL = ''
  let adapter
  // #ifndef H5
  platformBaseURL = import.meta.env.VITE_MP_API_BASE_URL || ''
  adapter = createUniRequestAdapter()
  // #endif
  const defaultOptions = {
    baseURL: resolvePlatformRequestBaseURL(import.meta.env.VITE_REQUEST_PREFIX, platformBaseURL),
    timeout: 12000,
    ...(adapter ? { adapter } : {}),
  }
  const service = axios.create({
    ...defaultOptions,
    ...options,
  })
  setupInterceptors(service)
  return service
}

export const request = createAxios()

// 创建一个不带baseURL的axios实例，专门用于登录等不需要前缀的请求
export const noPrefixRequest = createAxios({
  baseURL: '',
})

export const mockRequest = createAxios({
  baseURL: '/mock-api',
})
