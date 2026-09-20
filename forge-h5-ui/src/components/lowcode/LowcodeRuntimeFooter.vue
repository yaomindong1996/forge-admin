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
.runtime-footer-actions { position: sticky; bottom: 0; z-index: 2; display: flex; align-items: center; justify-content: stretch; gap: 12rpx; padding: 18rpx 0 calc(18rpx + env(safe-area-inset-bottom)); background: rgba(248, 250, 252, .95); }
.runtime-footer-actions > * { flex: 1; }
</style>
