<template>
  <AiCrudPage
    ref="crudRef"
    class="data-scope-page"
    api="/system/dataScopeConfig"
    :api-config="apiConfig"
    :search-schema="searchSchema"
    :columns="tableColumns"
    :edit-schema="editSchema"
    :before-render-form="beforeRenderForm"
    :before-submit="beforeSubmit"
    row-key="id"
    add-button-text="新增规则"
    :show-render-mode-switch="false"
    :table-props="{ showRefresh: false }"
    :load-detail-on-edit="true"
    :edit-grid-cols="1"
    edit-label-placement="left"
    edit-label-align="left"
    edit-label-width="96px"
    form-open-mode="drawer"
    :modal-width="isNarrow ? '100vw' : '680px'"
    :hide-form-section-nav="true"
    :hide-selection="true"
    :hide-batch-delete="true"
    :scroll-x="isNarrow ? 334 : 920"
    :search-grid-cols="isNarrow ? 1 : 3"
    :search-max-visible-fields="2"
    :search-y-gap="8"
    search-label-width="84"
    @submit-success="handleSaved"
  >
    <template #toolbar-end>
      <NTooltip trigger="hover">
        <template #trigger>
          <NButton size="small" :loading="refreshing" :disabled="refreshing" @click="refreshPermissions">
            <template #icon>
              <span class="i-lucide:refresh-cw" aria-hidden="true" />
            </template>
            刷新数据权限
          </NButton>
        </template>
        配置保存后自动更新。相关页面仍显示旧的数据范围时，可手动刷新，再重新查询。
      </NTooltip>
    </template>
    <template #toolbar-right-start>
      <span class="permission-update-hint" role="status">{{ refreshHint }}</span>
      <NButton size="small" quaternary @click="crudRef?.refresh()">
        刷新列表
      </NButton>
    </template>

    <template #form-resourceCode="{ value, updateValue, formData }">
      <NSelect
        :value="value"
        filterable
        tag
        clearable
        :options="pageOptions"
        placeholder="选择要设置数据范围的页面"
        @update:value="(next) => handlePageChange(next, updateValue, formData)"
      />
    </template>

    <template #form-technicalConfig="{ formData }">
      <NCollapse v-model:expanded-names="expandedTechnical">
        <NCollapseItem name="technical" title="技术配置">
          <template #header-extra>
            <span class="technical-summary">{{ formData.mapperMethod ? '已绑定查询方法' : '待完善 · 必填' }}</span>
          </template>
          <div class="technical-fields">
            <p class="technical-intro">
              由技术人员绑定查询方法和数据库字段。日常启停无需修改。
            </p>
            <label class="technical-field">
              <span>查询方法 <span class="required-mark">*</span></span>
              <div class="mapper-field">
                <NInput
                  :value="formData.mapperMethod"
                  type="textarea"
                  :autosize="{ minRows: 2, maxRows: 4 }"
                  placeholder="完整方法名，必须与 XML 中 namespace.方法名 一致，例如：&#10;com.mdframe.forge.plugin.system.mapper.SysUserMapper.selectUserPage"
                  @update:value="value => patchForm(formData, 'mapperMethod', value)"
                />
                <div class="mapper-field__hint">
                  必须与后台查询方法一致，选择适用页面不会自动完成绑定。
                </div>
                <div class="mapper-field__examples">
                  <button
                    v-for="item in mapperExamples"
                    :key="item.value"
                    type="button"
                    class="mapper-example"
                    @click="patchForm(formData, 'mapperMethod', item.value)"
                  >
                    <span>{{ item.label }}</span>
                    <code>{{ shortMapper(item.value) }}</code>
                  </button>
                </div>
              </div>
            </label>
            <label class="technical-field">
              <span>主表别名</span>
              <NInput :value="formData.tableAlias" placeholder="如 t，查询无别名可留空" @update:value="value => patchForm(formData, 'tableAlias', value)" />
            </label>
            <div class="technical-field-grid">
              <label class="technical-field">
                <span>用户区划字段（选填）</span>
                <NInput :value="formData.userRegionColumn" placeholder="关联用户表时使用" @update:value="value => patchForm(formData, 'userRegionColumn', value)" />
              </label>
              <label class="technical-field">
                <span>用户表别名（选填）</span>
                <NInput :value="formData.userTableAlias" placeholder="如 u" @update:value="value => patchForm(formData, 'userTableAlias', value)" />
              </label>
            </div>
          </div>
        </NCollapseItem>
      </NCollapse>
    </template>

    <template #form-scopeRules="{ formData }">
      <DataScopeRuleEditor :form-data="formData" @update="(field, value) => patchForm(formData, field, value)" />
    </template>
  </AiCrudPage>
</template>

<script setup>
import { useMediaQuery } from '@vueuse/core'
import { NButton, NCollapse, NCollapseItem, NDropdown, NInput, NSelect, NTooltip } from 'naive-ui'
import { computed, h, onMounted, ref } from 'vue'
import { AiCrudPage } from '@/components/ai-form'
import SystemTableCell from '@/components/common/SystemTableCell.vue'
import { useDict } from '@/composables/useDict'
import { toNumberDictOptions } from '@/utils/dict-options'
import { request } from '@/utils/request'
import DataScopeRuleEditor from './components/DataScopeRuleEditor.vue'
import DataScopeStatusSwitch from './components/DataScopeStatusSwitch.vue'

defineOptions({ name: 'DataScopeConfig' })

const crudRef = ref(null)
const isNarrow = useMediaQuery('(max-width: 640px)')
const pageOptions = ref([])
const refreshing = ref(false)
const expandedTechnical = ref([])
const refreshHint = ref('保存后自动更新权限')
const { dict } = useDict('sys_enable_disable')
const statusOptions = computed(() => toNumberDictOptions(dict.value.sys_enable_disable))

const apiConfig = {
  list: 'get@/system/dataScopeConfig/page',
  detail: 'post@/system/dataScopeConfig/getById',
  add: 'post@/system/dataScopeConfig/add',
  update: 'post@/system/dataScopeConfig/edit',
  delete: 'post@/system/dataScopeConfig/remove',
}

const mapperExamples = [
  { label: '用户分页', value: 'com.mdframe.forge.plugin.system.mapper.SysUserMapper.selectUserPage' },
  { label: '组织列表', value: 'com.mdframe.forge.plugin.system.mapper.SysOrgMapper.selectOrgList' },
  { label: '流程监控', value: 'com.mdframe.forge.starter.flow.mapper.FlowBusinessMapper.selectMonitorBusinessPage' },
]

const searchSchema = computed(() => [
  { field: 'resourceName', label: '适用页面', type: 'input', props: { placeholder: '搜索页面名称' } },
  { field: 'enabled', label: '规则状态', type: 'select', props: { placeholder: '全部状态', options: statusOptions.value } },
])

const tableColumns = computed(() => [
  {
    prop: 'resourceName',
    label: '适用页面',
    minWidth: isNarrow.value ? 160 : 240,
    render: row => h(SystemTableCell, {
      title: row.resourceName || '-',
      subtitle: row.resourceCode || '未设置编码',
      interactive: true,
      tooltip: `编辑数据范围：${row.resourceName || row.resourceCode || '-'}`,
      onActivate: () => crudRef.value?.showEdit(row),
    }),
  },
  {
    prop: 'scopeFields',
    label: '支持的限制范围',
    minWidth: 200,
    render: renderScopeTags,
  },
  {
    prop: 'enabled',
    label: '规则状态',
    width: isNarrow.value ? 110 : 148,
    render: row => h(DataScopeStatusSwitch, {
      row,
      options: statusOptions.value,
      onUpdated: handleSaved,
      onRefresh: () => crudRef.value?.refresh(),
    }),
  },
  {
    prop: 'remark',
    label: '备注',
    minWidth: 180,
    ellipsis: { tooltip: true },
  },
  {
    prop: 'action',
    label: '操作',
    width: isNarrow.value ? 64 : 150,
    fixed: isNarrow.value ? undefined : 'right',
    render: isNarrow.value ? renderMobileActions : undefined,
    actions: isNarrow.value
      ? undefined
      : [
          { label: '编辑', key: 'edit', type: 'primary', onClick: row => crudRef.value?.showEdit(row) },
          { label: '删除', key: 'delete', type: 'error', onClick: handleDelete },
        ],
  },
].filter(column => !isNarrow.value || !['scopeFields', 'remark'].includes(column.prop)))

function renderMobileActions(row) {
  return h(NDropdown, {
    trigger: 'click',
    options: [{ label: '编辑', key: 'edit' }, { label: '删除', key: 'delete' }],
    onSelect: key => key === 'edit' ? crudRef.value?.showEdit(row) : handleDelete(row),
  }, {
    default: () => h(NButton, {
      'text': true,
      'aria-label': `${row.resourceName || '此规则'}的更多操作`,
    }, { default: () => h('span', { 'class': 'i-lucide:ellipsis', 'aria-hidden': true }) }),
  })
}

const editSchema = computed(() => [
  { type: 'divider', label: '适用页面', span: 1, props: { titlePlacement: 'left' } },
  {
    field: 'resourceCode',
    label: '选择页面',
    type: 'slot',
    slotName: 'resourceCode',
    rules: [{ required: true, message: '请选择适用页面', trigger: 'change' }],
  },
  {
    field: 'resourceName',
    label: '显示名称',
    type: 'input',
    rules: [{ required: true, message: '请输入显示名称', trigger: 'blur' }],
    props: { placeholder: '如：用户管理' },
  },
  { type: 'divider', label: '支持的限制范围', span: 1, props: { titlePlacement: 'left' } },
  {
    field: 'scopeRules',
    label: '过滤规则',
    type: 'slot',
    slotName: 'scopeRules',
    showLabel: false,
  },
  {
    field: 'remark',
    label: '备注',
    type: 'textarea',
    props: { rows: 2, maxlength: 500, placeholder: '说明规则的用途，方便其他管理员理解' },
  },
  {
    field: 'technicalConfig',
    type: 'slot',
    slotName: 'technicalConfig',
    showLabel: false,
  },
])

function renderScopeTags(row) {
  const values = []
  if (hasValue(row.userIdColumn))
    values.push('本人')
  if (hasValue(row.orgIdColumn))
    values.push('本组织')
  if (hasValue(row.regionCodeColumn))
    values.push('本区划')
  if (hasValue(row.tenantIdColumn))
    values.push('租户')
  if (Number(row.flowRelatedVisible) === 1)
    values.push('经手可见')

  if (!values.length)
    return h('span', { class: 'empty-text' }, '未配置过滤')

  return h(SystemTableCell, { values })
}

function hasValue(value) {
  return String(value || '').trim() !== ''
}

function shortMapper(mapperMethod) {
  if (!mapperMethod)
    return '未填写'
  const parts = String(mapperMethod).split('.')
  if (parts.length < 2)
    return mapperMethod
  return `${parts[parts.length - 2]}.${parts[parts.length - 1]}`
}

function patchForm(formData, field, value) {
  formData[field] = value
}

function handlePageChange(next, updateValue, formData) {
  updateValue(next)
  const matched = pageOptions.value.find(item => item.value === next)
  if (matched?.resourceName)
    formData.resourceName = matched.resourceName
}

function beforeRenderForm(row) {
  expandedTechnical.value = row?.mapperMethod ? [] : ['technical']
  if (!row) {
    return {
      enabled: 1,
      tableAlias: 't',
      userIdColumn: 'create_by',
      orgIdColumn: 'org_id',
      tenantIdColumn: 'tenant_id',
      flowRelatedVisible: 0,
      recordIdColumn: 'id',
    }
  }
  return {
    ...row,
    enabled: Number(row.enabled) === 0 ? 0 : 1,
    flowRelatedVisible: Number(row.flowRelatedVisible) === 1 ? 1 : 0,
    recordIdColumn: row.recordIdColumn || 'id',
  }
}

function beforeSubmit(formData) {
  if (!hasValue(formData.userIdColumn) && !hasValue(formData.orgIdColumn)) {
    window.$message?.error('至少配置「按本人限制」或「按组织限制」')
    return false
  }
  if (!hasValue(formData.mapperMethod)) {
    expandedTechnical.value = ['technical']
    window.$message?.error('请完善「技术配置」中的查询方法')
    return false
  }
  const payload = { ...formData }
  delete payload.scopeRules
  delete payload.technicalConfig
  delete payload.enabled
  if (Number(payload.flowRelatedVisible) !== 1) {
    payload.flowRelatedVisible = 0
    payload.flowBusinessType = undefined
    payload.recordIdColumn = undefined
  }
  Object.keys(payload).forEach((key) => {
    if (typeof payload[key] === 'string')
      payload[key] = payload[key].trim()
  })
  return payload
}

async function refreshPermissions() {
  if (refreshing.value) {
    return
  }
  refreshing.value = true
  try {
    const response = await request.post('/system/dataScopeConfig/refreshCache', undefined, { needTip: false })
    if (response?.code !== 200) {
      throw new Error(response?.message || '刷新数据权限失败，请重试')
    }
    refreshHint.value = '数据权限已刷新，请重新查询业务页面'
    window.$message.success('数据权限已刷新，请在相关页面重新查询')
  }
  catch (error) {
    refreshHint.value = '权限刷新未完成，请重试'
    window.$message.error(error?.message || '刷新数据权限失败，请重试')
  }
  finally {
    refreshing.value = false
  }
}

function handleSaved() {
  refreshHint.value = '配置已保存，请重新查询业务页面'
}

function handleDelete(row) {
  window.$dialog.warning({
    title: '确认删除',
    content: `删除「${row.resourceName || row.resourceCode || row.id}」后将移除此规则，查询按系统的未配置策略处理。如需暂时停用，请使用列表中的开关。`,
    positiveText: '确认删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await request.post('/system/dataScopeConfig/remove', null, {
          params: { id: row.id },
        })
        if (res.code === 200) {
          window.$message.success('已删除')
          crudRef.value?.refresh()
        }
      }
      catch {
        window.$message.error('删除失败')
      }
    },
  })
}

function flattenPages(nodes = [], parentName = '', result = []) {
  nodes.forEach((node) => {
    const type = Number(node.resourceType)
    const name = node.resourceName || node.perms || node.path || ''
    const label = parentName && type !== 2 ? `${parentName} / ${name}` : name
    if ((type === 2 || type === 3 || type === 4) && node.perms) {
      result.push({
        label,
        value: node.perms,
        resourceName: node.resourceName || name,
      })
    }
    if (Array.isArray(node.children) && node.children.length)
      flattenPages(node.children, type === 1 || type === 2 ? name : parentName, result)
  })
  return result
}

async function loadPageOptions() {
  try {
    const res = await request.get('/system/resource/tree')
    const list = Array.isArray(res) ? res : (Array.isArray(res?.data) ? res.data : [])
    const seen = new Set()
    pageOptions.value = flattenPages(list).filter((item) => {
      if (!item.value || seen.has(item.value))
        return false
      seen.add(item.value)
      return true
    })
  }
  catch (error) {
    console.error('加载列表选项失败', error)
  }
}

onMounted(() => {
  loadPageOptions()
})
</script>

<style scoped>
.empty-text {
  color: var(--text-tertiary);
  font-size: 12px;
}

.permission-update-hint,
.technical-summary {
  color: var(--text-tertiary);
  font-size: 12px;
}

.technical-fields {
  display: grid;
  gap: 14px;
  padding-top: 4px;
}

.technical-intro {
  margin: 0;
  color: var(--text-tertiary);
  font-size: 12px;
  line-height: 18px;
}

.technical-field {
  display: grid;
  gap: 6px;
  min-width: 0;
  color: var(--text-secondary);
  font-size: 13px;
}

.technical-field-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.required-mark {
  color: var(--error-color);
}

:global(.dark) .data-scope-page :deep(.system-table-cell--interactive .system-table-cell__primary) {
  color: color-mix(in srgb, var(--primary-color) 65%, var(--text-primary));
}

@media (max-width: 640px) {
  .data-scope-page :deep(.ai-table-toolbar) {
    flex-wrap: wrap;
    gap: 8px;
  }

  .data-scope-page :deep(.ai-table-toolbar-left) {
    flex: 1 1 100%;
    min-width: 0;
  }

  .data-scope-page :deep(.ai-table-toolbar-right) {
    margin-left: auto;
  }

  .permission-update-hint {
    display: none;
  }

  .technical-field-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}

.mapper-field {
  display: grid;
  gap: 8px;
  width: 100%;
  min-width: 0;
}

.mapper-field :deep(textarea) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  line-height: 18px;
}

.mapper-field__hint {
  color: var(--text-tertiary);
  font-size: 12px;
  line-height: 18px;
}

.mapper-field__examples {
  display: grid;
  gap: 6px;
}

.mapper-example {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  width: 100%;
  min-width: 0;
  padding: 6px 8px;
  border: 1px solid var(--border-light);
  border-radius: 4px;
  background: var(--bg-primary);
  text-align: left;
  cursor: pointer;
}

.mapper-example:hover {
  border-color: var(--primary-color);
}

.mapper-example span {
  flex-shrink: 0;
  color: var(--text-secondary);
  font-size: 12px;
}

.mapper-example code {
  min-width: 0;
  overflow: hidden;
  color: var(--text-tertiary);
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
