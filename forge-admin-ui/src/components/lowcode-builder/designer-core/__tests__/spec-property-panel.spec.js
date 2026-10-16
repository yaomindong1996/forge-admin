/**
 * @fileoverview SpecPropertyPanel 组件测试 — C3 两列紧凑网格布局红线
 * @description 验证 schema 驱动属性面板的布局规则：
 *   boolean 标题上置 / json·textarea 占满整行 / section 分组标题占满整行 /
 *   excludeKeys 排除 / update:prop 事件签名。
 */

import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import SpecPropertyPanel from '../panel/SpecPropertyPanel.vue'
import '../index'

/** Naive UI 组件 stub：switch 需要可交互以验证 update:prop 事件链路，其余渲染占位即可 */
const STUBS = {
  NSwitch: {
    name: 'NSwitch',
    props: ['value', 'size', 'disabled'],
    emits: ['update:value'],
    template: '<div class="n-switch-stub" :data-value="String(!!value)" @click="$emit(\'update:value\', !value)" />',
  },
  NInput: true,
  NInputNumber: true,
  NSelect: true,
  NColorPicker: true,
  NTooltip: true,
  NIcon: true,
}

function mountPanel(props = {}) {
  return mount(SpecPropertyPanel, {
    props,
    global: { stubs: STUBS },
  })
}

describe('specPropertyPanel (C3 两列紧凑网格)', () => {
  it('boolean 属性和其它控件保持统一的上置标题', () => {
    const wrapper = mountPanel({ blockType: 'collapse', modelProps: {} })
    const switches = wrapper.findAll('.spec-property-switch')
    expect(switches).toHaveLength(1)
    const field = switches[0].element.closest('.spec-property-field')
    expect(field.textContent).toContain('手风琴模式')
    expect(field.querySelector('.spec-property-field-label')).not.toBeNull()
  })

  it('json 属性占满整行（full row），不与其它属性并排', () => {
    const wrapper = mountPanel({ blockType: 'collapse', modelProps: {} })
    const fullRows = wrapper.findAll('.spec-property-field--full')
    expect(fullRows).toHaveLength(1)
    expect(fullRows[0].text()).toContain('默认展开面板')
  })

  it('group 分组渲染为分组标题并占满整行', () => {
    const wrapper = mountPanel({ blockType: 'AiCrudPage', modelProps: {} })
    const sections = wrapper.findAll('.spec-property-section')
    expect(sections.length).toBeGreaterThan(0)
    expect(sections[0].text()).toContain('查询与列表')
  })

  it('栅格组件按 group 分节渲染（栅格/外观），两个设计器共用同一面板', () => {
    const wrapper = mountPanel({ blockType: 'grid-layout', modelProps: {} })
    const titles = wrapper.findAll('.spec-property-section').map(s => s.text())
    expect(titles.some(t => t.includes('栅格'))).toBe(true)
    expect(titles.some(t => t.includes('外观'))).toBe(true)
    // 表单侧别名 row 解析到同一份 spec，属性结构一致
    const formWrapper = mountPanel({ blockType: 'row', modelProps: {}, excludeKeys: ['cellMinHeight', 'alignItems', 'justifyItems', 'showCellBorder', 'cellBackground'] })
    expect(formWrapper.findAll('.spec-property-section').map(s => s.text()).some(t => t.includes('栅格'))).toBe(true)
  })

  it('excludeKeys 跳过接入方手写已覆盖的属性', () => {
    const wrapper = mountPanel({ blockType: 'collapse', modelProps: {}, excludeKeys: ['accordion'] })
    expect(wrapper.findAll('.spec-property-switch')).toHaveLength(0)
    // 未排除的 json 属性仍在
    expect(wrapper.findAll('.spec-property-field--full')).toHaveLength(1)
  })

  it('AiCrudPage 排除表单弹窗类 key 后 Spec 抽屉不再暴露无效入口', () => {
    const excludeKeys = [
      'formOpenMode', 'editLabelPlacement', 'editLabelWidth', 'editGridCols',
      'editSize', 'modalWidth', 'drawerPlacement', 'maxHeight',
    ]
    const wrapper = mountPanel({ blockType: 'AiCrudPage', modelProps: {}, excludeKeys })
    const labels = wrapper.findAll('.spec-property-field-label').map(n => n.text())
    expect(labels.some(t => t.includes('弹窗方式'))).toBe(false)
    expect(labels.some(t => t.includes('弹窗宽度'))).toBe(false)
    expect(labels.some(t => t.includes('表单列数'))).toBe(false)
    // 列表侧仍可配的工具栏/表格开关保留
    expect(labels.some(t => t.includes('分页') || t.includes('斑马纹') || t.includes('隐藏新增'))).toBe(true)
  })

  it('update:prop 事件签名为 { key, value }', async () => {
    const wrapper = mountPanel({ blockType: 'collapse', modelProps: {} })
    await wrapper.find('.n-switch-stub').trigger('click')
    const emitted = wrapper.emitted('update:prop')
    expect(emitted).toBeTruthy()
    expect(emitted[0][0]).toEqual({ key: 'accordion', value: true })
  })

  it('无属性可编辑时渲染空态', () => {
    const wrapper = mountPanel({ blockType: 'divider', modelProps: {}, excludeKeys: ['direction', 'dashed', 'titlePlacement', 'vertical'] })
    expect(wrapper.find('.spec-property-empty').exists()).toBe(true)
    expect(wrapper.find('.spec-property-grid').exists()).toBe(false)
  })

  it('属性值缺失时回退 schema default', () => {
    const wrapper = mountPanel({ blockType: 'divider', modelProps: {} })
    // divider.dashed 默认 false
    expect(wrapper.find('.n-switch-stub').attributes('data-value')).toBe('false')
    // 已设置值时不回退 default
    const wrapper2 = mountPanel({ blockType: 'divider', modelProps: { dashed: true } })
    expect(wrapper2.find('.n-switch-stub').attributes('data-value')).toBe('true')
  })
})
