<template>
  <n-tabs
    v-model:value="activeKey"
    class="expand-nested-tabs"
    size="small"
    type="line"
    animated
    @update:value="loadActivePanel"
  >
    <n-tab-pane
      v-for="child in childPanels"
      :key="child.key"
      :name="child.key"
      :tab="child.title || child.key"
      display-directive="show:lazy"
    >
      <n-spin :show="panelState(child).loading">
        <n-alert
          v-if="panelState(child).error"
          type="error"
          size="small"
          :show-icon="false"
        >
          {{ panelState(child).error }}
          <template #action>
            <n-button text size="small" @click="loadPanel(child, true)">
              重试
            </n-button>
          </template>
        </n-alert>
        <ExpandPanelRenderer
          v-else
          :panel="child"
          :row="row"
          :data="panelState(child).data"
          :loading="panelState(child).loading"
          :context="context"
        >
          <template v-for="(_, name) in $slots" #[name]="slotProps">
            <slot :name="name" v-bind="slotProps" />
          </template>
        </ExpandPanelRenderer>
      </n-spin>
    </n-tab-pane>
  </n-tabs>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import ExpandPanelRenderer from '../ExpandPanelRenderer.vue'
import { loadExpandPanelData } from '../expand-utils'

const props = defineProps({
  panel: { type: Object, required: true },
  row: { type: Object, default: () => ({}) },
  data: { type: null, default: null },
  context: { type: Object, default: () => ({}) },
})

const activeKey = ref('')
const states = ref({})
const childPanels = computed(() => props.panel.panels || [])

watch(childPanels, (panels) => {
  if (!activeKey.value || !panels.some(panel => panel.key === activeKey.value))
    activeKey.value = panels[0]?.key || ''
}, { immediate: true })

onMounted(loadActivePanel)

function panelState(panel) {
  return states.value[panel.key] || { loading: false, loaded: false, data: fallbackPanelData(panel), error: '' }
}

function fallbackPanelData(panel) {
  if (props.data && typeof props.data === 'object' && !Array.isArray(props.data) && props.data[panel.key] !== undefined)
    return props.data[panel.key]
  return props.data
}

function setPanelState(panel, patch) {
  states.value = {
    ...states.value,
    [panel.key]: {
      ...panelState(panel),
      ...patch,
    },
  }
}

function loadActivePanel() {
  const panel = childPanels.value.find(item => item.key === activeKey.value)
  if (panel)
    loadPanel(panel)
}

async function loadPanel(panel, force = false) {
  const state = panelState(panel)
  if (!force && state.loaded)
    return
  setPanelState(panel, { loading: true, error: '' })
  try {
    const data = await loadExpandPanelData(panel, props.row, props.context)
    setPanelState(panel, { data, loading: false, loaded: true, error: '' })
  }
  catch (error) {
    console.error('[ExpandTabsPanel] 加载子面板失败:', error)
    setPanelState(panel, {
      loading: false,
      loaded: false,
      error: error?.message || '展开内容加载失败',
    })
  }
}
</script>

<style scoped>
.expand-nested-tabs {
  min-width: 0;
}
</style>
