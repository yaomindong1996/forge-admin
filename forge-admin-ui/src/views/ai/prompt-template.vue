<template>
  <div class="prompt-template-page">
    <AiCrudPage
      ref="crudRef"
      :api-config="{
        list: 'get@/ai/prompt-template/page',
        detail: 'get@/ai/prompt-template/:id',
        add: 'post@/ai/prompt-template',
        update: 'put@/ai/prompt-template',
        delete: 'delete@/ai/prompt-template/:id',
      }"
      :search-schema="searchSchema"
      :columns="tableColumns"
      :edit-schema="editSchema"
      :before-submit="beforeSubmit"
      row-key="id"
      :load-detail-on-edit="true"
      :edit-grid-cols="2"
      edit-label-placement="top"
      modal-type="drawer"
      modal-width="920px"
      add-button-text="新增提示词模板"
      :scroll-x="1560"
      max-height="calc(100vh - 300px)"
    />

    <n-modal
      v-model:show="previewVisible"
      preset="card"
      :title="previewTitle"
      style="width: min(960px, calc(100vw - 32px))"
    >
      <n-spin :show="previewLoading">
        <div v-if="currentTemplate" class="template-preview">
          <n-descriptions label-placement="left" bordered :column="2" size="small">
            <n-descriptions-item label="模板名称">
              {{ currentTemplate.templateName || '-' }}
            </n-descriptions-item>
            <n-descriptions-item label="适用场景">
              {{ getUsageSceneLabel(currentTemplate.usageScene) }}
            </n-descriptions-item>
            <n-descriptions-item label="业务分类">
              {{ currentTemplate.businessCategory || '-' }}
            </n-descriptions-item>
            <n-descriptions-item label="领域分类">
              {{ currentTemplate.domainCategory || '-' }}
            </n-descriptions-item>
            <n-descriptions-item label="标签">
              {{ currentTemplate.templateTags || '-' }}
            </n-descriptions-item>
            <n-descriptions-item label="统计">
              使用 {{ currentTemplate.useCount || 0 }} / 测试 {{ currentTemplate.testCount || 0 }} / 下载 {{ currentTemplate.downloadCount || 0 }}
            </n-descriptions-item>
          </n-descriptions>

          <n-tabs type="line" animated>
            <n-tab-pane name="prompt" tab="提示词内容">
              <pre class="prompt-block">{{ currentTemplate.promptContent || '-' }}</pre>
            </n-tab-pane>
            <n-tab-pane name="example" tab="示例输入">
              <pre class="prompt-block">{{ currentTemplate.exampleInput || '-' }}</pre>
            </n-tab-pane>
            <n-tab-pane name="description" tab="说明">
              <pre class="prompt-block">{{ currentTemplate.description || currentTemplate.remark || '-' }}</pre>
            </n-tab-pane>
          </n-tabs>
        </div>
      </n-spin>
      <template #footer>
        <n-space justify="end">
          <n-button @click="previewVisible = false">
            关闭
          </n-button>
          <n-button v-if="currentTemplate" @click="downloadCurrentTemplate">
            下载
          </n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { NEllipsis } from 'naive-ui'
import { computed, h, ref } from 'vue'
import { AiCrudPage } from '@/components/ai-form'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'
import { request } from '@/utils'

defineOptions({ name: 'AiPromptTemplate' })

const { dict } = useDict('ai_prompt_usage_scene', 'ai_prompt_status', 'ai_prompt_recommended')

const crudRef = ref(null)
const previewVisible = ref(false)
const previewLoading = ref(false)
const previewTitle = ref('提示词模板预览')
const currentTemplate = ref(null)

const usageSceneOptions = computed(() => dict.value.ai_prompt_usage_scene || [])
const statusOptions = computed(() => dict.value.ai_prompt_status || [])
const recommendedOptions = computed(() => dict.value.ai_prompt_recommended || [])

const searchSchema = [
  {
    field: 'keyword',
    label: '关键词',
    type: 'input',
    props: { placeholder: '名称/编码/分类/标签' },
  },
  {
    field: 'usageScene',
    label: '适用场景',
    type: 'select',
    props: { placeholder: '请选择场景', options: usageSceneOptions },
  },
  {
    field: 'businessCategory',
    label: '业务分类',
    type: 'input',
    props: { placeholder: '请输入业务分类' },
  },
  {
    field: 'domainCategory',
    label: '领域分类',
    type: 'input',
    props: { placeholder: '请输入领域分类' },
  },
  {
    field: 'status',
    label: '状态',
    type: 'select',
    props: { placeholder: '请选择状态', options: statusOptions },
  },
  {
    field: 'isRecommended',
    label: '推荐',
    type: 'select',
    props: { placeholder: '是否推荐', options: recommendedOptions },
  },
]

const tableColumns = computed(() => [
  {
    prop: 'templateName',
    label: '模板名称',
    width: 190,
    render(row) {
      return h(NEllipsis, { lineClamp: 1 }, { default: () => row.templateName || '-' })
    },
  },
  {
    prop: 'usageScene',
    label: '适用场景',
    width: 120,
    render(row) {
      return h(DictTag, { dictType: 'ai_prompt_usage_scene', value: row.usageScene, size: 'small' })
    },
  },
  {
    prop: 'businessCategory',
    label: '业务分类',
    width: 120,
    render(row) {
      return row.businessCategory || '-'
    },
  },
  {
    prop: 'domainCategory',
    label: '领域分类',
    width: 120,
    render(row) {
      return row.domainCategory || '-'
    },
  },
  {
    prop: 'templateTags',
    label: '标签',
    minWidth: 160,
    render(row) {
      return h(NEllipsis, { lineClamp: 1 }, { default: () => row.templateTags || '-' })
    },
  },
  {
    prop: 'contentSummary',
    label: '提示词摘要',
    minWidth: 240,
    render(row) {
      return h(NEllipsis, { lineClamp: 2 }, { default: () => row.contentSummary || '-' })
    },
  },
  {
    prop: 'isRecommended',
    label: '推荐',
    width: 80,
    render(row) {
      return h(DictTag, { dictType: 'ai_prompt_recommended', value: row.isRecommended, size: 'small' })
    },
  },
  {
    prop: 'status',
    label: '状态',
    width: 80,
    render(row) {
      return h(DictTag, { dictType: 'ai_prompt_status', value: row.status, size: 'small' })
    },
  },
  {
    prop: 'useCount',
    label: '使用/测试/下载',
    width: 130,
    render(row) {
      return `${row.useCount || 0}/${row.testCount || 0}/${row.downloadCount || 0}`
    },
  },
  {
    prop: 'creatorName',
    label: '创建人',
    width: 100,
    render(row) {
      return row.creatorName || row.createBy || '-'
    },
  },
  {
    prop: 'updateTime',
    label: '更新时间',
    width: 170,
  },
  {
    prop: 'action',
    label: '操作',
    width: 230,
    fixed: 'right',
    maxActionButtons: 3,
    actions: [
      { label: '编辑', key: 'edit', onClick: row => crudRef.value?.showEdit(row) },
      { label: '预览', key: 'preview', onClick: row => openPreview(row) },
      { label: '试用', key: 'test', onClick: row => handleTestTemplate(row) },
      { label: '下载', key: 'download', onClick: row => handleDownload(row) },
      { label: '删除', key: 'delete', type: 'error', onClick: row => crudRef.value?.handleDelete(row) },
    ],
  },
])

const editSchema = computed(() => [
  {
    field: 'templateName',
    label: '模板名称',
    type: 'input',
    span: 1,
    required: true,
    props: { placeholder: '请输入模板名称', maxlength: 128, showCount: true },
  },
  {
    field: 'templateCode',
    label: '模板编码',
    type: 'input',
    span: 1,
    props: { placeholder: '可选，建议英文唯一编码', maxlength: 80, showCount: true },
  },
  {
    field: 'usageScene',
    label: '适用场景',
    type: 'select',
    defaultValue: 'dashboard_generate',
    required: true,
    props: { placeholder: '请选择适用场景', options: usageSceneOptions },
  },
  {
    field: 'businessCategory',
    label: '业务分类',
    type: 'input',
    props: { placeholder: '如 经营分析、销售管理、生产运营', maxlength: 64 },
  },
  {
    field: 'domainCategory',
    label: '领域分类',
    type: 'input',
    props: { placeholder: '如 数据大屏、流程、客服、代码', maxlength: 64 },
  },
  {
    field: 'templateTags',
    label: '模板标签',
    type: 'input',
    props: { placeholder: '多个标签用逗号分隔', maxlength: 255 },
  },
  {
    field: 'isRecommended',
    label: '是否推荐',
    type: 'radio',
    defaultValue: '0',
    props: { options: recommendedOptions },
  },
  {
    field: 'status',
    label: '状态',
    type: 'radio',
    defaultValue: '0',
    props: { options: statusOptions },
  },
  {
    field: 'sortOrder',
    label: '排序号',
    type: 'inputNumber',
    defaultValue: 0,
    props: { min: 0, precision: 0 },
  },
  {
    field: 'description',
    label: '模板说明',
    type: 'textarea',
    span: 2,
    props: { placeholder: '说明适用场景、使用边界和最佳实践', rows: 3, maxlength: 500, showCount: true },
  },
  {
    field: 'promptContent',
    label: '提示词内容',
    type: 'textarea',
    span: 2,
    required: true,
    props: {
      placeholder: '请输入完整提示词模板内容',
      rows: 14,
      autosize: { minRows: 12, maxRows: 22 },
    },
  },
  {
    field: 'exampleInput',
    label: '示例输入',
    type: 'textarea',
    span: 2,
    props: { placeholder: '可填写用户使用该模板时的示例需求', rows: 4 },
  },
  {
    field: 'remark',
    label: '备注',
    type: 'textarea',
    span: 2,
    props: { placeholder: '补充说明', rows: 2, maxlength: 500, showCount: true },
  },
])

function beforeSubmit(formData) {
  return {
    ...formData,
    usageScene: formData.usageScene || 'dashboard_generate',
    status: formData.status ?? '0',
    isRecommended: formData.isRecommended ?? '0',
    sortOrder: formData.sortOrder ?? 0,
  }
}

function getUsageSceneLabel(value) {
  const item = dict.value.ai_prompt_usage_scene?.find(d => d.value === value)
  return item?.label || value || '-'
}

async function fetchTemplateDetail(row, action = 'preview') {
  const url = action === 'preview'
    ? `/ai/prompt-template/${row.id}/preview`
    : `/ai/prompt-template/${row.id}/${action}`
  const method = action === 'preview' ? 'get' : 'post'
  const res = await request({ method, url })
  return res.data
}

async function openPreview(row) {
  previewTitle.value = '提示词模板预览'
  previewVisible.value = true
  previewLoading.value = true
  currentTemplate.value = null
  try {
    currentTemplate.value = await fetchTemplateDetail(row, 'preview')
  }
  catch (error) {
    window.$message.error(error?.message || '获取模板详情失败')
  }
  finally {
    previewLoading.value = false
  }
}

async function handleTestTemplate(row) {
  previewTitle.value = '提示词模板试用'
  previewVisible.value = true
  previewLoading.value = true
  currentTemplate.value = null
  try {
    currentTemplate.value = await fetchTemplateDetail(row, 'test')
    crudRef.value?.refresh()
    window.$message.success('已记录一次模板试用')
  }
  catch (error) {
    window.$message.error(error?.message || '模板试用失败')
  }
  finally {
    previewLoading.value = false
  }
}

async function handleDownload(row) {
  try {
    const template = await fetchTemplateDetail(row, 'download')
    downloadTemplate(template)
    crudRef.value?.refresh()
  }
  catch (error) {
    window.$message.error(error?.message || '下载模板失败')
  }
}

async function downloadCurrentTemplate() {
  if (!currentTemplate.value)
    return
  try {
    const latest = await fetchTemplateDetail(currentTemplate.value, 'download')
    currentTemplate.value = latest
    downloadTemplate(latest)
    crudRef.value?.refresh()
  }
  catch (error) {
    window.$message.error(error?.message || '下载模板失败')
  }
}

function downloadTemplate(template) {
  const content = [
    `# ${template.templateName || '提示词模板'}`,
    '',
    `- 适用场景：${getUsageSceneLabel(template.usageScene)}`,
    `- 业务分类：${template.businessCategory || '-'}`,
    `- 领域分类：${template.domainCategory || '-'}`,
    `- 标签：${template.templateTags || '-'}`,
    '',
    '## 提示词内容',
    '',
    template.promptContent || '',
    '',
    '## 示例输入',
    '',
    template.exampleInput || '',
    '',
    '## 说明',
    '',
    template.description || template.remark || '',
  ].join('\n')
  const blob = new Blob([content], { type: 'text/markdown;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `${sanitizeFilename(template.templateName || 'prompt-template')}.md`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

function sanitizeFilename(name) {
  return String(name).replace(/[\\/:*?"<>|]/g, '_')
}
</script>

<style scoped>
.prompt-template-page {
  min-height: 100%;
}

.template-preview {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.prompt-block {
  max-height: 520px;
  margin: 0;
  padding: 12px;
  overflow: auto;
  border-radius: 8px;
  background: #0f172a;
  color: #dbeafe;
  font-size: 12px;
  line-height: 1.65;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
