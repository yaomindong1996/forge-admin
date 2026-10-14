<template>
  <BusinessObjectDesignerShell
    :embedded="embedded"
    :compact-embedded="embedded && activePanel === 'flow-app'"
    :active-panel="activePanel"
    :designer="designer"
    :loading="loading"
    :dirty="dirty"
    :confirm-dirty-switch="!fieldDraftDirty"
    :saving="saving"
    :publishing="publishing"
    :publish-disabled="publishDisabled"
    :preview-disabled="!objectId"
    :show-advanced="canAdvanced"
    :nav-panels="designerNavPanels"
    :show-preview="!isCodeAppDesigner"
    :show-publish="!isCodeAppDesigner"
    @save="handleSave"
    @preview="handlePreview"
    @publish="handlePublish"
    @back="handleBack"
    @refresh="loadDesigner"
    @open-runtime="openRuntime"
    @open-fields="handlePanelSwitch('fields')"
    @open-function-market="functionMarketVisible = true"
    @update:active-panel="handlePanelSwitch"
  >
    <BusinessTableMappingSummary
      v-if="objectId && !isCodeAppDesigner"
      ref="tableMappingSummaryRef"
      :object-id="objectId"
      :expanded="activePanel === 'fields' || activePanel === 'data-model' || activePanel === 'tree-model'"
      :model-schema="draft.modelSchema"
      @loaded="tableMapping = $event"
      @open-structure="handlePanelSwitch('fields')"
      @update:model-schema="handleModelSchemaUpdated"
      @dirty-change="handleDirtyChange"
      @apply-indexes="handleModelSchemaApplied"
      @apply-model-schema="handleModelSchemaApplied"
    />

    <section v-if="activePanel === 'basic'" class="basic-panel">
      <div class="panel-head">
        <div>
          <h2>基本信息</h2>
          <p>维护业务单元的名称、说明、默认标题字段和启停状态。</p>
        </div>
      </div>
      <n-form label-placement="top" :show-feedback="false" class="basic-form">
        <n-grid :cols="2" :x-gap="16" :y-gap="4" responsive="screen">
          <n-form-item-gi label="对象名称">
            <n-input v-model:value="draft.objectName" placeholder="例如：客户" @update:value="markDirty" />
          </n-form-item-gi>
          <n-form-item-gi label="默认标题字段">
            <n-select
              v-model:value="draft.displayField"
              :options="fieldOptions"
              clearable
              filterable
              placeholder="关联关系未单独配置时使用"
              @update:value="markDirty"
            />
          </n-form-item-gi>
          <n-form-item-gi label="对象图标">
            <IconSelector v-model="draft.icon" @update:model-value="markDirty" />
          </n-form-item-gi>
          <n-form-item-gi label="启用状态">
            <n-switch
              :value="draft.status !== 0"
              @update:value="updateStatus"
            />
          </n-form-item-gi>
          <n-form-item-gi :span="2" label="对象说明">
            <n-input
              v-model:value="draft.description"
              type="textarea"
              :rows="4"
              placeholder="描述对象的业务含义和使用场景"
              @update:value="markDirty"
            />
          </n-form-item-gi>
        </n-grid>
      </n-form>
    </section>

    <BusinessFieldManager
      v-else-if="activePanel === 'fields'"
      ref="fieldManagerRef"
      :object-id="objectId"
      :object-code="draft.objectCode || objectCode"
      :fields="draft.fields"
      :relations="draft.relations"
      :form-designer-schema="draft.formDesignerSchema"
      :developer-mode="developerMode"
      :table-mapping="tableMapping"
      @updated="handleFieldsUpdated"
      @dirty-change="handleFieldDirtyChange"
      @add-to-form="handleAddFieldToForm"
    />

    <section v-else-if="activePanel === 'data-model'" class="grouped-designer-panel">
      <BusinessRelationDesigner
        ref="relationDesignerRef"
        v-model:linkage-schema="draft.linkageSchema"
        :object-id="objectId"
        :suite-code="draft.suiteCode"
        :object-code="draft.objectCode"
        :object-name="draft.objectName"
        :fields="draft.fields"
        :designer-options="draft.designerOptions"
        :designer-actions="draft.designerOptions?.actions || []"
        model-only
        @updated="handleRelationsUpdated"
        @update:designer-actions="handleActionsUpdated"
        @fields-updated="handleFieldsUpdated"
        @dirty-change="handleDirtyChange"
      />
      <n-alert
        v-if="!embedded"
        class="process-migration-alert"
        type="info"
        title="流程与自动化配置已移至应用工作台"
        :bordered="false"
      >
        触发器、流程绑定和业务动作已统一为业务流程画布，请在应用工作台 → 业务流程中配置。
        <template #action>
          <n-button text type="primary" @click="openProcessWorkspace">
            前往应用工作台
          </n-button>
        </template>
      </n-alert>
      <ObjectProcessReadOnlyPanel
        :object-code="draft.objectCode || objectCode"
        :application-code="applicationCode"
      />
    </section>

    <BusinessFormDesigner
      v-else-if="activePanel === 'form'"
      ref="formDesignerRef"
      v-model="draft.pageSchema"
      v-model:form-designer-schema="draft.formDesignerSchema"
      v-model:view-schema="draft.viewSchema"
      v-model:linkage-schema="draft.linkageSchema"
      :object-id="objectId"
      :object-code="draft.objectCode"
      :object-name="draft.objectName"
      :application-code="applicationCode"
      :model-schema="draft.modelSchema"
      :fields="draft.fields"
      :relations="draft.relations"
      :actions="draft.designerOptions?.actions || []"
      :initial-property-tab="initialFormPropertyTab"
      :initial-canvas-view="initialFormCanvasView"
      @saved="handleLayoutSaved"
      @fields-updated="handleFieldsUpdated"
      @dirty-change="handleDirtyChange"
      @create-field="handlePanelSwitch('fields')"
      @open-relations="handlePanelSwitch('relations')"
      @update-field-generation="handleFieldGenerationUpdate"
    />

    <BusinessListDesigner
      v-else-if="activePanel === 'list'"
      ref="listDesignerRef"
      v-model="draft.pageSchema"
      v-model:view-schema="draft.viewSchema"
      :model-schema="draft.modelSchema"
      :object-id="objectId"
      :fields="draft.fields"
      :form-options="runtimeFormOptions"
      :application-pages="applicationPages"
      :designer-options="draft.designerOptions"
      :designer-actions="draft.designerOptions?.actions || []"
      @update:model-schema="handleListModelSchemaUpdate"
      @update:designer-actions="handleActionsUpdated"
      @saved="handleLayoutSaved"
      @dirty-change="handleDirtyChange"
    />

    <BusinessRelationDesigner
      v-else-if="activePanel === 'relations'"
      ref="relationDesignerRef"
      v-model:linkage-schema="draft.linkageSchema"
      :object-id="objectId"
      :suite-code="draft.suiteCode"
      :object-code="draft.objectCode"
      :object-name="draft.objectName"
      :fields="draft.fields"
      :designer-options="draft.designerOptions"
      :designer-actions="draft.designerOptions?.actions || []"
      model-only
      @updated="handleRelationsUpdated"
      @update:designer-actions="handleActionsUpdated"
      @fields-updated="handleFieldsUpdated"
      @dirty-change="handleDirtyChange"
    />

    <ObjectActionEditor
      v-else-if="activePanel === 'actions'"
      :actions="draft.designerOptions?.actions || []"
      :fields="draft.fields"
      :relations="draft.relations"
      :objects="workspaceObjects"
      :object-code="draft.objectCode"
      :config-key="draft.configKey"
      @save="handleActionsUpdated"
    />

    <BusinessFlowAppConfigPanel
      v-else-if="activePanel === 'flow-app'"
      ref="flowAppConfigRef"
      :object-id="objectId"
      :suite-code="draft.suiteCode"
      :object-code="draft.objectCode"
      :object-name="draft.objectName"
      :application-code="applicationCode"
      :fields="draft.fields"
      :initial-config="draft.documentConfig"
      :initial-section="embedded ? 'flow' : route.query.section"
      :code-app="isCodeAppDesigner"
      :compact="embedded"
      @saved="handleFlowAppSaved"
      @flow-context-change="emit('flowContextChange', $event)"
      @dirty-change="handleDirtyChange"
      @open-publish="handlePanelSwitch('publish')"
      @update-field-generation="handleFieldGenerationUpdate"
    />

    <BusinessPermissionFlowPanel
      v-else-if="activePanel === 'tree-model'"
      ref="treeModelRef"
      v-model:model-schema="draft.modelSchema"
      v-model:page-schema="draft.pageSchema"
      :fields="draft.fields"
      :object-name="draft.objectName"
      @dirty-change="handleDirtyChange"
    />

    <BusinessPublishChecklist
      v-else-if="activePanel === 'publish'"
      ref="publishChecklistRef"
      :object-id="objectId"
      :fields="draft.fields"
      :runtime-info="runtimeInfo"
      :publishing="publishing"
      @check-updated="handlePublishCheckUpdated"
      @fix="handleFixTarget"
      @open-runtime="openRuntime"
      @rolled-back="loadDesigner"
    />

    <BusinessAdvancedConfig
      v-else-if="activePanel === 'advanced'"
      v-model:developer-mode="developerMode"
      :draft="draft"
      :can-advanced="canAdvanced"
      @open-developer="openDeveloperPath"
    />
  </BusinessObjectDesignerShell>

  <FormulaFunctionMarket v-model:show="functionMarketVisible" />
</template>

<script>
import { objectDesignerLocalComponents } from './objectDesignerLocalComponents'
import { useObjectDesigner } from './composables/useObjectDesigner'

export default {
  name: 'ObjectDesigner',
  components: {
    ...objectDesignerLocalComponents,
  },
  props: {
  embedded: {
    type: Boolean,
    default: false,
  },
  embeddedObjectCode: {
    type: String,
    default: '',
  },
  embeddedObjectId: {
    type: [Number, String],
    default: null,
  },
  embeddedSuiteCode: {
    type: String,
    default: '',
  },
  initialPanel: {
    type: String,
    default: 'fields',
  },
  initialDetailTab: {
    type: String,
    default: 'form',
  },
  initialFormPropertyTab: {
    type: String,
    default: 'basic',
  },
  embeddedNavPanels: {
    type: Array,
    default: () => [],
  },
  applicationPages: {
    type: Array,
    default: () => [],
  },
},
  emits: ['close', 'saved', 'dirtyChange', 'flowContextChange'],
  setup(props, { emit, expose }) {
    return useObjectDesigner(props, emit, expose)
  },
}
</script>

<style scoped src="./objectDesigner.css"></style>
