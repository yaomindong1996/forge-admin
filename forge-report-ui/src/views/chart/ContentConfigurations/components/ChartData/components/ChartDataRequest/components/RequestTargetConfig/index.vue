<template>
  <!-- 组件配置 -->
  <n-divider class="go-my-3" title-placement="left"></n-divider>
  
  <setting-item-box
    name="接口来源"
    class="go-mt-0"
  >
    <setting-item name="数据来源">
      <n-radio-group v-model:value="requestSourceValue" @update:value="handleRequestSourceChange">
        <n-radio-button value="internal">内部接口</n-radio-button>
        <n-radio-button value="external">外部接口</n-radio-button>
      </n-radio-group>
    </setting-item>
  </setting-item-box>

  <setting-item-box
    v-show="requestSourceValue === 'internal'"
    name="地址"
    :itemRightStyle="{
      gridTemplateColumns: '6fr 2fr'
    }"
    style="padding-right: 25px"
  >
    <setting-item name="请求方式 & URL 地址">
      <n-input-group>
        <n-select class="select-type-options" v-model:value="requestHttpType" :options="selectTypeOptions" />
        <n-input v-model:value.trim="requestUrl" :min="1" placeholder="请输入地址（去除前置URL）">
          <template #prefix>
            <n-text>{{ requestOriginUrl }}</n-text>
            <n-divider vertical />
          </template>
        </n-input>
      </n-input-group>
      <n-tooltip trigger="hover" v-if="isDev()">
        <template #trigger>
          <n-text class="dev-api-tip" depth="3">查看 mock 地址</n-text>
        </template>
        <ul class="go-pl-0">
          开发环境使用 mock 数据，请输入
          <li v-for="item in apiList" :key="item.value">
            <n-text type="info"> {{ item.value }} </n-text>
          </li>
        </ul>
      </n-tooltip>
    </setting-item>

    <setting-item name="更新间隔，为 0 只会初始化">
      <n-input-group>
        <n-input-number
          v-model:value.trim="requestInterval"
          class="select-time-number"
          min="0"
          :show-button="false"
          placeholder="默认使用全局数据"
        >
        </n-input-number>
        <n-select class="select-time-options" v-model:value="requestIntervalUnit" :options="selectTimeOptions" />
      </n-input-group>
    </setting-item>
  </setting-item-box>

  <div v-show="requestSourceValue === 'external'" class="external-request-config">
    <setting-item-box name="外部接口" class="go-mt-0">
      <setting-item name="选择接口">
        <n-select
          v-model:value="externalApiId"
          :options="externalApiOptions"
          :loading="externalApiLoading"
          placeholder="请选择外部接口"
          filterable
          clearable
          @focus="loadExternalApiOptions"
          @update:value="handleExternalApiChange"
        />
      </setting-item>

      <setting-item name="请求参数">
        <n-space align="center">
          <n-button size="small" @click="showParamModal = true">
            <template #icon>
              <n-icon><edit-icon /></n-icon>
            </template>
            配置参数
          </n-button>
          <n-text v-if="externalRequestParamsText" depth="3">
            {{ externalRequestParamsText }}
          </n-text>
        </n-space>
      </setting-item>

      <setting-item name="更新间隔，为 0 只会初始化">
        <n-input-group>
          <n-input-number
            v-model:value.trim="requestInterval"
            class="select-time-number"
            min="0"
            :show-button="false"
            placeholder="默认使用全局数据"
          >
          </n-input-number>
          <n-select class="select-time-options" v-model:value="requestIntervalUnit" :options="selectTimeOptions" />
        </n-input-group>
      </setting-item>
    </setting-item-box>
  </div>

  <setting-item-box v-show="requestSourceValue === 'internal'" name="选择方式" class="go-mt-0">
    <request-header :targetDataRequest="targetDataRequest"></request-header>
  </setting-item-box>

  <!-- 参数配置弹窗 -->
  <n-modal v-model:show="showParamModal" preset="dialog" title="配置外部接口参数" style="width: 500px;">
    <n-form>
      <n-form-item label="请求参数（JSON）">
        <n-input
          v-model:value="externalRequestParamsJson"
          type="textarea"
          :rows="8"
          placeholder="请输入 JSON 格式的参数配置"
        />
      </n-form-item>
    </n-form>
    <template #action>
      <n-space justify="end">
        <n-button @click="showParamModal = false">取消</n-button>
        <n-button type="primary" @click="handleSaveParams">确定</n-button>
      </n-space>
    </template>
  </n-modal>
</template>

<script setup lang="ts">
import { PropType, toRefs, ref, computed, onMounted, watch } from 'vue'
import { SettingItemBox, SettingItem } from '@/components/Pages/ChartItemSetting'
import { useTargetData } from '@/views/chart/ContentConfigurations/components/hooks/useTargetData.hook'
import { selectTypeOptions, selectTimeOptions } from '@/views/chart/ContentConfigurations/components/ChartData/index.d'
import { RequestConfigType } from '@/store/modules/chartEditStore/chartEditStore.d'
import { RequestHeader } from '../RequestHeader'
import { isDev } from '@/utils'
import { icon } from '@/plugins'
import { useMessage } from 'naive-ui'
import {
  graphUrl,
  chartDataUrl,
  chartSingleDataUrl,
  rankListUrl,
  scrollBoardUrl,
  numberFloatUrl,
  numberIntUrl,
  textUrl,
  imageUrl,
  radarUrl,
  heatMapUrl,
  scatterBasicUrl,
  mapUrl,
  capsuleUrl,
  wordCloudUrl,
  treemapUrl,
  threeEarth01Url,
  sankeyUrl,
  vchartBarDataUrl
} from '@/api/mock'
import { getExternalApiListApi, ExternalApiVO } from '@/api/external/api'

const props = defineProps({
  targetDataRequest: Object as PropType<RequestConfigType>
})

const message = useMessage()
const { EditIcon } = icon.ionicons5
const { chartEditStore } = useTargetData()
const { requestOriginUrl } = toRefs(chartEditStore.getRequestGlobalConfig)
const { requestInterval, requestIntervalUnit, requestHttpType, requestUrl } = toRefs(
  props.targetDataRequest as RequestConfigType
)

const requestSourceValue = ref<'internal' | 'external'>('internal')

const handleRequestSourceChange = (value: 'internal' | 'external') => {
  requestSourceValue.value = value
  const requestConfig = props.targetDataRequest as RequestConfigType
  requestConfig.requestSource = value
  if (value === 'external') {
    loadExternalApiOptions()
  }
}

const externalApiId = computed({
  get: () => (props.targetDataRequest as RequestConfigType).externalApiId || null,
  set: (val) => {
    (props.targetDataRequest as RequestConfigType).externalApiId = val
  }
})

const externalRequestParams = computed({
  get: () => (props.targetDataRequest as RequestConfigType).externalRequestParams || {},
  set: (val) => {
    (props.targetDataRequest as RequestConfigType).externalRequestParams = val
  }
})

const showParamModal = ref(false)
const externalRequestParamsJson = ref('')
const externalApiLoading = ref(false)
const externalApiOptions = ref<{ label: string; value: number; systemId: number; apiPath: string }[]>([])
const externalRequestParamsText = computed(() => {
  const params = externalRequestParams.value
  if (params && Object.keys(params).length > 0) {
    return `已配置 ${Object.keys(params).length} 个参数`
  }
  return ''
})

const apiList = [
  {
    value: `【图表】${chartDataUrl}`
  },
  {
    value: `【单数据图表】${chartSingleDataUrl}`
  },
  {
    value: `【文本】${textUrl}`
  },
  {
    value: `【0~100 整数】${numberIntUrl}`
  },
  {
    value: `【0~1小数】${numberFloatUrl}`
  },
  {
    value: `【图片地址】${imageUrl}`
  },
  {
    value: `【排名列表】${rankListUrl}`
  },
  {
    value: `【滚动表格】${scrollBoardUrl}`
  },
  {
    value: `【雷达】${radarUrl}`
  },
  {
    value: `【热力图】${heatMapUrl}`
  },
  {
    value: `【基础散点图】${scatterBasicUrl}`
  },
  {
    value: `【地图数据】${mapUrl}`
  },
  {
    value: `【胶囊柱图】${capsuleUrl}`
  },
  {
    value: `【词云】${wordCloudUrl}`
  },
  {
    value: `【树图】${treemapUrl}`
  },
  {
    value: `【三维地球】${threeEarth01Url}`
  },
{
    value: `【桑基图】${sankeyUrl}`
  },
  {
    value: `【关系图】${graphUrl}`
  },
  {
    value: `【VChart 柱状图】${vchartBarDataUrl}`
  }
]

const loadExternalApiOptions = async () => {
  if (externalApiLoading.value) return
  externalApiLoading.value = true
  try {
    const res = await getExternalApiListApi()
    externalApiOptions.value = (res.data || [])
      .filter(api => api.apiStatus === undefined || api.apiStatus === 1)
      .map(api => ({
        label: `${api.systemName || '未知系统'} - ${api.apiName} (${api.apiMethod || 'GET'} ${api.apiPath})`,
        value: api.id as number,
        systemId: api.systemId,
        apiPath: api.apiPath
      }))
  } catch (error) {
    message.error('加载外部接口列表失败')
  } finally {
    externalApiLoading.value = false
  }
}

const handleExternalApiChange = (value: number | null) => {
  externalApiId.value = value || null
  externalRequestParams.value = {}
  externalRequestParamsJson.value = ''
}

const handleSaveParams = () => {
  try {
    const params = JSON.parse(externalRequestParamsJson.value || '{}')
    externalRequestParams.value = params
    showParamModal.value = false
    message.success('参数配置已保存')
  } catch (error) {
    message.error('JSON 格式错误，请检查')
  }
}

onMounted(() => {
  const requestConfig = props.targetDataRequest as RequestConfigType
  requestSourceValue.value = requestConfig.requestSource || 'internal'
  requestConfig.requestSource = requestSourceValue.value
  loadExternalApiOptions()
  if (externalRequestParams.value) {
    externalRequestParamsJson.value = JSON.stringify(externalRequestParams.value, null, 2)
  }
})

watch(showParamModal, show => {
  if (show) {
    externalRequestParamsJson.value = JSON.stringify(externalRequestParams.value || {}, null, 2)
  }
})

watch(
  () => props.targetDataRequest?.requestSource,
  value => {
    requestSourceValue.value = value || 'internal'
  }
)
</script>

<style lang="scss" scoped>
.select-time-number {
  width: 100%;
}
.select-time-options {
  width: 100px;
}
.select-type-options {
  width: 120px;
}
.dev-api-tip {
  display: inline-block;
  margin-top: 6px;
  cursor: pointer;
}
.external-request-config {
  :deep(.go-config-item-box) {
    padding-right: 25px;
  }
}
</style>
