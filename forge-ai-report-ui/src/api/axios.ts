import axios, { AxiosResponse, InternalAxiosRequestConfig, AxiosError } from 'axios'
import { ResultEnum } from "@/enums/httpEnum"
import { getLocalStorage } from '@/utils/storage'
import { StorageEnum } from '@/enums/storageEnum'

interface ApiResponse<T = any> {
  code?: number
  data?: T
  msg?: string
}

interface RequestError extends Error {
  code?: number
  data?: any
  response?: AxiosResponse
}

// 生成 UUID（防重放 Nonce）
function generateUUID(): string {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
    const r = Math.random() * 16 | 0
    const v = c === 'x' ? r : (r & 0x3 | 0x8)
    return v.toString(16)
  })
}

// 不需要防重放的路径
const REPLAY_EXCLUDE_PATHS = ['/auth/captcha', '/crypto/public-key']

const axiosInstance = axios.create({
  baseURL: import.meta.env.DEV ? import.meta.env.VITE_DEV_PATH : import.meta.env.VITE_PRO_PATH,
  timeout: ResultEnum.TIMEOUT,
})

const createRequestError = (response: AxiosResponse<ApiResponse>, fallbackMessage = '请求失败'): RequestError => {
  const error = new Error(response.data?.msg || fallbackMessage) as RequestError
  error.code = response.data?.code
  error.data = response.data?.data
  error.response = response
  return error
}

const createAxiosRequestError = (error: AxiosError<ApiResponse>): RequestError => {
  const response = error.response
  const requestError = new Error(response?.data?.msg || error.message || '请求失败') as RequestError
  requestError.code = response?.data?.code ?? response?.status
  requestError.data = response?.data?.data
  requestError.response = response
  return requestError
}

axiosInstance.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    config.headers = config.headers || {}

    // Authorization token
    const token = getLocalStorage(StorageEnum.GO_ACCESS_TOKEN_STORE)
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }

    // 防重放参数：X-Timestamp + X-Nonce
    const url = config.url || ''
    const path = url.split('?')[0]
    const excluded = REPLAY_EXCLUDE_PATHS.some(p => path === p || path.endsWith(p))
    if (!excluded) {
      config.headers['X-Timestamp'] = Date.now().toString()
      config.headers['X-Nonce'] = generateUUID()
    }

    return config
  },
  (error: AxiosError) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
axiosInstance.interceptors.response.use(
  (res: AxiosResponse<ApiResponse>) => {
    const { code } = res.data || {}
    if (code === undefined || code === null) return res.data as any
    if (code === ResultEnum.DATA_SUCCESS) return res.data as any
    if (code === ResultEnum.SUCCESS) return res.data as any
    throw createRequestError(res)
  },
  (err: AxiosError<ApiResponse>) => {
    return Promise.reject(createAxiosRequestError(err))
  }
)

export default axiosInstance
