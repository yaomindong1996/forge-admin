<template>
  <div class="go-project-ai-provider">
    <n-card :bordered="false" title="AI 供应商配置">
      <template #header-extra>
        <n-space>
          <n-button @click="loadProviders" :loading="loading">刷新</n-button>
          <n-button type="primary" @click="openAddForm">新增供应商</n-button>
        </n-space>
      </template>

      <n-space vertical :size="16">
        <n-alert type="info" :show-icon="false">
          在这里维护供应商配置；保存后可在 AI 对话面板中直接选择供应商、模型与温度参数。
        </n-alert>

        <n-spin :show="loading">
          <n-empty v-if="!loading && providerList.length === 0" description="暂无供应商，请先新增配置" />
          <div v-else class="provider-list">
            <n-card v-for="item in providerList" :key="item.id" size="small" class="provider-item">
              <template #header>
                <n-space align="center" :wrap="false">
                  <n-tag :type="item.isDefault === '1' ? 'success' : 'default'" size="small">
                    {{ item.isDefault === '1' ? '默认' : '备用' }}
                  </n-tag>
                  <n-text strong>{{ item.providerName || '-' }}</n-text>
                  <n-tag v-if="item.providerType" size="small" type="info">{{ getProviderTypeLabel(item.providerType) }}</n-tag>
                  <n-tag v-if="item.status === '1'" type="warning" size="small">停用</n-tag>
                </n-space>
              </template>
              <template #header-extra>
                <n-space>
                  <n-button size="small" @click="setDefault(item)" :disabled="item.isDefault === '1'">设为默认</n-button>
                  <n-button size="small" @click="openEditForm(item)">编辑</n-button>
                  <n-button size="small" type="error" @click="deleteProvider(item)">删除</n-button>
                </n-space>
              </template>

              <n-descriptions label-placement="left" :column="1" size="small">
                <n-descriptions-item label="供应商类型">{{ getProviderTypeLabel(item.providerType) }}</n-descriptions-item>
                <n-descriptions-item label="Base URL">{{ item.baseUrl || '-' }}</n-descriptions-item>
                <n-descriptions-item label="默认模型">{{ item.defaultModel || '-' }}</n-descriptions-item>
                <n-descriptions-item label="可用模型">
                  <n-space v-if="getModelTags(item).length" size="small">
                    <n-tag v-for="model in getModelTags(item)" :key="model" size="small" type="info">{{ model }}</n-tag>
                  </n-space>
                  <span v-else>-</span>
                </n-descriptions-item>
                <n-descriptions-item label="备注">{{ item.remark || '-' }}</n-descriptions-item>
              </n-descriptions>
            </n-card>
          </div>
        </n-spin>
      </n-space>
    </n-card>
  </div>

  <n-modal v-model:show="showFormRef" class="go-ai-form-modal">
    <n-card class="form-card" :title="editingProvider?.id ? '编辑供应商' : '新增供应商'" :bordered="false">
      <template #header-extra>
        <n-button text @click="closeForm">
          <n-icon size="20"><component :is="CloseIcon" /></n-icon>
        </n-button>
      </template>

      <n-form :model="formData" label-placement="left" label-width="96" style="padding: 0 8px">
        <n-form-item label="选择模板">
          <n-select
            v-model:value="selectedTemplate"
            :options="templateOptions"
            placeholder="选择预设供应商（可自动填充配置）"
            clearable
            @update:value="onTemplateSelect"
          />
        </n-form-item>

        <n-form-item label="供应商名称" required>
          <n-input v-model:value="formData.providerName" placeholder="如 阿里百炼" />
        </n-form-item>

        <n-form-item label="供应商类型" required>
          <n-select
            v-model:value="formData.providerType"
            :options="providerTypeOptions"
            placeholder="请选择供应商类型"
          />
        </n-form-item>

        <n-form-item label="Base URL" required>
          <n-input v-model:value="formData.baseUrl" placeholder="如 https://dashscope.aliyuncs.com/compatible-mode" />
        </n-form-item>

        <n-form-item label="API Key" required>
          <n-input
            v-model:value="formData.apiKey"
            type="password"
            show-password-on="click"
            placeholder="请输入 API Key"
          />
        </n-form-item>

        <n-form-item label="默认模型">
          <n-input
            v-model:value="formData.defaultModel"
            placeholder="手动填写默认模型；也可在下方可用模型中点“设为默认”"
          />
        </n-form-item>

        <n-form-item label="可用模型">
          <div class="model-editor">
            <n-space class="model-input-row" :wrap="false">
              <n-input
                v-model:value="modelInput"
                placeholder="输入模型后回车或点击添加，例如 qwen-plus"
                @keydown.enter.prevent="handleAddModel"
              />
              <n-button type="primary" ghost @click="handleAddModel">添加</n-button>
            </n-space>
            <div v-if="modelTags.length" class="model-tag-list">
              <div v-for="model in modelTags" :key="model" class="model-tag-item">
                <n-tag closable type="info" @close="handleRemoveModel(model)">{{ model }}</n-tag>
                <n-button
                  size="tiny"
                  text
                  :type="formData.defaultModel === model ? 'primary' : 'default'"
                  @click="formData.defaultModel = model"
                >
                  {{ formData.defaultModel === model ? '已设为默认' : '设为默认' }}
                </n-button>
              </div>
            </div>
            <n-empty v-else size="small" description="暂无可用模型，请先添加" />
            <n-text depth="3" class="model-tip">支持自由输入；标签可删除，也可单独指定其中一个为默认模型。</n-text>
          </div>
        </n-form-item>

        <n-form-item label="备注">
          <n-input v-model:value="formData.remark" placeholder="可选" />
        </n-form-item>
      </n-form>

      <template #footer>
        <n-space justify="space-between">
          <n-button
            :loading="testing"
            @click="testConnection"
            :disabled="!formData.baseUrl || !formData.apiKey"
          >
            测试连接
          </n-button>
          <n-space>
            <n-button @click="closeForm">取消</n-button>
            <n-button type="primary" :loading="saving" @click="saveProvider">保存</n-button>
          </n-space>
        </n-space>
      </template>
    </n-card>
  </n-modal>
</template>

<script lang="ts" setup>
import { ref, computed, reactive, onMounted, onActivated } from 'vue'
import { icon } from '@/plugins'
import {
  type AiProvider,
  type AiProviderTemplate,
  getProviderPageApi,
  getProviderTemplatesApi,
  createProviderApi,
  updateProviderApi,
  deleteProviderApi,
  testProviderApi,
  setDefaultProviderApi
} from '@/api/ai'

const { CloseIcon } = icon.ionicons5

const PROVIDER_TYPE_LABEL_MAP: Record<string, string> = {
  openai: 'OpenAI',
  azure: 'Azure OpenAI',
  dashscope: '阿里百炼',
  ollama: 'Ollama',
  zhipu: '智谱 AI',
  moonshot: 'Moonshot',
  deepseek: 'DeepSeek',
  custom: '自定义'
}

const TEMPLATE_PROVIDER_TYPE_MAP: Record<string, string> = {
  alibaba: 'dashscope',
  openai: 'openai',
  zhipu: 'zhipu',
  moonshot: 'moonshot',
  deepseek: 'deepseek',
  ollama: 'ollama',
  custom: 'openai'
}

const showFormRef = ref(false)
const loading = ref(false)
const saving = ref(false)
const testing = ref(false)
const modelInput = ref('')
const modelTags = ref<string[]>([])

const providerList = ref<AiProvider[]>([])
const templates = ref<AiProviderTemplate[]>([])
const editingProvider = ref<AiProvider | null>(null)
const selectedTemplate = ref<string | null>(null)

const formData = reactive<AiProvider>({
  providerName: '',
  providerType: 'openai',
  baseUrl: '',
  apiKey: '',
  defaultModel: '',
  models: '',
  remark: '',
  status: '0'
})

const templateOptions = computed(() =>
  templates.value.map(t => ({ label: t.name, value: t.templateKey }))
)

const providerTypeOptions = computed(() =>
  Object.entries(PROVIDER_TYPE_LABEL_MAP).map(([value, label]) => ({ label, value }))
)

const normalizeModels = (models?: string | string[]) => {
  if (Array.isArray(models)) {
    return models.map(item => String(item).trim()).filter(Boolean)
  }
  if (!models) return []
  const trimmed = models.trim()
  if (!trimmed) return []

  try {
    const parsed = JSON.parse(trimmed)
    if (Array.isArray(parsed)) {
      return parsed.map(item => String(item).trim()).filter(Boolean)
    }
  } catch {
    // ignore
  }

  return trimmed
    .split(/[\n,，]/)
    .map(item => item.trim())
    .filter(Boolean)
}

const syncModelsToForm = () => {
  const normalized = normalizeModels(modelTags.value)
  modelTags.value = normalized
  formData.models = normalized.join('\n')
}

const fillModels = (models?: string | string[]) => {
  modelTags.value = normalizeModels(models)
  syncModelsToForm()
}

const getModelTags = (item: AiProvider) => {
  const models = normalizeModels(item.models)
  if (models.length) return models
  return item.defaultModel ? [item.defaultModel] : []
}

const getProviderTypeLabel = (providerType?: string) => {
  if (!providerType) return '-'
  return PROVIDER_TYPE_LABEL_MAP[providerType] || providerType
}

const handleAddModel = () => {
  const value = modelInput.value.trim()
  if (!value) return
  if (!modelTags.value.includes(value)) {
    modelTags.value = [...modelTags.value, value]
    syncModelsToForm()
  }
  modelInput.value = ''
}

const handleRemoveModel = (model: string) => {
  modelTags.value = modelTags.value.filter(item => item !== model)
  syncModelsToForm()
  if (formData.defaultModel === model) {
    formData.defaultModel = ''
  }
}

const loadProviders = async () => {
  loading.value = true
  try {
    const res = await getProviderPageApi({ pageNum: 1, pageSize: 50 })
    providerList.value = res?.data?.records || []
  } catch (e: any) {
    window['$message']?.error('加载供应商失败: ' + e?.message)
  } finally {
    loading.value = false
  }
}

const loadTemplates = async () => {
  try {
    const res = await getProviderTemplatesApi()
    templates.value = res?.data || []
  } catch {
    // ignore
  }
}

const onTemplateSelect = (key: string | null) => {
  if (!key) return
  const tmpl = templates.value.find(t => t.templateKey === key)
  if (tmpl) {
    formData.providerName = tmpl.name === '自定义' ? '' : tmpl.name
    formData.providerType = TEMPLATE_PROVIDER_TYPE_MAP[key] || formData.providerType || 'openai'
    formData.baseUrl = tmpl.baseUrl
    formData.defaultModel = tmpl.defaultModel
    if (!modelTags.value.length && tmpl.defaultModel) {
      fillModels([tmpl.defaultModel])
    }
  }
}

const resetForm = () => {
  Object.assign(formData, {
    providerName: '',
    providerType: 'openai',
    baseUrl: '',
    apiKey: '',
    defaultModel: '',
    models: '',
    remark: '',
    status: '0'
  })
  modelInput.value = ''
  modelTags.value = []
}

const openAddForm = () => {
  editingProvider.value = null
  selectedTemplate.value = null
  resetForm()
  showFormRef.value = true
}

const openEditForm = (item: AiProvider) => {
  editingProvider.value = { ...item }
  selectedTemplate.value = null
  Object.assign(formData, {
    providerName: item.providerName || '',
    providerType: item.providerType || 'openai',
    baseUrl: item.baseUrl || '',
    apiKey: item.apiKey || '',
    defaultModel: item.defaultModel || '',
    models: item.models || '',
    remark: item.remark || '',
    status: item.status || '0'
  })
  fillModels(item.models)
  modelInput.value = ''
  showFormRef.value = true
}

const closeForm = () => {
  showFormRef.value = false
  editingProvider.value = null
}

const testConnection = async () => {
  if (!formData.baseUrl || !formData.apiKey) return
  testing.value = true
  try {
    const res = await testProviderApi({
      providerName: formData.providerName,
      baseUrl: formData.baseUrl,
      apiKey: formData.apiKey,
      defaultModel: formData.defaultModel
    })
    if (res?.code === 200) {
      window['$message']?.success('连接成功！')
    } else {
      window['$message']?.error(res?.msg || '连接失败')
    }
  } catch (e: any) {
    window['$message']?.error('连接失败: ' + e?.message)
  } finally {
    testing.value = false
  }
}

const saveProvider = async () => {
  syncModelsToForm()
  if (!formData.providerName || !formData.providerType || !formData.baseUrl || !formData.apiKey) {
    window['$message']?.warning('供应商名称、供应商类型、Base URL、API Key 不能为空')
    return
  }
  const normalizedModels = normalizeModels(modelTags.value)
  const defaultModel = formData.defaultModel?.trim() || normalizedModels[0] || ''
  if (!normalizedModels.length && !defaultModel) {
    window['$message']?.warning('请至少配置一个默认模型或可用模型')
    return
  }
  saving.value = true
  try {
    const payload: AiProvider = {
      ...formData,
      providerType: formData.providerType?.trim(),
      models: normalizedModels.length ? JSON.stringify(normalizedModels) : undefined,
      defaultModel: defaultModel || undefined
    }
    if (editingProvider.value?.id) {
      payload.id = editingProvider.value.id
      await updateProviderApi(payload)
      window['$message']?.success('更新成功')
    } else {
      await createProviderApi(payload)
      window['$message']?.success('创建成功')
    }
    closeForm()
    await loadProviders()
  } catch (e: any) {
    window['$message']?.error('保存失败: ' + e?.message)
  } finally {
    saving.value = false
  }
}

const setDefault = async (item: AiProvider) => {
  if (!item.id) return
  try {
    await setDefaultProviderApi(item.id)
    window['$message']?.success(`已将 "${item.providerName}" 设为默认供应商`)
    await loadProviders()
  } catch (e: any) {
    window['$message']?.error('操作失败: ' + e?.message)
  }
}

const deleteProvider = async (item: AiProvider) => {
  if (!item.id) return
  window['$dialog']?.warning({
    title: '确认删除',
    content: `确定要删除供应商 "${item.providerName}" 吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await deleteProviderApi(item.id!)
        window['$message']?.success('删除成功')
        await loadProviders()
      } catch (e: any) {
        window['$message']?.error('删除失败: ' + e?.message)
      }
    }
  })
}

const initPageData = async () => {
  await Promise.all([loadProviders(), loadTemplates()])
}

onMounted(async () => {
  await initPageData()
})

onActivated(async () => {
  await loadProviders()
})
</script>

<style lang="scss" scoped>
@include go(project-ai-provider) {
  padding: 30px 20px;

  .provider-list {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(420px, 1fr));
    gap: 16px;
  }

  .provider-item {
    :deep(.n-card-header) {
      align-items: flex-start;
    }
  }

  .model-editor {
    width: 100%;
    display: flex;
    flex-direction: column;
    gap: 10px;

    .model-input-row {
      width: 100%;
    }

    .model-tag-list {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .model-tag-item {
      display: flex;
      align-items: center;
      gap: 8px;
      flex-wrap: wrap;
    }

    .model-tip {
      font-size: 12px;
    }
  }
}

@include go('ai-form-modal') {
  position: fixed;
  top: 120px;
  left: 50%;
  transform: translateX(-50%);

  .form-card {
    width: 560px;
  }
}
</style>
