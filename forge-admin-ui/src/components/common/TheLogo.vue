<template>
  <router-link to="/" class="w-30 cursor-pointer">
    <AuthImage :src="tenantStore.systemLogo" :fallback="defaultLogo" alt="Logo" />
  </router-link>
</template>

<script setup>
import defaultLogo from '@/assets/images/logo.png'
import { useTenantStore } from '@/store'
import AuthImage from './AuthImage.vue'

const tenantStore = useTenantStore()
</script>
