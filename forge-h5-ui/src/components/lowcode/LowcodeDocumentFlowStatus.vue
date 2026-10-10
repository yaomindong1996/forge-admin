<template>
  <view v-if="store.visible" class="document-flow-status">
    <view class="document-flow-status__head">
      <text class="document-flow-status__label">审批状态</text>
      <AiTag v-if="store.statusText" :type="store.statusTone" size="sm">{{ store.statusText }}</AiTag>
    </view>
    <text v-if="store.message" class="document-flow-status__message">{{ store.message }}</text>
    <text v-if="store.disabledReason || store.hint" class="document-flow-status__hint">
      {{ store.disabledReason || store.hint }}
    </text>
    <!-- 页面使用自定义底部栏时没有默认页脚，审批按钮放在状态卡内 -->
    <view v-if="showActions && store.buttons.length" class="document-flow-status__actions">
      <AiButton
        v-for="button in store.buttons"
        :key="button.key"
        :variant="button.variant"
        size="sm"
        :disabled="button.disabled"
        :loading="store.loadingKey === button.key"
        @click="emit('action', button)"
      >
        {{ button.label }}
      </AiButton>
    </view>
  </view>
</template>

<script setup>
import AiButton from '@/components/AiButton.vue'
import AiTag from '@/components/AiTag.vue'
import { useDocumentFlowStore } from '@/store'

defineProps({
  showActions: { type: Boolean, default: false },
})
const emit = defineEmits(['action'])
const store = useDocumentFlowStore()
</script>

<style lang="scss" scoped>
.document-flow-status {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 12px;
  padding: 14px 16px;
  border-radius: var(--forge-radius-card);
  background: var(--forge-surface);
}
.document-flow-status__head { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.document-flow-status__label { color: var(--forge-text-secondary); font-size: 14px; }
.document-flow-status__message { color: var(--forge-text-primary); font-size: 14px; line-height: 1.5; }
.document-flow-status__hint { color: var(--forge-text-tertiary); font-size: 12px; line-height: 1.5; }
.document-flow-status__actions { display: flex; flex-wrap: wrap; justify-content: flex-end; gap: 8px; margin-top: 4px; }
</style>
