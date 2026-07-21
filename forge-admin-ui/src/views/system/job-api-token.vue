<template>
  <div class="job-api-token-page" :style="pageThemeStyle">
    <main class="token-content">
      <div class="token-toolbar">
        <NTooltip>
          <template #trigger>
            <NButton secondary circle aria-label="返回定时任务" @click="handleBack">
              <template #icon>
                <i class="i-material-symbols:arrow-back-rounded" />
              </template>
            </NButton>
          </template>
          返回定时任务
        </NTooltip>
        <NButton v-if="canCreate" type="primary" @click="handleOpenCreate">
          <template #icon>
            <i class="i-material-symbols:add-rounded" />
          </template>
          创建服务账号
        </NButton>
        <div class="toolbar-spacer" />
        <NButton secondary @click="usageVisible = true">
          <template #icon>
            <i class="i-material-symbols:terminal-rounded" />
          </template>
          调用说明
        </NButton>
        <NTooltip>
          <template #trigger>
            <NButton secondary circle aria-label="刷新服务账号" @click="handleRefresh">
              <template #icon>
                <i class="i-material-symbols:refresh-rounded" />
              </template>
            </NButton>
          </template>
          刷新服务账号
        </NTooltip>
      </div>
      <AiCrudPage
        ref="crudRef"
        :api-config="{ list: 'get@/job/api-token/page' }"
        :search-schema="searchSchema"
        :columns="tableColumns"
        row-key="id"
        :hide-add="true"
        :hide-toolbar="true"
        max-height="var(--token-table-max-height)"
        :search-y-gap="8"
      />
    </main>

    <NModal
      v-model:show="createVisible"
      preset="card"
      title="创建开放 API 服务账号"
      class="job-api-token-create-modal"
      :mask-closable="false"
      @after-leave="resetCreateForm"
    >
      <NForm
        ref="formRef"
        :model="form"
        :rules="formRules"
        label-placement="top"
      >
        <div class="form-grid">
          <NFormItem label="调用方名称" path="callerName">
            <NInput v-model:value="form.callerName" maxlength="100" show-count placeholder="例如：订单中台" />
          </NFormItem>
          <NFormItem label="过期时间" path="expiresAt">
            <NDatePicker
              v-model:value="form.expiresAt"
              type="datetime"
              clearable
              class="full-width"
              :is-date-disabled="isPastDate"
            />
          </NFormItem>
        </div>

        <NFormItem label="调用方说明" path="callerDescription">
          <NInput
            v-model:value="form.callerDescription"
            type="textarea"
            maxlength="500"
            show-count
            :autosize="{ minRows: 2, maxRows: 4 }"
            placeholder="记录系统归属、负责人或使用场景"
          />
        </NFormItem>

        <NFormItem label="授权范围" path="scopes">
          <NCheckboxGroup v-model:value="form.scopes" class="scope-options">
            <NCheckbox
              v-for="option in scopeOptions"
              :key="option.value"
              :value="option.value"
              :label="option.label"
            />
          </NCheckboxGroup>
        </NFormItem>

        <div class="resource-section">
          <div class="resource-heading">
            <strong>任务资源</strong>
            <span>指定任务与任务组取并集</span>
          </div>
          <div class="form-grid">
            <NFormItem label="指定任务">
              <NSelect
                v-model:value="form.jobIds"
                :options="jobOptions"
                :loading="resourceLoading"
                multiple
                filterable
                clearable
                max-tag-count="responsive"
                placeholder="选择可访问任务"
              />
            </NFormItem>
            <NFormItem label="任务组">
              <NSelect
                v-model:value="form.jobGroups"
                :options="groupOptions"
                :loading="resourceLoading"
                multiple
                filterable
                clearable
                max-tag-count="responsive"
                placeholder="选择可访问任务组"
              />
            </NFormItem>
          </div>
        </div>
      </NForm>

      <template #footer>
        <div class="modal-actions">
          <NButton :disabled="saving" @click="createVisible = false">
            取消
          </NButton>
          <NButton type="primary" :loading="saving" @click="handleCreate">
            <template #icon>
              <i class="i-material-symbols:key-outline-rounded" />
            </template>
            创建并生成 Token
          </NButton>
        </div>
      </template>
    </NModal>

    <NModal
      v-model:show="usageVisible"
      preset="card"
      title="开放 API 调用说明"
      class="job-api-token-usage-modal"
      :mask-closable="false"
    >
      <JobOpenApiUsageGuide />

      <template #footer>
        <div class="modal-actions">
          <NButton type="primary" @click="usageVisible = false">
            关闭
          </NButton>
        </div>
      </template>
    </NModal>

    <NModal
      v-model:show="issuedVisible"
      preset="card"
      :title="issuedToken?.title || 'Token 已生成'"
      class="job-api-token-issued-modal"
      :mask-closable="false"
      :closable="false"
      @after-leave="clearIssuedToken"
    >
      <NAlert type="warning" :show-icon="true">
        此 Token 仅展示一次。关闭前请完成保存，之后无法从系统再次查看。
      </NAlert>

      <div class="issued-token-block">
        <code>{{ issuedToken?.token }}</code>
        <NTooltip>
          <template #trigger>
            <NButton quaternary circle aria-label="复制 Token" @click="handleCopyToken">
              <template #icon>
                <i class="i-material-symbols:content-copy-outline-rounded" />
              </template>
            </NButton>
          </template>
          复制 Token
        </NTooltip>
      </div>

      <div class="issued-meta">
        <span>凭据前缀</span>
        <strong>{{ issuedToken?.tokenPrefix || '-' }}</strong>
        <span>过期时间</span>
        <strong>{{ formatDateTime(issuedToken?.expiresAt) }}</strong>
      </div>

      <JobOpenApiUsageGuide
        class="issued-usage-guide"
        :token="issuedToken?.token"
        :scopes="issuedToken?.scopes"
        compact
      />

      <template #footer>
        <div class="modal-actions">
          <NButton type="primary" @click="issuedVisible = false">
            我已安全保存
          </NButton>
        </div>
      </template>
    </NModal>
  </div>
</template>

<script setup>
import dayjs from 'dayjs'
import {
  NAlert,
  NButton,
  NCheckbox,
  NCheckboxGroup,
  NDatePicker,
  NForm,
  NFormItem,
  NInput,
  NModal,
  NSelect,
  NTooltip,
  useThemeVars,
} from 'naive-ui'
import { computed, h, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  createJobApiToken,
  getJobApiTokenResources,
  revokeJobApiToken,
  rotateJobApiToken,
} from '@/api/system/job'
import { AiCrudPage } from '@/components/ai-form'
import SystemTableCell from '@/components/common/SystemTableCell.vue'
import DictTag from '@/components/DictTag.vue'
import JobOpenApiUsageGuide from '@/components/job/JobOpenApiUsageGuide.vue'
import { useDict } from '@/composables'
import { useUserStore } from '@/store'
import { copy } from '@/utils/clipboard'
import {
  buildJobApiTokenPayload,
  createJobApiTokenForm,
  normalizeJobApiResources,
  summarizeJobApiResources,
} from './job-api-token.js'
import { hasJobPermission, JOB_PERMISSIONS } from './job-config/job-permission'

defineOptions({ name: 'JobApiToken' })

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const themeVars = useThemeVars()
const { dict } = useDict('sys_job_api_token_status', 'sys_job_api_scope')
const crudRef = ref(null)
const formRef = ref(null)
const createVisible = ref(false)
const issuedVisible = ref(false)
const usageVisible = ref(false)
const issuedToken = ref(null)
const saving = ref(false)
const resourceLoading = ref(false)
const jobOptions = ref([])
const groupOptions = ref([])
const form = reactive(createJobApiTokenForm())
const pageThemeStyle = computed(() => ({
  '--action-color': themeVars.value.actionColor,
  '--body-color': themeVars.value.bodyColor,
  '--border-color': themeVars.value.borderColor,
  '--card-color': themeVars.value.cardColor,
  '--divider-color': themeVars.value.dividerColor,
  '--error-color': themeVars.value.errorColor,
  '--primary-color': themeVars.value.primaryColor,
  '--success-color': themeVars.value.successColor,
  '--text-color-1': themeVars.value.textColor1,
  '--text-color-2': themeVars.value.textColor2,
  '--text-color-3': themeVars.value.textColor3,
}))

const statusOptions = computed(() => dict.value.sys_job_api_token_status || [])
const scopeOptions = computed(() => dict.value.sys_job_api_scope || [])
const canCreate = computed(() => hasJobPermission(userStore, route, JOB_PERMISSIONS.apiTokenAdd))
const canRevoke = computed(() => hasJobPermission(userStore, route, JOB_PERMISSIONS.apiTokenRevoke))
const canRotate = computed(() => hasJobPermission(userStore, route, JOB_PERMISSIONS.apiTokenRotate))

const formRules = {
  callerName: [{ required: true, message: '请输入调用方名称', trigger: ['blur', 'input'] }],
  scopes: [{ type: 'array', required: true, message: '至少选择一个授权范围', trigger: 'change' }],
  expiresAt: [{ type: 'number', required: true, message: '请选择过期时间', trigger: 'change' }],
}

const searchSchema = computed(() => [
  {
    field: 'callerName',
    label: '调用方',
    type: 'input',
    props: { placeholder: '搜索调用方名称' },
  },
  {
    field: 'status',
    label: '状态',
    type: 'select',
    props: {
      placeholder: '全部状态',
      options: statusOptions.value,
      clearable: true,
    },
  },
])

const tableColumns = computed(() => [
  {
    prop: 'callerName',
    label: '服务账号',
    minWidth: 210,
    render: row => h(SystemTableCell, {
      title: row.callerName,
      subtitle: row.callerDescription || '未填写调用方说明',
    }),
  },
  {
    prop: 'tokenPrefix',
    label: '凭据前缀',
    width: 220,
    render: row => h('code', { class: 'token-prefix' }, row.tokenPrefix || '-'),
  },
  {
    prop: 'scopes',
    label: 'Scope',
    minWidth: 210,
    render: row => h('div', { class: 'scope-list' }, (row.scopes || []).map(scope => h(DictTag, {
      key: scope,
      options: scopeOptions.value,
      value: scope,
      size: 'small',
      forceTag: true,
    }))),
  },
  {
    prop: 'resources',
    label: '任务资源',
    minWidth: 170,
    render: row => h('div', { class: 'resource-cell' }, [
      h('strong', summarizeJobApiResources(row)),
      h('span', formatResourceDetail(row)),
    ]),
  },
  {
    prop: 'status',
    label: '状态',
    width: 110,
    render: row => h(DictTag, {
      options: statusOptions.value,
      value: row.status,
      size: 'small',
      forceTag: true,
    }),
  },
  {
    prop: 'expiresAt',
    label: '有效期',
    width: 170,
    render: row => h('div', { class: 'time-cell' }, [
      h('strong', formatDateTime(row.expiresAt)),
      h('span', row.lastUsedAt ? `最近调用 ${formatDateTime(row.lastUsedAt)}` : '尚未调用'),
    ]),
  },
  {
    prop: 'action',
    label: '操作',
    width: 150,
    fixed: 'right',
    maxActionButtons: 2,
    actions: [
      {
        label: '轮换',
        key: 'rotate',
        type: 'warning',
        onClick: handleRotate,
        visible: row => canRotate.value && row.status === 'ACTIVE',
      },
      {
        label: '吊销',
        key: 'revoke',
        type: 'error',
        onClick: handleRevoke,
        visible: row => canRevoke.value && row.status === 'ACTIVE',
      },
    ],
  },
])

function handleBack() {
  router.push('/system/job-config')
}

function handleRefresh() {
  crudRef.value?.refresh()
  loadResourceOptions()
}

async function handleOpenCreate() {
  if (!canCreate.value)
    return
  if (!jobOptions.value.length)
    await loadResourceOptions()
  createVisible.value = true
}

async function handleCreate() {
  try {
    await formRef.value?.validate()
  }
  catch {
    return
  }
  if (!form.jobIds.length && !form.jobGroups.length) {
    window.$message.warning('至少选择一个指定任务或任务组')
    return
  }
  if (!form.expiresAt || form.expiresAt <= Date.now()) {
    window.$message.warning('Token 过期时间必须晚于当前时间')
    return
  }
  saving.value = true
  try {
    const payload = buildJobApiTokenPayload(form)
    const response = await createJobApiToken(payload)
    createVisible.value = false
    showIssuedToken(response.data, '服务账号已创建', payload.scopes)
    crudRef.value?.refresh()
  }
  finally {
    saving.value = false
  }
}

function handleRevoke(row) {
  if (!canRevoke.value)
    return
  window.$dialog.warning({
    title: '吊销服务账号',
    content: `吊销“${row.callerName}”后，当前 Token 将立即失效。`,
    positiveText: '吊销',
    negativeText: '取消',
    onPositiveClick: async () => {
      await revokeJobApiToken(row.id)
      window.$message.success('服务账号已吊销')
      crudRef.value?.refresh()
    },
  })
}

function handleRotate(row) {
  if (!canRotate.value)
    return
  window.$dialog.warning({
    title: '轮换 Token',
    content: `轮换“${row.callerName}”后，旧 Token 将立即失效。`,
    positiveText: '确认轮换',
    negativeText: '取消',
    onPositiveClick: async () => {
      const response = await rotateJobApiToken(row.id)
      showIssuedToken(response.data, 'Token 已轮换', row.scopes)
      crudRef.value?.refresh()
    },
  })
}

async function loadResourceOptions() {
  resourceLoading.value = true
  try {
    const response = await getJobApiTokenResources()
    const normalized = normalizeJobApiResources(response.data)
    jobOptions.value = normalized.jobOptions
    groupOptions.value = normalized.groupOptions
  }
  catch {
    jobOptions.value = []
    groupOptions.value = []
  }
  finally {
    resourceLoading.value = false
  }
}

function showIssuedToken(token, title, scopes) {
  issuedToken.value = token ? { ...token, title, scopes: [...(scopes || [])] } : null
  issuedVisible.value = Boolean(token?.token)
}

function clearIssuedToken() {
  issuedToken.value = null
}

function resetCreateForm() {
  Object.assign(form, createJobApiTokenForm())
  formRef.value?.restoreValidation?.()
}

function handleCopyToken() {
  if (issuedToken.value?.token)
    copy(issuedToken.value.token, 'Token 已复制')
}

function formatResourceDetail(row) {
  const groups = Array.isArray(row.jobGroups) ? row.jobGroups : []
  if (groups.length)
    return groups.join('、')
  const ids = Array.isArray(row.jobIds) ? row.jobIds : []
  return ids.length ? `任务 ID：${ids.join('、')}` : '-'
}

function formatDateTime(value) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-'
}

function isPastDate(timestamp) {
  return timestamp < dayjs().startOf('day').valueOf()
}

onMounted(loadResourceOptions)
</script>

<style scoped>
.job-api-token-page {
  --token-table-max-height: calc(100vh - 150px);
  height: 100%;
  min-height: 0;
  padding: 14px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.token-content {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  padding: 10px 12px;
  display: flex;
  flex-direction: column;
  background: var(--card-color);
  border: 1px solid var(--border-color);
  border-radius: 8px;
}

.token-toolbar {
  flex: 0 0 auto;
  min-height: 42px;
  padding: 0 4px 9px;
  display: flex;
  align-items: center;
  gap: 8px;
  border-bottom: 1px solid var(--divider-color);
}

.toolbar-spacer {
  flex: 1;
}

.token-content :deep(.ai-crud-page),
.token-content :deep(.ai-crud-main) {
  min-height: 0;
}

.token-content :deep(.ai-crud-page) {
  flex: 1;
  height: auto;
}

.token-content :deep(.ai-crud-main) {
  height: 100%;
}

.token-content :deep(.ai-search-box) {
  padding: 10px 12px 4px;
}

.token-content :deep(.token-prefix),
.issued-token-block code {
  overflow-wrap: anywhere;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  letter-spacing: 0;
}

.token-content :deep(.token-prefix) {
  color: var(--text-color-1);
  font-size: 12px;
}

.token-content :deep(.scope-list) {
  min-width: 0;
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
}

.token-content :deep(.resource-cell),
.token-content :deep(.time-cell) {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.token-content :deep(.resource-cell strong),
.token-content :deep(.time-cell strong) {
  overflow: hidden;
  color: var(--text-color-1);
  font-size: 12px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.token-content :deep(.resource-cell span),
.token-content :deep(.time-cell span) {
  overflow: hidden;
  color: var(--text-color-3);
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.full-width {
  width: 100%;
}

.scope-options {
  min-height: 34px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px 18px;
}

.resource-section {
  padding: 14px 16px 2px;
  background: var(--action-color);
  border: 1px solid var(--border-color);
  border-radius: 7px;
}

.resource-heading {
  margin-bottom: 10px;
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}

.resource-heading strong {
  color: var(--text-color-1);
  font-size: 13px;
}

.resource-heading span {
  color: var(--text-color-3);
  font-size: 12px;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.issued-token-block {
  margin-top: 16px;
  padding: 12px 10px 12px 14px;
  display: flex;
  align-items: center;
  gap: 10px;
  background: var(--action-color);
  border: 1px solid var(--border-color);
  border-radius: 7px;
}

.issued-token-block code {
  min-width: 0;
  flex: 1;
  color: var(--text-color-1);
  font-size: 12px;
  line-height: 1.6;
  user-select: all;
}

.issued-meta {
  margin-top: 14px;
  display: grid;
  grid-template-columns: 84px minmax(0, 1fr);
  gap: 8px 12px;
  font-size: 12px;
}

.issued-meta span {
  color: var(--text-color-3);
}

.issued-meta strong {
  min-width: 0;
  overflow-wrap: anywhere;
  color: var(--text-color-1);
  font-weight: 600;
}

.issued-usage-guide {
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px solid var(--divider-color);
}

:global(.job-api-token-create-modal) {
  width: min(720px, 92vw);
}

:global(.job-api-token-issued-modal) {
  width: min(860px, 94vw);
  max-height: calc(100vh - 48px);
}

:global(.job-api-token-usage-modal) {
  width: min(820px, 94vw);
  max-height: calc(100vh - 48px);
}

:global(.job-api-token-issued-modal .n-card__content),
:global(.job-api-token-usage-modal .n-card__content) {
  min-height: 0;
  overflow-y: auto;
}

@media (max-width: 768px) {
  .job-api-token-page {
    --token-table-max-height: calc(100vh - 148px);
    padding: 10px;
  }

  .token-content {
    padding: 8px;
  }

  .token-toolbar {
    overflow-x: auto;
  }

  .form-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .resource-heading {
    align-items: flex-start;
    flex-direction: column;
    gap: 2px;
  }
}
</style>
