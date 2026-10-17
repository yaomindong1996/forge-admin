/** todo.vue setup part 2. */
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
import { useWorkspaceBadgeStore } from '@/stores/flow/workspaceBadgeStore'
import { normalizeFieldPermissions, pickFirstNonEmptyFieldPermissions, pickFirstNonEmptyPermissionSource } from '@/utils/field-permissions'
import { createFlowActionCredentials } from '@/utils/flow-action-idempotency'
import { applyChildTableFieldPermissions, childTableKeysAlias } from '@/utils/flow-field-permissions'
import { buildFlowCategoryTreeOptions, resolveFlowCategoryLabel } from '../utils/categoryOptions'
import { FLOW_PRIORITY_LABEL_FALLBACK, getFlowPriorityClass, isUrgentFlowPriority, resolveFlowPriorityLevel, shouldShowFlowPriority } from '../utils/priority'
import { getBusinessFormDisplayTitle, getProcessDisplayName, getRowDisplayTitle, getTaskDisplayName, getTaskHandlerName } from '../utils/processDisplay'
import { resolveBusinessTaskAiFormSchema } from '../utils/resolveBusinessTaskAiFormSchema'
import { loadTaskFormBundle } from '../utils/task-form-bundle'
export function applyFlowTodoPart2(deps = {}) {
  const {
    __impl, mut, applyTaskFormInfo, assertQuickActionAllowed, buildActionVariables, buildApprovalPointResults, buildBusinessTaskActionPayload, buildBusinessTaskFormQuery,
    buildBusinessTaskFormSavePayload, canRunAction, claimTaskBeforeQuickAction, compactParams, confirmRejectToTarget, createBusinessFormSnapshot, currentApprovalPoints, findBusinessChildRows,
    getActionSuccessText, getCategoryDisplayName, getPriorityClass, getPriorityText, handleExternalFormSubmit, hasBusinessTaskFormQuery, hasSignatureValue, hasWritableBusinessFormFields,
    hydrateBusinessFormFromAssets, isActionLoading, isCandidateTask, isChildBusinessFormField, isConfiguredBusinessTaskForm, isSyntheticTestBusinessKey, loadBusinessTaskFormContext, loadQuickBusinessTaskFormContext,
    loadTaskFormInfo, logBusinessApprovalChildren, normalizeBusinessChildrenData, normalizeBusinessRecordData, normalizeFallbackBusinessFields, openBusinessCodeForm, openDrawer, openQuickAction,
    openRejectTargetModal, rememberBusinessFormSnapshot, requiredApprovalPointsIncomplete, resetBusinessTaskForm, resolveActionApi, resolveBusinessChildKey, resolveQuickActionTargets, resolveSignature,
    resolveTaskFormRef, resolveTaskIdentityBusinessKey, saveBusinessTaskFormFields, submitApprove, submitTaskAction, summarizeBusinessChildren, toNumberOptions, validateApprovalInput,
    userStore, route, router, loading, loadError, dataSource, pagination, queryParams,
    categoryTreeOptions, urgentCount, selectedTaskKeys, showDrawer, currentTask, approvalHistory, taskFormInfo, approvalPointChecks,
    formInfoLoading, dynamicFormRef, dynamicFormData, dynamicFormSchema, businessFormContext, businessFormData, businessChildFormData, businessFormSavedSnapshot,
    businessFormRef, businessChildFormRef, businessFormLoading, taskFormLoading, businessFormSaving, useBusinessObjectForm, useBusinessCodeForm, useBusinessManagedForm,
    useDynamicForm, useExternalForm, businessFormTitle, businessFormWarnings, businessFormFieldPermissions, businessFormChildrenConfig, businessFormHasWritableFields, flowPrintHasUnsavedChanges,
    businessCodeFormUrl, businessFormGridCols, businessFormLabelPlacement, businessFormLabelWidth, businessFormSize, businessFormAiSchema, useBusinessCodeComponentForm, useComponentTaskForm,
    businessFormMissingText, componentTaskFormUrl, componentTaskFormInfo, dynamicFormFieldPermissions, businessFormRenderContext, taskPolicySource, canApprove, canReject,
    canRejectToStart, canDelegate, canReturn, returnTargetOptions, canChooseReturnTarget, canDirectSend, canTerminate, requireComment,
    requireSignature, approvalPolicy, approveLoading, approveForm, selectedReturnTarget, rejectTargetVisible, pendingRejectSubmit, directSendAfterReturn,
    approveSignatureRef, approveSignatureKey, claimLoadingTaskId, quickActionVisible, quickActionLoading, quickActionType, quickActionTargets, quickActionFailedTargets,
    quickActionForm, quickActionInputRef, quickActionIsApprove, quickActionTitle, quickActionTitleId, quickActionSubject, quickActionMeta, showDelegateModal,
    showUserSelectModal, delegateLoading, delegateTargetUser, delegateForm, delegateSignatureRef, delegateSignatureKey, routeTaskOpening, statusOptions,
    isApprovalBusy,
  } = deps
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
      // 列表刷新后同步侧栏待办徽标
      useWorkspaceBadgeStore().refresh()
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
  __impl.executeQuickAction = executeQuickAction
  __impl.submitQuickAction = submitQuickAction
  __impl.collectDynamicFormVariables = collectDynamicFormVariables
  __impl.handleDelegate = handleDelegate
  __impl.handleUserSelected = handleUserSelected
  __impl.submitDelegate = submitDelegate
  __impl.handleClaim = handleClaim
  __impl.isClaimingTask = isClaimingTask
  __impl.loadData = loadData
  __impl.getRouteTaskId = getRouteTaskId
  __impl.openTaskFromRoute = openTaskFromRoute
  __impl.clearRouteTaskId = clearRouteTaskId
  __impl.loadCategories = loadCategories
  __impl.handleSearch = handleSearch
  __impl.handleReset = handleReset

  return {
    ...deps,
  }
}
