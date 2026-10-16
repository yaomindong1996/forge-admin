<template>
  <div class="business-list-designer">
    <div class="list-designer-head">
      <div class="list-designer-title">
        <h3>列表设计</h3>
        <p>{{ layoutModeLabel }} · {{ designerPages.length }} 个页面</p>
      </div>
      <n-space class="list-designer-actions" size="small" align="center">
        <n-button class="list-toolbar-icon-button" circle size="small" secondary :disabled="!canUndo" title="撤销" @click="undoSchema">
          <template #icon>
            <n-icon><ArrowUndoOutline /></n-icon>
          </template>
        </n-button>
        <n-button class="list-toolbar-icon-button" circle size="small" secondary :disabled="!canRedo" title="重做" @click="redoSchema">
          <template #icon>
            <n-icon><ArrowRedoOutline /></n-icon>
          </template>
        </n-button>
        <n-dropdown trigger="click" :options="listMoreOptions" @select="handleListMoreSelect">
          <n-button class="list-toolbar-more-button" circle size="small" type="primary" title="更多操作">
            <template #icon>
              <n-icon><EllipsisHorizontalOutline /></n-icon>
            </template>
          </n-button>
        </n-dropdown>
      </n-space>
    </div>

    <div class="list-page-switch">
      <div class="page-switch-row">
        <div class="page-switch-title">
          <span class="page-switch-icon">P</span>
          <div class="page-switch-copy">
            <n-dropdown
              trigger="click"
              :options="listTemplateOptions"
              @select="updateListTemplate"
            >
              <button class="list-template-trigger" type="button">
                <n-icon><LayersOutline /></n-icon>
                <span>{{ activeTemplateLabel }}</span>
                <n-icon class="template-chevron">
                  <ChevronDownOutline />
                </n-icon>
              </button>
            </n-dropdown>
            <small>{{ pageTypeText(activeDesignerPage?.pageType) }} · {{ designerPages.length }} 个页面</small>
          </div>
        </div>
        <div class="page-tab-list">
          <button
            v-for="page in designerPages"
            :key="page.pageKey"
            type="button"
            class="page-tab-button"
            :class="[{ active: page.pageKey === activePageKey }, `type-${page.pageType || 'custom'}`]"
            @click="switchActivePage(page.pageKey)"
          >
            <span class="page-tab-name">{{ page.pageName || page.pageKey }}</span>
            <small>{{ pageTypeText(page.pageType) }} · {{ page.pageKey }}</small>
          </button>
          <n-button class="page-tool-button page-icon-button" circle size="small" secondary type="primary" title="新增页面" @click="addDesignerPage">
            <template #icon>
              <n-icon><AddOutline /></n-icon>
            </template>
          </n-button>
        </div>
        <div class="page-actions">
          <n-button class="page-tool-button page-icon-button" circle size="small" secondary :disabled="!canUndo" title="撤销" @click="undoSchema">
            <template #icon>
              <n-icon><ArrowUndoOutline /></n-icon>
            </template>
          </n-button>
          <n-button class="page-tool-button page-icon-button" circle size="small" secondary :disabled="!canRedo" title="重做" @click="redoSchema">
            <template #icon>
              <n-icon><ArrowRedoOutline /></n-icon>
            </template>
          </n-button>
          <n-button class="page-tool-button page-icon-button" circle size="small" secondary :title="pageSettingsExpanded ? '收起页面设置' : '页面设置'" @click="pageSettingsExpanded = !pageSettingsExpanded">
            <template #icon>
              <n-icon><DocumentTextOutline /></n-icon>
            </template>
          </n-button>
          <n-dropdown trigger="click" placement="bottom-end" :options="pageActionOptions" @select="handlePageActionSelect">
            <n-button class="page-tool-button page-icon-button" circle size="small" secondary title="页面操作">
              <template #icon>
                <n-icon><EllipsisHorizontalOutline /></n-icon>
              </template>
            </n-button>
          </n-dropdown>
        </div>
      </div>
      <div v-if="activeDesignerPage && pageSettingsExpanded" class="page-settings-popover">
        <div class="page-settings-head">
          <strong>页面设置</strong>
          <span>{{ activeDesignerPage.pageName || activeDesignerPage.pageKey }}</span>
        </div>
        <div class="page-config-row">
          <n-input
            :value="activeDesignerPage.pageName"
            size="small"
            placeholder="页面名称"
            @update:value="patchActivePage({ pageName: $event })"
          />
          <n-input
            :value="activeDesignerPage.pageKey"
            size="small"
            placeholder="页面编码"
            :disabled="isProtectedPage(activeDesignerPage.pageKey)"
            @update:value="updateActivePageKey($event)"
          />
          <n-select
            :value="activeDesignerPage.pageType || 'custom'"
            :options="pageTypeOptions"
            size="small"
            @update:value="patchActivePage({ pageType: $event })"
          />
          <n-input
            :value="activeDesignerPage.routePath"
            size="small"
            placeholder="路由片段/页面标识，如 dialog/customer"
            title="当前多页面仍由业务对象运行页统一承载，这里用于事件跳转、弹窗页和自定义页面识别，不是单独前端路由文件"
            @update:value="patchActivePage({ routePath: $event })"
          />
          <n-input
            :value="activeDesignerPage.description"
            size="small"
            placeholder="页面说明"
            @update:value="patchActivePage({ description: $event })"
          />
        </div>
        <div class="page-config-row page-watermark-row">
          <div class="page-watermark-label">
            <strong>页面水印</strong>
            <small>覆盖整个列表页（预览/打印），不是画布上的水印组件</small>
          </div>
          <n-switch
            size="small"
            :value="pageWatermark.enabled"
            @update:value="patchPageWatermark({ enabled: $event })"
          />
          <n-input
            v-if="pageWatermark.enabled"
            size="small"
            :value="pageWatermark.text"
            maxlength="50"
            placeholder="水印文字，例如内部资料"
            @update:value="patchPageWatermark({ text: $event })"
          />
        </div>
        <div class="page-param-row">
          <div class="page-param-title">
            页面入参
          </div>
          <div
            v-for="(param, paramIdx) in (activeDesignerPage.params || [])"
            :key="paramIdx"
            class="page-param-item"
          >
            <n-input
              :value="param.name"
              size="tiny"
              placeholder="参数名"
              @update:value="updateActivePageParam(paramIdx, { name: normalizePageParamName($event) })"
            />
            <n-input
              :value="param.value"
              size="tiny"
              placeholder="默认值 / 字段映射"
              @update:value="updateActivePageParam(paramIdx, { value: $event })"
            />
            <n-button size="tiny" quaternary circle title="删除参数" @click="removeActivePageParam(paramIdx)">
              <template #icon>
                <n-icon><TrashOutline /></n-icon>
              </template>
            </n-button>
          </div>
          <n-button class="page-param-add-button" size="tiny" dashed @click="addActivePageParam">
            + 参数
          </n-button>
        </div>
        <div class="page-settings-footer">
          <n-button size="tiny" secondary title="复制页面" @click="duplicateActivePage">
            <template #icon>
              <n-icon><CopyOutline /></n-icon>
            </template>
            复制
          </n-button>
          <n-button size="tiny" secondary title="重置当前页面布局" @click="resetActivePageLayout">
            <template #icon>
              <n-icon><RefreshOutline /></n-icon>
            </template>
            重置
          </n-button>
          <n-popconfirm
            :show-icon="false"
            positive-text="清空"
            negative-text="取消"
            @positive-click="clearActivePageLayout"
          >
            <template #trigger>
              <n-button size="tiny" secondary title="清空当前页面布局">
                <template #icon>
                  <n-icon><CloseCircleOutline /></n-icon>
                </template>
                清空
              </n-button>
            </template>
            清空当前页面画布上的所有组件？
          </n-popconfirm>
          <n-popconfirm
            :show-icon="false"
            positive-text="删除"
            negative-text="取消"
            :disabled="isProtectedPage(activePageKey)"
            @positive-click="removeActivePage"
          >
            <template #trigger>
              <n-button size="tiny" secondary type="error" :disabled="isProtectedPage(activePageKey)" title="删除当前页面">
                <template #icon>
                  <n-icon><TrashOutline /></n-icon>
                </template>
                删除
              </n-button>
            </template>
            删除当前页面及布局？列表页不允许删除。
          </n-popconfirm>
        </div>
      </div>
    </div>

    <div class="list-designer-body">
      <main class="list-workspace">
        <ListPageGridDesigner
          ref="listGridDesignerRef"
          :model-value="designerGridLayout"
          :fields="designFields"
          :model-schema="effectiveModelSchema"
          :layout-type="localSchema.layoutType"
          :page-name="activeDesignerPage?.pageName || '列表页'"
          :pages="designerPages"
          :action-pages="actionPageOptions"
          :form-options="formOptions"
          :runtime-crud-props="designerRuntimeCrudProps"
          :custom-actions="visibleListCustomActions"
          :custom-actions-editable="!defaultViewOnly"
          @update:model-value="handleGridLayoutUpdate"
          @update:model-schema="handleGridModelSchemaUpdate"
          @update:custom-actions="handleListCustomActionsUpdate"
        />
      </main>
    </div>

    <!-- 预览始终锁定「列表页」：自定义/弹窗等二级页只通过列表跳转进入，不进预览回落 -->
    <n-modal
      v-model:show="listPreviewVisible"
      preset="card"
      title="预览当前列表"
      class="list-preview-modal"
      :bordered="false"
      :style="{ width: 'calc(100vw - 32px)', maxWidth: 'calc(100vw - 32px)', minWidth: 0, height: 'calc(100vh - 32px)' }"
    >
      <n-watermark
        v-if="listPreviewWatermark.enabled && listPreviewWatermarkContent"
        class="list-preview-watermark"
        :content="listPreviewWatermarkContent"
        cross
        :font-size="16"
        :line-height="16"
        :width="192"
        :height="128"
        :x-offset="12"
        :y-offset="28"
        :rotate="-18"
        font-color="rgba(100, 116, 139, 0.18)"
      >
        <div
          v-if="listPreviewRuntimeModel"
          class="list-preview-runtime-flow"
        >
          <RuntimeListGridFlow
            :key="`preview-flow-${listPreviewLayoutKey}`"
            :layout="listPreviewRuntimeModel.layout"
            :fields="listPreviewRuntimeModel.fields"
            :runtime-crud-props="designerRuntimeCrudProps"
            :runtime-interactive="false"
            :force-live-preview="false"
            fill-host
          />
        </div>
        <ListPageGridDesigner
          v-else
          :key="`preview-${listPreviewLayoutKey}`"
          :model-value="listOnlyPreviewLayout"
          :fields="designFields"
          :model-schema="effectiveModelSchema"
          :layout-type="localSchema.layoutType"
          :page-name="listPreviewPageName"
          :pages="designerPages"
          :action-pages="actionPageOptions"
          :form-options="formOptions"
          :runtime-crud-props="designerRuntimeCrudProps"
          :custom-actions="visibleListCustomActions"
          readonly
        />
      </n-watermark>
      <div
        v-else-if="listPreviewRuntimeModel"
        class="list-preview-runtime-flow"
      >
        <RuntimeListGridFlow
          :key="`preview-flow-${listPreviewLayoutKey}`"
          :layout="listPreviewRuntimeModel.layout"
          :fields="listPreviewRuntimeModel.fields"
          :runtime-crud-props="designerRuntimeCrudProps"
          :runtime-interactive="false"
          :force-live-preview="false"
          fill-host
        />
      </div>
      <ListPageGridDesigner
        v-else
        :key="`preview-${listPreviewLayoutKey}`"
        :model-value="listOnlyPreviewLayout"
        :fields="designFields"
        :model-schema="effectiveModelSchema"
        :layout-type="localSchema.layoutType"
        :page-name="listPreviewPageName"
        :pages="designerPages"
        :action-pages="actionPageOptions"
        :form-options="formOptions"
        :runtime-crud-props="designerRuntimeCrudProps"
        :custom-actions="visibleListCustomActions"
        readonly
      />
    </n-modal>
  </div>
</template>

<script>
import { businessListDesignerLocalComponents } from './businessListDesignerLocalComponents'
import { useBusinessListDesigner } from './composables/useBusinessListDesigner'

export default {
  name: 'BusinessListDesigner',
  components: {
    ...businessListDesignerLocalComponents,
  },
  props: {
  objectId: {
    type: [Number, String],
    default: null,
  },
  modelValue: {
    type: Object,
    default: null,
  },
  modelSchema: {
    type: Object,
    default: () => ({}),
  },
  fields: {
    type: Array,
    default: () => [],
  },
  viewSchema: {
    type: Object,
    default: null,
  },
  formOptions: {
    type: Array,
    default: () => [],
  },
  /** 应用级页面列表（pageId），用于自定义操作「应用内页面」跳转 */
  applicationPages: {
    type: Array,
    default: () => [],
  },
  designerOptions: {
    type: Object,
    default: () => ({}),
  },
  designerActions: {
    type: Array,
    default: () => [],
  },
  defaultViewOnly: {
    type: Boolean,
    default: false,
  },
},
  emits: ['update:modelValue', 'update:modelSchema', 'update:viewSchema', 'update:designerActions', 'saved', 'dirtyChange'],
  setup(props, { emit, expose }) {
    return useBusinessListDesigner(props, emit, expose)
  },
}
</script>

<style scoped src="./businessListDesigner.css"></style>
