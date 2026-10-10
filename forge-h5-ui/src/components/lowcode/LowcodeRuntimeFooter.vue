<template>
  <view class="runtime-footer-actions" :class="{ 'has-flow-actions': flowButtons.length }">
    <AiButton variant="secondary" @click="$emit('cancel')">{{ mode === 'detail' ? '返回列表' : '取消' }}</AiButton>
    <AiButton v-if="mode !== 'detail'" :variant="flowButtons.length ? 'secondary' : 'primary'" :loading="saving" @click="$emit('save')">保存</AiButton>
    <AiButton v-else-if="canEdit" :variant="flowButtons.length ? 'secondary' : 'primary'" @click="$emit('edit')">编辑</AiButton>
    <!-- 单据审批按钮由后端运行态决定，见 documentFlow store -->
    <AiButton
      v-for="button in flowButtons"
      :key="button.key"
      :variant="button.variant"
      :disabled="button.disabled || saving"
      :loading="documentFlowStore.loadingKey === button.key"
      @click="$emit('flow-action', button)"
    >
      {{ button.label }}
    </AiButton>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import AiButton from '@/components/AiButton.vue'
import { useDocumentFlowStore } from '@/store'

const props = defineProps({
  mode: { type: String, default: 'detail' },
  saving: { type: Boolean, default: false },
  canEdit: { type: Boolean, default: false },
})
defineEmits(['cancel', 'save', 'edit', 'flow-action'])
const documentFlowStore = useDocumentFlowStore()
const flowButtons = computed(() => documentFlowStore.footerButtons(props.mode))
</script>

<style lang="scss" scoped>
.runtime-footer-actions { position: sticky; bottom: 0; z-index: 2; display: flex; flex-direction: column-reverse; align-items: stretch; justify-content: stretch; gap: 16rpx; padding: 32rpx 0 calc(32rpx + env(safe-area-inset-bottom)); border-top: 1rpx solid var(--border-light); background: #fff; }
.runtime-footer-actions > * { flex: 1; }
.runtime-footer-actions.has-flow-actions { flex-direction: row; }
.runtime-footer-actions.has-flow-actions > * { min-width: 0; }

@media (min-width: 1024px) {
  .runtime-footer-actions { flex-direction: row; justify-content: flex-end; padding: 16px 0; }
  .runtime-footer-actions > * { flex: 0 0 auto; min-width: 128px; }
}
</style>
