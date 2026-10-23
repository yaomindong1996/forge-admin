<template>
<n-form label-placement="top" :show-feedback="false" class="property-form">
            <n-collapse v-model:expanded-names="selectedBasicExpandedNames" class="config-collapse">
              <n-collapse-item name="identity">
                <template #header>
                  <span class="collapse-section-title">
                    <i class="i-material-symbols:badge-outline" />
                    标识
                  </span>
                </template>
                <section class="panel-item">
                  <n-form-item label="显示名称">
                    <n-input :value="selectedComponent.label" placeholder="请输入" @update:value="updateLabel" />
                  </n-form-item>
                  <n-form-item v-if="canSwitchComponentType" label="组件类型">
                    <n-select
                      :value="selectedComponent.componentKey"
                      :options="switchableComponentOptions"
                      :disabled="fieldStructureLocked"
                      filterable
                      @update:value="handleSwitchComponentType"
                    />
                    <span v-if="fieldStructureLocked" class="property-help">该字段已有业务数据，不能修改组件或存储类型；画布中删除该组件不影响已有数据，字段仍可从左侧字段列表重新拖入。</span>
                  </n-form-item>
                  <n-form-item v-if="isField" label="绑定字段">
                    <n-input
                      :value="selectedComponent.fieldBinding?.fieldCode || ''"
                      :disabled="fieldStructureLocked"
                      clearable
                      placeholder="请输入字段编码"
                      @update:value="updateFieldBindingCode"
                    />
                  </n-form-item>
                  <n-form-item v-if="isCrudBlock" label="区块说明">
                    <n-input
                      :value="selectedComponent.props?.description"
                      type="textarea"
                      :autosize="{ minRows: 2, maxRows: 3 }"
                      clearable
                      @update:value="updateComponent({ props: { description: $event } })"
                    />
                  </n-form-item>
                  <template v-if="isPageWidget">
                    <n-form-item label="组件标题">
                      <n-input
                        :value="selectedComponent.props?.title || selectedComponent.label"
                        clearable
                        @update:value="updateComponent({ props: { title: $event } })"
                      />
                    </n-form-item>
                    <n-form-item v-if="dataBindablePageWidgetKeys.includes(selectedComponent.componentKey)" label="数据来源">
                      <div class="data-source-editor">
                        <div class="property-help">
                          选择组件内容从哪里来。静态配置使用当前属性；当前表单/详情数据从已加载记录取值；远程接口会请求接口后再按字段映射渲染。
                        </div>
                        <div class="data-source-row">
                          <span>来源类型</span>
                          <n-select
                            :value="selectedComponent.props?.dataBinding?.sourceType || 'static'"
                            :options="widgetDataSourceOptions"
                            @update:value="updatePageWidgetDataBinding({ enabled: $event !== 'static', sourceType: $event || 'static' })"
                          />
                        </div>
                        <div v-if="selectedComponent.props?.dataBinding?.sourceType === 'context'" class="data-source-row">
                          <span>取值路径</span>
                          <n-input
                            :value="selectedComponent.props?.dataBinding?.contextPath || ''"
                            clearable
                            placeholder="不填表示整条当前记录；例如 customer.name"
                            @update:value="updatePageWidgetDataBinding({ contextPath: $event || '' })"
                          />
                        </div>
                        <template v-if="selectedComponent.props?.dataBinding?.sourceType === 'remote'">
                          <div class="data-source-row">
                            <span>接口地址</span>
                            <n-input
                              :value="selectedComponent.props?.dataBinding?.api || ''"
                              clearable
                              placeholder="例如 /api/order/detail/{{ id }}"
                              @update:value="updatePageWidgetDataBinding({ api: $event || '' })"
                            />
                          </div>
                          <div class="data-source-row">
                            <span>请求方式</span>
                            <n-select
                              :value="selectedComponent.props?.dataBinding?.method || 'get'"
                              :options="requestMethodOptions"
                              @update:value="updatePageWidgetDataBinding({ method: $event || 'get' })"
                            />
                          </div>
                          <div class="data-source-row">
                            <span>响应路径</span>
                            <n-input
                              :value="selectedComponent.props?.dataBinding?.dataPath || 'data'"
                              clearable
                              placeholder="如 data、data.records"
                              @update:value="updatePageWidgetDataBinding({ dataPath: $event || 'data' })"
                            />
                          </div>
                          <div class="data-source-row">
                            <span>请求参数</span>
                            <n-input
                              :value="selectedComponent.props?.dataBinding?.paramsText || '{}'"
                              type="textarea"
                              :rows="3"
                              placeholder="固定参数 JSON，如 {&quot;type&quot;:&quot;user&quot;}；引用表单字段值写 ${字段名}，如 {&quot;deptId&quot;:&quot;${deptId}&quot;}"
                              @update:value="updatePageWidgetDataBinding({ paramsText: $event || '{}' })"
                            />
                          </div>
                          <div v-if="selectedComponent.props?.dataBinding?.sourceType === 'remote'" class="data-source-row">
                            <span>引用字段</span>
                            <div class="param-ref-editor">
                              <n-input
                                :value="widgetParamRefName"
                                size="small"
                                clearable
                                placeholder="参数名，如 deptId"
                                @update:value="widgetParamRefName = $event || ''"
                              />
                              <n-select
                                :value="widgetParamRefField"
                                :options="optionLinkageSourceFieldOptions"
                                size="small"
                                filterable
                                clearable
                                placeholder="选字段，值变化后自动重新查询"
                                @update:value="widgetParamRefField = $event || ''"
                              />
                              <n-button size="small" :disabled="!widgetParamRefName || !widgetParamRefField" @click="addWidgetParamRef">
                                写入
                              </n-button>
                            </div>
                            <div class="property-help">
                              把所选字段的当前值作为参数传给接口，字段值变化后组件自动重新查询（任意组件间级联，不限于下拉）。
                            </div>
                          </div>
                        </template>
                        <div v-if="selectedComponent.props?.dataBinding?.sourceType !== 'static'" class="property-help">
                          字段映射用于告诉组件接口返回的字段含义。列表/标签/步骤类组件常用“显示文本、值、标题、描述”；单值组件通常只需要“值字段”或“内容字段”。
                        </div>
                        <div v-if="selectedComponent.props?.dataBinding?.sourceType !== 'static'" class="data-source-mapping-grid">
                          <div class="data-source-row">
                            <span>显示文本</span>
                            <n-input
                              :value="selectedComponent.props?.dataBinding?.labelField || 'label'"
                              placeholder="label / name"
                              @update:value="updatePageWidgetDataBinding({ labelField: $event || 'label' })"
                            />
                          </div>
                          <div class="data-source-row">
                            <span>值字段</span>
                            <n-input
                              :value="selectedComponent.props?.dataBinding?.valueField || 'value'"
                              placeholder="value / id / src"
                              @update:value="updatePageWidgetDataBinding({ valueField: $event || 'value' })"
                            />
                          </div>
                          <div class="data-source-row">
                            <span>标题字段</span>
                            <n-input
                              :value="selectedComponent.props?.dataBinding?.titleField || 'title'"
                              placeholder="title / text"
                              @update:value="updatePageWidgetDataBinding({ titleField: $event || 'title' })"
                            />
                          </div>
                          <div class="data-source-row">
                            <span>描述字段</span>
                            <n-input
                              :value="selectedComponent.props?.dataBinding?.descriptionField || 'description'"
                              placeholder="description / content"
                              @update:value="updatePageWidgetDataBinding({ descriptionField: $event || 'description' })"
                            />
                          </div>
                        </div>
                      </div>
                    </n-form-item>
                    <template v-if="selectedComponent.componentKey === 'transfer'">
                      <n-form-item label="数据来源">
                        <n-select
                          :value="selectedComponent.props?.dataSourceType || 'static'"
                          :options="transferDataSourceOptions"
                          @update:value="updateComponent({ props: { dataSourceType: $event || 'static' } })"
                        />
                      </n-form-item>
                      <n-form-item label="分栏标题">
                        <div class="option-editor-row two-columns">
                          <n-input
                            :value="selectedComponent.props?.sourceTitle || '可选项'"
                            size="small"
                            placeholder="左侧标题"
                            @update:value="updateComponent({ props: { sourceTitle: $event || '可选项' } })"
                          />
                          <n-input
                            :value="selectedComponent.props?.targetTitle || '已选项'"
                            size="small"
                            placeholder="右侧标题"
                            @update:value="updateComponent({ props: { targetTitle: $event || '已选项' } })"
                          />
                        </div>
                      </n-form-item>
                      <n-form-item v-if="selectedComponent.props?.dataSourceType === 'remote'" label="远程接口">
                        <div class="page-widget-config-stack">
                          <n-input
                            :value="selectedComponent.props?.optionSource?.api || ''"
                            placeholder="例如 get@/api/system/user/options"
                            @update:value="updatePageWidgetOptionSource({ api: $event })"
                          />
                          <div class="option-editor-row two-columns">
                            <n-select
                              :value="selectedComponent.props?.optionSource?.method || 'get'"
                              :options="requestMethodOptions"
                              @update:value="updatePageWidgetOptionSource({ method: $event || 'get' })"
                            />
                            <n-input
                              :value="selectedComponent.props?.optionSource?.recordsField || 'records'"
                              placeholder="列表路径"
                              @update:value="updatePageWidgetOptionSource({ recordsField: $event || 'records' })"
                            />
                          </div>
                          <div class="option-editor-row two-columns">
                            <n-input
                              :value="selectedComponent.props?.optionSource?.labelField || 'label'"
                              placeholder="显示字段"
                              @update:value="updatePageWidgetOptionSource({ labelField: $event || 'label' })"
                            />
                            <n-input
                              :value="selectedComponent.props?.optionSource?.valueField || 'value'"
                              placeholder="值字段"
                              @update:value="updatePageWidgetOptionSource({ valueField: $event || 'value' })"
                            />
                          </div>
                          <n-input
                            :value="selectedComponent.props?.optionSource?.paramsText || '{}'"
                            placeholder="请求参数 JSON"
                            @update:value="updatePageWidgetOptionSource({ paramsText: $event || '{}' })"
                          />
                        </div>
                      </n-form-item>
                    </template>
                    <template v-if="selectedComponent.componentKey === 'watermark'">
                      <n-form-item label="水印文字">
                        <n-input
                          :value="selectedComponent.props?.content || ''"
                          type="textarea"
                          :autosize="{ minRows: 2, maxRows: 4 }"
                          placeholder="支持多行文本"
                          @update:value="updateComponent({ props: { content: $event } })"
                        />
                      </n-form-item>
                      <n-form-item label="字体 / 行高 / 字重">
                        <div class="option-editor-row three-columns">
                          <n-input-number
                            :value="selectedComponent.props?.fontSize || 14"
                            size="small"
                            :min="8"
                            :max="72"
                            :show-button="false"
                            @update:value="updateComponent({ props: { fontSize: $event || 14 } })"
                          />
                          <n-input-number
                            :value="selectedComponent.props?.lineHeight || 14"
                            size="small"
                            :min="8"
                            :max="96"
                            :show-button="false"
                            @update:value="updateComponent({ props: { lineHeight: $event || 14 } })"
                          />
                          <n-input-number
                            :value="selectedComponent.props?.fontWeight || 400"
                            size="small"
                            :min="100"
                            :max="900"
                            :step="100"
                            :show-button="false"
                            @update:value="updateComponent({ props: { fontWeight: $event || 400 } })"
                          />
                        </div>
                      </n-form-item>
                      <n-form-item label="颜色 / 样式 / 对齐">
                        <div class="option-editor-row three-columns">
                          <n-color-picker
                            :value="selectedComponent.props?.fontColor || 'rgba(128, 128, 128, .3)'"
                            :show-alpha="true"
                            @update:value="updateComponent({ props: { fontColor: $event || 'rgba(128, 128, 128, .3)' } })"
                          />
                          <n-select
                            :value="selectedComponent.props?.fontStyle || 'normal'"
                            :options="watermarkFontStyleOptions"
                            @update:value="updateComponent({ props: { fontStyle: $event || 'normal' } })"
                          />
                          <n-select
                            :value="selectedComponent.props?.textAlign || 'left'"
                            :options="watermarkTextAlignOptions"
                            @update:value="updateComponent({ props: { textAlign: $event || 'left' } })"
                          />
                        </div>
                      </n-form-item>
                      <n-form-item label="宽高 / 旋转">
                        <div class="option-editor-row three-columns">
                          <n-input-number :value="selectedComponent.props?.width || 32" size="small" :min="1" :max="600" :show-button="false" @update:value="updateComponent({ props: { width: $event || 32 } })" />
                          <n-input-number :value="selectedComponent.props?.height || 32" size="small" :min="1" :max="600" :show-button="false" @update:value="updateComponent({ props: { height: $event || 32 } })" />
                          <n-input-number :value="selectedComponent.props?.rotate || 0" size="small" :min="-180" :max="180" :show-button="false" @update:value="updateComponent({ props: { rotate: $event || 0 } })" />
                        </div>
                      </n-form-item>
                      <n-form-item label="间隔 / 偏移">
                        <div class="option-editor-row three-columns">
                          <n-input-number :value="selectedComponent.props?.xGap || 0" size="small" :min="0" :max="600" :show-button="false" @update:value="updateComponent({ props: { xGap: $event || 0 } })" />
                          <n-input-number :value="selectedComponent.props?.yGap || 0" size="small" :min="0" :max="600" :show-button="false" @update:value="updateComponent({ props: { yGap: $event || 0 } })" />
                          <n-input-number :value="selectedComponent.props?.zIndex || 10" size="small" :min="0" :max="9999" :show-button="false" @update:value="updateComponent({ props: { zIndex: $event || 10 } })" />
                        </div>
                      </n-form-item>
                      <n-form-item label="图片水印">
                        <div class="page-widget-config-stack">
                          <n-input :value="selectedComponent.props?.image || ''" clearable placeholder="图片 URL" @update:value="updateComponent({ props: { image: $event } })" />
                          <div class="option-editor-row three-columns">
                            <n-input-number :value="selectedComponent.props?.imageWidth" size="small" :min="1" :max="600" :show-button="false" placeholder="图片宽" @update:value="updateComponent({ props: { imageWidth: $event || undefined } })" />
                            <n-input-number :value="selectedComponent.props?.imageHeight" size="small" :min="1" :max="600" :show-button="false" placeholder="图片高" @update:value="updateComponent({ props: { imageHeight: $event || undefined } })" />
                            <n-input-number :value="selectedComponent.props?.imageOpacity ?? 1" size="small" :min="0" :max="1" :step="0.1" :show-button="false" @update:value="updateComponent({ props: { imageOpacity: $event ?? 1 } })" />
                          </div>
                        </div>
                      </n-form-item>
                      <n-form-item label="开关">
                        <n-checkbox-group :value="resolveBooleanKeys(selectedComponent.props, ['cross', 'debug', 'fullscreen', 'selectable'])" @update:value="values => updateComponent({ props: { cross: values.includes('cross'), debug: values.includes('debug'), fullscreen: values.includes('fullscreen'), selectable: values.includes('selectable') } })">
                          <n-space size="small">
                            <n-checkbox value="cross" label="跨边界" />
                            <n-checkbox value="debug" label="调试" />
                            <n-checkbox value="fullscreen" label="全屏" />
                            <n-checkbox value="selectable" label="内容可选" />
                          </n-space>
                        </n-checkbox-group>
                      </n-form-item>
                    </template>
                    <n-form-item v-if="selectedComponent.componentKey === 'rich-text'" label="富文本 HTML">
                      <n-input
                        :value="selectedComponent.props?.content"
                        type="textarea"
                        :autosize="{ minRows: 5, maxRows: 10 }"
                        @update:value="updateComponent({ props: { content: $event } })"
                      />
                    </n-form-item>
                    <n-form-item v-if="selectedComponent.componentKey === 'markdown'" label="Markdown 源码">
                      <n-input
                        :value="selectedComponent.props?.content"
                        type="textarea"
                        :autosize="{ minRows: 6, maxRows: 12 }"
                        @update:value="updateComponent({ props: { content: $event } })"
                      />
                    </n-form-item>
                    <template v-if="['barcode', 'qrcode'].includes(selectedComponent.componentKey)">
                      <n-form-item label="编码内容">
                        <div class="option-editor-row two-columns">
                          <n-input
                            :value="selectedComponent.props?.value || ''"
                            size="small"
                            placeholder="编码内容"
                            @update:value="updateComponent({ props: { value: $event } })"
                          />
                          <n-color-picker
                            :value="selectedComponent.props?.foreground || selectedComponent.props?.lineColor || '#0f172a'"
                            :show-alpha="true"
                            @update:value="updateCodeColor($event)"
                          />
                        </div>
                      </n-form-item>
                      <n-form-item v-if="selectedComponent.componentKey === 'barcode'" label="条码格式 / 尺寸">
                        <div class="option-editor-row three-columns">
                          <n-select
                            :value="selectedComponent.props?.format || 'CODE128'"
                            :options="barcodeFormatOptions"
                            @update:value="updateComponent({ props: { format: $event || 'CODE128' } })"
                          />
                          <n-input-number
                            :value="selectedComponent.props?.barHeight || 72"
                            size="small"
                            :min="24"
                            :max="240"
                            :show-button="false"
                            @update:value="updateComponent({ props: { barHeight: $event || 72 } })"
                          />
                          <n-switch
                            :value="selectedComponent.props?.showText !== false"
                            @update:value="updateComponent({ props: { showText: $event } })"
                          />
                        </div>
                      </n-form-item>
                      <n-form-item v-if="selectedComponent.componentKey === 'qrcode'" label="二维码样式">
                        <div class="option-editor-row three-columns">
                          <n-input-number
                            :value="selectedComponent.props?.size || 132"
                            size="small"
                            :min="64"
                            :max="480"
                            :show-button="false"
                            @update:value="updateComponent({ props: { size: $event || 132 } })"
                          />
                          <n-select
                            :value="selectedComponent.props?.errorCorrectionLevel || 'Q'"
                            :options="qrcodeErrorCorrectionOptions"
                            @update:value="updateComponent({ props: { errorCorrectionLevel: $event || 'Q' } })"
                          />
                          <n-switch
                            :value="selectedComponent.props?.showText !== false"
                            @update:value="updateComponent({ props: { showText: $event } })"
                          />
                        </div>
                      </n-form-item>
                    </template>
                    <n-form-item v-if="selectedComponent.componentKey === 'html-tag'" label="HTML 内容">
                      <n-input
                        :value="selectedComponent.props?.htmlContent || selectedComponent.props?.textContent"
                        type="textarea"
                        :autosize="{ minRows: 5, maxRows: 10 }"
                        @update:value="updateComponent({ props: { htmlContent: $event } })"
                      />
                    </n-form-item>
                    <n-form-item v-if="selectedComponent.componentKey === 'vue-component'" label="Vue Template">
                      <n-input
                        :value="selectedComponent.props?.templateCode"
                        type="textarea"
                        :autosize="{ minRows: 5, maxRows: 10 }"
                        @update:value="updateComponent({ props: { templateCode: $event } })"
                      />
                    </n-form-item>
                    <n-form-item v-if="selectedComponent.componentKey === 'vue-component'" label="预览模式">
                      <div class="option-editor-row two-columns">
                        <n-select
                          :value="selectedComponent.props?.previewMode || 'safe-template'"
                          :options="vuePreviewModeOptions"
                          @update:value="updateComponent({ props: { previewMode: $event || 'safe-template', safeMode: $event === 'live' ? false : selectedComponent.props?.safeMode !== false } })"
                        />
                        <n-switch
                          :value="selectedComponent.props?.safeMode !== false"
                          @update:value="updateComponent({ props: { safeMode: $event, previewMode: $event ? 'safe-template' : selectedComponent.props?.previewMode || 'live' } })"
                        />
                      </div>
                    </n-form-item>
                    <n-form-item v-if="selectedComponent.componentKey === 'vue-component'" label="Script / Style">
                      <div class="page-widget-config-stack">
                        <n-input
                          :value="selectedComponent.props?.scriptCode"
                          type="textarea"
                          :autosize="{ minRows: 3, maxRows: 8 }"
                          @update:value="updateComponent({ props: { scriptCode: $event } })"
                        />
                        <n-input
                          :value="selectedComponent.props?.styleCode"
                          type="textarea"
                          :autosize="{ minRows: 3, maxRows: 8 }"
                          @update:value="updateComponent({ props: { styleCode: $event } })"
                        />
                      </div>
                    </n-form-item>
                    <n-form-item v-if="selectedComponent.componentKey === 'vue-component'" label="Props JSON">
                      <n-input
                        :value="selectedComponent.props?.propsJson"
                        type="textarea"
                        :autosize="{ minRows: 3, maxRows: 8 }"
                        @update:value="updateComponent({ props: { propsJson: $event } })"
                      />
                    </n-form-item>
                    <template v-if="['calendar', 'code', 'countdown', 'descriptions', 'announcement', 'list', 'log', 'number-animation', 'breadcrumb', 'menu', 'pagination', 'split'].includes(selectedComponent.componentKey)">
                      <n-form-item v-if="['code', 'log'].includes(selectedComponent.componentKey)" label="内容">
                        <n-input
                          :value="selectedComponent.props?.code || selectedComponent.props?.log || ''"
                          type="textarea"
                          :autosize="{ minRows: 5, maxRows: 10 }"
                          @update:value="updateComponent({ props: selectedComponent.componentKey === 'code' ? { code: $event } : { log: $event } })"
                        />
                      </n-form-item>
                      <n-form-item v-if="['descriptions', 'list', 'breadcrumb'].includes(selectedComponent.componentKey)" label="数据 JSON">
                        <n-input
                          :value="selectedComponent.props?.itemsText || '[]'"
                          type="textarea"
                          :autosize="{ minRows: 5, maxRows: 10 }"
                          placeholder="数组 JSON"
                          @update:value="updateComponent({ props: { itemsText: $event || '[]' } })"
                        />
                      </n-form-item>
                      <n-form-item v-if="selectedComponent.componentKey === 'menu'" label="菜单配置">
                        <div class="page-widget-config-stack">
                          <n-input
                            :value="selectedComponent.props?.optionsText || '[]'"
                            type="textarea"
                            :autosize="{ minRows: 5, maxRows: 10 }"
                            placeholder="菜单 options JSON"
                            @update:value="updateComponent({ props: { optionsText: $event || '[]' } })"
                          />
                          <div class="option-editor-row two-columns">
                            <n-select :value="selectedComponent.props?.mode || 'vertical'" :options="menuModeOptions" @update:value="updateComponent({ props: { mode: $event || 'vertical' } })" />
                            <n-input :value="selectedComponent.props?.value || ''" placeholder="当前 key" @update:value="updateComponent({ props: { value: $event || '' } })" />
                          </div>
                        </div>
                      </n-form-item>
                      <n-form-item v-if="selectedComponent.componentKey === 'announcement'" label="公示内容">
                        <div class="page-widget-config-stack">
                          <n-input :value="selectedComponent.props?.content || ''" type="textarea" :autosize="{ minRows: 4, maxRows: 8 }" @update:value="updateComponent({ props: { content: $event } })" />
                          <div class="option-editor-row three-columns">
                            <n-select :value="selectedComponent.props?.type || 'info'" :options="alertTypeOptions" @update:value="updateComponent({ props: { type: $event || 'info' } })" />
                            <n-switch :value="selectedComponent.props?.showIcon !== false" @update:value="updateComponent({ props: { showIcon: $event } })" />
                            <n-switch :value="selectedComponent.props?.bordered !== false" @update:value="updateComponent({ props: { bordered: $event } })" />
                          </div>
                        </div>
                      </n-form-item>
                      <n-form-item v-if="selectedComponent.componentKey === 'countdown'" label="倒计时">
                        <div class="option-editor-row three-columns">
                          <n-input-number :value="selectedComponent.props?.duration || 3600000" size="small" :min="1000" :max="86400000" :step="1000" :show-button="false" @update:value="updateComponent({ props: { duration: $event || 3600000 } })" />
                          <n-input-number :value="selectedComponent.props?.precision || 0" size="small" :min="0" :max="3" :show-button="false" @update:value="updateComponent({ props: { precision: $event || 0 } })" />
                          <n-switch :value="selectedComponent.props?.active !== false" @update:value="updateComponent({ props: { active: $event } })" />
                        </div>
                      </n-form-item>
                      <n-form-item v-if="selectedComponent.componentKey === 'number-animation'" label="数值动画">
                        <div class="option-editor-row three-columns">
                          <n-input-number :value="selectedComponent.props?.from || 0" size="small" :show-button="false" @update:value="updateComponent({ props: { from: $event || 0 } })" />
                          <n-input-number :value="selectedComponent.props?.to || 0" size="small" :show-button="false" @update:value="updateComponent({ props: { to: $event || 0 } })" />
                          <n-input-number :value="selectedComponent.props?.duration || 1200" size="small" :min="100" :max="10000" :show-button="false" @update:value="updateComponent({ props: { duration: $event || 1200 } })" />
                        </div>
                      </n-form-item>
                      <n-form-item v-if="selectedComponent.componentKey === 'pagination'" label="分页">
                        <div class="option-editor-row three-columns">
                          <n-input-number :value="selectedComponent.props?.page || 1" size="small" :min="1" :show-button="false" @update:value="updateComponent({ props: { page: $event || 1 } })" />
                          <n-input-number :value="selectedComponent.props?.pageSize || 10" size="small" :min="1" :show-button="false" @update:value="updateComponent({ props: { pageSize: $event || 10 } })" />
                          <n-input-number :value="selectedComponent.props?.itemCount || 0" size="small" :min="0" :show-button="false" @update:value="updateComponent({ props: { itemCount: $event || 0 } })" />
                        </div>
                      </n-form-item>
                      <n-form-item v-if="selectedComponent.componentKey === 'split'" label="面板分隔">
                        <div class="page-widget-config-stack">
                          <div class="option-editor-row three-columns">
                            <n-select :value="selectedComponent.props?.direction || 'horizontal'" :options="splitDirectionOptions" @update:value="updateComponent({ props: { direction: $event || 'horizontal' } })" />
                            <n-input-number :value="selectedComponent.props?.defaultSize || 0.38" size="small" :min="0.1" :max="0.9" :step="0.01" :show-button="false" @update:value="updateComponent({ props: { defaultSize: $event || 0.38 } })" />
                            <n-input-number :value="selectedComponent.props?.max || 0.8" size="small" :min="0.1" :max="1" :step="0.01" :show-button="false" @update:value="updateComponent({ props: { max: $event || 0.8 } })" />
                          </div>
                          <n-input :value="selectedComponent.props?.pane1Content || ''" placeholder="面板 1 内容" @update:value="updateComponent({ props: { pane1Content: $event } })" />
                          <n-input :value="selectedComponent.props?.pane2Content || ''" placeholder="面板 2 内容" @update:value="updateComponent({ props: { pane2Content: $event } })" />
                        </div>
                      </n-form-item>
                    </template>
                  </template>
                </section>
              </n-collapse-item>

              <n-collapse-item v-if="isRowLayout || isColumnLayout" name="gridQuick">
                <template #header>
                  <span class="collapse-section-title">
                    <i class="i-material-symbols:grid-view-outline" />
                    栅格快捷配置
                  </span>
                </template>
                <section class="panel-item grid-quick-config">
                  <template v-if="isRowLayout">
                    <!-- 统一栅格属性面板（spec 驱动）：与列表设计器共用同一份 grid spec 渲染。
                         表单画布（AiFormLayoutNodes / n-grid）仅消费 columns/gutter/rowGap，列表画布专属属性已排除 -->
                    <SpecPropertyPanel
                      :block-type="selectedComponent.componentKey"
                      :model-props="selectedComponent.props || {}"
                      :exclude-keys="GRID_LIST_ONLY_PROPS"
                      @update:prop="handleGridPropUpdate"
                    />
                    <n-form-item label="格子数量">
                      <n-input-number
                        :value="rowColumnCount"
                        :min="1"
                        :max="maxFormGridColumns"
                        size="small"
                        @update:value="updateRowCellCount($event || 1)"
                      />
                      <span class="property-help">调减数量时，多余格子的组件会并入最后一个格子；画布上的格子删除按钮则连同内部组件一起删除。</span>
                    </n-form-item>
                    <div class="grid-column-span-editor">
                      <div
                        v-for="(column, columnIndex) in rowColumns"
                        :key="column.id || columnIndex"
                        class="grid-column-span-row"
                      >
                        <span>{{ column.label || `第 ${columnIndex + 1} 列` }}</span>
                        <n-input-number
                          :value="column.layout?.span || column.props?.span || 1"
                          :min="1"
                          :max="rowTotalColumns"
                          size="small"
                          :show-button="false"
                          @update:value="updateRowColumnSpan(columnIndex, $event || 1)"
                        />
                      </div>
                    </div>
                  </template>
                  <template v-else>
                    <n-form-item label="当前格子 span">
                      <div class="slider-control">
                        <n-slider
                          :value="selectedComponent.layout?.span || selectedComponent.props?.span || 1"
                          :min="1"
                          :max="maxFormGridColumns"
                          :step="1"
                          :marks="gridColumnMarks"
                          @update:value="updateComponent({ layout: { span: $event || 1 }, props: { span: $event || 1 } })"
                        />
                        <n-input-number
                          :value="selectedComponent.layout?.span || selectedComponent.props?.span || 1"
                          :min="1"
                          :max="maxFormGridColumns"
                          :show-button="false"
                          size="small"
                          @update:value="updateComponent({ layout: { span: $event || 1 }, props: { span: $event || 1 } })"
                        />
                      </div>
                    </n-form-item>
                  </template>
                </section>
              </n-collapse-item>

              <SelectedFieldCollapse />
              <n-collapse-item v-if="isButtonComponent" name="button">
                <template #header>
                  <span class="collapse-section-title">
                    <i class="i-material-symbols:smart-button" />
                    按钮组件
                  </span>
                </template>
                <section class="panel-item">
                  <n-form-item label="按钮文字">
                    <n-input
                      :value="selectedComponent.props?.text || selectedComponent.label"
                      clearable
                      placeholder="请输入"
                      @update:value="updateComponent({ label: $event || '按钮', props: { text: $event || '按钮' } })"
                    />
                  </n-form-item>
                  <n-form-item label="按钮类型">
                    <n-select
                      :value="selectedComponent.props?.type || 'primary'"
                      :options="buttonTypeOptions"
                      @update:value="updateComponent({ props: { type: $event || 'primary' } })"
                    />
                  </n-form-item>
                  <n-form-item label="组件尺寸">
                    <n-select
                      :value="selectedComponent.props?.size || 'medium'"
                      :options="componentSizeOptions.filter(item => item.value)"
                      @update:value="updateComponent({ props: { size: $event || 'medium' } })"
                    />
                  </n-form-item>
                  <n-form-item v-if="hasSpecPanelProps" label="组件属性">
                    <n-button size="small" dashed block @click="componentPropsVisible = true">
                      <template #icon>
                        <n-icon :size="14">
                          <SettingsOutline />
                        </n-icon>
                      </template>
                      更多属性
                    </n-button>
                  </n-form-item>
                  <div class="switch-list">
                    <label>
                      <span>块级按钮</span>
                      <n-switch
                        size="small"
                        :value="!!selectedComponent.props?.block"
                        @update:value="updateComponent({ props: { block: $event } })"
                      />
                    </label>
                    <label>
                      <span>禁用</span>
                      <n-switch
                        size="small"
                        :value="!!selectedComponent.props?.disabled"
                        @update:value="updateComponent({ props: { disabled: $event } })"
                      />
                    </label>
                  </div>
                </section>
              </n-collapse-item>

              <n-collapse-item v-if="selectedCrudFieldConfig" name="crud-field">
                <template #header>
                  <span class="collapse-section-title">
                    <i class="i-material-symbols:view-column-outline" />
                    CRUD 字段配置
                  </span>
                </template>
                <section class="panel-item">
                  <div class="crud-field-config-title">
                    查询条件
                  </div>
                  <n-form-item label="查询标签">
                    <n-input
                      :value="selectedCrudFieldConfig.search?.label || ''"
                      clearable
                      placeholder="默认使用字段名称"
                      @update:value="updateCrudFieldConfig('search', { label: $event || undefined })"
                    />
                  </n-form-item>
                  <n-form-item label="查询占位提示">
                    <n-input
                      :value="selectedCrudFieldConfig.search?.placeholder || ''"
                      clearable
                      placeholder="默认使用字段占位提示"
                      @update:value="updateCrudFieldConfig('search', { placeholder: $event || undefined })"
                    />
                  </n-form-item>
                  <n-form-item label="查询控件跨度">
                    <n-input-number
                      :value="selectedCrudFieldConfig.search?.span || selectedComponent.layout?.span || 1"
                      :min="1"
                      :max="maxFormGridColumns"
                      @update:value="updateCrudFieldConfig('search', { span: $event || 1 })"
                    />
                  </n-form-item>

                  <div class="crud-field-config-title">
                    表格列
                  </div>
                  <n-form-item label="列标题">
                    <n-input
                      :value="selectedCrudFieldConfig.table?.title || ''"
                      clearable
                      placeholder="默认使用字段名称"
                      @update:value="updateCrudFieldConfig('table', { title: $event || undefined })"
                    />
                  </n-form-item>
                  <n-form-item label="列宽">
                    <n-input-number
                      :value="selectedCrudFieldConfig.table?.width"
                      clearable
                      :min="60"
                      :max="800"
                      @update:value="updateCrudFieldConfig('table', { width: $event || undefined })"
                    />
                  </n-form-item>
                  <n-form-item label="最小宽度">
                    <n-input-number
                      :value="selectedCrudFieldConfig.table?.minWidth || 120"
                      :min="60"
                      :max="800"
                      @update:value="updateCrudFieldConfig('table', { minWidth: $event || undefined })"
                    />
                  </n-form-item>
                  <n-form-item label="对齐方式">
                    <n-select
                      :value="selectedCrudFieldConfig.table?.align || 'left'"
                      :options="tableAlignOptions"
                      @update:value="updateCrudFieldConfig('table', { align: $event || undefined })"
                    />
                  </n-form-item>
                  <n-form-item label="固定列">
                    <n-select
                      :value="selectedCrudFieldConfig.table?.fixed || ''"
                      :options="tableFixedOptions"
                      @update:value="updateCrudFieldConfig('table', { fixed: $event || undefined })"
                    />
                  </n-form-item>
                  <div class="switch-list">
                    <label>
                      <span>文字省略</span>
                      <n-switch
                        size="small"
                        :value="selectedCrudFieldConfig.table?.ellipsis !== false"
                        @update:value="updateCrudFieldConfig('table', { ellipsis: $event })"
                      />
                    </label>
                    <label>
                      <span>可排序</span>
                      <n-switch
                        size="small"
                        :value="!!selectedCrudFieldConfig.table?.sorter"
                        @update:value="updateCrudFieldConfig('table', { sorter: $event })"
                      />
                    </label>
                  </div>

                  <div class="crud-field-config-title">
                    编辑弹窗
                  </div>
                  <n-form-item label="编辑标签">
                    <n-input
                      :value="selectedCrudFieldConfig.edit?.label || ''"
                      clearable
                      placeholder="默认使用字段名称"
                      @update:value="updateCrudFieldConfig('edit', { label: $event || undefined })"
                    />
                  </n-form-item>
                  <n-form-item label="编辑跨度">
                    <n-input-number
                      :value="selectedCrudFieldConfig.edit?.span || selectedComponent.layout?.span || 1"
                      :min="1"
                      :max="maxFormGridColumns"
                      @update:value="updateCrudFieldConfig('edit', { span: $event || 1 })"
                    />
                  </n-form-item>
                  <div class="switch-list">
                    <label>
                      <span>编辑只读</span>
                      <n-switch
                        size="small"
                        :value="!!selectedCrudFieldConfig.edit?.readonly"
                        @update:value="updateCrudFieldConfig('edit', { readonly: $event })"
                      />
                    </label>
                  </div>
                </section>
              </n-collapse-item>

              <n-collapse-item v-if="isTemporalField" name="temporal">
                <template #header>
                  <span class="collapse-section-title">
                    <i class="i-material-symbols:calendar-month-outline" />
                    日期时间组件
                  </span>
                </template>
                <section class="panel-item">
                  <n-form-item v-if="isDatePickerField" label="选择器类型">
                    <n-select
                      :value="selectedComponent.props?.type || datePickerType"
                      :options="datePickerTypeOptions"
                      @update:value="updateComponent({ props: { type: $event || undefined } })"
                    />
                  </n-form-item>
                  <n-form-item label="显示格式">
                    <n-input
                      :value="selectedComponent.props?.format"
                      clearable
                      :placeholder="isTimePickerField ? 'HH:mm:ss' : 'yyyy-MM-dd'"
                      @update:value="updateComponent({ props: { format: $event || undefined } })"
                    />
                  </n-form-item>
                  <n-form-item label="值格式">
                    <n-input
                      :value="selectedComponent.props?.valueFormat"
                      clearable
                      :placeholder="isTimePickerField ? 'HH:mm:ss' : 'yyyy-MM-dd HH:mm:ss'"
                      @update:value="updateComponent({ props: { valueFormat: $event || undefined } })"
                    />
                  </n-form-item>
                  <n-form-item label="弹出位置">
                    <n-select
                      :value="selectedComponent.props?.placement || 'bottom-start'"
                      :options="pickerPlacementOptions"
                      @update:value="updateComponent({ props: { placement: $event || 'bottom-start' } })"
                    />
                  </n-form-item>
                  <n-form-item label="底部动作">
                    <n-select
                      multiple
                      clearable
                      :value="selectedComponent.props?.actions || []"
                      :options="pickerActionOptions"
                      @update:value="updateComponent({ props: { actions: $event?.length ? $event : undefined } })"
                    />
                  </n-form-item>
                  <div class="switch-list">
                    <label>
                      <span>显示边框</span>
                      <n-switch
                        size="small"
                        :value="selectedComponent.props?.bordered !== false"
                        @update:value="updateComponent({ props: { bordered: $event } })"
                      />
                    </label>
                    <label>
                      <span>输入只读</span>
                      <n-switch
                        size="small"
                        :value="!!selectedComponent.props?.inputReadonly"
                        @update:value="updateComponent({ props: { inputReadonly: $event } })"
                      />
                    </label>
                    <label v-if="isTimePickerField">
                      <span>显示图标</span>
                      <n-switch
                        size="small"
                        :value="selectedComponent.props?.showIcon !== false"
                        @update:value="updateComponent({ props: { showIcon: $event } })"
                      />
                    </label>
                  </div>
                </section>
              </n-collapse-item>

              <n-collapse-item name="assist">
                <template #header>
                  <span class="collapse-section-title">
                    <i class="i-material-symbols:info-outline" />
                    辅助展示
                  </span>
                </template>
                <section class="panel-item">
                  <n-form-item label="说明文本">
                    <n-input
                      :value="selectedComponent.props?.description"
                      type="textarea"
                      :autosize="{ minRows: 2, maxRows: 3 }"
                      clearable
                      placeholder="显示在组件下方"
                      @update:value="updateComponent({ props: { description: $event } })"
                    />
                  </n-form-item>
                  <n-form-item label="角标">
                    <n-input
                      :value="selectedComponent.props?.badge"
                      clearable
                      placeholder="例如 推荐"
                      @update:value="updateComponent({ props: { badge: $event } })"
                    />
                  </n-form-item>
                </section>
              </n-collapse-item>

              <n-collapse-item v-if="isTabsLayout" name="tabs">
                <template #header>
                  <span class="collapse-section-title">
                    <i class="i-material-symbols:tab-outline" />
                    标签页
                  </span>
                </template>
                <section class="panel-item">
                  <!-- tabs 外观属性统一由「更多属性」抽屉的 SpecPropertyPanel 配置（spec 唯一属性源），
                       折叠项只保留页签管理，与列表设计器口径一致 -->
                  <n-form-item v-if="hasSpecPanelProps" label="组件属性">
                    <n-button size="small" dashed block @click="componentPropsVisible = true">
                      <template #icon>
                        <n-icon :size="14">
                          <SettingsOutline />
                        </n-icon>
                      </template>
                      更多属性
                    </n-button>
                  </n-form-item>
                  <div class="layout-child-manager">
                    <div class="panel-title-row">
                      <div class="panel-item-title">
                        页签管理
                      </div>
                      <n-button size="tiny" type="primary" secondary @click="addLayoutChild('tabPane')">
                        新增页签
                      </n-button>
                    </div>
                    <div v-for="(child, childIndex) in layoutChildren" :key="child.id" class="layout-child-card">
                      <div class="layout-child-card-main">
                        <n-input
                          :value="child.props?.label || child.label"
                          size="small"
                          placeholder="页签名称"
                          @update:value="updateLayoutChild(childIndex, { label: $event || `标签 ${childIndex + 1}`, props: { label: $event || `标签 ${childIndex + 1}` } })"
                        />
                      </div>
                      <div class="layout-child-actions">
                        <n-button size="tiny" tertiary :disabled="childIndex === 0" @click="moveLayoutChild(childIndex, -1)">
                          上移
                        </n-button>
                        <n-button size="tiny" tertiary :disabled="childIndex === layoutChildren.length - 1" @click="moveLayoutChild(childIndex, 1)">
                          下移
                        </n-button>
                        <n-button size="tiny" quaternary type="error" :disabled="layoutChildren.length <= 1" @click="removeLayoutChild(childIndex)">
                          删除
                        </n-button>
                      </div>
                    </div>
                  </div>
                </section>
              </n-collapse-item>

              <n-collapse-item v-if="isField" name="validation">
                <template #header>
                  <span class="collapse-section-title">
                    <i class="i-material-symbols:rule-outline" />
                    校验规则
                  </span>
                </template>
                <section class="panel-item validation-panel">
                  <div class="switch-line compact">
                    <span>必填项 Required</span>
                    <n-switch
                      size="small"
                      :value="!!selectedComponent.validation?.required"
                      @update:value="updateComponent({ validation: { required: $event } })"
                    />
                  </div>
                  <n-form-item v-if="selectedComponent.validation?.required" label="必填错误提示">
                    <n-input
                      :value="selectedComponent.validation?.requiredMessage"
                      clearable
                      placeholder="为空时使用默认提示"
                      @update:value="updateComponent({ validation: { requiredMessage: $event } })"
                    />
                  </n-form-item>
                  <n-form-item label="常用校验">
                    <n-select
                      :value="selectedComponent.validation?.preset || ''"
                      :options="commonValidationOptions"
                      clearable
                      placeholder="选择手机号、邮箱等规则"
                      @update:value="updateValidationPreset"
                    />
                  </n-form-item>
                  <n-form-item v-if="selectedComponent.validation?.preset" label="校验提示">
                    <n-input
                      :value="selectedComponent.validation?.message || ''"
                      clearable
                      placeholder="为空时使用规则默认提示"
                      @update:value="updateComponent({ validation: { message: $event || undefined } })"
                    />
                  </n-form-item>
                  <n-form-item label="正则表达式 Pattern">
                    <n-input
                      :value="selectedComponent.validation?.pattern || ''"
                      clearable
                      placeholder="例如: ^[A-Za-z]+$"
                      @update:value="updateComponent({ validation: { pattern: $event || undefined } })"
                    />
                  </n-form-item>
                  <div class="switch-line compact">
                    <span>唯一校验</span>
                    <n-switch
                      size="small"
                      :value="!!selectedComponent.advancedProps?.unique"
                      @update:value="updateUniqueValidation"
                    />
                  </div>
                </section>
              </n-collapse-item>
            </n-collapse>
          </n-form>
</template>

<script>
import { useForgePropertyPanelApi } from '../forgePropertyPanelContext'
import { forgePropertyPanelLocalComponents } from '../forgePropertyPanelLocalComponents'
import SelectedFieldCollapse from './SelectedFieldCollapse.vue'

export default {
  name: 'SelectedBasicTab',
  components: { ...forgePropertyPanelLocalComponents, SelectedFieldCollapse },
  setup() {
    return useForgePropertyPanelApi()
  },
}
</script>

<style scoped src="../forge-property-panel-shell.css"></style>
<style scoped src="../forge-property-panel-fields.css"></style>
