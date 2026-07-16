<template>
  <div class="forge-property-panel">
    <div class="edit-panel-header">
      <div class="edit-panel-title">
        <strong>{{ selectedComponent ? selectedLabel : '表单属性' }}</strong>
        <span>{{ panelDescription }}</span>
      </div>
      <div class="edit-panel-tools">
        <button type="button" class="panel-close-button" title="收起" @click="$emit('close')">
          <svg width="1em" height="1em" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M20.207 20.207a.99.99 0 0 0 .003-1.403L13.406 12l6.804-6.804a.99.99 0 0 0-.003-1.403.99.99 0 0 0-1.403-.003L12 10.594 5.196 3.79a.99.99 0 0 0-1.403.003.99.99 0 0 0-.003 1.403L10.594 12 3.79 18.804a.99.99 0 0 0 .003 1.403.99.99 0 0 0 1.403.003L12 13.406l6.804 6.804a.99.99 0 0 0 1.403-.003Z" fill="currentColor" />
          </svg>
        </button>
      </div>
    </div>

    <div class="property-search-box">
      <n-input
        v-model:value="propertySearchKeyword"
        size="small"
        clearable
        placeholder="搜索属性：字典 / 默认值 / 校验 / 弹窗"
        @update:value="handlePropertySearch"
      />
      <div v-if="propertySearchHit" class="property-search-hit">
        已定位：{{ propertySearchHit }}
      </div>
    </div>

    <template v-if="selectedComponent">
      <n-tabs v-model:value="propertyActiveTab" type="line" size="medium" animated class="property-tabs">
        <n-tab-pane name="basic">
          <template #tab>
            <span class="property-tab-label">
              <n-icon><SettingsOutline /></n-icon>
              属性
            </span>
          </template>
          <n-form label-placement="top" :show-feedback="false" class="property-form">
            <n-collapse v-model:expanded-names="selectedBasicExpandedNames" class="config-collapse">
              <n-collapse-item title="标识" name="identity">
                <section class="panel-item">
                  <n-form-item label="显示名称">
                    <n-input :value="selectedComponent.label" placeholder="请输入" @update:value="updateLabel" />
                  </n-form-item>
                  <n-form-item v-if="canSwitchComponentType" label="组件类型">
                    <n-select
                      :value="selectedComponent.componentKey"
                      :options="switchableComponentOptions"
                      filterable
                      @update:value="handleSwitchComponentType"
                    />
                  </n-form-item>
                  <n-form-item v-if="isField" label="绑定字段">
                    <n-input
                      :value="selectedComponent.fieldBinding?.fieldCode || ''"
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
                              placeholder="{ &quot;id&quot;: &quot;{{ id }}&quot; }，可引用当前表单/详情数据"
                              @update:value="updatePageWidgetDataBinding({ paramsText: $event || '{}' })"
                            />
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

              <n-collapse-item v-if="isRowLayout || isColumnLayout" title="栅格快捷配置" name="gridQuick">
                <section class="panel-item grid-quick-config">
                  <template v-if="isRowLayout">
                    <n-form-item label="栅格总列数">
                      <div class="slider-control">
                        <n-slider
                          :value="rowTotalColumns"
                          :min="1"
                          :max="maxFormGridColumns"
                          :step="1"
                          :marks="gridColumnMarks"
                          @update:value="updateRowTotalColumns"
                        />
                        <n-input-number
                          :value="rowTotalColumns"
                          :min="1"
                          :max="maxFormGridColumns"
                          :show-button="false"
                          size="small"
                          @update:value="updateRowTotalColumns($event || 1)"
                        />
                      </div>
                    </n-form-item>
                    <n-form-item label="格子数量">
                      <n-input-number
                        :value="rowColumnCount"
                        :min="1"
                        :max="maxFormGridColumns"
                        size="small"
                        @update:value="updateRowCellCount($event || 1)"
                      />
                    </n-form-item>
                    <n-form-item label="列间距">
                      <n-input-number
                        :value="selectedComponent.props?.gutter ?? 16"
                        :min="0"
                        :max="40"
                        size="small"
                        @update:value="updateComponent({ props: { gutter: $event ?? 16 } })"
                      />
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

              <n-collapse-item v-if="isField" title="字段组件" name="field">
                <section class="panel-item">
                  <n-form-item label="占位提示">
                    <n-input
                      :value="selectedComponent.props?.placeholder"
                      clearable
                      placeholder="请输入"
                      @update:value="updateComponent({ props: { placeholder: $event } })"
                    />
                  </n-form-item>
                  <n-form-item label="默认值">
                    <n-select
                      v-if="defaultValueSelectEnabled"
                      :value="selectedDefaultValueForSelect"
                      :options="defaultValueSelectOptions"
                      :multiple="defaultValueSelectMultiple"
                      :loading="defaultValueOptionsLoading"
                      filterable
                      clearable
                      placeholder="请选择默认值"
                      @update:value="updateDefaultValue"
                    />
                    <n-input
                      v-else
                      :value="selectedComponent.props?.defaultValue"
                      clearable
                      placeholder="请输入"
                      @update:value="updateDefaultValue"
                    />
                  </n-form-item>
                  <n-form-item v-if="supportsFieldMaxLength" label="最大长度">
                    <div class="option-editor-row two-columns">
                      <n-input-number
                        :value="selectedFieldMaxLength"
                        :min="1"
                        :max="2048"
                        :show-button="false"
                        clearable
                        placeholder="最大长度"
                        @update:value="updateFieldMaxLength"
                      />
                      <n-switch
                        :value="selectedComponent.props?.showCount === true"
                        size="small"
                        @update:value="updateComponent({ props: { showCount: $event } })"
                      >
                        <template #checked>
                          计数
                        </template>
                        <template #unchecked>
                          计数
                        </template>
                      </n-switch>
                    </div>
                  </n-form-item>
                  <n-form-item label="字段约束">
                    <div class="field-constraint-config">
                      <n-select
                        :value="selectedComponent.validation?.preset || ''"
                        :options="commonValidationOptions"
                        clearable
                        placeholder="常用校验：手机号、邮箱、身份证等"
                        @update:value="updateValidationPreset"
                      />
                      <n-input
                        :value="selectedComponent.validation?.pattern || ''"
                        clearable
                        placeholder="正则表达式会随常用校验自动填入"
                        @update:value="updateComponent({ validation: { pattern: $event || undefined } })"
                      />
                    </div>
                  </n-form-item>
                  <n-form-item label="自动编号">
                    <div class="auto-code-config">
                      <div class="switch-line compact">
                        <span>新增时自动生成</span>
                        <n-switch
                          size="small"
                          :value="selectedGenerationEnabled"
                          @update:value="handleGenerationEnabled"
                        />
                      </div>
                      <template v-if="selectedGenerationEnabled">
                        <n-select
                          :value="selectedGenerationRuleCode"
                          :options="codeRuleOptions"
                          :loading="codeRuleLoading"
                          clearable
                          filterable
                          placeholder="选择编码规则"
                          @update:value="updateGenerationRule"
                        />
                        <div class="option-editor-row two-columns">
                          <n-select
                            :value="selectedGenerationConfig.fillPolicy || 'EMPTY_ONLY'"
                            :options="generationFillPolicyOptions"
                            @update:value="updateGenerationConfig({ fillPolicy: $event || 'EMPTY_ONLY' })"
                          />
                          <n-select
                            :value="selectedGenerationConfig.trigger || 'ON_CREATE'"
                            :options="generationTriggerOptions"
                            @update:value="updateGenerationConfig({ trigger: $event || 'ON_CREATE' })"
                          />
                        </div>
                        <div class="switch-line compact">
                          <span>表单填写隐藏</span>
                          <n-switch
                            size="small"
                            :value="!!selectedComponent.visibility?.hidden"
                            @update:value="updateComponentHidden"
                          />
                        </div>
                        <div class="switch-line compact">
                          <span>运行态只读</span>
                          <n-switch
                            size="small"
                            :value="selectedGenerationConfig.readonly !== false"
                            @update:value="updateGenerationReadonly"
                          />
                        </div>
                        <div v-if="selectedGenerationRule" class="auto-code-rule-summary">
                          <span>{{ selectedGenerationRule.ruleName }}</span>
                          <code>{{ selectedGenerationRule.template }}</code>
                          <small>{{ selectedGenerationRule.category || 'COMMON' }} · 结构化规则</small>
                        </div>
                        <div class="auto-code-preview-row">
                          <n-button
                            size="small"
                            secondary
                            :disabled="!selectedGenerationRuleCode"
                            :loading="codeRulePreviewing"
                            @click="previewSelectedGenerationRule"
                          >
                            预览编号
                          </n-button>
                          <strong :class="{ invalid: codeRulePreview?.valid === false }">
                            {{ codeRulePreview?.previewCode || '选择规则后可预览' }}
                          </strong>
                        </div>
                        <div v-if="codeRulePreview?.errors?.length" class="auto-code-issue error">
                          {{ codeRulePreview.errors[0].message }}
                        </div>
                        <div v-else-if="codeRulePreview?.warnings?.length" class="auto-code-issue warning">
                          {{ codeRulePreview.warnings[0].message }}
                        </div>
                      </template>
                    </div>
                  </n-form-item>
                  <n-form-item label="公式配置">
                    <div class="formula-config-entry">
                      <div>
                        <strong>{{ selectedFormulaConfig?.type ? `${selectedFormulaConfig.type} 公式` : '未启用公式' }}</strong>
                        <span>{{ selectedFormulaSummary }}</span>
                      </div>
                      <n-button size="small" secondary @click="openFieldFormulaPanel">
                        配置公式
                      </n-button>
                    </div>
                  </n-form-item>
                  <n-form-item label="组件尺寸">
                    <n-select
                      :value="selectedComponent.props?.size || ''"
                      :options="componentSizeOptions"
                      @update:value="updateComponent({ props: { size: $event || undefined } })"
                    />
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
                  <n-form-item v-if="isDictLikeField" label="字典类型">
                    <DictTypeSelect
                      :value="selectedComponent.props?.dictType || ''"
                      :fields="dictTypeFields"
                      compact
                      @update:value="updateDictType"
                    />
                  </n-form-item>
                  <template v-if="isObjectReferenceField">
                    <n-form-item label="引用对象">
                      <n-select
                        :value="referenceObjectCode"
                        :options="businessObjectOptions"
                        :loading="businessObjectLoading"
                        filterable
                        clearable
                        placeholder="选择目标业务对象"
                        @update:value="updateReferenceObjectCode"
                      />
                    </n-form-item>
                    <n-form-item v-if="referenceObjectCode" label="显示字段">
                      <n-select
                        :value="referenceDisplayField"
                        :options="referenceTargetFieldOptions"
                        :loading="referenceTargetFieldLoading"
                        filterable
                        clearable
                        placeholder="选择下拉选项中显示的字段"
                        @update:value="updateReferenceDisplayField"
                      />
                    </n-form-item>
                    <n-form-item v-if="referenceObjectCode" label="值字段">
                      <n-select
                        :value="referenceValueField"
                        :options="referenceTargetFieldOptions"
                        :loading="referenceTargetFieldLoading"
                        filterable
                        clearable
                        placeholder="选择保存到当前字段的值字段，默认 id"
                        @update:value="updateReferenceValueField"
                      />
                    </n-form-item>
                  </template>
                  <n-form-item v-if="activePropGroups.length" label="组件属性">
                    <n-button class="more-config-button" secondary block @click="componentPropsVisible = true">
                      <template #icon>
                        <span class="button-icon">
                          <svg width="1em" height="1em" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                            <path d="M4 7h9" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
                            <path d="M17 7h3" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
                            <path d="M4 17h3" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
                            <path d="M11 17h9" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
                            <circle cx="15" cy="7" r="2" stroke="currentColor" stroke-width="2" />
                            <circle cx="9" cy="17" r="2" stroke="currentColor" stroke-width="2" />
                          </svg>
                        </span>
                      </template>
                      更多属性
                    </n-button>
                  </n-form-item>
                  <div class="switch-list">
                    <label>
                      <span>可清空</span>
                      <n-switch
                        size="small"
                        :value="selectedComponent.props?.clearable !== false"
                        @update:value="updateComponent({ props: { clearable: $event } })"
                      />
                    </label>
                    <label>
                      <span>显示反馈</span>
                      <n-switch
                        size="small"
                        :value="selectedComponent.props?.showFeedback !== false"
                        @update:value="updateComponent({ props: { showFeedback: $event } })"
                      />
                    </label>
                  </div>
                </section>
              </n-collapse-item>

              <n-collapse-item v-if="isButtonComponent" title="按钮组件" name="button">
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
                  <n-form-item v-if="activePropGroups.length" label="组件属性">
                    <n-button class="more-config-button" secondary block @click="componentPropsVisible = true">
                      <template #icon>
                        <span class="button-icon">
                          <svg width="1em" height="1em" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                            <path d="M4 7h9" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
                            <path d="M17 7h3" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
                            <path d="M4 17h3" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
                            <path d="M11 17h9" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
                            <circle cx="15" cy="7" r="2" stroke="currentColor" stroke-width="2" />
                            <circle cx="9" cy="17" r="2" stroke="currentColor" stroke-width="2" />
                          </svg>
                        </span>
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

              <n-collapse-item v-if="isManualOptionField" title="选项配置" name="options">
                <section class="panel-item">
                  <div class="option-list">
                    <div v-for="(option, optionIndex) in selectedOptions" :key="`option-${optionIndex}`" class="option-editor-row">
                      <n-input
                        :value="option.label"
                        size="small"
                        placeholder="选项名"
                        @update:value="updateOption(optionIndex, { label: $event })"
                      />
                      <n-input
                        :value="String(option.value ?? '')"
                        size="small"
                        placeholder="值"
                        @update:value="updateOption(optionIndex, { value: $event })"
                      />
                      <n-switch
                        size="small"
                        :value="!!option.disabled"
                        title="禁用"
                        @update:value="updateOption(optionIndex, { disabled: $event })"
                      />
                      <n-button size="tiny" quaternary type="error" @click="removeOption(optionIndex)">
                        删除
                      </n-button>
                      <template v-if="selectedComponent.componentKey === 'checkbox'">
                        <n-switch
                          size="small"
                          :value="!!option.indeterminate"
                          title="半选"
                          @update:value="updateOption(optionIndex, { indeterminate: $event })"
                        />
                        <n-switch
                          size="small"
                          :value="option.focusable !== false"
                          title="可聚焦"
                          @update:value="updateOption(optionIndex, { focusable: $event })"
                        />
                        <n-input
                          class="option-props-input"
                          :value="stringifyJsonProp(option.props)"
                          type="textarea"
                          :autosize="{ minRows: 1, maxRows: 3 }"
                          placeholder="Checkbox props JSON，例如 {&quot;checkedValue&quot;:true}"
                          @update:value="updateOptionJsonProps(optionIndex, $event)"
                        />
                      </template>
                    </div>
                  </div>
                  <n-button class="option-add-button" size="small" secondary block @click="addOption">
                    <template #icon>
                      <span class="option-add-icon">+</span>
                    </template>
                    新增选项
                  </n-button>
                </section>
              </n-collapse-item>

              <n-collapse-item v-if="selectedCrudFieldConfig" title="CRUD 字段配置" name="crud-field">
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

              <n-collapse-item v-if="isTemporalField" title="日期时间组件" name="temporal">
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

              <n-collapse-item title="辅助展示" name="assist">
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

              <n-collapse-item v-if="isField" title="校验规则" name="validation">
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
        </n-tab-pane>

        <n-tab-pane name="style">
          <template #tab>
            <span class="property-tab-label">
              <n-icon><ColorPaletteOutline /></n-icon>
              样式
            </span>
          </template>
          <n-form label-placement="top" :show-feedback="false" class="property-form">
            <n-collapse :default-expanded-names="['position', 'layout', 'typography', 'appearance']" class="config-collapse style-config-collapse">
              <n-collapse-item title="位置与尺寸" name="position">
                <section class="panel-item position-control">
                  <div class="position-axis-grid">
                    <label class="position-number-field">
                      <span>左 X</span>
                      <n-input-number
                        :value="selectedDesignerTranslate.x"
                        size="small"
                        :show-button="false"
                        @update:value="updateDesignerTranslate('x', $event)"
                      />
                      <em>px</em>
                    </label>
                    <label class="position-number-field">
                      <span>上 Y</span>
                      <n-input-number
                        :value="selectedDesignerTranslate.y"
                        size="small"
                        :show-button="false"
                        @update:value="updateDesignerTranslate('y', $event)"
                      />
                      <em>px</em>
                    </label>
                  </div>

                  <div class="position-rule">
                    <div class="position-rule-head">
                      <span>宽度</span>
                      <label v-if="(selectedDesignerStyle.widthMode || 'default') === 'default'" class="position-inline-number">
                        <n-input-number
                          :value="resolvePxNumber(selectedDesignerStyle.width, 0)"
                          size="tiny"
                          :min="0"
                          :show-button="false"
                          placeholder="自动"
                          @update:value="updateDesignerStyle({ width: valueToPx($event) })"
                        />
                        <em>px</em>
                      </label>
                    </div>
                    <div class="segmented-mini">
                      <button
                        type="button"
                        :class="{ active: (selectedDesignerStyle.widthMode || 'default') === 'default' }"
                        @click="updateWidthMode('default')"
                      >
                        默认宽度
                      </button>
                      <button
                        type="button"
                        :class="{ active: selectedDesignerStyle.widthMode === 'fill' }"
                        @click="updateWidthMode('fill')"
                      >
                        填充容器
                      </button>
                    </div>
                  </div>

                  <div class="position-rule">
                    <div class="position-rule-head">
                      <span>高度</span>
                      <label v-if="(selectedDesignerStyle.heightMode || 'default') === 'default'" class="position-inline-number">
                        <n-input-number
                          :value="resolvePxNumber(selectedDesignerStyle.height, 0)"
                          size="tiny"
                          :min="0"
                          :show-button="false"
                          placeholder="自动"
                          @update:value="updateDesignerStyle({ height: valueToPx($event) })"
                        />
                        <em>px</em>
                      </label>
                    </div>
                    <div class="segmented-mini three">
                      <button
                        type="button"
                        :class="{ active: (selectedDesignerStyle.heightMode || 'default') === 'default' }"
                        @click="updateHeightMode('default')"
                      >
                        默认高度
                      </button>
                      <button
                        type="button"
                        :class="{ active: selectedDesignerStyle.heightMode === 'fit' }"
                        @click="updateHeightMode('fit')"
                      >
                        适应内容
                      </button>
                      <button
                        type="button"
                        :class="{ active: selectedDesignerStyle.heightMode === 'fill' }"
                        @click="updateHeightMode('fill')"
                      >
                        填充容器
                      </button>
                    </div>
                  </div>
                </section>
              </n-collapse-item>

              <n-collapse-item title="布局与边距" name="layout">
                <section class="panel-item">
                  <div class="spacing-editor">
                    <div class="spacing-editor-title">
                      Padding
                    </div>
                    <div class="spacing-grid">
                      <label v-for="item in spacingSides" :key="`component-padding-${item.key}`">
                        <span>{{ item.label }}</span>
                        <n-input-number
                          :value="resolvePxNumber(selectedDesignerStyle.customStyle?.[`padding${item.key}`], 0)"
                          :min="0"
                          :max="120"
                          :show-button="false"
                          size="small"
                          @update:value="updateDesignerSpacing(`padding${item.key}`, $event)"
                        />
                      </label>
                    </div>
                  </div>
                  <div class="spacing-editor">
                    <div class="spacing-editor-title">
                      Margin
                    </div>
                    <div class="spacing-grid">
                      <label v-for="item in spacingSides" :key="`component-margin-${item.key}`">
                        <span>{{ item.label }}</span>
                        <n-input-number
                          :value="resolvePxNumber(selectedDesignerStyle.customStyle?.[`margin${item.key}`], 0)"
                          :min="-80"
                          :max="120"
                          :show-button="false"
                          size="small"
                          @update:value="updateDesignerSpacing(`margin${item.key}`, $event)"
                        />
                      </label>
                    </div>
                  </div>
                </section>
              </n-collapse-item>

              <n-collapse-item title="文字排版" name="typography">
                <section class="panel-item">
                  <div class="crud-inline-grid">
                    <n-form-item label="字号">
                      <n-input-number
                        :value="resolvePxNumber(selectedDesignerStyle.customStyle?.fontSize, 14)"
                        :min="10"
                        :max="48"
                        :show-button="false"
                        @update:value="updateDesignerSpacing('fontSize', $event)"
                      />
                    </n-form-item>
                    <n-form-item label="行高">
                      <n-input
                        :value="selectedDesignerStyle.customStyle?.lineHeight || ''"
                        clearable
                        placeholder="1.5 / 22px"
                        @update:value="updateDesignerCustomStyle({ lineHeight: $event || undefined })"
                      />
                    </n-form-item>
                  </div>
                  <n-form-item label="文字颜色">
                    <div class="color-control">
                      <n-color-picker
                        :value="selectedDesignerStyle.customStyle?.color || ''"
                        :show-alpha="true"
                        :modes="['hex']"
                        :swatches="colorSwatches"
                        @update:value="updateDesignerCustomStyle({ color: $event || undefined })"
                      />
                      <n-button size="small" quaternary @click="updateDesignerCustomStyle({ color: undefined })">
                        默认
                      </n-button>
                    </div>
                  </n-form-item>
                </section>
              </n-collapse-item>

              <n-collapse-item title="外观与装饰" name="appearance">
                <section class="panel-item appearance-control">
                  <div class="appearance-field">
                    <label>背景色</label>
                    <div class="appearance-input-shell">
                      <label class="appearance-swatch" :style="{ backgroundColor: selectedAppearanceBackgroundPreview }" title="选择背景色">
                        <input
                          type="color"
                          :value="selectedAppearanceBackgroundColorInput"
                          @input="updateSelectedAppearanceBackground($event.target.value)"
                        >
                      </label>
                      <input
                        :value="selectedAppearanceBackgroundHex"
                        class="appearance-hex-input"
                        placeholder="透明"
                        @input="updateSelectedAppearanceBackground($event.target.value)"
                      >
                      <span class="appearance-percent">{{ selectedOpacityPercent }}%</span>
                    </div>
                  </div>
                  <div class="appearance-field">
                    <label>边框 (Border)</label>
                    <div class="appearance-input-shell">
                      <select
                        :value="selectedDesignerStyle.borderStyle || 'solid'"
                        class="appearance-select"
                        @change="updateDesignerBorderStyle($event.target.value)"
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
                      <label class="appearance-swatch" :style="{ backgroundColor: selectedAppearanceBorderPreview }" title="选择边框颜色">
                        <input
                          type="color"
                          :value="selectedAppearanceBorderPreview"
                          @input="updateSelectedAppearanceBorder($event.target.value)"
                        >
                      </label>
                      <input
                        :value="selectedAppearanceBorderHex"
                        class="appearance-hex-input"
                        placeholder="E4E4E7"
                        @input="updateSelectedAppearanceBorder($event.target.value)"
                      >
                    </div>
                  </div>
                  <div class="appearance-field">
                    <label>圆角 (Border Radius)</label>
                    <div class="appearance-radius-shell">
                      <span>R</span>
                      <input
                        :value="resolvePxNumber(selectedDesignerStyle.borderRadius, 6)"
                        type="number"
                        min="0"
                        max="32"
                        @input="updateDesignerStyle({ borderRadius: `${$event.target.value || 6}px` })"
                      >
                    </div>
                  </div>
                  <div class="appearance-field">
                    <label class="appearance-row-label">
                      <span>阴影 (Shadow)</span>
                      <select
                        :value="selectedDesignerStyle.boxShadow || ''"
                        class="appearance-plain-select"
                        @change="updateDesignerStyle({ boxShadow: $event.target.value || undefined })"
                      >
                        <option
                          v-for="option in shadowOptions"
                          :key="option.value || 'none'"
                          :value="option.value"
                        >
                          {{ option.label }}
                        </option>
                      </select>
                    </label>
                  </div>
                  <div class="appearance-field">
                    <label>透明度 (Opacity)</label>
                    <div class="appearance-radius-shell">
                      <span>%</span>
                      <input
                        :value="selectedOpacityPercent"
                        type="number"
                        min="20"
                        max="100"
                        step="5"
                        @input="updateSelectedAppearanceOpacity($event.target.value)"
                      >
                    </div>
                  </div>
                  <n-form-item label="CSS Style">
                    <n-input
                      :value="selectedDesignerStyle.customStyleText || stringifyStyle(selectedDesignerStyle.customStyle)"
                      type="textarea"
                      :autosize="{ minRows: 3, maxRows: 6 }"
                      placeholder="例如 color:#111; padding:12px;"
                      @update:value="updateComponentStyleText"
                    />
                  </n-form-item>
                </section>
              </n-collapse-item>
            </n-collapse>
          </n-form>
        </n-tab-pane>

        <n-tab-pane v-if="isCrudBlock" name="crud">
          <template #tab>
            <span class="property-tab-label">
              <n-icon><ServerOutline /></n-icon>
              CRUD
            </span>
          </template>
          <n-form label-placement="top" :show-feedback="false" class="property-form">
            <section class="panel-item panel-item-strong form-api-panel">
              <div class="panel-title-row">
                <div class="panel-item-title">
                  接口与数据源
                </div>
                <n-button class="more-config-button" size="tiny" type="primary" @click="advancedConfigVisible = true">
                  更多配置
                </n-button>
              </div>
              <div class="form-api-field">
                <span class="form-api-label">基础路径 / 行主键</span>
                <div class="form-api-base-row">
                  <n-input
                    :value="selectedComponent.props?.apiBase"
                    clearable
                    placeholder="/employee"
                    @update:value="updateCrudApiBase"
                  />
                  <n-input
                    :value="selectedComponent.props?.rowKey || 'id'"
                    placeholder="id"
                    @update:value="updateComponent({ props: { rowKey: $event || 'id' } })"
                  />
                </div>
              </div>
              <div class="form-api-field">
                <span class="form-api-label">API 接口地址配置</span>
                <div class="form-api-endpoint-list">
                  <div v-for="item in crudApiFields" :key="item.key" class="form-api-endpoint-row">
                    <span class="form-api-method-badge" :class="resolveCrudApiMethodClass(item)">
                      {{ resolveCrudApiMethodLabel(item) }}
                    </span>
                    <n-input
                      :value="crudApiConfig[item.key]"
                      clearable
                      :placeholder="item.placeholder"
                      @update:value="updateCrudApiConfig(item.key, $event)"
                    />
                  </div>
                </div>
              </div>
            </section>

            <section class="panel-item">
              <div class="panel-item-title">
                字段与列配置
              </div>
              <div v-if="crudConfigFields.length" class="crud-field-config-list">
                <div v-for="item in crudConfigFields" :key="item.id" class="crud-field-config-card">
                  <div class="crud-field-card-head">
                    <button type="button" class="crud-field-name" @click="$emit('update:selectedId', item.id)">
                      <strong>{{ item.label }}</strong>
                      <small>{{ item.fieldCode }}</small>
                    </button>
                    <div class="crud-role-switches">
                      <label>
                        <span>查询</span>
                        <n-switch
                          size="small"
                          :value="item.roles.search"
                          @update:value="updateCrudFieldRole(item.id, 'search', $event)"
                        />
                      </label>
                      <label>
                        <span>表格列</span>
                        <n-switch
                          size="small"
                          :value="item.roles.table"
                          @update:value="updateCrudFieldRole(item.id, 'table', $event)"
                        />
                      </label>
                      <label>
                        <span>编辑</span>
                        <n-switch
                          size="small"
                          :value="item.roles.edit"
                          @update:value="updateCrudFieldRole(item.id, 'edit', $event)"
                        />
                      </label>
                    </div>
                  </div>
                  <div class="crud-inline-grid">
                    <n-form-item label="列标题">
                      <n-input
                        :value="item.config.table?.title || ''"
                        size="small"
                        clearable
                        placeholder="默认字段名"
                        @update:value="updateCrudFieldConfigById(item.id, 'table', { title: $event || undefined })"
                      />
                    </n-form-item>
                    <n-form-item label="列宽">
                      <n-input-number
                        :value="item.config.table?.width"
                        size="small"
                        clearable
                        :min="60"
                        :max="800"
                        :show-button="false"
                        @update:value="updateCrudFieldConfigById(item.id, 'table', { width: $event || undefined })"
                      />
                    </n-form-item>
                    <n-form-item label="对齐">
                      <n-select
                        :value="item.config.table?.align || 'left'"
                        size="small"
                        :options="tableAlignOptions"
                        @update:value="updateCrudFieldConfigById(item.id, 'table', { align: $event || undefined })"
                      />
                    </n-form-item>
                    <n-form-item label="固定">
                      <n-select
                        :value="item.config.table?.fixed || ''"
                        size="small"
                        :options="tableFixedOptions"
                        @update:value="updateCrudFieldConfigById(item.id, 'table', { fixed: $event || undefined })"
                      />
                    </n-form-item>
                  </div>
                  <div class="crud-compact-switches">
                    <label>
                      <span>省略</span>
                      <n-switch
                        size="small"
                        :value="item.config.table?.ellipsis !== false"
                        @update:value="updateCrudFieldConfigById(item.id, 'table', { ellipsis: $event })"
                      />
                    </label>
                    <label>
                      <span>排序</span>
                      <n-switch
                        size="small"
                        :value="!!item.config.table?.sorter"
                        @update:value="updateCrudFieldConfigById(item.id, 'table', { sorter: $event })"
                      />
                    </label>
                    <n-button size="tiny" tertiary @click="openCrudFieldDrawer(item.id)">
                      更多字段配置
                    </n-button>
                  </div>
                </div>
              </div>
              <div v-else class="crud-field-empty">
                先把字段拖入 CRUD 区块，字段会自动生成查询条件、表格列和编辑弹窗字段。
              </div>
            </section>

            <section class="panel-item">
              <div class="panel-item-title">
                查询表单
              </div>
              <n-form-item label="搜索列数">
                <n-input-number
                  :value="crudOptions.searchGridCols || 4"
                  :min="1"
                  :max="6"
                  @update:value="updateCrudOption('searchGridCols', $event || 4)"
                />
              </n-form-item>
              <n-form-item label="最大显示字段数">
                <n-input-number
                  :value="crudOptions.searchMaxVisibleFields || 3"
                  :min="1"
                  :max="12"
                  @update:value="updateCrudOption('searchMaxVisibleFields', $event || 3)"
                />
              </n-form-item>
            </section>

            <section class="panel-item">
              <div class="panel-item-title">
                表格
              </div>
              <n-form-item label="表格尺寸">
                <n-select
                  :value="crudOptions.tableSize || 'small'"
                  :options="componentSizeOptions.filter(item => item.value)"
                  @update:value="updateCrudOption('tableSize', $event || 'small')"
                />
              </n-form-item>
              <n-form-item label="渲染模式">
                <n-radio-group
                  :value="crudOptions.renderMode || 'table'"
                  size="small"
                  @update:value="updateCrudOption('renderMode', $event)"
                >
                  <n-radio-button value="table">
                    表格
                  </n-radio-button>
                  <n-radio-button value="card">
                    卡片
                  </n-radio-button>
                </n-radio-group>
              </n-form-item>
            </section>

            <section class="panel-item">
              <div class="panel-item-title">
                编辑表单
              </div>
              <n-form-item label="编辑列数">
                <n-input-number
                  :value="crudOptions.editGridCols || 1"
                  :min="1"
                  :max="maxFormGridColumns"
                  @update:value="updateCrudOption('editGridCols', $event || 1)"
                />
              </n-form-item>
              <n-form-item label="标签位置">
                <n-radio-group
                  :value="crudOptions.editLabelPlacement || 'left'"
                  size="small"
                  @update:value="updateCrudOption('editLabelPlacement', $event)"
                >
                  <n-radio-button value="left">
                    左侧
                  </n-radio-button>
                  <n-radio-button value="top">
                    顶部
                  </n-radio-button>
                </n-radio-group>
              </n-form-item>
              <n-form-item label="编辑表单尺寸">
                <n-select
                  :value="crudOptions.editSize || 'medium'"
                  :options="componentSizeOptions.filter(item => item.value)"
                  @update:value="updateCrudOption('editSize', $event || 'medium')"
                />
              </n-form-item>
            </section>
          </n-form>
        </n-tab-pane>

        <n-tab-pane v-if="false" name="layout">
          <template #tab>
            <span class="property-tab-label">
              <n-icon><GridOutline /></n-icon>
              布局
            </span>
          </template>
          <n-form label-placement="top" :show-feedback="false" class="property-form">
            <section class="panel-item">
              <div class="panel-item-title">
                栅格
              </div>
              <n-form-item v-if="!isRowLayout" label="占据列数">
                <div class="slider-control">
                  <n-slider
                    :value="selectedComponent.layout?.span || 1"
                    :min="1"
                    :max="normalizedFormGridColumns"
                    :step="1"
                    @update:value="updateComponent({ layout: { span: $event || 1 } })"
                  />
                  <n-input-number
                    :value="selectedComponent.layout?.span || 1"
                    :min="1"
                    :max="normalizedFormGridColumns"
                    :show-button="false"
                    size="small"
                    @update:value="updateComponent({ layout: { span: $event || 1 } })"
                  />
                </div>
              </n-form-item>
              <n-form-item v-if="isRowLayout" label="栅格总列数">
                <div class="slider-control">
                  <n-slider
                    :value="rowTotalColumns"
                    :min="1"
                    :max="maxFormGridColumns"
                    :step="1"
                    :marks="gridColumnMarks"
                    @update:value="updateRowTotalColumns"
                  />
                  <n-input-number
                    :value="rowTotalColumns"
                    :min="1"
                    :max="maxFormGridColumns"
                    :show-button="false"
                    size="small"
                    @update:value="updateRowTotalColumns($event || 1)"
                  />
                </div>
              </n-form-item>
              <n-form-item v-if="isRowLayout" label="格子数量">
                <n-input-number
                  :value="rowColumnCount"
                  :min="1"
                  :max="maxFormGridColumns"
                  size="small"
                  @update:value="updateRowCellCount($event || 1)"
                />
              </n-form-item>
              <n-form-item v-if="isRowLayout" label="列间距">
                <n-input-number
                  :value="selectedComponent.props?.gutter ?? 16"
                  :min="0"
                  :max="40"
                  @update:value="updateComponent({ props: { gutter: $event ?? 16 } })"
                />
              </n-form-item>
              <n-form-item v-if="isRowLayout" label="每列 span">
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
              </n-form-item>
              <n-form-item v-if="isColumnLayout" label="栅格列宽">
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
              <n-form-item v-if="isField" label="标签宽度">
                <n-input-number
                  :value="selectedComponent.layout?.labelWidth"
                  clearable
                  :min="60"
                  :max="260"
                  @update:value="updateComponent({ layout: { labelWidth: $event || undefined } })"
                />
              </n-form-item>
            </section>
            <section v-if="isCardLayout" class="panel-item">
              <div class="panel-item-title">
                Card 属性
              </div>
              <n-form-item label="size">
                <n-select
                  :value="selectedComponent.props?.size || 'small'"
                  :options="cardSizeOptions"
                  @update:value="updateComponent({ props: { size: $event || 'small' } })"
                />
              </n-form-item>
              <div class="switch-list">
                <label>
                  <span>bordered</span>
                  <n-switch
                    size="small"
                    :value="selectedComponent.props?.bordered !== false"
                    @update:value="updateComponent({ props: { bordered: $event } })"
                  />
                </label>
                <label>
                  <span>embedded</span>
                  <n-switch
                    size="small"
                    :value="!!selectedComponent.props?.embedded"
                    @update:value="updateComponent({ props: { embedded: $event } })"
                  />
                </label>
                <label>
                  <span>segmented</span>
                  <n-switch
                    size="small"
                    :value="!!selectedComponent.props?.segmented"
                    @update:value="updateComponent({ props: { segmented: $event } })"
                  />
                </label>
                <label>
                  <span>hoverable</span>
                  <n-switch
                    size="small"
                    :value="!!selectedComponent.props?.hoverable"
                    @update:value="updateComponent({ props: { hoverable: $event } })"
                  />
                </label>
              </div>
            </section>

            <section v-if="isTabsLayout" class="panel-item">
              <div class="panel-item-title">
                Tabs 属性
              </div>
              <n-form-item label="type">
                <n-select
                  :value="selectedComponent.props?.type || 'line'"
                  :options="tabsTypeOptions"
                  @update:value="updateComponent({ props: { type: $event || 'line' } })"
                />
              </n-form-item>
              <n-form-item label="placement">
                <n-select
                  :value="selectedComponent.props?.placement || 'top'"
                  :options="tabsPlacementOptions"
                  @update:value="updateComponent({ props: { placement: $event || 'top' } })"
                />
              </n-form-item>
              <n-form-item label="trigger">
                <n-select
                  :value="selectedComponent.props?.trigger || 'click'"
                  :options="tabsTriggerOptions"
                  @update:value="updateComponent({ props: { trigger: $event || 'click' } })"
                />
              </n-form-item>
              <n-form-item label="size">
                <n-select
                  :value="selectedComponent.props?.size || 'medium'"
                  :options="componentSizeOptions.filter(item => item.value)"
                  @update:value="updateComponent({ props: { size: $event || 'medium' } })"
                />
              </n-form-item>
              <div class="switch-list">
                <label>
                  <span>animated</span>
                  <n-switch
                    size="small"
                    :value="selectedComponent.props?.animated !== false"
                    @update:value="updateComponent({ props: { animated: $event } })"
                  />
                </label>
                <label>
                  <span>closable</span>
                  <n-switch
                    size="small"
                    :value="!!selectedComponent.props?.closable"
                    @update:value="updateComponent({ props: { closable: $event } })"
                  />
                </label>
              </div>
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
                    <n-input
                      :value="child.props?.name || child.id"
                      size="small"
                      placeholder="name"
                      @update:value="updateLayoutChild(childIndex, { props: { name: $event || undefined } })"
                    />
                  </div>
                  <div class="layout-child-actions">
                    <n-button size="tiny" tertiary @click="$emit('update:selectedId', child.id)">
                      配置内容
                    </n-button>
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

            <section v-if="isCollapseLayout" class="panel-item">
              <div class="panel-item-title">
                Collapse 属性
              </div>
              <n-form-item label="arrowPlacement">
                <n-select
                  :value="selectedComponent.props?.arrowPlacement || 'left'"
                  :options="collapseArrowPlacementOptions"
                  @update:value="updateComponent({ props: { arrowPlacement: $event || 'left' } })"
                />
              </n-form-item>
              <n-form-item label="displayDirective">
                <n-select
                  :value="selectedComponent.props?.displayDirective || 'if'"
                  :options="collapseDisplayDirectiveOptions"
                  @update:value="updateComponent({ props: { displayDirective: $event || 'if' } })"
                />
              </n-form-item>
              <n-form-item label="triggerAreas">
                <n-select
                  multiple
                  :value="selectedComponent.props?.triggerAreas || ['main', 'arrow']"
                  :options="collapseTriggerAreaOptions"
                  @update:value="updateComponent({ props: { triggerAreas: $event?.length ? $event : ['main', 'arrow'] } })"
                />
              </n-form-item>
              <div class="switch-list">
                <label>
                  <span>accordion</span>
                  <n-switch
                    size="small"
                    :value="!!selectedComponent.props?.accordion"
                    @update:value="updateComponent({ props: { accordion: $event } })"
                  />
                </label>
              </div>
              <div class="layout-child-manager">
                <div class="panel-title-row">
                  <div class="panel-item-title">
                    面板管理
                  </div>
                  <n-button size="tiny" type="primary" secondary @click="addLayoutChild('collapseItem')">
                    新增面板
                  </n-button>
                </div>
                <div v-for="(child, childIndex) in layoutChildren" :key="child.id" class="layout-child-card">
                  <div class="layout-child-card-main">
                    <n-input
                      :value="child.props?.title || child.label"
                      size="small"
                      placeholder="面板标题"
                      @update:value="updateLayoutChild(childIndex, { label: $event || `分组 ${childIndex + 1}`, props: { title: $event || `分组 ${childIndex + 1}` } })"
                    />
                    <n-input
                      :value="child.props?.name || child.id"
                      size="small"
                      placeholder="name"
                      @update:value="updateLayoutChild(childIndex, { props: { name: $event || undefined } })"
                    />
                  </div>
                  <div class="layout-child-actions">
                    <n-button size="tiny" tertiary @click="$emit('update:selectedId', child.id)">
                      配置内容
                    </n-button>
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
          </n-form>
        </n-tab-pane>

        <n-tab-pane v-if="false" name="state">
          <template #tab>
            <span class="property-tab-label">
              <n-icon><ToggleOutline /></n-icon>
              状态
            </span>
          </template>
          <n-form label-placement="top" :show-feedback="false" class="property-form">
            <section class="panel-item">
              <div class="panel-item-title">
                可见性与校验
              </div>
              <div class="switch-list">
                <label v-if="isField">
                  <span>必填</span>
                  <n-switch
                    size="small"
                    :value="!!selectedComponent.validation?.required"
                    @update:value="updateComponent({ validation: { required: $event } })"
                  />
                </label>
                <label v-if="isField">
                  <span>唯一校验</span>
                  <n-switch
                    size="small"
                    :value="!!selectedComponent.advancedProps?.unique"
                    @update:value="updateUniqueValidation"
                  />
                </label>
                <label v-if="isField">
                  <span>只读</span>
                  <n-switch
                    size="small"
                    :value="!!selectedComponent.visibility?.readonly"
                    @update:value="updateComponent({ visibility: { readonly: $event } })"
                  />
                </label>
                <label v-if="isField || isButtonComponent">
                  <span>禁用</span>
                  <n-switch
                    size="small"
                    :value="!!selectedComponent.props?.disabled"
                    @update:value="updateComponent({ props: { disabled: $event } })"
                  />
                </label>
                <label>
                  <span>隐藏</span>
                  <n-switch
                    size="small"
                    :value="!!selectedComponent.visibility?.hidden"
                    @update:value="updateComponentHidden"
                  />
                </label>
              </div>
              <n-form-item v-if="isField" label="必填提示">
                <n-input
                  :value="selectedComponent.validation?.requiredMessage"
                  clearable
                  placeholder="为空时使用默认提示"
                  @update:value="updateComponent({ validation: { requiredMessage: $event } })"
                />
              </n-form-item>
            </section>
          </n-form>
        </n-tab-pane>

        <n-tab-pane name="interaction">
          <template #tab>
            <span class="property-tab-label">
              <n-icon><FlashOutline /></n-icon>
              交互
            </span>
          </template>
          <n-form label-placement="top" :show-feedback="false" class="property-form">
            <section class="panel-item">
              <RuntimeRulesEditor
                title="组件运行规则"
                :rules="selectedComponent.props?.runtimeRules || []"
                :field-options="runtimeRuleFieldOptions"
                @update:rules="updateComponent({ props: { runtimeRules: $event } })"
              />
            </section>

            <section class="panel-item">
              <div class="panel-title-row">
                <div class="panel-item-title">
                  事件规则
                </div>
                <n-button size="tiny" type="primary" secondary @click="addInteractionRule">
                  新增规则
                </n-button>
              </div>
              <div class="interaction-presets">
                <button
                  v-for="preset in interactionPresets"
                  :key="preset.key"
                  type="button"
                  class="interaction-preset-card"
                  @click="addInteractionPreset(preset.key)"
                >
                  <strong>{{ preset.title }}</strong>
                  <span>{{ preset.description }}</span>
                </button>
              </div>
              <div v-if="interactionRules.length" class="interaction-rule-list">
                <details v-for="(rule, ruleIndex) in interactionRules" :key="rule.id || ruleIndex" class="interaction-rule-card" open>
                  <summary class="interaction-rule-head">
                    <div>
                      <strong>{{ resolveTriggerLabel(rule.trigger) }}</strong>
                      <span>{{ resolveActionLabel(rule.action) }}</span>
                    </div>
                    <n-button size="tiny" quaternary type="error" @click.stop.prevent="removeInteractionRule(ruleIndex)">
                      删除
                    </n-button>
                  </summary>
                  <div class="interaction-grid">
                    <n-form-item>
                      <template #label>
                        <span class="field-label-with-help">
                          触发事件
                          <n-tooltip trigger="hover">
                            <template #trigger>
                              <span class="help-icon">?</span>
                            </template>
                            什么时候执行这条规则。
                          </n-tooltip>
                        </span>
                      </template>
                      <n-select
                        :value="rule.trigger || defaultTrigger"
                        size="small"
                        :consistent-menu-width="false"
                        :options="triggerOptions"
                        @update:value="updateInteractionRule(ruleIndex, { trigger: $event || defaultTrigger })"
                      />
                    </n-form-item>
                    <n-form-item>
                      <template #label>
                        <span class="field-label-with-help">
                          执行动作
                          <n-tooltip trigger="hover">
                            <template #trigger>
                              <span class="help-icon">?</span>
                            </template>
                            规则触发后对目标组件做什么。
                          </n-tooltip>
                        </span>
                      </template>
                      <n-select
                        :value="rule.action || 'setValue'"
                        size="small"
                        :consistent-menu-width="false"
                        :options="actionOptions"
                        @update:value="updateInteractionRule(ruleIndex, { action: $event || 'setValue' })"
                      />
                    </n-form-item>
                    <n-form-item>
                      <template #label>
                        <span class="field-label-with-help">
                          目标组件
                          <n-tooltip trigger="hover">
                            <template #trigger>
                              <span class="help-icon">?</span>
                            </template>
                            被这条规则影响的字段、按钮或区块。
                          </n-tooltip>
                        </span>
                      </template>
                      <n-select
                        :value="rule.targetId || ''"
                        size="small"
                        filterable
                        clearable
                        :consistent-menu-width="false"
                        :options="componentTargetOptions"
                        @update:value="updateInteractionRule(ruleIndex, { targetId: $event || '' })"
                      />
                    </n-form-item>
                    <n-form-item>
                      <template #label>
                        <span class="field-label-with-help">
                          触发值等于
                          <n-tooltip trigger="hover">
                            <template #trigger>
                              <span class="help-icon">?</span>
                            </template>
                            用于下拉联动，例如选择“省份A”时才更新城市。
                          </n-tooltip>
                        </span>
                      </template>
                      <n-input
                        :value="rule.whenValue ?? ''"
                        size="small"
                        clearable
                        placeholder="为空表示任何值都触发"
                        @update:value="updateInteractionRule(ruleIndex, { whenValue: $event || undefined })"
                      />
                    </n-form-item>
                  </div>
                  <template v-if="rule.action === 'setOptions'">
                    <n-form-item label="选项来源 API">
                      <n-input
                        :value="rule.api || ''"
                        size="small"
                        clearable
                        placeholder="get@/api/options?parent=:value"
                        @update:value="updateInteractionRule(ruleIndex, { api: $event || undefined })"
                      />
                    </n-form-item>
                    <n-form-item label="静态选项 JSON">
                      <n-input
                        :value="rule.optionsJson || stringifyJsonProp(rule.options)"
                        type="textarea"
                        :autosize="{ minRows: 2, maxRows: 5 }"
                        placeholder="[{&quot;label&quot;:&quot;选项&quot;,&quot;value&quot;:&quot;A&quot;}]"
                        @update:value="updateInteractionRuleJson(ruleIndex, 'options', $event)"
                      />
                    </n-form-item>
                  </template>
                  <n-form-item v-else-if="['setValue', 'showHide', 'enableDisable'].includes(rule.action)">
                    <template #label>
                      <span class="field-label-with-help">
                        {{ resolveActionValueMeta(rule.action).label }}
                        <n-tooltip trigger="hover">
                          <template #trigger>
                            <span class="help-icon">?</span>
                          </template>
                          {{ resolveActionValueMeta(rule.action).help }}
                        </n-tooltip>
                      </span>
                    </template>
                    <n-input
                      :value="rule.value ?? ''"
                      size="small"
                      clearable
                      :placeholder="resolveActionValueMeta(rule.action).placeholder"
                      @update:value="updateInteractionRule(ruleIndex, { value: $event || undefined })"
                    />
                  </n-form-item>
                  <template v-else-if="rule.action === 'openModal'">
                    <n-form-item label="弹窗标题">
                      <n-input
                        :value="rule.modalTitle || ''"
                        size="small"
                        clearable
                        placeholder="请输入弹窗标题"
                        @update:value="updateInteractionRule(ruleIndex, { modalTitle: $event || undefined })"
                      />
                    </n-form-item>
                    <n-form-item>
                      <template #label>
                        <span class="field-label-with-help">
                          弹窗内容
                          <n-tooltip trigger="hover">
                            <template #trigger>
                              <span class="help-icon">?</span>
                            </template>
                            不需要手写 JSON。可以复用当前表单，或选择画布里的某个组件作为弹窗内容。
                          </n-tooltip>
                        </span>
                      </template>
                      <n-select
                        :value="rule.modalContentMode || 'currentForm'"
                        :options="modalContentModeOptions"
                        @update:value="updateInteractionRule(ruleIndex, { modalContentMode: $event || 'currentForm' })"
                      />
                    </n-form-item>
                    <n-form-item v-if="rule.modalContentMode === 'component'" label="弹窗组件">
                      <n-select
                        :value="rule.modalComponentId || ''"
                        filterable
                        clearable
                        :consistent-menu-width="false"
                        :options="componentTargetOptions"
                        @update:value="updateInteractionRule(ruleIndex, { modalComponentId: $event || '' })"
                      />
                    </n-form-item>
                    <n-form-item v-if="rule.modalContentMode === 'formAsset'" label="引用表单">
                      <n-select
                        :value="rule.modalFormKey || 'current'"
                        filterable
                        :consistent-menu-width="false"
                        :options="formAssetOptions"
                        @update:value="updateInteractionRule(ruleIndex, { modalFormKey: $event || 'current' })"
                      />
                    </n-form-item>
                  </template>
                  <n-form-item v-else-if="rule.action === 'apiRequest'" label="请求接口">
                    <n-input
                      :value="rule.api || ''"
                      size="small"
                      clearable
                      placeholder="post@/api/action"
                      @update:value="updateInteractionRule(ruleIndex, { api: $event || undefined })"
                    />
                  </n-form-item>
                </details>
              </div>
              <div v-else class="empty-config-box">
                暂无事件规则
              </div>
            </section>

            <section class="panel-item">
              <div class="panel-item-title">
                状态联动
              </div>
              <div class="linkage-action-list">
                <button type="button" class="linkage-action-row" @click="configureLinkageRule('showHide')">
                  <span>显示/隐藏规则</span>
                  <strong>配置</strong>
                </button>
                <button type="button" class="linkage-action-row" @click="configureLinkageRule('enableDisable')">
                  <span>禁用/只读规则</span>
                  <strong>配置</strong>
                </button>
              </div>
              <div class="switch-list">
                <label>
                  <span>隐藏组件</span>
                  <n-switch
                    size="small"
                    :value="!!selectedComponent.visibility?.hidden"
                    @update:value="updateComponentHidden"
                  />
                </label>
                <label v-if="isField">
                  <span>只读</span>
                  <n-switch
                    size="small"
                    :value="!!selectedComponent.visibility?.readonly"
                    @update:value="updateComponent({ visibility: { readonly: $event } })"
                  />
                </label>
                <label v-if="isField || isButtonComponent">
                  <span>禁用</span>
                  <n-switch
                    size="small"
                    :value="!!selectedComponent.props?.disabled"
                    @update:value="updateComponent({ props: { disabled: $event } })"
                  />
                </label>
              </div>
            </section>
          </n-form>
        </n-tab-pane>

        <n-tab-pane v-if="false" name="source">
          <template #tab>
            <span class="property-tab-label">
              <n-icon><CodeSlashOutline /></n-icon>
              源码
            </span>
          </template>
          <n-form label-placement="top" :show-feedback="false" class="property-form">
            <section class="panel-item source-panel">
              <div class="panel-title-row">
                <div>
                  <div class="panel-item-title">
                    当前组件 JSON
                  </div>
                  <div class="source-path">
                    {{ selectedComponent.id }}
                  </div>
                </div>
                <div class="source-actions">
                  <n-button size="tiny" tertiary @click="resetSelectedCodeDraft">
                    重置
                  </n-button>
                  <n-button size="tiny" type="primary" secondary @click="applySelectedCode">
                    应用
                  </n-button>
                </div>
              </div>
              <n-input
                :value="selectedCodeText"
                type="textarea"
                class="source-editor"
                :autosize="{ minRows: 16, maxRows: 28 }"
                @update:value="updateSelectedCodeDraft"
              />
              <div v-if="sourceError" class="source-error">
                {{ sourceError }}
              </div>
            </section>
          </n-form>
        </n-tab-pane>
      </n-tabs>

      <n-drawer
        v-if="isCrudBlock"
        v-model:show="advancedConfigVisible"
        :width="380"
        placement="right"
        :trap-focus="false"
        :block-scroll="false"
      >
        <n-drawer-content title="CRUD 更多配置" closable>
          <n-form label-placement="top" :show-feedback="false" class="property-form drawer-property-form">
            <section class="panel-item">
              <div class="panel-item-title">
                AiCrudPage API
              </div>
              <div class="form-api-endpoint-list">
                <div v-for="item in crudApiFields" :key="item.key" class="form-api-endpoint-row">
                  <span class="form-api-method-badge" :class="resolveCrudApiMethodClass(item)">
                    {{ resolveCrudApiMethodLabel(item) }}
                  </span>
                  <n-input
                    :value="crudApiConfig[item.key]"
                    clearable
                    :placeholder="item.placeholder"
                    @update:value="updateCrudApiConfig(item.key, $event)"
                  />
                </div>
              </div>
            </section>

            <section class="panel-item">
              <div class="panel-item-title">
                查询表单细节
              </div>
              <n-form-item label="搜索标签宽度">
                <n-input
                  :value="crudOptions.searchLabelWidth || 'auto'"
                  placeholder="auto / 100"
                  @update:value="updateCrudOption('searchLabelWidth', $event || 'auto')"
                />
              </n-form-item>
              <n-form-item label="搜索行间距">
                <n-input-number
                  :value="crudOptions.searchYGap || 16"
                  :min="0"
                  :max="40"
                  @update:value="updateCrudOption('searchYGap', $event || 16)"
                />
              </n-form-item>
            </section>

            <section class="panel-item">
              <div class="panel-item-title">
                表格细节
              </div>
              <n-form-item label="列宽拖拽">
                <n-switch
                  size="small"
                  :value="crudOptions.resizable !== false"
                  @update:value="updateCrudOption('resizable', $event)"
                />
              </n-form-item>
              <n-form-item label="表格最大高度">
                <n-input
                  :value="crudOptions.maxHeight || ''"
                  clearable
                  placeholder="例如 520 / 60vh"
                  @update:value="updateCrudOption('maxHeight', $event || undefined)"
                />
              </n-form-item>
              <n-form-item label="横向滚动宽度">
                <n-input-number
                  :value="crudOptions.scrollX"
                  clearable
                  :min="0"
                  :max="5000"
                  @update:value="updateCrudOption('scrollX', $event || undefined)"
                />
              </n-form-item>
            </section>

            <section class="panel-item">
              <div class="panel-item-title">
                展开面板
              </div>
              <div class="crud-expand-config-panel">
                <div class="crud-expand-config-head">
                  <n-switch
                    size="small"
                    :value="crudOptions.expandConfig?.enabled === true"
                    @update:value="updateCrudExpandEnabled"
                  />
                  <span>{{ crudOptions.expandConfig?.enabled === true ? '已启用行展开' : '未启用行展开' }}</span>
                </div>
              </div>
              <template v-if="crudOptions.expandConfig?.enabled === true">
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
                        :value="crudOptions.expandConfig?.trigger || 'icon'"
                        :options="expandTriggerOptions"
                        size="small"
                        @update:value="value => updateCrudExpandConfig({ trigger: value || 'icon' })"
                      />
                    </label>
                    <label class="crud-expand-config-field">
                      <span>
                        展示方式
                      </span>
                      <n-select
                        :value="crudOptions.expandConfig?.layout?.mode || 'single'"
                        :options="expandLayoutModeOptions"
                        size="small"
                        @update:value="value => updateCrudExpandConfig({ layout: { ...(crudOptions.expandConfig?.layout || {}), mode: value || 'single' } })"
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
                        :value="firstCrudExpandPanel?.type || 'descriptions'"
                        :options="expandPanelTypeOptions"
                        size="small"
                        @update:value="value => updateFirstCrudExpandPanel({ type: value || 'descriptions' })"
                      />
                    </label>
                    <label class="crud-expand-config-field">
                      <span>
                        面板标题
                      </span>
                      <n-input
                        :value="firstCrudExpandPanel?.title || ''"
                        size="small"
                        clearable
                        placeholder="概览 / 明细"
                        @update:value="value => updateFirstCrudExpandPanel({ title: value || undefined })"
                      />
                    </label>
                    <label class="crud-expand-config-field">
                      <span>
                        数据来源
                      </span>
                      <n-select
                        :value="firstCrudExpandPanel?.dataSource?.type || 'row'"
                        :options="expandDataSourceTypeOptions"
                        size="small"
                        @update:value="value => updateFirstCrudExpandPanel({ dataSource: { ...(firstCrudExpandPanel?.dataSource || {}), type: value || 'row' } })"
                      />
                    </label>
                  </div>
                </div>
                <div v-if="['api', 'quantity'].includes(firstCrudExpandPanel?.dataSource?.type)" class="crud-expand-config-section">
                  <div class="crud-expand-section-title">
                    {{ firstCrudExpandPanel?.dataSource?.type === 'quantity' ? '数量查询参数' : '接口参数' }}
                  </div>
                  <label v-if="firstCrudExpandPanel?.dataSource?.type === 'api'" class="crud-expand-config-field">
                    <span>
                      接口地址
                    </span>
                    <n-input
                      :value="firstCrudExpandPanel?.dataSource?.api || ''"
                      size="small"
                      clearable
                      placeholder="get@/api/order/item/page"
                      @update:value="value => updateFirstCrudExpandDataSource({ api: value || '' })"
                    />
                  </label>
                  <label class="crud-expand-config-field">
                    <span>
                      参数映射
                    </span>
                    <n-input
                      :value="stringifyJsonProp(firstCrudExpandPanel?.dataSource?.paramsMap || {})"
                      type="textarea"
                      :autosize="{ minRows: 2, maxRows: 4 }"
                      placeholder="例如 {&quot;sourceRecordId&quot;:&quot;${row.id}&quot;}"
                      @update:value="updateFirstCrudExpandParamsMap"
                    />
                  </label>
                </div>
                <div v-if="firstCrudExpandPanel?.type === 'descriptions'" class="crud-expand-config-section">
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
                          {{ resolveCrudExpandDescriptionSelectedFields(firstCrudExpandPanel).length }} 个字段
                        </div>
                        <div class="bitable-config-value-box-btn">
                          <n-popover
                            v-model:show="crudDescriptionFieldPanelOpen"
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
                                  :model-value="resolveCrudExpandDescriptionPanelFields(firstCrudExpandPanel)"
                                  item-key="field"
                                  handle=".crud-bitable-field-drag"
                                  :animation="160"
                                  @update:model-value="handleCrudExpandDescriptionPanelReorder"
                                >
                                  <template #item="{ element }">
                                    <div class="crud-bitable-field-row" :class="{ invisible: !element.selected }">
                                      <button type="button" class="crud-bitable-field-drag" title="拖拽排序">
                                        <n-icon>
                                          <BitableDragIcon />
                                        </n-icon>
                                      </button>
                                      <n-icon class="crud-bitable-field-icon">
                                        <component :is="resolveCrudBitableFieldIconComponent(element)" />
                                      </n-icon>
                                      <div class="crud-bitable-field-name">
                                        <span>{{ element.label || element.field }}</span>
                                        <small>{{ element.field }}</small>
                                      </div>
                                      <button
                                        type="button"
                                        class="crud-bitable-field-visible"
                                        :title="element.selected ? '隐藏字段' : '显示字段'"
                                        @click="toggleFirstCrudExpandDescriptionField(element.field, !element.selected)"
                                      >
                                        <n-icon>
                                          <component :is="element.selected ? EyeOutline : EyeOffOutline" />
                                        </n-icon>
                                      </button>
                                    </div>
                                  </template>
                                </draggable>
                                <span v-if="!crudDescriptionFieldOptions.length" class="custom-action-empty">暂无可选字段</span>
                              </div>
                            </div>
                          </n-popover>
                        </div>
                      </div>
                    </div>
                  </div>
                  <n-empty
                    v-if="!crudDescriptionFieldOptions.length"
                    size="small"
                    description="暂无可选字段"
                  />
                </div>
              </template>
            </section>

            <section class="panel-item">
              <div class="panel-item-title">
                编辑弹窗
                <small class="panel-item-hint">配置已移至「表单属性 → 表单项配置」</small>
              </div>
              <div class="crud-readonly-summary">
                <span>打开方式：{{ schema.layout?.formOpenMode || schema.layout?.modalType || 'modal' }}</span>
                <span>弹窗宽度：{{ schema.layout?.modalWidth || '800px' }}</span>
                <span>表单列数：{{ normalizedFormGridColumns }}</span>
              </div>
              <n-form-item label="每页条数">
                <n-input-number
                  :value="crudOptions.pageSize || 10"
                  :min="1"
                  :max="200"
                  @update:value="updateCrudOption('pageSize', $event || 10)"
                />
              </n-form-item>
            </section>

            <section class="panel-item">
              <div class="panel-item-title">
                开关
              </div>
              <div class="switch-list">
                <label v-for="item in crudSwitchFields" :key="item.key">
                  <span>{{ item.label }}</span>
                  <n-switch
                    size="small"
                    :value="resolveCrudSwitchValue(item.key, item.defaultValue)"
                    @update:value="updateCrudOption(item.key, $event)"
                  />
                </label>
              </div>
            </section>
          </n-form>
        </n-drawer-content>
      </n-drawer>

      <n-drawer
        v-if="isCrudBlock"
        v-model:show="crudFieldDrawerVisible"
        :width="420"
        placement="right"
        :trap-focus="false"
        :block-scroll="false"
      >
        <n-drawer-content :title="`${editingCrudField?.label || '字段'} 配置`" closable>
          <n-form label-placement="top" :show-feedback="false" class="property-form drawer-property-form">
            <section class="panel-item">
              <div class="panel-item-title">
                查询条件
              </div>
              <n-form-item label="查询标签">
                <n-input
                  :value="editingCrudConfig.search?.label || ''"
                  clearable
                  placeholder="默认使用字段名称"
                  @update:value="updateEditingCrudFieldConfig('search', { label: $event || undefined })"
                />
              </n-form-item>
              <n-form-item label="查询占位提示">
                <n-input
                  :value="editingCrudConfig.search?.placeholder || ''"
                  clearable
                  placeholder="默认使用字段占位提示"
                  @update:value="updateEditingCrudFieldConfig('search', { placeholder: $event || undefined })"
                />
              </n-form-item>
              <n-form-item label="查询控件跨度">
                <n-input-number
                  :value="editingCrudConfig.search?.span || editingCrudField?.layout?.span || 1"
                  :min="1"
                  :max="maxFormGridColumns"
                  @update:value="updateEditingCrudFieldConfig('search', { span: $event || 1 })"
                />
              </n-form-item>
            </section>

            <section class="panel-item">
              <div class="panel-item-title">
                表格列
              </div>
              <n-form-item label="列标题">
                <n-input
                  :value="editingCrudConfig.table?.title || ''"
                  clearable
                  placeholder="默认使用字段名称"
                  @update:value="updateEditingCrudFieldConfig('table', { title: $event || undefined })"
                />
              </n-form-item>
              <div class="crud-inline-grid">
                <n-form-item label="列宽">
                  <n-input-number
                    :value="editingCrudConfig.table?.width"
                    clearable
                    :min="60"
                    :max="800"
                    @update:value="updateEditingCrudFieldConfig('table', { width: $event || undefined })"
                  />
                </n-form-item>
                <n-form-item label="最小宽度">
                  <n-input-number
                    :value="editingCrudConfig.table?.minWidth || 120"
                    :min="60"
                    :max="800"
                    @update:value="updateEditingCrudFieldConfig('table', { minWidth: $event || undefined })"
                  />
                </n-form-item>
                <n-form-item label="对齐方式">
                  <n-select
                    :value="editingCrudConfig.table?.align || 'left'"
                    :options="tableAlignOptions"
                    @update:value="updateEditingCrudFieldConfig('table', { align: $event || undefined })"
                  />
                </n-form-item>
                <n-form-item label="固定列">
                  <n-select
                    :value="editingCrudConfig.table?.fixed || ''"
                    :options="tableFixedOptions"
                    @update:value="updateEditingCrudFieldConfig('table', { fixed: $event || undefined })"
                  />
                </n-form-item>
              </div>
              <div class="switch-list">
                <label>
                  <span>文字省略</span>
                  <n-switch
                    size="small"
                    :value="editingCrudConfig.table?.ellipsis !== false"
                    @update:value="updateEditingCrudFieldConfig('table', { ellipsis: $event })"
                  />
                </label>
                <label>
                  <span>可排序</span>
                  <n-switch
                    size="small"
                    :value="!!editingCrudConfig.table?.sorter"
                    @update:value="updateEditingCrudFieldConfig('table', { sorter: $event })"
                  />
                </label>
              </div>
            </section>

            <section class="panel-item">
              <div class="panel-item-title">
                编辑弹窗
              </div>
              <n-form-item label="编辑标签">
                <n-input
                  :value="editingCrudConfig.edit?.label || ''"
                  clearable
                  placeholder="默认使用字段名称"
                  @update:value="updateEditingCrudFieldConfig('edit', { label: $event || undefined })"
                />
              </n-form-item>
              <n-form-item label="编辑占位提示">
                <n-input
                  :value="editingCrudConfig.edit?.placeholder || ''"
                  clearable
                  placeholder="默认使用字段占位提示"
                  @update:value="updateEditingCrudFieldConfig('edit', { placeholder: $event || undefined })"
                />
              </n-form-item>
              <n-form-item label="编辑控件跨度">
                <n-input-number
                  :value="editingCrudConfig.edit?.span || editingCrudField?.layout?.span || 1"
                  :min="1"
                  :max="maxFormGridColumns"
                  @update:value="updateEditingCrudFieldConfig('edit', { span: $event || 1 })"
                />
              </n-form-item>
              <div class="switch-list">
                <label>
                  <span>只读</span>
                  <n-switch
                    size="small"
                    :value="!!editingCrudConfig.edit?.readonly"
                    @update:value="updateEditingCrudFieldConfig('edit', { readonly: $event })"
                  />
                </label>
              </div>
            </section>
          </n-form>
        </n-drawer-content>
      </n-drawer>

      <n-drawer
        v-model:show="componentPropsVisible"
        :width="380"
        placement="right"
        :trap-focus="false"
        :block-scroll="false"
      >
        <n-drawer-content :title="`${selectedLabel} 更多属性`" closable>
          <n-form label-placement="top" :show-feedback="false" class="property-form drawer-property-form">
            <section v-for="propGroup in activePropGroups" :key="propGroup.title" class="panel-item">
              <div class="panel-item-title">
                {{ propGroup.title }}
              </div>
              <n-form-item v-for="item in propGroup.fields" :key="item.key" :label="item.label || item.key">
                <n-switch
                  v-if="item.type === 'boolean'"
                  size="small"
                  :value="resolvePropValue(item)"
                  @update:value="updateSelectedProp(item, $event)"
                />
                <n-input-number
                  v-else-if="item.type === 'number'"
                  :value="resolvePropValue(item)"
                  clearable
                  :min="item.min"
                  :max="item.max"
                  :step="item.step || 1"
                  @update:value="updateSelectedProp(item, $event)"
                />
                <n-select
                  v-else-if="item.type === 'select'"
                  :value="resolvePropValue(item)"
                  clearable
                  :options="item.options"
                  @update:value="updateSelectedProp(item, $event)"
                />
                <n-select
                  v-else-if="item.type === 'multiSelect'"
                  multiple
                  clearable
                  :value="resolvePropValue(item) || []"
                  :options="item.options"
                  @update:value="updateSelectedProp(item, $event?.length ? $event : undefined)"
                />
                <n-input
                  v-else-if="item.type === 'json'"
                  :value="stringifyJsonProp(resolvePropValue(item))"
                  type="textarea"
                  :autosize="{ minRows: 3, maxRows: 8 }"
                  placeholder="JSON"
                  @update:value="updateJsonProp(item, $event)"
                />
                <n-input
                  v-else
                  :value="resolvePropValue(item)"
                  clearable
                  :placeholder="item.placeholder"
                  @update:value="updateSelectedProp(item, $event || undefined)"
                />
              </n-form-item>
            </section>
          </n-form>
        </n-drawer-content>
      </n-drawer>
    </template>

    <n-tabs v-else v-model:value="formPropertyActiveTab" type="line" size="medium" animated class="property-tabs form-property-tabs">
      <n-tab-pane name="basic">
        <template #tab>
          <span class="property-tab-label">
            <n-icon><LayersOutline /></n-icon>
            表单属性
          </span>
        </template>
        <n-form label-placement="top" :show-feedback="false" class="property-form">
          <n-collapse v-model:expanded-names="formBasicExpandedNames" class="form-property-collapse">
            <n-collapse-item title="表单资产" name="assets">
              <section class="panel-item form-asset-panel">
                <div class="panel-title-row">
                  <div>
                    <div class="panel-item-title">
                      表单资产
                    </div>
                    <p class="panel-item-desc">
                      一个业务对象可以维护多个表单，弹窗、详情和流程可以引用不同表单。
                    </p>
                  </div>
                  <div class="form-asset-actions">
                    <button type="button" class="form-asset-icon-button" title="复制当前表单" @click="duplicateCurrentFormAsset">
                      <svg width="1em" height="1em" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                        <path d="M9 3a1 1 0 0 1 1-1h10a1 1 0 0 1 1 1v12a1 1 0 1 1-2 0V4h-9a1 1 0 0 1-1-1Z" fill="currentColor" />
                        <path d="M5 6a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2H5Zm0 2h10v12H5V8Z" fill="currentColor" />
                      </svg>
                    </button>
                    <button type="button" class="form-asset-icon-button" title="新建空白表单" @click="createBlankFormAsset">
                      <svg width="1em" height="1em" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                        <path d="M11 4a1 1 0 1 1 2 0v7h7a1 1 0 1 1 0 2h-7v7a1 1 0 1 1-2 0v-7H4a1 1 0 1 1 0-2h7V4Z" fill="currentColor" />
                      </svg>
                    </button>
                  </div>
                </div>
                <div :key="schema.formKey" class="current-form-banner">
                  <span>当前编辑</span>
                  <strong>{{ schema.formName || '主表单' }}</strong>
                </div>
                <div class="form-asset-tabs">
                  <button type="button" class="form-asset-tab active" @click="showFormSettings">
                    <em>1</em>
                    <span>{{ schema.formName || '主表单' }}</span>
                    <strong>当前</strong>
                  </button>
                  <button
                    v-for="(asset, assetIndex) in formAssets"
                    :key="asset.formKey"
                    type="button"
                    class="form-asset-tab"
                    @click="switchFormAsset(asset.formKey)"
                  >
                    <em>{{ assetIndex + 2 }}</em>
                    <span>{{ asset.formName || `表单 ${assetIndex + 2}` }}</span>
                  </button>
                </div>
                <div class="form-asset-edit-grid">
                  <n-form-item label="当前表单名称">
                    <n-input
                      :value="schema.formName || ''"
                      placeholder="请输入表单名称"
                      @update:value="updateCurrentFormMeta({ formName: $event || '业务表单' })"
                    />
                  </n-form-item>
                  <n-form-item label="当前表单编码">
                    <n-input
                      :value="schema.formKey || ''"
                      placeholder="form_key"
                      @update:value="updateCurrentFormMeta({ formKey: $event || schema.formKey })"
                    />
                  </n-form-item>
                  <n-form-item label="当前表单用途">
                    <n-select
                      :value="schema.usage || ['create', 'edit']"
                      :options="formUsageOptions"
                      multiple
                      clearable
                      placeholder="选择用途"
                      @update:value="updateCurrentFormMeta({ usage: $event })"
                    />
                  </n-form-item>
                  <n-form-item label="默认表单">
                    <span v-if="defaultFormKey === schema.formKey" class="form-default-badge">
                      默认表单
                    </span>
                    <n-button
                      v-else
                      size="small"
                      secondary
                      @click="setDefaultFormKey(schema.formKey)"
                    >
                      设为默认
                    </n-button>
                  </n-form-item>
                </div>
                <div v-if="formAssets.length" class="form-asset-list">
                  <div class="form-asset-list-title">
                    其他表单维护
                  </div>
                  <div v-for="asset in formAssets" :key="asset.formKey" class="form-asset-card">
                    <div class="form-asset-main">
                      <n-input
                        :value="asset.formName"
                        size="small"
                        placeholder="表单名称"
                        @update:value="updateFormAssetMeta(asset.formKey, { formName: $event || '未命名表单' })"
                      />
                      <n-select
                        :value="asset.usage || ['create', 'edit']"
                        :options="formUsageOptions"
                        multiple
                        clearable
                        size="small"
                        placeholder="用途"
                        @update:value="updateFormAssetMeta(asset.formKey, { usage: $event })"
                      />
                    </div>
                    <n-button size="tiny" quaternary :type="defaultFormKey === asset.formKey ? 'primary' : 'default'" @click="setDefaultFormKey(asset.formKey)">
                      {{ defaultFormKey === asset.formKey ? '默认' : '设默认' }}
                    </n-button>
                    <n-button size="tiny" quaternary type="error" @click="removeFormAsset(asset.formKey)">
                      删除
                    </n-button>
                  </div>
                </div>
              </section>
            </n-collapse-item>

            <n-collapse-item title="表单项配置" name="layout">
              <section class="panel-item form-item-config">
                <div class="compact-config-row">
                  <label>编辑打开方式</label>
                  <n-select
                    :value="schema.layout?.formOpenMode || schema.layout?.modalType || 'modal'"
                    :options="formOpenModeOptions"
                    size="small"
                    @update:value="updateFormOpenModeLayout"
                  />
                </div>
                <div class="compact-config-row">
                  <label>弹窗宽度</label>
                  <n-input
                    :value="schema.layout?.modalWidth || '800px'"
                    placeholder="800px / 60vw"
                    size="small"
                    @update:value="updateFormModalWidth"
                  />
                </div>
                <div class="compact-config-row">
                  <label>抽屉方向</label>
                  <n-select
                    :value="schema.layout?.drawerPlacement || 'right'"
                    :options="drawerPlacementOptions"
                    size="small"
                    @update:value="updateFormLayout({ drawerPlacement: $event || 'right' })"
                  />
                </div>
                <div class="form-columns-control">
                  <div class="form-columns-head">
                    <label>表单列数</label>
                    <n-input-number
                      :value="normalizedFormGridColumns"
                      :min="1"
                      :max="maxFormGridColumns"
                      :show-button="false"
                      size="tiny"
                      @update:value="updateFormLayout({ gridColumns: $event || 1, gridCols: $event || 1 })"
                    />
                  </div>
                  <div class="slider-control">
                    <n-slider
                      :value="normalizedFormGridColumns"
                      :min="1"
                      :max="maxFormGridColumns"
                      :step="1"
                      :marks="gridColumnMarks"
                      @update:value="updateFormLayout({ gridColumns: $event, gridCols: $event })"
                    />
                  </div>
                </div>
                <div class="compact-config-row">
                  <label>表单大小</label>
                  <div class="segmented-mini three form-config-segment">
                    <button
                      type="button"
                      :class="{ active: (schema.layout?.size || 'medium') === 'small' }"
                      @click="updateFormLayout({ size: 'small' })"
                    >
                      小
                    </button>
                    <button
                      type="button"
                      :class="{ active: (schema.layout?.size || 'medium') === 'medium' }"
                      @click="updateFormLayout({ size: 'medium' })"
                    >
                      中
                    </button>
                    <button
                      type="button"
                      :class="{ active: (schema.layout?.size || 'medium') === 'large' }"
                      @click="updateFormLayout({ size: 'large' })"
                    >
                      大
                    </button>
                  </div>
                </div>
                <div class="compact-config-row">
                  <label>标签位置</label>
                  <div class="segmented-mini form-config-segment">
                    <button
                      type="button"
                      :class="{ active: (schema.layout?.labelPlacement || 'left') === 'left' }"
                      @click="updateFormLayout({ labelPlacement: 'left' })"
                    >
                      左侧
                    </button>
                    <button
                      type="button"
                      :class="{ active: (schema.layout?.labelPlacement || 'left') === 'top' }"
                      @click="updateFormLayout({ labelPlacement: 'top' })"
                    >
                      顶部
                    </button>
                  </div>
                </div>
                <div class="compact-config-row">
                  <label>标签对齐</label>
                  <div class="segmented-mini form-config-segment">
                    <button
                      type="button"
                      :class="{ active: (schema.layout?.labelAlign || 'right') === 'left' }"
                      @click="updateFormLayout({ labelAlign: 'left' })"
                    >
                      左对齐
                    </button>
                    <button
                      type="button"
                      :class="{ active: (schema.layout?.labelAlign || 'right') === 'right' }"
                      @click="updateFormLayout({ labelAlign: 'right' })"
                    >
                      右对齐
                    </button>
                  </div>
                </div>
                <div class="compact-config-row">
                  <label>标签宽度</label>
                  <div class="label-width-control">
                    <n-input
                      :value="String(schema.layout?.labelWidth ?? 'auto')"
                      placeholder="auto / 100"
                      size="small"
                      @update:value="updateFormLayout({ labelWidth: normalizeLabelWidthInput($event) })"
                    />
                    <em>px</em>
                  </div>
                </div>
              </section>
            </n-collapse-item>

            <n-collapse-item title="表单权限控制" name="permissions">
              <section class="panel-item form-permission-panel">
                <div class="compact-field">
                  <label>查看权限码</label>
                  <n-input
                    :value="formPermissionConfig.viewPermission || ''"
                    clearable
                    placeholder="例如: ai:business:customer:query"
                    size="small"
                    @update:value="updateFormPermission({ viewPermission: $event || '' })"
                  />
                </div>
                <div class="compact-field">
                  <label>编辑权限码</label>
                  <n-input
                    :value="formPermissionConfig.editPermission || ''"
                    clearable
                    placeholder="例如: ai:business:customer:edit"
                    size="small"
                    @update:value="updateFormPermission({ editPermission: $event || '' })"
                  />
                </div>
                <div class="field-permission-rules">
                  <div class="field-permission-head">
                    <span>字段权限覆盖</span>
                    <n-button size="tiny" text type="primary" @click="addFormFieldRule">
                      添加
                    </n-button>
                  </div>
                  <div v-for="(rule, idx) in formFieldRuleRows" :key="rule.id || idx" class="field-permission-card">
                    <n-select
                      :value="rule.field || ''"
                      :options="formFieldOptions"
                      filterable
                      clearable
                      placeholder="选择字段"
                      size="small"
                      @update:value="updateFormFieldRule(idx, { field: $event || '' })"
                    />
                    <div class="field-rule-switches">
                      <label>
                        <span>必填</span>
                        <n-switch size="small" :value="!!rule.required" @update:value="updateFormFieldRule(idx, { required: $event })" />
                      </label>
                      <label>
                        <span>只读</span>
                        <n-switch size="small" :value="!!rule.readonly" @update:value="updateFormFieldRule(idx, { readonly: $event })" />
                      </label>
                      <label>
                        <span>隐藏</span>
                        <n-switch size="small" :value="!!rule.hidden" @update:value="updateFormFieldRule(idx, { hidden: $event })" />
                      </label>
                    </div>
                    <div class="field-permission-footer">
                      <n-input
                        :value="rule.defaultValue ?? ''"
                        clearable
                        placeholder="默认值"
                        size="small"
                        @update:value="updateFormFieldRule(idx, { defaultValue: $event })"
                      />
                      <n-button size="tiny" quaternary type="error" @click="removeFormFieldRule(idx)">
                        删除
                      </n-button>
                    </div>
                  </div>
                </div>
              </section>
            </n-collapse-item>

            <n-collapse-item title="表单事件与生命周期" name="events">
              <section class="panel-item form-lifecycle-panel">
                <div v-for="(eventItem, idx) in formEventRows" :key="eventItem.id || idx" class="lifecycle-event-card">
                  <button type="button" class="event-delete-icon" title="删除事件" @click="removeFormEvent(idx)">
                    ×
                  </button>
                  <div class="compact-field event-type-field">
                    <label>事件类型 / 默认值</label>
                    <n-select
                      :value="eventItem.hook || 'beforeLoad'"
                      :options="formEventHookOptions"
                      placeholder="触发时机"
                      size="small"
                      @update:value="updateFormEvent(idx, { hook: $event || 'beforeLoad' })"
                    />
                  </div>
                  <div class="compact-field">
                    <label>脚本名 / 接口地址 / 动作编码</label>
                    <n-input
                      :value="eventItem.handler || ''"
                      clearable
                      placeholder="例如: /api/v1/customer/init"
                      size="small"
                      @update:value="updateFormEvent(idx, { handler: $event || '' })"
                    />
                  </div>
                  <div class="compact-field">
                    <label>结果回填</label>
                    <n-input
                      :value="eventItem.resultMapping || ''"
                      clearable
                      placeholder="如: data.name->name,total->total"
                      size="small"
                      @update:value="updateFormEvent(idx, { resultMapping: $event || '' })"
                    />
                  </div>
                </div>
                <n-button size="small" dashed block type="primary" @click="addFormEvent">
                  + 添加表单事件
                </n-button>
              </section>
            </n-collapse-item>

            <n-collapse-item title="校验规则" name="spacing">
              <section class="panel-item">
                <div class="panel-item-title">
                  校验规则
                </div>
                <n-form-item label="行间距">
                  <n-input-number
                    :value="schema.layout?.rowGap || 16"
                    :min="0"
                    :max="48"
                    @update:value="updateFormLayout({ rowGap: $event || 16, yGap: $event || 16 })"
                  />
                </n-form-item>
                <n-form-item label="列间距">
                  <n-input-number
                    :value="schema.layout?.columnGap || 16"
                    :min="0"
                    :max="48"
                    @update:value="updateFormLayout({ columnGap: $event || 16, xGap: $event || 16 })"
                  />
                </n-form-item>
                <div class="switch-list">
                  <label>
                    <span>显示校验反馈</span>
                    <n-switch
                      size="small"
                      :value="schema.layout?.showFeedback !== false"
                      @update:value="updateFormLayout({ showFeedback: $event })"
                    />
                  </label>
                  <label>
                    <span>隐藏必填星号</span>
                    <n-switch
                      size="small"
                      :value="!!schema.layout?.hideRequiredAsterisk"
                      @update:value="updateFormLayout({ hideRequiredAsterisk: $event })"
                    />
                  </label>
                  <label>
                    <span>行内反馈</span>
                    <n-switch
                      size="small"
                      :value="!!schema.layout?.inlineFeedback"
                      @update:value="updateFormLayout({ inlineFeedback: $event })"
                    />
                  </label>
                  <label>
                    <span>启用折叠</span>
                    <n-switch
                      size="small"
                      :value="!!schema.layout?.enableCollapse"
                      @update:value="updateFormLayout({ enableCollapse: $event })"
                    />
                  </label>
                </div>
                <n-form-item label="最大显示字段数">
                  <n-input-number
                    :value="schema.layout?.maxVisibleFields || 6"
                    :min="1"
                    :max="50"
                    @update:value="updateFormLayout({ maxVisibleFields: $event || 6 })"
                  />
                </n-form-item>
              </section>
            </n-collapse-item>

            <n-collapse-item title="操作按钮" name="actions">
              <section class="panel-item">
                <div class="panel-item-title">
                  操作按钮
                </div>
                <div class="switch-list">
                  <label>
                    <span>显示操作区</span>
                    <n-switch
                      size="small"
                      :value="schema.layout?.showActions !== false"
                      @update:value="updateFormLayout({ showActions: $event })"
                    />
                  </label>
                  <label>
                    <span>提交按钮</span>
                    <n-switch
                      size="small"
                      :value="schema.layout?.showSubmit !== false"
                      @update:value="updateFormLayout({ showSubmit: $event })"
                    />
                  </label>
                  <label>
                    <span>重置按钮</span>
                    <n-switch
                      size="small"
                      :value="schema.layout?.showReset !== false"
                      @update:value="updateFormLayout({ showReset: $event })"
                    />
                  </label>
                  <label>
                    <span>取消按钮</span>
                    <n-switch
                      size="small"
                      :value="!!schema.layout?.showCancel"
                      @update:value="updateFormLayout({ showCancel: $event })"
                    />
                  </label>
                </div>
                <div class="crud-inline-grid form-action-text-grid">
                  <n-form-item label="提交文案">
                    <n-input
                      :value="schema.layout?.submitText || '提交'"
                      placeholder="提交"
                      @update:value="updateFormLayout({ submitText: $event || '提交' })"
                    />
                  </n-form-item>
                  <n-form-item label="重置文案">
                    <n-input
                      :value="schema.layout?.resetText || '重置'"
                      placeholder="重置"
                      @update:value="updateFormLayout({ resetText: $event || '重置' })"
                    />
                  </n-form-item>
                  <n-form-item label="取消文案">
                    <n-input
                      :value="schema.layout?.cancelText || '取消'"
                      placeholder="取消"
                      @update:value="updateFormLayout({ cancelText: $event || '取消' })"
                    />
                  </n-form-item>
                </div>
              </section>
            </n-collapse-item>
          </n-collapse>
        </n-form>
      </n-tab-pane>

      <n-tab-pane name="style">
        <template #tab>
          <span class="property-tab-label">
            <n-icon><ColorPaletteOutline /></n-icon>
            样式
          </span>
        </template>
        <n-form label-placement="top" :show-feedback="false" class="property-form">
          <n-collapse v-model:expanded-names="formStyleExpandedNames" class="form-property-collapse">
            <n-collapse-item title="位置与尺寸" name="position">
              <section class="panel-item position-control">
                <div class="position-axis-grid">
                  <label class="position-number-field">
                    <span>左 X</span>
                    <n-input-number
                      :value="formTranslate.x"
                      size="small"
                      :show-button="false"
                      @update:value="updateFormTranslate('x', $event)"
                    />
                    <em>px</em>
                  </label>
                  <label class="position-number-field">
                    <span>上 Y</span>
                    <n-input-number
                      :value="formTranslate.y"
                      size="small"
                      :show-button="false"
                      @update:value="updateFormTranslate('y', $event)"
                    />
                    <em>px</em>
                  </label>
                </div>
                <div class="position-rule">
                  <div class="position-rule-head">
                    <span>页面宽度</span>
                    <label class="position-inline-number">
                      <n-input-number
                        :value="resolvePxNumber(formStyle.maxWidth, 960)"
                        size="tiny"
                        :min="320"
                        :show-button="false"
                        @update:value="updateFormStyle({ maxWidth: valueToPx($event) })"
                      />
                      <em>px</em>
                    </label>
                  </div>
                  <div class="segmented-mini">
                    <button
                      type="button"
                      :class="{ active: formStyle.maxWidth !== '100%' }"
                      @click="updateFormStyle({ maxWidth: formStyle.maxWidth === '100%' ? '960px' : formStyle.maxWidth || '960px' })"
                    >
                      默认宽度
                    </button>
                    <button
                      type="button"
                      :class="{ active: formStyle.maxWidth === '100%' }"
                      @click="updateFormStyle({ maxWidth: '100%' })"
                    >
                      填充容器
                    </button>
                  </div>
                </div>
                <div class="position-rule">
                  <div class="position-rule-head">
                    <span>页面高度</span>
                    <label class="position-inline-number">
                      <n-input-number
                        :value="resolvePxNumber(formStyle.minHeight, 320)"
                        size="tiny"
                        :min="0"
                        :show-button="false"
                        @update:value="updateFormStyle({ minHeight: valueToPx($event) })"
                      />
                      <em>px</em>
                    </label>
                  </div>
                  <div class="segmented-mini">
                    <button
                      type="button"
                      :class="{ active: formStyle.minHeight !== 'auto' }"
                      @click="updateFormStyle({ minHeight: formStyle.minHeight === 'auto' ? '320px' : formStyle.minHeight || '320px' })"
                    >
                      默认高度
                    </button>
                    <button
                      type="button"
                      :class="{ active: formStyle.minHeight === 'auto' }"
                      @click="updateFormStyle({ minHeight: 'auto' })"
                    >
                      适应内容
                    </button>
                  </div>
                </div>
              </section>
            </n-collapse-item>

            <n-collapse-item title="布局与边距" name="spacing">
              <section class="panel-item">
                <div class="panel-item-title">
                  布局与边距
                </div>
                <div class="spacing-editor">
                  <div class="spacing-editor-title">
                    Padding
                  </div>
                  <div class="spacing-grid">
                    <label v-for="item in spacingSides" :key="`form-padding-${item.key}`">
                      <span>{{ item.label }}</span>
                      <n-input-number
                        :value="resolvePxNumber(formStyle[`padding${item.key}`], 0)"
                        :min="0"
                        :max="120"
                        :show-button="false"
                        size="small"
                        @update:value="updateFormSpacing(`padding${item.key}`, $event)"
                      />
                    </label>
                  </div>
                </div>
                <div class="spacing-editor">
                  <div class="spacing-editor-title">
                    Margin
                  </div>
                  <div class="spacing-grid">
                    <label v-for="item in spacingSides" :key="`form-margin-${item.key}`">
                      <span>{{ item.label }}</span>
                      <n-input-number
                        :value="resolvePxNumber(formStyle[`margin${item.key}`], 0)"
                        :min="-80"
                        :max="120"
                        :show-button="false"
                        size="small"
                        @update:value="updateFormSpacing(`margin${item.key}`, $event)"
                      />
                    </label>
                  </div>
                </div>
              </section>
            </n-collapse-item>

            <n-collapse-item title="文字排版" name="typography">
              <section class="panel-item">
                <div class="panel-item-title">
                  文字排版
                </div>
                <div class="crud-inline-grid">
                  <n-form-item label="字号">
                    <n-input-number
                      :value="resolvePxNumber(formStyle.fontSize, 14)"
                      :min="10"
                      :max="48"
                      :show-button="false"
                      @update:value="updateFormStyle({ fontSize: valueToPx($event) })"
                    />
                  </n-form-item>
                  <n-form-item label="行高">
                    <n-input
                      :value="formStyle.lineHeight || ''"
                      clearable
                      placeholder="1.5 / 22px"
                      @update:value="updateFormStyle({ lineHeight: $event || undefined })"
                    />
                  </n-form-item>
                </div>
                <n-form-item label="文字颜色">
                  <div class="color-control">
                    <n-color-picker
                      :value="formStyle.color || ''"
                      :show-alpha="true"
                      :modes="['hex']"
                      :swatches="colorSwatches"
                      @update:value="updateFormStyle({ color: $event || undefined })"
                    />
                    <n-button size="small" quaternary @click="updateFormStyle({ color: undefined })">
                      默认
                    </n-button>
                  </div>
                </n-form-item>
              </section>
            </n-collapse-item>

            <n-collapse-item title="外观与装饰" name="appearance">
              <section class="panel-item appearance-control">
                <div class="appearance-field">
                  <label>背景色</label>
                  <div class="appearance-input-shell">
                    <label class="appearance-swatch" :style="{ backgroundColor: formAppearanceBackgroundPreview }" title="选择背景色">
                      <input
                        type="color"
                        :value="formAppearanceBackgroundColorInput"
                        @input="updateFormAppearanceBackground($event.target.value)"
                      >
                    </label>
                    <input
                      :value="formAppearanceBackgroundHex"
                      class="appearance-hex-input"
                      placeholder="透明"
                      @input="updateFormAppearanceBackground($event.target.value)"
                    >
                    <span class="appearance-percent">{{ formOpacityPercent }}%</span>
                  </div>
                </div>
                <div class="appearance-field">
                  <label>边框 (Border)</label>
                  <div class="appearance-input-shell">
                    <select
                      :value="formStyle.borderStyle || 'solid'"
                      class="appearance-select"
                      @change="updateFormStyle({ borderStyle: $event.target.value || undefined })"
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
                    <label class="appearance-swatch" :style="{ backgroundColor: formAppearanceBorderPreview }" title="选择边框颜色">
                      <input
                        type="color"
                        :value="formAppearanceBorderPreview"
                        @input="updateFormAppearanceBorder($event.target.value)"
                      >
                    </label>
                    <input
                      :value="formAppearanceBorderHex"
                      class="appearance-hex-input"
                      placeholder="E4E4E7"
                      @input="updateFormAppearanceBorder($event.target.value)"
                    >
                  </div>
                </div>
                <div class="appearance-field">
                  <label>圆角 (Border Radius)</label>
                  <div class="appearance-radius-shell">
                    <span>R</span>
                    <input
                      :value="resolvePxNumber(formStyle.borderRadius, 0)"
                      type="number"
                      min="0"
                      max="32"
                      @input="updateFormStyle({ borderRadius: `${$event.target.value || 0}px` })"
                    >
                  </div>
                </div>
                <div class="appearance-field">
                  <label class="appearance-row-label">
                    <span>阴影 (Shadow)</span>
                    <select
                      :value="formStyle.boxShadow || ''"
                      class="appearance-plain-select"
                      @change="updateFormStyle({ boxShadow: $event.target.value || undefined })"
                    >
                      <option
                        v-for="option in shadowOptions"
                        :key="option.value || 'none'"
                        :value="option.value"
                      >
                        {{ option.label }}
                      </option>
                    </select>
                  </label>
                </div>
                <div class="appearance-field">
                  <label>透明度 (Opacity)</label>
                  <div class="appearance-radius-shell">
                    <span>%</span>
                    <input
                      :value="formOpacityPercent"
                      type="number"
                      min="20"
                      max="100"
                      step="5"
                      @input="updateFormAppearanceOpacity($event.target.value)"
                    >
                  </div>
                </div>
              </section>
            </n-collapse-item>

            <n-collapse-item title="自定义样式" name="custom-style">
              <section class="panel-item">
                <div class="panel-item-title">
                  自定义样式
                </div>
                <n-form-item label="表单 Class">
                  <n-input
                    :value="schema.layout?.formClass"
                    clearable
                    placeholder="自定义 className"
                    @update:value="updateFormLayout({ formClass: $event || undefined })"
                  />
                </n-form-item>
                <n-form-item label="CSS Style">
                  <n-input
                    :value="schema.layout?.formStyleText || stringifyStyle(formStyle)"
                    type="textarea"
                    :autosize="{ minRows: 3, maxRows: 6 }"
                    placeholder="例如 padding:12px; background:#fff;"
                    @update:value="updateFormStyleText"
                  />
                </n-form-item>
              </section>
            </n-collapse-item>
          </n-collapse>
        </n-form>
      </n-tab-pane>

      <n-tab-pane v-if="false" name="source">
        <template #tab>
          <span class="property-tab-label">
            <n-icon><CodeSlashOutline /></n-icon>
            源码
          </span>
        </template>
        <n-form label-placement="top" :show-feedback="false" class="property-form">
          <section class="panel-item source-panel">
            <div class="panel-title-row">
              <div>
                <div class="panel-item-title">
                  表单 Schema JSON
                </div>
                <div class="source-path">
                  {{ schema.formKey || 'formDesignerSchema' }}
                </div>
              </div>
              <div class="source-actions">
                <n-button size="tiny" tertiary @click="resetSchemaCodeDraft">
                  重置
                </n-button>
                <n-button size="tiny" type="primary" secondary @click="applySchemaCode">
                  应用
                </n-button>
              </div>
            </div>
            <n-input
              :value="schemaCodeText"
              type="textarea"
              class="source-editor"
              :autosize="{ minRows: 18, maxRows: 32 }"
              @update:value="updateSchemaCodeDraft"
            />
            <div v-if="sourceError" class="source-error">
              {{ sourceError }}
            </div>
          </section>
        </n-form>
      </n-tab-pane>
    </n-tabs>

    <n-modal
      v-model:show="fieldFormulaPanelVisible"
      preset="card"
      class="field-formula-modal"
      :bordered="false"
      :mask-closable="false"
      style="width: min(1120px, calc(100vw - 40px))"
      :title="`字段公式：${selectedFieldAsset?.fieldName || selectedFieldCode || '未绑定字段'}`"
    >
      <BusinessFieldPropertyPanel
        v-if="selectedFieldAsset"
        class="field-formula-property-panel"
        :field="selectedFieldAsset"
        :all-fields="formulaPanelFields"
        :relations="relations"
        :object-code="objectCode"
        default-active-tab="formula"
        mode="formula"
        @save="handleFieldAssetSave"
      />
    </n-modal>

    <n-modal v-model:show="sourceModalVisible" preset="card" class="form-source-modal" :bordered="false" title="源码编辑">
      <section class="panel-item source-panel">
        <div class="panel-title-row">
          <div>
            <div class="panel-item-title">
              {{ selectedComponent ? '当前组件 JSON' : '表单 Schema JSON' }}
            </div>
            <div class="source-path">
              {{ selectedComponent ? selectedComponent.id : (schema.formKey || 'formDesignerSchema') }}
            </div>
          </div>
          <div class="source-actions">
            <n-button size="tiny" tertiary @click="selectedComponent ? resetSelectedCodeDraft() : resetSchemaCodeDraft()">
              重置
            </n-button>
          </div>
        </div>
        <div class="source-editor-hint">
          支持实时编辑，并保存应用到画布
        </div>
        <n-input
          :value="selectedComponent ? selectedCodeText : schemaCodeText"
          type="textarea"
          class="source-editor"
          :autosize="{ minRows: 18, maxRows: 32 }"
          @update:value="value => selectedComponent ? updateSelectedCodeDraft(value) : updateSchemaCodeDraft(value)"
        />
        <div v-if="sourceError" class="source-error">
          {{ sourceError }}
        </div>
      </section>
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
  CodeSlashOutline,
  ColorPaletteOutline,
  EyeOffOutline,
  EyeOutline,
  FlashOutline,
  GridOutline,
  LayersOutline,
  ServerOutline,
  SettingsOutline,
  ToggleOutline,
} from '@vicons/ionicons5'
import { computed, h, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import draggable from 'vuedraggable'
import { businessObjectDesigner, businessObjectList, codeRuleList, previewCodeRule } from '@/api/business-app'
import DictTypeSelect from '@/components/lowcode-builder/shared/DictTypeSelect.vue'
import { pageWidgetComponentKeys } from '@/components/lowcode-builder/shared/page-widget-schema'
import RuntimeRulesEditor from '@/components/lowcode-builder/shared/RuntimeRulesEditor.vue'
import { getDictData } from '@/composables/useDict'
import { COMMON_VALIDATION_PRESETS, getValidationPreset } from '@/utils/validation-presets'
import BusinessFieldPropertyPanel from '../BusinessFieldPropertyPanel.vue'
import { cloneValue, findDesignerComponentPath, getDesignerComponent, isFieldComponent, isLayoutComponent, normalizeFormDesignerSchema, updateDesignerComponent, updateDesignerLayout } from '../form-first/formDesignerSchema'
import { camelToSnake } from '../form-first/namingUtils'

const props = defineProps({
  schema: {
    type: Object,
    required: true,
  },
  selectedId: {
    type: String,
    default: '',
  },
  fields: {
    type: Array,
    default: () => [],
  },
  relations: {
    type: Array,
    default: () => [],
  },
  objectCode: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['update:schema', 'update:selectedId', 'close', 'fieldAssetUpdated'])

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
const BitableNumberIcon = createBitableSvgIcon('BitableNumberIcon', [
  { d: 'M8.774 2.14a1 1 0 0 1 .85 1.129L9.242 6h6.98l.423-3.01a1 1 0 1 1 1.98.279L18.242 6H22a1 1 0 1 1 0 2h-4.04l-.984 7H20a1 1 0 1 1 0 2h-3.305l-.575 4.093a1 1 0 1 1-1.98-.278L14.674 17h-6.98l-.575 4.093a1 1 0 1 1-1.98-.278L5.674 17H2a1 1 0 1 1 0-2h3.956l.984-7H4a1 1 0 1 1 0-2h3.221l.423-3.01a1 1 0 0 1 1.13-.85ZM14.956 15l.984-7H8.96l-.984 7h6.98Z', fill: 'currentColor' },
])
const BitableCalendarIcon = createBitableSvgIcon('BitableCalendarIcon', [
  { d: 'M7 2a1 1 0 0 1 1 1h8a1 1 0 1 1 2 0h2a2 2 0 0 1 2 2v15a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h2a1 1 0 0 1 1-1Zm9 3H8a1 1 0 0 1-2 0H4v15h16V5h-2a1 1 0 1 1-2 0ZM9 15a1 1 0 0 0-1-1H7a1 1 0 0 0-1 1v1a1 1 0 0 0 1 1h1a1 1 0 0 0 1-1v-1Zm1.5-5a1 1 0 0 1 1-1h1a1 1 0 0 1 1 1v1a1 1 0 0 1-1 1h-1a1 1 0 0 1-1-1v-1Zm3 5a1 1 0 0 0-1-1h-1a1 1 0 0 0-1 1v1a1 1 0 0 0 1 1h1a1 1 0 0 0 1-1v-1Zm1.5 0a1 1 0 0 1 1-1h1a1 1 0 0 1 1 1v1a1 1 0 0 1-1 1h-1a1 1 0 0 1-1-1v-1Zm3-5a1 1 0 0 0-1-1h-1a1 1 0 0 0-1 1v1a1 1 0 0 0 1 1h1a1 1 0 0 0 1-1v-1Z', fill: 'currentColor' },
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

const advancedConfigVisible = ref(false)
const componentPropsVisible = ref(false)
const crudFieldDrawerVisible = ref(false)
const fieldFormulaPanelVisible = ref(false)
const crudDescriptionFieldPanelOpen = ref(false)
const editingCrudFieldId = ref('')
const propertyActiveTab = ref('basic')
const formPropertyActiveTab = ref('basic')
const basicExpandedNames = ['identity', 'gridQuick', 'field', 'validation']
const selectedBasicExpandedNames = ref([...basicExpandedNames])
const formBasicExpandedNames = ref(['assets', 'layout', 'permissions', 'events'])
const formStyleExpandedNames = ref(['position', 'layout', 'typography', 'appearance'])
const allSelectedBasicExpandNames = ['identity', 'gridQuick', 'field', 'button', 'options', 'crud-field', 'temporal', 'assist', 'validation']
const allFormBasicExpandNames = ['assets', 'layout', 'permissions', 'events', 'spacing', 'actions']
const allFormStyleExpandNames = ['position', 'spacing', 'typography', 'appearance', 'custom-style']
const commonValidationOptions = COMMON_VALIDATION_PRESETS.map(item => ({
  label: item.label,
  value: item.value,
}))
const propertySearchKeyword = ref('')
const propertySearchHit = ref('')
const selectedCodeDraft = ref('')
const selectedCodeDraftTarget = ref('')
const schemaCodeDraft = ref('')
const schemaCodeDirty = ref(false)
const sourceError = ref('')
const sourceModalVisible = ref(false)
const dictDefaultOptions = ref({})
const dictDefaultOptionsLoading = ref({})
const codeRules = ref([])
const codeRuleLoading = ref(false)
let codeRuleRequestVersion = 0
const codeRulePreview = ref(null)
const codeRulePreviewing = ref(false)
const selectedComponent = computed(() => getDesignerComponent(props.schema, props.selectedId))
const isField = computed(() => selectedComponent.value ? isFieldComponent(selectedComponent.value) : false)
const isLayout = computed(() => selectedComponent.value ? isLayoutComponent(selectedComponent.value) : false)
const isCrudBlock = computed(() => ['AiCrudPage', 'crudBlock'].includes(selectedComponent.value?.componentKey))
const isRowLayout = computed(() => ['row', 'fcRow'].includes(selectedComponent.value?.componentKey))
const isColumnLayout = computed(() => selectedComponent.value?.componentKey === 'col')
const isCardLayout = computed(() => ['card', 'elCard'].includes(selectedComponent.value?.componentKey))
const isTabsLayout = computed(() => ['tabs', 'elTabs'].includes(selectedComponent.value?.componentKey))
const isCollapseLayout = computed(() => ['collapse', 'elCollapse'].includes(selectedComponent.value?.componentKey))
const isButtonComponent = computed(() => ['button', 'elButton'].includes(selectedComponent.value?.componentKey))
const isPageWidget = computed(() => pageWidgetComponentKeys.includes(selectedComponent.value?.componentKey))
const selectedLabel = computed(() => selectedComponent.value?.label || selectedComponent.value?.props?.header || selectedComponent.value?.props?.title || '未命名组件')
const componentTypeLabel = computed(() => isCrudBlock.value ? '系统 AiCrudPage 组件' : isField.value ? 'AiForm 字段组件' : isLayout.value ? '布局组件' : selectedComponent.value?.componentKey || '组件')
const panelDescription = computed(() => selectedComponent.value ? componentTypeLabel.value : 'AiForm / AiCrudPage 通用配置')
const crudApiConfig = computed(() => selectedComponent.value?.props?.apiConfig || {})
const crudOptions = computed(() => selectedComponent.value?.props?.crudOptions || {})
const selectedDesignerStyle = computed(() => selectedComponent.value?.props?.__designerStyle || {})
const formStyle = computed(() => props.schema.layout?.formStyle || {})
const formAssets = computed(() => Array.isArray(props.schema.settings?.formAssets) ? props.schema.settings.formAssets : [])
const defaultFormKey = computed(() => props.schema.defaultFormKey || props.schema.settings?.defaultFormKey || props.schema.formKey)
const formGovernanceSettings = computed(() => props.schema.settings?.governance || {})
const formPermissionConfig = computed(() => formGovernanceSettings.value.permission || {})
const formFieldRuleRows = computed(() => Array.isArray(formGovernanceSettings.value.fieldRules) ? formGovernanceSettings.value.fieldRules : [])
const formEventRows = computed(() => Array.isArray(formGovernanceSettings.value.events) ? formGovernanceSettings.value.events : [])
const formFieldOptions = computed(() => collectBoundFieldOptions(props.schema.components || []))
const selectedFieldCode = computed(() => String(
  selectedComponent.value?.fieldBinding?.fieldCode
  || selectedComponent.value?.field
  || selectedComponent.value?.props?.fieldCode
  || '',
).trim())
const selectedFieldAsset = computed(() => {
  if (!isField.value || !selectedFieldCode.value)
    return null
  const matched = (props.fields || []).find((field) => {
    const code = field?.fieldCode || field?.field
    return code === selectedFieldCode.value
  })
  return normalizeSelectedFieldAsset(matched || createFieldAssetFromSelectedComponent())
})
const switchableComponentGroups = [
  ['input', 'textarea', 'number', 'inputNumber', 'money'],
  ['select', 'radio', 'checkbox', 'dictSelect'],
  ['date', 'datetime'],
  ['fileUpload', 'imageUpload'],
  ['objectReference', 'recordSelector'],
]
const componentTypeLabelMap = {
  input: '单行输入',
  textarea: '多行文本',
  number: '数字输入',
  inputNumber: '数字输入',
  money: '金额',
  select: '下拉选择',
  radio: '单选框',
  checkbox: '多选框',
  dictSelect: '字典选择',
  date: '日期',
  datetime: '日期时间',
  fileUpload: '文件上传',
  imageUpload: '图片上传',
  objectReference: '引用对象',
  recordSelector: '记录选择器',
}
const canSwitchComponentType = computed(() => {
  if (!isField.value || !selectedComponent.value)
    return false
  const key = selectedComponent.value.componentKey
  return switchableComponentGroups.some(group => group.includes(key))
})
const switchableComponentOptions = computed(() => {
  const key = selectedComponent.value?.componentKey
  const group = switchableComponentGroups.find(item => item.includes(key)) || []
  return group.map(value => ({
    label: componentTypeLabelMap[value] || value,
    value,
  }))
})
const supportsFieldMaxLength = computed(() => ['input', 'textarea'].includes(selectedComponent.value?.componentKey))
const selectedFieldMaxLength = computed(() => {
  const fromProps = normalizePositiveInteger(selectedComponent.value?.props?.maxlength)
  if (fromProps)
    return fromProps
  return normalizePositiveInteger(selectedFieldAsset.value?.length)
})
const selectedFormulaConfig = computed(() => selectedFieldAsset.value?.formulaConfig || null)
const selectedFormulaSummary = computed(() => {
  const config = selectedFormulaConfig.value
  if (!config?.type)
    return '字段资产级计算逻辑，可用于金额、状态、跨对象取值等场景。'
  if (config.type === 'LOOKUP')
    return `查找 ${config.lookup?.returnField || '目标字段'}`
  if (config.type === 'AGGREGATE')
    return `${config.aggregate?.function || '聚合'} ${config.aggregate?.targetField || ''}`.trim()
  return config.expression || config.condition?.expression || '已配置公式'
})
const formulaPanelFields = computed(() => {
  const current = selectedFieldAsset.value
  const fields = Array.isArray(props.fields) ? props.fields : []
  if (!current)
    return fields
  let matched = false
  const merged = fields.map((field) => {
    const code = field?.fieldCode || field?.field
    if (code !== current.fieldCode)
      return field
    matched = true
    return { ...field, ...current }
  })
  return matched ? merged : [current, ...merged]
})
const formUsageOptions = [
  { label: '新增', value: 'create' },
  { label: '编辑', value: 'edit' },
  { label: '详情', value: 'detail' },
  { label: '填报', value: 'submit' },
  { label: '审批', value: 'approve' },
  { label: '移动端', value: 'mobile' },
]
const formEventHookOptions = [
  { label: '加载前', value: 'beforeLoad' },
  { label: '加载后', value: 'afterLoad' },
  { label: '提交前', value: 'beforeSubmit' },
  { label: '提交后', value: 'afterSubmit' },
  { label: '字段变化', value: 'fieldChange' },
  { label: '按钮动作', value: 'buttonAction' },
]
const formAssetOptions = computed(() => [
  { label: `${props.schema.formName || '主表单'}（当前表单）`, value: 'current' },
  ...formAssets.value.map(asset => ({
    label: `${asset.formName || asset.formKey}（${asset.formKey}）`,
    value: asset.formKey,
  })),
])
const layoutChildren = computed(() => selectedComponent.value?.children || [])
const interactionRules = computed(() => Array.isArray(selectedComponent.value?.props?.__events) ? selectedComponent.value.props.__events : [])
const defaultTrigger = computed(() => selectedComponent.value?.componentKey === 'button' ? 'click' : 'change')
const componentTargetOptions = computed(() => collectComponentTargetOptions(props.schema.components || []))
const dictTypeFields = computed(() => collectDictTypeFields(props.schema.components || []))
const selectedComponentCodeRaw = computed(() => JSON.stringify(selectedComponent.value || {}, null, 2))
const selectedCodeText = computed(() => selectedCodeDraftTarget.value === props.selectedId ? selectedCodeDraft.value : selectedComponentCodeRaw.value)
const schemaCodeRaw = computed(() => JSON.stringify(props.schema || {}, null, 2))
const schemaCodeText = computed(() => schemaCodeDirty.value ? schemaCodeDraft.value : schemaCodeRaw.value)
const selectedOpacityPercent = computed(() => Math.round(Number(selectedDesignerStyle.value.opacity ?? 1) * 100))
const formOpacityPercent = computed(() => Math.round(Number(formStyle.value.opacity ?? 1) * 100))
const selectedDesignerTranslate = computed(() => parseTranslateStyle(selectedDesignerStyle.value.customStyle?.transform))
const formTranslate = computed(() => parseTranslateStyle(formStyle.value.transform))
const selectedAppearanceBackgroundHex = computed(() => colorToHexInput(selectedDesignerStyle.value.backgroundColor, ''))
const selectedAppearanceBorderHex = computed(() => colorToHexInput(selectedDesignerStyle.value.borderColor, 'E4E4E7'))
const selectedAppearanceBackgroundPreview = computed(() => hexInputToColor(selectedAppearanceBackgroundHex.value, 'transparent'))
const selectedAppearanceBackgroundColorInput = computed(() => hexInputToColor(selectedAppearanceBackgroundHex.value, '#ffffff'))
const selectedAppearanceBorderPreview = computed(() => {
  if (selectedDesignerStyle.value.borderStyle === 'none')
    return '#e4e4e7'
  return hexInputToColor(selectedAppearanceBorderHex.value, '#e4e4e7')
})
const formAppearanceBackgroundHex = computed(() => colorToHexInput(formStyle.value.backgroundColor, ''))
const formAppearanceBorderHex = computed(() => colorToHexInput(formStyle.value.borderColor, 'E4E4E7'))
const formAppearanceBackgroundPreview = computed(() => hexInputToColor(formAppearanceBackgroundHex.value, 'transparent'))
const formAppearanceBackgroundColorInput = computed(() => hexInputToColor(formAppearanceBackgroundHex.value, '#ffffff'))
const formAppearanceBorderPreview = computed(() => {
  if (formStyle.value.borderStyle === 'none')
    return '#e4e4e7'
  return hexInputToColor(formAppearanceBorderHex.value, '#e4e4e7')
})
const maxFormGridColumns = 24
const normalizedFormGridColumns = computed(() => normalizeGridCount(props.schema.layout?.gridColumns || 2))
const isDatePickerField = computed(() => ['date', 'datetime', 'daterange', 'datetimerange', 'month', 'year', 'quarter'].includes(selectedComponent.value?.componentKey))
const isTimePickerField = computed(() => ['time', 'timerange'].includes(selectedComponent.value?.componentKey))
const isTemporalField = computed(() => isDatePickerField.value || isTimePickerField.value)
const datePickerType = computed(() => {
  const key = selectedComponent.value?.componentKey
  const map = {
    date: 'date',
    datetime: 'datetime',
    daterange: 'daterange',
    datetimerange: 'datetimerange',
    month: 'month',
    year: 'year',
    quarter: 'quarter',
  }
  return map[key] || 'date'
})

watch(() => props.selectedId, () => {
  propertySearchHit.value = ''
  selectedBasicExpandedNames.value = [...basicExpandedNames]
})

watch(() => props.objectCode, () => {
  codeRuleRequestVersion += 1
  codeRules.value = []
  codeRuleLoading.value = false
  loadCodeRuleOptions()
})

const gridColumnOptions = Array.from({ length: maxFormGridColumns }).map((_, index) => index + 1)
const gridColumnMarks = gridColumnOptions.reduce((marks, item) => {
  if (![1, 6, 12, 18, 24].includes(item))
    return marks
  marks[item] = `${item}`
  return marks
}, {})
const componentSizeOptions = [
  { label: '默认', value: '' },
  { label: '小', value: 'small' },
  { label: '中', value: 'medium' },
  { label: '大', value: 'large' },
]
const requestMethodOptions = [
  { label: 'GET', value: 'get' },
  { label: 'POST', value: 'post' },
]
const transferDataSourceOptions = [
  { label: '静态选项', value: 'static' },
  { label: '远程接口', value: 'remote' },
]
const widgetDataSourceOptions = [
  { label: '静态配置', value: 'static' },
  { label: '当前表单/详情数据', value: 'context' },
  { label: '远程接口', value: 'remote' },
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
const splitDirectionOptions = [
  { label: '横向', value: 'horizontal' },
  { label: '纵向', value: 'vertical' },
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
const vuePreviewModeOptions = [
  { label: '安全模板预览', value: 'safe-template' },
  { label: 'Props 模板预览', value: 'live' },
  { label: '代码视图', value: 'code' },
]
const buttonTypeOptions = [
  { label: '默认 default', value: 'default' },
  { label: '主要 primary', value: 'primary' },
  { label: '信息 info', value: 'info' },
  { label: '成功 success', value: 'success' },
  { label: '警告 warning', value: 'warning' },
  { label: '错误 error', value: 'error' },
]
const propertySearchIndex = [
  { keys: ['标识', '名称', '绑定字段', 'field', '字段编码'], label: '基础配置 / 标识', selectedTab: 'basic', selectedExpand: ['identity'], formTab: 'basic', formExpand: ['assets'] },
  { keys: ['字段', '字段组件', '占位', 'placeholder', '默认', '默认值', '字典', 'dict', '组件属性', '标签', '标题', '公式', 'formula', '计算'], label: '基础配置 / 字段组件', selectedTab: 'basic', selectedExpand: ['field'] },
  { keys: ['选项', 'option', '新增选项', '静态选项', '标签', '值'], label: '基础配置 / 选项配置', selectedTab: 'basic', selectedExpand: ['options'] },
  { keys: ['按钮', 'button', '块级', '禁用', '类型', '文案', '动作'], label: '基础配置 / 按钮组件', selectedTab: 'basic', selectedExpand: ['button'] },
  { keys: ['说明', '说明文本', '角标', '辅助', 'badge'], label: '基础配置 / 辅助展示', selectedTab: 'basic', selectedExpand: ['assist'] },
  { keys: ['日期', '时间', '格式', 'datetime', 'date', '范围', '年月日'], label: '基础配置 / 日期时间组件', selectedTab: 'basic', selectedExpand: ['temporal'] },
  { keys: ['crud字段', '查询字段', '搜索字段', '表格列字段', '编辑字段', '列标题', '列宽', '对齐', '固定', '省略', '排序'], label: '基础配置 / CRUD 字段配置', selectedTab: 'basic', selectedExpand: ['crud-field'] },
  { keys: ['crud', '查询', '搜索', '表格', '列表', '分页', '接口', 'api', '数据源', '基础路径', '行主键', '渲染模式', '表格尺寸', '编辑表单'], label: 'CRUD 配置', selectedTab: 'crud' },
  { keys: ['布局', '跨度', '栅格', '列数', '宽度', 'labelWidth', '标签宽度', '标签位置', '标签对齐', '打开方式'], label: '布局', selectedTab: 'basic', selectedExpand: ['gridQuick'], formTab: 'basic', formExpand: ['layout'] },
  { keys: ['校验', '唯一', '唯一校验', '不能重复', '必填', '只读', '隐藏', '状态', 'unique', 'required', 'readonly'], label: '可见性与校验', selectedTab: 'basic', selectedExpand: ['validation'], formTab: 'basic', formExpand: ['spacing'] },
  { keys: ['事件', '交互', '联动', '弹窗事件', 'openModal', '生命周期', '加载', '提交', '字段变化'], label: '交互 / 事件规则', selectedTab: 'interaction', formTab: 'basic', formExpand: ['events'] },
  { keys: ['样式', '颜色', '背景', '边框', '圆角', '阴影', '间距', 'padding', 'margin'], label: '样式配置', selectedTab: 'style', formTab: 'style', formExpand: ['appearance', 'spacing'] },
  { keys: ['表单资产', '多表单', '表单名称', '表单编码'], label: '表单属性 / 表单资产', formTab: 'basic', formExpand: ['assets'] },
  { keys: ['弹窗', '抽屉', 'modal', 'drawer', '打开方式'], label: '表单属性 / AiForm 布局', formTab: 'basic', formExpand: ['layout'] },
  { keys: ['反馈', '折叠', '最大显示字段', '行间距', '列间距', '表单列数'], label: '表单属性 / 间距与反馈', formTab: 'basic', formExpand: ['spacing', 'layout'] },
  { keys: ['操作', '提交', '重置', '取消', '提交文案'], label: '表单属性 / 操作按钮', formTab: 'basic', formExpand: ['actions'] },
  { keys: ['位置', '尺寸', '最大宽度', '最小高度', '左', '上', 'x', 'y', '坐标', '填充', '适应内容'], label: '样式 / 位置与尺寸', selectedTab: 'style', formTab: 'style', selectedExpand: ['position'], formExpand: ['position'] },
  { keys: ['排版', '文字', '字号', '行高', '文字颜色', 'font', 'lineHeight'], label: '样式 / 文字排版', selectedTab: 'style', formTab: 'style', selectedExpand: ['typography'], formExpand: ['typography'] },
  { keys: ['外观', '透明度', '边框', '背景色', '圆角', '阴影', 'border', 'shadow', 'opacity'], label: '样式 / 外观与装饰', selectedTab: 'style', formTab: 'style', selectedExpand: ['appearance'], formExpand: ['appearance'] },
  { keys: ['自定义', 'class', 'css', 'style', '自定义样式'], label: '样式配置 / 自定义样式', selectedTab: 'style', formTab: 'style', formExpand: ['custom-style'] },
  { keys: ['源码', 'json', 'schema'], label: '源码入口在画布中间工具栏', selectedTab: 'basic', formTab: 'basic' },
]
const cardSizeOptions = [
  { label: '小 small', value: 'small' },
  { label: '中 medium', value: 'medium' },
  { label: '大 large', value: 'large' },
  { label: '巨大 huge', value: 'huge' },
]
const tabsTypeOptions = [
  { label: 'line', value: 'line' },
  { label: 'bar', value: 'bar' },
  { label: 'card', value: 'card' },
  { label: 'segment', value: 'segment' },
]
const tabsPlacementOptions = [
  { label: 'top', value: 'top' },
  { label: 'bottom', value: 'bottom' },
  { label: 'left', value: 'left' },
  { label: 'right', value: 'right' },
]
const tabsTriggerOptions = [
  { label: 'click', value: 'click' },
  { label: 'hover', value: 'hover' },
]
const collapseArrowPlacementOptions = [
  { label: 'left', value: 'left' },
  { label: 'right', value: 'right' },
]
const collapseDisplayDirectiveOptions = [
  { label: 'if', value: 'if' },
  { label: 'show', value: 'show' },
]
const collapseTriggerAreaOptions = [
  { label: 'main', value: 'main' },
  { label: 'arrow', value: 'arrow' },
  { label: 'extra', value: 'extra' },
]
const datePickerTypeOptions = [
  { label: 'date', value: 'date' },
  { label: 'datetime', value: 'datetime' },
  { label: 'daterange', value: 'daterange' },
  { label: 'datetimerange', value: 'datetimerange' },
  { label: 'month', value: 'month' },
  { label: 'year', value: 'year' },
  { label: 'quarter', value: 'quarter' },
]
const pickerPlacementOptions = [
  { label: 'bottom-start', value: 'bottom-start' },
  { label: 'bottom', value: 'bottom' },
  { label: 'bottom-end', value: 'bottom-end' },
  { label: 'top-start', value: 'top-start' },
  { label: 'top', value: 'top' },
  { label: 'top-end', value: 'top-end' },
]
const pickerActionOptions = [
  { label: 'clear', value: 'clear' },
  { label: 'now', value: 'now' },
  { label: 'confirm', value: 'confirm' },
]
const booleanValueOptions = [
  { label: 'true', value: true },
  { label: 'false', value: false },
]
const tableAlignOptions = [
  { label: '左对齐', value: 'left' },
  { label: '居中', value: 'center' },
  { label: '右对齐', value: 'right' },
]
const tableFixedOptions = [
  { label: '不固定', value: '' },
  { label: '左固定', value: 'left' },
  { label: '右固定', value: 'right' },
]
const triggerOptions = [
  { label: '点击组件时', value: 'click' },
  { label: '字段值变化时', value: 'change' },
  { label: '输入获得焦点时', value: 'focus' },
  { label: '输入失去焦点时', value: 'blur' },
  { label: '字段被清空时', value: 'clear' },
  { label: '组件初始化后', value: 'mounted' },
]
const actionOptions = [
  { label: '给目标字段赋值', value: 'setValue' },
  { label: '清空目标字段', value: 'clearValue' },
  { label: '刷新下拉选项', value: 'setOptions' },
  { label: '控制显示或隐藏', value: 'showHide' },
  { label: '控制启用或禁用', value: 'enableDisable' },
  { label: '打开弹窗表单', value: 'openModal' },
  { label: '请求后端接口', value: 'apiRequest' },
]
const formOpenModeOptions = [
  { label: '弹窗', value: 'modal' },
  { label: '抽屉', value: 'drawer' },
  { label: '平铺', value: 'flat' },
  { label: '多页签', value: 'tabWorkspace' },
]
const drawerPlacementOptions = [
  { label: '右侧', value: 'right' },
  { label: '左侧', value: 'left' },
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
const modalContentModeOptions = [
  { label: '复用当前表单', value: 'currentForm' },
  { label: '引用表单资产', value: 'formAsset' },
  { label: '选择画布组件', value: 'component' },
  { label: '空白弹窗，后续配置', value: 'empty' },
]
const interactionPresets = [
  { key: 'cascade', title: '下拉联动', description: '当前字段变化后，更新另一个下拉的选项。' },
  { key: 'openModal', title: '打开弹窗', description: '按钮点击后打开表单弹窗或业务弹窗。' },
  { key: 'showHide', title: '显示隐藏', description: '根据当前值控制另一个字段是否显示。' },
  { key: 'apiRequest', title: '调用接口', description: '点击按钮或值变化后提交接口请求。' },
]
const propChineseLabels = {
  size: '组件尺寸',
  clearable: '可清空',
  disabled: '禁用',
  text: '按钮文字',
  secondary: '次要按钮',
  tertiary: '三级按钮',
  quaternary: '四级按钮',
  dashed: '虚线按钮',
  loading: '加载中',
  block: '块级按钮',
  placeholder: '占位提示',
  status: '校验状态',
  maxlength: '最大长度',
  showCount: '显示字数',
  round: '圆角',
  readonly: '只读',
  inputProps: '原生输入属性',
  rows: '行数',
  autosize: '自适应高度',
  min: '最小值',
  max: '最大值',
  step: '步长',
  precision: '精度',
  showButton: '显示加减按钮',
  multiple: '多选',
  filterable: '可搜索',
  remote: '远程搜索',
  maxTagCount: '最多显示标签数',
  placement: '弹出位置',
  fallbackOption: '保留回填选项',
  dictType: '字典类型',
  name: '表单控件名称',
  defaultValue: '默认值',
  value: '受控值',
  checkedValue: '选中值',
  uncheckedValue: '未选中值',
  checkedText: '选中文案',
  uncheckedText: '未选中文案',
  rubberBand: '橡皮筋动效',
  range: '范围选择',
  vertical: '垂直显示',
  tooltip: '显示提示',
  marks: '刻度标记',
  count: '数量',
  allowHalf: '允许半选',
  showAlpha: '显示透明度',
  modes: '颜色模式',
  swatches: '预设色',
  actions: '底部动作',
  cascade: '级联勾选',
  checkStrategy: '勾选策略',
  showPath: '显示路径',
  checkable: '显示复选框',
  bordered: '显示边框',
  embedded: '嵌入模式',
  segmented: '分段线',
  hoverable: '悬浮阴影',
  contentScrollable: '内容可滚动',
  role: 'ARIA 角色',
  headerStyle: '头部样式',
  contentStyle: '内容样式',
  footerStyle: '底部样式',
  type: '类型',
  trigger: '触发方式',
  animated: '动画',
  closable: '可关闭',
  addable: '可新增',
  justifyContent: '对齐分布',
  tabsPadding: '标签栏内边距',
  paneStyle: '面板样式',
  tabStyle: '标签样式',
  accordion: '手风琴模式',
  arrowPlacement: '箭头位置',
  displayDirective: '渲染策略',
  triggerAreas: '可触发区域',
  defaultExpandedNames: '默认展开项',
  expandedNames: '受控展开项',
  format: '显示格式',
  valueFormat: '值格式',
  inputReadonly: '输入框只读',
  closeOnSelect: '选择后关闭',
  updateValueOnClose: '关闭时更新值',
  defaultTime: '默认时间',
  separator: '分隔符',
  startPlaceholder: '开始占位',
  endPlaceholder: '结束占位',
  firstDayOfWeek: '每周起始日',
  showIcon: '显示图标',
  hours: '小时选项',
  minutes: '分钟选项',
  seconds: '秒选项',
}
const groupChineseLabels = {
  'NInput Props': '输入框属性',
  'NInput Textarea Props': '多行文本属性',
  'NInputNumber Props': '数字输入属性',
  'NSelect Props': '下拉选择属性',
  'DictSelect / NSelect Props': '字典下拉属性',
  'RadioGroup Props': '单选组属性',
  'CheckboxGroup Props': '多选组属性',
  'NSwitch Props': '开关属性',
  'NSlider Props': '滑块属性',
  'NRate Props': '评分属性',
  'NColorPicker Props': '颜色选择属性',
  'NCascader Props': '级联选择属性',
  'NTreeSelect Props': '树形选择属性',
  'Card Props': '卡片属性',
  'Tabs Props': '标签页属性',
  'Collapse Props': '折叠面板属性',
  'DatePicker Props': '日期选择属性',
  'TimePicker Props': '时间选择属性',
  'Button Props': '按钮属性',
}
const colorSwatches = ['#ffffff', '#f8fafc', '#eff6ff', '#ecfdf5', '#fffbeb', '#fef2f2', '#dbe3ee', '#94a3b8', '#2563eb', '#16a34a', '#f59e0b', '#dc2626', 'rgba(255, 255, 255, 0)']
const shadowOptions = [
  { label: '无', value: '' },
  { label: '轻微', value: '0 4px 12px rgba(15, 23, 42, 0.06)' },
  { label: '标准', value: '0 10px 24px rgba(15, 23, 42, 0.10)' },
  { label: '强调', value: '0 16px 36px rgba(15, 23, 42, 0.16)' },
]
const spacingSides = [
  { key: 'Top', label: '上' },
  { key: 'Right', label: '右' },
  { key: 'Bottom', label: '下' },
  { key: 'Left', label: '左' },
]
const crudApiFields = [
  { key: 'list', label: '分页列表', placeholder: 'get@/employee/page' },
  { key: 'detail', label: '详情', placeholder: 'post@/employee/getById' },
  { key: 'add', label: '新增', placeholder: 'post@/employee/add' },
  { key: 'update', label: '更新', placeholder: 'post@/employee/edit' },
  { key: 'delete', label: '删除', placeholder: 'post@/employee/remove/:id' },
  { key: 'import', label: '导入', placeholder: 'post@/employee/import' },
  { key: 'export', label: '导出', placeholder: 'post@/employee/export' },
  { key: 'importTemplate', label: '导入模板', placeholder: 'get@/employee/importTemplate' },
]
const crudSwitchFields = [
  { key: 'showSearch', label: '显示查询区', defaultValue: true },
  { key: 'searchEnableCollapse', label: '查询折叠', defaultValue: true },
  { key: 'showPagination', label: '显示分页', defaultValue: true },
  { key: 'loadDetailOnEdit', label: '编辑前加载详情', defaultValue: true },
  { key: 'hideToolbar', label: '隐藏工具栏', defaultValue: false },
  { key: 'hideAdd', label: '隐藏新增', defaultValue: false },
  { key: 'hideBatchDelete', label: '隐藏批量删除', defaultValue: false },
  { key: 'hideSelection', label: '隐藏多选', defaultValue: false },
  { key: 'striped', label: '斑马纹', defaultValue: false },
  { key: 'bordered', label: '表格边框', defaultValue: false },
  { key: 'showRenderModeSwitch', label: '列表/卡片切换', defaultValue: true },
  { key: 'showImport', label: '显示导入', defaultValue: false },
  { key: 'showExport', label: '显示导出', defaultValue: false },
  { key: 'showExportTasks', label: '导出任务入口', defaultValue: true },
  { key: 'editShowFeedback', label: '编辑校验反馈', defaultValue: true },
  { key: 'hideModalFooter', label: '隐藏弹窗底部', defaultValue: false },
]
const rowTotalColumns = computed(() => normalizeGridCount(selectedComponent.value?.props?.columns || maxFormGridColumns))
const rowColumns = computed(() => (selectedComponent.value?.children || []).filter(child => child?.componentKey === 'col'))
const rowColumnCount = computed(() => rowColumns.value.length || 1)
const crudConfigFields = computed(() => collectCrudConfigFields(selectedComponent.value?.children || []))
const crudDescriptionFieldOptions = computed(() => crudConfigFields.value.map(field => ({
  label: `${field.label || field.fieldCode}（${field.fieldCode}）`,
  rawLabel: field.label || field.fieldCode,
  value: field.fieldCode,
  field: field.fieldCode,
  componentKey: field.componentKey,
})))
const firstCrudExpandPanel = computed(() => crudOptions.value?.expandConfig?.panels?.[0] || null)
const editingCrudField = computed(() => editingCrudFieldId.value ? getDesignerComponent(props.schema, editingCrudFieldId.value) : null)
const editingCrudConfig = computed(() => editingCrudField.value?.props?.__crudConfig || {})
const selectedOptions = computed(() => selectedComponent.value?.props?.options || [])
const isFieldInsideCrud = computed(() => isField.value && hasAncestorComponent(props.schema, props.selectedId, ['AiCrudPage', 'crudBlock']))
const selectedCrudFieldConfig = computed(() => isFieldInsideCrud.value ? selectedComponent.value?.props?.__crudConfig || {} : null)
const isOptionField = computed(() => ['select', 'radio', 'radioButton', 'checkbox', 'transfer', 'cascader', 'treeSelect'].includes(selectedComponent.value?.componentKey || ''))
const isManualOptionField = computed(() => isOptionField.value && !selectedComponent.value?.props?.dictType && selectedComponent.value?.props?.dataSourceType !== 'remote')
const defaultValueSelectMultiple = computed(() => ['checkbox'].includes(selectedComponent.value?.componentKey || ''))
const defaultValueSelectEnabled = computed(() => {
  const key = selectedComponent.value?.componentKey || ''
  if (!['select', 'dictSelect', 'radio', 'radioButton', 'checkbox'].includes(key))
    return false
  return selectedComponent.value?.props?.dataSourceType !== 'remote'
})
const selectedDictType = computed(() => String(selectedComponent.value?.props?.dictType || '').trim())
const defaultValueOptionsLoading = computed(() => {
  const dictType = selectedDictType.value
  return dictType ? dictDefaultOptionsLoading.value[dictType] === true : false
})
const defaultValueSelectOptions = computed(() => {
  const staticOptions = normalizeDefaultValueOptions(selectedOptions.value || [])
  if (staticOptions.length)
    return staticOptions
  const dictType = selectedDictType.value
  return dictType ? dictDefaultOptions.value[dictType] || [] : []
})
const selectedDefaultValueForSelect = computed(() => {
  const value = selectedComponent.value?.props?.defaultValue
  if (!defaultValueSelectMultiple.value)
    return value ?? null
  if (Array.isArray(value))
    return value
  if (value === undefined || value === null || value === '')
    return []
  return [value]
})
const selectedGenerationConfig = computed(() => selectedComponent.value?.props?.generation || {})
const selectedGenerationEnabled = computed(() => selectedGenerationConfig.value.enabled === true)
const selectedGenerationRuleCode = computed(() => selectedGenerationConfig.value.ruleCode || '')
const codeRuleOptions = computed(() => codeRules.value.map(rule => ({
  label: `${rule.ruleName || rule.ruleCode}（${rule.ruleCode}）`,
  value: rule.ruleCode,
  rule,
})))
const selectedGenerationRule = computed(() => {
  const ruleCode = selectedGenerationRuleCode.value
  return codeRules.value.find(rule => rule.ruleCode === ruleCode) || null
})
const activePropGroups = computed(() => buildNaivePropGroups(selectedComponent.value?.componentKey || ''))
const runtimeRuleFieldOptions = computed(() => collectRuntimeRuleFieldOptions(props.schema?.components || []))
const isDictLikeField = computed(() => {
  const key = selectedComponent.value?.componentKey || ''
  return ['select', 'dictSelect', 'radio', 'checkbox', 'cascader'].includes(key)
})
const isObjectReferenceField = computed(() => selectedComponent.value?.componentKey === 'objectReference')
const referenceObjectCode = computed(() => selectedComponent.value?.props?.referenceObjectCode || '')
const referenceDisplayField = computed(() => selectedComponent.value?.props?.referenceDisplayField || '')
const referenceValueField = computed(() => selectedComponent.value?.props?.referenceValueField || '')
const businessObjectOptions = ref([])
const businessObjectLoading = ref(false)
const referenceTargetFieldsMap = ref({})
const referenceTargetFieldLoading = computed(() => {
  return !!referenceTargetFieldsMap.value[referenceObjectCode.value]?.loading
})
const referenceTargetFieldOptions = computed(() => referenceTargetFieldsMap.value[referenceObjectCode.value]?.options || [])

const generationFillPolicyOptions = [
  { label: '为空时生成', value: 'EMPTY_ONLY' },
  { label: '总是覆盖', value: 'OVERWRITE' },
]

const generationTriggerOptions = [
  { label: '新增时', value: 'ON_CREATE' },
]

const componentFieldDefaults = {
  input: { fieldType: 'TEXT', dataType: 'varchar', componentType: 'input', length: 128, precision: 2, queryType: 'like' },
  textarea: { fieldType: 'MULTILINE', dataType: 'text', componentType: 'textarea', length: null, precision: 2, queryType: 'like' },
  number: { fieldType: 'NUMBER', dataType: 'int', componentType: 'number', length: 11, precision: 0, queryType: 'eq' },
  inputNumber: { fieldType: 'NUMBER', dataType: 'int', componentType: 'number', length: 11, precision: 0, queryType: 'eq' },
  integer: { fieldType: 'NUMBER', dataType: 'int', componentType: 'number', length: 11, precision: 0, queryType: 'eq' },
  money: { fieldType: 'MONEY', dataType: 'decimal', componentType: 'number', length: 18, precision: 2, queryType: 'eq' },
  date: { fieldType: 'DATE', dataType: 'date', componentType: 'date', length: null, precision: null, queryType: 'eq' },
  datetime: { fieldType: 'DATETIME', dataType: 'datetime', componentType: 'datetime', length: null, precision: null, queryType: 'eq' },
  switch: { fieldType: 'SWITCH', dataType: 'tinyint', componentType: 'switch', length: 1, precision: 0, queryType: 'eq' },
  select: { fieldType: 'DICT', dataType: 'varchar', componentType: 'select', length: 64, precision: 2, queryType: 'eq' },
  radio: { fieldType: 'RADIO', dataType: 'varchar', componentType: 'radio', length: 64, precision: 2, queryType: 'eq' },
  checkbox: { fieldType: 'CHECKBOX', dataType: 'varchar', componentType: 'checkbox', length: 255, precision: 2, queryType: 'in' },
  dictSelect: { fieldType: 'DICT', dataType: 'varchar', componentType: 'dictSelect', length: 64, precision: 2, queryType: 'eq' },
  cascader: { fieldType: 'DICT', dataType: 'varchar', componentType: 'cascader', length: 128, precision: 2, queryType: 'eq' },
  regionTreeSelect: { fieldType: 'REGION', dataType: 'varchar', componentType: 'regionTreeSelect', length: 32, precision: 2, queryType: 'eq' },
  orgTreeSelect: { fieldType: 'DEPT', dataType: 'bigint', componentType: 'orgTreeSelect', length: null, precision: null, queryType: 'eq' },
  userSelect: { fieldType: 'USER', dataType: 'bigint', componentType: 'userSelect', length: null, precision: null, queryType: 'eq' },
  fileUpload: { fieldType: 'FILE', dataType: 'varchar', componentType: 'fileUpload', length: 512, precision: 2, queryType: 'eq' },
  imageUpload: { fieldType: 'IMAGE', dataType: 'varchar', componentType: 'imageUpload', length: 512, precision: 2, queryType: 'eq' },
  objectReference: { fieldType: 'REFERENCE', dataType: 'bigint', componentType: 'objectReference', length: null, precision: null, queryType: 'eq' },
  recordSelector: { fieldType: 'RECORD_SELECTOR', dataType: 'bigint', componentType: 'recordSelector', length: null, precision: null, queryType: 'eq' },
}

watch(
  selectedDictType,
  (dictType) => {
    loadDefaultValueDictOptions(dictType)
  },
  { immediate: true },
)

watch(() => props.selectedId, () => {
  codeRulePreview.value = null
  if (selectedGenerationEnabled.value)
    loadCodeRuleOptions()
})

watch(selectedGenerationRuleCode, () => {
  codeRulePreview.value = null
})

function collectRuntimeRuleFieldOptions(components = []) {
  const options = []
  const seen = new Set()
  const walk = (items = []) => {
    ;(Array.isArray(items) ? items : []).forEach((component) => {
      if (!component || typeof component !== 'object')
        return
      const field = component.fieldBinding?.fieldCode || component.field || component.props?.field
      if (field && !seen.has(field)) {
        seen.add(field)
        options.push({
          label: `${component.label || component.props?.label || field}（${field}）`,
          value: field,
        })
      }
      walk(component.children || [])
    })
  }
  walk(components)
  return options
}

function updateComponent(patch) {
  if (!props.selectedId)
    return
  emit('update:schema', updateDesignerComponent(props.schema, props.selectedId, patch))
}

function pickSwitchableCommonProps(sourceProps = {}, newKey = '') {
  const commonKeys = ['defaultValue', 'placeholder', 'disabled', 'clearable', 'required', 'dictType']
  const nextProps = {}
  commonKeys.forEach((key) => {
    if (sourceProps[key] !== undefined)
      nextProps[key] = sourceProps[key]
  })
  if (['input', 'textarea'].includes(newKey)) {
    if (sourceProps.maxlength !== undefined)
      nextProps.maxlength = sourceProps.maxlength
    if (sourceProps.showCount !== undefined)
      nextProps.showCount = sourceProps.showCount
  }
  else {
    nextProps.maxlength = undefined
    nextProps.showCount = undefined
  }
  return nextProps
}

function handleSwitchComponentType(newKey) {
  const component = selectedComponent.value
  if (!component || !newKey || newKey === component.componentKey)
    return
  const group = switchableComponentGroups.find(item => item.includes(component.componentKey))
  if (!group?.includes(newKey))
    return
  const defaults = componentFieldDefaults[newKey] || componentFieldDefaults.input
  const nextProps = pickSwitchableCommonProps(component.props || {}, newKey)
  const nextFieldBinding = {
    ...(component.fieldBinding || {}),
    fieldType: defaults.fieldType,
    dataType: defaults.dataType,
    componentType: defaults.componentType || newKey,
  }
  updateComponent({
    componentKey: newKey,
    props: nextProps,
    fieldBinding: nextFieldBinding,
  })
}

function updateComponentHidden(value) {
  const hidden = value === true
  updateComponent({
    visibility: { hidden },
    ...(hidden
      ? {
          validation: {
            required: false,
            requiredMessage: '',
          },
        }
      : {}),
  })
}

function updateValidationPreset(value) {
  const preset = getValidationPreset(value)
  const nextValidation = {
    ...(selectedComponent.value?.validation || {}),
    preset: value || '',
    pattern: value ? preset?.pattern || '' : undefined,
    message: value ? preset?.message || '' : undefined,
  }
  if (!value) {
    nextValidation.pattern = ''
    nextValidation.message = ''
  }
  updateComponent({ validation: nextValidation })
}

function normalizePositiveInteger(value) {
  const number = Number(value)
  if (!Number.isFinite(number) || number <= 0)
    return null
  return Math.floor(number)
}

function updateFieldMaxLength(value) {
  const maxLength = normalizePositiveInteger(value)
  updateComponent({
    props: {
      maxlength: maxLength || undefined,
      showCount: maxLength ? true : undefined,
    },
  })
}

function openFieldFormulaPanel() {
  if (!selectedFieldAsset.value)
    return
  fieldFormulaPanelVisible.value = true
}

function handleFieldAssetSave(payload = {}) {
  const fieldCode = payload.fieldCode || selectedFieldCode.value
  if (!fieldCode)
    return
  const formulaConfig = payload.formulaConfig ?? null
  updateComponent(buildFieldAssetComponentPatch(payload, formulaConfig, fieldCode))
  emit('fieldAssetUpdated', {
    ...payload,
    fieldCode,
    formulaConfig,
  })
  fieldFormulaPanelVisible.value = false
}

function buildFieldAssetComponentPatch(payload = {}, formulaConfig = null, fieldCode = '') {
  const propsPatch = {
    formulaConfig,
  }
  if (Object.prototype.hasOwnProperty.call(payload, 'placeholder'))
    propsPatch.placeholder = payload.placeholder || ''
  if (Object.prototype.hasOwnProperty.call(payload, 'defaultValue'))
    propsPatch.defaultValue = payload.defaultValue
  if (Object.prototype.hasOwnProperty.call(payload, 'dictType'))
    propsPatch.dictType = payload.dictType || ''
  if (Object.prototype.hasOwnProperty.call(payload, 'referenceObjectCode'))
    propsPatch.referenceObjectCode = payload.referenceObjectCode || ''
  if (Object.prototype.hasOwnProperty.call(payload, 'referenceDisplayField'))
    propsPatch.referenceDisplayField = payload.referenceDisplayField || ''
  if (payload.basicProps && Object.prototype.hasOwnProperty.call(payload.basicProps, 'recordSelector'))
    propsPatch.recordSelector = payload.basicProps.recordSelector || undefined

  const payloadValidation = payload.validation && typeof payload.validation === 'object'
    ? { ...payload.validation }
    : null
  const patch = {
    label: payload.fieldName || selectedComponent.value?.label || fieldCode,
    props: propsPatch,
    advancedProps: {
      ...(selectedComponent.value?.advancedProps || {}),
      ...(payload.advancedProps || {}),
      formulaConfig,
    },
  }
  if (payloadValidation) {
    patch.validation = {
      ...(selectedComponent.value?.validation || {}),
      ...payloadValidation,
    }
  }
  if (Object.prototype.hasOwnProperty.call(payload, 'required')) {
    patch.validation = {
      ...(patch.validation || selectedComponent.value?.validation || {}),
      required: Boolean(payload.required),
      requiredMessage: payload.required ? `${payload.fieldName || selectedComponent.value?.label || fieldCode}不能为空` : '',
    }
  }
  if (Object.prototype.hasOwnProperty.call(payload, 'formVisible') || Object.prototype.hasOwnProperty.call(payload, 'readonly')) {
    patch.visibility = {
      ...(Object.prototype.hasOwnProperty.call(payload, 'formVisible') ? { hidden: payload.formVisible === false } : {}),
      ...(Object.prototype.hasOwnProperty.call(payload, 'readonly') ? { readonly: Boolean(payload.readonly) } : {}),
    }
  }
  return patch
}

function normalizeSelectedFieldAsset(source = {}) {
  const component = selectedComponent.value || {}
  const fieldCode = source.fieldCode || source.field || selectedFieldCode.value
  const formulaConfig = source.formulaConfig ?? component.props?.formulaConfig ?? component.advancedProps?.formulaConfig ?? null
  return {
    ...source,
    field: source.field || fieldCode,
    fieldCode,
    fieldName: source.fieldName || source.label || component.label || fieldCode,
    columnName: source.columnName || component.fieldBinding?.columnName || camelToSnake(fieldCode),
    validation: source.validation || component.validation || {},
    formulaConfig,
  }
}

function createFieldAssetFromSelectedComponent() {
  const component = selectedComponent.value || {}
  const fieldCode = selectedFieldCode.value
  const defaults = componentFieldDefaults[component.componentKey] || componentFieldDefaults.input
  const fieldBinding = {
    mode: 'field',
    fieldCode,
    columnName: component.fieldBinding?.columnName || camelToSnake(fieldCode),
    createIfMissing: true,
    source: 'designer',
    locked: false,
    ...(component.fieldBinding || {}),
  }
  return {
    field: fieldCode,
    fieldName: component.label || fieldCode,
    fieldCode,
    columnName: fieldBinding.columnName,
    fieldType: defaults.fieldType,
    dataType: defaults.dataType,
    length: defaults.length,
    precision: defaults.precision,
    required: Boolean(component.validation?.required),
    defaultValue: component.props?.defaultValue ?? '',
    searchable: false,
    listVisible: true,
    formVisible: component.visibility?.hidden !== true,
    importable: true,
    exportable: true,
    componentType: defaults.componentType,
    queryType: defaults.queryType,
    dictType: component.props?.dictType || '',
    sortable: false,
    systemField: false,
    readonly: Boolean(component.visibility?.readonly),
    fieldStatus: 'ENABLED',
    referenceObjectCode: component.props?.referenceObjectCode || '',
    referenceDisplayField: component.props?.referenceDisplayField || '',
    validation: component.validation || {},
    placeholder: component.props?.placeholder || '',
    remark: component.label || '',
    sortOrder: Number(component.props?.sortOrder ?? component.layout?.order ?? 0),
    fieldBinding,
    basicProps: {
      ...(component.props || {}),
      fieldBinding,
    },
    advancedProps: {
      ...(component.advancedProps || {}),
    },
    formulaConfig: component.props?.formulaConfig ?? component.advancedProps?.formulaConfig ?? null,
  }
}

function updatePageWidgetOptionSource(patch = {}) {
  updateComponent({
    props: {
      optionSource: {
        ...(selectedComponent.value?.props?.optionSource || {}),
        ...patch,
      },
    },
  })
}

function updatePageWidgetDataBinding(patch = {}) {
  const next = {
    ...(selectedComponent.value?.props?.dataBinding || {}),
    ...patch,
  }
  next.enabled = next.sourceType !== 'static'
  updateComponent({
    props: {
      dataBinding: next,
    },
  })
}

function resolveBooleanKeys(source = {}, keys = []) {
  return keys.filter(key => source?.[key] === true)
}

function updateCodeColor(value = '') {
  if (selectedComponent.value?.componentKey === 'barcode') {
    updateComponent({ props: { lineColor: value || '#0f172a' } })
    return
  }
  updateComponent({ props: { foreground: value || '#0f172a' } })
}

function updateDictType(value = '') {
  loadDefaultValueDictOptions(value)
  updateComponent({
    props: {
      dictType: value || '',
      options: [],
      defaultValue: undefined,
    },
  })
}

async function loadCodeRuleOptions() {
  if (codeRules.value.length || codeRuleLoading.value)
    return
  const requestVersion = ++codeRuleRequestVersion
  const requestedObjectCode = props.objectCode || ''
  codeRuleLoading.value = true
  try {
    const res = await codeRuleList({ scene: 'COMMON', objectCode: requestedObjectCode || undefined })
    if (requestVersion === codeRuleRequestVersion && requestedObjectCode === (props.objectCode || ''))
      codeRules.value = Array.isArray(res.data) ? res.data : []
  }
  finally {
    if (requestVersion === codeRuleRequestVersion)
      codeRuleLoading.value = false
  }
}

async function loadBusinessObjectOptions() {
  if (businessObjectOptions.value.length || businessObjectLoading.value)
    return
  businessObjectLoading.value = true
  try {
    const res = await businessObjectList({})
    const list = Array.isArray(res.data) ? res.data : []
    const seen = new Set()
    businessObjectOptions.value = list
      .filter((item) => {
        if (!item.objectCode || seen.has(item.objectCode))
          return false
        seen.add(item.objectCode)
        return true
      })
      .map(item => ({
        label: `${item.objectName || item.objectCode}（${item.objectCode}）`,
        value: item.objectCode,
        object: item,
      }))
  }
  catch {
    businessObjectOptions.value = []
  }
  finally {
    businessObjectLoading.value = false
  }
}

async function loadReferenceTargetFields(objectCode, force = false) {
  if (!objectCode)
    return
  if (!force && referenceTargetFieldsMap.value[objectCode])
    return
  const target = businessObjectOptions.value.find(item => item.value === objectCode)?.object
  if (!target?.id) {
    referenceTargetFieldsMap.value = {
      ...referenceTargetFieldsMap.value,
      [objectCode]: { loading: false, options: [] },
    }
    return
  }
  referenceTargetFieldsMap.value = {
    ...referenceTargetFieldsMap.value,
    [objectCode]: { loading: true, options: referenceTargetFieldsMap.value[objectCode]?.options || [] },
  }
  try {
    const res = await businessObjectDesigner(target.id)
    const fields = res.data?.fields || res.data?.modelSchema?.fields || []
    const options = fields
      .filter(field => !['tenantId', 'tenant_id', 'createBy', 'create_by', 'createTime', 'create_time', 'updateBy', 'update_by', 'updateTime', 'update_time', 'delFlag', 'del_flag'].includes(field.fieldCode || field.field))
      .map(field => ({
        label: `${field.fieldName || field.label || field.fieldCode || field.field}（${field.fieldCode || field.field}）`,
        value: field.fieldCode || field.field,
        field,
      }))
    referenceTargetFieldsMap.value = {
      ...referenceTargetFieldsMap.value,
      [objectCode]: { loading: false, options },
    }
  }
  catch {
    referenceTargetFieldsMap.value = {
      ...referenceTargetFieldsMap.value,
      [objectCode]: { loading: false, options: [] },
    }
  }
}

async function updateReferenceObjectCode(value) {
  updateComponent({
    props: {
      referenceObjectCode: value || '',
      referenceDisplayField: '',
      referenceValueField: '',
    },
  })
  if (value) {
    if (!businessObjectOptions.value.length)
      await loadBusinessObjectOptions()
    await loadReferenceTargetFields(value, true)
  }
}

function updateReferenceDisplayField(value) {
  updateComponent({ props: { referenceDisplayField: value || '' } })
}

function updateReferenceValueField(value) {
  updateComponent({ props: { referenceValueField: value || '' } })
}

watch(
  () => selectedComponent.value?.componentKey,
  async (key) => {
    if (key === 'objectReference') {
      await loadBusinessObjectOptions()
      if (referenceObjectCode.value)
        await loadReferenceTargetFields(referenceObjectCode.value)
    }
  },
  { immediate: true },
)

watch(
  businessObjectOptions,
  () => {
    if (isObjectReferenceField.value && referenceObjectCode.value && !referenceTargetFieldsMap.value[referenceObjectCode.value])
      loadReferenceTargetFields(referenceObjectCode.value)
  },
)

function normalizeGenerationConfig(patch = {}) {
  return {
    enabled: true,
    type: 'CODE_RULE',
    mode: 'CODE_RULE',
    ruleCode: selectedGenerationRuleCode.value || codeRules.value[0]?.ruleCode || '',
    trigger: 'ON_CREATE',
    fillPolicy: 'EMPTY_ONLY',
    readonly: true,
    ...(selectedGenerationConfig.value || {}),
    ...(patch || {}),
  }
}

async function handleGenerationEnabled(value) {
  if (value) {
    await loadCodeRuleOptions()
    const next = normalizeGenerationConfig({
      enabled: true,
      ruleCode: selectedGenerationRuleCode.value || codeRules.value[0]?.ruleCode || '',
      readonly: true,
    })
    updateComponent({
      props: {
        generation: next,
        defaultValue: undefined,
      },
      validation: {
        required: false,
        requiredMessage: '',
      },
      visibility: {
        readonly: true,
        hidden: true,
      },
    })
    if (next.ruleCode)
      previewSelectedGenerationRule(next.ruleCode)
    return
  }
  updateComponent({
    props: {
      generation: {
        ...(selectedGenerationConfig.value || {}),
        enabled: false,
      },
    },
  })
  codeRulePreview.value = null
}

function updateGenerationConfig(patch = {}) {
  const next = normalizeGenerationConfig(patch)
  updateComponent({
    props: { generation: next },
    ...(next.readonly !== false ? { visibility: { readonly: true } } : {}),
  })
}

function updateGenerationRule(ruleCode = '') {
  updateGenerationConfig({ ruleCode: ruleCode || '' })
  if (ruleCode)
    previewSelectedGenerationRule(ruleCode)
}

function updateGenerationReadonly(value) {
  updateGenerationConfig({ readonly: value !== false })
  updateComponent({ visibility: { readonly: value !== false } })
}

async function previewSelectedGenerationRule(ruleCode = selectedGenerationRuleCode.value) {
  if (!ruleCode)
    return
  await loadCodeRuleOptions()
  codeRulePreviewing.value = true
  try {
    const fieldCode = selectedComponent.value?.fieldBinding?.fieldCode || selectedComponent.value?.field || 'code'
    const res = await previewCodeRule({
      ruleCode,
      sequence: 1,
      fields: {
        suiteCode: props.schema?.suiteCode || props.schema?.settings?.suiteCode || 'SUITE',
        objectCode: props.schema?.objectCode || props.schema?.settings?.objectCode || 'OBJECT',
        fieldCode,
        [fieldCode]: selectedComponent.value?.label || fieldCode,
      },
    })
    codeRulePreview.value = res.data || null
  }
  finally {
    codeRulePreviewing.value = false
  }
}

function updateFieldBindingCode(value = '') {
  const fieldCode = String(value || '').trim()
  if (!selectedComponent.value || !isField.value)
    return
  updateComponent({
    fieldBinding: {
      ...(selectedComponent.value.fieldBinding || {}),
      mode: selectedComponent.value.fieldBinding?.mode || 'field',
      fieldCode,
      columnName: camelToSnake(fieldCode),
    },
  })
}

function updateUniqueValidation(value) {
  updateComponent({
    advancedProps: {
      ...(selectedComponent.value?.advancedProps || {}),
      unique: Boolean(value),
    },
  })
}

function handlePropertySearch(value = '') {
  const keyword = String(value || '').trim().toLowerCase()
  if (!keyword) {
    propertySearchHit.value = ''
    return
  }

  const hit = propertySearchIndex.find(item => item.keys.some(key => String(key).toLowerCase().includes(keyword) || keyword.includes(String(key).toLowerCase())))
  if (!hit) {
    expandAllSearchableSections()
    propertySearchHit.value = '已展开当前面板全部配置'
    return
  }

  propertySearchHit.value = hit.label
  if (selectedComponent.value) {
    if (!hit.selectedTab && hit.formTab) {
      emit('update:selectedId', '')
      formPropertyActiveTab.value = hit.formTab
      if (hit.formTab === 'style' && hit.formExpand?.length)
        formStyleExpandedNames.value = mergeExpandNames(formStyleExpandedNames.value, hit.formExpand)
      else if (hit.formExpand?.length)
        formBasicExpandedNames.value = mergeExpandNames(formBasicExpandedNames.value, hit.formExpand)
      return
    }
    propertyActiveTab.value = hit.selectedTab || hit.formTab || 'basic'
    if (hit.selectedExpand?.length)
      selectedBasicExpandedNames.value = mergeExpandNames(selectedBasicExpandedNames.value, hit.selectedExpand)
    return
  }

  formPropertyActiveTab.value = hit.formTab || hit.selectedTab || 'basic'
  if (hit.formPropertyTab === 'style' || formPropertyActiveTab.value === 'style') {
    if (hit.formExpand?.length)
      formStyleExpandedNames.value = mergeExpandNames(formStyleExpandedNames.value, hit.formExpand)
    return
  }
  if (hit.formExpand?.length)
    formBasicExpandedNames.value = mergeExpandNames(formBasicExpandedNames.value, hit.formExpand)
}

function mergeExpandNames(current = [], names = []) {
  return Array.from(new Set([...(Array.isArray(current) ? current : []), ...names]))
}

function expandAllSearchableSections() {
  if (selectedComponent.value) {
    propertyActiveTab.value = 'basic'
    selectedBasicExpandedNames.value = mergeExpandNames(selectedBasicExpandedNames.value, allSelectedBasicExpandNames)
    return
  }
  formPropertyActiveTab.value = 'basic'
  formBasicExpandedNames.value = mergeExpandNames(formBasicExpandedNames.value, allFormBasicExpandNames)
  formStyleExpandedNames.value = mergeExpandNames(formStyleExpandedNames.value, allFormStyleExpandNames)
}

function showFormSettings() {
  emit('update:selectedId', '')
  formPropertyActiveTab.value = 'basic'
  sourceError.value = ''
}

function updateCurrentFormMeta(patch = {}) {
  const nextFormKey = patch.formKey || props.schema.formKey
  const nextDefaultFormKey = patch.formKey && defaultFormKey.value === props.schema.formKey
    ? nextFormKey
    : defaultFormKey.value
  emit('update:schema', {
    ...props.schema,
    ...patch,
    usage: patch.usage ? resolveFormUsage(patch.usage) : props.schema.usage,
    defaultFormKey: nextDefaultFormKey,
    settings: {
      ...(props.schema.settings || {}),
      defaultFormKey: nextDefaultFormKey,
    },
  })
}

function setDefaultFormKey(formKey = '') {
  if (!formKey)
    return
  emit('update:schema', {
    ...props.schema,
    defaultFormKey: formKey,
    settings: {
      ...(props.schema.settings || {}),
      defaultFormKey: formKey,
    },
  })
}

function updateFormGovernance(patch = {}) {
  emit('update:schema', {
    ...props.schema,
    settings: {
      ...(props.schema.settings || {}),
      governance: {
        ...formGovernanceSettings.value,
        ...patch,
      },
    },
  })
}

function updateFormPermission(patch = {}) {
  updateFormGovernance({
    permission: {
      ...formPermissionConfig.value,
      ...patch,
    },
  })
}

function addFormFieldRule() {
  updateFormGovernance({
    fieldRules: [
      ...formFieldRuleRows.value,
      {
        id: `rule_${Date.now()}`,
        field: '',
        required: false,
        readonly: false,
        hidden: false,
        defaultValue: '',
      },
    ],
  })
}

function updateFormFieldRule(index, patch = {}) {
  const list = [...formFieldRuleRows.value]
  list[index] = { ...(list[index] || {}), ...patch }
  updateFormGovernance({ fieldRules: list })
}

function removeFormFieldRule(index) {
  const list = [...formFieldRuleRows.value]
  list.splice(index, 1)
  updateFormGovernance({ fieldRules: list })
}

function addFormEvent() {
  updateFormGovernance({
    events: [
      ...formEventRows.value,
      {
        id: `event_${Date.now()}`,
        hook: 'beforeLoad',
        action: 'customScript',
        handler: '',
        resultMapping: '',
      },
    ],
  })
}

function updateFormEvent(index, patch = {}) {
  const list = [...formEventRows.value]
  list[index] = { ...(list[index] || {}), ...patch }
  updateFormGovernance({ events: list })
}

function removeFormEvent(index) {
  const list = [...formEventRows.value]
  list.splice(index, 1)
  updateFormGovernance({ events: list })
}

function updateFormAssetMeta(formKey = '', patch = {}) {
  const nextPatch = {
    ...patch,
    ...(Object.prototype.hasOwnProperty.call(patch, 'usage')
      ? { usage: resolveFormUsage(patch.usage) }
      : {}),
  }
  emit('update:schema', {
    ...props.schema,
    settings: {
      ...(props.schema.settings || {}),
      formAssets: formAssets.value.map((asset) => {
        if (asset.formKey !== formKey)
          return asset
        return {
          ...asset,
          ...nextPatch,
          schema: asset.schema
            ? {
                ...asset.schema,
                ...nextPatch,
              }
            : asset.schema,
        }
      }),
    },
  })
}

function duplicateCurrentFormAsset() {
  const nextAssetKey = `${props.schema.formKey || 'form'}_dialog_${Date.now()}`
  const usage = resolveFormUsage(props.schema.usage)
  const assetSchema = cloneValue(props.schema)
  assetSchema.formKey = nextAssetKey
  assetSchema.formName = `${props.schema.formName || '表单'}弹窗`
  assetSchema.usage = usage
  assetSchema.settings = {
    ...(assetSchema.settings || {}),
    formAssets: [],
  }
  emit('update:schema', {
    ...props.schema,
    settings: {
      ...(props.schema.settings || {}),
      formAssets: [
        {
          formKey: nextAssetKey,
          formName: assetSchema.formName,
          usage,
          schema: assetSchema,
        },
        ...formAssets.value,
      ],
    },
  })
}

function createBlankFormAsset() {
  const nextAssetIndex = formAssets.value.length + 2
  const nextAssetKey = `${props.schema.formKey || 'form'}_form_${Date.now()}`
  const usage = ['create', 'edit']
  const assetSchema = {
    ...cloneValue(props.schema),
    formKey: nextAssetKey,
    formName: `表单 ${nextAssetIndex}`,
    usage,
    components: [],
    settings: {
      ...(props.schema.settings || {}),
      formAssets: [],
    },
  }
  emit('update:schema', {
    ...props.schema,
    settings: {
      ...(props.schema.settings || {}),
      formAssets: [
        {
          formKey: nextAssetKey,
          formName: assetSchema.formName,
          usage,
          schema: assetSchema,
        },
        ...formAssets.value,
      ],
    },
  })
}

function switchFormAsset(formKey = '') {
  const asset = formAssets.value.find(item => item.formKey === formKey)
  if (!asset?.schema)
    return
  const nextDefaultFormKey = defaultFormKey.value || props.schema.formKey
  const currentUsage = resolveFormUsage(props.schema.usage)
  const assetUsage = resolveFormUsage(asset.usage || asset.schema.usage)
  const currentAsset = {
    formKey: props.schema.formKey,
    formName: props.schema.formName,
    usage: currentUsage,
    schema: {
      ...cloneValue(props.schema),
      usage: currentUsage,
      settings: {
        ...(props.schema.settings || {}),
        formAssets: [],
      },
    },
  }
  const nextAssets = formAssets.value
    .filter(item => item.formKey !== formKey && item.formKey !== currentAsset.formKey)
    .concat(currentAsset)
  emit('update:selectedId', '')
  const nextSchema = normalizeFormDesignerSchema({
    ...cloneValue(asset.schema),
    usage: assetUsage,
    defaultFormKey: nextDefaultFormKey,
    settings: {
      ...(asset.schema.settings || {}),
      formAssets: nextAssets,
      defaultFormKey: nextDefaultFormKey,
    },
  })
  emit('update:schema', {
    ...nextSchema,
    usage: assetUsage,
    defaultFormKey: nextDefaultFormKey,
    settings: {
      ...(nextSchema.settings || {}),
      defaultFormKey: nextDefaultFormKey,
    },
  })
}

function removeFormAsset(formKey = '') {
  const nextDefaultFormKey = defaultFormKey.value === formKey ? props.schema.formKey : defaultFormKey.value
  emit('update:schema', {
    ...props.schema,
    defaultFormKey: nextDefaultFormKey,
    settings: {
      ...(props.schema.settings || {}),
      defaultFormKey: nextDefaultFormKey,
      formAssets: formAssets.value.filter(item => item.formKey !== formKey),
    },
  })
}

function resolveFormUsage(value) {
  const usage = Array.isArray(value)
    ? value.map(item => String(item || '').trim()).filter(Boolean)
    : []
  return usage.length ? Array.from(new Set(usage)) : ['create', 'edit']
}

function collectBoundFieldOptions(components = [], result = []) {
  ;(Array.isArray(components) ? components : []).forEach((component) => {
    const field = component?.fieldBinding?.fieldCode || component?.field || component?.props?.field
    if (field && !result.some(item => item.value === field)) {
      result.push({
        label: `${component.label || field}（${field}）`,
        value: field,
      })
    }
    collectBoundFieldOptions(component?.children || component?.props?.children || [], result)
  })
  return result
}

function updateLabel(value) {
  const component = selectedComponent.value
  const patch = { label: value }
  let propsPatch = null
  if (isCrudBlock.value) {
    propsPatch = {
      title: value,
    }
  }
  else if (isLayout.value) {
    propsPatch = {
      header: value,
      title: value,
      label: value,
    }
  }
  else if (isField.value && shouldSyncPlaceholder(component, value)) {
    propsPatch = {
      placeholder: buildDefaultPlaceholder(component?.componentKey, value),
    }
  }
  if (propsPatch)
    patch.props = propsPatch
  updateComponent(patch)
}

function shouldSyncPlaceholder(component) {
  if (!component)
    return false
  const currentPlaceholder = component.props?.placeholder
  if (currentPlaceholder === undefined || currentPlaceholder === null)
    return true
  const currentText = String(currentPlaceholder).trim()
  if (!currentText)
    return true
  return isAutoGeneratedPlaceholder(component, currentText)
}

function buildDefaultPlaceholder(componentKey = '', label = '') {
  const cleanLabel = String(label || '').trim()
  const prefix = isChoicePlaceholderComponent(componentKey) ? '请选择' : '请填写'
  return cleanLabel ? `${prefix}${cleanLabel}` : prefix
}

function buildAutoPlaceholderCandidates(componentKey = '', label = '') {
  const cleanLabel = String(label || '').trim()
  const prefixes = isChoicePlaceholderComponent(componentKey)
    ? ['请选择']
    : ['请填写', '请输入', '请填入']
  return prefixes.map(prefix => cleanLabel ? `${prefix}${cleanLabel}` : prefix)
}

function isAutoGeneratedPlaceholder(component = {}, placeholder = '') {
  const text = String(placeholder || '').trim()
  if (!text)
    return true
  const labels = resolveAutoPlaceholderLabels(component)
  return labels.some(label => buildAutoPlaceholderCandidates(component.componentKey, label).includes(text))
}

function resolveAutoPlaceholderLabels(component = {}) {
  const labels = [
    component.label,
    component.props?.label,
    component.props?.title,
    component.props?.header,
    component.fieldBinding?.fieldName,
    component.fieldBinding?.fieldCode,
    component.field,
    component.name,
    resolveComponentDefaultLabel(component.componentKey),
    '字段',
  ]
  return Array.from(new Set(labels.map(label => String(label || '').trim()).filter(Boolean)))
}

function resolveComponentDefaultLabel(componentKey = '') {
  const labelMap = {
    input: '输入框',
    textarea: '多行文本',
    number: '数字',
    money: '金额',
    slider: '滑块',
    rate: '评分',
    color: '颜色选择',
    select: '静态下拉',
    dictSelect: '字典下拉',
    radio: '单选',
    radioButton: '按钮单选',
    checkbox: '多选',
    transfer: '穿梭框',
    cascader: '级联选择',
    treeSelect: '树形选择',
    customSelect: '远程选择',
    date: '日期',
    datetime: '日期时间',
    time: '时间',
    daterange: '日期范围',
    datetimerange: '日期时间范围',
    timerange: '时间范围',
    fileUpload: '文件上传',
    imageUpload: '图片上传',
    userSelect: '人员选择',
    orgTreeSelect: '组织选择',
    regionTreeSelect: '区域选择',
  }
  return labelMap[componentKey] || ''
}

function isChoicePlaceholderComponent(componentKey = '') {
  const key = String(componentKey || '').toLowerCase()
  return [
    'select',
    'dictselect',
    'radio',
    'radiobutton',
    'checkbox',
    'cascader',
    'treeselect',
    'dateselect',
    'datepicker',
    'timepicker',
    'time',
    'date',
    'datetime',
    'upload',
    'fileupload',
    'imageupload',
    'userselect',
    'deptselect',
    'transfer',
  ].includes(key)
}

async function loadDefaultValueDictOptions(dictType = '') {
  const normalizedType = String(dictType || '').trim()
  if (!normalizedType || dictDefaultOptions.value[normalizedType] || dictDefaultOptionsLoading.value[normalizedType])
    return
  dictDefaultOptionsLoading.value = {
    ...dictDefaultOptionsLoading.value,
    [normalizedType]: true,
  }
  try {
    const data = await getDictData(normalizedType)
    dictDefaultOptions.value = {
      ...dictDefaultOptions.value,
      [normalizedType]: normalizeDefaultValueOptions(data),
    }
  }
  finally {
    const nextLoading = { ...dictDefaultOptionsLoading.value }
    delete nextLoading[normalizedType]
    dictDefaultOptionsLoading.value = nextLoading
  }
}

function normalizeDefaultValueOptions(options = []) {
  return (Array.isArray(options) ? options : [])
    .map((option) => {
      if (option == null)
        return null
      if (typeof option !== 'object') {
        return {
          label: String(option),
          value: option,
        }
      }
      const value = option.value ?? option.dictValue ?? option.key
      if (value === undefined || value === null)
        return null
      const label = option.label ?? option.dictLabel ?? option.title ?? String(value)
      return {
        label: formatDefaultValueOptionLabel(label, value),
        value,
        disabled: option.disabled === true,
      }
    })
    .filter(Boolean)
}

function formatDefaultValueOptionLabel(label, value) {
  const labelText = String(label ?? '')
  const valueText = String(value ?? '')
  return labelText && valueText && labelText !== valueText ? `${labelText}（${valueText}）` : labelText || valueText
}

function updateDefaultValue(value) {
  if (defaultValueSelectMultiple.value) {
    const nextValue = Array.isArray(value)
      ? value
      : value === undefined || value === null || value === ''
        ? []
        : [value]
    updateComponent({ props: { defaultValue: nextValue.length ? nextValue : undefined } })
    return
  }
  updateComponent({ props: { defaultValue: value === null || value === undefined || value === '' ? undefined : value } })
}

function addLayoutChild(componentKey = '') {
  if (!selectedComponent.value)
    return
  const nextChildren = cloneValue(layoutChildren.value || [])
  const index = nextChildren.length + 1
  const isTabsChild = componentKey === 'tabPane'
  const label = isTabsChild ? `标签 ${index}` : `分组 ${index}`
  const id = `${selectedComponent.value.id}_${isTabsChild ? 'pane' : 'item'}_${Date.now()}`
  nextChildren.push({
    id,
    componentKey: isTabsChild ? 'tabPane' : 'collapseItem',
    label,
    props: isTabsChild ? { label, name: id } : { title: label, name: id },
    layout: {
      span: selectedComponent.value.layout?.span || normalizedFormGridColumns.value,
      align: 'left',
    },
    children: [],
  })
  updateComponent({ children: nextChildren })
  emit('update:selectedId', id)
}

function updateLayoutChild(index, patch = {}) {
  const nextChildren = cloneValue(layoutChildren.value || [])
  if (!nextChildren[index])
    return
  nextChildren[index] = {
    ...nextChildren[index],
    ...patch,
    props: patch.props
      ? {
          ...(nextChildren[index].props || {}),
          ...patch.props,
        }
      : nextChildren[index].props,
    layout: patch.layout
      ? {
          ...(nextChildren[index].layout || {}),
          ...patch.layout,
        }
      : nextChildren[index].layout,
  }
  updateComponent({ children: nextChildren })
}

function moveLayoutChild(index, delta) {
  const nextChildren = cloneValue(layoutChildren.value || [])
  const nextIndex = index + delta
  if (!nextChildren[index] || nextIndex < 0 || nextIndex >= nextChildren.length)
    return
  const [item] = nextChildren.splice(index, 1)
  nextChildren.splice(nextIndex, 0, item)
  updateComponent({ children: nextChildren })
}

function removeLayoutChild(index) {
  const nextChildren = cloneValue(layoutChildren.value || [])
  if (nextChildren.length <= 1 || !nextChildren[index])
    return
  const [removed] = nextChildren.splice(index, 1)
  updateComponent({ children: nextChildren })
  if (removed?.id === props.selectedId)
    emit('update:selectedId', selectedComponent.value?.id || '')
}

function addInteractionRule() {
  const nextRules = cloneValue(interactionRules.value || [])
  nextRules.push({
    id: `evt_${Date.now()}`,
    trigger: defaultTrigger.value,
    action: defaultTrigger.value === 'click' ? 'openModal' : 'setValue',
    targetId: '',
    condition: '',
  })
  updateComponent({ props: { __events: nextRules } })
}

function addInteractionPreset(key = '') {
  const nextRules = cloneValue(interactionRules.value || [])
  const base = {
    id: `evt_${Date.now()}`,
    trigger: defaultTrigger.value,
    action: 'setValue',
    targetId: '',
    whenValue: '',
  }
  const map = {
    cascade: {
      trigger: 'change',
      action: 'setOptions',
      api: 'get@/api/options?parent=:value',
    },
    openModal: {
      trigger: 'click',
      action: 'openModal',
      modalTitle: '业务弹窗',
      modalContentMode: 'currentForm',
    },
    showHide: {
      trigger: 'change',
      action: 'showHide',
      value: 'true',
    },
    enableDisable: {
      trigger: 'change',
      action: 'enableDisable',
      value: 'true',
    },
    apiRequest: {
      trigger: defaultTrigger.value,
      action: 'apiRequest',
      api: 'post@/api/action',
    },
  }
  nextRules.push({
    ...base,
    ...(map[key] || {}),
  })
  updateComponent({ props: { __events: nextRules } })
}

function configureLinkageRule(action = 'showHide') {
  addInteractionPreset(action)
  propertyActiveTab.value = 'interaction'
}

function updateInteractionRule(index, patch = {}) {
  const nextRules = cloneValue(interactionRules.value || [])
  if (!nextRules[index])
    return
  nextRules[index] = {
    ...nextRules[index],
    ...patch,
  }
  updateComponent({ props: { __events: nextRules } })
}

function updateInteractionRuleJson(index, key, value = '') {
  const patch = { [`${key}Json`]: value }
  try {
    patch[key] = value?.trim() ? JSON.parse(value) : undefined
  }
  catch {
    patch[key] = value
  }
  updateInteractionRule(index, patch)
}

function removeInteractionRule(index) {
  const nextRules = cloneValue(interactionRules.value || [])
  nextRules.splice(index, 1)
  updateComponent({ props: { __events: nextRules } })
}

function resolveTriggerLabel(value = '') {
  return triggerOptions.find(item => item.value === value)?.label || '事件规则'
}

function resolveActionLabel(value = '') {
  return actionOptions.find(item => item.value === value)?.label || '未选择动作'
}

function resolveActionValueMeta(action = '') {
  const map = {
    setValue: {
      label: '要填入目标的值',
      placeholder: '例如：已处理',
      help: '触发后会把这个值写入目标组件。',
    },
    showHide: {
      label: '是否显示目标',
      placeholder: 'true 显示，false 隐藏',
      help: '用于根据当前字段值显示或隐藏目标组件。',
    },
    enableDisable: {
      label: '是否启用目标',
      placeholder: 'true 启用，false 禁用',
      help: '用于根据当前字段值启用或禁用目标组件。',
    },
  }
  return map[action] || {
    label: '动作值',
    placeholder: '请输入',
    help: '这条动作执行时使用的值。',
  }
}

function openCrudFieldDrawer(componentId = '') {
  editingCrudFieldId.value = componentId
  crudFieldDrawerVisible.value = true
}

function updateEditingCrudFieldConfig(area, patch = {}) {
  if (!editingCrudFieldId.value)
    return
  updateCrudFieldConfigById(editingCrudFieldId.value, area, patch)
}

function collectComponentTargetOptions(components = [], result = [], depth = 0) {
  ;(Array.isArray(components) ? components : []).forEach((component) => {
    if (!component?.id)
      return
    const prefix = depth ? `${'　'.repeat(depth)}` : ''
    result.push({
      label: `${prefix}${component.label || component.props?.label || component.props?.title || component.componentKey} (${component.fieldBinding?.fieldCode || component.id})`,
      value: component.id,
    })
    collectComponentTargetOptions(component.children || [], result, depth + 1)
  })
  return result
}

function collectDictTypeFields(components = [], result = []) {
  ;(Array.isArray(components) ? components : []).forEach((component) => {
    if (!component)
      return
    const dictType = component.props?.dictType || component.dictType || component.basicProps?.dictType || component.advancedProps?.dictType
    if (dictType)
      result.push({ dictType })
    collectDictTypeFields(component.children || [], result)
  })
  return result
}

function updateSelectedCodeDraft(value) {
  selectedCodeDraftTarget.value = props.selectedId
  selectedCodeDraft.value = value
  sourceError.value = ''
}

function resetSelectedCodeDraft() {
  selectedCodeDraftTarget.value = props.selectedId
  selectedCodeDraft.value = selectedComponentCodeRaw.value
  sourceError.value = ''
}

function applySelectedCode() {
  if (!props.selectedId)
    return false
  try {
    const parsed = JSON.parse(selectedCodeText.value || '{}')
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed))
      throw new Error('当前组件 JSON 必须是对象')
    const nextSchema = replaceComponentInSchema(props.schema, props.selectedId, parsed)
    emit('update:schema', normalizeFormDesignerSchema(nextSchema))
    sourceError.value = ''
    selectedCodeDraftTarget.value = ''
    selectedCodeDraft.value = ''
    return true
  }
  catch (error) {
    sourceError.value = error?.message || 'JSON 解析失败'
    return false
  }
}

function updateSchemaCodeDraft(value) {
  schemaCodeDirty.value = true
  schemaCodeDraft.value = value
  sourceError.value = ''
}

function resetSchemaCodeDraft() {
  schemaCodeDirty.value = true
  schemaCodeDraft.value = schemaCodeRaw.value
  sourceError.value = ''
}

function applySchemaCode() {
  try {
    const parsed = JSON.parse(schemaCodeText.value || '{}')
    emit('update:schema', normalizeFormDesignerSchema(parsed))
    sourceError.value = ''
    schemaCodeDirty.value = false
    schemaCodeDraft.value = ''
    return true
  }
  catch (error) {
    sourceError.value = error?.message || 'JSON 解析失败'
    return false
  }
}

function applySourceModalCode() {
  const applied = selectedComponent.value ? applySelectedCode() : applySchemaCode()
  if (applied)
    sourceModalVisible.value = false
}

function cancelSourceModalEdit() {
  if (selectedComponent.value)
    resetSelectedCodeDraft()
  else
    resetSchemaCodeDraft()
  sourceModalVisible.value = false
}

function replaceComponentInSchema(schema = {}, componentId = '', replacement = {}) {
  const nextSchema = cloneValue(schema)
  const path = findDesignerComponentPath(nextSchema, componentId)
  if (!path)
    return nextSchema
  let children = nextSchema.components || []
  for (let index = 0; index < path.length - 1; index += 1)
    children = children[path[index]].children || []
  children[path[path.length - 1]] = replacement
  return nextSchema
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

function normalizeAppearanceColor(value, fallback = '#ffffff') {
  return hexInputToColor(value, fallback)
}

function normalizeOpacityPercent(value, fallback = 100) {
  const number = Number(value)
  if (!Number.isFinite(number))
    return fallback
  return Math.min(100, Math.max(20, number))
}

function updateSelectedAppearanceBackground(value) {
  updateDesignerStyle({ backgroundColor: normalizeAppearanceColor(value, 'transparent') })
}

function updateSelectedAppearanceBorder(value) {
  updateDesignerBorderColor(normalizeAppearanceColor(value, '#e4e4e7'))
}

function updateSelectedAppearanceOpacity(value) {
  updateDesignerStyle({ opacity: normalizeOpacityPercent(value, selectedOpacityPercent.value) / 100 })
}

function updateDesignerStyle(stylePatch = {}) {
  updateComponent({
    props: {
      __designerStyle: {
        ...selectedDesignerStyle.value,
        ...stylePatch,
      },
    },
  })
}

function updateDesignerBorderStyle(value) {
  if (value === 'none') {
    updateDesignerStyle({
      borderStyle: 'none',
      borderColor: 'transparent',
      hideInnerBorder: true,
    })
    return
  }
  updateDesignerStyle({
    borderStyle: value || undefined,
    borderColor: selectedDesignerStyle.value.borderColor === 'transparent'
      ? undefined
      : selectedDesignerStyle.value.borderColor,
    hideInnerBorder: false,
  })
}

function updateDesignerBorderColor(value) {
  if (!value) {
    updateDesignerStyle({ borderColor: undefined })
    return
  }
  updateDesignerStyle({
    borderColor: value,
    borderStyle: selectedDesignerStyle.value.borderStyle === 'none'
      ? 'solid'
      : selectedDesignerStyle.value.borderStyle || 'solid',
    hideInnerBorder: false,
  })
}

function updateWidthMode(value) {
  updateDesignerStyle({
    widthMode: value,
    width: value === 'fill' ? '100%' : undefined,
  })
}

function updateHeightMode(value) {
  updateDesignerStyle({
    heightMode: value,
    minHeight: value === 'fill' ? '100%' : undefined,
    height: value === 'default' ? undefined : 'auto',
  })
}

function updateComponentStyleText(value) {
  updateDesignerStyle({
    customStyleText: value,
    customStyle: parseStyleText(value),
  })
}

function updateDesignerSpacing(key, value) {
  const nextCustomStyle = applyStyleValue(selectedDesignerStyle.value.customStyle || {}, key, valueToPx(value))
  updateDesignerStyle({
    customStyle: nextCustomStyle,
    customStyleText: stringifyStyle(nextCustomStyle),
  })
}

function updateDesignerCustomStyle(patch = {}) {
  const nextCustomStyle = Object.entries(patch).reduce((style, [key, value]) => applyStyleValue(style, key, value), {
    ...(selectedDesignerStyle.value.customStyle || {}),
  })
  updateDesignerStyle({
    customStyle: nextCustomStyle,
    customStyleText: stringifyStyle(nextCustomStyle),
  })
}

function updateDesignerTranslate(axis, value) {
  const current = selectedDesignerTranslate.value
  const next = {
    x: axis === 'x' ? normalizePositionNumber(value) : current.x,
    y: axis === 'y' ? normalizePositionNumber(value) : current.y,
  }
  updateDesignerCustomStyle({ transform: formatTranslateStyle(next) })
}

function updateFormStyle(stylePatch = {}) {
  updateFormLayout({
    formStyle: {
      ...formStyle.value,
      ...stylePatch,
    },
  })
}

function updateFormAppearanceBackground(value) {
  updateFormStyle({ backgroundColor: normalizeAppearanceColor(value, 'transparent') })
}

function updateFormAppearanceBorder(value) {
  updateFormStyle({
    borderColor: normalizeAppearanceColor(value, '#e4e4e7'),
    borderStyle: formStyle.value.borderStyle === 'none'
      ? 'solid'
      : formStyle.value.borderStyle || 'solid',
  })
}

function updateFormAppearanceOpacity(value) {
  updateFormStyle({ opacity: normalizeOpacityPercent(value, formOpacityPercent.value) / 100 })
}

function updateFormStyleText(value) {
  updateFormLayout({
    formStyleText: value,
    formStyle: parseStyleText(value),
  })
}

function updateFormSpacing(key, value) {
  const nextStyle = applyStyleValue(formStyle.value || {}, key, valueToPx(value))
  updateFormLayout({
    formStyle: nextStyle,
    formStyleText: stringifyStyle(nextStyle),
  })
}

function updateFormTranslate(axis, value) {
  const current = formTranslate.value
  const next = {
    x: axis === 'x' ? normalizePositionNumber(value) : current.x,
    y: axis === 'y' ? normalizePositionNumber(value) : current.y,
  }
  updateFormStyle({ transform: formatTranslateStyle(next) })
}

function updateCrudApiBase(value) {
  const apiBase = normalizeApiBase(value)
  updateComponent({
    props: {
      apiBase,
      apiConfig: buildDefaultCrudApiConfig(apiBase),
    },
  })
}

function updateCrudApiConfig(key, value) {
  updateComponent({
    props: {
      apiConfig: {
        ...crudApiConfig.value,
        [key]: value,
      },
    },
  })
}

function resolveCrudApiMethodLabel(item = {}) {
  const apiValue = crudApiConfig.value?.[item.key] || item.placeholder || ''
  const method = String(apiValue).split('@')[0]?.trim()?.toUpperCase()
  if (method === 'DELETE')
    return 'DEL'
  return method || 'API'
}

function resolveCrudApiMethodClass(item = {}) {
  const label = resolveCrudApiMethodLabel(item).toLowerCase()
  if (label === 'del')
    return 'delete'
  return ['get', 'post', 'put'].includes(label) ? label : 'post'
}

function updateCrudOption(key, value) {
  updateComponent({
    props: {
      crudOptions: {
        ...crudOptions.value,
        [key]: value,
      },
    },
  })
}

function updateCrudExpandEnabled(enabled) {
  const current = crudOptions.value?.expandConfig || {}
  updateCrudOption('expandConfig', enabled
    ? {
        enabled: true,
        trigger: current.trigger || 'icon',
        lazy: current.lazy !== false,
        cache: current.cache !== false,
        layout: current.layout || { mode: 'single', density: 'compact', padding: 12 },
        panels: current.panels?.length ? current.panels : [createDefaultCrudExpandPanel()],
      }
    : { ...current, enabled: false })
}

function updateCrudExpandConfig(patch = {}) {
  const current = crudOptions.value?.expandConfig || {}
  updateCrudOption('expandConfig', {
    ...current,
    ...patch,
    enabled: current.enabled === true,
  })
}

function updateFirstCrudExpandPanel(patch = {}) {
  const current = crudOptions.value?.expandConfig || {}
  const panels = current.panels?.length ? [...current.panels] : [createDefaultCrudExpandPanel()]
  panels[0] = normalizeCrudExpandPanelPatch({ ...panels[0], ...patch })
  updateCrudOption('expandConfig', {
    ...current,
    enabled: true,
    panels,
  })
}

function updateFirstCrudExpandDataSource(patch = {}) {
  updateFirstCrudExpandPanel({
    dataSource: {
      ...(firstCrudExpandPanel.value?.dataSource || {}),
      ...patch,
    },
  })
}

function updateFirstCrudExpandParamsMap(value) {
  updateFirstCrudExpandDataSource({ paramsMap: parseJsonObjectProp(value) })
}

function updateFirstCrudExpandDescriptionFields(value) {
  const fields = Array.isArray(value)
    ? value.map(item => String(item || '').trim()).filter(Boolean)
    : String(value || '').split(/\r?\n|,/).map(item => item.trim()).filter(Boolean)
  updateFirstCrudExpandPanel({
    descriptions: {
      ...(firstCrudExpandPanel.value?.descriptions || {}),
      fields: fields.map(field => ({ field, label: resolveCrudFieldLabel(field) })),
    },
  })
}

function resolveCrudExpandDescriptionFieldKeys(panel) {
  const configured = (panel?.descriptions?.fields || [])
    .map(field => field.field || field.key)
    .filter(Boolean)
  if (configured.length)
    return configured
  return resolveDefaultCrudDescriptionFields().map(field => field.field).filter(Boolean)
}

function resolveCrudExpandDescriptionSelectedFields(panel) {
  const optionMap = new Map(crudDescriptionFieldOptions.value.map(option => [option.value, option]))
  return resolveCrudExpandDescriptionFieldKeys(panel).map((field) => {
    const option = optionMap.get(field)
    return {
      field,
      label: option?.rawLabel || resolveCrudFieldLabel(field),
      componentKey: option?.componentKey,
    }
  })
}

function resolveCrudExpandDescriptionPanelFields(panel) {
  const selectedKeys = resolveCrudExpandDescriptionFieldKeys(panel)
  const selected = new Set(selectedKeys)
  const optionMap = new Map(crudDescriptionFieldOptions.value.map(option => [option.value, option]))
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
        label: option?.rawLabel || resolveCrudFieldLabel(field),
        componentKey: option?.componentKey,
        selected: selected.has(field),
      }
    })
    .filter(Boolean)
}

function addFirstCrudExpandDescriptionField(field) {
  if (!field)
    return
  const fields = resolveCrudExpandDescriptionFieldKeys(firstCrudExpandPanel.value)
  updateFirstCrudExpandDescriptionFields(Array.from(new Set([...fields, field])))
}

function removeFirstCrudExpandDescriptionField(field) {
  const fields = resolveCrudExpandDescriptionFieldKeys(firstCrudExpandPanel.value)
  updateFirstCrudExpandDescriptionFields(fields.filter(item => item !== field))
}

function toggleFirstCrudExpandDescriptionField(field, visible) {
  if (visible)
    addFirstCrudExpandDescriptionField(field)
  else
    removeFirstCrudExpandDescriptionField(field)
}

function handleCrudExpandDescriptionPanelReorder(fields = []) {
  updateFirstCrudExpandDescriptionFields(fields.filter(field => field?.selected).map(field => field.field))
}

function resolveCrudBitableFieldIconComponent(field = {}) {
  const key = String(field.componentKey || '').toLowerCase()
  if (['number', 'inputnumber', 'input-number', 'rate', 'slider'].includes(key))
    return BitableNumberIcon
  if (['date', 'datetime', 'datepicker', 'timepicker', 'time', 'month', 'year'].includes(key))
    return BitableCalendarIcon
  if (['select', 'dictselect', 'radio', 'radiobutton', 'checkbox', 'cascader', 'treeselect', 'switch'].includes(key))
    return BitableSelectIcon
  if (['upload', 'fileupload', 'imageupload'].includes(key))
    return BitableAttachmentIcon
  if (['userselect', 'deptselect'].includes(key))
    return BitableMemberIcon
  if (['lookup', 'relation', 'reference'].includes(key))
    return BitableLookupIcon
  return BitableStyleIcon
}

function createDefaultCrudExpandPanel() {
  return {
    key: 'summary',
    title: '概览',
    type: 'descriptions',
    dataSource: { type: 'row' },
    descriptions: {
      columns: 3,
      fields: resolveDefaultCrudDescriptionFields(),
    },
  }
}

function normalizeCrudExpandPanelPatch(panel = {}) {
  const type = panel.type || 'descriptions'
  const quantityPanel = ['quantity-balance', 'quantity-ledger', 'quantity-lock'].includes(type)
  return {
    ...panel,
    key: panel.key || (type === 'table' ? 'detailTable' : quantityPanel ? type : 'summary'),
    title: panel.title || (type === 'table' ? '明细' : quantityPanel ? expandPanelTypeOptions.find(item => item.value === type)?.label : '概览'),
    type,
    dataSource: quantityPanel ? { ...(panel.dataSource || {}), type: 'quantity', queryType: type } : panel.dataSource || { type: 'row' },
    quantity: quantityPanel ? { ...(panel.quantity || {}), queryType: type } : panel.quantity,
    descriptions: panel.descriptions || { columns: 3, fields: resolveDefaultCrudDescriptionFields() },
    table: panel.table || { rowKey: 'id', columns: [], pagination: false, maxHeight: 320 },
  }
}

function resolveDefaultCrudDescriptionFields() {
  return crudConfigFields.value.slice(0, 6).map(field => ({
    field: field.fieldCode,
    label: field.label || field.fieldCode,
  }))
}

function resolveCrudFieldLabel(fieldCode) {
  const field = crudConfigFields.value.find(item => item.fieldCode === fieldCode || item.id === fieldCode)
  return field?.label || fieldCode
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

function normalizeFormOpenModePatch(value) {
  const formOpenMode = value === 'tabWorkspace' ? 'tabWorkspace' : (['modal', 'drawer', 'flat'].includes(value) ? value : 'modal')
  return {
    formOpenMode,
    modalType: ['modal', 'drawer'].includes(formOpenMode) ? formOpenMode : 'modal',
  }
}

function updateFormOpenModeLayout(value) {
  updateFormLayout(normalizeFormOpenModePatch(value))
}

function updateFormModalWidth(value) {
  const width = value || '800px'
  updateFormLayout({
    modalWidth: width,
    detailModalWidth: width,
  })
}

function updateCrudFieldRole(componentId, role, value) {
  if (!componentId)
    return
  const field = getDesignerComponent(props.schema, componentId)
  const currentRoles = field?.props?.__crudRoles || {}
  emit('update:schema', updateDesignerComponent(props.schema, componentId, {
    props: {
      __crudRoles: {
        ...currentRoles,
        [role]: Boolean(value),
      },
    },
  }))
}

function updateCrudFieldConfig(area, patch = {}) {
  if (!props.selectedId || !area)
    return
  const current = selectedComponent.value?.props?.__crudConfig || {}
  updateComponent({
    props: {
      __crudConfig: {
        ...current,
        [area]: {
          ...(current[area] || {}),
          ...patch,
        },
      },
    },
  })
}

function updateCrudFieldConfigById(componentId, area, patch = {}) {
  if (!componentId || !area)
    return
  const field = getDesignerComponent(props.schema, componentId)
  const current = field?.props?.__crudConfig || {}
  emit('update:schema', updateDesignerComponent(props.schema, componentId, {
    props: {
      __crudConfig: {
        ...current,
        [area]: {
          ...(current[area] || {}),
          ...patch,
        },
      },
    },
  }))
}

function updateOption(index, patch = {}) {
  const nextOptions = cloneValue(selectedOptions.value || [])
  if (!nextOptions[index])
    return
  nextOptions[index] = {
    ...nextOptions[index],
    ...patch,
  }
  updateComponent({ props: { options: nextOptions } })
}

function updateOptionJsonProps(index, value = '') {
  try {
    updateOption(index, { props: value?.trim() ? JSON.parse(value) : undefined })
  }
  catch {
    updateOption(index, { props: value })
  }
}

function addOption() {
  const nextOptions = cloneValue(selectedOptions.value || [])
  const nextIndex = nextOptions.length + 1
  nextOptions.push({ label: `选项${nextIndex}`, value: `${nextIndex}` })
  updateComponent({ props: { options: nextOptions } })
}

function removeOption(index) {
  const nextOptions = cloneValue(selectedOptions.value || [])
  nextOptions.splice(index, 1)
  updateComponent({ props: { options: nextOptions } })
}

function updateSelectedProp(item = {}, value) {
  if (!item.key)
    return
  if (item.key === 'dictType') {
    updateDictType(value)
    return
  }
  updateComponent({
    props: {
      [item.key]: normalizePropValue(item, value),
    },
  })
}

function updateJsonProp(item = {}, value = '') {
  if (!item.key)
    return
  try {
    updateSelectedProp(item, value?.trim() ? JSON.parse(value) : undefined)
  }
  catch {
    updateComponent({
      props: {
        [item.key]: value,
      },
    })
  }
}

function resolvePropValue(item = {}) {
  const value = selectedComponent.value?.props?.[item.key]
  return value === undefined ? item.defaultValue : value
}

function normalizePropValue(item = {}, value) {
  if (value === '' || value === null)
    return undefined
  if (item.type === 'number')
    return value === undefined ? undefined : Number(value)
  return value
}

function stringifyJsonProp(value) {
  if (value === undefined || value === null || value === '')
    return ''
  if (typeof value === 'string')
    return value
  return JSON.stringify(value, null, 2)
}

function resolveCrudSwitchValue(key, defaultValue = false) {
  const value = crudOptions.value?.[key]
  return value === undefined ? defaultValue : Boolean(value)
}

function collectCrudConfigFields(components = [], result = []) {
  ;(Array.isArray(components) ? components : []).forEach((component) => {
    if (!component)
      return
    if (isFieldComponent(component)) {
      const index = result.length
      result.push({
        id: component.id,
        label: component.label || component.fieldBinding?.fieldCode || component.componentKey,
        fieldCode: component.fieldBinding?.fieldCode || component.id,
        componentKey: component.componentKey,
        roles: resolveCrudFieldRoles(component, index),
        config: component.props?.__crudConfig || {},
      })
      return
    }
    collectCrudConfigFields(component.children || [], result)
  })
  return result
}

function resolveCrudFieldRoles(component = {}, index = 0) {
  const roles = component.props?.__crudRoles || {}
  return {
    search: Object.prototype.hasOwnProperty.call(roles, 'search') ? roles.search !== false : index < 3,
    table: Object.prototype.hasOwnProperty.call(roles, 'table') ? roles.table !== false : true,
    edit: Object.prototype.hasOwnProperty.call(roles, 'edit') ? roles.edit !== false : true,
  }
}

function updateFormLayout(patch = {}) {
  emit('update:schema', updateDesignerLayout(props.schema, patch))
}

function updateRowTotalColumns(value) {
  const columns = normalizeGridCount(value)
  const row = selectedComponent.value
  if (!row)
    return
  const children = cloneValue(row.children || []).map(child => child?.componentKey === 'col'
    ? {
        ...child,
        props: { ...(child.props || {}), span: Math.min(columns, normalizeGridCount(child.props?.span || child.layout?.span || 1)) },
        layout: { ...(child.layout || {}), span: Math.min(columns, normalizeGridCount(child.layout?.span || child.props?.span || 1)) },
      }
    : child)
  updateComponent({
    props: {
      ...(row.props || {}),
      columns,
      gutter: row.props?.gutter ?? 16,
    },
    children,
  })
}

function updateRowCellCount(value) {
  const count = normalizeGridCount(value)
  const row = selectedComponent.value
  if (!row)
    return
  const totalColumns = normalizeGridCount(row.props?.columns || maxFormGridColumns)
  const defaultSpan = Math.max(1, Math.floor(totalColumns / Math.max(1, count)))

  const currentChildren = Array.isArray(row.children) ? cloneValue(row.children) : []
  const currentColumns = currentChildren.filter(child => child?.componentKey === 'col')
  const directChildren = currentChildren.filter(child => child?.componentKey !== 'col')
  if (!currentColumns.length && directChildren.length) {
    currentColumns.push(createColumn(row.id, 0))
    currentColumns[0].children = directChildren
  }
  else if (currentColumns.length && directChildren.length) {
    currentColumns[0].children = [
      ...(currentColumns[0].children || []),
      ...directChildren,
    ]
  }

  const nextColumns = currentColumns.slice(0, count)
  while (nextColumns.length < count)
    nextColumns.push(createColumn(row.id, nextColumns.length, defaultSpan))

  const overflowColumns = currentColumns.slice(count)
  if (overflowColumns.length && nextColumns.length) {
    const lastColumn = nextColumns[nextColumns.length - 1]
    lastColumn.children = [
      ...(lastColumn.children || []),
      ...overflowColumns.flatMap(column => column.children || []),
    ]
  }

  nextColumns.forEach((column, index) => {
    const span = Math.min(totalColumns, normalizeGridCount(column.layout?.span || column.props?.span || defaultSpan))
    column.label = `第 ${index + 1} 列`
    column.layout = { ...(column.layout || {}), span }
    column.props = { ...(column.props || {}), span }
  })

  updateComponent({
    label: `${count} 列栅格`,
    props: {
      columns: totalColumns,
      gutter: row.props?.gutter ?? 16,
    },
    children: nextColumns,
  })
}

function updateRowColumnSpan(index, value) {
  const row = selectedComponent.value
  if (!row || !Array.isArray(row.children))
    return
  const span = Math.min(normalizeGridCount(row.props?.columns || maxFormGridColumns), normalizeGridCount(value))
  const children = cloneValue(row.children)
  const childIndex = children.reduce((matchedIndex, child, currentIndex) => {
    if (matchedIndex !== -1 || child?.componentKey !== 'col')
      return matchedIndex
    const currentColumnIndex = children.slice(0, currentIndex + 1).filter(item => item?.componentKey === 'col').length - 1
    return currentColumnIndex === index ? currentIndex : -1
  }, -1)
  const column = children[childIndex]
  if (!column || column.componentKey !== 'col')
    return
  children[childIndex] = {
    ...column,
    props: { ...(column.props || {}), span },
    layout: { ...(column.layout || {}), span },
  }
  updateComponent({ children })
}

function createColumn(rowId, index, span = 6) {
  const normalizedSpan = normalizeGridCount(span)
  return {
    id: `${rowId || 'row'}_col_${Date.now()}_${index + 1}`,
    componentKey: 'col',
    label: `第 ${index + 1} 列`,
    props: { span: normalizedSpan },
    layout: { span: normalizedSpan, align: 'left' },
    children: [],
  }
}

function normalizeGridCount(value) {
  const number = Number(value)
  return Math.max(1, Math.min(maxFormGridColumns, Number.isFinite(number) ? number : 2))
}

function normalizeLabelWidthInput(value) {
  const text = String(value ?? '').trim()
  if (!text || text === 'auto')
    return 'auto'
  const number = Number(text)
  return Number.isFinite(number) ? number : text
}

function normalizeApiBase(value) {
  const text = String(value || '').trim().replace(/\/+/g, '/')
  if (!text)
    return '/business/object'
  return text.startsWith('/') ? text : `/${text}`
}

function buildDefaultCrudApiConfig(apiBase = '/business/object') {
  return {
    list: `get@${apiBase}/page`,
    detail: `post@${apiBase}/getById`,
    add: `post@${apiBase}/add`,
    update: `post@${apiBase}/edit`,
    delete: `post@${apiBase}/remove/:id`,
  }
}

function hasAncestorComponent(schema = {}, componentId = '', componentKeys = []) {
  const path = findDesignerComponentPath(schema, componentId)
  if (!path || path.length <= 1)
    return false
  const wanted = new Set(componentKeys)
  let children = schema.components || []
  for (let index = 0; index < path.length - 1; index += 1) {
    const component = children[path[index]]
    if (!component)
      return false
    if (wanted.has(component.componentKey))
      return true
    children = component.children || []
  }
  return false
}

function buildNaivePropGroups(componentKey = '') {
  const key = componentKey || ''
  const commonInputProps = [
    prop('size', 'size', 'select', componentSizeOptions.filter(item => item.value)),
    prop('clearable', 'clearable', 'boolean', null, true),
    prop('disabled', 'disabled', 'boolean', null, false),
    prop('placeholder', 'placeholder'),
    prop('status', 'status', 'select', [
      { label: 'default', value: '' },
      { label: 'success', value: 'success' },
      { label: 'warning', value: 'warning' },
      { label: 'error', value: 'error' },
    ]),
  ]
  const maps = {
    input: [
      group('NInput Props', [
        ...commonInputProps,
        prop('maxlength', 'maxlength', 'number', null, undefined, { min: 0 }),
        prop('showCount', 'show-count', 'boolean', null, false),
        prop('round', 'round', 'boolean', null, false),
        prop('readonly', 'readonly', 'boolean', null, false),
        prop('inputProps', 'input-props', 'json'),
      ]),
    ],
    textarea: [
      group('NInput Textarea Props', [
        ...commonInputProps,
        prop('rows', 'rows', 'number', null, 3, { min: 1, max: 20 }),
        prop('autosize', 'autosize', 'json'),
        prop('maxlength', 'maxlength', 'number', null, undefined, { min: 0 }),
        prop('showCount', 'show-count', 'boolean', null, false),
        prop('readonly', 'readonly', 'boolean', null, false),
      ]),
    ],
    number: [
      group('NInputNumber Props', [
        prop('size', 'size', 'select', componentSizeOptions.filter(item => item.value)),
        prop('min', 'min', 'number'),
        prop('max', 'max', 'number'),
        prop('step', 'step', 'number', null, 1),
        prop('precision', 'precision', 'number', null, undefined, { min: 0 }),
        prop('showButton', 'show-button', 'boolean', null, true),
        prop('clearable', 'clearable', 'boolean', null, true),
        prop('disabled', 'disabled', 'boolean', null, false),
        prop('placeholder', 'placeholder'),
      ]),
    ],
    money: [
      group('NInputNumber Props', [
        prop('size', 'size', 'select', componentSizeOptions.filter(item => item.value)),
        prop('min', 'min', 'number'),
        prop('max', 'max', 'number'),
        prop('step', 'step', 'number', null, 0.01),
        prop('precision', 'precision', 'number', null, 2, { min: 0 }),
        prop('showButton', 'show-button', 'boolean', null, true),
        prop('clearable', 'clearable', 'boolean', null, true),
        prop('placeholder', 'placeholder'),
      ]),
    ],
    select: [
      group('NSelect Props', [
        prop('size', 'size', 'select', componentSizeOptions.filter(item => item.value)),
        prop('multiple', 'multiple', 'boolean', null, false),
        prop('clearable', 'clearable', 'boolean', null, true),
        prop('filterable', 'filterable', 'boolean', null, true),
        prop('disabled', 'disabled', 'boolean', null, false),
        prop('remote', 'remote', 'boolean', null, false),
        prop('loading', 'loading', 'boolean', null, false),
        prop('maxTagCount', 'max-tag-count', 'number', null, undefined, { min: 1 }),
        prop('placement', 'placement', 'select', pickerPlacementOptions),
        prop('fallbackOption', 'fallback-option', 'boolean', null, true),
      ]),
    ],
    dictSelect: [
      group('DictSelect / NSelect Props', [
        prop('dictType', 'dict-type'),
        prop('size', 'size', 'select', componentSizeOptions.filter(item => item.value)),
        prop('multiple', 'multiple', 'boolean', null, false),
        prop('clearable', 'clearable', 'boolean', null, true),
        prop('filterable', 'filterable', 'boolean', null, true),
        prop('disabled', 'disabled', 'boolean', null, false),
      ]),
    ],
    radio: [
      group('RadioGroup Props', [
        prop('name', 'name'),
        prop('defaultValue', 'default-value'),
        prop('size', 'size', 'select', componentSizeOptions.filter(item => item.value)),
        prop('disabled', 'disabled', 'boolean', null, false),
      ]),
    ],
    radioButton: [
      group('RadioGroup Props', [
        prop('name', 'name'),
        prop('defaultValue', 'default-value'),
        prop('size', 'size', 'select', componentSizeOptions.filter(item => item.value)),
        prop('disabled', 'disabled', 'boolean', null, false),
      ]),
    ],
    checkbox: [
      group('CheckboxGroup Props', [
        prop('disabled', 'disabled', 'boolean', null, false),
        prop('defaultValue', 'default-value', 'json'),
        prop('value', 'value（受控，高级）', 'json'),
        prop('min', 'min', 'number', null, undefined, { min: 0 }),
        prop('max', 'max', 'number', null, undefined, { min: 0 }),
      ]),
    ],
    switch: [
      group('NSwitch Props', [
        prop('size', 'size', 'select', componentSizeOptions.filter(item => item.value)),
        prop('disabled', 'disabled', 'boolean', null, false),
        prop('round', 'round', 'boolean', null, true),
        prop('rubberBand', 'rubber-band', 'boolean', null, true),
        prop('checkedValue', 'checked-value', 'select', booleanValueOptions, true),
        prop('uncheckedValue', 'unchecked-value', 'select', booleanValueOptions, false),
        prop('checkedText', 'checked 文案'),
        prop('uncheckedText', 'unchecked 文案'),
      ]),
    ],
    slider: [
      group('NSlider Props', [
        prop('min', 'min', 'number', null, 0),
        prop('max', 'max', 'number', null, 100),
        prop('step', 'step', 'number', null, 1),
        prop('range', 'range', 'boolean', null, false),
        prop('vertical', 'vertical', 'boolean', null, false),
        prop('tooltip', 'tooltip', 'boolean', null, true),
        prop('marks', 'marks', 'json'),
      ]),
    ],
    rate: [
      group('NRate Props', [
        prop('count', 'count', 'number', null, 5, { min: 1, max: 10 }),
        prop('allowHalf', 'allow-half', 'boolean', null, false),
        prop('readonly', 'readonly', 'boolean', null, false),
        prop('clearable', 'clearable', 'boolean', null, false),
        prop('size', 'size', 'select', componentSizeOptions.filter(item => item.value)),
      ]),
    ],
    color: [
      group('NColorPicker Props', [
        prop('showAlpha', 'show-alpha', 'boolean', null, true),
        prop('modes', 'modes', 'multiSelect', [
          { label: 'hex', value: 'hex' },
          { label: 'rgb', value: 'rgb' },
          { label: 'hsl', value: 'hsl' },
          { label: 'hsv', value: 'hsv' },
        ]),
        prop('swatches', 'swatches', 'json'),
        prop('actions', 'actions', 'multiSelect', pickerActionOptions),
        prop('placement', 'placement', 'select', pickerPlacementOptions),
      ]),
    ],
    cascader: [
      group('NCascader Props', [
        prop('multiple', 'multiple', 'boolean', null, false),
        prop('cascade', 'cascade', 'boolean', null, true),
        prop('checkStrategy', 'check-strategy', 'select', [
          { label: 'all', value: 'all' },
          { label: 'parent', value: 'parent' },
          { label: 'child', value: 'child' },
        ]),
        prop('showPath', 'show-path', 'boolean', null, true),
        prop('filterable', 'filterable', 'boolean', null, false),
        prop('clearable', 'clearable', 'boolean', null, true),
        prop('placement', 'placement', 'select', pickerPlacementOptions),
      ]),
    ],
    treeSelect: [
      group('NTreeSelect Props', [
        prop('multiple', 'multiple', 'boolean', null, false),
        prop('cascade', 'cascade', 'boolean', null, true),
        prop('checkable', 'checkable', 'boolean', null, false),
        prop('filterable', 'filterable', 'boolean', null, false),
        prop('clearable', 'clearable', 'boolean', null, true),
        prop('showPath', 'show-path', 'boolean', null, true),
        prop('maxTagCount', 'max-tag-count', 'number', null, undefined, { min: 1 }),
      ]),
    ],
    card: [
      group('Card Props', [
        prop('size', 'size', 'select', cardSizeOptions, 'small'),
        prop('bordered', 'bordered', 'boolean', null, true),
        prop('embedded', 'embedded', 'boolean', null, false),
        prop('segmented', 'segmented', 'boolean', null, false),
        prop('hoverable', 'hoverable', 'boolean', null, false),
        prop('contentScrollable', 'content-scrollable', 'boolean', null, false),
        prop('role', 'role'),
        prop('headerStyle', 'header-style', 'json'),
        prop('contentStyle', 'content-style', 'json'),
        prop('footerStyle', 'footer-style', 'json'),
      ]),
    ],
    tabs: [
      group('Tabs Props', [
        prop('type', 'type', 'select', tabsTypeOptions, 'line'),
        prop('size', 'size', 'select', componentSizeOptions.filter(item => item.value), 'medium'),
        prop('placement', 'placement', 'select', tabsPlacementOptions, 'top'),
        prop('trigger', 'trigger', 'select', tabsTriggerOptions, 'click'),
        prop('animated', 'animated', 'boolean', null, true),
        prop('closable', 'closable', 'boolean', null, false),
        prop('addable', 'addable', 'boolean', null, false),
        prop('justifyContent', 'justify-content', 'select', [
          { label: 'start', value: 'start' },
          { label: 'center', value: 'center' },
          { label: 'end', value: 'end' },
          { label: 'space-between', value: 'space-between' },
          { label: 'space-around', value: 'space-around' },
          { label: 'space-evenly', value: 'space-evenly' },
        ]),
        prop('tabsPadding', 'tabs-padding', 'number', null, 0, { min: 0 }),
        prop('paneStyle', 'pane-style', 'json'),
        prop('tabStyle', 'tab-style', 'json'),
      ]),
    ],
    collapse: [
      group('Collapse Props', [
        prop('accordion', 'accordion', 'boolean', null, false),
        prop('arrowPlacement', 'arrow-placement', 'select', collapseArrowPlacementOptions, 'left'),
        prop('displayDirective', 'display-directive', 'select', collapseDisplayDirectiveOptions, 'if'),
        prop('triggerAreas', 'trigger-areas', 'multiSelect', collapseTriggerAreaOptions, ['main', 'arrow']),
        prop('defaultExpandedNames', 'default-expanded-names', 'json'),
        prop('expandedNames', 'expanded-names（受控，高级）', 'json'),
      ]),
    ],
    button: [
      group('Button Props', [
        prop('text', 'text'),
        prop('type', 'type', 'select', buttonTypeOptions, 'primary'),
        prop('size', 'size', 'select', componentSizeOptions.filter(item => item.value), 'medium'),
        prop('secondary', 'secondary', 'boolean', null, false),
        prop('tertiary', 'tertiary', 'boolean', null, false),
        prop('quaternary', 'quaternary', 'boolean', null, false),
        prop('dashed', 'dashed', 'boolean', null, false),
        prop('round', 'round', 'boolean', null, false),
        prop('block', 'block', 'boolean', null, false),
        prop('loading', 'loading', 'boolean', null, false),
        prop('disabled', 'disabled', 'boolean', null, false),
      ]),
    ],
  }
  if (isDateLikeComponent(key))
    return [group('DatePicker Props', datePickerPropFields())]
  if (isTimeLikeComponent(key))
    return [group('TimePicker Props', timePickerPropFields())]
  return maps[key] || []
}

function datePickerPropFields() {
  return [
    prop('type', 'type', 'select', datePickerTypeOptions),
    prop('size', 'size', 'select', componentSizeOptions.filter(item => item.value)),
    prop('format', 'format'),
    prop('valueFormat', 'value-format'),
    prop('placement', 'placement', 'select', pickerPlacementOptions, 'bottom-start'),
    prop('actions', 'actions', 'multiSelect', pickerActionOptions),
    prop('bordered', 'bordered', 'boolean', null, true),
    prop('clearable', 'clearable', 'boolean', null, true),
    prop('inputReadonly', 'input-readonly', 'boolean', null, false),
    prop('closeOnSelect', 'close-on-select', 'boolean', null, false),
    prop('updateValueOnClose', 'update-value-on-close', 'boolean', null, false),
    prop('defaultTime', 'default-time', 'json'),
    prop('separator', 'separator'),
    prop('startPlaceholder', 'start-placeholder'),
    prop('endPlaceholder', 'end-placeholder'),
    prop('firstDayOfWeek', 'first-day-of-week', 'number', null, undefined, { min: 0, max: 6 }),
  ]
}

function timePickerPropFields() {
  return [
    prop('size', 'size', 'select', componentSizeOptions.filter(item => item.value)),
    prop('format', 'format'),
    prop('valueFormat', 'value-format'),
    prop('placement', 'placement', 'select', pickerPlacementOptions, 'bottom-start'),
    prop('actions', 'actions', 'multiSelect', pickerActionOptions),
    prop('bordered', 'bordered', 'boolean', null, true),
    prop('clearable', 'clearable', 'boolean', null, true),
    prop('inputReadonly', 'input-readonly', 'boolean', null, false),
    prop('showIcon', 'show-icon', 'boolean', null, true),
    prop('hours', 'hours', 'json'),
    prop('minutes', 'minutes', 'json'),
    prop('seconds', 'seconds', 'json'),
  ]
}

function isDateLikeComponent(key = '') {
  return ['date', 'datetime', 'daterange', 'datetimerange', 'month', 'year', 'quarter'].includes(key)
}

function isTimeLikeComponent(key = '') {
  return ['time', 'timerange'].includes(key)
}

function group(title, fields = []) {
  const zh = groupChineseLabels[title]
  return { title: zh ? `${zh} ${title}` : title, fields }
}

function prop(key, label, type = 'text', options = null, defaultValue = undefined, extra = {}) {
  return { key, label: formatPropLabel(key, label), type, options, defaultValue, ...extra }
}

function formatPropLabel(key = '', label = '') {
  const zh = propChineseLabels[key] || propChineseLabels[label] || ''
  const propName = label || key
  if (!zh)
    return propName
  if (propName.includes('（'))
    return `${zh}（${propName}）`
  return `${zh}（${propName}）`
}

function resolvePxNumber(value, fallback = 0) {
  const match = String(value ?? '').match(/-?\d+(?:\.\d+)?/)
  if (!match)
    return fallback
  return Number(match[0])
}

function normalizePositionNumber(value) {
  const next = Number(value)
  return Number.isFinite(next) ? next : 0
}

function parseTranslateStyle(value = '') {
  const text = String(value || '')
  const translateMatch = text.match(/translate(?:3d)?\(\s*(-?\d+(?:\.\d+)?)px(?:\s*,\s*(-?\d+(?:\.\d+)?)px)?/)
  if (translateMatch) {
    return {
      x: Number(translateMatch[1]) || 0,
      y: Number(translateMatch[2]) || 0,
    }
  }
  const xMatch = text.match(/translateX\(\s*(-?\d+(?:\.\d+)?)px\)/)
  const yMatch = text.match(/translateY\(\s*(-?\d+(?:\.\d+)?)px\)/)
  return {
    x: xMatch ? Number(xMatch[1]) || 0 : 0,
    y: yMatch ? Number(yMatch[1]) || 0 : 0,
  }
}

function formatTranslateStyle(position = {}) {
  const x = normalizePositionNumber(position.x)
  const y = normalizePositionNumber(position.y)
  if (!x && !y)
    return undefined
  return `translate(${x}px, ${y}px)`
}

function parseStyleText(value = '') {
  return String(value || '')
    .split(';')
    .map(item => item.trim())
    .filter(Boolean)
    .reduce((style, item) => {
      const [rawKey, ...rawValue] = item.split(':')
      const key = rawKey?.trim()
      const nextValue = rawValue.join(':').trim()
      if (!key || !nextValue)
        return style
      style[toCamelCase(key)] = nextValue
      return style
    }, {})
}

function stringifyStyle(style = {}) {
  return Object.entries(style || {})
    .filter(([, value]) => value !== undefined && value !== null && value !== '')
    .map(([key, value]) => `${toKebabCase(key)}: ${value}`)
    .join('; ')
}

function applyStyleValue(source = {}, key, value) {
  const next = { ...(source || {}) }
  if (value === undefined || value === null || value === '')
    delete next[key]
  else
    next[key] = value
  return next
}

function valueToPx(value) {
  if (value === undefined || value === null || value === '')
    return undefined
  const number = Number(value)
  return Number.isFinite(number) ? `${number}px` : undefined
}

function toCamelCase(value = '') {
  return String(value).replace(/-([a-z])/g, (_, letter) => letter.toUpperCase())
}

function toKebabCase(value = '') {
  return String(value).replace(/[A-Z]/g, letter => `-${letter.toLowerCase()}`)
}

function handleOpenSourcePanel() {
  sourceError.value = ''
  sourceModalVisible.value = true
}

onMounted(() => {
  window.addEventListener('forge-form-designer:open-source-panel', handleOpenSourcePanel)
  loadCodeRuleOptions()
})

onBeforeUnmount(() => {
  window.removeEventListener('forge-form-designer:open-source-panel', handleOpenSourcePanel)
})
</script>

<style scoped>
.forge-property-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  background: #fafafa;
}

.edit-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  height: 48px;
  padding: 0 10px 0 12px;
  border-bottom: 1px solid #e4e4e7;
  background: #fafafa;
}

.edit-panel-title {
  min-width: 0;
}

.edit-panel-title strong,
.edit-panel-title span {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.edit-panel-title strong {
  color: #18181b;
  font-size: 13px;
  font-weight: 600;
  line-height: 20px;
}

.edit-panel-title span {
  color: #71717a;
  font-size: 11px;
  line-height: 16px;
}

.edit-panel-tools {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

.panel-close-button {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  cursor: pointer;
  border: 1px solid #e4e4e7;
  border-radius: 6px;
  background: #fff;
  color: #52525b;
  font-size: 14px;
}

.panel-close-button:hover {
  border-color: #c7d2fe;
  background: #f4f6ff;
  color: #3153d8;
}

.property-search-box {
  display: grid;
  gap: 6px;
  padding: 8px 10px;
  border-bottom: 1px solid #e4e4e7;
  background: #fafafa;
}

.property-search-box :deep(.n-input) {
  --n-border: 1px solid #e4e4e7 !important;
  --n-border-hover: 1px solid #c7d2fe !important;
  --n-border-focus: 1px solid #4266f7 !important;
  --n-box-shadow-focus: 0 0 0 2px rgba(66, 102, 247, 0.12) !important;
  border-radius: 7px;
}

.property-search-hit {
  width: fit-content;
  max-width: 100%;
  padding: 3px 8px;
  overflow: hidden;
  border: 1px solid #bfdbfe;
  border-radius: 999px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 12px;
  line-height: 18px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.property-tabs {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.form-property-tabs {
  margin-top: 0;
}

.property-tabs :deep(.n-tabs-nav) {
  flex: 0 0 auto;
  padding: 6px 8px;
  border-bottom: 1px solid #e4e4e7;
  background: #fafafa;
}

.property-tabs :deep(.n-tabs-tab) {
  flex: 0 0 auto;
  justify-content: center;
  min-width: 68px;
  min-height: 28px;
  margin-bottom: 0;
  padding: 0 7px;
  border-radius: 7px;
  color: #71717a;
  font-size: 12px;
  font-weight: 600;
}

.property-tabs :deep(.n-tabs-nav-scroll-content) {
  display: flex;
  gap: 3px;
  padding: 3px;
  border: 1px solid #e4e4e7;
  border-radius: 9px;
  background: #f4f4f5;
}

.property-tabs :deep(.n-tabs-scroll-padding) {
  display: none;
}

.property-tab-label {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  line-height: 1;
  white-space: nowrap;
}

.property-tab-label .n-icon {
  color: #71717a;
  font-size: 14px;
}

.property-tabs :deep(.n-tabs-tab--active .property-tab-label .n-icon) {
  color: #27272a;
}

.property-tabs :deep(.n-tabs-tab:hover) {
  background: rgba(228, 228, 231, 0.72);
  color: #27272a;
}

.property-tabs :deep(.n-tabs-tab--active) {
  background: #fff;
  color: #27272a;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.08);
  font-weight: 600;
}

.property-tabs :deep(.n-tabs-bar) {
  display: none;
}

.property-tabs :deep(.n-tabs-pane-wrapper) {
  min-height: 0;
  overflow: auto;
  background: #fafafa;
}

.property-form {
  min-height: 0;
  overflow: auto;
  padding: 8px 8px 42px;
  background: #fafafa;
}

.property-form :deep(.n-collapse),
.property-form > .panel-item {
  display: grid;
  gap: 6px;
}

.panel-item {
  margin-bottom: 12px;
}

.form-property-collapse,
.config-collapse {
  display: grid;
  gap: 6px;
}

.form-property-collapse :deep(.n-collapse-item),
.config-collapse :deep(.n-collapse-item) {
  overflow: hidden;
  border: 1px solid rgba(228, 228, 231, 0.72);
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
}

.form-property-collapse :deep(.n-collapse-item__header:hover),
.config-collapse :deep(.n-collapse-item__header:hover) {
  background: #fafafa;
}

.form-property-collapse :deep(.n-collapse-item__header),
.config-collapse :deep(.n-collapse-item__header) {
  min-height: 35px;
  padding: 0 12px;
  border-bottom: 0;
}

.form-property-collapse :deep(.n-collapse-item__header-main),
.config-collapse :deep(.n-collapse-item__header-main) {
  color: #3f3f46;
  font-size: 12px;
  font-weight: 650;
}

.form-property-collapse :deep(.n-collapse-item__content-inner),
.config-collapse :deep(.n-collapse-item__content-inner) {
  padding: 2px 10px 10px;
}

.form-property-collapse :deep(.n-collapse-item__content-inner > .panel-item),
.config-collapse :deep(.n-collapse-item__content-inner > .panel-item) {
  margin-bottom: 0;
  border: 0;
  border-radius: 0;
  padding: 0;
}

.form-property-collapse :deep(.n-collapse-item__content-inner > .panel-item > .panel-item-title),
.config-collapse :deep(.n-collapse-item__content-inner > .panel-item > .panel-item-title) {
  display: none;
}

.property-form > .panel-item {
  border: 1px solid rgba(228, 228, 231, 0.72);
  border-radius: 8px;
  background: #fff;
  padding: 10px;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
}

.panel-item-strong {
  border: 1px solid rgba(228, 228, 231, 0.72);
  border-radius: 8px;
  background: #fff;
  padding: 12px;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
}

.more-config-button {
  --n-color: #fff !important;
  --n-color-hover: #f4f4f5 !important;
  --n-color-pressed: #e4e4e7 !important;
  --n-color-focus: #fff !important;
  --n-border: 1px solid #e4e4e7 !important;
  --n-border-hover: 1px solid #c7d2fe !important;
  --n-border-pressed: 1px solid #a5b4fc !important;
  --n-border-focus: 1px solid #c7d2fe !important;
  --n-text-color: #4f46e5 !important;
  --n-text-color-hover: #4338ca !important;
  --n-text-color-pressed: #3730a3 !important;
  --n-text-color-focus: #4f46e5 !important;
  height: 28px;
  justify-content: center;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
  font-weight: 600;
}

.more-config-button :deep(.n-button__content) {
  justify-content: center;
  width: 100%;
}

.button-icon {
  display: inline-grid;
  place-items: center;
  width: 20px;
  height: 20px;
  border-radius: 6px;
  background: #2563eb;
  color: #fff;
  font-size: 13px;
}

.more-config-button:hover .button-icon {
  background: #1d4ed8;
}

.form-api-panel {
  display: grid;
  gap: 12px;
}

.form-api-field {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.form-api-label {
  color: #52525b;
  font-size: 11px;
  font-weight: 600;
  line-height: 16px;
}

.form-api-base-row {
  display: flex;
  gap: 8px;
  min-width: 0;
}

.form-api-base-row :deep(.n-input:first-child) {
  flex: 2 1 0;
}

.form-api-base-row :deep(.n-input:last-child) {
  flex: 1 1 72px;
}

.form-api-endpoint-list {
  display: grid;
  gap: 6px;
}

.form-api-endpoint-row {
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

.form-api-endpoint-row:focus-within {
  border-color: #6366f1;
  box-shadow: 0 0 0 2px rgba(99, 102, 241, 0.1);
}

.form-api-method-badge {
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

.form-api-method-badge.get {
  background: #eff6ff;
  color: #2563eb;
}

.form-api-method-badge.post {
  background: #ecfdf5;
  color: #059669;
}

.form-api-method-badge.put {
  background: #fffbeb;
  color: #d97706;
}

.form-api-method-badge.delete {
  background: #fff1f2;
  color: #e11d48;
}

.form-api-endpoint-row :deep(.n-input) {
  --n-border: 0 !important;
  --n-border-hover: 0 !important;
  --n-border-focus: 0 !important;
  --n-box-shadow-focus: none !important;
  flex: 1 1 0;
  min-width: 0;
  width: 100%;
  border-radius: 0;
}

.form-api-endpoint-row :deep(.n-input__input-el) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 11px !important;
}

.option-add-button {
  --n-color: #f0fdf4 !important;
  --n-color-hover: #dcfce7 !important;
  --n-color-pressed: #bbf7d0 !important;
  --n-border: 1px dashed #86efac !important;
  --n-border-hover: 1px dashed #4ade80 !important;
  --n-border-pressed: 1px dashed #22c55e !important;
  --n-text-color: #15803d !important;
  --n-text-color-hover: #166534 !important;
  --n-text-color-pressed: #14532d !important;
  height: 34px;
  font-weight: 700;
}

.option-add-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  border-radius: 999px;
  background: #22c55e;
  color: #fff;
  font-size: 13px;
  font-weight: 800;
  line-height: 1;
}

.validation-panel {
  display: grid;
  gap: 10px;
}

.field-constraint-config {
  display: grid;
  width: 100%;
  gap: 8px;
  border: 1px solid #dbeafe;
  border-radius: 7px;
  background: #f8fbff;
  padding: 10px;
}

.switch-line.compact {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 8px 10px;
  border: 1px solid #e4e4e7;
  border-radius: 7px;
  background: #fafafa;
  color: #3f3f46;
  font-size: 12px;
  font-weight: 600;
}

.auto-code-config {
  display: grid;
  width: 100%;
  gap: 8px;
}

.formula-config-entry {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  gap: 10px;
  border: 1px solid #e4e4e7;
  border-radius: 7px;
  background: #fafafa;
  padding: 9px 10px;
}

.formula-config-entry > div {
  display: grid;
  min-width: 0;
  gap: 3px;
}

.formula-config-entry strong {
  color: #18181b;
  font-size: 12px;
  font-weight: 700;
}

.formula-config-entry span {
  overflow: hidden;
  color: #71717a;
  font-size: 12px;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.field-formula-property-panel {
  height: min(760px, calc(100vh - 136px));
  border: 0;
  border-radius: 0;
}

.auto-code-rule-summary {
  display: grid;
  gap: 4px;
  border: 1px solid #dbeafe;
  border-radius: 7px;
  background: #eff6ff;
  padding: 8px 10px;
}

.auto-code-rule-summary span {
  color: #1e3a8a;
  font-size: 12px;
  font-weight: 700;
}

.auto-code-rule-summary code {
  overflow: hidden;
  color: #0f172a;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.auto-code-rule-summary small {
  color: #64748b;
  font-size: 11px;
}

.auto-code-preview-row {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 8px;
  align-items: center;
}

.auto-code-preview-row strong {
  overflow: hidden;
  color: #111827;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.auto-code-preview-row strong.invalid,
.auto-code-issue.error {
  color: #dc2626;
}

.auto-code-issue {
  font-size: 12px;
  line-height: 1.5;
}

.auto-code-issue.warning {
  color: #b45309;
}

.linkage-action-list {
  display: grid;
  gap: 6px;
  margin-bottom: 10px;
}

.appearance-control {
  display: grid;
  gap: 14px;
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

.appearance-radius-shell {
  position: relative;
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

.linkage-action-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  width: 100%;
  cursor: pointer;
  border: 1px solid #e4e4e7;
  border-radius: 6px;
  background: #fafafa;
  padding: 7px 9px;
  color: #3f3f46;
  font-size: 11px;
  font-weight: 600;
  text-align: left;
  transition:
    border-color 160ms ease,
    background-color 160ms ease,
    color 160ms ease;
}

.linkage-action-row:hover {
  border-color: #c7d2fe;
  background: #fff;
  color: #4f46e5;
}

.linkage-action-row strong {
  color: #4f46e5;
  font-size: 10px;
  font-weight: 600;
  opacity: 0;
  transition: opacity 160ms ease;
}

.linkage-action-row:hover strong {
  opacity: 1;
}

.panel-item-title {
  display: flex;
  align-items: center;
  min-height: 20px;
  margin-bottom: 8px;
  color: #0f172a;
  font-size: 13px;
  font-weight: 700;
  line-height: 20px;
}

.panel-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.panel-title-row > :first-child {
  min-width: 0;
}

.panel-title-row > .n-button,
.panel-title-row > .source-actions {
  flex-shrink: 0;
}

.panel-title-row .panel-item-title {
  margin-bottom: 0;
}

.panel-item-desc {
  margin: 3px 0 0;
  color: #8f959e;
  font-size: 11px;
  line-height: 16px;
}

.panel-item :deep(.n-form-item) {
  margin-bottom: 10px;
  border: 0;
  border-radius: 0;
  background: transparent;
  padding: 0;
  box-shadow: none;
}

.panel-item :deep(.n-form-item:last-child) {
  margin-bottom: 0;
}

.panel-item :deep(.n-form-item-label) {
  position: relative;
  display: flex !important;
  align-items: center;
  align-self: flex-start;
  min-height: 18px;
  height: auto;
  padding-top: 0;
  padding-bottom: 5px;
  padding-left: 0;
  color: #52525b;
  font-size: 11px;
  font-weight: 600;
  line-height: 16px;
}

.panel-item :deep(.n-form-item-label::before) {
  display: none;
}

.panel-item :deep(.n-form-item-label--right-mark) {
  display: flex !important;
  align-items: center !important;
}

.panel-item :deep(.n-form-item-label__text) {
  display: inline-flex;
  align-items: center;
  min-height: 20px;
  line-height: 20px;
  font-size: 11px;
}

.crud-expand-config-panel {
  display: grid;
  gap: 12px;
  min-width: 0;
  margin-bottom: 10px;
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
  margin-bottom: 10px;
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

.bitable-field-panel-list {
  display: grid;
  gap: 1px;
  max-height: 386px;
  min-width: 0;
  overflow-y: auto;
  padding: 6px;
}

.crud-expand-field-list {
  display: grid;
  gap: 6px;
  max-height: 240px;
  overflow-y: auto;
  padding-right: 2px;
}

.crud-bitable-field-row {
  display: grid;
  grid-template-columns: 20px 18px minmax(0, 1fr) 24px;
  align-items: center;
  gap: 5px;
  min-height: 32px;
  min-width: 0;
  border-radius: 5px;
  background: #f8f9fa;
  color: #27272a;
  padding: 4px;
}

.crud-bitable-field-row:hover {
  background: #eef1f4;
}

.crud-bitable-field-row.invisible {
  visibility: visible !important;
  background: #f8f9fa;
  color: #a1a1aa;
}

.crud-bitable-field-row.invisible .crud-bitable-field-name span,
.crud-bitable-field-row.invisible .crud-bitable-field-icon,
.crud-bitable-field-row.invisible .crud-bitable-field-name small {
  color: #c4c4cc;
}

.crud-bitable-field-row.invisible:hover {
  background: #eef1f4;
}

.crud-bitable-field-drag,
.crud-bitable-field-visible {
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
  line-height: 1;
}

.crud-bitable-field-drag {
  cursor: grab;
}

.crud-bitable-field-drag:hover,
.crud-bitable-field-visible:hover {
  color: #2563eb;
}

.crud-bitable-field-icon {
  color: #71717a;
  font-size: 14px;
}

.crud-bitable-field-name {
  display: grid;
  min-width: 0;
}

.crud-bitable-field-name span,
.crud-bitable-field-name small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.crud-bitable-field-name span {
  color: inherit;
  font-size: 12px;
  font-weight: 500;
  line-height: 17px;
}

.crud-bitable-field-name small {
  color: #a1a1aa;
  font-size: 10px;
  line-height: 13px;
}

.custom-action-empty {
  padding: 4px 2px 2px;
  color: #94a3b8;
  font-size: 11px;
  line-height: 16px;
}

@media (max-width: 1280px) {
  .crud-expand-config-grid {
    grid-template-columns: 1fr;
  }
}

.panel-item :deep(.n-radio-group),
.panel-item :deep(.n-input-number),
.panel-item :deep(.n-select) {
  width: 100%;
}

.property-form :deep(.n-input),
.property-form :deep(.n-input-number),
.property-form :deep(.n-base-selection) {
  --n-color: #f4f4f5 !important;
  --n-color-focus: #fff !important;
  --n-color-hover: #f4f4f5 !important;
  --n-border: 1px solid transparent !important;
  --n-border-hover: 1px solid #a5b4fc !important;
  --n-border-focus: 1px solid #6366f1 !important;
  --n-box-shadow-focus: 0 0 0 3px rgba(99, 102, 241, 0.1) !important;
  border-radius: 6px;
}

.property-form :deep(.n-input__input-el),
.property-form :deep(.n-input__textarea-el),
.property-form :deep(.n-base-selection-label) {
  color: #27272a !important;
  font-size: 12px !important;
}

.property-form :deep(.n-button) {
  --n-border-radius: 6px !important;
  font-size: 12px;
}

.drawer-property-form {
  padding: 2px 2px 18px;
}

.color-control {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.slider-control {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 96px;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 0 2px 8px;
}

.form-item-config,
.form-permission-panel,
.form-lifecycle-panel {
  display: grid;
  gap: 14px;
}

.compact-config-row {
  display: grid;
  grid-template-columns: minmax(86px, 1fr) 124px;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.compact-config-row > label,
.form-columns-head > label,
.compact-field > label {
  color: #52525b;
  font-size: 11px;
  font-weight: 600;
  line-height: 16px;
}

.form-config-segment {
  width: 124px;
}

.form-config-segment.three,
.form-config-segment:has(button:nth-child(3)) {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.form-columns-control {
  display: grid;
  gap: 8px;
  padding-top: 2px;
}

.form-columns-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.form-columns-head :deep(.n-input-number) {
  width: 52px;
}

.form-columns-control .slider-control {
  display: block;
  padding: 0 4px 2px;
}

.label-width-control {
  position: relative;
  min-width: 0;
}

.label-width-control :deep(.n-input__input-el) {
  padding-right: 24px !important;
  text-align: right;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

.label-width-control em {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  color: #a1a1aa;
  font-size: 10px;
  font-style: normal;
  pointer-events: none;
}

.compact-field {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.grid-quick-config {
  border: 0;
  background: transparent;
}

.spacing-editor {
  border: 1px solid #eff0f1;
  border-radius: 6px;
  background: #fff;
  padding: 10px;
}

.spacing-editor + .spacing-editor {
  margin-top: 10px;
}

.spacing-editor-title {
  margin-bottom: 8px;
  color: #646a73;
  font-size: 12px;
  font-weight: 600;
}

.spacing-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.spacing-grid label {
  display: grid;
  grid-template-columns: 20px minmax(86px, 1fr);
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.spacing-grid span {
  color: #646a73;
  font-size: 12px;
}

.grid-column-span-editor {
  display: grid;
  gap: 8px;
}

.grid-column-span-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 88px;
  gap: 8px;
  align-items: center;
  padding: 8px 10px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #f8fafc;
}

.grid-column-span-row span {
  color: #475569;
  font-size: 12px;
  font-weight: 600;
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

.segmented-mini {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 2px;
  padding: 2px;
  border: 1px solid rgba(228, 228, 231, 0.72);
  border-radius: 6px;
  background: rgba(244, 244, 245, 0.8);
}

.segmented-mini.three {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.segmented-mini button {
  min-width: 0;
  height: 25px;
  cursor: pointer;
  border: 1px solid transparent;
  border-radius: 4px;
  background: transparent;
  color: #71717a;
  font-size: 11px;
  font-weight: 600;
}

.segmented-mini button:hover {
  color: #3f3f46;
  background: rgba(228, 228, 231, 0.5);
}

.segmented-mini button.active {
  border-color: rgba(212, 212, 216, 0.72);
  background: #fff;
  color: #4f46e5;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.08);
}

.form-asset-panel {
  background: #fff;
}

.form-asset-list {
  display: grid;
  gap: 6px;
}

.form-asset-list-title {
  color: #71717a;
  font-size: 11px;
  font-weight: 600;
  line-height: 16px;
}

.form-default-badge {
  display: inline-flex;
  align-items: center;
  height: 26px;
  border: 1px solid #bbf7d0;
  border-radius: 6px;
  background: #f0fdf4;
  color: #16a34a;
  font-size: 11px;
  font-weight: 700;
  padding: 0 8px;
}

.form-asset-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

.form-asset-icon-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  width: 20px;
  height: 20px;
  cursor: pointer;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: #a1a1aa;
  font-size: 13px;
  font-weight: 600;
  padding: 0;
  transition:
    background-color 160ms ease,
    color 160ms ease;
}

.form-asset-icon-button:hover {
  background: #eef2ff;
  color: #4f46e5;
}

.form-asset-tabs {
  display: flex;
  gap: 6px;
  margin-bottom: 10px;
  overflow-x: auto;
  padding-bottom: 3px;
}

.form-asset-tab {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  max-width: 168px;
  flex: 0 0 auto;
  height: 32px;
  cursor: pointer;
  border: 1px solid #e5e7eb;
  border-radius: 7px;
  background: #fff;
  color: #475569;
  padding: 0 9px;
}

.form-asset-tab:hover {
  border-color: #bfdbfe;
  background: #f8fafc;
}

.form-asset-tab.active {
  border-color: #2563eb;
  background: #eff6ff;
  color: #1d4ed8;
}

.form-asset-tab em {
  display: inline-grid;
  place-items: center;
  width: 18px;
  height: 18px;
  flex: 0 0 auto;
  border-radius: 50%;
  background: #e2e8f0;
  color: #475569;
  font-style: normal;
  font-size: 11px;
  font-weight: 700;
}

.form-asset-tab.active em {
  background: #2563eb;
  color: #fff;
}

.form-asset-tab span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
  font-weight: 600;
}

.form-asset-tab strong {
  flex: 0 0 auto;
  border-radius: 999px;
  background: #dbeafe;
  color: #1d4ed8;
  font-size: 10px;
  line-height: 16px;
  padding: 0 5px;
}

.current-form-banner {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  border: 1px solid #bfdbfe;
  border-radius: 8px;
  background: #eff6ff;
  color: #1d4ed8;
  padding: 9px 10px;
  animation: current-form-pulse 420ms ease;
}

.current-form-banner span {
  border-radius: 999px;
  background: #2563eb;
  color: #fff;
  font-size: 11px;
  line-height: 18px;
  padding: 0 7px;
}

.current-form-banner strong {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
}

.form-asset-edit-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 8px;
  margin-bottom: 10px;
}

.form-asset-edit-grid :deep(.n-form-item) {
  margin-bottom: 0;
}

.form-asset-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
  min-width: 0;
  border: 1px solid #e5e7eb;
  border-radius: 7px;
  background: #fff;
  color: #1f2329;
  padding: 9px 10px;
  text-align: left;
}

button.form-asset-card,
.form-asset-main {
  cursor: pointer;
}

.form-asset-card.active {
  border-color: #93c5fd;
  background: #eff6ff;
}

.form-asset-main {
  min-width: 0;
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  align-items: center;
  gap: 6px;
}

.form-asset-card strong,
.form-asset-card span,
.form-asset-main strong,
.form-asset-main span {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.form-asset-card strong,
.form-asset-main strong {
  font-size: 12px;
  font-weight: 700;
}

.form-asset-card span,
.form-asset-main span {
  margin-top: 2px;
  color: #8f959e;
  font-size: 11px;
  line-height: 16px;
}

@keyframes current-form-pulse {
  0% {
    transform: translateY(-2px);
    box-shadow: 0 0 0 0 rgba(37, 99, 235, 0.24);
  }
  100% {
    transform: translateY(0);
    box-shadow: 0 0 0 8px rgba(37, 99, 235, 0);
  }
}

.layout-child-manager {
  display: grid;
  gap: 8px;
  margin-top: 12px;
  border-top: 1px solid #eff0f1;
  padding-top: 12px;
}

.layout-child-card,
.interaction-rule-card {
  border: 1px solid rgba(228, 228, 231, 0.82);
  border-radius: 8px;
  background: #fff;
  padding: 10px;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
}

.layout-child-card {
  display: grid;
  gap: 8px;
}

.layout-child-card-main {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(0, 1fr);
  gap: 8px;
}

.layout-child-actions,
.source-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
}

.interaction-rule-list {
  display: grid;
  gap: 10px;
}

.interaction-presets {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 8px;
  margin-bottom: 12px;
}

.interaction-preset-card {
  display: grid;
  gap: 4px;
  min-height: 64px;
  cursor: pointer;
  border: 1px solid #e4e4e7;
  border-radius: 8px;
  background: #fafafa;
  color: #27272a;
  padding: 10px;
  text-align: left;
  transition:
    border-color 160ms ease,
    box-shadow 160ms ease,
    transform 160ms ease;
}

.interaction-preset-card:hover {
  border-color: #c7d2fe;
  box-shadow: 0 6px 14px rgba(99, 102, 241, 0.1);
  transform: translateY(-1px);
}

.interaction-preset-card strong {
  color: #27272a;
  font-size: 12px;
  font-weight: 650;
}

.interaction-preset-card span {
  color: #71717a;
  font-size: 11px;
  line-height: 16px;
}

.interaction-rule-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 10px;
  cursor: pointer;
  list-style: none;
}

.interaction-rule-head::-webkit-details-marker {
  display: none;
}

.interaction-rule-head::before {
  content: '';
  width: 0;
  height: 0;
  border-top: 4px solid transparent;
  border-bottom: 4px solid transparent;
  border-left: 5px solid #64748b;
  transform: rotate(90deg);
  transition: transform 160ms ease;
}

.interaction-rule-card:not([open]) .interaction-rule-head {
  margin-bottom: 0;
}

.interaction-rule-card:not([open]) .interaction-rule-head::before {
  transform: rotate(0deg);
}

.interaction-rule-head > div {
  flex: 1;
  min-width: 0;
}

.interaction-rule-head strong {
  display: block;
  color: #27272a;
  font-size: 12px;
  font-weight: 650;
}

.interaction-rule-head span {
  display: block;
  margin-top: 2px;
  color: #71717a;
  font-size: 11px;
  line-height: 16px;
}

.interaction-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 10px;
}

.interaction-grid :deep(.n-form-item) {
  margin-bottom: 0;
}

.interaction-rule-card :deep(.n-form-item-blank) {
  min-width: 0;
}

.interaction-rule-card :deep(.n-select),
.interaction-rule-card :deep(.n-input),
.interaction-rule-card :deep(.n-input-wrapper) {
  width: 100%;
}

.field-help {
  margin-top: 4px;
  color: #8f959e;
  font-size: 11px;
  line-height: 16px;
}

.field-label-with-help {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  min-width: 0;
}

.help-icon {
  display: inline-grid;
  place-items: center;
  width: 15px;
  height: 15px;
  cursor: help;
  border-radius: 50%;
  background: #eef2ff;
  color: #2563eb;
  font-size: 11px;
  font-weight: 700;
  line-height: 1;
}

.help-icon:hover {
  background: #dbeafe;
}

.empty-config-box {
  border: 1px dashed #d4d4d8;
  border-radius: 8px;
  background: #fafafa;
  color: #71717a;
  font-size: 12px;
  line-height: 18px;
  padding: 18px 12px;
  text-align: center;
}

.source-editor {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
}

.source-editor :deep(.n-input) {
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

.source-panel {
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  background: #fff;
  padding: 12px;
}

.source-path {
  margin-top: 2px;
  color: #8f959e;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 11px;
  line-height: 16px;
}

.source-editor :deep(textarea) {
  background: #050816;
  caret-color: #f8fafc;
  color: #f8fafc;
  font-family: inherit;
  font-size: 12px;
  line-height: 1.55;
}

.source-editor :deep(textarea::selection) {
  background: rgba(37, 99, 235, 0.48);
}

.source-editor-hint {
  margin: 10px 0;
  border: 1px solid rgba(59, 130, 246, 0.28);
  border-radius: 6px;
  background: rgba(37, 99, 235, 0.12);
  color: #bfdbfe;
  font-size: 12px;
  line-height: 18px;
  padding: 7px 10px;
}

.source-error {
  margin-top: 8px;
  border: 1px solid #fecaca;
  border-radius: 6px;
  background: #fef2f2;
  color: #b91c1c;
  font-size: 12px;
  line-height: 18px;
  padding: 8px 10px;
}

.form-source-modal {
  width: min(860px, calc(100vw - 48px));
}

.form-source-modal :deep(.n-card) {
  background: #09090b;
  color: #f8fafc;
}

.form-source-modal :deep(.n-card-header),
.form-source-modal :deep(.n-card__footer) {
  border-color: #1f2937;
}

.form-source-modal :deep(.n-card-header__main) {
  color: #f8fafc;
}

.form-source-modal .source-panel {
  margin: 0;
  border-color: #1f2937;
  background: #09090b;
}

.form-source-modal .panel-item-title {
  color: #f8fafc;
}

.form-source-modal .source-path {
  color: #94a3b8;
}

.source-modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.crud-field-config-list {
  display: grid;
  gap: 10px;
}

.crud-field-config-card {
  border: 1px solid rgba(228, 228, 231, 0.82);
  border-radius: 8px;
  background: #fff;
  padding: 10px;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
}

.crud-field-card-head {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: start;
  gap: 10px;
  margin-bottom: 10px;
}

.crud-field-name {
  min-width: 0;
  cursor: pointer;
  border: 0;
  background: transparent;
  color: #1f2329;
  text-align: left;
  padding: 0;
}

.crud-field-name strong,
.crud-field-name small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.crud-field-name strong {
  font-size: 12px;
  font-weight: 650;
  line-height: 18px;
}

.crud-field-name small {
  color: #71717a;
  font-size: 11px;
  line-height: 16px;
}

.crud-field-name:hover strong {
  color: #2563eb;
}

.crud-role-switches,
.crud-compact-switches {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.crud-role-switches label,
.crud-compact-switches label {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: #52525b;
  font-size: 12px;
  line-height: 22px;
}

.crud-inline-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.crud-inline-grid :deep(.n-form-item) {
  margin-bottom: 0;
}

.crud-compact-switches {
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
  border-top: 1px solid #f4f4f5;
  padding-top: 8px;
}

.crud-field-empty {
  border: 1px dashed #d4d4d8;
  border-radius: 8px;
  background: #fafafa;
  color: #71717a;
  font-size: 12px;
  line-height: 18px;
  padding: 18px 12px;
  text-align: center;
}

.option-list {
  display: grid;
  gap: 8px;
  margin-bottom: 10px;
}

.option-editor-row {
  display: grid;
  grid-template-columns: minmax(72px, 1fr) minmax(64px, 0.8fr) 42px 44px;
  align-items: center;
  gap: 6px;
  border: 1px solid #eff0f1;
  border-radius: 6px;
  background: #fff;
  padding: 7px;
}

.option-editor-row.two-columns {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.option-editor-row.three-columns {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.option-editor-row.four-columns {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.page-widget-config-stack {
  display: grid;
  gap: 8px;
  min-width: 0;
}

.property-help {
  padding: 7px 9px;
  border: 1px dashed #d4d4d8;
  border-radius: 6px;
  background: #fff;
  color: #71717a;
  font-size: 11px;
  line-height: 1.55;
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

.data-source-inline-row {
  border: 0;
  background: transparent;
  padding: 0;
}

.data-source-mapping-grid {
  display: grid;
  gap: 8px;
  min-width: 0;
}

.data-source-mapping-grid .data-source-row {
  grid-template-columns: minmax(64px, 74px) minmax(0, 1fr);
}

.option-editor-row > .n-switch:nth-last-child(-n + 2) {
  justify-self: center;
}

.option-props-input {
  grid-column: 1 / -1;
}

.crud-readonly-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
  padding: 8px 12px;
  background: var(--n-color-hover, #f7f8fa);
  border-radius: 6px;
  font-size: 12px;
  color: var(--n-text-color-3);
  margin-bottom: 12px;
}

.panel-item-hint {
  font-weight: normal;
  font-size: 11px;
  color: var(--n-text-color-3);
  margin-left: 8px;
}

.crud-field-config-title {
  margin: 14px 0 8px;
  border-top: 1px solid #eff0f1;
  padding-top: 12px;
  color: #1f2329;
  font-size: 12px;
  font-weight: 700;
}

.crud-field-config-title:first-child {
  margin-top: 0;
  border-top: 0;
  padding-top: 0;
}

.switch-list {
  display: grid;
  gap: 8px;
}

.switch-list.compact {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin: 10px 0 14px;
}

.form-rule-list {
  display: grid;
  gap: 8px;
  margin-bottom: 14px;
}

.field-permission-rules {
  display: grid;
  gap: 8px;
}

.field-permission-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  color: #52525b;
  font-size: 11px;
  font-weight: 700;
}

.field-permission-card,
.lifecycle-event-card {
  display: grid;
  gap: 10px;
  border: 1px solid rgba(228, 228, 231, 0.8);
  border-radius: 8px;
  background: #fafafa;
  padding: 10px;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.02);
}

.field-permission-footer {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  align-items: center;
}

.form-rule-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 8px;
  padding: 8px;
  border: 1px solid rgba(228, 228, 231, 0.72);
  border-radius: 8px;
  background: #fff;
}

.field-rule-switches {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 6px;
}

.field-rule-switches label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  min-width: 0;
  padding: 6px 7px;
  border: 1px solid #e4e4e7;
  border-radius: 6px;
  background: #fafafa;
}

.field-rule-switches span {
  color: #52525b;
  font-size: 11px;
  font-weight: 600;
  white-space: nowrap;
}

.form-event-row {
  display: grid;
  grid-template-columns: minmax(90px, 1fr) minmax(90px, 1fr) minmax(120px, 1.4fr) auto;
  align-items: center;
  gap: 6px;
}

.lifecycle-event-card {
  position: relative;
  padding-right: 34px;
}

.event-delete-icon {
  position: absolute;
  top: 8px;
  right: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  cursor: pointer;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: #a1a1aa;
  font-size: 15px;
  line-height: 1;
  opacity: 0;
  transition:
    opacity 160ms ease,
    background-color 160ms ease,
    color 160ms ease;
}

.lifecycle-event-card:hover .event-delete-icon {
  opacity: 1;
}

.event-delete-icon:hover {
  background: #fef2f2;
  color: #ef4444;
}

.event-type-field {
  padding-right: 2px;
}

.switch-list + :deep(.n-form-item) {
  margin-top: 12px;
}

.switch-list label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 32px;
  border: 1px solid #eff0f1;
  border-radius: 6px;
  background: #fff;
  color: #1f2329;
  font-size: 12px;
  padding: 0 9px;
}

.switch-list span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.size-setting-row {
  display: grid;
  gap: 8px;
  margin-bottom: 12px;
}

.size-setting-row:last-child {
  margin-bottom: 0;
}

.size-setting-label {
  color: #646a73;
  font-size: 12px;
  font-weight: 500;
  line-height: 20px;
}
</style>
