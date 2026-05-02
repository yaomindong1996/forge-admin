<template>
  <div v-if="visible" class="shortcuts-bar">
    <div class="shortcuts-inner">
      <div class="shortcut-item">
        <kbd>Ctrl+Z</kbd>
        <span>撤销</span>
      </div>
      <div class="shortcut-item">
        <kbd>Ctrl+Y</kbd>
        <span>重做</span>
      </div>
      <div class="shortcut-item">
        <kbd>Ctrl+C</kbd>
        <span>复制</span>
      </div>
      <div class="shortcut-item">
        <kbd>Ctrl+V</kbd>
        <span>粘贴</span>
      </div>
      <div class="shortcut-item">
        <kbd>Delete</kbd>
        <span>删除</span>
      </div>
      <div class="shortcut-item">
        <kbd>Ctrl+A</kbd>
        <span>全选</span>
      </div>
      <div class="shortcut-item">
        <kbd>Ctrl+滚轮</kbd>
        <span>缩放</span>
      </div>
      <div class="shortcut-item">
        <kbd>Space+拖拽</kbd>
        <span>平移</span>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  visible: {
    type: Boolean,
    default: true,
  },
})
</script>

<style scoped>
.shortcuts-bar {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 32px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(4px);
  border-top: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
}

.shortcuts-inner {
  display: flex;
  align-items: center;
  gap: 12px;
}

.shortcut-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: #64748b;
}

.shortcut-item kbd {
  padding: 1px 5px;
  font-size: 10px;
  font-family: 'Fira Code', 'Consolas', monospace;
  background: #f1f5f9;
  border: 1px solid #cbd5e1;
  border-radius: 3px;
  color: #334155;
  min-width: 20px;
  text-align: center;
}

.shortcut-item span {
  white-space: nowrap;
}
</style>
