<script setup>
/**
 * 图表属性面板：分区配置 + Option JSON 覆盖。
 * 数据融合：结构化/绑定 → 基础 option；高级 JSON 深合并；绑定数据最终写回轴与系列。
 */
import { computed, ref, watch } from 'vue'
import {
  DEFAULT_COLORS,
  parseChartOptionOverrides,
  stringifyChartOptionOverrides,
  buildPageChartOption,
} from './page-chart-option'

const props = defineProps({
  block: { type: Object, required: true },
  listBound: { type: Boolean, default: false },
})

const emit = defineEmits([
  'patchProps',
  'updateCategories',
  'updateSeriesItem',
  'addSeries',
  'removeSeries',
])

const optionText = ref('')
const optionError = ref('')
const optionSynced = ref(true)

const isLine = computed(() => props.block?.blockType === 'line-chart')
const chartProps = computed(() => props.block?.props || {})
const seriesList = computed(() => Array.isArray(chartProps.value.series) ? chartProps.value.series : [])
const isFullWidth = computed(() => {
  const mode = chartProps.value.style?.widthMode || 'full'
  return mode === 'full' || mode === 'auto'
})
const chartWidthValue = computed(() => {
  if (isFullWidth.value)
    return null
  const raw = chartProps.value.style?.pageFlowWidth ?? chartProps.value.style?.width
  const n = Number(String(raw ?? '').replace('px', ''))
  return Number.isFinite(n) && n > 0 ? Math.round(n) : 480
})

const legendPositionOptions = [
  { label: '右上', value: 'top-right' },
  { label: '顶部居中', value: 'top' },
  { label: '底部', value: 'bottom' },
  { label: '左侧', value: 'left' },
]

const colorPresets = computed(() => {
  const raw = chartProps.value.colors
  return Array.isArray(raw) && raw.length ? raw : [...DEFAULT_COLORS]
})

watch(
  () => chartProps.value.optionOverrides,
  (value) => {
    if (!optionSynced.value)
      return
    optionText.value = stringifyChartOptionOverrides(value)
    optionError.value = ''
  },
  { immediate: true, deep: true },
)

function patch(partial = {}) {
  emit('patchProps', partial)
}

function onTitle(value) {
  patch({ title: value })
}

function onHeight(value) {
  const height = Math.max(160, Math.min(720, Number(value) || 260))
  patch({
    height,
    style: {
      ...(chartProps.value.style || {}),
      heightMode: 'fixed',
      height,
      pageFlowHeight: height,
      minHeight: height,
    },
  })
}

function onWidth(value) {
  if (value == null || value === '') {
    onFullWidth(true)
    return
  }
  const width = Math.max(180, Math.min(1600, Number(value) || 480))
  patch({
    style: {
      ...(chartProps.value.style || {}),
      widthMode: 'fixed',
      width,
      pageFlowWidth: `${width}px`,
      pageFlowX: 0,
      maxWidth: '100%',
    },
  })
}

function onFullWidth(enabled) {
  if (enabled) {
    const style = { ...(chartProps.value.style || {}) }
    delete style.pageFlowWidth
    patch({
      style: {
        ...style,
        widthMode: 'full',
        width: '100%',
        pageFlowX: 0,
        maxWidth: '100%',
      },
    })
    return
  }
  onWidth(chartWidthValue.value || 480)
}

function onColorAt(index, value) {
  const next = [...colorPresets.value]
  next[index] = value || DEFAULT_COLORS[index % DEFAULT_COLORS.length]
  patch({ colors: next })
}

function fillOptionFromCurrent() {
  const option = buildPageChartOption(isLine.value ? 'line' : 'bar', {
    title: chartProps.value.title,
    categories: chartProps.value.categories,
    series: chartProps.value.series,
    showLegend: chartProps.value.showLegend,
    smooth: chartProps.value.smooth,
    showArea: chartProps.value.showArea,
    colors: chartProps.value.colors,
    legendPosition: chartProps.value.legendPosition,
    barMaxWidth: chartProps.value.barMaxWidth,
    categoryLabelRotate: chartProps.value.categoryLabelRotate,
    yAxisName: chartProps.value.yAxisName,
    showSplitLine: chartProps.value.showSplitLine,
    symbolSize: chartProps.value.symbolSize,
    areaOpacity: chartProps.value.areaOpacity,
    gridLeft: chartProps.value.gridLeft,
    gridRight: chartProps.value.gridRight,
    gridBottom: chartProps.value.gridBottom,
  })
  // 给用户可编辑的「覆盖层」草稿：去掉 data，避免和数据源打架
  const draft = { ...option }
  if (draft.xAxis && typeof draft.xAxis === 'object') {
    const { data: _data, ...axisRest } = draft.xAxis
    draft.xAxis = axisRest
  }
  if (Array.isArray(draft.series)) {
    draft.series = draft.series.map((item) => {
      const { data: _d, ...rest } = item || {}
      return rest
    })
  }
  optionText.value = JSON.stringify(draft, null, 2)
  optionSynced.value = false
  optionError.value = ''
}

function applyOptionText() {
  const text = String(optionText.value || '').trim()
  if (!text) {
    patch({ optionOverrides: null })
    optionError.value = ''
    optionSynced.value = true
    return
  }
  const parsed = parseChartOptionOverrides(text)
  if (!parsed) {
    optionError.value = 'JSON 无效：请输入对象，例如 { "grid": { "left": 24 } }'
    return
  }
  patch({ optionOverrides: parsed })
  optionError.value = ''
  optionSynced.value = true
}

function clearOptionOverrides() {
  optionText.value = '{\n  \n}'
  patch({ optionOverrides: null })
  optionError.value = ''
  optionSynced.value = true
}

function onOptionInput(value) {
  optionText.value = value
  optionSynced.value = false
  optionError.value = ''
}
</script>

<template>
  <div class="page-chart-property-panel">
    <!-- 基础 -->
    <section class="chart-prop-section">
      <header class="chart-prop-section__head">
        <strong>基础</strong>
        <span>标题与画布高度（拖拽锚点会同步这里）</span>
      </header>
      <div class="chart-prop-section__body">
        <label class="chart-prop-field">
          <span>标题</span>
          <n-input
            :value="chartProps.title || ''"
            size="small"
            placeholder="图表标题"
            @update:value="onTitle"
          />
        </label>
        <div class="chart-prop-size-row">
          <label class="chart-prop-field">
            <span>宽度</span>
            <n-input-number
              :value="chartWidthValue"
              :min="180"
              :max="1600"
              size="small"
              style="width: 100%"
              placeholder="通栏"
              @update:value="onWidth"
            />
          </label>
          <label class="chart-prop-field">
            <span>高度</span>
            <n-input-number
              :value="Number(chartProps.height || 260)"
              :min="160"
              :max="720"
              size="small"
              style="width: 100%"
              @update:value="onHeight"
            />
          </label>
        </div>
        <div class="chart-prop-switch-row">
          <span>宽度通栏</span>
          <n-switch
            :value="isFullWidth"
            size="small"
            @update:value="onFullWidth"
          />
        </div>
        <p class="chart-prop-tip">
          可拖左右锚点改宽、拖下边/角点改高。两个组件上下叠着要并排时：选中下面那个，右上角「…」→「与上一组件并排」。
        </p>
      </div>
    </section>

    <!-- 外观 -->
    <section class="chart-prop-section">
      <header class="chart-prop-section__head">
        <strong>外观</strong>
        <span>图例、坐标与配色</span>
      </header>
      <div class="chart-prop-section__body">
        <div class="chart-prop-switch-row">
          <span>图例</span>
          <n-switch
            :value="chartProps.showLegend !== false"
            size="small"
            @update:value="patch({ showLegend: $event })"
          />
        </div>
        <label v-if="chartProps.showLegend !== false" class="chart-prop-field">
          <span>图例位置</span>
          <n-select
            :value="chartProps.legendPosition || 'top-right'"
            :options="legendPositionOptions"
            size="small"
            @update:value="patch({ legendPosition: $event || 'top-right' })"
          />
        </label>
        <template v-if="isLine">
          <div class="chart-prop-switch-row">
            <span>平滑曲线</span>
            <n-switch
              :value="chartProps.smooth !== false"
              size="small"
              @update:value="patch({ smooth: $event })"
            />
          </div>
          <div class="chart-prop-switch-row">
            <span>面积填充</span>
            <n-switch
              :value="chartProps.showArea === true"
              size="small"
              @update:value="patch({ showArea: $event })"
            />
          </div>
          <label v-if="chartProps.showArea" class="chart-prop-field">
            <span>面积透明度</span>
            <n-input-number
              :value="Number(chartProps.areaOpacity ?? 0.12)"
              :min="0.04"
              :max="0.4"
              :step="0.02"
              size="small"
              style="width: 100%"
              @update:value="patch({ areaOpacity: $event ?? 0.12 })"
            />
          </label>
          <label class="chart-prop-field">
            <span>拐点大小</span>
            <n-input-number
              :value="Number(chartProps.symbolSize || 6)"
              :min="4"
              :max="14"
              size="small"
              style="width: 100%"
              @update:value="patch({ symbolSize: $event || 6 })"
            />
          </label>
        </template>
        <label v-else class="chart-prop-field">
          <span>柱宽上限</span>
          <n-input-number
            :value="Number(chartProps.barMaxWidth || 36)"
            :min="8"
            :max="80"
            size="small"
            style="width: 100%"
            @update:value="patch({ barMaxWidth: $event || 36 })"
          />
        </label>
        <label class="chart-prop-field">
          <span>分类标签旋转</span>
          <n-input-number
            :value="Number(chartProps.categoryLabelRotate || 0)"
            :min="0"
            :max="60"
            size="small"
            style="width: 100%"
            @update:value="patch({ categoryLabelRotate: $event || 0 })"
          />
        </label>
        <label class="chart-prop-field">
          <span>Y 轴名称</span>
          <n-input
            :value="chartProps.yAxisName || ''"
            size="small"
            placeholder="可选，如 数量"
            @update:value="patch({ yAxisName: $event || '' })"
          />
        </label>
        <div class="chart-prop-switch-row">
          <span>网格线</span>
          <n-switch
            :value="chartProps.showSplitLine !== false"
            size="small"
            @update:value="patch({ showSplitLine: $event })"
          />
        </div>
        <div class="chart-prop-field">
          <span>绘图区内边距</span>
          <div class="chart-prop-grid-row">
            <label>
              <em>左</em>
              <n-input-number
                :value="Number(chartProps.gridLeft ?? 8)"
                :min="0"
                :max="80"
                size="tiny"
                @update:value="patch({ gridLeft: $event ?? 8 })"
              />
            </label>
            <label>
              <em>右</em>
              <n-input-number
                :value="Number(chartProps.gridRight ?? 12)"
                :min="0"
                :max="80"
                size="tiny"
                @update:value="patch({ gridRight: $event ?? 12 })"
              />
            </label>
            <label>
              <em>底</em>
              <n-input-number
                :value="Number(chartProps.gridBottom ?? 8)"
                :min="0"
                :max="80"
                size="tiny"
                @update:value="patch({ gridBottom: $event ?? 8 })"
              />
            </label>
          </div>
        </div>
        <div class="chart-prop-field">
          <span>系列配色</span>
          <div class="chart-color-row">
            <label
              v-for="(color, index) in colorPresets.slice(0, 6)"
              :key="`c-${index}`"
              class="chart-color-swatch"
              :title="`系列色 ${index + 1}`"
            >
              <input
                type="color"
                :value="color"
                @input="onColorAt(index, $event.target.value)"
              >
            </label>
          </div>
        </div>
      </div>
    </section>

    <!-- 数据 -->
    <section class="chart-prop-section">
      <header class="chart-prop-section__head">
        <strong>数据</strong>
        <span>{{ listBound ? '列表绑定中 · 下方仅作无数据占位' : '静态分类与系列' }}</span>
      </header>
      <div class="chart-prop-section__body">
        <div class="chart-prop-fusion-note">
          <strong>与数据源怎么融合</strong>
          <ol>
            <li>上方「数据来源」选静态或列表绑定。</li>
            <li>绑定启用后：用字段映射生成分类轴 / 系列数值。</li>
            <li>下方「高级 Option」只改样式与结构，不会盖住绑定数据。</li>
          </ol>
        </div>
        <template v-if="!listBound">
          <label class="chart-prop-field">
            <span>分类轴</span>
            <n-input
              :value="(chartProps.categories || []).join(',')"
              type="textarea"
              :autosize="{ minRows: 2, maxRows: 4 }"
              size="small"
              placeholder="类型A, 类型B, 类型C"
              @update:value="emit('updateCategories', $event)"
            />
            <em>多个分类用逗号分隔</em>
          </label>
          <div class="chart-prop-field">
            <span>系列数据</span>
            <div class="chart-series-list">
              <div
                v-for="(serie, idx) in seriesList"
                :key="`serie-${idx}`"
                class="chart-series-card"
              >
                <n-input
                  :value="serie.name"
                  size="small"
                  placeholder="系列名"
                  @update:value="emit('updateSeriesItem', idx, { name: $event })"
                />
                <n-input
                  :value="Array.isArray(serie.data) ? serie.data.join(',') : ''"
                  size="small"
                  placeholder="数值，如 18,12,9,6"
                  @update:value="emit('updateSeriesItem', idx, { dataText: $event })"
                />
                <n-button size="tiny" quaternary type="error" @click="emit('removeSeries', idx)">
                  删除
                </n-button>
              </div>
              <n-button size="small" dashed block @click="emit('addSeries')">
                + 添加系列
              </n-button>
            </div>
          </div>
        </template>
        <p v-else class="chart-prop-tip">
          请在「数据来源」中选择数组字段，并映射分类轴 / 数值 / 系列名。
        </p>
      </div>
    </section>

    <!-- 高级 Option -->
    <section class="chart-prop-section is-advanced">
      <header class="chart-prop-section__head">
        <strong>高级 · ECharts Option</strong>
        <span>JSON 深合并到基础配置；数据集由上方数据源锁定</span>
      </header>
      <div class="chart-prop-section__body">
        <div class="chart-option-toolbar">
          <n-button size="tiny" secondary @click="fillOptionFromCurrent">
            从当前生成
          </n-button>
          <n-button size="tiny" type="primary" secondary @click="applyOptionText">
            应用 JSON
          </n-button>
          <n-button size="tiny" quaternary @click="clearOptionOverrides">
            清空覆盖
          </n-button>
        </div>
        <n-input
          :value="optionText"
          type="textarea"
          class="chart-option-editor"
          :autosize="{ minRows: 8, maxRows: 16 }"
          placeholder='{ "grid": { "left": 24 }, "tooltip": { "trigger": "item" } }'
          @update:value="onOptionInput"
        />
        <p v-if="optionError" class="chart-prop-error">
          {{ optionError }}
        </p>
        <p v-else class="chart-prop-tip">
          示例：改边距 <code>{ "grid": { "left": 32, "right": 16 } }</code>；改提示
          <code>{ "tooltip": { "trigger": "item" } }</code>。
          不要在覆盖里写死 <code>xAxis.data</code> / <code>series[].data</code>，绑定数据会优先生效。
        </p>
      </div>
    </section>
  </div>
</template>

<style scoped>
.page-chart-property-panel {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.chart-prop-section {
  border: 1px solid var(--border-light, #e5eaf3);
  border-radius: 8px;
  background: var(--bg-primary, #fff);
  overflow: hidden;
}

.chart-prop-section.is-advanced {
  border-color: color-mix(in srgb, var(--primary-color, #2563eb) 22%, var(--border-light, #e5eaf3));
}

.chart-prop-section__head {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 10px 12px 8px;
  border-bottom: 1px solid var(--border-light, #e5eaf3);
  background: color-mix(in srgb, var(--bg-secondary, #f6f8fb) 88%, transparent);
}

.chart-prop-section__head strong {
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 600;
}

.chart-prop-section__head span {
  color: var(--text-tertiary, #94a3b8);
  font-size: 11px;
  line-height: 1.4;
}

.chart-prop-section__body {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 10px 12px 12px;
}

.chart-prop-size-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.chart-prop-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.chart-prop-field > span {
  color: var(--text-secondary, #64748b);
  font-size: 12px;
}

.chart-prop-field > em {
  color: var(--text-tertiary, #94a3b8);
  font-size: 11px;
  font-style: normal;
}

.chart-prop-switch-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 28px;
  color: var(--text-secondary, #64748b);
  font-size: 12px;
}

.chart-prop-tip,
.chart-prop-error {
  margin: 0;
  font-size: 11px;
  line-height: 1.5;
}

.chart-prop-tip {
  color: var(--text-tertiary, #94a3b8);
}

.chart-prop-tip code {
  padding: 0 4px;
  border-radius: 3px;
  background: var(--bg-secondary, #f1f5f9);
  font-size: 11px;
}

.chart-prop-error {
  color: var(--error-color, #d03050);
}

.chart-prop-fusion-note {
  display: grid;
  gap: 6px;
  padding: 8px 10px;
  border-radius: 6px;
  background: color-mix(in srgb, var(--primary-color, #2563eb) 6%, var(--bg-secondary, #f6f8fb));
  border: 1px solid color-mix(in srgb, var(--primary-color, #2563eb) 14%, transparent);
}

.chart-prop-fusion-note strong {
  color: var(--text-primary);
  font-size: 12px;
}

.chart-prop-fusion-note ol {
  margin: 0;
  padding-left: 18px;
  color: var(--text-secondary, #64748b);
  font-size: 11px;
  line-height: 1.55;
}

.chart-prop-grid-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.chart-prop-grid-row label {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.chart-prop-grid-row em {
  color: var(--text-tertiary, #94a3b8);
  font-size: 11px;
  font-style: normal;
}

.chart-color-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.chart-color-swatch {
  position: relative;
  width: 28px;
  height: 28px;
  border-radius: 6px;
  border: 1px solid var(--border-light, #e2e8f0);
  overflow: hidden;
  cursor: pointer;
}

.chart-color-swatch input {
  position: absolute;
  inset: -4px;
  width: calc(100% + 8px);
  height: calc(100% + 8px);
  padding: 0;
  border: 0;
  cursor: pointer;
}

.chart-series-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.chart-series-card {
  display: grid;
  gap: 6px;
  padding: 8px;
  border: 1px solid var(--border-light, #e5eaf3);
  border-radius: 6px;
  background: var(--bg-secondary, #f8fafc);
}

.chart-option-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.chart-option-editor :deep(textarea) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 11px;
  line-height: 1.45;
}
</style>
