<template>
  <n-timeline class="go-chart-configurations-timeline">
    <!-- 处理 echarts 的数据映射 -->
    <n-timeline-item v-if="isCharts && dimensionsAndSource" type="info" :title="TimelineTitleEnum.MAPPING">
      <n-table striped>
        <thead>
          <tr>
            <th v-for="item in tableTitle" :key="item">{{ item }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(item, index) in dimensionsAndSource" :key="index">
            <td>{{ item.field }}</td>
            <td>{{ item.mapping }}</td>
            <td>
              <n-space v-if="item.result === 0">
                <n-badge dot type="success"></n-badge>
                <n-text>无</n-text>
              </n-space>
              <n-space v-else>
                <n-badge dot :type="item.result === 1 ? 'success' : 'error'"></n-badge>
                <n-text>匹配{{ item.result === 1 ? '成功' : '失败' }}</n-text>
              </n-space>
            </td>
          </tr>
        </tbody>
      </n-table>
    </n-timeline-item>
    <!-- 处理 vcharts 的数据映射 -->
    <n-timeline-item v-if="isVChart" type="info" :title="TimelineTitleEnum.MAPPING">
      <n-table striped>
        <thead>
          <tr>
            <th v-for="item in vchartTableTitle" :key="item">{{ item }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in fieldList" :key="item.field">
            <td>
              <n-ellipsis style="width: 70px; max-width: 240px">
                {{ item.field }}
              </n-ellipsis>
            </td>
            <td v-if="isArray(item.mapping)">
              <n-space :size="4" vertical>
                <n-input
                  v-for="(mappingItem, index) in item.mapping"
                  :key="index"
                  v-model:value="item.mapping[index]"
                  type="tiny"
                  size="small"
                  placeholder="输入字段"
                  @change="() => (item.result = matchingHandle(item.mapping[index]))"
                />
              </n-space>
            </td>
            <td v-else>
              <n-input v-model:value="item.mapping" type="text" size="small" placeholder="小" />
            </td>
            <!-- <td>
              <n-space style="width: 70px" :size="4">
                <n-badge dot :type="item.result === 1 ? 'success' : 'error'"></n-badge>
                <n-text>匹配{{ item.result === 1 ? '成功' : '失败' }}</n-text>
              </n-space>
            </td> -->
          </tr>
        </tbody>
      </n-table>
    </n-timeline-item>
    <n-timeline-item v-show="filterShow" color="#97846c" :title="TimelineTitleEnum.FILTER">
      <n-space :size="18" vertical>
        <n-text depth="3">过滤器默认处理接口返回值的「data」字段</n-text>
        <chart-data-monaco-editor></chart-data-monaco-editor>
      </n-space>
    </n-timeline-item>
    <n-timeline-item type="success" :title="TimelineTitleEnum.CONTENT">
      <n-space vertical>
        <n-space class="source-btn-box">
          <n-button class="sourceBtn-item" :disabled="noData" @click="openOnlineEditHandle">
            <template #icon>
              <n-icon>
                <EditIcon />
              </n-icon>
            </template>
            编辑
          </n-button>
          <n-upload
            v-model:file-list="uploadFileListRef"
            :show-file-list="false"
            :customRequest="customRequest"
            @before-upload="beforeUpload"
          >
            <n-space>
              <n-button v-if="!ajax" class="sourceBtn-item" :disabled="noData">
                <template #icon>
                  <n-icon>
                    <document-add-icon />
                  </n-icon>
                </template>
                导入（json / txt）
              </n-button>
            </n-space>
          </n-upload>
          <div>
            <n-button class="sourceBtn-item" :disabled="noData" @click="download">
              <template #icon>
                <n-icon>
                  <document-download-icon />
                </n-icon>
              </template>
              下载
            </n-button>
            <n-tooltip trigger="hover">
              <template #trigger>
                <n-icon class="go-ml-1" size="21" :depth="3">
                  <help-outline-icon></help-outline-icon>
                </n-icon>
              </template>
              <span>点击【下载】查看完整数据</span>
            </n-tooltip>
          </div>
        </n-space>
        <n-card size="small">
          <n-code :code="toString(source)" language="json"></n-code>
        </n-card>
      </n-space>
    </n-timeline-item>
  </n-timeline>
  <!-- 编辑数据 -->
  <n-modal
    class="go-online-edit go-background-filter"
    :title="'组件数据在线编辑 — ' + targetData.chartConfig.title"
    preset="card"
    size="small"
    style="width: 800px"
    v-model:show="showRef"
    transform-origin="center"
    :mask-closable="false"
    :bordered="true"
    @afterLeave="closeOlineEditHandle"
    content-style="padding: 0;"
  >
    <n-divider style="margin: 4px 0 10px" />
    <monaco-editor
      v-model:modelValue="editorCode"
      height="70vh"
      :language="targetData.chartConfig.chartKey === 'VHtmlCommon' ? 'html' : 'json'"
    />
    <template #action>
      <n-space justify="space-between">
        <div class="go-flex-items-center">
          <n-tag :bordered="false" type="primary" style="border-radius: 5px">
            <template #icon>
              <n-icon :component="DocumentTextIcon" />
            </template>
            说明
          </n-tag>
          <n-space :wrap-item="false" vertical>
            <n-text class="go-ml-2" depth="3" style="font-size: 1">
              1. 格式化代码：
              <n-tag :bordered="false" type="warning" style="font-size: 12px">( shift + alt + f ) </n-tag>
              <n-divider vertical />
              <n-tag :bordered="false" type="warning" style="font-size: 12px">右键 -> Format Document</n-tag>
            </n-text>
            <n-text class="go-ml-2" depth="3" style="font-size: 1; text-align: left">
              2. 字符串需要用英文双引号包裹，否则会报错
            </n-text>
          </n-space>
        </div>
        <n-space>
          <n-button class="go-px-4" :focusable="false" @click="closeOlineEditHandle">取消</n-button>
          <n-button class="go-px-4" type="primary" @click="saveOlineEditHandle"> 保存 </n-button>
        </n-space>
      </n-space>
    </template>
  </n-modal>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ChartFrameEnum } from '@/packages/index.d'
import { RequestDataTypeEnum } from '@/enums/httpEnum'
import { icon } from '@/plugins'
import { DataResultEnum, TimelineTitleEnum } from '../../index.d'
import { ChartDataMonacoEditor } from '../ChartDataMonacoEditor'
import { useFile } from '../../hooks/useFile.hooks'
import { useTargetData } from '../../../hooks/useTargetData.hook'
import { toString, isArray, goDialog } from '@/utils'
// @ts-ignore
import MonacoEditor from '@/components/Pages/MonacoEditor/index.vue'

const { targetData } = useTargetData()
defineProps({
  show: {
    type: Boolean,
    required: false
  },
  ajax: {
    type: Boolean,
    required: true
  }
})

// 表格标题
const tableTitle = ['字段', '映射', '状态']
const vchartTableTitle = ['字段', '接口映射字段']

const { HelpOutlineIcon, DocumentTextIcon } = icon.ionicons5
const { DocumentAddIcon, DocumentDownloadIcon, EditIcon } = icon.carbon

const source = ref()
const dimensions = ref()
const dimensionsAndSource = ref()
const noData = ref(false)
const showRef = ref(false)
// 编辑的代码
const editorCode = ref('')

// 映射列表, 注意内部的mapping是响应式的，上方需要修改
const fieldList = ref<
  Array<{
    field: string
    mapping: string[]
    result: DataResultEnum
  }>
>([])

const { uploadFileListRef, customRequest, beforeUpload, download } = useFile(targetData)

// 是否展示过滤器
const filterShow = computed(() => {
  return targetData.value.request.requestDataType !== RequestDataTypeEnum.STATIC
})

// 是支持 dataset 的图表类型
const isCharts = computed(() => {
  return targetData.value.chartConfig.chartFrame === ChartFrameEnum.ECHARTS
})
// 是支持 vchart 的图表类型
const isVChart = computed(() => {
  return targetData.value.chartConfig.chartFrame === ChartFrameEnum.VCHART
})

// 处理映射列表状态结果
const matchingHandle = (mapping: string) => {
  let res = DataResultEnum.SUCCESS
  for (let i = 0; i < source.value.length; i++) {
    if (source.value[i][mapping] === undefined) {
      res = DataResultEnum.FAILURE
      return res
    }
  }
  return DataResultEnum.SUCCESS
}

// 处理映射列表
const dimensionsAndSourceHandle = () => {
  try {
    // 去除首项数据轴标识
    return dimensions.value.map((dimensionsItem: string, index: number) => {
      return index === 0
        ? {
            // 字段
            field: '通用标识',
            // 映射
            mapping: dimensionsItem,
            // 结果
            result: DataResultEnum.NULL
          }
        : {
            field: `数据项-${index}`,
            mapping: dimensionsItem,
            result: matchingHandle(dimensionsItem)
          }
    })
  } catch (error) {
    return []
  }
}

// 处理 vchart 映射列表
const initFieldListHandle = () => {
  if (targetData.value?.option) {
    fieldList.value = []
    // 所有名称，找到其中中 Field 结尾 的 key 和值
    for (const key in targetData.value.option) {
      if (key.endsWith('Field')) {
        const value = targetData.value.option[key]
        targetData.value.option[key] = value
        const item = {
          field: key,
          mapping: value,
          result: DataResultEnum.SUCCESS
        }
        if (item.mapping === undefined) {
          item.result = DataResultEnum.FAILURE
        }
        fieldList.value.push(item)
      }
    }
  }
}

// 打开在线编辑
const openOnlineEditHandle = () => {
  showRef.value = true
  if (targetData.value.chartConfig.chartKey !== 'VHtmlCommon') {
    editorCode.value = JSON.stringify(source.value, null, 2)
  } else {
    editorCode.value = source.value
  }
}

// 取消在线编辑
const closeOlineEditHandle = () => {
  showRef.value = false
}

// 取消在线编辑
const saveOlineEditHandle = () => {
  // 设置
  const setDataHandle = (newData: any) => {
    targetData.value.option.dataset = newData
  }
  goDialog({
    message: '是否保存编辑后的数据？',
    onPositiveCallback: () => {
      try {
        let jsonData = editorCode.value
        if (targetData.value.chartConfig.chartKey !== 'VHtmlCommon') {
          jsonData = JSON.parse(jsonData)
        }
        if (typeof jsonData !== typeof source.value) {
          goDialog({
            message: '当前数据类型与原类型不相同, 是否强制应用数据?',
            onPositiveCallback: () => {
              try {
                setDataHandle(jsonData)
                editorCode.value = ''
                window['$message'].success('保存成功')
                closeOlineEditHandle()
              } catch (error) {
                window['$message'].error('内容应用错误, 请检查格式是否正确')
              }
            }
          })
        } else {
          try {
            setDataHandle(jsonData)
            editorCode.value = ''
            window['$message'].success('保存成功')
            closeOlineEditHandle()
          } catch (error) {
            window['$message'].error('内容应用错误, 请检查格式是否正确')
          }
        }
      } catch (error) {
        console.log(error)
        window['$message'].error('内容应用错误, 请检查格式是否正确')
      }
    }
  })
}

watch(
  () => targetData.value?.option?.dataset,
  (
    newData?: {
      source: any
      dimensions: any
    } | null
  ) => {
    noData.value = false
    if (newData && targetData?.value?.chartConfig?.chartFrame === ChartFrameEnum.ECHARTS) {
      // 只有 DataSet 数据才有对应的格式
      source.value = newData
      if (isCharts.value) {
        dimensions.value = newData.dimensions
        dimensionsAndSource.value = dimensionsAndSourceHandle()
      }
    } else if (newData && targetData?.value?.chartConfig?.chartFrame === ChartFrameEnum.VCHART) {
      source.value = newData
      initFieldListHandle()
    } else if (newData !== undefined && newData !== null) {
      dimensionsAndSource.value = null
      source.value = newData
      fieldList.value = []
    } else {
      noData.value = true
      source.value = '此组件无数据源'
    }
    if (isArray(newData)) {
      dimensionsAndSource.value = null
    }
  },
  {
    immediate: true
  }
)
</script>

<style lang="scss" scoped>
@include go('chart-configurations-timeline') {
  @include deep() {
    pre {
      white-space: pre-wrap;
      word-wrap: break-word;
    }
  }
  .source-btn-box {
    margin-top: 10px !important;
  }
}
</style>
