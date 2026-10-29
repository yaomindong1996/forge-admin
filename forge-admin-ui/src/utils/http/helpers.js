import { useAuthStore } from '@/store'
import { showRequestErrorDialog } from './error-dialog'

let isConfirming = false

const SESSION_INVALID_MESSAGES = [
  '登录凭证无效',
  '登录已过期',
  '未提供登录凭证',
  '您的账号已在其他地方登录',
  '您已被强制下线',
  '未登录或登录已过期，请重新登录',
  '登录闲置超时，请重新登录',
  'token 已被冻结',
  '未能读取到有效 token',
]

const SESSION_INVALID_CODES = new Set([-8, '-8', 11007, 11008, 11011, 11012, 11013, 11014, 11015, 11017])

const NON_SESSION_AUTH_PATHS = [
  '/api/flow',
  '/openapi/v1/jobs',
  '/openapi/v1/executions',
  '/openapi/v1/capabilities',
  '/oauth2/',
  '/mcp',
]

export function isAuthErrorCode(code) {
  return code === '-8' || code === 401 || code === '401' || code === 11007 || code === 11008
}

export function isSessionInvalidMessage(message) {
  const text = String(message || '').trim()
  if (!text) {
    return false
  }
  return SESSION_INVALID_MESSAGES.some(item => text.includes(item))
}

export function isNonSessionAuthPath(url) {
  const path = String(url || '')
  return NON_SESSION_AUTH_PATHS.some(prefix => path.includes(prefix))
}

export function shouldForceReLogin(code, message, detail = null) {
  if (isNonSessionAuthPath(detail?.url)) {
    return false
  }
  if (SESSION_INVALID_CODES.has(code)) {
    return true
  }
  if (code !== 401 && code !== '401') {
    return false
  }
  return isSessionInvalidMessage(message)
}

export function shouldSilenceAuthError() {
  const authStore = useAuthStore()
  return authStore.isLoggingOut || window.location.pathname === '/login'
}

export function isSilentAuthError(error) {
  return Boolean(error?.silentAuthError || (isAuthErrorCode(error?.code) && shouldSilenceAuthError()))
}

/** AiCrudPage 换参/重复 loadList 会 abort 上一次请求，不应弹「诊断详情」 */
export function isCanceledRequest(error) {
  return error?.code === 'ERR_CANCELED'
    || error?.name === 'CanceledError'
    || error?.name === 'AbortError'
    || error?.error?.code === 'ERR_CANCELED'
    || error?.error?.name === 'CanceledError'
    || error?.error?.name === 'AbortError'
}

function resolveErrorTitle(code) {
  switch (code) {
    case 400:
      return '请求参数错误'
    case 403:
      return '请求被拒绝'
    case 404:
      return '请求资源不存在'
    case 500:
      return '服务端异常'
    case 'NETWORK_ERROR':
      return '网络连接失败'
    default:
      return '请求失败'
  }
}

function showCommonErrorTip(code, message, detail) {
  if (window.$dialog?.error) {
    showRequestErrorDialog({
      title: resolveErrorTitle(code),
      code,
      message,
      method: detail?.method,
      url: detail?.url,
      traceId: detail?.traceId,
      detail,
    })
    return
  }
  window.$message?.error(message)
}

export function resolveResError(code, message, needTip = true, detail = null) {
  const isLoginPage = window.location.pathname === '/login'
  const authStore = useAuthStore()

  switch (code) {
    case '-8':
    case 401:
    case '401':
    case 11007:
    case 11008:
      if (authStore.isLoggingOut || isLoginPage) {
        authStore.resetToken()
        return false
      }
      if (shouldForceReLogin(code, message, detail)) {
        if (isConfirming) {
          return false
        }
        isConfirming = true
        if (needTip) {
          window.$message?.error(message || '登录已过期，请重新登录')
        }
        authStore.logout()
        window.setTimeout(() => {
          isConfirming = false
        }, 1200)
        return false
      }
      if (isNonSessionAuthPath(detail?.url) && isSessionInvalidMessage(message)) {
        return false
      }
      message = message || '请求未授权'
      break
    case 403:
      message = message || '请求被拒绝'
      break
    case 404:
      message = message || '请求资源或接口不存在'
      break
    case 500:
      message = message || '服务器发生异常'
      break
    default:
      message = message ?? `【${code}】: 未知异常!`
      break
  }
  if (needTip) {
    showCommonErrorTip(code, message, detail)
  }
  return message
}
