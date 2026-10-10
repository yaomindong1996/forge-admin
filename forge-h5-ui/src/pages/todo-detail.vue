<template>
  <view class="todo-detail-page">
    <AiFeedbackHost />
    <!-- 审批动作属于不可并行的状态流转，接口返回前统一阻止重复操作。 -->
    <AiLoadingOverlay :visible="actionLoading" :text="actionLoadingText" />
    <scroll-view
      class="detail-scroll"
      scroll-y
      :show-scrollbar="true"
      :scroll-into-view="scrollTarget"
      scroll-with-animation
    >
      <TodoDetailSkeleton v-if="loading" />
      <template v-else-if="task">
        <TodoTaskSummary :task="task" @refresh="refresh" />

        <view class="detail-content">
          <!-- 动态业务表单 -->
          <view v-if="formLoading" class="content-panel detail-loading-card">
            <TodoDetailSkeleton form-only />
          </view>
          <view v-else-if="blockedReason" class="blocked-panel">
            <view class="blocked-panel__icon">
              <AiIcon icon="/static/icons/ai-icon/info.svg" color="#0066ff" size="md" />
            </view>
            <view class="blocked-panel__copy">
              <text class="blocked-title">请在 PC 端处理</text>
              <text class="blocked-copy">{{ blockedReason }}</text>
            </view>
          </view>

          <view v-else-if="showBusinessFormPanel" class="content-panel">
            <view class="detail-section-head">
              <view class="detail-section-heading">
                <view class="detail-section-icon"><AiIcon icon="/static/icons/ai-icon/file-text.svg" color="#0066ff" size="sm" /></view>
                <view>
                  <text class="detail-section-title">表单信息</text>
                  <text class="detail-section-desc">当前审批节点对应的业务内容</text>
                </view>
              </view>
              <button
                v-if="businessFormHasWritableFields"
                class="save-form-button"
                :disabled="actionLoading || formSaving"
                @click="saveBusinessFields"
              >
                {{ formSaving ? '暂存中' : '暂存修改' }}
              </button>
            </view>
            <FlowBusinessFormPanel :form="businessForm" />

            <view v-if="responsibilityDescription || approvalPoints.length" class="approval-duty-panel">
              <view v-if="responsibilityDescription" class="duty-block">
                <text class="form-label">审批职责</text>
                <text class="duty-copy">{{ responsibilityDescription }}</text>
              </view>
              <view v-if="approvalPoints.length" class="duty-block">
                <text class="form-label">审批要点</text>
                <view
                  v-for="point in approvalPoints"
                  :key="point.id"
                  class="approval-point-row"
                  @click="store.toggleApprovalPoint(point)"
                >
                  <text class="approval-point-check">{{ approvalPointChecks[point.id] ? '☑' : '☐' }}</text>
                  <text class="approval-point-copy">{{ point.content }}</text>
                  <text class="approval-point-tag">{{ point.required ? '必审' : '非必审' }}</text>
                </view>
              </view>
            </view>
          </view>

          <!-- 流程进度与审批记录在同一卡片连续展示 -->
          <view class="workflow-panel">
            <view class="detail-section-head">
              <view class="detail-section-heading">
                <view class="detail-section-icon"><AiIcon icon="/static/icons/ai-icon/check-circle.svg" color="#0066ff" size="sm" /></view>
                <view>
                  <text class="detail-section-title">审批流程</text>
                  <text class="detail-section-desc">节点进度与办理记录</text>
                </view>
              </view>
            </view>
            <view class="trace-sections">
              <TodoFlowTrace mode="process" :loading="diagramLoading" :items="processNodes" />
              <TodoFlowTrace mode="history" :loading="historyLoading" :items="history" />
            </view>
            <TodoSignRelations />
          </view>

          <!-- 处理模式下才展示审批意见和签名 -->
          <view v-if="!readonlyMode" id="approval-comment-panel" class="approval-comment-panel">
            <view class="detail-section-head">
              <view class="detail-section-heading">
                <view class="detail-section-icon"><AiIcon icon="/static/icons/ai-icon/edit-3.svg" color="#0066ff" size="sm" /></view>
                <view>
                  <text class="detail-section-title">审批意见<text v-if="requireComment" class="required-mark"> *</text></text>
                  <text class="detail-section-desc">填写本次处理意见</text>
                </view>
              </view>
            </view>
            <view class="comment-row">
              <FlowCommentPhraseInput
                v-model="comment"
                maxlength="500"
                min-height="96px"
                :placeholder="requireComment ? '请输入审批意见' : '请输入审批意见（选填）'"
              />
            </view>
            <view v-if="requireSignature" class="comment-row signature-row">
              <text class="form-label">手写签名<text class="required-mark"> *</text></text>
              <AiSignaturePad ref="approvalSignatureRef" v-model="signature" />
            </view>
          </view>
        </view>
      </template>
      <view v-else class="page-hint">待办不存在或已处理</view>
    </scroll-view>

    <!-- 底部操作区：只读模式（我发起的/已处理）不展示办理按钮，只在可催办时显示催办 -->
    <TodoActionBar
      v-if="task && !readonlyMode"
      :blocked="Boolean(blockedReason)"
      @claim="claimTask"
      @reject="submitAction('reject', approvalSignatureRef)"
      @approve="submitAction('approve', approvalSignatureRef)"
    />
    <TodoRemindBar v-else-if="task" />

    <!-- 多节点驳回：流程允许选择退回节点时先选目标 -->
    <AiPopupSheet v-model="rejectTargetVisible" title="选择驳回节点" description="流程会从所选节点继续，而不是整单结束">
      <view class="return-target-list">
        <button
          v-for="option in returnTargetOptions"
          :key="option.value"
          class="return-target-item"
          :class="{ active: selectedReturnTarget === option.value }"
          @click="selectedReturnTarget = option.value"
        >
          {{ option.label }}
        </button>
      </view>
      <view class="delegate-comment" style="margin-top: 24rpx;">
        <AiButton block :disabled="!selectedReturnTarget" :loading="actionLoading && pendingAction === 'return'" @click="submitAction('return', approvalSignatureRef)">确认驳回</AiButton>
      </view>
    </AiPopupSheet>

    <!-- 更多操作、转办、加签/减签 -->
    <TodoMoreActionSheet @action="action => submitMoreAction(action, approvalSignatureRef)" />
    <TodoDelegateSheet :current-user-id="userId" @confirm="signatureRef => submitAction('delegate', signatureRef)" />
    <TodoSignSheet @confirm="submitSign" />
  </view>
</template>

<script setup>
import { computed, nextTick, reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { storeToRefs } from 'pinia'
import AiButton from '@/components/AiButton.vue'
import AiFeedbackHost from '@/components/feedback/AiFeedbackHost.vue'
import AiIcon from '@/components/AiIcon.vue'
import AiLoadingOverlay from '@/components/AiLoadingOverlay.vue'
import AiPopupSheet from '@/components/AiPopupSheet.vue'
import AiSignaturePad from '@/components/AiSignaturePad.vue'
import FlowBusinessFormPanel from '@/components/flow/FlowBusinessFormPanel.vue'
import FlowCommentPhraseInput from '@/components/flow/FlowCommentPhraseInput.vue'
import TodoActionBar from '@/components/flow/TodoActionBar.vue'
import TodoDelegateSheet from '@/components/flow/TodoDelegateSheet.vue'
import TodoDetailSkeleton from '@/components/flow/TodoDetailSkeleton.vue'
import TodoFlowTrace from '@/components/flow/TodoFlowTrace.vue'
import TodoMoreActionSheet from '@/components/flow/TodoMoreActionSheet.vue'
import TodoRemindBar from '@/components/flow/TodoRemindBar.vue'
import TodoSignRelations from '@/components/flow/TodoSignRelations.vue'
import TodoSignSheet from '@/components/flow/TodoSignSheet.vue'
import TodoTaskSummary from '@/components/flow/TodoTaskSummary.vue'
import { useFlowBusinessForm } from '@/composables/flow/useFlowBusinessForm'
import { useTodoDetailLoader } from '@/composables/flow/useTodoDetailLoader'
import { useTodoSignActions } from '@/composables/flow/useTodoSignActions'
import { useTodoTaskActions } from '@/composables/flow/useTodoTaskActions'
import { useAuthStore, useTodoDetailStore } from '@/store'

const authStore = useAuthStore()
const store = useTodoDetailStore()
const {
  taskId, task, formInfo, businessContext, businessContextError, history, loading, formLoading,
  historyLoading, diagramLoading, comment, signature, approvalPointChecks, actionLoading, pendingAction,
  rejectTargetVisible, selectedReturnTarget, readonlyMode, requireComment, requireSignature,
  responsibilityDescription, approvalPoints, processNodes, actionLoadingText, returnTargetOptions,
} = storeToRefs(store)
const approvalSignatureRef = ref(null)
const scrollTarget = ref('')

const userId = computed(() => String(authStore.userInfo?.id || authStore.userInfo?.userId || authStore.userInfo?.user_id || ''))
const businessForm = reactive(useFlowBusinessForm({
  businessContext,
  formInfo,
  businessContextError,
  isReadonly: () => readonlyMode.value,
  getRouteQuery: () => ({ taskId: taskId.value }),
  seedApprovalPointChecks: info => store.seedApprovalPointChecks(info),
}))
// 不能对 businessForm 用 toRefs：该运行时会立即求值全部 computed，业务上下文未加载时部分计算会抛错。
const showBusinessFormPanel = computed(() => businessForm.showBusinessFormPanel)
const blockedReason = computed(() => businessForm.blockedReason)
const businessFormHasWritableFields = computed(() => businessForm.businessFormHasWritableFields)
const { refresh: refreshTask } = useTodoDetailLoader(businessForm)
const { loadSignRelations, submitSign } = useTodoSignActions({ userId, refresh })
const { formSaving, claimTask, submitAction, submitMoreAction, saveBusinessFields } = useTodoTaskActions({
  businessForm,
  userId,
  refresh,
  scrollToApprovalComment,
})

onLoad(async (options = {}) => {
  store.init(options, userId.value)
  await refresh()
})

// 加签记录依赖任务摘要中的 taskId，必须在详情加载完成后再查。
async function refresh() {
  await refreshTask()
  await loadSignRelations()
}

function scrollToApprovalComment() {
  scrollTarget.value = ''
  nextTick(() => {
    scrollTarget.value = 'approval-comment-panel'
  })
}
</script>

<style lang="scss" scoped src="./styles/todo-detail.scss"></style>
