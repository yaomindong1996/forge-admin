<template>
  <n-menu
    ref="menu"
    class="side-menu"
    accordion
    :indent="10"
    :collapsed-icon-size="22"
    :collapsed-width="64"
    :collapsed="appStore.collapsed"
    :options="sideMenuOptions"
    :value="activeKey"
    @update:value="handleMenuSelect"
  />
</template>

<script setup>
import { useAppStore, usePermissionStore } from '@/store'
import { isExternal } from '@/utils'
import { useRoute, useRouter } from 'vue-router'
import { processTopMenus, processMenuData } from '@/utils/menu-utils'
import { h } from 'vue'

const router = useRouter()
const route = useRoute()
const appStore = useAppStore()
const permissionStore = usePermissionStore()

// 获取当前顶部选中的一级菜单的子菜单
const sideMenuOptions = computed(() => {
  const menus = permissionStore.menus || []
  
  // 如果菜单数据还没加载完成，返回空数组
  if (!menus.length || !permissionStore.menuDataLoaded) {
    return []
  }
  
  const topMenus = processTopMenus(menus)

  // 递归查找包含当前路由的顶级菜单
  const findTopMenuByPath = (currentPath) => {
    const findInMenu = (menuItems, topMenu) => {
      for (const item of menuItems) {
        if (item.path === currentPath) {
          return topMenu
        }
        if (item.children && item.children.length > 0) {
          const found = findInMenu(item.children, topMenu)
          if (found) {
            return found
          }
        }
      }
      return null
    }

    for (const topMenu of topMenus) {
      if (topMenu.path === currentPath) {
        return topMenu
      }
      if (topMenu.children && topMenu.children.length > 0) {
        const found = findInMenu(topMenu.children, topMenu)
        if (found) {
          return found
        }
      }
    }
    return null
  }

  // 根据当前路由查找对应的顶级菜单
  let activeTopMenu = findTopMenuByPath(route.path)

  // 如果没找到（可能是隐藏菜单或二级页面），尝试根据路径前缀匹配
  if (!activeTopMenu) {
    // 尝试根据路径前缀匹配（例如 /system/dictData 应该匹配 /system 开头的顶级菜单）
    const pathSegments = route.path.split('/').filter(Boolean)
    if (pathSegments.length > 0) {
      const firstSegment = '/' + pathSegments[0]
      activeTopMenu = topMenus.find(menu => {
        if (menu.path && menu.path.startsWith(firstSegment)) {
          return true
        }
        // 检查子菜单是否有匹配的路径前缀
        if (menu.children && menu.children.length > 0) {
          return menu.children.some(child => 
            child.path && child.path.startsWith(firstSegment)
          )
        }
        return false
      })
    }
  }

  // 如果还是没找到，使用store中保存的ID
  if (!activeTopMenu && appStore.selectedTopMenuId) {
    activeTopMenu = topMenus.find(item => item.id === appStore.selectedTopMenuId)
  }

  // 更新store中的选中状态（避免重复更新）
  if (activeTopMenu && appStore.selectedTopMenuId !== activeTopMenu.id) {
    appStore.setSelectedTopMenuId(activeTopMenu.id)
  }

  if (activeTopMenu && activeTopMenu.children) {
    const processed = processMenuData(activeTopMenu.children)
    return processed
  }
  return []
})

const activeKey = computed(() => {
  // 优先使用 route.meta.parentKey（用于隐藏的二级页面）
  if (route.meta?.parentKey) {
    return route.meta.parentKey
  }
  
  // 根据当前路由 path 查找对应的菜单项 id
  const findMenuIdByPath = (items, targetPath) => {
    if (!items || !Array.isArray(items)) return null

    for (const item of items) {
      if (item.path === targetPath) {
        return item.key || item.id
      }
      if (item.children && item.children.length > 0) {
        const found = findMenuIdByPath(item.children, targetPath)
        if (found) return found
      }
    }
    return null
  }

  // 尝试精确匹配
  let menuId = findMenuIdByPath(sideMenuOptions.value, route.path)
  
  // 如果没找到，尝试根据路径前缀匹配父级菜单
  if (!menuId) {
    const pathSegments = route.path.split('/').filter(Boolean)
    // 从最长路径开始尝试匹配
    for (let i = pathSegments.length - 1; i > 0; i--) {
      const parentPath = '/' + pathSegments.slice(0, i).join('/')
      menuId = findMenuIdByPath(sideMenuOptions.value, parentPath)
      if (menuId) break
    }
  }
  
  return menuId || route.path
})

const menu = ref(null)
watch(route, async () => {
  await nextTick()
  menu.value?.showOption()
})

function handleMenuSelect(key, item) {
  // 先从侧边菜单选项中查找
  let originalItem = findMenuItem(sideMenuOptions.value, key)
  
  // 如果没找到，再从完整菜单树中查找
  if (!originalItem) {
    originalItem = findMenuItem(permissionStore.menus, key)
  }
  
  if (!originalItem) {
    console.warn('未找到菜单项:', key)
    return
  }

  // 处理外部链接
  if (originalItem.path && isExternal(originalItem.path)) {
    $dialog.confirm({
      type: 'info',
      title: `请选择打开方式`,
      positiveText: '外链打开',
      negativeText: '在本站内嵌打开',
      confirm() {
        window.open(originalItem.path)
      },
      cancel: () => {
        router.push(originalItem.path)
      },
    })
    return
  }

  // iframe模式处理
  if (originalItem.openMode === 'iframe' && originalItem.subAppURL) {
    const iframePath = `/iframe?page=${encodeURIComponent(originalItem.subAppURL + (originalItem.path || ''))}`
    router.push(iframePath)
    return
  }

  // 普通路由跳转
  if (originalItem.path) {
    router.push(originalItem.path)
  } else {
    console.warn('菜单项没有路径:', originalItem)
  }
}

// 递归查找菜单项
const findMenuItem = (menuItems, key) => {
  if (!menuItems || !Array.isArray(menuItems)) return null

  for (const item of menuItems) {
    if ((item.key || item.id) === key) {
      return item
    }
    if (item.children && item.children.length > 0) {
      const found = findMenuItem(item.children, key)
      if (found) return found
    }
  }
  return null
}

function renderMenuIcon(MenuOption) {
  return  h(IconRenderer, {
    icon: MenuOption.icon
  })
}
</script>

<style>
.side-menu:not(.n-menu--collapsed) {
  .n-menu-item-content {
    &::before {
      left: 8px;
      right: 8px;
    }
    &.n-menu-item-content--selected::before {
      border-left: 4px solid rgb(var(--primary-color));
    }
  }
}
</style>
