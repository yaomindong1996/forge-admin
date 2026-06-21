<template>
  <div class="grid-block" :class="[`block-${block.blockType}`, { selected }]" :style="blockStyle" :data-block-id="block.id">
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
      <div class="page-title-preview" :class="`size-${block.props?.size || 'medium'}`">
        <div>
          <div class="page-title-main">
            {{ block.props?.title || block.label || '页面标题' }}
          </div>
          <div v-if="block.props?.subtitle" class="page-title-sub">
            {{ block.props.subtitle }}
          </div>
        </div>
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
              {{ detailValue(field, 0) }}
            </div>
          </div>
        </div>
        <div v-else class="block-empty">
          点击右侧"配置字段"按钮添加详情字段
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
          :lazy="block.props?.previewLiveData !== true"
          :api="block.props?.api || ''"
          :api-config="resolvedApiConfig"
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
          :form-open-mode="block.props?.formOpenMode || block.props?.modalType || 'modal'"
          :tab-workspace="block.props?.tabWorkspace || {}"
          :modal-type="block.props?.modalType || 'modal'"
          :drawer-placement="block.props?.drawerPlacement || 'right'"
          :hide-modal-footer="block.props?.hideModalFooter === true"
          :hide-default-detail-content="block.props?.hideDefaultDetailContent === true"
          :hide-toolbar="block.props?.hideToolbar === true"
          :hide-add="block.props?.hideAdd === true"
          :hide-batch-delete="block.props?.hideBatchDelete === true"
          :show-import="block.props?.showImport === true"
          :show-export="block.props?.showExport === true"
          :show-export-tasks="block.props?.showExportTasks !== false"
          :enable-custom-query="block.props?.enableCustomQuery === true"
          :add-button-text="block.props?.addButtonText || '新增'"
          :export-button-text="block.props?.exportButtonText || '导出'"
          :export-file-name="block.props?.exportFileName || ''"
          :render-mode="block.props?.renderMode || 'table'"
          :show-render-mode-switch="block.props?.showRenderModeSwitch !== false"
          :enable-tree-add-child="block.props?.enableTreeAddChild === true"
          :table-size="block.props?.tableSize || 'small'"
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
          v-bind="designerCrudHookHandlers"
          @load-list-success="handleCrudPreviewSuccess"
          @load-list-error="handleCrudPreviewError"
        />
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
          v-for="(metric, idx) in (block.props?.metrics || [])"
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
        <div v-if="!(block.props?.metrics?.length)" class="block-empty">
          点击右侧添加指标项
        </div>
      </div>
    </template>

    <!-- 提示面板 -->
    <template v-else-if="block.blockType === 'info-panel'">
      <div class="info-panel-preview" :class="`type-${block.props?.type || 'info'}`">
        <strong>{{ block.props?.title || '提示信息' }}</strong>
        <span>{{ block.props?.content || '在右侧填写提示内容' }}</span>
      </div>
    </template>

    <!-- 说明文本 -->
    <template v-else-if="block.blockType === 'custom-html'">
      <div class="custom-html">
        <div v-if="block.props?.title" class="custom-title">
          {{ block.props.title }}
        </div>
        <div class="custom-body">
          {{ block.props?.content || '在右侧填写说明内容' }}
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
          v-for="tag in (block.props?.tags || [])"
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
          v-for="step in (block.props?.steps || [])"
          :key="step.title"
          :title="step.title"
          :description="step.description"
        />
      </n-steps>
    </template>

    <!-- 时间线 -->
    <template v-else-if="block.blockType === 'timeline'">
      <div class="timeline-preview">
        <div v-if="block.props?.title" class="custom-title">
          {{ block.props.title }}
        </div>
        <div
          v-for="item in (block.props?.items || [])"
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
        <strong>{{ block.props?.title || '暂无数据' }}</strong>
        <span>{{ block.props?.description || '当前没有可展示的数据' }}</span>
        <n-button v-if="block.props?.actionText" size="small" secondary disabled>
          {{ block.props.actionText }}
        </n-button>
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
  </div>
</template>

<script setup>
import { ChevronBackOutline } from '@vicons/ionicons5'
import { computed, h, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AiCrudPage from '@/components/ai-form/AiCrudPage.vue'
import AiForm from '@/components/ai-form/AiForm.vue'
import AiTable from '@/components/ai-form/AiTable.vue'
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
  'childBlockSelect',
  'childBlockMenuSelect',
  'childBlockDragStart',
  'childBlockMoveStart',
  'childBlockDragEnd',
  'childBlockResizeStart',
])

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const fieldMap = computed(() => new Map(props.fields.map(f => [f.field, f])))
const resolvedFields = computed(() => (props.block.fieldRefs || [])
  .map(ref => fieldMap.value.get(ref))
  .filter(Boolean))
const previewFormValue = ref({})
const runtimeCrudRef = ref(null)
const runtimeTreeLoading = ref(false)
const runtimeTreeNodes = ref([])
const runtimeTreeNodeMap = ref(new Map())
const runtimeExpandedTreeKeys = ref([])
const previewTreeExpanded = ref(true)
const treePanelCollapsed = ref(false)
const runtimeTreeChildrenField = computed(() => props.block.props?.childrenField || 'children')
const runtimeSelectedTreeKeys = computed(() => props.runtimeTreeActiveKey === '__all__' ? [] : [props.runtimeTreeActiveKey])
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
    return {
      key: cell.key || `cell_${index + 1}`,
      title: cell.title ?? `栅格 ${index + 1}`,
      span: clampGridSpan(cell.span, 6),
      children: Array.isArray(cell.children) ? cell.children : [],
    }
  })
})
const gridLayoutStyle = computed(() => ({
  gridTemplateColumns: `repeat(${Math.max(1, Number(props.block.props?.columns || 24))}, minmax(0, 1fr))`,
  gap: `${Math.max(0, Number(props.block.props?.rowGap ?? 0))}px ${Math.max(0, Number(props.block.props?.gutter ?? props.block.props?.gap ?? 16))}px`,
  alignItems: props.block.props?.alignItems || 'stretch',
  justifyItems: props.block.props?.justifyItems || 'stretch',
}))
function gridCellStyle(cell = {}) {
  return {
    gridColumn: `span ${clampGridSpan(cell.span, 6)}`,
    minHeight: `${Math.max(24, Number(props.block.props?.cellMinHeight || 120))}px`,
    backgroundColor: props.block.props?.cellBackground || 'transparent',
  }
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
  ...resolvedFields.value.slice(0, 8).map(field => ({
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
  const rowActions = (props.block.props?.customActions || []).filter(action => (action.position || 'row') === 'row')
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
const aiTableColumns = computed(() => resolvedFields.value.slice(0, 8).map(field => ({
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
  return {
    ...props.runtimeCrudProps,
    ...hookHandlers,
    api: blockProps.api || props.runtimeCrudProps.api || '',
    rowKey: blockProps.rowKey || props.runtimeCrudProps.rowKey || 'id',
    title: blockProps.title || props.runtimeCrudProps.title,
    columns: props.runtimeCrudProps.columns?.length ? props.runtimeCrudProps.columns : aiTableColumns.value,
    searchSchema: props.runtimeCrudProps.searchSchema?.length ? props.runtimeCrudProps.searchSchema : aiSearchSchema.value,
    editSchema: props.runtimeCrudProps.editSchema?.length ? props.runtimeCrudProps.editSchema : aiFormSchema.value,
    apiConfig: {
      ...(props.runtimeCrudProps.apiConfig || {}),
      ...Object.fromEntries(Object.entries(blockApiConfig.value).filter(([, value]) => value)),
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
    formOpenMode: blockProps.formOpenMode || props.runtimeCrudProps.formOpenMode || blockProps.modalType || props.runtimeCrudProps.modalType,
    tabWorkspace: blockProps.tabWorkspace || props.runtimeCrudProps.tabWorkspace,
    modalType: blockProps.modalType || props.runtimeCrudProps.modalType,
    drawerPlacement: blockProps.drawerPlacement || props.runtimeCrudProps.drawerPlacement,
    hideModalFooter: blockProps.hideModalFooter ?? props.runtimeCrudProps.hideModalFooter,
    hideDefaultDetailContent: blockProps.hideDefaultDetailContent ?? props.runtimeCrudProps.hideDefaultDetailContent,
    hideToolbar: blockProps.hideToolbar ?? props.runtimeCrudProps.hideToolbar,
    hideAdd: blockProps.hideAdd ?? props.runtimeCrudProps.hideAdd,
    hideBatchDelete: blockProps.hideBatchDelete ?? props.runtimeCrudProps.hideBatchDelete,
    showImport: blockProps.showImport ?? props.runtimeCrudProps.showImport,
    showExport: blockProps.showExport ?? props.runtimeCrudProps.showExport,
    showExportTasks: blockProps.showExportTasks ?? props.runtimeCrudProps.showExportTasks,
    enableCustomQuery: blockProps.enableCustomQuery ?? props.runtimeCrudProps.enableCustomQuery,
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
const previewPagination = {
  page: 1,
  pageSize: 10,
  itemCount: 3,
  showSizePicker: true,
  showQuickJumper: true,
}
const toolbarCustomActions = computed(() => (props.block.props?.customActions || [])
  .filter(action => (action.position || 'toolbar') === 'toolbar'))

const sampleRows = computed(() => Array.from({ length: 3 }).map((_, idx) => {
  const row = { id: idx + 1, __actions: '编辑' }
  resolvedFields.value.slice(0, 8).forEach((field) => {
    row[field.field] = sampleValue(field, idx)
  })
  return row
}))

function componentType(field) {
  return field?.componentType || field?.dataType || 'input'
}

function hasAction(key) {
  return Array.isArray(props.block.props?.actions) && props.block.props.actions.includes(key)
}

function fieldAlign(fieldName) {
  const align = props.block.props?.fieldSettings?.[fieldName]?.align
  return ['left', 'center', 'right'].includes(align) ? align : 'left'
}

function fieldSetting(fieldName) {
  return props.block.props?.fieldSettings?.[fieldName] || {}
}

function renderTableCell(field, row = {}) {
  const setting = fieldSetting(field.field)
  const text = row[field.field] ?? ''
  const style = {}
  if (setting.textColor)
    style.color = setting.textColor
  const renderType = setting.renderType || (field.dictType ? 'dictTag' : '')
  if (renderType === 'dictTag') {
    return h('span', {
      class: 'designer-dict-tag',
      style,
      title: field.dictType ? `字典：${field.dictType}` : '枚举标签',
    }, resolveDictLabel(field, text))
  }
  const isLink = setting.renderType === 'link' || setting.clickAction === 'navigate'
  if (isLink) {
    const targetTip = setting.targetPageKey
      ? `跳转到：${setting.targetPageKey}${setting.targetFormKey ? ` / ${setting.targetFormKey}` : ''}，参数 ${setting.targetParamName || 'id'} = ${setting.targetParamField || 'id'}`
      : '跳转页面'
    return h('a', {
      href: '#',
      class: 'designer-table-link',
      style,
      title: targetTip,
      onClick: event => event.preventDefault(),
    }, String(text || '-'))
  }
  return h('span', { style }, String(text || '-'))
}

function resolveDictLabel(field, value) {
  const options = Array.isArray(field.options) ? field.options : []
  const match = options.find(option => String(option.value) === String(value))
  if (match)
    return match.label || match.value || '-'
  if (value && value !== '字典')
    return String(value)
  return field.dictType || '字典标签'
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
  const record = props.runtimeRecord || {}
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
    patch: {
      lastPreviewAt: new Date().toISOString(),
      ...patch,
    },
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
  const text = String(expression || '').trim()
  if (!text)
    return true
  const lowerText = text.toLowerCase()
  const inIndex = lowerText.indexOf(' in ')
  if (inIndex > 0) {
    const actual = resolveConditionValue(row, text.slice(0, inIndex).trim())
    return text.slice(inIndex + 4).split(',').map(item => item.trim()).filter(Boolean).includes(String(actual ?? ''))
  }
  const operator = text.includes('!=') ? '!=' : text.includes('==') ? '==' : text.includes('=') ? '=' : ''
  if (operator) {
    const [fieldName, ...expectedParts] = text.split(operator)
    const actual = String(resolveConditionValue(row, fieldName.trim()) ?? '')
    const expected = String(expectedParts.join(operator) || '').trim().replace(/^['"]|['"]$/g, '')
    return operator === '!=' ? actual !== expected : actual === expected
  }
  return true
}

function resolveConditionValue(row = {}, path = '') {
  return String(path || '').split('.').filter(Boolean).reduce((value, key) => value?.[key], row)
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

onMounted(loadRuntimeTree)

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
  min-width: 0;
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

.page-title-main {
  color: #0f172a;
  font-size: 20px;
  font-weight: 800;
  line-height: 1.2;
}

.page-title-preview.size-small .page-title-main {
  font-size: 16px;
}

.page-title-preview.size-large .page-title-main {
  font-size: 26px;
}

.page-title-sub {
  margin-top: 4px;
  color: #64748b;
  font-size: 13px;
  line-height: 1.5;
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

.block-table :deep(.n-data-table-tr) {
  height: var(--block-table-row-height, 40px);
}

.layout-grid-preview {
  display: grid;
  width: 100%;
  height: 100%;
  min-width: 0;
  min-height: 0;
}

.layout-grid-cell {
  position: relative;
  min-width: 0;
  min-height: 0;
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
  position: absolute;
  inset: 8px;
  z-index: 2;
  min-width: 0;
  min-height: 0;
}

.layout-grid-cell-child {
  position: absolute;
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
  width: 100%;
  min-width: 0;
  overflow: visible;
}

.ai-crud-preview :deep(.ai-crud-page) {
  width: 100%;
  min-width: 0;
  min-height: 0;
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
