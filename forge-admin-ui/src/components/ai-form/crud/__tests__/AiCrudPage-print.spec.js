import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { h, nextTick } from 'vue'
import { printRecordFromQuery } from '@/components/print/management/printRouteContext'
import { request } from '@/utils'
import actions from '../../../../../../code-copilot/changes/forge-native-print/verification/m4c-actions.json'
import AiCrudPage from '../../AiCrudPage.vue'

const { router, user } = vi.hoisted(() => ({ router: { push: vi.fn(), resolve: vi.fn(path => ({ href: path })) }, user: { getDataPermission: ['print:execute'] } }))

vi.mock('@/utils', () => ({ request: vi.fn(), downloadFile: vi.fn() }))
vi.mock('@/utils/encrypt-request', () => ({ postEncrypt: vi.fn() }))
vi.mock('@/api/formula', () => ({ previewFormula: vi.fn() }))
vi.mock('@/api/ai', () => ({ crudConfigRender: vi.fn(), customQueryExecute: vi.fn() }))
vi.mock('@/api/business-app', () => ({ businessDocumentRuntimeBatch: vi.fn(), businessFlowStartConfig: vi.fn(), executeBusinessAction: vi.fn() }))
vi.mock('@/api/business-process', () => ({ businessProcessStartConfig: vi.fn(), startBusinessProcess: vi.fn() }))
vi.mock('@/store', () => ({ useUserStore: () => user }))
vi.mock('vue-router', () => ({ useRoute: () => ({ query: {}, params: {} }), useRouter: () => router }))
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
vi.mock('../../AiTable.vue', () => ({
  default: {
    name: 'AiTable',
    props: ['dataSource', 'columns'],
    methods: { setCheckedKeys() {}, clearSelection() {}, getCheckedRows: () => [] },
    render() {
      return h('div', (this.dataSource || []).map(row => h('div', this.columns.filter(column => column.render).map(column => column.render(row)))))
    },
  },
}))

const wrappers = []
const pass = { template: '<div><slot /><slot name="footer" /></div>' }
const overlay = { props: ['show'], template: '<div v-if="show"><slot /><slot name="footer" /></div>' }
function mountPage() {
  const wrapper = mount(AiCrudPage, { props: { lazy: true, rowKey: 'documentKey', columns: [{ key: 'actions', actions: [] }], runtimeActions: actions.filter(action => action.key.startsWith('forgePrint:')), hideAdd: true, hideBatchDelete: true, loadDetailOnEdit: false }, global: { stubs: { NModal: overlay, NDrawer: overlay, NDrawerContent: pass, NIcon: pass, NSpin: pass, NSpace: pass, NButton: { template: '<button><slot /></button>' }, NResult: pass, NSelect: true, NFormItem: pass, NInputNumber: true, NForm: pass, NDataTable: true, NAlert: pass, NTabs: pass, NTabPane: pass } } })
  wrappers.push(wrapper)
  return wrapper
}
beforeEach(() => {
  vi.clearAllMocks()
  user.getDataPermission = ['print:execute']
  window.$message = { success: vi.fn(), warning: vi.fn(), error: vi.fn() }
  vi.stubGlobal('requestAnimationFrame', vi.fn(() => 1))
  vi.stubGlobal('cancelAnimationFrame', vi.fn())
  vi.spyOn(window, 'open').mockImplementation(() => null)
})
afterEach(() => {
  wrappers.splice(0).forEach(wrapper => wrapper.unmount())
  vi.restoreAllMocks()
  vi.unstubAllGlobals()
})
const record = { documentKey: '采购/&?=9007199254740993', secret: '不得传入打印 URL' }
function assertRoute(scene) {
  expect(window.open).toHaveBeenCalledTimes(1)
  const url = new URL(window.open.mock.calls[0][0], 'https://local.invalid')
  expect(url.pathname).toBe('/print/preview')
  const query = Object.fromEntries(url.searchParams)
  expect(printRecordFromQuery(query)).toMatchObject({ source: { applicationId: '2', pageId: 'page_purchase', objectCode: 'purchase' }, scene, recordId: record.documentKey })
  expect(query).not.toHaveProperty('secret')
  expect(request).not.toHaveBeenCalled()
}
it('真实列表动作处理器将字符串主键编码后送入统一打印路由', async () => {
  const wrapper = mountPage()
  wrapper.vm.setTableData([record])
  await nextTick()
  await wrapper.get('a.table-action-link').trigger('click')
  await flushPromises()
  assertRoute('LIST')
})
it('真实详情动作使用 DETAIL 场景且不提交表单', async () => {
  const wrapper = mountPage()
  await wrapper.vm.showDetail(record)
  await flushPromises()
  const button = wrapper.findAll('button').find(item => item.text() === '打印')
  expect(button).toBeTruthy()
  await button.trigger('click')
  await flushPromises()
  assertRoute('DETAIL')
})
it('普通用户没有打印权限时不显示列表动作', async () => {
  user.getDataPermission = []
  const wrapper = mountPage()
  wrapper.vm.setTableData([record])
  await nextTick()
  expect(wrapper.find('a.table-action-link').exists()).toBe(false)
})
