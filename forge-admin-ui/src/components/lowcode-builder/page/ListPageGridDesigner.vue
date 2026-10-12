<template>
  <div
    class="list-grid-designer"
    :class="{ 'left-collapsed': paletteCollapsed && !canvasFocusMode, 'right-collapsed': propertyCollapsed && !canvasFocusMode, 'canvas-focused': canvasFocusMode, 'panel-only': panelOnly, readonly }"
  >
    <button
      v-if="!panelOnly && paletteCollapsed && !readonly && !canvasFocusMode"
      type="button"
      class="side-rail-toggle-button left"
      title="展开页面组件"
      @click="paletteCollapsed = false"
    >
      <n-icon><ChevronForwardOutline /></n-icon>
    </button>

    <aside v-else-if="!panelOnly && !readonly && !canvasFocusMode" class="palette-panel">
      <div class="palette-panel-head">
        <div>
          <div class="panel-title">
            页面组件
          </div>
          <div class="panel-desc">
            拖拽组件到画布，右侧可调整样式
          </div>
          <div class="palette-stats">
            <span>共 {{ unifiedPaletteTotal }} 个</span>
          </div>
        </div>
        <n-button
          class="panel-collapse-button"
          circle
          size="small"
          secondary
          title="收起页面组件"
          @click="paletteCollapsed = true"
        >
          <template #icon>
            <n-icon><ChevronBackOutline /></n-icon>
          </template>
        </n-button>
      </div>
      <n-input
        v-model:value="paletteKeyword"
        class="palette-search"
        clearable
        size="small"
        placeholder="搜索区块名称或组件名"
      >
        <template #prefix>
          <n-icon><SearchOutline /></n-icon>
        </template>
      </n-input>
      <div class="palette-groups">
        <!-- 统一组件物料面板（designer-core）：与表单设计器同一注册表、同一合集、同一分组、同一交互 -->
        <!-- B2 修正：画布不支持的组件直接隐藏（与表单侧策略统一，消除"由表单区块承载"等开发术语文案） -->
        <UnifiedComponentPalette
          scope="ALL"
          :keyword="paletteKeyword"
          :item-filter="listPaletteItemFilter"
          :item-disabled-reason="unifiedPaletteDisabledReason"
          @item-drag-start="handleUnifiedPaletteDragStart"
          @item-click="handleUnifiedPaletteClick"
          @total-change="unifiedPaletteTotal = $event"
        />
        <div v-if="!unifiedPaletteTotal" class="palette-empty">
          没有匹配的区块
        </div>
      </div>
    </aside>

    <main
      v-if="!panelOnly"
      class="canvas-panel"
      :class="{ 'drag-over': canvasDragActive }"
      @dragenter.prevent="handleCanvasDragEnter"
      @dragover.prevent="handleCanvasDragOver"
      @dragleave="handleCanvasDragLeave"
      @drop="handleCanvasDrop"
    >
      <div v-if="!readonly" class="canvas-toolbar">
        <div class="toolbar-info">
          <strong>{{ pageName || '列表页面' }} · {{ layoutTitle }}</strong>
          <span>{{ blocks.length }} 个区块 · {{ canvasGridWidth }}px · {{ canvasViewportSummary }}</span>
        </div>
        <n-space v-if="!readonly" class="canvas-primary-actions" size="small" align="center">
          <n-popconfirm
            :show-icon="false"
            positive-text="清空"
            negative-text="取消"
            @positive-click="clearCanvas"
          >
            <template #trigger>
              <n-button size="small" secondary type="error" :disabled="!blocks.length">
                清空
              </n-button>
            </template>
            清空当前列表画布上的所有组件？
          </n-popconfirm>
          <n-button size="small" @click="resetLayout">
            重置默认
          </n-button>
        </n-space>
      </div>

      <div ref="canvasScrollRef" class="canvas-scroll" @wheel="handleCanvasWheel">
        <div class="canvas-zoom-stage" :style="canvasZoomStageStyle">
          <div
            ref="canvasRef"
            class="canvas-grid"
            :style="[canvasStyle, canvasScaleStyle]"
            @click.self="clearSelection"
          >
            <!-- Grid background -->
            <template v-if="!readonly">
              <div
                v-for="row in totalRows"
                :key="`row-${row}`"
                class="grid-row"
                :style="{ top: `${(row - 1) * (rowHeight + gap)}px`, height: `${rowHeight}px` }"
              />
              <div
                v-for="col in 12"
                :key="`col-${col}`"
                class="grid-col"
                :style="{ left: `${(col - 1) * (colWidth + gap)}px`, width: `${colWidth}px` }"
              />
            </template>
            <div
              v-if="dropPreviewStyle"
              class="drop-preview"
              :style="dropPreviewStyle"
            >
              {{ dropPreviewLabel }}
            </div>
            <div
              v-if="blockedDropPreviewStyle"
              class="drop-preview drop-preview-blocked"
              :style="blockedDropPreviewStyle"
            >
              该组件不支持嵌套
            </div>
            <div
              v-if="movePlaceholderStyle"
              class="move-placeholder"
              :style="movePlaceholderStyle"
            >
              释放到这里
            </div>

            <!-- Blocks -->
            <div
              v-for="block in blocks"
              :key="block.id"
              :data-block-id="block.id"
              class="grid-item"
              :class="{ selected: block.id === selectedBlockId, moving: block.id === movingBlockId }"
              :style="resolveBlockStyle(block)"
              @click.stop="handleBlockClick(block.id)"
            >
              <DesignerNodeOverlay
                v-if="!readonly"
                mode="block"
                :show-menu="true"
                :show-drag-handle="true"
                drag-title="拖动区块"
                :menu-options="resolveBlockMoreOptions(block)"
                @menu-select="key => handleBlockMoreSelect(key, block)"
                @drag-start="startMove(block, $event)"
              />
              <GridBlockRenderer
                :block="block"
                :fields="fields"
                :selected="block.id === selectedBlockId"
                :selected-block-id="selectedBlockId || ''"
                :readonly="readonly"
                suppress-runtime-list-grid
                :runtime-crud-props="resolvedRuntimeCrudProps"
                :runtime-record="runtimeRecord"
                :runtime-tree-active-key="runtimeTreeActiveKey"
                :active-drop-cell="activeDropCell"
                :nested-moving-block-id="nestedMovingBlockId"
                :catalog-drag-block-type="draggedBlockType"
                @child-block-select="handleBlockClick"
                @child-block-menu-select="handleNestedBlockMenuSelect"
                @block-props-update="handleBlockPropsUpdate"
                @tabs-active-change="handleTabsActiveChange"
                @tab-drop="handleTabDrop"
                @grid-cell-drop="handleGridCellDrop"
                @grid-cell-insert="handleGridCellDrop"
                @grid-cell-drag-over="handleGridCellDragOverHint"
                @container-insert="handleContainerInsert"
                @container-clear="handleContainerClear"
                @child-block-drag-start="handleNestedBlockDragStart"
                @child-block-move-start="payload => startNestedMove(payload.block, payload.event)"
                @child-block-drag-end="resetCanvasDragState"
                @child-block-resize-start="payload => startNestedResize(payload.block, payload.event, payload.anchor)"
                @runtime-tree-select="handleRuntimeTreeSelect"
                @tree-panel-collapse-change="handleTreePanelCollapseChange"
                @crud-preview-state-change="handleCrudPreviewStateChange"
              />
            </div>

            <!-- 顶层区块 resize 锚点渲染在画布层：不受 grid-item 内容裁剪影响，
                 “填充容器”贴画布边缘时锚点仍完整可见（选中态由 v-if 控制） -->
            <template v-if="!readonly && selectedResizeBlock">
              <button
                v-for="anchor in resizeAnchors"
                :key="anchor"
                type="button"
                class="resize-anchor canvas-resize-anchor"
                :class="`anchor-${anchor}`"
                title="调整区块大小"
                :style="resolveCanvasAnchorStyle(anchor)"
                @pointerdown.stop="startResize(selectedResizeBlock, $event, anchor)"
              />
            </template>
          </div>
        </div>
        <div v-if="!readonly" class="canvas-viewport-dock">
          <div class="viewport-device-group" aria-label="预览设备">
            <button
              type="button"
              class="viewport-device-button"
              :class="{ active: canvasPreviewMode === 'desktop' }"
              title="Desktop"
              @click.stop="applyCanvasPreviewMode('desktop')"
            >
              <n-icon><DesktopOutline /></n-icon>
            </button>
            <button
              type="button"
              class="viewport-device-button"
              :class="{ active: canvasPreviewMode === 'narrow' }"
              title="Tablet"
              @click.stop="applyCanvasPreviewMode('narrow')"
            >
              <n-icon><TabletLandscapeOutline /></n-icon>
            </button>
            <button
              type="button"
              class="viewport-device-button"
              :class="{ active: canvasPreviewMode === 'mobile' }"
              title="Mobile"
              @click.stop="applyCanvasPreviewMode('mobile')"
            >
              <n-icon><PhonePortraitOutline /></n-icon>
            </button>
          </div>
          <n-popover trigger="click" placement="bottom" :width="282" :to="false">
            <template #trigger>
              <n-button class="viewport-icon-button" circle secondary title="预览形态和设计宽度">
                <template #icon>
                  <n-icon><BrowsersOutline /></n-icon>
                </template>
              </n-button>
            </template>
            <div class="canvas-viewport-popover">
              <div class="viewport-popover-head">
                <strong>预览视口</strong>
                <span>{{ canvasPreviewModeLabel }} · {{ designCanvasWidth }}px</span>
              </div>
              <label class="canvas-viewport-field">
                <span>预览形态</span>
                <n-select
                  :value="canvasPreviewMode"
                  :options="canvasPreviewModeOptions"
                  size="small"
                  class="canvas-preview-select"
                  @update:value="applyCanvasPreviewMode"
                />
              </label>
              <label class="canvas-viewport-field">
                <span>设计宽度</span>
                <n-select
                  :value="designCanvasWidth"
                  :options="canvasWidthOptions"
                  size="small"
                  class="canvas-width-select"
                  @update:value="updateDesignWidth"
                />
              </label>
              <label class="canvas-viewport-field">
                <span>自定义宽度</span>
                <n-input-number
                  :value="designCanvasWidth"
                  :min="375"
                  :max="2560"
                  :step="10"
                  size="small"
                  class="canvas-width-input"
                  :show-button="false"
                  @update:value="updateDesignWidth"
                />
              </label>
            </div>
          </n-popover>
          <span class="viewport-divider" />
          <n-button class="viewport-icon-button" circle secondary title="缩小" @click.stop="updateCanvasZoom(canvasZoom - 0.1)">
            <template #icon>
              <n-icon><RemoveOutline /></n-icon>
            </template>
          </n-button>
          <n-popover trigger="click" placement="bottom" :width="244" :to="false">
            <template #trigger>
              <n-button class="viewport-zoom-button" secondary title="画布缩放">
                <template #icon>
                  <n-icon><ResizeOutline /></n-icon>
                </template>
                {{ canvasZoomLabel }}
              </n-button>
            </template>
            <div class="canvas-viewport-popover">
              <div class="viewport-popover-head">
                <strong>画布缩放</strong>
                <span>Ctrl/⌘ + 滚轮也可缩放</span>
              </div>
              <div class="canvas-viewport-zoom-actions">
                <n-button size="small" secondary @click="updateCanvasZoom(canvasZoom - 0.1)">
                  -
                </n-button>
                <n-select
                  :value="canvasZoom"
                  :options="canvasZoomOptions"
                  size="small"
                  class="canvas-zoom-select"
                  @update:value="updateCanvasZoom"
                />
                <n-button size="small" secondary @click="updateCanvasZoom(canvasZoom + 0.1)">
                  +
                </n-button>
              </div>
            </div>
          </n-popover>
          <n-button class="viewport-icon-button" circle secondary title="放大" @click.stop="updateCanvasZoom(canvasZoom + 0.1)">
            <template #icon>
              <n-icon><AddOutline /></n-icon>
            </template>
          </n-button>
          <span class="viewport-divider" />
          <n-button class="viewport-icon-button" circle secondary title="查看源码" @click.stop="openSourceModal">
            <template #icon>
              <n-icon><CodeSlashOutline /></n-icon>
            </template>
          </n-button>
          <n-button class="viewport-icon-button" circle secondary :title="canvasFocusMode ? '退出专注' : '专注画布'" @click.stop="toggleCanvasFocus">
            <template #icon>
              <n-icon>
                <ContractOutline v-if="canvasFocusMode" />
                <ExpandOutline v-else />
              </n-icon>
            </template>
          </n-button>
        </div>
      </div>
    </main>

    <button
      v-if="!panelOnly && propertyCollapsed && !readonly && !canvasFocusMode"
      type="button"
      class="side-rail-toggle-button right"
      title="展开配置区块"
      @click="propertyCollapsed = false"
    >
      <n-icon><ChevronBackOutline /></n-icon>
    </button>

    <ListPageBlockPropertyPanel
      v-else-if="panelOnly || (!readonly && !canvasFocusMode)"
    />

    <FieldConfigDrawer
      v-model:show="fieldDrawerOpen"
      :mode="fieldDrawerMode"
      :fields="fields"
      :initial-field="fieldDrawerInitialField"
      :block-meta-title="selectedBlockMeta?.title || ''"
      :page-target-options="pageTargetOptions"
      :form-target-options="formTargetOptions"
      @patch-props="handleFieldDrawerPatchProps"
      @patch-block="handleFieldDrawerPatchBlock"
    />

    <!-- 组件属性抽屉（与表单设计器 ForgePropertyPanel 同款）：spec 驱动，常用属性平铺 + 高级属性折叠 + 搜索 -->
    <n-drawer
      v-model:show="specDrawerVisible"
      :width="440"
      placement="right"
      :trap-focus="false"
      :block-scroll="false"
    >
      <n-drawer-content :title="`${selectedBlock?.label || selectedBlock?.blockType || '组件'} 组件属性`" closable>
        <SpecPropertyPanel
          v-if="selectedBlock"
          :block-type="selectedBlock.blockType"
          :model-props="selectedBlock.props || {}"
          :exclude-keys="specPanelExcludeKeys"
          @update:prop="handleSpecPropUpdate"
        />
      </n-drawer-content>
    </n-drawer>

    <n-modal v-if="customActionsEditable" v-model:show="customActionModalOpen" :mask-closable="false">
      <n-card
        class="custom-action-modal"
        title="配置自定义操作"
        :bordered="false"
        role="dialog"
        aria-modal="true"
      >
        <div class="action-modal-layout">
          <div class="action-modal-list">
            <button
              v-for="(action, idx) in customActionList"
              :key="action.clientKey || action.key || idx"
              type="button"
              class="action-list-item"
              :class="{ active: activeActionIndex === idx }"
              @click="activeActionIndex = idx"
            >
              <span>{{ action.label || '自定义按钮' }}</span>
              <small>{{ action.key || '未设置编码' }}</small>
            </button>
            <button type="button" class="action-add-item" @click="addCustomAction">
              + 添加操作
            </button>
          </div>

          <div v-if="activeAction" class="action-editor-panel">
            <div class="action-editor-head">
              <div>
                <div class="action-editor-title">
                  {{ activeAction.label || '自定义按钮' }}
                </div>
                <div class="action-editor-desc">
                  支持站内跳转、外部链接、调用 API、发起主流程、执行触发器和刷新列表；目标地址和参数值可使用 :id 或 ${field} 占位符。
                </div>
              </div>
              <n-button quaternary type="error" @click="removeCustomAction(activeActionIndex)">
                删除
              </n-button>
            </div>

            <n-form class="action-editor-form" size="small" label-placement="top" :show-feedback="false">
              <section class="action-form-section action-form-section--compact">
                <div class="action-section-head">
                  <strong>基础信息</strong>
                  <span>控制按钮显示、位置和视觉类型。</span>
                </div>
                <div class="action-form-grid">
                  <n-form-item label="按钮名称">
                    <n-input
                      :value="activeAction.label"
                      placeholder="例如：查看详情"
                      @update:value="updateActiveCustomAction({ label: $event })"
                    />
                  </n-form-item>
                  <n-form-item label="唯一编码">
                    <n-input
                      :value="activeAction.key"
                      placeholder="view_detail"
                      @update:value="updateActiveCustomAction({ key: normalizeActionKey($event) })"
                    />
                  </n-form-item>
                  <n-form-item label="显示位置">
                    <n-select
                      :value="activeAction.position || 'toolbar'"
                      :options="actionPositionOptions"
                      @update:value="updateActiveCustomAction({ position: $event })"
                    />
                  </n-form-item>
                  <n-form-item label="按钮样式">
                    <n-select
                      :value="activeAction.type || 'default'"
                      :options="actionTypeOptions"
                      @update:value="updateActiveCustomAction({ type: $event })"
                    />
                  </n-form-item>
                </div>
              </section>

              <section class="action-form-section">
                <div class="action-section-head">
                  <strong>交互配置</strong>
                  <span>选择点击后的动作，并配置目标页面、接口或流程。</span>
                </div>
                <div class="action-form-grid">
                  <n-form-item label="交互方式">
                    <n-select
                      :value="resolveActionBehaviorValue(activeAction.actionType)"
                      :options="actionBehaviorOptions"
                      @update:value="updateActiveCustomActionType"
                    />
                  </n-form-item>
                  <n-form-item label="打开方式">
                    <n-select
                      :value="activeAction.openTarget || '_self'"
                      :disabled="['refresh', 'CALL_API', 'START_FLOW', 'TRIGGER'].includes(resolveActionBehaviorValue(activeAction.actionType))"
                      :options="actionOpenTargetOptions"
                      @update:value="updateActiveCustomAction({ openTarget: $event })"
                    />
                  </n-form-item>
                </div>

                <n-form-item v-if="isStartFlowCustomAction(activeAction)" label="主流程">
                  <div class="main-flow-action-hint">
                    <strong>使用“流程与自动化”中配置的主流程</strong>
                    <span>这里只维护按钮名称、位置、权限、确认提示和成功失败文案。</span>
                  </div>
                </n-form-item>

                <n-form-item v-else-if="isTriggerCustomAction(activeAction)" label="触发器标识">
                  <n-input
                    :value="activeAction.actionConfig?.triggerCode || activeAction.routePath || ''"
                    placeholder="例如：customer_notify"
                    @update:value="updateActiveGenericActionConfig({ triggerCode: $event || '' })"
                  />
                </n-form-item>

                <n-form-item v-else-if="isPageCustomAction(activeAction)" label="应用内页面">
                  <div class="action-route-panel">
                    <div class="action-config-tip">
                      <strong>应用内页面</strong>
                      <span>跳转到当前低代码应用里的其它页面；运行态会用 pageId 切页，并可通过参数映射传入行字段。</span>
                    </div>
                    <div class="action-form-grid">
                      <div class="action-field">
                        <span class="action-field-label">目标页面</span>
                        <n-select
                          :value="activeAction.targetPageKey || ''"
                          :options="pageTargetOptions"
                          clearable
                          filterable
                          placeholder="选择应用内页面"
                          @update:value="updateActiveCustomAction({ targetPageKey: $event || '', actionType: 'page' })"
                        />
                        <small>来自当前应用的页面设计列表。</small>
                      </div>
                      <div v-if="formTargetOptions.length" class="action-field">
                        <span class="action-field-label">目标表单</span>
                        <n-select
                          :value="activeAction.targetFormKey || ''"
                          :options="formTargetOptions"
                          clearable
                          filterable
                          placeholder="目标表单（可选）"
                          @update:value="updateActiveCustomAction({ targetFormKey: $event || '' })"
                        />
                        <small>用于打开目标页时带上 formKey。</small>
                      </div>
                    </div>
                  </div>
                </n-form-item>

                <n-form-item v-else-if="!isApiCustomAction(activeAction)" label="目标地址 / 表单">
                  <div class="action-route-panel">
                    <div v-if="isRouteCustomAction(activeAction)" class="action-config-tip">
                      <strong>站内跳转</strong>
                      <span>优先选择系统菜单中已经配置的页面，会自动填入目标地址；未配置菜单时可以直接手工输入路由。应用内页面请改用「应用内页面」交互方式。</span>
                    </div>
                    <div class="action-form-grid">
                      <div v-if="isRouteCustomAction(activeAction)" class="action-field">
                        <span class="action-field-label">系统菜单页面</span>
                        <n-select
                          :value="resolveSystemMenuPageTargetValue(activeAction)"
                          :options="systemMenuPageTargetOptions"
                          :loading="systemMenuPageLoading"
                          clearable
                          filterable
                          placeholder="选择系统菜单页面"
                          @focus="loadSystemMenuPages"
                          @update:value="applySystemMenuPageTarget"
                        />
                        <small>来源于系统管理 / 菜单管理，只列出菜单类型资源。</small>
                      </div>
                      <div class="action-field">
                        <span class="action-field-label">目标地址</span>
                        <n-input
                          :value="activeAction.routePath"
                          :disabled="(activeAction.actionType || 'route') === 'refresh'"
                          :placeholder="actionPathPlaceholder(activeAction)"
                          @update:value="updateActiveCustomAction({ routePath: $event })"
                        />
                        <small v-if="isRouteCustomAction(activeAction)">例如 /system/user、/app/customer/detail/:id，:id 可由参数映射替换。</small>
                        <small v-else-if="normalizeCustomActionType(activeAction.actionType) === 'external'">填写完整外部地址，例如 https://example.com/detail/:id。</small>
                      </div>
                      <div v-if="formTargetOptions.length" class="action-field">
                        <span class="action-field-label">目标表单</span>
                        <n-select
                          :value="activeAction.targetFormKey || ''"
                          :disabled="(activeAction.actionType || 'route') === 'refresh'"
                          :options="formTargetOptions"
                          clearable
                          filterable
                          placeholder="目标表单"
                          @update:value="updateActiveCustomAction({ targetFormKey: $event || '' })"
                        />
                        <small>用于当前业务对象内的表单页、弹窗页或抽屉页。</small>
                      </div>
                    </div>
                  </div>
                </n-form-item>

                <n-form-item v-else label="API 调用">
                  <div class="api-action-panel">
                    <div class="action-config-tip">
                      <strong>API 调用</strong>
                      <span>接口地址填写后端路径，Path 参数使用 :id 这类占位；GET 默认走 Query，POST/PUT/PATCH 默认走 Body。</span>
                    </div>
                    <div class="action-form-grid">
                      <div class="action-field">
                        <span class="action-field-label">已登记 API</span>
                        <n-select
                          :value="activeAction.actionConfig?.apiConfigId || null"
                          :options="apiConfigOptions"
                          :loading="apiConfigLoading"
                          clearable
                          filterable
                          placeholder="选择 API 配置；关闭时可留空"
                          @focus="loadEnabledApiConfigs"
                          @update:value="applyCustomActionApiConfig"
                        />
                        <small>选择后会带出请求方式和接口地址，也可以不选直接手工填写。</small>
                      </div>
                      <div class="action-field">
                        <span class="action-field-label">请求方式</span>
                        <n-select
                          :value="activeAction.actionConfig?.method || 'POST'"
                          :options="apiMethodOptions"
                          @update:value="updateActiveActionConfig({ method: normalizeCustomApiMethod($event) })"
                        />
                        <small>POST 加密会走前端加密请求链路。</small>
                      </div>
                      <div class="action-field">
                        <span class="action-field-label">能力标识</span>
                        <n-input
                          :value="activeAction.actionConfig?.capabilityCode || ''"
                          placeholder="例如 customer_audit，可选"
                          @update:value="updateActiveActionConfig({ capabilityCode: $event || '' })"
                        />
                        <small>业务模块存在统一能力处理器时填写；普通接口可不填。</small>
                      </div>
                      <div class="action-field">
                        <span class="action-field-label">接口地址</span>
                        <n-input
                          :value="resolveCustomApiUrl(activeAction)"
                          placeholder="/business/customer/audit/:id"
                          @update:value="updateActiveActionConfig({ url: $event || '' })"
                        />
                        <small>Path 占位写成 :id、:code，参数映射位置选择 Path。</small>
                      </div>
                      <div class="action-field">
                        <span class="action-field-label">成功提示</span>
                        <n-input
                          :value="activeAction.successMessage || activeAction.actionConfig?.successMessage || ''"
                          placeholder="例如 审核成功；留空默认操作成功"
                          @update:value="updateActiveCustomAction({ successMessage: $event || '' })"
                        />
                        <small>接口返回成功后展示，失败提示可在下方单独配置。</small>
                      </div>
                    </div>

                    <div class="api-param-head">
                      <span>API 参数映射</span>
                      <n-button size="small" secondary @click="addApiActionParam">
                        添加参数
                      </n-button>
                    </div>
                    <div v-if="activeAction.actionConfig?.params?.length" class="api-param-list">
                      <div class="api-param-columns">
                        <span>参数名</span>
                        <span>位置</span>
                        <span>来源</span>
                        <span>来源值</span>
                        <span />
                      </div>
                      <div
                        v-for="(param, paramIdx) in activeAction.actionConfig.params"
                        :key="param.clientKey || paramIdx"
                        class="api-param-row"
                      >
                        <n-input
                          :value="param.name"
                          placeholder="参数名"
                          @update:value="updateApiActionParam(paramIdx, { name: normalizeParamName($event) })"
                        />
                        <n-select
                          :value="param.target || ''"
                          :options="apiParamTargetOptions"
                          placeholder="自动"
                          clearable
                          @update:value="updateApiActionParam(paramIdx, { target: $event || '' })"
                        />
                        <n-select
                          :value="param.sourceType || 'rowField'"
                          :options="paramSourceOptions"
                          @update:value="updateApiActionParam(paramIdx, normalizeApiParamSourcePatch($event, param))"
                        />
                        <n-select
                          v-if="param.sourceType === 'rowField'"
                          :value="param.sourceField || ''"
                          :options="rowFieldOptions"
                          filterable
                          clearable
                          placeholder="当前行字段"
                          @update:value="updateApiActionParam(paramIdx, { sourceField: $event || '' })"
                        />
                        <n-select
                          v-else-if="param.sourceType === 'system'"
                          :value="param.sourceField || ''"
                          :options="systemVariableOptions"
                          clearable
                          placeholder="系统变量"
                          @update:value="updateApiActionParam(paramIdx, { sourceField: $event || '' })"
                        />
                        <n-input
                          v-else-if="param.sourceType === 'routeQuery'"
                          :value="param.sourceField || ''"
                          placeholder="路由参数名"
                          @update:value="updateApiActionParam(paramIdx, { sourceField: normalizeParamName($event) })"
                        />
                        <n-input
                          v-else
                          :value="param.value || ''"
                          placeholder="固定值，支持 :id / ${field}"
                          @update:value="updateApiActionParam(paramIdx, { value: $event })"
                        />
                        <n-button quaternary type="error" @click="removeApiActionParam(paramIdx)">
                          删除
                        </n-button>
                      </div>
                    </div>
                    <span v-else class="empty">暂无 API 参数映射</span>
                  </div>
                </n-form-item>
              </section>

              <section class="action-form-section">
                <div class="action-section-head">
                  <strong>权限与反馈</strong>
                  <span>控制是否展示、是否二次确认以及执行后的反馈。</span>
                </div>
                <div class="action-form-grid">
                  <n-form-item label="确认提示">
                    <n-input
                      :value="activeAction.confirmText"
                      placeholder="例如：确认处理 :id 吗？留空则不提示"
                      @update:value="updateActiveCustomAction({ confirmText: $event })"
                    />
                  </n-form-item>
                  <n-form-item label="显示条件">
                    <n-input
                      :value="activeAction.displayCondition || ''"
                      placeholder="例如 status=待处理、status!=已关闭、type in A,B"
                      @update:value="updateActiveCustomAction({ displayCondition: $event || '' })"
                    />
                  </n-form-item>
                  <n-form-item label="权限码">
                    <n-input
                      :value="activeAction.permissionCode || ''"
                      clearable
                      placeholder="例如 ai:business:customer:detail"
                      @update:value="updateActiveCustomAction({ permissionCode: $event || '' })"
                    />
                  </n-form-item>
                  <n-form-item label="成功后行为">
                    <n-select
                      :value="activeAction.successBehavior || 'none'"
                      :options="successBehaviorOptions"
                      @update:value="updateActiveCustomAction({ successBehavior: $event || 'none' })"
                    />
                  </n-form-item>
                </div>
              </section>

              <section v-if="isParamConfigurableAction(activeAction)" class="action-form-section">
                <div class="action-section-head">
                  <strong>参数映射</strong>
                  <span>用于把当前行、路由或系统变量填入目标地址。</span>
                </div>
                <div class="action-param-editor">
                  <div
                    v-for="(param, paramIdx) in (activeAction.params || [])"
                    :key="paramIdx"
                    class="action-param-row"
                  >
                    <n-input
                      :value="param.name"
                      placeholder="参数名，如 id"
                      @update:value="updateActionParam(paramIdx, { name: normalizeParamName($event) })"
                    />
                    <n-select
                      :value="param.sourceType || 'static'"
                      :options="paramSourceOptions"
                      @update:value="updateActionParam(paramIdx, normalizeParamSourcePatch($event, param))"
                    />
                    <n-select
                      v-if="param.sourceType === 'rowField'"
                      :value="param.sourceField || ''"
                      :options="rowFieldOptions"
                      filterable
                      clearable
                      placeholder="当前行字段"
                      @update:value="updateActionParam(paramIdx, buildParamValuePatch({ ...param, sourceType: 'rowField' }, $event))"
                    />
                    <n-select
                      v-else-if="param.sourceType === 'routeQuery'"
                      :value="param.sourceField || ''"
                      :options="routeParamOptions"
                      filterable
                      tag
                      clearable
                      placeholder="路由参数"
                      @update:value="updateActionParam(paramIdx, buildParamValuePatch({ ...param, sourceType: 'routeQuery' }, $event))"
                    />
                    <n-select
                      v-else-if="param.sourceType === 'system'"
                      :value="param.sourceField || ''"
                      :options="systemVariableOptions"
                      clearable
                      placeholder="系统变量"
                      @update:value="updateActionParam(paramIdx, buildParamValuePatch({ ...param, sourceType: 'system' }, $event))"
                    />
                    <n-input
                      v-else
                      :value="param.value"
                      placeholder="固定值"
                      @update:value="updateActionParam(paramIdx, { value: $event })"
                    />
                    <n-button quaternary type="error" @click="removeActionParam(paramIdx)">
                      删除
                    </n-button>
                  </div>
                  <n-button size="small" dashed block @click="addActionParam">
                    + 添加参数
                  </n-button>
                </div>
              </section>
            </n-form>
          </div>
          <div v-else class="action-empty-panel">
            请选择或添加一个自定义操作
          </div>
        </div>

        <template #footer>
          <n-space justify="end">
            <n-button @click="customActionModalOpen = false">
              完成
            </n-button>
          </n-space>
        </template>
      </n-card>
    </n-modal>

    <n-modal v-model:show="sourceModalOpen" preset="card" title="源码编辑" class="list-source-modal" :bordered="false">
      <div class="source-editor-hint">
        支持实时编辑，并保存应用到画布
      </div>
      <n-tabs v-model:value="sourceModalTab" type="line" animated>
        <n-tab-pane name="layout" tab="画布布局 JSON">
          <n-input
            v-model:value="layoutSourceDraft"
            type="textarea"
            :autosize="{ minRows: 18, maxRows: 28 }"
            class="source-code-textarea"
            @update:value="sourceError = ''"
          />
        </n-tab-pane>
        <n-tab-pane name="block" tab="当前区块 JSON" :disabled="!selectedBlock">
          <n-input
            v-model:value="blockSourceDraft"
            type="textarea"
            :autosize="{ minRows: 18, maxRows: 28 }"
            class="source-code-textarea"
            placeholder="请先选中一个区块"
            @update:value="sourceError = ''"
          />
        </n-tab-pane>
      </n-tabs>
      <div v-if="sourceError" class="source-error">
        {{ sourceError }}
      </div>
      <template #footer>
        <div class="source-modal-footer">
          <n-button @click="cancelSourceModalEdit">
            取消
          </n-button>
          <n-button type="primary" @click="applySourceModalCode">
            保存并应用
          </n-button>
        </div>
      </template>
    </n-modal>
  </div>
</template>

<script>
import { useListPageGridDesigner } from './composables/useListPageGridDesigner'
import ListPageBlockPropertyPanel from './ListPageBlockPropertyPanel.vue'
import { listPageDesignerLocalComponents } from './listPageDesignerLocalComponents'

export default {
  name: 'ListPageGridDesigner',
  components: { ...listPageDesignerLocalComponents, ListPageBlockPropertyPanel },
  props: {
  modelValue: {
    type: Object,
    required: true,
  },
  fields: {
    type: Array,
    default: () => [],
  },
  formDesignerSchema: {
    type: Object,
    default: null,
  },
  modelSchema: {
    type: Object,
    default: () => ({}),
  },
  layoutType: {
    type: String,
    default: 'simple-crud',
  },
  pageName: {
    type: String,
    default: '',
  },
  pages: {
    type: Array,
    default: () => [],
  },
  /** 自定义操作「应用内页面」候选；优先于 pages（对象内 list/detail） */
  actionPages: {
    type: Array,
    default: () => [],
  },
  formOptions: {
    type: Array,
    default: () => [],
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
  customActions: {
    type: Array,
    default: null,
  },
  customActionsEditable: {
    type: Boolean,
    default: true,
  },
  panelOnly: {
    type: Boolean,
    default: false,
  },
  activeBlockId: {
    type: String,
    default: '',
  },
},
  emits: ['update:modelValue', 'update:modelSchema', 'update:customActions'],
  setup(props, { emit, expose }) {
    return useListPageGridDesigner(props, emit, expose)
  },
}
</script>


<style scoped src="./list-page-grid-designer-shell.css"></style>
<style scoped src="./list-page-grid-designer-panels.css"></style>
