<template>
  <view class="action-bar">
    <AiButton v-if="isCandidateTask" block size="sm" :loading="claimLoading" @click="emit('claim')">签收后处理</AiButton>
    <template v-else>
      <button v-if="hasMoreActions" class="action-more-button" :disabled="blocked || actionLoading" @click="moreVisible = true">
        <AiIcon icon="/static/icons/ai-icon/more-horizontal.svg" color="#747677" size="sm" />
        <text>更多</text>
      </button>
      <AiButton
        v-if="canReject"
        size="sm"
        variant="danger"
        :loading="actionLoading && pendingAction === 'reject'"
        :disabled="blocked || actionLoading"
        @click="emit('reject')"
      >
        驳回
      </AiButton>
      <AiButton v-if="canApprove" size="sm" :loading="actionLoading && pendingAction === 'approve'" :disabled="blocked || actionLoading" @click="emit('approve')">同意</AiButton>
    </template>
  </view>
</template>

<script setup>
import { storeToRefs } from 'pinia'
import AiButton from '@/components/AiButton.vue'
import AiIcon from '@/components/AiIcon.vue'
import { useTodoDetailStore } from '@/store'

defineProps({
  /** 表单无法在移动端办理时禁用全部动作 */
  blocked: { type: Boolean, default: false },
})
const emit = defineEmits(['claim', 'reject', 'approve'])

const {
  isCandidateTask, claimLoading, hasMoreActions, moreVisible, actionLoading, pendingAction, canReject, canApprove,
} = storeToRefs(useTodoDetailStore())
</script>

<style lang="scss" scoped>
/* 钉钉审批底栏：更多 | 驳回（灰底次按钮）| 同意（主按钮），按钮等高同圆角 */
.action-bar { position: relative; z-index: 5; display: flex; align-items: center; gap: 8px; padding: 8px 12px calc(8px + env(safe-area-inset-bottom)); border-top: 1px solid var(--forge-border); background: var(--forge-surface); box-shadow: none; }
.action-bar :deep(.ai-button) { min-width: 0; min-height: 44px; flex: 1; padding: 0 12px; border-radius: var(--forge-radius-control) !important; font-size: 16px; }
.action-bar :deep(.ai-button--block) { width: 100%; }
.action-bar :deep(.ai-button--danger) { flex: 1; color: var(--forge-text-primary) !important; background: var(--forge-surface-muted) !important; }
.action-more-button { display: flex; width: 48px; min-height: 44px; flex: 0 0 48px; flex-direction: column; align-items: center; justify-content: center; gap: 2px; margin: 0; padding: 0; border: 0; border-radius: var(--forge-radius-control); color: var(--forge-text-secondary); font-size: 10px; line-height: 1.2; background: transparent; }
.action-more-button::after { border: 0; }
.action-more-button:active { background: var(--surface-muted); }
.action-more-button[disabled] { opacity: .5; }

@media (min-width: 1024px) {
  .action-bar {
    justify-content: flex-end;
    padding-right: max(24px, calc((100vw - 1280px) / 2 + 24px));
    padding-left: max(24px, calc((100vw - 1280px) / 2 + 24px));
  }

  .action-bar :deep(.ai-button),
  .action-bar :deep(.ai-button--primary) {
    width: auto;
    flex: 0 0 auto;
    min-width: 128px;
  }
}
</style>
