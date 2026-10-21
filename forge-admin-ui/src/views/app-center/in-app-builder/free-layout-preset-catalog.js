/**
 * 自由布局页面预设：全部基于栅格布局（grid-layout）排版，
 * 组件落在格子里，顶层通栏以便运行态走文档流堆叠，观感规整。
 */
import {
  createDefaultBlockStyle,
  createGridBlock,
} from '@/components/lowcode-builder/page/page-schema'

let presetSeq = 0
function nextId(prefix = 'blk') {
  presetSeq += 1
  return `${prefix}_${Date.now().toString(36)}_${presetSeq}`
}

function nestChild(blockType, overrides = {}, heightPx = 120) {
  const base = createGridBlock(blockType, { fields: [] }, { gridX: 0, gridY: 0 })
  if (!base)
    return null
  const overrideProps = { ...(overrides.props || {}) }
  const overrideStyle = { ...(overrideProps.style || {}) }
  delete overrideProps.style
  return {
    ...base,
    id: nextId(blockType.replace(/[^a-z0-9]/gi, '_') || 'child'),
    label: overrides.label || base.label,
    gridX: 0,
    gridY: 0,
    gridW: 12,
    gridH: Math.max(1, Number(base.gridH) || 2),
    props: {
      ...(base.props || {}),
      ...overrideProps,
      style: {
        ...createDefaultBlockStyle('full'),
        ...(base.props?.style || {}),
        ...overrideStyle,
        widthMode: 'full',
        width: '100%',
        // 图表等固定高组件禁止残留 height:100%，否则嵌套格子塌缩后画布溢出叠层
        heightMode: 'fixed',
        height: heightPx,
        minHeight: heightPx,
        x: 0,
        y: 0,
      },
    },
  }
}

function cell(key, title, span, children = [], minHeight = 0) {
  return {
    key,
    title,
    span,
    minHeight: minHeight || undefined,
    children: (Array.isArray(children) ? children : []).filter(Boolean),
  }
}

function buildRootGrid({ id, label, cells, gridH = 18 }) {
  const grid = createGridBlock('grid-layout', { fields: [] }, { gridX: 0, gridY: 0 })
  return {
    ...grid,
    id: id || nextId('grid'),
    label: label || '页面栅格',
    gridX: 0,
    gridY: 0,
    gridW: 12,
    gridH,
    props: {
      ...(grid.props || {}),
      columns: 24,
      gutter: 16,
      rowGap: 16,
      cellMinHeight: 88,
      alignItems: 'stretch',
      justifyItems: 'stretch',
      showCellBorder: false,
      cellBackground: 'transparent',
      cells,
      style: {
        ...createDefaultBlockStyle('full'),
        ...(grid.props?.style || {}),
        widthMode: 'full',
        width: '100%',
        heightMode: 'auto',
        height: 'auto',
        backgroundColor: 'transparent',
        borderColor: 'transparent',
        borderWidth: 0,
        padding: 0,
        pageFlowX: 0,
        pageFlowY: 0,
      },
    },
    children: [],
  }
}

function buildOpsOverview(options = {}) {
  const title = options.pageTitle || '运营总览'
  return buildRootGrid({
    id: nextId('preset_ops'),
    label: `${title} · 栅格`,
    gridH: 16,
    cells: [
      cell('metrics', '指标', 24, [
        nestChild('stats-strip', {
          label: '核心指标',
          props: {
            columns: 4,
            showIcon: true,
            showTrend: true,
            metrics: [
              { label: '今日访问', value: '1,286', trend: '+8%', icon: 'EyeOutline', tone: 'primary', linkType: 'none' },
              { label: '待办事项', value: '24', trend: '+3', icon: 'CheckboxOutline', tone: 'warning', linkType: 'none' },
              { label: '本周完成', value: '168', trend: '+12%', icon: 'CheckmarkDoneOutline', tone: 'success', linkType: 'none' },
              { label: '异常告警', value: '2', trend: '-1', icon: 'AlertCircleOutline', tone: 'danger', linkType: 'none' },
            ],
          },
        }, 96),
      ], 96),
      cell('entries', '快捷入口', 24, [
        nestChild('quick-entry', {
          label: '快捷入口',
          props: {
            columns: 3,
            variant: 'card',
            showDescription: true,
            items: [
              { key: 'e1', title: '新建业务', description: '填写并提交', icon: 'AddOutline', tone: 'primary', linkType: 'none', targetPageId: '' },
              { key: 'e2', title: '数据列表', description: '查询与导出', icon: 'ListOutline', tone: 'info', linkType: 'none', targetPageId: '' },
              { key: 'e3', title: '待办办理', description: '处理审批', icon: 'CheckmarkCircleOutline', tone: 'success', linkType: 'none', targetPageId: '' },
            ],
          },
        }, 120),
      ], 120),
      cell('bar', '柱状图', 12, [
        nestChild('bar-chart', {
          label: '分类对比',
          props: {
            title: '业务分类对比',
            height: 260,
            showLegend: false,
            categories: ['类型A', '类型B', '类型C', '类型D'],
            series: [{ name: '数量', data: [18, 12, 9, 6] }],
          },
        }, 280),
      ], 280),
      cell('line', '折线图', 12, [
        nestChild('line-chart', {
          label: '趋势',
          props: {
            title: '近六月趋势',
            height: 260,
            showLegend: true,
            smooth: true,
            showArea: true,
            categories: ['1月', '2月', '3月', '4月', '5月', '6月'],
            series: [
              { name: '申请', data: [8, 12, 10, 16, 14, 18] },
              { name: '完成', data: [6, 9, 8, 12, 11, 15] },
            ],
          },
        }, 280),
      ], 280),
    ],
  })
}

function buildEntryWorkbench(options = {}) {
  const title = options.pageTitle || '业务工作台'
  return buildRootGrid({
    id: nextId('preset_entry'),
    label: `${title} · 栅格`,
    gridH: 22,
    cells: [
      cell('title', '标题', 24, [
        nestChild('page-title', {
          label: '页面标题',
          props: {
            title,
            subtitle: '从下方入口进入常用功能，列表可对接业务对象',
            content: `<h1>${title}</h1><p>从下方入口进入常用功能，列表可对接业务对象</p>`,
          },
        }, 72),
      ], 72),
      cell('tip', '提示', 24, [
        nestChild('info-panel', {
          label: '使用提示',
          props: {
            type: 'info',
            title: '使用提示',
            content: '请配置快捷入口的目标页，并为下方列表选择业务对象与数据源。',
            showIcon: true,
            linkType: 'none',
          },
        }, 88),
      ], 88),
      cell('entries', '快捷入口', 24, [
        nestChild('quick-entry', {
          label: '快捷入口',
          props: {
            columns: 4,
            variant: 'card',
            showDescription: true,
            items: [
              { key: 'e1', title: '发起申请', description: '打开表单页', icon: 'CreateOutline', tone: 'primary', linkType: 'none', targetPageId: '' },
              { key: 'e2', title: '我的单据', description: '查看进度', icon: 'DocumentTextOutline', tone: 'info', linkType: 'none', targetPageId: '' },
              { key: 'e3', title: '审批办理', description: '处理待办', icon: 'CheckmarkCircleOutline', tone: 'success', linkType: 'none', targetPageId: '' },
              { key: 'e4', title: '数据统计', description: '查看看板', icon: 'StatsChartOutline', tone: 'warning', linkType: 'none', targetPageId: '' },
            ],
          },
        }, 120),
      ], 120),
      cell('list', '数据列表', 24, [
        nestChild('AiCrudPage', {
          label: '数据列表',
          props: {
            title: '业务列表',
            formOnly: false,
            showSearch: true,
            hideToolbar: false,
          },
        }, 480),
      ], 480),
    ],
  })
}

function buildAnalyticsBoard(options = {}) {
  const title = options.pageTitle || '分析看板'
  return buildRootGrid({
    id: nextId('preset_analytics'),
    label: `${title} · 栅格`,
    gridH: 18,
    cells: [
      cell('metrics', '指标', 24, [
        nestChild('stats-strip', {
          label: '核心指标',
          props: {
            columns: 4,
            showIcon: true,
            showTrend: true,
            metrics: [
              { label: '总量', value: '8,420', trend: '+6%', icon: 'LayersOutline', tone: 'primary', linkType: 'none' },
              { label: '环比', value: '12.4%', trend: '+1.2%', icon: 'TrendingUpOutline', tone: 'info', linkType: 'none' },
              { label: '转化率', value: '36%', trend: '+2%', icon: 'PieChartOutline', tone: 'success', linkType: 'none' },
              { label: '异常', value: '5', trend: '-2', icon: 'AlertCircleOutline', tone: 'danger', linkType: 'none' },
            ],
          },
        }, 96),
      ], 96),
      cell('trend', '趋势', 24, [
        nestChild('line-chart', {
          label: '趋势图',
          props: {
            title: '核心指标趋势',
            height: 280,
            showLegend: true,
            smooth: true,
            showArea: true,
            categories: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
            series: [
              { name: '访问', data: [120, 132, 101, 134, 90, 230, 210] },
              { name: '转化', data: [45, 52, 48, 60, 40, 88, 76] },
            ],
          },
        }, 300),
      ], 300),
      cell('bar', '分布', 16, [
        nestChild('bar-chart', {
          label: '分布图',
          props: {
            title: '渠道分布',
            height: 260,
            showLegend: false,
            categories: ['渠道A', '渠道B', '渠道C', '渠道D', '渠道E'],
            series: [{ name: '数量', data: [42, 35, 28, 18, 12] }],
          },
        }, 280),
      ], 280),
      cell('note', '说明', 8, [
        nestChild('info-panel', {
          label: '说明',
          props: {
            type: 'success',
            title: '数据说明',
            content: '图表默认使用示例数据。在属性面板开启「数据来源」后，可绑定列表字段或远程接口。',
            showIcon: true,
            linkType: 'none',
          },
        }, 160),
        nestChild('empty-state', {
          label: '空状态',
          props: {
            title: '可替换为说明',
            description: '无真实数据时可保留此占位，或换成自定义 HTML',
            actionText: '',
          },
        }, 140),
      ], 320),
    ],
  })
}

export const freeLayoutPresetCatalog = [
  {
    key: 'ops-overview',
    label: '运营总览',
    description: '指标 + 快捷入口 + 双图并排，适合首页看板',
    thumb: 'ops',
    build: buildOpsOverview,
  },
  {
    key: 'entry-workbench',
    label: '入口工作台',
    description: '标题提示 + 快捷入口 + 数据列表，适合业务入口',
    thumb: 'entry',
    build: buildEntryWorkbench,
  },
  {
    key: 'analytics-board',
    label: '分析看板',
    description: '指标 + 通栏趋势 + 分布与说明，适合看数页',
    thumb: 'analytics',
    build: buildAnalyticsBoard,
  },
]

export function resolveFreeLayoutPreset(key = '') {
  const normalized = String(key || '').trim()
  if (!normalized || normalized === 'blank')
    return null
  return freeLayoutPresetCatalog.find(item => item.key === normalized) || null
}

/**
 * 生成完整 gridLayout（cols/rowHeight/items）
 */
export function buildFreeLayoutPresetGrid(presetKey, options = {}) {
  const preset = resolveFreeLayoutPreset(presetKey)
  if (!preset)
    return null
  const root = preset.build(options)
  return {
    cols: 12,
    rowHeight: 32,
    gap: 8,
    designWidth: 1366,
    layoutType: 'custom',
    items: [root],
    appliedLayoutPreset: preset.key,
  }
}

export function listFreeLayoutPresetOptions() {
  return freeLayoutPresetCatalog.map(item => ({
    key: item.key,
    label: item.label,
    description: item.description,
    thumb: item.thumb,
  }))
}
