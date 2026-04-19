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
      :disabled="disabled"
      :show-file-list="showFileList"
      @update:file-list="handleFileListChange"
    >
      <NButton :disabled="disabled" size="small">
        <template #icon>
          <NIcon><CloudUploadOutline /></NIcon>
        </template>
        {{ uploadButtonText }}
      </NButton>
    </NUpload>

    <!-- 上传提示 -->
    <div v-if="showTip" class="upload-tip">
      <NAlert type="info" :show-icon="false" size="small">
        <template #default>
          <div class="tip-content">
            <span v-if="fileType && fileType.length > 0">
              <NIcon><DocumentTextOutline /></NIcon>
              支持格式: {{ fileType.join(', ') }}
            </span>
            <span v-if="fileSize">
              <NIcon><ResizeOutline /></NIcon>
              单个文件不超过 {{ fileSize }}MB
            </span>
            <span v-if="limit">
              <NIcon><FileTrayFullOutline /></NIcon>
              最多上传 {{ limit }} 个文件
            </span>
          </div>
        </template>
      </NAlert>
      <slot name="tip" />
    </div>
  </div>
</template>

<script setup>
import {
  CloudUploadOutline,
  DocumentTextOutline,
  FileTrayFullOutline,
  ResizeOutline,
} from '@vicons/ionicons5'
import { NAlert, NButton, NIcon, NUpload } from 'naive-ui'
import { computed, ref, watch } from 'vue'
import { useAuthStore } from '@/store'
import { generateUUID, getFileUrl } from '@/utils'

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
    default: 'local',
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
const uploadRef = ref(null)
const fileList = ref([])

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
  return {
    businessType: props.businessType,
    businessId: props.businessId,
    storageType: props.storageType,
  }
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
  console.log('[FileUpload] modelValue 变化:', val, '旧值:', oldVal)

  // 如果有文件正在上传，不要覆盖列表
  const hasUploading = fileList.value.some(file => file.status === 'uploading')
  if (hasUploading) {
    console.log('[FileUpload] 有文件正在上传，跳过列表更新')
    return
  }

  // 如果新值和旧值相同，不要重复更新
  if (val === oldVal) {
    console.log('[FileUpload] 值未变化，跳过更新')
    return
  }

  if (!val) {
    // 只有在当前没有已完成的文件时才清空
    const hasFinished = fileList.value.some(file => file.status === 'finished')
    if (!hasFinished) {
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
    console.log('[FileUpload] 新值为空但有已完成文件，保持当前列表')
    return
  }

  // 转换为 Naive UI 需要的格式
  const newFileList = list.map((item, index) => {
    if (typeof item === 'string') {
      return {
        id: `file-${Date.now()}-${index}`,
        name: extractFileName(item),
        url: getFileUrl(item), // 使用 getFileUrl 转换
        originalUrl: item, // 保存原始值
        status: 'finished',
        percentage: 100,
      }
    }
    else if (typeof item === 'object') {
      return {
        id: item.id || item.fileId || `file-${Date.now()}-${index}`,
        name: item.originalName || item.name || item.fileName || extractFileName(item.url || item.filePath), // 优先使用 originalName
        url: getFileUrl(item), // 使用 getFileUrl 转换
        originalUrl: item.url || item.filePath || item.accessUrl, // 保存原始值
        status: 'finished',
        percentage: 100,
        ...item, // 保留原始数据
      }
    }
    return null
  }).filter(Boolean)

  console.log('[FileUpload] 更新文件列表:', newFileList)
  fileList.value = newFileList
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
  if (props.fileSize) {
    const isLt = file.file.size / 1024 / 1024 < props.fileSize
    if (!isLt) {
      window.$message.error(`上传文件大小不能超过 ${props.fileSize}MB！`)
      return false
    }
  }

  // 校验文件数量
  if (props.limit && fileList.value.length >= props.limit) {
    window.$message.error(`最多只能上传 ${props.limit} 个文件！`)
    return false
  }

  return true
}

// 上传完成
function handleFinish({ file, event }) {
  try {
    const response = JSON.parse(event.target.response)

    if (response.code === 200) {
      const fileData = response.data

      // 使用 getFileUrl 构建完整的访问URL
      const accessUrl = getFileUrl(fileData)

      // 在 fileList 中找到对应的文件并更新
      const fileIndex = fileList.value.findIndex(f => f.id === file.id || f.name === file.name)
      if (fileIndex > -1) {
        // 更新现有文件对象
        const targetFile = fileList.value[fileIndex]
        targetFile.fileId = fileData.fileId
        targetFile.filePath = fileData.filePath
        targetFile.originalUrl = fileData.fileId || fileData.filePath
        targetFile.url = accessUrl
        targetFile.name = fileData.originalName || file.name // 优先使用 originalName
        targetFile.status = 'finished'
        targetFile.percentage = 100
        targetFile.metadata = fileData
      }
      else {
        // 如果找不到，添加新文件
        fileList.value.push({
          id: file.id || fileData.fileId || `file-${Date.now()}`,
          name: fileData.originalName || file.name, // 优先使用 originalName
          fileId: fileData.fileId,
          filePath: fileData.filePath,
          originalUrl: fileData.fileId || fileData.filePath,
          url: accessUrl,
          status: 'finished',
          percentage: 100,
          metadata: fileData,
        })
      }

      window.$message.success('文件上传成功')

      // 触发成功事件
      emit('success', fileData)

      // 延迟更新值，确保文件列表已更新
      setTimeout(() => {
        emitValue()
      }, 100)
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
  const { response } = event.target
  const { msg } = JSON.parse(response)
  window.$message.error(msg || '上传失败，请重试')
  emit('error', { file, event })
}

// 删除文件
function handleRemove({ file }) {
  emit('remove', file)
  emitValue()
}

// 文件列表变化
function handleFileListChange(newFileList) {
  console.log('[FileUpload] 文件列表变化:', newFileList)

  // 保留已完成文件的完整信息
  const updatedList = newFileList.map((newFile) => {
    // 查找是否已存在
    const existingFile = fileList.value.find(f => f.id === newFile.id)

    // 如果已存在且已完成，保留完整信息
    if (existingFile && existingFile.status === 'finished' && existingFile.fileId) {
      return {
        ...newFile,
        name: existingFile.metadata?.originalName || existingFile.name || newFile.name, // 优先使用 originalName
        fileId: existingFile.fileId,
        filePath: existingFile.filePath,
        originalUrl: existingFile.originalUrl,
        url: existingFile.url || newFile.url,
        metadata: existingFile.metadata,
      }
    }

    return newFile
  })

  fileList.value = updatedList

  // 如果所有文件都已完成上传，更新值
  const allFinished = updatedList.every(file => file.status === 'finished' || file.status === 'error')
  if (allFinished && updatedList.some(file => file.status === 'finished')) {
    setTimeout(() => {
      emitValue()
    }, 50)
  }
}

// 触发值更新
function emitValue() {
  const finishedFiles = fileList.value.filter(file => file.status === 'finished')

  console.log('[FileUpload] emitValue - 已完成文件:', finishedFiles)

  if (props.valueType === 'object') {
    // 返回完整对象数组
    const result = finishedFiles.map(file => ({
      id: file.id || file.fileId,
      name: file.name,
      url: file.fileId || file.filePath || file.originalUrl, // 优先返回 fileId
      ...file.metadata,
    }))
    console.log('[FileUpload] emitValue - object 模式，发出值:', result)
    emit('update:modelValue', result)
  }
  else if (props.valueType === 'array') {
    // 返回数组（优先使用 fileId）
    const result = finishedFiles.map(file => file.fileId || file.filePath || file.originalUrl).filter(Boolean)
    console.log('[FileUpload] emitValue - array 模式，发出值:', result)
    emit('update:modelValue', result)
  }
  else {
    // 返回逗号分隔的字符串（优先使用 fileId）
    const result = finishedFiles.map(file => file.fileId || file.filePath || file.originalUrl).filter(Boolean).join(',')
    console.log('[FileUpload] emitValue - string 模式，发出值:', result)
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

// 暴露方法
defineExpose({
  submit: () => uploadRef.value?.submit(),
  clear: () => {
    fileList.value = []
    emitValue()
  },
})
</script>

<style scoped>
.file-upload-wrapper {
  width: 100%;
}

.upload-tip {
  margin-top: 8px;
}

.tip-content {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  font-size: 12px;
}

.tip-content span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.tip-content .n-icon {
  font-size: 14px;
}
</style>
