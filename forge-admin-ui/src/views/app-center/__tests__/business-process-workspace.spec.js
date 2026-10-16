import { enableAutoUnmount, flushPromises, mount, shallowMount } from '@vue/test-utils'
import { NTab, NTabs } from 'naive-ui'
import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick, ref } from 'vue'
import { useProcessListStore } from '@/stores/business-process/processListStore'

import ApplicationProcessPanel from '../application-workspace/ApplicationProcessPanel.vue'
import ApplicationWorkspace from '../application.[applicationCode].vue'
import BusinessProcessPage from '../business-process.[processId].vue'

enableAutoUnmount(afterEach)

const processApiMocks = vi.hoisted(() => ({
  businessProcessPage: vi.fn(),
  createBusinessProcess: vi.fn(),
  copyBusinessProcess: vi.fn(),
  businessProcessDesigner: vi.fn(),
  businessProcessFlowModels: vi.fn(),
  saveBusinessProcessSchema: vi.fn(),
  validateBusinessProcess: vi.fn(),
  publishBusinessProcess: vi.fn(),
  updateBusinessProcessStatus: vi.fn(),
  deleteBusinessProcess: vi.fn(),
  businessProcessRunPage: vi.fn(),
  businessProcessRunDetail: vi.fn(),
  retryBusinessProcessRun: vi.fn(),
  cancelBusinessProcessRun: vi.fn(),
}))

const applicationApiMocks = vi.hoisted(() => ({
  businessApplicationObjects: vi.fn(),
  businessApplicationWorkspace: vi.fn(),
  businessApplicationWorkspaceByCode: vi.fn(),
  publishBusinessApplication: vi.fn(),
}))

const catalogApiMocks = vi.hoisted(() => ({
  businessObjectFields: vi.fn(),
  businessObjectActions: vi.fn(),
  businessFlowFormAssets: vi.fn(),
}))

const flowApiMocks = vi.hoisted(() => ({
  getModelList: vi.fn(),
}))

const messageApiMocks = vi.hoisted(() => ({
  getTemplatePage: vi.fn(),
}))

const routeState = vi.hoisted(() => ({
  params: { applicationCode: 'PURCHASE_APP', processId: '1900000000000003001' },
  query: {
    section: 'automation',
    processKeyword: '采购',
    processStatus: '1',
    returnTo: '/app-center/application/PURCHASE_APP?section=automation&processKeyword=%E9%87%87%E8%B4%AD',
    applicationCode: 'PURCHASE_APP',
  },
  fullPath: '/app-center/application/PURCHASE_APP?section=automation&processKeyword=%E9%87%87%E8%B4%AD',
  path: '/app-center/application/PURCHASE_APP',
  meta: {},
}))

const routerMocks = vi.hoisted(() => ({
  push: vi.fn(),
  replace: vi.fn(),
}))

const routeGuardState = vi.hoisted(() => ({ callback: null }))

vi.mock('@/api/business-process', () => processApiMocks)
vi.mock('@/api/business-application', () => applicationApiMocks)
vi.mock('@/api/business-app', () => catalogApiMocks)
vi.mock('@/api/flow', () => ({ default: flowApiMocks }))
vi.mock('@/api/message', () => ({ default: messageApiMocks }))
vi.mock('vue-router', async (importOriginal) => {
  const actual = await importOriginal()
  return {
    ...actual,
    useRoute: () => routeState,
    useRouter: () => routerMocks,
    onBeforeRouteLeave: (callback) => { routeGuardState.callback = callback },
  }
})

// application.vue 在 setup 中使用 useMessage，测试环境无 <n-message-provider>，mock 掉并保留其余导出
vi.mock('naive-ui', async (importOriginal) => {
  const actual = await importOriginal()
  return {
    ...actual,
    useMessage: () => ({
      success: vi.fn(),
      error: vi.fn(),
      warning: vi.fn(),
      info: vi.fn(),
      loading: vi.fn(),
    }),
  }
})

vi.mock('@/composables/useDict', () => ({
  useDict: () => ({
    dict: {
      value: {
        sys_normal_disable: [
          { label: '启用', value: 1 },
          { label: '停用', value: 0 },
        ],
      },
    },
  }),
}))

vi.mock('@/components/business-process-designer/BusinessProcessDesigner.vue', () => ({
  default: {
    name: 'BusinessProcessDesigner',
    props: [
      'schema',
      'saveState',
      'serverValidation',
      'flowModels',
      'fields',
      'formAssets',
      'businessActions',
      'messageTemplates',
      'subProcesses',
    ],
    emits: ['update:schema', 'save', 'validate', 'dirtyChange', 'refreshFlowModel', 'openFlowDesigner'],
    template: `
      <div data-process-designer :data-save-state="saveState">
        <button data-dirty @click="$emit('dirtyChange', true)">dirty</button>
        <button data-save @click="$emit('save', schema, { reason: 'manual', hashInput: 'client-dirty-hash' })">save</button>
        <button data-validate @click="$emit('validate', schema, { valid: true, issues: [] })">validate</button>
        <button data-refresh-flow @click="$emit('refreshFlowModel', 'sample_purchase_order_approval')">refresh</button>
        <button data-open-flow @click="$emit('openFlowDesigner', { modelId: '2001', modelKey: 'sample_purchase_order_approval', businessFormKey: 'purchase_form' })">open flow</button>
      </div>
    `,
  },
}))

const application = {
  id: '1900000000000000001',
  applicationCode: 'PURCHASE_APP',
  applicationName: '采购应用',
  suiteCode: 'PURCHASE',
}

const applicationObjects = [
  {
    objectId: '1900000000000001001',
    objectCode: 'sample_purchase_order',
    objectName: '采购单',
    objectRole: 'PRIMARY',
  },
]

const processRecord = {
  id: '1900000000000003001',
  applicationId: application.id,
  processCode: 'purchase_submit_approval',
  processName: '采购提交审批',
  subjectObjectId: applicationObjects[0].objectId,
  subjectObjectCode: applicationObjects[0].objectCode,
  draftSchemaHash: 'a'.repeat(64),
  designStatus: 'DRAFT',
  currentVersion: 0,
  publishedVersion: null,
  status: 1,
}

const processSchema = {
  schemaVersion: '1.0',
  processCode: processRecord.processCode,
  subject: {
    objectId: applicationObjects[0].objectId,
    objectCode: applicationObjects[0].objectCode,
    objectVersionId: null,
    recordIdSource: 'RUNTIME_RECORD',
  },
  nodes: [
    { id: 'start_manual', type: 'START_MANUAL', name: '提交审批', config: { positions: ['ROW'] } },
    { id: 'end_success', type: 'END', name: '完成', config: { result: 'SUCCESS' } },
  ],
  edges: [{ id: 'edge_1', source: 'start_manual', target: 'end_success', sourcePort: 'NEXT' }],
  policies: {},
  dependencies: {},
}

function processPageResponse(records = [processRecord]) {
  return {
    data: {
      records,
      total: records.length,
      current: 1,
      size: 10,
    },
  }
}

function designerResponse(overrides = {}) {
  return {
    data: {
      ...processRecord,
      businessProcessJson: processSchema,
      validation: { valid: true, errorCount: 0, warningCount: 0, issues: [] },
      ...overrides,
    },
  }
}

function panelMountOptions() {
  return {
    props: { application, initialObjects: applicationObjects },
    global: {
      plugins: [createPinia()],
      components: { NTabs, NTab },
      stubs: {
        AiTable: {
          name: 'AiTable',
          props: ['columns', 'dataSource', 'pagination', 'emptyTitle', 'emptyDescription'],
          emits: ['refresh', 'page-change', 'page-size-change'],
          template: `<div data-ai-table><slot name="toolbar-left" />
            <div v-for="row in dataSource" :key="row.id"><template v-for="column in columns" :key="column.prop">
              <slot v-if="column.slot" :name="column.slot" :row="row" /><span v-else>{{ row[column.prop] }}</span>
            </template></div><span v-if="!dataSource?.length">{{ emptyTitle }} {{ emptyDescription }}</span></div>`,
        },
        AiSearch: { name: 'AiSearch', props: ['schema', 'modelValue'], emits: ['search', 'reset'], template: '<div data-ai-search />' },
        AiForm: {
          name: 'AiForm',
          props: ['value', 'schema'],
          emits: ['update:value'],
          methods: { validate() { return this.value.processName?.trim() ? Promise.resolve() : Promise.reject(new Error('请输入流程名称')) } },
          template: '<input data-process-field="name" :value="value.processName" @input="$emit(\'update:value\', { ...value, processName: $event.target.value })">',
        },
        AiModal: {
          name: 'AiModal',
          setup(_, { expose }) {
            const show = ref(false)
            expose({
              open: () => { show.value = true },
              close: () => { show.value = false },
            })
            return { show }
          },
          template: '<div v-if="show" data-ai-modal><slot /><slot name="footer" /></div>',
        },
        SystemTableCell: {
          name: 'SystemTableCell',
          props: ['title', 'subtitle', 'interactive'],
          emits: ['activate'],
          template: '<div class="system-table-cell"><span class="cell-title">{{ title }}</span><span v-if="subtitle" class="cell-subtitle">{{ subtitle }}</span></div>',
        },
        NDropdown: {
          props: ['options'],
          emits: ['select'],
          template: '<div><slot /><button v-for="option in options.filter(item => item.key !== \'divider\')" :key="option.key" :data-menu-key="option.key" :disabled="option.disabled" @click="$emit(\'select\', option.key)">{{ option.label }}</button></div>',
        },
        NDivider: true,
        NDescriptions: { template: '<div><slot /></div>' },
        NDescriptionsItem: { template: '<div><slot /></div>' },
        NResult: { props: ['title', 'description'], template: '<div>{{ title }} {{ description }}<slot name="footer" /></div>' },
        DictTag: { props: ['value'], template: '<span class="dict-tag">{{ value }}</span>' },
        NButton: { emits: ['click'], template: '<button v-bind="$attrs" @click="$emit(\'click\')"><slot /></button>' },
        NModal: {
          props: ['show'],
          template: '<div v-if="show" class="n-modal"><slot /><slot name="footer" /></div>',
        },
        NPagination: {
          props: ['page'],
          template: '<button data-page-two @click="$emit(\'update:page\', 2)">2</button>',
        },
        NSpin: { template: '<div><slot /></div>' },
        NEmpty: { template: '<div><slot /></div>' },
      },
    },
  }
}

function designerMountOptions() {
  return {
    global: {
      stubs: {
        NSpin: { template: '<div><slot /></div>' },
        NResult: { template: '<div><slot /><slot name="footer" /></div>' },
        NButton: { template: '<button v-bind="$attrs" @click="$emit(\'click\')"><slot /></button>' },
        NModal: {
          props: ['show'],
          template: '<div v-if="show" class="n-modal"><slot /></div>',
        },
        FlowDesignPage: {
          props: ['modelId', 'businessFormKey'],
          emits: ['close', 'saved', 'deployed'],
          template: `
            <div data-flow-design-page :data-model-id="modelId" :data-form-key="businessFormKey">
              <button data-flow-save @click="$emit('saved', { id: modelId, modelKey: 'sample_purchase_order_approval', status: 0 })">save</button>
              <button data-flow-deploy @click="$emit('deployed', { id: modelId, modelKey: 'sample_purchase_order_approval', status: 1, deploymentId: 'dep-2' })">deploy</button>
              <button data-flow-close @click="$emit('close')">close</button>
            </div>
          `,
        },
      },
    },
  }
}

describe('application business process panel', () => {
  beforeEach(() => {
    Object.values(processApiMocks).forEach(mock => mock.mockReset())
    applicationApiMocks.publishBusinessApplication.mockReset()
    processApiMocks.businessProcessPage.mockResolvedValue(processPageResponse())
    processApiMocks.createBusinessProcess.mockResolvedValue(designerResponse())
    processApiMocks.copyBusinessProcess.mockResolvedValue(designerResponse({ id: '1900000000000003002' }))
    processApiMocks.updateBusinessProcessStatus.mockResolvedValue({ data: null })
    processApiMocks.deleteBusinessProcess.mockResolvedValue({ data: null })
    processApiMocks.businessProcessRunPage.mockResolvedValue({ data: { records: [], total: 0 } })
    processApiMocks.retryBusinessProcessRun.mockResolvedValue({ data: null })
    processApiMocks.cancelBusinessProcessRun.mockResolvedValue({ data: null })
    routerMocks.replace.mockReset()
    window.$message = { success: vi.fn(), error: vi.fn(), info: vi.fn(), warning: vi.fn() }
    window.$dialog = {
      warning: vi.fn(options => options.onPositiveClick?.()),
    }
  })

  afterEach(() => {
    delete window.$message
    delete window.$dialog
  })

  it('lists processes and creates one for the current application object', async () => {
    const wrapper = mount(ApplicationProcessPanel, panelMountOptions())
    await flushPromises()

    expect(processApiMocks.businessProcessPage).toHaveBeenCalledWith(expect.objectContaining({
      applicationId: application.id,
      keyword: '采购',
      status: 1,
      pageNum: 1,
      pageSize: 10,
    }))
    expect(wrapper.text()).toContain('采购提交审批')
    expect(wrapper.text()).toContain('业务流程')

    await wrapper.find('[data-process-action="create"]').trigger('click')
    await wrapper.find('[data-process-field="name"]').setValue('采购自动审批')
    await wrapper.find('[data-process-action="confirm-create"]').trigger('click')
    await flushPromises()

    expect(processApiMocks.createBusinessProcess).toHaveBeenCalledWith({
      applicationId: application.id,
      processName: '采购自动审批',
      processDescription: '',
      subjectObjectId: applicationObjects[0].objectId,
      status: 1,
    })
    expect(wrapper.emitted('openDesigner').at(-1)[0]).toEqual({
      processId: processRecord.id,
    })
  })

  it('copies, toggles and logically deletes through existing control-plane endpoints', async () => {
    const wrapper = mount(ApplicationProcessPanel, panelMountOptions())
    await flushPromises()

    await wrapper.find('[data-menu-key="copy"]').trigger('click')
    await flushPromises()
    expect(processApiMocks.copyBusinessProcess).toHaveBeenCalledWith(processRecord.id, {})

    await wrapper.find('[data-menu-key="status"]').trigger('click')
    await flushPromises()
    expect(processApiMocks.updateBusinessProcessStatus).toHaveBeenCalledWith(processRecord.id, 0)

    await wrapper.find('[data-menu-key="delete"]').trigger('click')
    await flushPromises()
    expect(processApiMocks.deleteBusinessProcess).toHaveBeenCalledWith(processRecord.id)
    expect(window.$dialog.warning).toHaveBeenCalled()
  })

  it('publishes a single process through the standalone publish endpoint after validation', async () => {
    processApiMocks.validateBusinessProcess.mockResolvedValue({
      data: { valid: true, errorCount: 0, warningCount: 0, issues: [] },
    })
    processApiMocks.publishBusinessProcess.mockResolvedValue({
      data: { versionNo: 1, processCode: processRecord.processCode },
    })
    const wrapper = mount(ApplicationProcessPanel, panelMountOptions())
    await flushPromises()

    await wrapper.find(`[data-process-publish="${processRecord.id}"]`).trigger('click')
    await flushPromises()

    expect(processApiMocks.validateBusinessProcess).toHaveBeenCalledWith(processRecord.id)
    expect(processApiMocks.publishBusinessProcess).toHaveBeenCalledWith(processRecord.id)
    expect(applicationApiMocks.publishBusinessApplication).not.toHaveBeenCalled()
    expect(window.$message.success).toHaveBeenCalled()
  })

  it('blocks single-process publishing when validation fails', async () => {
    processApiMocks.validateBusinessProcess.mockResolvedValue({
      data: { valid: false, errorCount: 2, warningCount: 0, issues: [] },
    })
    const wrapper = mount(ApplicationProcessPanel, panelMountOptions())
    await flushPromises()

    await wrapper.find(`[data-process-publish="${processRecord.id}"]`).trigger('click')
    await flushPromises()

    expect(processApiMocks.publishBusinessProcess).not.toHaveBeenCalled()
    expect(window.$message.warning).toHaveBeenCalled()
  })

  it('loads runtime records from the run page endpoint', async () => {
    processApiMocks.businessProcessRunPage.mockResolvedValue({
      data: {
        records: [{
          id: '1900000000000009001',
          processCode: 'submit_approval',
          processName: '提交审批',
          subjectObjectCode: 'order',
          subjectRecordId: '12',
          status: 'SUCCESS',
          currentNodeId: 'end',
        }],
        total: 1,
      },
    })
    const wrapper = mount(ApplicationProcessPanel, panelMountOptions())
    await flushPromises()
    await wrapper.find('[data-process-section="runs"]').trigger('click')
    await flushPromises()

    // 运行面板已按需动态导入；等待真实子组件挂载，不只等待当前一轮微任务。
    await vi.waitFor(() => expect(processApiMocks.businessProcessRunPage).toHaveBeenCalledWith(expect.objectContaining({
      applicationId: application.id,
      pageNum: 1,
      pageSize: 10,
    })))
    expect(wrapper.text()).toContain('提交审批')
    expect(wrapper.text()).toContain('order')
  })

  it('resets filters and pagination through the shared search and table contracts', async () => {
    const wrapper = shallowMount(ApplicationProcessPanel, panelMountOptions())
    await flushPromises()
    wrapper.findComponent({ name: 'AiTable' }).vm.$emit('page-change', 3)
    await flushPromises()
    wrapper.findComponent({ name: 'AiSearch' }).vm.$emit('search', { keyword: ' 自动 ', status: 0 })
    await flushPromises()
    expect(processApiMocks.businessProcessPage).toHaveBeenLastCalledWith(expect.objectContaining({ keyword: '自动', status: 0, pageNum: 1 }))
    wrapper.findComponent({ name: 'AiTable' }).vm.$emit('page-size-change', 20)
    await flushPromises()
    expect(processApiMocks.businessProcessPage).toHaveBeenLastCalledWith(expect.objectContaining({ pageNum: 1, pageSize: 20 }))
    wrapper.findComponent({ name: 'AiSearch' }).vm.$emit('reset')
    await flushPromises()
    expect(processApiMocks.businessProcessPage).toHaveBeenLastCalledWith(expect.objectContaining({ keyword: undefined, status: null, pageNum: 1 }))
    expect(routerMocks.replace).toHaveBeenLastCalledWith(expect.objectContaining({ query: expect.objectContaining({ processKeyword: undefined, processStatus: undefined }) }))
  })

  it('preserves string IDs when opening process-scoped runs and loads remote options', async () => {
    const wrapper = mount(ApplicationProcessPanel, panelMountOptions())
    await flushPromises()
    await wrapper.find('[data-menu-key="runs"]').trigger('click')
    await flushPromises()
    await nextTick()
    await vi.waitFor(() => expect(processApiMocks.businessProcessRunPage)
      .toHaveBeenLastCalledWith(expect.objectContaining({ processId: processRecord.id })))
    expect(processApiMocks.businessProcessPage).toHaveBeenLastCalledWith(expect.objectContaining({ pageSize: 50, keyword: undefined }))
    expect(wrapper.findComponent({ name: 'AiSearch' }).props('modelValue')).toEqual({ processId: processRecord.id })
    wrapper.findComponent({ name: 'AiSearch' }).vm.$emit('reset')
    await flushPromises()
    expect(processApiMocks.businessProcessRunPage).toHaveBeenLastCalledWith(expect.objectContaining({ processId: undefined }))
  })

  it('distinguishes failed loading from empty matches and permits retry', async () => {
    processApiMocks.businessProcessPage.mockRejectedValueOnce(new Error('列表连接失败'))
    const wrapper = shallowMount(ApplicationProcessPanel, panelMountOptions())
    await flushPromises()
    expect(wrapper.text()).toContain('业务流程加载失败')
    expect(wrapper.text()).toContain('列表连接失败')
    processApiMocks.businessProcessPage.mockResolvedValue(processPageResponse([]))
    wrapper.findComponent({ name: 'AiTable' }).vm.$emit('refresh')
    await flushPromises()
    expect(wrapper.text()).toContain('没有匹配的业务流程')
  })

  it('keeps create data on failure and prevents duplicate submissions', async () => {
    let rejectCreate
    processApiMocks.createBusinessProcess.mockImplementation(() => new Promise((_, reject) => {
      rejectCreate = reject
    }))
    const wrapper = shallowMount(ApplicationProcessPanel, panelMountOptions())
    await flushPromises()
    await wrapper.find('[data-process-action="create"]').trigger('click')
    await wrapper.find('[data-process-action="confirm-create"]').trigger('click')
    await flushPromises()
    expect(processApiMocks.createBusinessProcess).not.toHaveBeenCalled()
    await wrapper.find('[data-process-field="name"]').setValue('重试创建')
    await wrapper.find('[data-process-action="confirm-create"]').trigger('click')
    await wrapper.find('[data-process-action="confirm-create"]').trigger('click')
    await flushPromises()
    expect(processApiMocks.createBusinessProcess).toHaveBeenCalledTimes(1)
    rejectCreate(new Error('创建失败'))
    await flushPromises()
    expect(wrapper.find('[data-process-field="name"]').element.value).toBe('重试创建')
    expect(window.$message.error).toHaveBeenCalledWith('创建失败')
  })

  it('ignores stale list responses after a newer search', async () => {
    let resolveOld
    processApiMocks.businessProcessPage.mockImplementationOnce(() => new Promise((resolve) => {
      resolveOld = resolve
    }))
    const wrapper = shallowMount(ApplicationProcessPanel, panelMountOptions())
    wrapper.findComponent({ name: 'AiSearch' }).vm.$emit('search', { keyword: '最新' })
    await flushPromises()
    resolveOld(processPageResponse([{ ...processRecord, processName: '过期结果' }]))
    await flushPromises()
    expect(wrapper.text()).not.toContain('过期结果')
    expect(wrapper.text()).toContain(processRecord.processName)
  })

  it('retains delete confirmation on failure and describes historical retention', async () => {
    let confirmation
    window.$dialog.warning.mockImplementation((options) => {
      confirmation = options
    })
    processApiMocks.deleteBusinessProcess.mockRejectedValue(new Error('仍有活跃运行'))
    const wrapper = mount(ApplicationProcessPanel, panelMountOptions())
    await flushPromises()
    await wrapper.find('[data-menu-key="delete"]').trigger('click')
    expect(confirmation.content).toContain('历史版本和运行记录会保留')
    await expect(confirmation.onPositiveClick()).resolves.toBe(false)
    expect(window.$message.error).toHaveBeenCalledWith('仍有活跃运行')
  })

  it('loads details and confirms cancellation only for active runs', async () => {
    const run = { id: '1900000000000009001', processName: '提交审批', status: 'WAITING' }
    processApiMocks.businessProcessRunPage.mockResolvedValue({ data: { records: [run], total: 1 } })
    processApiMocks.businessProcessRunDetail.mockResolvedValue({ data: { ...run, timeline: [{ id: 'node-1', nodeName: '审批节点', status: 'WAITING' }] } })
    let confirmation
    window.$dialog.warning.mockImplementation((options) => {
      confirmation = options
      return { loading: false }
    })
    const wrapper = mount(ApplicationProcessPanel, panelMountOptions())
    await flushPromises()
    await wrapper.find('[data-menu-key="runs"]').trigger('click')
    await flushPromises()
    await wrapper.find(`[data-run-detail="${run.id}"]`).trigger('click')
    await flushPromises()
    expect(processApiMocks.businessProcessRunDetail).toHaveBeenCalledWith(run.id)
    expect(wrapper.text()).toContain('审批节点')
    await wrapper.findAll('button').find(item => item.text() === '取消运行').trigger('click')
    expect(processApiMocks.cancelBusinessProcessRun).not.toHaveBeenCalled()
    await confirmation.onPositiveClick()
    expect(processApiMocks.cancelBusinessProcessRun).toHaveBeenCalledWith(run.id)
  })
})

describe('process list shared state isolation', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    processApiMocks.businessProcessRunPage.mockReset()
  })

  it('rejects stale run responses across application switches', async () => {
    const store = useProcessListStore()
    store.syncContext(application, applicationObjects)
    let resolveOld
    processApiMocks.businessProcessRunPage.mockImplementationOnce(() => new Promise((resolve) => {
      resolveOld = resolve
    }))
    const pending = store.loadRuns()
    store.viewRuns(processRecord)
    store.syncContext({ id: '1900000000000000002' }, [])
    resolveOld({ data: { records: [{ id: 'old-run' }], total: 1 } })
    await pending
    expect(store.runRecords).toEqual([])
    expect(store.runFilters).toEqual({})
    expect(store.activeSection).toBe('list')
    expect(store.runLoading).toBe(false)
  })

  it('keeps selected labels when searching beyond the first result page', async () => {
    const store = useProcessListStore()
    store.syncContext(application, applicationObjects)
    store.viewRuns(processRecord)
    processApiMocks.businessProcessPage.mockResolvedValue(processPageResponse([{ ...processRecord, id: '1900000000000003099', processName: '远程流程' }]))
    await store.searchProcesses('远程')
    expect(processApiMocks.businessProcessPage).toHaveBeenLastCalledWith({ applicationId: application.id, keyword: '远程', pageNum: 1, pageSize: 50 })
    expect(store.processOptions.map(item => item.value)).toEqual([processRecord.id, '1900000000000003099'])
  })
})

describe('application workspace process routing', () => {
  beforeEach(() => {
    applicationApiMocks.businessApplicationWorkspaceByCode.mockReset().mockResolvedValue({
      data: {
        application,
        objects: applicationObjects,
        sections: [{ sectionKey: 'automation', sectionName: '流程自动化' }],
      },
    })
    routerMocks.push.mockReset()
    routerMocks.replace.mockReset()
  })

  it('replaces the old automation section with a designer redirect card', async () => {
    processApiMocks.businessProcessPage.mockResolvedValue(processPageResponse())
    const wrapper = mount(ApplicationWorkspace, {
      global: {
        stubs: {
          ApplicationWorkspaceHeader: true,
          ApplicationWorkspaceNav: true,
          KeepAlive: false,
          AppCodePanel: true,
          DictTag: { props: ['value'], template: '<span>{{ value }}</span>' },
          NButton: { template: '<button v-bind="$attrs" @click="$emit(\'click\')"><slot /></button>' },
          NModal: { props: ['show'], template: '<div v-if="show"><slot /><slot name="footer" /></div>' },
          NPagination: true,
          NSpin: { template: '<div><slot /></div>' },
          NEmpty: { template: '<div><slot /></div>' },
          NResult: { template: '<div><slot /><slot name="footer" /></div>' },
        },
      },
    })
    await flushPromises()
    await nextTick()

    // 业务流程已收敛进应用设计器：工作台不再渲染流程面板，旧 automation 链接落地引导卡。
    expect(wrapper.find('[data-workspace-process]').exists()).toBe(false)
    expect(wrapper.find('.designer-redirect-card').exists()).toBe(true)
    await wrapper.find('.designer-redirect-card button').trigger('click')

    expect(routerMocks.push).toHaveBeenCalledWith({
      name: 'BusinessApplicationRuntime',
      params: { applicationCode: application.applicationCode },
      query: { edit: '1', designSection: 'automation' },
    })
  })
})

describe('full-screen business process designer page', () => {
  beforeEach(() => {
    Object.values(processApiMocks).forEach(mock => mock.mockReset())
    Object.values(applicationApiMocks).forEach(mock => mock.mockReset())
    Object.values(catalogApiMocks).forEach(mock => mock.mockReset())
    messageApiMocks.getTemplatePage.mockReset()
    routerMocks.push.mockReset()
    routeGuardState.callback = null

    processApiMocks.businessProcessDesigner.mockResolvedValue(designerResponse())
    processApiMocks.businessProcessFlowModels.mockResolvedValue({
      data: [{
        modelId: '2001',
        modelKey: 'sample_purchase_order_approval',
        modelName: '采购审批',
        status: 1,
        version: 1,
        deploymentId: 'dep-1',
        deployed: true,
      }],
    })
    processApiMocks.businessProcessPage.mockResolvedValue(processPageResponse())
    processApiMocks.saveBusinessProcessSchema.mockResolvedValue(designerResponse({ draftSchemaHash: 'b'.repeat(64) }))
    processApiMocks.validateBusinessProcess.mockResolvedValue({
      data: { valid: true, errorCount: 0, warningCount: 0, issues: [] },
    })
    applicationApiMocks.businessApplicationObjects.mockResolvedValue({ data: applicationObjects })
    catalogApiMocks.businessObjectFields.mockResolvedValue({ data: [{ fieldCode: 'status', fieldName: '状态' }] })
    catalogApiMocks.businessObjectActions.mockResolvedValue({ data: [{ actionCode: 'submit', actionName: '提交', status: 1 }] })
    catalogApiMocks.businessFlowFormAssets.mockResolvedValue({ data: { formAssets: [{ formKey: 'purchase_form' }] } })
    messageApiMocks.getTemplatePage.mockResolvedValue({ data: { records: [{ templateCode: 'purchase_notice', templateName: '采购通知', status: 1 }] } })
    window.$message = { success: vi.fn(), error: vi.fn(), warning: vi.fn() }
  })

  afterEach(() => {
    delete window.$message
    delete window.$dialog
  })

  it('loads governed catalogs and saves with the latest server draft hash', async () => {
    const wrapper = mount(BusinessProcessPage, designerMountOptions())
    await flushPromises()

    expect(processApiMocks.businessProcessDesigner).toHaveBeenCalledWith(processRecord.id)
    expect(catalogApiMocks.businessObjectFields).toHaveBeenCalledWith(applicationObjects[0].objectId)
    expect(processApiMocks.businessProcessFlowModels).toHaveBeenCalledWith(processRecord.id)

    await wrapper.find('[data-dirty]').trigger('click')
    await wrapper.find('[data-save]').trigger('click')
    await flushPromises()

    expect(processApiMocks.saveBusinessProcessSchema).toHaveBeenCalledWith(processRecord.id, {
      businessProcessJson: processSchema,
      expectedSchemaHash: 'a'.repeat(64),
    })
    expect(wrapper.find('[data-process-designer]').attributes('data-save-state')).toBe('saved')
  })

  it('owns one embedded approval designer and keeps it open while refreshing after save or deploy', async () => {
    const wrapper = mount(BusinessProcessPage, {
      ...designerMountOptions(),
      props: { embedded: true, processId: processRecord.id },
    })
    await flushPromises()

    await wrapper.find('[data-open-flow]').trigger('click')
    await flushPromises()

    expect(wrapper.findAll('[data-flow-design-page]')).toHaveLength(1)
    expect(wrapper.find('[data-flow-design-page]').attributes('data-model-id')).toBe('2001')
    expect(wrapper.find('[data-flow-design-page]').attributes('data-form-key')).toBe('purchase_form')

    const callsBeforeSave = processApiMocks.businessProcessFlowModels.mock.calls.length
    await wrapper.find('[data-flow-save]').trigger('click')
    await flushPromises()
    expect(wrapper.findAll('[data-flow-design-page]')).toHaveLength(1)
    expect(processApiMocks.businessProcessFlowModels.mock.calls.length).toBeGreaterThan(callsBeforeSave)

    await wrapper.find('[data-flow-deploy]').trigger('click')
    await flushPromises()
    expect(wrapper.findAll('[data-flow-design-page]')).toHaveLength(1)

    await wrapper.find('[data-flow-close]').trigger('click')
    await flushPromises()
    expect(wrapper.find('[data-flow-design-page]').exists()).toBe(false)
  })

  it('saves dirty content before validation and exposes HTTP 409 as a conflict', async () => {
    const callOrder = []
    processApiMocks.saveBusinessProcessSchema.mockImplementation(async () => {
      callOrder.push('save')
      return designerResponse({ draftSchemaHash: 'b'.repeat(64) })
    })
    processApiMocks.validateBusinessProcess.mockImplementation(async () => {
      callOrder.push('validate')
      return { data: { valid: true, issues: [] } }
    })
    const wrapper = mount(BusinessProcessPage, designerMountOptions())
    await flushPromises()

    await wrapper.find('[data-dirty]').trigger('click')
    await wrapper.find('[data-validate]').trigger('click')
    await flushPromises()
    expect(callOrder).toEqual(['save', 'validate'])

    processApiMocks.saveBusinessProcessSchema.mockRejectedValueOnce({
      response: { status: 409, data: { message: '草稿冲突' } },
    })
    await wrapper.find('[data-dirty]').trigger('click')
    await wrapper.find('[data-save]').trigger('click')
    await flushPromises()
    expect(wrapper.find('[data-process-designer]').attributes('data-save-state')).toBe('conflict')
  })

  it('confirms route leave for a dirty draft and returns to the original filtered application section', async () => {
    const wrapper = mount(BusinessProcessPage, designerMountOptions())
    await flushPromises()
    await wrapper.find('[data-dirty]').trigger('click')

    let confirmOptions
    const captureConfirm = vi.fn((options) => {
      confirmOptions = options
    })
    window.$dialog = { warning: captureConfirm }
    const navigation = routeGuardState.callback()
    confirmOptions.onPositiveClick()
    await expect(navigation).resolves.toBe(true)

    await wrapper.find('[data-process-action="back"]').trigger('click')
    expect(routerMocks.push).toHaveBeenCalledWith(routeState.query.returnTo)
  })

  it('adds a refresh token when returning to a page button designer', async () => {
    const previousQuery = routeState.query
    routeState.query = {
      applicationCode: 'PURCHASE_APP',
      from: 'button',
      objectCode: 'sample_purchase_order',
      returnTo: '/app-center/object/sample_purchase_order/designer?panel=form&detailTab=sections&applicationCode=PURCHASE_APP',
    }
    try {
      const wrapper = mount(BusinessProcessPage, designerMountOptions())
      await flushPromises()

      await wrapper.find('[data-process-action="back"]').trigger('click')

      expect(routerMocks.push).toHaveBeenCalledWith(
        `/app-center/object/sample_purchase_order/designer?panel=form&detailTab=sections&applicationCode=PURCHASE_APP&processRefresh=${processRecord.id}`,
      )
    }
    finally {
      routeState.query = previousQuery
    }
  })
})
