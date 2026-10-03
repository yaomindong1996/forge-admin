import { ref } from 'vue'
import { loginThemeOverrides } from '../loginTheme'
import { applyLoginPagePart1 } from './useLoginPage.part1.js'

export function useLoginPage() {
  const api = applyLoginPagePart1()
  const { __impl, mut, ...publicApi } = api
  return { ...publicApi, supportVisible: ref(false), loginThemeOverrides }
}
