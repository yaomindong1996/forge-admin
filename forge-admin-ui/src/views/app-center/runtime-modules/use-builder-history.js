import { computed, ref, watch } from 'vue'

const HISTORY_LIMIT = 50

/**
 * 应用设计器（builder schema）的撤销 / 重做历史。
 *
 * 通过深比较快照判定变更，watch 使用 flush: 'sync' 保证拖拽等高频回写
 * 也能记录完整历史；主组件只需在 load 完成后调用 resetBuilderHistory。
 *
 * @param {object} options
 * @param {object} options.builder builder schema 的 ref
 * @param {Function} options.scheduleNavigationSave 历史回滚后触发的草稿保存调度
 * @param {Function} options.isHistoryActive 是否允许快捷键（编辑态且非表单设计器）
 */
export function useBuilderHistory({ builder, scheduleNavigationSave, isHistoryActive }) {
  const undoStack = ref([])
  const redoStack = ref([])
  const historyReady = ref(false)
  let latestBuilderSnapshot = null

  const canUndo = computed(() => undoStack.value.length > 0)
  const canRedo = computed(() => redoStack.value.length > 0)

  function cloneBuilderSchema(schema) {
    return JSON.parse(JSON.stringify(schema || {}))
  }

  function resetBuilderHistory(schema = builder.value) {
    undoStack.value = []
    redoStack.value = []
    latestBuilderSnapshot = cloneBuilderSchema(schema)
    historyReady.value = true
  }

  function applyBuilderHistorySnapshot(snapshot) {
    historyReady.value = false
    builder.value = cloneBuilderSchema(snapshot)
    latestBuilderSnapshot = cloneBuilderSchema(builder.value)
    historyReady.value = true
    scheduleNavigationSave()
  }

  function undoBuilder() {
    if (!canUndo.value)
      return
    const currentSnapshot = cloneBuilderSchema(builder.value)
    const previousSnapshot = undoStack.value[undoStack.value.length - 1]
    undoStack.value = undoStack.value.slice(0, -1)
    redoStack.value = [currentSnapshot, ...redoStack.value].slice(0, HISTORY_LIMIT)
    applyBuilderHistorySnapshot(previousSnapshot)
  }

  function redoBuilder() {
    if (!canRedo.value)
      return
    const currentSnapshot = cloneBuilderSchema(builder.value)
    const nextSnapshot = redoStack.value[0]
    redoStack.value = redoStack.value.slice(1)
    undoStack.value = [...undoStack.value, currentSnapshot].slice(-HISTORY_LIMIT)
    applyBuilderHistorySnapshot(nextSnapshot)
  }

  function handleBuilderShortcut(event) {
    if (!isHistoryActive())
      return
    const key = event.key?.toLowerCase?.()
    const isUndoKey = (event.metaKey || event.ctrlKey) && !event.shiftKey && key === 'z'
    const isRedoKey = (event.metaKey || event.ctrlKey) && ((event.shiftKey && key === 'z') || key === 'y')
    if (!isUndoKey && !isRedoKey)
      return
    if (event.target?.closest?.('input, textarea, [contenteditable="true"]'))
      return
    event.preventDefault()
    if (isRedoKey)
      redoBuilder()
    else
      undoBuilder()
  }

  watch(builder, (nextBuilder) => {
    if (!nextBuilder) {
      latestBuilderSnapshot = null
      return
    }
    const nextSnapshot = cloneBuilderSchema(nextBuilder)
    if (!historyReady.value) {
      latestBuilderSnapshot = nextSnapshot
      return
    }
    if (JSON.stringify(nextSnapshot) === JSON.stringify(latestBuilderSnapshot))
      return
    if (latestBuilderSnapshot)
      undoStack.value = [...undoStack.value, latestBuilderSnapshot].slice(-HISTORY_LIMIT)
    redoStack.value = []
    latestBuilderSnapshot = nextSnapshot
  }, { deep: true, flush: 'sync' })

  return {
    undoStack,
    redoStack,
    historyReady,
    canUndo,
    canRedo,
    cloneBuilderSchema,
    resetBuilderHistory,
    undoBuilder,
    redoBuilder,
    handleBuilderShortcut,
  }
}
