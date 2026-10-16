import { describe, expect, it } from 'vitest'
import { colorToHexInput, hexInputToColor } from '../designer-color'

describe('designer-color', () => {
  it('解析 #hex / 短 hex', () => {
    expect(colorToHexInput('#fbdbdb', '')).toBe('FBDBDB')
    expect(colorToHexInput('fbdbdb', '')).toBe('FBDBDB')
    expect(colorToHexInput('#abc', '')).toBe('AABBCC')
  })

  it('解析 hsl / hsla（现代空格语法）', () => {
    expect(colorToHexInput('hsl(0 78% 92%)', '')).toBe('FBDBDB')
    expect(colorToHexInput('hsl(0, 78%, 92%)', '')).toBe('FBDBDB')
    expect(colorToHexInput('hsla(0 70% 45% / 0)', 'E4E4E7')).toBe('E4E4E7')
  })

  it('解析 rgb / rgba', () => {
    expect(colorToHexInput('rgb(251, 219, 219)', '')).toBe('FBDBDB')
    expect(colorToHexInput('rgba(195, 34, 34, 1)', '')).toBe('C32222')
    expect(colorToHexInput('rgba(0,0,0,0)', 'E4E4E7')).toBe('E4E4E7')
  })

  it('transparent 回退', () => {
    expect(colorToHexInput('transparent', '')).toBe('')
    expect(colorToHexInput('transparent', 'E4E4E7')).toBe('E4E4E7')
  })

  it('hexInputToColor 规范化写入', () => {
    expect(hexInputToColor('FBDBDB')).toBe('#FBDBDB')
    expect(hexInputToColor('#abc').toLowerCase()).toBe('#aabbcc')
    expect(hexInputToColor('oops', '#ffffff')).toBe('#ffffff')
  })
})
