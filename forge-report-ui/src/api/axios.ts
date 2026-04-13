import axios, { AxiosResponse, InternalAxiosRequestConfig, AxiosError } from 'axios'
import { ResultEnum } from "@/enums/httpEnum"
import { ErrorPageNameMap } from "@/enums/pageEnum"
import { redirectErrorPage } from '@/utils'
import { getLocalStorage } from '@/utils/storage'
import { StorageEnum } from '@/enums/storageEnum'

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
  (res: AxiosResponse) => {
    const { code } = res.data as { code: number }
    if (code === undefined || code === null) return Promise.resolve(res.data)
    if (code === ResultEnum.DATA_SUCCESS) return Promise.resolve(res.data)
    if (code === ResultEnum.SUCCESS) return Promise.resolve(res.data)
    // 重定向
    if (ErrorPageNameMap.get(code)) redirectErrorPage(code)
    return Promise.resolve(res.data)
  },
  (err: AxiosResponse) => {
    return Promise.reject(err)
  }
)

export default axiosInstance
