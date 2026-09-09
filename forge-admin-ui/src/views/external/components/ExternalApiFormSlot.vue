<template>
  <div v-if="name === 'apiPresetGuide'" class="preset-guide">
    <div class="preset-guide__text">
      <strong>快速填充</strong>
      <span>按企查查企业查询接口生成一份可改的配置，保存前请核对路径和权限。</span>
    </div>
    <NButton size="small" secondary @click="emit('fillPreset', formData)">
      填充企查查模板
    </NButton>
  </div>

  <div v-else-if="name === 'urlPreview'" class="url-preview">
    <span class="url-preview__label">最终请求地址</span>
    <code v-if="previewUrl" class="url-preview__url">{{ previewUrl }}</code>
    <span v-else class="url-preview__empty">选择所属系统并填写接口路径后，这里会显示完整请求地址</span>
  </div>

  <ApiConfigSection
    v-else
    :step="sectionMeta.step"
    :title="sectionMeta.title"
    :desc="sectionMeta.desc"
  >
    <NDynamicTags v-if="name === 'successCodes'" v-model:value="codeList" />

    <JsonFieldEditor
      v-else-if="jsonEditorNames.includes(name)"
      :model-value="value"
      :textarea-rows="textareaRows"
      @update:model-value="updateValue"
    />

    <SchemaTreeEditor
      v-else-if="schemaEditorNames.includes(name)"
      :model-value="value"
      :mode="name === 'inputSchemaJson' ? 'input' : 'output'"
      @update:model-value="updateValue"
    />

    <ExternalConfigEditor
      v-else
      :model-value="value"
      :mode="editorMode"
      @update:model-value="updateValue"
    />
  </ApiConfigSection>
</template>

<script setup>
import { NButton, NDynamicTags } from 'naive-ui'
import { computed } from 'vue'
import ApiConfigSection from './ApiConfigSection.vue'
import ExternalConfigEditor from './ExternalConfigEditor.vue'
import JsonFieldEditor from './JsonFieldEditor.vue'
import SchemaTreeEditor from './SchemaTreeEditor.vue'

const props = defineProps({
  name: { type: String, required: true },
  value: { type: [String, Array, Object], default: '' },
  updateValue: { type: Function, default: () => {} },
  formData: { type: Object, default: () => ({}) },
  field: { type: Object, default: () => ({}) },
  baseUrlMap: { type: Object, default: () => ({}) },
})

const emit = defineEmits(['fillPreset'])

const jsonEditorNames = ['requestParams', 'requestBodyTemplate', 'mockResponseJson']
const schemaEditorNames = ['inputSchemaJson', 'outputSchemaJson']

const editorMode = computed(() => {
  if (props.name === 'paramMappings')
    return 'mapping'
  return 'key-value'
})

// 每个配置块以「块头（标题+说明）+ 内容区」的卡片呈现；响应配置区按处理顺序编号
const sectionMetaMap = {
  requestHeaders: { title: '额外请求头', desc: '只填业务侧需要的头，认证头由所属系统配置自动附加' },
  inputSchemaJson: { title: '输入参数定义', desc: '调用方传入的参数；可粘贴参数 JSON 实例自动生成，调试表单按这里渲染' },
  requestParams: { title: '固定请求参数', desc: '每次调用固定携带的参数' },
  requestBodyTemplate: { title: '请求体模板', desc: '字段值支持 {参数名} 占位，调用时自动替换' },
  mockResponseJson: { title: 'Mock 响应', desc: 'MOCK 模式下调试和调用直接返回该 JSON' },
  paramMappings: { title: '参数映射规则', desc: '把页面字段名映射成对方接口真正使用的参数名' },
  outputSchemaJson: { step: 1, title: '返回字段解析', desc: '先粘贴响应 JSON 实例生成字段树，支持上下级嵌套；后续路径配置从该结果中选择' },
  successCodes: { step: 2, title: '成功判定', desc: '对方返回的业务状态码，命中任意一个即视为调用成功' },
}

const sectionMeta = computed(() => sectionMetaMap[props.name] || { title: props.field?.label || '', desc: '' })

const textareaRows = computed(() => {
  if (props.name === 'mockResponseJson')
    return 8
  if (props.name === 'requestBodyTemplate')
    return 6
  return 4
})

// 与后端 ExternalProxyServiceImpl#buildFullUrl 保持一致的拼接规则
const previewUrl = computed(() => {
  const base = props.baseUrlMap?.[props.formData?.systemId]
  const path = String(props.formData?.apiPath ?? '').trim()
  if (!base || !path)
    return ''
  const normalizedBase = base.endsWith('/') ? base : `${base}/`
  const normalizedPath = path.startsWith('/') ? path.slice(1) : path
  return `${normalizedBase}${normalizedPath}`
})

const codeList = computed({
  get: () => String(props.value ?? '').split(/[,，]/).map(item => item.trim()).filter(Boolean),
  set: (list) => {
    props.updateValue(list.join(','))
  },
})
</script>

<style scoped>
.preset-guide {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-width: 0;
  padding: 10px 12px;
  border: 1px solid var(--border-light);
  border-radius: 6px;
  background: var(--bg-secondary);
}

.preset-guide__text {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 2px;
}

.preset-guide__text strong {
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 600;
}

.preset-guide__text span {
  color: var(--text-tertiary);
  font-size: 12px;
  line-height: 18px;
}

.url-preview {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  flex-wrap: wrap;
}

.url-preview__label {
  color: var(--text-tertiary);
  font-size: 12px;
  white-space: nowrap;
}

.url-preview__url {
  min-width: 0;
  overflow: hidden;
  padding: 2px 8px;
  border: 1px solid var(--border-light);
  border-radius: 4px;
  background: var(--bg-secondary);
  color: var(--text-secondary);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', monospace;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.url-preview__empty {
  color: var(--text-tertiary);
  font-size: 12px;
}

@media (max-width: 720px) {
  .preset-guide {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
