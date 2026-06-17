/**
 * useFlowHistory — 撤销 / 重做
 *
 * 基于 JSON 快照（深拷贝）的命令栈：
 * - snapshot()：把当前 flowJson 深拷贝压入 undoStack；清空 redoStack
 * - undo()：把 undoStack 栈顶弹出压入 redoStack；恢复 flowJsonRef.value 到上一个快照
 * - redo()：反向操作
 *
 * 调用约定：
 * - 由 useFlowDesigner 的每个变更操作前调用 snapshot()，让 undo 可恢复到变更前
 * - 拖拽场景节流：drop 完成后才 snapshot
 *
 * 键盘绑定：
 * - bindKeyboard(target)：在 target（默认 window）上监听 Ctrl/Cmd+Z / Ctrl/Cmd+Y / Ctrl/Cmd+Shift+Z
 * - 返回 unbind 函数，调用方在 unmount 时清理
 *
 * 容量：
 * - maxStack 默认 50；超出时丢弃最早项（FIFO）
 */

import { computed, ref } from 'vue'

const DEFAULT_MAX = 50

export function useFlowHistory(flowJsonRef, options = {}) {
  const max = Number.isInteger(options.maxStack) && options.maxStack > 0
    ? options.maxStack
    : DEFAULT_MAX

  const undoStack = ref([])
  const redoStack = ref([])

  const canUndo = computed(() => undoStack.value.length > 0)
  const canRedo = computed(() => redoStack.value.length > 0)

  function snapshot() {
    if (!flowJsonRef || !flowJsonRef.value)
      return
    undoStack.value.push(deepClone(flowJsonRef.value))
    if (undoStack.value.length > max)
      undoStack.value.shift()
    redoStack.value = []
  }

  function undo() {
    if (!canUndo.value || !flowJsonRef)
      return false
    redoStack.value.push(deepClone(flowJsonRef.value))
    const prev = undoStack.value.pop()
    flowJsonRef.value = prev
    return true
  }

  function redo() {
    if (!canRedo.value || !flowJsonRef)
      return false
    undoStack.value.push(deepClone(flowJsonRef.value))
    if (undoStack.value.length > max)
      undoStack.value.shift()
    const next = redoStack.value.pop()
    flowJsonRef.value = next
    return true
  }

  function clear() {
    undoStack.value = []
    redoStack.value = []
  }

  function bindKeyboard(target) {
    const el = target || (typeof window !== 'undefined' ? window : null)
    if (!el)
      return () => {}

    const handler = (event) => {
      const meta = event.metaKey || event.ctrlKey
      if (!meta)
        return
      const key = (event.key || '').toLowerCase()
      // Cmd/Ctrl + Shift + Z 或 Cmd/Ctrl + Y → redo
      if ((key === 'z' && event.shiftKey) || key === 'y') {
        if (canRedo.value) {
          redo()
          event.preventDefault()
        }
        return
      }
      // Cmd/Ctrl + Z → undo
      if (key === 'z') {
        if (canUndo.value) {
          undo()
          event.preventDefault()
        }
      }
    }

    el.addEventListener('keydown', handler)
    return () => el.removeEventListener('keydown', handler)
  }

  return {
    undoStack,
    redoStack,
    canUndo,
    canRedo,
    snapshot,
    undo,
    redo,
    clear,
    bindKeyboard,
  }
}

function deepClone(v) {
  return JSON.parse(JSON.stringify(v))
}
