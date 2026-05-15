<template>
  <div :class="`go-preview ${chartEditStore.editCanvasConfig.previewScaleType}`" @mousedown="dragCanvas">
    <template v-if="showEntity">
      <!-- 实体区域 -->
      <div ref="entityRef" class="go-preview-entity">
        <!-- 缩放层 -->
        <div ref="previewRef" class="go-preview-scale">
          <!-- 展示层 -->
          <transition :name="previewTransitionName" mode="out-in">
            <div
              v-if="show"
              :key="chartEditStore.getActivePageId"
              class="go-preview-page"
              :style="previewRefStyle"
            >
              <!-- 渲染层 -->
              <preview-render-list :key="chartEditStore.getActivePageId"></preview-render-list>
            </div>
          </transition>
        </div>
      </div>
    </template>
    <template v-else>
      <!-- 缩放层 -->
      <div ref="previewRef" class="go-preview-scale">
        <!-- 展示层 -->
        <transition :name="previewTransitionName" mode="out-in">
          <div
            v-if="show"
            :key="chartEditStore.getActivePageId"
            class="go-preview-page"
            :style="previewRefStyle"
          >
            <!-- 渲染层 -->
            <preview-render-list :key="chartEditStore.getActivePageId"></preview-render-list>
          </div>
        </transition>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { PreviewRenderList } from './components/PreviewRenderList'
import { getFilterStyle, setTitle } from '@/utils'
import { getSessionStorageInfo, keyRecordHandle, dragCanvas } from './utils'
import { useComInstall } from './hooks/useComInstall.hook'
import { useScale } from './hooks/useScale.hook'
import { useStore } from './hooks/useStore.hook'
import { PreviewScaleEnum } from '@/enums/styleEnum'
import type { ChartEditStorageType } from './index.d'
import { useChartEditStore } from '@/store/modules/chartEditStore/chartEditStore'
import { useInitVChartsTheme } from '@/hooks'
import { resolveAssetSourceUrl } from '@/utils'

// const localStorageInfo: ChartEditStorageType = getSessionStorageInfo() as ChartEditStorageType

await getSessionStorageInfo()
const chartEditStore = useChartEditStore() as unknown as ChartEditStorageType
const resolvedBackgroundImage = ref('')

setTitle(`预览-${chartEditStore.editCanvasConfig.projectName}`)

const previewRefStyle = computed(() => {
  const canvas = chartEditStore.editCanvasConfig
  const computedBackground = canvas.selectColor
    ? { background: canvas.background }
    : resolvedBackgroundImage.value
      ? { background: `url(${resolvedBackgroundImage.value}) center center / cover no-repeat !important` }
      : {}
  return {
    overflow: 'hidden',
    position: 'relative',
    width: canvas.width ? `${canvas.width || 100}px` : '100%',
    height: canvas.height ? `${canvas.height}px` : '100%',
    ...computedBackground,
    ...getFilterStyle(chartEditStore.editCanvasConfig)
  }
})

watch(
  () => chartEditStore.editCanvasConfig.backgroundImage,
  async (newValue) => {
    resolvedBackgroundImage.value = newValue ? await resolveAssetSourceUrl(newValue) : ''
  },
  { immediate: true }
)

const showEntity = computed(() => {
  const type = chartEditStore.editCanvasConfig.previewScaleType
  return type === PreviewScaleEnum.SCROLL_Y || type === PreviewScaleEnum.SCROLL_X
})

const previewTransitionName = computed(() => {
  const transition = chartEditStore.getRuntimePageTransition || chartEditStore.getPageTransition
  return transition === 'none' ? 'go-preview-page-none' : `go-preview-page-${transition || 'fade'}`
})

useStore(chartEditStore)
const { entityRef, previewRef } = useScale(chartEditStore)
const { show } = useComInstall(chartEditStore)

// 开启键盘监听
keyRecordHandle()

// 处理全局的 vChart 主题
useInitVChartsTheme(chartEditStore)
</script>

<style lang="scss" scoped>
@include go('preview') {
  position: relative;
  height: 100vh;
  width: 100vw;
  @include background-image('background-image');
  &.fit,
  &.full {
    display: flex;
    align-items: center;
    justify-content: center;
    overflow: hidden;
    .go-preview-scale {
      transform-origin: center center;
    }
  }
  &.scrollY {
    overflow-x: hidden;
    .go-preview-scale {
      transform-origin: left top;
    }
  }
  &.scrollX {
    overflow-y: hidden;
    .go-preview-scale {
      transform-origin: left top;
    }
  }
  .go-preview-entity {
    overflow: hidden;
  }
  .go-preview-page {
    transform-origin: center center;
  }
}

.go-preview-page-fade-enter-active,
.go-preview-page-fade-leave-active,
.go-preview-page-slide-left-enter-active,
.go-preview-page-slide-left-leave-active,
.go-preview-page-slide-right-enter-active,
.go-preview-page-slide-right-leave-active,
.go-preview-page-zoom-enter-active,
.go-preview-page-zoom-leave-active {
  transition: opacity 0.24s ease, transform 0.24s ease;
}

.go-preview-page-fade-enter-from,
.go-preview-page-fade-leave-to {
  opacity: 0;
}

.go-preview-page-slide-left-enter-from {
  opacity: 0;
  transform: translateX(32px);
}

.go-preview-page-slide-left-leave-to {
  opacity: 0;
  transform: translateX(-32px);
}

.go-preview-page-slide-right-enter-from {
  opacity: 0;
  transform: translateX(-32px);
}

.go-preview-page-slide-right-leave-to {
  opacity: 0;
  transform: translateX(32px);
}

.go-preview-page-zoom-enter-from,
.go-preview-page-zoom-leave-to {
  opacity: 0;
  transform: scale(0.96);
}
</style>
