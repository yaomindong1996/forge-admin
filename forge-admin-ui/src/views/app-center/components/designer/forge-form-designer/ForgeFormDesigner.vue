<template>
  <div
    class="forge-form-designer"
    :class="{ 'left-collapsed': leftCollapsed, 'right-open': rightOpen, 'right-collapsed': !rightOpen, 'canvas-focused': canvasFocusMode, 'section-view': canvasView !== 'layout' }"
  >
    <aside v-if="canvasView === 'layout'" class="designer-left">
      <button
        v-if="leftCollapsed"
        type="button"
        class="side-rail-toggle-button"
        title="展开组件库"
        @click="leftCollapsed = false"
      >
        <n-icon><ChevronForwardOutline /></n-icon>
      </button>
      <ForgeFieldShelf
        v-if="!leftCollapsed"
        :fields="fields"
        :used-field-set="usedFieldSet"
        :relations="relations"
        @append-field="appendField"
      >
        <template #actions>
          <n-button
            class="field-shelf-collapse-button"
            circle
            size="small"
            secondary
            title="收起组件库"
            @click="leftCollapsed = true"
          >
            <template #icon>
              <n-icon><ChevronBackOutline /></n-icon>
            </template>
          </n-button>
        </template>
      </ForgeFieldShelf>
    </aside>

    <main class="designer-center">
      <div class="designer-toolbar">
        <div>
          <div class="flex items-center gap-4">
            <h3>{{ normalizedSchema.formName || objectName || '业务表单' }}</h3>
            <n-button
              quaternary
              circle
              size="tiny"
              class="designer-toolbar-rename-btn"
              title="编辑表单名称"
              @click="openRenameCurrentForm"
            >
              <template #icon>
                <span class="designer-toolbar-rename-icon" aria-hidden="true">✎</span>
              </template>
            </n-button>
          </div>
          <p>{{ canvasMetaText }}</p>
        </div>
        <NSpace size="small" align="center">
          <n-button class="designer-toolbar-text-button" size="small" secondary :disabled="!canUndo" @click="undoSchema">
            <template #icon>
              <n-icon><ArrowUndoOutline /></n-icon>
            </template>
            撤销
          </n-button>
          <n-button class="designer-toolbar-text-button" size="small" secondary :disabled="!canRedo" @click="redoSchema">
            <template #icon>
              <n-icon><ArrowRedoOutline /></n-icon>
            </template>
            重做
          </n-button>
          <n-button class="designer-toolbar-text-button designer-toolbar-danger-button" size="small" secondary :disabled="!canClearCanvas" @click="openClearCanvasDialog">
            <template #icon>
              <n-icon><TrashOutline /></n-icon>
            </template>
            清空
          </n-button>
          <n-dropdown trigger="click" :options="designerMoreOptions" @select="handleDesignerMoreSelect">
            <n-button class="designer-toolbar-more-button" circle size="small" type="primary" title="更多操作">
              <template #icon>
                <n-icon><EllipsisHorizontalOutline /></n-icon>
              </template>
            </n-button>
          </n-dropdown>
          <n-button
            class="designer-toolbar-icon-button"
            circle
            size="small"
            type="primary"
            :title="rightOpen ? '收起属性栏' : '打开属性栏'"
            @click="rightOpen ? (rightOpen = false) : openPropertyPanel(selectedId)"
          >
            <template #icon>
              <n-icon>
                <ChevronForwardOutline v-if="rightOpen" />
                <ChevronBackOutline v-else />
              </n-icon>
            </template>
          </n-button>
        </NSpace>
      </div>

      <n-dropdown
        trigger="manual"
        placement="bottom-start"
        :show="formTabsMenuVisible"
        :x="formTabsMenuX"
        :y="formTabsMenuY"
        :options="formTabsMenuOptions"
        @select="handleFormTabsMenuSelect"
        @clickoutside="formTabsMenuVisible = false"
      />
      <div class="designer-form-tabs-bar page-design-switcher" @contextmenu.prevent="openFormTabsMenu">
        <div class="page-switch-title">
          <span class="page-switch-icon">{{ canvasViewIcon }}</span>
          <div>
            <strong>{{ canvasViewTitle }}</strong>
            <small>{{ canvasViewDescription }}</small>
          </div>
        </div>
        <div class="page-view-controls">
          <n-radio-group v-model:value="canvasView" size="small" aria-label="表单页画布视图">
            <n-radio-button value="layout">
              表单布局
            </n-radio-button>
            <n-radio-button v-if="enableSectionsView" value="sections">
              页面分区
            </n-radio-button>
            <n-radio-button v-if="hasDetailSettings" value="detail">
              详情设置
            </n-radio-button>
          </n-radio-group>
          <div v-if="formAssets.length && canvasView === 'layout'" class="designer-form-tabs" aria-label="表单切换">
            <button type="button" class="designer-form-tab active" @click="selectedId = ''">
              <em>1</em>
              <span>{{ normalizedSchema.formName || '主表单' }}</span>
            </button>
            <button
              v-for="(asset, assetIndex) in formAssets"
              :key="asset.formKey"
              type="button"
              class="designer-form-tab"
              @click="switchFormAsset(asset.formKey)"
            >
              <em>{{ assetIndex + 2 }}</em>
              <span>{{ asset.formName || `表单 ${assetIndex + 2}` }}</span>
            </button>
          </div>
        </div>
        <div class="page-tools">
          <div v-if="canvasView === 'layout'" class="page-switch-actions">
            <n-button class="designer-toolbar-icon-button neutral" circle size="small" secondary :disabled="!canUndo" title="撤销" @click="undoSchema">
              <template #icon>
                <n-icon><ArrowUndoOutline /></n-icon>
              </template>
            </n-button>
            <n-button class="designer-toolbar-icon-button neutral" circle size="small" secondary :disabled="!canRedo" title="重做" @click="redoSchema">
              <template #icon>
                <n-icon><ArrowRedoOutline /></n-icon>
              </template>
            </n-button>
            <n-button class="designer-toolbar-icon-button neutral danger" circle size="small" secondary :disabled="!canClearCanvas" title="清空画布" @click="openClearCanvasDialog">
              <template #icon>
                <n-icon><TrashOutline /></n-icon>
              </template>
            </n-button>
            <n-dropdown trigger="click" placement="bottom-end" :options="formTabsMenuOptions" @select="handleFormTabsMenuSelect">
              <n-button class="designer-toolbar-icon-button neutral" circle size="small" secondary title="表单页面操作">
                <template #icon>
                  <n-icon><EllipsisHorizontalOutline /></n-icon>
                </template>
              </n-button>
            </n-dropdown>
            <n-button
              class="designer-toolbar-icon-button neutral"
              circle
              size="small"
              secondary
              :title="rightOpen ? '收起属性栏' : '打开属性栏'"
              @click="rightOpen ? (rightOpen = false) : openPropertyPanel(selectedId)"
            >
              <template #icon>
                <n-icon>
                  <ChevronForwardOutline v-if="rightOpen" />
                  <ChevronBackOutline v-else />
                </n-icon>
              </template>
            </n-button>
          </div>
        </div>
      </div>

      <ForgeFormCanvas
        v-if="canvasView === 'layout'"
        :schema="normalizedSchema"
        :fields="fields"
        :selected-id="selectedId"
        @update:schema="updateSchema"
        @update:selected-id="handleCanvasSelectedIdChange"
        @configure="openPropertyPanel"
        @configure-sub-table="relationKey => emit('editSubTableContainer', relationKey)"
        @open-source="openSourcePanel"
        @toggle-focus="toggleCanvasFocus"
      />
      <PageSectionEditor
        v-else-if="canvasView === 'sections'"
        class="inline-page-section-editor"
        :model-value="pageSectionProtocol"
        :fields="fields"
        :relations="relations"
        :actions="actions"
        @update:model-value="updatePageSectionProtocol"
        @configure-bottom-action="emit('configureBottomAction', $event)"
        @edit-child-table-section="emit('editChildTableSection', $event)"
        @remove-child-table-section="emit('removeChildTableSection', $event)"
      />
      <div v-else class="inline-detail-settings">
        <slot name="detail-settings" />
      </div>
    </main>

    <aside v-if="canvasView === 'layout'" class="designer-right">
      <ForgePropertyPanel
        v-if="rightOpen"
        :schema="normalizedSchema"
        :fields="fields"
        :relations="relations"
        :object-code="objectCode"
        :selected-id="selectedId"
        :initial-form-tab="initialPropertyTab"
        @update:schema="updateSchema"
        @update:selected-id="selectedId = $event"
        @field-asset-updated="emit('fieldAssetUpdated', $event)"
        @update-field-generation="emit('updateFieldGeneration', $event)"
        @close="rightOpen = false"
      />
    </aside>
  </div>
  <n-modal
    v-model:show="bottomBarDialogVisible"
    preset="card"
    title="底部操作栏"
    :bordered="false"
    class="designer-bottom-bar-modal"
    :style="{ width: 'min(960px, calc(100vw - 40px))' }"
  >
    <BottomBarEditor
      :model-value="normalizedSchema.bottomBar || {}"
      :fields="fields"
      @update:model-value="updateBottomBarFromDialog"
      @configure-bottom-action="payload => emit('configureBottomAction', payload)"
    />
  </n-modal>
  <n-modal
    v-model:show="previewDialogVisible"
    preset="card"
    title="预览当前表单"
    class="designer-preview-modal"
    :bordered="false"
    :style="{
      width: 'min(1120px, calc(100vw - 40px))',
      maxWidth: 'calc(100vw - 40px)',
      height: 'min(860px, calc(100vh - 40px))',
    }"
  >
    <div class="designer-preview-toolbar">
      <div>
        <strong>{{ previewModeTitle }}</strong>
        <span>{{ previewModeDescription }}</span>
      </div>
      <n-radio-group
        v-model:value="previewMode"
        size="small"
      >
        <n-radio-button
          v-for="option in previewModeOptions"
          :key="option.value"
          :value="option.value"
        >
          {{ option.label }}
        </n-radio-button>
      </n-radio-group>
    </div>
    <div class="designer-preview-runtime">
      <AiForm
        class="designer-preview-runtime-form"
        :schema="previewSchema"
        :value="previewFormValue"
        :label-placement="previewLayout.labelPlacement || 'left'"
        :label-width="previewLayout.labelWidth ?? 'auto'"
        :label-align="previewLayout.labelAlign || 'right'"
        :size="previewLayout.size || 'medium'"
        :grid-cols="previewGridCols"
        :x-gap="previewLayout.xGap || previewLayout.columnGap || 12"
        :y-gap="previewLayout.yGap || previewLayout.rowGap || 0"
        :show-actions="false"
        :show-feedback="previewLayout.showFeedback !== false"
        :context="previewRuntimeContext"
        :form-assets="formAssets"
        :keep-empty-layout-nodes="true"
        @update:value="previewFormValue = $event || {}"
      />
    </div>
  </n-modal>

  <n-modal
    v-model:show="clearDialogVisible"
    preset="card"
    title="清空画布"
    class="designer-clear-modal"
    :bordered="false"
    :mask-closable="false"
    style="width: 420px"
  >
    <div class="designer-clear-content">
      <div class="designer-clear-warning">
        <n-icon><WarningOutline /></n-icon>
        <div>
          <strong>清空后会删除画布上的组件配置</strong>
          <span>字段资产不会删除，操作可通过撤销恢复。</span>
        </div>
      </div>
      <n-radio-group v-model:value="clearScope">
        <NSpace vertical size="small">
          <n-radio value="current">
            仅清空当前画布：{{ normalizedSchema.formName || '当前表单' }}
          </n-radio>
          <n-radio value="all">
            清空全部画布：主表单和 {{ formAssets.length }} 个子表单
          </n-radio>
        </NSpace>
      </n-radio-group>
    </div>
    <template #footer>
      <div class="designer-clear-footer">
        <n-button size="small" @click="clearDialogVisible = false">
          取消
        </n-button>
        <n-button size="small" type="error" @click="confirmClearCanvas">
          确认清空
        </n-button>
      </div>
    </template>
  </n-modal>

  <n-modal
    v-model:show="renameDialogVisible"
    preset="card"
    title="编辑表单名称"
    class="designer-rename-modal"
    :bordered="false"
    :mask-closable="false"
    style="width: 400px"
  >
    <n-input
      v-model:value="renameFormName"
      placeholder="请输入表单名称"
      clearable
      @keyup.enter="confirmRenameCurrentForm"
    />
    <template #footer>
      <div class="designer-rename-modal-footer">
        <n-button size="small" @click="renameDialogVisible = false">
          取消
        </n-button>
        <n-button size="small" type="primary" @click="confirmRenameCurrentForm">
          保存
        </n-button>
      </div>
    </template>
  </n-modal>
</template>

<script>
import { forgeFormDesignerPageLocalComponents } from './forgeFormDesignerPageLocalComponents'
import { useForgeFormDesignerPage } from './composables/useForgeFormDesignerPage'

export default {
  name: 'ForgeFormDesignerPage',
  components: {
    ...forgeFormDesignerPageLocalComponents,
  },
  props: {
  modelValue: {
    type: Object,
    default: null,
  },
  fields: {
    type: Array,
    default: () => [],
  },
  objectCode: {
    type: String,
    default: '',
  },
  objectName: {
    type: String,
    default: '',
  },
  relations: {
    type: Array,
    default: () => [],
  },
  actions: {
    type: Array,
    default: () => [],
  },
  linkageSchema: {
    type: Object,
    default: null,
  },
  extraMoreOptions: {
    type: Array,
    default: () => [],
  },
  initialPropertyTab: {
    type: String,
    default: 'basic',
  },
  initialCanvasView: {
    type: String,
    default: 'layout',
    validator: value => ['layout', 'sections', 'detail'].includes(value),
  },
  // 是否提供独立「页面分区」视图：分区由布局容器承载时宿主应关闭，分区随画布派生维护。
  enableSectionsView: {
    type: Boolean,
    default: true,
  },
  // 开启后每次 schema 变更都会从布局组件树派生 pageSections 写回（card/collapse=内容分区、subTable=子表分区）。
  deriveSectionsFromLayout: {
    type: Boolean,
    default: false,
  },
},
  emits: [
  'update:modelValue',
  'update:linkageSchema',
  'dirtyChange',
  'moreSelect',
  'fieldAssetUpdated',
  'updateFieldGeneration',
  'configureBottomAction',
  'editSubTableContainer',
  'editChildTableSection',
  'removeChildTableSection',
],
  setup(props, { emit, expose }) {
    return useForgeFormDesignerPage(props, emit, expose)
  },
}
</script>

<style scoped src="./forgeFormDesignerPage.css"></style>
