<script setup>
/**
 * BranchAddButton — 画布上的网关添加分支按钮
 *
 * 放在分支连线区域下方，保持结构编辑行为停留在画布上下文。
 */
const props = defineProps({
  position: { type: Object, required: true },
  readonly: { type: Boolean, default: false },
})

const emit = defineEmits(['click'])

function handleClick() {
  if (props.readonly)
    return
  emit('click')
}
</script>

<template>
  <div
    class="branch-add-button-wrap absolute z-20"
    :style="{
      left: `${position.x}px`,
      top: `${position.y}px`,
      transform: 'translate(-50%, -50%)',
    }"
  >
    <button
      type="button"
      class="branch-add-btn flex items-center justify-center bg-white transition-all duration-200"
      :class="{ 'opacity-40 cursor-not-allowed': readonly }"
      :disabled="readonly"
      data-test="canvas-add-branch"
      aria-label="添加条件分支"
      title="添加条件分支"
      @click.stop="handleClick"
    >
      <i class="i-lucide:plus text-sm" />
    </button>
  </div>
</template>

<style scoped>
.branch-add-btn {
  width: 20px;
  height: 20px;
  border: 1px solid var(--border-light);
  border-radius: 4px;
  color: var(--text-tertiary);
  cursor: pointer;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.1);
}

.branch-add-btn:hover:not(:disabled) {
  border-color: #2563eb;
  background: #eff6ff;
  color: #2563eb;
  box-shadow: 0 2px 6px rgba(37, 99, 235, 0.14);
}

.branch-add-btn:disabled {
  cursor: not-allowed;
}

@media (prefers-reduced-motion: reduce) {
  .branch-add-btn {
    transition: none;
  }

  .branch-add-btn:hover:not(:disabled) {
    transform: none;
  }
}
</style>
