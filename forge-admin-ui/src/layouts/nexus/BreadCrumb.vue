<template>
  <n-breadcrumb separator="/">
    <!-- 面包屑匹配到菜单：显示父级链 -->
    <template v-if="breadItems?.length">
      <n-breadcrumb-item
        v-for="(item, index) of breadItems"
        :key="item.code"
        :clickable="!!item.path"
        @click="handleItemClick(item)"
      >
        <n-dropdown
          v-if="item.children && item.children.length > 0"
          :options="getDropdownOptions(item.children)"
          @select="handleDropdownSelect"
        >
          <div class="flex items-center">
            <IconRenderer
              v-if="item.icon"
              :icon="item.icon"
              :size="16"
            />
            <span class="ml-4">{{ item.label }}</span>
          </div>
        </n-dropdown>
        <div v-else class="flex items-center">
          <IconRenderer
            v-if="item.icon"
            :icon="item.icon"
            :size="16"
          />
          <span class="ml-4">{{ item.label }}</span>
        </div>
      </n-breadcrumb-item>
    </template>

    <!-- 未匹配到菜单：直接显示页面标题 -->
    <template v-else>
      <n-breadcrumb-item v-if="route.meta.title" :clickable="false">
        {{ route.meta.title }}
      </n-breadcrumb-item>
    </template>
  </n-breadcrumb>
</template>

<script setup>
import { h, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { usePermissionStore } from '@/store'
import IconRenderer from '@/components/IconRenderer.vue'

const router = useRouter()
const route = useRoute()
const permissionStore = usePermissionStore()

const breadItems = ref([])

// 根据当前路由 path 在菜单树中查找
watch(
  () => route.path,
  (path) => {
    if (!path) {
      breadItems.value = []
      return
    }
    breadItems.value = findBreadItemsByPath(permissionStore.menus, path)
  },
  { immediate: true },
)

function findBreadItemsByPath(tree, targetPath, parents = []) {
  if (!Array.isArray(tree))
    return []
  for (const item of tree) {
    const itemPath = item.path || ''
    if (itemPath === targetPath) {
      return [...parents, item]
    }
    if (item.children && item.children.length > 0) {
      const found = findBreadItemsByPath(item.children, targetPath, [...parents, item])
      if (found.length > 0)
        return found
    }
  }
  return []
}

function handleItemClick(item) {
  if (item.path) {
    router.push(item.path)
  }
}

function getDropdownOptions(children = []) {
  if (!children || !Array.isArray(children))
    return []
  return children
    .filter(item => item.path)
    .map(child => ({
      label: child.label || child.name,
      key: child.name,
      icon: child.icon ? () => h(IconRenderer, { icon: child.icon, size: 16 }) : undefined,
    }))
}

function handleDropdownSelect(key) {
  if (!key)
    return
  // 递归查找菜单项
  function findItem(tree) {
    if (!Array.isArray(tree))
      return null
    for (const item of tree) {
      if ((item.name || item.label) === String(key))
        return item
      if (item.children) {
        const found = findItem(item.children)
        if (found)
          return found
      }
    }
    return null
  }

  const item = findItem(permissionStore.menus)
  if (item?.path) {
    router.push({ path: item.path })
  }
}
</script>
