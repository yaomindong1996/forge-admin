import { computed, ref } from 'vue'

/**
 * useNodeOverlay — 统一画布节点操作条的共用状态/交互逻辑
 * @description 供 ForgeFormCanvasNode 和 ListPageGridDesigner 等画布节点组件使用。
 *   封装选中态、hover 态、删除确认、操作按钮显隐等通用状态管理。
 *
 * @param {object} options
 * @param {import('vue').Ref<string>} options.selectedId - 当前选中的节点 ID（来自父级或全局状态）
 * @param {string | import('vue').Ref<string>} options.nodeId - 当前节点自身的 ID
 * @param {object} [options.confirmDelete] - 删除确认配置
 * @param {boolean} [options.confirmDelete.enabled] - 是否启用删除确认弹窗
 * @param {string} [options.confirmDelete.title] - 确认弹窗标题
 * @param {string} [options.confirmDelete.content] - 确认弹窗内容
 * @param {(id: string) => void} [options.onDelete] - 实际删除回调
 * @param {(id: string) => void} [options.onDuplicate] - 复制回调
 * @param {(id: string) => void} [options.onSelect] - 选中回调
 *
 * @returns {{
 *   isSelected: import('vue').ComputedRef<boolean>,
 *   hovered: import('vue').Ref<boolean>,
 *   overlayVisible: import('vue').ComputedRef<boolean>,
 *   setHovered: (v: boolean) => void,
 *   select: () => void,
 *   confirmAndDelete: () => Promise<void>,
 *   duplicate: () => void,
 * }}
 */
export function useNodeOverlay(options = {}) {
  const {
    selectedId,
    nodeId,
    confirmDelete = {},
    onDelete,
    onDuplicate,
    onSelect,
  } = options

  const hovered = ref(false)

  /** 当前节点是否被选中 */
  const isSelected = computed(() => {
    const id = typeof nodeId === 'string' ? nodeId : nodeId?.value
    const sel = selectedId?.value
    return !!id && id === sel
  })

  /** 操作条是否可见（hover 或选中时显示） */
  const overlayVisible = computed(() => hovered.value || isSelected.value)

  /** 设置 hover 状态 */
  function setHovered(v) {
    hovered.value = v
  }

  /** 触发选中 */
  function select() {
    const id = typeof nodeId === 'string' ? nodeId : nodeId?.value
    if (id && typeof onSelect === 'function') {
      onSelect(id)
    }
  }

  /** 删除确认 + 执行 */
  async function confirmAndDelete() {
    const id = typeof nodeId === 'string' ? nodeId : nodeId?.value
    if (!id)
      return

    if (confirmDelete.enabled) {
      try {
        const { useDialog } = await import('naive-ui')
        const dialog = useDialog()
        await new Promise((resolve, reject) => {
          dialog.warning({
            title: confirmDelete.title || '确认删除',
            content: confirmDelete.content || '删除后无法撤销，是否继续？',
            positiveText: '删除',
            negativeText: '取消',
            onPositiveClick: resolve,
            onNegativeClick: reject,
            onMaskClick: reject,
          })
        })
      }
      catch {
        return // 用户取消
      }
    }

    if (typeof onDelete === 'function') {
      onDelete(id)
    }
  }

  /** 复制 */
  function duplicate() {
    const id = typeof nodeId === 'string' ? nodeId : nodeId?.value
    if (id && typeof onDuplicate === 'function') {
      onDuplicate(id)
    }
  }

  return {
    isSelected,
    hovered,
    overlayVisible,
    setHovered,
    select,
    confirmAndDelete,
    duplicate,
  }
}
