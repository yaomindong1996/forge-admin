/**
 * 快捷入口 / 柱状图 / 折线图：组件渲染 + 属性面板回填生效自测
 */
import { flushPromises, mount } from '@vue/test-utils'
import { NMessageProvider } from 'naive-ui'
import { createPinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, h, nextTick } from 'vue'
import { createGridBlock } from '../page-schema'
import QuickEntryBlock from '../QuickEntryBlock.vue'
import PageChartBlock from '../PageChartBlock.vue'
import GridBlockRenderer from '../GridBlockRenderer.vue'
import ListPageGridDesigner from '../ListPageGridDesigner.vue'
import { buildPageChartOption } from '../page-chart-option'

vi.mock('@/router', () => ({
  default: { currentRoute: { value: { query: {} } }, push: vi.fn(), resolve: vi.fn(() => ({ href: '/' })) },
}))

vi.mock('vue-router', () => ({
  useRoute: () => ({ query: {}, params: {}, path: '/', fullPath: '/', name: 'test' }),
  useRouter: () => ({ push: vi.fn(), replace: vi.fn(), resolve: vi.fn(() => ({ href: '/' })) }),
}))

vi.mock('../gridBlockRendererLocalComponents.js', async () => {
  const { default: QuickEntryBlockSync } = await import('../QuickEntryBlock.vue')
  const { default: PageChartBlockSync } = await import('../PageChartBlock.vue')
  const { default: StatsStripBlockSync } = await import('../StatsStripBlock.vue')
  const { default: InfoPanelBlockSync } = await import('../InfoPanelBlock.vue')
  const IconRenderer = { name: 'IconRenderer', props: ['icon', 'size'], template: '<span class="icon-stub">{{ icon }}</span>' }
  return {
    gridBlockRendererLocalComponents: {
      IconRenderer,
      QuickEntryBlock: QuickEntryBlockSync,
      PageChartBlock: PageChartBlockSync,
      StatsStripBlock: StatsStripBlockSync,
      InfoPanelBlock: InfoPanelBlockSync,
      ChevronBackOutline: { render: () => null },
      CubeOutline: { render: () => null },
      QRCodeVue3: true,
      DesignerGridRenderer: true,
      AiForm: true,
      AiTable: true,
      SignaturePad: true,
      FieldValueRenderer: true,
      InlineRichText: true,
      PageWidgetRenderer: true,
      ContainerContextMenu: true,
      WorkspaceSummaryMetrics: true,
      WorkflowTaskListBlock: true,
      NoticeBoardBlock: true,
      AiCrudPage: true,
      TreeCrudTemplate: true,
      RuntimeListGridFlow: true,
    },
  }
})

const chartInstances = vi.hoisted(() => [])
const echartsMocks = vi.hoisted(() => {
  const setOption = vi.fn()
  const resize = vi.fn()
  const dispose = vi.fn()
  const init = vi.fn(() => {
    const instance = { setOption, resize, dispose }
    chartInstances.push(instance)
    return instance
  })
  return { init, setOption, resize, dispose }
})

vi.mock('echarts', () => ({
  init: echartsMocks.init,
}))

const rendererStubs = {
  IconRenderer: { props: ['icon', 'size'], template: '<span class="icon-stub">{{ icon }}</span>' },
  NButton: { template: '<button><slot /></button>' },
  NIcon: { template: '<i><slot /></i>' },
  NSkeleton: true,
  NFormItem: true,
  NInput: true,
  NSelect: true,
  NSwitch: true,
  NInputNumber: true,
  NDataTable: true,
  NDatePicker: true,
  NTreeSelect: true,
  NTabs: true,
  NTabPane: true,
  NSteps: true,
  NStep: true,
  NTag: true,
  NTransfer: true,
  NTree: true,
  NWatermark: true,
  NAvatar: true,
  NDropdown: true,
  NSpin: true,
  AiCrudPage: true,
  AiForm: true,
  AiTable: true,
  WorkspaceSummaryMetrics: true,
}

function mountRenderer(block, extra = {}) {
  const Host = defineComponent({
    name: 'MessageHost',
    setup() {
      return () => h(NMessageProvider, null, {
        default: () => h(GridBlockRenderer, {
          block,
          fields: [],
          readonly: true,
          runtimeInteractive: true,
          ...extra,
        }),
      })
    },
  })
  return mount(Host, {
    global: {
      plugins: [createPinia()],
      stubs: rendererStubs,
    },
  })
}

function createDemoLayout() {
  return {
    cols: 12,
    rowHeight: 32,
    gap: 8,
    designWidth: 1366,
    layoutType: 'custom',
    items: [
      {
        id: 'blk_quick',
        blockType: 'quick-entry',
        label: '快捷入口',
        gridX: 0,
        gridY: 0,
        gridW: 12,
        gridH: 3,
        props: {
          columns: 3,
          items: [
            {
              key: 'go_apply',
              title: '发起请假',
              description: '填写申请',
              icon: 'CalendarOutline',
              tone: 'primary',
              targetPageId: 'page_leave_apply',
            },
            {
              key: 'go_list',
              title: '请假记录',
              description: '查询导出',
              icon: 'ListOutline',
              tone: 'info',
              targetPageId: 'page_leave_list',
            },
          ],
        },
      },
      {
        id: 'blk_bar',
        blockType: 'bar-chart',
        label: '柱状图',
        gridX: 0,
        gridY: 3,
        gridW: 6,
        gridH: 8,
        props: {
          title: '请假类型分布',
          height: 280,
          showLegend: false,
          categories: ['年假', '事假', '病假'],
          series: [{ name: '申请数', data: [14, 9, 6] }],
        },
      },
      {
        id: 'blk_line',
        blockType: 'line-chart',
        label: '折线图',
        gridX: 6,
        gridY: 3,
        gridW: 6,
        gridH: 8,
        props: {
          title: '近六月趋势',
          height: 280,
          showLegend: true,
          smooth: true,
          showArea: true,
          categories: ['1月', '2月', '3月'],
          series: [
            { name: '申请', data: [8, 12, 10] },
            { name: '通过', data: [6, 9, 8] },
          ],
        },
      },
    ],
  }
}

describe('QuickEntryBlock', () => {
  it('renders items/columns/tone and emits select with targetPageId', async () => {
    const wrapper = mount(QuickEntryBlock, {
      props: {
        columns: 2,
        interactive: true,
        items: [
          {
            key: 'a',
            title: '发起请假',
            description: '填写申请',
            icon: 'CalendarOutline',
            tone: 'success',
            targetPageId: 'page_leave_apply',
          },
        ],
      },
      global: {
        stubs: {
          IconRenderer: { props: ['icon'], template: '<span class="icon-stub">{{ icon }}</span>' },
        },
      },
    })

    expect(wrapper.find('.quick-entry-grid').attributes('style')).toContain('repeat(2')
    expect(wrapper.text()).toContain('发起请假')
    expect(wrapper.text()).toContain('填写申请')
    expect(wrapper.find('.icon-stub').text()).toBe('CalendarOutline')
    expect(wrapper.find('.quick-entry-card').attributes('style')).toContain('--fl-tone-color')
    expect(wrapper.find('.quick-entry-card').classes()).toContain('is-interactive')

    await wrapper.find('.quick-entry-card').trigger('click')
    expect(wrapper.emitted('select')?.[0]?.[0]).toMatchObject({
      title: '发起请假',
      targetPageId: 'page_leave_apply',
      linkType: 'page',
    })
    wrapper.unmount()
  })

  it('shows empty hint when items is empty', () => {
    const wrapper = mount(QuickEntryBlock, {
      props: { items: [], interactive: false },
      global: { stubs: { IconRenderer: true } },
    })
    expect(wrapper.text()).toContain('点击右侧添加快捷入口')
    wrapper.unmount()
  })
})

describe('PageChartBlock', () => {
  beforeEach(() => {
    chartInstances.length = 0
    echartsMocks.init.mockClear()
    echartsMocks.setOption.mockClear()
    echartsMocks.resize.mockClear()
    echartsMocks.dispose.mockClear()
  })

  it('applies title/categories/series/height to echarts option', async () => {
    const wrapper = mount(PageChartBlock, {
      props: {
        chartType: 'bar',
        title: '请假类型分布',
        height: 300,
        showLegend: false,
        categories: ['年假', '事假'],
        series: [{ name: '申请数', data: [14, 9] }],
      },
    })
    await flushPromises()
    await nextTick()

    expect(echartsMocks.init).toHaveBeenCalled()
    expect(echartsMocks.setOption).toHaveBeenCalled()
    const option = echartsMocks.setOption.mock.calls.at(-1)?.[0]
    expect(option.title.text).toBe('请假类型分布')
    expect(option.xAxis.data).toEqual(['年假', '事假'])
    expect(option.series[0]).toMatchObject({ type: 'bar', name: '申请数', data: [14, 9] })
    expect(wrapper.find('.page-chart-canvas').attributes('style')).toContain('height: 100%')
    expect(wrapper.find('.page-chart-canvas').attributes('style')).toContain('min-height: 0')
    expect(wrapper.find('.page-chart-empty').exists()).toBe(false)
    wrapper.unmount()
  })

  it('applies line smooth/area props and shows empty state without data', async () => {
    const empty = mount(PageChartBlock, {
      props: { chartType: 'line', categories: [], series: [] },
    })
    await flushPromises()
    expect(empty.text()).toContain('暂无图表数据')
    empty.unmount()

    const wrapper = mount(PageChartBlock, {
      props: {
        chartType: 'line',
        title: '趋势',
        categories: ['1月', '2月'],
        series: [{ name: '申请', data: [1, 2] }],
        smooth: true,
        showArea: true,
        showLegend: true,
      },
    })
    await flushPromises()
    const option = echartsMocks.setOption.mock.calls.at(-1)?.[0]
    expect(option.series[0].type).toBe('line')
    expect(option.series[0].smooth).toBe(true)
    expect(option.series[0].areaStyle.opacity).toBe(0.12)
    wrapper.unmount()
  })

  it('rebuilds option when props change', async () => {
    const wrapper = mount(PageChartBlock, {
      props: {
        chartType: 'bar',
        title: '旧标题',
        categories: ['A'],
        series: [{ name: 'S', data: [1] }],
      },
    })
    await flushPromises()
    await wrapper.setProps({ title: '新标题', categories: ['A', 'B'], series: [{ name: 'S', data: [1, 2] }] })
    await flushPromises()
    const option = echartsMocks.setOption.mock.calls.at(-1)?.[0]
    expect(option.title.text).toBe('新标题')
    expect(option.xAxis.data).toEqual(['A', 'B'])
    wrapper.unmount()
  })
})

describe('createGridBlock defaults for new blocks', () => {
  it('creates quick-entry / bar-chart / line-chart with usable default props', () => {
    const entry = createGridBlock('quick-entry', { fields: [] }, { gridX: 0, gridY: 0 })
    expect(entry.blockType).toBe('quick-entry')
    expect(entry.props.columns).toBe(3)
    expect(entry.props.items.length).toBeGreaterThan(0)
    expect(entry.props.items[0]).toHaveProperty('targetPageId')
    expect(entry.props.dataBinding?.renderMode).toBe('manual')

    const bar = createGridBlock('bar-chart', { fields: [] }, { gridX: 0, gridY: 0 })
    expect(bar.props.title).toContain('柱状')
    expect(bar.props.categories.length).toBeGreaterThan(0)
    expect(bar.props.series[0].data.length).toBeGreaterThan(0)

    const line = createGridBlock('line-chart', { fields: [] }, { gridX: 0, gridY: 0 })
    expect(line.props.title).toContain('折线')
    expect(line.props.smooth).toBe(true)
    expect(line.props.series.length).toBeGreaterThan(0)
  })
})

describe('GridBlockRenderer wires props into child blocks', () => {
  beforeEach(() => {
    chartInstances.length = 0
    echartsMocks.init.mockClear()
    echartsMocks.setOption.mockClear()
  })

  it('passes quick-entry items and chart series from block props', async () => {
    const quick = mountRenderer({
      id: 'q1',
      blockType: 'quick-entry',
      props: {
        columns: 4,
        items: [
          { key: '1', title: '入口A', description: 'desc', icon: 'AddOutline', tone: 'warning', targetPageId: 'page_a' },
        ],
      },
    })
    await flushPromises()
    expect(quick.text()).toContain('入口A')
    expect(quick.text()).toContain('desc')
    expect(quick.find('.icon-stub').text()).toBe('AddOutline')
    expect(quick.find('.quick-entry-card').attributes('style')).toContain('--fl-tone-color')
    expect(quick.find('.quick-entry-card').classes()).toContain('is-interactive')
    expect(quick.find('.quick-entry-grid').attributes('style')).toContain('repeat(4')
    quick.unmount()

    const bar = mountRenderer({
      id: 'b1',
      blockType: 'bar-chart',
      props: {
        title: '分类对比',
        height: 240,
        showLegend: false,
        categories: ['X', 'Y'],
        series: [{ name: '数量', data: [3, 5] }],
      },
    })
    await flushPromises()
    await nextTick()
    expect(echartsMocks.setOption).toHaveBeenCalled()
    const barOption = echartsMocks.setOption.mock.calls.at(-1)?.[0]
    expect(barOption.title.text).toBe('分类对比')
    expect(barOption.xAxis.data).toEqual(['X', 'Y'])
    expect(barOption.series[0]).toMatchObject({ type: 'bar', name: '数量', data: [3, 5] })
    expect(bar.find('.page-chart-canvas').attributes('style')).toContain('height: 100%')
    expect(bar.find('.page-chart-canvas').attributes('style')).toContain('min-height: 0')
    bar.unmount()

    echartsMocks.setOption.mockClear()
    const line = mountRenderer({
      id: 'l1',
      blockType: 'line-chart',
      props: {
        title: '趋势',
        height: 260,
        smooth: false,
        showArea: true,
        categories: ['1月'],
        series: [{ name: '值', data: [7] }],
      },
    })
    await flushPromises()
    await nextTick()
    const lineOption = echartsMocks.setOption.mock.calls.at(-1)?.[0]
    expect(lineOption.title.text).toBe('趋势')
    expect(lineOption.series[0].type).toBe('line')
    expect(lineOption.series[0].smooth).toBe(false)
    expect(lineOption.series[0].areaStyle.opacity).toBe(0.12)
    line.unmount()
  })
})

describe('property panel for quick-entry / charts', () => {
  it('shows quick-entry fields when selected', async () => {
    const wrapper = mount(ListPageGridDesigner, {
      props: {
        modelValue: createDemoLayout(),
        modelSchema: {},
        fields: [],
        panelOnly: true,
        activeBlockId: 'blk_quick',
      },
      global: {
        plugins: [createPinia()],
        stubs: { FieldConfigDrawer: true, WidgetDataBindingEditor: true },
      },
    })
    await nextTick()
    expect(wrapper.find('.prop-title').text()).toMatch(/快捷入口/)
    const html = wrapper.find('.property-body').html()
    expect(html).toContain('列数')
    expect(html).toContain('入口项')
    expect(html).toContain('发起请假')
    expect(html).toContain('page_leave_apply')
    expect(html).toContain('CalendarOutline')
    wrapper.unmount()
  })

  it('shows bar-chart fields when selected', async () => {
    const wrapper = mount(ListPageGridDesigner, {
      props: {
        modelValue: createDemoLayout(),
        modelSchema: {},
        fields: [],
        panelOnly: true,
        activeBlockId: 'blk_bar',
      },
      global: {
        plugins: [createPinia()],
        stubs: { FieldConfigDrawer: true, WidgetDataBindingEditor: true },
      },
    })
    await nextTick()
    expect(wrapper.find('.prop-title').text()).toMatch(/柱状图/)
    const html = wrapper.find('.property-body').html()
    expect(html).toContain('标题')
    expect(html).toContain('分类轴')
    expect(html).toContain('系列数据')
    expect(html).toContain('请假类型分布')
    expect(html).toContain('年假')
    expect(html).toContain('申请数')
    wrapper.unmount()
  })

  it('shows line-chart specific smooth/area fields', async () => {
    const wrapper = mount(ListPageGridDesigner, {
      props: {
        modelValue: createDemoLayout(),
        modelSchema: {},
        fields: [],
        panelOnly: true,
        activeBlockId: 'blk_line',
      },
      global: {
        plugins: [createPinia()],
        stubs: { FieldConfigDrawer: true, WidgetDataBindingEditor: true },
      },
    })
    await nextTick()
    expect(wrapper.find('.prop-title').text()).toMatch(/折线图/)
    const html = wrapper.find('.property-body').html()
    expect(html).toContain('平滑曲线')
    expect(html).toContain('面积填充')
    expect(html).toContain('近六月趋势')
    expect(html).toContain('申请')
    wrapper.unmount()
  })

  it('writes quick-entry and chart prop edits back to layout', async () => {
    const layout = createDemoLayout()
    const wrapper = mount(ListPageGridDesigner, {
      props: {
        modelValue: layout,
        modelSchema: {},
        fields: [],
        panelOnly: true,
        activeBlockId: 'blk_quick',
      },
      global: {
        plugins: [createPinia()],
        stubs: { FieldConfigDrawer: true, WidgetDataBindingEditor: true },
      },
    })
    await nextTick()

    expect(wrapper.find('.property-body').html()).toContain('入口项')
    expect(wrapper.find('.property-body').html()).toContain('添加')
    expect(typeof wrapper.vm.addQuickEntryItem).toBe('function')
    wrapper.vm.addQuickEntryItem()
    await nextTick()

    let emitted = wrapper.emitted('update:modelValue')?.at(-1)?.[0]
    const quick = emitted.items.find(item => item.id === 'blk_quick')
    expect(quick.props.items.length).toBe(3)
    expect(quick.props.items.at(-1).title).toBe('新入口')

    await wrapper.setProps({ activeBlockId: 'blk_bar', modelValue: emitted })
    await nextTick()
    expect(wrapper.find('.property-body').html()).toContain('添加系列')
    expect(typeof wrapper.vm.addChartSeriesItem).toBe('function')
    wrapper.vm.addChartSeriesItem()
    await nextTick()

    emitted = wrapper.emitted('update:modelValue')?.at(-1)?.[0]
    const bar = emitted.items.find(item => item.id === 'blk_bar')
    expect(bar.props.series.length).toBe(2)
    expect(bar.props.series.at(-1).name).toMatch(/系列/)

    wrapper.vm.updateChartCategories('年假,事假,调休')
    await nextTick()
    emitted = wrapper.emitted('update:modelValue')?.at(-1)?.[0]
    expect(emitted.items.find(item => item.id === 'blk_bar').props.categories).toEqual(['年假', '事假', '调休'])

    wrapper.vm.updateQuickEntryItem(0, { title: '改名入口', targetPageId: 'page_x' })
    await wrapper.setProps({ activeBlockId: 'blk_quick' })
    await nextTick()
    // 切回快捷入口后再改：确保 updateQuickEntryItem 对当前选中块生效
    wrapper.vm.updateQuickEntryItem(0, { title: '改名入口', targetPageId: 'page_x' })
    await nextTick()
    emitted = wrapper.emitted('update:modelValue')?.at(-1)?.[0]
    expect(emitted.items.find(item => item.id === 'blk_quick').props.items[0]).toMatchObject({
      title: '改名入口',
      targetPageId: 'page_x',
    })
    wrapper.unmount()
  })
})

describe('chart option helper used by renderer', () => {
  it('keeps manual props coherent with PageChartBlock expectations', () => {
    const option = buildPageChartOption('bar', {
      title: '请假类型分布',
      categories: ['年假', '事假', '病假'],
      series: [{ name: '申请数', data: [14, 9, 6] }],
      showLegend: false,
    })
    expect(option.series[0].type).toBe('bar')
    expect(option.xAxis.data).toHaveLength(3)
    expect(option.legend).toBeUndefined()
  })
})

describe('list-binding chart data through GridBlockRenderer', () => {
  beforeEach(() => {
    chartInstances.length = 0
    echartsMocks.init.mockClear()
    echartsMocks.setOption.mockClear()
  })

  it('aggregates bound rows into chart series when renderMode=list', async () => {
    const wrapper = mountRenderer({
      id: 'bound_bar',
      blockType: 'bar-chart',
      props: {
        title: '绑定数据',
        height: 220,
        categories: ['占位'],
        series: [{ name: '占位', data: [0] }],
        dataBinding: {
          enabled: true,
          sourceType: 'context',
          renderMode: 'list',
          contextPath: 'stats',
          labelField: 'name',
          valueField: 'count',
          metaField: 'group',
        },
      },
    }, {
      runtimeRecord: {
        stats: [
          { name: '年假', count: 14, group: '申请' },
          { name: '事假', count: 9, group: '申请' },
          { name: '年假', count: 10, group: '通过' },
          { name: '事假', count: 5, group: '通过' },
        ],
      },
    })
    await flushPromises()
    await nextTick()
    const option = echartsMocks.setOption.mock.calls.at(-1)?.[0]
    expect(option.title.text).toBe('绑定数据')
    expect(option.xAxis.data).toEqual(['年假', '事假'])
    expect(option.series).toEqual(expect.arrayContaining([
      expect.objectContaining({ name: '申请', data: [14, 9], type: 'bar' }),
      expect.objectContaining({ name: '通过', data: [10, 5], type: 'bar' }),
    ]))
    wrapper.unmount()
  })
})
