<script setup>
import { NAlert, NButton, NEmpty, NSelect, NSpace, NTag } from 'naive-ui'
import { computed, ref, watch } from 'vue'
import { useDict } from '@/composables/useDict'
import { usePrintTemplateStore } from '@/stores/print/printTemplateStore'

const props = defineProps({ canManage: Boolean })
const store = usePrintTemplateStore()
const { dict } = useDict('sys_print_scene')
const scenes = computed(() => dict.value.sys_print_scene || [])
const busy = ref(false)
const error = ref('')

const SCENE_META = {
  LIST: {
    where: '列表每行的「操作」列',
    tip: '有应用编辑权限时，绑定后刷新本页即可看到「打印」。普通用户需重新发布应用。',
  },
  DETAIL: {
    where: '详情页操作区',
    tip: '有应用编辑权限时，绑定后打开详情即可看到「打印」。普通用户需重新发布应用。',
  },
  FLOW_TODO: {
    where: '流程待办',
    tip: '绑定后随应用发布生效。',
  },
  FLOW_DONE: {
    where: '流程已办',
    tip: '绑定后随应用发布生效。',
  },
  FLOW_STARTED: {
    where: '流程我发起',
    tip: '绑定后随应用发布生效。',
  },
}

const sceneMeta = computed(() => SCENE_META[store.scene] || {
  where: '对应运行场景',
  tip: '绑定后随应用发布生效。',
})

const sceneLabel = computed(() => labelOf(store.scene))

const currentName = computed(() => store.row?.templateName || store.name || '当前模板')

const binding = computed(() => store.bindings.find(item =>
  String(item.templateId) === String(store.row?.id)
  && item.scene === store.scene
  && isEnabled(item)))

const enabledBindings = computed(() => store.bindings.filter(isEnabled))

const boundSceneGroups = computed(() => {
  const order = scenes.value.map(item => item.value)
  const map = new Map()
  for (const item of enabledBindings.value) {
    const key = item.scene || ''
    if (!map.has(key))
      map.set(key, [])
    map.get(key).push(item)
  }
  return [...map.entries()]
    .sort((a, b) => {
      const ia = order.indexOf(a[0])
      const ib = order.indexOf(b[0])
      return (ia < 0 ? 999 : ia) - (ib < 0 ? 999 : ib)
    })
    .map(([scene, items]) => ({
      scene,
      label: labelOf(scene),
      where: SCENE_META[scene]?.where || '',
      items,
      includesCurrent: items.some(isCurrent),
    }))
})

function labelOf(scene) {
  const hit = scenes.value.find(item => item.value === scene)
  return hit?.label || scene
}

function templateLabel(row) {
  if (row.templateName)
    return row.templateName
  if (String(row.templateId) === String(store.row?.id))
    return currentName.value
  return `模板 #${row.templateId}`
}

function isCurrent(row) {
  return String(row.templateId) === String(store.row?.id)
}

function isEnabled(row) {
  return Number(row?.status) === 1
}

function selectScene(scene) {
  if (scene)
    store.scene = scene
}

async function run(action) {
  if (busy.value)
    return
  busy.value = true
  error.value = ''
  try {
    await action()
  }
  catch (reason) {
    error.value = reason.message || '绑定操作失败'
  }
  finally {
    busy.value = false
  }
}

watch(() => store.row?.id, () => run(() => store.loadBindings()), { immediate: true })
</script>

<template>
  <div class="binding-panel">
    <section class="binding-panel__block">
      <div class="binding-panel__label">
        使用场景
      </div>
      <NSelect
        v-model:value="store.scene"
        aria-label="打印场景"
        :options="scenes"
        :disabled="busy"
        placeholder="选择列表 / 详情等场景"
      />
      <div class="binding-panel__placement">
        <div class="binding-panel__placement-row">
          <span class="binding-panel__muted">出现位置</span>
          <strong>{{ sceneMeta.where }}</strong>
        </div>
        <p class="binding-panel__hint">
          {{ sceneMeta.tip }}
        </p>
      </div>
    </section>

    <NAlert v-if="error" type="error" class="binding-panel__alert">
      {{ error }}
    </NAlert>

    <section class="binding-panel__block binding-panel__current">
      <div class="binding-panel__label">
        当前模板 · {{ currentName }}
      </div>

      <template v-if="binding">
        <div class="binding-panel__status">
          <NTag size="small" type="success" :bordered="false">
            已绑定「{{ sceneLabel }}」
          </NTag>
          <NTag v-if="binding.isDefault" size="small" type="info" :bordered="false">
            默认模板
          </NTag>
          <span v-else class="binding-panel__muted">同场景有多个模板时，默认模板会优先选用</span>
        </div>
        <NSpace v-if="props.canManage" :size="8" wrap>
          <NButton
            v-if="!binding.isDefault"
            size="small"
            type="primary"
            :disabled="busy"
            @click="run(() => store.bind(true))"
          >
            设为默认
          </NButton>
          <NButton size="small" :disabled="busy" type="error" secondary @click="run(() => store.unbind(binding))">
            解除绑定
          </NButton>
        </NSpace>
      </template>

      <template v-else>
        <p class="binding-panel__hint">
          尚未绑定到「{{ sceneLabel }}」。绑定后，运行页对应位置会出现「打印」。
        </p>
        <NSpace v-if="props.canManage" :size="8" wrap>
          <NButton size="small" type="primary" :disabled="busy" @click="run(() => store.bind(true))">
            绑定到此场景
          </NButton>
        </NSpace>
      </template>
    </section>

    <section class="binding-panel__block">
      <div class="binding-panel__label-row">
        <div class="binding-panel__label">
          已绑定的场景
        </div>
        <span class="binding-panel__muted">{{ boundSceneGroups.length }} 个场景</span>
      </div>
      <div v-if="boundSceneGroups.length" class="binding-list" role="list">
        <div
          v-for="group in boundSceneGroups"
          :key="group.scene"
          class="binding-list__group"
          :class="{ 'is-active': group.scene === store.scene, 'is-current': group.includesCurrent }"
          role="listitem"
        >
          <button type="button" class="binding-list__scene" @click="selectScene(group.scene)">
            <span class="binding-list__scene-main">
              <strong>{{ group.label }}</strong>
              <span v-if="group.where" class="binding-panel__muted">{{ group.where }}</span>
            </span>
            <NTag v-if="group.includesCurrent" size="small" type="primary" :bordered="false">
              当前模板
            </NTag>
          </button>
          <div class="binding-list__templates">
            <div v-for="item in group.items" :key="item.id" class="binding-list__template">
              <span :class="{ 'is-current-name': isCurrent(item) }">{{ templateLabel(item) }}</span>
              <NTag v-if="item.isDefault" size="small" type="info" :bordered="false">
                默认
              </NTag>
            </div>
          </div>
        </div>
      </div>
      <NEmpty v-else size="small" description="还没有绑定任何场景" />
    </section>
  </div>
</template>

<style scoped>
.binding-panel {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.binding-panel__block {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.binding-panel__label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary, #1f2329);
}

.binding-panel__label-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.binding-panel__muted {
  font-size: 12px;
  color: var(--text-tertiary, #8a9099);
}

.binding-panel__hint {
  margin: 0;
  font-size: 12px;
  line-height: 1.5;
  color: var(--text-tertiary, #8a9099);
}

.binding-panel__placement {
  padding: 10px 12px;
  border: 1px solid var(--border-light, #e5e7eb);
  border-radius: 6px;
  background: var(--gray-100, #f6f8fb);
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.binding-panel__placement-row {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 8px;
  font-size: 13px;
}

.binding-panel__placement-row strong {
  font-weight: 600;
  color: var(--text-primary, #1f2329);
}

.binding-panel__current {
  padding: 12px;
  border: 1px solid var(--border-light, #e5e7eb);
  border-radius: 6px;
}

.binding-panel__status {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  min-height: 24px;
}

.binding-panel__alert {
  margin: 0;
}

.binding-list {
  border: 1px solid var(--border-light, #e5e7eb);
  border-radius: 6px;
  overflow: hidden;
  max-height: 280px;
  overflow-y: auto;
}

.binding-list__group {
  border-bottom: 1px solid var(--border-light, #e5e7eb);
  background: var(--bg-primary, #fff);
}

.binding-list__group:last-child {
  border-bottom: none;
}

.binding-list__group.is-active {
  background: color-mix(in srgb, var(--primary-color, #2080f0) 6%, #fff);
}

.binding-list__scene {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 10px 12px 6px;
  border: 0;
  background: transparent;
  cursor: pointer;
  text-align: left;
}

.binding-list__scene-main {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.binding-list__scene-main strong {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary, #1f2329);
}

.binding-list__templates {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 0 12px 10px;
}

.binding-list__template {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-tertiary, #8a9099);
}

.binding-list__template .is-current-name {
  color: var(--text-primary, #1f2329);
  font-weight: 500;
}
</style>
