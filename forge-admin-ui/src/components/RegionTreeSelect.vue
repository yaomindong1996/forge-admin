<!--
  行政区划树选择组件（共用）
  - 一次性加载指定 rootCode 下的完整区划树（含虚拟组织）
  - 支持 dataRight 参数控制数据权限
  - 支持 filterable 搜索
  - virtualDisabled: 编辑表单中虚拟节点不可选，搜索筛选中虚拟节点可选（代表"该区域下所有"）

  用法：
  <RegionTreeSelect v-model="form.regionCode" />                                  <!-- 编辑表单：虚拟节点不可选 -->
  <RegionTreeSelect v-model="query.regionCode" :virtual-disabled="false" />       <!-- 搜索筛选：虚拟节点可选 -->
  <RegionTreeSelect v-model="form.regionCode" root-code="150000" :data-right="true" />
-->
<template>
  <n-tree-select
    :value="modelValue"
    :options="regionOptions"
    :placeholder="placeholder"
    :clearable="clearable"
    :filterable="filterable"
    :disabled="disabled"
    :size="size"
    @update:value="handleUpdate"
  />
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { request } from '@/utils'

const props = defineProps({
  modelValue: {
    type: [String, Number],
    default: undefined,
  },
  rootCode: {
    type: String,
    default: '150000',
  },
  dataRight: {
    type: Boolean,
    default: true,
  },
  placeholder: {
    type: String,
    default: '请选择行政区划',
  },
  clearable: {
    type: Boolean,
    default: true,
  },
  filterable: {
    type: Boolean,
    default: true,
  },
  disabled: {
    type: Boolean,
    default: false,
  },
  size: {
    type: String,
    default: 'medium',
  },
  /**
   * 虚拟组织节点是否不可选中
   * - true（默认）：编辑表单场景，虚拟节点不可选（避免存入ALL后缀编码）
   * - false：搜索筛选场景，虚拟节点可选（代表"该区域下所有"）
   */
  virtualDisabled: {
    type: Boolean,
    default: true,
  },
})

const emit = defineEmits(['update:modelValue', 'change'])

const regionOptions = ref([])

function handleUpdate(value) {
  emit('update:modelValue', value)
  emit('change', value)
}

async function loadRegionOptions() {
  try {
    const res = await request.get('/system/region/treeAll', {
      params: { rootCode: props.rootCode, dataRight: props.dataRight },
    })
    if (res.code === 200) {
      regionOptions.value = convertToTreeSelect(res.data || [])
    }
  }
  catch (error) {
    console.error('加载行政区划选项失败:', error)
  }
}

function convertToTreeSelect(list) {
  return list.map((item) => {
    const node = {
      label: item.name,
      value: item.code,
      key: item.code,
    }
    // 虚拟组织节点：编辑场景不可选，筛选场景可选
    if (props.virtualDisabled && item.code && item.code.endsWith('ALL')) {
      node.disabled = true
    }
    if (item.children && item.children.length > 0) {
      node.children = convertToTreeSelect(item.children)
    }
    return node
  })
}

onMounted(() => {
  loadRegionOptions()
})
</script>
