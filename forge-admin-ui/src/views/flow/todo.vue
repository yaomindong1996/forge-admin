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
    <!-- 任务列表 -->
    <FlowTaskCardList
      v-model:selected-keys="selectedTaskKeys"
      v-model:search-value="queryParams.title"
      title="我的待办任务"
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
                :schema="businessFormContext.fields || []"
                :field-permissions="businessFormFieldPermissions"
                :show-actions="false"
                :show-feedback="true"
                :grid-cols="businessFormGridCols"
                :label-placement="businessFormLabelPlacement"
                :label-width="businessFormLabelWidth"
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

<script setup>
import { NButton, NSpace, NTreeSelect } from 'naive-ui'
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { businessFlowFormAssets, businessTaskFormContext, completeBusinessTaskAction, saveBusinessTaskFormContext } from '@/api/business-app'
import flowApi from '@/api/flow'
import { AiForm } from '@/components/ai-form'
import { formCreateToAiSchema } from '@/components/ai-form/adapters/formCreate'
import FlowBusinessForm from '@/components/common/FlowBusinessForm.vue'
import UserAvatar from '@/components/common/UserAvatar.vue'
import UserSelectModal from '@/components/common/UserSelectModal.vue'
import DingFlowViewer from '@/components/flow-designer/viewer/DingFlowViewer.vue'
import FlowApprovalChecklist from '@/components/flow/FlowApprovalChecklist.vue'
import FlowCommentPhraseInput from '@/components/flow/FlowCommentPhraseInput.vue'
import FlowPrintAction from '@/components/flow/FlowPrintAction.vue'
import FlowTaskBusinessSummary from '@/components/flow/FlowTaskBusinessSummary.vue'
import FlowTaskCardList from '@/components/flow/FlowTaskCardList.vue'
import FlowTaskDetailShell from '@/components/flow/FlowTaskDetailShell.vue'
import SignaturePad from '@/components/flow/SignaturePad.vue'
import ChildTableEditor from '@/components/page-templates/ChildTableEditor.vue'
import { useDict } from '@/composables/useDict'
import { useUserStore } from '@/store'
import { normalizeFieldPermissions, pickFirstNonEmptyFieldPermissions, pickFirstNonEmptyPermissionSource } from '@/utils/field-permissions'
import { createFlowActionCredentials } from '@/utils/flow-action-idempotency'
import { applyChildTableFieldPermissions } from '@/utils/flow-field-permissions'
import { buildFlowCategoryTreeOptions, resolveFlowCategoryLabel } from './utils/categoryOptions'
import { FLOW_PRIORITY_LABEL_FALLBACK, getFlowPriorityClass, isUrgentFlowPriority, resolveFlowPriorityLevel, shouldShowFlowPriority } from './utils/priority'
import { getBusinessFormDisplayTitle, getProcessDisplayName, getRowDisplayTitle, getTaskDisplayName, getTaskHandlerName } from './utils/processDisplay'
import { loadTaskFormBundle } from './utils/task-form-bundle'

const userStore = useUserStore()
const route = useRoute()
const router = useRouter()
const { dict, getLabel } = useDict('flow_todo_status', 'flow_priority')
const loading = ref(false)
const loadError = ref(false)
const dataSource = ref([])
const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50],
  onChange: (page) => {
    pagination.page = page
    loadData()
  },
  onUpdatePageSize: (size) => {
    pagination.pageSize = size
    pagination.page = 1
    loadData()
  },
})

const queryParams = reactive({ title: '', category: '', status: null })
const categoryTreeOptions = ref([])

const urgentCount = ref(0)
const selectedTaskKeys = ref([])

// 抽屉状态
const showDrawer = ref(false)
const currentTask = ref(null)
const approvalHistory = ref([])

// 业务自定义表单
const taskFormInfo = ref(null)
const approvalPointChecks = ref({})
const formInfoLoading = ref(false)
const dynamicFormRef = ref(null)
const dynamicFormData = ref({})
const dynamicFormSchema = computed(() => formCreateToAiSchema(taskFormInfo.value?.formJson || []))
const businessFormContext = ref(null)
const businessFormData = ref({})
const businessChildFormData = ref({})
const businessFormSavedSnapshot = ref('')
const businessFormRef = ref(null)
const businessChildFormRef = ref(null)
const businessFormLoading = ref(false)
// 统一的表单加载态：表单信息与业务上下文并行加载，模板只展示这一个 loading，避免先后两次转圈
const taskFormLoading = computed(() => formInfoLoading.value || businessFormLoading.value)
const businessFormSaving = ref(false)
const useBusinessObjectForm = computed(() => businessFormContext.value?.configured === true && businessFormContext.value?.formType === 'business-object')
const useBusinessCodeForm = computed(() => businessFormContext.value?.configured === true && businessFormContext.value?.formType === 'business-code')
const useBusinessManagedForm = computed(() => useBusinessObjectForm.value || useBusinessCodeForm.value)
const useDynamicForm = computed(() => {
  if (useBusinessManagedForm.value && Array.isArray(businessFormContext.value?.fields) && businessFormContext.value.fields.length > 0)
    return false
  return dynamicFormSchema.value.length > 0
})
const useExternalForm = computed(() => !useBusinessManagedForm.value && taskFormInfo.value?.formType === 'external' && taskFormInfo.value?.formUrl)
const businessFormTitle = computed(() => getBusinessFormDisplayTitle(businessFormContext.value, '业务表单'))
const businessFormWarnings = computed(() => Array.isArray(businessFormContext.value?.warnings) ? businessFormContext.value.warnings : [])
const businessFormFieldPermissions = computed(() => pickFirstNonEmptyFieldPermissions([
  businessFormContext.value?.fieldPermissions,
  taskFormInfo.value?.fieldPermissions,
  taskFormInfo.value?.formFieldPermissions,
]))
const businessFormChildrenConfig = computed(() => {
  const children = Array.isArray(businessFormContext.value?.childrenConfig) ? businessFormContext.value.childrenConfig : []
  return applyChildTableFieldPermissions(
    children.filter(child => child?.showInDetail !== false && Array.isArray(child.fields) && child.fields.length),
    [
      taskFormInfo.value?.formFieldPermissions,
      businessFormContext.value?.taskFormInfo?.formFieldPermissions,
      businessFormFieldPermissions.value,
    ],
  )
})
const businessFormHasWritableFields = computed(() => hasWritableBusinessFormFields(businessFormContext.value))
const flowPrintHasUnsavedChanges = computed(() => {
  const businessDirty = Boolean(businessFormSavedSnapshot.value)
    && businessFormSavedSnapshot.value !== createBusinessFormSnapshot()
  const dynamicDirty = Boolean(taskFormInfo.value)
    && JSON.stringify(dynamicFormData.value) !== JSON.stringify(taskFormInfo.value?.variables || {})
  return businessDirty || dynamicDirty
})
const businessCodeFormUrl = computed(() => businessFormContext.value?.formUrl || businessFormContext.value?.formRef?.formUrl || '')
const businessFormGridCols = computed(() => Math.max(1, Number(businessFormContext.value?.gridCols || 1)))
const businessFormLabelPlacement = computed(() => ['left', 'top'].includes(businessFormContext.value?.labelPlacement)
  ? businessFormContext.value.labelPlacement
  : 'left')
const businessFormLabelWidth = computed(() => businessFormContext.value?.labelWidth || '100')
const useBusinessCodeComponentForm = computed(() => useBusinessCodeForm.value && Boolean(businessCodeFormUrl.value))
const useComponentTaskForm = computed(() => useExternalForm.value || useBusinessCodeComponentForm.value)
const businessFormMissingText = computed(() => {
  const warnings = Array.isArray(businessFormContext.value?.warnings)
    ? businessFormContext.value.warnings.filter(Boolean)
    : []
  if (warnings.length)
    return warnings[0]
  return '当前节点未加载到可渲染的业务应用表单。请确认流程已部署，且待办能关联到业务单据。'
})
const componentTaskFormUrl = computed(() => useBusinessCodeComponentForm.value ? businessCodeFormUrl.value : taskFormInfo.value?.formUrl)
const componentTaskFormInfo = computed(() => ({
  taskId: businessFormContext.value?.taskId || taskFormInfo.value?.taskId,
  businessKey: businessFormContext.value?.businessKey || taskFormInfo.value?.businessKey,
  processInstanceId: businessFormContext.value?.processInstanceId || taskFormInfo.value?.processInstanceId,
  taskDefKey: businessFormContext.value?.taskDefKey || taskFormInfo.value?.taskDefKey,
  processDefKey: businessFormContext.value?.processDefKey || taskFormInfo.value?.processDefKey,
}))
const dynamicFormFieldPermissions = computed(() => pickFirstNonEmptyPermissionSource([
  taskFormInfo.value?.fieldPermissions,
  taskFormInfo.value?.formFieldPermissions,
]))
const businessFormRenderContext = computed(() => ({
  task: currentTask.value,
  taskFormInfo: taskFormInfo.value,
  businessFormContext: businessFormContext.value,
  formAssets: businessFormContext.value?.formAssets || [],
}))
const taskPolicySource = computed(() => taskFormInfo.value || businessFormContext.value || {})
const canApprove = computed(() => taskPolicySource.value?.allowApprove !== false)
const canReject = computed(() => taskPolicySource.value?.allowReject !== false)
const canRejectToStart = computed(() => taskPolicySource.value?.allowRejectToStart === true)
const canDelegate = computed(() => taskPolicySource.value?.allowDelegate !== false)
const canReturn = computed(() => taskPolicySource.value?.allowReturn === true)
const returnTargetOptions = computed(() => (Array.isArray(taskPolicySource.value?.returnTargets)
  ? taskPolicySource.value.returnTargets
  : []).map(item => ({
  label: item.activityName || item.activityId,
  value: item.activityId,
})))
const canChooseReturnTarget = computed(() => {
  return taskPolicySource.value?.allowMultiReturn === true && returnTargetOptions.value.length > 0
})
const canDirectSend = computed(() => taskPolicySource.value?.allowDirectSend === true)
const canTerminate = computed(() => taskPolicySource.value?.allowTerminate === true)
const requireComment = computed(() => taskPolicySource.value?.requireComment !== false)
const requireSignature = computed(() => taskPolicySource.value?.requireSignature === true)
const approvalPolicy = computed(() => ({
  allowApprove: canApprove.value,
  allowReject: canReject.value,
  allowDelegate: canDelegate.value,
  allowReturn: canReturn.value,
  allowTerminate: canTerminate.value,
  requireComment: requireComment.value,
  requireSignature: requireSignature.value,
  allowDirectSend: canDirectSend.value,
  returnSourceActivityId: taskPolicySource.value?.returnSourceActivityId,
  returnSourceActivityName: taskPolicySource.value?.returnSourceActivityName,
}))

// 审批表单
const approveLoading = ref(false)
const approveForm = reactive({ action: '', comment: '', signature: '' })
const selectedReturnTarget = ref(null)
const rejectTargetVisible = ref(false)
const pendingRejectSubmit = ref(null)
const directSendAfterReturn = ref(false)
const approveSignatureRef = ref(null)
const approveSignatureKey = ref(0)
const claimLoadingTaskId = ref('')
const quickActionVisible = ref(false)
const quickActionLoading = ref(false)
const quickActionType = ref('approve')
const quickActionTargets = ref([])
const quickActionFailedTargets = ref([])
const quickActionForm = reactive({ comment: '' })
const quickActionInputRef = ref(null)
const quickActionIsApprove = computed(() => quickActionType.value === 'approve')
const quickActionTitle = computed(() => quickActionIsApprove.value ? '同意' : '驳回')
const quickActionTitleId = 'flow-todo-quick-action-title'
const quickActionSubject = computed(() => {
  if (quickActionTargets.value.length === 1)
    return getRowDisplayTitle(quickActionTargets.value[0])
  return `处理 ${quickActionTargets.value.length} 条待办`
})
const quickActionMeta = computed(() => {
  const rows = quickActionTargets.value
  if (rows.length === 1) {
    const row = rows[0]
    const node = getTaskDisplayName(row, '')
    const applicant = row?.startUserName
    return [node, applicant ? `申请人 ${applicant}` : ''].filter(Boolean).join(' · ')
  }
  if (!rows.length)
    return ''
  const names = rows.slice(0, 2).map(row => getRowDisplayTitle(row)).filter(Boolean)
  return names.join('、') + (rows.length > 2 ? ` 等${rows.length}条` : '')
})

// 转办
const showDelegateModal = ref(false)
const showUserSelectModal = ref(false)
const delegateLoading = ref(false)
const delegateTargetUser = ref(null)
const delegateForm = reactive({ comment: '', signature: '' })
const delegateSignatureRef = ref(null)
const delegateSignatureKey = ref(0)
const routeTaskOpening = ref(false)

const statusOptions = computed(() => toNumberOptions(dict.value.flow_todo_status))
const isApprovalBusy = computed(() => approveLoading.value || delegateLoading.value || businessFormSaving.value || Boolean(claimLoadingTaskId.value))

// 优先级
function getPriorityClass(p) {
  return getFlowPriorityClass(p)
}
function getPriorityText(p) {
  if (!shouldShowFlowPriority(p))
    return ''
  const level = resolveFlowPriorityLevel(p)
  const label = getLabel('flow_priority', level)
  return String(label) === String(level) ? FLOW_PRIORITY_LABEL_FALLBACK[level] : label
}

function getCategoryDisplayName(row) {
  return row?.categoryName || resolveFlowCategoryLabel(row?.category, categoryTreeOptions.value, '-') || '-'
}

function toNumberOptions(options = []) {
  return options.map(item => ({
    ...item,
    value: Number(item.value),
  }))
}

function resetBusinessTaskForm() {
  businessFormContext.value = null
  businessFormData.value = {}
  businessChildFormData.value = {}
  businessFormSavedSnapshot.value = ''
  businessFormLoading.value = false
  businessFormSaving.value = false
}

function createBusinessFormSnapshot() {
  return JSON.stringify({
    main: businessFormData.value || {},
    children: businessChildFormData.value || {},
  })
}

function rememberBusinessFormSnapshot() {
  businessFormSavedSnapshot.value = createBusinessFormSnapshot()
}

function normalizeBusinessRecordData(recordData) {
  if (recordData && typeof recordData === 'object' && !Array.isArray(recordData)) {
    const main = recordData.main
    if (main && typeof main === 'object' && !Array.isArray(main))
      return { ...main }
    const { children, ...mainRecord } = recordData
    return { ...mainRecord }
  }
  return {}
}

function normalizeBusinessChildrenData(recordData) {
  const source = recordData?.children && typeof recordData.children === 'object' && !Array.isArray(recordData.children)
    ? recordData.children
    : {}
  const result = {}
  businessFormChildrenConfig.value.forEach((child) => {
    const key = resolveBusinessChildKey(child)
    result[key] = Array.isArray(source[key]) ? source[key] : []
  })
  return result
}

function resolveBusinessChildKey(child = {}) {
  return child.modelCode || child.relationKey || child.key || child.tableName || 'children'
}

function logBusinessApprovalChildren(source, recordData) {
  console.warn('[FlowApprovalChildren]', {
    source,
    configKey: businessFormContext.value?.configKey,
    recordId: businessFormContext.value?.recordId,
    childrenConfig: businessFormChildrenConfig.value.map(child => ({
      key: resolveBusinessChildKey(child),
      modelCode: child.modelCode,
      tableName: child.tableName,
      relationType: child.relationType,
      sourceField: child.sourceField,
      targetField: child.targetField,
      fieldCount: Array.isArray(child.fields) ? child.fields.length : 0,
    })),
    recordChildren: summarizeBusinessChildren(recordData?.children),
    renderChildren: summarizeBusinessChildren(businessChildFormData.value),
  })
}

function summarizeBusinessChildren(children) {
  if (!children || typeof children !== 'object' || Array.isArray(children))
    return {}
  return Object.fromEntries(Object.entries(children).map(([key, rows]) => [
    key,
    {
      rows: Array.isArray(rows) ? rows.length : 0,
      rowIds: Array.isArray(rows) ? rows.slice(0, 5).map(row => row?.id) : [],
      firstFields: Array.isArray(rows) && rows[0] ? Object.keys(rows[0]).slice(0, 12) : [],
    },
  ]))
}

function isSyntheticTestBusinessKey(value) {
  const text = String(value || '').trim()
  return text === 'FLOW_TEST' || text.startsWith('FLOW_TEST:')
}

function resolveTaskIdentityBusinessKey(context = {}, formInfo = {}, row = {}) {
  const taskKey = formInfo.businessKey || row.businessKey || taskFormInfo.value?.businessKey || currentTask.value?.businessKey
  const contextKey = context.businessKey
  if (context.recordId && contextKey && !isSyntheticTestBusinessKey(contextKey) && !isSyntheticTestBusinessKey(taskKey))
    return contextKey
  return taskKey || contextKey
}

function buildBusinessTaskFormQuery(row = {}, formInfo = {}) {
  const formRef = resolveTaskFormRef(formInfo)
  const taskId = formInfo.taskId || row.taskId || row.id
  const businessKey = resolveTaskIdentityBusinessKey({}, formInfo, row)
  return compactParams({
    taskId,
    businessKey,
    processInstanceId: formInfo.processInstanceId || row.processInstanceId,
    processDefKey: formInfo.processDefKey || row.processDefKey || row.processDefinitionKey,
    taskDefKey: formInfo.taskDefKey || row.taskDefKey || row.taskDefinitionKey,
    objectCode: formInfo.objectCode || formRef.objectCode || row.objectCode,
    recordId: isSyntheticTestBusinessKey(businessKey)
      ? undefined
      : (formInfo.recordId || formRef.recordId || row.recordId),
    formKey: formInfo.formKey || formRef.formKey,
  })
}

function resolveTaskFormRef(formInfo = {}) {
  if (formInfo.formRef && typeof formInfo.formRef === 'object' && !Array.isArray(formInfo.formRef))
    return formInfo.formRef
  const raw = formInfo.formJson
  if (raw && typeof raw === 'object' && !Array.isArray(raw))
    return raw.formRef && typeof raw.formRef === 'object' ? { ...raw, ...raw.formRef } : raw
  if (typeof raw === 'string' && raw.trim()) {
    try {
      const parsed = JSON.parse(raw)
      if (parsed && typeof parsed === 'object' && !Array.isArray(parsed))
        return parsed.formRef && typeof parsed.formRef === 'object' ? { ...parsed, ...parsed.formRef } : parsed
    }
    catch {
      return {}
    }
  }
  return {}
}

function compactParams(source = {}) {
  const result = {}
  Object.entries(source).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '')
      result[key] = value
  })
  return result
}

function hasBusinessTaskFormQuery(query = {}) {
  return Boolean(query.taskId || query.processInstanceId || query.businessKey || (query.objectCode && query.recordId))
}

function hasWritableBusinessFormFields(context) {
  const mainWritable = Array.isArray(context?.fields) && context.fields.some(field =>
    field?.writable === true && field?.readonly !== true && field?.disabled !== true,
  )
  const childWritable = Array.isArray(context?.childrenConfig) && context.childrenConfig.some((child) => {
    if (child?.allowCreate === true || child?.allowDelete === true || child?.allowUpdate === true)
      return true
    return Array.isArray(child?.fields) && child.fields.some(field =>
      field?.writable === true && field?.readonly !== true && field?.disabled !== true,
    )
  })
  return mainWritable || childWritable
}

async function loadBusinessTaskFormContext(row, formInfo) {
  businessFormContext.value = null
  businessFormData.value = {}
  businessChildFormData.value = {}

  const query = buildBusinessTaskFormQuery(row, formInfo)
  if (!hasBusinessTaskFormQuery(query))
    return null

  businessFormLoading.value = true
  try {
    const res = await businessTaskFormContext(query)
    if (res.code !== 200) {
      console.error('加载业务表单上下文失败', res.message)
      return null
    }
    businessFormContext.value = res.data || null
    if (res.data?.taskFormInfo && typeof res.data.taskFormInfo === 'object')
      applyTaskFormInfo(res.data.taskFormInfo)
    businessFormData.value = normalizeBusinessRecordData(res.data?.recordData)
    businessChildFormData.value = normalizeBusinessChildrenData(res.data?.recordData)
    rememberBusinessFormSnapshot()
    logBusinessApprovalChildren('todo', res.data?.recordData)
    return businessFormContext.value
  }
  catch (error) {
    console.error('加载业务表单上下文失败', error)
    return null
  }
  finally {
    businessFormLoading.value = false
  }
}

async function loadTaskFormInfo(taskId) {
  if (!taskId)
    return null
  try {
    const res = await flowApi.getTaskFormInfo(taskId)
    if (res.code === 200) {
      return applyTaskFormInfo(res.data)
    }
  }
  catch (error) {
    console.error('加载表单信息失败', error)
  }
  return null
}

function applyTaskFormInfo(formInfo) {
  if (!formInfo || typeof formInfo !== 'object')
    return null
  taskFormInfo.value = formInfo
  dynamicFormData.value = { ...(formInfo.variables || {}) }
  approvalPointChecks.value = Object.fromEntries(
    (formInfo.approvalPoints || []).map(point => [point.id, false]),
  )
  return taskFormInfo.value
}

function isConfiguredBusinessTaskForm(context) {
  return context?.configured === true && ['business-object', 'business-code'].includes(context?.formType)
}

async function hydrateBusinessFormFromAssets(formInfo) {
  if (!formInfo || isConfiguredBusinessTaskForm(businessFormContext.value))
    return
  const formRef = resolveTaskFormRef(formInfo)
  const objectCode = String(formInfo.objectCode || formRef.objectCode || '').trim()
  const formKey = String(formInfo.formKey || formRef.formKey || '').trim()
  const formType = String(formInfo.formType || formRef.formMode || formRef.type || '').toLowerCase()
  const isBusinessForm = formType === 'business'
    || formType === 'business_object_form'
    || formType === 'business-object'
    || String(formRef.formMode || formRef.type || '').toUpperCase() === 'BUSINESS_OBJECT_FORM'
  if (!isBusinessForm || !objectCode)
    return
  try {
    const res = await businessFlowFormAssets(objectCode, {
      includeInternal: true,
      applicationId: formRef.applicationId || undefined,
    })
    const assets = Array.isArray(res.data) ? res.data : res.data?.formAssets || []
    const selected = assets.find(item => String(item?.formKey || '') === formKey) || assets[0]
    const fieldPermissions = pickFirstNonEmptyFieldPermissions([
      formInfo.formFieldPermissions,
      formInfo.fieldPermissions,
    ])
    const fields = normalizeFallbackBusinessFields(selected, fieldPermissions)
    if (!fields.length)
      return
    businessFormContext.value = {
      configured: true,
      formType: 'business-object',
      formKey: selected?.formKey || formKey,
      formName: selected?.formName || formInfo.formName || formKey,
      objectCode,
      taskId: formInfo.taskId,
      processInstanceId: formInfo.processInstanceId,
      processDefKey: formInfo.processDefKey,
      taskDefKey: formInfo.taskDefKey,
      businessKey: formInfo.businessKey,
      fields,
      formAssets: assets,
      fieldPermissions,
      recordData: formInfo.variables || {},
    }
    businessFormData.value = { ...(formInfo.variables || {}) }
    rememberBusinessFormSnapshot()
  }
  catch (error) {
    console.error('按表单资产回退渲染失败', error)
  }
}

function isChildBusinessFormField(field = {}, fieldCode = '') {
  const scope = String(field?.scope || '').trim().toLowerCase()
  if (scope === 'child' || String(field?.childKey || '').trim())
    return true
  return String(fieldCode || '').includes('__')
}

function normalizeFallbackBusinessFields(asset = null, permissions = []) {
  if (!asset || typeof asset !== 'object')
    return []
  const catalog = Array.isArray(asset.fieldCatalog) && asset.fieldCatalog.length
    ? asset.fieldCatalog
    : Array.isArray(asset.fields) ? asset.fields : []
  const permissionMap = new Map(normalizeFieldPermissions(permissions).map(item => [item.field, item]))
  const seen = new Set()
  return catalog
    .map((field) => {
      const fieldCode = String(field?.field || field?.fieldCode || field?.name || field?.key || '').trim()
      if (!fieldCode || seen.has(fieldCode) || isChildBusinessFormField(field, fieldCode))
        return null
      seen.add(fieldCode)
      const permission = permissionMap.get(fieldCode)
      const readable = permission ? permission.readable !== false : true
      if (!readable)
        return null
      const writable = permission ? permission.writable === true : false
      const required = writable && permission?.required === true
      return {
        field: fieldCode,
        code: fieldCode,
        prop: fieldCode,
        label: String(field?.label || field?.title || field?.fieldName || fieldCode).trim(),
        type: field?.type || field?.componentType || 'input',
        required,
        readonly: !writable,
        disabled: !writable,
        writable,
        visible: true,
      }
    })
    .filter(Boolean)
}

function buildBusinessTaskFormSavePayload() {
  const context = businessFormContext.value || {}
  const businessKey = resolveTaskIdentityBusinessKey(context, taskFormInfo.value, currentTask.value)
  const writableMainData = {}
  ;(Array.isArray(context.fields) ? context.fields : []).forEach((field) => {
    if (field?.writable === true && field?.readonly !== true && field?.disabled !== true) {
      const code = field.field || field.fieldCode
      if (code && Object.prototype.hasOwnProperty.call(businessFormData.value, code))
        writableMainData[code] = businessFormData.value[code]
    }
  })
  const childValue = businessChildFormRef.value?.getValue?.() || businessChildFormData.value
  const childPayload = {}
  businessFormChildrenConfig.value.forEach((child) => {
    const key = resolveBusinessChildKey(child)
    const writableFields = new Set((Array.isArray(child.fields) ? child.fields : [])
      .filter(field => field?.writable === true && field?.readonly !== true && field?.disabled !== true)
      .flatMap(field => [field.field, field.fieldCode].filter(Boolean)))
    const rows = Array.isArray(childValue?.[key]) ? childValue[key] : []
    if (!writableFields.size && child.allowCreate !== true && child.allowUpdate !== true && child.allowDelete !== true)
      return
    childPayload[key] = rows.map((row) => {
      const next = {}
      Object.entries(row || {}).forEach(([field, value]) => {
        if (field === 'id' || field === 'ID' || field === '_deleted' || field === '__deleted' || writableFields.has(field))
          next[field] = value
      })
      return next
    })
  })
  return compactParams({
    taskId: context.taskId || taskFormInfo.value?.taskId || currentTask.value?.taskId || currentTask.value?.id,
    businessKey,
    processInstanceId: context.processInstanceId || taskFormInfo.value?.processInstanceId || currentTask.value?.processInstanceId,
    processDefKey: context.processDefKey || taskFormInfo.value?.processDefKey || currentTask.value?.processDefKey || currentTask.value?.processDefinitionKey,
    taskDefKey: context.taskDefKey || taskFormInfo.value?.taskDefKey || currentTask.value?.taskDefKey || currentTask.value?.taskDefinitionKey,
    objectCode: context.objectCode || taskFormInfo.value?.objectCode || currentTask.value?.objectCode,
    recordId: isSyntheticTestBusinessKey(businessKey)
      ? context.recordId
      : (context.recordId || taskFormInfo.value?.recordId || currentTask.value?.recordId),
    formKey: context.formKey || taskFormInfo.value?.formKey,
    data: {
      main: writableMainData,
      ...(businessFormChildrenConfig.value.length
        ? { children: childPayload }
        : {}),
    },
  })
}

async function saveBusinessTaskFormFields(options = {}) {
  if (!useBusinessManagedForm.value || !businessFormHasWritableFields.value)
    return null

  const { validate = true, silent = true } = options
  businessFormSaving.value = true
  try {
    if (validate)
      await businessFormRef.value?.validate?.()
    if (validate)
      await businessChildFormRef.value?.validate?.()

    const res = await saveBusinessTaskFormContext(buildBusinessTaskFormSavePayload())
    if (res.code !== 200)
      throw new Error(res.message || '业务字段保存失败')

    businessFormContext.value = res.data || businessFormContext.value
    businessFormData.value = normalizeBusinessRecordData(businessFormContext.value?.recordData || businessFormData.value)
    businessChildFormData.value = normalizeBusinessChildrenData(businessFormContext.value?.recordData)
    rememberBusinessFormSnapshot()
    if (!silent)
      window.$message.success('修改已暂存')
    return businessFormContext.value
  }
  catch (error) {
    if (!silent) {
      window.$message.error(error?.message || '业务字段保存失败')
      return null
    }
    throw error
  }
  finally {
    businessFormSaving.value = false
  }
}

async function buildBusinessTaskActionPayload(action, comment, signature, variables = {}) {
  const context = businessFormContext.value || {}
  const taskId = context.taskId || taskFormInfo.value?.taskId || currentTask.value?.taskId || currentTask.value?.id
  const actionVariables = buildActionVariables(action, variables)
  const data = useBusinessManagedForm.value && businessFormHasWritableFields.value
    ? buildBusinessTaskFormSavePayload().data
    : undefined
  const credentials = await createFlowActionCredentials(action, taskId, {
    comment,
    signature,
    variables: actionVariables,
    data,
  })
  return compactParams({
    action,
    taskId,
    businessKey: resolveTaskIdentityBusinessKey(context, taskFormInfo.value, currentTask.value),
    processInstanceId: context.processInstanceId || taskFormInfo.value?.processInstanceId || currentTask.value?.processInstanceId,
    processDefKey: context.processDefKey || taskFormInfo.value?.processDefKey || currentTask.value?.processDefKey || currentTask.value?.processDefinitionKey,
    taskDefKey: context.taskDefKey || taskFormInfo.value?.taskDefKey || currentTask.value?.taskDefKey || currentTask.value?.taskDefinitionKey,
    objectCode: context.objectCode || taskFormInfo.value?.objectCode || currentTask.value?.objectCode,
    recordId: context.recordId || taskFormInfo.value?.recordId || currentTask.value?.recordId,
    formKey: context.formKey || taskFormInfo.value?.formKey,
    userId: userStore.userId,
    comment,
    signature,
    variables: actionVariables,
    targetActivityId: action === 'return' ? selectedReturnTarget.value : undefined,
    data,
    approvalPointResults: buildApprovalPointResults(),
    ...credentials,
  })
}

async function submitTaskAction(action, comment, signature, variables = {}) {
  if (isConfiguredBusinessTaskForm(businessFormContext.value)) {
    return completeBusinessTaskAction(await buildBusinessTaskActionPayload(action, comment, signature, variables))
  }
  const api = resolveActionApi(action)
  const taskId = currentTask.value.taskId || currentTask.value.id
  const credentials = await createFlowActionCredentials(action, taskId, { comment, signature, variables })
  return api({
    taskId,
    userId: userStore.userId,
    comment,
    signature,
    variables: buildActionVariables(action, variables),
    targetActivityId: action === 'return' ? selectedReturnTarget.value : undefined,
    approvalPointResults: buildApprovalPointResults(),
    ...credentials,
  })
}

function buildActionVariables(action, variables = {}) {
  const result = variables && typeof variables === 'object' ? { ...variables } : {}
  if (action === 'approve' && canDirectSend.value)
    result.directSend = directSendAfterReturn.value
  return result
}

function openBusinessCodeForm() {
  const url = businessCodeFormUrl.value
  if (!url)
    return
  if (/^https?:\/\//i.test(url)) {
    window.open(url, '_blank', 'noopener,noreferrer')
    return
  }
  router.push({
    path: url,
    query: compactParams({
      taskId: businessFormContext.value?.taskId || taskFormInfo.value?.taskId || currentTask.value?.taskId,
      businessKey: businessFormContext.value?.businessKey,
      processInstanceId: businessFormContext.value?.processInstanceId,
      taskDefKey: businessFormContext.value?.taskDefKey,
      processDefKey: businessFormContext.value?.processDefKey,
      objectCode: businessFormContext.value?.objectCode,
      recordId: businessFormContext.value?.recordId,
      source: 'flowTodo',
    }),
  })
}

async function loadQuickBusinessTaskFormContext(row, formInfo) {
  const query = buildBusinessTaskFormQuery(row, formInfo)
  if (!hasBusinessTaskFormQuery(query))
    return null
  const res = await businessTaskFormContext(query)
  if (res.code !== 200)
    throw new Error(res.message || '业务表单策略加载失败')
  return res.data || null
}

async function openDrawer(row) {
  currentTask.value = row
  approveForm.comment = ''
  approveForm.action = ''
  approveForm.signature = ''
  selectedReturnTarget.value = null
  directSendAfterReturn.value = false
  approveSignatureKey.value += 1
  approvalHistory.value = []
  taskFormInfo.value = null
  approvalPointChecks.value = {}
  dynamicFormData.value = {}
  resetBusinessTaskForm()
  showDrawer.value = true

  const promises = []
  if (row.processInstanceId) {
    promises.push(
      flowApi.getProcessHistory(row.processInstanceId)
        .then((res) => {
          if (res.code === 200)
            approvalHistory.value = res.data || []
        })
        .catch(e => console.error('加载审批历史失败', e)),
    )
  }

  const taskId = row.taskId || row.id
  if (taskId) {
    formInfoLoading.value = true
    promises.push((async () => {
      try {
        // 业务上下文已携带服务端本次解析过的 Flow 表单快照，常规路径只发一个请求；
        // 直连 Flow 仅作为新旧服务滚动升级期间的兼容兜底。
        const { formInfo } = await loadTaskFormBundle({
          row,
          loadBusinessContext: loadBusinessTaskFormContext,
          loadFlowFormInfo: () => loadTaskFormInfo(taskId),
          isConfiguredBusinessContext: isConfiguredBusinessTaskForm,
        })
        if (!isConfiguredBusinessTaskForm(businessFormContext.value))
          await hydrateBusinessFormFromAssets(formInfo)
      }
      finally {
        formInfoLoading.value = false
        businessFormLoading.value = false
      }
    })())
  }

  await Promise.all(promises)
}

async function handleExternalFormSubmit({ action, comment, signature, variables }) {
  const approvalSignature = signature || variables?.signature
  if (action === 'reject' && canChooseReturnTarget.value) {
    pendingRejectSubmit.value = { comment, signature: approvalSignature, variables }
    openRejectTargetModal()
    return
  }
  if (!canRunAction(action))
    return
  if (!validateApprovalInput(comment, approvalSignature, null, action))
    return

  approveForm.action = action
  approveLoading.value = true
  try {
    const res = await submitTaskAction(action, comment, approvalSignature, variables)
    if (res.code === 200) {
      window.$message.success(getActionSuccessText(action))
      showDrawer.value = false
      loadData()
    }
    else {
      window.$message.error(res.message || '操作失败')
    }
  }
  catch (error) {
    window.$message.error(error?.message || '操作失败')
  }
  finally {
    approveLoading.value = false
    approveForm.action = ''
  }
}

function canRunAction(action) {
  const allowed = {
    approve: canApprove.value,
    reject: canReject.value,
    rejectToStart: canRejectToStart.value,
    return: canReturn.value || canChooseReturnTarget.value,
    terminate: canTerminate.value,
    delegate: canDelegate.value,
  }
  if (allowed[action] === false) {
    window.$message.warning('当前节点不允许执行该操作')
    return false
  }
  return true
}

function hasSignatureValue(signature, signatureRef) {
  return Boolean(signature?.trim()) || Boolean(signatureRef?.hasSignature?.())
}

async function resolveSignature(signatureRef, signature) {
  if (!requireSignature.value)
    return signature || ''
  if (!signatureRef?.upload)
    return signature || ''

  try {
    return await signatureRef.upload()
  }
  catch (error) {
    throw new Error(error?.message || '签名图片保存失败')
  }
}

function currentApprovalPoints() {
  return Array.isArray(taskFormInfo.value?.approvalPoints) ? taskFormInfo.value.approvalPoints : []
}

function requiredApprovalPointsIncomplete() {
  return currentApprovalPoints()
    .filter(point => point?.required === true)
    .some(point => !approvalPointChecks.value?.[point.id])
}

function buildApprovalPointResults() {
  return currentApprovalPoints()
    .filter(point => point?.id)
    .map(point => ({
      id: point.id,
      content: point.content,
      required: point.required === true,
      checked: Boolean(approvalPointChecks.value?.[point.id]),
    }))
}

function validateApprovalInput(comment, signature, signatureRef = null, action = '') {
  if (action === 'approve' && requiredApprovalPointsIncomplete()) {
    window.$message.warning('请完成全部必审要点')
    return false
  }
  if (requireComment.value && !comment?.trim()) {
    window.$message.warning('请输入审批意见')
    return false
  }
  if (requireSignature.value && !hasSignatureValue(signature, signatureRef)) {
    window.$message.warning('请完成手写签名')
    return false
  }
  return true
}

function resolveActionApi(action) {
  const apiMap = {
    approve: flowApi.approveTask,
    reject: flowApi.rejectTask,
    rejectToStart: flowApi.rejectToStartTask,
    return: flowApi.returnTask,
    terminate: flowApi.terminateTask,
  }
  return apiMap[action] || flowApi.approveTask
}

function getActionSuccessText(action) {
  const textMap = {
    approve: '审批通过',
    reject: '已驳回',
    rejectToStart: '已退回发起人修改，可在原流程修改后重提',
    return: '已退回',
    terminate: '流程已终结',
  }
  return textMap[action] || '操作成功'
}

function openRejectTargetModal() {
  selectedReturnTarget.value = null
  rejectTargetVisible.value = true
}

async function confirmRejectToTarget() {
  if (!selectedReturnTarget.value) {
    window.$message.warning('请选择驳回至哪个已审批节点')
    return
  }
  const pending = pendingRejectSubmit.value
  pendingRejectSubmit.value = null
  rejectTargetVisible.value = false
  if (pending)
    await handleExternalFormSubmit({ action: 'return', ...pending })
  else
    await submitApprove('return')
}

async function submitApprove(action) {
  if (action === 'reject' && canChooseReturnTarget.value) {
    pendingRejectSubmit.value = null
    openRejectTargetModal()
    return
  }
  if (!canRunAction(action))
    return
  if (!validateApprovalInput(approveForm.comment, approveForm.signature, approveSignatureRef.value, action))
    return
  approveForm.action = action
  approveLoading.value = true
  try {
    const signature = await resolveSignature(approveSignatureRef.value, approveForm.signature)
    approveForm.signature = signature
    const variables = await collectDynamicFormVariables(action)
    if (useBusinessManagedForm.value && businessFormHasWritableFields.value) {
      await businessFormRef.value?.validate?.()
      await businessChildFormRef.value?.validate?.()
    }
    const res = await submitTaskAction(action, approveForm.comment, signature, variables)
    if (res.code === 200) {
      window.$message.success(getActionSuccessText(action))
      showDrawer.value = false
      loadData()
    }
    else {
      window.$message.error(res.message || '操作失败')
    }
  }
  catch (error) {
    window.$message.error(error?.message || '操作失败')
  }
  finally {
    approveLoading.value = false
    approveForm.action = ''
  }
}

function isActionLoading(action) {
  return approveLoading.value && approveForm.action === action
}

function resolveQuickActionTargets(targets = []) {
  return targets
    .map((target) => {
      if (target && typeof target === 'object')
        return target
      return dataSource.value.find(row => String(row.id) === String(target) || String(row.taskId) === String(target))
    })
    .filter(Boolean)
}

function openQuickAction(action, targets) {
  const resolvedTargets = resolveQuickActionTargets(targets)
  if (resolvedTargets.length === 0) {
    window.$message.warning('请选择待办任务')
    return
  }
  quickActionType.value = action
  quickActionTargets.value = resolvedTargets
  quickActionFailedTargets.value = []
  quickActionForm.comment = action === 'approve' ? '同意' : '驳回'
  quickActionVisible.value = true
  nextTick(() => quickActionInputRef.value?.focus?.())
}

function isCandidateTask(row) {
  return row?.status === 0 && !row?.assignee
}

async function claimTaskBeforeQuickAction(row, taskId) {
  if (!isCandidateTask(row))
    return
  const res = await flowApi.claimTask(taskId, userStore.userId)
  if (res.code !== 200)
    throw new Error(res.message || '签收失败')
}

function assertQuickActionAllowed(action, formInfo, businessFormContext = null) {
  const businessManaged = businessFormContext?.configured === true
    && ['business-object', 'business-code'].includes(businessFormContext?.formType)
  if (action === 'approve' && formInfo?.allowApprove === false)
    throw new Error('当前节点不允许同意')
  if (action === 'reject' && formInfo?.allowReject === false)
    throw new Error('当前节点不允许驳回')
  if (action === 'reject' && formInfo?.allowMultiReturn === true && Array.isArray(formInfo.returnTargets) && formInfo.returnTargets.length)
    throw new Error('该流程已开启指定节点驳回，请进入详情选择驳回节点')
  if (action === 'rejectToStart' && formInfo?.allowRejectToStart !== true)
    throw new Error('当前节点不允许退回发起人修改')
  if (formInfo?.requireSignature === true)
    throw new Error('需要手写签名，请进入详情处理')
  if (action === 'approve' && !businessManaged && formInfo?.formType === 'dynamic' && formInfo?.formJson)
    throw new Error('需要填写节点表单，请进入详情处理')
  if (action === 'approve' && !businessManaged && formInfo?.formType === 'external' && formInfo?.formUrl)
    throw new Error('需要填写业务表单，请进入详情处理')
  if (action === 'approve' && businessFormContext?.configured === true && businessFormContext?.formType === 'business-code')
    throw new Error('需要进入业务表单处理')
  if (action === 'approve' && businessFormContext?.configured === true && hasWritableBusinessFormFields(businessFormContext))
    throw new Error('需要填写业务表单，请进入详情处理')
  if (action === 'approve' && Array.isArray(formInfo?.approvalPoints) && formInfo.approvalPoints.some(point => point?.required === true))
    throw new Error('需要勾选审批要点，请进入详情处理')
}

async function executeQuickAction(action, row, comment) {
  const taskId = row.taskId || row.id
  if (!taskId)
    throw new Error('缺少任务ID')

  await claimTaskBeforeQuickAction(row, taskId)

  const { businessContext, formInfo } = await loadTaskFormBundle({
    row,
    loadBusinessContext: loadQuickBusinessTaskFormContext,
    loadFlowFormInfo: async () => {
      const formRes = await flowApi.getTaskFormInfo(taskId)
      if (formRes.code !== 200)
        throw new Error(formRes.message || '审批策略加载失败')
      return formRes.data || {}
    },
    isConfiguredBusinessContext: isConfiguredBusinessTaskForm,
  })
  assertQuickActionAllowed(action, formInfo, businessContext)

  const res = isConfiguredBusinessTaskForm(businessContext)
    ? await completeBusinessTaskAction(compactParams({
        action,
        taskId,
        businessKey: businessContext.businessKey || row.businessKey,
        processInstanceId: businessContext.processInstanceId || row.processInstanceId,
        processDefKey: businessContext.processDefKey || formInfo.processDefKey || row.processDefKey || row.processDefinitionKey,
        taskDefKey: businessContext.taskDefKey || formInfo.taskDefKey || row.taskDefKey || row.taskDefinitionKey,
        objectCode: businessContext.objectCode || row.objectCode,
        recordId: businessContext.recordId || row.recordId,
        formKey: businessContext.formKey || formInfo.formKey,
        userId: userStore.userId,
        comment,
        variables: formInfo.variables || undefined,
        ...(await createFlowActionCredentials(action, taskId, { comment, variables: formInfo.variables || undefined })),
      }))
    : await (action === 'approve' ? flowApi.approveTask : flowApi.rejectTask)({
        taskId,
        userId: userStore.userId,
        comment,
        variables: formInfo.variables || undefined,
        ...(await createFlowActionCredentials(action, taskId, { comment, variables: formInfo.variables || undefined })),
      })
  if (res.code !== 200)
    throw new Error(res.message || '操作失败')
}

async function submitQuickAction() {
  const comment = quickActionForm.comment.trim()
  if (!comment) {
    window.$message.warning(quickActionType.value === 'approve' ? '请输入同意意见' : '请输入驳回原因')
    return
  }

  quickActionLoading.value = true
  const action = quickActionType.value
  const targets = [...quickActionTargets.value]
  const errors = []
  const failedTargets = []
  let successCount = 0

  try {
    for (const row of targets) {
      try {
        await executeQuickAction(action, row, comment)
        successCount += 1
      }
      catch (error) {
        const taskName = getTaskDisplayName(row, row.title || row.taskId || row.id || '未知任务')
        errors.push(`${taskName}：${error?.message || '操作失败'}`)
        failedTargets.push(row)
      }
    }

    if (successCount > 0) {
      window.$message.success(`${getActionSuccessText(action)} ${successCount} 条`)
      selectedTaskKeys.value = []
      await loadData()
    }

    if (errors.length > 0) {
      quickActionFailedTargets.value = failedTargets
      quickActionTargets.value = failedTargets
      const content = errors.slice(0, 6).join('\n')
      if (window.$dialog?.warning) {
        window.$dialog.warning({
          title: successCount > 0 ? '部分任务未处理' : '任务未处理',
          content,
          positiveText: '知道了',
        })
      }
      else {
        window.$message.warning(errors[0])
      }
    }
    else {
      quickActionFailedTargets.value = []
      quickActionVisible.value = false
    }
  }
  finally {
    quickActionLoading.value = false
  }
}

async function collectDynamicFormVariables(action) {
  if (!useDynamicForm.value || !dynamicFormRef.value)
    return undefined
  if (action === 'approve') {
    await dynamicFormRef.value.validate()
  }
  return dynamicFormRef.value.getData?.() || dynamicFormRef.value.getFormData?.() || { ...dynamicFormData.value }
}

function handleDelegate() {
  delegateTargetUser.value = null
  delegateForm.comment = ''
  delegateForm.signature = ''
  delegateSignatureKey.value += 1
  showDelegateModal.value = true
}

function handleUserSelected(user) {
  delegateTargetUser.value = user
}

async function submitDelegate() {
  if (!canRunAction('delegate'))
    return
  if (!delegateTargetUser.value) {
    window.$message.warning('请选择转办人')
    return
  }
  if (!validateApprovalInput(delegateForm.comment, delegateForm.signature, delegateSignatureRef.value))
    return
  delegateLoading.value = true
  try {
    const signature = await resolveSignature(delegateSignatureRef.value, delegateForm.signature)
    delegateForm.signature = signature
    const taskId = currentTask.value.taskId
    const res = await flowApi.delegateTask({
      taskId,
      userId: String(userStore.userId),
      targetUserId: String(delegateTargetUser.value.id),
      comment: delegateForm.comment,
      signature,
      ...(await createFlowActionCredentials('delegate', taskId, {
        targetUserId: String(delegateTargetUser.value.id),
        comment: delegateForm.comment,
        signature,
      })),
    })
    if (res.code === 200) {
      window.$message.success('转办成功')
      showDelegateModal.value = false
      showDrawer.value = false
      loadData()
    }
    else {
      window.$message.error(res.message || '转办失败')
    }
  }
  catch (error) {
    window.$message.error(error?.message || '转办失败')
  }
  finally {
    delegateLoading.value = false
  }
}

async function handleClaim(row) {
  const taskId = row?.taskId || row?.id
  if (!taskId || claimLoadingTaskId.value)
    return
  claimLoadingTaskId.value = String(taskId)
  try {
    const res = await flowApi.claimTask(taskId, userStore.userId)
    if (res.code === 200) {
      window.$message.success('签收成功')
      if (currentTask.value && (currentTask.value.taskId === taskId || currentTask.value.id === taskId)) {
        currentTask.value.status = 1
        currentTask.value.assignee = userStore.userId
      }
      loadData()
    }
    else {
      window.$message.error(res.message || '签收失败')
    }
  }
  catch {
    window.$message.error('签收失败')
  }
  finally {
    claimLoadingTaskId.value = ''
  }
}

function isClaimingTask(row) {
  const taskId = row?.taskId || row?.id
  return Boolean(taskId) && claimLoadingTaskId.value === String(taskId)
}

async function loadData() {
  loading.value = true
  loadError.value = false
  try {
    const res = await flowApi.getTodoTasks({
      pageNum: pagination.page,
      pageSize: pagination.pageSize,
      userId: userStore.userId,
      title: queryParams.title || undefined,
      category: queryParams.category || undefined,
      status: queryParams.status ?? undefined,
    })
    if (res.code === 200 && res.data) {
      dataSource.value = res.data.records || []
      pagination.itemCount = res.data.total || 0
      urgentCount.value = dataSource.value.filter(r => isUrgentFlowPriority(r.priority)).length
    }
    else {
      loadError.value = true
    }
  }
  catch {
    console.error('加载待办任务失败')
    loadError.value = true
  }
  finally {
    loading.value = false
  }
}

function getRouteTaskId() {
  const taskId = route.query.taskId
  if (Array.isArray(taskId))
    return taskId[0] ? String(taskId[0]) : ''
  return taskId ? String(taskId) : ''
}

async function openTaskFromRoute() {
  const taskId = getRouteTaskId()
  if (!taskId || routeTaskOpening.value)
    return

  if (showDrawer.value && currentTask.value?.taskId === taskId)
    return

  routeTaskOpening.value = true
  try {
    const existing = dataSource.value.find(row => row.taskId === taskId || row.id === taskId)
    if (existing) {
      await openDrawer(existing)
      return
    }

    const res = await flowApi.getTaskDetail(taskId)
    if (res.code === 200 && res.data) {
      await openDrawer(res.data)
    }
    else {
      window.$message.warning('待办任务不存在或已处理')
      clearRouteTaskId()
    }
  }
  catch {
    window.$message.warning('待办任务不存在或已处理')
    clearRouteTaskId()
  }
  finally {
    routeTaskOpening.value = false
  }
}

function clearRouteTaskId() {
  if (!getRouteTaskId())
    return
  const query = { ...route.query }
  delete query.taskId
  delete query.source
  delete query.t
  router.replace({ path: route.path, query })
}

async function loadCategories() {
  try {
    const res = await flowApi.getCategoryTreeSelect(false)
    if (res.code === 200 && res.data) {
      categoryTreeOptions.value = buildFlowCategoryTreeOptions(res.data)
    }
  }
  catch {
    console.error('加载分类失败')
  }
}

function handleSearch() {
  pagination.page = 1
  loadData()
}

function handleReset() {
  queryParams.title = ''
  queryParams.category = ''
  queryParams.status = null
  pagination.page = 1
  loadData()
}

onMounted(async () => {
  loadCategories()
  await loadData()
  await openTaskFromRoute()
})

watch(
  () => route.fullPath,
  async () => {
    if (route.name === 'ApplicationPortal' || route.path === '/flow/todo' || route.path === '/workspace/todo')
      await openTaskFromRoute()
  },
)
</script>

<style scoped src="./todo.css"></style>
