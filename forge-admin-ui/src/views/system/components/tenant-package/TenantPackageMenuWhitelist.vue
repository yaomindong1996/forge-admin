<template>
  <div class="tenant-package-menu-whitelist">
    <div class="toolbar">
      <n-space size="small" align="center">
        <n-input
          v-model:value="keyword"
          size="small"
          clearable
          placeholder="搜索菜单"
          style="width: 220px"
        />
        <n-button size="small" quaternary @click="expandAll">
          展开
        </n-button>
        <n-button size="small" quaternary @click="collapseAll">
          折叠
        </n-button>
        <n-button size="small" quaternary @click="clearChecked">
          清空
        </n-button>
        <n-text depth="3" style="font-size: 12px">
          已选 {{ checkedKeys.length }} 项；不选则开户时从默认 admin 拷贝并排除租户管理
        </n-text>
      </n-space>
    </div>
    <n-spin :show="loading">
      <div class="tree-panel">
        <n-empty v-if="!loading && !filteredTree.length" description="暂无菜单数据" />
        <n-tree
          v-else
          block-line
          checkable
          cascade
          :data="filteredTree"
          :checked-keys="checkedKeys"
          :expanded-keys="expandedKeys"
          key-field="key"
          label-field="label"
          children-field="children"
          :pattern="keyword"
          :show-irrelevant-nodes="false"
          @update:checked-keys="handleCheckedKeys"
          @update:expanded-keys="keys => expandedKeys = keys"
        />
      </div>
    </n-spin>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { request } from '@/utils'
import { parseResourceIds, toMenuTreeOptions } from './tenant-package-schema'

const props = defineProps({
  formData: {
    type: Object,
    default: () => ({}),
  },
  value: {
    type: [Array, String, Number],
    default: undefined,
  },
  // AiForm 插槽透传：必须走它写回，否则提交拿不到 resourceIds
  updateValue: {
    type: Function,
    default: null,
  },
})

const loading = ref(false)
const treeReady = ref(false)
const treeData = ref([])
const expandedKeys = ref([])
const keyword = ref('')

const checkedKeys = computed(() => {
  if (props.value !== undefined && props.value !== null && props.value !== '')
    return parseResourceIds(props.value)
  return parseResourceIds(props.formData?.resourceIds)
})

const filteredTree = computed(() => treeData.value)

async function loadTree() {
  loading.value = true
  treeReady.value = false
  try {
    const res = await request.get('/system/resource/tree')
    treeData.value = toMenuTreeOptions(res.data || [])
    if (!expandedKeys.value.length)
      expandTopLevel()
  }
  catch (error) {
    console.error('加载菜单树失败:', error)
    window.$message?.warning?.('菜单树加载失败')
    treeData.value = []
  }
  finally {
    loading.value = false
    // 树数据就绪后再接受勾选变更，避免 n-tree 初始化/cascade 回写空数组冲掉详情回显
    treeReady.value = true
  }
}

function collectKeys(nodes = [], acc = []) {
  for (const node of nodes) {
    if (node?.key != null)
      acc.push(node.key)
    if (node?.children?.length)
      collectKeys(node.children, acc)
  }
  return acc
}

function expandTopLevel() {
  expandedKeys.value = treeData.value.map(node => node.key).filter(key => key != null)
}

function expandAll() {
  expandedKeys.value = collectKeys(treeData.value)
}

function collapseAll() {
  expandedKeys.value = []
}

function writeResourceIds(ids) {
  const next = parseResourceIds(ids)
  if (typeof props.updateValue === 'function') {
    props.updateValue(next)
    return
  }
  if (props.formData)
    props.formData.resourceIds = next
}

function clearChecked() {
  writeResourceIds([])
}

function handleCheckedKeys(keys) {
  if (!treeReady.value || loading.value)
    return
  writeResourceIds(keys)
}

loadTree()
</script>

<style scoped>
.tenant-package-menu-whitelist {
  width: 100%;
  border: 1px solid var(--n-border-color);
  border-radius: 8px;
  padding: 10px 12px 4px;
  background: var(--n-color);
}
.toolbar {
  margin-bottom: 8px;
}
.tree-panel {
  max-height: 360px;
  overflow: auto;
  padding-bottom: 8px;
}
</style>
