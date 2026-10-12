<template>
  <div class="business-object-table">
    <section v-for="group in groups" :key="group.key" class="object-group">
      <button
        class="group-header"
        :class="groupToneClass(group.groupObject)"
        type="button"
        @click="toggleGroup(group.key)"
      >
        <span class="group-toggle">
          <n-icon>
            <ChevronForwardOutline v-if="isCollapsed(group.key)" />
            <ChevronDownOutline v-else />
          </n-icon>
        </span>
        <span class="group-icon">
          <IconRenderer v-if="group.groupObject?.icon" :icon="group.groupObject.icon" :size="18" />
          <n-icon v-else :component="CubeOutline" />
        </span>
        <span class="group-copy">
          <span class="group-title-line">
            <strong>{{ objectTitle(group.groupObject) }}</strong>
            <DictTag
              v-if="group.groupObject?.objectType && !group.synthetic"
              dict-type="ai_business_object_type"
              :value="group.groupObject.objectType"
              :bordered="false"
            />
          </span>
          <small v-if="showSuite && group.groupObject?.suiteName">{{ group.groupObject.suiteName }}</small>
        </span>
        <span class="group-stats">
          <span>{{ group.children.length }} 个关系</span>
          <span>{{ group.entryCount }} 个入口</span>
        </span>
      </button>

      <div v-show="!isCollapsed(group.key)" class="object-table-shell">
        <div class="object-table-head">
          <span>类型</span>
          <span>名称</span>
          <span>状态</span>
          <span>入口</span>
          <span>描述</span>
          <span>操作</span>
        </div>

        <template v-for="row in groupRows(group)" :key="row.key">
          <div
            class="object-table-row"
            :class="{
              'child-row': row.level > 0,
              'synthetic-row': row.synthetic,
              'entry-open': isEntryExpanded(row.key),
            }"
          >
            <span class="type-cell">
              <span class="type-chip" :class="rowRoleClass(row)">
                <n-icon :component="rowRoleIcon(row)" />
                {{ rowRoleLabel(row) }}
              </span>
            </span>

            <span class="name-cell">
              <button
                class="object-name-button"
                type="button"
                :disabled="!row.object?.objectCode"
                @click="emit('openObject', row.object)"
              >
                <span class="row-object-icon">
                  <IconRenderer v-if="row.object?.icon" :icon="row.object.icon" :size="16" />
                  <n-icon v-else :component="CubeOutline" />
                </span>
                <span>
                  <strong>{{ objectTitle(row.object) }}</strong>
                  <small>{{ row.object?.objectCode || row.relation?.targetObjectCode || '-' }}</small>
                </span>
              </button>
            </span>

            <span class="status-cell">
              <DictTag
                v-if="row.object?.status !== undefined && !row.synthetic"
                dict-type="sys_enable_disable"
                :value="row.object.status"
                :bordered="false"
              />
              <n-tag v-else size="small" :bordered="false">配置项</n-tag>
            </span>

            <span class="entry-count-cell">
              <button
                v-if="row.apps.length"
                class="entry-count-button"
                type="button"
                @click="toggleEntry(row.key)"
              >
                {{ row.apps.length }} 个
              </button>
              <span v-else class="empty-value">-</span>
            </span>

            <span class="description-cell">
              {{ row.object?.description || row.relation?.description || '集中管理表单、列表、流程和访问入口。' }}
            </span>

            <span class="action-cell">
              <n-button
                secondary
                size="small"
                :disabled="!canManageObject(row.object)"
                @click="emit('designObject', row.object, primaryDesignPanel(row.object))"
              >
                <template #icon>
                  <n-icon><BuildOutline /></n-icon>
                </template>
                设计
              </n-button>
              <n-dropdown
                trigger="click"
                :options="objectOptions(row)"
                @select="key => handleObjectSelect(key, row)"
              >
                <n-button
                  quaternary
                  circle
                  size="small"
                  :disabled="!canManageObject(row.object)"
                  aria-label="更多对象操作"
                >
                  <template #icon>
                    <n-icon><EllipsisHorizontal /></n-icon>
                  </template>
                </n-button>
              </n-dropdown>
            </span>
          </div>

          <div v-if="isEntryExpanded(row.key)" class="entry-expand-row">
            <div class="entry-list">
              <div v-for="app in row.apps" :key="app.id" class="entry-row">
                <button
                  class="entry-main"
                  type="button"
                  :disabled="isOpenDisabled(app)"
                  @click="emit('openApp', app)"
                >
                  <span class="entry-icon">
                    <IconRenderer v-if="app.icon" :icon="app.icon" :size="16" />
                    <n-icon v-else><OpenOutline /></n-icon>
                  </span>
                  <span class="entry-copy">
                    <strong>{{ app.appName || app.appCode }}</strong>
                    <small>{{ entryModeLabel(app) }}</small>
                  </span>
                  <span class="entry-tags">
                    <n-tag v-if="isCodeDownload(app)" size="small" type="info" :bordered="false">
                      下载代码
                    </n-tag>
                    <DictTag dict-type="sys_enable_disable" :value="app.status" :bordered="false" />
                  </span>
                </button>
                <span class="entry-actions">
                  <n-button secondary size="small" @click="emit('configApp', app)">
                    编辑入口
                  </n-button>
                  <n-dropdown trigger="click" :options="appOptions(app)" @select="key => handleAppSelect(key, app)">
                    <n-button quaternary circle size="small" aria-label="更多入口操作">
                      <template #icon>
                        <n-icon><EllipsisHorizontal /></n-icon>
                      </template>
                    </n-button>
                  </n-dropdown>
                </span>
              </div>
            </div>
          </div>
        </template>
      </div>
    </section>
  </div>
</template>

<script setup>
import {
  BuildOutline,
  ChevronDownOutline,
  ChevronForwardOutline,
  CubeOutline,
  DocumentTextOutline,
  EllipsisHorizontal,
  GitBranchOutline,
  ListOutline,
  OpenOutline,
} from '@vicons/ionicons5'
import { ref } from 'vue'
import DictTag from '@/components/DictTag.vue'
import IconRenderer from '@/components/IconRenderer.vue'

defineProps({
  groups: {
    type: Array,
    default: () => [],
  },
  showSuite: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits([
  'openObject',
  'editObject',
  'designObject',
  'statsObject',
  'toggleObject',
  'deleteObject',
  'openApp',
  'codeApp',
  'configApp',
  'toggleApp',
  'deleteApp',
  'createApp',
])

const collapsedGroups = ref(new Set())
const expandedEntries = ref(new Set())

function groupRows(group) {
  return [
    {
      key: `${group.key}:main`,
      level: 0,
      object: group.groupObject,
      relation: null,
      apps: group.apps || [],
      synthetic: group.synthetic,
      role: 'main',
    },
    ...(group.children || []).map((child, index) => ({
      key: `${group.key}:child:${child.object?.objectCode || child.relation?.targetObjectCode || index}`,
      level: 1,
      object: child.object,
      relation: child.relation,
      apps: child.apps || [],
      synthetic: child.synthetic,
      role: 'child',
    })),
  ]
}

function isCollapsed(key) {
  return collapsedGroups.value.has(key)
}

function toggleGroup(key) {
  const next = new Set(collapsedGroups.value)
  if (next.has(key))
    next.delete(key)
  else
    next.add(key)
  collapsedGroups.value = next
}

function isEntryExpanded(key) {
  return expandedEntries.value.has(key)
}

function toggleEntry(key) {
  const next = new Set(expandedEntries.value)
  if (next.has(key))
    next.delete(key)
  else
    next.add(key)
  expandedEntries.value = next
}

function objectTitle(object) {
  return object?.objectName || object?.objectCode || '独立访问入口'
}

function groupToneClass(object) {
  return normalizeObjectType(object) === 'TRANSACTION' ? 'transaction-group' : 'master-group'
}

function rowRoleLabel(row) {
  if (row.role === 'main') {
    if (normalizeObjectType(row.object) === 'TRANSACTION')
      return '单据'
    if (normalizeObjectType(row.object) === 'LOOKUP')
      return '查找'
    if (row.synthetic)
      return '入口'
    return '主表'
  }
  const relationType = normalizeRelationType(row.relation)
  if (relationType === 'REFERENCE')
    return '引用'
  if (relationType === 'MANY_TO_MANY')
    return '多对多'
  return '明细'
}

function rowRoleClass(row) {
  if (row.role === 'main') {
    if (normalizeObjectType(row.object) === 'TRANSACTION')
      return 'transaction'
    if (normalizeObjectType(row.object) === 'LOOKUP')
      return 'lookup'
    if (row.synthetic)
      return 'entry'
    return 'master'
  }
  return normalizeRelationType(row.relation) === 'REFERENCE' ? 'reference' : 'detail'
}

function rowRoleIcon(row) {
  if (row.role === 'main') {
    if (normalizeObjectType(row.object) === 'TRANSACTION')
      return DocumentTextOutline
    if (row.synthetic)
      return OpenOutline
    return CubeOutline
  }
  return normalizeRelationType(row.relation) === 'REFERENCE' ? GitBranchOutline : ListOutline
}

function normalizeObjectType(object) {
  return String(object?.objectType || '').toUpperCase()
}

function normalizeRelationType(relation) {
  return String(relation?.relationType || '').toUpperCase()
}

function canManageObject(object) {
  return Boolean(object?.id)
}

function primaryDesignPanel(object) {
  return isCodeAppMeta(object) ? 'flow-app' : 'form'
}

function objectOptions(row) {
  const object = row?.object || {}
  const primaryApp = primaryEntryApp(row)
  return [
    {
      label: '打开业务单元',
      key: 'open',
      disabled: !object?.objectCode,
    },
    {
      label: '报表看板',
      key: 'stats',
      disabled: !canManageObject(object),
    },
    {
      type: 'divider',
      key: 'divider-view',
    },
    {
      label: '编辑业务单元',
      key: 'edit',
      disabled: !canManageObject(object),
    },
    {
      label: '业务流程配置',
      key: 'flow-app',
      disabled: !canManageObject(object),
    },
    {
      type: 'divider',
      key: 'divider-config',
    },
    {
      label: primaryApp ? '编辑访问入口' : '新增访问入口',
      key: primaryApp ? 'edit-app' : 'create-app',
      disabled: !object?.objectCode && !primaryApp,
    },
    {
      type: 'divider',
      key: 'divider-capability',
    },
    {
      label: object?.status === 1 ? '停用业务单元' : '启用业务单元',
      key: 'toggle',
      disabled: !canManageObject(object),
    },
    {
      type: 'divider',
      key: 'divider',
    },
    {
      label: '删除业务单元',
      key: 'delete',
      disabled: !canManageObject(object),
    },
  ]
}

function handleObjectSelect(key, row) {
  const object = row?.object || {}
  if (key === 'open') {
    emit('openObject', object)
    return
  }
  if (key === 'edit') {
    emit('editObject', object)
    return
  }
  if (key === 'flow-app') {
    emit('designObject', object, key)
    return
  }
  if (key === 'stats') {
    emit('statsObject', object)
    return
  }
  if (key === 'create-app') {
    emit('createApp', object)
    return
  }
  if (key === 'edit-app') {
    const app = primaryEntryApp(row)
    if (app)
      emit('configApp', app)
    return
  }
  if (key === 'toggle') {
    emit('toggleObject', object)
    return
  }
  if (key === 'delete')
    emit('deleteObject', object)
}

function primaryEntryApp(row) {
  const apps = Array.isArray(row?.apps) ? row.apps : []
  if (!apps.length)
    return null
  return apps.find(app => app?.entryMode === 'RUNTIME') || apps[0]
}

function appOptions(app) {
  const options = [
    {
      label: '编辑入口',
      key: 'config',
    },
  ]
  if (isCodeDownload(app)) {
    options.push({
      label: '功能代码',
      key: 'code',
    })
  }
  options.push(
    {
      type: 'divider',
      key: 'divider-status',
    },
    {
      label: app.status === 1 ? '停用入口' : '启用入口',
      key: 'toggle',
    },
    {
      type: 'divider',
      key: 'divider',
    },
    {
      label: '删除入口',
      key: 'delete',
    },
  )
  return options
}

function handleAppSelect(key, app) {
  if (key === 'config') {
    emit('configApp', app)
    return
  }
  if (key === 'code') {
    emit('codeApp', app)
    return
  }
  if (key === 'toggle') {
    emit('toggleApp', app)
    return
  }
  if (key === 'delete')
    emit('deleteApp', app)
}

function isOpenDisabled(app) {
  if (app.status !== 1)
    return true
  if (isCodeDownload(app))
    return !app.id
  if (app.entryMode === 'RUNTIME')
    return !app.configKey && !app.entryUrl
  return !app.entryUrl
}

function entryModeLabel(app) {
  if (isCodeDownload(app))
    return '下载代码'
  if (app.entryMode === 'RUNTIME')
    return '业务页面'
  if (app.entryMode === 'ROUTE')
    return '系统页面'
  if (app.entryMode === 'IFRAME')
    return '内嵌页面'
  if (app.entryMode === 'EXTERNAL')
    return '外部链接'
  if (app.entryMode === 'H5')
    return '移动端'
  if (app.entryMode === 'API')
    return '接口能力'
  return app.entryMode || '入口'
}

function isCodeDownload(app) {
  return app?.entryMode === 'RUNTIME' && app?.appMode === 'CODE_DOWNLOAD'
}

function isCodeAppMeta(object) {
  return hasCodeAppFlag(object?.options) || hasCodeAppFlag(object?.designerOptions)
}

function hasCodeAppFlag(value) {
  if (!value)
    return false
  if (typeof value === 'object')
    return value.codeApp === true
  if (typeof value !== 'string')
    return false
  const compactValue = value.replace(/\s/g, '')
  if (compactValue.includes('"codeApp":true'))
    return true
  try {
    return JSON.parse(value)?.codeApp === true
  }
  catch {
    return false
  }
}
</script>

<style scoped>
.business-object-table {
  display: grid;
  gap: 10px;
  min-width: 0;
}

.object-group {
  min-width: 0;
  overflow: hidden;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: var(--bg-primary);
}

.group-header {
  display: grid;
  grid-template-columns: 26px 30px minmax(0, 1fr) auto;
  gap: 8px;
  align-items: center;
  width: 100%;
  min-height: 40px;
  cursor: pointer;
  border: 0;
  border-left: 3px solid #2080f0;
  background: rgb(0 0 0 / 2%);
  padding: 6px 10px 6px 8px;
  text-align: left;
}

.group-header.transaction-group {
  border-left-color: #f0a020;
}

.group-toggle,
.group-icon,
.row-object-icon,
.entry-icon {
  display: grid;
  place-items: center;
}

.group-toggle {
  width: 24px;
  height: 24px;
  color: var(--text-secondary);
}

.group-icon {
  width: 30px;
  height: 30px;
  border-radius: 6px;
  background: #eef6ff;
  color: #2563eb;
}

.transaction-group .group-icon {
  background: #fff7ed;
  color: #ea580c;
}

.group-copy,
.group-title-line {
  min-width: 0;
}

.group-title-line {
  display: flex;
  gap: 8px;
  align-items: center;
}

.group-title-line strong,
.group-copy small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.group-title-line strong {
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 650;
  line-height: 1.3;
}

.group-copy small {
  display: block;
  margin-top: 1px;
  color: var(--text-secondary);
  font-size: 11px;
}

.group-stats {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 6px;
  justify-content: flex-end;
  color: var(--text-secondary);
  font-size: 12px;
}

.group-stats span {
  border-radius: 4px;
  background: var(--bg-primary);
  line-height: 22px;
  padding: 0 7px;
}

.object-table-shell {
  position: relative;
  overflow-x: auto;
}

.object-table-head,
.object-table-row {
  display: grid;
  grid-template-columns: 116px minmax(210px, 1.35fr) 104px 82px minmax(220px, 1fr) 160px;
  min-width: 960px;
  align-items: center;
}

.object-table-head {
  min-height: 34px;
  border-top: 1px solid #eef2f7;
  border-bottom: 1px solid #eef2f7;
  background: var(--bg-secondary);
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 600;
}

.object-table-head span,
.object-table-row > span {
  min-width: 0;
  padding: 0 10px;
}

.object-table-head span:last-child,
.action-cell {
  position: sticky;
  right: 0;
  z-index: 2;
  min-height: inherit;
  border-left: 1px solid #eef2f7;
  background: var(--bg-secondary);
  box-shadow: -8px 0 12px -12px rgb(15 23 42 / 40%);
}

.object-table-row .action-cell {
  background: var(--bg-primary);
}

.object-table-row:hover .action-cell,
.object-table-row.entry-open .action-cell {
  background: var(--bg-secondary);
}

.object-table-row.child-row .action-cell {
  background: #fbfdff;
}

.object-table-row.child-row:hover .action-cell,
.object-table-row.child-row.entry-open .action-cell {
  background: #f4f8ff;
}

.object-table-row {
  min-height: 44px;
  border-bottom: 1px solid #f1f5f9;
  transition: background 160ms ease;
}

.object-table-row:hover,
.object-table-row.entry-open {
  background: rgb(0 0 0 / 3%);
}

.object-table-row.child-row {
  background: #fbfdff;
}

.object-table-row.child-row:hover,
.object-table-row.child-row.entry-open {
  background: #f4f8ff;
}

.type-cell,
.status-cell,
.entry-count-cell,
.action-cell {
  display: inline-flex;
  align-items: center;
}

.type-cell {
  padding-left: 12px;
}

.child-row .type-cell {
  padding-left: 34px;
}

.type-chip {
  display: inline-flex;
  gap: 5px;
  align-items: center;
  height: 24px;
  border-radius: 5px;
  background: #eef6ff;
  color: #2563eb;
  font-size: 12px;
  line-height: 24px;
  padding: 0 7px;
}

.type-chip.transaction {
  background: #fff7ed;
  color: #ea580c;
}

.type-chip.detail {
  background: #f0fdf4;
  color: #16a34a;
}

.type-chip.reference,
.type-chip.lookup {
  background: var(--bg-secondary);
  color: var(--text-secondary);
}

.type-chip.entry {
  background: #ecfdf5;
  color: #15803d;
}

.object-name-button {
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr);
  gap: 8px;
  align-items: center;
  width: 100%;
  min-width: 0;
  cursor: pointer;
  border: 0;
  background: transparent;
  padding: 0;
  text-align: left;
}

.object-name-button:disabled {
  cursor: default;
}

.row-object-icon {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  background: var(--bg-tertiary);
  color: #2563eb;
}

.object-name-button strong,
.object-name-button small,
.description-cell,
.entry-copy strong,
.entry-copy small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.object-name-button strong {
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 650;
}

.object-name-button small {
  margin-top: 1px;
  color: var(--text-secondary);
  font-size: 11px;
}

.description-cell {
  color: var(--text-secondary);
  font-size: 12px;
}

.entry-count-button {
  cursor: pointer;
  border: 0;
  border-radius: 4px;
  background: #eef6ff;
  color: #2563eb;
  font-size: 12px;
  line-height: 24px;
  padding: 0 8px;
}

.entry-count-button:hover {
  background: #dbeafe;
}

.empty-value {
  color: var(--text-tertiary);
  font-size: 12px;
}

.action-cell {
  justify-content: flex-end;
  gap: 4px;
}

.entry-expand-row {
  min-width: 960px;
  border-bottom: 1px solid #eef2f7;
  background: var(--bg-secondary);
  padding: 8px 12px 8px 126px;
}

.entry-list {
  display: grid;
  gap: 6px;
}

.entry-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 6px;
  align-items: center;
  max-width: 760px;
}

.entry-main {
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr) auto;
  gap: 8px;
  align-items: center;
  min-width: 0;
  min-height: 36px;
  cursor: pointer;
  border: 1px solid var(--border-light);
  border-radius: 6px;
  background: var(--bg-primary);
  padding: 4px 7px;
  text-align: left;
}

.entry-main:hover {
  border-color: var(--border-light);
  background: var(--bg-secondary);
}

.entry-main:disabled {
  cursor: not-allowed;
  opacity: 0.68;
}

.entry-icon {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  background: #ecfdf5;
  color: #16a34a;
}

.entry-copy {
  min-width: 0;
}

.entry-copy strong {
  color: var(--text-primary);
  font-size: 12px;
}

.entry-copy small {
  color: var(--text-secondary);
  font-size: 11px;
}

.entry-tags {
  display: inline-flex;
  gap: 6px;
  align-items: center;
}

.entry-actions {
  display: inline-flex;
  gap: 4px;
  align-items: center;
  justify-content: flex-end;
}

@media (max-width: 760px) {
  .group-header {
    grid-template-columns: 24px 28px minmax(0, 1fr);
  }

  .group-stats {
    grid-column: 3 / 4;
    justify-content: flex-start;
  }

  .object-table-head,
  .object-table-row,
  .entry-expand-row {
    min-width: 860px;
  }

  .entry-expand-row {
    padding-left: 64px;
  }
}
</style>
