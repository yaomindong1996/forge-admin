import { describe, expect, it } from 'vitest'
import {
  resolveFieldAffix,
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
})
