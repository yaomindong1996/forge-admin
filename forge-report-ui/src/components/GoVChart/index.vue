<template>
  <div
    ref="vChartRef"
    v-on="{
      ...Object.fromEntries(event.map((eventName: string) => [eventName, (eventData: MouseEvent) => eventHandlers(eventData, eventName)]))
    }"
  ></div>
</template>

<script setup lang="ts">
import { ref, PropType, watch, onBeforeUnmount, nextTick, toRaw, toRefs } from 'vue'
import { VChart, type IVChart, type IInitOption } from '@visactor/vchart'
import { transformHandler } from './transformProps'
import { IOption } from '@/packages/components/VChart/index.d'
import { registerChartsAndComponents } from './register'

// VChart按需加载: 注册图表及组件
registerChartsAndComponents()

// 事件说明 v1.13.0 https://www.visactor.io/vchart/api/API/event
const event = [
  'mousedown',
  'mouseup',
  'mouseupoutside',
  'rightdown',
  'rightup',
  'rightupoutside',
  'click',
  'dblclick',
  'mousemove',
  'mouseover',
  'mouseout',
  'mouseenter',
  'mouseleave',
  'wheel',
  'touchstart',
  'touchend',
  'touchendoutside',
  'touchmove',
  'touchcancel',
  'tap',
  'dragstart',
  'dragend',
  'drag',
  'dragenter',
  'dragleave',
  'dragover',
  'drop',
  'pan',
  'panstart',
  'panend',
  'press',
  'pressup',
  'pressend',
  'pinch',
  'pinchstart',
  'pinchend',
  'swipe',
  'dimensionHover',
  'dimensionClick',
  'dataZoomChange',
  'scrollBarChange',
  'brushStart',
  'brushChange',
  'brushEnd',
  'brushClear',
  'drill',
  'legendItemClick',
  'legendItemHover',
  'legendItemUnHover',
  'legendFilter',
  'initialized',
  'rendered',
  'renderFinished',
  'animationFinished',
  'layoutStart',
  'layoutEnd',
  'afterResizef'
]
const emit = defineEmits([
  'mousedown',
  'mouseup',
  'mouseupoutside',
  'rightdown',
  'rightup',
  'rightupoutside',
  'click',
  'dblclick',
  'mousemove',
  'mouseover',
  'mouseout',
  'mouseenter',
  'mouseleave',
  'wheel',
  'touchstart',
  'touchend',
  'touchendoutside',
  'touchmove',
  'touchcancel',
  'tap',
  'dragstart',
  'dragend',
  'drag',
  'dragenter',
  'dragleave',
  'dragover',
  'drop',
  'pan',
  'panstart',
  'panend',
  'press',
  'pressup',
  'pressend',
  'pinch',
  'pinchstart',
  'pinchend',
  'swipe',
  'dimensionHover',
  'dimensionClick',
  'dataZoomChange',
  'scrollBarChange',
  'brushStart',
  'brushChange',
  'brushEnd',
  'brushClear',
  'drill',
  'legendItemClick',
  'legendItemHover',
  'legendItemUnHover',
  'legendFilter',
  'initialized',
  'rendered',
  'renderFinished',
  'animationFinished',
  'layoutStart',
  'layoutEnd',
  'afterResizef'
])

const props = defineProps({
  option: {
    type: Object as PropType<
      IOption & {
        dataset: any
      }
    >,
    required: true
  },
  initOptions: {
    type: Object as PropType<
      IInitOption & {
        deepWatch?: boolean | number
      }
    >,
    required: false,
    default: () => ({})
  }
})

const vChartRef = ref()
let chart: IVChart

// 解构 props.option，排除 dataset
const { dataset, ...restOfOption } = toRefs(props.option)

// 排除 data 监听
watch(
  () => ({
    ...restOfOption
  }),
  () => {
    nextTick(() => {
      createOrUpdateChart(props.option)
    })
  },
  {
    deep: props.initOptions?.deepWatch || true,
    immediate: true
  }
)
watch(
  () => dataset.value,
  () => {
    nextTick(() => {
      createOrUpdateChart(props.option)
    })
  },
  {
    deep: false
  }
)

// 更新
const createOrUpdateChart = (
  chartProps: IOption & {
    dataset: any
  }
) => {
  if (vChartRef.value && !chart) {
    const spec = transformHandler[chartProps.category || '']?.(chartProps)
    chart = new VChart(
      { ...spec, data: chartProps.dataset },
      {
        dom: vChartRef.value,
        ...props.initOptions
      }
    )
    chart.renderSync()
    return true
  } else if (chart) {
    const spec = transformHandler[chartProps.category || '']?.(chartProps)
    chart.updateSpec({ ...spec, data: toRaw(chartProps.dataset), dataset: undefined }, false, undefined, {
      change: false,
      reMake: true,
      reAnimate: true
    })
    return true
  }
  return false
}

// 刷新
const refresh = () => {
  if (chart) {
    chart.renderSync()
  }
}

// 抛出事件
const eventHandlers = (eventData: MouseEvent, eventName: string) => {
  if (event.includes(eventName)) emit(eventName as any, eventData)
}

// 卸载
onBeforeUnmount(() => {
  if (chart) {
    chart.release()
  }
})

defineExpose({
  // 重刷新
  refresh,
  release: () => {
    if (chart) {
      chart.release()
    }
  }
})
</script>
