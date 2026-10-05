import { ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api'
import { useAuthStore } from '@/store'
import { isSilentAuthError } from '@/utils/http/helpers'

// 顶栏与紧凑账户面板共用同一注销流程，门户可提供应用内资料路由。
export function useAccountActions(profileRoute = () => null) {
  const router = useRouter()
  const authStore = useAuthStore()
  const signingOut = ref(false)

  function openProfile() {
    const target = profileRoute()
    const resolved = typeof target === 'function' ? target() : target
    return router.push(resolved || '/profile')
  }

  async function logout() {
    if (signingOut.value) {
      return
    }
    signingOut.value = true
    try {
      authStore.beginLogout()
      try {
        await api.logout()
      }
      catch (error) {
        if (!isSilentAuthError(error)) {
          console.error('logout error', error)
        }
      }
      authStore.logout()
      window.$message.success('已退出登录')
    }
    finally {
      signingOut.value = false
    }
  }

  function confirmLogout() {
    window.$dialog.confirm({
      'title': '提示',
      'type': 'info',
      'content': '确认退出？',
      'positive-button-props': { type: 'primary' },
      'confirm': logout,
    })
  }

  function handleAccountAction(key) {
    if (key === 'profile') {
      return openProfile()
    }
    if (key === 'logout') {
      confirmLogout()
    }
  }

  return { openProfile, confirmLogout, handleAccountAction }
}
