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
      :value="value"
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
      :format="field.format || 'yyyy-MM-dd'"
      :value-format="field.valueFormat || 'yyyy-MM-dd'"
      style="width: 100%"
      v-bind="field.props"
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
      :format="field.format || 'yyyy-MM-dd HH:mm:ss'"
      :value-format="field.valueFormat || 'yyyy-MM-dd HH:mm:ss'"
      style="width: 100%"
      v-bind="field.props"
      @update:value="handleUpdate"
      v-on="getComponentEvents(field)"
    />

    <!-- 日期范围选择 -->
    <n-date-picker
      v-else-if="field.type === 'daterange'"
      :value="value"
      type="daterange"
      :placeholder="field.placeholder"
      :start-placeholder="field.startPlaceholder || '开始日期'"
      :end-placeholder="field.endPlaceholder || '结束日期'"
      :disabled="disabledHandler(field)"
      :clearable="field.clearable !== false"
      :format="field.format || 'yyyy-MM-dd'"
      :value-format="field.valueFormat || 'yyyy-MM-dd'"
      style="width: 100%"
      v-bind="field.props"
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
      :format="field.format || 'yyyy-MM'"
      :value-format="field.valueFormat || 'yyyy-MM'"
      style="width: 100%"
      v-bind="field.props"
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
      :format="field.format || 'yyyy'"
      :value-format="field.valueFormat || 'yyyy'"
      style="width: 100%"
      v-bind="field.props"
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
      :format="field.format || 'HH:mm:ss'"
      :value-format="field.valueFormat || 'HH:mm:ss'"
      style="width: 100%"
      v-bind="field.props"
      @update:value="handleUpdate"
      v-on="getComponentEvents(field)"
    />

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
      @update:model-value="handleUpdate"
      :upload-button-text="field.uploadButtonText"
      :disabled="disabledHandler(field)"
      :value-type="field.valueType"
      v-bind="field.props"
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
      @update:model-value="handleUpdate"
      :value-type="field.valueType"
      v-bind="field.props"
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
      :value="value"
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

    <!-- 树形选择 -->
    <n-tree-select
      v-else-if="field.type === 'treeSelect'"
      :value="value"
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
      @update:value="handleUpdate"
      :options="field.options"
      :params="field.params"
      :transform="field.transform"
      v-bind="field.props"
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
import { computed } from 'vue'
import FileUpload from '@/components/file-upload/index.vue'
import ImageUpload from '@/components/image-upload/index.vue'
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
      result.then((options) => {
        field._cachedOptions = options
      })
      return []
    }

    return result
  }

  // 其次使用 options 数组
  if (field.options && Array.isArray(field.options)) {
    return field.options
  }

  // 检查 props.options（兼容旧的配置方式）
  if (field.props?.options && Array.isArray(field.props.options)) {
    return field.props.options
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

/**
 * 获取选项数据 (保留此函数以兼容模板中的直接调用)
 * @deprecated 建议直接使用 currentOptions
 */
function getOptions(field) {
  return currentOptions.value
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

  // 如果有 onChange 回调，执行它
  if (props.field.onChange && typeof props.field.onChange === 'function') {
    props.field.onChange({
      value: newValue,
      field: props.field,
      formData: props.formData,
      context: props.context,
    })
  }
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
