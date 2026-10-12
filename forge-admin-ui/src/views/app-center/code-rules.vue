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
    <!-- 页头 -->
    <header class="code-rule-page__header">
      <div class="code-rule-page__title">
        <h2>编码规则</h2>
        <p>为业务字段配置日期、固定值、流水号与变量组成的编号规则</p>
      </div>
      <div class="code-rule-page__legend" aria-label="分段类型图例">
        <span><i class="legend-dot legend-dot--date" />日期</span>
        <span><i class="legend-dot legend-dot--fixed" />固定值</span>
        <span><i class="legend-dot legend-dot--sequence" />流水号</span>
        <span><i class="legend-dot legend-dot--variable" />变量</span>
      </div>
    </header>

    <!-- 与其它 CRUD 页一致：查询 + 工具栏 + 表格同一面板 -->
    <section class="code-rule-list-panel">
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
        empty-description="创建规则后，可为业务字段自动生成稳定编号"
      >
        <template #toolbar>
          <div class="code-rule-toolbar">
            <n-button v-if="canAdd" type="primary" size="small" @click="openCreate">
              <template #icon>
                <i class="i-material-symbols:add-rounded" />
              </template>
              新增规则
            </n-button>
            <span class="code-rule-toolbar__hint">
              <i class="i-material-symbols:info-outline-rounded" />
              修改规则不会重置已分配的流水号
            </span>
          </div>
        </template>
      </AiCrudPage>
    </section>
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
      dictType: 'sys_code_rule_category',
      ...(categoryOptions.value.length ? { options: categoryOptions.value } : {}),
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
      dictType: 'sys_code_rule_scene',
      ...(sceneOptions.value.length ? { options: sceneOptions.value } : {}),
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
      dictType: 'sys_normal_disable',
      ...(statusOptions.value.length ? { options: statusOptions.value } : {}),
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
        width: 220,
        fixed: 'right',
        render: row => h('div', { class: 'code-rule-actions' }, [
          canEdit.value
            ? actionLink('编辑', 'text-primary', () => openEdit(row), 'i-material-symbols:edit-outline-rounded')
            : null,
          canEdit.value
            ? actionLink(
              Number(row.status) === 1 ? '停用' : '启用',
              Number(row.status) === 1 ? 'text-warning' : 'text-success',
              () => toggleStatus(row),
              Number(row.status) === 1
                ? 'i-material-symbols:pause-circle-outline-rounded'
                : 'i-material-symbols:play-circle-outline-rounded',
            )
            : null,
          canRemove.value
            ? actionLink(
              '删除',
              Number(row.builtin) === 1 ? 'is-disabled' : 'text-error',
              () => Number(row.builtin) === 1
                ? message.info('内置规则不能删除，可停用后新建自定义规则')
                : confirmRemove(row),
              'i-material-symbols:delete-outline-rounded',
            )
            : null,
        ].filter(Boolean)),
      }]
    : []),
])

onMounted(async () => {
  const res = await systemCodeRuleCapabilities()
  capabilities.value = res.data || {}
})

function actionLink(label, className, onClick, icon) {
  return h('button', {
    type: 'button',
    class: ['code-rule-action', className],
    title: label,
    onClick,
  }, [
    icon ? h('i', { class: icon, 'aria-hidden': 'true' }) : null,
    h('span', label),
  ].filter(Boolean))
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
  gap: 8px;
  height: 100%;
  min-height: 0;
  color: var(--text-primary);
}

.code-rule-page__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border: 1px solid var(--border-light);
  border-radius: 6px;
  background: var(--bg-primary);
}

.code-rule-page__title {
  min-width: 0;
}

.code-rule-page__header h2 {
  margin: 0;
  color: var(--text-primary);
  font-size: 16px;
  font-weight: 600;
  line-height: 1.4;
}

.code-rule-page__header p {
  margin: 3px 0 0;
  color: var(--text-tertiary);
  font-size: 12px;
  line-height: 1.4;
}

.code-rule-page__legend {
  display: flex;
  flex: none;
  flex-wrap: wrap;
  gap: 6px;
  color: var(--text-secondary);
  font-size: 12px;
}

.code-rule-page__legend span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 3px 8px;
  border: 1px solid var(--border-light);
  border-radius: 999px;
  background: var(--bg-secondary);
}

.legend-dot {
  width: 7px;
  height: 7px;
  border-radius: 2px;
}

.legend-dot--date {
  background: var(--primary-color);
}

.legend-dot--fixed {
  background: var(--warning-color, #f0a020);
}

.legend-dot--sequence {
  background: var(--success-color, #18a058);
}

.legend-dot--variable {
  background: var(--info-color, #2080f0);
}

.code-rule-list-panel {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  border: 1px solid var(--border-light);
  border-radius: 6px;
  background: var(--bg-primary);
}

.code-rule-list-panel :deep(.ai-crud-page) {
  height: 100%;
}

.code-rule-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.code-rule-toolbar__hint {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  min-width: 0;
  color: var(--text-tertiary);
  font-size: 12px;
}

.code-rule-toolbar__hint > i {
  flex: none;
  font-size: 14px;
}

:deep(.rule-code-cell) {
  display: flex;
  align-items: center;
  gap: 6px;
}

:deep(.rule-code-cell code),
:deep(.rule-template-cell) {
  color: var(--text-secondary);
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 12px;
}

:deep(.code-rule-actions) {
  display: flex;
  align-items: center;
  gap: 4px;
}

:deep(.code-rule-action) {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 2px 6px;
  border: 0;
  border-radius: 4px;
  background: transparent;
  cursor: pointer;
  font: inherit;
  font-size: 13px;
  line-height: 1.4;
  transition:
    color 150ms ease,
    background-color 150ms ease;
}

:deep(.code-rule-action > i) {
  font-size: 15px;
  line-height: 1;
}

:deep(.code-rule-action:hover:not(.is-disabled)) {
  background: var(--bg-secondary);
}

:deep(.code-rule-action.is-disabled) {
  color: var(--text-disabled);
  cursor: not-allowed;
}

@media (max-width: 900px) {
  .code-rule-page__header {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
