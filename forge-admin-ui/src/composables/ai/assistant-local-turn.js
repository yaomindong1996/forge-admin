import { findAssistantGuide } from './assistant-page-context'

/** 社区版本地回合：仅拦截菜单/操作定位问答，不走智能体路由。 */
export function assistantLocalTurn(text, { page, entries = [], can = () => false } = {}) {
  const guide = findAssistantGuide(text, page, entries, can)
  if (!guide)
    return { scope: page?.scope || '', resolve: null }
  return {
    scope: page?.scope || '',
    resolve: () => ({ guide }),
  }
}
