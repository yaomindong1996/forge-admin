<template>
  <n-drawer v-model:show="drawerShow" :width="680" placement="right">
    <n-drawer-content :title="drawerTitle">
      <n-form label-placement="left" label-width="104" size="small">
        <div class="form-section">
          <div class="section-title">
            基础信息
          </div>
          <n-form-item label="父级领域">
            <n-select
              v-model:value="form.parentId"
              clearable
              filterable
              :options="parentOptions"
              placeholder="不选则为顶级领域"
            />
          </n-form-item>
          <n-form-item label="领域编码">
            <n-input v-model:value="form.domainCode" :disabled="!!form.id" placeholder="sales_contract" />
          </n-form-item>
          <n-form-item label="领域名称">
            <n-input v-model:value="form.domainName" placeholder="销售合同域" />
          </n-form-item>
          <n-form-item label="领域说明">
            <n-input v-model:value="form.domainDesc" type="textarea" :autosize="{ minRows: 2, maxRows: 4 }" />
          </n-form-item>
          <n-form-item label="启用状态">
            <n-switch v-model:value="form.status" checked-value="ENABLED" unchecked-value="DISABLED" />
          </n-form-item>
        </div>

        <div class="form-section">
          <div class="section-title">
            默认规则
          </div>
          <div class="two-col">
            <n-form-item label="表名前缀">
              <n-input v-model:value="form.tablePrefix" placeholder="tf_f_order" />
            </n-form-item>
            <n-form-item label="配置键前缀">
              <n-input v-model:value="form.configKeyPrefix" placeholder="contract_" />
            </n-form-item>
            <n-form-item label="菜单父级">
              <MenuParentSelect v-model:value="form.menuParentId" />
            </n-form-item>
          </div>
        </div>

        <div class="form-section">
          <div class="section-title">
            代码生成默认值
          </div>
          <div class="two-col">
            <n-form-item label="Group ID">
              <n-input v-model:value="schema.codegen.groupId" placeholder="com.mdframe.forge.business" />
            </n-form-item>
            <n-form-item label="Java 基础包名">
              <n-input v-model:value="schema.codegen.domainPackage" placeholder="com.mdframe.forge.business" />
            </n-form-item>
            <n-form-item label="代码模块名">
              <n-input v-model:value="schema.codegen.moduleName" placeholder="crm" />
            </n-form-item>
            <n-form-item label="前端路径">
              <n-input v-model:value="schema.codegen.frontendBasePath" placeholder="frontend/src/views" />
            </n-form-item>
          </div>
        </div>

        <div class="form-section">
          <div class="section-title">
            AI 上下文
          </div>
          <n-form-item label="业务描述">
            <n-input v-model:value="schema.aiContext.description" type="textarea" :autosize="{ minRows: 2, maxRows: 4 }" />
          </n-form-item>
          <n-form-item label="业务术语">
            <n-input v-model:value="termsText" type="textarea" placeholder="每行一个术语" :autosize="{ minRows: 2, maxRows: 4 }" />
          </n-form-item>
          <n-form-item label="业务约束">
            <n-input v-model:value="constraintsText" type="textarea" placeholder="每行一条约束" :autosize="{ minRows: 2, maxRows: 4 }" />
          </n-form-item>
        </div>
      </n-form>

      <template #footer>
        <n-space justify="end">
          <n-button @click="drawerShow = false">
            取消
          </n-button>
          <n-button type="primary" :loading="saving" @click="save">
            保存领域
          </n-button>
        </n-space>
      </template>
    </n-drawer-content>
  </n-drawer>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { lowcodeCreateDomain, lowcodeUpdateDomain } from '@/api/lowcode-crud'
import { cloneSchema } from '../model/model-schema'
import MenuParentSelect from '../shared/MenuParentSelect.vue'

defineOptions({ name: 'DomainEditorDrawer' })

const props = defineProps({
  show: {
    type: Boolean,
    default: false,
  },
  domain: {
    type: Object,
    default: null,
  },
  domains: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['update:show', 'saved'])

const saving = ref(false)
const termsText = ref('')
const constraintsText = ref('')
const form = reactive(createBlankForm())
const schema = reactive(createBlankSchema())

const drawerShow = computed({
  get: () => props.show,
  set: value => emit('update:show', value),
})

const drawerTitle = computed(() => {
  if (form.id)
    return '编辑业务领域'
  return form.parentId ? '新增子目录' : '新增业务领域'
})

const parentOptions = computed(() => flattenDomains(props.domains)
  .filter(item => item.id !== form.id)
  .map(item => ({
    label: `${'  '.repeat(item.level)}${item.domainName} (${item.domainCode})`,
    value: item.id,
  })))

watch(
  () => props.show,
  (visible) => {
    if (visible)
      resetForm(props.domain)
  },
)

function createBlankForm() {
  return {
    id: null,
    parentId: null,
    domainCode: '',
    domainName: '',
    domainDesc: '',
    icon: '',
    sort: 0,
    status: 'ENABLED',
    menuParentId: null,
    tablePrefix: '',
    configKeyPrefix: '',
  }
}

function createBlankSchema() {
  return {
    aiContext: {
      description: '',
      terms: [],
      commonObjects: [],
      fieldNamingPreference: 'lowerCamel',
      constraints: [],
      generationNotes: [],
    },
    naming: {
      tablePrefix: '',
      configKeyPrefix: '',
      objectCodeStyle: 'lower_snake',
    },
    defaults: {
      menuParentId: null,
    },
    codegen: {
      groupId: '',
      domainPackage: '',
      moduleName: '',
      frontendBasePath: '',
    },
    fieldTemplates: [],
    dictRecommendations: [],
    securityPolicies: [],
  }
}

function resetForm(domain) {
  Object.assign(form, createBlankForm(), domain || {})
  delete form.defaultTableMode
  const nextSchema = cloneSchema(domain?.domainSchema || createBlankSchema())
  if (nextSchema.defaults)
    delete nextSchema.defaults.tableMode
  if (nextSchema.defaults) {
    delete nextSchema.defaults.appType
    delete nextSchema.defaults.layoutType
  }
  if (nextSchema.naming)
    delete nextSchema.naming.tableMode
  Object.assign(schema, createBlankSchema(), nextSchema)
  schema.codegen = {
    ...createBlankSchema().codegen,
    ...(nextSchema.codegen || {}),
  }
  termsText.value = (schema.aiContext.terms || []).join('\n')
  constraintsText.value = (schema.aiContext.constraints || []).join('\n')
}

async function save() {
  if (!form.domainCode || !form.domainName) {
    window.$message?.warning('请填写领域编码和领域名称')
    return
  }
  saving.value = true
  try {
    schema.aiContext.terms = splitLines(termsText.value)
    schema.aiContext.constraints = splitLines(constraintsText.value)
    schema.naming.tablePrefix = form.tablePrefix
    schema.naming.configKeyPrefix = form.configKeyPrefix
    schema.defaults.menuParentId = form.menuParentId
    schema.codegen = {
      groupId: trimText(schema.codegen?.groupId),
      domainPackage: trimText(schema.codegen?.domainPackage),
      moduleName: trimText(schema.codegen?.moduleName),
      frontendBasePath: trimText(schema.codegen?.frontendBasePath),
    }

    const payload = {
      id: form.id,
      parentId: form.parentId || 0,
      domainCode: form.domainCode,
      domainName: form.domainName,
      domainDesc: form.domainDesc,
      icon: form.icon,
      sort: form.sort,
      status: form.status,
      menuParentId: form.menuParentId,
      tablePrefix: form.tablePrefix,
      configKeyPrefix: form.configKeyPrefix,
      domainSchema: cloneSchema(schema),
    }
    if (form.id)
      await lowcodeUpdateDomain(payload)
    else
      await lowcodeCreateDomain(payload)
    window.$message?.success('业务领域已保存')
    emit('saved')
    drawerShow.value = false
  }
  catch (e) {
    window.$message?.error(e?.message || '保存业务领域失败')
  }
  finally {
    saving.value = false
  }
}

function splitLines(value) {
  return String(value || '')
    .split(/\n|,/)
    .map(item => item.trim())
    .filter(Boolean)
}

function trimText(value) {
  return String(value || '').trim()
}

function flattenDomains(nodes, level = 0) {
  const result = []
  for (const node of nodes || []) {
    result.push({ ...node, level })
    if (node.children?.length)
      result.push(...flattenDomains(node.children, level + 1))
  }
  return result
}
</script>

<style scoped>
.form-section {
  border-bottom: 1px solid #eef2f7;
  margin-bottom: 16px;
  padding-bottom: 8px;
}

.section-title {
  margin-bottom: 12px;
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 700;
}

.two-col {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  column-gap: 12px;
}

@media (max-width: 720px) {
  .two-col {
    grid-template-columns: 1fr;
  }
}
</style>
