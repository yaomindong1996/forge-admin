import { applyBusinessFieldPropertyPanelPart1 } from './useBusinessFieldPropertyPanel.part1.js'
import { applyBusinessFieldPropertyPanelPart2 } from './useBusinessFieldPropertyPanel.part2.js'

export function useBusinessFieldPropertyPanel(props, emit, expose) {
  let api = applyBusinessFieldPropertyPanelPart1(props, emit, expose)
  api = applyBusinessFieldPropertyPanelPart2(props, emit, expose, api)
  // part2 已覆盖 __impl；再跑首次 resetForm，等价于原 immediate watch
  if (api.mut)
    api.mut.partsReady = true
  api.__impl?.resetForm?.()
  const { __impl, mut, props: _p, emit: _e, ...publicApi } = api
  return publicApi
}
