import { describe, expect, it, vi } from 'vitest'
import { createBrowserPrintSession } from '../browserPrint'
import {
  CALIBRATION_CHECKS,
  CALIBRATION_PAPERS,
  CALIBRATION_STORAGE_KEY,
  calibrationProfileKey,
  createPrintCalibrationResult,
  detectPrintCapabilities,
  readCalibrationAcceptance,
  resolveCalibrationPaper,
  writeCalibrationAcceptance,
} from '../printCalibration'

function memoryStorage(initial = {}) {
  const values = new Map(Object.entries(initial))
  return {
    getItem: vi.fn(key => values.get(key) ?? null),
    setItem: vi.fn((key, value) => values.set(key, value)),
  }
}

describe('print calibration', () => {
  it.each(Object.entries(CALIBRATION_PAPERS))('resolves %s in portrait and landscape millimetres', (preset, source) => {
    expect(resolveCalibrationPaper({ preset, orientation: 'PORTRAIT' })).toMatchObject({
      widthMm: source.widthMm,
      heightMm: source.heightMm,
      orientationLabel: '纵向',
    })
    expect(resolveCalibrationPaper({ preset, orientation: 'LANDSCAPE' })).toMatchObject({
      widthMm: source.heightMm,
      heightMm: source.widthMm,
      orientationLabel: '横向',
    })
  })

  it('normalizes a custom paper and rejects sizes that cannot contain the rulers', () => {
    expect(resolveCalibrationPaper({ preset: 'CUSTOM', orientation: 'LANDSCAPE', widthMm: 360, heightMm: 180 })).toMatchObject({
      widthMm: 360,
      heightMm: 180,
      baseWidthMm: 180,
      baseHeightMm: 360,
    })
    expect(() => resolveCalibrationPaper({ preset: 'CUSTOM', widthMm: 120, heightMm: 200 })).toThrow(/148/)
    expect(() => resolveCalibrationPaper({ preset: 'LETTER' })).toThrow(/不支持/)
  })

  it('builds a frozen paper with an exact 10mm frame and two 100mm rulers', () => {
    const result = createPrintCalibrationResult({ preset: 'A5', orientation: 'LANDSCAPE' })
    expect(result.geometry).toEqual({ widthMm: 210, heightMm: 148 })
    expect(result.dataMode).toBe('CALIBRATION')
    const elements = result.pages[0].fragments[0].elements
    expect(elements.find(item => item.id === 'calibration-margin-frame')).toMatchObject({ xMm: 10, yMm: 10, widthMm: 190, heightMm: 128 })
    expect(elements.find(item => item.id === 'calibration-horizontal-ruler')).toMatchObject({ widthMm: 100 })
    expect(elements.find(item => item.id === 'calibration-vertical-ruler')).toMatchObject({ heightMm: 100 })
    expect(elements.find(item => item.id === 'calibration-paper').text).toContain('A5 · 横向 · 210 × 148 mm')
    expect(Object.isFrozen(result.pages[0].fragments[0].elements)).toBe(true)
  })

  it('does not infer physical success from a print function', () => {
    const printOnly = detectPrintCapabilities({ print() {} })
    expect(printOnly.printFunction).toBe(true)
    expect(printOnly.dialogAvailable).toBe(false)
    expect(printOnly.physicalOutputConfirmed).toBe(false)

    const ready = detectPrintCapabilities({
      print() {},
      document: { body: {}, createElement() {} },
      CSS: { supports: () => true },
    })
    expect(ready).toMatchObject({ dialogAvailable: true, millimetreCss: true, physicalOutputConfirmed: false, status: 'READY' })
  })

  it('prints calibration through the isolated iframe and exact page CSS', async () => {
    const result = createPrintCalibrationResult({ preset: 'B5', orientation: 'LANDSCAPE' })
    const event = vi.fn()
    const session = await createBrowserPrintSession(result, { onEvent: event })
    const frame = document.querySelector('iframe[data-forge-print]')
    expect(frame.contentDocument.querySelector('style').textContent).toContain('@page { size: 250mm 176mm; margin: 0; }')
    expect(frame.contentDocument.querySelector('[data-print-element="calibration-margin-frame"]')).not.toBeNull()
    frame.contentWindow.focus = vi.fn()
    frame.contentWindow.print = vi.fn()
    session.print()
    expect(event).toHaveBeenCalledWith({ result: 'DIALOG_OPENED', pageCount: 1 })
    expect(event.mock.calls.flat().join(' ')).not.toContain('SUCCESS')
    frame.contentWindow.dispatchEvent(new Event('afterprint'))
    expect(document.querySelector('iframe[data-forge-print]')).toBeNull()
  })

  it('stores only recognized user checks per paper profile', () => {
    const storage = memoryStorage()
    const config = { preset: 'A4', orientation: 'PORTRAIT' }
    const checks = Object.fromEntries(CALIBRATION_CHECKS.map(item => [item.key, true]))
    checks.untrusted = true
    const saved = writeCalibrationAcceptance(config, checks, storage, new Date('2026-09-19T08:00:00.000Z'))
    expect(saved.confirmedByUser).toBe(true)
    expect(saved.updatedAt).toBe('2026-09-19T08:00:00.000Z')
    expect(saved.checks).not.toHaveProperty('untrusted')
    expect(readCalibrationAcceptance(config, storage)).toEqual(saved)
    expect(readCalibrationAcceptance({ ...config, orientation: 'LANDSCAPE' }, storage).confirmedByUser).toBe(false)
    expect(calibrationProfileKey(config)).toBe('A4:PORTRAIT:210x297')
  })

  it('fails closed when a local acceptance record is corrupt or incomplete', () => {
    const corrupt = memoryStorage({ [CALIBRATION_STORAGE_KEY]: '{bad json' })
    const record = readCalibrationAcceptance({ preset: 'A3', orientation: 'PORTRAIT' }, corrupt)
    expect(record.confirmedByUser).toBe(false)
    expect(Object.values(record.checks).every(value => value === false)).toBe(true)

    const storage = memoryStorage()
    const partial = writeCalibrationAcceptance({ preset: 'A3', orientation: 'PORTRAIT' }, { browserScale: true }, storage)
    expect(partial.confirmedByUser).toBe(false)
  })
})
