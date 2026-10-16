import { describe, expect, it } from 'vitest'
import {
  DEFAULT_EMPTY_STATE_PLACEHOLDER,
  DEFAULT_INFO_PANEL_PLACEHOLDER,
  isDefaultEmptyStatePlaceholder,
  isDefaultInfoPanelPlaceholder,
  isRuntimeAutoHeightBlock,
  resolvePortalPageBlocks,
  shouldUseContentSizedFlow,
  shouldUsePageFlowStack,
  sortBlocksByPageFlowY,
} from '../portal-page-runtime-layout'

describe('portal-page-runtime-layout', () => {
  it('treats standalone form pages as auto-height runtime blocks', () => {
    expect(isRuntimeAutoHeightBlock({ blockType: 'AiForm' })).toBe(true)
    expect(isRuntimeAutoHeightBlock({
      blockType: 'AiCrudPage',
      props: { formOnly: true },
    })).toBe(true)
    expect(isRuntimeAutoHeightBlock({
      blockType: 'AiCrudPage',
      props: { objectRef: { pageMode: 'form' } },
    })).toBe(true)
    expect(isRuntimeAutoHeightBlock({
      blockType: 'AiCrudPage',
      props: { objectRef: { pageKey: 'form' } },
    })).toBe(true)
  })

  it('keeps list CRUD pages on the designer canvas height', () => {
    expect(isRuntimeAutoHeightBlock({ blockType: 'AiCrudPage' })).toBe(false)
    expect(isRuntimeAutoHeightBlock({
      blockType: 'AiCrudPage',
      props: { objectRef: { pageMode: 'list' } },
    })).toBe(false)
    expect(isRuntimeAutoHeightBlock({
      blockType: 'AiCrudPage',
      props: { formOnly: false, objectRef: { pageKey: 'form', pageMode: 'form' } },
    })).toBe(false)
    expect(isRuntimeAutoHeightBlock({
      blockType: 'AiCrudPage',
      props: { objectRef: { pageKey: 'form', pageMode: 'list' } },
    })).toBe(false)
  })

  it('uses content-sized flow for published form pages, not design preview fill', () => {
    const formBlocks = [{
      blockType: 'AiCrudPage',
      props: { formOnly: true },
    }]
    expect(shouldUseContentSizedFlow(formBlocks)).toBe(true)
    expect(shouldUseContentSizedFlow(formBlocks, { fillHost: true })).toBe(false)
  })

  it('keeps fillHost multi-block pages on document flow so lists are not clipped', () => {
    const mixed = [
      { blockType: 'page-title', props: { style: { widthMode: 'full' } } },
      { blockType: 'AiCrudPage', props: { style: { widthMode: 'full' } } },
    ]
    expect(shouldUseContentSizedFlow(mixed, { fillHost: true })).toBe(true)
    expect(shouldUseContentSizedFlow([
      { blockType: 'AiCrudPage' },
    ], { fillHost: true })).toBe(false)
  })

  it('keeps free-layout absolute even when fillHost and runtimePreview are set', () => {
    const free = [
      { blockType: 'AiCrudPage', props: { style: { widthMode: 'fixed', pageFlowWidth: '360px', pageFlowX: 80, pageFlowY: 40 } } },
      { blockType: 'page-title', props: { style: { widthMode: 'full', pageFlowY: 648 } } },
      { blockType: 'custom-html', props: { style: { widthMode: 'fixed', pageFlowWidth: '280px', pageFlowY: 736 } } },
    ]
    expect(shouldUsePageFlowStack(free)).toBe(false)
    expect(shouldUseContentSizedFlow(free, { fillHost: true, runtimePreview: true })).toBe(false)
  })

  it('stacks full-width workbench blocks in document flow so resize cannot overlap', () => {
    const workbench = [
      { id: 'm', blockType: 'workspace-summary-metrics', props: { style: { widthMode: 'full', pageFlowY: 20 } } },
      { id: 'i', blockType: 'info-panel', props: { style: { widthMode: 'full', pageFlowY: 236 } } },
      { id: 'e', blockType: 'empty-state', props: { style: { widthMode: 'full', pageFlowY: 360 } } },
    ]
    expect(shouldUsePageFlowStack(workbench)).toBe(true)
    expect(shouldUseContentSizedFlow(workbench)).toBe(true)
    expect(sortBlocksByPageFlowY(workbench).map(item => item.id)).toEqual(['m', 'i', 'e'])
  })

  it('forces workbench page stack even when a block lost full width', () => {
    const mixed = [
      { blockType: 'card', props: { style: { widthMode: 'fixed' } } },
      { blockType: 'info-panel', props: { style: { widthMode: 'full' } } },
    ]
    expect(shouldUsePageFlowStack(mixed)).toBe(false)
    expect(shouldUsePageFlowStack(mixed, { pageId: 'system:workbench' })).toBe(true)
    // 工作台 pageId 仍走文档流；普通运行预览不再因 runtimePreview 强行堆叠自由布局
    expect(shouldUseContentSizedFlow(mixed, { pageId: 'system:workbench', runtimePreview: true })).toBe(true)
  })

  it('keeps absolute free layout when any block is not full width', () => {
    const free = [
      { blockType: 'info-panel', props: { style: { widthMode: 'full' } } },
      { blockType: 'card', props: { style: { widthMode: 'fixed' } } },
    ]
    expect(shouldUsePageFlowStack(free)).toBe(false)
    expect(shouldUseContentSizedFlow(free, { runtimePreview: true })).toBe(false)
  })

  it('hides untouched info-panel and empty-state placeholders on object pages', () => {
    const objectRef = { objectId: '1', objectCode: 'demo', configKey: 'demo' }
    const blocks = resolvePortalPageBlocks({
      node: { id: 'page_1', pageType: 'object', objectRef },
      page: {
        layout: {
          gridLayout: {
            items: [
              {
                id: 'tip_1',
                blockType: 'info-panel',
                props: { ...DEFAULT_INFO_PANEL_PLACEHOLDER },
              },
              {
                id: 'empty_1',
                blockType: 'empty-state',
                props: { ...DEFAULT_EMPTY_STATE_PLACEHOLDER, actionText: '新建数据' },
              },
            ],
          },
        },
      },
      resolveObjectRef: source => source?.objectRef || objectRef,
    })
    expect(blocks).toHaveLength(1)
    expect(blocks[0]).toMatchObject({
      blockType: 'AiCrudPage',
      props: { objectRef },
    })
    expect(isDefaultInfoPanelPlaceholder({
      blockType: 'info-panel',
      props: { ...DEFAULT_INFO_PANEL_PLACEHOLDER },
    })).toBe(true)
    expect(isDefaultEmptyStatePlaceholder({
      blockType: 'empty-state',
      props: { ...DEFAULT_EMPTY_STATE_PLACEHOLDER },
    })).toBe(true)
  })

  it('keeps customized info-panel copy on object pages that already have CRUD', () => {
    const objectRef = { objectId: '1', configKey: 'demo' }
    const blocks = resolvePortalPageBlocks({
      node: { id: 'page_1', pageType: 'object', objectRef },
      page: {
        layout: {
          gridLayout: {
            items: [
              {
                id: 'tip_1',
                blockType: 'info-panel',
                props: { title: '报名须知', content: '请先核对身份信息。' },
              },
              {
                id: 'crud_1',
                blockType: 'AiCrudPage',
                props: { objectRef },
              },
            ],
          },
        },
      },
      resolveObjectRef: () => objectRef,
    })
    expect(blocks.map(item => item.blockType)).toEqual(['info-panel', 'AiCrudPage'])
  })

  it('keeps intro pages that only use the default info-panel', () => {
    const blocks = resolvePortalPageBlocks({
      node: { id: 'page_intro', pageType: 'content' },
      page: {
        layout: {
          gridLayout: {
            items: [{
              id: 'tip_1',
              blockType: 'info-panel',
              props: { ...DEFAULT_INFO_PANEL_PLACEHOLDER },
            }],
          },
        },
      },
      resolveObjectRef: () => null,
    })
    expect(blocks).toHaveLength(1)
    expect(blocks[0].blockType).toBe('info-panel')
  })

  it('keeps designed page-title blocks on business pages during preview/runtime', () => {
    const blocks = resolvePortalPageBlocks({
      node: { id: 'page_custom', pageType: 'content' },
      page: {
        layout: {
          gridLayout: {
            items: [
              {
                id: 'title_1',
                blockType: 'page-title',
                props: { title: '报名须知', content: '<h1>报名须知</h1>' },
              },
              {
                id: 'tip_1',
                blockType: 'info-panel',
                props: { title: '说明', content: '请认真填写' },
              },
            ],
          },
        },
      },
      resolveObjectRef: () => null,
    })
    expect(blocks.map(item => item.blockType)).toEqual(['page-title', 'info-panel'])
  })

  it('strips canvas page-title only on the workbench page', () => {
    const blocks = resolvePortalPageBlocks({
      node: { id: 'system:workbench', pageType: 'content' },
      page: {
        layout: {
          gridLayout: {
            items: [
              { id: 'title_1', blockType: 'page-title', props: { title: '工作台' } },
              { id: 'm1', blockType: 'workspace-summary-metrics' },
            ],
          },
        },
      },
      resolveObjectRef: () => null,
    })
    expect(blocks.map(item => item.blockType)).toEqual(['workspace-summary-metrics'])
  })
})
