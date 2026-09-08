import { describe, expect, it } from 'vitest'
import { getTransitionEparchyCode } from '../user'

describe('user staff helpers', () => {
  it('keeps an existing OA region code unchanged', () => {
    expect(getTransitionEparchyCode('150100')).toBe('150100')
  })

  it('supports OA region objects and empty values', () => {
    expect(getTransitionEparchyCode({ code: 150100 })).toBe('150100')
    expect(getTransitionEparchyCode(null)).toBe('')
  })
})
