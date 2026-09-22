import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { h, nextTick } from 'vue'
import { previewFormula } from '@/api/formula'
import { request } from '@/utils'
import AiCrudPage from '../../AiCrudPage.vue'
import { aiCrudPageProps } from '../../AiCrudPageProps'

vi.mock('@/utils', () => ({ request: vi.fn(), downloadFile: vi.fn() }))
vi.mock('@/utils/encrypt-request', () => ({ postEncrypt: vi.fn() }))
vi.mock('@/api/formula', () => ({ previewFormula: vi.fn() }))
vi.mock('@/api/ai', () => ({ crudConfigRender: vi.fn(), customQueryExecute: vi.fn() }))
vi.mock('@/api/business-app', () => ({ businessDocumentRuntimeBatch: vi.fn(), businessFlowStartConfig: vi.fn(), executeBusinessAction: vi.fn() }))
vi.mock('@/api/business-process', () => ({ businessProcessStartConfig: vi.fn(), startBusinessProcess: vi.fn() }))
vi.mock('@/store', () => ({ useUserStore: () => ({ permissions: [] }) }))
vi.mock('vue-router', () => ({ useRoute: () => ({ query: {}, params: {} }), useRouter: () => ({ push: vi.fn() }) }))
vi.mock('@/components/common/AuthImage.vue', () => ({ default: { render: () => null } }))
vi.mock('@/components/common/SystemTableCell.vue', () => ({ default: { render: () => null } }))
vi.mock('@/components/common/UserSelectPicker.vue', () => ({ default: { render: () => null } }))
vi.mock('@/components/DictTag.vue', () => ({ default: { render: () => null } }))
vi.mock('@/components/page-templates/ChildTableEditor.vue', () => ({ default: { name: 'ChildTableEditor', props: ['value', 'childrenConfig', 'readonly'], render: () => null } }))
vi.mock('../../AiCrudFlowDetail.vue', () => ({ default: { render: () => null } }))
vi.mock('../../AiCrudImportModal.vue', () => ({ default: { name: 'AiCrudImportModal', props: ['show', 'importer', 'templateDownloader', 'hasTemplate'], render: () => null } }))
vi.mock('../../AiCrudRowExpand.vue', () => ({ default: { render: () => null } }))
vi.mock('../../AiCustomQuery.vue', () => ({ default: { render: () => null } }))
vi.mock('../../AiForm.vue', () => ({ default: { name: 'AiForm', props: ['value', 'schema', 'context'], methods: { restoreValidation() {} }, template: '<div class="form-stub"><slot name="name" :value="value.name" /></div>' } }))
vi.mock('../../AiSearch.vue', () => ({ default: { name: 'AiSearch', props: ['modelValue'], template: '<div><slot name="name" :value="modelValue.name" /><slot name="extra-actions" :form-data="modelValue" /></div>' } }))
vi.mock('../../AiTable.vue', () => ({ default: { name: 'AiTable', props: ['dataSource', 'columns'], methods: { setCheckedKeys() {}, clearSelection() {}, getCheckedRows: () => [] }, template: '<div><slot name="name" :row="dataSource[0]" /><slot name="card" :row="dataSource[0]" /><slot name="toolbar" /><slot name="toolbar-extra" /></div>' } }))

const exposedMethods = [
  'search',
  'refresh',
  'loadList',
  'getSelectedRows',
  'getSelectedKeys',
  'clearSelection',
  'setSelectedKeys',
  'getTableData',
  'setTableData',
  'showAdd',
  'showEdit',
  'showDetail',
  'handleEdit',
  'handleDetail',
  'handleDelete',
  'handleBatchDelete',
  'submitForm',
  'triggerAction',
  'closeModal',
  'getSearchParams',
  'setSearchParams',
  'resetSearch',
]
const emittedEvents = [
  'load-list-success',
  'load-list-error',
  'add',
  'edit',
  'detail',
  'delete',
  'submit-success',
  'submit-error',
  'selection-change',
  'modal-open',
  'modal-close',
  'render-mode-change',
  'custom-action',
]
const wrappers = []
const passThrough = { template: '<div><slot /><slot name="footer" /></div>' }
const overlay = { props: ['show'], template: '<div v-if="show"><slot /><slot name="footer" /></div>' }

function mountPage(props = {}, slots = {}) {
  const wrapper = mount(AiCrudPage, {
    props: { lazy: true, columns: [], ...props },
    slots,
    global: {
      stubs: {
        NModal: overlay,
        NDrawer: overlay,
        NDrawerContent: passThrough,
        NButton: passThrough,
        NIcon: passThrough,
        NTag: passThrough,
        NDropdown: passThrough,
        NSpin: passThrough,
        NAlert: passThrough,
        NResult: passThrough,
        NInputNumber: true,
        NDataTable: true,
        NTabs: passThrough,
        NTabPane: passThrough,
        NProgress: true,
        NSpace: passThrough,
        NSelect: true,
        NForm: passThrough,
        NFormItem: passThrough,
      },
    },
  })
  wrappers.push(wrapper)
  return wrapper
}

beforeEach(() => {
  vi.clearAllMocks()
  window.$message = { success: vi.fn(), warning: vi.fn(), error: vi.fn() }
  request.mockResolvedValue({ data: { records: [], total: 0 } })
  previewFormula.mockResolvedValue({ data: { success: true, result: 12 } })
  vi.stubGlobal('requestAnimationFrame', vi.fn(() => 1))
  vi.stubGlobal('cancelAnimationFrame', vi.fn())
})
afterEach(() => {
  wrappers.splice(0).forEach(wrapper => wrapper.unmount())
  vi.useRealTimers()
  vi.unstubAllGlobals()
})

describe('aiCrudPage 对外兼容基线', () => {
  it('保持 props、事件及全部公开方法', () => {
    const wrapper = mountPage()
    expect(AiCrudPage.props).toBe(aiCrudPageProps)
    expect(AiCrudPage.emits).toEqual(emittedEvents)
    expect(Object.keys(wrapper.vm.$.exposed).sort()).toEqual([...exposedMethods].sort())
    exposedMethods.forEach(name => expect(wrapper.vm[name]).toBeTypeOf('function'))
    wrapper.vm.setTableData([{ id: '1' }])
    expect(wrapper.vm.getTableData()).toEqual([{ id: '1' }])
    wrapper.vm.setSearchParams({ name: '搜索' })
    expect(wrapper.vm.getSearchParams()).toEqual({ name: '搜索' })
    wrapper.vm.setSelectedKeys(['1'])
    expect(wrapper.vm.getSelectedKeys()).toEqual(['1'])
    wrapper.vm.clearSelection()
    expect(wrapper.vm.getSelectedKeys()).toEqual([])
    expect(() => wrapper.vm.triggerAction('missing')).toThrow('页面动作不存在或未发布')
  })

  it.each(['modal', 'drawer', 'flat', 'tabWorkspace'])('%s 保持新增、编辑、详情及关闭行为', async (mode) => {
    const wrapper = mountPage({ formOpenMode: mode, editSchema: [{ field: 'name', type: 'input' }] })
    const defaults = { name: '默认值' }
    const context = { title: '新建记录' }
    await wrapper.vm.showAdd(defaults, context)
    expect(wrapper.emitted('add')[0]).toEqual([{ defaults, context }])
    expect(wrapper.emitted('modal-open')[0]).toEqual([{ status: 'add', row: null, defaults, context }])
    expect(wrapper.findComponent({ name: 'AiForm' }).props('value')).toMatchObject(defaults)
    if (mode === 'flat') {
      const back = wrapper.findAll('button').find(item => item.text().includes('返回列表'))
      expect(back).toBeTruthy()
      expect(back.classes()).toContain('inline-form-back-btn')
      // context.title 与模式文案不同时才展示模式标签，避免「新增 / 新增」重复
      expect(wrapper.find('.inline-form-panel-title strong').text()).toBe('新建记录')
      expect(wrapper.find('.inline-form-mode-tag').text()).toBe('新增')
    }
    const row = { id: '1', name: '编辑值' }
    await wrapper.vm.showEdit(row)
    expect(wrapper.emitted('edit')[0]).toEqual([row])
    expect(wrapper.emitted('modal-open')[1]).toEqual([{ status: 'edit', row }])
    await wrapper.vm.showDetail(row)
    expect(wrapper.emitted('detail')[0]).toEqual([row])
    wrapper.vm.closeModal()
    await nextTick()
    if (mode === 'modal' || mode === 'drawer')
      expect(wrapper.findComponent({ name: 'AiForm' }).exists()).toBe(false)
  })

  it('formOnly 初始化及主子表传值保持不变', async () => {
    const wrapper = mountPage({
      formOnly: true,
      editSchema: [{ field: 'name', type: 'input', defaultValue: '主表' }],
      childrenConfig: [{ key: 'items', title: '明细', fields: [{ field: 'name', type: 'input' }] }],
    })
    await flushPromises()
    expect(wrapper.classes()).toContain('is-form-only')
    expect(wrapper.findComponent({ name: 'AiForm' }).props('value').name).toBe('主表')
    expect(wrapper.findComponent({ name: 'ChildTableEditor' }).exists()).toBe(true)
    expect(request).not.toHaveBeenCalled()
  })

  it('保留搜索、表格、卡片、表单动态插槽及载荷', async () => {
    const slot = label => data => h('span', { class: label }, JSON.stringify(data))
    const wrapper = mountPage({
      searchSchema: [{ field: 'name', type: 'slot' }],
      columns: [{ field: 'name', slot: 'name' }],
      editSchema: [{ field: 'name', type: 'slot' }],
    }, {
      'search-name': slot('search-slot'),
      'search-extra-actions': slot('search-extra'),
      'table-name': slot('table-slot'),
      'table-card': slot('card-slot'),
      'form-name': slot('form-slot'),
    })
    wrapper.vm.setSearchParams({ name: '查询值' })
    wrapper.vm.setTableData([{ id: 1, name: '列表值' }])
    await nextTick()
    expect(wrapper.find('.search-slot').text()).toContain('查询值')
    expect(wrapper.find('.search-extra').text()).toContain('查询值')
    expect(wrapper.find('.table-slot').text()).toContain('列表值')
    expect(wrapper.find('.card-slot').text()).toContain('列表值')
    await wrapper.vm.showAdd({ name: '表单值' })
    expect(wrapper.find('.form-slot').text()).toContain('表单值')
  })

  it('打开表单后仍回填原表单并保留公式触发', async () => {
    vi.useFakeTimers()
    const wrapper = mountPage({ editSchema: [
      { field: 'qty', type: 'number', defaultValue: 3 },
      { field: 'total', formulaConfig: { expression: 'qty * 4' } },
    ] })
    await wrapper.vm.showAdd()
    await vi.advanceTimersByTimeAsync(1)
    expect(previewFormula).toHaveBeenCalledWith(expect.objectContaining({ expression: 'qty * 4', sampleValues: { qty: 3, total: null } }))
    expect(wrapper.findComponent({ name: 'AiForm' }).props('value').total).toBe(12)
  })
})
