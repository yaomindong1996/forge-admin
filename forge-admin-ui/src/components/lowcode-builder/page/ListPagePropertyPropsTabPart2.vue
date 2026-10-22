<template>
            <div v-show="propertyPanelTab === 'props'" class="property-tab-content">
              <!-- Search / Table 字段配置 -->
              <template v-if="isFieldConfigurableBlock(selectedBlock.blockType)">
                <p v-if="selectedBlock.blockType === 'data-table'" class="property-inline-tip">
                  列表数据来自「数据」Tab 绑定的业务对象；下方只配展示字段。
                </p>
                <n-form-item :label="resolveFieldConfigLabel(selectedBlock.blockType)">
                  <n-button size="small" type="primary" secondary @click="openFieldDrawer('table')">
                    {{ selectedBlock.blockType === 'AiCrudPage' ? '配置列表字段' : '配置字段' }}（{{ selectedBlock.fieldRefs?.length || 0 }}/{{ fields.length }}）
                  </n-button>
                </n-form-item>
                <n-form-item v-if="showChildListDisplaySetting" label="子表行展示">
                  <div class="child-list-display">
                    <n-radio-group
                      :value="selectedBlock.props?.childListDisplayMode || 'aggregate'"
                      @update:value="patchBlockProps(selectedBlock.id, { childListDisplayMode: $event || 'aggregate' })"
                    >
                      <n-space vertical size="small">
                        <n-radio value="aggregate">
                          聚合到一行
                        </n-radio>
                        <n-radio value="expand">
                          拆成多条
                        </n-radio>
                      </n-space>
                    </n-radio-group>
                    <p class="field-help">
                      {{ resolveChildListDisplayHint(selectedBlock.props?.childListDisplayMode) }}
                    </p>
                  </div>
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
                <!-- 数据表单：列数/标签等常用项提到折叠外，避免被收进「接口与数据源」找不到 -->
                <template v-if="selectedBlock.blockType === 'AiForm'">
                  <p class="property-inline-tip">
                    先在「数据」Tab 绑业务对象生成字段；下方调列数与标签，高度随字段自适应。
                  </p>
                  <n-form-item label="表单列数">
                    <n-input-number
                      :value="selectedBlock.props?.gridCols || 2"
                      :min="1"
                      :max="4"
                      :show-button="false"
                      @update:value="patchBlockProps(selectedBlock.id, { gridCols: $event || 2 })"
                    />
                  </n-form-item>
                  <n-form-item label="标签位置 / 对齐 / 宽度">
                    <div class="style-grid three">
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
                      <n-input-number
                        :value="toNumberOrNull(selectedBlock.props?.labelWidth) ?? 100"
                        :min="0"
                        :max="260"
                        :show-button="false"
                        @update:value="patchBlockProps(selectedBlock.id, { labelWidth: $event ?? 100 })"
                      />
                    </div>
                  </n-form-item>
                  <n-form-item label="尺寸 / 行距 / 列距">
                    <div class="style-grid three">
                      <n-select
                        :value="selectedBlock.props?.size || 'medium'"
                        :options="componentSizeOptions"
                        @update:value="patchBlockProps(selectedBlock.id, { size: $event || 'medium' })"
                      />
                      <n-input-number
                        :value="selectedBlock.props?.yGap ?? 12"
                        :min="0"
                        :max="48"
                        :show-button="false"
                        placeholder="行距"
                        @update:value="patchBlockProps(selectedBlock.id, { yGap: $event ?? 12 })"
                      />
                      <n-input-number
                        :value="selectedBlock.props?.xGap ?? 12"
                        :min="0"
                        :max="48"
                        :show-button="false"
                        placeholder="列距"
                        @update:value="patchBlockProps(selectedBlock.id, { xGap: $event ?? 12 })"
                      />
                    </div>
                  </n-form-item>
                  <n-form-item label="提交接口">
                    <n-input
                      :value="selectedBlock.props?.createApi || defaultApiValues.createApi"
                      :placeholder="defaultApiValues.createApi"
                      @update:value="patchBlockProps(selectedBlock.id, { createApi: $event || '' })"
                    />
                  </n-form-item>
                  <n-form-item label="表单字段">
                    <n-button size="small" type="primary" secondary @click="openFieldDrawer('table')">
                      配置字段（{{ selectedBlock.fieldRefs?.length || 0 }}/{{ fields.length }}）
                    </n-button>
                  </n-form-item>
                </template>
                <template v-if="selectedBlock.blockType === 'AiCrudPage'">
                  <n-form-item label="空态标题">
                    <n-input
                      :value="selectedBlock.props?.emptyTitle || ''"
                      placeholder="暂无数据"
                      @update:value="patchBlockProps(selectedBlock.id, { emptyTitle: $event || '' })"
                    />
                  </n-form-item>
                  <n-form-item label="空态描述">
                    <n-input
                      :value="selectedBlock.props?.emptyDescription || selectedBlock.props?.emptyText || ''"
                      placeholder="当前列表还没有记录"
                      @update:value="patchBlockProps(selectedBlock.id, { emptyDescription: $event || '', emptyText: $event || '' })"
                    />
                  </n-form-item>
                </template>
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
                    <template v-else-if="selectedBlock.blockType === 'AiForm'">
                      <n-form-item label="提交接口（可覆盖上方）">
                        <n-input
                          :value="selectedBlock.props?.createApi || defaultApiValues.createApi"
                          :placeholder="defaultApiValues.createApi"
                          @update:value="patchBlockProps(selectedBlock.id, { createApi: $event || '' })"
                        />
                      </n-form-item>
                      <n-form-item label="折叠时最多显示字段数">
                        <n-input-number
                          :value="selectedBlock.props?.maxVisibleFields ?? 40"
                          :min="1"
                          :max="80"
                          :show-button="false"
                          @update:value="patchBlockProps(selectedBlock.id, { maxVisibleFields: $event || 40 })"
                        />
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
                                  <span>
                                    <em class="bitable-field-scope" :class="isChildListField(element) ? 'is-child' : 'is-main'">
                                      {{ isChildListField(element) ? '子表' : '主表' }}
                                    </em>
                                    {{ resolveCrudFieldInlineTitle(element) }}
                                  </span>
                                  <small>{{ isChildListField(element) ? (element.modelName || element.sourceLabel || '子表') : '主表' }} · {{ element.sourceField || element.field }}</small>
                                </div>
                                <button
                                  type="button"
                                  class="bitable-field-icon-button"
                                  :title="isCrudTableFieldVisible(element.field) ? '隐藏字段' : '显示字段'"
                                  @click="toggleCrudTableField(element.field, !isCrudTableFieldVisible(element.field))"
                                >
                                  <n-icon>
                                    <component :is="isCrudTableFieldVisible(element.field) ? 'EyeOutline' : 'BitableInvisibleIcon'" />
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
                        <div v-if="customActionsEditable" class="bitable-field-operation-divider" />
                        <div v-if="customActionsEditable" class="bitable-field-operation-group">
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
                        <div v-if="customActionsEditable" class="bitable-operation-list">
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
                                    <component :is="element.visible === false ? 'BitableInvisibleIcon' : 'BitableVisibleIcon'" />
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
                                                    <component :is="element.selected ? 'BitableVisibleIcon' : 'BitableInvisibleIcon'" />
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
                    <!-- 自由布局 panel-only：区块即表单 chrome 编辑入口；对象列表设计器仍只读摘要 -->
                    <template v-if="panelOnly">
                      <div class="form-modal-help">
                        自由布局下打开方式与弹窗宽度写在当前 CRUD 组件上，运行时优先于对象默认配置。
                      </div>
                      <n-form-item label="打开方式">
                        <n-select
                          :value="selectedAiCrudFormModalProps.formOpenMode || selectedAiCrudFormModalProps.modalType || 'modal'"
                          :options="formOpenModeOptions"
                          size="small"
                          @update:value="patchBlockProps(selectedBlock.id, {
                            formOpenMode: $event || 'modal',
                            modalType: ['modal', 'drawer'].includes($event) ? $event : (selectedBlock.props?.modalType || 'modal'),
                          })"
                        />
                      </n-form-item>
                      <n-form-item v-if="(selectedAiCrudFormModalProps.formOpenMode || 'modal') === 'drawer'" label="抽屉方向">
                        <n-select
                          :value="selectedAiCrudFormModalProps.drawerPlacement || 'right'"
                          :options="drawerPlacementOptions"
                          size="small"
                          @update:value="patchBlockProps(selectedBlock.id, { drawerPlacement: $event || 'right' })"
                        />
                      </n-form-item>
                      <n-form-item label="弹窗宽度">
                        <n-input
                          :value="selectedAiCrudFormModalProps.modalWidth || '800px'"
                          size="small"
                          placeholder="800px / 80vw"
                          @update:value="patchBlockProps(selectedBlock.id, { modalWidth: $event || '800px' })"
                        />
                      </n-form-item>
                    </template>
                    <template v-else>
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
                    </template>
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
                          新增单独显示；批量删除、导入、导出、导出任务收进工具栏「更多」。自定义查询是旁边的独立入口，需要能从列表接口解析出对象编码。若「查询与列表字段 → 显示项」关掉了工具栏，整排都不会出现。
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
                    <n-form-item v-if="customActionsEditable" label="工具栏按钮">
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
                        :value="selectedBlock.props?.yGap ?? 12"
                        :min="0"
                        :max="48"
                        :show-button="false"
                        placeholder="行距"
                        @update:value="patchBlockProps(selectedBlock.id, { yGap: $event ?? 12 })"
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
                <n-divider v-if="customActionsEditable">
                  自定义操作按钮
                </n-divider>
                <div v-if="customActionsEditable" class="custom-action-builder">
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
                <div class="tree-panel-property">
                  <div class="tree-panel-property-intro">
                    左侧树通常来自分类、组织等<strong>另一个对象</strong>，不必用当前列表对象。选好数据源后，右侧列表仍按本对象字段过滤。
                  </div>
                  <n-form-item label="树数据来源">
                    <n-select
                      :value="selectedTreeSourceValue"
                      :options="treeSourceOptions"
                      filterable
                      clearable
                      :loading="treeSourceLoading"
                      placeholder="选择树对象，一般不是当前列表"
                      @update:value="handleTreeSourceChange"
                    />
                  </n-form-item>
                  <n-form-item label="树标题">
                    <n-input
                      :value="selectedBlock.props?.treeTitle"
                      placeholder="例如：组织树、分类树"
                      @update:value="patchBlockProps(selectedBlock.id, { treeTitle: $event })"
                    />
                  </n-form-item>
                  <n-form-item label="树节点主键">
                    <n-select
                      :value="selectedBlock.props?.keyField"
                      :options="treeFieldOptions"
                      filterable
                      clearable
                      :loading="treeSourceFieldsLoading"
                      @update:value="patchBlockProps(selectedBlock.id, { keyField: $event })"
                    />
                  </n-form-item>
                  <n-form-item label="树父级字段">
                    <n-select
                      :value="selectedBlock.props?.parentField"
                      :options="treeFieldOptions"
                      filterable
                      clearable
                      :loading="treeSourceFieldsLoading"
                      @update:value="patchBlockProps(selectedBlock.id, { parentField: $event })"
                    />
                  </n-form-item>
                  <n-form-item label="树显示字段">
                    <n-select
                      :value="selectedBlock.props?.labelField"
                      :options="treeFieldOptions"
                      filterable
                      clearable
                      :loading="treeSourceFieldsLoading"
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
                  <n-form-item>
                    <template #label>
                      <span class="tree-panel-property-label">
                        节点取值字段
                        <n-tooltip trigger="hover" placement="top" :style="{ maxWidth: '280px' }">
                          <template #trigger>
                            <n-icon :size="13" class="tree-panel-property-tip">
                              <HelpCircleOutline />
                            </n-icon>
                          </template>
                          点击树节点时，把这个字段的值传给右侧列表。
                        </n-tooltip>
                      </span>
                    </template>
                    <n-select
                      :value="selectedBlock.props?.targetField"
                      :options="treeFieldOptions"
                      filterable
                      clearable
                      :loading="treeSourceFieldsLoading"
                      @update:value="patchBlockProps(selectedBlock.id, { targetField: $event })"
                    />
                  </n-form-item>
                  <n-form-item>
                    <template #label>
                      <span class="tree-panel-property-label">
                        右表过滤字段
                        <n-tooltip trigger="hover" placement="top" :style="{ maxWidth: '320px' }">
                          <template #trigger>
                            <n-icon :size="13" class="tree-panel-property-tip">
                              <HelpCircleOutline />
                            </n-icon>
                          </template>
                          该字段属于当前列表对象。树接口按所选数据源独立加载；点节点后右表按此字段筛选，清除节点即可看全部数据。
                        </n-tooltip>
                      </span>
                    </template>
                    <n-select
                      :value="selectedBlock.props?.filterField"
                      :options="primaryFieldOptions"
                      filterable
                      clearable
                      @update:value="patchBlockProps(selectedBlock.id, { filterField: $event })"
                    />
                  </n-form-item>
                  <n-form-item v-if="layoutType === 'tree-crud'" label="树表操作">
                    <div class="tree-action-compact">
                      <div>
                        <strong>添加下级</strong>
                        <span>在右侧行上新增子节点，写入父级字段</span>
                      </div>
                      <n-switch
                        size="small"
                        :value="treeAddChildEnabled"
                        @update:value="updateTreeAddChildEnabled"
                      />
                    </div>
                  </n-form-item>
                </div>
              </template>

              <!-- detail-info 在下方专用区挂同一套编辑器（关槽位映射 + 填入对象接口） -->
              <template v-if="localDataBindableBlockTypes.includes(selectedBlock.blockType) && selectedBlock.blockType !== 'detail-info'">
                <n-form-item label="数据来源">
                  <WidgetDataBindingEditor
                    :model-value="selectedBlock.props?.dataBinding || {}"
                    :block-type="selectedBlock.blockType"
                    :fields="fields"
                    :form-designer-schema="formDesignerSchema"
                    @update:model-value="updateWidgetDataBinding"
                  />
                </n-form-item>
              </template>

              <!-- 指标卡片 -->
              <StatsStripPropertyPanel
                v-if="selectedBlock.blockType === 'stats-strip'"
                :block="selectedBlock"
                :page-options="pageTargetOptions"
                :list-bound="isStatsStripListMode(selectedBlock)"
                @patch-props="patchBlockProps(selectedBlock.id, $event)"
              />

              <!-- 快捷入口 -->
              <QuickEntryPropertyPanel
                v-if="selectedBlock.blockType === 'quick-entry'"
                :block="selectedBlock"
                :page-options="pageTargetOptions"
                :list-bound="isQuickEntryListMode(selectedBlock)"
                @patch-props="patchBlockProps(selectedBlock.id, $event)"
              />

              <!-- 柱状图 / 折线图 / 面积图 / 饼图 -->
              <PageChartPropertyPanel
                v-if="['bar-chart', 'line-chart', 'area-chart', 'pie-chart'].includes(selectedBlock.blockType)"
                :block="selectedBlock"
                :list-bound="isChartListMode(selectedBlock)"
                @patch-props="patchBlockProps(selectedBlock.id, $event)"
                @update-categories="updateChartCategories"
                @update-series-item="(idx, patch) => updateChartSeriesItem(idx, patch)"
                @add-series="addChartSeriesItem"
                @remove-series="removeChartSeriesItem"
              />

              <WorkflowTaskListPropertyPanel
                v-if="selectedBlock.blockType === 'workflow-task-list'"
                :block="selectedBlock"
                @patch-props="patchBlockProps(selectedBlock.id, $event)"
              />

              <NoticeBoardPropertyPanel
                v-if="selectedBlock.blockType === 'notice-board'"
                :block="selectedBlock"
                @patch-props="patchBlockProps(selectedBlock.id, $event)"
              />

              <WelcomeBannerPropertyPanel
                v-if="selectedBlock.blockType === 'welcome-banner'"
                :block="selectedBlock"
                @patch-props="patchBlockProps(selectedBlock.id, $event)"
              />

              <CardPropertyPanel
                v-if="selectedBlock.blockType === 'card'"
                :block="selectedBlock"
                @patch-props="patchBlockProps(selectedBlock.id, $event)"
              />

              <PanelBoxPropertyPanel
                v-if="selectedBlock.blockType === 'panel-box'"
                :block="selectedBlock"
                @patch-props="patchBlockProps(selectedBlock.id, $event)"
              />

              <template v-if="selectedBlock.blockType === 'detail-info'">
                <p class="property-inline-tip">
                  选「远程接口」填地址；地址栏
                  <code v-pre>?id=</code>
                  替换
                  <code v-pre>{{ id }}</code>。
                </p>
                <n-form-item label="详情字段">
                  <n-button size="small" type="primary" secondary @click="openFieldDrawer('table')">
                    配置字段（{{ selectedBlock.fieldRefs?.length || 0 }}/{{ fields.length }}）
                  </n-button>
                </n-form-item>
                <n-form-item label="数据来源">
                  <WidgetDataBindingEditor
                    :model-value="resolveDetailInfoBinding(selectedBlock)"
                    block-type="detail-info"
                    :fields="fields"
                    :form-designer-schema="formDesignerSchema"
                    :show-mapping-fields="false"
                    hide-intro-help
                    @update:model-value="updateDetailInfoDataBinding"
                  />
                </n-form-item>
                <div class="detail-binding-actions">
                  <n-button
                    size="small"
                    secondary
                    type="primary"
                    :disabled="!detailInfoDefaultApi"
                    @click="applyDetailInfoDefaultApi"
                  >
                    填入对象接口
                  </n-button>
                </div>
                <n-form-item label="描述标题">
                  <div class="style-grid two">
                    <n-input
                      :value="selectedBlock.props?.title || '详情信息'"
                      placeholder="标题文案"
                      @update:value="patchBlockProps(selectedBlock.id, { title: $event || '详情信息' })"
                    />
                    <n-switch
                      :value="selectedBlock.props?.showTitle !== false"
                      @update:value="patchBlockProps(selectedBlock.id, { showTitle: $event })"
                    >
                      <template #checked>
                        显示标题
                      </template>
                      <template #unchecked>
                        隐藏标题
                      </template>
                    </n-switch>
                  </div>
                </n-form-item>
                <n-form-item label="Descriptions 布局">
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
                      <span>标签位置</span>
                      <n-select
                        :value="selectedBlock.props?.labelPlacement || 'left'"
                        :options="labelPlacementOptions"
                        @update:value="patchBlockProps(selectedBlock.id, { labelPlacement: $event || 'left' })"
                      />
                    </label>
                    <label class="detail-layout-control">
                      <span>标签对齐</span>
                      <n-select
                        :value="selectedBlock.props?.labelAlign || 'left'"
                        :options="labelAlignOptions"
                        @update:value="patchBlockProps(selectedBlock.id, { labelAlign: $event || 'left' })"
                      />
                    </label>
                    <label class="detail-layout-control">
                      <span>尺寸</span>
                      <n-select
                        :value="selectedBlock.props?.size || 'medium'"
                        :options="componentSizeOptions"
                        @update:value="patchBlockProps(selectedBlock.id, { size: $event || 'medium' })"
                      />
                    </label>
                    <label class="detail-layout-control">
                      <span>标签宽度</span>
                      <n-input-number
                        :value="toNumberOrNull(selectedBlock.props?.labelWidth) ?? 100"
                        :min="60"
                        :max="240"
                        :show-button="false"
                        @update:value="patchBlockProps(selectedBlock.id, { labelWidth: $event ?? 100 })"
                      />
                    </label>
                    <label class="detail-layout-control">
                      <span>分隔符</span>
                      <n-input
                        :value="selectedBlock.props?.separator ?? ':'"
                        placeholder=":"
                        :disabled="selectedBlock.props?.bordered !== false"
                        @update:value="patchBlockProps(selectedBlock.id, { separator: $event ?? ':' })"
                      />
                    </label>
                    <label class="detail-layout-switch">
                      <span>
                        <strong>显示边框</strong>
                        <small>表格边框样式（Descriptions bordered）</small>
                      </span>
                      <n-switch
                        :value="selectedBlock.props?.bordered !== false"
                        @update:value="patchBlockProps(selectedBlock.id, { bordered: $event })"
                      />
                    </label>
                    <label class="detail-layout-switch">
                      <span>
                        <strong>内容换行</strong>
                        <small>关闭后长文本单行省略</small>
                      </span>
                      <n-switch
                        :value="selectedBlock.props?.contentWrap !== false"
                        @update:value="patchBlockProps(selectedBlock.id, { contentWrap: $event })"
                      />
                    </label>
                  </div>
                </n-form-item>
              </template>

              <InfoPanelPropertyPanel
                v-if="selectedBlock.blockType === 'info-panel'"
                :block="selectedBlock"
                :page-options="pageTargetOptions"
                @patch-props="patchBlockProps(selectedBlock.id, $event)"
              />

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

            </div>

</template>

<script>
import { useListPageDesignerApi } from './listPageDesignerContext'
import { listPageDesignerLocalComponents } from './listPageDesignerLocalComponents'

export default {
  components: { ...listPageDesignerLocalComponents },
  name: 'ListPagePropertyPropsTabPart2',
  setup() {
    // 禁止展开 inject Proxy：展开会把 propertyPanelTab 等解成一次性快照，
    // 切到「样式/交互」后「属性」区仍显示（如数据表排序/行间距）。
    return useListPageDesignerApi()
  },
}
</script>

<style scoped src="./list-page-grid-designer-shell.css"></style>
<style scoped src="./list-page-grid-designer-panels.css"></style>
