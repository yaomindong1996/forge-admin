<template>
  <div id="top-tab">
    <n-tabs
      :value="tabStore.activeTab"
      :closable="tabStore.tabs.length > 1"
      type="card"
      @close="(path) => tabStore.removeTab(path)"
      size="small"
    >
      <n-tab
        v-for="item in tabStore.tabs"
        :key="item.path"
        :name="item.path"
        @click="handleItemClick(item.path)"
        @contextmenu.prevent="handleContextMenu($event, item)"
      >
        {{ item.title }}
      </n-tab>
    </n-tabs>

    <ContextMenu
      v-if="contextMenuOption.show"
      v-model:show="contextMenuOption.show"
      :current-path="contextMenuOption.currentPath"
      :x="contextMenuOption.x"
      :y="contextMenuOption.y"
    />
  </div>
</template>

<script setup>
import { useTabStore } from '@/store'
import ContextMenu from './ContextMenu.vue'

const router = useRouter()
const tabStore = useTabStore()

const contextMenuOption = reactive({
  show: false,
  x: 0,
  y: 0,
  currentPath: '',
})

function handleItemClick(path) {
  tabStore.setActiveTab(path)
  router.push(path)
}

function showContextMenu() {
  contextMenuOption.show = true
}
function hideContextMenu() {
  contextMenuOption.show = false
}
function setContextMenu(x, y, currentPath) {
  Object.assign(contextMenuOption, { x, y, currentPath })
}

// 右击菜单
async function handleContextMenu(e, tagItem) {
  const { clientX, clientY } = e
  hideContextMenu()
  setContextMenu(clientX, clientY, tagItem.path)
  await nextTick()
  showContextMenu()
}
</script>

<style scoped>
#top-tab {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  overflow: hidden;
}

:deep(.n-tabs) {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;

  /* 清除默认底部边线 */
  .n-tabs-nav {
    height: 100%;
    width: 100%;
    border-bottom: none !important;
    box-shadow: none !important;
    justify-content: flex-start !important;
  }

  .n-tabs-nav-scroll-wrapper {
    height: 100%;
    flex: 1;
    min-width: 0;
  }

  .n-tabs-nav-scroll-content {
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: flex-start;
    gap: 3px;
    padding: 0 2px;
  }

  /* 标签页 - SnowAdmin 风格 */
  .n-tabs-tab {
    position: relative;
    display: inline-flex;
    align-items: center;
    padding: 0 10px;
    height: 28px;
    font-size: 12px;
    font-weight: 400;
    color: var(--text-secondary);
    background: var(--bg-primary);
    border: 1px solid var(--border-light);
    border-radius: var(--radius-md);
    margin: 0;
    cursor: pointer;
    transition: background-color var(--transition-fast), border-color var(--transition-fast), color var(--transition-fast);
    user-select: none;
    white-space: nowrap;
    flex-shrink: 0;
  }

  /* 悬停 */
  .n-tabs-tab:hover {
    color: var(--primary-500);
    border-color: var(--primary-200);
    background: var(--primary-50);
  }

  /* 激活标签页 */
  .n-tabs-tab--active {
    color: var(--primary-500) !important;
    background: var(--primary-50) !important;
    border-color: var(--primary-300) !important;
    font-weight: 500;
  }

  /* 激活项小圆点 */
  .n-tabs-tab--active::before {
    content: '';
    display: inline-block;
    width: 5px;
    height: 5px;
    border-radius: 50%;
    background: var(--primary-500);
    margin-right: 5px;
    flex-shrink: 0;
  }

  /* 关闭按钮 */
  .n-tabs-tab-close {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 14px;
    height: 14px;
    margin-left: 3px;
    border-radius: 2px;
    color: var(--text-tertiary);
    font-size: 10px;
    transition: background-color var(--transition-fast), color var(--transition-fast);
    flex-shrink: 0;
  }

  .n-tabs-tab-close:hover {
    background: rgba(239, 68, 68, 0.1);
    color: #ef4444;
  }

  /* 隐藏 Naive UI 默认的底部装饰线 */
  .n-tabs-pad,
  .n-tabs-tab-pad,
  .n-tabs-scroll-padding {
    display: none !important;
  }

  /* 滚动按钮 */
  .n-tabs-nav__prefix,
  .n-tabs-nav__suffix {
    display: flex;
    align-items: center;
    padding: 0 2px;
  }
}

/* 暗色模式 */
.dark :deep(.n-tabs) {
  .n-tabs-tab {
    color: var(--text-secondary);
    background: var(--bg-secondary);
    border-color: var(--border-default);
  }

  .n-tabs-tab:hover {
    color: var(--primary-400);
    border-color: var(--primary-700);
    background: rgba(64, 128, 255, 0.08);
  }

  .n-tabs-tab--active {
    color: var(--primary-400) !important;
    background: rgba(64, 128, 255, 0.1) !important;
    border-color: var(--primary-700) !important;
  }

  .n-tabs-tab--active::before {
    background: var(--primary-400);
  }

  .n-tabs-tab-close:hover {
    background: rgba(239, 68, 68, 0.15);
    color: #f87171;
  }
}
</style>
