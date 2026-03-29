<template>
  <div class="file-management-page">
    <div class="file-management-container">
      <!-- 左侧分组导航 -->
      <div class="sidebar">
        <div class="sidebar-header">
          <h3 class="sidebar-title">文件分组</h3>
          <n-button
            type="primary"
            size="tiny"
            quaternary
            @click="showGroupModal = true"
          >
            <template #icon>
              <i class="i-material-symbols:add" />
            </template>
          </n-button>
        </div>

        <div class="group-list">
          <!-- 默认分组 -->
          <div
            class="group-item"
            :class="{ active: selectedGroup === 'all' }"
            @click="selectGroup('all')"
          >
            <div class="group-info">
              <i class="i-material-symbols:folder group-icon" />
              <span class="group-name">全部文件</span>
            </div>
            <span class="file-count">{{ totalFiles }}</span>
          </div>

          <div
            class="group-item"
            :class="{ active: selectedGroup === 'recent' }"
            @click="selectGroup('recent')"
          >
            <div class="group-info">
              <i class="i-material-symbols:history group-icon" />
              <span class="group-name">最近上传</span>
            </div>
            <span class="file-count">-</span>
          </div>

          <div
            class="group-item"
            :class="{ active: selectedGroup === 'images' }"
            @click="selectGroup('images')"
          >
            <div class="group-info">
              <i class="i-material-symbols:image group-icon" />
              <span class="group-name">图片</span>
            </div>
            <span class="file-count">{{ imageCount }}</span>
          </div>

          <div
            class="group-item"
            :class="{ active: selectedGroup === 'documents' }"
            @click="selectGroup('documents')"
          >
            <div class="group-info">
              <i class="i-material-symbols:description group-icon" />
              <span class="group-name">文档</span>
            </div>
            <span class="file-count">{{ documentCount }}</span>
          </div>

          <div class="group-divider" v-if="fileGroups.length > 0"></div>

          <!-- 自定义分组 -->
          <div
            v-for="group in fileGroups"
            :key="group.id"
            class="group-item"
            :class="{ active: selectedGroup === group.id }"
            @click="selectGroup(group.id)"
          >
            <div class="group-info">
              <i
                :class="getGroupIcon(group.groupType)"
                class="group-icon"
              />
              <span class="group-name">{{ group.groupName }}</span>
            </div>
            <span class="file-count">{{ group.fileCount || 0 }}</span>
          </div>
        </div>
      </div>

      <!-- 右侧内容区域 -->
      <div class="main-content">
        <!-- 工具栏 -->
        <div class="toolbar">
          <div class="toolbar-left">
            <n-button-group size="small">
              <n-button
                :type="viewMode === 'list' ? 'primary' : 'default'"
                @click="viewMode = 'list'"
              >
                <i class="i-material-symbols:view-list mr-4" />
                列表
              </n-button>
              <n-button
                :type="viewMode === 'grid' ? 'primary' : 'default'"
                @click="switchToGridView"
              >
                <i class="i-material-symbols:grid-view mr-4" />
                网格
              </n-button>
            </n-button-group>

            <n-divider vertical style="height: 24px; margin: 0 12px;" />

            <span class="current-group-title">{{ currentGroupTitle }}</span>
          </div>

          <div class="upload-section">
            <n-select
              v-if="storageConfigOptions.length > 0"
              v-model:value="selectedStorageConfigId"
              :options="storageConfigOptions"
              size="small"
              placeholder="存储配置"
              style="width: 160px;"
            />
            <n-upload
              :action="uploadUrl"
              :headers="uploadHeaders"
              :data="uploadData"
              :multiple="true"
              :show-file-list="false"
              @finish="handleUploadFinish"
              @error="handleUploadError"
            >
              <n-button type="primary" size="small">
                <template #icon>
                  <i class="i-material-symbols:upload" />
                </template>
                上传文件
              </n-button>
            </n-upload>
          </div>
        </div>

        <!-- 文件列表/网格视图 -->
        <div class="file-content">
          <!-- 列表视图 -->
          <div v-show="viewMode === 'list'" class="file-table">
            <AiCrudPage
              ref="crudRef"
              :api-config="{
                list: 'get@/system/file/metadata/page',
                delete: 'delete@/system/file/metadata'
              }"
              :search-schema="searchSchema"
              :columns="tableColumns"
              row-key="id"
              :show-add-button="false"
              :public-params="listParams"
            >
              <!-- 自定义操作列 -->
              <template #table-action="{ row }">
                <div class="table-action-buttons">
                  <n-button
                    v-if="row.mimeType && row.mimeType.startsWith('image/')"
                    size="small"
                    type="info"
                    ghost
                    @click="handlePreview(row)"
                  >
                    <i class="i-material-symbols:visibility" />
                  </n-button>
                  <n-button
                    size="small"
                    type="primary"
                    ghost
                    @click="handleDownload(row)"
                  >
                    <i class="i-material-symbols:download" />
                  </n-button>
                  <n-dropdown
                    trigger="click"
                    :options="getDropdownOptions(row)"
                    @select="(key) => handleDropdownSelect(key, row)"
                  >
                    <n-button size="small" type="tertiary" circle>
                      <i class="i-material-symbols:more-vert" />
                    </n-button>
                  </n-dropdown>
                </div>
              </template>
            </AiCrudPage>
          </div>

          <!-- 网格视图 -->
          <div v-show="viewMode === 'grid'" class="file-grid">
            <div class="grid-header">
              <span class="grid-title">{{ currentGroupTitle }}</span>
              <span class="grid-count">{{ fileList.length }} 个文件</span>
            </div>

            <div v-if="fileList.length === 0" class="empty-state">
              <i class="i-material-symbols:folder-open empty-icon" />
              <p class="empty-text">暂无文件</p>
              <n-upload
                :action="uploadUrl"
                :headers="uploadHeaders"
                :data="uploadData"
                :multiple="true"
                :show-file-list="false"
                @finish="handleUploadFinish"
                @error="handleUploadError"
              >
                <n-button type="primary" size="small">上传文件</n-button>
              </n-upload>
            </div>

            <div v-else class="file-grid-container">
              <div
                v-for="file in fileList"
                :key="file.id"
                class="file-card"
              >
                <div class="file-preview">
                  <template v-if="file.mimeType && file.mimeType.startsWith('image/')">
                    <img
                      v-if="file.thumbnailUrl"
                      :src="file.thumbnailUrl"
                      :alt="file.originalName"
                      @click="handlePreview(file)"
                    />
                    <div v-else class="thumbnail-loading" @click="handlePreview(file)">
                      <i class="i-material-symbols:image thumbnail-loading-icon" />
                      <span class="thumbnail-loading-text">加载中...</span>
                    </div>
                  </template>
                  <template v-else>
                    <div
                      class="file-icon-container"
                      @click="handleDownload(file)"
                    >
                      <i
                        :class="getFileIcon(file.extension)"
                        class="file-icon"
                        :style="{ color: getFileIconColor(file.extension) }"
                      />
                    </div>
                  </template>

                  <div class="file-actions">
                    <n-dropdown
                      trigger="click"
                      :options="getDropdownOptions(file)"
                      @select="(key) => handleDropdownSelect(key, file)"
                    >
                      <n-button
                        circle
                        size="tiny"
                        quaternary
                        @click.stop
                      >
                        <i class="i-material-symbols:more-vert" />
                      </n-button>
                    </n-dropdown>
                  </div>
                </div>

                <div class="file-info">
                  <div class="file-name" :title="file.originalName">
                    {{ file.originalName }}
                  </div>
                  <div class="file-meta">
                    <span class="file-size">{{ formatFileSize(file.fileSize) }}</span>
                    <span class="file-time">{{ formatDate(file.uploadTime) }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 分组管理弹窗 -->
    <n-modal
      v-model:show="showGroupModal"
      preset="card"
      title="管理分组"
      style="width: 500px"
    >
      <div class="group-modal-content">
        <div class="group-form">
          <n-input
            v-model:value="newGroupName"
            placeholder="输入分组名称"
            @keyup.enter="addGroup"
          />
          <n-select
            v-model:value="newGroupType"
            :options="groupTypeOptions"
            placeholder="选择类型"
            style="width: 120px;"
          />
          <n-button
            type="primary"
            @click="addGroup"
            :disabled="!newGroupName.trim()"
          >
            添加
          </n-button>
        </div>

        <div class="existing-groups">
          <div
            v-for="group in fileGroups"
            :key="group.id"
            class="existing-group-item"
          >
            <div class="group-info-row">
              <i :class="getGroupIcon(group.groupType)" class="group-type-icon" />
              <span class="group-name">{{ group.groupName }}</span>
              <span class="group-count">({{ group.fileCount || 0 }}个文件)</span>
            </div>
            <n-button
              type="error"
              size="tiny"
              quaternary
              @click="deleteGroup(group.id)"
            >
              <template #icon>
                <i class="i-material-symbols:delete" />
              </template>
            </n-button>
          </div>
          <div v-if="fileGroups.length === 0" class="no-groups">
            暂无自定义分组
          </div>
        </div>
      </div>
    </n-modal>

    <!-- 移动文件到分组弹窗 -->
    <n-modal
      v-model:show="showMoveModal"
      preset="card"
      title="移动到分组"
      style="width: 400px"
    >
      <div class="move-modal-content">
        <p class="move-hint">选择要将文件移动到的分组：</p>
        <div class="move-group-list">
          <div
            v-for="group in fileGroups"
            :key="group.id"
            class="move-group-item"
            :class="{ active: selectedMoveGroup === group.id }"
            @click="selectedMoveGroup = group.id"
          >
            <i :class="getGroupIcon(group.groupType)" class="group-type-icon" />
            <span class="group-name">{{ group.groupName }}</span>
            <span class="group-count">({{ group.fileCount || 0 }}个文件)</span>
          </div>
        </div>
        <div class="move-actions">
          <n-button @click="showMoveModal = false">取消</n-button>
          <n-button
            type="primary"
            :disabled="!selectedMoveGroup"
            @click="confirmMoveFile"
          >
            确定
          </n-button>
        </div>
      </div>
    </n-modal>

    <!-- 图片预览弹窗 -->
    <n-modal
      v-model:show="showPreviewModal"
      preset="card"
      title="图片预览"
      style="width: 800px"
    >
      <div class="preview-content">
        <n-image :src="previewUrl" style="max-width: 100%;" />
      </div>
    </n-modal>
  </div>
</template>

<script setup>
import { ref, h, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { NTag, NImage, NButton, NInput, NModal, NDropdown, NSelect, NDivider } from 'naive-ui'
import { AiCrudPage } from '@/components/ai-form'
import { request, getFileUrl } from '@/utils'
import { useAuthStore } from '@/store'

defineOptions({ name: 'FileList' })

const crudRef = ref(null)
const authStore = useAuthStore()
const showPreviewModal = ref(false)
const previewUrl = ref('')
const viewMode = ref('list')
const selectedGroup = ref('all')
const showGroupModal = ref(false)
const showMoveModal = ref(false)
const newGroupName = ref('')
const newGroupType = ref('default')
const fileList = ref([])
const selectedMoveGroup = ref(null)
const currentMoveFile = ref(null)

// 文件分组数据
const fileGroups = ref([])

// 存储配置数据
const storageConfigs = ref([])
const selectedStorageConfigId = ref(null)
let thumbnailLoadVersion = 0

// 存储配置选项（用于上传选择）
const storageConfigOptions = computed(() => {
  return storageConfigs.value.map(c => ({
    label: `${c.configName}${c.isDefault ? ' (默认)' : ''}`,
    value: c.id
  }))
})

// 统计数据
const totalFiles = ref(0)
const imageCount = ref(0)
const documentCount = ref(0)

// 分组类型选项
const groupTypeOptions = [
  { label: '默认', value: 'default' },
  { label: '文档', value: 'document' },
  { label: '图片', value: 'image' },
  { label: '视频', value: 'video' },
  { label: '音频', value: 'audio' },
  { label: '压缩包', value: 'archive' }
]

// 当前分组标题
const currentGroupTitle = computed(() => {
  switch (selectedGroup.value) {
    case 'all':
      return '全部文件'
    case 'recent':
      return '最近上传'
    case 'images':
      return '图片'
    case 'documents':
      return '文档'
    default:
      // 对于自定义分组，需要比较字符串类型的ID（避免精度丢失）
      const groupIdStr = String(selectedGroup.value)
      const group = fileGroups.value.find(g => String(g.id) === groupIdStr)
      return group ? group.groupName : '文件列表'
  }
})

// 列表参数
const listParams = computed(() => {
  // 根据选中的分组添加过滤条件
  if (selectedGroup.value === 'images') {
    return { mimeType: 'image/' }
  } else if (selectedGroup.value === 'documents') {
    return { mimeType: 'application/' }
  } else if (selectedGroup.value === 'recent') {
    // 最近上传可能需要特定的时间范围参数
    return { sort: 'uploadTime,desc' }
  } else if (selectedGroup.value !== 'all' && isValidGroupId(selectedGroup.value)) {
    // 如果选择了自定义分组，使用 groupId 过滤（保持字符串类型避免精度丢失）
    return { groupId: String(selectedGroup.value) }
  }

  // 全部文件，返回空对象
  return {}
})

// 判断是否是有效的分组ID
function isValidGroupId(value) {
  if (value === null || value === undefined || value === '') return false
  const num = Number(value)
  return !isNaN(num) && num > 0
}

// 监听弹窗关闭，释放 blob URL
watch(showPreviewModal, (newVal) => {
  if (!newVal && previewUrl.value && previewUrl.value.startsWith('blob:')) {
    URL.revokeObjectURL(previewUrl.value)
    previewUrl.value = ''
  }
})

// 监听分组变化，刷新列表
watch(selectedGroup, (newVal, oldVal) => {
  console.log('分组变化:', oldVal, '->', newVal, 'listParams:', listParams.value)

  if (newVal === oldVal) return

  if (viewMode.value === 'grid') {
    refreshFileGrid()
  } else {
    // 列表视图：public-params 变化会自动触发 AiCrudPage 刷新
    // 但为确保刷新，手动调用 refresh
    nextTick(() => {
      crudRef.value?.refresh()
    })
  }
}, { immediate: false })

// 初始化数据
onMounted(async () => {
  await Promise.all([
    fetchStatistics(),
    fetchFileGroups(),
    fetchStorageConfigs()
  ])
})

// 清理 blob URLs 防止内存泄漏
onBeforeUnmount(() => {
  cleanupBlobUrls()
})

// 获取统计信息
async function fetchStatistics() {
  try {
    // 获取总体统计数据
    const statsResponse = await request.get('/system/file/metadata/statistics')
    if (statsResponse.code === 200) {
      const data = statsResponse.data || {}
      totalFiles.value = Number(data.total) || 0
      imageCount.value = Number(data.imageCount) || 0
      documentCount.value = Number(data.documentCount) || 0
    }
  } catch (error) {
    console.error('获取统计数据失败:', error)
    totalFiles.value = 0
    imageCount.value = 0
    documentCount.value = 0
  }
}

// 获取文件分组列表
async function fetchFileGroups() {
  try {
    const response = await request.get('/system/file/group/list')
    if (response.code === 200 && response.data) {
      fileGroups.value = response.data.map(group => ({
        id: group.id,
        groupName: group.groupName,
        groupType: group.groupType || 'default',
        icon: group.icon,
        fileCount: group.fileCount || 0
      }))
    }
  } catch (error) {
    console.error('获取文件分组失败:', error)
    fileGroups.value = []
  }
}

// 上传配置
const uploadUrl = computed(() => {
  return import.meta.env.VITE_REQUEST_PREFIX + '/api/file/upload'
})

const uploadHeaders = computed(() => {
  const token = authStore.accessToken
  return {
    Authorization: token ? `Bearer ${token}` : ''
  }
})

const uploadData = computed(() => {
  // 如果选择了自定义分组，上传时关联到该分组（保持字符串类型避免精度丢失）
  const groupId = isValidGroupId(selectedGroup.value) ? String(selectedGroup.value) : null

  // 根据选中的存储配置获取 storageType
  const config = storageConfigs.value.find(c => c.id === selectedStorageConfigId.value)
  const storageType = config ? config.storageType : 'local'

  return {
    businessType: 'common',
    businessId: '',
    storageType,
    groupId: groupId || ''
  }
})

// 存储类型选项
const storageTypeOptions = [
  { label: '本地存储', value: 'local' },
  { label: 'RustFS存储', value: 'rustfs' },
  { label: 'MinIO', value: 'minio' },
  { label: '阿里云OSS', value: 'aliyun' },
  { label: '腾讯云COS', value: 'tencent' },
  { label: '七牛云', value: 'qiniu' },
  { label: 'AWS S3', value: 's3' }
]

// 搜索表单配置
const searchSchema = [
  {
    field: 'originalName',
    label: '文件名',
    type: 'input',
    props: {
      placeholder: '请输入文件名'
    }
  },
  {
    field: 'storageType',
    label: '存储类型',
    type: 'select',
    props: {
      placeholder: '请选择存储类型',
      options: storageTypeOptions
    }
  },
  {
    field: 'businessType',
    label: '业务类型',
    type: 'input',
    props: {
      placeholder: '请输入业务类型'
    }
  },
  {
    field: 'mimeType',
    label: '文件类型',
    type: 'select',
    props: {
      placeholder: '请选择文件类型',
      options: [
        { label: '图片', value: 'image/' },
        { label: '文档', value: 'application/pdf' },
        { label: '压缩包', value: 'application/zip' }
      ]
    }
  }
]

// 表格列配置
const tableColumns = [
  {
    prop: 'id',
    label: 'ID',
    width: 80
  },
  {
    prop: 'originalName',
    label: '文件名',
    minWidth: 200,
    showOverflowTooltip: true,
    render: (row) => {
      const isImage = row.mimeType && row.mimeType.startsWith('image/')
      return h('div', { class: 'flex items-center gap-8' }, [
        isImage ? h('i', {
          class: 'i-material-symbols:image text-20 text-success'
        }) : h('i', {
          class: 'i-material-symbols:description text-20 text-info'
        }),
        h('span', row.originalName)
      ])
    }
  },
  {
    prop: 'fileSize',
    label: '文件大小',
    width: 120,
    render: (row) => {
      return h('span', formatFileSize(row.fileSize))
    }
  },
  {
    prop: 'extension',
    label: '扩展名',
    width: 100,
    render: (row) => {
      return h(NTag, { size: 'small', bordered: false }, {
        default: () => row.extension?.toUpperCase()
      })
    }
  },
  {
    prop: 'storageType',
    label: '存储类型',
    width: 120,
    render: (row) => {
      const typeMap = {
        'local': { text: '本地存储', type: 'default' },
        'rustfs': { text: 'RustFS', type: 'success' },
        'minio': { text: 'MinIO', type: 'info' },
        'aliyun': { text: '阿里云', type: 'warning' },
        'tencent': { text: '腾讯云', type: 'success' },
        'qiniu': { text: '七牛云', type: 'error' },
        's3': { text: 'AWS S3', type: 'primary' }
      }
      const config = typeMap[row.storageType] || { text: row.storageType, type: 'default' }
      return h(NTag, { type: config.type, size: 'small' }, { default: () => config.text })
    }
  },
  {
    prop: 'businessType',
    label: '业务类型',
    width: 120
  },
  {
    prop: 'downloadCount',
    label: '下载次数',
    width: 100
  },
  {
    prop: 'uploadTime',
    label: '上传时间',
    width: 180
  },
  {
    prop: 'action',
    label: '操作',
    width: 220,
    fixed: 'right',
    _slot: 'action'
  }
]

// 分组图标映射
const groupIconMap = {
  document: 'i-material-symbols:description',
  image: 'i-material-symbols:image',
  video: 'i-material-symbols:movie',
  audio: 'i-material-symbols:music-note',
  archive: 'i-material-symbols:archive',
  default: 'i-material-symbols:folder'
}

// 文件图标映射
const fileIconMap = {
  'jpg': 'i-material-symbols:image',
  'jpeg': 'i-material-symbols:image',
  'png': 'i-material-symbols:image',
  'gif': 'i-material-symbols:image',
  'svg': 'i-material-symbols:image',
  'pdf': 'i-material-symbols:picture-as-pdf',
  'doc': 'i-material-symbols:description',
  'docx': 'i-material-symbols:description',
  'xls': 'i-material-symbols:table-chart',
  'xlsx': 'i-material-symbols:table-chart',
  'ppt': 'i-material-symbols:slideshow',
  'pptx': 'i-material-symbols:slideshow',
  'txt': 'i-material-symbols:notes',
  'zip': 'i-material-symbols:archive',
  'rar': 'i-material-symbols:archive',
  '7z': 'i-material-symbols:archive',
  'mp3': 'i-material-symbols:music-note',
  'wav': 'i-material-symbols:music-note',
  'mp4': 'i-material-symbols:movie',
  'avi': 'i-material-symbols:movie',
  'mov': 'i-material-symbols:movie',
  'default': 'i-material-symbols:insert-drive-file'
}

// 文件图标颜色映射
const fileIconColors = {
  'jpg': '#4CAF50',
  'jpeg': '#4CAF50',
  'png': '#4CAF50',
  'gif': '#4CAF50',
  'svg': '#4CAF50',
  'pdf': '#F44336',
  'doc': '#2196F3',
  'docx': '#2196F3',
  'xls': '#4CAF50',
  'xlsx': '#4CAF50',
  'ppt': '#FF9800',
  'pptx': '#FF9800',
  'txt': '#9E9E9E',
  'zip': '#FF9800',
  'rar': '#FF9800',
  '7z': '#FF9800',
  'mp3': '#E91E63',
  'wav': '#E91E63',
  'mp4': '#9C27B0',
  'avi': '#9C27B0',
  'mov': '#9C27B0',
  'default': '#607D8B'
}

// 获取分组图标
function getGroupIcon(type) {
  return groupIconMap[type] || groupIconMap.default
}

// 获取文件图标
function getFileIcon(extension) {
  if (!extension) return fileIconMap.default
  const ext = extension.toLowerCase().replace('.', '')
  return fileIconMap[ext] || fileIconMap.default
}

// 获取文件图标颜色
function getFileIconColor(extension) {
  if (!extension) return fileIconColors.default
  const ext = extension.toLowerCase().replace('.', '')
  return fileIconColors[ext] || fileIconColors.default
}

// 格式化日期
function formatDate(dateString) {
  if (!dateString) return '-'
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'short',
    day: 'numeric'
  })
}

// 格式化文件大小
function formatFileSize(bytes) {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i]
}

// 选择分组
function selectGroup(groupId) {
  // 确保 groupId 是字符串类型，避免数字比较问题
  selectedGroup.value = String(groupId)

  // 如果是网格视图，需要重新获取数据
  if (viewMode.value === 'grid') {
    refreshFileGrid()
  }
  // 列表视图的刷新由 watch(selectedGroup) 处理
}

// 切换到网格视图
function switchToGridView() {
  viewMode.value = 'grid'
  refreshFileGrid()
}

// 刷新文件网格数据
async function refreshFileGrid() {
  try {
    // 清理旧的 blob URLs
    cleanupBlobUrls()

    const params = {
      page: 1,
      size: 50,
      ...listParams.value
    }

    const response = await request.get('/system/file/metadata/page', params)

    if (response.code === 200) {
      const records = response.data?.records || []
      fileList.value = records.map(item => ({
        id: item.id,
        fileId: item.fileId,
        originalName: item.originalName,
        fileSize: item.fileSize,
        extension: item.extension ? '.' + item.extension : '',
        mimeType: item.mimeType,
        thumbnailUrl: '',
        accessUrl: getFileUrl(item.fileId),
        uploadTime: item.uploadTime,
        storageType: item.storageType,
        businessType: item.businessType,
        groupId: item.groupId,
        downloadCount: item.downloadCount
      }))

      // 异步加载图片缩略图（带认证）
      nextTick(() => loadImageThumbnails())
    } else {
      throw new Error(response.msg || '获取文件列表失败')
    }
  } catch (error) {
    console.error('获取文件列表失败:', error)
    window.$message.error(error.message || '获取文件列表失败')
    fileList.value = []
  }
}

// 获取启用的存储配置列表
async function fetchStorageConfigs() {
  try {
    const response = await request.get('/system/storage/config/page', {
      pageNum: 1,
      pageSize: 100,
      enabled: 1
    })
    if (response.code === 200) {
      storageConfigs.value = (response.data?.records || []).map(c => ({
        id: c.id,
        configName: c.configName,
        storageType: c.storageType,
        isDefault: c.isDefault
      }))
      // 默认选中默认配置
      const defaultConfig = storageConfigs.value.find(c => c.isDefault)
      if (defaultConfig) {
        selectedStorageConfigId.value = defaultConfig.id
      } else if (storageConfigs.value.length > 0) {
        selectedStorageConfigId.value = storageConfigs.value[0].id
      }
    }
  } catch (error) {
    console.error('获取存储配置失败:', error)
  }
}

// 异步加载图片缩略图（使用 fetch + blob URL 携带认证信息）
async function loadImageThumbnails() {
  const version = ++thumbnailLoadVersion
  const token = authStore.accessToken

  // 收集需要加载缩略图的图片文件索引
  const imageIndexes = []
  for (let i = 0; i < fileList.value.length; i++) {
    const file = fileList.value[i]
    if (file.mimeType && file.mimeType.startsWith('image/')) {
      imageIndexes.push(i)
    }
  }

  // 分批加载，每批 6 个并发
  const batchSize = 6
  for (let b = 0; b < imageIndexes.length; b += batchSize) {
    if (thumbnailLoadVersion !== version) return

    const batch = imageIndexes.slice(b, b + batchSize)
    await Promise.allSettled(batch.map(async (idx) => {
      if (thumbnailLoadVersion !== version) return
      try {
        const file = fileList.value[idx]
        if (!file) return
        const url = getFileUrl(file.fileId)
        const res = await fetch(url, {
          headers: { Authorization: token ? `Bearer ${token}` : '' }
        })
        if (res.ok && thumbnailLoadVersion === version) {
          const blob = await res.blob()
          fileList.value[idx].thumbnailUrl = URL.createObjectURL(blob)
        }
      } catch (e) {
        // 静默处理单个缩略图加载失败
      }
    }))
  }
}

// 清理所有 blob URLs
function cleanupBlobUrls() {
  fileList.value.forEach(file => {
    if (file.thumbnailUrl && file.thumbnailUrl.startsWith('blob:')) {
      URL.revokeObjectURL(file.thumbnailUrl)
    }
  })
}

// 添加分组
async function addGroup() {
  if (!newGroupName.value.trim()) return

  try {
    const newGroup = {
      groupName: newGroupName.value.trim(),
      groupType: newGroupType.value,
      groupCode: 'group_' + Date.now()
    }

    const response = await request.post('/system/file/group', newGroup)
    if (response.code === 200) {
      window.$message.success('分组添加成功')
      newGroupName.value = ''
      newGroupType.value = 'default'
      // 刷新分组列表
      await fetchFileGroups()
      await fetchStatistics()
    } else {
      window.$message.error(response.msg || '添加失败')
    }
  } catch (error) {
    console.error('添加分组失败:', error)
    window.$message.error('添加分组失败')
  }
}

// 删除分组
async function deleteGroup(groupId) {
  window.$dialog.warning({
    title: '确认删除',
    content: '确定要删除这个分组吗？此操作不会删除分组内的文件，文件将变为未分组状态。',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const response = await request.delete(`/system/file/group/${groupId}`)
        if (response.code === 200) {
          window.$message.success('分组删除成功')
          // 如果当前选中的是被删除的分组，切换到全部
          if (selectedGroup.value === groupId) {
            selectedGroup.value = 'all'
          }
          // 刷新分组列表
          await fetchFileGroups()
          await fetchStatistics()
        } else {
          window.$message.error(response.msg || '删除失败')
        }
      } catch (error) {
        console.error('删除分组失败:', error)
        window.$message.error('删除分组失败')
      }
    }
  })
}

// 获取下拉菜单选项
function getDropdownOptions(file) {
  return [
    {
      label: '预览',
      key: 'preview',
      disabled: !(file.mimeType && file.mimeType.startsWith('image/'))
    },
    {
      label: '下载',
      key: 'download'
    },
    {
      label: '复制链接',
      key: 'copy'
    },
    {
      label: '移动到分组',
      key: 'move'
    },
    {
      label: '删除',
      key: 'delete',
      props: {
        style: { color: '#ff4d4f' }
      }
    }
  ]
}

// 处理下拉菜单选择
function handleDropdownSelect(key, file) {
  switch (key) {
    case 'preview':
      handlePreview(file)
      break
    case 'download':
      handleDownload(file)
      break
    case 'copy':
      handleCopyUrl(file)
      break
    case 'move':
      handleMoveFile(file)
      break
    case 'delete':
      handleDelete(file)
      break
  }
}

// 处理移动文件
function handleMoveFile(file) {
  currentMoveFile.value = file
  selectedMoveGroup.value = null
  showMoveModal.value = true
}

// 确认移动文件
async function confirmMoveFile() {
  if (!selectedMoveGroup.value || !currentMoveFile.value) return

  try {
    const response = await request.put('/system/file/metadata', {
      id: currentMoveFile.value.id,
      groupId: selectedMoveGroup.value
    })

    if (response.code === 200) {
      window.$message.success('文件移动成功')
      showMoveModal.value = false

      // 刷新数据
      if (viewMode.value === 'grid') {
        await refreshFileGrid()
      } else {
        crudRef.value?.refresh()
      }
      await fetchFileGroups()
      await fetchStatistics()
    } else {
      window.$message.error(response.msg || '移动失败')
    }
  } catch (error) {
    console.error('移动文件失败:', error)
    window.$message.error('移动文件失败')
  }
}

// 上传成功
function handleUploadFinish({ file, event }) {
  try {
    const response = JSON.parse(event.target.response)
    if (response.code === 200) {
      window.$message.success(`文件 "${file.name}" 上传成功`)

      // 刷新列表
      if (viewMode.value === 'grid') {
        refreshFileGrid()
      } else {
        crudRef.value?.refresh()
      }

      // 刷新统计
      fetchStatistics()
      fetchFileGroups()
    } else {
      window.$message.error(response.msg || '上传失败')
    }
  } catch (error) {
    window.$message.error('上传失败')
  }
}

// 上传失败
function handleUploadError({ file }) {
  window.$message.error(`文件 "${file.name}" 上传失败`)
}

// 预览
async function handlePreview(row) {
  try {
    const token = authStore.accessToken
    const url = getFileUrl(row.fileId)

    // 使用 fetch 获取图片并转换为 blob URL（带 token）
    const response = await fetch(url, {
      headers: {
        Authorization: token ? `Bearer ${token}` : ''
      }
    })

    if (!response.ok) {
      throw new Error('图片加载失败')
    }

    const blob = await response.blob()
    const blobUrl = URL.createObjectURL(blob)
    previewUrl.value = blobUrl
    showPreviewModal.value = true
  } catch (error) {
    window.$message.error('预览失败：' + (error.message || '未知错误'))
  }
}

// 下载
async function handleDownload(row) {
  let loadingMessage = null
  try {
    const token = authStore.accessToken
    const url = getFileUrl(row.fileId)

    // 使用 fetch 下载文件（带 token）
    //loadingMessage = window.$message.loading('正在下载...', { duration: 0 })
    const response = await fetch(url, {
      headers: {
        Authorization: token ? `Bearer ${token}` : ''
      }
    })

    if (!response.ok) {
      throw new Error('下载失败')
    }

    const blob = await response.blob()
    const blobUrl = URL.createObjectURL(blob)

    // 创建下载链接
    const link = document.createElement('a')
    link.href = blobUrl
    link.download = row.originalName
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)

    // 释放 blob URL
    setTimeout(() => URL.revokeObjectURL(blobUrl), 100)

    window.$message.success('下载成功')
  } catch (error) {
    window.$message.error('下载失败：' + (error.message || '未知错误'))
  } finally {
    if (loadingMessage) loadingMessage.destroy()
  }
}

// 复制链接
function handleCopyUrl(row) {
  try {
    const url = getFileUrl(row.fileId)
    // 使用现代浏览器的 Clipboard API
    if (navigator.clipboard && window.isSecureContext) {
      navigator.clipboard.writeText(url).then(() => {
        window.$message.success('链接已复制到剪贴板')
      })
    } else {
      // 降级方案
      const textarea = document.createElement('textarea')
      textarea.value = url
      textarea.style.position = 'fixed'
      textarea.style.opacity = '0'
      document.body.appendChild(textarea)
      textarea.select()
      document.execCommand('copy')
      document.body.removeChild(textarea)
      window.$message.success('链接已复制到剪贴板')
    }
  } catch (error) {
    window.$message.error('复制失败')
  }
}

// 删除
function handleDelete(row) {
  window.$dialog.warning({
    title: '确认删除',
    content: `确定要删除文件"${row.originalName}"吗？删除后将无法恢复！`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await request.delete(`/system/file/metadata/${row.id}`)
        if (res.code === 200) {
          window.$message.success('删除成功')

          // 刷新列表
          if (viewMode.value === 'grid') {
            await refreshFileGrid()
          } else {
            crudRef.value?.refresh()
          }

          // 刷新统计
          await fetchStatistics()
          await fetchFileGroups()
        }
      } catch (error) {
        window.$message.error('删除失败')
      }
    }
  })
}
</script>

<style scoped>
/* ═══════════════════════════════════════
 * 文件管理页面 - SnowAdmin / Arco Design 风格
 * 全部使用 design-tokens.css 变量
 * ═══════════════════════════════════════ */

/* --- 页面布局 --- */
.file-management-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--bg-page);
}

.file-management-container {
  display: flex;
  height: 100%;
  overflow: hidden;
}

/* --- 左侧分组导航 --- */
.sidebar {
  width: 220px;
  background: var(--bg-primary);
  border-right: 1px solid var(--border-light);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  flex-shrink: 0;
}

.sidebar-header {
  padding: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid var(--border-light);
}

.sidebar-title {
  margin: 0;
  font-size: 15px;
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
}

.group-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.group-list::-webkit-scrollbar { width: 4px; }
.group-list::-webkit-scrollbar-thumb { background: var(--border-light); border-radius: 2px; }

.group-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  cursor: pointer;
  transition: background-color var(--transition-fast), color var(--transition-fast);
  border-radius: var(--radius-md);
  margin-bottom: 2px;
}

.group-item:hover {
  background: var(--bg-hover);
}

.group-item.active {
  background: var(--primary-50);
  color: var(--primary-500);
}

.group-divider {
  height: 1px;
  background: var(--border-light);
  margin: 8px 12px;
}

.group-info {
  display: flex;
  align-items: center;
  flex: 1;
  min-width: 0;
  gap: 8px;
}

.group-icon {
  font-size: 16px;
  width: 16px;
  height: 16px;
  flex-shrink: 0;
  color: var(--text-tertiary);
}

.group-item.active .group-icon {
  color: var(--primary-500);
}

.group-name {
  font-size: 14px;
  font-weight: var(--font-weight-normal);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.group-item.active .group-name {
  font-weight: var(--font-weight-medium);
}

.file-count {
  font-size: 12px;
  color: var(--text-tertiary);
  background: var(--bg-secondary);
  padding: 1px 8px;
  border-radius: var(--radius-full);
  min-width: 20px;
  text-align: center;
  flex-shrink: 0;
}

.group-item.active .file-count {
  background: var(--primary-100);
  color: var(--primary-600);
}

/* --- 右侧内容区域 --- */
.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
}

/* --- 工具栏 --- */
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-light);
  background: var(--bg-primary);
  flex-shrink: 0;
}

.toolbar-left {
  display: flex;
  align-items: center;
}

.current-group-title {
  font-size: 14px;
  font-weight: var(--font-weight-medium);
  color: var(--text-secondary);
}

.upload-section {
  display: flex;
  gap: 8px;
}

/* --- 文件内容区域 --- */
.file-content {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background: var(--bg-page);
}

/* --- 列表视图 --- */
.file-table {
  flex: 1;
  overflow: hidden;
  padding: 16px;
}

.file-table :deep(.ai-crud-page) {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--bg-primary);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-card);
  overflow: hidden;
}

:deep(.n-data-table) { font-size: 14px; }
:deep(.n-data-table-th) { font-weight: var(--font-weight-semibold); color: var(--text-secondary); background: var(--bg-secondary); }
:deep(.n-data-table-td) { padding: 10px 16px; }
:deep(.n-data-table tr:hover td) { background: var(--bg-hover); }

.file-name-text { color: var(--text-primary); font-weight: var(--font-weight-medium); }
.file-size-text { color: var(--text-secondary); font-family: var(--font-family-mono); font-size: 13px; }
.file-ext-tag { font-weight: var(--font-weight-semibold); letter-spacing: 0.5px; }
.download-count-text { color: var(--primary-500); font-weight: var(--font-weight-medium); font-family: var(--font-family-mono); }
.upload-time-text { color: var(--text-tertiary); }

.file-icon-preview {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}

.file-icon-preview.image { background: var(--primary-50); color: var(--primary-500); }
.file-icon-preview.document { background: var(--bg-tertiary); color: var(--primary-400); }

.table-action-buttons {
  display: flex;
  align-items: center;
  gap: 6px;
}

.table-action-buttons :deep(.n-button) {
  border-radius: var(--radius-md);
}

/* --- 网格视图 --- */
.file-grid {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 16px;
  background: var(--bg-page);
}

.grid-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding: 10px 16px;
  background: var(--bg-primary);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-card);
}

.grid-title {
  font-size: 14px;
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
}

.grid-count {
  font-size: 12px;
  color: var(--text-tertiary);
  background: var(--bg-secondary);
  padding: 2px 10px;
  border-radius: var(--radius-full);
}

/* --- 空状态 --- */
.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--text-tertiary);
  padding: 48px;
  background: var(--bg-primary);
  border-radius: var(--radius-card);
  border: 1px dashed var(--border-default);
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
  color: var(--text-disabled);
}

.empty-text {
  font-size: 14px;
  margin-bottom: 16px;
  color: var(--text-tertiary);
}

/* --- 文件卡片网格 --- */
.file-grid-container {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
  overflow-y: auto;
  flex: 1;
}

.file-grid-container::-webkit-scrollbar { width: 4px; }
.file-grid-container::-webkit-scrollbar-thumb { background: var(--border-light); border-radius: 2px; }

/* --- 文件卡片 (SnowAdmin 风格) --- */
.file-card {
  background: var(--bg-primary);
  border-radius: var(--radius-card);
  border: 1px solid var(--border-light);
  overflow: hidden;
  transition: box-shadow var(--transition-base), border-color var(--transition-base);
  position: relative;
}

.file-card:hover {
  box-shadow: var(--shadow-card-hover);
  border-color: var(--border-default);
}

.file-preview {
  position: relative;
  height: 148px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-secondary);
  overflow: hidden;
  cursor: pointer;
}

.file-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform var(--transition-slow);
}

.file-card:hover .file-preview img {
  transform: scale(1.03);
}

.file-icon-container {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48px;
  color: var(--text-disabled);
  transition: color var(--transition-base);
}

.file-card:hover .file-icon-container {
  color: var(--text-tertiary);
}

/* --- 缩略图加载占位符 --- */
.thumbnail-loading {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  background: var(--bg-secondary);
}

.thumbnail-loading-icon {
  font-size: 36px;
  color: var(--text-disabled);
  animation: pulse 1.5s ease-in-out infinite;
}

.thumbnail-loading-text {
  font-size: 12px;
  color: var(--text-disabled);
}

@keyframes pulse {
  0%, 100% { opacity: 0.4; }
  50% { opacity: 0.8; }
}

.file-actions {
  position: absolute;
  top: 8px;
  right: 8px;
  z-index: 10;
  opacity: 0;
  transition: opacity var(--transition-fast);
}

.file-card:hover .file-actions {
  opacity: 1;
}

.file-info {
  padding: 12px;
  border-top: 1px solid var(--border-light);
}

.file-name {
  font-size: 13px;
  font-weight: var(--font-weight-medium);
  color: var(--text-primary);
  margin-bottom: 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.file-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: var(--text-tertiary);
}

.file-size { color: var(--text-tertiary); }
.file-time { color: var(--text-disabled); }

/* --- 分组管理弹窗 --- */
.group-modal-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.group-form {
  display: flex;
  gap: 8px;
  align-items: center;
  padding: 12px;
  background: var(--bg-secondary);
  border-radius: var(--radius-md);
}

.existing-groups {
  max-height: 300px;
  overflow-y: auto;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  padding: 8px;
}

.existing-groups::-webkit-scrollbar { width: 4px; }
.existing-groups::-webkit-scrollbar-thumb { background: var(--border-light); border-radius: 2px; }

.existing-group-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  border-bottom: 1px solid var(--border-light);
  transition: background-color var(--transition-fast);
  border-radius: var(--radius-md);
}

.existing-group-item:hover { background: var(--bg-hover); }
.existing-group-item:last-child { border-bottom: none; }

.group-info-row {
  display: flex;
  align-items: center;
  flex: 1;
  gap: 8px;
}

.group-type-icon { font-size: 16px; width: 16px; height: 16px; color: var(--text-tertiary); }
.group-count { font-size: 12px; color: var(--text-tertiary); }
.no-groups { text-align: center; padding: 24px; color: var(--text-tertiary); font-size: 14px; }

/* --- 移动文件弹窗 --- */
.move-modal-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.move-hint {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0;
  padding: 10px 12px;
  background: var(--primary-50);
  border-radius: var(--radius-md);
  border-left: 3px solid var(--primary-500);
}

.move-group-list {
  max-height: 300px;
  overflow-y: auto;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
}

.move-group-list::-webkit-scrollbar { width: 4px; }
.move-group-list::-webkit-scrollbar-thumb { background: var(--border-light); border-radius: 2px; }

.move-group-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  cursor: pointer;
  transition: background-color var(--transition-fast), color var(--transition-fast);
  border-bottom: 1px solid var(--border-light);
}

.move-group-item:last-child { border-bottom: none; }
.move-group-item:hover { background: var(--bg-hover); }

.move-group-item.active {
  background: var(--primary-50);
  color: var(--primary-500);
}

.move-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
  padding-top: 12px;
  border-top: 1px solid var(--border-light);
}

/* --- 预览弹窗 --- */
.preview-content {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
  background: var(--bg-secondary);
  border-radius: var(--radius-md);
  padding: 16px;
}

/* --- 响应式 --- */
@media (max-width: 768px) {
  .file-management-container { flex-direction: column; }
  .sidebar { width: 100%; height: 180px; border-right: none; border-bottom: 1px solid var(--border-light); }
  .file-grid-container { grid-template-columns: repeat(auto-fill, minmax(150px, 1fr)); gap: 12px; }
  .file-preview { height: 110px; }
}
</style>
