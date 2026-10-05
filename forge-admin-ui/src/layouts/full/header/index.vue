<template>
  <div class="layout-header layout-chrome-header flex items-center px-12" border-b="1px solid light_border dark:dark_border">
    <MenuCollapse />

    <div class="header-breadcrumb-wrap">
      <BreadCrumb />
    </div>

    <HeaderTools class="header-tools-wrap" />
  </div>
</template>

<script setup>
import { BreadCrumb, MenuCollapse } from '@/layouts/components'
import HeaderTools from '@/layouts/components/HeaderTools.vue'
</script>

<style scoped>
.layout-header {
  width: 100%;
  height: 50px;
  flex-shrink: 0;
  background: var(--bg-primary);
  border-bottom: 1px solid var(--border-light);
}

.header-breadcrumb-wrap {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  margin-left: 12px;
}

.header-tools-wrap {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
  padding-left: 12px;
}
</style>
