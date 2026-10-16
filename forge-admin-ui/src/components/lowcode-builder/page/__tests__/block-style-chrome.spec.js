import { describe, expect, it } from 'vitest'
import {
  isAccidentalSelectionChrome,
  selectionChromeClearPatch,
  stripAccidentalSelectionChrome,
} from '../block-style-chrome.js'

describe('block-style-chrome', () => {
  it('detects Naive selection chrome pair', () => {
    expect(isAccidentalSelectionChrome({
      borderColor: '#2080f0',
      borderWidth: 2,
      backgroundColor: '#f3f8ff',
    })).toBe(true)
  })

  it('detects rgb form and transparent bg', () => {
    expect(isAccidentalSelectionChrome({
      borderColor: 'rgb(32, 128, 240)',
      borderWidth: 2,
      backgroundColor: 'transparent',
    })).toBe(true)
  })

  it('ignores intentional gray borders', () => {
    expect(isAccidentalSelectionChrome({
      borderColor: '#e4e4e7',
      borderWidth: 1,
      backgroundColor: '#fff',
    })).toBe(false)
  })

  it('strips chrome for readonly render', () => {
    const next = stripAccidentalSelectionChrome({
      borderColor: '#2563eb',
      borderWidth: 2,
      borderStyle: 'solid',
      backgroundColor: '#fafdff',
      padding: 12,
    })
    expect(next.borderWidth).toBe(0)
    expect(next.borderStyle).toBe('none')
    expect(next.borderColor).toBe('transparent')
    expect(next.backgroundColor).toBe('transparent')
    expect(next.padding).toBe(12)
  })

  it('clear patch zeros selection chrome fields', () => {
    expect(selectionChromeClearPatch()).toEqual({
      borderColor: 'transparent',
      borderWidth: 0,
      borderStyle: 'none',
      backgroundColor: 'transparent',
    })
  })
})
