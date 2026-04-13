<template>
  <n-modal v-model:show="showRef" class="go-create-modal" @afterLeave="closeHandle">
    <n-space size="large">
      <n-card class="card-box" hoverable>
        <template #header>
          <n-text class="card-box-tite">{{ $t('project.create_tip') }}</n-text>
        </template>
        <template #header-extra>
          <n-text @click="closeHandle">
            <n-icon size="20">
              <component :is="CloseIcon"></component>
            </n-icon>
          </n-text>
        </template>
        <n-space class="card-box-content" justify="center">
          <n-button
            size="large"
            :disabled="item.disabled"
            v-for="item in typeList"
            :key="item.key"
            @click="item.handle ? item.handle() : btnHandle(item.key)"
          >
            <component :is="item.title"></component>
            <template #icon>
              <n-icon size="18">
                <component :is="item.icon"></component>
              </n-icon>
            </template>
          </n-button>
        </n-space>
        <template #action></template>
      </n-card>
    </n-space>
  </n-modal>
  <!-- AI 生成大屏对话框 -->
  <AIGenerateDialog v-model:show="showAIDialog" mode="create" @applied="onAIApplied" />
</template>

<script lang="ts" setup>
import { ref, watch, shallowRef } from 'vue'
import { icon } from '@/plugins'
import { PageEnum, ChartEnum } from '@/enums/pageEnum'
import { fetchPathByName, routerTurnByPath, renderLang, getUUID, setSessionStorage, getSessionStorage } from '@/utils'
import { StorageEnum } from '@/enums/storageEnum'
import { useChartEditStore } from '@/store/modules/chartEditStore/chartEditStore'
import { AIGenerateDialog } from '@/components/GoAI'
import { AIGenerateResponse } from '@/api/ai/ai.d'
import router from '@/router'

const { FishIcon, CloseIcon, SparklesIcon } = icon.ionicons5
const { StoreIcon, ObjectStorageIcon } = icon.carbon
const showRef = ref(false)
const showAIDialog = ref(false)

const emit = defineEmits(['close'])
const props = defineProps({
  show: Boolean
})

const typeList = shallowRef([
  {
    title: renderLang('ai.generate_dashboard'),
    key: 'ai-generate',
    icon: SparklesIcon,
    disabled: false,
    handle: () => {
      closeHandle()
      showAIDialog.value = true
    }
  },
  {
    title: renderLang('project.new_project'),
    key: ChartEnum.CHART_HOME_NAME,
    icon: FishIcon,
    disabled: false
  },
  {
    title: renderLang('project.my_template'),
    key: PageEnum.BASE_HOME_TEMPLATE_NAME,
    icon: ObjectStorageIcon,
    disabled: true
  },
  {
    title: renderLang('project.template_market'),
    key: PageEnum.BASE_HOME_TEMPLATE_MARKET_NAME,
    icon: StoreIcon,
    disabled: true
  }
])

watch(props, newValue => {
  showRef.value = newValue.show
})

// 关闭对话框
const closeHandle = () => {
  emit('close', false)
}

// 处理按钮点击
const btnHandle = (key: string) => {
  closeHandle()
  const id = getUUID()
  const path = fetchPathByName(ChartEnum.CHART_HOME_NAME, 'href')
  routerTurnByPath(path, [id], undefined, true)
}

// AI 生成完成后，将 chartEditStore 数据写入 SessionStorage，跳转编辑器
const onAIApplied = (_response: AIGenerateResponse) => {
  const id = getUUID()
  const chartEditStore = useChartEditStore()
  // 将 AI 生成的画布数据存入 SessionStorage，供编辑器加载
  const storageInfo = chartEditStore.getStorageInfo()
  const sessionStorageInfo = getSessionStorage(StorageEnum.GO_CHART_STORAGE_LIST) || []
  const existIndex = sessionStorageInfo.findIndex((e: { id: string }) => e.id === id)
  if (existIndex !== -1) {
    sessionStorageInfo.splice(existIndex, 1, { ...storageInfo, id })
  } else {
    sessionStorageInfo.push({ ...storageInfo, id })
  }
  setSessionStorage(StorageEnum.GO_CHART_STORAGE_LIST, sessionStorageInfo)
  // 关闭 AI 对话框
  showAIDialog.value = false
  // 在当前窗口跳转到编辑器（直接用 router.push，避免 fetchPathByName 返回带 # 的 href）
  router.push({ name: ChartEnum.CHART_HOME_NAME, params: { id } })
  console.log('[AI] 准备跳转编辑器, id:', id)
}
</script>
<style lang="scss" scoped>
$cardWidth: 570px;

@include go('create-modal') {
  position: fixed;
  top: 200px;
  left: 50%;
  transform: translateX(-50%);
  .card-box {
    width: $cardWidth;
    cursor: pointer;
    border: 1px solid rgba(0, 0, 0, 0);
    @extend .go-transition;
    &:hover {
      @include hover-border-color('hover-border-color');
    }
    &-tite {
      font-size: 14px;
    }
    &-content {
      padding: 0px 10px;
      width: 100%;
    }
  }
}
</style>
