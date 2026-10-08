import { defineStore } from 'pinia'
import { computed } from 'vue'
import { getRuntimeLicenseStatus } from '@/api/system/runtimeLicense'
import { useAuthStore, useUserStore } from '@/store'
import { createRuntimeLicenseState } from './runtimeLicenseState'

export const useRuntimeLicenseStore = defineStore('plugin-runtime-license', () => {
  const user = useUserStore()
  const auth = useAuthStore()
  // 管理员身份不替代查询权限；撤销权限时必须立即清空旧诊断。
  const canQuery = computed(() => !!user.isAdmin && user.permissions.some(
    grant => ['**', '*:*:*', 'system:plugin:list'].includes(grant),
  ))
  // Token 只用于内存请求隔离，不新增存储或返回到页面。
  const identity = () => JSON.stringify([user.userId, user.tenantId, auth.accessToken])
  return { canQuery, ...createRuntimeLicenseState(getRuntimeLicenseStatus, () => canQuery.value, identity) }
})
