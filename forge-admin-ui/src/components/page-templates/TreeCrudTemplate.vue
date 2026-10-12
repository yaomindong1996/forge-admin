<template>
  <!--
    TreeCrudTemplate - 树形 CRUD 页面模板
    适合具有父子层级结构的数据（部门、分类等）
    布局：左侧树 + 右侧标准 CRUD 表格
    当 treeConfig 未配置时，降级为标准 CRUD
  -->
  <div v-if="hasTree && !crudProps.formOnly" class="tree-crud-layout">
    <!-- 左侧树形导航 -->
    <div class="tree-crud-left" :class="{ 'is-collapsed': treePanelCollapsed }">
      <div v-if="treePanelCollapsed" class="tree-panel-rail" :title="treeTitle">
        <button
          type="button"
          class="tree-panel-edge-toggle is-collapsed"
          aria-label="展开筛选树"
          title="展开筛选树"
          @click="treePanelCollapsed = false"
        >
          <span>›</span>
        </button>
        <span class="tree-panel-rail__label">树</span>
      </div>
      <template v-else>
        <div class="tree-header">
          <span>{{ treeTitle }}</span>
          <div class="tree-header-actions">
            <n-button text size="tiny" @click="clearTreeSelect">
              全部
            </n-button>
            <button
              type="button"
              class="tree-panel-edge-toggle"
              aria-label="收起筛选树"
              title="收起筛选树"
              @click="treePanelCollapsed = true"
            >
              <span>‹</span>
            </button>
          </div>
        </div>
        <div class="tree-node-toolbar">
          <span>节点层级</span>
          <div class="tree-expand-actions">
            <button type="button" @click="expandAllTreeNodes">
              展开
            </button>
            <button type="button" @click="collapseAllTreeNodes">
              收起
            </button>
          </div>
        </div>
        <n-spin :show="treeLoading">
          <n-tree
            :data="normalizedTreeData"
            key-field="key"
            label-field="label"
            children-field="children"
            block-line
            :expanded-keys="expandedKeys"
            :on-load="isLazyTree ? handleLoadTreeNode : undefined"
            :selected-keys="selectedKeys"
            @update:expanded-keys="expandedKeys = $event"
            @update:selected-keys="handleTreeSelect"
          />
        </n-spin>
      </template>
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
import { buildLeftTreeFilterParams, collectTreeFilterValues } from '@/components/lowcode-builder/shared/runtime-tree-table'
import { request } from '@/utils'

const props = defineProps({
  crudProps: {
    type: Object,
    required: true,
  },
})

// 从 options 中读取 treeConfig
const treeConfig = computed(() => {
  return normalizeTreeConfig(props.crudProps?.options?.treeConfig)
})

const hasTree = computed(() => {
  if (treeConfig.value && treeConfig.value.enabled !== false)
    return true
  // layoutType=tree-crud 但 treeConfig 不完整时，只要有 tree API 仍展示左树
  return Boolean(props.crudProps?.apiConfig?.tree)
})
const treeTitle = computed(() => treeConfig.value?.treeTitle || '树形分类')
const treeLoadMode = computed(() => treeConfig.value?.loadMode === 'lazy' ? 'lazy' : 'full')
const isLazyTree = computed(() => treeLoadMode.value === 'lazy')

const treeLoading = ref(false)
const treeData = ref([])
const selectedKeys = ref([])
const selectedId = ref(null)
const expandedKeys = ref([])
const treePanelCollapsed = ref(false)
const crudRef = ref(null)
const normalizedTreeData = computed(() => normalizeTreeNodes(treeData.value))

// 合并 crudProps，注入 parentId 过滤（默认含下级）
const mergedCrudProps = computed(() => {
  const filterField = treeConfig.value?.filterField || 'parentId'
  const includeChildren = treeConfig.value?.includeChildren !== false
  const publicParams = { ...(props.crudProps.publicParams || {}) }
  if (selectedId.value !== null && selectedId.value !== undefined && selectedId.value !== '') {
    const selectedNode = findTreeNodeByTargetValue(normalizedTreeData.value, selectedId.value)
    Object.assign(publicParams, buildLeftTreeFilterParams({
      filterField,
      value: selectedId.value,
      includeChildren,
      expandedValues: collectTreeFilterValues(selectedNode, {
        targetField: treeConfig.value?.targetField || treeConfig.value?.keyField || 'id',
        childrenField: treeConfig.value?.childrenField || 'children',
        includeChildren,
      }),
    }))
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
  const apiConfig = props.crudProps?.apiConfig || {}
  // 优先使用 tree 接口，fallback 到 list 接口；treeConfig 可为空（仅有 apiConfig.tree）
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
      treeData.value = resolveTreeResponseData(res)
      expandedKeys.value = isLazyTree.value
        ? []
        : collectTreeKeys(normalizeTreeNodes(treeData.value))
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
        parentValue: parentValue === null || parentValue === undefined ? '' : String(parentValue),
      },
    })
    node.children = normalizeTreeNodes(resolveTreeResponseData(res))
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

function expandAllTreeNodes() {
  expandedKeys.value = collectTreeKeys(normalizedTreeData.value)
}

function collapseAllTreeNodes() {
  expandedKeys.value = []
}

function collectTreeKeys(nodes = []) {
  return (Array.isArray(nodes) ? nodes : []).flatMap((node) => {
    const key = node?.key === undefined || node?.key === null ? '' : String(node.key)
    const childKeys = collectTreeKeys(node?.children || [])
    return key ? [key, ...childKeys] : childKeys
  })
}

function resolveTreeSortParams() {
  // 右表列表的 publicParams.orderBy 不能拿来排左树节点，否则会跟着列表 defaultSort（如 id desc）跑偏
  const config = treeConfig.value || {}
  const orderByColumn = config.orderByColumn || config.defaultSortField
  if (!orderByColumn)
    return {}
  const isAsc = String(config.isAsc || config.defaultSortOrder || 'asc').toLowerCase() === 'asc' ? 'asc' : 'desc'
  return { orderByColumn, isAsc }
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
    const rawKey = node?.key ?? node?.[keyField]
    const normalized = {
      ...(node || {}),
      key: rawKey === null || rawKey === undefined ? '' : String(rawKey),
      label: resolveTreeLabel(node, config),
    }
    const normalizedChildren = normalizeTreeNodes(children)
    if (normalizedChildren.length) {
      normalized.children = normalizedChildren
      normalized.isLeaf = false
    }
    else if (node?.isLeaf !== undefined) {
      normalized.isLeaf = node.isLeaf === true || node.isLeaf === 1 || node.isLeaf === '1'
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
  const value = node[targetField] ?? node.targetValue ?? node.key ?? fallbackKey ?? null
  return value === null || value === undefined ? null : String(value)
}

function findTreeNodeByTargetValue(nodes = [], targetValue) {
  const expected = targetValue === null || targetValue === undefined ? '' : String(targetValue)
  if (!expected)
    return null
  for (const node of Array.isArray(nodes) ? nodes : []) {
    if (resolveTreeTargetValue(node) === expected)
      return node
    const child = findTreeNodeByTargetValue(node?.children || [], expected)
    if (child)
      return child
  }
  return null
}

function normalizeTreeConfig(source) {
  if (!source || typeof source !== 'object')
    return null
  const targetField = source.targetField || source.nodeValueField || source.valueField || source.keyField || 'id'
  const filterField = source.filterField || source.rightFilterField || source.listFilterField || source.parentField || 'parentId'
  return {
    ...source,
    enabled: source.enabled !== false,
    treeTitle: source.treeTitle || source.title || '树形分类',
    keyField: source.keyField || source.nodeKeyField || 'id',
    parentField: source.parentField || source.parentIdField || 'parentId',
    labelField: source.labelField || source.displayField || source.nameField || 'name',
    targetField,
    nodeValueField: targetField,
    filterField,
    rightFilterField: filterField,
    childrenField: source.childrenField || 'children',
    loadMode: source.loadMode === 'lazy' || source.lazy === true ? 'lazy' : 'full',
  }
}

function resolveTreeResponseData(response) {
  const data = response?.data
  if (Array.isArray(data))
    return data
  if (Array.isArray(data?.records))
    return data.records
  if (Array.isArray(data?.rows))
    return data.rows
  if (Array.isArray(data?.list))
    return data.list
  return []
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
  width: 100%;
  height: 100%;
  min-height: 520px;
  gap: 0;
  min-width: 0;
}

.tree-crud-left {
  position: relative;
  width: 240px;
  flex-shrink: 0;
  border-right: 1px solid var(--border-light);
  padding: 12px 8px;
  overflow: hidden;
  background: var(--bg-secondary);
  display: flex;
  flex-direction: column;
  min-height: 0;
  border-radius: 0;
  transition: width 0.18s ease;
}

.tree-crud-left.is-collapsed {
  width: 36px;
  padding: 8px 0;
}

.tree-panel-edge-toggle {
  position: static;
  top: auto;
  right: auto;
  z-index: 1;
  display: inline-grid;
  place-items: center;
  width: 22px;
  height: 22px;
  border: 1px solid var(--border-light);
  border-radius: 6px;
  background: var(--bg-primary);
  color: var(--text-secondary);
  cursor: pointer;
  line-height: 1;
  padding: 0;
}

.tree-panel-edge-toggle:hover {
  border-color: #93c5fd;
  background: #eff6ff;
  color: #2563eb;
}

.tree-panel-edge-toggle span {
  display: block;
  margin-top: -1px;
  font-size: 16px;
  font-weight: 600;
  line-height: 1;
}

.tree-panel-rail {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
  gap: 10px;
  flex: 1;
  width: 100%;
  padding-top: 4px;
  color: var(--text-secondary);
}

.tree-panel-rail__label {
  writing-mode: vertical-rl;
  letter-spacing: 0.18em;
  font-size: 12px;
  color: var(--text-secondary);
}

.tree-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #374151;
  padding: 0 0 10px;
  border-bottom: 1px solid #f3f4f6;
  margin-bottom: 8px;
  flex-shrink: 0;
}

.tree-header-actions {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.tree-node-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 0 6px 8px;
  color: var(--text-secondary);
  font-size: 12px;
  flex-shrink: 0;
}

.tree-expand-actions {
  display: inline-flex;
  gap: 6px;
}

.tree-expand-actions button {
  border: 1px solid var(--border-light);
  border-radius: 4px;
  background: var(--bg-primary);
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1;
  padding: 4px 8px;
  cursor: pointer;
}

.tree-expand-actions button:hover {
  border-color: #93c5fd;
  color: #2563eb;
}

.tree-crud-left :deep(.n-spin-container) {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

.tree-crud-right {
  flex: 1;
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.tree-crud-right :deep(.ai-crud-page) {
  flex: 1 1 0;
  width: 100%;
  height: 100%;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}
</style>
