/**
 * 自由布局：编辑 / 预览 / 运行 在属性、样式、数据上的契约一致性。
 * 同一份 layout fixture 走设计壳几何、门户壳几何、CRUD 属性合并、样式剥离。
 */
import { describe, expect, it } from 'vitest'
import { stripAccidentalSelectionChrome, isAccidentalSelectionChrome } from '@/components/lowcode-builder/page/block-style-chrome'
import { buildRuntimeCrudBlockProps, resolveEffectiveFormOpenMode } from '@/components/lowcode-builder/page/runtime-crud-block-props'
import { listPageBlockCatalog } from '@/components/lowcode-builder/page/page-schema'
import { pageWidgetComponentKeys } from '@/components/lowcode-builder/shared/page-widget-schema'
import {
  normalizeFlowBlockItem,
  buildRuntimeListFlowBlocks,
} from '@/components/lowcode-builder/shared/runtime-list-grid'
import {
  shouldUseContentSizedFlow,
  shouldUsePageFlowStack,
  resolvePortalPageBlocks,
} from '@/views/app-center/components/portal/portal-page-runtime-layout'
import { resolvePageBlockShellStyle } from '@/views/app-center/runtime-modules/page-flow-geometry'

/** 代表性自由布局页：固定宽 CRUD + 标题 + 提示 + 小部件，覆盖属性/样式/数据三态 */
function createFreeLayoutFixture() {
  return {
    pageFlowCoordSpace: 'content',
    pagePadding: { top: 8, right: 8, bottom: 8, left: 8 },
    items: [
      {
        id: 'title_1',
        blockType: 'page-title',
        props: {
          title: '自由布局验收页',
          subtitle: '编辑预览运行一致',
          style: {
            widthMode: 'full',
            heightMode: 'fixed',
            pageFlowX: 0,
            pageFlowY: 0,
            pageFlowHeight: 72,
            backgroundColor: '#ffffff',
            borderColor: '#e4e4e7',
            borderWidth: 1,
            borderStyle: 'solid',
            borderRadius: 8,
            padding: 12,
            color: '#111827',
            fontSize: 18,
          },
        },
      },
      {
        id: 'crud_1',
        blockType: 'AiCrudPage',
        props: {
          formOpenMode: 'drawer',
          modalWidth: 720,
          drawerPlacement: 'right',
          previewLiveData: false,
          searchFieldRefs: ['name', 'status'],
          fieldRefs: ['name', 'status', 'updatedAt'],
          style: {
            widthMode: 'fixed',
            heightMode: 'fixed',
            pageFlowX: 24,
            pageFlowY: 96,
            pageFlowWidth: '960px',
            pageFlowHeight: 480,
            backgroundColor: 'transparent',
            borderColor: 'transparent',
            borderWidth: 0,
            borderStyle: 'none',
            borderRadius: 0,
            boxShadow: 'none',
          },
        },
      },
      {
        id: 'info_1',
        blockType: 'info-panel',
        props: {
          title: '业务说明',
          content: '这条提示必须在预览/运行保留，不能被当装饰占位过滤。',
          style: {
            widthMode: 'fixed',
            heightMode: 'auto',
            pageFlowX: 1000,
            pageFlowY: 96,
            pageFlowWidth: '280px',
            pageFlowHeight: 160,
            backgroundColor: '#f8fafc',
            borderColor: '#cbd5e1',
            borderWidth: 1,
            borderStyle: 'dashed',
            padding: 16,
          },
        },
      },
      {
        id: 'md_1',
        blockType: 'markdown',
        props: {
          content: '## Widget 数据\n预览只读，运行可读绑定。',
          dataBinding: { enabled: false },
          style: {
            widthMode: 'fixed',
            heightMode: 'auto',
            pageFlowX: 24,
            pageFlowY: 600,
            pageFlowWidth: '640px',
            pageFlowHeight: 120,
            opacity: 0.95,
            fontSize: 14,
            color: '#334155',
          },
        },
      },
      {
        id: 'btn_1',
        blockType: 'action-button',
        props: {
          text: '提交申请',
          type: 'primary',
          events: [{ trigger: 'click', action: 'navigate', targetPageId: 'page_detail' }],
          style: {
            widthMode: 'fixed',
            heightMode: 'fixed',
            pageFlowX: 700,
            pageFlowY: 600,
            pageFlowWidth: '140px',
            pageFlowHeight: 40,
          },
        },
      },
    ],
  }
}

function authoredVisualStyle(style = {}) {
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

describe('free-layout edit / preview / runtime parity', () => {
  const layout = createFreeLayoutFixture()
  const items = layout.items
  const shellOpts = {
    pagePadding: layout.pagePadding,
    pageFlowCoordSpace: layout.pageFlowCoordSpace,
  }

  it('keeps free-layout absolute across edit shell and portal preview (not content-sized)', () => {
    expect(shouldUsePageFlowStack(items)).toBe(false)
    expect(shouldUseContentSizedFlow(items)).toBe(false)
    expect(shouldUseContentSizedFlow(items, { fillHost: true, runtimePreview: true })).toBe(false)

    const crud = items.find(item => item.id === 'crud_1')
    const editShell = resolvePageBlockShellStyle(crud, items, shellOpts)
    const previewShell = resolvePageBlockShellStyle(crud, items, {
      ...shellOpts,
      fillHost: false,
      runtimePreview: true,
    })
    expect(editShell.position).toBe('absolute')
    expect(previewShell.position).toBe('absolute')
    expect(editShell.left).toBe(previewShell.left)
    expect(editShell.top).toBe(previewShell.top)
    // 内容坐标：padding.left(8) + pageFlowX(24)
    expect(editShell.left).toBe('32px')
    expect(editShell.top).toBe('104px')
  })

  it('preserves authored appearance styles for every fixture block through readonly chrome strip', () => {
    for (const block of items) {
      const visual = authoredVisualStyle(block.props.style || {})
      expect(isAccidentalSelectionChrome(visual)).toBe(false)
      const stripped = stripAccidentalSelectionChrome(visual)
      expect(stripped.backgroundColor).toBe(visual.backgroundColor)
      expect(stripped.borderColor).toBe(visual.borderColor)
      expect(stripped.borderWidth).toBe(visual.borderWidth)
      expect(stripped.borderStyle).toBe(visual.borderStyle)
      if (visual.borderRadius != null)
        expect(stripped.borderRadius).toBe(visual.borderRadius)
      if (visual.padding != null)
        expect(stripped.padding).toBe(visual.padding)
      if (visual.color)
        expect(stripped.color).toBe(visual.color)
      if (visual.fontSize != null)
        expect(stripped.fontSize).toBe(visual.fontSize)
      if (visual.opacity != null)
        expect(stripped.opacity).toBe(visual.opacity)
    }
  })

  it('strips only accidental selection chrome, not designer borders', () => {
    const leaked = {
      borderColor: '#2080f0',
      borderWidth: 2,
      borderStyle: 'solid',
      backgroundColor: '#f3f8ff',
      padding: 8,
    }
    const cleaned = stripAccidentalSelectionChrome(leaked)
    expect(cleaned.borderWidth).toBe(0)
    expect(cleaned.borderStyle).toBe('none')
    expect(cleaned.backgroundColor).toBe('transparent')
    expect(cleaned.padding).toBe(8)
  })

  it('portal keeps customized info-panel / page-title / widgets (does not filter real content)', () => {
    const page = { id: 'page_free', layout: { gridLayout: layout } }
    const blocks = resolvePortalPageBlocks({
      page,
      node: { id: 'page_free', pageShape: 'custom' },
    })
    expect(blocks.map(item => item.id)).toEqual([
      'title_1',
      'crud_1',
      'info_1',
      'md_1',
      'btn_1',
    ])
    expect(blocks.find(item => item.id === 'info_1').props.content).toContain('必须在预览/运行保留')
    expect(blocks.find(item => item.id === 'title_1').props.title).toBe('自由布局验收页')
    expect(blocks.find(item => item.id === 'btn_1').props.events?.[0]?.targetPageId).toBe('page_detail')
  })

  it('free-layout CRUD prefers block form chrome over object runtime (属性编辑入口)', () => {
    const blockProps = items.find(item => item.id === 'crud_1').props
    const runtimeProps = {
      configKey: 'obj_demo',
      formOpenMode: 'modal',
      modalWidth: 520,
      drawerPlacement: 'left',
      columns: [{ field: 'name' }],
      fieldCatalog: [
        { field: 'name', listVisible: true },
        { field: 'status', listVisible: true },
        { field: 'updatedAt', listVisible: true },
      ],
      searchSchema: [
        { field: 'name', label: '名称' },
        { field: 'status', label: '状态' },
      ],
    }

    expect(resolveEffectiveFormOpenMode(blockProps, runtimeProps, { preferBlockFormChrome: true }))
      .toBe('drawer')
    expect(resolveEffectiveFormOpenMode(blockProps, runtimeProps, { preferBlockFormChrome: false }))
      .toBe('modal')

    const freeLayoutMerged = buildRuntimeCrudBlockProps({
      runtimeProps,
      blockProps,
      runtimeInteractive: true,
      configuredFieldRefs: blockProps.fieldRefs,
      hasExplicitSearchFieldRefs: true,
      preferBlockFormChrome: true,
    })
    expect(freeLayoutMerged.formOpenMode).toBe('drawer')
    expect(freeLayoutMerged.modalWidth).toBe(720)
    expect(freeLayoutMerged.drawerPlacement).toBe('right')

    const objectListMerged = buildRuntimeCrudBlockProps({
      runtimeProps,
      blockProps,
      runtimeInteractive: true,
      configuredFieldRefs: blockProps.fieldRefs,
      hasExplicitSearchFieldRefs: true,
      preferBlockFormChrome: false,
    })
    expect(objectListMerged.formOpenMode).toBe('modal')
    expect(objectListMerged.modalWidth).toBe(520)
  })

  it('preserves search/table field refs from block props into runtime CRUD merge (数据)', () => {
    const blockProps = items.find(item => item.id === 'crud_1').props
    const merged = buildRuntimeCrudBlockProps({
      runtimeProps: {
        configKey: 'obj_demo',
        columns: [
          { field: 'name' },
          { field: 'status' },
          { field: 'updatedAt' },
          { field: 'createdAt' },
        ],
        fieldCatalog: [
          { field: 'name', listVisible: true },
          { field: 'status', listVisible: true },
          { field: 'updatedAt', listVisible: true },
          { field: 'createdAt', listVisible: true },
        ],
        searchSchema: [
          { field: 'name', label: '名称' },
          { field: 'status', label: '状态' },
          { field: 'createdAt', label: '创建时间' },
        ],
      },
      blockProps,
      runtimeInteractive: true,
      configuredFieldRefs: blockProps.fieldRefs,
      hasExplicitSearchFieldRefs: true,
      preferBlockFormChrome: true,
    })
    // 勾选字段不得被对象全量字段淹没
    expect(blockProps.fieldRefs).toEqual(['name', 'status', 'updatedAt'])
    expect(blockProps.searchFieldRefs).toEqual(['name', 'status'])
    expect(merged).toBeTruthy()
  })

  it('object rich-list flow (RuntimeListGridFlow) intentionally differs from free-layout absolute', () => {
    const live = normalizeFlowBlockItem(items.find(item => item.id === 'crud_1'), { forceLivePreview: true })
    const designPreview = normalizeFlowBlockItem(items.find(item => item.id === 'crud_1'), { forceLivePreview: false })

    // 伴生块文档流：通栏 + auto 高（自由布局绝对定位不走这条）
    expect(live.props.style.widthMode).toBe('full')
    expect(live.props.style.heightMode).toBe('auto')
    expect(live.props.previewLiveData).toBe(true)
    expect(designPreview.props.previewLiveData).toBeUndefined()

    const flowBlocks = buildRuntimeListFlowBlocks(layout, { forceLivePreview: true })
    expect(flowBlocks.map(item => item.id)).toEqual([
      'title_1',
      'crud_1',
      'info_1',
      'md_1',
      'btn_1',
    ])
    // 自由布局页本身不得被误判成 content-sized 文档流
    expect(shouldUseContentSizedFlow(items)).toBe(false)
  })

  it('CRUD free-layout shell applies min height floor consistently (edit ≈ preview absolute)', () => {
    const crud = items.find(item => item.id === 'crud_1')
    const shell = resolvePageBlockShellStyle(crud, items, shellOpts)
    // pageFlowHeight 480 >= 420 下限，保持作者高度
    expect(shell.height).toBe('480px')
    expect(shell.minHeight).toBe('480px')

    const shortCrud = {
      ...crud,
      props: {
        ...crud.props,
        style: { ...crud.props.style, pageFlowHeight: 100 },
      },
    }
    const floored = resolvePageBlockShellStyle(shortCrud, items.map(item => (item.id === 'crud_1' ? shortCrud : item)), shellOpts)
    expect(floored.height).toBe('420px')
    expect(floored.minHeight).toBe('420px')
  })

  it('catalog covers every page widget key used by GridBlockRenderer', () => {
    const catalogTypes = new Set(listPageBlockCatalog.map(item => item.blockType))
    const missing = pageWidgetComponentKeys.filter(key => !catalogTypes.has(key))
    expect(missing).toEqual([])
  })

  it('representative native blocks expose style + props that survive portal resolve', () => {
    const samples = [
      { blockType: 'stats-strip', props: { metrics: [{ label: '待办', value: '3' }], style: { widthMode: 'fixed', pageFlowX: 10, pageFlowY: 10, backgroundColor: '#eff6ff' } } },
      { blockType: 'steps', props: { steps: [{ title: '提交' }, { title: '审批' }], style: { widthMode: 'full', pageFlowY: 20, borderRadius: 6 } } },
      { blockType: 'timeline', props: { items: [{ title: '创建', time: '10:00' }], style: { widthMode: 'fixed', pageFlowWidth: '320px', padding: 8 } } },
      { blockType: 'tag-list', props: { tags: [{ label: '紧急', type: 'error' }], style: { widthMode: 'auto', pageFlowX: 40, pageFlowY: 40 } } },
      { blockType: 'empty-state', props: { title: '自定义空态', description: '请新建一条', style: { widthMode: 'fixed', pageFlowWidth: '400px' } } },
      { blockType: 'card', props: { title: '分组', children: [], style: { widthMode: 'fixed', pageFlowWidth: '480px', boxShadow: '0 1px 2px rgba(0,0,0,.08)' } } },
      { blockType: 'custom-html', props: { html: '<p>说明</p>', style: { widthMode: 'fixed', pageFlowWidth: '360px', color: '#64748b' } } },
      { blockType: 'divider', props: { style: { widthMode: 'full', pageFlowY: 200 } } },
    ]

    for (const sample of samples) {
      const block = { id: `sample_${sample.blockType}`, ...sample }
      const page = {
        id: 'page_sample',
        layout: { gridLayout: { items: [block, items.find(item => item.id === 'crud_1')] } },
      }
      const resolved = resolvePortalPageBlocks({ page, node: { id: 'page_sample', pageShape: 'custom' } })
      const hit = resolved.find(item => item.id === block.id)
      expect(hit, sample.blockType).toBeTruthy()
      expect(hit.props).toMatchObject(sample.props)
      const visual = authoredVisualStyle(hit.props.style || {})
      expect(stripAccidentalSelectionChrome(visual)).toMatchObject(visual)
    }
  })
})
