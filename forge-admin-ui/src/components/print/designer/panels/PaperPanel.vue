<script setup>
import { NButton, NFormItem, NInput, NInputNumber, NSelect, NSwitch } from 'naive-ui'
import { computed } from 'vue'
import FileUpload from '@/components/file-upload/index.vue'
import { usePrintDesignerStore } from '@/stores/print/printDesignerStore'
import { PRINT_MM_PRESETS, printMmOptions } from '../printMeasures'

const store = usePrintDesignerStore()
const orientations = [{ label: '纵向', value: 'PORTRAIT' }, { label: '横向', value: 'LANDSCAPE' }]
const tiling = computed(() => store.document.paper.tiling || {})
const overlay = computed(() => store.document.paper.designBackground || {})
const watermark = computed(() => store.document.watermark || {})
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
function patchWatermark(key, value) {
  store.execute((doc) => {
    const next = { opacity: 0.08, rotateDeg: -24, gapXMm: 64, gapYMm: 42, fontSizePt: 14, color: '#94a3b8', ...doc.watermark, [key]: value }
    if (!String(next.expression || '').trim())
      delete next.expression
    if (!String(next.text || '').trim() && !next.expression)
      delete doc.watermark
    else
      doc.watermark = next
  })
}
function setExportFileName(value) {
  store.execute((doc) => {
    const text = String(value || '').trim()
    if (!text)
      delete doc.exportFileName
    else
      doc.exportFileName = text
  })
}
const fileNameFieldToken = '{{main.name}}'
const fileNameTemplateToken = '{template}'
const fileNameTimeToken = '{timestamp}'
</script>

<template>
  <section class="designer-group">
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
    <template v-for="(label, key) in { header: '页眉', footer: '页脚' }" :key="key">
      <div class="panel-grid band-row">
        <NFormItem :label="`${label}高度 mm`" size="small">
          <NSelect :value="store.document[key].heightMm" :options="printMmOptions(store.document[key].heightMm, PRINT_MM_PRESETS.band)" :filterable="false" :consistent-menu-width="false" @update:value="store.execute(doc => { doc[key].heightMm = $event })" />
        </NFormItem>
        <NFormItem :label="`${label}每页重复`" size="small">
          <NSwitch :value="store.document[key].repeat" @update:value="store.execute(doc => { doc[key].repeat = $event })" />
        </NFormItem>
      </div>
      <div class="panel-row">
        <NButton size="tiny" :disabled="store.document[key].heightMm > 0" @click="store.expandBand(key)">
          展开为 12mm
        </NButton>
        <NButton size="tiny" :disabled="store.document[key].heightMm <= 0" @click="store.collapseBand(key)">
          收起
        </NButton>
        <NButton size="tiny" secondary @click="store.selectSurface(key)">
          编辑{{ label }}
        </NButton>
      </div>
    </template>
    <h3>标签拼版</h3>
    <NFormItem label="启用拼版" size="small">
      <NSwitch :value="!!tiling.enabled" @update:value="patchTiling('enabled', $event)" />
    </NFormItem>
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
    <h3>导出文件名</h3>
    <NFormItem label="PDF 文件名" size="small">
      <NInput
        :value="String(store.document.exportFileName || '')"
        placeholder="留空则用模板名、单据名称和时间戳"
        @update:value="setExportFileName"
      />
    </NFormItem>
    <p class="export-name-hint">
      可改。支持 <code>{{ fileNameFieldToken }}</code>、<code>{{ fileNameTemplateToken }}</code>、<code>{{ fileNameTimeToken }}</code>。未写时间戳时导出仍会自动加上。
    </p>
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
    <h3>水印</h3>
    <NFormItem label="水印文字" size="small">
      <NInput :value="String(watermark.text || '')" placeholder="例如 内部资料" @update:value="patchWatermark('text', $event)" />
    </NFormItem>
    <NFormItem label="水印表达式" size="small">
      <NInput :value="String(watermark.expression || '')" placeholder="可选，如 main.company" @update:value="patchWatermark('expression', $event)" />
    </NFormItem>
  </section>
</template>

<style scoped>
.export-name-hint {
  margin: -4px 0 12px;
  color: var(--text-tertiary, #64748b);
  font-size: 11px;
  line-height: 1.45;
}
.export-name-hint code {
  font-size: 11px;
}
</style>
