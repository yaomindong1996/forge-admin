<template>
  <n-tooltip trigger="hover" placement="top">
    <template #trigger>
      <button
        type="button"
        class="ai-form-quick-create-trigger"
        :class="{ 'is-disabled': disabled }"
        :disabled="disabled"
        :aria-label="label"
        @click.stop="$emit('click', $event)"
      >
        <!-- 图标源文件：./quick-create-icon.svg -->
        <svg
          class="ai-form-quick-create-trigger__icon"
          viewBox="0 0 32 32"
          fill="none"
          xmlns="http://www.w3.org/2000/svg"
          aria-hidden="true"
        >
          <g
            stroke="currentColor"
            stroke-width="1.8"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path d="M22.4 16V9.2a1.5 1.5 0 0 0-.44-1.06l-5.5-5.5a1.5 1.5 0 0 0-1.06-.44H6.1a2.9 2.9 0 0 0-2.9 2.9v21a2.9 2.9 0 0 0 2.9 2.9h12.7" />
            <path d="M16 2.5v4.8a1.5 1.5 0 0 0 1.5 1.5h4.4" />
            <path d="m26.1 24.9 1.8 1.8-3.3 3.1-1.5-1.9" />
          </g>
          <g
            class="ai-form-quick-create-trigger__accent"
            stroke-width="1.8"
            stroke-linecap="round"
            stroke-linejoin="round"
            fill="none"
          >
            <path d="m18.4 16.6 6.5 3.1 1.2 4.6-3.5 2.8-3-2.2-1.2-8.3Z" />
            <path d="m18.4 16.6 3.4 4.5" />
          </g>
          <circle
            class="ai-form-quick-create-trigger__accent-dot"
            cx="21.8"
            cy="21.1"
            r=".9"
          />
        </svg>
      </button>
    </template>
    {{ label }}
  </n-tooltip>
</template>

<script setup>
defineProps({
  label: {
    type: String,
    required: true,
  },
  disabled: {
    type: Boolean,
    default: false,
  },
})

defineEmits(['click'])
</script>

<style scoped>
.ai-form-quick-create-trigger {
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  align-self: center;
  box-sizing: border-box;
  width: 24px;
  height: 24px;
  margin: 0;
  padding: 0;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: var(--text-secondary, #64748b);
  cursor: pointer;
  line-height: 0;
  transition:
    background-color 0.15s ease,
    color 0.15s ease;
}

.ai-form-quick-create-trigger__icon {
  display: block;
  width: 15px;
  height: 15px;
  flex-shrink: 0;
}

.ai-form-quick-create-trigger__accent {
  stroke: var(--primary-color, #2680ff);
}

.ai-form-quick-create-trigger__accent-dot {
  fill: var(--primary-color, #2680ff);
}

.ai-form-quick-create-trigger:hover:not(.is-disabled):not(:disabled),
.ai-form-quick-create-trigger:focus-visible:not(.is-disabled):not(:disabled) {
  background: color-mix(in srgb, var(--primary-color, #2563eb) 14%, transparent);
  color: var(--text-primary, #334155);
}

.ai-form-quick-create-trigger:focus-visible {
  outline: 2px solid var(--primary-color, #2563eb);
  outline-offset: 2px;
}

.ai-form-quick-create-trigger.is-disabled,
.ai-form-quick-create-trigger:disabled {
  background: transparent;
  color: var(--text-disabled, #c2c2c2);
  cursor: not-allowed;
  opacity: 0.55;
}

.ai-form-quick-create-trigger.is-disabled .ai-form-quick-create-trigger__accent,
.ai-form-quick-create-trigger:disabled .ai-form-quick-create-trigger__accent {
  stroke: var(--text-disabled, #c2c2c2);
}

.ai-form-quick-create-trigger.is-disabled .ai-form-quick-create-trigger__accent-dot,
.ai-form-quick-create-trigger:disabled .ai-form-quick-create-trigger__accent-dot {
  fill: var(--text-disabled, #c2c2c2);
}
</style>
