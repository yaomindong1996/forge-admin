<template>
  <setting-item name="间距">
    <n-input v-model:value="paddingArray" size="small" @update:value="updateHandle"/>
  </setting-item>
</template>

<script setup lang="ts">
import type { ICartesianTitle } from '@visactor/vchart/esm/component/axis'
import { forEach, isNumber } from 'lodash'
import { SettingItem } from '@/components/Pages/ChartItemSetting'
import { PropType, ref } from 'vue'

const props = defineProps({
  axis: {
    type: Object as PropType<ICartesianTitle>,
    required: true
  }
})

// 解析间距
const paddingInit = (padding: ICartesianTitle['padding']) => {
  const arr = [0, 0, 0, 0]
  if (!padding) {
    return arr
  }
  if (isNumber(padding)) {
    arr.forEach((item, index) => {
      arr[index] = padding
    })
  } else if (Array.isArray(padding)) {
    if (padding.length === 1) {
      arr.forEach((item, index) => {
        arr[index] = padding[0]
      })
    } else if (padding.length === 2) {
      arr[0] = padding[0]
      arr[1] = padding[1]
      arr[2] = padding[0]
      arr[3] = padding[1]
    } else if (padding.length === 3) {
      arr[0] = padding[0]
      arr[1] = padding[1]
      arr[2] = padding[2]
      arr[3] = padding[1]
    } else if (padding.length === 4) {
      arr[0] = padding[0]
      arr[1] = padding[1]
      arr[2] = padding[2]
      arr[3] = padding[3]
    }
  }

  // 转成字符串，逗号分隔
  return arr.map(item => item.toString()).join(',')
}

// 间距处理
const paddingArray = ref(paddingInit(props.axis.padding))

// 字符串转成数组
const paddingArrayToNumber = (padding: string) => {
  const arr = padding.split(',').map(item => parseFloat(item.trim()))
  if (arr.length === 1) {
    return [arr[0], arr[0], arr[0], arr[0]]
  } else if (arr.length === 2) {
    return [arr[0], arr[1], arr[0], arr[1]]
  } else if (arr.length === 3) {
    return [arr[0], arr[1], arr[2], arr[1]]
  } else if (arr.length === 4) {
    return arr
  }
  return [0, 0, 0, 0]
}

const updateHandle = (value: string) => {
  const padding = paddingArrayToNumber(value)
  forEach(padding, (item, index) => {
    if (isNaN(item)) {
      padding[index] = 0
    }
  })
  props.axis.padding = padding
}
</script>
