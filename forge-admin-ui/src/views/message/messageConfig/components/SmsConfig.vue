<template>
  <div class="sms-config">
    <n-spin :show="loading">
      <div class="sms-config-body">
      <!-- 策略：选路 + Ping 并排铺满 -->
      <div class="config-section">
        <div class="section-toolbar">
          <div class="section-title">
            路由与 Ping 策略
          </div>
          <div class="section-actions">
            <n-button type="primary" :loading="policySaving" @click="savePolicy">
              保存策略
            </n-button>
            <n-button :loading="pingLoading" @click="runPing">
              立即 Ping
            </n-button>
          </div>
        </div>

        <div class="policy-grid">
          <n-form label-placement="left" label-width="120" class="policy-pane">
            <div class="pane-label">
              选路
            </div>
            <n-form-item label="选路模式">
              <n-radio-group v-model:value="policy.routeMode">
                <n-radio value="WEIGHTED_RR">
                  权重轮询
                </n-radio>
                <n-radio value="PRIMARY_ONLY">
                  仅主通道
                </n-radio>
              </n-radio-group>
            </n-form-item>
            <n-form-item label="验证码锁主">
              <div class="inline-field">
                <n-switch
                  v-model:value="policy.captchaLockPrimary"
                  :checked-value="1"
                  :unchecked-value="0"
                />
                <span class="hint">验证码固定走主通道</span>
              </div>
            </n-form-item>
            <n-form-item label="失败 Failover">
              <div class="inline-field">
                <n-switch
                  v-model:value="policy.failoverEnabled"
                  :checked-value="1"
                  :unchecked-value="0"
                />
                <n-input-number
                  v-model:value="policy.failoverMaxAttempts"
                  :min="1"
                  :max="5"
                  :disabled="!policy.failoverEnabled"
                  class="attempts-input"
                />
                <span class="hint">最大尝试次数</span>
              </div>
            </n-form-item>
          </n-form>

          <n-form label-placement="left" label-width="120" class="policy-pane">
            <div class="pane-label pane-label--row">
              <span>Ping 探活</span>
              <div class="header-switch">
                <span class="hint">定时</span>
                <n-switch
                  v-model:value="policy.pingEnabled"
                  :checked-value="1"
                  :unchecked-value="0"
                />
              </div>
            </div>
            <n-form-item label="运维手机号">
              <n-input
                v-model:value="policy.pingPhones"
                placeholder="多个号码用逗号分隔"
              />
            </n-form-item>
            <n-form-item label="Ping Cron">
              <n-input
                v-model:value="policy.pingCron"
                placeholder="0 0 */6 * * ?"
              />
            </n-form-item>
          </n-form>
        </div>
      </div>

      <!-- 短信通道列表：全宽 -->
      <div class="config-section">
        <div class="section-toolbar">
          <div class="section-title">
            短信通道列表
          </div>
          <n-button type="primary" @click="openCreate">
            新增通道
          </n-button>
        </div>
        <n-data-table
          :columns="columns"
          :data="rows"
          :bordered="false"
          size="medium"
        />
      </div>

      <!-- Ping 日志 -->
      <div v-if="pingLogs.length" class="config-section">
        <div class="section-toolbar">
          <div class="section-title">
            最近 Ping 日志
          </div>
        </div>
        <n-data-table
          :columns="pingColumns"
          :data="pingLogs"
          :bordered="false"
          size="medium"
        />
      </div>
      </div>
    </n-spin>

    <n-drawer v-model:show="drawerVisible" :width="640" placement="right">
      <n-drawer-content :title="formData.id ? '编辑短信通道' : '新增短信通道'" closable>
        <n-form ref="formRef" :model="formData" :rules="rules" label-placement="left" label-width="120">
          <n-form-item label="状态" path="status">
            <n-switch v-model:value="formData.status" :checked-value="1" :unchecked-value="0" />
          </n-form-item>
          <n-form-item label="通道角色" path="channelRole">
            <n-radio-group v-model:value="formData.channelRole">
              <n-radio value="PRIMARY">
                主通道
              </n-radio>
              <n-radio value="SECONDARY">
                一般通道
              </n-radio>
            </n-radio-group>
          </n-form-item>
          <n-form-item label="短信厂商" path="supplier">
            <n-select v-model:value="formData.supplier" :options="supplierOptions" placeholder="请选择短信厂商" />
          </n-form-item>
          <n-form-item label="配置标识" path="configId">
            <n-input v-model:value="formData.configId" placeholder="如 tencent-main" />
          </n-form-item>
          <n-form-item label="AccessKey ID" path="accessKeyId">
            <n-input v-model:value="formData.accessKeyId" />
          </n-form-item>
          <n-form-item label="AccessKey Secret" path="accessKeySecret">
            <n-input v-model:value="formData.accessKeySecret" type="password" show-password-on="click" />
          </n-form-item>
          <n-form-item label="短信签名" path="signature">
            <n-input v-model:value="formData.signature" />
          </n-form-item>
          <n-form-item label="默认模板ID" path="templateId">
            <n-input v-model:value="formData.templateId" />
          </n-form-item>
          <n-form-item label="验证码模板">
            <n-input v-model:value="sceneTemplates.LOGIN_CAPTCHA" placeholder="LOGIN_CAPTCHA 场景模板 ID" />
          </n-form-item>
          <n-form-item label="Ping 模板">
            <n-input v-model:value="sceneTemplates.SMS_PING" placeholder="SMS_PING 场景模板 ID" />
          </n-form-item>
          <n-form-item label="权重" path="weight">
            <div class="inline-field">
              <n-input-number v-model:value="formData.weight" :min="1" />
              <span class="hint">主:备 = 2:1 即主 weight=2、备=1</span>
            </div>
          </n-form-item>
          <n-form-item v-if="formData.supplier === 'tencent'" label="SdkAppId">
            <n-input v-model:value="extraConfig.tencent.sdkAppId" />
          </n-form-item>
          <n-form-item label="备注">
            <n-input v-model:value="formData.remark" type="textarea" :rows="2" />
          </n-form-item>
        </n-form>
        <template #footer>
          <n-space justify="end">
            <n-button @click="drawerVisible = false">
              取消
            </n-button>
            <n-button type="primary" :loading="saving" @click="handleSave">
              保存
            </n-button>
          </n-space>
        </template>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup>
import { NButton, NSpace, NTag } from 'naive-ui'
import { h, onMounted, ref } from 'vue'
import api from '../api.js'

const loading = ref(false)
const saving = ref(false)
const policySaving = ref(false)
const pingLoading = ref(false)
const drawerVisible = ref(false)
const formRef = ref(null)
const rows = ref([])
const pingLogs = ref([])
const supplierOptions = ref([])

const policy = ref({
  routeMode: 'WEIGHTED_RR',
  captchaLockPrimary: 1,
  failoverEnabled: 1,
  failoverMaxAttempts: 2,
  pingEnabled: 0,
  pingPhones: '',
  pingCron: '0 0 */6 * * ?',
  pingMode: 'PER_CHANNEL',
})

const emptyForm = () => ({
  id: null,
  configId: '',
  supplier: null,
  accessKeyId: '',
  accessKeySecret: '',
  signature: '',
  templateId: '',
  weight: 1,
  retryInterval: 5,
  maxRetries: 0,
  status: 1,
  channelRole: 'SECONDARY',
  remark: '',
  extraConfig: null,
  sceneTemplates: null,
})

const formData = ref(emptyForm())
const sceneTemplates = ref({ LOGIN_CAPTCHA: '', SMS_PING: '' })
const extraConfig = ref({
  tencent: { sdkAppId: '', territory: 'ap-guangzhou', requestUrl: 'sms.tencentcloudapi.com', action: 'SendSms', version: '2021-01-11', service: 'sms' },
  alibaba: {},
  mas: {},
})

const rules = {
  supplier: { required: true, message: '请选择短信厂商', trigger: 'change' },
  configId: { required: true, message: '请输入配置标识', trigger: 'blur' },
  accessKeyId: { required: true, message: '请输入 AccessKey ID', trigger: 'blur' },
  accessKeySecret: {
    required: true,
    trigger: 'blur',
    validator: (_rule, value) => {
      if (value === '******' || (value && String(value).trim()))
        return true
      return new Error('请输入 AccessKey Secret')
    },
  },
}

const columns = [
  { title: '配置标识', key: 'configId' },
  {
    title: '厂商',
    key: 'supplier',
    render: row => supplierOptions.value.find(i => i.value === row.supplier)?.label || row.supplier,
  },
  {
    title: '角色',
    key: 'channelRole',
    width: 100,
    render: row => h(NTag, { type: row.channelRole === 'PRIMARY' ? 'success' : 'default', size: 'small' }, {
      default: () => (row.channelRole === 'PRIMARY' ? '主通道' : '一般'),
    }),
  },
  { title: '权重', key: 'weight', width: 80 },
  {
    title: '状态',
    key: 'status',
    width: 90,
    render: row => h(NTag, { type: row.status === 1 ? 'success' : 'warning', size: 'small' }, {
      default: () => (row.status === 1 ? '启用' : '禁用'),
    }),
  },
  {
    title: '操作',
    key: 'actions',
    width: 240,
    render: (row) => {
      return h(NSpace, { size: 12 }, {
        default: () => [
          h('a', { class: 'text-primary cursor-pointer', onClick: () => openEdit(row) }, '编辑'),
          row.channelRole !== 'PRIMARY'
            ? h('a', { class: 'text-success cursor-pointer', onClick: () => setPrimary(row) }, '设为主')
            : null,
          h('a', {
            class: 'text-warning cursor-pointer',
            onClick: () => toggleStatus(row),
          }, row.status === 1 ? '禁用' : '启用'),
          h('a', { class: 'text-error cursor-pointer', onClick: () => removeRow(row) }, '删除'),
        ].filter(Boolean),
      })
    },
  },
]

const pingColumns = [
  { title: '时间', key: 'sendTime', width: 180 },
  { title: '通道', key: 'configId' },
  { title: '厂商', key: 'supplier', width: 100 },
  { title: '手机号', key: 'phone', width: 130 },
  {
    title: '结果',
    key: 'status',
    width: 80,
    render: row => (row.status === 1 ? '成功' : '失败'),
  },
  { title: '错误', key: 'errorMsg', ellipsis: { tooltip: true } },
]

async function loadSuppliers() {
  const res = await api.getSmsSuppliers()
  if (res.code === 200 && res.data) {
    supplierOptions.value = Object.entries(res.data).map(([value, label]) => ({ label, value }))
  }
}

async function loadList() {
  loading.value = true
  try {
    const [listRes, policyRes, logRes] = await Promise.all([
      api.listSmsConfigs(),
      api.getSmsRoutePolicy(),
      api.listSmsPingLogs(20),
    ])
    if (listRes.code === 200)
      rows.value = listRes.data || []
    if (policyRes.code === 200 && policyRes.data)
      Object.assign(policy.value, policyRes.data)
    if (logRes.code === 200)
      pingLogs.value = logRes.data || []
  }
  finally {
    loading.value = false
  }
}

function openCreate() {
  formData.value = emptyForm()
  sceneTemplates.value = { LOGIN_CAPTCHA: '', SMS_PING: '' }
  drawerVisible.value = true
}

function openEdit(row) {
  formData.value = { ...emptyForm(), ...row }
  try {
    const scenes = row.sceneTemplates ? JSON.parse(row.sceneTemplates) : {}
    sceneTemplates.value = {
      LOGIN_CAPTCHA: scenes.LOGIN_CAPTCHA || '',
      SMS_PING: scenes.SMS_PING || '',
    }
  }
  catch {
    sceneTemplates.value = { LOGIN_CAPTCHA: '', SMS_PING: '' }
  }
  try {
    const extra = row.extraConfig ? JSON.parse(row.extraConfig) : {}
    if (extra.tencent)
      Object.assign(extraConfig.value.tencent, extra.tencent)
  }
  catch {
    // ignore
  }
  drawerVisible.value = true
}

async function handleSave() {
  await formRef.value?.validate()
  saving.value = true
  try {
    const submitData = { ...formData.value }
    submitData.sceneTemplates = JSON.stringify(sceneTemplates.value)
    if (submitData.supplier === 'tencent') {
      submitData.extraConfig = JSON.stringify({ tencent: extraConfig.value.tencent })
    }
    const res = await api.saveSmsConfig(submitData)
    if (res.code === 200) {
      window.$message.success('保存成功')
      drawerVisible.value = false
      await loadList()
    }
    else {
      window.$message.error(res.message || res.msg || '保存失败')
    }
  }
  finally {
    saving.value = false
  }
}

async function setPrimary(row) {
  const res = await api.setSmsPrimary(row.id)
  if (res.code === 200) {
    window.$message.success('已设为主通道')
    await loadList()
  }
  else {
    window.$message.error(res.message || res.msg || '设置失败')
  }
}

async function toggleStatus(row) {
  const next = row.status === 1 ? 0 : 1
  const res = await api.updateSmsStatus(row.id, next)
  if (res.code === 200) {
    window.$message.success('状态已更新')
    await loadList()
  }
  else {
    window.$message.error(res.message || res.msg || '更新失败')
  }
}

async function removeRow(row) {
  if (!window.confirm(`确定删除短信通道 ${row.configId} 吗？`))
    return
  const res = await api.deleteSmsConfig(row.id)
  if (res.code === 200) {
    window.$message.success('已删除')
    await loadList()
  }
  else {
    window.$message.error(res.message || res.msg || '删除失败')
  }
}

async function savePolicy() {
  policySaving.value = true
  try {
    const res = await api.saveSmsRoutePolicy(policy.value)
    if (res.code === 200) {
      window.$message.success('策略已保存')
      if (res.data)
        Object.assign(policy.value, res.data)
    }
    else {
      window.$message.error(res.message || res.msg || '保存失败')
    }
  }
  finally {
    policySaving.value = false
  }
}

async function runPing() {
  pingLoading.value = true
  try {
    const res = await api.pingSmsNow()
    if (res.code === 200) {
      window.$message.success(`Ping 完成，成功 ${res.data?.successCount ?? 0} 条`)
      await loadList()
    }
    else {
      window.$message.error(res.message || res.msg || 'Ping 失败')
    }
  }
  finally {
    pingLoading.value = false
  }
}

onMounted(async () => {
  await loadSuppliers()
  await loadList()
})
</script>

<style scoped>
.sms-config {
  width: 100%;
}

.sms-config-body {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.config-section {
  background: var(--bg-primary);
  border: 1px solid var(--border-light);
  border-radius: 8px;
  padding: 16px 20px;
}

.section-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.section-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.policy-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 32px;
}

.policy-pane {
  min-width: 0;
}

.policy-pane + .policy-pane {
  border-left: 1px solid var(--border-light);
  padding-left: 32px;
}

.pane-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 10px;
}

.pane-label--row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.header-switch {
  display: flex;
  align-items: center;
  gap: 8px;
}

.policy-pane :deep(.n-form-item) {
  margin-bottom: 12px;
}

.policy-pane :deep(.n-form-item-label) {
  font-weight: 500;
}

.inline-field {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px 12px;
}

.attempts-input {
  width: 96px;
}

.hint {
  font-size: 12px;
  color: var(--text-secondary);
}

@media (max-width: 960px) {
  .policy-grid {
    grid-template-columns: 1fr;
    gap: 16px 0;
  }

  .policy-pane + .policy-pane {
    border-left: none;
    border-top: 1px solid var(--border-light);
    padding-left: 0;
    padding-top: 16px;
  }
}

.dark .config-section {
  background: var(--bg-primary);
}

.dark .section-title {
  color: #f1f5f9;
}

.dark .pane-label,
.dark .hint {
  color: var(--text-tertiary);
}
</style>
