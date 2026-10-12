import { computed } from 'vue'
import { useAuthStore } from '@/store/modules/auth'
import { useUserStore } from '@/store/modules/user'

// 与现有系统权限来源一致；未知权限失败关闭，不能靠显示菜单推断查询许可。
export function useAiImagePermission() {
  const auth = useAuthStore()
  const user = useUserStore()
  const has = (code) => {
    if (!auth.accessToken || !user.userId || !user.tenantId) {
      return false
    }
    const grants = [user.permissions, user.apiPermissions, user.getDataPermission].flat()
    return user.isAdmin || grants.some(grant => ['**', '*:*:*', code].includes(grant))
  }
  return {
    canQuery: computed(() => has('ai:image:generate')),
    canHistory: computed(() => has('ai:image:page')),
  }
}
