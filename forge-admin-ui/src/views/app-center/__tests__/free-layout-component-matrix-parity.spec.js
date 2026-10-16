/**
 * 自由布局：目录内各组件「属性 / 样式 / 数据」编辑→预览→运行贯通矩阵。
 * 用 createGridBlock 生成标准块，再经 portal resolve + chrome strip + shell 几何校验。
 */
import { describe, expect, it } from 'vitest'
import { stripAccidentalSelectionChrome, isAccidentalSelectionChrome } from '@/components/lowcode-builder/page/block-style-chrome'
import {
  createGridBlock,
  listPageBlockCatalog,
} from '@/components/lowcode-builder/page/page-schema'
import { pageWidgetComponentKeys } from '@/components/lowcode-builder/shared/page-widget-schema'
import {
  resolvePortalPageBlocks,
  shouldUseContentSizedFlow,
  shouldUsePageFlowStack,
} from '@/views/app-center/components/portal/portal-page-runtime-layout'
import { resolvePageBlockShellStyle } from '@/views/app-center/runtime-modules/page-flow-geometry'

const SAMPLE_MODEL = {
  fields: [
    { field: 'id', label: 'ID', listVisible: true, queryable: true },
    { field: 'name', label: '名称', listVisible: true, queryable: true },
    { field: 'status', label: '状态', listVisible: true, queryable: true },
  ],
}

/** 自由布局里常见、需重点保证三态一致的组件（含数据块 / 装饰 / 容器 / 小部件） */
const PRIORITY_BLOCK_TYPES = [
  'AiCrudPage',
  'AiForm',
  'AiTable',
  'page-title',
  'custom-html',
  'stats-strip',
  'info-panel',
  'empty-state',
  'action-button',
  'button-group',
  'tag-list',
  'steps',
  'timeline',
  'card',
  'tabs',
  'grid-layout',
  'box-layout',
  'divider',
  'spacer',
  'section-divider',
  'link',
  'markdown',
  'rich-text',
  'announcement',
  'iframe',
  'qrcode',
  'barcode',
]

function authoredVisual(style = {}) {
  const {
    pageFlowX: _x,
    pageFlowY: _y,
    pageFlowWidth: _w,
    pageFlowHeight: _h,
    widthMode: _wm,
    heightMode: _hm,
    ...visual
  } = style
  return visual
}

function stampDesignerStyle(block, index = 0) {
  const style = {
    ...(block.props?.style || {}),
    widthMode: block.props?.style?.widthMode === 'full' && index % 2 === 1 ? 'fixed' : (block.props?.style?.widthMode || 'fixed'),
    heightMode: block.props?.style?.heightMode || 'fixed',
    pageFlowX: 24 + (index % 3) * 40,
    pageFlowY: 40 + index * 80,
    pageFlowWidth: '420px',
    pageFlowHeight: Math.max(80, Number(block.gridH || 2) * 40),
    backgroundColor: index % 2 === 0 ? '#f8fafc' : 'transparent',
    borderColor: '#cbd5e1',
    borderWidth: 1,
    borderStyle: 'solid',
    borderRadius: 6,
    padding: 8,
    margin: 0,
    boxShadow: 'none',
    fontSize: 14,
    color: '#0f172a',
    opacity: 1,
  }
  return {
    ...block,
    props: {
      ...(block.props || {}),
      style,
    },
  }
}

describe('free-layout component matrix parity', () => {
  it('priority block types exist in catalog or widget keys', () => {
    const catalog = new Set(listPageBlockCatalog.map(item => item.blockType))
    const widgets = new Set(pageWidgetComponentKeys)
    const missing = PRIORITY_BLOCK_TYPES.filter(type => !catalog.has(type) && !widgets.has(type))
    expect(missing).toEqual([])
  })

  it('createGridBlock yields style + props for every priority type', () => {
    for (const blockType of PRIORITY_BLOCK_TYPES) {
      const block = createGridBlock(blockType, SAMPLE_MODEL, { gridX: 0, gridY: 0 })
      expect(block, blockType).toBeTruthy()
      expect(block.blockType).toBe(blockType)
      expect(block.props?.style).toBeTruthy()
      expect(block.props.style).toHaveProperty('widthMode')
      expect(block.props.style).toHaveProperty('borderColor')
      expect(Array.isArray(block.props.events)).toBe(true)
    }
  })

  it('portal resolve keeps stamped props/style for mixed free-layout page', () => {
    const items = PRIORITY_BLOCK_TYPES
      .map((blockType, index) => stampDesignerStyle(
        createGridBlock(blockType, SAMPLE_MODEL, { gridX: 0, gridY: index }),
        index,
      ))
      .filter(Boolean)

    // 掺入固定宽块，确保整页仍是自由布局绝对定位
    expect(shouldUsePageFlowStack(items)).toBe(false)
    expect(shouldUseContentSizedFlow(items, { fillHost: true, runtimePreview: true })).toBe(false)

    const page = {
      id: 'page_matrix',
      layout: {
        gridLayout: {
          pageFlowCoordSpace: 'content',
          pagePadding: { top: 8, right: 8, bottom: 8, left: 8 },
          items,
        },
      },
    }
    const resolved = resolvePortalPageBlocks({
      page,
      node: { id: 'page_matrix', pageShape: 'custom' },
    })
    expect(resolved.length).toBe(items.length)

    for (const source of items) {
      const hit = resolved.find(item => item.id === source.id)
      expect(hit, source.blockType).toBeTruthy()
      // 属性：非 style 的业务 props 不得被 portal 抹掉
      const { style: _s, ...sourceProps } = source.props || {}
      const { style: _t, ...hitProps } = hit.props || {}
      expect(hitProps).toMatchObject(sourceProps)

      // 样式：作者边框/背景经只读剥离后仍保留（非选中态 chrome）
      const visual = authoredVisual(hit.props.style || {})
      expect(isAccidentalSelectionChrome(visual)).toBe(false)
      expect(stripAccidentalSelectionChrome(visual)).toMatchObject({
        backgroundColor: visual.backgroundColor,
        borderColor: visual.borderColor,
        borderWidth: visual.borderWidth,
        borderStyle: visual.borderStyle,
        borderRadius: visual.borderRadius,
        padding: visual.padding,
        fontSize: visual.fontSize,
        color: visual.color,
      })

      // 壳几何：编辑与预览同源 resolvePageBlockShellStyle
      const shell = resolvePageBlockShellStyle(hit, resolved, {
        pagePadding: { top: 8, right: 8, bottom: 8, left: 8 },
        pageFlowCoordSpace: 'content',
      })
      expect(shell.position).toBe('absolute')
      expect(shell.left).toMatch(/px$/)
      expect(shell.top).toMatch(/px$/)
    }
  })

  it('data-bearing blocks keep fieldRefs / metrics / events across portal', () => {
    const crud = stampDesignerStyle(createGridBlock('AiCrudPage', SAMPLE_MODEL), 0)
    crud.props.searchFieldRefs = ['name', 'status']
    crud.fieldRefs = ['name', 'status', 'id']
    crud.props.formOpenMode = 'drawer'

    const stats = stampDesignerStyle(createGridBlock('stats-strip', SAMPLE_MODEL), 1)
    stats.props.metrics = [
      { label: '待办', value: '3', trend: '+1' },
      { label: '已办', value: '12' },
    ]

    const button = stampDesignerStyle(createGridBlock('action-button', SAMPLE_MODEL), 2)
    button.props.text = '去详情'
    button.props.events = [{
      id: 'evt_1',
      trigger: 'click',
      action: 'navigate',
      targetPageId: 'page_detail',
    }]

    const info = stampDesignerStyle(createGridBlock('info-panel', SAMPLE_MODEL), 3)
    info.props.title = '验收提示'
    info.props.content = '自定义内容，不能被当占位过滤'

    const page = {
      id: 'page_data',
      layout: { gridLayout: { items: [crud, stats, button, info] } },
    }
    const resolved = resolvePortalPageBlocks({
      page,
      node: { id: 'page_data', pageShape: 'custom' },
    })
    expect(resolved.find(item => item.id === crud.id).fieldRefs).toEqual(['name', 'status', 'id'])
    expect(resolved.find(item => item.id === crud.id).props.searchFieldRefs).toEqual(['name', 'status'])
    expect(resolved.find(item => item.id === crud.id).props.formOpenMode).toBe('drawer')
    expect(resolved.find(item => item.id === stats.id).props.metrics).toHaveLength(2)
    expect(resolved.find(item => item.id === button.id).props.events[0].targetPageId).toBe('page_detail')
    expect(resolved.find(item => item.id === info.id).props.content).toContain('不能被当占位过滤')
  })

  it('default decorative placeholders are filtered only on object pages, not custom free-layout', () => {
    const info = createGridBlock('info-panel', SAMPLE_MODEL)
    const empty = createGridBlock('empty-state', SAMPLE_MODEL)
    const crud = createGridBlock('AiCrudPage', SAMPLE_MODEL)
    const items = [info, empty, crud].map((block, index) => stampDesignerStyle(block, index))

    const customResolved = resolvePortalPageBlocks({
      page: { id: 'custom', layout: { gridLayout: { items } } },
      node: { id: 'custom', pageShape: 'custom' },
    })
    expect(customResolved.map(item => item.blockType).sort()).toEqual([
      'AiCrudPage',
      'empty-state',
      'info-panel',
    ].sort())
  })

  it('catalog block defaults are not accidental selection chrome', () => {
    for (const meta of listPageBlockCatalog.filter(item => !item.hidden)) {
      const block = createGridBlock(meta.blockType, SAMPLE_MODEL)
      if (!block)
        continue
      expect(isAccidentalSelectionChrome(block.props?.style || {}), meta.blockType).toBe(false)
    }
  })

  it('author colored borders survive readonly strip (组件验收单常见 3px 色边)', () => {
    const authorStyles = [
      { borderColor: 'rgb(37, 151, 177)', borderWidth: 3, backgroundColor: 'rgb(229, 246, 250)' },
      { borderColor: '#b12525', borderWidth: 3, backgroundColor: '#fae5e5' },
      { borderColor: 'rgb(13, 12, 13)', borderWidth: 3, borderStyle: 'dashed', backgroundColor: 'rgb(249, 229, 250)' },
    ]
    for (const style of authorStyles) {
      expect(isAccidentalSelectionChrome(style)).toBe(false)
      expect(stripAccidentalSelectionChrome(style)).toMatchObject(style)
    }
  })

  it('acceptance-page widget set is covered by catalog/widget keys', () => {
    // 与 hr_code_1014「组件验收单」运行态盘点对齐的类型清单
    const acceptanceTypes = [
      'AiCrudPage', 'announcement', 'audio-player', 'avatar', 'back-button', 'barcode',
      'box-layout', 'breadcrumb', 'button-group', 'calendar', 'card', 'code', 'countdown',
      'action-button', 'custom-html', 'descriptions', 'detail-info', 'divider', 'grid-layout',
      'html-tag', 'iframe', 'link', 'list', 'log', 'markdown', 'menu', 'number-animation',
      'page-title', 'pagination', 'qrcode', 'rich-text', 'section-divider', 'signature-pad',
      'spacer', 'split', 'tabs', 'tag-list', 'timeline', 'transfer', 'video-player',
      'vue-component', 'watermark', 'workspace-summary-metrics', 'info-panel', 'stats-strip',
      'empty-state', 'steps',
    ]
    const known = new Set([
      ...listPageBlockCatalog.map(item => item.blockType),
      ...pageWidgetComponentKeys,
    ])
    expect(acceptanceTypes.filter(type => !known.has(type))).toEqual([])
  })
})
