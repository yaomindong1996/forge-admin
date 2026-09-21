<script setup>
import { AddOutline, ChevronBackOutline, ChevronForwardOutline, ContractOutline, DocumentOutline, ExpandOutline, PrintOutline, RemoveOutline } from '@vicons/ionicons5'
import { NAlert, NButton, NEmpty, NIcon, NSelect, useThemeVars } from 'naive-ui'
import { computed, nextTick, onBeforeUnmount, onMounted, ref, shallowRef, watch } from 'vue'
import { layoutPrintDocument } from '../engine/layout'
import { createBrowserMeasurer } from '../engine/measure'
import { buildPrintExportFileName } from '../protocol/exportFileName'
import { validateFieldCatalog } from '../protocol/fieldCatalog'
import { PrintError } from '../protocol/types'
import { mmToPx } from '../protocol/units'
import { assertPrintDocument } from '../protocol/validate'
import { createBrowserPrintSession } from './browserPrint'
import { exportPrintPdf } from './exportPrintPdf'
import PrintPage from './PrintPage.vue'
import PrintPreviewSkeleton from './PrintPreviewSkeleton.vue'
import { loadPrintResources } from './printResourceLoader'

const props = defineProps({
  template: { type: Object, required: true },
  context: { type: Object, required: true },
  catalog: { type: Array, default: () => [] },
  templateVersion: { type: [String, Number], default: null },
  templateName: { type: String, default: '' },
  dataLabel: { type: String, default: '当前已保存数据' },
  /** 嵌入运行页时隐藏重复标题，只保留页码/缩放/打印操作 */
  embedded: { type: Boolean, default: false },
  allowPrint: { type: Boolean, default: true },
  resolveFile: { type: Function, default: undefined },
})
const emit = defineEmits(['ready', 'error', 'execution'])
const theme = useThemeVars()
const themeStyle = computed(() => ({ '--bg-primary': theme.value.cardColor, '--gray-100': theme.value.bodyColor, '--text-primary': theme.value.textColor1, '--text-tertiary': theme.value.textColor3, '--border-light': theme.value.borderColor, '--primary-color': theme.value.primaryColor }))
const layout = shallowRef(null)
const loading = ref(false)
const printing = ref(false)
const exporting = ref(false)
const error = shallowRef(null)
const zoom = ref(0.8)
const fitMode = ref('width')
const currentPage = ref(1)
const scrollRef = ref(null)
const previewRoot = ref(null)
const isFullscreen = ref(false)
const pageRefs = []
const standardZooms = [0.4, 0.5, 0.65, 0.8, 1, 1.25, 1.5]
const zoomOptions = computed(() => [...new Set([...standardZooms, Number(zoom.value.toFixed(2))])]
  .sort((a, b) => a - b)
  .map(value => ({ value, label: `${Math.round(value * 100)}%` })))
const paperLabel = computed(() => layout.value ? `${layout.value.geometry.widthMm} × ${layout.value.geometry.heightMm} mm` : '')
const showThumbs = computed(() => (layout.value?.pages?.length || 0) > 1)
const thumbZoom = computed(() => {
  if (!layout.value)
    return 0.12
  return Number(Math.min(0.2, Math.max(0.08, 72 / mmToPx(layout.value.geometry.widthMm))).toFixed(3))
})
let printSession
let generation = 0
let activeController
let resizeObserver

watch(() => [props.template, props.context, props.catalog, props.templateVersion], async (_, __, onCleanup) => {
  const current = ++generation
  const controller = new AbortController()
  activeController = controller
  let resources
  onCleanup(() => {
    if (current === generation)
      generation++
    controller.abort()
    printSession?.dispose()
    resources?.dispose()
  })
  layout.value = null
  error.value = null
  loading.value = true
  currentPage.value = 1
  try {
    assertPrintDocument(props.template)
    const issues = validateFieldCatalog(props.template, props.catalog)
    if (issues.length)
      throw new PrintError('FIELD_NOT_ALLOWED', issues[0].message, issues[0].path)
    resources = await loadPrintResources(props.template, props.context, { signal: controller.signal, resolveFile: props.resolveFile, catalog: props.catalog })
    if (controller.signal.aborted) {
      resources.dispose()
      return
    }
    const measure = createBrowserMeasurer()
    try {
      layout.value = layoutPrintDocument(props.template, props.context, { measure, resources, catalog: props.catalog, templateVersion: props.templateVersion })
      emit('ready', layout.value)
      await nextTick()
      fitWidth()
    }
    finally {
      measure.dispose()
    }
  }
  catch (reason) {
    resources?.dispose()
    if (!controller.signal.aborted && current === generation) {
      error.value = reason
      emit('error', reason)
    }
  }
  finally {
    if (current === generation)
      loading.value = false
  }
}, { immediate: true, deep: true })

function fitWidth() {
  if (!layout.value || !scrollRef.value)
    return
  const available = Math.max(240, scrollRef.value.clientWidth - 104)
  zoom.value = Number(Math.min(1.25, Math.max(0.35, available / mmToPx(layout.value.geometry.widthMm))).toFixed(2))
  fitMode.value = 'width'
}

function setZoom(value) {
  zoom.value = value
  fitMode.value = 'manual'
}

function stepZoom(step) {
  const current = standardZooms.reduce((best, value, index) => Math.abs(value - zoom.value) < Math.abs(standardZooms[best] - zoom.value) ? index : best, 0)
  const next = Math.min(standardZooms.length - 1, Math.max(0, current + step))
  setZoom(standardZooms[next])
}

function setPageRef(element, index) {
  pageRefs[index] = element
}

function goToPage(page) {
  if (!layout.value)
    return
  const target = Math.min(layout.value.pages.length, Math.max(1, page))
  currentPage.value = target
  pageRefs[target - 1]?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function trackPage() {
  const viewport = scrollRef.value
  if (!viewport || !pageRefs.length)
    return
  const top = viewport.getBoundingClientRect().top + 24
  let closest = { index: 0, distance: Number.POSITIVE_INFINITY }
  pageRefs.forEach((element, index) => {
    if (!element)
      return
    const distance = Math.abs(element.getBoundingClientRect().top - top)
    if (distance < closest.distance)
      closest = { index, distance }
  })
  currentPage.value = closest.index + 1
}

async function print() {
  if (!props.allowPrint || !layout.value || printing.value)
    return
  printing.value = true
  const current = generation
  try {
    printSession?.dispose()
    const session = await createBrowserPrintSession(layout.value, { signal: activeController.signal, onEvent: event => emit('execution', event) })
    if (current !== generation) {
      session.dispose()
      return
    }
    printSession = session
    session.print()
  }
  catch (reason) {
    if (current === generation) {
      error.value = reason
      emit('error', reason)
    }
  }
  finally {
    printing.value = false
  }
}

/** Download a client-side PDF of the already paginated paper. */
async function exportPdf() {
  if (!props.allowPrint || !layout.value || printing.value || exporting.value)
    return
  exporting.value = true
  const current = generation
  try {
    await nextTick()
    await exportPrintPdf(layout.value, {
      sources: pageRefs.map(shell => shell?.querySelector?.('[data-print-page]')).filter(Boolean),
      signal: activeController.signal,
      filename: buildPrintExportFileName({
        pattern: props.template.exportFileName,
        templateName: props.templateName,
        context: props.context,
      }),
      onEvent: event => emit('execution', event),
    })
  }
  catch (reason) {
    if (current === generation) {
      error.value = reason
      emit('error', reason)
    }
  }
  finally {
    exporting.value = false
  }
}

async function toggleFullscreen() {
  const target = previewRoot.value
  if (!target)
    return
  try {
    if (!document.fullscreenElement) {
      await target.requestFullscreen?.()
      isFullscreen.value = true
    }
    else {
      await document.exitFullscreen?.()
      isFullscreen.value = false
    }
  }
  catch (reason) {
    error.value = reason
  }
}

function onFullscreenChange() {
  isFullscreen.value = document.fullscreenElement === previewRoot.value
}

onMounted(() => {
  document.addEventListener('fullscreenchange', onFullscreenChange)
  if (typeof ResizeObserver !== 'undefined') {
    resizeObserver = new ResizeObserver(() => {
      if (fitMode.value === 'width')
        fitWidth()
    })
    if (scrollRef.value)
      resizeObserver.observe(scrollRef.value)
  }
})
onBeforeUnmount(() => {
  document.removeEventListener('fullscreenchange', onFullscreenChange)
  resizeObserver?.disconnect()
})
</script>

<template>
  <section ref="previewRoot" class="print-preview" :class="{ fullscreen: isFullscreen, embedded }" :style="themeStyle">
    <div class="print-toolbar">
      <div v-if="!embedded" class="preview-identity">
        <strong>打印预览</strong>
        <span v-if="layout">{{ [dataLabel, paperLabel, `共 ${layout.pages.length} 页`].filter(Boolean).join(' · ') }}</span>
        <span v-else>{{ dataLabel }}</span>
      </div>
      <div v-else-if="layout" class="preview-meta">
        {{ paperLabel }} · {{ layout.pages.length }} 页
      </div>
      <div v-else class="toolbar-spacer" />

      <div v-if="layout" class="page-navigation" aria-label="预览页码导航">
        <button type="button" class="preview-tool" title="上一页" aria-label="上一页" :disabled="currentPage <= 1" @click="goToPage(currentPage - 1)">
          <NIcon :component="ChevronBackOutline" />
        </button>
        <span><b>{{ currentPage }}</b> / {{ layout.pages.length }}</span>
        <button type="button" class="preview-tool" title="下一页" aria-label="下一页" :disabled="currentPage >= layout.pages.length" @click="goToPage(currentPage + 1)">
          <NIcon :component="ChevronForwardOutline" />
        </button>
      </div>

      <div class="print-actions">
        <button type="button" class="preview-tool" title="适合宽度" aria-label="适合宽度" :class="{ active: fitMode === 'width' }" :disabled="!layout" @click="fitWidth">
          <NIcon :component="ContractOutline" />
        </button>
        <button type="button" class="preview-tool" :title="isFullscreen ? '退出全屏' : '全屏预览'" :aria-label="isFullscreen ? '退出全屏' : '全屏预览'" :disabled="!layout" @click="toggleFullscreen">
          <NIcon :component="isFullscreen ? ContractOutline : ExpandOutline" />
        </button>
        <button type="button" class="preview-tool" title="缩小预览" aria-label="缩小预览" :disabled="zoom <= standardZooms[0]" @click="stepZoom(-1)">
          <NIcon :component="RemoveOutline" />
        </button>
        <NSelect :value="zoom" aria-label="预览缩放" :options="zoomOptions" class="preview-zoom" size="small" @update:value="setZoom" />
        <button type="button" class="preview-tool" title="放大预览" aria-label="放大预览" :disabled="zoom >= standardZooms.at(-1)" @click="stepZoom(1)">
          <NIcon :component="AddOutline" />
        </button>
        <NButton v-if="allowPrint" size="small" quaternary :disabled="!layout || loading || !!error" :loading="exporting" @click="exportPdf">
          <template #icon>
            <NIcon :component="DocumentOutline" />
          </template>
          PDF
        </NButton>
        <NButton v-if="allowPrint" type="primary" size="small" :disabled="!layout || loading || !!error" :loading="printing" @click="print">
          <template #icon>
            <NIcon :component="PrintOutline" />
          </template>
          打印
        </NButton>
      </div>
    </div>
    <NAlert v-if="error" type="error" :bordered="false" class="preview-alert">
      {{ error.message }}<span v-if="error.path">（{{ error.path }}）</span>
    </NAlert>
    <div class="preview-body">
      <aside v-if="showThumbs" class="page-thumbs" aria-label="页面缩略图">
        <button
          v-for="page in layout.pages"
          :key="page.number"
          type="button"
          class="thumb-item"
          :class="{ active: currentPage === page.number }"
          :title="`第 ${page.number} 页`"
          :aria-label="`第 ${page.number} 页`"
          :aria-current="currentPage === page.number ? 'page' : undefined"
          @click="goToPage(page.number)"
        >
          <div
            class="thumb-paper"
            :style="{
              width: `${layout.geometry.widthMm * thumbZoom}mm`,
              height: `${layout.geometry.heightMm * thumbZoom}mm`,
            }"
          >
            <div :style="{ transform: `scale(${thumbZoom})`, transformOrigin: 'top left' }">
              <PrintPage :page="page" :geometry="layout.geometry" :watermark="layout.watermark" :overlay="layout.overlay" />
            </div>
          </div>
          <span class="thumb-label">{{ page.number }}</span>
        </button>
      </aside>
      <div ref="scrollRef" class="print-scroll" @scroll.passive="trackPage">
        <PrintPreviewSkeleton v-if="loading && !layout" />
        <div v-else-if="layout" class="print-pages">
          <article
            v-for="(page, index) in layout.pages"
            :key="page.number"
            :ref="element => setPageRef(element, index)"
            class="preview-page-shell"
          >
            <div class="print-paper-space" :style="{ width: `${layout.geometry.widthMm * zoom}mm`, height: `${layout.geometry.heightMm * zoom}mm` }">
              <div :style="{ transform: `scale(${zoom})`, transformOrigin: 'top left' }">
                <PrintPage :page="page" :geometry="layout.geometry" :watermark="layout.watermark" :overlay="layout.overlay" />
              </div>
            </div>
            <div class="page-caption">
              第 {{ page.number }} 页
            </div>
          </article>
        </div>
        <NEmpty v-else-if="!error" description="暂无打印内容" />
      </div>
    </div>
  </section>
</template>

<style scoped>
.print-preview {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  color: var(--text-primary, #222);
  background: var(--bg-primary, #fff);
}
.print-preview.fullscreen {
  background: var(--gray-100, #f1f5f9);
}
.print-toolbar {
  position: relative;
  z-index: 2;
  min-height: 40px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 8px;
  border-bottom: 1px solid var(--border-light, #ddd);
}
.print-preview:not(.embedded) .print-toolbar {
  display: grid;
  grid-template-columns: minmax(140px, 1fr) auto minmax(200px, 1fr);
}
.preview-identity {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 1px;
}
.preview-identity strong {
  font-size: 13px;
  line-height: 1.2;
}
.preview-identity span,
.preview-meta {
  overflow: hidden;
  color: var(--text-tertiary, #64748b);
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.preview-meta {
  flex: none;
  padding-left: 2px;
}
.toolbar-spacer {
  flex: 1 1 auto;
  min-width: 4px;
}
.page-navigation,
.print-actions {
  display: flex;
  align-items: center;
  gap: 2px;
  flex: none;
}
.page-navigation span {
  min-width: 44px;
  color: var(--text-tertiary, #64748b);
  font-size: 12px;
  text-align: center;
}
.page-navigation b {
  color: var(--text-primary, #222);
}
.print-actions {
  margin-left: auto;
}
.preview-tool {
  width: 26px;
  height: 26px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: 1px solid transparent;
  border-radius: 4px;
  color: var(--text-primary, #334155);
  background: transparent;
  cursor: pointer;
  font-size: 15px;
}
.preview-tool:hover:not(:disabled),
.preview-tool.active {
  color: var(--primary-color, #356cde);
  border-color: color-mix(in srgb, var(--primary-color, #356cde) 24%, transparent);
  background: color-mix(in srgb, var(--primary-color, #356cde) 8%, transparent);
}
.preview-tool:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}
.preview-zoom {
  width: 96px;
}
.preview-alert {
  flex: none;
  padding: 6px 10px;
}
.preview-body {
  flex: 1;
  min-height: 0;
  display: flex;
  align-items: stretch;
}
.page-thumbs {
  flex: none;
  width: 96px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 10px 8px;
  overflow: auto;
  border-right: 1px solid var(--border-light, #ddd);
  background: #f1f4f8;
  scrollbar-gutter: stable;
}
.thumb-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 4px;
  border: 1px solid transparent;
  border-radius: 6px;
  background: transparent;
  cursor: pointer;
}
.thumb-item:hover {
  border-color: color-mix(in srgb, var(--primary-color, #356cde) 30%, transparent);
  background: #fff;
}
.thumb-item.active {
  border-color: var(--primary-color, #356cde);
  background: #fff;
  box-shadow: 0 0 0 1px color-mix(in srgb, var(--primary-color, #356cde) 35%, transparent);
}
.thumb-paper {
  overflow: hidden;
  background: #fff;
  outline: 1px solid rgb(15 23 42 / 12%);
  box-shadow: 0 1px 4px rgb(15 23 42 / 8%);
  pointer-events: none;
}
.thumb-label {
  color: var(--text-tertiary, #64748b);
  font-size: 11px;
  line-height: 1;
}
.thumb-item.active .thumb-label {
  color: var(--primary-color, #356cde);
  font-weight: 600;
}
.print-scroll {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 12px 16px 20px;
  background: #e8ecf1;
  scrollbar-gutter: stable;
}
.print-pages {
  min-width: max-content;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
}
.preview-page-shell {
  position: relative;
  flex: none;
  scroll-margin-top: 12px;
}
.page-caption {
  margin-top: 6px;
  color: #6b7280;
  font-size: 11px;
  text-align: center;
}
.print-paper-space {
  flex: none;
  overflow: hidden;
  outline: 1px solid rgb(15 23 42 / 10%);
  background: #fff;
  box-shadow: 0 2px 10px rgb(15 23 42 / 10%);
}
@media (max-width: 760px) {
  .print-preview:not(.embedded) .print-toolbar {
    grid-template-columns: 1fr auto;
  }
  .preview-identity span,
  .preview-meta,
  .page-navigation {
    display: none;
  }
  .page-thumbs {
    display: none;
  }
  .print-scroll {
    padding: 10px 10px 16px;
  }
}
</style>
