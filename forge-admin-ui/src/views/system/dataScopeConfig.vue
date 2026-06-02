<template>
  <div class="data-scope-config-page">
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
        edit-label-placement="left"
        edit-label-align="left"
        edit-label-width="110"
        edit-form-class="data-scope-edit-form"
        modal-width="900px"
        :hide-selection="true"
        :striped="true"
        :scroll-x="1100"
        :search-grid-cols="3"
        :search-max-visible-fields="3"
        :search-y-gap="8"
        search-label-width="84"
      >
        <template #toolbar-left>
          <div class="page-tip">
            <i class="i-material-symbols:info-outline" />
            <span>配置哪些 SQL 查询需要数据权限过滤，系统会根据用户角色自动追加 WHERE 条件实现数据隔离</span>
          </div>
        </template>

        <template #form-mapperMethod="{ value: mapperValue, updateValue }">
          <ScopeColumnEditor
            :value="mapperValue"
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
            description="简单模式填字段名，复杂模式用 <sql> 开头"
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
            description="支持当前组织、下级组织和自定义组织集合"
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
            description="支持本级、下级和祖先区划变量"
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
      props.description ? h('div', { class: 'scope-editor-desc' }, props.description) : null,
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
    width: 240,
    render: renderResourceCell,
  },
  {
    prop: 'mapperMethod',
    label: 'Mapper 精确匹配',
    width: 280,
    render: renderMapperCell,
  },
  {
    prop: 'tableAlias',
    label: '表别名',
    width: 90,
    render: renderAliasCell,
  },
  {
    prop: 'scopeFields',
    label: '权限字段',
    width: 300,
    render: renderScopeFields,
  },
  {
    prop: 'enabled',
    label: '状态',
    width: 80,
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
    width: 140,
    render: renderRemark,
  },
  {
    prop: 'action',
    label: '操作',
    width: 120,
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
    label: '① 绑定查询接口',
    props: {
      titlePlacement: 'left',
    },
    span: 2,
  },
  {
    field: 'resourceCode',
    label: '资源编码',
    type: 'input',
    span: 1,
    rules: [{ required: true, message: '请输入资源编码', trigger: 'blur' }],
    props: {
      placeholder: '如 system:user:list',
    },
  },
  {
    field: 'resourceName',
    label: '资源名称',
    type: 'input',
    span: 1,
    rules: [{ required: true, message: '请输入资源名称', trigger: 'blur' }],
    props: {
      placeholder: '如 用户列表查询',
    },
  },
  {
    field: 'mapperMethod',
    label: 'Mapper方法',
    type: 'slot',
    span: 2,
    rules: [{ required: true, message: '请输入Mapper方法', trigger: 'blur' }],
    props: {
      placeholder: 'com.mdframe.forge...Mapper.selectXxx',
      rows: 2,
    },
  },
  {
    field: 'tableAlias',
    label: '主表别名',
    type: 'input',
    span: 1,
    defaultValue: 't',
    props: {
      placeholder: '如 t',
    },
  },
  {
    field: 'enabled',
    label: '是否启用',
    type: 'radioButton',
    span: 1,
    defaultValue: 1,
    props: {
      options: enabledOptions,
      clearable: false,
    },
  },
  {
    type: 'divider',
    label: '② 配置过滤字段（按需填写）',
    props: {
      titlePlacement: 'left',
    },
    span: 2,
  },
  {
    field: 'userIdColumn',
    label: '用户字段',
    type: 'slot',
    span: 2,
    defaultValue: 'user_id',
    rules: [{ required: true, message: '请输入用户ID字段', trigger: 'blur' }],
    props: {
      placeholder: 'user_id 或 create_by',
      rows: 3,
      type: 'textarea',
    },
  },
  {
    field: 'orgIdColumn',
    label: '组织字段',
    type: 'slot',
    span: 2,
    defaultValue: 'org_id',
    rules: [{ required: true, message: '请输入组织ID字段', trigger: 'blur' }],
    props: {
      placeholder: 'org_id 或 dept_id',
      rows: 3,
      type: 'textarea',
    },
  },
  {
    field: 'tenantIdColumn',
    label: '租户字段',
    type: 'slot',
    span: 2,
    defaultValue: 'tenant_id',
    rules: [{ required: true, message: '请输入租户ID字段', trigger: 'blur' }],
    props: {
      placeholder: 'tenant_id',
      rows: 3,
      type: 'textarea',
    },
  },
  {
    field: 'regionCodeColumn',
    label: '区划字段',
    type: 'slot',
    span: 2,
    props: {
      placeholder: 'region_code 或 area_code（选填）',
      rows: 3,
      type: 'textarea',
    },
  },
  {
    field: 'userRegionColumn',
    label: '用户区划字段',
    type: 'input',
    span: 1,
    props: {
      placeholder: '选填，如 area_code',
    },
  },
  {
    field: 'userTableAlias',
    label: '用户表别名',
    type: 'input',
    span: 1,
    props: {
      placeholder: '选填，如 u',
    },
  },
  {
    type: 'divider',
    label: '③ 备注',
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
      placeholder: '描述该配置的用途或注意事项',
      rows: 2,
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
  display: flex;
  flex-direction: column;
}

.data-scope-content {
  flex: 1;
  min-height: 0;
  background: #fff;
  border-radius: 8px;
}

.data-scope-content :deep(.ai-crud-page) {
  height: 100%;
}

/* 表格单元格样式 */
.resource-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.resource-name-line {
  display: flex;
  align-items: center;
  gap: 6px;
}

.resource-name {
  overflow: hidden;
  color: #1f2937;
  font-size: 13px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.resource-code,
.mapper-method,
.remark-text {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  color: #6b7280;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.resource-code,
.mapper-method {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

.mapper-cell {
  min-width: 0;
}

.mapper-method {
  max-width: 260px;
  padding: 2px 6px;
  border-radius: 4px;
  background: #f3f4f6;
  color: #4338ca;
  font-size: 12px;
  font-weight: 500;
}

.alias-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 32px;
  height: 22px;
  padding: 0 6px;
  border-radius: 4px;
  background: #f3f4f6;
  color: #374151;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  font-weight: 600;
}

.alias-badge.is-empty,
.empty-text {
  color: #9ca3af;
}

.scope-field-tags {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
}

.scope-field-tag {
  cursor: help;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 11px;
}

.tooltip-text {
  word-break: break-all;
  white-space: pre-wrap;
}

/* ScopeColumnEditor 编辑器样式 */
:global(.scope-column-editor) {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fafbfc;
}

:global(.scope-editor-desc) {
  margin-bottom: 6px;
  color: #9ca3af;
  font-size: 11px;
  line-height: 16px;
}

:global(.scope-code-input textarea) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  line-height: 18px;
}

:global(.scope-editor-section) {
  margin-top: 8px;
  padding: 8px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
}

:global(.scope-editor-section-head) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 6px;
}

:global(.scope-editor-section-title) {
  color: #6b7280;
  font-size: 11px;
  font-weight: 600;
}

:global(.scope-editor-section-hint) {
  color: #9ca3af;
  font-size: 11px;
}

:global(.scope-editor-example-list) {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

:global(.scope-token-list) {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
}

:global(.scope-example-row) {
  width: 100%;
  padding: 6px 8px;
  display: flex;
  align-items: center;
  gap: 8px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
  cursor: pointer;
  transition:
    border-color 0.15s,
    background-color 0.15s;
}

:global(.scope-example-row:hover) {
  border-color: #6366f1;
  background: #f5f3ff;
}

:global(.scope-token-button) {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 8px;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  background: #fff;
  color: #374151;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 11px;
  cursor: pointer;
  transition:
    border-color 0.15s,
    background-color 0.15s;
}

:global(.scope-token-button:hover) {
  border-color: #6366f1;
  background: #f5f3ff;
  color: #4f46e5;
}

:global(.scope-example-label) {
  flex-shrink: 0;
  padding: 2px 6px;
  border-radius: 4px;
  background: #eff6ff;
  color: #2563eb;
  font-size: 11px;
  font-weight: 600;
  white-space: nowrap;
}

:global(.scope-example-code) {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 11px;
  color: #4b5563;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 编辑器色调 */
:global(.tone-indigo) {
  border-left: 3px solid #4338ca;
}
:global(.tone-blue) {
  border-left: 3px solid #2563eb;
}
:global(.tone-teal) {
  border-left: 3px solid #0f766e;
}
:global(.tone-amber) {
  border-left: 3px solid #b45309;
}
:global(.tone-green) {
  border-left: 3px solid #15803d;
}

/* 页面提示 */
.page-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 6px;
  background: #eff6ff;
  color: #2563eb;
  font-size: 12px;
  line-height: 18px;
}

.page-tip i {
  flex-shrink: 0;
  font-size: 14px;
}

/* 编辑表单样式 */
:global(.data-scope-edit-form) {
  padding: 4px 8px 12px;
}

:global(.data-scope-edit-form .n-form-item-label) {
  text-align: left;
  justify-content: flex-start;
}

:global(.data-scope-edit-form .n-divider) {
  margin-top: 8px;
  margin-bottom: 12px;
}

:global(.data-scope-edit-form .n-divider .n-divider__title) {
  color: #374151;
  font-weight: 600;
  font-size: 13px;
}

:global(.data-scope-edit-form .n-form-item-label__text) {
  color: #374151;
  font-weight: 500;
  font-size: 13px;
}

/* 深色模式 */
.dark .data-scope-content {
  background: #111827;
}

.dark .resource-name {
  color: #f3f4f6;
}

.dark .resource-code,
.dark .remark-text {
  color: #9ca3af;
}

.dark .mapper-method {
  background: #1e1b4b;
  color: #c4b5fd;
}

.dark .alias-badge {
  background: #1f2937;
  color: #d1d5db;
}

:global(.dark .scope-column-editor) {
  background: #1f2937;
  border-color: #374151;
}

:global(.dark .scope-editor-section) {
  background: #111827;
  border-color: #374151;
}

:global(.dark .scope-example-row) {
  background: #111827;
  border-color: #374151;
}

:global(.dark .scope-example-row:hover) {
  border-color: #6366f1;
  background: #1e1b4b;
}

:global(.dark .scope-token-button) {
  background: #111827;
  border-color: #374151;
  color: #d1d5db;
}

:global(.dark .scope-token-button:hover) {
  border-color: #6366f1;
  background: #1e1b4b;
  color: #a5b4fc;
}

:global(.dark .scope-example-label) {
  background: #1e3a8a;
  color: #bfdbfe;
}

:global(.dark .scope-example-code) {
  color: #d1d5db;
}
</style>
