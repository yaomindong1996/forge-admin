<!--
  表格工具栏操作组件
  提供刷新、密度调整、列设置、搜索显示/隐藏等功能
  参考 toolbarAction 设计

  @author AI Form Team
  @version 1.0.0
-->

<template>
  <div class="ai-toolbar-action">
    <!-- 刷新按钮 -->
    <n-tooltip v-if="showRefresh">
      <template #trigger>
        <n-button text @click="handleRefresh">
          <template #icon>
            <n-icon size="18">
              <RefreshOutline />
            </n-icon>
          </template>
        </n-button>
      </template>
      刷新
    </n-tooltip>

    <!-- 密度调整 -->
    <n-dropdown
      v-if="showDensity"
      trigger="click"
      placement="bottom-end"
      :options="densityOptions"
      @select="handleDensityChange"
    >
      <n-button text>
        <template #icon>
          <n-icon size="18">
            <ResizeOutline />
          </n-icon>
        </template>
      </n-button>
    </n-dropdown>

    <!-- 列设置 -->
    <AiTableFilter
      v-if="showColumnFilter"
      :columns="columns"
      :filter-max-height="filterMaxHeight"
      :default-checked="defaultCheckedColumns"
      @filter-change="handleFilterChange"
    />

    <!-- 搜索显示/隐藏 -->
    <n-button
      v-if="showSearchToggle"
      text
      @click="handleSearchToggle"
    >
      <span style="font-size: var(--font-size-base); margin-right: 4px">
        {{ searchVisible ? '隐藏搜索' : '显示搜索' }}
      </span>
      <template #icon>
        <n-icon size="14">
          <component :is="searchVisible ? ChevronUpOutline : ChevronDownOutline" />
        </n-icon>
      </template>
    </n-button>

    <!-- 全屏按钮 -->
    <n-tooltip v-if="showFullscreen">
      <template #trigger>
        <n-button text @click="handleFullscreen">
          <template #icon>
            <n-icon size="18">
              <component :is="isFullscreen ? ContractOutline : ExpandOutline" />
            </n-icon>
          </template>
        </n-button>
      </template>
      {{ isFullscreen ? '退出全屏' : '全屏' }}
    </n-tooltip>

    <!-- 自定义操作插槽 -->
    <slot name="extra" />
  </div>
</template>

<script setup>
import {
  ChevronDownOutline,
  ChevronUpOutline,
  ContractOutline,
  ExpandOutline,
  RefreshOutline,
  ResizeOutline,
} from '@vicons/ionicons5'
import { ref } from 'vue'
import AiTableFilter from './AiTableFilter.vue'

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
   * 列筛选下拉菜单最大高度
   * @type {string}
   */
  filterMaxHeight: {
    type: String,
    default: '400px',
  },

  /**
   * 默认选中的列
   * @type {Array<string>}
   */
  defaultCheckedColumns: {
    type: Array,
    default: () => [],
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
   * 是否显示搜索切换
   * @type {boolean}
   */
  showSearchToggle: {
    type: Boolean,
    default: true,
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
   * 搜索表单是否可见
   * @type {boolean}
   */
  searchVisible: {
    type: Boolean,
    default: true,
  },
})

/**
 * Emits 定义
 */
const emit = defineEmits([
  'refresh', // 刷新
  'density-change', // 密度变化
  'filter-change', // 列筛选变化
  'search-toggle', // 搜索显示/隐藏切换
  'fullscreen-change', // 全屏状态变化
])

/**
 * 密度选项
 */
const densityOptions = [
  {
    label: '紧凑',
    key: 'small',
  },
  {
    label: '默认',
    key: 'medium',
  },
  {
    label: '宽松',
    key: 'large',
  },
]

/**
 * 是否全屏
 */
const isFullscreen = ref(false)

/**
 * 刷新
 */
function handleRefresh() {
  emit('refresh')
}

/**
 * 密度变化
 */
function handleDensityChange(key) {
  emit('density-change', key)
}

/**
 * 列筛选变化
 */
function handleFilterChange(columns) {
  emit('filter-change', columns)
}

/**
 * 搜索显示/隐藏切换
 */
function handleSearchToggle() {
  emit('search-toggle', !props.searchVisible)
}

/**
 * 全屏切换
 */
function handleFullscreen() {
  isFullscreen.value = !isFullscreen.value
  emit('fullscreen-change', isFullscreen.value)
}

/**
 * 暴露方法
 */
defineExpose({
  /**
   * 切换全屏
   */
  toggleFullscreen: handleFullscreen,

  /**
   * 获取全屏状态
   */
  getFullscreenStatus: () => isFullscreen.value,
})
</script>

<style scoped>
.ai-toolbar-action {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: var(--font-size-lg);
}
</style>
