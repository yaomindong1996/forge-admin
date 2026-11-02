import { computed, watch } from 'vue'
import { applyBizFormSchemeToEditSchema } from '@/composables/bizForm/applyBizFormSchemeToEditSchema'
import { useBizFormScheme } from '@/composables/bizForm/useBizFormScheme'
import { useUserStore } from '@/store'

const SIDE_TABLE_TYPES = new Set([
  'USER', 'ROLE', 'ORG', 'POST', 'DICT_TYPE', 'DICT_DATA', 'NOTICE', 'CLIENT', 'TENANT',
  'CONFIG', 'STORAGE_CONFIG', 'API_CONFIG', 'MENU', 'JOB_CONFIG',
])

function hasBizFormEditPermission(userStore, objectType) {
  const perms = userStore?.permissions || []
  if (perms.includes('*') || perms.includes('*:*:*'))
    return true
  const type = String(objectType || '').toUpperCase()
  if (SIDE_TABLE_TYPES.has(type))
    return perms.includes('system:biz-form:edit')
  return perms.includes('crm:biz-form:edit')
}

function resolveBizId(payload) {
  const data = payload?.data || {}
  const responseData = payload?.response?.data
  return data.id
    ?? responseData?.id
    ?? responseData?.data?.id
    ?? null
}

/**
 * AiCrudPage 接入配置表单：合并 editSchema、标题入口、扩展值读写。
 */
export function applyAiCrudBizForm(props, emit, api, options = {}) {
  const userStore = useUserStore()
  const objectTypeRef = computed(() => String(props.bizFormObjectType || '').trim().toUpperCase())
  const enabled = computed(() => Boolean(objectTypeRef.value))

  const bizForm = useBizFormScheme(objectTypeRef, {
    canEdit: () => enabled.value && hasBizFormEditPermission(userStore, objectTypeRef.value),
  })

  const baseModalFormSchema = api.modalFormSchema
  const modalFormSchema = computed(() => {
    const base = typeof baseModalFormSchema === 'object' && 'value' in (baseModalFormSchema || {})
      ? baseModalFormSchema.value
      : (props.editSchema || [])
    if (!enabled.value)
      return base
    return applyBizFormSchemeToEditSchema(Array.isArray(base) ? base : [], bizForm.formScheme.value)
  })

  function ensureExtBag() {
    const form = api.formData?.value
    if (!form)
      return
    if (!form.extValues || typeof form.extValues !== 'object' || Array.isArray(form.extValues))
      form.extValues = {}
    bizForm.ensureExtValueKeys(form)
    if (!SIDE_TABLE_TYPES.has(objectTypeRef.value)) {
      form.extJson = JSON.stringify(form.extValues || {})
    }
  }

  watch(
    () => [api.modalVisible?.value, api.inlineWorkspaceVisible?.value, objectTypeRef.value],
    async ([modalOpen, inlineOpen]) => {
      if (!enabled.value)
        return
      if (!modalOpen && !inlineOpen)
        return
      await bizForm.reloadFormScheme(api.formData?.value)
      ensureExtBag()
      const id = api.formData?.value?.id
      if (id != null && SIDE_TABLE_TYPES.has(objectTypeRef.value))
        await bizForm.loadSideExtValues(api.formData.value, id)
    },
  )

  watch(
    () => api.formData?.value?.extValues,
    () => {
      if (!enabled.value || SIDE_TABLE_TYPES.has(objectTypeRef.value))
        return
      const form = api.formData?.value
      if (form)
        form.extJson = JSON.stringify(form.extValues || {})
    },
    { deep: true },
  )

  async function persistExtAfterSubmit(payload) {
    if (!enabled.value || !SIDE_TABLE_TYPES.has(objectTypeRef.value))
      return
    const bizId = resolveBizId(payload)
    if (bizId == null)
      return
    try {
      await bizForm.saveSideExtValues(api.formData?.value, bizId)
    }
    catch (error) {
      window.$message?.error?.(error?.message || '扩展字段保存失败')
    }
  }

  if (options.onSubmitSuccessRef)
    options.onSubmitSuccessRef.current = persistExtAfterSubmit

  async function reloadBizFormScheme() {
    await bizForm.reloadFormScheme(api.formData?.value)
    ensureExtBag()
  }

  return {
    ...api,
    modalFormSchema,
    bizFormEnabled: enabled,
    bizFormObjectType: objectTypeRef,
    bizFormConfigVisible: bizForm.configVisible,
    canConfigBizForm: bizForm.canConfigForm,
    reloadBizFormScheme,
    openBizFormConfig: () => {
      bizForm.configVisible.value = true
    },
  }
}

export { SIDE_TABLE_TYPES, hasBizFormEditPermission }
