export function findScrollParent(element) {
  let parent = element?.parentElement
  while (parent && parent !== document.documentElement) {
    const overflowY = getComputedStyle(parent).overflowY
    if ((overflowY === 'auto' || overflowY === 'scroll') && parent.scrollHeight > parent.clientHeight + 1)
      return parent
    parent = parent.parentElement
  }
  return null
}

export function scrollElementIntoScroller(target, offset = 0) {
  if (!target)
    return false
  const scroller = findScrollParent(target)
  if (!scroller) {
    target.scrollIntoView({ behavior: 'smooth', block: 'start', inline: 'nearest' })
    return true
  }
  const nextTop = scroller.scrollTop
    + target.getBoundingClientRect().top
    - scroller.getBoundingClientRect().top
    - offset
  scroller.scrollTo({ top: Math.max(0, nextTop), behavior: 'smooth' })
  return true
}

export function keepChildInHorizontalView(container, child, padding = 8) {
  if (!container || !child)
    return
  const left = child.offsetLeft
  const right = left + child.offsetWidth
  if (left < container.scrollLeft)
    container.scrollLeft = Math.max(0, left - padding)
  else if (right > container.scrollLeft + container.clientWidth)
    container.scrollLeft = right - container.clientWidth + padding
}
