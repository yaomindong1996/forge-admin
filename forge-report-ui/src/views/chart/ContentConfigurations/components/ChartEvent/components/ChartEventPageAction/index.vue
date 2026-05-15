<template>
  <n-collapse-item title="页面跳转" name="3">
    <template #header-extra>
      <n-button type="primary" tertiary size="small" @click.stop="addAction">
        <template #icon>
          <n-icon>
            <add-icon />
          </n-icon>
        </template>
        新增
      </n-button>
    </template>

    <div v-if="!pageActions.length" class="no-data go-flex-center">
      <img :src="noData" alt="暂无数据" />
      <n-text :depth="3">暂无页面跳转</n-text>
    </div>

    <n-card
      v-for="(item, index) in pageActions"
      :key="item.id"
      class="n-card-shallow page-action-card"
      size="small"
    >
      <n-space justify="space-between" align="center">
        <n-space align="center" :size="6">
          <n-tag :bordered="false" type="primary" size="small">跳转 {{ index + 1 }}</n-tag>
          <n-tag v-if="!isValidTarget(item.targetPageId)" :bordered="false" type="error" size="small">
            目标页面已失效
          </n-tag>
        </n-space>
        <n-button type="error" text size="small" @click="deleteAction(item.id)">
          <template #icon>
            <n-icon>
              <close-icon />
            </n-icon>
          </template>
        </n-button>
      </n-space>

      <n-divider />

      <setting-item-box name="触发事件" :alone="true">
        <n-select v-model:value="item.trigger" size="tiny" :options="triggerOptions" />
      </setting-item-box>

      <setting-item-box name="目标页面" :alone="true">
        <n-select
          v-model:value="item.targetPageId"
          size="tiny"
          filterable
          clearable
          placeholder="选择项目内页面"
          :options="pageOptions"
        />
      </setting-item-box>

      <setting-item-box name="切换动画" :alone="true">
        <n-select
          v-model:value="item.transition"
          size="tiny"
          clearable
          placeholder="跟随项目默认"
          :options="transitionOptions"
        />
      </setting-item-box>
    </n-card>
  </n-collapse-item>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import { SettingItemBox } from '@/components/Pages/ChartItemSetting'
import { BaseEvent, ComponentActionType } from '@/enums/eventEnum'
import { getUUID } from '@/utils'
import { icon } from '@/plugins'
import noData from '@/assets/images/canvas/noData.png'
import { useTargetData } from '../../../hooks/useTargetData.hook'
import type { ComponentAction, ReportPageTransition } from '@/store/modules/chartEditStore/chartEditStore.d'

const { CloseIcon, AddIcon } = icon.ionicons5
const { targetData, chartEditStore } = useTargetData()

const triggerOptions = [
  { label: '单击', value: BaseEvent.ON_CLICK },
  { label: '双击', value: BaseEvent.ON_DBL_CLICK }
]

const transitionOptions: Array<{ label: string; value: ReportPageTransition }> = [
  { label: '无动画', value: 'none' },
  { label: '淡入淡出', value: 'fade' },
  { label: '左滑', value: 'slide-left' },
  { label: '右滑', value: 'slide-right' },
  { label: '缩放', value: 'zoom' }
]

const ensureActions = () => {
  targetData.value.events.actions = targetData.value.events.actions || []
  return targetData.value.events.actions
}

const pageActions = computed(() => {
  return ensureActions().filter(action => action.type === ComponentActionType.GO_PAGE)
})

const pageOptions = computed(() => {
  return chartEditStore.getProjectPages.map(page => ({
    label: `${page.name}${page.id === chartEditStore.getHomePageId ? '（首页）' : ''}`,
    value: page.id
  }))
})

const isValidTarget = (pageId?: string) => {
  return !!pageId && chartEditStore.getProjectPages.some(page => page.id === pageId)
}

const getDefaultTargetPageId = () => {
  return chartEditStore.getProjectPages.find(page => page.id !== chartEditStore.getActivePageId)?.id
    || chartEditStore.getHomePageId
    || chartEditStore.getProjectPages[0]?.id
    || ''
}

const addAction = () => {
  const action: ComponentAction = {
    id: getUUID(),
    trigger: BaseEvent.ON_CLICK,
    type: 'goPage',
    targetPageId: getDefaultTargetPageId(),
    transition: undefined,
    params: []
  }
  ensureActions().push(action)
}

const deleteAction = (id: string) => {
  targetData.value.events.actions = ensureActions().filter(action => action.id !== id)
}
</script>

<style lang="scss" scoped>
.page-action-card {
  :deep(.n-divider) {
    margin: 10px 0;
  }
}

.no-data {
  flex-direction: column;
  width: 100%;
  img {
    width: 120px;
  }
}
</style>
