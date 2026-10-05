<template>
  <div class="modern-side-header">
    <div class="logo-link">
      <div class="logo-wrapper">
        <TheLogo />
      </div>
      <transition name="fade-slide">
        <TheTitle v-show="!appStore.collapsed" class="system-name" />
      </transition>
    </div>
  </div>
</template>

<script setup>
import TheTitle from '@/components/common/TheTitle.vue'
import { useAppStore } from '@/store'

const appStore = useAppStore()
</script>

<style scoped>
.modern-side-header {
  --brand-title-text-color: var(--side-menu-text-color);
  height: 52px;
  padding: 0 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: var(--side-menu-bg-color);
}

.logo-link {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  overflow: hidden;
}

.logo-wrapper {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-md);
  flex-shrink: 0;
  background: transparent;
}

.logo-wrapper :deep(img) {
  width: 28px;
  height: 28px;
  object-fit: contain;
}

.system-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--side-menu-text-color);
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  letter-spacing: 0;
}

/* 过渡动画 */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition:
    opacity var(--transition-fast),
    transform var(--transition-fast);
}

.fade-slide-enter-from,
.fade-slide-leave-to {
  opacity: 0;
  transform: translateX(-6px);
}
</style>
