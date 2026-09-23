<template>
  <view class="lowcode-layout">
    <template v-for="node in visibleNodes" :key="nodeKey(node)">
      <!-- Card container -->
      <CardSection
        v-if="isCardNode(node)"
        :title="node.label || node.props?.header || ''"
        :collapsible="isCollapsibleCard(node)"
        :collapsed-by-default="node.props?.collapsedByDefault === true"
      >
        <LowcodeLayoutNodes
          :ref="instance => setChildLayoutRef(nodeKey(node), instance)"
          :nodes="node.children || []"
          :data="data"
          :dict-options="dictOptions"
          :readonly="readonly"
          :context="context"
          :field-linkages="fieldLinkages"
          @update:data="(...args) => emit('update:data', ...args)"
          @field-event="(...args) => emit('field-event', ...args)"
          @action="(...args) => emit('action', ...args)"
        />
      </CardSection>

      <!-- Mobile containers: desktop grids/tables collapse into a readable stack. -->
      <view v-else-if="isContainerNode(node)" :class="containerClass(node)">
        <LowcodeLayoutNodes
          :ref="instance => setChildLayoutRef(nodeKey(node), instance)"
          :nodes="node.children || []"
          :data="data"
          :dict-options="dictOptions"
          :readonly="readonly"
          :context="context"
          :field-linkages="fieldLinkages"
          @update:data="(...args) => emit('update:data', ...args)"
          @field-event="(...args) => emit('field-event', ...args)"
          @action="(...args) => emit('action', ...args)"
        />
      </view>

      <!-- Tabs -->
      <view v-else-if="isTabsNode(node)" class="lowcode-layout-tabs">
        <AiTabs v-model="tabStates[nodeKey(node)]" :tabs="tabLabels(node)">
          <AiTab
            v-for="(pane, pIndex) in paneChildren(node)"
            :key="pane.key || pIndex"
            :index="pIndex"
          >
            <LowcodeLayoutNodes
              :ref="instance => setChildLayoutRef(`${nodeKey(node)}:${nodeKey(pane)}`, instance)"
              :nodes="pane.children || []"
              :data="data"
              :dict-options="dictOptions"
              :readonly="readonly"
              :context="context"
              :field-linkages="fieldLinkages"
              @update:data="(...args) => emit('update:data', ...args)"
              @field-event="(...args) => emit('field-event', ...args)"
              @action="(...args) => emit('action', ...args)"
            />
          </AiTab>
        </AiTabs>
      </view>

      <!-- Collapse -->
      <view v-else-if="isCollapseNode(node)" class="lowcode-layout-collapse">
        <CardSection
          v-for="item in paneChildren(node)"
          :key="item.key"
          :title="item.label || '分组'"
          :collapsible="true"
          :collapsed-by-default="node.props?.accordion !== true && item.props?.collapsedByDefault === true"
        >
          <LowcodeLayoutNodes
            :ref="instance => setChildLayoutRef(`${nodeKey(node)}:${nodeKey(item)}`, instance)"
            :nodes="item.children || []"
            :data="data"
            :dict-options="dictOptions"
            :readonly="readonly"
            :context="context"
            :field-linkages="fieldLinkages"
            @update:data="(...args) => emit('update:data', ...args)"
            @field-event="(...args) => emit('field-event', ...args)"
            @action="(...args) => emit('action', ...args)"
          />
        </CardSection>
      </view>

      <!-- Leaf field -->
      <LowcodeField
        v-else-if="isFieldNode(node)"
        :ref="instance => setFieldRef(node.field, instance)"
        :field="enrichField(node)"
        :model-value="data[node.field]"
        :options="fieldOptions(node)"
        :readonly="readonly"
        :form-data="data"
        :context="context"
        :error="errors[node.field]"
        @update:model-value="updateField(node, $event)"
        @blur="emit('field-event', { trigger: 'BLUR', field: node, data })"
        @change="emit('field-event', { trigger: 'CHANGE', field: node, data })"
        @scan="emit('field-event', { trigger: 'SCAN_COMPLETE', field: node, data, scan: $event })"
        @selection="applySelection(node, $event)"
      />

      <LowcodeStaticNode
        v-else
        :node="node"
        :data="data"
        :readonly="readonly"
        @action="action => emit('action', action)"
      />
    </template>
  </view>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import AiTab from '@/components/AiTab.vue'
import AiTabs from '@/components/AiTabs.vue'
import CardSection from './CardSection.vue'
import LowcodeField from './LowcodeField.vue'
import LowcodeStaticNode from './LowcodeStaticNode.vue'
import { normalizeMobileComponentType } from './mobile-component-registry'
import {
  applyFieldLinkageChange,
  filterFieldOptionsByLinkage,
  resolveFieldControl,
  resolveFieldLinkageContext,
} from '@/utils/lowcode-runtime'

defineOptions({ name: 'LowcodeLayoutNodes' })

const props = defineProps({
  nodes: { type: Array, default: () => [] },
  data: { type: Object, default: () => ({}) },
  dictOptions: { type: Object, default: () => ({}) },
  readonly: { type: Boolean, default: false },
  context: { type: Object, default: () => ({}) },
  fieldLinkages: { type: Array, default: () => [] },
})

const emit = defineEmits(['update:data', 'field-event', 'action'])
const errors = reactive({})
const tabStates = reactive({})
const fieldRefs = new Map()
const childLayoutRefs = new Map()

// ── Node type helpers ──

function resolveType(node = {}) {
  return normalizeMobileComponentType(node.nodeType || node.type || node.componentKey || '')
}

function isCardNode(node) {
  return resolveType(node) === 'card'
}

function isContainerNode(node) {
  return ['grid', 'col', 'table', 'tableCell', 'box', 'space'].includes(resolveType(node))
}

function containerClass(node) {
  return ['lowcode-layout-container', `is-${resolveType(node)}`]
}

function isCollapsibleCard(node) {
  return node.props?.collapsible === true
}

function isTabsNode(node) {
  return resolveType(node) === 'tabs'
}

function isCollapseNode(node) {
  return resolveType(node) === 'collapse'
}

function isFieldNode(node) {
  return !isCardLikeNode(node) && !isTabsNode(node) && !isCollapseNode(node) && node.field
}

function nodeKey(node) {
  return node.key || node.id || node.field || `${resolveType(node)}_${node.label || ''}`
}

function paneChildren(node) {
  const children = Array.isArray(node.children) ? node.children : []
  const panes = children.filter(c => ['tabPane', 'collapseItem'].includes(resolveType(c)))
  if (panes.length) return panes
  return [{ key: `${nodeKey(node)}_pane`, label: node.label, props: {}, children }]
}

function tabLabels(node) {
  return paneChildren(node).map(pane => pane.label || '标签页')
}

// ── Visible nodes with runtime control ──

const visibleNodes = computed(() => props.nodes
  .filter((node) => {
    if (isFieldNode(node)) {
      const control = resolveFieldControl(node, {
        record: props.data,
        formData: props.data,
        row: props.data,
        route: { query: props.context.routeQuery || {} },
        user: props.context.user || {},
      })
      return control.visible
    }
    // Layout nodes are always visible (their children handle own visibility)
    return true
  }))

// ── Field enrichment (same logic as LowcodeForm) ──

function enrichField(node) {
  return {
    ...node,
    props: {
      ...(node.props || {}),
      linkageContext: resolveFieldLinkageContext(props.fieldLinkages, node.field, props.data),
    },
    __runtimeControl: resolveFieldControl(node, {
      record: props.data,
      formData: props.data,
      row: props.data,
      route: { query: props.context.routeQuery || {} },
      user: props.context.user || {},
    }),
  }
}

function fieldOptions(node) {
  if (node.dictType || node.props?.dictType) {
    const options = props.dictOptions[node.dictType || node.props?.dictType] || []
    return filterFieldOptionsByLinkage(options, node.props?.linkageContext)
  }
  const source = node.props?.options || node.options
  if (Array.isArray(source)) {
    return filterFieldOptionsByLinkage(
      source.map(item => typeof item === 'object' ? item : { label: String(item), value: item }),
      node.props?.linkageContext,
    )
  }
  return []
}

function updateField(node, value) {
  props.data[node.field] = value
  applyFieldLinkageChange(props.fieldLinkages, node.field, props.data)
  delete errors[node.field]
  emit('update:data', props.data)
}

function applySelection(node, payload = {}) {
  const patch = payload.patch && typeof payload.patch === 'object' ? payload.patch : {}
  Object.assign(props.data, patch)
  applyFieldLinkageChange(props.fieldLinkages, node.field, props.data)
  emit('update:data', props.data)
  emit('field-event', { trigger: 'SELECT', field: node, data: props.data, selection: payload })
}

function setFieldRef(field, instance) {
  if (instance) fieldRefs.set(field, instance)
  else fieldRefs.delete(field)
}

function setChildLayoutRef(key, instance) {
  if (instance) childLayoutRefs.set(key, instance)
  else childLayoutRefs.delete(key)
}

// ── Validation ──

function collectFieldErrors(nodes, fieldErrors) {
  for (const node of nodes) {
    if (isFieldNode(node)) {
      const control = resolveFieldControl(node, { record: props.data, formData: props.data, row: props.data })
      if (!control.visible || !control.required || props.readonly || control.readonly) continue
      const value = props.data[node.field]
      if (value === undefined || value === null || value === '' || (Array.isArray(value) && !value.length)) {
        fieldErrors[node.field] = node.requiredMessage || `请输入${node.label}`
      }
    }
    else if (Array.isArray(node.children)) {
      collectFieldErrors(node.children, fieldErrors)
    }
  }
}

function validate() {
  Object.keys(errors).forEach(key => delete errors[key])
  collectFieldErrors(props.nodes, errors)
  let valid = Object.keys(errors).length === 0
  for (const instance of fieldRefs.values()) {
    if (instance?.validate?.() === false) valid = false
  }
  for (const instance of childLayoutRefs.values()) {
    if (instance?.validate?.() === false) valid = false
  }
  return valid
}

defineExpose({ validate })
</script>

<style lang="scss" scoped>
.lowcode-layout { display: flex; flex-direction: column; }
.lowcode-layout-container { min-width: 0; }
.lowcode-layout-container.is-grid, .lowcode-layout-container.is-table, .lowcode-layout-container.is-box { margin-bottom: 18rpx; }
.lowcode-layout-container.is-table { padding: 14rpx; border: 1rpx solid var(--forge-color-border, #c9cdd4); border-radius: 12rpx; }
.lowcode-layout-container.is-space > :deep(.lowcode-layout) { gap: 16rpx; }
.lowcode-layout-tabs { margin-bottom: 24rpx; }
.lowcode-layout-collapse { margin-bottom: 24rpx; }
</style>
