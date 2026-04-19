<!--
  表格列筛选组件
  用于控制表格列的显示/隐藏
  参考 nmTableFilter 设计

  @author AI Form Team
  @version 1.0.0
-->

<template>
  <n-popover
    trigger="click"
    placement="bottom-end"
    :show-arrow="false"
    style="padding: 0"
  >
    <template #trigger>
      <n-button text>
        <template #icon>
          <n-icon size="18">
            <SettingsOutline />
          </n-icon>
        </template>
      </n-button>
    </template>

    <div class="ai-table-filter-dropdown">
      <div class="ai-table-filter-header">
        <span>列设置</span>
        <n-button text size="small" @click="handleReset">
          重置
        </n-button>
      </div>

      <n-scrollbar :style="{ maxHeight: filterMaxHeight }">
        <n-checkbox-group v-model:value="checkedColumns" @update:value="handleChange">
          <transition-group name="drag-list" tag="div" style="padding: 12px">
            <div
              v-for="(column, index) in sortedColumns"
              :key="column.key || column.prop"
              class="column-item"
              :class="{
                'dragging': draggingIndex === index,
                'drag-over': dragOverIndex === index && draggingIndex !== index,
              }"
              draggable="true"
              @dragstart="handleDragStart(index, $event)"
              @dragover="handleDragOver(index, $event)"
              @dragenter="handleDragEnter(index)"
              @dragleave="handleDragLeave"
              @dragend="handleDragEnd"
              @drop="handleDrop(index, $event)"
            >
              <n-icon class="drag-handle" size="16">
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24">
                  <path fill="currentColor" d="M9 3h2v2H9V3m4 0h2v2h-2V3M9 7h2v2H9V7m4 0h2v2h-2V7m-4 4h2v2H9v-2m4 0h2v2h-2v-2m-4 4h2v2H9v-2m4 0h2v2h-2v-2m-4 4h2v2H9v-2m4 0h2v2h-2v-2Z" />
                </svg>
              </n-icon>
              <n-checkbox
                :value="column.key || column.prop"
                :disabled="column.disabled"
              >
                {{ column.label || column.title }}
              </n-checkbox>
            </div>
          </transition-group>
        </n-checkbox-group>
      </n-scrollbar>
    </div>
  </n-popover>
</template>

<script setup>
import { SettingsOutline } from '@vicons/ionicons5'
import { computed, ref, watch } from 'vue'

/**
 * Props 定义
 */
const props = defineProps({
  /**
   * 表格列配置
   * @type {Array<object>}
   */
  columns: {
    type: Array,
    default: () => [],
  },

  /**
   * 下拉菜单最大高度
   * @type {string}
   */
  filterMaxHeight: {
    type: String,
    default: '400px',
  },

  /**
   * 默认选中的列（列的 key 或 prop）
   * @type {Array<string>}
   */
  defaultChecked: {
    type: Array,
    default: () => [],
  },
})

/**
 * Emits 定义
 */
const emit = defineEmits(['filter-change', 'update:checked-columns', 'column-order-change'])

/**
 * 选中的列
 */
const checkedColumns = ref([])

/**
 * 排序后的列
 */
const sortedColumns = ref([])

/**
 * 拖拽相关状态
 */
const draggingIndex = ref(null)
const dragOverIndex = ref(null)

/**
 * 可筛选的列（排除操作列、选择列等）
 */
const filterableColumns = computed(() => {
  return props.columns.filter((col) => {
    // 排除操作列
    if (col.prop === 'action' || col.key === 'action') {
      return false
    }
    // 排除选择列
    if (col.type === 'selection') {
      return false
    }
    // 排除明确设置不可筛选的列
    if (col.filterable === false) {
      return false
    }
    return true
  })
})

/**
 * 初始化选中的列
 */
function initCheckedColumns() {
  if (props.defaultChecked && props.defaultChecked.length > 0) {
    checkedColumns.value = [...props.defaultChecked]
  }
  else {
    // 默认选中所有非隐藏的列
    checkedColumns.value = filterableColumns.value
      .filter(col => col.hidden !== true && col.visible !== false)
      .map(col => col.key || col.prop)
  }
  // 初始化排序列表
  sortedColumns.value = [...filterableColumns.value]
}

/**
 * 监听列配置变化
 */
watch(() => props.columns, () => {
  initCheckedColumns()
}, { immediate: true })

/**
 * 拖拽开始
 */
function handleDragStart(index, event) {
  draggingIndex.value = index
  event.dataTransfer.effectAllowed = 'move'
  event.dataTransfer.setData('text/html', event.target.innerHTML)
}

/**
 * 拖拽经过
 */
function handleDragOver(index, event) {
  event.preventDefault()
  event.dataTransfer.dropEffect = 'move'
}

/**
 * 拖拽进入
 */
function handleDragEnter(index) {
  if (draggingIndex.value !== index) {
    dragOverIndex.value = index
  }
}

/**
 * 拖拽离开
 */
function handleDragLeave() {
  dragOverIndex.value = null
}

/**
 * 拖拽结束
 */
function handleDragEnd() {
  draggingIndex.value = null
  dragOverIndex.value = null
}

/**
 * 放置
 */
function handleDrop(dropIndex, event) {
  event.preventDefault()

  if (draggingIndex.value === null || draggingIndex.value === dropIndex) {
    return
  }

  const newColumns = [...sortedColumns.value]
  const draggedItem = newColumns[draggingIndex.value]

  // 移除拖拽的项
  newColumns.splice(draggingIndex.value, 1)
  // 插入到新位置
  newColumns.splice(dropIndex, 0, draggedItem)

  sortedColumns.value = newColumns

  // 触发排序变化事件
  emit('column-order-change', newColumns)

  // 重新触发筛选变化
  handleChange(checkedColumns.value)
}

/**
 * 处理选中变化
 */
function handleChange(values) {
  // 根据排序后的列顺序重新组织可见列
  const sortedVisibleColumns = []
  const actionColumns = []
  const selectionColumns = []

  // 分离选择列、操作列和其他非可筛选列
  props.columns.forEach((col) => {
    const isFilterable = filterableColumns.value.some(fc => (fc.key || fc.prop) === (col.key || col.prop))
    if (!isFilterable) {
      // 选择列放在最前面
      if (col.type === 'selection') {
        selectionColumns.push(col)
      }
      // 操作列放在最后面
      else if (col.prop === 'action' || col.key === 'action') {
        actionColumns.push(col)
      }
      // 其他非可筛选列保持原始位置
      else {
        sortedVisibleColumns.push(col)
      }
    }
  })

  // 按照排序后的顺序添加可筛选的列
  sortedColumns.value.forEach((sortedCol) => {
    if (values.includes(sortedCol.key || sortedCol.prop)) {
      const originalCol = props.columns.find(col =>
        (col.key || col.prop) === (sortedCol.key || sortedCol.prop),
      )
      if (originalCol) {
        sortedVisibleColumns.push(originalCol)
      }
    }
  })

  // 组合最终顺序：选择列 -> 其他列 -> 操作列
  const finalColumns = [...selectionColumns, ...sortedVisibleColumns, ...actionColumns]

  emit('filter-change', finalColumns)
  emit('update:checked-columns', values)
}

/**
 * 重置
 */
function handleReset() {
  // 重置选中状态
  checkedColumns.value = filterableColumns.value.map(col => col.key || col.prop)
  // 重置排序
  sortedColumns.value = [...filterableColumns.value]
  handleChange(checkedColumns.value)
}

/**
 * 暴露方法
 */
defineExpose({
  /**
   * 重置筛选
   */
  reset: handleReset,

  /**
   * 获取选中的列
   */
  getCheckedColumns: () => checkedColumns.value,

  /**
   * 设置选中的列
   */
  setCheckedColumns: (columns) => {
    checkedColumns.value = columns
    handleChange(columns)
  },
})
</script>

<style scoped>
.ai-table-filter-dropdown {
  min-width: 200px;
  background: var(--n-color);
}

.ai-table-filter-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--n-divider-color);
  font-weight: 500;
}

.column-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  margin-bottom: 4px;
  border-radius: 4px;
  cursor: move;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  user-select: none;
}

.column-item:hover {
  background-color: var(--n-color-hover);
  transform: translateX(2px);
}

.column-item.dragging {
  opacity: 0.4;
  background-color: var(--n-color-pressed);
  transform: scale(0.95);
}

.column-item.drag-over {
  border-top: 2px solid var(--n-primary-color);
  padding-top: 8px;
}

.drag-handle {
  color: var(--n-text-color-disabled);
  cursor: grab;
  flex-shrink: 0;
  transition: color 0.2s;
}

.column-item:hover .drag-handle {
  color: var(--n-text-color);
}

.drag-handle:active {
  cursor: grabbing;
}

.column-item :deep(.n-checkbox) {
  flex: 1;
}

/* 拖拽列表动画 */
.drag-list-move {
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.drag-list-enter-active,
.drag-list-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.drag-list-enter-from {
  opacity: 0;
  transform: translateX(-20px);
}

.drag-list-leave-to {
  opacity: 0;
  transform: translateX(20px);
}
</style>
