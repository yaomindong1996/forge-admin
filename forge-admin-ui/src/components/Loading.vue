<template>
  <div
    :class="fullscreen ? 'mask-fullscreen' : 'mask'"
    :style="{
      zIndex: 9999,
      background: `rgba(${background})`,
      fontSize: `${fontSize}px`,
      color,
    }"
  >
    <div class="loading-container">
      <div class="spinner">
        <div class="spinner-ring" />
        <div class="spinner-ring" />
        <div class="spinner-ring" />
        <div class="spinner-ring" />
      </div>
      <div v-if="text" class="loading-text">
        {{ text }}
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted } from 'vue'

// 定义props
const props = defineProps({
  fullscreen: {
    type: Boolean,
    default: false,
  },
  background: {
    type: String,
    default: '0, 0, 0, 0.7', // 更暗的背景，更明显
  },
  text: {
    type: String,
    default: '加载中...',
  },
  color: {
    type: String,
    default: '#ffffff',
  },
  fontSize: {
    type: Number,
    default: 14,
  },
})

// 组件挂载时的处理
onMounted(() => {
  if (props.fullscreen) {
    document.body.style.overflow = 'hidden'
  }
})

// 组件销毁时的处理
onUnmounted(() => {
  if (props.fullscreen) {
    document.body.style.overflow = ''
  }
})
</script>

<style scoped>
.mask {
  position: absolute;
  z-index: 2000;
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: var(--font-size-base);
}

.mask-fullscreen {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  z-index: 9999;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: var(--font-size-base);
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.loading-text {
  margin-top: 16px;
  font-size: var(--font-size-base);
  font-weight: 500;
  color: v-bind(color);
}

.spinner {
  position: relative;
  width: 50px;
  height: 50px;
  animation: rotate 2s linear infinite;
}

.spinner-ring {
  position: absolute;
  width: 100%;
  height: 100%;
  border: 4px solid transparent;
  border-radius: 50%;
}

.spinner-ring:nth-child(1) {
  border-top-color: var(--base-primary-color);
  animation: spin 1.5s ease-in-out infinite;
}

.spinner-ring:nth-child(2) {
  border-top-color: var(--base-primary-color);
  animation: spin 1.5s ease-in-out infinite 0.1s;
}

.spinner-ring:nth-child(3) {
  border-top-color: var(--base-primary-color);
  animation: spin 1.5s ease-in-out infinite 0.2s;
}

.spinner-ring:nth-child(4) {
  border-top-color: var(--base-primary-color);
  animation: spin 1.5s ease-in-out infinite 0.3s;
}

@keyframes rotate {
  100% {
    transform: rotate(360deg);
  }
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
    opacity: 0.8;
  }
  50% {
    transform: rotate(180deg);
    opacity: 0.2;
  }
  100% {
    transform: rotate(360deg);
    opacity: 0.8;
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .loading-container {
    padding: 16px;
  }

  .spinner {
    width: 40px;
    height: 40px;
  }

  .loading-text {
    margin-top: 12px;
    font-size: var(--font-size-sm);
  }
}
</style>
