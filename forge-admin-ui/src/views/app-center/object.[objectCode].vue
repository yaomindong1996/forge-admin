<template>
  <div class="object-detail-page">
    <header class="object-head">
      <n-button text @click="backToSuite">
        返回套件
      </n-button>
      <div class="object-title">
        <span>
          <n-icon><CubeOutline /></n-icon>
        </span>
        <div>
          <h1>{{ object?.objectName || objectCode }}</h1>
          <p>{{ object?.description || '业务对象详情' }}</p>
        </div>
        <DictTag v-if="object" dict-type="sys_enable_disable" :value="object.status" :bordered="false" />
      </div>
      <div class="object-actions">
        <n-button type="primary" @click="openDesigner('form')">
          <template #icon>
            <n-icon><BuildOutline /></n-icon>
          </template>
          设计对象
        </n-button>
        <n-button secondary :disabled="!runtimeInfo?.canOpen" @click="openRuntime">
          <template #icon>
            <n-icon><OpenOutline /></n-icon>
          </template>
          打开应用
        </n-button>
        <n-button secondary @click="scrollToReadiness">
          <template #icon>
            <n-icon><CheckmarkCircleOutline /></n-icon>
          </template>
          查看就绪度
        </n-button>
        <n-button secondary :disabled="!object" @click="toggleObject">
          <template #icon>
            <n-icon><PowerOutline /></n-icon>
          </template>
          {{ object?.status === 1 ? '停用对象' : '启用对象' }}
        </n-button>
        <n-button secondary type="error" :disabled="!object" @click="deleteObject">
          <template #icon>
            <n-icon><TrashOutline /></n-icon>
          </template>
          删除对象
        </n-button>
        <n-button v-if="canAdvanced" secondary @click="openDesigner('advanced')">
          <template #icon>
            <n-icon><SettingsOutline /></n-icon>
          </template>
          高级配置
        </n-button>
      </div>
    </header>

    <section ref="readinessSectionRef" class="readiness-section">
      <ReadinessPanel v-if="object" :object-id="object.id" @action="handleReadinessAction" />
    </section>

    <section class="operation-guide">
      <div class="guide-head">
        <div>
          <h2>交付路径</h2>
          <p>按设计对象、发布对象、运行应用三段检查当前对象，待处理步骤会进入设计器修复。</p>
        </div>
      </div>
      <div class="guide-steps">
        <button
          v-for="step in operationSteps"
          :key="step.key"
          type="button"
          class="guide-step"
          :class="`step-${step.state}`"
          @click="step.action()"
        >
          <span>{{ step.index }}</span>
          <div>
            <strong>{{ step.title }}</strong>
            <small>{{ step.desc }}</small>
          </div>
          <em>{{ step.stateLabel }}</em>
        </button>
      </div>
    </section>

    <section class="runtime-panel">
      <n-alert :type="runtimeStatusType" :show-icon="false">
        <div class="runtime-alert">
          <div>
            <strong>{{ runtimeInfo?.message || '正在加载运行态信息' }}</strong>
            <p>{{ runtimeHint }}</p>
          </div>
          <n-space :wrap="true">
            <n-button secondary @click="openDesigner('form')">
              <template #icon>
                <n-icon><BuildOutline /></n-icon>
              </template>
              设计对象
            </n-button>
            <n-button secondary @click="openDesigner('publish')">
              <template #icon>
                <n-icon><RocketOutline /></n-icon>
              </template>
              发布对象
            </n-button>
            <n-button secondary :disabled="!canUseRuntime" @click="openRuntime">
              <template #icon>
                <n-icon><OpenOutline /></n-icon>
              </template>
              运行应用
            </n-button>
            <n-button
              secondary
              :disabled="!canUseRuntime"
              :loading="templateLoading"
              @click="downloadImportTemplate"
            >
              <template #icon>
                <n-icon><CloudDownloadOutline /></n-icon>
              </template>
              导入模板
            </n-button>
            <n-button secondary :disabled="!canUseRuntime" :loading="importing" @click="triggerImport">
              <template #icon>
                <n-icon><CloudUploadOutline /></n-icon>
              </template>
              导入
            </n-button>
            <n-button secondary :disabled="!canUseRuntime" :loading="exportLoading" @click="exportData">
              <template #icon>
                <n-icon><CloudDownloadOutline /></n-icon>
              </template>
              导出
            </n-button>
          </n-space>
        </div>
      </n-alert>
      <input ref="importInputRef" class="hidden-file-input" type="file" accept=".xlsx,.xls" @change="handleImportFile">
    </section>

    <n-tabs v-model:value="activeTab" type="line" animated class="object-tabs">
      <n-tab-pane name="relation" tab="关联数据">
        <ObjectRelationPanel :object-id="object?.id" @action="handleReadinessAction" />
      </n-tab-pane>
      <n-tab-pane name="capability" tab="能力配置">
        <BusinessBindingPanel
          v-if="object"
          target-type="OBJECT"
          :target-id="object.id"
          :target-code="object.objectCode"
        />
      </n-tab-pane>
      <n-tab-pane v-if="canAdvanced" name="developer" tab="开发者信息">
        <div class="developer-info">
          <div><span>对象编码</span><code>{{ object?.objectCode || '-' }}</code></div>
          <div><span>模型编码</span><code>{{ object?.modelCode || '-' }}</code></div>
          <div><span>运行配置</span><code>{{ runtimeInfo?.configKey || '-' }}</code></div>
          <div><span>运行路由</span><code>{{ runtimeInfo?.routePath || '-' }}</code></div>
        </div>
      </n-tab-pane>
    </n-tabs>
    <BusinessObjectDesignerPage
      v-if="designerVisible"
      :key="designerMountKey"
      embedded
      :embedded-object-code="object?.objectCode || objectCode"
      :embedded-object-id="object?.id || null"
      :embedded-suite-code="suiteCode || ''"
      :initial-panel="designerPanel"
      @saved="loadObject"
      @close="closeDesigner"
    />
  </div>
</template>

<script setup>
import {
  BuildOutline,
  CheckmarkCircleOutline,
  CloudDownloadOutline,
  CloudUploadOutline,
  CubeOutline,
  OpenOutline,
  PowerOutline,
  RocketOutline,
  SettingsOutline,
  TrashOutline,
} from '@vicons/ionicons5'
import { useMessage } from 'naive-ui'
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  businessObjectList,
  businessObjectRuntimeInfo,
  deleteBusinessObject,
  dynamicCrudExport,
  dynamicCrudImport,
  dynamicCrudImportTemplate,
  updateBusinessObjectStatus,
} from '@/api/business-app'
import DictTag from '@/components/DictTag.vue'
import { useTabStore, useUserStore } from '@/store'
import BusinessBindingPanel from './components/BusinessBindingPanel.vue'
import ObjectRelationPanel from './components/ObjectRelationPanel.vue'
import ReadinessPanel from './components/ReadinessPanel.vue'
import BusinessObjectDesignerPage from './object-designer.[objectCode].vue'

const route = useRoute()
const router = useRouter()
const message = useMessage()
const tabStore = useTabStore()
const userStore = useUserStore()
const activeTab = ref('relation')
const object = ref(null)
const runtimeInfo = ref(null)
const importInputRef = ref(null)
const readinessSectionRef = ref(null)
const templateLoading = ref(false)
const importing = ref(false)
const exportLoading = ref(false)
const designerVisible = ref(false)
const designerPanel = ref('form')
const objectCode = computed(() => route.params.objectCode)
const suiteCode = computed(() => route.query.suiteCode || object.value?.suiteCode)
const pageTitle = computed(() => object.value?.objectName || objectCode.value || '业务对象详情')
const canUseRuntime = computed(() => Boolean(runtimeInfo.value?.canOpen && runtimeInfo.value?.configKey))
const runtimeStatusType = computed(() => runtimeInfo.value?.canOpen ? 'success' : 'warning')
const runtimeHint = computed(() => {
  if (runtimeInfo.value?.canOpen)
    return '对象已发布，可以打开业务应用，也可以继续使用导入、导出等运行能力。'
  if (runtimeInfo.value?.message)
    return '先进入对象设计器处理提示中的缺口，再打开业务应用或使用导入导出。'
  return '请先完成对象设计和发布检查，再生成标准业务应用入口。'
})
const designerMountKey = computed(() => `${object.value?.objectCode || objectCode.value}_${designerPanel.value}`)
const canAdvanced = computed(() => {
  return userStore.isAdmin
    || hasPermission(userStore.permissions, 'ai:businessObject:advanced')
    || hasPermission(userStore.apiPermissions, 'ai:businessObject:advanced')
    || hasPermission(userStore.getDataPermission, 'ai:businessObject:advanced')
})

const operationSteps = computed(() => [
  {
    key: 'design',
    index: '01',
    title: '设计对象',
    desc: '维护对象基础信息、字段、表单、列表和详情。',
    state: object.value?.modelCode ? 'done' : 'todo',
    stateLabel: object.value?.modelCode ? '已设计' : '去设计',
    action: () => openDesigner('form'),
  },
  {
    key: 'page',
    index: '02',
    title: '页面编排',
    desc: '检查列表、表单和详情页是否已生成。',
    state: runtimeInfo.value?.configKey ? 'done' : 'todo',
    stateLabel: runtimeInfo.value?.configKey ? '有配置' : '去搭建',
    action: () => openDesigner('form'),
  },
  {
    key: 'publish',
    index: '03',
    title: '发布对象',
    desc: '执行发布检查并生成可运行应用入口。',
    state: runtimeInfo.value?.canOpen ? 'done' : 'todo',
    stateLabel: runtimeInfo.value?.canOpen ? '已发布' : '去发布',
    action: () => openDesigner('publish'),
  },
  {
    key: 'runtime',
    index: '04',
    title: '运行应用',
    desc: '打开列表、导入、导出并验证数据。',
    state: canUseRuntime.value ? 'done' : 'locked',
    stateLabel: canUseRuntime.value ? '可打开' : '待就绪',
    action: canUseRuntime.value ? openRuntime : () => openDesigner('publish'),
  },
])

onMounted(loadObject)

watch(pageTitle, (title) => {
  if (!title)
    return
  route.meta.title = title
  document.title = `${title} | ${import.meta.env.VITE_TITLE}`
  tabStore.updateTabTitle(route.fullPath, title)
}, { immediate: true })

async function loadObject() {
  const res = await businessObjectList({
    suiteCode: suiteCode.value,
    objectCode: objectCode.value,
  })
  object.value = (res.data || [])[0] || null
  if (object.value) {
    const runtimeRes = await businessObjectRuntimeInfo(object.value.id)
    runtimeInfo.value = runtimeRes.data || null
  }
}

function backToSuite() {
  if (suiteCode.value)
    router.push(`/app-center/suite/${suiteCode.value}`)
  else
    router.push('/app-center')
}

function openRuntime() {
  if (!runtimeInfo.value?.canOpen) {
    message.warning(runtimeInfo.value?.message || '应用暂不可打开')
    return
  }
  router.push(runtimeInfo.value.routePath)
}

function openDesigner(panel = 'form') {
  const code = object.value?.objectCode || objectCode.value
  if (!code)
    return
  designerPanel.value = panel || 'form'
  designerVisible.value = true
}

async function closeDesigner() {
  designerVisible.value = false
  await loadObject()
}

function scrollToReadiness() {
  readinessSectionRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function handleReadinessAction(action) {
  switch (action) {
    case 'CREATE_OBJECT':
    case 'ENABLE_OBJECT':
      // 刷新页面数据
      loadObject()
      break
    case 'CREATE_APP_ENTRY':
    case 'ENABLE_APP_ENTRY':
      openDesigner('publish')
      break
    case 'CONFIGURE_MODEL':
      openDesigner('fields')
      break
    case 'CONFIGURE_RUNTIME':
    case 'PUBLISH_APP':
    case 'ENABLE_RUNTIME':
      openDesigner('publish')
      break
    case 'OPEN_RUNTIME':
      openRuntime()
      break
    case 'CONFIGURE_RELATIONS':
      openDesigner('relations')
      break
    case 'CONFIGURE_BINDINGS':
      activeTab.value = 'capability'
      break
    default:
      break
  }
}

async function toggleObject() {
  if (!object.value)
    return
  await updateBusinessObjectStatus(object.value.id, object.value.status === 1 ? 0 : 1)
  message.success(object.value.status === 1 ? '业务对象已停用' : '业务对象已启用')
  await loadObject()
}

function deleteObject() {
  if (!object.value)
    return
  window.$dialog?.warning({
    title: '删除业务对象',
    content: `确定删除“${object.value.objectName || object.value.objectCode}”吗？已关联关系或应用入口的对象会被后端拦截。`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      await deleteBusinessObject(object.value.id)
      message.success('业务对象已删除')
      backToSuite()
    },
  })
}

async function downloadImportTemplate() {
  if (!canUseRuntime.value) {
    message.warning('请先完成对象设计和发布')
    return
  }
  templateLoading.value = true
  try {
    const response = await dynamicCrudImportTemplate(runtimeInfo.value.configKey)
    downloadBlobResponse(response, `${object.value?.objectName || objectCode.value}-导入模板.xlsx`)
  }
  finally {
    templateLoading.value = false
  }
}

function triggerImport() {
  if (!canUseRuntime.value) {
    message.warning('请先完成对象设计和发布')
    return
  }
  importInputRef.value?.click()
}

async function handleImportFile(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file)
    return
  importing.value = true
  try {
    const res = await dynamicCrudImport(runtimeInfo.value.configKey, file)
    const result = res.data || {}
    if (result.success === false) {
      message.error(result.summary || '导入失败')
      return
    }
    message.success(result.summary || '导入完成')
  }
  finally {
    importing.value = false
  }
}

async function exportData() {
  if (!canUseRuntime.value) {
    message.warning('请先完成对象设计和发布')
    return
  }
  exportLoading.value = true
  try {
    const response = await dynamicCrudExport(runtimeInfo.value.configKey, {})
    if (response?.data?.async) {
      message.success(response.data.message || '导出任务已提交')
      return
    }
    downloadBlobResponse(response, `${object.value?.objectName || objectCode.value}-导出数据.xlsx`)
    message.success('导出成功')
  }
  finally {
    exportLoading.value = false
  }
}

function hasPermission(source, permission) {
  if (!Array.isArray(source))
    return false
  return source.includes(permission) || source.includes('**') || source.includes('*:*:*')
}

function downloadBlobResponse(response, fallbackName) {
  const blob = response?.data instanceof Blob ? response.data : response
  if (!(blob instanceof Blob)) {
    throw new TypeError('下载响应不是文件流')
  }
  const fileName = resolveDownloadFileName(response, fallbackName)
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

function resolveDownloadFileName(response, fallbackName) {
  const disposition = response?.headers?.['content-disposition']
    || response?.headers?.get?.('content-disposition')
    || ''
  const utf8Match = disposition.match(/filename\*=utf-8''([^;]+)/i)
  if (utf8Match?.[1])
    return decodeURIComponent(utf8Match[1])
  const normalMatch = disposition.match(/filename="?([^";]+)"?/i)
  if (normalMatch?.[1])
    return decodeURIComponent(normalMatch[1])
  return fallbackName
}
</script>

<style scoped>
.object-detail-page {
  min-height: 100%;
  background: #f6f8fb;
  padding: 20px;
}

.object-head,
.runtime-panel,
.operation-guide,
.readiness-section,
.object-tabs {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

.object-head {
  margin-bottom: 16px;
  padding: 16px;
}

.runtime-panel {
  margin-bottom: 16px;
  padding: 12px;
}

.operation-guide {
  margin-bottom: 16px;
  padding: 16px;
}

.readiness-section {
  margin-bottom: 16px;
}

.guide-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.guide-head h2 {
  margin: 0;
  color: #111827;
  font-size: 16px;
  letter-spacing: 0;
}

.guide-head p {
  margin: 5px 0 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}

.guide-steps {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.guide-step {
  display: grid;
  grid-template-columns: 36px minmax(0, 1fr);
  gap: 10px;
  align-items: start;
  min-width: 0;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 12px;
  cursor: pointer;
  text-align: left;
  transition:
    border-color 0.2s,
    box-shadow 0.2s,
    transform 0.2s;
}

.guide-step:hover {
  border-color: #93c5fd;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.06);
  transform: translateY(-1px);
}

.guide-step > span {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: #eff6ff;
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
}

.guide-step.step-done > span {
  background: #dcfce7;
  color: #16a34a;
}

.guide-step.step-locked > span {
  background: #f1f5f9;
  color: #64748b;
}

.guide-step strong,
.guide-step small,
.guide-step em {
  display: block;
}

.guide-step strong {
  overflow: hidden;
  color: #111827;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.guide-step small {
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.4;
}

.guide-step em {
  grid-column: 2;
  margin-top: 8px;
  color: #2563eb;
  font-size: 12px;
  font-style: normal;
  font-weight: 600;
}

.guide-step.step-done em {
  color: #16a34a;
}

.guide-step.step-locked em {
  color: #64748b;
}

.runtime-alert {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
}

.runtime-alert strong,
.runtime-alert p {
  display: block;
}

.runtime-alert strong {
  color: #111827;
  font-size: 14px;
}

.runtime-alert p {
  margin: 6px 0 0;
  color: #6b7280;
  font-size: 12px;
  line-height: 1.5;
}

.hidden-file-input {
  display: none;
}

.object-title {
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr) auto;
  gap: 14px;
  align-items: center;
  margin-top: 12px;
}

.object-title > span {
  display: grid;
  width: 48px;
  height: 48px;
  place-items: center;
  border-radius: 8px;
  background: #eef6ff;
  color: #2563eb;
  font-size: 24px;
}

.object-title h1 {
  margin: 0;
  color: #111827;
  font-size: 24px;
  letter-spacing: 0;
}

.object-title p {
  margin: 6px 0 0;
  color: #6b7280;
  font-size: 13px;
}

.object-actions {
  display: flex;
  gap: 10px;
  margin-top: 16px;
}

.object-tabs {
  padding: 8px 16px 16px;
}

.placeholder-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 12px;
}

.placeholder-item,
.developer-info > div {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 14px;
}

.placeholder-item strong {
  color: #111827;
  font-size: 14px;
}

.placeholder-item p {
  margin: 8px 0 0;
  color: #6b7280;
  font-size: 13px;
  line-height: 1.5;
}

.developer-info {
  display: grid;
  gap: 10px;
}

.developer-info > div {
  display: grid;
  grid-template-columns: 100px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
}

.developer-info span {
  color: #6b7280;
  font-size: 13px;
}

.developer-info code {
  overflow-wrap: anywhere;
  color: #111827;
  font-size: 13px;
}

@media (max-width: 620px) {
  .object-detail-page {
    padding: 12px;
  }

  .object-title {
    grid-template-columns: 42px minmax(0, 1fr);
  }

  .object-title :deep(.n-tag) {
    grid-column: 2;
    justify-self: start;
  }

  .object-actions {
    flex-direction: column;
  }

  .guide-steps {
    grid-template-columns: 1fr;
  }

  .runtime-alert {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
