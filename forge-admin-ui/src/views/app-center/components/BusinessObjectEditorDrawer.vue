<template>
  <n-drawer :show="show" width="640" @update:show="value => emit('update:show', value)">
    <n-drawer-content title="编辑业务单元" closable>
      <div class="object-editor">
        <n-alert type="info" :show-icon="false">
          对象编码是系统引用，创建后不在这里修改；关联回显字段在对象设计器的关联关系配置中维护。
        </n-alert>

        <n-form ref="formRef" :model="form" :rules="rules" label-placement="top">
          <n-form-item label="所属业务域" path="suiteCode">
            <n-select
              v-model:value="form.suiteCode"
              filterable
              :options="suiteOptions"
              placeholder="选择业务单元归属的业务域"
            />
          </n-form-item>

          <n-grid :cols="2" :x-gap="12">
            <n-form-item-gi label="业务单元名称" path="objectName">
              <n-input v-model:value="form.objectName" placeholder="例如：采购申请" />
            </n-form-item-gi>
            <n-form-item-gi label="对象编码">
              <n-input v-model:value="form.objectCode" disabled placeholder="系统引用编码" />
            </n-form-item-gi>
            <n-form-item-gi label="对象类型" path="objectType">
              <DictSelect v-model:value="form.objectType" dict-type="ai_business_object_type" quick-create />
            </n-form-item-gi>
            <n-form-item-gi label="图标">
              <IconSelector v-model="form.icon" />
            </n-form-item-gi>
            <n-form-item-gi label="排序">
              <n-input-number v-model:value="form.sortOrder" :min="0" :show-button="false" placeholder="排序值" />
            </n-form-item-gi>
            <n-form-item-gi label="启用状态">
              <n-switch v-model:value="form.status" :checked-value="1" :unchecked-value="0" />
            </n-form-item-gi>
          </n-grid>

          <n-form-item label="业务说明">
            <n-input
              v-model:value="form.description"
              type="textarea"
              placeholder="说明这个业务单元管理的业务信息和典型使用场景"
            />
          </n-form-item>
        </n-form>
      </div>

      <template #footer>
        <n-space justify="end">
          <n-button @click="emit('update:show', false)">
            取消
          </n-button>
          <n-button type="primary" :loading="saving" @click="save">
            保存
          </n-button>
        </n-space>
      </template>
    </n-drawer-content>
  </n-drawer>
</template>

<script setup>
import { useMessage } from 'naive-ui'
import { computed, reactive, ref, watch } from 'vue'
import { updateBusinessObject } from '@/api/business-app'
import DictSelect from '@/components/DictSelect.vue'
import IconSelector from '@/components/IconSelector.vue'

const props = defineProps({
  show: {
    type: Boolean,
    default: false,
  },
  object: {
    type: Object,
    default: null,
  },
  suites: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['update:show', 'saved'])
const message = useMessage()
const formRef = ref(null)
const saving = ref(false)
const form = reactive(defaultForm())

const rules = {
  suiteCode: { required: true, message: '请选择业务域', trigger: ['blur', 'change'] },
  objectName: { required: true, message: '请输入业务单元名称', trigger: 'blur' },
  objectType: { required: true, message: '请选择对象类型', trigger: ['blur', 'change'] },
}

const suiteOptions = computed(() => {
  const options = props.suites.map(item => ({
    label: item.suiteName || item.suiteCode,
    value: item.suiteCode,
  }))
  if (form.suiteCode && !options.some(item => item.value === form.suiteCode)) {
    options.unshift({
      label: props.object?.suiteName || form.suiteCode,
      value: form.suiteCode,
    })
  }
  return options
})

watch(() => props.show, (visible) => {
  if (!visible)
    return
  Object.assign(form, defaultForm(), normalizeObject(props.object))
})

async function save() {
  await formRef.value?.validate()
  const payload = buildPayload()
  if (props.object?.status === 1 && payload.status === 0) {
    if (!window.$dialog?.warning) {
      await persist(payload)
      return
    }
    window.$dialog.warning({
      title: '停用业务单元',
      content: `确定停用“${payload.objectName || payload.objectCode}”吗？停用后关联配置仍保留，但用户不应继续进入该业务单元办理新业务。`,
      positiveText: '停用并保存',
      negativeText: '取消',
      onPositiveClick: () => persist(payload),
    })
    return
  }
  await persist(payload)
}

function buildPayload() {
  return {
    id: form.id,
    suiteCode: form.suiteCode,
    objectCode: form.objectCode,
    objectName: String(form.objectName || '').trim(),
    objectType: form.objectType,
    modelId: form.modelId,
    modelCode: form.modelCode,
    displayField: trimToNull(form.displayField),
    icon: trimToNull(form.icon),
    description: trimToNull(form.description),
    status: form.status,
    sortOrder: Number(form.sortOrder || 0),
    options: trimToNull(form.options),
  }
}

async function persist(payload) {
  saving.value = true
  try {
    await updateBusinessObject(payload)
    message.success('业务单元已保存')
    emit('saved', payload)
    emit('update:show', false)
  }
  finally {
    saving.value = false
  }
}

function normalizeObject(object) {
  if (!object)
    return {}
  return {
    id: object.id,
    suiteCode: object.suiteCode,
    objectCode: object.objectCode,
    objectName: object.objectName,
    objectType: object.objectType || 'MASTER',
    modelId: object.modelId ?? null,
    modelCode: object.modelCode || '',
    displayField: object.displayField || '',
    icon: object.icon || '',
    description: object.description || '',
    status: object.status ?? 1,
    sortOrder: object.sortOrder ?? 0,
    options: object.options || '',
  }
}

function trimToNull(value) {
  const text = String(value || '').trim()
  return text || null
}

function defaultForm() {
  return {
    id: null,
    suiteCode: null,
    objectCode: '',
    objectName: '',
    objectType: 'MASTER',
    modelId: null,
    modelCode: '',
    displayField: '',
    icon: '',
    description: '',
    status: 1,
    sortOrder: 0,
    options: '',
  }
}
</script>

<style scoped>
.object-editor {
  display: grid;
  gap: 14px;
}
</style>
