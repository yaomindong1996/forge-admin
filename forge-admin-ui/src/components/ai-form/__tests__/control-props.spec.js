import { describe, expect, it } from 'vitest'
import { resolveControlProps } from '../control-props'

describe('resolveControlProps', () => {
  it('keeps placeholder while dropping value bindings that would override controlled inputs', () => {
    expect(resolveControlProps({
      'placeholder': '请输入输入框',
      'value': '',
      'defaultValue': '',
      'modelValue': '',
      'onUpdate:value': () => {},
      'maxlength': 64,
    })).toEqual({
      placeholder: '请输入输入框',
      maxlength: 64,
    })
  })

  it('drops kebab-case update listeners from schema props as well', () => {
    expect(resolveControlProps({
      'on-update:value': () => {},
      'on-update:model-value': () => {},
      'on-update:label-value': () => {},
      'clearable': true,
    })).toEqual({ clearable: true })
  })

  it('strips readonly and disabled so AiFormItem can own those states', () => {
    expect(resolveControlProps({
      placeholder: 'name',
      readonly: true,
      disabled: true,
      maxlength: 32,
    })).toEqual({
      placeholder: 'name',
      maxlength: 32,
    })
  })

  it('strips optionSource / fieldMappings so they are not bound onto Naive controls', () => {
    expect(resolveControlProps({
      placeholder: '请选择',
      optionSource: { type: 'QUERY_SOURCE', sourceKey: 'metric' },
      fieldMappings: [{ sourceField: 'name', targetField: 'title' }],
      mappings: [{ source: 'a', target: 'b' }],
      clearable: true,
    })).toEqual({
      placeholder: '请选择',
      clearable: true,
    })
  })

  it('strips options so AiFormItem currentOptions is not overridden by empty designer leftovers', () => {
    expect(resolveControlProps({
      placeholder: '请选择',
      options: [],
      labelValueField: 'name',
      clearable: true,
    })).toEqual({
      placeholder: '请选择',
      clearable: true,
    })
  })

  it('strips designer meta (__events / __designerStyle / e2eMarker / __*) from control props', () => {
    expect(resolveControlProps({
      placeholder: '请选择',
      clearable: true,
      __events: [{ action: 'message' }],
      __designerStyle: { borderWidth: '3px' },
      e2eMarker: 'E2E_select',
      __customMeta: { x: 1 },
    })).toEqual({
      placeholder: '请选择',
      clearable: true,
    })
  })

  it('strips dataSourceType / labelName / valueName leftover option-source meta', () => {
    expect(resolveControlProps({
      placeholder: '请选择',
      clearable: true,
      dataSourceType: 'static',
      labelName: 'label',
      valueName: 'value',
    })).toEqual({
      placeholder: '请选择',
      clearable: true,
    })
  })

  it('strips prefix / suffix / currencySymbol so they render via slots instead of props', () => {
    expect(resolveControlProps({
      placeholder: '金额',
      prefix: '￥',
      suffix: '%',
      currencySymbol: '¥',
      clearable: true,
    })).toEqual({
      placeholder: '金额',
      clearable: true,
    })
  })

  it('strips input/select/date-picker slot design props so they cannot leak into Naive controls', () => {
    expect(resolveControlProps({
      placeholder: '请输入',
      prefixIcon: 'search',
      clearIcon: 'close',
      inputType: 'password',
      passwordVisibleIcon: 'eye',
      dropdownHeader: '头部',
      arrowIcon: 'chevron-down',
      dateIcon: 'calendar',
      dateFooter: '页脚',
      prevMonthIcon: 'chevron-back',
      nextYearIcon: 'chevron-forward',
      clearable: true,
      pair: true,
      separator: '-',
    })).toEqual({
      placeholder: '请输入',
      clearable: true,
      pair: true,
    })
  })
})
