import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import AiForm from '../AiForm.vue'

// 组件测试不启动完整应用路由或自动生成路由，避免测试依赖开发服务器插件。
vi.mock('@/router', () => ({ default: { currentRoute: { value: { query: {} } } } }))

vi.mock('vue-router', () => ({
  useRoute: () => ({
    query: {},
    params: {},
    path: '/',
    fullPath: '/',
    name: 'ai-form-layout-preview-test',
  }),
}))

const naiveStubs = Object.fromEntries([
  'NInput',
  'NInputNumber',
  'NSelect',
  'NRadio',
  'NSpace',
  'NRadioGroup',
  'NRadioButton',
  'NCheckbox',
  'NCheckboxGroup',
  'NSwitch',
  'NDatePicker',
  'NTimePicker',
  'NButton',
  'NUpload',
  'NSlider',
  'NRate',
  'NColorPicker',
  'NCascader',
  'NTreeSelect',
  'NTransfer',
  'NInputGroup',
  'NIcon',
  'NTooltip',
  'NCard',
  'NTabs',
  'NTabPane',
  'NCollapse',
  'NCollapseItem',
  'NFormItem',
  'NDivider',
].map(name => [name, true]))

// NForm/NGrid/NGi 必须渲染插槽，否则嵌套布局结构断言不到
naiveStubs.NForm = { name: 'NForm', template: '<form class="n-form-stub"><slot /></form>' }
naiveStubs.NGrid = { name: 'NGrid', props: ['cols', 'xGap', 'yGap'], template: '<div class="n-grid-stub" :data-cols="String(cols)"><slot /></div>' }
naiveStubs.NGi = { name: 'NGi', props: ['span'], template: '<div class="n-gi-stub" :data-span="String(span)"><slot /></div>' }

/**
 * 复现设计器「栅格布局」预览链路：
 * designerLayoutFactory.createForgeLayoutComponent('row') 生成 row + 4 个 col，
 * 经 ForgeFormDesigner.normalizeRuntimeComponent 转为运行态节点形态（nodeType/type/componentKey）。
 */
function buildRowSchema(withField = false) {
  return [{
    id: 'cmp_row_1',
    componentKey: 'row',
    type: 'row',
    component: 'row',
    nodeType: 'row',
    label: '4 列栅格',
    props: { gutter: 16, columns: 24 },
    layout: { span: 2, align: 'left' },
    children: [1, 2, 3, 4].map(index => ({
      id: `cmp_row_1_col_${index}`,
      componentKey: 'col',
      type: 'col',
      component: 'col',
      nodeType: 'col',
      label: `第 ${index} 列`,
      props: { span: 6 },
      layout: { span: 6, align: 'left' },
      children: withField && index === 1
        ? [{
            id: 'cmp_field_1',
            componentKey: 'input',
            type: 'input',
            component: 'input',
            nodeType: 'field',
            field: 'fieldInput',
            prop: 'fieldInput',
            label: '名称',
            required: false,
            rules: [],
            props: { placeholder: '请填写名称' },
          }]
        : [],
    })),
  }]
}

function mountForm(schema, extraProps = {}) {
  return mount(AiForm, {
    props: { schema, value: {}, showActions: false, ...extraProps },
    global: { stubs: naiveStubs },
  })
}

describe('ai-form 栅格布局（设计器 row/col）渲染', () => {
  it('设计器预览（keepEmptyLayoutNodes）：空栅格保留结构并渲染可见占位', () => {
    const wrapper = mountForm(buildRowSchema(false), { keepEmptyLayoutNodes: true })
    // 结构：1 个 row gi + 4 个 col gi + 4 个空列占位 gi = 9
    expect(wrapper.findAll('.n-gi-stub').length).toBe(9)
    // 每个空列渲染一个可见占位块，预览不再一片空白
    expect(wrapper.findAll('.af-empty-layout-placeholder').length).toBe(4)
    expect(wrapper.text()).toContain('空容器')
  })

  it('运行态保留布局列结构，但不显示设计器专用空容器提示', () => {
    const wrapper = mountForm(buildRowSchema(false))
    // 布局容器属于独立协议节点，不能改变已发布画布的对齐；设计器占位提示仍禁止展示。
    expect(wrapper.findAll('.n-gi-stub').length).toBe(5)
    expect(wrapper.findAll('.af-empty-layout-placeholder')).toHaveLength(0)
    expect(wrapper.text()).not.toContain('空容器')
  })

  it('有字段的栅格：字段所在列应正常渲染', () => {
    const wrapper = mountForm(buildRowSchema(true))
    const html = wrapper.html()
    expect(html).toContain('名称')
  })
})
