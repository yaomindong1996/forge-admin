<template>
  <div class="image-upload-wrapper">
    <!-- 自定义图片列表 -->
    <div class="image-list">
      <div
        v-for="file in fileList"
        :key="file.id"
        class="image-item"
        :class="{ 'is-error': file.status === 'error' }"
      >
        <!-- 上传中 -->
        <template v-if="file.status === 'uploading'">
          <div class="uploading-mask">
            <NProgress type="circle" :percentage="file.percentage || 0" :show-indicator="false" :stroke-width="4" style="width: 40px" />
          </div>
        </template>

        <!-- 上传完成/回显 -->
        <template v-else-if="file.status === 'finished'">
          <img :src="file.url || file.thumbnailUrl" class="image-preview" @click="handlePreview(file)">
          <div class="image-actions">
            <NIcon class="action-icon" @click="handlePreview(file)">
              <EyeOutline />
            </NIcon>
            <NIcon v-if="!disabled" class="action-icon delete" @click="handleRemoveFile(file)">
              <TrashOutline />
            </NIcon>
          </div>
        </template>

        <!-- 上传失败 -->
        <template v-else-if="file.status === 'error'">
          <div class="error-mask">
            <NIcon size="24" color="#ff4d4f">
              <CloseCircleOutline />
            </NIcon>
            <span class="error-text">上传失败</span>
          </div>
          <NIcon v-if="!disabled" class="remove-icon" @click="handleRemoveFile(file)">
            <CloseOutline />
          </NIcon>
        </template>
      </div>

      <!-- 上传按钮 -->
      <div v-if="!disabled && fileList.length < limit" class="upload-trigger" @click="triggerUpload">
        <NIcon size="24" color="#999">
          <AddOutline />
        </NIcon>
      </div>
    </div>

    <!-- 隐藏的上传组件 -->
    <NUpload
      ref="uploadRef"
      :action="uploadUrl"
      :headers="headers"
      :data="uploadData"
      :max="limit"
      accept="image/*"
      :multiple="multiple"
      :file-list="fileList"
      :show-file-list="false"
      :on-before-upload="handleBeforeUpload"
      :on-finish="handleFinish"
      :on-error="handleError"
      :disabled="disabled"
      @update:file-list="handleFileListChange"
    />

    <!-- 上传提示 -->
    <div v-if="showTip" class="upload-tip">
      <NText depth="3" style="font-size: 12px">
        请上传
        <template v-if="fileSize">
          大小不超过 <NText type="error" strong>
            {{ fileSize }}MB
          </NText>
        </template>
        <template v-if="fileType && fileType.length > 0">
          格式为 <NText type="error" strong>
            {{ fileType.join('/') }}
          </NText>
        </template>
        的图片
      </NText>
    </div>

    <!-- 图片预览 -->
    <NModal
      v-model:show="previewVisible"
      preset="card"
      title="图片预览"
      style="width: 800px"
      :bordered="false"
    >
      <img :src="previewUrl" style="width: 100%; display: block">
    </NModal>
  </div>
</template>

<script setup>
import { AddOutline, CloseCircleOutline, CloseOutline, EyeOutline, TrashOutline } from '@vicons/ionicons5'
import { NIcon, NModal, NProgress, NText, NUpload } from 'naive-ui'
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
    default: 'image',
  },
  // 业务ID
  businessId: {
    type: String,
    default: '',
  },
  // 存储类型
  storageType: {
    type: String,
    default: 'local',
  },
  // 图片数量限制
  limit: {
    type: Number,
    default: 5,
  },
  // 大小限制(MB)
  fileSize: {
    type: Number,
    default: 5,
  },
  // 文件类型，例如['png', 'jpg', 'jpeg']
  fileType: {
    type: Array,
    default: () => ['png', 'jpg', 'jpeg', 'webp', 'gif'],
  },
  // 是否支持多选
  multiple: {
    type: Boolean,
    default: true,
  },
  // 是否显示提示
  showTip: {
    type: Boolean,
    default: true,
  },
  // 是否禁用
  disabled: {
    type: Boolean,
    default: false,
  },
  // 返回值类型: string-逗号分隔的URL字符串, array-URL数组
  valueType: {
    type: String,
    default: 'string',
    validator: value => ['string', 'array'].includes(value),
  },
})

const emit = defineEmits(['update:modelValue', 'success', 'error', 'remove'])
const authStore = useAuthStore()
const uploadRef = ref(null)
const fileList = ref([])
const previewVisible = ref(false)
const previewUrl = ref('')

// 缓存文件属性（解决 Naive UI 覆盖自定义属性的问题）
const filePropsCache = new Map()

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

// 监听 modelValue 变化，初始化文件列表
watch(() => props.modelValue, async (val) => {
  if (!val) {
    fileList.value = []
    return
  }

  let list = []

  if (props.valueType === 'array') {
    list = Array.isArray(val) ? val : []
  }
  else {
    // 字符串格式，逗号分隔
    list = typeof val === 'string' ? val.split(',').filter(Boolean) : []
  }

  // 转换为 Naive UI 需要的格式，并转换为 blob URL
  const token = authStore.accessToken
  const filePromises = list.map(async (url, index) => {
    // 使用 getFileUrl 转换为完整的访问 URL
    const fullUrl = getFileUrl(url)

    const fileItem = {
      id: `image-${Date.now()}-${index}`,
      name: extractFileName(url),
      originalUrl: url,
      // 如果 url 不包含 /，认为是 fileId
      fileId: url.includes('/') ? null : url,
      filePath: url.includes('/') ? url : null,
      url: fullUrl,
      thumbnailUrl: fullUrl, // Naive UI image-card 模式需要
      status: 'finished',
      percentage: 100,
    }

    // 尝试转换为 blob URL
    try {
      const response = await fetch(fullUrl, {
        headers: {
          Authorization: token ? `Bearer ${token}` : '',
        },
      })

      if (response.ok) {
        const blob = await response.blob()
        const blobUrl = URL.createObjectURL(blob)
        fileItem.url = blobUrl
        fileItem.thumbnailUrl = blobUrl
      }
    }
    catch (err) {
      console.warn('图片加载失败，使用原始URL:', err)
    }

    return fileItem
  })

  fileList.value = await Promise.all(filePromises)
}, { immediate: true })

// 上传前校验
function handleBeforeUpload({ file }) {
  // 校验文件类型
  let isImg = false
  if (props.fileType && props.fileType.length > 0) {
    const fileName = file.name
    const fileExt = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase()
    isImg = props.fileType.some((type) => {
      if (file.type.includes(type))
        return true
      if (fileExt && fileExt.includes(type))
        return true
      return false
    })
  }
  else {
    isImg = file.type.includes('image')
  }

  if (!isImg) {
    window.$message.error(`文件格式不正确，请上传 ${props.fileType.join('/')} 格式的图片！`)
    return false
  }

  // 校验文件大小
  if (props.fileSize) {
    const isLt = file.file.size / 1024 / 1024 < props.fileSize
    if (!isLt) {
      window.$message.error(`上传图片大小不能超过 ${props.fileSize}MB！`)
      return false
    }
  }

  // 校验文件数量
  if (props.limit && fileList.value.length >= props.limit) {
    window.$message.error(`最多只能上传 ${props.limit} 张图片！`)
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

      // 设置文件属性
      if (!file.id) {
        file.id = fileData.fileId || `image-${Date.now()}`
      }

      // 优先使用本地 blob URL（避免闪烁）
      let displayUrl = accessUrl
      if (file.file) {
        try {
          displayUrl = URL.createObjectURL(file.file)
        }
        catch (err) {
          console.warn('创建本地 blob URL 失败:', err)
        }
      }

      // 缓存文件属性（关键！）
      const cachedProps = {
        fileId: fileData.fileId,
        filePath: fileData.filePath,
        originalUrl: fileData.fileId || fileData.filePath,
        url: displayUrl,
        thumbnailUrl: displayUrl,
      }
      filePropsCache.set(file.id, cachedProps)

      // 设置文件属性
      Object.assign(file, cachedProps)
      file.status = 'finished'
      file.percentage = 100

      window.$message.success('图片上传成功')

      // 触发成功事件
      emit('success', fileData)

      // 延迟更新值
      setTimeout(() => {
        emitValue()
      }, 100)

      return file
    }
    else {
      file.status = 'error'
      window.$message.error(response.msg || '上传失败')
      emit('error', response)
      return file
    }
  }
  catch (error) {
    file.status = 'error'
    window.$message.error('上传失败')
    emit('error', error)
    return file
  }
}

// 上传失败
function handleError({ file, event }) {
  const { response } = event.target
  const { msg } = JSON.parse(response)
  window.$message.error(msg || '上传失败，请重试')
  emit('error', { file, event })
}

// 触发上传
function triggerUpload() {
  // 触发隐藏的 n-upload 组件
  const input = uploadRef.value?.$el?.querySelector('input[type="file"]')
  if (input) {
    input.click()
  }
}

// 删除图片
function handleRemoveFile(file) {
  const index = fileList.value.findIndex(f => f.id === file.id)
  if (index > -1) {
    fileList.value.splice(index, 1)
    // 清除缓存
    filePropsCache.delete(file.id)
    // 触发事件
    emit('remove', file)
    emitValue()
  }
}

// 预览图片
async function handlePreview(file) {
  // 如果已经是 blob URL，直接使用
  if (file.url && file.url.startsWith('blob:')) {
    previewUrl.value = file.url
    previewVisible.value = true
    return
  }

  // 通过 fetch 获取图片并转换为 blob URL（带 token）
  try {
    const token = authStore.accessToken
    const response = await fetch(file.url, {
      headers: {
        Authorization: token ? `Bearer ${token}` : '',
      },
    })

    if (!response.ok) {
      throw new Error('图片加载失败')
    }

    const blob = await response.blob()
    const blobUrl = URL.createObjectURL(blob)
    previewUrl.value = blobUrl
    previewVisible.value = true
  }
  catch (error) {
    window.$message.error('图片预览失败')
    console.error('图片预览失败:', error)
  }
}

// 文件列表变化
function handleFileListChange(newFileList) {
  // 从缓存中恢复自定义属性
  const updatedList = newFileList.map((newFile) => {
    const cached = filePropsCache.get(newFile.id)
    if (cached) {
      return {
        ...newFile,
        ...cached,
      }
    }
    // 也尝试从已有列表中获取
    const existingFile = fileList.value.find(f => f.id === newFile.id)
    if (existingFile) {
      return {
        ...newFile,
        fileId: newFile.fileId || existingFile.fileId,
        filePath: newFile.filePath || existingFile.filePath,
        originalUrl: newFile.originalUrl || existingFile.originalUrl,
        url: newFile.url || existingFile.url,
        thumbnailUrl: newFile.thumbnailUrl || existingFile.thumbnailUrl || newFile.url || existingFile.url,
      }
    }
    return newFile
  })
  fileList.value = updatedList
}

// 触发值更新
function emitValue() {
  const finishedFiles = fileList.value.filter(file => file.status === 'finished')

  console.log('[ImageUpload] emitValue - finishedFiles:', finishedFiles.map(f => ({
    id: f.id,
    fileId: f.fileId,
    filePath: f.filePath,
    originalUrl: f.originalUrl,
    status: f.status,
  })))

  if (props.valueType === 'array') {
    // 返回数组（优先使用 fileId，其次 filePath，再其次 originalUrl）
    const result = finishedFiles.map((file) => {
      const value = file.fileId || file.filePath || file.originalUrl
      // 过滤掉 blob: URL 和空值
      if (!value || (typeof value === 'string' && value.startsWith('blob:'))) {
        return null
      }
      return value
    }).filter(Boolean)
    console.log('[ImageUpload] emit array result:', result)
    emit('update:modelValue', result)
  }
  else {
    // 返回逗号分隔的字符串（优先使用 fileId，其次 filePath，再其次 originalUrl）
    const result = finishedFiles.map((file) => {
      const value = file.fileId || file.filePath || file.originalUrl
      // 过滤掉 blob: URL 和空值
      if (!value || (typeof value === 'string' && value.startsWith('blob:'))) {
        return null
      }
      return value
    }).filter(Boolean).join(',')
    console.log('[ImageUpload] emit string result:', result)
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
.image-upload-wrapper {
  width: 100%;
}

.image-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.image-item {
  position: relative;
  width: 96px;
  height: 96px;
  border: 1px solid #e0e0e6;
  border-radius: 4px;
  overflow: hidden;
  background: #fafafc;
}

.image-item.is-error {
  border-color: #ff4d4f;
}

.image-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
  cursor: pointer;
}

.image-actions {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  opacity: 0;
  transition: opacity 0.2s;
}

.image-item:hover .image-actions {
  opacity: 1;
}

.action-icon {
  color: #fff;
  font-size: 18px;
  cursor: pointer;
  transition: transform 0.2s;
}

.action-icon:hover {
  transform: scale(1.2);
}

.action-icon.delete:hover {
  color: #ff4d4f;
}

.uploading-mask {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fafafc;
}

.error-mask {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.error-text {
  font-size: 12px;
  color: #ff4d4f;
}

.remove-icon {
  position: absolute;
  top: 4px;
  right: 4px;
  font-size: 16px;
  color: #999;
  cursor: pointer;
}

.remove-icon:hover {
  color: #ff4d4f;
}

.upload-trigger {
  width: 96px;
  height: 96px;
  border: 1px dashed #d9d9d9;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: border-color 0.2s;
  background: #fafafc;
}

.upload-trigger:hover {
  border-color: var(--primary-color, #18a058);
}

.upload-tip {
  margin-top: 8px;
}
</style>
