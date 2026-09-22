<script setup>
import { NButton, NColorPicker, NFormItem, NInput, NInputNumber, NSelect, NSwitch } from 'naive-ui'
import { computed } from 'vue'
import FileUpload from '@/components/file-upload/index.vue'
import { usePrintDesignerStore } from '@/stores/print/printDesignerStore'
import { toPrintColor } from '../../protocol/printColor'
import { groupedFieldSelectOptions } from '../fieldGroups'
import {
  buildExportFileNamePattern,
  DEFAULT_WATERMARK_STYLE,
  describeExportFileName,
  nextWatermark,
  parseExportFileNameParts,
  parseWatermarkContent,
  WATERMARK_DENSITY_OPTIONS,
  watermarkDensityValue,
} from '../paperPanelModel'
import { printFontSizeOptions } from '../printFonts'
import { PRINT_MM_PRESETS, printMmOptions } from '../printMeasures'

const store = usePrintDesignerStore()
const orientations = [{ label: '纵向', value: 'PORTRAIT' }, { label: '横向', value: 'LANDSCAPE' }]
const tiling = computed(() => store.document.paper.tiling || {})
const overlay = computed(() => store.document.paper.designBackground || {})
const watermark = computed(() => store.document.watermark || {})
const watermarkOn = computed(() => watermark.value.enabled !== false)
const watermarkContent = computed(() => parseWatermarkContent(watermark.value))
const fieldOptions = computed(() => groupedFieldSelectOptions(store.catalog))
const exportName = computed(() => parseExportFileNameParts(store.document.exportFileName))
const exportNameHint = computed(() => describeExportFileName(exportName.value, store.catalog, { isDefault: !store.document.exportFileName }))
const densityOptions = WATERMARK_DENSITY_OPTIONS.map(item => ({ label: item.label, value: item.value }))

function paper(key, value) {
  if (value === null)
    return
  if (key === 'orientation') {
    store.execute((doc) => {
      doc.paper.orientation = value
      store.clampDocumentToPaper(doc)
    })
    return
  }
  store.execute((doc) => {
    doc.paper[key] = value
  })
}
function margin(key, value) {
  if (value !== null) {
    store.execute((doc) => {
      doc.paper.marginMm[key] = value
      store.clampDocumentToPaper(doc)
    })
  }
}
function preset(width, height) {
  store.setPaperSize(width, height)
}
function setKind(continuous) {
  store.execute((doc) => {
    doc.paper.kind = continuous ? 'CONTINUOUS' : 'SHEET'
  })
}
function patchTiling(key, value) {
  if (value === null)
    return
  store.execute((doc) => {
    if (key === 'enabled' && !value) {
      delete doc.paper.tiling
      return
    }
    doc.paper.tiling = {
      enabled: true,
      columns: 2,
      rows: 5,
      gapXMm: 2,
      gapYMm: 2,
      sheetWidthMm: 210,
      sheetHeightMm: 297,
      repeatToFill: true,
      ...doc.paper.tiling,
      [key]: value,
    }
  })
}
function setOverlay(value) {
  const fileId = String((Array.isArray(value) ? value[0] : value) || '')
  store.execute((doc) => {
    if (!fileId) {
      delete doc.paper.designBackground
      return
    }
    doc.paper.designBackground = { opacity: 1, rotationDeg: 0, print: false, ...doc.paper.designBackground, fileId }
  })
}
function patchOverlay(key, value) {
  if (value === null || !overlay.value.fileId)
    return
  store.execute((doc) => {
    doc.paper.designBackground = { ...doc.paper.designBackground, [key]: value }
  })
}
function applyWatermark(patch) {
  store.execute((doc) => {
    const next = nextWatermark(doc.watermark, patch)
    if (next)
      doc.watermark = next
    else
      delete doc.watermark
  })
}
function setWatermarkDensity(value) {
  const item = WATERMARK_DENSITY_OPTIONS.find(option => option.value === value)
  if (item)
    applyWatermark({ gapXMm: item.gapXMm, gapYMm: item.gapYMm })
}
function applyExportName(patch) {
  const next = { ...exportName.value, ...patch }
  const customized = !!(String(next.extraText || '').trim() || next.fields.length || !next.includeTemplate)
  store.execute((doc) => {
    if (!customized) {
      delete doc.exportFileName
      return
    }
    const pattern = buildExportFileNamePattern(next)
    if (!pattern)
      delete doc.exportFileName
    else
      doc.exportFileName = pattern
  })
}
</script>

<template>
  <section class="designer-group paper-panel">
    <h3>纸张与页边距</h3>
    <div class="panel-row paper-presets">
      <NButton size="tiny" @click="preset(297, 420)">
        A3
      </NButton>
      <NButton size="tiny" @click="preset(210, 297)">
        A4
      </NButton>
      <NButton size="tiny" @click="preset(148, 210)">
        A5
      </NButton>
      <NButton size="tiny" @click="preset(250, 353)">
        B4
      </NButton>
      <NButton size="tiny" @click="preset(176, 250)">
        B5
      </NButton>
      <NButton size="tiny" @click="preset(80, 297)">
        80mm
      </NButton>
      <NButton size="tiny" @click="preset(50, 30)">
        标签
      </NButton>
    </div>
    <NFormItem label="连续纸" size="small">
      <NSwitch :value="store.document.paper.kind === 'CONTINUOUS'" @update:value="setKind" />
    </NFormItem>
    <NFormItem label="方向" size="small">
      <NSelect :value="store.document.paper.orientation" :options="orientations" @update:value="paper('orientation', $event)" />
    </NFormItem>
    <div class="panel-grid">
      <NFormItem v-for="(label, key) in { widthMm: '短边 mm', heightMm: '长边 mm' }" :key="key" :label="label" size="small">
        <NSelect :value="store.document.paper[key]" :options="printMmOptions(store.document.paper[key], PRINT_MM_PRESETS.paper)" :filterable="false" :consistent-menu-width="false" @update:value="paper(key, $event)" />
      </NFormItem>
      <NFormItem v-for="(label, key) in { top: '上边距 mm', right: '右边距 mm', bottom: '下边距 mm', left: '左边距 mm' }" :key="key" :label="label" size="small">
        <NSelect :value="store.document.paper.marginMm[key]" :options="printMmOptions(store.document.paper.marginMm[key], PRINT_MM_PRESETS.margin)" :filterable="false" :consistent-menu-width="false" @update:value="margin(key, $event)" />
      </NFormItem>
    </div>

    <div v-for="(label, key) in { header: '页眉', footer: '页脚' }" :key="key" class="paper-block">
      <div class="block-head">
        <h3>{{ label }}</h3>
        <label class="head-switch">
          <span>每页重复</span>
          <NSwitch size="small" :value="store.document[key].repeat" @update:value="store.execute(doc => { doc[key].repeat = $event })" />
        </label>
      </div>
      <NFormItem label="高度 mm" size="small">
        <NSelect :value="store.document[key].heightMm" :options="printMmOptions(store.document[key].heightMm, PRINT_MM_PRESETS.band)" :filterable="false" :consistent-menu-width="false" @update:value="store.execute(doc => { doc[key].heightMm = $event })" />
      </NFormItem>
      <div class="panel-row">
        <NButton size="tiny" :disabled="store.document[key].heightMm > 0" @click="store.expandBand(key)">
          展开
        </NButton>
        <NButton size="tiny" :disabled="store.document[key].heightMm <= 0" @click="store.collapseBand(key)">
          收起
        </NButton>
        <NButton size="tiny" secondary @click="store.selectSurface(key)">
          编辑{{ label }}
        </NButton>
      </div>
    </div>

    <div class="paper-block">
      <div class="block-head">
        <h3>水印</h3>
        <NSwitch size="small" :value="watermarkOn" @update:value="applyWatermark({ enabled: $event })" />
      </div>
      <p class="panel-hint">
        页面打印设置打开水印时，这张模板默认一起打。关掉后这张模板不打水印。
      </p>
      <template v-if="watermarkOn">
        <NFormItem label="固定文字" size="small">
          <NInput :value="String(watermark.text || watermarkContent.text || '')" placeholder="例如 内部资料，可留空" @update:value="applyWatermark({ text: $event })" />
        </NFormItem>
        <NFormItem label="拼接字段" size="small">
          <NSelect
            :value="watermarkContent.fields"
            :options="fieldOptions"
            multiple
            filterable
            clearable
            :max-tag-count="2"
            placeholder="多选字段，打印时用 · 拼在一起"
            :consistent-menu-width="false"
            @update:value="applyWatermark({ fields: $event || [] })"
          />
        </NFormItem>
        <NFormItem label="颜色" size="small">
          <div class="swatch-only">
            <NColorPicker
              :value="watermark.color || DEFAULT_WATERMARK_STYLE.color"
              :show-alpha="false"
              :modes="['hex']"
              @update:value="applyWatermark({ color: toPrintColor($event) })"
            />
          </div>
        </NFormItem>
        <NFormItem label="字号" size="small">
          <NSelect
            :value="watermark.fontSizePt || DEFAULT_WATERMARK_STYLE.fontSizePt"
            :options="printFontSizeOptions(watermark.fontSizePt || DEFAULT_WATERMARK_STYLE.fontSizePt)"
            :filterable="false"
            :consistent-menu-width="false"
            @update:value="applyWatermark({ fontSizePt: $event })"
          />
        </NFormItem>
        <NFormItem label="疏密" size="small">
          <NSelect :value="watermarkDensityValue(watermark)" :options="densityOptions" :consistent-menu-width="false" @update:value="setWatermarkDensity" />
        </NFormItem>
      </template>
    </div>

    <div class="paper-block">
      <h3>PDF 文件名</h3>
      <NFormItem label="固定文字" size="small">
        <NInput :value="exportName.extraText" placeholder="可留空" @update:value="applyExportName({ extraText: $event })" />
      </NFormItem>
      <NFormItem label="带上模板名称" size="small">
        <NSwitch :value="exportName.includeTemplate" @update:value="applyExportName({ includeTemplate: $event })" />
      </NFormItem>
      <NFormItem label="带上单据字段" size="small">
        <NSelect
          :value="exportName.fields"
          :options="fieldOptions"
          multiple
          filterable
          clearable
          :max-tag-count="2"
          placeholder="多选后会拼进文件名"
          :consistent-menu-width="false"
          @update:value="applyExportName({ fields: $event || [] })"
        />
      </NFormItem>
      <p class="panel-hint">
        {{ exportNameHint }}
      </p>
    </div>

    <div class="paper-block">
      <h3>套打底图</h3>
      <NFormItem label="底图" size="small">
        <FileUpload
          :model-value="overlay.fileId || ''"
          :limit="1"
          :multiple="false"
          :show-download="false"
          :file-type="['png', 'jpg', 'jpeg', 'webp']"
          business-type="print"
          upload-button-text="选择底图"
          @update:model-value="setOverlay"
        />
      </NFormItem>
      <template v-if="overlay.fileId">
        <NFormItem label="打印时输出底图" size="small">
          <NSwitch :value="!!overlay.print" @update:value="patchOverlay('print', $event)" />
        </NFormItem>
        <NFormItem label="透明度" size="small">
          <NInputNumber :value="overlay.opacity ?? 1" :min="0" :max="1" :step="0.05" :show-button="false" @update:value="patchOverlay('opacity', $event)" />
        </NFormItem>
      </template>
    </div>

    <div class="paper-block">
      <div class="block-head">
        <h3>标签拼版</h3>
        <NSwitch size="small" :value="!!tiling.enabled" @update:value="patchTiling('enabled', $event)" />
      </div>
      <template v-if="tiling.enabled">
        <div class="panel-grid">
          <NFormItem label="列" size="small">
            <NInputNumber :value="tiling.columns || 2" :min="1" :max="12" :show-button="false" @update:value="patchTiling('columns', $event)" />
          </NFormItem>
          <NFormItem label="行" size="small">
            <NInputNumber :value="tiling.rows || 5" :min="1" :max="20" :show-button="false" @update:value="patchTiling('rows', $event)" />
          </NFormItem>
          <NFormItem label="横向间隙 mm" size="small">
            <NSelect :value="tiling.gapXMm || 0" :options="printMmOptions(tiling.gapXMm || 0, PRINT_MM_PRESETS.gap)" :filterable="false" :consistent-menu-width="false" @update:value="patchTiling('gapXMm', $event)" />
          </NFormItem>
          <NFormItem label="纵向间隙 mm" size="small">
            <NSelect :value="tiling.gapYMm || 0" :options="printMmOptions(tiling.gapYMm || 0, PRINT_MM_PRESETS.gap)" :filterable="false" :consistent-menu-width="false" @update:value="patchTiling('gapYMm', $event)" />
          </NFormItem>
          <NFormItem label="目标纸宽 mm" size="small">
            <NSelect :value="tiling.sheetWidthMm || 210" :options="printMmOptions(tiling.sheetWidthMm || 210, PRINT_MM_PRESETS.paper)" :filterable="false" :consistent-menu-width="false" @update:value="patchTiling('sheetWidthMm', $event)" />
          </NFormItem>
          <NFormItem label="目标纸高 mm" size="small">
            <NSelect :value="tiling.sheetHeightMm || 297" :options="printMmOptions(tiling.sheetHeightMm || 297, PRINT_MM_PRESETS.paper)" :filterable="false" :consistent-menu-width="false" @update:value="patchTiling('sheetHeightMm', $event)" />
          </NFormItem>
        </div>
        <NFormItem label="铺满空白格" size="small">
          <NSwitch :value="tiling.repeatToFill !== false" @update:value="patchTiling('repeatToFill', $event)" />
        </NFormItem>
      </template>
    </div>
  </section>
</template>

<style scoped>
.paper-block {
  padding: 10px 0 2px;
  border-top: 1px solid var(--border-light, #e5e7eb);
}
.paper-block h3 {
  margin: 0 0 6px !important;
}
.block-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 4px;
}
.block-head h3 {
  margin: 0 !important;
}
.head-switch {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--text-tertiary, #64748b);
  font-size: 11px;
  white-space: nowrap;
}
.panel-hint {
  margin: 0 0 8px;
  color: var(--text-tertiary, #64748b);
  font-size: 11px;
  line-height: 1.45;
}
:deep(.swatch-only .n-color-picker-trigger__value) {
  display: none;
}
</style>
