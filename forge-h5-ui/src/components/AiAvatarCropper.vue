<template>
  <Teleport to="body">
    <view v-if="modelValue" class="avatar-cropper">
      <view class="cropper-topbar">
        <button class="topbar-button" @click="cancel">
          <AiIcon icon="/static/icons/ai-icon/x.svg" color="#ffffff" size="sm" />
        </button>
        <view class="topbar-title">
          <text class="topbar-main">编辑头像</text>
          <text class="topbar-sub">拖动图片调整位置，双指或滑块缩放</text>
        </view>
        <button class="topbar-text" @click="resetCrop">
          <text>重置</text>
        </button>
      </view>

      <view
        ref="stageRef"
        class="cropper-stage"
        @touchstart.stop="startDrag"
        @touchmove.stop.prevent="moveDrag"
        @touchend.stop="endDrag"
        @touchcancel.stop="endDrag"
        @mousedown.stop.prevent="startMouseDrag"
        @wheel.stop.prevent="handleWheel"
      >
        <image
          v-if="source"
          class="cropper-image"
          :src="source"
          mode="aspectFill"
          :style="imageStyle"
          draggable="false"
        />
        <view class="cropper-frame" :class="`cropper-frame--${shape}`" :style="frameStyle" />
        <view class="cropper-grid" :class="`cropper-grid--${shape}`" :style="frameStyle">
          <view v-for="line in 4" :key="line" class="grid-line" />
        </view>
      </view>

      <view class="cropper-panel">
        <view class="shape-tabs">
          <button
            v-for="item in shapeOptions"
            :key="item.value"
            class="shape-tab"
            :class="{ active: shape === item.value }"
            @click="shape = item.value"
          >
            <AiIcon :icon="item.icon" :color="shape === item.value ? '#ffffff' : item.color" size="sm" />
            <text>{{ item.label }}</text>
          </button>
        </view>

        <view class="zoom-control">
          <AiIcon icon="/static/icons/ai-icon/image.svg" color="#86909c" size="sm" />
          <slider
            :value="zoomPercent"
            min="0"
            max="100"
            active-color="#ffffff"
            background-color="rgba(255,255,255,0.2)"
            block-color="#ffffff"
            block-size="22"
            @changing="handleZoomChange"
            @change="handleZoomChange"
          />
          <AiIcon icon="/static/icons/ai-icon/maximize-2.svg" color="#86909c" size="sm" />
        </view>

        <view class="cropper-actions">
          <button class="action-button action-button--ghost" :disabled="loading" @click="cancel">
            <text>取消</text>
          </button>
          <button class="action-button action-button--primary" :disabled="loading" @click="confirm">
            <text>{{ loading ? '处理中' : '使用头像' }}</text>
          </button>
        </view>
      </view>
    </view>
  </Teleport>
</template>

<script setup>
import { computed, nextTick, onUnmounted, ref, watch } from 'vue'
import AiIcon from '@/components/AiIcon.vue'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false,
  },
  source: {
    type: String,
    default: '',
  },
  size: {
    type: Number,
    default: 512,
  },
  quality: {
    type: Number,
    default: 0.92,
  },
})

const emit = defineEmits(['update:modelValue', 'confirm', 'cancel'])

const stageRef = ref(null)
const imageMeta = ref({ width: 1, height: 1 })
const stageSize = ref({ width: 360, height: 520 })
const cropSize = ref(280)
const zoom = ref(1)
const offset = ref({ x: 0, y: 0 })
const dragState = ref(null)
const pinchState = ref(null)
const shape = ref('circle')
const loading = ref(false)

const shapeOptions = [
  { value: 'circle', label: '圆形', icon: '/static/icons/ai-icon/circle.svg', color: '#4266f7' },
  { value: 'round', label: '圆角', icon: '/static/icons/ai-icon/square.svg', color: '#16815d' },
  { value: 'square', label: '方形', icon: '/static/icons/ai-icon/crop.svg', color: '#ff7d00' },
]

const baseScale = computed(() => Math.max(
  cropSize.value / imageMeta.value.width,
  cropSize.value / imageMeta.value.height
))
const renderScale = computed(() => baseScale.value * zoom.value)
const renderedSize = computed(() => ({
  width: imageMeta.value.width * renderScale.value,
  height: imageMeta.value.height * renderScale.value,
}))
const zoomPercent = computed(() => Math.round((zoom.value - 1) / 2 * 100))
const frameStyle = computed(() => ({
  width: `${cropSize.value}px`,
  height: `${cropSize.value}px`,
}))
const imageStyle = computed(() => ({
  width: `${renderedSize.value.width}px`,
  height: `${renderedSize.value.height}px`,
  transform: `translate(calc(-50% + ${offset.value.x}px), calc(-50% + ${offset.value.y}px))`,
}))

watch(() => props.modelValue, async (visible) => {
  if (!visible) {
    unlockBodyScroll()
    return
  }
  lockBodyScroll()
  await nextTick()
  measureStage()
  loadImageMeta()
}, { immediate: true })

watch(() => props.source, () => {
  if (props.modelValue) {
    loadImageMeta()
  }
})

function measureStage() {
  const width = window.innerWidth || 375
  const height = window.innerHeight || 667
  stageSize.value = {
    width,
    height: Math.max(320, height - 260),
  }
  cropSize.value = Math.round(Math.min(width * 0.78, stageSize.value.height * 0.72, 340))
}

function loadImageMeta() {
  if (!props.source) {
    return
  }
  const image = new Image()
  image.onload = () => {
    imageMeta.value = {
      width: image.naturalWidth || 1,
      height: image.naturalHeight || 1,
    }
    resetCrop()
  }
  image.src = props.source
}

function resetCrop() {
  zoom.value = 1
  offset.value = { x: 0, y: 0 }
  clampOffset()
}

function handleZoomChange(event) {
  zoom.value = 1 + Number(event.detail.value || 0) / 100 * 2
  clampOffset()
}

function handleWheel(event) {
  const next = zoom.value + (event.deltaY > 0 ? -0.08 : 0.08)
  zoom.value = Math.min(3, Math.max(1, next))
  clampOffset()
}

function startDrag(event) {
  if (event.touches?.length === 2) {
    pinchState.value = {
      distance: getTouchDistance(event.touches),
      zoom: zoom.value,
    }
    dragState.value = null
    return
  }

  const point = event.touches?.[0]
  if (!point) {
    return
  }
  dragState.value = {
    x: point.clientX,
    y: point.clientY,
    originX: offset.value.x,
    originY: offset.value.y,
  }
}

function moveDrag(event) {
  if (event.touches?.length === 2 && pinchState.value) {
    const ratio = getTouchDistance(event.touches) / Math.max(1, pinchState.value.distance)
    zoom.value = Math.min(3, Math.max(1, pinchState.value.zoom * ratio))
    clampOffset()
    return
  }
  applyDrag(event.touches?.[0])
}

function startMouseDrag(event) {
  dragState.value = {
    x: event.clientX,
    y: event.clientY,
    originX: offset.value.x,
    originY: offset.value.y,
  }
  window.addEventListener('mousemove', handleMouseMove)
  window.addEventListener('mouseup', endMouseDrag)
}

function handleMouseMove(event) {
  applyDrag(event)
}

function endMouseDrag() {
  endDrag()
  window.removeEventListener('mousemove', handleMouseMove)
  window.removeEventListener('mouseup', endMouseDrag)
}

function applyDrag(point) {
  if (!dragState.value || !point) {
    return
  }
  offset.value = {
    x: dragState.value.originX + point.clientX - dragState.value.x,
    y: dragState.value.originY + point.clientY - dragState.value.y,
  }
  clampOffset()
}

function endDrag() {
  dragState.value = null
  pinchState.value = null
}

function clampOffset() {
  const maxX = Math.max(0, (renderedSize.value.width - cropSize.value) / 2)
  const maxY = Math.max(0, (renderedSize.value.height - cropSize.value) / 2)
  offset.value = {
    x: Math.min(maxX, Math.max(-maxX, offset.value.x)),
    y: Math.min(maxY, Math.max(-maxY, offset.value.y)),
  }
}

function getTouchDistance(touches) {
  const [a, b] = touches
  const x = a.clientX - b.clientX
  const y = a.clientY - b.clientY
  return Math.sqrt(x * x + y * y)
}

async function confirm() {
  if (loading.value) {
    return
  }
  loading.value = true
  try {
    const file = await cropToFile()
    emit('confirm', { file, shape: shape.value })
    close()
  }
  finally {
    loading.value = false
  }
}

function cancel() {
  emit('cancel')
  close()
}

function close() {
  emit('update:modelValue', false)
}

function cropToFile() {
  return new Promise((resolve, reject) => {
    const image = new Image()
    image.onload = () => {
      const scale = renderScale.value
      const sourceSize = cropSize.value / scale
      const sx = Math.min(
        imageMeta.value.width - sourceSize,
        Math.max(0, imageMeta.value.width / 2 - sourceSize / 2 - offset.value.x / scale)
      )
      const sy = Math.min(
        imageMeta.value.height - sourceSize,
        Math.max(0, imageMeta.value.height / 2 - sourceSize / 2 - offset.value.y / scale)
      )
      const canvas = document.createElement('canvas')
      canvas.width = props.size
      canvas.height = props.size
      const ctx = canvas.getContext('2d')
      ctx.clearRect(0, 0, props.size, props.size)
      applyClip(ctx)
      ctx.drawImage(image, sx, sy, sourceSize, sourceSize, 0, 0, props.size, props.size)
      const mimeType = shape.value === 'square' ? 'image/jpeg' : 'image/png'
      canvas.toBlob((blob) => {
        if (!blob) {
          reject(new Error('图片裁剪失败'))
          return
        }
        const ext = mimeType === 'image/png' ? 'png' : 'jpg'
        resolve(new File([blob], `avatar-${Date.now()}.${ext}`, { type: mimeType }))
      }, mimeType, props.quality)
    }
    image.onerror = () => reject(new Error('图片读取失败'))
    image.src = props.source
  })
}

function applyClip(ctx) {
  if (shape.value === 'square') {
    return
  }
  if (shape.value === 'circle') {
    ctx.beginPath()
    ctx.arc(props.size / 2, props.size / 2, props.size / 2, 0, Math.PI * 2)
    ctx.clip()
    return
  }
  const radius = Math.round(props.size * 0.18)
  roundRect(ctx, 0, 0, props.size, props.size, radius)
  ctx.clip()
}

function roundRect(ctx, x, y, width, height, radius) {
  ctx.beginPath()
  ctx.moveTo(x + radius, y)
  ctx.arcTo(x + width, y, x + width, y + height, radius)
  ctx.arcTo(x + width, y + height, x, y + height, radius)
  ctx.arcTo(x, y + height, x, y, radius)
  ctx.arcTo(x, y, x + width, y, radius)
  ctx.closePath()
}

function lockBodyScroll() {
  document.body.style.overflow = 'hidden'
}

function unlockBodyScroll() {
  document.body.style.overflow = ''
}

onUnmounted(() => {
  unlockBodyScroll()
  endMouseDrag()
})
</script>

<style lang="scss" scoped>
.avatar-cropper {
  position: fixed;
  z-index: 2147483000;
  inset: 0;
  display: flex;
  flex-direction: column;
  color: #ffffff;
  background: #1d2129;
}

.cropper-topbar {
  display: flex;
  min-height: 118rpx;
  align-items: center;
  gap: 18rpx;
  padding: calc(20rpx + env(safe-area-inset-top)) 28rpx 18rpx;
  box-sizing: border-box;
}

.topbar-button,
.topbar-text {
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0;
  padding: 0;
  border: 0;
  background: transparent;
}

.topbar-button::after,
.topbar-text::after,
.shape-tab::after,
.action-button::after {
  border: 0;
}

.topbar-button {
  width: 88rpx;
  height: 88rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.18);
  border-radius: 8rpx;
  background: #1d2129;
}

.topbar-title {
  min-width: 0;
  flex: 1;
}

.topbar-main,
.topbar-sub {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.topbar-main {
  font-size: 32rpx;
  font-weight: 500;
}

.topbar-sub {
  margin-top: 6rpx;
  color: rgba(226, 232, 240, 0.72);
  font-size: 22rpx;
  font-weight: 400;
}

.topbar-text {
  min-width: 74rpx;
  min-height: 88rpx;
}

.topbar-text text {
  color: #4266f7;
  font-size: 25rpx;
  font-weight: 500;
}

.cropper-stage {
  position: relative;
  min-height: 0;
  flex: 1;
  overflow: hidden;
  cursor: grab;
  touch-action: none;
}

.cropper-image {
  position: absolute;
  top: 50%;
  left: 50%;
  max-width: none;
  max-height: none;
  transform-origin: center center;
  user-select: none;
  -webkit-user-drag: none;
}

.cropper-frame,
.cropper-grid {
  position: absolute;
  top: 50%;
  left: 50%;
  overflow: hidden;
  transform: translate(-50%, -50%);
  pointer-events: none;
}

.cropper-frame {
  border: 3rpx solid rgba(255, 255, 255, 0.94);
  box-shadow: 0 0 0 9999px rgba(2, 6, 23, 0.7);
}

.cropper-frame--circle,
.cropper-grid--circle {
  border-radius: 9999px;
}

.cropper-frame--round,
.cropper-grid--round {
  border-radius: 20%;
}

.cropper-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  grid-template-rows: repeat(3, 1fr);
  opacity: 0.58;
}

.grid-line {
  border-right: 1rpx solid rgba(255, 255, 255, 0.32);
  border-bottom: 1rpx solid rgba(255, 255, 255, 0.32);
}

.cropper-panel {
  padding: 22rpx 28rpx calc(28rpx + env(safe-area-inset-bottom));
  border-top: 1rpx solid rgba(255, 255, 255, 0.1);
  background: #1d2129;
}

.shape-tabs {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14rpx;
}

.shape-tab {
  display: flex;
  min-height: 88rpx;
  align-items: center;
  justify-content: center;
  gap: 10rpx;
  margin: 0;
  padding: 0;
  border: 1rpx solid rgba(255, 255, 255, 0.12);
  border-radius: 8rpx;
  background: #1d2129;
}

.shape-tab.active {
  border-color: #4266f7;
  background: #4266f7;
}

.shape-tab text {
  color: #c9cdd4;
  font-size: 24rpx;
  font-weight: 500;
}

.zoom-control {
  display: grid;
  grid-template-columns: 34rpx minmax(0, 1fr) 34rpx;
  align-items: center;
  gap: 16rpx;
  margin-top: 22rpx;
}

.cropper-actions {
  display: grid;
  grid-template-columns: 0.84fr 1.16fr;
  gap: 16rpx;
  margin-top: 24rpx;
}

.action-button {
  min-height: 88rpx;
  margin: 0;
  padding: 0;
  border-radius: 8rpx;
  font-size: 28rpx;
  font-weight: 500;
  line-height: 86rpx;
}

.action-button--ghost {
  color: #c9cdd4;
  border: 1rpx solid rgba(255, 255, 255, 0.18);
  background: #1d2129;
}

.action-button--primary {
  color: #ffffff;
  background: #4266f7;
}
</style>
