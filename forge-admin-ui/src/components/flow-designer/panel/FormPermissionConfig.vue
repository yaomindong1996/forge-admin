<script setup>
/**
 * FormPermissionConfig — 表单字段权限
 *
 * config.formFieldPermissions: [{ field, label, readable, writable, required }]
 */
import { computed } from 'vue'

const props = defineProps({
  config: { type: Object, required: true },
  readonly: Boolean,
})

const emit = defineEmits(['update:config'])

const list = computed({
  get: () => Array.isArray(props.config?.formFieldPermissions) ? props.config.formFieldPermissions : [],
  set: v => emit('update:config', { formFieldPermissions: v }),
})

function add() {
  list.value = [...list.value, { field: '', label: '', readable: true, writable: true, required: false }]
}
function update(i, patch) {
  const arr = [...list.value]
  arr[i] = { ...arr[i], ...patch }
  list.value = arr
}
function remove(i) {
  const arr = [...list.value]
  arr.splice(i, 1)
  list.value = arr
}
</script>

<template>
  <div class="space-y-3">
    <div class="flex items-center justify-between">
      <span class="text-sm font-medium">表单字段权限</span>
      <n-button v-if="!readonly" size="tiny" @click="add">
        + 新增
      </n-button>
    </div>
    <div v-if="list.length === 0" class="text-xs rounded bg-gray-50 px-3 py-2 text-gray-400">
      暂无字段权限配置（默认所有字段可读可写）
    </div>
    <div v-else class="space-y-2">
      <div
        v-for="(f, i) in list"
        :key="i"
        class="border border-gray-100 rounded p-2 space-y-2"
      >
        <div class="flex gap-2">
          <n-input
            :value="f.field"
            placeholder="字段名"
            :disabled="readonly"
            @update:value="update(i, { field: $event })"
          />
          <n-input
            :value="f.label"
            placeholder="字段标签"
            :disabled="readonly"
            @update:value="update(i, { label: $event })"
          />
          <n-button v-if="!readonly" size="tiny" type="error" @click="remove(i)">
            删除
          </n-button>
        </div>
        <div class="text-xs flex items-center gap-4">
          <n-checkbox :checked="f.readable" :disabled="readonly" @update:checked="update(i, { readable: $event })">
            可读
          </n-checkbox>
          <n-checkbox :checked="f.writable" :disabled="readonly" @update:checked="update(i, { writable: $event })">
            可写
          </n-checkbox>
          <n-checkbox :checked="f.required" :disabled="readonly" @update:checked="update(i, { required: $event })">
            必填
          </n-checkbox>
        </div>
      </div>
    </div>
  </div>
</template>
