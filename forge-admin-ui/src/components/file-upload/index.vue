<template>
  <div class="file-upload-wrapper">
    <NUpload
      ref="uploadRef"
      :action="uploadUrl"
      :headers="headers"
      :data="uploadData"
      :max="limit"
      :accept="acceptTypes"
      :multiple="multiple"
      :file-list="fileList"
      :on-before-upload="handleBeforeUpload"
      :on-finish="handleFinish"
      :on-error="handleError"
      :on-remove="handleRemove"
      :on-preview="handlePreview"
      :on-download="handleDownload"
      :disabled="disabled"
      :show-file-list="false"
      @update:file-list="handleFileListChange"
    >
      <!-- 自定义上传触发区 -->
      <div v-if="!disabled && (!limit || fileList.length < limit)" class="upload-dropzone">
        <NIcon size="24" class="upload-dropzone-icon">
          <CloudUploadOutline />
        </NIcon>
        <div class="upload-dropzone-text">
          <span class="upload-dropzone-main">{{ uploadButtonText }}</span>
          <span v-if="uploadHint" class="upload-dropzone-hint">{{ uploadHint }}</span>
        </div>
      </div>
    </NUpload>

    <!-- 自定义文件列表 -->
    <div v-if="displayFiles.length" class="custom-file-list">
      <div
        v-for="file in displayFiles"
        :key="file.id"
        class="file-card"
        :class="{ 'is-error': file.status === 'error', 'is-uploading': file.status === 'uploading' }"
      >
        <div class="file-icon">
          <NIcon size="22" :color="getFileIconColor(file)">
            <component :is="getFileTypeIcon(file)" />
          </NIcon>
        </div>

        <div class="file-body">
          <div class="file-row">
            <span class="file-name" :title="file.name">
              {{ file.name }}
              <NIcon
                v-if="file.status === 'finished' && !disabled"
                size="13"
                class="file-rename-trigger"
                title="重命名"
                @click="openRename(file)"
              >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="12" height="12"><path d="M17 3a2.83 2.83 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5Z" /></svg>
              </NIcon>
            </span>
            <span v-if="file.status === 'uploading'" class="file-progress">
              {{ file.percentage || 0 }}%
            </span>
          </div>
          <div class="file-meta">
            <template v-if="file.status === 'finished'">
              <span>{{ formatSize(file.metadata?.fileSize || file.fileSize) }}</span>
              <span class="file-meta-dot">·</span>
              <span>{{ file.metadata?.extension || getExt(file.name) || '—' }}</span>
            </template>
            <span v-else-if="file.status === 'error'" class="file-error-text">上传失败</span>
            <span v-else>上传中...</span>
          </div>
        </div>

        <div v-if="file.status === 'finished'" class="file-actions">
          <NIcon v-if="showDownload" size="18" title="下载" @click="handleDownload(file)">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="16" height="16"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" /><polyline points="7 10 12 15 17 10" /><line x1="12" x2="12" y1="15" y2="3" /></svg>
          </NIcon>
          <NIcon v-if="!disabled" size="18" class="file-action-danger" title="删除" @click="handleRemoveFile(file)">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="16" height="16"><polyline points="3 6 5 6 21 6" /><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" /></svg>
          </NIcon>
        </div>
      </div>
    </div>

    <!-- 重命名弹窗 -->
    <NModal v-model:show="renameVisible" preset="dialog" title="重命名" positive-text="确定" negative-text="取消" @positive-click="confirmRename">
      <NInput v-model:value="renameName" placeholder="请输入新文件名" maxlength="255" />
    </NModal>

    <!-- 上传提示 -->
    <div v-if="showTip" class="upload-tip">
      <NText depth="3" style="font-size: 12px">
        <template v-if="fileType && fileType.length > 0">
          支持 {{ fileType.join('/') }}
        </template>
        <template v-if="fileSize">
          单文件 ≤ {{ fileSize }}MB
        </template>
        <template v-if="limit">
          最多 {{ limit }} 个
        </template>
      </NText>
    </div>
  </div>
</template>

<script setup>
import {
  ArchiveOutline,
  CloudUploadOutline,
  CodeOutline,
  DocumentTextOutline,
  ImageOutline,
  MusicalNoteOutline,
  VideocamOutline,
} from '@vicons/ionicons5'
import { NIcon, NInput, NModal, NText, NUpload } from 'naive-ui'
import { computed, onMounted, ref, watch } from 'vue'
import { finishGlobalLoading, startGlobalLoading } from '@/composables/useGlobalLoading'
import { useStorageConfig } from '@/composables/useStorageConfig'
import { useAuthStore } from '@/store'
import { downloadFile, generateUUID, getFileUrl, request, resolveRenderableFileUrl } from '@/utils'

const props = defineProps({
  // v-model 绑定的值
  modelValue: {
    type: [String, Array],
    default: '',
  },
  // 上传地址
  action: {
    type: String,
    default: '/api/file/upload',
  },
  // 业务类型
  businessType: {
    type: String,
    default: 'common',
  },
  // 业务ID
  businessId: {
    type: String,
    default: '',
  },
  // 存储类型 (local/minio/oss等)
  storageType: {
    type: String,
    default: '',
  },
  // 数量限制
  limit: {
    type: Number,
    default: 5,
  },
  // 大小限制(MB)
  fileSize: {
    type: Number,
    default: 10,
  },
  // 文件类型限制，例如['png', 'jpg', 'jpeg']
  fileType: {
    type: Array,
    default: () => [],
  },
  // 是否支持多选
  multiple: {
    type: Boolean,
    default: true,
  },
  // 是否显示文件列表
  showFileList: {
    type: Boolean,
    default: true,
  },
  // 是否显示提示
  showTip: {
    type: Boolean,
    default: true,
  },
  // 是否显示下载按钮（图片预览场景可关闭，避免误触触发下载）
  showDownload: {
    type: Boolean,
    default: true,
  },
  // 上传按钮文本
  uploadButtonText: {
    type: String,
    default: '选择文件',
  },
  // 是否禁用
  disabled: {
    type: Boolean,
    default: false,
  },
  // 返回值类型: string-逗号分隔的URL字符串, array-URL数组, object-完整对象数组
  valueType: {
    type: String,
    default: 'string',
    validator: value => ['string', 'array', 'object'].includes(value),
  },
})

const emit = defineEmits(['update:modelValue', 'success', 'error', 'remove'])

const authStore = useAuthStore()
const { storageType: serverStorageType, fileSize: serverFileSize, loadConfig: loadStorageConfig } = useStorageConfig()
const uploadRef = ref(null)
const fileList = ref([])
const renameVisible = ref(false)
const renameFile = ref(null)
const renameName = ref('')
let loadSeq = 0
const uploadLoadingTokens = new Map()

// 上传提示文案
const uploadHint = computed(() => {
  const parts = []
  if (props.fileType?.length)
    parts.push(`支持 ${props.fileType.join('/')}`)
  if (props.fileSize)
    parts.push(`≤${props.fileSize}MB`)
  return parts.join(' · ')
})

// 展示文件列表（过滤掉空状态 pending 等已移除的文件）
const displayFiles = computed(() => {
  return fileList.value.filter(f => f.status !== 'removed')
})

// 文件类型 → 图标
function getFileTypeIcon(file) {
  const ext = (file.metadata?.extension || getExt(file.name)).toLowerCase()
  const map = {
    'png': ImageOutline,
    'jpg': ImageOutline,
    'jpeg': ImageOutline,
    'gif': ImageOutline,
    'webp': ImageOutline,
    'svg': ImageOutline,
    'bmp': ImageOutline,
    'ico': ImageOutline,
    'mp4': VideocamOutline,
    'avi': VideocamOutline,
    'mov': VideocamOutline,
    'wmv': VideocamOutline,
    'mp3': MusicalNoteOutline,
    'wav': MusicalNoteOutline,
    'flac': MusicalNoteOutline,
    'zip': ArchiveOutline,
    'rar': ArchiveOutline,
    '7z': ArchiveOutline,
    'tar': ArchiveOutline,
    'gz': ArchiveOutline,
    'js': CodeOutline,
    'ts': CodeOutline,
    'py': CodeOutline,
    'java': CodeOutline,
    'html': CodeOutline,
    'css': CodeOutline,
    'json': CodeOutline,
    'xml': CodeOutline,
  }
  return map[ext] || DocumentTextOutline
}

function getFileIconColor(file) {
  const ext = (file.metadata?.extension || getExt(file.name)).toLowerCase()
  if (['png', 'jpg', 'jpeg', 'gif', 'webp', 'svg'].includes(ext))
    return '#e8590c'
  if (['mp4', 'avi', 'mov'].includes(ext))
    return '#1971c2'
  if (['mp3', 'wav'].includes(ext))
    return '#e03131'
  if (['zip', 'rar', '7z', 'tar', 'gz'].includes(ext))
    return '#2f9e44'
  if (['js', 'ts', 'py', 'java', 'html', 'css', 'json'].includes(ext))
    return '#6741d9'
  return '#868e96'
}

function getExt(name) {
  if (!name)
    return ''
  const i = name.lastIndexOf('.')
  return i > -1 ? name.slice(i + 1) : ''
}

function formatSize(bytes) {
  if (!bytes || bytes <= 0)
    return ''
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0
  let size = Number(bytes)
  while (size >= 1024 && i < units.length - 1) { size /= 1024; i++ }
  return `${size.toFixed(size < 10 ? 1 : 0)} ${units[i]}`
}

// 上传地址
const uploadUrl = computed(() => {
  return import.meta.env.VITE_REQUEST_PREFIX + props.action
})

// 请求头
const headers = computed(() => {
  const token = authStore.accessToken
  return {
    'Authorization': token ? `Bearer ${token}` : '',
    'X-Timestamp': Date.now().toString(),
    'X-Nonce': generateUUID(),
  }
})

// 上传附加数据
const uploadData = computed(() => {
  const data = {
    businessType: props.businessType,
    businessId: props.businessId,
  }
  // 只在有明确值时才传 storageType，否则后端 FileManager 会从默认配置取
  const resolvedType = props.storageType || serverStorageType.value
  if (resolvedType) {
    data.storageType = resolvedType
  }
  return data
})

// 接受的文件类型
const acceptTypes = computed(() => {
  if (!props.fileType || props.fileType.length === 0) {
    return undefined
  }
  return props.fileType.map(type => `.${type}`).join(',')
})

// 监听 modelValue 变化，初始化文件列表
watch(() => props.modelValue, (val, oldVal) => {
  const seq = ++loadSeq
  ;(async () => {
    // 如果有文件正在上传，不要覆盖列表
    const hasUploading = fileList.value.some(file => file.status === 'uploading')
    if (hasUploading) {
      return
    }

    // 如果新值和旧值相同，不要重复更新
    if (val === oldVal) {
      return
    }

    if (!val) {
      // 只有在当前没有已完成的文件时才清空
      const hasFinished = fileList.value.some(file => file.status === 'finished')
      if (!hasFinished && seq === loadSeq) {
        fileList.value = []
      }
      return
    }

    let list = []

    if (props.valueType === 'object') {
      // 对象数组格式
      list = Array.isArray(val) ? val : []
    }
    else if (props.valueType === 'array') {
      // URL数组格式
      list = Array.isArray(val) ? val : []
    }
    else {
      // 字符串格式，逗号分隔
      list = typeof val === 'string' ? val.split(',').filter(Boolean) : []
    }

    // 如果列表为空且当前有已完成的文件，不要清空
    if (list.length === 0 && fileList.value.some(file => file.status === 'finished')) {
      return
    }

    // 转换为 Naive UI 需要的格式
    const newFileList = (await Promise.all(list.map(async (item, index) => {
      if (typeof item === 'string') {
        const isFileId = !item.includes('/')
        let name = isFileId ? item : extractFileName(item)
        let metadata = {}
        if (isFileId) {
          const resolved = await fetchFileName(item)
          if (resolved) {
            name = resolved.name
            metadata = resolved.metadata
          }
        }
        return {
          id: `file-${Date.now()}-${index}`,
          name,
          url: await resolveRenderableFileUrl(item),
          originalUrl: item,
          fileId: isFileId ? item : null,
          filePath: !isFileId ? item : null,
          metadata,
          status: 'finished',
          percentage: 100,
        }
      }
      else if (typeof item === 'object') {
        return {
          id: item.id || item.fileId || `file-${Date.now()}-${index}`,
          name: item.originalName || item.name || item.fileName || extractFileName(item.url || item.filePath || ''),
          url: await resolveRenderableFileUrl(item),
          originalUrl: item.url || item.filePath || item.accessUrl,
          fileId: item.fileId || null,
          filePath: item.filePath || null,
          metadata: item,
          status: 'finished',
          percentage: 100,
          ...item,
        }
      }
      return null
    }))).filter(Boolean)

    if (seq === loadSeq) {
      fileList.value = newFileList.filter(Boolean)
    }
  })()
}, { immediate: true })

// 上传前校验
function handleBeforeUpload({ file }) {
  // 校验文件类型
  if (props.fileType && props.fileType.length > 0) {
    const fileName = file.name
    const fileExt = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase()
    const isTypeOk = props.fileType.includes(fileExt)
    if (!isTypeOk) {
      window.$message.error(`文件格式不正确，请上传 ${props.fileType.join('/')} 格式文件！`)
      return false
    }
  }

  // 校验文件大小
  const maxSize = props.fileSize || serverFileSize.value
  if (maxSize) {
    const isLt = file.file.size / 1024 / 1024 < maxSize
    if (!isLt) {
      window.$message.error(`上传文件大小不能超过 ${maxSize}MB！`)
      return false
    }
  }

  // 校验文件数量
  if (props.limit && fileList.value.length >= props.limit) {
    window.$message.error(`最多只能上传 ${props.limit} 个文件！`)
    return false
  }

  startUploadLoading(file)
  return true
}

function resolveUploadLoadingKey(file) {
  return file?.id || file?.name || `file-${Date.now()}`
}

function startUploadLoading(file) {
  const key = resolveUploadLoadingKey(file)
  if (uploadLoadingTokens.has(key))
    return

  const token = startGlobalLoading({
    globalLoadingType: 'upload',
    globalLoadingText: '文件上传中，请稍候...',
  })
  uploadLoadingTokens.set(key, token)
}

function finishUploadLoading(file) {
  const key = resolveUploadLoadingKey(file)
  const token = uploadLoadingTokens.get(key)
  finishGlobalLoading(token)
  uploadLoadingTokens.delete(key)
}

// 上传完成（⚠ 必须同步返回，Naive Upload 不等待 Promise，
// 否则 id 变 undefined 导致图片不显示 + warning）
function handleFinish({ file, event }) {
  finishUploadLoading(file)

  try {
    const response = JSON.parse(event.target.response)

    if (response.code === 200) {
      const fileData = response.data
      const downloadUrl = getFileUrl(fileData)

      // 同步更新本地 fileList 以备 emitValue 使用
      const idx = fileList.value.findIndex(f => f.id === file.id)
      const enriched = {
        id: file.id,
        name: fileData.originalName || file.name,
        url: downloadUrl,
        status: 'finished',
        percentage: 100,
        fileId: fileData.fileId,
        filePath: fileData.filePath,
        originalUrl: fileData.fileId || fileData.filePath,
        metadata: fileData,
      }
      if (idx > -1) {
        fileList.value.splice(idx, 1, enriched)
      }
      else {
        fileList.value.push(enriched)
      }

      // 后台异步解析 blob URL
      resolveRenderableFileUrl(fileData).then((blobUrl) => {
        if (blobUrl) {
          const pos = fileList.value.findIndex(f => f.fileId === fileData.fileId)
          if (pos > -1) {
            fileList.value.splice(pos, 1, { ...fileList.value[pos], url: blobUrl })
          }
        }
      })

      window.$message.success('文件上传成功')
      emit('success', fileData)
      setTimeout(() => emitValue(), 100)

      // 返回给 Naive 用于内部文件列表渲染
      return { ...file, url: downloadUrl, name: fileData.originalName || file.name }
    }
    else {
      file.status = 'error'
      window.$message.error(response.message || response.msg || '上传失败')
      emit('error', response)
    }
  }
  catch (error) {
    console.error('上传响应解析失败:', error)
    file.status = 'error'
    window.$message.error('上传失败')
    emit('error', error)
  }
}

// 上传失败
function handleError({ file, event }) {
  finishUploadLoading(file)

  const { response } = event.target
  let msg = ''
  try {
    msg = JSON.parse(response)?.msg
  }
  catch {
    msg = ''
  }
  window.$message.error(msg || '上传失败，请重试')
  emit('error', { file, event })
}

// 删除文件（Naive on-remove 回调）
function handleRemove({ file }) {
  handleRemoveFile(file)
}

// 自定义删除
function handleRemoveFile(file) {
  const index = fileList.value.findIndex(f => f.id === file.id)
  if (index > -1) {
    fileList.value.splice(index, 1)
  }
  emit('remove', file)
  emitValue()
}

// 点击文件列表时，使用带认证头的请求下载，避免直接打开 URL 丢失 token
function handlePreview(file, { event } = {}) {
  event?.preventDefault()
  if (!props.showDownload)
    return false
  handleDownload(file)
}

async function handleDownload(file) {
  try {
    await downloadFile(file.fileId || file.originalUrl || file.filePath || file.url, file.name || file.originalName || file.fileName || '')
  }
  catch (error) {
    window.$message.error(`下载失败：${error.message || '未知错误'}`)
  }

  return false
}

// 文件列表变化 — 仅用于检测上传完成触发 emitValue，不覆盖 fileList
function handleFileListChange(newFileList) {
  // 保留已完成文件的完整信息（从本地 fileList 中补充）
  const enrichedList = newFileList.map((newFile) => {
    const existingFile = fileList.value.find(f => f.id === newFile.id)
    if (existingFile && existingFile.status === 'finished' && existingFile.fileId) {
      return {
        ...newFile,
        name: existingFile.metadata?.originalName || existingFile.name || newFile.name,
        fileId: existingFile.fileId,
        filePath: existingFile.filePath,
        originalUrl: existingFile.originalUrl,
        url: existingFile.url || newFile.url,
        metadata: existingFile.metadata,
      }
    }
    // 如果 handleFinish 刚处理过，file 对象上可能直接有 fileId
    if (newFile.fileId) {
      return newFile
    }
    return newFile
  })

  fileList.value = enrichedList

  // 全部上传完成时 emit 最新值
  const allFinished = enrichedList.every(file => file.status === 'finished' || file.status === 'error')
  if (allFinished && enrichedList.some(file => file.status === 'finished')) {
    setTimeout(() => emitValue(), 50)
  }
}

// 触发值更新
function emitValue() {
  const finishedFiles = fileList.value.filter(file => file.status === 'finished')

  if (props.valueType === 'object') {
    // 返回完整对象数组
    const result = finishedFiles.map(file => ({
      id: file.id || file.fileId,
      name: file.name,
      url: file.fileId || file.filePath || file.originalUrl, // 优先返回 fileId
      ...file.metadata,
    }))
    emit('update:modelValue', result)
  }
  else if (props.valueType === 'array') {
    // 返回数组（优先使用 fileId）
    const result = finishedFiles.map(file => file.fileId || file.filePath || file.originalUrl).filter(Boolean)
    emit('update:modelValue', result)
  }
  else {
    // 返回逗号分隔的字符串（优先使用 fileId）
    const result = finishedFiles.map(file => file.fileId || file.filePath || file.originalUrl).filter(Boolean).join(',')
    if (result === String(props.modelValue || ''))
      return
    emit('update:modelValue', result)
  }
}

// 从文件路径中提取文件名
function extractFileName(path) {
  if (!path || typeof path !== 'string') {
    return ''
  }
  const lastSlashIndex = path.lastIndexOf('/')
  if (lastSlashIndex > -1) {
    return path.slice(lastSlashIndex + 1)
  }
  return path
}

// 获取真实文件名（同步等待结果）
async function fetchFileName(fileId) {
  try {
    const res = await request.get(`/system/file/metadata/byFileId/${fileId}`)
    if (res?.code === 200 && res.data?.originalName) {
      return { name: res.data.originalName, metadata: res.data }
    }
  }
  catch { /* 静默 */ }
  return null
}

// 暴露方法
defineExpose({
  submit: () => uploadRef.value?.submit(),
  clear: () => {
    fileList.value = []
    emitValue()
  },
})

// 重命名
function openRename(file) {
  renameFile.value = file
  renameName.value = file.name || file.metadata?.originalName || ''
  renameVisible.value = true
}

async function confirmRename() {
  const file = renameFile.value
  const newName = renameName.value.trim()
  if (!file || !newName)
    return
  try {
    file.name = newName
    if (file.metadata)
      file.metadata.originalName = newName
    await request.put(`/system/file/metadata/rename?fileId=${encodeURIComponent(file.fileId)}&originalName=${encodeURIComponent(newName)}`)
    window.$message.success('重命名成功')
  }
  catch (e) {
    window.$message.error('重命名失败')
  }
  finally {
    renameVisible.value = false
  }
}

// 组件挂载时加载存储配置
onMounted(() => {
  loadStorageConfig()
})
</script>

<style scoped>
.file-upload-wrapper {
  width: 100%;
}

/* ── 上传拖拽区 ── */
.upload-dropzone {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 16px 24px;
  border: 1.5px dashed #d0d5dd;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
  background: #fafbfc;
  width: 100%;
  min-height: 56px;
}
.upload-dropzone:hover {
  border-color: var(--primary-500, #18a058);
  background: #f6fcf8;
}
.upload-dropzone-icon {
  color: #adb5bd;
  flex-shrink: 0;
  transition: color 0.2s;
}
.upload-dropzone:hover .upload-dropzone-icon {
  color: var(--primary-500, #18a058);
}
.upload-dropzone-text {
  display: flex;
  align-items: center;
  gap: 8px;
}
.upload-dropzone-main {
  font-size: 13px;
  font-weight: 600;
  color: #495057;
}
.upload-dropzone-hint {
  font-size: 11px;
  color: #adb5bd;
}

/* ── 自定义文件卡片列表 ── */
.custom-file-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 8px;
}

.file-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-radius: 8px;
  border: 1px solid #e9ecef;
  background: #fff;
  transition: all 0.15s ease;
}
.file-card:hover {
  border-color: #dee2e6;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}
.file-card.is-error {
  border-color: #ffc9c9;
  background: #fff5f5;
}
.file-card.is-uploading {
  border-color: #bac8ff;
  background: #f8f9ff;
}

.file-icon {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8f9fa;
}

.file-body {
  flex: 1;
  min-width: 0;
}

.file-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.file-name {
  font-size: 13px;
  font-weight: 500;
  color: #212529;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-rename-trigger {
  flex-shrink: 0;
  opacity: 0;
  cursor: pointer;
  color: #868e96;
  transition:
    opacity 0.15s,
    color 0.15s;
}
.file-card:hover .file-rename-trigger {
  opacity: 0.5;
}
.file-rename-trigger:hover {
  opacity: 1 !important;
  color: var(--primary-500, #18a058);
}

.file-progress {
  font-size: 12px;
  color: #4c6ef5;
  font-weight: 600;
  flex-shrink: 0;
}

.file-meta {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: #adb5bd;
  margin-top: 2px;
}
.file-meta-dot {
  margin: 0 2px;
}
.file-error-text {
  color: #e03131;
}

.file-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
  opacity: 0;
  transition: opacity 0.15s;
}
.file-card:hover .file-actions {
  opacity: 1;
}
.file-actions .n-icon {
  cursor: pointer;
  color: #868e96;
  transition: color 0.15s;
  padding: 2px;
  border-radius: 4px;
}
.file-actions .n-icon:hover {
  color: #212529;
  background: #f1f3f5;
}
.file-action-danger:hover {
  color: #e03131 !important;
  background: #fff5f5 !important;
}

/* ── 提示 ── */
.upload-tip {
  margin-top: 8px;
}
</style>
