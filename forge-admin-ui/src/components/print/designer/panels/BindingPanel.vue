<script setup>
import { NFormItem, NInput, NSelect } from 'naive-ui'
import { computed } from 'vue'
import FileUpload from '@/components/file-upload/index.vue'
import { usePrintDesignerStore } from '@/stores/print/printDesignerStore'
import { describeFieldPath } from '../fieldGroups'
import PrintFieldPicker from '../PrintFieldPicker.vue'

const store = usePrintDesignerStore()
const target = computed(() => store.activeElement || (store.activeSurface?.kind === 'TEXT' ? store.activeSurface : null))
const sources = [
  { label: '固定内容', value: 'CONSTANT' },
  { label: '数据字段（主表/流程）', value: 'FIELD' },
  { label: '表达式', value: 'EXPRESSION' },
  { label: '系统时间', value: 'SYSTEM' },
]
const fieldSummary = computed(() => {
  if (target.value?.binding?.source !== 'FIELD')
    return ''
  return describeFieldPath(store.catalog, target.value.binding.path)
})
const isImage = computed(() => target.value?.type === 'IMAGE')
const isHtml = computed(() => target.value?.type === 'HTML')
const imageFileId = computed(() => (isImage.value && target.value?.binding?.source === 'CONSTANT' ? String(target.value.binding.value || '') : ''))

function patch(binding) {
  if (store.activeElement)
    store.patchSelected({ binding })
  else store.patchSurface({ binding })
}
function source(value) {
  if (value === 'CONSTANT') {
    patch({ source: value, value: isHtml.value ? '<div>HTML 内容</div>' : '' })
  }
  else if (value === 'SYSTEM') {
    patch({ source: value, path: 'system.generatedAt' })
  }
  else if (value === 'EXPRESSION') {
    patch({ source: value, expression: 'MONEY(main.amount)' })
  }
  else {
    const first = store.catalog.find(field => field.type !== 'COLLECTION' && (field.path.startsWith('main.') || field.path.startsWith('flow.')))
    if (first)
      patch({ source: value, path: first.path })
    else
      store.error = '当前字段目录没有可绑定的主表/流程字段；子表请用左侧「明细表」拖入'
  }
}
function onImageUpload(value) {
  const fileId = String((Array.isArray(value) ? value[0] : value) || '')
  if (fileId === imageFileId.value)
    return
  patch({ source: 'CONSTANT', value: fileId })
}
</script>

<template>
  <section v-if="target?.binding" class="designer-group binding-panel">
    <div class="binding-card">
      <div class="binding-head">
        <strong>绑定字段</strong>
        <span>常用 · 点选即可切换数据来源</span>
      </div>
      <p class="muted tip">
        文本/图片等绑主表或流程字段。若字段是<strong>子表数组</strong>，请从左侧拖入「子表 · 明细表」，不要绑到单个文本框。
      </p>
      <NFormItem label="内容来源" size="small">
        <NSelect :value="target.binding.source" :options="sources" @update:value="source" />
      </NFormItem>
      <template v-if="target.binding.source === 'FIELD'">
        <NFormItem label="绑定字段" size="small" class="field-bind-item">
          <PrintFieldPicker
            :value="target.binding.path"
            :catalog="store.catalog"
            placeholder="点击选择字段（常用）"
            @update:value="patch({ source: 'FIELD', path: $event })"
          />
        </NFormItem>
        <p v-if="fieldSummary" class="binding-summary">
          当前：{{ fieldSummary }}
        </p>
      </template>
      <template v-else-if="target.binding.source === 'EXPRESSION'">
        <NFormItem label="表达式" size="small">
          <NInput
            :value="String(target.binding.expression ?? '')"
            type="textarea"
            :autosize="{ minRows: 3, maxRows: 6 }"
            placeholder="例如 MONEY(main.qty * main.price) 或 合计：{UPPER(main.amount/100)}"
            @update:value="patch({ source: 'EXPRESSION', expression: $event })"
          />
        </NFormItem>
        <p class="muted tip">
          可用 SUM/AVG/COUNT/MIN/MAX、四则运算、IF、UPPER(元) / RMB(分) 金额大写。禁止脚本。
        </p>
      </template>
      <template v-else-if="target.binding.source === 'CONSTANT'">
        <NFormItem v-if="isImage" label="上传图片" size="small" class="upload-item">
          <FileUpload
            :model-value="imageFileId"
            :limit="1"
            :multiple="false"
            :show-download="false"
            :file-type="['png', 'jpg', 'jpeg', 'webp']"
            business-type="print"
            upload-button-text="选择图片"
            @update:model-value="onImageUpload"
          />
        </NFormItem>
        <NFormItem v-if="isImage" label="受控文件 ID" size="small">
          <NInput :value="String(target.binding.value ?? '')" placeholder="也可手动填写 fileId" @update:value="patch({ source: 'CONSTANT', value: $event })" />
        </NFormItem>
        <NFormItem v-else-if="isHtml" label="HTML 内容" size="small">
          <NInput :value="String(target.binding.value ?? '')" type="textarea" :autosize="{ minRows: 4, maxRows: 10 }" placeholder="仅支持安全展示标签，脚本会被过滤" @update:value="patch({ source: 'CONSTANT', value: $event })" />
        </NFormItem>
        <NFormItem v-else label="固定内容" size="small">
          <NInput :value="String(target.binding.value ?? '')" type="textarea" :autosize="{ minRows: 2, maxRows: 5 }" @update:value="patch({ source: 'CONSTANT', value: $event })" />
        </NFormItem>
      </template>
      <p v-if="isImage" class="muted tip">
        图片在预览时鉴权加载。
      </p>
      <p v-else-if="isHtml" class="muted tip">
        HTML 会做安全过滤，不支持脚本。
      </p>
    </div>
  </section>
</template>

<style scoped>
.binding-card {
  margin: 0 0 4px;
  padding: 10px;
  border: 1px solid color-mix(in srgb, var(--primary-color, #356cde) 28%, #e2e8f0);
  border-radius: 8px;
  background: color-mix(in srgb, var(--primary-color, #356cde) 6%, #fff);
  position: relative;
  z-index: 2;
  overflow: visible;
}
.binding-head {
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin-bottom: 6px;
}
.binding-head strong {
  font-size: 13px;
  color: var(--text-primary, #0f172a);
}
.binding-head span {
  font-size: 11px;
  color: var(--text-tertiary, #64748b);
}
.tip {
  margin: 0 0 8px;
  font-size: 11px;
  line-height: 1.45;
}
.binding-summary {
  margin: 0 0 4px;
  color: var(--text-secondary, #475569);
  font-size: 11px;
  line-height: 1.4;
  word-break: break-all;
}
.field-bind-item :deep(.picker-trigger) {
  min-height: 34px;
  border: 1px solid color-mix(in srgb, var(--primary-color, #356cde) 35%, #cbd5e1);
  background: #fff;
  font-weight: 600;
}
.upload-item :deep(.file-upload-wrapper) {
  width: 100%;
  max-width: 100%;
}
.upload-item :deep(.upload-dropzone) {
  min-height: 44px;
  padding: 8px;
}
.binding-panel :deep(.n-form-item) {
  width: 100%;
  margin-bottom: 4px;
}
.binding-panel :deep(.n-form-item-blank) {
  width: 100%;
}
</style>
