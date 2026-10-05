// 触控板横向事件交给浏览器；普通纵向滚轮仅在菜单还能移动时转换，边界不阻塞页面。
export function scrollMenuWithWheel(event) {
  if (event.ctrlKey || event.shiftKey || Math.abs(event.deltaX) >= Math.abs(event.deltaY)) {
    return false
  }
  const track = event.currentTarget
  const maximum = Math.max(0, track.scrollWidth - track.clientWidth)
  const unit = event.deltaMode === 1 ? 16 : event.deltaMode === 2 ? track.clientWidth : 1
  const next = Math.min(maximum, Math.max(0, track.scrollLeft + event.deltaY * unit))
  if (next === track.scrollLeft) {
    return false
  }
  track.scrollLeft = next
  event.preventDefault()
  return true
}
