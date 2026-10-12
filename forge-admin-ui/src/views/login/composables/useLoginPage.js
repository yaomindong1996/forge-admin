import { computed, ref } from 'vue'
import { useAppStore } from '@/store'
import { buildLoginThemeOverrides } from '../loginTheme'
import { applyLoginPagePart1 } from './useLoginPage.part1.js'

export function useLoginPage() {
  const appStore = useAppStore()
  const api = applyLoginPagePart1()
  const { __impl, mut, ...publicApi } = api
  // 跟随 loginConfig / 租户主题写入的 appStore 主色，不再写死登录蓝
  const loginThemeOverrides = computed(() =>
    buildLoginThemeOverrides(appStore.naiveThemeOverrides?.common || {}),
  )
  return { ...publicApi, supportVisible: ref(false), loginThemeOverrides }
}
