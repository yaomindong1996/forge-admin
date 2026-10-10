<template>
  <view class="task-summary">
    <view class="task-summary__head">
      <view class="task-summary__avatar"><text>{{ applicantInitials }}</text></view>
      <view class="task-summary__heading">
        <text class="task-title">{{ title }}</text>
        <text class="task-summary__applicant">{{ applicant }} 提交</text>
      </view>
      <button class="task-summary__refresh" aria-label="刷新详情" @click.stop="emit('refresh')">
        <AiIcon icon="/static/icons/ai-icon/refresh-cw.svg" color="#747677" size="sm" />
      </button>
    </view>
    <!-- 当前节点：详情页顶部的“审批中”提示条 -->
    <view class="task-node">
      <AiIcon icon="/static/icons/ai-icon/file-text.svg" color="#fd8838" size="xs" />
      <text>当前节点：{{ task.taskName || task.name || '审批节点' }}</text>
    </view>
    <view class="task-facts">
      <view class="task-fact"><view class="task-fact__label"><AiIcon icon="/static/icons/ai-icon/user.svg" color="#a2a3a5" size="xs" /><text>申请人</text></view><text>{{ applicant }}</text></view>
      <view class="task-fact"><view class="task-fact__label"><AiIcon icon="/static/icons/ai-icon/briefcase.svg" color="#a2a3a5" size="xs" /><text>发起部门</text></view><text>{{ task.startDeptName || '-' }}</text></view>
      <view class="task-fact"><view class="task-fact__label"><AiIcon icon="/static/icons/ai-icon/folder.svg" color="#a2a3a5" size="xs" /><text>流程分类</text></view><text>{{ task.categoryName || task.category || '-' }}</text></view>
      <view class="task-fact"><view class="task-fact__label"><AiIcon icon="/static/icons/ai-icon/clock.svg" color="#a2a3a5" size="xs" /><text>提交时间</text></view><text>{{ formatFlowDateTime(task.createTime || task.startTime) }}</text></view>
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import AiIcon from '@/components/AiIcon.vue'
import { contactInitials } from '@/utils/contacts'
import { formatFlowDateTime } from '@/utils/flow-display'

const props = defineProps({ task: { type: Object, default: () => ({}) } })
const emit = defineEmits(['refresh'])
const title = computed(() => props.task.title || props.task.businessTitle || props.task.processName || props.task.processDefinitionName || props.task.taskName || '审批任务')
const applicant = computed(() => props.task.startUserName || props.task.createByName || '-')
const applicantInitials = computed(() => contactInitials(applicant.value === '-' ? '' : applicant.value))
</script>

<style lang="scss" scoped>
.task-summary { margin: 12px 12px 0; padding: 16px; border-radius: var(--forge-radius-card); background: var(--forge-surface); }
.task-summary__head { display: flex; min-width: 0; align-items: center; gap: 12px; }
.task-summary__avatar { display: flex; width: 44px; height: 44px; flex: 0 0 44px; align-items: center; justify-content: center; border-radius: 50%; color: #fff; font-size: 15px; font-weight: 500; background: var(--forge-color-primary); }
.task-summary__heading { display: flex; min-width: 0; flex: 1; flex-direction: column; gap: 2px; }
.task-title, .task-fact text { display: block; }
.task-title { min-width: 0; color: var(--forge-text-primary); font-size: 18px; font-weight: 600; line-height: 1.4; overflow-wrap: anywhere; }
.task-summary__applicant { color: var(--forge-text-secondary); font-size: 13px; line-height: 1.4; }
.task-summary__refresh { display: flex; width: 36px; height: 36px; flex: 0 0 36px; align-items: center; justify-content: center; margin: 0 -6px 0 0; padding: 0; border: 0; border-radius: 50%; background: transparent; }
.task-summary__refresh::after { border: 0; }
.task-summary__refresh:active { background: var(--forge-surface-subtle); }
.task-node { display: flex; align-items: center; gap: 6px; margin-top: 14px; padding: 8px 12px; border-radius: 10px; color: var(--forge-tone-orange); font-size: 14px; line-height: 1.4; background: var(--forge-tone-orange-bg); }
.task-node text { min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.task-facts { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); gap: 12px 16px; margin-top: 14px; }
.task-fact { min-width: 0; }
.task-fact__label { display: flex; min-width: 0; align-items: center; gap: 4px; }
.task-fact__label text { color: var(--forge-text-tertiary); font-size: 12px; }
.task-fact > text { overflow: hidden; margin-top: 4px; color: var(--forge-text-primary); font-size: 14px; text-overflow: ellipsis; white-space: nowrap; }

@media (min-width: 1024px) {
  .task-summary {
    width: calc(100% - 48px);
    max-width: 1280px;
    margin: 24px auto 0;
    padding: 24px;
    box-sizing: border-box;
  }
}
</style>
