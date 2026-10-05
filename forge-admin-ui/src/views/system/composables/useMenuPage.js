import { applyMenuPagePart1 } from './useMenuPage.part1.js'
import { applyMenuPagePart2 } from './useMenuPage.part2.js'
import { useMenuWorkspace } from './useMenuWorkspace'

export function useMenuPage() {
  let api = applyMenuPagePart1()
  api = applyMenuPagePart2(api)
  const { __impl, mut, ...publicApi } = api
  return { ...publicApi, ...useMenuWorkspace(publicApi) }
}
