<template>
  <!-- 只读详情（我发起的）：任务运行中且已有办理人、并有催办权限时显示 -->
  <view v-if="showRemind(task)" class="remind-bar">
    <AiButton
      block
      size="sm"
      variant="secondary"
      :loading="isReminding(task)"
      :disabled="isRemindCoolingDown(task)"
      @click="remind(task)"
    >
      {{ isRemindCoolingDown(task) ? '已催办，10 分钟内不可重复' : '催办审批人' }}
    </AiButton>
  </view>
</template>

<script setup>
import { storeToRefs } from 'pinia'
import AiButton from '@/components/AiButton.vue'
import { useFlowRemind } from '@/composables/flow/useFlowRemind'
import { useTodoDetailStore } from '@/store'

const { task } = storeToRefs(useTodoDetailStore())
const { showRemind, isRemindCoolingDown, isReminding, remind } = useFlowRemind()
</script>

<style lang="scss" scoped>
.remind-bar { position: relative; z-index: 5; padding: 8px 12px calc(8px + env(safe-area-inset-bottom)); border-top: 1px solid var(--forge-border); background: var(--forge-surface); }
.remind-bar :deep(.ai-button) { min-height: 44px; border-radius: var(--forge-radius-control) !important; font-size: 16px; }

@media (min-width: 1024px) {
  .remind-bar { display: flex; justify-content: flex-end; padding-right: max(24px, calc((100vw - 1280px) / 2 + 24px)); padding-left: max(24px, calc((100vw - 1280px) / 2 + 24px)); }
  .remind-bar :deep(.ai-button) { width: 200px; }
}
</style>
