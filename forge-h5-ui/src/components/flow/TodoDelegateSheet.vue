<template>
  <AiPopupSheet v-model="delegateVisible" title="转办任务" description="选择处理人后，再确认转办" max-height="90vh" body-max-height="calc(90vh - 230rpx - env(safe-area-inset-bottom))">
    <FlowUserPicker v-model="delegateUser" :active="delegateVisible" :exclude-ids="excludeIds" empty-text="未找到可转办人员" />
    <view class="delegate-comment">
      <text class="form-label">转办说明<text v-if="requireComment" class="required-mark"> *</text></text>
      <AiTextarea v-model="delegateComment" maxlength="500" placeholder="请说明转办原因" />
    </view>
    <view v-if="requireSignature" class="delegate-signature">
      <text class="form-label">手写签名<text class="required-mark"> *</text></text>
      <AiSignaturePad ref="delegateSignatureRef" v-model="delegateSignature" />
    </view>
    <template #footer>
      <AiButton block :disabled="!delegateUser" :loading="actionLoading && pendingAction === 'delegate'" @click="emit('confirm', delegateSignatureRef)">确认转办</AiButton>
    </template>
  </AiPopupSheet>
</template>

<script setup>
import { computed, ref } from 'vue'
import { storeToRefs } from 'pinia'
import AiButton from '@/components/AiButton.vue'
import AiPopupSheet from '@/components/AiPopupSheet.vue'
import AiSignaturePad from '@/components/AiSignaturePad.vue'
import AiTextarea from '@/components/AiTextarea.vue'
import FlowUserPicker from '@/components/flow/FlowUserPicker.vue'
import { useTodoDetailStore } from '@/store'

const props = defineProps({
  currentUserId: { type: String, default: '' },
})
const emit = defineEmits(['confirm'])

const store = useTodoDetailStore()
const {
  delegateVisible, delegateUser, delegateComment, delegateSignature,
  requireComment, requireSignature, actionLoading, pendingAction, task,
} = storeToRefs(store)
const delegateSignatureRef = ref(null)
const excludeIds = computed(() => [props.currentUserId, task.value?.assignee])
</script>

<style lang="scss" scoped>
.delegate-comment, .delegate-signature { display: flex; flex-direction: column; gap: 12rpx; margin-top: 18rpx; }
.form-label { display: block; color: #747677; font-size: 14px; font-weight: 400; }
.required-mark { color: #ff5219; }
</style>
