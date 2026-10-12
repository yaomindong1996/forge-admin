<template>
  <div class="page-design-settings">
    <aside class="page-design-settings-nav">
      <button
        v-for="item in sections"
        :key="item.key"
        type="button"
        :class="{ active: activeSection === item.key }"
        @click="selectSection(item.key)"
      >
        <n-icon><component :is="item.icon" /></n-icon>
        <span>{{ item.label }}</span>
      </button>
    </aside>
    <main class="page-design-settings-content">
      <section v-if="activeSection === 'basic'" class="page-design-settings-card">
        <header>
          <h2>基础信息</h2>
          <p>这些信息会显示在应用导航和工作台页面树上。</p>
        </header>
        <n-form label-placement="top">
          <n-form-item label="页面名称" required>
            <n-input
              :value="node.title"
              maxlength="40"
              show-count
              placeholder="请输入页面名称"
              @update:value="patch({ title: $event })"
            />
          </n-form-item>
          <n-form-item label="页面图标">
            <IconSelector :model-value="node.icon || ''" @update:model-value="patch({ icon: $event })" />
          </n-form-item>
        </n-form>
      </section>

      <section v-else-if="activeSection === 'display'" class="page-design-settings-card">
        <header>
          <h2>显示设置</h2>
          <p>控制这个页面是否出现在应用导航中。</p>
        </header>
        <n-form label-placement="top">
          <n-form-item label="在导航中显示">
            <div class="page-design-settings-switch">
              <n-switch :value="navigationVisible" @update:value="patch({ navigationVisible: $event })" />
              <span>{{ navigationVisible ? '运行时导航会展示此页面' : '运行时导航会隐藏此页面' }}</span>
            </div>
          </n-form-item>
        </n-form>
      </section>

      <section v-else-if="activeSection === 'params'" class="page-design-settings-card">
        <header>
          <h2>页面入参</h2>
          <p>声明本页可从路由接收的参数。其它页面跳转时可传入；本页列表/表单可通过「路由参数」取值使用。</p>
        </header>
        <div class="page-params-editor">
          <div
            v-for="(param, index) in pageParams"
            :key="index"
            class="page-params-row"
          >
            <n-input
              size="small"
              :value="param.name"
              placeholder="参数名，如 orderId"
              @update:value="updatePageParam(index, { name: normalizeParamName($event) })"
            />
            <n-input
              size="small"
              :value="param.label"
              placeholder="显示名（可选）"
              @update:value="updatePageParam(index, { label: $event })"
            />
            <n-input
              size="small"
              :value="param.defaultValue"
              placeholder="默认值（可选）"
              @update:value="updatePageParam(index, { defaultValue: $event })"
            />
            <n-button size="tiny" quaternary type="error" @click="removePageParam(index)">
              删除
            </n-button>
          </div>
          <n-button size="small" dashed block @click="addPageParam">
            + 添加入参
          </n-button>
          <p class="page-params-hint">
            跳转时在事件「传参」里选「路由参数」或静态值写入这些名字；本页 AiCrudPage 会自动把同名路由参数并入查询条件 / 表单默认值。
          </p>
        </div>
      </section>

      <section v-else-if="activeSection === 'audit'" class="page-design-settings-card">
        <header>
          <h2>数据变更审计</h2>
          <p>统一控制该业务对象是否记录数据变化、提交时是否要求说明原因，以及详情中是否显示历史。</p>
        </header>
        <DataAuditPolicyPanel v-if="boundObjectId" hide-title :object-id="boundObjectId" />
        <n-empty v-else description="当前页面未绑定数据对象，无法启用审计" />
      </section>

      <section v-else-if="activeSection === 'printing'" class="page-design-settings-card is-print">
        <header>
          <h2>打印模板</h2>
          <p>只作用于当前页面。勾选列表或详情后，重新发布应用即可给业务用户使用。</p>
        </header>
        <div class="page-print-watermark">
          <div class="watermark-head">
            <div>
              <h3>页面水印</h3>
              <p>本页模板默认带这组水印。某张模板不想打，到设计器纸张里关掉。</p>
            </div>
            <n-switch size="small" :value="printWatermark.enabled" @update:value="patchPrintWatermark({ enabled: $event })" />
          </div>
          <div v-if="printWatermark.enabled" class="watermark-body">
            <n-input
              size="small"
              :value="printWatermark.text"
              maxlength="50"
              placeholder="自定义文字，例如内部资料"
              @update:value="patchPrintWatermark({ text: $event })"
            />
            <div class="watermark-row">
              <n-checkbox size="small" :checked="printWatermark.showUsername" @update:checked="patchPrintWatermark({ showUsername: $event })">
                用户名
              </n-checkbox>
              <n-checkbox size="small" :checked="printWatermark.showTime" @update:checked="patchPrintWatermark({ showTime: $event })">
                当前时间
              </n-checkbox>
              <n-color-picker
                class="watermark-swatch"
                size="small"
                :value="printWatermark.color"
                :show-alpha="false"
                :modes="['hex']"
                @update:value="patchWatermarkColor"
              />
              <n-select
                class="watermark-size"
                size="small"
                :value="printWatermark.fontSizePt"
                :options="printFontSizeOptions(printWatermark.fontSizePt)"
                @update:value="patchPrintWatermark({ fontSizePt: $event })"
              />
              <n-select
                class="watermark-density"
                size="small"
                :value="printWatermark.density"
                :options="watermarkDensityOptions"
                @update:value="patchPrintWatermark({ density: $event })"
              />
            </div>
          </div>
        </div>
        <ApplicationPrintPanel
          v-if="application"
          :application="application"
          :application-objects="objects"
          :page-id="node.id"
        />
        <n-empty v-else description="当前页面还没有可配置打印的应用上下文" />
      </section>

      <section v-else class="page-design-settings-card">
        <header>
          <h2>页面信息</h2>
          <p>页面形态和绑定对象在创建时确定，可在这里核对。</p>
        </header>
        <dl class="page-design-settings-meta">
          <div>
            <dt>页面形态</dt>
            <dd>{{ pageShapeLabel }}</dd>
          </div>
          <div>
            <dt>页面类型</dt>
            <dd>{{ pageTypeLabel }}</dd>
          </div>
          <div>
            <dt>数据对象</dt>
            <dd>{{ objectLabel }}</dd>
          </div>
          <div>
            <dt>页面编码</dt>
            <dd>{{ node.id }}</dd>
          </div>
        </dl>
        <div v-if="layoutPolluted" class="page-design-settings-restore">
          <n-alert type="warning" :bordered="false" title="布局已被自由布局组件污染">
            可一键清除多余组件，恢复为列表/表单标准布局（保留数据对象与表单资产）。
          </n-alert>
          <n-button type="warning" :loading="restoring" @click="emit('restore-layout')">
            恢复列表/表单布局
          </n-button>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { ColorPaletteOutline, EyeOutline, InformationCircleOutline, LinkOutline, PrintOutline, ShieldCheckmarkOutline } from '@vicons/ionicons5'
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import DataAuditPolicyPanel from '@/components/data-audit/DataAuditPolicyPanel.vue'
import IconSelector from '@/components/IconSelector.vue'
import { DEFAULT_WATERMARK_STYLE, WATERMARK_DENSITY_OPTIONS } from '@/components/print/designer/paperPanelModel'
import { printFontSizeOptions } from '@/components/print/designer/printFonts'
import { normalizePrintPageWatermark } from '@/components/print/management/pagePrintWatermark'
import { toPrintColor } from '@/components/print/protocol/printColor'
import ApplicationPrintPanel from '../../application-workspace/ApplicationPrintPanel.vue'
import { inAppPageTypes } from '../../in-app-builder/in-app-builder-schema'
import { PAGE_SHAPE_TYPES } from '../../in-app-builder/page-shape-design'

const props = defineProps({
  node: {
    type: Object,
    required: true,
  },
  application: {
    type: Object,
    default: null,
  },
  objects: {
    type: Array,
    default: () => [],
  },
  layoutPolluted: {
    type: Boolean,
    default: false,
  },
  restoring: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['update', 'restore-layout'])
const route = useRoute()
const router = useRouter()

const PAGE_SETTINGS_SECTIONS = new Set(['basic', 'display', 'params', 'audit', 'printing', 'info'])

function resolvePageSettingsSection(value) {
  const normalized = String(Array.isArray(value) ? value[0] : value || '').trim()
  return PAGE_SETTINGS_SECTIONS.has(normalized) ? normalized : 'basic'
}

const activeSection = ref(resolvePageSettingsSection(route.query.settingsSection))
const sections = [
  { key: 'basic', label: '基础信息', icon: ColorPaletteOutline },
  { key: 'display', label: '显示设置', icon: EyeOutline },
  { key: 'params', label: '页面入参', icon: LinkOutline },
  { key: 'audit', label: '数据审计', icon: ShieldCheckmarkOutline },
  { key: 'printing', label: '打印模板', icon: PrintOutline },
  { key: 'info', label: '页面信息', icon: InformationCircleOutline },
]

function selectSection(section) {
  const next = resolvePageSettingsSection(section)
  activeSection.value = next
  const settingsSection = next === 'basic' ? undefined : next
  if (route.query.settingsSection === settingsSection)
    return
  router.replace({
    query: {
      ...route.query,
      settingsSection,
    },
  })
}

watch(() => route.query.settingsSection, (section) => {
  const next = resolvePageSettingsSection(section)
  if (activeSection.value !== next)
    activeSection.value = next
})

const navigationVisible = computed(() => (props.node.navigationVisible ?? props.node.settings?.navigationVisible) !== false)
const printWatermark = computed(() => normalizePrintPageWatermark(props.node.printWatermark ?? props.node.settings?.printWatermark))
const pageParams = computed(() => normalizePageParams(props.node.pageParams ?? props.node.settings?.pageParams))

function normalizePageParams(value) {
  if (!Array.isArray(value))
    return []
  return value
    .filter(item => item && typeof item === 'object')
    .map(item => ({
      name: String(item.name || '').trim(),
      label: String(item.label || '').trim(),
      defaultValue: item.defaultValue == null ? '' : String(item.defaultValue),
    }))
}

function normalizeParamName(value = '') {
  return String(value || '').trim().replace(/\s+/g, '_')
}

function addPageParam() {
  patch({
    pageParams: [
      ...pageParams.value,
      { name: '', label: '', defaultValue: '' },
    ],
  })
}

function updatePageParam(index, partial = {}) {
  const next = pageParams.value.map((item, idx) => (idx === index ? { ...item, ...partial } : item))
  patch({ pageParams: next })
}

function removePageParam(index) {
  patch({ pageParams: pageParams.value.filter((_, idx) => idx !== index) })
}

const pageShapeLabel = computed(() => {
  const value = props.node.pageTemplate || props.node.objectRef?.pageMode || ''
  return PAGE_SHAPE_TYPES.find(item => item.value === value)?.label
    || PAGE_SHAPE_TYPES.find(item => item.value === mapPageModeToShape(value))?.label
    || '未指定'
})

const pageTypeLabel = computed(() => {
  return inAppPageTypes.find(item => item.value === props.node.pageType)?.label || props.node.pageType || '未指定'
})

const objectLabel = computed(() => {
  const objectRef = props.node.objectRef
  if (!objectRef?.objectName && !objectRef?.objectCode)
    return '未绑定数据对象'
  return [objectRef.objectName, objectRef.objectCode].filter(Boolean).join(' · ')
})

const boundObjectId = computed(() => {
  const objectRef = props.node.objectRef || {}
  return objectRef.objectId || objectRef.id || ''
})

function mapPageModeToShape(value) {
  if (value === 'crud')
    return 'list-form'
  if (value === 'tree-crud' || value === 'tree_table')
    return 'tree-table'
  if (value === 'tree_list')
    return 'tree-list'
  return value
}

function patch(partial) {
  emit('update', partial)
}

function patchPrintWatermark(partial) {
  patch({ printWatermark: { ...printWatermark.value, ...partial } })
}

const watermarkDensityOptions = WATERMARK_DENSITY_OPTIONS.map(item => ({ label: item.label, value: item.value }))

function patchWatermarkColor(value) {
  const color = toPrintColor(value)
  patchPrintWatermark({ color: color && color !== 'transparent' ? color : DEFAULT_WATERMARK_STYLE.color })
}
</script>

<style scoped>
.page-design-settings {
  display: grid;
  grid-template-columns: 188px minmax(0, 1fr);
  gap: 20px;
  width: min(1120px, 100%);
  margin: 0 auto;
}

.page-design-settings-nav {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-self: start;
  padding: 8px;
  border: 1px solid var(--border-light);
  border-radius: 12px;
  background: var(--bg-primary);
}

.page-design-settings-nav button {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 10px 12px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: var(--text-secondary);
  font-size: 13px;
  text-align: left;
  cursor: pointer;
}

.page-design-settings-nav button:hover {
  background: var(--bg-tertiary);
  color: var(--text-primary);
}

.page-design-settings-nav button.active {
  background: rgba(22, 93, 255, 0.08);
  color: #165dff;
  font-weight: 600;
}

.page-design-settings-card {
  padding: 24px;
  border: 1px solid var(--border-light);
  border-radius: 12px;
  background: var(--bg-primary);
  box-shadow: 0 1px 3px rgb(31 35 41 / 6%);
}

.page-design-settings-card.is-print {
  padding: 20px;
}

.page-design-settings-card.is-print :deep(.print-template-list) {
  box-shadow: none;
}

.page-print-watermark {
  margin-bottom: 16px;
  padding: 10px 12px;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: var(--bg-secondary);
}

.watermark-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.watermark-head h3 {
  margin: 0;
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 600;
  line-height: 20px;
}

.watermark-head p,
.page-print-watermark .watermark-head p {
  margin: 2px 0 0;
  color: var(--text-tertiary);
  font-size: 12px;
  line-height: 18px;
}

.watermark-body {
  display: grid;
  gap: 8px;
  margin-top: 10px;
}

.watermark-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.watermark-swatch {
  width: 128px;
}

.watermark-size,
.watermark-density {
  width: 112px;
}

.page-design-settings-card header {
  margin-bottom: 20px;
}

.page-design-settings-card h2 {
  margin: 0;
  color: var(--text-primary);
  font-size: 16px;
  font-weight: 650;
  line-height: 24px;
}

.page-design-settings-card p {
  margin: 6px 0 0;
  color: var(--text-tertiary);
  font-size: 12px;
  line-height: 18px;
}

.page-design-settings-switch {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--text-secondary);
  font-size: 13px;
}

.page-params-editor {
  display: grid;
  gap: 10px;
}

.page-params-row {
  display: grid;
  grid-template-columns: minmax(120px, 1fr) minmax(120px, 1fr) minmax(120px, 1fr) auto;
  gap: 8px;
  align-items: center;
}

.page-params-hint {
  margin: 0 !important;
  color: var(--text-tertiary);
  font-size: 12px;
  line-height: 18px;
}

.page-design-settings-meta {
  display: grid;
  gap: 16px;
  margin: 0;
}

.page-design-settings-meta div {
  display: grid;
  gap: 6px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f2f3f5;
}

.page-design-settings-meta div:last-child {
  padding-bottom: 0;
  border-bottom: 0;
}

.page-design-settings-meta dt {
  color: var(--text-tertiary);
  font-size: 12px;
}

.page-design-settings-meta dd {
  margin: 0;
  color: var(--text-primary);
  font-size: 14px;
  word-break: break-all;
}

.page-design-settings-restore {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 16px;
}

@media (max-width: 768px) {
  .page-design-settings {
    grid-template-columns: 1fr;
  }

  .page-design-settings-nav {
    flex-direction: row;
    overflow-x: auto;
  }
}
</style>
