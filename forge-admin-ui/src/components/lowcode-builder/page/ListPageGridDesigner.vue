<template>
  <div
    class="list-grid-designer"
    :class="{ 'left-collapsed': paletteCollapsed && !canvasFocusMode, 'right-collapsed': propertyCollapsed && !canvasFocusMode, 'canvas-focused': canvasFocusMode, 'panel-only': panelOnly, readonly }"
  >
    <button
      v-if="paletteCollapsed && !readonly && !canvasFocusMode"
      type="button"
      class="side-rail-toggle-button left"
      title="展开页面组件"
      @click="paletteCollapsed = false"
    >
      <n-icon><ChevronForwardOutline /></n-icon>
    </button>

    <aside v-else-if="!readonly && !canvasFocusMode" class="palette-panel">
      <div class="palette-panel-head">
        <div>
          <div class="panel-title">
            页面组件
          </div>
          <div class="panel-desc">
            拖拽组件到画布，右侧可调整样式
          </div>
          <div class="palette-stats">
            <span>共 {{ paletteStats.total }} 个</span>
            <span v-if="paletteStats.filtered !== paletteStats.total">匹配 {{ paletteStats.filtered }} 个</span>
            <span>{{ groupedBlocks.length }} 组</span>
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
        <section v-for="group in groupedBlocks" :key="group.key" class="palette-group">
          <div class="group-title">
            <span>{{ group.title }}</span>
            <em>{{ group.items.length }}</em>
          </div>
          <div class="palette-list">
            <button
              v-for="item in group.items"
              :key="item.blockType"
              class="palette-item"
              :class="{
                'is-disabled': isBlockDisabled(item),
                'is-existing': resolveBlockDisabledReason(item) === '已在画布中',
                'is-unavailable': resolveBlockDisabledReason(item) === '当前布局不可用',
              }"
              type="button"
              :draggable="!isBlockDisabled(item)"
              :aria-disabled="isBlockDisabled(item)"
              :title="resolveBlockDisabledReason(item) || item.desc"
              @dragstart="handlePaletteDragStart($event, item)"
              @dragend="resetCanvasDragState"
              @click="handlePaletteClick(item)"
            >
              <span class="item-icon">
                <n-icon><component :is="resolvePaletteItemIcon(item)" /></n-icon>
              </span>
              <span class="item-main">
                <span class="item-title">{{ item.title }}</span>
                <span class="item-desc">{{ item.desc }}</span>
              </span>
              <span v-if="resolveBlockDisabledReason(item)" class="item-lock-reason">
                {{ resolveBlockDisabledReason(item) }}
              </span>
            </button>
          </div>
        </section>
        <div v-if="!groupedBlocks.length" class="palette-empty">
          没有匹配的区块
        </div>
      </div>
    </aside>

    <main
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
              <div v-if="!readonly" class="block-node-overlay">
                <span
                  class="block-drag-handle"
                  title="拖动区块"
                  @pointerdown.stop="startMove(block, $event)"
                  @click.stop
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
                  :options="resolveBlockMoreOptions(block)"
                  @select="key => handleBlockMoreSelect(key, block)"
                >
                  <button
                    type="button"
                    class="block-menu-trigger"
                    title="更多操作"
                    @click.stop
                    @mousedown.stop
                  >
                    <n-icon><EllipsisHorizontalOutline /></n-icon>
                  </button>
                </n-dropdown>
              </div>
              <GridBlockRenderer
                :block="block"
                :fields="fields"
                :selected="block.id === selectedBlockId"
                :selected-block-id="selectedBlockId || ''"
                :readonly="readonly"
                :runtime-crud-props="resolvedRuntimeCrudProps"
                :runtime-record="runtimeRecord"
                :runtime-tree-active-key="runtimeTreeActiveKey"
                :active-drop-cell="activeDropCell"
                :nested-moving-block-id="nestedMovingBlockId"
                @child-block-select="handleBlockClick"
                @child-block-menu-select="handleNestedBlockMenuSelect"
                @block-props-update="handleBlockPropsUpdate"
                @child-block-drag-start="handleNestedBlockDragStart"
                @child-block-move-start="payload => startNestedMove(payload.block, payload.event)"
                @child-block-drag-end="resetCanvasDragState"
                @child-block-resize-start="payload => startNestedResize(payload.block, payload.event, payload.anchor)"
                @runtime-tree-select="handleRuntimeTreeSelect"
                @tree-panel-collapse-change="handleTreePanelCollapseChange"
                @crud-preview-state-change="handleCrudPreviewStateChange"
              />
              <template v-if="!readonly">
                <button
                  v-for="anchor in resizeAnchors"
                  :key="anchor"
                  type="button"
                  class="resize-anchor"
                  :class="`anchor-${anchor}`"
                  title="调整区块大小"
                  @pointerdown.stop="startResize(block, $event, anchor)"
                />
              </template>
            </div>
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
      v-if="propertyCollapsed && !readonly && !canvasFocusMode"
      type="button"
      class="side-rail-toggle-button right"
      title="展开配置区块"
      @click="propertyCollapsed = false"
    >
      <n-icon><ChevronBackOutline /></n-icon>
    </button>

    <aside v-else-if="!readonly && !canvasFocusMode" class="block-property-panel">
      <div class="property-panel-head">
        <div>
          <div class="property-panel-title">
            配置组件
          </div>
          <div class="property-panel-desc">
            选中画布组件后在这里调整属性、字段、动作和外观。
          </div>
        </div>
        <n-button
          class="panel-collapse-button"
          circle
          size="small"
          secondary
          title="收起配置区块"
          @click="propertyCollapsed = true"
        >
          <template #icon>
            <n-icon><ChevronForwardOutline /></n-icon>
          </template>
        </n-button>
      </div>
      <div class="property-search designer-panel-search">
        <n-input
          v-model:value="propertyKeyword"
          clearable
          size="small"
          placeholder="搜索配置项，例如：接口、弹窗、工具栏、树、字段"
        >
          <template #prefix>
            <n-icon><SearchOutline /></n-icon>
          </template>
        </n-input>
      </div>
      <div class="property-panel-tabs" aria-label="配置类型">
        <button type="button" :class="{ active: propertyPanelTab === 'props' }" @click="selectPropertyPanelTab('props')">
          <n-icon><SettingsOutline /></n-icon>
          属性
        </button>
        <button type="button" :class="{ active: propertyPanelTab === 'style' }" @click="selectPropertyPanelTab('style')">
          <n-icon><ColorPaletteOutline /></n-icon>
          样式
        </button>
        <button type="button" :class="{ active: propertyPanelTab === 'interaction' }" @click="selectPropertyPanelTab('interaction')">
          <n-icon><FlashOutline /></n-icon>
          交互
        </button>
      </div>
      <div ref="propertyPanelRef" class="property-panel">
        <div v-if="!selectedBlock" class="property-empty">
          <p>选中画布上的组件以编辑属性</p>
        </div>
        <div v-else class="property-body">
          <div v-show="propertyPanelTab === 'props'" class="prop-head">
            <div>
              <div class="prop-title">
                {{ selectedBlockMeta?.title || selectedBlock.blockType }}
              </div>
              <div class="prop-meta">
                {{ selectedBlock.gridW }} 列 × {{ selectedBlock.gridH }} 行
              </div>
            </div>
          </div>

          <n-form size="small" label-placement="top" :show-feedback="false">
            <div v-show="propertyPanelTab === 'props'" class="property-tab-content">
              <div class="property-search-anchor" data-property-search="属性 区块标题 组件标题 默认排序 字段 行间距" />
              <n-form-item label="区块标题">
                <n-input
                  :value="selectedBlock.label"
                  @update:value="patchBlock(selectedBlock.id, { label: $event })"
                />
              </n-form-item>

              <template v-if="selectedBlock.blockType === 'grid-layout'">
                <n-divider>栅格配置</n-divider>
                <n-form-item label="栅格结构">
                  <div class="grid-config-grid three">
                    <label class="grid-config-field">
                      <span>总列数</span>
                      <n-input-number
                        :value="selectedBlock.props?.columns || 24"
                        :min="1"
                        :max="24"
                        size="small"
                        placeholder="24"
                        @update:value="updateGridLayoutStructure({ columns: $event || 24 })"
                      />
                    </label>
                    <label class="grid-config-field">
                      <span>列间距</span>
                      <n-input-number
                        :value="selectedBlock.props?.gutter ?? selectedBlock.props?.gap ?? 16"
                        :min="0"
                        size="small"
                        placeholder="16"
                        @update:value="patchBlockProps(selectedBlock.id, { gutter: $event ?? 0 })"
                      />
                    </label>
                    <label class="grid-config-field">
                      <span>组件行距</span>
                      <n-input-number
                        :value="selectedBlock.props?.rowGap ?? 0"
                        :min="0"
                        size="small"
                        placeholder="0"
                        @update:value="patchBlockProps(selectedBlock.id, { rowGap: $event ?? 0 })"
                      />
                    </label>
                  </div>
                </n-form-item>
                <n-form-item label="格子样式">
                  <div class="grid-config-grid four">
                    <label class="grid-config-field">
                      <span>最小高度</span>
                      <n-input-number
                        :value="selectedBlock.props?.cellMinHeight || 120"
                        :min="24"
                        size="small"
                        placeholder="120"
                        @update:value="patchBlockProps(selectedBlock.id, { cellMinHeight: $event || 24 })"
                      />
                    </label>
                    <label class="grid-config-field">
                      <span>垂直位置</span>
                      <n-select
                        :value="selectedBlock.props?.alignItems || 'stretch'"
                        :options="gridVerticalAlignOptions"
                        size="small"
                        @update:value="patchBlockProps(selectedBlock.id, { alignItems: $event })"
                      />
                    </label>
                    <label class="grid-config-field">
                      <span>水平位置</span>
                      <n-select
                        :value="selectedBlock.props?.justifyItems || 'stretch'"
                        :options="gridHorizontalAlignOptions"
                        size="small"
                        @update:value="patchBlockProps(selectedBlock.id, { justifyItems: $event })"
                      />
                    </label>
                    <label class="grid-config-switch">
                      <span>显示格子边框</span>
                      <n-switch
                        :value="selectedBlock.props?.showCellBorder !== false"
                        @update:value="patchBlockProps(selectedBlock.id, { showCellBorder: $event })"
                      />
                    </label>
                  </div>
                </n-form-item>
                <n-form-item label="格子背景">
                  <n-color-picker
                    :value="selectedBlock.props?.cellBackground || 'transparent'"
                    :show-alpha="true"
                    size="small"
                    @update:value="patchBlockProps(selectedBlock.id, { cellBackground: $event || 'transparent' })"
                  />
                </n-form-item>
                <n-form-item label="格子内容">
                  <div class="container-child-editor">
                    <div
                      v-for="(cell, idx) in selectedGridCells"
                      :key="cell.key"
                      class="grid-cell-editor"
                    >
                      <div class="grid-cell-editor-head">
                        <n-input
                          :value="cell.title"
                          size="small"
                          :placeholder="`栅格 ${idx + 1}`"
                          @update:value="updateGridCell(idx, { title: $event })"
                        />
                        <n-input-number
                          :value="cell.span || 6"
                          :min="1"
                          :max="selectedBlock.props?.columns || 24"
                          size="small"
                          class="grid-cell-span-input"
                          placeholder="span"
                          @update:value="updateGridCell(idx, { span: $event || 1 })"
                        />
                        <n-button size="tiny" quaternary type="error" @click="removeGridCell(idx)">
                          删格
                        </n-button>
                      </div>
                      <div
                        v-for="child in (cell.children || [])"
                        :key="child.id"
                        class="container-child-row"
                      >
                        <span>{{ child.label || child.blockType }}</span>
                        <n-button size="tiny" quaternary type="error" @click="removeGridCellChild(selectedBlock.id, cell.key, child.id)">
                          删除
                        </n-button>
                      </div>
                      <n-select
                        :options="childBlockTypeOptions"
                        size="small"
                        placeholder="添加组件到此格"
                        clearable
                        @update:value="value => value && appendGridCellChild(selectedBlock.id, cell.key, value)"
                      />
                    </div>
                    <n-button size="small" dashed block @click="addGridCell">
                      + 添加格子
                    </n-button>
                  </div>
                </n-form-item>
              </template>
            </div>

            <div v-show="propertyPanelTab === 'style'" class="property-tab-content">
              <div class="property-search-anchor" data-property-search="样式 位置 尺寸 坐标 左 上 x y 宽度 高度 背景 背景色 边框 边框色 圆角 阴影 内边距 外边距 padding margin 外观 装饰 颜色 自定义 style customStyle" />
              <div class="position-control">
                <div class="position-axis-grid">
                  <label class="position-number-field">
                    <span>左 X</span>
                    <n-input-number
                      :value="selectedBlockFrame.x"
                      :min="0"
                      size="small"
                      :show-button="false"
                      @update:value="patchBlockFrame(selectedBlock.id, { x: $event ?? 0 })"
                    />
                    <em>px</em>
                  </label>
                  <label class="position-number-field">
                    <span>上 Y</span>
                    <n-input-number
                      :value="selectedBlockFrame.y"
                      :min="0"
                      size="small"
                      :show-button="false"
                      @update:value="patchBlockFrame(selectedBlock.id, { y: $event ?? 0 })"
                    />
                    <em>px</em>
                  </label>
                </div>

                <div class="position-rule">
                  <div class="position-rule-head">
                    <span>宽度</span>
                    <label v-if="selectedBlockWidthMode === 'fixed'" class="position-inline-number">
                      <n-input-number
                        :value="selectedBlockFixedWidth"
                        :min="24"
                        size="tiny"
                        :show-button="false"
                        @update:value="patchBlockFrame(selectedBlock.id, { width: $event ?? 24 })"
                      />
                      <em>px</em>
                    </label>
                  </div>
                  <div class="designer-segmented-control width-mode-control">
                    <button
                      type="button"
                      :class="{ active: selectedBlockWidthMode === 'auto' }"
                      @click="setBlockWidthMode(selectedBlock.id, 'auto')"
                    >
                      <n-icon class="designer-segmented-icon">
                        <RemoveOutline />
                      </n-icon>
                      <span>默认宽度</span>
                    </button>
                    <button
                      type="button"
                      :class="{ active: selectedBlockWidthMode === 'full' }"
                      @click="setBlockWidthMode(selectedBlock.id, 'full')"
                    >
                      <n-icon class="designer-segmented-icon">
                        <SwapHorizontalOutline />
                      </n-icon>
                      <span>填充容器</span>
                    </button>
                    <button
                      type="button"
                      :class="{ active: selectedBlockWidthMode === 'fixed' }"
                      @click="setBlockWidthMode(selectedBlock.id, 'fixed')"
                    >
                      <n-icon class="designer-segmented-icon">
                        <ResizeOutline />
                      </n-icon>
                      <span>固定宽度</span>
                    </button>
                  </div>
                </div>

                <div class="position-rule">
                  <div class="position-rule-head">
                    <span>高度</span>
                    <label v-if="selectedBlockHeightMode === 'fixed'" class="position-inline-number">
                      <n-input-number
                        :value="selectedBlockFrame.height"
                        :min="24"
                        size="tiny"
                        :show-button="false"
                        @update:value="patchBlockFrame(selectedBlock.id, { height: $event ?? 24 })"
                      />
                      <em>px</em>
                    </label>
                  </div>
                  <div class="designer-segmented-control height-mode-control">
                    <button
                      type="button"
                      :class="{ active: selectedBlockHeightMode === 'fixed' }"
                      @click="setBlockHeightMode(selectedBlock.id, 'fixed')"
                    >
                      <n-icon class="designer-segmented-icon vertical-icon">
                        <RemoveOutline />
                      </n-icon>
                      <span>默认高度</span>
                    </button>
                    <button
                      type="button"
                      :class="{ active: selectedBlockHeightMode === 'auto' }"
                      @click="setBlockHeightMode(selectedBlock.id, 'auto')"
                    >
                      <n-icon class="designer-segmented-icon">
                        <ResizeOutline />
                      </n-icon>
                      <span>适应内容</span>
                    </button>
                    <button
                      type="button"
                      :class="{ active: selectedBlockHeightMode === 'full' }"
                      @click="setBlockHeightMode(selectedBlock.id, 'full')"
                    >
                      <n-icon class="designer-segmented-icon vertical-icon">
                        <SwapHorizontalOutline />
                      </n-icon>
                      <span>填充容器</span>
                    </button>
                  </div>
                </div>

                <div class="position-rule">
                  <div class="position-rule-head">
                    <span>对齐方式</span>
                  </div>
                  <div class="designer-segmented-control align-mode-control">
                    <button type="button" :class="{ active: selectedBlockContentAlign === 'left' }" @click="setBlockContentAlign(selectedBlock.id, 'left')">
                      <n-icon class="designer-segmented-icon">
                        <ReorderThreeOutline />
                      </n-icon>
                      <span>左对齐</span>
                    </button>
                    <button type="button" :class="{ active: selectedBlockContentAlign === 'center' }" @click="setBlockContentAlign(selectedBlock.id, 'center')">
                      <n-icon class="designer-segmented-icon">
                        <ReorderThreeOutline />
                      </n-icon>
                      <span>居中对齐</span>
                    </button>
                  </div>
                </div>
              </div>

              <template v-if="!panelOnly">
                <n-divider>外观样式</n-divider>
                <n-form-item label="临界值 px（最小 / 最大）">
                  <div class="style-grid four">
                    <n-input-number
                      :value="toNumberOrNull(selectedBlockStyle.minWidth)"
                      :min="0"
                      :show-button="false"
                      placeholder="最小宽"
                      clearable
                      @update:value="patchBlockStyle(selectedBlock.id, { minWidth: $event ?? '' })"
                    />
                    <n-input-number
                      :value="toNumberOrNull(selectedBlockStyle.maxWidth)"
                      :min="0"
                      :show-button="false"
                      placeholder="最大宽"
                      clearable
                      @update:value="patchBlockStyle(selectedBlock.id, { maxWidth: $event ?? '' })"
                    />
                    <n-input-number
                      :value="toNumberOrNull(selectedBlockStyle.minHeight)"
                      :min="0"
                      :show-button="false"
                      placeholder="最小高"
                      clearable
                      @update:value="patchBlockStyle(selectedBlock.id, { minHeight: $event ?? '' })"
                    />
                    <n-input-number
                      :value="toNumberOrNull(selectedBlockStyle.maxHeight)"
                      :min="0"
                      :show-button="false"
                      placeholder="最大高"
                      clearable
                      @update:value="patchBlockStyle(selectedBlock.id, { maxHeight: $event ?? '' })"
                    />
                  </div>
                </n-form-item>
                <div class="appearance-control list-appearance-control">
                  <div class="appearance-field">
                    <label>背景色</label>
                    <div class="appearance-input-shell">
                      <label class="appearance-swatch" :style="{ backgroundColor: selectedBlockBackgroundPreview }" title="选择背景色">
                        <input
                          type="color"
                          :value="selectedBlockBackgroundColorInput"
                          @input="updateSelectedBlockBackground($event.target.value)"
                        >
                      </label>
                      <input
                        :value="selectedBlockBackgroundHex"
                        class="appearance-hex-input"
                        placeholder="透明"
                        @input="updateSelectedBlockBackground($event.target.value)"
                      >
                      <span class="appearance-percent">100%</span>
                    </div>
                  </div>
                  <div class="appearance-field">
                    <label>边框 (Border)</label>
                    <div class="appearance-input-shell">
                      <select
                        :value="selectedBlockStyle.borderStyle"
                        class="appearance-select"
                        @change="updateSelectedBlockBorderStyle($event.target.value)"
                      >
                        <option value="solid">
                          实线
                        </option>
                        <option value="dashed">
                          虚线
                        </option>
                        <option value="none">
                          无
                        </option>
                      </select>
                      <label class="appearance-swatch" :style="{ backgroundColor: selectedBlockBorderPreview }" title="选择边框颜色">
                        <input
                          type="color"
                          :value="selectedBlockBorderPreview"
                          @input="updateSelectedBlockBorderColor($event.target.value)"
                        >
                      </label>
                      <input
                        :value="selectedBlockBorderHex"
                        class="appearance-hex-input"
                        placeholder="E4E4E7"
                        @input="updateSelectedBlockBorderColor($event.target.value)"
                      >
                    </div>
                  </div>
                  <div class="appearance-field">
                    <label>圆角 (Border Radius)</label>
                    <div class="appearance-radius-shell">
                      <span>R</span>
                      <input
                        :value="Number(selectedBlockStyle.borderRadius)"
                        type="number"
                        min="0"
                        max="48"
                        @input="patchBlockStyle(selectedBlock.id, { borderRadius: Number($event.target.value || 0) })"
                      >
                    </div>
                  </div>
                  <div class="appearance-field">
                    <label class="appearance-row-label">
                      <span>阴影 (Shadow)</span>
                      <select
                        :value="selectedBlockStyle.boxShadow"
                        class="appearance-plain-select"
                        @change="patchBlockStyle(selectedBlock.id, { boxShadow: $event.target.value || 'none' })"
                      >
                        <option
                          v-for="option in shadowOptions"
                          :key="option.value"
                          :value="option.value"
                        >
                          {{ option.label }}
                        </option>
                      </select>
                    </label>
                  </div>
                </div>
                <n-form-item label="内边距 / 外边距">
                  <div class="style-grid">
                    <n-input
                      :value="String(selectedBlockStyle.padding ?? 0)"
                      size="small"
                      placeholder="内边距，如 12 或 8px 12px"
                      @update:value="patchBlockStyle(selectedBlock.id, { padding: normalizeSpacingValue($event) })"
                    />
                    <n-input
                      :value="String(selectedBlockStyle.margin ?? 0)"
                      size="small"
                      placeholder="外边距，如 12 或 8px 12px"
                      @update:value="patchBlockStyle(selectedBlock.id, { margin: normalizeSpacingValue($event) })"
                    />
                  </div>
                </n-form-item>
                <n-form-item label="自定义 style">
                  <n-input
                    :value="selectedBlockStyle.customStyle"
                    type="textarea"
                    :rows="3"
                    placeholder="例如：backdrop-filter: blur(8px);"
                    @update:value="patchBlockStyle(selectedBlock.id, { customStyle: $event })"
                  />
                </n-form-item>
              </template>
            </div>

            <div v-show="propertyPanelTab === 'interaction'" class="property-tab-content">
              <div class="property-search-anchor" data-property-search="交互 事件 生命周期 回调 点击 加载完成 提交成功 行点击 跳转 刷新 过滤 接口请求 自定义脚本 参数 目标页面 目标表单" />
              <n-divider>事件配置</n-divider>
              <div class="event-editor">
                <div v-if="!selectedBlockEvents.length" class="copy-empty-state">
                  <span class="copy-empty-icon">+</span>
                  <strong>暂未配置事件</strong>
                  <small>可为点击、加载完成、行点击、提交成功等时机添加跳转、刷新、弹窗或接口请求动作。</small>
                </div>
                <div
                  v-for="(eventItem, eventIdx) in selectedBlockEvents"
                  :key="eventItem.id || eventIdx"
                  class="event-row"
                >
                  <div class="event-row-head">
                    <span>{{ eventTriggerText(eventItem.trigger) }} · {{ eventActionText(eventItem.action) }}</span>
                    <n-button size="tiny" quaternary type="error" @click="removeBlockEvent(eventIdx)">
                      删除
                    </n-button>
                  </div>
                  <div class="event-grid">
                    <label class="event-setting-field">
                      <span>什么时候触发</span>
                      <n-select
                        :value="eventItem.trigger"
                        :options="eventTriggerOptions"
                        size="small"
                        placeholder="触发时机"
                        @update:value="updateBlockEvent(eventIdx, { trigger: $event })"
                      />
                    </label>
                    <label class="event-setting-field">
                      <span>触发后做什么</span>
                      <n-select
                        :value="eventItem.action"
                        :options="blockEventActionOptions"
                        size="small"
                        placeholder="执行动作"
                        @update:value="updateBlockEvent(eventIdx, { action: $event })"
                      />
                    </label>
                    <label class="event-setting-field">
                      <span>影响哪个区块</span>
                      <n-select
                        :value="eventItem.targetBlockId"
                        :options="blockTargetOptions"
                        size="small"
                        clearable
                        placeholder="可选"
                        @update:value="updateBlockEvent(eventIdx, { targetBlockId: $event || '' })"
                      />
                    </label>
                    <label class="event-setting-field">
                      <span>跳转到页面</span>
                      <n-select
                        :value="eventItem.targetPageKey"
                        :options="pageTargetOptions"
                        size="small"
                        clearable
                        placeholder="可选"
                        @update:value="updateBlockEvent(eventIdx, { targetPageKey: $event || '' })"
                      />
                    </label>
                    <label v-if="formTargetOptions.length" class="event-setting-field">
                      <span>打开哪个表单</span>
                      <n-select
                        :value="eventItem.targetFormKey"
                        :options="formTargetOptions"
                        size="small"
                        clearable
                        placeholder="可选"
                        @update:value="updateBlockEvent(eventIdx, { targetFormKey: $event || '' })"
                      />
                    </label>
                    <label class="event-setting-field">
                      <span>备注说明</span>
                      <n-input
                        :value="eventItem.description"
                        size="small"
                        placeholder="例如点击行打开详情"
                        @update:value="updateBlockEvent(eventIdx, { description: $event })"
                      />
                    </label>
                  </div>
                  <div class="event-param-list">
                    <div
                      v-for="(param, paramIdx) in (eventItem.params || [])"
                      :key="paramIdx"
                      class="event-param-row"
                    >
                      <n-input
                        :value="param.name"
                        size="tiny"
                        placeholder="参数名"
                        @update:value="updateBlockEventParam(eventIdx, paramIdx, { name: normalizeParamName($event) })"
                      />
                      <n-select
                        :value="param.sourceType || 'static'"
                        :options="paramSourceOptions"
                        size="tiny"
                        @update:value="updateBlockEventParam(eventIdx, paramIdx, normalizeParamSourcePatch($event, param))"
                      />
                      <n-select
                        v-if="param.sourceType === 'rowField'"
                        :value="param.sourceField || ''"
                        :options="rowFieldOptions"
                        size="tiny"
                        filterable
                        clearable
                        placeholder="当前行字段"
                        @update:value="updateBlockEventParam(eventIdx, paramIdx, buildParamValuePatch({ ...param, sourceType: 'rowField' }, $event))"
                      />
                      <n-select
                        v-else-if="param.sourceType === 'routeQuery'"
                        :value="param.sourceField || ''"
                        :options="routeParamOptions"
                        size="tiny"
                        filterable
                        tag
                        clearable
                        placeholder="路由参数"
                        @update:value="updateBlockEventParam(eventIdx, paramIdx, buildParamValuePatch({ ...param, sourceType: 'routeQuery' }, $event))"
                      />
                      <n-select
                        v-else-if="param.sourceType === 'system'"
                        :value="param.sourceField || ''"
                        :options="systemVariableOptions"
                        size="tiny"
                        clearable
                        placeholder="系统变量"
                        @update:value="updateBlockEventParam(eventIdx, paramIdx, buildParamValuePatch({ ...param, sourceType: 'system' }, $event))"
                      />
                      <n-input
                        v-else
                        :value="param.value"
                        size="tiny"
                        placeholder="固定值"
                        @update:value="updateBlockEventParam(eventIdx, paramIdx, { value: $event })"
                      />
                      <n-button size="tiny" quaternary @click="removeBlockEventParam(eventIdx, paramIdx)">
                        删
                      </n-button>
                    </div>
                    <n-button size="tiny" dashed block @click="addBlockEventParam(eventIdx)">
                      + 添加参数
                    </n-button>
                  </div>
                </div>
                <n-button size="small" dashed block @click="addBlockEvent">
                  + 添加事件
                </n-button>
              </div>

              <n-divider>运行规则</n-divider>
              <RuntimeRulesEditor
                title="区块运行规则"
                :rules="selectedBlock.props?.runtimeRules || []"
                :field-options="runtimeRuleFieldOptions"
                @update:rules="patchBlockProps(selectedBlock.id, { runtimeRules: $event })"
              />
            </div>

            <div v-show="propertyPanelTab === 'props'" class="property-tab-content">
              <!-- Search / Table 字段配置 -->
              <template v-if="isFieldConfigurableBlock(selectedBlock.blockType)">
                <n-form-item :label="resolveFieldConfigLabel(selectedBlock.blockType)">
                  <n-button size="small" type="primary" secondary @click="openFieldDrawer('table')">
                    {{ selectedBlock.blockType === 'AiCrudPage' ? '配置列表字段' : '配置字段' }}（{{ selectedBlock.fieldRefs?.length || 0 }}/{{ fields.length }}）
                  </n-button>
                </n-form-item>
                <n-form-item v-if="selectedBlock.blockType === 'search-form'" label="可折叠">
                  <n-switch
                    :value="!!selectedBlock.props?.collapsible"
                    size="small"
                    @update:value="patchBlockProps(selectedBlock.id, { collapsible: $event })"
                  />
                </n-form-item>
                <template v-if="['data-table', 'AiCrudPage', 'AiTable'].includes(selectedBlock.blockType)">
                  <n-form-item label="默认排序字段">
                    <n-select
                      :value="selectedBlock.props?.defaultSortField || 'id'"
                      :options="sortFieldOptions"
                      filterable
                      clearable
                      @update:value="patchBlockProps(selectedBlock.id, { defaultSortField: $event || 'id' })"
                    />
                  </n-form-item>
                  <n-form-item label="默认排序方式">
                    <n-select
                      :value="selectedBlock.props?.defaultSortOrder || 'desc'"
                      :options="sortOrderOptions"
                      @update:value="patchBlockProps(selectedBlock.id, { defaultSortOrder: $event || 'desc' })"
                    />
                  </n-form-item>
                  <n-form-item label="全部列对齐">
                    <n-select
                      :value="selectedBlock.props?.globalAlign || 'left'"
                      :options="alignOptions"
                      @update:value="applySelectedTableGlobalAlign"
                    />
                  </n-form-item>
                  <n-form-item label="行间距">
                    <n-input-number
                      :value="selectedBlock.props?.rowGap ?? 8"
                      :min="0"
                      :max="32"
                      :step="2"
                      :show-button="false"
                      @update:value="patchBlockProps(selectedBlock.id, { rowGap: normalizeTableRowGap($event) })"
                    />
                  </n-form-item>
                </template>
              </template>

              <!-- System Ai components -->
              <template v-if="['AiCrudPage', 'AiForm', 'AiTable'].includes(selectedBlock.blockType)">
                <n-form-item label="组件标题">
                  <n-input
                    :value="selectedBlock.props?.title"
                    @update:value="patchBlockProps(selectedBlock.id, { title: $event })"
                  />
                </n-form-item>
                <n-collapse v-model:expanded-names="propertyCollapseExpandedNames" class="advanced-config-collapse">
                  <n-collapse-item v-if="propertySectionVisible(['基础配置', '接口', '基础路径', '行主键', '真实接口预览', '响应字段', '表单布局'])" name="base" title="接口与数据源">
                    <div class="property-search-anchor" data-property-search="基础配置 接口 数据源 基础路径 行主键 真实接口预览 响应字段 表单布局 api list detail create update delete import export" />
                    <template v-if="selectedBlock.blockType === 'AiCrudPage'">
                      <n-form-item label="基础路径 / 行主键" class="copy-form-item">
                        <div class="api-base-row">
                          <n-input
                            class="api-base-input"
                            :value="selectedBlock.props?.api || defaultApiValues.api"
                            :placeholder="defaultApiValues.api"
                            @update:value="patchBlockProps(selectedBlock.id, { api: $event })"
                          />
                          <n-input
                            class="api-key-input"
                            :value="selectedBlock.props?.rowKey || 'id'"
                            placeholder="id"
                            @update:value="patchBlockProps(selectedBlock.id, { rowKey: $event || 'id' })"
                          />
                        </div>
                      </n-form-item>
                      <n-form-item label="真实接口预览" class="copy-form-item">
                        <div class="preview-config-panel">
                          <div class="preview-config-head">
                            <span>启用真实请求</span>
                            <n-switch
                              :value="selectedBlock.props?.previewLiveData === true"
                              @update:value="patchBlockProps(selectedBlock.id, {
                                previewLiveData: $event,
                                previewMode: $event ? (selectedBlock.props?.previewMode === 'mock' ? 'realList' : (selectedBlock.props?.previewMode || 'realList')) : 'mock',
                                lastPreviewStatus: $event ? 'loading' : 'idle',
                                lastPreviewMessage: $event ? '正在请求真实接口预览' : '当前使用模拟预览，不请求接口',
                                lastPreviewError: '',
                              })"
                            />
                          </div>
                          <div class="preview-config-grid">
                            <n-select
                              :value="selectedBlock.props?.previewMode || (selectedBlock.props?.previewLiveData === true ? 'realList' : 'mock')"
                              :options="crudPreviewModeOptions"
                              @update:value="patchBlockProps(selectedBlock.id, {
                                previewMode: $event || 'mock',
                                previewLiveData: $event !== 'mock',
                                lastPreviewStatus: $event === 'mock' ? 'idle' : 'loading',
                                lastPreviewMessage: $event === 'mock' ? '当前使用模拟预览，不请求接口' : '正在请求真实接口预览',
                                lastPreviewError: '',
                              })"
                            />
                            <n-input
                              :value="selectedBlock.props?.previewRecordId || ''"
                              :disabled="!['edit', 'detail'].includes(selectedBlock.props?.previewMode)"
                              placeholder="编辑/详情记录 ID"
                              @update:value="patchBlockProps(selectedBlock.id, { previewRecordId: $event || '' })"
                            />
                          </div>
                          <div
                            class="preview-status"
                            :class="`is-${selectedBlock.props?.lastPreviewStatus || 'idle'}`"
                          >
                            <strong>{{ crudPreviewStatusText(selectedBlock.props?.lastPreviewStatus) }}</strong>
                            <span>{{ selectedBlock.props?.lastPreviewMessage || '默认使用模拟数据预览；打开真实请求后会显示接口请求状态。' }}</span>
                            <em v-if="selectedBlock.props?.lastPreviewError">{{ selectedBlock.props.lastPreviewError }}</em>
                          </div>
                          <div class="field-help">
                            默认 mock，不请求接口；选择列表/新增/编辑/详情真实预览后，中间画布会请求接口并打开对应状态。编辑和详情建议填写记录 ID。
                          </div>
                        </div>
                      </n-form-item>
                      <n-form-item label="接口地址" class="copy-form-item">
                        <div class="api-config-grid">
                          <div class="api-config-title">
                            API 接口地址配置
                          </div>
                          <div class="api-endpoint-row">
                            <span class="api-method-badge get">GET</span>
                            <n-input
                              :value="selectedBlock.props?.listApi || defaultApiValues.listApi"
                              :placeholder="defaultApiValues.listApi"
                              @update:value="patchBlockProps(selectedBlock.id, { listApi: $event })"
                            />
                          </div>
                          <div class="api-endpoint-row">
                            <span class="api-method-badge get">GET</span>
                            <n-input
                              :value="selectedBlock.props?.detailApi || defaultApiValues.detailApi"
                              :placeholder="defaultApiValues.detailApi"
                              @update:value="patchBlockProps(selectedBlock.id, { detailApi: $event })"
                            />
                          </div>
                          <div class="api-endpoint-row">
                            <span class="api-method-badge post">POST</span>
                            <n-input
                              :value="selectedBlock.props?.createApi || defaultApiValues.createApi"
                              :placeholder="defaultApiValues.createApi"
                              @update:value="patchBlockProps(selectedBlock.id, { createApi: $event })"
                            />
                          </div>
                          <div class="api-endpoint-row">
                            <span class="api-method-badge put">PUT</span>
                            <n-input
                              :value="selectedBlock.props?.updateApi || defaultApiValues.updateApi"
                              :placeholder="defaultApiValues.updateApi"
                              @update:value="patchBlockProps(selectedBlock.id, { updateApi: $event })"
                            />
                          </div>
                          <div class="api-endpoint-row">
                            <span class="api-method-badge delete">DEL</span>
                            <n-input
                              :value="selectedBlock.props?.deleteApi || defaultApiValues.deleteApi"
                              :placeholder="defaultApiValues.deleteApi"
                              @update:value="patchBlockProps(selectedBlock.id, { deleteApi: $event })"
                            />
                          </div>
                          <div class="api-endpoint-row">
                            <span class="api-method-badge post">POST</span>
                            <n-input
                              :value="selectedBlock.props?.importApi || defaultApiValues.importApi"
                              :placeholder="defaultApiValues.importApi"
                              @update:value="patchBlockProps(selectedBlock.id, { importApi: $event })"
                            />
                          </div>
                          <div class="api-endpoint-row">
                            <span class="api-method-badge get">GET</span>
                            <n-input
                              :value="selectedBlock.props?.exportApi || defaultApiValues.exportApi"
                              :placeholder="defaultApiValues.exportApi"
                              @update:value="patchBlockProps(selectedBlock.id, { exportApi: $event })"
                            />
                          </div>
                        </div>
                      </n-form-item>
                      <n-form-item label="响应字段">
                        <div class="style-grid three">
                          <n-select
                            :value="selectedBlock.props?.listMethod || 'get'"
                            :options="requestMethodOptions"
                            @update:value="patchBlockProps(selectedBlock.id, { listMethod: $event || 'get' })"
                          />
                          <n-input
                            :value="selectedBlock.props?.listDataField || 'records'"
                            placeholder="records"
                            @update:value="patchBlockProps(selectedBlock.id, { listDataField: $event || 'records' })"
                          />
                          <n-input
                            :value="selectedBlock.props?.listTotalField || 'total'"
                            placeholder="total"
                            @update:value="patchBlockProps(selectedBlock.id, { listTotalField: $event || 'total' })"
                          />
                        </div>
                      </n-form-item>
                    </template>
                    <template v-else-if="selectedBlock.blockType === 'AiTable'">
                      <n-form-item label="行主键 / 尺寸 / 模式">
                        <div class="style-grid three">
                          <n-input
                            :value="selectedBlock.props?.rowKey || 'id'"
                            placeholder="id"
                            @update:value="patchBlockProps(selectedBlock.id, { rowKey: $event || 'id' })"
                          />
                          <n-select
                            :value="selectedBlock.props?.size || 'small'"
                            :options="componentSizeOptions"
                            @update:value="patchBlockProps(selectedBlock.id, { size: $event || 'small' })"
                          />
                          <n-select
                            :value="selectedBlock.props?.renderMode || 'table'"
                            :options="renderModeOptions"
                            @update:value="patchBlockProps(selectedBlock.id, { renderMode: $event || 'table' })"
                          />
                        </div>
                      </n-form-item>
                    </template>
                    <template v-else>
                      <n-form-item label="表单布局">
                        <div class="style-grid three">
                          <n-input-number
                            :value="selectedBlock.props?.gridCols || 2"
                            :min="1"
                            :max="4"
                            :show-button="false"
                            @update:value="patchBlockProps(selectedBlock.id, { gridCols: $event || 2 })"
                          />
                          <n-select
                            :value="selectedBlock.props?.labelPlacement || 'left'"
                            :options="labelPlacementOptions"
                            @update:value="patchBlockProps(selectedBlock.id, { labelPlacement: $event || 'left' })"
                          />
                          <n-select
                            :value="selectedBlock.props?.labelAlign || 'right'"
                            :options="labelAlignOptions"
                            @update:value="patchBlockProps(selectedBlock.id, { labelAlign: $event || 'right' })"
                          />
                        </div>
                      </n-form-item>
                      <n-form-item label="标签宽度 / 尺寸">
                        <div class="style-grid three">
                          <n-input-number
                            :value="toNumberOrNull(selectedBlock.props?.labelWidth) ?? 100"
                            :min="0"
                            :max="260"
                            :show-button="false"
                            @update:value="patchBlockProps(selectedBlock.id, { labelWidth: $event ?? 100 })"
                          />
                          <n-select
                            :value="selectedBlock.props?.size || 'medium'"
                            :options="componentSizeOptions"
                            @update:value="patchBlockProps(selectedBlock.id, { size: $event || 'medium' })"
                          />
                          <n-input-number
                            :value="selectedBlock.props?.maxVisibleFields || 6"
                            :min="1"
                            :max="40"
                            :show-button="false"
                            @update:value="patchBlockProps(selectedBlock.id, { maxVisibleFields: $event || 6 })"
                          />
                        </div>
                      </n-form-item>
                    </template>
                  </n-collapse-item>

                  <n-collapse-item v-if="selectedBlock.blockType === 'AiCrudPage' && propertySectionVisible(['查询与列表字段', '搜索与表格', '搜索', '查询区', '搜索布局', '搜索字段', '表格', '列表字段', '表格列', '列标题', '列宽', '对齐', '固定', '省略', '排序', '分页', '列数', '最大高度', '横向宽度', '边框', '斑马纹', '行按钮', '操作列', '列表行自定义按钮'])" name="search" title="查询与列表字段">
                    <div class="property-search-anchor" data-property-search="查询与列表字段 搜索与表格 搜索 查询区 搜索布局 搜索字段 表格 列表字段 表格列 列标题 列宽 对齐 固定 省略 排序 分页 列数 最大高度 横向宽度 边框 斑马纹 行按钮 操作列 列表行自定义按钮" />
                    <n-form-item label="显示项">
                      <n-checkbox-group
                        :value="resolveAiCrudVisibleFlags(selectedBlock)"
                        @update:value="updateAiCrudVisibleFlags"
                      >
                        <n-space>
                          <n-checkbox value="showSearch" label="查询区" />
                          <n-checkbox value="showPagination" label="分页" />
                          <n-checkbox value="showToolbar" label="工具栏" />
                          <n-checkbox value="showRenderModeSwitch" label="模式切换" />
                        </n-space>
                      </n-checkbox-group>
                    </n-form-item>
                    <n-form-item label="搜索布局">
                      <div class="search-layout-grid">
                        <label class="mini-number-field">
                          <span>每行字段</span>
                          <n-input-number
                            :value="selectedBlock.props?.searchGridCols || 4"
                            :min="1"
                            :max="6"
                            :show-button="false"
                            placeholder="4"
                            @update:value="patchBlockProps(selectedBlock.id, { searchGridCols: $event || 4 })"
                          />
                        </label>
                        <label class="mini-number-field">
                          <span>标签宽</span>
                          <n-input-number
                            :value="toNumberOrNull(selectedBlock.props?.searchLabelWidth)"
                            :min="0"
                            :max="260"
                            :show-button="false"
                            placeholder="自动"
                            @update:value="patchBlockProps(selectedBlock.id, { searchLabelWidth: $event ?? 'auto' })"
                          />
                          <em>px</em>
                        </label>
                        <label class="mini-number-field">
                          <span>默认显示</span>
                          <n-input-number
                            :value="selectedBlock.props?.searchMaxVisibleFields || 3"
                            :min="1"
                            :max="30"
                            :show-button="false"
                            placeholder="3"
                            @update:value="patchBlockProps(selectedBlock.id, { searchMaxVisibleFields: $event || 3 })"
                          />
                          <em>项</em>
                        </label>
                        <label class="mini-number-field">
                          <span>行间距</span>
                          <n-input-number
                            :value="selectedBlock.props?.searchYGap ?? 16"
                            :min="0"
                            :max="48"
                            :show-button="false"
                            placeholder="16"
                            @update:value="patchBlockProps(selectedBlock.id, { searchYGap: $event ?? 16 })"
                          />
                          <em>px</em>
                        </label>
                      </div>
                    </n-form-item>
                    <n-form-item label="字段与操作">
                      <div class="bitable-field-panel">
                        <div class="bitable-field-panel-head">
                          <strong>字段</strong>
                          <div class="bitable-field-panel-actions">
                            <button type="button" @click="openFieldDrawer('search')">
                              查询字段 {{ resolveBlockFieldCount(selectedBlock, 'search') }}
                            </button>
                            <button type="button" @click="openFieldDrawer('table')">
                              列表字段 {{ resolveBlockFieldCount(selectedBlock, 'table') }}
                            </button>
                          </div>
                        </div>
                        <div class="bitable-field-panel-list">
                          <draggable
                            :model-value="crudTablePanelFields"
                            item-key="field"
                            handle=".bitable-field-drag"
                            :animation="160"
                            @update:model-value="handleCrudTableFieldReorder"
                          >
                            <template #item="{ element }">
                              <div class="bitable-field-row" :class="{ invisible: !isCrudTableFieldVisible(element.field) }">
                                <button type="button" class="bitable-field-drag" title="拖拽排序">
                                  <n-icon><BitableDragIcon /></n-icon>
                                </button>
                                <n-icon class="bitable-field-icon">
                                  <component :is="resolveCrudFieldIconComponent(element)" />
                                </n-icon>
                                <div class="bitable-field-name">
                                  <span>{{ resolveCrudFieldInlineTitle(element) }}</span>
                                  <small>{{ element.field }}</small>
                                </div>
                                <button
                                  type="button"
                                  class="bitable-field-icon-button"
                                  :title="isCrudTableFieldVisible(element.field) ? '隐藏字段' : '显示字段'"
                                  @click="toggleCrudTableField(element.field, !isCrudTableFieldVisible(element.field))"
                                >
                                  <n-icon>
                                    <component :is="isCrudTableFieldVisible(element.field) ? EyeOutline : BitableInvisibleIcon" />
                                  </n-icon>
                                </button>
                                <button type="button" class="bitable-field-icon-button" title="配置字段" @click="openInlineFieldDrawer(element.field, 'table')">
                                  <n-icon>
                                    <EllipsisHorizontalOutline />
                                  </n-icon>
                                </button>
                              </div>
                            </template>
                          </draggable>
                          <span v-if="!crudTablePanelFields.length" class="custom-action-empty">暂无可配置字段</span>
                        </div>
                        <div class="bitable-field-operation-divider" />
                        <div class="bitable-field-operation-group">
                          <div class="bitable-field-operation-head">
                            <div class="bitable-field-operation-title">
                              <span class="bitable-field-drag disabled">
                                <n-icon>
                                  <BitableDragIcon />
                                </n-icon>
                              </span>
                              <n-icon class="bitable-field-icon">
                                <BitableButtonIcon />
                              </n-icon>
                              <strong>操作</strong>
                              <n-popover trigger="click" placement="top" :show-arrow="false" class="bitable-field-info-popover">
                                <template #trigger>
                                  <button type="button" class="bitable-field-tip-button" title="操作说明">
                                    <n-icon class="bitable-field-tip-icon">
                                      <BitableInfoIcon />
                                    </n-icon>
                                  </button>
                                </template>
                                <div class="bitable-field-info-content">
                                  行按钮会显示在表格操作列；隐藏后仍保留配置，可随时重新显示。
                                </div>
                              </n-popover>
                            </div>
                            <button type="button" class="bitable-field-add-button" @click="addCustomAction('row')">
                              <n-icon><AddOutline /></n-icon>
                              <span>添加</span>
                            </button>
                          </div>
                        </div>
                        <div class="bitable-operation-list">
                          <draggable
                            :model-value="rowCustomActions"
                            item-key="clientKey"
                            handle=".custom-action-drag"
                            :animation="160"
                            @update:model-value="rows => handleCustomActionGroupReorder('row', rows)"
                          >
                            <template #item="{ element }">
                              <div class="bitable-field-row bitable-action-row" :class="{ invisible: element.visible === false }">
                                <button type="button" class="custom-action-drag bitable-field-drag" title="拖拽排序">
                                  <n-icon>
                                    <BitableDragIcon />
                                  </n-icon>
                                </button>
                                <n-icon class="bitable-field-icon">
                                  <BitableButtonIcon />
                                </n-icon>
                                <div class="bitable-field-name">
                                  <span>{{ element.label || '自定义按钮' }}</span>
                                  <small>{{ resolveActionBehaviorLabel(element) }}</small>
                                </div>
                                <button type="button" class="bitable-field-icon-button" :title="element.visible === false ? '显示按钮' : '隐藏按钮'" @click="toggleCustomActionVisible(element)">
                                  <n-icon>
                                    <component :is="element.visible === false ? BitableInvisibleIcon : BitableVisibleIcon" />
                                  </n-icon>
                                </button>
                                <n-dropdown
                                  trigger="click"
                                  :options="customActionMoreOptions(element)"
                                  @select="key => handleCustomActionMoreSelect(key, element)"
                                >
                                  <button type="button" class="bitable-field-icon-button" title="配置按钮">
                                    <n-icon>
                                      <BitableAdminIcon />
                                    </n-icon>
                                  </button>
                                </n-dropdown>
                              </div>
                            </template>
                          </draggable>
                          <span v-if="!rowCustomActions.length" class="custom-action-empty">暂无行自定义按钮</span>
                        </div>
                      </div>
                    </n-form-item>
                    <n-form-item label="表格展示">
                      <div class="style-grid four">
                        <n-select
                          :value="selectedBlock.props?.tableSize || 'medium'"
                          :options="tableDensityOptions"
                          @update:value="patchBlockProps(selectedBlock.id, { tableSize: $event || 'medium' })"
                        />
                        <n-select
                          :value="selectedBlock.props?.renderMode || 'table'"
                          :options="renderModeOptions"
                          @update:value="patchBlockProps(selectedBlock.id, { renderMode: $event || 'table' })"
                        />
                        <n-input-number
                          :value="toNumberOrNull(selectedBlock.props?.maxHeight)"
                          :min="0"
                          :show-button="false"
                          placeholder="最大高度"
                          @update:value="patchBlockProps(selectedBlock.id, { maxHeight: $event || undefined })"
                        />
                        <n-input-number
                          :value="toNumberOrNull(selectedBlock.props?.scrollX)"
                          :min="0"
                          :show-button="false"
                          placeholder="横向宽度"
                          @update:value="patchBlockProps(selectedBlock.id, { scrollX: $event || undefined })"
                        />
                      </div>
                    </n-form-item>
                    <n-form-item label="表格样式">
                      <n-checkbox-group
                        :value="resolveAiCrudTableFlags(selectedBlock)"
                        @update:value="updateAiCrudTableFlags"
                      >
                        <n-space>
                          <n-checkbox value="bordered" label="边框" />
                          <n-checkbox value="striped" label="斑马纹" />
                          <n-checkbox value="hideSelection" label="隐藏多选" />
                          <n-checkbox value="resizable" label="列宽拖拽" />
                          <n-checkbox value="searchEnableCollapse" label="搜索折叠" />
                        </n-space>
                      </n-checkbox-group>
                    </n-form-item>
                    <n-form-item label="展开面板">
                      <div class="crud-expand-config-panel">
                        <div class="crud-expand-config-head">
                          <n-switch
                            :value="selectedBlock.props?.expandConfig?.enabled === true"
                            size="small"
                            @update:value="checked => updateAiCrudExpandEnabled(checked)"
                          />
                          <span>{{ selectedBlock.props?.expandConfig?.enabled === true ? '已启用行展开' : '未启用行展开' }}</span>
                        </div>
                        <template v-if="selectedBlock.props?.expandConfig?.enabled === true">
                          <div class="crud-expand-config-section">
                            <div class="crud-expand-section-title">
                              基础设置
                            </div>
                            <div class="crud-expand-config-grid">
                              <label class="crud-expand-config-field">
                                <span>
                                  触发方式
                                </span>
                                <n-select
                                  :value="selectedBlock.props?.expandConfig?.trigger || 'icon'"
                                  :options="expandTriggerOptions"
                                  @update:value="value => updateAiCrudExpandConfig({ trigger: value || 'icon' })"
                                />
                              </label>
                              <label class="crud-expand-config-field">
                                <span>
                                  展示方式
                                </span>
                                <n-select
                                  :value="selectedBlock.props?.expandConfig?.layout?.mode || 'single'"
                                  :options="expandLayoutModeOptions"
                                  @update:value="value => updateAiCrudExpandConfig({ layout: { ...(selectedBlock.props?.expandConfig?.layout || {}), mode: value || 'single' } })"
                                />
                              </label>
                            </div>
                          </div>
                          <div class="crud-expand-config-section">
                            <div class="crud-expand-section-title">
                              内容设置
                            </div>
                            <div class="crud-expand-config-grid">
                              <label class="crud-expand-config-field">
                                <span>
                                  面板类型
                                </span>
                                <n-select
                                  :value="firstExpandPanel(selectedBlock)?.type || 'descriptions'"
                                  :options="expandPanelTypeOptions"
                                  @update:value="value => updateFirstAiCrudExpandPanel({ type: value || 'descriptions' })"
                                />
                              </label>
                              <label class="crud-expand-config-field">
                                <span>
                                  面板标题
                                </span>
                                <n-input
                                  :value="firstExpandPanel(selectedBlock)?.title || ''"
                                  clearable
                                  placeholder="概览 / 明细"
                                  @update:value="value => updateFirstAiCrudExpandPanel({ title: value || undefined })"
                                />
                              </label>
                              <label class="crud-expand-config-field">
                                <span>
                                  数据来源
                                </span>
                                <n-select
                                  :value="firstExpandPanel(selectedBlock)?.dataSource?.type || 'row'"
                                  :options="expandDataSourceTypeOptions"
                                  @update:value="value => updateFirstAiCrudExpandPanel({ dataSource: { ...(firstExpandPanel(selectedBlock)?.dataSource || {}), type: value || 'row' } })"
                                />
                              </label>
                            </div>
                          </div>
                          <div
                            v-if="['api', 'quantity'].includes(firstExpandPanel(selectedBlock)?.dataSource?.type)"
                            class="crud-expand-config-section"
                          >
                            <div class="crud-expand-section-title">
                              {{ firstExpandPanel(selectedBlock)?.dataSource?.type === 'quantity' ? '数量查询参数' : '接口参数' }}
                            </div>
                            <label v-if="firstExpandPanel(selectedBlock)?.dataSource?.type === 'api'" class="crud-expand-config-field">
                              <span>
                                接口地址
                              </span>
                              <n-input
                                :value="firstExpandPanel(selectedBlock)?.dataSource?.api || ''"
                                clearable
                                placeholder="子表接口，例如 get@/api/order/item/page"
                                @update:value="value => updateFirstAiCrudExpandDataSource({ api: value || '' })"
                              />
                            </label>
                            <label class="crud-expand-config-field">
                              <span>
                                参数映射
                              </span>
                              <n-input
                                :value="stringifyJsonProp(firstExpandPanel(selectedBlock)?.dataSource?.paramsMap || {})"
                                type="textarea"
                                :autosize="{ minRows: 2, maxRows: 4 }"
                                placeholder="例如 {&quot;sourceRecordId&quot;:&quot;${row.id}&quot;}"
                                @update:value="value => updateFirstAiCrudExpandParamsMap(value)"
                              />
                            </label>
                          </div>
                          <div
                            v-if="firstExpandPanel(selectedBlock)?.type === 'descriptions'"
                            class="crud-expand-config-section"
                          >
                            <div class="crud-expand-section-title">
                              描述字段
                            </div>
                            <div class="bitable-config-summary-row">
                              <div class="bitable-config-summary-label">
                                展示字段
                              </div>
                              <div class="bitable-config-summary-value">
                                <div class="bitable-config-value-box">
                                  <div class="bitable-config-value-box-text">
                                    {{ resolveExpandDescriptionSelectedFields(firstExpandPanel(selectedBlock), selectedBlock).length }} 个字段
                                  </div>
                                  <div class="bitable-config-value-box-btn">
                                    <n-popover
                                      v-model:show="expandDescriptionFieldPanelOpen"
                                      trigger="click"
                                      placement="bottom-end"
                                      :show-arrow="false"
                                      raw
                                    >
                                      <template #trigger>
                                        <button type="button" class="bitable-config-icon-button" title="设置展示字段">
                                          <n-icon><SettingsOutline /></n-icon>
                                        </button>
                                      </template>
                                      <div class="bitable-field-popover-panel">
                                        <div class="bitable-field-panel-arrow" />
                                        <div class="bitable-field-popover-head">
                                          展示字段
                                        </div>
                                        <div class="bitable-field-panel-list">
                                          <draggable
                                            :model-value="resolveExpandDescriptionPanelFields(firstExpandPanel(selectedBlock), selectedBlock)"
                                            item-key="field"
                                            handle=".crud-expand-field-drag"
                                            :animation="160"
                                            @update:model-value="handleFirstAiCrudDescriptionPanelReorder"
                                          >
                                            <template #item="{ element }">
                                              <div class="bitable-field-row" :class="{ invisible: !element.selected }">
                                                <button type="button" class="bitable-field-drag crud-expand-field-drag" title="拖拽排序">
                                                  <n-icon><BitableDragIcon /></n-icon>
                                                </button>
                                                <n-icon class="bitable-field-icon">
                                                  <component :is="resolveCrudFieldIconComponent(element)" />
                                                </n-icon>
                                                <div class="bitable-field-name">
                                                  <span>{{ element.label || element.field }}</span>
                                                  <small>{{ element.field }}</small>
                                                </div>
                                                <button
                                                  type="button"
                                                  class="bitable-field-icon-button"
                                                  :title="element.selected ? '隐藏字段' : '显示字段'"
                                                  @click="toggleFirstAiCrudDescriptionField(element.field, !element.selected)"
                                                >
                                                  <n-icon>
                                                    <component :is="element.selected ? BitableVisibleIcon : BitableInvisibleIcon" />
                                                  </n-icon>
                                                </button>
                                              </div>
                                            </template>
                                          </draggable>
                                          <span v-if="!resolveExpandDescriptionFieldOptions(selectedBlock).length" class="custom-action-empty">暂无可选字段</span>
                                        </div>
                                      </div>
                                    </n-popover>
                                  </div>
                                </div>
                              </div>
                            </div>
                            <n-empty
                              v-if="!resolveExpandDescriptionFieldOptions(selectedBlock).length"
                              size="small"
                              description="暂无可选字段"
                            />
                          </div>
                        </template>
                      </div>
                    </n-form-item>
                  </n-collapse-item>

                  <n-collapse-item v-if="selectedBlock.blockType === 'AiCrudPage' && propertySectionVisible(['表单与弹窗', '新增', '编辑', '表单', '弹窗', '抽屉', '详情', '页脚', '打开方式', '标签位置', '标签对齐', '标签宽度', '表单列数'])" name="edit" title="表单与弹窗">
                    <div class="property-search-anchor" data-property-search="表单与弹窗 新增 编辑 表单 弹窗 抽屉 详情 页脚 打开方式 标签位置 标签对齐 标签宽度 表单列数" />
                    <div class="form-modal-help">
                      弹窗方式和表单布局已统一在「表单设计」的「表单属性 → 表单项配置」中管理。
                    </div>
                    <div class="form-modal-readonly-summary">
                      <div class="readonly-summary-row">
                        <span>打开方式</span>
                        <strong>{{ selectedAiCrudFormModalProps.formOpenMode || selectedAiCrudFormModalProps.modalType || 'modal' }}</strong>
                      </div>
                      <div class="readonly-summary-row">
                        <span>弹窗宽度</span>
                        <strong>{{ selectedAiCrudFormModalProps.modalWidth || '800px' }}</strong>
                      </div>
                      <div class="readonly-summary-row">
                        <span>编辑列数</span>
                        <strong>{{ selectedAiCrudFormModalProps.editGridCols || 1 }}</strong>
                      </div>
                    </div>
                    <n-form-item label="详情与页脚">
                      <div class="metrics-editor">
                        <n-checkbox-group
                          :value="resolveAiCrudEditFlags(selectedBlock)"
                          @update:value="updateAiCrudEditFlags"
                        >
                          <n-space>
                            <n-checkbox value="editShowFeedback" label="校验反馈" />
                            <n-checkbox value="hideModalFooter" label="隐藏页脚" />
                            <n-checkbox value="hideDefaultDetailContent" label="隐藏默认详情" />
                          </n-space>
                        </n-checkbox-group>
                      </div>
                    </n-form-item>
                  </n-collapse-item>

                  <n-collapse-item v-if="selectedBlock.blockType === 'AiCrudPage' && propertySectionVisible(['工具栏与导入导出', '工具栏', '导入', '导出', '导出任务', '自定义查询', '自定义操作', '按钮文案', '回调', '参数处理', '提交前', '搜索前', '加载列表前'])" name="toolbar" title="工具栏按钮与导入导出">
                    <div class="property-search-anchor" data-property-search="工具栏与导入导出 工具栏 导入 导出 导出任务 自定义查询 自定义操作 按钮文案 回调 参数处理 提交前 搜索前 加载列表前" />
                    <n-form-item label="工具栏显示项">
                      <div class="toolbar-toggle-list">
                        <div class="switch-line toolbar-toggle-row">
                          <span class="switch-line-text">
                            <strong>新增按钮</strong>
                            <small>控制顶部工具栏里的“新增”按钮。</small>
                          </span>
                          <n-switch
                            :value="isAiCrudToolbarSwitchOn(selectedBlock, 'add')"
                            @update:value="updateAiCrudToolbarSwitch('add', $event)"
                          />
                        </div>
                        <div class="switch-line toolbar-toggle-row">
                          <span class="switch-line-text">
                            <strong>批量删除按钮</strong>
                            <small>控制勾选多行后使用的“批量删除”。</small>
                          </span>
                          <n-switch
                            :value="isAiCrudToolbarSwitchOn(selectedBlock, 'batchDelete')"
                            @update:value="updateAiCrudToolbarSwitch('batchDelete', $event)"
                          />
                        </div>
                        <div class="switch-line toolbar-toggle-row">
                          <span class="switch-line-text">
                            <strong>导入按钮</strong>
                            <small>控制顶部“导入”，需要下方导入接口地址可用。</small>
                          </span>
                          <n-switch
                            :value="isAiCrudToolbarSwitchOn(selectedBlock, 'import')"
                            @update:value="updateAiCrudToolbarSwitch('import', $event)"
                          />
                        </div>
                        <div class="switch-line toolbar-toggle-row">
                          <span class="switch-line-text">
                            <strong>导出按钮</strong>
                            <small>控制顶部“导出”，需要下方导出接口地址可用。</small>
                          </span>
                          <n-switch
                            :value="isAiCrudToolbarSwitchOn(selectedBlock, 'export')"
                            @update:value="updateAiCrudToolbarSwitch('export', $event)"
                          />
                        </div>
                        <div class="switch-line toolbar-toggle-row">
                          <span class="switch-line-text">
                            <strong>自定义查询</strong>
                            <small>控制工具栏里的自定义查询入口。</small>
                          </span>
                          <n-switch
                            :value="isAiCrudToolbarSwitchOn(selectedBlock, 'customQuery')"
                            @update:value="updateAiCrudToolbarSwitch('customQuery', $event)"
                          />
                        </div>
                        <div class="switch-line toolbar-toggle-row">
                          <span class="switch-line-text">
                            <strong>导出任务入口</strong>
                            <small>控制异步导出任务入口；未配置任务时预览不会显示。</small>
                          </span>
                          <n-switch
                            :value="isAiCrudToolbarSwitchOn(selectedBlock, 'exportTasks')"
                            @update:value="updateAiCrudToolbarSwitch('exportTasks', $event)"
                          />
                        </div>
                        <div class="field-help">
                          这些开关只控制当前 AiCrudPage 顶部工具栏的按钮。若“搜索与表格”里的“工具栏”关闭，整排工具按钮都会隐藏。
                        </div>
                      </div>
                    </n-form-item>
                    <n-form-item label="按钮文案 / 导出文件名">
                      <div class="style-grid three">
                        <n-input
                          :value="selectedBlock.props?.addButtonText || '新增'"
                          placeholder="新增按钮"
                          @update:value="patchBlockProps(selectedBlock.id, { addButtonText: $event || '新增' })"
                        />
                        <n-input
                          :value="selectedBlock.props?.exportButtonText || '导出'"
                          placeholder="导出按钮"
                          @update:value="patchBlockProps(selectedBlock.id, { exportButtonText: $event || '导出' })"
                        />
                        <n-input
                          :value="selectedBlock.props?.exportFileName"
                          placeholder="导出文件名"
                          @update:value="patchBlockProps(selectedBlock.id, { exportFileName: $event })"
                        />
                      </div>
                    </n-form-item>
                    <n-form-item label="工具栏按钮">
                      <div class="custom-action-builder">
                        <div class="custom-action-builder-section">
                          <div class="custom-action-builder-head">
                            <div>
                              <strong>工具栏自定义按钮</strong>
                              <span>显示在列表顶部工具栏。</span>
                            </div>
                            <n-button size="tiny" secondary @click="addCustomAction('toolbar')">
                              <template #icon>
                                <n-icon><AddOutline /></n-icon>
                              </template>
                              添加
                            </n-button>
                          </div>
                          <draggable
                            :model-value="toolbarCustomActions"
                            item-key="clientKey"
                            handle=".custom-action-drag"
                            :animation="160"
                            class="custom-action-card-list"
                            @update:model-value="rows => handleCustomActionGroupReorder('toolbar', rows)"
                          >
                            <template #item="{ element }">
                              <div class="custom-action-card-row">
                                <button type="button" class="custom-action-drag" title="拖拽排序">
                                  <n-icon><ReorderThreeOutline /></n-icon>
                                </button>
                                <n-input
                                  class="custom-action-name-input"
                                  size="small"
                                  :value="element.label || '自定义按钮'"
                                  placeholder="按钮文字"
                                  @update:value="value => updateCustomActionByIdentity(element, { label: value || '自定义按钮' })"
                                >
                                  <template #suffix>
                                    <button type="button" class="custom-action-inline-icon" title="设置" @click.stop="openCustomActionEditor(element)">
                                      <n-icon><SettingsOutline /></n-icon>
                                    </button>
                                  </template>
                                </n-input>
                                <n-dropdown
                                  trigger="click"
                                  :options="customActionMoreOptions(element)"
                                  @select="key => handleCustomActionMoreSelect(key, element)"
                                >
                                  <button type="button" class="custom-action-more" title="更多">
                                    <n-icon><EllipsisHorizontalOutline /></n-icon>
                                  </button>
                                </n-dropdown>
                              </div>
                            </template>
                          </draggable>
                          <span v-if="!toolbarCustomActions.length" class="custom-action-empty">暂无工具栏自定义按钮</span>
                        </div>
                      </div>
                    </n-form-item>
                    <n-form-item label="树表操作">
                      <div class="tree-action-compact">
                        <div>
                          <strong>添加下级</strong>
                          <span>树形表行操作</span>
                        </div>
                        <n-switch
                          size="small"
                          :value="selectedBlock.props?.enableTreeAddChild === true"
                          @update:value="patchBlockProps(selectedBlock.id, { enableTreeAddChild: $event })"
                        />
                      </div>
                    </n-form-item>
                  </n-collapse-item>

                  <n-collapse-item v-if="selectedBlock.blockType === 'AiTable' && propertySectionVisible(['表格功能', '工具栏', '分页', '刷新', '密度', '列设置', '搜索切换', '全屏', '滚动尺寸'])" name="table" title="表格功能">
                    <div class="property-search-anchor" data-property-search="表格功能 工具栏 分页 刷新 密度 列设置 搜索切换 全屏 滚动尺寸 表格样式" />
                    <n-form-item label="功能开关">
                      <n-checkbox-group
                        :value="resolveAiTableVisibleFlags(selectedBlock)"
                        @update:value="updateAiTableVisibleFlags"
                      >
                        <n-space>
                          <n-checkbox value="showToolbar" label="工具栏" />
                          <n-checkbox value="showPagination" label="分页" />
                          <n-checkbox value="showSelection" label="选择列" />
                          <n-checkbox value="showRefresh" label="刷新" />
                          <n-checkbox value="showDensity" label="密度" />
                          <n-checkbox value="showColumnFilter" label="列设置" />
                          <n-checkbox value="showSearchToggle" label="搜索切换" />
                          <n-checkbox value="showFullscreen" label="全屏" />
                          <n-checkbox value="showRenderModeSwitch" label="模式切换" />
                        </n-space>
                      </n-checkbox-group>
                    </n-form-item>
                    <n-form-item label="表格样式">
                      <n-checkbox-group
                        :value="resolveAiTableStyleFlags(selectedBlock)"
                        @update:value="updateAiTableStyleFlags"
                      >
                        <n-space>
                          <n-checkbox value="bordered" label="边框" />
                          <n-checkbox value="striped" label="斑马纹" />
                          <n-checkbox value="singleLine" label="单线" />
                        </n-space>
                      </n-checkbox-group>
                    </n-form-item>
                    <n-form-item label="滚动尺寸">
                      <div class="style-grid">
                        <n-input-number
                          :value="toNumberOrNull(selectedBlock.props?.maxHeight)"
                          :min="0"
                          :show-button="false"
                          placeholder="最大高度 px"
                          @update:value="patchBlockProps(selectedBlock.id, { maxHeight: $event || undefined })"
                        />
                        <n-input-number
                          :value="toNumberOrNull(selectedBlock.props?.scrollX)"
                          :min="0"
                          :show-button="false"
                          placeholder="横向宽度 px"
                          @update:value="patchBlockProps(selectedBlock.id, { scrollX: $event || undefined })"
                        />
                      </div>
                    </n-form-item>
                  </n-collapse-item>

                  <n-collapse-item v-if="selectedBlock.blockType === 'AiForm' && propertySectionVisible(['按钮与折叠', '按钮', '提交', '重置', '取消', '折叠', '校验'])" name="form-actions" title="按钮与折叠">
                    <div class="property-search-anchor" data-property-search="按钮与折叠 按钮 提交 重置 取消 折叠 校验 操作区 字段折叠 校验反馈 按钮文案" />
                    <n-form-item label="间距">
                      <div class="style-grid">
                        <n-input-number
                          :value="selectedBlock.props?.xGap ?? 12"
                          :min="0"
                          :max="48"
                          :show-button="false"
                          placeholder="列距"
                          @update:value="patchBlockProps(selectedBlock.id, { xGap: $event ?? 12 })"
                        />
                        <n-input-number
                          :value="selectedBlock.props?.yGap ?? 0"
                          :min="0"
                          :max="48"
                          :show-button="false"
                          placeholder="行距"
                          @update:value="patchBlockProps(selectedBlock.id, { yGap: $event ?? 0 })"
                        />
                      </div>
                    </n-form-item>
                    <n-form-item label="显示项">
                      <n-checkbox-group
                        :value="resolveAiFormFlags(selectedBlock)"
                        @update:value="updateAiFormFlags"
                      >
                        <n-space>
                          <n-checkbox value="showActions" label="操作区" />
                          <n-checkbox value="showSubmit" label="提交" />
                          <n-checkbox value="showReset" label="重置" />
                          <n-checkbox value="showCancel" label="取消" />
                          <n-checkbox value="enableCollapse" label="字段折叠" />
                          <n-checkbox value="showFeedback" label="校验反馈" />
                        </n-space>
                      </n-checkbox-group>
                    </n-form-item>
                    <n-form-item label="按钮文案">
                      <div class="style-grid three">
                        <n-input
                          :value="selectedBlock.props?.submitText || '提交'"
                          @update:value="patchBlockProps(selectedBlock.id, { submitText: $event || '提交' })"
                        />
                        <n-input
                          :value="selectedBlock.props?.resetText || '重置'"
                          @update:value="patchBlockProps(selectedBlock.id, { resetText: $event || '重置' })"
                        />
                        <n-input
                          :value="selectedBlock.props?.cancelText || '取消'"
                          @update:value="patchBlockProps(selectedBlock.id, { cancelText: $event || '取消' })"
                        />
                      </div>
                    </n-form-item>
                  </n-collapse-item>

                  <n-collapse-item v-if="selectedBlock.blockType === 'AiCrudPage' && propertySectionVisible(['默认参数', '公共参数', 'publicParams', 'publicQuery', '表单默认值', '提交固定参数', '查询默认参数'])" name="default-params" title="默认参数与数据处理">
                    <div class="property-search-anchor" data-property-search="默认参数 公共参数 publicParams publicQuery 表单默认值 提交固定参数 查询默认参数 数据处理" />
                    <CrudDefaultParamsEditor
                      :model-value="resolveSelectedBlockDefaultParams(selectedBlock)"
                      :field-options="sortFieldOptions"
                      @update:model-value="updateSelectedBlockDefaultParams"
                    />
                  </n-collapse-item>

                  <n-collapse-item v-if="propertySectionVisible(['事件回调', '事件', '回调', '点击', '加载完成', '提交成功', '参数处理', '提交前', '搜索前', '加载列表前'])" name="event-help" title="生命周期回调">
                    <div class="property-search-anchor" data-property-search="生命周期回调 事件回调 事件 回调 点击 加载完成 提交成功 参数处理 提交前 搜索前 加载列表前" />
                    <template v-if="selectedBlock.blockType === 'AiCrudPage'">
                      <CrudHookRulesEditor
                        :model-value="selectedBlock.props?.crudHookRules || {}"
                        :legacy-before-submit-rules="selectedBlock.props?.beforeSubmitRules || []"
                        :field-options="sortFieldOptions"
                        @update:model-value="updateSelectedBlockHookRules"
                      />
                    </template>
                    <div v-else class="event-help">
                      组件交互统一在上方“事件配置”里维护，可以配置点击、加载完成、行点击、提交成功等触发时机，并选择跳转、刷新、打开弹窗、接口请求等动作。
                    </div>
                  </n-collapse-item>
                </n-collapse>
              </template>

              <template v-if="selectedBlock.blockType === 'back-button'">
                <n-form-item label="按钮文字">
                  <n-input
                    :value="selectedBlock.props?.text"
                    @update:value="patchBlockProps(selectedBlock.id, { text: $event })"
                  />
                </n-form-item>
                <n-form-item label="按钮样式 / 动作">
                  <div class="style-grid">
                    <n-select
                      :value="selectedBlock.props?.type || 'default'"
                      :options="actionTypeOptions"
                      @update:value="patchBlockProps(selectedBlock.id, { type: $event || 'default' })"
                    />
                    <n-select
                      :value="selectedBlock.props?.action || 'back'"
                      :options="backButtonActionOptions"
                      @update:value="patchBlockProps(selectedBlock.id, { action: $event || 'back' })"
                    />
                  </div>
                </n-form-item>
                <n-form-item v-if="selectedBlock.props?.action === 'navigate'" label="返回目标页面">
                  <div class="style-grid">
                    <n-select
                      :value="selectedBlock.props?.targetPageKey"
                      :options="pageTargetOptions"
                      clearable
                      placeholder="目标页面"
                      @update:value="patchBlockProps(selectedBlock.id, { targetPageKey: $event || '' })"
                    />
                    <n-select
                      v-if="formTargetOptions.length"
                      :value="selectedBlock.props?.targetFormKey"
                      :options="formTargetOptions"
                      clearable
                      placeholder="目标表单"
                      @update:value="patchBlockProps(selectedBlock.id, { targetFormKey: $event || '' })"
                    />
                  </div>
                </n-form-item>
              </template>

              <template v-if="selectedBlock.blockType === 'page-title'">
                <n-form-item label="富文本内容">
                  <n-input
                    :value="selectedBlock.props?.content || ''"
                    type="textarea"
                    :rows="5"
                    placeholder="在页面中直接编辑内容；这里可查看或粘贴 HTML"
                    @update:value="patchBlockProps(selectedBlock.id, { content: $event })"
                  />
                </n-form-item>
                <n-form-item label="状态 / 尺寸">
                  <div class="style-grid three">
                    <n-input
                      :value="selectedBlock.props?.statusText"
                      placeholder="状态文本"
                      @update:value="patchBlockProps(selectedBlock.id, { statusText: $event })"
                    />
                    <n-select
                      :value="selectedBlock.props?.statusType || 'info'"
                      :options="tagTypeOptions"
                      @update:value="patchBlockProps(selectedBlock.id, { statusType: $event || 'info' })"
                    />
                    <n-select
                      :value="selectedBlock.props?.size || 'medium'"
                      :options="componentSizeOptions"
                      @update:value="patchBlockProps(selectedBlock.id, { size: $event || 'medium' })"
                    />
                  </div>
                </n-form-item>
              </template>

              <!-- Toolbar 动作 -->
              <template v-if="selectedBlock.blockType === 'toolbar'">
                <n-form-item label="按钮">
                  <n-checkbox-group
                    :value="selectedBlock.props?.actions || []"
                    @update:value="patchBlockProps(selectedBlock.id, { actions: $event })"
                  >
                    <n-space vertical size="small">
                      <n-checkbox value="add" label="新增" />
                      <n-checkbox value="import" label="导入" />
                      <n-checkbox value="export" label="导出" />
                      <n-checkbox value="batch-delete" label="批量删除" />
                      <n-checkbox value="custom-query" label="自定义查询" />
                    </n-space>
                  </n-checkbox-group>
                </n-form-item>
                <n-divider>自定义操作按钮</n-divider>
                <div class="custom-action-builder">
                  <div class="custom-action-builder-section">
                    <div class="custom-action-builder-head">
                      <div>
                        <strong>工具栏自定义按钮</strong>
                        <span>显示在当前工具栏区块。</span>
                      </div>
                      <n-button size="tiny" secondary @click="addCustomAction('toolbar')">
                        <template #icon>
                          <n-icon><AddOutline /></n-icon>
                        </template>
                        添加
                      </n-button>
                    </div>
                    <draggable
                      :model-value="toolbarCustomActions"
                      item-key="clientKey"
                      handle=".custom-action-drag"
                      :animation="160"
                      class="custom-action-card-list"
                      @update:model-value="rows => handleCustomActionGroupReorder('toolbar', rows)"
                    >
                      <template #item="{ element }">
                        <div class="custom-action-card-row">
                          <button type="button" class="custom-action-drag" title="拖拽排序">
                            <n-icon><ReorderThreeOutline /></n-icon>
                          </button>
                          <n-input
                            class="custom-action-name-input"
                            size="small"
                            :value="element.label || '自定义按钮'"
                            placeholder="按钮文字"
                            @update:value="value => updateCustomActionByIdentity(element, { label: value || '自定义按钮' })"
                          >
                            <template #suffix>
                              <button type="button" class="custom-action-inline-icon" title="设置" @click.stop="openCustomActionEditor(element)">
                                <n-icon><SettingsOutline /></n-icon>
                              </button>
                            </template>
                          </n-input>
                          <n-dropdown
                            trigger="click"
                            :options="customActionMoreOptions(element)"
                            @select="key => handleCustomActionMoreSelect(key, element)"
                          >
                            <button type="button" class="custom-action-more" title="更多">
                              <n-icon><EllipsisHorizontalOutline /></n-icon>
                            </button>
                          </n-dropdown>
                        </div>
                      </template>
                    </draggable>
                    <span v-if="!toolbarCustomActions.length" class="custom-action-empty">暂无工具栏自定义按钮</span>
                  </div>
                </div>
              </template>

              <!-- Tree panel -->
              <template v-if="selectedBlock.blockType === 'tree-panel'">
                <n-form-item label="树数据来源">
                  <n-select
                    :value="selectedBlock.props?.sourceModelCode"
                    :options="treeSourceOptions"
                    clearable
                    @update:value="handleTreeSourceChange"
                  />
                </n-form-item>
                <n-form-item label="树标题">
                  <n-input
                    :value="selectedBlock.props?.treeTitle"
                    @update:value="patchBlockProps(selectedBlock.id, { treeTitle: $event })"
                  />
                </n-form-item>
                <n-form-item label="树节点主键">
                  <n-select
                    :value="selectedBlock.props?.keyField"
                    :options="treeFieldOptions"
                    clearable
                    @update:value="patchBlockProps(selectedBlock.id, { keyField: $event })"
                  />
                </n-form-item>
                <n-form-item label="树父级字段">
                  <n-select
                    :value="selectedBlock.props?.parentField"
                    :options="treeFieldOptions"
                    clearable
                    @update:value="patchBlockProps(selectedBlock.id, { parentField: $event })"
                  />
                </n-form-item>
                <n-form-item label="树显示字段">
                  <n-select
                    :value="selectedBlock.props?.labelField"
                    :options="treeFieldOptions"
                    clearable
                    @update:value="patchBlockProps(selectedBlock.id, { labelField: $event })"
                  />
                </n-form-item>
                <n-form-item label="加载方式">
                  <n-select
                    :value="selectedBlock.props?.loadMode || 'full'"
                    :options="treeLoadModeOptions"
                    @update:value="patchBlockProps(selectedBlock.id, { loadMode: $event })"
                  />
                </n-form-item>
                <n-form-item label="节点取值字段">
                  <n-select
                    :value="selectedBlock.props?.targetField"
                    :options="treeFieldOptions"
                    clearable
                    @update:value="patchBlockProps(selectedBlock.id, { targetField: $event })"
                  />
                </n-form-item>
                <n-form-item label="右表过滤字段">
                  <n-select
                    :value="selectedBlock.props?.filterField"
                    :options="primaryFieldOptions"
                    clearable
                    @update:value="patchBlockProps(selectedBlock.id, { filterField: $event })"
                  />
                  <div class="field-help">
                    树接口独立加载；点击节点只给右侧列表追加“{{ selectedBlock.props?.filterField || '列表过滤字段' }} = 节点的 {{ selectedBlock.props?.targetField || selectedBlock.props?.keyField || 'id' }}”过滤条件。
                  </div>
                </n-form-item>
              </template>

              <template v-if="localDataBindableBlockTypes.includes(selectedBlock.blockType)">
                <n-form-item label="数据来源">
                  <div class="data-source-editor">
                    <div class="field-help">
                      选择组件内容从哪里来。静态配置使用当前属性；当前详情数据从页面已有记录取值；远程接口会请求接口后再按字段映射渲染。
                    </div>
                    <div class="data-source-row">
                      <span>来源类型</span>
                      <n-select
                        :value="selectedBlock.props?.dataBinding?.sourceType || 'static'"
                        :options="widgetDataSourceOptions"
                        @update:value="updateWidgetDataBinding({ enabled: $event !== 'static', sourceType: $event || 'static' })"
                      />
                    </div>
                    <div v-if="selectedBlock.props?.dataBinding?.sourceType === 'context'" class="data-source-row">
                      <span>取值路径</span>
                      <n-input
                        :value="selectedBlock.props?.dataBinding?.contextPath || ''"
                        clearable
                        placeholder="不填表示整条当前记录；例如 customer.name"
                        @update:value="updateWidgetDataBinding({ contextPath: $event || '' })"
                      />
                    </div>
                    <template v-if="selectedBlock.props?.dataBinding?.sourceType === 'remote'">
                      <div class="data-source-row">
                        <span>接口地址</span>
                        <n-input
                          :value="selectedBlock.props?.dataBinding?.api || ''"
                          clearable
                          placeholder="例如 /api/order/detail/{{ id }}"
                          @update:value="updateWidgetDataBinding({ api: $event || '' })"
                        />
                      </div>
                      <div class="data-source-row">
                        <span>请求方式</span>
                        <n-select
                          :value="selectedBlock.props?.dataBinding?.method || 'get'"
                          :options="requestMethodOptions"
                          @update:value="updateWidgetDataBinding({ method: $event || 'get' })"
                        />
                      </div>
                      <div class="data-source-row">
                        <span>响应路径</span>
                        <n-input
                          :value="selectedBlock.props?.dataBinding?.dataPath || 'data'"
                          clearable
                          placeholder="如 data、data.records"
                          @update:value="updateWidgetDataBinding({ dataPath: $event || 'data' })"
                        />
                      </div>
                      <div class="data-source-row">
                        <span>请求参数</span>
                        <n-input
                          :value="selectedBlock.props?.dataBinding?.paramsText || '{}'"
                          type="textarea"
                          :rows="3"
                          placeholder="{ &quot;id&quot;: &quot;{{ id }}&quot; }，可引用当前详情数据"
                          @update:value="updateWidgetDataBinding({ paramsText: $event || '{}' })"
                        />
                      </div>
                    </template>
                    <div v-if="selectedBlock.props?.dataBinding?.sourceType !== 'static'" class="field-help">
                      字段映射用于告诉组件接口返回的字段含义。列表/标签/步骤类组件常用“显示文本、值、标题、描述”；单值组件通常只需要“值字段”或“内容字段”。
                    </div>
                    <div v-if="selectedBlock.props?.dataBinding?.sourceType !== 'static'" class="data-source-mapping-grid">
                      <div class="data-source-row">
                        <span>显示文本</span>
                        <n-input
                          :value="selectedBlock.props?.dataBinding?.labelField || 'label'"
                          placeholder="label / name"
                          @update:value="updateWidgetDataBinding({ labelField: $event || 'label' })"
                        />
                      </div>
                      <div class="data-source-row">
                        <span>值字段</span>
                        <n-input
                          :value="selectedBlock.props?.dataBinding?.valueField || 'value'"
                          placeholder="value / id / src"
                          @update:value="updateWidgetDataBinding({ valueField: $event || 'value' })"
                        />
                      </div>
                      <div class="data-source-row">
                        <span>标题字段</span>
                        <n-input
                          :value="selectedBlock.props?.dataBinding?.titleField || 'title'"
                          placeholder="title / text"
                          @update:value="updateWidgetDataBinding({ titleField: $event || 'title' })"
                        />
                      </div>
                      <div class="data-source-row">
                        <span>描述字段</span>
                        <n-input
                          :value="selectedBlock.props?.dataBinding?.descriptionField || 'description'"
                          placeholder="description / content"
                          @update:value="updateWidgetDataBinding({ descriptionField: $event || 'description' })"
                        />
                      </div>
                    </div>
                  </div>
                </n-form-item>
              </template>

              <!-- Stats strip -->
              <template v-if="selectedBlock.blockType === 'stats-strip'">
                <n-form-item label="指标项">
                  <div class="metrics-editor">
                    <div
                      v-for="(metric, idx) in (selectedBlock.props?.metrics || [])"
                      :key="idx"
                      class="metric-row"
                    >
                      <n-input
                        :value="metric.label"
                        placeholder="标签"
                        size="small"
                        @update:value="updateMetric(idx, { label: $event })"
                      />
                      <n-input
                        :value="metric.value"
                        placeholder="数值"
                        size="small"
                        @update:value="updateMetric(idx, { value: $event })"
                      />
                      <n-input
                        :value="metric.trend"
                        placeholder="+5%"
                        size="small"
                        @update:value="updateMetric(idx, { trend: $event })"
                      />
                      <n-button size="tiny" quaternary @click="removeMetric(idx)">
                        删
                      </n-button>
                    </div>
                    <n-button size="small" dashed block @click="addMetric">
                      + 添加指标
                    </n-button>
                  </div>
                </n-form-item>
              </template>

              <template v-if="selectedBlock.blockType === 'detail-info'">
                <n-form-item label="详情字段">
                  <n-button size="small" type="primary" secondary @click="openFieldDrawer('table')">
                    配置字段（{{ selectedBlock.fieldRefs?.length || 0 }}/{{ fields.length }}）
                  </n-button>
                </n-form-item>
                <n-form-item label="数据来源">
                  <div class="metrics-editor">
                    <div class="style-grid three">
                      <n-select
                        :value="selectedBlock.props?.dataSourceType || 'current'"
                        :options="detailInfoDataSourceOptions"
                        @update:value="patchBlockProps(selectedBlock.id, { dataSourceType: $event || 'current' })"
                      />
                      <n-input
                        :value="selectedBlock.props?.contextPath || ''"
                        clearable
                        placeholder="当前详情数据路径，如 detail"
                        @update:value="patchBlockProps(selectedBlock.id, { contextPath: $event || '' })"
                      />
                      <n-input
                        :value="selectedBlock.props?.dataPath || 'data'"
                        clearable
                        placeholder="接口响应路径，如 data"
                        @update:value="patchBlockProps(selectedBlock.id, { dataPath: $event || 'data' })"
                      />
                    </div>
                    <template v-if="selectedBlock.props?.dataSourceType === 'remote'">
                      <div class="style-grid two">
                        <n-input
                          :value="selectedBlock.props?.detailApi || ''"
                          clearable
                          placeholder="详情接口，如 get@/api/order/{{ id }}"
                          @update:value="patchBlockProps(selectedBlock.id, { detailApi: $event || '' })"
                        />
                        <n-select
                          :value="selectedBlock.props?.detailMethod || 'get'"
                          :options="requestMethodOptions"
                          @update:value="patchBlockProps(selectedBlock.id, { detailMethod: $event || 'get' })"
                        />
                      </div>
                      <n-input
                        :value="selectedBlock.props?.paramsText || '{}'"
                        type="textarea"
                        :rows="3"
                        placeholder="{ &quot;id&quot;: &quot;{{ id }}&quot; }，可引用当前详情数据"
                        @update:value="patchBlockProps(selectedBlock.id, { paramsText: $event || '{}' })"
                      />
                    </template>
                  </div>
                </n-form-item>
                <n-form-item label="详情布局">
                  <div class="detail-layout-grid">
                    <label class="detail-layout-control">
                      <span>列数</span>
                      <n-input-number
                        :value="selectedBlock.props?.columnCount || 2"
                        :min="1"
                        :max="4"
                        :show-button="false"
                        @update:value="patchBlockProps(selectedBlock.id, { columnCount: $event || 2 })"
                      />
                    </label>
                    <label class="detail-layout-control">
                      <span>标题位置</span>
                      <n-select
                        :value="selectedBlock.props?.labelPlacement || 'left'"
                        :options="labelPlacementOptions"
                        @update:value="patchBlockProps(selectedBlock.id, { labelPlacement: $event || 'left' })"
                      />
                    </label>
                    <label class="detail-layout-switch">
                      <span>
                        <strong>显示边框</strong>
                        <small>控制详情字段之间是否显示分隔边框</small>
                      </span>
                      <n-switch
                        :value="!!selectedBlock.props?.bordered"
                        @update:value="patchBlockProps(selectedBlock.id, { bordered: $event })"
                      />
                    </label>
                  </div>
                </n-form-item>
              </template>

              <template v-if="selectedBlock.blockType === 'info-panel'">
                <n-form-item label="提示内容">
                  <div class="metrics-editor">
                    <n-input
                      :value="selectedBlock.props?.title"
                      placeholder="标题"
                      @update:value="patchBlockProps(selectedBlock.id, { title: $event })"
                    />
                    <n-input
                      :value="selectedBlock.props?.content"
                      type="textarea"
                      :rows="3"
                      placeholder="内容"
                      @update:value="patchBlockProps(selectedBlock.id, { content: $event })"
                    />
                  </div>
                </n-form-item>
                <n-form-item label="提示类型">
                  <n-select
                    :value="selectedBlock.props?.type || 'info'"
                    :options="tagTypeOptions"
                    @update:value="patchBlockProps(selectedBlock.id, { type: $event || 'info' })"
                  />
                </n-form-item>
              </template>

              <!-- Custom html -->
              <template v-if="selectedBlock.blockType === 'custom-html'">
                <n-form-item label="标题">
                  <n-input
                    :value="selectedBlock.props?.title"
                    @update:value="patchBlockProps(selectedBlock.id, { title: $event })"
                  />
                </n-form-item>
                <n-form-item label="正文">
                  <n-input
                    :value="selectedBlock.props?.content"
                    type="textarea"
                    :rows="4"
                    @update:value="patchBlockProps(selectedBlock.id, { content: $event })"
                  />
                </n-form-item>
              </template>

              <template v-if="selectedBlock.blockType === 'action-button'">
                <n-form-item label="基础样式">
                  <div class="style-grid three">
                    <n-input
                      :value="selectedBlock.props?.text"
                      placeholder="按钮文本"
                      @update:value="patchBlockProps(selectedBlock.id, { text: $event })"
                    />
                    <n-select
                      :value="selectedBlock.props?.type || 'primary'"
                      :options="actionTypeOptions"
                      @update:value="patchBlockProps(selectedBlock.id, { type: $event || 'primary' })"
                    />
                    <n-select
                      :value="selectedBlock.props?.size || 'small'"
                      :options="componentSizeOptions"
                      @update:value="patchBlockProps(selectedBlock.id, { size: $event || 'small' })"
                    />
                  </div>
                </n-form-item>
                <n-form-item label="点击动作">
                  <div class="action-button-event-grid">
                    <n-select
                      :value="resolvePrimaryClickEvent(selectedBlock).action"
                      :options="blockEventActionOptions"
                      placeholder="点击后执行"
                      @update:value="updatePrimaryClickEvent({ action: $event || 'none' })"
                    />
                    <n-select
                      v-if="resolvePrimaryClickEvent(selectedBlock).action === 'navigate'"
                      :value="resolvePrimaryClickEvent(selectedBlock).targetPageKey"
                      :options="pageTargetOptions"
                      clearable
                      filterable
                      placeholder="目标页面"
                      @update:value="updatePrimaryClickEvent({ targetPageKey: $event || '' })"
                    />
                    <n-select
                      v-if="resolvePrimaryClickEvent(selectedBlock).action === 'navigate' && formTargetOptions.length"
                      :value="resolvePrimaryClickEvent(selectedBlock).targetFormKey"
                      :options="formTargetOptions"
                      clearable
                      filterable
                      placeholder="目标表单"
                      @update:value="updatePrimaryClickEvent({ targetFormKey: $event || '' })"
                    />
                    <n-input
                      v-if="resolvePrimaryClickEvent(selectedBlock).action === 'request'"
                      :value="resolvePrimaryClickEvent(selectedBlock).requestUrl"
                      clearable
                      placeholder="接口地址，例如 post@/api/xxx"
                      @update:value="updatePrimaryClickEvent({ requestUrl: $event || '' })"
                    />
                  </div>
                  <div class="field-help">
                    这里会写入按钮的 click 事件；参数、权限、确认提示可在“事件回调”里继续补充。
                  </div>
                </n-form-item>
                <n-form-item label="权限与确认">
                  <div class="action-button-event-grid">
                    <n-input
                      :value="resolvePrimaryClickEvent(selectedBlock).permissionCode"
                      clearable
                      placeholder="权限码，例如 ai:business:customer:edit"
                      @update:value="updatePrimaryClickEvent({ permissionCode: $event || '' })"
                    />
                    <n-input
                      :value="resolvePrimaryClickEvent(selectedBlock).confirmText"
                      clearable
                      placeholder="确认提示，留空则不弹窗"
                      @update:value="updatePrimaryClickEvent({ confirmText: $event || '' })"
                    />
                    <n-input
                      :value="resolvePrimaryClickEvent(selectedBlock).displayCondition || ''"
                      clearable
                      placeholder="显示条件，如 status=待处理"
                      @update:value="updatePrimaryClickEvent({ displayCondition: $event || '' })"
                    />
                  </div>
                </n-form-item>
                <n-form-item label="成功后行为">
                  <n-select
                    :value="resolvePrimaryClickEvent(selectedBlock).successBehavior || 'none'"
                    :options="successBehaviorOptions"
                    @update:value="updatePrimaryClickEvent({ successBehavior: $event || 'none' })"
                  />
                </n-form-item>
                <n-form-item label="按钮状态">
                  <div class="toolbar-toggle-list">
                    <div class="switch-line toolbar-toggle-row">
                      <span class="switch-line-text">
                        <strong>次要按钮</strong>
                        <small>打开后使用浅色按钮样式，适合“取消、查看、次操作”。</small>
                      </span>
                      <n-switch
                        :value="!!selectedBlock.props?.secondary"
                        @update:value="patchBlockProps(selectedBlock.id, { secondary: $event })"
                      />
                    </div>
                    <div class="switch-line toolbar-toggle-row">
                      <span class="switch-line-text">
                        <strong>撑满宽度</strong>
                        <small>打开后按钮宽度撑满当前按钮组件区块。</small>
                      </span>
                      <n-switch
                        :value="!!selectedBlock.props?.block"
                        @update:value="patchBlockProps(selectedBlock.id, { block: $event })"
                      />
                    </div>
                    <div class="switch-line toolbar-toggle-row">
                      <span class="switch-line-text">
                        <strong>禁用状态</strong>
                        <small>打开后按钮不可点击，用来预览无权限或条件不满足状态。</small>
                      </span>
                      <n-switch
                        :value="!!selectedBlock.props?.disabled"
                        @update:value="patchBlockProps(selectedBlock.id, { disabled: $event })"
                      />
                    </div>
                    <div class="switch-line toolbar-toggle-row">
                      <span class="switch-line-text">
                        <strong>加载状态</strong>
                        <small>打开后显示加载中，用来预览提交中的按钮状态。</small>
                      </span>
                      <n-switch
                        :value="!!selectedBlock.props?.loading"
                        @update:value="patchBlockProps(selectedBlock.id, { loading: $event })"
                      />
                    </div>
                    <div class="field-help">
                      这里的开关只改当前按钮的显示状态；点击后要跳转、刷新或调用接口，请在“事件回调”里配置点击事件。
                    </div>
                  </div>
                </n-form-item>
              </template>

              <template v-if="selectedBlock.blockType === 'button-group'">
                <n-form-item label="按钮组">
                  <div class="metrics-editor">
                    <div
                      v-for="(button, idx) in (selectedBlock.props?.buttons || [])"
                      :key="button.key || idx"
                      class="metric-row"
                    >
                      <n-input
                        :value="button.text"
                        placeholder="文本"
                        size="small"
                        @update:value="updateButtonGroupItem(idx, { text: $event })"
                      />
                      <n-select
                        :value="button.type || 'default'"
                        :options="actionTypeOptions"
                        size="small"
                        @update:value="updateButtonGroupItem(idx, { type: $event || 'default' })"
                      />
                      <n-button size="tiny" quaternary @click="removeButtonGroupItem(idx)">
                        删
                      </n-button>
                    </div>
                    <n-button size="small" dashed block @click="addButtonGroupItem">
                      + 添加按钮
                    </n-button>
                  </div>
                </n-form-item>
              </template>

              <template v-if="selectedBlock.blockType === 'tag-list'">
                <n-form-item label="标签">
                  <div class="metrics-editor">
                    <div
                      v-for="(tag, idx) in (selectedBlock.props?.tags || [])"
                      :key="idx"
                      class="metric-row"
                    >
                      <n-input
                        :value="tag.label"
                        placeholder="标签"
                        size="small"
                        @update:value="updateTagItem(idx, { label: $event })"
                      />
                      <n-select
                        :value="tag.type || 'default'"
                        :options="tagTypeOptions"
                        size="small"
                        @update:value="updateTagItem(idx, { type: $event || 'default' })"
                      />
                      <n-button size="tiny" quaternary @click="removeTagItem(idx)">
                        删
                      </n-button>
                    </div>
                    <n-button size="small" dashed block @click="addTagItem">
                      + 添加标签
                    </n-button>
                  </div>
                </n-form-item>
              </template>

              <template v-if="selectedBlock.blockType === 'steps'">
                <n-form-item label="步骤">
                  <div class="metrics-editor">
                    <n-input-number
                      :value="selectedBlock.props?.current || 1"
                      :min="1"
                      :max="10"
                      :show-button="false"
                      @update:value="patchBlockProps(selectedBlock.id, { current: $event || 1 })"
                    />
                    <div
                      v-for="(step, idx) in (selectedBlock.props?.steps || [])"
                      :key="idx"
                      class="metric-row"
                    >
                      <n-input
                        :value="step.title"
                        placeholder="标题"
                        size="small"
                        @update:value="updateStepItem(idx, { title: $event })"
                      />
                      <n-input
                        :value="step.description"
                        placeholder="描述"
                        size="small"
                        @update:value="updateStepItem(idx, { description: $event })"
                      />
                      <n-button size="tiny" quaternary @click="removeStepItem(idx)">
                        删
                      </n-button>
                    </div>
                    <n-button size="small" dashed block @click="addStepItem">
                      + 添加步骤
                    </n-button>
                  </div>
                </n-form-item>
              </template>

              <template v-if="selectedBlock.blockType === 'timeline'">
                <n-form-item label="时间线">
                  <div class="metrics-editor">
                    <n-input
                      :value="selectedBlock.props?.title"
                      placeholder="标题"
                      @update:value="patchBlockProps(selectedBlock.id, { title: $event })"
                    />
                    <div
                      v-for="(item, idx) in (selectedBlock.props?.items || [])"
                      :key="idx"
                      class="metric-row"
                    >
                      <n-input
                        :value="item.title"
                        placeholder="节点"
                        size="small"
                        @update:value="updateTimelineItem(idx, { title: $event })"
                      />
                      <n-input
                        :value="item.time"
                        placeholder="时间"
                        size="small"
                        @update:value="updateTimelineItem(idx, { time: $event })"
                      />
                      <n-button size="tiny" quaternary @click="removeTimelineItem(idx)">
                        删
                      </n-button>
                      <n-input
                        :value="item.content"
                        placeholder="内容"
                        size="small"
                        @update:value="updateTimelineItem(idx, { content: $event })"
                      />
                    </div>
                    <n-button size="small" dashed block @click="addTimelineItem">
                      + 添加节点
                    </n-button>
                  </div>
                </n-form-item>
              </template>

              <template v-if="selectedBlock.blockType === 'empty-state'">
                <n-form-item label="空状态">
                  <div class="metrics-editor">
                    <n-input
                      :value="selectedBlock.props?.title"
                      placeholder="标题"
                      @update:value="patchBlockProps(selectedBlock.id, { title: $event })"
                    />
                    <n-input
                      :value="selectedBlock.props?.description"
                      placeholder="描述"
                      @update:value="patchBlockProps(selectedBlock.id, { description: $event })"
                    />
                    <n-input
                      :value="selectedBlock.props?.actionText"
                      placeholder="按钮文本"
                      @update:value="patchBlockProps(selectedBlock.id, { actionText: $event })"
                    />
                  </div>
                </n-form-item>
              </template>

              <!-- Sub table tabs -->
              <template v-if="selectedBlock.blockType === 'sub-table-tabs'">
                <n-form-item label="Tab 项">
                  <div class="metrics-editor">
                    <div
                      v-for="(tab, idx) in (selectedBlock.props?.tabs || [])"
                      :key="idx"
                      class="metric-row"
                    >
                      <n-input
                        :value="tab.title"
                        placeholder="标题"
                        size="small"
                        @update:value="updateTab(idx, { title: $event })"
                      />
                      <n-input
                        :value="tab.key"
                        placeholder="key"
                        size="small"
                        @update:value="updateTab(idx, { key: $event })"
                      />
                      <n-button size="tiny" quaternary @click="removeTab(idx)">
                        删
                      </n-button>
                    </div>
                    <n-button size="small" dashed block @click="addTab">
                      + 添加 Tab
                    </n-button>
                  </div>
                </n-form-item>
              </template>

              <!-- Section divider -->
              <template v-if="selectedBlock.blockType === 'section-divider'">
                <n-form-item label="标题">
                  <n-input
                    :value="selectedBlock.props?.title"
                    @update:value="patchBlockProps(selectedBlock.id, { title: $event })"
                  />
                </n-form-item>
              </template>

              <template v-if="selectedBlock.blockType === 'divider'">
                <n-form-item label="方向">
                  <n-radio-group
                    :value="selectedBlock.props?.orientation || 'horizontal'"
                    size="small"
                    @update:value="patchBlockProps(selectedBlock.id, { orientation: $event })"
                  >
                    <n-radio-button value="horizontal">
                      横向
                    </n-radio-button>
                    <n-radio-button value="vertical">
                      竖向
                    </n-radio-button>
                  </n-radio-group>
                </n-form-item>
                <n-form-item label="标题">
                  <n-input
                    :value="selectedBlock.props?.title"
                    @update:value="patchBlockProps(selectedBlock.id, { title: $event })"
                  />
                </n-form-item>
              </template>

              <template v-if="selectedBlock.blockType === 'card'">
                <n-form-item label="卡片标题">
                  <n-input
                    :value="selectedBlock.props?.title"
                    @update:value="patchBlockProps(selectedBlock.id, { title: $event })"
                  />
                </n-form-item>
                <n-form-item label="卡片内容">
                  <n-input
                    :value="selectedBlock.props?.content"
                    type="textarea"
                    :rows="3"
                    @update:value="patchBlockProps(selectedBlock.id, { content: $event })"
                  />
                </n-form-item>
                <n-form-item label="卡片内组件">
                  <div class="container-child-editor">
                    <div
                      v-for="child in (selectedBlock.children || [])"
                      :key="child.id"
                      class="container-child-row"
                    >
                      <span>{{ child.label || child.blockType }}</span>
                      <n-button size="tiny" quaternary type="error" @click="removeContainerChild(selectedBlock.id, child.id)">
                        删除
                      </n-button>
                    </div>
                    <div v-if="!(selectedBlock.children || []).length" class="container-child-empty">
                      可把左侧组件拖入卡片，也可以在这里添加。
                    </div>
                    <n-select
                      :options="childBlockTypeOptions"
                      size="small"
                      placeholder="添加组件到卡片"
                      clearable
                      @update:value="value => value && appendContainerChild(selectedBlock.id, value)"
                    />
                  </div>
                </n-form-item>
              </template>

              <template v-if="selectedBlock.blockType === 'tabs'">
                <n-form-item label="Tab 项">
                  <div class="metrics-editor">
                    <div
                      v-for="(tab, idx) in (selectedBlock.props?.tabs || [])"
                      :key="idx"
                      class="metric-row"
                    >
                      <n-input
                        :value="tab.title"
                        placeholder="标题"
                        size="small"
                        @update:value="updateTab(idx, { title: $event })"
                      />
                      <n-input
                        :value="tab.key"
                        placeholder="key"
                        size="small"
                        @update:value="updateTab(idx, { key: $event })"
                      />
                      <n-button size="tiny" quaternary @click="removeTab(idx)">
                        删
                      </n-button>
                    </div>
                    <n-button size="small" dashed block @click="addTab">
                      + 添加 Tab
                    </n-button>
                  </div>
                </n-form-item>
                <n-form-item label="当前 Tab 内容">
                  <div class="container-child-editor">
                    <n-select
                      :value="activeTabKey"
                      :options="tabPaneOptions"
                      size="small"
                      @update:value="activeTabKey = $event"
                    />
                    <div
                      v-for="child in activeTabChildren"
                      :key="child.id"
                      class="container-child-row"
                    >
                      <span>{{ child.label || child.blockType }}</span>
                      <n-button size="tiny" quaternary type="error" @click="removeTabChild(child.id)">
                        删除
                      </n-button>
                    </div>
                    <div v-if="!activeTabChildren.length" class="container-child-empty">
                      可把左侧组件拖入当前 Tab，也可以在这里添加。
                    </div>
                    <n-select
                      :options="childBlockTypeOptions"
                      size="small"
                      placeholder="添加组件到当前 Tab"
                      clearable
                      @update:value="value => value && appendTabChild(value)"
                    />
                  </div>
                </n-form-item>
              </template>
              <template v-if="simpleConfigBlockTypes.includes(selectedBlock.blockType)">
                <div class="property-search-anchor" data-property-search="内容 文本 标题 段落 富文本 签名 穿梭框 分步表单 Vue HTML 统计 链接 提示 水印 音频 视频 头像 条码 二维码 Markdown 日历 代码 倒计时 描述 公示 列表 日志 数值动画 面包屑 菜单 分页 面板分隔 盒子 间距 iframe" />
                <n-divider>组件内容</n-divider>
                <template v-if="pageWidgetComponentKeys.includes(selectedBlock.blockType)">
                  <n-form-item label="组件标题">
                    <n-input
                      :value="selectedBlock.props?.title || selectedBlock.label"
                      clearable
                      placeholder="请输入标题"
                      @update:value="patchBlockProps(selectedBlock.id, { title: $event })"
                    />
                  </n-form-item>
                  <template v-if="dataBindablePageWidgetKeys.includes(selectedBlock.blockType)">
                    <n-form-item label="数据来源">
                      <div class="data-source-editor">
                        <div class="field-help">
                          选择组件内容从哪里来。静态配置使用当前属性；当前详情数据从页面已有记录取值；远程接口会请求接口后再按字段映射渲染。
                        </div>
                        <div class="data-source-row">
                          <span>来源类型</span>
                          <n-select
                            :value="selectedBlock.props?.dataBinding?.sourceType || 'static'"
                            :options="widgetDataSourceOptions"
                            @update:value="updateWidgetDataBinding({ enabled: $event !== 'static', sourceType: $event || 'static' })"
                          />
                        </div>
                        <div v-if="selectedBlock.props?.dataBinding?.sourceType === 'context'" class="data-source-row">
                          <span>取值路径</span>
                          <n-input
                            :value="selectedBlock.props?.dataBinding?.contextPath || ''"
                            clearable
                            placeholder="不填表示整条当前记录；例如 customer.name"
                            @update:value="updateWidgetDataBinding({ contextPath: $event || '' })"
                          />
                        </div>
                        <template v-if="selectedBlock.props?.dataBinding?.sourceType === 'remote'">
                          <div class="data-source-row">
                            <span>接口地址</span>
                            <n-input
                              :value="selectedBlock.props?.dataBinding?.api || ''"
                              clearable
                              placeholder="例如 /api/order/detail/{{ id }}"
                              @update:value="updateWidgetDataBinding({ api: $event || '' })"
                            />
                          </div>
                          <div class="data-source-row">
                            <span>请求方式</span>
                            <n-select
                              :value="selectedBlock.props?.dataBinding?.method || 'get'"
                              :options="requestMethodOptions"
                              @update:value="updateWidgetDataBinding({ method: $event || 'get' })"
                            />
                          </div>
                          <div class="data-source-row">
                            <span>响应路径</span>
                            <n-input
                              :value="selectedBlock.props?.dataBinding?.dataPath || 'data'"
                              clearable
                              placeholder="如 data、data.records"
                              @update:value="updateWidgetDataBinding({ dataPath: $event || 'data' })"
                            />
                          </div>
                          <div class="data-source-row">
                            <span>请求参数</span>
                            <n-input
                              :value="selectedBlock.props?.dataBinding?.paramsText || '{}'"
                              type="textarea"
                              :rows="3"
                              placeholder="{ &quot;id&quot;: &quot;{{ id }}&quot; }，可引用当前详情数据"
                              @update:value="updateWidgetDataBinding({ paramsText: $event || '{}' })"
                            />
                          </div>
                        </template>
                        <div v-if="selectedBlock.props?.dataBinding?.sourceType !== 'static'" class="field-help">
                          字段映射用于告诉组件接口返回的字段含义。列表/标签/步骤类组件常用“显示文本、值、标题、描述”；单值组件通常只需要“值字段”或“内容字段”。
                        </div>
                        <div v-if="selectedBlock.props?.dataBinding?.sourceType !== 'static'" class="data-source-mapping-grid">
                          <div class="data-source-row">
                            <span>显示文本</span>
                            <n-input
                              :value="selectedBlock.props?.dataBinding?.labelField || 'label'"
                              placeholder="label / name"
                              @update:value="updateWidgetDataBinding({ labelField: $event || 'label' })"
                            />
                          </div>
                          <div class="data-source-row">
                            <span>值字段</span>
                            <n-input
                              :value="selectedBlock.props?.dataBinding?.valueField || 'value'"
                              placeholder="value / id / src"
                              @update:value="updateWidgetDataBinding({ valueField: $event || 'value' })"
                            />
                          </div>
                          <div class="data-source-row">
                            <span>标题字段</span>
                            <n-input
                              :value="selectedBlock.props?.dataBinding?.titleField || 'title'"
                              placeholder="title / text"
                              @update:value="updateWidgetDataBinding({ titleField: $event || 'title' })"
                            />
                          </div>
                          <div class="data-source-row">
                            <span>描述字段</span>
                            <n-input
                              :value="selectedBlock.props?.dataBinding?.descriptionField || 'description'"
                              placeholder="description / content"
                              @update:value="updateWidgetDataBinding({ descriptionField: $event || 'description' })"
                            />
                          </div>
                        </div>
                      </div>
                    </n-form-item>
                  </template>
                  <template v-if="selectedBlock.blockType === 'rich-text'">
                    <n-form-item label="编辑模式 / 工具栏模式">
                      <div class="style-grid two">
                        <n-select
                          :value="selectedBlock.props?.editorMode || 'visual'"
                          :options="richEditorModeOptions"
                          @update:value="patchBlockProps(selectedBlock.id, { editorMode: $event || 'visual' })"
                        />
                        <n-select
                          :value="selectedBlock.props?.toolbarMode || 'default'"
                          :options="wangEditorModeOptions"
                          @update:value="patchBlockProps(selectedBlock.id, { toolbarMode: $event || 'default', editorModeName: $event || 'default' })"
                        />
                      </div>
                    </n-form-item>
                    <n-form-item label="富文本 HTML / 源码">
                      <n-input
                        :value="selectedBlock.props?.content"
                        type="textarea"
                        :rows="8"
                        placeholder="输入富文本 HTML 内容"
                        @update:value="patchBlockProps(selectedBlock.id, { content: $event })"
                      />
                    </n-form-item>
                    <n-form-item label="字号 / 行高 / 最小高度">
                      <div class="style-grid three">
                        <n-input-number :value="selectedBlock.props?.fontSize || 14" :min="10" :max="48" :show-button="false" @update:value="patchBlockProps(selectedBlock.id, { fontSize: $event || 14 })" />
                        <n-input-number :value="selectedBlock.props?.lineHeight || 1.7" :min="1" :max="3" :step="0.1" :show-button="false" @update:value="patchBlockProps(selectedBlock.id, { lineHeight: $event || 1.7 })" />
                        <n-input-number :value="selectedBlock.props?.minHeight || 180" :min="80" :max="600" :show-button="false" @update:value="patchBlockProps(selectedBlock.id, { minHeight: $event || 180 })" />
                      </div>
                    </n-form-item>
                  </template>
                  <template v-if="selectedBlock.blockType === 'markdown'">
                    <n-form-item label="预览模式">
                      <n-select
                        :value="selectedBlock.props?.previewMode || 'split'"
                        :options="markdownPreviewModeOptions"
                        @update:value="patchBlockProps(selectedBlock.id, { previewMode: $event || 'split' })"
                      />
                    </n-form-item>
                    <n-form-item label="编辑器高度">
                      <n-input-number
                        :value="selectedBlock.props?.height || 320"
                        :min="180"
                        :max="900"
                        :show-button="false"
                        @update:value="patchBlockProps(selectedBlock.id, { height: $event || 320 })"
                      />
                    </n-form-item>
                    <n-form-item label="Markdown 源码">
                      <n-input
                        :value="selectedBlock.props?.content"
                        type="textarea"
                        :rows="10"
                        placeholder="输入 Markdown 源码"
                        @update:value="patchBlockProps(selectedBlock.id, { content: $event })"
                      />
                    </n-form-item>
                  </template>
                  <template v-if="selectedBlock.blockType === 'watermark'">
                    <n-form-item label="水印文字">
                      <n-input
                        :value="selectedBlock.props?.content || ''"
                        type="textarea"
                        :rows="3"
                        placeholder="支持多行文本"
                        @update:value="patchBlockProps(selectedBlock.id, { content: $event })"
                      />
                    </n-form-item>
                    <n-form-item label="字体 / 行高 / 字重">
                      <div class="style-grid three">
                        <n-input-number :value="selectedBlock.props?.fontSize || 14" :min="8" :max="72" :show-button="false" @update:value="patchBlockProps(selectedBlock.id, { fontSize: $event || 14 })" />
                        <n-input-number :value="selectedBlock.props?.lineHeight || 14" :min="8" :max="96" :show-button="false" @update:value="patchBlockProps(selectedBlock.id, { lineHeight: $event || 14 })" />
                        <n-input-number :value="selectedBlock.props?.fontWeight || 400" :min="100" :max="900" :step="100" :show-button="false" @update:value="patchBlockProps(selectedBlock.id, { fontWeight: $event || 400 })" />
                      </div>
                    </n-form-item>
                    <n-form-item label="颜色 / 样式 / 对齐">
                      <div class="style-grid three">
                        <n-color-picker :value="selectedBlock.props?.fontColor || 'rgba(128, 128, 128, .3)'" :show-alpha="true" @update:value="patchBlockProps(selectedBlock.id, { fontColor: $event || 'rgba(128, 128, 128, .3)' })" />
                        <n-select :value="selectedBlock.props?.fontStyle || 'normal'" :options="watermarkFontStyleOptions" @update:value="patchBlockProps(selectedBlock.id, { fontStyle: $event || 'normal' })" />
                        <n-select :value="selectedBlock.props?.textAlign || 'left'" :options="watermarkTextAlignOptions" @update:value="patchBlockProps(selectedBlock.id, { textAlign: $event || 'left' })" />
                      </div>
                    </n-form-item>
                    <n-form-item label="宽高 / 旋转">
                      <div class="style-grid four">
                        <n-input-number :value="selectedBlock.props?.width || 32" :min="1" :max="600" :show-button="false" @update:value="patchBlockProps(selectedBlock.id, { width: $event || 32 })" />
                        <n-input-number :value="selectedBlock.props?.height || 32" :min="1" :max="600" :show-button="false" @update:value="patchBlockProps(selectedBlock.id, { height: $event || 32 })" />
                        <n-input-number :value="selectedBlock.props?.rotate || 0" :min="-180" :max="180" :show-button="false" @update:value="patchBlockProps(selectedBlock.id, { rotate: $event || 0 })" />
                        <n-input-number :value="selectedBlock.props?.globalRotate || 0" :min="-180" :max="180" :show-button="false" @update:value="patchBlockProps(selectedBlock.id, { globalRotate: $event || 0 })" />
                      </div>
                    </n-form-item>
                    <n-form-item label="间隔 / 偏移">
                      <div class="style-grid four">
                        <n-input-number :value="selectedBlock.props?.xGap || 0" :min="0" :max="600" :show-button="false" @update:value="patchBlockProps(selectedBlock.id, { xGap: $event || 0 })" />
                        <n-input-number :value="selectedBlock.props?.yGap || 0" :min="0" :max="600" :show-button="false" @update:value="patchBlockProps(selectedBlock.id, { yGap: $event || 0 })" />
                        <n-input-number :value="selectedBlock.props?.xOffset || 0" :min="-600" :max="600" :show-button="false" @update:value="patchBlockProps(selectedBlock.id, { xOffset: $event || 0 })" />
                        <n-input-number :value="selectedBlock.props?.yOffset || 0" :min="-600" :max="600" :show-button="false" @update:value="patchBlockProps(selectedBlock.id, { yOffset: $event || 0 })" />
                      </div>
                    </n-form-item>
                    <n-form-item label="图片水印">
                      <div class="style-grid three">
                        <n-input :value="selectedBlock.props?.image || ''" clearable placeholder="图片 URL" @update:value="patchBlockProps(selectedBlock.id, { image: $event })" />
                        <n-input-number :value="selectedBlock.props?.imageWidth" :min="1" :max="600" :show-button="false" placeholder="图片宽" @update:value="patchBlockProps(selectedBlock.id, { imageWidth: $event || undefined })" />
                        <n-input-number :value="selectedBlock.props?.imageHeight" :min="1" :max="600" :show-button="false" placeholder="图片高" @update:value="patchBlockProps(selectedBlock.id, { imageHeight: $event || undefined })" />
                      </div>
                    </n-form-item>
                    <n-form-item label="开关">
                      <n-checkbox-group :value="resolveBooleanKeys(selectedBlock.props, ['cross', 'debug', 'fullscreen', 'selectable'])" @update:value="values => patchBlockProps(selectedBlock.id, { cross: values.includes('cross'), debug: values.includes('debug'), fullscreen: values.includes('fullscreen'), selectable: values.includes('selectable') })">
                        <n-space size="small">
                          <n-checkbox value="cross" label="跨边界" />
                          <n-checkbox value="debug" label="调试" />
                          <n-checkbox value="fullscreen" label="全屏" />
                          <n-checkbox value="selectable" label="内容可选中" />
                        </n-space>
                      </n-checkbox-group>
                    </n-form-item>
                  </template>
                  <template v-if="selectedBlock.blockType === 'html-tag'">
                    <n-form-item label="标签 / 角色 / 渲染模式">
                      <div class="style-grid three">
                        <n-select
                          :value="selectedBlock.props?.tagName || 'section'"
                          :options="htmlTagOptions"
                          filterable
                          tag
                          @update:value="patchBlockProps(selectedBlock.id, { tagName: $event || 'section' })"
                        />
                        <n-input
                          :value="selectedBlock.props?.semanticRole || ''"
                          clearable
                          placeholder="role"
                          @update:value="patchBlockProps(selectedBlock.id, { semanticRole: $event || '' })"
                        />
                        <n-select
                          :value="selectedBlock.props?.renderMode || 'html'"
                          :options="htmlRenderModeOptions"
                          @update:value="patchBlockProps(selectedBlock.id, { renderMode: $event || 'html' })"
                        />
                      </div>
                    </n-form-item>
                    <n-form-item label="属性 JSON">
                      <n-input
                        :value="selectedBlock.props?.attributesText || '{}'"
                        type="textarea"
                        :rows="4"
                        placeholder="{ &quot;class&quot;: &quot;notice&quot; }"
                        @update:value="patchBlockProps(selectedBlock.id, { attributesText: $event })"
                      />
                    </n-form-item>
                    <n-form-item label="文本 / HTML 内容">
                      <div class="metrics-editor">
                        <n-input
                          :value="selectedBlock.props?.textContent || ''"
                          clearable
                          placeholder="纯文本内容"
                          @update:value="patchBlockProps(selectedBlock.id, { textContent: $event })"
                        />
                        <n-input
                          :value="selectedBlock.props?.htmlContent || ''"
                          type="textarea"
                          :rows="6"
                          placeholder="HTML 内容"
                          @update:value="patchBlockProps(selectedBlock.id, { htmlContent: $event })"
                        />
                      </div>
                    </n-form-item>
                  </template>
                  <template v-if="selectedBlock.blockType === 'vue-component'">
                    <n-form-item label="组件名 / 预览模式">
                      <div class="style-grid two">
                        <n-input
                          :value="selectedBlock.props?.componentName || ''"
                          placeholder="组件名"
                          @update:value="patchBlockProps(selectedBlock.id, { componentName: $event })"
                        />
                        <n-select
                          :value="selectedBlock.props?.previewMode || 'safe-template'"
                          :options="vuePreviewModeOptions"
                          @update:value="patchBlockProps(selectedBlock.id, { previewMode: $event || 'safe-template', safeMode: $event === 'live' ? false : selectedBlock.props?.safeMode !== false })"
                        />
                      </div>
                    </n-form-item>
                    <n-form-item label="Template 代码">
                      <n-input
                        :value="selectedBlock.props?.templateCode || ''"
                        type="textarea"
                        :rows="7"
                        placeholder="<template>...</template>"
                        @update:value="patchBlockProps(selectedBlock.id, { templateCode: $event })"
                      />
                    </n-form-item>
                    <n-form-item label="Script 代码">
                      <n-input
                        :value="selectedBlock.props?.scriptCode || ''"
                        type="textarea"
                        :rows="6"
                        placeholder="export default { ... }"
                        @update:value="patchBlockProps(selectedBlock.id, { scriptCode: $event })"
                      />
                    </n-form-item>
                    <n-form-item label="Style 代码">
                      <n-input
                        :value="selectedBlock.props?.styleCode || ''"
                        type="textarea"
                        :rows="5"
                        placeholder=".custom-widget { ... }"
                        @update:value="patchBlockProps(selectedBlock.id, { styleCode: $event })"
                      />
                    </n-form-item>
                    <n-form-item label="Props JSON">
                      <n-input
                        :value="selectedBlock.props?.propsJson || '{}'"
                        type="textarea"
                        :rows="5"
                        placeholder="{ &quot;title&quot;: &quot;标题&quot; }"
                        @update:value="patchBlockProps(selectedBlock.id, { propsJson: $event })"
                      />
                    </n-form-item>
                  </template>
                  <template v-if="['calendar', 'code', 'countdown', 'descriptions', 'announcement', 'list', 'log', 'number-animation', 'breadcrumb', 'menu', 'pagination', 'split'].includes(selectedBlock.blockType)">
                    <n-form-item v-if="['code', 'log'].includes(selectedBlock.blockType)" label="内容">
                      <n-input
                        :value="selectedBlock.props?.code || selectedBlock.props?.log || ''"
                        type="textarea"
                        :rows="8"
                        placeholder="请输入内容"
                        @update:value="patchBlockProps(selectedBlock.id, selectedBlock.blockType === 'code' ? { code: $event } : { log: $event })"
                      />
                    </n-form-item>
                    <n-form-item v-if="['descriptions', 'list', 'breadcrumb'].includes(selectedBlock.blockType)" label="数据 JSON">
                      <n-input
                        :value="selectedBlock.props?.itemsText || '[]'"
                        type="textarea"
                        :rows="8"
                        placeholder="数组 JSON"
                        @update:value="patchBlockProps(selectedBlock.id, { itemsText: $event || '[]' })"
                      />
                    </n-form-item>
                    <n-form-item v-if="selectedBlock.blockType === 'menu'" label="菜单配置">
                      <div class="metrics-editor">
                        <n-input
                          :value="selectedBlock.props?.optionsText || '[]'"
                          type="textarea"
                          :rows="8"
                          placeholder="菜单 options JSON"
                          @update:value="patchBlockProps(selectedBlock.id, { optionsText: $event || '[]' })"
                        />
                        <div class="style-grid two">
                          <n-select :value="selectedBlock.props?.mode || 'vertical'" :options="menuModeOptions" @update:value="patchBlockProps(selectedBlock.id, { mode: $event || 'vertical' })" />
                          <n-input :value="selectedBlock.props?.value || ''" placeholder="当前 key" @update:value="patchBlockProps(selectedBlock.id, { value: $event || '' })" />
                        </div>
                      </div>
                    </n-form-item>
                    <n-form-item v-if="selectedBlock.blockType === 'announcement'" label="公示内容">
                      <div class="metrics-editor">
                        <n-input :value="selectedBlock.props?.content || ''" type="textarea" :rows="5" placeholder="公示内容" @update:value="patchBlockProps(selectedBlock.id, { content: $event })" />
                        <div class="style-grid three">
                          <n-select :value="selectedBlock.props?.type || 'info'" :options="alertTypeOptions" @update:value="patchBlockProps(selectedBlock.id, { type: $event || 'info' })" />
                          <n-switch :value="selectedBlock.props?.showIcon !== false" @update:value="patchBlockProps(selectedBlock.id, { showIcon: $event })" />
                          <n-switch :value="selectedBlock.props?.bordered !== false" @update:value="patchBlockProps(selectedBlock.id, { bordered: $event })" />
                        </div>
                      </div>
                    </n-form-item>
                    <n-form-item v-if="selectedBlock.blockType === 'countdown'" label="倒计时">
                      <div class="style-grid three">
                        <n-input-number :value="selectedBlock.props?.duration || 3600000" :min="1000" :max="86400000" :step="1000" :show-button="false" @update:value="patchBlockProps(selectedBlock.id, { duration: $event || 3600000 })" />
                        <n-input-number :value="selectedBlock.props?.precision || 0" :min="0" :max="3" :show-button="false" @update:value="patchBlockProps(selectedBlock.id, { precision: $event || 0 })" />
                        <n-switch :value="selectedBlock.props?.active !== false" @update:value="patchBlockProps(selectedBlock.id, { active: $event })" />
                      </div>
                    </n-form-item>
                    <n-form-item v-if="selectedBlock.blockType === 'number-animation'" label="数值动画">
                      <div class="style-grid four">
                        <n-input-number :value="selectedBlock.props?.from || 0" :show-button="false" @update:value="patchBlockProps(selectedBlock.id, { from: $event || 0 })" />
                        <n-input-number :value="selectedBlock.props?.to || 0" :show-button="false" @update:value="patchBlockProps(selectedBlock.id, { to: $event || 0 })" />
                        <n-input-number :value="selectedBlock.props?.duration || 1200" :min="100" :max="10000" :show-button="false" @update:value="patchBlockProps(selectedBlock.id, { duration: $event || 1200 })" />
                        <n-color-picker :value="selectedBlock.props?.color || '#2563eb'" :show-alpha="true" @update:value="patchBlockProps(selectedBlock.id, { color: $event || '#2563eb' })" />
                      </div>
                    </n-form-item>
                    <n-form-item v-if="selectedBlock.blockType === 'pagination'" label="分页">
                      <div class="style-grid four">
                        <n-input-number :value="selectedBlock.props?.page || 1" :min="1" :show-button="false" @update:value="patchBlockProps(selectedBlock.id, { page: $event || 1 })" />
                        <n-input-number :value="selectedBlock.props?.pageSize || 10" :min="1" :show-button="false" @update:value="patchBlockProps(selectedBlock.id, { pageSize: $event || 10 })" />
                        <n-input-number :value="selectedBlock.props?.itemCount || 0" :min="0" :show-button="false" @update:value="patchBlockProps(selectedBlock.id, { itemCount: $event || 0 })" />
                        <n-switch :value="selectedBlock.props?.simple === true" @update:value="patchBlockProps(selectedBlock.id, { simple: $event })" />
                      </div>
                    </n-form-item>
                    <n-form-item v-if="selectedBlock.blockType === 'split'" label="面板分隔">
                      <div class="metrics-editor">
                        <div class="style-grid four">
                          <n-select :value="selectedBlock.props?.direction || 'horizontal'" :options="splitDirectionOptions" @update:value="patchBlockProps(selectedBlock.id, { direction: $event || 'horizontal' })" />
                          <n-input-number :value="selectedBlock.props?.defaultSize || 0.38" :min="0.1" :max="0.9" :step="0.01" :show-button="false" @update:value="patchBlockProps(selectedBlock.id, { defaultSize: $event || 0.38 })" />
                          <n-input-number :value="selectedBlock.props?.min || 0.2" :min="0" :max="0.9" :step="0.01" :show-button="false" @update:value="patchBlockProps(selectedBlock.id, { min: $event || 0.2 })" />
                          <n-input-number :value="selectedBlock.props?.max || 0.8" :min="0.1" :max="1" :step="0.01" :show-button="false" @update:value="patchBlockProps(selectedBlock.id, { max: $event || 0.8 })" />
                        </div>
                        <n-input :value="selectedBlock.props?.pane1Content || ''" placeholder="面板 1 内容" @update:value="patchBlockProps(selectedBlock.id, { pane1Content: $event })" />
                        <n-input :value="selectedBlock.props?.pane2Content || ''" placeholder="面板 2 内容" @update:value="patchBlockProps(selectedBlock.id, { pane2Content: $event })" />
                      </div>
                    </n-form-item>
                  </template>
                </template>
                <template v-if="selectedBlock.blockType === 'signature-pad'">
                  <n-form-item label="签名配置">
                    <div class="style-grid three">
                      <n-input :value="selectedBlock.props?.title" placeholder="标题" @update:value="patchBlockProps(selectedBlock.id, { title: $event })" />
                      <n-input-number :value="selectedBlock.props?.height || 160" :min="80" :max="360" :show-button="false" placeholder="高度" @update:value="patchBlockProps(selectedBlock.id, { height: $event || 160 })" />
                      <n-input-number :value="selectedBlock.props?.strokeWidth || 2.6" :min="1" :max="8" :step="0.2" :show-button="false" placeholder="笔画" @update:value="patchBlockProps(selectedBlock.id, { strokeWidth: $event || 2.6 })" />
                    </div>
                  </n-form-item>
                  <n-form-item label="状态">
                    <n-checkbox-group :value="resolveBooleanKeys(selectedBlock.props, ['required', 'disabled'])" @update:value="values => patchBlockProps(selectedBlock.id, { required: values.includes('required'), disabled: values.includes('disabled') })">
                      <n-space vertical size="small">
                        <n-checkbox value="required" label="必填" />
                        <n-checkbox value="disabled" label="禁用签名" />
                      </n-space>
                    </n-checkbox-group>
                  </n-form-item>
                </template>
                <template v-if="selectedBlock.blockType === 'transfer'">
                  <n-form-item label="标题 / 分栏">
                    <div class="style-grid three">
                      <n-input :value="selectedBlock.props?.title" placeholder="标题" @update:value="patchBlockProps(selectedBlock.id, { title: $event })" />
                      <n-input :value="selectedBlock.props?.sourceTitle" placeholder="左侧标题" @update:value="patchBlockProps(selectedBlock.id, { sourceTitle: $event })" />
                      <n-input :value="selectedBlock.props?.targetTitle" placeholder="右侧标题" @update:value="patchBlockProps(selectedBlock.id, { targetTitle: $event })" />
                    </div>
                  </n-form-item>
                  <n-form-item label="数据来源">
                    <div class="style-grid two">
                      <n-select
                        :value="selectedBlock.props?.dataSourceType || 'static'"
                        :options="transferDataSourceOptions"
                        @update:value="patchBlockProps(selectedBlock.id, { dataSourceType: $event || 'static' })"
                      />
                      <n-checkbox-group
                        :value="resolveBooleanKeys(selectedBlock.props, ['filterable', 'virtualScroll', 'disabled'])"
                        @update:value="values => patchBlockProps(selectedBlock.id, { filterable: values.includes('filterable'), virtualScroll: values.includes('virtualScroll'), disabled: values.includes('disabled') })"
                      >
                        <n-space size="small">
                          <n-checkbox value="filterable" label="可搜索" />
                          <n-checkbox value="virtualScroll" label="虚拟滚动" />
                          <n-checkbox value="disabled" label="禁用" />
                        </n-space>
                      </n-checkbox-group>
                    </div>
                  </n-form-item>
                  <n-form-item v-if="selectedBlock.props?.dataSourceType === 'remote'" label="远程接口">
                    <div class="metrics-editor">
                      <div class="style-grid two">
                        <n-input
                          :value="selectedBlock.props?.optionSource?.api || ''"
                          placeholder="例如 get@/api/system/user/options"
                          @update:value="updateOptionSource({ api: $event })"
                        />
                        <n-select
                          :value="selectedBlock.props?.optionSource?.method || 'get'"
                          :options="requestMethodOptions"
                          @update:value="updateOptionSource({ method: $event || 'get' })"
                        />
                      </div>
                      <div class="style-grid three">
                        <n-input
                          :value="selectedBlock.props?.optionSource?.recordsField || 'records'"
                          placeholder="列表路径"
                          @update:value="updateOptionSource({ recordsField: $event || 'records' })"
                        />
                        <n-input
                          :value="selectedBlock.props?.optionSource?.labelField || 'label'"
                          placeholder="显示字段"
                          @update:value="updateOptionSource({ labelField: $event || 'label' })"
                        />
                        <n-input
                          :value="selectedBlock.props?.optionSource?.valueField || 'value'"
                          placeholder="值字段"
                          @update:value="updateOptionSource({ valueField: $event || 'value' })"
                        />
                      </div>
                      <div class="style-grid two">
                        <n-input
                          :value="selectedBlock.props?.optionSource?.disabledField || 'disabled'"
                          placeholder="禁用字段"
                          @update:value="updateOptionSource({ disabledField: $event || 'disabled' })"
                        />
                        <n-input
                          :value="selectedBlock.props?.optionSource?.paramsText || '{}'"
                          placeholder="请求参数 JSON"
                          @update:value="updateOptionSource({ paramsText: $event || '{}' })"
                        />
                      </div>
                    </div>
                  </n-form-item>
                  <n-form-item v-else label="静态选项">
                    <div class="metrics-editor">
                      <div v-for="(option, idx) in (selectedBlock.props?.options || [])" :key="idx" class="metric-row">
                        <n-input :value="option.label" size="small" placeholder="显示名" @update:value="updateOptionItem('options', idx, { label: $event })" />
                        <n-input :value="option.value" size="small" placeholder="值" @update:value="updateOptionItem('options', idx, { value: $event })" />
                        <n-button size="tiny" quaternary @click="removeOptionItem('options', idx)">
                          删
                        </n-button>
                      </div>
                      <n-button size="small" dashed block @click="addOptionItem('options')">
                        + 添加选项
                      </n-button>
                    </div>
                  </n-form-item>
                </template>
                <template v-if="selectedBlock.blockType === 'step-form'">
                  <n-form-item label="标题 / 当前步骤 / 方向">
                    <div class="style-grid three">
                      <n-input :value="selectedBlock.props?.title" placeholder="标题" @update:value="patchBlockProps(selectedBlock.id, { title: $event })" />
                      <n-input-number :value="selectedBlock.props?.current || 1" :min="1" :max="10" :show-button="false" @update:value="patchBlockProps(selectedBlock.id, { current: $event || 1 })" />
                      <n-select :value="selectedBlock.props?.direction || 'horizontal'" :options="directionOptions" @update:value="patchBlockProps(selectedBlock.id, { direction: $event || 'horizontal' })" />
                    </div>
                  </n-form-item>
                  <n-form-item label="表单字段">
                    <n-button size="small" type="primary" secondary @click="openFieldDrawer('table')">
                      配置字段（{{ selectedBlock.fieldRefs?.length || 0 }}/{{ fields.length }}）
                    </n-button>
                  </n-form-item>
                </template>
                <template v-if="selectedBlock.blockType === 'paragraph'">
                  <n-form-item label="段落内容">
                    <n-input
                      :value="selectedBlock.props?.content"
                      type="textarea"
                      :rows="5"
                      placeholder="输入段落文字"
                      @update:value="patchBlockProps(selectedBlock.id, { content: $event })"
                    />
                  </n-form-item>
                </template>
                <template v-if="selectedBlock.blockType === 'text-title'">
                  <n-form-item label="标题文本">
                    <div class="metrics-editor">
                      <n-input :value="selectedBlock.props?.text" placeholder="标题" @update:value="patchBlockProps(selectedBlock.id, { text: $event })" />
                      <n-input :value="selectedBlock.props?.subtitle" placeholder="副标题" @update:value="patchBlockProps(selectedBlock.id, { subtitle: $event })" />
                    </div>
                  </n-form-item>
                  <n-form-item label="层级 / 字重 / 对齐 / 颜色">
                    <div class="style-grid four">
                      <n-input-number :value="selectedBlock.props?.level || 2" :min="1" :max="6" :show-button="false" @update:value="patchBlockProps(selectedBlock.id, { level: $event || 2 })" />
                      <n-input-number :value="selectedBlock.props?.weight || 800" :min="300" :max="900" :step="100" :show-button="false" @update:value="patchBlockProps(selectedBlock.id, { weight: $event || 800 })" />
                      <n-select :value="selectedBlock.props?.align || 'left'" :options="alignOptions" @update:value="patchBlockProps(selectedBlock.id, { align: $event || 'left' })" />
                      <n-color-picker :value="selectedBlock.props?.color || '#0f172a'" :show-alpha="true" @update:value="patchBlockProps(selectedBlock.id, { color: $event || '#0f172a' })" />
                    </div>
                  </n-form-item>
                </template>
                <template v-if="selectedBlock.blockType === 'statistic'">
                  <n-form-item label="统计内容">
                    <div class="style-grid two">
                      <n-input :value="selectedBlock.props?.title" placeholder="标题" @update:value="patchBlockProps(selectedBlock.id, { title: $event })" />
                      <n-input :value="selectedBlock.props?.value" placeholder="数值" @update:value="patchBlockProps(selectedBlock.id, { value: $event })" />
                      <n-input :value="selectedBlock.props?.prefix" placeholder="前缀" @update:value="patchBlockProps(selectedBlock.id, { prefix: $event })" />
                      <n-input :value="selectedBlock.props?.suffix" placeholder="后缀" @update:value="patchBlockProps(selectedBlock.id, { suffix: $event })" />
                      <n-input :value="selectedBlock.props?.trend" placeholder="趋势" @update:value="patchBlockProps(selectedBlock.id, { trend: $event })" />
                      <n-color-picker :value="selectedBlock.props?.color || '#2563eb'" :show-alpha="true" @update:value="patchBlockProps(selectedBlock.id, { color: $event || '#2563eb' })" />
                    </div>
                  </n-form-item>
                </template>
                <template v-if="['link', 'audio-player', 'video-player', 'iframe'].includes(selectedBlock.blockType)">
                  <n-form-item label="地址配置">
                    <div class="metrics-editor">
                      <n-input :value="selectedBlock.props?.title || selectedBlock.props?.text" placeholder="标题/文本" @update:value="updateLinkLikeTitle($event)" />
                      <n-input :value="selectedBlock.props?.src || selectedBlock.props?.href" placeholder="URL 地址" @update:value="updateLinkLikeUrl($event)" />
                      <n-input v-if="selectedBlock.blockType === 'video-player'" :value="selectedBlock.props?.poster" placeholder="封面 poster" @update:value="patchBlockProps(selectedBlock.id, { poster: $event })" />
                    </div>
                  </n-form-item>
                </template>
                <template v-if="selectedBlock.blockType === 'avatar'">
                  <n-form-item label="头像信息">
                    <div class="style-grid two">
                      <n-input :value="selectedBlock.props?.name" placeholder="名称" @update:value="patchBlockProps(selectedBlock.id, { name: $event })" />
                      <n-input :value="selectedBlock.props?.description" placeholder="描述" @update:value="patchBlockProps(selectedBlock.id, { description: $event })" />
                      <n-input :value="selectedBlock.props?.src" placeholder="头像 URL" @update:value="patchBlockProps(selectedBlock.id, { src: $event })" />
                      <n-input-number :value="selectedBlock.props?.size || 48" :min="24" :max="120" :show-button="false" @update:value="patchBlockProps(selectedBlock.id, { size: $event || 48 })" />
                    </div>
                  </n-form-item>
                </template>
                <template v-if="['barcode', 'qrcode'].includes(selectedBlock.blockType)">
                  <n-form-item label="编码内容">
                    <div class="style-grid two">
                      <n-input :value="selectedBlock.props?.value" placeholder="编码内容" @update:value="patchBlockProps(selectedBlock.id, { value: $event })" />
                      <n-color-picker :value="selectedBlock.props?.foreground || selectedBlock.props?.lineColor || '#0f172a'" :show-alpha="true" @update:value="updateCodeColor($event)" />
                    </div>
                  </n-form-item>
                  <template v-if="selectedBlock.blockType === 'barcode'">
                    <n-form-item label="条码格式 / 尺寸">
                      <div class="style-grid four">
                        <n-select :value="selectedBlock.props?.format || 'CODE128'" :options="barcodeFormatOptions" @update:value="patchBlockProps(selectedBlock.id, { format: $event || 'CODE128' })" />
                        <n-input-number :value="selectedBlock.props?.barWidth || 2" :min="1" :max="8" :show-button="false" @update:value="patchBlockProps(selectedBlock.id, { barWidth: $event || 2 })" />
                        <n-input-number :value="selectedBlock.props?.barHeight || 72" :min="24" :max="240" :show-button="false" @update:value="patchBlockProps(selectedBlock.id, { barHeight: $event || 72 })" />
                        <n-input-number :value="selectedBlock.props?.margin ?? 8" :min="0" :max="80" :show-button="false" @update:value="patchBlockProps(selectedBlock.id, { margin: $event ?? 8 })" />
                      </div>
                    </n-form-item>
                    <n-form-item label="文字">
                      <div class="style-grid two">
                        <n-input-number :value="selectedBlock.props?.fontSize || 14" :min="8" :max="36" :show-button="false" @update:value="patchBlockProps(selectedBlock.id, { fontSize: $event || 14 })" />
                        <n-switch :value="selectedBlock.props?.showText !== false" @update:value="patchBlockProps(selectedBlock.id, { showText: $event })" />
                      </div>
                    </n-form-item>
                  </template>
                  <template v-if="selectedBlock.blockType === 'qrcode'">
                    <n-form-item label="二维码尺寸 / 纠错">
                      <div class="style-grid three">
                        <n-input-number :value="selectedBlock.props?.size || 132" :min="64" :max="480" :show-button="false" @update:value="patchBlockProps(selectedBlock.id, { size: $event || 132 })" />
                        <n-input-number :value="selectedBlock.props?.margin || 0" :min="0" :max="80" :show-button="false" @update:value="patchBlockProps(selectedBlock.id, { margin: $event || 0 })" />
                        <n-select :value="selectedBlock.props?.errorCorrectionLevel || 'Q'" :options="qrcodeErrorCorrectionOptions" @update:value="patchBlockProps(selectedBlock.id, { errorCorrectionLevel: $event || 'Q' })" />
                      </div>
                    </n-form-item>
                    <n-form-item label="点样式 / 角样式">
                      <div class="style-grid three">
                        <n-select :value="selectedBlock.props?.dotsType || 'square'" :options="qrcodeDotsTypeOptions" @update:value="patchBlockProps(selectedBlock.id, { dotsType: $event || 'square' })" />
                        <n-select :value="selectedBlock.props?.cornersSquareType || 'square'" :options="qrcodeCornerTypeOptions" @update:value="patchBlockProps(selectedBlock.id, { cornersSquareType: $event || 'square' })" />
                        <n-select :value="selectedBlock.props?.cornersDotType || 'square'" :options="qrcodeCornerTypeOptions" @update:value="patchBlockProps(selectedBlock.id, { cornersDotType: $event || 'square' })" />
                      </div>
                    </n-form-item>
                    <n-form-item label="背景 / 角颜色 / 显示文本">
                      <div class="style-grid three">
                        <n-color-picker :value="selectedBlock.props?.background || 'transparent'" :show-alpha="true" @update:value="patchBlockProps(selectedBlock.id, { background: $event || 'transparent' })" />
                        <n-color-picker :value="selectedBlock.props?.cornerColor || selectedBlock.props?.foreground || '#0f172a'" :show-alpha="true" @update:value="patchBlockProps(selectedBlock.id, { cornerColor: $event || '#0f172a' })" />
                        <n-switch :value="selectedBlock.props?.showText !== false" @update:value="patchBlockProps(selectedBlock.id, { showText: $event })" />
                      </div>
                    </n-form-item>
                  </template>
                </template>
                <template v-if="selectedBlock.blockType === 'descriptions'">
                  <n-form-item label="描述项">
                    <div class="metrics-editor">
                      <div v-for="(item, idx) in (selectedBlock.props?.items || [])" :key="idx" class="metric-row">
                        <n-input :value="item.label" size="small" placeholder="标签" @update:value="updateOptionItem('items', idx, { label: $event })" />
                        <n-input :value="item.value" size="small" placeholder="内容" @update:value="updateOptionItem('items', idx, { value: $event })" />
                        <n-button size="tiny" quaternary @click="removeOptionItem('items', idx)">
                          删
                        </n-button>
                      </div>
                      <n-button size="small" dashed block @click="addOptionItem('items')">
                        + 添加描述项
                      </n-button>
                    </div>
                  </n-form-item>
                </template>
                <template v-if="selectedBlock.blockType === 'box-layout'">
                  <n-form-item label="盒子布局">
                    <div class="style-grid two">
                      <n-select :value="selectedBlock.props?.direction || 'row'" :options="directionOptions" @update:value="patchBlockProps(selectedBlock.id, { direction: $event || 'row' })" />
                      <n-input-number :value="selectedBlock.props?.gap ?? 12" :min="0" :max="80" :show-button="false" @update:value="patchBlockProps(selectedBlock.id, { gap: $event ?? 12 })" />
                      <n-select :value="selectedBlock.props?.alignItems || 'stretch'" :options="gridVerticalAlignOptions" @update:value="patchBlockProps(selectedBlock.id, { alignItems: $event || 'stretch' })" />
                      <n-select :value="selectedBlock.props?.justifyContent || 'flex-start'" :options="justifyContentOptions" @update:value="patchBlockProps(selectedBlock.id, { justifyContent: $event || 'flex-start' })" />
                    </div>
                  </n-form-item>
                </template>
                <template v-if="selectedBlock.blockType === 'space'">
                  <n-form-item label="间距设置">
                    <div class="style-grid three">
                      <n-select :value="selectedBlock.props?.direction || 'vertical'" :options="directionOptions" @update:value="patchBlockProps(selectedBlock.id, { direction: $event || 'vertical' })" />
                      <n-input-number :value="selectedBlock.props?.size || 24" :min="1" :max="200" :show-button="false" @update:value="patchBlockProps(selectedBlock.id, { size: $event || 24 })" />
                      <n-switch :value="!!selectedBlock.props?.lineVisible" @update:value="patchBlockProps(selectedBlock.id, { lineVisible: $event })" />
                    </div>
                  </n-form-item>
                </template>
              </template>
            </div>
          </n-form>
        </div>
      </div>
    </aside>

    <n-drawer v-model:show="fieldDrawerOpen" :width="680" placement="right">
      <n-drawer-content :title="`配置${selectedFieldDrawerTitle} · ${selectedBlockMeta?.title || ''}`" closable>
        <div v-if="selectedBlock" class="field-config">
          <div class="field-config-section">
            <div class="section-title">
              已选{{ selectedFieldDrawerTitle }} ({{ selectedFieldRefs.length || 0 }})
            </div>
            <draggable
              :model-value="selectedFieldsList"
              item-key="field"
              handle=".f-handle"
              :animation="160"
              class="selected-list"
              @update:model-value="handleSelectedReorder"
            >
              <template #item="{ element }">
                <div
                  class="selected-row"
                  :class="{
                    search: selectedBlockZoneKey === 'search',
                    table: selectedBlockZoneKey !== 'search',
                    active: activeDrawerField?.field === element.field,
                  }"
                  @click="selectDrawerField(element.field)"
                >
                  <span class="f-handle">☰</span>
                  <span class="f-name">
                    {{ element.label || element.field }}
                    <small v-if="element.sourceLabel || element.modelName">{{ element.sourceLabel || element.modelName }}</small>
                  </span>
                  <span class="f-code">{{ element.field }}</span>
                  <button type="button" class="f-remove" title="移除字段" @click.stop="toggleField(element.field, false)">
                    ×
                  </button>
                  <div v-if="selectedBlockZoneKey === 'search'" class="field-setting-row search-setting-row">
                    <n-select
                      :value="resolveFieldSetting(element.field).queryType || element.queryType || 'like'"
                      size="tiny"
                      :options="queryTypeOptions"
                      placeholder="查询方式"
                      @update:value="updateFieldSetting(element.field, { queryType: $event })"
                    />
                    <n-select
                      :value="resolveFieldSetting(element.field).componentType || resolveDefaultSearchComponentType(element)"
                      size="tiny"
                      :options="searchComponentOptions"
                      placeholder="查询组件"
                      @update:value="updateFieldSetting(element.field, { componentType: $event })"
                    />
                    <n-select
                      :value="resolveFieldSetting(element.field).queryField || element.field"
                      size="tiny"
                      :options="queryFieldOptions"
                      filterable
                      placeholder="映射字段"
                      @update:value="updateFieldSetting(element.field, { queryField: $event })"
                    />
                    <n-select
                      :value="resolveFieldSetting(element.field).align || 'left'"
                      size="tiny"
                      :options="alignOptions"
                      placeholder="对齐"
                      @update:value="updateFieldSetting(element.field, { align: $event || 'left' })"
                    />
                  </div>
                  <div v-if="selectedBlockZoneKey !== 'search' && ['data-table', 'AiCrudPage', 'AiTable', 'AiForm', 'detail-info'].includes(selectedBlock.blockType)" class="field-setting-row table-setting-row">
                    <n-select
                      :value="resolveFieldSetting(element.field).renderType || resolveDefaultTableRenderType(element)"
                      size="tiny"
                      :options="tableRenderOptions"
                      placeholder="渲染方式"
                      @update:value="updateFieldSetting(element.field, { renderType: $event })"
                    />
                    <n-select
                      :value="resolveFieldSetting(element.field).align || 'left'"
                      size="tiny"
                      :options="alignOptions"
                      placeholder="对齐"
                      @update:value="updateFieldSetting(element.field, { align: $event || 'left' })"
                    />
                    <n-select
                      v-if="isNameRenderType(resolveFieldSetting(element.field).renderType || resolveDefaultTableRenderType(element))"
                      :value="resolveFieldSetting(element.field).targetField || `${element.field}Name`"
                      size="tiny"
                      :options="renderTargetFieldOptions(element)"
                      filterable
                      tag
                      placeholder="名称字段"
                      @update:value="updateFieldSetting(element.field, { targetField: $event })"
                    />
                  </div>
                  <div v-if="selectedBlockZoneKey !== 'search' && ['data-table', 'AiCrudPage', 'AiTable'].includes(selectedBlock.blockType)" class="field-setting-row column-link-row">
                    <label class="field-setting-control">
                      <span>文字颜色</span>
                      <n-color-picker
                        :value="resolveFieldSetting(element.field).textColor || ''"
                        size="small"
                        :show-alpha="true"
                        placeholder="文字颜色"
                        @update:value="updateFieldSetting(element.field, { textColor: $event || '' })"
                      />
                    </label>
                    <label class="field-setting-control">
                      <span>点击动作</span>
                      <n-select
                        :value="resolveFieldSetting(element.field).clickAction || 'none'"
                        size="tiny"
                        :options="columnClickActionOptions"
                        placeholder="点击动作"
                        @update:value="updateFieldSetting(element.field, { clickAction: $event || 'none' })"
                      />
                    </label>
                    <label v-if="resolveFieldSetting(element.field).clickAction === 'navigate'" class="field-setting-control">
                      <span>目标页面</span>
                      <n-select
                        :value="resolveFieldSetting(element.field).targetPageKey || ''"
                        size="tiny"
                        :options="pageTargetOptions"
                        filterable
                        placeholder="目标页面"
                        @update:value="updateFieldSetting(element.field, { targetPageKey: $event || '' })"
                      />
                    </label>
                    <label v-if="resolveFieldSetting(element.field).clickAction === 'navigate' && formTargetOptions.length" class="field-setting-control">
                      <span>目标表单</span>
                      <n-select
                        :value="resolveFieldSetting(element.field).targetFormKey || ''"
                        size="tiny"
                        :options="formTargetOptions"
                        clearable
                        filterable
                        placeholder="默认表单"
                        @update:value="updateFieldSetting(element.field, { targetFormKey: $event || '' })"
                      />
                    </label>
                    <label v-if="resolveFieldSetting(element.field).clickAction === 'navigate'" class="field-setting-control">
                      <span>参数名</span>
                      <n-input
                        :value="resolveFieldSetting(element.field).targetParamName || 'id'"
                        size="tiny"
                        placeholder="参数名"
                        @update:value="updateFieldSetting(element.field, { targetParamName: normalizeParamName($event) || 'id' })"
                      />
                    </label>
                    <label v-if="resolveFieldSetting(element.field).clickAction === 'navigate'" class="field-setting-control">
                      <span>取值字段</span>
                      <n-select
                        :value="resolveFieldSetting(element.field).targetParamField || 'id'"
                        size="tiny"
                        :options="queryFieldOptions"
                        filterable
                        placeholder="取值字段"
                        @update:value="updateFieldSetting(element.field, { targetParamField: $event || 'id' })"
                      />
                    </label>
                    <div v-if="resolveFieldSetting(element.field).clickAction === 'navigate'" class="field-help">
                      点击当前列后跳到目标页面，默认传参：id = 当前行 id。参数名是目标页面接收的名字，取值字段是从当前行取哪个字段。
                    </div>
                  </div>
                </div>
              </template>
            </draggable>
            <div v-if="!selectedFieldRefs.length" class="empty">
              当前没有选择字段
            </div>
            <div v-if="activeDrawerField" class="field-detail-card">
              <div class="field-detail-head">
                <div class="field-detail-title">
                  <strong>{{ activeDrawerField.label || activeDrawerField.field }}</strong>
                  <span>{{ activeDrawerField.sourceField || activeDrawerField.field }}</span>
                </div>
                <div class="field-role-switches">
                  <label>
                    <span>查询</span>
                    <n-switch
                      size="small"
                      :value="resolveFieldRoleEnabled(activeDrawerField.field, 'search')"
                      :disabled="selectedBlock.blockType !== 'AiCrudPage'"
                      @update:value="updateFieldRole(activeDrawerField.field, 'search', $event)"
                    />
                  </label>
                  <label>
                    <span>表格列</span>
                    <n-switch
                      size="small"
                      :value="resolveFieldRoleEnabled(activeDrawerField.field, 'table')"
                      @update:value="updateFieldRole(activeDrawerField.field, 'table', $event)"
                    />
                  </label>
                  <label>
                    <span>编辑</span>
                    <n-switch
                      size="small"
                      :value="resolveFieldRoleEnabled(activeDrawerField.field, 'edit')"
                      @update:value="updateFieldRole(activeDrawerField.field, 'edit', $event)"
                    />
                  </label>
                  <label v-if="selectedBlock.blockType === 'AiCrudPage'">
                    <span>导入</span>
                    <n-switch
                      size="small"
                      :value="resolveFieldRoleEnabled(activeDrawerField.field, 'import')"
                      @update:value="updateFieldRole(activeDrawerField.field, 'import', $event)"
                    />
                  </label>
                  <label v-if="selectedBlock.blockType === 'AiCrudPage'">
                    <span>导出</span>
                    <n-switch
                      size="small"
                      :value="resolveFieldRoleEnabled(activeDrawerField.field, 'export')"
                      @update:value="updateFieldRole(activeDrawerField.field, 'export', $event)"
                    />
                  </label>
                </div>
              </div>
              <div class="field-detail-grid">
                <label class="field-detail-control">
                  <span>列标题</span>
                  <n-input
                    :value="activeDrawerFieldSetting.title || activeDrawerField.label || activeDrawerField.field"
                    size="small"
                    @update:value="updateFieldSetting(activeDrawerField.field, { title: $event || '' })"
                  />
                </label>
                <label class="field-detail-control">
                  <span>列宽</span>
                  <n-input
                    :value="activeDrawerFieldSetting.width || ''"
                    size="small"
                    placeholder="auto / px"
                    @update:value="updateFieldSetting(activeDrawerField.field, { width: $event || '' })"
                  />
                </label>
                <label class="field-detail-control">
                  <span>对齐</span>
                  <n-select
                    :value="activeDrawerFieldSetting.align || 'left'"
                    size="small"
                    :options="alignOptions"
                    @update:value="updateFieldSetting(activeDrawerField.field, { align: $event || 'left' })"
                  />
                </label>
                <label class="field-detail-control">
                  <span>固定</span>
                  <n-select
                    :value="activeDrawerFieldSetting.fixed || ''"
                    size="small"
                    :options="fixedColumnOptions"
                    @update:value="updateFieldSetting(activeDrawerField.field, { fixed: $event || '' })"
                  />
                </label>
              </div>
              <div class="field-detail-footer">
                <div class="field-detail-toggles">
                  <label>
                    <span>省略</span>
                    <n-switch
                      size="small"
                      :value="activeDrawerFieldSetting.ellipsis !== false"
                      @update:value="updateFieldSetting(activeDrawerField.field, { ellipsis: $event })"
                    />
                  </label>
                  <label>
                    <span>排序</span>
                    <n-switch
                      size="small"
                      :value="!!activeDrawerFieldSetting.sortable"
                      @update:value="updateFieldSetting(activeDrawerField.field, { sortable: $event })"
                    />
                  </label>
                </div>
                <n-button size="tiny" secondary @click="fieldAdvancedOpen = !fieldAdvancedOpen">
                  {{ fieldAdvancedOpen ? '收起配置' : '更多字段配置' }}
                </n-button>
              </div>
              <div v-if="fieldAdvancedOpen" class="field-advanced-panel">
                <template v-if="selectedBlockZoneKey === 'search'">
                  <label class="field-detail-control">
                    <span>查询方式</span>
                    <n-select
                      :value="activeDrawerFieldSetting.queryType || activeDrawerField.queryType || 'like'"
                      size="small"
                      :options="queryTypeOptions"
                      @update:value="updateFieldSetting(activeDrawerField.field, { queryType: $event })"
                    />
                  </label>
                  <label class="field-detail-control">
                    <span>查询组件</span>
                    <n-select
                      :value="activeDrawerFieldSetting.componentType || resolveDefaultSearchComponentType(activeDrawerField)"
                      size="small"
                      :options="searchComponentOptions"
                      @update:value="updateFieldSetting(activeDrawerField.field, { componentType: $event })"
                    />
                  </label>
                  <label class="field-detail-control">
                    <span>映射字段</span>
                    <n-select
                      :value="activeDrawerFieldSetting.queryField || activeDrawerField.field"
                      size="small"
                      :options="queryFieldOptions"
                      filterable
                      @update:value="updateFieldSetting(activeDrawerField.field, { queryField: $event })"
                    />
                  </label>
                </template>
                <template v-else>
                  <label class="field-detail-control">
                    <span>渲染方式</span>
                    <n-select
                      :value="activeDrawerFieldSetting.renderType || resolveDefaultTableRenderType(activeDrawerField)"
                      size="small"
                      :options="tableRenderOptions"
                      @update:value="updateFieldSetting(activeDrawerField.field, { renderType: $event })"
                    />
                  </label>
                  <label v-if="isNameRenderType(activeDrawerFieldSetting.renderType || resolveDefaultTableRenderType(activeDrawerField))" class="field-detail-control">
                    <span>名称字段</span>
                    <n-select
                      :value="activeDrawerFieldSetting.targetField || `${activeDrawerField.field}Name`"
                      size="small"
                      :options="renderTargetFieldOptions(activeDrawerField)"
                      filterable
                      tag
                      @update:value="updateFieldSetting(activeDrawerField.field, { targetField: $event })"
                    />
                  </label>
                  <label class="field-detail-control">
                    <span>点击动作</span>
                    <n-select
                      :value="activeDrawerFieldSetting.clickAction || 'none'"
                      size="small"
                      :options="columnClickActionOptions"
                      @update:value="updateFieldSetting(activeDrawerField.field, { clickAction: $event || 'none' })"
                    />
                  </label>
                  <label class="field-detail-control">
                    <span>文字颜色</span>
                    <n-color-picker
                      :value="activeDrawerFieldSetting.textColor || ''"
                      size="small"
                      :show-alpha="true"
                      @update:value="updateFieldSetting(activeDrawerField.field, { textColor: $event || '' })"
                    />
                  </label>
                </template>
              </div>
            </div>
          </div>
          <div class="field-config-section">
            <div class="section-title">
              可选字段
            </div>
            <div class="available-list">
              <button
                v-for="field in availableFields"
                :key="field.field"
                type="button"
                class="available-item"
                @click="toggleField(field.field, true)"
              >
                <span>{{ field.label || field.field }}</span>
                <small v-if="field.sourceLabel || field.modelName">{{ field.sourceLabel || field.modelName }}</small>
              </button>
              <span v-if="!availableFields.length" class="empty">所有字段已选择</span>
            </div>
          </div>
        </div>
      </n-drawer-content>
    </n-drawer>

    <n-modal v-model:show="customActionModalOpen" :mask-closable="false">
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

                <n-form-item v-else-if="!isApiCustomAction(activeAction)" label="目标地址 / 表单">
                  <div class="action-route-panel">
                    <div v-if="isRouteCustomAction(activeAction)" class="action-config-tip">
                      <strong>站内跳转</strong>
                      <span>优先选择系统菜单中已经配置的页面，会自动填入目标地址；未配置菜单时可以直接手工输入路由。</span>
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

<script setup>
import {
  AddOutline,
  AlbumsOutline,
  AlertCircleOutline,
  AnalyticsOutline,
  BrowsersOutline,
  CalendarOutline,
  ChevronBackOutline,
  ChevronForwardOutline,
  CodeSlashOutline,
  ColorPaletteOutline,
  ContractOutline,
  DesktopOutline,
  DocumentTextOutline,
  EllipsisHorizontalOutline,
  ExpandOutline,
  EyeOutline,
  FlashOutline,
  GridOutline,
  ListOutline,
  MenuOutline,
  NavigateOutline,
  PhonePortraitOutline,
  PricetagOutline,
  QrCodeOutline,
  ReaderOutline,
  RemoveOutline,
  ReorderThreeOutline,
  ResizeOutline,
  SearchOutline,
  SettingsOutline,
  StatsChartOutline,
  SwapHorizontalOutline,
  TabletLandscapeOutline,
  TerminalOutline,
  TextOutline,
  TimerOutline,
  ToggleOutline,
} from '@vicons/ionicons5'
import { computed, h, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import draggable from 'vuedraggable'
import { enabledApiConfigs } from '@/api/business-app'
import { pageWidgetComponentKeys } from '@/components/lowcode-builder/shared/page-widget-schema'
import RuntimeRulesEditor from '@/components/lowcode-builder/shared/RuntimeRulesEditor.vue'
import { request } from '@/utils/http'
import CrudDefaultParamsEditor from './CrudDefaultParamsEditor.vue'
import CrudHookRulesEditor from './CrudHookRulesEditor.vue'
import GridBlockRenderer from './GridBlockRenderer.vue'
import {
  createDefaultBlockStyle,
  createDefaultListGridLayout,
  createGridBlock,
  isPageFieldVisible,
  LIST_PAGE_DESIGN_WIDTH,
  LIST_PAGE_GRID_COLS,
  listPageBlockCatalog,
  resolveDefaultTreeConfig,
  resolveListPageBlockMeta,
  resolveTreeFieldOptions,
  resolveTreeSourceRefs,
  syncGridLayoutWithModel,
} from './page-schema'

const props = defineProps({
  modelValue: {
    type: Object,
    required: true,
  },
  fields: {
    type: Array,
    default: () => [],
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
  panelOnly: {
    type: Boolean,
    default: false,
  },
  activeBlockId: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['update:modelValue', 'update:customActions'])

const rowHeight = 32
const gap = 8
const previewMinWidth = 360
const TREE_PANEL_COLLAPSED_WIDTH = 44

function createBitableSvgIcon(name, children = []) {
  return {
    name,
    render() {
      return h(
        'svg',
        {
          width: '1em',
          height: '1em',
          viewBox: '0 0 24 24',
          fill: 'none',
          xmlns: 'http://www.w3.org/2000/svg',
        },
        children.map((child, index) => h(child.tag || 'path', { key: index, ...child })),
      )
    },
  }
}

const BitableDragIcon = createBitableSvgIcon('BitableDragIcon', [
  { d: 'M8.25 6.5a1.75 1.75 0 1 0 0-3.5 1.75 1.75 0 0 0 0 3.5Zm0 7.25a1.75 1.75 0 1 0 0-3.5 1.75 1.75 0 0 0 0 3.5Zm1.75 5.5a1.75 1.75 0 1 1-3.5 0 1.75 1.75 0 0 1 3.5 0ZM14.753 6.5a1.75 1.75 0 1 0 0-3.5 1.75 1.75 0 0 0 0 3.5ZM16.5 12a1.75 1.75 0 1 1-3.5 0 1.75 1.75 0 0 1 3.5 0Zm-1.747 9a1.75 1.75 0 1 0 0-3.5 1.75 1.75 0 0 0 0 3.5Z', fill: 'currentColor' },
])
const BitableStyleIcon = createBitableSvgIcon('BitableStyleIcon', [
  { d: 'M8.437 4.898 5.447 13h6.063L8.437 4.898Zm6.025 15.881L12.269 15h-7.56l-2.131 5.78a1 1 0 1 1-1.873-.703L7.02 2.982c.491-1.31 2.344-1.31 2.835 0l6.48 17.095a1 1 0 1 1-1.872.702ZM15.056 5a1 1 0 1 0 0 2H23a1 1 0 1 0 0-2h-7.944Zm1.055 7a1 1 0 0 1 1-1H23a1 1 0 1 1 0 2h-5.89a1 1 0 0 1-1-1Zm3.056 5a1 1 0 1 0 0 2H23a1 1 0 1 0 0-2h-3.833Z', fill: 'currentColor' },
])
const BitableSelectIcon = createBitableSvgIcon('BitableSelectIcon', [
  { d: 'M7.755 11.658a1 1 0 0 1 1.416-1.415L12 13.07l2.828-2.829a1 1 0 0 1 1.416 1.416c-1.181 1.189-2.356 2.386-3.553 3.56a.987.987 0 0 1-1.383 0c-1.196-1.175-2.371-2.371-3.553-3.56Z', fill: 'currentColor' },
  { d: 'M12 23C5.925 23 1 18.075 1 12S5.925 1 12 1s11 4.925 11 11-4.925 11-11 11Zm0-2a9 9 0 1 0 0-18 9 9 0 0 0 0 18Z', fill: 'currentColor' },
])
const BitableTodoIcon = createBitableSvgIcon('BitableTodoIcon', [
  { d: 'M17.207 10.207a1 1 0 0 0-1.414-1.414L11 13.586l-2.293-2.293a1 1 0 0 0-1.414 1.414l3 3a1 1 0 0 0 1.414 0l5.5-5.5Z', fill: 'currentColor' },
  { d: 'M2 4a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v16a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V4Zm2 0v16h16V4H4Z', fill: 'currentColor' },
])
const BitableNumberIcon = createBitableSvgIcon('BitableNumberIcon', [
  { d: 'M8.774 2.14a1 1 0 0 1 .85 1.129L9.242 6h6.98l.423-3.01a1 1 0 1 1 1.98.279L18.242 6H22a1 1 0 1 1 0 2h-4.04l-.984 7H20a1 1 0 1 1 0 2h-3.305l-.575 4.093a1 1 0 1 1-1.98-.278L14.674 17h-6.98l-.575 4.093a1 1 0 1 1-1.98-.278L5.674 17H2a1 1 0 1 1 0-2h3.956l.984-7H4a1 1 0 1 1 0-2h3.221l.423-3.01a1 1 0 0 1 1.13-.85ZM14.956 15l.984-7H8.96l-.984 7h6.98Z', fill: 'currentColor' },
])
const BitableCalendarIcon = createBitableSvgIcon('BitableCalendarIcon', [
  { d: 'M7 2a1 1 0 0 1 1 1h8a1 1 0 1 1 2 0h2a2 2 0 0 1 2 2v15a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h2a1 1 0 0 1 1-1Zm9 3H8a1 1 0 0 1-2 0H4v15h16V5h-2a1 1 0 1 1-2 0ZM9 15a1 1 0 0 0-1-1H7a1 1 0 0 0-1 1v1a1 1 0 0 0 1 1h1a1 1 0 0 0 1-1v-1Zm1.5-5a1 1 0 0 1 1-1h1a1 1 0 0 1 1 1v1a1 1 0 0 1-1 1h-1a1 1 0 0 1-1-1v-1Zm3 5a1 1 0 0 0-1-1h-1a1 1 0 0 0-1 1v1a1 1 0 0 0 1 1h1a1 1 0 0 0 1-1v-1Zm1.5 0a1 1 0 0 1 1-1h1a1 1 0 0 1 1 1v1a1 1 0 0 1-1 1h-1a1 1 0 0 1-1-1v-1Zm3-5a1 1 0 0 0-1-1h-1a1 1 0 0 0-1 1v1a1 1 0 0 0 1 1h1a1 1 0 0 0 1-1v-1Z', fill: 'currentColor' },
])
const BitablePhoneIcon = createBitableSvgIcon('BitablePhoneIcon', [
  { d: 'M12.858 1.6c.678 0 1.337.02 1.973.06l.628.049c.21.018.42.04.624.063l.607.078c4.183.599 6.993 2.3 7.16 5.396.108 2.033-.837 3.395-2.435 3.936a4.554 4.554 0 0 1-.706.175c.353.989.618 2.512.792 3.342.134.636.228 1.45.287 2.449l.034.693.022.748.008.394a3 3 0 0 1-2.777 3.039l-.223.008H5.142l-.223-.008a3 3 0 0 1-2.777-3.039l.008-.394.023-.748.033-.693c.059-.999.154-1.813.287-2.449.174-.83.436-2.354.789-3.343a4.535 4.535 0 0 1-.7-.174C.984 10.642.04 9.28.15 7.246c.162-3.02 2.84-4.714 6.855-5.35l.304-.046.606-.078.31-.034.315-.03.628-.047a30.04 30.04 0 0 1 1.301-.054l2.39-.007Zm3.109 6.648-.056-.314H8.087l-.008.055c-.243 1.49-.967 2.568-2.174 3.123a2.94 2.94 0 0 1-.457.162l.18-.458c-.566 1.398-.957 3.24-1.178 4.293-.111.533-.195 1.255-.247 2.16l-.019.35-.026.67-.016.725a1 1 0 0 0 .874 1.007l.118.009H18.86c.56-.009 1-.463.992-1.016l-.016-.725-.026-.67c-.051-1.072-.14-1.91-.266-2.51-.197-.938-.531-2.628-1.001-3.839a2.728 2.728 0 0 1-.45-.158c-1.136-.522-1.844-1.508-2.126-2.864Zm-1.108 7.396a1 1 0 0 1 .117 1.993l-.117.007H9.142a1 1 0 0 1-.117-1.993l.117-.007h5.717ZM12.858 3.6l-2.05.002c-5.044.056-8.536 1.391-8.662 3.752-.06 1.127.321 1.678 1.078 1.934.624.211 1.498.166 1.846.007.625-.287.985-.9 1.08-1.99A1.5 1.5 0 0 1 7.507 5.94l.136-.006h8.71a1.5 1.5 0 0 1 1.494 1.371c.094 1.09.455 1.703 1.08 1.99.347.16 1.222.204 1.846-.007.757-.256 1.139-.807 1.078-1.934-.127-2.36-3.618-3.696-8.663-3.752l-.331-.002Z', fill: 'currentColor' },
])
const BitableMailIcon = createBitableSvgIcon('BitableMailIcon', [
  { d: 'M5.558 10.214a1 1 0 0 1 .925-1.77l5.481 2.861 5.481-2.862a1 1 0 0 1 .925 1.772l-5.887 3.074a.995.995 0 0 1-.52.112.994.994 0 0 1-.518-.112l-5.887-3.075Z', fill: 'currentColor' },
  { d: 'M21.009 3C22.113 3 23 3.895 23 5v14c0 1.105-.888 2-1.992 2H2.99A1.993 1.993 0 0 1 1 19V5c0-1.104.888-2 1.992-2H21.01ZM21 5H3v14h18V5Z', fill: 'currentColor' },
])
const BitableAttachmentIcon = createBitableSvgIcon('BitableAttachmentIcon', [
  { d: 'M12.304 7.315a1 1 0 0 1 1.414 1.414L8.13 14.317a1.485 1.485 0 0 0 0 2.1l.01.011a1.5 1.5 0 0 0 2.117-.005l7.43-7.43a3.5 3.5 0 0 0 0-4.95l-.036-.037a3.5 3.5 0 0 0-4.95 0l-7.778 7.777a5.521 5.521 0 0 0 7.808 7.809l7.07-7.07a1 1 0 0 1 1.415 1.414l-7.07 7.07A7.521 7.521 0 0 1 3.509 10.37l7.778-7.778a5.5 5.5 0 0 1 7.778 0l.037.037a5.5 5.5 0 0 1 0 7.778l-7.43 7.43a3.5 3.5 0 0 1-4.939.012l-.006-.006-.012-.012a3.485 3.485 0 0 1 0-4.928l5.589-5.588Z', fill: 'currentColor' },
])
const BitableMemberIcon = createBitableSvgIcon('BitableMemberIcon', [
  { d: 'M15 6.5a3 3 0 1 0-6 0 3 3 0 0 0 6 0Zm2 0a5 5 0 1 1-10 0 5 5 0 0 1 10 0ZM4 19v2h16v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4Zm-2 0a6 6 0 0 1 6-6h8a6 6 0 0 1 6 6v2a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2v-2Z', fill: 'currentColor' },
])
const BitableLookupIcon = createBitableSvgIcon('BitableLookupIcon', [
  { d: 'M20 4H4v16h7v2H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v6h-2V4Z', fill: 'currentColor' },
  { d: 'M7 6.5a1 1 0 0 0 0 2h8a1 1 0 1 0 0-2H7Zm-1 5a1 1 0 0 1 1-1h3.5a1 1 0 1 1 0 2H7a1 1 0 0 1-1-1Zm1 3a1 1 0 1 0 0 2h2.5a1 1 0 1 0 0-2H7Zm13.939 4.58a5 5 0 1 0-1.522 1.298l1.698 1.953a1 1 0 0 0 1.51-1.312l-1.686-1.939ZM17 19a3 3 0 1 1 0-6 3 3 0 0 1 0 6Z', fill: 'currentColor' },
])
const BitableButtonIcon = createBitableSvgIcon('BitableButtonIcon', [
  { d: 'M21 6.133H3V16.8h9.662l1.214 3.2H3c-1.105 0-2-.955-2-2.133V6.133C1 4.955 1.895 4 3 4h18c1.105 0 2 .955 2 2.133v7.786l-2-.91V6.132Z', fill: 'currentColor' },
  { d: 'M23.172 18.16a1 1 0 0 0 .182-1.883l-8.366-3.808a1 1 0 0 0-1.35 1.265l3.26 8.595a1 1 0 0 0 1.89-.06l1.018-3.307 3.366-.802Z', fill: 'currentColor' },
])
const BitableVisibleIcon = createBitableSvgIcon('BitableVisibleIcon', [
  { d: 'M11.985 18.5c3.238 0 6.236-2.06 9.015-6.513C18.292 7.55 15.3 5.5 11.985 5.5 8.67 5.5 5.689 7.549 3 11.987c2.76 4.454 5.748 6.513 8.985 6.513ZM1.502 12.89a1.782 1.782 0 0 1 .023-1.838C4.428 6.017 7.915 3.5 11.984 3.5c4.086 0 7.594 2.538 10.523 7.614l.028.048c.296.519.294 1.16-.01 1.675-3.006 5.108-6.52 7.663-10.541 7.663-4.007 0-7.501-2.537-10.482-7.61ZM12 16a4 4 0 1 1 0-8 4 4 0 0 1 0 8Zm0-2a2 2 0 1 0 0-4 2 2 0 0 0 0 4Z', fill: 'currentColor' },
])
const BitableInvisibleIcon = createBitableSvgIcon('BitableInvisibleIcon', [
  { d: 'M2.032 8.172a1 1 0 0 1 1.388.267C5.263 11.159 8.637 13 12 13c3.364 0 6.737-1.841 8.58-4.561a1 1 0 0 1 1.656 1.122 11.928 11.928 0 0 1-2.002 2.259l2.009 2.008a1 1 0 1 1-1.415 1.415l-2.12-2.122a1.003 1.003 0 0 1-.085-.096c-.745.472-1.54.87-2.368 1.181l.712 2.658a1 1 0 1 1-1.932.517l-.702-2.62A11.64 11.64 0 0 1 12 15c-.71 0-1.42-.068-2.118-.197l-.691 2.578a1 1 0 1 1-1.932-.517l.692-2.582a13.01 13.01 0 0 1-2.607-1.278c-.03.04-.064.08-.101.117L3.12 15.243a1 1 0 1 1-1.414-1.415l2.032-2.032a11.919 11.919 0 0 1-1.974-2.235 1 1 0 0 1 .267-1.389Z', fill: 'currentColor' },
])
const BitableInfoIcon = createBitableSvgIcon('BitableInfoIcon', [
  { d: 'M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18Zm0 2C5.925 23 1 18.075 1 12S5.925 1 12 1s11 4.925 11 11-4.925 11-11 11Zm-1-7.5v-4a1 1 0 1 1 0-2h1.004c.55 0 .998.445.998.996.003 1.668-.002 3.336-.002 5.004h.5a1 1 0 1 1 0 2h-3a1 1 0 1 1 0-2h.5Zm1-7a1 1 0 1 1 0-2 1 1 0 0 1 0 2Z', fill: 'currentColor' },
])
const BitableAdminIcon = createBitableSvgIcon('BitableAdminIcon', [
  { d: 'M18.874 7a4.002 4.002 0 0 1-7.748 0H3a1 1 0 0 1 0-2h8.126a4.002 4.002 0 0 1 7.748 0H21a1 1 0 1 1 0 2h-2.126ZM15 8a2 2 0 1 0 0-4 2 2 0 0 0 0 4Zm-2.126 11a4.002 4.002 0 0 1-7.748 0H3a1 1 0 1 1 0-2h2.126a4.002 4.002 0 0 1 7.748 0H21a1 1 0 1 1 0 2h-8.126ZM9 20a2 2 0 1 0 0-4 2 2 0 0 0 0 4Z', fill: 'currentColor' },
])
const canvasWidthOptions = [
  { label: '390 移动', value: 390 },
  { label: '768 窄屏', value: 768 },
  { label: '960 抽屉', value: 960 },
  { label: '1200', value: 1200 },
  { label: '1366 默认', value: 1366 },
  { label: '1440', value: 1440 },
  { label: '1600', value: 1600 },
  { label: '1920', value: 1920 },
]
const canvasZoomOptions = [
  { label: '50%', value: 0.5 },
  { label: '67%', value: 0.67 },
  { label: '75%', value: 0.75 },
  { label: '90%', value: 0.9 },
  { label: '100%', value: 1 },
  { label: '125%', value: 1.25 },
]
const canvasPreviewModeOptions = [
  { label: '桌面', value: 'desktop' },
  { label: '窄屏', value: 'narrow' },
  { label: '弹窗', value: 'modal' },
  { label: '抽屉', value: 'drawer' },
  { label: '移动', value: 'mobile' },
]
const crudPreviewModeOptions = [
  { label: '模拟数据', value: 'mock' },
  { label: '真实列表', value: 'realList' },
  { label: '新增表单', value: 'create' },
  { label: '编辑表单', value: 'edit' },
  { label: '详情状态', value: 'detail' },
]
const actionPositionOptions = [
  { label: '工具栏', value: 'toolbar' },
  { label: '行操作列', value: 'row' },
  { label: '详情页', value: 'detail' },
]
const actionTypeOptions = [
  { label: '默认', value: 'default' },
  { label: '主要', value: 'primary' },
  { label: '信息', value: 'info' },
  { label: '成功', value: 'success' },
  { label: '警告', value: 'warning' },
  { label: '危险', value: 'error' },
]
const actionBehaviorOptions = [
  { label: '站内跳转', value: 'route' },
  { label: '外部链接', value: 'external' },
  { label: '调用 API', value: 'CALL_API' },
  { label: '发起主流程', value: 'START_FLOW' },
  { label: '执行触发器', value: 'TRIGGER' },
  { label: '刷新列表', value: 'refresh' },
]
const actionOpenTargetOptions = [
  { label: '当前页', value: '_self' },
  { label: '新窗口', value: '_blank' },
]
const successBehaviorOptions = [
  { label: '无', value: 'none' },
  { label: '刷新列表', value: 'refreshList' },
  { label: '返回上一页', value: 'goBack' },
]
const apiMethodOptions = [
  { label: 'GET', value: 'GET' },
  { label: 'POST', value: 'POST' },
  { label: 'POST 加密', value: 'POST_ENCRYPT' },
  { label: 'PUT', value: 'PUT' },
  { label: 'DELETE', value: 'DELETE' },
  { label: 'PATCH', value: 'PATCH' },
]
const apiParamTargetOptions = [
  { label: 'Path', value: 'path' },
  { label: 'Query', value: 'query' },
  { label: 'Body', value: 'body' },
  { label: 'Header', value: 'header' },
]
const paramSourceOptions = [
  { label: '固定值', value: 'static' },
  { label: '当前行字段', value: 'rowField' },
  { label: '路由参数', value: 'routeQuery' },
  { label: '系统变量', value: 'system' },
]
const routeParamOptions = [
  { label: '记录 ID（id）', value: 'id' },
  { label: '记录 ID（recordId）', value: 'recordId' },
  { label: '页面 Key（pageKey）', value: 'pageKey' },
  { label: '表单 Key（formKey）', value: 'formKey' },
]
const systemVariableOptions = [
  { label: '当前时间戳', value: 'now' },
  { label: '当前日期', value: 'today' },
  { label: '当前用户 ID', value: 'userId' },
  { label: '当前租户 ID', value: 'tenantId' },
  { label: '勾选行 ID', value: 'selectedIds' },
]
const labelPlacementOptions = [
  { label: '左侧', value: 'left' },
  { label: '顶部', value: 'top' },
]
const labelAlignOptions = [
  { label: '左对齐', value: 'left' },
  { label: '右对齐', value: 'right' },
]
const gridVerticalAlignOptions = [
  { label: '垂直填满', value: 'stretch' },
  { label: '靠上', value: 'start' },
  { label: '垂直居中', value: 'center' },
  { label: '靠下', value: 'end' },
]
const gridHorizontalAlignOptions = [
  { label: '水平填满', value: 'stretch' },
  { label: '靠左', value: 'start' },
  { label: '水平居中', value: 'center' },
  { label: '靠右', value: 'end' },
]
const directionOptions = [
  { label: '横向', value: 'row' },
  { label: '纵向', value: 'vertical' },
  { label: '纵向 CSS', value: 'column' },
]
const splitDirectionOptions = [
  { label: '横向', value: 'horizontal' },
  { label: '纵向', value: 'vertical' },
]
const menuModeOptions = [
  { label: '纵向', value: 'vertical' },
  { label: '横向', value: 'horizontal' },
]
const alertTypeOptions = [
  { label: '信息', value: 'info' },
  { label: '成功', value: 'success' },
  { label: '警告', value: 'warning' },
  { label: '错误', value: 'error' },
]
const richEditorModeOptions = [
  { label: '可视编辑', value: 'visual' },
  { label: '源码模式', value: 'source' },
]
const wangEditorModeOptions = [
  { label: '默认', value: 'default' },
  { label: '简洁', value: 'simple' },
]
const watermarkFontStyleOptions = [
  { label: 'normal', value: 'normal' },
  { label: 'italic', value: 'italic' },
  { label: 'oblique 12deg', value: 'oblique 12deg' },
]
const watermarkTextAlignOptions = [
  { label: '左对齐', value: 'left' },
  { label: '居中', value: 'center' },
  { label: '右对齐', value: 'right' },
]
const barcodeFormatOptions = [
  'CODE128',
  'CODE39',
  'EAN13',
  'EAN8',
  'UPC',
  'ITF14',
  'MSI',
  'pharmacode',
  'codabar',
].map(value => ({ label: value, value }))
const qrcodeErrorCorrectionOptions = [
  { label: 'L', value: 'L' },
  { label: 'M', value: 'M' },
  { label: 'Q', value: 'Q' },
  { label: 'H', value: 'H' },
]
const qrcodeDotsTypeOptions = [
  'square',
  'dots',
  'rounded',
  'classy',
  'classy-rounded',
  'extra-rounded',
].map(value => ({ label: value, value }))
const qrcodeCornerTypeOptions = [
  'square',
  'dot',
  'extra-rounded',
].map(value => ({ label: value, value }))
const transferDataSourceOptions = [
  { label: '静态选项', value: 'static' },
  { label: '远程接口', value: 'remote' },
]
const widgetDataSourceOptions = [
  { label: '静态配置', value: 'static' },
  { label: '当前详情/表单数据', value: 'context' },
  { label: '远程接口', value: 'remote' },
]
const detailInfoDataSourceOptions = [
  { label: '当前详情数据', value: 'current' },
  { label: '当前详情字段路径', value: 'context' },
  { label: '远程详情接口', value: 'remote' },
]
const dataBindablePageWidgetKeys = [
  'rich-text',
  'watermark',
  'vue-component',
  'html-tag',
  'markdown',
  'barcode',
  'qrcode',
  'calendar',
  'code',
  'countdown',
  'descriptions',
  'announcement',
  'list',
  'log',
  'number-animation',
  'breadcrumb',
  'menu',
  'pagination',
  'split',
]
const localDataBindableBlockTypes = [
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
]
const markdownPreviewModeOptions = [
  { label: '源码 + 预览', value: 'split' },
  { label: '仅源码', value: 'source' },
  { label: '仅预览', value: 'preview' },
]
const htmlTagOptions = ['div', 'section', 'article', 'aside', 'header', 'footer', 'main', 'span', 'p', 'strong', 'em', 'small', 'label'].map(value => ({ label: value, value }))
const htmlRenderModeOptions = [
  { label: 'HTML 安全渲染', value: 'html' },
  { label: '纯文本', value: 'text' },
]
const vuePreviewModeOptions = [
  { label: '安全模板预览', value: 'safe-template' },
  { label: 'Props 模板预览', value: 'live' },
  { label: '代码视图', value: 'code' },
]
const justifyContentOptions = [
  { label: '靠左', value: 'flex-start' },
  { label: '居中', value: 'center' },
  { label: '靠右', value: 'flex-end' },
  { label: '两端', value: 'space-between' },
  { label: '环绕', value: 'space-around' },
]
const simpleConfigBlockTypes = [
  'rich-text',
  'signature-pad',
  'transfer',
  'step-form',
  'vue-component',
  'html-tag',
  'text-title',
  'paragraph',
  'statistic',
  'link',
  'text-tip',
  'watermark',
  'audio-player',
  'video-player',
  'avatar',
  'barcode',
  'iframe',
  'qrcode',
  'markdown',
  'calendar',
  'code',
  'countdown',
  'announcement',
  'list',
  'log',
  'number-animation',
  'breadcrumb',
  'menu',
  'pagination',
  'split',
  'box-layout',
  'space',
  'descriptions',
]
const componentSizeOptions = [
  { label: '小', value: 'small' },
  { label: '中', value: 'medium' },
  { label: '大', value: 'large' },
]
const tableDensityOptions = [
  { label: '紧凑', value: 'small' },
  { label: '默认', value: 'medium' },
  { label: '宽松', value: 'large' },
]
const renderModeOptions = [
  { label: '表格', value: 'table' },
  { label: '卡片', value: 'card' },
]
const expandTriggerOptions = [
  { label: '图标', value: 'icon' },
  { label: '整行', value: 'row' },
  { label: '图标和整行', value: 'both' },
]
const expandLayoutModeOptions = [
  { label: '单面板', value: 'single' },
  { label: 'Tabs', value: 'tabs' },
  { label: '上下堆叠', value: 'stack' },
]
const expandPanelTypeOptions = [
  { label: '描述信息', value: 'descriptions' },
  { label: '子表表格', value: 'table' },
  { label: '只读表单', value: 'form' },
  { label: '数量余额', value: 'quantity-balance' },
  { label: '数量流水', value: 'quantity-ledger' },
  { label: '数量锁定', value: 'quantity-lock' },
  { label: '多面板 Tabs', value: 'tabs' },
  { label: '自定义插槽', value: 'custom' },
]
const expandDataSourceTypeOptions = [
  { label: '当前行数据', value: 'row' },
  { label: '接口加载', value: 'api' },
  { label: '数量台账', value: 'quantity' },
  { label: '静态数据', value: 'static' },
]
const requestMethodOptions = [
  { label: 'GET', value: 'get' },
  { label: 'POST', value: 'post' },
]
const tagTypeOptions = [
  { label: '默认', value: 'default' },
  { label: '主要', value: 'primary' },
  { label: '信息', value: 'info' },
  { label: '成功', value: 'success' },
  { label: '警告', value: 'warning' },
  { label: '危险', value: 'error' },
]
const queryTypeOptions = [
  { label: '等于', value: 'eq' },
  { label: '包含', value: 'like' },
  { label: '大于等于', value: 'ge' },
  { label: '小于等于', value: 'le' },
  { label: '区间', value: 'between' },
  { label: '多值', value: 'in' },
]
const searchComponentOptions = [
  { label: '自动', value: '' },
  { label: '输入框', value: 'input' },
  { label: '数字输入', value: 'number' },
  { label: '下拉选择', value: 'select' },
  { label: '字典选择', value: 'dictSelect' },
  { label: '组织树', value: 'orgTreeSelect' },
  { label: '用户选择', value: 'userSelect' },
  { label: '区划树', value: 'regionTreeSelect' },
  { label: '树形选择', value: 'treeSelect' },
  { label: '日期', value: 'date' },
  { label: '日期时间', value: 'datetime' },
  { label: '时间', value: 'time' },
]
const tableRenderOptions = [
  { label: '默认', value: '' },
  { label: '链接文本', value: 'link' },
  { label: '字典标签', value: 'dictTag' },
  { label: '组织名称', value: 'orgName' },
  { label: '用户名称', value: 'userName' },
  { label: '区划名称', value: 'regionName' },
  { label: '文件名称', value: 'fileUpload' },
  { label: '图片预览', value: 'imageUpload' },
]
const treeLoadModeOptions = [
  { label: '全量加载', value: 'full' },
  { label: '懒加载', value: 'lazy' },
]
const sortOrderOptions = [
  { label: '降序', value: 'desc' },
  { label: '升序', value: 'asc' },
]
const alignOptions = [
  { label: '左对齐', value: 'left' },
  { label: '居中', value: 'center' },
  { label: '右对齐', value: 'right' },
]
const fixedColumnOptions = [
  { label: '不固定', value: '' },
  { label: '左侧固定', value: 'left' },
  { label: '右侧固定', value: 'right' },
]
const columnClickActionOptions = [
  { label: '无', value: 'none' },
  { label: '跳转页面', value: 'navigate' },
]
const shadowOptions = [
  { label: '无阴影', value: 'none' },
  { label: '轻微', value: '0 2px 8px rgba(15, 23, 42, 0.08)' },
  { label: '中等', value: '0 10px 24px rgba(15, 23, 42, 0.12)' },
  { label: '明显', value: '0 18px 42px rgba(15, 23, 42, 0.16)' },
]
const eventTriggerOptions = [
  { label: '点击组件时', value: 'click' },
  { label: '区块加载完成后', value: 'load' },
  { label: '字段值变化时', value: 'change' },
  { label: '选择树节点时', value: 'nodeSelect' },
  { label: '点击表格行时', value: 'rowClick' },
  { label: '表单提交成功后', value: 'submitSuccess' },
]
const blockEventActionOptions = [
  { label: '不执行动作', value: 'none' },
  { label: '返回上一页', value: 'back' },
  { label: '跳转到目标页面', value: 'navigate' },
  { label: '刷新目标区块', value: 'refreshBlock' },
  { label: '过滤目标区块', value: 'filterBlock' },
  { label: '打开弹窗表单', value: 'openModal' },
  { label: '打开抽屉表单', value: 'openDrawer' },
  { label: '请求后端接口', value: 'request' },
  { label: '执行自定义脚本', value: 'customScript' },
]
const backButtonActionOptions = [
  { label: '浏览器返回', value: 'back' },
  { label: '跳转页面', value: 'navigate' },
]
const resizeAnchors = ['top-left', 'top', 'top-right', 'right', 'bottom-right', 'bottom', 'bottom-left', 'left']
const CANVAS_AUTO_SCROLL_EDGE = 56
const CANVAS_AUTO_SCROLL_MAX_STEP = 20

const canvasRef = ref(null)
const canvasScrollRef = ref(null)
const selectedBlockId = ref(null)
const fieldDrawerOpen = ref(false)
const fieldDrawerMode = ref('table')
const activeDrawerFieldName = ref('')
const expandDescriptionFieldPanelOpen = ref(false)
const fieldAdvancedOpen = ref(false)
const customActionModalOpen = ref(false)
const sourceModalOpen = ref(false)
const sourceModalTab = ref('layout')
const layoutSourceDraft = ref('')
const blockSourceDraft = ref('')
const sourceError = ref('')
const activeActionIndex = ref(0)
const propertyPanelTab = ref('props')
const propertyPanelRef = ref(null)
const paletteKeyword = ref('')
const propertyKeyword = ref('')
const propertyCollapseExpandedNames = ref(['base'])
const allPropertyCollapseNames = ['base', 'search', 'edit', 'toolbar', 'table', 'form-actions', 'default-params', 'event-help']
const propertySearchTabIndex = {
  props: [
    '属性',
    '基础配置',
    '接口',
    '数据源',
    '基础路径',
    '行主键',
    '真实接口预览',
    '响应字段',
    '表单布局',
    '查询',
    '列表',
    '字段',
    '搜索',
    '搜索布局',
    '搜索字段',
    '表格',
    '表格列',
    '列标题',
    '列宽',
    '对齐',
    '固定',
    '省略',
    '排序',
    '分页',
    '表单',
    '弹窗',
    '新增',
    '编辑',
    '详情',
    '页脚',
    '打开方式',
    '标签位置',
    '标签对齐',
    '标签宽度',
    '表单列数',
    '抽屉',
    '工具栏',
    '导入',
    '导出',
    '自定义查询',
    '自定义操作',
    '按钮文案',
    '默认参数',
    '公共参数',
    '表单默认值',
    '查询默认参数',
    '数据处理',
    '组件标题',
    '默认排序',
    '行间距',
    '显示项',
    '多选',
    '模式切换',
    '斑马纹',
    '边框',
    '富文本',
    '签名',
    '穿梭框',
    '分步表单',
    'Vue',
    'HTML',
    '标题',
    '段落',
    '统计',
    '链接',
    '提示',
    '水印',
    '音频',
    '视频',
    '头像',
    '条码',
    '二维码',
    'Markdown',
    '盒子',
    '间距',
    '描述列表',
    'iframe',
    'api',
    'list',
    'detail',
    'create',
    'update',
    'delete',
    'import',
    'export',
  ],
  style: [
    '样式',
    '位置',
    '尺寸',
    '坐标',
    '左',
    '上',
    'x',
    'y',
    '宽度',
    '高度',
    '固定宽',
    '自适应',
    '背景',
    '背景色',
    '边框色',
    '圆角',
    '阴影',
    '内边距',
    '外边距',
    'padding',
    'margin',
    '外观',
    '装饰',
    '颜色',
    '自定义 style',
    'customStyle',
  ],
  interaction: [
    '交互',
    '事件',
    '生命周期',
    '回调',
    '点击',
    '加载完成',
    '提交成功',
    '行点击',
    '跳转',
    '刷新',
    '过滤',
    '接口请求',
    '自定义脚本',
    '参数',
    '目标页面',
    '目标表单',
  ],
}
const propertySearchKeywordRegistry = [
  '基础配置 接口 基础路径 行主键 真实接口预览 响应字段 表单布局 api list detail create update delete import export',
  '查询与列表字段 搜索 查询区 搜索布局 搜索字段 表格 表格列 列标题 列宽 对齐 固定 省略 排序 分页 最大高度 横向宽度 边框 斑马纹 行按钮 操作列 列表行自定义按钮',
  '表单与弹窗 新增 编辑 表单 弹窗 抽屉 详情 页脚 打开方式 标签位置 标签对齐 标签宽度',
  '工具栏 导入 导出 自定义查询 自定义操作 按钮文案 回调 参数处理 提交前 搜索前 加载列表前',
  '默认参数 公共参数 publicParams publicQuery 表单默认值 提交固定参数 查询默认参数',
  '事件 生命周期 回调 点击 加载完成 提交成功 行点击 跳转 弹窗 接口请求',
  '样式 位置 尺寸 左 上 宽度 高度 背景 边框 圆角 阴影 内边距 外边距 自定义 style',
  '树 左树右表 树接口 节点字段 过滤字段 展开 折叠',
  Object.values(propertySearchTabIndex).flat().join(' '),
].map(item => item.toLowerCase())
const paletteCollapsed = ref(false)
const propertyCollapsed = ref(false)
const canvasFocusMode = ref(false)
const canvasPreviewMode = ref('desktop')
const activeTabKey = ref('')
const canvasDragActive = ref(false)
const draggedBlockType = ref('')
const draggedExistingBlockId = ref('')
const dragOverCell = ref(null)
const dragOverPoint = ref(null)
const dragBlockedBlockId = ref('')
const activeDropCell = ref(null)
const deferLayoutEmit = ref(false)
const movingBlockId = ref('')
const movingPreviewBlock = ref(null)
const movingPixelOffset = ref({ x: 0, y: 0 })
const nestedMovingBlockId = ref('')
const canvasViewportWidth = ref(0)
const canvasZoom = ref(1)
let suppressNextBlockClick = false
let hasDeferredLayoutEmit = false
let canvasResizeObserver = null

const localLayout = ref(normalizeDesignerLayout(syncGridLayoutWithModel(
  props.modelValue || createDefaultListGridLayout(props.modelSchema, { layoutType: props.layoutType }),
  props.modelSchema,
  { layoutType: props.layoutType },
)))

const blocks = computed(() => localLayout.value.items || [])
watch(() => props.activeBlockId, (blockId) => {
  if (blockId && blocks.value.some(block => block.id === blockId))
    selectedBlockId.value = blockId
}, { immediate: true })
const runtimeTreeFilter = ref({})
const runtimeTreeActiveKey = ref('__all__')
const collapsedTreePanelMap = ref({})
const resolvedRuntimeCrudProps = computed(() => {
  if (!props.runtimeCrudProps)
    return props.runtimeCrudProps
  const { treeConfig: _treeConfig, ...runtimeOptions } = props.runtimeCrudProps.options || {}
  return {
    ...props.runtimeCrudProps,
    treeConfig: {},
    options: runtimeOptions,
    publicParams: {
      ...(props.runtimeCrudProps.publicParams || {}),
      ...runtimeTreeFilter.value,
    },
  }
})
const designCanvasWidth = computed(() => clamp(localLayout.value.designWidth || LIST_PAGE_DESIGN_WIDTH, 375, 2560))
const totalRows = computed(() => {
  const maxGridBottom = blocks.value.reduce((acc, b) => Math.max(acc, b.gridY + b.gridH), 0)
  const maxPixelBottom = blocks.value.reduce((acc, block) => {
    const rect = resolveBlockFrame(block)
    return Math.max(acc, rect.y + rect.height)
  }, 0)
  const rowsByPixel = Math.ceil(maxPixelBottom / (rowHeight + gap))
  const minRows = props.readonly ? 1 : 20
  const bufferRows = props.readonly ? 0 : 4
  return Math.max(minRows, maxGridBottom + bufferRows, rowsByPixel + bufferRows)
})
const colWidth = computed(() => {
  const availableWidth = props.readonly
    ? Math.max(previewMinWidth, canvasViewportWidth.value)
    : designCanvasWidth.value
  const nextWidth = Math.floor((availableWidth - (LIST_PAGE_GRID_COLS - 1) * gap) / LIST_PAGE_GRID_COLS)
  return Math.max(1, nextWidth)
})
const canvasGridWidth = computed(() => LIST_PAGE_GRID_COLS * colWidth.value + (LIST_PAGE_GRID_COLS - 1) * gap)
const canvasGridHeight = computed(() => totalRows.value * (rowHeight + gap))
const canvasStyle = computed(() => ({
  width: `${canvasGridWidth.value}px`,
  minHeight: `${canvasGridHeight.value}px`,
}))
const canvasZoomLabel = computed(() => `${Math.round(canvasZoom.value * 100)}%`)
const canvasPreviewModeLabel = computed(() => canvasPreviewModeOptions.find(item => item.value === canvasPreviewMode.value)?.label || '桌面')
const canvasViewportSummary = computed(() => {
  return `${canvasPreviewModeLabel.value} ${designCanvasWidth.value}px / ${canvasZoomLabel.value}`
})
const canvasScaleStyle = computed(() => ({
  transform: `scale(${canvasZoom.value})`,
  transformOrigin: '0 0',
}))
const canvasZoomStageStyle = computed(() => ({
  width: `${(canvasGridWidth.value + (props.readonly ? 0 : 24)) * canvasZoom.value}px`,
  minHeight: `${(canvasGridHeight.value + (props.readonly ? 0 : 24)) * canvasZoom.value}px`,
}))
const collapsedTreeFrames = computed(() => {
  return blocks.value
    .filter(block => block.blockType === 'tree-panel' && collapsedTreePanelMap.value[block.id])
    .map(block => ({ block, rect: resolveBlockFrame(block) }))
    .sort((a, b) => a.rect.x - b.rect.x)
})

const selectedBlock = computed(() => findBlockInTree(blocks.value, selectedBlockId.value) || null)
const selectedAiCrudFormModalProps = computed(() => {
  if (selectedBlock.value?.blockType !== 'AiCrudPage')
    return {}
  return {
    ...(selectedBlock.value.props || {}),
    ...(resolvedRuntimeCrudProps.value || {}),
  }
})
const selectedBlockEvents = computed(() => selectedBlock.value?.props?.events || [])
const selectedBlockStyle = computed(() => ({
  ...createDefaultBlockStyle(),
  ...(selectedBlock.value?.props?.style || {}),
}))
const selectedBlockBackgroundHex = computed(() => colorToHexInput(selectedBlockStyle.value.backgroundColor, ''))
const selectedBlockBorderHex = computed(() => colorToHexInput(selectedBlockStyle.value.borderColor, 'E4E4E7'))
const selectedBlockBackgroundPreview = computed(() => hexInputToColor(selectedBlockBackgroundHex.value, 'transparent'))
const selectedBlockBackgroundColorInput = computed(() => hexInputToColor(selectedBlockBackgroundHex.value, '#ffffff'))
const selectedBlockBorderPreview = computed(() => {
  if (selectedBlockStyle.value.borderStyle === 'none')
    return '#e4e4e7'
  return hexInputToColor(selectedBlockBorderHex.value, '#e4e4e7')
})
const selectedBlockFrame = computed(() => selectedBlock.value
  ? resolveBlockFrame(selectedBlock.value)
  : { x: 0, y: 0, width: 24, height: 24 })
const selectedBlockWidthMode = computed(() => selectedBlock.value ? resolveBlockWidthMode(selectedBlock.value) : 'full')
const selectedBlockHeightMode = computed(() => selectedBlock.value ? resolveBlockHeightMode(selectedBlock.value) : 'fixed')
const selectedBlockContentAlign = computed(() => selectedBlock.value?.props?.style?.textAlign || selectedBlock.value?.props?.textAlign || selectedBlock.value?.props?.align || 'left')
const selectedBlockFixedWidth = computed(() => {
  if (!selectedBlock.value)
    return 24
  const value = selectedBlock.value.props?.style?.width
  const fallback = selectedBlockFrame.value.width
  return Math.max(24, resolveCssNumber(value, fallback))
})
const layoutCodeText = computed(() => JSON.stringify(localLayout.value || {}, null, 2))
const selectedBlockCodeText = computed(() => selectedBlock.value ? JSON.stringify(selectedBlock.value, null, 2) : '')
const externalCustomActionsEnabled = computed(() => Array.isArray(props.customActions))
const customActionList = computed(() => normalizeCustomActionList(
  externalCustomActionsEnabled.value ? (props.customActions || []) : (selectedBlock.value?.props?.customActions || []),
))
const toolbarCustomActions = computed(() => customActionList.value.filter(action => (action.position || 'toolbar') === 'toolbar'))
const rowCustomActions = computed(() => customActionList.value.filter(action => (action.position || 'row') === 'row'))
const activeAction = computed(() => customActionList.value[activeActionIndex.value] || null)
const apiConfigs = ref([])
const apiConfigLoading = ref(false)
const apiConfigLoaded = ref(false)
const systemMenuPages = ref([])
const systemMenuPageLoading = ref(false)
const systemMenuPageLoaded = ref(false)
const apiConfigOptions = computed(() => apiConfigs.value.map(item => ({
  label: `${item.apiName || item.apiCode || item.urlPath} · ${item.reqMethod || 'GET'} ${item.urlPath || ''}`,
  value: String(item.id || item.apiCode || item.urlPath),
})))
const systemMenuPageTargetOptions = computed(() => buildSystemMenuPageTargetOptions(systemMenuPages.value))
const dropPreviewLabel = computed(() => {
  const meta = resolveListPageBlockMeta(resolveDraggedPreviewBlockType())
  return meta ? `放置 ${meta.title}` : '放置区块'
})
const dropPreviewStyle = computed(() => {
  if (!canvasDragActive.value || !dragOverPoint.value)
    return null
  if (activeDropCell.value)
    return null
  if (dragBlockedBlockId.value)
    return null
  const meta = resolveListPageBlockMeta(resolveDraggedPreviewBlockType())
  if (!meta)
    return null
  const source = draggedExistingBlockId.value ? findBlockInTree(blocks.value, draggedExistingBlockId.value) : null
  const sourceFrame = source ? resolveDetachedBlockFrame(source, meta) : null
  const width = Math.min(
    sourceFrame?.width || gridWidthToPixels(Math.min(meta.defaultW || 4, LIST_PAGE_GRID_COLS)),
    canvasGridWidth.value,
  )
  const height = sourceFrame?.height || gridHeightToPixels(Math.max(1, meta.defaultH || 2))
  const frame = resolveCanvasDropFrame(source?.id || '', {
    x: clamp(dragOverPoint.value.x, 0, Math.max(0, canvasGridWidth.value - width)),
    y: Math.max(0, dragOverPoint.value.y),
    width,
    height,
  })
  return {
    left: `${frame.x}px`,
    top: `${frame.y}px`,
    width: `${width}px`,
    height: `${height}px`,
  }
})
const blockedDropPreviewStyle = computed(() => {
  if (!canvasDragActive.value || !dragBlockedBlockId.value)
    return null
  const block = findBlockInTree(blocks.value, dragBlockedBlockId.value)
  if (!block)
    return null
  const rect = resolveBlockFrame(block)
  return {
    left: `${rect.x}px`,
    top: `${rect.y}px`,
    width: `${rect.width}px`,
    height: `${rect.height}px`,
  }
})
const movePlaceholderStyle = computed(() => {
  const block = movingPreviewBlock.value
  if (!block)
    return null
  const rect = resolveBlockFrame(block)
  return {
    left: `${rect.x}px`,
    top: `${rect.y}px`,
    width: `${rect.width}px`,
    height: `${rect.height}px`,
  }
})
const layoutTitle = computed(() => {
  if (props.layoutType === 'tree-crud')
    return '自由画布 · 左树右表模板'
  if (props.layoutType === 'master-detail-crud')
    return '自由画布 · 关联数据'
  return '自由画布'
})
const selectedBlockMeta = computed(() => selectedBlock.value ? resolveListPageBlockMeta(selectedBlock.value.blockType) : null)

function resolveDraggedPreviewBlockType() {
  if (draggedBlockType.value)
    return draggedBlockType.value
  if (!draggedExistingBlockId.value)
    return ''
  return findBlockInTree(blocks.value, draggedExistingBlockId.value)?.blockType || ''
}

function resolveDetachedBlockFrame(block = {}, meta = null) {
  const style = block.props?.style || {}
  const fallbackGridW = Math.min(Number(block.gridW) || meta?.defaultW || 4, LIST_PAGE_GRID_COLS)
  const fallbackGridH = Math.max(1, Number(block.gridH) || meta?.defaultH || 2)
  const fallbackWidth = gridWidthToPixels(fallbackGridW)
  const fallbackHeight = gridHeightToPixels(fallbackGridH)
  const width = style.widthMode === 'auto'
    ? Math.min(520, Math.max(240, fallbackWidth))
    : resolveCssNumber(style.width, fallbackWidth)
  const height = resolveCssNumber(style.height, fallbackHeight)
  return {
    width: Math.max(24, Math.min(width || fallbackWidth, canvasGridWidth.value)),
    height: Math.max(24, height || fallbackHeight),
  }
}
const blockTargetOptions = computed(() => blocks.value
  .flatMap(block => collectBlocksInTree(block))
  .filter(block => block.id !== selectedBlock.value?.id)
  .map(block => ({
    label: `${block.label || block.blockType}（${block.blockType}）`,
    value: block.id,
  })))
const pageTargetOptions = computed(() => props.pages.map(page => ({
  label: `${page.pageName || page.pageKey}（${page.pageType || 'custom'}）`,
  value: page.pageKey,
})))
const formTargetOptions = computed(() => props.formOptions
  .filter(item => item?.value)
  .map(item => ({
    label: item.label || item.value,
    value: item.value,
  })))
const rowFieldOptions = computed(() => props.fields
  .filter(field => field?.field)
  .map(field => ({
    label: `${field.label || field.field}（${field.field}）`,
    value: field.field,
  })))
const runtimeRuleFieldOptions = computed(() => rowFieldOptions.value)
const childBlockTypeOptions = computed(() => listPageBlockCatalog
  .filter(item => !item.unique)
  .filter(item => !['card', 'tabs', 'grid-layout', 'box-layout'].includes(item.blockType))
  .filter(item => !item.onlyFor || item.onlyFor.includes(props.layoutType))
  .map(item => ({
    label: `${item.title}（${item.blockType}）`,
    value: item.blockType,
  })))
const tabPaneOptions = computed(() => (selectedBlock.value?.props?.tabs || []).map(tab => ({
  label: tab.title || tab.key,
  value: tab.key,
})))
const activeTabChildren = computed(() => {
  const tabs = selectedBlock.value?.props?.tabs || []
  const tab = tabs.find(item => item.key === activeTabKey.value) || tabs[0]
  return tab?.children || []
})
const selectedGridCells = computed(() => normalizeGridLayoutCells(selectedBlock.value))

const primaryModelCode = computed(() => props.modelSchema?.pageModelRefs?.find(ref => ref.primary)?.modelCode || '')
const primaryFieldOptions = computed(() => props.fields
  .filter(field => !field.modelCode || !primaryModelCode.value || field.modelCode === primaryModelCode.value)
  .filter(field => isPageFieldVisible(field, 'table'))
  .map(f => ({
    label: f.label ? `${f.label}（${f.sourceField || f.field}）` : (f.sourceField || f.field),
    value: f.sourceField || f.field,
  })))
const sortFieldOptions = computed(() => {
  const options = primaryFieldOptions.value.map(item => ({ ...item }))
  if (!options.some(item => item.value === 'id')) {
    options.unshift({ label: 'ID（id）', value: 'id' })
  }
  return options
})
const defaultApiValues = computed(() => {
  const key = props.modelSchema?.configKey
    || props.modelSchema?.object?.configKey
    || props.modelSchema?.object?.code
    || props.modelSchema?.objectCode
    || props.modelSchema?.modelCode
    || ''
  const prefix = key ? `/ai/crud/${key}` : '/ai/crud/当前配置'
  return {
    api: prefix,
    listApi: `get@${prefix}/page`,
    detailApi: `get@${prefix}/:id`,
    createApi: `post@${prefix}`,
    updateApi: `put@${prefix}`,
    deleteApi: `delete@${prefix}/:id`,
    importApi: `post@${prefix}/import`,
    exportApi: `get@${prefix}/export`,
  }
})
const treeSourceOptions = computed(() => resolveTreeSourceRefs(props.modelSchema).map(ref => ({
  label: `${ref.modelName || ref.modelCode || '数据模型'}${ref.primary ? '（主模型）' : '（引用模型）'}`,
  value: ref.modelCode || '',
})))
const treeFieldOptions = computed(() => resolveTreeFieldOptions(
  props.modelSchema,
  selectedBlock.value?.props?.sourceModelCode || '',
))

const groupedBlocks = computed(() => {
  const keyword = paletteKeyword.value.trim().toLowerCase()
  const groups = [
    { key: 'page', title: '页面组件', items: [] },
    { key: 'data', title: '数据区块', items: [] },
    { key: 'action', title: '动作区块', items: [] },
    { key: 'navigation', title: '导航组件', items: [] },
    { key: 'layout', title: '布局组件', items: [] },
    { key: 'content', title: '文本内容', items: [] },
    { key: 'media', title: '媒体展示', items: [] },
    { key: 'advanced', title: '高级嵌入', items: [] },
    { key: 'extra', title: '辅助区块', items: [] },
  ]
  for (const item of listPageBlockCatalog) {
    if (keyword) {
      const text = `${item.title || ''} ${item.desc || ''} ${item.blockType || ''} ${item.componentKey || ''}`.toLowerCase()
      if (!text.includes(keyword))
        continue
    }
    const target = groups.find(g => g.key === item.group) || groups[2]
    target.items.push(item)
  }
  return groups.filter(group => group.items.length)
})

function resolvePaletteItemIcon(item = {}) {
  const iconMap = {
    'search-form': SearchOutline,
    'toolbar': FlashOutline,
    'back-button': ChevronBackOutline,
    'page-title': DocumentTextOutline,
    'grid-layout': ResizeOutline,
    'detail-info': ReaderOutline,
    'AiCrudPage': DesktopOutline,
    'AiTable': GridOutline,
    'AiForm': DocumentTextOutline,
    'data-table': ListOutline,
    'tree-panel': ContractOutline,
    'stats-strip': AnalyticsOutline,
    'info-panel': AlertCircleOutline,
    'custom-html': CodeSlashOutline,
    'action-button': FlashOutline,
    'button-group': ToggleOutline,
    'tag-list': PricetagOutline,
    'steps': ReorderThreeOutline,
    'timeline': TimerOutline,
    'empty-state': RemoveOutline,
    'card': AlbumsOutline,
    'tabs': BrowsersOutline,
    'divider': RemoveOutline,
    'spacer': ResizeOutline,
    'rich-text': DocumentTextOutline,
    'transfer': SwapHorizontalOutline,
    'watermark': TextOutline,
    'vue-component': BrowsersOutline,
    'html-tag': CodeSlashOutline,
    'markdown': ReorderThreeOutline,
    'barcode': StatsChartOutline,
    'qrcode': QrCodeOutline,
    'calendar': CalendarOutline,
    'code': CodeSlashOutline,
    'countdown': TimerOutline,
    'descriptions': ReaderOutline,
    'announcement': AlertCircleOutline,
    'list': ListOutline,
    'log': TerminalOutline,
    'number-animation': AnalyticsOutline,
    'breadcrumb': NavigateOutline,
    'menu': MenuOutline,
    'pagination': ReorderThreeOutline,
    'split': ResizeOutline,
    'signature-pad': TextOutline,
    'step-form': ExpandOutline,
    'text-title': DesktopOutline,
    'paragraph': DocumentTextOutline,
    'statistic': StatsChartOutline,
    'link': FlashOutline,
    'text-tip': AlertCircleOutline,
    'audio-player': PhonePortraitOutline,
    'video-player': TabletLandscapeOutline,
    'avatar': PhonePortraitOutline,
    'iframe': BrowsersOutline,
    'page': DesktopOutline,
    'data': GridOutline,
    'action': FlashOutline,
    'navigation': ChevronForwardOutline,
    'layout': ResizeOutline,
    'content': CodeSlashOutline,
    'media': PhonePortraitOutline,
    'advanced': BrowsersOutline,
    'extra': ExpandOutline,
  }
  return iconMap[item.blockType] || iconMap[item.group] || DesktopOutline
}

const paletteStats = computed(() => {
  const total = listPageBlockCatalog.length
  const filtered = groupedBlocks.value.reduce((sum, group) => sum + group.items.length, 0)
  return {
    total,
    filtered,
  }
})

const fieldMap = computed(() => new Map(props.fields.map(f => [f.field, f])))
const queryFieldOptions = computed(() => {
  const options = props.fields
    .filter(field => !field.systemField || field.field === 'id')
    .map(field => ({
      label: field.label ? `${field.label}（${field.sourceField || field.field}）` : (field.sourceField || field.field),
      value: field.field,
    }))
  if (!options.some(item => item.value === 'id'))
    options.unshift({ label: 'ID（id）', value: 'id' })
  return options
})
const selectedBlockZoneKey = computed(() => {
  if (fieldDrawerMode.value === 'search')
    return 'search'
  return selectedBlock.value?.blockType === 'search-form' ? 'search' : 'table'
})
const selectedFieldDrawerTitle = computed(() => selectedBlockZoneKey.value === 'search' ? '查询条件' : '字段')
const selectedFieldRefs = computed(() => resolveSelectedFieldRefs())
const selectedFieldsList = computed(() => selectedFieldRefs.value
  .map(ref => fieldMap.value.get(ref))
  .filter(Boolean))
const availableFields = computed(() => {
  const set = new Set(selectedFieldRefs.value)
  return props.fields.filter(f => isPageFieldVisible(f, selectedBlockZoneKey.value) && !set.has(f.field))
})
const crudTablePanelFields = computed(() => {
  if (selectedBlock.value?.blockType !== 'AiCrudPage')
    return []
  const tableFields = props.fields.filter(field => isPageFieldVisible(field, 'table') && field?.field)
  const refIndex = new Map(resolveSelectedFieldRefs(selectedBlock.value, 'table').map((ref, index) => [ref, index]))
  return [...tableFields].sort((left, right) => {
    const leftIndex = refIndex.has(left.field) ? refIndex.get(left.field) : Number.MAX_SAFE_INTEGER
    const rightIndex = refIndex.has(right.field) ? refIndex.get(right.field) : Number.MAX_SAFE_INTEGER
    if (leftIndex !== rightIndex)
      return leftIndex - rightIndex
    return tableFields.indexOf(left) - tableFields.indexOf(right)
  })
})
const activeDrawerField = computed(() => {
  if (!selectedFieldsList.value.length)
    return null
  return selectedFieldsList.value.find(field => field.field === activeDrawerFieldName.value) || selectedFieldsList.value[0]
})
const activeDrawerFieldSetting = computed(() => activeDrawerField.value ? resolveFieldSetting(activeDrawerField.value.field) : {})

watch(selectedFieldRefs, (refs) => {
  if (!refs.length) {
    activeDrawerFieldName.value = ''
    return
  }
  if (!refs.includes(activeDrawerFieldName.value))
    activeDrawerFieldName.value = refs[0]
})

watch(
  selectedBlockId,
  (blockId) => {
    const block = findBlockInTree(blocks.value, blockId)
    propertyCollapseExpandedNames.value = ['base']
    if (block?.blockType === 'tabs') {
      const firstKey = block.props?.tabs?.[0]?.key || ''
      if (!block.props?.tabs?.some(tab => tab.key === activeTabKey.value))
        activeTabKey.value = firstKey
    }
  },
)

watch(
  propertyKeyword,
  (value) => {
    const keyword = String(value || '').trim().toLowerCase()
    if (!keyword)
      return
    propertyPanelTab.value = resolvePropertySearchTab(keyword)
    propertyCollapseExpandedNames.value = [...allPropertyCollapseNames]
    nextTick(() => {
      scrollPropertySearchTarget(keyword)
      setTimeout(() => scrollPropertySearchTarget(keyword), 80)
    })
  },
)

watch(
  () => props.modelValue,
  (value) => {
    const next = normalizeDesignerLayout(syncGridLayoutWithModel(value, props.modelSchema, { layoutType: props.layoutType }))
    if (JSON.stringify(next) !== JSON.stringify(localLayout.value))
      localLayout.value = next
  },
  { deep: true },
)

watch(
  () => props.modelSchema,
  () => {
    localLayout.value = normalizeDesignerLayout(syncGridLayoutWithModel(localLayout.value, props.modelSchema, { layoutType: props.layoutType }))
  },
  { deep: true },
)

watch(
  () => props.layoutType,
  () => {
    localLayout.value = normalizeDesignerLayout(syncGridLayoutWithModel(localLayout.value, props.modelSchema, { layoutType: props.layoutType }))
  },
)

watch(
  blocks,
  (value) => {
    const blockIds = new Set(value.map(block => block.id))
    const nextMap = Object.fromEntries(
      Object.entries(collapsedTreePanelMap.value).filter(([id]) => blockIds.has(id)),
    )
    if (Object.keys(nextMap).length !== Object.keys(collapsedTreePanelMap.value).length)
      collapsedTreePanelMap.value = nextMap
  },
  { deep: true },
)

watch(
  localLayout,
  (value) => {
    if (deferLayoutEmit.value) {
      hasDeferredLayoutEmit = true
      return
    }
    emitLayoutChange(value)
  },
  { deep: true },
)

function emitLayoutChange(value = localLayout.value) {
  emit('update:modelValue', JSON.parse(JSON.stringify(value)))
}

function openSourceModal() {
  layoutSourceDraft.value = layoutCodeText.value
  blockSourceDraft.value = selectedBlockCodeText.value
  sourceModalTab.value = selectedBlock.value ? 'block' : 'layout'
  sourceError.value = ''
  sourceModalOpen.value = true
}

function cancelSourceModalEdit() {
  layoutSourceDraft.value = ''
  blockSourceDraft.value = ''
  sourceError.value = ''
  sourceModalOpen.value = false
}

function applySourceModalCode() {
  const applied = sourceModalTab.value === 'block' && selectedBlock.value
    ? applyBlockSourceCode()
    : applyLayoutSourceCode()
  if (applied)
    cancelSourceModalEdit()
}

function applyLayoutSourceCode() {
  try {
    const parsed = JSON.parse(layoutSourceDraft.value || '{}')
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed))
      throw new Error('画布布局 JSON 必须是对象')
    localLayout.value = normalizeDesignerLayout(syncGridLayoutWithModel(parsed, props.modelSchema, { layoutType: props.layoutType }))
    sourceError.value = ''
    return true
  }
  catch (error) {
    sourceError.value = error?.message || 'JSON 解析失败'
    return false
  }
}

function applyBlockSourceCode() {
  if (!selectedBlock.value) {
    sourceError.value = '请先选中一个区块'
    return false
  }
  try {
    const parsed = JSON.parse(blockSourceDraft.value || '{}')
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed))
      throw new Error('当前区块 JSON 必须是对象')
    const targetId = selectedBlock.value.id
    if (parsed.id && parsed.id !== targetId)
      throw new Error('当前区块 JSON 的 id 不能改成其他区块')
    const replacement = {
      ...parsed,
      id: targetId,
    }
    localLayout.value = {
      ...localLayout.value,
      items: normalizeGridItems(mapBlocksInTree(blocks.value, block => block.id === targetId ? replacement : block)),
    }
    sourceError.value = ''
    return true
  }
  catch (error) {
    sourceError.value = error?.message || 'JSON 解析失败'
    return false
  }
}

function resolveBlockStyle(block) {
  const componentStyle = block.props?.style || {}
  const rect = resolveRuntimeBlockFrame(block, resolveBlockFrame(block))
  const widthMode = resolveBlockWidthMode(block)
  const heightMode = resolveBlockHeightMode(block)
  const collapsedTree = isTreePanelCollapsed(block)
  const style = {
    left: `${rect.x}px`,
    top: `${rect.y}px`,
    minWidth: resolveAbsoluteCssSize(componentStyle.minWidth),
    maxWidth: resolveAbsoluteCssSize(componentStyle.maxWidth),
    minHeight: resolveAbsoluteCssSize(componentStyle.minHeight),
    maxHeight: resolveAbsoluteCssSize(componentStyle.maxHeight),
    textAlign: componentStyle.textAlign || block.props?.textAlign || block.props?.align || 'left',
  }
  if (collapsedTree)
    style.width = `${rect.width}px`
  else if (widthMode === 'full')
    style.right = '0'
  else
    style.width = `${rect.width}px`
  if (heightMode === 'full')
    style.bottom = '0'
  else if (heightMode === 'auto')
    style.height = 'auto'
  else
    style.height = `${rect.height}px`
  if (movingBlockId.value === block.id) {
    style.transform = `translate3d(${movingPixelOffset.value.x}px, ${movingPixelOffset.value.y}px, 0) scale(0.995)`
  }
  return style
}

function resolveRuntimeBlockFrame(block, rect) {
  if (!collapsedTreeFrames.value.length)
    return rect
  const next = { ...rect }
  for (const { block: treeBlock, rect: treeRect } of collapsedTreeFrames.value) {
    const delta = Math.max(0, treeRect.width - TREE_PANEL_COLLAPSED_WIDTH)
    if (!delta)
      continue
    if (block.id === treeBlock.id) {
      next.width = TREE_PANEL_COLLAPSED_WIDTH
      continue
    }
    if (!isVerticalFrameOverlap(next, treeRect))
      continue
    const originalTreeRight = treeRect.x + treeRect.width
    if (next.x >= originalTreeRight - 1) {
      next.x -= delta
      next.width += delta
    }
  }
  return next
}

function isTreePanelCollapsed(block = {}) {
  return block.blockType === 'tree-panel' && collapsedTreePanelMap.value[block.id]
}

function isVerticalFrameOverlap(a, b) {
  const aTop = Number(a.y) || 0
  const bTop = Number(b.y) || 0
  return aTop < bTop + (Number(b.height) || 0) && bTop < aTop + (Number(a.height) || 0)
}

function resolveBlockFrame(block = {}) {
  const componentStyle = block.props?.style || {}
  const fallbackX = Number(block.gridX || 0) * (colWidth.value + gap)
  const fallbackY = Number(block.gridY || 0) * (rowHeight + gap)
  const fallbackWidth = gridWidthToPixels(block.gridW || 1)
  const fallbackHeight = gridHeightToPixels(block.gridH || 1)
  const x = resolveCssNumber(componentStyle.x ?? componentStyle.left, fallbackX)
  const y = resolveCssNumber(componentStyle.y ?? componentStyle.top, fallbackY)
  const widthMode = resolveBlockWidthMode(block)
  const width = Math.max(24, resolveFrameWidth(componentStyle.width, widthMode, x, fallbackWidth))
  const height = Math.max(24, resolveCssNumber(componentStyle.height, fallbackHeight))
  return { x, y, width, height }
}

function resolveBlockWidthMode(block = {}) {
  const style = block.props?.style || {}
  if (['full', 'auto', 'fixed'].includes(style.widthMode))
    return style.widthMode
  if (style.width === 'auto')
    return 'auto'
  if (style.width === '100%' || style.width === '' || style.width === undefined || style.width === null)
    return 'full'
  return 'fixed'
}

function resolveBlockHeightMode(block = {}) {
  const mode = block.props?.style?.heightMode
  return ['fixed', 'auto', 'full'].includes(mode) ? mode : 'fixed'
}

function resolveFrameWidth(value, mode, x = 0, fallback = 320) {
  if (mode === 'full')
    return Math.max(24, canvasGridWidth.value - x)
  if (mode === 'auto')
    return Math.max(240, Math.min(520, fallback || 320))
  return resolveCssNumber(value, fallback)
}

function gridWidthToPixels(gridW = 1) {
  const width = clamp(Number(gridW) || 1, 1, LIST_PAGE_GRID_COLS)
  return width * colWidth.value + (width - 1) * gap
}

function gridHeightToPixels(gridH = 1) {
  const height = Math.max(1, Number(gridH) || 1)
  return height * rowHeight + (height - 1) * gap
}

function resolveCssNumber(value, fallback = 0) {
  if (value === null || value === undefined || value === '' || value === '100%')
    return Math.round(fallback)
  if (typeof value === 'number')
    return Math.round(value)
  const text = String(value).trim()
  if (!text)
    return Math.round(fallback)
  const num = Number(text.replace('px', ''))
  return Number.isFinite(num) ? Math.round(num) : Math.round(fallback)
}

function frameToGridPatch(frame = {}) {
  const cellW = colWidth.value + gap
  const cellH = rowHeight + gap
  const gridX = clamp(Math.round((Number(frame.x) || 0) / cellW), 0, LIST_PAGE_GRID_COLS - 1)
  const gridY = Math.max(0, Math.round((Number(frame.y) || 0) / cellH))
  const gridW = clamp(Math.max(1, Math.round(((Number(frame.width) || 24) + gap) / cellW)), 1, LIST_PAGE_GRID_COLS - gridX)
  const gridH = Math.max(1, Math.round(((Number(frame.height) || 24) + gap) / cellH))
  return { gridX, gridY, gridW, gridH }
}

function resolveAbsoluteCssSize(value) {
  if (value === null || value === undefined || value === '' || value === '100%')
    return undefined
  if (typeof value === 'number')
    return `${value}px`
  const text = String(value).trim()
  if (!text)
    return undefined
  return /^\d+(?:\.\d+)?$/.test(text) ? `${text}px` : text
}

function toNumberOrNull(value) {
  if (value === null || value === undefined || value === '' || value === '100%')
    return null
  const num = Number(String(value).replace('px', ''))
  return Number.isFinite(num) ? num : null
}

function normalizeSpacingValue(value) {
  const text = String(value ?? '').trim()
  if (!text)
    return 0
  return text
    .split(/\s+/)
    .map(part => (/^\d+(?:\.\d+)?$/.test(part) ? `${part}px` : part))
    .join(' ')
}

function colorToHexInput(value, fallback = '') {
  const text = String(value || '').trim()
  const match = text.match(/^#?([0-9a-f]{3}(?:[0-9a-f]{3})?)/i)
  if (!match)
    return fallback
  const hex = match[1]
  if (hex.length === 3)
    return hex.split('').map(item => `${item}${item}`).join('').toUpperCase()
  return hex.toUpperCase()
}

function hexInputToColor(value, fallback = '#ffffff') {
  const text = String(value || '').trim().replace(/^#/, '')
  if (!/^[0-9a-f]{3}(?:[0-9a-f]{3})?$/i.test(text))
    return fallback
  return `#${text}`
}

function updateSelectedBlockBackground(value) {
  if (!selectedBlock.value)
    return
  patchBlockStyle(selectedBlock.value.id, { backgroundColor: hexInputToColor(value, 'transparent') })
}

function updateSelectedBlockBorderStyle(value) {
  if (!selectedBlock.value)
    return
  patchBlockStyle(selectedBlock.value.id, {
    borderStyle: value || 'solid',
    borderColor: value === 'none'
      ? 'transparent'
      : selectedBlockStyle.value.borderColor === 'transparent'
        ? '#e4e4e7'
        : selectedBlockStyle.value.borderColor,
    borderWidth: value === 'none' ? 0 : selectedBlockStyle.value.borderWidth || 1,
  })
}

function updateSelectedBlockBorderColor(value) {
  if (!selectedBlock.value)
    return
  patchBlockStyle(selectedBlock.value.id, {
    borderColor: hexInputToColor(value, '#e4e4e7'),
    borderStyle: selectedBlockStyle.value.borderStyle === 'none'
      ? 'solid'
      : selectedBlockStyle.value.borderStyle || 'solid',
    borderWidth: selectedBlockStyle.value.borderWidth || 1,
  })
}

function selectBlock(blockId) {
  if (props.readonly)
    return
  selectedBlockId.value = blockId
  propertyCollapsed.value = false
}

function handleBlockClick(blockId) {
  if (suppressNextBlockClick) {
    suppressNextBlockClick = false
    return
  }
  selectBlock(blockId)
}

function handleTreePanelCollapseChange(payload = {}) {
  const id = payload.id || payload.blockId
  if (!id)
    return
  collapsedTreePanelMap.value = {
    ...collapsedTreePanelMap.value,
    [id]: payload.collapsed === true,
  }
}

function handleRuntimeTreeSelect(payload = {}) {
  const filterField = payload.filterField
  runtimeTreeActiveKey.value = payload.key || '__all__'
  if (!filterField || payload.clear || payload.value === undefined || payload.value === null || payload.value === '') {
    runtimeTreeFilter.value = {}
    return
  }
  runtimeTreeFilter.value = {
    [filterField]: payload.value,
  }
}

function clearSelection() {
  if (props.readonly)
    return
  selectedBlockId.value = null
}

function isFieldConfigurableBlock(blockType) {
  return ['search-form', 'data-table', 'AiCrudPage', 'AiTable', 'AiForm'].includes(blockType)
}

function resolveFieldConfigLabel(blockType) {
  if (blockType === 'search-form')
    return '查询字段'
  if (blockType === 'AiForm')
    return '表单字段'
  return '展示字段'
}

function resolveAiCrudVisibleFlags(block = {}) {
  const props = block.props || {}
  const flags = []
  if (props.showSearch !== false)
    flags.push('showSearch')
  if (props.showPagination !== false)
    flags.push('showPagination')
  if (props.hideToolbar !== true)
    flags.push('showToolbar')
  if (props.showRenderModeSwitch !== false)
    flags.push('showRenderModeSwitch')
  return flags
}

function updateAiCrudVisibleFlags(values = []) {
  if (!selectedBlock.value)
    return
  patchBlockProps(selectedBlock.value.id, {
    showSearch: values.includes('showSearch'),
    showPagination: values.includes('showPagination'),
    hideToolbar: !values.includes('showToolbar'),
    showRenderModeSwitch: values.includes('showRenderModeSwitch'),
  })
}

function resolveAiCrudTableFlags(block = {}) {
  const props = block.props || {}
  const flags = []
  if (props.bordered === true)
    flags.push('bordered')
  if (props.striped === true)
    flags.push('striped')
  if (props.hideSelection === true)
    flags.push('hideSelection')
  if (props.resizable !== false)
    flags.push('resizable')
  if (props.searchEnableCollapse !== false)
    flags.push('searchEnableCollapse')
  return flags
}

function updateAiCrudTableFlags(values = []) {
  if (!selectedBlock.value)
    return
  patchBlockProps(selectedBlock.value.id, {
    bordered: values.includes('bordered'),
    striped: values.includes('striped'),
    hideSelection: values.includes('hideSelection'),
    resizable: values.includes('resizable'),
    searchEnableCollapse: values.includes('searchEnableCollapse'),
  })
}

function firstExpandPanel(block = {}) {
  return block.props?.expandConfig?.panels?.[0] || null
}

function updateAiCrudExpandEnabled(enabled) {
  if (!selectedBlock.value)
    return
  const current = selectedBlock.value.props?.expandConfig || {}
  const nextConfig = enabled
    ? {
        enabled: true,
        trigger: current.trigger || 'icon',
        lazy: current.lazy !== false,
        cache: current.cache !== false,
        layout: current.layout || { mode: 'single', density: 'compact', padding: 12 },
        panels: current.panels?.length ? current.panels : [createDefaultAiCrudExpandPanel(selectedBlock.value)],
      }
    : { ...current, enabled: false }
  patchBlockProps(selectedBlock.value.id, { expandConfig: nextConfig })
}

function updateAiCrudExpandConfig(patch = {}) {
  if (!selectedBlock.value)
    return
  const current = selectedBlock.value.props?.expandConfig || {}
  patchBlockProps(selectedBlock.value.id, {
    expandConfig: {
      ...current,
      ...patch,
      enabled: current.enabled === true,
    },
  })
}

function updateFirstAiCrudExpandPanel(patch = {}) {
  if (!selectedBlock.value)
    return
  const current = selectedBlock.value.props?.expandConfig || {}
  const panels = current.panels?.length ? [...current.panels] : [createDefaultAiCrudExpandPanel(selectedBlock.value)]
  panels[0] = normalizeAiCrudExpandPanelPatch({ ...panels[0], ...patch })
  patchBlockProps(selectedBlock.value.id, {
    expandConfig: {
      ...current,
      enabled: true,
      panels,
    },
  })
}

function updateFirstAiCrudExpandDataSource(patch = {}) {
  const panel = firstExpandPanel(selectedBlock.value) || createDefaultAiCrudExpandPanel(selectedBlock.value)
  updateFirstAiCrudExpandPanel({
    dataSource: {
      ...(panel.dataSource || {}),
      ...patch,
    },
  })
}

function updateFirstAiCrudExpandParamsMap(value) {
  updateFirstAiCrudExpandDataSource({ paramsMap: parseJsonObjectProp(value) })
}

function updateFirstAiCrudDescriptionFields(value) {
  const fields = Array.isArray(value)
    ? value.map(item => String(item || '').trim()).filter(Boolean)
    : String(value || '').split(/\r?\n|,/).map(item => item.trim()).filter(Boolean)
  updateFirstAiCrudExpandPanel({
    descriptions: {
      ...(firstExpandPanel(selectedBlock.value)?.descriptions || {}),
      fields: fields.map(field => ({ field, label: resolveFieldLabelByCode(field) })),
    },
  })
}

function resolveExpandDescriptionFieldKeys(panel, block = {}) {
  const configured = (panel?.descriptions?.fields || [])
    .map(field => field.field || field.key)
    .filter(Boolean)
  if (configured.length)
    return configured
  return resolveAiCrudDefaultDescriptionFields(block).map(field => field.field).filter(Boolean)
}

function resolveExpandDescriptionFieldOptions() {
  const sourceFields = crudTablePanelFields.value.length
    ? crudTablePanelFields.value
    : props.fields || []
  const seen = new Set()
  return sourceFields
    .map((field) => {
      const value = resolveCrudFieldKey(field)
      if (!value || seen.has(value))
        return null
      seen.add(value)
      const rawLabel = resolveCrudFieldLabel(field)
      return {
        label: `${rawLabel}（${value}）`,
        rawLabel,
        value,
      }
    })
    .filter(Boolean)
}

function resolveExpandDescriptionSelectedFields(panel, block = {}) {
  const optionMap = new Map(resolveExpandDescriptionFieldOptions(block).map(option => [option.value, option]))
  return resolveExpandDescriptionFieldKeys(panel, block).map((field) => {
    const option = optionMap.get(field)
    return {
      field,
      label: option?.rawLabel || resolveFieldLabelByCode(field),
    }
  })
}

function resolveExpandDescriptionPanelFields(panel, block = {}) {
  const selectedKeys = resolveExpandDescriptionFieldKeys(panel, block)
  const selected = new Set(selectedKeys)
  const optionMap = new Map(resolveExpandDescriptionFieldOptions(block).map(option => [option.value, option]))
  const orderedKeys = [
    ...selectedKeys,
    ...Array.from(optionMap.keys()).filter(key => !selected.has(key)),
  ]
  return orderedKeys
    .map((field) => {
      const option = optionMap.get(field)
      if (!option && !selected.has(field))
        return null
      return {
        field,
        label: option?.rawLabel || resolveFieldLabelByCode(field),
        selected: selected.has(field),
      }
    })
    .filter(Boolean)
}

function addFirstAiCrudDescriptionField(field) {
  if (!field)
    return
  const fields = resolveExpandDescriptionFieldKeys(firstExpandPanel(selectedBlock.value), selectedBlock.value)
  updateFirstAiCrudDescriptionFields(Array.from(new Set([...fields, field])))
}

function removeFirstAiCrudDescriptionField(field) {
  const fields = resolveExpandDescriptionFieldKeys(firstExpandPanel(selectedBlock.value), selectedBlock.value)
  updateFirstAiCrudDescriptionFields(fields.filter(item => item !== field))
}

function toggleFirstAiCrudDescriptionField(field, visible) {
  if (visible)
    addFirstAiCrudDescriptionField(field)
  else
    removeFirstAiCrudDescriptionField(field)
}

function handleFirstAiCrudDescriptionPanelReorder(fields = []) {
  updateFirstAiCrudDescriptionFields(fields.filter(field => field?.selected).map(field => field.field))
}

function createDefaultAiCrudExpandPanel(block = {}) {
  const fields = resolveAiCrudDefaultDescriptionFields(block)
  return {
    key: 'summary',
    title: '概览',
    type: 'descriptions',
    dataSource: { type: 'row' },
    descriptions: {
      columns: 3,
      fields,
    },
  }
}

function normalizeAiCrudExpandPanelPatch(panel = {}) {
  const type = panel.type || 'descriptions'
  const quantityPanel = ['quantity-balance', 'quantity-ledger', 'quantity-lock'].includes(type)
  const key = panel.key || (type === 'table' ? 'detailTable' : quantityPanel ? type : 'summary')
  return {
    ...panel,
    key,
    title: panel.title || (type === 'table' ? '明细' : quantityPanel ? expandPanelTypeOptions.find(item => item.value === type)?.label : '概览'),
    type,
    dataSource: quantityPanel ? { ...(panel.dataSource || {}), type: 'quantity', queryType: type } : panel.dataSource || { type: 'row' },
    quantity: quantityPanel ? { ...(panel.quantity || {}), queryType: type } : panel.quantity,
    descriptions: panel.descriptions || { columns: 3, fields: resolveAiCrudDefaultDescriptionFields(selectedBlock.value) },
    table: panel.table || { rowKey: 'id', columns: [], pagination: false, maxHeight: 320 },
  }
}

function resolveAiCrudDefaultDescriptionFields(block = {}) {
  const refs = Array.isArray(block.props?.tableFieldRefs) && block.props.tableFieldRefs.length
    ? block.props.tableFieldRefs
    : Array.isArray(block.fieldRefs) ? block.fieldRefs : []
  return refs.slice(0, 6).map(field => ({ field, label: resolveFieldLabelByCode(field) }))
}

function resolveFieldLabelByCode(fieldCode) {
  const field = props.fields.find(item => item.field === fieldCode || item.fieldCode === fieldCode || item.key === fieldCode)
  return field?.label || field?.title || fieldCode
}

function stringifyJsonProp(value) {
  if (value === undefined || value === null || value === '')
    return ''
  if (typeof value === 'string')
    return value
  return JSON.stringify(value, null, 2)
}

function parseJsonObjectProp(value) {
  const text = String(value || '').trim()
  if (!text)
    return {}
  try {
    const parsed = JSON.parse(text)
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {}
  }
  catch {
    return {}
  }
}

function resolveAiCrudEditFlags(block = {}) {
  const props = block.props || {}
  const flags = []
  if (props.editShowFeedback !== false)
    flags.push('editShowFeedback')
  if (props.hideModalFooter === true)
    flags.push('hideModalFooter')
  if (props.hideDefaultDetailContent === true)
    flags.push('hideDefaultDetailContent')
  return flags
}

function updateAiCrudEditFlags(values = []) {
  if (!selectedBlock.value)
    return
  patchBlockProps(selectedBlock.value.id, {
    editShowFeedback: values.includes('editShowFeedback'),
    hideModalFooter: values.includes('hideModalFooter'),
    hideDefaultDetailContent: values.includes('hideDefaultDetailContent'),
  })
}

function isAiCrudToolbarSwitchOn(block = {}, key) {
  const props = block.props || {}
  if (key === 'add')
    return props.hideAdd !== true
  if (key === 'batchDelete')
    return props.hideBatchDelete !== true
  if (key === 'import')
    return props.showImport !== false
  if (key === 'export')
    return props.showExport !== false
  if (key === 'customQuery')
    return props.enableCustomQuery !== false
  if (key === 'exportTasks')
    return props.showExportTasks !== false
  return false
}

function updateAiCrudToolbarSwitch(key, checked) {
  if (!selectedBlock.value)
    return
  const value = checked === true
  const propPatchMap = {
    add: { hideAdd: !value },
    batchDelete: { hideBatchDelete: !value },
    import: { showImport: value },
    export: { showExport: value },
    customQuery: { enableCustomQuery: value },
    exportTasks: { showExportTasks: value },
  }
  patchBlockProps(selectedBlock.value.id, propPatchMap[key] || {})
}

function updateSelectedBlockHookRules(rules) {
  if (!selectedBlock.value)
    return
  patchBlockProps(selectedBlock.value.id, { crudHookRules: rules || {}, beforeSubmitRules: [] })
}

function resolveSelectedBlockDefaultParams(block = {}) {
  const props = block?.props || {}
  return {
    publicParams: props.publicParams || {},
    publicQuery: props.publicQuery || {},
    formDefaultValues: props.formDefaultValues || {},
    submitDefaultParams: props.submitDefaultParams || {},
  }
}

function updateSelectedBlockDefaultParams(params = {}) {
  if (!selectedBlock.value)
    return
  if (isSameDefaultParams(resolveSelectedBlockDefaultParams(selectedBlock.value), params))
    return
  patchBlockProps(selectedBlock.value.id, {
    publicParams: params.publicParams || {},
    publicQuery: params.publicQuery || {},
    formDefaultValues: params.formDefaultValues || {},
    submitDefaultParams: params.submitDefaultParams || {},
  })
}

function isSameDefaultParams(left = {}, right = {}) {
  return JSON.stringify(normalizeDefaultParams(left)) === JSON.stringify(normalizeDefaultParams(right))
}

function normalizeDefaultParams(source = {}) {
  return ['publicParams', 'publicQuery', 'formDefaultValues', 'submitDefaultParams'].reduce((result, key) => {
    const params = source[key] && typeof source[key] === 'object' && !Array.isArray(source[key])
      ? source[key]
      : {}
    result[key] = Object.keys(params).sort().reduce((next, paramKey) => {
      next[paramKey] = params[paramKey]
      return next
    }, {})
    return result
  }, {})
}

function resolveAiTableVisibleFlags(block = {}) {
  const props = block.props || {}
  const flags = []
  if (props.showToolbar !== false)
    flags.push('showToolbar')
  if (props.showPagination === true)
    flags.push('showPagination')
  if (props.hideSelection !== true)
    flags.push('showSelection')
  if (props.showRefresh !== false)
    flags.push('showRefresh')
  if (props.showDensity !== false)
    flags.push('showDensity')
  if (props.showColumnFilter !== false)
    flags.push('showColumnFilter')
  if (props.showSearchToggle === true)
    flags.push('showSearchToggle')
  if (props.showFullscreen === true)
    flags.push('showFullscreen')
  if (props.showRenderModeSwitch !== false)
    flags.push('showRenderModeSwitch')
  return flags
}

function updateAiTableVisibleFlags(values = []) {
  if (!selectedBlock.value)
    return
  patchBlockProps(selectedBlock.value.id, {
    showToolbar: values.includes('showToolbar'),
    showPagination: values.includes('showPagination'),
    hideSelection: !values.includes('showSelection'),
    showRefresh: values.includes('showRefresh'),
    showDensity: values.includes('showDensity'),
    showColumnFilter: values.includes('showColumnFilter'),
    showSearchToggle: values.includes('showSearchToggle'),
    showFullscreen: values.includes('showFullscreen'),
    showRenderModeSwitch: values.includes('showRenderModeSwitch'),
  })
}

function resolveAiTableStyleFlags(block = {}) {
  const props = block.props || {}
  const flags = []
  if (props.bordered !== false)
    flags.push('bordered')
  if (props.striped === true)
    flags.push('striped')
  if (props.singleLine === true)
    flags.push('singleLine')
  return flags
}

function updateAiTableStyleFlags(values = []) {
  if (!selectedBlock.value)
    return
  patchBlockProps(selectedBlock.value.id, {
    bordered: values.includes('bordered'),
    striped: values.includes('striped'),
    singleLine: values.includes('singleLine'),
  })
}

function resolveAiFormFlags(block = {}) {
  const props = block.props || {}
  const flags = []
  if (props.showActions !== false)
    flags.push('showActions')
  if (props.showSubmit !== false)
    flags.push('showSubmit')
  if (props.showReset !== false)
    flags.push('showReset')
  if (props.showCancel === true)
    flags.push('showCancel')
  if (props.enableCollapse === true)
    flags.push('enableCollapse')
  if (props.showFeedback !== false)
    flags.push('showFeedback')
  return flags
}

function updateAiFormFlags(values = []) {
  if (!selectedBlock.value)
    return
  patchBlockProps(selectedBlock.value.id, {
    showActions: values.includes('showActions'),
    showSubmit: values.includes('showSubmit'),
    showReset: values.includes('showReset'),
    showCancel: values.includes('showCancel'),
    enableCollapse: values.includes('enableCollapse'),
    showFeedback: values.includes('showFeedback'),
  })
}

function isBlockDisabled(item) {
  return !!resolveBlockDisabledReason(item)
}

function resolveBlockDisabledReason(item = {}) {
  if (item.unique && findExistingBlockByType(item.blockType))
    return '已在画布中'
  if (item.onlyFor && !item.onlyFor.includes(props.layoutType))
    return '当前布局不可用'
  return ''
}

function findExistingBlockByType(blockType) {
  return blocks.value.find(block => blockContainsType(block, blockType))
}

function blockContainsType(block = {}, blockType = '') {
  if (block.blockType === blockType)
    return true
  if ((block.children || []).some(child => blockContainsType(child, blockType)))
    return true
  if ((block.props?.tabs || []).some(tab => (tab.children || []).some(child => blockContainsType(child, blockType))))
    return true
  if ((block.props?.cells || []).some(cell => (cell.children || []).some(child => blockContainsType(child, blockType))))
    return true
  return false
}

function handlePaletteDragStart(event, item) {
  if (props.readonly) {
    event.preventDefault()
    return
  }
  if (isBlockDisabled(item)) {
    event.preventDefault()
    return
  }
  draggedBlockType.value = item.blockType
  event.dataTransfer.effectAllowed = 'copy'
  event.dataTransfer.setData('application/x-list-block', item.blockType)
}

function handleNestedBlockDragStart(payload = {}) {
  const block = payload.block || {}
  if (!block.id)
    return
  selectBlock(block.id)
  draggedExistingBlockId.value = block.id
  draggedBlockType.value = block.blockType || ''
  canvasDragActive.value = true
}

function handlePaletteClick(item) {
  if (props.readonly)
    return
  const existingBlock = item.unique ? findExistingBlockByType(item.blockType) : null
  if (existingBlock) {
    selectBlock(existingBlock.id)
    scrollBlockIntoView(existingBlock)
    return
  }
  const disabledReason = resolveBlockDisabledReason(item)
  if (disabledReason) {
    window.$message?.info(disabledReason)
    return
  }
  appendBlock(item.blockType)
}

function scrollBlockIntoView(block) {
  const scrollEl = canvasScrollRef.value
  if (!scrollEl || !block)
    return
  nextTick(() => {
    const rect = resolveBlockFrame(block)
    const zoom = canvasZoom.value || 1
    scrollEl.scrollTo({
      left: Math.max(0, rect.x * zoom - 32),
      top: Math.max(0, rect.y * zoom - 32),
      behavior: 'smooth',
    })
  })
}

function resolveBlockMoreOptions(block = {}) {
  const index = blocks.value.findIndex(item => item.id === block.id)
  const meta = resolveListPageBlockMeta(block.blockType)
  return [
    { label: '复制区块', key: 'duplicate', disabled: !!meta?.unique },
    { label: '上移一行', key: 'moveUp' },
    { label: '下移一行', key: 'moveDown' },
    { label: '置顶', key: 'moveTop' },
    { label: '置底', key: 'moveBottom' },
    { type: 'divider', key: 'divider' },
    { label: '前移一层', key: 'layerForward', disabled: index >= blocks.value.length - 1 },
    { label: '后移一层', key: 'layerBackward', disabled: index <= 0 },
    { type: 'divider', key: 'dangerDivider' },
    { label: '删除', key: 'delete' },
  ]
}

function handleBlockMoreSelect(key, block = {}) {
  if (!block?.id)
    return
  selectBlock(block.id)
  if (key === 'duplicate') {
    duplicateBlock(block.id)
    return
  }
  if (key === 'moveUp') {
    patchBlock(block.id, { gridY: Math.max(0, Number(block.gridY) - 1) })
    return
  }
  if (key === 'moveDown') {
    patchBlock(block.id, { gridY: Number(block.gridY) + 1 })
    return
  }
  if (key === 'moveTop') {
    patchBlock(block.id, { gridY: 0 })
    return
  }
  if (key === 'moveBottom') {
    const bottomRow = blocks.value
      .filter(item => item.id !== block.id)
      .reduce((acc, item) => Math.max(acc, item.gridY + item.gridH), 0)
    patchBlock(block.id, { gridY: bottomRow })
    return
  }
  if (key === 'layerForward') {
    reorderBlockLayer(block.id, 1)
    return
  }
  if (key === 'layerBackward') {
    reorderBlockLayer(block.id, -1)
    return
  }
  if (key === 'delete')
    removeBlock(block.id)
}

function handleNestedBlockMenuSelect(payload = {}) {
  const key = payload.key
  const block = payload.block || {}
  if (!block.id)
    return
  selectBlock(block.id)
  if (key === 'duplicate') {
    duplicateBlock(block.id)
    return
  }
  if (key === 'delete')
    removeBlock(block.id)
}

function handleTreeSourceChange(sourceModelCode) {
  if (!selectedBlock.value)
    return
  const defaultConfig = resolveDefaultTreeConfig(props.modelSchema, { sourceModelCode })
  patchBlockProps(selectedBlock.value.id, defaultConfig)
}

function handleCanvasDrop(event) {
  if (props.readonly)
    return
  const existingBlockId = event.dataTransfer?.getData('application/x-list-existing-block') || draggedExistingBlockId.value
  const blockType = event.dataTransfer?.getData('application/x-list-block')
  if (existingBlockId) {
    const container = resolveDropContainer(event)
    if (!container && resolveNonContainerDropBlock(event, existingBlockId)) {
      resetCanvasDragState()
      return
    }
    if (container?.block?.blockType === 'grid-layout' && container.cellKey) {
      resetCanvasDragState()
      moveExistingBlockToGridCell(existingBlockId, container.id, container.cellKey)
      return
    }
    if (container?.block && container.block.blockType !== 'grid-layout' && container.block.id !== existingBlockId) {
      resetCanvasDragState()
      moveExistingBlockToContainer(existingBlockId, container.id)
      return
    }
    const point = pixelToPoint(event.clientX, event.clientY)
    resetCanvasDragState()
    moveExistingBlockToCanvas(existingBlockId, point)
    return
  }
  resetCanvasDragState()
  if (!blockType)
    return
  const meta = resolveListPageBlockMeta(blockType)
  if (!meta)
    return
  if (meta.unique && findExistingBlockByType(blockType))
    return
  const container = resolveDropContainer(event)
  if (container) {
    appendContainerChild(container.id, blockType, container.cellKey)
    return
  }
  if (resolveNonContainerDropBlock(event)) {
    resetCanvasDragState()
    return
  }
  const point = pixelToPoint(event.clientX, event.clientY)
  appendBlock(blockType, point)
}

function handleCanvasDragEnter(event) {
  if (props.readonly)
    return
  const blockType = event.dataTransfer?.getData('application/x-list-block') || draggedBlockType.value
  const existingBlockId = event.dataTransfer?.getData('application/x-list-existing-block') || draggedExistingBlockId.value
  if (!blockType && !existingBlockId)
    return
  canvasDragActive.value = true
  draggedExistingBlockId.value = existingBlockId || ''
  draggedBlockType.value = blockType || findBlockInTree(blocks.value, existingBlockId)?.blockType || ''
}

function handleCanvasDragOver(event) {
  if (props.readonly)
    return
  const blockType = event.dataTransfer?.getData('application/x-list-block') || draggedBlockType.value
  const existingBlockId = event.dataTransfer?.getData('application/x-list-existing-block') || draggedExistingBlockId.value
  if (!blockType && !existingBlockId)
    return
  canvasDragActive.value = true
  draggedExistingBlockId.value = existingBlockId || ''
  draggedBlockType.value = blockType || findBlockInTree(blocks.value, existingBlockId)?.blockType || ''
  autoScrollCanvasOnPointer(event)
  const container = resolveDropContainer(event)
  dragBlockedBlockId.value = !container ? resolveNonContainerDropBlock(event, existingBlockId)?.id || '' : ''
  activeDropCell.value = container?.block?.blockType === 'grid-layout' && container.cellKey
    ? { containerId: container.id, cellKey: container.cellKey }
    : null
  dragOverCell.value = activeDropCell.value || dragBlockedBlockId.value ? null : pixelToCell(event.clientX, event.clientY)
  dragOverPoint.value = pixelToPoint(event.clientX, event.clientY)
}

function handleCanvasDragLeave(event) {
  if (props.readonly)
    return
  if (event.currentTarget?.contains?.(event.relatedTarget))
    return
  resetCanvasDragState()
}

function resetCanvasDragState() {
  canvasDragActive.value = false
  draggedBlockType.value = ''
  draggedExistingBlockId.value = ''
  dragOverCell.value = null
  dragOverPoint.value = null
  dragBlockedBlockId.value = ''
  activeDropCell.value = null
}

function resolveAutoScrollStep(distanceToEdge) {
  const ratio = clamp((CANVAS_AUTO_SCROLL_EDGE - distanceToEdge) / CANVAS_AUTO_SCROLL_EDGE, 0, 1)
  return Math.max(1, Math.round(ratio * CANVAS_AUTO_SCROLL_MAX_STEP))
}

function autoScrollCanvasOnPointer(event) {
  const scrollEl = canvasScrollRef.value
  if (!scrollEl)
    return
  const rect = scrollEl.getBoundingClientRect()
  let nextLeft = scrollEl.scrollLeft
  let nextTop = scrollEl.scrollTop

  if (event.clientX < rect.left + CANVAS_AUTO_SCROLL_EDGE) {
    nextLeft -= resolveAutoScrollStep(event.clientX - rect.left)
  }
  else if (event.clientX > rect.right - CANVAS_AUTO_SCROLL_EDGE) {
    nextLeft += resolveAutoScrollStep(rect.right - event.clientX)
  }

  if (event.clientY < rect.top + CANVAS_AUTO_SCROLL_EDGE) {
    nextTop -= resolveAutoScrollStep(event.clientY - rect.top)
  }
  else if (event.clientY > rect.bottom - CANVAS_AUTO_SCROLL_EDGE) {
    nextTop += resolveAutoScrollStep(rect.bottom - event.clientY)
  }

  nextLeft = clamp(nextLeft, 0, scrollEl.scrollWidth - scrollEl.clientWidth)
  nextTop = clamp(nextTop, 0, scrollEl.scrollHeight - scrollEl.clientHeight)
  if (nextLeft !== scrollEl.scrollLeft)
    scrollEl.scrollLeft = nextLeft
  if (nextTop !== scrollEl.scrollTop)
    scrollEl.scrollTop = nextTop
}

function appendBlock(blockType, position) {
  const meta = resolveListPageBlockMeta(blockType)
  if (!meta)
    return
  if (meta.unique && blocks.value.some(block => block.blockType === blockType))
    return
  const point = position || { x: 0, y: nextFreePixelTop() }
  const gridPatch = frameToGridPatch({
    x: point.x || 0,
    y: point.y || 0,
    width: gridWidthToPixels(meta.defaultW || 4),
    height: gridHeightToPixels(meta.defaultH || 2),
  })
  const block = createGridBlock(blockType, props.modelSchema, gridPatch)
  if (!block)
    return
  const frame = resolveCanvasDropFrame('', {
    x: clamp(Number(point.x) || 0, 0, Math.max(0, canvasGridWidth.value - gridWidthToPixels(block.gridW))),
    y: Math.max(0, Number(point.y) || 0),
    width: gridWidthToPixels(block.gridW),
    height: gridHeightToPixels(block.gridH),
  })
  Object.assign(block, frameToGridPatch(frame))
  block.props = {
    ...(block.props || {}),
    style: {
      ...createDefaultBlockStyle(),
      ...(block.props?.style || {}),
      ...frame,
    },
  }
  if (block.gridX + block.gridW > LIST_PAGE_GRID_COLS)
    block.gridX = Math.max(0, LIST_PAGE_GRID_COLS - block.gridW)
  localLayout.value = {
    ...localLayout.value,
    items: normalizeGridItems([...blocks.value, block]),
  }
  selectBlock(block.id)
}

function resolveDropContainer(event) {
  const match = resolveClosestContainerBlock(event.target)
  if (!match?.block)
    return null
  const cellNode = event.target?.closest?.('[data-grid-cell-key][data-grid-container-id]')
  const cellKey = match.block.blockType === 'grid-layout' && cellNode?.dataset?.gridContainerId === match.block.id
    ? cellNode.dataset.gridCellKey || ''
    : ''
  return {
    block: match.block,
    id: match.block.id,
    cellKey,
  }
}

function resolveDropContainerFromPoint(clientX, clientY) {
  const target = document.elementFromPoint(clientX, clientY)
  const match = resolveClosestContainerBlock(target)
  if (match?.block) {
    const cellNode = target?.closest?.('[data-grid-cell-key][data-grid-container-id]')
    if (match.block.blockType === 'grid-layout' && cellNode?.dataset?.gridContainerId === match.block.id) {
      return {
        block: match.block,
        id: match.block.id,
        cellKey: cellNode.dataset.gridCellKey || '',
        cellRect: cellNode.getBoundingClientRect?.() || null,
      }
    }
    return {
      block: match.block,
      id: match.block.id,
      cellKey: '',
    }
  }
  const cellNode = target?.closest?.('[data-grid-cell-key][data-grid-container-id]')
  if (cellNode) {
    const containerId = cellNode.dataset.gridContainerId || ''
    const block = findBlockInTree(blocks.value, containerId)
    if (block?.blockType === 'grid-layout') {
      return {
        block,
        id: block.id,
        cellKey: cellNode.dataset.gridCellKey || '',
        cellRect: cellNode.getBoundingClientRect?.() || null,
      }
    }
  }
  return null
}

function resolveNonContainerDropBlock(event, sourceId = '') {
  const target = document.elementFromPoint(event.clientX, event.clientY) || event.target
  const node = target?.closest?.('[data-block-id]')
  if (!node)
    return null
  const block = findBlockInTree(blocks.value, node.dataset?.blockId || '')
  if (!block || block.id === sourceId || isContainerBlock(block))
    return null
  return block
}

function resolveClosestContainerBlock(target) {
  let node = target?.closest?.('[data-block-id]')
  while (node) {
    const block = findBlockInTree(blocks.value, node.dataset?.blockId || '')
    if (isContainerBlock(block))
      return { node, block }
    node = node.parentElement?.closest?.('[data-block-id]')
  }
  return null
}

function isContainerBlock(block = {}) {
  return ['card', 'tabs', 'grid-layout', 'box-layout'].includes(block?.blockType)
}

function appendContainerChild(containerId, blockType, cellKey = '') {
  const container = findBlockInTree(blocks.value, containerId)
  if (!container || !blockType)
    return
  if (container.blockType === 'tabs') {
    appendTabChild(blockType, containerId)
    selectBlock(containerId)
    return
  }
  if (container.blockType === 'box-layout') {
    const child = createContainerChildBlock(blockType)
    if (!child)
      return
    localLayout.value = {
      ...localLayout.value,
      items: normalizeGridItems(mapBlocksInTree(blocks.value, block => block.id === containerId
        ? { ...block, children: [...(block.children || []), child] }
        : block)),
    }
    selectBlock(child.id)
    return
  }
  if (container.blockType === 'grid-layout') {
    const child = appendGridCellChild(containerId, cellKey || normalizeGridLayoutCells(container)[0]?.key, blockType)
    selectBlock(child?.id || containerId)
    return
  }
  const child = createContainerChildBlock(blockType)
  if (!child)
    return
  localLayout.value = {
    ...localLayout.value,
    items: normalizeGridItems(mapBlocksInTree(blocks.value, block => block.id === containerId
      ? { ...block, children: [...(block.children || []), child] }
      : block)),
  }
  selectBlock(child.id)
}

function createContainerChildBlock(blockType) {
  const child = createGridBlock(blockType, props.modelSchema, { gridX: 0, gridY: 0 })
  if (!child)
    return null
  const childHeight = resolveNestedChildHeight(child)
  return {
    ...child,
    id: `${child.id}_child_${Date.now()}`,
    gridX: 0,
    gridY: 0,
    gridW: LIST_PAGE_GRID_COLS,
    gridH: Math.max(1, child.gridH || 2),
    props: {
      ...(child.props || {}),
      style: {
        ...createDefaultBlockStyle(),
        ...(child.props?.style || {}),
        x: 0,
        y: 0,
        widthMode: 'full',
        width: '100%',
        height: child.props?.style?.height || childHeight,
      },
    },
  }
}

function removeContainerChild(containerId, childId) {
  localLayout.value = {
    ...localLayout.value,
    items: blocks.value.map(block => block.id === containerId
      ? { ...block, children: (block.children || []).filter(child => child.id !== childId) }
      : block),
  }
}

function normalizeGridLayoutCells(block = {}) {
  if (block?.blockType !== 'grid-layout')
    return []
  const columns = Math.max(1, Math.min(24, Number(block.props?.columns || 24)))
  const cells = Array.isArray(block.props?.cells) ? block.props.cells : []
  const sourceCells = cells.length ? cells : [{ key: 'cell_1', title: '栅格 1', span: columns, children: [] }]
  return sourceCells.map((cell, index) => {
    const children = Array.isArray(cell.children) ? cell.children : []
    return {
      key: cell.key || `cell_${index + 1}`,
      title: cell.title ?? `栅格 ${index + 1}`,
      span: clamp(Number(cell.span) || 6, 1, columns),
      children,
      minHeight: Math.max(Number(cell.minHeight || 0), resolveGridCellAutoMinHeight(children)),
    }
  })
}

function resolveNestedChildHeight(child = {}) {
  const styleHeight = resolveCssNumber(child.props?.style?.height, 0)
  if (styleHeight)
    return styleHeight
  return Math.max(72, gridHeightToPixels(child.gridH || 2))
}

function resolveGridCellAutoMinHeight(children = []) {
  if (!children.length)
    return 120
  return children.reduce((sum, child) => sum + resolveNestedChildHeight(child), 0) + Math.max(0, children.length - 1) * 8 + 16
}

function patchGridLayoutCells(containerId, updater) {
  localLayout.value = {
    ...localLayout.value,
    items: blocks.value.map((block) => {
      if (block.id !== containerId || block.blockType !== 'grid-layout')
        return block
      const cells = updater(normalizeGridLayoutCells(block))
      return {
        ...block,
        props: {
          ...(block.props || {}),
          cells,
        },
      }
    }),
  }
}

function appendGridCellChild(containerId, cellKey, blockType) {
  const container = findBlockInTree(blocks.value, containerId)
  if (!container || container.blockType !== 'grid-layout' || !blockType)
    return null
  const child = createContainerChildBlock(blockType)
  if (!child)
    return null
  const targetKey = cellKey || normalizeGridLayoutCells(container)[0]?.key
  localLayout.value = {
    ...localLayout.value,
    items: normalizeGridItems(mapBlocksInTree(blocks.value, (block) => {
      if (block.id !== containerId || block.blockType !== 'grid-layout')
        return block
      const cells = normalizeGridLayoutCells(block).map((cell, index) => {
        const match = cell.key === targetKey || (!targetKey && index === 0)
        return match ? { ...cell, children: [...(cell.children || []), child] } : cell
      })
      return {
        ...block,
        props: {
          ...(block.props || {}),
          cells,
        },
      }
    })),
  }
  return child
}

function removeGridCellChild(containerId, cellKey, childId) {
  patchGridLayoutCells(containerId, cells => cells.map(cell => cell.key === cellKey
    ? { ...cell, children: (cell.children || []).filter(child => child.id !== childId) }
    : cell))
}

function moveExistingBlockToGridCell(blockId, containerId, cellKey, stylePatch = {}) {
  if (!blockId || !containerId || blockId === containerId)
    return
  const source = findBlockInTree(blocks.value, blockId)
  const container = findBlockInTree(blocks.value, containerId)
  if (!source || container?.blockType !== 'grid-layout')
    return
  const cleanedItems = removeBlockFromTree(blocks.value, blockId)
  const movedSource = {
    ...source,
    props: {
      ...(source.props || {}),
      style: {
        ...createDefaultBlockStyle(),
        ...(source.props?.style || {}),
        ...stylePatch,
      },
    },
  }
  const targetKey = cellKey || normalizeGridLayoutCells(container)[0]?.key
  localLayout.value = {
    ...localLayout.value,
    items: normalizeGridItems(mapBlocksInTree(cleanedItems, (block) => {
      if (block.id !== containerId || block.blockType !== 'grid-layout')
        return block
      const cells = normalizeGridLayoutCells(block).map((cell, index) => {
        const match = cell.key === targetKey || (!targetKey && index === 0)
        return match ? { ...cell, children: [...(cell.children || []), movedSource] } : cell
      })
      return {
        ...block,
        props: {
          ...(block.props || {}),
          cells,
        },
      }
    })),
  }
  selectBlock(blockId)
}

function moveExistingBlockToContainer(blockId, containerId) {
  if (!blockId || !containerId || blockId === containerId)
    return
  const source = findBlockInTree(blocks.value, blockId)
  const container = findBlockInTree(blocks.value, containerId)
  if (!source || !isContainerBlock(container) || container.blockType === 'grid-layout')
    return
  if (collectBlocksInTree(source).some(block => block.id === containerId))
    return
  const movedSource = {
    ...source,
    gridX: 0,
    gridY: 0,
    gridW: LIST_PAGE_GRID_COLS,
    props: {
      ...(source.props || {}),
      style: {
        ...createDefaultBlockStyle(),
        ...(source.props?.style || {}),
        x: 0,
        y: 0,
        widthMode: 'full',
        width: '100%',
      },
    },
  }
  const cleanedItems = removeBlockFromTree(blocks.value, blockId)
  localLayout.value = {
    ...localLayout.value,
    items: normalizeGridItems(mapBlocksInTree(cleanedItems, (block) => {
      if (block.id !== containerId)
        return block
      if (block.blockType === 'tabs') {
        const tabs = block.props?.tabs?.length
          ? block.props.tabs
          : [{ key: 'tab_1', title: '标签 1', children: [] }]
        return {
          ...block,
          props: {
            ...(block.props || {}),
            tabs: tabs.map((tab, index) => index === 0
              ? { ...tab, children: [...(tab.children || []), movedSource] }
              : tab),
          },
        }
      }
      return {
        ...block,
        children: [...(block.children || []), movedSource],
      }
    })),
  }
  selectBlock(blockId)
}

function moveExistingBlockToCanvas(blockId, point = { x: 0, y: 0 }) {
  if (!blockId)
    return
  const source = findBlockInTree(blocks.value, blockId)
  if (!source)
    return
  const meta = resolveListPageBlockMeta(source.blockType)
  const sourceFrame = resolveDetachedBlockFrame(source, meta)
  const width = Math.min(sourceFrame.width, canvasGridWidth.value)
  const height = sourceFrame.height
  const frame = resolveCanvasDropFrame(blockId, {
    x: clamp(Number(point.x) || 0, 0, Math.max(0, canvasGridWidth.value - width)),
    y: Math.max(0, Number(point.y) || 0),
    width,
    height,
  })
  const gridPatch = frameToGridPatch(frame)
  const movedBlock = {
    ...source,
    ...gridPatch,
    props: {
      ...(source.props || {}),
      style: {
        ...createDefaultBlockStyle(),
        ...(source.props?.style || {}),
        ...frame,
        widthMode: 'fixed',
        width: frame.width,
        height: frame.height,
      },
    },
  }
  localLayout.value = {
    ...localLayout.value,
    items: normalizeGridItems([...removeBlockFromTree(blocks.value, blockId), movedBlock]),
  }
  selectBlock(blockId)
}

function updateGridCell(index, patch) {
  if (!selectedBlock.value || selectedBlock.value.blockType !== 'grid-layout')
    return
  patchGridLayoutCells(selectedBlock.value.id, cells => cells.map((cell, idx) => idx === index ? { ...cell, ...patch } : cell))
}

function updateGridLayoutStructure(patch = {}) {
  if (!selectedBlock.value || selectedBlock.value.blockType !== 'grid-layout')
    return
  const current = selectedBlock.value
  const nextColumns = clamp(Number(patch.columns ?? current.props?.columns ?? 24), 1, 24)
  const nextCells = normalizeGridLayoutCells(current).map(cell => ({
    ...cell,
    span: clamp(Number(cell.span) || 1, 1, nextColumns),
  }))
  patchBlockProps(current.id, {
    ...patch,
    columns: nextColumns,
    cells: nextCells,
  })
}

function addGridCell() {
  if (!selectedBlock.value || selectedBlock.value.blockType !== 'grid-layout')
    return
  const cells = normalizeGridLayoutCells(selectedBlock.value)
  const columns = Math.max(1, Number(selectedBlock.value.props?.columns || 24))
  const usedSpan = cells.reduce((sum, cell) => sum + (Number(cell.span) || 0), 0)
  const remainder = usedSpan % columns
  const nextSpan = clamp(remainder > 0 ? columns - remainder : Math.min(6, columns), 1, columns)
  const nextCells = [
    ...cells,
    {
      key: `cell_${Date.now()}`,
      title: `栅格 ${cells.length + 1}`,
      span: nextSpan,
      children: [],
    },
  ]
  patchBlockProps(selectedBlock.value.id, {
    cells: nextCells,
  })
}

function removeGridCell(index) {
  if (!selectedBlock.value || selectedBlock.value.blockType !== 'grid-layout')
    return
  const cells = normalizeGridLayoutCells(selectedBlock.value)
  if (cells.length <= 1)
    return
  const nextCells = cells.filter((_, idx) => idx !== index)
  patchBlockProps(selectedBlock.value.id, {
    cells: nextCells,
  })
}

function appendTabChild(blockType, containerId = selectedBlock.value?.id) {
  const container = blocks.value.find(block => block.id === containerId)
  if (!container || container.blockType !== 'tabs')
    return
  const child = createContainerChildBlock(blockType)
  if (!child)
    return
  const tabs = container.props?.tabs?.length
    ? container.props.tabs
    : [{ key: 'tab1', title: '标签一', children: [] }]
  const targetKey = tabs.some(tab => tab.key === activeTabKey.value)
    ? activeTabKey.value
    : tabs[0]?.key
  const nextTabs = tabs.map(tab => (tab.key === targetKey || (!targetKey && tab === tabs[0]))
    ? { ...tab, children: [...(tab.children || []), child] }
    : tab)
  localLayout.value = {
    ...localLayout.value,
    items: blocks.value.map(block => block.id === container.id
      ? { ...block, props: { ...(block.props || {}), tabs: nextTabs } }
      : block),
  }
  activeTabKey.value = targetKey || nextTabs[0]?.key || ''
}

function removeTabChild(childId) {
  if (!selectedBlock.value || selectedBlock.value.blockType !== 'tabs')
    return
  const tabs = (selectedBlock.value.props?.tabs || []).map(tab => tab.key === activeTabKey.value
    ? { ...tab, children: (tab.children || []).filter(child => child.id !== childId) }
    : tab)
  patchBlockProps(selectedBlock.value.id, { tabs })
}

function resolveCanvasDropFrame(sourceId = '', frame = {}) {
  if (!doesFrameOverlapBlocks(sourceId, frame))
    return frame
  return {
    ...frame,
    y: nextFreePixelTop(sourceId),
  }
}

function nextFreePixelTop(excludeId = '') {
  return blocks.value.reduce((acc, block) => {
    if (block.id === excludeId)
      return acc
    const rect = resolveBlockFrame(block)
    return Math.max(acc, rect.y + rect.height + gap)
  }, 0)
}

function pixelToCell(clientX, clientY) {
  const rect = canvasRef.value?.getBoundingClientRect()
  if (!rect)
    return { gridX: 0, gridY: 0 }
  const zoom = canvasZoom.value || 1
  const localX = (clientX - rect.left) / zoom
  const localY = (clientY - rect.top) / zoom
  const cellW = colWidth.value + gap
  return {
    gridX: Math.max(0, Math.min(11, Math.floor(localX / cellW))),
    gridY: Math.max(0, Math.floor(localY / (rowHeight + gap))),
  }
}

function pixelToPoint(clientX, clientY) {
  const rect = canvasRef.value?.getBoundingClientRect()
  if (!rect)
    return { x: 0, y: 0 }
  const zoom = canvasZoom.value || 1
  return {
    x: Math.max(0, Math.round((clientX - rect.left) / zoom)),
    y: Math.max(0, Math.round((clientY - rect.top) / zoom)),
  }
}

function findBlockInTree(list = [], id = '') {
  if (!id)
    return null
  for (const block of list || []) {
    if (block?.id === id)
      return block
    const nested = findBlockInTree(resolveNestedBlocks(block), id)
    if (nested)
      return nested
  }
  return null
}

function collectBlocksInTree(block = {}) {
  if (!block?.id)
    return []
  return [block, ...resolveNestedBlocks(block).flatMap(child => collectBlocksInTree(child))]
}

function resolveNestedBlocks(block = {}) {
  const children = Array.isArray(block.children) ? block.children : []
  const tabChildren = (block.props?.tabs || []).flatMap(tab => Array.isArray(tab.children) ? tab.children : [])
  const cellChildren = (block.props?.cells || []).flatMap(cell => Array.isArray(cell.children) ? cell.children : [])
  return [...children, ...tabChildren, ...cellChildren]
}

function mapBlocksInTree(list = [], mapper) {
  return (list || []).map(block => mapBlockInTree(block, mapper))
}

function mapBlockSiblingsInTree(list = [], mapper) {
  const mappedList = mapper(list || [])
  return mappedList.map((block) => {
    let next = block
    if (Array.isArray(next.children) && next.children.length) {
      next = {
        ...next,
        children: mapBlockSiblingsInTree(next.children, mapper),
      }
    }
    if (Array.isArray(next.props?.tabs) && next.props.tabs.length) {
      next = {
        ...next,
        props: {
          ...(next.props || {}),
          tabs: next.props.tabs.map(tab => ({
            ...tab,
            children: mapBlockSiblingsInTree(tab.children || [], mapper),
          })),
        },
      }
    }
    if (Array.isArray(next.props?.cells) && next.props.cells.length) {
      next = {
        ...next,
        props: {
          ...(next.props || {}),
          cells: next.props.cells.map(cell => ({
            ...cell,
            children: mapBlockSiblingsInTree(cell.children || [], mapper),
          })),
        },
      }
    }
    return next
  })
}

function mapBlockInTree(block = {}, mapper) {
  let next = block
  if (Array.isArray(next.children) && next.children.length) {
    next = {
      ...next,
      children: mapBlocksInTree(next.children, mapper),
    }
  }
  if (Array.isArray(next.props?.tabs) && next.props.tabs.length) {
    next = {
      ...next,
      props: {
        ...(next.props || {}),
        tabs: next.props.tabs.map(tab => ({
          ...tab,
          children: mapBlocksInTree(tab.children || [], mapper),
        })),
      },
    }
  }
  if (Array.isArray(next.props?.cells) && next.props.cells.length) {
    next = {
      ...next,
      props: {
        ...(next.props || {}),
        cells: next.props.cells.map(cell => ({
          ...cell,
          children: mapBlocksInTree(cell.children || [], mapper),
        })),
      },
    }
  }
  return mapper(next)
}

function removeBlockFromTree(list = [], id = '') {
  return (list || [])
    .filter(block => block?.id !== id)
    .map((block) => {
      let next = block
      if (Array.isArray(next.children) && next.children.length) {
        next = {
          ...next,
          children: removeBlockFromTree(next.children, id),
        }
      }
      if (Array.isArray(next.props?.tabs) && next.props.tabs.length) {
        next = {
          ...next,
          props: {
            ...(next.props || {}),
            tabs: next.props.tabs.map(tab => ({
              ...tab,
              children: removeBlockFromTree(tab.children || [], id),
            })),
          },
        }
      }
      if (Array.isArray(next.props?.cells) && next.props.cells.length) {
        next = {
          ...next,
          props: {
            ...(next.props || {}),
            cells: next.props.cells.map(cell => ({
              ...cell,
              children: removeBlockFromTree(cell.children || [], id),
            })),
          },
        }
      }
      return next
    })
}

function patchBlock(id, patch) {
  localLayout.value = {
    ...localLayout.value,
    items: normalizeGridItems(mapBlocksInTree(blocks.value, b => b.id === id
      ? {
          ...b,
          ...patch,
          gridX: clamp(patch.gridX ?? b.gridX, 0, LIST_PAGE_GRID_COLS - 1),
          gridW: clamp(patch.gridW ?? b.gridW, 1, LIST_PAGE_GRID_COLS),
        }
      : b)),
  }
}

function updateDesignWidth(value) {
  const nextWidth = clamp(value || LIST_PAGE_DESIGN_WIDTH, 375, 2560)
  localLayout.value = {
    ...localLayout.value,
    designWidth: nextWidth,
  }
}

function applyCanvasPreviewMode(value = 'desktop') {
  const modeMap = {
    desktop: { width: 1366, zoom: 1 },
    narrow: { width: 768, zoom: 0.9 },
    modal: { width: 960, zoom: 0.9 },
    drawer: { width: 720, zoom: 0.9 },
    mobile: { width: 390, zoom: 1 },
  }
  const next = modeMap[value] || modeMap.desktop
  canvasPreviewMode.value = value
  updateDesignWidth(next.width)
  updateCanvasZoom(next.zoom)
}

function updateCanvasZoom(value) {
  canvasZoom.value = clamp(Number(value) || 1, 0.5, 1.25)
}

function handleCanvasWheel(event) {
  if (!event.ctrlKey && !event.metaKey)
    return
  event.preventDefault()
  const delta = event.deltaY > 0 ? -0.08 : 0.08
  updateCanvasZoom(Number((canvasZoom.value + delta).toFixed(2)))
}

function patchBlockProps(id, patch) {
  localLayout.value = {
    ...localLayout.value,
    items: normalizeGridItems(mapBlocksInTree(blocks.value, b => b.id === id
      ? { ...b, props: { ...(b.props || {}), ...patch } }
      : b)),
  }
}

function handleBlockPropsUpdate(payload = {}) {
  if (!payload.blockId || !payload.propsData)
    return
  patchBlockProps(payload.blockId, payload.propsData)
}

function handleCrudPreviewStateChange(payload = {}) {
  if (!payload.blockId || !payload.patch)
    return
  const block = blocks.value.find(item => item.id === payload.blockId)
  const currentProps = block?.props || {}
  const changedPatch = Object.fromEntries(
    Object.entries(payload.patch).filter(([key, value]) => currentProps[key] !== value),
  )
  if (!Object.keys(changedPatch).length)
    return
  patchBlockProps(payload.blockId, changedPatch)
}

function crudPreviewStatusText(status) {
  if (status === 'loading')
    return '请求中'
  if (status === 'success')
    return '预览成功'
  if (status === 'error')
    return '预览失败'
  return '模拟预览'
}

function patchBlockFrame(id, patch) {
  const source = blocks.value.find(block => block.id === id)
  if (!source)
    return false
  const current = resolveBlockFrame(source)
  const widthChanged = Object.prototype.hasOwnProperty.call(patch, 'width')
  const heightChanged = Object.prototype.hasOwnProperty.call(patch, 'height')
  const currentWidthMode = resolveBlockWidthMode(source)
  const currentHeightMode = resolveBlockHeightMode(source)
  const nextWidthMode = widthChanged ? 'fixed' : currentWidthMode
  const nextHeightMode = heightChanged ? 'fixed' : currentHeightMode
  const next = {
    ...current,
    ...patch,
  }
  next.width = Math.max(24, Number(next.width) || current.width)
  next.height = Math.max(24, Number(next.height) || current.height)
  next.x = clamp(Number(next.x) || 0, 0, Math.max(0, canvasGridWidth.value - next.width))
  next.y = Math.max(0, Number(next.y) || 0)
  if (doesFrameOverlapBlocks(id, next, { ignoreTreeMainBlocks: source.blockType === 'tree-panel' }))
    return false
  const gridPatch = frameToGridPatch(next)
  const nextWidthValue = nextWidthMode === 'full' ? '100%' : nextWidthMode === 'auto' ? 'auto' : next.width
  const patchedItems = blocks.value.map(block => block.id === id
    ? {
        ...block,
        ...gridPatch,
        props: {
          ...(block.props || {}),
          style: {
            ...createDefaultBlockStyle(),
            ...(block.props?.style || {}),
            x: next.x,
            y: next.y,
            widthMode: nextWidthMode,
            width: nextWidthValue,
            ...(props.panelOnly && widthChanged ? { pageFlowWidth: `${next.width}px` } : {}),
            heightMode: nextHeightMode,
            height: next.height,
          },
        },
      }
    : block)
  const nextItems = applyTreePanelResponsiveMainFrames(source, next, patchedItems)
  localLayout.value = {
    ...localLayout.value,
    items: normalizeGridItems(nextItems),
  }
  return true
}

function applyTreePanelResponsiveMainFrames(source = {}, nextTreeFrame = {}, items = []) {
  if (props.layoutType !== 'tree-crud' || source.blockType !== 'tree-panel')
    return items
  const mainX = clamp(
    Math.round((Number(nextTreeFrame.x) || 0) + (Number(nextTreeFrame.width) || 0) + gap),
    0,
    Math.max(0, canvasGridWidth.value - 24),
  )
  const mainWidth = Math.max(24, canvasGridWidth.value - mainX)
  return items.map((block) => {
    if (!isTreeCrudMainBlock(block) || resolveBlockWidthMode(block) !== 'full')
      return block
    const rect = resolveBlockFrame(block)
    if (!isVerticalFrameOverlap(rect, nextTreeFrame))
      return block
    const nextFrame = {
      ...rect,
      x: mainX,
      width: mainWidth,
    }
    const gridPatch = frameToGridPatch(nextFrame)
    return {
      ...block,
      ...gridPatch,
      props: {
        ...(block.props || {}),
        style: {
          ...createDefaultBlockStyle(),
          ...(block.props?.style || {}),
          x: nextFrame.x,
          y: nextFrame.y,
          widthMode: 'full',
          width: '100%',
          height: nextFrame.height,
        },
      },
    }
  })
}

function isTreeCrudMainBlock(block = {}) {
  return props.layoutType === 'tree-crud'
    && ['AiCrudPage', 'AiTable', 'data-table', 'search-form', 'toolbar'].includes(block.blockType)
}

function doesFrameOverlapBlocks(sourceId, frame, options = {}) {
  const source = blocks.value.find(block => block.id === sourceId)
  return blocks.value.some((block) => {
    if (block.id === sourceId)
      return false
    if (options.ignoreTreeMainBlocks && source?.blockType === 'tree-panel' && isTreeCrudMainBlock(block) && resolveBlockWidthMode(block) === 'full')
      return false
    return framesOverlap(frame, resolveBlockFrame(block))
  })
}

function framesOverlap(a = {}, b = {}) {
  const aLeft = Number(a.x) || 0
  const aTop = Number(a.y) || 0
  const aRight = aLeft + (Number(a.width) || 0)
  const aBottom = aTop + (Number(a.height) || 0)
  const bLeft = Number(b.x) || 0
  const bTop = Number(b.y) || 0
  const bRight = bLeft + (Number(b.width) || 0)
  const bBottom = bTop + (Number(b.height) || 0)
  return aLeft < bRight && aRight > bLeft && aTop < bBottom && aBottom > bTop
}

function setBlockWidthMode(id, mode = 'full') {
  const source = blocks.value.find(block => block.id === id)
  if (!source)
    return
  const widthMode = ['full', 'auto', 'fixed'].includes(mode) ? mode : 'full'
  const current = resolveBlockFrame(source)
  const fixedRuntimeWidth = props.panelOnly && resolveBlockWidthMode(source) !== 'fixed'
    ? Math.max(280, Math.min(640, Math.round(canvasGridWidth.value * 0.52)))
    : current.width
  const nextFrame = {
    ...current,
    width: widthMode === 'full'
      ? Math.max(24, canvasGridWidth.value - current.x)
      : widthMode === 'auto'
        ? Math.max(240, Math.min(520, current.width))
        : fixedRuntimeWidth,
  }
  const gridPatch = frameToGridPatch(nextFrame)
  localLayout.value = {
    ...localLayout.value,
    items: normalizeGridItems(blocks.value.map(block => block.id === id
      ? {
          ...block,
          ...gridPatch,
          props: {
            ...(block.props || {}),
            style: {
              ...createDefaultBlockStyle(),
              ...(block.props?.style || {}),
              x: nextFrame.x,
              y: nextFrame.y,
              widthMode,
              width: widthMode === 'full' ? '100%' : widthMode === 'auto' ? 'auto' : nextFrame.width,
              ...(props.panelOnly
                ? { pageFlowWidth: widthMode === 'full' ? 'calc(100% - 48px)' : `${nextFrame.width}px` }
                : {}),
              height: nextFrame.height,
            },
          },
        }
      : block)),
  }
}

function setBlockHeightMode(id, mode = 'fixed') {
  const source = blocks.value.find(block => block.id === id)
  if (!source)
    return
  const heightMode = ['fixed', 'auto', 'full'].includes(mode) ? mode : 'fixed'
  const current = resolveBlockFrame(source)
  const nextFrame = {
    ...current,
    height: heightMode === 'auto' ? Math.max(48, Math.min(240, current.height)) : current.height,
  }
  const gridPatch = frameToGridPatch(nextFrame)
  localLayout.value = {
    ...localLayout.value,
    items: normalizeGridItems(blocks.value.map(block => block.id === id
      ? {
          ...block,
          ...gridPatch,
          props: {
            ...(block.props || {}),
            style: {
              ...createDefaultBlockStyle(),
              ...(block.props?.style || {}),
              x: nextFrame.x,
              y: nextFrame.y,
              width: nextFrame.width,
              heightMode,
              height: nextFrame.height,
            },
          },
        }
      : block)),
  }
}

function setBlockContentAlign(id, align = 'left') {
  const source = findBlockInTree(blocks.value, id)
  if (!source)
    return
  const textAlign = align === 'center' ? 'center' : 'left'
  patchBlockProps(id, {
    align: textAlign,
    textAlign,
    style: {
      ...createDefaultBlockStyle(),
      ...(source.props?.style || {}),
      textAlign,
    },
  })
}

function addBlockEvent() {
  if (!selectedBlock.value)
    return
  const list = [...(selectedBlock.value.props?.events || [])]
  list.push({
    id: `evt_${Date.now()}`,
    trigger: selectedBlock.value.blockType === 'tree-panel' ? 'nodeSelect' : 'click',
    action: 'none',
    targetBlockId: '',
    targetPageKey: '',
    targetFormKey: '',
    description: '',
    params: [],
  })
  patchBlockProps(selectedBlock.value.id, { events: list })
}

function resolvePrimaryClickEvent(block = selectedBlock.value) {
  const eventItem = (block?.props?.events || []).find(item => (item.trigger || 'click') === 'click')
  return {
    trigger: 'click',
    action: 'none',
    targetBlockId: '',
    targetPageKey: '',
    targetFormKey: '',
    requestUrl: '',
    description: '',
    params: [],
    permissionCode: '',
    confirmText: '',
    displayCondition: '',
    successBehavior: 'none',
    ...(eventItem || {}),
  }
}

function updatePrimaryClickEvent(patch = {}) {
  if (!selectedBlock.value)
    return
  const list = [...(selectedBlock.value.props?.events || [])]
  const index = list.findIndex(item => (item.trigger || 'click') === 'click')
  const nextEvent = {
    id: index >= 0 ? list[index].id : `evt_${Date.now()}`,
    ...resolvePrimaryClickEvent(selectedBlock.value),
    ...patch,
    trigger: 'click',
  }
  if (index >= 0)
    list[index] = nextEvent
  else
    list.unshift(nextEvent)
  patchBlockProps(selectedBlock.value.id, { events: list })
}

function updateBlockEvent(eventIdx, patch) {
  if (!selectedBlock.value)
    return
  const list = [...(selectedBlock.value.props?.events || [])]
  list[eventIdx] = { ...(list[eventIdx] || {}), ...patch }
  patchBlockProps(selectedBlock.value.id, { events: list })
}

function removeBlockEvent(eventIdx) {
  if (!selectedBlock.value)
    return
  const list = [...(selectedBlock.value.props?.events || [])]
  list.splice(eventIdx, 1)
  patchBlockProps(selectedBlock.value.id, { events: list })
}

function addBlockEventParam(eventIdx) {
  const eventItem = selectedBlock.value?.props?.events?.[eventIdx]
  if (!eventItem)
    return
  updateBlockEvent(eventIdx, {
    params: [...(eventItem.params || []), createActionParam()],
  })
}

function updateBlockEventParam(eventIdx, paramIdx, patch) {
  const eventItem = selectedBlock.value?.props?.events?.[eventIdx]
  if (!eventItem)
    return
  const params = [...(eventItem.params || [])]
  params[paramIdx] = { ...(params[paramIdx] || {}), ...patch }
  updateBlockEvent(eventIdx, { params })
}

function removeBlockEventParam(eventIdx, paramIdx) {
  const eventItem = selectedBlock.value?.props?.events?.[eventIdx]
  if (!eventItem)
    return
  const params = [...(eventItem.params || [])]
  params.splice(paramIdx, 1)
  updateBlockEvent(eventIdx, { params })
}

function eventTriggerText(trigger) {
  return eventTriggerOptions.find(item => item.value === trigger)?.label || trigger || '触发'
}

function eventActionText(action) {
  return blockEventActionOptions.find(item => item.value === action)?.label || action || '动作'
}

function patchBlockStyle(id, patch) {
  const source = findBlockInTree(blocks.value, id)
  if (!source)
    return
  patchBlockProps(id, {
    style: {
      ...createDefaultBlockStyle(),
      ...(source.props?.style || {}),
      ...patch,
    },
  })
}

function removeBlock(id) {
  if (!id)
    return
  localLayout.value = {
    ...localLayout.value,
    items: normalizeGridItems(removeBlockFromTree(blocks.value, id)),
  }
  if (selectedBlockId.value === id) {
    selectedBlockId.value = null
  }
}

function duplicateBlock(id) {
  const source = blocks.value.find(block => block.id === id)
  if (!source) {
    duplicateNestedBlock(id)
    return
  }
  const meta = resolveListPageBlockMeta(source.blockType)
  if (meta?.unique)
    return
  const copy = cloneBlockWithFreshIds(source, `${source.id}_copy_${Date.now()}`)
  const sourceFrame = resolveBlockFrame(source)
  const sourceWidthMode = resolveBlockWidthMode(source)
  const nextFrame = {
    x: clamp(
      sourceFrame.x + 32,
      0,
      sourceWidthMode === 'full'
        ? Math.max(0, canvasGridWidth.value - 24)
        : Math.max(0, canvasGridWidth.value - sourceFrame.width),
    ),
    y: sourceFrame.y + 32,
    width: sourceFrame.width,
    height: sourceFrame.height,
  }
  const gridPatch = frameToGridPatch(nextFrame)
  copy.label = `${source.label || resolveListPageBlockMeta(source.blockType)?.title || '区块'} 副本`
  copy.gridX = gridPatch.gridX
  copy.gridY = gridPatch.gridY
  copy.gridW = gridPatch.gridW
  copy.gridH = gridPatch.gridH
  copy.props = {
    ...(copy.props || {}),
    style: {
      ...createDefaultBlockStyle(),
      ...(copy.props?.style || {}),
      ...nextFrame,
    },
  }
  localLayout.value = {
    ...localLayout.value,
    items: normalizeGridItems([...blocks.value, copy]),
  }
  selectBlock(copy.id)
}

function duplicateNestedBlock(id) {
  const source = findBlockInTree(blocks.value, id)
  if (!source)
    return
  const meta = resolveListPageBlockMeta(source.blockType)
  if (meta?.unique)
    return
  const copy = cloneBlockWithFreshIds(source, `${source.id}_copy_${Date.now()}`)
  copy.label = `${source.label || meta?.title || '组件'} 副本`
  let inserted = false
  localLayout.value = {
    ...localLayout.value,
    items: normalizeGridItems(mapBlockSiblingsInTree(blocks.value, (siblings) => {
      const sourceIndex = siblings.findIndex(item => item.id === id)
      if (sourceIndex < 0 || inserted)
        return siblings
      const next = [...siblings]
      next.splice(sourceIndex + 1, 0, copy)
      inserted = true
      return next
    })),
  }
  if (inserted)
    selectBlock(copy.id)
}

function cloneBlockWithFreshIds(block = {}, rootId = '') {
  const copy = JSON.parse(JSON.stringify(block))
  const stamp = Date.now()
  const applyIds = (node, suffix = 'root') => {
    const next = {
      ...node,
      id: suffix === 'root' ? (rootId || `${node.id}_copy_${stamp}`) : `${node.id}_copy_${stamp}_${suffix}`,
    }
    if (Array.isArray(next.children)) {
      next.children = next.children.map((child, index) => applyIds(child, `${suffix}_child_${index}`))
    }
    if (Array.isArray(next.props?.tabs)) {
      next.props = {
        ...(next.props || {}),
        tabs: next.props.tabs.map((tab, tabIndex) => ({
          ...tab,
          children: (tab.children || []).map((child, childIndex) => applyIds(child, `${suffix}_tab_${tabIndex}_${childIndex}`)),
        })),
      }
    }
    if (Array.isArray(next.props?.cells)) {
      next.props = {
        ...(next.props || {}),
        cells: next.props.cells.map((cell, cellIndex) => ({
          ...cell,
          children: (cell.children || []).map((child, childIndex) => applyIds(child, `${suffix}_cell_${cellIndex}_${childIndex}`)),
        })),
      }
    }
    return next
  }
  return applyIds(copy)
}

function reorderBlockLayer(id, offset) {
  const index = blocks.value.findIndex(block => block.id === id)
  if (index < 0)
    return
  const nextIndex = clamp(index + offset, 0, blocks.value.length - 1)
  if (nextIndex === index)
    return
  const items = [...blocks.value]
  const [target] = items.splice(index, 1)
  items.splice(nextIndex, 0, target)
  localLayout.value = {
    ...localLayout.value,
    items,
  }
}

function resetLayout() {
  localLayout.value = normalizeDesignerLayout(createDefaultListGridLayout(props.modelSchema, { layoutType: props.layoutType }))
  clearSelection()
}

function clearCanvas() {
  localLayout.value = normalizeDesignerLayout({
    ...(localLayout.value || {}),
    cols: LIST_PAGE_GRID_COLS,
    rowHeight,
    gap,
    designWidth: designCanvasWidth.value,
    layoutType: props.layoutType,
    items: [],
  })
  fieldDrawerOpen.value = false
  clearSelection()
}

function clamp(value, min, max) {
  const n = Number(value)
  if (!Number.isFinite(n))
    return min
  return Math.min(Math.max(n, min), max)
}

function normalizeDesignerLayout(layout = {}) {
  return {
    ...layout,
    designWidth: clamp(layout.designWidth || LIST_PAGE_DESIGN_WIDTH, 960, 2560),
    items: normalizeGridItems(layout.items || []),
  }
}

function normalizeGridItems(items = []) {
  return items.map((item) => {
    const gridW = clamp(item.gridW, 1, LIST_PAGE_GRID_COLS)
    const gridX = clamp(item.gridX, 0, LIST_PAGE_GRID_COLS - gridW)
    const autoGridH = resolveAutoGridH(item, gridW)
    const gridH = Math.max(autoGridH, Number(item.gridH) || 1)
    return normalizeGridItemFrame({
      ...item,
      gridX,
      gridW,
      gridY: Math.max(0, Number(item.gridY) || 0),
      gridH,
    }, gridH)
  })
}

function normalizeGridItemFrame(item = {}, gridH = item.gridH) {
  const autoHeight = gridHeightToPixels(gridH || item.gridH || 1)
  const style = item.props?.style || {}
  const currentHeight = resolveCssNumber(style.height, 0)
  if (currentHeight >= autoHeight)
    return item
  return {
    ...item,
    props: {
      ...(item.props || {}),
      style: {
        ...createDefaultBlockStyle(),
        ...style,
        height: autoHeight,
      },
    },
  }
}

function resolveAutoGridH(block = {}, normalizedGridW = block.gridW) {
  const gridW = Number(normalizedGridW) || Number(block.gridW) || LIST_PAGE_GRID_COLS
  if (block.blockType === 'search-form') {
    const fieldRows = Math.max(1, Math.ceil((block.fieldRefs?.length || 0) / resolveFieldColumnCount(gridW)))
    return Math.max(4, gridRowsForPixels(76 + fieldRows * 54))
  }
  if (block.blockType === 'toolbar') {
    const actionRows = Math.max(1, Math.ceil(resolveToolbarActionCount(block) / resolveToolbarColumnCount(gridW)))
    return Math.max(2, gridRowsForPixels(20 + actionRows * 34))
  }
  if (block.blockType === 'data-table') {
    return Math.max(8, gridRowsForPixels(238))
  }
  if (block.blockType === 'AiCrudPage') {
    return Math.max(5, gridRowsForPixels(160))
  }
  if (block.blockType === 'AiTable') {
    return Math.max(8, gridRowsForPixels(250))
  }
  if (block.blockType === 'AiForm') {
    return Math.max(5, gridRowsForPixels(164))
  }
  if (block.blockType === 'detail-info') {
    const fieldRows = Math.max(1, Math.ceil((block.fieldRefs?.length || 0) / Math.max(1, Number(block.props?.columnCount || 2))))
    return Math.max(5, gridRowsForPixels(58 + fieldRows * 54))
  }
  if (block.blockType === 'tree-panel') {
    return Math.max(12, gridRowsForPixels(360))
  }
  if (block.blockType === 'grid-layout') {
    const cells = normalizeGridLayoutCells(block)
    const maxCellHeight = cells.reduce((max, cell) => Math.max(max, Number(cell.minHeight || 0)), 0)
    return Math.max(4, gridRowsForPixels(maxCellHeight + 16))
  }
  if (block.blockType === 'box-layout') {
    const childrenHeight = (block.children || []).reduce((sum, child) => sum + resolveNestedChildHeight(child), 0)
    const gapValue = Math.max(0, Number(block.props?.gap ?? 12))
    return Math.max(4, gridRowsForPixels(childrenHeight + Math.max(0, (block.children || []).length - 1) * gapValue + 24))
  }
  if (['rich-text', 'transfer', 'step-form', 'video-player', 'iframe', 'markdown', 'descriptions', 'signature-pad'].includes(block.blockType))
    return Math.max(4, gridRowsForPixels(resolveCssNumber(block.props?.style?.height, 180)))
  return 1
}

function resolveFieldColumnCount(gridW) {
  if (gridW >= 10)
    return 4
  if (gridW >= 7)
    return 3
  if (gridW >= 4)
    return 2
  return 1
}

function resolveToolbarColumnCount(gridW) {
  if (gridW >= 10)
    return 6
  if (gridW >= 7)
    return 4
  if (gridW >= 4)
    return 3
  return 2
}

function resolveToolbarActionCount(block = {}) {
  const baseActions = Array.isArray(block.props?.actions) ? block.props.actions.length : 0
  const customActions = (block.props?.customActions || []).filter(action => (action.position || 'toolbar') === 'toolbar').length
  return Math.max(1, baseActions + customActions)
}

function gridRowsForPixels(height) {
  return Math.max(1, Math.ceil((Number(height) + gap) / (rowHeight + gap)))
}

// 拖动移动
let moveCtx = null
let nestedMoveCtx = null
function startMove(block, event) {
  if (props.readonly)
    return
  if (event.button !== 0)
    return
  event.preventDefault()
  selectBlock(block.id)
  const rect = resolveBlockFrame(block)
  movingBlockId.value = block.id
  movingPreviewBlock.value = { ...block }
  movingPixelOffset.value = { x: 0, y: 0 }
  deferLayoutEmit.value = true
  hasDeferredLayoutEmit = false
  moveCtx = {
    blockId: block.id,
    startX: event.clientX,
    startY: event.clientY,
    startScrollLeft: canvasScrollRef.value?.scrollLeft || 0,
    startScrollTop: canvasScrollRef.value?.scrollTop || 0,
    originX: rect.x,
    originY: rect.y,
    originW: rect.width,
    originH: rect.height,
  }
  window.addEventListener('pointermove', onMove)
  window.addEventListener('pointerup', endMove)
}

function startNestedMove(block, event) {
  if (props.readonly || !block?.id)
    return
  if (event.button !== 0)
    return
  event.preventDefault()
  selectBlock(block.id)
  draggedExistingBlockId.value = block.id
  draggedBlockType.value = block.blockType || ''
  canvasDragActive.value = true
  nestedMovingBlockId.value = block.id
  const node = event.target?.closest?.('.layout-grid-cell-child')
  const rect = node?.getBoundingClientRect?.()
  const zoom = canvasZoom.value || 1
  nestedMoveCtx = {
    blockId: block.id,
    startX: event.clientX,
    startY: event.clientY,
    pointerOffsetX: rect ? (event.clientX - rect.left) / zoom : 0,
    pointerOffsetY: rect ? (event.clientY - rect.top) / zoom : 0,
    originW: rect ? rect.width / zoom : 120,
    originH: rect ? rect.height / zoom : 48,
    moved: false,
  }
  document.body.classList.add('list-grid-nested-moving')
  updateNestedMovePreview(event)
  window.addEventListener('pointermove', onNestedMove, { passive: false })
  window.addEventListener('pointerup', endNestedMove)
  window.addEventListener('pointercancel', cancelNestedMove)
}

function onNestedMove(event) {
  if (!nestedMoveCtx)
    return
  event.preventDefault()
  const dx = event.clientX - nestedMoveCtx.startX
  const dy = event.clientY - nestedMoveCtx.startY
  if (Math.abs(dx) > 2 || Math.abs(dy) > 2) {
    nestedMoveCtx.moved = true
    suppressNextBlockClick = true
  }
  updateNestedMovePreview(event)
}

function updateNestedMovePreview(event) {
  if (!nestedMoveCtx)
    return
  autoScrollCanvasOnPointer(event)
  canvasDragActive.value = true
  draggedExistingBlockId.value = nestedMoveCtx.blockId
  const block = findBlockInTree(blocks.value, nestedMoveCtx.blockId)
  draggedBlockType.value = block?.blockType || draggedBlockType.value
  const container = resolveDropContainerFromPoint(event.clientX, event.clientY)
  dragBlockedBlockId.value = !container ? resolveNonContainerDropBlock(event, nestedMoveCtx.blockId)?.id || '' : ''
  activeDropCell.value = container?.block?.blockType === 'grid-layout' && container.cellKey
    ? { containerId: container.id, cellKey: container.cellKey }
    : null
  dragOverCell.value = activeDropCell.value || dragBlockedBlockId.value ? null : pixelToCell(event.clientX, event.clientY)
  dragOverPoint.value = pixelToPoint(event.clientX, event.clientY)
}

function endNestedMove(event) {
  if (!nestedMoveCtx)
    return
  const ctx = nestedMoveCtx
  const blockId = ctx.blockId
  const container = resolveDropContainerFromPoint(event.clientX, event.clientY)
  const blockedBlock = !container ? resolveNonContainerDropBlock(event, blockId) : null
  const cellStylePatch = container?.cellRect ? resolveNestedCellDropStyle(event, container, ctx) : {}
  const point = pixelToPoint(event.clientX, event.clientY)
  cleanupNestedMove()
  if (!ctx.moved)
    return
  if (blockedBlock)
    return
  if (container?.block?.blockType === 'grid-layout' && container.cellKey) {
    moveExistingBlockToGridCell(blockId, container.id, container.cellKey, cellStylePatch)
    return
  }
  if (container?.block && container.block.blockType !== 'grid-layout' && container.block.id !== blockId) {
    moveExistingBlockToContainer(blockId, container.id)
    return
  }
  moveExistingBlockToCanvas(blockId, point)
}

function cancelNestedMove() {
  cleanupNestedMove()
}

function cleanupNestedMove() {
  nestedMoveCtx = null
  nestedMovingBlockId.value = ''
  document.body.classList.remove('list-grid-nested-moving')
  resetCanvasDragState()
  window.removeEventListener('pointermove', onNestedMove)
  window.removeEventListener('pointerup', endNestedMove)
  window.removeEventListener('pointercancel', cancelNestedMove)
}

function resolveNestedCellDropStyle(event, container = {}, ctx = {}) {
  if (!container.cellRect)
    return {}
  const zoom = canvasZoom.value || 1
  const cellWidth = Math.max(24, container.cellRect.width / zoom - 16)
  const cellHeight = Math.max(24, container.cellRect.height / zoom - 16)
  const nextX = clamp(
    (event.clientX - container.cellRect.left) / zoom - (ctx.pointerOffsetX || 0) - 8,
    0,
    Math.max(0, cellWidth - Math.min(ctx.originW || 24, cellWidth)),
  )
  const nextY = clamp(
    (event.clientY - container.cellRect.top) / zoom - (ctx.pointerOffsetY || 0) - 8,
    0,
    Math.max(0, cellHeight - Math.min(ctx.originH || 24, cellHeight)),
  )
  return {
    x: Math.round(nextX),
    y: Math.round(nextY),
  }
}
function onMove(event) {
  if (!moveCtx)
    return
  const block = findBlockInTree(blocks.value, moveCtx.blockId)
  if (!block)
    return
  autoScrollCanvasOnPointer(event)
  const zoom = canvasZoom.value || 1
  const scrollEl = canvasScrollRef.value
  const scrollDx = (scrollEl?.scrollLeft || 0) - (moveCtx.startScrollLeft || 0)
  const scrollDy = (scrollEl?.scrollTop || 0) - (moveCtx.startScrollTop || 0)
  const rawDx = (event.clientX - moveCtx.startX + scrollDx) / zoom
  const rawDy = (event.clientY - moveCtx.startY + scrollDy) / zoom
  const minDx = -moveCtx.originX
  const widthMode = resolveBlockWidthMode(block)
  const maxDx = widthMode === 'full'
    ? Math.max(0, canvasGridWidth.value - 24 - moveCtx.originX)
    : Math.max(0, canvasGridWidth.value - moveCtx.originW - moveCtx.originX)
  const minDy = -moveCtx.originY
  const offsetX = clamp(rawDx, minDx, maxDx)
  const offsetY = Math.max(rawDy, minDy)
  movingPixelOffset.value = { x: offsetX, y: offsetY }
  canvasDragActive.value = true
  draggedExistingBlockId.value = moveCtx.blockId
  draggedBlockType.value = block.blockType || draggedBlockType.value
  const container = resolveDropContainerFromPoint(event.clientX, event.clientY)
  dragBlockedBlockId.value = !container ? resolveNonContainerDropBlock(event, moveCtx.blockId)?.id || '' : ''
  activeDropCell.value = container?.block?.blockType === 'grid-layout' && container.cellKey
    ? { containerId: container.id, cellKey: container.cellKey }
    : null
  const nextFrame = {
    x: Math.round(moveCtx.originX + offsetX),
    y: Math.round(moveCtx.originY + offsetY),
    width: moveCtx.originW,
    height: moveCtx.originH,
  }
  const gridPatch = frameToGridPatch(nextFrame)
  movingPreviewBlock.value = {
    ...block,
    ...gridPatch,
    props: {
      ...(block.props || {}),
      style: {
        ...createDefaultBlockStyle(),
        ...(block.props?.style || {}),
        ...nextFrame,
      },
    },
  }
  if (Math.abs(rawDx) > 2 || Math.abs(rawDy) > 2)
    suppressNextBlockClick = true
}
function endMove() {
  const preview = movingPreviewBlock.value
  if (moveCtx && preview) {
    const block = findBlockInTree(blocks.value, moveCtx.blockId)
    if (block && !dragBlockedBlockId.value) {
      const rect = resolveBlockFrame(preview)
      patchBlockFrame(block.id, { x: rect.x, y: rect.y, width: rect.width, height: rect.height })
    }
  }
  moveCtx = null
  movingBlockId.value = ''
  movingPreviewBlock.value = null
  movingPixelOffset.value = { x: 0, y: 0 }
  resetCanvasDragState()
  flushDeferredLayoutEmit()
  window.removeEventListener('pointermove', onMove)
  window.removeEventListener('pointerup', endMove)
}

// Resize
let resizeCtx = null
let nestedResizeCtx = null
function startResize(block, event, anchor = 'bottom-right') {
  if (props.readonly)
    return
  if (event.button !== 0)
    return
  event.preventDefault()
  const rect = resolveBlockFrame(block)
  deferLayoutEmit.value = true
  hasDeferredLayoutEmit = false
  resizeCtx = {
    blockId: block.id,
    anchor,
    startX: event.clientX,
    startY: event.clientY,
    startScrollLeft: canvasScrollRef.value?.scrollLeft || 0,
    startScrollTop: canvasScrollRef.value?.scrollTop || 0,
    originX: rect.x,
    originY: rect.y,
    originW: rect.width,
    originH: rect.height,
  }
  selectBlock(block.id)
  window.addEventListener('pointermove', onResize)
  window.addEventListener('pointerup', endResize)
}
function onResize(event) {
  if (!resizeCtx)
    return
  autoScrollCanvasOnPointer(event)
  const zoom = canvasZoom.value || 1
  const scrollEl = canvasScrollRef.value
  const scrollDx = (scrollEl?.scrollLeft || 0) - (resizeCtx.startScrollLeft || 0)
  const scrollDy = (scrollEl?.scrollTop || 0) - (resizeCtx.startScrollTop || 0)
  const dw = Math.round((event.clientX - resizeCtx.startX + scrollDx) / zoom)
  const dh = Math.round((event.clientY - resizeCtx.startY + scrollDy) / zoom)
  const block = findBlockInTree(blocks.value, resizeCtx.blockId)
  if (!block)
    return
  const anchor = resizeCtx.anchor || 'bottom-right'
  const minW = 24
  const minH = 24
  const originRight = resizeCtx.originX + resizeCtx.originW
  const originBottom = resizeCtx.originY + resizeCtx.originH
  let nextX = resizeCtx.originX
  let nextY = resizeCtx.originY
  let nextW = resizeCtx.originW
  let nextH = resizeCtx.originH

  if (anchor.includes('right')) {
    nextW = clamp(resizeCtx.originW + dw, minW, Math.max(minW, canvasGridWidth.value - resizeCtx.originX))
  }
  if (anchor.includes('left')) {
    nextX = clamp(resizeCtx.originX + dw, 0, originRight - minW)
    nextW = originRight - nextX
  }
  if (anchor.includes('bottom')) {
    nextH = Math.max(minH, resizeCtx.originH + dh)
  }
  if (anchor.includes('top')) {
    nextY = Math.max(0, Math.min(resizeCtx.originY + dh, originBottom - minH))
    nextH = originBottom - nextY
  }

  const current = resolveBlockFrame(block)
  if (current.x !== nextX || current.y !== nextY || current.width !== nextW || current.height !== nextH)
    patchBlockFrame(block.id, { x: nextX, y: nextY, width: nextW, height: nextH })
}
function endResize() {
  resizeCtx = null
  flushDeferredLayoutEmit()
  window.removeEventListener('pointermove', onResize)
  window.removeEventListener('pointerup', endResize)
}

function startNestedResize(block, event, anchor = 'bottom-right') {
  if (props.readonly || !block?.id)
    return
  if (event.button !== 0)
    return
  event.preventDefault()
  selectBlock(block.id)
  const node = event.target?.closest?.('.layout-grid-cell-child')
  const rect = node?.getBoundingClientRect?.()
  const currentStyle = block.props?.style || {}
  nestedResizeCtx = {
    blockId: block.id,
    anchor,
    startX: event.clientX,
    startY: event.clientY,
    originW: rect?.width || resolveCssNumber(currentStyle.width, 240),
    originH: rect?.height || resolveCssNumber(currentStyle.height, 96),
  }
  window.addEventListener('pointermove', onNestedResize)
  window.addEventListener('pointerup', endNestedResize)
}

function onNestedResize(event) {
  if (!nestedResizeCtx)
    return
  const zoom = canvasZoom.value || 1
  const dx = Math.round((event.clientX - nestedResizeCtx.startX) / zoom)
  const dy = Math.round((event.clientY - nestedResizeCtx.startY) / zoom)
  const anchor = nestedResizeCtx.anchor || 'bottom-right'
  const patch = {}
  if (anchor.includes('right') || anchor.includes('left')) {
    const widthDelta = anchor.includes('left') ? -dx : dx
    patch.widthMode = 'fixed'
    patch.width = Math.max(80, nestedResizeCtx.originW + widthDelta)
  }
  if (anchor.includes('bottom') || anchor.includes('top')) {
    const heightDelta = anchor.includes('top') ? -dy : dy
    patch.height = Math.max(40, nestedResizeCtx.originH + heightDelta)
  }
  if (Object.keys(patch).length)
    patchBlockStyle(nestedResizeCtx.blockId, patch)
}

function endNestedResize() {
  nestedResizeCtx = null
  window.removeEventListener('pointermove', onNestedResize)
  window.removeEventListener('pointerup', endNestedResize)
}

function flushDeferredLayoutEmit() {
  if (!deferLayoutEmit.value)
    return
  deferLayoutEmit.value = false
  if (hasDeferredLayoutEmit) {
    hasDeferredLayoutEmit = false
    emitLayoutChange()
  }
}

function updateCanvasViewportWidth() {
  canvasViewportWidth.value = canvasScrollRef.value?.clientWidth || 0
}

onMounted(() => {
  nextTick(() => {
    updateCanvasViewportWidth()
    if (typeof ResizeObserver !== 'undefined' && canvasScrollRef.value) {
      canvasResizeObserver = new ResizeObserver(updateCanvasViewportWidth)
      canvasResizeObserver.observe(canvasScrollRef.value)
      return
    }
    window.addEventListener('resize', updateCanvasViewportWidth)
  })
})

onBeforeUnmount(() => {
  endMove()
  cancelNestedMove()
  endResize()
  endNestedResize()
  canvasResizeObserver?.disconnect?.()
  canvasResizeObserver = null
  window.removeEventListener('resize', updateCanvasViewportWidth)
})

// Field config drawer
function openFieldDrawer(mode = 'table') {
  fieldDrawerMode.value = mode === 'search' ? 'search' : 'table'
  activeDrawerFieldName.value = resolveSelectedFieldRefs(selectedBlock.value, mode === 'search' ? 'search' : 'table')?.[0] || ''
  fieldAdvancedOpen.value = false
  fieldDrawerOpen.value = true
}

function openInlineFieldDrawer(fieldName = '', mode = 'table') {
  fieldDrawerMode.value = mode === 'search' ? 'search' : 'table'
  activeDrawerFieldName.value = fieldName || resolveSelectedFieldRefs(selectedBlock.value, fieldDrawerMode.value)?.[0] || ''
  fieldAdvancedOpen.value = false
  fieldDrawerOpen.value = true
}

function selectDrawerField(fieldName = '') {
  activeDrawerFieldName.value = fieldName
  fieldAdvancedOpen.value = false
}

function resolveSelectedFieldRefs(block = selectedBlock.value, zoneKey = selectedBlockZoneKey.value) {
  if (!block)
    return []
  if (zoneKey === 'search' && block.blockType === 'AiCrudPage') {
    const refs = Array.isArray(block.props?.searchFieldRefs)
      ? block.props.searchFieldRefs
      : block.fieldRefs || []
    const fieldSet = new Set(props.fields.filter(field => isPageFieldVisible(field, 'search')).map(field => field.field))
    return refs.filter(ref => fieldSet.has(ref))
  }
  return Array.isArray(block.fieldRefs) ? block.fieldRefs : []
}

function resolveBlockFieldCount(block = selectedBlock.value, zoneKey = 'table') {
  if (block?.blockType === 'AiCrudPage' && zoneKey === 'table') {
    const refs = new Set(resolveSelectedFieldRefs(block, 'table'))
    return props.fields.filter(field => refs.has(field.field) && isCrudTableFieldVisible(field.field, block)).length
  }
  return resolveSelectedFieldRefs(block, zoneKey).length
}

function toggleField(fieldName, add) {
  if (!selectedBlock.value)
    return
  const current = selectedFieldRefs.value || []
  const next = add
    ? [...current, fieldName]
    : current.filter(f => f !== fieldName)
  updateSelectedFieldRefs(Array.from(new Set(next)))
}

function handleSelectedReorder(rows) {
  if (!selectedBlock.value)
    return
  updateSelectedFieldRefs(rows.map(r => r.field))
}

function handleCrudTableFieldReorder(rows = []) {
  if (!selectedBlock.value?.id)
    return
  const nextRefs = resolveCrudTablePanelFieldRefs(rows)
  patchBlock(selectedBlock.value.id, {
    fieldRefs: nextRefs,
    props: {
      ...(selectedBlock.value.props || {}),
      fieldSettings: buildCrudTableVisibilitySettings(nextRefs),
    },
  })
}

function toggleCrudTableField(fieldName = '', visible = true) {
  if (!selectedBlock.value?.id || !fieldName)
    return
  const nextRefs = resolveCrudTablePanelFieldRefs()
  patchBlock(selectedBlock.value.id, {
    fieldRefs: nextRefs,
    props: {
      ...(selectedBlock.value.props || {}),
      fieldSettings: buildCrudTableVisibilitySettings(nextRefs, { [fieldName]: visible === true }),
    },
  })
}

function resolveCrudFieldInlineTitle(field = {}) {
  const setting = selectedBlock.value?.props?.fieldSettings?.[field.field] || {}
  return setting.title || field.label || field.field
}

function resolveCrudTablePanelFieldRefs(rows = crudTablePanelFields.value) {
  return Array.from(new Set((rows || [])
    .map(row => row?.field)
    .filter(Boolean)))
}

function isCrudTableFieldVisible(fieldName = '', block = selectedBlock.value) {
  if (!fieldName || !block)
    return false
  const refs = resolveSelectedFieldRefs(block, 'table')
  const setting = block.props?.fieldSettings?.[fieldName] || {}
  return refs.includes(fieldName) && setting.visible !== false
}

function buildCrudTableVisibilitySettings(nextRefs = [], overrides = {}) {
  const block = selectedBlock.value || {}
  const previousRefs = new Set(resolveSelectedFieldRefs(block, 'table'))
  const currentSettings = block.props?.fieldSettings || {}
  const nextSettings = { ...currentSettings }
  nextRefs.forEach((fieldName) => {
    const previous = currentSettings[fieldName] || {}
    const nextVisible = Object.prototype.hasOwnProperty.call(overrides, fieldName)
      ? overrides[fieldName] === true
      : previous.visible === false
        ? false
        : previousRefs.has(fieldName)
    nextSettings[fieldName] = {
      ...previous,
      visible: nextVisible,
    }
  })
  return nextSettings
}

function resolveCrudFieldIconComponent(field = {}) {
  const componentType = String(field.componentType || field.type || '').toLowerCase()
  const dataType = String(field.dataType || '').toLowerCase()
  const fieldName = String(field.field || '').toLowerCase()
  const text = `${componentType} ${dataType} ${fieldName}`
  if (field.dictType || ['select', 'dictselect', 'radio', 'radiogroup', 'switch', 'selector'].some(type => text.includes(type)))
    return BitableSelectIcon
  if (['checkbox', 'checkboxgroup', 'todo', 'boolean'].some(type => text.includes(type)))
    return BitableTodoIcon
  if (['number', 'inputnumber', 'counter', 'int', 'long', 'bigint', 'decimal', 'double', 'float'].some(type => text.includes(type)))
    return BitableNumberIcon
  if (['date', 'time', 'calendar', 'localdatetime', 'timestamp'].some(type => text.includes(type)))
    return BitableCalendarIcon
  if (['phone', 'mobile', 'tel'].some(type => text.includes(type)))
    return BitablePhoneIcon
  if (['email', 'mail'].some(type => text.includes(type)))
    return BitableMailIcon
  if (['file', 'image', 'upload', 'attachment'].some(type => text.includes(type)))
    return BitableAttachmentIcon
  if (['user', 'member', 'owner', 'creator', 'createby', 'updateby'].some(type => text.includes(type)))
    return BitableMemberIcon
  if (['lookup', 'relation', 'reference', 'org', 'dept', 'region', 'tree'].some(type => text.includes(type)))
    return BitableLookupIcon
  return BitableStyleIcon
}

function updateSelectedFieldRefs(fieldRefs = []) {
  if (!selectedBlock.value)
    return
  if (selectedBlockZoneKey.value === 'search' && selectedBlock.value.blockType === 'AiCrudPage') {
    patchBlockProps(selectedBlock.value.id, { searchFieldRefs: fieldRefs })
    return
  }
  patchBlock(selectedBlock.value.id, { fieldRefs })
}

function resolveFieldRoleEnabled(fieldName, role) {
  if (!fieldName)
    return false
  if (role === 'search')
    return resolveSelectedFieldRefs(selectedBlock.value, 'search').includes(fieldName)
  if (role === 'table')
    return resolveSelectedFieldRefs(selectedBlock.value, 'table').includes(fieldName)
  if (role === 'import')
    return resolveCrudFieldQuickValue(selectedBlock.value, fieldName, 'importable', true)
  if (role === 'export')
    return resolveCrudFieldQuickValue(selectedBlock.value, fieldName, 'exportable', true)
  return resolveFieldSetting(fieldName).editVisible !== false
}

function updateFieldRole(fieldName, role, enabled) {
  if (!selectedBlock.value || !fieldName)
    return
  if (role === 'import' || role === 'export') {
    updateCrudFieldQuickSetting(fieldName, role === 'import' ? 'importable' : 'exportable', enabled)
    return
  }
  if (role === 'search' || role === 'table') {
    const current = resolveSelectedFieldRefs(selectedBlock.value, role)
    const next = enabled
      ? Array.from(new Set([...current, fieldName]))
      : current.filter(ref => ref !== fieldName)
    if (role === 'search' && selectedBlock.value.blockType === 'AiCrudPage') {
      patchBlockProps(selectedBlock.value.id, { searchFieldRefs: next })
      return
    }
    if (role === 'table')
      patchBlock(selectedBlock.value.id, { fieldRefs: next })
    return
  }
  updateFieldSetting(fieldName, { editVisible: enabled })
}

function resolveFieldSetting(fieldName) {
  if (selectedBlockZoneKey.value === 'search' && selectedBlock.value?.blockType === 'AiCrudPage')
    return selectedBlock.value?.props?.searchFieldSettings?.[fieldName] || {}
  return selectedBlock.value?.props?.fieldSettings?.[fieldName] || {}
}

function resolveDefaultSearchComponentType(field = {}) {
  const componentType = field.componentType || field.dataType || 'input'
  if (field.dictType)
    return 'dictSelect'
  if (['int', 'bigint', 'decimal', 'double', 'float'].includes(field.dataType))
    return 'number'
  return componentType === 'inputNumber' ? 'number' : componentType
}

function resolveDefaultTableRenderType(field = {}) {
  const componentType = field.componentType || ''
  if (field.dictType)
    return 'dictTag'
  if (componentType === 'orgTreeSelect')
    return 'orgName'
  if (componentType === 'userSelect')
    return 'userName'
  if (componentType === 'regionTreeSelect')
    return 'regionName'
  if (componentType === 'fileUpload' || componentType === 'imageUpload')
    return componentType
  return ''
}

function isNameRenderType(renderType) {
  return ['orgName', 'userName', 'regionName', 'fileUpload', 'imageUpload'].includes(renderType)
}

function renderTargetFieldOptions(field = {}) {
  const options = queryFieldOptions.value.map(item => ({ ...item }))
  const defaultTarget = `${field.field}Name`
  if (!options.some(item => item.value === defaultTarget)) {
    options.unshift({
      label: `${defaultTarget}（默认翻译字段）`,
      value: defaultTarget,
    })
  }
  return options
}

function updateFieldSetting(fieldName, settingPatch) {
  if (!selectedBlock.value)
    return
  if (selectedBlockZoneKey.value === 'search' && selectedBlock.value.blockType === 'AiCrudPage') {
    patchBlockProps(selectedBlock.value.id, {
      searchFieldSettings: {
        ...(selectedBlock.value.props?.searchFieldSettings || {}),
        [fieldName]: {
          ...(selectedBlock.value.props?.searchFieldSettings?.[fieldName] || {}),
          ...settingPatch,
        },
      },
    })
    return
  }
  patchBlockProps(selectedBlock.value.id, {
    fieldSettings: {
      ...(selectedBlock.value.props?.fieldSettings || {}),
      [fieldName]: {
        ...(selectedBlock.value.props?.fieldSettings?.[fieldName] || {}),
        ...settingPatch,
      },
    },
  })
}

function applySelectedTableGlobalAlign(value) {
  if (!selectedBlock.value || !['data-table', 'AiCrudPage', 'AiTable'].includes(selectedBlock.value.blockType))
    return
  const align = ['left', 'center', 'right'].includes(value) ? value : 'left'
  const nextSettings = { ...(selectedBlock.value.props?.fieldSettings || {}) }
  ;(selectedFieldRefs.value || []).forEach((fieldName) => {
    nextSettings[fieldName] = {
      ...(nextSettings[fieldName] || {}),
      align,
    }
  })
  patchBlockProps(selectedBlock.value.id, {
    globalAlign: align,
    fieldSettings: nextSettings,
  })
}

function normalizeTableRowGap(value) {
  return Math.max(0, Math.min(32, Number(value ?? 8)))
}

// Metric editing
function updateMetric(idx, patch) {
  const list = [...(selectedBlock.value?.props?.metrics || [])]
  list[idx] = { ...list[idx], ...patch }
  patchBlockProps(selectedBlock.value.id, { metrics: list })
}
function addMetric() {
  const list = [...(selectedBlock.value?.props?.metrics || []), { label: '指标', value: '0', trend: '' }]
  patchBlockProps(selectedBlock.value.id, { metrics: list })
}
function removeMetric(idx) {
  const list = [...(selectedBlock.value?.props?.metrics || [])]
  list.splice(idx, 1)
  patchBlockProps(selectedBlock.value.id, { metrics: list })
}

function updateButtonGroupItem(idx, patch) {
  const list = [...(selectedBlock.value?.props?.buttons || [])]
  list[idx] = { ...list[idx], ...patch }
  patchBlockProps(selectedBlock.value.id, { buttons: list })
}
function addButtonGroupItem() {
  const list = [...(selectedBlock.value?.props?.buttons || []), { key: `btn_${Date.now()}`, text: '按钮', type: 'default' }]
  patchBlockProps(selectedBlock.value.id, { buttons: list })
}
function removeButtonGroupItem(idx) {
  const list = [...(selectedBlock.value?.props?.buttons || [])]
  list.splice(idx, 1)
  patchBlockProps(selectedBlock.value.id, { buttons: list })
}

function updateTagItem(idx, patch) {
  const list = [...(selectedBlock.value?.props?.tags || [])]
  list[idx] = { ...list[idx], ...patch }
  patchBlockProps(selectedBlock.value.id, { tags: list })
}
function addTagItem() {
  const list = [...(selectedBlock.value?.props?.tags || []), { label: '标签', type: 'default' }]
  patchBlockProps(selectedBlock.value.id, { tags: list })
}
function removeTagItem(idx) {
  const list = [...(selectedBlock.value?.props?.tags || [])]
  list.splice(idx, 1)
  patchBlockProps(selectedBlock.value.id, { tags: list })
}

function updateStepItem(idx, patch) {
  const list = [...(selectedBlock.value?.props?.steps || [])]
  list[idx] = { ...list[idx], ...patch }
  patchBlockProps(selectedBlock.value.id, { steps: list })
}
function addStepItem() {
  const list = [...(selectedBlock.value?.props?.steps || []), { title: '步骤', description: '' }]
  patchBlockProps(selectedBlock.value.id, { steps: list })
}
function removeStepItem(idx) {
  const list = [...(selectedBlock.value?.props?.steps || [])]
  list.splice(idx, 1)
  patchBlockProps(selectedBlock.value.id, { steps: list })
}

function updateTimelineItem(idx, patch) {
  const list = [...(selectedBlock.value?.props?.items || [])]
  list[idx] = { ...list[idx], ...patch }
  patchBlockProps(selectedBlock.value.id, { items: list })
}
function addTimelineItem() {
  const list = [...(selectedBlock.value?.props?.items || []), { title: '节点', time: '', content: '' }]
  patchBlockProps(selectedBlock.value.id, { items: list })
}
function removeTimelineItem(idx) {
  const list = [...(selectedBlock.value?.props?.items || [])]
  list.splice(idx, 1)
  patchBlockProps(selectedBlock.value.id, { items: list })
}

function updateOptionItem(propName = 'items', idx, patch) {
  const list = [...(selectedBlock.value?.props?.[propName] || [])]
  list[idx] = { ...(list[idx] || {}), ...patch }
  patchBlockProps(selectedBlock.value.id, { [propName]: list })
}

function updateOptionSource(patch = {}) {
  if (!selectedBlock.value)
    return
  patchBlockProps(selectedBlock.value.id, {
    optionSource: {
      ...(selectedBlock.value.props?.optionSource || {}),
      ...patch,
    },
  })
}

function updateWidgetDataBinding(patch = {}) {
  if (!selectedBlock.value)
    return
  const next = {
    ...(selectedBlock.value.props?.dataBinding || {}),
    ...patch,
  }
  if (next.sourceType === 'static')
    next.enabled = false
  else
    next.enabled = true
  patchBlockProps(selectedBlock.value.id, { dataBinding: next })
}

function addOptionItem(propName = 'items') {
  const fallback = propName === 'options'
    ? { label: '新选项', value: `option_${Date.now()}` }
    : { label: '标签', value: '内容' }
  const list = [...(selectedBlock.value?.props?.[propName] || []), fallback]
  patchBlockProps(selectedBlock.value.id, { [propName]: list })
}

function removeOptionItem(propName = 'items', idx) {
  const list = [...(selectedBlock.value?.props?.[propName] || [])]
  list.splice(idx, 1)
  patchBlockProps(selectedBlock.value.id, { [propName]: list })
}

function resolveBooleanKeys(source = {}, keys = []) {
  return keys.filter(key => source?.[key] === true)
}

function updateLinkLikeTitle(value = '') {
  if (!selectedBlock.value)
    return
  const key = selectedBlock.value.blockType === 'link' ? 'text' : 'title'
  patchBlockProps(selectedBlock.value.id, { [key]: value })
}

function updateLinkLikeUrl(value = '') {
  if (!selectedBlock.value)
    return
  const key = selectedBlock.value.blockType === 'link' ? 'href' : 'src'
  patchBlockProps(selectedBlock.value.id, { [key]: value })
}

function updateCodeColor(value = '') {
  if (!selectedBlock.value)
    return
  if (selectedBlock.value.blockType === 'barcode') {
    patchBlockProps(selectedBlock.value.id, { lineColor: value || '#0f172a' })
    return
  }
  patchBlockProps(selectedBlock.value.id, { foreground: value || '#0f172a' })
}

function clonePlainValue(value) {
  return JSON.parse(JSON.stringify(value || {}))
}

function createCustomActionClientKey(prefix = 'custom_action') {
  return `${prefix}_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`
}

function getCustomActionIdentity(action = {}) {
  if (action && typeof action === 'object')
    return action.clientKey || action.key || ''
  return String(action || '')
}

function addCustomAction(position = 'row') {
  const list = [...customActionList.value]
  const normalizedPosition = ['toolbar', 'row', 'detail'].includes(position) ? position : 'row'
  const clientKey = createCustomActionClientKey()
  list.push({
    clientKey,
    key: clientKey,
    label: '自定义按钮',
    position: normalizedPosition,
    type: 'primary',
    actionType: 'route',
    routePath: '',
    targetFormKey: '',
    openTarget: '_self',
    permissionCode: '',
    confirmText: '',
    displayCondition: '',
    successBehavior: 'none',
    successMessage: '',
    failureMessage: '',
    actionConfig: normalizeCustomApiConfig({}),
    params: [],
  })
  persistCustomActionList(list)
  activeActionIndex.value = list.length - 1
  if (!['toolbar', 'row'].includes(position))
    customActionModalOpen.value = true
}

function openCustomActionManager(actionOrIdentity = '') {
  if (!customActionList.value.length) {
    addCustomAction('toolbar')
    activeActionIndex.value = 0
    customActionModalOpen.value = true
    return
  }
  const index = actionOrIdentity ? findCustomActionIndexByIdentity(actionOrIdentity) : -1
  activeActionIndex.value = index >= 0 ? index : 0
  if (isApiCustomAction(customActionList.value[activeActionIndex.value]))
    loadEnabledApiConfigs()
  customActionModalOpen.value = true
}

function createAndEditCustomAction(position = 'toolbar') {
  addCustomAction(position)
  customActionModalOpen.value = true
}

function updateCustomAction(idx, patch) {
  const list = [...customActionList.value]
  list[idx] = { ...list[idx], ...patch }
  persistCustomActionList(list)
}

function findCustomActionIndexByIdentity(actionOrIdentity = '') {
  const identity = getCustomActionIdentity(actionOrIdentity)
  if (!identity)
    return -1
  return customActionList.value.findIndex(action => getCustomActionIdentity(action) === identity)
}

function updateCustomActionByIdentity(actionOrIdentity = '', patch = {}) {
  const index = findCustomActionIndexByIdentity(actionOrIdentity)
  if (index < 0)
    return
  updateCustomAction(index, patch)
}

function toggleCustomActionVisible(action = {}) {
  updateCustomActionByIdentity(action, { visible: action.visible === false })
}

function openCustomActionEditor(actionOrIdentity = '') {
  const index = findCustomActionIndexByIdentity(actionOrIdentity)
  activeActionIndex.value = index
  if (isApiCustomAction(customActionList.value[index]))
    loadEnabledApiConfigs()
  customActionModalOpen.value = index >= 0
}

defineExpose({
  openCustomActionManager,
  createAndEditCustomAction,
})

function handleCustomActionGroupReorder(position = 'toolbar', rows = []) {
  const targetPosition = ['toolbar', 'row', 'detail'].includes(position) ? position : 'toolbar'
  const rowKeys = new Set(rows.map(action => getCustomActionIdentity(action)))
  const normalizedRows = rows.map(action => ({ ...action, position: targetPosition }))
  const next = []
  let inserted = false
  customActionList.value.forEach((action) => {
    if ((action.position || 'toolbar') === targetPosition) {
      if (!inserted) {
        next.push(...normalizedRows)
        inserted = true
      }
      return
    }
    if (!rowKeys.has(getCustomActionIdentity(action)))
      next.push(action)
  })
  if (!inserted)
    next.push(...normalizedRows)
  persistCustomActionList(next)
}

function customActionMoreOptions(action = {}) {
  const currentPosition = action.position || 'toolbar'
  return [
    { label: '设置', key: 'configure' },
    {
      label: currentPosition === 'toolbar' ? '移到行按钮' : '移到工具栏',
      key: currentPosition === 'toolbar' ? 'move-row' : 'move-toolbar',
    },
    { label: '复制', key: 'copy' },
    { label: '删除', key: 'delete' },
  ]
}

function handleCustomActionMoreSelect(key = '', action = {}) {
  const index = findCustomActionIndexByIdentity(action)
  if (index < 0)
    return
  if (key === 'configure') {
    openCustomActionEditor(action)
    return
  }
  if (key === 'move-row') {
    updateCustomAction(index, { position: 'row' })
    return
  }
  if (key === 'move-toolbar') {
    updateCustomAction(index, { position: 'toolbar' })
    return
  }
  if (key === 'copy') {
    const list = [...customActionList.value]
    const clientKey = createCustomActionClientKey('custom_action_copy')
    const copied = {
      ...clonePlainValue(list[index]),
      clientKey,
      key: clientKey,
      label: `${list[index].label || '自定义按钮'} 副本`,
    }
    list.splice(index + 1, 0, copied)
    persistCustomActionList(list)
    activeActionIndex.value = index + 1
    return
  }
  if (key === 'delete')
    removeCustomActionByIdentity(action)
}

function removeCustomAction(idx) {
  const list = [...customActionList.value]
  if (idx < 0 || idx >= list.length)
    return
  list.splice(idx, 1)
  persistCustomActionList(list)
  activeActionIndex.value = list.length ? Math.min(idx, list.length - 1) : 0
}

function removeCustomActionByIdentity(actionOrIdentity = '') {
  const identity = getCustomActionIdentity(actionOrIdentity)
  if (!identity)
    return
  const list = customActionList.value.filter(action => getCustomActionIdentity(action) !== identity)
  if (list.length === customActionList.value.length)
    return
  persistCustomActionList(list)
  activeActionIndex.value = list.length ? Math.min(activeActionIndex.value, list.length - 1) : 0
}

function persistCustomActionList(list = []) {
  const normalized = normalizeCustomActionList(list)
  if (externalCustomActionsEnabled.value) {
    emit('update:customActions', normalized)
  }
  if (selectedBlock.value && ['AiCrudPage', 'toolbar', 'data-table', 'AiTable'].includes(selectedBlock.value.blockType)) {
    patchBlockProps(selectedBlock.value.id, { customActions: normalized })
  }
}

function updateActiveCustomAction(patch) {
  if (activeActionIndex.value < 0)
    return
  updateCustomAction(activeActionIndex.value, patch)
}

function updateActiveCustomActionType(value) {
  const actionType = normalizeCustomActionType(value)
  const patch = { actionType }
  if (actionType === 'CALL_API') {
    const nextConfig = normalizeCustomApiConfig(activeAction.value?.actionConfig)
    if (!nextConfig.url && activeAction.value?.routePath)
      nextConfig.url = activeAction.value.routePath
    patch.actionConfig = nextConfig
    patch.openTarget = '_self'
    loadEnabledApiConfigs()
  }
  else if (actionType === 'START_FLOW') {
    patch.actionConfig = { ...(activeAction.value?.actionConfig || {}), useMainFlow: true }
    patch.routePath = ''
    patch.openTarget = '_self'
  }
  else if (actionType === 'TRIGGER') {
    patch.actionConfig = { ...(activeAction.value?.actionConfig || {}), triggerCode: activeAction.value?.routePath || '' }
    patch.openTarget = '_self'
  }
  updateActiveCustomAction(patch)
}

function updateActiveActionConfig(patch = {}) {
  const current = normalizeCustomApiConfig(activeAction.value?.actionConfig)
  updateActiveCustomAction({
    actionType: 'CALL_API',
    actionConfig: {
      ...current,
      ...patch,
    },
  })
}

function updateActiveGenericActionConfig(patch = {}) {
  updateActiveCustomAction({
    actionConfig: {
      ...(activeAction.value?.actionConfig || {}),
      ...patch,
    },
    ...(Object.prototype.hasOwnProperty.call(patch, 'triggerCode') ? { routePath: patch.triggerCode || '' } : {}),
  })
}

async function loadEnabledApiConfigs() {
  if (apiConfigLoaded.value || apiConfigLoading.value)
    return
  apiConfigLoading.value = true
  try {
    const res = await enabledApiConfigs()
    apiConfigs.value = Array.isArray(res.data) ? res.data : []
    apiConfigLoaded.value = true
  }
  catch (error) {
    apiConfigs.value = []
    apiConfigLoaded.value = true
    console.warn('[ListPageGridDesigner] API配置不可用，已切换为手工输入模式', error?.message || error)
  }
  finally {
    apiConfigLoading.value = false
  }
}

async function loadSystemMenuPages() {
  if (systemMenuPageLoaded.value || systemMenuPageLoading.value)
    return
  systemMenuPageLoading.value = true
  try {
    const res = await request.get('/system/resource/tree')
    systemMenuPages.value = normalizeResourceTreeResponse(res)
    systemMenuPageLoaded.value = true
  }
  catch (error) {
    systemMenuPages.value = []
    systemMenuPageLoaded.value = true
    console.warn('[ListPageGridDesigner] 系统菜单资源不可用，已保留手工输入模式', error?.message || error)
  }
  finally {
    systemMenuPageLoading.value = false
  }
}

function applyCustomActionApiConfig(value) {
  const configId = value === undefined || value === null ? '' : String(value)
  const selected = apiConfigs.value.find(item => String(item.id || item.apiCode || item.urlPath) === configId)
  const patch = { apiConfigId: configId || null }
  if (selected) {
    patch.apiCode = selected.apiCode || ''
    patch.apiName = selected.apiName || ''
    patch.method = selected.needEncrypt && String(selected.reqMethod || '').toUpperCase() === 'POST'
      ? 'POST_ENCRYPT'
      : normalizeCustomApiMethod(selected.reqMethod || 'POST')
    patch.url = selected.urlPath || ''
  }
  updateActiveActionConfig(patch)
}

function applySystemMenuPageTarget(value) {
  const routePath = value === undefined || value === null ? '' : String(value)
  const selected = systemMenuPageTargetOptions.value.find(item => item.value === routePath)
  updateActiveCustomAction({
    routePath,
    actionConfig: {
      ...(activeAction.value?.actionConfig || {}),
      targetPath: routePath,
      targetMenuId: selected?.raw?.id || '',
      targetMenuName: selected?.raw?.resourceName || '',
    },
  })
}

function addApiActionParam() {
  const config = normalizeCustomApiConfig(activeAction.value?.actionConfig)
  const target = normalizeCustomApiMethod(config.method) === 'GET' ? 'query' : 'body'
  updateActiveActionConfig({
    params: [
      ...config.params,
      normalizeCustomApiParam({
        target,
        sourceType: 'rowField',
      }),
    ],
  })
}

function updateApiActionParam(paramIdx, patch) {
  const config = normalizeCustomApiConfig(activeAction.value?.actionConfig)
  const params = [...config.params]
  params[paramIdx] = normalizeCustomApiParam({ ...(params[paramIdx] || {}), ...patch })
  updateActiveActionConfig({ params })
}

function removeApiActionParam(paramIdx) {
  const config = normalizeCustomApiConfig(activeAction.value?.actionConfig)
  const params = [...config.params]
  params.splice(paramIdx, 1)
  updateActiveActionConfig({ params })
}

function addActionParam() {
  const params = [...(activeAction.value?.params || []), createActionParam()]
  updateActiveCustomAction({ params })
}

function updateActionParam(idx, patch) {
  const params = [...(activeAction.value?.params || [])]
  params[idx] = { ...params[idx], ...patch }
  updateActiveCustomAction({ params })
}

function removeActionParam(idx) {
  const params = [...(activeAction.value?.params || [])]
  params.splice(idx, 1)
  updateActiveCustomAction({ params })
}

function createActionParam() {
  return {
    name: '',
    sourceType: 'static',
    sourceField: '',
    value: '',
  }
}

function normalizeParamSourcePatch(sourceType = 'static', param = {}) {
  return {
    sourceType,
    sourceField: '',
    value: sourceType === 'static' ? (param.value || '') : '',
  }
}

function buildParamValuePatch(param = {}, sourceField = '') {
  const sourceType = param.sourceType || 'static'
  return {
    sourceField: sourceField || '',
    value: resolveParamTemplateValue(sourceType, sourceField),
  }
}

function resolveParamTemplateValue(sourceType = 'static', sourceField = '') {
  if (!sourceField)
    return ''
  if (sourceType === 'rowField')
    return `:${sourceField}`
  if (sourceType === 'routeQuery')
    return `\${route.${sourceField}}`
  if (sourceType === 'system')
    return `\${system.${sourceField}}`
  return ''
}

function normalizeApiParamSourcePatch(sourceType = 'rowField', param = {}) {
  return {
    sourceType,
    sourceField: '',
    value: sourceType === 'static' ? (param.value || '') : '',
  }
}

function resolveSystemMenuPageTargetValue(action = {}) {
  const routePath = String(action?.routePath || action?.actionConfig?.targetPath || '').trim()
  if (!routePath)
    return null
  return systemMenuPageTargetOptions.value.some(item => item.value === routePath) ? routePath : null
}

function normalizeCustomActionType(value) {
  const normalized = String(value || 'route')
    .replace(/[-\s]+/g, '_')
  const upper = normalized.toUpperCase()
  if (['CALL_API', 'REQUEST'].includes(upper))
    return 'CALL_API'
  if (['START_FLOW', 'START_APPROVAL'].includes(upper))
    return 'START_FLOW'
  if (upper === 'TRIGGER')
    return 'TRIGGER'
  if (upper === 'EXTERNAL')
    return 'external'
  if (upper === 'REFRESH')
    return 'refresh'
  return 'route'
}

function resolveActionBehaviorValue(value) {
  return normalizeCustomActionType(value)
}

function resolveActionBehaviorLabel(action = {}) {
  return actionBehaviorOptions.find(item => item.value === resolveActionBehaviorValue(action.actionType))?.label || '站内跳转'
}

function normalizeCustomActionList(list = []) {
  return (Array.isArray(list) ? list : [])
    .filter(action => action && typeof action === 'object')
    .map((action, index) => {
      const actionType = normalizeCustomActionType(action.actionType)
      const actionConfig = actionType === 'CALL_API'
        ? normalizeCustomApiConfig(action.actionConfig)
        : normalizeGenericActionConfig(actionType, action.actionConfig)
      const actionParams = actionType === 'CALL_API'
        ? actionConfig.params || []
        : action.params?.length ? action.params : actionConfig.params || []
      const clientKey = action.clientKey || action.key || `custom_${index + 1}`
      return {
        ...action,
        clientKey,
        key: normalizeActionKey(action.key || action.label) || `custom_${index + 1}`,
        label: action.label || '自定义按钮',
        position: ['toolbar', 'row', 'detail'].includes(action.position) ? action.position : 'row',
        type: action.type || defaultCustomActionButtonType(actionType),
        actionType,
        routePath: action.routePath || resolveCustomActionRoutePath(actionType, actionConfig),
        targetFormKey: action.targetFormKey || actionConfig.targetFormKey || '',
        openTarget: action.openTarget || actionConfig.openTarget || (actionType === 'external' ? '_blank' : '_self'),
        permissionCode: action.permissionCode || action.permission || '',
        confirmText: action.confirmText || '',
        displayCondition: action.displayCondition || actionConfig.displayCondition || '',
        successBehavior: action.successBehavior || actionConfig.successBehavior || 'none',
        successMessage: action.successMessage || actionConfig.successMessage || '',
        failureMessage: action.failureMessage || actionConfig.failureMessage || '',
        params: normalizeCustomActionParams(actionParams),
        actionConfig,
      }
    })
}

function normalizeGenericActionConfig(actionType = 'route', config = {}) {
  const source = config && typeof config === 'object' && !Array.isArray(config) ? config : parseJsonObject(config)
  if (actionType === 'START_FLOW') {
    return {
      ...source,
      useMainFlow: true,
    }
  }
  return {
    ...source,
    params: normalizeCustomActionParams(source.params || []),
  }
}

function normalizeCustomActionParams(params = []) {
  return (Array.isArray(params) ? params : [])
    .filter(param => param && typeof param === 'object')
    .map((param, index) => ({
      clientKey: param.clientKey || `param_${Date.now()}_${index}`,
      name: String(param.name || '').trim(),
      sourceType: ['rowField', 'routeQuery', 'static', 'system'].includes(param.sourceType) ? param.sourceType : 'static',
      sourceField: String(param.sourceField || '').trim(),
      value: param.value === undefined || param.value === null ? '' : String(param.value),
      target: ['path', 'query', 'body', 'header'].includes(param.target) ? param.target : '',
    }))
}

function defaultCustomActionButtonType(actionType = 'route') {
  if (actionType === 'START_FLOW')
    return 'success'
  if (actionType === 'CALL_API')
    return 'primary'
  if (actionType === 'TRIGGER')
    return 'warning'
  return 'default'
}

function resolveCustomActionRoutePath(actionType = 'route', config = {}) {
  if (actionType === 'external')
    return config.url || ''
  if (actionType === 'CALL_API')
    return config.url || ''
  if (actionType === 'TRIGGER')
    return config.triggerCode || ''
  return config.targetPath || ''
}

function isApiCustomAction(action = {}) {
  return normalizeCustomActionType(action?.actionType) === 'CALL_API'
}

function isRouteCustomAction(action = {}) {
  return normalizeCustomActionType(action?.actionType) === 'route'
}

function isStartFlowCustomAction(action = {}) {
  return normalizeCustomActionType(action?.actionType) === 'START_FLOW'
}

function isTriggerCustomAction(action = {}) {
  return normalizeCustomActionType(action?.actionType) === 'TRIGGER'
}

function isParamConfigurableAction(action = {}) {
  const actionType = normalizeCustomActionType(action?.actionType)
  return ['route', 'external'].includes(actionType)
}

function normalizeCustomApiConfig(config = {}) {
  const source = typeof config === 'string' ? parseJsonObject(config) : config && typeof config === 'object' ? config : {}
  const params = Array.isArray(source.params)
    ? source.params
    : Array.isArray(source.paramMappings)
      ? source.paramMappings
      : []
  return {
    ...source,
    apiConfigId: source.apiConfigId === undefined || source.apiConfigId === null ? null : String(source.apiConfigId),
    apiCode: String(source.apiCode || '').trim(),
    apiName: String(source.apiName || '').trim(),
    method: normalizeCustomApiMethod(source.method || source.reqMethod || source.apiMethod || 'POST'),
    url: String(source.url || source.apiUrl || source.urlPath || source.path || '').trim(),
    capabilityCode: String(source.capabilityCode || '').trim(),
    params: params.map(normalizeCustomApiParam).filter(Boolean),
  }
}

function resolveCustomApiUrl(action = {}) {
  const config = action.actionConfig && typeof action.actionConfig === 'object' ? action.actionConfig : {}
  return config.url || config.apiUrl || config.urlPath || config.path || action.routePath || ''
}

function normalizeCustomApiMethod(value) {
  const method = String(value || 'POST')
    .replace('-', '_')
    .toUpperCase()
  return apiMethodOptions.some(item => item.value === method) ? method : 'POST'
}

function normalizeCustomApiParam(param = {}) {
  const sourceType = ['rowField', 'routeQuery', 'static', 'system'].includes(param.sourceType) ? param.sourceType : 'rowField'
  const target = ['path', 'query', 'body', 'header'].includes(param.target) ? param.target : ''
  return {
    clientKey: param.clientKey || `param_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
    name: String(param.name || '').trim(),
    target,
    sourceType,
    sourceField: String(param.sourceField || '').trim(),
    value: param.value === undefined || param.value === null ? '' : String(param.value),
  }
}

function normalizeResourceTreeResponse(res) {
  if (Array.isArray(res))
    return res
  if (Array.isArray(res?.data))
    return res.data
  if (Array.isArray(res?.data?.records))
    return res.data.records
  if (Array.isArray(res?.data?.list))
    return res.data.list
  if (Array.isArray(res?.data?.children))
    return res.data.children
  return []
}

function buildSystemMenuPageTargetOptions(resources = []) {
  const result = []
  const seen = new Set()
  const walk = (items = [], parents = []) => {
    ;(Array.isArray(items) ? items : []).forEach((item) => {
      if (!item || typeof item !== 'object')
        return
      const name = String(item.resourceName || item.menuName || item.name || item.title || '').trim()
      const nextParents = name ? [...parents, name] : parents
      const routePath = normalizeSystemMenuRoutePath(item)
      if (Number(item.resourceType) === 2 && routePath && !seen.has(routePath)) {
        seen.add(routePath)
        result.push({
          label: `${nextParents.join(' / ')} · ${routePath}`,
          value: routePath,
          raw: item,
        })
      }
      if (Array.isArray(item.children) && item.children.length)
        walk(item.children, nextParents)
    })
  }
  walk(resources)
  return result
}

function normalizeSystemMenuRoutePath(resource = {}) {
  return String(resource.path || resource.routePath || resource.component || '').trim()
}

function parseJsonObject(value) {
  try {
    const parsed = JSON.parse(value || '{}')
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {}
  }
  catch {
    return {}
  }
}

function normalizeActionKey(value) {
  return String(value || '')
    .trim()
    .replace(/([a-z0-9])([A-Z])/g, '$1_$2')
    .replace(/\W/g, '_')
    .replace(/_+/g, '_')
    .toLowerCase()
    .replace(/^[^a-z]+/, '')
}

function normalizeParamName(value) {
  return String(value || '')
    .trim()
    .replace(/[^\w.-]/g, '')
}

function actionPathPlaceholder(action = {}) {
  const actionType = resolveActionBehaviorValue(action.actionType)
  if (actionType === 'external')
    return 'https://example.com/:id'
  if (actionType === 'refresh')
    return '刷新列表无需地址'
  if (actionType === 'CALL_API')
    return '/business/customer/audit/:id'
  return '/ai/xxx/:id'
}

// Tab editing
function updateTab(idx, patch) {
  const list = [...(selectedBlock.value?.props?.tabs || [])]
  list[idx] = { ...list[idx], ...patch }
  patchBlockProps(selectedBlock.value.id, { tabs: list })
}
function addTab() {
  const list = [...(selectedBlock.value?.props?.tabs || []), { key: `tab_${Date.now()}`, title: '新 Tab' }]
  patchBlockProps(selectedBlock.value.id, { tabs: list })
}
function removeTab(idx) {
  const list = [...(selectedBlock.value?.props?.tabs || [])]
  list.splice(idx, 1)
  patchBlockProps(selectedBlock.value.id, { tabs: list })
}

function propertySectionVisible(keywords = []) {
  const keyword = String(propertyKeyword.value || '').trim().toLowerCase()
  if (!keyword)
    return true
  const sectionMatched = keywords.some(item => String(item || '').toLowerCase().includes(keyword))
  if (sectionMatched)
    return true
  const blockText = [
    selectedBlock.value?.label,
    selectedBlock.value?.blockType,
    selectedBlock.value?.id,
    JSON.stringify(selectedBlock.value?.props || {}),
    JSON.stringify(selectedBlock.value?.fieldRefs || []),
  ].join(' ').toLowerCase()
  if (blockText.includes(keyword))
    return true
  const knownSectionMatched = propertySearchKeywordRegistry.some(item => item.includes(keyword))
  return !knownSectionMatched
}

function resolvePropertySearchTab(keyword = '') {
  const text = String(keyword || '').trim().toLowerCase()
  if (!text)
    return propertyPanelTab.value
  if (isPropertySearchTabMatched('props', text))
    return 'props'
  if (isPropertySearchTabMatched('style', text))
    return 'style'
  if (isPropertySearchTabMatched('interaction', text))
    return 'interaction'
  return 'props'
}

function isPropertySearchTabMatched(tab, keyword) {
  return (propertySearchTabIndex[tab] || []).some((item) => {
    const text = String(item || '').toLowerCase()
    return text === keyword || text.includes(keyword) || keyword.includes(text)
  })
}

function scrollPropertySearchTarget(keyword = '') {
  const panel = propertyPanelRef.value
  const text = String(keyword || '').trim().toLowerCase()
  if (!panel || !text)
    return
  const anchors = Array.from(panel.querySelectorAll('[data-property-search]'))
    .filter(anchor => anchor.offsetParent !== null)
  const target = anchors.find(anchor => isPropertyAnchorMatched(anchor, text)) || anchors[0]
  if (!target)
    return
  const panelRect = panel.getBoundingClientRect()
  const targetRect = target.getBoundingClientRect()
  panel.scrollTo({
    top: Math.max(0, panel.scrollTop + targetRect.top - panelRect.top - 8),
    behavior: 'smooth',
  })
}

function isPropertyAnchorMatched(anchor, keyword = '') {
  const text = String(anchor?.dataset?.propertySearch || '').toLowerCase()
  return text === keyword || text.includes(keyword) || keyword.includes(text)
}

function toggleCanvasFocus() {
  const nextFocused = !canvasFocusMode.value
  canvasFocusMode.value = nextFocused
  if (nextFocused)
    centerCanvasViewport()
}

function centerCanvasViewport() {
  nextTick(() => {
    const scrollEl = canvasScrollRef.value
    if (!scrollEl)
      return
    scrollEl.scrollTo({
      left: Math.max(0, (scrollEl.scrollWidth - scrollEl.clientWidth) / 2),
      top: scrollEl.scrollTop,
      behavior: 'smooth',
    })
  })
}

function selectPropertyPanelTab(tab) {
  propertyPanelTab.value = tab
}
function resolveCrudFieldKey(field = {}) {
  return String(field.field || field.fieldCode || field.code || field.prop || field.key || field.id || '').trim()
}

function resolveCrudFieldLabel(field = {}) {
  return field.label || field.fieldName || field.name || field.title || resolveCrudFieldKey(field)
}

function resolveCrudFieldQuickValue(block = {}, fieldKey = '', settingKey = '', fallback = false) {
  const setting = block.props?.fieldSettings?.[fieldKey] || {}
  if (Object.prototype.hasOwnProperty.call(setting, settingKey))
    return setting[settingKey] === true
  if (settingKey === 'searchable' && Object.prototype.hasOwnProperty.call(setting, 'showInSearch'))
    return setting.showInSearch === true
  return fallback === true
}

function updateCrudFieldQuickSetting(fieldKey = '', settingKey = '', value = false) {
  if (!selectedBlock.value?.id || !fieldKey || !settingKey)
    return
  const blockProps = selectedBlock.value.props || {}
  const fieldSettings = { ...(blockProps.fieldSettings || {}) }
  const previous = { ...(fieldSettings[fieldKey] || {}) }
  fieldSettings[fieldKey] = {
    ...previous,
    field: fieldKey,
    [settingKey]: value === true,
  }
  if (settingKey === 'searchable')
    fieldSettings[fieldKey].showInSearch = value === true

  patchBlockProps(selectedBlock.value.id, {
    fieldSettings,
    ...buildCrudFieldListPatch(blockProps, fieldKey, settingKey, value === true),
  })
}

function buildCrudFieldListPatch(blockProps = {}, fieldKey = '', settingKey = '', enabled = false) {
  const propName = {
    searchable: 'searchFields',
    importable: 'importFields',
    exportable: 'exportFields',
  }[settingKey]
  if (!propName)
    return {}
  const currentList = Array.isArray(blockProps[propName]) ? blockProps[propName] : []
  const nextSet = new Set(currentList.map(item => String(item || '').trim()).filter(Boolean))
  if (enabled)
    nextSet.add(fieldKey)
  else
    nextSet.delete(fieldKey)
  return { [propName]: Array.from(nextSet) }
}
</script>

<style scoped>
.list-grid-designer {
  position: relative;
  display: grid;
  grid-template-columns: 224px minmax(0, 1fr) 320px;
  gap: 0;
  height: 100%;
  min-height: 0;
  min-width: 0;
  overflow: hidden;
}

.list-grid-designer.left-collapsed {
  grid-template-columns: minmax(0, 1fr) 320px;
}

.list-grid-designer.right-collapsed {
  grid-template-columns: 224px minmax(0, 1fr);
}

.list-grid-designer.left-collapsed.right-collapsed {
  grid-template-columns: minmax(0, 1fr);
}

.list-grid-designer.canvas-focused {
  grid-template-columns: minmax(0, 1fr);
}

.list-grid-designer.canvas-focused .side-rail-toggle-button {
  display: none;
}

.list-grid-designer.canvas-focused .canvas-scroll {
  padding: 0 14px 14px;
}

.list-grid-designer.canvas-focused .canvas-zoom-stage {
  min-width: max-content;
  margin-right: auto;
  margin-left: auto;
  padding: 64px 12px 24px;
}

.list-grid-designer.readonly,
.list-grid-designer.readonly.left-collapsed,
.list-grid-designer.readonly.right-collapsed,
.list-grid-designer.readonly.left-collapsed.right-collapsed {
  grid-template-columns: minmax(0, 1fr);
  min-height: 0;
  background: #fff;
}

.palette-panel,
.canvas-panel,
.block-property-panel,
.property-panel {
  border: 0;
  border-radius: 0;
  background: #fff;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.palette-panel {
  min-height: 0;
  padding: 10px;
  border-right: 1px solid #e4e4e7;
  background: #fcfcfc;
}

.block-property-panel,
.property-panel {
  border-left: 1px solid #e4e4e7;
  background: #fafafa;
}

.palette-panel-head,
.property-panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.panel-collapse-button {
  --n-color: #fff !important;
  --n-color-hover: #f4f6ff !important;
  --n-color-pressed: #e8edff !important;
  --n-color-focus: #fff !important;
  --n-border: 1px solid #e4e4e7 !important;
  --n-border-hover: 1px solid #c7d2fe !important;
  --n-border-pressed: 1px solid #a5b4fc !important;
  --n-border-focus: 1px solid #c7d2fe !important;
  --n-text-color: #52525b !important;
  --n-text-color-hover: #3153d8 !important;
}

.side-rail-toggle-button {
  position: absolute;
  z-index: 20;
  top: 50%;
  display: grid;
  place-items: center;
  width: 30px;
  height: 44px;
  border: 1px solid #dbe3ee;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.14);
  color: #1d4ed8;
  cursor: pointer;
  transform: translateY(-50%);
}

.side-rail-toggle-button.left {
  left: 6px;
}

.side-rail-toggle-button.right {
  right: 6px;
}

.side-rail-toggle-button:hover {
  border-color: #93c5fd;
  background: #eff6ff;
  box-shadow: 0 10px 22px rgba(37, 99, 235, 0.18);
}

.panel-title {
  font-size: 13px;
  font-weight: 700;
  color: #18181b;
}

.panel-desc {
  margin-top: 2px;
  font-size: 11px;
  color: #71717a;
}

.palette-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 6px;
}

.palette-stats span,
.group-title em {
  display: inline-flex;
  align-items: center;
  height: 18px;
  padding: 0 6px;
  border: 1px solid #dbeafe;
  border-radius: 999px;
  background: #eff6ff;
  color: #2563eb;
  font-size: 11px;
  font-style: normal;
  font-weight: 700;
  line-height: 16px;
}

.palette-groups {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  overflow-y: auto;
}

.palette-search {
  margin-top: 10px;
}

.palette-empty {
  display: grid;
  place-items: center;
  min-height: 88px;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
  color: #94a3b8;
  font-size: 12px;
}

.group-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  color: #52525b;
  margin-bottom: 6px;
}

.group-title span {
  min-width: 0;
}

.group-title::before {
  content: '';
  position: relative;
  z-index: 1;
  flex: 0 0 auto;
  width: 6px;
  height: 6px;
  margin-left: 3px;
  border-radius: 999px;
  background: #4266f7;
  outline: 3px solid #e8edff;
}

.palette-list {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 5px;
}

.palette-item {
  display: grid;
  grid-template-columns: 24px minmax(0, 1fr) auto;
  align-items: center;
  gap: 7px;
  min-height: 40px;
  padding: 6px 7px;
  border: 1px solid #e4e4e7;
  border-radius: 7px;
  background: rgba(255, 255, 255, 0.9);
  text-align: left;
  cursor: grab;
  transition: all 0.15s;
}

.palette-item:hover:not(.is-disabled) {
  border-color: #c7d2fe;
  background: #f8faff;
  box-shadow: 0 6px 14px rgba(49, 83, 216, 0.08);
  transform: translateY(-1px);
}

.item-icon {
  display: grid;
  place-items: center;
  width: 24px;
  height: 24px;
  border-radius: 6px;
  background: #f4f4f5;
  color: #64748b;
  transition:
    background 0.15s,
    color 0.15s;
}

.item-icon :deep(.n-icon) {
  font-size: 16px;
}

.palette-item:hover:not(.is-disabled) .item-icon {
  background: #eff6ff;
  color: #2563eb;
}

.item-main {
  display: grid;
  gap: 1px;
  min-width: 0;
}

.palette-item.is-disabled {
  border-color: #e2e8f0;
  background: #f8fafc;
}

.palette-item.is-existing {
  cursor: pointer;
}

.palette-item.is-unavailable {
  cursor: not-allowed;
}

.palette-item.is-disabled .item-title,
.palette-item.is-disabled .item-desc,
.palette-item.is-disabled .item-icon {
  color: #94a3b8;
}

.palette-item.is-disabled:hover {
  border-color: #cbd5e1;
  background: #f8fafc;
}

.item-title {
  overflow: hidden;
  font-size: 12px;
  font-weight: 600;
  color: #27272a;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-desc {
  overflow: hidden;
  font-size: 11px;
  color: #71717a;
  line-height: 15px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-lock-reason {
  grid-column: 3;
  justify-self: end;
  padding: 1px 6px;
  border-radius: 999px;
  background: #e2e8f0;
  color: #64748b;
  font-size: 10px;
  font-weight: 600;
  line-height: 16px;
}

.canvas-panel {
  position: relative;
  background:
    radial-gradient(circle at 1px 1px, rgba(100, 116, 139, 0.24) 1px, transparent 0),
    linear-gradient(180deg, #eef3f8 0%, #e7edf4 100%);
  background-size:
    20px 20px,
    auto;
  min-width: 0;
  min-height: 0;
}

.canvas-panel.drag-over {
  border-color: #93c5fd;
  box-shadow: inset 0 0 0 1px #bfdbfe;
}

.canvas-scroll {
  position: relative;
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 0 16px 16px;
  overscroll-behavior: contain;
}

.canvas-zoom-stage {
  position: relative;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  min-width: 100%;
  box-sizing: content-box;
  padding: 64px 1px 28px;
}

.canvas-toolbar {
  display: none;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 42px;
  padding: 6px 10px;
  border-bottom: 1px solid #e4e4e7;
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(10px);
}

.canvas-primary-actions {
  flex: 0 0 auto;
  flex-wrap: nowrap;
}

.canvas-viewport-dock {
  position: absolute;
  z-index: 8;
  top: 14px;
  left: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  width: max-content;
  max-width: calc(100% - 28px);
  margin: 0;
  padding: 5px 7px;
  border: 1px solid rgba(228, 228, 231, 0.9);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow:
    0 4px 12px rgba(15, 23, 42, 0.06),
    0 1px 3px rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(12px);
  color: #52525b;
  transform: translateX(-50%);
}

.viewport-divider {
  flex: 0 0 auto;
  width: 1px;
  height: 18px;
  margin: 0 2px;
  background: #e4e4e7;
}

.viewport-device-group {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding-right: 6px;
  border-right: 1px solid #e4e4e7;
}

.viewport-device-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  cursor: pointer;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: #a1a1aa;
  font-size: 16px;
  padding: 0;
  transition:
    background-color 160ms ease,
    color 160ms ease;
}

.viewport-device-button:hover,
.viewport-device-button.active {
  background: #f4f4f5;
  color: #3f3f46;
}

.canvas-viewport-popover {
  display: grid;
  gap: 10px;
}

.viewport-popover-head {
  display: grid;
  gap: 2px;
}

.viewport-popover-head strong {
  color: #0f172a;
  font-size: 13px;
  line-height: 18px;
}

.viewport-popover-head span {
  color: #64748b;
  font-size: 12px;
  line-height: 16px;
}

.canvas-viewport-field,
.canvas-viewport-zoom {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.canvas-viewport-field span,
.canvas-viewport-zoom > span {
  color: #64748b;
  font-size: 12px;
  font-weight: 600;
  line-height: 16px;
}

.canvas-viewport-zoom-actions {
  display: grid;
  grid-template-columns: 34px minmax(92px, 1fr) 34px;
  gap: 6px;
  align-items: center;
}

.viewport-icon-button,
.viewport-zoom-button {
  --n-color: #fff !important;
  --n-color-hover: #f4f6ff !important;
  --n-color-pressed: #e8edff !important;
  --n-color-focus: #fff !important;
  --n-border: 1px solid #e4e4e7 !important;
  --n-border-hover: 1px solid #c7d2fe !important;
  --n-border-pressed: 1px solid #a5b4fc !important;
  --n-border-focus: 1px solid #c7d2fe !important;
  --n-text-color: #52525b !important;
  --n-text-color-hover: #3153d8 !important;
  --n-text-color-pressed: #253fb2 !important;
  --n-text-color-focus: #3153d8 !important;
  font-weight: 700;
}

.viewport-icon-button {
  width: 28px;
  height: 28px;
  border-radius: 999px !important;
}

.viewport-zoom-button {
  min-width: 72px;
  height: 28px;
  padding: 0 8px !important;
  border-radius: 999px !important;
}

.canvas-width-select,
.canvas-width-input,
.canvas-preview-select,
.canvas-zoom-select {
  width: 100%;
}

.canvas-width-input :deep(.n-input__input-el) {
  text-align: center;
}

.toolbar-info strong {
  display: block;
  font-size: 13px;
  color: #0f172a;
  line-height: 17px;
}

.toolbar-info span {
  display: block;
  margin-top: 1px;
  font-size: 12px;
  color: #64748b;
  line-height: 16px;
}

.canvas-grid {
  position: relative;
  margin: 12px;
  box-sizing: content-box;
  padding: 0;
  border-radius: 8px;
  border: 1px solid #cbd5e1;
  background: #f3f6fa;
  box-shadow:
    0 18px 42px rgba(15, 23, 42, 0.12),
    inset 0 0 0 1px rgba(255, 255, 255, 0.72);
  will-change: transform;
}

.list-grid-designer.readonly .canvas-panel {
  border: 0;
  background: #fff;
}

.list-grid-designer.readonly .canvas-scroll {
  overflow: visible;
  background: #fff;
}

.list-grid-designer.readonly .canvas-zoom-stage {
  padding: 0;
  background: #fff;
}

.list-grid-designer.readonly .canvas-grid {
  margin: 0;
  background: #fff;
  box-shadow: none;
}

.list-grid-designer.readonly .grid-row,
.list-grid-designer.readonly .grid-col {
  display: none;
}

.list-grid-designer.readonly .grid-item:hover {
  z-index: auto;
}

.list-grid-designer.readonly .grid-item::after {
  display: none;
}

.canvas-panel.drag-over .canvas-grid {
  border-color: #2563eb;
  background: #eff6ff;
}

.grid-row,
.grid-col {
  position: absolute;
  pointer-events: none;
}

.grid-row {
  display: none;
}

.grid-col {
  top: 0;
  bottom: 0;
  border-right: 1px solid rgba(100, 116, 139, 0.08);
}

.grid-item {
  position: absolute;
  cursor: default;
  transition:
    opacity 120ms ease,
    transform 120ms ease,
    box-shadow 160ms ease;
}

.grid-item::after {
  content: '';
  position: absolute;
  inset: -2px;
  z-index: 24;
  border: 1px solid transparent;
  border-radius: 2px;
  pointer-events: none;
  box-shadow: none;
  transition:
    border-color 160ms ease,
    box-shadow 160ms ease;
}

.grid-item:hover {
  z-index: 12;
}

.grid-item:hover::after {
  border-color: #60a5fa;
  box-shadow: 0 0 0 1px rgba(96, 165, 250, 0.12);
}

.grid-item.selected {
  z-index: 10;
  filter: none;
}

.grid-item.selected::after {
  border-color: #2563eb;
  box-shadow: 0 0 0 1px rgba(37, 99, 235, 0.26);
}

.grid-item.moving {
  opacity: 0.96;
  z-index: 32;
  transition: none;
}

.grid-item.moving,
.grid-item.moving * {
  cursor: grabbing !important;
  user-select: none;
}

.block-node-overlay {
  position: absolute;
  top: 6px;
  right: 6px;
  left: 6px;
  z-index: 28;
  height: 24px;
  opacity: 0.58;
  pointer-events: auto;
  transition: opacity 160ms ease;
}

.grid-item:hover .block-node-overlay,
.grid-item.selected .block-node-overlay {
  opacity: 1;
}

.block-drag-handle,
.block-menu-trigger {
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

.block-drag-handle {
  position: absolute;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  cursor: grab;
}

.block-drag-handle svg {
  width: 15px;
  height: 15px;
  transform: rotate(90deg);
}

.block-menu-trigger {
  position: absolute;
  top: 0;
  right: 0;
  padding: 0;
  background: #1d4ed8;
  color: #fff;
  cursor: pointer;
}

.block-drag-handle:hover,
.block-menu-trigger:hover {
  background: #2563eb;
  color: #fff;
}

.block-drag-handle:active {
  cursor: grabbing;
}

.drop-preview {
  position: absolute;
  z-index: 20;
  display: grid;
  place-items: center;
  border: 1px dashed #2563eb;
  border-radius: 8px;
  background: rgba(239, 246, 255, 0.84);
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 700;
  pointer-events: none;
  box-shadow: 0 8px 20px rgba(37, 99, 235, 0.12);
}

.drop-preview-blocked {
  border-color: #ef4444;
  background: rgba(254, 242, 242, 0.9);
  color: #b91c1c;
  box-shadow: 0 8px 20px rgba(239, 68, 68, 0.12);
}

.move-placeholder {
  position: absolute;
  z-index: 18;
  display: grid;
  place-items: center;
  border: 2px solid #2563eb;
  border-radius: 10px;
  background: repeating-linear-gradient(
    -45deg,
    rgba(37, 99, 235, 0.12) 0,
    rgba(37, 99, 235, 0.12) 8px,
    rgba(219, 234, 254, 0.55) 8px,
    rgba(219, 234, 254, 0.55) 16px
  );
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 700;
  pointer-events: none;
  box-shadow:
    inset 0 0 0 1px rgba(255, 255, 255, 0.85),
    0 12px 24px rgba(37, 99, 235, 0.16);
}

.resize-anchor {
  position: absolute;
  z-index: 31;
  display: none;
  width: 10px;
  height: 10px;
  border: 2px solid #fff;
  border-radius: 999px;
  background: #1d4ed8;
  box-shadow: 0 2px 6px rgba(29, 78, 216, 0.36);
}

.resize-anchor::before {
  content: '';
  position: absolute;
  inset: -8px;
  border-radius: 999px;
}

.grid-item.selected .resize-anchor {
  display: block;
}

.anchor-top-left {
  top: -7px;
  left: -7px;
  cursor: nwse-resize;
}

.anchor-top {
  top: -7px;
  left: 50%;
  transform: translateX(-50%);
  cursor: ns-resize;
}

.anchor-top-right {
  top: -7px;
  right: -7px;
  cursor: nesw-resize;
}

.anchor-right {
  top: 50%;
  right: -7px;
  transform: translateY(-50%);
  cursor: ew-resize;
}

.anchor-bottom-right {
  right: -7px;
  bottom: -7px;
  cursor: nwse-resize;
}

.anchor-bottom {
  bottom: -7px;
  left: 50%;
  transform: translateX(-50%);
  cursor: ns-resize;
}

.anchor-bottom-left {
  bottom: -7px;
  left: -7px;
  cursor: nesw-resize;
}

.anchor-left {
  top: 50%;
  left: -7px;
  transform: translateY(-50%);
  cursor: ew-resize;
}

.block-property-panel {
  min-width: 0;
  min-height: 0;
}

.list-grid-designer.panel-only {
  display: block;
  height: 100%;
}

.list-grid-designer.panel-only > :not(.block-property-panel) {
  display: none !important;
}

.list-grid-designer.panel-only .block-property-panel {
  width: 100%;
  height: 100%;
  border-left: 0;
}

.property-panel-head {
  align-items: center;
  min-height: 48px;
  padding: 0 10px 0 12px;
  border-bottom: 1px solid #e4e4e7;
  background: #fafafa;
}

.property-panel-head > div:first-child {
  min-width: 0;
}

.property-panel-tabs {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 3px;
  flex: 0 0 auto;
  margin: 6px 8px;
  padding: 3px;
  border: 1px solid #e4e4e7;
  border-radius: 9px;
  background: #f4f4f5;
}

.property-panel-tabs button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  height: 28px;
  cursor: pointer;
  border: 0;
  border-radius: 7px;
  background: transparent;
  color: #71717a;
  font-size: 12px;
  font-weight: 600;
}

.property-panel-tabs button .n-icon {
  color: #71717a;
  font-size: 14px;
}

.property-panel-tabs button:hover {
  background: rgba(228, 228, 231, 0.72);
  color: #27272a;
}

.property-panel-tabs button.active {
  background: #fff;
  color: #27272a;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.08);
}

.property-panel-tabs button.active .n-icon {
  color: #27272a;
}

.property-search,
.designer-panel-search {
  position: relative;
  z-index: 3;
  flex: 0 0 auto;
  padding: 8px 10px;
  border-bottom: 1px solid #e4e4e7;
  background: #fafafa;
  box-shadow: none;
}

.property-panel-title {
  color: #18181b;
  font-size: 13px;
  font-weight: 600;
  line-height: 20px;
}

.property-panel-desc {
  margin-top: 2px;
  color: #71717a;
  font-size: 11px;
  line-height: 16px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.property-panel {
  flex: 1;
  min-height: 0;
  border: 0;
  border-radius: 0;
  background: #fafafa;
  padding: 8px;
  overflow: auto;
}

.property-empty {
  display: grid;
  place-items: center;
  height: 100%;
  color: #94a3b8;
  font-size: 13px;
}

.prop-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 12px;
  padding: 12px;
  border: 1px solid rgba(228, 228, 231, 0.72);
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
}

.prop-title {
  color: #27272a;
  font-size: 13px;
  font-weight: 650;
}

.prop-meta {
  margin-top: 2px;
  color: #71717a;
  font-size: 11px;
}

.property-body {
  display: grid;
  gap: 12px;
}

.property-tab-content {
  display: grid;
  gap: 10px;
  padding: 8px;
  overflow: hidden;
  border: 1px solid rgba(228, 228, 231, 0.72);
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
}

.property-search-anchor {
  height: 0;
  overflow: hidden;
}

.property-tab-content + .property-tab-content {
  margin-top: 8px;
}

.property-tab-content :deep(.n-divider) {
  justify-content: flex-start;
  margin: 0 -8px 2px;
  padding: 8px;
  border-top: 1px solid #f4f4f5;
  color: #3f3f46;
  font-size: 12px;
  font-weight: 650;
}

.property-tab-content :deep(.n-divider .n-divider__title) {
  padding: 0;
}

.property-tab-content :deep(.n-divider::before),
.property-tab-content :deep(.n-divider::after) {
  display: none;
}

.property-body :deep(.n-form-item) {
  margin-bottom: 10px;
  border: 0;
  border-radius: 0;
  background: transparent;
  padding: 0;
  box-shadow: none;
}

.property-body :deep(.n-form-item-label) {
  position: relative;
  display: flex !important;
  align-items: center;
  align-self: flex-start;
  min-height: 18px;
  height: auto;
  padding-left: 0;
  padding-top: 0;
  padding-bottom: 5px;
  color: #52525b;
  font-size: 11px;
  font-weight: 600;
  line-height: 16px;
}

.property-body :deep(.n-form-item-label--right-mark) {
  display: flex !important;
  align-items: center !important;
}

.property-body :deep(.n-form-item-label__text) {
  display: inline-flex;
  align-items: center;
  min-height: 20px;
  line-height: 20px;
  font-size: 11px;
}

.property-body :deep(.n-form-item-label::before) {
  display: none;
}

.property-body :deep(.n-input),
.property-body :deep(.n-input-number),
.property-body :deep(.n-base-selection) {
  --n-color: #f4f4f5 !important;
  --n-color-focus: #fff !important;
  --n-color-hover: #f4f4f5 !important;
  --n-border: 1px solid transparent !important;
  --n-border-hover: 1px solid #a5b4fc !important;
  --n-border-focus: 1px solid #6366f1 !important;
  --n-box-shadow-focus: 0 0 0 3px rgba(99, 102, 241, 0.1) !important;
  border-radius: 6px;
}

.property-body :deep(.n-input__input-el),
.property-body :deep(.n-input__textarea-el),
.property-body :deep(.n-base-selection-label) {
  color: #27272a !important;
  font-size: 12px !important;
}

.property-body :deep(.n-button) {
  --n-border-radius: 6px !important;
  font-size: 12px;
}

.event-editor,
.container-child-editor {
  display: grid;
  gap: 8px;
}

.copy-empty-state {
  display: grid;
  justify-items: center;
  gap: 5px;
  padding: 18px 14px;
  border: 1px dashed #d4d4d8;
  border-radius: 8px;
  background: #fafafa;
  color: #71717a;
  text-align: center;
}

.copy-empty-icon {
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  border: 1px solid #e4e4e7;
  border-radius: 999px;
  background: #fff;
  color: #a1a1aa;
  font-size: 18px;
  line-height: 1;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.05);
}

.copy-empty-state strong {
  color: #52525b;
  font-size: 12px;
  font-weight: 650;
  line-height: 18px;
}

.copy-empty-state small {
  max-width: 220px;
  color: #a1a1aa;
  font-size: 10px;
  line-height: 15px;
}

.form-modal-help {
  margin-bottom: 10px;
  padding: 8px 10px;
  border: 1px dashed #bfdbfe;
  border-radius: 8px;
  background: #eff6ff;
  color: #475569;
  font-size: 12px;
  line-height: 18px;
}

.form-modal-readonly-summary {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 10px 12px;
  background: var(--n-color-hover, #f7f8fa);
  border-radius: 6px;
  margin-bottom: 12px;
}

.readonly-summary-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  font-size: 12px;
}

.readonly-summary-row span {
  color: var(--n-text-color-3);
}

.readonly-summary-row strong {
  color: var(--n-text-color-1);
  font-weight: 500;
  text-align: right;
}

.event-row {
  display: grid;
  gap: 8px;
  padding: 10px;
  border: 1px solid rgba(228, 228, 231, 0.82);
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
}

.event-row-head,
.container-child-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-width: 0;
}

.event-row-head span,
.container-child-row span {
  min-width: 0;
  overflow: hidden;
  color: #27272a;
  font-size: 12px;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.event-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.event-setting-field {
  display: grid;
  gap: 5px;
  min-width: 0;
}

.event-setting-field > span {
  color: #71717a;
  font-size: 11px;
  font-weight: 600;
  line-height: 16px;
}

.event-grid :deep(.n-select),
.event-grid :deep(.n-input),
.event-param-row :deep(.n-select),
.event-param-row :deep(.n-input) {
  min-width: 0;
  width: 100%;
}

.grid-config-grid {
  display: grid;
  gap: 8px;
  width: 100%;
}

.grid-config-grid.three {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.grid-config-grid.four {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.grid-config-field,
.grid-config-switch {
  display: grid;
  gap: 5px;
  min-width: 0;
  padding: 8px 9px;
  border: 1px solid #e4e4e7;
  border-radius: 6px;
  background: #fafafa;
}

.grid-config-field > span,
.grid-config-switch > span {
  color: #52525b;
  font-size: 11px;
  font-weight: 600;
}

.grid-config-switch {
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
}

.grid-config-switch > span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.grid-cell-editor {
  display: grid;
  gap: 6px;
  padding: 9px;
  border: 1px solid #e4e4e7;
  border-radius: 8px;
  background: #fff;
}

.grid-cell-editor-head {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 72px auto;
  gap: 6px;
  align-items: center;
}

.grid-cell-span-input {
  width: 72px;
}

.event-param-list {
  display: grid;
  gap: 6px;
}

.event-param-row {
  display: grid;
  grid-template-columns: minmax(72px, 0.82fr) minmax(72px, 0.82fr) minmax(96px, 1.1fr) auto;
  gap: 6px;
  align-items: center;
  min-width: 0;
  padding: 7px;
  border: 1px solid #e4e4e7;
  border-radius: 6px;
  background: #fafafa;
}

.container-child-row {
  padding: 8px 10px;
  border: 1px solid #e4e4e7;
  border-radius: 6px;
  background: #fff;
}

.container-child-empty {
  display: grid;
  place-items: center;
  min-height: 64px;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
  color: #94a3b8;
  font-size: 12px;
  text-align: center;
}

.property-body :deep(.n-form-item-label--right-mark::before) {
  top: 50%;
  margin-top: 0;
}

.grid-pos-editor {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  width: 100%;
}

.style-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  width: 100%;
}

.style-grid.three {
  grid-template-columns: repeat(auto-fit, minmax(92px, 1fr));
}

.style-grid.four {
  grid-template-columns: repeat(auto-fit, minmax(92px, 1fr));
}

.style-grid :deep(.n-color-picker),
.style-grid :deep(.n-select),
.style-grid :deep(.n-input),
.style-grid :deep(.n-input-number) {
  width: 100%;
  min-width: 0;
}

.detail-layout-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  width: 100%;
}

.detail-layout-control,
.detail-layout-switch {
  min-width: 0;
  border: 1px solid #e4e4e7;
  border-radius: 7px;
  background: #fafafa;
}

.detail-layout-control {
  display: grid;
  gap: 5px;
  padding: 8px;
}

.detail-layout-control > span {
  color: #71717a;
  font-size: 11px;
  font-weight: 600;
  line-height: 16px;
}

.detail-layout-control :deep(.n-input-number),
.detail-layout-control :deep(.n-select) {
  width: 100%;
  min-width: 0;
}

.detail-layout-switch {
  grid-column: 1 / -1;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  padding: 8px 10px;
}

.detail-layout-switch strong,
.detail-layout-switch small {
  display: block;
}

.detail-layout-switch strong {
  color: #27272a;
  font-size: 12px;
  line-height: 17px;
}

.detail-layout-switch small {
  margin-top: 1px;
  color: #a1a1aa;
  font-size: 10px;
  line-height: 14px;
}

.search-layout-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  width: 100%;
}

.mini-number-field {
  position: relative;
  display: grid;
  gap: 5px;
  min-width: 0;
}

.mini-number-field > span {
  color: #71717a;
  font-size: 11px;
  font-weight: 600;
  line-height: 16px;
}

.mini-number-field em {
  position: absolute;
  right: 8px;
  bottom: 7px;
  color: #a1a1aa;
  font-size: 10px;
  font-style: normal;
  pointer-events: none;
}

.mini-number-field :deep(.n-input-number) {
  width: 100%;
  min-width: 0;
}

.mini-number-field :deep(.n-input__input-el) {
  padding-right: 18px !important;
}

.position-control {
  display: grid;
  gap: 14px;
}

.position-axis-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.position-number-field,
.position-inline-number {
  position: relative;
  display: grid;
  gap: 6px;
  min-width: 0;
}

.position-number-field > span,
.position-rule-head > span {
  color: #71717a;
  font-size: 11px;
  font-weight: 600;
  line-height: 16px;
}

.position-number-field em,
.position-inline-number em {
  position: absolute;
  right: 8px;
  bottom: 7px;
  color: #a1a1aa;
  font-size: 10px;
  font-style: normal;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  pointer-events: none;
}

.position-rule {
  display: grid;
  gap: 8px;
}

.position-rule-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.position-inline-number {
  width: 76px;
}

.position-inline-number :deep(.n-input__input-el) {
  padding-right: 18px !important;
  text-align: right;
}

.designer-segmented-control {
  display: grid;
  gap: 2px;
  padding: 2px;
  border: 1px solid #dcdfe4;
  border-radius: 7px;
  background: #fff;
}

.width-mode-control {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.align-mode-control {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.height-mode-control {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.designer-segmented-control button {
  display: inline-flex;
  min-width: 0;
  height: 30px;
  align-items: center;
  justify-content: center;
  gap: 4px;
  cursor: pointer;
  border: 0;
  border-radius: 5px;
  background: transparent;
  color: #1f2329;
  font-size: 11px;
  font-weight: 500;
  line-height: 1;
  white-space: nowrap;
}

.designer-segmented-icon {
  flex: 0 0 auto;
  font-size: 14px;
}

.designer-segmented-icon.vertical-icon {
  transform: rotate(90deg);
}

.designer-segmented-control button:hover {
  background: #f2f3f5;
}

.designer-segmented-control button.active {
  background: #edf4ff;
  color: #1456f0;
  font-weight: 600;
}

.segmented-mini {
  min-width: 0;
  display: none;
}

.appearance-control {
  display: grid;
  gap: 14px;
  margin-bottom: 12px;
}

.list-appearance-control {
  padding: 0 0 2px;
}

.appearance-field {
  display: grid;
  gap: 6px;
}

.appearance-field > label,
.appearance-row-label {
  color: #71717a;
  font-size: 11px;
  font-weight: 600;
  line-height: 16px;
}

.appearance-row-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.appearance-input-shell,
.appearance-radius-shell {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 32px;
  border: 1px solid transparent;
  border-radius: 6px;
  background: #f4f4f5;
  padding: 4px;
  transition:
    border-color 160ms ease,
    background-color 160ms ease;
}

.appearance-input-shell:hover,
.appearance-radius-shell:hover {
  background: rgba(228, 228, 231, 0.5);
}

.appearance-input-shell:focus-within,
.appearance-radius-shell:focus-within {
  border-color: #6366f1;
  background: #fff;
  box-shadow: 0 0 0 4px rgba(99, 102, 241, 0.1);
}

.appearance-swatch {
  position: relative;
  flex: 0 0 auto;
  width: 20px;
  height: 20px;
  margin-left: 4px;
  cursor: pointer;
  border: 1px solid rgba(212, 212, 216, 0.72);
  border-radius: 3px;
  background-color: transparent;
  background-image:
    linear-gradient(45deg, #e5e7eb 25%, transparent 25%), linear-gradient(-45deg, #e5e7eb 25%, transparent 25%),
    linear-gradient(45deg, transparent 75%, #e5e7eb 75%), linear-gradient(-45deg, transparent 75%, #e5e7eb 75%);
  background-position:
    0 0,
    0 5px,
    5px -5px,
    -5px 0;
  background-size: 10px 10px;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.08);
  overflow: hidden;
}

.appearance-swatch input {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  cursor: pointer;
  border: 0;
  opacity: 0;
}

.appearance-hex-input,
.appearance-radius-shell input {
  min-width: 0;
  width: 100%;
  border: 0;
  outline: 0;
  background: transparent;
  color: #3f3f46;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  line-height: 18px;
  text-transform: uppercase;
}

.appearance-percent {
  flex: 0 0 auto;
  margin-right: 6px;
  color: #a1a1aa;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 11px;
}

.appearance-select {
  flex: 0 0 auto;
  max-width: 70px;
  border: 0;
  border-right: 1px solid rgba(212, 212, 216, 0.72);
  outline: 0;
  background: transparent;
  color: #3f3f46;
  font-size: 12px;
  cursor: pointer;
}

.appearance-plain-select {
  max-width: 96px;
  border: 0;
  outline: 0;
  background: transparent;
  color: #71717a;
  font-size: 11px;
  cursor: pointer;
}

.appearance-plain-select:hover,
.appearance-select:hover {
  color: #4f46e5;
}

.appearance-radius-shell span {
  flex: 0 0 auto;
  margin-left: 6px;
  color: #a1a1aa;
  font-size: 10px;
  font-weight: 700;
}

.appearance-radius-shell:focus-within span {
  color: #6366f1;
}

.advanced-config-collapse {
  display: grid;
  gap: 6px;
  margin: 2px 0 12px;
}

.advanced-config-collapse :deep(.n-collapse-item) {
  margin: 0;
  overflow: hidden;
  border: 1px solid rgba(228, 228, 231, 0.72);
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
}

.advanced-config-collapse :deep(.n-collapse-item__header) {
  min-height: 35px;
  padding: 0 12px;
  border-bottom: 1px solid transparent;
  background: #fff;
}

.advanced-config-collapse :deep(.n-collapse-item__header-main) {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #3f3f46;
  font-size: 12px;
  font-weight: 650;
}

.advanced-config-collapse :deep(.n-collapse-item__header-main::before) {
  display: none;
}

.advanced-config-collapse :deep(.n-collapse-item--active .n-collapse-item__header) {
  border-bottom-color: #eef2f7;
}

.advanced-config-collapse :deep(.n-collapse-item__content-inner) {
  padding: 2px 10px 10px;
  background: #fff;
}

.advanced-config-collapse :deep(.n-form-item) {
  margin-bottom: 12px;
}

.advanced-config-collapse :deep(.n-form-item:last-child) {
  margin-bottom: 0;
}

.advanced-config-collapse :deep(.n-form-item-label) {
  min-height: 16px;
  padding-bottom: 5px;
  color: #52525b;
  font-size: 11px;
  font-weight: 600;
  line-height: 16px;
}

.copy-form-item :deep(.n-form-item-blank) {
  min-width: 0;
}

.api-base-row {
  display: flex;
  gap: 8px;
  width: 100%;
  min-width: 0;
}

.api-base-input {
  flex: 2 1 0;
  min-width: 0;
}

.api-key-input {
  flex: 1 1 72px;
  min-width: 72px;
}

.api-config-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 6px;
  width: 100%;
}

.api-config-title {
  color: #52525b;
  font-size: 11px;
  font-weight: 600;
  line-height: 16px;
}

.api-endpoint-row {
  display: flex;
  align-items: stretch;
  min-width: 0;
  overflow: hidden;
  border: 1px solid #e4e4e7;
  border-radius: 6px;
  background: #fff;
  transition:
    border-color 160ms ease,
    box-shadow 160ms ease;
}

.api-endpoint-row:focus-within {
  border-color: #6366f1;
  box-shadow: 0 0 0 2px rgba(99, 102, 241, 0.1);
}

.api-method-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  flex: 0 0 48px;
  border-right: 1px solid currentColor;
  font-size: 9px;
  font-weight: 800;
  letter-spacing: 0;
}

.api-method-badge.get {
  background: #eff6ff;
  color: #2563eb;
}

.api-method-badge.post {
  background: #ecfdf5;
  color: #059669;
}

.api-method-badge.put {
  background: #fffbeb;
  color: #d97706;
}

.api-method-badge.delete {
  background: #fff1f2;
  color: #e11d48;
}

.api-config-grid :deep(.n-input),
.api-endpoint-row :deep(.n-input) {
  width: 100%;
}

.api-endpoint-row :deep(.n-input) {
  --n-border: 0 !important;
  --n-border-hover: 0 !important;
  --n-border-focus: 0 !important;
  --n-box-shadow-focus: none !important;
  flex: 1 1 0;
  min-width: 0;
  border-radius: 0;
}

.api-endpoint-row :deep(.n-input__input-el) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 11px !important;
}

.event-help {
  padding: 14px 12px;
  border: 1px dashed #d4d4d8;
  border-radius: 8px;
  background: #fafafa;
  color: #71717a;
  font-size: 12px;
  line-height: 1.6;
  text-align: center;
}

.pos-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.pos-label {
  font-size: 11px;
  color: #64748b;
}

.pos-input {
  width: 100%;
}

.pos-input :deep(.n-input-number) {
  width: 100%;
}

.pos-input :deep(.n-input__input-el) {
  text-align: center;
}

.metrics-editor {
  display: grid;
  gap: 6px;
}

.data-source-editor {
  display: grid;
  gap: 10px;
  min-width: 0;
}

.data-source-row {
  display: grid;
  grid-template-columns: minmax(70px, 86px) minmax(0, 1fr);
  gap: 8px;
  align-items: center;
  min-width: 0;
}

.data-source-row > span {
  overflow: hidden;
  color: #52525b;
  font-size: 12px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.data-source-row :deep(.n-input),
.data-source-row :deep(.n-select),
.data-source-row :deep(.n-input-number) {
  width: 100%;
  min-width: 0;
}

.data-source-mapping-grid {
  display: grid;
  gap: 8px;
  min-width: 0;
}

.data-source-mapping-grid .data-source-row {
  grid-template-columns: minmax(64px, 74px) minmax(0, 1fr);
}

.bitable-field-panel {
  display: grid;
  gap: 4px;
  width: 100%;
  min-width: 0;
  padding: 6px;
  background: #f8f9fa;
}

.bitable-field-panel-head,
.bitable-field-operation-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-width: 0;
}

.bitable-field-panel-head strong,
.bitable-field-operation-head strong {
  color: #18181b;
  font-size: 12px;
  font-weight: 700;
  line-height: 18px;
}

.bitable-field-panel-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 4px;
  min-width: 0;
}

.bitable-field-panel-actions button {
  max-width: 86px;
  min-width: 0;
  overflow: hidden;
  padding: 0;
  border: 0;
  background: transparent;
  color: #2563eb;
  cursor: pointer;
  font-size: 11px;
  line-height: 18px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bitable-field-panel-actions button:hover {
  color: #1d4ed8;
}

.bitable-field-panel-list {
  display: grid;
  gap: 1px;
  max-height: 386px;
  min-width: 0;
  overflow-y: auto;
  padding-right: 2px;
}

.bitable-field-row {
  display: grid;
  grid-template-columns: 20px 18px minmax(0, 1fr) 24px 24px;
  align-items: center;
  gap: 5px;
  min-height: 32px;
  min-width: 0;
  padding: 4px 4px;
  border-radius: 5px;
  background: #f8f9fa;
  color: #27272a;
  transition:
    background-color 120ms ease,
    color 120ms ease;
}

.bitable-field-row:hover {
  background: #eef1f4;
}

.bitable-field-row.invisible {
  visibility: visible !important;
  background: #f8f9fa;
  color: #a1a1aa;
}

.bitable-field-row.invisible .bitable-field-name span,
.bitable-field-row.invisible .bitable-field-icon,
.bitable-field-row.invisible .bitable-field-name small {
  color: #c4c4cc;
}

.bitable-field-row.invisible:hover {
  background: #eef1f4;
}

.bitable-field-drag,
.bitable-field-icon-button {
  display: inline-grid;
  place-items: center;
  width: 22px;
  height: 22px;
  padding: 0;
  border: 0;
  background: transparent;
  color: #71717a;
  cursor: pointer;
  font-size: 16px;
}

.bitable-field-drag {
  cursor: grab;
}

.bitable-field-drag.disabled {
  cursor: default;
  opacity: 0.36;
}

.bitable-field-icon-button:hover,
.bitable-field-drag:hover:not(.disabled) {
  color: #2563eb;
}

.bitable-field-icon {
  color: #71717a;
  font-size: 14px;
}

.bitable-field-name {
  display: grid;
  min-width: 0;
}

.bitable-field-name span {
  overflow: hidden;
  color: inherit;
  font-size: 12px;
  font-weight: 500;
  line-height: 17px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bitable-field-name small {
  overflow: hidden;
  color: #a1a1aa;
  font-size: 10px;
  line-height: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bitable-field-operation-divider {
  height: 1px;
  margin: 6px 0 0;
  background: #e4e4e7;
}

.bitable-field-operation-group {
  display: grid;
  gap: 1px;
}

.bitable-field-operation-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-height: 34px;
  padding: 4px;
  border-radius: 5px;
  color: #52525b;
}

.bitable-field-operation-head:hover {
  background: #f8fafc;
}

.bitable-field-operation-title {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.bitable-field-operation-title strong {
  flex: 0 0 auto;
}

.bitable-field-tip-icon {
  color: #71717a;
  font-size: 14px;
}

.bitable-field-tip-button {
  display: inline-grid;
  place-items: center;
  width: 20px;
  height: 20px;
  padding: 0;
  border: 0;
  background: transparent;
  color: #71717a;
  cursor: pointer;
}

.bitable-field-tip-button:hover {
  color: #2563eb;
}

:global(.bitable-field-info-popover) {
  max-width: 220px;
  padding: 8px 10px;
  border-radius: 6px;
  color: #3f3f46;
  font-size: 12px;
  line-height: 18px;
}

.bitable-field-info-content {
  color: #3f3f46;
}

.bitable-field-add-button {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  height: 22px;
  min-width: 0;
  padding: 0 6px;
  border: 1px solid #e4e4e7;
  border-radius: 4px;
  background: #f8fafc;
  color: #52525b;
  cursor: pointer;
  font-size: 12px;
  line-height: 20px;
  transition:
    border-color 120ms ease,
    color 120ms ease,
    background-color 120ms ease;
}

.bitable-field-add-button:hover {
  border-color: #93c5fd;
  background: #eff6ff;
  color: #2563eb;
}

.bitable-field-add-button .n-icon {
  flex: 0 0 auto;
  font-size: 13px;
}

.bitable-operation-list {
  display: grid;
  gap: 1px;
}

.bitable-action-row {
  grid-template-columns: 20px 18px minmax(0, 1fr) 24px 24px;
  border-color: transparent;
  background: #f8f9fa;
  padding: 4px;
}

.bitable-action-row:hover {
  background: #eef1f4;
}

.custom-action-builder {
  display: grid;
  gap: 10px;
  width: 100%;
}

.custom-action-builder-section {
  display: grid;
  gap: 8px;
  min-width: 0;
  padding: 8px;
  border: 1px solid #e4e4e7;
  border-radius: 6px;
  background: #fff;
}

.custom-action-builder-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-width: 0;
}

.custom-action-builder-head > div {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.custom-action-builder-head strong {
  color: #18181b;
  font-size: 11px;
  font-weight: 700;
  line-height: 16px;
}

.custom-action-builder-head span {
  overflow: hidden;
  color: #71717a;
  font-size: 10px;
  line-height: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.custom-action-card-list {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.custom-action-card-row {
  display: grid;
  grid-template-columns: 22px minmax(0, 1fr) 24px;
  align-items: center;
  gap: 6px;
  min-width: 0;
  padding: 6px;
  border: 1px solid #e4e4e7;
  border-radius: 6px;
  background: #fafafa;
}

.custom-action-drag,
.custom-action-more,
.custom-action-inline-icon {
  display: inline-grid;
  place-items: center;
  width: 22px;
  height: 22px;
  padding: 0;
  border: 0;
  background: transparent;
  color: #71717a;
  cursor: pointer;
}

.custom-action-drag {
  cursor: grab;
}

.custom-action-drag:active {
  cursor: grabbing;
}

.custom-action-drag:hover,
.custom-action-more:hover,
.custom-action-inline-icon:hover {
  color: #2563eb;
}

.custom-action-name-input {
  min-width: 0;
}

.custom-action-empty {
  padding: 4px 2px 2px;
  color: #94a3b8;
  font-size: 11px;
  line-height: 16px;
}

.custom-action-modal {
  display: flex;
  flex-direction: column;
  width: min(1160px, calc(100vw - 40px));
  height: min(86vh, 920px);
  max-height: min(86vh, 920px);
}

.custom-action-modal :deep(.n-card-header),
.custom-action-modal :deep(.n-card__footer) {
  flex: 0 0 auto;
}

.custom-action-modal :deep(.n-card-content) {
  display: grid;
  flex: 1 1 0;
  min-height: 0;
  overflow: hidden;
}

.action-modal-layout {
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr);
  gap: 16px;
  min-height: 0;
  height: 100%;
}

.action-modal-list {
  display: grid;
  min-height: 0;
  overflow-y: auto;
  align-content: start;
  gap: 8px;
  padding-right: 12px;
  border-right: 1px solid #e5e7eb;
}

.action-list-item,
.action-add-item {
  display: grid;
  gap: 2px;
  min-height: 48px;
  padding: 8px 10px;
  border: 1px solid #dbe3ee;
  border-radius: 6px;
  background: #fff;
  text-align: left;
  cursor: pointer;
}

.action-list-item.active {
  border-color: #2563eb;
  background: #eff6ff;
}

.action-list-item span {
  color: #0f172a;
  font-size: 13px;
  font-weight: 700;
}

.action-list-item small {
  color: #64748b;
  font-size: 11px;
}

.action-add-item {
  place-items: center;
  border-style: dashed;
  color: #2563eb;
  font-size: 12px;
}

.action-editor-panel {
  min-width: 0;
  min-height: 0;
  overflow-y: auto;
  padding-right: 4px;
}

.action-editor-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.action-editor-title {
  color: #0f172a;
  font-size: 15px;
  font-weight: 700;
}

.action-editor-desc {
  margin-top: 3px;
  color: #64748b;
  font-size: 12px;
}

.action-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.action-editor-form {
  display: grid;
  gap: 16px;
}

.action-form-section {
  display: grid;
  gap: 10px;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fcfdff;
}

.action-section-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}

.action-section-head strong {
  color: #0f172a;
  font-size: 13px;
  font-weight: 700;
}

.action-section-head span {
  color: #64748b;
  font-size: 11px;
}

.action-form-section .action-form-grid {
  gap: 10px;
}

.action-form-section--compact .action-form-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.action-form-section :deep(.n-form-item) {
  margin-bottom: 0;
}

.action-form-section :deep(.n-form-item .n-form-item-label) {
  padding-bottom: 5px;
}

.action-route-panel {
  display: grid;
  width: 100%;
  gap: 12px;
}

.action-config-tip {
  display: grid;
  gap: 3px;
  border: 1px solid #dbeafe;
  border-radius: 6px;
  background: #eff6ff;
  color: #334155;
  padding: 8px 10px;
  font-size: 12px;
  line-height: 1.5;
}

.action-config-tip strong {
  color: #1d4ed8;
  font-size: 13px;
}

.action-field {
  display: grid;
  gap: 5px;
  min-width: 0;
}

.action-field-label {
  color: #334155;
  font-size: 12px;
  font-weight: 600;
}

.action-field small {
  color: #64748b;
  font-size: 12px;
  line-height: 1.45;
}

.action-button-event-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  width: 100%;
}

.action-param-editor {
  display: grid;
  width: 100%;
  gap: 8px;
}

.action-param-row {
  display: grid;
  grid-template-columns: minmax(92px, 0.8fr) minmax(92px, 0.8fr) minmax(130px, 1.2fr) auto;
  gap: 8px;
  align-items: center;
}

.api-action-panel {
  display: grid;
  gap: 12px;
  width: 100%;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fbfcfe;
  padding: 12px;
}

.api-param-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: #334155;
  font-size: 13px;
  font-weight: 600;
}

.api-param-list {
  display: grid;
  gap: 8px;
}

.api-param-columns {
  display: grid;
  grid-template-columns: minmax(92px, 0.8fr) 96px 112px minmax(140px, 1.1fr) auto;
  gap: 8px;
  align-items: center;
  color: #64748b;
  font-size: 12px;
}

.api-param-row {
  display: grid;
  grid-template-columns: minmax(92px, 0.8fr) 96px 112px minmax(140px, 1.1fr) auto;
  gap: 8px;
  align-items: center;
}

.action-empty-panel {
  display: grid;
  place-items: center;
  border: 1px dashed #cbd5e1;
  border-radius: 6px;
  color: #94a3b8;
  font-size: 13px;
}

.metric-row {
  display: grid;
  grid-template-columns: 1fr 1fr 80px auto;
  gap: 4px;
  align-items: center;
}

/* Field drawer */
.field-config {
  display: grid;
  gap: 14px;
  min-width: 0;
}

.field-config-section .section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  font-weight: 700;
  color: #334155;
  margin-bottom: 8px;
}

.selected-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  max-height: 146px;
  overflow: auto;
  padding-right: 4px;
}

.selected-row {
  display: inline-grid;
  grid-template-columns: 12px minmax(0, auto) auto;
  align-items: center;
  gap: 5px;
  min-width: 0;
  max-width: 210px;
  padding: 5px 5px 5px 8px;
  border: 1px solid #e4e4e7;
  border-radius: 6px;
  background: #fff;
  color: #3f3f46;
  font-size: 11px;
  cursor: pointer;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.03);
  transition:
    border-color 160ms ease,
    box-shadow 160ms ease,
    background-color 160ms ease;
}

.selected-row.search,
.selected-row.table {
  grid-template-columns: 12px minmax(0, auto) auto;
}

.selected-row:hover {
  border-color: #a5b4fc;
}

.selected-row.active {
  border-color: #6366f1;
  background: #eef2ff;
  color: #4338ca;
  box-shadow: 0 0 0 1px rgba(99, 102, 241, 0.18);
}

.field-setting-row {
  display: none;
  grid-column: 1 / -1;
  gap: 8px;
  min-width: 0;
  margin-left: 26px;
  padding-top: 8px;
  border-top: 1px dashed #e2e8f0;
}

.search-setting-row {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.table-setting-row {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.column-link-row {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.field-detail-card {
  position: relative;
  display: grid;
  gap: 12px;
  margin-top: 12px;
  padding: 12px;
  border: 1px solid rgba(228, 228, 231, 0.8);
  border-radius: 8px;
  background: rgba(250, 250, 250, 0.82);
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.03);
}

.field-detail-card::before {
  content: '';
  position: absolute;
  top: -6px;
  left: 18px;
  width: 10px;
  height: 10px;
  transform: rotate(45deg);
  border-top: 1px solid rgba(228, 228, 231, 0.8);
  border-left: 1px solid rgba(228, 228, 231, 0.8);
  background: rgba(250, 250, 250, 0.82);
}

.field-detail-head {
  position: relative;
  z-index: 1;
  display: grid;
  gap: 10px;
  padding-bottom: 10px;
  border-bottom: 1px solid rgba(228, 228, 231, 0.72);
}

.field-detail-title {
  display: flex;
  align-items: baseline;
  gap: 7px;
  min-width: 0;
}

.field-detail-title strong {
  min-width: 0;
  overflow: hidden;
  color: #18181b;
  font-size: 13px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.field-detail-title span {
  min-width: 0;
  overflow: hidden;
  color: #a1a1aa;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.field-role-switches,
.field-detail-toggles {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
}

.field-role-switches label,
.field-detail-toggles label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #52525b;
  font-size: 11px;
  cursor: pointer;
}

.field-detail-grid {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 12px;
}

.field-detail-control {
  display: grid;
  gap: 5px;
  min-width: 0;
}

.field-detail-control > span {
  color: #52525b;
  font-size: 10px;
  font-weight: 600;
}

.field-detail-footer {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding-top: 10px;
  border-top: 1px solid rgba(228, 228, 231, 0.72);
}

.field-advanced-panel {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 12px;
  padding-top: 10px;
  border-top: 1px dashed rgba(212, 212, 216, 0.9);
}

.field-help {
  grid-column: 1 / -1;
  padding: 7px 9px;
  border: 1px dashed #d4d4d8;
  border-radius: 6px;
  background: #fff;
  color: #71717a;
  font-size: 11px;
  line-height: 1.55;
}

.preview-config-panel {
  width: 100%;
  display: grid;
  gap: 8px;
  padding: 10px;
  border: 1px solid rgba(228, 228, 231, 0.72);
  border-radius: 8px;
  background: #fafafa;
}

.preview-config-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-width: 0;
  color: #3f3f46;
  font-size: 11px;
  font-weight: 700;
  line-height: 16px;
}

.preview-config-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  width: 100%;
}

.preview-config-grid :deep(.n-select),
.preview-config-grid :deep(.n-input) {
  min-width: 0;
  width: 100%;
}

.field-config-entry,
.main-flow-action-hint {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
  border: 1px solid #e4e4e7;
  border-radius: 6px;
  background: #fafafa;
  padding: 10px;
}

.field-config-card-list {
  display: grid;
  gap: 8px;
  width: 100%;
}

.field-config-card-list.compact {
  gap: 6px;
}

.field-config-entry.compact {
  gap: 8px;
  padding: 7px 8px;
  border-color: rgba(228, 228, 231, 0.76);
  background: #fff;
}

.field-config-entry.compact strong {
  font-size: 11px;
  line-height: 16px;
}

.field-config-entry.compact span {
  overflow: hidden;
  margin-top: 1px;
  color: #71717a;
  font-size: 10px;
  line-height: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.field-config-entry.compact :deep(.n-button) {
  flex: 0 0 auto;
  min-width: 58px;
}

.field-config-icon-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 26px;
  height: 26px;
  cursor: pointer;
  border: 1px solid #e4e4e7;
  border-radius: 6px;
  background: #fff;
  color: #71717a;
  padding: 0;
  transition:
    border-color 160ms ease,
    background-color 160ms ease,
    color 160ms ease;
}

.field-config-icon-action:hover {
  border-color: #c7d2fe;
  background: #eef2ff;
  color: #4f46e5;
}

.field-config-entry > div,
.main-flow-action-hint {
  min-width: 0;
}

.field-config-entry strong,
.main-flow-action-hint strong {
  display: block;
  color: #0f172a;
  font-size: 12px;
  line-height: 18px;
}

.field-config-entry span,
.main-flow-action-hint span {
  display: block;
  margin-top: 2px;
  color: #64748b;
  font-size: 11px;
  line-height: 16px;
}

.main-flow-action-hint {
  display: grid;
  justify-content: stretch;
  border-color: #bfdbfe;
  background: #eff6ff;
}

.preview-status {
  display: grid;
  gap: 3px;
  padding: 8px 10px;
  border: 1px solid #e4e4e7;
  border-radius: 6px;
  background: #fff;
  color: #71717a;
  font-size: 11px;
  line-height: 1.45;
}

.preview-status strong {
  color: #0f172a;
  font-size: 12px;
}

.preview-status em {
  font-style: normal;
  color: #dc2626;
  word-break: break-all;
}

.preview-status.is-loading {
  border-color: #bfdbfe;
  background: #eff6ff;
}

.preview-status.is-success {
  border-color: #bbf7d0;
  background: #f0fdf4;
}

.preview-status.is-error {
  border-color: #fecaca;
  background: #fef2f2;
}

.switch-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
  min-width: 0;
  padding: 9px 10px;
  border: 1px solid #e4e4e7;
  border-radius: 6px;
  background: #fafafa;
  color: #3f3f46;
  font-size: 12px;
  font-weight: 600;
}

.tree-action-compact {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  width: 100%;
  min-width: 0;
  padding: 7px 8px;
  border: 1px solid rgba(228, 228, 231, 0.76);
  border-radius: 6px;
  background: #fff;
}

.tree-action-compact > div {
  display: grid;
  gap: 1px;
  min-width: 0;
}

.tree-action-compact strong {
  color: #3f3f46;
  font-size: 11px;
  font-weight: 700;
  line-height: 16px;
}

.tree-action-compact span {
  color: #71717a;
  font-size: 10px;
  line-height: 14px;
}

.toolbar-toggle-list {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 6px;
  width: 100%;
}

.toolbar-toggle-row {
  align-items: flex-start;
  background: #fff;
}

.switch-line-text {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.switch-line-text strong {
  color: #0f172a;
  font-size: 12px;
  line-height: 18px;
}

.switch-line-text small {
  color: #64748b;
  font-size: 11px;
  font-weight: 400;
  line-height: 16px;
}

.field-setting-control {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.field-setting-control > span {
  color: #475569;
  font-size: 11px;
  font-weight: 700;
  line-height: 16px;
}

.field-setting-control :deep(.n-select),
.field-setting-control :deep(.n-input),
.field-setting-control :deep(.n-color-picker) {
  width: 100%;
  min-width: 0;
}

.field-setting-row :deep(.n-select),
.field-setting-row :deep(.n-input),
.field-setting-row :deep(.n-input-number),
.field-setting-row :deep(.n-color-picker) {
  width: 100%;
  min-width: 0;
}

.f-handle {
  color: #d4d4d8;
  font-size: 11px;
  line-height: 1;
  cursor: grab;
}

.f-name {
  display: block;
  min-width: 0;
  overflow: hidden;
  color: inherit;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.f-name small {
  display: none;
}

.available-item small {
  color: #64748b;
  font-size: 11px;
  font-weight: 400;
}

.f-code {
  display: none;
}

.f-remove {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: #d4d4d8;
  font-size: 14px;
  line-height: 1;
  cursor: pointer;
  opacity: 0;
  transition:
    opacity 160ms ease,
    color 160ms ease,
    background-color 160ms ease;
}

.selected-row:hover .f-remove {
  opacity: 1;
}

.f-remove:hover {
  background: #fef2f2;
  color: #ef4444;
}

.available-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  max-height: 240px;
  overflow: auto;
  padding-right: 4px;
}

.available-item {
  display: inline-grid;
  gap: 2px;
  padding: 4px 10px;
  border: 1px dashed #cbd5e1;
  border-radius: 6px;
  background: #f8fafc;
  color: #2563eb;
  font-size: 12px;
  cursor: pointer;
}

.available-item:hover {
  border-color: #2563eb;
  background: #eff6ff;
}

.list-source-modal {
  width: min(980px, calc(100vw - 56px));
}

.list-source-modal :deep(.n-card) {
  background: #09090b;
  color: #f8fafc;
}

.list-source-modal :deep(.n-card-header),
.list-source-modal :deep(.n-card__footer) {
  border-color: #1f2937;
}

.list-source-modal :deep(.n-card-header__main) {
  color: #f8fafc;
}

.list-source-modal :deep(.n-tabs-tab__label) {
  color: #cbd5e1;
}

.list-source-modal :deep(.n-tabs-tab.n-tabs-tab--active .n-tabs-tab__label) {
  color: #93c5fd;
}

.list-source-modal :deep(.n-tabs-nav-scroll-content) {
  border-color: #1f2937;
}

.source-editor-hint {
  margin-bottom: 10px;
  border: 1px solid rgba(59, 130, 246, 0.28);
  border-radius: 6px;
  background: rgba(37, 99, 235, 0.12);
  color: #bfdbfe;
  font-size: 12px;
  line-height: 18px;
  padding: 7px 10px;
}

.source-code-textarea :deep(.n-input) {
  --n-border: 1px solid #1f2937 !important;
  --n-border-hover: 1px solid #334155 !important;
  --n-border-focus: 1px solid #3b82f6 !important;
  --n-box-shadow-focus: 0 0 0 2px rgba(59, 130, 246, 0.18) !important;
  --n-color: #050816 !important;
  --n-color-focus: #050816 !important;
  --n-color-hover: #050816 !important;
  --n-text-color: #f8fafc !important;
  --n-placeholder-color: #64748b !important;
  background: #050816;
}

.source-code-textarea :deep(.n-input__textarea-el) {
  background: #050816;
  caret-color: #f8fafc;
  color: #f8fafc;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  line-height: 1.6;
}

.source-code-textarea :deep(.n-input__textarea-el::selection) {
  background: rgba(37, 99, 235, 0.48);
}

.source-error {
  margin-top: 10px;
  border: 1px solid #7f1d1d;
  border-radius: 6px;
  background: rgba(127, 29, 29, 0.28);
  color: #fecaca;
  font-size: 12px;
  line-height: 18px;
  padding: 8px 10px;
}

.source-modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.empty {
  font-size: 12px;
  color: #94a3b8;
}

@media (max-width: 980px) {
  .list-grid-designer,
  .list-grid-designer.left-collapsed,
  .list-grid-designer.right-collapsed,
  .list-grid-designer.left-collapsed.right-collapsed {
    grid-template-columns: 1fr;
    min-height: auto;
  }

  .palette-groups {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    overflow-y: visible;
  }

  .palette-list {
    grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  }
}

@media (max-width: 900px) {
  .palette-groups {
    grid-template-columns: 1fr;
  }

  .canvas-toolbar {
    align-items: center;
    flex-direction: row;
    flex-wrap: wrap;
  }

  .action-editor-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .canvas-primary-actions,
  .canvas-toolbar :deep(.n-space) {
    flex-wrap: wrap;
  }

  .toolbar-info span {
    display: none;
  }

  .canvas-scroll {
    padding: 0 8px 8px;
  }

  .canvas-zoom-stage {
    padding: 54px 1px 18px;
  }

  .canvas-viewport-dock {
    top: 54px;
    left: 50%;
  }

  .action-modal-layout,
  .action-form-grid,
  .action-section-head,
  .action-param-row,
  .api-param-columns,
  .api-param-row {
    grid-template-columns: 1fr;
  }

  .action-section-head {
    align-items: flex-start;
    flex-direction: column;
  }
}
.crud-expand-config-panel {
  display: grid;
  width: 100%;
  gap: 12px;
  min-width: 0;
}

.crud-expand-config-head {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #52525b;
  font-size: 12px;
  line-height: 20px;
}

.crud-expand-config-section {
  display: grid;
  gap: 8px;
  min-width: 0;
  border: 1px solid #e4e4e7;
  border-radius: 8px;
  background: #fafafa;
  padding: 10px;
}

.crud-expand-section-title {
  color: #3f3f46;
  font-size: 12px;
  font-weight: 700;
  line-height: 18px;
}

.crud-expand-config-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.crud-expand-config-field {
  display: grid;
  gap: 5px;
  min-width: 0;
}

.crud-expand-config-field > span {
  color: #71717a;
  font-size: 12px;
  font-weight: 600;
  line-height: 18px;
}

.crud-expand-config-section :deep(.n-input),
.crud-expand-config-section :deep(.n-select) {
  width: 100%;
}

.bitable-config-summary-row {
  display: grid;
  grid-template-columns: minmax(72px, 86px) minmax(0, 1fr);
  align-items: center;
  min-height: 32px;
  min-width: 0;
  border-radius: 5px;
}

.bitable-config-summary-label {
  overflow: hidden;
  color: #3f3f46;
  font-size: 12px;
  font-weight: 500;
  line-height: 18px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bitable-config-summary-value {
  display: block;
  min-width: 0;
}

.bitable-config-value-box {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  min-height: 32px;
  min-width: 0;
  overflow: hidden;
  border: 1px solid transparent;
  border-radius: 5px;
  background: #f4f5f7;
  transition:
    background-color 120ms ease,
    border-color 120ms ease;
}

.bitable-config-value-box:hover {
  background: #eef1f4;
  border-color: #e4e7ec;
}

.bitable-config-value-box-text {
  flex: 1 1 0;
  min-width: 0;
  overflow: hidden;
  padding: 0 8px;
  color: #52525b;
  font-size: 12px;
  line-height: 30px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bitable-config-value-box-btn {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
}

.bitable-config-icon-button {
  display: inline-grid;
  place-items: center;
  width: 24px;
  height: 24px;
  padding: 0;
  border: 0;
  border-radius: 5px;
  background: transparent;
  color: #71717a;
  cursor: pointer;
  font-size: 15px;
}

.bitable-config-icon-button:hover {
  background: #e4e7ec;
  color: #2563eb;
}

.bitable-field-popover-panel {
  position: relative;
  display: grid;
  gap: 0;
  width: 310px;
  max-width: calc(100vw - 48px);
  padding: 0;
  border: 1px solid #e4e4e7;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.14);
}

.bitable-field-panel-arrow {
  position: absolute;
  top: -6px;
  right: 16px;
  width: 10px;
  height: 10px;
  border-top: 1px solid #e4e4e7;
  border-left: 1px solid #e4e4e7;
  background: #fff;
  transform: rotate(45deg);
}

.bitable-field-popover-head {
  height: 40px;
  padding: 10px 12px 8px;
  border-bottom: 1px solid #f1f5f9;
  color: #18181b;
  font-size: 13px;
  font-weight: 700;
  line-height: 20px;
}

.bitable-field-popover-panel .bitable-field-panel-list {
  max-height: 386px;
  padding: 6px;
}

.crud-expand-field-list {
  display: grid;
  gap: 6px;
  max-height: 240px;
  overflow-y: auto;
  padding-right: 2px;
}

@media (max-width: 1280px) {
  .crud-expand-config-grid {
    grid-template-columns: 1fr;
  }
}
</style>
