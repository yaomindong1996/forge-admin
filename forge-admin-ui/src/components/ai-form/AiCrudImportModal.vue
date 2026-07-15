<template>
  <NModal
    :show="show"
    title="批量导入"
    preset="card"
    class="ai-crud-import-modal"
    :style="modalStyle"
    :mask-closable="false"
    :close-on-esc="!importing"
    @update:show="handleVisibleChange"
  >
    <div class="import-workspace">
      <NAlert v-if="stage === 'select'" type="info" :bordered="false">
        选择 Excel 后会先在浏览器本地预览前 {{ previewLimit }} 行，确认无误后再提交导入。
      </NAlert>

      <NUpload
        v-if="stage === 'select' || stage === 'preview'"
        v-model:file-list="fileList"
        :default-upload="false"
        :max="1"
        accept=".xlsx,.xls"
        :disabled="parsing"
        @change="handleFileChange"
        @remove="handleFileRemove"
      >
        <NUploadDragger>
          <div class="upload-icon">
            <NIcon size="42" :depth="3">
              <CloudUploadOutline />
            </NIcon>
          </div>
          <NText class="upload-title">
            点击或拖动 Excel 文件到此处
          </NText>
          <NP depth="3" class="upload-hint">
            支持 .xlsx、.xls，文件只在点击“开始导入”后上传
          </NP>
        </NUploadDragger>
      </NUpload>

      <div v-if="parsing" class="import-loading">
        <NSpin size="small" />
        <span>正在解析预览数据...</span>
      </div>

      <section v-if="stage === 'preview' && !parsing" class="preview-section">
        <div class="section-heading">
          <div>
            <strong>数据预览</strong>
            <span>共 {{ preview.totalRows }} 行数据</span>
          </div>
          <NTag v-if="preview.truncated" size="small" :bordered="false">
            仅展示前 {{ preview.rows.length }} 行
          </NTag>
        </div>
        <NDataTable
          :columns="previewColumns"
          :data="preview.rows"
          :row-key="row => row.__rowNumber"
          :max-height="300"
          :scroll-x="previewScrollX"
          size="small"
          bordered
        />
      </section>

      <section v-if="stage === 'importing'" class="progress-section">
        <div class="progress-heading">
          <strong>{{ processing ? '服务端正在校验并导入' : '正在上传文件' }}</strong>
          <span>{{ processing ? '上传已完成，请勿关闭窗口' : `${uploadPercent}%` }}</span>
        </div>
        <NProgress
          type="line"
          :percentage="uploadPercent"
          :processing="processing"
          :status="processing ? 'success' : 'default'"
          :show-indicator="!processing"
        />
        <div v-if="processing" class="processing-tip">
          <NSpin size="small" />
          <span>文件已上传，正在执行格式校验、数据校验和写入...</span>
        </div>
      </section>

      <section v-if="stage === 'result' && result" class="result-section">
        <NAlert :type="resultAlertType" :title="resultTitle" :bordered="false">
          {{ result.summary }}
        </NAlert>

        <div class="result-metrics">
          <div class="result-metric">
            <span>总行数</span>
            <strong>{{ result.totalRows }}</strong>
          </div>
          <div class="result-metric result-metric--success">
            <span>成功</span>
            <strong>{{ result.successRows }}</strong>
          </div>
          <div class="result-metric result-metric--error">
            <span>失败</span>
            <strong>{{ result.failedRows }}</strong>
          </div>
          <div class="result-metric">
            <span>成功率</span>
            <strong>{{ successRate }}%</strong>
          </div>
        </div>

        <div v-if="result.errors.length" class="error-section">
          <div class="section-heading">
            <div>
              <strong>失败原因</strong>
              <span>共 {{ result.errors.length }} 条错误</span>
            </div>
            <NTag v-if="result.errors.length > errorDisplayLimit" type="warning" size="small" :bordered="false">
              仅展示前 {{ errorDisplayLimit }} 条
            </NTag>
          </div>
          <NDataTable
            :columns="errorColumns"
            :data="displayErrors"
            :row-key="row => row.__errorKey"
            :max-height="280"
            :scroll-x="760"
            size="small"
            bordered
          />
        </div>
      </section>
    </div>

    <template #footer>
      <NSpace justify="space-between" align="center">
        <NButton v-if="hasTemplate" text :disabled="importing" @click="downloadTemplate">
          <template #icon>
            <NIcon><DownloadOutline /></NIcon>
          </template>
          下载导入模板
        </NButton>
        <span v-else />

        <NSpace>
          <NButton v-if="stage === 'result'" :disabled="importing" @click="resetImport">
            继续导入
          </NButton>
          <NButton :disabled="importing" @click="closeModal">
            {{ stage === 'result' ? '关闭' : '取消' }}
          </NButton>
          <NButton
            v-if="stage === 'preview'"
            type="primary"
            :disabled="!selectedFile || preview.totalRows <= 0"
            @click="submitImport"
          >
            开始导入
          </NButton>
        </NSpace>
      </NSpace>
    </template>
  </NModal>
</template>

<script setup>
import { CloudUploadOutline, DownloadOutline } from '@vicons/ionicons5'
import {
  NAlert,
  NButton,
  NDataTable,
  NIcon,
  NModal,
  NP,
  NProgress,
  NSpace,
  NSpin,
  NTag,
  NText,
  NUpload,
  NUploadDragger,
} from 'naive-ui'
import { computed, ref, watch } from 'vue'
import { buildImportPreview, normalizeImportResult } from './import-utils'

const props = defineProps({
  show: { type: Boolean, default: false },
  importer: { type: Function, required: true },
  templateDownloader: { type: Function, default: null },
  hasTemplate: { type: Boolean, default: false },
  previewLimit: { type: Number, default: 20 },
})

const emit = defineEmits(['update:show', 'success'])

const modalStyle = { width: 'min(980px, calc(100vw - 32px))' }
const errorDisplayLimit = 100
const stage = ref('select')
const fileList = ref([])
const selectedFile = ref(null)
const parsing = ref(false)
const importing = ref(false)
const processing = ref(false)
const uploadPercent = ref(0)
const preview = ref({ headers: [], rows: [], totalRows: 0, truncated: false })
const result = ref(null)

const previewColumns = computed(() => [
  { title: 'Excel行', key: '__rowNumber', width: 76, fixed: 'left', align: 'center' },
  ...preview.value.headers.map((title, index) => ({
    title,
    key: `column_${index}`,
    minWidth: 140,
    ellipsis: { tooltip: true },
  })),
])
const previewScrollX = computed(() => Math.max(760, 76 + preview.value.headers.length * 140))
const displayErrors = computed(() => result.value?.errors?.slice(0, errorDisplayLimit) || [])
const successRate = computed(() => {
  if (!result.value?.totalRows)
    return result.value?.success ? 100 : 0
  return Math.round((result.value.successRows / result.value.totalRows) * 100)
})
const resultAlertType = computed(() => {
  if (result.value?.failedRows > 0 && result.value?.successRows > 0)
    return 'warning'
  return result.value?.success ? 'success' : 'error'
})
const resultTitle = computed(() => {
  if (result.value?.failedRows > 0 && result.value?.successRows > 0)
    return '导入完成，部分数据失败'
  return result.value?.success ? '导入完成' : '导入失败'
})
const errorColumns = [
  { title: '行号', key: 'rowNum', width: 70, fixed: 'left' },
  { title: '字段', key: 'label', width: 130, ellipsis: { tooltip: true } },
  { title: '原始值', key: 'rawValue', width: 150, ellipsis: { tooltip: true } },
  { title: '失败原因', key: 'message', minWidth: 220, ellipsis: { tooltip: true } },
  { title: '修正建议', key: 'suggestion', minWidth: 180, ellipsis: { tooltip: true } },
]

watch(() => props.show, (visible) => {
  if (visible)
    resetImport()
})

async function handleFileChange(options) {
  const nextFileList = Array.isArray(options?.fileList) ? options.fileList.slice(-1) : []
  fileList.value = nextFileList
  const fileInfo = nextFileList[0]
  const file = fileInfo?.file
  if (!file) {
    resetFileState()
    return
  }
  selectedFile.value = file
  await parsePreview(file)
}

function handleFileRemove() {
  resetFileState()
  return true
}

async function parsePreview(file) {
  parsing.value = true
  result.value = null
  try {
    const XLSX = await import('xlsx')
    const buffer = await file.arrayBuffer()
    const workbook = XLSX.read(buffer, { type: 'array', cellDates: true })
    const firstSheetName = workbook.SheetNames?.[0]
    if (!firstSheetName)
      throw new Error('Excel 中没有可读取的工作表')
    const rows = XLSX.utils.sheet_to_json(workbook.Sheets[firstSheetName], {
      header: 1,
      defval: '',
      blankrows: false,
      raw: false,
      dateNF: 'yyyy-mm-dd hh:mm:ss',
    })
    preview.value = buildImportPreview(rows, props.previewLimit)
    if (preview.value.totalRows <= 0)
      throw new Error('Excel 中没有可导入的数据行')
    stage.value = 'preview'
  }
  catch (error) {
    resetFileState()
    window.$message.error(error?.message || 'Excel 预览解析失败')
  }
  finally {
    parsing.value = false
  }
}

async function submitImport() {
  if (!selectedFile.value || importing.value)
    return

  stage.value = 'importing'
  importing.value = true
  processing.value = false
  uploadPercent.value = 0
  try {
    const payload = await props.importer(selectedFile.value, (percent) => {
      uploadPercent.value = Math.max(0, Math.min(100, Math.round(Number(percent) || 0)))
      if (uploadPercent.value >= 100)
        processing.value = true
    })
    uploadPercent.value = 100
    processing.value = false
    result.value = normalizeImportResult(payload)
    stage.value = 'result'
    if (result.value.successRows > 0)
      emit('success', result.value)
  }
  catch (error) {
    processing.value = false
    result.value = normalizeImportResult({
      success: false,
      totalRows: preview.value.totalRows,
      successRows: 0,
      failedRows: preview.value.totalRows,
      summary: error?.message || '导入请求失败',
      errors: [{ errorMessage: error?.message || '导入请求失败' }],
    })
    stage.value = 'result'
  }
  finally {
    importing.value = false
  }
}

async function downloadTemplate() {
  if (!props.templateDownloader)
    return
  try {
    await props.templateDownloader()
  }
  catch (error) {
    window.$message.error(error?.message || '下载导入模板失败')
  }
}

function handleVisibleChange(visible) {
  if (!visible)
    closeModal()
}

function closeModal() {
  if (importing.value)
    return
  emit('update:show', false)
}

function resetFileState() {
  fileList.value = []
  selectedFile.value = null
  preview.value = { headers: [], rows: [], totalRows: 0, truncated: false }
  stage.value = 'select'
}

function resetImport() {
  parsing.value = false
  importing.value = false
  processing.value = false
  uploadPercent.value = 0
  result.value = null
  resetFileState()
}
</script>

<style scoped>
.import-workspace {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.upload-icon {
  margin-bottom: 10px;
}

.upload-title {
  font-size: 15px;
  font-weight: 600;
}

.upload-hint {
  margin: 7px 0 0;
}

.import-loading,
.processing-tip,
.progress-heading,
.section-heading,
.section-heading > div {
  display: flex;
  align-items: center;
}

.import-loading,
.processing-tip {
  justify-content: center;
  gap: 8px;
  color: var(--text-color-2);
}

.preview-section,
.progress-section,
.result-section,
.error-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.section-heading,
.progress-heading {
  justify-content: space-between;
  gap: 16px;
}

.section-heading > div {
  gap: 10px;
}

.section-heading span,
.progress-heading span {
  color: var(--text-color-3);
  font-size: 12px;
}

.processing-tip {
  padding: 12px;
  background: var(--action-color);
  border-radius: 6px;
}

.result-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.result-metric {
  padding: 14px 16px;
  background: var(--card-color);
  border: 1px solid var(--border-color);
  border-radius: 7px;
}

.result-metric span {
  display: block;
  margin-bottom: 5px;
  color: var(--text-color-3);
  font-size: 12px;
}

.result-metric strong {
  color: var(--text-color-1);
  font-size: 22px;
  font-variant-numeric: tabular-nums;
}

.result-metric--success strong {
  color: var(--success-color);
}

.result-metric--error strong {
  color: var(--error-color);
}

@media (max-width: 640px) {
  .result-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
