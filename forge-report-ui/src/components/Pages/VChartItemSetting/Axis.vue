<template>
  <collapse-item :name="axis.name">
    <template #header>
      <n-switch v-model:value="axis.visible" size="small"></n-switch>
    </template>
    <setting-item-box name="单位">
      <setting-item name="可见性">
        <n-space>
          <n-switch v-model:value="axis.unit.visible" size="small"></n-switch>
        </n-space>
      </setting-item>
      <setting-item name="内容">
        <n-input v-model:value="axis.unit.text" size="small"></n-input>
      </setting-item>
      <FontStyle :style="toRefs(axis.unit.style)"></FontStyle>
    </setting-item-box>
    <setting-item-box name="轴标签">
      <setting-item v-if="axis.label" name="可见性">
        <n-space>
          <n-switch v-model:value="axis.label.visible" size="small"></n-switch>
        </n-space>
      </setting-item>
      <setting-item v-if="axis.label" name="角度">
        <n-input-number v-model:value="axis.label.style.angle" :min="0" :max="360" size="small" />
      </setting-item>
      <FontStyle v-if="axis.label" :style="toRefs(axis.label.style)"></FontStyle>
    </setting-item-box>
    <setting-item-box name="轴标题">
      <setting-item name="可见性">
        <n-space>
          <n-switch v-model:value="axis.title.visible" size="small"></n-switch>
        </n-space>
      </setting-item>
      <setting-item name="内容">
        <n-input v-model:value="axis.title.style.text" size="small"></n-input>
      </setting-item>
      <setting-item name="位置">
        <n-select v-model:value="axis.title.position" :options="legendsConfig.position" size="small" />
      </setting-item>
      <setting-item name="角度">
        <n-input-number v-model:value="axis.title.angle" :min="0" :max="360" size="small" />
      </setting-item>
      <FontStyle :style="toRefs(axis.title.style)"></FontStyle>
    </setting-item-box>
    <setting-item-box name="轴线">
      <setting-item name="可见性">
        <n-space>
          <n-switch v-model:value="axis.domainLine.visible" size="small"></n-switch>
        </n-space>
      </setting-item>
      <setting-item name=""> </setting-item>
      <setting-item name="粗细">
        <n-input-number v-model:value="axis.domainLine.style.lineWidth" :min="0" size="small" />
      </setting-item>
      <setting-item name="颜色">
        <n-color-picker v-model:value="axis.domainLine.style.stroke" size="small" />
      </setting-item>
    </setting-item-box>
    <setting-item-box name="网格线">
      <setting-item name="可见性">
        <n-space>
          <n-switch v-model:value="axis.grid.visible" size="small"></n-switch>
        </n-space>
      </setting-item>
      <setting-item name="开启虚线">
        <n-space>
          <n-switch v-model:value="isLineDashRef" size="small" @update:value="changeLineDash"></n-switch>
        </n-space>
      </setting-item>
      <setting-item name="粗细">
        <n-input-number v-model:value="axis.grid.style.lineWidth" :min="0" size="small" />
      </setting-item>
      <setting-item name="颜色">
        <n-color-picker v-model:value="axis.grid.style.stroke" size="small" />
      </setting-item>
    </setting-item-box>
  </collapse-item>
</template>

<script setup lang="ts">
import { PropType, ref, toRefs } from 'vue'
import FontStyle from './common/FontStyle.vue'
import { vChartGlobalThemeJsonType } from '@/settings/vchartThemes/index'
import { CollapseItem, SettingItemBox, SettingItem } from '@/components/Pages/ChartItemSetting'
import { legendsConfig } from '@/packages/chartConfiguration/vcharts/index'

const props = defineProps({
  axis: {
    type: Object as PropType<vChartGlobalThemeJsonType>,
    required: true
  }
})

// 判断是否是虚线
const isDash = (data: undefined | Array<number>) => {
  if (!data || data.length === 0 || data[0] === 0) return false
  return true
}

// 虚线
const isLineDashRef = ref(isDash(props.axis.grid.style.lineDash))

const changeLineDash = (data: boolean) => {
  if (data) {
    props.axis.grid.style.lineDash = [4, 4] // 设置为虚线
  } else {
    props.axis.grid.style.lineDash = [0] // 设置为实线
  }
}
</script>
