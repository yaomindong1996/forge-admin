import {
  FORGE_DEFAULT_PRESET_PREFIX,
  mapColumnDefaultToFieldDefault,
  resolveDefaultValueEditorKind,
  resolveDesignerCanvasPreviewValue,
  resolveRuntimeDefaultValue,
  resolveSwitchValuePair,
} from '../field-default-value'

describe('field-default-value', () => {
  it('resolves editor kind by component type', () => {
    expect(resolveDefaultValueEditorKind('switch')).toBe('switch')
    expect(resolveDefaultValueEditorKind('number')).toBe('number')
    expect(resolveDefaultValueEditorKind('datetime')).toBe('date')
    expect(resolveDefaultValueEditorKind('time')).toBe('time')
    expect(resolveDefaultValueEditorKind('select')).toBe('option')
    expect(resolveDefaultValueEditorKind('fileUpload')).toBe('none')
    expect(resolveDefaultValueEditorKind('input')).toBe('text')
  })

  it('prefers numeric 0/1 switch pair and coerces boolean props', () => {
    expect(resolveSwitchValuePair({ props: {} }, { dataType: 'tinyint', fieldType: 'SWITCH' }))
      .toEqual({ checkedValue: 1, uncheckedValue: 0 })
    expect(resolveSwitchValuePair({ props: { checkedValue: true, uncheckedValue: false } }))
      .toEqual({ checkedValue: 1, uncheckedValue: 0 })
    expect(resolveSwitchValuePair({ componentKey: 'switch', props: {} }))
      .toEqual({ checkedValue: 1, uncheckedValue: 0 })
  })

  it('resolves date presets at runtime', () => {
    const now = new Date('2026-09-22T15:30:45')
    expect(resolveRuntimeDefaultValue(`${FORGE_DEFAULT_PRESET_PREFIX}today`, 'date', { now }))
      .toBe('2026-09-22')
    expect(resolveRuntimeDefaultValue(`${FORGE_DEFAULT_PRESET_PREFIX}yesterday`, 'date', { now }))
      .toBe('2026-09-21')
    expect(resolveRuntimeDefaultValue(`${FORGE_DEFAULT_PRESET_PREFIX}tomorrow`, 'date', { now }))
      .toBe('2026-09-23')
    expect(resolveRuntimeDefaultValue(`${FORGE_DEFAULT_PRESET_PREFIX}now`, 'datetime', { now }))
      .toBe('2026-09-22 15:30:45')
    expect(resolveRuntimeDefaultValue('fixed-text', 'input')).toBe('fixed-text')
  })

  it('maps database column defaults into field defaults', () => {
    expect(mapColumnDefaultToFieldDefault('CURRENT_TIMESTAMP', { componentType: 'datetime', dataType: 'datetime' }))
      .toBe(`${FORGE_DEFAULT_PRESET_PREFIX}now`)
    expect(mapColumnDefaultToFieldDefault('CURRENT_DATE', { componentType: 'date', dataType: 'date' }))
      .toBe(`${FORGE_DEFAULT_PRESET_PREFIX}today`)
    expect(mapColumnDefaultToFieldDefault('1', { componentType: 'switch', dataType: 'tinyint', fieldType: 'SWITCH' }))
      .toBe(1)
    expect(mapColumnDefaultToFieldDefault('0', { componentType: 'switch', dataType: 'tinyint' }))
      .toBe(0)
    expect(mapColumnDefaultToFieldDefault("'ACTIVE'", { componentType: 'input', dataType: 'varchar' }))
      .toBe('ACTIVE')
    expect(mapColumnDefaultToFieldDefault("'DRAFT'::character varying", { componentType: 'input' }))
      .toBe('DRAFT')
    expect(mapColumnDefaultToFieldDefault('100', { componentType: 'number', dataType: 'int' }))
      .toBe(100)
    expect(mapColumnDefaultToFieldDefault(null, { componentType: 'input' })).toBeNull()
  })

  it('resolves designer canvas preview value from configured defaults', () => {
    expect(resolveDesignerCanvasPreviewValue({
      componentKey: 'switch',
      props: { defaultValue: 1, checkedValue: 1, uncheckedValue: 0 },
    })).toBe(1)
    expect(resolveDesignerCanvasPreviewValue({
      componentKey: 'switch',
      props: {},
    })).toBe(0)
    expect(resolveDesignerCanvasPreviewValue({
      componentKey: 'input',
      props: { defaultValue: 'hello' },
    })).toBe('hello')
    expect(resolveDesignerCanvasPreviewValue({
      componentKey: 'date',
      props: { defaultValue: `${FORGE_DEFAULT_PRESET_PREFIX}today` },
    })).toMatch(/^\d{4}-\d{2}-\d{2}$/)
  })
})
