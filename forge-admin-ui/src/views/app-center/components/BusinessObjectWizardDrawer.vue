<template>
  <n-drawer :show="show" width="640" @update:show="value => emit('update:show', value)">
    <n-drawer-content title="新建业务对象" closable>
      <div class="wizard-shell">
        <n-steps :current="currentStep" size="small">
          <n-step title="业务套件" />
          <n-step title="创建方式" />
          <n-step title="对象信息" />
        </n-steps>

        <section v-if="currentStep === 1" class="wizard-step">
          <n-radio-group v-model:value="form.suiteMode" class="choice-row">
            <n-radio-button value="EXISTING">
              选择已有套件
            </n-radio-button>
            <n-radio-button value="NEW">
              新建业务套件
            </n-radio-button>
          </n-radio-group>

          <template v-if="form.suiteMode === 'EXISTING'">
            <n-form-item label="业务套件" required>
              <n-select
                v-model:value="form.suiteCode"
                filterable
                :options="suiteOptions"
                placeholder="选择业务对象所属套件"
              />
            </n-form-item>
          </template>

          <template v-else>
            <n-form-item label="套件名称" required>
              <n-input v-model:value="form.newSuiteName" placeholder="例如：合同管理" />
            </n-form-item>
            <n-form-item label="套件编码" required>
              <n-input
                v-model:value="form.newSuiteCode"
                placeholder="例如：CONTRACT"
                @blur="form.newSuiteCode = normalizeCode(form.newSuiteCode)"
              />
            </n-form-item>
            <n-form-item label="套件图标">
              <IconSelector v-model="form.newSuiteIcon" />
            </n-form-item>
            <n-grid :cols="2" :x-gap="12">
              <n-form-item-gi label="创建管理端目录">
                <n-switch v-model:value="form.createSuiteMenu" />
              </n-form-item-gi>
              <n-form-item-gi v-if="form.createSuiteMenu" label="目录排序">
                <n-input-number v-model:value="form.suiteMenuSort" :min="0" :show-button="false" />
              </n-form-item-gi>
            </n-grid>
            <n-form-item v-if="form.createSuiteMenu" label="父级菜单或模块">
              <MenuParentSelect v-model:value="form.suiteMenuParentId" placeholder="选择套件目录挂载位置，默认顶级" />
            </n-form-item>
            <n-form-item label="业务说明">
              <n-input
                v-model:value="form.newSuiteDescription"
                type="textarea"
                placeholder="说明这个套件覆盖的业务范围"
              />
            </n-form-item>
          </template>
        </section>

        <section v-if="currentStep === 2" class="wizard-step">
          <n-radio-group v-model:value="form.createMode" class="method-grid">
            <label v-for="item in createModes" :key="item.value" class="method-card">
              <n-radio :value="item.value" />
              <span>
                <strong>{{ item.label }}</strong>
                <small>{{ item.description }}</small>
              </span>
            </label>
          </n-radio-group>
        </section>

        <section v-if="currentStep === 3" class="wizard-step">
          <n-form label-placement="top">
            <n-grid :cols="2" :x-gap="12">
              <n-form-item-gi label="对象名称" required>
                <n-input v-model:value="form.objectName" placeholder="例如：客户" />
              </n-form-item-gi>
              <n-form-item-gi label="对象编码" required>
                <n-input
                  v-model:value="form.objectCode"
                  placeholder="例如：CUSTOMER"
                  @blur="form.objectCode = normalizeCode(form.objectCode)"
                />
              </n-form-item-gi>
              <n-form-item-gi label="对象类型" required>
                <DictSelect v-model:value="form.objectType" dict-type="ai_business_object_type" />
              </n-form-item-gi>
              <n-form-item-gi label="显示字段">
                <n-input v-model:value="form.displayField" placeholder="例如：customerName" />
              </n-form-item-gi>
              <n-form-item-gi label="图标">
                <IconSelector v-model="form.icon" />
              </n-form-item-gi>
              <n-form-item-gi label="启用状态">
                <n-switch v-model:value="form.status" :checked-value="1" :unchecked-value="0" />
              </n-form-item-gi>
            </n-grid>
            <n-form-item label="业务说明">
              <n-input
                v-model:value="form.description"
                type="textarea"
                placeholder="说明这个对象管理的业务信息和典型使用场景"
              />
            </n-form-item>
          </n-form>
        </section>
      </div>

      <template #footer>
        <n-space justify="space-between" align="center">
          <span class="wizard-hint">{{ footerHint }}</span>
          <n-space>
            <n-button @click="emit('update:show', false)">
              取消
            </n-button>
            <n-button :disabled="currentStep === 1" @click="currentStep -= 1">
              上一步
            </n-button>
            <n-button v-if="currentStep < 3" type="primary" @click="nextStep">
              下一步
            </n-button>
            <n-button v-else type="primary" :loading="saving" @click="saveObject">
              保存对象
            </n-button>
          </n-space>
        </n-space>
      </template>
    </n-drawer-content>
  </n-drawer>
</template>

<script setup>
import { useMessage } from 'naive-ui'
import { computed, reactive, ref, watch } from 'vue'
import { createBusinessObject, createBusinessSuite } from '@/api/business-app'
import DictSelect from '@/components/DictSelect.vue'
import IconSelector from '@/components/IconSelector.vue'
import MenuParentSelect from '@/components/lowcode-builder/shared/MenuParentSelect.vue'

const props = defineProps({
  show: {
    type: Boolean,
    default: false,
  },
  suites: {
    type: Array,
    default: () => [],
  },
  defaultSuiteCode: {
    type: String,
    default: null,
  },
})

const emit = defineEmits(['update:show', 'saved'])
const message = useMessage()
const currentStep = ref(1)
const saving = ref(false)
const form = reactive(defaultForm())

const createModes = [
  {
    label: '从空白对象创建',
    value: 'BLANK',
    description: '保存对象草稿后进入设计器，继续维护字段、页面和发布。',
  },
  {
    label: '从数据库表导入',
    value: 'DB_IMPORT',
    description: '保存对象草稿后进入设计器，用业务字段语言继续整理导入结果。',
  },
  {
    label: '从 AI 描述生成',
    value: 'AI_GENERATE',
    description: '保存对象草稿后进入设计器，继续确认 AI 生成的字段和页面。',
  },
]

const suiteOptions = computed(() => props.suites.map(item => ({
  label: item.suiteName || item.suiteCode,
  value: item.suiteCode,
})))

const footerHint = computed(() => {
  if (currentStep.value === 1)
    return '业务对象必须归属到一个业务套件。'
  if (currentStep.value === 2)
    return '三种创建方式都会进入同一个业务对象设计器。'
  return '保存后进入设计器继续维护字段、页面和发布检查。'
})

watch(() => props.show, (visible) => {
  if (!visible)
    return
  Object.assign(form, defaultForm())
  currentStep.value = 1
  if (props.defaultSuiteCode) {
    form.suiteMode = 'EXISTING'
    form.suiteCode = props.defaultSuiteCode
  }
  else if (props.suites.length) {
    form.suiteCode = props.suites[0].suiteCode
  }
})

function nextStep() {
  if (!validateStep())
    return
  currentStep.value += 1
}

async function saveObject() {
  if (!validateStep())
    return
  saving.value = true
  try {
    const suiteCode = await resolveSuiteCode()
    const res = await createBusinessObject({
      suiteCode,
      objectName: form.objectName.trim(),
      objectCode: normalizeCode(form.objectCode),
      objectType: form.objectType,
      displayField: trimToNull(form.displayField),
      icon: trimToNull(form.icon),
      description: trimToNull(form.description),
      status: form.status,
      options: JSON.stringify({ createMode: form.createMode }),
    })
    message.success('业务对象已创建，正在进入设计器')
    emit('saved', {
      id: res.data,
      suiteCode,
      objectCode: normalizeCode(form.objectCode),
      objectName: form.objectName.trim(),
      createMode: form.createMode,
      nextAction: 'OPEN_DESIGNER',
      designerPanel: resolveDesignerPanel(form.createMode),
    })
    emit('update:show', false)
  }
  finally {
    saving.value = false
  }
}

function resolveDesignerPanel(createMode) {
  switch (createMode) {
    case 'BLANK':
      return 'form'
    case 'DB_IMPORT':
    case 'AI_GENERATE':
      return 'form'
    default:
      return 'form'
  }
}

async function resolveSuiteCode() {
  if (form.suiteMode === 'EXISTING')
    return form.suiteCode
  const suiteCode = normalizeCode(form.newSuiteCode)
  await createBusinessSuite({
    suiteCode,
    suiteName: form.newSuiteName.trim(),
    icon: trimToNull(form.newSuiteIcon),
    description: trimToNull(form.newSuiteDescription),
    status: 1,
    sortOrder: 0,
    options: buildSuiteOptions(),
  })
  return suiteCode
}

function buildSuiteOptions() {
  if (!form.createSuiteMenu)
    return null
  return JSON.stringify({
    adminMenu: {
      syncEnabled: true,
      parentId: form.suiteMenuParentId || null,
      sort: Number(form.suiteMenuSort || 0),
    },
  })
}

function validateStep() {
  if (currentStep.value === 1) {
    if (form.suiteMode === 'EXISTING' && !form.suiteCode) {
      message.warning('请选择业务套件')
      return false
    }
    if (form.suiteMode === 'NEW') {
      if (!form.newSuiteName.trim()) {
        message.warning('请输入套件名称')
        return false
      }
      if (!isValidCode(form.newSuiteCode)) {
        message.warning('套件编码需以字母开头，仅包含字母、数字和下划线')
        return false
      }
    }
  }
  if (currentStep.value === 2 && !form.createMode) {
    message.warning('请选择创建方式')
    return false
  }
  if (currentStep.value === 3) {
    if (!form.objectName.trim()) {
      message.warning('请输入对象名称')
      return false
    }
    if (!isValidCode(form.objectCode)) {
      message.warning('对象编码需以字母开头，仅包含字母、数字和下划线')
      return false
    }
  }
  return true
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

function trimToNull(value) {
  const text = String(value || '').trim()
  return text || null
}

function defaultForm() {
  return {
    suiteMode: 'EXISTING',
    suiteCode: null,
    newSuiteName: '',
    newSuiteCode: '',
    newSuiteIcon: '',
    newSuiteDescription: '',
    createSuiteMenu: true,
    suiteMenuParentId: null,
    suiteMenuSort: 0,
    createMode: 'BLANK',
    objectName: '',
    objectCode: '',
    objectType: 'MASTER',
    displayField: '',
    icon: '',
    description: '',
    status: 1,
  }
}
</script>

<style scoped>
.wizard-shell {
  display: grid;
  gap: 18px;
}

.wizard-step {
  min-height: 340px;
  padding-top: 4px;
}

.choice-row {
  margin-bottom: 16px;
}

.method-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.method-card {
  display: grid;
  grid-template-columns: 24px minmax(0, 1fr);
  gap: 10px;
  min-height: 104px;
  cursor: pointer;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 14px;
  transition:
    border-color 160ms ease,
    background 160ms ease,
    box-shadow 160ms ease;
}

.method-card:hover {
  border-color: #2f6feb;
  background: #f8fbff;
  box-shadow: 0 8px 20px rgb(15 23 42 / 7%);
}

.method-card strong,
.method-card small {
  display: block;
}

.method-card strong {
  color: #111827;
  font-size: 14px;
  line-height: 1.45;
}

.method-card small {
  margin-top: 6px;
  color: #6b7280;
  font-size: 12px;
  line-height: 1.5;
}

.wizard-hint {
  max-width: 280px;
  color: #6b7280;
  font-size: 12px;
  line-height: 1.5;
}

@media (max-width: 640px) {
  .method-grid {
    grid-template-columns: 1fr;
  }

  .wizard-hint {
    display: none;
  }
}
</style>
