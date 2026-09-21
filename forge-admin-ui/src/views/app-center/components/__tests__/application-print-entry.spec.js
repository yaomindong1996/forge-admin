import { describe, expect, it } from 'vitest'
import {
  buildApplicationPrintLocation,
  resolveApplicationSettingsSection,
} from '../application-print-entry'

describe('application print entry', () => {
  it('targets the current runtime settings print section', () => {
    expect(buildApplicationPrintLocation({ applicationCode: 'cgou_app_1ko3psh' })).toEqual({
      name: 'BusinessApplicationRuntime',
      params: { applicationCode: 'cgou_app_1ko3psh' },
      query: { view: 'settings', settingsSection: 'printing' },
    })
    expect(buildApplicationPrintLocation({})).toBeNull()
  })

  it('accepts known settings sections and rejects stale query values', () => {
    expect(resolveApplicationSettingsSection('printing')).toBe('printing')
    expect(resolveApplicationSettingsSection(['advanced'])).toBe('advanced')
    expect(resolveApplicationSettingsSection('legacy-printing')).toBe('basic')
  })
})
