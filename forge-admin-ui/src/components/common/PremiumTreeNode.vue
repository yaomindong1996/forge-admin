<template>
  <div class="premium-tree-node" role="none">
    <span
      v-if="level > 0"
      class="tree-guide tree-guide-vertical"
      :style="guideVerticalStyle"
    />
    <div
      class="premium-tree-row"
      :class="[
        `tone-${nodeTone}`,
        {
          'is-selected': isSelected,
          'has-children': hasChildren,
          'is-expanded': isExpanded,
        },
      ]"
      :style="{ paddingLeft: `${level * 14 + 2}px` }"
      role="treeitem"
      :aria-selected="isSelected"
      :aria-expanded="hasChildren ? isExpanded : undefined"
      tabindex="0"
      @click="handleSelect"
      @keydown.enter.prevent="handleSelect"
      @keydown.space.prevent="handleSelect"
      @keydown.right.prevent="handleArrowRight"
      @keydown.left.prevent="handleArrowLeft"
    >
      <button
        class="premium-tree-switcher"
        :class="{ 'is-visible': hasChildren }"
        type="button"
        :aria-label="isExpanded ? '折叠节点' : '展开节点'"
        :tabindex="hasChildren ? 0 : -1"
        @click.stop="handleToggle"
      >
        <i v-if="hasChildren" class="i-lucide:chevron-right" />
      </button>

      <input
        v-if="checkable"
        class="premium-tree-checkbox"
        type="checkbox"
        :checked="isChecked"
        :indeterminate="isIndeterminate"
        :aria-checked="isIndeterminate ? 'mixed' : isChecked"
        @click.stop
        @change="handleCheckChange"
      >

      <span class="premium-tree-icon">
        <i :class="nodeIcon" />
      </span>

      <span class="premium-tree-copy" :title="nodeTooltip">
        <span class="premium-tree-title" :title="nodeLabel">{{ nodeLabel }}</span>
        <small v-if="nodeSubtitle" class="premium-tree-subtitle" :title="nodeSubtitle">{{ nodeSubtitle }}</small>
      </span>

      <span v-if="nodeMeta" class="premium-tree-meta">
        <span v-if="nodeMeta.label">{{ nodeMeta.label }}</span>
        <strong>{{ nodeMeta.value }}</strong>
      </span>

      <span v-if="nodeActions.length" class="premium-tree-actions" @click.stop>
        <button
          v-for="action in nodeActions"
          :key="action.key || action.title || action.label"
          class="premium-tree-action"
          :class="[
            action.type ? `type-${action.type}` : '',
            { 'is-disabled': resolveActionDisabled(action) },
          ]"
          type="button"
          :title="action.title || action.label"
          :aria-label="action.title || action.label"
          :disabled="resolveActionDisabled(action)"
          @click.stop="handleActionClick(action)"
        >
          <i :class="action.icon || 'i-material-symbols:more-horiz-rounded'" />
        </button>
      </span>
    </div>

    <Transition name="premium-tree-children">
      <div v-if="hasChildren && isExpanded" class="premium-tree-children" role="group">
        <div class="premium-tree-children-inner">
          <PremiumTreeNode
            v-for="child in nodeChildren"
            :key="getNodeKey(child)"
            :node="child"
            :level="level + 1"
            :selected-key="selectedKey"
            :expanded-key-set="expandedKeySet"
            :checked-key-set="checkedKeySet"
            :indeterminate-key-set="indeterminateKeySet"
            :key-field="keyField"
            :label-field="labelField"
            :children-field="childrenField"
            :checkable="checkable"
            :get-node-icon="getNodeIcon"
            :get-node-meta="getNodeMeta"
            :get-node-subtitle="getNodeSubtitle"
            :get-node-tone="getNodeTone"
            :actions="actions"
            :show-meta="showMeta"
            :show-subtitle="showSubtitle"
            @select="$emit('select', $event)"
            @check="(treeNode, checked) => $emit('check', treeNode, checked)"
            @toggle="(treeNode, nextExpanded) => $emit('toggle', treeNode, nextExpanded)"
            @action="(action, treeNode) => $emit('action', action, treeNode)"
          />
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { computed } from 'vue'

defineOptions({ name: 'PremiumTreeNode' })

const props = defineProps({
  node: {
    type: Object,
    required: true,
  },
  level: {
    type: Number,
    default: 0,
  },
  selectedKey: {
    type: [String, Number],
    default: null,
  },
  expandedKeySet: {
    type: Object,
    required: true,
  },
  checkedKeySet: {
    type: Object,
    default: () => new Set(),
  },
  indeterminateKeySet: {
    type: Object,
    default: () => new Set(),
  },
  keyField: {
    type: String,
    default: 'key',
  },
  labelField: {
    type: String,
    default: 'label',
  },
  childrenField: {
    type: String,
    default: 'children',
  },
  checkable: {
    type: Boolean,
    default: false,
  },
  getNodeIcon: {
    type: Function,
    default: null,
  },
  getNodeMeta: {
    type: Function,
    default: null,
  },
  getNodeSubtitle: {
    type: Function,
    default: null,
  },
  getNodeTone: {
    type: Function,
    default: null,
  },
  actions: {
    type: [Array, Function],
    default: () => [],
  },
  showMeta: {
    type: Boolean,
    default: false,
  },
  showSubtitle: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['select', 'toggle', 'check', 'action'])

const nodeKey = computed(() => getNodeKey(props.node))
const nodeChildren = computed(() => props.node?.[props.childrenField] || [])
const hasChildren = computed(() => nodeChildren.value.length > 0)
const isExpanded = computed(() => props.expandedKeySet.has(nodeKey.value))
const isSelected = computed(() => props.selectedKey === nodeKey.value)
const isChecked = computed(() => props.checkedKeySet.has(nodeKey.value))
const isIndeterminate = computed(() => props.indeterminateKeySet.has(nodeKey.value))
const nodeLabel = computed(() => props.node?.[props.labelField] || props.node?.label || props.node?.title || '')
const nodeSubtitle = computed(() => {
  if (!props.showSubtitle)
    return ''
  return props.getNodeSubtitle?.(props.node) || props.node?.subtitle || props.node?.path || ''
})
const nodeIcon = computed(() => props.getNodeIcon?.(props.node) || props.node?.icon
  || (hasChildren.value ? 'i-lucide:folder' : 'i-lucide:file-text'))
const nodeMeta = computed(() => {
  if (!props.showMeta)
    return null
  return props.getNodeMeta?.(props.node) || null
})
const nodeTone = computed(() => props.getNodeTone?.(props.node) || 'default')
const nodeActions = computed(() => resolveNodeActions(props.node))
const nodeTooltip = computed(() => [nodeLabel.value, nodeSubtitle.value].filter(Boolean).join(' / '))

const guideLeft = computed(() => `${(props.level - 1) * 14 + 11}px`)
const guideVerticalStyle = computed(() => ({
  left: guideLeft.value,
}))

function getNodeKey(node = {}) {
  return node[props.keyField] ?? node.key ?? node.id
}

function handleSelect() {
  if (props.checkable) {
    emit('check', props.node, !isChecked.value)
    return
  }
  emit('select', props.node)
  if (hasChildren.value && !isExpanded.value)
    emit('toggle', props.node, true)
}

function handleCheckChange(event) {
  emit('check', props.node, event.target.checked)
}

function handleToggle() {
  if (hasChildren.value)
    emit('toggle', props.node)
}

function handleActionClick(action) {
  if (resolveActionDisabled(action))
    return
  action.onClick?.(props.node, action)
  emit('action', action, props.node)
}

function handleArrowRight() {
  if (hasChildren.value && !isExpanded.value)
    emit('toggle', props.node, true)
}

function handleArrowLeft() {
  if (hasChildren.value && isExpanded.value)
    emit('toggle', props.node, false)
}

function resolveNodeActions(node) {
  const actions = typeof props.actions === 'function'
    ? props.actions(node)
    : props.actions
  return (Array.isArray(actions) ? actions : [])
    .filter((action) => {
      if (!action)
        return false
      if (typeof action.visible === 'function')
        return action.visible(node) !== false
      return action.visible !== false
    })
}

function resolveActionDisabled(action) {
  if (typeof action.disabled === 'function')
    return action.disabled(props.node)
  return action.disabled === true
}
</script>

<style scoped>
.premium-tree-node {
  position: relative;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.tree-guide {
  position: absolute;
  z-index: 0;
  pointer-events: none;
  background: var(--border-light, #e5e7eb);
}

.tree-guide-vertical {
  top: -3px;
  bottom: 13px;
  width: 1px;
}

.premium-tree-row {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  min-width: 0;
  min-height: 28px;
  padding-top: 2px;
  padding-right: 6px;
  padding-bottom: 2px;
  margin: 0;
  border: 1px solid transparent;
  border-radius: 4px;
  color: var(--text-secondary, #4b5563);
  cursor: pointer;
  outline: none;
  transition:
    background-color 0.18s ease,
    border-color 0.18s ease,
    color 0.18s ease;
}

.premium-tree-row:hover {
  background: color-mix(in srgb, var(--primary-color, #2563eb) 4%, var(--bg-secondary, #f6f8fb));
  border-color: transparent;
}

.premium-tree-row:focus-visible {
  border-color: color-mix(in srgb, var(--primary-color, #2563eb) 34%, var(--border-light, #e5e7eb));
  box-shadow: 0 0 0 2px color-mix(in srgb, var(--primary-color, #2563eb) 10%, transparent);
}

.premium-tree-row.is-selected {
  background: color-mix(in srgb, var(--primary-color, #2563eb) 9%, var(--bg-primary, #fff));
  border-color: color-mix(in srgb, var(--primary-color, #2563eb) 20%, var(--border-light, #e5e7eb));
  color: var(--primary-color, #2563eb);
  box-shadow: none;
}

.premium-tree-switcher {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 18px;
  height: 18px;
  padding: 0;
  margin: 0 2px 0 0;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: transparent;
  cursor: default;
}

.premium-tree-switcher.is-visible {
  color: var(--text-tertiary, #9ca3af);
  cursor: pointer;
}

.premium-tree-switcher.is-visible:hover {
  background: var(--bg-secondary, #f6f8fb);
  color: var(--text-secondary, #4b5563);
}

.premium-tree-switcher i {
  font-size: 15px;
  transition: transform 0.2s cubic-bezier(0.2, 0.8, 0.2, 1);
}

.premium-tree-row.is-expanded .premium-tree-switcher i {
  transform: rotate(90deg);
}

.premium-tree-checkbox {
  flex: 0 0 auto;
  width: 15px;
  height: 15px;
  margin: 0 6px 0 0;
  accent-color: var(--primary-color, #2563eb);
  cursor: pointer;
}

.premium-tree-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 18px;
  height: 18px;
  margin-right: 5px;
  border-radius: 4px;
  background: transparent;
  color: var(--text-tertiary, #9ca3af);
  transition: color 0.18s ease;
}

.premium-tree-icon i {
  font-size: 14px;
  line-height: 1;
}

.premium-tree-copy {
  display: flex;
  align-items: baseline;
  min-width: 0;
  overflow: hidden;
  flex: 1;
  gap: 6px;
}

.premium-tree-title {
  min-width: 0;
  overflow: hidden;
  color: var(--text-secondary, #4b5563);
  font-size: 12px;
  font-weight: 500;
  line-height: 18px;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color 0.18s ease;
}

.premium-tree-subtitle {
  min-width: 0;
  overflow: hidden;
  color: var(--text-tertiary, #9ca3af);
  font-size: 11px;
  line-height: 16px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.premium-tree-meta {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  min-width: 38px;
  gap: 4px;
  padding: 0 6px;
  border: 1px solid var(--border-light, #e5e7eb);
  border-radius: 999px;
  background: var(--bg-secondary, #f6f8fb);
  color: var(--text-tertiary, #9ca3af);
  font-size: 11px;
  font-weight: 650;
  line-height: 15px;
}

.premium-tree-meta strong {
  color: var(--text-secondary, #4b5563);
  font-size: 11px;
  font-weight: 750;
}

.premium-tree-actions {
  display: inline-flex;
  align-items: center;
  flex: 0 0 auto;
  gap: 2px;
  margin-left: 4px;
  opacity: 0;
  pointer-events: none;
  transform: translateX(4px);
  transition:
    opacity 0.16s ease,
    transform 0.16s ease;
}

.premium-tree-row:hover .premium-tree-actions,
.premium-tree-row:focus-within .premium-tree-actions,
.premium-tree-row.is-selected .premium-tree-actions {
  opacity: 1;
  pointer-events: auto;
  transform: translateX(0);
}

.premium-tree-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  padding: 0;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: var(--text-tertiary, #9ca3af);
  cursor: pointer;
  transition:
    background-color 0.16s ease,
    color 0.16s ease;
}

.premium-tree-action:hover {
  background: var(--bg-secondary, #f6f8fb);
  color: var(--primary-color, #2563eb);
}

.premium-tree-action.type-error:hover,
.premium-tree-action.type-danger:hover {
  background: rgba(239, 68, 68, 0.1);
  color: #dc2626;
}

.premium-tree-action.is-disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.premium-tree-action i {
  font-size: 14px;
  line-height: 1;
}

.premium-tree-row.is-selected .premium-tree-title {
  color: var(--text-primary, #111827);
  font-weight: 600;
}

.premium-tree-row.tone-root .premium-tree-icon {
  color: var(--text-tertiary, #9ca3af);
}

.premium-tree-row.tone-folder .premium-tree-icon {
  color: #64748b;
}

.premium-tree-row.tone-menu .premium-tree-icon {
  color: #64748b;
}

.premium-tree-row.tone-action .premium-tree-icon {
  color: #64748b;
}

.premium-tree-row.tone-api .premium-tree-icon {
  color: #64748b;
}

.premium-tree-row.is-selected .premium-tree-icon {
  color: var(--primary-color, #2563eb);
}

.premium-tree-row.is-selected .premium-tree-subtitle {
  color: var(--text-tertiary, #9ca3af);
}

.premium-tree-row.is-selected .premium-tree-meta {
  background: var(--bg-primary, #fff);
  border-color: color-mix(in srgb, var(--primary-color, #2563eb) 18%, var(--border-light, #e5e7eb));
}

.premium-tree-children {
  position: relative;
  overflow: hidden;
}

.premium-tree-children-inner {
  min-height: 0;
  overflow: hidden;
}

.premium-tree-children-enter-active,
.premium-tree-children-leave-active {
  display: grid;
  transition:
    grid-template-rows 0.22s cubic-bezier(0.2, 0.8, 0.2, 1),
    opacity 0.18s ease;
}

.premium-tree-children-enter-from,
.premium-tree-children-leave-to {
  grid-template-rows: 0fr;
  opacity: 0;
}

.premium-tree-children-enter-to,
.premium-tree-children-leave-from {
  grid-template-rows: 1fr;
  opacity: 1;
}

:global(.dark) .premium-tree-row {
  color: #cbd5e1;
}

:global(.dark) .premium-tree-row:hover {
  background: rgba(30, 41, 59, 0.86);
  border-color: transparent;
}

:global(.dark) .premium-tree-row.is-selected {
  background: rgba(30, 64, 175, 0.22);
  border-color: transparent;
  color: #dbeafe;
  box-shadow: inset 2px 0 0 #60a5fa;
}

:global(.dark) .premium-tree-title {
  color: #cbd5e1;
}

:global(.dark) .premium-tree-row.is-selected .premium-tree-title {
  color: #eff6ff;
}

:global(.dark) .premium-tree-subtitle {
  color: #64748b;
}

:global(.dark) .premium-tree-row.is-selected .premium-tree-subtitle {
  color: #bfdbfe;
}

:global(.dark) .premium-tree-action:hover {
  background: rgba(30, 41, 59, 0.86);
  color: #bfdbfe;
}

:global(.dark) .premium-tree-action.type-error:hover,
:global(.dark) .premium-tree-action.type-danger:hover {
  background: rgba(239, 68, 68, 0.14);
  color: #fca5a5;
}

:global(.dark) .tree-guide {
  background: rgba(100, 116, 139, 0.36);
}

@media (prefers-reduced-motion: reduce) {
  .premium-tree-row,
  .premium-tree-switcher i,
  .premium-tree-children-enter-active,
  .premium-tree-children-leave-active {
    transition: none;
  }

  .premium-tree-row:hover {
    transform: none;
  }
}
</style>
