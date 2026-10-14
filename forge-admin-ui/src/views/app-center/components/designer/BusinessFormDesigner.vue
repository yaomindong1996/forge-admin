<template>
  <div class="business-form-designer" :class="{ 'relation-object-active': !isPrimaryObjectActive, 'native-form-active': isPrimaryObjectActive && !useLegacyFormCreateDesigner }">
    <section class="form-canvas-region">
      <div v-if="!isPrimaryObjectActive || useLegacyFormCreateDesigner" class="designer-section-head">
        <div class="designer-section-main">
          <h3>{{ activeObjectTitle }}</h3>
          <p>{{ activeObjectDescription }}</p>
        </div>
        <div class="designer-head-actions">
          <div v-if="formObjectTabs.length > 1" class="object-switch-control">
            <span>表单范围</span>
            <n-radio-group v-model:value="activeObjectKey" size="small">
              <n-radio-button v-for="tab in formObjectTabs" :key="tab.key" :value="tab.key">
                {{ tab.label }}
              </n-radio-button>
            </n-radio-group>
          </div>
          <div v-if="useLegacyFormCreateDesigner" class="modal-type-control">
            <span>编辑打开方式</span>
            <n-select
              v-model:value="editFormOpenMode"
              size="small"
              :options="formOpenModeOptions"
              class="form-open-mode-select"
            />
          </div>
          <div v-if="isPrimaryObjectActive && useLegacyFormCreateDesigner" class="layout-columns-control">
            <span>表单列数</span>
            <n-radio-group v-model:value="formGridColumns" size="small">
              <n-radio-button :value="1">
                单列
              </n-radio-button>
              <n-radio-button :value="2">
                两列
              </n-radio-button>
              <n-radio-button :value="3">
                三列
              </n-radio-button>
            </n-radio-group>
          </div>
          <n-button
            v-if="isPrimaryObjectActive && useLegacyFormCreateDesigner"
            class="designer-head-compact-button"
            size="small"
            secondary
            @click="useLegacyFormCreateDesigner = false"
          >
            使用新版画布
          </n-button>
        </div>
      </div>

      <div class="form-builder-grid" :class="{ 'relation-mode': !isPrimaryObjectActive, 'has-relation-summary': isPrimaryObjectActive }">
        <template v-if="isPrimaryObjectActive">
          <section class="relation-overview-bar subtable-overview-bar">
            <div class="relation-overview-main">
              <strong>关联子表</strong>
              <span v-if="subTableSummaryRows.length">
                已配置 {{ subTableSummaryRows.length }} 个关联子表，子表数据随主表单一起加载和保存。
              </span>
              <span v-else>
                添加关联子表后可在主表单中同时录入主表与子表数据，如采购订单 + 明细行。
              </span>
            </div>
            <div v-if="subTableSummaryRows.length" class="relation-overview-tags">
              <n-tag
                v-for="row in subTableSummaryRows"
                :key="row.key"
                size="small"
                :bordered="false"
                class="subtable-summary-tag"
                @click="locateSubTableOnCanvas(row.componentId)"
              >
                {{ row.title }}
                <template v-if="row.fieldCount">
                  · {{ row.fieldCount }} 字段
                </template>
              </n-tag>
            </div>
            <n-space size="small">
              <n-button size="small" secondary @click="relationOverviewVisible = true">
                关系全景图
              </n-button>
              <n-button size="small" type="primary" @click="openChildTableSectionWizard">
                + 添加子表
              </n-button>
            </n-space>
          </section>
          <BusinessFormCreateDesigner
            v-if="useLegacyFormCreateDesigner"
            ref="formCreateDesignerRef"
            v-model="localFormDesignerSchema"
            :fields="primaryDesignFields"
            :object-code="objectCode"
            :object-name="objectName"
            @dirty-change="emit('dirtyChange', $event)"
          />
          <ForgeFormDesigner
            v-else
            ref="forgeFormDesignerRef"
            v-model="localFormDesignerSchema"
            :fields="primaryDesignFields"
            :object-code="objectCode"
            :object-name="objectName"
            :relations="relations"
            :actions="actions"
            :linkage-schema="linkageSchema"
            :initial-property-tab="initialPropertyTab"
            :initial-canvas-view="initialCanvasView"
            :extra-more-options="formDesignerMoreOptions"
            :enable-sections-view="false"
            :derive-sections-from-layout="true"
            @dirty-change="emit('dirtyChange', $event)"
            @update:linkage-schema="emit('update:linkageSchema', $event)"
            @field-asset-updated="handleFieldAssetUpdated"
            @update-field-generation="emit('updateFieldGeneration', $event)"
            @more-select="handleFormDesignerMoreSelect"
            @configure-bottom-action="handleConfigureBottomAction"
            @edit-sub-table-container="handleEditSubTableContainer"
          >
            <template #detail-settings>
              <BusinessDetailDesigner
                v-model="localSchema"
                v-model:view-schema="localViewSchema"
                settings-only
                :object-id="objectId"
                :model-schema="modelSchema"
                :fields="fields"
                :relations="relations"
                @dirty-change="emit('dirtyChange', $event)"
                @open-relations="emit('openRelations')"
              />
            </template>
          </ForgeFormDesigner>
        </template>

        <section v-else class="relation-object-workbench">
          <div class="relation-object-head">
            <div>
              <h4>{{ activeRelationRow?.title || '关联对象表单' }}</h4>
              <p>
                {{ activeRelationGroup?.fields?.length || 0 }} 个可配置字段，
                已选择 {{ activeRelationGroup?.selectedCount || 0 }} 个。
              </p>
            </div>
            <n-space size="small">
              <n-tag v-if="activeRelationRow?.inlineCreateEnabled" size="small" type="success" :bordered="false">
                新增表单
              </n-tag>
              <n-tag v-if="activeRelationRow?.inlineEditEnabled" size="small" type="info" :bordered="false">
                编辑表单
              </n-tag>
              <n-tag v-if="activeRelationRow?.showInDetail" size="small" :bordered="false">
                详情页
              </n-tag>
              <n-button size="small" secondary @click="$emit('openRelations')">
                配置关系
              </n-button>
            </n-space>
          </div>

          <template v-if="activeRelationGroup?.fields?.length">
            <div class="relation-object-layout">
              <section class="relation-object-canvas-shell">
                <div class="relation-canvas-toolbar">
                  <span>{{ activeRelationCanvasFields.length }} 个字段已放入关联表单</span>
                  <n-space size="small">
                    <n-button size="small" secondary @click="selectRelationGroupFields(activeRelationGroup)">
                      补齐字段
                    </n-button>
                    <n-button size="small" secondary :disabled="!activeRelationCanvasFields.length" @click="clearRelationGroupFields(activeRelationGroup)">
                      清空画布
                    </n-button>
                  </n-space>
                </div>

                <div class="relation-form-canvas">
                  <div v-if="activeRelationCanvasFields.length" class="relation-canvas-grid">
                    <article
                      v-for="field in activeRelationCanvasFields"
                      :key="field.field"
                      class="relation-canvas-field"
                    >
                      <div>
                        <strong>{{ field.label || field.sourceField || field.field }}</strong>
                        <span>{{ field.sourceField || field.field }} · {{ field.componentType || field.dataType || 'input' }}</span>
                      </div>
                      <div class="relation-order-actions">
                        <n-button
                          quaternary
                          circle
                          size="tiny"
                          :disabled="!canMoveRelationField(field, -1)"
                          @click="moveRelationField(field, -1)"
                        >
                          <template #icon>
                            <n-icon><ChevronUpOutline /></n-icon>
                          </template>
                        </n-button>
                        <n-button
                          quaternary
                          circle
                          size="tiny"
                          :disabled="!canMoveRelationField(field, 1)"
                          @click="moveRelationField(field, 1)"
                        >
                          <template #icon>
                            <n-icon><ChevronDownOutline /></n-icon>
                          </template>
                        </n-button>
                        <n-button quaternary size="tiny" type="error" @click="toggleRelationField(field, false)">
                          移除
                        </n-button>
                      </div>
                    </article>
                  </div>
                  <n-empty v-else description="请选择右侧字段生成关联表单画布" />
                </div>
              </section>

              <aside class="relation-field-shelf">
                <div class="relation-shelf-head">
                  <strong>关联字段库</strong>
                  <span>{{ activeRelationAvailableFields.length }} 个未使用</span>
                </div>
                <div class="relation-shelf-list">
                  <button
                    v-for="field in activeRelationAvailableFields"
                    :key="field.field"
                    type="button"
                    class="relation-shelf-field"
                    @click="toggleRelationField(field, true)"
                  >
                    <strong>{{ field.label || field.sourceField || field.field }}</strong>
                    <span>{{ field.sourceField || field.field }}</span>
                  </button>
                  <n-empty v-if="!activeRelationAvailableFields.length" size="small" description="字段已全部放入画布" />
                </div>
              </aside>
            </div>
          </template>
          <n-empty
            v-else
            description="未读取到关联对象字段，请先保存关系配置或刷新设计器"
          />
        </section>
      </div>
    </section>

    <aside v-if="isPrimaryObjectActive && useLegacyFormCreateDesigner" class="field-shelf">
      <div class="shelf-head">
        <div>
          <h3>字段库</h3>
          <p>未使用字段可拖入画布，已使用字段会标记。</p>
        </div>
        <n-button size="small" secondary @click="$emit('createField')">
          新增字段
        </n-button>
      </div>

      <div class="field-state-tabs">
        <button type="button" :class="{ active: shelfTab === 'unused' }" @click="shelfTab = 'unused'">
          未使用 {{ unusedFields.length }}
        </button>
        <button type="button" :class="{ active: shelfTab === 'used' }" @click="shelfTab = 'used'">
          已使用 {{ usedFields.length }}
        </button>
        <button type="button" :class="{ active: shelfTab === 'system' }" @click="shelfTab = 'system'">
          系统 {{ systemFields.length }}
        </button>
      </div>

      <div class="shelf-list">
        <button
          v-for="field in visibleShelfFields"
          :key="field.field"
          type="button"
          class="shelf-field"
          :disabled="isReadonlySystemField(field) || usedFieldSet.has(field.field)"
          @click="appendField(field)"
        >
          <strong>{{ field.label || field.field }}</strong>
          <span>{{ field.field }} · {{ field.componentType || field.dataType || 'input' }}</span>
          <em v-if="usedFieldSet.has(field.field)">已使用</em>
          <em v-else-if="isReadonlySystemField(field)">系统字段</em>
        </button>
        <n-empty v-if="!visibleShelfFields.length" description="当前分组没有字段" />
      </div>
    </aside>

    <ChildTableSectionWizard
      v-model:show="childTableWizardVisible"
      :relations="relations"
      :model-refs="pageModelRefs"
      :model-value="childTableWizardValue"
      @confirm="handleChildTableSectionConfirm"
    />
    <RelationOverviewModal
      v-model:show="relationOverviewVisible"
      :object-code="objectCode"
      :object-name="objectName"
      :fields="fields"
      :relations="relations"
    />
    <ButtonActionConfig
      v-model:show="buttonActionConfigVisible"
      :model-value="buttonActionValue"
      :application-code="applicationCode"
      :object-code="objectCode"
      :pages="buttonTargetPages"
      @confirm="handleButtonActionConfirm"
    />
  </div>
</template>

<script>
import { businessFormDesignerLocalComponents } from './businessFormDesignerLocalComponents'
import { useBusinessFormDesigner } from './composables/useBusinessFormDesigner'

export default {
  name: 'BusinessFormDesigner',
  components: {
    ...businessFormDesignerLocalComponents,
  },
  props: {
  objectId: {
    type: [Number, String],
    default: null,
  },
  objectCode: {
    type: String,
    default: '',
  },
  objectName: {
    type: String,
    default: '',
  },
  applicationCode: {
    type: String,
    default: '',
  },
  modelValue: {
    type: Object,
    default: null,
  },
  formDesignerSchema: {
    type: Object,
    default: null,
  },
  viewSchema: {
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
  initialPropertyTab: {
    type: String,
    default: 'basic',
  },
  initialCanvasView: {
    type: String,
    default: 'layout',
  },
},
  emits: ['update:modelValue', 'update:formDesignerSchema', 'update:viewSchema', 'update:linkageSchema', 'saved', 'fieldsUpdated', 'dirtyChange', 'createField', 'openRelations', 'updateFieldGeneration'],
  setup(props, { emit, expose }) {
    return useBusinessFormDesigner(props, emit, expose)
  },
}
</script>

<style scoped src="./businessFormDesigner.css"></style>
