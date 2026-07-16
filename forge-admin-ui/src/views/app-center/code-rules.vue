<template>
  <CodeRuleEditorWorkspace
    v-if="editorVisible"
    :rule-id="editingRuleId"
    :capabilities="capabilities"
    :category-options="categoryOptions"
    :scene-options="sceneOptions"
    @close="closeEditor"
    @saved="handleSaved"
  />

  <div v-else class="code-rule-page">
    <header class="code-rule-page__header">
      <div>
        <h2>编码规则</h2>
        <p>配置日期、固定值、流水号和上下文变量，为业务字段生成稳定编号。</p>
      </div>
      <div class="code-rule-page__legend">
        <span><i class="legend-dot legend-dot--date" />日期</span>
        <span><i class="legend-dot legend-dot--fixed" />固定值</span>
        <span><i class="legend-dot legend-dot--sequence" />流水号</span>
        <span><i class="legend-dot legend-dot--variable" />变量</span>
      </div>
    </header>

    <AiCrudPage
      ref="crudRef"
      api="/system/code-rule"
      :api-config="{
        list: 'get@/system/code-rule/page',
      }"
      :search-schema="searchSchema"
      :columns="tableColumns"
      :edit-schema="[]"
      row-key="id"
      :hide-add="true"
      :hide-batch-delete="true"
      :bordered="false"
      empty-title="暂无编码规则"
      empty-description="创建第一条结构化规则，为业务字段提供自动编号"
    >
      <template #toolbar>
        <div class="code-rule-toolbar">
          <n-button v-if="canAdd" type="primary" size="small" @click="openCreate">
            <template #icon>
              <i class="i-material-symbols:add-rounded" />
            </template>
            新增规则
          </n-button>
          <span>规则修改不会重置已分配的数值序列。</span>
        </div>
      </template>
    </AiCrudPage>
  </div>
</template>

<script setup>
import { NTag, useDialog, useMessage } from 'naive-ui'
import { computed, h, nextTick, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  removeSystemCodeRule,
  systemCodeRuleCapabilities,
  updateSystemCodeRuleStatus,
} from '@/api/business-app'
import { AiCrudPage } from '@/components/ai-form'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'
import { useUserStore } from '@/store'
import { hasCodeRulePermission } from './code-rule-utils'
import CodeRuleEditorWorkspace from './components/CodeRuleEditorWorkspace.vue'

defineOptions({ name: 'AppCenterCodeRules' })

const crudRef = ref(null)
const capabilities = ref({})
const dialog = useDialog()
const message = useMessage()
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { dict } = useDict('sys_code_rule_category', 'sys_code_rule_scene', 'sys_normal_disable')

const categoryOptions = computed(() => dict.value.sys_code_rule_category || [])
const sceneOptions = computed(() => dict.value.sys_code_rule_scene || [])
const statusOptions = computed(() => dict.value.sys_normal_disable || [])
const canAdd = computed(() => hasCodeRulePermission(userStore, 'system:codeRule:add'))
const canEdit = computed(() => hasCodeRulePermission(userStore, 'system:codeRule:edit'))
const canRemove = computed(() => hasCodeRulePermission(userStore, 'system:codeRule:remove'))
const editorMode = computed(() => ['create', 'edit'].includes(String(route.query.editor || ''))
  ? String(route.query.editor)
  : '')
const editorVisible = computed(() => (editorMode.value === 'create' && canAdd.value)
  || (editorMode.value === 'edit' && canEdit.value))
const editingRuleId = computed(() => editorMode.value === 'edit' ? route.query.ruleId || null : null)

const searchSchema = computed(() => [
  { field: 'ruleCode', label: '规则编码', type: 'input', props: { placeholder: '输入规则编码' } },
  { field: 'ruleName', label: '规则名称', type: 'input', props: { placeholder: '输入规则名称' } },
  {
    field: 'category',
    label: '编码分类',
    type: 'select',
    props: { options: categoryOptions.value, clearable: true, placeholder: '全部分类' },
  },
  {
    field: 'status',
    label: '状态',
    type: 'select',
    props: { options: statusOptions.value, clearable: true, placeholder: '全部状态' },
  },
])

const tableColumns = computed(() => [
  {
    prop: 'ruleCode',
    label: '规则编码',
    minWidth: 170,
    render: row => h('div', { class: 'rule-code-cell' }, [
      h('code', row.ruleCode || '-'),
      Number(row.builtin) === 1
        ? h(NTag, { size: 'tiny', bordered: false, type: 'info' }, { default: () => '内置' })
        : null,
    ]),
  },
  { prop: 'ruleName', label: '规则名称', minWidth: 150 },
  {
    prop: 'category',
    label: '编码分类',
    width: 120,
    render: row => h(DictTag, {
      options: categoryOptions.value,
      value: row.category,
      size: 'small',
      forceTag: true,
      bordered: false,
    }),
  },
  {
    prop: 'scene',
    label: '适用场景',
    width: 120,
    render: row => h(DictTag, {
      options: sceneOptions.value,
      value: row.scene,
      size: 'small',
      forceTag: true,
      bordered: false,
    }),
  },
  {
    prop: 'template',
    label: '格式摘要',
    minWidth: 240,
    ellipsis: { tooltip: true },
    render: row => h('code', { class: 'rule-template-cell' }, row.template || '-'),
  },
  {
    prop: 'segmentCount',
    label: '分段',
    width: 76,
    render: row => `${Number(row.segmentCount || 0)} 段`,
  },
  {
    prop: 'compatibilityMode',
    label: '配置模式',
    width: 104,
    render: row => h(NTag, {
      size: 'small',
      bordered: false,
      type: row.compatibilityMode === 'LEGACY' ? 'warning' : 'success',
    }, { default: () => row.compatibilityMode === 'LEGACY' ? '待结构化' : '结构化' }),
  },
  {
    prop: 'status',
    label: '状态',
    width: 88,
    render: row => h(DictTag, {
      options: statusOptions.value,
      value: String(row.status),
      size: 'small',
      forceTag: true,
      bordered: false,
    }),
  },
  {
    prop: 'updateTime',
    label: '更新时间',
    width: 168,
  },
  ...((canEdit.value || canRemove.value)
    ? [{
        prop: 'actions',
        label: '操作',
        width: 210,
        fixed: 'right',
        render: row => h('div', { class: 'code-rule-actions' }, [
          canEdit.value ? actionLink('编辑', 'text-primary', () => openEdit(row)) : null,
          canEdit.value
            ? actionLink(Number(row.status) === 1 ? '停用' : '启用', Number(row.status) === 1 ? 'text-warning' : 'text-success', () => toggleStatus(row))
            : null,
          canRemove.value
            ? actionLink('删除', Number(row.builtin) === 1 ? 'is-disabled' : 'text-error', () => Number(row.builtin) === 1 ? message.info('内置规则不能删除，可停用后新建自定义规则') : confirmRemove(row))
            : null,
        ].filter(Boolean)),
      }]
    : []),
])

onMounted(async () => {
  const res = await systemCodeRuleCapabilities()
  capabilities.value = res.data || {}
})

function actionLink(label, className, onClick) {
  return h('a', {
    class: ['code-rule-action', className],
    onClick,
  }, label)
}

function openCreate() {
  openEditor('create')
}

function openEdit(row) {
  openEditor('edit', row.id)
}

function openEditor(mode, ruleId = null) {
  const query = { ...route.query, editor: mode }
  delete query.ruleId
  if (ruleId)
    query.ruleId = String(ruleId)
  router.push({ path: route.path, query })
}

async function closeEditor() {
  const query = { ...route.query }
  delete query.editor
  delete query.ruleId
  await router.replace({ path: route.path, query })
}

async function toggleStatus(row) {
  await updateSystemCodeRuleStatus({
    id: row.id,
    versionNo: row.versionNo,
    status: Number(row.status) === 1 ? 0 : 1,
  })
  message.success(Number(row.status) === 1 ? '编码规则已停用' : '编码规则已启用')
  crudRef.value?.refresh()
}

function confirmRemove(row) {
  dialog.warning({
    title: '删除编码规则',
    content: `确认删除“${row.ruleName || row.ruleCode}”吗？已有业务字段绑定将无法继续生成编号。`,
    positiveText: '删除',
    negativeText: '取消',
    positiveButtonProps: { type: 'error' },
    async onPositiveClick() {
      await removeSystemCodeRule(row.id)
      message.success('编码规则已删除')
      crudRef.value?.refresh()
    },
  })
}

async function handleSaved() {
  await closeEditor()
  await nextTick()
  crudRef.value?.refresh()
}
</script>

<style scoped>
.code-rule-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
  box-sizing: border-box;
  height: 100%;
  min-height: 0;
  padding: 16px;
  color: var(--text-primary, #1d2129);
  background: var(--bg-secondary, #f7f8fa);
}

.code-rule-page__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 2px 2px 0;
}

.code-rule-page__header h2 {
  margin: 0;
  color: var(--text-primary, #1d2129);
  font-size: 18px;
  font-weight: 680;
}

.code-rule-page__header p {
  margin: 4px 0 0;
  color: var(--text-tertiary, #86909c);
  font-size: 13px;
}

.code-rule-page__legend {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
  color: var(--text-tertiary, #86909c);
  font-size: 12px;
}

.code-rule-page__legend span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.legend-dot {
  width: 7px;
  height: 7px;
  border-radius: 2px;
}

.legend-dot--date {
  background: var(--primary-color, #4242f7);
}
.legend-dot--fixed {
  background: var(--warning-500, #f59e0b);
}
.legend-dot--sequence {
  background: var(--success-500, #10b981);
}
.legend-dot--variable {
  background: var(--info-500, #0ea5e9);
}

.code-rule-page :deep(.ai-crud-page) {
  flex: 1;
  min-height: 0;
}

.code-rule-page :deep(.ai-crud-table) {
  border-radius: 10px;
  border: 1px solid var(--border-light, #e5e6eb);
  background: var(--bg-primary, #fff);
}

.code-rule-toolbar {
  display: flex;
  align-items: center;
  gap: 14px;
}

.code-rule-toolbar > span {
  color: var(--text-tertiary, #86909c);
  font-size: 12px;
}

:deep(.rule-code-cell) {
  display: flex;
  align-items: center;
  gap: 7px;
}

:deep(.rule-code-cell code),
:deep(.rule-template-cell) {
  color: var(--text-secondary, #4e5969);
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 12px;
}

:deep(.code-rule-actions) {
  display: flex;
  align-items: center;
  gap: 14px;
}

:deep(.code-rule-action) {
  cursor: pointer;
  font-size: 13px;
}

:deep(.code-rule-action.is-disabled) {
  color: var(--text-disabled, #c9cdd4);
  cursor: not-allowed;
}

@media (max-width: 900px) {
  .code-rule-page__header {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
