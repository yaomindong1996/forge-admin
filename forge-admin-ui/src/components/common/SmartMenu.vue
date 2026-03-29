<template>
  <div class="smart-menu-wrapper">
    <n-menu
      :options="sortedMenuOptions"
      :value="activeKey"
      :expanded-keys="expandedKeys"
      :collapsed="collapsed"
      :collapsed-width="collapsedWidth"
      :collapsed-icon-size="22"
      @update:value="handleSelect"
      @update:expanded-keys="handleExpand"
    >
      <template #default="{ option }">
        <div class="menu-item-content">
          <!-- 图标 -->
          <i v-if="option.icon" :class="option.icon" class="menu-icon" />
          
          <!-- 标签文字 -->
          <span class="menu-label">{{ option.label }}</span>
          
          <!-- 使用频率徽章 -->
          <n-tag
            v-if="showFrequency && option.frequency >= threshold"
            size="small"
            type="success"
            class="menu-badge"
          >
            常用
          </n-tag>
          
          <!-- 通知计数 -->
          <n-badge
            v-if="option.badge"
            :value="option.badge"
            :max="99"
            class="menu-notification"
          />
        </div>
      </template>
    </n-menu>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useMenuStore } from '@/store'

const props = defineProps({
  // 是否显示使用频率
  showFrequency: {
    type: Boolean,
    default: true
  },
  // 显示常用标签的阈值
  threshold: {
    type: Number,
    default: 10
  },
  // 是否折叠
  collapsed: {
    type: Boolean,
    default: false
  },
  // 折叠宽度
  collapsedWidth: {
    type: Number,
    default: 64
  },
  // 菜单选项
  options: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['select', 'expand'])

const router = useRouter()
const menuStore = useMenuStore()

const activeKey = ref('')
const expandedKeys = ref([])

// 从store获取菜单使用频率
const menuFrequency = computed(() => menuStore.menuFrequency || {})

// 为菜单选项添加频率信息
const optionsWithFrequency = computed(() => {
  return addFrequencyToProps(props.options)
})

// 递归添加频率
function addFrequencyToProps(options) {
  return options.map(option => ({
    ...option,
    frequency: menuFrequency.value[option.key] || 0,
    children: option.children ? addFrequencyToProps(option.children) : undefined
  }))
}

// 根据频率排序菜单
const sortedMenuOptions = computed(() => {
  return sortMenuByFrequency(optionsWithFrequency.value)
})

// 递归排序菜单
function sortMenuByFrequency(options) {
  const sorted = [...options].sort((a, b) => {
    // 如果两个都有频率，按频率降序
    if (a.frequency && b.frequency) {
      return b.frequency - a.frequency
    }
    // 如果只有一个有频率，有频率的排前面
    if (a.frequency) return -1
    if (b.frequency) return 1
    // 都没有频率，保持原顺序
    return 0
  })

  // 递归处理子菜单
  return sorted.map(option => ({
    ...option,
    children: option.children ? sortMenuByFrequency(option.children) : undefined
  }))
}

// 处理菜单选择
function handleSelect(key) {
  activeKey.value = key
  
  // 记录菜单使用频率
  menuStore.incrementMenuFrequency(key)
  
  // 导航
  const findMenuItem = (options) => {
    for (const option of options) {
      if (option.key === key) {
        return option
      }
      if (option.children) {
        const found = findMenuItem(option.children)
        if (found) return found
      }
    }
    return null
  }
  
  const menuItem = findMenuItem(props.options)
  if (menuItem && menuItem.path) {
    router.push(menuItem.path)
  }
  
  emit('select', key, menuItem)
}

// 处理菜单展开
function handleExpand(keys) {
  expandedKeys.value = keys
  emit('expand', keys)
}

// 监听路由变化，更新当前激活的菜单
watch(() => router.currentRoute.value.path, (path) => {
  const findKeyByPath = (options) => {
    for (const option of options) {
      if (option.path === path) {
        return option.key
      }
      if (option.children) {
        const key = findKeyByPath(option.children)
        if (key) return key
      }
    }
    return null
  }
  
  activeKey.value = findKeyByPath(props.options) || ''
}, { immediate: true })

// 暴露方法供外部调用
defineExpose({
  incrementFrequency: (key) => {
    menuStore.incrementMenuFrequency(key)
  },
  getFrequency: (key) => {
    return menuFrequency.value[key] || 0
  }
})
</script>

<style scoped>
.smart-menu-wrapper {
  width: 100%;
  height: 100%;
  background: var(--side-menu-bg-color);
}

.menu-item-content {
  position: relative;
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: 0 var(--space-2);
  overflow: hidden;
}

.menu-icon {
  font-size: 18px;
  flex-shrink: 0;
  color: var(--side-menu-icon-color);
  transition: color var(--transition);
}

.menu-label {
  flex: 1;
  font-size: var(--side-menu-font-size);
  font-weight: var(--side-menu-font-weight);
  color: var(--side-menu-text-color);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color var(--transition);
}

.menu-badge {
  flex-shrink: 0;
  margin-left: var(--space-2);
  font-size: 11px;
  padding: 2px 6px;
  height: 18px;
  line-height: 14px;
}

.menu-notification {
  flex-shrink: 0;
  margin-left: var(--space-2);
}

/* 选中状态 */
:deep(.n-menu-item-content--selected) {
  background: var(--side-menu-bg-color-active) !important;
}

:deep(.n-menu-item-content--selected .menu-icon) {
  color: var(--side-menu-icon-color-active) !important;
}

:deep(.n-menu-item-content--selected .menu-label) {
  color: var(--side-menu-text-color-active) !important;
  font-weight: var(--font-weight-semibold);
}

/* 悬停状态 */
:deep(.n-menu-item-content:hover:not(.n-menu-item-content--selected)) {
  background: var(--side-menu-bg-color-hover) !important;
}

:deep(.n-menu-item-content:hover:not(.n-menu-item-content--selected) .menu-icon) {
  color: var(--side-menu-text-color-hover) !important;
}

:deep(.n-menu-item-content:hover:not(.n-menu-item-content--selected) .menu-label) {
  color: var(--side-menu-text-color-hover) !important;
}

/* 折叠状态 */
:deep(.n-menu.n-menu--collapsed .menu-badge) {
  display: none;
}

:deep(.n-menu.n-menu--collapsed .menu-notification) {
  display: none;
}

/* 暗色模式适配 */
.dark .menu-badge {
  background: var(--success-900) !important;
  color: var(--success-100) !important;
}

/* 动画效果 */
:deep(.n-menu-item-content) {
  transition: all var(--transition);
}

:deep(.n-menu-item-content::before) {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: var(--primary-600);
  opacity: 0;
  transition: opacity var(--transition);
}

:deep(.n-menu-item-content--selected::before) {
  opacity: 1;
}

/* 滚动条优化 */
:deep(.n-menu-scroll-content) {
  scrollbar-width: thin;
  scrollbar-color: var(--gray-300) transparent;
}

:deep(.n-menu-scroll-content::-webkit-scrollbar) {
  width: 6px;
}

:deep(.n-menu-scroll-content::-webkit-scrollbar-track) {
  background: transparent;
}

:deep(.n-menu-scroll-content::-webkit-scrollbar-thumb) {
  background: var(--gray-300);
  border-radius: var(--radius-sm);
}

.dark :deep(.n-menu-scroll-content) {
  scrollbar-color: var(--gray-600) transparent;
}

.dark :deep(.n-menu-scroll-content::-webkit-scrollbar-thumb) {
  background: var(--gray-600);
}
</style>