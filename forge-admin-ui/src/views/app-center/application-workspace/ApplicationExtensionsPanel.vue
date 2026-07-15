<template>
  <div class="extensions-panel">
    <header class="panel-heading">
      <div>
        <h2>动作与增强</h2>
        <p>可视化规则面向业务配置；脚本、样式和服务绑定统一经过校验、测试、版本与审计。</p>
      </div>
      <n-space>
        <n-dropdown
          v-if="objects.length"
          :options="objectActionOptions"
          @select="openObjectActions"
        >
          <n-button secondary>
            对象业务动作
          </n-button>
        </n-dropdown>
        <n-button type="primary" :loading="editorDependencyLoading" @click="openCreate">
          新建扩展
        </n-button>
      </n-space>
    </header>

    <div class="extension-guidance">
      <strong>增强怎么用</strong>
      <span>选择作用对象和触发时机，再选择业务规则、页面 JS、页面 CSS 或 Java 服务增强，保存并测试通过后启用。</span>
    </div>

    <div class="extension-steps">
      <span><i>1</i>选择对象或入口</span>
      <span><i>2</i>选择执行钩子</span>
      <span><i>3</i>配置增强内容</span>
      <span><i>4</i>测试并启用</span>
    </div>

    <div class="filter-bar">
      <n-input
        v-model:value="filters.keyword"
        clearable
        placeholder="搜索扩展名称或编码"
        @keyup.enter="loadExtensions"
      />
      <DictSelect
        v-model:value="filters.extensionType"
        dict-type="ai_business_extension_type"
        placeholder="全部类型"
      />
      <DictSelect
        v-model:value="filters.hookCode"
        dict-type="ai_business_extension_hook"
        placeholder="全部钩子"
      />
      <DictSelect
        v-model:value="filters.status"
        dict-type="ai_business_extension_status"
        placeholder="全部状态"
      />
      <n-button secondary @click="loadExtensions">
        查询
      </n-button>
    </div>

    <n-spin :show="loading">
      <n-empty
        v-if="!loading && !extensions.length"
        class="extensions-empty"
        description="当前应用还没有受治理扩展"
      >
        <template #extra>
          <n-button type="primary" @click="openCreate">
            创建第一条可视化规则
          </n-button>
        </template>
      </n-empty>

      <div v-else class="extension-table">
        <div class="extension-row extension-row-head">
          <span>扩展</span>
          <span>类型 / 钩子</span>
          <span>作用对象</span>
          <span>状态 / 版本</span>
          <span>失败策略</span>
          <span>操作</span>
        </div>
        <div v-for="item in extensions" :key="item.id" class="extension-row">
          <div class="extension-name">
            <strong>{{ item.extensionName }}</strong>
            <code>{{ item.extensionCode }}</code>
          </div>
          <div class="extension-type">
            <DictTag dict-type="ai_business_extension_type" :value="item.extensionType" :bordered="false" />
            <DictTag dict-type="ai_business_extension_hook" :value="item.hookCode" :bordered="false" />
          </div>
          <span>{{ item.objectName || item.entryName || scopeLabel(item.scopeType) }}</span>
          <div class="version-state">
            <DictTag dict-type="ai_business_extension_status" :value="item.status" :bordered="false" />
            <small>草稿 v{{ item.draftVersion }}<template v-if="item.enabledVersion"> / 运行 v{{ item.enabledVersion }}</template></small>
          </div>
          <div class="failure-state">
            <DictTag dict-type="ai_business_extension_failure_policy" :value="item.failurePolicy" :bordered="false" />
            <DictTag dict-type="ai_business_extension_risk_level" :value="item.riskLevel" :bordered="false" />
          </div>
          <div class="extension-actions">
            <a class="cursor-pointer text-primary" @click="openEdit(item)">编辑</a>
            <a class="cursor-pointer text-info" @click="validateItem(item)">校验</a>
            <a class="cursor-pointer text-primary" @click="testItem(item)">测试</a>
            <n-dropdown :options="actionOptions(item)" @select="key => handleMoreAction(key, item)">
              <a class="cursor-pointer text-info">更多</a>
            </n-dropdown>
          </div>
        </div>
      </div>
    </n-spin>

    <ExtensionEditorDrawer
      v-model:show="editorVisible"
      :application="application"
      :extension="editingExtension"
      :objects="objects"
      :entries="entries"
      :handlers="handlers"
      :start-with-test="startWithTest"
      @saved="handleSaved"
      @closed="handleEditorClosed"
    />

    <ExtensionVersionDrawer
      v-model:show="versionVisible"
      :extension="versionExtension"
      @changed="handleSaved"
    />
  </div>
</template>

<script setup>
import { useDialog, useMessage } from 'naive-ui'
import { computed, reactive, ref, watch } from 'vue'
import { businessAppList } from '@/api/business-app'
import { businessApplicationObjects } from '@/api/business-application'
import {
  acquireBusinessExtensionLock,
  businessExtensionDetail,
  businessExtensionPage,
  businessExtensionServerHandlers,
  deleteBusinessExtension,
  releaseBusinessExtensionLock,
  updateBusinessExtensionStatus,
  validateBusinessExtension,
} from '@/api/business-extension'
import DictSelect from '@/components/DictSelect.vue'
import DictTag from '@/components/DictTag.vue'
import ExtensionEditorDrawer from './ExtensionEditorDrawer.vue'
import ExtensionVersionDrawer from './ExtensionVersionDrawer.vue'

const props = defineProps({
  application: {
    type: Object,
    default: null,
  },
  initialExtensions: {
    type: Array,
    default: null,
  },
  initialObjects: {
    type: Array,
    default: null,
  },
  initialEntries: {
    type: Array,
    default: null,
  },
})

const emit = defineEmits(['changed', 'openDesigner'])
const dialog = useDialog()
const message = useMessage()
const loading = ref(false)
const extensions = ref([])
const objects = ref([])
const entries = ref([])
const handlers = ref([])
const editorDependencyLoading = ref(false)
const editorDependenciesLoaded = ref(false)
let editorDependencyPromise = null
let editorDependencyApplicationId = null
const editorVisible = ref(false)
const editingExtension = ref(null)
const startWithTest = ref(false)
const versionVisible = ref(false)
const versionExtension = ref(null)
const filters = reactive({
  keyword: '',
  extensionType: null,
  hookCode: null,
  status: null,
})

const objectActionOptions = computed(() => objects.value.map(item => ({
  label: item.objectName || item.objectCode,
  key: String(item.objectId),
})))

watch([
  () => props.application?.id,
  () => props.initialExtensions,
], ([applicationId, initialExtensions]) => {
  if (!applicationId)
    return
  if (String(editorDependencyApplicationId || '') !== String(applicationId))
    editorDependenciesLoaded.value = false
  if (Array.isArray(initialExtensions)) {
    extensions.value = [...initialExtensions]
    return
  }
  loadExtensions()
}, { immediate: true })

watch(() => props.initialObjects, (value) => {
  if (Array.isArray(value))
    objects.value = [...value]
}, { immediate: true })

watch(() => props.initialEntries, (value) => {
  if (Array.isArray(value))
    entries.value = [...value]
}, { immediate: true })

async function loadExtensions() {
  if (!props.application?.id)
    return
  loading.value = true
  try {
    const response = await businessExtensionPage({
      pageNum: 1,
      pageSize: 200,
      applicationId: props.application.id,
      ...filters,
    })
    extensions.value = response.data?.records || []
  }
  finally {
    loading.value = false
  }
}

async function loadTargets(applicationId) {
  const [objectResponse, entryResponse] = await Promise.all([
    businessApplicationObjects(applicationId),
    businessAppList({ applicationId }),
  ])
  objects.value = objectResponse.data || []
  entries.value = entryResponse.data || []
}

async function loadHandlers() {
  try {
    const response = await businessExtensionServerHandlers()
    handlers.value = response.data || []
  }
  catch {
    handlers.value = []
  }
}

async function openCreate() {
  await ensureEditorDependencies()
  startWithTest.value = false
  editingExtension.value = null
  editorVisible.value = true
}

async function openEdit(item, shouldTest = false) {
  const [detailResponse, lockResponse] = await Promise.all([
    businessExtensionDetail(item.id),
    acquireBusinessExtensionLock(item.id),
    ensureEditorDependencies(),
  ])
  startWithTest.value = shouldTest
  editingExtension.value = {
    ...(detailResponse.data || item),
    lockToken: lockResponse.data?.lockToken,
    lockExpireTime: lockResponse.data?.expireTime,
  }
  editorVisible.value = true
}

async function ensureEditorDependencies() {
  const applicationId = props.application?.id
  if (!applicationId)
    return
  if (editorDependenciesLoaded.value
    && String(editorDependencyApplicationId) === String(applicationId))
    return
  if (editorDependencyPromise
    && String(editorDependencyApplicationId) === String(applicationId))
    return editorDependencyPromise

  editorDependencyApplicationId = applicationId
  editorDependencyLoading.value = true
  const pending = (async () => {
    const tasks = [loadHandlers()]
    if (!Array.isArray(props.initialObjects) || !Array.isArray(props.initialEntries))
      tasks.push(loadTargets(applicationId))
    await Promise.all(tasks)
    if (String(props.application?.id) === String(applicationId))
      editorDependenciesLoaded.value = true
  })().finally(() => {
    if (editorDependencyPromise !== pending)
      return
    editorDependencyPromise = null
    editorDependencyLoading.value = false
  })
  editorDependencyPromise = pending
  return pending
}

function openObjectActions(objectId) {
  const item = objects.value.find(object => String(object.objectId) === String(objectId))
  if (!item)
    return
  emit('openDesigner', {
    objectId: item.objectId,
    objectCode: item.objectCode,
    panel: 'actions',
  })
}

async function validateItem(item) {
  const response = await validateBusinessExtension(item.id)
  if (response.data?.passed)
    message.success('当前草稿校验通过')
  else
    message.warning(response.data?.summary || '当前草稿校验未通过')
  await loadExtensions()
}

async function testItem(item) {
  await openEdit(item, true)
}

function actionOptions(item) {
  return [
    {
      label: item.status === 'ENABLED' ? '停用' : '启用',
      key: item.status === 'ENABLED' ? 'disable' : 'enable',
    },
    { label: '版本与差异', key: 'versions' },
    { label: '删除', key: 'delete' },
  ]
}

async function handleMoreAction(key, item) {
  if (key === 'versions') {
    versionExtension.value = item
    versionVisible.value = true
    return
  }
  if (key === 'enable' || key === 'disable') {
    await updateBusinessExtensionStatus(item.id, key === 'enable' ? 'ENABLED' : 'DISABLED')
    message.success(key === 'enable' ? '扩展已启用' : '扩展已停用')
    await handleSaved()
    return
  }
  if (key === 'delete')
    confirmDelete(item)
}

function confirmDelete(item) {
  dialog.warning({
    title: '删除业务扩展',
    content: `确认删除“${item.extensionName}”及其设计态版本吗？已启用扩展必须先停用。`,
    positiveText: '确认删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      await deleteBusinessExtension(item.id)
      message.success('扩展已删除')
      await handleSaved()
    },
  })
}

async function handleSaved() {
  await loadExtensions()
  emit('changed')
}

async function handleEditorClosed(payload) {
  const id = payload?.id
  const lockToken = payload?.lockToken
  if (!id || !lockToken)
    return
  try {
    await releaseBusinessExtensionLock(id, lockToken)
  }
  catch {
    // 锁可能已超时或保存链路已释放，关闭抽屉不阻断用户。
  }
}

function scopeLabel(scopeType) {
  return {
    APPLICATION: '整个应用',
    OBJECT: '业务对象',
    ENTRY: '页面入口',
    PAGE: '指定页面',
    COMPONENT: '指定组件',
  }[scopeType] || '整个应用'
}
</script>

<style scoped>
.extensions-panel {
  display: grid;
  gap: 14px;
}

.panel-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--border-light, #e5e6eb);
}

.panel-heading h2 {
  margin: 0;
  font-size: 18px;
}

.panel-heading p {
  margin: 5px 0 0;
  color: var(--text-tertiary, #86909c);
}

.extension-guidance {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 40px;
  padding: 8px 11px;
  border-left: 3px solid var(--primary-color, #165dff);
  color: var(--text-tertiary, #86909c);
  background: var(--bg-secondary, #f7f8fa);
  font-size: 12px;
}

.extension-guidance strong {
  flex: 0 0 auto;
  color: var(--text-secondary, #4e5969);
}

.extension-steps {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}

.extension-steps span {
  display: flex;
  align-items: center;
  gap: 7px;
  min-height: 34px;
  padding: 6px 9px;
  border: 1px solid var(--border-light, #e5e6eb);
  border-radius: 6px;
  color: var(--text-secondary, #4e5969);
  background: var(--bg-primary, #fff);
  font-size: 12px;
}

.extension-steps i {
  display: inline-grid;
  width: 20px;
  height: 20px;
  flex: 0 0 20px;
  place-items: center;
  border-radius: 50%;
  color: var(--primary-color, #165dff);
  background: color-mix(in srgb, var(--primary-color, #165dff) 9%, var(--bg-primary, #fff));
  font-size: 11px;
  font-style: normal;
  font-weight: 600;
}

.filter-bar {
  display: grid;
  grid-template-columns: minmax(180px, 1fr) 160px 160px 140px auto;
  gap: 8px;
}

.extensions-empty {
  min-height: 250px;
  padding-top: 60px;
  border: 1px solid var(--border-default, #c9cdd4);
  border-radius: 7px;
}

.extension-table {
  overflow-x: auto;
  border: 1px solid var(--border-default, #c9cdd4);
  border-radius: 7px;
}

.extension-row {
  display: grid;
  grid-template-columns: minmax(180px, 1.3fr) minmax(170px, 1fr) 130px 150px 130px 180px;
  gap: 12px;
  align-items: center;
  min-width: 1020px;
  min-height: 62px;
  padding: 8px 12px;
  border-bottom: 1px solid var(--border-light, #e5e6eb);
  color: var(--text-secondary, #4e5969);
  font-size: 12px;
}

.extension-row:last-child {
  border-bottom: 0;
}

.extension-row-head {
  min-height: 38px;
  color: var(--text-tertiary, #86909c);
  background: var(--bg-secondary, #f7f8fa);
  font-weight: 600;
}

.extension-name,
.version-state {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 3px;
}

.extension-name strong {
  overflow: hidden;
  color: var(--text-primary, #1d2129);
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.extension-name code,
.version-state small {
  color: var(--text-tertiary, #86909c);
  font-size: 11px;
}

.extension-type,
.failure-state,
.extension-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

@media (max-width: 900px) {
  .extension-steps {
    grid-template-columns: 1fr 1fr;
  }

  .filter-bar {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
