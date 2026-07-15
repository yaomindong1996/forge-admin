<template>
  <div class="initialize-step">
    <section class="template-section">
      <div class="section-heading">
        <div>
          <strong>选择常用应用模板</strong>
          <span>选择数据资产后，系统按模板配置基础表单和列表页面。</span>
        </div>
        <n-tag class="recommend-badge" size="small" :bordered="false" type="info">
          推荐
        </n-tag>
      </div>

      <div class="template-grid">
        <button
          v-for="option in templateOptions"
          :key="option.value"
          type="button"
          class="template-option"
          :class="{ active: modelValue === option.value }"
          @click="selectOption(option)"
        >
          <span class="template-preview" :class="option.previewClass" aria-hidden="true">
            <span class="preview-top" />
            <span v-if="option.previewClass === 'tree'" class="preview-tree">
              <i /><i /><i />
            </span>
            <span class="preview-main">
              <span class="preview-search"><i /><i /><b /></span>
              <span class="preview-table"><i /><i /><i /></span>
              <span v-if="option.previewClass === 'master-detail'" class="preview-detail"><i /><i /></span>
            </span>
          </span>
          <span class="template-copy">
            <span class="template-title">
              <n-icon :component="option.icon" />
              <strong>{{ option.label }}</strong>
              <n-icon v-if="modelValue === option.value" class="selected-icon"><CheckmarkCircle /></n-icon>
            </span>
            <small>{{ option.description }}</small>
            <em>{{ option.result }}</em>
          </span>
        </button>
      </div>
    </section>

    <section v-if="isTemplateMode" class="guided-config">
      <div class="guided-heading">
        <div>
          <strong>{{ activeTemplate?.label }}配置</strong>
          <span>{{ activeTemplate?.guide }}</span>
        </div>
        <n-tag size="small" :bordered="false">
          配置后可修改
        </n-tag>
      </div>

      <n-form label-placement="top" :show-feedback="false">
        <TemplateObjectSourcePicker
          :model-value="templateConfig.primarySource"
          title="主对象"
          description="列表、表单和详情都以这个对象为核心。"
          :object-options="objectOptions"
          :datasource-options="datasourceOptions"
          :loading-objects="loadingObjects"
          @update:model-value="updateTemplateSource('primarySource', $event)"
          @update:field-options="updateSourceFieldOptions('primary', $event)"
        />

        <div v-if="modelValue === 'TEMPLATE_TREE_TABLE'" class="conditional-panel">
          <div class="conditional-title">
            <n-icon><GitNetworkOutline /></n-icon>
            <span>
              <strong>树形导航配置</strong>
              <small>左侧树负责筛选右侧主表，不需要进入页面设计器再寻找树配置。</small>
            </span>
          </div>
          <TemplateObjectSourcePicker
            :model-value="templateConfig.treeSource"
            title="左侧树对象"
            description="选择提供树节点的数据表或已有对象。"
            :object-options="objectOptions"
            :datasource-options="datasourceOptions"
            :loading-objects="loadingObjects"
            @update:model-value="updateTemplateSource('treeSource', $event)"
            @update:field-options="updateSourceFieldOptions('tree', $event)"
          />
          <n-grid cols="1 s:2" :x-gap="12" :y-gap="4" responsive="screen">
            <n-form-item-gi label="树节点主键">
              <n-select
                :value="templateConfig.treeKeyField"
                :options="sourceFieldOptions.tree"
                :disabled="!sourceFieldOptions.tree.length"
                placeholder="从树对象字段选择"
                @update:value="updateTemplateConfig('treeKeyField', $event)"
              />
            </n-form-item-gi>
            <n-form-item-gi label="树节点显示字段">
              <n-select
                :value="templateConfig.treeLabelField"
                :options="sourceFieldOptions.tree"
                :disabled="!sourceFieldOptions.tree.length"
                placeholder="例如名称、标题"
                @update:value="updateTemplateConfig('treeLabelField', $event)"
              />
            </n-form-item-gi>
            <n-form-item-gi label="树父级字段">
              <n-select
                :value="templateConfig.treeParentField"
                :options="sourceFieldOptions.tree"
                :disabled="!sourceFieldOptions.tree.length"
                placeholder="例如 parentId"
                @update:value="updateTemplateConfig('treeParentField', $event)"
              />
            </n-form-item-gi>
            <n-form-item-gi label="主表筛选字段">
              <n-select
                :value="templateConfig.primaryTreeField"
                :options="sourceFieldOptions.primary"
                :disabled="!sourceFieldOptions.primary.length"
                placeholder="选择主对象中的关联字段"
                @update:value="updateTemplateConfig('primaryTreeField', $event)"
              />
            </n-form-item-gi>
          </n-grid>
        </div>

        <div v-if="modelValue === 'TEMPLATE_MASTER_DETAIL'" class="conditional-panel">
          <div class="conditional-title detail-title">
            <n-icon><LayersOutline /></n-icon>
            <span>
              <strong>子表清单与主外键</strong>
              <small>每个所选明细对象会嵌入主表新增、编辑和详情页面。</small>
            </span>
            <n-button size="small" secondary @click="addDetail">
              添加子表
            </n-button>
          </div>
          <n-form-item label="主对象主键">
            <n-select
              :value="templateConfig.primaryKeyField"
              :options="sourceFieldOptions.primary"
              :disabled="!sourceFieldOptions.primary.length"
              placeholder="选择主对象主键"
              @update:value="updateTemplateConfig('primaryKeyField', $event)"
            />
          </n-form-item>
          <div class="detail-list">
            <div v-for="(detail, index) in templateConfig.details" :key="detail.clientKey" class="detail-card">
              <div class="detail-card-heading">
                <strong>子表 {{ index + 1 }}</strong>
                <n-button
                  quaternary
                  circle
                  type="error"
                  :disabled="templateConfig.details.length <= 1"
                  aria-label="移除子表"
                  @click="removeDetail(index)"
                >
                  <template #icon>
                    <n-icon><TrashOutline /></n-icon>
                  </template>
                </n-button>
              </div>
              <TemplateObjectSourcePicker
                :model-value="detail.source"
                title="明细对象来源"
                description="选择承载这一组明细行的数据表或已有对象。"
                :object-options="objectOptions"
                :datasource-options="datasourceOptions"
                :loading-objects="loadingObjects"
                @update:model-value="updateDetailSource(index, $event)"
                @update:field-options="updateDetailFieldOptions(detail.clientKey, $event)"
              />
              <n-grid cols="1 s:2" :x-gap="12" responsive="screen">
                <n-form-item-gi label="子表外键">
                  <n-select
                    :value="detail.foreignKeyField"
                    :options="sourceFieldOptions.details[detail.clientKey] || []"
                    :disabled="!(sourceFieldOptions.details[detail.clientKey] || []).length"
                    placeholder="选择关联主对象的字段"
                    @update:value="updateDetail(index, 'foreignKeyField', $event)"
                  />
                </n-form-item-gi>
                <n-form-item-gi label="页面分组名称">
                  <n-input
                    :value="detail.relationName"
                    placeholder="例如：订单明细"
                    @update:value="updateDetail(index, 'relationName', $event)"
                  />
                </n-form-item-gi>
              </n-grid>
            </div>
          </div>
        </div>
      </n-form>

      <div class="generation-summary">
        <n-icon><FlashOutline /></n-icon>
        <span>{{ generationSummary }}</span>
      </div>
    </section>

    <section class="other-starts">
      <div class="section-heading compact">
        <div>
          <strong>其他起点</strong>
          <span>已有表或已有对象继续走原有设计协议，不复制模型。</span>
        </div>
      </div>
      <div class="other-grid">
        <button
          v-for="option in otherOptions"
          :key="option.value"
          type="button"
          class="other-option"
          :class="{ active: modelValue === option.value }"
          @click="selectOption(option)"
        >
          <n-icon :component="option.icon" />
          <span><strong>{{ option.label }}</strong><small>{{ option.description }}</small></span>
          <n-icon v-if="modelValue === option.value" class="selected-icon"><CheckmarkCircle /></n-icon>
        </button>
      </div>
    </section>

    <div v-if="modelValue === 'EXISTING_OBJECT'" class="object-selector">
      <div class="selector-copy">
        <strong>选择主业务对象</strong>
        <span>应用只建立 PRIMARY 关联，对象仍可被其他应用复用。</span>
      </div>
      <n-select
        :value="objectId"
        filterable
        clearable
        :loading="loadingObjects"
        :options="objectOptions"
        placeholder="选择当前业务域中的业务对象"
        @update:value="value => emit('update:objectId', value)"
      />
    </div>

    <div v-if="modelValue === 'DATABASE_TABLE'" class="database-selector">
      <div class="selector-copy">
        <strong>选择要导入的数据表</strong>
        <span>系统按真实表结构创建主对象，并自动生成基础表单和列表。</span>
      </div>
      <n-grid cols="1 s:2" :x-gap="12" responsive="screen">
        <n-form-item-gi label="运行数据源" :show-feedback="false">
          <n-select
            :value="datasourceId"
            filterable
            :loading="loadingDatasources"
            :options="datasourceOptions"
            placeholder="选择数据源"
            @update:value="value => emit('update:datasourceId', value)"
          />
        </n-form-item-gi>
        <n-form-item-gi label="数据表" :show-feedback="false">
          <n-select
            :value="tableName"
            filterable
            clearable
            :disabled="!datasourceId"
            :loading="loadingTables"
            :options="tableOptions"
            placeholder="选择数据表"
            @update:value="value => emit('update:tableName', value)"
          />
        </n-form-item-gi>
      </n-grid>
      <div v-if="selectedTable" class="selected-table-summary">
        <code>{{ selectedTable.tableName }}</code>
        <span>{{ selectedTable.tableComment || '未配置表说明，将使用表名作为对象名称' }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import {
  AlbumsOutline,
  CheckmarkCircle,
  CubeOutline,
  FlashOutline,
  GitNetworkOutline,
  GridOutline,
  LayersOutline,
  TabletLandscapeOutline,
  TrashOutline,
} from '@vicons/ionicons5'
import { computed, reactive } from 'vue'
import TemplateObjectSourcePicker from './TemplateObjectSourcePicker.vue'

const props = defineProps({
  modelValue: { type: String, default: 'TEMPLATE_SINGLE_CRUD' },
  objectId: { type: [Number, String], default: null },
  objectOptions: { type: Array, default: () => [] },
  loadingObjects: { type: Boolean, default: false },
  datasourceId: { type: [Number, String], default: null },
  datasourceOptions: { type: Array, default: () => [] },
  tableName: { type: String, default: null },
  tableOptions: { type: Array, default: () => [] },
  selectedTable: { type: Object, default: null },
  loadingDatasources: { type: Boolean, default: false },
  loadingTables: { type: Boolean, default: false },
  templateConfig: {
    type: Object,
    default: () => ({ details: [] }),
  },
})

const emit = defineEmits([
  'update:modelValue',
  'update:objectId',
  'update:datasourceId',
  'update:tableName',
  'update:templateConfig',
])

const sourceFieldOptions = reactive({
  primary: [],
  tree: [],
  details: {},
})

const templateOptions = [
  {
    value: 'TEMPLATE_SINGLE_CRUD',
    templateCode: 'SINGLE_CRUD',
    label: '单表 CRUD',
    description: '适合档案、配置、基础台账等一个数据对象即可完成的场景。',
    result: '生成 1 个对象 · 列表 · 表单 · 详情',
    guide: '选择主对象来源后生成标准增删改查页面。',
    previewClass: 'single',
    icon: GridOutline,
  },
  {
    value: 'TEMPLATE_TREE_TABLE',
    templateCode: 'TREE_TABLE',
    label: '左树右表',
    description: '适合分类、组织、区域等树形导航筛选右侧业务列表。',
    result: '生成 2 个对象 · 树形关系 · 左树右表',
    guide: '配置树节点和主表关联字段，其余页面结构自动生成。',
    previewClass: 'tree',
    icon: GitNetworkOutline,
  },
  {
    value: 'TEMPLATE_MASTER_DETAIL',
    templateCode: 'MASTER_DETAIL',
    label: '主子表',
    description: '适合订单、合同、出入库单等主记录带多行明细的场景。',
    result: '生成主对象 · 子表清单 · 明细编辑区',
    guide: '维护子表清单和主外键，系统自动生成明细关系。',
    previewClass: 'master-detail',
    icon: LayersOutline,
  },
]

const otherOptions = [
  {
    value: 'DATABASE_TABLE',
    label: '从数据库表开始',
    description: '选择现有表并按真实结构导入',
    icon: AlbumsOutline,
  },
  {
    value: 'EXISTING_OBJECT',
    label: '绑定已有对象',
    description: '复用当前业务域内的对象',
    icon: CubeOutline,
  },
  {
    value: 'BLANK',
    label: '空白应用',
    description: '只创建应用草稿，稍后设计',
    icon: TabletLandscapeOutline,
  },
]

const isTemplateMode = computed(() => props.modelValue.startsWith('TEMPLATE_'))
const activeTemplate = computed(() => templateOptions.find(item => item.value === props.modelValue))
const generationSummary = computed(() => {
  if (props.modelValue === 'TEMPLATE_TREE_TABLE')
    return '将导入或复用主对象与树对象，并配置引用关系和左树右表页面。'
  if (props.modelValue === 'TEMPLATE_MASTER_DETAIL')
    return `将导入或复用 1 个主对象、${props.templateConfig.details?.length || 1} 个明细对象及主子表页面。`
  return '将导入或复用 1 个主对象，并配置可继续修改的列表、表单和详情页面。'
})

function selectOption(option) {
  emit('update:modelValue', option.value)
}

function updateTemplateConfig(key, value) {
  updateTemplateConfigPatch({ [key]: value })
}

function updateTemplateConfigPatch(patch) {
  emit('update:templateConfig', patch)
}

function updateTemplateSource(key, source) {
  const clearFields = key === 'primarySource'
    ? { primaryKeyField: null, primaryTreeField: null }
    : { treeKeyField: null, treeLabelField: null, treeParentField: null }
  emit('update:templateConfig', { ...clearFields, [key]: source })
}

function updateSourceFieldOptions(role, options) {
  sourceFieldOptions[role] = options || []
  if (!options?.length)
    return
  if (role === 'primary') {
    const patch = {}
    if (!hasField(options, props.templateConfig.primaryKeyField)) {
      patch.primaryKeyField = preferredField(
        options,
        item => item.isPrimaryKey,
        () => true,
      )
    }
    if (!hasField(options, props.templateConfig.primaryTreeField))
      patch.primaryTreeField = null
    if (Object.keys(patch).length)
      updateTemplateConfigPatch(patch)
    return
  }
  const patch = {}
  if (!hasField(options, props.templateConfig.treeKeyField))
    patch.treeKeyField = preferredField(options, item => item.isPrimaryKey, () => true)
  if (!hasField(options, props.templateConfig.treeLabelField)) {
    patch.treeLabelField = preferredField(
      options,
      item => /(^|_)(name|title|label|code)$/i.test(item.value) || /(Name|Title|Label)$/.test(item.value),
      item => !item.isPrimaryKey && !item.systemField,
    )
  }
  if (!hasField(options, props.templateConfig.treeParentField)) {
    patch.treeParentField = preferredField(
      options,
      item => /(^parent(Id)?$|parent_id)/i.test(item.value),
    )
  }
  if (Object.keys(patch).length)
    updateTemplateConfigPatch(patch)
}

function addDetail() {
  const index = (props.templateConfig.details || []).length + 1
  updateTemplateConfig('details', [
    ...(props.templateConfig.details || []),
    {
      clientKey: `detail_${Date.now()}_${index}`,
      source: { sourceType: 'DATABASE_TABLE', datasourceId: null, tableName: null, objectId: null },
      foreignKeyField: null,
      relationName: `明细${index}`,
    },
  ])
}

function removeDetail(index) {
  const detail = (props.templateConfig.details || [])[index]
  if (detail?.clientKey)
    delete sourceFieldOptions.details[detail.clientKey]
  updateTemplateConfig('details', (props.templateConfig.details || []).filter((_, itemIndex) => itemIndex !== index))
}

function updateDetail(index, key, value) {
  updateTemplateConfig('details', (props.templateConfig.details || []).map((item, itemIndex) => (
    itemIndex === index ? { ...item, [key]: value } : item
  )))
}

function updateDetailSource(index, source) {
  const details = (props.templateConfig.details || []).map((item, itemIndex) => (
    itemIndex === index ? { ...item, source, foreignKeyField: null } : item
  ))
  updateTemplateConfig('details', details)
}

function updateDetailFieldOptions(clientKey, options) {
  sourceFieldOptions.details[clientKey] = options || []
  if (!options?.length)
    return
  const index = (props.templateConfig.details || []).findIndex(item => item.clientKey === clientKey)
  if (index < 0)
    return
  const detail = props.templateConfig.details[index]
  if (!hasField(options, detail.foreignKeyField)) {
    updateDetail(index, 'foreignKeyField', preferredField(
      options,
      item => item.value !== 'id' && /(_id|Id)$/i.test(item.value),
    ))
  }
}

function hasField(options, value) {
  return Boolean(value) && options.some(item => item.value === value)
}

function preferredField(options, ...matchers) {
  for (const matcher of matchers) {
    const matched = options.find(matcher)
    if (matched)
      return matched.value
  }
  return null
}
</script>

<style scoped>
.initialize-step,
.template-section,
.guided-config,
.other-starts,
.conditional-panel,
.database-selector {
  display: grid;
  gap: 14px;
}

.section-heading,
.guided-heading,
.conditional-title,
.template-title,
.other-option,
.selected-table-summary,
.generation-summary {
  display: flex;
  align-items: center;
}

.section-heading,
.guided-heading {
  justify-content: space-between;
  gap: 16px;
}

.section-heading > div,
.guided-heading > div,
.selector-copy,
.template-copy,
.other-option > span,
.conditional-title > span {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 3px;
}

.section-heading strong,
.guided-heading strong {
  color: var(--n-text-color);
  font-size: 14px;
}

.section-heading span,
.guided-heading span,
.selector-copy span,
.conditional-title small,
.selected-table-summary span {
  color: var(--n-text-color-3);
  font-size: 12px;
  line-height: 1.5;
}

.template-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.template-option {
  box-sizing: border-box;
  display: grid;
  min-width: 0;
  grid-template-rows: 82px minmax(96px, auto);
  overflow: hidden;
  padding: 0;
  cursor: pointer;
  border: 1px solid var(--n-border-color, #e5e7eb);
  border-radius: 9px;
  color: var(--n-text-color);
  background: var(--n-color, #fff);
  text-align: left;
  transition: border-color 0.16s ease, box-shadow 0.16s ease;
}

.template-option:hover,
.template-option.active {
  border-color: var(--n-primary-color);
}

.template-option.active {
  box-shadow: inset 0 0 0 1px var(--n-primary-color);
}

.template-preview {
  position: relative;
  box-sizing: border-box;
  display: grid;
  width: 100%;
  min-width: 0;
  min-height: 0;
  grid-template-columns: 1fr;
  height: 100%;
  overflow: hidden;
  padding: 19px 9px 8px;
  background: var(--n-color-embedded, #f7f8fa);
}

.template-preview.tree {
  grid-template-columns: 30% 1fr;
  gap: 7px;
}

.preview-top {
  position: absolute;
  top: 8px;
  right: 10px;
  left: 10px;
  height: 5px;
  border-radius: 3px;
  background: color-mix(in srgb, var(--n-text-color-3) 22%, transparent);
}

.preview-tree,
.preview-main,
.preview-search,
.preview-table,
.preview-detail {
  display: flex;
  min-width: 0;
  min-height: 0;
  border: 1px solid var(--n-border-color, #dfe3e8);
  border-radius: 4px;
  background: var(--n-color, #fff);
}

.preview-tree {
  flex-direction: column;
  gap: 6px;
  padding: 7px 6px;
}

.preview-tree i {
  width: 70%;
  height: 4px;
  border-radius: 2px;
  background: color-mix(in srgb, var(--n-primary-color) 30%, transparent);
}

.preview-tree i:nth-child(2) {
  width: 88%;
  margin-left: 8px;
}

.preview-main {
  flex-direction: column;
  gap: 4px;
  padding: 6px;
}

.preview-search {
  align-items: center;
  gap: 5px;
  padding: 4px;
}

.preview-search i {
  width: 27%;
  height: 4px;
  border-radius: 2px;
  background: color-mix(in srgb, var(--n-text-color-3) 18%, transparent);
}

.preview-search b {
  width: 16px;
  height: 7px;
  margin-left: auto;
  border-radius: 2px;
  background: color-mix(in srgb, var(--n-primary-color) 45%, transparent);
}

.preview-table,
.preview-detail {
  flex: 1;
  flex-direction: column;
  gap: 4px;
  padding: 5px;
}

.preview-table i,
.preview-detail i {
  height: 4px;
  border-radius: 2px;
  background: color-mix(in srgb, var(--n-text-color-3) 16%, transparent);
}

.preview-detail {
  flex: 0 0 17px;
  flex-direction: row;
}

.preview-detail i {
  flex: 1;
}

.template-copy {
  position: relative;
  z-index: 1;
  box-sizing: border-box;
  display: grid;
  min-width: 0;
  min-height: 96px;
  grid-template-rows: auto minmax(34px, 1fr) auto;
  gap: 4px;
  padding: 10px 11px;
  background: var(--n-color, #fff);
}

.template-title {
  min-width: 0;
  min-height: 20px;
  gap: 6px;
}

.template-title strong {
  min-width: 0;
  flex: 1;
  font-size: 14px;
  line-height: 1.4;
  white-space: nowrap;
}

.selected-icon {
  color: var(--n-primary-color);
}

.template-copy small {
  color: var(--n-text-color-3);
  font-size: 12px;
  line-height: 1.45;
}

.template-copy em {
  color: var(--n-text-color-2);
  font-size: 11px;
  font-style: normal;
}

.guided-config,
.object-selector,
.database-selector {
  padding: 15px;
  border: 1px solid var(--n-border-color, #e5e7eb);
  border-radius: 9px;
  background: var(--n-color-embedded, #f7f8fa);
}

.conditional-panel {
  margin-top: 6px;
  padding: 13px;
  border: 1px solid var(--n-border-color, #e5e7eb);
  border-radius: 7px;
  background: var(--n-color, #fff);
}

.conditional-title {
  gap: 9px;
}

.conditional-title > .n-icon {
  color: var(--n-primary-color);
  font-size: 18px;
}

.conditional-title > span {
  flex: 1;
}

.recommend-badge {
  flex: 0 0 auto;
  min-width: 42px;
  white-space: nowrap;
}

.recommend-badge :deep(.n-tag__content) {
  line-height: 1;
  white-space: nowrap;
}

.detail-list {
  display: grid;
  gap: 8px;
}

.detail-card {
  display: grid;
  gap: 10px;
  padding: 11px;
  border: 1px solid var(--n-border-color, #e5e7eb);
  border-radius: 8px;
  background: var(--n-color-embedded, #f7f8fa);
}

.detail-card-heading {
  display: flex;
  min-height: 28px;
  align-items: center;
  justify-content: space-between;
}

.detail-card-heading strong {
  color: var(--n-text-color);
  font-size: 12px;
}

.generation-summary {
  gap: 7px;
  color: var(--n-text-color-2);
  font-size: 12px;
}

.generation-summary .n-icon {
  color: var(--n-primary-color);
}

.other-starts {
  padding-top: 4px;
  border-top: 1px solid var(--n-border-color, #e5e7eb);
}

.other-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.other-option {
  min-height: 56px;
  gap: 9px;
  padding: 9px 11px;
  cursor: pointer;
  border: 1px solid var(--n-border-color, #e5e7eb);
  border-radius: 7px;
  color: var(--n-text-color);
  background: var(--n-color, #fff);
  text-align: left;
}

.other-option.active {
  border-color: var(--n-primary-color);
  background: color-mix(in srgb, var(--n-primary-color) 6%, var(--n-color, #fff));
}

.other-option > .n-icon:first-child {
  color: var(--n-primary-color);
  font-size: 18px;
}

.other-option > span {
  flex: 1;
}

.other-option strong {
  font-size: 12px;
}

.other-option small {
  color: var(--n-text-color-3);
  font-size: 11px;
}

.object-selector {
  display: grid;
  grid-template-columns: minmax(220px, 0.8fr) minmax(280px, 1.2fr);
  gap: 18px;
  align-items: center;
}

.selected-table-summary {
  min-width: 0;
  gap: 10px;
}

.selected-table-summary code {
  flex: 0 0 auto;
  color: var(--n-text-color);
  font-weight: 600;
}

.selected-table-summary span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 760px) {
  .template-grid,
  .other-grid,
  .object-selector,
  .database-selector :deep(.n-grid) {
    grid-template-columns: 1fr;
  }
}
</style>
