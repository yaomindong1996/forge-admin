<template>
  <div class="designer-async-loader" :class="{ 'is-overlay': overlay }" role="status" aria-live="polite">
    <div class="loader-copy">
      <strong>{{ title }}</strong>
      <span>{{ description }}</span>
    </div>
    <div class="loader-track">
      <span class="loader-segment" />
    </div>
  </div>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    default: '正在打开设计器',
  },
  description: {
    type: String,
    default: '首次加载需要准备组件资源',
  },
  overlay: {
    type: Boolean,
    default: false,
  },
})
</script>

<style scoped>
.designer-async-loader {
  display: grid;
  gap: 12px;
  width: min(360px, 100%);
  margin: 56px auto;
  padding: 14px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
}

.designer-async-loader.is-overlay {
  width: min(400px, calc(100% - 40px));
  margin: 0 auto;
}

.loader-copy {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.loader-copy strong {
  color: #18181b;
  font-size: 13px;
  font-weight: 600;
  line-height: 20px;
}

.loader-copy span {
  color: #71717a;
  font-size: 12px;
  line-height: 18px;
}

.loader-track {
  position: relative;
  height: 3px;
  overflow: hidden;
  border-radius: 999px;
  background: #e5e7eb;
}

.loader-segment {
  position: absolute;
  top: 0;
  left: -40%;
  height: 100%;
  width: 40%;
  border-radius: inherit;
  background: #2563eb;
  animation: loader-slide 1.4s ease-in-out infinite;
}

@keyframes loader-slide {
  0% {
    transform: translateX(0);
  }

  100% {
    transform: translateX(350%);
  }
}
</style>
