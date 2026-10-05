<template>
  <router-link :to="homePath" class="the-title">
    {{ systemName }}
  </router-link>
</template>

<script setup>
import { computed } from 'vue'
import { useTenantStore } from '@/store'
import { getHomePath } from '@/utils/home-path'
import { getDefaultPageTitle } from '@/utils/page-title'

const tenantStore = useTenantStore()
const homePath = computed(() => getHomePath())

// 优先使用租户配置的系统名称，否则使用默认值
const systemName = computed(() => {
  return tenantStore.systemName || getDefaultPageTitle()
})
</script>

<style scoped>
.the-title {
  display: block;
  min-width: 0;
  overflow: hidden;
  color: var(--brand-title-text-color, var(--layout-header-text-color, #ffffff));
  font-size: 16px;
  font-weight: 600;
  line-height: 1.2;
  text-decoration: none;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
  transition: opacity 0.16s ease;
}

.the-title:hover {
  opacity: 0.82;
}

.the-title:focus-visible {
  outline: 1px solid currentColor;
  outline-offset: 3px;
}
</style>
