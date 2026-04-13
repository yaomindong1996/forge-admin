<template>
  <!-- 工作台相关 -->
  <div class="go-chart">
    <n-layout>
      <layout-header-pro>
        <template #left>
          <header-left-btn></header-left-btn>
        </template>
        <template #center>
          <header-title></header-title>
        </template>
        <template #ri-left>
          <header-right-btn></header-right-btn>
        </template>
      </layout-header-pro>
      <n-layout-content content-style="overflow:hidden; display: flex">
        <div style="overflow:hidden; display: flex; flex: 1">
          <content-charts></content-charts>
          <content-layers></content-layers>
        </div>
        <content-configurations></content-configurations>
      </n-layout-content>
    </n-layout>
  </div>
  <!-- 右键 -->
  <n-dropdown
    placement="bottom-start"
    trigger="manual"
    size="small"
    :x="mousePosition.x"
    :y="mousePosition.y"
    :options="menuOptions"
    :show="chartEditStore.getRightMenuShow"
    :on-clickoutside="onClickOutSide"
    @select="handleMenuSelect"
  ></n-dropdown>
  <!-- 加载蒙层 -->
  <content-load></content-load>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { loadAsyncComponent, getSessionStorage } from '@/utils'
import { LayoutHeaderPro } from '@/layout/components/LayoutHeaderPro'
import { useContextMenu } from './hooks/useContextMenu.hook'
import { useChartEditStore } from '@/store/modules/chartEditStore/chartEditStore'
import { useChartHistoryStore } from '@/store/modules/chartHistoryStore/chartHistoryStore'
import { StorageEnum } from '@/enums/storageEnum'
import { fetchRouteParamsLocation } from '@/utils'

const chartHistoryStoreStore = useChartHistoryStore()
const chartEditStore = useChartEditStore()

// 记录初始化
chartHistoryStoreStore.canvasInit(chartEditStore.getEditCanvas)

// 编辑器加载时，如果画布为空，检查 SessionStorage 中是否有待加载的项目数据（如 AI 生成的大屏）
onMounted(() => {
  // 如果 chartEditStore 已经有组件（如从 AI 生成流程直接跳转），不需要从 SessionStorage 恢复
  if (chartEditStore.getComponentList.length > 0) {
    console.log('[Chart Editor] 画布已有', chartEditStore.getComponentList.length, '个组件，跳过 SessionStorage 恢复')
    return
  }

  try {
    const id = fetchRouteParamsLocation()
    const storageList = getSessionStorage(StorageEnum.GO_CHART_STORAGE_LIST) as any[]
    if (storageList && id) {
      const found = storageList.find((item: any) => item.id === id)
      if (found && found.componentList && found.componentList.length > 0) {
        // 恢复画布配置
        if (found.editCanvasConfig) {
          Object.keys(found.editCanvasConfig).forEach((key: string) => {
            try {
              chartEditStore.setEditCanvasConfig(key as any, found.editCanvasConfig[key])
            } catch {}
          })
        }
        // 恢复请求配置
        if (found.requestGlobalConfig) {
          Object.assign(chartEditStore.requestGlobalConfig, found.requestGlobalConfig)
        }
        // 恢复组件列表
        chartEditStore.componentList = found.componentList
        chartEditStore.computedScale()
        console.log('[Chart Editor] 从 SessionStorage 恢复了', found.componentList.length, '个组件')
      }
    }
  } catch (e) {
    console.warn('[Chart Editor] SessionStorage 数据恢复失败:', e)
  }
})

const HeaderLeftBtn = loadAsyncComponent(() => import('./ContentHeader/headerLeftBtn/index.vue'))
const HeaderRightBtn = loadAsyncComponent(() => import('./ContentHeader/headerRightBtn/index.vue'))
const HeaderTitle = loadAsyncComponent(() => import('./ContentHeader/headerTitle/index.vue'))
const ContentLayers = loadAsyncComponent(() => import('./ContentLayers/index.vue'))
const ContentCharts = loadAsyncComponent(() => import('./ContentCharts/index.vue'))
const ContentConfigurations = loadAsyncComponent(() => import('./ContentConfigurations/index.vue'))
const ContentLoad = loadAsyncComponent(() => import('./ContentLoad/index.vue'))

// 右键
const {
  menuOptions,
  onClickOutSide,
  mousePosition,
  handleMenuSelect
} = useContextMenu()
</script>

<style lang="scss" scoped>
@include go("chart") {
  height: 100vh;
  width: 100vw;
  overflow: hidden;
  @include background-image("background-image");
}
</style>
