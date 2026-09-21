<script setup>
import { NColorPicker, NFormItem, NInputNumber, NSelect, NSlider } from 'naive-ui'
import { computed } from 'vue'
import { usePrintDesignerStore } from '@/stores/print/printDesignerStore'
import { toPrintColor } from '../../protocol/printColor'
import { PRINT_FONT_OPTIONS, printFontSelectValue, printFontSizeOptions } from '../printFonts'
import { PRINT_MM_PRESETS, printMmOptions } from '../printMeasures'

const props = defineProps({
  /** all | style | border */
  mode: { type: String, default: 'all' },
})

const store = usePrintDesignerStore()
const STYLE_TYPES = new Set(['TEXT', 'PAGE_NUMBER', 'BARCODE', 'QRCODE', 'IMAGE', 'HTML', 'STATIC_TABLE', 'DATA_TABLE', 'RECTANGLE', 'ELLIPSE', 'LINE'])
const isDataTable = computed(() => store.activeElement?.type === 'DATA_TABLE'
  || (store.activeSurface?.kind === 'TABLE' && !store.activeElement))
const isStaticTable = computed(() => store.activeElement?.type === 'STATIC_TABLE')
const isAnyTable = computed(() => isDataTable.value || isStaticTable.value)
const isLine = computed(() => store.activeElement?.type === 'LINE')
const tableTarget = computed(() => {
  if (store.activeElement?.type === 'DATA_TABLE' || store.activeElement?.type === 'STATIC_TABLE')
    return store.activeElement
  if (store.activeSurface?.kind === 'TABLE')
    return store.activeSurface
  return null
})
const target = computed(() => {
  if (store.activeElement)
    return STYLE_TYPES.has(store.activeElement.type) ? store.activeElement : null
  if (store.activeSurface?.kind === 'TEXT' || store.activeSurface?.kind === 'TABLE')
    return store.activeSurface
  return null
})
const headerBag = computed(() => tableTarget.value?.headerStyle || {})
const bodyBag = computed(() => tableTarget.value?.style || {})
const oddBag = computed(() => tableTarget.value?.oddRowStyle || {})
const evenBag = computed(() => tableTarget.value?.evenRowStyle || {})
const styleBag = computed(() => target.value?.style || {})
const borderBag = computed(() => {
  if (isStaticTable.value && store.tableCellIds.length)
    return store.activeTableCell?.style || {}
  if (isAnyTable.value)
    return bodyBag.value
  return styleBag.value
})
const lineColor = computed(() => styleBag.value.borderColor || styleBag.value.backgroundColor || '#000000')
const borderWidthValue = computed(() => {
  if (Number.isFinite(borderBag.value.borderWidthMm))
    return borderBag.value.borderWidthMm
  if (isLine.value && store.activeElement)
    return store.activeElement.heightMm <= store.activeElement.widthMm ? store.activeElement.heightMm : store.activeElement.widthMm
  return 0.15
})
const showTypography = computed(() => {
  if (isAnyTable.value)
    return true
  return !store.activeElement || ['TEXT', 'PAGE_NUMBER', 'BARCODE', 'QRCODE', 'HTML', 'STATIC_TABLE', 'DATA_TABLE'].includes(store.activeElement.type) || store.activeSurface?.kind === 'TEXT'
})
const showStyle = computed(() => props.mode === 'all' || props.mode === 'style')
const showBorder = computed(() => props.mode === 'all' || props.mode === 'border')
const alignments = [{ label: '左对齐', value: 'left' }, { label: '居中', value: 'center' }, { label: '右对齐', value: 'right' }, { label: '两端对齐', value: 'justify' }]
const verticalAlignments = [{ label: '顶部', value: 'top' }, { label: '垂直居中', value: 'middle' }, { label: '底部', value: 'bottom' }]
const decorations = [{ label: '无', value: 'none' }, { label: '下划线', value: 'underline' }, { label: '删除线', value: 'line-through' }, { label: '上划线', value: 'overline' }]
const fontOptions = PRINT_FONT_OPTIONS.map(item => ({ label: item.label, value: item.value }))
const formats = [{ label: '文本', value: 'TEXT' }, { label: '金额（分转元）', value: 'MONEY' }, { label: '金额大写', value: 'MONEY_UPPER' }, { label: '数字', value: 'NUMBER' }, { label: '日期', value: 'DATE' }, { label: '布尔值', value: 'BOOLEAN' }]
const textFits = [{ label: '超出时报错', value: '' }, { label: '截断', value: 'CLIP' }, { label: '自动缩小字号', value: 'SHRINK' }, { label: '自适应行高', value: 'AUTO_HEIGHT' }]
function opacityPercent(value) {
  const n = Number(value)
  return Number.isFinite(n) ? Math.round(Math.min(1, Math.max(0, n)) * 100) : 100
}
function patchOpacity(pct) {
  if (pct === null)
    return
  patch('opacity', Number((Math.min(100, Math.max(0, pct)) / 100).toFixed(2)))
}
function patch(key, value, group = 'style') {
  if (value === null || !target.value)
    return
  let next = value
  if (['color', 'backgroundColor', 'borderColor'].includes(key))
    next = toPrintColor(value)
  const change = { [group]: { ...target.value[group], [key]: next } }
  if (store.activeElement)
    store.patchSelected(change)
  else store.patchSurface(change)
}
function patchTableBand(band, key, value) {
  if (value === null || !tableTarget.value)
    return
  let next = value
  if (['color', 'backgroundColor', 'borderColor'].includes(key))
    next = toPrintColor(value)
  const field = band === 'header'
    ? 'headerStyle'
    : band === 'odd'
      ? 'oddRowStyle'
      : band === 'even'
        ? 'evenRowStyle'
        : 'style'
  const change = { [field]: { ...tableTarget.value[field], [key]: next } }
  if (isStaticTable.value && store.activeElement && (band === 'header' || band === 'body'))
    store.patchStaticTableBand(band, { [key]: next })
  else if (store.activeElement)
    store.patchSelected(change)
  else
    store.patchSurface(change)
}
function patchBorder(key, value) {
  if (isStaticTable.value && store.tableCellIds.length) {
    if (value === null)
      return
    let next = value
    if (['color', 'backgroundColor', 'borderColor'].includes(key))
      next = toPrintColor(value)
    store.patchSelectedTableCellStyle({ [key]: next })
    return
  }
  patch(key, value)
}
</script>

<template>
  <section v-if="target || tableTarget" class="designer-group">
    <h3 v-if="mode === 'all'">
      {{ showTypography ? '外观与格式' : '边框与背景' }}
    </h3>
    <h3 v-else-if="mode === 'style'">
      {{ isAnyTable ? '表头 / 表体' : '样式' }}
    </h3>
    <h3 v-else>
      边框
    </h3>

    <template v-if="showStyle && isAnyTable">
      <p class="muted tip">
        {{ isStaticTable ? '表头改第一行默认色，表体改其余行。点选格子后，中间栏或「基础」里的对齐只覆盖当前格。' : '表头和表体分开设置。某一列的覆盖色仍在「基础 → 列」里改。' }}
      </p>
      <h4>表头</h4>
      <div class="panel-grid">
        <NFormItem label="表头文字色" size="small">
          <NColorPicker :value="headerBag.color || '#000000'" :show-alpha="false" :modes="['hex']" @update:value="patchTableBand('header', 'color', $event)" />
        </NFormItem>
        <NFormItem label="表头背景" size="small">
          <NColorPicker :value="headerBag.backgroundColor || (isDataTable ? '#f1f5f9' : '#ffffff')" :show-alpha="false" :modes="['hex']" @update:value="patchTableBand('header', 'backgroundColor', $event)" />
        </NFormItem>
        <NFormItem label="表头字号" size="small">
          <NSelect :value="headerBag.fontSizePt || 10" :options="printFontSizeOptions(headerBag.fontSizePt || 10)" :consistent-menu-width="false" @update:value="patchTableBand('header', 'fontSizePt', $event)" />
        </NFormItem>
        <NFormItem label="表头对齐" size="small">
          <NSelect :value="headerBag.textAlign || (isStaticTable ? 'left' : 'left')" :options="alignments" @update:value="patchTableBand('header', 'textAlign', $event)" />
        </NFormItem>
        <NFormItem label="表头字重" size="small">
          <NSelect :value="headerBag.fontWeight || (isDataTable ? 700 : 400)" :options="[{ label: '常规', value: 400 }, { label: '加粗', value: 700 }]" @update:value="patchTableBand('header', 'fontWeight', $event)" />
        </NFormItem>
      </div>
      <h4>表体</h4>
      <div class="panel-grid">
        <NFormItem label="表体文字色" size="small">
          <NColorPicker :value="bodyBag.color || '#000000'" :show-alpha="false" :modes="['hex']" @update:value="patchTableBand('body', 'color', $event)" />
        </NFormItem>
        <NFormItem label="表体背景" size="small">
          <NColorPicker :value="bodyBag.backgroundColor || '#ffffff'" :show-alpha="false" :modes="['hex']" @update:value="patchTableBand('body', 'backgroundColor', $event)" />
        </NFormItem>
        <NFormItem label="表体字号" size="small">
          <NSelect :value="bodyBag.fontSizePt || 10" :options="printFontSizeOptions(bodyBag.fontSizePt || 10)" :consistent-menu-width="false" @update:value="patchTableBand('body', 'fontSizePt', $event)" />
        </NFormItem>
        <NFormItem label="表体对齐" size="small">
          <NSelect :value="bodyBag.textAlign || 'left'" :options="alignments" @update:value="patchTableBand('body', 'textAlign', $event)" />
        </NFormItem>
      </div>
      <template v-if="isDataTable">
        <h4>斑马纹</h4>
        <div class="panel-grid">
          <NFormItem label="奇数行背景" size="small">
            <NColorPicker :value="oddBag.backgroundColor || bodyBag.backgroundColor || '#ffffff'" :show-alpha="false" :modes="['hex']" @update:value="patchTableBand('odd', 'backgroundColor', $event)" />
          </NFormItem>
          <NFormItem label="偶数行背景" size="small">
            <NColorPicker :value="evenBag.backgroundColor || '#f8fafc'" :show-alpha="false" :modes="['hex']" @update:value="patchTableBand('even', 'backgroundColor', $event)" />
          </NFormItem>
        </div>
      </template>
      <NFormItem label="透明度" size="small">
        <div class="opacity-row">
          <NSlider :value="opacityPercent(styleBag.opacity)" :min="0" :max="100" :step="1" @update:value="patchOpacity" />
          <span class="opacity-pct">{{ opacityPercent(styleBag.opacity) }}%</span>
        </div>
      </NFormItem>
    </template>

    <template v-else-if="showStyle && showTypography">
      <NFormItem label="字体" size="small">
        <NSelect
          :value="printFontSelectValue(styleBag.fontFamily)"
          :options="fontOptions"
          filterable
          :consistent-menu-width="false"
          @update:value="patch('fontFamily', $event)"
        />
      </NFormItem>
      <div class="panel-grid">
        <NFormItem label="字号" size="small">
          <NSelect :value="styleBag.fontSizePt || 10" :options="printFontSizeOptions(styleBag.fontSizePt || 10)" :consistent-menu-width="false" @update:value="patch('fontSizePt', $event)" />
        </NFormItem>
        <NFormItem label="行高倍数" size="small">
          <NInputNumber :value="styleBag.lineHeight || 1.4" :min="1" :max="4" :step="0.1" :show-button="false" @update:value="patch('lineHeight', $event)" />
        </NFormItem>
      </div>
      <div class="panel-grid">
        <NFormItem label="水平对齐" size="small">
          <NSelect :value="styleBag.textAlign || 'left'" :options="alignments" @update:value="patch('textAlign', $event)" />
        </NFormItem>
        <NFormItem label="垂直对齐" size="small">
          <NSelect :value="styleBag.verticalAlign || 'top'" :options="verticalAlignments" @update:value="patch('verticalAlign', $event)" />
        </NFormItem>
      </div>
      <NFormItem label="字重" size="small">
        <NSelect :value="styleBag.fontWeight || 400" :options="[{ label: '常规', value: 400 }, { label: '加粗', value: 700 }]" @update:value="patch('fontWeight', $event)" />
      </NFormItem>
      <div class="panel-grid">
        <NFormItem label="字形" size="small">
          <NSelect :value="styleBag.fontStyle || 'normal'" :options="[{ label: '常规', value: 'normal' }, { label: '斜体', value: 'italic' }]" @update:value="patch('fontStyle', $event)" />
        </NFormItem>
        <NFormItem label="装饰" size="small">
          <NSelect :value="styleBag.textDecoration || 'none'" :options="decorations" @update:value="patch('textDecoration', $event)" />
        </NFormItem>
        <NFormItem label="文字颜色" size="small">
          <NColorPicker :value="styleBag.color || '#000000'" :show-alpha="false" :modes="['hex']" @update:value="patch('color', $event)" />
        </NFormItem>
        <NFormItem label="背景颜色" size="small">
          <NColorPicker :value="styleBag.backgroundColor || '#ffffff'" :show-alpha="false" :modes="['hex']" @update:value="patch('backgroundColor', $event)" />
        </NFormItem>
      </div>
      <NFormItem v-if="target.binding && target.type !== 'HTML'" label="数据格式" size="small">
        <NSelect :value="target.format?.type || 'TEXT'" :options="formats" @update:value="patch('type', $event, 'format')" />
      </NFormItem>
      <NFormItem v-if="target.type === 'TEXT' || store.activeSurface?.kind === 'TEXT'" label="文字溢出" size="small">
        <NSelect :value="styleBag.textFit || ''" :options="textFits" @update:value="patch('textFit', $event || undefined)" />
      </NFormItem>
      <NFormItem v-if="styleBag.textFit === 'SHRINK'" label="最小字号" size="small">
        <NSelect :value="styleBag.shrinkMinFontSizePt || 6" :options="printFontSizeOptions(styleBag.shrinkMinFontSizePt || 6)" :consistent-menu-width="false" @update:value="patch('shrinkMinFontSizePt', $event)" />
      </NFormItem>
      <NFormItem label="透明度" size="small">
        <div class="opacity-row">
          <NSlider :value="opacityPercent(styleBag.opacity)" :min="0" :max="100" :step="1" @update:value="patchOpacity" />
          <span class="opacity-pct">{{ opacityPercent(styleBag.opacity) }}%</span>
        </div>
      </NFormItem>
      <NFormItem v-if="target.format?.type === 'NUMBER'" label="小数位" size="small">
        <NInputNumber :value="target.format?.scale ?? 2" :min="0" :max="6" @update:value="patch('scale', $event, 'format')" />
      </NFormItem>
    </template>

    <div v-else-if="showStyle && !showTypography" class="panel-grid">
      <NFormItem :label="isLine ? '线条颜色' : '背景颜色'" size="small">
        <NColorPicker :value="isLine ? lineColor : (styleBag.backgroundColor || '#ffffff')" :show-alpha="false" :modes="['hex']" @update:value="patch(isLine ? 'borderColor' : 'backgroundColor', $event)" />
      </NFormItem>
      <NFormItem label="透明度" size="small">
        <div class="opacity-row">
          <NSlider :value="opacityPercent(styleBag.opacity)" :min="0" :max="100" :step="1" @update:value="patchOpacity" />
          <span class="opacity-pct">{{ opacityPercent(styleBag.opacity) }}%</span>
        </div>
      </NFormItem>
    </div>

    <div v-if="showBorder" class="panel-grid">
      <NFormItem :label="isLine ? '线条颜色' : '边框颜色'" size="small">
        <NColorPicker :value="(isLine ? lineColor : borderBag.borderColor) || '#000000'" :show-alpha="false" :modes="['hex']" @update:value="patchBorder('borderColor', $event)" />
      </NFormItem>
      <NFormItem :label="isLine ? '粗细 mm' : '边框 mm'" size="small">
        <NSelect :value="borderWidthValue" :options="printMmOptions(borderWidthValue, PRINT_MM_PRESETS.border)" :filterable="false" :consistent-menu-width="false" @update:value="patchBorder('borderWidthMm', $event)" />
      </NFormItem>
      <NFormItem :label="isLine ? '线条样式' : '边框样式'" size="small">
        <NSelect :value="borderBag.borderStyle || 'solid'" :options="[{ label: '实线', value: 'solid' }, { label: '虚线', value: 'dashed' }, { label: '点线', value: 'dotted' }]" @update:value="patchBorder('borderStyle', $event)" />
      </NFormItem>
      <NFormItem v-if="!isAnyTable && !isLine" label="圆角 mm" size="small">
        <NSelect :value="styleBag.borderRadiusMm ?? 0" :options="printMmOptions(styleBag.borderRadiusMm ?? 0, PRINT_MM_PRESETS.radius)" :filterable="false" :consistent-menu-width="false" @update:value="patch('borderRadiusMm', $event)" />
      </NFormItem>
      <NFormItem v-if="!isAnyTable && !isLine" label="内边距 mm" size="small">
        <NSelect :value="styleBag.paddingMm ?? 0" :options="printMmOptions(styleBag.paddingMm ?? 0, PRINT_MM_PRESETS.padding)" :filterable="false" :consistent-menu-width="false" @update:value="patch('paddingMm', $event)" />
      </NFormItem>
    </div>
  </section>
  <p v-else class="muted empty">
    选中元素后可编辑样式
  </p>
</template>

<style scoped>
.empty,
.tip {
  margin: 8px 4px;
  font-size: 11px;
}
h4 {
  margin: 10px 0 6px;
  color: var(--text-secondary, #475569);
  font-size: 11px;
  font-weight: 600;
}
:deep(.n-color-picker__value) {
  display: none !important;
}
.opacity-row {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}
.opacity-row :deep(.n-slider) {
  flex: 1 1 auto;
}
.opacity-pct {
  flex: 0 0 40px;
  color: var(--text-secondary, #64748b);
  font-size: 12px;
  text-align: right;
}
</style>
