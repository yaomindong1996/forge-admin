import { nextTick, watch } from 'vue'
import { buildMenuNavigationLocation } from '@/composables/useMenu'
import { canAccessRoute } from '@/router/guards/permission-guard'
import { isApplicationRuntimePath } from '@/router/shell-layout'
import { assistantPageDirectory, isInternalGuidePath } from './assistant-page-context'

const MARK = 'assistant-guide-highlight'
const REGION = '.system-page-layout, .content-area, .content-full, .content-simple, .layout-page-content, .immersive-content, .side-flyout-content, .business-layout-content, .bento-content, .nexus-content'
let currentMark

function isVisible(element) {
  return !!element?.isConnected && !!element.getClientRects().length
    && !['hidden', 'collapse'].includes(getComputedStyle(element).visibility)
    && !element.closest('[hidden], [inert], [aria-hidden="true"]')
}

export function findGuideElement(anchor, root = document) {
  if (!/^[a-z][a-z-]*$/.test(anchor))
    return null
  const matches = [...root.querySelectorAll(`[data-assistant-guide="${anchor}"]`)].filter(element => isVisible(element)
    && !element.matches(':disabled, [aria-disabled="true"]'))
  if (matches.length === 1)
    return matches[0]
  const inDialog = matches.filter(element => element.closest('[role="dialog"]'))
  if (inDialog.length === 1)
    return inDialog[0]
  if (anchor === 'page') {
    const region = [...root.querySelectorAll(REGION)].find(isVisible)
    const heading = region && [...region.querySelectorAll('h1, h2')].find(isVisible)
    return heading || region || matches[0] || null
  }
  return null
}

export function guideLocation(router, permission, user, option) {
  if (!user.userId || !permission.menuDataLoaded)
    return null
  const current = router.currentRoute.value
  if (option.currentScope) {
    return current.fullPath === option.currentScope && isInternalGuidePath(current.path)
      && canAccessRoute(current, permission, user)
      ? current
      : null
  }
  const entry = assistantPageDirectory(permission, user).find(entry => entry.id === option.id)
  if (!entry)
    return null
  const target = router.resolve(buildMenuNavigationLocation(entry.menu.path, entry.menu))
  return target.matched.some(record => !record.path.includes('pathMatch')) && canAccessRoute(target, permission, user) ? target : null
}

function clearMark() {
  currentMark?.cleanup()
}

function markElement(router, isCurrent, element) {
  element.classList.add(MARK)
  element.scrollIntoView?.({ block: 'center', inline: 'nearest' })
  if (element.matches('button, input, a[href], textarea, [tabindex="0"]'))
    element.focus({ preventScroll: true })
  const expire = setTimeout(clearMark, 10_000)
  const validate = () => {
    if (!isCurrent())
      clearMark()
  }
  const removeGuard = router.afterEach(validate)
  const unwatch = watch(isCurrent, validate)
  currentMark.cleanup = () => {
    element.classList.remove(MARK)
    clearTimeout(expire)
    removeGuard()
    unwatch()
    currentMark = null
  }
}

export async function locateAssistantGuide(router, permission, user, option, anchor, onNavigate) {
  const target = guideLocation(router, permission, user, option)
  if (!target)
    return 'unavailable'
  clearMark()
  const pendingCleanup = () => {
    currentMark = null
  }
  const pending = { cleanup: pendingCleanup }
  currentMark = pending
  const scope = () => [user.userId, user.tenantId, user.activeOrgId].join(':')
  const owner = scope()
  const appEntryKey = target.path.startsWith('/app-center/app/') ? target.query.menuKey : null
  const route = () => router.currentRoute.value
  const onRuntime = () => appEntryKey && isApplicationRuntimePath(route().path)
    && (route().query?.menuKey === appEntryKey || route().query?.menuResourceId === appEntryKey)
  const isCurrent = () => scope() === owner && permission.menuDataLoaded
    && (route().fullPath === target.fullPath || Boolean(onRuntime()))
    && canAccessRoute(route(), permission, user)
  onNavigate?.()
  try {
    if (router.currentRoute.value.fullPath !== target.fullPath)
      await router.push(target.fullPath)
    await nextTick()
    // 页面懒加载时，短时间等待明确的定位点。路由或账号发生变化立即停止。
    const until = Date.now() + 3600
    while (currentMark === pending && isCurrent()) {
      const element = !appEntryKey || onRuntime() ? findGuideElement(anchor) : null
      if (element) {
        markElement(router, isCurrent, element)
        return 'marked'
      }
      if (Date.now() >= until)
        break
      await new Promise(resolve => setTimeout(resolve, 100))
    }
    if (currentMark !== pending || !isCurrent())
      return 'unavailable'
    const page = findGuideElement('page')
    if (page)
      markElement(router, isCurrent, page)
    return 'opened'
  }
  finally {
    if (currentMark === pending && currentMark.cleanup === pendingCleanup)
      clearMark()
  }
}
