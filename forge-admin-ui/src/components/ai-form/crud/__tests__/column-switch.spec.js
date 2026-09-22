import { describe, expect, it } from 'vitest'
import {
  isSwitchColumnConfig,
  normalizeSwitchCellValue,
  resolveSwitchColumnTexts,
  resolveSwitchColumnValuePair,
} from '../column-switch'

describe('column-switch', () => {
  it('detects switch from render.type / componentType / editSchema', () => {
    expect(isSwitchColumnConfig({ render: { type: 'switch' } })).toBe(true)
    expect(isSwitchColumnConfig({ renderType: 'switch' })).toBe(true)
    expect(isSwitchColumnConfig({ componentType: 'switch' })).toBe(true)
    expect(isSwitchColumnConfig({ prop: 'enabled' }, { type: 'switch' })).toBe(true)
    expect(isSwitchColumnConfig({ prop: 'name' }, { type: 'input' })).toBe(false)
  })

  it('normalizes 0/1 string cell values to numeric pair', () => {
    expect(normalizeSwitchCellValue('1', 1, 0)).toBe(1)
    expect(normalizeSwitchCellValue('0', 1, 0)).toBe(0)
    expect(normalizeSwitchCellValue(null, 1, 0)).toBe(0)
  })

  it('resolves checked/unchecked pair and texts from column render', () => {
    const pair = resolveSwitchColumnValuePair({
      render: { type: 'switch', checkedValue: 1, uncheckedValue: 0, checkedText: '开', uncheckedText: '关' },
    })
    expect(pair).toEqual({ checkedValue: 1, uncheckedValue: 0 })
    expect(resolveSwitchColumnTexts({
      render: { type: 'switch', checkedText: '开', uncheckedText: '关' },
    })).toEqual({ checkedText: '开', uncheckedText: '关' })
  })
})
