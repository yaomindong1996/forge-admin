/**
 * Common print-safe fonts for the designer (system + widely available CJK).
 * Values omit quotes so protocol validation (`fontFamily`) accepts them.
 */
export const PRINT_FONT_OPTIONS = [
  { label: '微软雅黑', value: 'Microsoft YaHei, PingFang SC, Hiragino Sans GB, sans-serif' },
  { label: '黑体', value: 'SimHei, Heiti SC, Hiragino Sans GB, sans-serif' },
  { label: '宋体', value: 'SimSun, Songti SC, STSong, serif' },
  { label: '楷体', value: 'KaiTi, Kaiti SC, STKaiti, serif' },
  { label: '仿宋', value: 'FangSong, STFangsong, serif' },
  { label: '苹方', value: 'PingFang SC, Microsoft YaHei, sans-serif' },
  { label: '冬青黑体', value: 'Hiragino Sans GB, PingFang SC, sans-serif' },
  { label: '华文黑体', value: 'Heiti SC, STHeiti, SimHei, sans-serif' },
  { label: '华文楷体', value: 'Kaiti SC, STKaiti, KaiTi, serif' },
  { label: '华文宋体', value: 'Songti SC, STSong, SimSun, serif' },
  { label: 'Arial', value: 'Arial, Helvetica, sans-serif' },
  { label: 'Helvetica', value: 'Helvetica, Arial, sans-serif' },
  { label: 'Times New Roman', value: 'Times New Roman, Times, serif' },
  { label: 'Georgia', value: 'Georgia, serif' },
  { label: 'Verdana', value: 'Verdana, sans-serif' },
  { label: 'Tahoma', value: 'Tahoma, sans-serif' },
  { label: 'Courier New', value: 'Courier New, Courier, monospace' },
]

export const DEFAULT_PRINT_FONT = 'Microsoft YaHei, PingFang SC, sans-serif'

/** Word-like presets; current custom sizes are appended so existing templates still display. */
export const PRINT_FONT_SIZE_PRESETS = [6, 7, 8, 9, 10, 11, 12, 14, 16, 18, 20, 22, 24, 26, 28, 36, 48, 72]

export function printFontSizeOptions(current) {
  const sizes = [...PRINT_FONT_SIZE_PRESETS]
  const n = Number(current)
  if (Number.isFinite(n) && !sizes.includes(n))
    sizes.push(n)
  sizes.sort((a, b) => a - b)
  return sizes.map(value => ({ label: `${value} pt`, value }))
}

export function matchPrintFontOption(value) {
  if (!value)
    return PRINT_FONT_OPTIONS[0]
  const primary = value.split(',')[0].trim()
  return PRINT_FONT_OPTIONS.find((item) => {
    if (item.value === value || item.value.startsWith(value) || value.startsWith(item.value.split(',')[0].trim()))
      return true
    return item.value.split(',').map(name => name.trim()).includes(primary)
  }) || null
}

export function printFontLabel(value) {
  return matchPrintFontOption(value)?.label || (value ? value.split(',')[0].trim() : '微软雅黑')
}

export function printFontSelectValue(value) {
  return matchPrintFontOption(value)?.value || value || DEFAULT_PRINT_FONT
}
