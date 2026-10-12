<template>
  <article class="unit-card">
    <header class="unit-head">
      <button
        class="unit-title-button"
        type="button"
        :disabled="!unit.object?.objectCode"
        @click="emit('openObject', unit.object)"
      >
        <span class="unit-icon">
          <IconRenderer v-if="unit.object?.icon" :icon="unit.object.icon" :size="22" />
          <n-icon v-else :component="CubeOutline" />
        </span>
        <span class="unit-title-copy">
          <span class="unit-title-line">
            <strong>{{ unit.object?.objectName || unit.object?.objectCode || '独立访问入口' }}</strong>
            <DictTag
              v-if="unit.object?.status !== undefined && !unit.synthetic"
              dict-type="sys_enable_disable"
              :value="unit.object.status"
              :bordered="false"
            />
          </span>
          <small v-if="showSuite && unit.object?.suiteName">{{ unit.object.suiteName }}</small>
        </span>
      </button>
      <n-dropdown
        v-if="canManageObject"
        trigger="click"
        :options="objectOptions"
        @select="handleObjectSelect"
      >
        <n-button quaternary circle size="small" aria-label="更多业务单元操作">
          <template #icon>
            <n-icon><EllipsisVertical /></n-icon>
          </template>
        </n-button>
      </n-dropdown>
    </header>

    <p class="unit-description">
      {{ unit.object?.description || (unit.standalone ? '未绑定具体业务单元，可作为外部页面、移动端或接口能力入口。' : '集中管理表单、列表、流程和入口。') }}
    </p>

    <div class="unit-meta">
      <DictTag
        v-if="unit.object?.objectType && !unit.synthetic"
        dict-type="ai_business_object_type"
        :value="unit.object.objectType"
        :bordered="false"
      />
      <span v-if="!unit.synthetic">{{ unit.object?.relationCount || 0 }} 个关系</span>
      <span v-if="!unit.synthetic">{{ unit.object?.bindingCount || 0 }} 项能力</span>
      <span>{{ unit.apps.length }} 个入口</span>
    </div>

    <section class="entry-panel">
      <div class="entry-panel-head">
        <strong>访问入口</strong>
        <span>{{ unit.apps.length ? '可从这里打开或配置' : '尚未配置入口' }}</span>
      </div>

      <div v-if="visibleApps.length" class="entry-list">
        <div v-for="app in visibleApps" :key="app.id" class="entry-row">
          <button
            class="entry-main"
            type="button"
            :disabled="isOpenDisabled(app)"
            @click="emit('openApp', app)"
          >
            <span class="entry-icon">
              <IconRenderer v-if="app.icon" :icon="app.icon" :size="18" />
              <n-icon v-else><OpenOutline /></n-icon>
            </span>
            <span class="entry-copy">
              <strong>{{ app.appName || app.appCode }}</strong>
              <small>
                <span v-if="showSuite && app.suiteName">{{ app.suiteName }}</span>
                <span v-if="showSuite && app.suiteName"> · </span>
                <span>{{ entryModeLabel(app) }}</span>
              </small>
            </span>
            <span class="entry-tags">
              <n-tag v-if="isCodeDownload(app)" size="small" type="info" :bordered="false">
                下载代码
              </n-tag>
              <DictTag dict-type="sys_enable_disable" :value="app.status" :bordered="false" />
            </span>
          </button>
          <n-dropdown trigger="click" :options="appOptions(app)" @select="key => handleAppSelect(key, app)">
            <n-button quaternary circle size="small" aria-label="更多入口操作">
              <template #icon>
                <n-icon><EllipsisVertical /></n-icon>
              </template>
            </n-button>
          </n-dropdown>
        </div>
        <div v-if="hiddenAppCount > 0" class="entry-more">
          还有 {{ hiddenAppCount }} 个入口，可通过搜索或进入业务域查看
        </div>
      </div>
      <n-empty v-else size="small" description="还没有访问入口" />
    </section>

    <footer class="unit-actions">
      <n-button secondary size="small" :disabled="!canManageObject" @click="emit('designObject', unit.object, primaryDesignPanel)">
        <template #icon>
          <n-icon><BuildOutline /></n-icon>
        </template>
        {{ primaryDesignText }}
      </n-button>
      <n-button secondary size="small" :disabled="!canManageObject" @click="emit('statsObject', unit.object)">
        <template #icon>
          <n-icon><StatsChartOutline /></n-icon>
        </template>
        看板
      </n-button>
      <n-button type="primary" secondary size="small" @click="emit('createApp', unit.object)">
        <template #icon>
          <n-icon><AddOutline /></n-icon>
        </template>
        添加入口
      </n-button>
    </footer>
  </article>
</template>

<script setup>
import {
  AddOutline,
  BuildOutline,
  CubeOutline,
  EllipsisVertical,
  OpenOutline,
  StatsChartOutline,
} from '@vicons/ionicons5'
import { computed } from 'vue'
import DictTag from '@/components/DictTag.vue'
import IconRenderer from '@/components/IconRenderer.vue'

const props = defineProps({
  unit: {
    type: Object,
    required: true,
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

const canManageObject = computed(() => Boolean(props.unit.object?.id))
const visibleApps = computed(() => props.unit.apps.slice(0, 4))
const hiddenAppCount = computed(() => Math.max(props.unit.apps.length - visibleApps.value.length, 0))
const codeAppObject = computed(() => isCodeAppMeta(props.unit.object))
const primaryDesignPanel = computed(() => codeAppObject.value ? 'flow-app' : 'form')
const primaryDesignText = computed(() => codeAppObject.value ? '业务流程配置' : '数据设计')
const objectOptions = computed(() => [
  {
    label: '打开详情',
    key: 'open',
  },
  {
    label: '编辑业务单元',
    key: 'edit',
  },
  {
    label: '业务流程配置',
    key: 'flow-app',
  },
  {
    label: props.unit.object?.status === 1 ? '停用业务单元' : '启用业务单元',
    key: 'toggle',
  },
  {
    type: 'divider',
    key: 'divider',
  },
  {
    label: '删除业务单元',
    key: 'delete',
  },
])

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

function appOptions(app) {
  const options = [
    {
      label: '配置入口',
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

function handleObjectSelect(key) {
  if (key === 'open') {
    emit('openObject', props.unit.object)
    return
  }
  if (key === 'edit') {
    emit('editObject', props.unit.object)
    return
  }
  if (key === 'flow-app') {
    emit('designObject', props.unit.object, 'flow-app')
    return
  }
  if (key === 'toggle') {
    emit('toggleObject', props.unit.object)
    return
  }
  if (key === 'delete')
    emit('deleteObject', props.unit.object)
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
</script>

<style scoped>
.unit-card {
  display: grid;
  gap: 12px;
  min-width: 0;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: var(--bg-primary);
  padding: 14px;
  transition:
    border-color 180ms ease,
    box-shadow 180ms ease;
}

.unit-card:hover {
  border-color: #2f6feb;
  box-shadow: 0 12px 26px rgb(15 23 42 / 8%);
}

.unit-head {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 32px;
  gap: 8px;
  align-items: start;
}

.unit-title-button {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
  min-width: 0;
  cursor: pointer;
  border: 0;
  background: transparent;
  padding: 0;
  text-align: left;
}

.unit-title-button:disabled {
  cursor: default;
}

.unit-icon,
.entry-icon {
  display: grid;
  place-items: center;
  border-radius: 8px;
}

.unit-icon {
  width: 42px;
  height: 42px;
  background: #eef6ff;
  color: #2563eb;
  font-size: 22px;
}

.unit-title-copy,
.unit-title-line,
.entry-copy {
  min-width: 0;
}

.unit-title-line {
  display: flex;
  gap: 8px;
  align-items: center;
}

.unit-title-line strong,
.entry-copy strong {
  overflow: hidden;
  color: var(--text-primary);
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.unit-title-line strong {
  font-size: 16px;
  line-height: 1.35;
}

.unit-title-copy small {
  display: block;
  margin-top: 3px;
  overflow: hidden;
  color: var(--text-secondary);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.unit-description {
  display: -webkit-box;
  min-height: 39px;
  margin: 0;
  overflow: hidden;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.5;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.unit-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.unit-meta span {
  display: inline-flex;
  align-items: center;
  border-radius: 4px;
  background: var(--bg-tertiary);
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 22px;
  padding: 0 8px;
}

.entry-panel {
  display: grid;
  gap: 10px;
  border: 1px solid #eef2f7;
  border-radius: 8px;
  background: var(--bg-secondary);
  padding: 10px;
}

.entry-panel-head {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
}

.entry-panel-head strong {
  color: var(--text-primary);
  font-size: 13px;
}

.entry-panel-head span {
  color: var(--text-secondary);
  font-size: 12px;
}

.entry-list {
  display: grid;
  gap: 8px;
}

.entry-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 32px;
  gap: 6px;
  align-items: center;
}

.entry-main {
  display: grid;
  grid-template-columns: 32px minmax(0, 1fr) auto;
  gap: 8px;
  align-items: center;
  min-width: 0;
  min-height: 44px;
  cursor: pointer;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: var(--bg-primary);
  padding: 6px 8px;
  text-align: left;
  transition:
    border-color 160ms ease,
    background 160ms ease;
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
  width: 32px;
  height: 32px;
  background: #ecfdf5;
  color: #16a34a;
}

.entry-copy strong,
.entry-copy small {
  display: block;
}

.entry-tags {
  display: inline-flex;
  gap: 6px;
  align-items: center;
}

.entry-copy strong {
  max-width: 100%;
  font-size: 13px;
}

.entry-copy small {
  margin-top: 2px;
  overflow: hidden;
  color: var(--text-secondary);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.entry-more {
  border-radius: 6px;
  background: var(--bg-tertiary);
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 28px;
  padding: 0 10px;
}

.unit-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  justify-content: flex-end;
  border-top: 1px solid #eef2f7;
  padding-top: 10px;
}

@media (max-width: 560px) {
  .unit-actions {
    justify-content: stretch;
  }

  .unit-actions :deep(.n-button) {
    flex: 1 1 120px;
  }
}
</style>
