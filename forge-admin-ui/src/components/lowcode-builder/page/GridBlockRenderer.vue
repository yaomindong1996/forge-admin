<template>
  <div
    v-if="blockRuntimeVisible"
    class="grid-block"
    :class="[`block-${block.blockType}`, { selected: selected && !readonly, 'is-form-only': crudPagePresentation.formOnly, 'is-readonly': readonly }]"
    :style="blockStyle"
    :data-block-id="block.id"
    @click="handleRuntimeBlockShellClick"
  >
    <template v-if="isDataFieldBlock && runtimeCrudLoading">
      <div class="runtime-crud-loading">
        <n-skeleton height="32px" :sharp="false" />
        <n-skeleton text :repeat="5" />
      </div>
    </template>

    <template v-else-if="shouldShowDataSourceGuide">
      <div class="data-source-guide" :class="{ 'is-readonly': readonly }">
        <span class="data-source-guide-icon" aria-hidden="true">
          <n-icon><CubeOutline /></n-icon>
        </span>
        <div class="data-source-guide-copy">
          <strong>选择业务对象后，字段将自动生成</strong>
          <span>{{ readonly ? '该页面组件尚未配置数据来源' : '无需逐项添加字段，选择后即可在画布中预览' }}</span>
        </div>
        <n-button v-if="!readonly" size="small" type="primary" secondary @click.stop="emit('requestDataSource', block.id)">
          选择数据源
        </n-button>
      </div>
    </template>

    <!-- 查询表单 -->
    <template v-else-if="block.blockType === 'search-form'">
      <div class="block-header">
        <strong>{{ block.label || '查询表单' }}</strong>
        <span class="block-meta">{{ resolvedFields.length }} 个查询字段</span>
      </div>
      <div v-if="resolvedFields.length" class="search-grid">
        <n-form-item
          v-for="field in resolvedFields"
          :key="field.field"
          :label="field.label || field.field"
          :show-feedback="false"
          :style="{ textAlign: fieldAlign(field.field) }"
        >
          <n-tree-select
            v-if="['treeSelect', 'orgTreeSelect', 'regionTreeSelect'].includes(componentType(field))"
            :placeholder="`请选择${field.label || field.field}`"
            disabled
            size="small"
          />
          <n-input
            v-else-if="!field.dictType && !['date', 'datetime', 'dictSelect', 'userSelect'].includes(componentType(field))"
            :placeholder="`请输入${field.label || field.field}`"
            disabled
            size="small"
          />
          <n-date-picker
            v-else-if="['date', 'datetime'].includes(componentType(field))"
            :type="componentType(field) === 'datetime' ? 'datetime' : 'date'"
            disabled
            size="small"
            style="width: 100%"
          />
          <n-select
            v-else
            :placeholder="`请选择${field.label || field.field}`"
            disabled
            size="small"
          />
        </n-form-item>
      </div>
      <div v-else class="block-empty">
        点击右侧"配置字段"按钮添加查询字段
      </div>
      <div class="search-actions">
        <n-button size="small" type="primary" disabled>
          查询
        </n-button>
        <n-button size="small" disabled>
          重置
        </n-button>
        <n-button v-if="block.props?.collapsible" text size="small" type="primary" disabled>
          收起
        </n-button>
      </div>
    </template>

    <!-- 操作工具栏 -->
    <template v-else-if="block.blockType === 'toolbar'">
      <div class="toolbar-actions">
        <n-button v-if="hasAction('add')" size="small" type="primary" disabled>
          + 新增
        </n-button>
        <n-button v-if="hasAction('import')" size="small" disabled>
          导入
        </n-button>
        <n-button v-if="hasAction('export')" size="small" disabled>
          导出
        </n-button>
        <n-button v-if="hasAction('batch-delete')" size="small" disabled>
          批量删除
        </n-button>
        <n-button v-if="hasAction('custom-query')" size="small" text type="primary" disabled>
          自定义查询
        </n-button>
        <n-button
          v-for="action in toolbarCustomActions"
          :key="action.key"
          size="small"
          :type="action.type === 'default' ? undefined : action.type"
          disabled
        >
          {{ action.label }}
        </n-button>
        <span v-if="!(block.props?.actions?.length)" class="block-empty inline">
          未启用任何工具按钮
        </span>
      </div>
    </template>

    <!-- 返回上一页 -->
    <template v-else-if="block.blockType === 'back-button'">
      <div class="back-button-preview">
        <button type="button" class="back-chip" @click.prevent="handleBackClick">
          <span class="back-icon">
            <n-icon><ChevronBackOutline /></n-icon>
          </span>
          <span>{{ block.props?.text || block.props?.title || block.label || '返回' }}</span>
        </button>
      </div>
    </template>

    <!-- 页面标题 -->
    <template v-else-if="block.blockType === 'page-title'">
      <div class="page-title-preview" :class="`size-${block.props?.size || 'medium'}`" :style="pageTitleStyle">
        <InlineRichText
          class="page-title-rich-editor"
          :model-value="pageTitleRichContent"
          :editable="inlineTextEditing"
          placeholder="输入标题、说明或任意富文本内容"
          @activate="emit('blockActivate', block.id)"
          @update:model-value="updatePageTitleRichContent"
        />
        <n-tag v-if="block.props?.statusText" size="small" :type="block.props?.statusType || 'info'" :bordered="false">
          {{ block.props.statusText }}
        </n-tag>
      </div>
    </template>

    <!-- 基础列表：绑定业务对象后走真实接口；未绑定时才用示意数据 -->
    <template v-else-if="block.blockType === 'data-table'">
      <div v-if="runtimeCrudLoading" class="runtime-crud-loading">
        <n-skeleton height="32px" :sharp="false" />
        <n-skeleton text :repeat="4" />
      </div>
      <AiCrudPage
        v-else-if="effectiveRuntimeCrudProps"
        ref="runtimeCrudRef"
        v-bind="effectiveRuntimeCrudProps"
        :show-search="block.props?.showSearch !== false"
        :show-pagination="block.props?.showPagination !== false"
        :hide-toolbar="block.props?.hideToolbar === true"
        :hide-add="block.props?.hideAdd === true"
        :hide-batch-delete="block.props?.hideBatchDelete === true"
        :table-size="block.props?.tableSize || 'small'"
        :table-row-gap="normalizeTableRowGap(block.props?.rowGap, 8)"
        :render-mode="block.props?.renderMode || 'table'"
        @load-list-success="handleCrudPreviewSuccess"
        @load-list-error="handleCrudPreviewError"
      />
      <template v-else>
        <div class="block-header">
          <strong>{{ block.label || block.props?.title || '基础列表' }}</strong>
          <span class="block-meta">{{ resolvedFields.length ? `${resolvedFields.length} 列示意` : '未绑定数据源' }}</span>
        </div>
        <div v-if="!readonly && !dataSourceConfigured" class="data-source-guide">
          <span class="data-source-guide-icon" aria-hidden="true">
            <n-icon><CubeOutline /></n-icon>
          </span>
          <div class="data-source-guide-copy">
            <strong>先选择业务对象</strong>
            <span>在右侧「数据」Tab 绑定对象后，列表会自动对接分页接口</span>
          </div>
          <n-button size="small" type="primary" secondary @click.stop="emit('requestDataSource', block.id)">
            选择数据源
          </n-button>
        </div>
        <n-data-table
          v-else-if="resolvedFields.length"
          :columns="tableColumns"
          :data="sampleRows"
          :bordered="false"
          size="small"
          class="block-table"
          :style="{ '--block-table-row-height': blockTableRowHeight }"
        />
        <div v-else class="block-empty">
          绑定业务对象或配置字段后，这里会显示列表
        </div>
      </template>
    </template>

    <!-- 详情信息（Naive Descriptions） -->
    <template v-else-if="block.blockType === 'detail-info'">
      <div class="detail-info-preview is-form-grow">
        <n-spin :show="detailInfoBusy">
          <n-descriptions
            v-if="resolvedFields.length"
            class="detail-info-descriptions"
            :title="block.props?.showTitle === false ? undefined : (block.props?.title || block.label || '详情信息')"
            :column="Math.max(1, Math.min(4, Number(block.props?.columnCount || 2)))"
            :label-placement="block.props?.labelPlacement || 'left'"
            :label-align="block.props?.labelAlign || 'left'"
            :size="block.props?.size || 'medium'"
            :bordered="block.props?.bordered !== false"
            :separator="block.props?.separator ?? ':'"
            :label-style="detailInfoLabelStyle"
            :content-style="detailInfoContentStyle"
          >
            <n-descriptions-item
              v-for="field in resolvedFields"
              :key="field.field"
              :label="resolveDetailFieldLabel(field)"
              :span="resolveDetailFieldSpan(field)"
            >
              <FieldValueRenderer
                :value="detailValue(field, 0)"
                :row="detailInfoRecord"
                :field="field"
                :setting="fieldSetting(field.field)"
                :context="runtimeRuleContext"
              />
            </n-descriptions-item>
          </n-descriptions>
          <div v-else class="block-empty">
            点击右侧「配置字段」添加详情字段
          </div>
        </n-spin>
        <div v-if="detailInfoFailMessage" class="detail-info-state error">
          {{ detailInfoFailMessage }}
        </div>
      </div>
    </template>

    <!-- 栅格布局（统一栅格渲染器） -->
    <template v-else-if="block.blockType === 'grid-layout'">
      <DesignerGridRenderer
        :columns="Math.max(1, Number(block.props?.columns || 24))"
        :gutter="Math.max(0, Number(block.props?.gutter ?? block.props?.gap ?? 16))"
        :row-gap="Math.max(0, Number(block.props?.rowGap ?? 0))"
        :cell-min-height="Number(block.props?.cellMinHeight || 160)"
        :cell-padding="Number(block.props?.cellPadding ?? 8)"
        :align-items="block.props?.alignItems || 'stretch'"
        :justify-items="block.props?.justifyItems || 'stretch'"
        :show-cell-border="!readonly && block.props?.showCellBorder !== false"
        :cell-background="block.props?.cellBackground"
        :cells="gridLayoutCells"
        :container-id="block.id"
        :active-cell-key="activeDropCell?.containerId === block.id ? activeDropCell?.cellKey : ''"
        :mode="readonly ? 'preview' : 'designer'"
        class="layout-grid-preview"
        :class="{ 'is-nested-moving': !!nestedMovingBlockId }"
        @cell-drag-enter="payload => handleGridCellDragEnter(payload.event)"
        @cell-drag-over="payload => handleGridCellDragOver(payload.event, payload.cellKey)"
        @cell-drop="payload => handleGridCellDrop(payload.event, payload.cellKey)"
        @cell-context-menu="handleGridCellContextMenu"
      >
        <template #cell="{ cell }">
          <div class="designer-grid-cell-inner">
            <div v-if="hasGridCellChildren(cell)" class="layout-grid-cell-body">
              <div
                v-for="child in cell.children"
                :key="child.id"
                class="layout-grid-cell-child"
                :class="{
                  'selected': !readonly && child.id === selectedBlockId,
                  'is-moving-source': child.id === nestedMovingBlockId,
                }"
                :style="nestedChildShellStyle(child)"
                :data-grid-child-id="child.id"
                @pointerenter="onNestedPointerEnter(child.id)"
                @pointerleave="onNestedPointerLeave(child.id)"
                @click.stop="emit('childBlockSelect', child.id)"
                @contextmenu.prevent.stop="!readonly && handleChildContextMenu($event, child, { cellKey: cell.key, title: '栅格内组件' })"
              >
                <div v-if="showNestedChrome(child.id)" class="nested-block-node-overlay">
                  <span
                    class="nested-block-drag-handle"
                    title="拖动组件"
                    @click.stop
                    @pointerdown.stop.prevent="emit('childBlockMoveStart', { block: child, event: $event })"
                  >
                    <svg width="1em" height="1em" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
                      <path d="M8.25 6.5a1.75 1.75 0 1 0 0-3.5 1.75 1.75 0 0 0 0 3.5Zm0 7.25a1.75 1.75 0 1 0 0-3.5 1.75 1.75 0 0 0 0 3.5Zm1.75 5.5a1.75 1.75 0 1 1-3.5 0 1.75 1.75 0 0 1 3.5 0ZM14.753 6.5a1.75 1.75 0 1 0 0-3.5 1.75 1.75 0 0 0 0 3.5ZM16.5 12a1.75 1.75 0 1 1-3.5 0 1.75 1.75 0 0 1 3.5 0Zm-1.747 9a1.75 1.75 0 1 0 0-3.5 1.75 1.75 0 0 0 0 3.5Z" fill="currentColor" />
                    </svg>
                  </span>
                  <n-dropdown
                    trigger="click"
                    placement="bottom-end"
                    :options="nestedBlockMenuOptions"
                    @select="key => emit('childBlockMenuSelect', { key, block: child })"
                  >
                    <button type="button" class="nested-block-menu-trigger" title="更多操作" @click.stop @mousedown.stop>
                      <svg width="1em" height="1em" viewBox="0 0 512 512" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
                        <circle cx="256" cy="256" r="32" fill="none" stroke="currentColor" stroke-miterlimit="10" stroke-width="32" />
                        <circle cx="416" cy="256" r="32" fill="none" stroke="currentColor" stroke-miterlimit="10" stroke-width="32" />
                        <circle cx="96" cy="256" r="32" fill="none" stroke="currentColor" stroke-miterlimit="10" stroke-width="32" />
                      </svg>
                    </button>
                  </n-dropdown>
                </div>
                <GridBlockRenderer
                  :block="child"
                  :fields="resolveNestedBlockFields(child)"
                  :selected="false"
                  :selected-block-id="selectedBlockId"
                  :readonly="readonly"
                  :runtime-crud-props="resolveNestedBlockRuntimeCrudProps(child)"
                  :runtime-crud-loading="resolveNestedBlockRuntimeCrudLoading(child)"
                  :show-data-source-guide="showDataSourceGuide"
                  :data-source-configured="resolveNestedBlockDataSourceConfigured(child)"
                  :runtime-interactive="runtimeInteractive"
                  :runtime-extension-hooks="runtimeExtensionHooks"
                  :runtime-record="runtimeRecord"
                  :active-drop-cell="activeDropCell"
                  :active-drop-container="activeDropContainer"
                  :nested-moving-block-id="nestedMovingBlockId"
                  :catalog-drag-block-type="catalogDragBlockType"
                  :block-fields-resolver="blockFieldsResolver"
                  :runtime-crud-props-resolver="runtimeCrudPropsResolver"
                  :runtime-crud-loading-resolver="runtimeCrudLoadingResolver"
                  :data-source-configured-resolver="dataSourceConfiguredResolver"
                  :runtime-tree-active-key="runtimeTreeActiveKey"
                  @child-block-select="emit('childBlockSelect', $event)"
                  @child-block-menu-select="emit('childBlockMenuSelect', $event)"
                  @block-props-update="emit('blockPropsUpdate', $event)"
                  @tabs-active-change="emit('tabsActiveChange', $event)"
                  @tab-drop="emit('tabDrop', $event)"
                  @grid-cell-drop="emit('gridCellDrop', $event)"
                  @grid-cell-insert="emit('gridCellInsert', $event)"
                  @container-insert="emit('containerInsert', $event)"
                  @container-clear="emit('containerClear', $event)"
                  @child-block-drag-start="emit('childBlockDragStart', $event)"
                  @child-block-move-start="emit('childBlockMoveStart', $event)"
                  @child-block-drag-end="emit('childBlockDragEnd')"
                  @child-block-resize-start="emit('childBlockResizeStart', $event)"
                  @runtime-tree-select="emit('runtimeTreeSelect', $event)"
                  @request-data-source="emit('requestDataSource', $event)"
                />
                <template v-if="!readonly && child.id === selectedBlockId">
                  <button
                    v-for="anchor in resizeAnchors"
                    :key="anchor"
                    type="button"
                    class="nested-resize-anchor"
                    :class="`anchor-${anchor}`"
                    title="调整组件大小"
                    @pointerdown.stop="emit('childBlockResizeStart', { block: child, event: $event, anchor })"
                  />
                </template>
              </div>
            </div>
            <div v-if="!readonly && shouldShowGridCellDropPreview(cell)" class="layout-grid-cell-drop-preview">
              释放到此格
            </div>
            <div v-else-if="!readonly && shouldShowGridCellEmpty(cell)" class="layout-grid-cell-empty">
              拖入组件 · 右键可插入
            </div>
          </div>
        </template>
      </DesignerGridRenderer>
    </template>

    <!-- 系统 AiCrudPage 组件 -->
    <template v-else-if="block.blockType === 'AiCrudPage'">
      <div class="system-component-preview ai-crud-preview">
        <div v-if="runtimeCrudLoading" class="runtime-crud-loading">
          <n-skeleton height="32px" :sharp="false" />
          <n-skeleton text :repeat="5" />
        </div>
        <RuntimeListGridFlow
          v-else-if="effectiveRuntimeCrudProps && shouldRenderRuntimeListGridShell && runtimeListGridModel"
          ref="runtimeCrudRef"
          :layout="runtimeListGridModel.layout"
          :fields="runtimeListGridModel.fields"
          :runtime-crud-props="effectiveRuntimeCrudProps"
          :runtime-crud-loading="runtimeCrudLoading"
          :data-source-configured="dataSourceConfigured"
          :runtime-extension-hooks="runtimeExtensionHooks"
          :block-fields-resolver="blockFieldsResolver"
          :runtime-crud-props-resolver="runtimeCrudPropsResolver"
          :runtime-crud-loading-resolver="runtimeCrudLoadingResolver"
          :data-source-configured-resolver="dataSourceConfiguredResolver"
          :runtime-tree-active-key="runtimeTreeActiveKey"
          fill-host
          @runtime-tree-select="$emit('runtimeTreeSelect', $event)"
        />
        <TreeCrudTemplate
          v-else-if="effectiveRuntimeCrudProps && shouldRenderTreeCrudShell"
          ref="runtimeCrudRef"
          :crud-props="effectiveRuntimeCrudProps"
        />
        <div
          v-else-if="effectiveRuntimeCrudProps && shouldSuppressBareCrudFallback"
          class="runtime-object-page-empty"
          data-runtime-empty-page="1"
        >
          当前页面暂无组件
        </div>
        <AiCrudPage
          v-else-if="effectiveRuntimeCrudProps"
          ref="runtimeCrudRef"
          v-bind="effectiveRuntimeCrudProps"
          @load-list-success="handleCrudPreviewSuccess"
          @load-list-error="handleCrudPreviewError"
        />
        <AiCrudPage
          v-else
          ref="runtimeCrudRef"
          :lazy="!shouldRequestCrudPreviewApi"
          :api="shouldRequestCrudPreviewApi ? (block.props?.api || '') : ''"
          :api-config="shouldRequestCrudPreviewApi ? resolvedApiConfig : {}"
          :row-key="block.props?.rowKey || 'id'"
          :columns="aiTableColumns"
          :search-schema="aiSearchSchema"
          :edit-schema="aiFormSchema"
          :show-search="block.props?.showSearch !== false"
          :show-pagination="block.props?.showPagination !== false"
          :search-grid-cols="block.props?.searchGridCols || 4"
          :search-label-width="block.props?.searchLabelWidth || 'auto'"
          :search-enable-collapse="block.props?.searchEnableCollapse !== false"
          :search-max-visible-fields="block.props?.searchMaxVisibleFields || 3"
          :search-y-gap="block.props?.searchYGap ?? 16"
          :edit-grid-cols="block.props?.editGridCols || 1"
          :edit-label-width="block.props?.editLabelWidth || 'auto'"
          :edit-label-placement="block.props?.editLabelPlacement || 'left'"
          :edit-label-align="block.props?.editLabelAlign || 'right'"
          :edit-size="block.props?.editSize || 'medium'"
          :edit-show-feedback="block.props?.editShowFeedback !== false"
          :edit-enable-collapse="block.props?.editEnableCollapse === true"
          :edit-max-visible-fields="block.props?.editMaxVisibleFields || 6"
          :edit-x-gap="block.props?.editXGap ?? 16"
          :edit-y-gap="block.props?.editYGap ?? 8"
          :modal-width="block.props?.modalWidth || '800px'"
          :detail-modal-width="block.props?.detailModalWidth || 'min(1080px, 92vw)'"
          :form-open-mode="resolveEffectiveFormOpenMode(block.props || {}, runtimeCrudProps || {})"
          :tab-workspace="block.props?.tabWorkspace || {}"
          :modal-type="resolveEffectiveModalType(block.props || {}, runtimeCrudProps || {})"
          :drawer-placement="block.props?.drawerPlacement || 'right'"
          :hide-modal-footer="block.props?.hideModalFooter === true"
          :hide-default-detail-content="block.props?.hideDefaultDetailContent === true"
          :hide-toolbar="block.props?.hideToolbar === true"
          :hide-add="block.props?.hideAdd === true"
          :hide-batch-delete="block.props?.hideBatchDelete === true"
          :show-import="!isStaticCrudPreview && block.props?.showImport !== false"
          :show-export="!isStaticCrudPreview && block.props?.showExport !== false"
          :show-export-tasks="!isStaticCrudPreview && block.props?.showExportTasks !== false"
          :enable-custom-query="!isStaticCrudPreview && block.props?.enableCustomQuery !== false"
          :add-button-text="block.props?.addButtonText || '新增'"
          :export-button-text="block.props?.exportButtonText || '导出'"
          :export-file-name="block.props?.exportFileName || ''"
          :render-mode="block.props?.renderMode || 'table'"
          :show-render-mode-switch="block.props?.showRenderModeSwitch !== false"
          :enable-tree-add-child="block.props?.enableTreeAddChild === true"
          :table-size="block.props?.tableSize || 'medium'"
          :table-row-gap="normalizeTableRowGap(block.props?.rowGap, 8)"
          :bordered="!!block.props?.bordered"
          :striped="!!block.props?.striped"
          :empty-title="block.props?.emptyTitle || ''"
          :empty-description="block.props?.emptyDescription || block.props?.emptyText || ''"
          :empty-text="block.props?.emptyText || block.props?.emptyDescription || ''"
          :hide-selection="block.props?.hideSelection === true"
          :max-height="block.props?.maxHeight || undefined"
          :scroll-x="block.props?.scrollX || undefined"
          :list-method="block.props?.listMethod || 'get'"
          :list-data-field="block.props?.listDataField || 'records'"
          :list-total-field="block.props?.listTotalField || 'total'"
          :is-encrypt="block.props?.isEncrypt === true"
          :public-params="designerCrudPublicParams"
          :public-query="block.props?.publicQuery || {}"
          :form-default-values="block.props?.formDefaultValues || {}"
          :submit-default-params="block.props?.submitDefaultParams || {}"
          v-bind="{ ...resolvedDesignerCrudHookHandlers, ...crudPagePresentation }"
          @load-list-success="handleCrudPreviewSuccess"
          @load-list-error="handleCrudPreviewError"
        />
        <div v-if="isStaticCrudPreview" class="ai-crud-preview-static-tip">
          {{ staticCrudPreviewMessage }}
        </div>
      </div>
    </template>

    <!-- 系统 AiTable 组件 -->
    <template v-else-if="block.blockType === 'AiTable'">
      <div class="system-component-preview">
        <AiTable
          :columns="aiTableColumns"
          :data-source="sampleRows"
          :pagination="block.props?.showPagination ? previewPagination : false"
          :show-toolbar="block.props?.showToolbar !== false"
          :show-refresh="block.props?.showRefresh === true"
          :show-density="block.props?.showDensity !== false"
          :show-column-filter="block.props?.showColumnFilter !== false"
          :show-search-toggle="block.props?.showSearchToggle === true"
          :show-fullscreen="block.props?.showFullscreen === true"
          :show-render-mode-switch="block.props?.showRenderModeSwitch !== false"
          :size="block.props?.size || 'small'"
          :table-row-gap="normalizeTableRowGap(block.props?.rowGap, 8)"
          :render-mode="block.props?.renderMode || 'table'"
          :row-key="block.props?.rowKey || 'id'"
          :bordered="block.props?.bordered !== false"
          :striped="!!block.props?.striped"
          :single-line="!!block.props?.singleLine"
          :max-height="block.props?.maxHeight || undefined"
          :scroll-x="block.props?.scrollX || undefined"
          :hide-selection="block.props?.hideSelection !== false"
        />
      </div>
    </template>

    <!-- 系统 AiForm 组件 -->
    <template v-else-if="block.blockType === 'AiForm'">
      <div class="system-component-preview is-form-grow">
        <AiForm
          ref="aiFormRef"
          v-model:value="previewFormValue"
          :schema="aiFormSchema"
          :grid-cols="block.props?.gridCols || 2"
          :label-placement="block.props?.labelPlacement || 'left'"
          :label-width="block.props?.labelWidth || 100"
          :label-align="block.props?.labelAlign || 'right'"
          :x-gap="block.props?.xGap ?? 12"
          :y-gap="block.props?.yGap ?? 12"
          :size="block.props?.size || 'medium'"
          :show-actions="block.props?.showActions !== false"
          :show-submit="block.props?.showSubmit !== false && runtimeInteractive"
          :submit-loading="aiFormSubmitting"
          :show-reset="block.props?.showReset !== false"
          :show-cancel="block.props?.showCancel === true"
          :submit-text="block.props?.submitText || '提交'"
          :reset-text="block.props?.resetText || '重置'"
          :cancel-text="block.props?.cancelText || '取消'"
          :enable-collapse="block.props?.enableCollapse === true"
          :max-visible-fields="block.props?.maxVisibleFields ?? 40"
          :show-feedback="block.props?.showFeedback !== false"
          @update:value="handleStandaloneAiFormValueUpdate"
          @submit="handleAiFormSubmit"
        />
      </div>
    </template>

    <!-- 筛选树：有树 API 就加载真实数据（设计态/预览/运行态），不再仅限 readonly -->
    <template v-else-if="block.blockType === 'tree-panel'">
      <div class="tree-preview" :class="{ 'is-runtime': hasRuntimeTreeApi, 'is-panel-collapsed': treePanelCollapsed }">
        <div
          v-if="treePanelCollapsed"
          class="tree-panel-rail"
          :title="block.props?.treeTitle || '筛选树'"
        >
          <button
            type="button"
            class="tree-panel-edge-toggle is-collapsed"
            :aria-label="'展开筛选树'"
            title="展开筛选树"
            @click.stop="toggleTreePanel"
          >
            <span>›</span>
          </button>
          <span class="tree-panel-rail__label">树</span>
        </div>
        <template v-else>
          <div class="tree-preview-head">
            <div class="tree-preview-head__meta">
              <strong>{{ block.props?.treeTitle || '筛选树' }}</strong>
              <span>{{ block.props?.sourceModelName || '按树节点筛选右侧列表' }}</span>
            </div>
            <div class="tree-preview-head__actions">
              <small>{{ block.props?.loadMode === 'lazy' ? '懒加载' : '全量' }}</small>
              <button
                type="button"
                class="tree-panel-edge-toggle"
                aria-label="收起筛选树"
                title="收起筛选树"
                @click.stop="toggleTreePanel"
              >
                <span>‹</span>
              </button>
            </div>
          </div>
          <div class="tree-node-toolbar">
            <span>节点层级</span>
            <div class="tree-expand-actions">
              <button type="button" @click.stop="expandTree">
                展开
              </button>
              <button type="button" @click.stop="collapseTree">
                收起
              </button>
            </div>
          </div>
          <template v-if="hasRuntimeTreeApi">
            <button
              type="button"
              class="tree-node tree-node-button"
              :class="{ active: runtimeTreeActiveKey === '__all__' }"
              @click="clearRuntimeTreeSelection"
            >
              <span class="tree-node-dot" />
              <span>全部</span>
              <small>{{ runtimeTreeTotal }}</small>
            </button>
            <n-spin :show="runtimeTreeLoading" size="small">
              <n-tree
                v-if="runtimeTreeNodes.length"
                block-line
                :data="runtimeTreeNodes"
                :selected-keys="runtimeSelectedTreeKeys"
                key-field="key"
                label-field="label"
                :children-field="runtimeTreeChildrenField"
                :on-load="runtimeTreeLoadMode === 'lazy' ? loadRuntimeTreeChildren : undefined"
                :expanded-keys="runtimeExpandedTreeKeys"
                @update:expanded-keys="runtimeExpandedTreeKeys = $event"
                @update:selected-keys="handleRuntimeTreeSelected"
              />
              <div v-else class="tree-empty">
                {{ runtimeTreeLoading ? '加载中...' : (runtimeTreeError || '暂无树节点，请检查树数据来源与字段配置') }}
              </div>
            </n-spin>
          </template>
          <div v-else class="tree-empty tree-empty-hint">
            请先在右侧选择「树数据来源」对象，并配置主键/父级/显示字段
          </div>
          <div class="tree-foot">
            点击树节点后，右侧列表会自动按对应字段筛选
          </div>
        </template>
      </div>
    </template>

    <!-- 指标卡片 -->
    <template v-else-if="block.blockType === 'stats-strip'">
      <StatsStripBlock
        :metrics="statsMetrics"
        :columns="block.props?.columns || 4"
        :compact="block.props?.compact === true"
        :show-icon="block.props?.showIcon !== false"
        :show-trend="block.props?.showTrend !== false && isBlockSlotVisible('metaField')"
        :interactive="runtimeInteractive"
        @select="handleWidgetLinkSelect"
      />
    </template>

    <!-- 快捷入口 -->
    <template v-else-if="block.blockType === 'quick-entry'">
      <QuickEntryBlock
        :items="quickEntryItems"
        :columns="block.props?.columns || 3"
        :variant="block.props?.variant || 'card'"
        :show-description="block.props?.showDescription !== false"
        :interactive="runtimeInteractive"
        @select="handleWidgetLinkSelect"
      />
    </template>

    <!-- 柱状图 / 折线图 / 面积图 / 饼图 -->
    <template v-else-if="['bar-chart', 'line-chart', 'area-chart', 'pie-chart'].includes(block.blockType)">
      <PageChartBlock
        :chart-type="block.blockType"
        :title="block.props?.title || ''"
        :categories="chartSeriesPayload.categories"
        :series="chartSeriesPayload.series"
        :height="resolveChartBlockHeight(block)"
        :show-legend="block.props?.showLegend !== false"
        :smooth="block.props?.smooth !== false"
        :show-area="block.props?.showArea === true || block.blockType === 'area-chart'"
        :colors="block.props?.colors || []"
        :legend-position="block.props?.legendPosition || 'top-right'"
        :bar-max-width="block.props?.barMaxWidth || 36"
        :category-label-rotate="block.props?.categoryLabelRotate || 0"
        :y-axis-name="block.props?.yAxisName || ''"
        :show-split-line="block.props?.showSplitLine !== false"
        :symbol-size="block.props?.symbolSize || 6"
        :area-opacity="block.props?.areaOpacity ?? (block.blockType === 'area-chart' ? 0.22 : 0.12)"
        :grid-left="block.props?.gridLeft ?? 8"
        :grid-right="block.props?.gridRight ?? 12"
        :grid-bottom="block.props?.gridBottom ?? 8"
        :doughnut="block.props?.doughnut === true || block.props?.pieVariant === 'doughnut'"
        :pie-variant="block.props?.pieVariant || 'pie'"
        :inner-radius="block.props?.innerRadius ?? 48"
        :outer-radius="block.props?.outerRadius ?? 68"
        :rose-type="block.props?.roseType || 'none'"
        :show-label="block.props?.showLabel !== false"
        :show-percent="block.props?.showPercent !== false"
        :pie-border-radius="block.props?.pieBorderRadius ?? 4"
        :pie-border-width="block.props?.pieBorderWidth ?? 2"
        :option-overrides="block.props?.optionOverrides || null"
        :data-locked="true"
      />
    </template>

    <!-- 待办 / 已办列表 -->
    <template v-else-if="block.blockType === 'workflow-task-list'">
      <WorkflowTaskListBlock
        :title="block.props?.title || '我的待办/已办'"
        :subtitle="block.props?.subtitle || ''"
        :mode="block.props?.mode || 'both'"
        :page-size="block.props?.pageSize || 6"
        :compact="block.props?.compact === true"
        :show-header="block.props?.showHeader !== false"
        :show-tabs="block.props?.showTabs !== false"
        :show-search="block.props?.showSearch === true"
        :show-status="block.props?.showStatus !== false"
        :show-node="block.props?.showNode !== false"
        :show-applicant="block.props?.showApplicant !== false"
        :show-time="block.props?.showTime !== false"
        :show-refresh="block.props?.showRefresh !== false"
        :show-more-link="block.props?.showMoreLink !== false"
        :accent-color="block.props?.accentColor || '#2563eb'"
        :empty-text="block.props?.emptyText || ''"
        :todo-path="block.props?.todoPath || '/flow/todo'"
        :done-path="block.props?.donePath || '/flow/done'"
        :preview-items="block.props?.previewItems || []"
        :interactive="runtimeInteractive"
        :auto-load="runtimeInteractive"
      />
    </template>

    <!-- 通知公告 -->
    <template v-else-if="block.blockType === 'notice-board'">
      <NoticeBoardBlock
        :title="block.props?.title || '通知公告'"
        :subtitle="block.props?.subtitle || ''"
        :page-size="block.props?.pageSize || 6"
        :compact="block.props?.compact === true"
        :variant="block.props?.variant || 'list'"
        :density="block.props?.density || 'comfortable'"
        :show-header="block.props?.showHeader !== false"
        :show-filter-tabs="block.props?.showFilterTabs !== false"
        :show-unread-badge="block.props?.showUnreadBadge !== false"
        :show-unread-dot="block.props?.showUnreadDot !== false"
        :show-type-tag="block.props?.showTypeTag !== false"
        :show-summary="block.props?.showSummary !== false"
        :show-sender="block.props?.showSender === true"
        :show-time="block.props?.showTime !== false"
        :show-refresh="block.props?.showRefresh !== false"
        :show-mark-all="block.props?.showMarkAll !== false"
        :show-more-link="block.props?.showMoreLink !== false"
        :accent-color="block.props?.accentColor || '#2563eb'"
        :unread-only="block.props?.unreadOnly === true"
        :empty-text="block.props?.emptyText || ''"
        :more-path="block.props?.morePath || '/message'"
        :mark-read-on-open="block.props?.markReadOnOpen !== false"
        :preview-items="block.props?.previewItems || []"
        :interactive="runtimeInteractive"
        :auto-load="runtimeInteractive"
      />
    </template>

    <!-- 欢迎条 -->
    <template v-else-if="block.blockType === 'welcome-banner'">
      <WelcomeBannerBlock
        :title="block.props?.title || ''"
        :subtitle="block.props?.subtitle || ''"
        :greeting-prefix="block.props?.greetingPrefix || ''"
        :tone="block.props?.tone || 'primary'"
        :compact="block.props?.compact === true"
        :show-avatar="block.props?.showAvatar !== false"
        :show-date="block.props?.showDate !== false"
        :show-cta="block.props?.showCta !== false"
        :cta-text="block.props?.ctaText || '去办理'"
        :cta-path="block.props?.ctaPath || '/flow/todo'"
        :background-mode="block.props?.backgroundMode || 'gradient'"
        :background-color="block.props?.backgroundColor || '#eff6ff'"
        :gradient-from="block.props?.gradientFrom || '#eff6ff'"
        :gradient-to="block.props?.gradientTo || '#ffffff'"
        :gradient-angle="block.props?.gradientAngle ?? 135"
        :background-image="block.props?.backgroundImage || ''"
        :background-size="block.props?.backgroundSize || 'cover'"
        :interactive="runtimeInteractive"
      />
    </template>

    <!-- 工作台统计（待办/已办/发起中/抄送） -->
    <template v-else-if="block.blockType === 'workspace-summary-metrics'">
      <WorkspaceSummaryMetrics
        :route-targets="block.props?.routeTargets || {}"
        :visible-keys="block.props?.visibleKeys || ['todo', 'done', 'started', 'cc']"
        :columns="Number(block.props?.columns || 4)"
        :compact="block.props?.compact === true"
        :readonly="!runtimeInteractive"
        :auto-load="true"
      />
    </template>

    <!-- 提示面板 -->
    <template v-else-if="block.blockType === 'info-panel'">
      <InfoPanelBlock
        :title="isBlockSlotVisible('titleField') ? (boundInfoTitle || '提示信息') : ''"
        :content="isBlockSlotVisible('contentField') ? (boundInfoContent || '在右侧填写提示内容') : ''"
        :type="boundInfoType || 'info'"
        :icon="block.props?.icon || ''"
        :color="block.props?.color || ''"
        :show-icon="block.props?.showIcon !== false"
        :link-type="block.props?.linkType || 'none'"
        :target-page-id="block.props?.targetPageId || ''"
        :href="block.props?.href || ''"
        :interactive="runtimeInteractive"
        @select="handleWidgetLinkSelect"
      />
    </template>

    <!-- 说明文本 -->
    <template v-else-if="block.blockType === 'custom-html'">
      <div class="custom-html" :style="textContentStyle">
        <div v-if="boundCustomTitle" class="custom-title" :style="textContentStyle">
          {{ boundCustomTitle }}
        </div>
        <div class="custom-body" :style="textContentStyle">
          {{ boundCustomContent || '在右侧填写说明内容' }}
        </div>
      </div>
    </template>

    <!-- 单按钮 -->
    <template v-else-if="block.blockType === 'action-button'">
      <div v-if="actionButtonVisible" class="action-button-preview" :class="{ 'is-block': !!block.props?.block }">
        <n-button
          :type="block.props?.type === 'default' ? undefined : block.props?.type"
          :secondary="!!block.props?.secondary"
          :tertiary="!!block.props?.tertiary"
          :quaternary="!!block.props?.quaternary"
          :dashed="!!block.props?.dashed"
          :round="!!block.props?.round"
          :block="!!block.props?.block"
          :disabled="!!block.props?.disabled"
          :loading="!!block.props?.loading"
          :size="block.props?.size || 'small'"
          @click.stop="handleActionButtonClick"
        >
          <template v-if="block.props?.icon" #icon>
            <IconRenderer :icon="block.props.icon" :size="14" />
          </template>
          {{ block.props?.text || block.props?.title || block.label || '操作' }}
        </n-button>
      </div>
      <div v-else class="block-empty">
        当前权限或显示条件不满足
      </div>
    </template>

    <!-- 按钮组：设计态禁用避免误点；运行态可点并走区块 click 事件 -->
    <template v-else-if="block.blockType === 'button-group'">
      <div class="button-group-preview">
        <n-button
          v-for="button in (block.props?.buttons || [])"
          :key="button.key || button.text"
          :type="button.type === 'default' ? undefined : button.type"
          size="small"
          :disabled="!readonly || button.disabled === true"
          @click.stop="handleButtonGroupClick(button)"
        >
          {{ button.text || button.label || '按钮' }}
        </n-button>
      </div>
    </template>

    <!-- 标签列表 -->
    <template v-else-if="block.blockType === 'tag-list'">
      <div class="tag-list-preview">
        <n-tag
          v-for="tag in boundTags"
          :key="tag.label"
          :type="tag.type || 'default'"
          size="small"
          :bordered="false"
        >
          {{ tag.label }}
        </n-tag>
      </div>
    </template>

    <!-- 步骤条 -->
    <template v-else-if="block.blockType === 'steps'">
      <n-steps size="small" :current="Number(block.props?.current || 1)" class="steps-preview">
        <n-step
          v-for="step in boundSteps"
          :key="step.title"
          :title="step.title"
          :description="step.description"
        />
      </n-steps>
    </template>

    <!-- 时间线 -->
    <template v-else-if="block.blockType === 'timeline'">
      <div class="timeline-preview">
        <div v-if="boundTimelineTitle" class="custom-title">
          {{ boundTimelineTitle }}
        </div>
        <div
          v-for="item in boundTimelineItems"
          :key="`${item.title}-${item.time}`"
          class="timeline-item"
        >
          <span class="timeline-dot" />
          <div>
            <strong>{{ item.title }}</strong>
            <small>{{ item.time }}</small>
            <p>{{ item.content }}</p>
          </div>
        </div>
      </div>
    </template>

    <!-- 空状态 -->
    <template v-else-if="block.blockType === 'empty-state'">
      <div class="empty-state-preview">
        <div class="empty-state-icon">
          ∅
        </div>
        <strong>{{ boundEmptyTitle || '暂无数据' }}</strong>
        <span>{{ boundEmptyDescription || '当前没有可展示的数据' }}</span>
        <n-button v-if="boundEmptyActionText" size="small" secondary disabled>
          {{ boundEmptyActionText }}
        </n-button>
      </div>
    </template>

    <template v-else-if="pageWidgetComponentKeys.includes(block.blockType)">
      <PageWidgetRenderer
        :component-key="block.blockType"
        :props-data="block.props || {}"
        :data-context="runtimeRecord || {}"
        :readonly="readonly"
        @update:props-data="emit('blockPropsUpdate', { blockId: block.id, propsData: $event })"
      />
    </template>

    <!-- 手写签名 -->
    <template v-else-if="block.blockType === 'signature-pad'">
      <div class="signature-block-preview">
        <div class="block-header">
          <strong>{{ block.props?.title || '手写签名' }}</strong>
          <span class="block-meta">{{ block.props?.required ? '必填' : '可选' }}</span>
        </div>
        <SignaturePad
          v-model="signaturePreviewValue"
          :height="Number(block.props?.height || 160)"
          :stroke-width="Number(block.props?.strokeWidth || 2.6)"
          :disabled="readonly || block.props?.disabled === true"
          :business-type="block.props?.businessType || 'lowcode_signature'"
        />
      </div>
    </template>

    <!-- 穿梭框 -->
    <template v-else-if="block.blockType === 'transfer'">
      <div class="transfer-preview">
        <div class="block-header">
          <strong>{{ block.props?.title || '穿梭框' }}</strong>
          <span class="block-meta">{{ transferSelectedOptions.length }}/{{ transferOptions.length }}</span>
        </div>
        <n-transfer
          :value="transferValue"
          :options="transferOptions"
          :source-title="block.props?.sourceTitle || '可选项'"
          :target-title="block.props?.targetTitle || '已选项'"
          :filterable="block.props?.filterable !== false"
          :virtual-scroll="block.props?.virtualScroll === true"
          disabled
          size="small"
        />
      </div>
    </template>

    <!-- 分步表单 -->
    <template v-else-if="block.blockType === 'step-form'">
      <div class="step-form-preview">
        <div class="block-header">
          <strong>{{ block.props?.title || '分步表单' }}</strong>
          <span class="block-meta">第 {{ block.props?.current || 1 }} 步</span>
        </div>
        <n-steps size="small" :current="Number(block.props?.current || 1)" :vertical="block.props?.direction === 'vertical'">
          <n-step
            v-for="step in (block.props?.steps || [])"
            :key="step.title"
            :title="step.title"
            :description="step.description"
          />
        </n-steps>
        <div class="step-form-fields">
          <div v-for="field in resolvedFields.slice(0, 4)" :key="field.field" class="step-form-field">
            <span>{{ field.label || field.field }}</span>
            <em>待填写</em>
          </div>
        </div>
      </div>
    </template>

    <!-- 标题 -->
    <template v-else-if="block.blockType === 'text-title'">
      <div class="text-title-preview" :style="textTitleStyle">
        <template v-if="isBlockSlotVisible('titleField')">{{ boundTextTitle || '页面标题' }}</template>
        <small v-if="isBlockSlotVisible('descriptionField') && boundTextSubtitle">{{ boundTextSubtitle }}</small>
      </div>
    </template>

    <!-- 段落 -->
    <template v-else-if="block.blockType === 'paragraph'">
      <component :is="paragraphListTag" v-if="paragraphListTag" class="paragraph-preview paragraph-list-preview" :style="paragraphStyle">
        <li v-for="(line, index) in paragraphLines" :key="`${line}-${index}`">
          {{ line }}
        </li>
      </component>
      <blockquote v-else-if="block.props?.quote" class="paragraph-preview paragraph-quote-preview" :style="paragraphStyle">
        {{ boundParagraphContent || '段落内容' }}
      </blockquote>
      <p v-else class="paragraph-preview" :style="paragraphStyle">
        {{ boundParagraphContent || '段落内容' }}
      </p>
    </template>

    <!-- 统计数值 -->
    <template v-else-if="block.blockType === 'statistic'">
      <div class="single-stat-preview" :style="{ '--stat-color': block.props?.color || '#2563eb' }">
        <span v-if="isBlockSlotVisible('titleField')">{{ boundStatisticTitle || '统计指标' }}</span>
        <strong v-if="isBlockSlotVisible('valueField')">{{ block.props?.prefix }}{{ boundStatisticValue || '0' }}{{ block.props?.suffix }}</strong>
        <small v-if="isBlockSlotVisible('metaField') || isBlockSlotVisible('descriptionField')">
          <template v-if="isBlockSlotVisible('metaField')">{{ boundStatisticTrend || '' }}</template>
          <template v-if="isBlockSlotVisible('descriptionField')">{{ boundStatisticDescription || '' }}</template>
        </small>
      </div>
    </template>

    <!-- 链接：设计态拦截点击避免跳离画布；预览/发布态按 href/target 正常跳转 -->
    <template v-else-if="block.blockType === 'link'">
      <a
        class="link-preview"
        :class="`type-${block.props?.type || 'primary'}`"
        :href="boundLinkHref || '#'"
        :target="block.props?.target || '_self'"
        @click="handleLinkClick"
      >
        {{ boundLinkText || '链接文本' }}
      </a>
    </template>

    <!-- 文字提示 -->
    <template v-else-if="block.blockType === 'text-tip'">
      <div class="text-tip-preview" :class="`type-${boundTextTipType || 'info'}`">
        <span v-if="block.props?.showIcon !== false" class="tip-icon">i</span>
        <div :style="textContentStyle">
          <strong>{{ boundTextTipTitle || '提示' }}</strong>
          <p>{{ boundTextTipContent || '提示内容' }}</p>
        </div>
      </div>
    </template>

    <!-- 水印 -->
    <template v-else-if="block.blockType === 'watermark'">
      <n-watermark class="watermark-preview" v-bind="watermarkProps">
        <div class="watermark-preview-inner">
          <strong>{{ block.props?.previewText || '水印覆盖区域' }}</strong>
          <span>{{ block.props?.content || '水印文字' }}</span>
        </div>
      </n-watermark>
    </template>

    <!-- 音频播放器 -->
    <template v-else-if="block.blockType === 'audio-player'">
      <div class="media-preview audio-preview">
        <strong>{{ boundMediaTitle || '音频播放器' }}</strong>
        <audio :src="boundMediaSrc || undefined" :controls="block.props?.controls !== false" :autoplay="false" :loop="block.props?.loop === true" />
        <span v-if="!boundMediaSrc">未配置音频地址</span>
      </div>
    </template>

    <!-- 视频播放器 -->
    <template v-else-if="block.blockType === 'video-player'">
      <div class="media-preview video-preview">
        <video :src="boundMediaSrc || undefined" :poster="boundVideoPoster || undefined" :controls="block.props?.controls !== false" :autoplay="false" :loop="block.props?.loop === true" :muted="block.props?.muted === true" />
        <span v-if="!boundMediaSrc">{{ boundMediaTitle || '视频播放器' }} · 未配置视频地址</span>
      </div>
    </template>

    <!-- 头像框 -->
    <template v-else-if="block.blockType === 'avatar'">
      <div class="avatar-preview">
        <n-avatar :src="boundAvatarSrc || undefined" :size="Number(block.props?.size || 48)" :round="block.props?.shape !== 'square'">
          {{ avatarInitial }}
        </n-avatar>
        <div v-if="block.props?.showInfo !== false">
          <strong>{{ boundAvatarName || '用户名称' }}</strong>
          <span>{{ boundAvatarDescription || '角色 / 部门' }}</span>
        </div>
      </div>
    </template>

    <!-- 条形码 -->
    <template v-else-if="block.blockType === 'barcode'">
      <div class="barcode-preview" :style="{ background: block.props?.background || 'var(--bg-primary)' }">
        <svg ref="barcodeSvgRef" class="barcode-svg" />
        <span v-if="!barcodeValid" class="code-invalid">条形码内容无效</span>
      </div>
    </template>

    <!-- 内嵌页面 -->
    <template v-else-if="block.blockType === 'iframe'">
      <div class="iframe-preview">
        <iframe
          v-if="safeIframeSrc"
          :src="safeIframeSrc"
          :title="block.props?.title || '内嵌页面'"
          :sandbox="block.props?.sandbox || 'allow-same-origin allow-forms'"
          :loading="block.props?.loading || 'lazy'"
          :allowfullscreen="block.props?.allowFullscreen === true"
        />
        <div v-else class="block-empty">
          请配置 http(s) 内嵌页面地址
        </div>
      </div>
    </template>

    <!-- 二维码 -->
    <template v-else-if="block.blockType === 'qrcode'">
      <div class="qrcode-preview">
        <QRCodeVue3
          :key="`${block.id}_${block.props?.value || ''}`"
          :width="Number(block.props?.size || 132)"
          :height="Number(block.props?.size || 132)"
          :value="String(block.props?.value || 'https://forge.local')"
          :margin="Number(block.props?.margin || 0)"
          :qr-options="qrcodeQrOptions"
          :dots-options="qrcodeDotsOptions"
          :background-options="qrcodeBackgroundOptions"
          :corners-square-options="qrcodeCornersSquareOptions"
          :corners-dot-options="qrcodeCornersDotOptions"
        />
        <strong v-if="block.props?.title">{{ block.props.title }}</strong>
        <small v-if="block.props?.showText !== false">{{ block.props?.value }}</small>
      </div>
    </template>

    <!-- 盒子布局 -->
    <template v-else-if="block.blockType === 'box-layout'">
      <div
        class="box-layout-preview"
        :class="{ 'is-drop-active': isActiveDropContainer }"
        :style="boxLayoutStyle"
        :data-page-container-id="block.id"
        data-page-container-type="box-layout"
        :data-grid-container-id="block.id"
        data-grid-cell-key="__body__"
        @dragover.prevent.stop="handleContainerDragOver"
        @dragenter.prevent.stop="handleContainerDragEnter"
        @drop.prevent.stop="handleContainerDrop"
        @contextmenu.prevent.stop="!readonly && handleContainerSlotContextMenu($event, { title: '插入到盒子' })"
      >
        <div
          v-if="!readonly && catalogDragBlockType"
          class="container-catalog-drop-layer"
          :class="{ 'is-active': isActiveDropContainer }"
          :data-grid-container-id="block.id"
          data-grid-cell-key="__body__"
          :data-page-container-id="block.id"
          data-page-container-type="box-layout"
        />
        <div
          v-if="!readonly && !(block.children || []).length && !isActiveDropContainer"
          class="container-drop-empty"
        >
          拖入组件到此盒子 · 或右键插入
        </div>
        <div
          v-for="child in (block.children || [])"
          :key="child.id"
          class="container-child-item"
          :class="{
            selected: !readonly && child.id === selectedBlockId,
            'is-moving-source': child.id === nestedMovingBlockId,
            'is-fill-height': isNestedFillHeightChild(child),
          }"
          :style="nestedChildShellStyle(child)"
          :data-grid-child-id="child.id"
          @pointerenter="onNestedPointerEnter(child.id)"
          @pointerleave="onNestedPointerLeave(child.id)"
          @click.stop="emit('childBlockSelect', child.id)"
          @contextmenu.prevent.stop="!readonly && handleChildContextMenu($event, child, { title: '盒子内组件' })"
        >
          <div v-if="showNestedChrome(child.id)" class="nested-block-node-overlay">
            <span
              class="nested-block-drag-handle"
              title="拖动组件"
              @click.stop
              @pointerdown.stop.prevent="emit('childBlockMoveStart', { block: child, event: $event })"
            >
              <svg width="1em" height="1em" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                <path d="M8.25 6.5a1.75 1.75 0 1 0 0-3.5 1.75 1.75 0 0 0 0 3.5Zm0 7.25a1.75 1.75 0 1 0 0-3.5 1.75 1.75 0 0 0 0 3.5Zm1.75 5.5a1.75 1.75 0 1 1-3.5 0 1.75 1.75 0 0 1 0 3.5ZM14.753 6.5a1.75 1.75 0 1 0 0-3.5 1.75 1.75 0 0 0 0 3.5ZM16.5 12a1.75 1.75 0 1 1-3.5 0 1.75 1.75 0 0 1 3.5 0Zm-1.747 9a1.75 1.75 0 1 0 0-3.5 1.75 1.75 0 0 0 0 3.5Z" fill="currentColor" />
              </svg>
            </span>
            <n-dropdown
              trigger="click"
              placement="bottom-end"
              :options="nestedBlockMenuOptions"
              @select="key => emit('childBlockMenuSelect', { key, block: child })"
            >
              <button type="button" class="nested-block-menu-trigger" title="更多操作" @click.stop @mousedown.stop>
                <svg width="1em" height="1em" viewBox="0 0 512 512" aria-hidden="true">
                  <circle cx="256" cy="256" r="32" fill="currentColor" />
                  <circle cx="416" cy="256" r="32" fill="currentColor" />
                  <circle cx="96" cy="256" r="32" fill="currentColor" />
                </svg>
              </button>
            </n-dropdown>
          </div>
          <GridBlockRenderer
            :block="child"
            :fields="resolveNestedBlockFields(child)"
            :selected="false"
            :selected-block-id="selectedBlockId"
            :readonly="readonly"
            :runtime-crud-props="resolveNestedBlockRuntimeCrudProps(child)"
            :runtime-crud-loading="resolveNestedBlockRuntimeCrudLoading(child)"
            :show-data-source-guide="showDataSourceGuide"
            :data-source-configured="resolveNestedBlockDataSourceConfigured(child)"
            :runtime-interactive="runtimeInteractive"
            :runtime-extension-hooks="runtimeExtensionHooks"
            :runtime-record="runtimeRecord"
            :active-drop-cell="activeDropCell"
            :active-drop-container="activeDropContainer"
            :nested-moving-block-id="nestedMovingBlockId"
            :catalog-drag-block-type="catalogDragBlockType"
            :block-fields-resolver="blockFieldsResolver"
            :runtime-crud-props-resolver="runtimeCrudPropsResolver"
            :runtime-crud-loading-resolver="runtimeCrudLoadingResolver"
            :data-source-configured-resolver="dataSourceConfiguredResolver"
            :runtime-tree-active-key="runtimeTreeActiveKey"
            @child-block-select="emit('childBlockSelect', $event)"
            @child-block-menu-select="emit('childBlockMenuSelect', $event)"
            @block-props-update="emit('blockPropsUpdate', $event)"
            @tabs-active-change="emit('tabsActiveChange', $event)"
            @tab-drop="emit('tabDrop', $event)"
            @grid-cell-drop="emit('gridCellDrop', $event)"
            @container-insert="emit('containerInsert', $event)"
            @container-clear="emit('containerClear', $event)"
            @child-block-move-start="emit('childBlockMoveStart', $event)"
            @child-block-drag-end="emit('childBlockDragEnd')"
            @child-block-resize-start="emit('childBlockResizeStart', $event)"
            @runtime-tree-select="emit('runtimeTreeSelect', $event)"
            @request-data-source="emit('requestDataSource', $event)"
          />
          <template v-if="!readonly && child.id === selectedBlockId">
            <button
              v-for="anchor in resizeAnchors"
              :key="anchor"
              type="button"
              class="nested-resize-anchor"
              :class="`anchor-${anchor}`"
              title="调整组件大小"
              @pointerdown.stop="emit('childBlockResizeStart', { block: child, event: $event, anchor })"
            />
          </template>
        </div>
        <div v-if="!readonly && isActiveDropContainer" class="layout-grid-cell-drop-preview">
          释放到盒子
        </div>
      </div>
    </template>

    <!-- 间距 -->
    <template v-else-if="block.blockType === 'space'">
      <div class="space-preview" :class="block.props?.direction === 'horizontal' ? 'horizontal' : 'vertical'" :style="spacePreviewStyle">
        <span v-if="block.props?.lineVisible" />
      </div>
    </template>

    <!-- 描述列表 -->
    <template v-else-if="block.blockType === 'descriptions'">
      <div class="descriptions-preview">
        <div v-if="block.props?.title" class="custom-title">
          {{ block.props.title }}
        </div>
        <div class="description-grid" :class="{ bordered: block.props?.bordered }" :style="descriptionGridStyle">
          <div v-for="item in (block.props?.items || [])" :key="item.label" class="description-item">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </div>
        </div>
      </div>
    </template>

    <!-- 子表 Tab -->
    <template v-else-if="block.blockType === 'sub-table-tabs'">
      <n-tabs type="line" size="small" :default-value="block.props?.tabs?.[0]?.key" class="sub-tabs">
        <n-tab-pane
          v-for="tab in (block.props?.tabs || [])"
          :key="tab.key"
          :name="tab.key"
          :tab="tab.title"
        >
          <div class="sub-tab-empty">
            子表内容由发布运行时联动加载
          </div>
        </n-tab-pane>
      </n-tabs>
    </template>

    <!-- 分组标题 -->
    <template v-else-if="block.blockType === 'section-divider'">
      <div class="section-divider">
        <span class="bar" />
        <span class="divider-title">{{ block.props?.title || '分组标题' }}</span>
        <span class="bar" />
      </div>
    </template>

    <!-- 通用分隔线 -->
    <template v-else-if="block.blockType === 'divider'">
      <div class="layout-divider" :class="block.props?.orientation === 'vertical' ? 'vertical' : 'horizontal'">
        <span class="line" />
        <span v-if="block.props?.title" class="divider-title">{{ block.props.title }}</span>
        <span v-if="block.props?.title" class="line" />
      </div>
    </template>

    <!-- 卡片容器 -->
    <template v-else-if="block.blockType === 'card'">
      <div
        class="layout-card"
        :class="{
          'layout-card--small': block.props?.size === 'small',
          'layout-card--large': block.props?.size === 'large',
          'layout-card--huge': block.props?.size === 'huge',
          'layout-card--borderless': block.props?.bordered === false,
          'layout-card--embedded': block.props?.embedded,
          'layout-card--hoverable': block.props?.hoverable,
          'layout-card--accent': block.props?.showTitleAccent !== false && !!block.props?.title,
          'is-drop-active': isActiveDropContainer,
        }"
        :style="layoutCardStyle"
        :data-page-container-id="block.id"
        data-page-container-type="card"
        :data-grid-container-id="block.id"
        data-grid-cell-key="__body__"
        @dragover.prevent.stop="handleContainerDragOver"
        @dragenter.prevent.stop="handleContainerDragEnter"
        @drop.prevent.stop="handleContainerDrop"
        @contextmenu.prevent.stop="!readonly && handleContainerSlotContextMenu($event, { title: '插入到卡片' })"
      >
        <div
          v-if="!readonly && catalogDragBlockType"
          class="container-catalog-drop-layer"
          :class="{ 'is-active': isActiveDropContainer }"
          :data-grid-container-id="block.id"
          data-grid-cell-key="__body__"
          :data-page-container-id="block.id"
          data-page-container-type="card"
        />
        <div
          v-if="block.props?.title"
          class="layout-card-title"
          title="选中卡片"
          @click.stop="emit('childBlockSelect', block.id)"
        >
          {{ block.props.title }}
        </div>
        <div class="layout-card-body">
          <div v-if="block.props?.content" class="layout-card-text">
            {{ block.props.content }}
          </div>
          <div
            v-if="!readonly && !(block.children || []).length && !isActiveDropContainer && !block.props?.content"
            class="container-drop-empty"
          >
            拖入组件到此卡片 · 或右键插入
          </div>
          <div v-if="block.children?.length" class="container-child-list">
            <div
              v-for="child in block.children"
              :key="child.id"
              class="container-child-item"
              :class="{
                selected: !readonly && child.id === selectedBlockId,
                'is-moving-source': child.id === nestedMovingBlockId,
                'is-fill-height': isNestedFillHeightChild(child),
              }"
              :style="nestedChildShellStyle(child)"
              :data-grid-child-id="child.id"
              @pointerenter="onNestedPointerEnter(child.id)"
              @pointerleave="onNestedPointerLeave(child.id)"
              @click.stop="emit('childBlockSelect', child.id)"
              @contextmenu.prevent.stop="!readonly && handleChildContextMenu($event, child, { title: '卡片内组件' })"
            >
              <div v-if="showNestedChrome(child.id)" class="nested-block-node-overlay">
                <span
                  class="nested-block-drag-handle"
                  title="拖动组件"
                  @click.stop
                  @pointerdown.stop.prevent="emit('childBlockMoveStart', { block: child, event: $event })"
                >
                  <svg width="1em" height="1em" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                    <path d="M8.25 6.5a1.75 1.75 0 1 0 0-3.5 1.75 1.75 0 0 0 0 3.5Zm0 7.25a1.75 1.75 0 1 0 0-3.5 1.75 1.75 0 0 0 0 3.5Zm1.75 5.5a1.75 1.75 0 1 1-3.5 0 1.75 1.75 0 0 1 0 3.5ZM14.753 6.5a1.75 1.75 0 1 0 0-3.5 1.75 1.75 0 0 0 0 3.5ZM16.5 12a1.75 1.75 0 1 1-3.5 0 1.75 1.75 0 0 1 3.5 0Zm-1.747 9a1.75 1.75 0 1 0 0-3.5 1.75 1.75 0 0 0 0 3.5Z" fill="currentColor" />
                  </svg>
                </span>
                <n-dropdown
                  trigger="click"
                  placement="bottom-end"
                  :options="nestedBlockMenuOptions"
                  @select="key => emit('childBlockMenuSelect', { key, block: child })"
                >
                  <button type="button" class="nested-block-menu-trigger" title="更多操作" @click.stop @mousedown.stop>
                    <svg width="1em" height="1em" viewBox="0 0 512 512" aria-hidden="true">
                      <circle cx="256" cy="256" r="32" fill="currentColor" />
                      <circle cx="416" cy="256" r="32" fill="currentColor" />
                      <circle cx="96" cy="256" r="32" fill="currentColor" />
                    </svg>
                  </button>
                </n-dropdown>
              </div>
              <GridBlockRenderer
                :block="child"
                :fields="resolveNestedBlockFields(child)"
                :selected="false"
                :selected-block-id="selectedBlockId"
                :readonly="readonly"
                :runtime-crud-props="resolveNestedBlockRuntimeCrudProps(child)"
                :runtime-crud-loading="resolveNestedBlockRuntimeCrudLoading(child)"
                :show-data-source-guide="showDataSourceGuide"
                :data-source-configured="resolveNestedBlockDataSourceConfigured(child)"
                :runtime-interactive="runtimeInteractive"
                :runtime-extension-hooks="runtimeExtensionHooks"
                :runtime-record="runtimeRecord"
                :active-drop-cell="activeDropCell"
                :active-drop-container="activeDropContainer"
                :nested-moving-block-id="nestedMovingBlockId"
                :catalog-drag-block-type="catalogDragBlockType"
                :block-fields-resolver="blockFieldsResolver"
                :runtime-crud-props-resolver="runtimeCrudPropsResolver"
                :runtime-crud-loading-resolver="runtimeCrudLoadingResolver"
                :data-source-configured-resolver="dataSourceConfiguredResolver"
                :runtime-tree-active-key="runtimeTreeActiveKey"
                @child-block-select="emit('childBlockSelect', $event)"
                @child-block-menu-select="emit('childBlockMenuSelect', $event)"
                @block-props-update="emit('blockPropsUpdate', $event)"
                @tabs-active-change="emit('tabsActiveChange', $event)"
                @tab-drop="emit('tabDrop', $event)"
                @grid-cell-drop="emit('gridCellDrop', $event)"
                @container-insert="emit('containerInsert', $event)"
                @container-clear="emit('containerClear', $event)"
                @child-block-move-start="emit('childBlockMoveStart', $event)"
                @child-block-drag-end="emit('childBlockDragEnd')"
                @child-block-resize-start="emit('childBlockResizeStart', $event)"
                @runtime-tree-select="emit('runtimeTreeSelect', $event)"
                @request-data-source="emit('requestDataSource', $event)"
              />
              <template v-if="!readonly && child.id === selectedBlockId">
                <button
                  v-for="anchor in resizeAnchors"
                  :key="anchor"
                  type="button"
                  class="nested-resize-anchor"
                  :class="`anchor-${anchor}`"
                  title="调整组件大小"
                  @pointerdown.stop="emit('childBlockResizeStart', { block: child, event: $event, anchor })"
                />
              </template>
            </div>
          </div>
          <div v-if="!readonly && isActiveDropContainer" class="layout-grid-cell-drop-preview">
            释放到卡片
          </div>
        </div>
      </div>
    </template>

    <!-- 面板容器（类 div：背景 / 尺寸 / 可嵌套） -->
    <template v-else-if="block.blockType === 'panel-box'">
      <div
        class="panel-box"
        :class="{ 'is-drop-active': isActiveDropContainer }"
        :style="panelBoxStyle"
        :data-page-container-id="block.id"
        data-page-container-type="panel-box"
        :data-grid-container-id="block.id"
        data-grid-cell-key="__body__"
        @dragover.prevent.stop="handleContainerDragOver"
        @dragenter.prevent.stop="handleContainerDragEnter"
        @drop.prevent.stop="handleContainerDrop"
        @contextmenu.prevent.stop="!readonly && handleContainerSlotContextMenu($event, { title: '插入到面板' })"
      >
        <div
          v-if="!readonly && catalogDragBlockType"
          class="container-catalog-drop-layer"
          :class="{ 'is-active': isActiveDropContainer }"
          :data-grid-container-id="block.id"
          data-grid-cell-key="__body__"
          :data-page-container-id="block.id"
          data-page-container-type="panel-box"
        />
        <div
          v-if="block.props?.title"
          class="panel-box-title"
          title="选中面板"
          @click.stop="emit('childBlockSelect', block.id)"
        >
          {{ block.props.title }}
        </div>
        <div
          v-if="!readonly && !(block.children || []).length && !isActiveDropContainer"
          class="container-drop-empty"
        >
          拖入组件到此面板 · 或右键插入
        </div>
        <div v-if="block.children?.length" class="container-child-list">
          <div
            v-for="child in block.children"
            :key="child.id"
            class="container-child-item"
            :class="{
              selected: !readonly && child.id === selectedBlockId,
              'is-moving-source': child.id === nestedMovingBlockId,
              'is-fill-height': isNestedFillHeightChild(child),
            }"
            :style="nestedChildShellStyle(child)"
            :data-grid-child-id="child.id"
            @pointerenter="onNestedPointerEnter(child.id)"
            @pointerleave="onNestedPointerLeave(child.id)"
            @click.stop="emit('childBlockSelect', child.id)"
            @contextmenu.prevent.stop="!readonly && handleChildContextMenu($event, child, { title: '面板内组件' })"
          >
            <div v-if="showNestedChrome(child.id)" class="nested-block-node-overlay">
              <span
                class="nested-block-drag-handle"
                title="拖动组件"
                @click.stop
                @pointerdown.stop.prevent="emit('childBlockMoveStart', { block: child, event: $event })"
              >
                <svg width="1em" height="1em" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                  <path d="M8.25 6.5a1.75 1.75 0 1 0 0-3.5 1.75 1.75 0 0 0 0 3.5Zm0 7.25a1.75 1.75 0 1 0 0-3.5 1.75 1.75 0 0 0 0 3.5Zm1.75 5.5a1.75 1.75 0 1 1-3.5 0 1.75 1.75 0 0 1 0 3.5ZM14.753 6.5a1.75 1.75 0 1 0 0-3.5 1.75 1.75 0 0 0 0 3.5ZM16.5 12a1.75 1.75 0 1 1-3.5 0 1.75 1.75 0 0 1 3.5 0Zm-1.747 9a1.75 1.75 0 1 0 0-3.5 1.75 1.75 0 0 0 0 3.5Z" fill="currentColor" />
                </svg>
              </span>
              <n-dropdown
                trigger="click"
                placement="bottom-end"
                :options="nestedBlockMenuOptions"
                @select="key => emit('childBlockMenuSelect', { key, block: child })"
              >
                <button type="button" class="nested-block-menu-trigger" title="更多操作" @click.stop @mousedown.stop>
                  <svg width="1em" height="1em" viewBox="0 0 512 512" aria-hidden="true">
                    <circle cx="256" cy="256" r="32" fill="currentColor" />
                    <circle cx="416" cy="256" r="32" fill="currentColor" />
                    <circle cx="96" cy="256" r="32" fill="currentColor" />
                  </svg>
                </button>
              </n-dropdown>
            </div>
            <GridBlockRenderer
              :block="child"
              :fields="resolveNestedBlockFields(child)"
              :selected="false"
              :selected-block-id="selectedBlockId"
              :readonly="readonly"
              :runtime-crud-props="resolveNestedBlockRuntimeCrudProps(child)"
              :runtime-crud-loading="resolveNestedBlockRuntimeCrudLoading(child)"
              :show-data-source-guide="showDataSourceGuide"
              :data-source-configured="resolveNestedBlockDataSourceConfigured(child)"
              :runtime-interactive="runtimeInteractive"
              :runtime-extension-hooks="runtimeExtensionHooks"
              :runtime-record="runtimeRecord"
              :active-drop-cell="activeDropCell"
              :active-drop-container="activeDropContainer"
              :nested-moving-block-id="nestedMovingBlockId"
              :catalog-drag-block-type="catalogDragBlockType"
              :block-fields-resolver="blockFieldsResolver"
              :runtime-crud-props-resolver="runtimeCrudPropsResolver"
              :runtime-crud-loading-resolver="runtimeCrudLoadingResolver"
              :data-source-configured-resolver="dataSourceConfiguredResolver"
              :runtime-tree-active-key="runtimeTreeActiveKey"
              @child-block-select="emit('childBlockSelect', $event)"
              @child-block-menu-select="emit('childBlockMenuSelect', $event)"
              @block-props-update="emit('blockPropsUpdate', $event)"
              @tabs-active-change="emit('tabsActiveChange', $event)"
              @tab-drop="emit('tabDrop', $event)"
              @grid-cell-drop="emit('gridCellDrop', $event)"
              @container-insert="emit('containerInsert', $event)"
              @container-clear="emit('containerClear', $event)"
              @child-block-move-start="emit('childBlockMoveStart', $event)"
              @child-block-drag-end="emit('childBlockDragEnd')"
              @child-block-resize-start="emit('childBlockResizeStart', $event)"
              @runtime-tree-select="emit('runtimeTreeSelect', $event)"
              @request-data-source="emit('requestDataSource', $event)"
            />
            <template v-if="!readonly && child.id === selectedBlockId">
              <button
                v-for="anchor in resizeAnchors"
                :key="anchor"
                type="button"
                class="nested-resize-anchor"
                :class="`anchor-${anchor}`"
                title="调整组件大小"
                @pointerdown.stop="emit('childBlockResizeStart', { block: child, event: $event, anchor })"
              />
            </template>
          </div>
        </div>
        <div v-if="!readonly && isActiveDropContainer" class="layout-grid-cell-drop-preview">
          释放到面板
        </div>
      </div>
    </template>

    <!-- Tabs 布局 -->
    <template v-else-if="block.blockType === 'tabs'">
      <n-tabs
        :type="block.props?.type || 'line'"
        :size="block.props?.size || 'medium'"
        :placement="block.props?.placement || 'top'"
        :trigger="block.props?.trigger || 'click'"
        :animated="block.props?.animated !== false"
        :closable="!!block.props?.closable"
        :addable="!!block.props?.addable"
        :justify-content="block.props?.justifyContent"
        :value="resolveActiveTabKey(block)"
        class="layout-tabs"
        @update:value="value => handleTabsValueChange(block, value)"
      >
        <n-tab-pane
          v-for="tab in (block.props?.tabs || [])"
          :key="tab.key"
          :name="tab.key"
          :tab="tab.title"
          display-directive="show"
        >
          <div
            class="tab-pane-drop-target"
            :data-grid-container-id="block.id"
            :data-grid-tab-key="tab.key"
            @pointerenter="emit('tabsActiveChange', { blockId: block.id, tabKey: tab.key })"
            @dragenter.prevent.stop="handleTabPaneDragEnter"
            @dragover.prevent.stop="handleTabPaneDragOver"
            @drop.prevent.stop="event => handleTabPaneDrop(event, tab.key)"
            @contextmenu.prevent.stop="!readonly && handleContainerSlotContextMenu($event, { title: '插入到标签页', tabKey: tab.key })"
          >
            <div v-if="tab.children?.length" class="container-child-list">
              <div
                v-for="child in tab.children"
                :key="child.id"
                class="tab-pane-child"
                :class="{
                  selected: !readonly && child.id === selectedBlockId,
                  'is-fill-height': isNestedFillHeightChild(child),
                }"
                :style="nestedChildShellStyle(child)"
                :data-grid-child-id="child.id"
                @pointerenter="onNestedPointerEnter(child.id)"
                @pointerleave="onNestedPointerLeave(child.id)"
                @click.stop="emit('childBlockSelect', child.id)"
                @contextmenu.prevent.stop="!readonly && handleChildContextMenu($event, child, { tabKey: tab.key, title: '标签页内组件' })"
              >
                <div v-if="showNestedChrome(child.id)" class="nested-block-node-overlay">
                  <span
                    class="nested-block-drag-handle"
                    title="拖动组件"
                    @click.stop
                    @pointerdown.stop.prevent="emit('childBlockMoveStart', { block: child, event: $event })"
                  >
                    <svg width="1em" height="1em" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                      <path d="M8.25 6.5a1.75 1.75 0 1 0 0-3.5 1.75 1.75 0 0 0 0 3.5Zm0 7.25a1.75 1.75 0 1 0 0-3.5 1.75 1.75 0 0 0 0 3.5Zm1.75 5.5a1.75 1.75 0 1 1-3.5 0 1.75 1.75 0 0 1 0 3.5ZM14.753 6.5a1.75 1.75 0 1 0 0-3.5 1.75 1.75 0 0 0 0 3.5ZM16.5 12a1.75 1.75 0 1 1-3.5 0 1.75 1.75 0 0 1 3.5 0Zm-1.747 9a1.75 1.75 0 1 0 0-3.5 1.75 1.75 0 0 0 0 3.5Z" fill="currentColor" />
                    </svg>
                  </span>
                  <n-dropdown
                    trigger="click"
                    placement="bottom-end"
                    :options="nestedBlockMenuOptions"
                    @select="key => emit('childBlockMenuSelect', { key, block: child })"
                  >
                    <button type="button" class="nested-block-menu-trigger" title="更多操作" @click.stop @mousedown.stop>
                      <svg width="1em" height="1em" viewBox="0 0 512 512" aria-hidden="true">
                        <circle cx="256" cy="256" r="32" fill="currentColor" />
                        <circle cx="416" cy="256" r="32" fill="currentColor" />
                        <circle cx="96" cy="256" r="32" fill="currentColor" />
                      </svg>
                    </button>
                  </n-dropdown>
                </div>
                <GridBlockRenderer
                  :block="child"
                  :fields="resolveNestedBlockFields(child)"
                  :selected="false"
                  :selected-block-id="selectedBlockId"
                  :readonly="readonly"
                  :runtime-crud-props="resolveNestedBlockRuntimeCrudProps(child)"
                  :runtime-crud-loading="resolveNestedBlockRuntimeCrudLoading(child)"
                  :show-data-source-guide="showDataSourceGuide"
                  :data-source-configured="resolveNestedBlockDataSourceConfigured(child)"
                  :runtime-interactive="runtimeInteractive"
                  :runtime-extension-hooks="runtimeExtensionHooks"
                  :runtime-record="runtimeRecord"
                  :active-drop-cell="activeDropCell"
                  :active-drop-container="activeDropContainer"
                  :nested-moving-block-id="nestedMovingBlockId"
                  :catalog-drag-block-type="catalogDragBlockType"
                  :block-fields-resolver="blockFieldsResolver"
                  :runtime-crud-props-resolver="runtimeCrudPropsResolver"
                  :runtime-crud-loading-resolver="runtimeCrudLoadingResolver"
                  :data-source-configured-resolver="dataSourceConfiguredResolver"
                  :runtime-tree-active-key="runtimeTreeActiveKey"
                  @child-block-select="emit('childBlockSelect', $event)"
                  @child-block-menu-select="emit('childBlockMenuSelect', $event)"
                  @block-props-update="emit('blockPropsUpdate', $event)"
                  @tabs-active-change="emit('tabsActiveChange', $event)"
                  @tab-drop="emit('tabDrop', $event)"
                  @grid-cell-drop="emit('gridCellDrop', $event)"
                  @container-insert="emit('containerInsert', $event)"
                  @container-clear="emit('containerClear', $event)"
                  @child-block-move-start="emit('childBlockMoveStart', $event)"
                  @child-block-drag-end="emit('childBlockDragEnd')"
                  @child-block-resize-start="emit('childBlockResizeStart', $event)"
                  @runtime-tree-select="emit('runtimeTreeSelect', $event)"
                  @request-data-source="emit('requestDataSource', $event)"
                />
                <template v-if="!readonly && child.id === selectedBlockId">
                  <button
                    v-for="anchor in resizeAnchors"
                    :key="anchor"
                    type="button"
                    class="nested-resize-anchor"
                    :class="`anchor-${anchor}`"
                    title="调整组件大小"
                    @pointerdown.stop="emit('childBlockResizeStart', { block: child, event: $event, anchor })"
                  />
                </template>
              </div>
            </div>
            <div v-else-if="!readonly" class="sub-tab-empty">
              拖入组件 · 右键可插入
            </div>
          </div>
        </n-tab-pane>
      </n-tabs>
    </template>

    <!-- 留白 -->
    <template v-else-if="block.blockType === 'spacer'">
      <div class="layout-spacer" />
    </template>

    <template v-else>
      <div class="block-empty">
        未知区块类型：{{ block.blockType }}
      </div>
    </template>
    <div v-if="showBlockBindingState" class="block-binding-state" :class="{ error: !!blockBindingError }">
      {{ blockBindingError || '正在加载数据...' }}
    </div>
  </div>
  <ContainerContextMenu
    v-if="!readonly"
    :show="containerMenu.visible && containerMenu.containerId === block.id"
    :x="containerMenu.x"
    :y="containerMenu.y"
    :mode="containerMenu.mode"
    :title="containerMenu.title"
    :exclude-block-types="containerMenuExcludeBlockTypes"
    @update:show="value => !value && closeContainerMenu()"
    @insert="handleContainerMenuInsert"
    @action="handleContainerMenuAction"
  />
</template>


<script>
import { useGridBlockRenderer } from './composables/useGridBlockRenderer'
import { gridBlockRendererLocalComponents } from './gridBlockRendererLocalComponents'

export default {
  name: 'GridBlockRenderer',
  components: { ...gridBlockRendererLocalComponents },
  props: {
  block: {
    type: Object,
    required: true,
  },
  fields: {
    type: Array,
    default: () => [],
  },
  selected: {
    type: Boolean,
    default: false,
  },
  readonly: {
    type: Boolean,
    default: false,
  },
  inlineTextEditing: {
    type: Boolean,
    default: false,
  },
  runtimeCrudProps: {
    type: Object,
    default: null,
  },
  /** 已在 RuntimeListGridFlow 内时禁止再展开 listGridLayout，避免无限嵌套 */
  suppressRuntimeListGrid: {
    type: Boolean,
    default: false,
  },
  runtimeCrudLoading: {
    type: Boolean,
    default: false,
  },
  showDataSourceGuide: {
    type: Boolean,
    default: false,
  },
  dataSourceConfigured: {
    type: Boolean,
    default: false,
  },
  runtimeInteractive: {
    type: Boolean,
    default: false,
  },
  blockFieldsResolver: {
    type: Function,
    default: null,
  },
  runtimeCrudPropsResolver: {
    type: Function,
    default: null,
  },
  runtimeCrudLoadingResolver: {
    type: Function,
    default: null,
  },
  dataSourceConfiguredResolver: {
    type: Function,
    default: null,
  },
  runtimeRecord: {
    type: Object,
    default: () => ({}),
  },
  runtimeTreeActiveKey: {
    type: [String, Number],
    default: '__all__',
  },
  selectedBlockId: {
    type: String,
    default: '',
  },
  activeDropCell: {
    type: Object,
    default: null,
  },
  activeDropContainer: {
    type: Object,
    default: null,
  },
  nestedMovingBlockId: {
    type: String,
    default: '',
  },
  catalogDragBlockType: {
    type: String,
    default: '',
  },
  runtimeExtensionHooks: {
    type: [Object, Function],
    default: () => ({}),
  },
},
  emits: [
  'runtimeTreeSelect',
  'treePanelCollapseChange',
  'crudPreviewStateChange',
  'blockPropsUpdate',
  'childBlockSelect',
  'childBlockMenuSelect',
  'childBlockDragStart',
  'childBlockMoveStart',
  'childBlockDragEnd',
  'childBlockResizeStart',
  'inlineTextUpdate',
  'blockActivate',
  'tabsActiveChange',
  'tabDrop',
  'gridCellDrop',
  'gridCellDragOver',
  'gridCellInsert',
  'containerInsert',
  'containerClear',
  'containerDragOver',
  'requestDataSource',
],
  setup(props, { emit, expose }) {
    const api = useGridBlockRenderer(props, emit)
    if (api.gridBlockExposeApi)
      expose(api.gridBlockExposeApi)
    return api
  },
}
</script>

<style scoped src="./grid-block-renderer.css"></style>
