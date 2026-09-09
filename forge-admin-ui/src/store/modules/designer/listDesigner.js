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
 * 布局算法（normalizeDesignerLayout/syncGridLayoutWithModel）与 patch 类写操作
 * 仍由设计器组件负责，避免 store 承载过多业务算法。
 */
export const useListDesignerStore = defineStore('listDesigner', () => {
  const layout = ref({})
  const selectedBlockId = ref(null)
  const propertyPanelTab = ref('props')
  const deferLayoutEmit = ref(false)
  const hasDeferredLayoutEmit = ref(false)

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
    blocks,
    selectedBlock,
    applyLayout,
    selectBlock,
    clearSelection,
    setPropertyTab,
    beginDeferLayoutEmit,
    markDeferredLayoutEmit,
    takeDeferredLayoutEmit,
  }
})

// v2: layout/selectedBlockId/propertyPanelTab/defer 通信状态收敛（Pinia 桥接）
