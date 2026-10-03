<template>
  <div class="dataset-studio">
    <!-- 分类与列表共用主从工作区，保持单一高度链 -->
    <MasterDetailWorkspace :aside-width="228" class="dataset-workspace">
      <template #aside>
        <aside class="workspace-sidebar">
          <div class="sidebar-head">
            <h3>数据集分类</h3>
            <n-button text size="small" @click="goToCategoryManage">
              分类管理
            </n-button>
          </div>

          <div class="sidebar-shortcuts">
            <button
              class="scope-chip"
              :class="{ active: activeCategoryScope === 'all' }"
              type="button"
              @click="selectAllCategories"
            >
              全部数据集
            </button>
            <button
              class="scope-chip"
              :class="{ active: activeCategoryScope === 'uncategorized' }"
              type="button"
              @click="selectUncategorized"
            >
              未分类
            </button>
          </div>

          <NInput
            v-model:value="categoryKeyword"
            placeholder="搜索分类名称或编码"
            clearable
            class="category-search"
          >
            <template #prefix>
              <i class="i-material-symbols:search-rounded" />
            </template>
          </NInput>

          <div class="category-tree-shell">
            <n-empty v-if="categoryTreeNodes.length === 0" description="暂无分类，请前往分类管理页配置" size="small" />
            <n-tree
              v-else
              block-line
              :data="categoryTreeNodes"
              :default-expanded-keys="categoryTreeNodes.map(node => node.key)"
              :selected-keys="selectedTreeKeys"
              @update:selected-keys="handleCategoryTreeSelect"
            />
          </div>

          <div v-if="selectedCategoryNode" class="category-detail-card">
            <div class="category-detail-top">
              <div>
                <div class="category-detail-name">
                  {{ selectedCategoryNode.categoryName }}
                </div>
                <div class="category-detail-code">
                  {{ selectedCategoryNode.categoryCode }}
                </div>
              </div>
              <DictTag
                :options="statusOptions"
                :value="selectedCategoryNode.status"
                size="small"
                :bordered="false"
                force-tag
              />
            </div>
            <p class="category-detail-desc">
              {{ selectedCategoryNode.description || '当前分类暂无补充说明。' }}
            </p>
          </div>
        </aside>
      </template>

      <section class="workspace-main">
        <div class="main-toolbar">
          <div class="toolbar-title-row">
            <h3>数据集列表</h3>
            <div class="toolbar-title-meta">
              <span class="toolbar-scope">{{ activeCategoryScopeLabel }}</span>
              <n-button text @click="applySearch">
                刷新
              </n-button>
              <n-button type="primary" @click="handleAddDataset">
                新增数据集
              </n-button>
            </div>
          </div>

          <div class="toolbar-filters">
            <NInput
              v-model:value="queryForm.datasetName"
              class="toolbar-filter toolbar-filter--keyword"
              clearable
              placeholder="搜索数据集名称"
              @keydown.enter="applySearch"
            >
              <template #prefix>
                <i class="i-material-symbols:search-rounded" />
              </template>
            </NInput>
            <n-button @click="applySearch">
              搜索
            </n-button>
            <n-popover v-model:show="filtersVisible" trigger="click" placement="bottom-start">
              <template #trigger>
                <n-button :type="filterCount ? 'primary' : 'default'" secondary>
                  <template #icon>
                    <i class="i-material-symbols:filter-alt-outline" />
                  </template>
                  筛选{{ filterCount ? `（${filterCount}）` : '' }}
                </n-button>
              </template>
              <div class="dataset-filters-panel">
                <label>数据连接<NSelect v-model:value="queryForm.connectionId" clearable filterable placeholder="全部数据连接" :options="connectionOptions" /></label>
                <label>数据集类型<NSelect v-model:value="queryForm.datasetType" clearable placeholder="全部类型" :options="datasetTypeOptions" /></label>
                <label>发布状态<NSelect v-model:value="queryForm.publishStatus" clearable placeholder="全部发布状态" :options="publishStatusOptions" /></label>
                <div class="dataset-filters-actions">
                  <n-button @click="handleResetFilters(); filtersVisible = false">
                    重置
                  </n-button>
                  <n-button type="primary" @click="applySearch(); filtersVisible = false">
                    应用筛选
                  </n-button>
                </div>
              </div>
            </n-popover>
          </div>
        </div>

        <!-- crudRef 由原查询 composable 和 Pinia 配置面板使用 -->
        <AiCrudPage
          :ref="value => crudRef = value"
          class="dataset-crud"
          :api-config="{
            list: 'get@/data/dataset/page',
            detail: 'get@/data/dataset/:id',
            add: 'post@/data/dataset',
            update: 'put@/data/dataset',
            delete: 'delete@/data/dataset/:id',
          }"
          :show-search="false"
          :hide-toolbar="true"
          :columns="tableColumns"
          :edit-schema="editSchema"
          :before-render-form="beforeRenderForm"
          :before-render-detail="beforeRenderDetail"
          :before-submit="beforeSubmit"
          :hide-modal-footer="true"
          row-key="id"
          :load-detail-on-edit="true"
          :striped="false"
          :bordered="false"
          :scroll-x="tableScrollX"
          :edit-grid-cols="12"
          edit-label-placement="top"
          edit-form-class="data-dataset-edit-form"
          modal-type="modal"
          modal-width="min(1120px, calc(100vw - 32px))"
          add-button-text="新增数据集"
          @modal-close="handleDatasetModalClose"
        >
          <template #form-datasetEditor="{ formData, updateValue }">
            <DatasetDefinitionEditor :form-data="formData" :update-value="updateValue" />
          </template>
        </AiCrudPage>
      </section>
    </MasterDetailWorkspace>

    <!-- 字段与 SQL 预览弹窗 -->
    <n-modal
      v-model:show="fieldModalVisible"
      preset="card"
      :title="fieldModalTitle"
      :style="{ width: 'min(1320px, calc(100vw - 32px))' }"
      :segmented="{ content: 'soft' }"
      :mask-closable="false"
    >
      <div class="field-config-modal">
        <div class="field-config-head">
          <div>
            <div class="field-config-title">
              字段配置台
            </div>
            <div class="field-config-desc">
              维护显示名称、标准类型、字段角色和扩展属性。筛选/展示开关不再外露，保持运行时默认可用。
            </div>
          </div>
          <div class="field-config-stats">
            <div v-for="item in fieldConfigStats" :key="item.label" class="field-config-stat">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </div>
        </div>

        <n-alert v-if="fieldConfigReadonly" type="warning" :show-icon="false" class="field-config-alert">
          当前数据集已发布，字段配置处于只读状态。如需调整，请先下架数据集。
        </n-alert>

        <n-data-table
          class="field-config-table"
          :columns="fieldColumns"
          :data="fieldRows"
          :loading="fieldLoading"
          :pagination="{ pageSize: 10 }"
          :scroll-x="fieldTableScrollX"
          max-height="calc(100vh - 390px)"
          size="small"
          striped
        />
      </div>

      <template #footer>
        <div class="field-config-footer">
          <n-button @click="fieldModalVisible = false">
            关闭
          </n-button>
          <n-button
            v-if="currentFieldDataset?.publishStatus !== 1"
            secondary
            :loading="fieldLoading"
            @click="handleSyncCurrentFields"
          >
            同步字段
          </n-button>
          <n-button
            v-if="currentFieldDataset?.publishStatus !== 1"
            type="primary"
            :loading="fieldSaving"
            @click="handleSaveFieldConfig"
          >
            保存字段配置
          </n-button>
        </div>
      </template>
    </n-modal>

    <n-modal
      v-model:show="sqlPreviewVisible"
      preset="card"
      title="SQL预览结果"
      :style="{ width: 'min(1000px, calc(100vw - 32px))' }"
      :segmented="{ content: 'soft' }"
    >
      <n-data-table
        :columns="sqlPreviewColumns"
        :data="sqlPreviewRows"
        :loading="sqlPreviewLoading"
        :pagination="{ pageSize: 10 }"
        :scroll-x="sqlPreviewScrollX"
        size="small"
      />
    </n-modal>
  </div>
</template>

<script>
import { computed, onBeforeUnmount, ref } from 'vue'
import MasterDetailWorkspace from '@/components/common/MasterDetailWorkspace.vue'
import { useDatasetWorkspaceStore } from '@/stores/data/datasetWorkspaceStore'
import DatasetDefinitionEditor from './components/DatasetDefinitionEditor.vue'
import { useDatasetPage } from './composables/useDatasetPage'
import { datasetLocalComponents } from './datasetLocalComponents'

export default {
  name: 'DataDataset',
  components: {
    ...datasetLocalComponents,
    MasterDetailWorkspace,
    DatasetDefinitionEditor,
  },
  setup(_props, { expose }) {
    const api = useDatasetPage(expose)
    const workspace = useDatasetWorkspaceStore()
    workspace.bind(api)
    onBeforeUnmount(() => workspace.release(api))
    const filterCount = computed(() => [
      api.queryForm.connectionId,
      api.queryForm.datasetType,
      api.queryForm.publishStatus,
    ].filter(value => value !== null && value !== undefined && value !== '').length)
    const tableScrollX = computed(() => api.tableColumns.value.reduce((total, column) => total + column.width, 0))
    return { ...api, filtersVisible: ref(false), filterCount, tableScrollX }
  },
}
</script>

<style scoped src="./dataset-shell.css"></style>
