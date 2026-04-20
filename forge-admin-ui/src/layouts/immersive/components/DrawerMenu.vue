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
import { useRouter, useRoute } from 'vue-router'
import { h } from 'vue'
import { usePermissionStore } from '@/store'
import IconRenderer from '@/components/IconRenderer.vue'
import { isExternal } from '@/utils'

const router = useRouter()
const route = useRoute()
const permissionStore = usePermissionStore()

const drawerVisible = defineModel('show', { type: Boolean, default: false })
const emit = defineEmits(['select'])

const searchKeyword = ref('')

// 处理菜单数据
const processedMenus = computed(() => {
  const menus = permissionStore.menus || []
  return processMenuData(menus)
})

// 过滤菜单
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

function processMenuData(menuItems) {
  if (!menuItems || !Array.isArray(menuItems))
    return []

  return menuItems.map((item) => {
    const menuItem = {
      key: item.key || item.id,
      label: item.label || item.name,
    }

    if (item.icon) {
      if (typeof item.icon === 'string' && item.icon.trim() !== '') {
        menuItem.icon = () => h(IconRenderer, { icon: item.icon })
      }
    }

    if (item.path) {
      menuItem.path = item.path
      menuItem.originalItem = item
    }

    if (item.children && item.children.length > 0) {
      const children = processMenuData(item.children)
      if (children.length > 0) {
        menuItem.children = children
      }
    }

    return menuItem
  })
}

const activeKey = computed(() => {
  const findMenuIdByPath = (items, targetPath) => {
    if (!items || !Array.isArray(items))
      return null

    for (const item of items) {
      if (item.path === targetPath) {
        return item.key
      }
      if (item.children && item.children.length > 0) {
        const found = findMenuIdByPath(item.children, targetPath)
        if (found)
          return found
      }
    }
    return null
  }

  if (route.meta?.parentKey) {
    return route.meta.parentKey
  }

  return findMenuIdByPath(processedMenus.value, route.path) || route.name
})

function handleMenuSelect(key, item) {
  const originalItem = findMenuItem(permissionStore.menus, key)
  if (!originalItem)
    return

  if (isExternal(originalItem.path)) {
    window.open(originalItem.path)
    return
  }

  if (originalItem.openMode === 'iframe' && originalItem.subAppURL) {
    const iframePath = `/iframe?page=${encodeURIComponent(originalItem.subAppURL + (originalItem.path || ''))}`
    router.push(iframePath)
    return
  }

  if (originalItem.path) {
    router.push(originalItem.path)
    emit('select')
    drawerVisible.value = false
  }
}

function findMenuItem(menuItems, key) {
  if (!menuItems || !Array.isArray(menuItems))
    return null

  for (const item of menuItems) {
    if ((item.key || item.id) === key) {
      return item
    }
    if (item.children && item.children.length > 0) {
      const found = findMenuItem(item.children, key)
      if (found)
        return found
    }
  }
  return null
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
