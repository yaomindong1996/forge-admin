<template>
  <Teleport to="body">
    <div v-if="visible" class="quick-action-overlay" @click.self="emit('close')" @keydown.escape="emit('close')">
      <div
        class="quick-action-ring"
        :style="{ left: `${position.x}px`, top: `${position.y}px` }"
        @click.stop
      >
        <div
          v-for="(action, index) in actions"
          :key="action.key"
          class="action-btn"
          :style="btnStyle(index)"
          @click="handleAction(action.key)"
        >
          <n-tooltip trigger="hover" placement="top">
            <template #trigger>
              <div class="btn-inner" :class="action.colorClass">
                <i :class="action.icon" />
              </div>
            </template>
            {{ action.label }}
          </n-tooltip>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
const props = defineProps({
  element: { type: Object, default: null },
  modeler: { type: Object, default: null },
  visible: { type: Boolean, default: false },
  position: { type: Object, default: () => ({ x: 0, y: 0 }) },
})

const emit = defineEmits(['delete', 'connect', 'change-type', 'copy', 'open-properties', 'close'])

const RING_RADIUS = 70

const actions = [
  { key: 'delete', label: '删除', icon: 'i-material-symbols:delete-outline', colorClass: 'color-red' },
  { key: 'connect', label: '连线', icon: 'i-material-symbols:timeline', colorClass: 'color-blue' },
  { key: 'change-type', label: '改类型', icon: 'i-material-symbols:swap-horiz', colorClass: 'color-purple' },
  { key: 'copy', label: '复制', icon: 'i-material-symbols:content-copy-outline', colorClass: 'color-green' },
  { key: 'open-properties', label: '属性', icon: 'i-material-symbols:settings-outline', colorClass: 'color-slate' },
]

function btnStyle(index) {
  const total = actions.length
  const angle = (index * (360 / total) - 90) * (Math.PI / 180)
  const x = Math.cos(angle) * RING_RADIUS
  const y = Math.sin(angle) * RING_RADIUS
  return {
    transform: `translate(${x}px, ${y}px)`,
    animationDelay: `${index * 40}ms`,
  }
}

function handleAction(key) {
  if (key === 'delete') {
    if (props.modeler && props.element) {
      const modeling = props.modeler.get('modeling')
      modeling.removeElements([props.element])
    }
  }
  emit(key)
  emit('close')
}
</script>

<style scoped>
.quick-action-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: rgba(0, 0, 0, 0.08);
}

.quick-action-ring {
  position: absolute;
  width: 0;
  height: 0;
}

.action-btn {
  position: absolute;
  width: 40px;
  height: 40px;
  margin-left: -20px;
  margin-top: -20px;
  animation: ring-pop 0.2s ease-out both;
}

@keyframes ring-pop {
  from {
    opacity: 0;
    transform: scale(0.3) translate(0, 0);
  }
  to {
    opacity: 1;
  }
}

.btn-inner {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #fff;
  border: 1px solid #e2e8f0;
  box-shadow: 0 2px 12px rgba(15, 23, 42, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-inner:hover {
  transform: scale(1.15);
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.18);
}

.btn-inner.color-red:hover {
  background: #ef4444;
  color: #fff;
  border-color: #ef4444;
}
.btn-inner.color-blue:hover {
  background: #3b82f6;
  color: #fff;
  border-color: #3b82f6;
}
.btn-inner.color-purple:hover {
  background: #8b5cf6;
  color: #fff;
  border-color: #8b5cf6;
}
.btn-inner.color-green:hover {
  background: #10b981;
  color: #fff;
  border-color: #10b981;
}
.btn-inner.color-slate:hover {
  background: #475569;
  color: #fff;
  border-color: #475569;
}
</style>
