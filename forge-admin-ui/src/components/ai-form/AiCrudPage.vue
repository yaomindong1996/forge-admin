<!--
  eslint-disable vue/component-name-in-template-casing
  CRUD 页面组件
  基于 Naive UI 的完整 CRUD 解决方案
  参考 LxBasePage 设计，集成搜索、表格、新增、编辑、删除、导入导出等功能

  @author AI Form Team
  @version 1.0.0
-->

<template>
  <div class="ai-crud-page" :class="{ 'is-form-only': formOnly }">
    <div v-if="flowStartPageLoading" class="ai-crud-page-loading-mask">
      <n-spin size="large">
        <template #description>
          正在发起流程...
        </template>
      </n-spin>
    </div>

    <div v-if="formOnlySubmitted" class="ai-crud-form-only-result">
      <n-result
        status="success"
        :title="formOnlySuccessTitle"
        :description="formOnlySuccessDescription"
      >
        <template #footer>
          <n-button type="primary" @click="resetFormOnly">
            继续填报
          </n-button>
        </template>
      </n-result>
    </div>

    <div v-else-if="formOnly" class="ai-crud-form-only">
      <header class="form-only-head">
        <h2>{{ resolvedFormOnlyTitle }}</h2>
      </header>
      <div class="form-only-body">
        <AiForm
          v-if="showDefaultDetailContent"
          ref="formRef"
          v-model:value="formData"
          :class="resolvedEditFormClass"
          :style="editFormStyle"
          :schema="modalFormSchema"
          :grid-cols="editGridCols"
          :label-width="editLabelWidth"
          :label-placement="editLabelPlacement"
          :label-align="editLabelAlign"
          :size="editSize"
          :x-gap="editXGap"
          :y-gap="editYGap"
          :show-feedback="editShowFeedback"
          :show-actions="false"
          :context="formContext"
          :form-assets="formAssets"
        >
          <template v-for="slotName in formSlots" #[slotName]="slotProps">
            <slot :name="`form-${slotName}`" v-bind="slotProps" />
          </template>
        </AiForm>
        <ChildTableEditor
          v-if="hasChildrenConfig"
          ref="childFormRef"
          v-model:value="childFormData"
          :children-config="visibleChildrenConfig"
          :readonly="isDetailMode"
          :parent-form-data="formData"
          :context="formContext"
        />
      </div>
      <footer class="form-only-footer">
        <n-button secondary @click="resetFormOnly">
          重置
        </n-button>
        <n-button type="primary" :loading="confirmLoading" @click="handleModalConfirm">
          {{ formOnlySubmitText }}
        </n-button>
      </footer>
    </div>

    <!-- 搜索表单区域 -->
    <div v-if="!formOnly && showInlineListPane && hasSearchSchema && searchPanelVisible" class="ai-crud-search">
      <AiSearch
        ref="searchRef"
        v-model="searchParams"
        :schema="normalizedSearchSchema"
        :grid-cols="searchGridCols"
        :label-width="searchLabelWidth"
        :enable-collapse="searchEnableCollapse"
        :max-visible-fields="searchMaxVisibleFields"
        :y-gap="searchYGap"
        :before-reset="beforeRenderReset"
        @search="handleSearch"
        @reset="handleReset"
      >
        <!-- 透传搜索表单插槽 -->
        <template v-for="slotName in searchSlots" #[slotName]="slotProps">
          <slot :name="`search-${slotName}`" v-bind="slotProps" />
        </template>

        <!-- 搜索表单额外操作按钮 -->
        <template #extra-actions="{ formData: searchFormData }">
          <slot name="search-extra-actions" :form-data="searchFormData" />
        </template>
      </AiSearch>
    </div>

    <!-- 主内容区域 -->
    <div
      v-if="!formOnly"
      class="ai-crud-main"
      :class="{
        'has-inline-workspace': inlineWorkspaceVisible,
        'is-tab-workspace': isTabWorkspaceMode,
        'is-form-workspace-active': showInlineFormWorkspacePane,
      }"
    >
      <div
        v-if="isTabWorkspaceMode && inlineWorkspaceVisible"
        class="ai-crud-workspace-tabs"
        role="tablist"
      >
        <button
          type="button"
          class="inline-form-tab inline-form-tab--list"
          :class="{ active: activeInlineWorkspaceKey === INLINE_WORKSPACE_LIST_KEY }"
          role="tab"
          :aria-selected="activeInlineWorkspaceKey === INLINE_WORKSPACE_LIST_KEY"
          @click="handleInlineWorkspaceListTab"
        >
          <span class="inline-form-tab__title">列表</span>
        </button>
        <button
          v-for="tab in inlineFormTabs"
          :key="tab.key"
          type="button"
          class="inline-form-tab"
          :class="{ active: tab.key === activeInlineWorkspaceKey }"
          role="tab"
          :aria-selected="tab.key === activeInlineWorkspaceKey"
          @click="handleInlineFormTabChange(tab.key)"
        >
          <span class="inline-form-tab__title">{{ inlineFormTabTitle(tab) }}</span>
          <span
            v-if="resolvedTabWorkspace.showDirtyMark && tab.dirty"
            class="inline-form-tab__dirty"
            aria-label="未保存"
          />
          <span
            class="inline-form-tab__close"
            title="关闭"
            @click.stop="closeInlineFormTab(tab.key)"
          >
            <n-icon size="13"><CloseOutline /></n-icon>
          </span>
        </button>
      </div>

      <!-- 数据表格区域 -->
      <div v-show="showInlineListPane" class="ai-crud-table">
        <AiTable
          ref="tableRef"
          v-model:checked-row-keys="selectedKeys"
          v-model:expanded-row-keys="expandedRowKeys"
          :columns="tableColumns"
          :data-source="dataSource"
          :loading="tableLoading"
          :pagination="paginationConfig"
          :row-key="rowKeyFn"
          :hide-selection="hideSelection"
          :striped="striped"
          :bordered="bordered"
          :size="tableSize"
          :render-mode="activeRenderMode"
          :card-props="cardProps"
          :show-render-mode-switch="showRenderModeSwitch"
          :show-search-toggle="hasSearchSchema"
          :search-visible="searchPanelVisible"
          :max-height="computedMaxHeight"
          :scroll-x="computedScrollX"
          :resizable="resolvedResizable"
          v-bind="tableProps"
          @page-change="handlePageChange"
          @page-size-change="handlePageSizeChange"
          @refresh="handleRefresh"
          @search-toggle="handleSearchToggle"
          @render-mode-change="handleRenderModeChange"
        >
          <template #toolbar-left>
            <!-- 工具栏区域 -->
            <div v-if="!hideToolbar" class="ai-crud-toolbar">
              <slot name="toolbar">
                <div class="toolbar-left">
                  <slot name="toolbar-start" />
                  <!-- 新增按钮 -->
                  <n-button
                    v-if="!hideAdd"
                    type="primary"
                    size="small"
                    @click="handleAdd()"
                  >
                    <template #icon>
                      <n-icon><Add /></n-icon>
                    </template>
                    {{ addButtonText }}
                  </n-button>
                  <!-- 批量删除按钮 -->
                  <n-button
                    v-if="!hideBatchDelete"
                    size="small"
                    type="error"
                    secondary
                    :disabled="selectedKeys.length === 0"
                    @click="handleBatchDelete"
                  >
                    <template #icon>
                      <n-icon><TrashOutline /></n-icon>
                    </template>
                    批量删除
                  </n-button>
                  <!-- 批量导入按钮 -->
                  <n-button
                    v-if="showImport"
                    size="small"
                    @click="handleShowImport"
                  >
                    <template #icon>
                      <n-icon><CloudUploadOutline /></n-icon>
                    </template>
                    批量导入
                  </n-button>

                  <!-- 导出按钮 -->
                  <n-button
                    v-if="showExport"
                    size="small"
                    strong
                    secondary
                    :loading="exportLoading"
                    @click="handleExport"
                  >
                    <template #icon>
                      <n-icon><DownloadOutline /></n-icon>
                    </template>
                    {{ exportButtonText }}
                  </n-button>

                  <n-button
                    v-if="showExportTaskEntry"
                    size="small"
                    tertiary
                    @click="handleOpenExportTasks"
                  >
                    <template #icon>
                      <n-icon><TimeOutline /></n-icon>
                    </template>
                    导出任务
                  </n-button>

                  <AiCustomQuery
                    v-if="enableCustomQuery && resolvedCustomQueryConfigKey"
                    :config-key="resolvedCustomQueryConfigKey"
                    :columns="props.columns"
                    :search-schema="searchSchema"
                    :edit-schema="editSchema"
                    :render-mode="activeRenderMode"
                    @apply="handleApplyCustomQuery"
                    @clear="handleClearCustomQuery"
                  />

                  <n-button
                    v-for="action in visibleToolbarActions"
                    :key="action.key || action.label"
                    size="small"
                    :type="resolveButtonType(action)"
                    @click="handleActionClick(action, null)"
                  >
                    {{ action.label }}
                  </n-button>

                  <slot name="toolbar-end" />
                </div>

                <!-- 右侧操作按钮 -->
                <div class="toolbar-right">
                  <slot name="toolbar-right-start" />
                  <slot name="toolbar-right-end" />
                </div>
              </slot>
            </div>
          </template>
          <!-- 透传表格插槽 -->
          <template v-for="slotName in tableSlots" #[slotName]="slotProps">
            <slot :name="`table-${slotName}`" v-bind="slotProps" />
          </template>
          <template v-if="$slots['table-card']" #card="slotProps">
            <slot name="table-card" v-bind="slotProps" />
          </template>
        </AiTable>
      </div>

      <div
        v-if="showInlineFormWorkspacePane"
        class="ai-crud-inline-workspace"
        :class="{ 'is-tab-workspace': isTabWorkspaceMode }"
      >
        <section class="ai-crud-inline-form-panel">
          <header class="inline-form-panel-head">
            <div>
              <strong>{{ activeInlineFormTitle }}</strong>
              <span>{{ inlineFormModeLabel }}</span>
            </div>
            <n-button
              quaternary
              :circle="isTabWorkspaceMode"
              size="small"
              @click="handleCloseActiveInlineFormTab"
            >
              <template #icon>
                <n-icon><CloseOutline /></n-icon>
              </template>
              <template v-if="!isTabWorkspaceMode">
                返回列表
              </template>
            </n-button>
          </header>

          <div class="inline-form-panel-body">
            <n-tabs
              v-if="showDetailFlowTabs"
              v-model:value="detailActiveTab"
              type="line"
              animated
              class="ai-crud-detail-tabs"
            >
              <n-tab-pane name="business" tab="业务数据">
                <AiForm
                  v-if="showDefaultDetailContent"
                  ref="formRef"
                  v-model:value="formData"
                  :class="resolvedEditFormClass"
                  :style="editFormStyle"
                  :schema="modalFormSchema"
                  :grid-cols="editGridCols"
                  :label-width="editLabelWidth"
                  :label-placement="editLabelPlacement"
                  :label-align="editLabelAlign"
                  :size="editSize"
                  :x-gap="editXGap"
                  :y-gap="editYGap"
                  :show-feedback="editShowFeedback"
                  :show-actions="false"
                  :context="formContext"
                  :form-assets="formAssets"
                >
                  <template v-for="slotName in formSlots" #[slotName]="slotProps">
                    <slot :name="`form-${slotName}`" v-bind="slotProps" />
                  </template>
                </AiForm>
                <ChildTableEditor
                  v-if="showDefaultDetailChildren"
                  ref="childFormRef"
                  v-model:value="childFormData"
                  :children-config="visibleChildrenConfig"
                  :readonly="isDetailMode"
                  :parent-form-data="formData"
                  :context="formContext"
                />
                <AiCrudRowExpand
                  v-if="showDetailPanels"
                  class="ai-crud-detail-panels"
                  :config="normalizedDetailPanelConfig"
                  :row="formData"
                  :row-key-value="detailPanelRowKeyValue"
                  :context="formContext"
                />
              </n-tab-pane>
              <n-tab-pane name="flow" tab="流程进度" display-directive="show:lazy">
                <AiCrudFlowDetail
                  :runtime="detailRuntime"
                  :loading="detailRuntimeLoading"
                  :show-timeline="detailFlowTimelineVisible"
                  :show-diagram="detailFlowDiagramVisible"
                />
              </n-tab-pane>
            </n-tabs>
            <template v-else>
              <AiForm
                ref="formRef"
                v-model:value="formData"
                :class="resolvedEditFormClass"
                :style="editFormStyle"
                :schema="modalFormSchema"
                :grid-cols="editGridCols"
                :label-width="editLabelWidth"
                :label-placement="editLabelPlacement"
                :label-align="editLabelAlign"
                :size="editSize"
                :x-gap="editXGap"
                :y-gap="editYGap"
                :show-feedback="editShowFeedback"
                :show-actions="false"
                :context="formContext"
                :form-assets="formAssets"
              >
                <template v-for="slotName in formSlots" #[slotName]="slotProps">
                  <slot :name="`form-${slotName}`" v-bind="slotProps" />
                </template>
              </AiForm>
              <ChildTableEditor
                v-if="showDefaultDetailChildren"
                ref="childFormRef"
                v-model:value="childFormData"
                :children-config="visibleChildrenConfig"
                :readonly="isDetailMode"
                :parent-form-data="formData"
                :context="formContext"
              />
              <AiCrudRowExpand
                v-if="showDetailPanels"
                class="ai-crud-detail-panels"
                :config="normalizedDetailPanelConfig"
                :row="formData"
                :row-key-value="detailPanelRowKeyValue"
                :context="formContext"
              />
            </template>
          </div>

          <footer v-if="!hideModalFooter && !isDetailMode" class="inline-form-panel-footer">
            <n-button @click="handleInlineFormCancel">
              取消
            </n-button>
            <n-button
              type="primary"
              :loading="confirmLoading"
              @click="handleModalConfirm"
            >
              确定
            </n-button>
          </footer>
        </section>
      </div>
    </div>

    <!-- 新增/编辑/详情弹窗 - Modal 模式。详情默认使用弹窗，避免动态页详情占用右侧抽屉。 -->
    <n-modal
      v-if="!formOnly && !usesInlineFormWorkspace && (resolvedFormOpenMode === 'modal' || isDetailMode)"
      v-model:show="modalVisible"
      :title="modalTitle"
      preset="card"
      :style="{ width: activeModalWidth }"
      :segmented="{ content: 'soft', footer: 'soft' }"
      :closable="true"
      :mask-closable="false"
      @after-leave="handleModalClose"
    >
      <n-tabs
        v-if="showDetailFlowTabs"
        v-model:value="detailActiveTab"
        type="line"
        animated
        class="ai-crud-detail-tabs"
      >
        <n-tab-pane name="business" tab="业务数据">
          <AiForm
            v-if="showDefaultDetailContent"
            ref="formRef"
            v-model:value="formData"
            :class="resolvedEditFormClass"
            :style="editFormStyle"
            :schema="modalFormSchema"
            :grid-cols="editGridCols"
            :label-width="editLabelWidth"
            :label-placement="editLabelPlacement"
            :label-align="editLabelAlign"
            :size="editSize"
            :x-gap="editXGap"
            :y-gap="editYGap"
            :show-feedback="editShowFeedback"
            :show-actions="false"
            :context="formContext"
            :form-assets="formAssets"
          >
            <template v-for="slotName in formSlots" #[slotName]="slotProps">
              <slot :name="`form-${slotName}`" v-bind="slotProps" />
            </template>
          </AiForm>
          <ChildTableEditor
            v-if="showDefaultDetailChildren"
            ref="childFormRef"
            v-model:value="childFormData"
            :children-config="visibleChildrenConfig"
            :readonly="isDetailMode"
            :parent-form-data="formData"
            :context="formContext"
          />
          <AiCrudRowExpand
            v-if="showDetailPanels"
            class="ai-crud-detail-panels"
            :config="normalizedDetailPanelConfig"
            :row="formData"
            :row-key-value="detailPanelRowKeyValue"
            :context="formContext"
          />
        </n-tab-pane>
        <n-tab-pane name="flow" tab="流程进度" display-directive="show:lazy">
          <AiCrudFlowDetail
            :runtime="detailRuntime"
            :loading="detailRuntimeLoading"
            :show-timeline="detailFlowTimelineVisible"
            :show-diagram="detailFlowDiagramVisible"
          />
        </n-tab-pane>
      </n-tabs>
      <template v-else>
        <AiForm
          ref="formRef"
          v-model:value="formData"
          :class="resolvedEditFormClass"
          :style="editFormStyle"
          :schema="modalFormSchema"
          :grid-cols="editGridCols"
          :label-width="editLabelWidth"
          :label-placement="editLabelPlacement"
          :label-align="editLabelAlign"
          :size="editSize"
          :x-gap="editXGap"
          :y-gap="editYGap"
          :show-feedback="editShowFeedback"
          :show-actions="false"
          :context="formContext"
          :form-assets="formAssets"
        >
          <!-- 透传表单插槽 -->
          <template v-for="slotName in formSlots" #[slotName]="slotProps">
            <slot :name="`form-${slotName}`" v-bind="slotProps" />
          </template>
        </AiForm>
        <ChildTableEditor
          v-if="showDefaultDetailChildren"
          ref="childFormRef"
          v-model:value="childFormData"
          :children-config="visibleChildrenConfig"
          :readonly="isDetailMode"
          :parent-form-data="formData"
          :context="formContext"
        />
        <AiCrudRowExpand
          v-if="showDetailPanels"
          class="ai-crud-detail-panels"
          :config="normalizedDetailPanelConfig"
          :row="formData"
          :row-key-value="detailPanelRowKeyValue"
          :context="formContext"
        />
      </template>

      <!-- 弹窗底部按钮 -->
      <template v-if="!hideModalFooter && !isDetailMode" #footer>
        <n-space justify="end">
          <n-button @click="handleModalCancel">
            取消
          </n-button>
          <n-button
            type="primary"
            :loading="confirmLoading"
            @click="handleModalConfirm"
          >
            确定
          </n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 新增/编辑抽屉 - Drawer 模式 -->
    <n-drawer
      v-else-if="!formOnly && !usesInlineFormWorkspace && !isDetailMode && resolvedFormOpenMode === 'drawer'"
      v-model:show="modalVisible"
      :width="modalWidth"
      :placement="drawerPlacement"
      :mask-closable="false"
      @after-leave="handleModalClose"
    >
      <n-drawer-content :title="modalTitle" :closable="true">
        <AiForm
          ref="formRef"
          v-model:value="formData"
          :class="resolvedEditFormClass"
          :style="editFormStyle"
          :schema="modalFormSchema"
          :grid-cols="editGridCols"
          :label-width="editLabelWidth"
          :label-placement="editLabelPlacement"
          :label-align="editLabelAlign"
          :size="editSize"
          :x-gap="editXGap"
          :y-gap="editYGap"
          :show-feedback="editShowFeedback"
          :show-actions="false"
          :context="formContext"
          :form-assets="formAssets"
        >
          <!-- 透传表单插槽 -->
          <template v-for="slotName in formSlots" #[slotName]="slotProps">
            <slot :name="`form-${slotName}`" v-bind="slotProps" />
          </template>
        </AiForm>
        <ChildTableEditor
          v-if="hasChildrenConfig"
          ref="childFormRef"
          v-model:value="childFormData"
          :children-config="visibleChildrenConfig"
          :readonly="isDetailMode"
          :parent-form-data="formData"
          :context="formContext"
        />

        <!-- 抽屉底部按钮 -->
        <template v-if="!hideModalFooter && !isDetailMode" #footer>
          <n-space justify="end">
            <n-button @click="handleModalCancel">
              取消
            </n-button>
            <n-button
              type="primary"
              :loading="confirmLoading"
              @click="handleModalConfirm"
            >
              确定
            </n-button>
          </n-space>
        </template>
      </n-drawer-content>
    </n-drawer>

    <!-- 导入弹窗 -->
    <n-modal
      v-model:show="importModalVisible"
      title="批量导入"
      preset="card"
      style="width: 600px"
      :mask-closable="false"
    >
      <n-upload
        :data="importData"
        :custom-request="handleImportRequest"
        :max="1"
        accept=".xlsx,.xls"
      >
        <n-upload-dragger>
          <div style="margin-bottom: 12px">
            <n-icon size="48" :depth="3">
              <CloudUploadOutline />
            </n-icon>
          </div>
          <n-text style="font-size: var(--font-size-lg)">
            点击或者拖动文件到该区域来上传
          </n-text>
          <n-p depth="3" style="margin: 8px 0 0 0">
            请上传 .xlsx 或 .xls 格式的文件
          </n-p>
        </n-upload-dragger>
      </n-upload>

      <template v-if="importTemplateUrl" #footer>
        <n-space justify="space-between">
          <n-button text @click="handleDownloadTemplate">
            <template #icon>
              <n-icon><DownloadOutline /></n-icon>
            </template>
            下载导入模板
          </n-button>
          <n-button @click="importModalVisible = false">
            关闭
          </n-button>
        </n-space>
      </template>
    </n-modal>

    <n-modal
      v-model:show="commandActionModalVisible"
      :title="commandActionTitle"
      preset="card"
      style="width: min(720px, calc(100vw - 32px))"
      :mask-closable="false"
    >
      <AiForm
        ref="commandActionFormRef"
        v-model:value="commandActionFormData"
        :schema="commandActionFormSchema"
        :grid-cols="2"
        :show-actions="false"
        :context="commandActionFormContext"
      />

      <template #footer>
        <n-space justify="end">
          <n-button :disabled="commandActionSubmitting" @click="closeCommandActionModal">
            取消
          </n-button>
          <n-button type="primary" :loading="commandActionSubmitting" @click="submitCommandAction">
            确定
          </n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 导出任务抽屉 -->
    <n-drawer
      v-model:show="exportTaskDrawerVisible"
      :width="exportTaskDrawerWidth"
      placement="right"
    >
      <n-drawer-content title="导出任务" :closable="true">
        <template #header-extra>
          <n-button
            size="small"
            quaternary
            aria-label="刷新导出任务"
            title="刷新"
            :loading="exportTaskLoading"
            @click="loadExportTasks"
          >
            <template #icon>
              <n-icon><RefreshOutline /></n-icon>
            </template>
          </n-button>
        </template>

        <div v-if="activeExportTask" class="export-task-current">
          <div class="export-task-current__main">
            <span class="export-task-current__title">{{ activeExportTask.fileName || '导出任务' }}</span>
            <n-tag size="small" :type="resolveExportTaskTagType(activeExportTask.status)">
              {{ resolveExportTaskStatusText(activeExportTask.status) }}
            </n-tag>
          </div>
          <n-progress
            type="line"
            :percentage="activeExportTask.progress || 0"
            :processing="isExportTaskRunning(activeExportTask)"
            indicator-placement="inside"
          />
        </div>

        <n-data-table
          remote
          size="small"
          :bordered="false"
          :columns="exportTaskColumns"
          :data="exportTasks"
          :loading="exportTaskLoading"
          :pagination="exportTaskPaginationConfig"
          :row-key="row => row.id"
          :scroll-x="720"
        />
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup>
/* eslint-disable vue/custom-event-name-casing */
import {
  Add,
  CloseOutline,
  CloudUploadOutline,
  DownloadOutline,
  RefreshOutline,
  TimeOutline,
  TrashOutline,
} from '@vicons/ionicons5'
import { NButton, NDropdown, NProgress, NTag } from 'naive-ui'
import { computed, h, nextTick, onBeforeUnmount, onMounted, ref, useSlots, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { customQueryExecute } from '@/api/ai'
import { executeBusinessAction } from '@/api/business-app'
import { previewFormula } from '@/api/formula'
import AuthImage from '@/components/common/AuthImage.vue'
import DictTag from '@/components/DictTag.vue'
import ChildTableEditor from '@/components/page-templates/ChildTableEditor.vue'
import { useUserStore } from '@/store'
import { downloadFile, request } from '@/utils'
import { postEncrypt } from '@/utils/encrypt-request'
import AiCrudFlowDetail from './AiCrudFlowDetail.vue'
import { aiCrudPageProps } from './AiCrudPageProps'
import AiCrudRowExpand from './AiCrudRowExpand.vue'
import AiCustomQuery from './AiCustomQuery.vue'
import AiForm from './AiForm.vue'
import AiSearch from './AiSearch.vue'
import AiTable from './AiTable.vue'
import { normalizeExpandConfig, shouldExpandRow } from './expand-utils'

/**
 * ==================== Props 定义 ====================
 */
const props = defineProps(aiCrudPageProps)

/**
 * ==================== Emits 定义 ====================
 */
const emit = defineEmits([
  'load-list-success', // 列表加载成功
  'load-list-error', // 列表加载失败
  'add', // 点击新增
  'edit', // 点击编辑
  'detail', // 点击查看详情
  'delete', // 删除成功
  'submit-success', // 提交成功
  'submit-error', // 提交失败
  'selection-change', // 选中项变化
  'modal-open', // 弹窗打开
  'modal-close', // 弹窗关闭
  'render-mode-change', // 列表/卡片模式切换
  'custom-action', // 自定义按钮点击
])
const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const slots = useSlots()

/**
 * ==================== Refs ====================
 */
const searchRef = ref(null)
const tableRef = ref(null)
const formRef = ref(null)
const childFormRef = ref(null)
const commandActionFormRef = ref(null)

/**
 * ==================== 响应式数据 ====================
 */
// 搜索参数
const searchParams = ref({})

// 表格数据
const dataSource = ref([])
const tableLoading = ref(false)
const selectedKeys = ref([])
const expandedRowKeys = ref([])
const customQueryPayload = ref(null)
const customQueryFields = ref([])
const activeRenderMode = ref(props.renderMode || 'table')
const searchPanelVisible = ref(props.showSearch !== false)

// 分页
const pagination = ref({
  page: props.pageNum,
  pageSize: props.pageSize,
  itemCount: 0,
})

// 弹窗
const modalVisible = ref(false)
const modalTitle = ref('')
const modalStatus = ref('') // 'add' | 'edit' | 'detail'
const formData = ref({})
const childFormData = ref({})
const confirmLoading = ref(false)
const currentRow = ref(null)
const inlineFormTabs = ref([])
const activeInlineFormTabKey = ref('')
const INLINE_WORKSPACE_LIST_KEY = 'list'
const activeInlineWorkspaceKey = ref(INLINE_WORKSPACE_LIST_KEY)
const inlineFormHydrating = ref(false)
const formOnlySubmitted = ref(false)
const detailRuntime = ref(null)
const detailRuntimeLoading = ref(false)
const detailActiveTab = ref('business')
const actionLoadingKeys = ref(new Set())
const flowStartPageLoading = ref(false)
const commandActionModalVisible = ref(false)
const commandActionSubmitting = ref(false)
const commandActionFormData = ref({})
const commandActionContext = ref(null)
const commandActionTitle = computed(() => commandActionContext.value?.action?.label || commandActionContext.value?.action?.actionName || '业务动作')
const commandActionFormSchema = computed(() => {
  const config = normalizeCommandActionRuntimeConfig(commandActionContext.value?.action || {})
  return Array.isArray(config.formSchema) ? config.formSchema : []
})
const commandActionFormContext = computed(() => ({
  row: commandActionContext.value?.row || null,
  action: commandActionContext.value?.action || null,
  mode: 'businessAction',
}))
const formulaRuntimeTimers = new Map()
const formulaRuntimeSequences = new Map()
let inlineFormTabSequence = 0

// 导入
const importModalVisible = ref(false)

// 导出
const exportLoading = ref(false)

// 导出任务
const exportTaskDrawerVisible = ref(false)
const exportTaskLoading = ref(false)
const exportTasks = ref([])
const activeExportTaskId = ref(null)
const exportTaskPollTimer = ref(null)
const exportTaskDrawerWidth = 'min(680px, 92vw)'
const exportTaskPagination = ref({
  page: 1,
  pageSize: 10,
  itemCount: 0,
})
const visibleToolbarActions = computed(() => (Array.isArray(props.toolbarActions) ? props.toolbarActions : [])
  .filter(action => action?.visible !== false
    && hasRuntimePermission(action?.permissionCode)
    && matchDisplayCondition(action?.displayCondition || action?.visibleCondition, {})))

/**
 * 操作列最大显示按钮数
 */
const maxActionButtons = 2

/**
 * 渲染操作列（支持自动折叠）
 * 使用文字链接风格，紧凑排列，超过 maxActionButtons 个时折叠到"更多"下拉
 * @param {object} row - 行数据
 * @param {Array} actions - 操作按钮配置 [{ label, key, type, onClick, visible }]
 */
function renderActionColumn(row, actions, maxVisibleActions = maxActionButtons) {
  const mergedActions = mergeRuntimeActions(actions, row)
  // 过滤不可见的按钮
  const visibleActions = mergedActions.filter((action) => {
    if (typeof action.visible === 'function')
      return action.visible(row)
    if (action.visible === false)
      return false
    if (!hasRuntimePermission(action.permissionCode))
      return false
    if (!matchDisplayCondition(action.displayCondition || action.visibleCondition, row))
      return false
    return true
  })

  if (visibleActions.length === 0) {
    return h('span', { style: { color: '#999' } }, '-')
  }

  // 所有按钮都能直接显示
  if (visibleActions.length <= maxVisibleActions) {
    return h('div', { class: 'table-action-column' }, visibleActions.map((action, index) => {
      const nodes = []
      if (index > 0) {
        nodes.push(h('span', { class: 'table-action-divider' }, ' | '))
      }
      const typeClass = action.type ? `type-${action.type}` : ''
      const isDanger = action.type === 'error' || action.type === 'danger'
      const loading = isActionLoading(action, row)
      const disabled = isActionDisabled(action, row) || loading
      nodes.push(h('a', {
        class: ['table-action-link', typeClass, isDanger ? 'danger' : '', disabled ? 'disabled' : '', loading ? 'loading' : ''],
        title: disabled ? actionDisabledReason(action, row) : action.label,
        onClick: (e) => {
          e?.stopPropagation()
          e?.preventDefault()
          if (disabled) {
            showActionDisabledMessage(action, row)
            return
          }
          if (action.onClick)
            handleCustomActionClick(action, row)
          else handleActionClick(action, row)
        },
      }, resolveActionDisplayLabel(action, row)))
      return nodes
    }).flat())
  }

  // 需要折叠：显示前 maxActionButtons 个，其余放入"更多"下拉
  const inlineActions = visibleActions.slice(0, maxVisibleActions)
  const dropdownOptions = visibleActions.slice(maxVisibleActions).map(action => ({
    label: resolveActionDisplayLabel(action, row),
    key: action.key || action.label,
    disabled: isActionDisabled(action, row) || isActionLoading(action, row),
  }))

  const inlineNodes = inlineActions.map((action, index) => {
    const nodes = []
    if (index > 0) {
      nodes.push(h('span', { class: 'table-action-divider' }, ' | '))
    }
    const typeClass = action.type ? `type-${action.type}` : ''
    const isDanger = action.type === 'error' || action.type === 'danger'
    const loading = isActionLoading(action, row)
    const disabled = isActionDisabled(action, row) || loading
    nodes.push(h('a', {
      class: ['table-action-link', typeClass, isDanger ? 'danger' : '', disabled ? 'disabled' : '', loading ? 'loading' : ''],
      title: disabled ? actionDisabledReason(action, row) : action.label,
      onClick: (e) => {
        e?.stopPropagation()
        e?.preventDefault()
        if (disabled) {
          showActionDisabledMessage(action, row)
          return
        }
        if (action.onClick)
          handleCustomActionClick(action, row)
        else handleActionClick(action, row)
      },
    }, resolveActionDisplayLabel(action, row)))
    return nodes
  }).flat()

  return h('div', { class: 'table-action-column' }, [
    ...inlineNodes,
    h('span', { class: 'table-action-divider' }, ' | '),
    h(NDropdown, {
      options: dropdownOptions,
      trigger: 'click',
      onSelect: (key) => {
        const action = visibleActions.find(a => (a.key || a.label) === key)
        if (action && isActionDisabled(action, row)) {
          showActionDisabledMessage(action, row)
          return
        }
        if (action && isActionLoading(action, row)) {
          showActionDisabledMessage(action, row)
          return
        }
        if (action?.onClick)
          handleCustomActionClick(action, row)
        else handleActionClick(action || key, row)
      },
    }, {
      default: () => h('a', {
        class: 'table-action-link',
        onClick: (e) => { e?.preventDefault() },
      }, '更多'),
    }),
  ])
}

function mergeRuntimeActions(actions = [], row) {
  const next = Array.isArray(actions) ? [...actions] : []
  const runtimeActions = [
    ...(Array.isArray(props.runtimeActions) ? props.runtimeActions : []),
    ...(Array.isArray(row?._runtimeActions) ? row._runtimeActions : []),
  ].map(action => normalizeRuntimeAction(action, row)).filter(Boolean)
  runtimeActions.forEach((action) => {
    const existingIndex = next.findIndex(item => sameAction(item, action))
    if (existingIndex >= 0) {
      next[existingIndex] = {
        ...next[existingIndex],
        ...action,
        label: next[existingIndex].label || action.label,
      }
    }
    else {
      next.push(action)
    }
  })
  return next
}

function normalizeRuntimeAction(action, row) {
  if (!action || action.visible === false)
    return null
  const key = action.key || action.actionType || action.label
  if (!key)
    return null
  return {
    ...action,
    key,
    label: action.label || key,
    actionType: action.actionType || key,
    objectCode: resolveRuntimeObjectCode(action, row),
    recordId: action.recordId || resolveRowKeyValue(row),
  }
}

function resolveRuntimeObjectCode(action = {}, row = {}) {
  return firstNonBlankText(
    action.objectCode,
    action.businessObjectCode,
    action.targetObjectCode,
    action.referenceObjectCode,
    action.candidateObjectCode,
    action.props?.objectCode,
    action.props?.businessObjectCode,
    props.businessObjectCode,
    row?._runtimeObjectCode,
    row?.objectCode,
    row?.businessObjectCode,
    row?.targetObjectCode,
    row?.referenceObjectCode,
  )
}

function firstNonBlankText(...values) {
  for (const value of values) {
    const text = String(value ?? '').trim()
    if (text)
      return text
  }
  return ''
}

function sameAction(left, right) {
  if (!left || !right)
    return false
  const leftKey = String(left.key || '').toUpperCase()
  const rightKey = String(right.key || '').toUpperCase()
  const leftType = String(left.actionType || '').toUpperCase()
  const rightType = String(right.actionType || '').toUpperCase()
  if (leftKey && rightKey && leftKey === rightKey)
    return true
  return leftType && rightType && leftType === rightType
}

function isActionDisabled(action, row) {
  if (typeof action.disabled === 'function')
    return !!action.disabled(row)
  return !!action.disabled
}

function actionDisabledReason(action, row) {
  if (isActionLoading(action, row))
    return resolveActionTextValue(action.loadingReason, row) || '操作执行中，请稍候'
  if (typeof action.disabledReason === 'function')
    return action.disabledReason(row)
  return action.disabledReason || '当前状态不可执行'
}

function showActionDisabledMessage(action, row) {
  window.$message?.warning(actionDisabledReason(action, row))
}

function resolveActionDisplayLabel(action, row) {
  if (isActionLoading(action, row)) {
    const loadingLabel = resolveActionTextValue(action.loadingLabel, row)
    if (loadingLabel)
      return loadingLabel
  }
  return resolveActionTextValue(action.label, row) || action.key || ''
}

function resolveActionTextValue(value, row) {
  if (typeof value === 'function')
    return value(row)
  if (value === undefined || value === null)
    return ''
  return String(value)
}

async function handleCustomActionClick(action, row) {
  const loadingKey = getActionLoadingKey(action, row)
  if (loadingKey && actionLoadingKeys.value.has(loadingKey)) {
    window.$message?.info(resolveActionTextValue(action.loadingReason, row) || '操作正在执行，请稍候')
    return
  }
  setActionLoading(loadingKey, true)
  try {
    await action.onClick(row)
  }
  catch (error) {
    const failureMessage = resolveActionTextValue(action.failureMessage, row) || error?.message || '操作失败'
    window.$message?.error(failureMessage)
  }
  finally {
    setActionLoading(loadingKey, false)
  }
}

function hasRuntimePermission(permissionCode = '') {
  const code = String(permissionCode || '').trim()
  if (!code)
    return true
  if (userStore.isAdmin || userStore.isTenantAdmin)
    return true
  const permissions = [
    ...(Array.isArray(userStore.permissions) ? userStore.permissions : []),
    ...(Array.isArray(userStore.apiPermissions) ? userStore.apiPermissions : []),
    ...(Array.isArray(userStore.getDataPermission) ? userStore.getDataPermission : []),
  ]
  return permissions.includes('**') || permissions.includes(code)
}

function matchDisplayCondition(expression = '', row = {}) {
  const text = String(expression || '').trim()
  if (!text)
    return true
  const lowerText = text.toLowerCase()
  const inIndex = lowerText.indexOf(' in ')
  if (inIndex > 0) {
    const actual = resolveConditionValue(row, text.slice(0, inIndex).trim())
    const expectedValues = text.slice(inIndex + 4).split(',').map(item => item.trim()).filter(Boolean)
    return expectedValues.includes(String(actual ?? ''))
  }
  const operator = text.includes('!=') ? '!=' : text.includes('==') ? '==' : text.includes('=') ? '=' : ''
  if (operator) {
    const [fieldName, ...expectedParts] = text.split(operator)
    const actual = String(resolveConditionValue(row, fieldName.trim()) ?? '')
    const expected = stripConditionQuote(expectedParts.join(operator))
    return operator === '!=' ? actual !== expected : actual === expected
  }
  return true
}

function resolveConditionValue(row = {}, path = '') {
  return String(path || '').split('.').filter(Boolean).reduce((value, key) => value?.[key], row)
}

function stripConditionQuote(value = '') {
  return String(value || '').trim().replace(/^['"]|['"]$/g, '')
}

/**
 * 处理操作列按钮点击（内置 key 映射）
 */
function handleActionClick(actionOrKey, row) {
  const action = typeof actionOrKey === 'string' ? { key: actionOrKey, label: actionOrKey } : actionOrKey || {}
  if (row?._dataScopeWritable === false && ['edit', 'delete', 'addChild'].includes(action.key)) {
    window.$message.warning('该节点仅用于导航展示，不能执行数据操作')
    return
  }
  switch (action.key) {
    case 'addChild':
      handleAddChild(row)
      break
    case 'edit':
      handleEdit(row)
      break
    case 'detail':
      handleDetail(row)
      break
    case 'delete':
      handleDelete(row)
      break
    default:
      handleConfiguredAction(action, row)
      break
  }
}

async function handleConfiguredAction(action, row) {
  const actionType = action.actionType || 'route'
  const normalizedActionType = String(actionType).toUpperCase()
  if (actionType === 'START_FLOW' || action.key === 'START_FLOW') {
    await startFlowAction(action, row)
    return
  }
  emit('custom-action', { action, row })
  if (action.confirmText && !(await confirmConfiguredAction(resolveActionText(action.confirmText, row))))
    return
  if (normalizedActionType === 'COMMAND') {
    await handleCommandAction(action, row)
    return
  }
  if (actionType === 'refresh') {
    loadList()
    return
  }
  if (['CALL_API', 'REQUEST'].includes(normalizedActionType)) {
    await callConfiguredApiAction(action, row)
    return
  }
  if (actionType === 'route' && action.routePath) {
    const path = buildActionTarget(action, row)
    if ((action.openTarget || '_self') === '_blank') {
      const route = router.resolve(path)
      window.open(route.href, '_blank')
    }
    else {
      router.push(path)
    }
    handleConfiguredActionSuccess(action)
    return
  }
  if (actionType === 'external' && action.routePath) {
    window.open(buildActionTarget(action, row), action.openTarget || '_blank')
    handleConfiguredActionSuccess(action)
  }
}

function handleConfiguredActionSuccess(action = {}) {
  if (action.successBehavior === 'refreshList')
    loadList()
  else if (action.successBehavior === 'goBack')
    router.back()
}

async function startFlowAction(action, row) {
  const objectCode = resolveRuntimeObjectCode(action, row)
  const recordId = action.recordId || resolveRowKeyValue(row)
  if (!objectCode || !recordId) {
    window.$message.warning('缺少业务对象或记录ID，无法发起主流程')
    return
  }
  const loadingKey = getActionLoadingKey(action, row)
  if (loadingKey && actionLoadingKeys.value.has(loadingKey)) {
    window.$message.info('流程正在发起，请稍候')
    return
  }
  const confirmed = await confirmConfiguredAction(
    resolveActionText(action.confirmText || '确定要发起该记录的主流程吗？', row),
    { title: action.label || '发起主流程', positiveText: '发起流程' },
  )
  if (!confirmed)
    return
  setActionLoading(loadingKey, true)
  flowStartPageLoading.value = true
  try {
    const res = await request.post('/ai/business/flow/start', {
      objectCode,
      recordId,
    })
    if (row)
      row._documentRuntime = res?.data || row?._documentRuntime || null
    window.$message.success('流程已发起')
    await loadList()
  }
  catch (error) {
    window.$message.error(error.message || '发起主流程失败')
  }
  finally {
    flowStartPageLoading.value = false
    setActionLoading(loadingKey, false)
  }
}

async function callConfiguredApiAction(action, row) {
  const requestInfo = buildConfiguredApiRequest(action, row)
  if (!requestInfo.url) {
    if (requestInfo.config.capabilityCode) {
      window.$message.info('已触发自定义能力事件')
      return null
    }
    window.$message.warning('未配置接口地址，无法调用 API')
    return null
  }
  const loadingKey = getActionLoadingKey(action, row)
  if (loadingKey && actionLoadingKeys.value.has(loadingKey)) {
    window.$message.info('操作正在执行，请稍候')
    return null
  }
  setActionLoading(loadingKey, true)
  try {
    const response = await sendConfiguredApiRequest(requestInfo)
    const successMessage = action.successMessage || requestInfo.config.successMessage || '操作成功'
    if (successMessage)
      window.$message.success(resolveActionText(successMessage, row))
    const successBehavior = action.successBehavior || requestInfo.config.successBehavior
    if (successBehavior) {
      handleConfiguredActionSuccess({ ...action, successBehavior })
    }
    else if (!isFalseLike(requestInfo.config.refreshAfter)) {
      await loadList()
    }
    return response
  }
  catch (error) {
    const failureMessage = action.failureMessage || requestInfo.config.failureMessage || error.message || '操作失败'
    window.$message.error(resolveActionText(failureMessage, row))
    return null
  }
  finally {
    setActionLoading(loadingKey, false)
  }
}

async function handleCommandAction(action, row) {
  const config = normalizeCommandActionRuntimeConfig(action)
  if (Array.isArray(config.formSchema) && config.formSchema.length) {
    commandActionContext.value = { action, row }
    commandActionFormData.value = buildCommandActionInitialData(config, row)
    commandActionModalVisible.value = true
    await nextTick()
    commandActionFormRef.value?.restoreValidation?.()
    return
  }
  await executeCommandAction(action, row, {})
}

async function submitCommandAction() {
  const context = commandActionContext.value
  if (!context?.action)
    return
  commandActionSubmitting.value = true
  try {
    await nextTick()
    await commandActionFormRef.value?.validate?.()
    const latestFormData = commandActionFormRef.value?.getFormData?.() || commandActionFormData.value || {}
    const success = await executeCommandAction(context.action, context.row, latestFormData, { fromModal: true })
    if (success)
      closeCommandActionModal()
  }
  finally {
    commandActionSubmitting.value = false
  }
}

function closeCommandActionModal() {
  if (commandActionSubmitting.value)
    return
  commandActionModalVisible.value = false
  commandActionFormData.value = {}
  commandActionContext.value = null
}

async function executeCommandAction(action, row, formData = {}, options = {}) {
  const config = normalizeCommandActionRuntimeConfig(action)
  const objectCode = resolveRuntimeObjectCode(action, row)
  const recordId = action.recordId || resolveRowKeyValue(row)
  const actionCode = action.actionCode || action.key
  if (!objectCode || !actionCode) {
    window.$message?.warning('缺少业务对象或动作编码，无法执行')
    return false
  }
  const loadingKey = getActionLoadingKey({ ...action, actionType: 'COMMAND', objectCode, recordId }, row)
  if (loadingKey && actionLoadingKeys.value.has(loadingKey)) {
    window.$message?.info(resolveActionTextValue(action.loadingReason, row) || '操作正在执行，请稍候')
    return false
  }
  setActionLoading(loadingKey, true)
  try {
    const response = await executeBusinessAction({
      suiteCode: action.suiteCode || config.suiteCode || '',
      objectCode,
      recordId: recordId === undefined || recordId === null ? '' : String(recordId),
      actionCode,
      formData: formData || {},
      context: {
        routeQuery: { ...route.query },
        row,
      },
      idempotencyKey: config.idempotencyKey || '',
    })
    const result = response?.data || {}
    if (String(result.executeStatus || '').toUpperCase() === 'FAILED') {
      throw new Error(result.message || action.failureMessage || '动作执行失败')
    }
    const successMessage = action.successMessage || result.message || config.successMessage || '操作成功'
    if (successMessage)
      window.$message?.success(resolveActionText(successMessage, row))
    const successBehavior = action.successBehavior || config.successBehavior || 'refreshList'
    if (successBehavior && successBehavior !== 'none')
      handleConfiguredActionSuccess({ ...action, successBehavior })
    else if (!options.fromModal)
      await loadList()
    return true
  }
  catch (error) {
    const failureMessage = action.failureMessage || config.failureMessage || error?.message || '动作执行失败'
    window.$message?.error(resolveActionText(failureMessage, row))
    return false
  }
  finally {
    setActionLoading(loadingKey, false)
  }
}

function normalizeCommandActionRuntimeConfig(action = {}) {
  const config = parseActionConfig(action.actionConfig)
  return {
    ...config,
    formSchema: Array.isArray(config.formSchema) ? config.formSchema : [],
    steps: Array.isArray(config.steps) ? config.steps : [],
    successBehavior: action.successBehavior || config.successBehavior || 'refreshList',
    successMessage: action.successMessage || config.successMessage || '',
    failureMessage: action.failureMessage || config.failureMessage || '',
  }
}

function buildCommandActionInitialData(config = {}, row = {}) {
  const defaults = {}
  const defaultValues = config.defaultValues && typeof config.defaultValues === 'object' ? config.defaultValues : {}
  Object.entries(defaultValues).forEach(([key, value]) => {
    defaults[key] = typeof value === 'string' && value.startsWith('row.')
      ? resolveConditionValue(row, value.slice(4))
      : value
  })
  return defaults
}

function parseActionConfig(config) {
  if (!config)
    return {}
  if (typeof config === 'string') {
    try {
      return JSON.parse(config) || {}
    }
    catch {
      return {}
    }
  }
  return typeof config === 'object' ? { ...config } : {}
}

async function sendConfiguredApiRequest(requestInfo) {
  const { method, url, params, data, headers } = requestInfo
  if (method === 'postEncrypt')
    return postEncrypt(url, data, compactRequestOptions({ params, headers }))
  const requestConfig = {
    method,
    url,
    ...compactRequestOptions({ params, headers }),
  }
  if (method !== 'get' || Object.keys(data).length)
    requestConfig.data = data
  return request(requestConfig)
}

function compactRequestOptions(options = {}) {
  const result = {}
  Object.entries(options).forEach(([key, value]) => {
    if (value && typeof value === 'object' && !Array.isArray(value) && !Object.keys(value).length)
      return
    if (value === undefined || value === null)
      return
    result[key] = value
  })
  return result
}

function buildConfiguredApiRequest(action = {}, row = {}) {
  const config = normalizeConfiguredApiConfig(action)
  const urlTemplate = config.url
  let url = resolveActionText(urlTemplate, row)
  const method = normalizeConfiguredApiMethod(config.method)
  const params = {}
  const data = {}
  const headers = {}
  config.params.forEach((param) => {
    const name = String(param?.name || '').trim()
    if (!name)
      return
    const value = resolveActionParamRawValue(param, row)
    if (isEmptyApiParamValue(value))
      return
    const target = resolveApiParamTarget(param, method, urlTemplate)
    if (target === 'path') {
      url = replaceApiPathParam(url, name, value)
    }
    else if (target === 'header') {
      headers[name] = value
    }
    else if (target === 'query') {
      setApiObjectValue(params, name, value)
    }
    else {
      setApiObjectValue(data, name, value)
    }
  })
  return {
    method,
    url,
    params,
    data,
    headers,
    config,
  }
}

function normalizeConfiguredApiConfig(action = {}) {
  const actionConfig = action.actionConfig && typeof action.actionConfig === 'object' ? action.actionConfig : {}
  const parsed = parseConfiguredApiValue(
    actionConfig.apiConfigValue
    || actionConfig.api
    || actionConfig.request
    || action.apiConfigValue
    || action.api,
  )
  const params = Array.isArray(actionConfig.params) && actionConfig.params.length
    ? actionConfig.params
    : Array.isArray(actionConfig.paramMappings) && actionConfig.paramMappings.length
      ? actionConfig.paramMappings
      : Array.isArray(action.params)
        ? action.params
        : []
  return {
    ...actionConfig,
    method: normalizeConfiguredApiMethod(
      actionConfig.method
      || actionConfig.reqMethod
      || actionConfig.apiMethod
      || action.method
      || parsed.method
      || 'post',
    ),
    url: String(
      actionConfig.url
      || actionConfig.apiUrl
      || actionConfig.urlPath
      || actionConfig.path
      || action.url
      || action.apiUrl
      || parsed.url
      || action.routePath
      || '',
    ).trim(),
    capabilityCode: String(actionConfig.capabilityCode || action.capabilityCode || '').trim(),
    params: params.map(normalizeConfiguredApiParam).filter(Boolean),
  }
}

function parseConfiguredApiValue(value) {
  const text = String(value || '').trim()
  if (!text)
    return {}
  const parts = text.split('@')
  if (parts.length > 1 && /^[A-Z_]+$/i.test(parts[0]))
    return { method: normalizeConfiguredApiMethod(parts[0]), url: parts.slice(1).join('@') }
  return { url: text }
}

function normalizeConfiguredApiMethod(value) {
  const method = String(value || 'post')
    .replace('-', '_')
    .toLowerCase()
  if (['postencrypt', 'post_encrypt'].includes(method))
    return 'postEncrypt'
  if (['get', 'post', 'put', 'delete', 'patch'].includes(method))
    return method
  return 'post'
}

function normalizeConfiguredApiParam(param = {}) {
  if (!param || typeof param !== 'object')
    return null
  const sourceType = ['rowField', 'routeQuery', 'static', 'system'].includes(param.sourceType) ? param.sourceType : 'static'
  const target = ['path', 'query', 'body', 'header'].includes(param.target) ? param.target : ''
  return {
    name: String(param.name || '').trim(),
    target,
    sourceType,
    sourceField: String(param.sourceField || '').trim(),
    value: param.value,
  }
}

function resolveApiParamTarget(param = {}, method = 'post', urlTemplate = '') {
  if (param.target)
    return param.target
  const name = String(param.name || '').trim()
  if (name && (String(urlTemplate).includes(`:${name}`) || String(urlTemplate).includes(`{${name}}`)))
    return 'path'
  return method === 'get' ? 'query' : 'body'
}

function resolveActionParamRawValue(param = {}, row = {}) {
  const sourceType = param.sourceType || 'static'
  const sourceField = String(param.sourceField || '').trim()
  if (sourceType === 'rowField' && sourceField)
    return resolveObjectPathValue(row, sourceField)
  if (sourceType === 'routeQuery' && sourceField)
    return route.query?.[sourceField] ?? ''
  if (sourceType === 'system' && sourceField)
    return resolveSystemParamValue(sourceField)
  return resolveActionText(param.value, row)
}

function resolveObjectPathValue(source = {}, path = '') {
  const directValue = source?.[path]
  if (directValue !== undefined)
    return directValue
  return String(path || '').split('.').filter(Boolean).reduce((value, key) => value?.[key], source)
}

function replaceApiPathParam(url, name, value) {
  const encoded = encodeURIComponent(Array.isArray(value) ? value.join(',') : String(value))
  return String(url || '')
    .replaceAll(`:${name}`, encoded)
    .replaceAll(`{${name}}`, encoded)
}

function setApiObjectValue(target, name, value) {
  const keys = String(name || '').split('.').filter(Boolean)
  if (keys.length <= 1) {
    target[name] = value
    return
  }
  let cursor = target
  keys.forEach((key, index) => {
    if (index === keys.length - 1) {
      cursor[key] = value
      return
    }
    if (!cursor[key] || typeof cursor[key] !== 'object' || Array.isArray(cursor[key]))
      cursor[key] = {}
    cursor = cursor[key]
  })
}

function isEmptyApiParamValue(value) {
  if (value === null || value === undefined)
    return true
  if (Array.isArray(value))
    return value.length === 0
  return typeof value === 'string' && value === ''
}

function isFalseLike(value) {
  return value === false || value === 0 || String(value).toLowerCase() === 'false'
}

function confirmConfiguredAction(message, options = {}) {
  if (!window.$dialog?.warning) {
    const nativeConfirm = globalThis?.confirm
    return Promise.resolve(typeof nativeConfirm !== 'function' || nativeConfirm(message))
  }
  return new Promise((resolve) => {
    window.$dialog.warning({
      title: options.title || '确认操作',
      content: message,
      positiveText: options.positiveText || '确定',
      negativeText: '取消',
      onPositiveClick: () => resolve(true),
      onNegativeClick: () => resolve(false),
      onClose: () => resolve(false),
      onMaskClick: () => resolve(false),
    })
  })
}

function getActionLoadingKey(action, row) {
  const actionType = String(action?.actionType || action?.key || '').toUpperCase()
  const actionKey = action?.key || action?.actionCode || action?.label || ''
  const objectCode = resolveRuntimeObjectCode(action, row)
  const recordId = action?.recordId || resolveRowKeyValue(row) || ''
  return `${actionType}:${actionKey}:${objectCode}:${recordId}`
}

function isActionLoading(action, row) {
  const internalLoading = actionLoadingKeys.value.has(getActionLoadingKey(action, row))
  if (typeof action?.loading === 'function')
    return internalLoading || !!action.loading(row)
  return internalLoading || !!action?.loading
}

function setActionLoading(key, loading) {
  if (!key)
    return
  const next = new Set(actionLoadingKeys.value)
  if (loading)
    next.add(key)
  else
    next.delete(key)
  actionLoadingKeys.value = next
}

function buildActionTarget(action, row) {
  let target = resolveActionText(action.routePath, row)
  const params = Array.isArray(action.params) ? action.params : []
  const query = new URLSearchParams()
  if (action.targetFormKey)
    query.set('formKey', action.targetFormKey)
  params.forEach((param) => {
    const name = String(param?.name || '').trim()
    if (!name)
      return
    const value = resolveActionParamValue(param, row)
    if (value !== '')
      query.append(name, value)
  })
  const queryString = query.toString()
  if (!queryString)
    return target
  target += target.includes('?') ? '&' : '?'
  return `${target}${queryString}`
}

function resolveActionParamValue(param = {}, row = {}) {
  const sourceType = param.sourceType || 'static'
  const sourceField = String(param.sourceField || '').trim()
  if (sourceType === 'rowField' && sourceField)
    return row?.[sourceField] ?? ''
  if (sourceType === 'routeQuery' && sourceField)
    return route.query?.[sourceField] ?? ''
  if (sourceType === 'system' && sourceField)
    return resolveSystemParamValue(sourceField)
  return resolveActionText(param.value, row)
}

function resolveSystemParamValue(sourceField = '') {
  if (sourceField === 'now')
    return new Date().toISOString()
  if (sourceField === 'today')
    return new Date().toISOString().slice(0, 10)
  if (sourceField === 'userId')
    return userStore.userId || ''
  if (sourceField === 'tenantId')
    return userStore.userInfo?.tenantId || route.query?.tenantId || ''
  if (sourceField === 'selectedIds')
    return [...selectedKeys.value]
  return ''
}

function resolveActionText(template, row) {
  let text = String(template || '')
  const data = row || {}
  Object.keys(data).forEach((key) => {
    const value = data[key]
    if (!isUsableKeyValue(value))
      return
    text = text.replaceAll(`:${key}`, value)
    text = text.replaceAll(`\${${key}}`, value)
  })
  const idValue = resolveRowKeyValue(data)
  if (isUsableKeyValue(idValue))
    text = text.replaceAll(':id', idValue)
  Object.keys(route.query || {}).forEach((key) => {
    const value = route.query[key]
    if (!isUsableKeyValue(value))
      return
    text = text.replaceAll(resolveTemplatePlaceholder(`route.${key}`), Array.isArray(value) ? value[0] : value)
  })
  text = text
    .replaceAll(resolveTemplatePlaceholder('system.now'), new Date().toISOString())
    .replaceAll(resolveTemplatePlaceholder('system.today'), new Date().toISOString().slice(0, 10))
  return text
}

function resolveTemplatePlaceholder(name = '') {
  return ['$', '{', name, '}'].join('')
}

function resolveButtonType(action) {
  return action?.type && action.type !== 'default' ? action.type : undefined
}

/**
 * ==================== 计算属性 ====================
 */

/**
 * 行键函数
 */
const rowKeyFn = computed(() => {
  if (typeof props.rowKey === 'function') {
    return props.rowKey
  }
  return row => row[props.rowKey]
})

function isUsableKeyValue(value) {
  if (value === null || value === undefined)
    return false
  const textValue = String(value).trim()
  return textValue !== '' && textValue !== 'undefined' && textValue !== 'null'
}

function readBoolean(value, defaultValue = false) {
  if (value === null || value === undefined)
    return defaultValue
  if (typeof value === 'boolean')
    return value
  if (typeof value === 'number')
    return value !== 0
  return ['true', '1', 'yes'].includes(String(value).trim().toLowerCase())
}

function resolveRowKeyValue(row) {
  if (!row || typeof row !== 'object')
    return undefined

  const values = []
  try {
    values.push(rowKeyFn.value(row))
  }
  catch (error) {
    console.warn('[AiCrudPage] rowKey解析失败:', error)
  }

  if (typeof props.rowKey === 'string')
    values.push(row[props.rowKey])
  values.push(row.id, row.Id)

  return values.find(isUsableKeyValue)
}

function mergeHookRowWithOriginal(row, processedRow) {
  if (!processedRow || typeof processedRow !== 'object')
    return row
  const merged = row && typeof row === 'object'
    ? { ...row, ...processedRow }
    : { ...processedRow }
  const originalKey = resolveRowKeyValue(row)
  if (isUsableKeyValue(originalKey) && !isUsableKeyValue(resolveRowKeyValue(merged))) {
    const key = typeof props.rowKey === 'string' ? props.rowKey : 'id'
    merged[key] = originalKey
    if (!isUsableKeyValue(merged.id))
      merged.id = originalKey
  }
  return merged
}

function resolveFormDefaultValues() {
  return isPlainRecord(props.formDefaultValues) ? props.formDefaultValues : {}
}

function resolveSubmitDefaultParams() {
  return isPlainRecord(props.submitDefaultParams) ? props.submitDefaultParams : {}
}

const resolvedCustomQueryConfigKey = computed(() => {
  if (props.customQueryConfigKey) {
    return props.customQueryConfigKey
  }
  const listApi = props.apiConfig?.list || ''
  const match = listApi.match(/\/ai\/crud\/([^/]+)\/page/)
  return match?.[1] || ''
})

const resolvedExportTaskConfigKey = computed(() => {
  if (props.exportTaskConfigKey) {
    return props.exportTaskConfigKey
  }
  const exportApi = props.apiConfig?.export || props.exportApi || ''
  const exportUrl = extractApiUrl(exportApi)
  const match = exportUrl.match(/\/ai\/crud\/([^/]+)\/export(?:$|\?)/)
  return match?.[1] || ''
})

const showExportTaskEntry = computed(() => {
  return props.showExport && props.showExportTasks && !!resolvedExportTaskConfigKey.value
})

const activeExportTask = computed(() => {
  if (!activeExportTaskId.value) {
    return null
  }
  return exportTasks.value.find(task => String(task.id) === String(activeExportTaskId.value)) || null
})

const exportTaskPaginationConfig = computed(() => ({
  page: exportTaskPagination.value.page,
  pageSize: exportTaskPagination.value.pageSize,
  itemCount: exportTaskPagination.value.itemCount,
  pageSizes: [10, 20, 50],
  showSizePicker: true,
  prefix: ({ itemCount }) => `共${itemCount}个任务`,
  onUpdatePage: handleExportTaskPageChange,
  onUpdatePageSize: handleExportTaskPageSizeChange,
}))

const exportTaskColumns = computed(() => [
  {
    title: '文件',
    key: 'fileName',
    minWidth: 180,
    ellipsis: { tooltip: true },
    render: row => row.fileName || '-',
  },
  {
    title: '状态',
    key: 'status',
    width: 96,
    render: row => h(NTag, {
      size: 'small',
      type: resolveExportTaskTagType(row.status),
    }, { default: () => resolveExportTaskStatusText(row.status) }),
  },
  {
    title: '进度',
    key: 'progress',
    width: 150,
    render: row => h(NProgress, {
      type: 'line',
      percentage: row.progress || 0,
      processing: isExportTaskRunning(row),
      indicatorPlacement: 'inside',
      height: 10,
      status: row.status === 'FAILED' ? 'error' : undefined,
    }),
  },
  {
    title: '数据量',
    key: 'totalCount',
    width: 120,
    render: row => `${row.exportedCount || 0}/${row.totalCount || 0}`,
  },
  {
    title: '创建时间',
    key: 'createTime',
    width: 160,
    render: row => row.createTime || '-',
  },
  {
    title: '操作',
    key: 'actions',
    width: 88,
    fixed: 'right',
    render: row => h(NButton, {
      text: true,
      type: 'primary',
      size: 'small',
      disabled: row.status !== 'SUCCESS' || !row.fileId,
      onClick: () => handleDownloadExportTask(row),
    }, { default: () => '下载' }),
  },
])

function getColumnKey(col) {
  return String(col?.prop || col?.key || col?.dataIndex || '').trim()
}

function isActionColumnConfig(col) {
  const key = getColumnKey(col)
  const title = String(col?.label || col?.title || '').trim()
  return ['action', 'actions', 'operation', 'operations'].includes(key) || title === '操作'
}

function shouldDefaultActionFixedRight(col) {
  return col.fixed === undefined || col.fixed === null || col.fixed === ''
}

const activeSourceColumns = computed(() => {
  if (!customQueryFields.value.length) {
    return props.columns
  }
  const actionColumns = props.columns.filter(isActionColumnConfig)
  const orderedColumns = customQueryFields.value
    .map(field => props.columns.find(col => getColumnKey(col) === field))
    .filter(Boolean)
  return [...orderedColumns, ...actionColumns]
})

/**
 * 表单上下文（传递 modalStatus 等信息）
 */
const formContext = computed(() => {
  return {
    modalStatus: modalStatus.value, // 'add' | 'edit' | 'detail'
    isEdit: modalStatus.value === 'edit',
    isAdd: modalStatus.value === 'add',
    isDetail: modalStatus.value === 'detail',
    currentRow: currentRow.value,
    formAssets: props.formAssets,
  }
})

const normalizedExpandConfig = computed(() => normalizeExpandConfig(props.expandConfig, props.childrenConfig))
const hasExpandConfig = computed(() => normalizedExpandConfig.value.enabled && normalizedExpandConfig.value.panels.length > 0)
const resolvedResizable = computed(() => props.resizable === true || props.tableProps?.resizable === true)

/**
 * 表格列配置（添加操作列）
 */
const tableColumns = computed(() => {
  const cols = []

  if (hasExpandConfig.value) {
    cols.push({
      type: 'expand',
      key: '__expand',
      width: 48,
      fixed: 'left',
      expandable: row => shouldExpandRow(normalizedExpandConfig.value.rowExpandable, row, formContext.value),
      renderExpand: row => h(AiCrudRowExpand, {
        config: normalizedExpandConfig.value,
        row,
        rowKeyValue: resolveRowKeyValue(row),
        context: formContext.value,
      }, collectExpandSlots()),
    })
  }

  // 判断是否是操作列（兼容 action / actions / operation 等写法）
  const isActionCol = (col) => {
    return isActionColumnConfig(col)
  }

  activeSourceColumns.value.forEach((col) => {
    if (isActionCol(col) && col.actions) {
      const actionCol = { ...col }
      const actions = normalizeRowActions(col.actions)
      if (shouldDefaultActionFixedRight(actionCol)) {
        actionCol.fixed = 'right'
      }
      delete actionCol.actions
      delete actionCol._slot
      delete actionCol.slot
      actionCol.render = row => renderActionColumn(row, actions, col.maxActionButtons)
      cols.push(actionCol)
      return
    }
    if (isActionCol(col) && col.render) {
      cols.push({
        ...col,
        fixed: shouldDefaultActionFixedRight(col) ? 'right' : col.fixed,
      })
      return
    }
    if (isActionCol(col) && !col.actions && !col.render) {
      return
    }
    const resolvedCol = resolveColumnRender(col)
    cols.push(resolvedCol)
  })

  // 如果没有操作列且没有隐藏，添加默认操作列
  const hasActionColumn = cols.some(isActionCol)

  if (!hasActionColumn) {
    cols.push({
      prop: 'action',
      label: '操作',
      width: 200,
      fixed: 'right',
      render: (row) => {
        const actions = normalizeRowActions([
          { label: '编辑', key: 'edit', type: 'primary' },
          { label: '删除', key: 'delete', type: 'error' },
        ])
        return renderActionColumn(row, actions)
      },
    })
  }

  return cols
})

function collectExpandSlots() {
  return Object.keys(slots).reduce((result, name) => {
    if (name.startsWith('expand-'))
      result[name] = slotProps => slots[name]?.(slotProps)
    return result
  }, {})
}

function normalizeRowActions(actions = []) {
  const next = Array.isArray(actions) ? [...actions] : []
  if (!props.enableTreeAddChild)
    return next
  if (next.some(action => action?.key === 'addChild'))
    return next
  const editIndex = next.findIndex(action => action?.key === 'edit')
  const addChildAction = { label: '添加下级', key: 'addChild', type: 'success' }
  if (editIndex >= 0) {
    next.splice(editIndex + 1, 0, addChildAction)
    return next
  }
  next.unshift(addChildAction)
  return next
}

function resolveColumnRender(col) {
  const nextCol = { ...col }
  if (!col.render || typeof col.render === 'function') {
    return nextCol
  }
  if (typeof col.render !== 'object') {
    return nextCol
  }
  const key = col.prop || col.key || col.dataIndex
  const renderType = col.render.type
  if (renderType === 'dictTag') {
    nextCol.render = row => h(DictTag, {
      dictType: col.render.dictType,
      value: row[key],
      size: 'small',
    })
  }
  else if (renderType === 'relationName') {
    const targetField = col.render.targetField || `${key}Name`
    nextCol.render = row => row[targetField] ?? row[key] ?? '-'
  }
  else if (renderType === 'orgName' || renderType === 'userName' || renderType === 'regionName') {
    const targetField = col.render.targetField || `${key}Name`
    nextCol.render = row => row[targetField] ?? row[key] ?? '-'
  }
  else if (renderType === 'imageUpload') {
    nextCol.render = (row) => {
      const value = row[key]
      if (!value)
        return '-'
      const fileIds = String(value).split(',').filter(Boolean)
      if (fileIds.length === 0)
        return '-'
      return h('div', { style: 'display: flex; gap: 4px; flex-wrap: wrap;' }, fileIds.map(fileId => h(AuthImage, { fileId, style: 'width: 32px; height: 32px; border-radius: 4px; object-fit: cover;' })))
    }
  }
  else if (renderType === 'fileUpload') {
    nextCol.render = (row) => {
      const value = row[key]
      if (!value)
        return '-'
      const nameField = col.render.targetField || `${key}Name`
      const name = row[nameField]
      if (name)
        return name
      return `${String(value).split(',').filter(Boolean).length} 个文件`
    }
  }
  return nextCol
}

/**
 * 分页配置
 */
const paginationConfig = computed(() => {
  if (!props.showPagination) {
    return false
  }

  return {
    page: pagination.value.page,
    pageSize: pagination.value.pageSize,
    itemCount: pagination.value.itemCount,
    pageCount: Math.ceil(pagination.value.itemCount / pagination.value.pageSize),
    showSizePicker: true,
    pageSizes: props.pageSizes,
    showQuickJumper: true,
    prefix: ({ itemCount }) => `共${itemCount}条`,
  }
})

/**
 * 搜索表单插槽名称
 */
const searchSlots = computed(() => {
  return props.searchSchema
    .filter(field => field.type === 'slot')
    .map(field => field.slotName || field.field)
})

/**
 * 表格插槽名称
 */
const tableSlots = computed(() => {
  return props.columns
    .filter(col => (col.slot || col._slot) && !col.actions)
    .map(col => col.slot || col._slot)
})

/**
 * 表单插槽名称
 */
const formSlots = computed(() => {
  return props.editSchema
    .filter(field => field.type === 'slot')
    .map(field => field.slotName || field.field)
})

const isDetailMode = computed(() => modalStatus.value === 'detail')

const resolvedFormOpenMode = computed(() => {
  const configured = String(props.formOpenMode || '').trim()
  if (configured === 'tabWorkspace' || configured.toLowerCase() === 'tabworkspace')
    return 'tabWorkspace'
  const normalized = configured.toLowerCase()
  if (['modal', 'drawer', 'flat'].includes(normalized))
    return normalized
  return String(props.modalType || '').trim().toLowerCase() === 'drawer' ? 'drawer' : 'modal'
})

const usesInlineFormWorkspace = computed(() => ['flat', 'tabWorkspace'].includes(resolvedFormOpenMode.value))
const isTabWorkspaceMode = computed(() => resolvedFormOpenMode.value === 'tabWorkspace')
const inlineWorkspaceVisible = computed(() => usesInlineFormWorkspace.value && inlineFormTabs.value.length > 0)
const showInlineListPane = computed(() => {
  return !inlineWorkspaceVisible.value
    || (isTabWorkspaceMode.value && activeInlineWorkspaceKey.value === INLINE_WORKSPACE_LIST_KEY)
})
const showInlineFormWorkspacePane = computed(() => {
  return inlineWorkspaceVisible.value
    && (!isTabWorkspaceMode.value || activeInlineWorkspaceKey.value !== INLINE_WORKSPACE_LIST_KEY)
})
const activeInlineFormTab = computed(() => inlineFormTabs.value.find(tab => tab.key === activeInlineFormTabKey.value) || null)
const activeInlineFormTitle = computed(() => activeInlineFormTab.value?.title || modalTitle.value || '编辑')
const inlineFormModeLabel = computed(() => {
  if (modalStatus.value === 'add')
    return '新增'
  if (modalStatus.value === 'edit')
    return '编辑'
  if (modalStatus.value === 'detail')
    return '详情'
  return '表单'
})

const resolvedTabWorkspace = computed(() => {
  const config = props.tabWorkspace || {}
  return {
    maxTabs: Math.max(1, Number(config.maxTabs || 8)),
    reuseRecordTab: config.reuseRecordTab !== false,
    closeAfterSave: config.closeAfterSave === true,
    showDirtyMark: config.showDirtyMark !== false,
  }
})

const activeModalWidth = computed(() => {
  if (isDetailMode.value)
    return props.detailModalWidth
  return props.modalWidth
})

const detailFlowTimelineVisible = computed(() => {
  return readBoolean(detailRuntime.value?.detailFlowTimelineVisible, true)
})

const detailFlowDiagramVisible = computed(() => {
  return readBoolean(detailRuntime.value?.detailFlowDiagramVisible, true)
})

const showDetailFlowTabs = computed(() => {
  if (!isDetailMode.value)
    return false
  if (!detailRuntime.value || detailRuntime.value.documentEnabled !== true)
    return false
  if (!detailRuntime.value.processInstanceId && detailRuntime.value.nextAction === 'CONFIG_FLOW')
    return false
  return detailFlowTimelineVisible.value || detailFlowDiagramVisible.value
})

const visibleChildrenConfig = computed(() => {
  const status = modalStatus.value || 'add'
  return (props.childrenConfig || []).filter((child) => {
    if (!child?.fields?.length)
      return false
    if (status === 'add')
      return child.showInCreate !== false
    if (status === 'edit')
      return child.showInEdit !== false
    if (status === 'detail')
      return child.showInDetail !== false
    return true
  })
})

const hasChildrenConfig = computed(() => visibleChildrenConfig.value.some(child => child?.fields?.length))
const showDefaultDetailContent = computed(() => {
  return !isDetailMode.value || !props.hideDefaultDetailContent
})

const showDefaultDetailChildren = computed(() => {
  return hasChildrenConfig.value && showDefaultDetailContent.value
})
const normalizedDetailPanelConfig = computed(() => normalizeExpandConfig({
  enabled: isDetailMode.value && Array.isArray(props.detailPanels) && props.detailPanels.length > 0,
  lazy: true,
  cache: false,
  layout: {
    mode: props.detailPanels?.length > 1 ? 'tabs' : 'single',
    density: 'compact',
    padding: 12,
  },
  panels: props.detailPanels || [],
}, props.childrenConfig))
const showDetailPanels = computed(() => isDetailMode.value && normalizedDetailPanelConfig.value.enabled && normalizedDetailPanelConfig.value.panels.length > 0)
const detailPanelRowKeyValue = computed(() => {
  const value = resolveRowKeyValue(formData.value)
  if (isUsableKeyValue(value))
    return value
  return resolveRowKeyValue(currentRow.value) || `detail_${modalStatus.value || 'current'}`
})
const hasSearchSchema = computed(() => !props.formOnly && props.showSearch && props.searchSchema.length > 0)
const normalizedSearchSchema = computed(() => {
  return (props.searchSchema || []).map((field) => {
    const propsData = { ...(field.props || {}) }
    delete propsData.showCount
    delete propsData.showWordLimit
    return {
      ...field,
      props: propsData,
      showCount: false,
      showWordLimit: false,
    }
  })
})

const modalFormSchema = computed(() => {
  if (!isDetailMode.value)
    return props.editSchema
  return props.editSchema.map(toReadonlyField)
})

const runtimeFormulaFields = computed(() => {
  return flattenRuntimeFormFields(props.editSchema)
    .map(field => normalizeRuntimeFormulaField(field))
    .filter(Boolean)
})

const runtimeFormFieldMap = computed(() => {
  const result = new Map()
  flattenRuntimeFormFields(props.editSchema).forEach((field) => {
    if (field?.field)
      result.set(field.field, field)
  })
  return result
})

const runtimeFormulaCalculationEnabled = computed(() => {
  if (isDetailMode.value || formOnlySubmitted.value)
    return false
  return props.formOnly || modalVisible.value || inlineWorkspaceVisible.value
})

const runtimeFormulaSignature = computed(() => {
  if (!runtimeFormulaCalculationEnabled.value || !runtimeFormulaFields.value.length)
    return ''
  return JSON.stringify(runtimeFormulaFields.value.map((field) => {
    const dependsOn = getRuntimeFormulaDependsOn(field.config, field.field)
    return {
      field: field.field,
      expression: field.config.expression || '',
      condition: field.config.condition || null,
      values: dependsOn.map(dep => [dep, formData.value?.[dep]]),
    }
  }))
})

function toReadonlyField(field) {
  if (!field || field.type === 'divider')
    return field
  if (field.nodeType && field.nodeType !== 'field') {
    return {
      ...field,
      children: Array.isArray(field.children) ? field.children.map(toReadonlyField) : field.children,
    }
  }
  return {
    ...field,
    disabled: true,
    readonly: true,
    props: {
      ...(field.props || {}),
      disabled: true,
      readonly: true,
    },
  }
}

function flattenRuntimeFormFields(nodes = []) {
  const result = []
  const walk = (items = []) => {
    ;(Array.isArray(items) ? items : []).forEach((node) => {
      if (!node || typeof node !== 'object')
        return
      if (node.nodeType && node.nodeType !== 'field') {
        walk(node.children || [])
        return
      }
      if (node.field)
        result.push(node)
    })
  }
  walk(nodes)
  return result
}

function normalizeRuntimeFormulaField(field) {
  if (!field?.field)
    return null
  const config = normalizeRuntimeFormulaConfig(field.formulaConfig)
  if (!config)
    return null
  const type = String(config.type || 'CALC').toUpperCase()
  if (type === 'AGGREGATE')
    return null
  if (type === 'CONDITIONAL') {
    const expression = config.condition?.expression || config.expression
    if (!expression)
      return null
    return {
      field: field.field,
      config: {
        ...config,
        type,
        expression,
        condition: {
          ...(config.condition || {}),
          expression,
        },
      },
    }
  }
  if (!config.expression)
    return null
  return {
    field: field.field,
    config: {
      ...config,
      type,
    },
  }
}

function normalizeRuntimeFormulaConfig(raw) {
  if (!raw)
    return null
  if (typeof raw === 'string') {
    try {
      return JSON.parse(raw)
    }
    catch {
      return null
    }
  }
  if (typeof raw !== 'object')
    return null
  return { ...raw }
}

function getRuntimeFormulaDependsOn(config = {}, targetField = '') {
  const result = []
  appendRuntimeDependsOn(result, config.dependsOn)
  appendRuntimeExpressionVariables(result, config.expression)
  if (String(config.type || '').toUpperCase() === 'CONDITIONAL')
    appendRuntimeExpressionVariables(result, config.condition?.expression)
  return result.filter(field => field !== targetField)
}

function appendRuntimeDependsOn(result, dependsOn) {
  ;(Array.isArray(dependsOn) ? dependsOn : []).forEach(value => appendRuntimeFormulaVariable(result, value))
}

function appendRuntimeExpressionVariables(result, expression) {
  extractRuntimeFormulaVariables(expression).forEach(value => appendRuntimeFormulaVariable(result, value))
}

function appendRuntimeFormulaVariable(result, value) {
  const field = String(value || '').trim()
  if (field && !result.includes(field))
    result.push(field)
}

function extractRuntimeFormulaVariables(expression) {
  const text = stripRuntimeFormulaStringLiterals(expression)
  if (!text)
    return []
  const variables = []
  const pattern = /[a-z_]\w*/gi
  let match = pattern.exec(text)
  while (match) {
    const token = match[0]
    const previous = text[match.index - 1] || ''
    const nextIndex = match.index + token.length
    const nextText = text.slice(nextIndex).trimStart()
    if (!isRuntimeFormulaReservedToken(token) && previous !== '.' && !nextText.startsWith('(') && !variables.includes(token))
      variables.push(token)
    match = pattern.exec(text)
  }
  return variables
}

function stripRuntimeFormulaStringLiterals(expression) {
  return String(expression || '').replace(/'[^']*'|"[^"]*"/g, ' ')
}

function isRuntimeFormulaReservedToken(token) {
  return [
    'true',
    'false',
    'null',
    'nil',
    'and',
    'or',
    'not',
    'if',
    'else',
    'return',
    'math',
    'string',
    'seq',
    'date',
  ].includes(String(token || '').toLowerCase())
}

function scheduleRuntimeFormulaCalculation(formulaField, delay = 180) {
  if (!formulaField?.field || !runtimeFormulaCalculationEnabled.value)
    return
  const field = formulaField.field
  if (formulaRuntimeTimers.has(field))
    clearTimeout(formulaRuntimeTimers.get(field))
  formulaRuntimeTimers.set(field, setTimeout(() => {
    formulaRuntimeTimers.delete(field)
    calculateRuntimeFormula(formulaField)
  }, delay))
}

function clearRuntimeFormulaTimers() {
  formulaRuntimeTimers.forEach(timer => clearTimeout(timer))
  formulaRuntimeTimers.clear()
}

function refreshRuntimeFormulas(delay = 0) {
  if (!runtimeFormulaCalculationEnabled.value)
    return
  runtimeFormulaFields.value.forEach(field => scheduleRuntimeFormulaCalculation(field, delay))
}

async function calculateRuntimeFormula(formulaField) {
  if (!formulaField?.field || !runtimeFormulaCalculationEnabled.value)
    return
  const dependsOn = getRuntimeFormulaDependsOn(formulaField.config, formulaField.field)
  if (dependsOn.some(dep => isEmptyRuntimeFormulaValue(formData.value?.[dep]))) {
    patchRuntimeFormulaValue(formulaField.field, null)
    return
  }
  const sequence = (formulaRuntimeSequences.get(formulaField.field) || 0) + 1
  formulaRuntimeSequences.set(formulaField.field, sequence)
  try {
    const response = await previewFormula(buildRuntimeFormulaPreviewPayload(formulaField))
    if (!runtimeFormulaCalculationEnabled.value || formulaRuntimeSequences.get(formulaField.field) !== sequence)
      return
    const result = response?.data ?? response
    if (result?.success) {
      patchRuntimeFormulaValue(formulaField.field, result.result)
    }
    else if (result?.errorMessage) {
      console.warn(`[AiCrudPage] 公式计算失败: ${formulaField.field}`, result.errorMessage)
    }
  }
  catch (error) {
    if (runtimeFormulaCalculationEnabled.value && formulaRuntimeSequences.get(formulaField.field) === sequence)
      console.warn(`[AiCrudPage] 公式计算请求失败: ${formulaField.field}`, error)
  }
}

function buildRuntimeFormulaPreviewPayload(formulaField) {
  const config = formulaField.config || {}
  const type = String(config.type || 'CALC').toUpperCase()
  const payload = {
    expression: config.expression,
    type,
    dependsOn: getRuntimeFormulaDependsOn(config, formulaField.field),
    sampleValues: buildRuntimeFormulaSampleValues(formulaField),
  }
  if (type === 'CONDITIONAL') {
    const condition = config.condition || {}
    payload.condition = {
      expression: condition.expression || config.expression,
      trueValue: condition.trueValue ?? '',
      falseValue: condition.falseValue ?? '',
    }
    payload.expression = payload.condition.expression
  }
  return payload
}

function buildRuntimeFormulaSampleValues(formulaField) {
  const values = { ...(formData.value || {}) }
  getRuntimeFormulaDependsOn(formulaField.config, formulaField.field).forEach((field) => {
    values[field] = normalizeRuntimeFormulaSampleValue(
      values[field],
      runtimeFormFieldMap.value.get(field),
      formulaField.config,
      field,
    )
  })
  return values
}

function normalizeRuntimeFormulaSampleValue(value, field = {}, config = {}, fieldName = '') {
  if (isEmptyRuntimeFormulaValue(value))
    return value
  if (typeof value !== 'string')
    return value
  if (!isRuntimeNumericField(field) && !isRuntimeNumericFormulaOperand(config, fieldName))
    return value
  const text = value.trim()
  if (!/^-?\d+(?:\.\d+)?$/.test(text))
    return value
  const number = Number(text)
  return Number.isFinite(number) ? number : value
}

function isRuntimeNumericField(field = {}) {
  const type = String(field.type || field.componentType || '').toLowerCase()
  const dataType = String(field.dataType || field.fieldDataType || field.props?.dataType || '').toLowerCase()
  return ['number', 'inputnumber'].includes(type)
    || ['int', 'integer', 'bigint', 'decimal', 'double', 'float', 'number'].includes(dataType)
}

function isRuntimeNumericFormulaOperand(config = {}, fieldName = '') {
  if (!fieldName)
    return false
  if (isRuntimeNumericLikeFieldName(fieldName))
    return true
  if (isRuntimeArithmeticOperand(config.expression, fieldName))
    return true
  return String(config.type || '').toUpperCase() === 'CONDITIONAL'
    && isRuntimeArithmeticOperand(config.condition?.expression, fieldName)
}

function isRuntimeNumericLikeFieldName(fieldName = '') {
  const value = String(fieldName || '').toLowerCase()
  return [
    'qty',
    'quantity',
    'count',
    'num',
    'number',
    'price',
    'amount',
    'money',
    'fee',
    'cost',
    'total',
    'rate',
    'ratio',
    'percent',
    'discount',
    'tax',
    'score',
    'weight',
    'area',
    'volume',
  ].some(keyword => value.includes(keyword))
}

function isRuntimeArithmeticOperand(expression, fieldName = '') {
  const text = stripRuntimeFormulaStringLiterals(expression)
  if (!text || !fieldName)
    return false
  const tokenPattern = new RegExp(`(^|[^\\w.])${escapeRuntimeFormulaRegex(fieldName)}(?=$|[^\\w])`, 'g')
  const arithmeticOperators = new Set(['*', '/', '%', '-'])
  let match = tokenPattern.exec(text)
  while (match) {
    const tokenStart = match.index + match[1].length
    const tokenEnd = tokenStart + fieldName.length
    const previous = findRuntimeFormulaAdjacentOperator(text, tokenStart, -1)
    const next = findRuntimeFormulaAdjacentOperator(text, tokenEnd, 1)
    if (arithmeticOperators.has(previous) || arithmeticOperators.has(next))
      return true
    match = tokenPattern.exec(text)
  }
  return false
}

function findRuntimeFormulaAdjacentOperator(text = '', startIndex = 0, direction = 1) {
  let index = startIndex + (direction < 0 ? -1 : 0)
  while (index >= 0 && index < text.length) {
    const char = text[index]
    if (!/\s/.test(char))
      return char
    index += direction < 0 ? -1 : 1
  }
  return ''
}

function escapeRuntimeFormulaRegex(value = '') {
  return String(value).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

function patchRuntimeFormulaValue(field, value) {
  if (!field)
    return
  if (Object.is(formData.value?.[field], value))
    return
  formData.value = {
    ...(formData.value || {}),
    [field]: value,
  }
}

function isEmptyRuntimeFormulaValue(value) {
  if (Array.isArray(value))
    return value.length === 0
  return value === null || value === undefined || value === ''
}

watch(runtimeFormulaSignature, () => {
  if (!runtimeFormulaCalculationEnabled.value)
    return
  runtimeFormulaFields.value.forEach(scheduleRuntimeFormulaCalculation)
})

watch(formData, () => {
  persistActiveInlineFormTab({ dirty: true })
}, { deep: true })

watch(childFormData, () => {
  persistActiveInlineFormTab({ dirty: true })
}, { deep: true })

watch(detailActiveTab, () => {
  persistActiveInlineFormTab()
})

const resolvedFormOnlyTitle = computed(() => props.formOnlyTitle || props.addButtonText || '单据填报')

const resolvedEditFormClass = computed(() => [
  'ai-crud-edit-form',
  props.editFormClass,
].filter(Boolean))

/**
 * 计算横向滚动宽度
 * 如果没有设置 scrollX，自动计算所有列的宽度总和
 */
const DEFAULT_SELECTION_COLUMN_WIDTH = 48
const DEFAULT_EXPAND_COLUMN_WIDTH = 48
const DEFAULT_ACTION_COLUMN_WIDTH = 180
const DEFAULT_DATA_COLUMN_WIDTH = 120
const TABLE_SCROLL_X_BUFFER = 24

const computedScrollX = computed(() => {
  if (props.scrollX !== undefined) {
    return props.scrollX
  }

  if (!tableColumns.value.length) {
    return undefined
  }

  let totalWidth = props.hideSelection ? 0 : DEFAULT_SELECTION_COLUMN_WIDTH
  tableColumns.value.forEach((col) => {
    const width = resolveColumnWidth(col.width ?? col.minWidth)
    if (width > 0) {
      totalWidth += width
      return
    }
    if (col.type === 'expand') {
      totalWidth += DEFAULT_EXPAND_COLUMN_WIDTH
      return
    }
    if (isActionColumnConfig(col)) {
      totalWidth += DEFAULT_ACTION_COLUMN_WIDTH
      return
    }
    totalWidth += DEFAULT_DATA_COLUMN_WIDTH
  })

  return totalWidth > 0 ? totalWidth + TABLE_SCROLL_X_BUFFER : undefined
})

function resolveColumnWidth(value) {
  if (typeof value === 'number')
    return value
  if (typeof value !== 'string')
    return 0
  const matched = value.trim().match(/^(\d+(?:\.\d+)?)px?$/i)
  return matched ? Number(matched[1]) : 0
}

/**
 * 表格高度交给外层 flex 容器和 Naive flex-height 处理，避免页面级滚动条。
 */
const computedMaxHeight = computed(() => {
  if (props.maxHeight !== undefined) {
    return props.maxHeight
  }
  return undefined
})

/**
 * ==================== 方法 ====================
 */

/**
 * 执行钩子函数
 * @param {string} hookName - 钩子函数名
 * @param {*} params - 参数
 * @param {Function} success - 成功回调
 * @returns {Promise} 处理后的钩子结果
 */
async function callHook(hookName, params, success) {
  if (props[hookName] && typeof props[hookName] === 'function') {
    const result = props[hookName](params)

    // 如果是 Promise
    if (result instanceof Promise) {
      try {
        const data = await result
        return success ? success(data) : data
      }
      catch (error) {
        console.error(`Hook ${hookName} error:`, error)
        return success ? success(params) : params
      }
    }
    else {
      return success ? success(result) : result
    }
  }
  else {
    return success ? success(params) : params
  }
}

/**
 * 解析 API 配置
 * @param {string} key - API 配置键名
 * @param {string} defaultApi - 默认 API
 * @param {string} defaultMethod - 默认请求方法
 * @param {object} urlParams - URL 参数，用于替换 :id 等占位符
 * @returns {object} { method, url }
 */
function parseApiConfig(key, defaultApi, defaultMethod = 'get', urlParams = {}) {
  const apiConfigValue = props.apiConfig[key]
  const normalizedUrlParams = normalizeUrlParams(urlParams)

  if (apiConfigValue) {
    const [method, url] = apiConfigValue.split('@')
    const finalMethod = method === 'postEncrypt' ? method : method.toLowerCase()
    let finalUrl = url
    let hasPlaceholder = false
    Object.keys(normalizedUrlParams).forEach((paramKey) => {
      const paramValue = normalizedUrlParams[paramKey]
      if (!isUsableKeyValue(paramValue)) {
        return
      }
      if (finalUrl.includes(`:${paramKey}`)) {
        hasPlaceholder = true
        finalUrl = finalUrl.replaceAll(`:${paramKey}`, paramValue)
      }
      if (finalUrl.includes(`{${paramKey}}`)) {
        hasPlaceholder = true
        finalUrl = finalUrl.replaceAll(`{${paramKey}}`, paramValue)
      }
      if (paramKey === 'id' && /\/id(?=\/|$|\?)/.test(finalUrl)) {
        hasPlaceholder = true
        finalUrl = finalUrl.replace(/\/id(?=\/|$|\?)/g, `/${paramValue}`)
      }
    })
    if (!hasPlaceholder && finalMethod === 'get' && Object.keys(normalizedUrlParams).length > 0) {
      const paramValues = resolveUrlParamValues(normalizedUrlParams).join('/')
      if (paramValues) {
        finalUrl = `${finalUrl}/${paramValues}`
      }
    }
    return { method: finalMethod, url: finalUrl }
  }

  return { method: defaultMethod, url: defaultApi }
}

function extractApiUrl(apiConfigValue) {
  if (!apiConfigValue) {
    return ''
  }
  const text = String(apiConfigValue)
  const parts = text.split('@')
  return parts.length > 1 ? parts.slice(1).join('@') : text
}

function normalizeUrlParams(urlParams = {}) {
  const normalized = { ...(urlParams || {}) }
  const rowKey = typeof props.rowKey === 'string' ? props.rowKey : ''
  if (rowKey && isUsableKeyValue(normalized[rowKey]) && !isUsableKeyValue(normalized.id))
    normalized.id = normalized[rowKey]
  if (rowKey && isUsableKeyValue(normalized.id) && !isUsableKeyValue(normalized[rowKey]))
    normalized[rowKey] = normalized.id
  return normalized
}

function resolveUrlParamValues(urlParams = {}) {
  const rowKey = typeof props.rowKey === 'string' ? props.rowKey : ''
  if (rowKey && isUsableKeyValue(urlParams[rowKey]))
    return [urlParams[rowKey]]
  if (isUsableKeyValue(urlParams.id))
    return [urlParams.id]
  return Object.values(urlParams).filter(isUsableKeyValue)
}

function stableSerialize(value, seen = new WeakSet()) {
  if (value === undefined)
    return 'undefined'
  if (typeof value === 'function' || typeof value === 'symbol')
    return JSON.stringify(String(value))
  if (value === null || typeof value !== 'object')
    return JSON.stringify(value)
  if (seen.has(value))
    return '"[Circular]"'
  seen.add(value)
  if (Array.isArray(value)) {
    const result = `[${value.map(item => stableSerialize(item, seen)).join(',')}]`
    seen.delete(value)
    return result
  }
  const keys = Object.keys(value).sort()
  const result = `{${keys.map(key => `${JSON.stringify(key)}:${stableSerialize(value[key], seen)}`).join(',')}}`
  seen.delete(value)
  return result
}

/**
 * 加载列表数据
 */
async function loadList() {
  if (!customQueryPayload.value && !props.api && !props.apiConfig.list) {
    console.warn('未配置 API 地址')
    return
  }

  tableLoading.value = true

  try {
    // 构建请求参数
    let params = {
      ...searchParams.value,
      ...props.publicParams,
    }

    // 分页参数
    if (props.showPagination) {
      if (props.listMethod === 'get') {
        params = {
          ...params,
          ...props.publicQuery,
          pageNum: pagination.value.page,
          pageSize: pagination.value.pageSize,
        }
      }
      else {
        params.pageNum = pagination.value.page
        params.pageSize = pagination.value.pageSize
      }
    }

    // 调用 beforeLoadList 钩子
    params = await callHook('beforeLoadList', params, data => data)

    // 发送请求
    let response
    if (customQueryPayload.value && resolvedCustomQueryConfigKey.value) {
      response = await customQueryExecute(resolvedCustomQueryConfigKey.value, {
        ...resolveDefaultRequestSortParams(),
        ...customQueryPayload.value,
        pageNum: pagination.value.page,
        pageSize: pagination.value.pageSize,
      })
    }
    else {
      // 解析 API
      const { method, url } = parseApiConfig('list', props.api, props.listMethod)

      // 确定使用哪种请求方法
      let requestMethod = method
      // 如果方法明确指定为 postEncrypt，则使用加密请求，不管 isEncrypt 属性
      const useEncrypt = method === 'postEncrypt' || (props.isEncrypt && method !== 'get')
      if (useEncrypt) {
        requestMethod = method === 'postEncrypt' ? 'postEncrypt' : method.toLowerCase()
      }
      else {
        requestMethod = method.toLowerCase()
      }

      if (useEncrypt && requestMethod === 'postEncrypt') {
        // 使用加密请求
        response = await postEncrypt(url, params)
      }
      else {
        // 使用普通请求
        const requestConfig = {
          method: requestMethod,
          url,
        }

        if (requestMethod === 'get') {
          requestConfig.params = params
        }
        else {
          requestConfig.data = params
          requestConfig.params = props.publicQuery
        }

        response = await request(requestConfig)
      }
    }

    // 提取数据
    let list = []
    let total = 0

    if (Array.isArray(response.data)) {
      // 后端直接返回数组（不分页）
      list = response.data
      total = response.data.length
    }
    else if (response.data && typeof response.data === 'object') {
      // 后端返回对象（分页数据）
      list = response.data[props.listDataField] || []
      total = response.data[props.listTotalField] || 0
    }

    // 调用 beforeRenderList 钩子
    list = await callHook('beforeRenderList', list, data => data)

    // 更新数据
    dataSource.value = list
    pagination.value.itemCount = total

    emit('load-list-success', { list, total })
  }
  catch (error) {
    console.error('加载列表失败:', error)
    window.$message.error('加载数据失败')
    emit('load-list-error', error)
  }
  finally {
    tableLoading.value = false
  }
}

function resolveDefaultRequestSortParams() {
  const orderByColumn = props.publicParams?.orderByColumn
  const isAsc = props.publicParams?.isAsc
  if (!orderByColumn && !isAsc)
    return {}
  return {
    ...(orderByColumn ? { orderByColumn } : {}),
    ...(isAsc ? { isAsc } : {}),
  }
}

/**
 * 搜索
 */
async function handleSearch(params) {
  // 调用 beforeSearch 钩子
  const processedParams = await callHook('beforeSearch', params, data => data)

  // 如果钩子返回 false，中断搜索
  if (processedParams === false) {
    return
  }

  searchParams.value = { ...processedParams }
  pagination.value.page = 1
  loadList()
}

/**
 * 重置
 */
function handleReset() {
  searchParams.value = {}
  pagination.value.page = 1
  loadList()
}

/**
 * 刷新列表
 */
function handleRefresh() {
  loadList()
}

function handleSearchToggle(visible) {
  searchPanelVisible.value = visible
}

function handleApplyCustomQuery(payload) {
  customQueryPayload.value = payload
  customQueryFields.value = payload?.fields || []
  if (payload?.renderMode) {
    activeRenderMode.value = payload.renderMode
  }
  pagination.value.page = 1
  loadList()
}

function handleClearCustomQuery() {
  customQueryPayload.value = null
  customQueryFields.value = []
  activeRenderMode.value = props.renderMode || 'table'
  pagination.value.page = 1
  loadList()
}

/**
 * 列表/卡片渲染模式切换
 */
function handleRenderModeChange(mode) {
  activeRenderMode.value = mode
  emit('render-mode-change', mode)
}

/**
 * 翻页
 */
function handlePageChange(page) {
  pagination.value.page = page
  loadList()
}

/**
 * 改变每页条数
 */
function handlePageSizeChange(pageSize) {
  pagination.value.pageSize = pageSize
  pagination.value.page = 1
  loadList()
}

/**
 * 选中项变化
 */
watch(selectedKeys, (newKeys) => {
  const rows = tableRef.value?.getCheckedRows() || []
  emit('selection-change', { keys: newKeys, rows })
})

watch(
  () => props.renderMode,
  (mode) => {
    if (!customQueryPayload.value) {
      activeRenderMode.value = mode || 'table'
    }
  },
)

watch(
  () => props.showSearch,
  (visible) => {
    searchPanelVisible.value = visible !== false
  },
)

watch(
  () => props.formOnly,
  (value, oldValue) => {
    if (value && !oldValue) {
      handleAdd()
      return
    }
    if (!value && oldValue && !props.lazy)
      loadList()
  },
)

/**
 * 统一规范化编辑表单回填数据
 * - number/inputNumber：字符串 → 数字
 * - select/radio/checkbox：数字 → 字符串（匹配字典选项的 string value）
 */
function normalizeEditData(data) {
  if (!data || typeof data !== 'object')
    return data
  const result = {}
  for (const [key, value] of Object.entries(data)) {
    const fieldConfig = props.editSchema.find(f => f.field === key)
    if (!fieldConfig) {
      result[key] = value
      continue
    }
    if (['number', 'inputNumber'].includes(fieldConfig.type)) {
      // 数字字段：字符串转数字
      if (typeof value === 'string') {
        result[key] = Number.parseFloat(value)
      }
      else if (value === null || value === undefined) {
        result[key] = 0
      }
      else {
        result[key] = value
      }
    }
    else if (['select', 'radio', 'checkbox'].includes(fieldConfig.type)) {
      // 字典选择字段：根据 options 的 value 类型或 valueType 配置决定类型转换
      const isNumberOption = fieldConfig.props?.options?.some?.(o => typeof o.value === 'number')
        || fieldConfig.valueType === 'number'
      if (isNumberOption) {
        // options 的 value 是数字类型，保留数字类型
        if (typeof value === 'string') {
          result[key] = Number.parseFloat(value)
        }
        else if (value === null || value === undefined) {
          result[key] = 0
        }
        else {
          result[key] = value
        }
      }
      else {
        // 默认：数字转字符串
        if (typeof value === 'number') {
          result[key] = String(value)
        }
        else {
          result[key] = value
        }
      }
    }
    else {
      result[key] = value
    }
  }
  return result
}

function isMasterDetailPayload(data) {
  return data && typeof data === 'object' && ('main' in data || 'children' in data)
}

function resolveChildKey(child) {
  return child.key || child.modelCode || child.tableName || 'children'
}

function buildInitialChildrenData() {
  const result = {}
  visibleChildrenConfig.value.forEach((child) => {
    result[resolveChildKey(child)] = []
  })
  return result
}

function normalizeChildrenData(children) {
  const source = children && typeof children === 'object' ? children : {}
  const result = {}
  visibleChildrenConfig.value.forEach((child) => {
    const key = resolveChildKey(child)
    result[key] = Array.isArray(source[key]) ? source[key] : []
  })
  return result
}

function applyDetailData(data) {
  if (hasChildrenConfig.value && isMasterDetailPayload(data)) {
    formData.value = normalizeEditData({ ...resolveFormDefaultValues(), ...(data.main || {}) })
    childFormData.value = normalizeChildrenData(data.children)
    return
  }
  formData.value = normalizeEditData({ ...resolveFormDefaultValues(), ...(data || {}) })
  childFormData.value = buildInitialChildrenData()
}

function buildMasterDetailSubmitData(data) {
  if (!hasChildrenConfig.value) {
    return data
  }
  const payload = isMasterDetailPayload(data)
    ? {
        main: { ...(data.main || {}) },
        children: normalizeChildrenData(data.children),
      }
    : {
        main: { ...(data || {}) },
        children: childFormRef.value?.getValue?.() || childFormData.value || {},
      }
  if (modalStatus.value === 'edit') {
    const key = typeof props.rowKey === 'string' ? props.rowKey : 'id'
    const idValue = payload.main?.[key] ?? currentRow.value?.[key]
    if (idValue !== undefined && idValue !== null) {
      payload.main[key] = idValue
    }
  }
  return payload
}

function cloneInlineFormValue(value) {
  if (value === null || value === undefined || typeof value !== 'object')
    return value
  try {
    if (typeof structuredClone === 'function')
      return structuredClone(value)
  }
  catch {}
  try {
    return JSON.parse(JSON.stringify(value))
  }
  catch {
    return Array.isArray(value) ? [...value] : { ...value }
  }
}

function buildInlineRecordTabKey(status, row) {
  const idValue = resolveRowKeyValue(row)
  if (!isUsableKeyValue(idValue))
    return ''
  return `${status}:${idValue}`
}

function findReusableInlineFormTab(status, row) {
  if (!usesInlineFormWorkspace.value || !isTabWorkspaceMode.value || status === 'add')
    return null
  if (!resolvedTabWorkspace.value.reuseRecordTab)
    return null
  const key = buildInlineRecordTabKey(status, row)
  if (!key)
    return null
  return inlineFormTabs.value.find(tab => tab.key === key) || null
}

function activateReusableInlineFormTab(status, row) {
  const tab = findReusableInlineFormTab(status, row)
  if (!tab)
    return false
  handleInlineFormTabChange(tab.key)
  return true
}

function buildInlineFormTabKey(status, row) {
  if (!isTabWorkspaceMode.value)
    return 'flat'
  if (status !== 'add') {
    const reusableKey = buildInlineRecordTabKey(status, row)
    if (reusableKey)
      return reusableKey
  }
  inlineFormTabSequence += 1
  return `${status || 'form'}:${inlineFormTabSequence}`
}

function openFormContainer(status, title, row = null, context = {}) {
  if (!usesInlineFormWorkspace.value || props.formOnly) {
    modalVisible.value = true
    return true
  }
  const existing = findReusableInlineFormTab(status, row)
  if (existing) {
    handleInlineFormTabChange(existing.key)
    return true
  }
  if (isTabWorkspaceMode.value && inlineFormTabs.value.length >= resolvedTabWorkspace.value.maxTabs) {
    window.$message.warning(`最多同时打开 ${resolvedTabWorkspace.value.maxTabs} 个录入页签`)
    return false
  }

  const tab = {
    key: buildInlineFormTabKey(status, row),
    status,
    title,
    row: cloneInlineFormValue(row),
    context: cloneInlineFormValue(context),
    formData: cloneInlineFormValue(formData.value || {}),
    childFormData: cloneInlineFormValue(childFormData.value || {}),
    detailRuntime: cloneInlineFormValue(detailRuntime.value),
    detailActiveTab: detailActiveTab.value,
    dirty: false,
  }
  if (isTabWorkspaceMode.value) {
    inlineFormTabs.value = [...inlineFormTabs.value, tab]
    activeInlineWorkspaceKey.value = tab.key
  }
  else {
    inlineFormTabs.value = [tab]
  }
  activeInlineFormTabKey.value = tab.key
  hydrateInlineFormTab(tab)
  return true
}

function hydrateInlineFormTab(tab) {
  if (!tab)
    return
  inlineFormHydrating.value = true
  modalStatus.value = tab.status || ''
  modalTitle.value = tab.title || ''
  currentRow.value = cloneInlineFormValue(tab.row)
  detailRuntime.value = cloneInlineFormValue(tab.detailRuntime)
  detailActiveTab.value = tab.detailActiveTab || 'business'
  formData.value = cloneInlineFormValue(tab.formData || {})
  childFormData.value = cloneInlineFormValue(tab.childFormData || {})
  nextTick(() => {
    formRef.value?.restoreValidation()
    inlineFormHydrating.value = false
  })
}

function persistActiveInlineFormTab({ dirty = false } = {}) {
  if (!usesInlineFormWorkspace.value || inlineFormHydrating.value)
    return
  const tab = activeInlineFormTab.value
  if (!tab)
    return
  tab.formData = cloneInlineFormValue(formData.value || {})
  tab.childFormData = cloneInlineFormValue(childFormData.value || {})
  tab.detailRuntime = cloneInlineFormValue(detailRuntime.value)
  tab.detailActiveTab = detailActiveTab.value
  if (dirty)
    tab.dirty = true
}

function markActiveInlineFormClean() {
  const tab = activeInlineFormTab.value
  if (!tab)
    return
  persistActiveInlineFormTab({ dirty: false })
  tab.dirty = false
}

function handleInlineFormTabChange(key) {
  if (!key)
    return
  persistActiveInlineFormTab()
  const tab = inlineFormTabs.value.find(item => item.key === key)
  if (!tab)
    return
  activeInlineWorkspaceKey.value = key
  if (key === activeInlineFormTabKey.value) {
    hydrateInlineFormTab(tab)
    return
  }
  activeInlineFormTabKey.value = key
  hydrateInlineFormTab(tab)
}

function handleInlineWorkspaceListTab() {
  persistActiveInlineFormTab()
  activeInlineWorkspaceKey.value = INLINE_WORKSPACE_LIST_KEY
}

function inlineFormTabTitle(tab) {
  return tab?.title || '表单'
}

function closeInlineFormTab(key = activeInlineFormTabKey.value, force = false) {
  const tab = inlineFormTabs.value.find(item => item.key === key)
  if (!tab)
    return
  if (!force && tab.dirty) {
    window.$dialog.warning({
      title: '关闭录入页签',
      content: '当前页签存在未保存变更，确定关闭吗？',
      positiveText: '关闭',
      negativeText: '取消',
      onPositiveClick: () => closeInlineFormTab(key, true),
    })
    return
  }

  const listPaneActive = isTabWorkspaceMode.value && activeInlineWorkspaceKey.value === INLINE_WORKSPACE_LIST_KEY
  const workspacePaneActive = activeInlineWorkspaceKey.value === key
  const index = inlineFormTabs.value.findIndex(item => item.key === key)
  inlineFormTabs.value = inlineFormTabs.value.filter(item => item.key !== key)
  if (activeInlineFormTabKey.value !== key) {
    if (workspacePaneActive)
      activeInlineWorkspaceKey.value = INLINE_WORKSPACE_LIST_KEY
    return
  }
  const nextTab = inlineFormTabs.value[Math.max(0, index - 1)] || inlineFormTabs.value[0]
  if (nextTab) {
    activeInlineFormTabKey.value = nextTab.key
    if (!listPaneActive)
      activeInlineWorkspaceKey.value = nextTab.key
    hydrateInlineFormTab(nextTab)
  }
  else {
    activeInlineFormTabKey.value = ''
    activeInlineWorkspaceKey.value = INLINE_WORKSPACE_LIST_KEY
    modalStatus.value = ''
    modalTitle.value = ''
    currentRow.value = null
    detailRuntime.value = null
    formData.value = {}
    childFormData.value = {}
    emit('modal-close')
  }
}

function handleCloseActiveInlineFormTab() {
  persistActiveInlineFormTab()
  closeInlineFormTab()
}

function handleInlineFormCancel() {
  persistActiveInlineFormTab()
  closeInlineFormTab()
}

function handleInlineFormSubmitSuccess(response, isEdit) {
  const tab = activeInlineFormTab.value
  if (!tab)
    return
  const previousKey = tab.key
  if (!isEdit) {
    const savedRecord = response?.data && typeof response.data === 'object' ? response.data : response
    const idValue = resolveRowKeyValue(savedRecord || {})
    if (isUsableKeyValue(idValue)) {
      tab.status = 'edit'
      tab.row = cloneInlineFormValue(savedRecord)
      tab.title = modalTitle.value === props.addButtonText ? '编辑' : tab.title
      tab.key = isTabWorkspaceMode.value ? `edit:${idValue}` : tab.key
      activeInlineFormTabKey.value = tab.key
      if (activeInlineWorkspaceKey.value === previousKey)
        activeInlineWorkspaceKey.value = tab.key
      modalStatus.value = 'edit'
      currentRow.value = cloneInlineFormValue(savedRecord)
    }
  }
  markActiveInlineFormClean()
  if (resolvedTabWorkspace.value.closeAfterSave)
    closeInlineFormTab(tab.key, true)
}

/**
 * 新增
 */
async function handleAdd(defaultValues = null, options = {}) {
  formOnlySubmitted.value = false
  const presetValues = isPlainRecord(defaultValues)
    ? defaultValues
    : null
  modalTitle.value = options.title || props.addButtonText
  modalStatus.value = 'add'
  currentRow.value = null
  detailRuntime.value = null
  detailActiveTab.value = 'business'

  // 初始化表单数据，设置默认值
  const initialData = {}
  props.editSchema.forEach((field) => {
    if (field.field) {
      initialData[field.field] = field.defaultValue ?? null
    }
  })

  // 调用 beforeRenderForm 钩子（新增时）
  const formDataFromHook = await callHook('beforeRenderForm', null, data => data)

  // 合并默认值和钩子返回的数据
  if (formDataFromHook && typeof formDataFromHook === 'object') {
    if (hasChildrenConfig.value && isMasterDetailPayload(formDataFromHook)) {
      formData.value = { ...initialData, ...resolveFormDefaultValues(), ...(formDataFromHook.main || {}), ...(presetValues || {}) }
      childFormData.value = normalizeChildrenData(formDataFromHook.children)
    }
    else {
      formData.value = { ...initialData, ...resolveFormDefaultValues(), ...formDataFromHook, ...(presetValues || {}) }
      childFormData.value = buildInitialChildrenData()
    }
  }
  else {
    formData.value = { ...initialData, ...resolveFormDefaultValues(), ...(presetValues || {}) }
    childFormData.value = buildInitialChildrenData()
  }

  if (!props.formOnly && !openFormContainer('add', modalTitle.value, null, options))
    return

  await nextTick()
  formRef.value?.restoreValidation()
  refreshRuntimeFormulas(0)
  markActiveInlineFormClean()

  emit('add', { defaults: presetValues, context: options })
  emit('modal-open', { status: 'add', row: null, defaults: presetValues, context: options })
}

async function handleAddChild(row) {
  if (!row) {
    window.$message.warning('缺少父级数据，无法添加下级')
    return
  }
  const parentField = resolveTreeParentField()
  const parentValue = resolveTreeParentValue(row)
  if (!isUsableKeyValue(parentValue)) {
    window.$message.warning(`缺少${parentField}对应的父级值，无法添加下级`)
    return
  }
  await handleAdd({ [parentField]: parentValue }, { title: '新增下级', parentRow: row })
}

function resolveTreeParentField() {
  return props.treeConfig?.parentField || props.treeConfig?.filterField || 'parentId'
}

function resolveTreeParentValue(row = {}) {
  const keyField = props.treeConfig?.keyField || (typeof props.rowKey === 'string' ? props.rowKey : 'id')
  const rowKey = typeof props.rowKey === 'string' ? props.rowKey : ''
  return row?.[keyField] ?? (rowKey ? row?.[rowKey] : undefined) ?? row?.id ?? row?.key ?? row?.targetValue
}

function isPlainRecord(value) {
  if (!value || typeof value !== 'object')
    return false
  const prototype = Object.getPrototypeOf(value)
  return prototype === Object.prototype || prototype === null
}

/**
 * 编辑
 */
async function handleEdit(row) {
  if (activateReusableInlineFormTab('edit', row)) {
    emit('edit', row)
    emit('modal-open', { status: 'edit', row })
    return
  }

  modalTitle.value = row?.__modalTitle || '编辑'
  modalStatus.value = 'edit'
  currentRow.value = row

  // 调用 beforeRenderForm 钩子（编辑时）
  const processedRow = await callHook('beforeRenderForm', row, data => data)
  const renderRow = mergeHookRowWithOriginal(row, processedRow)

  // 如果需要加载详情
  if (props.loadDetailOnEdit) {
    window.$loading.show('加载中...')
    await loadDetail(renderRow)
    window.$loading.close()
  }
  else {
    // 调用 beforeRenderDetail 钩子
    const data = await callHook('beforeRenderDetail', renderRow, data => data)
    applyDetailData(data)
  }

  if (!openFormContainer('edit', modalTitle.value, row))
    return
  // 清除上一次潜留的表单校验状态
  await nextTick()
  formRef.value?.restoreValidation()
  refreshRuntimeFormulas(0)
  markActiveInlineFormClean()

  emit('edit', row)
  emit('modal-open', { status: 'edit', row })
}

/**
 * 查看详情
 */
async function handleDetail(row) {
  if (activateReusableInlineFormTab('detail', row)) {
    emit('detail', row)
    emit('modal-open', { status: 'detail', row })
    return
  }

  modalTitle.value = row?.__modalTitle || '查看详情'
  modalStatus.value = 'detail'
  currentRow.value = row
  detailActiveTab.value = 'business'
  detailRuntime.value = row?._documentRuntime || null

  const processedRow = await callHook('beforeRenderForm', row, data => data)
  const renderRow = mergeHookRowWithOriginal(row, processedRow)

  if (props.loadDetailOnEdit) {
    window.$loading.show('加载中...')
    await loadDetail(renderRow)
    window.$loading.close()
  }
  else {
    const data = await callHook('beforeRenderDetail', renderRow, data => data)
    applyDetailData(data)
  }

  await loadDetailRuntime(renderRow)

  if (!openFormContainer('detail', modalTitle.value, row))
    return
  await nextTick()
  formRef.value?.restoreValidation()
  markActiveInlineFormClean()

  emit('detail', row)
  emit('modal-open', { status: 'detail', row })
}

async function loadDetailRuntime(row) {
  const objectCode = resolveRuntimeObjectCode({}, row)
  const recordId = resolveRowKeyValue(row)
  if (!objectCode || !recordId) {
    return
  }
  detailRuntimeLoading.value = true
  try {
    const res = await request.get(`/ai/business/document/${objectCode}/${recordId}/runtime`, { needTip: false })
    detailRuntime.value = res?.data || null
  }
  catch (error) {
    if (!detailRuntime.value)
      detailRuntime.value = null
    console.warn('[AiCrudPage] 加载单据流程运行态失败:', error?.message || error)
  }
  finally {
    detailRuntimeLoading.value = false
  }
}

/**
 * 加载详情
 */
async function loadDetail(row) {
  confirmLoading.value = true

  try {
    const idValue = resolveRowKeyValue(row)
    if (!isUsableKeyValue(idValue)) {
      console.warn('[AiCrudPage] loadDetail id缺失', { rowKey: props.rowKey, rowKeys: Object.keys(row || {}).slice(0, 20), row })
      window.$message.warning(`缺少${props.rowKey}参数，无法加载详情`)
      confirmLoading.value = false
      return
    }
    const { method, url } = parseApiConfig(
      'detail',
      `${props.api}/${idValue}`,
      'get',
      { id: idValue },
    )

    // 确定使用哪种请求方法
    let requestMethod = method
    // 如果方法明确指定为 postEncrypt，则使用加密请求，不管 isEncrypt 属性
    const useEncrypt = method === 'postEncrypt' || (props.isEncrypt && method !== 'get')
    if (useEncrypt) {
      requestMethod = method === 'postEncrypt' ? 'postEncrypt' : method.toLowerCase()
    }
    else {
      requestMethod = method.toLowerCase()
    }

    // 构建请求参数
    const requestConfig = {
      method: requestMethod,
      url,
    }

    // 对于 POST 请求，如果 URL 中不包含 ID，则将主键作为 query 参数传递
    const urlHasId = url.endsWith(`/${idValue}`) || url.includes(`/${idValue}?`) || url.includes(`/${idValue}/`)
    if ((requestMethod === 'post' || requestMethod === 'postEncrypt') && !urlHasId) {
      const idKey = props.rowKey
      requestConfig.params = { [idKey]: idValue }
    }

    // 发送请求
    let response
    if (useEncrypt && requestMethod === 'postEncrypt') {
      // 使用加密请求
      response = await postEncrypt(url, {}, { params: requestConfig.params })
    }
    else {
      // 使用普通请求
      response = await request(requestConfig)
    }

    // 调用 beforeRenderDetail 钩子
    const data = await callHook('beforeRenderDetail', response.data, data => data)
    applyDetailData(data)
  }
  catch (error) {
    console.error('加载详情失败:', error)
    window.$message.error('加载详情失败')
  }
  finally {
    confirmLoading.value = false
  }
}

/**
 * 删除
 */
async function handleDelete(row) {
  const rows = [row]
  const key = resolveRowKeyValue(row)
  if (!isUsableKeyValue(key)) {
    console.warn('[AiCrudPage] delete id缺失', { rowKey: props.rowKey, rowKeys: Object.keys(row || {}).slice(0, 20), row })
    window.$message.warning(`缺少${props.rowKey}参数，无法删除`)
    return
  }
  const keys = [key]

  await performDelete(rows, keys)
}

/**
 * 批量删除
 */
async function handleBatchDelete() {
  const rows = tableRef.value?.getCheckedRows() || []
  const keys = selectedKeys.value

  if (rows.length === 0) {
    window.$message.warning('请先选择要删除的数据')
    return
  }

  await performDelete(rows, keys)
}

/**
 * 执行删除
 */
async function performDelete(rows, keys) {
  // 调用 beforeDelete 钩子
  const shouldContinue = await callHook('beforeDelete', rows, result => result)

  if (shouldContinue === false) {
    return
  }

  window.$dialog.warning({
    title: '确认删除',
    content: `确定要删除选中的 ${keys.length} 条数据吗？此操作不可恢复！`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        // 检查是否配置了带 :id 占位符的删除 URL
        const deleteApiConfig = props.apiConfig.delete
        const hasIdPlaceholder = deleteApiConfig && deleteApiConfig.includes(':id')
        const hasRowKeyPlaceholder = deleteApiConfig && deleteApiConfig.includes(`:${props.rowKey}`)
        const hasBraceIdPlaceholder = deleteApiConfig && deleteApiConfig.includes('{id}')
        const hasBraceRowKeyPlaceholder = deleteApiConfig && deleteApiConfig.includes(`{${props.rowKey}}`)

        // 配置了占位符时，批量删除逐条替换 ID 调用，避免把数组提交到单条删除接口。
        if (hasIdPlaceholder || hasRowKeyPlaceholder || hasBraceIdPlaceholder || hasBraceRowKeyPlaceholder) {
          for (const key of keys) {
            const urlParams = { id: key }
            const { method, url } = parseApiConfig('delete', props.api, 'delete', urlParams)

            let requestMethod = method
            const useEncrypt = method === 'postEncrypt' || (props.isEncrypt && method !== 'get')
            if (useEncrypt) {
              requestMethod = method === 'postEncrypt' ? 'postEncrypt' : method.toLowerCase()
            }
            else {
              requestMethod = method.toLowerCase()
            }

            if (useEncrypt && requestMethod === 'postEncrypt') {
              await postEncrypt(url, key)
            }
            else {
              await request({
                method: requestMethod,
                url,
              })
            }
          }
        }
        else {
          // 批量删除或未配置占位符时，使用原有逻辑
          const { method, url } = parseApiConfig('delete', props.api, 'delete')

          let requestMethod = method
          const useEncrypt = method === 'postEncrypt' || (props.isEncrypt && method !== 'get')
          if (useEncrypt) {
            requestMethod = method === 'postEncrypt' ? 'postEncrypt' : method.toLowerCase()
          }
          else {
            requestMethod = method.toLowerCase()
          }

          if (useEncrypt && requestMethod === 'postEncrypt') {
            await postEncrypt(url, keys)
          }
          else {
            await request({
              method: requestMethod,
              url,
              data: keys,
            })
          }
        }

        window.$message.success('删除成功')
        selectedKeys.value = []
        loadList()
      }
      catch (error) {
        console.error('删除失败:', error)
        window.$message.error('删除失败')
      }
    },
  })
}

/**
 * 提交表单
 */
async function handleModalConfirm() {
  if (isDetailMode.value) {
    if (usesInlineFormWorkspace.value)
      handleCloseActiveInlineFormTab()
    else
      modalVisible.value = false
    return
  }

  try {
    await nextTick()
    await formRef.value?.validate()
    await childFormRef.value?.validate?.()

    // 调用 beforeSubmit 钩子
    const latestFormData = formRef.value?.getFormData?.() || formData.value
    formData.value = latestFormData
    let data = await callHook('beforeSubmit', { ...latestFormData, ...resolveSubmitDefaultParams() }, data => data)

    if (data === false) {
      return
    }
    data = buildMasterDetailSubmitData(data)
    data = await callHook('afterBuildSubmitData', data, data => data)
    if (data === false) {
      return
    }
    // 统一处理时间戳，转换为标准日期格式
    data = JSON.parse(JSON.stringify(data), (key, value) => {
      // 判断是否是时间戳（数字且长度在10位（秒）到13位（毫秒）之间）
      if (typeof value === 'number' && (value.toString().length === 10 || value.toString().length === 13)) {
        const date = new Date(value.toString().length === 10 ? value * 1000 : value)
        // 格式化为 yyyy-MM-dd HH:mm:ss
        const year = date.getFullYear()
        const month = String(date.getMonth() + 1).padStart(2, '0')
        const day = String(date.getDate()).padStart(2, '0')
        const hours = String(date.getHours()).padStart(2, '0')
        const minutes = String(date.getMinutes()).padStart(2, '0')
        const seconds = String(date.getSeconds()).padStart(2, '0')
        return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
      }
      return value
    })

    confirmLoading.value = true

    const isEdit = modalStatus.value === 'edit'

    // 新增时优先使用 apiConfig.add,其次使用 apiConfig.create
    let createKey = 'add'
    if (!isEdit) {
      if (props.apiConfig.create) {
        createKey = 'create'
      }
      else if (props.apiConfig.add) {
        createKey = 'add'
      }
    }

    const idValue = isEdit ? resolveRowKeyValue(currentRow.value) : null
    if (isEdit && !isUsableKeyValue(idValue)) {
      window.$message.warning(`缺少${props.rowKey}参数，无法提交编辑`)
      return
    }
    const { method, url } = parseApiConfig(
      isEdit ? 'update' : createKey,
      isEdit ? `${props.api}/${idValue}` : props.api,
      isEdit ? 'put' : 'post',
      isEdit ? { id: idValue } : {},
    )

    // 确定使用哪种请求方法
    let requestMethod = method
    // 如果方法明确指定为 postEncrypt，则使用加密请求，不管 isEncrypt 属性
    const useEncrypt = method === 'postEncrypt' || (props.isEncrypt && method !== 'get')
    if (useEncrypt) {
      requestMethod = method === 'postEncrypt' ? 'postEncrypt' : method.toLowerCase()
    }
    else {
      requestMethod = method.toLowerCase()
    }

    // 发送请求
    let response
    if (useEncrypt && requestMethod === 'postEncrypt') {
      // 使用加密请求
      response = await postEncrypt(url, data)
    }
    else {
      // 使用普通请求
      response = await request({ method: requestMethod, url, data })
    }

    window.$message.success(`${isEdit ? '编辑' : '新增'}成功`)
    if (props.formOnly) {
      formOnlySubmitted.value = true
    }
    else if (usesInlineFormWorkspace.value) {
      handleInlineFormSubmitSuccess(response, isEdit)
    }
    else {
      modalVisible.value = false
    }

    await callHook('afterSubmit', { data, response, isEdit }, data => data)

    // 触发提交成功事件
    emit('submit-success', { data, response, isEdit })

    if (!props.formOnly)
      loadList()
  }
  catch (error) {
    console.error('提交失败:', error)

    // 前端表单校验失败时 AiForm 已完成滚动和高亮定位，这里不再弹出重复提示
    if (Array.isArray(error) && error.length > 0) {
      console.error('验证错误详情:', error)
    }
    else {
      window.$message.error(error?.message || '提交失败')
    }

    // 触发提交失败事件
    emit('submit-error', { error, data: formData.value })
  }
  finally {
    confirmLoading.value = false
  }
}

/**
 * 弹窗取消
 */
function handleModalCancel() {
  modalVisible.value = false
}

/**
 * 弹窗关闭后
 */
function handleModalClose() {
  formData.value = {}
  childFormData.value = {}
  currentRow.value = null
  formRef.value?.restoreValidation()

  emit('modal-close')
}

function resetFormOnly() {
  formOnlySubmitted.value = false
  handleAdd()
}

/**
 * 显示导入弹窗
 */
function handleShowImport() {
  importModalVisible.value = true
}

/**
 * 自定义导入请求，复用统一 axios 拦截器补齐认证和防重放头。
 */
async function handleImportRequest({ file, data, onFinish, onError, onProgress }) {
  try {
    const { method, url } = parseApiConfig('import', props.importApi, 'post')
    const requestMethod = method === 'postEncrypt' ? 'post' : method.toLowerCase()
    const formData = new FormData()
    formData.append('file', file.file)

    const extraData = { ...props.importData, ...(data || {}) }
    Object.entries(extraData).forEach(([key, value]) => {
      if (value !== undefined && value !== null) {
        formData.append(key, value)
      }
    })

    const response = await request({
      method: requestMethod,
      url,
      data: formData,
      headers: props.importHeaders,
      encrypt: false,
      onUploadProgress: (event) => {
        if (!event.total)
          return
        onProgress?.({ percent: Math.round((event.loaded / event.total) * 100) })
      },
    })

    const result = response?.data || response
    if (result?.success === false) {
      window.$message.error(buildImportFailureMessage(result))
      onError?.()
      return
    }

    window.$message.success(result?.summary || '导入成功')
    importModalVisible.value = false
    loadList()
    onFinish?.()
  }
  catch (error) {
    console.error('导入失败:', error)
    window.$message.error(error?.message || '导入失败')
    onError?.()
  }
}

function buildImportFailureMessage(result) {
  if (result?.summary && result?.errors?.length) {
    return result.summary
  }
  const firstError = result?.errors?.[0]
  if (!firstError) {
    return result?.summary || '导入校验失败'
  }
  const label = firstError.columnName || firstError.label || firstError.field || '字段'
  const message = firstError.errorMessage || firstError.message || '数据不正确'
  return `导入校验失败：第${firstError.rowNum || '-'}行【${label}】${message}`
}

/**
 * 下载导入模板
 */
async function handleDownloadTemplate() {
  if (!props.importTemplateUrl && !props.apiConfig.importTemplate) {
    window.$message.warning('未配置导入模板地址')
    return
  }

  try {
    const { method, url } = parseApiConfig('importTemplate', props.importTemplateUrl, 'get')
    const response = await request({
      method: method === 'postEncrypt' ? 'post' : method.toLowerCase(),
      url,
      responseType: 'blob',
      rawResponse: true,
      encrypt: false,
    })
    downloadBlobResponse(response, props.exportFileName || '导入模板.xlsx')
  }
  catch (error) {
    console.error('下载导入模板失败:', error)
    window.$message.error(error?.message || '下载导入模板失败')
  }
}

/**
 * 导出
 */
async function handleExport() {
  exportLoading.value = true
  try {
    const { method, url } = parseApiConfig('export', props.exportApi || props.api, 'post')

    const params = {
      ...searchParams.value,
      ...props.publicParams,
    }

    // 确定使用哪种请求方法
    let requestMethod = method
    // 如果方法明确指定为 postEncrypt，则使用加密请求，不管 isEncrypt 属性
    const useEncrypt = method === 'postEncrypt' || (props.isEncrypt && method !== 'get')
    if (useEncrypt) {
      requestMethod = method === 'postEncrypt' ? 'postEncrypt' : method.toLowerCase()
    }
    else {
      requestMethod = method.toLowerCase()
    }

    // 发送请求
    let response
    if (useEncrypt && requestMethod === 'postEncrypt') {
      response = await postEncrypt(url, params, { responseType: 'blob', rawResponse: true })
    }
    else {
      response = await request({
        method: requestMethod,
        url,
        data: requestMethod === 'get' ? undefined : params,
        params: requestMethod === 'get' ? params : undefined,
        responseType: 'blob',
        rawResponse: true,
        encrypt: false,
      })
    }

    const asyncExportResult = await resolveAsyncExportResult(response)
    if (asyncExportResult?.async) {
      window.$message.success(asyncExportResult.message || '导出任务已提交')
      activeExportTaskId.value = asyncExportResult.taskId
      exportTaskDrawerVisible.value = true
      exportTaskPagination.value.page = 1
      await loadExportTasks()
      pollExportTask(asyncExportResult.taskId)
      return
    }

    downloadBlobResponse(response, props.exportFileName || '导出数据.xlsx')
    window.$message.success('导出成功')
  }
  catch (error) {
    console.error('导出失败:', error)
    window.$message.error('导出失败')
  }
  finally {
    exportLoading.value = false
  }
}

async function resolveAsyncExportResult(response) {
  if (response?.data?.async !== undefined) {
    return response.data
  }
  if (response?.async !== undefined) {
    return response
  }
  const blob = response?.data instanceof Blob ? response.data : response instanceof Blob ? response : null
  if (blob?.type?.includes('json')) {
    try {
      const text = await blob.text()
      if (!text) {
        return null
      }
      const parsed = JSON.parse(text)
      if (parsed?.data?.async !== undefined) {
        return parsed.data
      }
      if (parsed?.async !== undefined) {
        return parsed
      }
    }
    catch (error) {
      console.warn('解析异步导出响应失败:', error)
    }
  }
  return null
}

async function handleOpenExportTasks() {
  if (!resolvedExportTaskConfigKey.value) {
    window.$message.warning('当前导出接口不支持任务查询')
    return
  }
  exportTaskDrawerVisible.value = true
  exportTaskPagination.value.page = 1
  await loadExportTasks()
}

async function loadExportTasks() {
  if (!resolvedExportTaskConfigKey.value) {
    return
  }
  exportTaskLoading.value = true
  try {
    const response = await request({
      method: 'get',
      url: `/ai/crud/${resolvedExportTaskConfigKey.value}/export/tasks`,
      params: {
        pageNum: exportTaskPagination.value.page,
        pageSize: exportTaskPagination.value.pageSize,
      },
    })
    const page = response?.data || {}
    exportTasks.value = page.records || []
    exportTaskPagination.value.itemCount = page.total || 0
  }
  catch (error) {
    console.error('加载导出任务失败:', error)
    window.$message.error(error?.message || '加载导出任务失败')
  }
  finally {
    exportTaskLoading.value = false
  }
}

async function pollExportTask(taskId) {
  clearExportTaskPollTimer()
  if (!taskId || !resolvedExportTaskConfigKey.value) {
    return
  }

  const fetchTask = async () => {
    try {
      const response = await request({
        method: 'get',
        url: `/ai/crud/${resolvedExportTaskConfigKey.value}/export/tasks/${taskId}`,
        needTip: false,
      })
      const task = response?.data
      if (!task) {
        return
      }
      upsertExportTask(task)
      if (isExportTaskRunning(task)) {
        exportTaskPollTimer.value = window.setTimeout(fetchTask, 2000)
      }
      else if (task.status === 'SUCCESS') {
        window.$message.success('导出任务已完成')
      }
      else if (task.status === 'FAILED') {
        window.$message.error(task.errorMessage || '导出任务失败')
      }
    }
    catch (error) {
      console.warn('轮询导出任务失败:', error)
    }
  }

  await fetchTask()
}

function upsertExportTask(task) {
  const index = exportTasks.value.findIndex(item => String(item.id) === String(task.id))
  if (index >= 0) {
    exportTasks.value.splice(index, 1, task)
  }
  else {
    exportTasks.value.unshift(task)
  }
}

async function handleDownloadExportTask(row) {
  if (!row?.fileId) {
    window.$message.warning('导出文件还未生成')
    return
  }
  try {
    await downloadFile(row.fileId, row.fileName || '导出数据.xlsx')
  }
  catch (error) {
    console.error('下载导出文件失败:', error)
    window.$message.error(error?.message || '下载导出文件失败')
  }
}

function handleExportTaskPageChange(page) {
  exportTaskPagination.value.page = page
  loadExportTasks()
}

function handleExportTaskPageSizeChange(pageSize) {
  exportTaskPagination.value.pageSize = pageSize
  exportTaskPagination.value.page = 1
  loadExportTasks()
}

function clearExportTaskPollTimer() {
  if (exportTaskPollTimer.value) {
    window.clearTimeout(exportTaskPollTimer.value)
    exportTaskPollTimer.value = null
  }
}

function isExportTaskRunning(task) {
  return ['PENDING', 'RUNNING'].includes(task?.status)
}

function resolveExportTaskStatusText(status) {
  switch (status) {
    case 'PENDING':
      return '排队中'
    case 'RUNNING':
      return '导出中'
    case 'SUCCESS':
      return '已完成'
    case 'FAILED':
      return '失败'
    default:
      return '未知'
  }
}

function resolveExportTaskTagType(status) {
  switch (status) {
    case 'SUCCESS':
      return 'success'
    case 'FAILED':
      return 'error'
    case 'RUNNING':
      return 'info'
    case 'PENDING':
      return 'warning'
    default:
      return 'default'
  }
}

function downloadBlobResponse(response, fallbackName) {
  const blob = response?.data instanceof Blob ? response.data : response
  if (!(blob instanceof Blob)) {
    throw new TypeError('下载响应不是文件流')
  }

  const fileName = resolveDownloadFileName(response, fallbackName)
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

function resolveDownloadFileName(response, fallbackName) {
  const disposition = response?.headers?.['content-disposition']
    || response?.headers?.get?.('content-disposition')
    || ''
  const utf8Match = disposition.match(/filename\*=utf-8''([^;]+)/i)
  if (utf8Match?.[1]) {
    return decodeURIComponent(utf8Match[1])
  }
  const normalMatch = disposition.match(/filename="?([^";]+)"?/i)
  if (normalMatch?.[1]) {
    return decodeURIComponent(normalMatch[1])
  }
  return fallbackName
}

/**
 * ==================== 暴露方法 ====================
 */
defineExpose({
  /**
   * 外部触发搜索
   */
  search: handleSearch,

  /**
   * 刷新列表
   */
  refresh: loadList,

  /**
   * 加载列表
   */
  loadList,

  /**
   * 获取选中的行
   */
  getSelectedRows: () => tableRef.value?.getCheckedRows() || [],

  /**
   * 获取选中的键
   */
  getSelectedKeys: () => selectedKeys.value,

  /**
   * 清除选中
   */
  clearSelection: () => {
    selectedKeys.value = []
    tableRef.value?.clearSelection()
  },

  /**
   * 设置选中的键
   */
  setSelectedKeys: (keys) => {
    selectedKeys.value = keys
    tableRef.value?.setCheckedKeys(keys)
  },

  /**
   * 获取表格数据
   */
  getTableData: () => dataSource.value,

  /**
   * 设置表格数据
   */
  setTableData: (data) => {
    dataSource.value = data
  },

  /**
   * 打开新增弹窗
   */
  showAdd: handleAdd,

  /**
   * 打开编辑弹窗
   */
  showEdit: handleEdit,

  /**
   * 打开详情弹窗
   */
  showDetail: handleDetail,

  /**
   * 编辑（同 showEdit）
   */
  handleEdit,

  /**
   * 查看详情（同 showDetail）
   */
  handleDetail,

  /**
   * 删除
   */
  handleDelete,

  /**
   * 批量删除
   */
  handleBatchDelete,

  /**
   * 提交当前弹窗表单
   */
  submitForm: handleModalConfirm,

  /**
   * 关闭弹窗
   */
  closeModal: () => {
    if (usesInlineFormWorkspace.value)
      handleCloseActiveInlineFormTab()
    else
      modalVisible.value = false
  },

  /**
   * 获取搜索参数
   */
  getSearchParams: () => searchParams.value,

  /**
   * 设置搜索参数
   */
  setSearchParams: (params) => {
    searchParams.value = params
  },

  /**
   * 重置搜索
   */
  resetSearch: () => {
    searchRef.value?.handleReset()
  },
})

/**
 * ==================== 生命周期 ====================
 */
onMounted(() => {
  if (props.formOnly) {
    handleAdd()
    return
  }
  if (!props.lazy) {
    loadList()
  }
})

onBeforeUnmount(() => {
  clearExportTaskPollTimer()
  clearRuntimeFormulaTimers()
})

// 监听公共参数内容变化，避免设计器拖拽尺寸时仅对象引用变化导致重复请求
watch(() => stableSerialize(props.publicParams || {}), () => {
  if (props.formOnly)
    return
  pagination.value.page = 1
  loadList()
})

watch(() => stableSerialize(props.publicQuery || {}), () => {
  if (props.formOnly)
    return
  pagination.value.page = 1
  loadList()
})
</script>

<style scoped>
.ai-crud-page {
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 0;
  background: var(--bg-primary);
}

.ai-crud-page-loading-mask {
  position: absolute;
  inset: 0;
  z-index: 20;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgb(255 255 255 / 64%);
  backdrop-filter: blur(1px);
}

.ai-crud-page.is-form-only {
  overflow: auto;
  background: #f6f8fb;
  padding: 16px;
}

.ai-crud-form-only,
.ai-crud-form-only-result {
  width: min(960px, 100%);
  margin: 0 auto;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

.ai-crud-form-only-result {
  display: grid;
  min-height: 360px;
  place-items: center;
}

.form-only-head {
  border-bottom: 1px solid #e5e7eb;
  padding: 16px 18px;
}

.form-only-head h2 {
  margin: 0;
  color: #111827;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0;
}

.form-only-body {
  padding: 18px;
}

.form-only-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  border-top: 1px solid #e5e7eb;
  background: #f8fafc;
  padding: 12px 18px;
}

.ai-crud-edit-form {
  --ai-crud-form-item-gap: 4px;
}

.ai-crud-edit-form :deep(.n-form-item) {
  --n-feedback-height: 16px;
  margin-bottom: 0;
}

.ai-crud-edit-form :deep(.n-form-item-label) {
  min-height: 32px;
  display: grid;
  align-items: center;
  color: var(--text-secondary);
  font-weight: 600;
}

.ai-crud-edit-form :deep(.n-form-item-label__asterisk) {
  align-self: center;
  line-height: 1;
}

.ai-crud-edit-form :deep(.n-form-item-blank) {
  min-height: 32px;
  display: flex;
  align-items: center;
}

.ai-crud-edit-form :deep(.n-form-item-feedback-wrapper) {
  min-height: 16px;
  font-size: 12px;
}

.ai-crud-edit-form :deep(.ai-form-control) {
  min-height: 32px;
  display: flex;
  align-items: center;
}

.ai-crud-edit-form :deep(.ai-form-control > *) {
  width: 100%;
}

.ai-crud-edit-form :deep(.ai-form-control--radio),
.ai-crud-edit-form :deep(.ai-form-control--radioButton),
.ai-crud-edit-form :deep(.ai-form-control--checkbox) {
  min-height: 32px;
}

.ai-crud-edit-form :deep(.ai-form-control--radio .n-space),
.ai-crud-edit-form :deep(.ai-form-control--checkbox .n-space) {
  align-items: center !important;
}

.ai-crud-edit-form :deep(.ai-form-item--textarea .n-form-item-label),
.ai-crud-edit-form :deep(.ai-form-item--imageUpload .n-form-item-label),
.ai-crud-edit-form :deep(.ai-form-item--fileUpload .n-form-item-label) {
  align-items: flex-start;
  padding-top: 5px;
}

.ai-crud-edit-form :deep(.n-input),
.ai-crud-edit-form :deep(.n-input-number),
.ai-crud-edit-form :deep(.n-base-selection) {
  --n-border-radius: 6px;
}

.ai-crud-edit-form :deep(.ai-form-section-title) {
  margin-top: 10px;
}

.ai-crud-edit-form :deep(.af-layout-grid > .n-gi:first-child .ai-form-section-title) {
  margin-top: 0;
}

/* 搜索区域 */
.ai-crud-search {
  background: var(--bg-primary);
  flex-shrink: 0;
  min-height: 0;
}

/* 主内容区域 */
.ai-crud-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--bg-primary);
  overflow: hidden;
  min-height: 0;
}

.ai-crud-main.has-inline-workspace {
  gap: 12px;
}

.ai-crud-main.is-tab-workspace {
  gap: 0;
}

/* 工具栏区域 */
.ai-crud-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  flex-wrap: wrap;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.toolbar-left {
  flex: 1;
}

.toolbar-right {
  flex-shrink: 0;
}

/* 表格区域 */
.ai-crud-table {
  flex: 1 1 auto;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.ai-crud-main.has-inline-workspace:not(.is-tab-workspace) .ai-crud-table {
  flex: 1 1 auto;
  min-height: clamp(220px, 38vh, 460px);
}

.ai-crud-main.is-tab-workspace .ai-crud-table {
  flex: 1 1 auto;
}

.ai-crud-table :deep(.ai-table-wrapper) {
  height: 100%;
  flex: 1 1 auto;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.ai-crud-table :deep(.n-data-table-wrapper) {
  flex: 1 1 auto;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
}

.ai-crud-table :deep(.n-data-table) {
  flex: 1 1 auto;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.ai-crud-table :deep(.n-data-table-base-table) {
  flex: 1 1 auto;
  min-height: 0;
}

.ai-crud-table :deep(.n-data-table-base-table-body) {
  min-height: 0;
  overflow: hidden;
}

/* 表格工具栏 - 不需要额外内边距因为 AiTable 已处理 */
.ai-crud-table :deep(.ai-table-toolbar) {
  padding: 10px 16px;
}

/* 操作列样式 */
.table-action-column {
  display: flex;
  align-items: center;
  white-space: nowrap;
}

/* 按钮操作链接样式 */
:deep(.table-action-link) {
  display: inline-flex;
  align-items: center;
  font-size: var(--font-size-sm);
  color: var(--primary-600);
  cursor: pointer;
  padding: 2px 4px;
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast);
  text-decoration: none;
  line-height: 1.5;
}

:deep(.table-action-link:hover) {
  background: var(--primary-50);
  color: var(--primary-700);
}

:deep(.table-action-link.disabled),
:deep(.table-action-link.disabled:hover) {
  background: transparent;
  color: var(--text-disabled, #94a3b8);
  cursor: not-allowed;
}

:deep(.table-action-link.loading) {
  color: var(--success-600);
}

:deep(.table-action-link.type-info) {
  color: var(--info-600);
}

:deep(.table-action-link.type-info:hover) {
  background: var(--info-50);
  color: var(--info-700);
}

:deep(.table-action-link.type-success) {
  color: var(--success-600);
}

:deep(.table-action-link.type-success:hover) {
  background: var(--success-50);
  color: var(--success-700);
}

:deep(.table-action-link.type-warning) {
  color: var(--warning-600);
}

:deep(.table-action-link.type-warning:hover) {
  background: var(--warning-50);
  color: var(--warning-700);
}

:deep(.table-action-link.danger) {
  color: var(--error-600);
}

:deep(.table-action-link.danger:hover) {
  background: var(--error-50);
  color: var(--error-700);
}

:deep(.table-action-divider) {
  color: var(--border-strong);
  user-select: none;
}

/* 操作列折叠下拉样式 */
:deep(.table-action-column .n-dropdown) {
  min-width: 80px;
}

.ai-crud-detail-tabs {
  min-height: 360px;
}

.ai-crud-detail-tabs :deep(.n-tab-pane) {
  padding-top: 10px;
}

.ai-crud-inline-workspace {
  flex: 0 0 auto;
  display: flex;
  flex-direction: column;
  min-height: 280px;
  max-height: min(52vh, 560px);
  overflow: hidden;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  background: var(--bg-primary);
  box-shadow: 0 8px 22px rgb(15 23 42 / 8%);
}

.ai-crud-inline-workspace.is-tab-workspace {
  flex: 1 1 auto;
  min-height: 0;
  max-height: none;
  border-top: 0;
  border-radius: 0 0 var(--radius-md) var(--radius-md);
  box-shadow: none;
}

.ai-crud-main.is-form-workspace-active:not(.is-tab-workspace) .ai-crud-inline-workspace {
  flex: 1 1 auto;
  min-height: 0;
  max-height: none;
  box-shadow: none;
}

.ai-crud-workspace-tabs,
.ai-crud-inline-tabs {
  flex: 0 0 auto;
  display: flex;
  align-items: flex-end;
  gap: 4px;
  min-height: 42px;
  overflow-x: auto;
  overflow-y: hidden;
  padding: 7px 10px 0;
  border-bottom: 1px solid var(--border-light);
  background: #f8fafc;
  scrollbar-width: thin;
}

.ai-crud-workspace-tabs {
  border: 1px solid var(--border-light);
  border-bottom: 0;
  border-radius: var(--radius-md) var(--radius-md) 0 0;
}

.inline-form-tab {
  height: 34px;
  max-width: 220px;
  min-width: 104px;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  border: 1px solid transparent;
  border-bottom: 0;
  border-radius: 6px 6px 0 0;
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  font-size: var(--font-size-sm);
  line-height: 1;
  padding: 0 8px 0 12px;
  transition:
    background-color var(--transition-fast),
    border-color var(--transition-fast),
    color var(--transition-fast);
}

.inline-form-tab--list {
  min-width: 72px;
}

.inline-form-tab:hover {
  background: #eef2f7;
  color: var(--text-primary);
}

.inline-form-tab.active {
  position: relative;
  background: var(--bg-primary);
  border-color: var(--border-light);
  color: var(--text-primary);
  font-weight: var(--font-weight-medium);
}

.inline-form-tab.active::before {
  position: absolute;
  top: 0;
  left: 10px;
  right: 10px;
  height: 2px;
  border-radius: 999px;
  background: var(--primary-500, #2563eb);
  content: '';
}

.inline-form-tab__title {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.inline-form-tab__dirty {
  width: 6px;
  height: 6px;
  flex: 0 0 auto;
  border-radius: 999px;
  background: var(--warning-500, #f59e0b);
}

.inline-form-tab__close {
  width: 18px;
  height: 18px;
  flex: 0 0 auto;
  display: inline-grid;
  place-items: center;
  border-radius: 4px;
  color: var(--text-tertiary);
}

.inline-form-tab__close:hover {
  background: var(--error-50, #fef2f2);
  color: var(--error-600, #dc2626);
}

.ai-crud-inline-form-panel {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: var(--bg-primary);
}

.inline-form-panel-head {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 48px;
  padding: 10px 14px;
  border-bottom: 1px solid var(--border-light);
}

.inline-form-panel-head > div {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.inline-form-panel-head strong {
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
}

.inline-form-panel-head span {
  font-size: var(--font-size-xs);
  color: var(--text-tertiary);
}

.inline-form-panel-body {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 14px;
}

.inline-form-panel-body .ai-crud-detail-tabs {
  min-height: 0;
}

.inline-form-panel-footer {
  flex: 0 0 auto;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 10px 14px;
  border-top: 1px solid var(--border-light);
  background: #f8fafc;
}

.export-task-current {
  padding: 12px;
  margin-bottom: 12px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  background: var(--bg-secondary);
}

.export-task-current__main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.export-task-current__title {
  min-width: 0;
  flex: 1;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ai-crud-detail-panels {
  margin-top: 12px;
  border-radius: var(--radius-md);
  overflow: hidden;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .toolbar-left,
  .toolbar-right {
    width: 100%;
    justify-content: flex-start;
  }

  .ai-crud-main {
    overflow: hidden;
  }

  .ai-crud-main.has-inline-workspace:not(.is-tab-workspace) .ai-crud-table {
    flex: 0 0 auto;
    min-height: 360px;
  }

  .ai-crud-inline-workspace {
    max-height: none;
  }

  .ai-crud-inline-form-panel {
    min-height: 320px;
  }

  .inline-form-panel-body {
    padding: 12px;
  }

  .inline-form-panel-footer {
    flex-direction: column-reverse;
  }

  .inline-form-panel-footer :deep(.n-button) {
    width: 100%;
  }
}

/* 表格内容优化 */
:deep(.n-data-table) {
  border-radius: 0;
}

:deep(.n-data-table .n-data-table-th) {
  background: var(--bg-secondary) !important;
  font-weight: var(--font-weight-semibold);
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
}

:deep(.n-data-table .n-data-table-td) {
  font-size: var(--font-size-sm);
  color: var(--text-primary);
}

/* 分页器样式 */
:deep(.n-data-table__pagination) {
  width: 100%;
  margin: 0 !important;
  padding: 12px 16px;
  border-top: 1px solid var(--border-light);
}

:deep(.n-data-table__pagination .n-pagination) {
  flex: 0 0 auto;
  width: 100%;
  padding: 0;
  justify-content: flex-end;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  border-top: 0;
}

@media (max-width: 768px) {
  :deep(.n-data-table__pagination .n-pagination) {
    justify-content: center;
  }

  :deep(.n-pagination .n-pagination-item) {
    min-width: 32px;
    height: 32px;
    font-size: 13px;
  }

  :deep(.n-pagination .n-pagination-item__button) {
    padding: 0 6px;
  }

  :deep(.n-pagination-size-picker) {
    font-size: 13px;
  }

  :deep(.n-pagination-quick-jumper) {
    font-size: 13px;
  }
}

@media (max-width: 576px) {
  :deep(.n-data-table__pagination) {
    padding: 10px;
  }

  :deep(.n-data-table__pagination .n-pagination) {
    gap: 4px;
  }

  :deep(.n-pagination .n-pagination-item) {
    min-width: 28px;
    height: 28px;
    font-size: 12px;
  }

  :deep(.n-pagination .n-pagination-item__button) {
    padding: 0 4px;
  }
}
</style>
