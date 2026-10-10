<template>
  <AiPopupSheet v-model="moreVisible" title="更多操作">
    <view class="more-action-grid">
      <button v-if="canRejectToStart" class="more-action-item warning" @click="emit('action', 'rejectToStart')">
        <view class="more-action-item__icon"><AiIcon icon="/static/icons/ai-icon/rotate-ccw.svg" color="#ff7d00" size="sm" /></view>
        <text>退回发起人修改</text>
      </button>
      <button v-if="canDelegate" class="more-action-item" @click="store.openDelegate()">
        <view class="more-action-item__icon"><AiIcon icon="/static/icons/ai-icon/user-plus.svg" color="#0066ff" size="sm" /></view>
        <text>转办</text>
      </button>
      <!-- 仅办理人/所有者可见；节点明确禁止加签时隐藏加签，减签只要有有效加签人员就显示 -->
      <button v-if="canAddSign" class="more-action-item" @click="store.openSign('addSign')">
        <view class="more-action-item__icon"><AiIcon icon="/static/icons/ai-icon/users.svg" color="#0066ff" size="sm" /></view>
        <text>加签</text>
      </button>
      <button v-if="canReduceSign" class="more-action-item" @click="store.openSign('reduceSign')">
        <view class="more-action-item__icon"><AiIcon icon="/static/icons/ai-icon/user-minus.svg" color="#0066ff" size="sm" /></view>
        <text>减签</text>
      </button>
      <button v-if="canTerminate" class="more-action-item danger" @click="emit('action', 'terminate')">
        <view class="more-action-item__icon"><AiIcon icon="/static/icons/ai-icon/x-circle.svg" color="#ff5219" size="sm" /></view>
        <text>终结流程</text>
      </button>
    </view>
    <template #footer>
      <button class="more-cancel-button" @click="moreVisible = false">取消</button>
    </template>
  </AiPopupSheet>
</template>

<script setup>
import { storeToRefs } from 'pinia'
import AiIcon from '@/components/AiIcon.vue'
import AiPopupSheet from '@/components/AiPopupSheet.vue'
import { useTodoDetailStore } from '@/store'

const emit = defineEmits(['action'])

const store = useTodoDetailStore()
const { moreVisible, canRejectToStart, canDelegate, canTerminate, canAddSign, canReduceSign } = storeToRefs(store)
</script>

<style lang="scss" scoped>
.more-action-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 22px 12px; padding: 8px 0 12px; }
.more-action-item { display: flex; min-width: 0; min-height: 76px; flex-direction: column; align-items: center; justify-content: flex-start; gap: 8px; margin: 0; padding: 0; border: 0; color: var(--text-secondary); font-size: 12px; font-weight: 500; line-height: 1.3; background: transparent; }
.more-action-item::after { border: 0; }
.more-action-item:active { opacity: .7; }
.more-action-item__icon { display: flex; width: 48px; height: 48px; align-items: center; justify-content: center; border-radius: 50%; background: var(--primary-soft); }
.more-action-item.danger { color: var(--forge-color-danger); }
.more-action-item.danger .more-action-item__icon { background: #fff1f1; }
.more-action-item.warning { color: #b45309; }
.more-action-item.warning .more-action-item__icon { background: #fff7e8; }
.more-cancel-button { width: 100%; min-height: 48px; margin: 0; padding: 0 10px; border: 0; border-radius: var(--forge-radius-control); color: var(--forge-text-primary); font-size: 16px; font-weight: 400; background: var(--forge-surface-muted); }
.more-cancel-button::after { border: 0; }
.more-cancel-button:active { opacity: .8; }

@media (hover: hover) {
  .more-action-item:hover { background: var(--surface-muted); }
}
</style>
