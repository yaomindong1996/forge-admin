import { PrintError } from '../protocol/types'

export const GENERIC_FONT_FAMILIES = new Set(['serif', 'sans-serif', 'monospace', 'cursive', 'fantasy', 'system-ui'])

export function fontFamilyNames(family) {
  return String(family || '')
    .split(',')
    .map(item => item.trim().replace(/^["']|["']$/g, ''))
    .filter(Boolean)
}

export function hasGenericFontFallback(family) {
  return fontFamilyNames(family).some(name => GENERIC_FONT_FAMILIES.has(name.toLowerCase()))
}

/**
 * Accept a font stack if any named local face loads, or if CSS has a generic fallback.
 * Do not fail the whole template because the first name (e.g. STHeiti) is missing on this OS.
 */
export async function requireLocalFont(family, FontFaceType = globalThis.FontFace) {
  const names = fontFamilyNames(family)
  const named = names.filter(name => !GENERIC_FONT_FAMILIES.has(name.toLowerCase()))
  if (!named.length)
    return
  if (!FontFaceType)
    throw new PrintError('FONT_CHECK_UNAVAILABLE', '浏览器不支持打印字体校验', 'fonts')
  for (const primary of named) {
    try {
      const face = new FontFaceType('ForgePrintLocalFontCheck', `local("${primary}")`)
      await face.load()
      return
    }
    catch {
      /* try the next named face in the stack */
    }
  }
  if (hasGenericFontFallback(family))
    return
  throw new PrintError('FONT_UNAVAILABLE', `打印字体未安装：${named[0]}`, 'fonts')
}
