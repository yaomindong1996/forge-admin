<script setup>
import { NAlert, NInput, NModal, NSelect } from 'naive-ui'
import { computed, ref, watch } from 'vue'
import { DEFAULT_PRINT_SCENES, FALLBACK_PRINT_SCENE_OPTIONS } from '@/components/print/management/printSceneBinding'
import { useDict } from '@/composables/useDict'
import { usePrintTemplateStore } from '@/stores/print/printTemplateStore'

const props = defineProps({
  show: Boolean,
  source: { type: Object, default: null },
})
const emit = defineEmits(['update:show', 'created'])
const store = usePrintTemplateStore()
const { dict } = useDict('sys_print_scene')
const name = ref('')
const scenes = ref([...DEFAULT_PRINT_SCENES])
const busy = ref(false)
const error = ref('')
const sceneOptions = computed(() => dict.value.sys_print_scene?.length ? dict.value.sys_print_scene : FALLBACK_PRINT_SCENE_OPTIONS)
watch(() => props.show, () => {
  name.value = ''
  scenes.value = [...DEFAULT_PRINT_SCENES]
  error.value = ''
})
async function create() {
  if (!props.source || !name.value.trim() || busy.value)
    return false
  busy.value = true
  error.value = ''
  try {
    const row = await store.create(props.source, name.value.trim(), scenes.value)
    emit('created', row)
    emit('update:show', false)
  }
  catch (reason) {
    error.value = reason.message || '新建失败'
  }
  finally {
    busy.value = false
  }
  return false
}
</script>

<template>
  <NModal :show="show" preset="dialog" title="新建打印模板" positive-text="创建并设计" negative-text="取消" :loading="busy" :mask-closable="false" :positive-button-props="{ disabled: !name.trim() || !source }" @positive-click="create" @negative-click="!busy && emit('update:show', false)" @close="!busy && emit('update:show', false)">
    <NAlert v-if="error" type="error" style="margin-bottom: 12px">
      {{ error }}
    </NAlert>
    <p class="create-hint">
      模板自动关联当前表单。勾选场景后，随应用发布即可打印。
    </p>
    <NInput v-model:value="name" :input-props="{ 'aria-label': '打印模板名称' }" placeholder="模板名称" :maxlength="100" :disabled="busy" />
    <NSelect
      v-model:value="scenes"
      multiple
      :options="sceneOptions"
      :disabled="busy"
      :consistent-menu-width="false"
      placeholder="使用场景"
      aria-label="使用场景"
      style="margin-top: 12px"
    />
  </NModal>
</template>

<style scoped>
.create-hint {
  margin: 0 0 12px;
  color: #86909c;
  font-size: 12px;
  line-height: 18px;
}
</style>
