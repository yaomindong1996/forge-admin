<template>
  <div class="designer-shell">
    <header class="designer-topbar">
      <div class="topbar-left">
        <n-button quaternary circle @click="$emit('back')">
          <template #icon>
            <n-icon><ArrowBackOutline /></n-icon>
          </template>
        </n-button>
        <div class="object-mark">
          <n-icon><CubeOutline /></n-icon>
        </div>
        <div class="object-title">
          <div class="title-row">
            <h1>{{ designer?.objectName || designer?.objectCode || '业务对象设计' }}</h1>
            <n-tag size="small" :type="designStatusType" :bordered="false">
              {{ designStatusLabel }}
            </n-tag>
            <n-tag size="small" :type="publishStatusType" :bordered="false">
              {{ publishStatusLabel }}
            </n-tag>
            <n-tag v-if="dirty" size="small" type="warning" :bordered="false">
              未保存
            </n-tag>
          </div>
          <p>
            {{ designer?.suiteName || designer?.suiteCode || '未关联业务套件' }}
            <span v-if="designer?.updateTime">最近保存 {{ designer.updateTime }}</span>
            <span v-if="designer?.lastPublishTime">最后发布 {{ designer.lastPublishTime }}</span>
          </p>
        </div>
      </div>

      <div class="topbar-actions">
        <n-button secondary :disabled="previewDisabled" @click="$emit('preview')">
          <template #icon>
            <n-icon><EyeOutline /></n-icon>
          </template>
          预览
        </n-button>
        <n-button :loading="saving" type="primary" @click="$emit('save')">
          <template #icon>
            <n-icon><SaveOutline /></n-icon>
          </template>
          保存
        </n-button>
        <n-button :loading="publishing" :disabled="publishDisabled" type="success" secondary @click="$emit('publish')">
          <template #icon>
            <n-icon><RocketOutline /></n-icon>
          </template>
          {{ publishing ? '发布中' : '发布' }}
        </n-button>
        <n-dropdown trigger="click" :options="moreOptions" @select="$emit($event)">
          <n-button quaternary circle>
            <template #icon>
              <n-icon><EllipsisHorizontalOutline /></n-icon>
            </template>
          </n-button>
        </n-dropdown>
      </div>
    </header>

    <div class="designer-workbench">
      <aside class="designer-nav">
        <button
          v-for="item in filteredNavItems"
          :key="item.key"
          type="button"
          class="nav-item"
          :class="{ active: item.key === activePanel }"
          @click="handlePanelClick(item.key)"
        >
          <n-icon>
            <component :is="item.icon" />
          </n-icon>
          <span>{{ item.label }}</span>
          <em v-if="item.key === 'publish' && designer?.hasUnpublishedChanges">待发布</em>
        </button>

        <div v-if="closureSteps.length" class="closure-steps">
          <div class="closure-steps-head">
            <strong>单据闭环配置</strong>
            <span>{{ closureDoneCount }}/{{ closureSteps.length }}</span>
          </div>
          <button
            v-for="step in closureSteps"
            :key="step.key"
            type="button"
            class="closure-step"
            :class="[`step-${step.status || 'todo'}`]"
            @click="handleStepClick(step)"
          >
            <span>{{ step.label }}</span>
            <em>{{ step.statusLabel }}</em>
          </button>
        </div>
      </aside>

      <main class="designer-main">
        <div class="designer-main-content">
          <div v-if="loading" class="designer-loading-mask">
            <span class="loading-dot" />
          </div>
          <div class="panel-frame" :class="{ dirty }">
            <slot />
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import {
  ArrowBackOutline,
  BuildOutline,
  CheckmarkDoneOutline,
  CubeOutline,
  DocumentTextOutline,
  EllipsisHorizontalOutline,
  EyeOutline,
  GitBranchOutline,
  GitNetworkOutline,
  KeyOutline,
  ListOutline,
  OptionsOutline,
  ReaderOutline,
  RocketOutline,
  SaveOutline,
  SettingsOutline,
  TextOutline,
} from '@vicons/ionicons5'
import { computed } from 'vue'

const props = defineProps({
  designer: {
    type: Object,
    default: null,
  },
  activePanel: {
    type: String,
    default: 'basic',
  },
  loading: {
    type: Boolean,
    default: false,
  },
  dirty: {
    type: Boolean,
    default: false,
  },
  saving: {
    type: Boolean,
    default: false,
  },
  publishing: {
    type: Boolean,
    default: false,
  },
  publishDisabled: {
    type: Boolean,
    default: false,
  },
  previewDisabled: {
    type: Boolean,
    default: false,
  },
  showAdvanced: {
    type: Boolean,
    default: true,
  },
  closureSteps: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['update:activePanel', 'save', 'preview', 'publish', 'back', 'refresh', 'openRuntime', 'openTrigger'])

const navItems = [
  { key: 'basic', label: '基本信息', icon: OptionsOutline },
  { key: 'form', label: '表单设计', icon: ReaderOutline },
  { key: 'fields', label: '字段资产', icon: TextOutline },
  { key: 'list', label: '列表设计', icon: ListOutline },
  { key: 'relations', label: '关系与级联', icon: GitNetworkOutline },
  { key: 'document', label: '单据设置', icon: DocumentTextOutline },
  { key: 'automation', label: '流程与自动化', icon: GitBranchOutline },
  { key: 'actions', label: '自定义操作', icon: BuildOutline },
  { key: 'permission', label: '数据权限', icon: KeyOutline },
  { key: 'publish', label: '发布检查', icon: CheckmarkDoneOutline },
  { key: 'advanced', label: '高级配置', icon: SettingsOutline },
]

const moreOptions = [
  { label: '刷新设计器', key: 'refresh' },
  { label: '打开运行应用', key: 'openRuntime' },
  { label: '配置触发器', key: 'openTrigger' },
]

const filteredNavItems = computed(() => {
  if (props.showAdvanced)
    return navItems
  return navItems.filter(item => item.key !== 'advanced')
})
const closureDoneCount = computed(() => props.closureSteps.filter(step => step.status === 'done').length)

function handlePanelClick(key) {
  if (key === props.activePanel)
    return
  if (!props.dirty) {
    emit('update:activePanel', key)
    return
  }
  if (!window.$dialog) {
    emit('update:activePanel', key)
    return
  }
  window.$dialog.warning({
    title: '未保存变更',
    content: '当前面板存在尚未保存的设计变更，切换后草稿仍会保留，但发布前需要先保存。',
    positiveText: '继续切换',
    negativeText: '留在当前',
    onPositiveClick: () => emit('update:activePanel', key),
  })
}

function handleStepClick(step = {}) {
  if (step.key === 'trigger') {
    emit('openTrigger')
    return
  }
  if (step.key === 'runtime') {
    emit('openRuntime')
    return
  }
  handlePanelClick(step.panel || step.key)
}

const designStatusLabel = computed(() => {
  const status = props.designer?.designStatus || 'DRAFT'
  const labels = {
    DRAFT: '草稿',
    DESIGNING: '设计中',
    PUBLISHED: '已发布',
    ARCHIVED: '已归档',
  }
  return labels[status] || status
})

const designStatusType = computed(() => {
  const status = props.designer?.designStatus || 'DRAFT'
  if (status === 'PUBLISHED')
    return 'success'
  if (status === 'ARCHIVED')
    return 'default'
  return 'info'
})

const publishStatusLabel = computed(() => {
  const status = props.designer?.publishStatus || (props.designer?.lastPublishVersion ? 'PUBLISHED' : 'UNPUBLISHED')
  const labels = {
    UNPUBLISHED: '未发布',
    PUBLISHED: '已发布',
    NEED_PUBLISH: '有未发布变更',
    FAILED: '发布失败',
  }
  return labels[status] || status
})

const publishStatusType = computed(() => {
  if (props.designer?.hasUnpublishedChanges)
    return 'warning'
  const status = props.designer?.publishStatus
  if (status === 'PUBLISHED')
    return 'success'
  if (status === 'FAILED')
    return 'error'
  return 'warning'
})
</script>

<style scoped>
.designer-shell {
  position: fixed;
  z-index: 1000;
  inset: 0;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  width: 100vw;
  height: 100vh;
  background: #f6f8fb;
}

.designer-main-content {
  position: relative;
  min-height: 100%;
  height: 100%;
}

.designer-loading-mask {
  position: absolute;
  z-index: 10;
  display: grid;
  place-items: center;
  inset: 0;
  background: rgba(248, 250, 252, 0.58);
}

.loading-dot {
  width: 24px;
  height: 24px;
  border: 2px solid #cbd5e1;
  border-top-color: #2563eb;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.designer-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-height: 72px;
  border-bottom: 1px solid #e5e7eb;
  background: #fff;
  padding: 12px 20px;
}

.topbar-left,
.topbar-actions,
.title-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.object-mark {
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  border-radius: 8px;
  background: #eef6ff;
  color: #2563eb;
  font-size: 22px;
}

.object-title h1 {
  margin: 0;
  color: #111827;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0;
}

.object-title p {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin: 4px 0 0;
  color: #64748b;
  font-size: 12px;
}

.designer-workbench {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  min-height: 0;
  height: calc(100vh - 72px);
}

.designer-nav {
  min-height: 0;
  overflow-y: auto;
  border-right: 1px solid #e5e7eb;
  background: #fff;
  padding: 14px 10px;
}

.nav-item {
  display: grid;
  grid-template-columns: 22px minmax(0, 1fr) auto;
  align-items: center;
  width: 100%;
  min-height: 40px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: #475569;
  cursor: pointer;
  font-size: 13px;
  text-align: left;
  padding: 0 10px;
}

.nav-item + .nav-item {
  margin-top: 4px;
}

.nav-item:hover {
  background: #f1f5f9;
  color: #111827;
}

.nav-item.active {
  background: #eaf2ff;
  color: #1d4ed8;
  font-weight: 700;
}

.nav-item span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.nav-item em {
  border-radius: 4px;
  background: #fff7ed;
  color: #c2410c;
  font-size: 11px;
  font-style: normal;
  line-height: 20px;
  padding: 0 6px;
}

.closure-steps {
  display: grid;
  gap: 6px;
  margin-top: 16px;
  border-top: 1px solid #e5e7eb;
  padding-top: 14px;
}

.closure-steps-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #334155;
  font-size: 12px;
  padding: 0 8px;
}

.closure-steps-head strong {
  font-size: 12px;
}

.closure-steps-head span {
  color: #64748b;
}

.closure-step {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  min-height: 32px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  color: #475569;
  cursor: pointer;
  font-size: 12px;
  text-align: left;
  padding: 0 8px;
}

.closure-step:hover {
  border-color: #bfdbfe;
  background: #f8fbff;
}

.closure-step span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.closure-step em {
  border-radius: 4px;
  font-size: 11px;
  font-style: normal;
  line-height: 20px;
  padding: 0 6px;
}

.closure-step.step-done em {
  background: #dcfce7;
  color: #15803d;
}

.closure-step.step-warn em {
  background: #fff7ed;
  color: #c2410c;
}

.closure-step.step-todo em {
  background: #f1f5f9;
  color: #64748b;
}

.designer-main {
  min-width: 0;
  min-height: 0;
  overflow: auto;
  padding: 16px;
}

.designer-main :deep(.n-spin-container),
.designer-main :deep(.n-spin-content) {
  width: 100%;
  min-width: 0;
}

.panel-frame {
  box-sizing: border-box;
  width: 100%;
  min-height: calc(100vh - 104px);
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  overflow: hidden;
}

.panel-frame.dirty {
  border-color: #f59e0b;
}

@media (max-width: 900px) {
  .designer-topbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .designer-workbench {
    grid-template-columns: 1fr;
    height: calc(100vh - 116px);
  }

  .designer-nav {
    display: flex;
    overflow-x: auto;
    overflow-y: hidden;
    border-right: 0;
    border-bottom: 1px solid #e5e7eb;
  }

  .nav-item {
    min-width: 124px;
  }
}
</style>
