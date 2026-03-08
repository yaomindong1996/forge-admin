import axios from 'axios'
import { setupInterceptors } from './interceptors'

export function createAxios(options = {}) {
  const defaultOptions = {
    baseURL: import.meta.env.VITE_REQUEST_PREFIX,
    timeout: 12000,
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