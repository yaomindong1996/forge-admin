<!--
  字典选择器组件
  用于在表单中选择字典项

  使用示例：
  <DictSelect v-model:value="formData.status" dict-type="case_status" />
  <DictSelect v-model:value="formData.types" dict-type="matter_type" multiple />
  <DictSelect v-model:value="form.type" dict-type="sys_message_type" quick-create />
-->

<template>
  <div
    class="dict-select-wrap"
    :class="{ 'has-quick-create': showQuickCreate }"
  >
    <n-select
      class="dict-select-control"
      :value="selectValue"
      :options="dictOptions"
      :placeholder="placeholder"
      :disabled="disabled"
      :clearable="clearable"
      :filterable="filterable"
      :multiple="multiple"
      :loading="loading"
      v-bind="$attrs"
      @update:value="handleUpdate"
    />
    <FormQuickCreateTrigger
      v-if="showQuickCreate"
      :label="quickCreateTooltipLabel"
      @click="openQuickCreate"
    />
    <FormQuickCreateHost
      v-if="quickCreateDefinition"
      v-model:show="quickCreateVisible"
      :definition="quickCreateDefinition"
      :parent-form-data="formData"
      :field="quickCreateField"
      @success="handleQuickCreateSuccess"
    />
  </div>
</template>

<script setup>
import { computed, defineAsyncComponent, ref, useAttrs, watch } from 'vue'
import FormQuickCreateTrigger from '@/components/ai-form/quick-create/FormQuickCreateTrigger.vue'
import { useFormQuickCreate } from '@/components/ai-form/quick-create/useFormQuickCreate'
import { isPlatformDictQuickCreateBlocked } from '@/components/ai-form/quick-create/presets/dictData'
import { getDictData } from '@/composables/useDict'
import { fromDictSelectValue, stringifyDictValue, toDictSelectValue } from '@/utils/dict-options'

/** 异步 Host，避免 DictSelect → Host → AiForm → AiFormItem → DictSelect TDZ */
const FormQuickCreateHost = defineAsyncComponent(() =>
  import('@/components/ai-form/quick-create/FormQuickCreateHost.vue'),
)

defineOptions({ inheritAttrs: false })

const attrs = useAttrs()

const props = defineProps({
  // v-model 绑定值
  value: {
    type: [String, Number, Array],
    default: null,
  },

  // 字典类型
  dictType: {
    type: String,
    required: true,
  },

  // 占位符
  placeholder: {
    type: String,
    default: '请选择',
  },

  // 是否禁用
  disabled: {
    type: Boolean,
    default: false,
  },

  // 只读（与 disabled 一样隐藏快捷入口）
  readonly: {
    type: Boolean,
    default: false,
  },

  // 是否可清空
  clearable: {
    type: Boolean,
    default: true,
  },

  // 是否可搜索
  filterable: {
    type: Boolean,
    default: true,
  },

  // 是否多选
  multiple: {
    type: Boolean,
    default: false,
  },

  // 当前表单数据，用于字典级联过滤
  formData: {
    type: Object,
    default: () => ({}),
  },

  // { enabled, sourceField, mode: parentDictCode | linkedDict }
  cascade: {
    type: Object,
    default: null,
  },

  // 静态父级/关联过滤，便于非 AiForm 场景直接使用
  parentDictCode: {
    type: [String, Number],
    default: null,
  },
  linkedDictType: {
    type: String,
    default: '',
  },
  linkedDictValue: {
    type: [String, Number],
    default: null,
  },
  sourceField: {
    type: String,
    default: '',
  },
  sourceDictType: {
    type: String,
    default: '',
  },
  matchMode: {
    type: String,
    default: '',
  },

  /**
   * 字段旁快捷新增字典项。
   * true | { linkLabel?, allowPlatformDict? }；筛选栏等勿开。
   */
  quickCreate: {
    type: [Boolean, Object],
    default: false,
  },
})

const emit = defineEmits(['update:value'])

const dictList = ref([])
const loading = ref(false)

const quickCreateOverrides = computed(() => {
  const raw = props.quickCreate
  if (!raw)
    return null
  if (raw === true)
    return { preset: 'dictData' }
  if (typeof raw === 'object')
    return { preset: 'dictData', ...raw }
  return null
})

const quickCreateField = computed(() => ({
  type: 'dictSelect',
  dictType: props.dictType,
  quickCreate: quickCreateOverrides.value || undefined,
}))

function isDictSelectNonEditable() {
  return Boolean(
    props.disabled
    || props.readonly
    || attrs.disabled
    || attrs.readonly,
  )
}

const {
  quickCreateVisible,
  quickCreateDefinition,
  quickCreateTooltipLabel,
  showQuickCreateTrigger,
  openQuickCreate,
  handleQuickCreateSuccess: applyQuickCreateSuccess,
} = useFormQuickCreate({
  field: quickCreateField,
  formData: computed(() => props.formData),
  value: computed(() => props.value),
  fieldMultiple: computed(() => props.multiple),
  loadDictOptions: async () => {
    await loadDict(true)
  },
  handleUpdate: (nextValue) => {
    emit('update:value', nextValue)
  },
  disabledHandler: () => isDictSelectNonEditable(),
})

const showQuickCreate = computed(() => {
  if (!quickCreateOverrides.value || !props.dictType)
    return false
  if (isPlatformDictQuickCreateBlocked(props.dictType, quickCreateOverrides.value))
    return false
  // showQuickCreateTrigger 已含权限 + 不可编辑隐藏
  return showQuickCreateTrigger.value
})

async function handleQuickCreateSuccess(payload) {
  await applyQuickCreateSuccess(payload)
  await loadDict(true)
}

const effectiveCascade = computed(() => {
  const source = props.cascade || {}
  const mode = props.matchMode || source.mode || source.matchMode || (props.parentDictCode !== null ? 'parentDictCode' : 'linkedDict')
  return {
    ...source,
    enabled: source.enabled !== false && Boolean(source.sourceField || props.sourceField || props.parentDictCode !== null || props.linkedDictValue !== null),
    sourceField: source.sourceField || props.sourceField,
    sourceDictType: source.sourceDictType || props.sourceDictType,
    linkedDictType: source.linkedDictType || props.linkedDictType,
    linkedDictValue: source.linkedDictValue ?? props.linkedDictValue,
    parentDictCode: source.parentDictCode ?? props.parentDictCode,
    mode,
  }
})

// 字典选项。n-select 用 === 匹配，字典 dictValue 是字符串、业务字段常是数字，统一成字符串才能回显汉字。
const dictOptions = computed(() => {
  return filterDictList(dictList.value).map(item => ({
    label: item.label,
    value: stringifyDictValue(item.value),
    disabled: item.status === 0, // 状态为 0 时禁用
    raw: item.raw || item,
  }))
})

const selectValue = computed(() => toDictSelectValue(props.value, props.multiple))

// 加载字典数据
async function loadDict(force = false) {
  if (!props.dictType) {
    console.warn('DictSelect: 未指定 dictType')
    return
  }

  loading.value = true
  try {
    dictList.value = await getDictData(props.dictType, force)
  }
  finally {
    loading.value = false
  }
}

// 监听 dictType 变化
watch(() => props.dictType, () => {
  loadDict()
}, { immediate: true })

// 更新值
function handleUpdate(val) {
  emit('update:value', fromDictSelectValue(val, props.value, props.multiple))
}

function filterDictList(list = []) {
  const cascade = effectiveCascade.value
  if (!cascade.enabled || !cascade.sourceField)
    return filterByStaticCascade(list, cascade)
  const sourceValue = cascade.sourceField ? props.formData?.[cascade.sourceField] : undefined
  if (sourceValue === null || sourceValue === undefined || sourceValue === '')
    return []
  const mode = cascade.mode || 'linkedDict'
  return list.filter((item) => {
    const raw = item.raw || item
    if (mode === 'parentDictCode') {
      return isSameValue(raw.parentDictCode ?? item.parentDictCode, sourceValue)
        || isSameValue(raw.parentDictCode ?? item.parentDictCode, resolveSourceDictCode(sourceValue))
    }
    if (mode === 'linkedDict') {
      const linkedType = cascade.linkedDictType || cascade.sourceDictType
      const typeMatched = !linkedType || isSameValue(raw.linkedDictType ?? item.linkedDictType, linkedType)
      return typeMatched && isSameValue(raw.linkedDictValue ?? item.linkedDictValue, sourceValue)
    }
    return true
  })
}

function filterByStaticCascade(list = [], cascade = {}) {
  if (cascade.parentDictCode === null && cascade.parentDictCode === undefined && cascade.linkedDictValue === null && cascade.linkedDictValue === undefined)
    return list
  const mode = cascade.mode || 'linkedDict'
  return list.filter((item) => {
    const raw = item.raw || item
    if (mode === 'parentDictCode')
      return isSameValue(raw.parentDictCode ?? item.parentDictCode, cascade.parentDictCode)
    if (mode === 'linkedDict') {
      const typeMatched = !cascade.linkedDictType || isSameValue(raw.linkedDictType ?? item.linkedDictType, cascade.linkedDictType)
      return typeMatched && isSameValue(raw.linkedDictValue ?? item.linkedDictValue, cascade.linkedDictValue)
    }
    return true
  })
}

function resolveSourceDictCode(sourceValue) {
  const sourceOptions = props.cascade?.sourceOptions || []
  const matched = sourceOptions.find(item => isSameValue(item.value, sourceValue))
  return matched?.dictCode ?? matched?.raw?.dictCode ?? sourceValue
}

function isSameValue(left, right) {
  if (left === right)
    return true
  if (left === null || left === undefined || right === null || right === undefined)
    return false
  return String(left) === String(right)
}
</script>

<style scoped>
.dict-select-wrap {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  width: 100%;
  min-width: 0;
}

.dict-select-wrap.has-quick-create .dict-select-control {
  flex: 1 1 auto;
  min-width: 0;
}
</style>
