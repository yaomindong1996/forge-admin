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
  gap: 10px;
  width: 100%;
  min-width: 0;
  margin: 6px 0 8px;
  padding: 8px 10px;
  border: 1px solid rgba(22, 93, 255, 0.12);
  border-radius: 6px;
  background:
    linear-gradient(135deg, rgba(22, 93, 255, 0.08), rgba(22, 93, 255, 0.02) 46%, rgba(255, 255, 255, 0)),
    var(--bg-secondary, #f7f8fa);
}

.ai-form-section-title__rail {
  width: 4px;
  height: 28px;
  flex: 0 0 4px;
  border-radius: 2px;
  background: linear-gradient(180deg, #6aa1ff 0%, #67e8f9 100%);
  box-shadow: 0 0 0 3px rgba(64, 128, 255, 0.06);
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
  color: var(--text-primary, #1d2129);
  font-size: 13px;
  font-weight: 700;
  line-height: 20px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ai-form-section-title__badge {
  flex: 0 0 auto;
  padding: 1px 6px;
  border-radius: 4px;
  background: rgba(22, 93, 255, 0.1);
  color: #165dff;
  font-size: 11px;
  font-weight: 600;
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
  background: linear-gradient(90deg, rgba(22, 93, 255, 0.18), rgba(22, 93, 255, 0));
}

:global(.dark) .ai-form-section-title {
  border-color: rgba(64, 128, 255, 0.22);
  background:
    linear-gradient(135deg, rgba(64, 128, 255, 0.16), rgba(20, 184, 166, 0.06) 46%, rgba(15, 23, 42, 0)),
    var(--bg-secondary, #1e293b);
}

:global(.dark) .ai-form-section-title__badge {
  background: rgba(64, 128, 255, 0.16);
  color: #94bfff;
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
