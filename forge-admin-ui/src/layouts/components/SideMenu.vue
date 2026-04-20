<template>
  <n-menu
    ref="menu"
    class="modern-side-menu"
    accordion
    :indent="18"
    :collapsed-icon-size="22"
    :collapsed-width="64"
    :collapsed="appStore.collapsed"
    :options="processedMenus"
    :value="activeKey"
    @update:value="handleMenuSelect"
  />
</template>

<script setup>
import { h } from 'vue'
import IconRenderer from '@/components/IconRenderer.vue'
import { useAppStore, usePermissionStore } from '@/store'
import { isExternal } from '@/utils'

const router = useRouter()
const route = useRoute()
const appStore = useAppStore()
const permissionStore = usePermissionStore()

// 处理菜单数据，适配naive-ui的menu组件
const processedMenus = computed(() => {
  const menus = permissionStore.menus || []
  return processMenuData(menus)
})

// 递归处理菜单数据
function processMenuData(menuItems) {
  if (!menuItems || !Array.isArray(menuItems))
    return []

  // 展平处理：过滤掉 type 为 subapp 的菜单项，但保留其 children
  const flattenedItems = menuItems.reduce((acc, item) => {
    if (item.type === 'subapp') {
      // 如果是 subapp 类型，将其 children 提升到当前层级
      if (item.children && item.children.length > 0) {
        acc.push(...item.children)
      }
    }
    else {
      // 不是 subapp 类型，正常保留
      acc.push(item)
    }
    return acc
  }, [])

  return flattenedItems.map((item) => {
    const menuItem = {
      key: item.key || item.id,
      label: item.label || item.name,
    }

    // 设置图标
    if (item.icon) {
      // 如果是图标类名，创建图标组件
      if (typeof item.icon === 'string' && item.icon.trim() !== '') {
        menuItem.icon = () => h(IconRenderer, {
          icon: item.icon,
        })
      }
    }
    else {
      menuItem.icon = () => h(IconRenderer, {
        icon: '-1',
      })
    }

    // 设置路由路径
    if (item.path) {
      menuItem.path = item.path
    }

    // 处理子菜单
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
  // 根据当前路由 path 查找对应的菜单项 id
  const findMenuIdByPath = (items, targetPath) => {
    if (!items || !Array.isArray(items))
      return null

    for (const item of items) {
      if (item.path === targetPath) {
        return item.key || item.id
      }
      if (item.children && item.children.length > 0) {
        const found = findMenuIdByPath(item.children, targetPath)
        if (found)
          return found
      }
    }
    return null
  }

  console.log('SideMenu activeKey 计算:', {
    path: route.path,
    parentKey: route.meta?.parentKey,
    routeName: route.name,
  })

  // 优先使用 route.meta.parentKey（用于隐藏的二级页面）
  if (route.meta?.parentKey) {
    console.log('使用 parentKey:', route.meta.parentKey)
    return route.meta.parentKey
  }

  // 尝试精确匹配
  let menuId = findMenuIdByPath(processedMenus.value, route.path)
  console.log('精确匹配结果:', menuId)

  // 如果没找到（可能是隐藏菜单），尝试根据路径前缀匹配父级菜单
  if (!menuId) {
    const pathSegments = route.path.split('/').filter(Boolean)
    // 从最长路径开始尝试匹配
    for (let i = pathSegments.length - 1; i > 0; i--) {
      const parentPath = `/${pathSegments.slice(0, i).join('/')}`
      menuId = findMenuIdByPath(processedMenus.value, parentPath)
      if (menuId) {
        console.log('通过路径前缀匹配找到:', parentPath, menuId)
        break
      }
    }
  }

  const finalKey = menuId || route.name
  console.log('最终 activeKey:', finalKey)
  return finalKey
})

const menu = ref(null)
watch(route, async () => {
  await nextTick()
  menu.value?.showOption()
})

function handleMenuSelect(key, item) {
  // 查找原始菜单项
  const originalItem = findMenuItem(permissionStore.menus, key)
  if (!originalItem)
    return

  // 处理外部链接
  if (isExternal(originalItem.path)) {
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
  }
}

// 递归查找菜单项
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

<style>
.modern-side-menu {
  padding: 6px 0;
  background: transparent;
}

/* 菜单项 - SnowAdmin 风格 */
.modern-side-menu .n-menu-item-content {
  margin: 1px 6px;
  border-radius: var(--radius-md);
  transition:
    background-color var(--transition-fast),
    color var(--transition-fast);
  font-size: 13px;
  font-weight: 400;
  color: var(--text-secondary);
  min-height: 38px;
  padding: 0 12px !important;
}

.modern-side-menu .n-menu-item-content:hover {
  color: var(--primary-500);
  background: var(--primary-50);
}

/* 选中状态 */
.modern-side-menu .n-menu-item-content--selected {
  background: var(--primary-50) !important;
  color: var(--primary-500) !important;
  font-weight: 500;
}

.modern-side-menu .n-menu-item-content--selected:hover {
  background: var(--primary-50) !important;
  color: var(--primary-500) !important;
}

/* 选中项左侧指示条 */
.modern-side-menu .n-menu-item-content--selected::after {
  content: '';
  position: absolute;
  left: 0;
  top: 20%;
  height: 60%;
  width: 3px;
  background: var(--primary-500);
  border-radius: 0 2px 2px 0;
}

/* 图标 */
.modern-side-menu .n-menu-item-content__icon {
  font-size: 16px;
  margin-right: 8px !important;
  color: var(--text-tertiary);
  opacity: 0.8;
}

.modern-side-menu .n-menu-item-content:hover .n-menu-item-content__icon {
  color: var(--primary-500);
  opacity: 1;
}

.modern-side-menu .n-menu-item-content--selected .n-menu-item-content__icon {
  color: var(--primary-500) !important;
  opacity: 1;
}

/* 菜单文字 */
.modern-side-menu .n-menu-item-content__label {
  font-size: 13px;
  line-height: 1.4;
}

.modern-side-menu .n-menu-item-content--selected .n-menu-item-content__label {
  color: var(--primary-500) !important;
}

.modern-side-menu .n-menu-item-content:hover .n-menu-item-content__label {
  color: var(--primary-500) !important;
}

/* 子菜单缩进 */
.modern-side-menu .n-submenu-children {
  padding-left: 0;
}

.modern-side-menu .n-submenu-children .n-menu-item-content {
  font-size: 13px;
}

/* 分组标题 */
.modern-side-menu .n-menu-item-group-title {
  padding: 14px 16px 4px !important;
  font-size: 11px !important;
  font-weight: 600;
  color: var(--text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

/* 折叠状态 */
.modern-side-menu.n-menu--collapsed .n-menu-item-content {
  justify-content: center;
  padding: 0 !important;
  width: 38px;
  height: 38px;
  margin: 2px auto;
  border-radius: var(--radius-md);
}

/* 折叠状态下强制图标居中 - 隐藏文字和箭头占位区域 */
.modern-side-menu.n-menu--collapsed .n-menu-item-content__icon {
  margin-right: 0 !important;
}

.modern-side-menu.n-menu--collapsed .n-menu-item-content-header {
  display: none !important;
}

.modern-side-menu.n-menu--collapsed .n-menu-item-content__arrow {
  display: none !important;
}

.modern-side-menu.n-menu--collapsed .n-menu-item-content--selected::after {
  display: none;
}

/* 滚动条 */
.modern-side-menu::-webkit-scrollbar {
  width: 4px;
}

.modern-side-menu::-webkit-scrollbar-track {
  background: transparent;
}

.modern-side-menu::-webkit-scrollbar-thumb {
  background: var(--border-light);
  border-radius: var(--radius-full);
}

.modern-side-menu::-webkit-scrollbar-thumb:hover {
  background: var(--border-default);
}
</style>
