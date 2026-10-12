<template>
  <section class="flow-task-card-list">
    <div class="task-list-toolbar">
      <div class="task-list-titlebar">
        <div class="task-list-title">
          {{ title }}
        </div>
        <span v-if="!selectedKeys.length" class="task-list-count">共 {{ pagination?.itemCount ?? items.length }} 项</span>
        <template v-else>
          <span class="task-list-selected">已选 {{ selectedKeys.length }} 项</span>
          <slot name="batch-actions" :selected-keys="selectedKeys" />
          <button class="task-list-clear" type="button" @click="clearSelection">
            清空
          </button>
        </template>
      </div>

      <div class="task-list-tools">
        <n-input
          v-if="showSearch"
          :value="searchValue"
          :placeholder="searchPlaceholder"
          clearable
          class="task-list-search"
          @update:value="emit('update:searchValue', $event)"
          @keydown.enter="emit('search')"
        >
          <template #prefix>
            <i class="i-material-symbols:search" />
          </template>
        </n-input>
        <slot name="filters" />
        <n-button quaternary class="task-list-icon-btn" title="刷新" aria-label="刷新列表" @click="emit('refresh')">
          <n-icon :size="16">
            <RefreshOutline />
          </n-icon>
        </n-button>
      </div>
    </div>

    <n-spin :show="loading" class="task-list-spin">
      <div v-if="items.length > 0" class="task-card-list">
        <article
          v-for="item in items"
          :key="getRowKey(item)"
          class="task-card-item"
          :class="{ selected: isSelected(item), unread: isUnread(item) }"
          @click="emit('rowClick', item)"
        >
          <div class="task-card-header">
            <button type="button" class="task-card-title" @click.stop="emit('rowClick', item)">
              <slot name="title" :row="item">
                {{ item.title || item.taskName || '-' }}
              </slot>
            </button>
            <div class="task-card-status">
              <slot name="status" :row="item" />
            </div>
          </div>

          <div class="task-card-node">
            当前所在节点：
            <slot name="node" :row="item">
              {{ item.currentNode || item.taskName || item.nodeName || '-' }}
            </slot>
          </div>

          <div class="task-card-footer">
            <div class="task-card-user">
              <slot name="user" :row="item">
                <span class="user-avatar-small">{{ (item.startUserName || item.userName || '?').charAt(0) }}</span>
                <span>{{ item.startUserName || item.userName || '-' }}</span>
              </slot>
            </div>
            <div class="task-card-time">
              提交于 {{ item.createTime || item.submitTime || '-' }}
            </div>
          </div>

          <div v-if="$slots.actions" class="task-card-actions" @click.stop>
            <slot name="actions" :row="item" />
          </div>
        </article>
      </div>
      <div v-else class="task-card-list empty">
        <n-empty class="task-list-empty" :description="emptyText" size="small" />
      </div>
    </n-spin>

    <div v-if="pagination && pagination.itemCount > 0" class="task-list-pagination">
      <div class="task-list-total">
        共 {{ pagination.itemCount }} 条记录
      </div>
      <n-pagination
        :page="pagination.page"
        :page-size="pagination.pageSize"
        :item-count="pagination.itemCount"
        :page-sizes="pagination.pageSizes || [10, 20, 50]"
        :show-size-picker="pagination.showSizePicker !== false"
        @update:page="emit('update:page', $event)"
        @update:page-size="emit('update:pageSize', $event)"
      />
    </div>
  </section>
</template>

<script setup>
import { RefreshOutline } from '@vicons/ionicons5'
import { computed } from 'vue'

const props = defineProps({
  title: { type: String, default: '列表' },
  items: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  pagination: { type: Object, default: null },
  selectedKeys: { type: Array, default: () => [] },
  rowKey: { type: [String, Function], default: 'id' },
  searchValue: { type: String, default: '' },
  searchPlaceholder: { type: String, default: '通过名称搜索' },
  emptyText: { type: String, default: '暂无数据' },
  selectable: { type: Boolean, default: true },
  showSearch: { type: Boolean, default: true },
  unreadKey: { type: String, default: '' },
  entityTitle: { type: String, default: '任务详情' },
  statusTitle: { type: String, default: '当前状态' },
  nodeTitle: { type: String, default: '当前节点' },
  userTitle: { type: String, default: '申请人' },
  actionTitle: { type: String, default: '操作' },
})

const emit = defineEmits([
  'update:selectedKeys',
  'update:searchValue',
  'update:page',
  'update:pageSize',
  'search',
  'refresh',
  'rowClick',
])

const selectedSet = computed(() => new Set(props.selectedKeys))

function getRowKey(row) {
  if (typeof props.rowKey === 'function')
    return props.rowKey(row)
  return row?.[props.rowKey]
}

function isSelected(row) {
  return selectedSet.value.has(getRowKey(row))
}

function isUnread(row) {
  return props.unreadKey ? row?.[props.unreadKey] === 0 : false
}

function clearSelection() {
  emit('update:selectedKeys', [])
}
</script>

<style scoped>
.flow-task-card-list {
  box-sizing: border-box;
  display: flex;
  flex: 1;
  flex-direction: column;
  width: 100%;
  min-height: 0;
  overflow: hidden;
  border: 1px solid var(--border-light, #e8ecf1);
  border-radius: 8px;
  background: var(--bg-primary, #fff);
}

:deep(.n-spin-container),
:deep(.n-spin-content) {
  display: flex;
  flex: 1;
  flex-direction: column;
  width: 100%;
  min-height: 0;
}

.task-list-toolbar {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 0 0 auto;
  padding: 12px;
  border-bottom: 1px solid var(--border-light, #e8ecf1);
  background: var(--bg-primary, #fff);
}

.task-list-titlebar {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.task-list-title {
  color: var(--text-primary, #1a1a2e);
  font-size: 15px;
  font-weight: 600;
  white-space: nowrap;
}

.task-list-count,
.task-list-selected {
  color: var(--text-tertiary, #8c8c9a);
  font-size: 12px;
  font-weight: 400;
  white-space: nowrap;
}

.task-list-clear {
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--primary-color, #2563eb);
  cursor: pointer;
  font: inherit;
  font-size: 12px;
  font-weight: 500;
  line-height: 1;
}

.task-list-tools {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.task-list-search {
  width: 200px;
  flex-shrink: 0;
}

.task-list-search :deep(.n-input__input-el) {
  font-size: 13px;
}

.task-list-icon-btn {
  width: 28px;
  min-width: 28px;
  height: 28px;
  border-radius: 4px;
  background: transparent;
  color: var(--text-secondary, #5c5c6e);
  transition:
    background-color 160ms ease,
    color 160ms ease;
}

.task-list-icon-btn:hover {
  background: var(--bg-secondary, #f5f5fa);
  color: var(--primary-color, #2563eb);
}

:deep(.task-list-icon-btn .n-button__content) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}

:deep(.task-list-icon-btn .n-icon) {
  color: currentColor;
}

:deep(.task-list-icon-btn i) {
  font-size: 16px;
  line-height: 1;
}

.task-list-spin {
  flex: 1;
  min-height: 0;
  min-width: 0;
}

.task-card-list {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 8px;
  gap: 8px;
}

.task-card-list.empty {
  align-items: center;
  justify-content: center;
}

.task-card-item {
  position: relative;
  padding: 12px;
  border: 1px solid var(--border-light, #e8ecf1);
  border-radius: 8px;
  background: var(--bg-primary, #fff);
  cursor: pointer;
  transition:
    border-color 150ms ease,
    background-color 150ms ease,
    box-shadow 150ms ease;
}

.task-card-item:hover {
  border-color: var(--primary-color, #2563eb);
  background: var(--bg-secondary, #fafbff);
  box-shadow: 0 1px 4px rgb(37 99 235 / 8%);
}

.task-card-item.selected {
  border-left: 3px solid var(--primary-color, #2563eb);
  border-color: var(--primary-color, #2563eb);
  background: color-mix(in srgb, var(--primary-color, #2563eb) 4%, var(--bg-primary, #fff));
}

.task-card-item.unread {
  background: color-mix(in srgb, var(--primary-color, #2563eb) 3%, var(--bg-primary, #fff));
}

.task-card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 6px;
}

.task-card-title {
  flex: 1;
  min-width: 0;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--text-primary, #1a1a2e);
  cursor: pointer;
  font: inherit;
  font-size: 14px;
  font-weight: 600;
  line-height: 1.4;
  overflow: hidden;
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color 150ms ease;
}

.task-card-title:hover {
  color: var(--primary-color, #2563eb);
}

.task-card-status {
  flex: 0 0 auto;
}

.task-card-node {
  color: var(--text-secondary, #5c5c6e);
  font-size: 12px;
  line-height: 1.5;
  margin-bottom: 10px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.task-card-user {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  color: var(--text-secondary, #5c5c6e);
  font-size: 12px;
}

.task-card-user span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-avatar-small {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: var(--primary-color, #2563eb);
  color: #fff;
  font-size: 11px;
  font-weight: 500;
  flex: 0 0 auto;
}

.task-card-time {
  color: var(--text-tertiary, #8c8c9a);
  font-size: 11px;
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
}

.task-card-actions {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid var(--border-light, #e8ecf1);
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
}

.task-list-empty {
  padding: 40px 0;
}

.task-list-pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex: 0 0 auto;
  gap: 8px;
  padding: 8px 12px;
  border-top: 1px solid var(--border-light, #e8ecf1);
  background: var(--bg-primary, #fff);
}

.task-list-total {
  color: var(--text-tertiary, #8c8c9a);
  font-size: 12px;
  white-space: nowrap;
}

:global(.task-status-pill) {
  display: inline-flex;
  align-items: center;
  height: 20px;
  padding: 0 8px;
  border: 1px solid transparent;
  border-radius: 4px;
  background: #fff7ed;
  color: #c2410c;
  font-size: 11px;
  font-weight: 500;
  line-height: 1;
  white-space: nowrap;
}

:global(.task-list-hint) {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 22px;
  padding: 0 7px;
  border: 1px solid var(--border-light, #e2e8f0);
  border-radius: 4px;
  background: var(--bg-primary, #fff);
  color: var(--text-secondary, #475569);
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
}

:global(.task-list-hint.urgent) {
  background: #fff7ed;
  color: #c2410c;
}

:global(.task-list-hint.pending) {
  background: #eff6ff;
  color: #2563eb;
}

:global(.task-status-pill.success),
:global(.task-status-pill.read) {
  background: #ecfdf3;
  color: #15803d;
  border-color: #bbf7d0;
}

:global(.task-status-pill.error),
:global(.task-status-pill.unread) {
  background: #fff1f2;
  color: #be123c;
  border-color: #fecdd3;
}

:global(.task-status-pill.info) {
  background: #eff6ff;
  color: #2563eb;
  border-color: #bfdbfe;
}

:global(.task-status-pill.default) {
  background: var(--bg-tertiary);
  color: var(--text-secondary);
  border-color: var(--border-light);
}

:global(.task-row-link-action) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 1px;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--primary-color, #2563eb);
  cursor: pointer;
  font: inherit;
  font-size: 12px;
  font-weight: 500;
  line-height: 1;
  white-space: nowrap;
  transition: color 150ms ease;
}

:global(.task-row-link-action:hover) {
  color: color-mix(in srgb, var(--primary-color, #2563eb) 82%, var(--text-primary));
}

:global(.task-row-link-action.info) {
  color: #2563eb;
}

:global(.task-row-link-action.success) {
  color: #2563eb;
}

:global(.task-row-link-action.danger) {
  color: var(--text-secondary, #475569);
}

:global(.task-row-link-action.danger:hover) {
  color: var(--error-color, #dc2626);
}

:global(.task-row-link-action.muted) {
  color: var(--text-tertiary, #64748b);
}

:global(.task-row-link-action.muted:hover) {
  color: var(--text-secondary, #475569);
}

:global(.task-row-action-separator) {
  width: 1px;
  height: 12px;
  background: var(--border-light, #e2e8f0);
}

:global(.task-row-link-action i) {
  font-size: 14px;
  line-height: 1;
}

@media (max-width: 900px) {
  .task-list-toolbar {
    align-items: stretch;
  }

  .task-list-tools {
    flex-wrap: wrap;
  }

  .task-list-search {
    width: 100%;
  }
}
</style>
