<template>
  <div class="menu-container">
    <!-- 侧边栏菜单 -->
    <n-menu
      :options="menuOptions"
      :collapsed="collapsed"
      :collapsed-width="64"
      :collapsed-icon-size="22"
      :indent="24"
      :expanded-keys="expandedKeys"
      @update:expanded-keys="handleExpand"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, h } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NMenu } from 'naive-ui'
import { usePermissionStore } from '@/store'

const props = defineProps({
  collapsed: {
    type: Boolean,
    default: false
  }
})

const route = useRoute()
const router = useRouter()
const permissionStore = usePermissionStore()

// 展开的菜单key
const expandedKeys = ref([])

// 处理菜单展开
const handleExpand = (keys) => {
  expandedKeys.value = keys
}

// 递归处理菜单数据
const processMenuData = (menuItems) => {
  if (!menuItems || !Array.isArray(menuItems)) return []
  
  return menuItems.map(item => {
    // 只处理subapp、module和menu类型的菜单项
    if (!['subapp', 'module', 'menu'].includes(item.type)) {
      return null
    }
    
    const menuItem = {
      key: item.id,
      label: item.name
    }
    
    // 设置图标
    if (item.icon && item.icon.trim() !== '') {
      menuItem.icon = () => h('i', { class: item.icon })
    }
    
    // 对于menu类型，设置路由路径和点击事件
    if (item.type === 'menu' && item.path) {
      menuItem.path = item.path
      menuItem.onClick = () => handleMenuClick(item)
    }
    
    // 处理子菜单
    if (item.children && item.children.length > 0) {
      // 过滤掉空的子菜单
      const children = processMenuData(item.children).filter(child => child !== null)
      if (children.length > 0) {
        menuItem.children = children
      }
    }
    
    return menuItem
  }).filter(item => item !== null)
}

// 处理菜单点击
const handleMenuClick = (item) => {
  if (item.path) {
    // iframe模式处理
    if (item.openMode === 'iframe' && item.subAppURL) {
      router.push(`/iframe?page=${encodeURIComponent(item.subAppURL + (item.path || ''))}`)
    } else {
      router.push(item.path)
    }
  }
}

// 菜单选项
const menuOptions = computed(() => {
  // 使用从新接口获取的菜单数据
  const menuData = permissionStore.menus || []
  return processMenuData(menuData)
})

// 初始化展开菜单
onMounted(() => {
  // 默认展开当前路由匹配的菜单
  const currentPath = route.path
  const expandKeys = []
  
  const findMatchingKeys = (menus) => {
    menus.forEach(menu => {
      if (menu.path === currentPath) {
        expandKeys.push(menu.key)
      }
      if (menu.children && menu.children.length > 0) {
        findMatchingKeys(menu.children)
        // 如果子菜单中有匹配的，也要展开当前菜单
        const hasMatchingChild = menu.children.some(child => expandKeys.includes(child.key))
        if (hasMatchingChild) {
          expandKeys.push(menu.key)
        }
      }
    })
  }
  
  if (menuOptions.value && menuOptions.value.length > 0) {
    findMatchingKeys(menuOptions.value)
    expandedKeys.value = expandKeys
  }
})
</script>

<style scoped>
.menu-container {
  height: 100%;
  overflow-y: auto;
}
</style>