import { FIT_EPSILON } from './pageGeometry'

export function paginateText(section, cursor) {
  const { lines, lineHeightMm, insetMm } = section
  cursor.ensure(lineHeightMm + 2 * insetMm, section.id)
  let offset = 0
  while (offset < lines.length) {
    const available = Math.floor((cursor.remainingMm - 2 * insetMm + FIT_EPSILON) / lineHeightMm)
    if (available < 1) {
      cursor.next()
      continue
    }
    const count = Math.min(available, lines.length - offset)
    cursor.place({ ...section, lines: lines.slice(offset, offset + count), lineOffset: offset, heightMm: count * lineHeightMm + 2 * insetMm })
    offset += count
  }
}
