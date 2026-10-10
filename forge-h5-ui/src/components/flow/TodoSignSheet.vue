<template>
  <AiPopupSheet
    v-model="signVisible"
    :title="isAdd ? '加签' : '减签'"
    :description="isAdd ? '加签人与你并行处理当前节点' : '移除后对方不再处理当前节点'"
    max-height="90vh"
    body-max-height="calc(90vh - 230rpx - env(safe-area-inset-bottom))"
  >
    <!-- 加签：内嵌选人；减签：从有效加签人员中选择 -->
    <FlowUserPicker v-if="isAdd" v-model="signUser" :active="signVisible" :exclude-ids="excludeIds" empty-text="未找到可加签人员" />
    <view v-else class="sign-target-list">
      <button
        v-for="relation in activeSignRelations"
        :key="relation.targetUserId"
        class="sign-target-item"
        :class="{ active: signTargetId === String(relation.targetUserId) }"
        @click="signTargetId = String(relation.targetUserId)"
      >
        <text class="sign-target-name">{{ signUserName(relation) }}</text>
        <text v-if="relation.reason" class="sign-target-reason">{{ relation.reason }}</text>
      </button>
    </view>
    <view class="sign-comment">
      <text class="form-label">{{ isAdd ? '加签原因' : '减签原因' }}</text>
      <AiTextarea v-model="signComment" maxlength="200" placeholder="选填" />
    </view>
    <template #footer>
      <AiButton block :disabled="!canConfirm" :loading="actionLoading && pendingAction === signAction" @click="emit('confirm')">
        {{ isAdd ? '确认加签' : '确认减签' }}
      </AiButton>
    </template>
  </AiPopupSheet>
</template>

<script setup>
import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import AiButton from '@/components/AiButton.vue'
import AiPopupSheet from '@/components/AiPopupSheet.vue'
import AiTextarea from '@/components/AiTextarea.vue'
import FlowUserPicker from '@/components/flow/FlowUserPicker.vue'
import { useTodoDetailStore } from '@/store'
import { buildSignExcludeIds, signUserName } from '@/utils/flow-sign'

const emit = defineEmits(['confirm'])

const store = useTodoDetailStore()
const {
  signVisible, signAction, signUser, signTargetId, signComment, activeSignRelations,
  actionLoading, pendingAction, task, currentUserId, signRelations,
} = storeToRefs(store)
const isAdd = computed(() => signAction.value === 'addSign')
const canConfirm = computed(() => (isAdd.value ? Boolean(signUser.value) : Boolean(signTargetId.value)))
const excludeIds = computed(() => buildSignExcludeIds({
  userId: currentUserId.value, task: task.value, relations: signRelations.value,
}))
</script>

<style lang="scss" scoped>
.sign-target-list { display: flex; flex-direction: column; gap: 8px; }

.sign-target-item {
  display: flex;
  width: 100%;
  min-height: 52px;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  margin: 0;
  padding: 8px 14px;
  border: 1px solid var(--forge-border);
  border-radius: var(--forge-radius-control);
  background: var(--forge-surface);
  text-align: left;
  box-sizing: border-box;
}

.sign-target-item::after { border: 0; }
.sign-target-item.active { border-color: var(--forge-color-primary); background: var(--forge-color-primary-soft); }
.sign-target-name { color: var(--forge-text-primary); font-size: 15px; line-height: 1.4; }
.sign-target-item.active .sign-target-name { color: var(--forge-color-primary); font-weight: 600; }
.sign-target-reason { margin-top: 2px; color: var(--forge-text-tertiary); font-size: 12px; line-height: 1.4; }
.sign-comment { display: flex; flex-direction: column; gap: 8px; margin-top: 16px; }
.form-label { color: var(--forge-text-secondary); font-size: 14px; }
</style>
