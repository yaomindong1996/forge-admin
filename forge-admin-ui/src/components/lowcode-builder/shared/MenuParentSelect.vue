<template>
  <n-tree-select
    :value="normalizedValue"
    :options="treeData"
    :loading="loading"
    clearable
    filterable
    block-line
    default-expand-all
    :render-label="renderLabel"
    :placeholder="placeholder"
    @update:value="handleUpdateValue"
  />
</template>

<script setup>
import { NTag } from 'naive-ui'
import { computed, h, onMounted, ref, watch } from 'vue'
import { request } from '@/utils'

defineOptions({ name: 'MenuParentSelect' })

const props = defineProps({
  value: {
    type: [String, Number],
    default: null,
  },
  placeholder: {
    type: String,
    default: '请选择父级菜单',
  },
  autoLoad: {
    type: Boolean,
    default: true,
  },
})

const emit = defineEmits(['update:value', 'loaded'])

const loading = ref(false)
const treeData = ref([])
const normalizedValue = computed(() => normalizeTreeKey(props.value))

watch(
  () => props.autoLoad,
  (value) => {
    if (value && !treeData.value.length)
      loadMenuTree()
  },
)

onMounted(() => {
  if (props.autoLoad)
    loadMenuTree()
})

async function loadMenuTree() {
  loading.value = true
  try {
    const res = await request.get('/system/resource/tree')
    const data = Array.isArray(res?.data) ? res.data : []
    treeData.value = convertToTree(data)
    emit('loaded', treeData.value)
  }
  catch (error) {
    console.error('加载菜单树失败:', error)
    window.$message?.error('加载菜单树失败')
  }
  finally {
    loading.value = false
  }
}

function convertToTree(list) {
  return (list || []).map(item => ({
    label: item.resourceName,
    key: normalizeTreeKey(item.id),
    resourceType: item.resourceType,
    disabled: !isSelectableMenu(item.resourceType),
    children: item.children?.length ? convertToTree(item.children) : undefined,
  }))
}

function isSelectableMenu(resourceType) {
  return Number(resourceType) === 1 || Number(resourceType) === 2
}

function renderLabel({ option }) {
  const typeMap = {
    1: { label: '目录', type: 'success' },
    2: { label: '菜单', type: 'info' },
    3: { label: '按钮', type: 'default' },
    4: { label: '接口', type: 'default' },
  }
  const meta = typeMap[Number(option.resourceType)] || { label: '资源', type: 'default' }
  return h('div', { class: 'menu-parent-node' }, [
    h(NTag, { size: 'small', type: meta.type, bordered: false }, { default: () => meta.label }),
    h('span', { class: 'menu-parent-label' }, option.label),
  ])
}

function handleUpdateValue(nextValue) {
  emit('update:value', normalizeTreeKey(nextValue))
}

function normalizeTreeKey(value) {
  if (value === null || value === undefined || value === '')
    return null
  return String(value)
}
</script>

<style scoped>
:deep(.menu-parent-node) {
  display: inline-flex;
  min-width: 0;
  align-items: center;
  gap: 8px;
}

:deep(.menu-parent-label) {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
