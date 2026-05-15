<template>
  <div class="data-scope-config-page">
    <div class="page-header">
      <div class="header-main">
        <div class="title-row">
          <div class="title-icon">
            <i class="i-material-symbols:policy-outline-rounded" />
          </div>
          <div>
            <h2 class="page-title">
              数据权限配置
            </h2>
            <div class="header-desc">
              按资源与 Mapper 方法维护数据范围字段，支撑用户、组织、租户与行政区划隔离
            </div>
          </div>
        </div>
      </div>

      <div class="scope-summary">
        <div
          v-for="item in summaryItems"
          :key="item.label"
          class="summary-item"
        >
          <i :class="item.icon" />
          <div>
            <div class="summary-label">
              {{ item.label }}
            </div>
            <div class="summary-value">
              {{ item.value }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="data-scope-content">
      <AiCrudPage
        ref="crudRef"
        api="/system/dataScopeConfig"
        :api-config="{
          list: 'get@/system/dataScopeConfig/page',
          detail: 'post@/system/dataScopeConfig/getById',
          add: 'post@/system/dataScopeConfig/add',
          update: 'post@/system/dataScopeConfig/edit',
          delete: 'post@/system/dataScopeConfig/remove',
        }"
        :search-schema="searchSchema"
        :columns="tableColumns"
        :edit-schema="editSchema"
        row-key="id"
        add-button-text="新增配置"
        :load-detail-on-edit="true"
        :edit-grid-cols="2"
        edit-label-placement="top"
        edit-form-class="data-scope-edit-form"
        modal-width="1080px"
        :hide-selection="true"
        :striped="true"
        :scroll-x="1460"
        max-height="calc(100vh - 360px)"
        :search-grid-cols="3"
        :search-max-visible-fields="3"
        search-label-width="84"
      >
        <template #form-mapperMethod="{ value: mapperValue, updateValue }">
          <ScopeColumnEditor
            :value="mapperValue"
            title="Mapper Method"
            description="完整接口方法名，需与 XML SQL 的 mapperMethod 精确匹配"
            placeholder="com.mdframe.forge.plugin.system.mapper.SysUserMapper.selectUserPage"
            :examples="mapperExamples"
            :tokens="[]"
            tone="indigo"
            :rows="2"
            @update:value="updateValue"
          />
        </template>

        <template #form-userIdColumn="{ value: userColumnValue, updateValue }">
          <ScopeColumnEditor
            :value="userColumnValue"
            title="USER Scope"
            description="本人数据范围字段，可直接填写字段名，也可使用 SQL 表达式"
            placeholder="user_id 或 create_by"
            :examples="userColumnExamples"
            :tokens="commonSqlTokens"
            tone="blue"
            @update:value="updateValue"
          />
        </template>

        <template #form-orgIdColumn="{ value: orgColumnValue, updateValue }">
          <ScopeColumnEditor
            :value="orgColumnValue"
            title="ORG Scope"
            description="组织数据范围字段，支持当前组织、下级组织和自定义组织集合"
            placeholder="org_id 或 dept_id"
            :examples="orgColumnExamples"
            :tokens="commonSqlTokens"
            tone="teal"
            @update:value="updateValue"
          />
        </template>

        <template #form-tenantIdColumn="{ value: tenantColumnValue, updateValue }">
          <ScopeColumnEditor
            :value="tenantColumnValue"
            title="TENANT Scope"
            description="租户隔离字段，通常为 tenant_id"
            placeholder="tenant_id"
            :examples="tenantColumnExamples"
            :tokens="commonSqlTokens"
            tone="amber"
            @update:value="updateValue"
          />
        </template>

        <template #form-regionCodeColumn="{ value: regionColumnValue, updateValue }">
          <ScopeColumnEditor
            :value="regionColumnValue"
            title="REGION Scope"
            description="行政区划数据范围字段，支持本级、下级和祖先区划变量"
            placeholder="region_code 或 area_code"
            :examples="regionColumnExamples"
            :tokens="regionSqlTokens"
            tone="green"
            @update:value="updateValue"
          />
        </template>
      </AiCrudPage>
    </div>
  </div>
</template>

<script setup>
import { NInput, NTag, NTooltip } from 'naive-ui'
import { computed, defineComponent, h, ref } from 'vue'
import { AiCrudPage } from '@/components/ai-form'
import { request } from '@/utils'

defineOptions({ name: 'DataScopeConfig' })

const crudRef = ref(null)

const summaryItems = [
  {
    label: '资源定位',
    value: 'resourceCode',
    icon: 'i-material-symbols:barcode-scanner-rounded',
  },
  {
    label: 'SQL 匹配',
    value: 'mapperMethod',
    icon: 'i-material-symbols:account-tree-outline-rounded',
  },
  {
    label: '范围字段',
    value: 'user/org/tenant/region',
    icon: 'i-material-symbols:rule-settings-rounded',
  },
]

// 是否启用选项
const enabledOptions = [
  { label: '启用', value: 1 },
  { label: '禁用', value: 0 },
]

const commonSqlTokens = ['#{userId}', '#{tenantId}', '#{orgIds}', '#{customOrgIds}']
const regionSqlTokens = ['#{regionCode}', '#{regionLevel}', '#{regionAncestors}']

const mapperExamples = [
  {
    label: '用户分页',
    value: 'com.mdframe.forge.plugin.system.mapper.SysUserMapper.selectUserPage',
  },
  {
    label: '组织列表',
    value: 'com.mdframe.forge.plugin.system.mapper.SysOrgMapper.selectOrgList',
  },
]

const userColumnExamples = [
  { label: '创建人', value: 'create_by' },
  { label: '用户字段', value: 'user_id' },
  { label: 'SQL', value: '<sql>t.user_id = #{userId}' },
]

const orgColumnExamples = [
  { label: '部门字段', value: 'dept_id' },
  { label: '组织字段', value: 'org_id' },
  { label: 'SQL', value: '<sql>t.org_id IN (#{orgIds})' },
]

const tenantColumnExamples = [
  { label: '默认租户字段', value: 'tenant_id' },
  { label: 'SQL', value: '<sql>t.tenant_id = #{tenantId}' },
]

const regionColumnExamples = [
  { label: '区划字段', value: 'region_code' },
  { label: '区域字段', value: 'area_code' },
  { label: 'SQL', value: '<sql>t.region_code = #{regionCode}' },
]

const ScopeColumnEditor = defineComponent({
  name: 'ScopeColumnEditor',
  props: {
    value: {
      type: [String, Number],
      default: '',
    },
    title: {
      type: String,
      required: true,
    },
    description: {
      type: String,
      default: '',
    },
    placeholder: {
      type: String,
      default: '',
    },
    rows: {
      type: Number,
      default: 3,
    },
    examples: {
      type: Array,
      default: () => [],
    },
    tokens: {
      type: Array,
      default: () => [],
    },
    tone: {
      type: String,
      default: 'blue',
    },
  },
  emits: ['update:value'],
  setup(props, { emit }) {
    const updateValue = value => emit('update:value', value)
    const getValue = () => props.value == null ? '' : String(props.value)

    function appendToken(token) {
      const currentValue = getValue()
      updateValue(currentValue ? `${currentValue} ${token}` : token)
    }

    function renderExamples() {
      if (!props.examples.length) {
        return null
      }

      return h('div', { class: 'scope-editor-section scope-editor-example-section' }, [
        h('div', { class: 'scope-editor-section-head' }, [
          h('span', { class: 'scope-editor-section-title' }, '常用示例'),
          h('span', { class: 'scope-editor-section-hint' }, '点击整行填入'),
        ]),
        h('div', { class: 'scope-editor-example-list' }, props.examples.map(example => h('div', {
          'key': example.value,
          'class': 'scope-example-row',
          'role': 'button',
          'tabindex': 0,
          'aria-label': `使用示例：${example.label}`,
          'onClick': () => updateValue(example.value),
          'onKeydown': (event) => {
            if (event.key === 'Enter' || event.key === ' ') {
              event.preventDefault()
              updateValue(example.value)
            }
          },
        }, [
          h('div', { class: 'scope-example-label' }, example.label),
          h('code', { class: 'scope-example-code' }, example.value),
        ]))),
      ])
    }

    function renderTokens() {
      if (!props.tokens.length) {
        return null
      }

      return h('div', { class: 'scope-editor-section scope-editor-token-section' }, [
        h('div', { class: 'scope-editor-section-head' }, [
          h('span', { class: 'scope-editor-section-title' }, '可用变量'),
          h('span', { class: 'scope-editor-section-hint' }, '点击追加到当前内容'),
        ]),
        h('div', { class: 'scope-token-list' }, props.tokens.map(token => h('button', {
          key: token,
          type: 'button',
          class: 'scope-token-button',
          onClick: () => appendToken(token),
        }, token))),
      ])
    }

    return () => h('div', { class: ['scope-column-editor', `tone-${props.tone}`] }, [
      h('div', { class: 'scope-editor-head' }, [
        h('div', { class: 'scope-editor-title' }, props.title),
        h('div', { class: 'scope-editor-desc' }, props.description),
      ]),
      h(NInput, {
        value: getValue(),
        type: 'textarea',
        rows: props.rows,
        autosize: { minRows: props.rows, maxRows: 8 },
        placeholder: props.placeholder,
        class: 'scope-code-input',
        onUpdateValue: updateValue,
      }),
      renderExamples(),
      renderTokens(),
    ])
  },
})

// 搜索表单配置
const searchSchema = [
  {
    field: 'resourceCode',
    label: '资源编码',
    type: 'input',
    props: {
      placeholder: '资源编码 / 权限标识',
    },
  },
  {
    field: 'resourceName',
    label: '资源名称',
    type: 'input',
    props: {
      placeholder: '资源名称',
    },
  },
  {
    field: 'enabled',
    label: '是否启用',
    type: 'select',
    props: {
      placeholder: '状态',
      options: enabledOptions,
    },
  },
]

function isEnabled(row) {
  return Number(row.enabled) === 1
}

function getShortMapperMethod(mapperMethod) {
  if (!mapperMethod) {
    return '-'
  }

  const parts = mapperMethod.split('.')
  if (parts.length < 2) {
    return mapperMethod
  }

  return `${parts[parts.length - 2]}.${parts[parts.length - 1]}`
}

function getFieldPreview(value) {
  if (!value) {
    return '-'
  }

  const text = String(value)
  if (text.startsWith('<sql>')) {
    return '<sql>'
  }

  return text
}

function renderTextTooltip(text, triggerClass, displayText) {
  if (!text) {
    return h('span', { class: 'empty-text' }, '-')
  }

  return h(NTooltip, {
    trigger: 'hover',
    placement: 'top-start',
    width: 520,
  }, {
    trigger: () => h('span', { class: triggerClass }, displayText || text),
    default: () => h('span', { class: 'tooltip-text' }, text),
  })
}

function renderResourceCell(row) {
  return h('div', { class: 'resource-cell' }, [
    h('div', { class: 'resource-name-line' }, [
      h('span', { class: 'resource-name' }, row.resourceName || '-'),
      isEnabled(row)
        ? h(NTag, { type: 'success', size: 'small', bordered: false, round: true }, { default: () => '启用' })
        : h(NTag, { type: 'default', size: 'small', bordered: false, round: true }, { default: () => '禁用' }),
    ]),
    renderTextTooltip(row.resourceCode, 'resource-code', row.resourceCode || '-'),
  ])
}

function renderMapperCell(row) {
  return h('div', { class: 'mapper-cell' }, [
    renderTextTooltip(row.mapperMethod, 'mapper-method', getShortMapperMethod(row.mapperMethod)),
  ])
}

function renderAliasCell(row) {
  return h('span', {
    class: row.tableAlias ? 'alias-badge' : 'alias-badge is-empty',
  }, row.tableAlias || '未配置')
}

function renderScopeFields(row) {
  const fields = [
    { label: '用户', value: row.userIdColumn, type: 'info' },
    { label: '组织', value: row.orgIdColumn, type: 'success' },
    { label: '租户', value: row.tenantIdColumn, type: 'warning' },
    { label: '区划', value: row.regionCodeColumn, type: 'primary' },
  ].filter(item => item.value)

  if (!fields.length) {
    return h('span', { class: 'empty-text' }, '未配置')
  }

  return h('div', { class: 'scope-field-tags' }, fields.map(item => h(NTooltip, {
    key: item.label,
    trigger: 'hover',
    placement: 'top-start',
    width: 420,
  }, {
    trigger: () => h(NTag, {
      type: item.type,
      size: 'small',
      bordered: false,
      round: true,
      class: 'scope-field-tag',
    }, { default: () => `${item.label} ${getFieldPreview(item.value)}` }),
    default: () => h('span', { class: 'tooltip-text' }, item.value),
  })))
}

function renderRemark(row) {
  return renderTextTooltip(row.remark, 'remark-text', row.remark || '-')
}

// 表格列配置
const tableColumns = computed(() => [
  {
    prop: 'resource',
    label: '资源',
    width: 280,
    render: renderResourceCell,
  },
  {
    prop: 'mapperMethod',
    label: 'Mapper 精确匹配',
    width: 340,
    render: renderMapperCell,
  },
  {
    prop: 'tableAlias',
    label: '表别名',
    width: 100,
    render: renderAliasCell,
  },
  {
    prop: 'scopeFields',
    label: '权限字段',
    width: 360,
    render: renderScopeFields,
  },
  {
    prop: 'enabled',
    label: '状态',
    width: 90,
    render: (row) => {
      return h(
        NTag,
        {
          type: isEnabled(row) ? 'success' : 'error',
          size: 'small',
          bordered: false,
          round: true,
        },
        { default: () => isEnabled(row) ? '启用' : '禁用' },
      )
    },
  },
  {
    prop: 'remark',
    label: '备注',
    width: 160,
    render: renderRemark,
  },
  {
    prop: 'createTime',
    label: '创建时间',
    width: 180,
  },
  {
    prop: 'action',
    label: '操作',
    width: 130,
    fixed: 'right',
    actions: [
      { label: '编辑', key: 'edit', type: 'primary', onClick: handleEdit },
      { label: '删除', key: 'delete', type: 'error', onClick: handleDelete },
    ],
  },
])

// 编辑表单配置
const editSchema = [
  {
    type: 'divider',
    label: '基础信息',
    props: {
      titlePlacement: 'left',
    },
    span: 2,
  },
  {
    field: 'resourceCode',
    label: '资源编码',
    type: 'input',
    span: 2,
    rules: [{ required: true, message: '请输入资源编码', trigger: 'blur' }],
    props: {
      placeholder: '请输入资源编码，如：system:user:list',
    },
  },
  {
    field: 'resourceName',
    label: '资源名称',
    type: 'input',
    rules: [{ required: true, message: '请输入资源名称', trigger: 'blur' }],
    props: {
      placeholder: '请输入资源名称',
    },
  },
  {
    field: 'mapperMethod',
    label: 'Mapper方法',
    type: 'slot',
    span: 2,
    rules: [{ required: true, message: '请输入Mapper方法', trigger: 'blur' }],
    props: {
      placeholder: '请输入Mapper方法，如：com.mdframe.forge.plugin.system.mapper.SysUserMapper.selectList',
      rows: 2,
    },
  },
  {
    type: 'divider',
    label: '字段配置',
    props: {
      titlePlacement: 'left',
    },
    span: 2,
  },
  {
    field: 'tableAlias',
    label: '主表别名',
    type: 'input',
    defaultValue: 't',
    props: {
      placeholder: '请输入主表别名，如：t、lc、o',
    },
  },
  {
    field: 'enabled',
    label: '是否启用',
    type: 'radioButton',
    defaultValue: 1,
    props: {
      options: enabledOptions,
      clearable: false,
    },
  },
  {
    field: 'userIdColumn',
    label: '用户ID字段',
    type: 'slot',
    span: 2,
    defaultValue: 'user_id',
    rules: [{ required: true, message: '请输入用户ID字段', trigger: 'blur' }],
    props: {
      placeholder: '简单模式：字段名（如：user_id、create_by）\n复杂模式：<sql>开头，支持占位符#{userId}、#{tenantId}、#{orgIds}、#{customOrgIds}',
      rows: 3,
      type: 'textarea',
    },
  },
  {
    field: 'orgIdColumn',
    label: '组织ID字段',
    type: 'slot',
    span: 2,
    defaultValue: 'org_id',
    rules: [{ required: true, message: '请输入组织ID字段', trigger: 'blur' }],
    props: {
      placeholder: '简单模式：字段名（如：org_id、dept_id）\n复杂模式：<sql>开头，支持占位符#{userId}、#{tenantId}、#{orgIds}、#{customOrgIds}',
      rows: 3,
      type: 'textarea',
    },
  },
  {
    field: 'tenantIdColumn',
    label: '租户ID字段',
    type: 'slot',
    span: 2,
    defaultValue: 'tenant_id',
    rules: [{ required: true, message: '请输入租户ID字段', trigger: 'blur' }],
    props: {
      placeholder: '简单模式：字段名（如：tenant_id）\n复杂模式：<sql>开头，支持占位符#{userId}、#{tenantId}、#{orgIds}、#{customOrgIds}',
      rows: 3,
      type: 'textarea',
    },
  },
  {
    field: 'regionCodeColumn',
    label: '行政区划字段',
    type: 'slot',
    span: 2,
    props: {
      placeholder: '用于 REGION（本行政区划）数据权限\n简单模式：字段名（如：area_code、region_code）\n复杂模式：<sql>开头，支持占位符#{regionCode}、#{regionLevel}、#{regionAncestors}',
      rows: 3,
      type: 'textarea',
    },
  },
  {
    field: 'userRegionColumn',
    label: '用户行政区划字段',
    type: 'input',
    span: 1,
    props: {
      placeholder: '可选，配合用户表别名做 OR 匹配（如：area_code）',
    },
  },
  {
    field: 'userTableAlias',
    label: '用户表别名',
    type: 'input',
    span: 1,
    props: {
      placeholder: '可选，配合用户行政区划字段（如：u）',
    },
  },
  {
    type: 'divider',
    label: '其他信息',
    props: {
      titlePlacement: 'left',
    },
    span: 2,
  },
  {
    field: 'remark',
    label: '备注',
    type: 'textarea',
    span: 2,
    props: {
      placeholder: '请输入备注说明',
      rows: 3,
    },
  },
]

// 编辑
function handleEdit(row) {
  crudRef.value?.showEdit(row)
}

// 删除
function handleDelete(row) {
  window.$dialog.warning({
    title: '确认删除',
    content: `确定要删除「${row.resourceName || row.resourceCode || row.id}」的数据权限配置吗？删除后将无法恢复！`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await request.post('/system/dataScopeConfig/remove', null, {
          params: { id: row.id },
        })
        if (res.code === 200) {
          window.$message.success('删除成功')
          crudRef.value?.refresh()
        }
      }
      catch {
        window.$message.error('删除失败')
      }
    },
  })
}
</script>

<style scoped>
.data-scope-config-page {
  height: 100%;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  background: #f6f8fb;
  color: #0f172a;
}

.page-header {
  display: flex;
  align-items: stretch;
  justify-content: space-between;
  gap: 18px;
  padding: 18px 20px;
  background: linear-gradient(135deg, #ffffff 0%, #f8fbff 100%);
  border: 1px solid #dbe5f3;
  border-radius: 14px;
  box-shadow: 0 10px 26px rgba(15, 23, 42, 0.05);
}

.header-main {
  display: flex;
  align-items: center;
  min-width: 0;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.title-icon {
  width: 42px;
  height: 42px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #fff;
  font-size: 22px;
  background: linear-gradient(135deg, #1e40af 0%, #0f766e 100%);
  box-shadow: 0 10px 18px rgba(30, 64, 175, 0.18);
}

.page-title {
  margin: 0;
  color: #0f172a;
  font-size: 20px;
  line-height: 28px;
  font-weight: 700;
}

.header-desc {
  margin-top: 3px;
  color: #64748b;
  font-size: 13px;
  line-height: 20px;
}

.scope-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(132px, 1fr));
  gap: 10px;
  min-width: 480px;
}

.summary-item {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  background: rgba(255, 255, 255, 0.82);
}

.summary-item i {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #1d4ed8;
  font-size: 18px;
  background: #eff6ff;
}

.summary-label {
  color: #64748b;
  font-size: 12px;
  line-height: 16px;
}

.summary-value {
  max-width: 160px;
  margin-top: 2px;
  overflow: hidden;
  color: #0f172a;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  line-height: 18px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.data-scope-content {
  flex: 1;
  min-height: 0;
  padding: 16px 18px;
  background: #fff;
  border: 1px solid #dbe5f3;
  border-radius: 14px;
  box-shadow: 0 10px 26px rgba(15, 23, 42, 0.04);
}

.data-scope-content :deep(.ai-crud-page) {
  height: 100%;
  background: transparent;
}

.data-scope-content :deep(.ai-crud-search) {
  padding: 2px 0 14px;
  background: transparent;
  border-bottom: 1px solid #eef2f7;
}

.data-scope-content :deep(.ai-crud-main) {
  background: transparent;
}

.data-scope-content :deep(.ai-table-toolbar) {
  padding: 12px 0;
}

.data-scope-content :deep(.n-data-table) {
  border-radius: 10px;
}

.data-scope-content :deep(.n-data-table-th) {
  background: #f8fafc !important;
  color: #334155;
  font-size: 13px;
  font-weight: 700;
}

.data-scope-content :deep(.n-data-table-tr:hover .n-data-table-td) {
  background: #f8fbff;
}

.resource-cell {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.resource-name-line {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.resource-name {
  overflow: hidden;
  color: #0f172a;
  font-size: 14px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.resource-code,
.mapper-method,
.remark-text {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  color: #64748b;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: bottom;
}

.resource-code,
.mapper-method {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

.mapper-cell {
  min-width: 0;
}

.mapper-method {
  max-width: 300px;
  padding: 3px 7px;
  border-radius: 6px;
  background: #eef2ff;
  color: #3730a3;
  font-weight: 600;
}

.alias-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 38px;
  height: 24px;
  padding: 0 8px;
  border-radius: 7px;
  background: #f1f5f9;
  color: #334155;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  font-weight: 700;
}

.alias-badge.is-empty,
.empty-text {
  color: #94a3b8;
}

.scope-field-tags {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}

.scope-field-tag {
  cursor: help;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

.tooltip-text {
  word-break: break-all;
  white-space: pre-wrap;
}

:global(.scope-column-editor) {
  width: 100%;
  padding: 12px;
  border: 1px solid #dbeafe;
  border-radius: 10px;
  background: linear-gradient(180deg, #f8fbff 0%, #ffffff 100%);
}

:global(.scope-editor-head) {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

:global(.scope-editor-title) {
  color: #1e40af;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  font-weight: 700;
  text-transform: uppercase;
}

:global(.scope-editor-desc) {
  color: #64748b;
  font-size: 12px;
  line-height: 18px;
  text-align: right;
}

:global(.scope-code-input textarea) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 13px;
  line-height: 20px;
}

:global(.scope-editor-section) {
  margin-top: 10px;
  padding: 10px;
  border: 1px solid #e2e8f0;
  border-radius: 9px;
  background: #f8fafc;
}

:global(.scope-editor-section-head) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 8px;
}

:global(.scope-editor-section-title) {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 8px;
  border-radius: 6px;
  background: #e0f2fe;
  color: #0369a1;
  font-size: 12px;
  font-weight: 700;
}

:global(.scope-editor-section-hint) {
  color: #94a3b8;
  font-size: 12px;
}

:global(.scope-editor-example-list) {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

:global(.scope-token-list) {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}

:global(.scope-example-row) {
  width: 100%;
  min-height: 42px;
  padding: 8px;
  display: grid;
  grid-template-columns: 82px minmax(0, 1fr);
  align-items: start;
  column-gap: 10px;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background: #fff;
  color: #334155;
  cursor: pointer;
  transition: all 0.18s ease;
}

:global(.scope-example-row:hover),
:global(.scope-example-row:focus-visible) {
  border-color: #2563eb;
  background: #eff6ff;
  outline: none;
}

:global(.scope-token-button) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-height: 28px;
  padding: 0 9px;
  border: 1px solid #dbeafe;
  border-radius: 7px;
  background: #fff;
  color: #334155;
  cursor: pointer;
  transition: all 0.18s ease;
}

:global(.scope-token-button:hover),
:global(.scope-token-button:focus-visible) {
  border-color: #2563eb;
  background: #eff6ff;
  color: #1d4ed8;
  outline: none;
}

:global(.scope-example-label) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 20px;
  width: 82px;
  padding: 0 6px;
  border-radius: 6px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

:global(.scope-example-code),
:global(.scope-token-button) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
}

:global(.scope-example-code) {
  display: block;
  width: 100%;
  min-height: 26px;
  padding: 4px 7px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: #f8fafc;
  color: #334155;
  line-height: 18px;
  white-space: pre-wrap;
  word-break: break-all;
}

:global(.tone-indigo .scope-editor-title) {
  color: #4338ca;
}

:global(.tone-teal .scope-editor-title) {
  color: #0f766e;
}

:global(.tone-amber .scope-editor-title) {
  color: #b45309;
}

:global(.tone-green .scope-editor-title) {
  color: #15803d;
}

:global(.data-scope-edit-form) {
  padding: 2px 4px 14px;
}

:global(.data-scope-edit-form .n-divider) {
  margin-top: 6px;
  margin-bottom: 14px;
  color: #334155;
  font-weight: 700;
}

:global(.data-scope-edit-form .n-form-item-label__text) {
  color: #334155;
  font-weight: 600;
}

@media (max-width: 1180px) {
  .page-header {
    flex-direction: column;
  }

  .scope-summary {
    min-width: 0;
  }
}

@media (max-width: 768px) {
  .data-scope-config-page {
    padding: 12px;
  }

  .page-header,
  .data-scope-content {
    border-radius: 10px;
  }

  .scope-summary {
    grid-template-columns: 1fr;
  }

  :global(.scope-editor-head) {
    align-items: flex-start;
    flex-direction: column;
    gap: 4px;
  }

  :global(.scope-editor-desc) {
    text-align: left;
  }

  :global(.scope-example-row) {
    grid-template-columns: 1fr;
    row-gap: 6px;
  }

  :global(.scope-example-label) {
    justify-content: flex-start;
    width: auto;
  }
}

.dark .data-scope-config-page {
  background: #0f172a;
  color: #e2e8f0;
}

.dark .page-header,
.dark .data-scope-content {
  background: #111827;
  border-color: #1f2937;
  box-shadow: none;
}

.dark .page-title,
.dark .resource-name,
.dark .summary-value {
  color: #f8fafc;
}

.dark .header-desc,
.dark .summary-label,
.dark .resource-code,
.dark .remark-text {
  color: #94a3b8;
}

.dark .summary-item,
:global(.dark .scope-column-editor),
:global(.dark .scope-editor-section),
:global(.dark .scope-example-row),
:global(.dark .scope-token-button) {
  background: #0f172a;
  border-color: #334155;
}

.dark .mapper-method {
  background: #1e1b4b;
  color: #c4b5fd;
}

.dark .alias-badge {
  background: #1e293b;
  color: #cbd5e1;
}

:global(.dark .scope-example-label) {
  background: #1e3a8a;
  color: #bfdbfe;
}

:global(.dark .scope-example-code) {
  border-color: #334155;
  background: #111827;
  color: #cbd5e1;
}
</style>
