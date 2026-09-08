<template>
  <div class="biz-type-page">
    <MasterDetailWorkspace :aside-width="260">
      <template #aside>
        <div class="biz-aside">
          <div class="biz-aside-head">
            <div>
              <strong>通知业务</strong>
              <p>一类业务通知配一次，发消息时直接选用</p>
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
          <n-spin :show="listLoading" class="biz-aside-list-spin">
            <n-scrollbar class="biz-aside-scroll">
              <button
                v-for="item in filteredList"
                :key="item.id"
                type="button"
                class="biz-item"
                :class="{ active: String(item.id) === String(form.id) }"
                @click="selectItem(item)"
              >
                <span class="biz-item-name">{{ item.bizName || '未命名' }}</span>
                <span class="biz-item-meta">
                  {{ item.bizType || '-' }}
                  <em :class="item.enabled === 1 ? 'is-on' : 'is-off'">{{ item.enabled === 1 ? '启用' : '停用' }}</em>
                </span>
              </button>
              <n-empty v-if="!listLoading && !filteredList.length" size="small" description="还没有通知业务，先新增一个" />
            </n-scrollbar>
          </n-spin>
        </div>
      </template>

      <div class="biz-main">
        <template v-if="editing">
          <div class="biz-main-head">
            <div>
              <strong>{{ form.id ? (form.bizName || '未命名') : '新增通知业务' }}</strong>
              <p>{{ form.id ? '改完保存即生效。发消息时带上编码和业务编号，同事点消息就会按这里跳转。' : '先起个业务人员能看懂的名字，再给开发一个稳定编码。' }}</p>
            </div>
            <div class="biz-main-actions">
              <n-switch
                :value="form.enabled === 1"
                @update:value="checked => form.enabled = checked ? 1 : 0"
              >
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

          <n-tabs v-model:value="activeTab" type="line" size="small" class="biz-tabs">
            <n-tab-pane name="config" tab="怎么配" display-directive="show">
              <n-scrollbar class="biz-pane-scroll">
                <n-form
                  ref="formRef"
                  :model="form"
                  :rules="formRules"
                  label-placement="left"
                  label-width="92"
                  class="biz-form"
                >
                  <n-form-item label="业务名称" path="bizName">
                    <n-input v-model:value="form.bizName" maxlength="100" show-count placeholder="例如：采购审批、合同到期" />
                    <p class="field-hint">
                      给人看的名字，会出现在发消息和下拉选择里。
                    </p>
                  </n-form-item>
                  <n-form-item label="业务编码" path="bizType">
                    <n-input
                      :value="form.bizType"
                      :disabled="Boolean(form.id)"
                      maxlength="50"
                      placeholder="例如：PURCHASE_ORDER"
                      @update:value="onBizTypeInput"
                    />
                    <p class="field-hint">
                      发给系统和开发用的代号。保存后不要改，代码会按这个值发送。
                    </p>
                  </n-form-item>
                  <n-form-item label="点开去哪">
                    <n-radio-group :value="jumpPreset" name="jumpPreset" @update:value="onJumpPresetChange">
                      <n-radio-button
                        v-for="preset in JUMP_PRESETS"
                        :key="preset.key"
                        :value="preset.key"
                      >
                        {{ preset.label }}
                      </n-radio-button>
                    </n-radio-group>
                    <p class="field-hint">
                      {{ currentPresetHint }}
                    </p>
                  </n-form-item>
                  <n-form-item v-if="jumpPreset === 'custom'" label="页面路径" path="jumpUrl">
                    <n-input
                      v-model:value="form.jumpUrl"
                      :placeholder="customPathPlaceholder"
                    />
                    <p class="field-hint">
                      只填本系统路径。{{ bizKeyToken }} 会在发送时换成单据编号，{{ messageIdToken }} 一般不用。
                    </p>
                  </n-form-item>
                  <n-form-item v-if="jumpPreset !== 'none'" label="打开方式">
                    <n-select
                      v-model:value="form.jumpTarget"
                      :options="jumpTargetOptions"
                      placeholder="当前页"
                    />
                  </n-form-item>
                  <n-form-item label="试一下">
                    <div class="preview-row">
                      <n-input
                        v-model:value="sampleBizKey"
                        size="small"
                        placeholder="示例业务编号"
                        style="width: 180px"
                      />
                      <span class="preview-copy">{{ jumpPreviewText }}</span>
                    </div>
                  </n-form-item>
                  <n-form-item label="图标">
                    <IconSelector v-model="form.icon" />
                    <p class="field-hint">
                      可选。消息列表里用来辨认这一类通知。
                    </p>
                  </n-form-item>
                  <n-form-item label="说明">
                    <n-input
                      v-model:value="form.remark"
                      type="textarea"
                      :rows="2"
                      maxlength="200"
                      show-count
                      placeholder="写给后来的同事：这类通知什么时候发、编号填什么"
                    />
                  </n-form-item>
                </n-form>
              </n-scrollbar>
            </n-tab-pane>

            <n-tab-pane name="usage" tab="怎么用" display-directive="show">
              <n-scrollbar class="biz-pane-scroll">
                <div class="usage-panel">
                  <n-alert v-if="isBuiltInFlowTodo(form.bizType)" type="info" :show-icon="false">
                    流程待办是系统内置能力。同事点消息会进入待办页，这里的跳转配置不会覆盖它。
                  </n-alert>

                  <section>
                    <h3>配好之后怎么用</h3>
                    <ol>
                      <li>到「消息管理」点发送，业务选 <strong>{{ form.bizName || '这个名称' }}</strong>。</li>
                      <li>业务编号填那张单或那条待办的编号，例如 <code>{{ sampleBizKey }}</code>。</li>
                      <li>同事在右上角消息或消息中心点「查看详情」，就会{{ jumpActionText }}。</li>
                      <li>事情办完后，开发可按同一编码和编号把站内信标为已读，避免重复打扰。</li>
                    </ol>
                    <NButton size="small" secondary @click="goTrySend">
                      去消息管理试发一条
                    </NButton>
                  </section>

                  <section>
                    <h3>适合哪些事</h3>
                    <div class="scenario-list">
                      <article
                        v-for="item in USAGE_SCENARIOS"
                        :key="item.key"
                        class="scenario"
                        :class="{ current: item.preset === jumpPreset }"
                      >
                        <strong>{{ item.title }}</strong>
                        <p>{{ item.desc }}</p>
                        <span>常见：{{ item.example }}</span>
                      </article>
                    </div>
                  </section>

                  <section>
                    <h3>给开发同事</h3>
                    <p class="usage-lead">
                      发通知时带上编码 <code>{{ form.bizType || '业务编码' }}</code> 和业务编号。点开路径由本页配置，不用写死在代码里。
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

        <div v-else class="biz-empty">
          <p>从左侧选一类通知，或新增一个。</p>
          <p>配的是「这类消息点开去哪」，不是改消息正文。正文仍在消息模板或发送时填写。</p>
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
import IconSelector from '@/components/IconSelector.vue'
import { useDict } from '@/composables/useDict'
import { copy } from '@/utils/clipboard'
import {
  buildHttpSendExample,
  buildJavaSendExample,
  DEFAULT_SAMPLE_BIZ_KEY,
  describeJumpResult,
  isBuiltInFlowTodo,
  JUMP_PRESETS,
  normalizeBizTypeCode,
  resolveJumpPreset,
  USAGE_SCENARIOS,
} from './biz-type-usage'

defineOptions({ name: 'MessageBizType' })

const router = useRouter()
const { dict } = useDict('sys_link_open_target')
const list = ref([])
const listLoading = ref(false)
const saving = ref(false)
const keyword = ref('')
const editing = ref(false)
const activeTab = ref('config')
const sampleBizKey = ref(DEFAULT_SAMPLE_BIZ_KEY)
const formRef = ref(null)
const form = reactive(createEmptyForm())
const bizKeyToken = '$' + '{bizKey}'
const messageIdToken = '$' + '{messageId}'
const customPathPlaceholder = `/leave/list?id=${bizKeyToken}`

const filteredList = computed(() => {
  const query = keyword.value.trim().toLowerCase()
  if (!query)
    return list.value
  return list.value.filter(item => `${item.bizName || ''} ${item.bizType || ''}`.toLowerCase().includes(query))
})
const jumpTargetOptions = computed(() => dict.value.sys_link_open_target || [])
const jumpPreset = computed(() => resolveJumpPreset(form.jumpUrl))
const currentPresetHint = computed(() => JUMP_PRESETS.find(item => item.key === jumpPreset.value)?.hint || '')
const jumpPreviewText = computed(() => describeJumpResult(form.jumpUrl, form.jumpTarget, sampleBizKey.value))
const jumpActionText = computed(() => {
  if (!form.jumpUrl)
    return '只看到消息内容，不跳转'
  return form.jumpTarget === '_blank' ? '在新窗口打开对应页面' : '打开对应页面'
})
const javaExample = computed(() => buildJavaSendExample({
  bizType: form.bizType,
  bizName: form.bizName,
  sampleKey: sampleBizKey.value,
}))
const httpExample = computed(() => buildHttpSendExample({
  bizType: form.bizType,
  bizName: form.bizName,
  sampleKey: sampleBizKey.value,
}))

const formRules = {
  bizName: { required: true, message: '请填写业务名称', trigger: 'blur' },
  bizType: { required: true, message: '请填写业务编码', trigger: 'blur' },
}

function createEmptyForm() {
  return {
    id: null,
    bizName: '',
    bizType: '',
    jumpUrl: JUMP_PRESETS.find(item => item.key === 'todo')?.url || '',
    jumpTarget: '_self',
    icon: '',
    sort: 0,
    enabled: 1,
    remark: '',
  }
}

function applyForm(source = {}) {
  Object.assign(form, createEmptyForm(), source)
}

function onBizTypeInput(value) {
  form.bizType = normalizeBizTypeCode(value)
}

function onJumpPresetChange(key) {
  const preset = JUMP_PRESETS.find(item => item.key === key)
  if (!preset)
    return
  if (key === 'custom') {
    if (!form.jumpUrl || JUMP_PRESETS.some(item => item.url && item.url === form.jumpUrl))
      form.jumpUrl = `/business/detail?id=${'$'}{bizKey}`
    return
  }
  form.jumpUrl = preset.url
}

async function loadList(keepSelection = true) {
  listLoading.value = true
  try {
    const res = await messageApi.getBizTypePage({
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
    window.$message?.error(error?.message || '加载通知业务失败')
  }
  finally {
    listLoading.value = false
  }
}

function startCreate() {
  editing.value = true
  activeTab.value = 'config'
  applyForm()
}

function selectItem(item) {
  editing.value = true
  activeTab.value = 'config'
  applyForm(item)
}

async function saveItem() {
  try {
    await formRef.value?.validate()
  }
  catch {
    activeTab.value = 'config'
    return
  }
  saving.value = true
  try {
    const payload = {
      id: form.id,
      bizName: form.bizName.trim(),
      bizType: normalizeBizTypeCode(form.bizType),
      jumpUrl: form.jumpUrl?.trim() || '',
      jumpTarget: form.jumpTarget || '_self',
      icon: form.icon || '',
      sort: Number(form.sort || 0),
      enabled: form.enabled === 1 ? 1 : 0,
      remark: form.remark?.trim() || '',
    }
    if (payload.id)
      await messageApi.updateBizType(payload)
    else
      await messageApi.createBizType(payload)
    window.$message?.success('已保存。发消息时选这个业务，并填上单据编号即可。')
    await loadList(false)
    const saved = list.value.find(item => item.bizType === payload.bizType) || list.value.find(item => String(item.id) === String(payload.id))
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
    title: '删除这个通知业务？',
    content: '已经发出的消息还在，但之后点开将无法再按这个配置跳转。',
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
    await messageApi.deleteBizType(form.id)
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
.biz-type-page {
  height: 100%;
  min-height: 0;
}

.biz-aside,
.biz-main {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}

.biz-aside {
  padding: 10px;
  gap: 8px;
}

.biz-aside-head,
.biz-main-head {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  justify-content: space-between;
}

.biz-aside-head strong,
.biz-main-head strong {
  display: block;
  color: var(--text-primary, #0f172a);
  font-size: 14px;
  line-height: 22px;
}

.biz-aside-head p,
.biz-main-head p {
  margin: 2px 0 0;
  color: var(--text-tertiary, #94a3b8);
  font-size: 12px;
  line-height: 18px;
}

.biz-aside-list-spin,
.biz-aside-scroll,
.biz-tabs,
.biz-pane-scroll {
  flex: 1;
  min-height: 0;
}

.biz-item {
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

.biz-item:hover {
  background: var(--bg-secondary, #f8fafc);
}

.biz-item.active {
  border-color: var(--border-light, #e2e8f0);
  background: color-mix(in srgb, var(--primary-color, #2080f0) 8%, white);
}

.biz-item-name {
  color: var(--text-primary, #0f172a);
  font-size: 13px;
  line-height: 20px;
}

.biz-item-meta {
  display: flex;
  gap: 8px;
  align-items: center;
  color: var(--text-tertiary, #94a3b8);
  font-size: 11px;
  line-height: 16px;
}

.biz-item-meta em {
  font-style: normal;
}

.biz-item-meta em.is-on {
  color: var(--success-color, #18a058);
}

.biz-item-meta em.is-off {
  color: var(--text-tertiary, #94a3b8);
}

.biz-main {
  min-width: 0;
}

.biz-main-head {
  padding: 10px 16px;
  border-bottom: 1px solid var(--border-light, #eef2f6);
}

.biz-main-actions {
  display: flex;
  flex-shrink: 0;
  gap: 8px;
  align-items: center;
}

.biz-tabs {
  padding: 0 16px 12px;
}

.biz-tabs :deep(.n-tabs-pane-wrapper),
.biz-tabs :deep(.n-tab-pane) {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
}

.biz-form {
  max-width: 720px;
  padding: 12px 4px 24px;
}

.biz-form :deep(.n-form-item-blank) {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  width: 100%;
}

.biz-form :deep(.n-radio-group) {
  display: flex;
  flex-wrap: wrap;
  gap: 0;
}

.field-hint,
.usage-lead {
  margin: 6px 0 0;
  color: var(--text-tertiary, #64748b);
  font-size: 12px;
  line-height: 18px;
}

.preview-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  width: 100%;
}

.preview-copy {
  flex: 1;
  min-width: 220px;
  color: var(--text-secondary, #475569);
  font-size: 12px;
  line-height: 18px;
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

.usage-panel ol {
  margin: 0 0 10px;
  padding-left: 18px;
  color: var(--text-secondary, #475569);
  font-size: 13px;
  line-height: 22px;
}

.scenario-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.scenario {
  padding: 10px 12px;
  border: 1px solid var(--border-light, #e2e8f0);
  border-radius: 4px;
}

.scenario.current {
  border-color: var(--primary-color, #2080f0);
}

.scenario strong {
  display: block;
  font-size: 13px;
}

.scenario p,
.scenario span {
  margin: 4px 0 0;
  color: var(--text-tertiary, #64748b);
  font-size: 12px;
  line-height: 18px;
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

.biz-empty {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 6px;
  align-items: flex-start;
  justify-content: center;
  padding: 24px;
  color: var(--text-tertiary, #64748b);
  font-size: 13px;
  line-height: 20px;
}

@media (max-width: 960px) {
  .scenario-list {
    grid-template-columns: 1fr;
  }
}
</style>
