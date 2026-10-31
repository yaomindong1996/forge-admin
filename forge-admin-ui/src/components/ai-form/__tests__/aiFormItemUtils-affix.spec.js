import { describe, expect, it } from 'vitest'
import {
  resolveFieldAffix,
  resolveFieldIcon,
  resolveFieldSlotText,
  resolveInputFieldType,
  resolveNumberFieldPrefix,
  resolveNumberFieldSuffix,
} from '../aiFormItemUtils'

describe('field affix helpers', () => {
  it('reads prefix / suffix from props', () => {
    expect(resolveFieldAffix({ props: { prefix: '￥' } }, 'prefix')).toBe('￥')
    expect(resolveNumberFieldSuffix({ props: { suffix: '%' } })).toBe('%')
  })

  it('falls back to money currencySymbol for prefix', () => {
    expect(resolveNumberFieldPrefix(
      { props: { currencySymbol: '$' } },
      () => 'money',
    )).toBe('$')
    expect(resolveNumberFieldPrefix(
      { props: { prefix: '￥', currencySymbol: '$' } },
      () => 'money',
    )).toBe('￥')
  })

  it('reads icon / slot text / password inputType', () => {
    expect(resolveFieldIcon({ props: { clearIcon: 'close-circle' } }, 'clearIcon')).toBe('close-circle')
    expect(resolveFieldSlotText({ props: { dropdownHeader: '常用' } }, 'dropdownHeader')).toBe('常用')
    expect(resolveInputFieldType({ props: { inputType: 'password' } })).toBe('password')
    expect(resolveInputFieldType({ props: { type: 'password' } })).toBe('password')
    expect(resolveInputFieldType({ type: 'password' })).toBe('password')
    expect(resolveInputFieldType({ props: { type: 'text' } })).toBe('text')
    expect(resolveInputFieldType({ props: {} })).toBe('text')
  })
})
