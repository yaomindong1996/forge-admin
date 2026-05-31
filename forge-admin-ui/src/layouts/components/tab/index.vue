<template>
  <div id="top-tab">
    <n-tabs
      :value="tabStore.activeTab"
      :closable="hasClosableTabs"
      type="card"
      size="small"
      @close="(path) => tabStore.removeTab(path)"
    >
      <n-tab
        v-for="item in tabStore.tabs"
        :key="item.path"
        :name="item.path"
        :closable="isTabClosable(item)"
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
import { useMenu } from '@/composables'
import { useTabStore } from '@/store'
import ContextMenu from './ContextMenu.vue'

const tabStore = useTabStore()
const { handleMenuSelect: baseHandleMenuSelect } = useMenu()

const contextMenuOption = reactive({
  show: false,
  x: 0,
  y: 0,
  currentPath: '',
})

const hasClosableTabs = computed(() => tabStore.tabs.some(isTabClosable))

function isTabClosable(tab) {
  if (tab?.closable === false)
    return false
  if (tab?.forceClosable)
    return true
  return tabStore.tabs.length > 1
}

function handleItemClick(path) {
  tabStore.setActiveTab(path)
  baseHandleMenuSelect(undefined, path)
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
  width: auto;
  height: 100%;
  display: flex;
  align-items: center;
}
</style>
