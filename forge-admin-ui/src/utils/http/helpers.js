import { useAuthStore } from '@/store'

let isConfirming = false
export function resolveResError(code, message, needTip = true) {
  // 检查是否在登录页面
  const isLoginPage = window.location.pathname === '/login'

  switch (code) {
    case '-8': // 令牌无效
    case 401:
      // 如果在登录页面，静默清除 token，不弹对话框
      if (isLoginPage) {
        const authStore = useAuthStore()
        authStore.resetToken()
        return false
      }
      if (isConfirming || !needTip) {
        return
      }
      isConfirming = true
      $dialog.confirm({
        title: '提示',
        type: 'info',
        content: message || '登录已过期，是否重新登录？',
        confirm() {
          useAuthStore().logout()
          window.$message?.success('已退出登录')
          isConfirming = false
        },
        cancel() {
          isConfirming = false
        },
      })
      return false
    case 11007:
    case 11008:
      if (isConfirming || !needTip)
        return
      isConfirming = true
      $dialog.confirm({
        title: '提示',
        type: 'info',
        content: `${message}，是否重新登录？`,
        confirm() {
          useAuthStore().logout()
          window.$message?.success('已退出登录')
          isConfirming = false
        },
        cancel() {
          isConfirming = false
        },
      })
      return false
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
  needTip && window.$message?.error(message)
  return message
}
