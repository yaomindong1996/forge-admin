<template>
  <!-- 高度跟外壳 100%；首帧父级未定高时用 props.height 兜底，避免 ECharts 压成细条 -->
  <div
    class="page-chart-block"
    :style="chartShellStyle"
  >
    <div
      ref="chartRef"
      class="page-chart-canvas"
      :style="chartShellStyle"
    />
    <div v-if="!hasData" class="page-chart-empty">
      暂无图表数据，请在右侧配置分类与数值，或绑定列表数据源
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { buildPageChartOption, buildPieSeriesData, resolvePageChartType } from './page-chart-option'

const props = defineProps({
  chartType: { type: String, default: 'bar' },
  title: { type: String, default: '' },
  categories: { type: Array, default: () => [] },
  series: { type: Array, default: () => [] },
  pieItems: { type: Array, default: () => [] },
  height: { type: [Number, String], default: 260 },
  showLegend: { type: Boolean, default: true },
  smooth: { type: Boolean, default: true },
  showArea: { type: Boolean, default: false },
  colors: { type: Array, default: () => [] },
  legendPosition: { type: String, default: 'top-right' },
  barMaxWidth: { type: [Number, String], default: 36 },
  categoryLabelRotate: { type: [Number, String], default: 0 },
  yAxisName: { type: String, default: '' },
  showSplitLine: { type: Boolean, default: true },
  symbolSize: { type: [Number, String], default: 6 },
  areaOpacity: { type: [Number, String], default: 0.12 },
  gridLeft: { type: [Number, String], default: 8 },
  gridRight: { type: [Number, String], default: 12 },
  gridBottom: { type: [Number, String], default: 8 },
  doughnut: { type: Boolean, default: false },
  pieVariant: { type: String, default: 'pie' },
  innerRadius: { type: [Number, String], default: 48 },
  outerRadius: { type: [Number, String], default: 68 },
  roseType: { type: String, default: 'none' },
  showLabel: { type: Boolean, default: true },
  showPercent: { type: Boolean, default: true },
  pieBorderRadius: { type: [Number, String], default: 4 },
  pieBorderWidth: { type: [Number, String], default: 2 },
  optionOverrides: { type: [Object, String], default: null },
  /** 列表绑定时锁定数据集，防止 optionOverrides 盖掉真实数据 */
  dataLocked: { type: Boolean, default: true },
})

const chartRef = ref(null)
let chart = null
let resizeObserver = null

const resolvedType = computed(() => resolvePageChartType(props.chartType))

/** 跟父壳填满；壳高由 .grid-item / 嵌套壳 + 锚点拖拽决定，勿再用 props.height 顶 minHeight（会拖不矮） */
const chartShellStyle = computed(() => ({
  height: '100%',
  minHeight: 0,
  maxHeight: '100%',
}))

const hasData = computed(() => {
  if (resolvedType.value === 'pie') {
    const pieData = buildPieSeriesData({
      categories: props.categories,
      series: props.series,
      pieItems: props.pieItems,
    })
    return pieData.some(item => Number(item.value) > 0 || item.name)
  }
  const cats = Array.isArray(props.categories) ? props.categories.filter(Boolean) : []
  const series = Array.isArray(props.series) ? props.series : []
  return cats.length > 0 && series.some(item => Array.isArray(item?.data) && item.data.length > 0)
})

function buildPayload() {
  return {
    title: props.title,
    categories: props.categories,
    series: props.series,
    pieItems: props.pieItems,
    showLegend: props.showLegend,
    smooth: props.smooth,
    showArea: props.showArea,
    colors: props.colors,
    legendPosition: props.legendPosition,
    barMaxWidth: props.barMaxWidth,
    categoryLabelRotate: props.categoryLabelRotate,
    yAxisName: props.yAxisName,
    showSplitLine: props.showSplitLine,
    symbolSize: props.symbolSize,
    areaOpacity: props.areaOpacity,
    gridLeft: props.gridLeft,
    gridRight: props.gridRight,
    gridBottom: props.gridBottom,
    doughnut: props.doughnut,
    pieVariant: props.pieVariant,
    innerRadius: props.innerRadius,
    outerRadius: props.outerRadius,
    roseType: props.roseType,
    showLabel: props.showLabel,
    showPercent: props.showPercent,
    pieBorderRadius: props.pieBorderRadius,
    pieBorderWidth: props.pieBorderWidth,
    optionOverrides: props.optionOverrides,
    dataLocked: props.dataLocked !== false,
  }
}

let resizeRaf = 0

function scheduleChartResize() {
  if (!chart)
    return
  if (resizeRaf)
    return
  resizeRaf = window.requestAnimationFrame(() => {
    resizeRaf = 0
    chart?.resize()
  })
}

function renderChart() {
  if (!chartRef.value)
    return
  if (!chart)
    chart = echarts.init(chartRef.value)
  const option = buildPageChartOption(resolvedType.value, buildPayload())
  chart.setOption(option, true)
  scheduleChartResize()
}

function bindResize() {
  if (!chartRef.value || typeof ResizeObserver === 'undefined')
    return
  resizeObserver?.disconnect()
  // 锚点拖宽高时壳尺寸连跳，合并到每帧一次 resize，避免一卡一卡
  resizeObserver = new ResizeObserver(() => {
    scheduleChartResize()
  })
  resizeObserver.observe(chartRef.value)
}

onMounted(async () => {
  await nextTick()
  renderChart()
  bindResize()
  // 画布刚落子时父壳可能下一帧才定高，补两次 resize
  requestAnimationFrame(() => {
    scheduleChartResize()
    requestAnimationFrame(() => scheduleChartResize())
  })
})

onBeforeUnmount(() => {
  if (resizeRaf) {
    window.cancelAnimationFrame(resizeRaf)
    resizeRaf = 0
  }
  resizeObserver?.disconnect()
  resizeObserver = null
  chart?.dispose()
  chart = null
})

// 高度变化只调 resize，不要全量 setOption（拖锚点会卡）
watch(
  () => props.height,
  () => {
    scheduleChartResize()
  },
)

watch(
  () => [
    props.chartType,
    props.title,
    props.categories,
    props.series,
    props.pieItems,
    props.showLegend,
    props.smooth,
    props.showArea,
    props.colors,
    props.legendPosition,
    props.barMaxWidth,
    props.categoryLabelRotate,
    props.yAxisName,
    props.showSplitLine,
    props.symbolSize,
    props.areaOpacity,
    props.gridLeft,
    props.gridRight,
    props.gridBottom,
    props.doughnut,
    props.pieVariant,
    props.innerRadius,
    props.outerRadius,
    props.roseType,
    props.showLabel,
    props.showPercent,
    props.pieBorderRadius,
    props.pieBorderWidth,
    props.optionOverrides,
    props.dataLocked,
  ],
  async () => {
    await nextTick()
    renderChart()
  },
  { deep: true },
)
</script>

<style scoped>
.page-chart-block {
  position: relative;
  width: 100%;
  min-width: 0;
  overflow: hidden;
}

.page-chart-canvas {
  width: 100%;
  max-width: 100%;
}

.page-chart-empty {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  font-size: 12px;
  color: var(--text-tertiary, #94a3b8);
  pointer-events: none;
  text-align: center;
}
</style>
