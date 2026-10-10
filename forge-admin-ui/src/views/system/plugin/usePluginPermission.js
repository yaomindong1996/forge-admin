import { useUserStore } from '@/store'

export function usePluginPermission() {
  const user = useUserStore()
  return code => user.isAdmin || user.permissions.some(grant => ['**', '*:*:*', code].includes(grant))
}
