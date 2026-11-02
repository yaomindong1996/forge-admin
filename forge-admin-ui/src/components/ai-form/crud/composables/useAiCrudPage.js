import { NButton, NDropdown, NIcon, NProgress, NSwitch, NTag } from 'naive-ui'
import { computed, h, nextTick, onBeforeUnmount, onMounted, ref, useSlots, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { crudConfigRender, customQueryExecute } from '@/api/ai'
import { businessDocumentRuntimeBatch, businessFlowStartConfig, executeBusinessAction, resubmitBusinessDocumentFlow, withdrawBusinessDocumentFlow } from '@/api/business-app'
import { businessProcessStartConfig, startBusinessProcess } from '@/api/business-process'
import { dataAuditRemove } from '@/api/data-audit'
import {
  applyDataAuditRemove,
  applyDataAuditSubmit,
  isAuditReasonRequiredError,
  isDataAuditHistoryAvailable,
  promptAuditReason,
  readDataAuditMeta,
  resolveDataAuditRemovePlan,
  shouldShowDataAuditHistory,
} from '@/components/data-audit/data-audit-submit'
import { useUserStore } from '@/store'
import { request } from '@/utils'
import { postEncrypt } from '@/utils/encrypt-request'
import { collectInitiatorSelectSelections } from '@/utils/initiatorSelect'
import {
  isSwitchColumnConfig,
  normalizeSwitchCellValue,
  resolveSwitchColumnTexts,
  resolveSwitchColumnValuePair,
} from '../column-switch'
import {
  buildBusinessActionExecutePayload,
  buildBusinessActionInitialData,
  buildBusinessActionInputFormSchema,
  buildChildRowActionContext,
  createBusinessActionIdempotencyKey,
  isRuntimeActionForPosition,
  matchesRuntimeDisplayCondition,
  resolveBusinessActionAttempt,
  shouldHideProcessStartAction,
  shouldShowDetailFlowHistory,
  unwrapBusinessActionResult,
} from '../../business-action-runtime'
import { flattenRuntimeFormFields, useCrudFormula } from './useCrudFormula'
import { useCrudImportExport } from './useCrudImportExport'
import { useCrudPageHeight } from './useCrudPageHeight'
import { useFlowActionFeedback } from './useFlowActionFeedback'
import { useRecordFormLoader } from './useRecordFormLoader'
import { resolveFormInitRecordId } from '../../data-source-binding-runtime'
import { normalizeExpandConfig, shouldExpandRow } from '../../expand-utils'
import { isNumberFieldType } from '../../field-type-utils'
import { resolveRuntimeDefaultValue } from '@/views/app-center/components/designer/forge-form-designer/field-default-value'
import { isImageFileName, resolveFileRenderItems } from '../../file-render-utils'
import { buildFormRuntimeContext } from '../../form-runtime-context'
import {
  createOfflineFormRuntime,
  createOfflinePublishedSnapshot,
} from '../../offline-form-runtime'
import {
  Add,
  ArrowBackOutline,
  CloseOutline,
  CloudUploadOutline,
  DownloadOutline,
  EllipsisVertical,
  PrintOutline,
  RefreshOutline,
  TimeOutline,
  TrashOutline,
} from '@vicons/ionicons5'
import AuthImage from '@/components/common/AuthImage.vue'
import SystemTableCell from '@/components/common/SystemTableCell.vue'
import DictTag from '@/components/DictTag.vue'
import AiCrudRowExpand from '../../AiCrudRowExpand.vue'
import AiForm from '../../AiForm.vue'
import { applyAiCrudPagePart1 } from './useAiCrudPage.part1.js'
import { applyAiCrudPagePart2 } from './useAiCrudPage.part2.js'
import { applyAiCrudPagePart3 } from './useAiCrudPage.part3.js'

export function useAiCrudPage(props, emit) {
  let api = applyAiCrudPagePart1(props, emit)
  api = applyAiCrudPagePart2(props, emit, api)
  api = applyAiCrudPagePart3(props, emit, api)
  // Strip cross-part plumbing so Vue setup() does not expose internal keys.
  const { __impl, mut, props: _props, emit: _emit, ...publicApi } = api
  return publicApi
}
