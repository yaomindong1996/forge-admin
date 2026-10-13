<template>
            <!-- 无匹配区块类型时不挂空卡片（靠 CSS :not(:has(> *)) 兜底） -->
            <div v-show="propertyPanelTab === 'props'" class="property-tab-content property-tab-content--widgets">
              <template v-if="selectedBlock.blockType === 'timeline'">
                <n-form-item :label="isTimelineBound(selectedBlock) ? '静态节点（无数据时占位）' : '时间线节点'">
                  <div class="metrics-editor">
                    <div v-if="isTimelineBound(selectedBlock)" class="field-help">
                      当前已绑定数据源：画布按数组渲染。这里仅在无数据时作占位。
                    </div>
                    <n-input
                      :value="selectedBlock.props?.title"
                      placeholder="区块标题，如操作记录"
                      @update:value="patchBlockProps(selectedBlock.id, { title: $event })"
                    />
                    <div
                      v-for="(item, idx) in (selectedBlock.props?.items || [])"
                      :key="idx"
                      class="metric-row metric-row-card"
                    >
                      <div class="metric-row-fields">
                        <n-input
                          :value="item.title"
                          placeholder="节点标题"
                          size="small"
                          @update:value="updateTimelineItem(idx, { title: $event })"
                        />
                        <n-input
                          :value="item.time"
                          placeholder="时间"
                          size="small"
                          @update:value="updateTimelineItem(idx, { time: $event })"
                        />
                        <n-input
                          class="metric-trend-input"
                          :value="item.content"
                          placeholder="节点内容"
                          size="small"
                          type="textarea"
                          :rows="2"
                          @update:value="updateTimelineItem(idx, { content: $event })"
                        />
                        <n-button class="metric-row-remove" size="tiny" quaternary type="error" @click="removeTimelineItem(idx)">
                          删除
                        </n-button>
                      </div>
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
                <!-- tabs 外观属性（type/placement/size/trigger/animated/closable/addable 等）统一由下方
                     「组件属性」SpecPropertyPanel 渲染（designer-core spec 唯一属性源），此处只保留页签管理 -->
                <n-divider>标签页</n-divider>
                <div class="tab-manager">
                  <div class="tab-manager-head">
                    <span>页签管理</span>
                    <n-button size="tiny" type="primary" secondary @click="addTab">
                      新增页签
                    </n-button>
                  </div>
                  <div
                    v-for="(tab, idx) in (selectedBlock.props?.tabs || [])"
                    :key="tab.key"
                    class="tab-manager-card"
                  >
                    <n-input
                      :value="tab.title"
                      placeholder="页签名称"
                      size="small"
                      @update:value="updateTab(idx, { title: $event || `标签 ${idx + 1}` })"
                    />
                    <div class="tab-manager-actions">
                      <n-button size="tiny" tertiary :disabled="idx === 0" @click="moveTab(idx, -1)">
                        上移
                      </n-button>
                      <n-button size="tiny" tertiary :disabled="idx === (selectedBlock.props?.tabs?.length || 0) - 1" @click="moveTab(idx, 1)">
                        下移
                      </n-button>
                      <n-button size="tiny" quaternary type="error" :disabled="(selectedBlock.props?.tabs?.length || 0) <= 1" @click="removeTab(idx)">
                        删除
                      </n-button>
                    </div>
                  </div>
                </div>
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
                      <WidgetDataBindingEditor
                        :model-value="selectedBlock.props?.dataBinding || {}"
                        :block-type="selectedBlock.blockType"
                        :fields="fields"
                        :form-designer-schema="formDesignerSchema"
                        @update:model-value="updateWidgetDataBinding"
                      />
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
                      <n-color-picker :value="selectedBlock.props?.color || '#0f172a'" :show-alpha="true" @update:value="patchBlockProps(selectedBlock.id, { color: $event || 'var(--text-primary)' })" />
                    </div>
                  </n-form-item>
                </template>
                <template v-if="selectedBlock.blockType === 'statistic'">
                  <n-form-item :label="isWidgetBindingActive(selectedBlock) ? '统计内容（占位默认）' : '统计内容'">
                    <div class="style-grid two">
                      <div v-if="isWidgetBindingActive(selectedBlock)" class="field-help" style="grid-column: 1 / -1">
                        标题 / 数值 / 说明 / 趋势由上方「本组件可赋值项」绑定字段；下方仅作未绑定时占位。前缀、后缀、颜色仍为静态样式。
                      </div>
                      <n-input
                        :value="selectedBlock.props?.title"
                        :placeholder="isWidgetBindingActive(selectedBlock) ? '占位标题' : '标题'"
                        @update:value="patchBlockProps(selectedBlock.id, { title: $event })"
                      />
                      <n-input
                        :value="selectedBlock.props?.value"
                        :placeholder="isWidgetBindingActive(selectedBlock) ? '占位数值' : '数值'"
                        @update:value="patchBlockProps(selectedBlock.id, { value: $event })"
                      />
                      <n-input :value="selectedBlock.props?.prefix" placeholder="前缀" @update:value="patchBlockProps(selectedBlock.id, { prefix: $event })" />
                      <n-input :value="selectedBlock.props?.suffix" placeholder="后缀" @update:value="patchBlockProps(selectedBlock.id, { suffix: $event })" />
                      <n-input
                        :value="selectedBlock.props?.trend"
                        :placeholder="isWidgetBindingActive(selectedBlock) ? '占位趋势' : '趋势'"
                        @update:value="patchBlockProps(selectedBlock.id, { trend: $event })"
                      />
                      <n-input
                        :value="selectedBlock.props?.description"
                        :placeholder="isWidgetBindingActive(selectedBlock) ? '占位说明' : '说明'"
                        @update:value="patchBlockProps(selectedBlock.id, { description: $event })"
                      />
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

</template>

<script>
import { useListPageDesignerApi } from './listPageDesignerContext'
import { listPageDesignerLocalComponents } from './listPageDesignerLocalComponents'

export default {
  components: { ...listPageDesignerLocalComponents },
  name: 'ListPagePropertyPropsTabPart3',
  setup() {
    return useListPageDesignerApi()
  },
}
</script>

<style scoped src="./list-page-grid-designer-shell.css"></style>
<style scoped src="./list-page-grid-designer-panels.css"></style>
