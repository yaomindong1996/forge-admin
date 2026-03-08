<template>
  <router-link to="/" class="w-30 cursor-pointer">
    <AuthImage :src="tenantStore.systemLogo" :fallback="defaultLogo" alt="Logo" />
  </router-link>
</template>

<script setup>
import { useTenantStore } from '@/store'
import AuthImage from './AuthImage.vue'
import defaultLogo from '@/assets/images/logo.png'

const tenantStore = useTenantStore()
</script>
