<template>
  <section class="flow-task-card-list" :class="{ 'is-embedded': embedded }">
    <div v-if="$slots.prepend" class="task-list-prepend">
      <slot name="prepend" />
    </div>

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
          :class="{
            selected: isSelected(item),
            unread: isUnread(item),
            'has-actions': !!$slots.actions,
          }"
          @click="emit('rowClick', item)"
        >
          <div class="task-card-body">
            <!-- 左侧彩色类型图标 -->
            <div class="task-card-icon" :class="resolveRowIconTone(item)">
              <slot name="icon" :row="item">
                <i :class="resolveRowIcon(item)" />
              </slot>
            </div>

            <div class="task-card-main">
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
                <span class="task-card-node-label">{{ nodeTitle }}：</span>
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
                <div v-if="showTime" class="task-card-time">
                  <slot name="time" :row="item">
                    提交于 {{ item.createTime || item.submitTime || '-' }}
                  </slot>
                </div>
              </div>
            </div>
          </div>

          <!-- 操作单独占一行，不挤在内容右侧 -->
          <div v-if="$slots.actions" class="task-card-actions" @click.stop>
            <slot name="actions" :row="item" />
          </div>
        </article>
      </div>
      <!-- loading 中不展示空态，避免白屏感的「暂无数据」闪一下 -->
      <div v-else-if="!loading" class="task-card-list empty">
        <n-empty class="task-list-empty" :description="emptyText" size="small" />
      </div>
      <div v-else class="task-card-list task-card-list--loading-placeholder" aria-hidden="true" />
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
  /** 是否显示卡片右下角时间；抄送等已在 user 区展示时间时关闭，避免贴紧重复 */
  showTime: { type: Boolean, default: true },
  /** 嵌在外层工作台时去掉自身边框，避免双框贴合 */
  embedded: { type: Boolean, default: false },
  unreadKey: { type: String, default: '' },
  entityTitle: { type: String, default: '任务详情' },
  statusTitle: { type: String, default: '当前状态' },
  nodeTitle: { type: String, default: '当前节点' },
  userTitle: { type: String, default: '申请人' },
  actionTitle: { type: String, default: '操作' },
  /** 列表左侧默认图标：todo / done / started / cc */
  iconPreset: { type: String, default: 'todo' },
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

const ICON_PRESETS = {
  todo: { icon: 'i-material-symbols:pending-actions', tone: 'blue' },
  done: { icon: 'i-material-symbols:task-alt', tone: 'green' },
  started: { icon: 'i-material-symbols:send', tone: 'amber' },
  cc: { icon: 'i-material-symbols:mark-email-unread', tone: 'violet' },
}

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

function resolveRowIcon(row) {
  if (props.iconPreset === 'cc')
    return Number(row?.isRead) === 1 ? 'i-material-symbols:mark-email-read' : 'i-material-symbols:mark-email-unread'
  if (props.iconPreset === 'done') {
    if (Number(row?.status) === 3)
      return 'i-material-symbols:cancel'
    if (Number(row?.status) === 4 || Number(row?.status) === 7)
      return 'i-material-symbols:keyboard-return'
    return 'i-material-symbols:task-alt'
  }
  if (props.iconPreset === 'started') {
    if ([2, 6].includes(Number(row?.status)))
      return 'i-material-symbols:check-circle'
    if ([3, 5, 8].includes(Number(row?.status)))
      return 'i-material-symbols:stop-circle'
    return 'i-material-symbols:schedule'
  }
  // todo：未签收 / 处理中
  if (Number(row?.status) === 0 && !row?.assignee)
    return 'i-material-symbols:assignment-ind'
  return 'i-material-symbols:pending-actions'
}

function resolveRowIconTone(row) {
  if (props.iconPreset === 'cc')
    return Number(row?.isRead) === 1 ? 'green' : 'violet'
  if (props.iconPreset === 'done') {
    if (Number(row?.status) === 3)
      return 'red'
    if (Number(row?.status) === 4 || Number(row?.status) === 7)
      return 'amber'
    return 'green'
  }
  if (props.iconPreset === 'started') {
    if ([2, 6].includes(Number(row?.status)))
      return 'green'
    if ([3, 5, 8].includes(Number(row?.status)))
      return 'red'
    return 'amber'
  }
  if (Number(row?.status) === 0 && !row?.assignee)
    return 'amber'
  return ICON_PRESETS.todo.tone
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

.flow-task-card-list.is-embedded {
  border: 0;
  border-radius: 0;
  background: transparent;
}

.task-list-prepend {
  flex: 0 0 auto;
  padding: 0 12px;
  border-bottom: 1px solid var(--border-light, #e8ecf1);
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
  align-content: flex-start;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 12px;
  gap: 10px;
  scrollbar-gutter: stable;
}

.task-card-list.empty {
  align-items: center;
  justify-content: center;
}

.task-card-list--loading-placeholder {
  min-height: 280px;
}

.task-card-item {
  position: relative;
  display: flex;
  flex: 0 0 auto;
  flex-direction: column;
  gap: 0;
  width: 100%;
  min-height: 132px;
  padding: 0;
  overflow: visible;
  border: 1px solid var(--border-light, #e5e7eb);
  border-radius: 6px;
  background: var(--bg-primary, #fff);
  cursor: pointer;
  transition:
    border-color 150ms ease,
    background-color 150ms ease,
    box-shadow 150ms ease;
}

.task-card-item:hover {
  border-color: #cbd5e1;
  background: #fcfcfd;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.05);
}

.task-card-item.selected {
  border-color: color-mix(in srgb, var(--primary-color, #165dff) 45%, #e5e7eb);
  background: color-mix(in srgb, var(--primary-color, #165dff) 4%, #fff);
}

.task-card-item.unread {
  background: color-mix(in srgb, var(--primary-color, #165dff) 3%, #fff);
}

.task-card-body {
  display: grid;
  grid-template-columns: 40px minmax(0, 1fr);
  gap: 12px;
  align-items: flex-start;
  min-width: 0;
  min-height: 88px;
  flex: 1 1 auto;
  padding: 14px 16px 12px;
}

.task-card-icon {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border: 1px solid transparent;
  border-radius: 6px;
  font-size: 20px;
  line-height: 1;
}

.task-card-icon.blue {
  border-color: #bfdbfe;
  background: #eff6ff;
  color: #2563eb;
}

.task-card-icon.green {
  border-color: #bbf7d0;
  background: #f0fdf4;
  color: #16a34a;
}

.task-card-icon.amber {
  border-color: #fde68a;
  background: #fffbeb;
  color: #d97706;
}

.task-card-icon.red {
  border-color: #fecaca;
  background: #fef2f2;
  color: #dc2626;
}

.task-card-icon.violet {
  border-color: #ddd6fe;
  background: #f5f3ff;
  color: #7c3aed;
}

.task-card-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  padding-top: 1px;
}

.task-card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 8px;
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
  line-height: 1.55;
  margin-bottom: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-card-node-label {
  color: var(--text-tertiary, #8c8c9a);
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
  flex-wrap: wrap;
  gap: 6px 10px;
  min-width: 0;
  color: var(--text-secondary, #5c5c6e);
  font-size: 12px;
}

.task-card-user > span {
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
  box-sizing: border-box;
  display: flex;
  flex: 0 0 auto;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 2px 6px;
  width: 100%;
  min-height: 32px;
  margin: 0;
  padding: 5px 12px;
  overflow: visible;
  border-top: 1px solid var(--border-light, #eef1f5);
  border-radius: 0 0 6px 6px;
  background: #f8fafc;
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
  gap: 2px;
  min-height: 24px;
  padding: 0 4px;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: var(--primary-color, #2563eb);
  cursor: pointer;
  font: inherit;
  font-size: 12px;
  font-weight: 500;
  line-height: 18px;
  white-space: nowrap;
  transition:
    color 150ms ease,
    background-color 150ms ease;
}

:global(.task-row-link-action:hover) {
  background: rgba(37, 99, 235, 0.06);
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
  display: inline-block;
  flex: 0 0 auto;
  width: 1px;
  height: 14px;
  margin: 0 2px;
  background: #e2e8f0;
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
