import { PrintError } from '../protocol/types'

export function fillPageNumbers(elements, number, total, measure) {
  return elements.map((element) => {
    let text
    if (element.type === 'PAGE_NUMBER') {
      text = element.pageNumberFormat === 'CURRENT' ? String(number) : `${number} / ${total}`
    }
    else if (element.binding?.source === 'SYSTEM' && element.binding.path === 'system.pageNumber') {
      text = String(number)
    }
    else if (element.binding?.source === 'SYSTEM' && element.binding.path === 'system.totalPages') {
      text = String(total)
    }
    if (text === undefined) {
      return element
    }
    const measured = measure.text(text, element.widthMm, element.style)
    if (measured.heightMm > element.heightMm + 0.0001) {
      throw new PrintError('PAGE_NUMBER_OVERFLOW', '页码槽尺寸不足', element.id)
    }
    return { ...element, ...measured, heightMm: element.heightMm, text }
  })
}
