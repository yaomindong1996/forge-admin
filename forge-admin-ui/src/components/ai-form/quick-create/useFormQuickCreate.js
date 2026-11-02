import { computed, ref } from 'vue'
import { getDictData } from '@/composables/useDict'
import { useUserStore } from '@/store'
import { flattenOptionNodes, isSameOptionValue } from '../aiFormItemUtils'
import { hasQuickCreatePermission } from './permissions'
import { mergeQuickCreateSelection } from './selectionBackfill'
import { resolveQuickCreateDefinition } from './registry'

function normalizePermissionList(definition) {
  if (!definition)
    return null
  if (Array.isArray(definition.managePermissions) && definition.managePermissions.length)
    return definition.managePermissions
  if (Array.isArray(definition.permission))
    return definition.permission
  return definition.permission || null
}

export function useFormQuickCreate(options = {}) {
  const {
    field,
    formData,
    value,
    fieldMultiple,
    remoteOptions,
    remoteOptionSource,
    loadRemoteOptions,
    loadDictOptions,
    handleSelectUpdate,
    handleUpdate,
    disabledHandler,
  } = options

  const userStore = useUserStore()
  const quickCreateVisible = ref(false)
  const quickCreateDefinition = computed(() => resolveQuickCreateDefinition(field.value || field))

  const quickCreateAllowed = computed(() => {
    const definition = quickCreateDefinition.value
    const permission = normalizePermissionList(definition)
    if (!permission)
      return false
    return hasQuickCreatePermission(userStore, permission)
  })

  /** Tooltip / aria：字典管理入口用「管理字典项」 */
  const quickCreateTooltipLabel = computed(() => {
    const definition = quickCreateDefinition.value
    if (!definition)
      return ''
    return definition.linkLabel || '新增'
  })

  /** @deprecated 仅兼容旧引用；与 quickCreateTooltipLabel 相同 */
  const quickCreateLinkLabel = quickCreateTooltipLabel

  const quickCreateDisabled = computed(() => {
    if (typeof disabledHandler === 'function' && disabledHandler(field.value || field))
      return true
    return false
  })

  /** 无权限 / 无定义 / 字段不可编辑时均不渲染入口（不只是灰掉） */
  const showQuickCreateTrigger = computed(() =>
    quickCreateAllowed.value
    && Boolean(quickCreateDefinition.value)
    && !quickCreateDisabled.value,
  )

  function openQuickCreate() {
    if (!showQuickCreateTrigger.value)
      return
    const definition = quickCreateDefinition.value
    if (!hasQuickCreatePermission(userStore, normalizePermissionList(definition)))
      return
    const gate = definition.beforeOpen?.({
      userStore,
      parentFormData: formData.value || formData,
      config: definition,
      field: field.value || field,
    })
    if (gate && gate.ok === false) {
      window.$message?.warning(gate.message || '当前无法管理字典项')
      return
    }
    quickCreateVisible.value = true
  }

  function applyValue(nextValue) {
    if (typeof handleUpdate === 'function')
      handleUpdate(nextValue)
    else if (typeof handleSelectUpdate === 'function')
      handleSelectUpdate(nextValue)
  }

  function readCurrentValue() {
    return typeof value === 'function' ? value() : value?.value ?? value
  }

  function readMultiple() {
    return typeof fieldMultiple === 'function'
      ? fieldMultiple()
      : fieldMultiple?.value ?? fieldMultiple
  }

  async function refreshDictOptions(definition, currentField) {
    const dictType = definition.dictType
      || currentField?.quickCreate?.dictType
      || currentField?.dictType
      || currentField?.props?.dictType
    if (!(definition.preset === 'dictData' && dictType))
      return dictType
    await getDictData(dictType, true)
    if (currentField?.type === 'dictSelect' && typeof loadDictOptions === 'function')
      await loadDictOptions(dictType)
    return dictType
  }

  async function handleQuickCreateSuccess({ record, submitted, action, dictValue } = {}) {
    const definition = quickCreateDefinition.value
    const currentField = field.value || field
    if (!definition)
      return

    const resolvedAction = action
      || (definition.preset === 'dictData' && definition.manageMode !== false ? 'managed' : 'created')
    const valueField = definition.valueField || 'id'
    const labelField = definition.labelField || 'label'
    let createdId = record?.[valueField] ?? record?.id ?? submitted?.[valueField]
    const createdLabel = record?.[labelField] ?? record?.label ?? submitted?.[labelField]
    const isDictQuickCreate = definition.preset === 'dictData'
    const isDictSelectField = currentField?.type === 'dictSelect'

    if (isDictQuickCreate)
      await refreshDictOptions(definition, currentField)

    // 管理面板：改/删后只刷新选项；删掉当前选中则清空
    if (isDictQuickCreate && definition.manageMode !== false) {
      if (resolvedAction === 'deleted') {
        const deleted = dictValue ?? record?.[valueField] ?? submitted?.[valueField]
        const currentRaw = readCurrentValue()
        const multiple = readMultiple()
        if (multiple && Array.isArray(currentRaw)) {
          const next = currentRaw.filter(item => !isSameOptionValue(item, deleted))
          if (next.length !== currentRaw.length)
            applyValue(next)
        }
        else if (isSameOptionValue(currentRaw, deleted)) {
          applyValue(null)
          window.$message?.info?.('原选项已删除，请重新选择')
        }
      }
      else if (resolvedAction === 'created' && createdId !== undefined && createdId !== null && createdId !== '') {
        if (currentField?.type === 'radio' && typeof createdId !== 'number') {
          const optionSample = currentField?.props?.options?.[0]
          if (optionSample && typeof optionSample.value === 'number')
            createdId = Number(createdId)
        }
        applyValue(mergeQuickCreateSelection(readCurrentValue(), createdId, readMultiple()))
      }
      return
    }

    if (currentField?.type === 'radio' && createdId !== undefined && createdId !== null && createdId !== '') {
      const optionSample = currentField?.props?.options?.[0]
      if (optionSample && typeof optionSample.value === 'number')
        createdId = Number(createdId)
    }
    else if (createdId !== undefined && createdId !== null && createdLabel) {
      const optionsList = Array.isArray(remoteOptions?.value) ? remoteOptions.value : []
      const exists = flattenOptionNodes(optionsList).some(item =>
        isSameOptionValue(item?.value ?? item?.key, createdId),
      )
      if (!exists && remoteOptions) {
        remoteOptions.value = [
          { value: createdId, key: createdId, label: createdLabel, isLeaf: true },
          ...optionsList,
        ]
      }
    }

    const source = typeof remoteOptionSource === 'function'
      ? remoteOptionSource()
      : remoteOptionSource?.value
    if (!isDictQuickCreate && !isDictSelectField && source && typeof loadRemoteOptions === 'function')
      await loadRemoteOptions(source)

    if (createdId !== undefined && createdId !== null && createdId !== '') {
      applyValue(mergeQuickCreateSelection(readCurrentValue(), createdId, readMultiple()))
      quickCreateVisible.value = false
      window.$message?.success('新增成功')
      return
    }
    quickCreateVisible.value = false
    window.$message?.warning('已保存，请在下拉中手动选择新建项')
  }

  return {
    quickCreateVisible,
    quickCreateDefinition,
    quickCreateAllowed,
    quickCreateLinkLabel,
    quickCreateTooltipLabel,
    showQuickCreateTrigger,
    quickCreateDisabled,
    openQuickCreate,
    handleQuickCreateSuccess,
  }
}
