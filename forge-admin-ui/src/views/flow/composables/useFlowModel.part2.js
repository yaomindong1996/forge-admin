/** model.vue setup part 2. */
import { CopyOutline, CreateOutline, PauseCircleOutline, PlayCircleOutline, TimeOutline, TrashOutline } from '@vicons/ionicons5'
import { NIcon, NModal, NTreeSelect } from 'naive-ui'
import { computed, defineAsyncComponent, h, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { businessFlowFormAssets, businessFlowModelBindings } from '@/api/business-app'
import flowApi from '@/api/flow'
import AiForm from '@/components/ai-form/AiForm.vue'
import UserSelectPicker from '@/components/common/UserSelectPicker.vue'
import { useDict } from '@/composables/useDict'
import { collectInitiatorSelectSelections } from '@/utils/initiatorSelect'
import DesignerAsyncLoader from '@/views/app-center/components/designer/DesignerAsyncLoader.vue'
import { buildFlowCategoryTreeOptions, resolveFlowCategoryLabel, resolveFlowCategoryValue } from '../utils/categoryOptions'
export function applyFlowModelPart2(deps = {}) {
  const {
    __impl, mut, applyModelStatistics, applyStartTestBusinessFormLayout, buildModelFormBinding, cancelModelOrder, collectStartTestFormData, designerTypeClass,
    designerTypeLabel, enrichModelBusinessBindings, extractBusinessBindingRows, fetchCategories, fetchData, fetchModelPage, fetchModelStatistics, flattenStartTestBusinessComponents,
    formatBusinessBindings, formatDate, generateModelKey, getActionOptions, getCategoryDisplayName, handleActionSelect, handleAdd, handleCopy,
    handleDeploy, handleDesign, handleDesignModalClose, handleDragStart, handleDrop, handleEdit, handleFilter, handlePageSizeChange,
    handleReset, handleSearch, handleStartTest, handleStatusSelect, handleSubmit, handleSubmitStartTest, handleViewInstances, handleViewStarted,
    hasStartTestAssetSchema, isBusinessStartTestForm, isCodeAppBinding, isModelActionBusy, isModelActionLocked, loadStartTestBusinessForm, lockModelAction, mergeModelBusinessBindings,
    modelActionKey, normalizeBusinessFormReference, normalizeDesignerType, normalizeStartTestAssetSchema, normalizeStartTestBusinessAssets, normalizeStartTestBusinessFields, normalizeStartTestFieldType, parseBusinessFormReference,
    parseFormSchema, resetStartTestBusinessFormLayout, resolveApplicationIdFromFormKey, resolveStartTestBusinessFields, saveModelOrder, statusClass, textValue, toCount,
    toNumberOptions, unlockModelAction, router, DEFAULT_TODO_DETAIL_URL_TEMPLATE, FlowDesignAsyncLoader, FlowFormRendererAsyncLoader, FlowModalAsyncLoader, FlowDesignPage,
    FlowFormCreateRenderer, VersionHistory, statusOptions, categoryTreeOptions, designerTypePresentation, designerTypeOptions, modelActionLocks, queryParams,
    activeStatsStatus, dataSource, loading, sortMode, sortSaving, draggingModelId, pagination, showVersionHistory,
    currentModelId, currentModelVersion, showDesignModal, currentDesignModelId, currentDesignBinding, showStartTestModal, startTestLoading, startTestFormRef,
    startTestFormData, startTestFormSchema, startTestBusinessFormActive, startTestBusinessFormLoading, startTestBusinessFormAssets, startTestBusinessFormLayout, startTestApproverNodes, startTestApproverSelections,
    startTestApproverLabels, startTestPreflightDiagnostics, currentStartModel, startTestTitle, startTestAlert, totalCount, designingCount, deployedCount,
    suspendedCount, disabledCount, showModal, modalTitle, isEdit, submitLoading, formRef, formData,
    rules,
  } = deps
  async function handleSuspend(row) {
    const lockKey = lockModelAction(row, 'suspend')
    if (!lockKey)
      return
    window.$dialog?.warning({
      title: '确认挂起',
      content: `挂起后，「${row.modelName}」相关的进行中流程实例将暂停，确定继续？`,
      positiveText: '确定',
      negativeText: '取消',
      onPositiveClick: async () => {
        try {
          const res = await flowApi.suspendModel(row.id)
          if (res.code === 200) {
            window.$message?.success('已挂起')
            await fetchData()
          }
          else {
            window.$message?.error(res.message || '挂起失败')
          }
        }
        catch (error) {
          window.$message?.error(error?.message || error?.response?.data?.message || '挂起失败')
        }
        finally {
          unlockModelAction(lockKey)
        }
      },
      onNegativeClick: () => unlockModelAction(lockKey),
      onClose: () => unlockModelAction(lockKey),
    })
  }

  async function handleActivate(row) {
    const lockKey = lockModelAction(row, 'activate')
    if (!lockKey)
      return
    try {
      const res = await flowApi.activateModel(row.id)
      if (res.code === 200) {
        window.$message?.success('已激活')
        await fetchData()
      }
      else {
        window.$message?.error(res.message || '激活失败')
      }
    }
    catch (error) {
      window.$message?.error(error?.message || error?.response?.data?.message || '激活失败')
    }
    finally {
      unlockModelAction(lockKey)
    }
  }

  async function handleDelete(row) {
    const lockKey = lockModelAction(row, 'delete')
    if (!lockKey)
      return
    window.$dialog?.error({
      title: '确认删除',
      content: `删除「${row.modelName}」前会校验该模型下是否存在流程实例或历史数据；如存在数据，系统会拒绝删除。删除后不可恢复，确定继续？`,
      positiveText: '确定删除',
      negativeText: '取消',
      onPositiveClick: async () => {
        try {
          const res = await flowApi.deleteModel(row.id)
          if (res.code === 200) {
            window.$message?.success('删除成功')
            fetchData()
          }
          else {
            window.$message?.error(res.message || '删除失败')
          }
        }
        catch (error) {
          window.$message?.error(error?.message || error?.response?.data?.message || '删除失败')
        }
        finally {
          unlockModelAction(lockKey)
        }
      },
      onNegativeClick: () => unlockModelAction(lockKey),
      onClose: () => unlockModelAction(lockKey),
    })
  }

  function handleVersionHistory(row) {
    currentModelId.value = row.id
    currentModelVersion.value = row.version
    showVersionHistory.value = true
  }

  onMounted(() => {
    fetchCategories()
    fetchData()
  })
  __impl.handleSuspend = handleSuspend
  __impl.handleActivate = handleActivate
  __impl.handleDelete = handleDelete
  __impl.handleVersionHistory = handleVersionHistory

  return {
    ...deps,
  }
}
