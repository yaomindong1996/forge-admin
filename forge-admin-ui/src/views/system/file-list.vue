<template>
  <MasterDetailWorkspace class="file-workspace" :aside-width="212">
    <template #aside>
      <FileSidebar />
    </template>
    <header class="workspace-header">
      <h1>{{ store.currentGroupTitle }}</h1>
      <div class="upload-controls">
        <NSelect
          v-if="store.storageConfigOptions.length > 1" v-model:value="store.selectedStorageConfigId"
          :options="store.storageConfigOptions" size="small" class="upload-location"
          aria-label="上传存储位置" title="上传存储位置"
        />
        <NUpload
          :action="store.uploadUrl"
          :headers="store.uploadHeaders"
          :data="store.uploadData"
          multiple
          :show-file-list="false"
          @before-upload="store.beforeUpload"
          @finish="store.uploadFinished"
          @error="handleUploadError"
        >
          <NButton type="primary" size="small" :loading="store.uploading">
            <template v-if="!store.uploading" #icon>
              <i class="i-lucide:upload" />
            </template>
            {{ store.uploading ? '上传中…' : '上传文件' }}
          </NButton>
        </NUpload>
      </div>
    </header>
    <div class="file-toolbar">
      <form class="search-controls" @submit.prevent="store.search">
        <NInput v-model:value="store.searchName" clearable size="small" placeholder="搜索文件名" aria-label="搜索文件名" class="file-search" @clear="clearName">
          <template #prefix>
            <i class="i-lucide:search search-icon" aria-hidden="true" />
          </template>
        </NInput>
        <NButton size="small" attr-type="submit">
          搜索
        </NButton>
        <NPopover v-model:show="store.filtersVisible" trigger="click" placement="bottom-start">
          <template #trigger>
            <NButton size="small" :type="store.extraFilterCount ? 'primary' : 'default'" :secondary="!!store.extraFilterCount">
              <template #icon>
                <i class="i-lucide:sliders-horizontal" />
              </template>
              筛选{{ store.extraFilterCount ? ` · ${store.extraFilterCount}` : '' }}
            </NButton>
          </template>
          <div class="filter-panel">
            <label>存储类型</label>
            <NSelect v-model:value="store.draftStorageType" :options="storageTypeOptions" placeholder="全部存储类型" clearable aria-label="筛选存储类型" />
            <label>业务类型</label>
            <NInput v-model:value="store.draftBusinessType" placeholder="输入业务类型" clearable aria-label="筛选业务类型" @keyup.enter="store.applyFilters" />
            <div class="filter-actions">
              <NButton size="small" @click="store.clearFilters">
                重置
              </NButton>
              <NButton size="small" type="primary" @click="store.applyFilters">
                应用筛选
              </NButton>
            </div>
          </div>
        </NPopover>
        <NButton v-if="store.hasFilters" text size="small" @click="store.clearFilters">
          清除筛选
        </NButton>
      </form>
      <div class="view-controls">
        <div class="view-switch" role="group" aria-label="文件显示方式">
          <button type="button" :class="{ active: store.viewMode === 'list' }" :aria-pressed="store.viewMode === 'list'" title="列表视图" aria-label="列表视图" @click="store.viewMode = 'list'">
            <i class="i-lucide:list" />
          </button>
          <button type="button" :class="{ active: store.viewMode === 'grid' }" :aria-pressed="store.viewMode === 'grid'" title="网格视图" aria-label="网格视图" @click="store.viewMode = 'grid'">
            <i class="i-lucide:layout-grid" />
          </button>
        </div>
        <NButton quaternary size="small" title="刷新文件" aria-label="刷新文件" :loading="store.loading" @click="store.refresh">
          <template #icon>
            <i class="i-lucide:refresh-cw" />
          </template>
        </NButton>
      </div>
    </div>
    <div class="file-content" :aria-busy="store.loading">
      <div v-if="store.loadError" class="content-state" role="alert">
        <NEmpty description="文件加载失败">
          <template #extra>
            <NButton size="small" @click="store.fetchFiles">
              重新加载
            </NButton>
          </template>
        </NEmpty>
      </div>
      <AiTable
        v-else-if="store.viewMode === 'list'" :columns="columns" :data-source="store.files" :loading="store.loading"
        :pagination="false" :show-toolbar="false" hide-selection :resizable="false" :scroll-x="800" :theme-overrides="tableThemeOverrides"
        :empty-title="emptyTitle" :empty-description="emptyDescription" row-key="id" class="file-table"
      />
      <NSpin v-else :show="store.loading" class="grid-loading">
        <FileGrid v-if="store.files.length" />
        <div v-else class="content-state">
          <NEmpty :description="emptyTitle">
            <template #extra>
              <span class="empty-description">{{ emptyDescription }}</span>
            </template>
          </NEmpty>
        </div>
      </NSpin>
    </div>
    <footer class="workspace-footer">
      <span class="result-count" aria-live="polite">{{ store.hasFilters ? '找到' : '共' }} {{ store.total }} 个文件</span>
      <NPagination
        :page="store.page" :page-size="store.pageSize" :item-count="store.total" :page-sizes="[20, 50, 100]"
        :show-size-picker="!isNarrow" :page-slot="isNarrow ? 3 : 5" :simple="isNarrow" :disabled="store.loading"
        size="small" @update:page="store.changePage" @update:page-size="store.changePageSize"
      />
    </footer>
  </MasterDetailWorkspace>
  <FileDialogs />
</template>

<script setup>
import { useMediaQuery } from '@vueuse/core'
import { NButton, NEmpty, NInput, NPagination, NPopover, NSelect, NSpin, NUpload } from 'naive-ui'
import { computed, h, onBeforeUnmount, onMounted } from 'vue'
import AiTable from '@/components/ai-form/AiTable.vue'
import MasterDetailWorkspace from '@/components/common/MasterDetailWorkspace.vue'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'
import { useFileListStore } from '@/stores/system/fileListStore'
import FileActions from './file-list/components/FileActions.vue'
import FileDialogs from './file-list/components/FileDialogs.vue'
import FileGrid from './file-list/components/FileGrid.vue'
import FileSidebar from './file-list/components/FileSidebar.vue'
import FileThumbnail from './file-list/components/FileThumbnail.vue'
import { canPreviewFile, formatFileDate, formatFileSize, isImageFile } from './file-list/utils'

defineOptions({ name: 'FileList' })
const store = useFileListStore()
const isNarrow = useMediaQuery('(max-width: 600px)')
const tableThemeOverrides = { tdColor: 'var(--bg-primary)', tdColorHover: 'var(--bg-secondary)', thColor: 'var(--bg-secondary)' }
const { dict } = useDict('sys_file_storage_type')
const storageTypeOptions = computed(() => dict.value.sys_file_storage_type || [])
const emptyTitle = computed(() => store.hasFilters ? '没有匹配的文件' : '暂无文件')
const emptyDescription = computed(() => store.hasFilters ? '试试其他关键词，或清除筛选条件。' : '点击右上方“上传文件”，添加第一个文件。')
const columns = computed(() => [
  {
    prop: 'originalName',
    label: '文件名称',
    minWidth: 260,
    render: file => h('div', { class: 'file-identity' }, [
      h(FileThumbnail, { file, preview: isImageFile(file) }),
      canPreviewFile(file)
        ? h('button', { type: 'button', class: 'file-title file-title-link', title: file.originalName, onClick: () => store.preview(file) }, file.originalName)
        : h('span', { class: 'file-title', title: file.originalName }, file.originalName),
    ]),
  },
  { prop: 'fileSize', label: '大小', width: 100, render: file => h('span', { class: 'file-secondary' }, formatFileSize(file.fileSize)) },
  { prop: 'storageType', label: '存储类型', width: 120, render: file => h(DictTag, { options: storageTypeOptions.value, value: file.storageType, size: 'small' }) },
  { prop: 'uploadTime', label: '上传时间', width: 166, render: file => h('span', { class: 'file-secondary' }, formatFileDate(file.uploadTime)) },
  { prop: 'action', label: '操作', width: 154, fixed: 'right', align: 'right', render: file => h(FileActions, { file }) },
].map(column => ({ ...column, sortable: false, filterable: false })))

function clearName() {
  store.searchName = ''
  store.search()
}
function handleUploadError({ file }) {
  store.uploadError()
  window.$message?.error(`“${file?.name || '文件'}”上传失败`)
}
onMounted(() => store.initialize())
onBeforeUnmount(() => store.dispose())
</script>

<style scoped>
.file-workspace {
  --file-link-color: var(--primary-color);
  color: var(--text-primary);
}
:global(.dark .file-workspace) {
  --file-link-color: color-mix(in srgb, var(--primary-color) 65%, white);
}
.workspace-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
  gap: 16px;
  padding: 20px 20px 16px;
}
.workspace-header h1 {
  min-width: 0;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 16px;
  font-weight: 600;
}
.upload-controls {
  display: flex;
  align-items: center;
  flex-shrink: 0;
  gap: 8px;
}
.upload-location {
  width: 155px;
}
.upload-controls :deep(.n-upload) {
  width: auto;
}
.file-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  flex-shrink: 0;
  gap: 12px;
  padding: 0 20px 16px;
}
.search-controls {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  min-width: 0;
}
.file-search {
  width: 250px;
}
.search-icon {
  color: var(--text-tertiary);
  font-size: 16px;
}
.view-controls {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
}
.view-switch {
  display: flex;
  gap: 2px;
  padding: 2px;
  border-radius: 4px;
  background: var(--bg-tertiary);
}
.view-switch button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 24px;
  padding: 0;
  border: 1px solid transparent;
  border-radius: 3px;
  background: transparent;
  color: var(--text-tertiary);
  cursor: pointer;
  transition:
    color 150ms,
    background 150ms;
}
.view-switch button.active {
  background: var(--bg-primary);
  color: var(--file-link-color);
  border-color: var(--border-light);
}
.view-switch button:focus-visible {
  outline: 2px solid var(--primary-color);
}
.view-switch i {
  font-size: 16px;
}
.file-content {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}
.file-table {
  flex: 1;
  min-height: 0;
  padding: 0 20px;
}
.file-table :deep(.n-data-table-th) {
  font-weight: 500;
  color: var(--text-secondary);
}
.file-table :deep(.file-identity) {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  width: 100%;
}
.file-table :deep(.file-title) {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 500;
}
.file-table :deep(.file-title-link) {
  padding: 0;
  border: 0;
  background: transparent;
  text-align: left;
  cursor: pointer;
}
.file-table :deep(.file-title-link:hover) {
  color: var(--file-link-color);
}
.file-table :deep(.file-title-link:focus-visible) {
  outline: 2px solid var(--primary-color);
  outline-offset: 3px;
}
.file-table :deep(.file-secondary) {
  color: var(--text-secondary);
  font-size: 12px;
  font-variant-numeric: tabular-nums;
}
.grid-loading {
  flex: 1;
  min-height: 0;
  overflow: auto;
  scrollbar-gutter: stable;
}
.grid-loading :deep(.n-spin-content) {
  min-height: 100%;
}
.content-state {
  display: flex;
  flex: 1;
  align-items: center;
  justify-content: center;
  min-height: 240px;
  padding: 32px 16px;
}
.empty-description {
  color: var(--text-tertiary);
  font-size: 12px;
}
.workspace-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  flex-shrink: 0;
  gap: 10px;
  padding: 14px 20px;
  border-top: 1px solid var(--border-light);
}
.result-count {
  color: var(--text-tertiary);
  font-size: 12px;
  font-variant-numeric: tabular-nums;
}
.filter-panel {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: min(260px, 76vw);
  padding: 4px;
}
.filter-panel label {
  color: var(--text-secondary);
  font-size: 12px;
}
.filter-panel label:not(:first-child) {
  margin-top: 6px;
}
.filter-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}
@media (max-width: 600px) {
  .workspace-header {
    flex-wrap: wrap;
    padding: 14px 12px;
    gap: 10px;
  }
  .workspace-header h1 {
    flex: 1;
  }
  .upload-location {
    width: 128px;
  }
  .file-toolbar {
    padding: 0 12px 12px;
    gap: 10px;
  }
  .search-controls {
    flex: 1 1 100%;
  }
  .file-search {
    flex: 1;
    min-width: 120px;
    width: auto;
  }
  .file-table {
    padding: 0 12px;
  }
  .workspace-footer {
    padding: 12px;
  }
}
</style>
