<template>
  <NModal
    v-model:show="visible"
    preset="card"
    title="版本历史"
    style="width: 1080px"
    :mask-closable="false"
    @update:show="handleVisibleUpdate"
  >
    <div class="version-toolbar">
      <NSpace>
        <NButton size="small" @click="loadVersionList">
          <template #icon>
            <i class="i-material-symbols:refresh" />
          </template>
          刷新
        </NButton>
        <NButton size="small" type="primary" secondary @click="showCompare = true">
          <template #icon>
            <i class="i-material-symbols:compare-arrows" />
          </template>
          版本对比
        </NButton>
      </NSpace>
    </div>

    <NDataTable
      :columns="columns"
      :data="versionList"
      :pagination="pagination"
      :loading="loading"
      :row-key="row => row.id"
      :row-class-name="getRowClassName"
      size="small"
    />

    <NModal
      v-model:show="showDetail"
      preset="card"
      title="版本详情"
      style="width: 920px"
      @update:show="handleDetailVisibleUpdate"
    >
      <NSpin :show="detailLoading">
        <template v-if="versionDetail">
          <NDescriptions bordered :column="2" size="small" label-placement="left">
            <NDescriptionsItem label="版本号">
              <NSpace align="center" :size="8">
                <span>v{{ versionDetail.version }}</span>
                <NTag v-if="isCurrentVersion(versionDetail.version)" type="success" size="small">
                  当前版本
                </NTag>
              </NSpace>
            </NDescriptionsItem>
            <NDescriptionsItem label="版本名称">
              {{ versionDetail.versionName || '-' }}
            </NDescriptionsItem>
            <NDescriptionsItem label="版本标记">
              <DictTag dict-type="flow_version_tag" :value="versionDetail.versionTag" />
            </NDescriptionsItem>
            <NDescriptionsItem label="发布人">
              {{ versionDetail.publishBy || '-' }}
            </NDescriptionsItem>
            <NDescriptionsItem label="发布时间">
              {{ versionDetail.publishTime || '-' }}
            </NDescriptionsItem>
            <NDescriptionsItem label="Deployment ID">
              {{ versionDetail.deploymentId || '-' }}
            </NDescriptionsItem>
            <NDescriptionsItem label="流程定义 ID" :span="2">
              {{ versionDetail.processDefinitionId || '-' }}
            </NDescriptionsItem>
            <NDescriptionsItem label="变更说明" :span="2">
              {{ versionDetail.changeDescription || '-' }}
            </NDescriptionsItem>
          </NDescriptions>

          <NTabs v-model:value="activeDetailTab" type="line" class="mt-16" @update:value="handleDetailTabChange">
            <NTabPane name="diagram" tab="设计图">
              <div v-if="versionDetail.bpmnXml" class="version-diagram">
                <DingFlowViewer :bpmn-xml="versionDetail.bpmnXml" :compact="true" class="version-diagram-canvas" />
              </div>
              <NEmpty v-else description="该版本暂无 BPMN XML，无法渲染设计图" />
            </NTabPane>
            <NTabPane name="bpmn" tab="BPMN XML">
              <div class="version-code-toolbar">
                <NButton
                  size="small"
                  secondary
                  :disabled="!versionDetail.bpmnXml"
                  @click="handleCopyXml"
                >
                  <template #icon>
                    <i class="i-material-symbols:content-copy-outline" />
                  </template>
                  复制XML
                </NButton>
              </div>
              <NCode
                :code="versionDetail.bpmnXml || '无'"
                language="xml"
                :show-line-numbers="true"
                :word-wrap="true"
                class="version-code"
              />
            </NTabPane>
            <NTabPane name="form" tab="表单配置">
              <NCode
                :code="formatJson(versionDetail.formJson)"
                language="json"
                :show-line-numbers="true"
                :word-wrap="true"
                class="version-code"
              />
            </NTabPane>
          </NTabs>
        </template>
      </NSpin>
    </NModal>

    <NModal
      v-model:show="showRevert"
      preset="card"
      title="版本回退"
      style="width: 560px"
      :mask-closable="false"
    >
      <NAlert type="warning" class="mb-16">
        将回退到 v{{ revertTarget?.version }}。系统会基于该历史版本发布一个新版本，正在运行的流程实例继续按旧版本执行。
      </NAlert>
      <NForm ref="revertFormRef" :model="revertForm" :rules="revertRules" label-placement="top">
        <NFormItem label="变更说明" path="changeDescription">
          <NInput
            v-model:value="revertForm.changeDescription"
            type="textarea"
            placeholder="请输入回退原因，至少 10 个字符"
            :rows="4"
            maxlength="500"
            show-count
          />
        </NFormItem>
      </NForm>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="showRevert = false">
            取消
          </NButton>
          <NButton type="warning" :loading="revertLoading" @click="submitRevert">
            确认回退
          </NButton>
        </NSpace>
      </template>
    </NModal>

    <NModal
      v-model:show="showTag"
      preset="card"
      title="版本标记"
      style="width: 420px"
      :mask-closable="false"
    >
      <NForm label-placement="top">
        <NFormItem label="版本">
          v{{ tagTarget?.version }} {{ tagTarget?.versionName || '' }}
        </NFormItem>
        <NFormItem label="标记">
          <NSelect v-model:value="tagForm.versionTag" :options="versionTagOptions" />
        </NFormItem>
      </NForm>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="showTag = false">
            取消
          </NButton>
          <NButton type="primary" :loading="tagLoading" @click="submitTag">
            保存
          </NButton>
        </NSpace>
      </template>
    </NModal>

    <VersionCompare
      v-if="showCompare"
      :model-id="modelId"
      @close="showCompare = false"
    />
  </NModal>
</template>

<script setup>
import { DingFlowViewer } from '@/components/flow-designer'
import { NButton, NDropdown, NTag } from 'naive-ui'
import { computed, h, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import versionApi from '@/api/version'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'
import { useAuthStore } from '@/store'
import { generateUUID } from '@/utils/common'
import VersionCompare from './versionCompare.vue'

const props = defineProps({
  modelId: {
    type: [String, Number],
    required: true,
  },
  currentVersion: {
    type: [String, Number],
    default: null,
  },
})
const emit = defineEmits(['close', 'refresh'])

const { dict } = useDict('flow_version_tag')

const versionTagOptions = computed(() => dict.value.flow_version_tag || [])

const visible = ref(true)
const loading = ref(false)
const detailLoading = ref(false)
const revertLoading = ref(false)
const tagLoading = ref(false)
const showDetail = ref(false)
const showRevert = ref(false)
const showTag = ref(false)
const showCompare = ref(false)
const versionList = ref([])
const versionDetail = ref(null)
const revertTarget = ref(null)
const tagTarget = ref(null)
const revertFormRef = ref(null)
const activeDetailTab = ref('diagram')
const authStore = useAuthStore()
const currentVersionValue = ref(props.currentVersion)

const revertForm = reactive({
  changeDescription: '',
})
const tagForm = reactive({
  versionTag: 'test',
})
const revertRules = {
  changeDescription: [
    { required: true, message: '请输入变更说明', trigger: 'blur' },
    { min: 10, message: '变更说明长度不足，至少 10 字符', trigger: 'blur' },
    { max: 500, message: '变更说明不能超过 500 字符', trigger: 'blur' },
  ],
}

const pagination = reactive({
  page: 1,
  pageSize: 20,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50],
  onChange: (page) => {
    pagination.page = page
    loadVersionList()
  },
  onUpdatePageSize: (pageSize) => {
    pagination.pageSize = pageSize
    pagination.page = 1
    loadVersionList()
  },
})

const columns = [
  {
    title: '版本号',
    key: 'version',
    width: 130,
    render: (row) => {
      const current = isCurrentVersion(row.version)
      return h('div', { class: 'version-cell' }, [
        h('span', null, `v${row.version}`),
        current ? h(NTag, { type: 'success', size: 'small' }, { default: () => '当前版本' }) : null,
      ])
    },
  },
  { title: '版本名称', key: 'versionName', minWidth: 120 },
  {
    title: '版本标记',
    key: 'versionTag',
    width: 120,
    render: row => h(DictTag, { dictType: 'flow_version_tag', value: row.versionTag }),
  },
  { title: '变更说明', key: 'changeDescription', minWidth: 180, ellipsis: { tooltip: true } },
  { title: '发布人', key: 'publishBy', width: 110 },
  { title: '发布时间', key: 'publishTime', width: 170 },
  {
    title: '操作',
    key: 'action',
    width: 180,
    fixed: 'right',
    render: (row) => {
      return h('div', { class: 'version-actions' }, [
        actionButton('详情', 'primary', () => handleDetail(row)),
        actionButton('回退', 'warning', () => handleRevert(row)),
        h(NDropdown, {
          trigger: 'click',
          options: getMoreActionOptions(),
          onSelect: key => handleMoreAction(key, row),
        }, {
          default: () => h(NButton, { size: 'small', text: true }, { default: () => '更多' }),
        }),
      ])
    },
  },
]

function actionButton(label, type, onClick) {
  return h(NButton, {
    size: 'small',
    text: true,
    type,
    onClick,
  }, { default: () => label })
}

function getMoreActionOptions() {
  return [
    { label: '版本对比', key: 'compare' },
    { label: '修改标记', key: 'tag' },
    { label: '下载 BPMN', key: 'download' },
    { type: 'divider', key: 'd1' },
    { label: '删除版本', key: 'delete', props: { style: 'color: #d03050' } },
  ]
}

function handleMoreAction(key, row) {
  const map = {
    compare: () => {
      showCompare.value = true
    },
    tag: () => handleTag(row),
    download: () => handleDownload(row),
    delete: () => handleDelete(row),
  }
  map[key]?.()
}

function isCurrentVersion(version) {
  if (currentVersionValue.value === null || currentVersionValue.value === undefined)
    return false
  return String(version) === String(currentVersionValue.value)
}

function getRowClassName(row) {
  return isCurrentVersion(row.version) ? 'current-version-row' : ''
}

function handleVisibleUpdate(show) {
  if (!show)
    emit('close')
}

function handleDetailVisibleUpdate(show) {
  if (!show)
    destroyDetailViewer()
}

async function loadVersionList() {
  loading.value = true
  try {
    const res = await versionApi.getVersionList(props.modelId, pagination.page, pagination.pageSize)
    versionList.value = res.data?.records || []
    pagination.itemCount = res.data?.total || 0
  }
  catch (error) {
    console.error('加载版本列表失败', error)
  }
  finally {
    loading.value = false
  }
}

async function handleDetail(row) {
  showDetail.value = true
  detailLoading.value = true
  versionDetail.value = null
  activeDetailTab.value = 'diagram'
  destroyDetailViewer()
  try {
    const res = await versionApi.getVersionDetail(row.id)
    versionDetail.value = res.data
    await nextTick()
    await renderVersionDiagram()
  }
  catch (error) {
    console.error('加载版本详情失败', error)
  }
  finally {
    detailLoading.value = false
  }
}

async function handleDetailTabChange(tabName) {
  if (tabName === 'diagram') {
    await nextTick()
    await renderVersionDiagram()
  }
}

// DingFlowViewer 通过 :bpmn-xml prop 自动渲染，无需手动 importXML
async function renderVersionDiagram() {
  // no-op: DingFlowViewer 接收 bpmnXml 后自动解析渲染
}

// DingFlowViewer 自行管理生命周期，无需手动 destroy
function destroyDetailViewer() {
  // no-op
}

function handleRevert(row) {
  revertTarget.value = row
  revertForm.changeDescription = ''
  showRevert.value = true
}

async function submitRevert() {
  try {
    await revertFormRef.value?.validate()
    revertLoading.value = true
    const res = await versionApi.revertVersion({
      modelId: String(props.modelId),
      targetVersion: revertTarget.value.version,
      changeDescription: revertForm.changeDescription.trim(),
    })
    const runningInstances = res.data?.runningInstances ?? 0
    if (res.data?.newVersion !== undefined && res.data?.newVersion !== null) {
      currentVersionValue.value = res.data.newVersion
    }
    window.$message?.success(res.message || `回退成功，正在运行的 ${runningInstances} 个实例将继续按旧版本执行`)
    showRevert.value = false
    loadVersionList()
    emit('refresh')
  }
  catch (error) {
    console.error('版本回退失败', error)
  }
  finally {
    revertLoading.value = false
  }
}

function handleTag(row) {
  tagTarget.value = row
  tagForm.versionTag = row.versionTag || 'test'
  showTag.value = true
}

async function submitTag() {
  if (!tagTarget.value)
    return
  tagLoading.value = true
  try {
    await versionApi.updateVersionTag(tagTarget.value.id, tagForm.versionTag)
    window.$message?.success('版本标记更新成功')
    showTag.value = false
    loadVersionList()
  }
  catch (error) {
    console.error('版本标记更新失败', error)
  }
  finally {
    tagLoading.value = false
  }
}

async function handleDownload(row) {
  try {
    window.$message?.loading('正在下载版本文件...', { duration: 0, key: 'version-download' })
    const baseUrl = import.meta.env.VITE_REQUEST_PREFIX || ''
    const response = await fetch(`${baseUrl}/api/flow/model/version/download/${row.id}`, {
      method: 'GET',
      headers: {
        'Authorization': authStore.accessToken ? `Bearer ${authStore.accessToken}` : '',
        'X-Timestamp': Date.now().toString(),
        'X-Nonce': generateUUID(),
      },
    })

    if (!response.ok) {
      const text = await response.text()
      throw new Error(text || response.statusText || '下载失败')
    }

    const blob = await response.blob()
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = getDownloadFilename(response, row)
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    setTimeout(() => window.URL.revokeObjectURL(url), 100)
    window.$message?.success('版本文件下载成功', { key: 'version-download' })
  }
  catch (error) {
    console.error('版本下载失败', error)
    window.$message?.error('版本文件下载失败', { key: 'version-download' })
  }
}

function getDownloadFilename(response, row) {
  const disposition = response.headers.get('Content-Disposition') || response.headers.get('content-disposition')
  const fallback = `${row.versionName || `v${row.version}`}.bpmn20.xml`
  if (!disposition)
    return fallback

  const encodedMatch = disposition.match(/filename\*=UTF-8''([^;]+)/i)
  if (encodedMatch?.[1])
    return decodeURIComponent(encodedMatch[1])

  const quotedMatch = disposition.match(/filename="?([^";]+)"?/i)
  if (quotedMatch?.[1])
    return decodeURIComponent(quotedMatch[1])

  return fallback
}

async function handleCopyXml() {
  const xml = versionDetail.value?.bpmnXml || ''
  if (!xml) {
    window.$message?.warning('暂无可复制的 XML')
    return
  }

  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(xml)
    }
    else {
      const textarea = document.createElement('textarea')
      textarea.value = xml
      textarea.style.position = 'fixed'
      textarea.style.opacity = '0'
      document.body.appendChild(textarea)
      textarea.select()
      document.execCommand('copy')
      document.body.removeChild(textarea)
    }
    window.$message?.success('XML 已复制')
  }
  catch (error) {
    console.error('复制 XML 失败', error)
    window.$message?.error('复制失败')
  }
}

function handleDelete(row) {
  window.$dialog?.warning({
    title: '确认删除',
    content: `确定要删除版本「${row.versionName || `v${row.version}`}」吗？已发布版本和已废弃版本不允许删除。`,
    positiveText: '确定删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      await versionApi.deleteVersion(row.id)
      window.$message?.success('删除成功')
      loadVersionList()
    },
  })
}

function formatJson(value) {
  if (!value)
    return '无'
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  }
  catch {
    return value
  }
}

onMounted(() => {
  loadVersionList()
})

watch(
  () => props.currentVersion,
  (value) => {
    currentVersionValue.value = value
  },
)

onUnmounted(() => {
  destroyDetailViewer()
})
</script>

<style scoped>
.version-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}

.version-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  white-space: nowrap;
}

.version-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.current-version-row td {
  background: #f0f9ff !important;
}

.version-diagram {
  height: 460px;
  overflow: hidden;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
}

.version-diagram-canvas {
  width: 100%;
  height: 100%;
}

.version-code-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 8px;
}

.version-code {
  max-height: 420px;
  overflow: auto;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}
</style>
