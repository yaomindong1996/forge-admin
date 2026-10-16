import { flushPromises, mount, shallowMount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AiCrudPage from '@/components/ai-form/AiCrudPage.vue'
import GridBlockRenderer from '../GridBlockRenderer.vue'

const requestMock = vi.hoisted(() => vi.fn())
const postEncryptMock = vi.hoisted(() => vi.fn())

// 只替换子组件的实现，沿用真实 props 契约，隔离 CRUD 的路由/网络等依赖。
vi.mock('@/components/ai-form/AiCrudPage.vue', async () => {
  const { aiCrudPageProps } = await import('@/components/ai-form/AiCrudPageProps')
  return { default: { name: 'AiCrudPage', props: aiCrudPageProps, template: '<div class="crud-props-probe" />' } }
})

vi.mock('@/utils', () => ({
  postEncrypt: postEncryptMock,
  request: requestMock,
}))

vi.mock('vue-router', () => ({
  useRoute: () => ({ query: {}, params: {}, path: '/', fullPath: '/', name: 'test' }),
  useRouter: () => ({ push: vi.fn(), resolve: vi.fn(() => ({ href: '/' })) }),
}))

vi.mock('@/router', () => ({
  default: { push: vi.fn(), resolve: vi.fn(() => ({ href: '/' })) },
  router: { push: vi.fn(), resolve: vi.fn(() => ({ href: '/' })) },
}))

const STUBS = {
  AiForm: {
    name: 'AiForm',
    props: ['schema', 'showSubmit', 'submitLoading', 'value'],
    emits: ['submit'],
    template: `
      <div class="ai-form-stub">
        <span>{{ schema.map(item => item.label).join(",") }}</span>
        <button
          v-if="showSubmit"
          class="ai-form-submit-stub"
          :disabled="submitLoading"
          @click="$emit('submit', { customerName: '新客户' })"
        >提交</button>
      </div>
    `,
  },
  NButton: {
    template: '<button><slot /></button>',
  },
  NIcon: {
    template: '<i><slot /></i>',
  },
  NAvatar: true,
  NDataTable: true,
  NDatePicker: true,
  NDropdown: true,
  NFormItem: true,
  NInput: true,
  NSelect: true,
  NSpin: true,
  NStep: true,
  NSteps: true,
  NTabPane: true,
  NTabs: true,
  NTag: true,
  NTransfer: true,
  NTree: true,
  NTreeSelect: true,
  NWatermark: true,
}

function mountAiForm(props = {}) {
  return shallowMount(GridBlockRenderer, {
    props: {
      block: {
        id: 'form_1',
        blockType: 'AiForm',
        fieldRefs: [],
        props: {},
      },
      fields: [],
      showDataSourceGuide: true,
      ...props,
    },
    global: {
      plugins: [createPinia()],
      stubs: STUBS,
    },
  })
}

describe('grid block renderer data source experience', () => {
  beforeEach(() => {
    requestMock.mockReset()
    postEncryptMock.mockReset()
    window.$message = {
      error: vi.fn(),
      success: vi.fn(),
      warning: vi.fn(),
      info: vi.fn(),
    }
  })

  it('guides an editing user to select a business object instead of rendering a blank form', async () => {
    const wrapper = mountAiForm()

    expect(wrapper.find('.data-source-guide').text()).toContain('选择业务对象后，字段将自动生成')
    await wrapper.find('.data-source-guide button').trigger('click')
    expect(wrapper.emitted('requestDataSource')).toEqual([['form_1']])
  })

  it.each([
    [{ formOnly: true }, true],
    [{ objectRef: { pageMode: 'form' } }, true],
    [{ objectRef: { pageKey: 'form' } }, true],
    [{ formOnly: false, objectRef: { pageMode: 'form' } }, false],
    [{ objectRef: { pageMode: 'list' } }, false],
    [{ objectRef: { pageMode: 'crud', pageKey: 'form' } }, false],
  ])('passes the page shape to the actual CRUD component: %o', async (blockProps, formOnly) => {
    const wrapper = mountAiForm({
      block: { id: 'crud_shape', blockType: 'AiCrudPage', fieldRefs: ['customerName'], props: blockProps },
      readonly: true,
      runtimeInteractive: true,
      dataSourceConfigured: true,
      runtimeCrudProps: {
        configKey: 'customer',
        formOnly: true,
        formOpenMode: 'flat',
        editSchema: [{ field: 'customerName', label: '客户名称', type: 'input' }],
        columns: [{ prop: 'customerName', label: '客户名称' }],
      },
    })
    await flushPromises()
    const crud = wrapper.findComponent(AiCrudPage)
    expect(crud.exists()).toBe(true)
    expect(crud.props('formOnly')).toBe(formOnly)
    expect(crud.props('editSchema')).toEqual(expect.arrayContaining([expect.objectContaining({ field: 'customerName' })]))
    expect(crud.props('columns')).toHaveLength(1)
    expect(crud.props('formOpenMode')).toBe('flat')
  })

  it('renders a form-shaped static preview and retains submission protection', async () => {
    const wrapper = mountAiForm({
      block: {
        id: 'form_preview',
        blockType: 'AiCrudPage',
        fieldRefs: ['customerName'],
        props: { formOnly: true, formOnlyTitle: '登记', formOnlySubmitText: '提交登记' },
      },
      dataSourceConfigured: true,
      fields: [{ field: 'customerName', label: '客户名称', formVisible: true }],
    })
    await flushPromises()
    const crud = wrapper.findComponent(AiCrudPage)
    expect(crud.exists()).toBe(true)
    expect(crud.props('formOnly')).toBe(true)
    expect(crud.props('formOnlyTitle')).toBe('登记')
    expect(crud.props('formOnlySubmitText')).toBe('提交登记')
    expect(crud.props('lazy')).toBe(true)
    expect(crud.props('beforeSubmit')({})).toBe(false)
  })

  it('renders runtime object fields immediately when field refs have not been configured', () => {
    const wrapper = mountAiForm({
      dataSourceConfigured: true,
      fields: [
        { field: 'customerName', label: '客户名称', componentType: 'input' },
        { field: 'amount', label: '金额', componentType: 'number' },
      ],
    })

    expect(wrapper.find('.data-source-guide').exists()).toBe(false)
    expect(wrapper.find('.ai-form-stub').text()).toBe('客户名称,金额')
  })

  it('keeps explicit form field order and excludes fields hidden by component settings', () => {
    const wrapper = mountAiForm({
      dataSourceConfigured: true,
      block: {
        id: 'form_1',
        blockType: 'AiForm',
        fieldRefs: ['amount', 'internalNote', 'customerName'],
        props: {
          fieldSettings: {
            internalNote: { visible: false },
          },
        },
      },
      fields: [
        { field: 'customerName', label: '客户名称', formVisible: true },
        { field: 'amount', label: '金额', formVisible: true },
        { field: 'internalNote', label: '内部备注', formVisible: true },
      ],
    })

    expect(wrapper.find('.ai-form-stub span').text()).toBe('金额,客户名称')
  })

  it('uses only safe form-visible fields when field refs are empty', () => {
    const wrapper = mountAiForm({
      dataSourceConfigured: true,
      fields: [
        { field: 'id', label: 'ID', formVisible: true, systemField: true },
        { field: 'customerName', label: '客户名称', formVisible: true },
        { field: 'listRemark', label: '列表备注', formVisible: false },
        { field: 'disabledField', label: '停用字段', formVisible: true, fieldStatus: 'DISABLED' },
      ],
    })

    expect(wrapper.find('.ai-form-stub span').text()).toBe('客户名称')
  })

  it('uses a quiet read-only hint without exposing a configuration action at runtime', () => {
    const wrapper = mountAiForm({ readonly: true })

    expect(wrapper.find('.data-source-guide').classes()).toContain('is-readonly')
    expect(wrapper.find('.data-source-guide button').exists()).toBe(false)
  })

  it('shows the data-source guide for a stale object reference', () => {
    const wrapper = mountAiForm({
      block: {
        id: 'form_1',
        blockType: 'AiForm',
        fieldRefs: [],
        props: {
          objectRef: { objectId: 'deleted-object', valid: false },
        },
      },
      dataSourceConfigured: false,
    })

    expect(wrapper.find('.data-source-guide').exists()).toBe(true)
  })

  it('submits to the configured create endpoint only in interactive runtime mode', async () => {
    requestMock.mockResolvedValue({ data: { id: '1' } })
    const preview = mountAiForm({
      dataSourceConfigured: true,
      readonly: false,
      runtimeInteractive: false,
      block: {
        id: 'form_1',
        blockType: 'AiForm',
        fieldRefs: ['customerName'],
        props: { createApi: 'post@/ai/crud/customer' },
      },
      fields: [{ field: 'customerName', label: '客户名称', formVisible: true }],
    })
    expect(preview.find('.ai-form-submit-stub').exists()).toBe(false)

    const runtime = mountAiForm({
      dataSourceConfigured: true,
      readonly: true,
      runtimeInteractive: true,
      block: {
        id: 'form_1',
        blockType: 'AiForm',
        fieldRefs: ['customerName'],
        props: {
          createApi: 'post@/ai/crud/customer',
          submitDefaultParams: { source: 'page-builder' },
        },
      },
      fields: [{ field: 'customerName', label: '客户名称', formVisible: true }],
    })
    await runtime.find('.ai-form-submit-stub').trigger('click')
    await flushPromises()

    expect(requestMock).toHaveBeenCalledWith({
      method: 'post',
      url: '/ai/crud/customer',
      data: { source: 'page-builder', customerName: '新客户' },
      needTip: false,
    })
    expect(window.$message.success).toHaveBeenCalledWith('提交成功')
  })

  it('runs standalone form extension hooks before and after submission', async () => {
    requestMock.mockResolvedValue({ data: { id: '1' } })
    const beforeSubmit = vi.fn(async data => ({ ...data, enhanced: true }))
    const afterSubmit = vi.fn()
    const wrapper = mountAiForm({
      dataSourceConfigured: true,
      readonly: true,
      runtimeInteractive: true,
      runtimeExtensionHooks: { beforeSubmit, afterSubmit },
      block: {
        id: 'form_1',
        blockType: 'AiForm',
        fieldRefs: ['customerName'],
        props: { createApi: 'post@/ai/crud/customer' },
      },
      fields: [{ field: 'customerName', label: '客户名称', formVisible: true }],
    })

    await wrapper.find('.ai-form-submit-stub').trigger('click')
    await flushPromises()

    expect(beforeSubmit).toHaveBeenCalledWith(
      { customerName: '新客户' },
      expect.objectContaining({ triggerAction: expect.any(Function) }),
    )
    expect(requestMock).toHaveBeenCalledWith(expect.objectContaining({
      data: { customerName: '新客户', enhanced: true },
    }))
    expect(afterSubmit).toHaveBeenCalledWith(
      expect.objectContaining({
        data: { customerName: '新客户', enhanced: true },
        response: { data: { id: '1' } },
        isEdit: false,
      }),
      expect.objectContaining({ triggerAction: expect.any(Function) }),
    )
  })

  it('blocks standalone form submission when a pre-submit extension fails', async () => {
    const wrapper = mountAiForm({
      dataSourceConfigured: true,
      readonly: true,
      runtimeInteractive: true,
      runtimeExtensionHooks: {
        beforeSubmit: vi.fn().mockRejectedValue(new Error('增强校验失败')),
      },
      block: {
        id: 'form_1',
        blockType: 'AiForm',
        fieldRefs: ['customerName'],
        props: { createApi: 'post@/ai/crud/customer' },
      },
      fields: [{ field: 'customerName', label: '客户名称', formVisible: true }],
    })

    await wrapper.find('.ai-form-submit-stub').trigger('click')
    await flushPromises()

    expect(requestMock).not.toHaveBeenCalled()
    expect(window.$message.error).toHaveBeenCalledWith('增强校验失败')
  })

  it('keeps form data and reports an error when runtime submission fails', async () => {
    requestMock.mockRejectedValue(new Error('保存失败'))
    const wrapper = mountAiForm({
      dataSourceConfigured: true,
      readonly: true,
      runtimeInteractive: true,
      block: {
        id: 'form_1',
        blockType: 'AiForm',
        fieldRefs: ['customerName'],
        props: { createApi: 'post@/ai/crud/customer' },
      },
      fields: [{ field: 'customerName', label: '客户名称', formVisible: true }],
    })

    await wrapper.find('.ai-form-submit-stub').trigger('click')
    await flushPromises()

    expect(window.$message.error).toHaveBeenCalledWith('保存失败')
    expect(wrapper.find('.ai-form-submit-stub').exists()).toBe(true)
  })

  it('resolves nested data blocks with the child object context', () => {
    const child = {
      id: 'nested_form',
      blockType: 'AiForm',
      fieldRefs: [],
      props: { objectRef: { objectId: 'customer' } },
    }
    const blockFieldsResolver = vi.fn(block => block.id === child.id
      ? [{ field: 'customerName', label: '子区块客户名称', formVisible: true }]
      : [])
    const runtimeCrudPropsResolver = vi.fn(block => ({ configKey: block.props?.objectRef?.objectId }))
    const runtimeCrudLoadingResolver = vi.fn(() => false)
    const dataSourceConfiguredResolver = vi.fn(() => true)

    const wrapper = mount(GridBlockRenderer, {
      props: {
        block: {
          id: 'layout_1',
          blockType: 'grid-layout',
          props: {
            cells: [{ key: 'cell_1', span: 24, children: [child] }],
          },
        },
        fields: [{ field: 'parentField', label: '父区块字段' }],
        showDataSourceGuide: true,
        blockFieldsResolver,
        runtimeCrudPropsResolver,
        runtimeCrudLoadingResolver,
        dataSourceConfiguredResolver,
      },
      global: {
        plugins: [createPinia()],
        stubs: STUBS,
      },
    })

    expect(wrapper.find('.ai-form-stub span').text()).toBe('子区块客户名称')
    expect(blockFieldsResolver).toHaveBeenCalledWith(child)
    expect(runtimeCrudPropsResolver).toHaveBeenCalledWith(child)
    expect(dataSourceConfiguredResolver).toHaveBeenCalledWith(child)
  })
})
