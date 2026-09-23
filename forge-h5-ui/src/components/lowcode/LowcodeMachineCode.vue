<template>
  <view class="machine-code" :class="`is-${kind}`">
    <text v-if="title" class="machine-code__title">{{ title }}</text>
    <scroll-view v-if="!errorMessage" class="machine-code__scroll" scroll-x :show-scrollbar="false">
      <canvas :id="canvasId" :canvas-id="canvasId" :width="canvasWidth" :height="canvasHeight" class="machine-code__canvas" :style="canvasStyle" />
    </scroll-view>
    <view v-else class="machine-code__error">{{ errorMessage }}</view>
    <text v-if="showText && value" class="machine-code__value">{{ value }}</text>
  </view>
</template>

<script setup>
import { computed, getCurrentInstance, nextTick, onMounted, ref, watch } from 'vue'
import { buildBarcodeBits, buildQrMatrix } from '@/utils/machine-code'

let sequence = 0
const props = defineProps({
  kind: { type: String, default: 'qrcode' },
  value: { type: [String, Number], default: '' },
  title: { type: String, default: '' },
  options: { type: Object, default: () => ({}) },
})

const instance = getCurrentInstance()
const canvasId = `forge-machine-code-${++sequence}`
const errorMessage = ref('')
const isQr = computed(() => props.kind === 'qrcode')
const showText = computed(() => props.options.showText !== false)
const canvasWidth = computed(() => isQr.value
  ? clamp(props.options.size, 88, 320, 132)
  : barcodeDimensions.value.width)
const canvasHeight = computed(() => isQr.value
  ? canvasWidth.value
  : barcodeDimensions.value.height)
const canvasStyle = computed(() => ({ width: `${canvasWidth.value}px`, height: `${canvasHeight.value}px` }))
const barcodeDimensions = computed(() => {
  try {
    const bits = buildBarcodeBits(props.value, props.options.format)
    const width = clamp(props.options.barWidth, 1, 4, 2)
    const margin = clamp(props.options.margin, 0, 40, 8)
    return { bits, barWidth: width, margin, width: bits.length * width + margin * 2, height: clamp(props.options.barHeight, 40, 180, 72) }
  }
  catch {
    return { bits: '', barWidth: 2, margin: 8, width: 240, height: 72 }
  }
})

function render() {
  errorMessage.value = ''
  if (!String(props.value || '').trim()) {
    errorMessage.value = isQr.value ? '未配置二维码内容' : '未配置条形码内容'
    return
  }
  try {
    const context = uni.createCanvasContext(canvasId, instance?.proxy)
    clearCanvas(context)
    if (isQr.value) drawQr(context)
    else drawBarcode(context)
    context.draw()
  }
  catch (error) {
    console.warn('[lowcode h5] machine code render failed', error)
    errorMessage.value = isQr.value ? '二维码内容无法生成' : '条形码内容或格式无效'
  }
}

function clearCanvas(context) {
  context.clearRect(0, 0, canvasWidth.value, canvasHeight.value)
  const background = String(props.options.background || '').trim()
  if (background && background !== 'transparent') {
    context.setFillStyle(background)
    context.fillRect(0, 0, canvasWidth.value, canvasHeight.value)
  }
}

function drawQr(context) {
  const matrix = buildQrMatrix(props.value, props.options.errorCorrectionLevel)
  const modules = matrix.length
  const configuredMargin = clamp(props.options.margin, 0, 32, 0)
  const moduleSize = (canvasWidth.value - configuredMargin * 2) / modules
  context.setFillStyle(props.options.foreground || '#1d2129')
  matrix.forEach((row, y) => row.forEach((dark, x) => {
    if (dark) context.fillRect(configuredMargin + x * moduleSize, configuredMargin + y * moduleSize, Math.ceil(moduleSize), Math.ceil(moduleSize))
  }))
}

function drawBarcode(context) {
  const { bits, barWidth, margin, height } = barcodeDimensions.value
  if (!bits) throw new Error('invalid barcode')
  context.setFillStyle(props.options.lineColor || '#1d2129')
  let start = -1
  for (let index = 0; index <= bits.length; index += 1) {
    if (bits[index] === '1' && start < 0) start = index
    if (bits[index] !== '1' && start >= 0) {
      context.fillRect(margin + start * barWidth, 0, (index - start) * barWidth, height)
      start = -1
    }
  }
}

function scheduleRender() {
  nextTick(() => setTimeout(render, 0))
}
function clamp(value, min, max, fallback) {
  const number = Number(value)
  return Number.isFinite(number) ? Math.min(max, Math.max(min, number)) : fallback
}

onMounted(scheduleRender)
watch(() => [props.kind, props.value, JSON.stringify(props.options)], scheduleRender)
</script>

<style lang="scss" scoped>
.machine-code { display: flex; align-items: center; flex-direction: column; gap: 16rpx; padding: 32rpx; border: 1rpx solid var(--border-color); border-radius: var(--radius-card); background: #fff; }
.machine-code__title { align-self: stretch; color: #1d2129; font-size: 24rpx; font-weight: 500; }
.machine-code__scroll { width: 100%; text-align: center; white-space: nowrap; }
.machine-code__canvas { display: inline-block; vertical-align: middle; }
.machine-code__value { max-width: 100%; overflow: hidden; color: #4e5969; font-size: 21rpx; text-overflow: ellipsis; white-space: nowrap; }
.machine-code__error { padding: 30rpx 20rpx; color: #f53f3f; font-size: 23rpx; }
</style>
