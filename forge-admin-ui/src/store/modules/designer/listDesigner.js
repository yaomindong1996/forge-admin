import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { findBlockInTree } from '@/components/lowcode-builder/page/blockTree'

/**
 * 列表页网格设计器全局状态（ListPageGridDesigner 通信中枢）
 *
 * 职责边界：
 * - layout / selectedBlockId / propertyPanelTab 等跨组件通信状态统一收敛于此，
 *   设计器入口组件负责 props -> store 桥接，拆分出的子面板直接读写 store
 * - 拖拽/缩放过程中的 deferLayoutEmit 延迟提交标记
 * - 区块树只读查找（findBlockInTree，与设计器共用 blockTree.js）
 * - rowActionOverrides：操作列改名/隐藏/自定义按钮的权威缓存，避免 layout 回写时序把 props 冲掉后预览回落默认编辑/删除
 * 布局算法（normalizeDesignerLayout/syncGridLayoutWithModel）与 patch 类写操作
 * 仍由设计器组件负责，避免 store 承载过多业务算法。
 */
export const useListDesignerStore = defineStore('listDesigner', () => {
  const layout = ref({})
  const selectedBlockId = ref(null)
  const propertyPanelTab = ref('props')
  const deferLayoutEmit = ref(false)
  const hasDeferredLayoutEmit = ref(false)
  /** blockId -> { builtinRowActions?, customActions? } */
  const rowActionOverrides = ref({})
  /** 最近一次属性面板改过的操作配置（预览块 id 对不上时兜底） */
  const latestRowActionOverride = ref(null)

  const blocks = computed(() => layout.value.items || [])
  const selectedBlock = computed(() => findBlockInTree(blocks.value, selectedBlockId.value) || null)

  function applyLayout(next) {
    layout.value = next
  }

  function selectBlock(blockId) {
    selectedBlockId.value = blockId
  }

  function clearSelection() {
    selectedBlockId.value = null
  }

  function setPropertyTab(tab) {
    propertyPanelTab.value = tab
  }

  function setBlockRowActionOverride(blockId, patch = {}) {
    const id = String(blockId || '').trim()
    const prev = id ? (rowActionOverrides.value[id] || {}) : {}
    const next = { ...prev }
    if (Object.prototype.hasOwnProperty.call(patch, 'builtinRowActions'))
      next.builtinRowActions = Array.isArray(patch.builtinRowActions) ? patch.builtinRowActions : []
    if (Object.prototype.hasOwnProperty.call(patch, 'customActions'))
      next.customActions = Array.isArray(patch.customActions) ? patch.customActions : []
    if (id) {
      rowActionOverrides.value = {
        ...rowActionOverrides.value,
        [id]: next,
      }
    }
    latestRowActionOverride.value = {
      blockId: id || null,
      ...next,
    }
  }

  function getBlockRowActionOverride(blockId) {
    const id = String(blockId || '').trim()
    if (id && rowActionOverrides.value[id])
      return rowActionOverrides.value[id]
    // 预览块 id 对不上时，用最近一次属性面板写入的配置
    return latestRowActionOverride.value || null
  }

  function clearBlockRowActionOverride(blockId) {
    const id = String(blockId || '').trim()
    if (!id || !rowActionOverrides.value[id])
      return
    const next = { ...rowActionOverrides.value }
    delete next[id]
    rowActionOverrides.value = next
  }

  /** 拖拽/缩放开始：挂起 layout 提交，结束后由 flush 统一补发 */
  function beginDeferLayoutEmit() {
    deferLayoutEmit.value = true
    hasDeferredLayoutEmit.value = false
  }

  function markDeferredLayoutEmit() {
    if (deferLayoutEmit.value)
      hasDeferredLayoutEmit.value = true
  }

  /** 拖拽/缩放结束：解除挂起并返回是否存在被挂起的变更 */
  function takeDeferredLayoutEmit() {
    if (!deferLayoutEmit.value)
      return false
    deferLayoutEmit.value = false
    const pending = hasDeferredLayoutEmit.value
    hasDeferredLayoutEmit.value = false
    return pending
  }

  return {
    layout,
    selectedBlockId,
    propertyPanelTab,
    deferLayoutEmit,
    rowActionOverrides,
    latestRowActionOverride,
    blocks,
    selectedBlock,
    applyLayout,
    selectBlock,
    clearSelection,
    setPropertyTab,
    setBlockRowActionOverride,
    getBlockRowActionOverride,
    clearBlockRowActionOverride,
    beginDeferLayoutEmit,
    markDeferredLayoutEmit,
    takeDeferredLayoutEmit,
  }
})

// v2: layout/selectedBlockId/propertyPanelTab/defer 通信状态收敛（Pinia 桥接）
// v3: rowActionOverrides 权威缓存操作列配置，预览/回写不再丢隐藏删除与自定义按钮
