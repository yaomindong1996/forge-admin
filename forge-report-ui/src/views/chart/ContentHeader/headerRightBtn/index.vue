<template>
  <n-space class="go-mt-0" :wrap="false">
    <n-button v-for="item in comBtnList" :key="item.title" :type="item.type" ghost @click="item.event">
      <template #icon>
        <component :is="item.icon"></component>
      </template>
      <span>{{ item.title }}</span>
    </n-button>
  </n-space>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { renderIcon, goDialog, fetchPathByName, routerTurnByPath, setSessionStorage, getSessionStorage, setLocalStorage, getLocalStorage } from '@/utils'
import { PreviewEnum } from '@/enums/pageEnum'
import { StorageEnum } from '@/enums/storageEnum'
import { useRoute } from 'vue-router'
import { useChartEditStore } from '@/store/modules/chartEditStore/chartEditStore'
import { syncData } from '../../ContentEdit/components/EditTools/hooks/useSyncUpdate.hook'
import { icon } from '@/plugins'
import { cloneDeep } from 'lodash'

const { BrowsersOutlineIcon, SendIcon, AnalyticsIcon } = icon.ionicons5
const chartEditStore = useChartEditStore()

const routerParamsInfo = useRoute()

// 预览
const previewHandle = () => {
  const path = fetchPathByName(PreviewEnum.CHART_PREVIEW_NAME, 'href')
  if (!path) return
  const { id } = routerParamsInfo.params
  // id 标识
  const previewId = typeof id === 'string' ? id : id[0]
  const storageInfo = chartEditStore.getStorageInfo()
  const sessionStorageInfo = getSessionStorage(StorageEnum.GO_CHART_STORAGE_LIST) || []

  if (sessionStorageInfo?.length) {
    const repeateIndex = sessionStorageInfo.findIndex((e: { id: string }) => e.id === previewId)
    // 重复替换
    if (repeateIndex !== -1) {
      sessionStorageInfo.splice(repeateIndex, 1, { id: previewId, ...storageInfo })
      setSessionStorage(StorageEnum.GO_CHART_STORAGE_LIST, sessionStorageInfo)
    } else {
      sessionStorageInfo.push({
        id: previewId,
        ...storageInfo
      })
      setSessionStorage(StorageEnum.GO_CHART_STORAGE_LIST, sessionStorageInfo)
    }
  } else {
    setSessionStorage(StorageEnum.GO_CHART_STORAGE_LIST, [{ id: previewId, ...storageInfo }])
  }
  // 跳转
  routerTurnByPath(path, [previewId], undefined, true)
}

// 发布
const sendHandle = () => {
  const { id } = routerParamsInfo.params
  const previewId = typeof id === 'string' ? id : id[0]
  const storageInfo = chartEditStore.getStorageInfo()

  // 1. 将数据保存到 localStorage（持久化，关闭浏览器后依然可访问）
  const localList = getLocalStorage(StorageEnum.GO_CHART_STORAGE_LIST) || []
  const localSession = getSessionStorage(StorageEnum.GO_CHART_STORAGE_LIST) || []

  // 同时更新 localStorage 和 sessionStorage
  const saveData = { id: previewId, ...storageInfo }

  // 更新 localStorage
  const localIdx = localList.findIndex((e: { id: string }) => e.id === previewId)
  if (localIdx !== -1) {
    localList.splice(localIdx, 1, saveData)
  } else {
    localList.push(saveData)
  }
  setLocalStorage(StorageEnum.GO_CHART_STORAGE_LIST, localList)

  // 更新 sessionStorage
  const sessionIdx = localSession.findIndex((e: { id: string }) => e.id === previewId)
  if (sessionIdx !== -1) {
    localSession.splice(sessionIdx, 1, saveData)
  } else {
    localSession.push(saveData)
  }
  setSessionStorage(StorageEnum.GO_CHART_STORAGE_LIST, localSession)

  // 2. 生成可分享的预览链接
  const previewPath = fetchPathByName(PreviewEnum.CHART_PREVIEW_NAME, 'href')
  if (!previewPath) {
    window['$message'].error('获取预览路径失败')
    return
  }
  const previewUrl = `${window.location.origin}${previewPath}/${previewId}`

  // 3. 复制链接到剪贴板
  goDialog({
    message: `发布成功！大屏将保存在本地浏览器中。\n\n预览链接：\n${previewUrl}`,
    positiveText: '复制链接',
    negativeText: '直接预览',
    onPositiveCallback: () => {
      navigator.clipboard.writeText(previewUrl).then(() => {
        window['$message'].success('链接已复制到剪贴板')
      }).catch(() => {
        // 如果剪贴板 API 不可用，打开预览
        routerTurnByPath(previewPath, [previewId], undefined, true)
      })
    },
    onNegativeCallback: () => {
      routerTurnByPath(previewPath, [previewId], undefined, true)
    }
  })
}

const btnList = [
  {
    select: true,
    title: '同步内容',
    type: 'primary',
    icon: renderIcon(AnalyticsIcon),
    event: syncData
  },
  {
    select: true,
    title: '预览',
    icon: renderIcon(BrowsersOutlineIcon),
    event: previewHandle
  },
  {
    select: true,
    title: '发布',
    icon: renderIcon(SendIcon),
    event: sendHandle
  }
]

const comBtnList = computed(() => {
  if (chartEditStore.getEditCanvas.isCodeEdit) {
    return btnList
  }
  const cloneList = cloneDeep(btnList)
  cloneList.shift()
  return cloneList
})
</script>

<style lang="scss" scoped>
.align-center {
  margin-top: -4px;
}
</style>
