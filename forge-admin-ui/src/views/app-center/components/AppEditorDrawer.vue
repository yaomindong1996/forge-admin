<template>
  <n-drawer :show="show" width="680" @update:show="value => emit('update:show', value)">
    <n-drawer-content :title="form.id ? '编辑访问入口' : '新增访问入口'" closable>
      <n-form ref="formRef" :model="form" :rules="rules" label-placement="top">
        <n-grid :cols="2" :x-gap="12">
          <n-form-item-gi label="入口名称" path="appName">
            <n-input v-model:value="form.appName" placeholder="例如：客户管理" />
          </n-form-item-gi>
          <n-form-item-gi label="入口编码" path="appCode">
            <n-input v-model:value="form.appCode" placeholder="例如：CRM_CUSTOMER_MANAGE" />
          </n-form-item-gi>
        </n-grid>
        <n-form-item label="所属业务域" path="suiteCode">
          <n-select
            v-model:value="form.suiteCode"
            filterable
            :options="suiteOptions"
            placeholder="选择业务域"
            @update:value="loadObjects"
          />
        </n-form-item>

        <n-form-item label="挂载位置" path="mountTarget">
          <n-radio-group v-model:value="form.mountTarget" class="mount-target-grid">
            <label v-for="item in mountTargetOptions" :key="item.value" class="mount-card">
              <n-radio :value="item.value" />
              <span>
                <strong>{{ item.label }}</strong>
                <small>{{ item.description }}</small>
              </span>
            </label>
          </n-radio-group>
        </n-form-item>

        <template v-if="isAdminMount">
          <n-form-item :label="form.suiteAsMenuParent ? '业务域目录上级' : '父级菜单或模块'">
            <MenuParentSelect v-model:value="form.adminMenuParentId" placeholder="选择挂载在哪个管理端菜单下" />
          </n-form-item>
          <n-form-item v-if="form.suiteAsMenuParent && form.suiteMenuResourceId" label="实际挂载目录">
            <MenuParentSelect :value="form.suiteMenuResourceId" placeholder="已生成业务域目录" disabled />
          </n-form-item>
          <n-grid :cols="3" :x-gap="12">
            <n-form-item-gi label="同步为菜单">
              <n-switch v-model:value="form.adminMenuSyncEnabled" />
            </n-form-item-gi>
            <n-form-item-gi label="业务域作为父级目录">
              <n-switch v-model:value="form.suiteAsMenuParent" />
            </n-form-item-gi>
            <n-form-item-gi label="菜单排序">
              <n-input-number v-model:value="form.menuSort" :min="0" :show-button="false" placeholder="排序" />
            </n-form-item-gi>
          </n-grid>
        </template>

        <n-grid :cols="2" :x-gap="12">
          <n-form-item-gi label="打开方式" path="entryMode">
            <n-select v-model:value="form.entryMode" :options="entryModeOptions" />
          </n-form-item-gi>
          <n-form-item-gi label="入口类型">
            <n-select
              v-model:value="form.entryType"
              :options="entryTypeOptions"
              @update:value="handleEntryTypeChange"
            />
          </n-form-item-gi>
        </n-grid>

        <n-grid :cols="2" :x-gap="12">
          <n-form-item-gi label="关联业务单元">
            <n-select
              v-model:value="form.objectCode"
              clearable
              filterable
              :options="objectOptions"
              :placeholder="objectPlaceholder"
              @update:value="handleObjectCodeChange"
            />
          </n-form-item-gi>
          <n-form-item-gi label="入口权限码">
            <n-input
              v-model:value="form.permissionCode"
              clearable
              placeholder="例如：ai:business:customer:list"
            >
              <template #suffix>
                <n-button text type="primary" @click="applyDefaultPermissionCode">
                  生成
                </n-button>
              </template>
            </n-input>
          </n-form-item-gi>
        </n-grid>

        <div class="mode-explain">
          <strong>{{ entryModeExplain.title }}</strong>
          <p>{{ entryModeExplain.description }}</p>
        </div>

        <n-form-item v-if="showConfigKey" label="使用模式" path="appMode">
          <n-radio-group v-model:value="form.appMode" class="app-mode-grid">
            <label v-for="item in appModeOptions" :key="item.value" class="app-mode-card">
              <n-radio :value="item.value" />
              <span>
                <strong>{{ item.label }}</strong>
                <small>{{ item.description }}</small>
              </span>
            </label>
          </n-radio-group>
        </n-form-item>

        <n-form-item v-if="showConfigKey && isDynamicRenderMode" label="业务页面打开方式">
          <n-radio-group :value="form.runtimeOpenMode" class="runtime-mode-grid" @update:value="handleRuntimeOpenModeChange">
            <label v-for="item in runtimeOpenModeOptions" :key="item.value" class="runtime-mode-card">
              <n-radio :value="item.value" />
              <span>
                <strong>{{ item.label }}</strong>
                <small>{{ item.description }}</small>
              </span>
            </label>
          </n-radio-group>
        </n-form-item>
        <n-alert v-if="showConfigKey && isDynamicRenderMode" class="runtime-mode-tip" type="info" :bordered="false">
          {{ runtimeModeTip }}
        </n-alert>
        <n-alert v-if="showConfigKey && !isDynamicRenderMode" class="runtime-mode-tip" type="info" :bordered="false">
          下载代码模式不会打开在线页面，保存后可在应用总览中预览和下载完整功能代码。
        </n-alert>
        <div v-if="showConfigKey && isDynamicRenderMode" class="runtime-target-panel">
          <n-grid :cols="2" :x-gap="12">
            <n-form-item-gi label="目标页面">
              <n-select
                v-model:value="form.targetPageKey"
                :options="runtimePageOptions"
                :loading="objectDesignerLoading"
                filterable
                placeholder="选择入口打开的页面"
              />
            </n-form-item-gi>
            <n-form-item-gi label="目标表单">
              <n-select
                v-model:value="form.targetFormKey"
                :options="runtimeFormOptions"
                :loading="objectDesignerLoading"
                clearable
                filterable
                placeholder="默认表单"
              />
            </n-form-item-gi>
          </n-grid>
          <n-form-item label="入口参数">
            <CrudDefaultParamsEditor
              v-model="form.defaultParamsConfig"
              title="入口默认参数"
              description="配置打开入口时固定带入运行页的 query/defaultParams。"
              :sections="entryParamSections"
              :field-options="entryParamFieldOptions"
            />
          </n-form-item>
          <div class="runtime-target-help">
            入口负责决定从哪个页面进入；表单只决定新增、编辑、详情或填报时使用哪套字段结构。多个入口可以指向同一个业务单元的不同页面或不同表单。
          </div>
        </div>
        <n-form-item v-if="showConfigKey" label="业务页面配置">
          <n-input v-model:value="form.configKey" placeholder="选择业务单元后通常会自动带出" />
        </n-form-item>
        <n-form-item v-if="showEntryUrl" :label="entryUrlLabel">
          <n-input v-model:value="form.entryUrl" :placeholder="entryUrlPlaceholder" />
        </n-form-item>
        <n-form-item label="入口图标">
          <IconSelector v-model="form.icon" />
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
import { businessObjectDesigner, businessObjectList, createBusinessApp, updateBusinessApp } from '@/api/business-app'
import IconSelector from '@/components/IconSelector.vue'
import CrudDefaultParamsEditor from '@/components/lowcode-builder/page/CrudDefaultParamsEditor.vue'
import MenuParentSelect from '@/components/lowcode-builder/shared/MenuParentSelect.vue'
import { useDict } from '@/composables/useDict'
import { normalizeMultiFormDesignerSchema } from './designer/form-first/formDesignerSchema'

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
const selectedObjectDesigner = ref(null)
const objectDesignerLoading = ref(false)
const runtimeOpenModeTouched = ref(false)
let objectDesignerRequestSeq = 0
const { dict } = useDict('ai_business_app_entry_mode', 'ai_business_app_mode')

const form = reactive(defaultForm())

const rules = {
  appName: { required: true, message: '请输入入口名称', trigger: 'blur' },
  appCode: { required: true, message: '请输入入口编码', trigger: 'blur' },
  suiteCode: { required: true, message: '请选择业务域', trigger: 'change' },
  appType: { required: true, message: '请选择应用类型', trigger: 'change' },
  mountTarget: { required: true, message: '请选择挂载位置', trigger: 'change' },
  entryMode: { required: true, message: '请选择打开方式', trigger: 'change' },
  appMode: { required: true, message: '请选择使用模式', trigger: 'change' },
}

const mountTargetOptions = [
  {
    label: '管理端菜单',
    value: 'ADMIN',
    description: '在 PC 管理端生成一个入口菜单。',
  },
  {
    label: '移动应用',
    value: 'MOBILE',
    description: '登记 H5 或移动端业务入口。',
  },
  {
    label: '外部接口',
    value: 'API',
    description: '登记 API、Webhook 或外部系统调用资源。',
  },
]
const entryModeMeta = {
  RUNTIME: {
    title: '业务页面',
    description: '打开当前业务单元发布后的列表或填报页，适合客户、合同、申请单等业务。',
    urlLabel: '入口地址',
    urlPlaceholder: '业务页面通常由发布配置自动生成',
  },
  ROUTE: {
    title: '系统已有页面',
    description: '打开系统里已经开发好的页面，适合跳转到固定管理页或报表页。',
    urlLabel: '页面地址',
    urlPlaceholder: '例如：/app-center/stats 或 /system/user',
  },
  IFRAME: {
    title: '内嵌页面',
    description: '在管理端页面内嵌入外部系统页面，需要配置域名白名单。',
    urlLabel: '内嵌页面地址',
    urlPlaceholder: '例如：https://bi.example.com/dashboard',
  },
  EXTERNAL: {
    title: '外部打开',
    description: '点击菜单后新窗口打开外部地址，不占用管理端内容区。',
    urlLabel: '外部链接地址',
    urlPlaceholder: '例如：https://crm.example.com',
  },
  H5: {
    title: 'H5 入口',
    description: '面向移动端或轻应用的 H5 地址，可配置移动场景和可见范围。',
    urlLabel: 'H5 地址',
    urlPlaceholder: '例如：https://m.example.com/customer',
  },
  API: {
    title: '接口入口',
    description: '不直接打开页面，用于登记 API、Webhook 或外部系统集成资源。',
    urlLabel: '接口说明地址',
    urlPlaceholder: '例如：api://crm.customer.webhook',
  },
}
const entryTypeOptions = [
  { label: '对象列表入口', value: 'OBJECT_LIST' },
  { label: '新增表单入口', value: 'CREATE_FORM' },
  { label: '详情页入口', value: 'DETAIL_PAGE' },
  { label: '审批/待办入口', value: 'APPROVAL_TODO' },
  { label: '报表/看板入口', value: 'REPORT_DASHBOARD' },
  { label: '外链/API 入口', value: 'EXTERNAL_OR_API' },
]
const entryParamSections = [
  {
    key: 'publicQuery',
    label: '入口 Query 参数',
    description: '打开运行页时追加到 URL，例如 source、status、recordId。',
  },
  {
    key: 'formDefaultValues',
    label: '表单默认值',
    description: '打开新增或填报表单时预填字段值。',
  },
]

const suiteOptions = computed(() => props.suites.map(item => ({
  label: item.suiteName || item.suiteCode,
  value: item.suiteCode,
})))
const isAdminMount = computed(() => form.mountTarget === 'ADMIN')
const isMobileApp = computed(() => form.mountTarget === 'MOBILE')
const isIntegrationApp = computed(() => form.mountTarget === 'API')
const showConfigKey = computed(() => form.entryMode === 'RUNTIME')
const showEntryUrl = computed(() => !['RUNTIME', 'API'].includes(form.entryMode))
const showSecurityFields = computed(() => ['IFRAME', 'EXTERNAL', 'H5'].includes(form.entryMode))
const isDynamicRenderMode = computed(() => normalizeAppMode(form.appMode) === 'DYNAMIC_RENDER')
const entryModeOptions = computed(() => {
  const dictMap = new Map((dict.value.ai_business_app_entry_mode || []).map(item => [item.value, item]))
  return allowedEntryModesForTarget(form.mountTarget).map((value) => {
    const meta = entryModeMeta[value]
    const item = dictMap.get(value)
    return {
      label: meta?.title || item?.label || value,
      value,
    }
  })
})
const appModeMeta = {
  DYNAMIC_RENDER: {
    label: '在线运行',
    description: '在线搭建并由平台托管运行，适合需要快速发布和持续调整的业务页面。',
  },
  CODE_DOWNLOAD: {
    label: '下载代码',
    description: '生成完整功能代码包，导入本地工程后进行二次开发。',
  },
}
const appModeOptions = computed(() => {
  const dictMap = new Map((dict.value.ai_business_app_mode || []).map(item => [item.value, item]))
  return ['DYNAMIC_RENDER', 'CODE_DOWNLOAD'].map((value) => {
    const item = dictMap.get(value)
    const meta = appModeMeta[value]
    return {
      label: item?.label || meta.label,
      value,
      description: item?.remark || meta.description,
    }
  })
})
const entryModeExplain = computed(() => entryModeMeta[form.entryMode] || entryModeMeta.ROUTE)
const entryUrlLabel = computed(() => entryModeExplain.value.urlLabel)
const entryUrlPlaceholder = computed(() => entryModeExplain.value.urlPlaceholder)
const selectedObject = computed(() => objectOptions.value.find(item => item.value === form.objectCode) || null)
const runtimePageOptions = computed(() => {
  const pageSchema = selectedObjectDesigner.value?.pageSchema || {}
  const pages = Array.isArray(pageSchema.pages) ? pageSchema.pages : []
  const options = pages
    .filter(page => page?.pageKey)
    .map(page => ({
      label: `${page.pageName || page.pageKey}${page.pageKey === 'list' ? '（默认）' : ''}`,
      value: page.pageKey,
    }))
  const existing = new Set(options.map(item => item.value))
  ;[
    { label: '列表页（默认）', value: 'list' },
    { label: '详情页', value: 'detail' },
    { label: '新增页', value: 'create' },
    { label: '编辑页', value: 'edit' },
  ].forEach((item) => {
    if (!existing.has(item.value))
      options.push(item)
  })
  return options
})
const runtimeFormOptions = computed(() => {
  if (!selectedObjectDesigner.value?.formDesignerSchema)
    return []
  const schema = normalizeMultiFormDesignerSchema(selectedObjectDesigner.value?.formDesignerSchema || {})
  return (schema.forms || [])
    .filter(item => item?.formKey)
    .map(item => ({
      label: `${item.formName || item.formKey}${item.formKey === schema.defaultFormKey ? '（默认）' : ''}`,
      value: item.formKey,
    }))
})
const entryParamFieldOptions = computed(() => {
  const fields = selectedObjectDesigner.value?.modelSchema?.fields?.length
    ? selectedObjectDesigner.value.modelSchema.fields
    : selectedObjectDesigner.value?.fields || []
  return (Array.isArray(fields) ? fields : [])
    .map(field => ({
      label: `${field.label || field.fieldName || field.field || field.fieldCode}（${field.field || field.fieldCode}）`,
      value: field.field || field.fieldCode,
    }))
    .filter(item => item.value)
})
const objectPlaceholder = computed(() => {
  if (form.entryMode === 'RUNTIME')
    return '业务页面必须关联已发布业务单元'
  if (isIntegrationApp.value)
    return '可选，用于标识接口服务的业务单元'
  return '可选，关联后按业务单元归集'
})
const mobileSceneOptions = [
  { label: 'H5 入口', value: 'h5' },
  { label: '移动待办', value: 'todo' },
  { label: '移动流程待办', value: 'approval' },
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
const runtimeOpenModeOptions = computed(() => [
  {
    label: '列表管理',
    value: 'LIST',
    description: '显示列表和操作列，适合需要查看、编辑和执行自定义操作的对象。',
  },
  {
    label: '单据填报',
    value: 'CREATE_FORM',
    description: '只显示新增表单，不显示列表和行操作，适合一次性申请、登记、上报。',
  },
  {
    label: '详情查看',
    value: 'DETAIL',
    description: '用于带记录 ID 的详情入口，未带记录时回到列表。',
  },
])
const runtimeModeTip = computed(() => {
  if (form.runtimeOpenMode === 'CREATE_FORM')
    return '单据填报入口没有列表上下文，因此不会显示列表操作列和行操作按钮；需要自定义操作时请选择“列表管理”。'
  if (form.runtimeOpenMode === 'DETAIL')
    return '详情查看需要打开时带上记录 ID；普通菜单入口没有记录 ID 时会回到列表。'
  return '列表管理会展示搜索、列表、操作列和自定义操作，适合日常管理和流程处理。'
})

watch(() => props.show, (visible) => {
  if (!visible)
    return
  runtimeOpenModeTouched.value = false
  Object.assign(form, defaultForm(), props.app || {})
  hydrateOptions()
  if (!form.suiteCode && props.suites.length)
    form.suiteCode = props.suites[0].suiteCode
  normalizeEntryModeForMount()
  form.appType = resolveAppType()
  loadObjects()
})

watch(() => form.mountTarget, () => {
  normalizeEntryModeForMount()
  form.appType = resolveAppType()
  if (isIntegrationApp.value && form.entryUrl === '/app-center/integration')
    form.entryUrl = ''
})

watch(() => form.entryMode, () => {
  form.appType = resolveAppType()
  if (form.entryMode === 'RUNTIME' && !runtimeOpenModeTouched.value)
    form.runtimeOpenMode = inferRuntimeOpenMode()
  form.entryType = inferEntryType()
  if (form.entryMode !== 'RUNTIME')
    form.appMode = 'DYNAMIC_RENDER'
  if (form.entryMode === 'API' && form.entryUrl === '/app-center/integration')
    form.entryUrl = ''
  normalizeRuntimeTargets()
})

watch(() => form.appName, () => {
  if (!form.id && form.entryMode === 'RUNTIME' && !runtimeOpenModeTouched.value)
    form.runtimeOpenMode = inferRuntimeOpenMode()
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
    objectId: item.id || item.objectId,
    objectType: item.objectType,
    configKey: item.configKey,
  }))
  loadSelectedObjectDesigner()
}

async function save() {
  await formRef.value?.validate()
  if (form.entryMode === 'RUNTIME' && !form.objectCode) {
    message.warning('业务页面需要关联业务单元')
    return
  }
  if (form.entryMode === 'RUNTIME' && normalizeAppMode(form.appMode) === 'CODE_DOWNLOAD' && !form.configKey) {
    message.warning('下载代码模式需要选择业务页面配置')
    return
  }
  if (showEntryUrl.value && !String(form.entryUrl || '').trim()) {
    message.warning(`请输入${entryUrlLabel.value}`)
    return
  }
  saving.value = true
  try {
    const payload = { ...form, appType: resolveAppType(), options: buildOptions() }
    delete payload.allowedDomains
    delete payload.mobileScene
    delete payload.visibleScope
    delete payload.platformType
    delete payload.integrationResource
    delete payload.integrationEvents
    delete payload.mountTarget
    delete payload.adminMenuParentId
    delete payload.adminMenuSyncEnabled
    delete payload.suiteAsMenuParent
    delete payload.menuSort
    if (form.id)
      await updateBusinessApp(payload)
    else
      await createBusinessApp(payload)
    message.success('访问入口已保存')
    emit('saved')
    emit('update:show', false)
  }
  finally {
    saving.value = false
  }
}

function hydrateOptions() {
  try {
    const options = parseOptions(form.options)
    const adminMenu = options.adminMenu || {}
    const allowedDomains = Array.isArray(options.allowedDomains)
      ? options.allowedDomains
      : options.allowedDomains
        ? [options.allowedDomains]
        : []
    form.mountTarget = normalizeMountTarget(options.mountTarget || deriveMountTarget())
    const suiteAsMenuParent = normalizeBoolean(props.app?.suiteAsMenuParent ?? adminMenu.suiteAsParent ?? options.suiteAsMenuParent, true)
    const actualMenuParentId = normalizeMenuParentId(
      adminMenu.actualParentId
      ?? props.app?.adminMenuActualParentId
      ?? null,
    )
    const suiteMenuResourceId = normalizeMenuParentId(
      adminMenu.suiteMenuResourceId
      ?? (suiteAsMenuParent ? adminMenu.actualParentId : null)
      ?? props.app?.suiteMenuResourceId
      ?? null,
    )
    form.adminMenuParentId = normalizeAdminMenuParentId(
      props.app?.adminMenuParentId
      ?? adminMenu.originalParentId
      ?? adminMenu.parentId
      ?? options.adminMenuParentId
      ?? options.parentId
      ?? null,
      {
        suiteAsMenuParent,
        actualMenuParentId,
        suiteMenuResourceId,
        menuResourceId: props.app?.menuResourceId ?? adminMenu.menuResourceId ?? options.menuResourceId,
      },
    )
    form.actualMenuParentId = actualMenuParentId
    form.suiteMenuResourceId = suiteMenuResourceId
    form.adminMenuSyncEnabled = normalizeBoolean(props.app?.adminMenuSyncEnabled ?? adminMenu.syncEnabled ?? options.adminMenuSyncEnabled, true)
    form.suiteAsMenuParent = suiteAsMenuParent
    form.menuSort = Number(props.app?.menuSort ?? adminMenu.sort ?? options.menuSort ?? form.sortOrder ?? 0)
    form.runtimeOpenMode = normalizeRuntimeOpenMode(props.app?.runtimeOpenMode || options.runtimeOpenMode || inferRuntimeOpenMode())
    form.appMode = normalizeAppMode(props.app?.appMode || options.appMode || 'DYNAMIC_RENDER')
    form.entryType = normalizeEntryType(options.entryType || inferEntryType())
    form.permissionCode = String(options.permissionCode || '').trim()
    form.targetPageKey = String(options.targetPageKey || '').trim() || defaultTargetPageKey(form.runtimeOpenMode)
    form.targetFormKey = String(options.targetFormKey || '').trim()
    form.defaultParamsConfig = normalizeEntryDefaultParams(options.defaultParams)
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
    form.mountTarget = deriveMountTarget()
    form.adminMenuParentId = null
    form.actualMenuParentId = null
    form.suiteMenuResourceId = null
    form.adminMenuSyncEnabled = true
    form.suiteAsMenuParent = true
    form.menuSort = Number(form.sortOrder || 0)
    form.runtimeOpenMode = inferRuntimeOpenMode()
    form.appMode = 'DYNAMIC_RENDER'
    form.entryType = inferEntryType()
    form.permissionCode = ''
    form.targetPageKey = defaultTargetPageKey(form.runtimeOpenMode)
    form.targetFormKey = ''
    form.defaultParamsConfig = normalizeEntryDefaultParams()
    form.allowedDomains = ''
    form.mobileScene = defaultMobileScene(form.entryMode)
    form.visibleScope = 'all'
    form.platformType = defaultPlatformType(form.entryMode)
    form.integrationResource = ''
    form.integrationEvents = ''
  }
  normalizeEntryModeForMount()
  form.appType = resolveAppType()
  if (form.entryMode !== 'RUNTIME')
    form.runtimeOpenMode = 'LIST'
  if (form.entryMode !== 'RUNTIME')
    form.appMode = 'DYNAMIC_RENDER'
  normalizeRuntimeTargets()
}

function buildOptions() {
  const options = parseOptions(form.options)
  options.mountTarget = form.mountTarget
  options.entryType = normalizeEntryType(form.entryType)
  const permissionCode = String(form.permissionCode || '').trim()
  if (permissionCode)
    options.permissionCode = permissionCode
  else
    delete options.permissionCode
  if (isAdminMount.value) {
    const previousAdminMenu = options.adminMenu || {}
    const menuResourceId = props.app?.menuResourceId || previousAdminMenu.menuResourceId || options.menuResourceId
    const actualParentId = form.actualMenuParentId || previousAdminMenu.actualParentId || props.app?.adminMenuActualParentId
    const suiteMenuResourceId = form.suiteMenuResourceId || previousAdminMenu.suiteMenuResourceId || props.app?.suiteMenuResourceId
    const adminMenuParentId = normalizeAdminMenuParentId(form.adminMenuParentId, {
      suiteAsMenuParent: Boolean(form.suiteAsMenuParent),
      actualMenuParentId: actualParentId,
      suiteMenuResourceId,
      menuResourceId,
    })
    options.adminMenu = {
      parentId: adminMenuParentId || null,
      originalParentId: adminMenuParentId || null,
      syncEnabled: Boolean(form.adminMenuSyncEnabled),
      suiteAsParent: Boolean(form.suiteAsMenuParent),
      sort: Number(form.menuSort || 0),
    }
    if (menuResourceId)
      options.adminMenu.menuResourceId = String(menuResourceId)
    const activeMenuKey = previousAdminMenu.activeMenuKey || props.app?.activeMenuKey || menuResourceId
    if (activeMenuKey)
      options.adminMenu.activeMenuKey = String(activeMenuKey)
    if (actualParentId)
      options.adminMenu.actualParentId = String(actualParentId)
    if (suiteMenuResourceId)
      options.adminMenu.suiteMenuResourceId = String(suiteMenuResourceId)
  }
  else {
    delete options.adminMenu
    delete options.adminMenuParentId
    delete options.adminMenuSyncEnabled
    delete options.suiteAsMenuParent
    delete options.menuSort
  }
  if (form.entryMode === 'RUNTIME') {
    options.runtimeOpenMode = normalizeRuntimeOpenMode(form.runtimeOpenMode)
    options.appMode = normalizeAppMode(form.appMode)
    if (normalizeAppMode(form.appMode) === 'DYNAMIC_RENDER') {
      options.targetPageKey = form.targetPageKey || defaultTargetPageKey(form.runtimeOpenMode)
      options.targetFormKey = form.targetFormKey || null
      options.defaultParams = extractEntryDefaultParams(form.defaultParamsConfig)
    }
    else {
      delete options.targetPageKey
      delete options.targetFormKey
      delete options.defaultParams
    }
  }
  else {
    delete options.runtimeOpenMode
    delete options.appMode
    delete options.targetPageKey
    delete options.targetFormKey
    delete options.defaultParams
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

function handleRuntimeOpenModeChange(value) {
  runtimeOpenModeTouched.value = true
  form.runtimeOpenMode = normalizeRuntimeOpenMode(value)
  if (!form.targetPageKey || ['LIST', 'CREATE_FORM', 'DETAIL'].includes(form.runtimeOpenMode))
    form.targetPageKey = defaultTargetPageKey(form.runtimeOpenMode)
  form.entryType = entryTypeFromRuntimeMode(form.runtimeOpenMode)
}

function handleObjectCodeChange(value) {
  form.objectCode = value || null
  form.configKey = selectedObject.value?.configKey || ''
  form.targetPageKey = defaultTargetPageKey(form.runtimeOpenMode)
  form.targetFormKey = ''
  if (!form.permissionCode)
    form.permissionCode = defaultPermissionCode()
  selectedObjectDesigner.value = null
  loadSelectedObjectDesigner()
  if (form.entryMode === 'RUNTIME' && !runtimeOpenModeTouched.value)
    form.runtimeOpenMode = inferRuntimeOpenMode()
}

function handleEntryTypeChange(value) {
  form.entryType = normalizeEntryType(value)
  const runtimeMode = runtimeModeFromEntryType(form.entryType)
  if (runtimeMode) {
    runtimeOpenModeTouched.value = true
    form.runtimeOpenMode = runtimeMode
    form.targetPageKey = defaultTargetPageKey(runtimeMode)
  }
  if (!form.permissionCode)
    form.permissionCode = defaultPermissionCode()
}

function applyDefaultPermissionCode() {
  form.permissionCode = defaultPermissionCode()
}

async function loadSelectedObjectDesigner() {
  const objectId = selectedObject.value?.objectId
  const seq = ++objectDesignerRequestSeq
  if (!objectId) {
    selectedObjectDesigner.value = null
    normalizeRuntimeTargets()
    return
  }
  objectDesignerLoading.value = true
  try {
    const res = await businessObjectDesigner(objectId)
    if (seq !== objectDesignerRequestSeq)
      return
    selectedObjectDesigner.value = res.data || null
    normalizeRuntimeTargets()
  }
  catch (error) {
    if (seq === objectDesignerRequestSeq) {
      selectedObjectDesigner.value = null
      console.warn('[AppEditorDrawer] 加载业务对象设计数据失败', error?.message || error)
    }
  }
  finally {
    if (seq === objectDesignerRequestSeq)
      objectDesignerLoading.value = false
  }
}

function normalizeRuntimeTargets() {
  if (form.entryMode !== 'RUNTIME')
    return
  const pageValues = new Set(runtimePageOptions.value.map(item => item.value))
  if (!form.targetPageKey || !pageValues.has(form.targetPageKey))
    form.targetPageKey = defaultTargetPageKey(form.runtimeOpenMode)
  const formValues = new Set(runtimeFormOptions.value.map(item => item.value))
  if (form.targetFormKey && !formValues.has(form.targetFormKey))
    form.targetFormKey = ''
}

function allowedEntryModesForTarget(target) {
  if (target === 'MOBILE')
    return ['H5', 'ROUTE', 'EXTERNAL']
  if (target === 'API')
    return ['API']
  return ['RUNTIME', 'ROUTE', 'IFRAME', 'EXTERNAL']
}

function normalizeEntryModeForMount() {
  const allowedModes = allowedEntryModesForTarget(form.mountTarget)
  if (!allowedModes.includes(form.entryMode))
    form.entryMode = allowedModes[0]
}

function deriveMountTarget() {
  const appType = String(form.appType || '').toUpperCase()
  const entryMode = String(form.entryMode || '').toUpperCase()
  if (appType === 'MOBILE' || entryMode === 'H5')
    return 'MOBILE'
  if (appType === 'INTEGRATION' || entryMode === 'API')
    return 'API'
  return 'ADMIN'
}

function normalizeMountTarget(value) {
  return ['ADMIN', 'MOBILE', 'API'].includes(value) ? value : 'ADMIN'
}

function resolveAppType() {
  if (isMobileApp.value)
    return 'MOBILE'
  if (isIntegrationApp.value)
    return 'INTEGRATION'
  if (form.entryMode === 'RUNTIME' || form.objectCode)
    return 'BUSINESS'
  return 'EMBEDDED'
}

function normalizeBoolean(value, fallback) {
  if (value === undefined || value === null)
    return fallback
  if (typeof value === 'boolean')
    return value
  return String(value) === 'true' || String(value) === '1'
}

function defaultMobileScene(entryMode) {
  return entryMode === 'H5' ? 'h5' : 'business'
}

function defaultPlatformType(entryMode) {
  return entryMode === 'API' ? 'api' : 'external'
}

function normalizeRuntimeOpenMode(value) {
  const mode = String(value || 'LIST').toUpperCase()
  return ['LIST', 'CREATE_FORM', 'DETAIL'].includes(mode) ? mode : 'LIST'
}

function normalizeEntryType(value) {
  const type = String(value || '').toUpperCase()
  return entryTypeOptions.some(item => item.value === type) ? type : inferEntryType()
}

function entryTypeFromRuntimeMode(value) {
  const mode = normalizeRuntimeOpenMode(value)
  if (mode === 'CREATE_FORM')
    return 'CREATE_FORM'
  if (mode === 'DETAIL')
    return 'DETAIL_PAGE'
  return 'OBJECT_LIST'
}

function runtimeModeFromEntryType(value) {
  const type = normalizeEntryType(value)
  if (type === 'CREATE_FORM')
    return 'CREATE_FORM'
  if (type === 'DETAIL_PAGE' || type === 'APPROVAL_TODO')
    return 'DETAIL'
  if (type === 'OBJECT_LIST')
    return 'LIST'
  return ''
}

function defaultTargetPageKey(runtimeOpenMode) {
  const mode = normalizeRuntimeOpenMode(runtimeOpenMode)
  if (mode === 'DETAIL')
    return 'detail'
  return 'list'
}

function normalizeAppMode(value) {
  const mode = String(value || 'DYNAMIC_RENDER').toUpperCase()
  return ['DYNAMIC_RENDER', 'CODE_DOWNLOAD'].includes(mode) ? mode : 'DYNAMIC_RENDER'
}

function normalizeMenuParentId(value) {
  if (value === null || value === undefined || value === '')
    return null
  return String(value)
}

function normalizeAdminMenuParentId(value, context = {}) {
  const parentId = normalizeMenuParentId(value)
  if (!parentId || parentId === '0')
    return null
  if (!context.suiteAsMenuParent)
    return parentId
  const occupiedIds = [
    context.actualMenuParentId,
    context.suiteMenuResourceId,
    context.menuResourceId,
  ].map(normalizeMenuParentId).filter(Boolean)
  return occupiedIds.includes(parentId) ? null : parentId
}

function parseOptions(options) {
  if (!options)
    return {}
  if (typeof options === 'string') {
    try {
      const parsed = JSON.parse(options)
      return parsed && typeof parsed === 'object' ? parsed : {}
    }
    catch {
      return {}
    }
  }
  return typeof options === 'object' ? { ...options } : {}
}

function inferRuntimeOpenMode() {
  if (form.entryMode !== 'RUNTIME')
    return 'LIST'
  return /填报|申请|提交|录入|上报|登记/.test(String(form.appName || '')) ? 'CREATE_FORM' : 'LIST'
}

function inferEntryType() {
  if (form.entryMode === 'API')
    return 'EXTERNAL_OR_API'
  if (['IFRAME', 'EXTERNAL', 'H5', 'ROUTE'].includes(form.entryMode))
    return 'EXTERNAL_OR_API'
  return entryTypeFromRuntimeMode(form.runtimeOpenMode)
}

function defaultPermissionCode() {
  const objectCode = String(form.objectCode || '').trim()
  if (!objectCode)
    return ''
  const prefix = `ai:business:${objectCode}`
  const entryType = normalizeEntryType(form.entryType)
  if (entryType === 'CREATE_FORM')
    return `${prefix}:add`
  if (entryType === 'DETAIL_PAGE' || entryType === 'APPROVAL_TODO')
    return `${prefix}:query`
  if (entryType === 'REPORT_DASHBOARD')
    return 'ai:businessStats:view'
  return `${prefix}:list`
}

function normalizeEntryDefaultParams(value = {}) {
  if (value?.publicQuery || value?.formDefaultValues)
    return value
  return {
    publicQuery: value && typeof value === 'object' && !Array.isArray(value) ? value : {},
    formDefaultValues: {},
  }
}

function extractEntryDefaultParams(config = {}) {
  const query = config?.publicQuery && typeof config.publicQuery === 'object' ? config.publicQuery : {}
  const formValues = config?.formDefaultValues && typeof config.formDefaultValues === 'object' ? config.formDefaultValues : {}
  return {
    ...query,
    ...(Object.keys(formValues).length ? { formDefaultValues: formValues } : {}),
  }
}

function defaultForm() {
  return {
    id: null,
    appName: '',
    appCode: '',
    appType: 'BUSINESS',
    mountTarget: 'ADMIN',
    suiteCode: null,
    objectCode: null,
    entryMode: 'RUNTIME',
    entryType: 'OBJECT_LIST',
    appMode: 'DYNAMIC_RENDER',
    runtimeOpenMode: 'LIST',
    entryUrl: '',
    configKey: '',
    icon: '',
    description: '',
    status: 1,
    sortOrder: 0,
    options: '',
    adminMenuParentId: null,
    actualMenuParentId: null,
    suiteMenuResourceId: null,
    adminMenuSyncEnabled: true,
    suiteAsMenuParent: true,
    menuSort: 0,
    allowedDomains: '',
    mobileScene: 'h5',
    visibleScope: 'all',
    platformType: 'api',
    integrationResource: '',
    integrationEvents: '',
    permissionCode: '',
    targetPageKey: 'list',
    targetFormKey: '',
    defaultParamsConfig: normalizeEntryDefaultParams(),
  }
}
</script>

<style scoped>
.mount-target-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  width: 100%;
}

.runtime-mode-grid,
.app-mode-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  width: 100%;
}

.app-mode-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.runtime-mode-tip {
  margin-top: -6px;
  margin-bottom: 12px;
}

.runtime-target-panel {
  margin: -2px 0 14px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fafafa;
  padding: 12px;
}

.runtime-target-panel :deep(.n-form-item:last-child) {
  margin-bottom: 0;
}

.runtime-target-help {
  margin-top: 2px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.55;
}

.mount-card {
  display: grid;
  grid-template-columns: 22px minmax(0, 1fr);
  gap: 8px;
  min-height: 86px;
  cursor: pointer;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 12px;
  transition:
    border-color 160ms ease,
    background 160ms ease;
}

.mount-card:hover {
  border-color: #2f6feb;
  background: #f8fbff;
}

.runtime-mode-card,
.app-mode-card {
  display: grid;
  grid-template-columns: 22px minmax(0, 1fr);
  gap: 8px;
  min-height: 74px;
  cursor: pointer;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 10px 12px;
  transition:
    border-color 160ms ease,
    background 160ms ease;
}

.runtime-mode-card:hover,
.app-mode-card:hover {
  border-color: #2f6feb;
  background: #f8fbff;
}

.mount-card strong,
.mount-card small,
.runtime-mode-card strong,
.runtime-mode-card small,
.app-mode-card strong,
.app-mode-card small {
  display: block;
}

.mount-card strong,
.runtime-mode-card strong,
.app-mode-card strong {
  color: #111827;
  font-size: 14px;
  line-height: 1.4;
}

.mount-card small,
.runtime-mode-card small,
.app-mode-card small {
  margin-top: 4px;
  color: #6b7280;
  font-size: 12px;
  line-height: 1.45;
}

.mode-explain {
  margin: -2px 0 14px;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background: #eff6ff;
  padding: 10px 12px;
}

.mode-explain strong,
.mode-explain p {
  display: block;
}

.mode-explain strong {
  color: #1d4ed8;
  font-size: 13px;
  line-height: 1.4;
}

.mode-explain p {
  margin: 4px 0 0;
  color: #475569;
  font-size: 12px;
  line-height: 1.55;
}

@media (max-width: 640px) {
  .mount-target-grid,
  .runtime-mode-grid,
  .app-mode-grid {
    grid-template-columns: 1fr;
  }
}
</style>
