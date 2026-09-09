<template>
  <div class="template-page">
    <MasterDetailWorkspace :aside-width="280">
      <template #aside>
        <div class="tpl-aside">
          <div class="tpl-aside-head">
            <div>
              <strong>消息模板</strong>
              <p>写好标题和正文，发消息时套用</p>
            </div>
            <NButton type="primary" size="small" @click="startCreate">
              新增
            </NButton>
          </div>
          <n-input
            v-model:value="keyword"
            size="small"
            clearable
            placeholder="搜索名称或编码"
          />
          <n-spin :show="listLoading" class="tpl-aside-list-spin">
            <n-scrollbar class="tpl-aside-scroll">
              <button
                v-for="item in filteredList"
                :key="item.id"
                type="button"
                class="tpl-item"
                :class="{ active: String(item.id) === String(form.id) }"
                @click="selectItem(item)"
              >
                <span class="tpl-item-name">{{ item.templateName || '未命名' }}</span>
                <span class="tpl-item-meta">
                  {{ item.templateCode || '-' }}
                  <em :class="item.enabled === 1 ? 'is-on' : 'is-off'">{{ item.enabled === 1 ? '启用' : '停用' }}</em>
                </span>
              </button>
              <n-empty v-if="!listLoading && !filteredList.length" size="small" description="还没有模板，先新增一个" />
            </n-scrollbar>
          </n-spin>
        </div>
      </template>

      <div class="tpl-main">
        <template v-if="editing">
          <div class="tpl-main-head">
            <div>
              <strong>{{ form.id ? (form.templateName || '未命名') : '新增消息模板' }}</strong>
              <p>{{ form.id ? '改完保存即生效。发消息时填模板编码，正文会按这里生成。' : '先起名字和编码，再写标题、正文和样式。' }}</p>
            </div>
            <div class="tpl-main-actions">
              <n-switch :value="form.enabled === 1" @update:value="checked => form.enabled = checked ? 1 : 0">
                <template #checked>
                  启用
                </template>
                <template #unchecked>
                  停用
                </template>
              </n-switch>
              <NButton v-if="form.id" size="small" quaternary type="error" :loading="saving" @click="confirmRemove">
                删除
              </NButton>
              <NButton type="primary" size="small" :loading="saving" @click="saveItem">
                保存
              </NButton>
            </div>
          </div>

          <n-tabs v-model:value="activeTab" type="line" size="small" class="tpl-tabs">
            <n-tab-pane name="content" tab="怎么写" display-directive="show">
              <n-scrollbar class="tpl-pane-scroll">
                <div class="tpl-content">
                  <n-form
                    ref="formRef"
                    :model="form"
                    :rules="formRules"
                    label-placement="left"
                    label-width="84"
                    class="tpl-form"
                  >
                    <n-form-item label="模板名称" path="templateName">
                      <n-input v-model:value="form.templateName" maxlength="100" placeholder="例如：采购审批待办" />
                    </n-form-item>
                    <n-form-item label="模板编码" path="templateCode">
                      <n-input
                        :value="form.templateCode"
                        :disabled="Boolean(form.id)"
                        maxlength="50"
                        placeholder="例如：PURCHASE_TODO"
                        @update:value="onTemplateCodeInput"
                      />
                      <p class="field-hint">
                        发给系统和开发用。保存后不要改。
                      </p>
                    </n-form-item>
                    <n-form-item label="用在哪">
                      <div class="field-row">
                        <n-select v-model:value="form.type" :options="messageTypeOptions" placeholder="消息类型" />
                        <n-select v-model:value="form.defaultChannel" :options="channelOptions" placeholder="发送渠道" />
                      </div>
                      <p class="field-hint">
                        {{ styleGuide }}
                      </p>
                    </n-form-item>
                  </n-form>

                  <div class="template-designer">
                    <aside class="template-variable-panel">
                      <div class="variable-panel-head">
                        <div>
                          <div class="variable-panel-title">
                            可填内容
                          </div>
                          <div class="variable-panel-count">
                            点一下插到{{ activeTemplateField === 'titleTemplate' ? '标题' : '正文' }}光标处
                          </div>
                        </div>
                        <n-input
                          v-model:value="variableKeyword"
                          size="small"
                          clearable
                          placeholder="搜索或输入变量名"
                        />
                      </div>
                      <div class="template-target-switch">
                        <button
                          type="button"
                          :class="{ active: activeTemplateField === 'titleTemplate' }"
                          @click="setActiveTemplateField('titleTemplate')"
                        >
                          标题
                        </button>
                        <button
                          type="button"
                          :class="{ active: activeTemplateField === 'contentTemplate' }"
                          @click="setActiveTemplateField('contentTemplate')"
                        >
                          正文
                        </button>
                      </div>
                      <n-scrollbar class="variable-scrollbar">
                        <button
                          v-for="item in visibleVariables"
                          :key="item.key"
                          type="button"
                          class="variable-row"
                          :class="{ used: item.used, custom: item.source === 'current' }"
                          @click="insertTemplateVariable(item.key)"
                        >
                          <span class="variable-name">{{ item.label }}</span>
                          <span class="variable-code">{{ formatPlaceholder(item.key) }}</span>
                        </button>
                        <button
                          v-if="canInsertCustomVariable"
                          type="button"
                          class="variable-row custom"
                          @click="addCustomVariable"
                        >
                          <span class="variable-name">添加自定义变量</span>
                          <span class="variable-code">{{ formatPlaceholder(normalizeVariableKey(variableKeyword)) }}</span>
                        </button>
                        <div v-if="visibleVariables.length === 0 && !canInsertCustomVariable" class="variable-empty">
                          无匹配变量
                        </div>
                      </n-scrollbar>
                    </aside>

                    <section class="template-editor-panel">
                      <div class="template-field-block">
                        <div class="template-field-head">
                          <span>标题</span>
                          <span>标题只显示文字，不要放样式</span>
                        </div>
                        <n-input
                          :value="form.titleTemplate"
                          type="textarea"
                          :rows="2"
                          placeholder="例如：您有新的采购审批"
                          @focus="rememberTemplateCursor('titleTemplate', $event)"
                          @click="rememberTemplateCursor('titleTemplate', $event)"
                          @keyup="rememberTemplateCursor('titleTemplate', $event)"
                          @update:value="setTemplateField('titleTemplate', $event)"
                        />
                      </div>

                      <div class="template-field-block">
                        <div class="template-field-head">
                          <span>正文</span>
                          <span>{{ styleMode === 'plain' ? '纯文字' : '可点选样式' }}</span>
                        </div>
                        <div v-if="styleSnippets.length" class="style-toolbar">
                          <button
                            v-for="snippet in styleSnippets"
                            :key="snippet.key"
                            type="button"
                            class="style-chip"
                            :title="snippet.hint"
                            @click="insertStyleSnippet(snippet)"
                          >
                            {{ snippet.label }}
                          </button>
                        </div>
                        <n-input
                          :value="form.contentTemplate"
                          type="textarea"
                          :rows="8"
                          :placeholder="contentPlaceholder"
                          @focus="rememberTemplateCursor('contentTemplate', $event)"
                          @click="rememberTemplateCursor('contentTemplate', $event)"
                          @keyup="rememberTemplateCursor('contentTemplate', $event)"
                          @update:value="setTemplateField('contentTemplate', $event)"
                        />
                      </div>

                      <div class="template-preview" :data-mode="styleMode">
                        <div class="template-preview-head">
                          {{ previewTitle }}
                        </div>
                        <div class="template-preview-title">
                          {{ renderTemplatePreview(form.titleTemplate) || '标题预览' }}
                        </div>
                        <div
                          class="template-preview-content"
                          v-html="previewHtml || '正文预览'"
                        />
                      </div>
                    </section>
                  </div>
                </div>
              </n-scrollbar>
            </n-tab-pane>

            <n-tab-pane name="usage" tab="怎么用" display-directive="show">
              <n-scrollbar class="tpl-pane-scroll">
                <div class="usage-panel">
                  <section>
                    <h3>配好之后怎么用</h3>
                    <ol>
                      <li>发消息时填模板编码 <code>{{ form.templateCode || '模板编码' }}</code>，不必再手写标题和正文。</li>
                      <li>把上面点过的变量放到发送参数里，例如任务标题、流程名称。</li>
                      <li>到「消息管理」试发一条，选这个编码，看收到的内容和跳转是否对。</li>
                      <li>流程待办/结果/抄送可在流程设计器里绑定对应卡片模板。</li>
                    </ol>
                    <NButton size="small" secondary @click="goTrySend">
                      去消息管理试发一条
                    </NButton>
                  </section>

                  <section>
                    <h3>样式怎么配</h3>
                    <p class="usage-lead">
                      {{ styleGuide }}
                    </p>
                    <ul v-if="styleMode === 'card'" class="style-legend">
                      <li><span class="legend gray">次要说明</span>灰色小字，放卡片抬头</li>
                      <li><span class="legend normal">正文</span>普通信息行，可夹变量</li>
                      <li><span class="legend highlight">强调</span>行动号召，例如“点击查看”</li>
                    </ul>
                  </section>

                  <section>
                    <h3>给开发同事</h3>
                    <p class="usage-lead">
                      发送时带上模板编码和变量。正文样式已经写在模板里，代码里不用再拼 HTML。
                    </p>
                    <div class="code-block">
                      <div class="code-head">
                        <span>Java</span>
                        <NButton size="tiny" quaternary @click="copyText(javaExample, 'Java 示例已复制')">
                          复制
                        </NButton>
                      </div>
                      <pre><code>{{ javaExample }}</code></pre>
                    </div>
                    <div class="code-block">
                      <div class="code-head">
                        <span>接口</span>
                        <NButton size="tiny" quaternary @click="copyText(httpExample, '接口示例已复制')">
                          复制
                        </NButton>
                      </div>
                      <pre><code>{{ httpExample }}</code></pre>
                    </div>
                  </section>
                </div>
              </n-scrollbar>
            </n-tab-pane>
          </n-tabs>
        </template>

        <div v-else class="tpl-empty">
          <p>从左侧选一个模板，或新增一个。</p>
          <p>模板管的是「发出去长什么样」。点开去哪张单，在业务配置里登记。</p>
        </div>
      </div>
    </MasterDetailWorkspace>
  </div>
</template>

<script setup>
import { NButton } from 'naive-ui'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import messageApi from '@/api/message'
import MasterDetailWorkspace from '@/components/common/MasterDetailWorkspace.vue'
import { useDict } from '@/composables/useDict'
import { copy } from '@/utils/clipboard'
import {
  buildHttpTemplateExample,
  buildJavaTemplateExample,
  extractTemplateVariables,
  formatPlaceholder,
  getStyleGuide,
  getStyleSnippets,
  hasUnsupportedAtVariables,
  insertAtCursor,
  normalizeTemplateCode,
  renderPreviewHtml,
  renderTemplatePreview,
  resolveContentStyleMode,
  SYSTEM_BUILT_IN_VARIABLES,
  TEMPLATE_VARIABLE_CATALOG,
} from './template-style'

defineOptions({ name: 'MessageTemplate' })

const router = useRouter()
const { dict } = useDict('sys_message_type', 'sys_message_channel')
const list = ref([])
const listLoading = ref(false)
const saving = ref(false)
const keyword = ref('')
const editing = ref(false)
const activeTab = ref('content')
const formRef = ref(null)
const form = reactive(createEmptyForm())
const activeTemplateField = ref('contentTemplate')
const variableKeyword = ref('')
const templateCursorMap = ref({
  titleTemplate: null,
  contentTemplate: null,
})

const messageTypeOptions = computed(() => dict.value.sys_message_type || [])
const channelOptions = computed(() => dict.value.sys_message_channel || [])
const filteredList = computed(() => {
  const query = keyword.value.trim().toLowerCase()
  if (!query)
    return list.value
  return list.value.filter(item => `${item.templateName || ''} ${item.templateCode || ''}`.toLowerCase().includes(query))
})
const styleMode = computed(() => resolveContentStyleMode({
  channel: form.defaultChannel,
  templateCode: form.templateCode,
}))
const styleSnippets = computed(() => getStyleSnippets(styleMode.value))
const styleGuide = computed(() => getStyleGuide(styleMode.value))
const previewTitle = computed(() => {
  if (styleMode.value === 'card')
    return '卡片预览（企业微信/站内信）'
  if (styleMode.value === 'markdown')
    return '卡片预览（钉钉）'
  if (styleMode.value === 'plain')
    return '短信预览'
  return '正文预览'
})
const contentPlaceholder = computed(() => {
  if (styleMode.value === 'card')
    return '点上方「次要说明 / 正文 / 强调」插入一行，再改文字或插入变量'
  if (styleMode.value === 'markdown')
    return '可用 ### 标题、- 列表。点上方按钮插入'
  if (styleMode.value === 'plain')
    return `只写文字，例如：您的验证码是 ${formatPlaceholder('code')}`
  return '可写简单 HTML，或点样式按钮插入'
})
const previewHtml = computed(() => renderPreviewHtml(form.contentTemplate, styleMode.value))
const usedVariables = computed(() => extractTemplateVariables(form.titleTemplate, form.contentTemplate))
const visibleVariables = computed(() => {
  const query = String(variableKeyword.value || '').trim().toLowerCase()
  const variables = resolveTemplateVariables()
  if (!query)
    return variables
  return variables.filter(item => item.key.toLowerCase().includes(query) || item.label.toLowerCase().includes(query))
})
const canInsertCustomVariable = computed(() => {
  const key = normalizeVariableKey(variableKeyword.value)
  if (!/^[a-z_]\w*$/i.test(key))
    return false
  return !resolveTemplateVariables().some(item => item.key === key)
})
const javaExample = computed(() => buildJavaTemplateExample({
  templateCode: form.templateCode,
  variables: usedVariables.value,
}))
const httpExample = computed(() => buildHttpTemplateExample({
  templateCode: form.templateCode,
  variables: usedVariables.value,
}))
const formRules = {
  templateName: { required: true, message: '请填写模板名称', trigger: 'blur' },
  templateCode: { required: true, message: '请填写模板编码', trigger: 'blur' },
}

function createEmptyForm() {
  return {
    id: null,
    templateName: '',
    templateCode: '',
    type: 'SYSTEM',
    defaultChannel: 'WEB',
    titleTemplate: '',
    contentTemplate: '',
    enabled: 1,
    remark: '',
  }
}

function applyForm(source = {}) {
  Object.assign(form, createEmptyForm(), source)
}

function onTemplateCodeInput(value) {
  form.templateCode = normalizeTemplateCode(value)
}

function resolveTemplateVariables() {
  const usedKeys = new Set(usedVariables.value)
  const catalog = TEMPLATE_VARIABLE_CATALOG[form.templateCode] || []
  const variableMap = new Map()
  SYSTEM_BUILT_IN_VARIABLES.forEach((item) => {
    variableMap.set(item.key, { ...item, used: usedKeys.has(item.key), source: 'system' })
  })
  catalog.forEach((item) => {
    variableMap.set(item.key, { ...item, used: usedKeys.has(item.key), source: 'backend' })
  })
  usedKeys.forEach((key) => {
    if (!variableMap.has(key))
      variableMap.set(key, { key, label: key, used: true, source: 'current' })
  })
  return Array.from(variableMap.values())
}

function normalizeVariableKey(value) {
  return String(value || '').trim().replace(/^\$\{|\}$/g, '').replace(/^@/, '')
}

function setActiveTemplateField(field) {
  activeTemplateField.value = field
}

function setTemplateField(field, value) {
  setActiveTemplateField(field)
  form[field] = value
}

function rememberTemplateCursor(field, event) {
  setActiveTemplateField(field)
  const target = event?.target
  if (!target || typeof target.selectionStart !== 'number')
    return
  templateCursorMap.value = {
    ...templateCursorMap.value,
    [field]: target.selectionStart,
  }
}

function insertIntoActiveField(snippet) {
  const field = activeTemplateField.value || 'contentTemplate'
  const result = insertAtCursor(form[field], snippet, templateCursorMap.value[field])
  form[field] = result.value
  templateCursorMap.value = {
    ...templateCursorMap.value,
    [field]: result.cursor,
  }
}

function insertTemplateVariable(key) {
  insertIntoActiveField(formatPlaceholder(key))
}

function addCustomVariable() {
  insertTemplateVariable(normalizeVariableKey(variableKeyword.value))
  variableKeyword.value = ''
}

function insertStyleSnippet(snippet) {
  activeTemplateField.value = 'contentTemplate'
  insertIntoActiveField(snippet.insert)
}

async function loadList(keepSelection = true) {
  listLoading.value = true
  try {
    const res = await messageApi.getTemplatePage({
      pageNum: 1,
      pageSize: 200,
    })
    list.value = res?.data?.records || []
    if (keepSelection && form.id) {
      const current = list.value.find(item => String(item.id) === String(form.id))
      if (current)
        applyForm(current)
    }
  }
  catch (error) {
    list.value = []
    window.$message?.error(error?.message || '加载消息模板失败')
  }
  finally {
    listLoading.value = false
  }
}

function startCreate() {
  editing.value = true
  activeTab.value = 'content'
  applyForm()
}

function selectItem(item) {
  editing.value = true
  activeTab.value = 'content'
  applyForm(item)
}

async function saveItem() {
  try {
    await formRef.value?.validate()
  }
  catch {
    activeTab.value = 'content'
    return
  }
  if (!String(form.contentTemplate || '').trim()) {
    window.$message?.warning('请填写正文')
    activeTab.value = 'content'
    return
  }
  if (hasUnsupportedAtVariables(form.titleTemplate, form.contentTemplate)) {
    window.$message?.warning(`请使用 ${formatPlaceholder('变量')}，@变量 发出去不会被替换`)
    return
  }
  saving.value = true
  try {
    const payload = {
      id: form.id,
      templateName: form.templateName.trim(),
      templateCode: normalizeTemplateCode(form.templateCode),
      type: form.type || 'SYSTEM',
      defaultChannel: form.defaultChannel || 'WEB',
      titleTemplate: String(form.titleTemplate || '').trim(),
      contentTemplate: String(form.contentTemplate || '').trim(),
      enabled: form.enabled === 1 ? 1 : 0,
      remark: form.remark?.trim() || '',
    }
    if (payload.id)
      await messageApi.updateTemplate(payload)
    else
      await messageApi.createTemplate(payload)
    window.$message?.success('已保存。发消息时填这个模板编码即可。')
    await loadList(false)
    const saved = list.value.find(item => item.templateCode === payload.templateCode)
    if (saved) {
      applyForm(saved)
      editing.value = true
    }
    activeTab.value = 'usage'
  }
  catch (error) {
    window.$message?.error(error?.message || '保存失败')
  }
  finally {
    saving.value = false
  }
}

function confirmRemove() {
  window.$dialog?.warning({
    title: '删除这个模板？',
    content: '已经发出的消息还在，之后按这个编码发送将找不到模板。',
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: removeItem,
  })
}

async function removeItem() {
  if (!form.id)
    return
  saving.value = true
  try {
    await messageApi.deleteTemplate(form.id)
    window.$message?.success('已删除')
    applyForm()
    editing.value = false
    await loadList(false)
  }
  catch (error) {
    window.$message?.error(error?.message || '删除失败')
  }
  finally {
    saving.value = false
  }
}

function goTrySend() {
  router.push('/message/manage')
}

function copyText(text, successMsg) {
  copy(text, successMsg)
}

onMounted(async () => {
  await loadList(false)
  if (list.value[0])
    selectItem(list.value[0])
})
</script>

<style scoped>
.template-page {
  height: 100%;
  min-height: 0;
}

.tpl-aside,
.tpl-main {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}

.tpl-aside {
  gap: 8px;
  padding: 10px;
}

.tpl-aside-head,
.tpl-main-head {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  justify-content: space-between;
}

.tpl-aside-head strong,
.tpl-main-head strong {
  display: block;
  color: var(--text-primary, #0f172a);
  font-size: 14px;
  line-height: 22px;
}

.tpl-aside-head p,
.tpl-main-head p,
.field-hint,
.usage-lead {
  margin: 2px 0 0;
  color: var(--text-tertiary, #64748b);
  font-size: 12px;
  line-height: 18px;
}

.tpl-aside-list-spin,
.tpl-aside-scroll,
.tpl-tabs,
.tpl-pane-scroll {
  flex: 1;
  min-height: 0;
}

.tpl-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
  width: 100%;
  margin-bottom: 4px;
  padding: 8px 10px;
  border: 1px solid transparent;
  border-radius: 4px;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.tpl-item:hover {
  background: var(--bg-secondary, #f8fafc);
}

.tpl-item.active {
  border-color: var(--border-light, #e2e8f0);
  background: color-mix(in srgb, var(--primary-color, #2080f0) 8%, white);
}

.tpl-item-name {
  color: var(--text-primary, #0f172a);
  font-size: 13px;
  line-height: 20px;
}

.tpl-item-meta {
  display: flex;
  gap: 8px;
  align-items: center;
  color: var(--text-tertiary, #94a3b8);
  font-size: 11px;
  line-height: 16px;
}

.tpl-item-meta em {
  font-style: normal;
}

.tpl-item-meta em.is-on {
  color: var(--success-color, #18a058);
}

.tpl-main-head {
  padding: 10px 16px;
  border-bottom: 1px solid var(--border-light, #eef2f6);
}

.tpl-main-actions {
  display: flex;
  flex-shrink: 0;
  gap: 8px;
  align-items: center;
}

.tpl-tabs {
  padding: 0 16px 12px;
}

.tpl-tabs :deep(.n-tabs-pane-wrapper),
.tpl-tabs :deep(.n-tab-pane) {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
}

.tpl-content {
  padding: 12px 4px 24px;
}

.tpl-form {
  max-width: 760px;
  margin-bottom: 12px;
}

.tpl-form :deep(.n-form-item-blank) {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  width: 100%;
}

.field-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  width: 100%;
}

.template-designer {
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr);
  gap: 12px;
  width: 100%;
}

.template-variable-panel {
  min-width: 0;
  padding: 10px;
  border: 1px solid var(--border-light, #e2e8f0);
  border-radius: 4px;
  background: var(--bg-secondary, #f8fafc);
}

.variable-panel-head {
  display: grid;
  gap: 8px;
}

.variable-panel-title {
  color: var(--text-primary, #0f172a);
  font-size: 13px;
  font-weight: 600;
}

.variable-panel-count {
  color: var(--text-tertiary, #64748b);
  font-size: 12px;
  line-height: 18px;
}

.template-target-switch {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 4px;
  margin: 10px 0;
  padding: 3px;
  border-radius: 4px;
  background: #eef1f5;
}

.template-target-switch button {
  height: 26px;
  border: 0;
  border-radius: 3px;
  background: transparent;
  color: var(--text-secondary, #475569);
  cursor: pointer;
  font-size: 12px;
}

.template-target-switch button.active {
  background: #fff;
  color: var(--primary-color, #2080f0);
}

.variable-scrollbar {
  max-height: 360px;
}

.variable-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  align-items: center;
  width: 100%;
  min-height: 32px;
  margin-bottom: 4px;
  padding: 6px 8px;
  border: 1px solid transparent;
  border-radius: 4px;
  background: #fff;
  cursor: pointer;
  text-align: left;
}

.variable-row:hover,
.variable-row.used {
  border-color: var(--border-light, #cbd5e1);
}

.variable-row.custom {
  border-style: dashed;
}

.variable-name {
  overflow: hidden;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.variable-code {
  color: var(--text-tertiary, #64748b);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
}

.variable-empty {
  padding: 24px 0;
  color: var(--text-tertiary, #94a3b8);
  font-size: 12px;
  text-align: center;
}

.template-editor-panel {
  display: grid;
  gap: 10px;
  min-width: 0;
}

.template-field-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
  color: var(--text-primary, #0f172a);
  font-size: 13px;
  font-weight: 600;
}

.template-field-head span:last-child {
  color: var(--text-tertiary, #94a3b8);
  font-size: 12px;
  font-weight: 400;
}

.style-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 8px;
}

.style-chip {
  height: 24px;
  padding: 0 8px;
  border: 1px solid var(--border-light, #e2e8f0);
  border-radius: 4px;
  background: #fff;
  color: var(--text-secondary, #475569);
  cursor: pointer;
  font-size: 12px;
}

.style-chip:hover {
  border-color: var(--primary-color, #2080f0);
  color: var(--primary-color, #2080f0);
}

.template-preview {
  padding: 12px;
  border: 1px solid var(--border-light, #e2e8f0);
  border-radius: 4px;
  background: #fff;
}

.template-preview[data-mode='card'] {
  max-width: 420px;
}

.template-preview-head {
  margin-bottom: 8px;
  color: var(--text-tertiary, #64748b);
  font-size: 12px;
}

.template-preview-title {
  margin-bottom: 8px;
  color: var(--text-primary, #0f172a);
  font-size: 14px;
  font-weight: 600;
  line-height: 20px;
}

.template-preview-content {
  min-height: 48px;
  color: #4b5563;
  font-size: 13px;
  line-height: 20px;
  word-break: break-word;
}

.template-preview-content :deep(div) {
  margin: 2px 0;
}

.template-preview-content :deep(.gray) {
  color: #9ca3af;
}

.template-preview-content :deep(.normal) {
  color: #4b5563;
}

.template-preview-content :deep(.highlight) {
  color: #2563eb;
}

.template-preview-content :deep(a) {
  color: #2563eb;
  text-decoration: none;
}

.usage-panel {
  display: flex;
  flex-direction: column;
  gap: 18px;
  max-width: 760px;
  padding: 12px 4px 24px;
}

.usage-panel h3 {
  margin: 0 0 8px;
  color: var(--text-primary, #0f172a);
  font-size: 13px;
  font-weight: 600;
}

.usage-panel ol,
.usage-panel ul {
  margin: 0 0 10px;
  padding-left: 18px;
  color: var(--text-secondary, #475569);
  font-size: 13px;
  line-height: 22px;
}

.style-legend {
  display: grid;
  gap: 6px;
  list-style: none;
  padding-left: 0;
}

.style-legend li {
  display: flex;
  gap: 8px;
  align-items: center;
  font-size: 12px;
  color: var(--text-secondary, #475569);
}

.legend {
  display: inline-flex;
  min-width: 64px;
  justify-content: center;
  padding: 0 6px;
  border-radius: 3px;
  font-size: 12px;
  line-height: 20px;
}

.legend.gray {
  background: #f3f4f6;
  color: #9ca3af;
}

.legend.normal {
  background: #f8fafc;
  color: #4b5563;
}

.legend.highlight {
  background: #eff6ff;
  color: #2563eb;
}

.code-block {
  overflow: hidden;
  margin-top: 8px;
  border: 1px solid var(--border-light, #e2e8f0);
  border-radius: 4px;
  background: var(--bg-secondary, #f8fafc);
}

.code-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 8px;
  border-bottom: 1px solid var(--border-light, #eef2f6);
  color: var(--text-tertiary, #64748b);
  font-size: 12px;
}

.code-block pre {
  margin: 0;
  padding: 10px 12px;
  overflow: auto;
  color: var(--text-secondary, #334155);
  font-size: 12px;
  line-height: 18px;
  white-space: pre-wrap;
}

.tpl-empty {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 6px;
  justify-content: center;
  padding: 24px;
  color: var(--text-tertiary, #64748b);
  font-size: 13px;
  line-height: 20px;
}

@media (max-width: 960px) {
  .template-designer,
  .field-row {
    grid-template-columns: 1fr;
  }
}
</style>
