<template>
  <div class="go-edit">
    <n-layout>
      <n-layout-header class="go-edit-header go-px-5 go-flex-items-center" bordered>
        <div>
          <n-text class="go-edit-title go-mr-4">页面在线编辑器</n-text>
          <n-button v-if="showOpenFilePicker" class="go-mr-3" size="medium" @click="importJSON">
            <template #icon>
              <n-icon>
                <download-icon></download-icon>
              </n-icon>
            </template>
            导入
          </n-button>
        </div>
        <n-space>
          <!-- 暂时关闭 -->
          <!-- <n-tag :bordered="false" type="warning"> 「页面失焦保存」 </n-tag> -->
          <n-tag :bordered="false" type="warning"> 「Ctrl + S 更新视图」 </n-tag>
          <n-button v-if="showOpenFilePicker" class="go-mr-3" size="medium" @click="updateSync">
            <template #icon>
              <n-icon>
                <analytics-icon></analytics-icon>
              </n-icon>
            </template>
            保存
          </n-button>
        </n-space>
      </n-layout-header>
      <n-layout-content>
        <monaco-editor
          v-model:modelValue="content"
          language="json"
          :editorOptions="{
            lineNumbers: 'on',
            minimap: { enabled: true }
          }"
        />
      </n-layout-content>
    </n-layout>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { MonacoEditor } from '@/components/Pages/MonacoEditor/index'
import { SavePageEnum } from '@/enums/editPageEnum'
import { getSessionStorageInfo } from '../preview/utils'
import { setSessionStorage, JSONStringify, JSONParse, setTitle, goDialog } from '@/utils'
import { StorageEnum } from '@/enums/storageEnum'
import { icon } from '@/plugins'
import type { ChartEditStorageType } from '../preview'

const { ChevronBackOutlineIcon, DownloadIcon, AnalyticsIcon } = icon.ionicons5
const showOpenFilePicker: Function = (window as any).showOpenFilePicker
const content = ref('')

window['$message'].warning('请不要刷新此窗口！')

// 从sessionStorage 获取数据
async function getDataBySession() {
  const localStorageInfo: ChartEditStorageType = (await getSessionStorageInfo()) as unknown as ChartEditStorageType
  setTitle(`编辑-${localStorageInfo.editCanvasConfig.projectName}`)
  content.value = JSONStringify(localStorageInfo)
}
setTimeout(getDataBySession)

// 返回父窗口
function back() {
  window.opener.name = Date.now()
  window.open(window.opener.location.href, window.opener.name)
}

// 导入json文本
function importJSON() {
  goDialog({
    message: '导入数据将覆盖内容，此操作不可撤回，是否继续？',
    isMaskClosable: true,
    transformOrigin: 'center',
    onPositiveCallback: async () => {
      try {
        const files = await showOpenFilePicker()
        const file = await files[0].getFile()
        const fr = new FileReader()
        fr.readAsText(file)
        fr.onloadend = () => {
          content.value = (fr.result || '').toString()
        }
        window['$message'].success('导入成功！')
      } catch (error) {
        window['$message'].error('导入失败，请检查文件是否损坏！')
        console.log(error)
      }
    }
  })
}

// 同步数据编辑页
window.opener.addEventListener(SavePageEnum.CHART, (e: any) => {
  window['$message'].success('正在进行更新...')
  setSessionStorage(StorageEnum.GO_CHART_STORAGE_LIST, [e.detail])
  content.value = JSONStringify(e.detail)
})

// 保存按钮同步数据
document.addEventListener('keydown', function (e) {
  if (e.keyCode == 83 && (navigator.platform.match('Mac') ? e.metaKey : e.ctrlKey)) {
    e.preventDefault()
    updateSync()
  }
})

// 失焦保存（暂时关闭）
// addEventListener('blur', updateSync)

// 同步更新
async function updateSync() {
  if (!window.opener) {
    return window['$message'].error('源窗口已关闭，视图同步失败！')
  }
  goDialog({
    message: '是否覆盖源视图内容? 此操作不可撤！',
    isMaskClosable: true,
    transformOrigin: 'center',
    onPositiveCallback: () => {
      try {
        const detail = JSONParse(content.value)
        delete detail.id
        // 保持id不变
        window.opener.dispatchEvent(new CustomEvent(SavePageEnum.JSON, { detail }))
        window['$message'].success('正在同步内容...')
      } catch (e) {
        window['$message'].error('内容格式有误')
        console.log(e)
      }
    }
  })
}

// 关闭页面发送关闭事件
window.onbeforeunload = () => {
  if (window.opener) {
    window.opener.dispatchEvent(new CustomEvent(SavePageEnum.CLOSE))
  }
}
</script>

<style lang="scss" scoped>
.go-edit {
  display: flex;
  flex-direction: column;
  height: 100vh;
  .go-edit-header {
    display: flex;
    align-items: center;
    height: 60px;
    justify-content: space-between;
    .go-edit-title {
      position: relative;
      bottom: 3px;
      font-size: 18px;
      font-weight: bold;
    }
  }
  @include deep() {
    .go-editor-area {
      height: calc(100vh - 60px) !important;
    }
  }
}
</style>
