<template>
  <div
    class="ai-form-section-title"
    :data-ai-section="anchorId || undefined"
  >
    <span class="ai-form-section-title__rail" aria-hidden="true" />
    <div class="ai-form-section-title__content">
      <div class="ai-form-section-title__main">
        <span class="ai-form-section-title__text">{{ title }}</span>
        <span v-if="badge" class="ai-form-section-title__badge">{{ badge }}</span>
      </div>
      <p v-if="description" class="ai-form-section-title__description">
        {{ description }}
      </p>
    </div>
    <span class="ai-form-section-title__line" aria-hidden="true" />
  </div>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    default: '分组信息',
  },
  description: {
    type: String,
    default: '',
  },
  badge: {
    type: String,
    default: '',
  },
  anchorId: {
    type: String,
    default: '',
  },
})
</script>

<style scoped>
.ai-form-section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  min-width: 0;
  margin: 4px 0 6px;
  padding: 0;
  border: 0;
  background: transparent;
}

.ai-form-section-title__rail {
  width: 3px;
  height: 16px;
  flex: 0 0 3px;
  border-radius: 2px;
  background: var(--primary-color, #2563eb);
}

.ai-form-section-title__content {
  min-width: 0;
  flex: 0 1 auto;
}

.ai-form-section-title__main {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.ai-form-section-title__text {
  overflow: hidden;
  color: var(--text-primary, #111827);
  font-size: 13px;
  font-weight: 600;
  line-height: 20px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ai-form-section-title__badge {
  flex: 0 0 auto;
  padding: 1px 6px;
  border-radius: 4px;
  background: var(--bg-secondary, #f6f8fb);
  color: var(--text-tertiary, #9ca3af);
  font-size: 11px;
  font-weight: 500;
  line-height: 18px;
}

.ai-form-section-title__description {
  margin: 1px 0 0;
  color: var(--text-tertiary, #86909c);
  font-size: 12px;
  line-height: 18px;
}

.ai-form-section-title__line {
  height: 1px;
  min-width: 24px;
  flex: 1 1 auto;
  background: var(--border-light, #e5e7eb);
}

:global(.dark) .ai-form-section-title {
  background: transparent;
}

:global(.dark) .ai-form-section-title__badge {
  background: #162033;
  color: #94a3b8;
}

@media (max-width: 640px) {
  .ai-form-section-title {
    align-items: flex-start;
    gap: 8px;
  }

  .ai-form-section-title__line {
    display: none;
  }
}
</style>
