import { describe, expect, it } from 'vitest'
import {
  buildApplicationPrintLocation,
  resolveApplicationSettingsSection,
} from '../application-print-entry'

describe('application print entry', () => {
  it('requires a page id so print settings stay on that page', () => {
    expect(buildApplicationPrintLocation({ applicationCode: 'cgou_app_1ko3psh' })).toBeNull()
    expect(buildApplicationPrintLocation({ applicationCode: 'cgou_app_1ko3psh' }, 'page_purchase')).toEqual({
      name: 'BusinessApplicationRuntime',
      params: { applicationCode: 'cgou_app_1ko3psh' },
      query: {
        edit: '1',
        pageId: 'page_purchase',
        designTab: 'settings',
        settingsSection: 'printing',
      },
    })
    expect(buildApplicationPrintLocation({})).toBeNull()
  })

  it('accepts known settings sections and rejects stale print query values', () => {
    expect(resolveApplicationSettingsSection('advanced')).toBe('advanced')
    expect(resolveApplicationSettingsSection(['advanced'])).toBe('advanced')
    expect(resolveApplicationSettingsSection('printing')).toBe('basic')
  })
})
