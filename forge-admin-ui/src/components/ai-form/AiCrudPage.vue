<!--
  eslint-disable vue/component-name-in-template-casing
  CRUD 页面组件
  基于 Naive UI 的完整 CRUD 解决方案
  参考 LxBasePage 设计，集成搜索、表格、新增、编辑、删除、导入导出等功能

  @author AI Form Team
  @version 1.0.0
-->

<template>
  <div
    ref="crudRootRef"
    class="ai-crud-page"
    :class="{
      'is-form-only': formOnly,
      'is-inline-form-active': showInlineFormWorkspacePane && !isTabWorkspaceMode,
    }"
    :style="pageHeightStyle"
  >
    <div v-if="flowActionPageLoading" class="ai-crud-page-loading-mask">
      <n-spin size="large">
        <template #description>
          {{ flowActionPageLoadingText }}
        </template>
      </n-spin>
    </div>

    <n-alert
      v-if="offlineDraftNotice"
      type="info"
      :bordered="false"
      class="offline-draft-notice"
    >
      {{ offlineDraftNotice }}
      <template #action>
        <n-button
          v-if="offlineReplayAvailable"
          size="small"
          secondary
          :loading="offlineReplayLoading"
          @click="confirmOfflineReplay"
        >
          检查并重放
        </n-button>
      </template>
    </n-alert>

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
          :grid-cols="tiledEditGridCols"
          :label-width="editLabelWidth"
          :label-placement="editLabelPlacement"
          :label-align="editLabelAlign"
          :size="editSize"
          :x-gap="editXGap"
          :y-gap="editYGap"
          :show-feedback="editShowFeedback"
          :enable-collapse="editEnableCollapse"
          :max-visible-fields="editMaxVisibleFields"
          :hide-section-nav="hideFormSectionNav"
          :show-actions="false"
          :context="formContext"
          :form-assets="formAssets"
          @update:value="handleFormValueUpdate"
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
          :row-action-visible="isChildRowActionVisible"
          :row-action-loading="isChildRowActionLoading"
          @row-action="handleChildRowAction"
          @toolbar-action="handleChildToolbarAction"
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
          :row-key="tableRowKeyFn"
          :hide-selection="hideSelection"
          :striped="striped"
          :bordered="bordered"
          :size="tableSize"
          :table-row-gap="tableRowGap"
          :render-mode="activeRenderMode"
          :card-props="cardProps"
          :show-render-mode-switch="showRenderModeSwitch"
          :show-search-toggle="hasSearchSchema"
          :search-visible="searchPanelVisible"
          :max-height="computedMaxHeight"
          :scroll-x="computedScrollX"
          :resizable="resolvedResizable"
          :empty-title="resolvedEmptyTitle"
          :empty-description="resolvedEmptyDescription"
          v-bind="effectiveTableProps"
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
                  <n-button
                    v-if="toolbarOverflowOptions.length === 1"
                    size="small"
                    :type="toolbarOverflowOptions[0].buttonType"
                    :secondary="toolbarOverflowOptions[0].secondary"
                    :disabled="toolbarOverflowOptions[0].disabled"
                    :loading="toolbarOverflowOptions[0].loading"
                    @click="handleToolbarOverflowSelect(toolbarOverflowOptions[0].key)"
                  >
                    <template #icon>
                      <n-icon><component :is="toolbarOverflowOptions[0].iconComponent" /></n-icon>
                    </template>
                    {{ toolbarOverflowOptions[0].label }}
                  </n-button>
                  <n-dropdown
                    v-else-if="toolbarOverflowOptions.length > 1"
                    trigger="click"
                    placement="bottom-start"
                    :options="toolbarDropdownOptions"
                    @select="handleToolbarOverflowSelect"
                  >
                    <n-button size="small" secondary class="ai-crud-more-button">
                      <template #icon>
                        <n-icon><EllipsisVertical /></n-icon>
                      </template>
                      更多
                    </n-button>
                  </n-dropdown>

                  <AiCustomQuery
                    v-if="enableCustomQuery && resolvedCustomQueryConfigKey"
                    :config-key="resolvedCustomQueryConfigKey"
                    :columns="columns"
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
        <section class="ai-crud-inline-form-panel" :class="{ 'is-flat-form': !isTabWorkspaceMode }">
          <header class="inline-form-panel-head" :class="{ 'is-flat-head': !isTabWorkspaceMode }">
            <div class="inline-form-panel-head-main">
              <n-button
                v-if="!isTabWorkspaceMode"
                quaternary
                size="small"
                class="inline-form-back-btn"
                @click="handleCloseActiveInlineFormTab"
              >
                <template #icon>
                  <n-icon><ArrowBackOutline /></n-icon>
                </template>
                返回列表
              </n-button>
              <div class="inline-form-panel-title">
                <strong>{{ activeInlineFormTitle }}</strong>
                <span
                  v-if="showInlineFormModeTag"
                  class="inline-form-mode-tag"
                >{{ inlineFormModeLabel }}</span>
              </div>
            </div>
            <div class="inline-form-panel-head-actions">
              <n-button
                v-if="isTabWorkspaceMode"
                quaternary
                circle
                size="small"
                aria-label="关闭"
                @click="handleCloseActiveInlineFormTab"
              >
                <template #icon>
                  <n-icon><CloseOutline /></n-icon>
                </template>
              </n-button>
              <template v-if="isDetailMode">
                <n-button
                  v-for="action in visibleDetailActions"
                  :key="action.key || action.label"
                  size="small"
                  :quaternary="isPrintRuntimeAction(action)"
                  :circle="isPrintRuntimeAction(action)"
                  :type="isPrintRuntimeAction(action) ? 'default' : resolveButtonType(action)"
                  :loading="isActionLoading(action, formData)"
                  :disabled="isActionDisabled(action, formData) || isActionLoading(action, formData)"
                  :aria-label="resolveActionDisplayLabel(action, formData)"
                  :title="resolveActionDisplayLabel(action, formData)"
                  @click="handleActionClick(action, formData)"
                >
                  <template v-if="resolveDetailActionIcon(action)" #icon>
                    <n-icon><component :is="resolveDetailActionIcon(action)" /></n-icon>
                  </template>
                  <template v-if="!isPrintRuntimeAction(action)">
                    {{ resolveActionDisplayLabel(action, formData) }}
                  </template>
                </n-button>
              </template>
            </div>
          </header>

          <div class="inline-form-panel-body">
            <n-spin :show="confirmLoading" class="ai-crud-record-spin">
              <n-tabs
                v-if="showDetailExtraTabs"
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
                    :grid-cols="tiledEditGridCols"
                    :label-width="editLabelWidth"
                    :label-placement="editLabelPlacement"
                    :label-align="editLabelAlign"
                    :size="editSize"
                    :x-gap="editXGap"
                    :y-gap="editYGap"
                    :show-feedback="editShowFeedback"
                    :enable-collapse="editEnableCollapse"
                    :max-visible-fields="editMaxVisibleFields"
                    :hide-section-nav="hideFormSectionNav"
                    :show-actions="false"
                    :context="formContext"
                    :form-assets="formAssets"
                    @update:value="handleFormValueUpdate"
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
                    :row-action-visible="isChildRowActionVisible"
                    :row-action-loading="isChildRowActionLoading"
                    @row-action="handleChildRowAction"
                    @toolbar-action="handleChildToolbarAction"
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
                <n-tab-pane v-if="showDetailFlowTabs" name="flow" tab="流程进度" display-directive="show:lazy">
                  <AiCrudFlowDetail
                    :runtime="detailRuntime"
                    :loading="detailRuntimeLoading"
                    :show-timeline="detailFlowTimelineVisible"
                    :show-diagram="detailFlowDiagramVisible"
                  />
                </n-tab-pane>
                <n-tab-pane v-if="showDataChangeLogTab" name="audit" tab="变更记录" display-directive="show:lazy">
                  <DataAuditRecordPanel
                    :object-id="dataAuditObjectId"
                    :record-id="dataAuditRecordId"
                    :enabled="dataAuditEnabled"
                    :history-available="dataAuditHistoryAvailable"
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
                  :grid-cols="tiledEditGridCols"
                  :label-width="editLabelWidth"
                  :label-placement="editLabelPlacement"
                  :label-align="editLabelAlign"
                  :size="editSize"
                  :x-gap="editXGap"
                  :y-gap="editYGap"
                  :show-feedback="editShowFeedback"
                  :enable-collapse="editEnableCollapse"
                  :max-visible-fields="editMaxVisibleFields"
                  :hide-section-nav="hideFormSectionNav"
                  :show-actions="false"
                  :context="formContext"
                  :form-assets="formAssets"
                  @update:value="handleFormValueUpdate"
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
                  :row-action-visible="isChildRowActionVisible"
                  :row-action-loading="isChildRowActionLoading"
                  @row-action="handleChildRowAction"
                  @toolbar-action="handleChildToolbarAction"
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
            </n-spin>
          </div>

          <footer v-if="!hideModalFooter && !isDetailMode" class="inline-form-panel-footer">
            <n-button @click="handleInlineFormCancel">
              取消
            </n-button>
            <n-button
              type="primary"
              :loading="confirmLoading"
              :disabled="confirmLoading"
              @click="handleModalConfirm"
            >
              确定
            </n-button>
            <n-button
              v-for="action in visibleFormActions"
              :key="action.key || action.label"
              :type="resolveButtonType(action)"
              :loading="isActionLoading(action, formData)"
              :disabled="isActionDisabled(action, formData) || isActionLoading(action, formData)"
              @click="handleActionClick(action, formData)"
            >
              {{ resolveActionDisplayLabel(action, formData) }}
            </n-button>
          </footer>
        </section>
      </div>
    </div>

    <!-- 新增/编辑/详情弹窗 - Modal 模式。详情默认使用弹窗，避免动态页详情占用右侧抽屉。 -->
    <n-modal
      v-if="!formOnly && !usesInlineFormWorkspace && resolvedFormOpenMode === 'modal'"
      v-model:show="modalVisible"
      class="ai-crud-form-modal"
      :title="modalTitle"
      preset="card"
      :style="{ width: activeModalWidth, maxHeight: 'calc(100vh - 24px)' }"
      :segmented="{ content: 'soft', footer: 'soft' }"
      :closable="true"
      :mask-closable="false"
      @after-leave="handleModalClose"
    >
      <template v-if="isDetailMode && visibleDetailActions.length" #header-extra>
        <n-space>
          <n-button
            v-for="action in visibleDetailActions"
            :key="`modal-head-${action.key || action.label}`"
            size="small"
            :quaternary="isPrintRuntimeAction(action)"
            :circle="isPrintRuntimeAction(action)"
            :type="isPrintRuntimeAction(action) ? 'default' : resolveButtonType(action)"
            :loading="isActionLoading(action, formData)"
            :disabled="isActionDisabled(action, formData) || isActionLoading(action, formData) || confirmLoading"
            :aria-label="resolveActionDisplayLabel(action, formData)"
            :title="resolveActionDisplayLabel(action, formData)"
            @click="handleActionClick(action, formData)"
          >
            <template v-if="resolveDetailActionIcon(action)" #icon>
              <n-icon><component :is="resolveDetailActionIcon(action)" /></n-icon>
            </template>
            <template v-if="!isPrintRuntimeAction(action)">
              {{ resolveActionDisplayLabel(action, formData) }}
            </template>
          </n-button>
        </n-space>
      </template>
      <n-spin :show="confirmLoading" class="ai-crud-record-spin">
        <n-tabs
          v-if="showDetailExtraTabs"
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
              :enable-collapse="editEnableCollapse"
              :max-visible-fields="editMaxVisibleFields"
              :hide-section-nav="hideFormSectionNav"
              :show-actions="false"
              :context="formContext"
              :form-assets="formAssets"
              @update:value="handleFormValueUpdate"
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
              :row-action-visible="isChildRowActionVisible"
              :row-action-loading="isChildRowActionLoading"
              @row-action="handleChildRowAction"
              @toolbar-action="handleChildToolbarAction"
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
          <n-tab-pane v-if="showDetailFlowTabs" name="flow" tab="流程进度" display-directive="show:lazy">
            <AiCrudFlowDetail
              :runtime="detailRuntime"
              :loading="detailRuntimeLoading"
              :show-timeline="detailFlowTimelineVisible"
              :show-diagram="detailFlowDiagramVisible"
            />
          </n-tab-pane>
          <n-tab-pane v-if="showDataChangeLogTab" name="audit" tab="变更记录" display-directive="show:lazy">
            <DataAuditRecordPanel
              :object-id="dataAuditObjectId"
              :record-id="dataAuditRecordId"
              :enabled="dataAuditEnabled"
              :history-available="dataAuditHistoryAvailable"
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
            :enable-collapse="editEnableCollapse"
            :max-visible-fields="editMaxVisibleFields"
            :hide-section-nav="hideFormSectionNav"
            :show-actions="false"
            :context="formContext"
            :form-assets="formAssets"
            @update:value="handleFormValueUpdate"
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
            :row-action-visible="isChildRowActionVisible"
            :row-action-loading="isChildRowActionLoading"
            @row-action="handleChildRowAction"
            @toolbar-action="handleChildToolbarAction"
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
      </n-spin>

      <!-- 弹窗底部按钮：详情动作只放标题栏，避免上下重复 -->
      <template v-if="!hideModalFooter && !isDetailMode" #footer>
        <n-space justify="end">
          <n-button @click="handleModalCancel">
            取消
          </n-button>
          <n-button
            type="primary"
            :loading="confirmLoading"
            :disabled="confirmLoading"
            @click="handleModalConfirm"
          >
            确定
          </n-button>
          <n-button
            v-for="action in visibleFormActions"
            :key="action.key || action.label"
            :type="resolveButtonType(action)"
            :loading="isActionLoading(action, formData)"
            :disabled="isActionDisabled(action, formData) || isActionLoading(action, formData)"
            @click="handleActionClick(action, formData)"
          >
            {{ resolveActionDisplayLabel(action, formData) }}
          </n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 新增/编辑抽屉 - Drawer 模式 -->
    <n-drawer
      v-else-if="!formOnly && !usesInlineFormWorkspace && resolvedFormOpenMode === 'drawer'"
      v-model:show="modalVisible"
      :width="modalWidth"
      :placement="drawerPlacement"
      :mask-closable="false"
      @after-leave="handleModalClose"
    >
      <n-drawer-content :title="modalTitle" :closable="true">
        <template v-if="isDetailMode && visibleDetailActions.length" #header-extra>
          <n-space>
            <n-button
              v-for="action in visibleDetailActions"
              :key="`drawer-head-${action.key || action.label}`"
              size="small"
              :quaternary="isPrintRuntimeAction(action)"
              :circle="isPrintRuntimeAction(action)"
              :type="isPrintRuntimeAction(action) ? 'default' : resolveButtonType(action)"
              :loading="isActionLoading(action, formData)"
              :disabled="isActionDisabled(action, formData) || isActionLoading(action, formData) || confirmLoading"
              :aria-label="resolveActionDisplayLabel(action, formData)"
              :title="resolveActionDisplayLabel(action, formData)"
              @click="handleActionClick(action, formData)"
            >
              <template v-if="resolveDetailActionIcon(action)" #icon>
                <n-icon><component :is="resolveDetailActionIcon(action)" /></n-icon>
              </template>
              <template v-if="!isPrintRuntimeAction(action)">
                {{ resolveActionDisplayLabel(action, formData) }}
              </template>
            </n-button>
          </n-space>
        </template>
        <n-spin :show="confirmLoading" class="ai-crud-record-spin">
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
            :enable-collapse="editEnableCollapse"
            :max-visible-fields="editMaxVisibleFields"
            :hide-section-nav="hideFormSectionNav"
            :show-actions="false"
            :context="formContext"
            :form-assets="formAssets"
            @update:value="handleFormValueUpdate"
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
            :row-action-visible="isChildRowActionVisible"
            :row-action-loading="isChildRowActionLoading"
            @row-action="handleChildRowAction"
            @toolbar-action="handleChildToolbarAction"
          />
        </n-spin>

        <!-- 抽屉底部按钮：详情动作只放标题栏，避免上下重复 -->
        <template v-if="!hideModalFooter && !isDetailMode" #footer>
          <n-space justify="end">
            <n-button @click="handleModalCancel">
              取消
            </n-button>
            <n-button
              type="primary"
              :loading="confirmLoading"
              :disabled="confirmLoading"
              @click="handleModalConfirm"
            >
              确定
            </n-button>
            <n-button
              v-for="action in visibleFormActions"
              :key="action.key || action.label"
              :type="resolveButtonType(action)"
              :loading="isActionLoading(action, formData)"
              :disabled="isActionDisabled(action, formData) || isActionLoading(action, formData)"
              @click="handleActionClick(action, formData)"
            >
              {{ resolveActionDisplayLabel(action, formData) }}
            </n-button>
          </n-space>
        </template>
      </n-drawer-content>
    </n-drawer>

    <AiCrudImportModal
      v-model:show="importModalVisible"
      :importer="executeImport"
      :template-downloader="hasImportTemplate ? handleDownloadTemplate : null"
      :has-template="hasImportTemplate"
      @success="handleImportSuccess"
    />

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

    <!-- 子表工具栏动作弹窗（登记提货/登记退货） -->
    <n-modal
      v-model:show="childToolbarActionModalVisible"
      :title="childToolbarActionTitle"
      preset="card"
      style="width: min(520px, calc(100vw - 32px))"
      :mask-closable="false"
    >
      <n-form label-placement="left" :show-feedback="true">
        <n-form-item label="商品" required>
          <n-select
            v-model:value="childToolbarItemSelected"
            :options="childToolbarItemOptions"
            placeholder="请选择商品"
            clearable
            filterable
          />
        </n-form-item>
        <n-form-item :label="childToolbarQuantityLabel" required>
          <n-input-number
            v-model:value="childToolbarQuantity"
            :min="1"
            :max="childToolbarQuantityMax"
            style="width: 100%"
          />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button :disabled="childToolbarActionSubmitting" @click="closeChildToolbarActionModal">
            取消
          </n-button>
          <n-button type="primary" :loading="childToolbarActionSubmitting" @click="submitChildToolbarAction">
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

    <n-modal
      v-model:show="flowStartApproverModalVisible"
      preset="card"
      title="选择流程审批人"
      style="width: min(560px, calc(100vw - 32px))"
      :mask-closable="false"
    >
      <div class="flow-start-approver-modal">
        <p class="flow-start-approver-modal__tip">
          该流程有节点需要由申请人在发起时指定审批人，选定后将作为流程变量传入。
        </p>
        <n-form label-placement="top">
          <n-form-item v-for="node in flowStartApproverNodes" :key="node.nodeKey" :label="node.nodeName || node.nodeKey" required>
            <UserSelectPicker
              v-model="flowStartApproverSelections[node.nodeKey]"
              v-model:label-value="flowStartApproverLabels[node.nodeKey]"
              :multiple="node.multiple !== false"
              :title="`选择${node.nodeName || node.nodeKey}审批人`"
              :placeholder="node.multiple === false ? '请选择一名审批人' : '请选择一名或多名审批人'"
            />
          </n-form-item>
        </n-form>
      </div>
      <template #footer>
        <n-space justify="end">
          <n-button :disabled="flowStartApproverSubmitting" @click="flowStartApproverModalVisible = false">
            取消
          </n-button>
          <n-button type="primary" :loading="flowStartApproverSubmitting" @click="submitFlowStartWithApprovers">
            发起流程
          </n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script>
import { aiCrudPageLocalComponents } from './aiCrudPageLocalComponents'
import { aiCrudPageProps } from './AiCrudPageProps'
import { useAiCrudPage } from './crud/composables/useAiCrudPage'

export default {
  name: 'AiCrudPage',
  components: { ...aiCrudPageLocalComponents },
  props: aiCrudPageProps,
  emits: [
    'load-list-success',
    'load-list-error',
    'add',
    'edit',
    'detail',
    'delete',
    'submit-success',
    'submit-error',
    'selection-change',
    'modal-open',
    'modal-close',
    'render-mode-change',
    'custom-action',
  ],
  setup(props, { emit, expose }) {
    const api = useAiCrudPage(props, emit)
    if (api.aiCrudPageExposeApi)
      expose(api.aiCrudPageExposeApi)
    return api
  },
}
</script>

<style scoped src="./crud/styles/page.css"></style>

<style src="./crud/styles/modal.css"></style>
