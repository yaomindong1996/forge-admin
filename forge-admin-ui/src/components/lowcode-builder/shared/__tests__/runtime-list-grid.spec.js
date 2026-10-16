import { describe, expect, it } from 'vitest'
import {
  isRichListGridLayout,
  normalizeRuntimeListGridLayout,
  buildRuntimeListFlowBlocks,
  resolveListGridLayoutFromPageSchema,
  shouldRenderRuntimeListGrid,
} from '../runtime-list-grid'

describe('runtime-list-grid', () => {
  it('detects companion blocks as rich layout', () => {
    expect(isRichListGridLayout({
      items: [
        { id: 'a', blockType: 'AiCrudPage' },
        { id: 'b', blockType: 'info-panel' },
      ],
    })).toBe(true)
    expect(isRichListGridLayout({
      items: [{ id: 'a', blockType: 'AiCrudPage' }],
    })).toBe(false)
    expect(isRichListGridLayout({
      items: [
        { id: 'a', blockType: 'AiCrudPage' },
        { id: 'b', blockType: 'data-table' },
      ],
    })).toBe(false)
  })

  it('normalizes design heights into flow-friendly auto blocks', () => {
    const next = normalizeRuntimeListGridLayout({
      items: [{
        id: 'crud',
        blockType: 'AiCrudPage',
        props: { style: { heightMode: 'full', height: 632, width: 1360 }, previewLiveData: false },
      }],
    })
    expect(next.items[0].props.style.heightMode).toBe('auto')
    expect(next.items[0].props.style.widthMode).toBe('full')
    expect(next.items[0].props.style.height).toBeUndefined()
    expect(next.items[0].props.previewLiveData).toBe(true)
  })

  it('sorts flow blocks by pageFlowY', () => {
    const blocks = buildRuntimeListFlowBlocks({
      items: [
        { id: 'b', blockType: 'calendar', props: { style: { pageFlowY: 200 } } },
        { id: 'a', blockType: 'AiCrudPage', props: { style: { pageFlowY: 0 } } },
      ],
    })
    expect(blocks.map(item => item.id)).toEqual(['a', 'b'])
  })

  it('sorts by canvas style.y (designer order), not raw gridY', () => {
    const blocks = buildRuntimeListFlowBlocks({
      rowHeight: 32,
      gap: 8,
      items: [
        { id: 'block_crud', blockType: 'AiCrudPage', gridY: 0, props: { style: { y: 704 }, text: 'E2E_AiCrudPage' } },
        { id: 'announcement_x', blockType: 'announcement', gridY: 132, props: { style: { y: 44 }, text: 'E2E_announcement' } },
        { id: 'audio_x', blockType: 'audio-player', gridY: 170, props: { style: { y: 176 }, text: 'E2E_audio-player' } },
        { id: 'action-button_x', blockType: 'action-button', gridY: 62, props: { style: { y: '' }, text: 'E2E_action-button' } },
      ],
    })
    // announcement(44) → audio(176) → crud(704) → action 空 y 回落 gridY*40=2480
    expect(blocks.map(item => item.id)).toEqual([
      'announcement_x',
      'audio_x',
      'block_crud',
      'action-button_x',
    ])
  })

  it('renders grid for list only when rich', () => {
    const pageSchema = {
      listGridLayout: {
        items: [
          { id: 't', blockType: 'tree-panel' },
          { id: 'c', blockType: 'AiCrudPage' },
        ],
      },
    }
    expect(shouldRenderRuntimeListGrid({ pageSchema, pageKey: 'list' })).toBe(true)
    expect(shouldRenderRuntimeListGrid({
      pageSchema: { listGridLayout: { items: [{ id: 'c', blockType: 'AiCrudPage' }] } },
      pageKey: 'list',
    })).toBe(false)
  })

  it('resolves list page grid from pages[].gridLayout', () => {
    const layout = resolveListGridLayoutFromPageSchema({
      pages: [{ pageKey: 'list', gridLayout: { items: [{ id: 'x', blockType: 'page-title' }] } }],
      listGridLayout: { items: [{ id: 'y', blockType: 'AiCrudPage' }] },
    }, { pageKey: 'list' })
    expect(layout.items[0].id).toBe('x')
  })

  it('keeps empty non-list pages on RuntimeListGrid shell instead of bare CRUD', () => {
    const pageSchema = {
      pages: [
        { pageKey: 'list', gridLayout: { items: [{ id: 'c', blockType: 'AiCrudPage' }] } },
        { pageKey: 'e2e_dialog', title: '弹窗页', gridLayout: { items: [] } },
        { pageKey: 'detail_page', title: '详情页' },
      ],
    }
    expect(shouldRenderRuntimeListGrid({ pageSchema, pageKey: 'e2e_dialog' })).toBe(true)
    expect(shouldRenderRuntimeListGrid({ pageSchema, pageKey: 'detail_page' })).toBe(true)
    expect(shouldRenderRuntimeListGrid({ pageSchema, pageKey: 'missing_page' })).toBe(false)
    expect(normalizeRuntimeListGridLayout(
      resolveListGridLayoutFromPageSchema(pageSchema, { pageKey: 'e2e_dialog' }),
      pageSchema,
      'simple-crud',
      { allowEmpty: true },
    )?.items).toEqual([])
    expect(normalizeRuntimeListGridLayout(
      resolveListGridLayoutFromPageSchema(pageSchema, { pageKey: 'detail_page' }),
      pageSchema,
      'simple-crud',
      { allowEmpty: true },
    )?.items).toEqual([])
  })
})
