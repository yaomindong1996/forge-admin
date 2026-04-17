<template>
  <!-- vCharts 全局设置 -->
  <VChartGlobalSetting :optionData="optionData"></VChartGlobalSetting>
  <!-- 饼图配制 -->
  <CollapseItem name="饼图" expanded>
    <!-- <SettingItemBox name="动画">
      <SettingItem>
        <n-space>
          <n-switch v-model:value="animationRef" size="small" @update:value="animationHandle"></n-switch>
          <n-text>开启动画</n-text>
        </n-space>
      </SettingItem>
    </SettingItemBox> -->
    <SettingItemBox name="图形">
      <setting-item name="内圈范围">
        <n-input-number v-model:value="optionData.innerRadius" :step="0.1" :min="0" size="small"></n-input-number>
      </setting-item>
      <setting-item name="外圈范围">
        <n-input-number v-model:value="optionData.outerRadius" :step="0.1" :min="0" size="small"></n-input-number>
      </setting-item>
      <setting-item name="中心轴X">
        <n-input v-model:value="optionData.centerX" :step="1" :min="0" size="small"></n-input>
      </setting-item>
      <setting-item name="中心轴Y">
        <n-input v-model:value="optionData.centerY" :step="1" :min="0" size="small"></n-input>
      </setting-item>
    </SettingItemBox>
    <SettingItemBox name="标签" v-if="optionData.label">
      <SettingItem>
        <n-space>
          <n-switch v-model:value="optionData.label.visible" size="small"></n-switch>
          <n-text>展示标签</n-text>
        </n-space>
      </SettingItem>
      <SettingItem>
        <n-space>
          <n-switch v-model:value="optionData.label.line.visible" size="small"></n-switch>
          <n-text>引导线</n-text>
        </n-space>
      </SettingItem>
      <SettingItem name="位置">
        <n-select v-model:value="optionData.label.position" :options="labelConfig.position" size="small" />
      </SettingItem>
      <FontStyle :style="toRefs(optionData.label.style)"></FontStyle>
    </SettingItemBox>
    <SettingItemBox name="分段样式">
      <setting-item name="纹理类型">
        <n-select v-model:value="optionData.pie.style.texture" :options="styleConfig.texture" size="small"></n-select>
      </setting-item>
      <setting-item name="圆角大小">
        <n-input-number v-model:value="optionData.pie.style.cornerRadius" size="small" :min="0"></n-input-number>
      </setting-item>
      <setting-item name="描边宽度">
        <n-input-number
          v-model:value="optionData.pie.style.outerBorder.lineWidth"
          size="small"
          :min="0"
        ></n-input-number>
      </setting-item>
      <setting-item name="颜色">
        <n-color-picker v-model:value="optionData.pie.style.outerBorder.stroke" size="small" />
      </setting-item>
      <setting-item name="外描边距离">
        <n-input-number
          v-model:value="optionData.pie.style.outerBorder.distance"
          size="small"
          :min="0"
        ></n-input-number>
      </setting-item>
      <setting-item name="描边透明度">
        <n-input-number
          v-model:value="optionData.pie.style.outerBorder.strokeOpacity"
          :step="0.1"
          size="small"
          :min="0"
        ></n-input-number>
      </setting-item>
    </SettingItemBox>
    <SettingItemBox name="内环形">
      <setting-item name="可见性">
        <n-space>
          <n-switch v-model:value="extensionMarkRef" size="small" @update:value="markerHandle"></n-switch>
        </n-space>
      </setting-item>
    </SettingItemBox>
  </CollapseItem>
</template>

<script setup lang="ts">
import { PropType, ref, toRefs } from 'vue'
import { VChartGlobalSetting } from '@/components/Pages/VChartItemSetting'
import FontStyle from '@/components/Pages/VChartItemSetting/common/FontStyle.vue'
import type { vChartGlobalThemeJsonType } from '@/settings/vchartThemes/index'
import { CollapseItem, SettingItemBox, SettingItem } from '@/components/Pages/ChartItemSetting'
import { labelConfig, styleConfig } from '@/packages/chartConfiguration/vcharts/index'

const props = defineProps({
  optionData: {
    type: Object as PropType<vChartGlobalThemeJsonType>,
    required: true
  }
})

const animationRef = ref(props.optionData.animationNormal && Object.keys(props.optionData.animationNormal)?.length > 0)
const extensionMarkRef = ref(!!props.optionData?.extensionMark)

const markerHandle = (value: boolean) => {
  if (value) {
    props.optionData.extensionMark = []
  } else {
    delete props.optionData.extensionMark
  }
}

const animationHandle = (value: boolean) => {
  if (value) {
    props.optionData.animationNormal = {
      pie: [
        {
          loop: true,
          startTime: 800,
          oneByOne: true,
          timeSlices: [
            {
              effects: {
                channel: {
                  outerRadius: {
                    to: props.optionData.outerRadius * 100 + 10
                  }
                },
                easing: 'linear'
              },
              duration: 500
            },
            {
              effects: {
                channel: {
                  outerRadius: {
                    to: props.optionData.outerRadius * 100 + 10
                  }
                },
                easing: 'linear'
              },
              duration: 500
            },
            {
              effects: {
                channel: {
                  outerRadius: {
                    to: props.optionData.outerRadius * 100
                  }
                }
              },
              duration: 500
            }
          ]
        }
      ]
    }
  } else {
    props.optionData.animationNormal = {}
  }
}
</script>
