<script setup>
import { NButton, NInput, NModal, NTree } from 'naive-ui'
import { computed, ref, watch } from 'vue'
import { collectionPaths, describeFieldPath, groupPrintFields, isCollectionChild } from './fieldGroups'

const props = defineProps({
  value: { type: String, default: '' },
  catalog: { type: Array, default: () => [] },
  /** Limit to children of a collection path; emits relative path */
  onlyUnder: { type: String, default: '' },
  includeCollections: Boolean,
  /** Optional type filter e.g. ['IMAGE'] */
  types: { type: Array, default: null },
  placeholder: { type: String, default: '选择字段' },
  size: { type: String, default: 'small' },
  /** Hide the trigger button; open via expose openPicker() */
  hideTrigger: Boolean,
})
const emit = defineEmits(['update:value'])

const open = ref(false)
const query = ref('')
const selectedKeys = ref([])

const filteredCatalog = computed(() => {
  const collections = collectionPaths(props.catalog)
  let fields = props.catalog.filter((field) => {
    if (field.type === 'COLLECTION')
      return props.includeCollections
    if (props.onlyUnder)
      return field.path.startsWith(`${props.onlyUnder}.`)
    return true
  })
  if (!props.includeCollections && !props.onlyUnder)
    fields = fields.filter(field => !isCollectionChild(field.path, collections))
  if (props.types?.length)
    fields = fields.filter(field => props.types.includes(field.type))
  return fields
})

const treeData = computed(() => {
  const groups = groupPrintFields(filteredCatalog.value, query.value)
  return groups.map(group => ({
    key: `group:${group.key}`,
    label: group.title,
    disabled: true,
    children: group.fields.map((field) => {
      const relative = props.onlyUnder && field.path.startsWith(`${props.onlyUnder}.`)
        ? field.path.slice(props.onlyUnder.length + 1)
        : null
      const value = props.onlyUnder ? (relative || field.path) : field.path
      const leaf = field.label || field.path.split('.').at(-1)
      return {
        key: value,
        label: `${leaf}  ·  ${relative || field.path}`,
        isLeaf: true,
      }
    }),
  })).filter(group => group.children?.length)
})

const summary = computed(() => {
  if (!props.value)
    return ''
  if (props.onlyUnder) {
    const full = `${props.onlyUnder}.${props.value}`
    const field = props.catalog.find(item => item.path === full || item.path === props.value)
    return field?.label || props.value
  }
  return describeFieldPath(props.catalog, props.value) || props.value
})

const defaultExpanded = computed(() => treeData.value.map(node => node.key))

watch(open, (value) => {
  if (value) {
    query.value = ''
    selectedKeys.value = props.value ? [props.value] : []
  }
})

function onSelect(keys) {
  const key = keys?.[0]
  if (!key || String(key).startsWith('group:'))
    return
  emit('update:value', key)
  open.value = false
}

defineExpose({
  openPicker() {
    open.value = true
  },
})
</script>

<template>
  <div class="field-picker" :class="{ 'hide-trigger': hideTrigger }">
    <NButton
      v-if="!hideTrigger"
      class="picker-trigger"
      :size="size"
      secondary
      block
      @click="open = true"
    >
      <span class="picker-label" :class="{ placeholder: !summary }">
        {{ summary || placeholder }}
      </span>
      <span class="picker-hint">选择</span>
    </NButton>

    <NModal
      v-model:show="open"
      preset="card"
      title="选择字段"
      :style="{ width: '520px', maxWidth: '92vw' }"
      :bordered="false"
      size="small"
      :z-index="4100"
    >
      <p class="muted tip">
        按分组树形展示，可搜索标签或路径；点选叶子字段即可。
      </p>
      <NInput
        v-model:value="query"
        size="small"
        clearable
        placeholder="搜索字段名 / 路径"
        class="search"
      />
      <div class="tree-scroll">
        <NTree
          block-line
          selectable
          :data="treeData"
          :selected-keys="selectedKeys"
          :default-expanded-keys="defaultExpanded"
          key-field="key"
          label-field="label"
          children-field="children"
          @update:selected-keys="onSelect"
        />
        <p v-if="!treeData.length" class="muted empty">
          没有匹配的字段
        </p>
      </div>
    </NModal>
  </div>
</template>

<style scoped>
.field-picker {
  width: 100%;
}
.picker-trigger {
  justify-content: space-between;
  height: auto;
  min-height: 28px;
  padding: 4px 8px;
}
.picker-label {
  flex: 1 1 auto;
  min-width: 0;
  overflow: hidden;
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
}
.picker-label.placeholder {
  color: var(--text-tertiary, #94a3b8);
}
.picker-hint {
  flex: 0 0 auto;
  margin-left: 8px;
  color: var(--primary-color, #356cde);
  font-size: 11px;
}
.tip {
  margin: 0 0 8px;
  font-size: 12px;
  line-height: 1.45;
}
.search {
  margin-bottom: 8px;
}
.tree-scroll {
  max-height: min(52vh, 420px);
  overflow: auto;
  padding: 4px 0;
  border: 1px solid var(--border-light, #e2e8f0);
  border-radius: 6px;
  background: var(--bg-primary, #fff);
}
.empty {
  margin: 16px;
  text-align: center;
  font-size: 12px;
}
</style>
