<template>
  <div class="form-item-render">
    <!-- 输入框 -->
    <n-form-item
      v-if="item.type === 'input'"
      :label="item.label"
      :required="item.required"
    >
      <n-input
        :value="internalValue"
        :placeholder="item.props.placeholder"
        :disabled="item.disabled || disabled"
        :clearable="item.props.clearable"
        :maxlength="item.props.maxLength"
        :show-count="item.props.showCount"
        @update:value="handleChange"
      />
    </n-form-item>

    <!-- 文本域 -->
    <n-form-item
      v-else-if="item.type === 'textarea'"
      :label="item.label"
      :required="item.required"
    >
      <n-input
        :value="internalValue"
        type="textarea"
        :placeholder="item.props.placeholder"
        :disabled="item.disabled || disabled"
        :rows="item.props.rows"
        :maxlength="item.props.maxLength"
        @update:value="handleChange"
      />
    </n-form-item>

    <!-- 数字输入 -->
    <n-form-item
      v-else-if="item.type === 'inputNumber'"
      :label="item.label"
      :required="item.required"
    >
      <n-input-number
        :value="internalValue"
        :placeholder="item.props.placeholder"
        :disabled="item.disabled || disabled"
        :min="item.props.min"
        :max="item.props.max"
        :precision="item.props.precision"
        :step="item.props.step"
        style="width: 100%"
        @update:value="handleChange"
      />
    </n-form-item>

    <!-- 下拉选择 -->
    <n-form-item
      v-else-if="item.type === 'select'"
      :label="item.label"
      :required="item.required"
    >
      <n-select
        :value="internalValue"
        :placeholder="item.props.placeholder"
        :disabled="item.disabled || disabled"
        :options="item.props.options"
        :multiple="item.props.multiple"
        :filterable="item.props.filterable"
        @update:value="handleChange"
      />
    </n-form-item>

    <!-- 单选框 -->
    <n-form-item
      v-else-if="item.type === 'radio'"
      :label="item.label"
      :required="item.required"
    >
      <n-radio-group
        :value="internalValue"
        :disabled="item.disabled || disabled"
        @update:value="handleChange"
      >
        <n-space>
          <n-radio
            v-for="opt in item.props.options"
            :key="opt.value"
            :value="opt.value"
          >
            {{ opt.label }}
          </n-radio>
        </n-space>
      </n-radio-group>
    </n-form-item>

    <!-- 复选框 -->
    <n-form-item
      v-else-if="item.type === 'checkbox'"
      :label="item.label"
      :required="item.required"
    >
      <n-checkbox-group
        :value="internalValue"
        :disabled="item.disabled || disabled"
        @update:value="handleChange"
      >
        <n-space>
          <n-checkbox
            v-for="opt in item.props.options"
            :key="opt.value"
            :value="opt.value"
            :label="opt.label"
          />
        </n-space>
      </n-checkbox-group>
    </n-form-item>

    <!-- 日期选择 -->
    <n-form-item
      v-else-if="item.type === 'datePicker'"
      :label="item.label"
      :required="item.required"
    >
      <n-date-picker
        :value="internalValue"
        :type="item.props.type || 'date'"
        :placeholder="item.props.placeholder"
        :disabled="item.disabled || disabled"
        :format="item.props.format"
        style="width: 100%"
        @update:value="handleChange"
      />
    </n-form-item>

    <!-- 时间选择 -->
    <n-form-item
      v-else-if="item.type === 'timePicker'"
      :label="item.label"
      :required="item.required"
    >
      <n-time-picker
        :value="internalValue"
        :placeholder="item.props.placeholder"
        :disabled="item.disabled || disabled"
        :format="item.props.format"
        style="width: 100%"
        @update:value="handleChange"
      />
    </n-form-item>

    <!-- 开关 -->
    <n-form-item
      v-else-if="item.type === 'switch'"
      :label="item.label"
      :required="item.required"
    >
      <n-switch
        :value="internalValue"
        :disabled="item.disabled || disabled"
        @update:value="handleChange"
      />
    </n-form-item>

    <!-- 滑块 -->
    <n-form-item
      v-else-if="item.type === 'slider'"
      :label="item.label"
      :required="item.required"
    >
      <n-slider
        :value="internalValue"
        :min="item.props.min"
        :max="item.props.max"
        :step="item.props.step"
        :disabled="item.disabled || disabled"
        @update:value="handleChange"
      />
    </n-form-item>

    <!-- 文件上传 -->
    <n-form-item
      v-else-if="item.type === 'upload'"
      :label="item.label"
      :required="item.required"
    >
      <n-upload
        :file-list="fileList"
        :action="item.props.action || '#'"
        :multiple="item.props.multiple"
        :accept="item.props.accept"
        :max="item.props.maxCount"
        @change="handleUploadChange"
      >
        <n-button>
          <template #icon>
            <i class="i-material-symbols:upload" />
          </template>
          点击上传
        </n-button>
      </n-upload>
    </n-form-item>

    <!-- 评分 -->
    <n-form-item
      v-else-if="item.type === 'rate'"
      :label="item.label"
      :required="item.required"
    >
      <n-rate
        :value="internalValue"
        :disabled="item.disabled || disabled"
        :count="item.props.count"
        :allow-half="item.props.allowHalf"
        @update:value="handleChange"
      />
    </n-form-item>

    <!-- 颜色选择 -->
    <n-form-item
      v-else-if="item.type === 'colorPicker'"
      :label="item.label"
      :required="item.required"
    >
      <n-color-picker
        :value="internalValue"
        :disabled="item.disabled || disabled"
        @update:value="handleChange"
      />
    </n-form-item>

    <!-- 级联选择 -->
    <n-form-item
      v-else-if="item.type === 'cascader'"
      :label="item.label"
      :required="item.required"
    >
      <n-cascader
        :value="internalValue"
        :placeholder="item.props.placeholder"
        :disabled="item.disabled || disabled"
        :options="item.props.options"
        check-strategy="child"
        style="width: 100%"
        @update:value="handleChange"
      />
    </n-form-item>

    <!-- 树选择 -->
    <n-form-item
      v-else-if="item.type === 'treeSelect'"
      :label="item.label"
      :required="item.required"
    >
      <n-tree-select
        :value="internalValue"
        :placeholder="item.props.placeholder"
        :disabled="item.disabled || disabled"
        :options="item.props.options"
        style="width: 100%"
        @update:value="handleChange"
      />
    </n-form-item>

    <!-- 分割线 -->
    <n-divider
      v-else-if="item.type === 'divider'"
      title-placement="center"
    >
      {{ item.props.title }}
    </n-divider>

    <!-- 标题 -->
    <component
      :is="`h${item.props.level || 3}`"
      v-else-if="item.type === 'title'"
      class="form-title"
    >
      {{ item.props.text }}
    </component>

    <!-- 描述文本 -->
    <p
      v-else-if="item.type === 'description'"
      class="form-description"
    >
      {{ item.props.text }}
    </p>

    <!-- 未知类型 -->
    <n-alert v-else type="warning" title="未知组件类型">
      {{ item.type }}
    </n-alert>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  item: {
    type: Object,
    required: true,
  },
  modelValue: {
    type: [String, Number, Boolean, Array, Object, Date],
    default: null,
  },
  disabled: {
    type: Boolean,
    default: false,
  },
  preview: {
    type: Boolean,
    default: true,
  },
})

const emit = defineEmits(['update:modelValue', 'change'])

// 内部值
const internalValue = ref(props.modelValue ?? props.item.defaultValue ?? null)

// 文件列表
const fileList = ref([])

// 监听外部值变化
watch(() => props.modelValue, (val) => {
  internalValue.value = val
}, { immediate: true })

// 处理值变化
function handleChange(val) {
  internalValue.value = val
  emit('update:modelValue', val)
  emit('change', val)
}

// 处理上传变化
function handleUploadChange({ fileList: files }) {
  fileList.value = files
  const urls = files.map(f => f.url || f.file?.url).filter(Boolean)
  handleChange(urls)
}
</script>

<style scoped>
.form-item-render {
  width: 100%;
}

.form-title {
  margin: 8px 0;
  font-weight: 600;
}

.form-description {
  margin: 8px 0;
  color: #666;
  font-size: 14px;
}
</style>
