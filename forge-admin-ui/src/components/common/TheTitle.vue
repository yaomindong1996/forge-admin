<template>
  <router-link to="/" class="text-14 mr-10 cursor-pointer color-[#ffffff] transition-opacity hover:opacity-80">
    {{ systemName }}
  </router-link>
</template>

<script setup>
import { computed } from 'vue'
import { useTenantStore } from '@/store'

const tenantStore = useTenantStore()

// 优先使用租户配置的系统名称，否则使用默认值
const systemName = computed(() => {
  return tenantStore.systemName || import.meta.env.VITE_TITLE
})
</script>
