<template>
  <div v-if="blockRuntimeVisible" class="grid-block" :class="[`block-${block.blockType}`, { selected }]" :style="blockStyle" :data-block-id="block.id">
    <!-- 查询表单 -->
    <template v-if="block.blockType === 'search-form'">
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
          <span>{{ block.props?.text || block.label || '返回' }}</span>
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

    <!-- 数据列表 -->
    <template v-else-if="block.blockType === 'data-table'">
      <div class="block-header">
        <strong>{{ block.label || '数据列表' }}</strong>
        <span class="block-meta">{{ resolvedFields.length }} 列</span>
      </div>
      <n-data-table
        v-if="resolvedFields.length"
        :columns="tableColumns"
        :data="sampleRows"
        :bordered="false"
        size="small"
        class="block-table"
        :style="{ '--block-table-row-height': blockTableRowHeight }"
      />
      <div v-else class="block-empty">
        点击右侧"配置字段"按钮添加列表列
      </div>
    </template>

    <!-- 详情信息 -->
    <template v-else-if="block.blockType === 'detail-info'">
      <div class="detail-info-preview">
        <div class="block-header">
          <strong>{{ block.props?.title || block.label || '详情信息' }}</strong>
          <span class="block-meta">{{ resolvedFields.length }} 项</span>
        </div>
        <div v-if="resolvedFields.length" class="detail-info-grid" :style="detailInfoGridStyle">
          <div
            v-for="field in resolvedFields"
            :key="field.field"
            class="detail-info-item"
            :class="{ bordered: block.props?.bordered }"
          >
            <div class="detail-label">
              {{ field.label || field.field }}
            </div>
            <div class="detail-value">
              <FieldValueRenderer
                :value="detailValue(field, 0)"
                :row="detailInfoRecord"
                :field="field"
                :setting="fieldSetting(field.field)"
                :context="runtimeRuleContext"
              />
            </div>
          </div>
        </div>
        <div v-else class="block-empty">
          点击右侧"配置字段"按钮添加详情字段
        </div>
        <div v-if="detailInfoLoading" class="detail-info-state">
          正在加载详情数据...
        </div>
        <div v-else-if="detailInfoError" class="detail-info-state error">
          {{ detailInfoError }}
        </div>
      </div>
    </template>

    <!-- 栅格布局 -->
    <template v-else-if="block.blockType === 'grid-layout'">
      <div
        class="layout-grid-preview"
        :class="{ 'is-nested-moving': !!nestedMovingBlockId }"
        :style="gridLayoutStyle"
      >
        <div
          v-for="cell in gridLayoutCells"
          :key="cell.key"
          class="layout-grid-cell"
          :class="{
            'bordered': block.props?.showCellBorder !== false,
            'is-drop-active': isActiveDropCell(cell),
          }"
          :style="gridCellStyle(cell)"
          :data-grid-cell-key="cell.key"
          :data-grid-container-id="block.id"
        >
          <div v-if="hasGridCellChildren(cell)" class="layout-grid-cell-body">
            <div
              v-for="child in cell.children"
              :key="child.id"
              class="layout-grid-cell-child"
              :class="{
                'selected': child.id === selectedBlockId,
                'is-moving-source': child.id === nestedMovingBlockId,
              }"
              :style="nestedChildShellStyle(child)"
              :data-grid-child-id="child.id"
              @click.stop="emit('childBlockSelect', child.id)"
            >
              <div v-if="!readonly" class="nested-block-node-overlay">
                <span
                  class="nested-block-drag-handle"
                  title="拖动组件"
                  @click.stop
                  @pointerdown.stop.prevent="emit('childBlockMoveStart', { block: child, event: $event })"
                >
                  <svg
                    width="1em"
                    height="1em"
                    viewBox="0 0 24 24"
                    fill="none"
                    xmlns="http://www.w3.org/2000/svg"
                    aria-hidden="true"
                  >
                    <path
                      d="M8.25 6.5a1.75 1.75 0 1 0 0-3.5 1.75 1.75 0 0 0 0 3.5Zm0 7.25a1.75 1.75 0 1 0 0-3.5 1.75 1.75 0 0 0 0 3.5Zm1.75 5.5a1.75 1.75 0 1 1-3.5 0 1.75 1.75 0 0 1 3.5 0ZM14.753 6.5a1.75 1.75 0 1 0 0-3.5 1.75 1.75 0 0 0 0 3.5ZM16.5 12a1.75 1.75 0 1 1-3.5 0 1.75 1.75 0 0 1 3.5 0Zm-1.747 9a1.75 1.75 0 1 0 0-3.5 1.75 1.75 0 0 0 0 3.5Z"
                      fill="currentColor"
                    />
                  </svg>
                </span>
                <n-dropdown
                  trigger="click"
                  placement="bottom-end"
                  :options="nestedBlockMenuOptions"
                  @select="key => emit('childBlockMenuSelect', { key, block: child })"
                >
                  <button
                    type="button"
                    class="nested-block-menu-trigger"
                    title="更多操作"
                    @click.stop
                    @mousedown.stop
                  >
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
                :fields="fields"
                :selected="false"
                :selected-block-id="selectedBlockId"
                :readonly="readonly"
                :runtime-crud-props="runtimeCrudProps"
                :runtime-record="runtimeRecord"
                :active-drop-cell="activeDropCell"
                :nested-moving-block-id="nestedMovingBlockId"
                @child-block-select="emit('childBlockSelect', $event)"
                @child-block-menu-select="emit('childBlockMenuSelect', $event)"
                @block-props-update="emit('blockPropsUpdate', $event)"
                @child-block-drag-start="emit('childBlockDragStart', $event)"
                @child-block-move-start="emit('childBlockMoveStart', $event)"
                @child-block-drag-end="emit('childBlockDragEnd')"
                @child-block-resize-start="emit('childBlockResizeStart', $event)"
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
          <div v-if="shouldShowGridCellDropPreview(cell)" class="layout-grid-cell-drop-preview">
            释放到此格
          </div>
          <div v-else-if="shouldShowGridCellEmpty(cell)" class="layout-grid-cell-empty">
            拖入组件
          </div>
        </div>
      </div>
    </template>

    <!-- 系统 AiCrudPage 组件 -->
    <template v-else-if="block.blockType === 'AiCrudPage'">
      <div class="system-component-preview ai-crud-preview">
        <AiCrudPage
          v-if="effectiveRuntimeCrudProps"
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
          :edit-x-gap="block.props?.editXGap ?? 16"
          :edit-y-gap="block.props?.editYGap ?? 8"
          :modal-width="block.props?.modalWidth || '800px'"
          :detail-modal-width="block.props?.detailModalWidth || 'min(1080px, 92vw)'"
          :form-open-mode="resolveEffectiveFormOpenMode(block.props || {}, {})"
          :tab-workspace="block.props?.tabWorkspace || {}"
          :modal-type="resolveEffectiveModalType(block.props || {}, {})"
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
          :bordered="!!block.props?.bordered"
          :striped="!!block.props?.striped"
          :hide-selection="block.props?.hideSelection === true"
          :max-height="block.props?.maxHeight || undefined"
          :scroll-x="block.props?.scrollX || undefined"
          :list-method="block.props?.listMethod || 'get'"
          :list-data-field="block.props?.listDataField || 'records'"
          :list-total-field="block.props?.listTotalField || 'total'"
          :is-encrypt="block.props?.isEncrypt === true"
          :public-params="block.props?.publicParams || {}"
          :public-query="block.props?.publicQuery || {}"
          :form-default-values="block.props?.formDefaultValues || {}"
          :submit-default-params="block.props?.submitDefaultParams || {}"
          v-bind="resolvedDesignerCrudHookHandlers"
          @load-list-success="handleCrudPreviewSuccess"
          @load-list-error="handleCrudPreviewError"
        />
        <div v-if="isStaticCrudPreview" class="ai-crud-preview-static-tip">
          静态结构预览 · 可填写新增表单；配置真实数据接口后才会提交、查询数据
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
      <div class="system-component-preview">
        <AiForm
          v-model:value="previewFormValue"
          :schema="aiFormSchema"
          :grid-cols="block.props?.gridCols || 2"
          :label-placement="block.props?.labelPlacement || 'left'"
          :label-width="block.props?.labelWidth || 100"
          :label-align="block.props?.labelAlign || 'right'"
          :x-gap="block.props?.xGap ?? 12"
          :y-gap="block.props?.yGap ?? 0"
          :size="block.props?.size || 'medium'"
          :show-actions="block.props?.showActions !== false"
          :show-submit="block.props?.showSubmit !== false"
          :show-reset="block.props?.showReset !== false"
          :show-cancel="block.props?.showCancel === true"
          :submit-text="block.props?.submitText || '提交'"
          :reset-text="block.props?.resetText || '重置'"
          :cancel-text="block.props?.cancelText || '取消'"
          :enable-collapse="block.props?.enableCollapse === true"
          :max-visible-fields="block.props?.maxVisibleFields || 6"
          :show-feedback="block.props?.showFeedback !== false"
        />
      </div>
    </template>

    <!-- 筛选树 -->
    <template v-else-if="block.blockType === 'tree-panel'">
      <div class="tree-preview" :class="{ 'is-runtime': readonly && runtimeCrudProps, 'is-panel-collapsed': treePanelCollapsed }">
        <button
          type="button"
          class="tree-panel-edge-toggle"
          :aria-label="treePanelCollapsed ? '展开筛选树' : '收起筛选树'"
          :title="treePanelCollapsed ? '展开筛选树' : '收起筛选树'"
          @click.stop="toggleTreePanel"
        >
          <span>{{ treePanelCollapsed ? '›' : '‹' }}</span>
        </button>
        <div
          v-if="treePanelCollapsed"
          class="tree-panel-rail"
          :title="block.props?.treeTitle || '筛选树'"
        >
          <span>树</span>
        </div>
        <div v-else class="tree-preview-head">
          <div>
            <strong>{{ block.props?.treeTitle || '筛选树' }}</strong>
            <span>{{ block.props?.sourceModelName || '按树节点筛选右侧列表' }}</span>
          </div>
          <small>{{ block.props?.loadMode === 'lazy' ? '懒加载' : '全量' }}</small>
        </div>
        <div v-if="!treePanelCollapsed" class="tree-node-toolbar">
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
        <template v-if="!treePanelCollapsed && readonly && runtimeCrudProps">
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
              :expanded-keys="runtimeExpandedTreeKeys"
              @update:expanded-keys="runtimeExpandedTreeKeys = $event"
              @update:selected-keys="handleRuntimeTreeSelected"
            />
            <div v-else class="tree-empty">
              {{ runtimeTreeLoading ? '加载中...' : '暂无树节点' }}
            </div>
          </n-spin>
        </template>
        <template v-else-if="!treePanelCollapsed">
          <div class="tree-search-placeholder">
            搜索节点
          </div>
          <div class="tree-node active">
            <span class="tree-node-dot" />
            <span>全部</span>
            <small>12</small>
          </div>
          <div class="tree-node">
            <span class="tree-node-dot" />
            <span>{{ block.props?.labelField || '一级节点' }}</span>
            <small>8</small>
          </div>
          <div v-if="previewTreeExpanded" class="tree-node child">
            <span class="tree-node-dot" />
            <span>子节点示例</span>
            <small>3</small>
          </div>
        </template>
        <div v-if="!treePanelCollapsed" class="tree-foot">
          {{ block.props?.filterField || 'filterField' }} -> {{ block.props?.targetField || 'targetField' }}
        </div>
      </div>
    </template>

    <!-- 指标卡片 -->
    <template v-else-if="block.blockType === 'stats-strip'">
      <div class="stats-grid">
        <div
          v-for="(metric, idx) in statsMetrics"
          :key="idx"
          class="stats-card"
        >
          <div class="stats-label">
            {{ metric.label }}
          </div>
          <div class="stats-value">
            {{ metric.value }}
          </div>
          <div v-if="metric.trend" class="stats-trend" :class="trendClass(metric.trend)">
            {{ metric.trend }}
          </div>
        </div>
        <div v-if="!statsMetrics.length" class="block-empty">
          点击右侧添加指标项
        </div>
      </div>
    </template>

    <!-- 提示面板 -->
    <template v-else-if="block.blockType === 'info-panel'">
      <div class="info-panel-preview" :class="`type-${boundInfoType || 'info'}`">
        <strong>{{ boundInfoTitle || '提示信息' }}</strong>
        <span>{{ boundInfoContent || '在右侧填写提示内容' }}</span>
      </div>
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
          :block="!!block.props?.block"
          :disabled="!!block.props?.disabled"
          :loading="!!block.props?.loading"
          :size="block.props?.size || 'small'"
          @click.stop="handleActionButtonClick"
        >
          {{ block.props?.text || '操作' }}
        </n-button>
      </div>
      <div v-else class="block-empty">
        当前权限或显示条件不满足
      </div>
    </template>

    <!-- 按钮组 -->
    <template v-else-if="block.blockType === 'button-group'">
      <div class="button-group-preview">
        <n-button
          v-for="button in (block.props?.buttons || [])"
          :key="button.key || button.text"
          :type="button.type === 'default' ? undefined : button.type"
          size="small"
          disabled
        >
          {{ button.text || '按钮' }}
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
        {{ boundTextTitle || '页面标题' }}
        <small v-if="boundTextSubtitle">{{ boundTextSubtitle }}</small>
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
        <span>{{ boundStatisticTitle || '统计指标' }}</span>
        <strong>{{ block.props?.prefix }}{{ boundStatisticValue || '0' }}{{ block.props?.suffix }}</strong>
        <small>{{ boundStatisticTrend || '' }} {{ boundStatisticDescription || '' }}</small>
      </div>
    </template>

    <!-- 链接 -->
    <template v-else-if="block.blockType === 'link'">
      <a class="link-preview" :class="`type-${block.props?.type || 'primary'}`" :href="boundLinkHref || '#'" :target="block.props?.target || '_self'" @click.prevent>
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
      <div class="barcode-preview" :style="{ background: block.props?.background || '#fff' }">
        <Vue3Barcode
          :key="`${block.id}_${block.props?.value || ''}_${block.props?.format || ''}`"
          :value="String(block.props?.value || 'FORGE-2026-0001')"
          :format="block.props?.format || 'CODE128'"
          :width="Number(block.props?.barWidth || 2)"
          :height="Number(block.props?.barHeight || 72)"
          :display-value="block.props?.showText !== false"
          :line-color="block.props?.lineColor || '#0f172a'"
          :background="block.props?.background || '#ffffff'"
          :font-size="Number(block.props?.fontSize || 14)"
          :margin="Number(block.props?.margin ?? 8)"
          element-tag="svg"
        >
          <span class="code-invalid">条形码内容无效</span>
        </Vue3Barcode>
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
      <div class="box-layout-preview" :style="boxLayoutStyle">
        <GridBlockRenderer
          v-for="child in (block.children || [])"
          :key="child.id"
          :block="child"
          :fields="fields"
          :selected="child.id === selectedBlockId"
          :selected-block-id="selectedBlockId"
          :readonly="readonly"
          :runtime-crud-props="runtimeCrudProps"
          :runtime-record="runtimeRecord"
          @click.stop="emit('childBlockSelect', child.id)"
          @child-block-select="emit('childBlockSelect', $event)"
          @child-block-menu-select="emit('childBlockMenuSelect', $event)"
          @block-props-update="emit('blockPropsUpdate', $event)"
          @child-block-move-start="emit('childBlockMoveStart', $event)"
          @child-block-drag-end="emit('childBlockDragEnd')"
          @child-block-resize-start="emit('childBlockResizeStart', $event)"
        />
        <div v-if="!(block.children || []).length" class="container-empty">
          拖入组件到盒子中
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
      <div class="layout-card">
        <div v-if="block.props?.title" class="layout-card-title">
          {{ block.props.title }}
        </div>
        <div class="layout-card-body">
          <div v-if="block.props?.content" class="layout-card-text">
            {{ block.props.content }}
          </div>
          <div v-if="block.children?.length" class="container-child-list">
            <GridBlockRenderer
              v-for="child in block.children"
              :key="child.id"
              :block="child"
              :fields="fields"
              :selected="child.id === selectedBlockId"
              :selected-block-id="selectedBlockId"
              :readonly="readonly"
              :runtime-crud-props="runtimeCrudProps"
              :runtime-record="runtimeRecord"
              :active-drop-cell="activeDropCell"
              :nested-moving-block-id="nestedMovingBlockId"
              @click.stop="emit('childBlockSelect', child.id)"
              @child-block-select="emit('childBlockSelect', $event)"
              @child-block-menu-select="emit('childBlockMenuSelect', $event)"
              @block-props-update="emit('blockPropsUpdate', $event)"
              @child-block-move-start="emit('childBlockMoveStart', $event)"
              @child-block-drag-end="emit('childBlockDragEnd')"
              @child-block-resize-start="emit('childBlockResizeStart', $event)"
            />
          </div>
          <div v-else-if="!block.props?.content" class="container-empty">
            拖入组件到卡片中
          </div>
        </div>
      </div>
    </template>

    <!-- Tabs 布局 -->
    <template v-else-if="block.blockType === 'tabs'">
      <n-tabs type="line" size="small" :default-value="block.props?.tabs?.[0]?.key" class="layout-tabs">
        <n-tab-pane
          v-for="tab in (block.props?.tabs || [])"
          :key="tab.key"
          :name="tab.key"
          :tab="tab.title"
        >
          <div v-if="tab.children?.length" class="container-child-list">
            <GridBlockRenderer
              v-for="child in tab.children"
              :key="child.id"
              :block="child"
              :fields="fields"
              :selected="child.id === selectedBlockId"
              :selected-block-id="selectedBlockId"
              :readonly="readonly"
              :runtime-crud-props="runtimeCrudProps"
              :runtime-record="runtimeRecord"
              :active-drop-cell="activeDropCell"
              :nested-moving-block-id="nestedMovingBlockId"
              @click.stop="emit('childBlockSelect', child.id)"
              @child-block-select="emit('childBlockSelect', $event)"
              @child-block-menu-select="emit('childBlockMenuSelect', $event)"
              @block-props-update="emit('blockPropsUpdate', $event)"
              @child-block-move-start="emit('childBlockMoveStart', $event)"
              @child-block-drag-end="emit('childBlockDragEnd')"
              @child-block-resize-start="emit('childBlockResizeStart', $event)"
            />
          </div>
          <div v-else class="sub-tab-empty">
            拖入组件到当前标签页
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
</template>

<script setup>
import { ChevronBackOutline } from '@vicons/ionicons5'
import QRCodeVue3 from 'qrcode-vue3'
import { computed, h, onMounted, ref, watch } from 'vue'
import Vue3Barcode from 'vue3-barcode'
import { useRoute, useRouter } from 'vue-router'
import AiCrudPage from '@/components/ai-form/AiCrudPage.vue'
import AiForm from '@/components/ai-form/AiForm.vue'
import AiTable from '@/components/ai-form/AiTable.vue'
import SignaturePad from '@/components/flow/SignaturePad.vue'
import FieldValueRenderer from '@/components/lowcode-builder/shared/FieldValueRenderer.vue'
import InlineRichText from '@/components/lowcode-builder/shared/InlineRichText.vue'
import { pageWidgetComponentKeys } from '@/components/lowcode-builder/shared/page-widget-schema'
import PageWidgetRenderer from '@/components/lowcode-builder/shared/PageWidgetRenderer.vue'
import { resolveCurrentConfigPlaceholder } from '@/components/lowcode-builder/shared/runtime-crud-props'
import { matchSimpleExpression, resolveRuntimeControl } from '@/components/lowcode-builder/shared/runtime-rules'
import { useUserStore } from '@/store'
import { request } from '@/utils'
import { applyCrudHookRules, CRUD_HOOK_RULE_TARGETS, normalizeCrudHookRules } from './crud-hook-rules'

const props = defineProps({
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
  nestedMovingBlockId: {
    type: String,
    default: '',
  },
})

const emit = defineEmits([
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
])

const localDataBindableBlockTypes = new Set([
  'stats-strip',
  'info-panel',
  'custom-html',
  'tag-list',
  'steps',
  'timeline',
  'empty-state',
  'text-title',
  'paragraph',
  'statistic',
  'link',
  'text-tip',
  'audio-player',
  'video-player',
  'avatar',
])

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const fieldMap = computed(() => new Map(props.fields.map(f => [f.field, f])))
const resolvedFields = computed(() => (props.block.fieldRefs || [])
  .map(ref => fieldMap.value.get(ref))
  .filter(Boolean))
const visibleResolvedFields = computed(() => resolvedFields.value
  .filter(field => props.block.props?.fieldSettings?.[field.field]?.visible !== false))
const previewFormValue = ref({})
const signaturePreviewValue = ref('')
const runtimeCrudRef = ref(null)
const runtimeTreeLoading = ref(false)
const runtimeTreeNodes = ref([])
const runtimeTreeNodeMap = ref(new Map())
const runtimeExpandedTreeKeys = ref([])
const detailInfoLoading = ref(false)
const detailInfoError = ref('')
const remoteDetailInfoRecord = ref({})
const blockBindingLoading = ref(false)
const blockBindingError = ref('')
const remoteBlockBindingData = ref(null)
const previewTreeExpanded = ref(true)
const treePanelCollapsed = ref(false)
const runtimeTreeChildrenField = computed(() => props.block.props?.childrenField || 'children')
const runtimeSelectedTreeKeys = computed(() => props.runtimeTreeActiveKey === '__all__' ? [] : [props.runtimeTreeActiveKey])
const runtimeRuleContext = computed(() => ({
  record: props.runtimeRecord || {},
  row: props.runtimeRecord || {},
  data: props.runtimeRecord || {},
  route: {
    query: route.query || {},
    params: route.params || {},
    path: route.path,
    fullPath: route.fullPath,
    name: route.name,
  },
  user: userStore.userInfo || userStore.user || {},
}))
const blockRuntimeControl = computed(() => resolveRuntimeControl(props.block || {}, runtimeRuleContext.value))
const blockRuntimeVisible = computed(() => !props.readonly || blockRuntimeControl.value.visible !== false)
const detailInfoRecord = computed(() => {
  if (props.block.blockType !== 'detail-info')
    return props.runtimeRecord || {}
  const sourceType = props.block.props?.dataSourceType || 'current'
  if (sourceType === 'remote')
    return remoteDetailInfoRecord.value || {}
  const path = props.block.props?.contextPath || ''
  if (path)
    return getNestedRecordValue(props.runtimeRecord || {}, path) || {}
  return props.runtimeRecord || {}
})
const blockBoundData = computed(() => {
  const binding = props.block.props?.dataBinding || {}
  if (binding.enabled !== true || binding.sourceType === 'static')
    return null
  const source = binding.sourceType === 'remote' ? remoteBlockBindingData.value : props.runtimeRecord
  if (!source)
    return null
  const dataPath = binding.sourceType === 'context' ? binding.contextPath : binding.dataPath
  if (!dataPath)
    return source
  const nested = getNestedRecordValue(source, dataPath)
  return nested === undefined || nested === null ? source : nested
})
const isLocalDataBindableBlock = computed(() => localDataBindableBlockTypes.has(props.block.blockType))
const showBlockBindingState = computed(() => isLocalDataBindableBlock.value && (blockBindingLoading.value || blockBindingError.value))
const statsMetrics = computed(() => normalizeBlockBoundRows(props.block.props?.metrics || []).map((row, index) => ({
  label: boundRowField(row, 'labelField', 'label', `指标${index + 1}`),
  value: boundRowField(row, 'valueField', 'value', '-'),
  trend: boundRowField(row, 'metaField', 'trend', ''),
})))
const boundInfoTitle = computed(() => boundContentValue(props.block.props?.title || '提示信息', 'titleField', 'title'))
const boundInfoContent = computed(() => boundContentValue(props.block.props?.content || '在右侧填写提示内容', 'contentField', 'content'))
const boundInfoType = computed(() => boundContentValue(props.block.props?.type || 'info', 'valueField', 'type'))
const boundCustomTitle = computed(() => boundContentValue(props.block.props?.title || '', 'titleField', 'title'))
const boundCustomContent = computed(() => boundContentValue(props.block.props?.content || '', 'contentField', 'content'))
const boundTags = computed(() => normalizeBlockBoundRows(props.block.props?.tags || []).map((row, index) => ({
  label: boundRowField(row, 'labelField', 'label', `标签${index + 1}`),
  type: boundRowField(row, 'valueField', 'type', 'default'),
})))
const boundSteps = computed(() => normalizeBlockBoundRows(props.block.props?.steps || []).map((row, index) => ({
  title: boundRowField(row, 'titleField', 'title', `步骤${index + 1}`),
  description: boundRowField(row, 'descriptionField', 'description', ''),
})))
const boundTimelineTitle = computed(() => boundContentValue(props.block.props?.title || '', 'titleField', 'title'))
const boundTimelineItems = computed(() => normalizeBlockBoundRows(props.block.props?.items || []).map((row, index) => ({
  title: boundRowField(row, 'titleField', 'title', `节点${index + 1}`),
  content: boundRowField(row, 'descriptionField', 'content', ''),
  time: boundRowField(row, 'metaField', 'time', ''),
})))
const boundEmptyTitle = computed(() => boundContentValue(props.block.props?.title || '暂无数据', 'titleField', 'title'))
const boundEmptyDescription = computed(() => boundContentValue(props.block.props?.description || '当前没有可展示的数据', 'descriptionField', 'description'))
const boundEmptyActionText = computed(() => boundContentValue(props.block.props?.actionText || '', 'valueField', 'actionText'))
const boundTextTitle = computed(() => boundContentValue(props.block.props?.text || '页面标题', 'titleField', 'text'))
const boundTextSubtitle = computed(() => boundContentValue(props.block.props?.subtitle || '', 'descriptionField', 'subtitle'))
const boundParagraphContent = computed(() => boundContentValue(props.block.props?.content || '段落内容', 'contentField', 'content'))
const boundStatisticTitle = computed(() => boundContentValue(props.block.props?.title || '统计指标', 'titleField', 'title'))
const boundStatisticValue = computed(() => boundContentValue(props.block.props?.value || '0', 'valueField', 'value'))
const boundStatisticDescription = computed(() => boundContentValue(props.block.props?.description || '', 'descriptionField', 'description'))
const boundStatisticTrend = computed(() => boundContentValue(props.block.props?.trend || '', 'metaField', 'trend'))
const boundLinkText = computed(() => boundContentValue(props.block.props?.text || '链接文本', 'titleField', 'text'))
const boundLinkHref = computed(() => boundContentValue(props.block.props?.href || '#', 'valueField', 'href'))
const boundTextTipTitle = computed(() => boundContentValue(props.block.props?.title || '提示', 'titleField', 'title'))
const boundTextTipContent = computed(() => boundContentValue(props.block.props?.content || '提示内容', 'contentField', 'content'))
const boundTextTipType = computed(() => boundContentValue(props.block.props?.type || 'info', 'valueField', 'type'))
const boundMediaTitle = computed(() => boundContentValue(props.block.props?.title || '', 'titleField', 'title'))
const boundMediaSrc = computed(() => boundContentValue(props.block.props?.src || '', 'valueField', 'src'))
const boundVideoPoster = computed(() => boundContentValue(props.block.props?.poster || '', 'metaField', 'poster'))
const boundAvatarName = computed(() => boundContentValue(props.block.props?.name || '用户名称', 'titleField', 'name'))
const boundAvatarDescription = computed(() => boundContentValue(props.block.props?.description || '角色 / 部门', 'descriptionField', 'description'))
const boundAvatarSrc = computed(() => boundContentValue(props.block.props?.src || '', 'valueField', 'src'))
const runtimeTreeTotal = computed(() => countTreeNodes(runtimeTreeNodes.value, runtimeTreeChildrenField.value))
const resizeAnchors = ['top-left', 'top', 'top-right', 'right', 'bottom-right', 'bottom', 'bottom-left', 'left']
const nestedBlockMenuOptions = [
  { label: '复制', key: 'duplicate' },
  { label: '删除', key: 'delete' },
]
const crudPreviewMode = computed(() => props.block.props?.previewMode || (props.block.props?.previewLiveData === true ? 'realList' : 'mock'))
const gridLayoutCells = computed(() => {
  const rawCells = Array.isArray(props.block.props?.cells) ? props.block.props.cells : []
  const sourceCells = rawCells.length ? rawCells : [{ key: 'cell_1', title: '栅格 1', span: 24, children: [] }]
  return sourceCells.map((cell, index) => {
    const children = Array.isArray(cell.children) ? cell.children : []
    return {
      key: cell.key || `cell_${index + 1}`,
      title: cell.title ?? `栅格 ${index + 1}`,
      span: clampGridSpan(cell.span, 6),
      children,
      minHeight: Number(cell.minHeight || 0) || resolveGridCellPreviewMinHeight(children),
    }
  })
})
const gridLayoutStyle = computed(() => ({
  gridTemplateColumns: `repeat(${Math.max(1, Number(props.block.props?.columns || 24))}, minmax(0, 1fr))`,
  gap: `${Math.max(0, Number(props.block.props?.rowGap ?? 0))}px ${Math.max(0, Number(props.block.props?.gutter ?? props.block.props?.gap ?? 16))}px`,
  alignItems: props.block.props?.alignItems || 'stretch',
  justifyItems: props.block.props?.justifyItems || 'stretch',
  height: 'auto',
  minHeight: '100%',
}))
function gridCellStyle(cell = {}) {
  return {
    gridColumn: `span ${clampGridSpan(cell.span, 6)}`,
    minHeight: `${Math.max(24, Number(cell.minHeight || props.block.props?.cellMinHeight || 120))}px`,
    backgroundColor: props.block.props?.cellBackground || 'transparent',
  }
}
function resolveGridCellPreviewMinHeight(children = []) {
  if (!children.length)
    return Math.max(24, Number(props.block.props?.cellMinHeight || 120))
  return children.reduce((sum, child) => sum + resolvePreviewChildHeight(child), 0) + Math.max(0, children.length - 1) * 8 + 16
}
function resolvePreviewChildHeight(child = {}) {
  const height = normalizeCssNumber(child.props?.style?.height)
  if (height)
    return height
  return Math.max(72, Number(child.gridH || 2) * 32 + Math.max(0, Number(child.gridH || 2) - 1) * 8)
}
function normalizeCssNumberValue(value) {
  if (value === null || value === undefined || value === '' || value === '100%' || value === 'auto')
    return 0
  if (typeof value === 'number')
    return value
  const num = Number(String(value).trim().replace('px', ''))
  return Number.isFinite(num) ? num : 0
}
function normalizeCssNumber(value) {
  return normalizeCssNumberValue(value)
}
function isActiveDropCell(cell = {}) {
  return props.activeDropCell?.containerId === props.block.id && props.activeDropCell?.cellKey === cell.key
}
function hasGridCellChildren(cell = {}) {
  return Array.isArray(cell.children) && cell.children.length > 0
}
function shouldShowGridCellDropPreview(cell = {}) {
  if (!isActiveDropCell(cell))
    return false
  return !hasGridCellChildren(cell) || Boolean(props.nestedMovingBlockId)
}
function shouldShowGridCellEmpty(cell = {}) {
  return !props.readonly && !hasGridCellChildren(cell) && !isActiveDropCell(cell)
}
function clampGridSpan(value, fallback = 1) {
  const columns = Math.max(1, Number(props.block.props?.columns || 24))
  const number = Number(value)
  return Math.max(1, Math.min(columns, Number.isFinite(number) ? number : fallback))
}
function nestedChildShellStyle(child = {}) {
  const style = child.props?.style || {}
  const widthMode = style.widthMode || (style.width === '100%' || !style.width ? 'full' : 'fixed')
  const x = Math.max(0, Number(style.x ?? style.left ?? 0) || 0)
  const y = Math.max(0, Number(style.y ?? style.top ?? 0) || 0)
  const width = widthMode === 'fixed'
    ? normalizeCssSize(style.width, '100%')
    : widthMode === 'auto' ? 'auto' : `calc(100% - ${x}px)`
  return {
    left: `${x}px`,
    top: `${y}px`,
    width,
    height: normalizeCssSize(style.height, ''),
  }
}
function normalizeCssSize(value, fallback = '') {
  if (value === null || value === undefined || value === '')
    return fallback
  if (typeof value === 'number')
    return `${value}px`
  const text = String(value).trim()
  if (!text)
    return fallback
  return /^\d+(?:\.\d+)?$/.test(text) ? `${text}px` : text
}
const actionButtonVisible = computed(() => {
  if (props.block.blockType !== 'action-button')
    return true
  if (!props.readonly)
    return true
  const eventItem = resolvePrimaryClickEvent()
  return hasRuntimePermission(eventItem?.permissionCode)
    && matchDisplayCondition(eventItem?.displayCondition, props.runtimeRecord || {})
})
const blockStyle = computed(() => {
  const style = {
    width: '100%',
    height: '100%',
    backgroundColor: 'transparent',
    borderColor: 'transparent',
    borderWidth: 0,
    borderStyle: 'none',
    borderRadius: 0,
    boxShadow: 'none',
    padding: 0,
    margin: 0,
    minWidth: '',
    maxWidth: '',
    minHeight: '',
    maxHeight: '',
    ...(props.block.props?.style || {}),
  }
  const resolvedStyle = {
    width: toCssSize(style.width, '100%'),
    height: toCssSize(style.height, '100%'),
    backgroundColor: style.backgroundColor || 'transparent',
    borderColor: style.borderColor || 'transparent',
    borderWidth: toCssSize(style.borderWidth, '0px'),
    borderStyle: style.borderStyle || 'solid',
    borderRadius: toCssSize(style.borderRadius, '0px'),
    boxShadow: style.boxShadow || 'none',
    minWidth: toCssSize(style.minWidth, undefined),
    maxWidth: toCssSize(style.maxWidth, undefined),
    minHeight: toCssSize(style.minHeight, undefined),
    maxHeight: toCssSize(style.maxHeight, undefined),
    margin: toCssSize(style.margin, '0px'),
    padding: toCssSize(style.padding, '0px'),
    ...parseInlineStyle(style.customStyle),
  }
  if (props.block.blockType === 'tree-panel' && treePanelCollapsed.value) {
    resolvedStyle.width = '100%'
    resolvedStyle.minWidth = '0px'
    resolvedStyle.maxWidth = '100%'
    resolvedStyle.overflow = 'hidden'
  }
  return resolvedStyle
})

const tableColumns = computed(() => [
  ...visibleResolvedFields.value.slice(0, 8).map(field => ({
    key: field.field,
    title: field.label || field.field,
    minWidth: 96,
    align: fieldAlign(field.field),
    ellipsis: { tooltip: true },
    render: row => renderTableCell(field, row),
  })),
  { key: '__actions', title: '操作', width: 96, fixed: 'right' },
])
const aiActionColumn = computed(() => {
  const rowActions = (props.block.props?.customActions || []).filter(action => (action.position || 'row') === 'row' && action.visible !== false)
  if (!rowActions.length)
    return null
  return {
    key: 'actions',
    title: '操作',
    width: Math.max(96, rowActions.length * 58),
    fixed: 'right',
    actions: rowActions.map(action => ({
      key: action.key,
      label: action.label || action.key,
      type: action.type || 'primary',
    })),
    render: () => h('div', { class: 'designer-row-actions' }, rowActions.map(action => h('a', {
      key: action.key,
      href: '#',
      class: `designer-row-action type-${action.type || 'primary'}`,
      title: action.routePath || action.targetPageKey || action.actionType || '',
      onClick: event => event.preventDefault(),
    }, action.label || action.key))),
  }
})
const aiTableColumns = computed(() => visibleResolvedFields.value.slice(0, 8).map(field => ({
  key: field.field,
  field: field.field,
  title: field.label || field.field,
  minWidth: 110,
  align: fieldAlign(field.field),
  ellipsis: { tooltip: true },
  render: row => renderTableCell(field, row),
})).concat(aiActionColumn.value ? [aiActionColumn.value] : []))
const aiSearchSchema = computed(() => resolvedFields.value.slice(0, 4).map(field => toAiFormField(field, 'search')))
const aiFormSchema = computed(() => {
  const formFields = props.fields.length ? props.fields : resolvedFields.value
  return formFields.map(field => toAiFormField(field, 'form'))
})
const blockApiConfig = computed(() => ({
  list: props.block.props?.listApi || '',
  detail: props.block.props?.detailApi || '',
  create: props.block.props?.createApi || '',
  update: props.block.props?.updateApi || '',
  delete: props.block.props?.deleteApi || '',
  import: props.block.props?.importApi || '',
  export: props.block.props?.exportApi || '',
}))
const effectiveRuntimeCrudProps = computed(() => {
  if (!props.runtimeCrudProps)
    return null
  const blockProps = props.block.props || {}
  const rules = normalizeCrudHookRules(blockProps.crudHookRules || {}, blockProps.beforeSubmitRules || [])
  const hookHandlers = CRUD_HOOK_RULE_TARGETS.reduce((handlers, target) => {
    const list = (rules[target.value] || []).filter(rule => rule.field)
    if (list.length)
      handlers[target.value] = data => applyCrudHookRules(data, list)
    return handlers
  }, {})
  const runtimeConfigKey = props.runtimeCrudProps.configKey || ''
  const runtimeBlockApi = resolveCurrentConfigPlaceholder(blockProps.api, runtimeConfigKey)
  const runtimeBlockApiConfig = Object.fromEntries(Object.entries(blockApiConfig.value)
    .map(([key, value]) => [key, resolveCurrentConfigPlaceholder(value, runtimeConfigKey)])
    .filter(([, value]) => value))
  return {
    ...props.runtimeCrudProps,
    ...hookHandlers,
    api: runtimeBlockApi || props.runtimeCrudProps.api || '',
    rowKey: blockProps.rowKey || props.runtimeCrudProps.rowKey || 'id',
    title: blockProps.title || props.runtimeCrudProps.title,
    columns: props.runtimeCrudProps.columns?.length ? props.runtimeCrudProps.columns : aiTableColumns.value,
    searchSchema: props.runtimeCrudProps.searchSchema?.length ? props.runtimeCrudProps.searchSchema : aiSearchSchema.value,
    editSchema: props.runtimeCrudProps.editSchema?.length ? props.runtimeCrudProps.editSchema : aiFormSchema.value,
    apiConfig: {
      ...(props.runtimeCrudProps.apiConfig || {}),
      ...runtimeBlockApiConfig,
    },
    showSearch: blockProps.showSearch ?? props.runtimeCrudProps.showSearch,
    showPagination: blockProps.showPagination ?? props.runtimeCrudProps.showPagination,
    searchGridCols: blockProps.searchGridCols || props.runtimeCrudProps.searchGridCols,
    searchLabelWidth: blockProps.searchLabelWidth || props.runtimeCrudProps.searchLabelWidth,
    searchEnableCollapse: blockProps.searchEnableCollapse ?? props.runtimeCrudProps.searchEnableCollapse,
    searchMaxVisibleFields: blockProps.searchMaxVisibleFields || props.runtimeCrudProps.searchMaxVisibleFields,
    searchYGap: blockProps.searchYGap ?? props.runtimeCrudProps.searchYGap,
    editGridCols: blockProps.editGridCols || props.runtimeCrudProps.editGridCols,
    editLabelWidth: blockProps.editLabelWidth || props.runtimeCrudProps.editLabelWidth,
    editLabelPlacement: blockProps.editLabelPlacement || props.runtimeCrudProps.editLabelPlacement,
    editLabelAlign: blockProps.editLabelAlign || props.runtimeCrudProps.editLabelAlign,
    editSize: blockProps.editSize || props.runtimeCrudProps.editSize,
    editShowFeedback: blockProps.editShowFeedback ?? props.runtimeCrudProps.editShowFeedback,
    editXGap: blockProps.editXGap ?? props.runtimeCrudProps.editXGap,
    editYGap: blockProps.editYGap ?? props.runtimeCrudProps.editYGap,
    modalWidth: blockProps.modalWidth || props.runtimeCrudProps.modalWidth,
    detailModalWidth: blockProps.detailModalWidth || props.runtimeCrudProps.detailModalWidth,
    formOpenMode: resolveEffectiveFormOpenMode(blockProps, props.runtimeCrudProps),
    tabWorkspace: blockProps.tabWorkspace || props.runtimeCrudProps.tabWorkspace,
    modalType: resolveEffectiveModalType(blockProps, props.runtimeCrudProps),
    drawerPlacement: blockProps.drawerPlacement || props.runtimeCrudProps.drawerPlacement,
    hideModalFooter: blockProps.hideModalFooter ?? props.runtimeCrudProps.hideModalFooter,
    hideDefaultDetailContent: blockProps.hideDefaultDetailContent ?? props.runtimeCrudProps.hideDefaultDetailContent,
    hideToolbar: blockProps.hideToolbar ?? props.runtimeCrudProps.hideToolbar,
    hideAdd: blockProps.hideAdd ?? props.runtimeCrudProps.hideAdd,
    hideBatchDelete: blockProps.hideBatchDelete ?? props.runtimeCrudProps.hideBatchDelete,
    showImport: blockProps.showImport ?? props.runtimeCrudProps.showImport ?? true,
    showExport: blockProps.showExport ?? props.runtimeCrudProps.showExport ?? true,
    showExportTasks: blockProps.showExportTasks ?? props.runtimeCrudProps.showExportTasks,
    enableCustomQuery: blockProps.enableCustomQuery ?? props.runtimeCrudProps.enableCustomQuery ?? true,
    addButtonText: blockProps.addButtonText || props.runtimeCrudProps.addButtonText,
    exportButtonText: blockProps.exportButtonText || props.runtimeCrudProps.exportButtonText,
    exportFileName: blockProps.exportFileName || props.runtimeCrudProps.exportFileName,
    renderMode: blockProps.renderMode || props.runtimeCrudProps.renderMode,
    showRenderModeSwitch: blockProps.showRenderModeSwitch ?? props.runtimeCrudProps.showRenderModeSwitch,
    enableTreeAddChild: blockProps.enableTreeAddChild ?? props.runtimeCrudProps.enableTreeAddChild,
    tableSize: blockProps.tableSize || props.runtimeCrudProps.tableSize,
    bordered: blockProps.bordered ?? props.runtimeCrudProps.bordered,
    striped: blockProps.striped ?? props.runtimeCrudProps.striped,
    hideSelection: blockProps.hideSelection ?? props.runtimeCrudProps.hideSelection,
    maxHeight: blockProps.maxHeight || props.runtimeCrudProps.maxHeight,
    scrollX: blockProps.scrollX || props.runtimeCrudProps.scrollX,
    resizable: blockProps.resizable ?? props.runtimeCrudProps.resizable,
    expandConfig: blockProps.expandConfig || props.runtimeCrudProps.expandConfig || {},
    detailPanels: blockProps.detailPanels || props.runtimeCrudProps.detailPanels || [],
    listMethod: blockProps.listMethod || props.runtimeCrudProps.listMethod,
    listDataField: blockProps.listDataField || props.runtimeCrudProps.listDataField,
    listTotalField: blockProps.listTotalField || props.runtimeCrudProps.listTotalField,
    isEncrypt: blockProps.isEncrypt ?? props.runtimeCrudProps.isEncrypt,
    publicParams: {
      ...(props.runtimeCrudProps.publicParams || {}),
      ...(blockProps.publicParams || {}),
    },
    publicQuery: {
      ...(props.runtimeCrudProps.publicQuery || {}),
      ...(blockProps.publicQuery || {}),
    },
    formDefaultValues: {
      ...(props.runtimeCrudProps.formDefaultValues || {}),
      ...(blockProps.formDefaultValues || {}),
    },
    submitDefaultParams: {
      ...(props.runtimeCrudProps.submitDefaultParams || {}),
      ...(blockProps.submitDefaultParams || {}),
    },
  }
})

function resolveEffectiveFormOpenMode(blockProps = {}, runtimeProps = {}) {
  const blockMode = normalizeFormOpenMode(blockProps.formOpenMode)
  const runtimeMode = normalizeFormOpenMode(runtimeProps.formOpenMode)
  if (runtimeMode && runtimeMode !== 'modal')
    return runtimeMode
  if (blockMode && blockMode !== 'modal')
    return blockMode
  return runtimeMode || blockMode || normalizeModalType(runtimeProps.modalType) || normalizeModalType(blockProps.modalType) || 'modal'
}

function resolveEffectiveModalType(blockProps = {}, runtimeProps = {}) {
  const formOpenMode = resolveEffectiveFormOpenMode(blockProps, runtimeProps)
  if (['modal', 'drawer'].includes(formOpenMode))
    return formOpenMode
  return normalizeModalType(runtimeProps.modalType) || normalizeModalType(blockProps.modalType) || 'modal'
}

function normalizeFormOpenMode(value) {
  const mode = String(value || '').trim()
  if (mode === 'tabWorkspace' || mode.toLowerCase() === 'tabworkspace')
    return 'tabWorkspace'
  const normalized = mode.toLowerCase()
  return ['modal', 'drawer', 'flat'].includes(normalized) ? normalized : ''
}

function normalizeModalType(value) {
  const normalized = String(value || '').trim().toLowerCase()
  return ['modal', 'drawer'].includes(normalized) ? normalized : ''
}

function preventStaticCrudSubmit() {
  window.$message?.info('当前是静态结构预览，绑定真实数据接口后即可提交新增数据')
  return false
}

const designerCrudHookHandlers = computed(() => {
  const rules = normalizeCrudHookRules(props.block.props?.crudHookRules || {}, props.block.props?.beforeSubmitRules || [])
  return CRUD_HOOK_RULE_TARGETS.reduce((handlers, target) => {
    const list = (rules[target.value] || []).filter(rule => rule.field)
    if (list.length)
      handlers[target.value] = data => applyCrudHookRules(data, list)
    return handlers
  }, {})
})
const livePreviewRecord = computed(() => {
  const rowKey = props.block.props?.rowKey || props.runtimeCrudProps?.rowKey || 'id'
  const id = props.block.props?.previewRecordId
  return id === undefined || id === null || id === ''
    ? props.runtimeRecord || {}
    : { ...(props.runtimeRecord || {}), [rowKey]: id }
})
const blockTableRowHeight = computed(() => `${Math.max(34, 32 + Number(props.block.props?.rowGap ?? 8))}px`)
const detailInfoGridStyle = computed(() => ({
  gridTemplateColumns: `repeat(${Math.max(1, Math.min(4, Number(props.block.props?.columnCount || 2)))}, minmax(0, 1fr))`,
}))
const resolvedApiConfig = computed(() => ({
  list: props.block.props?.listApi || '',
  detail: props.block.props?.detailApi || '',
  create: props.block.props?.createApi || '',
  update: props.block.props?.updateApi || '',
  delete: props.block.props?.deleteApi || '',
  import: props.block.props?.importApi || '',
  export: props.block.props?.exportApi || '',
}))
const hasConfiguredCrudRequest = computed(() => {
  const blockProps = props.block.props || {}
  return Boolean(blockProps.api || Object.values(resolvedApiConfig.value).some(Boolean))
})
const shouldRequestCrudPreviewApi = computed(() => props.block.props?.previewLiveData === true && hasConfiguredCrudRequest.value)
const isStaticCrudPreview = computed(() => props.block.blockType === 'AiCrudPage' && !shouldRequestCrudPreviewApi.value)
const resolvedDesignerCrudHookHandlers = computed(() => ({
  ...designerCrudHookHandlers.value,
  ...(isStaticCrudPreview.value ? { beforeSubmit: preventStaticCrudSubmit } : {}),
}))
const previewPagination = {
  page: 1,
  pageSize: 10,
  itemCount: 3,
  showSizePicker: true,
  showQuickJumper: true,
}
const toolbarCustomActions = computed(() => (props.block.props?.customActions || [])
  .filter(action => (action.position || 'toolbar') === 'toolbar' && action.visible !== false))
const transferOptions = computed(() => normalizeOptionItems(props.block.props?.options))
const transferValue = computed(() => Array.isArray(props.block.props?.value) ? props.block.props.value : [])
const transferSelectedOptions = computed(() => transferOptions.value.filter(item => transferValue.value.includes(item.value)))
const resolvedTextAlign = computed(() => props.block.props?.textAlign || props.block.props?.align || 'left')
const resolvedTextFontSize = computed(() => Number(props.block.props?.fontSize) || 0)
const resolvedTextFontWeight = computed(() => Number(props.block.props?.fontWeight || props.block.props?.weight || 0) || undefined)
const textContentStyle = computed(() => ({
  color: props.block.props?.color || undefined,
  textAlign: resolvedTextAlign.value,
  fontSize: resolvedTextFontSize.value ? `${resolvedTextFontSize.value}px` : undefined,
  fontWeight: resolvedTextFontWeight.value,
  fontStyle: props.block.props?.fontStyle || 'normal',
  textDecoration: props.block.props?.textDecoration || 'none',
  paddingInlineStart: Number(props.block.props?.indent || 0) > 0 ? `${Number(props.block.props.indent) * 16}px` : undefined,
}))
const pageTitleStyle = computed(() => ({
  textAlign: resolvedTextAlign.value,
  paddingInlineStart: Number(props.block.props?.indent || 0) > 0 ? `${Number(props.block.props.indent) * 16}px` : undefined,
}))
const pageTitleRichContent = computed(() => props.block.props?.content || createLegacyPageTitleContent(
  props.block.props?.title || props.block.label || '页面标题',
  props.block.props?.subtitle || '',
))
const textTitleStyle = computed(() => ({
  color: props.block.props?.color || '#0f172a',
  fontSize: `${resolvedTextFontSize.value || resolveTitleFontSize(props.block.props?.level || 2)}px`,
  fontWeight: resolvedTextFontWeight.value || 800,
  textAlign: resolvedTextAlign.value,
  fontStyle: props.block.props?.fontStyle || 'normal',
  textDecoration: props.block.props?.textDecoration || 'none',
  paddingInlineStart: Number(props.block.props?.indent || 0) > 0 ? `${Number(props.block.props.indent) * 16}px` : undefined,
}))
const paragraphStyle = computed(() => ({
  color: props.block.props?.color || '#475569',
  textAlign: resolvedTextAlign.value,
  fontSize: resolvedTextFontSize.value ? `${resolvedTextFontSize.value}px` : undefined,
  fontWeight: resolvedTextFontWeight.value,
  fontStyle: props.block.props?.fontStyle || 'normal',
  textDecoration: props.block.props?.textDecoration || 'none',
  paddingInlineStart: Number(props.block.props?.indent || 0) > 0 ? `${Number(props.block.props.indent) * 16}px` : undefined,
  lineHeight: Number(props.block.props?.lineHeight || 1.7),
  WebkitLineClamp: Number(props.block.props?.clamp || 0) || undefined,
}))
const paragraphListTag = computed(() => ({ ordered: 'ol', unordered: 'ul' })[props.block.props?.listType] || '')
const paragraphLines = computed(() => String(boundParagraphContent.value || '段落内容').split(/\n+/).filter(Boolean))

function updatePageTitleRichContent(value) {
  emit('inlineTextUpdate', {
    blockId: props.block.id,
    patch: { content: value },
  })
}

function createLegacyPageTitleContent(title, subtitle) {
  const safeTitle = escapeRichText(String(title || '页面标题'))
  const safeSubtitle = escapeRichText(String(subtitle || ''))
  return `<h1>${safeTitle}</h1>${safeSubtitle ? `<p>${safeSubtitle}</p>` : ''}`
}

function escapeRichText(value) {
  return value.replace(/[&<>"']/g, (char) => {
    if (char === '\"')
      return '&quot;'
    if (char === String.fromCharCode(39))
      return '&#39;'
    return ({ '&': '&amp;', '<': '&lt;', '>': '&gt;' })[char]
  })
}
const watermarkContent = computed(() => {
  if (Array.isArray(props.block.props?.content))
    return props.block.props.content
  const text = String(props.block.props?.content || '内部资料')
  return text.includes('\n') ? text.split('\n') : text
})
const watermarkProps = computed(() => ({
  content: watermarkContent.value,
  cross: props.block.props?.cross === true,
  debug: props.block.props?.debug === true,
  fontSize: Number(props.block.props?.fontSize || 14),
  fontFamily: props.block.props?.fontFamily || undefined,
  fontStyle: props.block.props?.fontStyle || 'normal',
  fontVariant: props.block.props?.fontVariant || '',
  fontWeight: Number(props.block.props?.fontWeight || 400),
  fontColor: props.block.props?.fontColor || 'rgba(128, 128, 128, .3)',
  fullscreen: props.block.props?.fullscreen === true,
  globalRotate: Number(props.block.props?.globalRotate || 0),
  lineHeight: Number(props.block.props?.lineHeight || 14),
  height: Number(props.block.props?.height || 32),
  image: props.block.props?.image || undefined,
  imageHeight: toOptionalNumber(props.block.props?.imageHeight),
  imageOpacity: Number(props.block.props?.imageOpacity ?? 1),
  imageWidth: toOptionalNumber(props.block.props?.imageWidth),
  rotate: Number(props.block.props?.rotate || 0),
  selectable: props.block.props?.selectable !== false,
  textAlign: props.block.props?.textAlign || 'left',
  width: Number(props.block.props?.width || 32),
  xGap: Number(props.block.props?.xGap || 0),
  xOffset: Number(props.block.props?.xOffset || 0),
  yGap: Number(props.block.props?.yGap || 0),
  yOffset: Number(props.block.props?.yOffset || 0),
  zIndex: Number(props.block.props?.zIndex || 10),
}))
const avatarInitial = computed(() => String(boundAvatarName.value || '用户').trim().slice(0, 1) || 'U')
const safeIframeSrc = computed(() => sanitizeIframeSrc(props.block.props?.src || ''))
const qrcodeQrOptions = computed(() => ({
  typeNumber: 0,
  mode: 'Byte',
  errorCorrectionLevel: props.block.props?.errorCorrectionLevel || 'Q',
}))
const qrcodeDotsOptions = computed(() => ({
  type: props.block.props?.dotsType || 'square',
  color: props.block.props?.foreground || '#0f172a',
}))
const qrcodeBackgroundOptions = computed(() => ({
  color: props.block.props?.background || '#ffffff',
}))
const qrcodeCornersSquareOptions = computed(() => ({
  type: props.block.props?.cornersSquareType || 'square',
  color: props.block.props?.cornerColor || props.block.props?.foreground || '#0f172a',
}))
const qrcodeCornersDotOptions = computed(() => ({
  type: props.block.props?.cornersDotType || 'square',
  color: props.block.props?.cornerColor || props.block.props?.foreground || '#0f172a',
}))
const boxLayoutStyle = computed(() => ({
  display: 'flex',
  flexDirection: props.block.props?.direction || 'row',
  flexWrap: props.block.props?.wrap === false ? 'nowrap' : 'wrap',
  alignItems: props.block.props?.alignItems || 'stretch',
  justifyContent: props.block.props?.justifyContent || 'flex-start',
  gap: `${Number(props.block.props?.gap ?? 12)}px`,
}))
const spacePreviewStyle = computed(() => ({
  '--space-size': `${Number(props.block.props?.size || 24)}px`,
}))
const descriptionGridStyle = computed(() => ({
  gridTemplateColumns: `repeat(${Math.max(1, Math.min(4, Number(props.block.props?.columnCount || 2)))}, minmax(0, 1fr))`,
}))

const sampleRows = computed(() => Array.from({ length: 3 }).map((_, idx) => {
  const row = { id: idx + 1, __actions: '编辑' }
  visibleResolvedFields.value.slice(0, 8).forEach((field) => {
    row[field.field] = sampleValue(field, idx)
  })
  return row
}))

function componentType(field) {
  return field?.componentType || field?.dataType || 'input'
}

function normalizeOptionItems(options = []) {
  return (Array.isArray(options) ? options : []).map((item, index) => {
    if (typeof item === 'string')
      return { label: item, value: item }
    return {
      label: item?.label || item?.value || `选项${index + 1}`,
      value: item?.value || item?.label || `option${index + 1}`,
      disabled: item?.disabled === true,
    }
  })
}

function resolveTitleFontSize(level = 2) {
  const map = { 1: 28, 2: 22, 3: 18, 4: 16, 5: 14, 6: 13 }
  return map[Number(level)] || 22
}

function sanitizeIframeSrc(value = '') {
  const text = String(value || '').trim()
  if (!/^https?:\/\//i.test(text))
    return ''
  return text
}

function hasAction(key) {
  return Array.isArray(props.block.props?.actions) && props.block.props.actions.includes(key)
}

function fieldAlign(fieldName) {
  const align = props.block.props?.fieldSettings?.[fieldName]?.align
  if (['left', 'center', 'right'].includes(align))
    return align
  const globalAlign = props.block.props?.globalAlign
  return ['left', 'center', 'right'].includes(globalAlign) ? globalAlign : 'left'
}

function toOptionalNumber(value) {
  const number = Number(value)
  return Number.isFinite(number) && value !== '' && value !== undefined && value !== null ? number : undefined
}

function fieldSetting(fieldName) {
  return props.block.props?.fieldSettings?.[fieldName] || {}
}

function renderTableCell(field, row = {}) {
  const setting = fieldSetting(field.field)
  return h(FieldValueRenderer, {
    value: row[field.field],
    row,
    field,
    setting,
    context: runtimeRuleContext.value,
  })
}

function toAiFormField(field, mode = 'form') {
  return {
    field: field.field,
    label: field.label || field.field,
    type: resolveAiFieldType(field),
    placeholder: mode === 'search' ? `请输入${field.label || field.field}` : field.placeholder || `请输入${field.label || field.field}`,
    span: field.span || 1,
    clearable: true,
    options: field.options || [],
    dictType: field.dictType || '',
  }
}

function resolveAiFieldType(field) {
  if (field.dictType)
    return 'dictSelect'
  const type = field.componentType || field.dataType || 'input'
  if (['int', 'bigint', 'decimal', 'number'].includes(type))
    return 'number'
  if (['datetime', 'date', 'time'].includes(type))
    return type
  if (['textarea', 'select', 'checkbox', 'radio', 'switch'].includes(type))
    return type
  return 'input'
}

function trendClass(trend) {
  if (typeof trend !== 'string')
    return ''
  if (trend.startsWith('+'))
    return 'up'
  if (trend.startsWith('-'))
    return 'down'
  return ''
}

function sampleValue(field, idx) {
  if (!field)
    return '-'
  if (field.dictType)
    return '字典'
  if (field.componentType === 'switch' || field.dataType === 'tinyint')
    return idx % 2 === 0 ? '是' : '否'
  if (['int', 'bigint', 'decimal'].includes(field.dataType))
    return field.dataType === 'decimal' ? `${100 + idx}.00` : String(100 + idx)
  if (['date'].includes(field.dataType))
    return '2026-05-21'
  if (['datetime'].includes(field.dataType))
    return '2026-05-21 09:30:00'
  return field.label ? `${field.label}${idx + 1}` : `示例${idx + 1}`
}

function detailValue(field, idx) {
  const record = detailInfoRecord.value || {}
  const candidates = [
    field?.field,
    field?.sourceField,
    field?.columnName,
    field?.field ? `${field.field}Name` : '',
  ].filter(Boolean)
  for (const key of candidates) {
    const value = record[key]
    if (value !== undefined && value !== null && value !== '')
      return value
  }
  if (Object.keys(record).length)
    return '-'
  return sampleValue(field, idx)
}

function normalizeBlockBoundRows(fallbackRows = []) {
  const rows = extractBlockBoundRows(blockBoundData.value)
  const sourceRows = rows.length ? rows : (Array.isArray(fallbackRows) ? fallbackRows : [])
  return sourceRows.map((row, index) => normalizeBlockRow(row, index))
}

function extractBlockBoundRows(data) {
  if (Array.isArray(data))
    return data
  if (!data || typeof data !== 'object')
    return []
  const candidates = [
    data.records,
    data.list,
    data.rows,
    data.items,
    data.children,
    data.data,
  ]
  const rows = candidates.find(Array.isArray)
  return rows || []
}

function normalizeBlockRow(row, index = 0) {
  if (row && typeof row === 'object')
    return row
  return {
    label: String(row ?? `项目${index + 1}`),
    title: String(row ?? `项目${index + 1}`),
    value: row,
    content: String(row ?? ''),
  }
}

function boundContentValue(fallback = '', fieldKey = 'contentField', defaultField = 'content') {
  const data = blockBoundData.value
  if (data === undefined || data === null)
    return fallback
  if (Array.isArray(data))
    return fallback
  if (typeof data !== 'object')
    return fieldKey === 'valueField' || fieldKey === 'contentField' ? data : fallback
  return readBoundField(data, fieldKey, defaultField, fallback)
}

function boundRowField(row = {}, fieldKey = 'valueField', defaultField = 'value', fallback = '') {
  if (row === undefined || row === null)
    return fallback
  if (typeof row !== 'object')
    return fieldKey === 'valueField' || fieldKey === 'contentField' ? row : fallback
  return readBoundField(row, fieldKey, defaultField, fallback)
}

function readBoundField(source = {}, fieldKey = 'valueField', defaultField = 'value', fallback = '') {
  const binding = props.block.props?.dataBinding || {}
  const fieldName = binding[fieldKey] || defaultField
  const candidates = Array.from(new Set([fieldName, defaultField].filter(Boolean)))
  for (const key of candidates) {
    const value = getNestedRecordValue(source, key)
    if (value !== undefined && value !== null && value !== '')
      return value
  }
  return fallback
}

async function loadBlockBindingData() {
  const binding = props.block.props?.dataBinding || {}
  if (!isLocalDataBindableBlock.value || binding.enabled !== true || binding.sourceType !== 'remote') {
    remoteBlockBindingData.value = null
    blockBindingError.value = ''
    blockBindingLoading.value = false
    return
  }
  if (!binding.api) {
    remoteBlockBindingData.value = null
    blockBindingError.value = '未配置数据接口'
    blockBindingLoading.value = false
    return
  }
  blockBindingLoading.value = true
  blockBindingError.value = ''
  try {
    const apiConfig = String(binding.api).includes('@') ? binding.api : `${binding.method || 'get'}@${binding.api}`
    const { method, url } = parseApiConfigValue(apiConfig)
    const params = normalizeBlockBindingParams(binding.paramsText || '{}')
    const response = await request({
      method,
      url: interpolateText(url, props.runtimeRecord || {}),
      params: method === 'get' ? params : undefined,
      data: method === 'get' ? undefined : params,
      needTip: false,
    })
    remoteBlockBindingData.value = response?.data ?? response
  }
  catch (error) {
    remoteBlockBindingData.value = null
    blockBindingError.value = `数据加载失败：${error?.message || '请检查接口配置'}`
  }
  finally {
    blockBindingLoading.value = false
  }
}

function normalizeBlockBindingParams(value = '{}') {
  try {
    const parsed = typeof value === 'string' ? JSON.parse(value || '{}') : value
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed))
      return {}
    return Object.fromEntries(Object.entries(parsed).map(([key, item]) => [
      key,
      typeof item === 'string' ? interpolateText(item, props.runtimeRecord || {}) : item,
    ]))
  }
  catch {
    return {}
  }
}

async function loadDetailInfoRecord() {
  if (props.block.blockType !== 'detail-info' || props.block.props?.dataSourceType !== 'remote') {
    remoteDetailInfoRecord.value = {}
    detailInfoError.value = ''
    return
  }
  const api = props.block.props?.detailApi || ''
  if (!api) {
    remoteDetailInfoRecord.value = {}
    detailInfoError.value = '未配置详情接口'
    return
  }
  detailInfoLoading.value = true
  detailInfoError.value = ''
  try {
    const apiConfig = String(api).includes('@') ? api : `${props.block.props?.detailMethod || 'get'}@${api}`
    const { method, url } = parseApiConfigValue(apiConfig)
    const params = normalizeDetailInfoParams(props.block.props?.paramsText || '{}')
    const response = await request({
      method,
      url: interpolateText(url, props.runtimeRecord || {}),
      params: method === 'get' ? params : undefined,
      data: method === 'get' ? undefined : params,
      needTip: false,
    })
    const data = response?.data ?? response
    remoteDetailInfoRecord.value = props.block.props?.dataPath ? getNestedRecordValue(data, props.block.props.dataPath) || {} : data || {}
  }
  catch (error) {
    remoteDetailInfoRecord.value = {}
    detailInfoError.value = `详情加载失败：${error?.message || '请检查接口配置'}`
  }
  finally {
    detailInfoLoading.value = false
  }
}

function normalizeDetailInfoParams(value = '{}') {
  try {
    const parsed = typeof value === 'string' ? JSON.parse(value || '{}') : value
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed))
      return {}
    return Object.fromEntries(Object.entries(parsed).map(([key, item]) => [
      key,
      typeof item === 'string' ? interpolateText(item, props.runtimeRecord || {}) : item,
    ]))
  }
  catch {
    return {}
  }
}

function interpolateText(value = '', data = {}) {
  return String(value || '').replace(/\{\{\s*([\w.]+)\s*\}\}/g, (_, key) => {
    const result = getNestedRecordValue(data, key)
    return result === undefined || result === null ? '' : String(result)
  })
}

function getNestedRecordValue(source = {}, path = '') {
  return String(path || '')
    .split('.')
    .filter(Boolean)
    .reduce((value, key) => value?.[key], source)
}

async function loadRuntimeTree() {
  if (!props.readonly || props.block.blockType !== 'tree-panel' || !props.runtimeCrudProps)
    return
  const treeApi = effectiveRuntimeCrudProps.value?.apiConfig?.tree
  if (!treeApi)
    return
  runtimeTreeLoading.value = true
  try {
    const { method, url } = parseApiConfigValue(treeApi)
    const response = await request({
      method,
      url,
      params: {
        loadMode: props.block.props?.loadMode || 'full',
      },
      needTip: false,
    })
    const rows = extractRuntimeTreeRows(response)
    const nodeMap = new Map()
    runtimeTreeNodes.value = normalizeRuntimeTreeNodes(rows, nodeMap)
    runtimeTreeNodeMap.value = nodeMap
    runtimeExpandedTreeKeys.value = collectTreeKeys(runtimeTreeNodes.value, runtimeTreeChildrenField.value)
  }
  catch (error) {
    runtimeTreeNodes.value = []
    runtimeTreeNodeMap.value = new Map()
    console.warn('[GridBlockRenderer] 加载筛选树失败', error?.message || error)
  }
  finally {
    runtimeTreeLoading.value = false
  }
}

function parseApiConfigValue(apiConfigValue) {
  const text = String(apiConfigValue || '')
  const [method, ...urlParts] = text.includes('@') ? text.split('@') : ['get', text]
  return {
    method: String(method || 'get').toLowerCase(),
    url: urlParts.join('@') || text,
  }
}

function extractRuntimeTreeRows(response) {
  const data = response?.data
  if (Array.isArray(data))
    return data
  if (Array.isArray(data?.records))
    return data.records
  if (Array.isArray(data?.list))
    return data.list
  if (Array.isArray(data?.children))
    return data.children
  return []
}

function normalizeRuntimeTreeNodes(rows = [], nodeMap = new Map()) {
  const keyField = props.block.props?.keyField || 'id'
  const labelField = props.block.props?.labelField || 'label'
  const childrenField = runtimeTreeChildrenField.value
  return (Array.isArray(rows) ? rows : []).map((row) => {
    const rawKey = row?.[keyField] ?? row?.key ?? row?.id
    const key = String(rawKey ?? '')
    const label = row?.[labelField] ?? row?.label ?? row?.name ?? key
    const children = normalizeRuntimeTreeNodes(row?.[childrenField] || [], nodeMap)
    const node = {
      ...(row || {}),
      key,
      label: String(label || '-'),
      [childrenField]: children,
    }
    if (key)
      nodeMap.set(key, node)
    return node
  })
}

function countTreeNodes(nodes = [], childrenField = 'children') {
  return (Array.isArray(nodes) ? nodes : []).reduce((count, node) => {
    return count + 1 + countTreeNodes(node?.[childrenField] || [], childrenField)
  }, 0)
}

function collectTreeKeys(nodes = [], childrenField = 'children') {
  return (Array.isArray(nodes) ? nodes : []).flatMap((node) => {
    const key = node?.key === undefined || node?.key === null ? '' : String(node.key)
    const childKeys = collectTreeKeys(node?.[childrenField] || [], childrenField)
    return key ? [key, ...childKeys] : childKeys
  })
}

function expandTree() {
  if (props.readonly && props.runtimeCrudProps) {
    runtimeExpandedTreeKeys.value = collectTreeKeys(runtimeTreeNodes.value, runtimeTreeChildrenField.value)
    return
  }
  previewTreeExpanded.value = true
}

function collapseTree() {
  if (props.readonly && props.runtimeCrudProps) {
    runtimeExpandedTreeKeys.value = []
    return
  }
  previewTreeExpanded.value = false
}

function toggleTreePanel() {
  treePanelCollapsed.value = !treePanelCollapsed.value
  emit('treePanelCollapseChange', {
    id: props.block.id,
    collapsed: treePanelCollapsed.value,
  })
}

function clearRuntimeTreeSelection() {
  emit('runtimeTreeSelect', {
    key: '__all__',
    clear: true,
    filterField: props.block.props?.filterField || '',
  })
}

function handleRuntimeTreeSelected(keys = []) {
  const key = keys?.[0]
  if (!key) {
    clearRuntimeTreeSelection()
    return
  }
  const node = runtimeTreeNodeMap.value.get(String(key)) || {}
  const targetField = props.block.props?.targetField || props.block.props?.keyField || 'id'
  const filterField = props.block.props?.filterField || ''
  const value = node?.[targetField] ?? node?.targetValue ?? node?.value ?? key
  emit('runtimeTreeSelect', {
    key,
    node,
    filterField,
    value,
  })
}

function emitCrudPreviewState(patch = {}) {
  if (props.block.blockType !== 'AiCrudPage')
    return
  emit('crudPreviewStateChange', {
    blockId: props.block.id,
    patch,
  })
}

function handleCrudPreviewSuccess(payload = {}) {
  if (props.block.props?.previewLiveData !== true)
    return
  emitCrudPreviewState({
    lastPreviewStatus: 'success',
    lastPreviewError: '',
    lastPreviewMessage: `接口预览成功，读取 ${Number(payload.total ?? payload.list?.length ?? 0)} 条数据`,
  })
  applyCrudPreviewMode(payload?.list || [])
}

function handleCrudPreviewError(error) {
  if (props.block.props?.previewLiveData !== true)
    return
  emitCrudPreviewState({
    lastPreviewStatus: 'error',
    lastPreviewError: error?.message || '接口请求失败',
    lastPreviewMessage: '接口预览失败，请检查接口地址、响应字段或权限',
  })
}

async function applyCrudPreviewMode(list = []) {
  await Promise.resolve()
  const mode = crudPreviewMode.value
  if (!['create', 'edit', 'detail'].includes(mode))
    return
  const crud = runtimeCrudRef.value
  if (!crud)
    return
  if (mode === 'create') {
    crud.showAdd?.(props.block.props?.formDefaultValues || {})
    emitCrudPreviewState({
      lastPreviewStatus: 'success',
      lastPreviewMessage: '新增表单预览已打开',
      lastPreviewError: '',
    })
    return
  }
  const row = Object.keys(livePreviewRecord.value || {}).length
    ? livePreviewRecord.value
    : list[0]
  if (!row) {
    emitCrudPreviewState({
      lastPreviewStatus: 'error',
      lastPreviewError: '缺少预览记录，请填写记录 ID 或确保列表接口返回数据',
      lastPreviewMessage: '无法打开编辑/详情预览',
    })
    return
  }
  if (mode === 'edit') {
    await crud.showEdit?.(row)
    emitCrudPreviewState({
      lastPreviewStatus: 'success',
      lastPreviewMessage: '编辑表单预览已打开',
      lastPreviewError: '',
    })
    return
  }
  await crud.showDetail?.(row)
  emitCrudPreviewState({
    lastPreviewStatus: 'success',
    lastPreviewMessage: '详情状态预览已打开',
    lastPreviewError: '',
  })
}

function handleBackClick() {
  if (!props.readonly)
    return
  if (props.block.props?.action === 'navigate' && props.block.props?.targetPageKey) {
    const query = { ...route.query, pageKey: props.block.props.targetPageKey }
    ;['mode', 'id', 'recordId'].forEach(key => delete query[key])
    if (props.block.props.targetFormKey)
      query.formKey = props.block.props.targetFormKey
    else
      delete query.formKey
    router.replace({
      path: route.path,
      query,
      hash: route.hash,
    })
    return
  }
  if (window.history.length > 1) {
    router.back()
    return
  }
  const query = { ...route.query }
  ;['pageKey', 'formKey', 'mode', 'id', 'recordId'].forEach(key => delete query[key])
  router.replace({
    path: route.path,
    query,
    hash: route.hash,
  })
}

async function handleActionButtonClick() {
  if (!props.readonly)
    return
  const eventItem = resolvePrimaryClickEvent()
  if (!eventItem || eventItem.action === 'none')
    return
  if (eventItem.confirmText && !(await confirmRuntimeAction(resolveRuntimeText(eventItem.confirmText))))
    return
  if (eventItem.action === 'navigate') {
    navigateRuntimeEvent(eventItem)
    return
  }
  if (eventItem.action === 'request') {
    await requestRuntimeEvent(eventItem)
    handleRuntimeEventSuccess(eventItem)
  }
}

function resolvePrimaryClickEvent() {
  const events = Array.isArray(props.block.props?.events) ? props.block.props.events : []
  return events.find(item => (item.trigger || 'click') === 'click') || null
}

function navigateRuntimeEvent(eventItem = {}) {
  if (!eventItem.targetPageKey)
    return
  const query = {
    ...route.query,
    pageKey: eventItem.targetPageKey,
  }
  if (eventItem.targetFormKey)
    query.formKey = eventItem.targetFormKey
  else
    delete query.formKey
  appendRuntimeEventParams(query, eventItem.params || [])
  router.push({ path: route.path, query, hash: route.hash })
}

async function requestRuntimeEvent(eventItem = {}) {
  const apiConfig = parseRuntimeApiConfig(eventItem.requestUrl)
  if (!apiConfig.url)
    return
  const payload = {}
  appendRuntimeEventParams(payload, eventItem.params || [])
  await request({
    method: apiConfig.method,
    url: apiConfig.url,
    ...(apiConfig.method === 'get' ? { params: payload } : { data: payload }),
  })
}

function appendRuntimeEventParams(target = {}, params = []) {
  ;(Array.isArray(params) ? params : []).forEach((param) => {
    const name = String(param?.name || '').trim()
    if (!name)
      return
    const value = resolveRuntimeParamValue(param)
    if (value !== undefined && value !== '')
      target[name] = value
  })
  return target
}

function resolveRuntimeParamValue(param = {}) {
  const sourceType = param.sourceType || 'static'
  const sourceField = String(param.sourceField || '').trim()
  if (sourceType === 'rowField' && sourceField)
    return props.runtimeRecord?.[sourceField] ?? ''
  if (sourceType === 'routeQuery' && sourceField)
    return route.query?.[sourceField] ?? ''
  if (sourceType === 'system' && sourceField)
    return resolveSystemRuntimeParam(sourceField)
  return resolveRuntimeText(param.value || '')
}

function resolveRuntimeText(template = '') {
  let text = String(template || '')
  const data = props.runtimeRecord || {}
  Object.keys(data).forEach((key) => {
    const value = data[key]
    if (value === null || value === undefined || value === '')
      return
    text = text.replaceAll(`:${key}`, value)
    text = text.replaceAll(`\${${key}}`, value)
  })
  Object.keys(route.query || {}).forEach((key) => {
    const value = route.query[key]
    if (value === null || value === undefined || value === '')
      return
    text = text.replaceAll(resolveTemplatePlaceholder(`route.${key}`), Array.isArray(value) ? value[0] : value)
  })
  return text
    .replaceAll(resolveTemplatePlaceholder('system.now'), new Date().toISOString())
    .replaceAll(resolveTemplatePlaceholder('system.today'), new Date().toISOString().slice(0, 10))
}

function resolveTemplatePlaceholder(name = '') {
  return ['$', '{', name, '}'].join('')
}

function resolveSystemRuntimeParam(sourceField = '') {
  if (sourceField === 'now')
    return new Date().toISOString()
  if (sourceField === 'today')
    return new Date().toISOString().slice(0, 10)
  if (sourceField === 'tenantId')
    return route.query?.tenantId || ''
  return ''
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
  return matchSimpleExpression(expression, row)
}

function parseRuntimeApiConfig(value = '') {
  const text = String(value || '').trim()
  const [method, ...urlParts] = text.includes('@') ? text.split('@') : ['post', text]
  return {
    method: String(method || 'post').toLowerCase(),
    url: urlParts.join('@') || text,
  }
}

function handleRuntimeEventSuccess(eventItem = {}) {
  if (eventItem.successBehavior === 'goBack') {
    router.back()
  }
  else if (eventItem.successBehavior === 'refreshList') {
    router.replace({
      path: route.path,
      query: { ...route.query, _refresh: Date.now() },
      hash: route.hash,
    })
  }
}

function confirmRuntimeAction(message = '确认执行该操作？') {
  if (!window.$dialog?.warning) {
    const nativeConfirm = globalThis?.confirm
    return Promise.resolve(typeof nativeConfirm !== 'function' || nativeConfirm(message))
  }
  return new Promise((resolve) => {
    window.$dialog.warning({
      title: '确认操作',
      content: message,
      positiveText: '确定',
      negativeText: '取消',
      onPositiveClick: () => resolve(true),
      onNegativeClick: () => resolve(false),
      onClose: () => resolve(false),
      onMaskClick: () => resolve(false),
    })
  })
}

function toCssSize(value, fallback = '') {
  if (value === null || value === undefined || value === '')
    return fallback
  if (typeof value === 'number')
    return `${value}px`
  const text = String(value).trim()
  return /^\d+(?:\.\d+)?$/.test(text) ? `${text}px` : text
}

function parseInlineStyle(value = '') {
  if (!value || typeof value !== 'string')
    return {}
  return value.split(';').reduce((style, entry) => {
    const [rawKey, ...rawValue] = entry.split(':')
    const key = rawKey?.trim()
    const cssValue = rawValue.join(':').trim()
    if (!key || !cssValue)
      return style
    const camelKey = key.replace(/-([a-z])/g, (_, char) => char.toUpperCase())
    style[camelKey] = cssValue
    return style
  }, {})
}

defineExpose({
  getRuntimeCrudRef: () => runtimeCrudRef.value,
})

onMounted(() => {
  loadRuntimeTree()
  loadDetailInfoRecord()
  loadBlockBindingData()
})

watch(
  () => [
    props.block.blockType,
    props.block.props?.sourceModelCode,
    props.block.props?.keyField,
    props.block.props?.parentField,
    props.block.props?.labelField,
    props.block.props?.loadMode,
    props.runtimeCrudProps?.apiConfig?.tree,
  ],
  () => loadRuntimeTree(),
)

watch(
  () => [
    props.block.blockType,
    props.block.props?.dataSourceType,
    props.block.props?.detailApi,
    props.block.props?.detailMethod,
    props.block.props?.paramsText,
    props.block.props?.dataPath,
    props.runtimeRecord,
  ],
  () => loadDetailInfoRecord(),
)

watch(
  () => [
    props.block.blockType,
    props.block.props?.dataBinding?.enabled,
    props.block.props?.dataBinding?.sourceType,
    props.block.props?.dataBinding?.api,
    props.block.props?.dataBinding?.method,
    props.block.props?.dataBinding?.paramsText,
    props.block.props?.dataBinding?.dataPath,
    props.block.props?.dataBinding?.contextPath,
    props.runtimeRecord,
  ],
  () => loadBlockBindingData(),
)

watch(
  () => [
    props.block.props?.previewLiveData,
    crudPreviewMode.value,
    props.block.props?.previewRecordId,
  ],
  () => {
    if (props.block.blockType !== 'AiCrudPage' || props.block.props?.previewLiveData !== true)
      return
    emitCrudPreviewState({
      lastPreviewStatus: 'loading',
      lastPreviewMessage: '正在请求真实接口预览',
      lastPreviewError: '',
    })
    runtimeCrudRef.value?.loadList?.()
  },
)
</script>

<style scoped>
.grid-block {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 0;
  border: 0;
  border-radius: 0;
  background: transparent;
  overflow: hidden;
}

.grid-block.block-AiCrudPage {
  display: flex;
  min-width: 0;
  /* CRUD 的实际高度由页面区块框决定。这里不能再设固定最小高度，
   * 否则当用户缩小区块时内容会越出自己的框并压住相邻组件。 */
  min-height: 0;
  overflow: hidden;
}

.grid-block.block-page-title,
.grid-block.block-page-title:focus-within {
  position: relative;
  /* 操作层（拖拽、更多、缩放锚点）在应用页壳上，标题正文不应盖住它。 */
  z-index: 1;
  overflow: visible;
}

.grid-block.selected {
  border-color: #bfd2ea;
  background: #fafdff;
  box-shadow: inset 0 0 0 1px rgba(37, 99, 235, 0.06);
}

.block-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.block-header strong {
  font-size: 13px;
  color: #0f172a;
}

.block-meta {
  font-size: 11px;
  color: #64748b;
}

.block-empty {
  color: #94a3b8;
  font-size: 12px;
  padding: 8px;
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
  border-radius: 6px;
  text-align: center;
}

.block-empty.inline {
  display: inline-block;
  padding: 4px 8px;
}

.search-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 8px 12px;
}

.search-actions {
  display: flex;
  gap: 6px;
  margin-top: auto;
  padding-top: 4px;
  border-top: 1px dashed #e5e7eb;
}

.toolbar-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}

.back-button-preview {
  display: flex;
  align-items: center;
  height: 100%;
}

.back-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 30px;
  padding: 0 10px 0 6px;
  border: 1px solid transparent;
  border-radius: 6px;
  background: transparent;
  color: #475569;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}

.back-chip:hover {
  border-color: #cbd5e1;
  background: #fff;
  color: #0f172a;
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.08);
}

.back-icon {
  display: grid;
  place-items: center;
  width: 18px;
  height: 18px;
  color: currentColor;
}

.page-title-preview {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  height: 100%;
  min-height: 56px;
}

.page-title-rich-editor {
  width: min(100%, 920px);
  min-height: 0;
  height: 100%;
  overflow: visible;
  color: #1f2329;
  cursor: text;
  line-height: 1.3;
}

.page-title-rich-editor :deep(*) {
  cursor: text;
}

.page-title-rich-editor :deep(h1) {
  margin: 0;
  color: #0f172a;
  font-size: 28px;
  font-weight: 800;
  line-height: 1.3;
}

.page-title-preview.size-small .page-title-rich-editor :deep(h1) {
  font-size: 20px;
}

.page-title-preview.size-large .page-title-rich-editor :deep(h1) {
  font-size: 32px;
}

.page-title-rich-editor :deep(p) {
  margin: 4px 0 0;
  color: rgba(100, 106, 115, 0.79);
  font-size: 16px;
  line-height: 1.5;
}

.page-title-rich-editor :deep(ul),
.page-title-rich-editor :deep(ol) {
  margin: 6px 0;
  padding-left: 22px;
}

.block-table {
  flex: 1;
  min-height: 0;
}

.designer-table-link {
  color: #2563eb;
  font-weight: 600;
  text-decoration: none;
  cursor: pointer;
}

.designer-table-link:hover {
  text-decoration: underline;
}

.designer-dict-tag {
  display: inline-flex;
  align-items: center;
  max-width: 100%;
  min-height: 22px;
  padding: 0 8px;
  border: 1px solid #bfdbfe;
  border-radius: 999px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 700;
  line-height: 20px;
  vertical-align: middle;
}

.designer-row-actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.designer-row-action {
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
  text-decoration: none;
}

.designer-row-action.type-error {
  color: #dc2626;
}

.designer-row-action.type-warning {
  color: #d97706;
}

.designer-row-action.type-success {
  color: #16a34a;
}

.detail-info-preview {
  display: grid;
  gap: 10px;
  min-height: 0;
}

.detail-info-grid {
  display: grid;
  gap: 10px 18px;
  min-height: 0;
}

.detail-info-item {
  min-width: 0;
  padding: 2px 0 8px;
  border-bottom: 1px solid #eef2f7;
}

.detail-info-item.bordered {
  padding: 10px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
}

.detail-label {
  margin-bottom: 4px;
  color: #64748b;
  font-size: 12px;
}

.detail-value {
  min-width: 0;
  overflow: hidden;
  color: #0f172a;
  font-size: 13px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detail-info-state {
  border: 1px dashed #bfdbfe;
  border-radius: 8px;
  background: #eff6ff;
  padding: 8px 10px;
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 600;
}

.detail-info-state.error {
  border-color: #fecaca;
  background: #fff1f2;
  color: #dc2626;
}

.block-binding-state {
  position: absolute;
  right: 8px;
  bottom: 8px;
  z-index: 12;
  max-width: calc(100% - 16px);
  padding: 3px 8px;
  overflow: hidden;
  border-radius: 999px;
  background: rgba(239, 246, 255, 0.94);
  color: #2563eb;
  font-size: 11px;
  line-height: 1.5;
  text-overflow: ellipsis;
  white-space: nowrap;
  pointer-events: none;
}

.block-binding-state.error {
  background: rgba(254, 242, 242, 0.96);
  color: #dc2626;
}

.block-table :deep(.n-data-table-tr) {
  height: var(--block-table-row-height, 40px);
}

.layout-grid-preview {
  display: grid;
  width: 100%;
  height: auto;
  min-width: 0;
  min-height: 100%;
}

.layout-grid-cell {
  position: relative;
  min-width: 0;
  min-height: var(--grid-cell-min-height, 120px);
  padding: 8px;
  border-radius: 6px;
  transition:
    border-color 160ms ease,
    background-color 160ms ease;
}

.layout-grid-cell.bordered {
  border: 1px dashed rgba(148, 163, 184, 0.42);
}

.layout-grid-cell::after {
  position: absolute;
  inset: 4px;
  pointer-events: none;
  content: '';
  border: 2px solid transparent;
  border-radius: 7px;
  transition:
    border-color 160ms ease,
    background-color 160ms ease;
}

.layout-grid-cell:hover,
.layout-grid-cell.is-drop-active {
  background-color: rgba(37, 99, 235, 0.04);
}

.layout-grid-cell.is-drop-active::after {
  border-color: rgba(37, 99, 235, 0.72);
  background: rgba(37, 99, 235, 0.06);
}

.layout-grid-preview.is-nested-moving .layout-grid-cell {
  background: transparent;
}

.layout-grid-preview.is-nested-moving .layout-grid-cell.bordered {
  border-color: transparent;
}

.layout-grid-preview.is-nested-moving .layout-grid-cell::after {
  border-color: transparent;
  background: transparent;
}

.layout-grid-preview.is-nested-moving .layout-grid-cell.is-drop-active {
  background: rgba(239, 246, 255, 0.72);
}

.layout-grid-cell-body {
  position: relative;
  z-index: 2;
  display: grid;
  gap: 8px;
  min-width: 0;
  min-height: 0;
}

.layout-grid-cell-child {
  position: relative;
  min-width: 0;
  min-height: 0;
  border: 1px solid transparent;
  border-radius: 6px;
  transition:
    border-color 160ms ease,
    box-shadow 160ms ease;
  cursor: pointer;
}

.layout-grid-preview.is-nested-moving .layout-grid-cell-child {
  border-color: transparent;
  box-shadow: none;
}

.layout-grid-cell-child.is-moving-source {
  opacity: 0.2;
  border-color: transparent !important;
  box-shadow: none !important;
  pointer-events: none;
}

.layout-grid-cell-child.is-moving-source .nested-block-node-overlay,
.layout-grid-cell-child.is-moving-source .nested-resize-anchor {
  display: none;
}

.nested-block-node-overlay {
  position: absolute;
  top: 6px;
  right: 6px;
  left: 6px;
  z-index: 36;
  height: 24px;
  opacity: 0;
  pointer-events: auto;
  transition: opacity 160ms ease;
}

.layout-grid-cell-child:hover .nested-block-node-overlay,
.layout-grid-cell-child.selected .nested-block-node-overlay {
  opacity: 1;
}

.nested-block-drag-handle,
.nested-block-menu-trigger {
  display: grid;
  place-items: center;
  width: 26px;
  height: 24px;
  border: 1px solid #bfdbfe;
  border-radius: 5px;
  background: #eff6ff;
  color: #1d4ed8;
  box-shadow: 0 6px 14px rgba(37, 99, 235, 0.14);
}

.nested-block-drag-handle {
  position: absolute;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  cursor: grab;
}

.nested-block-drag-handle svg {
  width: 15px;
  height: 15px;
  transform: rotate(90deg);
}

.nested-block-menu-trigger {
  position: absolute;
  top: 0;
  right: 0;
  padding: 0;
  background: #1d4ed8;
  color: #fff;
  cursor: pointer;
}

.nested-block-drag-handle:hover,
.nested-block-menu-trigger:hover {
  background: #2563eb;
  color: #fff;
}

.nested-block-drag-handle:active {
  cursor: grabbing;
}

:global(.list-grid-nested-moving),
:global(.list-grid-nested-moving *) {
  cursor: grabbing !important;
  user-select: none !important;
}

.nested-resize-anchor {
  position: absolute;
  z-index: 38;
  display: block;
  width: 10px;
  height: 10px;
  border: 2px solid #fff;
  border-radius: 999px;
  background: #1d4ed8;
  box-shadow: 0 2px 6px rgba(29, 78, 216, 0.36);
}

.nested-resize-anchor::before {
  position: absolute;
  inset: -8px;
  content: '';
  border-radius: 999px;
}

.nested-resize-anchor.anchor-top-left {
  top: -7px;
  left: -7px;
  cursor: nwse-resize;
}

.nested-resize-anchor.anchor-top {
  top: -7px;
  left: 50%;
  cursor: ns-resize;
  transform: translateX(-50%);
}

.nested-resize-anchor.anchor-top-right {
  top: -7px;
  right: -7px;
  cursor: nesw-resize;
}

.nested-resize-anchor.anchor-right {
  top: 50%;
  right: -7px;
  cursor: ew-resize;
  transform: translateY(-50%);
}

.nested-resize-anchor.anchor-bottom-right {
  right: -7px;
  bottom: -7px;
  cursor: nwse-resize;
}

.nested-resize-anchor.anchor-bottom {
  bottom: -7px;
  left: 50%;
  cursor: ns-resize;
  transform: translateX(-50%);
}

.nested-resize-anchor.anchor-bottom-left {
  bottom: -7px;
  left: -7px;
  cursor: nesw-resize;
}

.nested-resize-anchor.anchor-left {
  top: 50%;
  left: -7px;
  cursor: ew-resize;
  transform: translateY(-50%);
}

.layout-grid-cell-child:hover {
  border-color: #93c5fd;
}

.layout-grid-preview.is-nested-moving .layout-grid-cell-child:hover {
  border-color: transparent;
}

.layout-grid-cell-child.selected {
  border-color: #2563eb;
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.12);
}

.layout-grid-preview.is-nested-moving .layout-grid-cell-child.selected {
  border-color: transparent;
  box-shadow: none;
}

.layout-grid-cell-empty {
  display: grid;
  position: absolute;
  inset: 8px;
  z-index: 1;
  place-items: center;
  border: 1px dashed #bfdbfe;
  border-radius: 6px;
  background: rgba(239, 246, 255, 0.7);
  color: #94a3b8;
  font-size: 12px;
}

.layout-grid-cell-drop-preview {
  position: absolute;
  inset: 6px;
  z-index: 1;
  display: grid;
  place-items: center;
  border: 2px solid #2563eb;
  border-radius: 8px;
  background: rgba(219, 234, 254, 0.82);
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 700;
  pointer-events: none;
  box-shadow:
    inset 0 0 0 1px rgba(255, 255, 255, 0.9),
    0 10px 22px rgba(37, 99, 235, 0.2);
}

.layout-grid-preview.is-nested-moving .layout-grid-cell-drop-preview {
  inset: 4px;
  border-width: 2px;
  background: linear-gradient(135deg, rgba(219, 234, 254, 0.95), rgba(239, 246, 255, 0.88));
  box-shadow:
    inset 0 0 0 1px rgba(255, 255, 255, 0.92),
    0 0 0 3px rgba(37, 99, 235, 0.12),
    0 14px 28px rgba(37, 99, 235, 0.22);
}

.system-component-preview {
  display: grid;
  gap: 8px;
  min-height: 0;
  overflow: hidden;
}

.ai-crud-preview {
  display: flex;
  flex-direction: column;
  flex: 1;
  width: 100%;
  height: 100%;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

.ai-crud-preview :deep(.ai-crud-page) {
  display: flex;
  flex: 1 1 0;
  width: 100%;
  height: auto;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

.ai-crud-preview :deep(.ai-crud-main.has-inline-workspace:not(.is-tab-workspace) .ai-crud-table),
.ai-crud-preview :deep(.ai-crud-detail-tabs),
.ai-crud-preview :deep(.ai-crud-inline-workspace) {
  min-height: 0;
}
.ai-crud-preview-static-tip {
  flex: 0 0 auto;
  padding: 0 10px 8px;
  color: #86909c;
  font-size: 12px;
  line-height: 18px;
}

.system-component-preview :deep(.ai-crud-search),
.system-component-preview :deep(.ai-crud-main),
.system-component-preview :deep(.ai-table-wrapper) {
  border-radius: 6px;
}

.tree-preview {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 8px;
  height: 100%;
  min-height: 0;
  padding: 10px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
}

.tree-preview.is-runtime {
  background: #fff;
}

.tree-preview.is-panel-collapsed {
  align-items: center;
  justify-content: center;
  gap: 0;
  padding: 10px 0;
  border-color: #bfdbfe;
  background: linear-gradient(180deg, #eff6ff 0%, #ffffff 100%), #fff;
  box-shadow: inset -1px 0 0 rgba(37, 99, 235, 0.08);
}

.tree-panel-edge-toggle {
  position: absolute;
  top: 10px;
  right: -11px;
  z-index: 8;
  display: grid;
  place-items: center;
  width: 22px;
  height: 44px;
  padding: 0;
  border: 1px solid #cbd5e1;
  border-radius: 999px;
  background: #fff;
  color: #2563eb;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.12);
  cursor: pointer;
  transition:
    border-color 160ms ease,
    background 160ms ease,
    color 160ms ease,
    transform 160ms ease;
}

.tree-panel-edge-toggle:hover {
  border-color: #2563eb;
  background: #2563eb;
  color: #fff;
  transform: translateX(-1px);
}

.tree-panel-edge-toggle span {
  display: block;
  margin-top: -1px;
  font-size: 22px;
  font-weight: 800;
  line-height: 1;
}

.tree-preview.is-panel-collapsed .tree-panel-edge-toggle {
  top: 50%;
  right: -11px;
  transform: translateY(-50%);
  border-color: #bfdbfe;
  background: #fff;
  color: #1d4ed8;
  box-shadow: 0 4px 10px rgba(37, 99, 235, 0.12);
}

.tree-preview.is-panel-collapsed .tree-panel-edge-toggle:hover {
  border-color: #2563eb;
  background: #2563eb;
  color: #fff;
  transform: translateY(-50%) translateX(-1px);
}

.tree-panel-rail {
  display: grid;
  align-content: center;
  justify-items: center;
  width: 100%;
  min-height: 72px;
  color: #1d4ed8;
}

.tree-panel-rail span {
  display: grid;
  place-items: center;
  width: 22px;
  min-height: 54px;
  padding: 8px 0;
  border-radius: 999px;
  background: #dbeafe;
  color: #1e40af;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0;
  writing-mode: vertical-rl;
  text-orientation: upright;
}

.tree-preview-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
  min-height: 46px;
  padding-right: 34px;
  padding-bottom: 8px;
  border-bottom: 1px solid #eef2f7;
}

.tree-preview-head strong {
  display: block;
  color: #0f172a;
  font-size: 13px;
  font-weight: 800;
  line-height: 18px;
}

.tree-preview-head span {
  display: block;
  margin-top: 1px;
  color: #64748b;
  font-size: 11px;
  line-height: 15px;
}

.tree-preview-head small {
  flex: 0 0 auto;
  padding: 2px 6px;
  border: 1px solid #bfdbfe;
  border-radius: 999px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 10px;
  font-weight: 700;
}

.tree-node-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-height: 28px;
  padding: 2px 0 0;
}

.tree-node-toolbar > span {
  color: #64748b;
  font-size: 11px;
  font-weight: 700;
}

.tree-expand-actions {
  display: inline-flex;
  flex: 0 0 auto;
  overflow: hidden;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: #f8fafc;
}

.tree-expand-actions button {
  height: 24px;
  padding: 0 8px;
  border: 0;
  border-right: 1px solid #e2e8f0;
  background: transparent;
  color: #2563eb;
  font-size: 11px;
  font-weight: 600;
  line-height: 24px;
  cursor: pointer;
}

.tree-expand-actions button:last-child {
  border-right: 0;
}

.tree-expand-actions button:hover {
  background: #eff6ff;
}

.tree-search-placeholder {
  height: 28px;
  padding: 0 10px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: #fff;
  color: #94a3b8;
  font-size: 11px;
  line-height: 26px;
}

.tree-node {
  display: grid;
  grid-template-columns: 8px minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
  min-height: 30px;
  padding: 0 8px;
  border: 1px solid transparent;
  border-radius: 6px;
  font-size: 12px;
  color: #475569;
}

.tree-node-button {
  width: 100%;
  border: 1px solid transparent;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.tree-node-button:hover {
  border-color: #dbeafe;
  background: #f8fbff;
}

.tree-node.child {
  margin-left: 14px;
}

.tree-node span:nth-child(2) {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tree-node small {
  color: #94a3b8;
  font-size: 10px;
}

.tree-node-dot {
  width: 6px;
  height: 6px;
  border-radius: 999px;
  background: #cbd5e1;
}

.tree-node.active {
  border-color: #bfdbfe;
  background: #eff6ff;
  color: #2563eb;
  font-weight: 600;
}

.tree-node.active .tree-node-dot {
  background: #2563eb;
}

.tree-foot {
  margin-top: auto;
  padding-top: 8px;
  border-top: 1px dashed #e2e8f0;
  font-size: 11px;
  color: #94a3b8;
  line-height: 15px;
}

.tree-preview :deep(.n-spin-container) {
  min-height: 0;
}

.tree-preview :deep(.n-tree) {
  min-height: 0;
  overflow: auto;
  font-size: 12px;
}

.tree-preview :deep(.n-tree-node),
.tree-preview :deep(.n-tree-node-content) {
  width: 100%;
}

.tree-preview :deep(.n-tree-node) {
  display: flex;
  align-items: stretch;
}

.tree-preview :deep(.n-tree-node-content) {
  flex: 1 1 auto;
  justify-content: flex-start;
  min-height: 30px;
}

.tree-preview :deep(.n-tree-node-content__text) {
  flex: 1 1 auto;
  min-width: 0;
  text-align: left;
}

.tree-empty {
  padding: 18px 8px;
  color: #94a3b8;
  font-size: 12px;
  text-align: center;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 8px;
  flex: 1;
}

.stats-card {
  padding: 10px 12px;
  border-radius: 6px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.stats-label {
  font-size: 11px;
  color: #64748b;
}

.stats-value {
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
  margin-top: 2px;
}

.stats-trend {
  font-size: 11px;
  margin-top: 2px;
}

.stats-trend.up {
  color: #16a34a;
}

.stats-trend.down {
  color: #dc2626;
}

.info-panel-preview {
  display: grid;
  gap: 5px;
  height: 100%;
  align-content: center;
  padding: 12px 14px;
  border-left: 3px solid #2563eb;
  border-radius: 8px;
  background: #eff6ff;
}

.info-panel-preview.type-success {
  border-left-color: #16a34a;
  background: #f0fdf4;
}

.info-panel-preview.type-warning {
  border-left-color: #d97706;
  background: #fffbeb;
}

.info-panel-preview.type-error {
  border-left-color: #dc2626;
  background: #fef2f2;
}

.info-panel-preview strong {
  color: #0f172a;
  font-size: 13px;
}

.info-panel-preview span {
  color: #475569;
  font-size: 12px;
  line-height: 1.6;
}

.custom-html {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 8px;
  background: #f8fafc;
  border-radius: 6px;
}

.custom-title {
  font-weight: 600;
  font-size: 13px;
  color: #0f172a;
}

.custom-body {
  font-size: 12px;
  color: #475569;
  line-height: 1.6;
  white-space: pre-wrap;
}

.action-button-preview {
  display: flex;
  align-items: center;
  height: 100%;
}

.action-button-preview.is-block {
  width: 100%;
}

.action-button-preview.is-block :deep(.n-button) {
  width: 100%;
}

.button-group-preview,
.tag-list-preview {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.steps-preview {
  padding: 8px 4px;
}

.timeline-preview {
  display: grid;
  gap: 10px;
  min-height: 0;
  overflow: hidden;
}

.timeline-item {
  position: relative;
  display: grid;
  grid-template-columns: 12px minmax(0, 1fr);
  gap: 8px;
  color: #475569;
  font-size: 12px;
}

.timeline-dot {
  width: 8px;
  height: 8px;
  margin-top: 5px;
  border-radius: 999px;
  background: #2563eb;
  box-shadow: 0 0 0 4px #dbeafe;
}

.timeline-item strong {
  display: block;
  color: #0f172a;
  font-size: 12px;
}

.timeline-item small {
  color: #94a3b8;
}

.timeline-item p {
  margin: 2px 0 0;
  line-height: 1.5;
}

.empty-state-preview {
  display: grid;
  place-items: center;
  gap: 6px;
  height: 100%;
  color: #64748b;
  text-align: center;
}

.empty-state-icon {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  border-radius: 999px;
  background: #f1f5f9;
  color: #94a3b8;
  font-size: 20px;
  font-weight: 800;
}

.empty-state-preview strong {
  color: #0f172a;
  font-size: 13px;
}

.empty-state-preview span {
  font-size: 12px;
}

.sub-tabs {
  flex: 1;
}

.sub-tab-empty {
  padding: 16px;
  text-align: center;
  color: #94a3b8;
  font-size: 12px;
}

.section-divider {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 4px 0;
}

.section-divider .bar {
  flex: 1;
  height: 1px;
  background: #e5e7eb;
}

.divider-title {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}

.layout-divider {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  width: 100%;
  height: 100%;
  min-height: 20px;
}

.layout-divider.vertical {
  flex-direction: column;
}

.layout-divider .line {
  flex: 1;
  width: 100%;
  min-height: 1px;
  background: #cbd5e1;
}

.layout-divider.vertical .line {
  width: 1px;
  min-width: 1px;
  min-height: 0;
}

.layout-card {
  display: grid;
  align-content: start;
  gap: 8px;
  height: 100%;
}

.layout-card-title {
  color: #0f172a;
  font-size: 14px;
  font-weight: 700;
}

.layout-card-body {
  min-height: 0;
  color: #475569;
  font-size: 12px;
  line-height: 1.7;
  white-space: pre-wrap;
}

.layout-card-text {
  margin-bottom: 8px;
}

.container-child-list {
  display: grid;
  gap: 8px;
  min-height: 0;
}

.container-child-list > :deep(.grid-block) {
  min-height: 72px;
}

.signature-block-preview,
.transfer-preview,
.step-form-preview,
.descriptions-preview,
.media-preview,
.iframe-preview,
.qrcode-preview,
.barcode-preview,
.box-layout-preview {
  min-height: 0;
  height: 100%;
}

.signature-block-preview,
.transfer-preview,
.step-form-preview,
.descriptions-preview {
  display: grid;
  gap: 8px;
}

.transfer-preview :deep(.n-transfer) {
  min-height: 0;
}

.step-form-fields {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 8px;
}

.step-form-field {
  display: grid;
  gap: 4px;
  padding: 8px 10px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #f8fafc;
}

.step-form-field span {
  color: #475569;
  font-size: 12px;
}

.step-form-field em {
  color: #94a3b8;
  font-size: 11px;
  font-style: normal;
}

.single-stat-preview,
.text-tip-preview,
.watermark-preview,
.audio-preview,
.video-preview,
.avatar-preview,
.barcode-preview,
.qrcode-preview {
  display: grid;
  align-content: center;
  gap: 8px;
  padding: 12px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #fff;
}

.text-title-preview {
  display: grid;
  gap: 4px;
  align-content: center;
  height: 100%;
}

.text-title-preview small {
  color: #64748b;
  font-size: 13px;
  font-weight: 500;
}

.paragraph-preview {
  display: -webkit-box;
  height: 100%;
  margin: 0;
  overflow: hidden;
  white-space: pre-wrap;
  -webkit-box-orient: vertical;
}

.paragraph-list-preview {
  display: block;
  height: auto;
  min-height: 100%;
  margin: 0;
  overflow: auto;
  white-space: pre-wrap;
}

.paragraph-list-preview li + li {
  margin-top: 4px;
}

.paragraph-quote-preview {
  display: block;
  height: auto;
  min-height: 100%;
  margin: 0;
  border-left: 3px solid #94a3b8;
  padding-left: 12px;
  color: #475569;
  white-space: pre-wrap;
}

.single-stat-preview {
  border-color: color-mix(in srgb, var(--stat-color) 28%, #e2e8f0);
  background: linear-gradient(135deg, color-mix(in srgb, var(--stat-color) 10%, #fff), #fff);
}

.single-stat-preview span,
.single-stat-preview small {
  color: #64748b;
  font-size: 12px;
}

.single-stat-preview strong {
  color: var(--stat-color);
  font-size: 24px;
  font-weight: 900;
}

.link-preview {
  display: inline-flex;
  align-items: center;
  color: #2563eb;
  font-size: 13px;
  font-weight: 700;
  text-decoration: none;
}

.link-preview:hover {
  text-decoration: underline;
}

.text-tip-preview {
  grid-template-columns: auto minmax(0, 1fr);
  align-content: start;
}

.text-tip-preview p {
  margin: 3px 0 0;
  color: #475569;
  font-size: 12px;
  line-height: 1.6;
}

.text-tip-preview.type-success .tip-icon {
  background: #dcfce7;
  color: #16a34a;
}

.text-tip-preview.type-warning .tip-icon {
  background: #fef3c7;
  color: #d97706;
}

.text-tip-preview.type-error .tip-icon {
  background: #fee2e2;
  color: #dc2626;
}

.watermark-preview {
  position: relative;
  overflow: hidden;
}

.watermark-preview-inner {
  display: grid;
  place-items: center;
  align-content: center;
  gap: 6px;
  min-height: 100%;
}

.media-preview audio,
.media-preview video,
.iframe-preview iframe {
  width: 100%;
}

.media-preview video,
.iframe-preview iframe {
  height: 100%;
  min-height: 180px;
  border: 0;
  border-radius: 8px;
  background: #0f172a;
}

.avatar-preview {
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
}

.avatar-preview strong,
.avatar-preview span {
  display: block;
}

.avatar-preview span {
  color: #64748b;
  font-size: 12px;
}

.barcode-preview small,
.qrcode-preview small {
  overflow: hidden;
  color: #64748b;
  font-size: 11px;
  text-align: center;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.barcode-preview :deep(svg),
.qrcode-preview :deep(svg),
.qrcode-preview :deep(canvas),
.qrcode-preview :deep(img) {
  max-width: 100%;
  height: auto;
  margin: 0 auto;
  display: block;
}

.code-invalid {
  color: #dc2626;
  font-size: 12px;
}

.box-layout-preview {
  min-height: 100%;
}

.box-layout-preview > :deep(.grid-block) {
  min-width: 120px;
  min-height: 72px;
}

.space-preview {
  display: grid;
  place-items: center;
  min-height: var(--space-size);
}

.space-preview.horizontal {
  min-width: var(--space-size);
  min-height: 100%;
}

.space-preview span {
  width: 100%;
  height: 1px;
  border-top: 1px dashed #cbd5e1;
}

.space-preview.horizontal span {
  width: 1px;
  height: 100%;
  border-top: 0;
  border-left: 1px dashed #cbd5e1;
}

.description-grid {
  display: grid;
  gap: 8px 16px;
}

.description-item {
  display: grid;
  gap: 3px;
  min-width: 0;
  padding-bottom: 8px;
  border-bottom: 1px solid #eef2f7;
}

.description-grid.bordered .description-item {
  padding: 10px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #f8fafc;
}

.description-item span {
  color: #64748b;
  font-size: 12px;
}

.description-item strong {
  color: #0f172a;
  font-size: 13px;
}

.container-empty {
  display: grid;
  place-items: center;
  min-height: 92px;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
  background: #f8fafc;
  color: #94a3b8;
}

.layout-tabs {
  min-height: 0;
}

.layout-spacer {
  width: 100%;
  height: 100%;
}
</style>
