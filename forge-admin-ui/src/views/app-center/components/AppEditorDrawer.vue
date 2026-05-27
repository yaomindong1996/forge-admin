<template>
  <n-drawer :show="show" width="520" @update:show="value => emit('update:show', value)">
    <n-drawer-content :title="form.id ? '编辑应用入口' : '新增应用入口'" closable>
      <n-form ref="formRef" :model="form" :rules="rules" label-placement="top">
        <n-form-item label="入口名称" path="appName">
          <n-input v-model:value="form.appName" placeholder="例如：客户管理" />
        </n-form-item>
        <n-form-item label="入口编码" path="appCode">
          <n-input v-model:value="form.appCode" placeholder="例如：CRM_CUSTOMER_MANAGE" />
        </n-form-item>
        <n-form-item label="所属套件" path="suiteCode">
          <n-select
            v-model:value="form.suiteCode"
            filterable
            :options="suiteOptions"
            placeholder="选择业务套件"
            @update:value="loadObjects"
          />
        </n-form-item>
        <n-form-item label="应用类型" path="appType">
          <DictSelect v-model:value="form.appType" dict-type="ai_business_app_type" />
        </n-form-item>
        <n-form-item label="关联业务对象">
          <n-select
            v-model:value="form.objectCode"
            clearable
            filterable
            :options="objectOptions"
            placeholder="标准业务应用建议关联对象"
          />
        </n-form-item>
        <n-form-item label="入口模式" path="entryMode">
          <DictSelect v-model:value="form.entryMode" dict-type="ai_business_app_entry_mode" />
        </n-form-item>
        <n-form-item label="运行配置键">
          <n-input v-model:value="form.configKey" placeholder="RUNTIME 模式可填写 configKey" />
        </n-form-item>
        <n-form-item label="入口地址">
          <n-input v-model:value="form.entryUrl" placeholder="内部路由、H5、iframe 或外部地址" />
        </n-form-item>
        <n-form-item label="业务说明">
          <n-input v-model:value="form.description" type="textarea" placeholder="说明这个入口面向的业务场景" />
        </n-form-item>
        <n-form-item label="启用状态">
          <n-switch v-model:value="form.status" :checked-value="1" :unchecked-value="0" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="emit('update:show', false)">取消</n-button>
          <n-button type="primary" :loading="saving" @click="save">
            <template #icon>
              <n-icon><SaveOutline /></n-icon>
            </template>
            保存
          </n-button>
        </n-space>
      </template>
    </n-drawer-content>
  </n-drawer>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useMessage } from 'naive-ui'
import { SaveOutline } from '@vicons/ionicons5'
import { businessObjectList, createBusinessApp, updateBusinessApp } from '@/api/business-app'
import DictSelect from '@/components/DictSelect.vue'

const props = defineProps({
  show: {
    type: Boolean,
    default: false,
  },
  app: {
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
const objectOptions = ref([])

const form = reactive(defaultForm())

const rules = {
  appName: { required: true, message: '请输入入口名称', trigger: 'blur' },
  appCode: { required: true, message: '请输入入口编码', trigger: 'blur' },
  suiteCode: { required: true, message: '请选择业务套件', trigger: 'change' },
  appType: { required: true, message: '请选择应用类型', trigger: 'change' },
  entryMode: { required: true, message: '请选择入口模式', trigger: 'change' },
}

const suiteOptions = computed(() => props.suites.map(item => ({
  label: item.suiteName || item.suiteCode,
  value: item.suiteCode,
})))

watch(() => props.show, (visible) => {
  if (!visible)
    return
  Object.assign(form, defaultForm(), props.app || {})
  if (!form.suiteCode && props.suites.length)
    form.suiteCode = props.suites[0].suiteCode
  loadObjects()
})

async function loadObjects() {
  if (!form.suiteCode) {
    objectOptions.value = []
    return
  }
  const res = await businessObjectList({ suiteCode: form.suiteCode, status: 1 })
  objectOptions.value = (res.data || []).map(item => ({
    label: item.objectName || item.objectCode,
    value: item.objectCode,
  }))
}

async function save() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (form.id)
      await updateBusinessApp({ ...form })
    else
      await createBusinessApp({ ...form })
    message.success('应用入口已保存')
    emit('saved')
    emit('update:show', false)
  }
  finally {
    saving.value = false
  }
}

function defaultForm() {
  return {
    id: null,
    appName: '',
    appCode: '',
    appType: 'BUSINESS',
    suiteCode: null,
    objectCode: null,
    entryMode: 'RUNTIME',
    entryUrl: '',
    configKey: '',
    icon: '',
    description: '',
    status: 1,
    sortOrder: 0,
  }
}
</script>
