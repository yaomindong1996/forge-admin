<template>
  <div
    class="list-grid-designer"
    :class="{ 'left-collapsed': paletteCollapsed, 'right-collapsed': propertyCollapsed, readonly }"
  >
    <button
      v-if="paletteCollapsed && !readonly"
      type="button"
      class="side-rail-toggle-button left"
      title="展开页面组件"
      @click="paletteCollapsed = false"
    >
      <n-icon><ChevronForwardOutline /></n-icon>
    </button>

    <aside v-else-if="!readonly" class="palette-panel">
      <div class="palette-panel-head">
        <div>
          <div class="panel-title">
            页面组件
          </div>
          <div class="panel-desc">
            拖拽组件到画布，右侧可调整样式
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
            {{ group.title }}
          </div>
          <div class="palette-list">
            <button
              v-for="item in group.items"
              :key="item.blockType"
              class="palette-item"
              type="button"
              draggable="true"
              :disabled="isBlockDisabled(item)"
              :title="isBlockDisabled(item) ? '该区块已存在或不适用当前布局' : item.desc"
              @dragstart="handlePaletteDragStart($event, item)"
              @dragend="resetCanvasDragState"
              @click="handlePaletteClick(item)"
            >
              <span class="item-title">{{ item.title }}</span>
              <span class="item-desc">{{ item.desc }}</span>
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
          <span>{{ blocks.length }} 个区块 · {{ totalRows }} 行 × 12 列 · {{ canvasGridWidth }}px</span>
        </div>
        <n-space v-if="!readonly" size="small" align="center">
          <div class="canvas-width-control">
            <span>设计宽度</span>
            <n-select
              :value="designCanvasWidth"
              :options="canvasWidthOptions"
              size="small"
              class="canvas-width-select"
              @update:value="updateDesignWidth"
            />
            <n-input-number
              :value="designCanvasWidth"
              :min="960"
              :max="2560"
              :step="10"
              size="small"
              class="canvas-width-input"
              :show-button="false"
              @update:value="updateDesignWidth"
            />
          </div>
          <div class="canvas-zoom-control">
            <span>缩放</span>
            <n-button size="tiny" secondary @click="updateCanvasZoom(canvasZoom - 0.1)">
              -
            </n-button>
            <n-select
              :value="canvasZoom"
              :options="canvasZoomOptions"
              size="small"
              class="canvas-zoom-select"
              @update:value="updateCanvasZoom"
            />
            <n-button size="tiny" secondary @click="updateCanvasZoom(canvasZoom + 0.1)">
              +
            </n-button>
          </div>
          <n-button size="small" secondary @click="sourceModalOpen = true">
            源码
          </n-button>
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

      <div ref="canvasScrollRef" class="canvas-scroll">
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
                :readonly="readonly"
                :runtime-crud-props="resolvedRuntimeCrudProps"
                :runtime-record="runtimeRecord"
                :runtime-tree-active-key="runtimeTreeActiveKey"
                @runtime-tree-select="handleRuntimeTreeSelect"
                @tree-panel-collapse-change="handleTreePanelCollapseChange"
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
      </div>
    </main>

    <button
      v-if="propertyCollapsed && !readonly"
      type="button"
      class="side-rail-toggle-button right"
      title="展开配置区块"
      @click="propertyCollapsed = false"
    >
      <n-icon><ChevronBackOutline /></n-icon>
    </button>

    <aside v-else-if="!readonly" class="block-property-panel">
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
      <div class="property-panel">
        <div v-if="!selectedBlock" class="property-empty">
          <p>选中画布上的组件以编辑属性</p>
        </div>
        <div v-else class="property-body">
          <div class="prop-head">
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
            <n-form-item label="区块标题">
              <n-input
                :value="selectedBlock.label"
                @update:value="patchBlock(selectedBlock.id, { label: $event })"
              />
            </n-form-item>

            <n-form-item label="位置与尺寸 px">
              <div class="grid-pos-editor">
                <div class="pos-cell">
                  <span class="pos-label">左 X</span>
                  <n-input-number
                    :value="selectedBlockFrame.x"
                    :min="0"
                    size="small"
                    class="pos-input"
                    @update:value="patchBlockFrame(selectedBlock.id, { x: $event ?? 0 })"
                  />
                </div>
                <div class="pos-cell">
                  <span class="pos-label">上 Y</span>
                  <n-input-number
                    :value="selectedBlockFrame.y"
                    :min="0"
                    size="small"
                    class="pos-input"
                    @update:value="patchBlockFrame(selectedBlock.id, { y: $event ?? 0 })"
                  />
                </div>
                <div class="pos-cell">
                  <span class="pos-label">宽 W</span>
                  <n-select
                    :value="selectedBlockWidthMode"
                    :options="blockWidthModeOptions"
                    size="small"
                    class="pos-input"
                    @update:value="setBlockWidthMode(selectedBlock.id, $event)"
                  />
                </div>
                <div class="pos-cell">
                  <span class="pos-label">固定宽</span>
                  <n-input-number
                    :value="selectedBlockFixedWidth"
                    :min="24"
                    size="small"
                    class="pos-input"
                    :disabled="selectedBlockWidthMode !== 'fixed'"
                    @update:value="patchBlockFrame(selectedBlock.id, { width: $event ?? 24 })"
                  />
                </div>
                <div class="pos-cell">
                  <span class="pos-label">高 H</span>
                  <n-input-number
                    :value="selectedBlockFrame.height"
                    :min="24"
                    size="small"
                    class="pos-input"
                    @update:value="patchBlockFrame(selectedBlock.id, { height: $event ?? 24 })"
                  />
                </div>
              </div>
            </n-form-item>

            <n-divider>外观样式</n-divider>
            <n-form-item label="运行时尺寸">
              <div class="style-grid">
                <n-select
                  :value="selectedBlockWidthMode"
                  :options="blockWidthModeOptions"
                  size="small"
                  @update:value="setBlockWidthMode(selectedBlock.id, $event)"
                />
                <n-input-number
                  :value="toNumberOrNull(selectedBlockStyle.height)"
                  size="small"
                  :min="1"
                  :show-button="false"
                  placeholder="高 px"
                  clearable
                  @update:value="patchBlockStyle(selectedBlock.id, { height: $event ?? '' })"
                />
              </div>
            </n-form-item>
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
            <n-form-item label="背景 / 边框颜色">
              <div class="style-grid">
                <n-color-picker
                  :value="selectedBlockStyle.backgroundColor"
                  size="small"
                  :show-alpha="true"
                  @update:value="patchBlockStyle(selectedBlock.id, { backgroundColor: $event || 'transparent' })"
                />
                <n-color-picker
                  :value="selectedBlockStyle.borderColor"
                  size="small"
                  :show-alpha="true"
                  @update:value="patchBlockStyle(selectedBlock.id, { borderColor: $event || 'transparent' })"
                />
              </div>
            </n-form-item>
            <n-form-item label="边框 / 圆角">
              <div class="style-grid three">
                <n-input-number
                  :value="Number(selectedBlockStyle.borderWidth)"
                  :min="0"
                  :max="16"
                  size="small"
                  :show-button="false"
                  placeholder="边框"
                  @update:value="patchBlockStyle(selectedBlock.id, { borderWidth: $event ?? 0 })"
                />
                <n-select
                  :value="selectedBlockStyle.borderStyle"
                  :options="borderStyleOptions"
                  size="small"
                  @update:value="patchBlockStyle(selectedBlock.id, { borderStyle: $event || 'solid' })"
                />
                <n-input-number
                  :value="Number(selectedBlockStyle.borderRadius)"
                  :min="0"
                  :max="48"
                  size="small"
                  :show-button="false"
                  placeholder="圆角"
                  @update:value="patchBlockStyle(selectedBlock.id, { borderRadius: $event ?? 0 })"
                />
              </div>
            </n-form-item>
            <n-form-item label="阴影">
              <div class="style-grid">
                <n-select
                  :value="selectedBlockStyle.boxShadow"
                  :options="shadowOptions"
                  size="small"
                  @update:value="patchBlockStyle(selectedBlock.id, { boxShadow: $event || 'none' })"
                />
              </div>
            </n-form-item>
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

            <n-divider>事件配置</n-divider>
            <div class="event-editor">
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
                  <n-select
                    :value="eventItem.trigger"
                    :options="eventTriggerOptions"
                    size="small"
                    placeholder="触发时机"
                    @update:value="updateBlockEvent(eventIdx, { trigger: $event })"
                  />
                  <n-select
                    :value="eventItem.action"
                    :options="blockEventActionOptions"
                    size="small"
                    placeholder="执行动作"
                    @update:value="updateBlockEvent(eventIdx, { action: $event })"
                  />
                  <n-select
                    :value="eventItem.targetBlockId"
                    :options="blockTargetOptions"
                    size="small"
                    clearable
                    placeholder="目标组件"
                    @update:value="updateBlockEvent(eventIdx, { targetBlockId: $event || '' })"
                  />
                  <n-select
                    :value="eventItem.targetPageKey"
                    :options="pageTargetOptions"
                    size="small"
                    clearable
                    placeholder="目标页面"
                    @update:value="updateBlockEvent(eventIdx, { targetPageKey: $event || '' })"
                  />
                  <n-input
                    :value="eventItem.description"
                    size="small"
                    placeholder="说明"
                    @update:value="updateBlockEvent(eventIdx, { description: $event })"
                  />
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
                    <n-input
                      :value="param.value"
                      size="tiny"
                      placeholder="参数值，如 :id / ${field}"
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

            <!-- Search / Table 字段配置 -->
            <template v-if="isFieldConfigurableBlock(selectedBlock.blockType)">
              <n-form-item :label="resolveFieldConfigLabel(selectedBlock.blockType)">
                <n-button size="small" type="primary" secondary @click="fieldDrawerOpen = true">
                  配置字段（{{ selectedBlock.fieldRefs?.length || 0 }}/{{ fields.length }}）
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
              <n-collapse class="advanced-config-collapse" :default-expanded-names="['base']">
                <n-collapse-item v-if="propertySectionVisible(['基础配置', '接口', '基础路径', '行主键', '真实接口预览', '响应字段', '表单布局'])" name="base" title="基础配置">
                  <template v-if="selectedBlock.blockType === 'AiCrudPage'">
                    <n-form-item label="基础路径 / 行主键">
                      <div class="style-grid">
                        <n-input
                          :value="selectedBlock.props?.api"
                          placeholder="/api/business/object"
                          @update:value="patchBlockProps(selectedBlock.id, { api: $event })"
                        />
                        <n-input
                          :value="selectedBlock.props?.rowKey || 'id'"
                          placeholder="id"
                          @update:value="patchBlockProps(selectedBlock.id, { rowKey: $event || 'id' })"
                        />
                      </div>
                    </n-form-item>
                    <n-form-item label="真实接口预览">
                      <n-switch
                        :value="selectedBlock.props?.previewLiveData === true"
                        @update:value="patchBlockProps(selectedBlock.id, { previewLiveData: $event })"
                      />
                      <div class="field-help">
                        默认不请求接口，避免设计器误触发真实数据；打开后中间预览会按接口配置加载数据。
                      </div>
                    </n-form-item>
                    <n-form-item label="接口地址">
                      <div class="api-config-grid">
                        <n-input
                          :value="selectedBlock.props?.listApi"
                          placeholder="列表 GET@/api/page"
                          @update:value="patchBlockProps(selectedBlock.id, { listApi: $event })"
                        />
                        <n-input
                          :value="selectedBlock.props?.detailApi"
                          placeholder="详情 GET@/api/:id"
                          @update:value="patchBlockProps(selectedBlock.id, { detailApi: $event })"
                        />
                        <n-input
                          :value="selectedBlock.props?.createApi"
                          placeholder="新增 POST@/api"
                          @update:value="patchBlockProps(selectedBlock.id, { createApi: $event })"
                        />
                        <n-input
                          :value="selectedBlock.props?.updateApi"
                          placeholder="编辑 PUT@/api/:id"
                          @update:value="patchBlockProps(selectedBlock.id, { updateApi: $event })"
                        />
                        <n-input
                          :value="selectedBlock.props?.deleteApi"
                          placeholder="删除 DELETE@/api/:id"
                          @update:value="patchBlockProps(selectedBlock.id, { deleteApi: $event })"
                        />
                        <n-input
                          :value="selectedBlock.props?.importApi"
                          placeholder="导入 POST@/api/import"
                          @update:value="patchBlockProps(selectedBlock.id, { importApi: $event })"
                        />
                        <n-input
                          :value="selectedBlock.props?.exportApi"
                          placeholder="导出 GET@/api/export"
                          @update:value="patchBlockProps(selectedBlock.id, { exportApi: $event })"
                        />
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

                <n-collapse-item v-if="selectedBlock.blockType === 'AiCrudPage' && propertySectionVisible(['搜索与表格', '搜索', '查询区', '表格', '分页', '列数', '最大高度', '横向宽度', '边框', '斑马纹'])" name="search" title="搜索与表格">
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
                    <div class="style-grid four">
                      <n-input-number
                        :value="selectedBlock.props?.searchGridCols || 4"
                        :min="1"
                        :max="6"
                        :show-button="false"
                        placeholder="列数"
                        @update:value="patchBlockProps(selectedBlock.id, { searchGridCols: $event || 4 })"
                      />
                      <n-input-number
                        :value="toNumberOrNull(selectedBlock.props?.searchLabelWidth)"
                        :min="0"
                        :max="260"
                        :show-button="false"
                        placeholder="标签宽"
                        @update:value="patchBlockProps(selectedBlock.id, { searchLabelWidth: $event ?? 'auto' })"
                      />
                      <n-input-number
                        :value="selectedBlock.props?.searchMaxVisibleFields || 3"
                        :min="1"
                        :max="30"
                        :show-button="false"
                        placeholder="最大显示"
                        @update:value="patchBlockProps(selectedBlock.id, { searchMaxVisibleFields: $event || 3 })"
                      />
                      <n-input-number
                        :value="selectedBlock.props?.searchYGap ?? 16"
                        :min="0"
                        :max="48"
                        :show-button="false"
                        placeholder="行距"
                        @update:value="patchBlockProps(selectedBlock.id, { searchYGap: $event ?? 16 })"
                      />
                    </div>
                  </n-form-item>
                  <n-form-item label="表格展示">
                    <div class="style-grid four">
                      <n-select
                        :value="selectedBlock.props?.tableSize || 'small'"
                        :options="componentSizeOptions"
                        @update:value="patchBlockProps(selectedBlock.id, { tableSize: $event || 'small' })"
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
                        <n-checkbox value="searchEnableCollapse" label="搜索折叠" />
                      </n-space>
                    </n-checkbox-group>
                  </n-form-item>
                </n-collapse-item>

                <n-collapse-item v-if="selectedBlock.blockType === 'AiCrudPage' && propertySectionVisible(['表单与弹窗', '新增', '编辑', '表单', '弹窗', '抽屉', '详情', '页脚'])" name="edit" title="表单与弹窗">
                  <div class="form-modal-help">
                    新增、编辑弹窗使用当前表单设计的字段与 AIForm 表单渲染；这里只调整弹窗方式、宽度和表单布局。
                  </div>
                  <n-form-item label="编辑表单布局">
                    <div class="style-grid four">
                      <n-input-number
                        :value="selectedBlock.props?.editGridCols || 1"
                        :min="1"
                        :max="4"
                        :show-button="false"
                        placeholder="列数"
                        @update:value="patchBlockProps(selectedBlock.id, { editGridCols: $event || 1 })"
                      />
                      <n-input-number
                        :value="toNumberOrNull(selectedBlock.props?.editLabelWidth)"
                        :min="0"
                        :max="260"
                        :show-button="false"
                        placeholder="标签宽"
                        @update:value="patchBlockProps(selectedBlock.id, { editLabelWidth: $event ?? 'auto' })"
                      />
                      <n-select
                        :value="selectedBlock.props?.editLabelPlacement || 'left'"
                        :options="labelPlacementOptions"
                        @update:value="patchBlockProps(selectedBlock.id, { editLabelPlacement: $event || 'left' })"
                      />
                      <n-select
                        :value="selectedBlock.props?.editLabelAlign || 'right'"
                        :options="labelAlignOptions"
                        @update:value="patchBlockProps(selectedBlock.id, { editLabelAlign: $event || 'right' })"
                      />
                    </div>
                  </n-form-item>
                  <n-form-item label="表单尺寸 / 间距">
                    <div class="style-grid three">
                      <n-select
                        :value="selectedBlock.props?.editSize || 'medium'"
                        :options="componentSizeOptions"
                        @update:value="patchBlockProps(selectedBlock.id, { editSize: $event || 'medium' })"
                      />
                      <n-input-number
                        :value="selectedBlock.props?.editXGap ?? 16"
                        :min="0"
                        :max="48"
                        :show-button="false"
                        placeholder="列距"
                        @update:value="patchBlockProps(selectedBlock.id, { editXGap: $event ?? 16 })"
                      />
                      <n-input-number
                        :value="selectedBlock.props?.editYGap ?? 8"
                        :min="0"
                        :max="48"
                        :show-button="false"
                        placeholder="行距"
                        @update:value="patchBlockProps(selectedBlock.id, { editYGap: $event ?? 8 })"
                      />
                    </div>
                  </n-form-item>
                  <n-form-item label="弹出方式">
                    <div class="style-grid three">
                      <n-select
                        :value="selectedBlock.props?.modalType || 'modal'"
                        :options="modalTypeOptions"
                        @update:value="patchBlockProps(selectedBlock.id, { modalType: $event || 'modal' })"
                      />
                      <n-select
                        :value="selectedBlock.props?.drawerPlacement || 'right'"
                        :options="drawerPlacementOptions"
                        @update:value="patchBlockProps(selectedBlock.id, { drawerPlacement: $event || 'right' })"
                      />
                      <n-input
                        :value="selectedBlock.props?.modalWidth || '800px'"
                        placeholder="弹窗宽度"
                        @update:value="patchBlockProps(selectedBlock.id, { modalWidth: $event || '800px' })"
                      />
                    </div>
                  </n-form-item>
                  <n-form-item label="详情与页脚">
                    <div class="metrics-editor">
                      <n-input
                        :value="selectedBlock.props?.detailModalWidth || 'min(1080px, 92vw)'"
                        placeholder="详情宽度"
                        @update:value="patchBlockProps(selectedBlock.id, { detailModalWidth: $event || 'min(1080px, 92vw)' })"
                      />
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

                <n-collapse-item v-if="selectedBlock.blockType === 'AiCrudPage' && propertySectionVisible(['工具栏与导入导出', '工具栏', '导入', '导出', '自定义查询', '自定义操作', '按钮文案', '回调', '参数处理', '提交前', '搜索前', '加载列表前'])" name="toolbar" title="工具栏与导入导出">
                  <n-form-item label="工具按钮">
                    <n-checkbox-group
                      :value="resolveAiCrudToolbarFlags(selectedBlock)"
                      @update:value="updateAiCrudToolbarFlags"
                    >
                      <n-space>
                        <n-checkbox value="hideAdd" label="隐藏新增" />
                        <n-checkbox value="hideBatchDelete" label="隐藏批量删除" />
                        <n-checkbox value="showImport" label="导入" />
                        <n-checkbox value="showExport" label="导出" />
                        <n-checkbox value="enableCustomQuery" label="自定义查询" />
                        <n-checkbox value="showExportTasks" label="导出任务" />
                      </n-space>
                    </n-checkbox-group>
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
                  <n-form-item label="自定义操作">
                    <div class="custom-action-summary">
                      <div v-if="customActionList.length" class="action-chip-list">
                        <span
                          v-for="action in customActionList"
                          :key="action.key"
                          class="action-chip"
                        >
                          {{ action.label || action.key }}
                          <small>{{ actionPositionText(action.position) }} · {{ actionBehaviorText(action.actionType) }}</small>
                        </span>
                      </div>
                      <span v-else class="empty">暂未配置自定义操作</span>
                      <n-button size="small" type="primary" secondary block @click="openCustomActionModal">
                        配置自定义操作（{{ customActionList.length }}）
                      </n-button>
                    </div>
                  </n-form-item>
                </n-collapse-item>

                <n-collapse-item v-if="selectedBlock.blockType === 'AiTable' && propertySectionVisible(['表格功能', '工具栏', '分页', '刷新', '密度', '列设置', '搜索切换', '全屏', '滚动尺寸'])" name="table" title="表格功能">
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

                <n-collapse-item v-if="propertySectionVisible(['事件回调', '事件', '回调', '点击', '加载完成', '提交成功', '参数处理', '提交前', '搜索前', '加载列表前'])" name="event-help" title="事件回调">
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
                <n-select
                  :value="selectedBlock.props?.targetPageKey"
                  :options="pageTargetOptions"
                  clearable
                  @update:value="patchBlockProps(selectedBlock.id, { targetPageKey: $event || '' })"
                />
              </n-form-item>
            </template>

            <template v-if="selectedBlock.blockType === 'page-title'">
              <n-form-item label="标题 / 副标题">
                <div class="metrics-editor">
                  <n-input
                    :value="selectedBlock.props?.title"
                    placeholder="标题"
                    @update:value="patchBlockProps(selectedBlock.id, { title: $event })"
                  />
                  <n-input
                    :value="selectedBlock.props?.subtitle"
                    placeholder="副标题"
                    @update:value="patchBlockProps(selectedBlock.id, { subtitle: $event })"
                  />
                </div>
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
              <div class="custom-action-summary">
                <div v-if="customActionList.length" class="action-chip-list">
                  <span
                    v-for="action in customActionList"
                    :key="action.key"
                    class="action-chip"
                  >
                    {{ action.label || action.key }}
                    <small>{{ actionPositionText(action.position) }} · {{ actionBehaviorText(action.actionType) }}</small>
                  </span>
                </div>
                <span v-else class="empty">暂未配置自定义操作</span>
                <n-button size="small" type="primary" secondary block @click="openCustomActionModal">
                  配置自定义操作（{{ customActionList.length }}）
                </n-button>
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
                <n-button size="small" type="primary" secondary @click="fieldDrawerOpen = true">
                  配置字段（{{ selectedBlock.fieldRefs?.length || 0 }}/{{ fields.length }}）
                </n-button>
              </n-form-item>
              <n-form-item label="详情布局">
                <div class="style-grid three">
                  <n-input-number
                    :value="selectedBlock.props?.columnCount || 2"
                    :min="1"
                    :max="4"
                    :show-button="false"
                    @update:value="patchBlockProps(selectedBlock.id, { columnCount: $event || 2 })"
                  />
                  <n-select
                    :value="selectedBlock.props?.labelPlacement || 'left'"
                    :options="labelPlacementOptions"
                    @update:value="patchBlockProps(selectedBlock.id, { labelPlacement: $event || 'left' })"
                  />
                  <n-switch
                    :value="!!selectedBlock.props?.bordered"
                    @update:value="patchBlockProps(selectedBlock.id, { bordered: $event })"
                  />
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
              <n-form-item label="按钮配置">
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
                  <n-switch
                    :value="!!selectedBlock.props?.secondary"
                    @update:value="patchBlockProps(selectedBlock.id, { secondary: $event })"
                  />
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
          </n-form>
        </div>
      </div>
    </aside>

    <n-drawer v-model:show="fieldDrawerOpen" :width="680" placement="right">
      <n-drawer-content :title="`配置字段 · ${selectedBlockMeta?.title || ''}`" closable>
        <div v-if="selectedBlock" class="field-config">
          <div class="field-config-section">
            <div class="section-title">
              已选字段 ({{ selectedBlock.fieldRefs?.length || 0 }})
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
                    search: selectedBlock.blockType === 'search-form',
                    table: ['data-table', 'AiCrudPage', 'AiTable', 'AiForm', 'detail-info'].includes(selectedBlock.blockType),
                  }"
                >
                  <span class="f-handle">☰</span>
                  <span class="f-name">
                    {{ element.label || element.field }}
                    <small v-if="element.sourceLabel || element.modelName">{{ element.sourceLabel || element.modelName }}</small>
                  </span>
                  <span class="f-code">{{ element.field }}</span>
                  <button type="button" class="f-remove" @click="toggleField(element.field, false)">
                    移除
                  </button>
                  <div v-if="selectedBlock.blockType === 'search-form'" class="field-setting-row search-setting-row">
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
                  <div v-if="['data-table', 'AiCrudPage', 'AiTable', 'AiForm', 'detail-info'].includes(selectedBlock.blockType)" class="field-setting-row table-setting-row">
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
                  <div v-if="['data-table', 'AiCrudPage', 'AiTable'].includes(selectedBlock.blockType)" class="field-setting-row column-link-row">
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
            <div v-if="!(selectedBlock.fieldRefs?.length)" class="empty">
              当前没有选择字段
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
              :key="action.key || idx"
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
                  支持站内跳转、外部链接、刷新列表；目标地址和参数值可使用 :id 或 ${field} 占位符。
                </div>
              </div>
              <n-button quaternary type="error" @click="removeCustomAction(activeActionIndex)">
                删除
              </n-button>
            </div>

            <n-form size="small" label-placement="top" :show-feedback="false">
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

              <div class="action-form-grid">
                <n-form-item label="交互方式">
                  <n-select
                    :value="activeAction.actionType || 'route'"
                    :options="actionBehaviorOptions"
                    @update:value="updateActiveCustomAction({ actionType: $event })"
                  />
                </n-form-item>
                <n-form-item label="打开方式">
                  <n-select
                    :value="activeAction.openTarget || '_self'"
                    :disabled="(activeAction.actionType || 'route') === 'refresh'"
                    :options="actionOpenTargetOptions"
                    @update:value="updateActiveCustomAction({ openTarget: $event })"
                  />
                </n-form-item>
              </div>

              <n-form-item label="目标地址">
                <n-input
                  :value="activeAction.routePath"
                  :disabled="(activeAction.actionType || 'route') === 'refresh'"
                  :placeholder="actionPathPlaceholder(activeAction)"
                  @update:value="updateActiveCustomAction({ routePath: $event })"
                />
              </n-form-item>

              <n-form-item label="确认提示">
                <n-input
                  :value="activeAction.confirmText"
                  placeholder="例如：确认处理 :id 吗？留空则不提示"
                  @update:value="updateActiveCustomAction({ confirmText: $event })"
                />
              </n-form-item>

              <n-form-item label="参数映射">
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
                    <n-input
                      :value="param.value"
                      placeholder="参数值，如 :id / ${name}"
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
              </n-form-item>
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

    <n-modal v-model:show="sourceModalOpen" preset="card" title="查看源码" class="list-source-modal" :bordered="false">
      <n-tabs type="line" animated>
        <n-tab-pane name="layout" tab="画布布局 JSON">
          <n-input
            :value="layoutCodeText"
            type="textarea"
            readonly
            :autosize="{ minRows: 18, maxRows: 28 }"
            class="source-code-textarea"
          />
        </n-tab-pane>
        <n-tab-pane name="block" tab="当前区块 JSON">
          <n-input
            :value="selectedBlockCodeText"
            type="textarea"
            readonly
            :autosize="{ minRows: 18, maxRows: 28 }"
            class="source-code-textarea"
          />
        </n-tab-pane>
      </n-tabs>
    </n-modal>
  </div>
</template>

<script setup>
import { ChevronBackOutline, ChevronForwardOutline, EllipsisHorizontalOutline, SearchOutline } from '@vicons/ionicons5'
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import draggable from 'vuedraggable'
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
})

const emit = defineEmits(['update:modelValue'])

const rowHeight = 32
const gap = 8
const previewMinWidth = 360
const TREE_PANEL_COLLAPSED_WIDTH = 44
const canvasWidthOptions = [
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
const blockWidthModeOptions = [
  { label: '撑满 100%', value: 'full' },
  { label: '内容 auto', value: 'auto' },
  { label: '固定 px', value: 'fixed' },
]
const actionPositionOptions = [
  { label: '工具栏', value: 'toolbar' },
  { label: '行操作列', value: 'row' },
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
  { label: '刷新列表', value: 'refresh' },
]
const actionOpenTargetOptions = [
  { label: '当前页', value: '_self' },
  { label: '新窗口', value: '_blank' },
]
const labelPlacementOptions = [
  { label: '左侧', value: 'left' },
  { label: '顶部', value: 'top' },
]
const labelAlignOptions = [
  { label: '左对齐', value: 'left' },
  { label: '右对齐', value: 'right' },
]
const componentSizeOptions = [
  { label: '小', value: 'small' },
  { label: '中', value: 'medium' },
  { label: '大', value: 'large' },
]
const renderModeOptions = [
  { label: '表格', value: 'table' },
  { label: '卡片', value: 'card' },
]
const requestMethodOptions = [
  { label: 'GET', value: 'get' },
  { label: 'POST', value: 'post' },
]
const modalTypeOptions = [
  { label: '弹窗', value: 'modal' },
  { label: '抽屉', value: 'drawer' },
]
const drawerPlacementOptions = [
  { label: '右侧', value: 'right' },
  { label: '左侧', value: 'left' },
  { label: '顶部', value: 'top' },
  { label: '底部', value: 'bottom' },
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
const columnClickActionOptions = [
  { label: '无', value: 'none' },
  { label: '跳转页面', value: 'navigate' },
]
const borderStyleOptions = [
  { label: '实线', value: 'solid' },
  { label: '虚线', value: 'dashed' },
  { label: '点线', value: 'dotted' },
  { label: '无边框', value: 'none' },
]
const shadowOptions = [
  { label: '无阴影', value: 'none' },
  { label: '轻微', value: '0 2px 8px rgba(15, 23, 42, 0.08)' },
  { label: '中等', value: '0 10px 24px rgba(15, 23, 42, 0.12)' },
  { label: '明显', value: '0 18px 42px rgba(15, 23, 42, 0.16)' },
]
const eventTriggerOptions = [
  { label: '点击', value: 'click' },
  { label: '加载完成', value: 'load' },
  { label: '值变化', value: 'change' },
  { label: '节点选择', value: 'nodeSelect' },
  { label: '行点击', value: 'rowClick' },
  { label: '提交成功', value: 'submitSuccess' },
]
const blockEventActionOptions = [
  { label: '无动作', value: 'none' },
  { label: '返回上一页', value: 'back' },
  { label: '页面跳转', value: 'navigate' },
  { label: '刷新组件', value: 'refreshBlock' },
  { label: '过滤组件', value: 'filterBlock' },
  { label: '打开弹窗', value: 'openModal' },
  { label: '打开抽屉', value: 'openDrawer' },
  { label: '接口请求', value: 'request' },
  { label: '自定义脚本', value: 'customScript' },
]
const backButtonActionOptions = [
  { label: '浏览器返回', value: 'back' },
  { label: '跳转页面', value: 'navigate' },
]
const resizeAnchors = ['top-left', 'top', 'top-right', 'right', 'bottom-right', 'bottom', 'bottom-left', 'left']

const canvasRef = ref(null)
const canvasScrollRef = ref(null)
const selectedBlockId = ref(null)
const fieldDrawerOpen = ref(false)
const customActionModalOpen = ref(false)
const sourceModalOpen = ref(false)
const activeActionIndex = ref(0)
const paletteKeyword = ref('')
const propertyKeyword = ref('')
const paletteCollapsed = ref(false)
const propertyCollapsed = ref(false)
const activeTabKey = ref('')
const canvasDragActive = ref(false)
const draggedBlockType = ref('')
const dragOverCell = ref(null)
const dragOverPoint = ref(null)
const deferLayoutEmit = ref(false)
const movingBlockId = ref('')
const movingPreviewBlock = ref(null)
const movingPixelOffset = ref({ x: 0, y: 0 })
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
const designCanvasWidth = computed(() => clamp(localLayout.value.designWidth || LIST_PAGE_DESIGN_WIDTH, 960, 2560))
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
const canvasScaleStyle = computed(() => ({
  transform: `scale(${canvasZoom.value})`,
  transformOrigin: '0 0',
}))
const canvasZoomStageStyle = computed(() => ({
  width: `${(canvasGridWidth.value + (props.readonly ? 0 : 24)) * canvasZoom.value}px`,
  minHeight: `${(canvasGridHeight.value + (props.readonly ? 0 : 24)) * canvasZoom.value}px`,
}))
const collapsedTreeFrames = computed(() => {
  if (!props.readonly)
    return []
  return blocks.value
    .filter(block => block.blockType === 'tree-panel' && collapsedTreePanelMap.value[block.id])
    .map(block => ({ block, rect: resolveBlockFrame(block) }))
    .sort((a, b) => a.rect.x - b.rect.x)
})

const selectedBlock = computed(() => blocks.value.find(b => b.id === selectedBlockId.value) || null)
const selectedBlockEvents = computed(() => selectedBlock.value?.props?.events || [])
const selectedBlockStyle = computed(() => ({
  ...createDefaultBlockStyle(),
  ...(selectedBlock.value?.props?.style || {}),
}))
const selectedBlockFrame = computed(() => selectedBlock.value
  ? resolveBlockFrame(selectedBlock.value)
  : { x: 0, y: 0, width: 24, height: 24 })
const selectedBlockWidthMode = computed(() => selectedBlock.value ? resolveBlockWidthMode(selectedBlock.value) : 'full')
const selectedBlockFixedWidth = computed(() => {
  if (!selectedBlock.value)
    return 24
  const value = selectedBlock.value.props?.style?.width
  const fallback = selectedBlockFrame.value.width
  return Math.max(24, resolveCssNumber(value, fallback))
})
const layoutCodeText = computed(() => JSON.stringify(localLayout.value || {}, null, 2))
const selectedBlockCodeText = computed(() => selectedBlock.value
  ? JSON.stringify(selectedBlock.value, null, 2)
  : '请先选中一个区块')
const customActionList = computed(() => selectedBlock.value?.props?.customActions || [])
const activeAction = computed(() => customActionList.value[activeActionIndex.value] || null)
const dropPreviewLabel = computed(() => {
  const meta = resolveListPageBlockMeta(draggedBlockType.value)
  return meta ? `放置 ${meta.title}` : '放置区块'
})
const dropPreviewStyle = computed(() => {
  if (!canvasDragActive.value || !draggedBlockType.value || !dragOverPoint.value)
    return null
  const meta = resolveListPageBlockMeta(draggedBlockType.value)
  if (!meta)
    return null
  const width = gridWidthToPixels(Math.min(meta.defaultW || 4, LIST_PAGE_GRID_COLS))
  const height = gridHeightToPixels(Math.max(1, meta.defaultH || 2))
  const x = clamp(dragOverPoint.value.x, 0, Math.max(0, canvasGridWidth.value - width))
  const y = Math.max(0, dragOverPoint.value.y)
  return {
    left: `${x}px`,
    top: `${y}px`,
    width: `${width}px`,
    height: `${height}px`,
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
    return '左树右表'
  if (props.layoutType === 'master-detail-crud')
    return '主子表'
  return '标准单表'
})
const selectedBlockMeta = computed(() => selectedBlock.value ? resolveListPageBlockMeta(selectedBlock.value.blockType) : null)
const blockTargetOptions = computed(() => blocks.value
  .filter(block => block.id !== selectedBlock.value?.id)
  .map(block => ({
    label: `${block.label || block.blockType}（${block.blockType}）`,
    value: block.id,
  })))
const pageTargetOptions = computed(() => props.pages.map(page => ({
  label: `${page.pageName || page.pageKey}（${page.pageType || 'custom'}）`,
  value: page.pageKey,
})))
const childBlockTypeOptions = computed(() => listPageBlockCatalog
  .filter(item => !item.unique)
  .filter(item => item.blockType !== 'card' && item.blockType !== 'tabs')
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
    { key: 'layout', title: '布局组件', items: [] },
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
const selectedBlockZoneKey = computed(() => selectedBlock.value?.blockType === 'search-form' ? 'search' : 'table')
const selectedFieldsList = computed(() => (selectedBlock.value?.fieldRefs || [])
  .map(ref => fieldMap.value.get(ref))
  .filter(Boolean))
const availableFields = computed(() => {
  const set = new Set(selectedBlock.value?.fieldRefs || [])
  return props.fields.filter(f => isPageFieldVisible(f, selectedBlockZoneKey.value) && !set.has(f.field))
})

watch(
  selectedBlock,
  (block) => {
    if (block?.blockType === 'tabs') {
      const firstKey = block.props?.tabs?.[0]?.key || ''
      if (!block.props?.tabs?.some(tab => tab.key === activeTabKey.value))
        activeTabKey.value = firstKey
    }
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

function resolveBlockStyle(block) {
  const componentStyle = block.props?.style || {}
  const rect = resolveRuntimeBlockFrame(block, resolveBlockFrame(block))
  const style = {
    left: `${rect.x}px`,
    top: `${rect.y}px`,
    width: `${rect.width}px`,
    height: `${rect.height}px`,
    minWidth: resolveAbsoluteCssSize(componentStyle.minWidth),
    maxWidth: resolveAbsoluteCssSize(componentStyle.maxWidth),
    minHeight: resolveAbsoluteCssSize(componentStyle.minHeight),
    maxHeight: resolveAbsoluteCssSize(componentStyle.maxHeight),
  }
  if (movingBlockId.value === block.id) {
    style.transform = `translate3d(${movingPixelOffset.value.x}px, ${movingPixelOffset.value.y}px, 0) scale(0.995)`
  }
  return style
}

function resolveRuntimeBlockFrame(block, rect) {
  if (!props.readonly || !collapsedTreeFrames.value.length)
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
    searchEnableCollapse: values.includes('searchEnableCollapse'),
  })
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

function resolveAiCrudToolbarFlags(block = {}) {
  const props = block.props || {}
  const flags = []
  if (props.hideAdd === true)
    flags.push('hideAdd')
  if (props.hideBatchDelete === true)
    flags.push('hideBatchDelete')
  if (props.showImport === true)
    flags.push('showImport')
  if (props.showExport === true)
    flags.push('showExport')
  if (props.enableCustomQuery === true)
    flags.push('enableCustomQuery')
  if (props.showExportTasks !== false)
    flags.push('showExportTasks')
  return flags
}

function updateAiCrudToolbarFlags(values = []) {
  if (!selectedBlock.value)
    return
  patchBlockProps(selectedBlock.value.id, {
    hideAdd: values.includes('hideAdd'),
    hideBatchDelete: values.includes('hideBatchDelete'),
    showImport: values.includes('showImport'),
    showExport: values.includes('showExport'),
    enableCustomQuery: values.includes('enableCustomQuery'),
    showExportTasks: values.includes('showExportTasks'),
  })
}

function updateSelectedBlockHookRules(rules) {
  if (!selectedBlock.value)
    return
  patchBlockProps(selectedBlock.value.id, { crudHookRules: rules || {}, beforeSubmitRules: [] })
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
  if (item.unique && blocks.value.some(b => b.blockType === item.blockType))
    return true
  if (item.onlyFor && !item.onlyFor.includes(props.layoutType))
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

function handlePaletteClick(item) {
  if (props.readonly)
    return
  if (isBlockDisabled(item))
    return
  appendBlock(item.blockType)
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

function handleTreeSourceChange(sourceModelCode) {
  if (!selectedBlock.value)
    return
  const defaultConfig = resolveDefaultTreeConfig(props.modelSchema, { sourceModelCode })
  patchBlockProps(selectedBlock.value.id, defaultConfig)
}

function handleCanvasDrop(event) {
  if (props.readonly)
    return
  const blockType = event.dataTransfer?.getData('application/x-list-block')
  resetCanvasDragState()
  if (!blockType)
    return
  const meta = resolveListPageBlockMeta(blockType)
  if (!meta)
    return
  if (meta.unique && blocks.value.some(block => block.blockType === blockType))
    return
  const container = resolveDropContainer(event)
  if (container) {
    appendContainerChild(container.id, blockType)
    return
  }
  const point = pixelToPoint(event.clientX, event.clientY)
  appendBlock(blockType, point)
}

function handleCanvasDragEnter(event) {
  if (props.readonly)
    return
  const blockType = event.dataTransfer?.getData('application/x-list-block') || draggedBlockType.value
  if (!blockType)
    return
  canvasDragActive.value = true
  draggedBlockType.value = blockType
}

function handleCanvasDragOver(event) {
  if (props.readonly)
    return
  const blockType = event.dataTransfer?.getData('application/x-list-block') || draggedBlockType.value
  if (!blockType)
    return
  canvasDragActive.value = true
  draggedBlockType.value = blockType
  dragOverCell.value = pixelToCell(event.clientX, event.clientY)
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
  dragOverCell.value = null
  dragOverPoint.value = null
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
  const frame = {
    x: clamp(Number(point.x) || 0, 0, Math.max(0, canvasGridWidth.value - gridWidthToPixels(block.gridW))),
    y: Math.max(0, Number(point.y) || 0),
    width: gridWidthToPixels(block.gridW),
    height: gridHeightToPixels(block.gridH),
  }
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
  const node = event.target?.closest?.('[data-block-id]')
  const blockId = node?.dataset?.blockId
  if (!blockId)
    return null
  const block = blocks.value.find(item => item.id === blockId)
  return isContainerBlock(block) ? block : null
}

function isContainerBlock(block = {}) {
  return ['card', 'tabs'].includes(block?.blockType)
}

function appendContainerChild(containerId, blockType) {
  const container = blocks.value.find(block => block.id === containerId)
  if (!container || !blockType)
    return
  if (container.blockType === 'tabs') {
    appendTabChild(blockType, containerId)
    selectBlock(containerId)
    return
  }
  const child = createContainerChildBlock(blockType)
  if (!child)
    return
  localLayout.value = {
    ...localLayout.value,
    items: blocks.value.map(block => block.id === containerId
      ? { ...block, children: [...(block.children || []), child] }
      : block),
  }
  selectBlock(containerId)
}

function createContainerChildBlock(blockType) {
  const child = createGridBlock(blockType, props.modelSchema, { gridX: 0, gridY: 0 })
  if (!child)
    return null
  return {
    ...child,
    id: `${child.id}_child_${Date.now()}`,
    gridX: 0,
    gridY: 0,
    gridW: LIST_PAGE_GRID_COLS,
    gridH: Math.max(1, child.gridH || 2),
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

function nextFreePixelTop() {
  return blocks.value.reduce((acc, block) => {
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

function patchBlock(id, patch) {
  localLayout.value = {
    ...localLayout.value,
    items: normalizeGridItems(blocks.value.map(b => b.id === id
      ? {
          ...b,
          ...patch,
          gridX: clamp(patch.gridX ?? b.gridX, 0, 11),
          gridW: clamp(patch.gridW ?? b.gridW, 1, 12),
        }
      : b)),
  }
}

function updateDesignWidth(value) {
  const nextWidth = clamp(value || LIST_PAGE_DESIGN_WIDTH, 960, 2560)
  localLayout.value = {
    ...localLayout.value,
    designWidth: nextWidth,
  }
}

function updateCanvasZoom(value) {
  canvasZoom.value = clamp(Number(value) || 1, 0.5, 1.25)
}

function patchBlockProps(id, patch) {
  localLayout.value = {
    ...localLayout.value,
    items: normalizeGridItems(blocks.value.map(b => b.id === id
      ? { ...b, props: { ...(b.props || {}), ...patch } }
      : b)),
  }
}

function patchBlockFrame(id, patch) {
  const source = blocks.value.find(block => block.id === id)
  if (!source)
    return false
  const current = resolveBlockFrame(source)
  const widthChanged = Object.prototype.hasOwnProperty.call(patch, 'width')
  const currentWidthMode = resolveBlockWidthMode(source)
  const nextWidthMode = widthChanged ? 'fixed' : currentWidthMode
  const next = {
    ...current,
    ...patch,
  }
  next.width = Math.max(24, Number(next.width) || current.width)
  next.height = Math.max(24, Number(next.height) || current.height)
  next.x = clamp(Number(next.x) || 0, 0, Math.max(0, canvasGridWidth.value - next.width))
  next.y = Math.max(0, Number(next.y) || 0)
  if (doesFrameOverlapBlocks(id, next))
    return false
  const gridPatch = frameToGridPatch(next)
  const nextWidthValue = nextWidthMode === 'full' ? '100%' : nextWidthMode === 'auto' ? 'auto' : next.width
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
              x: next.x,
              y: next.y,
              widthMode: nextWidthMode,
              width: nextWidthValue,
              height: next.height,
            },
          },
        }
      : block)),
  }
  return true
}

function doesFrameOverlapBlocks(sourceId, frame) {
  return blocks.value.some((block) => {
    if (block.id === sourceId)
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
  const nextFrame = {
    ...current,
    width: widthMode === 'full'
      ? Math.max(24, canvasGridWidth.value - current.x)
      : widthMode === 'auto'
        ? Math.max(240, Math.min(520, current.width))
        : current.width,
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
              height: nextFrame.height,
            },
          },
        }
      : block)),
  }
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
    description: '',
    params: [],
  })
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
    params: [...(eventItem.params || []), { name: '', value: '' }],
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
  const source = blocks.value.find(block => block.id === id)
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
    items: normalizeGridItems(blocks.value.filter(b => b.id !== id)),
  }
  if (selectedBlockId.value === id) {
    selectedBlockId.value = null
  }
}

function duplicateBlock(id) {
  const source = blocks.value.find(block => block.id === id)
  if (!source)
    return
  const meta = resolveListPageBlockMeta(source.blockType)
  if (meta?.unique)
    return
  const copy = JSON.parse(JSON.stringify(source))
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
  copy.id = `${source.id}_copy_${Date.now()}`
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
    return {
      ...item,
      gridX,
      gridW,
      gridY: Math.max(0, Number(item.gridY) || 0),
      gridH: Math.max(resolveAutoGridH(item, gridW), Number(item.gridH) || 1),
    }
  })
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
    return Math.max(10, gridRowsForPixels(320))
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
    originX: rect.x,
    originY: rect.y,
    originW: rect.width,
    originH: rect.height,
  }
  window.addEventListener('pointermove', onMove)
  window.addEventListener('pointerup', endMove)
}
function onMove(event) {
  if (!moveCtx)
    return
  const block = blocks.value.find(b => b.id === moveCtx.blockId)
  if (!block)
    return
  const zoom = canvasZoom.value || 1
  const rawDx = (event.clientX - moveCtx.startX) / zoom
  const rawDy = (event.clientY - moveCtx.startY) / zoom
  const minDx = -moveCtx.originX
  const widthMode = resolveBlockWidthMode(block)
  const maxDx = widthMode === 'full'
    ? Math.max(0, canvasGridWidth.value - 24 - moveCtx.originX)
    : Math.max(0, canvasGridWidth.value - moveCtx.originW - moveCtx.originX)
  const minDy = -moveCtx.originY
  const offsetX = clamp(rawDx, minDx, maxDx)
  const offsetY = Math.max(rawDy, minDy)
  movingPixelOffset.value = { x: offsetX, y: offsetY }
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
    const block = blocks.value.find(item => item.id === moveCtx.blockId)
    if (block) {
      const rect = resolveBlockFrame(preview)
      patchBlockFrame(block.id, { x: rect.x, y: rect.y, width: rect.width, height: rect.height })
    }
  }
  moveCtx = null
  movingBlockId.value = ''
  movingPreviewBlock.value = null
  movingPixelOffset.value = { x: 0, y: 0 }
  flushDeferredLayoutEmit()
  window.removeEventListener('pointermove', onMove)
  window.removeEventListener('pointerup', endMove)
}

// Resize
let resizeCtx = null
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
  const zoom = canvasZoom.value || 1
  const dw = Math.round((event.clientX - resizeCtx.startX) / zoom)
  const dh = Math.round((event.clientY - resizeCtx.startY) / zoom)
  const block = blocks.value.find(b => b.id === resizeCtx.blockId)
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
  endResize()
  canvasResizeObserver?.disconnect?.()
  canvasResizeObserver = null
  window.removeEventListener('resize', updateCanvasViewportWidth)
})

// Field config drawer
function toggleField(fieldName, add) {
  if (!selectedBlock.value)
    return
  const current = selectedBlock.value.fieldRefs || []
  const next = add
    ? [...current, fieldName]
    : current.filter(f => f !== fieldName)
  patchBlock(selectedBlock.value.id, { fieldRefs: Array.from(new Set(next)) })
}

function handleSelectedReorder(rows) {
  if (!selectedBlock.value)
    return
  patchBlock(selectedBlock.value.id, { fieldRefs: rows.map(r => r.field) })
}

function resolveFieldSetting(fieldName) {
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
  if (!selectedBlock.value || selectedBlock.value.blockType !== 'data-table')
    return
  const align = ['left', 'center', 'right'].includes(value) ? value : 'left'
  const nextSettings = { ...(selectedBlock.value.props?.fieldSettings || {}) }
  ;(selectedBlock.value.fieldRefs || []).forEach((fieldName) => {
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

function addCustomAction() {
  const list = [...(selectedBlock.value?.props?.customActions || [])]
  list.push({
    key: `custom_${Date.now()}`,
    label: '自定义按钮',
    position: 'row',
    type: 'primary',
    actionType: 'route',
    routePath: '',
    openTarget: '_self',
    params: [],
  })
  patchBlockProps(selectedBlock.value.id, { customActions: list })
  activeActionIndex.value = list.length - 1
  customActionModalOpen.value = true
}

function updateCustomAction(idx, patch) {
  const list = [...(selectedBlock.value?.props?.customActions || [])]
  list[idx] = { ...list[idx], ...patch }
  patchBlockProps(selectedBlock.value.id, { customActions: list })
}

function removeCustomAction(idx) {
  const list = [...(selectedBlock.value?.props?.customActions || [])]
  list.splice(idx, 1)
  patchBlockProps(selectedBlock.value.id, { customActions: list })
  activeActionIndex.value = Math.max(0, Math.min(activeActionIndex.value, list.length - 1))
}

function openCustomActionModal() {
  activeActionIndex.value = customActionList.value.length ? 0 : -1
  customActionModalOpen.value = true
}

function updateActiveCustomAction(patch) {
  if (activeActionIndex.value < 0)
    return
  updateCustomAction(activeActionIndex.value, patch)
}

function addActionParam() {
  const params = [...(activeAction.value?.params || []), { name: '', value: '' }]
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

function actionPositionText(position) {
  return actionPositionOptions.find(item => item.value === (position || 'toolbar'))?.label || '工具栏'
}

function actionBehaviorText(actionType) {
  return actionBehaviorOptions.find(item => item.value === (actionType || 'route'))?.label || '站内跳转'
}

function actionPathPlaceholder(action = {}) {
  if ((action.actionType || 'route') === 'external')
    return 'https://example.com/:id'
  if ((action.actionType || 'route') === 'refresh')
    return '刷新列表无需地址'
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
  return keywords.some(item => String(item || '').toLowerCase().includes(keyword))
}
</script>

<style scoped>
.list-grid-designer {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr) 380px;
  gap: 12px;
  height: 100%;
  min-height: 0;
  min-width: 0;
}

.list-grid-designer.left-collapsed {
  grid-template-columns: 42px minmax(0, 1fr) 380px;
}

.list-grid-designer.right-collapsed {
  grid-template-columns: 280px minmax(0, 1fr) 42px;
}

.list-grid-designer.left-collapsed.right-collapsed {
  grid-template-columns: 42px minmax(0, 1fr) 42px;
}

.list-grid-designer.readonly,
.list-grid-designer.readonly.left-collapsed,
.list-grid-designer.readonly.right-collapsed,
.list-grid-designer.readonly.left-collapsed.right-collapsed {
  grid-template-columns: minmax(0, 1fr);
  min-height: 0;
  background: transparent;
}

.palette-panel,
.canvas-panel,
.block-property-panel,
.property-panel {
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  background: #fff;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.palette-panel {
  min-height: 0;
  padding: 12px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
}

.palette-panel-head,
.property-panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.panel-collapse-button,
.side-rail-toggle-button {
  --n-color: #eef6ff !important;
  --n-color-hover: #dbeafe !important;
  --n-color-pressed: #bfdbfe !important;
  --n-color-focus: #eef6ff !important;
  --n-border: 1px solid #bfdbfe !important;
  --n-border-hover: 1px solid #93c5fd !important;
  --n-border-pressed: 1px solid #60a5fa !important;
  --n-border-focus: 1px solid #93c5fd !important;
  --n-text-color: #1d4ed8 !important;
}

.side-rail-toggle-button {
  display: grid;
  place-items: center;
  width: 42px;
  height: 100%;
  min-height: 220px;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  background: #fff;
  color: #1d4ed8;
  cursor: pointer;
}

.side-rail-toggle-button:hover {
  border-color: #93c5fd;
  background: #eff6ff;
}

.panel-title {
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}

.panel-desc {
  margin-top: 2px;
  font-size: 12px;
  color: #64748b;
}

.palette-groups {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow-y: auto;
}

.palette-search {
  margin-top: 12px;
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
  color: #475569;
  margin-bottom: 6px;
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
  background: #2563eb;
  outline: 3px solid #dbeafe;
}

.palette-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 6px;
}

.palette-item {
  display: grid;
  gap: 2px;
  padding: 10px 11px;
  border: 1px solid #dbe3ee;
  border-radius: 7px;
  background: rgba(255, 255, 255, 0.9);
  text-align: left;
  cursor: grab;
  transition: all 0.15s;
}

.palette-item:hover:not(:disabled) {
  border-color: #2563eb;
  background: #eff6ff;
  box-shadow: 0 8px 18px rgba(37, 99, 235, 0.1);
  transform: translateY(-1px);
}

.palette-item:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.item-title {
  font-size: 13px;
  font-weight: 600;
  color: #0f172a;
}

.item-desc {
  min-height: 30px;
  font-size: 11px;
  color: #64748b;
  line-height: 15px;
}

.canvas-panel {
  background: radial-gradient(circle at 18px 18px, rgba(37, 99, 235, 0.08) 0 1px, transparent 1px 100%), #eef3f8;
  background-size:
    18px 18px,
    auto;
  min-width: 0;
  min-height: 0;
}

.canvas-panel.drag-over {
  border-color: #93c5fd;
  box-shadow: inset 0 0 0 1px #bfdbfe;
}

.canvas-scroll {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 0;
}

.canvas-zoom-stage {
  position: relative;
  min-width: 100%;
  box-sizing: content-box;
  padding: 1px;
}

.canvas-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 44px;
  padding: 7px 10px;
  border-bottom: 1px solid #e5e7eb;
  background: #fff;
}

.canvas-width-control,
.canvas-zoom-control {
  display: inline-grid;
  align-items: center;
  gap: 6px;
  padding: 3px 6px 3px 8px;
  border: 1px solid #dbe3ee;
  border-radius: 7px;
  background: #f8fafc;
  color: #475569;
  font-size: 12px;
}

.canvas-width-control {
  grid-template-columns: auto 104px 68px;
}

.canvas-zoom-control {
  grid-template-columns: auto 24px 82px 24px;
}

.canvas-width-select,
.canvas-width-input,
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
  background:
    linear-gradient(#fff, #fff) padding-box,
    linear-gradient(135deg, rgba(37, 99, 235, 0.12), rgba(15, 23, 42, 0.04)) border-box;
  will-change: transform;
}

.list-grid-designer.readonly .canvas-panel {
  border: 0;
  background: transparent;
}

.list-grid-designer.readonly .canvas-scroll {
  overflow: visible;
}

.list-grid-designer.readonly .canvas-zoom-stage {
  padding: 0;
}

.list-grid-designer.readonly .canvas-grid {
  margin: 0;
  background: transparent;
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
  background: #f8fbff;
}

.grid-row,
.grid-col {
  position: absolute;
  pointer-events: none;
}

.grid-row {
  left: 0;
  right: 0;
  border-bottom: 1px dashed #eef2f7;
}

.grid-col {
  top: 0;
  bottom: 0;
  border-right: 1px dashed #eef2f7;
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
  border: 2px solid transparent;
  border-radius: 10px;
  pointer-events: none;
  transition:
    border-color 160ms ease,
    box-shadow 160ms ease;
}

.grid-item:hover {
  z-index: 12;
}

.grid-item:hover::after {
  border-color: rgba(37, 99, 235, 0.62);
  box-shadow:
    0 0 0 1px rgba(37, 99, 235, 0.08),
    0 10px 22px rgba(37, 99, 235, 0.12);
}

.grid-item.selected {
  z-index: 10;
  filter: drop-shadow(0 14px 24px rgba(37, 99, 235, 0.14));
}

.grid-item.selected::after {
  border-color: #2563eb;
  box-shadow: 0 0 0 1px rgba(37, 99, 235, 0.18);
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

.property-panel-head {
  padding: 12px 14px;
  border-bottom: 1px solid #eef2f7;
  background:
    linear-gradient(180deg, rgba(248, 251, 255, 0.96) 0%, #fff 100%),
    linear-gradient(90deg, rgba(37, 99, 235, 0.12), transparent 58%);
}

.property-search,
.designer-panel-search {
  position: relative;
  z-index: 3;
  flex: 0 0 auto;
  padding: 10px 12px;
  border-bottom: 1px solid #eef2f7;
  background: #fff;
  box-shadow: 0 1px 0 rgba(226, 232, 240, 0.75);
}

.property-panel-title {
  color: #0f172a;
  font-size: 14px;
  font-weight: 700;
}

.property-panel-desc {
  margin-top: 2px;
  color: #64748b;
  font-size: 12px;
  line-height: 18px;
}

.property-panel {
  flex: 1;
  min-height: 0;
  border: 0;
  border-radius: 0;
  background: linear-gradient(180deg, #f8fafc 0%, #eef4fb 100%);
  padding: 12px;
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
  border: 1px solid #bfdbfe;
  border-radius: 8px;
  background: linear-gradient(135deg, #eff6ff 0%, #fff 74%), #fff;
  box-shadow: 0 8px 20px rgba(37, 99, 235, 0.08);
}

.prop-title {
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}

.prop-meta {
  margin-top: 2px;
  font-size: 12px;
  color: #475569;
}

.property-body {
  display: grid;
  gap: 12px;
}

.property-body :deep(.n-form-item) {
  margin-bottom: 10px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.96);
  padding: 10px;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
}

.property-body :deep(.n-form-item-label) {
  position: relative;
  display: flex !important;
  align-items: center;
  align-self: flex-start;
  min-height: 24px;
  height: 24px;
  padding-left: 10px;
  padding-top: 0;
  padding-bottom: 0;
  color: #334155;
  font-weight: 600;
  line-height: 20px;
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
}

.property-body :deep(.n-form-item-label::before) {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  width: 4px;
  height: 12px;
  border-radius: 999px;
  background: #2563eb;
  transform: translateY(-50%);
}

.event-editor,
.container-child-editor {
  display: grid;
  gap: 8px;
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

.event-row {
  display: grid;
  gap: 8px;
  padding: 8px;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background: #f8fbff;
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
  color: #0f172a;
  font-size: 12px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.event-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 6px;
}

.event-param-list {
  display: grid;
  gap: 6px;
}

.event-param-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1.3fr) auto;
  gap: 6px;
  align-items: center;
}

.container-child-row {
  padding: 8px 10px;
  border: 1px solid #e2e8f0;
  border-radius: 7px;
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

.advanced-config-collapse {
  display: grid;
  gap: 8px;
  margin: 2px 0 12px;
}

.advanced-config-collapse :deep(.n-collapse-item) {
  margin: 0;
  overflow: hidden;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
}

.advanced-config-collapse :deep(.n-collapse-item__header) {
  min-height: 38px;
  padding: 0 12px;
  border-bottom: 1px solid transparent;
  background: linear-gradient(180deg, #f8fbff 0%, #fff 100%);
}

.advanced-config-collapse :deep(.n-collapse-item__header-main) {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #0f172a;
  font-size: 12px;
  font-weight: 800;
}

.advanced-config-collapse :deep(.n-collapse-item__header-main::before) {
  content: '';
  width: 4px;
  height: 14px;
  border-radius: 999px;
  background: #2563eb;
}

.advanced-config-collapse :deep(.n-collapse-item--active .n-collapse-item__header) {
  border-bottom-color: #eef2f7;
}

.advanced-config-collapse :deep(.n-collapse-item__content-inner) {
  padding: 10px 12px 12px;
  background: #fff;
}

.api-config-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 8px;
  width: 100%;
}

.api-config-grid :deep(.n-input) {
  width: 100%;
}

.event-help {
  padding: 10px 12px;
  border: 1px dashed #bfdbfe;
  border-radius: 8px;
  background: #eff6ff;
  color: #475569;
  font-size: 12px;
  line-height: 1.6;
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

.custom-action-summary {
  display: grid;
  gap: 10px;
}

.action-chip-list {
  display: grid;
  gap: 6px;
}

.action-chip {
  display: grid;
  gap: 2px;
  min-width: 0;
  padding: 8px 10px;
  border: 1px solid #dbe3ee;
  border-radius: 6px;
  background: #f8fafc;
  color: #0f172a;
  font-size: 12px;
  font-weight: 600;
}

.action-chip small {
  color: #64748b;
  font-size: 11px;
  font-weight: 400;
}

.custom-action-modal {
  width: min(1040px, calc(100vw - 48px));
}

.action-modal-layout {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 16px;
  min-height: 520px;
}

.action-modal-list {
  display: grid;
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

.action-param-editor {
  display: grid;
  width: 100%;
  gap: 8px;
}

.action-param-row {
  display: grid;
  grid-template-columns: minmax(120px, 0.8fr) minmax(180px, 1.2fr) auto;
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
  display: grid;
  gap: 8px;
  max-height: 430px;
  overflow: auto;
  padding-right: 4px;
}

.selected-row {
  display: grid;
  grid-template-columns: 18px minmax(150px, 1fr) minmax(112px, 0.8fr) auto;
  align-items: start;
  gap: 8px;
  min-width: 0;
  padding: 10px;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  background: #fff;
  font-size: 12px;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
}

.selected-row.search,
.selected-row.table {
  grid-template-columns: 18px minmax(150px, 1fr) minmax(112px, 0.8fr) auto;
}

.field-setting-row {
  display: grid;
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

.field-help {
  grid-column: 1 / -1;
  padding: 7px 9px;
  border: 1px dashed #bfdbfe;
  border-radius: 6px;
  background: #eff6ff;
  color: #475569;
  font-size: 12px;
  line-height: 1.55;
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
  padding-top: 2px;
  color: #94a3b8;
  cursor: grab;
}

.f-name {
  display: grid;
  gap: 2px;
  min-width: 0;
  color: #0f172a;
  font-weight: 600;
}

.f-name small,
.available-item small {
  color: #64748b;
  font-size: 11px;
  font-weight: 400;
}

.f-code {
  min-width: 0;
  overflow: hidden;
  color: #94a3b8;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.f-remove {
  border: 0;
  background: transparent;
  color: #ef4444;
  font-size: 12px;
  cursor: pointer;
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

.source-code-textarea :deep(.n-input__textarea-el) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  line-height: 1.6;
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

  .side-rail-toggle-button {
    width: 100%;
    min-height: 42px;
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

  .canvas-toolbar :deep(.n-space) {
    flex-wrap: wrap;
  }

  .toolbar-info span {
    display: none;
  }

  .canvas-width-control,
  .canvas-zoom-control {
    padding: 2px 5px;
  }

  .action-modal-layout,
  .action-form-grid,
  .action-param-row {
    grid-template-columns: 1fr;
  }
}
</style>
