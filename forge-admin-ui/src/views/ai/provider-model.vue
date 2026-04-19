<template>
  <div class="ai-provider-model-page">
    <div class="panel-card">
      <div class="panel-header">
        <div class="panel-title">
          <i class="ai-icon:server" />
          <span>供应商管理</span>
        </div>
        <NButton type="primary" size="small" @click="handleAddProvider">
          <i class="ai-icon:plus" /> 新增供应商
        </NButton>
      </div>
      <div class="search-bar">
        <n-input v-model:value="providerSearch.name" placeholder="供应商名称" clearable size="small" @keyup.enter="loadProviders" @clear="loadProviders" />
        <n-select v-model:value="providerSearch.type" placeholder="类型" clearable size="small" :options="providerTypeOptions" @update:value="loadProviders" />
        <n-select v-model:value="providerSearch.status" placeholder="状态" clearable size="small" :options="statusOptions" @update:value="loadProviders" />
        <NButton size="small" type="primary" @click="loadProviders">
          <i class="ai-icon:search" /> 查询
        </NButton>
      </div>
      <n-data-table
        :columns="providerColumns"
        :data="providerList"
        :loading="providerLoading"
        :row-key="row => row.id"
        :row-props="providerRowProps"
        size="small"
        class="provider-table"
      />
      <div class="pagination-wrap">
        <n-pagination v-model:page="providerPagination.pageNum" :page-count="providerPagination.totalPages" size="small" @update:page="loadProviders" />
      </div>
    </div>

    <n-collapse-transition :show="!!selectedProvider">
      <div class="panel-card model-panel">
        <div class="panel-header">
          <div class="panel-title">
            <i class="ai-icon:brain" />
            <span>{{ selectedProvider?.providerName || '' }} 下的模型</span>
            <NTag size="small" round type="info">
              {{ modelList.length }}
            </NTag>
          </div>
          <NButton type="primary" size="small" @click="handleAddModel">
            <i class="ai-icon:plus" /> 新增模型
          </NButton>
        </div>
        <n-data-table
          :columns="modelColumns"
          :data="modelList"
          :loading="modelLoading"
          :row-key="row => row.id"
          size="small"
          class="model-table"
        />
      </div>
    </n-collapse-transition>
    <div v-if="!selectedProvider" class="empty-hint">
      <i class="ai-icon:mouse-pointer" style="font-size: 24px; opacity: 0.3" />
      <span>点击供应商行查看其下的模型</span>
    </div>

    <n-modal v-model:show="providerModal.show" preset="card" :title="providerModal.isEdit ? '编辑供应商' : '新增供应商'" :style="{ maxWidth: '800px', width: '90vw' }">
      <n-form ref="providerFormRef" :model="providerModal.form" :rules="providerRules" label-placement="left" label-width="100">
        <n-grid :cols="2" :x-gap="16">
          <n-form-item-gi label="供应商名称" path="providerName">
            <n-input v-model:value="providerModal.form.providerName" placeholder="请输入供应商名称" />
          </n-form-item-gi>
          <n-form-item-gi label="供应商类型" path="providerType">
            <n-select v-model:value="providerModal.form.providerType" placeholder="请选择类型" :options="providerTypeOptions" />
          </n-form-item-gi>
          <n-form-item-gi label="Logo" path="logo">
            <n-upload
              :action="`${uploadPrefix}/system/file/upload`"
              :headers="uploadHeaders"
              :max="1"
              accept="png,jpg,jpeg,svg,webp"
              :default-upload="true"
              @finish="handleLogoUploadFinish"
            >
              <div v-if="providerModal.form.logo" class="logo-preview">
                <AuthImage :src="providerModal.form.logo" :img-style="{ width: '64px', height: '64px', borderRadius: '12px', objectFit: 'cover' }" />
                <NButton text size="tiny" type="error" class="logo-remove" @click.stop="providerModal.form.logo = ''">
                  <i class="ai-icon:x" />
                </NButton>
              </div>
              <NButton v-else size="small">
                <i class="ai-icon:upload" /> 上传Logo
              </NButton>
            </n-upload>
          </n-form-item-gi>
          <n-form-item-gi label="状态" path="status">
            <n-radio-group v-model:value="providerModal.form.status">
              <n-radio v-for="opt in statusOptions" :key="opt.value" :value="opt.value">
                {{ opt.label }}
              </n-radio>
            </n-radio-group>
          </n-form-item-gi>
          <n-form-item-gi :span="2" label="Base URL" path="baseUrl">
            <n-input v-model:value="providerModal.form.baseUrl" placeholder="如 https://api.openai.com" />
          </n-form-item-gi>
          <n-form-item-gi :span="2" label="API Key" path="apiKey">
            <n-input v-model:value="providerModal.form.apiKey" type="password" show-password-on="click" placeholder="请输入 API Key" />
          </n-form-item-gi>
          <n-form-item-gi :span="2" label="备注" path="remark">
            <n-input v-model:value="providerModal.form.remark" type="textarea" :rows="3" placeholder="请输入备注" />
          </n-form-item-gi>
        </n-grid>
      </n-form>
      <template #action>
        <NButton @click="providerModal.show = false">
          取消
        </NButton>
        <NButton type="primary" :loading="providerModal.saving" @click="handleSaveProvider">
          确定
        </NButton>
      </template>
    </n-modal>

    <n-modal v-model:show="testResult.show" preset="card" title="连接测试结果" :style="{ maxWidth: '600px', width: '90vw' }">
      <n-spin :show="testResult.loading">
        <pre class="test-result-content">{{ testResult.content }}</pre>
      </n-spin>
      <template #action>
        <NButton @click="testResult.show = false">
          关闭
        </NButton>
      </template>
    </n-modal>

    <n-modal v-model:show="modelModal.show" preset="card" :title="modelModal.isEdit ? '编辑模型' : '新增模型'" :style="{ maxWidth: '800px', width: '90vw' }">
      <n-form ref="modelFormRef" :model="modelModal.form" :rules="modelRules" label-placement="left" label-width="100">
        <n-grid :cols="2" :x-gap="16">
          <n-form-item-gi label="供应商" path="providerId">
            <n-select v-model:value="modelModal.form.providerId" placeholder="请选择供应商" :options="providerSelectOptions" :disabled="modelModal.isEdit" />
          </n-form-item-gi>
          <n-form-item-gi label="模型类型" path="modelType">
            <n-select v-model:value="modelModal.form.modelType" placeholder="请选择模型类型" :options="modelTypeOptions" />
          </n-form-item-gi>
          <n-form-item-gi label="模型标识" path="modelId">
            <n-input v-model:value="modelModal.form.modelId" placeholder="如 gpt-4o" />
          </n-form-item-gi>
          <n-form-item-gi label="模型名称" path="modelName">
            <n-input v-model:value="modelModal.form.modelName" placeholder="如 GPT-4o" />
          </n-form-item-gi>
          <n-form-item-gi label="最大Token" path="maxTokens">
            <n-input-number v-model:value="modelModal.form.maxTokens" placeholder="128000" :min="0" style="width: 100%" />
          </n-form-item-gi>
          <n-form-item-gi label="排序号" path="sortOrder">
            <n-input-number v-model:value="modelModal.form.sortOrder" placeholder="排序值" :min="0" style="width: 100%" />
          </n-form-item-gi>
          <n-form-item-gi label="默认模型" path="isDefault">
            <n-radio-group v-model:value="modelModal.form.isDefault">
              <n-radio v-for="opt in isDefaultOptions" :key="opt.value" :value="opt.value">
                {{ opt.label }}
              </n-radio>
            </n-radio-group>
          </n-form-item-gi>
          <n-form-item-gi label="状态" path="status">
            <n-radio-group v-model:value="modelModal.form.status">
              <n-radio v-for="opt in statusOptions" :key="opt.value" :value="opt.value">
                {{ opt.label }}
              </n-radio>
            </n-radio-group>
          </n-form-item-gi>
          <n-form-item-gi label="图标" path="icon">
            <n-upload
              :action="`${uploadPrefix}/system/file/upload`"
              :headers="uploadHeaders"
              :max="1"
              accept="png,jpg,jpeg,svg,webp"
              :default-upload="true"
              @finish="handleIconUploadFinish"
            >
              <div v-if="modelModal.form.icon" class="logo-preview">
                <AuthImage :src="modelModal.form.icon" :img-style="{ width: '64px', height: '64px', borderRadius: '12px', objectFit: 'cover' }" />
                <NButton text size="tiny" type="error" class="logo-remove" @click.stop="modelModal.form.icon = ''">
                  <i class="ai-icon:x" />
                </NButton>
              </div>
              <NButton v-else size="small">
                <i class="ai-icon:upload" /> 上传图标
              </NButton>
            </n-upload>
          </n-form-item-gi>
          <n-form-item-gi :span="2" label="描述" path="description">
            <n-input v-model:value="modelModal.form.description" type="textarea" :rows="3" placeholder="请输入模型描述" />
          </n-form-item-gi>
        </n-grid>
      </n-form>
      <template #action>
        <NButton @click="modelModal.show = false">
          取消
        </NButton>
        <NButton type="primary" :loading="modelModal.saving" @click="handleSaveModel">
          确定
        </NButton>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { NButton, NPopconfirm, NSwitch, NTag } from 'naive-ui'
import { computed, h, onMounted, reactive, ref } from 'vue'
import {
  modelPage as fetchModelPage,
  providerPage as fetchProviderPage,
  modelAdd,
  modelDelete,
  modelGetById,
  modelUpdate,
  providerAdd,
  providerDelete,
  providerGetById,
  providerSetDefault,
  providerTest,
  providerUpdate,
} from '@/api/ai'
import AuthImage from '@/components/common/AuthImage.vue'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'

defineOptions({ name: 'AiProviderModel' })

const { dict } = useDict('ai_provider_type', 'ai_model_type', 'ai_status', 'ai_is_default')

const providerTypeOptions = computed(() => dict.value.ai_provider_type || [])
const modelTypeOptions = computed(() => dict.value.ai_model_type || [])
const statusOptions = computed(() => dict.value.ai_status || [])
const isDefaultOptions = computed(() => dict.value.ai_is_default || [])

const providerSearch = reactive({ name: '', type: null, status: null })
const providerList = ref([])
const providerLoading = ref(false)
const providerPagination = reactive({ pageNum: 1, totalPages: 1 })
const selectedProvider = ref(null)
const modelList = ref([])
const modelLoading = ref(false)
const providerFormRef = ref(null)
const modelFormRef = ref(null)

const providerSelectOptions = computed(() => providerList.value.map(p => ({ label: p.providerName, value: p.id })))

const uploadPrefix = import.meta.env.VITE_API_BASEURL || '/api'
const uploadHeaders = computed(() => {
  const token = localStorage.getItem('token') || ''
  return { Authorization: `Bearer ${token}` }
})

const providerModal = reactive({
  show: false,
  isEdit: false,
  saving: false,
  form: { providerName: '', providerType: null, logo: '', baseUrl: '', apiKey: '', status: '0', remark: '' },
})

const modelModal = reactive({
  show: false,
  isEdit: false,
  saving: false,
  form: { providerId: null, modelType: null, modelId: '', modelName: '', icon: '', maxTokens: null, sortOrder: 0, isDefault: '0', status: '0', description: '' },
})

function handleLogoUploadFinish({ event }) {
  try {
    const res = JSON.parse(event.target.response)
    if (res.code === 200 && res.data) {
      providerModal.form.logo = res.data.fileId || res.data.id || res.data
    }
    else {
      window.$message.error(res.msg || '上传失败')
    }
  }
  catch {
    window.$message.error('上传失败')
  }
  return false
}

function handleIconUploadFinish({ event }) {
  try {
    const res = JSON.parse(event.target.response)
    if (res.code === 200 && res.data) {
      modelModal.form.icon = res.data.fileId || res.data.id || res.data
    }
    else {
      window.$message.error(res.msg || '上传失败')
    }
  }
  catch {
    window.$message.error('上传失败')
  }
  return false
}

const providerRules = {
  providerName: [{ required: true, message: '请输入供应商名称', trigger: 'blur' }],
  providerType: [{ required: true, message: '请选择类型', trigger: 'change' }],
  baseUrl: [{ required: true, message: '请输入 Base URL', trigger: 'blur' }],
  apiKey: [{ required: true, message: '请输入 API Key', trigger: 'blur' }],
}

const modelRules = {
  providerId: [{ required: true, message: '请选择供应商', trigger: 'change' }],
  modelType: [{ required: true, message: '请选择模型类型', trigger: 'change' }],
  modelId: [{ required: true, message: '请输入模型标识', trigger: 'blur' }],
  modelName: [{ required: true, message: '请输入模型名称', trigger: 'blur' }],
}

const providerColumns = [
  {
    title: 'Logo',
    key: 'logo',
    width: 50,
    render(row) {
      if (row.logo) {
        return h(AuthImage, { src: row.logo, imgStyle: { width: '28px', height: '28px', borderRadius: '50%', objectFit: 'cover' } })
      }
      return h('div', {
        style: `width:28px;height:28px;border-radius:50%;background:linear-gradient(135deg,#4242F7,#6366F1);display:flex;align-items:center;justify-content:center;color:#fff;font-size:12px;font-weight:600`,
      }, (row.providerName || '?').charAt(0))
    },
  },
  { title: '供应商名称', key: 'providerName', width: 140 },
  {
    title: '类型',
    key: 'providerType',
    width: 110,
    render(row) { return h(DictTag, { dictType: 'ai_provider_type', value: row.providerType, size: 'small' }) },
  },
  { title: 'Base URL', key: 'baseUrl', ellipsis: { tooltip: true } },
  {
    title: '默认',
    key: 'isDefault',
    width: 70,
    align: 'center',
    render(row) {
      return h(NSwitch, {
        value: row.isDefault === '1',
        size: 'small',
        disabled: row.isDefault === '1',
        onUpdateValue: () => handleSetDefault(row),
      })
    },
  },
  {
    title: '状态',
    key: 'status',
    width: 70,
    align: 'center',
    render(row) { return h(DictTag, { dictType: 'ai_status', value: row.status, size: 'small' }) },
  },
  {
    title: '操作',
    key: 'actions',
    width: 200,
    fixed: 'right',
    render(row) {
      return h('div', { style: 'display:flex;gap:8px' }, [
        h(NButton, {
          text: true,
          type: 'primary',
          size: 'small',
          onClick: (e) => {
            e.stopPropagation()
            handleEditProvider(row)
          },
        }, { default: () => '编辑' }),
        h(NButton, {
          text: true,
          size: 'small',
          onClick: (e) => {
            e.stopPropagation()
            handleTestConnection(row)
          },
        }, { default: () => '测试' }),
        h(NPopconfirm, { onPositiveClick: () => handleDeleteProvider(row.id) }, {
          trigger: () => h(NButton, {
            text: true,
            type: 'error',
            size: 'small',
            onClick: e => e.stopPropagation(),
          }, { default: () => '删除' }),
          default: () => '确定删除该供应商吗？',
        }),
      ])
    },
  },
]

const modelColumns = [
  {
    title: '图标',
    key: 'icon',
    width: 50,
    render(row) {
      if (row.icon) {
        return h(AuthImage, { src: row.icon, imgStyle: { width: '28px', height: '28px', borderRadius: '50%', objectFit: 'cover' } })
      }
      return h('div', {
        style: `width:28px;height:28px;border-radius:50%;background:linear-gradient(135deg,#6EE7B7,#34D399);display:flex;align-items:center;justify-content:center;color:#fff;font-size:12px;font-weight:600`,
      }, (row.modelName || '?').charAt(0))
    },
  },
  { title: '模型名称', key: 'modelName', width: 140 },
  { title: '模型标识', key: 'modelId', width: 180 },
  {
    title: '类型',
    key: 'modelType',
    width: 90,
    render(row) { return h(DictTag, { dictType: 'ai_model_type', value: row.modelType, size: 'small' }) },
  },
  {
    title: '最大Token',
    key: 'maxTokens',
    width: 100,
    align: 'right',
    render(row) { return row.maxTokens ? row.maxTokens.toLocaleString() : '-' },
  },
  {
    title: '默认',
    key: 'isDefault',
    width: 60,
    align: 'center',
    render(row) { return h(DictTag, { dictType: 'ai_is_default', value: row.isDefault, size: 'small' }) },
  },
  {
    title: '状态',
    key: 'status',
    width: 60,
    align: 'center',
    render(row) { return h(DictTag, { dictType: 'ai_status', value: row.status, size: 'small' }) },
  },
  { title: '描述', key: 'description', ellipsis: { tooltip: true } },
  {
    title: '操作',
    key: 'actions',
    width: 120,
    fixed: 'right',
    render(row) {
      return h('div', { style: 'display:flex;gap:8px' }, [
        h(NButton, { text: true, type: 'primary', size: 'small', onClick: () => handleEditModel(row) }, { default: () => '编辑' }),
        h(NPopconfirm, { onPositiveClick: () => handleDeleteModel(row.id) }, {
          trigger: () => h(NButton, { text: true, type: 'error', size: 'small' }, { default: () => '删除' }),
          default: () => '确定删除该模型吗？',
        }),
      ])
    },
  },
]

function providerRowProps(row) {
  return {
    style: selectedProvider.value?.id === row.id ? 'background:rgba(66,66,247,0.06);cursor:pointer' : 'cursor:pointer',
    onClick: () => handleSelectProvider(row),
  }
}

async function loadProviders() {
  providerLoading.value = true
  try {
    const params = { pageNum: providerPagination.pageNum, pageSize: 20 }
    if (providerSearch.name)
      params.providerName = providerSearch.name
    if (providerSearch.type)
      params.providerType = providerSearch.type
    if (providerSearch.status)
      params.status = providerSearch.status
    const res = await fetchProviderPage(params)
    if (res.code === 200 && res.data) {
      providerList.value = res.data.records || []
      providerPagination.totalPages = Math.ceil((res.data.total || 0) / 20) || 1
    }
  }
  catch {}
  finally {
    providerLoading.value = false
  }
}

async function loadModels() {
  if (!selectedProvider.value) {
    modelList.value = []
    return
  }
  modelLoading.value = true
  try {
    const res = await fetchModelPage({ pageNum: 1, pageSize: 100, providerId: selectedProvider.value.id })
    if (res.code === 200 && res.data) {
      modelList.value = res.data.records || []
    }
  }
  catch {}
  finally {
    modelLoading.value = false
  }
}

function handleSelectProvider(row) {
  if (selectedProvider.value?.id === row.id) {
    selectedProvider.value = null
    modelList.value = []
  }
  else {
    selectedProvider.value = row
    loadModels()
  }
}

function handleAddProvider() {
  providerModal.isEdit = false
  providerModal.form = { providerName: '', providerType: null, logo: '', baseUrl: '', apiKey: '', status: '0', remark: '' }
  providerModal.show = true
}

async function handleEditProvider(row) {
  try {
    const res = await providerGetById(row.id)
    if (res.code === 200 && res.data) {
      providerModal.isEdit = true
      providerModal.form = { ...res.data }
      providerModal.show = true
    }
  }
  catch {}
}

async function handleSaveProvider() {
  try {
    await providerFormRef.value?.validate()
  }
  catch { return }
  providerModal.saving = true
  try {
    const res = providerModal.isEdit
      ? await providerUpdate(providerModal.form)
      : await providerAdd(providerModal.form)
    if (res.code === 200) {
      window.$message.success(providerModal.isEdit ? '更新成功' : '新增成功')
      providerModal.show = false
      loadProviders()
    }
    else {
      window.$message.error(res.msg || '操作失败')
    }
  }
  catch (e) {
    window.$message.error(e.message || '操作失败')
  }
  finally {
    providerModal.saving = false
  }
}

async function handleDeleteProvider(id) {
  try {
    const res = await providerDelete(id)
    if (res.code === 200) {
      window.$message.success('删除成功')
      if (selectedProvider.value?.id === id) {
        selectedProvider.value = null
        modelList.value = []
      }
      loadProviders()
    }
    else {
      window.$message.error(res.msg || '删除失败')
    }
  }
  catch (e) {
    window.$message.error(e.message || '删除失败')
  }
}

const testResult = reactive({ show: false, content: '', loading: false })

async function handleTestConnection(row) {
  testResult.loading = true
  testResult.show = true
  testResult.content = '正在连接...'
  try {
    const res = await providerTest({ baseUrl: row.baseUrl, apiKey: row.apiKey, defaultModel: row.defaultModel, providerName: row.providerName })
    if (res.code === 200) {
      testResult.content = typeof res.data === 'string' ? res.data : JSON.stringify(res.data, null, 2)
    }
    else {
      testResult.content = `连接失败: ${res.msg || '未知错误'}`
    }
  }
  catch (e) {
    testResult.content = `连接失败: ${e.message || '未知错误'}`
  }
  finally {
    testResult.loading = false
  }
}

async function handleSetDefault(row) {
  try {
    const res = await providerSetDefault(row.id)
    if (res.code === 200) {
      window.$message.success('设置成功')
      loadProviders()
    }
  }
  catch {
    window.$message.error('设置失败')
  }
}

function handleAddModel() {
  modelModal.isEdit = false
  modelModal.form = { providerId: selectedProvider.value?.id || null, modelType: null, modelId: '', modelName: '', icon: '', maxTokens: null, sortOrder: 0, isDefault: '0', status: '0', description: '' }
  modelModal.show = true
}

async function handleEditModel(row) {
  try {
    const res = await modelGetById(row.id)
    if (res.code === 200 && res.data) {
      modelModal.isEdit = true
      modelModal.form = { ...res.data }
      modelModal.show = true
    }
  }
  catch {}
}

async function handleSaveModel() {
  try {
    await modelFormRef.value?.validate()
  }
  catch { return }
  modelModal.saving = true
  try {
    const res = modelModal.isEdit
      ? await modelUpdate(modelModal.form)
      : await modelAdd(modelModal.form)
    if (res.code === 200) {
      window.$message.success(modelModal.isEdit ? '更新成功' : '新增成功')
      modelModal.show = false
      loadModels()
      loadProviders()
    }
    else {
      window.$message.error(res.msg || '操作失败')
    }
  }
  catch (e) {
    window.$message.error(e.message || '操作失败')
  }
  finally {
    modelModal.saving = false
  }
}

async function handleDeleteModel(id) {
  try {
    const res = await modelDelete(id)
    if (res.code === 200) {
      window.$message.success('删除成功')
      loadModels()
      loadProviders()
    }
    else {
      window.$message.error(res.msg || '删除失败')
    }
  }
  catch (e) {
    window.$message.error(e.message || '删除失败')
  }
}

onMounted(() => {
  loadProviders()
})
</script>

<style scoped>
.ai-provider-model-page {
  padding: 24px;
  min-height: 100%;
  overflow-y: auto;
}

.panel-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  border: 1px solid rgba(226, 232, 240, 0.6);
  overflow: hidden;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02);
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 24px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.5);
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.5) 0%, transparent 100%);
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
}

.panel-title i {
  font-size: 20px;
  color: #4242f7;
}

.search-bar {
  display: flex;
  gap: 12px;
  padding: 12px 24px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.5);
  align-items: center;
}

.search-bar .n-input {
  min-width: 160px;
  flex: 1;
  max-width: 240px;
}

.search-bar .n-select {
  min-width: 120px;
}

.provider-table :deep(.n-data-table-td),
.model-table :deep(.n-data-table-td) {
  padding: 8px 16px;
}

.provider-table :deep(.n-data-table-row:hover),
.model-table :deep(.n-data-table-row:hover) {
  background: rgba(66, 66, 247, 0.04) !important;
}

.provider-table :deep(.n-data-table-row.row--selected) {
  background: rgba(66, 66, 247, 0.06) !important;
}

.pagination-wrap {
  padding: 12px 24px;
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid rgba(226, 232, 240, 0.5);
}

.model-panel {
  margin-bottom: 20px;
}

.empty-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 40px;
  color: #94a3b8;
  font-size: 14px;
}

.dark .panel-card {
  background: rgba(30, 41, 59, 0.7);
  border-color: rgba(51, 65, 85, 0.5);
}

.dark .panel-title {
  color: #e2e8f0;
}

.dark .panel-title i {
  color: #6366f1;
}

.dark .panel-header {
  background: transparent;
  border-bottom-color: rgba(51, 65, 85, 0.4);
}

.dark .search-bar {
  border-bottom-color: rgba(51, 65, 85, 0.4);
}

.dark .provider-table :deep(.n-data-table-row:hover),
.dark .model-table :deep(.n-data-table-row:hover) {
  background: rgba(67, 56, 202, 0.15) !important;
}

.dark .pagination-wrap {
  border-top-color: rgba(51, 65, 85, 0.4);
}

.logo-preview {
  position: relative;
  display: inline-block;
}

.logo-remove {
  position: absolute;
  top: -6px;
  right: -6px;
}

.test-result-content {
  background: rgba(248, 250, 252, 0.8);
  border-radius: 8px;
  padding: 12px 16px;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
  margin: 0;
  max-height: 400px;
  overflow-y: auto;
}

.dark .test-result-content {
  background: rgba(15, 23, 42, 0.6);
}
</style>
