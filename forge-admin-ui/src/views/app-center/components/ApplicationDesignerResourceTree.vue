<template>
  <aside class="application-resource-tree" aria-label="应用设计资源">
    <div class="resource-tree-heading">
      <span class="resource-tree-app-icon" aria-hidden="true">
        <n-icon><component :is="groupIcon(activeGroup?.key)" /></n-icon>
      </span>
      <div>
        <strong>{{ activeGroup?.label || '应用资源' }}</strong>
        <span>{{ applicationName || '未命名应用' }}</span>
      </div>
      <button
        v-if="activeGroup?.key === 'pages'"
        type="button"
        class="resource-group-create"
        aria-label="新建页面"
        title="新建页面"
        @click="emit('createPage')"
      >
        <n-icon><AddOutline /></n-icon>
      </button>
    </div>

    <div class="resource-tree-scroll">
      <div class="resource-node-list">
        <div
          v-for="node in activeGroup?.nodes || []"
          :key="node.key"
          class="resource-node-row"
          :class="{ active: node.key === activeKey }"
        >
          <button
            type="button"
            class="resource-node"
            :title="node.label"
            @click="emit('select', node)"
          >
            <n-icon class="resource-node-icon">
              <component :is="nodeIcon(node.kind)" />
            </n-icon>
            <span>{{ node.label }}</span>
            <em v-if="node.configured === false">未配置</em>
          </button>
          <n-dropdown
            v-if="node.editable"
            trigger="click"
            placement="bottom-end"
            :options="nodeMenuOptions(node)"
            @select="key => emit('nodeAction', { key, node })"
          >
            <button
              type="button"
              class="resource-node-more"
              aria-label="页面操作"
              title="重命名、复制、排序、删除"
              @click.stop
            >
              <n-icon><EllipsisHorizontalOutline /></n-icon>
            </button>
          </n-dropdown>
        </div>
        <span v-if="!activeGroup?.nodes?.length" class="resource-group-empty">暂无资源</span>
      </div>
    </div>
  </aside>
</template>

<script setup>
import {
  AddOutline,
  CubeOutline,
  DocumentTextOutline,
  EllipsisHorizontalOutline,
  FlashOutline,
  GitBranchOutline,
  GitNetworkOutline,
  ListOutline,
  SettingsOutline,
} from '@vicons/ionicons5'
import { computed } from 'vue'

const props = defineProps({
  groups: { type: Array, default: () => [] },
  activeGroupKey: { type: String, default: '' },
  activeKey: { type: String, default: '' },
  applicationName: { type: String, default: '' },
  // 由父级提供节点操作菜单，保证与顶层导航共用同一套页面结构操作。
  nodeMenuOptions: { type: Function, default: () => [] },
})
const emit = defineEmits(['select', 'createPage', 'nodeAction'])

const activeGroup = computed(() => {
  return props.groups.find(group => group.key === props.activeGroupKey)
    || props.groups.find(group => group.nodes?.some(node => node.key === props.activeKey))
    || props.groups[0]
    || null
})

function groupIcon(groupKey) {
  return {
    pages: DocumentTextOutline,
    data: CubeOutline,
    automation: FlashOutline,
    flow: GitBranchOutline,
    settings: SettingsOutline,
  }[groupKey] || DocumentTextOutline
}

function nodeIcon(kind) {
  if (kind === 'page-list')
    return ListOutline
  if (kind === 'page-form' || kind === 'page-custom')
    return DocumentTextOutline
  if (kind === 'data-relations')
    return GitNetworkOutline
  if (kind?.startsWith('data-'))
    return CubeOutline
  if (kind?.startsWith('automation-'))
    return FlashOutline
  if (kind === 'flow-object')
    return GitBranchOutline
  return SettingsOutline
}
</script>

<style scoped>
.application-resource-tree {
  position: fixed;
  z-index: 8;
  top: 56px;
  bottom: 0;
  left: 0;
  display: grid;
  width: 232px;
  min-height: 0;
  grid-template-rows: 54px minmax(0, 1fr);
  border-right: 1px solid var(--border-light);
  background: var(--bg-primary);
}

.resource-tree-heading {
  display: flex;
  align-items: center;
  gap: 9px;
  min-width: 0;
  padding: 0 8px 0 12px;
  border-bottom: 1px solid #eef0f2;
}

.resource-tree-heading > div {
  min-width: 0;
  flex: 1;
}

.resource-tree-heading strong,
.resource-tree-heading > div > span {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.resource-tree-heading strong {
  color: var(--text-primary);
  font-size: 13px;
}

.resource-tree-heading > div > span {
  margin-top: 2px;
  color: #8f959e;
  font-size: 11px;
}

.resource-tree-app-icon {
  display: grid !important;
  width: 28px;
  height: 28px;
  flex: 0 0 auto;
  place-items: center;
  border: 1px solid #d9e2f2;
  border-radius: 6px;
  background: #f4f7fb;
  color: #3370ff !important;
  font-size: 15px;
}

.resource-tree-scroll {
  min-height: 0;
  overflow-y: auto;
  padding: 8px 7px 16px;
}

.resource-group-create {
  display: grid;
  width: 26px;
  height: 26px;
  flex: 0 0 26px;
  cursor: pointer;
  place-items: center;
  border: 0;
  border-radius: 4px;
  background: transparent;
  padding: 0;
  color: #646a73;
  font-size: 15px;
}

.resource-group-create:hover {
  background: var(--bg-tertiary);
  color: var(--text-primary);
}

.resource-node-list {
  display: grid;
  gap: 2px;
}

.resource-node-row {
  display: grid;
  min-height: 34px;
  align-items: center;
  grid-template-columns: minmax(0, 1fr) auto;
  border-left: 2px solid transparent;
  border-radius: 0 4px 4px 0;
  padding-right: 4px;
}

.resource-node-row:hover {
  background: var(--bg-secondary);
}

.resource-node-row.active {
  border-left-color: #3370ff;
  background: #f4f7fc;
}

.resource-node {
  display: grid;
  width: 100%;
  min-width: 0;
  align-items: center;
  gap: 7px;
  grid-template-columns: 17px minmax(0, 1fr) auto;
  cursor: pointer;
  border: 0;
  background: transparent;
  padding: 5px 4px 5px 11px;
  color: var(--text-secondary);
  text-align: left;
}

.resource-node-row:hover .resource-node {
  color: var(--text-primary);
}

.resource-node-row.active .resource-node {
  color: #245bdb;
  font-weight: 600;
}

.resource-node > span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
}

.resource-node-icon {
  color: #8f959e;
  font-size: 15px;
}

.resource-node-row.active .resource-node-icon {
  color: #3370ff;
}

.resource-node em {
  color: #a0a5ad;
  font-size: 10px;
  font-style: normal;
  font-weight: 400;
}

.resource-node-more {
  display: grid;
  width: 22px;
  height: 22px;
  cursor: pointer;
  place-items: center;
  border: 0;
  border-radius: 4px;
  background: transparent;
  padding: 0;
  color: #8f959e;
  font-size: 14px;
  opacity: 0;
}

.resource-node-row:hover .resource-node-more,
.resource-node-row.active .resource-node-more {
  opacity: 1;
}

.resource-node-more:hover {
  background: #e9ebef;
  color: var(--text-primary);
}

.resource-group-empty {
  display: block;
  padding: 7px 14px 9px 13px;
  color: #b1b5bc;
  font-size: 11px;
}

@media (max-width: 980px) {
  .application-resource-tree {
    width: 200px;
  }
}
</style>
