import { computed, ref, unref } from 'vue'
import { bizFormExtGet, bizFormExtSave, bizFormScheme } from '@/api/crm/bizForm'
import { isExtValueEmpty } from '@/views/crm/components/bizFormFieldTypes'

/**
 * 通用业务表单方案：显隐/必填/标签/自定义字段 + 配置入口。
 * @param {string|import('vue').Ref<string>} objectType
 * @param {object} options
 * @param {() => boolean} [options.canEdit] 是否有配置权限
 * @param {Record<string, string[]>} [options.sectionCodes] section -> fieldCodes
 * @param {string[]} [options.defaultRequiredCodes] 方案未加载时的默认必填
 * @param {(form: any) => void} [options.ensureFormShape] 确保 form.extValues 存在
 */
export function useBizFormScheme(objectType, options = {}) {
  const formScheme = ref(null)
  const configVisible = ref(false)
  const loadingScheme = ref(false)

  const resolvedType = computed(() => String(unref(objectType) || '').trim().toUpperCase())

  const canConfigForm = computed(() => {
    if (typeof options.canEdit === 'function')
      return Boolean(options.canEdit())
    return false
  })

  const fieldMap = computed(() => {
    const map = Object.create(null)
    for (const item of formScheme.value?.fields || []) {
      if (item?.fieldCode)
        map[item.fieldCode] = item
    }
    return map
  })

  const customSchemeFields = computed(() =>
    (formScheme.value?.fields || [])
      .filter(item => String(item.fieldKind || '').toUpperCase() === 'CUSTOM')
      .slice()
      .sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0)))

  function fieldOn(code) {
    if (!formScheme.value)
      return true
    return Boolean(fieldMap.value[code])
  }

  function fieldLabel(code, fallback) {
    const item = fieldMap.value[code]
    return item?.displayLabel || item?.fieldLabel || fallback
  }

  function fieldRequired(code) {
    const item = fieldMap.value[code]
    if (item)
      return Number(item.required) === 1
    if (!formScheme.value) {
      const defaults = options.defaultRequiredCodes || []
      return defaults.includes(code)
    }
    return false
  }

  function sectionHasVisible(section) {
    const codes = options.sectionCodes?.[section] || []
    return codes.some(code => fieldOn(code))
  }

  function systemFieldsInSection(section) {
    const codes = options.sectionCodes?.[section] || []
    const visible = codes.filter(code => fieldOn(code))
    if (!formScheme.value)
      return visible
    return visible.slice().sort((a, b) => {
      const ao = fieldMap.value[a]?.sortOrder ?? 9999
      const bo = fieldMap.value[b]?.sortOrder ?? 9999
      return ao - bo
    })
  }

  function ensureExtValueKeys(form) {
    if (!form)
      return
    if (typeof options.ensureFormShape === 'function')
      options.ensureFormShape(form)
    if (!form.extValues || typeof form.extValues !== 'object' || Array.isArray(form.extValues))
      form.extValues = {}
    for (const item of customSchemeFields.value) {
      if (!(item.fieldCode in form.extValues))
        form.extValues[item.fieldCode] = null
    }
  }

  function buildCustomRequiredRules(form) {
    const rules = {}
    for (const item of customSchemeFields.value) {
      if (Number(item.required) !== 1)
        continue
      const label = item.displayLabel || item.fieldLabel || item.fieldCode
      rules[`extValues.${item.fieldCode}`] = {
        required: true,
        trigger: ['blur', 'change'],
        validator: () => {
          if (isExtValueEmpty(form?.extValues?.[item.fieldCode]))
            return new Error(`请填写${label}`)
          return true
        },
      }
    }
    return rules
  }

  async function reloadFormScheme(form) {
    if (!resolvedType.value)
      return
    loadingScheme.value = true
    try {
      const res = await bizFormScheme(resolvedType.value)
      formScheme.value = res.data || { fields: [] }
      if (form)
        ensureExtValueKeys(form)
    }
    catch (error) {
      formScheme.value = { fields: [] }
      window.$message?.error?.(error?.message || '表单方案加载失败，已按默认字段展示')
    }
    finally {
      loadingScheme.value = false
    }
  }

  async function loadSideExtValues(form, bizId) {
    if (!form || bizId == null || !resolvedType.value)
      return
    try {
      const res = await bizFormExtGet(resolvedType.value, bizId)
      const raw = res.data?.extJson
      let parsed = {}
      if (raw && typeof raw === 'string') {
        try {
          parsed = JSON.parse(raw) || {}
        }
        catch {
          parsed = {}
        }
      }
      else if (raw && typeof raw === 'object') {
        parsed = raw
      }
      form.extValues = { ...(form.extValues || {}), ...parsed }
      ensureExtValueKeys(form)
    }
    catch (error) {
      window.$message?.error?.(error?.message || '扩展字段加载失败')
    }
  }

  async function saveSideExtValues(form, bizId) {
    if (bizId == null || !resolvedType.value)
      return
    await bizFormExtSave({
      objectType: resolvedType.value,
      bizId,
      extJson: JSON.stringify(form?.extValues || {}),
    })
  }

  return {
    formScheme,
    configVisible,
    loadingScheme,
    canConfigForm,
    customSchemeFields,
    fieldOn,
    fieldLabel,
    fieldRequired,
    sectionHasVisible,
    systemFieldsInSection,
    ensureExtValueKeys,
    buildCustomRequiredRules,
    reloadFormScheme,
    loadSideExtValues,
    saveSideExtValues,
  }
}
