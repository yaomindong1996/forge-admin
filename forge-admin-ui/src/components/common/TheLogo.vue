<template>
  <router-link to="/" class="logo-container">
    <AuthImage :src="tenantStore.systemLogo" :fallback="defaultLogo" alt="Logo" class="logo-image" />
  </router-link>
</template>

<script setup>
import defaultLogo from '@/assets/images/logo.png'
import { useTenantStore } from '@/store'
import AuthImage from './AuthImage.vue'

const tenantStore = useTenantStore()
</script>

<style scoped>
.logo-container {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  cursor: pointer;
  flex-shrink: 0;
}

.logo-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}
</style>
