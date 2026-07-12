<!--
  数据表格组件
  基于 Naive UI 的 n-data-table 封装
  参考 LxTable 设计，支持配置化列定义
  集成 AiTableFilter（列设置）和 AiToolbarAction（工具栏操作）
-->

<template>
  <div class="ai-table-wrapper" :class="`ai-table-density-${currentSize}`">
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
          :show-render-mode-switch="showRenderModeSwitch"
          :search-visible="searchVisible"
          :render-mode="currentRenderMode"
          @refresh="handleRefresh"
          @density-change="handleDensityChange"
          @filter-change="handleFilterChange"
          @search-toggle="handleSearchToggle"
          @fullscreen-change="handleFullscreenChange"
          @render-mode-change="handleRenderModeChange"
        >
          <template #extra>
            <slot name="toolbar-extra" />
          </template>
        </AiToolbarAction>
      </div>
    </div>
    <!-- 数据表格 -->
    <n-data-table
      v-if="currentRenderMode === 'table'"
      ref="tableRef"
      v-table-scroll-enhance="dragScroll"
      remote
      :data-table-drag-scroll="dragScroll ? 'enabled' : 'disabled'"
      :columns="tableColumns"
      :data="dataSource"
      :loading="loading"
      :pagination="paginationProps"
      :row-key="rowKeyFn"
      :striped="striped"
      :bordered="bordered"
      :single-line="singleLine"
      :size="currentSize"
      flex-height
      :max-height="maxHeight"
      :scroll-x="scrollX"
      :checked-row-keys="innerCheckedRowKeys"
      :expanded-row-keys="expandedRowKeys"
      v-bind="$attrs"
      @update:checked-row-keys="handleUpdateCheckedKeys"
      @update:expanded-row-keys="handleUpdateExpandedKeys"
    >
      <template #empty>
        <NEmpty class="ai-table-empty-state" :description="emptyTitle" size="large">
          <template #extra>
            <div class="ai-table-empty-desc">
              {{ emptyDescription }}
            </div>
          </template>
        </NEmpty>
      </template>
    </n-data-table>

    <!-- 卡片列表 -->
    <div
      v-else
      class="ai-card-mode"
      :style="cardModeStyle"
    >
      <NSpin :show="loading">
        <NGrid
          v-if="dataSource.length > 0"
          class="ai-card-grid"
          :cols="cardGridCols"
          :x-gap="cardGridGap"
          :y-gap="cardGridGap"
          responsive="screen"
        >
          <NGridItem
            v-for="(row, index) in dataSource"
            :key="rowKeyFn(row)"
            class="ai-card-item"
            :class="{ 'is-checked': isRowChecked(row) }"
            @click="handleCardClick(row, index)"
            @mousemove="handleCardMouseMove"
            @mouseleave="handleCardMouseLeave"
          >
            <slot
              name="card"
              :row="row"
              :index="index"
              :columns="cardContentColumns"
              :action-column="cardActionColumn"
              :checked="isRowChecked(row)"
              :toggle-checked="checked => setRowChecked(row, checked)"
              :context="context"
            >
              <div class="ai-card-default">
                <div class="ai-card-header">
                  <NCheckbox
                    v-if="!hideSelection"
                    :checked="isRowChecked(row)"
                    @click.stop
                    @update:checked="checked => setRowChecked(row, checked)"
                  />
                  <div class="ai-card-title">
                    <RenderCell
                      v-if="cardTitleColumn"
                      :render="() => renderCardCell(row, cardTitleColumn, index)"
                    />
                    <span v-else>-</span>
                  </div>
                </div>

                <div class="ai-card-body">
                  <div
                    v-for="column in cardBodyColumns"
                    :key="column.key"
                    class="ai-card-field"
                  >
                    <span class="ai-card-field-label">{{ column.title }}</span>
                    <span class="ai-card-field-value">
                      <RenderCell :render="() => renderCardCell(row, column, index)" />
                    </span>
                  </div>
                </div>

                <div v-if="cardActionColumn" class="ai-card-footer" @click.stop>
                  <RenderCell :render="() => renderCardCell(row, cardActionColumn, index)" />
                </div>
              </div>
            </slot>
          </NGridItem>
        </NGrid>

        <NEmpty v-else class="ai-card-empty ai-table-empty-state" :description="emptyTitle" size="large">
          <template #extra>
            <div class="ai-table-empty-desc">
              {{ emptyDescription }}
            </div>
          </template>
        </NEmpty>
      </NSpin>

      <NPagination
        v-if="paginationProps"
        class="ai-card-pagination"
        v-bind="paginationProps"
      />
    </div>
  </div>
</template>

<script setup>
/* eslint-disable vue/custom-event-name-casing */
import { NCheckbox, NEmpty, NGrid, NGridItem, NPagination, NSpin } from 'naive-ui'
import { computed, h, ref, useSlots, watch } from 'vue'
import AiToolbarAction from './AiToolbarAction.vue'

const props = defineProps({
  // 列配置
  columns: {
    type: Array,
    default: () => [],
  },
  // 数据源
  dataSource: {
    type: Array,
    default: () => [],
  },
  // 加载状态
  loading: {
    type: Boolean,
    default: false,
  },
  // 分页配置
  pagination: {
    type: [Object, Boolean],
    default: () => ({
      page: 1,
      pageSize: 10,
      showSizePicker: true,
      pageSizes: [10, 20, 50, 100],
      showQuickJumper: true,
    }),
  },
  // 行键
  rowKey: {
    type: [String, Function],
    default: 'id',
  },
  // 是否显示斑马纹
  striped: {
    type: Boolean,
    default: false,
  },
  // 是否显示边框
  bordered: {
    type: Boolean,
    default: false,
  },
  // 是否单行
  singleLine: {
    type: Boolean,
    default: true,
  },
  // 尺寸
  size: {
    type: String,
    default: 'small', // 'small' | 'medium' | 'large'
  },
  // 渲染模式
  renderMode: {
    type: String,
    default: 'table',
    validator: value => ['table', 'card'].includes(value),
  },
  // 卡片渲染配置
  cardProps: {
    type: Object,
    default: () => ({}),
  },
  // 最大高度
  maxHeight: {
    type: [Number, String],
    default: undefined,
  },
  // 横向滚动宽度
  scrollX: {
    type: Number,
    default: undefined,
  },
  // 是否开启表格横向拖拽滚动
  dragScroll: {
    type: Boolean,
    default: true,
  },
  // 是否隐藏多选
  hideSelection: {
    type: Boolean,
    default: false,
  },
  checkedRowKeys: {
    type: Array,
    default: () => [],
  },
  expandedRowKeys: {
    type: Array,
    default: () => [],
  },
  resizable: {
    type: Boolean,
    default: false,
  },
  // 上下文对象（传递给插槽）
  context: {
    type: Object,
    default: () => ({}),
  },

  // ========== 工具栏配置 ==========

  /**
   * 是否显示工具栏
   * @type {boolean}
   */
  showToolbar: {
    type: Boolean,
    default: true,
  },

  /**
   * 是否显示刷新按钮
   * @type {boolean}
   */
  showRefresh: {
    type: Boolean,
    default: true,
  },

  /**
   * 是否显示密度调整
   * @type {boolean}
   */
  showDensity: {
    type: Boolean,
    default: true,
  },

  /**
   * 是否显示列设置
   * @type {boolean}
   */
  showColumnFilter: {
    type: Boolean,
    default: true,
  },

  /**
   * 是否显示搜索切换按钮
   * @type {boolean}
   */
  showSearchToggle: {
    type: Boolean,
    default: false,
  },

  /**
   * 是否显示全屏按钮
   * @type {boolean}
   */
  showFullscreen: {
    type: Boolean,
    default: false,
  },
  /**
   * 是否显示列表/卡片切换
   * @type {boolean}
   */
  showRenderModeSwitch: {
    type: Boolean,
    default: true,
  },

  /**
   * 列筛选下拉菜单最大高度
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
  defaultCheckedColumns: {
    type: Array,
    default: () => [],
  },

  /**
   * 搜索表单是否可见（用于搜索切换）
   * @type {boolean}
   */
  searchVisible: {
    type: Boolean,
    default: true,
  },
  emptyTitle: {
    type: String,
    default: '暂无数据',
  },
  emptyDescription: {
    type: String,
    default: '当前列表还没有可展示的记录。',
  },
})

const emit = defineEmits([
  'update:checked-row-keys',
  'update:expanded-row-keys',
  'page-change',
  'page-size-change',
  'refresh',
  'density-change',
  'filter-change',
  'search-toggle',
  'fullscreen-change',
  'render-mode-change',
])

const slots = useSlots()

const tableRef = ref(null)
const innerCheckedRowKeys = ref([...props.checkedRowKeys])

// 工具栏相关状态
const currentSize = ref(normalizeTableSize(props.size))
const currentRenderMode = ref(props.renderMode)
const visibleColumns = ref([])

const DEFAULT_TEXT_COLUMN_ALIGN = 'left'
const SELECTION_COLUMN_WIDTH = 48
const CENTER_COLUMN_KEYS = new Set([
  'status',
  'state',
  'enabled',
  'enabledMark',
  'enabled_mark',
  'type',
  'category',
  'gender',
  'sex',
  'userType',
  'userStatus',
  'postType',
  'postStatus',
  'orgType',
  'orgStatus',
  'captchaType',
  'concurrentLogin',
  'shareToken',
  'visible',
  'isDefault',
  'defaultFlag',
])
const CENTER_COLUMN_TITLES = new Set([
  '状态',
  '类型',
  '分类',
  '性别',
  '用户类型',
  '岗位类型',
  '组织类型',
  '验证码覆盖',
  '并发登录',
  '共享Token',
  '是否默认',
  '默认',
  '显示',
  '隐藏',
])
const RIGHT_COLUMN_KEYS = new Set([
  'sort',
  'orderNo',
  'orderNum',
  'order_num',
  'count',
  'total',
  'amount',
  'price',
  'size',
  'fileSize',
  'tokenTimeout',
  'tokenActivityTimeout',
])
const RIGHT_COLUMN_TITLES = new Set([
  '排序',
  '数量',
  '次数',
  '金额',
  '价格',
  '大小',
  'Token有效期',
  'Token活跃超时',
])

const RenderCell = props => props.render?.() ?? null

watch(
  () => props.checkedRowKeys,
  (keys = []) => {
    innerCheckedRowKeys.value = [...keys]
  },
)

watch(
  () => props.renderMode,
  (mode) => {
    currentRenderMode.value = mode || 'table'
  },
)

watch(
  () => props.size,
  (size) => {
    currentSize.value = normalizeTableSize(size)
  },
)

/**
 * 行键函数
 */
const rowKeyFn = computed(() => {
  if (typeof props.rowKey === 'function') {
    return props.rowKey
  }
  return row => row[props.rowKey]
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
      fixed: 'left',
      width: SELECTION_COLUMN_WIDTH,
    })
  }

  // 处理其他列
  columnsToUse.forEach((col) => {
    // 如果设置了 visible: false，跳过
    if (col.visible === false) {
      return
    }

    const actionColumn = isActionColumn(col)
    const resolvedFixed = shouldDefaultRightFixed(col) ? 'right' : col.fixed
    const columnAlign = col.align || inferColumnAlign(col, actionColumn)
    const columnTitleAlign = col.titleAlign || col.headerAlign || columnAlign

    const column = {
      key: col.prop || col.key || col.dataIndex || (actionColumn ? 'action' : undefined),
      title: col.label || col.title || (actionColumn ? '操作' : undefined),
      width: col.width,
      minWidth: col.minWidth,
      maxWidth: col.maxWidth,
      align: columnAlign,
      titleAlign: columnTitleAlign,
      fixed: resolvedFixed,
      className: mergeColumnClassName(
        col.className,
        actionColumn && resolvedFixed === 'right' ? 'forge-table-action-column' : undefined,
      ),
      type: col.type,
      resizable: col.resizable ?? props.resizable,
      ellipsis: col.ellipsis !== false ? { tooltip: true } : false,
      sorter: col.sortable
        ? (row1, row2) => {
            const val1 = row1[col.prop]
            const val2 = row2[col.prop]
            if (typeof val1 === 'number' && typeof val2 === 'number') {
              return val1 - val2
            }
            return String(val1).localeCompare(String(val2))
          }
        : false,
      filter: col.filter,
      filterMultiple: col.filterMultiple,
      filterOptions: col.filterOptions,
    }

    if (col.type === 'expand') {
      column.renderExpand = col.renderExpand
      column.expandable = col.expandable
      column.width = col.width || 48
      column.fixed = col.fixed || 'left'
      cols.push(column)
      return
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
            context: props.context,
          })
        }
        // 如果插槽不存在，显示占位文本
        return h('div', {
          class: 'table-slot-wrapper',
          style: {
            color: '#999',
            fontSize: '12px',
          },
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

function isActionColumn(column) {
  const key = String(column?.key || column?.prop || column?.dataIndex || '').trim()
  const title = String(column?.label || column?.title || '').trim()
  return ['action', 'actions', 'operation', 'operations'].includes(key) || title === '操作'
}

function inferColumnAlign(column, actionColumn = false) {
  if (actionColumn || column?.type === 'selection' || column?.type === 'expand')
    return 'center'

  const key = String(column?.key || column?.prop || column?.dataIndex || '').trim()
  const title = String(column?.label || column?.title || '').trim()

  if (RIGHT_COLUMN_KEYS.has(key) || RIGHT_COLUMN_TITLES.has(title))
    return 'right'

  if (CENTER_COLUMN_KEYS.has(key) || CENTER_COLUMN_TITLES.has(title))
    return 'center'

  return DEFAULT_TEXT_COLUMN_ALIGN
}

function shouldDefaultRightFixed(column) {
  return isActionColumn(column)
    && (column.fixed === undefined || column.fixed === null || column.fixed === '')
}

function mergeColumnClassName(className, extraClassName) {
  if (!extraClassName) {
    return className
  }
  if (!className) {
    return extraClassName
  }
  return Array.isArray(className)
    ? [...className, extraClassName]
    : [className, extraClassName]
}

function normalizeCssUnit(value) {
  if (value === undefined || value === null || value === '')
    return undefined
  if (typeof value === 'number')
    return `${value}px`
  return String(value)
}

const cardModeStyle = computed(() => {
  const style = { ...(props.cardProps.style || {}) }
  const maxHeight = props.cardProps.maxHeight ?? props.maxHeight
  if (maxHeight !== undefined) {
    style.maxHeight = normalizeCssUnit(maxHeight)
    style.overflow = style.overflow || 'auto'
  }
  return style
})

const cardGridCols = computed(() => props.cardProps.cols || props.cardProps.gridCols || '1 s:2 m:3 l:4 xl:4 2xl:4')
const cardGridGap = computed(() => props.cardProps.gap ?? 10)

const cardActionColumn = computed(() => tableColumns.value.find(isActionColumn))

const cardContentColumns = computed(() => {
  return tableColumns.value.filter(column => column.type !== 'selection' && !isActionColumn(column))
})

const cardTitleColumn = computed(() => {
  const titleKey = props.cardProps.titleKey || props.cardProps.titleField
  if (titleKey) {
    const matchedColumn = cardContentColumns.value.find(column => column.key === titleKey)
    if (matchedColumn)
      return matchedColumn
  }
  return cardContentColumns.value[0]
})

const cardBodyColumns = computed(() => {
  const limit = props.cardProps.fieldLimit ?? 4
  return cardContentColumns.value
    .filter(column => column.key !== cardTitleColumn.value?.key)
    .slice(0, limit)
})

function renderCardCell(row, column, index) {
  if (column?.render)
    return column.render(row, index)
  const value = row?.[column?.key]
  return value ?? '-'
}

function normalizeTableSize(size) {
  if (['small', 'medium', 'large'].includes(size))
    return size
  return 'medium'
}

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
  const nextSize = normalizeTableSize(size)
  currentSize.value = nextSize
  emit('density-change', nextSize)
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
 * 处理列表/卡片模式变化
 */
function handleRenderModeChange(mode) {
  currentRenderMode.value = mode
  emit('render-mode-change', mode)
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
    },
  }
  return config
})

/**
 * 更新选中的行
 */
function handleUpdateCheckedKeys(keys) {
  innerCheckedRowKeys.value = keys
  emit('update:checked-row-keys', keys)
}

function handleUpdateExpandedKeys(keys) {
  emit('update:expanded-row-keys', keys)
}

function isRowChecked(row) {
  return innerCheckedRowKeys.value.includes(rowKeyFn.value(row))
}

function setRowChecked(row, checked) {
  const key = rowKeyFn.value(row)
  const keys = new Set(innerCheckedRowKeys.value)
  if (checked) {
    keys.add(key)
  }
  else {
    keys.delete(key)
  }
  handleUpdateCheckedKeys(Array.from(keys))
}

function handleCardClick(row) {
  if (props.hideSelection || !props.cardProps.selectOnClick)
    return
  setRowChecked(row, !isRowChecked(row))
}

function handleCardMouseMove(event) {
  const target = event.currentTarget
  if (!target)
    return

  const rect = target.getBoundingClientRect()
  target.style.setProperty('--card-spotlight-x', `${event.clientX - rect.left}px`)
  target.style.setProperty('--card-spotlight-y', `${event.clientY - rect.top}px`)
}

function handleCardMouseLeave(event) {
  const target = event.currentTarget
  if (!target)
    return

  target.style.setProperty('--card-spotlight-x', '50%')
  target.style.setProperty('--card-spotlight-y', '50%')
}

/**
 * 清除选择
 */
function clearSelection() {
  innerCheckedRowKeys.value = []
  emit('update:checked-row-keys', [])
}

/**
 * 获取选中的行
 */
function getCheckedRows() {
  return props.dataSource.filter((row) => {
    const key = rowKeyFn.value(row)
    return innerCheckedRowKeys.value.includes(key)
  })
}

/**
 * 设置选中的行
 */
function setCheckedKeys(keys) {
  innerCheckedRowKeys.value = keys
  emit('update:checked-row-keys', keys)
}

// 暴露方法
defineExpose({
  clearSelection,
  getCheckedRows,
  setCheckedKeys,
  checkedRowKeys: innerCheckedRowKeys,
  getRenderMode: () => currentRenderMode.value,
  setRenderMode: handleRenderModeChange,
  handleRefresh,
  handleDensityChange,
  handleFilterChange,
})
</script>

<style scoped>
.ai-table-wrapper {
  width: 100%;
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.ai-card-mode {
  padding: 10px 12px 0;
}

.ai-card-mode :deep(.n-spin-container),
.ai-card-mode :deep(.n-spin-content) {
  width: 100%;
}

.ai-card-grid {
  width: 100%;
}

.ai-card-item {
  --card-spotlight-x: 50%;
  --card-spotlight-y: 50%;
  min-width: 0;
  position: relative;
  isolation: isolate;
  border: 1px solid var(--border-light);
  border-radius: 6px;
  background: var(--bg-primary);
  box-shadow: 0 1px 2px rgb(15 23 42 / 4%);
  overflow: hidden;
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease,
    background-color 0.2s ease;
}

.ai-card-item::before,
.ai-card-item::after {
  content: '';
  position: absolute;
  inset: 0;
  z-index: 1;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.2s ease;
}

.ai-card-item::before {
  background: radial-gradient(
    180px circle at var(--card-spotlight-x) var(--card-spotlight-y),
    color-mix(in srgb, var(--primary-color) 22%, transparent),
    color-mix(in srgb, #06b6d4 10%, transparent) 36%,
    transparent 68%
  );
  filter: blur(10px);
}

.ai-card-item::after {
  padding: 1px;
  border-radius: inherit;
  background: radial-gradient(
    150px circle at var(--card-spotlight-x) var(--card-spotlight-y),
    color-mix(in srgb, var(--primary-color) 48%, transparent),
    transparent 72%
  );
  -webkit-mask:
    linear-gradient(#000 0 0) content-box,
    linear-gradient(#000 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
}

.ai-card-item:hover {
  border-color: color-mix(in srgb, var(--primary-color) 46%, var(--border-light));
  box-shadow: 0 4px 12px rgb(15 23 42 / 7%);
}

.ai-card-item:hover::before,
.ai-card-item:hover::after {
  opacity: 1;
}

.ai-card-item.is-checked {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 1px color-mix(in srgb, var(--primary-color) 72%, transparent);
}

.ai-card-default {
  position: relative;
  z-index: 2;
  height: 100%;
  min-height: 136px;
  display: flex;
  flex-direction: column;
}

.ai-card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 42px;
  padding: 10px 12px;
  border-bottom: 1px solid var(--border-light);
  border-radius: 6px 6px 0 0;
  background:
    linear-gradient(135deg, rgb(37 99 235 / 8%), transparent 46%),
    linear-gradient(90deg, color-mix(in srgb, var(--bg-secondary) 82%, transparent), var(--bg-primary));
}

.ai-card-header :deep(.n-checkbox) {
  flex-shrink: 0;
}

.ai-card-title {
  min-width: 0;
  flex: 1;
  color: var(--text-primary);
  font-weight: var(--font-weight-semibold);
  font-size: var(--font-size-sm);
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ai-card-body {
  flex: 1;
  padding: 10px 12px;
  display: grid;
  gap: 6px;
}

.ai-card-field {
  min-width: 0;
  display: grid;
  grid-template-columns: 74px minmax(0, 1fr);
  gap: 6px;
  align-items: center;
  font-size: var(--font-size-sm);
  line-height: 1.4;
}

.ai-card-field-label {
  color: var(--text-tertiary);
  white-space: nowrap;
}

.ai-card-field-value {
  min-width: 0;
  color: var(--text-secondary);
  word-break: break-word;
  display: flex;
  align-items: center;
}

.ai-card-footer {
  display: flex;
  justify-content: flex-end;
  padding: 8px 12px;
  border-top: 1px solid var(--border-light);
  border-radius: 0 0 6px 6px;
  background:
    linear-gradient(90deg, transparent, rgb(16 185 129 / 6%)), color-mix(in srgb, var(--bg-secondary) 56%, transparent);
}

.ai-card-empty {
  padding: 48px 0;
}

.ai-table-empty-state {
  display: flex;
  min-height: 180px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 32px 16px;
  color: var(--text-secondary);
  text-align: center;
}

.ai-table-empty-state :deep(.n-empty__icon) {
  color: color-mix(in srgb, var(--text-tertiary) 72%, transparent);
  font-size: 48px;
}

.ai-table-empty-state :deep(.n-empty__description) {
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: var(--font-weight-medium);
  line-height: 20px;
}

.ai-table-empty-state :deep(.n-empty__extra) {
  margin-top: 4px;
}

.ai-table-empty-desc {
  max-width: 360px;
  color: var(--text-tertiary);
  font-size: 12px;
  line-height: 18px;
}

.ai-card-pagination {
  padding: 12px 0;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
  border-top: 1px solid var(--border-light);
  margin-top: 12px;
}

.ai-table-toolbar {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  border-bottom: 1px solid var(--border-light);
  background: var(--bg-secondary);
  min-height: 50px;
}

:deep(.n-data-table) {
  flex: 1 1 auto;
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

:deep(.n-data-table-wrapper) {
  flex: 1 1 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

:deep(.n-data-table-base-table) {
  flex: 1 1 0;
  height: auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

:deep(.n-data-table-base-table-body) {
  flex: 1 1 auto;
  min-height: 144px;
  overflow: hidden;
}

:deep(.n-data-table-base-table-body .n-scrollbar) {
  height: 100%;
}

:deep(.n-data-table-base-table-body .n-scrollbar-container) {
  min-height: 0;
}

:deep(.n-data-table-th--fixed-left) {
  z-index: 4;
  background-color: var(--n-merged-th-color) !important;
}

:deep(.n-data-table-td--fixed-left) {
  z-index: 3;
  background-color: var(--n-merged-td-color) !important;
}

:deep(.n-data-table-th--selection.n-data-table-th--fixed-left) {
  z-index: 5;
}

:deep(.n-data-table-td--selection.n-data-table-td--fixed-left) {
  z-index: 4;
}

:deep(.n-data-table-th--selection),
:deep(.n-data-table-td--selection) {
  width: 48px;
  min-width: 48px;
  max-width: 48px;
  padding: 0 !important;
  background-clip: padding-box;
}

:deep(.n-data-table-tr:hover .n-data-table-td--fixed-left) {
  background: var(--bg-secondary) !important;
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
:deep(.n-data-table__pagination) {
  width: 100%;
  margin: 0 !important;
  padding: 12px 16px;
  border-top: 1px solid var(--border-light);
}

:deep(.n-data-table__pagination .n-pagination) {
  flex: 0 0 auto;
  width: 100%;
  padding: 0;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
  border-top: 0;
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

.ai-table-density-small :deep(.n-data-table-th) {
  height: 34px;
  padding: 6px 10px;
}

.ai-table-density-small :deep(.n-data-table-td) {
  height: 38px;
  padding: 6px 10px;
}

.ai-table-density-medium :deep(.n-data-table-th) {
  height: 40px;
  padding: 8px 12px;
}

.ai-table-density-medium :deep(.n-data-table-td) {
  height: 44px;
  padding: 8px 12px;
}

.ai-table-density-large :deep(.n-data-table-th) {
  height: 48px;
  padding: 12px 16px;
}

.ai-table-density-large :deep(.n-data-table-td) {
  height: 54px;
  padding: 12px 16px;
}

/* 小屏幕适配 */
@media (max-width: 768px) {
  :deep(.n-data-table__pagination .n-pagination) {
    justify-content: center;
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
  :deep(.n-data-table__pagination) {
    padding: 10px;
  }

  :deep(.n-data-table__pagination .n-pagination) {
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
