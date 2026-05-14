<template>
  <div class="go-project-items">
    <div class="items-header go-float-up">
      <div class="header-left">
        <h2 class="page-title go-text-neon">
          <span class="title-accent">//</span>
          {{ $t('project.all_project') }}
        </h2>
        <p class="page-subtitle">按目录查看大屏项目，并支持按名称检索和调整所属目录</p>
      </div>
    </div>

    <div class="items-layout">
      <aside class="directory-panel">
        <div class="panel-title-row">
          <div>
            <div class="panel-title">目录筛选</div>
            <div class="panel-desc">目录由专门页面统一维护，这里只负责筛选项目</div>
          </div>
        </div>

        <div class="directory-tree">
          <n-spin :show="loadingDirectories">
            <n-tree
              block-line
              key-field="key"
              label-field="label"
              children-field="children"
              :data="directoryTreeData"
              :selected-keys="[selectedDirectoryKey]"
              :default-expanded-keys="['all']"
              @update:selected-keys="handleDirectorySelect"
            />
          </n-spin>
        </div>
      </aside>

      <section class="content-panel">
        <div class="content-toolbar">
          <div class="toolbar-meta">
            <div class="toolbar-title">{{ currentDirectoryTitle }}</div>
            <div class="toolbar-desc">共 {{ pagination.itemCount }} 个项目</div>
          </div>
          <div class="toolbar-actions">
            <n-input
              v-model:value="filters.projectName"
              clearable
              placeholder="按大屏名称模糊检索"
              class="search-input"
              @keydown.enter.prevent="handleSearch"
            >
              <template #prefix>
                <n-icon><SearchIcon /></n-icon>
              </template>
            </n-input>
            <n-button @click="handleResetSearch">重置</n-button>
            <n-button @click="loadProjectList">
              <template #icon>
                <n-icon><ArrowRedoIcon /></n-icon>
              </template>
              刷新
            </n-button>
          </div>
        </div>

        <div v-if="loadingProjects" class="content-loading">
          <n-spin size="large">
            <template #description>
              <span style="color: #64748b; margin-top: 12px; display: block;">正在加载项目列表...</span>
            </template>
          </n-spin>
        </div>

        <template v-else>
          <n-grid
            v-if="projectCards.length"
            :x-gap="20"
            :y-gap="20"
            cols="1 s:2 m:2 l:3 xl:4 xxl:4"
            responsive="screen"
            style="width: auto"
          >
            <n-grid-item v-for="(item, index) in projectCards" :key="item.id">
              <div class="go-float-up" :style="{ animationDelay: `${index * 0.05}s` }">
                <project-items-card
                  :cardData="item"
                  @resize="resizeHandle"
                  @delete="handleDeleteProject"
                  @edit="editHandle"
                  @refresh="loadProjectList"
                  @move-directory="openMoveProjectDirectory"
                />
              </div>
            </n-grid-item>
          </n-grid>
          <div v-else class="content-empty">
            <n-empty description="当前条件下暂无大屏项目" />
          </div>

          <div class="list-pagination">
            <n-pagination
              :item-count="pagination.itemCount"
              :page="pagination.page"
              :page-size="pagination.pageSize"
              :page-sizes="[8, 12, 16, 24]"
              show-size-picker
              @update:page="handlePageChange"
              @update:page-size="handlePageSizeChange"
            />
          </div>
        </template>
      </section>
    </div>

    <project-items-modal-card
      v-if="modalData"
      :modalShow="modalShow"
      :cardData="modalData"
      @close="closeModal"
      @edit="editHandle"
      @move-directory="openMoveProjectDirectory"
    />

    <n-modal v-model:show="showMoveProjectModal" preset="card" title="调整所属目录" style="width: 520px">
      <n-form label-placement="top">
        <n-form-item label="项目名称">
          <n-input :value="movingProject?.title || ''" disabled />
        </n-form-item>
        <n-form-item label="目标目录">
          <n-tree-select
            v-model:value="moveProjectForm.directoryId"
            :options="directorySelectOptions"
            clearable
            filterable
            placeholder="请选择项目所属目录"
          />
        </n-form-item>
      </n-form>
      <template #action>
        <div class="modal-actions">
          <n-button @click="showMoveProjectModal = false">取消</n-button>
          <n-button type="primary" :loading="submittingMoveProject" @click="handleSubmitMoveProject">
            保存
          </n-button>
        </div>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { deleteProjectApi, getProjectDirectoryTreeApi, getProjectPageApi, updateProjectApi, type ForgeProject, type ReportDirectory } from '@/api/project'
import { icon } from '@/plugins'
import { goDialog } from '@/utils'
import { DialogEnum } from '@/enums/pluginEnum'
import { ProjectItemsCard } from './components/ProjectItemsCard'
import { ProjectItemsModalCard } from './components/ProjectItemsModalCard'
import { useModalDataInit } from './components/ProjectItemsList/hooks/useModal.hook'
import type { ChartList, Chartype } from './index.d'

type DirectoryTreeNode = {
  key: string
  label: string
  children?: DirectoryTreeNode[]
}

const { SearchIcon, ArrowRedoIcon } = icon.ionicons5

const loadingDirectories = ref(false)
const loadingProjects = ref(false)
const showMoveProjectModal = ref(false)
const submittingMoveProject = ref(false)
const selectedDirectoryKey = ref('all')
const directoryTreeRaw = ref<ReportDirectory[]>([])
const directoryLookup = ref<Map<string, ReportDirectory>>(new Map())
const projectCards = ref<ChartList>([])
const movingProject = ref<Chartype | null>(null)

const filters = reactive({
  projectName: ''
})

const pagination = reactive({
  page: 1,
  pageSize: 12,
  itemCount: 0
})

const moveProjectForm = reactive({
  directoryId: undefined as string | number | undefined
})

const buildDirectoryLookup = (directories: ReportDirectory[]) => {
  const lookup = new Map<string, ReportDirectory>()
  const walk = (items: ReportDirectory[]) => {
    items.forEach(item => {
      lookup.set(String(item.id), item)
      if (Array.isArray(item.children) && item.children.length) {
        walk(item.children)
      }
    })
  }
  walk(directories)
  return lookup
}

const mapDirectoryTreeNodes = (directories: ReportDirectory[]): DirectoryTreeNode[] => {
  return (directories || []).map(directory => ({
    key: String(directory.id),
    label: directory.directoryName,
    children: mapDirectoryTreeNodes(directory.children || [])
  }))
}

const buildDirectoryTreeNodes = (directories: ReportDirectory[]): DirectoryTreeNode[] => {
  return [
    {
      key: 'all',
      label: '全部目录',
      children: mapDirectoryTreeNodes(directories)
    }
  ]
}

const buildDirectorySelectOptions = (directories: ReportDirectory[]) => {
  return (directories || []).map(directory => ({
    label: directory.directoryName,
    key: String(directory.id),
    children: buildDirectorySelectOptions(directory.children || [])
  }))
}

const directoryTreeData = computed(() => buildDirectoryTreeNodes(directoryTreeRaw.value))
const directorySelectOptions = computed(() => buildDirectorySelectOptions(directoryTreeRaw.value))
const currentDirectoryTitle = computed(() => directoryLookup.value.get(selectedDirectoryKey.value)?.directoryName || '全部目录')

const { modalData, modalShow, closeModal, resizeHandle, editHandle } = useModalDataInit(loadProjectList)

function mapProjectCards(records: ForgeProject[]): ChartList {
  return (records || []).map(item => ({
    id: item.id,
    directoryId: item.directoryId,
    title: item.projectName,
    release: item.publishStatus === '1',
    label: item.remark || '项目',
    publishUrl: item.publishUrl,
    createTime: item.createTime,
    indexImg: item.indexImg
  })) as ChartList
}

async function loadDirectoryTree(preferredKey?: string) {
  try {
    loadingDirectories.value = true
    const res = await getProjectDirectoryTreeApi()
    directoryTreeRaw.value = res?.data || []
    directoryLookup.value = buildDirectoryLookup(directoryTreeRaw.value)
    const targetKey = preferredKey || selectedDirectoryKey.value
    if (targetKey !== 'all' && !directoryLookup.value.has(String(targetKey))) {
      selectedDirectoryKey.value = 'all'
    } else if (targetKey) {
      selectedDirectoryKey.value = String(targetKey)
    }
  } catch (error: any) {
    window.$message.error(error?.message || '获取目录树失败')
  } finally {
    loadingDirectories.value = false
  }
}

async function loadProjectList() {
  try {
    loadingProjects.value = true
    const res = await getProjectPageApi({
      pageNum: pagination.page,
      pageSize: pagination.pageSize,
      projectName: filters.projectName || undefined,
      directoryId: selectedDirectoryKey.value === 'all' ? undefined : selectedDirectoryKey.value
    })
    const pageData = res?.data
    projectCards.value = mapProjectCards(pageData?.records || [])
    pagination.itemCount = pageData?.total || 0
  } catch (error: any) {
    window.$message.error(error?.message || '获取项目列表失败')
  } finally {
    loadingProjects.value = false
  }
}

function handleDirectorySelect(keys: Array<string | number>) {
  selectedDirectoryKey.value = keys?.length ? String(keys[0]) : 'all'
  pagination.page = 1
  loadProjectList()
}

function handleSearch() {
  pagination.page = 1
  loadProjectList()
}

function handleResetSearch() {
  filters.projectName = ''
  pagination.page = 1
  loadProjectList()
}

function handlePageChange(page: number) {
  pagination.page = page
  loadProjectList()
}

function handlePageSizeChange(pageSize: number) {
  pagination.page = 1
  pagination.pageSize = pageSize
  loadProjectList()
}

function handleDeleteProject(cardData: Chartype) {
  goDialog({
    type: DialogEnum.DELETE,
    promise: true,
    onPositiveCallback: () => deleteProjectApi(cardData.id),
    promiseResCallback: async () => {
      window.$message.success('删除成功')
      await loadProjectList()
    }
  })
}

function openMoveProjectDirectory(cardData: Chartype) {
  movingProject.value = cardData
  moveProjectForm.directoryId = cardData.directoryId ? String(cardData.directoryId) : undefined
  showMoveProjectModal.value = true
}

async function handleSubmitMoveProject() {
  if (!movingProject.value) {
    return
  }
  if (!moveProjectForm.directoryId) {
    window.$message.warning('请选择目录')
    return
  }
  submittingMoveProject.value = true
  try {
    await updateProjectApi({
      id: movingProject.value.id,
      directoryId: moveProjectForm.directoryId
    })
    window.$message.success('所属目录已更新')
    showMoveProjectModal.value = false
    await loadProjectList()
  } catch (error: any) {
    window.$message.error(error?.message || '更新所属目录失败')
  } finally {
    submittingMoveProject.value = false
  }
}

onMounted(async () => {
  await loadDirectoryTree()
  await loadProjectList()
})
</script>

<style lang="scss" scoped>
@include go(project-items) {
  padding: 0 32px 32px;
  min-height: calc(100vh - 2px);
}

.items-header {
  padding: 28px 0 24px;

  .page-title {
    font-size: 22px;
    font-weight: 700;
    margin: 0 0 4px;

    .title-accent {
      color: $--color-accent;
      font-weight: 300;
      margin-right: 8px;
    }
  }

  .page-subtitle {
    margin: 0;
    font-size: 12px;
    @include fetch-color(3);
  }
}

.items-layout {
  display: grid;
  grid-template-columns: minmax(260px, 300px) minmax(0, 1fr);
  gap: 20px;
  min-height: calc(100vh - 160px);
}

.directory-panel,
.content-panel {
  border: 1px solid rgba(var(--app-theme-rgb), 0.08);
  border-radius: $--border-radius-lg;
  @include fetch-bg-color('background-color1');
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.18);
}

.directory-panel {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.panel-title-row,
.content-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.04);
}

.panel-title,
.toolbar-title {
  font-size: 15px;
  font-weight: 600;
}

.panel-desc,
.toolbar-desc {
  margin-top: 4px;
  font-size: 12px;
  @include fetch-color(3);
}

.directory-tree {
  flex: 1;
  min-height: 0;
  padding: 12px 10px 16px;
  overflow: auto;
}

.content-panel {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.toolbar-actions,
.modal-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.search-input {
  width: 280px;
}

.content-loading,
.content-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 420px;
}

.content-panel :deep(.n-grid) {
  padding: 20px;
}

.list-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 4px 20px 20px;
}

@media (max-width: 1200px) {
  .items-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .content-toolbar,
  .panel-title-row {
    flex-direction: column;
    align-items: stretch;
  }

  .toolbar-actions {
    flex-wrap: wrap;
  }

  .search-input {
    width: 100%;
  }
}
</style>
