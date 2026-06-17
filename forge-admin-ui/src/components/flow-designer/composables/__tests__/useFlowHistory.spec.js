import { describe, expect, it } from 'vitest'
import { ref } from 'vue'
import { useFlowHistory } from '../useFlowHistory.js'

function makeFlow(name) {
  return { processId: 'P', processName: name, nodes: [], edges: [] }
}

describe('useFlowHistory - 基本快照', () => {
  it('snapshot 后 undo 恢复到上一状态', () => {
    const flow = ref(makeFlow('v1'))
    const h = useFlowHistory(flow)
    h.snapshot()
    flow.value = makeFlow('v2')
    expect(h.canUndo.value).toBe(true)
    h.undo()
    expect(flow.value.processName).toBe('v1')
    expect(h.canRedo.value).toBe(true)
  })

  it('undo + redo 还原', () => {
    const flow = ref(makeFlow('v1'))
    const h = useFlowHistory(flow)
    h.snapshot()
    flow.value = makeFlow('v2')
    h.undo()
    h.redo()
    expect(flow.value.processName).toBe('v2')
  })

  it('undo 多次按栈顺序恢复', () => {
    const flow = ref(makeFlow('v1'))
    const h = useFlowHistory(flow)
    h.snapshot()
    flow.value = makeFlow('v2')
    h.snapshot()
    flow.value = makeFlow('v3')
    h.undo()
    expect(flow.value.processName).toBe('v2')
    h.undo()
    expect(flow.value.processName).toBe('v1')
    expect(h.canUndo.value).toBe(false)
  })
})

describe('useFlowHistory - redo 失效条件', () => {
  it('做了新操作后 redoStack 清空', () => {
    const flow = ref(makeFlow('v1'))
    const h = useFlowHistory(flow)
    h.snapshot()
    flow.value = makeFlow('v2')
    h.undo()
    expect(h.canRedo.value).toBe(true)
    // 新一次 snapshot 应清空 redoStack
    h.snapshot()
    flow.value = makeFlow('v3')
    expect(h.canRedo.value).toBe(false)
  })
})

describe('useFlowHistory - maxStack', () => {
  it('超出 maxStack 时丢弃最早项', () => {
    const flow = ref(makeFlow('v1'))
    const h = useFlowHistory(flow, { maxStack: 3 })
    for (let i = 1; i <= 5; i += 1) {
      h.snapshot()
      flow.value = makeFlow(`v${i + 1}`)
    }
    expect(h.undoStack.value.length).toBe(3)
  })

  it('非法 maxStack 使用默认值 50', () => {
    const flow = ref(makeFlow('v1'))
    const h = useFlowHistory(flow, { maxStack: -1 })
    for (let i = 0; i < 60; i += 1) {
      h.snapshot()
      flow.value = makeFlow(`x${i}`)
    }
    expect(h.undoStack.value.length).toBe(50)
  })
})

describe('useFlowHistory - clear', () => {
  it('clear 同时清空 undoStack / redoStack', () => {
    const flow = ref(makeFlow('v1'))
    const h = useFlowHistory(flow)
    h.snapshot()
    flow.value = makeFlow('v2')
    h.undo()
    h.clear()
    expect(h.canUndo.value).toBe(false)
    expect(h.canRedo.value).toBe(false)
  })
})

describe('useFlowHistory - 边界', () => {
  it('canUndo=false 时 undo 返回 false', () => {
    const flow = ref(makeFlow('v1'))
    const h = useFlowHistory(flow)
    expect(h.undo()).toBe(false)
  })

  it('canRedo=false 时 redo 返回 false', () => {
    const flow = ref(makeFlow('v1'))
    const h = useFlowHistory(flow)
    expect(h.redo()).toBe(false)
  })

  it('快照是深拷贝，回滚后修改不影响 undoStack', () => {
    const flow = ref(makeFlow('v1'))
    const h = useFlowHistory(flow)
    h.snapshot()
    flow.value = makeFlow('v2')
    h.undo()
    flow.value.processName = 'mutated'
    // 再 redo 不应受 mutation 影响
    h.redo()
    expect(flow.value.processName).toBe('v2')
  })
})

describe('useFlowHistory - bindKeyboard', () => {
  it('ctrl+Z 触发 undo，Ctrl+Y 触发 redo', () => {
    const flow = ref(makeFlow('v1'))
    const h = useFlowHistory(flow)
    h.snapshot()
    flow.value = makeFlow('v2')

    const target = new EventTarget()
    const unbind = h.bindKeyboard(target)

    target.dispatchEvent(new KeyboardEvent('keydown', { key: 'z', ctrlKey: true }))
    expect(flow.value.processName).toBe('v1')

    target.dispatchEvent(new KeyboardEvent('keydown', { key: 'y', ctrlKey: true }))
    expect(flow.value.processName).toBe('v2')

    unbind()
  })

  it('ctrl+Shift+Z 触发 redo', () => {
    const flow = ref(makeFlow('v1'))
    const h = useFlowHistory(flow)
    h.snapshot()
    flow.value = makeFlow('v2')
    h.undo()

    const target = new EventTarget()
    h.bindKeyboard(target)
    target.dispatchEvent(new KeyboardEvent('keydown', { key: 'z', ctrlKey: true, shiftKey: true }))
    expect(flow.value.processName).toBe('v2')
  })

  it('unbind 后键盘事件不再触发', () => {
    const flow = ref(makeFlow('v1'))
    const h = useFlowHistory(flow)
    h.snapshot()
    flow.value = makeFlow('v2')

    const target = new EventTarget()
    const unbind = h.bindKeyboard(target)
    unbind()

    target.dispatchEvent(new KeyboardEvent('keydown', { key: 'z', ctrlKey: true }))
    expect(flow.value.processName).toBe('v2') // 没回滚
  })
})
