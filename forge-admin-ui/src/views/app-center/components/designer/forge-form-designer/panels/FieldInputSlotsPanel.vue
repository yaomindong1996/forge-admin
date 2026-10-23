<template>
  <div class="field-input-slots-panel">
    <!-- 前缀 / 后缀文本 + 图标（Input / InputNumber 共用） -->
    <n-form-item label="前缀 / 后缀">
      <div class="option-editor-row two-columns">
        <n-input
          :value="propsMap.prefix || ''"
          clearable
          :placeholder="isMoney ? '如 ￥（可空，空则用货币符号）' : '前缀文本'"
          @update:value="patch({ prefix: $event || undefined })"
        />
        <n-input
          :value="propsMap.suffix || ''"
          clearable
          placeholder="后缀文本"
          @update:value="patch({ suffix: $event || undefined })"
        />
      </div>
    </n-form-item>
    <n-form-item label="前缀 / 后缀图标">
      <div class="option-editor-row two-columns icon-row">
        <IconSelector
          :model-value="propsMap.prefixIcon || ''"
          @update:model-value="patch({ prefixIcon: $event || undefined })"
        />
        <IconSelector
          :model-value="propsMap.suffixIcon || ''"
          @update:model-value="patch({ suffixIcon: $event || undefined })"
        />
      </div>
    </n-form-item>

    <template v-if="isTextInput">
      <n-form-item label="输入类型">
        <n-select
          :value="propsMap.inputType || 'text'"
          :options="inputTypeOptions"
          @update:value="patch({ inputType: $event || 'text' })"
        />
      </n-form-item>
      <n-form-item label="清除图标">
        <IconSelector
          :model-value="propsMap.clearIcon || ''"
          @update:model-value="patch({ clearIcon: $event || undefined })"
        />
      </n-form-item>
      <n-form-item v-if="isPassword" label="密码显隐图标">
        <div class="option-editor-row two-columns icon-row">
          <div class="icon-field">
            <span class="icon-field-label">可见</span>
            <IconSelector
              :model-value="propsMap.passwordVisibleIcon || ''"
              @update:model-value="patch({ passwordVisibleIcon: $event || undefined })"
            />
          </div>
          <div class="icon-field">
            <span class="icon-field-label">隐藏</span>
            <IconSelector
              :model-value="propsMap.passwordInvisibleIcon || ''"
              @update:model-value="patch({ passwordInvisibleIcon: $event || undefined })"
            />
          </div>
        </div>
      </n-form-item>
      <n-form-item label="成对输入">
        <div class="pair-row">
          <n-switch
            size="small"
            :value="propsMap.pair === true"
            @update:value="patch({ pair: $event || undefined })"
          >
            <template #checked>
              开启
            </template>
            <template #unchecked>
              关闭
            </template>
          </n-switch>
          <n-input
            v-if="propsMap.pair === true"
            :value="propsMap.separator || ''"
            clearable
            placeholder="分隔符，如 - 或 ~"
            @update:value="patch({ separator: $event || undefined })"
          />
        </div>
      </n-form-item>
    </template>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import IconSelector from '@/components/IconSelector.vue'

const props = defineProps({
  component: { type: Object, required: true },
})

const emit = defineEmits(['update'])

const propsMap = computed(() => props.component?.props || {})
const componentKey = computed(() => props.component?.componentKey || '')
const isTextInput = computed(() => componentKey.value === 'input')
const isMoney = computed(() => componentKey.value === 'money')
const isPassword = computed(() => String(propsMap.value.inputType || '').toLowerCase() === 'password')

const inputTypeOptions = [
  { label: '文本', value: 'text' },
  { label: '密码', value: 'password' },
]

function patch(next) {
  emit('update', next)
}
</script>

<style scoped>
/* display:contents 时父级 scoped 的 .option-editor-row 打不进来，布局样式必须写在本组件 */
.field-input-slots-panel {
  display: contents;
}

.option-editor-row {
  display: grid;
  width: 100%;
  gap: 8px;
  min-width: 0;
}

.option-editor-row.two-columns {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.option-editor-row.icon-row {
  align-items: stretch;
}

.option-editor-row :deep(.icon-selector),
.option-editor-row :deep(.n-button),
.pair-row :deep(.n-input) {
  width: 100%;
}

.field-input-slots-panel > :deep(.n-form-item .icon-selector),
.field-input-slots-panel > :deep(.n-form-item .n-button) {
  width: 100%;
}

.icon-field {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.icon-field-label {
  font-size: 12px;
  color: var(--text-tertiary, #8b95a8);
}

.pair-row {
  display: grid;
  gap: 8px;
  width: 100%;
}
</style>
