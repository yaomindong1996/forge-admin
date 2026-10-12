<template>
  <div
    class="runtime-list-grid-flow"
    :class="{
      'is-fill': fillHost,
      'has-tree': Boolean(treeBlock),
      'is-tree-collapsed': treeCollapsed,
    }"
  >
    <aside
      v-if="treeBlock"
      class="runtime-list-grid-flow__tree"
      :class="{ 'is-collapsed': treeCollapsed }"
    >
      <GridBlockRenderer
        :block="treeBlock"
        :fields="fields"
        readonly
        runtime-interactive
        suppress-runtime-list-grid
        :runtime-crud-props="nestedRuntimeCrudProps"
        :runtime-crud-loading="runtimeCrudLoading"
        :data-source-configured="dataSourceConfigured"
        :runtime-extension-hooks="runtimeExtensionHooks"
        :block-fields-resolver="blockFieldsResolver"
        :runtime-crud-props-resolver="nestedRuntimeCrudPropsResolver"
        :runtime-crud-loading-resolver="runtimeCrudLoadingResolver"
        :data-source-configured-resolver="dataSourceConfiguredResolver"
        :runtime-tree-active-key="runtimeTreeActiveKey"
        :selected="false"
        @runtime-tree-select="$emit('runtimeTreeSelect', $event)"
        @tree-panel-collapse-change="handleTreeCollapseChange"
      />
    </aside>
    <div class="runtime-list-grid-flow__main">
      <section
        v-for="(block, index) in mainBlocks"
        :key="block.id || `${block.blockType}-${index}`"
        class="runtime-list-grid-flow__block"
        :class="{ 'is-crud': block.blockType === 'AiCrudPage' }"
        :data-page-block-type="block.blockType"
      >
        <GridBlockRenderer
          :block="block"
          :fields="fields"
          readonly
          runtime-interactive
          suppress-runtime-list-grid
          :runtime-crud-props="nestedRuntimeCrudProps"
          :runtime-crud-loading="runtimeCrudLoading"
          :data-source-configured="dataSourceConfigured"
          :runtime-extension-hooks="runtimeExtensionHooks"
          :block-fields-resolver="blockFieldsResolver"
          :runtime-crud-props-resolver="nestedRuntimeCrudPropsResolver"
          :runtime-crud-loading-resolver="runtimeCrudLoadingResolver"
          :data-source-configured-resolver="dataSourceConfiguredResolver"
          :runtime-tree-active-key="runtimeTreeActiveKey"
          :selected="false"
          @runtime-tree-select="$emit('runtimeTreeSelect', $event)"
        />
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, defineAsyncComponent, ref, watch } from 'vue'
import { buildRuntimeListFlowBlocks } from '@/components/lowcode-builder/shared/runtime-list-grid'

defineOptions({ name: 'RuntimeListGridFlow' })

const TREE_COLLAPSED_WIDTH_PX = '44px'

// 异步拉 GridBlockRenderer，避免与 LocalComponents 里对本组件的 async 导入形成环
const GridBlockRenderer = defineAsyncComponent(() => import('./GridBlockRenderer.vue'))

const props = defineProps({
  layout: { type: Object, default: null },
  fields: { type: Array, default: () => [] },
  runtimeCrudProps: { type: Object, default: null },
  runtimeCrudLoading: { type: Boolean, default: false },
  dataSourceConfigured: { type: Boolean, default: true },
  runtimeExtensionHooks: { type: [Object, Function], default: () => ({}) },
  blockFieldsResolver: { type: Function, default: null },
  runtimeCrudPropsResolver: { type: Function, default: null },
  runtimeCrudLoadingResolver: { type: Function, default: null },
  dataSourceConfiguredResolver: { type: Function, default: null },
  runtimeTreeActiveKey: { type: [String, Number], default: '__all__' },
  /** 门户 fillHost：占满宿主并内部滚动，避免绝对画布把下面组件裁掉 */
  fillHost: { type: Boolean, default: true },
})

defineEmits(['runtimeTreeSelect'])

const flowBlocks = computed(() => buildRuntimeListFlowBlocks(props.layout))

const treeBlock = computed(() => flowBlocks.value.find(item => item?.blockType === 'tree-panel') || null)

const mainBlocks = computed(() => flowBlocks.value.filter(item => item?.blockType !== 'tree-panel'))

const treeCollapsed = ref(false)

watch(treeBlock, () => {
  treeCollapsed.value = false
})

function handleTreeCollapseChange(payload = {}) {
  if (payload?.id && treeBlock.value?.id && payload.id !== treeBlock.value.id)
    return
  treeCollapsed.value = payload?.collapsed === true
}

/** 流式壳内已有 tree-panel 时，禁止嵌套 AiCrudPage 再套 TreeCrudTemplate */
function withSuppressedTreeShell(crudProps) {
  if (!crudProps || typeof crudProps !== 'object')
    return crudProps
  const { treeConfig: _treeConfig, ...runtimeOptions } = crudProps.options || {}
  return {
    ...crudProps,
    suppressTreeCrudShell: true,
    treeConfig: {},
    options: runtimeOptions,
  }
}

const nestedRuntimeCrudProps = computed(() => withSuppressedTreeShell(props.runtimeCrudProps))

const nestedRuntimeCrudPropsResolver = computed(() => {
  const resolver = props.runtimeCrudPropsResolver
  if (typeof resolver !== 'function')
    return null
  return (block) => withSuppressedTreeShell(resolver(block))
})
</script>

<style scoped>
.runtime-list-grid-flow {
  display: flex;
  flex-direction: column;
  gap: 16px;
  width: 100%;
  min-width: 0;
  min-height: 0;
  box-sizing: border-box;
}

.runtime-list-grid-flow.is-fill {
  height: 100%;
  overflow: hidden;
}

.runtime-list-grid-flow.is-fill .runtime-list-grid-flow__main {
  flex: 1 1 auto;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.runtime-list-grid-flow.has-tree {
  flex-direction: row;
  align-items: stretch;
  gap: 0;
}

.runtime-list-grid-flow__tree {
  flex: 0 0 260px;
  width: 260px;
  max-width: 32%;
  min-width: 200px;
  min-height: 0;
  height: 100%;
  overflow: hidden;
  border-right: 1px solid var(--border-light);
  background: var(--bg-secondary);
  box-sizing: border-box;
  transition: width 0.18s ease, flex-basis 0.18s ease, min-width 0.18s ease, max-width 0.18s ease;
}

.runtime-list-grid-flow__tree.is-collapsed {
  flex: 0 0 v-bind(TREE_COLLAPSED_WIDTH_PX);
  width: v-bind(TREE_COLLAPSED_WIDTH_PX);
  min-width: v-bind(TREE_COLLAPSED_WIDTH_PX);
  max-width: v-bind(TREE_COLLAPSED_WIDTH_PX);
}

.runtime-list-grid-flow__main {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
  min-height: 0;
}

.runtime-list-grid-flow.has-tree .runtime-list-grid-flow__main {
  height: 100%;
  overflow: hidden;
  padding-left: 12px;
  box-sizing: border-box;
}

.runtime-list-grid-flow.has-tree.is-tree-collapsed .runtime-list-grid-flow__main {
  padding-left: 8px;
}

.runtime-list-grid-flow__block {
  width: 100%;
  min-width: 0;
  flex: 0 0 auto;
  min-height: 0;
}

.runtime-list-grid-flow__block.is-crud {
  flex: 1 1 auto;
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.runtime-list-grid-flow__tree :deep(.grid-block),
.runtime-list-grid-flow__block :deep(.grid-block) {
  width: 100% !important;
  max-width: 100% !important;
  height: auto !important;
  min-height: 0;
}

.runtime-list-grid-flow__tree :deep(.grid-block.block-tree-panel) {
  height: 100% !important;
  min-height: 100%;
}

.runtime-list-grid-flow__block.is-crud :deep(.grid-block.block-AiCrudPage) {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  height: 100% !important;
  min-height: 0;
}

.runtime-list-grid-flow__block.is-crud :deep(.ai-crud-preview),
.runtime-list-grid-flow__block.is-crud :deep(.system-component-preview.ai-crud-preview) {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  width: 100%;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.runtime-list-grid-flow__block.is-crud :deep(.ai-crud-page) {
  display: flex !important;
  flex: 1 1 auto !important;
  width: 100%;
  height: 100% !important;
  min-height: 0 !important;
  overflow: hidden;
}

.runtime-list-grid-flow__block :deep(.page-widget-renderer),
.runtime-list-grid-flow__block :deep(.layout-tabs),
.runtime-list-grid-flow__block :deep(.system-component-preview) {
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
}

.runtime-list-grid-flow__block :deep(.ai-crud-preview-static-tip) {
  display: none;
}

@media (max-width: 900px) {
  .runtime-list-grid-flow.has-tree {
    flex-direction: column;
  }

  .runtime-list-grid-flow__tree,
  .runtime-list-grid-flow__tree.is-collapsed {
    flex: 0 0 auto;
    width: 100%;
    max-width: none;
    min-width: 0;
    height: auto;
    max-height: 280px;
    border-right: 0;
    border-bottom: 1px solid var(--border-light);
  }

  .runtime-list-grid-flow__tree.is-collapsed {
    max-height: 48px;
  }

  .runtime-list-grid-flow.has-tree .runtime-list-grid-flow__main {
    padding-left: 0;
    overflow: auto;
  }
}
</style>
