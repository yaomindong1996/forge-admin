import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import AiForm from '../AiForm.vue'

vi.mock('vue-router', () => ({
  useRoute: () => ({
    query: {},
    params: {},
    path: '/',
    fullPath: '/',
    name: 'ai-form-section-nav-test',
  }),
}))
vi.mock('@/api/business-app', () => ({
  queryBusinessRecordSelector: vi.fn(),
}))

const naiveStubs = {
  NForm: { name: 'NForm', template: '<form class="n-form-stub"><slot /></form>' },
  NGrid: { name: 'NGrid', template: '<div class="n-grid-stub"><slot /></div>' },
  NGi: { name: 'NGi', template: '<div class="n-gi-stub"><slot /></div>' },
  NFormItem: { name: 'NFormItem', template: '<div><slot /></div>' },
  NInput: true,
  NSpace: true,
  NButton: true,
  NIcon: true,
  NEmpty: true,
  NModal: true,
}

function divider(label, id) {
  return {
    type: 'divider',
    nodeType: 'divider',
    componentKey: 'AiFormSectionTitle',
    label,
    __sectionId: id,
  }
}

function mountForm(schema, extra = {}) {
  return mount(AiForm, {
    props: { schema, value: {}, ...extra },
    global: { stubs: naiveStubs },
  })
}

describe('AiForm 分组导航', () => {
  it('分组达到 3 个时显示顶部条，不占用左侧栏', () => {
    const wrapper = mountForm([
      divider('基础信息', 's1'),
      { field: 'name', type: 'input', label: '名称' },
      divider('组织与授权', 's2'),
      { field: 'org', type: 'input', label: '组织' },
      divider('联系信息', 's3'),
      { field: 'phone', type: 'input', label: '手机' },
    ])
    const nav = wrapper.find('.ai-form-section-nav')
    expect(wrapper.find('.ai-form-body--with-nav').exists()).toBe(true)
    expect(nav.exists()).toBe(true)
    expect(nav.find('.ai-form-section-nav__dot').exists()).toBe(false)
    expect(nav.findAll('button').map(item => item.text())).toEqual(['基础信息', '组织与授权', '联系信息'])
  })

  it('hideSectionNav 关闭分组导航', () => {
    const wrapper = mountForm([
      divider('基础信息', 's1'),
      divider('组织与授权', 's2'),
      divider('联系信息', 's3'),
    ], { hideSectionNav: true })
    expect(wrapper.find('.ai-form-section-nav').exists()).toBe(false)
  })
})
