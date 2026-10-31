<template>
  <div class="premium-tree" role="tree">
    <PremiumTreeNode
      v-for="node in data"
      :key="getNodeKey(node)"
      :node="node"
      :level="0"
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
      @select="handleSelect"
      @check="handleCheck"
      @toggle="handleToggle"
      @action="handleAction"
    />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import PremiumTreeNode from './PremiumTreeNode.vue'

defineOptions({ name: 'PremiumTree' })

const props = defineProps({
  data: {
    type: Array,
    default: () => [],
  },
  cascadeData: {
    type: Array,
    default: null,
  },
  selectedKeys: {
    type: Array,
    default: () => [],
  },
  expandedKeys: {
    type: Array,
    default: () => [],
  },
  checkedKeys: {
    type: Array,
    default: () => [],
  },
  checkable: {
    type: Boolean,
    default: false,
  },
  cascade: {
    type: Boolean,
    default: true,
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

const emit = defineEmits(['update:selected-keys', 'update:expanded-keys', 'update:checked-keys', 'select', 'check', 'action'])

const selectedKey = computed(() => props.selectedKeys?.[0])
const expandedKeySet = computed(() => new Set(props.expandedKeys || []))
const explicitCheckedKeySet = computed(() => new Set(props.checkedKeys || []))
const cascadeSourceData = computed(() => props.cascadeData || props.data || [])
const normalizedCheckedKeySet = computed(() => {
  if (!props.checkable || !props.cascade)
    return explicitCheckedKeySet.value
  return new Set(normalizeCascadeCheckedKeys(props.checkedKeys || [], cascadeSourceData.value))
})
const cascadeState = computed(() => {
  if (!props.checkable || !props.cascade) {
    return {
      checked: explicitCheckedKeySet.value,
      indeterminate: new Set(),
    }
  }

  return collectCascadeState(cascadeSourceData.value, normalizedCheckedKeySet.value)
})
const checkedKeySet = computed(() => cascadeState.value.checked)
const indeterminateKeySet = computed(() => {
  if (!props.checkable || !props.cascade)
    return new Set()
  return cascadeState.value.indeterminate
})

function getNodeKey(node = {}) {
  return node[props.keyField] ?? node.key ?? node.id
}

function handleSelect(node) {
  const key = getNodeKey(node)
  emit('update:selected-keys', key === undefined ? [] : [key], node)
  emit('select', node)
}

function handleCheck(node, checked) {
  const keys = new Set(props.checkedKeys || [])
  const cascadeNode = props.cascade ? findNodeByKey(getNodeKey(node), cascadeSourceData.value) || node : node
  const targetKeys = props.cascade ? collectLeafNodeKeys(cascadeNode) : [getNodeKey(node)]

  targetKeys.forEach((key) => {
    if (key === undefined)
      return
    if (checked)
      keys.add(key)
    else
      keys.delete(key)
  })

  const nextKeys = props.cascade
    ? normalizeCascadeCheckedKeys(Array.from(keys), cascadeSourceData.value)
    : Array.from(keys)
  emit('update:checked-keys', nextKeys, node)
  emit('check', nextKeys, node)
}

function handleToggle(node, nextExpanded) {
  const key = getNodeKey(node)
  if (key === undefined)
    return

  const keys = new Set(props.expandedKeys || [])
  const shouldExpand = typeof nextExpanded === 'boolean' ? nextExpanded : !keys.has(key)
  if (shouldExpand)
    keys.add(key)
  else
    keys.delete(key)

  emit('update:expanded-keys', Array.from(keys), node)
}

function handleAction(action, node) {
  emit('action', action, node)
}

function collectLeafNodeKeys(node = {}) {
  const keys = []
  const walk = (item) => {
    const key = getNodeKey(item)
    const children = item?.[props.childrenField] || []
    if (!children.length && key !== undefined) {
      keys.push(key)
      return
    }
    children.forEach(walk)
  }
  walk(node)
  return keys
}

function normalizeCascadeCheckedKeys(keys = [], nodes = []) {
  const explicitSet = new Set(keys)
  const result = new Set()

  const walk = (node = {}, ancestorChecked = false) => {
    const key = getNodeKey(node)
    const children = node?.[props.childrenField] || []
    const currentChecked = ancestorChecked || (key !== undefined && explicitSet.has(key))

    if (!children.length) {
      if (currentChecked && key !== undefined)
        result.add(key)
      return
    }

    children.forEach(child => walk(child, currentChecked))
  }

  nodes.forEach(node => walk(node))
  return Array.from(result)
}

function collectCascadeState(nodes = [], checkedSet) {
  const checked = new Set()
  const indeterminate = new Set()

  const walk = (node = {}) => {
    const key = getNodeKey(node)
    const children = node?.[props.childrenField] || []

    if (!children.length) {
      const isChecked = key !== undefined && checkedSet.has(key)
      if (isChecked)
        checked.add(key)
      return { checked: isChecked, partial: false }
    }

    const childStates = children.map(child => walk(child))
    const allChildrenChecked = childStates.length > 0 && childStates.every(item => item.checked)
    const hasCheckedOrPartialChild = childStates.some(item => item.checked || item.partial)

    if (key !== undefined && allChildrenChecked)
      checked.add(key)
    else if (key !== undefined && hasCheckedOrPartialChild)
      indeterminate.add(key)

    return {
      checked: allChildrenChecked,
      partial: !allChildrenChecked && hasCheckedOrPartialChild,
    }
  }

  nodes.forEach((node) => {
    walk(node)
  })

  return { checked, indeterminate }
}

function findNodeByKey(targetKey, nodes = []) {
  if (targetKey === undefined)
    return null

  for (const node of nodes) {
    if (getNodeKey(node) === targetKey)
      return node

    const match = findNodeByKey(targetKey, node?.[props.childrenField] || [])
    if (match)
      return match
  }

  return null
}
</script>

<style scoped>
.premium-tree {
  width: 100%;
  min-width: 0;
  padding: 0;
}
</style>
