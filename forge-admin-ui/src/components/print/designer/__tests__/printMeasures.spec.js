import { describe, expect, it } from 'vitest'
import { PRINT_MM_PRESETS, printMmOptions } from '../printMeasures'

describe('print millimetre selects', () => {
  it('lists numeric presets and keeps the current size', () => {
    const options = printMmOptions(0.5, PRINT_MM_PRESETS.border)
    expect(options.every(item => typeof item.value === 'number' && item.label.endsWith(' mm'))).toBe(true)
    expect(options.some(item => item.value === 0.5 && item.label === '0.5 mm')).toBe(true)
    expect(printMmOptions(0.37, PRINT_MM_PRESETS.border).some(item => item.value === 0.37)).toBe(true)
  })
})
