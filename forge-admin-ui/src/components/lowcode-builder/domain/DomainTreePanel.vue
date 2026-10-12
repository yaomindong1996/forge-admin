<template>
  <aside class="domain-tree-panel">
    <div class="panel-head">
      <div>
        <div class="panel-title">
          业务领域
        </div>
        <div class="panel-subtitle">
          {{ totalCount }} 个领域节点
        </div>
      </div>
      <n-space size="small">
        <n-tooltip>
          <template #trigger>
            <n-button quaternary circle size="small" :loading="loading" @click="$emit('refresh')">
              <template #icon>
                <n-icon><RefreshOutline /></n-icon>
              </template>
            </n-button>
          </template>
          刷新领域
        </n-tooltip>
        <n-tooltip>
          <template #trigger>
            <n-button type="primary" circle size="small" @click="$emit('create')">
              <template #icon>
                <n-icon><AddOutline /></n-icon>
              </template>
            </n-button>
          </template>
          新增领域
        </n-tooltip>
      </n-space>
    </div>

    <n-input
      :value="keyword"
      clearable
      size="small"
      placeholder="搜索领域编码 / 名称"
      class="domain-search"
      @update:value="$emit('update:keyword', $event)"
      @keyup.enter="$emit('search')"
      @clear="$emit('search')"
    />

    <n-spin :show="loading">
      <div class="tree-list">
        <button
          class="tree-node all-node"
          :class="{ active: selectedDomainId === null }"
          type="button"
          @click="$emit('select', null)"
        >
          <span class="node-icon all-icon">
            <n-icon><GridOutline /></n-icon>
          </span>
          <span class="node-main">
            <span class="node-title">全部应用</span>
            <span class="node-meta">跨领域视图</span>
          </span>
        </button>

        <button
          v-for="item in visibleDomains"
          :key="item.id"
          class="tree-node"
          :class="{ active: selectedDomainId === item.id, disabled: item.status === 'DISABLED' }"
          type="button"
          :style="{ '--indent': item.level }"
          @click="$emit('select', item)"
        >
          <span class="node-indent" />
          <span
            class="node-toggle"
            :class="{ empty: !hasChildren(item) }"
            @click.stop="toggleExpanded(item)"
          >
            <n-icon v-if="hasChildren(item)">
              <ChevronDownOutline v-if="isExpanded(item.id)" />
              <ChevronForwardOutline v-else />
            </n-icon>
          </span>
          <n-dropdown
            trigger="click"
            :options="domainActionOptions"
            @select="key => handleDomainAction(key, item)"
          >
            <span class="node-action" @click.stop>
              <n-icon><EllipsisVertical /></n-icon>
            </span>
          </n-dropdown>
          <span class="node-icon">
            <n-icon><FolderOpenOutline /></n-icon>
          </span>
          <span class="node-main">
            <span class="node-title">{{ item.domainName }}</span>
            <span class="node-meta">{{ item.domainCode }}</span>
          </span>
          <n-tag
            v-if="item.status === 'DISABLED'"
            size="small"
            type="warning"
            :bordered="false"
          >
            停用
          </n-tag>
        </button>
      </div>

      <n-empty v-if="!flatDomains.length && !loading" size="small" description="暂无业务领域" />
    </n-spin>
  </aside>
</template>

<script setup>
import {
  AddOutline,
  ChevronDownOutline,
  ChevronForwardOutline,
  EllipsisVertical,
  FolderOpenOutline,
  GridOutline,
  RefreshOutline,
} from '@vicons/ionicons5'
import { computed, ref, watch } from 'vue'

defineOptions({ name: 'DomainTreePanel' })

const props = defineProps({
  domains: {
    type: Array,
    default: () => [],
  },
  selectedDomainId: {
    type: [Number, String],
    default: null,
  },
  keyword: {
    type: String,
    default: '',
  },
  loading: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['select', 'create', 'createChild', 'edit', 'refresh', 'search', 'update:keyword', 'delete'])

const domainActionOptions = [
  { label: '新增子目录', key: 'create-child' },
  { label: '编辑领域', key: 'edit' },
  { type: 'divider', key: 'divider' },
  { label: '删除领域', key: 'delete' },
]

const flatDomains = computed(() => flattenDomains(props.domains))
const visibleDomains = computed(() => flattenVisibleDomains(props.domains))
const totalCount = computed(() => flatDomains.value.length)
const expandedIds = ref(new Set())

watch(
  () => props.domains,
  (domains) => {
    const next = new Set(expandedIds.value)
    ;(domains || []).forEach((domain) => {
      if (domain.children?.length)
        next.add(domain.id)
    })
    expandedIds.value = next
  },
  { immediate: true, deep: true },
)

function flattenDomains(nodes, level = 0) {
  const result = []
  for (const node of nodes || []) {
    result.push({ ...node, level })
    if (node.children?.length)
      result.push(...flattenDomains(node.children, level + 1))
  }
  return result
}

function flattenVisibleDomains(nodes, level = 0) {
  const result = []
  for (const node of nodes || []) {
    result.push({ ...node, level })
    if (node.children?.length && isExpanded(node.id))
      result.push(...flattenVisibleDomains(node.children, level + 1))
  }
  return result
}

function hasChildren(item) {
  return Boolean(item?.children?.length)
}

function isExpanded(id) {
  return expandedIds.value.has(id)
}

function toggleExpanded(item) {
  if (!hasChildren(item))
    return
  const next = new Set(expandedIds.value)
  if (next.has(item.id))
    next.delete(item.id)
  else
    next.add(item.id)
  expandedIds.value = next
}

function handleDomainAction(key, item) {
  if (key === 'create-child') {
    emit('createChild', item)
    return
  }
  if (key === 'edit') {
    emit('edit', item)
    return
  }
  if (key === 'delete')
    emit('delete', item)
}
</script>

<style scoped>
.domain-tree-panel {
  display: flex;
  min-height: 0;
  flex-direction: column;
  gap: 12px;
  padding: 14px;
  border: 1px solid #d8dee8;
  border-radius: 8px;
  background: var(--bg-primary);
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.06);
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.panel-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary);
}

.panel-subtitle {
  margin-top: 2px;
  font-size: 12px;
  color: var(--text-secondary);
}

.domain-search {
  flex: 0 0 auto;
}

.tree-list {
  display: flex;
  min-height: 0;
  max-height: calc(100vh - 250px);
  flex-direction: column;
  gap: 6px;
  overflow: auto;
  padding-right: 2px;
}

.tree-node {
  display: grid;
  width: 100%;
  min-height: 48px;
  grid-template-columns: calc(var(--indent, 0) * 16px) 20px 24px 28px minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
  border: 1px solid transparent;
  border-radius: 8px;
  background: transparent;
  color: inherit;
  cursor: pointer;
  padding: 7px 8px;
  text-align: left;
  transition:
    background 180ms ease,
    border-color 180ms ease,
    transform 180ms ease;
}

.tree-node:hover {
  border-color: #dbeafe;
  background: #f8fbff;
}

.tree-node.active {
  border-color: #93c5fd;
  background: #eff6ff;
}

.tree-node.disabled {
  opacity: 0.68;
}

.tree-node.all-node {
  grid-template-columns: 28px minmax(0, 1fr);
}

.node-action {
  display: inline-flex;
  width: 24px;
  height: 24px;
  justify-self: center;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  color: var(--text-secondary);
  opacity: 0;
  transition: opacity 160ms ease;
}

.tree-node:hover .node-action,
.tree-node.active .node-action {
  opacity: 1;
}

.node-action:hover {
  background: var(--bg-tertiary);
  color: var(--text-primary);
}

.node-indent {
  display: block;
}

.node-toggle {
  display: inline-flex;
  width: 20px;
  height: 24px;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  color: var(--text-secondary);
}

.node-toggle:not(.empty):hover {
  background: #e0ecff;
  color: #2563eb;
}

.node-toggle.empty {
  cursor: default;
}

.node-icon {
  display: inline-flex;
  width: 28px;
  height: 28px;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: #eef2ff;
  color: #2563eb;
}

.all-icon {
  background: #ecfdf5;
  color: #16a34a;
}

.node-main {
  display: grid;
  min-width: 0;
  gap: 2px;
}

.node-title {
  overflow: hidden;
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.node-meta {
  overflow: hidden;
  color: var(--text-secondary);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 1080px) {
  .domain-tree-panel {
    min-height: auto;
  }

  .tree-list {
    max-height: 360px;
  }
}
</style>
