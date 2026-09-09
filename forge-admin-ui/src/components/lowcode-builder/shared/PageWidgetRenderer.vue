<template>
  <div class="page-widget-renderer" :class="[`widget-${componentKey}`]">
    <template v-if="componentKey === 'rich-text'">
      <div class="rich-editor-shell" :class="{ readonly: propsData.readonly }">
        <div class="rich-editor-head">
          <strong>{{ propsData.title || '富文本' }}</strong>
          <span>{{ propsData.editorMode === 'source' ? '源码模式' : 'WangEditor' }}</span>
        </div>
        <pre v-if="propsData.editorMode === 'source'" class="code-preview html-code"><code>{{ richTextContent }}</code></pre>
        <div
          v-else
          class="wang-editor-wrap"
          :style="richContentStyle"
        >
          <WangToolbar
            class="wang-toolbar"
            :editor="wangEditorRef"
            :default-config="wangToolbarConfig"
            :mode="propsData.toolbarMode || 'default'"
          />
          <WangEditor
            class="wang-editor"
            :model-value="richTextContent"
            :default-config="wangEditorConfig"
            :mode="propsData.editorModeName || 'default'"
            @on-created="handleWangEditorCreated"
            @update:model-value="handleWangEditorUpdate"
          />
        </div>
      </div>
    </template>

    <template v-else-if="componentKey === 'transfer'">
      <div class="transfer-widget-shell">
        <div class="widget-title-row">
          <strong>{{ propsData.title || '穿梭框' }}</strong>
          <span>{{ transferValue.length }}/{{ transferOptions.length }}</span>
        </div>
        <n-transfer
          :value="transferValue"
          :options="transferOptions"
          :source-title="propsData.sourceTitle || '可选项'"
          :target-title="propsData.targetTitle || '已选项'"
          :filterable="propsData.filterable !== false"
          :virtual-scroll="propsData.virtualScroll === true"
          :loading="transferLoading"
          :disabled="readonly || propsData.disabled === true"
          size="small"
          @update:value="handleTransferUpdate"
        />
        <div v-if="transferError" class="widget-error">
          {{ transferError }}
        </div>
      </div>
    </template>

    <template v-else-if="componentKey === 'watermark'">
      <n-watermark
        class="watermark-widget"
        :content="watermarkContent"
        :cross="propsData.cross === true"
        :debug="propsData.debug === true"
        :font-size="Number(propsData.fontSize || 14)"
        :font-family="propsData.fontFamily || undefined"
        :font-style="propsData.fontStyle || 'normal'"
        :font-variant="propsData.fontVariant || ''"
        :font-weight="Number(propsData.fontWeight || 400)"
        :font-color="propsData.fontColor || 'rgba(128, 128, 128, .3)'"
        :fullscreen="propsData.fullscreen === true"
        :global-rotate="Number(propsData.globalRotate || 0)"
        :line-height="Number(propsData.lineHeight || 14)"
        :height="Number(propsData.height || 32)"
        :image="propsData.image || undefined"
        :image-height="toOptionalNumber(propsData.imageHeight)"
        :image-opacity="Number(propsData.imageOpacity ?? 1)"
        :image-width="toOptionalNumber(propsData.imageWidth)"
        :rotate="Number(propsData.rotate || 0)"
        :selectable="propsData.selectable !== false"
        :text-align="propsData.textAlign || 'left'"
        :width="Number(propsData.width || 32)"
        :x-gap="Number(propsData.xGap || 0)"
        :x-offset="Number(propsData.xOffset || 0)"
        :y-gap="Number(propsData.yGap || 0)"
        :y-offset="Number(propsData.yOffset || 0)"
        :z-index="Number(propsData.zIndex || 10)"
      >
        <div class="watermark-inner">
          <strong>{{ propsData.previewText || '水印覆盖区域' }}</strong>
          <span>{{ boundWatermarkText || '水印文字' }}</span>
        </div>
      </n-watermark>
    </template>

    <template v-else-if="componentKey === 'markdown'">
      <div class="markdown-shell" :class="`mode-${propsData.previewMode || 'split'}`">
        <div v-if="propsData.showTitle !== false" class="widget-title">
          {{ markdownTitle || 'Markdown' }}
        </div>
        <VMdEditor
          class="markdown-editor"
          :model-value="markdownContent"
          :mode="markdownMode"
          :height="`${Number(propsData.height || 320)}px`"
          :disabled-menus="markdownDisabledMenus"
          :disabled="propsData.readonly === true"
          @update:model-value="handleMarkdownUpdate"
        />
      </div>
    </template>

    <template v-else-if="componentKey === 'barcode'">
      <div class="barcode-widget" :style="{ background: propsData.background || 'transparent' }">
        <svg ref="barcodeSvgRef" class="barcode-svg" />
        <span v-if="!barcodeValid" class="widget-error">条形码内容无效</span>
      </div>
    </template>

    <template v-else-if="componentKey === 'qrcode'">
      <div class="qrcode-widget">
        <QRCodeVue3
          :key="qrcodeValue || ''"
          :width="Number(propsData.size || 132)"
          :height="Number(propsData.size || 132)"
          :value="String(qrcodeValue || 'https://forge.local')"
          :margin="Number(propsData.margin || 0)"
          :qr-options="qrcodeQrOptions"
          :dots-options="qrcodeDotsOptions"
          :background-options="qrcodeBackgroundOptions"
          :corners-square-options="qrcodeCornersSquareOptions"
          :corners-dot-options="qrcodeCornersDotOptions"
        />
        <strong v-if="qrcodeTitle">{{ qrcodeTitle }}</strong>
        <small v-if="propsData.showText !== false">{{ qrcodeValue }}</small>
      </div>
    </template>

    <template v-else-if="componentKey === 'calendar'">
      <div class="naive-widget-shell calendar-widget">
        <div v-if="propsData.showTitle !== false" class="widget-title">
          {{ propsData.title || '日历' }}
        </div>
        <n-calendar
          :value="calendarValue"
          :size="propsData.size || 'medium'"
          @update:value="value => emitPropsPatch({ value })"
        />
      </div>
    </template>

    <template v-else-if="componentKey === 'code'">
      <div class="naive-widget-shell code-widget">
        <div v-if="propsData.title" class="widget-title">
          {{ propsData.title }}
        </div>
        <n-code
          :code="codeContent"
          :language="propsData.language || 'javascript'"
          :show-line-numbers="propsData.showLineNumbers !== false"
          :word-wrap="propsData.wordWrap !== false"
          :trim="propsData.trim !== false"
        />
      </div>
    </template>

    <template v-else-if="componentKey === 'countdown'">
      <div class="naive-widget-shell countdown-widget">
        <span>{{ propsData.title || '倒计时' }}</span>
        <n-countdown
          :duration="countdownDuration"
          :active="propsData.active !== false"
          :precision="Number(propsData.precision || 0)"
          :separator="propsData.separator || ':'"
        />
      </div>
    </template>

    <template v-else-if="componentKey === 'descriptions'">
      <div class="naive-widget-shell descriptions-widget">
        <n-descriptions
          :title="propsData.title || undefined"
          :column="Number(propsData.column || 2)"
          :bordered="propsData.bordered === true"
          :label-placement="propsData.labelPlacement || 'left'"
          :size="propsData.size || 'small'"
        >
          <n-descriptions-item
            v-for="item in descriptionItems"
            :key="item.label"
            :label="item.label"
          >
            {{ item.value }}
          </n-descriptions-item>
        </n-descriptions>
      </div>
    </template>

    <template v-else-if="componentKey === 'announcement'">
      <n-alert
        class="announcement-widget"
        :title="announcementTitle"
        :type="propsData.type || 'info'"
        :bordered="propsData.bordered === true"
        :show-icon="propsData.showIcon !== false"
        :closable="propsData.closable === true"
      >
        {{ announcementContent }}
      </n-alert>
    </template>

    <template v-else-if="componentKey === 'list'">
      <div class="naive-widget-shell list-widget">
        <div v-if="propsData.title" class="widget-title">
          {{ propsData.title }}
        </div>
        <n-list
          :bordered="propsData.bordered === true"
          :hoverable="propsData.hoverable !== false"
          :size="propsData.size || 'small'"
        >
          <n-list-item v-for="item in listItems" :key="item.title || item.description">
            <n-thing :title="item.title" :description="item.description">
              <template v-if="item.meta" #header-extra>
                {{ item.meta }}
              </template>
            </n-thing>
          </n-list-item>
        </n-list>
      </div>
    </template>

    <template v-else-if="componentKey === 'log'">
      <div class="naive-widget-shell log-widget">
        <div v-if="propsData.title" class="widget-title">
          {{ propsData.title }}
        </div>
        <n-log
          :log="logContent"
          :rows="Number(propsData.rows || 6)"
          :font-size="Number(propsData.fontSize || 12)"
          :trim="propsData.trim !== false"
          :language="propsData.language || 'log'"
        />
      </div>
    </template>

    <template v-else-if="componentKey === 'number-animation'">
      <div class="naive-widget-shell number-animation-widget">
        <span>{{ propsData.title || '数值动画' }}</span>
        <strong :style="{ color: propsData.color || '#2563eb' }">
          {{ propsData.prefix || '' }}<n-number-animation
            :from="Number(propsData.from || 0)"
            :to="numberAnimationTo"
            :precision="Number(propsData.precision || 0)"
            :duration="Number(propsData.duration || 1200)"
          />{{ propsData.suffix || '' }}
        </strong>
      </div>
    </template>

    <template v-else-if="componentKey === 'breadcrumb'">
      <div class="naive-widget-shell breadcrumb-widget">
        <n-breadcrumb>
          <n-breadcrumb-item v-for="item in breadcrumbItems" :key="item.label">
            {{ item.label }}
          </n-breadcrumb-item>
        </n-breadcrumb>
      </div>
    </template>

    <template v-else-if="componentKey === 'menu'">
      <div class="naive-widget-shell menu-widget">
        <n-menu
          :value="propsData.value || menuOptions[0]?.key"
          :options="menuOptions"
          :mode="propsData.mode || 'vertical'"
          :collapsed="propsData.collapsed === true"
        />
      </div>
    </template>

    <template v-else-if="componentKey === 'pagination'">
      <div class="naive-widget-shell pagination-widget">
        <n-pagination
          :page="Number(propsData.page || 1)"
          :page-size="Number(propsData.pageSize || 10)"
          :item-count="paginationItemCount"
          :show-size-picker="propsData.showSizePicker !== false"
          :simple="propsData.simple === true"
          @update:page="page => emitPropsPatch({ page })"
          @update:page-size="pageSize => emitPropsPatch({ pageSize })"
        />
      </div>
    </template>

    <template v-else-if="componentKey === 'split'">
      <n-split
        class="split-widget"
        :direction="propsData.direction || 'horizontal'"
        :default-size="Number(propsData.defaultSize || 0.38)"
        :min="Number(propsData.min || 0.2)"
        :max="Number(propsData.max || 0.8)"
      >
        <template #1>
          <section class="split-pane">
            <strong>{{ propsData.pane1Title || '左侧面板' }}</strong>
            <p>{{ propsData.pane1Content || '这里放置筛选、导航或说明内容。' }}</p>
          </section>
        </template>
        <template #2>
          <section class="split-pane">
            <strong>{{ propsData.pane2Title || '右侧面板' }}</strong>
            <p>{{ propsData.pane2Content || '这里放置列表、详情或主要业务内容。' }}</p>
          </section>
        </template>
      </n-split>
    </template>

    <template v-else-if="componentKey === 'html-tag'">
      <component
        :is="safeTagName"
        class="html-widget"
        :role="propsData.semanticRole || undefined"
        v-bind="safeAttributes"
      >
        <span v-if="propsData.renderMode === 'text'">{{ htmlTextContent || 'HTML 标签内容' }}</span>
        <span v-else v-html="safeHtml(htmlContent || htmlTextContent || '')" />
      </component>
      <div class="html-meta">
        &lt;{{ safeTagName }}&gt; · {{ propsData.sanitize === false ? '原始模式需审查' : '安全渲染' }}
      </div>
    </template>

    <template v-else-if="componentKey === 'vue-component'">
      <div class="vue-widget-shell">
        <div class="vue-widget-toolbar">
          <span class="vue-badge">Vue</span>
          <strong>{{ propsData.componentName || 'CustomComponent' }}</strong>
          <em>{{ propsData.previewMode === 'code' ? '代码' : '安全预览' }}</em>
        </div>
        <div v-if="propsData.previewMode === 'code'" class="vue-code-grid">
          <pre class="code-preview"><code>{{ propsData.templateCode }}</code></pre>
          <pre class="code-preview"><code>{{ propsData.scriptCode }}</code></pre>
          <pre class="code-preview"><code>{{ propsData.styleCode }}</code></pre>
        </div>
        <div v-else class="vue-safe-preview" :class="{ 'is-live-template': propsData.previewMode === 'live' }">
          <div class="vue-template-preview" v-html="safeVueTemplateHtml" />
          <details v-if="scopedSafeStyle" class="vue-style-preview">
            <summary>样式代码</summary>
            <pre><code>{{ scopedSafeStyle }}</code></pre>
          </details>
        </div>
        <div class="vue-props-preview">
          <span>Props</span>
          <code>{{ compactPropsJson }}</code>
        </div>
      </div>
    </template>

    <!-- 音频播放器 -->
    <template v-else-if="componentKey === 'audio-player'">
      <div class="naive-widget-shell audio-player-widget">
        <div v-if="propsData.title" class="widget-title">
          {{ propsData.title }}
        </div>
        <audio
          class="audio-player-element"
          :src="propsData.src || ''"
          :controls="propsData.controls !== false"
          :autoplay="propsData.autoplay === true"
          :loop="propsData.loop === true"
          :muted="propsData.muted === true"
          :style="{ width: propsData.width || '100%' }"
        />
        <div v-if="!propsData.src" class="widget-placeholder">
          未设置音频地址
        </div>
      </div>
    </template>

    <!-- 视频播放器 -->
    <template v-else-if="componentKey === 'video-player'">
      <div class="naive-widget-shell video-player-widget">
        <div v-if="propsData.title" class="widget-title">
          {{ propsData.title }}
        </div>
        <video
          class="video-player-element"
          :src="propsData.src || ''"
          :controls="propsData.controls !== false"
          :autoplay="propsData.autoplay === true"
          :loop="propsData.loop === true"
          :muted="propsData.muted === true"
          :poster="propsData.poster || undefined"
          :style="{ width: propsData.width || '100%', height: propsData.height || 'auto' }"
        />
        <div v-if="!propsData.src" class="widget-placeholder">
          未设置视频地址
        </div>
      </div>
    </template>

    <!-- 头像 -->
    <template v-else-if="componentKey === 'avatar'">
      <div class="naive-widget-shell avatar-widget">
        <n-avatar
          :size="Number(propsData.size || 48)"
          :round="propsData.round !== false"
          :src="propsData.src || undefined"
          :style="{ backgroundColor: propsData.backgroundColor || '#ccc' }"
        >
          {{ propsData.text || '' }}
        </n-avatar>
      </div>
    </template>

    <!-- 内嵌框架 -->
    <template v-else-if="componentKey === 'iframe'">
      <div class="naive-widget-shell iframe-widget">
        <div v-if="propsData.title" class="widget-title">
          {{ propsData.title }}
        </div>
        <iframe
          v-if="propsData.src"
          class="iframe-element"
          :src="propsData.src"
          :style="{ width: propsData.width || '100%', height: propsData.height || '300px', border: propsData.border || 'none' }"
          :sandbox="propsData.sandbox || undefined"
          :title="propsData.title || 'iframe'"
        />
        <div v-else class="widget-placeholder">
          未设置 iframe 地址
        </div>
      </div>
    </template>
    <div v-if="bindingLoading" class="widget-binding-state">
      数据加载中...
    </div>
    <div v-else-if="bindingError" class="widget-error">
      {{ bindingError }}
    </div>
  </div>
</template>

<script setup>
import { computed, defineAsyncComponent, nextTick, onBeforeUnmount, onMounted, ref, shallowRef, watch } from 'vue'
import { request } from '@/utils'
import { buildTemplateRefSignature, interpolateTemplate, safeHtml, safeJsonParseObject } from './page-widget-schema'

const props = defineProps({
  componentKey: { type: String, required: true },
  propsData: { type: Object, default: () => ({}) },
  dataContext: { type: Object, default: () => ({}) },
  readonly: { type: Boolean, default: false },
})
const emit = defineEmits(['update:propsData'])

let wangEditorModulePromise
let markdownEditorPromise
let barcodeModulePromise

const WangEditor = defineAsyncComponent(() => loadWangEditorModule().then(module => module.Editor))
const WangToolbar = defineAsyncComponent(() => loadWangEditorModule().then(module => module.Toolbar))
const VMdEditor = defineAsyncComponent(() => loadMarkdownEditor())
const QRCodeVue3 = defineAsyncComponent(() => import('qrcode-vue3').then(module => module.default || module))

function loadWangEditorModule() {
  if (!wangEditorModulePromise) {
    wangEditorModulePromise = Promise.all([
      import('@wangeditor/editor/dist/css/style.css'),
      import('@wangeditor/editor-for-vue'),
    ]).then(([, module]) => module)
  }
  return wangEditorModulePromise
}

function loadMarkdownEditor() {
  if (!markdownEditorPromise) {
    markdownEditorPromise = Promise.all([
      import('@kangc/v-md-editor/lib/style/base-editor.css'),
      import('@kangc/v-md-editor/lib/theme/style/vuepress.css'),
      import('@kangc/v-md-editor'),
      import('@kangc/v-md-editor/lib/theme/vuepress.js'),
      import('prismjs'),
    ]).then(([, , editorModule, themeModule, prismModule]) => {
      const editor = editorModule.default || editorModule
      const theme = themeModule.default || themeModule
      const Prism = prismModule.default || prismModule
      editor.use(theme, { Prism })
      return editor
    })
  }
  return markdownEditorPromise
}

function loadBarcodeModule() {
  if (!barcodeModulePromise)
    barcodeModulePromise = import('jsbarcode').then(module => module.default || module)
  return barcodeModulePromise
}

const transferLoading = ref(false)
const transferError = ref('')
const remoteTransferOptions = ref([])
const bindingLoading = ref(false)
const bindingError = ref('')
const remoteBindingData = ref(null)
const wangEditorRef = shallowRef(null)
const barcodeSvgRef = ref(null)
const barcodeValid = ref(true)

const richContentStyle = computed(() => ({
  minHeight: `${Number(props.propsData.minHeight || 180)}px`,
  backgroundColor: props.propsData.backgroundColor || 'transparent',
  borderColor: props.propsData.bordered === true ? '#e2e8f0' : 'transparent',
  borderWidth: props.propsData.bordered === true ? '1px' : 0,
  borderRadius: 0,
}))
const wangToolbarConfig = computed(() => ({
  excludeKeys: Array.isArray(props.propsData.excludeToolbarKeys) ? props.propsData.excludeToolbarKeys : [],
}))
const wangEditorConfig = computed(() => ({
  placeholder: props.propsData.placeholder || '请输入富文本内容',
  readOnly: props.propsData.readonly === true,
  maxLength: Number(props.propsData.maxLength || 0) || undefined,
  MENU_CONF: safeJsonParseObject(props.propsData.menuConfigText, {}),
}))
const markdownMode = computed(() => {
  const mode = props.propsData.previewMode || 'split'
  if (mode === 'preview')
    return 'preview'
  if (mode === 'source')
    return 'edit'
  return 'editable'
})
const markdownDisabledMenus = computed(() => Array.isArray(props.propsData.disabledMenus) ? props.propsData.disabledMenus : [])
const richTextContent = computed(() => String(resolveBoundContent(props.propsData.content || '', 'content') || ''))
const markdownTitle = computed(() => String(resolveBoundContent(props.propsData.title || 'Markdown', 'title') || 'Markdown'))
const markdownContent = computed(() => String(resolveBoundContent(props.propsData.content || '', 'content') || ''))
const barcodeValue = computed(() => String(resolveBoundValue(props.propsData.value || 'FORGE-2026-0001', 'value') || 'FORGE-2026-0001'))
const qrcodeTitle = computed(() => String(resolveBoundContent(props.propsData.title || '', 'title') || ''))
const qrcodeValue = computed(() => String(resolveBoundValue(props.propsData.value || 'https://forge.local', 'value') || 'https://forge.local'))
const htmlTextContent = computed(() => String(resolveBoundContent(props.propsData.textContent || '', 'title') || ''))
const htmlContent = computed(() => String(resolveBoundContent(props.propsData.htmlContent || props.propsData.textContent || '', 'content') || ''))
const boundWatermarkText = computed(() => String(resolveBoundContent(props.propsData.content || '内部资料', 'content') || '内部资料'))
const watermarkContent = computed(() => {
  if (Array.isArray(props.propsData.content))
    return props.propsData.content
  const text = boundWatermarkText.value
  return text.includes('\n') ? text.split('\n') : text
})
const safeTagName = computed(() => sanitizeHtmlTagName(props.propsData.tagName || 'section'))
const safeAttributes = computed(() => sanitizeAttributes(safeJsonParseObject(props.propsData.attributesText, {})))
const qrcodeQrOptions = computed(() => ({
  typeNumber: 0,
  mode: 'Byte',
  errorCorrectionLevel: props.propsData.errorCorrectionLevel || 'Q',
}))
const qrcodeDotsOptions = computed(() => ({
  type: props.propsData.dotsType || 'square',
  color: props.propsData.foreground || '#0f172a',
}))
const qrcodeBackgroundOptions = computed(() => ({
  color: props.propsData.background || 'transparent',
}))
const qrcodeCornersSquareOptions = computed(() => ({
  type: props.propsData.cornersSquareType || 'square',
  color: props.propsData.cornerColor || props.propsData.foreground || '#0f172a',
}))
const qrcodeCornersDotOptions = computed(() => ({
  type: props.propsData.cornersDotType || 'square',
  color: props.propsData.cornerColor || props.propsData.foreground || '#0f172a',
}))
const vueProps = computed(() => safeJsonParseObject(props.propsData.propsJson, {}))
const compactPropsJson = computed(() => JSON.stringify(vueProps.value))
const safeVueTemplateHtml = computed(() => safeHtml(interpolateTemplate(stripVueTemplate(props.propsData.templateCode || ''), vueProps.value)))
const scopedSafeStyle = computed(() => sanitizeCss(props.propsData.styleCode || ''))
const transferOptions = computed(() => {
  if (props.propsData.dataSourceType === 'remote')
    return remoteTransferOptions.value
  return normalizeOptionItems(props.propsData.options)
})
const transferValue = computed(() => Array.isArray(props.propsData.value) ? props.propsData.value : [])
const boundData = computed(() => resolveBoundData())
const calendarValue = computed(() => Number(resolveBoundValue(props.propsData.value || Date.now(), 'value') || Date.now()))
const codeContent = computed(() => String(resolveBoundContent(props.propsData.code || '', 'code') || ''))
const countdownDuration = computed(() => Number(resolveBoundValue(props.propsData.duration || 3600000, 'duration') || 3600000))
const announcementTitle = computed(() => String(resolveBoundContent(props.propsData.title || '公示标题', 'title') || '公示标题'))
const announcementContent = computed(() => String(resolveBoundContent(props.propsData.content || '这里展示公告、公示、提示或业务说明内容。', 'content') || ''))
const logContent = computed(() => String(resolveBoundContent(props.propsData.log || '', 'log') || ''))
const numberAnimationTo = computed(() => Number(resolveBoundValue(props.propsData.to || 0, 'value') || 0))
const paginationItemCount = computed(() => Number(resolveBoundValue(props.propsData.itemCount || 0, 'total') || 0))
const descriptionItems = computed(() => normalizeDescriptionItems(boundData.value) || safeJsonParseArray(props.propsData.itemsText, [
  { label: '业务对象', value: '订单管理' },
  { label: '状态', value: '启用' },
]))
const listItems = computed(() => normalizeListItems(boundData.value) || safeJsonParseArray(props.propsData.itemsText, []))
const breadcrumbItems = computed(() => normalizeBreadcrumbItems(boundData.value) || safeJsonParseArray(props.propsData.itemsText, []))
const menuOptions = computed(() => normalizeMenuOptions(boundData.value) || safeJsonParseArray(props.propsData.optionsText, []))
// 只跟踪接口地址/参数里实际引用的字段值：被引用字段变化才重新请求，
// 避免表单里任意其它字段每次输入都触发本组件重复请求（也是任意组件间级联联动的触发机制）
const transferRefSignature = computed(() => buildTemplateRefSignature(
  props.dataContext || {},
  props.propsData.optionSource?.api,
  props.propsData.optionSource?.paramsText,
))
watch(
  () => [
    props.componentKey,
    props.propsData.dataSourceType,
    props.propsData.optionSource?.api,
    props.propsData.optionSource?.method,
    props.propsData.optionSource?.paramsText,
    props.propsData.optionSource?.recordsField,
    props.propsData.optionSource?.labelField,
    props.propsData.optionSource?.valueField,
    props.propsData.optionSource?.disabledField,
    transferRefSignature.value,
  ],
  () => {
    if (props.componentKey !== 'transfer' || props.propsData.dataSourceType !== 'remote') {
      remoteTransferOptions.value = []
      transferError.value = ''
      return
    }
    loadTransferOptions()
  },
  { immediate: true },
)
const bindingRefSignature = computed(() => buildTemplateRefSignature(
  props.dataContext || {},
  props.propsData.dataBinding?.api,
  props.propsData.dataBinding?.paramsText,
))
watch(
  () => [
    props.componentKey,
    props.propsData.dataBinding?.enabled,
    props.propsData.dataBinding?.sourceType,
    props.propsData.dataBinding?.api,
    props.propsData.dataBinding?.method,
    props.propsData.dataBinding?.paramsText,
    props.propsData.dataBinding?.dataPath,
    props.propsData.dataBinding?.contextPath,
    bindingRefSignature.value,
  ],
  () => {
    if (props.propsData.dataBinding?.enabled !== true || props.propsData.dataBinding?.sourceType !== 'remote') {
      remoteBindingData.value = null
      bindingError.value = ''
      return
    }
    loadBindingData()
  },
  { immediate: true },
)
watch(
  () => [
    props.componentKey,
    barcodeValue.value,
    props.propsData.format,
    props.propsData.barWidth,
    props.propsData.barHeight,
    props.propsData.showText,
    props.propsData.lineColor,
    props.propsData.background,
    props.propsData.fontSize,
    props.propsData.margin,
  ],
  () => {
    renderBarcode()
  },
  { deep: true },
)

onMounted(renderBarcode)

onBeforeUnmount(() => {
  const editor = wangEditorRef.value
  if (editor?.destroy)
    editor.destroy()
})

function emitPropsPatch(patch = {}) {
  emit('update:propsData', {
    ...(props.propsData || {}),
    ...patch,
  })
}

function handleWangEditorCreated(editor) {
  wangEditorRef.value = editor
}

function handleWangEditorUpdate(value) {
  emitPropsPatch({ content: value || '' })
}

function handleMarkdownUpdate(value) {
  emitPropsPatch({ content: value || '' })
}

function handleTransferUpdate(value) {
  emitPropsPatch({ value: Array.isArray(value) ? value : [] })
}

async function renderBarcode() {
  if (props.componentKey !== 'barcode')
    return
  await nextTick()
  const svg = barcodeSvgRef.value
  if (!svg)
    return
  try {
    const JsBarcode = await loadBarcodeModule()
    JsBarcode(svg, barcodeValue.value, {
      format: props.propsData.format || 'CODE128',
      width: Number(props.propsData.barWidth || 2),
      height: Number(props.propsData.barHeight || 72),
      displayValue: props.propsData.showText !== false,
      lineColor: props.propsData.lineColor || '#0f172a',
      background: props.propsData.background || 'transparent',
      fontSize: Number(props.propsData.fontSize || 14),
      margin: Number(props.propsData.margin ?? 8),
      valid: (value) => {
        barcodeValid.value = value !== false
      },
    })
    barcodeValid.value = true
  }
  catch {
    barcodeValid.value = false
    svg.replaceChildren()
  }
}

async function loadTransferOptions() {
  const source = props.propsData.optionSource || {}
  const api = source.api || source.url || ''
  if (!api) {
    remoteTransferOptions.value = []
    transferError.value = '未配置远程接口'
    return
  }
  transferLoading.value = true
  transferError.value = ''
  try {
    const apiConfig = String(api).includes('@') ? api : `${source.method || 'get'}@${api}`
    const { method, url } = parseApiConfig(apiConfig)
    const params = safeJsonParseObject(source.paramsText || source.params, {})
    const response = await request({
      method,
      url: interpolateTemplate(url, props.dataContext || {}),
      params: method === 'get' ? params : undefined,
      data: method === 'get' ? undefined : params,
      needTip: false,
    })
    remoteTransferOptions.value = normalizeRemoteOptions(response?.data ?? response, source)
  }
  catch (error) {
    remoteTransferOptions.value = []
    transferError.value = `选项加载失败：${error?.message || '请检查接口配置'}`
  }
  finally {
    transferLoading.value = false
  }
}

async function loadBindingData() {
  const binding = props.propsData.dataBinding || {}
  const api = binding.api || binding.url || ''
  if (!api) {
    remoteBindingData.value = null
    bindingError.value = '未配置数据接口'
    return
  }
  bindingLoading.value = true
  bindingError.value = ''
  try {
    const apiConfig = String(api).includes('@') ? api : `${binding.method || 'get'}@${api}`
    const { method, url } = parseApiConfig(apiConfig)
    const params = normalizeBindingParams(binding.paramsText || binding.params || '{}')
    const response = await request({
      method,
      url,
      params: method === 'get' ? params : undefined,
      data: method === 'get' ? undefined : params,
      needTip: false,
    })
    remoteBindingData.value = response?.data ?? response
  }
  catch (error) {
    remoteBindingData.value = null
    bindingError.value = `数据加载失败：${error?.message || '请检查接口配置'}`
  }
  finally {
    bindingLoading.value = false
  }
}

function normalizeBindingParams(value = '{}') {
  const parsed = safeJsonParseObject(value, {})
  return Object.fromEntries(Object.entries(parsed).map(([key, item]) => [
    key,
    typeof item === 'string' ? interpolateTemplate(item, props.dataContext || {}) : item,
  ]))
}

function resolveBoundData() {
  const binding = props.propsData.dataBinding || {}
  if (binding.enabled !== true || binding.sourceType === 'static')
    return null
  const source = binding.sourceType === 'remote'
    ? remoteBindingData.value
    : props.dataContext
  if (!source)
    return null
  const dataPath = binding.sourceType === 'context'
    ? binding.contextPath
    : binding.dataPath
  if (!dataPath)
    return source
  const nested = getNestedValue(source, dataPath)
  return nested === undefined || nested === null ? source : nested
}

function resolveBoundValue(fallback, defaultField = 'value') {
  const data = boundData.value
  if (data === null || data === undefined)
    return fallback
  if (typeof data !== 'object')
    return data
  const binding = props.propsData.dataBinding || {}
  const field = binding.valueField || binding.totalField || defaultField
  return getNestedValue(data, field) ?? data.value ?? data.total ?? fallback
}

function resolveBoundContent(fallback, defaultField = 'content') {
  const data = boundData.value
  if (data === null || data === undefined)
    return fallback
  if (typeof data === 'string')
    return data
  if (Array.isArray(data))
    return data.map(item => typeof item === 'string' ? item : JSON.stringify(item)).join('\n')
  const binding = props.propsData.dataBinding || {}
  const field = binding[`${defaultField}Field`] || binding.contentField || defaultField
  return getNestedValue(data, field) ?? data[defaultField] ?? fallback
}

function normalizeDescriptionItems(data) {
  if (data === null || data === undefined)
    return null
  const binding = props.propsData.dataBinding || {}
  const labelField = binding.labelField || 'label'
  const valueField = binding.valueField || 'value'
  if (Array.isArray(data)) {
    return data.map((item, index) => typeof item === 'string'
      ? { label: `项目${index + 1}`, value: item }
      : {
          label: getNestedValue(item, labelField) ?? item?.label ?? item?.title ?? item?.name ?? `项目${index + 1}`,
          value: getNestedValue(item, valueField) ?? item?.value ?? item?.text ?? '',
        })
  }
  if (typeof data === 'object') {
    return Object.entries(data).map(([label, value]) => ({
      label,
      value: value === null || value === undefined ? '' : typeof value === 'object' ? JSON.stringify(value) : value,
    }))
  }
  return [{ label: binding.labelField || '值', value: data }]
}

function normalizeListItems(data) {
  const rows = normalizeRows(data)
  if (!rows)
    return null
  const binding = props.propsData.dataBinding || {}
  const titleField = binding.titleField || 'title'
  const descriptionField = binding.descriptionField || 'description'
  const metaField = binding.metaField || 'meta'
  return rows.map((item, index) => typeof item === 'string'
    ? { title: item, description: '', meta: '' }
    : {
        ...item,
        title: getNestedValue(item, titleField) ?? item?.title ?? item?.label ?? item?.name ?? `项目${index + 1}`,
        description: getNestedValue(item, descriptionField) ?? item?.description ?? item?.content ?? '',
        meta: getNestedValue(item, metaField) ?? item?.meta ?? item?.time ?? '',
      })
}

function normalizeBreadcrumbItems(data) {
  const rows = normalizeRows(data)
  if (!rows)
    return null
  const binding = props.propsData.dataBinding || {}
  const labelField = binding.labelField || 'label'
  return rows.map((item, index) => typeof item === 'string'
    ? { label: item }
    : {
        ...item,
        label: getNestedValue(item, labelField) ?? item?.label ?? item?.title ?? item?.name ?? `路径${index + 1}`,
      })
}

function normalizeMenuOptions(data) {
  const rows = normalizeRows(data)
  if (!rows)
    return null
  const binding = props.propsData.dataBinding || {}
  return rows.map((item, index) => normalizeMenuOption(item, index, binding))
}

function normalizeMenuOption(item, index, binding = {}) {
  if (typeof item === 'string')
    return { label: item, key: item }
  const labelField = binding.labelField || 'label'
  const keyField = binding.keyField || binding.valueField || 'key'
  const childrenField = binding.childrenField || 'children'
  const key = getNestedValue(item, keyField) ?? item?.key ?? item?.value ?? item?.id ?? `menu_${index + 1}`
  const children = getNestedValue(item, childrenField) ?? item?.children
  return {
    ...item,
    label: getNestedValue(item, labelField) ?? item?.label ?? item?.title ?? item?.name ?? String(key),
    key,
    children: Array.isArray(children) ? children.map((child, childIndex) => normalizeMenuOption(child, childIndex, binding)) : undefined,
  }
}

function normalizeRows(data) {
  if (data === null || data === undefined)
    return null
  if (Array.isArray(data))
    return data
  if (typeof data === 'object') {
    const rows = extractRows(data, props.propsData.dataBinding || {})
    if (Array.isArray(rows) && rows.length)
      return rows
    return Object.entries(data).map(([key, value]) => ({ label: key, value }))
  }
  return [data]
}

function normalizeOptionItems(options = []) {
  return (Array.isArray(options) ? options : []).map((item, index) => {
    if (typeof item === 'string')
      return { label: item, value: item }
    return {
      ...(item || {}),
      label: item?.label || item?.value || `选项${index + 1}`,
      value: item?.value || item?.label || `option${index + 1}`,
      disabled: item?.disabled === true,
    }
  })
}

function parseApiConfig(value = '') {
  const text = String(value || '')
  const [method, ...urlParts] = text.includes('@') ? text.split('@') : ['get', text]
  return {
    method: String(method || 'get').toLowerCase(),
    url: urlParts.join('@') || text,
  }
}

function normalizeRemoteOptions(data, source = {}) {
  const rows = extractRows(data, source)
  const valueField = source.valueField || 'value'
  const labelField = source.labelField || 'label'
  const disabledField = source.disabledField || 'disabled'
  return (Array.isArray(rows) ? rows : []).map((row, index) => {
    if (typeof row === 'string')
      return { label: row, value: row }
    const value = row?.[valueField] ?? row?.value ?? row?.key ?? row?.id ?? `option_${index + 1}`
    const label = row?.[labelField] ?? row?.label ?? row?.name ?? row?.title ?? String(value)
    return {
      ...row,
      label: String(label || value),
      value,
      disabled: row?.[disabledField] === true,
    }
  }).filter(option => option.value !== null && option.value !== undefined && option.value !== '')
}

function safeJsonParseArray(value = '', fallback = []) {
  try {
    const parsed = typeof value === 'string' ? JSON.parse(value || '[]') : value
    return Array.isArray(parsed) ? parsed : fallback
  }
  catch {
    return fallback
  }
}

function extractRows(data, source = {}) {
  if (Array.isArray(data))
    return data
  if (!data || typeof data !== 'object')
    return []
  const recordsField = source.recordsField || source.valuePath || ''
  if (recordsField) {
    const nested = getNestedValue(data, recordsField)
    if (Array.isArray(nested))
      return nested
  }
  for (const key of ['records', 'list', 'rows', 'items']) {
    if (Array.isArray(data[key]))
      return data[key]
  }
  if (Array.isArray(data.data))
    return data.data
  if (data.data && typeof data.data === 'object')
    return extractRows(data.data, source)
  return []
}

function getNestedValue(source, path) {
  return String(path || '')
    .split('.')
    .filter(Boolean)
    .reduce((value, key) => value?.[key], source)
}

function toOptionalNumber(value) {
  const number = Number(value)
  return Number.isFinite(number) && value !== '' && value !== undefined && value !== null ? number : undefined
}

function sanitizeHtmlTagName(value = 'section') {
  const tag = String(value || 'section').trim().toLowerCase()
  const allowed = new Set(['div', 'section', 'article', 'aside', 'header', 'footer', 'main', 'span', 'p', 'strong', 'em', 'small', 'label'])
  return allowed.has(tag) ? tag : 'section'
}

function sanitizeAttributes(attrs = {}) {
  const allowed = ['class', 'title', 'aria-label', 'data-key']
  return Object.fromEntries(Object.entries(attrs).filter(([key]) => allowed.includes(key)))
}

function stripVueTemplate(value = '') {
  const text = String(value || '')
  const match = text.match(/<template[^>]*>([\s\S]*)<\/template>/i)
  return match?.[1] || text
}

function sanitizeCss(value = '') {
  return String(value || '')
    .replace(/<\/style>/gi, '')
    .replace(/@import[^;]+;/gi, '')
    .replace(/url\([^)]*javascript:[^)]*\)/gi, '')
}
</script>

<style scoped>
.page-widget-renderer {
  width: 100%;
  max-width: 100%;
  height: 100%;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

.rich-editor-shell,
.markdown-shell,
.vue-widget-shell,
.naive-widget-shell {
  display: grid;
  gap: 10px;
  width: 100%;
  max-width: 100%;
  height: 100%;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

.naive-widget-shell {
  align-content: start;
  padding: 0;
  border: 0;
  border-radius: 0;
  background: transparent;
}

.rich-editor-head,
.vue-widget-toolbar,
.widget-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.rich-editor-head strong,
.vue-widget-toolbar strong,
.widget-title-row strong,
.widget-title {
  color: #0f172a;
  font-size: 13px;
  font-weight: 800;
}

.rich-editor-head span,
.vue-widget-toolbar em,
.widget-title-row span {
  color: #64748b;
  font-size: 11px;
  font-style: normal;
}

.wang-editor-wrap {
  width: 100%;
  max-width: 100%;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  border-style: solid;
  background: transparent;
}

.wang-toolbar {
  border-bottom: 1px solid #e2e8f0;
}

.wang-editor {
  width: 100%;
  max-width: 100%;
  min-width: 0;
  min-height: inherit;
}

.wang-editor :deep(.w-e-text-container) {
  width: 100%;
  max-width: 100%;
  min-width: 0;
  min-height: inherit;
}

.wang-editor-wrap :deep(.w-e-toolbar),
.wang-editor-wrap :deep(.w-e-text-container) {
  background-color: transparent;
}

.wang-editor :deep(.w-e-scroll),
.wang-editor :deep(.w-e-text),
.wang-editor :deep(.w-e-text-placeholder) {
  max-width: 100%;
  min-width: 0;
  overflow-wrap: anywhere;
}

.markdown-editor {
  width: 100%;
  max-width: 100%;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  border: 0;
  border-radius: 0;
}

.markdown-editor :deep(.v-md-editor),
.markdown-editor :deep(.v-md-editor__main),
.markdown-editor :deep(.v-md-editor__left-area),
.markdown-editor :deep(.v-md-editor__right-area),
.markdown-editor :deep(.v-md-textarea-editor),
.markdown-editor :deep(.vuepress-markdown-body) {
  max-width: 100%;
  min-width: 0;
}

.markdown-editor :deep(.v-md-editor),
.markdown-editor :deep(.v-md-editor__main),
.markdown-editor :deep(.v-md-editor__left-area),
.markdown-editor :deep(.v-md-editor__right-area),
.markdown-editor :deep(.v-md-textarea-editor),
.markdown-editor :deep(.vuepress-markdown-body) {
  background-color: transparent;
}

.markdown-editor :deep(pre),
.markdown-editor :deep(code),
.markdown-editor :deep(table) {
  max-width: 100%;
  overflow: auto;
}

.transfer-widget-shell {
  display: grid;
  gap: 8px;
  height: 100%;
  min-height: 0;
}

.transfer-widget-shell :deep(.n-transfer) {
  min-height: 0;
}

.widget-error {
  color: #dc2626;
  font-size: 12px;
}

.widget-binding-state {
  align-self: start;
  width: fit-content;
  max-width: 100%;
  border: 1px solid #bfdbfe;
  border-radius: 999px;
  background: #eff6ff;
  padding: 2px 8px;
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 600;
}

.barcode-widget,
.qrcode-widget {
  display: grid;
  place-items: center;
  align-content: center;
  gap: 8px;
  width: 100%;
  max-width: 100%;
  min-width: 0;
  height: 100%;
  min-height: 96px;
  overflow: auto;
}

.barcode-svg {
  display: block;
  max-width: 100%;
  height: auto;
}

.qrcode-widget small {
  max-width: 100%;
  overflow: hidden;
  color: #64748b;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.calendar-widget,
.code-widget,
.list-widget,
.log-widget,
.menu-widget {
  overflow: auto;
}

.calendar-widget :deep(.n-calendar) {
  min-width: 640px;
}

.code-widget :deep(pre),
.code-widget :deep(code),
.log-widget :deep(.n-log) {
  max-width: 100%;
  min-width: 0;
}

.countdown-widget,
.number-animation-widget,
.pagination-widget,
.breadcrumb-widget {
  place-items: center;
  align-content: center;
  min-height: 96px;
}

.countdown-widget span,
.number-animation-widget span {
  color: #64748b;
  font-size: 12px;
}

.countdown-widget :deep(.n-countdown),
.number-animation-widget strong {
  color: #0f172a;
  font-size: 28px;
  font-weight: 800;
  line-height: 1.1;
}

.descriptions-widget {
  overflow: auto;
}

.announcement-widget {
  width: 100%;
  min-width: 0;
}

.split-widget {
  width: 100%;
  height: 100%;
  min-height: 180px;
  border: 0;
  border-radius: 0;
  overflow: hidden;
  background: transparent;
}

.split-pane {
  display: grid;
  align-content: start;
  gap: 8px;
  height: 100%;
  padding: 14px;
  min-width: 0;
  overflow: auto;
}

.split-pane strong {
  color: #0f172a;
  font-size: 13px;
  font-weight: 800;
}

.split-pane p {
  margin: 0;
  color: #64748b;
  line-height: 1.7;
}

.watermark-widget {
  width: 100%;
  height: 100%;
  min-height: 120px;
  border-radius: 0;
  overflow: hidden;
}

.watermark-inner {
  display: grid;
  place-items: center;
  align-content: center;
  gap: 6px;
  min-height: 100%;
  padding: 18px;
  border: 0;
  border-radius: 0;
  color: #1e3a8a;
  background: transparent;
}

.code-preview {
  min-height: 0;
  max-height: 100%;
  margin: 0;
  overflow: auto;
  padding: 12px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #f8fafc;
  color: #334155;
  font-size: 12px;
  line-height: 1.7;
}

.html-widget {
  display: block;
  min-height: 64px;
  padding: 12px;
  border: 0;
  border-radius: 0;
  background: transparent;
  color: #334155;
  line-height: 1.7;
}

.html-meta {
  margin-top: 6px;
  color: #64748b;
  font-size: 11px;
}

.vue-widget-shell {
  padding: 12px;
  border: 0;
  border-radius: 0;
  background: transparent;
}

.vue-badge {
  display: grid;
  place-items: center;
  width: 32px;
  height: 24px;
  border-radius: 7px;
  background: #16a34a;
  color: #fff;
  font-size: 12px;
  font-weight: 900;
}

.vue-code-grid {
  display: grid;
  gap: 8px;
}

.vue-safe-preview,
.vue-live-preview {
  overflow: auto;
  min-height: 96px;
  border-radius: 10px;
}

.vue-live-preview {
  padding: 12px;
  border: 1px solid #bbf7d0;
  background: #f0fdf4;
}

.vue-template-preview :deep(.custom-widget) {
  padding: 16px;
  border-radius: 12px;
  background: #eff6ff;
  color: #1e3a8a;
}

.vue-props-preview {
  display: flex;
  gap: 8px;
  align-items: center;
  min-width: 0;
  color: #64748b;
  font-size: 11px;
}

.vue-props-preview code {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ── 媒体挂件 ──────────────────────────── */
.widget-placeholder {
  padding: 12px 16px;
  border-radius: 6px;
  background: #f1f5f9;
  color: #64748b;
  font-size: 12px;
  text-align: center;
}

.audio-player-element,
.video-player-element {
  display: block;
  max-width: 100%;
  border-radius: 6px;
}

.avatar-widget {
  align-content: center;
  justify-items: center;
}

.iframe-element {
  display: block;
  width: 100%;
  border: none;
  border-radius: 6px;
}
</style>
