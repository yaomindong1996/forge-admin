import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { useAuthStore } from '@/store'
import { downloadFile, request, resolveFileAccessUrl, resolveRenderableFileUrl } from '@/utils'
import { canPreviewFile, isPdfFile } from '@/views/system/file-list/utils'

export const useFileListStore = defineStore('system-file-list', () => {
  const authStore = useAuthStore()
  const selectedGroup = ref('all')
  const viewMode = ref('list')
  const files = ref([])
  const groups = ref([])
  const statistics = ref({ total: null, imageCount: null, documentCount: null })
  const storageConfigs = ref([])
  const selectedStorageConfigId = ref(null)
  const searchName = ref('')
  const draftStorageType = ref(null)
  const draftBusinessType = ref('')
  const filters = ref({ originalName: '', storageType: null, businessType: '' })
  const filtersVisible = ref(false)
  const page = ref(1)
  const pageSize = ref(20)
  const total = ref(0)
  const loading = ref(false)
  const uploadingCount = ref(0)
  const uploading = computed(() => uploadingCount.value > 0)
  const loadError = ref('')
  const groupModalVisible = ref(false)
  const newGroupName = ref('')
  const newGroupType = ref('default')
  const groupSaving = ref(false)
  const moveFile = ref(null)
  const selectedMoveGroup = ref(null)
  const moveSaving = ref(false)
  const renameFile = ref(null)
  const renameName = ref('')
  const renameSaving = ref(false)
  const previewVisible = ref(false)
  const previewUrl = ref('')
  const previewType = ref('')
  const previewName = ref('')
  const previewLoadingId = ref('')
  let listVersion = 0
  let previewVersion = 0

  const isCustomGroup = computed(() => /^\d+$/.test(selectedGroup.value))
  const currentGroupTitle = computed(() => {
    if (selectedGroup.value === 'all')
      return '全部文件'
    if (selectedGroup.value === 'images')
      return '图片'
    if (selectedGroup.value === 'documents')
      return '文档'
    return groups.value.find(group => group.id === selectedGroup.value)?.groupName || '文件分组'
  })
  const hasFilters = computed(() => !!(filters.value.originalName || filters.value.storageType || filters.value.businessType))
  const extraFilterCount = computed(() => Number(!!filters.value.storageType) + Number(!!filters.value.businessType))
  const storageConfigOptions = computed(() => storageConfigs.value.map(config => ({
    label: `${config.configName}${config.isDefault ? '（默认）' : ''}`,
    value: config.id,
  })))
  const uploadUrl = `${import.meta.env.VITE_REQUEST_PREFIX || ''}/api/file/upload`
  const uploadHeaders = computed(() => ({ Authorization: authStore.accessToken ? `Bearer ${authStore.accessToken}` : '' }))
  const uploadData = computed(() => ({
    businessType: 'common',
    businessId: '',
    storageType: storageConfigs.value.find(config => config.id === selectedStorageConfigId.value)?.storageType || 'local',
    groupId: isCustomGroup.value ? selectedGroup.value : '',
  }))

  async function fetchFiles() {
    const version = ++listVersion
    loading.value = true
    loadError.value = ''
    const params = { pageNum: page.value, pageSize: pageSize.value }
    Object.entries(filters.value).forEach(([key, value]) => {
      if (value)
        params[key] = value
    })
    if (selectedGroup.value === 'images')
      params.mimeType = 'image/'
    else if (selectedGroup.value === 'documents')
      params.mimeType = 'application/'
    else if (isCustomGroup.value)
      params.groupId = selectedGroup.value
    try {
      const response = await request.get('/system/file/metadata/page', { params, needTip: false })
      if (version !== listVersion)
        return
      if (response.code !== 200)
        throw new Error(response.msg || response.message || '加载文件失败')
      files.value = response.data?.records || []
      total.value = Number(response.data?.total) || 0
      const lastPage = Math.max(1, Math.ceil(total.value / pageSize.value))
      if (page.value > lastPage) {
        page.value = lastPage
        return fetchFiles()
      }
    }
    catch (error) {
      if (version !== listVersion)
        return
      files.value = []
      total.value = 0
      loadError.value = error.message || '加载文件失败，请重试'
    }
    finally {
      if (version === listVersion)
        loading.value = false
    }
  }

  async function fetchGroups() {
    try {
      const response = await request.get('/system/file/group/list')
      if (response.code !== 200)
        throw new Error('加载文件分组失败')
      groups.value = (response.data || []).map(group => ({ ...group, id: String(group.id) }))
    }
    catch {
      window.$message?.error('加载文件分组失败')
    }
  }

  async function fetchStatistics() {
    try {
      const response = await request.get('/system/file/metadata/statistics')
      if (response.code === 200)
        statistics.value = response.data || {}
    }
    catch {
      statistics.value = { total: null, imageCount: null, documentCount: null }
    }
  }

  async function fetchStorageConfigs() {
    try {
      const response = await request.get('/system/storage/config/options')
      if (response.code !== 200)
        throw new Error('加载存储配置失败')
      storageConfigs.value = (response.data || []).map(config => ({ ...config, id: String(config.id) }))
      if (!storageConfigs.value.some(config => config.id === selectedStorageConfigId.value))
        selectedStorageConfigId.value = (storageConfigs.value.find(config => config.isDefault) || storageConfigs.value[0])?.id || null
    }
    catch {
      window.$message?.error('加载存储配置失败')
    }
  }

  async function refresh() {
    await Promise.all([fetchFiles(), fetchGroups(), fetchStatistics()])
  }

  async function initialize() {
    await Promise.all([refresh(), fetchStorageConfigs()])
  }

  function selectGroup(id) {
    if (selectedGroup.value === String(id))
      return
    selectedGroup.value = String(id)
    page.value = 1
    files.value = []
    return fetchFiles()
  }

  function search() {
    filters.value.originalName = searchName.value.trim()
    page.value = 1
    return fetchFiles()
  }

  function applyFilters() {
    filters.value = { originalName: searchName.value.trim(), storageType: draftStorageType.value, businessType: draftBusinessType.value.trim() }
    filtersVisible.value = false
    page.value = 1
    return fetchFiles()
  }

  function clearFilters() {
    searchName.value = ''
    draftStorageType.value = null
    draftBusinessType.value = ''
    return applyFilters()
  }

  function changePage(value) {
    page.value = value
    return fetchFiles()
  }

  function changePageSize(value) {
    pageSize.value = value
    return changePage(1)
  }

  async function addGroup() {
    if (!newGroupName.value.trim() || groupSaving.value)
      return
    groupSaving.value = true
    try {
      const response = await request.post('/system/file/group', {
        groupName: newGroupName.value.trim(),
        groupType: newGroupType.value,
        groupCode: `group_${Date.now()}`,
      })
      if (response.code !== 200)
        throw new Error(response.msg || '添加分组失败')
      newGroupName.value = ''
      newGroupType.value = 'default'
      window.$message?.success('分组已添加')
      await fetchGroups()
    }
    catch (error) {
      window.$message?.error(error.message || '添加分组失败')
    }
    finally {
      groupSaving.value = false
    }
  }

  function deleteGroup(group) {
    window.$dialog?.warning({
      title: '删除分组',
      content: `确定删除“${group.groupName}”分组？分组内的文件会保留。`,
      positiveText: '删除分组',
      negativeText: '取消',
      onPositiveClick: async () => {
        try {
          const response = await request.delete(`/system/file/group/${group.id}`)
          if (response.code !== 200)
            throw new Error(response.msg || '删除分组失败')
          if (selectedGroup.value === group.id) {
            selectedGroup.value = 'all'
            page.value = 1
          }
          await refresh()
          window.$message?.success('分组已删除')
        }
        catch (error) {
          window.$message?.error(error.message || '删除分组失败')
          return false
        }
      },
    })
  }

  function openRename(file) {
    renameFile.value = file
    renameName.value = file.originalName || ''
  }

  async function confirmRename() {
    const name = renameName.value.trim()
    if (!name || !renameFile.value || renameSaving.value)
      return
    renameSaving.value = true
    try {
      const response = await request.put(`/system/file/metadata/rename?fileId=${encodeURIComponent(renameFile.value.fileId)}&originalName=${encodeURIComponent(name)}`)
      if (response.code !== 200)
        throw new Error(response.msg || '重命名失败')
      renameFile.value = null
      await fetchFiles()
      window.$message?.success('文件已重命名')
    }
    catch (error) {
      window.$message?.error(error.message || '重命名失败')
    }
    finally {
      renameSaving.value = false
    }
  }

  function openMove(file) {
    moveFile.value = file
    selectedMoveGroup.value = file.groupId == null ? null : String(file.groupId)
  }

  async function confirmMove() {
    if (!selectedMoveGroup.value || !moveFile.value || moveSaving.value)
      return
    moveSaving.value = true
    try {
      const response = await request.put('/system/file/metadata', { id: moveFile.value.id, groupId: selectedMoveGroup.value })
      if (response.code !== 200)
        throw new Error(response.msg || '移动文件失败')
      moveFile.value = null
      await refresh()
      window.$message?.success('文件已移动')
    }
    catch (error) {
      window.$message?.error(error.message || '移动文件失败')
    }
    finally {
      moveSaving.value = false
    }
  }

  function closePreview() {
    if (previewUrl.value.startsWith('blob:'))
      URL.revokeObjectURL(previewUrl.value)
    previewVisible.value = false
    previewUrl.value = ''
  }

  async function preview(file) {
    if (previewLoadingId.value || !canPreviewFile(file))
      return
    const version = ++previewVersion
    previewLoadingId.value = file.fileId
    try {
      const url = await resolveRenderableFileUrl(file.fileId)
      if (version !== previewVersion) {
        if (url?.startsWith('blob:'))
          URL.revokeObjectURL(url)
        return
      }
      if (!url)
        throw new Error('文件加载失败')
      closePreview()
      previewUrl.value = url
      previewType.value = isPdfFile(file) ? 'pdf' : 'image'
      previewName.value = file.originalName
      previewVisible.value = true
    }
    catch (error) {
      window.$message?.error(`预览失败：${error.message || '请重试'}`)
    }
    finally {
      if (version === previewVersion)
        previewLoadingId.value = ''
    }
  }

  async function download(file) {
    try {
      await downloadFile(file.fileId, file.originalName)
    }
    catch (error) {
      window.$message?.error(`下载失败：${error.message || '请重试'}`)
    }
  }

  async function copyLink(file) {
    try {
      const url = await resolveFileAccessUrl(file.fileId)
      if (!url)
        throw new Error('链接获取失败')
      if (navigator.clipboard && window.isSecureContext) {
        await navigator.clipboard.writeText(url)
      }
      else {
        const textarea = document.createElement('textarea')
        textarea.value = url
        textarea.style.position = 'fixed'
        textarea.style.opacity = '0'
        document.body.appendChild(textarea)
        try {
          textarea.select()
          if (!document.execCommand('copy'))
            throw new Error('复制失败')
        }
        finally {
          textarea.remove()
        }
      }
      window.$message?.success('链接已复制')
    }
    catch {
      window.$message?.error('复制链接失败，请检查浏览器剪贴板权限')
    }
  }

  function deleteFile(file) {
    window.$dialog?.warning({
      title: '删除文件',
      content: `确定删除“${file.originalName}”？删除后将无法恢复。`,
      positiveText: '删除文件',
      negativeText: '取消',
      onPositiveClick: async () => {
        try {
          const response = await request.delete(`/system/file/metadata/${file.id}`)
          if (response.code !== 200)
            throw new Error(response.msg || '删除文件失败')
          await refresh()
          window.$message?.success('文件已删除')
        }
        catch (error) {
          window.$message?.error(error.message || '删除文件失败')
          return false
        }
      },
    })
  }

  function beginUpload() {
    uploadingCount.value += 1
  }

  function endUpload() {
    uploadingCount.value = Math.max(0, uploadingCount.value - 1)
  }

  function beforeUpload() {
    beginUpload()
    return true
  }

  function uploadFinished({ file, event }) {
    endUpload()
    try {
      const response = JSON.parse(event.target.response)
      if (response.code !== 200)
        throw new Error(response.msg || '上传失败')
      const uploaded = response.data || {}
      const alreadyListed = !!uploaded.fileId && files.value.some(item => item.fileId === uploaded.fileId)
      window.$message?.success(
        alreadyListed
          ? `“${file.name}”已存在（内容相同，未新增记录）`
          : `“${file.name}”上传成功`,
      )
      page.value = 1
      refresh()
      return file
    }
    catch (error) {
      window.$message?.error(error.message || '上传失败')
      file.status = 'error'
      return file
    }
  }

  function uploadError() {
    endUpload()
  }

  function dispose() {
    listVersion++
    previewVersion++
    previewLoadingId.value = ''
    uploadingCount.value = 0
    closePreview()
    groupModalVisible.value = false
    moveFile.value = null
    renameFile.value = null
    filtersVisible.value = false
  }

  return {
    selectedGroup,
    viewMode,
    files,
    groups,
    statistics,
    selectedStorageConfigId,
    searchName,
    draftStorageType,
    draftBusinessType,
    filters,
    filtersVisible,
    page,
    pageSize,
    total,
    loading,
    uploading,
    loadError,
    groupModalVisible,
    newGroupName,
    newGroupType,
    groupSaving,
    moveFile,
    selectedMoveGroup,
    moveSaving,
    renameFile,
    renameName,
    renameSaving,
    previewVisible,
    previewUrl,
    previewType,
    previewName,
    previewLoadingId,
    currentGroupTitle,
    hasFilters,
    extraFilterCount,
    storageConfigOptions,
    uploadUrl,
    uploadHeaders,
    uploadData,
    initialize,
    refresh,
    fetchFiles,
    selectGroup,
    search,
    applyFilters,
    clearFilters,
    changePage,
    changePageSize,
    addGroup,
    deleteGroup,
    openRename,
    confirmRename,
    openMove,
    confirmMove,
    closePreview,
    preview,
    download,
    copyLink,
    deleteFile,
    beforeUpload,
    uploadFinished,
    uploadError,
    dispose,
  }
})
