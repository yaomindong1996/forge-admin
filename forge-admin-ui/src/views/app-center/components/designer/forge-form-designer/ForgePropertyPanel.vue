<template>
  <div class="forge-property-panel">
    <div class="edit-panel-header">
      <div class="edit-panel-title">
        <button
          v-if="selectedComponent"
          type="button"
          class="panel-back-button"
          title="返回表单属性"
          @click="designerStore.selectComponent('')"
        >
          <svg width="1em" height="1em" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M15.41 7.41 14 6l-6 6 6 6 1.41-1.41L10.83 12z" fill="currentColor" />
          </svg>
        </button>
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

    <template v-if="selectedComponent && !isSubTable">
      <div class="property-panel-body">
        <n-tabs v-model:value="propertyActiveTab" type="segment" size="small" :animated="false" class="property-tabs">
          <n-tab-pane name="basic">
            <template #tab>
              <span class="property-tab-label">
                <n-icon><SettingsOutline /></n-icon>
                属性
              </span>
            </template>
            <SelectedBasicTab />
          </n-tab-pane>
          <n-tab-pane name="style">
            <template #tab>
              <span class="property-tab-label">
                <n-icon><ColorPaletteOutline /></n-icon>
                样式
              </span>
            </template>
            <SelectedStyleTab />
          </n-tab-pane>
          <n-tab-pane v-if="isCrudBlock" name="crud">
            <template #tab>
              <span class="property-tab-label">
                <n-icon><ServerOutline /></n-icon>
                CRUD
              </span>
            </template>
            <SelectedCrudTab />
          </n-tab-pane>
          <n-tab-pane name="interaction">
            <template #tab>
              <span class="property-tab-label">
                <n-icon><FlashOutline /></n-icon>
                交互
              </span>
            </template>
            <SelectedInteractionTab />
          </n-tab-pane>
        </n-tabs>
      </div>

      <SelectedPropertyDrawers />
    </template>

    <div v-else-if="isSubTable" class="subtable-inline-editor">
      <SubTableInlineEditor :component="selectedComponent" />
    </div>

    <div v-else class="property-panel-body">
      <FormPropertyTabs />
    </div>

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

    <!-- recordSelector 选择器设置弹窗：选择方式 / 搜索字段 / 展示列 / 过滤参数 / 字段映射 -->
    <RecordSelectorConfigDialog
      v-model:show="rsConfigDialogShow"
      :field-options="recordSelectorTargetFieldOptions"
      :main-field-options="formFieldOptions"
      :multiple="recordSelectorMultiple"
      :keyword-fields="recordSelectorKeywordFields"
      :display-fields="recordSelectorDisplayFields"
      :filters="rsDialogFilters"
      :mappings="rsDialogMappings"
      :loading="recordSelectorTargetFieldLoading"
      @confirm="handleRsConfigConfirm"
    />

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

    <!-- 查询源参数配置弹窗 -->
    <n-modal
      v-model:show="queryParamModalVisible"
      preset="card"
      class="query-source-param-modal"
      :bordered="false"
      :mask-closable="false"
      style="width: min(560px, calc(100vw - 40px))"
      title="查询参数配置"
    >
      <div class="query-param-modal-body">
        <div class="query-param-modal-hint">
          为每个参数选择表单字段引用（运行时自动取值），或输入固定值。
        </div>
        <div class="query-param-modal-list">
          <div
            v-for="param in querySourceParamRows"
            :key="param.name"
            class="query-param-modal-row"
          >
            <div class="query-param-modal-row-header">
              <span v-if="param.required" class="schema-param-required">*</span>
              <span class="query-param-modal-row-label">{{ param.label }}</span>
              <span class="query-param-modal-row-code">{{ param.name }}</span>
              <span v-if="param.type" class="query-param-modal-row-type">{{ param.type }}</span>
            </div>
            <n-select
              :value="queryParamModalDraft[param.name] || null"
              :options="getQuerySourceParamOptions(queryParamModalDraft[param.name])"
              size="small"
              filterable
              tag
              clearable
              placeholder="选择表单字段或输入固定值"
              @update:value="updateQueryParamDraft(param.name, $event)"
            />
            <div v-if="param.required && !queryParamModalDraft[param.name]" class="schema-param-empty-hint">
              必填：请选择表单字段引用或输入固定值
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <div class="query-param-modal-footer">
          <n-button @click="cancelQueryParamModal">
            取消
          </n-button>
          <n-button type="primary" @click="confirmQueryParamModal">
            确认
          </n-button>
        </div>
      </template>
    </n-modal>
  </div>
</template>

<script>
import { useForgePropertyPanel } from './composables/useForgePropertyPanel'
import { forgePropertyPanelLocalComponents } from './forgePropertyPanelLocalComponents'
import SelectedBasicTab from './panels/SelectedBasicTab.vue'
import SelectedStyleTab from './panels/SelectedStyleTab.vue'
import SelectedCrudTab from './panels/SelectedCrudTab.vue'
import SelectedInteractionTab from './panels/SelectedInteractionTab.vue'
import SelectedPropertyDrawers from './panels/SelectedPropertyDrawers.vue'
import FormPropertyTabs from './panels/FormPropertyTabs.vue'
import SubTableInlineEditor from './panels/SubTableInlineEditor.vue'

export default {
  name: 'ForgePropertyPanel',
  components: {
    ...forgePropertyPanelLocalComponents,
    SelectedBasicTab,
    SelectedStyleTab,
    SelectedCrudTab,
    SelectedInteractionTab,
    SelectedPropertyDrawers,
    FormPropertyTabs,
    SubTableInlineEditor,
  },
  props: {
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
  initialFormTab: {
    type: String,
    default: 'basic',
  },
},
  emits: ['update:schema', 'update:selectedId', 'close', 'fieldAssetUpdated', 'updateFieldGeneration'],
  setup(props, { emit }) {
    return useForgePropertyPanel(props, emit)
  },
}
</script>

<style scoped src="./forge-property-panel-shell.css"></style>
<style scoped src="./forge-property-panel-fields.css"></style>
