<template>
  <div class="go-content-charts-item-animation-patch">
    <div
      ref="contentChartsItemBoxRef"
      class="go-content-charts-item-box"
      :class="[chartMode === ChartModeEnum.DOUBLE ? 'double' : 'single']"
    >
      <!-- 每一项组件的渲染 -->
      <div
        class="item-box"
        v-for="(item, index) in menuOptions"
        :key="item.title"
        draggable="true"
        @dragstart.capture="!item.disabled && dragStartHandle($event, item)"
        @dragend="!item.disabled && dragendHandle()"
        @dblclick="dblclickHandle(item)"
        @click="clickHandle(item)"
      >
        <span class="asset-index">{{ index + 1 }}</span>
        <div class="list-center go-flex-center go-transition" draggable="false">
          <FgIconify v-if="item.icon" class="list-img" :icon="item.icon" color="#999" width="48" style="height: auto" />
          <chart-glob-image v-else class="list-img" :chartConfig="item" />
        </div>
        <div class="asset-meta">
          <n-text class="asset-title" depth="2">
            <n-ellipsis>{{ item.title }}</n-ellipsis>
          </n-text>
          <div class="asset-hint-row">
            <span class="asset-hint">{{ item.disabled ? '上传到素材库' : '拖拽 / 双击' }}</span>
            <span
              v-if="item.configEvents?.renameHandle"
              class="rename-trigger"
              @click.stop="openRenameModal(item)"
              title="重命名"
            >
              <FgIconify icon="ion:create-outline" width="12" color="rgba(148,163,184,0.56)" />
            </span>
          </div>
        </div>
        <!-- 遮罩 -->
        <div v-if="item.disabled" class="list-model"></div>
      </div>
    </div>

    <n-modal
      v-model:show="renameModalVisible"
      preset="dialog"
      title="重命名素材"
      positive-text="确定"
      negative-text="取消"
      @positive-click="confirmRename"
      @negative-click="cancelRename"
    >
      <n-input
        v-model:value="renameInputValue"
        placeholder="输入新的文件名"
      />
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { PropType, watch, ref, Ref, computed, nextTick } from 'vue'
import { ChartGlobImage } from '@/components/Pages/ChartGlobImage'
import { useChartEditStore } from '@/store/modules/chartEditStore/chartEditStore'
import { EditCanvasTypeEnum } from '@/store/modules/chartEditStore/chartEditStore.d'
import { ChartModeEnum } from '@/store/modules/chartLayoutStore/chartLayoutStore.d'
import { useChartLayoutStore } from '@/store/modules/chartLayoutStore/chartLayoutStore'
import { componentInstall, loadingStart, loadingFinish, loadingError, JSONStringify } from '@/utils'
import { DragKeyEnum } from '@/enums/editPageEnum'
import { createComponent } from '@/packages'
import { ConfigType, CreateComponentType } from '@/packages/index.d'
import { fetchConfigComponent, fetchChartComponent } from '@/packages/index'
import { FgIconify } from '@/components/FgIconify'

import omit from 'lodash/omit'

const chartEditStore = useChartEditStore()

const renameModalVisible = ref(false)
const renameItem = ref<ConfigType | null>(null)
const renameInputValue = ref('')

const openRenameModal = (item: ConfigType) => {
  renameItem.value = item
  renameInputValue.value = item.title
  renameModalVisible.value = true
}

const confirmRename = async () => {
  const item = renameItem.value
  if (!item || !renameInputValue.value.trim() || renameInputValue.value.trim() === item.title) {
    renameModalVisible.value = false
    return
  }
  try {
    const handle = item.configEvents?.renameHandle as ((name: string) => Promise<string>) | undefined
    if (handle) {
      const newName = await handle(renameInputValue.value.trim())
      item.title = newName
    }
    window['$message']?.success('重命名成功')
    renameModalVisible.value = false
  } catch (e: any) {
    window['$message']?.error(e?.message || '重命名失败')
  }
}

const cancelRename = () => {
  renameModalVisible.value = false
}

const props = defineProps({
  menuOptions: {
    type: Array as PropType<ConfigType[]>,
    default: () => []
  }
})

const chartLayoutStore = useChartLayoutStore()
const contentChartsItemBoxRef = ref()

// 组件展示状态
const chartMode: Ref<ChartModeEnum> = computed(() => {
  return chartLayoutStore.getChartType
})

// 拖拽处理
const dragStartHandle = (e: DragEvent, item: ConfigType) => {
  if (item.disabled) return
  // 动态注册图表组件
  componentInstall(item.chartKey, fetchChartComponent(item))
  componentInstall(item.conKey, fetchConfigComponent(item))
  // 将配置项绑定到拖拽属性上
  e!.dataTransfer!.setData(DragKeyEnum.DRAG_KEY, JSONStringify(omit(item, ['image'])))
  // 修改状态
  chartEditStore.setEditCanvas(EditCanvasTypeEnum.IS_CREATE, true)
}

// 拖拽结束
const dragendHandle = () => {
  chartEditStore.setEditCanvas(EditCanvasTypeEnum.IS_CREATE, false)
}

// 双击添加
const dblclickHandle = async (item: ConfigType) => {
  if (item.disabled) return
  try {
    loadingStart()
    // 动态注册图表组件
    componentInstall(item.chartKey, fetchChartComponent(item))
    componentInstall(item.conKey, fetchConfigComponent(item))
    // 创建新图表组件
    let newComponent: CreateComponentType = await createComponent(item)
    if (item.redirectComponent) {
      item.dataset && (newComponent.option.dataset = item.dataset)
      newComponent.chartConfig.title = item.title
      newComponent.chartConfig.chartFrame = item.chartFrame
    }
    // 添加
    chartEditStore.addComponentList(newComponent, false, true)
    // 选中
    chartEditStore.setTargetSelectChart(newComponent.id)
    loadingFinish()
  } catch (error) {
    loadingError()
    window['$message'].warning(`图表正在研发中, 敬请期待...`)
  }
}

// 单击事件
const clickHandle = (item: ConfigType) => {
  item?.configEvents?.addHandle(item)
}

watch(
  () => chartMode.value,
  (newValue: ChartModeEnum) => {
    if (newValue === ChartModeEnum.DOUBLE) {
      nextTick(() => {
        contentChartsItemBoxRef.value.classList.add('miniAnimation')
      })
    }
  }
)
</script>

<style lang="scss" scoped>
/* 列表项宽度 */
$itemWidth: 100%;
$maxItemWidth: 170px;
$halfItemWidth: calc(50% - 5px);
/* 内容高度 */
$centerHeight: 78px;
$halfCenterHeight: 74px;

@include go('content-charts-item-animation-patch') {
  padding: 12px;
}

@include go('content-charts-item-box') {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 10px;
  transition: all 0.7s linear;
  .item-box {
    position: relative;
    margin: 0;
    width: $itemWidth;
    overflow: hidden;
    border-radius: 8px;
    cursor: pointer;
    border: 1px solid rgba(148, 163, 184, 0.12);
    background:
      linear-gradient(135deg, rgba(255, 255, 255, 0.05), transparent 44%),
      rgba(15, 23, 42, 0.5);
    box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.03);
    min-height: 108px;
    transition:
      transform 0.2s ease,
      border-color 0.2s ease,
      box-shadow 0.2s ease,
      background 0.2s ease;

    &:hover {
      border-color: rgba(var(--app-theme-rgb), 0.28);
      box-shadow:
        0 14px 26px rgba(0, 0, 0, 0.24),
        0 0 16px rgba(var(--app-theme-rgb), 0.1);
      transform: translateY(-2px);
      .list-img {
        transform: scale(1.08);
      }
      .asset-hint {
        color: rgba(var(--app-theme-rgb), 0.95);
      }
    }

    .asset-index {
      position: absolute;
      top: 8px;
      right: 8px;
      z-index: 1;
      min-width: 22px;
      height: 20px;
      padding: 0 5px;
      border-radius: 6px;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      font-size: 10px;
      line-height: 20px;
      color: rgba(226, 232, 240, 0.76);
      background: rgba(2, 6, 23, 0.46);
      border: 1px solid rgba(255, 255, 255, 0.06);
    }

    .list-center {
      padding: 10px 0;
      height: $centerHeight;
      overflow: hidden;
      pointer-events: none;
      background:
        radial-gradient(circle at center, rgba(var(--app-theme-rgb), 0.08), transparent 62%),
        rgba(2, 6, 23, 0.16);

      :deep(img),
      :deep(svg),
      :deep(canvas) {
        pointer-events: none;
        user-select: none;
        -webkit-user-drag: none;
      }

      .list-img {
        height: 70px;
        max-width: 128px;
        border-radius: 6px;
        object-fit: contain;
        @extend .go-transition;
      }
    }

    .asset-meta {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 8px;
      padding: 9px 10px 10px;
      min-width: 0;

      .asset-title {
        flex: 1;
        min-width: 0;
        font-size: 12px;
        line-height: 16px;
        font-weight: 600;
        color: rgba(226, 232, 240, 0.92);
      }

      .asset-hint-row {
        flex-shrink: 0;
        display: flex;
        align-items: center;
        gap: 4px;

        .asset-hint {
          font-size: 10px;
          line-height: 16px;
          color: rgba(148, 163, 184, 0.72);
          transition: color 0.2s ease;
        }

        .rename-trigger {
          flex-shrink: 0;
          cursor: pointer;
          display: inline-flex;
          align-items: center;
          opacity: 0.56;
          transition: opacity 0.2s;
          &:hover {
            opacity: 1;
          }
        }
      }
    }

    .list-model {
      z-index: 1;
      position: absolute;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      background-color: rgba(0, 0, 0, 0);
    }
  }
  &.single {
    .item-box {
      @extend .go-transition;
      display: grid;
      grid-template-columns: 94px minmax(0, 1fr);
      align-items: center;
      min-height: 82px;
      background:
        linear-gradient(90deg, rgba(var(--app-theme-rgb), 0.08), transparent 44%),
        rgba(15, 23, 42, 0.46);
    }

    .list-center {
      height: 82px;
      padding: 0;
      background: rgba(2, 6, 23, 0.22);

      .list-img {
        height: 58px;
        max-width: 76px;
      }
    }

    .asset-meta {
      flex-direction: column;
      align-items: flex-start;
      justify-content: center;
      padding: 0 34px 0 10px;
      gap: 3px;
    }
  }
  &.double {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    align-items: stretch;
    gap: 10px;

    .item-box {
      width: 100%;
      max-width: none;
      .list-img {
        max-width: 76px;
      }
    }
    .list-center {
      height: $halfCenterHeight;
      padding: 8px 0 0;
      .list-img {
        height: 56px;
        width: auto;
        transition: all 0.2s;
        object-fit: contain;
      }
    }

    .asset-meta {
      display: block;
      padding: 8px 9px 10px;

      .asset-title {
        display: block;
      }

      .asset-hint-row {
        display: block;
        margin-top: 2px;
      }
    }
  }
  /* 缩小展示 */
  @keyframes miniAnimation {
    from {
      width: $itemWidth * 2;
    }
    to {
      width: $itemWidth;
    }
  }
  &.miniAnimation {
    animation: miniAnimation 0.5s;
  }
}
</style>
