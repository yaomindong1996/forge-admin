<template>
  <n-modal
    v-model:show="visibleProxy"
    :title="modalTitle"
    preset="card"
    class="form-quick-create-modal"
    :style="{ width: formLayout.modalWidth || (isDictManage ? '640px' : '640px') }"
    :mask-closable="false"
    transform-origin="center"
  >
    <DictDataQuickManagePanel
      v-if="isDictManage"
      :definition="definition"
      :field="field"
      :parent-form-data="parentFormData"
      :active="visibleProxy"
      @changed="handleManagedChange"
    />
    <template v-else>
      <AiForm
        ref="formRef"
        v-model:value="formValue"
        :schema="schema"
        :class="formLayout.formClass || undefined"
        :grid-cols="formLayout.gridCols || 1"
        :x-gap="formLayout.xGap"
        :y-gap="formLayout.yGap"
        :label-placement="formLayout.labelPlacement || 'left'"
        :label-width="formLayout.labelWidth ?? 120"
        :label-align="formLayout.labelAlign || 'right'"
        size="small"
        :show-actions="false"
      />
    </template>
    <template v-if="!isDictManage" #footer>
      <n-space justify="end">
        <n-button @click="visibleProxy = false">
          取消
        </n-button>
        <n-button type="primary" :loading="submitting" @click="handleSubmit">
          保存
        </n-button>
      </n-space>
    </template>
    <template v-else #footer>
      <n-space justify="end">
        <n-button @click="visibleProxy = false">
          完成
        </n-button>
      </n-space>
    </template>
  </n-modal>
</template>

<script setup>
import { computed, defineAsyncComponent, ref, watch } from 'vue'
import { getDictData } from '@/composables/useDict'
import { useUserStore } from '@/store'
import { useDictStore } from '@/stores/system/dictStore'
import DictDataQuickManagePanel from './DictDataQuickManagePanel.vue'
import { hasQuickCreatePermission } from './permissions'

/** 异步加载 AiForm，避免 Host → AiForm → AiFormItem → DictSelect/Host 循环依赖 TDZ */
const AiForm = defineAsyncComponent(() => import('../AiForm.vue'))

const props = defineProps({
  show: {
    type: Boolean,
    default: false,
  },
  definition: {
    type: Object,
    default: null,
  },
  parentFormData: {
    type: Object,
    default: () => ({}),
  },
  field: {
    type: Object,
    default: () => ({}),
  },
})

const emit = defineEmits(['update:show', 'success'])

const userStore = useUserStore()
const dictStore = useDictStore()
const formRef = ref(null)
const formValue = ref({})
const submitting = ref(false)

const isDictManage = computed(() =>
  props.definition?.preset === 'dictData' && props.definition?.manageMode !== false)

const modalTitle = computed(() => {
  if (isDictManage.value)
    return props.definition?.modalTitle || '管理字典项'
  return props.definition?.modalTitle || '快捷新增'
})

const editSchemaDictTypes = computed(() => props.definition?.editSchemaDictTypes || [])

/** 订阅字典缓存，快捷弹窗打开后 options 加载完成时 schema 自动重建 */
const editSchemaDictSnapshot = computed(() => {
  const types = editSchemaDictTypes.value
  if (!types.length)
    return null
  const dict = {}
  types.forEach((type) => {
    dict[type] = dictStore.dictCache.get(type) || []
  })
  return dict
})

const visibleProxy = computed({
  get: () => props.show,
  set: value => emit('update:show', value),
})

const formLayout = computed(() => props.definition?.formLayout || {})

const schema = computed(() => {
  if (!props.definition?.buildSchema)
    return []
  const dict = editSchemaDictSnapshot.value
  return props.definition.buildSchema({
    userStore,
    parentFormData: props.parentFormData,
    config: props.definition,
    field: props.field,
    dict: dict || undefined,
  })
})

watch(() => props.show, async (visible) => {
  if (!visible || isDictManage.value)
    return
  const types = editSchemaDictTypes.value
  if (types.length)
    await Promise.all(types.map(type => getDictData(type)))
  if (!props.definition?.buildInitialValues)
    return
  formValue.value = props.definition.buildInitialValues({
    userStore,
    parentFormData: props.parentFormData,
    config: props.definition,
    field: props.field,
  })
})

function handleManagedChange(payload) {
  emit('success', payload || {})
}

async function handleSubmit() {
  if (!props.definition?.submit)
    return
  if (!hasQuickCreatePermission(userStore, props.definition.addPermission || props.definition.permission)) {
    window.$message?.error('无权限执行快捷新增')
    return
  }
  try {
    await formRef.value?.validate?.()
  }
  catch {
    return
  }
  submitting.value = true
  try {
    const result = await props.definition.submit({ ...formValue.value }, {
      userStore,
      parentFormData: props.parentFormData,
      config: props.definition,
      field: props.field,
    })
    emit('success', result || { submitted: { ...formValue.value } })
  }
  catch (error) {
    window.$message?.error(error?.message || '保存失败')
  }
  finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.form-quick-create-modal :deep(.n-form-item) {
  margin-bottom: 12px;
}
</style>
