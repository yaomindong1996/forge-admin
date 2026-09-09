/**
 * @file 别名映射测试 — 验证运行时别名 + field-* 包装名正确映射
 */

import { describe, it, expect } from 'vitest'
import { getComponentSpec, resolveComponentType } from '../index'

describe('Aliases Mapping', () => {
  // ── 布局容器别名 ──
  it('row/fcRow/grid-layout should resolve to grid', () => {
    expect(resolveComponentType('row')).toBe('grid')
    expect(resolveComponentType('fcRow')).toBe('grid')
    expect(resolveComponentType('grid-layout')).toBe('grid')
  })

  it('tableGrid should resolve to tableCell, fcTable to table', () => {
    // tableGrid 是 tableCell 的别名（单元格包装层），不是 table 容器
    expect(resolveComponentType('tableGrid')).toBe('tableCell')
    expect(resolveComponentType('fcTable')).toBe('table')
  })

  it('elCard should resolve to card', () => {
    expect(resolveComponentType('elCard')).toBe('card')
  })

  it('elTabs should resolve to tabs', () => {
    expect(resolveComponentType('elTabs')).toBe('tabs')
  })

  it('elCollapse should resolve to collapse', () => {
    expect(resolveComponentType('elCollapse')).toBe('collapse')
  })

  it('box-layout should resolve to box', () => {
    expect(resolveComponentType('box-layout')).toBe('box')
  })

  // ── 包装节点别名 ──
  it('elTabPane should resolve to tabPane', () => {
    expect(resolveComponentType('elTabPane')).toBe('tabPane')
  })

  it('elCollapseItem should resolve to collapseItem', () => {
    expect(resolveComponentType('elCollapseItem')).toBe('collapseItem')
  })

  it('fcCol should resolve to col', () => {
    expect(resolveComponentType('fcCol')).toBe('col')
  })

  it('fcTableGrid should resolve to tableCell', () => {
    expect(resolveComponentType('fcTableGrid')).toBe('tableCell')
  })

  // ── 字段组件别名 ──
  it('inputNumber/integer should resolve to number', () => {
    expect(resolveComponentType('inputNumber')).toBe('number')
    expect(resolveComponentType('integer')).toBe('number')
  })

  it('radioGroup should resolve to radio', () => {
    expect(resolveComponentType('radioGroup')).toBe('radio')
  })

  it('checkboxGroup should resolve to checkbox', () => {
    expect(resolveComponentType('checkboxGroup')).toBe('checkbox')
  })

  it('datePicker should resolve to date', () => {
    expect(resolveComponentType('datePicker')).toBe('date')
  })

  // ── field-* 包装名 ──
  it('field-textarea should resolve to textarea', () => {
    expect(resolveComponentType('field-textarea')).toBe('textarea')
  })

  it('field-dict-select should resolve to dictSelect', () => {
    expect(resolveComponentType('field-dict-select')).toBe('dictSelect')
  })

  it('field-image-upload should resolve to imageUpload', () => {
    expect(resolveComponentType('field-image-upload')).toBe('imageUpload')
  })

  // ── 业务组件别名 ──
  it('crud/crudBlock/aiCrudPage should resolve to AiCrudPage', () => {
    expect(resolveComponentType('crud')).toBe('AiCrudPage')
    expect(resolveComponentType('crudBlock')).toBe('AiCrudPage')
    expect(resolveComponentType('aiCrudPage')).toBe('AiCrudPage')
  })

  // ── 布局辅助别名 ──
  it('section-divider should resolve to groupTitle', () => {
    expect(resolveComponentType('section-divider')).toBe('groupTitle')
  })

  it('AiFormSectionTitle/divider/elDivider should resolve to formSectionTitle', () => {
    expect(resolveComponentType('AiFormSectionTitle')).toBe('formSectionTitle')
    expect(resolveComponentType('FormSectionTitle')).toBe('formSectionTitle')
  })

  // ── getComponentSpec 通过别名获取 ──
  it('getComponentSpec should work with aliases', () => {
    const spec = getComponentSpec('elCard')
    expect(spec).not.toBeNull()
    expect(spec.type).toBe('card')
    expect(spec.container).toBe(true)
  })

  it('getComponentSpec should return undefined for unknown type', () => {
    expect(getComponentSpec('non-existent-component')).toBeUndefined()
  })
})
