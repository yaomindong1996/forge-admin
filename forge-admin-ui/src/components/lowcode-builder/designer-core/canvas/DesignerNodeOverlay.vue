<script setup>
/**
 * DesignerNodeOverlay — 统一画布节点操作条（P1 核心）
 * @description 表单设计器和列表设计器共用的 hover 操作条组件。
 *   统一颜色体系（主题色 #2563eb / hover 浅蓝 #93c5fd）、统一按钮风格、统一交互语义。
 *   布局统一为：拖拽把手居中顶部、更多菜单右上角、快捷操作组右侧（留空间给菜单）。
 *   mode=overlay：表单字段操作条（必填/复制/删除/更多/拖拽把手）
 *   mode=column：栅格格子操作条（仅删除，贴右上角）
 *   mode=block：列表画布 block 操作条（拖拽把手居中顶部、更多菜单右上角、删除走菜单）
 *   各设计器可通过 #extra-actions 插槽注入特有操作按钮。
 */
import { NDropdown } from 'naive-ui'
import { computed } from 'vue'
import './node-overlay.css'

const props = defineProps({
  /** 操作条模式：overlay（表单字段） / column（栅格格子） / block（列表 block） */
  mode: {
    type: String,
    default: 'overlay',
    validator: v => ['overlay', 'column', 'block'].includes(v),
  },
  /** 操作条是否可见（JS 控制；也可由父级 CSS hover/selected 驱动） */
  visible: { type: Boolean, default: false },
  /** 是否显示必填开关（仅 overlay 模式有效） */
  showRequiredSwitch: { type: Boolean, default: false },
  /** 当前必填状态 */
  required: { type: Boolean, default: false },
  /** 是否显示复制按钮（仅 overlay 模式有效） */
  showDuplicate: { type: Boolean, default: true },
  /** 是否显示删除按钮（overlay/column 模式有效；block 模式的删除走更多菜单） */
  showDelete: { type: Boolean, default: true },
  /** 删除按钮是否禁用 */
  deleteDisabled: { type: Boolean, default: false },
  /** 删除按钮 title 提示 */
  deleteTitle: { type: String, default: '删除（字段资产保留，可从左侧重新拖入）' },
  /** 是否显示拖拽把手 */
  showDragHandle: { type: Boolean, default: true },
  /** 拖拽把手 title 提示 */
  dragTitle: { type: String, default: '拖动排序' },
  /** 更多菜单选项（NDropdown options 格式） */
  menuOptions: { type: Array, default: () => [] },
  /** 是否显示更多菜单 */
  showMenu: { type: Boolean, default: true },
  /** 复制按钮 title */
  duplicateTitle: { type: String, default: '复制' },
})

const emit = defineEmits([
  'toggleRequired',
  'duplicate',
  'delete',
  'dragStart',
  'menuSelect',
])

const overlayClasses = computed(() => [
  'designer-node-overlay',
  `mode-${props.mode}`,
  { 'is-visible': props.visible },
])

/** overlay/column 模式下，快捷操作组是否有任何按钮 */
const hasQuickActions = computed(() => {
  if (props.mode === 'block')
    return false
  return props.showRequiredSwitch || props.showDuplicate || props.showDelete
})

function handleMenuSelect(key) {
  emit('menuSelect', key)
}
</script>

<template>
  <div :class="overlayClasses">
    <!-- 快捷操作组（overlay / column 模式：按钮群在右侧，为菜单留空间） -->
    <div v-if="hasQuickActions" class="overlay-quick-actions">
      <!-- 必填开关 -->
      <button
        v-if="showRequiredSwitch && mode === 'overlay'"
        type="button"
        class="overlay-required-switch"
        :class="{ active: required }"
        role="switch"
        :aria-checked="String(required)"
        title="必填"
        @click.stop="emit('toggleRequired')"
        @pointerdown.stop
      >
        <span>必填</span>
        <span class="overlay-switch-track">
          <span class="overlay-switch-thumb" />
        </span>
      </button>

      <!-- 复制按钮 -->
      <button
        v-if="showDuplicate && mode === 'overlay'"
        type="button"
        class="overlay-icon-action"
        :title="duplicateTitle"
        @click.stop="emit('duplicate')"
        @pointerdown.stop
      >
        <svg width="1em" height="1em" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" data-icon="CopyOutlined">
          <path d="M9 3a1 1 0 0 1 1-1h10a1 1 0 0 1 1 1v12a1 1 0 1 1-2 0V4h-9a1 1 0 0 1-1-1Z" fill="currentColor" />
          <path d="M5 6a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2H5Zm0 2h10v12H5V8Z" fill="currentColor" />
        </svg>
      </button>

      <!-- 删除按钮 -->
      <button
        v-if="showDelete"
        type="button"
        class="overlay-icon-action danger"
        :disabled="deleteDisabled"
        :title="deleteTitle"
        @click.stop="emit('delete')"
        @pointerdown.stop
      >
        <svg width="1em" height="1em" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" data-icon="DeleteTrashOutlined">
          <path d="M8 4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2h5a1 1 0 1 1 0 2h-1v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6H3a1 1 0 0 1 0-2h5ZM6 6v14h12V6H6Zm4 3a1 1 0 0 1 1 1v6a1 1 0 1 1-2 0v-6a1 1 0 0 1 1-1Zm4 0a1 1 0 0 1 1 1v6a1 1 0 1 1-2 0v-6a1 1 0 0 1 1-1Z" fill="currentColor" />
        </svg>
      </button>

      <!-- 额外操作插槽 -->
      <slot name="extra-actions" />
    </div>

    <!-- 更多菜单（右上角） -->
    <NDropdown
      v-if="showMenu && mode !== 'column'"
      trigger="click"
      placement="bottom-end"
      :options="menuOptions"
      @select="handleMenuSelect"
    >
      <button
        type="button"
        class="overlay-menu-trigger"
        title="更多操作"
        @click.stop
        @pointerdown.stop
      >
        <svg width="1em" height="1em" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" data-icon="MoreVerticalOutlined">
          <path d="M12 5.5A1.75 1.75 0 1 1 12 2a1.75 1.75 0 0 1 0 3.5Zm0 8.225a1.75 1.75 0 1 1 0-3.5 1.75 1.75 0 0 1 0 3.5ZM12 22a1.75 1.75 0 1 1 0-3.5 1.75 1.75 0 0 1 0 3.5Z" fill="currentColor" />
        </svg>
      </button>
    </NDropdown>

    <!-- 拖拽把手（居中顶部） -->
    <span
      v-if="showDragHandle && mode !== 'column'"
      class="overlay-drag-handle"
      :title="dragTitle"
      @pointerdown.stop="emit('dragStart', $event)"
      @click.stop
    >
      <svg
        width="1em"
        height="1em"
        viewBox="0 0 24 24"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
        data-icon="DragOutlined"
        class="overlay-drag-handle-svg"
      >
        <path
          d="M8.25 6.5a1.75 1.75 0 1 0 0-3.5 1.75 1.75 0 0 0 0 3.5Zm0 7.25a1.75 1.75 0 1 0 0-3.5 1.75 1.75 0 0 0 0 3.5Zm1.75 5.5a1.75 1.75 0 1 1-3.5 0 1.75 1.75 0 0 1 3.5 0ZM14.753 6.5a1.75 1.75 0 1 0 0-3.5 1.75 1.75 0 0 0 0 3.5ZM16.5 12a1.75 1.75 0 1 1-3.5 0 1.75 1.75 0 0 1 3.5 0Zm-1.747 9a1.75 1.75 0 1 0 0-3.5 1.75 1.75 0 0 0 0 3.5Z"
          fill="currentColor"
        />
      </svg>
    </span>
  </div>
</template>
