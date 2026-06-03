<template>
  <!--
    TreeCrudTemplate - 树形 CRUD 页面模板
    适合具有父子层级结构的数据（部门、分类等）
    布局：左侧树 + 右侧标准 CRUD 表格
    当 treeConfig 未配置时，降级为标准 CRUD
  -->
  <div v-if="hasTree && !crudProps.formOnly" class="tree-crud-layout">
    <!-- 左侧树形导航 -->
    <div class="tree-crud-left">
      <div class="tree-header">
        <span>{{ treeTitle }}</span>
        <n-button text size="tiny" @click="clearTreeSelect">
          全部
        </n-button>
      </div>
      <n-spin :show="treeLoading">
        <n-tree
          :data="normalizedTreeData"
          key-field="key"
          label-field="label"
          children-field="children"
          block-line
          :default-expand-all="!isLazyTree"
          :on-load="isLazyTree ? handleLoadTreeNode : undefined"
          :selected-keys="selectedKeys"
          @update:selected-keys="handleTreeSelect"
        />
      </n-spin>
    </div>
    <!-- 右侧 CRUD 表格 -->
    <div class="tree-crud-right">
      <AiCrudPage
        ref="crudRef"
        v-bind="mergedCrudProps"
        @submit-success="loadTreeData"
        @delete="loadTreeData"
      />
    </div>
  </div>
  <!-- 无树形配置时降级为标准 CRUD -->
  <AiCrudPage v-else ref="crudRef" v-bind="crudProps" />
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import AiCrudPage from '@/components/ai-form/AiCrudPage.vue'
import { request } from '@/utils'

const props = defineProps({
  crudProps: {
    type: Object,
    required: true,
  },
})

// 从 options 中读取 treeConfig
const treeConfig = computed(() => {
  return props.crudProps?.options?.treeConfig || null
})

const hasTree = computed(() => !!treeConfig.value)
const treeTitle = computed(() => treeConfig.value?.treeTitle || '树形分类')
const treeLoadMode = computed(() => treeConfig.value?.loadMode === 'lazy' ? 'lazy' : 'full')
const isLazyTree = computed(() => treeLoadMode.value === 'lazy')

const treeLoading = ref(false)
const treeData = ref([])
const selectedKeys = ref([])
const selectedId = ref(null)
const crudRef = ref(null)
const normalizedTreeData = computed(() => normalizeTreeNodes(treeData.value))

// 合并 crudProps，注入 parentId 过滤
const mergedCrudProps = computed(() => {
  const filterField = treeConfig.value?.filterField || treeConfig.value?.parentField || 'parentId'
  const includeChildren = treeConfig.value?.includeChildren !== false
  const publicParams = { ...(props.crudProps.publicParams || {}) }
  if (selectedId.value !== null && selectedId.value !== undefined && selectedId.value !== '') {
    publicParams[filterField] = selectedId.value
    if (includeChildren) {
      publicParams[`${filterField}_includeChildren`] = true
    }
  }

  return {
    ...props.crudProps,
    publicParams,
    beforeRenderForm: buildBeforeRenderForm(filterField),
  }
})

function buildBeforeRenderForm(filterField) {
  return async (row) => {
    const originalHook = props.crudProps.beforeRenderForm
    const originalData = typeof originalHook === 'function' ? await originalHook(row) : null
    const nextData = row && typeof row === 'object' ? { ...row } : {}
    if (originalData && typeof originalData === 'object') {
      Object.assign(nextData, originalData)
    }
    if (selectedId.value !== null && selectedId.value !== undefined && selectedId.value !== '' && !row) {
      nextData[filterField] = selectedId.value
    }
    return nextData
  }
}

async function loadTreeData() {
  if (!treeConfig.value)
    return
  const apiConfig = props.crudProps?.apiConfig || {}
  // 优先使用 tree 接口，fallback 到 list 接口
  const treeApi = apiConfig.tree || apiConfig.list
  if (!treeApi)
    return
  const [method, url] = treeApi.split('@')
  if (!url)
    return
  treeLoading.value = true
  try {
    const res = await request({
      method: method.toLowerCase(),
      url,
      params: {
        ...resolveTreeSortParams(),
        loadMode: treeLoadMode.value,
      },
    })
    if (res.code === 200) {
      treeData.value = res.data || []
    }
  }
  catch (e) {
    console.warn('[TreeCrudTemplate] 加载树形数据失败:', e.message)
  }
  finally {
    treeLoading.value = false
  }
}

async function handleLoadTreeNode(node) {
  const apiConfig = props.crudProps?.apiConfig || {}
  const treeApi = apiConfig.tree || apiConfig.list
  if (!treeApi || !node)
    return
  const [method, url] = treeApi.split('@')
  if (!url)
    return
  const config = treeConfig.value || {}
  const parentValue = node?.[config.keyField || 'id'] ?? node?.key ?? node?.targetValue
  try {
    const res = await request({
      method: method.toLowerCase(),
      url,
      params: {
        ...resolveTreeSortParams(),
        loadMode: 'lazy',
        parentValue,
      },
    })
    node.children = normalizeTreeNodes(res?.data || [])
    if (!node.children.length)
      node.isLeaf = true
  }
  catch (error) {
    console.warn('[TreeCrudTemplate] 加载树形子节点失败:', error.message)
    node.isLeaf = true
  }
}

function handleTreeSelect(keys, options = []) {
  selectedKeys.value = keys
  selectedId.value = resolveTreeTargetValue(options[0], keys[0])
}

function clearTreeSelect() {
  selectedKeys.value = []
  selectedId.value = null
}

function resolveTreeSortParams() {
  const params = props.crudProps?.publicParams || {}
  return {
    ...(params.orderByColumn ? { orderByColumn: params.orderByColumn } : {}),
    ...(params.isAsc ? { isAsc: params.isAsc } : {}),
  }
}

onMounted(() => {
  if (hasTree.value) {
    loadTreeData()
  }
})

watch(() => props.crudProps?.apiConfig, () => {
  if (hasTree.value) {
    loadTreeData()
  }
}, { deep: true })

function normalizeTreeNodes(nodes = []) {
  if (!Array.isArray(nodes))
    return []
  const config = treeConfig.value || {}
  const keyField = config.keyField || 'id'
  const childrenField = config.childrenField || 'children'
  return nodes.map((node) => {
    const children = Array.isArray(node?.[childrenField])
      ? node[childrenField]
      : Array.isArray(node?.children)
        ? node.children
        : []
    const normalized = {
      ...(node || {}),
      key: node?.key ?? node?.[keyField],
      label: resolveTreeLabel(node, config),
    }
    const normalizedChildren = normalizeTreeNodes(children)
    if (normalizedChildren.length) {
      normalized.children = normalizedChildren
      normalized.isLeaf = false
    }
    else if (node?.isLeaf !== undefined) {
      normalized.isLeaf = !!node.isLeaf
    }
    return normalized
  })
}

function resolveTreeLabel(node, config) {
  const keyField = config.keyField || 'id'
  const fields = [
    config.labelField,
    'label',
    'name',
    'title',
    'treeName',
    'deptName',
    'orgName',
    keyField,
  ].filter(Boolean)
  for (const field of fields) {
    const value = node?.[field]
    if (value !== undefined && value !== null && String(value) !== '')
      return String(value)
  }
  return '未命名节点'
}

function resolveTreeTargetValue(node, fallbackKey) {
  if (!node)
    return fallbackKey ?? null
  const targetField = treeConfig.value?.targetField || treeConfig.value?.keyField || 'id'
  return node[targetField] ?? node.targetValue ?? node.key ?? fallbackKey ?? null
}

defineExpose({
  showAdd: (...args) => crudRef.value?.showAdd?.(...args),
  showDetail: (...args) => crudRef.value?.showDetail?.(...args),
  refresh: (...args) => crudRef.value?.refresh?.(...args),
})
</script>

<style scoped>
.tree-crud-layout {
  display: flex;
  height: 100%;
  gap: 0;
}

.tree-crud-left {
  width: 220px;
  flex-shrink: 0;
  border-right: 1px solid #e5e7eb;
  padding: 12px 8px;
  overflow-y: auto;
  background: #fafafa;
}

.tree-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
  font-weight: 600;
  color: #374151;
  padding: 0 6px 10px;
  border-bottom: 1px solid #f3f4f6;
  margin-bottom: 8px;
}

.tree-crud-right {
  flex: 1;
  overflow: hidden;
}
</style>
