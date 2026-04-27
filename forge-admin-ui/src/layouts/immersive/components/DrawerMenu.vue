<template>
  <n-drawer
    v-model:show="drawerVisible"
    :width="280"
    placement="left"
    :mask-closable="true"
  >
    <n-drawer-content closable title="菜单导航">
      <div class="drawer-menu-container">
        <!-- 搜索框 -->
        <div class="drawer-search">
          <n-input
            v-model:value="searchKeyword"
            placeholder="搜索菜单..."
            clearable
            size="small"
            round
          >
            <template #prefix>
              <i class="i-mdi-magnify" />
            </template>
          </n-input>
        </div>

        <!-- 菜单列表 -->
        <n-scrollbar class="drawer-menu-list">
          <n-menu
            :options="filteredMenus"
            :value="activeKey"
            :indent="16"
            @update:value="handleMenuSelect"
          />
        </n-scrollbar>
      </div>
    </n-drawer-content>
  </n-drawer>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { usePermissionStore } from '@/store'
import { useMenu } from '@/composables'
import { processMenuData } from '@/utils/menu-utils'

const route = useRoute()
const permissionStore = usePermissionStore()

const { handleMenuSelect: baseHandleMenuSelect, findMenuIdByPath } = useMenu()

const drawerVisible = defineModel('show', { type: Boolean, default: false })
const emit = defineEmits(['select'])

const searchKeyword = ref('')

// Process menu data
const processedMenus = computed(() => {
  const menus = permissionStore.menus || []
  return processMenuData(menus)
})

// Filter menus by keyword
const filteredMenus = computed(() => {
  if (!searchKeyword.value.trim()) {
    return processedMenus.value
  }
  const keyword = searchKeyword.value.toLowerCase().trim()
  return filterMenus(processedMenus.value, keyword)
})

function filterMenus(items, keyword) {
  return items.reduce((acc, item) => {
    const titleMatch = (item.label || '').toLowerCase().includes(keyword)
    const filteredChildren = item.children ? filterMenus(item.children, keyword) : []

    if (titleMatch || filteredChildren.length > 0) {
      acc.push({
        ...item,
        children: filteredChildren.length > 0 ? filteredChildren : undefined,
      })
    }
    return acc
  }, [])
}

// Active menu key
const activeKey = computed(() => {
  if (route.meta?.parentKey) {
    return route.meta.parentKey
  }
  return findMenuIdByPath(route.path) || route.name
})

function handleMenuSelect(key) {
  baseHandleMenuSelect(key)
  emit('select')
  drawerVisible.value = false
}
</script>

<style scoped>
.drawer-menu-container {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.drawer-search {
  padding: 4px 0 12px;
}

.drawer-menu-list {
  flex: 1;
  overflow: hidden;
}
</style>
