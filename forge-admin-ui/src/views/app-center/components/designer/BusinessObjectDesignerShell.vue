<template>
  <div class="designer-shell" :class="{ embedded, 'compact-embedded': compactEmbedded, 'nav-hidden': !showDesignerNavigation, 'has-back-bar': showEmbeddedBackBar }">
    <header v-if="!embedded" class="designer-topbar">
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
          <span class="object-workbench-kind">业务对象设计</span>
          <div class="title-row">
            <h1>{{ designer?.objectName || designer?.objectCode || '未命名业务对象' }}</h1>
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
            <span class="object-workbench-description">维护对象字段、数据关系和树形模型</span>
            {{ designer?.suiteName || designer?.suiteCode || '未关联业务域' }}
            <span v-if="designer?.updateTime">最近保存 {{ designer.updateTime }}</span>
            <span v-if="designer?.lastPublishTime">最后发布 {{ designer.lastPublishTime }}</span>
          </p>
        </div>
      </div>

      <div class="topbar-actions" @click.capture="handleTopbarActionsClick">
        <div class="save-state" :class="{ dirty }">
          <n-icon>
            <AlertCircleOutline v-if="dirty" />
            <CheckmarkCircleOutline v-else />
          </n-icon>
          <span>{{ dirty ? '未保存' : '已保存' }}</span>
        </div>
        <n-dropdown trigger="click" :options="moreOptions" @select="$emit($event)">
          <n-button class="topbar-icon-button" quaternary circle title="设置">
            <template #icon>
              <n-icon><SettingsOutline /></n-icon>
            </template>
          </n-button>
        </n-dropdown>
        <n-button
          v-if="showPreview"
          class="topbar-icon-button"
          quaternary
          circle
          :disabled="loading || previewDisabled"
          title="预览"
          @click="$emit('preview')"
        >
          <template #icon>
            <n-icon><CaretForward /></n-icon>
          </template>
        </n-button>
        <n-button class="topbar-save-button" :loading="saving" :disabled="loading" secondary @click="$emit('save')">
          <template #icon>
            <n-icon><SaveOutline /></n-icon>
          </template>
          保存
        </n-button>
        <button
          v-if="showPublish"
          type="button"
          class="topbar-publish-button"
          :disabled="loading || publishDisabled || publishing"
          @click="$emit('publish')"
        >
          {{ publishing ? '发布中' : '发布' }}
        </button>
      </div>
    </header>

    <!-- 嵌入模式返回栏：当活动面板不在导航白名单时显示 -->
    <div v-if="showEmbeddedBackBar" class="embedded-back-bar">
      <button
        type="button"
        class="embedded-back-button"
        @click="$emit('update:activePanel', embeddedBackTarget)"
      >
        <svg width="1em" height="1em" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M15.41 7.41 14 6l-6 6 6 6 1.41-1.41L10.83 12z" fill="currentColor" />
        </svg>
        返回{{ filteredNavItems.find(i => i.key === embeddedBackTarget)?.label || '' }}
      </button>
    </div>

    <div class="designer-workbench" :class="{ 'nav-collapsed': navCollapsed }">
      <aside v-if="showDesignerNavigation" class="designer-nav" :class="{ collapsed: navCollapsed }">
        <button
          type="button"
          class="nav-collapse-button"
          :title="navCollapsed ? '展开导航' : '收起导航'"
          @click="navCollapsed = !navCollapsed"
        >
          <n-icon>
            <ChevronForwardOutline v-if="navCollapsed" />
            <ChevronBackOutline v-else />
          </n-icon>
          <span>导航</span>
        </button>
        <button
          v-for="item in filteredNavItems"
          :key="item.key"
          type="button"
          class="nav-item"
          :class="{ active: item.key === activeNavigationKey, disabled: loading }"
          :disabled="loading"
          :title="navCollapsed ? item.label : ''"
          @click="handlePanelClick(item.key)"
        >
          <n-icon>
            <component :is="item.icon" />
          </n-icon>
          <span>{{ item.label }}</span>
          <em v-if="item.key === 'publish' && designer?.hasUnpublishedChanges">待发布</em>
        </button>
      </aside>

      <main class="designer-main">
        <div class="designer-main-content">
          <div v-if="loading" class="designer-loading-mask">
            <DesignerAsyncLoader
              title="正在加载设计器配置"
              description="正在准备业务对象、字段和页面 Schema"
              overlay
            />
          </div>
          <div v-else class="panel-frame" :class="{ dirty }">
            <slot />
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import {
  AlertCircleOutline,
  ArrowBackOutline,
  CaretForward,
  CheckmarkCircleOutline,
  CheckmarkDoneOutline,
  ChevronBackOutline,
  ChevronForwardOutline,
  CubeOutline,
  FlashOutline,
  GitBranchOutline,
  GitNetworkOutline,
  ListOutline,
  OptionsOutline,
  ReaderOutline,
  SaveOutline,
  SettingsOutline,
  TextOutline,
} from '@vicons/ionicons5'
import { computed, ref } from 'vue'
import DesignerAsyncLoader from './DesignerAsyncLoader.vue'
import { resolveStandaloneObjectDesignerSection, standaloneObjectDesignerSections } from './object-designer-navigation'

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
  confirmDirtySwitch: {
    type: Boolean,
    default: true,
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
  navPanels: {
    type: Array,
    default: () => [],
  },
  showPreview: {
    type: Boolean,
    default: true,
  },
  showPublish: {
    type: Boolean,
    default: true,
  },
  embedded: {
    type: Boolean,
    default: false,
  },
  compactEmbedded: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits([
  'update:activePanel',
  'save',
  'preview',
  'publish',
  'back',
  'refresh',
  'openRuntime',
  'openFields',
  'openFunctionMarket',
])
const navCollapsed = ref(false)

const legacyNavItems = [
  { key: 'fields', label: '数据结构', icon: TextOutline },
  { key: 'form', label: '表单设计', icon: ReaderOutline },
  { key: 'list', label: '列表设计', icon: ListOutline },
  { key: 'relations', label: '关系与级联', icon: GitNetworkOutline },
  { key: 'actions', label: '业务动作', icon: FlashOutline },
  { key: 'flow-app', label: '业务流程配置', icon: GitBranchOutline },
  { key: 'tree-model', label: '树形模型', icon: GitNetworkOutline },
  { key: 'publish', label: '发布检查', icon: CheckmarkDoneOutline },
  { key: 'basic', label: '基本信息', icon: OptionsOutline },
  { key: 'advanced', label: '高级配置', icon: SettingsOutline },
]

const standaloneIconMap = {
  'basic': OptionsOutline,
  'fields': TextOutline,
  'data-model': GitNetworkOutline,
}

const standaloneNavItems = standaloneObjectDesignerSections.map(item => ({
  ...item,
  icon: standaloneIconMap[item.key],
}))

const filteredNavItems = computed(() => {
  const whitelist = props.navPanels || []
  if (!whitelist.length)
    return standaloneNavItems
  return legacyNavItems.filter((item) => {
    if (whitelist.length && !whitelist.includes(item.key))
      return false
    if (item.key === 'advanced')
      return props.showAdvanced
    return true
  })
})
const activeNavigationKey = computed(() => props.navPanels?.length
  ? props.activePanel
  : resolveStandaloneObjectDesignerSection(props.activePanel))
const showDesignerNavigation = computed(() => !props.embedded || filteredNavItems.value.length > 1)
/** 嵌入模式下，当前活动面板不在导航白名单内（例如从“查看数据结构”切入 fields），需要显示返回栏 */
const showEmbeddedBackBar = computed(() => {
  if (!props.embedded || !props.navPanels?.length)
    return false
  return !props.navPanels.includes(props.activePanel)
})
const embeddedBackTarget = computed(() => props.navPanels?.[0] || 'list')
const moreOptions = computed(() => {
  if (props.navPanels?.length) {
    return [
      { label: '刷新设计器', key: 'refresh' },
    ]
  }
  const options = [
    { label: '函数市场', key: 'openFunctionMarket' },
    { label: '刷新设计器', key: 'refresh' },
    { label: '打开运行应用', key: 'openRuntime' },
  ]
  if (props.showAdvanced) {
    options.splice(1, 0, {
      label: '数据结构',
      key: 'openFields',
    })
  }
  return options
})
function handlePanelClick(key) {
  if (props.loading)
    return
  if (key === props.activePanel)
    return
  if (!props.dirty || !props.confirmDirtySwitch) {
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
function handleTopbarActionsClick(event) {
  if (props.activePanel !== 'form')
    return
  const button = event.target?.closest?.('button')
  if (!button)
    return
  const actionText = `${button.textContent || ''} ${button.title || ''} ${button.getAttribute('aria-label') || ''}`
  if (!actionText.includes('预览'))
    return

  event.preventDefault()
  event.stopPropagation()
  event.stopImmediatePropagation?.()
  window.dispatchEvent(new CustomEvent('forge-form-designer:preview-current-form'))
}
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
  background: #f8f9fa;
}

.designer-shell.embedded {
  position: relative;
  z-index: auto;
  grid-template-rows: minmax(0, 1fr);
  width: 100%;
  height: 100%;
}

.designer-shell.embedded .designer-workbench {
  height: 100%;
}

.designer-shell.embedded.has-back-bar {
  grid-template-rows: auto minmax(0, 1fr);
}

.designer-shell.embedded.nav-hidden .designer-workbench {
  grid-template-columns: minmax(0, 1fr);
}

.designer-shell.embedded.compact-embedded,
.designer-shell.embedded.compact-embedded .designer-workbench,
.designer-shell.embedded.compact-embedded .designer-main-content,
.designer-shell.embedded.compact-embedded .panel-frame {
  height: auto;
  min-height: 0;
}

.designer-shell.embedded.compact-embedded .designer-workbench {
  grid-template-columns: minmax(0, 1fr);
}

.designer-shell.embedded.compact-embedded .designer-nav {
  display: none;
}

.designer-shell.embedded.compact-embedded .panel-frame {
  overflow: visible;
}

.embedded-back-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 12px;
  border-bottom: 1px solid #e5e6eb;
  background: #f7f8fa;
}

.embedded-back-button {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  cursor: pointer;
  border: 1px solid #d9dce1;
  border-radius: 6px;
  background: #fff;
  color: #1d2129;
  font-size: 13px;
  line-height: 20px;
  transition: border-color 0.15s, color 0.15s;
}

.embedded-back-button:hover {
  border-color: #818cf8;
  color: #3153d8;
}

.designer-main-content {
  position: relative;
  min-height: 100%;
  height: 100%;
}

.designer-loading-mask {
  position: absolute;
  z-index: 120;
  display: grid;
  place-items: center;
  inset: 0;
  border-radius: 12px;
  background: rgba(248, 250, 252, 0.82);
  backdrop-filter: blur(2px);
}

.designer-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 48px;
  border-bottom: 1px solid #e4e4e7;
  background: rgba(255, 255, 255, 0.94);
  padding: 6px 12px;
  backdrop-filter: blur(12px);
}

.topbar-left,
.topbar-actions,
.title-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.object-workbench-kind {
  display: block;
  margin-bottom: 1px;
  color: #71717a;
  font-size: 11px;
  font-weight: 650;
  letter-spacing: 0.04em;
  line-height: 14px;
}
.object-workbench-description {
  margin-right: 9px;
  color: #52525b;
  font-weight: 600;
}

.topbar-actions {
  gap: 6px;
}

.save-state {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-right: 2px;
  color: #71717a;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.save-state .n-icon {
  color: #22c55e;
  font-size: 15px;
}

.save-state.dirty .n-icon {
  color: #f59e0b;
}

.topbar-icon-button {
  width: 30px;
  height: 30px;
  --n-color-hover: #f4f4f5 !important;
  --n-color-pressed: #e4e4e7 !important;
  --n-text-color: #71717a !important;
  --n-text-color-hover: #18181b !important;
}

.topbar-save-button {
  height: 30px;
  padding: 0 10px;
  background: #fff !important;
  color: #27272a !important;
  border-color: #e4e4e7 !important;
  --n-border-radius: 6px !important;
  --n-color: #fff !important;
  --n-color-hover: #f8fafc !important;
  --n-color-pressed: #f4f4f5 !important;
  --n-border: 1px solid #e4e4e7 !important;
  --n-border-hover: 1px solid #d4d4d8 !important;
  --n-border-pressed: 1px solid #d4d4d8 !important;
  --n-text-color: #27272a !important;
  --n-text-color-hover: #18181b !important;
}

.topbar-save-button :deep(.n-button__content),
.topbar-save-button :deep(.n-button__icon) {
  color: #27272a !important;
}

.topbar-publish-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 30px;
  min-width: 52px;
  padding: 0 12px;
  cursor: pointer;
  border: 1px solid #2944cc;
  border-radius: 6px;
  background: #2944cc;
  color: #fff;
  font-size: 12px;
  font-weight: 650;
  line-height: 1;
  transition:
    background 0.16s ease,
    border-color 0.16s ease,
    opacity 0.16s ease;
}

.topbar-publish-button:hover:not(:disabled) {
  border-color: #0e32f3;
  background: #0e32f3;
}

.topbar-publish-button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.object-mark {
  display: grid;
  width: 30px;
  height: 30px;
  place-items: center;
  border: 1px solid #e4e4e7;
  border-radius: 7px;
  background: #f4f4f5;
  color: #52525b;
  font-size: 16px;
}

.object-title h1 {
  margin: 0;
  color: #18181b;
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0;
  line-height: 18px;
}

.object-title p {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 1px 0 0;
  color: #71717a;
  font-size: 11px;
  line-height: 14px;
}

.designer-workbench {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  min-height: 0;
  height: calc(100vh - 48px);
}

.designer-workbench.nav-collapsed {
  grid-template-columns: 56px minmax(0, 1fr);
}

.designer-nav {
  min-height: 0;
  overflow-y: auto;
  border-right: 1px solid #e4e4e7;
  background: #fcfcfc;
  padding: 10px 8px;
}

.designer-nav.collapsed {
  padding: 8px;
}

.nav-collapse-button {
  display: grid;
  grid-template-columns: 22px minmax(0, 1fr);
  align-items: center;
  width: 100%;
  min-height: 32px;
  margin-bottom: 8px;
  padding: 0 9px;
  cursor: pointer;
  border: 1px solid #e4e4e7;
  border-radius: 8px;
  background: #fff;
  color: #52525b;
  font-size: 12px;
  font-weight: 650;
  text-align: left;
}

.nav-collapse-button:hover {
  border-color: #d4d4d8;
  background: #f4f4f5;
  color: #18181b;
}

.designer-nav.collapsed .nav-collapse-button {
  grid-template-columns: 1fr;
  place-items: center;
  padding: 0;
}

.designer-nav.collapsed .nav-collapse-button span {
  display: none;
}

.nav-item {
  display: grid;
  grid-template-columns: 22px minmax(0, 1fr) auto;
  align-items: center;
  width: 100%;
  min-height: 34px;
  border: 0;
  border-radius: 7px;
  background: transparent;
  color: #52525b;
  cursor: pointer;
  font-size: 12px;
  text-align: left;
  padding: 0 10px;
}

.designer-nav.collapsed .nav-item {
  grid-template-columns: 1fr;
  place-items: center;
  padding: 0;
}

.designer-nav.collapsed .nav-item span,
.designer-nav.collapsed .nav-item em {
  display: none;
}

.nav-item + .nav-item {
  margin-top: 3px;
}

.nav-item:hover {
  background: #f4f4f5;
  color: #18181b;
}

.nav-item.disabled,
.nav-item:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.nav-item.disabled:hover,
.nav-item:disabled:hover {
  background: transparent;
  color: #475569;
}

.nav-item.active {
  background: #f4f4f5;
  color: #18181b;
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

.designer-main {
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  padding: 0;
  background: #f8f9fa;
}

.designer-main :deep(.n-spin-container),
.designer-main :deep(.n-spin-content) {
  width: 100%;
  min-width: 0;
}

.panel-frame {
  box-sizing: border-box;
  width: 100%;
  height: 100%;
  min-height: 0;
  border: 0;
  border-radius: 0;
  background: #f8f9fa;
  overflow-x: hidden;
  overflow-y: auto;
  overscroll-behavior: contain;
}

.panel-frame.dirty {
  box-shadow: inset 0 2px 0 #f59e0b;
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

  .designer-workbench.nav-collapsed {
    grid-template-columns: 1fr;
  }

  .designer-nav {
    display: flex;
    overflow-x: auto;
    overflow-y: hidden;
    border-right: 0;
    border-bottom: 1px solid #e5e7eb;
  }

  .designer-nav.collapsed {
    padding: 10px;
  }

  .designer-nav.collapsed .nav-collapse-button span,
  .designer-nav.collapsed .nav-item span,
  .designer-nav.collapsed .nav-item em {
    display: inline;
  }

  .designer-nav.collapsed .nav-collapse-button,
  .designer-nav.collapsed .nav-item {
    grid-template-columns: 22px minmax(0, 1fr) auto;
    place-items: initial;
    padding: 0 10px;
  }

  .nav-item {
    min-width: 124px;
  }
}
.panel-frame {
  min-height: 0;
  padding: 0;
}

.panel-frame > * {
  min-height: 0;
}
</style>
