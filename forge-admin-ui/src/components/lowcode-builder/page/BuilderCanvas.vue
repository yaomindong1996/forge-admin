<template>
  <div class="builder-canvas" @dragover.prevent @drop="handlePaletteDrop">
    <div class="canvas-head">
      <span>页面画布</span>
      <n-tag size="small" :bordered="false">
        {{ schema.layoutType || 'simple-crud' }}
      </n-tag>
    </div>
    <draggable
      :model-value="schema.zones"
      item-key="zoneKey"
      handle=".zone-drag"
      animation="180"
      group="page-zones"
      class="zone-list"
      @update:model-value="updateZones"
    >
      <template #item="{ element }">
        <div class="zone-frame">
          <n-button text size="tiny" class="zone-drag" @click.stop>
            <template #icon>
              <n-icon><ReorderFourOutline /></n-icon>
            </template>
          </n-button>
          <BuilderZone
            :zone="element"
            :fields="fields"
            :active="selectedZoneKey === element.zoneKey"
            @select="$emit('selectZone', element.zoneKey)"
            @update:zone="updateZone"
          />
        </div>
      </template>
    </draggable>
  </div>
</template>

<script setup>
import { ReorderFourOutline } from '@vicons/ionicons5'
import draggable from 'vuedraggable'
import BuilderZone from './BuilderZone.vue'
import { pageZoneCatalog } from './page-schema'

const props = defineProps({
  schema: {
    type: Object,
    required: true,
  },
  fields: {
    type: Array,
    default: () => [],
  },
  selectedZoneKey: {
    type: String,
    default: 'table',
  },
})

const emit = defineEmits(['update:schema', 'selectZone'])

function updateZones(zones) {
  emit('update:schema', { ...props.schema, zones: normalizeZones(zones) })
}

function handlePaletteDrop(event) {
  const zoneKey = event.dataTransfer.getData('application/x-lowcode-zone')
  if (!zoneKey)
    return
  const zones = normalizeZones(props.schema.zones || [])
  const target = zones.find(zone => zone.zoneKey === zoneKey)
  if (target) {
    target.enabled = true
  }
  else {
    const catalog = pageZoneCatalog.find(item => item.zoneKey === zoneKey)
    zones.push({
      zoneKey,
      componentKey: catalog?.componentKey || zoneKey,
      enabled: true,
      fieldRefs: [],
      props: {},
    })
  }
  emit('update:schema', { ...props.schema, zones })
  emit('selectZone', zoneKey)
}

function updateZone(zone) {
  const zones = (props.schema.zones || []).map(item => item.zoneKey === zone.zoneKey ? zone : item)
  emit('update:schema', { ...props.schema, zones })
}

function normalizeZones(zones) {
  const oldMap = new Map((props.schema.zones || []).map(zone => [zone.zoneKey, zone]))
  const used = new Set()
  return (zones || [])
    .filter(zone => zone?.zoneKey && !used.has(zone.zoneKey) && used.add(zone.zoneKey))
    .map((zone) => {
      const oldZone = oldMap.get(zone.zoneKey)
      const catalog = pageZoneCatalog.find(item => item.zoneKey === zone.zoneKey)
      return {
        zoneKey: zone.zoneKey,
        componentKey: zone.componentKey || oldZone?.componentKey || catalog?.componentKey || zone.zoneKey,
        enabled: zone.enabled ?? oldZone?.enabled ?? true,
        fieldRefs: Array.isArray(zone.fieldRefs) ? zone.fieldRefs : oldZone?.fieldRefs || [],
        props: zone.props || oldZone?.props || {},
      }
    })
}
</script>

<style scoped>
.builder-canvas {
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: var(--bg-secondary);
  min-height: 560px;
  overflow: hidden;
}

.canvas-head {
  height: 42px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 14px;
  background: var(--bg-primary);
  border-bottom: 1px solid var(--border-light);
  font-size: 13px;
  font-weight: 700;
  color: var(--text-primary);
}

.zone-list {
  display: grid;
  gap: 12px;
  padding: 14px;
}

.zone-frame {
  display: grid;
  grid-template-columns: 24px minmax(0, 1fr);
  align-items: stretch;
  gap: 8px;
}

.zone-drag {
  cursor: move;
  color: var(--text-tertiary);
}
</style>
