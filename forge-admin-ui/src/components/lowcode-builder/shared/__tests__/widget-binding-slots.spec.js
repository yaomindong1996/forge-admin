import { describe, expect, it } from 'vitest'
import {
  buildBindingPreviewRecord,
  hasResolvedBindingValues,
  isBindingSlotVisible,
  listAssignedBindingSlots,
  resolveBoundFallbackProps,
  resolveWidgetBindingProfile,
  resolveWidgetRenderMode,
} from '../widget-binding-slots'

describe('widget binding slots', () => {
  it('exposes statistic slots as title/value/description/trend', () => {
    const profile = resolveWidgetBindingProfile('statistic')
    expect(profile.kind).toBe('object')
    expect(profile.primarySlot).toBe('valueField')
    expect(profile.slots.map(item => item.label)).toEqual(['标题', '数值', '说明', '趋势'])
    expect(resolveBoundFallbackProps('statistic')).toEqual(['title', 'value', 'description', 'trend'])
  })

  it('exposes stats-strip as list row mappings', () => {
    const profile = resolveWidgetBindingProfile('stats-strip')
    expect(profile.kind).toBe('list')
    expect(profile.needsContextPath).toBe(true)
    expect(profile.slots.map(item => item.key)).toEqual(['labelField', 'valueField', 'metaField'])
  })

  it('exposes quick-entry / chart slots for list binding', () => {
    expect(resolveWidgetBindingProfile('quick-entry').slots.map(item => item.key))
      .toEqual(['titleField', 'descriptionField', 'valueField', 'metaField'])
    expect(resolveWidgetBindingProfile('bar-chart').slots.map(item => item.key))
      .toEqual(['labelField', 'valueField', 'metaField'])
    expect(resolveWidgetRenderMode({ enabled: true, sourceType: 'context' }, 'line-chart')).toBe('manual')
    expect(resolveWidgetRenderMode({ enabled: true, sourceType: 'context', renderMode: 'list' }, 'bar-chart')).toBe('list')
  })

  it('lists assigned slots and ignores classic defaults until real field is picked', () => {
    expect(listAssignedBindingSlots({
      titleField: 'title',
      valueField: 'value',
      metaField: 'trend',
    }, 'statistic', [{ field: 'orderCount' }])).toEqual([])

    expect(listAssignedBindingSlots({
      titleField: 'name',
      valueField: 'orderCount',
      metaField: 'growthRate',
    }, 'statistic', [
      { field: 'name' },
      { field: 'orderCount' },
      { field: 'growthRate' },
    ])).toEqual([
      { key: 'titleField', label: '标题', path: 'name', primary: false },
      { key: 'valueField', label: '数值', path: 'orderCount', primary: true },
      { key: 'metaField', label: '趋势', path: 'growthRate', primary: false },
    ])
  })

  it('builds preview sample when detail record is empty', () => {
    const binding = {
      enabled: true,
      sourceType: 'context',
      titleField: 'customerName',
      valueField: 'orderCount',
      metaField: 'growthRate',
    }
    const fields = [
      { field: 'customerName', label: '客户' },
      { field: 'orderCount', label: '订单数', dataType: 'int' },
      { field: 'growthRate', label: '增长率' },
    ]
    expect(hasResolvedBindingValues({}, binding, 'statistic', fields)).toBe(false)
    const preview = buildBindingPreviewRecord(binding, fields, 'statistic')
    expect(preview.customerName).toContain('客户')
    expect(Number(preview.orderCount)).toBeGreaterThan(0)
    expect(preview.growthRate).toMatch(/%/)
  })

  it('respects slotHidden visibility', () => {
    expect(isBindingSlotVisible({}, 'titleField')).toBe(true)
    expect(isBindingSlotVisible({ slotHidden: { titleField: true } }, 'titleField')).toBe(false)
    expect(isBindingSlotVisible({ slotHidden: { titleField: true } }, 'valueField')).toBe(true)
  })

  it('defaults stats-strip to manual render mode so metrics can be added', () => {
    expect(resolveWidgetRenderMode({ enabled: true, sourceType: 'context' }, 'stats-strip')).toBe('manual')
    expect(resolveWidgetRenderMode({ enabled: true, sourceType: 'context', renderMode: 'list' }, 'stats-strip')).toBe('list')
  })
})
