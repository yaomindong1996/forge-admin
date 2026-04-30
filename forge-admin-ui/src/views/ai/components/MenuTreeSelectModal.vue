<template>
  <n-modal
    v-model:show="visible"
    title="选择归属菜单"
    preset="card"
    style="width: 500px"
    :mask-closable="false"
  >
    <div style="margin-bottom: 12px;">
      <div style="font-size:13px;color:#666;margin-bottom:6px;">
        菜单名称（新菜单的名字，不是父级菜单）
      </div>
      <n-input
        v-model:value="menuName"
        placeholder="必填：新菜单名称，如『员工管理』"
      />
    </div>
    <div style="font-size:13px;color:#666;margin-bottom:6px;">
      选择放置位置（父级目录）
    </div>
    <div class="menu-tree-select-modal">
      <n-spin :show="loading">
        <n-empty
          v-if="!loading && treeData.length === 0"
          description="暂无菜单数据"
          style="padding: 40px 0"
        />
        <n-tree
          v-else
          block-line
          :data="treeData"
          :default-expand-all="true"
          :render-label="renderLabel"
          selectable
          :selected-keys="selectedKeys"
          @update:selected-keys="handleSelect"
        />
      </n-spin>
    </div>

    <template #footer>
      <n-space justify="end">
        <n-button @click="handleClose">
          取消
        </n-button>
        <n-button
          type="primary"
          :disabled="!selectedNode || !menuName.trim()"
          @click="handleConfirm"
        >
          确认
        </n-button>
      </n-space>
    </template>
  </n-modal>
</template>

<script setup>
import { NTag } from 'naive-ui'
import { h, ref, watch } from 'vue'
import { request } from '@/utils'

const props = defineProps({
  show: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['update:show', 'confirm'])

const visible = ref(false)
const loading = ref(false)
const treeData = ref([])
const selectedKeys = ref([])
const selectedNode = ref(null)
const menuName = ref('')

watch(() => props.show, (val) => {
  visible.value = val
  if (val) {
    loadMenuTree()
    selectedKeys.value = []
    selectedNode.value = null
    menuName.value = ''
  }
})

watch(visible, (val) => {
  emit('update:show', val)
})

async function loadMenuTree() {
  try {
    loading.value = true
    const res = await request.get('/system/resource/tree')
    if (res.code === 200) {
      treeData.value = convertToTree(res.data || [])
    }
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
  return list.map(item => ({
    label: item.resourceName,
    key: item.id,
    resourceType: item.resourceType,
    children: item.children && item.children.length > 0
      ? convertToTree(item.children)
      : undefined,
  }))
}

function renderLabel({ option }) {
  const typeMap = { 1: '目录', 2: '菜单', 3: '按钮', 4: 'API' }
  const typeLabel = typeMap[option.resourceType] || '其他'
  const typeColor = option.resourceType === 1 ? 'success' : option.resourceType === 2 ? 'primary' : 'default'
  return h('div', { style: 'display:flex;align-items:center;gap:8px;' }, [
    h(NTag, { size: 'small', type: typeColor }, { default: () => typeLabel }),
    h('span', null, option.label),
  ])
}

function handleSelect(keys, option) {
  selectedKeys.value = keys
  selectedNode.value = option[0] || null
  // 不自动覆盖菜单名称，父级目录名称不是新菜单的名字
}

function handleClose() {
  visible.value = false
}

function handleConfirm() {
  if (!selectedNode.value)
    return
  if (!menuName.value.trim()) {
    window.$message?.warning('请填写菜单名称')
    return
  }
  emit('confirm', {
    menuParentId: selectedNode.value.key,
    menuName: menuName.value.trim(),
  })
  handleClose()
}
</script>

<style scoped>
.menu-tree-select-modal {
  max-height: 400px;
  overflow-y: auto;
}
</style>
