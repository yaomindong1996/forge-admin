<!--
  AI 表单项组件 - 根据配置动态渲染不同类型的表单字段
-->

<template>
  <!-- 分割线 -->
  <n-divider
    v-if="field.type === 'divider'"
    :title-placement="field.props?.titlePlacement || 'left'"
    v-bind="field.props"
  >
    {{ field.label }}
  </n-divider>

  <!-- 普通表单项 -->
  <n-form-item
    v-else
    :label="field.label"
    :path="field.field"
    :label-width="field.labelWidth"
    :show-label="field.showLabel !== false"
    :show-feedback="field.showFeedback !== false"
    :style="field.formItemStyle"
    :class="fieldAlignClass"
  >
    <!-- 输入框 -->
    <n-input
      v-if="field.type === 'input'"
      :value="value"
      :placeholder="getPlaceholder(field)"
      :disabled="disabledHandler(field)"
      :clearable="field.clearable !== false"
      :maxlength="field.maxlength"
      :show-count="field.showCount"
      :size="field.size"
      v-bind="field.props"
      @update:value="handleUpdate"
      v-on="getComponentEvents(field)"
    />

    <!-- 多行文本 -->
    <n-input
      v-else-if="field.type === 'textarea'"
      type="textarea"
      :value="value"
      :placeholder="getPlaceholder(field)"
      :disabled="disabledHandler(field)"
      :clearable="field.clearable !== false"
      :rows="field.rows || 3"
      :maxlength="field.maxlength"
      :show-count="field.showCount"
      :autosize="field.autosize"
      v-bind="field.props"
      @update:value="handleUpdate"
      v-on="getComponentEvents(field)"
    />

    <!-- 数字输入框 -->
    <n-input-number
      v-else-if="field.type === 'number' || field.type === 'inputNumber'"
      :value="value"
      :placeholder="getPlaceholder(field)"
      :disabled="disabledHandler(field)"
      :min="field.min"
      :max="field.max"
      :step="field.step || 1"
      :precision="field.precision"
      :show-button="field.showButton !== false"
      :clearable="field.clearable !== false"
      style="width: 100%"
      v-bind="field.props"
      @update:value="handleUpdate"
      v-on="getComponentEvents(field)"
    />

    <!-- 下拉选择 -->
    <n-select
      v-else-if="field.type === 'select'"
      :value="resolveOptionValue(value)"
      :placeholder="getPlaceholder(field)"
      :disabled="disabledHandler(field)"
      :options="currentOptions"
      :clearable="field.clearable !== false"
      :filterable="field.filterable !== false"
      :multiple="field.multiple"
      :loading="field.loading"
      :remote="field.remote"
      :on-search="field.onSearch"
      v-bind="field.props"
      @update:value="handleUpdate"
      v-on="getComponentEvents(field)"
    />

    <!-- 字典选择器 -->
    <DictSelect
      v-else-if="field.type === 'dictSelect'"
      :value="value"
      :dict-type="field.dictType || field.props?.dictType"
      :placeholder="getPlaceholder(field)"
      :disabled="disabledHandler(field)"
      :clearable="field.clearable !== false"
      :filterable="field.filterable !== false"
      :multiple="field.multiple"
      :form-data="formData"
      :cascade="dictCascadeConfig"
      v-bind="field.props"
      @update:value="handleUpdate"
    />

    <!-- 单选框 -->
    <n-radio-group
      v-else-if="field.type === 'radio'"
      :value="value"
      :disabled="disabledHandler(field)"
      v-bind="field.props"
      @update:value="handleUpdate"
      v-on="getComponentEvents(field)"
    >
      <n-space>
        <n-radio
          v-for="option in currentOptions"
          :key="option.value"
          :value="option.value"
          :disabled="option.disabled"
        >
          {{ option.label }}
        </n-radio>
      </n-space>
    </n-radio-group>

    <!-- 单选按钮组 -->
    <n-radio-group
      v-else-if="field.type === 'radioButton'"
      :value="value"
      :disabled="disabledHandler(field)"
      v-bind="field.props"
      @update:value="handleUpdate"
      v-on="getComponentEvents(field)"
    >
      <n-radio-button
        v-for="option in currentOptions"
        :key="option.value"
        :value="option.value"
        :disabled="option.disabled"
      >
        {{ option.label }}
      </n-radio-button>
    </n-radio-group>

    <!-- 多选框 -->
    <n-checkbox-group
      v-else-if="field.type === 'checkbox'"
      :value="value"
      :disabled="disabledHandler(field)"
      v-bind="field.props"
      @update:value="handleUpdate"
      v-on="getComponentEvents(field)"
    >
      <n-space>
        <n-checkbox
          v-for="option in currentOptions"
          :key="option.value"
          :value="option.value"
          :disabled="option.disabled"
        >
          {{ option.label }}
        </n-checkbox>
      </n-space>
    </n-checkbox-group>

    <!-- 开关 -->
    <n-switch
      v-else-if="field.type === 'switch'"
      :value="value"
      :disabled="disabledHandler(field)"
      :checked-value="field.checkedValue ?? true"
      :unchecked-value="field.uncheckedValue ?? false"
      v-bind="field.props"
      @update:value="handleUpdate"
      v-on="getComponentEvents(field)"
    >
      <template v-if="field.checkedText" #checked>
        {{ field.checkedText }}
      </template>
      <template v-if="field.uncheckedText" #unchecked>
        {{ field.uncheckedText }}
      </template>
    </n-switch>

    <!-- 日期选择 -->
    <n-date-picker
      v-else-if="field.type === 'date'"
      :value="value"
      type="date"
      :placeholder="getPlaceholder(field)"
      :disabled="disabledHandler(field)"
      :clearable="field.clearable !== false"
      style="width: 100%"
      v-bind="field.props"
      :default-value="resolvePickerDefaultValue(field)"
      :format="field.props?.format || field.format || 'yyyy-MM-dd'"
      :value-format="field.props?.valueFormat || field.valueFormat || 'yyyy-MM-dd'"
      @update:value="handleUpdate"
      v-on="getComponentEvents(field)"
    />

    <!-- 日期时间选择 -->
    <n-date-picker
      v-else-if="field.type === 'datetime'"
      :value="value"
      type="datetime"
      :placeholder="getPlaceholder(field)"
      :disabled="disabledHandler(field)"
      :clearable="field.clearable !== false"
      style="width: 100%"
      v-bind="field.props"
      :default-value="resolvePickerDefaultValue(field)"
      :format="field.props?.format || field.format || 'yyyy-MM-dd HH:mm:ss'"
      :value-format="field.props?.valueFormat || field.valueFormat || 'yyyy-MM-dd HH:mm:ss'"
      @update:value="handleUpdate"
      v-on="getComponentEvents(field)"
    />

    <!-- 日期范围选择 -->
    <n-date-picker
      v-else-if="field.type === 'daterange'"
      :value="normalizeRangePickerValue(value)"
      type="daterange"
      :placeholder="field.placeholder"
      :start-placeholder="field.startPlaceholder || '开始日期'"
      :end-placeholder="field.endPlaceholder || '结束日期'"
      :disabled="disabledHandler(field)"
      :clearable="field.clearable !== false"
      style="width: 100%"
      v-bind="field.props"
      :default-value="resolvePickerDefaultValue(field, true)"
      :format="field.props?.format || field.format || 'yyyy-MM-dd'"
      :value-format="field.props?.valueFormat || field.valueFormat || 'yyyy-MM-dd'"
      @update:value="handleUpdate"
      v-on="getComponentEvents(field)"
    />

    <!-- 日期时间范围选择 -->
    <n-date-picker
      v-else-if="field.type === 'datetimerange'"
      :value="normalizeRangePickerValue(value)"
      type="datetimerange"
      :placeholder="field.placeholder"
      :start-placeholder="field.startPlaceholder || '开始时间'"
      :end-placeholder="field.endPlaceholder || '结束时间'"
      :disabled="disabledHandler(field)"
      :clearable="field.clearable !== false"
      style="width: 100%"
      v-bind="field.props"
      :default-value="resolvePickerDefaultValue(field, true)"
      :format="field.props?.format || field.format || 'yyyy-MM-dd HH:mm:ss'"
      :value-format="field.props?.valueFormat || field.valueFormat || 'yyyy-MM-dd HH:mm:ss'"
      @update:value="handleUpdate"
      v-on="getComponentEvents(field)"
    />

    <!-- 月份选择 -->
    <n-date-picker
      v-else-if="field.type === 'month'"
      :value="value"
      type="month"
      :placeholder="getPlaceholder(field)"
      :disabled="disabledHandler(field)"
      :clearable="field.clearable !== false"
      style="width: 100%"
      v-bind="field.props"
      :default-value="resolvePickerDefaultValue(field)"
      :format="field.props?.format || field.format || 'yyyy-MM'"
      :value-format="field.props?.valueFormat || field.valueFormat || 'yyyy-MM'"
      @update:value="handleUpdate"
      v-on="getComponentEvents(field)"
    />

    <!-- 年份选择 -->
    <n-date-picker
      v-else-if="field.type === 'year'"
      :value="value"
      type="year"
      :placeholder="getPlaceholder(field)"
      :disabled="disabledHandler(field)"
      :clearable="field.clearable !== false"
      style="width: 100%"
      v-bind="field.props"
      :default-value="resolvePickerDefaultValue(field)"
      :format="field.props?.format || field.format || 'yyyy'"
      :value-format="field.props?.valueFormat || field.valueFormat || 'yyyy'"
      @update:value="handleUpdate"
      v-on="getComponentEvents(field)"
    />

    <!-- 时间选择 -->
    <n-time-picker
      v-else-if="field.type === 'time'"
      :value="value"
      :placeholder="getPlaceholder(field)"
      :disabled="disabledHandler(field)"
      :clearable="field.clearable !== false"
      style="width: 100%"
      v-bind="field.props"
      :default-value="resolvePickerDefaultValue(field)"
      :format="field.props?.format || field.format || 'HH:mm:ss'"
      :value-format="field.props?.valueFormat || field.valueFormat || 'HH:mm:ss'"
      @update:value="handleUpdate"
      v-on="getComponentEvents(field)"
    />

    <!-- 时间范围选择 -->
    <div v-else-if="field.type === 'timerange'" class="time-range-picker">
      <n-time-picker
        :value="resolveRangeValue(value, 0)"
        :placeholder="field.startPlaceholder || '开始时间'"
        :disabled="disabledHandler(field)"
        :clearable="field.clearable !== false"
        style="width: 100%"
        v-bind="field.props"
        :default-value="resolvePickerDefaultValue(field)"
        :format="field.props?.format || field.format || 'HH:mm:ss'"
        :value-format="field.props?.valueFormat || field.valueFormat || 'HH:mm:ss'"
        @update:value="handleRangeUpdate(0, $event)"
        v-on="getComponentEvents(field)"
      />
      <span class="time-range-separator">至</span>
      <n-time-picker
        :value="resolveRangeValue(value, 1)"
        :placeholder="field.endPlaceholder || '结束时间'"
        :disabled="disabledHandler(field)"
        :clearable="field.clearable !== false"
        style="width: 100%"
        v-bind="field.props"
        :default-value="resolvePickerDefaultValue(field)"
        :format="field.props?.format || field.format || 'HH:mm:ss'"
        :value-format="field.props?.valueFormat || field.valueFormat || 'HH:mm:ss'"
        @update:value="handleRangeUpdate(1, $event)"
        v-on="getComponentEvents(field)"
      />
    </div>

    <!-- 文件上传 -->
    <n-upload
      v-else-if="field.type === 'upload'"
      :action="field.action"
      :headers="field.headers"
      :data="field.data"
      :max="field.max"
      :accept="field.accept"
      :multiple="field.multiple"
      :disabled="disabledHandler(field)"
      :list-type="field.listType || 'text'"
      :show-file-list="field.showFileList !== false"
      :on-change="handleUploadChange"
      v-bind="field.props"
      v-on="getComponentEvents(field)"
    >
      <n-button>{{ field.uploadText || '点击上传' }}</n-button>
    </n-upload>

    <!-- 文件上传组件 -->
    <FileUpload
      v-else-if="field.type === 'fileUpload'"
      :model-value="value"
      :action="field.action"
      :business-type="field.businessType"
      :business-id="field.businessId"
      :storage-type="field.storageType"
      :limit="field.limit"
      :file-size="field.fileSize"
      :file-type="field.fileType"
      :multiple="field.multiple"
      :show-file-list="field.showFileList"
      :show-tip="field.showTip"
      :upload-button-text="field.uploadButtonText"
      :disabled="disabledHandler(field)"
      :value-type="field.valueType"
      v-bind="field.props"
      @update:model-value="handleUpdate"
      @success="(data) => handleUploadSuccess(field, data)"
      @error="(error) => handleUploadError(field, error)"
      @remove="(file) => handleUploadRemove(field, file)"
    />

    <!-- 图片上传组件 -->
    <ImageUpload
      v-else-if="field.type === 'imageUpload'"
      :model-value="value"
      :action="field.action"
      :business-type="field.businessType"
      :business-id="field.businessId"
      :storage-type="field.storageType"
      :limit="field.limit"
      :file-size="field.fileSize"
      :file-type="field.fileType"
      :multiple="field.multiple"
      :show-tip="field.showTip"
      :disabled="disabledHandler(field)"
      :value-type="field.valueType"
      v-bind="field.props"
      @update:model-value="handleUpdate"
      @success="(data) => handleUploadSuccess(field, data)"
      @error="(error) => handleUploadError(field, error)"
      @remove="(file) => handleUploadRemove(field, file)"
    />

    <!-- 滑块 -->
    <n-slider
      v-else-if="field.type === 'slider'"
      :value="value"
      :disabled="disabledHandler(field)"
      :min="field.min || 0"
      :max="field.max || 100"
      :step="field.step || 1"
      :marks="field.marks"
      :tooltip="field.tooltip !== false"
      v-bind="field.props"
      @update:value="handleUpdate"
      v-on="getComponentEvents(field)"
    />

    <!-- 评分 -->
    <n-rate
      v-else-if="field.type === 'rate'"
      :value="value"
      :disabled="disabledHandler(field)"
      :count="field.count || 5"
      :allow-half="field.allowHalf"
      v-bind="field.props"
      @update:value="handleUpdate"
      v-on="getComponentEvents(field)"
    />

    <!-- 颜色选择器 -->
    <n-color-picker
      v-else-if="field.type === 'color'"
      :value="value"
      :disabled="disabledHandler(field)"
      :show-alpha="field.showAlpha"
      :modes="field.modes || ['hex']"
      v-bind="field.props"
      @update:value="handleUpdate"
      v-on="getComponentEvents(field)"
    />

    <!-- 级联选择 -->
    <n-cascader
      v-else-if="field.type === 'cascader'"
      :value="resolveOptionValue(value)"
      :placeholder="getPlaceholder(field)"
      :disabled="disabledHandler(field)"
      :options="currentOptions"
      :clearable="field.clearable !== false"
      :filterable="field.filterable"
      :multiple="field.multiple"
      :cascade="field.cascade !== false"
      :show-path="field.showPath !== false"
      v-bind="field.props"
      @update:value="handleUpdate"
      v-on="getComponentEvents(field)"
    />

    <!-- 系统组织树选择 -->
    <n-tree-select
      v-else-if="field.type === 'orgTreeSelect'"
      :value="resolveOptionValue(value)"
      :placeholder="getPlaceholder(field)"
      :disabled="disabledHandler(field)"
      :options="currentOptions"
      :loading="remoteLoading"
      :clearable="field.clearable !== false"
      :filterable="field.filterable !== false"
      :multiple="field.multiple"
      :cascade="field.cascade !== false"
      v-bind="field.props"
      @update:value="handleTreeSelectUpdate(field, $event)"
      v-on="getComponentEvents(field)"
    />

    <!-- 系统用户选择 -->
    <UserSelectPicker
      v-else-if="field.type === 'userSelect'"
      :model-value="value"
      :label-value="resolveUserSelectLabel(field)"
      :placeholder="getPlaceholder(field)"
      :disabled="disabledHandler(field)"
      :clearable="field.clearable !== false"
      :multiple="field.multiple"
      :size="field.size"
      v-bind="field.props"
      @update:model-value="handleUpdate"
      @update:label-value="handleUserSelectLabelUpdate(field, $event)"
      @select="handleUserSelect(field, $event)"
    />

    <!-- 行政区划树选择 -->
    <RegionTreeSelect
      v-else-if="field.type === 'regionTreeSelect'"
      :model-value="value"
      :placeholder="getPlaceholder(field)"
      :disabled="disabledHandler(field)"
      :clearable="field.clearable !== false"
      :filterable="field.filterable !== false"
      :virtual-disabled="field.props?.virtualDisabled ?? !context?.isSearch"
      v-bind="field.props"
      @update:model-value="handleRegionTreeSelectUpdate(field, $event)"
    />

    <!-- 树形选择 -->
    <n-tree-select
      v-else-if="field.type === 'treeSelect'"
      :value="resolveOptionValue(value)"
      :placeholder="getPlaceholder(field)"
      :disabled="disabledHandler(field)"
      :options="currentOptions"
      :loading="remoteLoading"
      :clearable="field.clearable !== false"
      :filterable="field.filterable"
      :multiple="field.multiple"
      :cascade="field.cascade !== false"
      :show-path="field.showPath !== false"
      v-bind="field.props"
      @update:value="handleTreeSelectUpdate(field, $event)"
      v-on="getComponentEvents(field)"
    />

    <!-- 穿梭框 -->
    <n-transfer
      v-else-if="field.type === 'transfer'"
      :value="value"
      :disabled="disabledHandler(field)"
      :options="currentOptions"
      :filterable="field.filterable"
      v-bind="field.props"
      @update:value="handleUpdate"
      v-on="getComponentEvents(field)"
    />

    <!-- 远程搜索下拉框 -->
    <AiCustomSelect
      v-else-if="field.type === 'customSelect'"
      :value="value"
      :placeholder="getPlaceholder(field)"
      :disabled="disabledHandler(field)"
      :clearable="field.clearable !== false"
      :api="field.api"
      :method="field.method"
      :label-field="field.labelField || field.props?.labelName || 'label'"
      :value-field="field.valueField || field.props?.valueName || 'value'"
      :filterable="field.filterable !== false"
      :multiple="field.multiple"
      :remote="field.remote"
      :options="field.options"
      :params="field.params"
      :transform="field.transform"
      v-bind="field.props"
      @update:value="handleUpdate"
      v-on="getComponentEvents(field)"
    />

    <!-- 业务对象引用选择 -->
    <n-select
      v-else-if="field.type === 'objectReference'"
      :value="resolveOptionValue(value)"
      :placeholder="getPlaceholder(field)"
      :disabled="disabledHandler(field)"
      :options="currentOptions"
      :loading="remoteLoading"
      :clearable="field.clearable !== false"
      :filterable="field.filterable !== false"
      :multiple="field.multiple"
      v-bind="field.props"
      @update:value="handleUpdate"
      v-on="getComponentEvents(field)"
    />

    <!-- 纯文本展示 -->
    <div
      v-else-if="field.type === 'text'"
      :style="field.style"
    >
      <span v-if="field.formatter">
        {{ field.formatter(value, field, formData) }}
      </span>
      <span v-else>
        {{ value }}
      </span>
      <n-button
        v-if="field.copy"
        text
        size="small"
        style="margin-left: 8px"
        @click="handleCopy(value)"
      >
        <template #icon>
          <n-icon><CopyOutline /></n-icon>
        </template>
      </n-button>
    </div>

    <!-- 自定义插槽 -->
    <slot
      v-else-if="field.type === 'slot'"
      :name="field.slotName || field.field"
      :value="value"
      :field="field"
      :form-data="formData"
      :update-value="handleUpdate"
    />

    <!-- 默认为输入框 -->
    <n-input
      v-else
      :value="value"
      :placeholder="getPlaceholder(field)"
      :disabled="disabledHandler(field)"
      :clearable="field.clearable !== false"
      v-bind="field.props"
      @update:value="handleUpdate"
      v-on="getComponentEvents(field)"
    />
  </n-form-item>
</template>

<script setup>
import { CopyOutline } from '@vicons/ionicons5'
import { useClipboard } from '@vueuse/core'
import { computed, ref, watch } from 'vue'
import UserSelectPicker from '@/components/common/UserSelectPicker.vue'
import DictSelect from '@/components/DictSelect.vue'
import FileUpload from '@/components/file-upload/index.vue'
import ImageUpload from '@/components/image-upload/index.vue'
import RegionTreeSelect from '@/components/RegionTreeSelect.vue'
import { getDictData } from '@/composables/useDict'
import { request } from '@/utils'
import AiCustomSelect from './AiCustomSelect.vue'

const props = defineProps({
  field: {
    type: Object,
    required: true,
  },
  value: {
    type: [String, Number, Boolean, Array, Object],
    default: null,
  },
  formData: {
    type: Object,
    default: () => ({}),
  },
  context: {
    type: Object,
    default: () => ({}),
  },
})

const emit = defineEmits(['update:value'])

const { copy } = useClipboard()
const remoteOptions = ref([])
const remoteLoading = ref(false)
const dictOptions = ref([])
const sourceDictOptions = ref([])
const pickerDefaultTimestamp = Date.now()
let remoteRequestSeq = 0

/**
 * 获取占位符文本
 */
function getPlaceholder(field) {
  if (field.placeholder) {
    return field.placeholder
  }

  const inputTypes = ['input', 'textarea', 'number', 'inputNumber']
  const prefix = inputTypes.includes(field.type) ? '请输入' : '请选择'
  return `${prefix}${field.label}`
}

/**
 * 处理禁用状态
 */
function disabledHandler(field) {
  if (isCascadeDisabledByEmptyParent())
    return true
  if (typeof field.disabled === 'boolean') {
    return field.disabled
  }
  if (typeof field.disabled === 'function') {
    return field.disabled({
      formData: props.formData,
      field,
      context: props.context,
    })
  }
  return false
}

const fieldAlign = computed(() => normalizeAlign(props.field?.align || props.field?.textAlign || props.field?.props?.align))
const fieldAlignClass = computed(() => fieldAlign.value === 'left' ? '' : `ai-form-item-align-${fieldAlign.value}`)
const fieldDictType = computed(() => props.field?.dictType || props.field?.props?.dictType || '')
const cascadeConfig = computed(() => resolveCascadeConfig(props.field))
const dictCascadeConfig = computed(() => {
  if (!cascadeConfig.value)
    return null
  return {
    ...cascadeConfig.value,
    sourceOptions: sourceDictOptions.value,
  }
})
const remoteOptionSource = computed(() => resolveDynamicOptionSource(props.field))
const cascadeSourceValue = computed(() => {
  const cascade = cascadeConfig.value
  return cascade?.enabled && cascade.sourceField ? props.formData?.[cascade.sourceField] : undefined
})
const sourceFieldConfig = computed(() => findSchemaField(cascadeConfig.value?.sourceField))
const sourceDictType = computed(() => cascadeConfig.value?.sourceDictType || sourceFieldConfig.value?.dictType || sourceFieldConfig.value?.props?.dictType || '')

function isCascadeDisabledByEmptyParent() {
  const cascade = cascadeConfig.value
  if (!cascade?.enabled || cascade.emptyStrategy !== 'disabled' || !cascade.sourceField)
    return false
  const sourceValue = props.formData?.[cascade.sourceField]
  return sourceValue === null || sourceValue === undefined || sourceValue === ''
}

watch(
  remoteOptionSource,
  (source) => {
    if (!source) {
      remoteOptions.value = []
      return
    }
    loadRemoteOptions(source)
  },
  { immediate: true, deep: true },
)

watch(fieldDictType, loadDictOptions, { immediate: true })
watch(sourceDictType, loadSourceDictOptions, { immediate: true })
watch(cascadeSourceValue, (value, oldValue) => {
  if (oldValue === undefined || value === oldValue || !cascadeConfig.value?.clearOnParentChange)
    return
  clearCurrentValue()
})

/**
 * 获取选项数据 - 使用 computed 确保响应式
 */
const currentOptions = computed(() => {
  const field = props.field

  // 优先使用 options 函数
  if (typeof field.options === 'function') {
    const result = field.options({
      formData: props.formData,
      field,
      context: props.context,
    })

    // 如果返回的是 Promise，需要在外部处理
    // 这里我们检查是否有缓存的选项
    if (result instanceof Promise) {
      // 如果有缓存的选项，使用缓存
      if (field._cachedOptions && Array.isArray(field._cachedOptions)) {
        return field._cachedOptions
      }
      // 否则返回空数组，并异步加载
      cacheAsyncOptions(field, result)
      return []
    }

    return resolveCascadedOptions(result)
  }

  // 其次使用 options 数组
  if (field.options && Array.isArray(field.options) && field.options.length > 0) {
    return resolveCascadedOptions(field.options)
  }

  // 检查 props.options（兼容旧的配置方式）
  if (field.props?.options && Array.isArray(field.props.options) && field.props.options.length > 0) {
    return resolveCascadedOptions(field.props.options)
  }

  if (fieldDictType.value) {
    const options = resolveCascadedOptions(dictOptions.value)
    if (field.type === 'cascader')
      return buildDictTreeOptions(options)
    return withCurrentValueOption(options)
  }

  if (remoteOptionSource.value) {
    return withCurrentValueOption(resolveCascadedOptions(remoteOptions.value))
  }

  // 最后处理 enumType (仅当 options 为空时)
  if (field.enumType) {
    // 这里应该根据 enumType 获取对应的枚举数据
    // 由于这是一个示例,我们返回一个空数组
    // 在实际项目中,这里应该从父组件传递的 context 中获取数据
    // 或者通过 props 传递具体的选项数据
    console.warn(`字段 ${field.field} 使用了 enumType: ${field.enumType},但未提供具体选项数据`)
    return []
  }

  return []
})

function withCurrentValueOption(options = []) {
  const result = Array.isArray(options) ? [...options] : []
  const field = props.field || {}
  const labelValueField = field.labelValueField || field.props?.labelValueField
  if (!labelValueField || props.value === null || props.value === undefined || props.value === '')
    return result
  const labelValue = props.formData?.[labelValueField]
    ?? field.labelValue
    ?? field.props?.labelValue
  if (labelValue === null || labelValue === undefined || labelValue === '')
    return result
  const values = Array.isArray(props.value)
    ? props.value
    : field.multiple && typeof props.value === 'string'
      ? props.value.split(',').map(item => item.trim()).filter(Boolean)
      : [props.value]
  const labels = Array.isArray(labelValue)
    ? labelValue
    : String(labelValue).split(',').map(item => item.trim()).filter(Boolean)
  values.forEach((value, index) => {
    if (flattenOptionNodes(result).some(option => isSameOptionValue(option?.value ?? option?.key, value)))
      return
    result.unshift({
      value,
      key: value,
      label: labels[index] || labels[0] || String(value),
    })
  })
  return result
}

function cacheAsyncOptions(field, promise) {
  promise.then((options) => {
    field._cachedOptions = options
  })
}

async function loadDictOptions(dictType) {
  if (!dictType) {
    dictOptions.value = []
    return
  }
  dictOptions.value = await getDictData(dictType)
}

async function loadSourceDictOptions(dictType) {
  if (!dictType) {
    sourceDictOptions.value = []
    return
  }
  sourceDictOptions.value = await getDictData(dictType)
}

function resolveOptionSource(field = {}) {
  if (field.type === 'userSelect')
    return null
  if (field.optionSource || field.props?.optionSource)
    return field.optionSource || field.props.optionSource
  if (field.type === 'orgTreeSelect') {
    return {
      type: 'tree',
      api: 'get@/system/org/tree',
      valueField: 'id',
      keyField: 'id',
      labelField: 'orgName',
      fallbackLabelFields: ['name'],
      childrenField: 'children',
    }
  }
  return null
}

function resolveDynamicOptionSource(field = {}) {
  const source = resolveOptionSource(field)
  if (!source)
    return null
  const next = {
    ...source,
    params: resolveDynamicParams(source.params || {}),
  }
  const cascade = cascadeConfig.value
  if (cascade?.enabled && cascade.mode === 'remoteParam' && cascade.sourceField && cascade.paramName) {
    const sourceValue = props.formData?.[cascade.sourceField]
    if ((sourceValue === null || sourceValue === undefined || sourceValue === '') && cascade.emptyStrategy !== 'all') {
      next.waitForParent = true
    }
    next.params = {
      ...next.params,
      [cascade.paramName]: sourceValue,
    }
  }
  return next
}

function resolveDynamicParams(params = {}) {
  const result = {}
  Object.entries(params || {}).forEach(([key, value]) => {
    if (typeof value === 'string') {
      const matched = value.match(/^\$\{(.+)\}$/) || value.match(/^\$form\.(.+)$/)
      result[key] = matched ? props.formData?.[matched[1]] : value
      return
    }
    result[key] = value
  })
  return result
}

async function loadRemoteOptions(source, keyword = '') {
  if (!source?.api)
    return
  if (source.waitForParent) {
    remoteOptions.value = []
    return
  }
  const requestSeq = ++remoteRequestSeq
  remoteLoading.value = true
  try {
    const { method, url } = parseOptionApi(source.api)
    const params = {
      ...(source.params || {}),
    }
    if (keyword && source.keywordParam)
      params[source.keywordParam] = keyword
    const res = await request({
      method,
      url,
      params: method === 'get' ? params : undefined,
      data: method === 'get' ? undefined : params,
    })
    if (requestSeq !== remoteRequestSeq)
      return
    remoteOptions.value = normalizeRemoteOptions(res?.data, source)
  }
  catch (error) {
    console.warn(`[AiFormItem] 加载 ${props.field?.field || ''} 选项失败:`, error)
    remoteOptions.value = []
  }
  finally {
    if (requestSeq === remoteRequestSeq)
      remoteLoading.value = false
  }
}

function parseOptionApi(api) {
  const text = String(api || '')
  const [method, ...urlParts] = text.includes('@') ? text.split('@') : ['get', text]
  return {
    method: String(method || 'get').toLowerCase(),
    url: urlParts.join('@') || text,
  }
}

function normalizeRemoteOptions(data, source = {}) {
  const rows = extractOptionRows(data, source)
  if (!Array.isArray(rows))
    return []
  const isTree = source.type === 'tree'
  return rows.map(row => normalizeOptionNode(row, source, isTree)).filter(Boolean)
}

function extractOptionRows(data, source = {}) {
  if (Array.isArray(data))
    return data
  if (!data || typeof data !== 'object')
    return []
  if (source.recordsField) {
    const nested = getNestedValue(data, source.recordsField)
    if (Array.isArray(nested))
      return nested
  }
  return data.records || data.list || data.rows || []
}

function normalizeOptionNode(row, source = {}, includeChildren = false) {
  if (!row || typeof row !== 'object')
    return null
  const valueField = source.valueField || source.keyField || 'value'
  const keyField = source.keyField || valueField
  const labelField = source.labelField || 'label'
  const childrenField = source.childrenField || 'children'
  const fallbackLabelFields = source.fallbackLabelFields || ['label', 'name', 'title']
  const value = row[valueField] ?? row.value ?? row.key ?? row[keyField]
  const label = row[labelField] ?? fallbackLabelFields.map(field => row[field]).find(item => item !== undefined && item !== null && item !== '')
  const option = {
    ...row,
    value,
    key: row.key ?? row[keyField] ?? value,
    label: label === undefined || label === null || label === '' ? String(value ?? '') : String(label),
  }
  if (includeChildren) {
    const children = Array.isArray(row[childrenField])
      ? row[childrenField]
      : Array.isArray(row.children)
        ? row.children
        : []
    option.children = children.map(child => normalizeOptionNode(child, source, true)).filter(Boolean)
  }
  return option
}

function resolveCascadeConfig(field = {}) {
  const configured = [field.cascade, field.cascadeConfig, field.props?.cascade, field.props?.cascadeConfig]
    .find(item => item && typeof item === 'object' && item.sourceField)
  const raw = configured || {
    sourceField: field.sourceField || field.props?.sourceField,
    sourceDictType: field.sourceDictType || field.props?.sourceDictType,
    linkedDictType: field.linkedDictType || field.props?.linkedDictType,
    mode: field.matchMode || field.props?.matchMode || field.mode || field.props?.mode,
    paramName: field.paramName || field.props?.paramName,
    emptyStrategy: field.emptyStrategy || field.props?.emptyStrategy,
    clearOnParentChange: field.clearOnParentChange ?? field.clearOnSourceChange ?? field.props?.clearOnParentChange ?? field.props?.clearOnSourceChange,
  }
  if (!raw || raw.enabled === false || !raw.sourceField)
    return null
  return {
    enabled: true,
    sourceField: raw.sourceField,
    sourceDictType: raw.sourceDictType || '',
    linkedDictType: raw.linkedDictType || '',
    mode: raw.mode || raw.matchMode || 'linkedDict',
    paramName: raw.paramName || '',
    emptyStrategy: raw.emptyStrategy || 'empty',
    clearOnParentChange: raw.clearOnParentChange !== false && raw.clearOnSourceChange !== false,
  }
}

function resolveCascadedOptions(options = []) {
  const cascade = cascadeConfig.value
  if (!cascade?.enabled || !cascade.sourceField)
    return options
  const sourceValue = props.formData?.[cascade.sourceField]
  if (sourceValue === null || sourceValue === undefined || sourceValue === '')
    return cascade.emptyStrategy === 'all' ? options : []
  if (cascade.mode === 'remoteParam')
    return options
  return (Array.isArray(options) ? options : []).filter(option => matchesCascade(option, sourceValue, cascade))
}

function matchesCascade(option, sourceValue, cascade) {
  const raw = option.raw || option
  if (cascade.mode === 'parentDictCode') {
    const parentDictCode = raw.parentDictCode ?? raw.parent_dict_code
    const sourceDictCode = resolveSourceDictCode(sourceValue)
    return isSameOptionValue(parentDictCode, sourceDictCode) || isSameOptionValue(parentDictCode, sourceValue)
  }
  if (cascade.mode === 'linkedDict') {
    const linkedType = raw.linkedDictType ?? raw.linked_dict_type
    const linkedValue = raw.linkedDictValue ?? raw.linked_dict_value
    const expectedType = cascade.linkedDictType || cascade.sourceDictType || sourceDictType.value
    const typeMatched = !expectedType || isSameOptionValue(linkedType, expectedType)
    return typeMatched && isSameOptionValue(linkedValue, sourceValue)
  }
  return true
}

function resolveSourceDictCode(sourceValue) {
  const matched = sourceDictOptions.value.find(option => isSameOptionValue(option.value, sourceValue))
  return matched?.dictCode ?? matched?.raw?.dictCode ?? sourceValue
}

function buildDictTreeOptions(options = []) {
  const nodes = (Array.isArray(options) ? options : []).map(option => ({
    ...option,
    key: option.dictCode ?? option.key ?? option.value,
    value: option.value,
    label: option.label,
    children: [],
  }))
  const byCode = new Map(nodes.map(node => [String(node.dictCode ?? node.key), node]))
  const roots = []
  nodes.forEach((node) => {
    const parentCode = node.parentDictCode ?? node.raw?.parentDictCode
    if (parentCode !== null && parentCode !== undefined && parentCode !== '' && Number(parentCode) !== 0 && byCode.has(String(parentCode))) {
      byCode.get(String(parentCode)).children.push(node)
    }
    else {
      roots.push(node)
    }
  })
  nodes.forEach((node) => {
    if (!node.children.length)
      delete node.children
  })
  return roots
}

function findSchemaField(fieldName) {
  if (!fieldName)
    return null
  const schemas = [
    ...(props.context?.schema || []),
    ...(props.context?.allSchema || []),
  ]
  return schemas.find(item => item?.field === fieldName) || null
}

function normalizeAlign(value) {
  const align = String(value || '').toLowerCase()
  return ['left', 'center', 'right'].includes(align) ? align : 'left'
}

function clearCurrentValue() {
  if (props.value === null || props.value === undefined || props.value === '')
    return
  emit('update:value', props.field?.multiple ? [] : null)
}

function getNestedValue(source, path) {
  return String(path || '')
    .split('.')
    .filter(Boolean)
    .reduce((value, key) => value?.[key], source)
}

function resolveOptionValue(rawValue) {
  return normalizeOptionValue(rawValue, currentOptions.value, props.field?.multiple)
}

function normalizeOptionValue(rawValue, options = [], multiple = false) {
  if (rawValue === null || rawValue === undefined || rawValue === '' || !Array.isArray(options) || !options.length)
    return rawValue

  if (Array.isArray(rawValue)) {
    return rawValue.map(item => findOptionValue(options, item)).filter(item => item !== undefined)
  }

  if (multiple && typeof rawValue === 'string') {
    return rawValue.split(',').map(item => item.trim()).filter(Boolean).map(item => findOptionValue(options, item)).filter(item => item !== undefined)
  }

  return findOptionValue(options, rawValue)
}

function findOptionValue(options = [], rawValue) {
  const match = flattenOptionNodes(options).find(option => isSameOptionValue(option?.value ?? option?.key, rawValue))
  if (match)
    return match.value ?? match.key
  return rawValue
}

function flattenOptionNodes(options = []) {
  const result = []
  const walk = (nodes) => {
    ;(Array.isArray(nodes) ? nodes : []).forEach((node) => {
      if (!node || typeof node !== 'object')
        return
      result.push(node)
      if (Array.isArray(node.children))
        walk(node.children)
    })
  }
  walk(options)
  return result
}

function isSameOptionValue(left, right) {
  if (left === right)
    return true
  if (left === null || left === undefined || right === null || right === undefined)
    return false
  return String(left) === String(right)
}

function handleTreeSelectUpdate(field, newValue) {
  const normalizedValue = normalizeOptionValue(newValue, currentOptions.value, field?.multiple)
  syncIncludeChildrenFlag(field, normalizedValue)
  emit('update:value', normalizedValue)
}

function handleRegionTreeSelectUpdate(field, newValue) {
  if (!props.context?.isSearch || !field?.field) {
    emit('update:value', newValue)
    return
  }
  const includeChildrenKey = `${field.field}_includeChildren`
  if (newValue === null || newValue === undefined || newValue === '') {
    props.context?.patchFormData?.({ [includeChildrenKey]: undefined })
    emit('update:value', newValue)
    return
  }
  const textValue = String(newValue)
  if (textValue.endsWith('ALL')) {
    props.context?.patchFormData?.({ [includeChildrenKey]: true })
    emit('update:value', textValue.replace(/ALL$/, ''))
    return
  }
  props.context?.patchFormData?.({ [includeChildrenKey]: undefined })
  emit('update:value', newValue)
}

function resolveUserSelectLabel(field) {
  const targetField = resolveUserSelectLabelField(field)
  return props.formData?.[targetField] ?? field?.labelValue ?? field?.props?.labelValue ?? ''
}

function resolveUserSelectLabelField(field) {
  return field?.props?.targetField || field?.targetField || `${field?.field || ''}Name`
}

function handleUserSelectLabelUpdate(field, labelValue) {
  const targetField = resolveUserSelectLabelField(field)
  if (!targetField)
    return
  props.context?.patchFormData?.({
    [targetField]: Array.isArray(labelValue) ? labelValue.join(',') : labelValue || undefined,
  })
}

function handleUserSelect(field, users) {
  const events = getComponentEvents(field)
  if (typeof events.select === 'function')
    events.select(users)
}

function syncIncludeChildrenFlag(field, value) {
  if (!props.context?.isSearch || !field?.field || !['treeSelect', 'orgTreeSelect'].includes(field.type))
    return
  const includeChildrenKey = `${field.field}_includeChildren`
  if (Array.isArray(value) || value === null || value === undefined || value === '') {
    props.context?.patchFormData?.({ [includeChildrenKey]: undefined })
    return
  }
  if (field.type === 'orgTreeSelect') {
    props.context?.patchFormData?.({ [includeChildrenKey]: true })
    return
  }
  const selectedNode = flattenOptionNodes(currentOptions.value).find(option => isSameOptionValue(option?.value ?? option?.key, value))
  if (selectedNode?.children?.length) {
    props.context?.patchFormData?.({ [includeChildrenKey]: true })
    return
  }
  props.context?.patchFormData?.({ [includeChildrenKey]: undefined })
}

/**
 * 获取组件事件
 */
function getComponentEvents(field) {
  if (!field.on)
    return {}

  const events = {}
  Object.keys(field.on).forEach((eventName) => {
    events[eventName] = (...args) => {
      if (typeof field.on[eventName] === 'function') {
        field.on[eventName]({
          field,
          formData: props.formData,
          context: props.context,
          args,
        })
      }
    }
  })
  return events
}

/**
 * 处理值更新
 */
function handleUpdate(newValue) {
  emit('update:value', newValue)
}

function resolveRangeValue(value, index) {
  return Array.isArray(value) ? value[index] ?? null : null
}

function normalizeRangePickerValue(value) {
  if (!Array.isArray(value))
    return null
  const hasValue = value.some(item => item !== null && item !== undefined && item !== '')
  return hasValue ? value : null
}

function resolvePickerDefaultValue(field, range = false) {
  const configured = field?.pickerDefaultValue ?? field?.props?.pickerDefaultValue ?? field?.props?.defaultPickerValue
  const value = configured ?? pickerDefaultTimestamp
  return range ? [value, value] : value
}

function handleRangeUpdate(index, nextValue) {
  const next = Array.isArray(props.value) ? [...props.value] : [null, null]
  next[index] = nextValue
  emit('update:value', next)
}

/**
 * 处理文件上传变化
 */
function handleUploadChange({ fileList }) {
  emit('update:value', fileList)
}

/**
 * 复制文本
 */
function handleCopy(text) {
  copy(text)
  window.$message?.success('复制成功')
}

/**
 * 文件上传成功回调
 */
function handleUploadSuccess(field, data) {
  if (field.onSuccess && typeof field.onSuccess === 'function') {
    field.onSuccess({
      data,
      field,
      formData: props.formData,
      context: props.context,
    })
  }
}

/**
 * 文件上传失败回调
 */
function handleUploadError(field, error) {
  if (field.onError && typeof field.onError === 'function') {
    field.onError({
      error,
      field,
      formData: props.formData,
      context: props.context,
    })
  }
}

/**
 * 文件删除回调
 */
function handleUploadRemove(field, file) {
  if (field.onRemove && typeof field.onRemove === 'function') {
    field.onRemove({
      file,
      field,
      formData: props.formData,
      context: props.context,
    })
  }
}
</script>

<style scoped>
.time-range-picker {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  gap: 8px;
  align-items: center;
}

.time-range-separator {
  color: #64748b;
  font-size: 12px;
}

.ai-form-item-align-center :deep(.n-input__input-el),
.ai-form-item-align-center :deep(.n-input__textarea-el),
.ai-form-item-align-center :deep(.n-input-number-input),
.ai-form-item-align-center :deep(.n-base-selection-label__render-label) {
  text-align: center;
}

.ai-form-item-align-right :deep(.n-input__input-el),
.ai-form-item-align-right :deep(.n-input__textarea-el),
.ai-form-item-align-right :deep(.n-input-number-input),
.ai-form-item-align-right :deep(.n-base-selection-label__render-label) {
  text-align: right;
}

.ai-form-item-align-center :deep(.n-base-selection-label),
.ai-form-item-align-right :deep(.n-base-selection-label) {
  justify-content: center;
}

.ai-form-item-align-right :deep(.n-base-selection-label) {
  justify-content: flex-end;
}
</style>
