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
  padding: 20px 16px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.logo-link {
  display: flex;
  align-items: center;
  gap: 12px;
  text-decoration: none;
  transition: all 0.3s ease;
}

.logo-link:hover .logo-wrapper {
  transform: scale(1.05);
}

.logo-wrapper {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
  transition: transform 0.3s ease;
  flex-shrink: 0;
}

.logo-wrapper :deep(img) {
  width: 28px;
  height: 28px;
  object-fit: contain;
}

.system-name {
  font-size: 16px;
  font-weight: 700;
  color: #1E293B;
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-family: 'Poppins', sans-serif;
}

/* 过渡动画 */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.3s ease;
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateX(-10px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateX(-10px);
}

@media (prefers-reduced-motion: reduce) {
  .logo-wrapper,
  .fade-slide-enter-active,
  .fade-slide-leave-active {
    transition: none;
  }
}
</style>
