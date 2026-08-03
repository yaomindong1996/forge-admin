<template>
  <div class="capability-grant-page">
    <AiCrudPage
      ref="crudRef"
      :api-config="{
        list: 'get@/ai/capability/grant/page',
      }"
      :search-schema="searchSchema"
      :columns="tableColumns"
      row-key="id"
      :hide-add="true"
      :hide-selection="true"
      :hide-batch-delete="true"
    >
      <template #toolbar-start>
        <n-button v-if="canAdd" type="primary" @click="openAddModal">
          新增授权
        </n-button>
      </template>
    </AiCrudPage>

    <!-- 新增授权弹窗 -->
    <n-modal
      v-model:show="addVisible"
      :title="grantModalTitle"
      preset="card"
      style="width: 600px"
    >
      <template #header-extra>
        <n-tooltip trigger="hover">
          <template #trigger>
            <n-button quaternary circle :loading="optionsLoading || dictLoading" aria-label="刷新授权候选项" @click="refreshGrantOptions(true)">
              <template #icon>
                <i class="i-material-symbols:refresh-rounded" />
              </template>
            </n-button>
          </template>
          刷新候选项
        </n-tooltip>
      </template>

      <n-alert v-if="optionsError" type="error" class="option-alert">
        {{ optionsError }}
      </n-alert>
      <n-alert v-else-if="!versionStrategyOptions.length" type="error" class="option-alert">
        版本策略字典尚未初始化，请先确认能力控制面数据库迁移已成功执行。
      </n-alert>
      <n-alert v-else-if="!clientSelectOptions.length" type="warning" class="option-alert">
        暂无可授权的机器客户端，请先创建并启用客户端。
      </n-alert>
      <n-alert v-else-if="!availableCapabilityCount" type="warning" class="option-alert">
        暂无可授权能力，请先在能力目录注册并发布非 HIGH 风险能力。
      </n-alert>
      <n-alert
        v-else-if="selectedCapability?.sourceType === 'FLOW_ACTION' && !flowOperationDictionaryReady"
        type="error"
        class="option-alert"
      >
        流程操作字典加载失败，请点击右上角刷新后再授权。
      </n-alert>

      <n-form
        ref="addFormRef"
        :model="addForm"
        :rules="addRules"
        label-placement="left"
        label-width="110px"
      >
        <n-form-item label="机器客户端" path="clientId">
          <n-select
            v-model:value="addForm.clientId"
            placeholder="请选择客户端"
            :options="clientSelectOptions"
            :loading="optionsLoading"
            :disabled="editingGrantId !== null || optionsLoading || clientSelectOptions.length === 0"
            filterable
          >
            <template #empty>
              <n-empty size="small" description="暂无启用且未过期的客户端" />
            </template>
          </n-select>
        </n-form-item>
        <n-form-item label="能力" path="capabilityId">
          <n-select
            v-model:value="addForm.capabilityId"
            placeholder="请选择能力（HIGH 风险能力不可授权）"
            :options="capabilitySelectOptions"
            :loading="optionsLoading"
            :disabled="editingGrantId !== null || optionsLoading || !versionStrategyOptions.length || capabilitySelectOptions.length === 0"
            filterable
            @update:value="handleCapabilityChange"
          >
            <template #empty>
              <n-empty size="small" description="暂无已发布能力" />
            </template>
          </n-select>
        </n-form-item>
        <n-form-item label="版本策略" path="versionStrategy">
          <n-select
            v-model:value="addForm.versionStrategy"
            placeholder="请选择版本策略"
            :options="versionStrategyOptions"
          />
        </n-form-item>
        <n-form-item :label="addForm.versionStrategy === 'FOLLOW_MAJOR' ? '基准版本' : '固定版本'" path="fixedVersion">
          <div class="version-field">
            <n-input
              v-model:value="addForm.fixedVersion"
              placeholder="如 1.0.0（授权时锚定的能力版本）"
            />
            <n-button
              v-if="selectedCapability?.currentVersion"
              secondary
              type="primary"
              @click="useCurrentCapabilityVersion"
            >
              使用当前 v{{ selectedCapability.currentVersion }}
            </n-button>
          </div>
        </n-form-item>
        <n-alert v-if="addForm.versionStrategy === 'FOLLOW_MAJOR'" type="info" class="policy-alert">
          跟随主版本会在基准版本所在主版本内自动使用最新版本，例如 1.0.0 自动跟随 1.x，不会跨到 2.x。
        </n-alert>
        <n-form-item
          v-if="selectedCapability?.sourceType === 'BUSINESS_ACTION'"
          label="允许字段"
          path="allowedFields"
        >
          <n-select
            v-model:value="addForm.allowedFields"
            :options="grantFieldOptions"
            placeholder="请选择客户端可以写入的字段"
            multiple
            filterable
            clearable
          />
        </n-form-item>
        <template v-else-if="selectedCapability?.sourceType === 'FLOW_ACTION'">
          <n-form-item label="允许操作" path="allowedOperations">
            <n-select
              v-model:value="addForm.allowedOperations"
              :options="grantOperationOptions"
              :loading="dictLoading"
              :disabled="!flowOperationDictionaryReady"
              placeholder="请选择客户端可以执行的流程操作"
              multiple
              clearable
            />
          </n-form-item>
          <n-form-item
            v-if="selectedCapability.allowedFields?.length"
            label="允许申请字段"
            path="allowedFields"
          >
            <n-select
              v-model:value="addForm.allowedFields"
              :options="grantFieldOptions"
              placeholder="可按客户端进一步收窄申请字段，必填字段不能移除"
              multiple
              filterable
              clearable
            />
          </n-form-item>
        </template>
        <n-alert v-else-if="selectedCapability?.sourceType === 'SYSTEM_SERVICE'" type="info" class="policy-alert">
          系统服务的入参、流程模型和变量白名单已经固化在发布版本中，授权时不能再次放宽。
        </n-alert>
        <n-alert v-else-if="selectedCapability" type="info" class="policy-alert">
          该能力无需配置额外字段策略。
        </n-alert>
        <n-alert
          v-if="selectedCapability?.requiredActorType === 'USER'"
          type="warning"
          class="policy-alert"
        >
          此能力仅接受用户委托 Token，不能使用机器签名或服务账号 Token 调用。
        </n-alert>
        <n-form-item label="过期时间" path="expiresAt">
          <n-date-picker
            v-model:value="addForm.expiresAt"
            type="datetime"
            placeholder="不填则长期有效"
            clearable
            class="w-full"
          />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="addVisible = false">
            取消
          </n-button>
          <n-button
            type="primary"
            :loading="addLoading"
            :disabled="optionsLoading || dictLoading || !!optionsError || !versionStrategyOptions.length || !clientSelectOptions.length || !availableCapabilityCount || !flowOperationDictionaryReady"
            @click="handleAddSubmit"
          >
            {{ editingGrantId === null ? '授权' : '保存修改' }}
          </n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { computed, h, onMounted, reactive, ref, watch } from 'vue'
import {
  addCapabilityGrant,
  getCapabilityGrantOptions,
  revokeCapabilityGrant,
  updateCapabilityGrant,
} from '@/api/ai/capability'
import { AiCrudPage } from '@/components/ai-form'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables'
import { useUserStore } from '@/store'
import { formatDateTime } from '@/utils'

defineOptions({ name: 'CapabilityGrant' })

const userStore = useUserStore()
const { dict, errors: dictErrors, loading: dictLoading, reload: reloadGrantDicts } = useDict(
  'ai_capability_grant_status',
  'ai_capability_version_strategy',
  'ai_capability_flow_operation',
)

const grantStatusOptions = computed(() => dict.value.ai_capability_grant_status || [])
const versionStrategyOptions = computed(() => dict.value.ai_capability_version_strategy || [])
const flowOperationLabelMap = computed(() => Object.fromEntries(
  (dict.value.ai_capability_flow_operation || [])
    .map(option => [option.value, option.label]),
))

function hasPermission(permission) {
  if (userStore?.isAdmin)
    return true
  const permissions = Array.isArray(userStore?.permissions) ? userStore.permissions : []
  return permissions.includes(permission) || permissions.includes('*:*:*')
}

const canAdd = computed(() => hasPermission('ai:capability:grant:add'))
const canEdit = computed(() => canAdd.value)
const canRevoke = computed(() => hasPermission('ai:capability:grant:revoke'))

const crudRef = ref(null)
const optionsLoading = ref(false)
const optionsError = ref('')

// ===== 客户端 / 能力选项（用于下拉与 id→名称回显） =====
const clientList = ref([])
const capabilityList = ref([])

const clientNameMap = computed(() => {
  const map = {}
  clientList.value.forEach((item) => {
    map[item.id] = `${item.clientName}（${item.clientCode}）`
  })
  return map
})

const capabilityNameMap = computed(() => {
  const map = {}
  capabilityList.value.forEach((item) => {
    map[item.id] = `${item.capabilityName}（${item.capabilityCode}）`
  })
  return map
})

const clientSelectOptions = computed(() => clientList.value
  .filter(item => item.status === 'ENABLED' && !isExpired(item.expiresAt))
  .map(item => ({
    label: `${item.clientName}（${item.clientCode}）`,
    value: item.id,
  })))

// HIGH 风险能力禁选（spec 决策：一期 HIGH 风险不开放授权）
const capabilitySelectOptions = computed(() => capabilityList.value
  .filter(item => item.publishStatus === 'PUBLISHED' && item.enabled !== 0)
  .map((item) => {
    const unavailableReason = getCapabilityUnavailableReason(item)
    return {
      label: unavailableReason
        ? `${item.capabilityName}（${item.capabilityCode}）- ${unavailableReason}`
        : `${item.capabilityName}（${item.capabilityCode}）· v${item.currentVersion || '-'}`,
      value: item.id,
      disabled: !!unavailableReason,
    }
  }))

const availableCapabilityCount = computed(() => capabilitySelectOptions.value
  .filter(item => !item.disabled)
  .length)

async function loadSelectOptions() {
  optionsLoading.value = true
  optionsError.value = ''
  try {
    const res = await getCapabilityGrantOptions()
    clientList.value = res.data?.clients || []
    capabilityList.value = res.data?.capabilities || []
  }
  catch (error) {
    clientList.value = []
    capabilityList.value = []
    optionsError.value = error?.message || '授权候选项加载失败'
  }
  finally {
    optionsLoading.value = false
  }
}

onMounted(loadSelectOptions)

// ===== 新增授权 =====
const addVisible = ref(false)
const addLoading = ref(false)
const editingGrantId = ref(null)
const addFormRef = ref(null)
const addForm = reactive({
  clientId: null,
  capabilityId: null,
  versionStrategy: null,
  fixedVersion: '',
  allowedFields: [],
  allowedOperations: [],
  expiresAt: null,
})

const selectedCapability = computed(() => capabilityList.value
  .find(item => item.id === addForm.capabilityId))

const grantModalTitle = computed(() => editingGrantId.value === null
  ? '新增能力授权'
  : '修改能力授权')

const grantFieldOptions = computed(() => (selectedCapability.value?.allowedFields || [])
  .map(field => ({
    label: selectedCapability.value?.requiredFields?.includes(field)
      ? `${field}（必填）`
      : field,
    value: field,
    disabled: selectedCapability.value?.requiredFields?.includes(field),
  })))

const grantOperationOptions = computed(() => (selectedCapability.value?.allowedOperations || [])
  .map(operation => ({
    label: flowOperationLabelMap.value[operation] || operation,
    value: operation,
  })))

const flowOperationDictionaryReady = computed(() => {
  if (selectedCapability.value?.sourceType !== 'FLOW_ACTION')
    return true
  const operations = selectedCapability.value.allowedOperations || []
  return operations.length > 0
    && operations.every(operation => !!flowOperationLabelMap.value[operation])
})

async function refreshGrantOptions(showSuccess = false) {
  await Promise.all([
    loadSelectOptions(),
    reloadGrantDicts(),
  ])
  if (showSuccess && !optionsError.value && Object.keys(dictErrors.value).length === 0)
    window.$message.success('候选项已刷新')
}

const addRules = {
  clientId: selectedIdRule('请选择客户端'),
  capabilityId: selectedIdRule('请选择能力'),
  versionStrategy: { required: true, message: '请选择版本策略', trigger: 'change' },
  fixedVersion: { required: true, message: '请输入固定版本', trigger: 'blur' },
  allowedFields: {
    trigger: 'change',
    validator: () => !requiresGrantFieldPolicy()
      || addForm.allowedFields.length > 0
      ? true
      : new Error('请至少选择一个允许字段'),
  },
  allowedOperations: {
    trigger: 'change',
    validator: () => {
      if (selectedCapability.value?.sourceType !== 'FLOW_ACTION')
        return true
      if (!flowOperationDictionaryReady.value)
        return new Error('流程操作字典未正确加载')
      return addForm.allowedOperations.length > 0
        ? true
        : new Error('请至少选择一个允许操作')
    },
  },
}

function selectedIdRule(message) {
  return {
    trigger: 'change',
    validator: (_rule, value) => isPositiveId(value) ? true : new Error(message),
  }
}

function requiresGrantFieldPolicy() {
  const capability = selectedCapability.value
  return capability?.sourceType === 'BUSINESS_ACTION'
    || (capability?.sourceType === 'FLOW_ACTION' && capability.allowedFields?.length > 0)
}

function isPositiveId(value) {
  if (typeof value === 'number')
    return Number.isInteger(value) && value > 0
  return typeof value === 'string' && /^[1-9]\d*$/.test(value)
}

watch(versionStrategyOptions, (options) => {
  if (addForm.versionStrategy || options.length === 0)
    return
  addForm.versionStrategy = resolveDefaultVersionStrategy()
})

watch(flowOperationDictionaryReady, (ready, wasReady) => {
  if (!ready || wasReady || selectedCapability.value?.sourceType !== 'FLOW_ACTION')
    return
  addForm.allowedOperations = [...(selectedCapability.value.allowedOperations || [])]
})

watch(() => addForm.allowedFields, (fields) => {
  if (selectedCapability.value?.sourceType !== 'FLOW_ACTION')
    return
  const requiredFields = selectedCapability.value.requiredFields || []
  const next = [...new Set([...fields, ...requiredFields])]
  if (next.length !== fields.length || next.some((field, index) => field !== fields[index]))
    addForm.allowedFields = next
}, { deep: true })

async function openAddModal() {
  editingGrantId.value = null
  Object.assign(addForm, {
    clientId: null,
    capabilityId: null,
    versionStrategy: resolveDefaultVersionStrategy(),
    fixedVersion: '',
    allowedFields: [],
    allowedOperations: [],
    expiresAt: null,
  })
  addVisible.value = true
  await refreshGrantOptions()
}

async function openEditModal(row) {
  editingGrantId.value = row.id
  addVisible.value = true
  await refreshGrantOptions()
  const fieldPolicy = parseFieldPolicy(row.fieldPolicy)
  Object.assign(addForm, {
    clientId: row.clientId,
    capabilityId: row.capabilityId,
    versionStrategy: row.versionStrategy,
    fixedVersion: row.fixedVersion || '',
    allowedFields: Array.isArray(fieldPolicy.allowedFields) ? [...fieldPolicy.allowedFields] : [],
    allowedOperations: Array.isArray(fieldPolicy.allowedOperations) ? [...fieldPolicy.allowedOperations] : [],
    expiresAt: parseDateTimeValue(row.expiresAt),
  })
}

function resolveDefaultVersionStrategy() {
  const options = versionStrategyOptions.value
  return (options.find(item => item.isDefault === 'Y') || options[0])?.value || null
}

function handleCapabilityChange(capabilityId) {
  const capability = capabilityList.value.find(item => item.id === capabilityId)
  addForm.fixedVersion = capability?.currentVersion || ''
  addForm.allowedFields = [...(capability?.allowedFields || [])]
  const operations = capability?.allowedOperations || []
  const operationsTranslated = operations.every(operation => !!flowOperationLabelMap.value[operation])
  addForm.allowedOperations = operationsTranslated ? [...operations] : []
}

function useCurrentCapabilityVersion() {
  addForm.fixedVersion = selectedCapability.value?.currentVersion || addForm.fixedVersion
}

function parseFieldPolicy(value) {
  if (value && typeof value === 'object')
    return value
  if (typeof value !== 'string' || !value.trim())
    return {}
  try {
    const parsed = JSON.parse(value)
    return parsed && typeof parsed === 'object' ? parsed : {}
  }
  catch {
    return {}
  }
}

function parseDateTimeValue(value) {
  if (!value)
    return null
  const timestamp = new Date(String(value).replace(' ', 'T')).getTime()
  return Number.isFinite(timestamp) ? timestamp : null
}

function getCapabilityUnavailableReason(capability) {
  if (capability.riskLevel === 'HIGH')
    return 'HIGH 风险不可授权'
  if (capability.behavior === 'READ_ONLY')
    return ''
  if (capability.sourceType === 'BUSINESS_ACTION' && capability.behavior === 'ACTION')
    return capability.allowedFields?.length ? '' : '缺少字段白名单'
  if (capability.sourceType === 'FLOW_ACTION' && capability.behavior === 'FLOW')
    return capability.allowedOperations?.length
      && (!capability.allowedOperations.includes('SUBMIT') || capability.allowedFields?.length)
      ? ''
      : '缺少操作或申请字段白名单'
  if (capability.sourceType === 'SYSTEM_SERVICE' && capability.behavior === 'ACTION')
    return ''
  return '当前类型不可授权'
}

function buildFieldPolicy() {
  if (selectedCapability.value?.sourceType === 'BUSINESS_ACTION')
    return { allowedFields: addForm.allowedFields }
  if (selectedCapability.value?.sourceType === 'FLOW_ACTION')
    return {
      allowedOperations: addForm.allowedOperations,
      ...(selectedCapability.value.allowedFields?.length
        ? { allowedFields: addForm.allowedFields }
        : {}),
    }
  return null
}

function isExpired(expiresAt) {
  if (!expiresAt)
    return false
  const timestamp = new Date(String(expiresAt).replace(' ', 'T')).getTime()
  return Number.isFinite(timestamp) && timestamp <= Date.now()
}

async function handleAddSubmit() {
  try {
    await addFormRef.value?.validate()
  }
  catch {
    return
  }
  addLoading.value = true
  try {
    const payload = {
      versionStrategy: addForm.versionStrategy,
      fixedVersion: addForm.fixedVersion,
      fieldPolicy: buildFieldPolicy(),
      expiresAt: addForm.expiresAt ? formatDateTime(addForm.expiresAt) : null,
    }
    const res = editingGrantId.value === null
      ? await addCapabilityGrant({
          clientId: addForm.clientId,
          capabilityId: addForm.capabilityId,
          ...payload,
        })
      : await updateCapabilityGrant(editingGrantId.value, payload)
    if (res.code === 200) {
      window.$message.success(editingGrantId.value === null ? '授权成功' : '授权已更新')
      addVisible.value = false
      crudRef.value?.refresh()
    }
  }
  finally {
    addLoading.value = false
  }
}

// ===== 撤销授权 =====
function handleRevoke(row) {
  const clientName = clientNameMap.value[row.clientId] || row.clientId
  const capabilityName = capabilityNameMap.value[row.capabilityId] || row.capabilityId
  window.$dialog.warning({
    title: '撤销授权确认',
    content: `确定撤销「${clientName}」对「${capabilityName}」的授权吗？撤销后该客户端将无法调用此能力。`,
    positiveText: '确定撤销',
    negativeText: '取消',
    onPositiveClick: async () => {
      const res = await revokeCapabilityGrant(row.id)
      if (res.code === 200) {
        window.$message.success('授权已撤销')
        crudRef.value?.refresh()
      }
    },
  })
}

// ===== 搜索与表格 =====
const searchSchema = computed(() => [
  {
    field: 'clientId',
    label: '客户端',
    type: 'select',
    props: {
      placeholder: '请选择客户端',
      clearable: true,
      filterable: true,
      options: clientList.value.map(item => ({
        label: `${item.clientName}（${item.clientCode}）`,
        value: item.id,
      })),
    },
  },
  {
    field: 'capabilityId',
    label: '能力',
    type: 'select',
    props: {
      placeholder: '请选择能力',
      clearable: true,
      filterable: true,
      options: capabilityList.value.map(item => ({
        label: `${item.capabilityName}（${item.capabilityCode}）`,
        value: item.id,
      })),
    },
  },
  {
    field: 'status',
    label: '状态',
    type: 'select',
    props: {
      placeholder: '请选择状态',
      clearable: true,
      options: grantStatusOptions.value,
    },
  },
])

const tableColumns = computed(() => [
  {
    prop: 'clientId',
    label: '机器客户端',
    minWidth: 180,
    ellipsis: { tooltip: true },
    render: row => clientNameMap.value[row.clientId] || row.clientId,
  },
  {
    prop: 'capabilityId',
    label: '能力',
    minWidth: 200,
    ellipsis: { tooltip: true },
    render: row => capabilityNameMap.value[row.capabilityId] || row.capabilityId,
  },
  {
    prop: 'versionStrategy',
    label: '版本策略',
    width: 120,
    render: (row) => {
      return h(DictTag, {
        options: versionStrategyOptions.value,
        value: row.versionStrategy,
        size: 'small',
      })
    },
  },
  {
    prop: 'fixedVersion',
    label: '固定版本',
    width: 100,
    render: row => row.fixedVersion || '-',
  },
  {
    prop: 'status',
    label: '状态',
    width: 90,
    render: (row) => {
      return h(DictTag, {
        options: grantStatusOptions.value,
        value: row.status,
        size: 'small',
      })
    },
  },
  {
    prop: 'expiresAt',
    label: '过期时间',
    width: 160,
    render: row => row.expiresAt || '长期有效',
  },
  {
    prop: 'createTime',
    label: '授权时间',
    width: 160,
  },
  {
    prop: 'action',
    label: '操作',
    width: 160,
    fixed: 'right',
    actions: [
      {
        label: '修改',
        key: 'edit',
        type: 'primary',
        onClick: openEditModal,
        visible: row => canEdit.value && row.status === 'ENABLED',
      },
      {
        label: '撤销',
        key: 'revoke',
        type: 'error',
        onClick: handleRevoke,
        visible: row => canRevoke.value && row.status === 'ENABLED',
      },
    ],
  },
])
</script>

<style scoped>
.capability-grant-page {
  height: 100%;
}

.w-full {
  width: 100%;
}

.version-field {
  display: flex;
  width: 100%;
  gap: 8px;
}

.option-alert {
  margin-bottom: 18px;
}

.policy-alert {
  margin-bottom: 18px;
}
</style>
