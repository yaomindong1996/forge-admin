<script setup>
defineProps({
  title: { type: String, default: '属性设置' },
  description: { type: String, default: '' },
  icon: { type: String, default: 'i-material-symbols:tune' },
  empty: { type: Boolean, default: false },
  emptyText: { type: String, default: '请先选择一个节点。' },
  bodyScrollable: { type: Boolean, default: true },
})

defineEmits(['close'])
</script>

<template>
  <div class="flow-property-panel-shell">
    <div class="flow-property-panel-shell__header">
      <div class="flow-property-panel-shell__heading">
        <span class="flow-property-panel-shell__icon">
          <i :class="icon" />
        </span>
        <span class="flow-property-panel-shell__text">
          <span class="flow-property-panel-shell__title">{{ title || '属性设置' }}</span>
          <span v-if="description" class="flow-property-panel-shell__desc">{{ description }}</span>
        </span>
      </div>
      <button type="button" class="flow-property-panel-shell__close" @click="$emit('close')">
        <i class="i-material-symbols:close" />
      </button>
    </div>

    <div class="flow-property-panel-shell__body" :class="{ 'is-scrollable': bodyScrollable }">
      <div v-if="!empty" class="flow-property-panel-shell__content">
        <slot />
      </div>
      <div v-else class="flow-property-panel-shell__empty">
        {{ emptyText }}
      </div>
    </div>

    <div v-if="$slots.footer" class="flow-property-panel-shell__footer">
      <slot name="footer" />
    </div>
  </div>
</template>

<style scoped>
.flow-property-panel-shell {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: var(--bg-primary);
  color: var(--text-primary);
}

.flow-property-panel-shell__header {
  min-height: 48px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 9px 14px;
  border-bottom: 1px solid var(--border-light);
  background: var(--bg-secondary);
}

.flow-property-panel-shell__heading {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.flow-property-panel-shell__icon {
  width: 28px;
  height: 28px;
  flex: 0 0 auto;
  border-radius: 4px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-tertiary);
  color: var(--text-secondary);
  font-size: 16px;
}

.flow-property-panel-shell__text {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.flow-property-panel-shell__title {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 600;
}

.flow-property-panel-shell__desc {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.25;
}

.flow-property-panel-shell__close {
  width: 28px;
  height: 28px;
  flex: 0 0 auto;
  border: none;
  border-radius: 4px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  color: var(--text-tertiary);
  cursor: pointer;
  transition:
    background 150ms ease,
    color 150ms ease;
}

.flow-property-panel-shell__close:hover {
  background: var(--bg-tertiary);
  color: var(--text-primary);
}

.flow-property-panel-shell__body {
  flex: 1;
  min-height: 0;
  background: var(--bg-primary);
}

.flow-property-panel-shell__body.is-scrollable {
  overflow-y: auto;
}

.flow-property-panel-shell__content {
  min-height: 100%;
}

.flow-property-panel-shell__footer {
  flex-shrink: 0;
  padding: 12px 14px;
  border-top: 1px solid var(--border-light);
  background: var(--bg-secondary);
}

.flow-property-panel-shell__empty {
  margin: 18px;
  border-radius: 4px;
  padding: 16px;
  background: var(--bg-secondary);
  color: var(--text-tertiary);
  font-size: 13px;
}

.flow-property-panel-shell__content :deep(.n-tabs .n-tabs-nav) {
  min-height: 42px;
  padding: 0;
  border-bottom: 1px solid var(--border-light);
  background: var(--bg-primary);
}

.flow-property-panel-shell__content :deep(.n-tabs .n-tabs-nav-scroll-wrapper),
.flow-property-panel-shell__content :deep(.n-tabs .n-tabs-nav-scroll-content) {
  overflow-x: auto;
}

.flow-property-panel-shell__content :deep(.n-tabs .n-tabs-nav-scroll-content) {
  min-width: max-content;
  padding: 0 14px;
  mask-image: linear-gradient(to right, #000 0, #000 calc(100% - 28px), transparent 100%);
}

.flow-property-panel-shell__content :deep(.n-tabs .n-tabs-tab) {
  min-height: 42px;
  padding: 0 10px;
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.flow-property-panel-shell__content :deep(.n-tabs .n-tabs-tab.n-tabs-tab--active) {
  color: #2563eb;
}

.flow-property-panel-shell__content :deep(.n-tabs .n-tabs-bar) {
  background-color: #2563eb;
}

.flow-property-panel-shell__content :deep(.n-tabs .n-tab-pane) {
  padding: 14px 16px 18px;
}

.flow-property-panel-shell__content :deep(.n-form-item) {
  margin-bottom: 12px;
}

.flow-property-panel-shell__content :deep(.n-form-item:last-child) {
  margin-bottom: 0;
}

.flow-property-panel-shell__content :deep(.n-form-item .n-form-item-label) {
  min-height: 22px;
  padding: 0 0 6px;
  color: var(--text-primary);
  font-size: 12px;
  font-weight: 600;
}

.flow-property-panel-shell__content :deep(.n-form-item .n-form-item-label__asterisk) {
  color: #ef4444;
}

.flow-property-panel-shell__content :deep(.n-input),
.flow-property-panel-shell__content :deep(.n-base-selection),
.flow-property-panel-shell__content :deep(.n-input-number) {
  width: 100%;
}

.flow-property-panel-shell__content :deep(.n-input .n-input-wrapper),
.flow-property-panel-shell__content :deep(.n-base-selection .n-base-selection-label) {
  min-height: 32px;
}

.flow-property-panel-shell__content :deep(.n-input__input-el),
.flow-property-panel-shell__content :deep(.n-base-selection-input),
.flow-property-panel-shell__content :deep(.n-base-selection-placeholder) {
  font-size: 12px;
}

.flow-property-panel-shell__content :deep(.basic-config),
.flow-property-panel-shell__content :deep(.space-y-2),
.flow-property-panel-shell__content :deep(.space-y-3) {
  padding: 16px 18px 20px;
}

.flow-property-panel-shell__content :deep(.n-tab-pane .basic-config),
.flow-property-panel-shell__content :deep(.n-tab-pane .space-y-2),
.flow-property-panel-shell__content :deep(.n-tab-pane .space-y-3) {
  padding: 0;
}

.flow-property-panel-shell__content :deep(.config-section-block) {
  margin-top: 18px;
}

.flow-property-panel-shell__content :deep(.config-section-title) {
  margin-bottom: 10px;
  color: var(--text-primary);
  font-size: 12px;
  font-weight: 600;
}

.flow-property-panel-shell__content :deep(.config-hint) {
  border: 1px solid var(--border-light);
  border-radius: 4px;
  padding: 10px 12px;
  background: var(--bg-secondary);
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.7;
}

@media (max-width: 640px) {
  .flow-property-panel-shell__content :deep(.n-tabs .n-tabs-nav) {
    padding: 0 12px;
  }

  .flow-property-panel-shell__content :deep(.n-tabs .n-tab-pane),
  .flow-property-panel-shell__content :deep(.basic-config),
  .flow-property-panel-shell__content :deep(.space-y-2),
  .flow-property-panel-shell__content :deep(.space-y-3) {
    padding: 14px 16px 18px;
  }
}
</style>
