<template>
  <router-link :to="homePath" class="logo-container">
    <AuthImage :src="tenantStore.systemLogo" :fallback="defaultLogo" alt="Logo" class="logo-image" />
  </router-link>
</template>

<script setup>
import { computed } from 'vue'
import defaultLogo from '@/assets/images/logo.png'
import { useTenantStore } from '@/store'
import { getHomePath } from '@/utils/home-path'
import AuthImage from './AuthImage.vue'

const tenantStore = useTenantStore()
const homePath = computed(() => getHomePath())
</script>

<style scoped>
.logo-container {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  cursor: pointer;
  flex-shrink: 0;
  border-radius: 4px;
  transition: opacity 0.16s ease;
}

.logo-container:hover {
  opacity: 0.86;
}

.logo-image {
  max-width: 28px;
  max-height: 28px;
  object-fit: contain;
}
</style>
