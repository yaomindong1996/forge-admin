<template>
  <div
    class="ai-crud-row-expand"
    :class="[`density-${layout.density || 'compact'}`]"
    :style="{ padding: `${layout.padding ?? 12}px` }"
  >
    <template v-if="visiblePanels.length === 1 && layout.mode !== 'tabs'">
      <section class="expand-panel-section">
        <header v-if="visiblePanels[0].title && layout.mode !== 'singleNoTitle'" class="expand-panel-head">
          <strong>{{ visiblePanels[0].title }}</strong>
        </header>
        <div class="expand-panel-body">
          <n-spin :show="panelState(visiblePanels[0]).loading">
            <n-alert
              v-if="panelState(visiblePanels[0]).error"
              type="error"
              size="small"
              :show-icon="false"
            >
              {{ panelState(visiblePanels[0]).error }}
              <template #action>
                <n-button text size="small" @click="reloadPanel(visiblePanels[0])">
                  重试
                </n-button>
              </template>
            </n-alert>
            <ExpandPanelRenderer
              v-else
              :panel="visiblePanels[0]"
              :row="row"
              :data="panelState(visiblePanels[0]).data"
              :loading="panelState(visiblePanels[0]).loading"
              :context="context"
            >
              <template v-for="(_, name) in $slots" #[name]="slotProps">
                <slot :name="name" v-bind="slotProps" />
              </template>
            </ExpandPanelRenderer>
          </n-spin>
        </div>
      </section>
    </template>

    <n-tabs
      v-else-if="visiblePanels.length > 1 && layout.mode !== 'stack'"
      v-model:value="activePanelKey"
      size="small"
      type="line"
      animated
      class="expand-tabs"
      @update:value="handleTabChange"
    >
      <n-tab-pane
        v-for="panel in visiblePanels"
        :key="panel.key"
        :name="panel.key"
        :tab="panel.title || panel.key"
        display-directive="show:lazy"
      >
        <n-spin :show="panelState(panel).loading">
          <n-alert
            v-if="panelState(panel).error"
            type="error"
            size="small"
            :show-icon="false"
          >
            {{ panelState(panel).error }}
            <template #action>
              <n-button text size="small" @click="reloadPanel(panel)">
                重试
              </n-button>
            </template>
          </n-alert>
          <ExpandPanelRenderer
            v-else
            :panel="panel"
            :row="row"
            :data="panelState(panel).data"
            :loading="panelState(panel).loading"
            :context="context"
          >
            <template v-for="(_, name) in $slots" #[name]="slotProps">
              <slot :name="name" v-bind="slotProps" />
            </template>
          </ExpandPanelRenderer>
        </n-spin>
      </n-tab-pane>
    </n-tabs>

    <div v-else class="expand-stack">
      <section v-for="panel in visiblePanels" :key="panel.key" class="expand-panel-section">
        <header class="expand-panel-head">
          <strong>{{ panel.title || panel.key }}</strong>
        </header>
        <div class="expand-panel-body">
          <n-spin :show="panelState(panel).loading">
            <n-alert
              v-if="panelState(panel).error"
              type="error"
              size="small"
              :show-icon="false"
            >
              {{ panelState(panel).error }}
              <template #action>
                <n-button text size="small" @click="reloadPanel(panel)">
                  重试
                </n-button>
              </template>
            </n-alert>
            <ExpandPanelRenderer
              v-else
              :panel="panel"
              :row="row"
              :data="panelState(panel).data"
              :loading="panelState(panel).loading"
              :context="context"
            >
              <template v-for="(_, name) in $slots" #[name]="slotProps">
                <slot :name="name" v-bind="slotProps" />
              </template>
            </ExpandPanelRenderer>
          </n-spin>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { loadExpandPanelData } from './expand-utils'
import ExpandPanelRenderer from './ExpandPanelRenderer.vue'

const props = defineProps({
  config: { type: Object, required: true },
  row: { type: Object, required: true },
  rowKeyValue: { type: [String, Number], required: true },
  context: { type: Object, default: () => ({}) },
})

const panelStates = ref({})
const activePanelKey = ref('')

const visiblePanels = computed(() => (props.config.panels || []).filter(panel => panel.visible !== false))
const layout = computed(() => props.config.layout || {})

watch(visiblePanels, (panels) => {
  if (!activePanelKey.value || !panels.some(panel => panel.key === activePanelKey.value))
    activePanelKey.value = panels[0]?.key || ''
}, { immediate: true })

watch(
  () => props.rowKeyValue,
  () => {
    if (props.config.cache === false)
      panelStates.value = {}
    loadInitialPanels()
  },
)

onMounted(() => {
  loadInitialPanels()
})

function panelState(panel) {
  return panelStates.value[panel.key] || { loading: false, loaded: false, data: null, error: '' }
}

function setPanelState(panel, patch) {
  panelStates.value = {
    ...panelStates.value,
    [panel.key]: {
      ...panelState(panel),
      ...patch,
    },
  }
}

function loadInitialPanels() {
  if (!visiblePanels.value.length)
    return
  if (layout.value.mode === 'stack') {
    visiblePanels.value.forEach(panel => loadPanel(panel))
    return
  }
  const activePanel = visiblePanels.value.find(panel => panel.key === activePanelKey.value) || visiblePanels.value[0]
  loadPanel(activePanel)
}

function handleTabChange(key) {
  const panel = visiblePanels.value.find(item => item.key === key)
  if (panel)
    loadPanel(panel)
}

function reloadPanel(panel) {
  loadPanel(panel, true)
}

async function loadPanel(panel, force = false) {
  if (!panel)
    return
  const state = panelState(panel)
  if (!force && props.config.cache !== false && state.loaded)
    return
  setPanelState(panel, { loading: true, error: '' })
  try {
    const data = await loadExpandPanelData(panel, props.row, props.context)
    setPanelState(panel, { data, loading: false, loaded: true, error: '' })
  }
  catch (error) {
    console.error('[AiCrudRowExpand] 加载展开面板失败:', error)
    setPanelState(panel, {
      loading: false,
      loaded: false,
      error: error?.message || '展开内容加载失败',
    })
  }
}
</script>

<style scoped>
.ai-crud-row-expand {
  min-width: 0;
  background: #fafbfc;
  border-top: 1px solid rgba(17, 24, 39, 0.06);
  border-bottom: 1px solid rgba(17, 24, 39, 0.06);
}

.expand-panel-section {
  min-width: 0;
}

.expand-panel-head {
  display: flex;
  align-items: center;
  min-height: 28px;
  margin-bottom: 8px;
  color: var(--text-primary);
}

.expand-panel-head strong {
  font-size: 13px;
  font-weight: 600;
}

.expand-panel-body {
  min-width: 0;
}

.expand-tabs {
  min-width: 0;
}

.expand-stack {
  display: grid;
  gap: 12px;
}

.density-compact {
  font-size: 13px;
}
</style>
