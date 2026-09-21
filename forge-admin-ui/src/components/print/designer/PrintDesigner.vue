<script setup>
import { CloseOutline } from '@vicons/ionicons5'
import { NAlert, NButton, NIcon, NInput, NModal, NTabPane, NTabs, useThemeVars } from 'naive-ui'
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { usePrintDesignerStore } from '@/stores/print/printDesignerStore'
import { createPrintDocument } from '../protocol/types'
import PrintPreview from '../runtime/PrintPreview.vue'
import { newPrintId } from './commands'
import { createDesignerSampleContext, hasDesignerData } from './designerSample'
import { parseDraft, readDraft, writeDraft } from './draftStorage'
import { cloneDocument } from './history'
import PaperPanel from './panels/PaperPanel.vue'
import PropertyTabsPanel from './panels/PropertyTabsPanel.vue'
import PrintCalibration from './PrintCalibration.vue'
import PrintCanvas from './PrintCanvas.vue'
import PrintDesignerToolbar from './PrintDesignerToolbar.vue'
import PrintElementPalette from './PrintElementPalette.vue'
import PrintFieldTree from './PrintFieldTree.vue'
import PrintSectionList from './PrintSectionList.vue'
import { renewStaticTableIds } from './staticTable'
import { usePrintDesignerLifecycle } from './usePrintDesignerLifecycle'

const props = defineProps({
  template: { type: Object, default: createPrintDocument },
  catalog: { type: Array, default: () => [] },
  context: { type: Object, default: () => ({}) },
  resolveFile: Function,
  saveDraft: Function,
  externalDirty: Boolean,
  externalError: { type: String, default: '' },
  externalNotice: { type: String, default: '' },
  confirmDiscard: Function,
  pageMode: Boolean,
  templateName: { type: String, default: '' },
  designStatus: { type: [String, Number], default: '' },
  draftRevision: { type: [String, Number], default: '' },
  canManage: { type: Boolean, default: true },
  canPublish: Boolean,
  publishing: Boolean,
  nameDirty: Boolean,
})
const emit = defineEmits(['update:templateName', 'back', 'bindings', 'versions', 'publish', 'clearExternalError', 'clearExternalNotice'])
const store = usePrintDesignerStore()
store.load(props.template, props.catalog)
const bannerError = computed(() => store.error || props.externalError || '')
const bannerNotice = computed(() => (!store.error && !props.externalError ? (store.notice || props.externalNotice || '') : ''))
function clearBanner() {
  if (store.error) {
    store.error = ''
    return
  }
  if (props.externalError) {
    emit('clearExternalError')
    return
  }
  if (store.notice) {
    store.notice = ''
    return
  }
  if (props.externalNotice)
    emit('clearExternalNotice')
}
const hasRealPreviewData = computed(() => hasDesignerData(props.context))
const designerContext = computed(() => hasRealPreviewData.value ? props.context : createDesignerSampleContext(store.catalog))
const previewDataLabel = computed(() => hasRealPreviewData.value ? '当前样本数据' : '字段示例数据')
let compactMedia
function collapseForCompact(event) {
  if (event.matches) {
    store.leftPanelOpen = false
    store.rightPanelOpen = false
  }
}
onMounted(() => {
  compactMedia = window.matchMedia?.('(max-width: 900px)')
  if (compactMedia) {
    collapseForCompact(compactMedia)
    compactMedia.addEventListener('change', collapseForCompact)
  }
})
const confirmOpen = ref(false)
let resolveConfirm = null
function confirmDiscard() {
  if (props.confirmDiscard)
    return props.confirmDiscard('有未保存的打印模板修改，确定放弃吗？')
  if (resolveConfirm)
    return false
  confirmOpen.value = true
  return new Promise((resolve) => {
    resolveConfirm = resolve
  })
}
function answerDiscard(answer) {
  confirmOpen.value = false
  resolveConfirm?.(answer)
  resolveConfirm = null
}
onBeforeUnmount(() => {
  answerDiscard(false)
  compactMedia?.removeEventListener('change', collapseForCompact)
})
const { canLeave } = usePrintDesignerLifecycle(store, confirmDiscard, () => props.externalDirty)
const theme = useThemeVars()
const themeStyle = computed(() => ({
  '--bg-primary': theme.value.cardColor,
  '--gray-100': theme.value.bodyColor,
  '--text-primary': theme.value.textColor1,
  '--text-secondary': theme.value.textColor2,
  '--text-tertiary': theme.value.textColor3,
  '--border-light': theme.value.borderColor,
  '--primary-color': theme.value.primaryColor,
  '--error-color': theme.value.errorColor || '#d03050',
}))
const protocolOpen = ref(false)
const calibrationOpen = ref(false)
const protocolText = ref('')
const protocolError = ref('')
const panel = ref('selection')
const leftTab = ref('palette')
watch(() => props.catalog, (value) => {
  store.catalog = cloneDocument(value)
}, { deep: true })
watch(() => props.template, async (value) => {
  if (await canLeave() && props.template === value)
    store.load(value, props.catalog)
})
async function createNew() {
  if (await canLeave())
    store.load(createPrintDocument(), props.catalog)
}
function copyTemplate() {
  store.execute((doc) => {
    for (const band of [doc.header, doc.footer, ...doc.body]) {
      if (band.id)
        band.id = newPrintId()
      for (const element of band.elements || []) {
        element.id = newPrintId()
        renewStaticTableIds(element)
      }
      for (const column of band.columns || []) column.id = newPrintId()
    }
  })
}
function save() {
  return store.save(props.saveDraft || writeDraft)
}
async function restore() {
  if (!await canLeave())
    return
  try {
    const saved = readDraft()
    if (saved)
      store.load(saved, props.catalog)
    else store.notice = '当前浏览器没有本地草稿'
  }
  catch (error) {
    store.error = `恢复失败：${error.message}`
  }
}
function openProtocol() {
  protocolText.value = JSON.stringify(store.document, null, 2)
  protocolError.value = ''
  protocolOpen.value = true
}
async function importProtocol() {
  try {
    const parsed = parseDraft(protocolText.value)
    if (!await canLeave())
      return
    store.execute((doc) => {
      for (const key of Object.keys(doc)) delete doc[key]
      Object.assign(doc, parsed)
    })
    protocolOpen.value = false
  }
  catch (error) {
    protocolError.value = `导入失败：${error.message}`
  }
}
function download() {
  const url = URL.createObjectURL(new Blob([JSON.stringify(store.document, null, 2)], { type: 'application/json' }))
  const link = document.createElement('a')
  link.href = url
  link.download = 'forge-print-template.json'
  link.click()
  setTimeout(() => URL.revokeObjectURL(url), 0)
}
function locateIssue(issue) {
  const sectionMatch = issue.path.match(/^body\[(\d+)\]/)
  const elementMatch = issue.path.match(/elements\[(\d+)\]/)
  const id = sectionMatch ? `section:${store.document.body[Number(sectionMatch[1])]?.id}` : issue.path.startsWith('footer') ? 'footer' : 'header'
  store.selectSurface(id)
  if (elementMatch)
    store.selectedIds = [store.activeSurface.elements[Number(elementMatch[1])].id]
  panel.value = 'selection'
}
defineExpose({ canLeave, save })
</script>

<template>
  <div class="print-designer" :style="themeStyle">
    <PrintDesignerToolbar
      :local="!saveDraft"
      :external-dirty="externalDirty"
      :page-mode="pageMode"
      :template-name="templateName"
      :design-status="designStatus"
      :draft-revision="draftRevision"
      :can-manage="canManage"
      :can-publish="canPublish"
      :publishing="publishing"
      :name-dirty="nameDirty"
      @new="createNew"
      @copy="copyTemplate"
      @save="save"
      @restore="restore"
      @protocol="openProtocol"
      @calibration="calibrationOpen = true"
      @back="$emit('back')"
      @update:template-name="$emit('update:templateName', $event)"
      @bindings="$emit('bindings')"
      @versions="$emit('versions')"
      @publish="$emit('publish')"
    />
    <NAlert v-if="bannerError" type="error" closable class="designer-banner" @close="clearBanner">
      {{ bannerError }}
    </NAlert>
    <NAlert v-else-if="bannerNotice" type="info" closable class="designer-banner" @close="clearBanner">
      {{ bannerNotice }}
    </NAlert>
    <div class="designer-layout-scroll">
      <div
        class="designer-layout"
        :class="{
          'left-closed': !store.leftPanelOpen,
          'right-closed': !store.rightPanelOpen,
        }"
      >
        <aside v-if="store.leftPanelOpen" class="designer-aside">
          <header class="panel-chrome">
            <span>组件</span>
            <button type="button" class="panel-close" title="关闭组件面板" aria-label="关闭组件面板" @click="store.leftPanelOpen = false">
              <NIcon :component="CloseOutline" :size="14" />
            </button>
          </header>
          <NTabs v-model:value="leftTab" type="segment" size="small" class="aside-tabs">
            <NTabPane name="palette" tab="组件">
              <PrintElementPalette />
            </NTabPane>
            <NTabPane name="fields" tab="数据源">
              <PrintFieldTree />
            </NTabPane>
            <NTabPane name="structure" tab="结构">
              <PrintSectionList />
            </NTabPane>
          </NTabs>
        </aside>
        <PrintCanvas :context="designerContext" :resolve-file="resolveFile" />
        <aside v-if="store.rightPanelOpen" class="designer-properties">
          <header class="panel-chrome">
            <span>属性</span>
            <button type="button" class="panel-close" title="关闭属性面板" aria-label="关闭属性面板" @click="store.rightPanelOpen = false">
              <NIcon :component="CloseOutline" :size="14" />
            </button>
          </header>
          <NTabs v-model:value="panel" type="segment" size="small" class="property-tabs">
            <NTabPane name="selection" tab="属性">
              <PropertyTabsPanel />
            </NTabPane>
            <NTabPane name="paper" tab="纸张">
              <PaperPanel />
            </NTabPane>
          </NTabs>
          <section v-if="store.fieldIssues.length" class="designer-group issue-group">
            <h3>失效字段（{{ store.fieldIssues.length }}）</h3>
            <button v-for="issue in store.fieldIssues" :key="issue.path" type="button" class="issue-button" @click="locateIssue(issue)">
              {{ issue.message }} · 定位
            </button>
          </section>
        </aside>
      </div>
    </div>
    <NModal v-model:show="store.previewOpen" preset="card" title="打印预览" :content-style="{ padding: 0, height: 'calc(92vh - 58px)', overflow: 'hidden' }" :style="{ width: '96vw', maxWidth: '1500px', height: '92vh' }" :mask-closable="false">
      <PrintPreview v-if="store.previewOpen" :data-label="previewDataLabel" :allow-print="!saveDraft" :template="store.document" :template-name="templateName" :context="designerContext" :catalog="store.catalog" :resolve-file="resolveFile" />
    </NModal>
    <NModal v-model:show="calibrationOpen" preset="card" title="打印校准与本机验收" :content-style="{ padding: 0, height: 'calc(90vh - 58px)', overflow: 'hidden' }" :style="{ width: '96vw', maxWidth: '1420px', height: '90vh' }" :mask-closable="false">
      <PrintCalibration v-if="calibrationOpen" :initial-paper="store.document.paper" />
    </NModal>
    <NModal :show="confirmOpen" preset="dialog" type="warning" title="放弃未保存的修改？" content="当前打印模板有未保存的修改，放弃后将载入其他内容。" positive-text="放弃修改" negative-text="继续编辑" :mask-closable="false" @positive-click="answerDiscard(true)" @negative-click="answerDiscard(false)" @close="answerDiscard(false)" @esc="answerDiscard(false)" />
    <NModal v-model:show="protocolOpen" preset="card" title="模板导入 / 导出" :style="{ width: 'min(760px, 94vw)' }">
      <p>仅包含模板结构和绑定，不包含预览数据。导入会替换当前模板，可撤销。</p>
      <NAlert v-if="protocolError" type="error">
        {{ protocolError }}
      </NAlert>
      <NInput v-model:value="protocolText" type="textarea" :autosize="{ minRows: 12, maxRows: 20 }" aria-label="模板 JSON" />
      <div class="panel-row protocol-actions">
        <NButton @click="download">
          下载当前模板
        </NButton><NButton type="primary" @click="importProtocol">
          校验并导入
        </NButton>
      </div>
    </NModal>
  </div>
</template>

<style scoped>
.print-designer {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: var(--bg-primary);
  color: var(--text-primary);
  border: 1px solid var(--border-light);
  border-radius: 6px;
  overflow: hidden;
}
.designer-banner :deep(.n-alert) {
  border-radius: 0;
}
.designer-banner {
  flex: none;
}
.designer-layout-scroll {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}
.designer-layout {
  --left-panel-width: 220px;
  --right-panel-width: 280px;
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 0;
  display: grid;
  grid-template-columns: var(--left-panel-width) minmax(0, 1fr) var(--right-panel-width);
  transition: grid-template-columns 0.16s ease;
}
.designer-layout.left-closed {
  grid-template-columns: minmax(0, 1fr) var(--right-panel-width);
}
.designer-layout.right-closed {
  grid-template-columns: var(--left-panel-width) minmax(0, 1fr);
}
.designer-layout.left-closed.right-closed {
  grid-template-columns: minmax(0, 1fr);
}
.designer-aside,
.designer-properties {
  min-height: 0;
  min-width: 0;
  overflow: auto;
  scrollbar-gutter: stable;
}
.designer-aside {
  border-right: 1px solid var(--border-light);
  background: var(--bg-primary);
  padding: 0 3px 2px;
  display: flex;
  flex-direction: column;
}
.designer-properties {
  border-left: 1px solid var(--border-light);
  padding: 0 4px 6px;
  background: var(--bg-primary);
  display: flex;
  flex-direction: column;
}
.panel-chrome {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 26px;
  padding: 2px 2px 0;
  flex: none;
  color: var(--text-secondary);
  font-size: 11px;
  font-weight: 600;
}
.panel-close {
  width: 22px;
  height: 22px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: 1px solid transparent;
  border-radius: 4px;
  color: var(--text-tertiary);
  background: transparent;
  cursor: pointer;
}
.panel-close:hover {
  color: var(--text-primary);
  border-color: var(--border-light);
  background: var(--gray-100);
}
.aside-tabs,
.property-tabs {
  --n-tab-gap: 2px;
  flex: 1;
  min-height: 0;
}
.aside-tabs :deep(.n-tabs-nav),
.property-tabs :deep(.n-tabs-nav) {
  --n-tab-padding: 4px 0;
}
.aside-tabs :deep(.n-tabs-tab),
.property-tabs :deep(.n-tabs-tab) {
  font-size: 11px;
  padding: 2px 0 !important;
}
.aside-tabs :deep(.n-tabs-pane-wrapper),
.property-tabs :deep(.n-tabs-pane-wrapper) {
  padding-top: 2px;
}
:deep(.designer-group) {
  padding: 4px 2px 6px;
  border-bottom: 1px solid var(--border-light);
}
:deep(.designer-group h3) {
  font-size: 10px;
  font-weight: 600;
  margin: 0 0 3px;
  color: var(--text-tertiary);
  letter-spacing: 0.02em;
}
:deep(.designer-group h3:not(:first-child)) {
  margin-top: 6px;
}
:deep(.panel-grid) {
  display: grid;
  grid-template-columns: 1fr;
  gap: 4px 0;
}
:deep(.panel-row) {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}
:deep(.muted) {
  font-size: 10px;
  color: var(--text-tertiary);
  overflow-wrap: anywhere;
  margin: 2px 0;
}
:deep(.n-form-item) {
  margin-top: 0;
  margin-bottom: 4px;
  width: 100%;
}
:deep(.n-form-item-blank) {
  width: 100%;
  min-width: 0;
}
:deep(.n-form-item-label) {
  font-size: 11px !important;
  padding-bottom: 2px !important;
}
:deep(.n-form-item-feedback-wrapper) {
  min-height: 0 !important;
  height: 0;
  padding: 0 !important;
}
.issue-group {
  margin-top: 4px;
}
.issue-button {
  background: transparent;
  border: 0;
  color: var(--primary-color);
  text-align: left;
  cursor: pointer;
  overflow-wrap: anywhere;
  font-size: 11px;
  padding: 2px 0;
}
.protocol-actions {
  margin-top: 10px;
}
@media (max-width: 900px) {
  .designer-layout {
    display: block;
  }
  .designer-layout > :deep(.print-canvas) {
    width: 100%;
    height: 100%;
  }
  .designer-aside,
  .designer-properties {
    position: absolute;
    z-index: 30;
    top: 0;
    bottom: 0;
    width: min(240px, 78vw);
    box-shadow: 0 8px 24px rgb(15 23 42 / 16%);
    padding: 6px;
  }
  .designer-aside {
    left: 0;
  }
  .designer-properties {
    right: 0;
  }
}
</style>
