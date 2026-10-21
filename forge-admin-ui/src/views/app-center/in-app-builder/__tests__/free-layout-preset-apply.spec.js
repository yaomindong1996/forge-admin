/**
 * 自由布局预设：应用逻辑 + 选择器确认 + 运行态接线自测
 */
import { flushPromises, mount } from '@vue/test-utils'
import { NMessageProvider } from 'naive-ui'
import { describe, expect, it, vi } from 'vitest'
import { defineComponent, h, nextTick, ref } from 'vue'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { buildFreeLayoutPresetGrid } from '../free-layout-preset-catalog'
import { shouldUsePageFlowStack } from '@/views/app-center/components/portal/portal-page-runtime-layout'
import PageTypeSelector from '@/views/app-center/components/designer/PageTypeSelector.vue'
import GridBlockRenderer from '@/components/lowcode-builder/page/GridBlockRenderer.vue'
import { createPinia } from 'pinia'

vi.mock('@/api/business-app', () => ({
  businessObjectCodeAvailable: vi.fn(async () => ({ data: true })),
  genDatasourceEnabled: vi.fn(async () => ({ data: [] })),
  genDatasourceTables: vi.fn(async () => ({ data: [] })),
  genDatasourceTableColumns: vi.fn(async () => ({ data: [] })),
}))

vi.mock('vue-router', () => ({
  useRoute: () => ({ query: {}, params: {}, path: '/', fullPath: '/', name: 'test' }),
  useRouter: () => ({ push: vi.fn(), replace: vi.fn(), resolve: vi.fn(() => ({ href: '/' })) }),
}))

vi.mock('@/router', () => ({
  default: { push: vi.fn(), resolve: vi.fn(() => ({ href: '/' })) },
}))

vi.mock('echarts', () => ({
  init: vi.fn(() => ({
    setOption: vi.fn(),
    resize: vi.fn(),
    dispose: vi.fn(),
  })),
}))

vi.mock('@/components/lowcode-builder/page/gridBlockRendererLocalComponents.js', async () => {
  const { default: QuickEntryBlock } = await import('@/components/lowcode-builder/page/QuickEntryBlock.vue')
  const { default: PageChartBlock } = await import('@/components/lowcode-builder/page/PageChartBlock.vue')
  return {
    gridBlockRendererLocalComponents: {
      IconRenderer: { props: ['icon', 'size'], template: '<span class="icon-stub">{{ icon }}</span>' },
      QuickEntryBlock,
      PageChartBlock,
      ChevronBackOutline: { render: () => null },
      CubeOutline: { render: () => null },
      QRCodeVue3: true,
      DesignerGridRenderer: {
        name: 'DesignerGridRenderer',
        props: ['cells', 'columns', 'gutter', 'rowGap', 'mode'],
        template: `
          <div class="designer-grid-stub" :data-columns="columns">
            <div v-for="cell in cells" :key="cell.key" class="designer-grid-cell-stub" :data-span="cell.span">
              <slot name="cell" :cell="cell" />
            </div>
          </div>
        `,
      },
      AiForm: true,
      AiTable: true,
      SignaturePad: true,
      FieldValueRenderer: true,
      InlineRichText: true,
      PageWidgetRenderer: true,
      ContainerContextMenu: true,
      WorkspaceSummaryMetrics: true,
      AiCrudPage: { name: 'AiCrudPage', template: '<div class="crud-stub">CRUD</div>' },
      TreeCrudTemplate: true,
      RuntimeListGridFlow: true,
    },
  }
})

/** 镜像运行态 applyFreeLayoutPreset 的核心写页逻辑，便于脱离巨型 composable 自测 */
function applyPresetToBuilder(builder, pageId, presetKey, options = {}) {
  const page = builder.pages?.[pageId]
  if (!page)
    return { ok: false, builder }
  const existingItems = page.layout?.gridLayout?.items || []
  if (existingItems.length && options.replace !== true)
    return { ok: false, builder }
  const gridLayout = buildFreeLayoutPresetGrid(presetKey, {
    pageTitle: options.pageTitle || '演示页',
  })
  if (!gridLayout)
    return { ok: false, builder }
  return {
    ok: true,
    builder: {
      ...builder,
      pages: {
        ...builder.pages,
        [pageId]: {
          ...page,
          layout: {
            ...page.layout,
            items: [],
            pageTitleComponentInitialized: true,
            appliedLayoutPreset: presetKey,
            gridLayout,
          },
        },
      },
    },
  }
}

function emptyBuilder(pageId = 'page_demo') {
  return {
    nodes: [{ id: pageId, type: 'page', title: '演示页', pageType: 'content', pageShape: 'custom' }],
    pages: {
      [pageId]: {
        layout: {
          gridLayout: {
            cols: 12,
            rowHeight: 32,
            gap: 8,
            designWidth: 1366,
            layoutType: 'custom',
            items: [],
          },
        },
      },
    },
  }
}

describe('apply free layout preset to builder page', () => {
  it('writes grid-layout items and marks appliedLayoutPreset', () => {
    const before = emptyBuilder()
    const { ok, builder } = applyPresetToBuilder(before, 'page_demo', 'ops-overview', { replace: true })
    expect(ok).toBe(true)
    const page = builder.pages.page_demo
    expect(page.layout.appliedLayoutPreset).toBe('ops-overview')
    expect(page.layout.gridLayout.items).toHaveLength(1)
    expect(page.layout.gridLayout.items[0].blockType).toBe('grid-layout')
    expect(shouldUsePageFlowStack(page.layout.gridLayout.items)).toBe(true)
  })

  it('refuses to overwrite non-empty layout unless replace=true', () => {
    const seeded = emptyBuilder()
    seeded.pages.page_demo.layout.gridLayout.items = [{ id: 'old', blockType: 'info-panel', props: {} }]
    const blocked = applyPresetToBuilder(seeded, 'page_demo', 'entry-workbench', { replace: false })
    expect(blocked.ok).toBe(false)
    expect(blocked.builder.pages.page_demo.layout.gridLayout.items[0].id).toBe('old')

    const replaced = applyPresetToBuilder(seeded, 'page_demo', 'entry-workbench', { replace: true })
    expect(replaced.ok).toBe(true)
    expect(replaced.builder.pages.page_demo.layout.appliedLayoutPreset).toBe('entry-workbench')
    expect(replaced.builder.pages.page_demo.layout.gridLayout.items[0].blockType).toBe('grid-layout')
  })

  it('applies all three presets without empty cells', () => {
    for (const key of ['ops-overview', 'entry-workbench', 'analytics-board']) {
      const { ok, builder } = applyPresetToBuilder(emptyBuilder(), 'page_demo', key, { replace: true })
      expect(ok).toBe(true)
      const cells = builder.pages.page_demo.layout.gridLayout.items[0].props.cells
      expect(cells.every(cell => (cell.children || []).length > 0)).toBe(true)
    }
  })
})

describe('PageTypeSelector layout preset confirm', () => {
  function mountSelector(defaultPageType = 'custom') {
    const Host = defineComponent({
      setup(_, { expose }) {
        const show = ref(true)
        expose({ show })
        return () => h(NMessageProvider, null, {
          default: () => h(PageTypeSelector, {
            show: show.value,
            defaultPageType,
            'onUpdate:show': value => { show.value = value },
          }),
        })
      },
    })
    return mount(Host, {
      global: {
        stubs: {
          NModal: {
            props: ['show'],
            template: '<div class="n-modal-stub"><slot /><slot name="footer" /></div>',
          },
        },
      },
    })
  }

  it('shows three grid presets when creating custom page', async () => {
    const wrapper = mountSelector('custom')
    await flushPromises()
    const html = wrapper.html()
    expect(html).toContain('布局预设')
    expect(html).toContain('运营总览')
    expect(html).toContain('入口工作台')
    expect(html).toContain('分析看板')
    expect(html).toContain('空白画布')
    wrapper.unmount()
  })

  it('confirms with layoutPresetKey for custom page', async () => {
    const wrapper = mountSelector('custom')
    await flushPromises()
    const selector = wrapper.findComponent(PageTypeSelector)
    // 直接走内部 confirm，避免依赖 Naive 表单校验
    selector.vm.form.pageName = '运营首页'
    selector.vm.form.layoutPresetKey = 'analytics-board'
    await selector.vm.confirmSelection()
    await nextTick()
    const payload = selector.emitted('confirm')?.at(-1)?.[0]
    expect(payload).toMatchObject({
      pageType: 'custom',
      pageName: '运营首页',
      layoutPresetKey: 'analytics-board',
    })
    wrapper.unmount()
  })

  it('emits empty layoutPresetKey when blank canvas selected', async () => {
    const wrapper = mountSelector('custom')
    await flushPromises()
    const selector = wrapper.findComponent(PageTypeSelector)
    selector.vm.form.pageName = '空白页'
    selector.vm.form.layoutPresetKey = 'blank'
    await selector.vm.confirmSelection()
    await nextTick()
    const payload = selector.emitted('confirm')?.at(-1)?.[0]
    expect(payload.layoutPresetKey).toBe('')
    wrapper.unmount()
  })
})

describe('GridBlockRenderer can render preset root', () => {
  it('renders ops-overview grid cells and nested quick-entry / charts', async () => {
    const layout = buildFreeLayoutPresetGrid('ops-overview')
    const root = layout.items[0]
    const Host = defineComponent({
      setup() {
        return () => h(NMessageProvider, null, {
          default: () => h(GridBlockRenderer, {
            block: root,
            fields: [],
            readonly: true,
            runtimeInteractive: true,
          }),
        })
      },
    })
    const wrapper = mount(Host, {
      global: {
        plugins: [createPinia()],
        stubs: {
          NButton: { template: '<button><slot /></button>' },
          NIcon: { template: '<i><slot /></i>' },
          NSkeleton: true,
        },
      },
    })
    await flushPromises()
    await nextTick()
    expect(wrapper.find('.designer-grid-stub').exists()).toBe(true)
    expect(wrapper.findAll('.designer-grid-cell-stub')).toHaveLength(4)
    expect(wrapper.text()).toMatch(/新建业务|数据列表|待办办理/)
    expect(wrapper.findAll('.page-chart-canvas').length).toBeGreaterThanOrEqual(1)
    wrapper.unmount()
  })
})

describe('runtime wiring source contracts', () => {
  it('wires applyFreeLayoutPreset into page creation and empty canvas', () => {
    const part1 = readFileSync(resolve('src/views/app-center/runtime-modules/useApplicationRuntime.part1.js'), 'utf8')
    const part2 = readFileSync(resolve('src/views/app-center/runtime-modules/useApplicationRuntime.part2.js'), 'utf8')
    const runtimeVue = readFileSync(resolve('src/views/app-center/application-runtime.[applicationCode].vue'), 'utf8')
    const selector = readFileSync(resolve('src/views/app-center/components/designer/PageTypeSelector.vue'), 'utf8')

    expect(part2).toContain('function applyFreeLayoutPreset')
    expect(part2).toContain('function applyCurrentFreeLayoutPreset')
    expect(part1).toContain('selection.layoutPresetKey')
    expect(part1).toContain('applyFreeLayoutPreset(result.pageId')
    expect(runtimeVue).toContain('applyCurrentFreeLayoutPreset')
    expect(runtimeVue).toContain('freeLayoutPresetOptions')
    expect(selector).toContain('layoutPresetKey')
    expect(selector).toContain('listFreeLayoutPresetOptions')
    // 追加组件时不得用默认 96 盖掉图表 createGridBlock 高度
    expect(part2).toContain('schemaHeight')
    expect(part2).toContain('Math.max(')
  })
})
