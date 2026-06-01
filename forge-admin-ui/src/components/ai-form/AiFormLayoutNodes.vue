<template>
  <n-grid :cols="gridCols" :x-gap="xGap" :y-gap="yGap" class="af-layout-grid">
    <n-gi
      v-for="node in nodes"
      :key="resolveNodeKey(node)"
      :span="resolveNodeSpan(node)"
      :style="node.gridStyle"
      :class="node.gridClass"
    >
      <AiFormItem
        v-if="isFieldNode(node)"
        :field="{ ...node, showFeedback: node.showFeedback ?? showFeedback }"
        :value="formValue[node.field]"
        :form-data="formValue"
        :context="itemContext"
        @update:value="emit('fieldChange', node.field, $event)"
      >
        <template v-for="slotName in Object.keys($slots)" #[slotName]="slotProps">
          <slot :name="slotName" v-bind="slotProps" />
        </template>
      </AiFormItem>

      <AiFormLayoutNodes
        v-else-if="node.nodeType === 'row'"
        :nodes="node.children || []"
        :form-value="formValue"
        :item-context="itemContext"
        :grid-cols="gridCols"
        :x-gap="resolveGap(node.props?.gutter, xGap)"
        :y-gap="yGap"
        :show-feedback="showFeedback"
        @field-change="(...args) => emit('fieldChange', ...args)"
      >
        <template v-for="slotName in Object.keys($slots)" #[slotName]="slotProps">
          <slot :name="slotName" v-bind="slotProps" />
        </template>
      </AiFormLayoutNodes>

      <AiFormLayoutNodes
        v-else-if="node.nodeType === 'col'"
        :nodes="node.children || []"
        :form-value="formValue"
        :item-context="itemContext"
        :grid-cols="1"
        :x-gap="xGap"
        :y-gap="yGap"
        :show-feedback="showFeedback"
        @field-change="(...args) => emit('fieldChange', ...args)"
      >
        <template v-for="slotName in Object.keys($slots)" #[slotName]="slotProps">
          <slot :name="slotName" v-bind="slotProps" />
        </template>
      </AiFormLayoutNodes>

      <n-card
        v-else-if="node.nodeType === 'card'"
        size="small"
        :title="node.label || node.props?.header || undefined"
        :bordered="true"
        :style="node.style"
        :class="node.className"
        class="af-layout-card"
      >
        <AiFormLayoutNodes
          :nodes="node.children || []"
          :form-value="formValue"
          :item-context="itemContext"
          :grid-cols="gridCols"
          :x-gap="xGap"
          :y-gap="yGap"
          :show-feedback="showFeedback"
          @field-change="(...args) => emit('fieldChange', ...args)"
        >
          <template v-for="slotName in Object.keys($slots)" #[slotName]="slotProps">
            <slot :name="slotName" v-bind="slotProps" />
          </template>
        </AiFormLayoutNodes>
      </n-card>

      <n-tabs
        v-else-if="node.nodeType === 'tabs'"
        :type="resolveTabsType(node.props?.type)"
        :placement="resolveTabsPlacement(node.props?.tabPosition)"
        :style="node.style"
        :class="node.className"
        class="af-layout-tabs"
      >
        <n-tab-pane
          v-for="pane in resolvePaneChildren(node, 'tabPane')"
          :key="resolveNodeKey(pane)"
          :name="resolveNodeKey(pane)"
          :tab="pane.label || pane.props?.label || '标签页'"
          :disabled="!!pane.props?.disabled"
        >
          <AiFormLayoutNodes
            :nodes="pane.children || []"
            :form-value="formValue"
            :item-context="itemContext"
            :grid-cols="gridCols"
            :x-gap="xGap"
            :y-gap="yGap"
            :show-feedback="showFeedback"
            @field-change="(...args) => emit('fieldChange', ...args)"
          >
            <template v-for="slotName in Object.keys($slots)" #[slotName]="slotProps">
              <slot :name="slotName" v-bind="slotProps" />
            </template>
          </AiFormLayoutNodes>
        </n-tab-pane>
      </n-tabs>

      <n-collapse
        v-else-if="node.nodeType === 'collapse'"
        :accordion="!!node.props?.accordion"
        :style="node.style"
        :class="node.className"
        class="af-layout-collapse"
      >
        <n-collapse-item
          v-for="item in resolvePaneChildren(node, 'collapseItem')"
          :key="resolveNodeKey(item)"
          :name="resolveNodeKey(item)"
          :title="item.label || item.props?.title || '分组'"
          :disabled="!!item.props?.disabled"
        >
          <AiFormLayoutNodes
            :nodes="item.children || []"
            :form-value="formValue"
            :item-context="itemContext"
            :grid-cols="gridCols"
            :x-gap="xGap"
            :y-gap="yGap"
            :show-feedback="showFeedback"
            @field-change="(...args) => emit('fieldChange', ...args)"
          >
            <template v-for="slotName in Object.keys($slots)" #[slotName]="slotProps">
              <slot :name="slotName" v-bind="slotProps" />
            </template>
          </AiFormLayoutNodes>
        </n-collapse-item>
      </n-collapse>

      <n-divider
        v-else-if="node.nodeType === 'divider'"
        :title-placement="node.props?.contentPosition || node.props?.titlePlacement || 'left'"
        :style="node.style"
        :class="node.className"
      >
        {{ node.label }}
      </n-divider>

      <AiFormLayoutNodes
        v-else
        :nodes="node.children || []"
        :form-value="formValue"
        :item-context="itemContext"
        :grid-cols="gridCols"
        :x-gap="xGap"
        :y-gap="yGap"
        :show-feedback="showFeedback"
        @field-change="(...args) => emit('fieldChange', ...args)"
      >
        <template v-for="slotName in Object.keys($slots)" #[slotName]="slotProps">
          <slot :name="slotName" v-bind="slotProps" />
        </template>
      </AiFormLayoutNodes>
    </n-gi>
  </n-grid>
</template>

<script setup>
import AiFormItem from './AiFormItem.vue'

defineOptions({
  name: 'AiFormLayoutNodes',
})

const props = defineProps({
  nodes: {
    type: Array,
    default: () => [],
  },
  formValue: {
    type: Object,
    default: () => ({}),
  },
  itemContext: {
    type: Object,
    default: () => ({}),
  },
  gridCols: {
    type: Number,
    default: 1,
  },
  xGap: {
    type: Number,
    default: 12,
  },
  yGap: {
    type: Number,
    default: 0,
  },
  showFeedback: {
    type: Boolean,
    default: true,
  },
})

const emit = defineEmits(['fieldChange'])

function isFieldNode(node = {}) {
  return !node.nodeType || node.nodeType === 'field'
}

function resolveNodeSpan(node = {}) {
  if (['row', 'card', 'tabs', 'collapse', 'divider'].includes(node.nodeType))
    return props.gridCols
  return Math.max(1, Math.min(props.gridCols, Number(node.span || 1)))
}

function resolveNodeKey(node = {}) {
  return node.key || node.field || node.id || `${node.nodeType || node.type || 'node'}_${node.label || ''}`
}

function resolveGap(value, fallback) {
  const number = Number(value)
  return Number.isFinite(number) ? number : fallback
}

function resolveTabsType(value) {
  return value === 'card' ? 'card' : 'line'
}

function resolveTabsPlacement(value) {
  return ['left', 'right', 'top', 'bottom'].includes(value) ? value : 'top'
}

function resolvePaneChildren(node = {}, paneType) {
  const children = Array.isArray(node.children) ? node.children : []
  const panes = children.filter(child => child?.nodeType === paneType)
  if (panes.length)
    return panes
  return [{
    key: `${resolveNodeKey(node)}_pane`,
    label: node.label,
    props: {},
    children,
  }]
}
</script>

<style scoped>
.af-layout-grid {
  min-width: 0;
}

.af-layout-card,
.af-layout-tabs,
.af-layout-collapse {
  width: 100%;
}

.af-layout-card :deep(.n-card__content) {
  padding-top: 12px;
}
</style>
