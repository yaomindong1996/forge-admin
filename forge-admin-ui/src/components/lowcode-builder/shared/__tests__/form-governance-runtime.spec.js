import { describe, expect, it } from 'vitest'
import {
  applySetFieldValueHandler,
  buildFormGovernanceHookProps,
} from '../form-governance-runtime'

describe('form-governance-runtime', () => {
  it('applies setFieldValue handler onto payload', () => {
    const payload = { name: 'a' }
    applySetFieldValueHandler('fieldInput=E2E_AFTERLOAD', payload)
    expect(payload.fieldInput).toBe('E2E_AFTERLOAD')
  })

  it('maps afterLoad events to beforeRenderForm and fills fields on null payload', async () => {
    const hooks = buildFormGovernanceHookProps([
      {
        id: 'evt1',
        hook: 'afterLoad',
        action: 'setFieldValue',
        handler: 'fieldInput=E2E_SETFIELD_AFTERLOAD',
      },
    ])
    expect(typeof hooks.beforeRenderForm).toBe('function')
    const result = await hooks.beforeRenderForm(null)
    expect(result).toEqual({ fieldInput: 'E2E_SETFIELD_AFTERLOAD' })
  })

  it('preserves existing payload keys when applying setFieldValue', async () => {
    const hooks = buildFormGovernanceHookProps([
      {
        hook: 'beforeLoad',
        action: 'setFieldValue',
        handler: 'remark=from-hook',
      },
    ])
    const result = await hooks.beforeRenderForm({ name: 'keep' })
    expect(result).toEqual({ name: 'keep', remark: 'from-hook' })
  })
})
