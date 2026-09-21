<script setup>
import { PrintOutline, SaveOutline } from '@vicons/ionicons5'
import { NAlert, NButton, NCheckbox, NFormItem, NIcon, NRadioButton, NRadioGroup, NSelect, NTag } from 'naive-ui'
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { mmToPx } from '../protocol/units'
import { createBrowserPrintSession } from '../runtime/browserPrint'
import {
  CALIBRATION_CHECKS,
  CALIBRATION_PAPERS,
  calibrationProfileKey,
  createPrintCalibrationResult,
  detectPrintCapabilities,
  readCalibrationAcceptance,
  resolveCalibrationPaper,
  writeCalibrationAcceptance,
} from '../runtime/printCalibration'
import PrintPage from '../runtime/PrintPage.vue'
import { PRINT_MM_PRESETS, printMmOptions } from './printMeasures'

const props = defineProps({ initialPaper: { type: Object, default: () => ({ widthMm: 210, heightMm: 297, orientation: 'PORTRAIT' }) } })
const presetOptions = [
  ...Object.values(CALIBRATION_PAPERS).map(item => ({ label: `${item.label} · ${item.widthMm} × ${item.heightMm} mm`, value: item.label })),
  { label: '自定义纸张', value: 'CUSTOM' },
]
function initialPreset() {
  const shorter = Math.min(props.initialPaper.widthMm, props.initialPaper.heightMm)
  const longer = Math.max(props.initialPaper.widthMm, props.initialPaper.heightMm)
  return Object.values(CALIBRATION_PAPERS).find(item => item.widthMm === shorter && item.heightMm === longer)?.label || 'CUSTOM'
}
const preset = ref(initialPreset())
const orientation = ref(props.initialPaper.orientation === 'LANDSCAPE' ? 'LANDSCAPE' : 'PORTRAIT')
const customWidthMm = ref(Math.max(148, Math.min(props.initialPaper.widthMm, props.initialPaper.heightMm)))
const customHeightMm = ref(Math.max(148, Math.max(props.initialPaper.widthMm, props.initialPaper.heightMm)))
const config = computed(() => ({ preset: preset.value, orientation: orientation.value, widthMm: customWidthMm.value, heightMm: customHeightMm.value }))
const paperError = computed(() => {
  try {
    resolveCalibrationPaper(config.value)
    return ''
  }
  catch (error) {
    return error.message
  }
})
const result = computed(() => paperError.value ? null : createPrintCalibrationResult(config.value))
const profile = computed(() => paperError.value ? '' : calibrationProfileKey(config.value))
const capability = detectPrintCapabilities()
const previewScale = computed(() => {
  if (!result.value)
    return 0.5
  const { widthMm, heightMm } = result.value.geometry
  return Math.min(0.72, 500 / mmToPx(widthMm), 540 / mmToPx(heightMm))
})
const acceptance = ref({ checks: {}, confirmedByUser: false, updatedAt: null })
const savedAt = ref(null)
const printing = ref(false)
const dialogOpened = ref(false)
const operationError = ref('')
let printSession

watch(profile, () => {
  if (!profile.value)
    return
  acceptance.value = readCalibrationAcceptance(config.value)
  savedAt.value = acceptance.value.updatedAt
  dialogOpened.value = false
  operationError.value = ''
}, { immediate: true })

function updateCheck(key, checked) {
  acceptance.value.checks[key] = checked
  acceptance.value.confirmedByUser = CALIBRATION_CHECKS.every(item => acceptance.value.checks[item.key] === true)
  savedAt.value = null
}

function saveAcceptance() {
  operationError.value = ''
  try {
    acceptance.value = writeCalibrationAcceptance(config.value, acceptance.value.checks)
    savedAt.value = acceptance.value.updatedAt
  }
  catch (error) {
    operationError.value = error.message
  }
}

async function openPrintDialog() {
  if (!result.value || printing.value)
    return
  printing.value = true
  operationError.value = ''
  dialogOpened.value = false
  try {
    printSession?.dispose()
    printSession = await createBrowserPrintSession(result.value, {
      onEvent(event) {
        if (event.result === 'DIALOG_OPENED')
          dialogOpened.value = true
      },
    })
    printSession.print()
  }
  catch (error) {
    operationError.value = error.message
  }
  finally {
    printing.value = false
  }
}

onBeforeUnmount(() => printSession?.dispose())
</script>

<template>
  <section class="calibration-workbench">
    <aside class="calibration-settings">
      <div>
        <h3>纸张设置</h3>
        <p>按实际打印机放入的纸张生成毫米校准页。</p>
      </div>
      <NFormItem label="纸张规格" size="small">
        <NSelect v-model:value="preset" aria-label="校准纸张规格" :options="presetOptions" />
      </NFormItem>
      <NFormItem label="纸张方向" size="small">
        <NRadioGroup v-model:value="orientation" aria-label="校准纸张方向" size="small">
          <NRadioButton value="PORTRAIT">
            纵向
          </NRadioButton>
          <NRadioButton value="LANDSCAPE">
            横向
          </NRadioButton>
        </NRadioGroup>
      </NFormItem>
      <div v-if="preset === 'CUSTOM'" class="custom-paper-grid">
        <NFormItem label="短边 mm" size="small">
          <NSelect
            v-model:value="customWidthMm"
            aria-label="自定义纸张短边"
            :options="printMmOptions(customWidthMm, PRINT_MM_PRESETS.paper.filter(n => n >= 148))"
            :filterable="false"
            :consistent-menu-width="false"
          />
        </NFormItem>
        <NFormItem label="长边 mm" size="small">
          <NSelect
            v-model:value="customHeightMm"
            aria-label="自定义纸张长边"
            :options="printMmOptions(customHeightMm, PRINT_MM_PRESETS.paper.filter(n => n >= 148))"
            :filterable="false"
            :consistent-menu-width="false"
          />
        </NFormItem>
      </div>
      <NAlert v-if="paperError" type="error" :bordered="false">
        {{ paperError }}
      </NAlert>

      <div class="capability-card">
        <div class="section-heading">
          <div>
            <h3>浏览器能力</h3>
            <p>检查当前浏览器能否准备隔离打印文档。</p>
          </div>
          <NTag size="small" :type="capability.status === 'READY' ? 'success' : 'warning'">
            {{ capability.status === 'READY' ? '可打开对话框' : '能力受限' }}
          </NTag>
        </div>
        <dl class="capability-list">
          <div><dt>打印 API</dt><dd>{{ capability.printFunction ? '可用' : '不可用' }}</dd></div>
          <div><dt>隔离文档</dt><dd>{{ capability.isolatedDocument ? '可用' : '不可用' }}</dd></div>
          <div><dt>CSS 毫米单位</dt><dd>{{ capability.millimetreCss === false ? '未报告支持' : capability.millimetreCss === true ? '支持' : '浏览器未提供检测结果' }}</dd></div>
          <div><dt>物理打印</dt><dd>等待人工验收</dd></div>
        </dl>
      </div>

      <NButton type="primary" block :loading="printing" :disabled="!capability.dialogAvailable || !!paperError" @click="openPrintDialog">
        <template #icon>
          <NIcon :component="PrintOutline" />
        </template>
        打开校准打印
      </NButton>
      <NAlert v-if="dialogOpened" type="info" :bordered="false">
        打印对话框已打开。该状态不代表打印机已出纸，也不代表尺寸准确。
      </NAlert>
      <NAlert v-if="operationError" type="error" :bordered="false">
        {{ operationError }}
      </NAlert>
    </aside>

    <main class="calibration-preview" aria-label="校准页预览">
      <div v-if="result" class="preview-caption">
        <strong>{{ result.calibration.label }} · {{ result.calibration.orientationLabel }}</strong>
        <span>{{ result.geometry.widthMm }} × {{ result.geometry.heightMm }} mm · 预览缩放 {{ Math.round(previewScale * 100) }}%</span>
      </div>
      <div v-if="result" class="calibration-paper-space" :style="{ width: `${result.geometry.widthMm * previewScale}mm`, height: `${result.geometry.heightMm * previewScale}mm` }">
        <div :style="{ transform: `scale(${previewScale})`, transformOrigin: 'top left' }">
          <PrintPage :page="result.pages[0]" :geometry="result.geometry" />
        </div>
      </div>
    </main>

    <aside class="acceptance-panel">
      <div class="section-heading">
        <div>
          <h3>本机实测验收</h3>
          <p>打印后用直尺核对。记录只保存在当前浏览器。</p>
        </div>
        <NTag size="small" :type="acceptance.confirmedByUser ? 'success' : 'default'">
          {{ acceptance.confirmedByUser ? '人工已确认' : '待人工确认' }}
        </NTag>
      </div>
      <div class="acceptance-checks">
        <NCheckbox v-for="item in CALIBRATION_CHECKS" :key="item.key" :checked="acceptance.checks[item.key]" @update:checked="updateCheck(item.key, $event)">
          {{ item.label }}
        </NCheckbox>
      </div>
      <NAlert type="warning" :bordered="false">
        这里保存的是使用者对实体纸张的测量结论，系统不会自动把它标记为打印成功。
      </NAlert>
      <NButton block :type="acceptance.confirmedByUser ? 'primary' : 'default'" :disabled="!!paperError" @click="saveAcceptance">
        <template #icon>
          <NIcon :component="SaveOutline" />
        </template>
        保存本机验收记录
      </NButton>
      <small v-if="savedAt">保存时间：{{ new Date(savedAt).toLocaleString() }}</small>
    </aside>
  </section>
</template>

<style scoped>
.calibration-workbench {
  height: 100%;
  min-height: 0;
  display: grid;
  grid-template-columns: 270px minmax(360px, 1fr) 300px;
  background: var(--bg-primary, #fff);
}
.calibration-settings,
.acceptance-panel {
  min-height: 0;
  overflow: auto;
  padding: 16px;
}
.calibration-settings {
  border-right: 1px solid var(--border-light, #ddd);
}
.acceptance-panel {
  border-left: 1px solid var(--border-light, #ddd);
}
h3 {
  margin: 0;
  font-size: 14px;
}
p {
  margin: 4px 0 14px;
  color: var(--text-tertiary, #64748b);
  font-size: 12px;
  line-height: 1.5;
}
.custom-paper-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}
.capability-card {
  margin: 10px 0 14px;
  padding: 12px;
  border: 1px solid var(--border-light, #ddd);
  border-radius: 6px;
  background: color-mix(in srgb, var(--gray-100, #f8fafc) 70%, transparent);
}
.section-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}
.capability-list {
  margin: 0;
  font-size: 12px;
}
.capability-list div {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  padding: 5px 0;
  border-top: 1px solid var(--border-light, #e5e7eb);
}
.capability-list dt {
  color: var(--text-tertiary, #64748b);
}
.capability-list dd {
  margin: 0;
  text-align: right;
}
.calibration-preview {
  min-width: 0;
  min-height: 0;
  overflow: auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 18px 30px 34px;
  background: #dfe3e8;
}
.preview-caption {
  width: min(100%, 520px);
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
  color: #475569;
  font-size: 11px;
}
.calibration-paper-space {
  flex: none;
  overflow: hidden;
  outline: 1px solid rgb(15 23 42 / 12%);
  background: white;
  box-shadow: 0 7px 26px rgb(15 23 42 / 20%);
}
.acceptance-checks {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin: 4px 0 16px;
}
.acceptance-checks :deep(.n-checkbox__label) {
  line-height: 1.45;
}
.acceptance-panel small {
  display: block;
  margin-top: 10px;
  color: var(--text-tertiary, #64748b);
  text-align: center;
}
.n-alert + .n-button,
.n-button + .n-alert {
  margin-top: 10px;
}
@media (max-width: 1050px) {
  .calibration-workbench {
    grid-template-columns: 250px minmax(340px, 1fr);
  }
  .acceptance-panel {
    grid-column: 1 / -1;
    border-top: 1px solid var(--border-light, #ddd);
    border-left: 0;
  }
}
</style>
