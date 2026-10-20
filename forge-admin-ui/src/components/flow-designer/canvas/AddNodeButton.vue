<script setup>
import { useEventListener, useMediaQuery } from '@vueuse/core'
/** Popover 交给 Naive UI 定位/翻转/传送到 body，避免被画布 transform 和 overflow 裁掉。 */
import { NPopover } from 'naive-ui'
import { ref } from 'vue'
import { useFlowOverlayTarget } from '../composables/useFlowOverlayTarget.js'
import AddNodePopover from './AddNodePopover.vue'

defineProps({
  position: { type: Object, required: true },
  allowTypes: { type: Array, default: null },
  readonly: Boolean,
  label: { type: String, default: '' },
})
const emit = defineEmits(['select'])
const popoverVisible = ref(false)
const spaciousPopover = useMediaQuery('(min-width: 1100px)')
const overlayTarget = useFlowOverlayTarget()
useEventListener('resize', () => {
  popoverVisible.value = false
})
useEventListener('wheel', (event) => {
  // 画布缩放/平移后旧锚点已失效；浮层内滚动高级菜单则不关闭。
  if (!event.target?.closest?.('.n-popover'))
    popoverVisible.value = false
}, { capture: true, passive: true })
function handleSelect(type) {
  popoverVisible.value = false
  emit('select', type)
}
</script>

<template>
  <div
    class="add-node-button-wrap"
    :class="{ 'is-open': popoverVisible }"
    :style="{ left: `${position.x}px`, top: `${position.y}px` }"
  >
    <NPopover
      v-model:show="popoverVisible" trigger="click"
      :placement="spaciousPopover ? 'left' : 'bottom'" :disabled="readonly"
      :theme-overrides="{ space: '10px' }"
      :show-arrow="true" :style="{ padding: '14px' }"
      :animated="false"
      :to="overlayTarget || undefined"
    >
      <template #trigger>
        <button
          type="button" class="add-node-btn" :disabled="readonly"
          :aria-expanded="popoverVisible" :aria-label="`添加${label}节点`" :title="`添加${label}节点`"
          @keydown.esc="popoverVisible = false"
        >
          <i class="i-lucide:plus" />
        </button>
      </template>
      <div @keydown.esc="popoverVisible = false">
        <AddNodePopover :allow-types="allowTypes" @select="handleSelect" />
      </div>
    </NPopover>
  </div>
</template>

<style scoped>
.add-node-button-wrap {
  position: absolute;
  z-index: 20;
  width: 26px;
  height: 26px;
  transform: translate(-50%, -50%);
}
.add-node-button-wrap.is-open {
  z-index: 30;
}
.add-node-btn {
  display: inline-flex;
  width: 26px;
  height: 26px;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: 1px solid var(--primary-color, #356de0);
  border-radius: 50%;
  background: var(--bg-primary, #fff);
  color: var(--primary-color, #356de0);
  font-size: 16px;
  cursor: pointer;
  transition: background-color 160ms;
}
.add-node-btn:hover:not(:disabled),
.add-node-btn[aria-expanded='true'] {
  background: color-mix(in srgb, var(--primary-color, #356de0) 8%, var(--bg-primary, #fff));
}
.add-node-btn:focus-visible {
  outline: 2px solid var(--primary-color, #356de0);
  outline-offset: 3px;
}
.add-node-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
@media (prefers-reduced-motion: reduce) {
  .add-node-btn {
    transition: none;
  }
}
</style>
