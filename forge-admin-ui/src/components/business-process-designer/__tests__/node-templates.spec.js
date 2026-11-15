import { describe, expect, it } from 'vitest'
import {
  createActionTemplateConfig,
  normalizeStatusFieldCode,
} from '../node-templates.js'

describe('createActionTemplateConfig', () => {
  it('UPDATE_STATUS defaults to flowStatus when no statusField provided', () => {
    const config = createActionTemplateConfig('UPDATE_STATUS', { objectCode: 'apply_kdcp' })
    expect(config).toMatchObject({
      actionType: 'UPDATE_RECORD',
      objectCode: 'apply_kdcp',
      fieldMappings: [{ field: 'flowStatus', valueSource: 'CONSTANT', value: '' }],
    })
  })

  it('UPDATE_STATUS uses suggested statusField from object fields', () => {
    const config = createActionTemplateConfig('UPDATE_STATUS', {
      objectCode: 'apply_kdcp',
      statusField: 'flowStatus',
    })
    expect(config.fieldMappings[0].field).toBe('flowStatus')
  })

  it('normalizeStatusFieldCode accepts flowStatus and status aliases', () => {
    expect(normalizeStatusFieldCode('flowStatus')).toBe('flowStatus')
    expect(normalizeStatusFieldCode('status')).toBe('status')
    expect(normalizeStatusFieldCode('biz_status')).toBe('biz_status')
    expect(normalizeStatusFieldCode('reason')).toBe('')
  })
})
