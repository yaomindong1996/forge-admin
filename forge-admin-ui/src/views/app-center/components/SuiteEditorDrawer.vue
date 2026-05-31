<template>
  <n-drawer :show="show" width="620" @update:show="value => emit('update:show', value)">
    <n-drawer-content :title="form.id ? '编辑业务套件' : '新建业务套件'" closable>
      <n-form ref="formRef" :model="form" :rules="rules" label-placement="top">
        <n-grid :cols="2" :x-gap="12">
          <n-form-item-gi label="套件名称" path="suiteName">
            <n-input v-model:value="form.suiteName" placeholder="例如：合同管理" />
          </n-form-item-gi>
          <n-form-item-gi label="套件编码" path="suiteCode">
            <n-input
              v-model:value="form.suiteCode"
              :disabled="Boolean(form.id)"
              placeholder="例如：CONTRACT"
              @blur="form.suiteCode = normalizeCode(form.suiteCode)"
            />
          </n-form-item-gi>
        </n-grid>

        <n-form-item label="套件图标">
          <IconSelector v-model="form.icon" />
        </n-form-item>

        <n-grid :cols="3" :x-gap="12">
          <n-form-item-gi label="启用状态">
            <n-switch v-model:value="form.status" :checked-value="1" :unchecked-value="0" />
          </n-form-item-gi>
          <n-form-item-gi label="排序">
            <n-input-number v-model:value="form.sortOrder" :min="0" :show-button="false" placeholder="排序" />
          </n-form-item-gi>
          <n-form-item-gi label="创建管理端目录">
            <n-switch v-model:value="form.createMenuDirectory" />
          </n-form-item-gi>
        </n-grid>

        <template v-if="form.createMenuDirectory">
          <n-form-item label="父级菜单或模块">
            <MenuParentSelect v-model:value="form.adminMenuParentId" placeholder="选择套件目录挂载位置，默认顶级" />
          </n-form-item>
          <n-form-item label="目录排序">
            <n-input-number v-model:value="form.menuSort" :min="0" :show-button="false" placeholder="默认使用套件排序" />
          </n-form-item>
        </template>

        <n-form-item label="业务说明">
          <n-input
            v-model:value="form.description"
            type="textarea"
            placeholder="说明这个套件覆盖的业务范围"
          />
        </n-form-item>
      </n-form>

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
import { reactive, ref, watch } from 'vue'
import { createBusinessSuite, updateBusinessSuite } from '@/api/business-app'
import IconSelector from '@/components/IconSelector.vue'
import MenuParentSelect from '@/components/lowcode-builder/shared/MenuParentSelect.vue'

const props = defineProps({
  show: {
    type: Boolean,
    default: false,
  },
  suite: {
    type: Object,
    default: null,
  },
})

const emit = defineEmits(['update:show', 'saved'])
const message = useMessage()
const formRef = ref(null)
const saving = ref(false)
const form = reactive(defaultForm())

const rules = {
  suiteName: { required: true, message: '请输入套件名称', trigger: 'blur' },
  suiteCode: {
    required: true,
    validator: (_, value) => isValidCode(value),
    message: '套件编码需以字母开头，仅包含字母、数字和下划线',
    trigger: ['blur', 'input'],
  },
}

watch(() => props.show, (visible) => {
  if (!visible)
    return
  Object.assign(form, defaultForm(), props.suite || {})
  hydrateOptions()
})

async function save() {
  await formRef.value?.validate()
  saving.value = true
  try {
    const payload = {
      id: form.id,
      suiteCode: normalizeCode(form.suiteCode),
      suiteName: form.suiteName.trim(),
      icon: trimToNull(form.icon),
      description: trimToNull(form.description),
      status: form.status,
      sortOrder: Number(form.sortOrder || 0),
      options: buildOptions(),
    }
    if (form.id)
      await updateBusinessSuite(payload)
    else
      await createBusinessSuite(payload)
    message.success('业务套件已保存')
    emit('saved', payload)
    emit('update:show', false)
  }
  finally {
    saving.value = false
  }
}

function hydrateOptions() {
  let options = {}
  try {
    options = form.options ? JSON.parse(form.options) : {}
  }
  catch {
    options = {}
  }
  const adminMenu = options.adminMenu || {}
  form.createMenuDirectory = normalizeBoolean(adminMenu.syncEnabled ?? options.createMenuDirectory, false)
  form.adminMenuParentId = adminMenu.parentId ?? options.adminMenuParentId ?? null
  form.menuSort = Number(adminMenu.sort ?? options.menuSort ?? form.sortOrder ?? 0)
}

function buildOptions() {
  let options = {}
  try {
    options = form.options ? JSON.parse(form.options) : {}
  }
  catch {
    options = {}
  }
  if (form.createMenuDirectory) {
    const previousAdminMenu = options.adminMenu || {}
    options.adminMenu = {
      syncEnabled: true,
      parentId: form.adminMenuParentId || null,
      sort: Number(form.menuSort ?? form.sortOrder ?? 0),
    }
    if (previousAdminMenu.menuResourceId)
      options.adminMenu.menuResourceId = previousAdminMenu.menuResourceId
  }
  else {
    delete options.adminMenu
    delete options.adminMenuParentId
    delete options.createMenuDirectory
    delete options.menuSort
  }
  return Object.keys(options).length ? JSON.stringify(options) : null
}

function normalizeCode(value) {
  return String(value || '')
    .trim()
    .replace(/[\s-]+/g, '_')
    .replace(/\W/g, '')
    .toUpperCase()
}

function isValidCode(value) {
  return /^[a-z]\w{1,63}$/i.test(normalizeCode(value))
}

function normalizeBoolean(value, fallback) {
  if (value === undefined || value === null)
    return fallback
  if (typeof value === 'boolean')
    return value
  return String(value) === 'true' || String(value) === '1'
}

function trimToNull(value) {
  const text = String(value || '').trim()
  return text || null
}

function defaultForm() {
  return {
    id: null,
    suiteCode: '',
    suiteName: '',
    icon: '',
    description: '',
    status: 1,
    sortOrder: 0,
    options: '',
    createMenuDirectory: false,
    adminMenuParentId: null,
    menuSort: 0,
  }
}
</script>
