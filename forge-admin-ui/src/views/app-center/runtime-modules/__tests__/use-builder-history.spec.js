import { describe, expect, it, vi } from 'vitest'
import { nextTick, ref } from 'vue'
import { useBuilderHistory } from '../use-builder-history'

function createHistory(initial = { pages: { p1: { layout: { gridLayout: { items: [] } } } } }) {
  const builder = ref(structuredClone(initial))
  const scheduleNavigationSave = vi.fn()
  const history = useBuilderHistory({
    builder,
    scheduleNavigationSave,
    isHistoryActive: () => true,
  })
  history.resetBuilderHistory(builder.value)
  return { builder, scheduleNavigationSave, history }
}

describe('useBuilderHistory', () => {
  it('records intentional builder edits into the undo stack', async () => {
    const { builder, history } = createHistory()
    builder.value = {
      ...builder.value,
      pages: {
        p1: { layout: { gridLayout: { items: [{ id: 'a' }] } } },
      },
    }
    await nextTick()
    expect(history.canUndo.value).toBe(true)
    history.undoBuilder()
    expect(builder.value.pages.p1.layout.gridLayout.items).toEqual([])
  })

  it('runWithoutHistory keeps auto writes out of the undo stack', async () => {
    const { builder, history } = createHistory({
      pages: { p1: { layout: { gridLayout: { items: [{ id: 'root', children: [] }] } } } },
    })

    // 业务操作：移入布局（应可撤销）
    builder.value = {
      pages: {
        p1: {
          layout: {
            gridLayout: {
              items: [{ id: 'host', children: [{ id: 'nested' }] }],
            },
          },
        },
      },
    }
    await nextTick()
    expect(history.canUndo.value).toBe(true)
    expect(history.undoStack.value).toHaveLength(1)

    // 系统高度回写：不得再 push
    history.runWithoutHistory(() => {
      builder.value = {
        pages: {
          p1: {
            layout: {
              gridLayout: {
                items: [{
                  id: 'host',
                  props: { style: { pageFlowHeight: 320 } },
                  children: [{ id: 'nested' }],
                }],
              },
            },
          },
        },
      }
    })
    await nextTick()
    expect(history.undoStack.value).toHaveLength(1)

    // 一次撤销应回到移入前，而不是高度回写前
    history.undoBuilder()
    expect(builder.value.pages.p1.layout.gridLayout.items).toEqual([
      { id: 'root', children: [] },
    ])
  })
})
