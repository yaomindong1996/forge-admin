<template>
  <view class="runtime-footer-actions">
    <AiButton variant="secondary" @click="$emit('cancel')">{{ mode === 'detail' ? '返回列表' : '取消' }}</AiButton>
    <AiButton v-if="mode !== 'detail'" :loading="saving" @click="$emit('save')">保存</AiButton>
    <AiButton v-else-if="canEdit" @click="$emit('edit')">编辑</AiButton>
  </view>
</template>

<script setup>
import AiButton from '@/components/AiButton.vue'
defineProps({
  mode: { type: String, default: 'detail' },
  saving: { type: Boolean, default: false },
  canEdit: { type: Boolean, default: false },
})
defineEmits(['cancel', 'save', 'edit'])
</script>

<style lang="scss" scoped>
.runtime-footer-actions { position: sticky; bottom: 0; z-index: 2; display: flex; flex-direction: column-reverse; align-items: stretch; justify-content: stretch; gap: 16rpx; padding: 32rpx 0 calc(32rpx + env(safe-area-inset-bottom)); border-top: 1rpx solid var(--border-light); background: #fff; }
.runtime-footer-actions > * { flex: 1; }

@media (min-width: 1024px) {
  .runtime-footer-actions { flex-direction: row; justify-content: flex-end; padding: 16px 0; }
  .runtime-footer-actions > * { flex: 0 0 auto; min-width: 128px; }
}
</style>
