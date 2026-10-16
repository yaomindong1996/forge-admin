import { describe, expect, it } from 'vitest'
import { applyRuntimeControl, resolveRuntimeControl } from '../runtime-rules'

describe('lowcode runtime visibility rules', () => {
  const field = {
    field: 'pickupMethod',
    hidden: true,
    required: true,
    runtimeRules: [{
      conditions: [{ field: 'deliveryMode', operator: 'eq', value: 'PICKUP' }],
      effect: { visible: true },
    }],
  }

  it('shows a statically hidden field when its visibility condition matches', () => {
    const control = resolveRuntimeControl(field, { formData: { deliveryMode: 'PICKUP' } })

    expect(control.visible).toBe(true)
    expect(applyRuntimeControl(field, { formData: { deliveryMode: 'PICKUP' } })).not.toBeNull()
  })

  it('hides the field and clears required validation when the condition does not match', () => {
    const control = resolveRuntimeControl(field, { formData: { deliveryMode: 'DELIVERY' } })

    expect(control.visible).toBe(false)
    expect(applyRuntimeControl(field, { formData: { deliveryMode: 'DELIVERY' } })).toBeNull()
  })

  it('keeps legacy hidden effects compatible', () => {
    const control = resolveRuntimeControl({
      field: 'pickupMethod',
      runtimeRules: [{
        conditions: [{ field: 'deliveryMode', operator: 'eq', value: 'DELIVERY' }],
        effect: { hidden: true },
      }],
    }, { formData: { deliveryMode: 'DELIVERY' } })

    expect(control.visible).toBe(false)
  })

  it('keeps a visible-by-condition rule hidden until its condition matches', () => {
    const target = {
      field: 'pickupAddress',
      runtimeRules: [{
        conditions: [{ field: 'deliveryMode', operator: 'eq', value: 'PICKUP' }],
        effect: { visible: true, whenUnmatched: 'hidden' },
      }],
    }

    expect(resolveRuntimeControl(target, { formData: { deliveryMode: 'DELIVERY' } }).visible).toBe(false)
    expect(resolveRuntimeControl(target, { formData: { deliveryMode: 'PICKUP' } }).visible).toBe(true)
  })

  it('does not treat function disabled/readonly as statically disabled', () => {
    const target = {
      field: 'dictType',
      disabled: ({ formData }) => Boolean(formData?.dictId),
      readonly: ({ formData }) => Boolean(formData?.dictId),
    }

    const control = resolveRuntimeControl(target, { formData: {} })
    expect(control.disabled).toBe(false)
    expect(control.readonly).toBe(false)

    const applied = applyRuntimeControl(target, { formData: {} })
    expect(applied.disabled).toBe(target.disabled)
    expect(applied.props?.disabled).toBeUndefined()
  })

  it('still honors static boolean disabled/readonly', () => {
    expect(resolveRuntimeControl({ disabled: true }, {}).disabled).toBe(true)
    expect(resolveRuntimeControl({ readonly: true }, {}).disabled).toBe(true)
    expect(resolveRuntimeControl({ props: { disabled: true } }, {}).disabled).toBe(true)
  })

  it('matches switch-like boolean values for showHide conditions', () => {
    const target = {
      field: 'fieldRate',
      runtimeRules: [{
        conditions: [{ source: 'formData', field: 'fieldSwitch', operator: 'eq', value: true }],
        effect: { visible: true, whenUnmatched: 'hidden' },
      }],
    }

    expect(resolveRuntimeControl(target, { formData: { fieldSwitch: false } }).visible).toBe(false)
    expect(resolveRuntimeControl(target, { formData: { fieldSwitch: 0 } }).visible).toBe(false)
    expect(resolveRuntimeControl(target, { formData: { fieldSwitch: true } }).visible).toBe(true)
    expect(resolveRuntimeControl(target, { formData: { fieldSwitch: 1 } }).visible).toBe(true)
    expect(resolveRuntimeControl(target, { formData: { fieldSwitch: 'true' } }).visible).toBe(true)
  })
})
