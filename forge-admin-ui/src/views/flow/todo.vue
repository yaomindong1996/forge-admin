<template>
  <div class="flow-page">
    <n-alert v-if="loadError" type="error" class="mb-3" :show-icon="true">
      待办任务加载失败，请重试。
      <template #action>
        <NButton text type="primary" @click="loadData">
          重试
        </NButton>
      </template>
    </n-alert>
    <!-- 顶部彩色统计：待办/已办/发起/抄送 -->
    <WorkspaceSummaryMetrics
      class="flow-summary-metrics"
      compact
      emphasis-key="todo"
    />
    <!-- 任务列表 -->
    <FlowTaskCardList
      v-model:selected-keys="selectedTaskKeys"
      v-model:search-value="queryParams.title"
      title="我的待办任务"
      icon-preset="todo"
      :items="dataSource"
      :loading="loading"
      :pagination="pagination"
      row-key="id"
      search-placeholder="搜索任务名称或编号..."
      empty-text="暂无待办任务"
      user-title="当前处理人"
      @search="handleSearch"
      @refresh="loadData"
      @row-click="openDrawer"
      @update:page="pagination.onChange"
      @update:page-size="pagination.onUpdatePageSize"
    >
      <template #filters>
        <NTreeSelect
          v-model:value="queryParams.category"
          placeholder="流程分类"
          clearable
          class="category-select"
          :options="categoryTreeOptions"
          :default-expand-all="true"
          @update:value="handleSearch"
        />
        <n-select
          v-model:value="queryParams.status"
          placeholder="任务状态"
          clearable
          class="category-select"
          :options="statusOptions"
          @update:value="handleSearch"
        />
        <NButton secondary @click="handleReset">
          重置
        </NButton>
      </template>
      <template #batch-actions>
        <NButton
          v-if="selectedTaskKeys.length > 0"
          size="small"
          type="error"
          secondary
          @click="openQuickAction('reject', selectedTaskKeys)"
        >
          驳回
        </NButton>
        <NButton
          v-if="selectedTaskKeys.length > 0"
          size="small"
          type="primary"
          @click="openQuickAction('approve', selectedTaskKeys)"
        >
          同意
        </NButton>
        <span v-if="urgentCount > 0" class="task-list-hint urgent">
          <i class="i-material-symbols:warning" />
          {{ urgentCount }} 紧急
        </span>
      </template>
      <template #status="{ row }">
        <span class="task-status-pill" :class="row.status === 0 ? 'todo-status-pending' : 'todo-status-active'">
          {{ getLabel('flow_todo_status', row.status) }}
        </span>
      </template>
      <template #title="{ row }">
        {{ getRowDisplayTitle(row) }}
      </template>
      <template #node="{ row }">
        {{ getTaskDisplayName(row) }}
      </template>
      <template #user="{ row }">
        <span>{{ getTaskHandlerName(row) }}</span>
        <small v-if="row.startUserName">申请人 {{ row.startUserName }}</small>
        <small>{{ row.createTime || '-' }}</small>
      </template>
      <template #summary="{ row }">
        <FlowTaskBusinessSummary :row="row" />
      </template>
      <template #actions="{ row }">
        <button type="button" class="task-row-link-action success" aria-label="同意任务" @click="openQuickAction('approve', [row])">
          同意
        </button>
        <span class="task-row-action-separator" />
        <button type="button" class="task-row-link-action danger" aria-label="驳回任务" @click="openQuickAction('reject', [row])">
          驳回
        </button>
        <span class="task-row-action-separator" />
        <button type="button" class="task-row-link-action" aria-label="去审批" @click="openDrawer(row)">
          审批
        </button>
        <template v-if="row.status === 0 && !row.assignee">
          <span class="task-row-action-separator" />
          <button type="button" class="task-row-link-action info" aria-label="签收任务" @click="handleClaim(row)">
            签收
          </button>
        </template>
        <span class="task-row-action-separator" />
        <button type="button" class="task-row-link-action muted" aria-label="更多操作" @click="openDrawer(row)">
          <i class="i-lucide:more-horizontal" />
        </button>
      </template>
    </FlowTaskCardList>

    <n-modal
      v-model:show="quickActionVisible"
      :auto-focus="false"
      :closable="false"
      :mask-closable="!quickActionLoading"
      :close-on-esc="!quickActionLoading"
    >
      <div
        class="quick-action-panel"
        :data-action="quickActionType"
        role="dialog"
        aria-modal="true"
        :aria-labelledby="quickActionTitleId"
      >
        <div class="quick-action-head">
          <div class="quick-action-head-text">
            <strong :id="quickActionTitleId">{{ quickActionTitle }}</strong>
            <p>
              {{ quickActionSubject }}<span v-if="quickActionMeta"> · {{ quickActionMeta }}</span>
            </p>
          </div>
          <button
            type="button"
            class="quick-action-close"
            aria-label="关闭"
            :disabled="quickActionLoading"
            @click="quickActionVisible = false"
          >
            <i class="i-material-symbols:close" />
          </button>
        </div>
        <FlowCommentPhraseInput
          ref="quickActionInputRef"
          v-model="quickActionForm.comment"
          :scene="quickActionIsApprove ? 'APPROVE' : 'REJECT'"
          :rows="3"
          :maxlength="200"
          :disabled="quickActionLoading"
          :placeholder="quickActionIsApprove ? '审批意见，可直接提交' : '驳回原因'"
          @submit="submitQuickAction"
        />
        <p v-if="quickActionTargets.length > 1" class="quick-action-tip">
          需填表或签名的任务会跳过
        </p>
        <n-alert v-if="quickActionFailedTargets.length" type="warning" :show-icon="true" class="quick-action-result">
          {{ quickActionFailedTargets.length }} 条任务未处理，可修改意见后重试。
        </n-alert>
        <div class="quick-action-actions">
          <NButton size="small" :disabled="quickActionLoading" @click="quickActionVisible = false">
            取消
          </NButton>
          <NButton
            size="small"
            :type="quickActionIsApprove ? 'primary' : 'error'"
            :loading="quickActionLoading"
            :disabled="quickActionLoading"
            @click="submitQuickAction"
          >
            {{ quickActionFailedTargets.length ? '重试未处理任务' : quickActionTitle }}
          </NButton>
        </div>
      </div>
    </n-modal>

    <n-modal
      v-model:show="rejectTargetVisible"
      preset="card"
      title="选择驳回节点"
      style="width: 480px"
      :mask-closable="false"
    >
      <NSpace vertical>
        <n-text depth="3">
          请选择驳回到哪个已审批节点。流程会从该节点继续，而不是整单结束。
        </n-text>
        <n-select
          v-model:value="selectedReturnTarget"
          :options="returnTargetOptions"
          placeholder="请选择已审批节点"
        />
      </NSpace>
      <template #footer>
        <NSpace justify="end">
          <NButton :disabled="approveLoading" @click="rejectTargetVisible = false">
            取消
          </NButton>
          <NButton type="error" :disabled="!selectedReturnTarget" :loading="approveLoading" @click="confirmRejectToTarget">
            确认驳回
          </NButton>
        </NSpace>
      </template>
    </n-modal>

    <!-- 审批详情弹窗 -->
    <FlowTaskDetailShell
      v-model:show="showDrawer"
      :busy="approveLoading"
      :title="currentTask ? getRowDisplayTitle(currentTask) : '审批详情'"
      :subtitle="getTaskDisplayName(currentTask, '') ? `当前节点：${getTaskDisplayName(currentTask)}` : ''"
      :status-text="getLabel('flow_todo_status', currentTask?.status)"
      :status-class="currentTask?.status === 0 ? 'todo-status-pending' : 'todo-status-active'"
      :status-icon="currentTask?.status === 0 ? 'i-material-symbols:schedule' : 'i-material-symbols:assignment-ind'"
      :priority-text="getPriorityText(currentTask?.priority)"
      :priority-class="getPriorityClass(currentTask?.priority)"
      :records="approvalHistory"
      record-title="审批记录"
      fullscreen
    >
      <template #toolbar>
        <FlowPrintAction
          :row="currentTask"
          scene="FLOW_TODO"
          :task-form-info="taskFormInfo"
          :business-context="businessFormContext"
          :dirty="flowPrintHasUnsavedChanges"
          :disabled="isApprovalBusy"
        />
      </template>
      <template v-if="currentTask">
        <section class="approval-detail-section">
          <div class="approval-section-header">
            <i class="i-material-symbols:info-outline" />
            基本信息
          </div>
          <div class="approval-field-grid">
            <div class="approval-field">
              <span class="approval-label">当前节点</span>
              <span class="approval-value">{{ getTaskDisplayName(currentTask) }}</span>
            </div>
            <div class="approval-field">
              <span class="approval-label">流程名称</span>
              <span class="approval-value">{{ getProcessDisplayName(currentTask) }}</span>
            </div>
            <div class="approval-field">
              <span class="approval-label">流程分类</span>
              <span class="approval-value">{{ getCategoryDisplayName(currentTask) }}</span>
            </div>
            <div class="approval-field">
              <span class="approval-label">发起人</span>
              <span class="approval-value approval-user-inline">
                <UserAvatar :name="currentTask.startUserName || '未知'" :size="24" />
                {{ currentTask.startUserName || '-' }}
              </span>
            </div>
            <div class="approval-field">
              <span class="approval-label">发起部门</span>
              <span class="approval-value">{{ currentTask.startDeptName || '-' }}</span>
            </div>
            <div class="approval-field">
              <span class="approval-label">发起时间</span>
              <span class="approval-value">{{ currentTask.createTime || '-' }}</span>
            </div>
            <div class="approval-field">
              <span class="approval-label">任务状态</span>
              <span class="approval-value">{{ getLabel('flow_todo_status', currentTask.status) || '-' }}</span>
            </div>
          </div>
        </section>

        <section class="approval-detail-section">
          <n-collapse arrow-placement="right">
            <n-collapse-item title="查看流程图" name="diagram">
              <div class="approval-diagram">
                <DingFlowViewer v-if="currentTask.processInstanceId" :process-instance-id="currentTask.processInstanceId" :compact="true" />
                <n-empty v-else description="暂无流程图" size="small" />
              </div>
            </n-collapse-item>
          </n-collapse>
        </section>

        <section class="approval-detail-section">
          <div class="approval-section-header">
            <i class="i-material-symbols:rate-review" />
            审批处理
          </div>

          <div v-if="taskFormLoading" class="form-loading">
            <n-spin size="small" />
            <span>加载表单中...</span>
          </div>

          <FlowApprovalChecklist
            v-if="!taskFormLoading"
            v-model="approvalPointChecks"
            :responsibility-description="taskFormInfo?.responsibilityDescription || ''"
            :approval-points="taskFormInfo?.approvalPoints || []"
            :legacy-approval-point="taskFormInfo?.approvalPoint || ''"
          />

          <div v-if="!taskFormLoading && canDirectSend" class="flow-routing-options">
            <n-form :model="approveForm" label-placement="top">
              <n-checkbox v-model:checked="directSendAfterReturn">
                修正后直送至 {{ taskPolicySource.returnSourceActivityName || taskPolicySource.returnSourceActivityId }}
              </n-checkbox>
            </n-form>
          </div>

          <template v-if="useComponentTaskForm">
            <FlowBusinessForm
              :form-url="componentTaskFormUrl"
              :task-id="componentTaskFormInfo.taskId"
              :business-key="componentTaskFormInfo.businessKey"
              :process-instance-id="componentTaskFormInfo.processInstanceId"
              :task-def-key="componentTaskFormInfo.taskDefKey"
              :process-def-key="componentTaskFormInfo.processDefKey"
              :variables="taskFormInfo?.variables || {}"
              :approval-policy="approvalPolicy"
              :initial-task-context="businessFormContext"
              :read-only="false"
              :submitting="approveLoading"
              :submitting-action="approveForm.action"
              @submit="handleExternalFormSubmit"
              @cancel="showDrawer = false"
            >
              <template #actions>
                <NButton v-if="canDelegate" size="small" :disabled="isApprovalBusy" @click="handleDelegate">
                  转办
                </NButton>

                <NButton
                  v-if="currentTask.status === 0 && !currentTask.assignee"
                  size="small"
                  :loading="isClaimingTask(currentTask)"
                  :disabled="isApprovalBusy"
                  @click="handleClaim(currentTask)"
                >
                  签收
                </NButton>
              </template>
            </FlowBusinessForm>
          </template>

          <template v-else>
            <div v-if="!taskFormLoading && useBusinessManagedForm" class="business-task-form-section">
              <div class="approval-form-title">
                <span>{{ businessFormTitle }}</span>
                <small v-if="businessFormContext?.pageName || businessFormContext?.formRef?.pageName">
                  页面：{{ businessFormContext.pageName || businessFormContext.formRef.pageName }}
                </small>
              </div>
              <AiForm
                ref="businessFormRef"
                v-model:value="businessFormData"
                :schema="businessFormAiSchema"
                :field-permissions="businessFormFieldPermissions"
                :show-actions="false"
                :show-feedback="true"
                :grid-cols="businessFormGridCols"
                :label-placement="businessFormLabelPlacement"
                :label-width="businessFormLabelWidth"
                :size="businessFormSize"
                :context="businessFormRenderContext"
                :form-assets="businessFormContext.formAssets || []"
              />
              <ChildTableEditor
                v-if="businessFormChildrenConfig.length"
                ref="businessChildFormRef"
                v-model:value="businessChildFormData"
                :children-config="businessFormChildrenConfig"
                :parent-form-data="businessFormData"
                :context="businessFormRenderContext"
              />
              <div v-if="businessFormWarnings.length" class="business-form-warnings">
                <n-alert v-for="warning in businessFormWarnings" :key="warning" type="warning" :show-icon="false">
                  {{ warning }}
                </n-alert>
              </div>
              <div v-if="businessFormHasWritableFields || (useBusinessCodeForm && businessCodeFormUrl)" class="business-form-actions">
                <n-tooltip v-if="businessFormHasWritableFields" trigger="hover">
                  <template #trigger>
                    <NButton
                      type="primary"
                      secondary
                      :loading="businessFormSaving"
                      :disabled="isApprovalBusy"
                      @click="() => saveBusinessTaskFormFields({ validate: true, silent: false })"
                    >
                      暂存修改
                    </NButton>
                  </template>
                  同意或驳回时会先提交本节点可编辑字段；这里用于暂存修改，不流转流程。
                </n-tooltip>
                <NButton
                  v-if="useBusinessCodeForm && businessCodeFormUrl"
                  secondary
                  :disabled="isApprovalBusy"
                  @click="openBusinessCodeForm"
                >
                  打开完整业务页
                </NButton>
              </div>
            </div>

            <n-empty
              v-if="!taskFormLoading && !useBusinessManagedForm && !useDynamicForm && !useComponentTaskForm"
              :description="businessFormMissingText"
              size="small"
              class="form-empty"
            />

            <div v-if="useDynamicForm" class="dynamic-form-section">
              <div class="approval-form-title">
                节点动态表单
              </div>
              <AiForm
                ref="dynamicFormRef"
                v-model:value="dynamicFormData"
                :schema="dynamicFormSchema"
                :field-permissions="dynamicFormFieldPermissions"
                :show-actions="false"
                :show-feedback="true"
                :grid-cols="2"
                label-placement="top"
              />
            </div>

            <n-form class="approve-comment-form" :model="approveForm" label-placement="left" :label-width="72">
              <n-form-item label="审批意见" :required="requireComment" :show-feedback="false">
                <FlowCommentPhraseInput
                  v-model="approveForm.comment"
                  :rows="2"
                  :maxlength="200"
                  :disabled="isApprovalBusy"
                  :placeholder="requireComment ? '请输入审批意见' : '审批意见（可选）'"
                />
              </n-form-item>
              <n-form-item v-if="requireSignature" label="审批签名" required>
                <SignaturePad
                  :key="approveSignatureKey"
                  ref="approveSignatureRef"
                  v-model="approveForm.signature"
                  :business-id="currentTask?.taskId || currentTask?.id || ''"
                />
              </n-form-item>
            </n-form>

            <div class="action-buttons">
              <n-popconfirm v-if="canApprove" @positive-click="() => submitApprove('approve')">
                <template #trigger>
                  <NButton type="primary" size="small" :loading="isActionLoading('approve')" :disabled="isApprovalBusy">
                    同意
                  </NButton>
                </template>
                确认同意该审批？
              </n-popconfirm>

              <NButton
                v-if="canReject && canChooseReturnTarget"
                type="error"
                size="small"
                :loading="isActionLoading('reject') || isActionLoading('return')"
                :disabled="isApprovalBusy"
                @click="openRejectTargetModal"
              >
                驳回
              </NButton>
              <n-popconfirm v-else-if="canReject" @positive-click="() => submitApprove('reject')">
                <template #trigger>
                  <NButton type="error" size="small" :loading="isActionLoading('reject')" :disabled="isApprovalBusy">
                    驳回
                  </NButton>
                </template>
                确认驳回该审批？
              </n-popconfirm>

              <n-popconfirm v-if="canRejectToStart" @positive-click="() => submitApprove('rejectToStart')">
                <template #trigger>
                  <NButton type="warning" ghost size="small" :loading="isActionLoading('rejectToStart')" :disabled="isApprovalBusy">
                    退回发起人修改
                  </NButton>
                </template>
                确认保留当前流程并退回发起人修改？修改后可沿原流程重提。
              </n-popconfirm>

              <n-popconfirm v-if="canTerminate" @positive-click="() => submitApprove('terminate')">
                <template #trigger>
                  <NButton type="error" ghost size="small" :loading="isActionLoading('terminate')" :disabled="isApprovalBusy">
                    终结
                  </NButton>
                </template>
                确认终结该流程？
              </n-popconfirm>

              <NButton v-if="canDelegate" size="small" :disabled="isApprovalBusy" @click="handleDelegate">
                转办
              </NButton>

              <NButton
                v-if="currentTask.status === 0 && !currentTask.assignee"
                size="small"
                :loading="isClaimingTask(currentTask)"
                :disabled="isApprovalBusy"
                @click="handleClaim(currentTask)"
              >
                签收
              </NButton>
            </div>
          </template>
        </section>
      </template>
    </FlowTaskDetailShell>

    <!-- 转办弹窗 -->
    <n-modal v-model:show="showDelegateModal" preset="card" title="转办任务" style="width: 480px" :mask-closable="false">
      <n-form :model="delegateForm" label-placement="top">
        <n-form-item label="转办给" required>
          <div class="delegate-user-row">
            <div class="delegate-user-display">
              <template v-if="delegateTargetUser">
                <UserAvatar :name="delegateTargetUser.name || delegateTargetUser.username || 'U'" :size="24" />
                <span class="delegate-user-name">{{ delegateTargetUser.name || delegateTargetUser.username }}</span>
                <span class="delegate-user-id">{{ delegateTargetUser.username }}</span>
              </template>
              <span v-else class="delegate-placeholder">未选择转办人</span>
            </div>
            <NButton size="small" @click="showUserSelectModal = true">
              <i class="i-material-symbols:person-search mr-2" />
              选择人员
            </NButton>
          </div>
        </n-form-item>
        <n-form-item label="转办说明">
          <n-input
            v-model:value="delegateForm.comment"
            type="textarea"
            :rows="2"
            :placeholder="requireComment ? '请输入转办说明' : '请输入转办说明（可选）'"
          />
        </n-form-item>
        <n-form-item v-if="requireSignature" label="审批签名" required>
          <SignaturePad
            :key="delegateSignatureKey"
            ref="delegateSignatureRef"
            v-model="delegateForm.signature"
            :business-id="currentTask?.taskId || currentTask?.id || ''"
          />
        </n-form-item>
      </n-form>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="showDelegateModal = false">
            取消
          </NButton>
          <NButton type="primary" :loading="delegateLoading" @click="submitDelegate">
            确认转办
          </NButton>
        </NSpace>
      </template>
    </n-modal>

    <!-- 用户选择弹窗 -->
    <UserSelectModal
      :show="showUserSelectModal"
      title="选择转办人"
      :multiple="false"
      @update:show="showUserSelectModal = $event"
      @confirm="handleUserSelected"
    />
  </div>
</template>

<script>
import { flowTodoLocalComponents } from './flowTodoLocalComponents'
import { useFlowTodo } from './composables/useFlowTodo'

export default {
  name: 'FlowTodo',
  components: {
    ...flowTodoLocalComponents,
  },
  setup() {
    return useFlowTodo()
  },
}
</script>

<style scoped src="./todo.css"></style>
