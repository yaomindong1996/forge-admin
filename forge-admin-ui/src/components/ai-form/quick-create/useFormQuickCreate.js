import { computed, ref } from 'vue'
import { getDictData } from '@/composables/useDict'
import { useUserStore } from '@/store'
import { flattenOptionNodes, isSameOptionValue } from '../aiFormItemUtils'
import { hasQuickCreatePermission } from './permissions'
import { mergeQuickCreateSelection } from './selectionBackfill'
import { resolveQuickCreateDefinition } from './registry'

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
    if (!definition?.permission)
      return false
    return hasQuickCreatePermission(userStore, definition.permission)
  })

  /** Tooltip / aria 文案：完整「新增角色」等，不含「+」前缀 */
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
    if (!hasQuickCreatePermission(userStore, quickCreateDefinition.value?.permission))
      return
    const definition = quickCreateDefinition.value
    const gate = definition.beforeOpen?.({
      userStore,
      parentFormData: formData.value || formData,
      config: definition,
      field: field.value || field,
    })
    if (gate && gate.ok === false) {
      window.$message?.warning(gate.message || '当前无法快捷新增')
      return
    }
    quickCreateVisible.value = true
  }

  async function handleQuickCreateSuccess({ record, submitted } = {}) {
    const definition = quickCreateDefinition.value
    const currentField = field.value || field
    if (!definition)
      return
    const valueField = definition.valueField || 'id'
    const labelField = definition.labelField || 'label'
    let createdId = record?.[valueField] ?? record?.id ?? submitted?.[valueField]
    const createdLabel = record?.[labelField] ?? record?.label ?? submitted?.[labelField]
    const dictType = definition.dictType
      || currentField?.quickCreate?.dictType
      || currentField?.dictType
      || currentField?.props?.dictType
    const isDictQuickCreate = definition.preset === 'dictData' && dictType
    const isDictSelectField = currentField?.type === 'dictSelect'

    if (isDictQuickCreate) {
      await getDictData(dictType, true)
      if (isDictSelectField && typeof loadDictOptions === 'function')
        await loadDictOptions(dictType)
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

    const applyValue = (nextValue) => {
      if (typeof handleUpdate === 'function')
        handleUpdate(nextValue)
      else if (typeof handleSelectUpdate === 'function')
        handleSelectUpdate(nextValue)
    }

    if (createdId !== undefined && createdId !== null && createdId !== '') {
      const currentRaw = typeof value === 'function' ? value() : value?.value ?? value
      const multiple = typeof fieldMultiple === 'function'
        ? fieldMultiple()
        : fieldMultiple?.value ?? fieldMultiple
      const nextValue = mergeQuickCreateSelection(currentRaw, createdId, multiple)
      applyValue(nextValue)
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
