const ENHANCED_STATE = new WeakMap()
const AUTO_ENHANCED = new WeakSet()
let globalObserver = null
let globalScanTimer = null

const INTERACTIVE_SELECTOR = [
  'a',
  'button',
  'input',
  'textarea',
  'select',
  '[contenteditable="true"]',
  '[role="button"]',
  '[role="checkbox"]',
  '[role="radio"]',
  '[role="switch"]',
  '.n-button',
  '.n-checkbox',
  '.n-radio',
  '.n-switch',
  '.n-input',
  '.n-input-number',
  '.n-select',
  '.n-dropdown',
  '.n-pagination',
  '.n-data-table-expand-trigger',
  '.n-data-table-expand-icon',
  '.n-data-table-resize-button',
  '.n-data-table-filter',
  '.n-scrollbar-rail',
  '[data-data-table-filter]',
  '[data-data-table-resizable]',
].join(',')

const SCROLL_CONTAINER_SELECTORS = [
  '.n-data-table-base-table-header',
  '.n-data-table-base-table-body',
  '.n-data-table-wrapper',
  '.n-scrollbar-container',
  '.n-scrollbar-content',
]

const VERTICAL_SCROLL_CONTAINER_SELECTORS = [
  '.n-data-table-base-table-body .n-scrollbar-container',
  '.n-data-table-base-table-body',
  '.n-data-table-wrapper',
]

function resolveOptions(value) {
  if (value === false) {
    return { enabled: false }
  }
  if (value && typeof value === 'object') {
    return {
      enabled: value.enabled !== false,
      threshold: Number.isFinite(value.threshold) ? value.threshold : 4,
    }
  }
  return { enabled: true, threshold: 4 }
}

function isInteractiveTarget(target) {
  return target instanceof Element && !!target.closest(INTERACTIVE_SELECTOR)
}

function isScrollableX(element) {
  if (!(element instanceof HTMLElement)) {
    return false
  }
  if (element.scrollWidth <= element.clientWidth + 1) {
    return false
  }
  const style = window.getComputedStyle(element)
  return /(auto|scroll|overlay)/.test(style.overflowX) || element.classList.contains('n-data-table-wrapper')
}

function findScrollableAncestor(target, root) {
  let current = target instanceof HTMLElement ? target : null
  while (current && current !== root.parentElement) {
    if (isScrollableX(current)) {
      return current
    }
    current = current.parentElement
  }
  return null
}

function findScrollableDescendant(root) {
  if (isScrollableX(root)) {
    return root
  }
  const preferredSelectors = [
    '.n-data-table-base-table-body',
    '.n-data-table-wrapper',
    '.n-scrollbar-container',
    '.n-scrollbar-content',
  ]
  for (const selector of preferredSelectors) {
    const matched = Array.from(root.querySelectorAll(selector)).find(isScrollableX)
    if (matched) {
      return matched
    }
  }
  return Array.from(root.querySelectorAll('*')).find(isScrollableX) || null
}

function resolveScrollContainer(root, target) {
  return findScrollableAncestor(target, root) || findScrollableDescendant(root)
}

function isDisabledByAttribute(el) {
  return el?.dataset?.tableDragScroll === 'disabled'
}

function resolveTableRoot(root) {
  return root.closest?.('.n-data-table') || root
}

function collectScrollContainers(root) {
  const tableRoot = resolveTableRoot(root)
  const containers = new Set([tableRoot])

  SCROLL_CONTAINER_SELECTORS.forEach((selector) => {
    tableRoot.querySelectorAll(selector).forEach(container => containers.add(container))
  })

  return Array.from(containers).filter(isScrollableX)
}

function syncTableScrollLeft(root, source, state) {
  const containers = collectScrollContainers(root)
  const changedContainers = []

  if (state) {
    state.syncing = true
  }

  containers.forEach((container) => {
    if (container === source) {
      return
    }
    if (Math.abs(container.scrollLeft - source.scrollLeft) <= 1) {
      return
    }
    container.scrollLeft = source.scrollLeft
    changedContainers.push(container)
  })

  changedContainers.forEach(dispatchScrollEvent)

  if (state) {
    state.syncing = false
  }
}

function unbindScrollListeners(state) {
  if (!state?.scrollContainers?.length || !state.onScroll) {
    return
  }
  state.scrollContainers.forEach(container => container.removeEventListener('scroll', state.onScroll))
  state.scrollContainers = []
}

function bindScrollListeners(root, state) {
  unbindScrollListeners(state)
  state.onScroll = state.onScroll || ((event) => {
    if (state.syncing || state.dragging) {
      return
    }
    const source = event.currentTarget
    if (!(source instanceof HTMLElement)) {
      return
    }
    syncTableScrollLeft(root, source, state)
  })
  state.scrollContainers = collectScrollContainers(root)
  state.scrollContainers.forEach(container => container.addEventListener('scroll', state.onScroll, { passive: true }))
}

function updateOverflowState(root) {
  const tableRoot = resolveTableRoot(root)
  const verticalContainer = VERTICAL_SCROLL_CONTAINER_SELECTORS
    .map(selector => tableRoot.querySelector(selector))
    .find(Boolean)

  const hasVerticalOverflow = verticalContainer
    ? verticalContainer.scrollHeight > verticalContainer.clientHeight + 1
    : false

  tableRoot.classList.toggle('forge-table-no-y-overflow', !hasVerticalOverflow)
}

function refreshTableState(root) {
  updateOverflowState(root)

  const scrollContainer = findScrollableDescendant(root)
  if (!scrollContainer) {
    return
  }

  const state = ENHANCED_STATE.get(root)
  syncTableScrollLeft(root, scrollContainer, state)
  dispatchScrollEvent(scrollContainer)
  const header = resolveTableRoot(root).querySelector('.n-data-table-base-table-header')
  if (header) {
    dispatchScrollEvent(header)
  }
}

function dispatchScrollEvent(element) {
  const EventConstructor = element.ownerDocument?.defaultView?.Event || Event
  element.dispatchEvent(new EventConstructor('scroll', { bubbles: true }))
}

function scheduleTableStateRefresh(root, state) {
  if (state.refreshTimer) {
    window.clearTimeout(state.refreshTimer)
  }
  state.refreshTimer = window.setTimeout(() => {
    state.refreshTimer = null
    bindScrollListeners(root, state)
    refreshTableState(root)
  }, 0)
}

function cleanup(el) {
  const state = ENHANCED_STATE.get(el)
  if (!state) {
    return
  }
  if (state.refreshTimer) {
    window.clearTimeout(state.refreshTimer)
  }
  unbindScrollListeners(state)
  el.removeEventListener('mousedown', state.onMouseDown, true)
  el.removeEventListener('click', state.onClickCapture, true)
  document.removeEventListener('mousemove', state.onMouseMove, true)
  document.removeEventListener('mouseup', state.onMouseUp, true)
  el.classList.remove('forge-table-scroll-enhanced', 'is-table-dragging', 'forge-table-no-y-overflow')
  state.scrollContainer?.classList.remove('is-table-dragging')
  if (state.dragging) {
    document.body.style.userSelect = state.previousUserSelect || ''
  }
  ENHANCED_STATE.delete(el)
}

function bindTableScrollEnhance(el, value) {
  cleanup(el)

  const options = resolveOptions(value)
  if (!options.enabled || isDisabledByAttribute(el)) {
    return
  }

  const state = {
    dragging: false,
    moved: false,
    startX: 0,
    startScrollLeft: 0,
    scrollContainer: null,
    previousUserSelect: '',
    preventClick: false,
    refreshTimer: null,
    scrollContainers: [],
    onScroll: null,
    syncing: false,
  }

  state.onMouseDown = (event) => {
    if (event.button !== 0 || isInteractiveTarget(event.target)) {
      return
    }
    const scrollContainer = resolveScrollContainer(el, event.target)
    if (!scrollContainer) {
      return
    }

    state.dragging = true
    state.moved = false
    state.startX = event.clientX
    state.startScrollLeft = scrollContainer.scrollLeft
    state.scrollContainer = scrollContainer
    state.previousUserSelect = document.body.style.userSelect

    document.body.style.userSelect = 'none'
    el.classList.add('is-table-dragging')
    scrollContainer.classList.add('is-table-dragging')
    document.addEventListener('mousemove', state.onMouseMove, true)
    document.addEventListener('mouseup', state.onMouseUp, true)
  }

  state.onMouseMove = (event) => {
    if (!state.dragging || !state.scrollContainer) {
      return
    }
    const deltaX = event.clientX - state.startX
    if (!state.moved && Math.abs(deltaX) < options.threshold) {
      return
    }
    state.moved = true
    state.preventClick = true
    state.scrollContainer.scrollLeft = state.startScrollLeft - deltaX
    syncTableScrollLeft(el, state.scrollContainer, state)
    event.preventDefault()
  }

  state.onMouseUp = () => {
    if (!state.dragging) {
      return
    }
    state.dragging = false
    document.body.style.userSelect = state.previousUserSelect || ''
    document.removeEventListener('mousemove', state.onMouseMove, true)
    document.removeEventListener('mouseup', state.onMouseUp, true)
    el.classList.remove('is-table-dragging')
    state.scrollContainer?.classList.remove('is-table-dragging')
    state.scrollContainer = null
    if (state.preventClick) {
      window.setTimeout(() => {
        state.preventClick = false
      }, 0)
    }
  }

  state.onClickCapture = (event) => {
    if (!state.preventClick) {
      return
    }
    state.preventClick = false
    event.preventDefault()
    event.stopImmediatePropagation()
  }

  el.classList.add('forge-table-scroll-enhanced')
  el.addEventListener('mousedown', state.onMouseDown, true)
  el.addEventListener('click', state.onClickCapture, true)
  ENHANCED_STATE.set(el, state)
  scheduleTableStateRefresh(el, state)
}

function scanAndEnhanceTables(root = document) {
  if (!root?.querySelectorAll) {
    return
  }
  root.querySelectorAll('.n-data-table').forEach((table) => {
    if (AUTO_ENHANCED.has(table) || isDisabledByAttribute(table)) {
      return
    }
    AUTO_ENHANCED.add(table)
    bindTableScrollEnhance(table, { enabled: true })
  })
}

function scheduleGlobalScan(root = document) {
  if (globalScanTimer) {
    window.clearTimeout(globalScanTimer)
  }
  globalScanTimer = window.setTimeout(() => {
    globalScanTimer = null
    scanAndEnhanceTables(root)
  }, 50)
}

export function setupGlobalTableScrollEnhance() {
  if (globalObserver || typeof window === 'undefined') {
    return
  }
  scheduleGlobalScan()
  globalObserver = new MutationObserver((mutations) => {
    for (const mutation of mutations) {
      if (mutation.type !== 'childList' || mutation.addedNodes.length === 0) {
        continue
      }
      scheduleGlobalScan(document)
      break
    }
  })
  globalObserver.observe(document.body, { childList: true, subtree: true })
}

export default {
  mounted(el, binding) {
    bindTableScrollEnhance(el, binding.value)
  },
  updated(el, binding) {
    if (binding.value !== binding.oldValue || isDisabledByAttribute(el)) {
      bindTableScrollEnhance(el, binding.value)
      return
    }
    const state = ENHANCED_STATE.get(el)
    if (state) {
      scheduleTableStateRefresh(el, state)
    }
  },
  unmounted(el) {
    cleanup(el)
  },
}
