<script setup>
import {
  BarcodeOutline,
  CheckmarkCircleOutline,
  CodeSlashOutline,
  DocumentLockOutline,
  ExtensionPuzzleOutline,
  DocumentTextOutline,
  EllipseOutline,
  GridOutline,
  ImageOutline,
  LayersOutline,
  ListOutline,
  QrCodeOutline,
  ReaderOutline,
  RemoveOutline,
  ReturnDownForwardOutline,
  SearchOutline,
  ShieldCheckmarkOutline,
  SquareOutline,
  TextOutline,
} from '@vicons/ionicons5'
import { NDropdown, NIcon, NInput } from 'naive-ui'
import { computed, h, nextTick, ref } from 'vue'
import ContentPackageImportDialog from '@/components/common/ContentPackageImportDialog.vue'
import InstalledContentAssetPicker from '@/components/common/InstalledContentAssetPicker.vue'
import marketStarUrl from '@/assets/print/component-market-star.png'
import { usePrintDesignerStore } from '@/stores/print/printDesignerStore'
import { officialMarketUrl } from '@/utils/officialMarket'
import { installContentPackageFromFile } from '@/utils/contentInstalledAsset'
import { findSurface, newPrintId } from './commands'
import { addDetailTable as insertDetailTable, addElement, addSection, elementCatalog, startItemDrag } from './elementCatalog'
import { insertRegisteredPrintComponent, printComponentRegistry } from './printComponentRegistry'
import PrintTableIcon from './PrintTableIcon.vue'
import { renewStaticTableIds } from './staticTable'
import './businessPrintComponents'

const emit = defineEmits(['pick-print-library', 'import-print-package'])

const store = usePrintDesignerStore()
const keyword = ref('')
const activeCategory = ref('basic')
const contentRef = ref(null)
const importOpen = ref(false)
const printImportOpen = ref(false)
const libraryOpen = ref(false)
const importing = ref(false)
const printImporting = ref(false)
const componentMarketUrl = computed(() => officialMarketUrl('print-component'))
const printMarketUrl = computed(() => officialMarketUrl('print'))

function menuIcon(cls) {
  return () => h('i', { class: cls, style: 'font-size:14px' })
}

const printTemplateMenu = computed(() => [
  {
    label: '探索模板市场',
    key: 'market',
    icon: menuIcon('i-lucide:store'),
    disabled: !printMarketUrl.value,
  },
  {
    label: '从我的整页模板选用',
    key: 'library',
    icon: menuIcon('i-lucide:folder-open'),
  },
  {
    label: '安装整页模板',
    key: 'install',
    icon: menuIcon('i-lucide:download'),
  },
])

const canvasComponentMenu = computed(() => [
  {
    label: '探索组件市场',
    key: 'market',
    icon: menuIcon('i-lucide:store'),
    disabled: !componentMarketUrl.value,
  },
  {
    label: '从我的组件选用',
    key: 'library',
    icon: menuIcon('i-lucide:puzzle'),
  },
  {
    label: '安装画布组件',
    key: 'install',
    icon: menuIcon('i-lucide:download'),
  },
])

function onPrintTemplateMenu(key) {
  if (key === 'market' && printMarketUrl.value)
    window.open(printMarketUrl.value, '_blank', 'noopener,noreferrer')
  else if (key === 'library')
    emit('pick-print-library')
  else if (key === 'install')
    printImportOpen.value = true
}

function onCanvasComponentMenu(key) {
  if (key === 'market' && componentMarketUrl.value)
    window.open(componentMarketUrl.value, '_blank', 'noopener,noreferrer')
  else if (key === 'library')
    libraryOpen.value = true
  else if (key === 'install')
    importOpen.value = true
}

const elementMeta = {
  title: { desc: '展示标题文本', tone: 'blue', glyph: 'Aa' },
  text: { desc: '固定文案内容', tone: 'slate' },
  'field-text': { desc: '绑定数据字段', tone: 'cyan' },
  'long-text': { desc: '多行自动换行', tone: 'indigo' },
  image: { desc: '插入图片资源', tone: 'sky' },
  html: { desc: '自定义 HTML 片段', tone: 'violet' },
  'line-horizontal': { desc: '水平分隔线', tone: 'amber' },
  'line-vertical': { desc: '垂直分隔线', tone: 'amber' },
  rectangle: { desc: '矩形边框或底色', tone: 'emerald' },
  ellipse: { desc: '椭圆形状', tone: 'teal' },
  circle: { desc: '正圆形状', tone: 'green' },
  barcode: { desc: '一维条形码', tone: 'orange' },
  qrcode: { desc: '二维码标识', tone: 'rose' },
  'page-number': { desc: '当前页 / 总页', tone: 'blue' },
  'static-table': { desc: '空白可编辑表格', tone: 'indigo' },
  descriptions: { desc: '字段详情排版', tone: 'purple' },
  'detail-table': { desc: '集合字段循环行', tone: 'blue' },
}

const elementIcons = {
  BARCODE: BarcodeOutline,
  IMAGE: ImageOutline,
  HTML: CodeSlashOutline,
  LINE: RemoveOutline,
  PAGE_NUMBER: DocumentTextOutline,
  QRCODE: QrCodeOutline,
  RECTANGLE: SquareOutline,
  ELLIPSE: EllipseOutline,
  TEXT: TextOutline,
  STATIC_TABLE: PrintTableIcon,
  DATA_TABLE: PrintTableIcon,
  DESCRIPTIONS: ReaderOutline,
}

const flowSections = [
  { type: 'FIXED', label: '自由画布层', icon: LayersOutline, tip: '可自由拖放组件的图层', tone: 'blue' },
  { type: 'TEXT', label: '流式长文', icon: ListOutline, tip: '随内容自动换页', tone: 'indigo' },
  { type: 'PAGE_BREAK', label: '手动分页', icon: ReturnDownForwardOutline, tip: '强制从下一页开始', tone: 'amber' },
]

const businessIcons = {
  approval: CheckmarkCircleOutline,
  contract: DocumentLockOutline,
  signature: ShieldCheckmarkOutline,
}

const businessMeta = {
  'forge.approval-status': { desc: '审批结果展示', tone: 'emerald' },
  'forge.signature-position': { desc: '签章 / 签名位', tone: 'blue' },
  'forge.contract-terms': { desc: '条款流式长文', tone: 'violet' },
}

const categories = [
  { key: 'basic', label: '基础组件', icon: GridOutline },
  { key: 'flow', label: '流式排版', icon: LayersOutline },
  { key: 'business', label: '业务组件', icon: ExtensionPuzzleOutline },
]

const businessComponents = computed(() => printComponentRegistry.list())

const basicItems = computed(() => {
  const items = elementCatalog.map(item => ({
    ...item,
    desc: elementMeta[item.key]?.desc || '拖到画布使用',
    tone: elementMeta[item.key]?.tone || 'slate',
    glyph: elementMeta[item.key]?.glyph,
  }))
  items.push({
    key: 'detail-table',
    type: 'DATA_TABLE',
    label: '明细表格',
    desc: elementMeta['detail-table'].desc,
    tone: elementMeta['detail-table'].tone,
    detailTable: true,
  })
  return items
})

const query = computed(() => keyword.value.trim().toLowerCase())

function matchQuery(label, desc) {
  if (!query.value)
    return true
  return `${label} ${desc || ''}`.toLowerCase().includes(query.value)
}

const filteredBasic = computed(() => basicItems.value.filter(item => matchQuery(item.label, item.desc)))
const filteredFlow = computed(() => flowSections.filter(item => matchQuery(item.label, item.tip)))
const filteredBusiness = computed(() => businessComponents.value.filter((item) => {
  const desc = businessMeta[item.key]?.desc || ''
  return matchQuery(item.label, desc)
}))

const visibleSections = computed(() => {
  const sections = []
  if (filteredBasic.value.length) {
    sections.push({
      key: 'basic',
      title: '基础组件',
      tip: '拖到中间画布即可自由摆放',
      items: filteredBasic.value,
      kind: 'basic',
    })
  }
  if (filteredFlow.value.length) {
    sections.push({
      key: 'flow',
      title: '流式排版',
      tip: '画布层、长文与强制分页',
      items: filteredFlow.value,
      kind: 'flow',
    })
  }
  if (filteredBusiness.value.length || !query.value) {
    sections.push({
      key: 'business',
      title: '业务组件',
      tip: '常用业务片段，可从市场扩展',
      items: filteredBusiness.value,
      kind: 'business',
    })
  }
  return sections
})

async function selectCategory(key) {
  activeCategory.value = key
  await nextTick()
  const root = contentRef.value
  const target = root?.querySelector(`[data-section="${key}"]`)
  target?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function addBusiness(key) {
  store.ensureFreeCanvas()
  insertRegisteredPrintComponent(store, key)
}

function addFlowSection(kind) {
  try {
    addSection(store, kind)
  }
  catch (error) {
    store.error = error?.message || '无法添加该区块'
  }
}

function addDetail() {
  try {
    insertDetailTable(store)
  }
  catch (error) {
    store.error = error?.message || '无法添加明细表格'
  }
}

function onBasicClick(item) {
  if (item.detailTable) {
    addDetail()
    return
  }
  addElement(store, item.type, undefined, undefined, item.preset)
}

function onBasicDrag(event, item) {
  if (item.detailTable) {
    startItemDrag(event, { detailTable: true })
    return
  }
  startItemDrag(event, { type: item.type, preset: item.preset })
}

function insertComponentPayload(payload, successMessage) {
  const elements = payload?.elements || (payload?.element ? [payload.element] : [])
  if (!elements.length)
    throw new Error('打印组件包里没有可插入的元素')
  store.ensureFreeCanvas()
  store.execute((document) => {
    const surface = findSurface(document, store.surfaceId)
    if (!surface?.elements)
      throw new Error('当前画布不能插入组件')
    for (const raw of elements)
      surface.elements.push(renewStaticTableIds({ ...raw, id: newPrintId() }, newPrintId))
  })
  window.$message?.success(successMessage)
}

async function importPrintAsset({ file, kind }) {
  importing.value = true
  try {
    if (kind !== 'print-component')
      throw new Error('请安装打印组件 JSON；整页模板请用下方「整页模板 → 安装」')
    const { pack } = await installContentPackageFromFile({ file, kind: 'print-component' })
    insertComponentPayload(pack.payload, '已安装到我的组件，并插入当前画布')
    importOpen.value = false
  }
  catch (error) {
    window.$message?.error(error.message || '安装失败')
  }
  finally {
    importing.value = false
  }
}

function applyLibraryComponent({ pack }) {
  try {
    insertComponentPayload(pack.payload, `已插入「${pack.name || '打印组件'}」`)
  }
  catch (error) {
    window.$message?.error(error.message || '选用失败')
  }
}

async function onPrintPackageConfirm({ file }) {
  printImporting.value = true
  try {
    emit('import-print-package', { file })
    printImportOpen.value = false
  }
  finally {
    printImporting.value = false
  }
}
</script>

<template>
  <!-- 组件库：左侧分类 + 右侧卡片列表 -->
  <section class="print-palette">
    <nav class="palette-rail" aria-label="组件分类">
      <button
        v-for="cat in categories"
        :key="cat.key"
        type="button"
        class="rail-item"
        :class="{ active: activeCategory === cat.key }"
        @click="selectCategory(cat.key)"
      >
        <NIcon :component="cat.icon" :size="16" />
        <span>{{ cat.label }}</span>
      </button>
    </nav>

    <div class="palette-main">
      <div class="palette-search">
        <NInput v-model:value="keyword" size="small" clearable placeholder="搜索组件" aria-label="搜索组件">
          <template #prefix>
            <NIcon :component="SearchOutline" :size="14" />
          </template>
        </NInput>
      </div>

      <div ref="contentRef" class="palette-scroll">
        <template v-if="visibleSections.length">
          <section
            v-for="section in visibleSections"
            :key="section.key"
            class="palette-section"
            :data-section="section.key"
          >
            <header class="section-head">
              <h3>{{ section.title }}</h3>
              <p>{{ section.tip }}</p>
            </header>

            <div v-if="section.kind === 'basic'" class="card-grid">
              <button
                v-for="item in section.items"
                :key="item.key"
                type="button"
                class="comp-card"
                draggable="true"
                :title="`拖入或点击添加${item.label}`"
                :aria-label="item.label"
                @dragstart="onBasicDrag($event, item)"
                @click="onBasicClick(item)"
              >
                <span class="comp-icon" :class="`tone-${item.tone}`">
                  <span v-if="item.glyph" class="comp-glyph">{{ item.glyph }}</span>
                  <NIcon
                    v-else
                    :component="elementIcons[item.type]"
                    :size="16"
                    :class="{ 'vertical-line-icon': item.preset === 'VERTICAL' }"
                  />
                </span>
                <span class="comp-copy">
                  <strong>{{ item.label }}</strong>
                  <em>{{ item.desc }}</em>
                </span>
              </button>
            </div>

            <div v-else-if="section.kind === 'flow'" class="card-grid">
              <button
                v-for="item in section.items"
                :key="item.type"
                type="button"
                class="comp-card"
                draggable="true"
                :title="item.tip"
                @dragstart="startItemDrag($event, { section: item.type })"
                @click="addFlowSection(item.type)"
              >
                <span class="comp-icon" :class="`tone-${item.tone}`">
                  <NIcon :component="item.icon" :size="16" />
                </span>
                <span class="comp-copy">
                  <strong>{{ item.label }}</strong>
                  <em>{{ item.tip }}</em>
                </span>
              </button>
            </div>

            <div v-else class="card-grid">
              <button
                v-for="item in section.items"
                :key="item.key"
                type="button"
                class="comp-card"
                draggable="true"
                :title="`拖入或点击插入${item.label}`"
                @dragstart="startItemDrag($event, { component: item.key })"
                @click="addBusiness(item.key)"
              >
                <span class="comp-icon" :class="`tone-${businessMeta[item.key]?.tone || 'slate'}`">
                  <NIcon :component="businessIcons[item.icon] || DocumentTextOutline" :size="15" />
                </span>
                <span class="comp-copy">
                  <strong>{{ item.label }}</strong>
                  <em>{{ businessMeta[item.key]?.desc || '业务片段' }}</em>
                </span>
              </button>
            </div>
          </section>
        </template>
        <p v-else class="empty-hint">
          没有匹配的组件
        </p>
      </div>

      <!-- 底部：插画 + 两个下拉，避免六颗按钮占满高度 -->
      <aside class="market-promo" aria-label="市场与我的模板">
        <img class="market-promo__art" :src="marketStarUrl" alt="" draggable="false">
        <div class="market-promo__copy">
          <strong>更多模板与组件</strong>
          <em>持续更新中</em>
        </div>
        <div class="market-promo__triggers">
          <NDropdown
            trigger="click"
            placement="top-start"
            :options="printTemplateMenu"
            @select="onPrintTemplateMenu"
          >
            <button type="button" class="market-btn market-btn--primary" title="整页单据版式：市场 / 选用 / 安装">
              整页模板
              <i class="i-lucide:chevron-right" aria-hidden="true" />
            </button>
          </NDropdown>
          <NDropdown
            trigger="click"
            placement="top-end"
            :options="canvasComponentMenu"
            @select="onCanvasComponentMenu"
          >
            <button type="button" class="market-btn market-btn--ghost" title="画布片段：市场 / 选用 / 安装">
              画布组件
              <i class="i-lucide:chevron-right" aria-hidden="true" />
            </button>
          </NDropdown>
        </div>
      </aside>
    </div>

    <ContentPackageImportDialog
      v-model:show="importOpen"
      kind="print-component"
      :busy="importing"
      @confirm="importPrintAsset"
    />
    <ContentPackageImportDialog
      v-model:show="printImportOpen"
      kind="print"
      :busy="printImporting"
      @confirm="onPrintPackageConfirm"
    />
    <InstalledContentAssetPicker
      v-model:show="libraryOpen"
      kind="print-component"
      select-label="插入到画布"
      title="我的画布组件"
      @select="applyLibraryComponent"
    />
  </section>
</template>

<style scoped>
.print-palette {
  display: grid;
  grid-template-columns: 64px minmax(0, 1fr);
  gap: 0;
  height: 100%;
  min-height: 280px;
  margin: 0;
  border: 1px solid var(--border-light, #e8edf3);
  border-radius: 0;
  background: #f7f8fb;
  overflow: hidden;
}

.palette-rail {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 8px 6px;
  border-right: 1px solid var(--border-light, #e8edf3);
  background: #f3f5f9;
}

.rail-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  width: 100%;
  padding: 8px 4px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: var(--text-secondary, #4e5969);
  cursor: pointer;
  font-size: 10px;
  line-height: 1.25;
  text-align: center;
}

.rail-item:hover {
  background: rgb(255 255 255 / 70%);
  color: var(--text-primary);
}

.rail-item.active {
  background: #e8f1ff;
  color: var(--primary-color, #2f6bff);
  font-weight: 600;
}

.palette-main {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  background: var(--bg-primary, #fff);
}

.palette-search {
  flex: none;
  padding: 8px 8px 6px;
  border-bottom: 1px solid var(--border-light, #eef2f7);
}

.palette-scroll {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 8px;
  scrollbar-gutter: stable;
}

.palette-section + .palette-section {
  margin-top: 14px;
}

.section-head {
  margin-bottom: 8px;
}

.section-head h3 {
  margin: 0;
  color: var(--text-primary, #1d2129);
  font-size: 13px;
  font-weight: 650;
  line-height: 1.3;
}

.section-head p {
  margin: 2px 0 0;
  color: var(--text-tertiary, #86909c);
  font-size: 11px;
  line-height: 1.4;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 6px;
}

.comp-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  width: 100%;
  min-height: 0;
  padding: 10px 8px;
  border: 1px solid #e8edf3;
  border-radius: 10px;
  background: #fff;
  box-shadow: 0 1px 2px rgb(15 23 42 / 3%);
  color: var(--text-primary);
  cursor: grab;
  text-align: center;
}

.comp-card:hover {
  border-color: color-mix(in srgb, var(--primary-color, #2f6bff) 35%, #e8edf3);
  box-shadow: 0 4px 12px rgb(47 107 255 / 8%);
}

.comp-card:active {
  cursor: grabbing;
}

.comp-icon {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  font-size: 11px;
  font-weight: 700;
}

.comp-glyph {
  letter-spacing: -0.04em;
}

.tone-blue { background: #e8f1ff; color: #2f6bff; }
.tone-sky { background: #e0f2fe; color: #0284c7; }
.tone-cyan { background: #cffafe; color: #0891b2; }
.tone-indigo { background: #eef2ff; color: #4f46e5; }
.tone-violet { background: #f3e8ff; color: #7c3aed; }
.tone-purple { background: #f5e8ff; color: #9333ea; }
.tone-emerald { background: #d1fae5; color: #059669; }
.tone-teal { background: #ccfbf1; color: #0d9488; }
.tone-green { background: #dcfce7; color: #16a34a; }
.tone-amber { background: #fef3c7; color: #d97706; }
.tone-orange { background: #ffedd5; color: #ea580c; }
.tone-rose { background: #ffe4e6; color: #e11d48; }
.tone-slate { background: #f1f5f9; color: #475569; }

.comp-copy {
  display: grid;
  gap: 2px;
  width: 100%;
  min-width: 0;
  justify-items: center;
}

.comp-copy strong {
  overflow: hidden;
  max-width: 100%;
  color: var(--text-primary, #1d2129);
  font-size: 12px;
  font-weight: 600;
  line-height: 1.25;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.comp-copy em {
  overflow: hidden;
  max-width: 100%;
  color: var(--text-tertiary, #86909c);
  font-size: 10px;
  font-style: normal;
  line-height: 1.3;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.vertical-line-icon {
  transform: rotate(90deg);
}

.market-promo {
  flex: none;
  display: grid;
  justify-items: center;
  gap: 6px;
  margin: 0;
  padding: 10px 10px 12px;
  border: 0;
  border-top: 1px solid #e8edf3;
  border-radius: 0;
  background: linear-gradient(180deg, #f7f9fc 0%, #eef3ff 100%);
  text-align: center;
}

.market-promo__art {
  display: block;
  width: 48px;
  height: 48px;
  object-fit: contain;
  pointer-events: none;
  user-select: none;
}

.market-promo__copy {
  display: grid;
  gap: 2px;
  justify-items: center;
}

.market-promo__copy strong {
  color: #3b6ef5;
  font-size: 12px;
  font-weight: 700;
  line-height: 1.25;
}

.market-promo__copy em {
  color: #7b8db0;
  font-size: 10px;
  font-style: normal;
  line-height: 1.25;
}

.market-promo__triggers {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px;
  width: 100%;
  margin-top: 2px;
}

.market-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  width: 100%;
  height: 30px;
  padding: 0 8px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 600;
  line-height: 1;
  text-decoration: none;
  cursor: pointer;
}

.market-btn i {
  font-size: 12px;
  opacity: 0.85;
}

.market-btn--primary {
  border: 1px solid transparent;
  background: var(--primary-color, #2f6bff);
  color: #fff;
  box-shadow: 0 1px 2px rgb(47 107 255 / 18%);
}

.market-btn--primary:hover {
  filter: brightness(1.04);
}

.market-btn--ghost {
  border: 1px solid #e8edf3;
  background: #fff;
  color: var(--text-secondary, #4e5969);
  font-size: 11px;
  font-weight: 600;
}

.market-btn--ghost:hover {
  border-color: color-mix(in srgb, var(--primary-color, #2f6bff) 35%, #e8edf3);
  color: var(--primary-color, #2f6bff);
}

.empty-hint {
  margin: 24px 0;
  color: var(--text-tertiary, #86909c);
  font-size: 12px;
  text-align: center;
}
</style>
