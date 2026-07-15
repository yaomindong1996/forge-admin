<template>
  <div class="application-preview-page">
    <header class="preview-page-header">
      <div class="preview-identity">
        <n-button quaternary circle aria-label="返回应用工作台" @click="backToWorkspace">
          <template #icon>
            <n-icon><ArrowBackOutline /></n-icon>
          </template>
        </n-button>
        <div>
          <span>应用草稿预览</span>
          <h1>{{ application?.applicationName || '预览应用' }}</h1>
          <p>直接读取对象设计草稿，不依赖菜单或页面入口。</p>
        </div>
      </div>
      <div class="preview-actions">
        <n-select
          v-if="objectOptions.length > 1"
          class="object-select"
          :value="selectedObjectId"
          :options="objectOptions"
          placeholder="切换预览对象"
          @update:value="selectObject"
        />
        <n-button secondary :disabled="!selectedObject" @click="openDesigner">
          继续设计
        </n-button>
        <n-button type="primary" @click="backToWorkspace">
          返回应用
        </n-button>
      </div>
    </header>

    <main class="preview-page-content">
      <n-spin :show="loading" description="正在准备应用草稿预览...">
        <LowcodePreviewPane
          v-if="previewDraft"
          :app-id="designer?.configId"
          :draft="previewDraft"
        />

        <n-result
          v-else-if="!loading && application && !objects.length"
          status="info"
          title="应用还没有数据对象"
          description="先创建或关联一个对象，即可在不配置页面入口的情况下预览表单和列表。"
        >
          <template #footer>
            <n-button type="primary" @click="openObjects">
              配置数据对象
            </n-button>
          </template>
        </n-result>

        <n-result
          v-else-if="!loading && !application"
          status="404"
          title="应用不存在或无权访问"
        >
          <template #footer>
            <n-button type="primary" @click="router.push('/app-center')">
              返回应用总览
            </n-button>
          </template>
        </n-result>
      </n-spin>
    </main>
  </div>
</template>

<script setup>
import { ArrowBackOutline } from '@vicons/ionicons5'
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { businessObjectDesigner } from '@/api/business-app'
import { businessApplicationWorkspaceByCode } from '@/api/business-application'
import LowcodePreviewPane from '@/components/lowcode-builder/preview/LowcodePreviewPane.vue'

const route = useRoute()
const router = useRouter()
const application = ref(null)
const workspace = ref(null)
const designer = ref(null)
const selectedObjectId = ref(null)
const loading = ref(false)

const objects = computed(() => workspace.value?.objects || [])
const objectOptions = computed(() => objects.value.map(item => ({
  label: `${item.objectName || item.objectCode}${item.objectRole === 'PRIMARY' ? '（主对象）' : ''}`,
  value: item.objectId,
})))
const selectedObject = computed(() => objects.value.find(
  item => String(item.objectId) === String(selectedObjectId.value),
) || null)
const previewDraft = computed(() => {
  if (!designer.value?.modelSchema || !designer.value?.pageSchema)
    return null
  return {
    configKey: designer.value.configKey,
    appName: designer.value.objectName,
    publishStatus: designer.value.publishStatus || 'DRAFT',
    modelSchema: designer.value.modelSchema,
    pageSchema: designer.value.pageSchema,
    formDesignerSchema: designer.value.formDesignerSchema,
    viewSchema: designer.value.viewSchema,
    linkageSchema: designer.value.linkageSchema,
  }
})

watch(() => route.params.applicationCode, loadPreview)
onMounted(loadPreview)

async function loadPreview() {
  const applicationCode = String(route.params.applicationCode || '')
  if (!applicationCode)
    return
  loading.value = true
  try {
    const response = await businessApplicationWorkspaceByCode(applicationCode)
    workspace.value = response.data || null
    application.value = workspace.value?.application || null
    const requestedObjectId = route.query.objectId
    const initialObject = objects.value.find(item => String(item.objectId) === String(requestedObjectId))
      || objects.value.find(item => item.objectRole === 'PRIMARY')
      || objects.value[0]
    selectedObjectId.value = initialObject?.objectId || null
    await loadObjectDraft()
  }
  catch {
    application.value = null
    workspace.value = null
    designer.value = null
  }
  finally {
    loading.value = false
  }
}

async function loadObjectDraft() {
  if (!selectedObjectId.value) {
    designer.value = null
    return
  }
  const response = await businessObjectDesigner(selectedObjectId.value)
  designer.value = response.data || null
}

async function selectObject(objectId) {
  if (!objectId || String(objectId) === String(selectedObjectId.value))
    return
  selectedObjectId.value = objectId
  designer.value = null
  loading.value = true
  try {
    await loadObjectDraft()
    router.replace({
      query: {
        ...route.query,
        objectId,
      },
    })
  }
  finally {
    loading.value = false
  }
}

function backToWorkspace() {
  if (!application.value?.applicationCode) {
    router.push('/app-center')
    return
  }
  router.push({
    name: 'BusinessApplicationWorkspace',
    params: { applicationCode: application.value.applicationCode },
  })
}

function openObjects() {
  router.push({
    name: 'BusinessApplicationWorkspace',
    params: { applicationCode: route.params.applicationCode },
    query: { section: 'objects' },
  })
}

function openDesigner() {
  if (!selectedObject.value?.objectCode)
    return
  router.push({
    name: 'BusinessObjectDesigner',
    params: { objectCode: selectedObject.value.objectCode },
    query: {
      objectId: selectedObject.value.objectId,
      suiteCode: application.value?.suiteCode,
      panel: 'list',
      returnTo: route.fullPath,
    },
  })
}
</script>

<style scoped>
.application-preview-page {
  min-height: 100%;
  color: var(--text-primary, #1d2129);
  background: var(--bg-secondary, #f7f8fa);
}

.preview-page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  min-height: 74px;
  padding: 10px 18px;
  border-bottom: 1px solid var(--border-default, #c9cdd4);
  background: var(--bg-primary, #fff);
}

.preview-identity,
.preview-actions {
  display: flex;
  align-items: center;
}

.preview-identity {
  min-width: 0;
  gap: 10px;
}

.preview-identity > div {
  display: grid;
  gap: 1px;
}

.preview-identity span,
.preview-identity p {
  color: var(--text-tertiary, #86909c);
  font-size: 11px;
}

.preview-identity h1,
.preview-identity p {
  margin: 0;
}

.preview-identity h1 {
  font-size: 17px;
}

.preview-actions {
  flex: 0 0 auto;
  gap: 8px;
}

.object-select {
  width: 220px;
}

.preview-page-content {
  padding: 14px 18px 20px;
}

.preview-page-content :deep(.preview-pane) {
  border: 1px solid var(--border-default, #c9cdd4);
  border-radius: 8px;
  background: var(--bg-primary, #fff);
}

@media (max-width: 760px) {
  .preview-page-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .preview-actions {
    width: 100%;
    flex-wrap: wrap;
  }

  .object-select {
    width: 100%;
  }
}
</style>
