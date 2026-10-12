<template>
  <div class="ai-provider-page">
    <AiCrudPage
      ref="crudRef"
      api="/ai/provider"
      :api-config="{
        list: 'get@/ai/provider/page',
        detail: 'get@/ai/provider/:id',
        add: 'post@/ai/provider',
        update: 'put@/ai/provider',
        delete: 'delete@/ai/provider/:id',
      }"
      :search-schema="searchSchema"
      :columns="tableColumns"
      :edit-schema="editSchema"
      :before-submit="beforeSubmit"
      row-key="id"
      :edit-grid-cols="2"
      modal-width="800px"
      add-button-text="新增供应商"
    />

    <n-modal
      v-model:show="modelModalVisible"
      preset="card"
      :title="modelModalTitle"
      class="provider-model-modal"
      :style="{ width: 'min(760px, 92vw)' }"
      :mask-closable="false"
    >
      <div v-if="activeModelProvider" class="model-provider-strip">
        <AuthImage
          v-if="activeModelProvider.logo"
          :src="activeModelProvider.logo"
          :img-style="{ width: '40px', height: '40px', borderRadius: '8px', objectFit: 'cover' }"
        />
        <div v-else class="model-provider-avatar">
          {{ getProviderInitial(activeModelProvider) }}
        </div>
        <div class="model-provider-copy">
          <div class="model-provider-name">
            {{ activeModelProvider.providerName }}
          </div>
          <div class="model-provider-meta">
            {{ getLabel('ai_provider_type', activeModelProvider.providerType) || '未分类' }}
          </div>
        </div>
      </div>

      <n-form
        ref="modelFormRef"
        :model="modelForm"
        :rules="modelRules"
        label-placement="top"
        class="model-form"
      >
        <n-grid :cols="2" :x-gap="16" :y-gap="4">
          <n-form-item-gi label="供应商" path="providerId">
            <n-select
              v-model:value="modelForm.providerId"
              :options="selectedProviderOptions"
              disabled
            />
          </n-form-item-gi>
          <n-form-item-gi label="模型类型" path="modelType">
            <n-select
              v-model:value="modelForm.modelType"
              :options="modelTypeOptions"
              placeholder="请选择模型类型"
            />
          </n-form-item-gi>
          <n-form-item-gi label="模型标识" path="modelId">
            <n-input v-model:value="modelForm.modelId" placeholder="如 gpt-4o" />
          </n-form-item-gi>
          <n-form-item-gi label="模型名称" path="modelName">
            <n-input v-model:value="modelForm.modelName" placeholder="如 GPT-4o" />
          </n-form-item-gi>
          <n-form-item-gi label="最大Token" path="maxTokens">
            <n-input-number
              v-model:value="modelForm.maxTokens"
              :min="0"
              placeholder="如 128000"
              style="width: 100%"
            />
          </n-form-item-gi>
          <n-form-item-gi label="排序号" path="sortOrder">
            <n-input-number
              v-model:value="modelForm.sortOrder"
              :min="0"
              placeholder="排序值"
              style="width: 100%"
            />
          </n-form-item-gi>
          <n-form-item-gi label="默认模型" path="isDefault">
            <n-radio-group v-model:value="modelForm.isDefault">
              <n-radio v-for="item in isDefaultOptions" :key="item.value" :value="item.value">
                {{ item.label }}
              </n-radio>
            </n-radio-group>
          </n-form-item-gi>
          <n-form-item-gi label="状态" path="status">
            <n-radio-group v-model:value="modelForm.status">
              <n-radio v-for="item in statusOptions" :key="item.value" :value="item.value">
                {{ item.label }}
              </n-radio>
            </n-radio-group>
          </n-form-item-gi>
          <n-form-item-gi :span="2" label="描述" path="description">
            <n-input
              v-model:value="modelForm.description"
              type="textarea"
              :rows="3"
              placeholder="请输入模型描述"
            />
          </n-form-item-gi>
        </n-grid>
      </n-form>

      <template #action>
        <NButton @click="modelModalVisible = false">
          取消
        </NButton>
        <NButton type="primary" :loading="modelSaving" @click="handleSaveModel">
          保存模型
        </NButton>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { NAvatar, NButton } from 'naive-ui'
import { computed, h, nextTick, reactive, ref } from 'vue'
import { modelAdd, providerSetDefault, providerTest } from '@/api/ai'
import { AiCrudPage } from '@/components/ai-form'
import AuthImage from '@/components/common/AuthImage.vue'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'

defineOptions({ name: 'AiProvider' })

const crudRef = ref(null)
const modelFormRef = ref(null)

// 加载字典
const { dict, getLabel } = useDict('ai_provider_type', 'ai_model_type', 'ai_status', 'ai_is_default')

// 供应商类型选项
const providerTypeOptions = computed(() => dict.value.ai_provider_type || [])
// 模型类型选项
const modelTypeOptions = computed(() => dict.value.ai_model_type || [])
// 状态选项
const statusOptions = computed(() => dict.value.ai_status || [])
// 是否默认选项
const isDefaultOptions = computed(() => dict.value.ai_is_default || [])

// 已知供应商类型的默认 Base URL（与后端 AiProviderBaseUrlPolicy 对齐）
// azure / custom 无固定官方端点，需手动填写
const PROVIDER_DEFAULT_BASE_URL = {
  alibaba: 'https://dashscope.aliyuncs.com/compatible-mode',
  openai: 'https://api.openai.com/v1',
  zhipu: 'https://open.bigmodel.cn/api/paas/v4',
  moonshot: 'https://api.moonshot.cn/v1',
  deepseek: 'https://api.deepseek.com/v1',
  ollama: 'http://localhost:11434/v1',
}

const modelModalVisible = ref(false)
const modelSaving = ref(false)
const activeModelProvider = ref(null)

const modelForm = reactive({
  providerId: null,
  modelType: null,
  modelId: '',
  modelName: '',
  maxTokens: null,
  sortOrder: 0,
  isDefault: '0',
  status: '0',
  description: '',
})

const modelRules = {
  providerId: [{ required: true, message: '请选择供应商', trigger: 'change' }],
  modelType: [{ required: true, message: '请选择模型类型', trigger: 'change' }],
  modelId: [{ required: true, message: '请输入模型标识', trigger: 'blur' }],
  modelName: [{ required: true, message: '请输入模型名称', trigger: 'blur' }],
}

const modelModalTitle = computed(() => activeModelProvider.value
  ? `新增模型 - ${activeModelProvider.value.providerName}`
  : '新增模型')

const selectedProviderOptions = computed(() => activeModelProvider.value
  ? [{ label: activeModelProvider.value.providerName, value: activeModelProvider.value.id }]
  : [])

// 搜索表单配置
const searchSchema = computed(() => [
  {
    field: 'providerName',
    label: '供应商名称',
    type: 'input',
    props: { placeholder: '请输入供应商名称' },
  },
  {
    field: 'providerType',
    label: '供应商类型',
    type: 'select',
    props: { placeholder: '请选择类型', options: providerTypeOptions.value },
  },
  {
    field: 'status',
    label: '状态',
    type: 'select',
    props: { placeholder: '请选择状态', options: statusOptions.value },
  },
])

// 表格列配置
const tableColumns = computed(() => [
  {
    prop: 'logo',
    label: 'Logo',
    width: 60,
    render: (row) => {
      if (row.logo) {
        return h(AuthImage, {
          src: row.logo,
          imgStyle: { width: '32px', height: '32px', borderRadius: '50%', objectFit: 'cover' },
        })
      }
      return h(NAvatar, { size: 32, round: true }, { default: () => getProviderInitial(row) })
    },
  },
  {
    prop: 'providerName',
    label: '供应商名称',
    width: 160,
  },
  {
    prop: 'providerType',
    label: '类型',
    width: 120,
    render: (row) => {
      return h(DictTag, { dictType: 'ai_provider_type', value: row.providerType, size: 'small' })
    },
  },
  {
    prop: 'baseUrl',
    label: 'Base URL',
    minWidth: 200,
  },
  {
    prop: 'defaultModel',
    label: '默认模型',
    width: 150,
  },
  {
    prop: 'isDefault',
    label: '默认供应商',
    width: 100,
    render: (row) => {
      return h(DictTag, { dictType: 'ai_is_default', value: row.isDefault, size: 'small' })
    },
  },
  {
    prop: 'status',
    label: '状态',
    width: 80,
    render: (row) => {
      return h(DictTag, { dictType: 'ai_status', value: row.status, size: 'small' })
    },
  },
  {
    prop: 'action',
    label: '操作',
    width: 260,
    fixed: 'right',
    maxActionButtons: 3,
    actions: [
      { label: '添加模型', key: 'addModel', type: 'primary', onClick: handleAddModel },
      { label: '编辑', key: 'edit', onClick: handleEdit },
      { label: '测试连接', key: 'test', onClick: handleTestConnection },
      { label: '设为默认', key: 'setDefault', onClick: handleSetDefault, visible: row => row.isDefault !== '1' },
      { label: '删除', key: 'delete', type: 'error', onClick: handleDelete },
    ],
  },
])

// 编辑表单配置
const editSchema = computed(() => [
  {
    field: 'providerName',
    label: '供应商名称',
    type: 'input',
    rules: [{ required: true, message: '请输入供应商名称', trigger: 'blur' }],
    props: { placeholder: '请输入供应商名称' },
  },
  {
    field: 'providerType',
    label: '供应商类型',
    type: 'select',
    rules: [{ required: true, message: '请选择供应商类型', trigger: 'change' }],
    props: { placeholder: '请选择供应商类型', options: providerTypeOptions.value },
    // 选择已知类型且 Base URL 为空时自动补默认地址，无需手动填写
    // （用 onChange：在表单重建后拿到活对象，赋值才能保留并随表单提交）
    onChange: ({ value, formData }) => {
      const defaultUrl = PROVIDER_DEFAULT_BASE_URL[value]
      if (defaultUrl && !formData.baseUrl) {
        formData.baseUrl = defaultUrl
      }
    },
  },
  {
    field: 'logo',
    label: '供应商Logo',
    type: 'imageUpload',
    businessType: 'ai-provider-logo',
    limit: 1,
    fileSize: 2,
    fileType: ['png', 'jpg', 'jpeg', 'svg', 'webp'],
    valueType: 'string',
    props: { showTip: true },
  },
  {
    field: 'baseUrl',
    label: 'Base URL',
    type: 'input',
    span: 2,
    // 已知供应商类型选择后自动填充默认地址；azure / custom 需手动填写
    props: { placeholder: '留空自动使用默认地址，如 https://dashscope.aliyuncs.com/compatible-mode' },
  },
  {
    field: 'apiKey',
    label: 'API Key',
    type: 'input',
    span: 2,
    rules: [{ required: true, message: '请输入 API Key', trigger: 'blur' }],
    props: { placeholder: '请输入 API Key', type: 'password', showPasswordOn: 'click' },
  },
  {
    type: 'divider',
    label: '其他配置',
    props: { titlePlacement: 'left' },
    span: 2,
  },
  {
    field: 'status',
    label: '状态',
    type: 'radio',
    defaultValue: '0',
    props: { options: statusOptions.value },
  },
  {
    field: 'remark',
    label: '备注',
    type: 'textarea',
    span: 2,
    props: { placeholder: '请输入备注', rows: 3 },
  },
])

// 表单提交前处理
function beforeSubmit(formData) {
  return formData
}

// 编辑
function handleEdit(row) {
  crudRef.value?.showEdit(row)
}

// 删除
function handleDelete(row) {
  crudRef.value?.handleDelete(row)
}

function getProviderInitial(row) {
  return (row?.providerName || getLabel('ai_provider_type', row?.providerType) || '-').charAt(0)
}

async function handleAddModel(row) {
  activeModelProvider.value = row
  Object.assign(modelForm, {
    providerId: row.id,
    modelType: modelTypeOptions.value[0]?.value ?? null,
    modelId: '',
    modelName: '',
    maxTokens: null,
    sortOrder: 0,
    isDefault: '0',
    status: '0',
    description: '',
  })
  modelModalVisible.value = true
  await nextTick()
  modelFormRef.value?.restoreValidation()
}

async function handleSaveModel() {
  try {
    await modelFormRef.value?.validate()
  }
  catch {
    return
  }

  modelSaving.value = true
  try {
    const payload = {
      ...modelForm,
      modelId: modelForm.modelId?.trim(),
      modelName: modelForm.modelName?.trim(),
      description: modelForm.description?.trim(),
    }
    const res = await modelAdd(payload)
    if (res.code === 200) {
      window.$message.success('模型新增成功')
      modelModalVisible.value = false
      crudRef.value?.refresh()
    }
    else {
      window.$message.error(res.msg || '模型新增失败')
    }
  }
  catch (error) {
    window.$message.error(error.message || '模型新增失败')
  }
  finally {
    modelSaving.value = false
  }
}

// 测试连接
async function handleTestConnection(row) {
  try {
    const res = await providerTest({
      baseUrl: row.baseUrl,
      apiKey: row.apiKey,
      defaultModel: row.defaultModel,
      providerName: row.providerName,
    })
    if (res.code === 200) {
      window.$message.success('连接成功！')
    }
    else {
      window.$message.error(res.msg || '连接失败')
    }
  }
  catch (error) {
    window.$message.error(`连接失败: ${error.message || '未知错误'}`)
  }
}

// 设为默认
async function handleSetDefault(row) {
  try {
    const res = await providerSetDefault(row.id)
    if (res.code === 200) {
      window.$message.success('设置成功')
      crudRef.value?.refresh()
    }
  }
  catch {
    window.$message.error('设置失败')
  }
}
</script>

<style scoped>
.ai-provider-page {
  height: 100%;
}

.provider-model-modal :deep(.n-card-header) {
  padding-bottom: 12px;
  border-bottom: 1px solid #eef0f3;
}

.provider-model-modal :deep(.n-card__content) {
  padding-top: 16px;
}

.model-provider-strip {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  margin-bottom: 16px;
  background: var(--bg-secondary);
  border: 1px solid var(--border-light);
  border-radius: 8px;
}

.model-provider-avatar {
  display: flex;
  width: 40px;
  height: 40px;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 700;
  background: #111827;
  border-radius: 8px;
}

.model-provider-copy {
  min-width: 0;
}

.model-provider-name {
  overflow: hidden;
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.model-provider-meta {
  margin-top: 2px;
  color: var(--text-secondary);
  font-size: 12px;
}

.model-form :deep(.n-form-item-label) {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}
</style>
