<template>
  <div class="business-form-designer" :class="{ 'relation-object-active': !isPrimaryObjectActive }">
    <section class="form-canvas-region">
      <div class="designer-section-head">
        <div>
          <h3>{{ activeObjectTitle }}</h3>
          <p>{{ activeObjectDescription }}</p>
        </div>
        <div class="designer-head-actions">
          <div v-if="formObjectTabs.length > 1" class="object-switch-control">
            <span>设计对象</span>
            <n-radio-group v-model:value="activeObjectKey" size="small">
              <n-radio-button v-for="tab in formObjectTabs" :key="tab.key" :value="tab.key">
                {{ tab.label }}
              </n-radio-button>
            </n-radio-group>
          </div>
          <div class="modal-type-control">
            <span>编辑打开方式</span>
            <n-radio-group v-model:value="editModalType" size="small">
              <n-radio-button value="modal">
                弹出框
              </n-radio-button>
              <n-radio-button value="drawer">
                抽屉
              </n-radio-button>
            </n-radio-group>
          </div>
          <n-button v-if="isPrimaryObjectActive" size="small" secondary :disabled="!unusedFields.length" @click="appendAllUnusedFields">
            补齐未使用字段
          </n-button>
          <n-button size="small" type="primary" :loading="saving" @click="saveLayout">
            保存表单
          </n-button>
        </div>
      </div>

      <div class="form-builder-grid" :class="{ 'relation-mode': !isPrimaryObjectActive }">
        <template v-if="isPrimaryObjectActive">
          <BusinessFormCreateDesigner
            ref="formCreateDesignerRef"
            v-model="localFormDesignerSchema"
            :fields="primaryDesignFields"
            :object-code="objectCode"
            :object-name="objectName"
            @save="handleFormDesignerSave"
            @dirty-change="emit('dirtyChange', $event)"
          />
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

    <aside v-if="isPrimaryObjectActive" class="field-shelf">
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
  </div>
</template>

<script setup>
import { ChevronDownOutline, ChevronUpOutline } from '@vicons/ionicons5'
import { useMessage } from 'naive-ui'
import { computed, ref, watch } from 'vue'
import { saveBusinessObjectDesigner, saveBusinessObjectFormLayout } from '@/api/business-app'
import { cloneSchema, isSameSchema } from '@/components/lowcode-builder/model/model-schema'
import {
  buildPageDesignModelSchema,
  createDefaultPageSchema,
  createPageModelRef,
  isReadonlySystemField,
  syncPageSchemaWithModel,
} from '@/components/lowcode-builder/page/page-schema'
import BusinessFormCreateDesigner from './BusinessFormCreateDesigner.vue'
import { buildAutoFieldAssets } from './form-first/autoFieldRegistry'
import { extractForgeSchemaFieldRefs, forgeSchemaToFormCreate } from './form-first/forgeToFormCreate'

const props = defineProps({
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
  modelValue: {
    type: Object,
    default: null,
  },
  formDesignerSchema: {
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
})

const emit = defineEmits(['update:modelValue', 'update:formDesignerSchema', 'saved', 'fieldsUpdated', 'dirtyChange', 'createField', 'openRelations'])

const message = useMessage()
const saving = ref(false)
const shelfTab = ref('unused')
const activeObjectKey = ref('primary')
const formCreateDesignerRef = ref(null)

const baseModelSchema = computed(() => {
  const modelFields = props.modelSchema?.fields || []
  return {
    ...(props.modelSchema || {}),
    fields: modelFields.length ? modelFields : props.fields.map(toPageField),
  }
})

const localSchema = ref(resolveSchema(props.modelValue, resolveDesignModelSchema(props.modelValue, baseModelSchema.value)))
const localFormDesignerSchema = ref(cloneSchema(props.formDesignerSchema || null))
const effectiveModelSchema = computed(() => resolveDesignModelSchema(localSchema.value, baseModelSchema.value))
const designFields = computed(() => effectiveModelSchema.value.fields || [])
const editZone = computed(() => localSchema.value.zones?.find(zone => zone.zoneKey === 'edit') || null)
const pageModelRefs = computed(() => effectiveModelSchema.value.pageModelRefs || [])
const primaryModelCode = computed(() => pageModelRefs.value.find(ref => ref?.primary)?.modelCode || '')
const primaryDesignFields = computed(() => designFields.value.filter(field => !isRelationField(field)))
const relationFields = computed(() => designFields.value.filter(field => isRelationField(field)))
const primaryFieldSet = computed(() => new Set(primaryDesignFields.value.map(field => field.field)))
const relationFieldSet = computed(() => new Set(relationFields.value.map(field => field.field)))
const usedFieldSet = computed(() => new Set(extractForgeSchemaFieldRefs(localFormDesignerSchema.value || {}).filter(ref => primaryFieldSet.value.has(ref))))
const selectedRelationFieldRefs = computed(() => (editZone.value?.fieldRefs || []).filter(ref => relationFieldSet.value.has(ref)))
const selectedRelationFieldSet = computed(() => new Set(selectedRelationFieldRefs.value))
const businessFields = computed(() => primaryDesignFields.value.filter(field => !isReadonlySystemField(field)))
const primaryBusinessFields = computed(() => primaryDesignFields.value.filter(field => !isReadonlySystemField(field)))
const systemFields = computed(() => primaryDesignFields.value.filter(field => isReadonlySystemField(field)))
const usedFields = computed(() => businessFields.value.filter(field => usedFieldSet.value.has(field.field)))
const unusedFields = computed(() => businessFields.value.filter(field => !usedFieldSet.value.has(field.field)))
const editModalType = computed({
  get: () => editZone.value?.props?.modalType || 'modal',
  set: value => updateEditZoneProps({ modalType: value || 'modal' }),
})
const relationFieldGroups = computed(() => {
  return pageModelRefs.value
    .filter(ref => ref && !ref.primary)
    .map((ref) => {
      const fields = sortRelationGroupFields(relationFields.value.filter(field => field.modelCode === ref.modelCode && isRelationFieldAllowed(field, ref)))
      const selectedCount = fields.filter(field => selectedRelationFieldSet.value.has(field.field)).length
      const props = ref.props || {}
      const aliases = buildRelationAliases(ref)
      return {
        key: aliases[0] || ref.modelCode || ref.modelName,
        modelCode: ref.modelCode || '',
        aliases,
        title: props.tabTitle || props.relationName || ref.modelName || ref.modelCode || '关联对象',
        fields,
        selectedCount,
        showInCreate: booleanFlag(props.inlineCreateEnabled),
        showInEdit: booleanFlag(props.inlineEditEnabled),
        showInDetail: props.showInDetail !== false && props.showInDetail !== 'false',
      }
    })
})
const relationFormRows = computed(() => normalizeRelationFormRows(props.relations || [], relationFieldGroups.value))
const formObjectTabs = computed(() => [
  {
    key: 'primary',
    label: props.modelSchema?.businessName || props.modelSchema?.object?.name || '当前对象',
  },
  ...relationFormRows.value.map(row => ({
    key: row.key,
    label: row.title,
  })),
])
const isPrimaryObjectActive = computed(() => activeObjectKey.value === 'primary')
const activeRelationRow = computed(() => relationFormRows.value.find(row => row.key === activeObjectKey.value) || null)
const activeRelationGroup = computed(() => activeRelationRow.value?.group || null)
const activeRelationCanvasFields = computed(() => {
  const group = activeRelationGroup.value
  if (!group?.fields?.length)
    return []
  return group.fields.filter(field => selectedRelationFieldSet.value.has(field.field))
})
const activeRelationAvailableFields = computed(() => {
  const group = activeRelationGroup.value
  if (!group?.fields?.length)
    return []
  return group.fields.filter(field => !selectedRelationFieldSet.value.has(field.field))
})
const activeObjectTitle = computed(() => {
  if (isPrimaryObjectActive.value)
    return '主表单画布'
  return activeRelationRow.value?.title || '关联对象表单'
})
const activeObjectDescription = computed(() => {
  if (isPrimaryObjectActive.value)
    return '拖拽右侧字段到画布，新增、编辑和查看详情共用这一套主表布局。'
  return '选择关联对象在新增、编辑或详情中展示的字段，发布后同步到内嵌关联表单。'
})
const visibleShelfFields = computed(() => {
  if (shelfTab.value === 'used')
    return usedFields.value
  if (shelfTab.value === 'system')
    return systemFields.value
  return unusedFields.value
})

watch(
  () => props.modelValue,
  (value) => {
    const next = resolveSchema(value, resolveDesignModelSchema(value, baseModelSchema.value))
    if (!isSameSchema(next, localSchema.value))
      localSchema.value = next
  },
  { deep: true },
)

watch(
  () => props.formDesignerSchema,
  (value) => {
    const next = cloneSchema(value || null)
    if (!isSameSchema(next, localFormDesignerSchema.value))
      localFormDesignerSchema.value = next
  },
  { deep: true },
)

watch(
  effectiveModelSchema,
  (value) => {
    const next = syncPageSchemaWithModel(localSchema.value, value)
    if (!isSameSchema(next, localSchema.value))
      localSchema.value = next
  },
  { deep: true },
)

watch(
  localFormDesignerSchema,
  (value) => {
    emit('update:formDesignerSchema', cloneSchema(value || null))
    syncFormDesignerSchemaToPageSchema(value)
  },
  { deep: true },
)

watch(
  localSchema,
  (value) => {
    if (!isSameSchema(value, props.modelValue)) {
      emit('update:modelValue', cloneSchema(value))
      emit('dirtyChange', true)
    }
  },
  { deep: true },
)

watch(formObjectTabs, (tabs) => {
  if (!tabs.some(tab => tab.key === activeObjectKey.value))
    activeObjectKey.value = 'primary'
}, { deep: true })

function handleFormDesignerSave(schema) {
  localFormDesignerSchema.value = cloneSchema(schema || localFormDesignerSchema.value)
  syncFormDesignerSchemaToPageSchema(localFormDesignerSchema.value)
  message.success('表单配置已应用')
}

function syncFormDesignerSchemaToPageSchema(schema, fields = primaryDesignFields.value) {
  if (!editZone.value || !schema)
    return
  const fieldSet = new Set((fields || []).map(field => field.field || field.fieldCode).filter(Boolean))
  const { rules, options } = forgeSchemaToFormCreate({
    schema,
    fields,
  })
  const fieldRefs = extractForgeSchemaFieldRefs(schema).filter(ref => fieldSet.has(ref))
  replaceZone({
    ...editZone.value,
    fieldRefs: mergeUniqueRefs(fieldRefs, selectedRelationFieldRefs.value),
    props: {
      ...(editZone.value.props || {}),
      formCreateRule: rules,
      formCreateOptions: options,
    },
  })
}

function appendField(field) {
  if (isReadonlySystemField(field) || usedFieldSet.value.has(field.field))
    return
  formCreateDesignerRef.value?.appendField?.(field)
}

function appendAllUnusedFields() {
  unusedFields.value.forEach(appendField)
}

function updateEditZoneProps(patch = {}) {
  if (!editZone.value)
    return
  replaceZone({
    ...editZone.value,
    props: {
      ...(editZone.value.props || {}),
      ...patch,
    },
  })
}

function toggleRelationField(field, checked) {
  if (!field?.field || !editZone.value)
    return
  const selected = new Set(selectedRelationFieldRefs.value)
  if (checked)
    selected.add(field.field)
  else
    selected.delete(field.field)
  replaceZone(normalizeEditZoneFieldRefs(editZone.value, Array.from(selected), true))
}

function selectRelationGroupFields(group) {
  if (!editZone.value)
    return
  const selected = new Set(selectedRelationFieldRefs.value)
  group.fields.forEach(field => selected.add(field.field))
  replaceZone(normalizeEditZoneFieldRefs(editZone.value, Array.from(selected), true))
}

function clearRelationGroupFields(group) {
  if (!editZone.value)
    return
  const removeRefs = new Set(group.fields.map(field => field.field))
  const selected = selectedRelationFieldRefs.value.filter(ref => !removeRefs.has(ref))
  replaceZone(normalizeEditZoneFieldRefs(editZone.value, selected, true))
}

function canMoveRelationField(field, direction) {
  const groupRefs = selectedRelationRefsByModel(field?.modelCode)
  const index = groupRefs.indexOf(field?.field)
  const nextIndex = index + direction
  return index >= 0 && nextIndex >= 0 && nextIndex < groupRefs.length
}

function moveRelationField(field, direction) {
  if (!field?.field || !editZone.value || !canMoveRelationField(field, direction))
    return
  const groupRefSet = new Set(relationFields.value
    .filter(item => item.modelCode === field.modelCode)
    .map(item => item.field))
  const groupRefs = selectedRelationRefsByModel(field.modelCode)
  const from = groupRefs.indexOf(field.field)
  const to = from + direction
  const [item] = groupRefs.splice(from, 1)
  groupRefs.splice(to, 0, item)
  let groupIndex = 0
  const nextRefs = selectedRelationFieldRefs.value.map((ref) => {
    if (!groupRefSet.has(ref))
      return ref
    return groupRefs[groupIndex++] || ref
  })
  replaceZone(normalizeEditZoneFieldRefs(editZone.value, nextRefs, true))
}

function selectedRelationRefsByModel(modelCode) {
  const groupRefSet = new Set(relationFields.value
    .filter(field => field.modelCode === modelCode)
    .map(field => field.field))
  return selectedRelationFieldRefs.value.filter(ref => groupRefSet.has(ref))
}

function replaceZone(zone) {
  if (!zone)
    return
  localSchema.value = {
    ...localSchema.value,
    zones: (localSchema.value.zones || []).map(item => item.zoneKey === zone.zoneKey ? zone : item),
  }
}

function normalizeEditZoneFieldRefs(zone, relationRefs = Array.from(selectedRelationFieldSet.value), relationSelectionTouched = false) {
  return {
    ...zone,
    fieldRefs: [
      ...resolvePrimaryFieldRefs(zone),
      ...orderRelationRefs(relationRefs),
    ],
    props: {
      ...(zone.props || {}),
      ...(relationSelectionTouched ? { relationFieldSelectionMode: 'CUSTOM' } : {}),
    },
  }
}

function resolvePrimaryFieldRefs(zone) {
  const canvasRefs = resolveCanvasFieldRefs(zone?.props?.canvas?.items || []).filter(ref => primaryFieldSet.value.has(ref))
  if (canvasRefs.length)
    return canvasRefs
  const explicitRefs = (zone?.fieldRefs || []).filter(ref => primaryFieldSet.value.has(ref))
  if (explicitRefs.length)
    return explicitRefs
  return (editZone.value?.fieldRefs || []).filter(ref => primaryFieldSet.value.has(ref))
}

function orderRelationRefs(refs = []) {
  const seen = new Set()
  return refs.filter((ref) => {
    if (!relationFieldSet.value.has(ref) || seen.has(ref))
      return false
    seen.add(ref)
    return true
  })
}

function sortRelationGroupFields(fields = []) {
  const order = new Map(selectedRelationFieldRefs.value.map((ref, index) => [ref, index]))
  return fields.map((field, index) => ({ field, index }))
    .sort((left, right) => {
      const leftOrder = order.has(left.field.field) ? order.get(left.field.field) : Number.MAX_SAFE_INTEGER
      const rightOrder = order.has(right.field.field) ? order.get(right.field.field) : Number.MAX_SAFE_INTEGER
      if (leftOrder !== rightOrder)
        return leftOrder - rightOrder
      return left.index - right.index
    })
    .map(item => item.field)
}

function buildRelationAliases(ref = {}) {
  const props = ref.props || {}
  return [
    props.targetObjectCode,
    props.businessObjectCode,
    props.objectCode,
    ref.objectCode,
    ref.businessObjectCode,
    ref.modelCode,
    ref.modelName,
  ]
    .map(normalizeRelationKey)
    .filter(Boolean)
    .filter((value, index, array) => array.indexOf(value) === index)
}

function isRelationField(field = {}) {
  const sourceField = field.sourceField || field.field
  return Boolean(field.modelCode)
    && field.field !== sourceField
    && (!primaryModelCode.value || field.modelCode !== primaryModelCode.value)
}

function isRelationFieldAllowed(field = {}, ref = {}) {
  if (isReadonlySystemField(field) || field.formVisible === false)
    return false
  const relation = Array.isArray(ref.relations) ? ref.relations[0] : null
  return !matchesSourceField(field, relation?.sourceField)
}

function matchesSourceField(field = {}, sourceField) {
  if (!sourceField)
    return false
  const source = field.sourceField || field.field
  return source === sourceField
    || field.field === sourceField
    || field.columnName === sourceField
    || field.columnName === camelToSnake(sourceField)
}

function camelToSnake(value) {
  return String(value || '').replace(/([a-z0-9])([A-Z])/g, '$1_$2').toLowerCase()
}

function booleanFlag(value) {
  return value === true || value === 'true'
}

function normalizeRelationFormRows(relations = [], groups = []) {
  return (relations || [])
    .map((relation, index) => {
      const config = parseRelationConfig(relation.relationConfig)
      const targetCode = relation.targetObjectCode || ''
      const group = findRelationGroup(targetCode, groups)
      const relationKey = normalizeRelationKey(targetCode) || 'relation'
      const key = `${relationKey}_${relation.id || relation.clientKey || index}`
      return {
        key,
        modelCode: targetCode,
        title: config.detailTabTitle || config.detailTab || relation.detailTabTitle || relation.relationName || relation.targetObjectName || targetCode || `关联表单${index + 1}`,
        fieldCount: group?.fields?.length || 0,
        selectedCount: group?.selectedCount || 0,
        group,
        showInDetail: config.showInDetail !== false && relation.showInDetail !== false,
        inlineCreateEnabled: resolveRelationToggle(relation, config, 'inlineCreateEnabled', true),
        inlineEditEnabled: resolveRelationToggle(relation, config, 'inlineEditEnabled', false),
        status: relation.status ?? 1,
      }
    })
    .filter(relation => relation.status !== 0 && (relation.showInDetail || relation.inlineCreateEnabled || relation.inlineEditEnabled))
}

function findRelationGroup(targetCode, groups = []) {
  const targetKey = normalizeRelationKey(targetCode)
  if (!targetKey)
    return null
  return groups.find((group) => {
    const aliases = Array.isArray(group.aliases) ? group.aliases : [normalizeRelationKey(group.key), normalizeRelationKey(group.modelCode)]
    return aliases.some(alias => relationKeyMatches(alias, targetKey))
  }) || null
}

function relationKeyMatches(left, right) {
  if (!left || !right)
    return false
  return left === right || left.endsWith(`_${right}`) || right.endsWith(`_${left}`)
}

function normalizeRelationKey(value) {
  return String(value || '')
    .replace(/([a-z0-9])([A-Z])/g, '$1_$2')
    .replace(/\W+/g, '_')
    .replace(/_+/g, '_')
    .replace(/^_|_$/g, '')
    .toLowerCase()
}

function parseRelationConfig(value) {
  if (!value)
    return {}
  try {
    const parsed = JSON.parse(value)
    return parsed && typeof parsed === 'object' ? parsed : {}
  }
  catch {
    return {}
  }
}

function canInlineRelation(relation = {}) {
  return ['CHILD_LIST', 'DETAIL'].includes(String(relation.relationType || '').toUpperCase())
}

function resolveRelationToggle(relation = {}, config = {}, key, defaultValue) {
  if (!canInlineRelation(relation))
    return false
  if (config[key] === true || config[key] === 'true')
    return true
  if (config[key] === false || config[key] === 'false')
    return false
  if (relation[key] === true)
    return true
  if (relation[key] === false)
    return false
  return defaultValue
}

async function saveLayout() {
  if (!props.objectId)
    return
  const formSchema = formCreateDesignerRef.value?.flushDesigner?.() || localFormDesignerSchema.value
  const { fields: nextFields, createdFields } = buildAutoFieldAssets(formSchema, primaryBusinessFields.value)
  const nextModelSchema = {
    ...effectiveModelSchema.value,
    fields: [
      ...systemFields.value,
      ...nextFields.map(toPageField),
      ...relationFields.value,
    ],
  }
  if (formSchema)
    syncFormDesignerSchemaToPageSchema(formSchema, nextModelSchema.fields)
  const schema = syncPageSchemaWithModel(localSchema.value, nextModelSchema)
  saving.value = true
  try {
    if (createdFields.length || formSchema) {
      await saveBusinessObjectDesigner(props.objectId, {
        fields: nextFields.map(toBusinessFieldPayload),
        formDesignerSchema: cloneSchema(formSchema || localFormDesignerSchema.value || {}),
      })
    }
    await saveBusinessObjectFormLayout(props.objectId, {
      layoutKey: 'form',
      layoutName: '表单布局',
      layoutType: schema.layoutType,
      pageSchema: cloneSchema(schema),
      zones: schema.zones?.filter(zone => zone.zoneKey === 'edit') || [],
      settings: {},
    })
    localSchema.value = schema
    emit('saved', cloneSchema(schema))
    if (createdFields.length)
      emit('fieldsUpdated', nextFields)
    emit('dirtyChange', false)
    message.success(createdFields.length ? `表单布局已保存，已自动创建 ${createdFields.length} 个字段` : '表单布局已保存')
  }
  finally {
    saving.value = false
  }
}

function resolveSchema(pageSchema, modelSchema) {
  return syncPageSchemaWithModel(
    cloneSchema(pageSchema || createDefaultPageSchema(modelSchema)),
    modelSchema,
  )
}

function resolveDesignModelSchema(pageSchema, modelSchema) {
  const refs = mergePrimaryModelRef(pageSchema?.modelRefs || [], modelSchema || {})
  return buildPageDesignModelSchema(modelSchema || {}, refs)
}

function mergePrimaryModelRef(modelRefs, modelSchema) {
  if (!Array.isArray(modelRefs) || !modelRefs.length)
    return []
  const primaryRef = createPageModelRef({ modelSchema }, { primary: true })
  const refs = modelRefs.map(ref => ref?.primary
    ? {
        ...ref,
        modelCode: primaryRef.modelCode || ref.modelCode,
        modelName: primaryRef.modelName || ref.modelName,
        tableName: primaryRef.tableName || ref.tableName,
        relations: primaryRef.relations?.length ? primaryRef.relations : ref.relations,
        fields: primaryRef.fields,
      }
    : ref)
  if (!refs.some(ref => ref?.primary))
    refs.unshift(primaryRef)
  return refs
}

function resolveCanvasFieldRefs(items) {
  const refs = new Set()
  items.forEach((item) => {
    if (item.fieldRef)
      refs.add(item.fieldRef)
    ;(item.fieldRefs || item.props?.fieldRefs || []).forEach(ref => refs.add(ref))
  })
  return Array.from(refs)
}

function mergeUniqueRefs(...groups) {
  return Array.from(new Set(groups.flat().filter(Boolean)))
}

function toPageField(field) {
  return {
    ...field,
    field: field.field || field.fieldCode,
    label: field.label || field.fieldName || field.fieldCode,
    comment: field.remark || field.fieldName,
    columnName: field.columnName,
    dataType: field.dataType,
    componentType: field.componentType,
    dictType: field.dictType,
    required: field.required,
    systemField: field.systemField,
    readonly: field.readonly,
    searchable: field.searchable,
    listVisible: field.listVisible,
    formVisible: field.formVisible,
    fieldStatus: field.fieldStatus,
    basicProps: { ...(field.basicProps || {}) },
    advancedProps: { ...(field.advancedProps || {}) },
  }
}

function toBusinessFieldPayload(field = {}) {
  return {
    fieldName: field.fieldName || field.label || field.field,
    fieldCode: field.fieldCode || field.field,
    columnName: field.columnName,
    fieldType: field.fieldType || field.businessFieldType || 'TEXT',
    dataType: field.dataType,
    length: field.length,
    precision: field.precision,
    required: field.required,
    defaultValue: field.defaultValue,
    searchable: field.searchable,
    listVisible: field.listVisible,
    formVisible: field.formVisible,
    importable: field.importable,
    exportable: field.exportable,
    componentType: field.componentType,
    queryType: field.queryType,
    dictType: field.dictType,
    sensitiveType: field.sensitiveType,
    encryptAlgorithm: field.encryptAlgorithm,
    sortable: field.sortable,
    systemField: false,
    readonly: field.readonly,
    fieldStatus: field.fieldStatus || 'ENABLED',
    referenceObjectCode: field.referenceObjectCode,
    referenceDisplayField: field.referenceDisplayField,
    placeholder: field.placeholder || field.basicProps?.placeholder || '',
    remark: field.remark,
    sortOrder: field.sortOrder,
    fieldBinding: { ...(field.fieldBinding || field.basicProps?.fieldBinding || {}) },
    basicProps: { ...(field.basicProps || {}) },
    advancedProps: { ...(field.advancedProps || {}) },
  }
}

defineExpose({
  saveLayout,
  appendFieldToForm: appendField,
})
</script>

<style scoped>
.business-form-designer {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 280px;
  min-height: calc(100vh - 106px);
  container-type: inline-size;
}

.business-form-designer.relation-object-active {
  grid-template-columns: minmax(0, 1fr);
}

.form-canvas-region {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  min-width: 0;
  border-right: 1px solid #e5e7eb;
}

.designer-section-head,
.shelf-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid #e5e7eb;
  padding: 14px 16px;
}

.designer-section-head h3,
.shelf-head h3 {
  margin: 0;
  color: #111827;
  font-size: 15px;
  letter-spacing: 0;
}

.designer-section-head p,
.shelf-head p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 12px;
}

.designer-head-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

.object-switch-control,
.modal-type-control {
  display: flex;
  align-items: center;
  gap: 8px;
}

.object-switch-control span,
.modal-type-control span {
  color: #64748b;
  font-size: 12px;
  white-space: nowrap;
}

.form-builder-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 14px;
  min-height: calc(100vh - 168px);
  background: #f8fafc;
  padding: 14px;
}

.form-builder-grid.relation-mode {
  grid-template-columns: minmax(0, 1fr);
}

.relation-object-workbench {
  display: grid;
  align-content: start;
  gap: 12px;
  min-height: calc(100vh - 196px);
}

.relation-object-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 14px;
}

.relation-object-head h4 {
  margin: 0;
  color: #111827;
  font-size: 15px;
  letter-spacing: 0;
}

.relation-object-head p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 12px;
}

.field-shelf {
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr);
  min-width: 0;
  background: #fbfcfe;
}

.relation-object-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 280px;
  gap: 14px;
  min-height: 520px;
}

.relation-object-canvas-shell,
.relation-field-shelf {
  min-width: 0;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

.relation-object-canvas-shell {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
}

.relation-canvas-toolbar,
.relation-shelf-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  border-bottom: 1px solid #e5e7eb;
  min-height: 44px;
  padding: 10px 12px;
}

.relation-canvas-toolbar span,
.relation-shelf-head span {
  color: #64748b;
  font-size: 12px;
}

.relation-shelf-head strong {
  color: #111827;
  font-size: 13px;
}

.relation-form-canvas {
  min-height: 460px;
  overflow: auto;
  background: linear-gradient(#f1f5f9 1px, transparent 1px), linear-gradient(90deg, #f1f5f9 1px, transparent 1px);
  background-color: #f8fafc;
  background-size: 24px 24px;
  padding: 18px;
}

.relation-canvas-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(260px, 1fr));
  gap: 12px;
  max-width: 900px;
}

.relation-canvas-field {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  background: #fff;
  min-height: 64px;
  padding: 10px 12px;
}

.relation-canvas-field strong,
.relation-shelf-field strong {
  display: block;
  overflow: hidden;
  color: #111827;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.relation-canvas-field span,
.relation-shelf-field span {
  display: block;
  overflow: hidden;
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.relation-field-shelf {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
}

.relation-shelf-list {
  display: grid;
  align-content: start;
  gap: 8px;
  min-height: 0;
  overflow: auto;
  padding: 10px;
}

.relation-shelf-field {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  padding: 10px;
  text-align: left;
}

.relation-shelf-field:hover {
  border-color: #2563eb;
  background: #f8fbff;
}

.relation-form-panel {
  display: grid;
  gap: 8px;
  border-bottom: 1px solid #e5e7eb;
  padding: 10px;
}

.relation-form-panel header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.relation-form-panel header div,
.relation-form-card {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.relation-form-panel strong,
.relation-form-card strong {
  overflow: hidden;
  color: #111827;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.relation-form-panel span,
.relation-form-card span {
  overflow: hidden;
  color: #64748b;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.relation-form-list {
  display: grid;
  gap: 8px;
}

.relation-form-card {
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background: #f8fbff;
  cursor: pointer;
  padding: 9px 10px;
  text-align: left;
}

.relation-form-card:hover {
  border-color: #60a5fa;
  background: #eff6ff;
}

.relation-form-card div {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
}

.relation-form-card em {
  border-radius: 4px;
  background: #e0f2fe;
  color: #0369a1;
  font-size: 11px;
  font-style: normal;
  line-height: 20px;
  padding: 0 6px;
}

.field-state-tabs {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 6px;
  border-bottom: 1px solid #e5e7eb;
  padding: 10px;
}

.field-state-tabs button {
  min-height: 30px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
  color: #475569;
  cursor: pointer;
  font-size: 12px;
}

.field-state-tabs button.active {
  border-color: #2563eb;
  background: #eaf2ff;
  color: #1d4ed8;
}

.shelf-list {
  display: grid;
  align-content: start;
  gap: 8px;
  min-height: 0;
  overflow: auto;
  padding: 10px;
}

.relation-order-actions {
  display: flex;
  align-items: center;
  gap: 2px;
}

.shelf-field {
  position: relative;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  cursor: grab;
  text-align: left;
  padding: 10px;
}

.shelf-field:disabled {
  cursor: default;
  opacity: 0.72;
}

.shelf-field strong {
  display: block;
  overflow: hidden;
  color: #111827;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.shelf-field span {
  display: block;
  overflow: hidden;
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.shelf-field em {
  display: inline-flex;
  margin-top: 8px;
  border-radius: 4px;
  background: #f1f5f9;
  color: #475569;
  font-size: 11px;
  font-style: normal;
  line-height: 20px;
  padding: 0 6px;
}

@media (max-width: 1500px) {
  .business-form-designer {
    grid-template-columns: 1fr;
  }

  .form-canvas-region {
    border-right: 0;
    border-bottom: 1px solid #e5e7eb;
  }

  .field-shelf {
    max-height: 300px;
  }

  .shelf-list {
    grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  }

  .relation-object-layout {
    grid-template-columns: 1fr;
  }

  .relation-shelf-list {
    grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  }
}

@container (max-width: 860px) {
  .form-builder-grid {
    grid-template-columns: 1fr;
  }

  .relation-canvas-grid {
    grid-template-columns: 1fr;
  }

  .designer-section-head,
  .shelf-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .designer-head-actions {
    justify-content: flex-start;
  }
}

@container (max-width: 640px) {
  .designer-head-actions {
    flex-wrap: wrap;
  }
}
</style>
