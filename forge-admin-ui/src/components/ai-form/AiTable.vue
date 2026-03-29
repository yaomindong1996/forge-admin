<!--
  数据表格组件
  基于 Naive UI 的 n-data-table 封装
  参考 LxTable 设计，支持配置化列定义
  集成 AiTableFilter（列设置）和 AiToolbarAction（工具栏操作）
-->

<template>
  <div class="ai-table-wrapper">
    <!-- 工具栏 -->
    <div v-if="showToolbar" class="ai-table-toolbar">
      <div class="ai-table-toolbar-left">
        <slot name="toolbar-left" />
      </div>
      <div class="ai-table-toolbar-right">
        <slot name="toolbar-right" />
        <AiToolbarAction
          :columns="columns"
          :filter-max-height="filterMaxHeight"
          :default-checked-columns="defaultCheckedColumns"
          :show-refresh="showRefresh"
          :show-density="showDensity"
          :show-column-filter="showColumnFilter"
          :show-search-toggle="showSearchToggle"
          :show-fullscreen="showFullscreen"
          :search-visible="searchVisible"
          @refresh="handleRefresh"
          @density-change="handleDensityChange"
          @filter-change="handleFilterChange"
          @search-toggle="handleSearchToggle"
          @fullscreen-change="handleFullscreenChange"
        >
          <template #extra>
            <slot name="toolbar-extra" />
          </template>
        </AiToolbarAction>
      </div>
    </div>
    <!-- 数据表格 -->
    <n-data-table
      remote
      ref="tableRef"
      :columns="tableColumns"
      :data="dataSource"
      :loading="loading"
      :pagination="paginationProps"
      :row-key="rowKeyFn"
      :striped="striped"
      :bordered="bordered"
      :single-line="singleLine"
      :size="currentSize"
      :max-height="maxHeight"
      :scroll-x="scrollX"
      :checked-row-keys="checkedRowKeys"
      @update:checked-row-keys="handleUpdateCheckedKeys"
      v-bind="$attrs"
    >
      <template #empty>
        <n-empty description="暂无数据" />
      </template>
    </n-data-table>
  </div>
</template>

<script setup>
import { ref, computed, h, useSlots } from 'vue'
import { NButton, NSpace, NIcon, NEmpty } from 'naive-ui'
import AiToolbarAction from './AiToolbarAction.vue'

const slots = useSlots()

const props = defineProps({
  // 列配置
  columns: {
    type: Array,
    default: () => [],
    required: true
  },
  // 数据源
  dataSource: {
    type: Array,
    default: () => []
  },
  // 加载状态
  loading: {
    type: Boolean,
    default: false
  },
  // 分页配置
  pagination: {
    type: [Object, Boolean],
    default: () => ({
      page: 1,
      pageSize: 10,
      showSizePicker: true,
      pageSizes: [10, 20, 50, 100],
      showQuickJumper: true
    })
  },
  // 行键
  rowKey: {
    type: [String, Function],
    default: 'id'
  },
  // 是否显示斑马纹
  striped: {
    type: Boolean,
    default: false
  },
  // 是否显示边框
  bordered: {
    type: Boolean,
    default: true
  },
  // 是否单行
  singleLine: {
    type: Boolean,
    default: false
  },
  // 尺寸
  size: {
    type: String,
    default: 'small' // 'small' | 'medium' | 'large'
  },
  // 最大高度
  maxHeight: {
    type: [Number, String],
    default: undefined
  },
  // 横向滚动宽度
  scrollX: {
    type: Number,
    default: undefined
  },
  // 是否隐藏多选
  hideSelection: {
    type: Boolean,
    default: false
  },
  // 上下文对象（传递给插槽）
  context: {
    type: Object,
    default: () => ({})
  },

  // ========== 工具栏配置 ==========

  /**
   * 是否显示工具栏
   * @type {Boolean}
   */
  showToolbar: {
    type: Boolean,
    default: true
  },

  /**
   * 是否显示刷新按钮
   * @type {Boolean}
   */
  showRefresh: {
    type: Boolean,
    default: true
  },

  /**
   * 是否显示密度调整
   * @type {Boolean}
   */
  showDensity: {
    type: Boolean,
    default: true
  },

  /**
   * 是否显示列设置
   * @type {Boolean}
   */
  showColumnFilter: {
    type: Boolean,
    default: true
  },

  /**
   * 是否显示搜索切换按钮
   * @type {Boolean}
   */
  showSearchToggle: {
    type: Boolean,
    default: false
  },

  /**
   * 是否显示全屏按钮
   * @type {Boolean}
   */
  showFullscreen: {
    type: Boolean,
    default: false
  },

  /**
   * 列筛选下拉菜单最大高度
   * @type {String}
   */
  filterMaxHeight: {
    type: String,
    default: '400px'
  },

  /**
   * 默认选中的列（列的 key 或 prop）
   * @type {Array<String>}
   */
  defaultCheckedColumns: {
    type: Array,
    default: () => []
  },

  /**
   * 搜索表单是否可见（用于搜索切换）
   * @type {Boolean}
   */
  searchVisible: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits([
  'update:checked-row-keys',
  'page-change',
  'page-size-change',
  'refresh',
  'density-change',
  'filter-change',
  'search-toggle',
  'fullscreen-change'
])

const tableRef = ref(null)
const checkedRowKeys = ref([])

// 工具栏相关状态
const currentSize = ref(props.size)
const visibleColumns = ref([])

/**
 * 行键函数
 */
const rowKeyFn = computed(() => {
  if (typeof props.rowKey === 'function') {
    return props.rowKey
  }
  return (row) => row[props.rowKey]
})

/**
 * 转换列配置
 */
const tableColumns = computed(() => {
  // 使用筛选后的列或原始列
  const columnsToUse = visibleColumns.value.length > 0 ? visibleColumns.value : props.columns

  const cols = []

  // 多选列
  if (!props.hideSelection) {
    cols.push({
      type: 'selection',
      fixed: 'left'
    })
  }

  // 处理其他列
  columnsToUse.forEach(col => {
    // 如果设置了 visible: false，跳过
    if (col.visible === false) {
      return
    }

    const column = {
      key: col.prop || col.key,
      title: col.label || col.title,
      width: col.width,
      minWidth: col.minWidth,
      maxWidth: col.maxWidth,
      align: col.align || 'left',
      fixed: col.fixed,
      ellipsis: col.ellipsis !== false ? { tooltip: true } : false,
      sorter: col.sortable ? (row1, row2) => {
        const val1 = row1[col.prop]
        const val2 = row2[col.prop]
        if (typeof val1 === 'number' && typeof val2 === 'number') {
          return val1 - val2
        }
        return String(val1).localeCompare(String(val2))
      } : false,
      filter: col.filter,
      filterMultiple: col.filterMultiple,
      filterOptions: col.filterOptions
    }

    // 自定义渲染
    if (col.render) {
      column.render = (row, index) => {
        return col.render(row, index, props.context)
      }
    }
    // 格式化函数
    else if (col.formatter) {
      column.render = (row, index) => {
        return col.formatter(row, col, row[col.prop], index)
      }
    }
    // 插槽
    else if (col.slot || col._slot) {
      const slotName = col.slot || col._slot
      column.render = (row, index) => {
        // 尝试从父组件获取插槽
        const slotFn = slots[slotName]
        if (slotFn) {
          // 如果插槽存在，渲染插槽内容
          return slotFn({
            row,
            index,
            column: col,
            context: props.context
          })
        }
        // 如果插槽不存在，显示占位文本
        return h('div', {
          class: 'table-slot-wrapper',
          style: {
            color: '#999',
            fontSize: '12px'
          }
        }, `[插槽: ${slotName}]`)
      }
    }
    // 默认显示
    else {
      column.render = (row) => {
        return row[col.prop] ?? '-'
      }
    }

    cols.push(column)
  })

  return cols
})

/**
 * 处理刷新
 */
function handleRefresh() {
  emit('refresh')
}

/**
 * 处理密度变化
 */
function handleDensityChange(size) {
  currentSize.value = size
  emit('density-change', size)
}

/**
 * 处理列筛选变化
 */
function handleFilterChange(columns) {
  visibleColumns.value = columns
  emit('filter-change', columns)
}

/**
 * 处理搜索切换
 */
function handleSearchToggle(visible) {
  emit('search-toggle', visible)
}

/**
 * 处理全屏变化
 */
function handleFullscreenChange(isFullscreen) {
  emit('fullscreen-change', isFullscreen)
}

/**
 * 分页配置
 */
const paginationProps = computed(() => {
  if (props.pagination === false) {
    return false
  }

  const config = {
    ...props.pagination,
    onChange: (page) => {
      emit('page-change', page)
    },
    onUpdatePageSize: (pageSize) => {
      emit('page-size-change', pageSize)
    }
  }
  return config
})

/**
 * 更新选中的行
 */
function handleUpdateCheckedKeys(keys) {
  checkedRowKeys.value = keys
  emit('update:checked-row-keys', keys)
}

/**
 * 清除选择
 */
function clearSelection() {
  checkedRowKeys.value = []
  emit('update:checked-row-keys', [])
}

/**
 * 获取选中的行
 */
function getCheckedRows() {
  return props.dataSource.filter(row => {
    const key = rowKeyFn.value(row)
    return checkedRowKeys.value.includes(key)
  })
}

/**
 * 设置选中的行
 */
function setCheckedKeys(keys) {
  checkedRowKeys.value = keys
  emit('update:checked-row-keys', keys)
}

// 暴露方法
defineExpose({
  clearSelection,
  getCheckedRows,
  setCheckedKeys,
  checkedRowKeys,
  handleRefresh,
  handleDensityChange,
  handleFilterChange
})
</script>

<style scoped>
.ai-table-wrapper {
  width: 100%;
}

.ai-table-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  border-bottom: 1px solid var(--border-light);
  background: var(--bg-secondary);
  min-height: 50px;
}

.ai-table-toolbar-left,
.ai-table-toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.table-slot-wrapper {
  color: var(--text-tertiary);
  font-style: italic;
  font-size: var(--font-size-xs);
}

/* 分页器样式 */
:deep(.n-pagination) {
  padding: 12px 16px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
  border-top: 1px solid var(--border-light);
}

/* 表格层 hover 效果增强 */
:deep(.n-data-table-tr:hover .n-data-table-td) {
  background: var(--bg-secondary) !important;
}

/* 头部单元格样式 */
:deep(.n-data-table-th) {
  background: var(--bg-secondary) !important;
  font-weight: var(--font-weight-semibold);
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  white-space: nowrap;
}

/* 小屏幕适配 */
@media (max-width: 768px) {
  :deep(.n-pagination) {
    justify-content: center;
    padding: 12px;
  }

  :deep(.n-pagination .n-pagination-item) {
    min-width: 32px;
    height: 32px;
    font-size: 13px;
  }

  :deep(.n-pagination .n-pagination-item__button) {
    padding: 0 6px;
  }

  :deep(.n-pagination-size-picker) {
    font-size: 13px;
  }

  :deep(.n-pagination-quick-jumper) {
    font-size: 13px;
  }
}

@media (max-width: 576px) {
  :deep(.n-pagination) {
    padding: 10px;
    gap: 4px;
  }

  :deep(.n-pagination .n-pagination-item) {
    min-width: 28px;
    height: 28px;
    font-size: 12px;
  }

  :deep(.n-pagination .n-pagination-item__button) {
    padding: 0 4px;
  }
}
</style>
