<template>
  <view class="cc-detail-page">
    <AiFeedbackHost />
    <scroll-view class="cc-detail-scroll" scroll-y :show-scrollbar="true">
      <TodoDetailSkeleton v-if="loading" />
      <view v-else-if="notFound" class="cc-detail-hint">抄送记录不存在或已撤回</view>
      <view v-else class="cc-detail-content">
        <!-- 抄送信息 -->
        <view class="cc-info-panel">
          <text class="cc-info-title">{{ title }}</text>
          <view class="cc-info-meta">
            <text class="cc-info-badge">抄送</text>
            <text>{{ sender }} 抄送给你 · {{ formatFlowDateTime(cc?.ccTime) }}</text>
          </view>
          <text v-if="cc?.content" class="cc-info-content">{{ cc.content }}</text>
          <view class="cc-info-facts">
            <view class="cc-info-fact"><text>申请人</text><text>{{ formInfo?.startUserName || '-' }}</text></view>
            <view class="cc-info-fact"><text>流程</text><text>{{ processName }}</text></view>
          </view>
        </view>

        <!-- 业务表单（只读）：只读上下文不可用时降级为提示 -->
        <view v-if="formLoading" class="cc-panel cc-loading-card">
          <TodoDetailSkeleton form-only />
        </view>
        <view v-else-if="showBusinessFormPanel" class="cc-panel">
          <view class="cc-section-head">
            <view class="cc-section-icon"><AiIcon icon="/static/icons/ai-icon/file-text.svg" color="#0066ff" size="sm" /></view>
            <text class="cc-section-title">表单信息</text>
          </view>
          <FlowBusinessFormPanel :form="businessForm" />
        </view>
        <view v-else class="cc-blocked-panel">
          <AiIcon icon="/static/icons/ai-icon/monitor.svg" color="#0066ff" size="md" />
          <view class="cc-blocked-copy">
            <text class="cc-blocked-title">业务表单请在 PC 端查看</text>
            <text class="cc-blocked-desc">移动端暂未取得该流程的只读表单</text>
          </view>
        </view>

        <!-- 审批流程：流程级接口包含抄送人可见性 -->
        <view class="cc-panel cc-workflow-panel">
          <view class="cc-section-head">
            <view class="cc-section-icon"><AiIcon icon="/static/icons/ai-icon/check-circle.svg" color="#0066ff" size="sm" /></view>
            <text class="cc-section-title">审批流程</text>
          </view>
          <TodoFlowTrace mode="process" :loading="traceLoading" :items="processNodes" />
          <TodoFlowTrace mode="history" :loading="traceLoading" :items="history" />
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import AiFeedbackHost from '@/components/feedback/AiFeedbackHost.vue'
import AiIcon from '@/components/AiIcon.vue'
import FlowBusinessFormPanel from '@/components/flow/FlowBusinessFormPanel.vue'
import TodoDetailSkeleton from '@/components/flow/TodoDetailSkeleton.vue'
import TodoFlowTrace from '@/components/flow/TodoFlowTrace.vue'
import { useCcDetail } from '@/composables/flow/useCcDetail'
import { ccSender, ccTitle } from '@/utils/flow-cc'
import { formatFlowDateTime } from '@/utils/flow-display'

const {
  cc, formInfo, businessForm, history, processNodes, loading, formLoading, traceLoading, notFound, load,
} = useCcDetail()

// 不能对 businessForm 用 toRefs：该运行时会立即求值全部 computed，业务上下文未加载时部分计算会抛错。
const showBusinessFormPanel = computed(() => businessForm.showBusinessFormPanel && !businessForm.blockedReason)
const processName = computed(() => cc.value?.processName || formInfo.value?.processName || formInfo.value?.processDefinitionName || '-')
const title = computed(() => (cc.value ? ccTitle(cc.value) : formInfo.value?.title || processName.value))
const sender = computed(() => (cc.value ? ccSender(cc.value) : '系统'))

onLoad((options = {}) => load(options))
</script>

<style lang="scss" scoped>
.cc-detail-page { display: flex; height: var(--forge-page-height, 100vh); flex-direction: column; background: var(--forge-page-bg); }
.cc-detail-scroll { height: 0; min-height: 0; flex: 1; }
.cc-detail-content { display: flex; flex-direction: column; gap: 12px; padding: 12px 12px 24px; }
.cc-detail-hint { padding: 80rpx 32rpx; color: var(--forge-text-tertiary); font-size: 14px; text-align: center; }

.cc-info-panel,
.cc-panel,
.cc-blocked-panel { padding: 16px; border-radius: var(--forge-radius-card); background: var(--forge-surface); }

.cc-info-title,
.cc-info-content,
.cc-info-fact text,
.cc-blocked-title,
.cc-blocked-desc { display: block; }

.cc-info-title { color: var(--forge-text-primary); font-size: 18px; font-weight: 600; line-height: 1.4; }
.cc-info-meta { display: flex; align-items: center; gap: 8px; margin-top: 8px; color: var(--forge-text-tertiary); font-size: 12px; }
.cc-info-badge { padding: 1px 6px; border-radius: 4px; color: var(--forge-tone-blue); background: var(--forge-tone-blue-bg); font-size: 11px; font-weight: 600; }

.cc-info-content {
  margin-top: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  color: var(--forge-text-secondary);
  background: var(--forge-surface-subtle);
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
}

.cc-info-facts { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); gap: 12px; margin-top: 14px; padding-top: 12px; border-top: 1px solid var(--forge-border); }
.cc-info-fact { min-width: 0; }
.cc-info-fact text:first-child { color: var(--forge-text-tertiary); font-size: 12px; }
.cc-info-fact text:last-child { overflow: hidden; margin-top: 2px; color: var(--forge-text-primary); font-size: 14px; text-overflow: ellipsis; white-space: nowrap; }

.cc-section-head { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; }
.cc-section-icon { display: flex; width: 28px; height: 28px; flex: 0 0 28px; align-items: center; justify-content: center; border-radius: 8px; background: var(--forge-color-primary-soft); }
.cc-section-title { color: var(--forge-text-primary); font-size: 16px; font-weight: 600; }
.cc-loading-card { min-height: 180px; }

.cc-blocked-panel { display: flex; align-items: center; gap: 12px; background: var(--forge-color-primary-soft); }
.cc-blocked-copy { min-width: 0; flex: 1; }
.cc-blocked-title { color: var(--forge-text-primary); font-size: 15px; font-weight: 600; }
.cc-blocked-desc { margin-top: 2px; color: var(--forge-text-secondary); font-size: 13px; }

.cc-workflow-panel :deep(.trace-panel) { margin: 0; padding: 14px 0; border: 0; border-radius: 0; background: transparent; }
.cc-workflow-panel :deep(.trace-panel:first-of-type) { padding-top: 4px; border-bottom: 1px solid var(--forge-border); }
.cc-workflow-panel :deep(.trace-panel:last-child) { padding-bottom: 0; }

@media (min-width: 1024px) {
  .cc-detail-content { width: calc(100% - 48px); max-width: 1280px; margin: 0 auto; box-sizing: border-box; }
  .cc-panel { padding: 24px; }
}
</style>
