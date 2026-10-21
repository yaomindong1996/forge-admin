<template>
  <!-- 高度优先跟外壳（锚点拖拽），props.height 作下限；画布 100% 填满以便 ResizeObserver 生效 -->
  <div
    class="page-chart-block"
    :style="{ height: '100%', minHeight: `${chartHeight}px` }"
  >
    <div
      ref="chartRef"
      class="page-chart-canvas"
      :style="{ height: '100%', minHeight: `${chartHeight}px` }"
    />
    <div v-if="!hasData" class="page-chart-empty">
      暂无图表数据，请在右侧配置分类与数值，或绑定列表数据源
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { buildPageChartOption } from './page-chart-option'

const props = defineProps({
  chartType: { type: String, default: 'bar' },
  title: { type: String, default: '' },
  categories: { type: Array, default: () => [] },
  series: { type: Array, default: () => [] },
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
  optionOverrides: { type: [Object, String], default: null },
  /** 列表绑定时锁定数据集，防止 optionOverrides 盖掉真实数据 */
  dataLocked: { type: Boolean, default: true },
})

const chartRef = ref(null)
let chart = null
let resizeObserver = null

const chartHeight = computed(() => {
  const n = Number(props.height)
  return Number.isFinite(n) && n > 120 ? n : 260
})

const hasData = computed(() => {
  const cats = Array.isArray(props.categories) ? props.categories.filter(Boolean) : []
  const series = Array.isArray(props.series) ? props.series : []
  return cats.length > 0 && series.some(item => Array.isArray(item?.data) && item.data.length > 0)
})

function renderChart() {
  if (!chartRef.value)
    return
  if (!chart)
    chart = echarts.init(chartRef.value)
  const option = buildPageChartOption(props.chartType === 'line' ? 'line' : 'bar', {
    title: props.title,
    categories: props.categories,
    series: props.series,
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
    optionOverrides: props.optionOverrides,
    dataLocked: props.dataLocked !== false,
  })
  chart.setOption(option, true)
  chart.resize()
}

function bindResize() {
  if (!chartRef.value || typeof ResizeObserver === 'undefined')
    return
  resizeObserver?.disconnect()
  resizeObserver = new ResizeObserver(() => {
    chart?.resize()
  })
  resizeObserver.observe(chartRef.value)
}

onMounted(async () => {
  await nextTick()
  renderChart()
  bindResize()
})

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  resizeObserver = null
  chart?.dispose()
  chart = null
})

watch(
  () => [
    props.chartType,
    props.title,
    props.categories,
    props.series,
    props.height,
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
