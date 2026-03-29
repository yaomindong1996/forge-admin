<template>
  <div class="modern-side-header">
    <router-link class="logo-link" to="/">
      <div class="logo-wrapper">
        <TheLogo />
      </div>
      <transition name="fade-slide">
        <h2 v-show="!appStore.collapsed" class="system-name">
          {{ systemName }}
        </h2>
      </transition>
    </router-link>
  </div>
</template>

<script setup>
import { useAppStore, useTenantStore } from '@/store'

const tenantStore = useTenantStore()
const appStore = useAppStore()

const systemName = computed(() => {
  return tenantStore.systemName || import.meta.env.VITE_TITLE
})
</script>

<style scoped>
.modern-side-header {
  height: 52px;
  padding: 0 16px;
  display: flex;
  align-items: center;
  border-bottom: 1px solid var(--border-light);
  flex-shrink: 0;
  background: var(--bg-primary);
}

.logo-link {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  overflow: hidden;
}

.logo-wrapper {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-md);
  flex-shrink: 0;
  background: var(--primary-500);
  box-shadow: 0 2px 6px rgba(22, 93, 255, 0.25);
}

.logo-wrapper :deep(img) {
  width: 16px;
  height: 16px;
  object-fit: contain;
  filter: brightness(10);
}

.system-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  letter-spacing: 0;
}

/* 过渡动画 */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: opacity var(--transition-fast), transform var(--transition-fast);
}

.fade-slide-enter-from,
.fade-slide-leave-to {
  opacity: 0;
  transform: translateX(-6px);
}
</style>
