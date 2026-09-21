<script setup>
/**
 * ApproverConfig — 审批人节点完整配置
 *
 * 内部用 NTabs 切换：
 *   - basic        审批办理（assignee / candidates / 会签）
 *   - multi        会签设置（multiInstance + completionCondition）
 *   - form         表单资产和字段权限（formKey / formFieldPermissions 列表）
 *   - permissions  审批操作权限（7 个布尔开关）
 *   - extensions   逾期提醒 / 任务监听器 / 执行监听器
 *
 * 字段 1:1 迁移自 NodePropertiesPanel.vue:1562-2200，但用更扁平的 emit('update:config') 通信。
 *
 * Props:
 *   - node      flowJson 节点
 *   - readonly
 *
 * Events:
 *   - update:config   增量 patch，外层 NodeConfigDrawer 合并到 draftNode.config
 */
import { computed, ref, watch } from 'vue'
import { appendChildTableCatalogFields, normalizeFlowFieldCatalog, normalizeFlowFormPermissions, serializeFlowFormPermissions } from '@/utils/flow-field-permissions'
import { loadFlowBusinessFormFieldCatalog } from '@/utils/flow-form-loader'
import BusinessFlowFormAssetSelect from '@/views/app-center/components/designer/BusinessFlowFormAssetSelect.vue'
import ApprovalDutyConfig from './ApprovalDutyConfig.vue'
import ApproverAssigneeForm from './ApproverAssigneeForm.vue'
import BasicConfig from './BasicConfig.vue'
import FormPermissionConfig from './FormPermissionConfig.vue'
import ListenerConfig from './ListenerConfig.vue'
import MultiInstanceConfig from './MultiInstanceConfig.vue'
import OverdueReminderConfig from './OverdueReminderConfig.vue'
import PermissionConfig from './PermissionConfig.vue'
import PrintTemplatePolicyConfig from './PrintTemplatePolicyConfig.vue'

const props = defineProps({
  node: { type: Object, required: true },
  formAssetOptions: { type: Array, default: () => [] },
  formFieldCatalog: { type: Array, default: () => [] },
  readonly: Boolean,
})

const emit = defineEmits(['update:config', 'update:node'])

const tab = ref('basic')
const externalFieldCatalog = ref([])
const externalFieldCatalogError = ref('')

const config = computed(() => props.node?.config || {})
const normalizedFormAssetOptions = computed(() => (props.formAssetOptions || [])
  .map((item) => {
    const value = String(item?.value || item?.formKey || item?.key || '').trim()
    if (!value)
      return null
    return {
      ...item,
      value,
      formKey: item?.formKey || value,
      formName: item?.formName || item?.label || value,
      label: item?.label || `${item?.formName || value}（${value}）`,
      formMode: item?.formMode || item?.type || 'BUSINESS_OBJECT_FORM',
      providerKey: item?.providerKey || '',
      providerName: item?.providerName || '',
      objectName: item?.objectName || '',
      sourceType: item?.sourceType || item?.source || '',
      fieldCount: item?.fieldCount,
      fieldPreview: item?.fieldPreview || [],
      fieldCatalog: Array.isArray(item?.fieldCatalog) ? item.fieldCatalog : [],
    }
  })
  .filter(Boolean))
const selectedFormAsset = computed(() => {
  const formKey = String(config.value.formKey || '').trim()
  if (!formKey)
    return null
  return findFormAsset({
    formMode: config.value.formMode,
    formKey,
    providerKey: config.value.providerKey,
  }) || null
})
const activeFormFieldCatalog = computed(() => {
  const base = selectedFormAsset.value?.fieldCatalog?.length
    ? selectedFormAsset.value.fieldCatalog
    : externalFieldCatalog.value.length
      ? externalFieldCatalog.value
      : props.formFieldCatalog
  return appendChildTableCatalogFields(base, selectedFormAsset.value || {})
})
const nodeFormForAssetSelect = computed(() => ({
  formMode: config.value.formMode
    || selectedFormAsset.value?.formMode
    || normalizedFormAssetOptions.value[0]?.formMode
    || 'BUSINESS_OBJECT_FORM',
  formKey: config.value.formKey || '',
  formName: config.value.formName || selectedFormAsset.value?.formName || '',
  providerKey: config.value.providerKey || selectedFormAsset.value?.providerKey || '',
  formUrl: config.value.formUrl || selectedFormAsset.value?.formUrl || '',
  viewKey: config.value.viewKey || selectedFormAsset.value?.viewKey || 'default',
  formRef: config.value.formRef || selectedFormAsset.value?.formRef || {},
}))

watch(
  () => [config.value.formMode, config.value.formUrl, selectedFormAsset.value?.fieldCatalog?.length],
  () => {
    refreshExternalFieldCatalog()
  },
  { immediate: true },
)

function patch(part) {
  emit('update:config', part)
}

async function refreshExternalFieldCatalog() {
  externalFieldCatalogError.value = ''
  const formUrl = String(config.value.formUrl || selectedFormAsset.value?.formUrl || '').trim()
  const formMode = normalizeFormMode(config.value.formMode || selectedFormAsset.value?.formMode)
  if (selectedFormAsset.value?.fieldCatalog?.length || formMode !== 'EXTERNAL' || !formUrl) {
    externalFieldCatalog.value = []
    return
  }
  try {
    externalFieldCatalog.value = await loadFlowBusinessFormFieldCatalog(formUrl)
    if (!externalFieldCatalog.value.length)
      externalFieldCatalogError.value = '该外置表单尚未登记字段目录，请在表单组件中配置 flowFieldCatalog'
  }
  catch (error) {
    externalFieldCatalog.value = []
    externalFieldCatalogError.value = error?.message || '外置表单字段目录加载失败'
  }
}

function updateNode(node) {
  emit('update:node', node)
}

function handleFormAssetUpdate(partial = {}) {
  if (!partial.formKey) {
    patch({
      formType: 'none',
      formMode: '',
      formKey: '',
      formName: '',
      providerKey: '',
      formJson: '',
      formUrl: '',
      formRef: {},
      formFieldPermissions: [],
      formChildPermissions: [],
      formArrayPermissions: [],
    })
    return
  }
  const asset = findFormAsset(partial)
  const formMode = resolveSelectedFormMode(partial, asset)
  const formFieldPermissions = buildFormFieldPermissionsForCatalog(
    config.value.formFieldPermissions,
    asset?.fieldCatalog,
  )
  const permissionBundle = normalizeFlowFormPermissions(formFieldPermissions)
  patch({
    formType: 'dynamic',
    formMode,
    formKey: partial.formKey || '',
    formName: partial.formName || asset?.formName || partial.formKey || '',
    providerKey: partial.providerKey || asset?.providerKey || '',
    formJson: '',
    formUrl: partial.formUrl || asset?.formUrl || '',
    viewKey: partial.viewKey || asset?.viewKey || 'default',
    formRef: partial.formRef || asset?.formRef || buildFormRefFromAsset(asset, formMode),
    formFieldPermissions,
    formChildPermissions: permissionBundle.children,
    formArrayPermissions: permissionBundle.arrays,
  })
}

function findFormAsset(partial = {}) {
  const formKey = String(partial.formKey || '').trim()
  if (!formKey)
    return null
  const providerKey = String(partial.providerKey || '')
  const formMode = normalizeFormMode(partial.formMode || partial.formRef?.type || config.value.formMode)
  const exactAsset = normalizedFormAssetOptions.value.find((asset) => {
    return String(asset.formKey || asset.value || '') === formKey
      && normalizeFormMode(asset.formMode || asset.type) === formMode
      && String(asset.providerKey || '') === providerKey
  })
  if (exactAsset)
    return exactAsset
  const providerAsset = normalizedFormAssetOptions.value.find(asset =>
    String(asset.formKey || asset.value || '') === formKey
    && String(asset.providerKey || '') === providerKey,
  )
  if (providerAsset)
    return providerAsset
  return normalizedFormAssetOptions.value.find(asset => String(asset.formKey || asset.value || '') === formKey) || null
}

function buildFormFieldPermissionsForCatalog(currentPermissions, fieldCatalog = []) {
  const currentBundle = normalizeFlowFormPermissions(currentPermissions)
  const current = new Map()
  for (const permission of currentBundle.fields) {
    if (permission.field)
      current.set(permission.permissionKey, permission)
  }
  const catalog = normalizeFlowFieldCatalog(fieldCatalog)
  if (!catalog.length)
    return serializeFlowFormPermissions(Array.from(current.values()), currentBundle.children, currentBundle.arrays)
  const fields = catalog.map((field) => {
    const permissionKey = field.scope === 'child'
      ? `child:${field.childKey}:${field.childField || field.field}`
      : field.scope === 'array'
        ? `array:${field.arrayKey}:${field.itemField || field.field}`
        : `main:${field.field}`
    const saved = current.get(permissionKey)
    if (saved) {
      return {
        ...saved,
        label: field.label || saved.label || field.field,
      }
    }
    const required = field.required === true
    return {
      field: field.field,
      fieldCode: field.field,
      label: field.label || field.field,
      ...(field.scope === 'child'
        ? { scope: 'child', childKey: field.childKey, childField: field.childField || field.field }
        : (field.scope === 'array'
            ? { scope: 'array', arrayKey: field.arrayKey, itemField: field.itemField || field.field }
            : {})),
      visible: true,
      editable: field.scope !== 'child',
      readable: true,
      writable: field.scope !== 'child',
      required,
    }
  })
  const currentArrays = new Map(currentBundle.arrays.map(item => [item.arrayKey, item]))
  const arrays = Array.from(new Set(catalog.filter(field => field.scope === 'array').map(field => field.arrayKey)))
    .filter(Boolean)
    .map(arrayKey => currentArrays.get(arrayKey) || {
      arrayKey,
      label: catalog.find(field => field.scope === 'array' && field.arrayKey === arrayKey)?.arrayLabel || arrayKey,
      readable: true,
      allowCreate: false,
      allowUpdate: true,
      allowDelete: false,
    })
  return serializeFlowFormPermissions(fields, currentBundle.children, arrays)
}

function resolveSelectedFormMode(partial = {}, asset = null) {
  const source = asset?.source || partial.source || ''
  if (source === 'flowForm')
    return ''
  const raw = partial.formMode || partial.formRef?.formMode || partial.formRef?.type || asset?.formMode || asset?.type || ''
  return normalizeFormMode(raw)
}

function normalizeFormMode(value) {
  const normalized = String(value || '').trim().toUpperCase()
  if (!normalized || normalized === 'DYNAMIC' || normalized === 'NONE')
    return ''
  if (normalized === 'BUSINESS_CODE_FORM' || normalized === 'EXTERNAL')
    return normalized
  if (normalized === 'BUSINESS_OBJECT_FORM')
    return normalized
  return 'BUSINESS_OBJECT_FORM'
}

function buildFormRefFromAsset(asset, formMode) {
  if (!asset?.formKey)
    return {}
  return {
    type: formMode,
    formMode,
    objectCode: asset.objectCode || '',
    objectName: asset.objectName || '',
    applicationId: asset.applicationId || '',
    pageId: asset.pageId || '',
    pageCode: asset.pageCode || '',
    pageName: asset.pageName || '',
    pageType: asset.pageType || '',
    sourceFormKey: asset.sourceFormKey || '',
    formKey: asset.formKey,
    formName: asset.formName || asset.formKey,
    providerKey: asset.providerKey || '',
    formUrl: asset.formUrl || '',
    viewKey: asset.viewKey || 'default',
  }
}
</script>

<template>
  <div class="approver-config">
    <n-tabs v-model:value="tab" class="approver-config-tabs" type="line" size="medium" animated>
      <n-tab-pane name="basic" tab="审批办理">
        <BasicConfig
          :node="node"
          :readonly="readonly"
          @update:node="updateNode"
        />
        <ApproverAssigneeForm
          :config="config"
          :readonly="readonly"
          @update:config="patch"
        />
        <div class="config-section-block">
          <div class="config-section-title">
            多人审批方式
          </div>
          <MultiInstanceConfig
            :config="config"
            :readonly="readonly"
            @update:config="patch"
          />
        </div>
        <div class="config-section-block">
          <div class="config-section-title">
            审批职责与要点
          </div>
          <ApprovalDutyConfig
            :config="config"
            :readonly="readonly"
            @update:config="patch"
          />
        </div>
      </n-tab-pane>
      <n-tab-pane name="form" tab="表单权限">
        <div class="config-section-block">
          <div class="config-section-title">
            节点表单资产
          </div>
          <div class="node-form-asset-block">
            <BusinessFlowFormAssetSelect
              :node-form="nodeFormForAssetSelect"
              :form-assets="normalizedFormAssetOptions"
              :disabled="readonly"
              show-all-modes
              @update="handleFormAssetUpdate"
            />
            <n-tag size="small" :type="config.formKey ? 'success' : 'default'" :bordered="false">
              {{ config.formKey ? '已绑定' : '未绑定' }}
            </n-tag>
          </div>
          <div class="node-form-asset-hint">
            运行时按节点 formKey 加载表单资产。外置表单会读取组件上的 flowFieldCatalog，自动生成下方字段权限。
          </div>
          <n-alert v-if="externalFieldCatalogError" type="warning" :show-icon="false" class="node-form-asset-alert">
            {{ externalFieldCatalogError }}
          </n-alert>
        </div>
        <FormPermissionConfig
          :config="config"
          :form-field-catalog="activeFormFieldCatalog"
          :readonly="readonly"
          @update:config="patch"
        />
        <div class="config-section-block">
          <div class="config-section-title">
            节点打印策略
          </div>
          <PrintTemplatePolicyConfig
            :config="config"
            :form-asset="selectedFormAsset"
            :readonly="readonly"
            @update:config="patch"
          />
        </div>
      </n-tab-pane>
      <n-tab-pane name="permissions" tab="审批权限">
        <div class="config-section-block">
          <div class="config-section-title">
            审批操作权限
          </div>
          <PermissionConfig
            :config="config"
            :readonly="readonly"
            @update:config="patch"
          />
        </div>
      </n-tab-pane>
      <n-tab-pane name="extensions" tab="扩展配置">
        <OverdueReminderConfig
          :config="config"
          :readonly="readonly"
          @update:config="patch"
        />
        <div class="config-section-block">
          <div class="config-section-title">
            监听器
          </div>
          <ListenerConfig
            :config="config"
            :readonly="readonly"
            @update:config="patch"
          />
        </div>
      </n-tab-pane>
    </n-tabs>
  </div>
</template>

<style scoped>
.node-form-asset-block {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 10px;
}

.node-form-asset-hint {
  margin-top: 8px;
  color: #64748b;
  font-size: 12px;
  line-height: 18px;
}

.node-form-asset-alert {
  margin-top: 8px;
}

.approver-config-tabs :deep(.n-tabs-nav-scroll-wrapper) {
  overflow-x: auto;
}
</style>
