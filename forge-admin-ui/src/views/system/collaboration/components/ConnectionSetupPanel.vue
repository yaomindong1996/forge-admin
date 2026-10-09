<template>
  <section v-if="visible" class="connection-workspace">
    <header class="workspace-heading">
      <n-button size="small" :disabled="appSubmitLoading || bindSubmitLoading" @click="visible = false">
        ← 返回连接
      </n-button>
      <div class="workspace-heading__title">
        <strong>{{ detail?.connection?.connectionName || '连接配置' }}</strong><span>平台应用与能力绑定</span>
      </div>
      <n-button v-if="canManage && detail" size="small" secondary @click="handleEditConnection">
        编辑基础信息
      </n-button>
    </header>
    <div class="workspace-body">
      <n-alert v-if="detailError" type="error" class="setup-hint">
        {{ detailError }} <n-button text @click="loadDetail(connectionId)">
          重新加载
        </n-button>
      </n-alert>
      <n-spin :show="detailLoading">
        <div v-if="detail" class="setup-panel">
          <div class="connection-summary">
            <DictTag dict-type="sys_collab_platform" :value="detail.connection?.platform" size="small" />
            <span class="mono-text">{{ detail.connection?.connectionCode }}</span>
            <span>企业标识 · {{ detail.connection?.enterpriseId || '不适用' }}</span>
            <DictTag dict-type="sys_normal_disable" :value="String(detail.connection?.status ?? '')" size="small" />
          </div>

          <n-alert v-if="manageMode" :type="setupHint.type" :show-icon="false" size="small" class="setup-hint">
            {{ setupHint.message }}
          </n-alert>

          <n-descriptions v-if="!manageMode" bordered :column="3" size="small">
            <n-descriptions-item label="平台">
              <DictTag dict-type="sys_collab_platform" :value="detail.connection?.platform" size="small" />
            </n-descriptions-item>
            <n-descriptions-item label="连接名称">
              {{ detail.connection?.connectionName || '-' }}
            </n-descriptions-item>
            <n-descriptions-item label="连接标识">
              <span class="mono-text">{{ detail.connection?.connectionCode || '-' }}</span>
            </n-descriptions-item>
            <n-descriptions-item label="企业标识">
              {{ detail.connection?.enterpriseId || '不适用' }}
            </n-descriptions-item>
            <n-descriptions-item label="身份匹配">
              <DictTag dict-type="sys_collab_identity_policy" :value="detail.connection?.identityPolicy" size="small" />
            </n-descriptions-item>
            <n-descriptions-item label="通讯录同步">
              <DictTag dict-type="sys_collab_directory_authority" :value="detail.connection?.directoryAuthority" size="small" />
            </n-descriptions-item>
            <n-descriptions-item label="同步到组织">
              {{ detail.connection?.defaultOrgId || '根组织' }}
            </n-descriptions-item>
            <n-descriptions-item label="自动同步">
              {{ Number(detail.connection?.syncScheduleEnabled) === 1 ? detail.connection?.syncCron || '已开启' : '未开启' }}
            </n-descriptions-item>
            <n-descriptions-item label="状态">
              <DictTag dict-type="sys_normal_disable" :value="String(detail.connection?.status ?? '')" size="small" />
            </n-descriptions-item>
          </n-descriptions>

          <section class="setup-section">
            <div class="setup-section__heading">
              <div>
                <div class="setup-section__title">
                  平台应用
                </div>
                <div class="setup-section__desc">
                  应用是平台侧创建的具体程序，登录、同步和消息能力都通过应用凭据运行。
                </div>
              </div>
              <n-button v-if="manageMode && canManage" size="small" type="primary" @click="handleAddApp">
                新增应用
              </n-button>
            </div>
            <n-data-table v-if="(detail.apps || []).length" :columns="appColumns" :data="detail.apps || []" :scroll-x="960" :bordered="false" size="small" class="setup-table" />
            <n-empty v-else description="还没有平台应用，请先新增应用凭据" size="small" />
          </section>

          <section class="setup-section">
            <div class="setup-section__heading">
              <div>
                <div class="setup-section__title">
                  启用能力
                </div>
                <div class="setup-section__desc">
                  选择应用实际承担的业务能力，同一连接可以绑定多个能力。
                </div>
              </div>
              <n-button v-if="manageMode && canManage" size="small" type="primary" @click="handleOpenBind">
                启用能力
              </n-button>
            </div>
            <n-data-table v-if="(detail.bindings || []).length" :columns="bindingColumns" :data="detail.bindings || []" :scroll-x="720" :bordered="false" size="small" class="setup-table" />
            <n-empty v-else description="尚未启用能力" size="small" />
          </section>
        </div>
      </n-spin>
    </div>
  </section>

  <n-modal
    v-model:show="appModalVisible"
    :title="appForm.id ? '编辑平台应用' : '新增平台应用'"
    preset="card"
    style="width: min(720px, calc(100vw - 32px))"
    :mask-closable="false"
    :closable="!appSubmitLoading" :close-on-esc="!appSubmitLoading"
    :content-style="{ maxHeight: '70vh', overflow: 'auto' }"
  >
    <n-alert type="info" :show-icon="false" size="small" class="app-tip">
      Secret、回调 Token 和 AES Key 不会回显。编辑时留空表示保留原值，只有输入新值才会轮换凭据。
    </n-alert>
    <n-form ref="appFormRef" :model="appForm" :rules="appFormRules" label-placement="top" :disabled="appSubmitLoading">
      <n-grid cols="1 600:2" responsive="screen" :x-gap="16">
        <n-form-item-gi label="应用编码" path="appCode">
          <n-input v-model:value="appForm.appCode" placeholder="连接内唯一，如 main-app" :disabled="!!appForm.id" />
        </n-form-item-gi>
        <n-form-item-gi label="应用名称" path="appName">
          <n-input v-model:value="appForm.appName" placeholder="如：企业微信登录应用" />
        </n-form-item-gi>
        <n-form-item-gi :label="clientIdLabel" path="clientId">
          <n-input v-model:value="appForm.clientId" :placeholder="clientIdPlaceholder" />
        </n-form-item-gi>
        <n-form-item-gi v-if="isEnterprisePlatform" label="AgentId" path="agentId">
          <n-input v-model:value="appForm.agentId" placeholder="企业微信自建应用 AgentId" />
        </n-form-item-gi>
        <n-form-item-gi label="应用 Secret" path="secret" span="1 600:2">
          <n-input v-model:value="appForm.secret" type="password" show-password-on="click" :placeholder="appForm.id ? '留空表示保留现有 Secret' : clientSecretPlaceholder" />
        </n-form-item-gi>
        <n-form-item-gi v-if="isEnterprisePlatform" label="回调 Token" path="callbackToken">
          <n-input v-model:value="appForm.callbackToken" type="password" show-password-on="click" :placeholder="appForm.id ? '留空保留现值' : '启用事件回调时填写'" />
        </n-form-item-gi>
        <n-form-item-gi v-if="isEnterprisePlatform" label="EncodingAESKey" path="encodingAesKey">
          <n-input v-model:value="appForm.encodingAesKey" type="password" show-password-on="click" :placeholder="appForm.id ? '留空保留现值' : '启用事件回调时填写'" />
        </n-form-item-gi>
        <n-form-item-gi v-if="!isEnterprisePlatform" label="OAuth 回调地址" path="redirectUri" span="1 600:2">
          <n-input v-model:value="appForm.redirectUri" placeholder="必须与平台侧登记的回调地址完全一致" />
        </n-form-item-gi>
        <n-form-item-gi v-if="!isEnterprisePlatform" label="授权范围" path="scope">
          <n-input v-model:value="appForm.scope" placeholder="如 user_info，多个用逗号分隔" />
        </n-form-item-gi>
        <n-form-item-gi label="状态" path="status">
          <n-select v-model:value="appForm.status" :options="appStatusOptions" />
        </n-form-item-gi>
        <n-form-item-gi label="备注" path="remark" span="1 600:2">
          <n-input v-model:value="appForm.remark" type="textarea" :rows="2" placeholder="备注说明" />
        </n-form-item-gi>
      </n-grid>
    </n-form>
    <template #footer>
      <n-space justify="end">
        <n-button :disabled="appSubmitLoading" @click="appModalVisible = false">
          取消
        </n-button>
        <n-button type="primary" :loading="appSubmitLoading" @click="handleSubmitApp">
          保存应用
        </n-button>
      </n-space>
    </template>
  </n-modal>

  <n-modal v-model:show="bindModalVisible" title="启用能力" preset="card" style="width: min(480px, calc(100vw - 32px))" :mask-closable="false" :closable="!bindSubmitLoading" :close-on-esc="!bindSubmitLoading">
    <n-alert type="info" :show-icon="false" size="small" class="app-tip">
      先选择能力，再指定负责这项能力的平台应用。
    </n-alert>
    <n-form label-placement="top" :disabled="bindSubmitLoading">
      <n-form-item label="要启用的能力">
        <n-select v-model:value="bindForm.capability" :options="availableCapabilityOptions" placeholder="请选择能力" />
      </n-form-item>
      <n-form-item label="使用的平台应用">
        <n-select v-model:value="bindForm.appConfigId" :options="appSelectOptions" placeholder="请选择应用" />
      </n-form-item>
    </n-form>
    <template #footer>
      <n-space justify="end">
        <n-button :disabled="bindSubmitLoading" @click="bindModalVisible = false">
          取消
        </n-button>
        <n-button type="primary" :loading="bindSubmitLoading" @click="handleSubmitBind">
          启用能力
        </n-button>
      </n-space>
    </template>
  </n-modal>
</template>

<script setup>
import { computed, h, ref, watch } from 'vue'
import {
  bindConnectionCapability,
  createConnectionApp,
  deleteConnectionApp,
  getConnectionDetail,
  unbindConnectionCapability,
  updateConnectionApp,
} from '@/api/collaboration'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'

const props = defineProps({
  show: Boolean,
  connectionId: [Number, String],
  manageMode: Boolean,
  canManage: Boolean,
})
const emit = defineEmits(['update:show', 'saved', 'edit'])
const visible = computed({ get: () => props.show, set: value => emit('update:show', value) })
const detail = ref(null)
const detailLoading = ref(false)
const detailError = ref('')
let detailSequence = 0
const appModalVisible = ref(false)
const appFormRef = ref(null)
const appSubmitLoading = ref(false)
const appForm = ref({})
const bindModalVisible = ref(false)
const bindSubmitLoading = ref(false)
const bindForm = ref({ capability: null, appConfigId: null })

const { dict, getLabel } = useDict('sys_collab_platform', 'sys_collab_capability', 'sys_normal_disable')
const capabilityOptions = computed(() => (dict.value.sys_collab_capability || []).filter(item => item.value !== 'TODO'))
const availableCapabilityOptions = computed(() => {
  const enabled = new Set((detail.value?.bindings || []).filter(item => Number(item.status) === 1).map(item => item.capability))
  return capabilityOptions.value.filter(item => !enabled.has(item.value))
})
const appStatusOptions = computed(() => (dict.value.sys_normal_disable || []).map(item => ({ ...item, value: Number(item.value) })))
const isEnterprisePlatform = computed(() => ['WECHAT_ENTERPRISE', 'DINGTALK', 'DINGTALK_ACCOUNT', 'FEISHU'].includes(detail.value?.connection?.platform))
const clientIdLabel = computed(() => isEnterprisePlatform.value ? '平台应用 ID' : 'Client ID')
const clientIdPlaceholder = computed(() => isEnterprisePlatform.value ? '平台应用的 CorpID / AppKey' : '平台 OAuth 应用的 Client ID')
const clientSecretPlaceholder = computed(() => isEnterprisePlatform.value ? '平台应用 Secret' : '平台 OAuth 应用的 Client Secret')
const configuredApps = computed(() => (detail.value?.apps || []).filter((app) => {
  return Number(app.status) === 1 && Boolean(app.clientId) && app.secretConfigured === true
    && (detail.value?.connection?.platform !== 'WECHAT_ENTERPRISE' || Boolean(app.agentId))
}))
const setupHint = computed(() => {
  const connection = detail.value?.connection || {}
  if (Number(connection.status) === 0)
    return { type: 'warning', message: '连接当前已停用，保存配置不会自动启用它。' }
  if (!(detail.value?.apps || []).length)
    return { type: 'warning', message: '基本信息已保存，还差一步：新增平台应用并填写凭据。' }
  if (!configuredApps.value.length)
    return { type: 'warning', message: '平台应用已添加，但仍缺少有效的应用 ID 或 Secret，企业微信还需填写 AgentId。' }
  if (!(detail.value?.bindings || []).length)
    return { type: 'info', message: '应用已配置，还没有启用业务能力。请至少绑定一项能力。' }
  return { type: 'info', message: '绑定已保存。运行时仍校验连接、应用状态与平台支持；请通过列表的连通测试、投递记录确认实际结果。' }
})

watch(() => [props.show, props.connectionId], ([show, id]) => {
  detailSequence++
  detail.value = null
  detailError.value = ''
  appModalVisible.value = bindModalVisible.value = false
  if (show && id)
    loadDetail(id)
}, { immediate: true })

async function loadDetail(id) {
  const sequence = ++detailSequence
  detailLoading.value = true
  detailError.value = ''
  try {
    const res = await getConnectionDetail(id)
    if (sequence !== detailSequence)
      return
    if (res.code === 200 && res.data)
      detail.value = res.data
    else throw new Error(res.message || '获取连接详情失败')
  }
  catch (error) {
    if (sequence === detailSequence)
      detailError.value = error?.message || '获取连接详情失败'
  }
  finally {
    if (sequence === detailSequence)
      detailLoading.value = false
  }
}

const appColumns = computed(() => {
  const columns = [
    { title: '应用名称', key: 'appName', width: 150 },
    { title: '应用编码', key: 'appCode', width: 120 },
    { title: '应用 ID', key: 'clientId', width: 150, ellipsis: { tooltip: true } },
    { title: 'AgentId', key: 'agentId', width: 100, render: row => row.agentId || '-' },
    { title: 'Secret 状态', key: 'secretMasked', width: 110, render: row => row.secretConfigured ? (row.secretMasked || '已配置') : '未配置' },
    { title: '状态', key: 'status', width: 80, render: row => h(DictTag, { dictType: 'sys_normal_disable', value: String(row.status ?? ''), size: 'small' }) },
  ]
  if (props.manageMode && props.canManage) {
    columns.push({ title: '操作', key: 'action', width: 110, fixed: 'right', render: row => h('div', { class: 'setup-actions' }, [
      h('a', { class: 'text-primary cursor-pointer', onClick: () => handleEditApp(row) }, '编辑'),
      h('a', { class: 'text-error cursor-pointer', onClick: () => handleDeleteApp(row) }, '删除'),
    ]) })
  }
  return columns
})
const bindingColumns = computed(() => {
  const columns = [
    { title: '启用能力', key: 'capability', width: 140, render: row => h(DictTag, { dictType: 'sys_collab_capability', value: row.capability, size: 'small' }) },
    {
      title: '使用的平台应用',
      key: 'appConfigId',
      render: (row) => {
        const app = (detail.value?.apps || []).find(item => String(item.id) === String(row.appConfigId))
        return app ? `${app.appName}（${app.appCode}）` : String(row.appConfigId ?? '-')
      },
    },
    { title: '状态', key: 'status', width: 80, render: row => h(DictTag, { dictType: 'sys_normal_disable', value: String(row.status ?? ''), size: 'small' }) },
  ]
  if (props.manageMode && props.canManage) {
    columns.push({ title: '操作', key: 'action', width: 70, fixed: 'right', render: row => h('a', { class: 'text-error cursor-pointer', onClick: () => handleUnbind(row) }, '停用') })
  }
  return columns
})
const appSelectOptions = computed(() => configuredApps.value.map(app => ({ label: `${app.appName}（${app.appCode}）`, value: String(app.id) })))
const appFormRules = { appCode: [{ required: true, message: '请输入应用编码', trigger: 'blur' }], appName: [{ required: true, message: '请输入应用名称', trigger: 'blur' }], clientId: [{ required: true, message: '请输入应用 ID', trigger: 'blur' }] }

function handleAddApp() {
  appForm.value = { status: 1 }
  appModalVisible.value = true
}
function handleEditConnection() {
  visible.value = false
  emit('edit', detail.value?.connection)
}
function handleEditApp(row) {
  appForm.value = { id: row.id, appCode: row.appCode, appName: row.appName, clientId: row.clientId, agentId: row.agentId, secret: '', callbackToken: '', encodingAesKey: '', redirectUri: row.redirectUri, scope: row.scope, status: row.status, remark: row.remark }
  appModalVisible.value = true
}
async function handleSubmitApp() {
  if (appSubmitLoading.value)
    return
  appSubmitLoading.value = true
  const connectionId = props.connectionId
  try {
    await appFormRef.value?.validate()
  }
  catch {
    appSubmitLoading.value = false
    return
  }
  try {
    if (connectionId !== props.connectionId || !props.show)
      return
    const res = appForm.value.id ? await updateConnectionApp(connectionId, { ...appForm.value }) : await createConnectionApp(connectionId, { ...appForm.value })
    if (connectionId !== props.connectionId || !props.show)
      return
    if (res.code === 200) {
      window.$message.success('平台应用已保存')
      appModalVisible.value = false
      await loadDetail(props.connectionId)
      emit('saved')
    }
  }
  catch { window.$message.error('保存平台应用失败') }
  finally { appSubmitLoading.value = false }
}
function handleDeleteApp(row) {
  window.$dialog.warning({ title: '确认删除', content: `确定要删除应用「${row.appName}」吗？被能力使用的应用无法删除。`, positiveText: '确定', negativeText: '取消', onPositiveClick: async () => {
    try {
      const res = await deleteConnectionApp(props.connectionId, row.id)
      if (res.code === 200) {
        window.$message.success('应用已删除')
        await loadDetail(props.connectionId)
        emit('saved')
      }
    }
    catch {
      window.$message.error('删除平台应用失败')
    }
  } })
}
function handleOpenBind() {
  if (!appSelectOptions.value.length) {
    window.$message.warning('请先补齐应用 ID、Secret（企业微信还需 AgentId），并启用平台应用')
    return
  }
  if (!availableCapabilityOptions.value.length) {
    window.$message.info('当前可用能力均已启用')
    return
  }
  bindForm.value = { capability: null, appConfigId: null }
  bindModalVisible.value = true
}
async function handleSubmitBind() {
  if (bindSubmitLoading.value)
    return
  if (!bindForm.value.capability || !bindForm.value.appConfigId) {
    window.$message.warning('请选择能力与平台应用')
    return
  }
  bindSubmitLoading.value = true
  const connectionId = props.connectionId
  try {
    const res = await bindConnectionCapability(connectionId, { ...bindForm.value })
    if (connectionId !== props.connectionId || !props.show)
      return
    if (res.code === 200) {
      window.$message.success('能力已启用')
      bindModalVisible.value = false
      await loadDetail(props.connectionId)
      emit('saved')
    }
  }
  catch {
    window.$message.error('启用能力失败')
  }
  finally {
    bindSubmitLoading.value = false
  }
}
function handleUnbind(row) {
  window.$dialog.warning({ title: '确认停用能力', content: `确定要停用「${getLabel('sys_collab_capability', row.capability)}」吗？`, positiveText: '确定', negativeText: '取消', onPositiveClick: async () => {
    try {
      const res = await unbindConnectionCapability(props.connectionId, row.capability)
      if (res.code === 200) {
        window.$message.success('能力已停用')
        await loadDetail(props.connectionId)
        emit('saved')
      }
    }
    catch {
      window.$message.error('停用能力失败')
    }
  } })
}
</script>

<style scoped>
.connection-workspace {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  min-width: 0;
  background: var(--bg-primary);
  border: 1px solid var(--border-light);
  border-radius: 4px;
}
.workspace-heading {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-light);
}
.workspace-heading__title {
  flex: 1;
  min-width: 0;
}
.workspace-heading__title strong {
  display: block;
  font-size: 15px;
  color: var(--text-primary);
  overflow-wrap: anywhere;
}
.workspace-heading__title span {
  font-size: 12px;
  color: var(--text-tertiary);
}
.workspace-body {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 16px 20px;
}
.connection-summary {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  padding-bottom: 16px;
  font-size: 12px;
  color: var(--text-secondary);
}
.setup-panel {
  width: 100%;
  min-width: 0;
}
.setup-section__desc {
  margin-top: 4px;
  color: var(--text-tertiary);
  font-size: 12px;
  line-height: 18px;
}
.setup-hint,
.app-tip {
  margin-bottom: 14px;
}
.setup-section {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--border-light);
}

.setup-table {
  width: 100%;
}
.setup-section__heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 8px;
}
.setup-section__title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}
:deep(.setup-actions) {
  display: flex;
  gap: 12px;
}
.mono-text {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  color: var(--text-tertiary);
  font-size: 12px;
}
@media (max-width: 720px) {
  .workspace-heading {
    flex-wrap: wrap;
    padding: 12px;
    gap: 8px;
  }
  .workspace-body {
    padding: 12px;
  }
}
</style>
