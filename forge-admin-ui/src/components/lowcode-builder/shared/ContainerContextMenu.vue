<script setup>
/**
 * 容器右键菜单：三列组件插入 + 删除/清空等快捷操作
 */
import { computed } from 'vue'
import { listPageBlockCatalog } from '@/components/lowcode-builder/page/page-schema'

const props = defineProps({
  show: { type: Boolean, default: false },
  x: { type: Number, default: 0 },
  y: { type: Number, default: 0 },
  /** insert | child */
  mode: { type: String, default: 'insert' },
  title: { type: String, default: '' },
  /** child 模式下是否仍允许插入到同一区域 */
  allowInsert: { type: Boolean, default: true },
  excludeBlockTypes: { type: Array, default: () => ['grid-layout', 'tabs', 'box-layout', 'card'] },
})

const emit = defineEmits(['update:show', 'insert', 'action', 'close'])

const actionOptions = computed(() => {
  if (props.mode === 'child') {
    return [
      { key: 'delete', label: '删除组件', danger: true },
      { key: 'duplicate', label: '复制组件' },
    ]
  }
  return [
    { key: 'clear', label: '清空此区域', danger: true },
  ]
})

const showInsertSection = computed(() => props.mode !== 'child' || props.allowInsert)

const insertGroups = computed(() => {
  const exclude = new Set((props.excludeBlockTypes || []).map(String))
  const items = listPageBlockCatalog.filter((item) => {
    const type = String(item?.blockType || '')
    if (!type || exclude.has(type))
      return false
    if (item.hidden)
      return false
    if (item.onlyFor)
      return false
    return true
  })
  const groups = new Map()
  items.forEach((item) => {
    const group = resolveGroup(item)
    if (!groups.has(group.key))
      groups.set(group.key, { key: group.key, label: group.label, items: [] })
    groups.get(group.key).items.push({
      blockType: item.blockType,
      label: item.title || item.blockType,
      desc: item.desc || '',
    })
  })
  return [...groups.values()]
})

function resolveGroup(item = {}) {
  const raw = `${item.group || ''} ${item.blockType || ''} ${item.title || ''}`.toLowerCase()
  if (/layout|tabs|grid|box|card|布局|栅格|标签/.test(raw))
    return { key: 'layout', label: '布局' }
  if (/data|table|form|crud|list|search|数据|列表|表单|查询/.test(raw))
    return { key: 'data', label: '数据' }
  if (/media|audio|video|image|avatar|iframe|媒体|音视频|头像/.test(raw))
    return { key: 'media', label: '媒体' }
  if (/action|button|link|toolbar|操作|按钮|链接/.test(raw))
    return { key: 'action', label: '操作' }
  return { key: 'content', label: '内容' }
}

function close() {
  emit('update:show', false)
  emit('close')
}

function onAction(key) {
  emit('action', key)
  close()
}

function onInsert(blockType) {
  emit('insert', blockType)
  close()
}

function onPanelClick(event) {
  event.stopPropagation()
}
</script>

<template>
  <teleport to="body">
    <div
      v-if="show"
      class="container-ctx-mask"
      @mousedown.self="close"
      @contextmenu.prevent="close"
    >
      <div
        class="container-ctx-menu"
        :style="{ left: `${x}px`, top: `${y}px` }"
        @mousedown="onPanelClick"
        @click="onPanelClick"
      >
        <div class="container-ctx-head">
          <strong>{{ title || (mode === 'child' ? '组件操作' : '插入组件') }}</strong>
          <button type="button" class="container-ctx-close" @click="close">
            ×
          </button>
        </div>

        <div v-if="actionOptions.length" class="container-ctx-actions">
          <button
            v-for="item in actionOptions"
            :key="item.key"
            type="button"
            class="container-ctx-action"
            :class="{ danger: item.danger }"
            @click="onAction(item.key)"
          >
            {{ item.label }}
          </button>
        </div>

        <template v-if="showInsertSection">
          <div class="container-ctx-section-title">
            {{ mode === 'child' ? '在此区域插入组件' : '选择要插入的组件' }}
          </div>
          <div class="container-ctx-scroll">
            <section
              v-for="group in insertGroups"
              :key="group.key"
              class="container-ctx-group"
            >
              <header>{{ group.label }}</header>
              <div class="container-ctx-grid">
                <button
                  v-for="item in group.items"
                  :key="item.blockType"
                  type="button"
                  class="container-ctx-item"
                  :title="item.desc || item.label"
                  @click="onInsert(item.blockType)"
                >
                  <strong>{{ item.label }}</strong>
                </button>
              </div>
            </section>
          </div>
        </template>
      </div>
    </div>
  </teleport>
</template>

<style scoped>
.container-ctx-mask {
  position: fixed;
  inset: 0;
  z-index: 4000;
}
.container-ctx-menu {
  position: fixed;
  z-index: 4001;
  display: grid;
  gap: 10px;
  width: min(480px, calc(100vw - 24px));
  max-height: min(560px, calc(100vh - 24px));
  padding: 10px;
  overflow: hidden;
  border: 1px solid var(--border-light);
  border-radius: 10px;
  background: var(--bg-primary);
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.16);
}
.container-ctx-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.container-ctx-head strong {
  color: var(--text-primary);
  font-size: 13px;
}
.container-ctx-close {
  width: 24px;
  height: 24px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: var(--text-tertiary);
  font-size: 18px;
  line-height: 1;
  cursor: pointer;
}
.container-ctx-close:hover {
  background: var(--bg-tertiary);
  color: var(--text-primary);
}
.container-ctx-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.container-ctx-action {
  padding: 4px 10px;
  border: 1px solid var(--border-light);
  border-radius: 6px;
  background: #fafbfc;
  color: var(--text-secondary);
  font-size: 12px;
  cursor: pointer;
}
.container-ctx-action:hover {
  border-color: #94bfff;
  background: #f0f7ff;
  color: #165dff;
}
.container-ctx-action.danger {
  color: #f53f3f;
}
.container-ctx-action.danger:hover {
  border-color: #fbaca3;
  background: #fff1f0;
  color: #cb2634;
}
.container-ctx-section-title {
  color: var(--text-tertiary);
  font-size: 12px;
}
.container-ctx-scroll {
  display: grid;
  gap: 10px;
  max-height: 380px;
  overflow: auto;
  padding-right: 2px;
}
.container-ctx-group {
  display: grid;
  gap: 6px;
}
.container-ctx-group header {
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 600;
}
.container-ctx-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 6px;
}
.container-ctx-item {
  min-width: 0;
  padding: 8px 6px;
  border: 1px solid #eef0f3;
  border-radius: 6px;
  background: #fafbfc;
  text-align: center;
  cursor: pointer;
}
.container-ctx-item:hover {
  border-color: #94bfff;
  background: #f0f7ff;
}
.container-ctx-item strong {
  display: block;
  overflow: hidden;
  color: var(--text-primary);
  font-size: 12px;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
