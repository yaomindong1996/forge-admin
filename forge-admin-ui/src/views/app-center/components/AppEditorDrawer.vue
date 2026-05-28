<template>
  <n-drawer :show="show" width="520" @update:show="value => emit('update:show', value)">
    <n-drawer-content :title="form.id ? '编辑应用入口' : '新增应用入口'" closable>
      <n-form ref="formRef" :model="form" :rules="rules" label-placement="top">
        <n-form-item label="入口名称" path="appName">
          <n-input v-model:value="form.appName" placeholder="例如：客户管理" />
        </n-form-item>
        <n-form-item label="入口编码" path="appCode">
          <n-input v-model:value="form.appCode" placeholder="例如：CRM_CUSTOMER_MANAGE" />
        </n-form-item>
        <n-form-item label="所属套件" path="suiteCode">
          <n-select
            v-model:value="form.suiteCode"
            filterable
            :options="suiteOptions"
            placeholder="选择业务套件"
            @update:value="loadObjects"
          />
        </n-form-item>
        <n-form-item label="应用类型" path="appType">
          <DictSelect v-model:value="form.appType" dict-type="ai_business_app_type" />
        </n-form-item>
        <n-form-item label="入口图标">
          <n-input v-model:value="form.icon" placeholder="例如：ionicons5:PhonePortraitOutline" />
        </n-form-item>
        <n-form-item label="关联业务对象">
          <n-select
            v-model:value="form.objectCode"
            clearable
            filterable
            :options="objectOptions"
            placeholder="标准业务应用建议关联对象"
          />
        </n-form-item>
        <n-form-item label="入口模式" path="entryMode">
          <DictSelect v-model:value="form.entryMode" dict-type="ai_business_app_entry_mode" />
        </n-form-item>
        <n-form-item label="运行配置键">
          <n-input v-model:value="form.configKey" placeholder="RUNTIME 模式可填写 configKey" />
        </n-form-item>
        <n-form-item label="入口地址">
          <n-input v-model:value="form.entryUrl" placeholder="内部路由、H5、iframe 或外部地址" />
        </n-form-item>
        <template v-if="isMobileApp">
          <n-form-item label="移动场景">
            <n-select v-model:value="form.mobileScene" :options="mobileSceneOptions" placeholder="选择移动入口场景" />
          </n-form-item>
          <n-form-item label="可见范围">
            <n-select v-model:value="form.visibleScope" :options="visibleScopeOptions" placeholder="选择移动端可见范围" />
          </n-form-item>
        </template>
        <template v-if="isIntegrationApp">
          <n-form-item label="集成类型">
            <n-select v-model:value="form.platformType" :options="platformTypeOptions" placeholder="选择集成类型" />
          </n-form-item>
          <n-form-item label="业务资源键">
            <n-input v-model:value="form.integrationResource" placeholder="例如：crm.customer.webhook" />
          </n-form-item>
          <n-form-item label="事件范围">
            <n-input v-model:value="form.integrationEvents" placeholder="例如：created,updated,approved" />
          </n-form-item>
        </template>
        <n-form-item v-if="showSecurityFields" label="域名白名单">
          <n-input
            v-model:value="form.allowedDomains"
            type="textarea"
            placeholder="example.com 或 *.example.com，多个域名换行"
          />
        </n-form-item>
        <n-form-item label="业务说明">
          <n-input v-model:value="form.description" type="textarea" placeholder="说明这个入口面向的业务场景" />
        </n-form-item>
        <n-form-item label="启用状态">
          <n-switch v-model:value="form.status" :checked-value="1" :unchecked-value="0" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="emit('update:show', false)">
            取消
          </n-button>
          <n-button type="primary" :loading="saving" @click="save">
            <template #icon>
              <n-icon><SaveOutline /></n-icon>
            </template>
            保存
          </n-button>
        </n-space>
      </template>
    </n-drawer-content>
  </n-drawer>
</template>

<script setup>
import { SaveOutline } from '@vicons/ionicons5'
import { useMessage } from 'naive-ui'
import { computed, reactive, ref, watch } from 'vue'
import { businessObjectList, createBusinessApp, updateBusinessApp } from '@/api/business-app'
import DictSelect from '@/components/DictSelect.vue'

const props = defineProps({
  show: {
    type: Boolean,
    default: false,
  },
  app: {
    type: Object,
    default: null,
  },
  suites: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['update:show', 'saved'])
const message = useMessage()
const formRef = ref(null)
const saving = ref(false)
const objectOptions = ref([])

const form = reactive(defaultForm())

const rules = {
  appName: { required: true, message: '请输入入口名称', trigger: 'blur' },
  appCode: { required: true, message: '请输入入口编码', trigger: 'blur' },
  suiteCode: { required: true, message: '请选择业务套件', trigger: 'change' },
  appType: { required: true, message: '请选择应用类型', trigger: 'change' },
  entryMode: { required: true, message: '请选择入口模式', trigger: 'change' },
}

const suiteOptions = computed(() => props.suites.map(item => ({
  label: item.suiteName || item.suiteCode,
  value: item.suiteCode,
})))
const isMobileApp = computed(() => form.appType === 'MOBILE')
const isIntegrationApp = computed(() => form.appType === 'INTEGRATION')
const showSecurityFields = computed(() => ['IFRAME', 'EXTERNAL', 'H5'].includes(form.entryMode))
const mobileSceneOptions = [
  { label: 'H5 入口', value: 'h5' },
  { label: '移动待办', value: 'todo' },
  { label: '移动审批', value: 'approval' },
  { label: '移动业务', value: 'business' },
]
const visibleScopeOptions = [
  { label: '全部用户', value: 'all' },
  { label: '指定角色', value: 'role' },
  { label: '指定部门', value: 'dept' },
  { label: '负责人范围', value: 'owner' },
]
const platformTypeOptions = [
  { label: '标准接口', value: 'api' },
  { label: 'Webhook', value: 'webhook' },
  { label: '企微 / 飞书 / 钉钉', value: 'collaboration' },
  { label: '外部系统', value: 'external' },
]

watch(() => props.show, (visible) => {
  if (!visible)
    return
  Object.assign(form, defaultForm(), props.app || {})
  hydrateOptions()
  if (!form.suiteCode && props.suites.length)
    form.suiteCode = props.suites[0].suiteCode
  loadObjects()
})

async function loadObjects() {
  if (!form.suiteCode) {
    objectOptions.value = []
    return
  }
  const res = await businessObjectList({ suiteCode: form.suiteCode, status: 1 })
  objectOptions.value = (res.data || []).map(item => ({
    label: item.objectName || item.objectCode,
    value: item.objectCode,
  }))
}

async function save() {
  await formRef.value?.validate()
  saving.value = true
  try {
    const payload = { ...form, options: buildOptions() }
    delete payload.allowedDomains
    delete payload.mobileScene
    delete payload.visibleScope
    delete payload.platformType
    delete payload.integrationResource
    delete payload.integrationEvents
    if (form.id)
      await updateBusinessApp(payload)
    else
      await createBusinessApp(payload)
    message.success('应用入口已保存')
    emit('saved')
    emit('update:show', false)
  }
  finally {
    saving.value = false
  }
}

function hydrateOptions() {
  try {
    const options = form.options ? JSON.parse(form.options) : {}
    const allowedDomains = Array.isArray(options.allowedDomains)
      ? options.allowedDomains
      : options.allowedDomains
        ? [options.allowedDomains]
        : []
    form.allowedDomains = allowedDomains.join('\n')
    form.mobileScene = options.mobileScene || defaultMobileScene(form.entryMode)
    form.visibleScope = options.visibleScope || 'all'
    form.platformType = options.platformType || defaultPlatformType(form.entryMode)
    form.integrationResource = options.integrationResource || ''
    form.integrationEvents = Array.isArray(options.integrationEvents)
      ? options.integrationEvents.join(',')
      : options.integrationEvents || ''
  }
  catch {
    form.allowedDomains = ''
    form.mobileScene = defaultMobileScene(form.entryMode)
    form.visibleScope = 'all'
    form.platformType = defaultPlatformType(form.entryMode)
    form.integrationResource = ''
    form.integrationEvents = ''
  }
}

function buildOptions() {
  let options = {}
  try {
    options = form.options ? JSON.parse(form.options) : {}
  }
  catch {
    options = {}
  }
  const allowedDomains = String(form.allowedDomains || '')
    .split(/[\n,，]/)
    .map(item => item.trim().toLowerCase())
    .filter(Boolean)
  if (showSecurityFields.value && allowedDomains.length) {
    options.allowedDomains = allowedDomains
  }
  else {
    delete options.allowedDomains
  }
  if (isMobileApp.value) {
    options.mobileScene = form.mobileScene || defaultMobileScene(form.entryMode)
    options.visibleScope = form.visibleScope || 'all'
  }
  else {
    delete options.mobileScene
    delete options.visibleScope
  }
  if (isIntegrationApp.value) {
    options.platformType = form.platformType || defaultPlatformType(form.entryMode)
    options.integrationResource = String(form.integrationResource || '').trim() || null
    options.integrationEvents = String(form.integrationEvents || '')
      .split(/[,，\n]/)
      .map(item => item.trim())
      .filter(Boolean)
  }
  else {
    delete options.platformType
    delete options.integrationResource
    delete options.integrationEvents
  }
  Object.keys(options).forEach((key) => {
    const value = options[key]
    if (value === null || value === undefined || value === '')
      delete options[key]
    if (Array.isArray(value) && !value.length)
      delete options[key]
  })
  return Object.keys(options).length ? JSON.stringify(options) : null
}

function defaultMobileScene(entryMode) {
  return entryMode === 'H5' ? 'h5' : 'business'
}

function defaultPlatformType(entryMode) {
  return entryMode === 'API' ? 'api' : 'external'
}

function defaultForm() {
  return {
    id: null,
    appName: '',
    appCode: '',
    appType: 'BUSINESS',
    suiteCode: null,
    objectCode: null,
    entryMode: 'RUNTIME',
    entryUrl: '',
    configKey: '',
    icon: '',
    description: '',
    status: 1,
    sortOrder: 0,
    options: '',
    allowedDomains: '',
    mobileScene: 'h5',
    visibleScope: 'all',
    platformType: 'api',
    integrationResource: '',
    integrationEvents: '',
  }
}
</script>
