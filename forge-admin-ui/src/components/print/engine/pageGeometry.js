import { PRINT_LIMITS, PrintError } from '../protocol/types'

export const FIT_EPSILON = 0.0001

export function createPageCursor(geometry) {
  const pages = [{ fragments: [] }]
  let usedMm = 0
  return {
    pages,
    get remainingMm() { return Math.max(0, geometry.contentHeightMm - usedMm) },
    get capacityMm() { return geometry.contentHeightMm },
    get usedMm() { return usedMm },
    next() {
      if (pages.length >= PRINT_LIMITS.pages) {
        throw new PrintError('PAGE_LIMIT', `打印页数超过 ${PRINT_LIMITS.pages} 页`)
      }
      pages.push({ fragments: [] })
      usedMm = 0
    },
    forceBreak() {
      this.next()
    },
    ensure(heightMm, path) {
      if (!Number.isFinite(heightMm) || heightMm <= 0 || heightMm > geometry.contentHeightMm + FIT_EPSILON) {
        throw new PrintError('ELEMENT_TOO_TALL', '内容高度超过单页正文，请调整纸张、字号或内容', path)
      }
      if (heightMm > this.remainingMm + FIT_EPSILON) {
        this.next()
      }
    },
    place(fragment) {
      this.ensure(fragment.heightMm, fragment.id)
      pages.at(-1).fragments.push({ ...fragment, xMm: geometry.bodyLeftMm, yMm: geometry.bodyTopMm + usedMm })
      usedMm += fragment.heightMm
    },
    gap(heightMm) {
      usedMm = Math.min(geometry.contentHeightMm, usedMm + heightMm)
    },
  }
}
