/** manage.vue setup part 2. */
import { NButton, NSpace, NTag, NTooltip } from 'naive-ui'
import { computed, h, nextTick, onMounted, ref } from 'vue'
import { clearExternalApiLogs, debugExternalApi, getExternalApiById, getExternalApiLogSummary, updateExternalApiDocument } from '@/api/external/api'
import { getExternalSystemList } from '@/api/external/system'
import { AiCrudPage } from '@/components/ai-form'
import DictTag from '@/components/DictTag.vue'
import FileUpload from '@/components/file-upload/index.vue'
import { useDict } from '@/composables/useDict'
import { managedFetch } from '@/composables/useGlobalLoading'
import { useAuthStore } from '@/store'
import { generateUUID, getFileDownloadUrl } from '@/utils'
import { toBooleanDictOptions, toNumberDictOptions } from '@/utils/dict-options'
import ApiConfigSection from '../components/ApiConfigSection.vue'
import ExternalApiFormSlot from '../components/ExternalApiFormSlot.vue'
import ExternalConfigEditor from '../components/ExternalConfigEditor.vue'
import PathFieldPicker from '../components/PathFieldPicker.vue'
export function applyExternalManagePart2(props, emit, deps = {}) {
  const {
    __impl, mut, backToSystems, beforeLoadApiList, beforeLoadLogList, beforeRenderApiDetail, beforeRenderApiForm, beforeRenderSystemDetail,
    beforeRenderSystemForm, beforeRenderSystemList, beforeSubmitApi, beforeSubmitSystem, boolSwitch, buildLogParams, copyUsageCode, downloadDoc,
    formatApiForm, formatDebugParams, formatDuration, formatJson, formatPercent, getDefaultApiForm, getDefaultSystemForm, getDownloadFileName,
    getSelectedSystemId, handleApiDeleted, handleApiSaved, handleClearLogs, handleDocRemove, handleDocUploadSuccess, handleOpenDebug, handleOpenDoc,
    handleOpenLogs, handleOpenSystemApis, handleOpenSystemLogs, handleOpenUsage, handleRefreshLogs, handleRunDebug, handleSaveDoc, handleSystemDeleted,
    handleSystemListLoaded, handleSystemSaved, loadLogSummary, loadSystemOptions, mergeSystemOptions, parseSchema, pathOptionsFor, renderApiRequestConfig,
    renderApiResponseRule, renderBooleanTag, renderDocTag, renderDurationTag, renderMethodTag, renderStatusTag, renderTextWithTooltip, showAllApis,
    toBool, useQichachaApiPreset, useQichachaPreset, validateJson, message, authStore, systemCrudRef, apiCrudRef,
    logCrudRef, selectedSystem, systemOptions, systemBaseUrlMap, systemCount, apiViewMode, logDrawerVisible, logApi,
    logSummary, debugModalVisible, debugApi, debugParamsText, debugFieldSchema, debugFieldValues, debugResult, debugLoading,
    usageModalVisible, usageApi, usageCode, docModalVisible, docApi, docFiles, docSaving, EXTERNAL_AUTH_ADAPTER_DICT,
    apiFormSlotNames, oauth2GrantTypeOptions, tokenHeaderOptions, tokenPrefixOptions, isApiStandalone, logDrawerWidth, apiPageTitle, panelSubtitle,
    logDrawerTitle, systemApiConfig, apiApiConfig, logApiConfig, authTypeOptions, apiKeyPositionOptions, statusOptions, methodOptions,
    contentTypeOptions, executionModeOptions, booleanOptions, callStatusOptions, callTypeOptions, systemCountText, systemSearchSchema, apiSearchSchema,
    systemTableColumns, apiTableColumns, logSearchSchema, logTableColumns, logDetailSchema, systemEditSchema, apiEditSchema, PATH_TYPE_LABELS,
    SCHEMA_SAFE_NAME,
  } = deps
  function validateApiSchemaFields(schemaJson, label, requirePath) {
    if (typeof schemaJson !== 'string' || schemaJson.trim() === '')
      return true
    let rows
    try {
      rows = JSON.parse(schemaJson)
    }
    catch {
      return false
    }
    if (!Array.isArray(rows))
      return false
    for (const row of rows) {
      if (!SCHEMA_SAFE_NAME.test(String(row?.name || ''))) {
        message.error(`${label}存在非法字段名（需英文字母开头，仅字母/数字/下划线）：${row?.name || '空'}`)
        return false
      }
      if (requirePath && !String(row?.path || '').trim()) {
        message.error(`${label}字段 ${row.name} 缺少取值路径`)
        return false
      }
    }
    return true
  }

  function formatSystemForm(data) {
    return {
      ...data,
      trustedInternal: toBool(data?.trustedInternal, false),
      sslVerifyEnabled: toBool(data?.sslVerifyEnabled, true),
      retryEnabled: toBool(data?.retryEnabled, true),
      requestLoggingEnabled: toBool(data?.requestLoggingEnabled, true),
    }
  }

  function trimPayload(data) {
    const payload = { ...data }
    Object.keys(payload).forEach((key) => {
      if (typeof payload[key] === 'string') {
        payload[key] = payload[key].trim()
      }
    })
    return payload
  }

  function cleanSystemAuthFields(data) {
    const payload = { ...data }
    if (payload.authType !== 'basic') {
      payload.basicUsername = undefined
      payload.basicPassword = undefined
    }
    if (payload.authType !== 'token' && payload.authType !== 'current_token') {
      payload.tokenValue = undefined
      payload.tokenHeaderName = undefined
      payload.tokenPrefix = undefined
    }
    if (payload.authType === 'current_token') {
      payload.tokenValue = undefined
    }
    if (payload.authType !== 'oauth2') {
      payload.oauth2TokenUrl = undefined
      payload.oauth2ClientId = undefined
      payload.oauth2ClientSecret = undefined
      payload.oauth2GrantType = undefined
      payload.oauth2Scope = undefined
    }
    if (payload.authType !== 'api_key') {
      payload.apiKeyName = undefined
      payload.apiKeyValue = undefined
      payload.apiKeyPosition = undefined
    }
    if (payload.authType !== 'custom') {
      payload.customAuthAdapter = undefined
      payload.customAuthConfig = undefined
    }
    // 代理与关闭SSL校验已被后端安全出站客户端禁止，此处仅清理历史遗留字段
    payload.proxyEnabled = undefined
    payload.proxyHost = undefined
    payload.proxyPort = undefined
    payload.proxyUsername = undefined
    payload.proxyPassword = undefined
    return payload
  }

  onMounted(() => {
    loadSystemOptions()
  })
  __impl.validateApiSchemaFields = validateApiSchemaFields
  __impl.formatSystemForm = formatSystemForm
  __impl.trimPayload = trimPayload
  __impl.cleanSystemAuthFields = cleanSystemAuthFields

  return {
    ...deps,
    validateApiSchemaFields,
    formatSystemForm,
    trimPayload,
    cleanSystemAuthFields,
  }
}
