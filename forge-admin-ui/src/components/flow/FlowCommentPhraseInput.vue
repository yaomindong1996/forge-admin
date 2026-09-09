<template>
  <div class="flow-comment-phrase-input" :data-scene="scene || 'ALL'">
    <n-input
      ref="inputRef"
      :value="modelValue"
      type="textarea"
      :size="size"
      :rows="rows"
      :maxlength="maxlength"
      :show-count="showCount"
      :disabled="disabled"
      :placeholder="placeholder"
      @update:value="onInput"
      @keydown.ctrl.enter.prevent="$emit('submit')"
      @keydown.meta.enter.prevent="$emit('submit')"
    />
    <div class="phrase-toolbar">
      <div
        v-if="visiblePhrases.length"
        class="phrase-chips"
        role="group"
        :aria-label="scene === 'REJECT' ? '常用驳回原因' : '常用审批意见'"
      >
        <button
          v-for="phrase in visiblePhrases"
          :key="phrase.id"
          type="button"
          class="phrase-chip"
          :class="{ active: modelValue === phrase.content }"
          :disabled="disabled"
          :title="phrase.content"
          @click="applyPhrase(phrase.content)"
        >
          {{ phrase.content }}
        </button>
        <button
          v-if="hiddenCount > 0"
          type="button"
          class="phrase-chip more"
          :disabled="disabled"
          @click="openManage"
        >
          更多 {{ hiddenCount }}
        </button>
      </div>
      <div class="phrase-actions">
        <button
          v-if="canSaveCurrent"
          type="button"
          class="phrase-link"
          :disabled="disabled || saving"
          @click="saveCurrent"
        >
          {{ saving ? '保存中' : '存为常用' }}
        </button>
        <button
          v-if="showManage"
          type="button"
          class="phrase-link"
          :disabled="disabled"
          @click="openManage"
        >
          管理
        </button>
      </div>
    </div>

    <n-modal
      v-model:show="manageVisible"
      preset="card"
      title="常用审批意见"
      style="width: 520px"
      :mask-closable="true"
    >
      <div class="phrase-manage">
        <div class="phrase-manage-add">
          <n-input
            v-model:value="draftContent"
            type="textarea"
            size="small"
            :rows="2"
            :maxlength="maxlength"
            show-count
            placeholder="输入后保存为个人常用意见"
          />
          <div class="phrase-manage-add-row">
            <n-select
              v-model:value="draftScene"
              size="small"
              :options="sceneOptions"
              :consistent-menu-width="false"
              style="width: 120px"
            />
            <NButton size="small" type="primary" :loading="saving" :disabled="!draftContent.trim()" @click="saveDraft">
              保存
            </NButton>
          </div>
        </div>
        <p v-if="!personalPhrases.length" class="phrase-empty">
          还没有个人常用意见，保存后可在审批时点选。
        </p>
        <ul v-else class="phrase-list">
          <li v-for="phrase in personalPhrases" :key="phrase.id" class="phrase-list-item">
            <button type="button" class="phrase-list-content" @click="applyPhrase(phrase.content); manageVisible = false">
              <span>{{ phrase.content }}</span>
              <em>{{ sceneLabel(phrase.scene) }}</em>
            </button>
            <button type="button" class="phrase-list-delete" @click="removePhrase(phrase)">
              删除
            </button>
          </li>
        </ul>
      </div>
    </n-modal>
  </div>
</template>

<script setup>
import { NButton } from 'naive-ui'
import { computed, onMounted, ref, watch } from 'vue'
import flowApi from '@/api/flow'
import { useDict } from '@/composables/useDict'

const props = defineProps({
  modelValue: { type: String, default: '' },
  scene: { type: String, default: '' },
  placeholder: { type: String, default: '请输入审批意见' },
  maxlength: { type: Number, default: 200 },
  rows: { type: Number, default: 3 },
  size: { type: String, default: 'small' },
  disabled: { type: Boolean, default: false },
  showCount: { type: Boolean, default: true },
  showManage: { type: Boolean, default: true },
  maxVisible: { type: Number, default: 12 },
})

const emit = defineEmits(['update:modelValue', 'submit'])

const { dict } = useDict('flow_comment_phrase_scene')
const inputRef = ref(null)
const usablePhrases = ref([])
const personalPhrases = ref([])
const manageVisible = ref(false)
const saving = ref(false)
const draftContent = ref('')
const draftScene = ref(props.scene || 'ALL')

const sceneOptions = computed(() => (dict.value.flow_comment_phrase_scene || []).map(item => ({
  label: item.label,
  value: item.value,
})))

const visiblePhrases = computed(() => usablePhrases.value.slice(0, props.maxVisible))
const hiddenCount = computed(() => Math.max(usablePhrases.value.length - visiblePhrases.value.length, 0))
const normalizedValue = computed(() => String(props.modelValue || '').trim())
const canSaveCurrent = computed(() => {
  if (!normalizedValue.value) {
    return false
  }
  return !usablePhrases.value.some(item => item.content === normalizedValue.value)
})

watch(() => props.scene, (scene) => {
  draftScene.value = scene || 'ALL'
  loadUsable()
})

onMounted(() => {
  loadUsable()
})

function onInput(value) {
  emit('update:modelValue', value)
}

function applyPhrase(content) {
  emit('update:modelValue', content)
}

function sceneLabel(scene) {
  const matched = sceneOptions.value.find(item => item.value === scene)
  return matched?.label || scene || '通用'
}

function unwrapList(response) {
  if (Array.isArray(response?.data)) {
    return response.data
  }
  if (Array.isArray(response)) {
    return response
  }
  return []
}

async function loadUsable() {
  try {
    const response = await flowApi.listUsableCommentPhrases(props.scene ? { scene: props.scene } : undefined)
    usablePhrases.value = unwrapList(response).filter(item => item?.id && item?.content)
  }
  catch {
    usablePhrases.value = []
  }
}

async function loadMine() {
  try {
    const response = await flowApi.listMyCommentPhrases()
    personalPhrases.value = unwrapList(response).filter(item => item?.id && item?.content)
  }
  catch (error) {
    personalPhrases.value = []
    window.$message?.error(error?.message || '加载常用意见失败')
  }
}

async function openManage() {
  draftContent.value = normalizedValue.value
  draftScene.value = props.scene || 'ALL'
  manageVisible.value = true
  await loadMine()
}

async function savePhrase(content, scene) {
  const payload = {
    content: String(content || '').trim(),
    scene: scene || 'ALL',
    ownerType: 1,
  }
  if (!payload.content) {
    window.$message?.warning('请输入审批意见')
    return false
  }
  saving.value = true
  try {
    await flowApi.createCommentPhrase(payload)
    window.$message?.success('已保存为常用意见')
    await loadUsable()
    if (manageVisible.value) {
      await loadMine()
    }
    return true
  }
  catch (error) {
    window.$message?.error(error?.message || '保存常用意见失败')
    return false
  }
  finally {
    saving.value = false
  }
}

async function saveCurrent() {
  await savePhrase(normalizedValue.value, props.scene || 'ALL')
}

async function saveDraft() {
  const ok = await savePhrase(draftContent.value, draftScene.value)
  if (ok) {
    draftContent.value = ''
  }
}

async function removePhrase(phrase) {
  try {
    await flowApi.deleteCommentPhrase(phrase.id)
    window.$message?.success('已删除')
    await Promise.all([loadUsable(), loadMine()])
  }
  catch (error) {
    window.$message?.error(error?.message || '删除常用意见失败')
  }
}

function focus() {
  inputRef.value?.focus?.()
}

defineExpose({ focus })
</script>

<style scoped>
.flow-comment-phrase-input {
  width: 100%;
}

.phrase-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: flex-start;
  justify-content: space-between;
  margin-top: 8px;
}

.phrase-chips {
  display: flex;
  flex: 1;
  flex-wrap: wrap;
  gap: 6px;
  min-width: 0;
}

.phrase-chip {
  max-width: 160px;
  height: 22px;
  overflow: hidden;
  padding: 0 8px;
  border: 1px solid var(--border-light, #e2e8f0);
  border-radius: 4px;
  background: transparent;
  color: var(--text-secondary, #475569);
  cursor: pointer;
  font-size: 12px;
  line-height: 20px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.phrase-chip:hover:not(:disabled) {
  border-color: var(--border-dark, #cbd5e1);
  background: var(--bg-secondary, #f8fafc);
}

.phrase-chip.active {
  border-color: var(--primary-color, #2080f0);
  color: var(--primary-color, #2080f0);
}

.flow-comment-phrase-input[data-scene='REJECT'] .phrase-chip.active {
  border-color: var(--error-color, #d03050);
  color: var(--error-color, #d03050);
}

.phrase-chip.more {
  color: var(--text-tertiary, #94a3b8);
}

.phrase-chip:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.phrase-actions {
  display: inline-flex;
  flex-shrink: 0;
  gap: 8px;
  align-items: center;
}

.phrase-link {
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--primary-color, #2080f0);
  cursor: pointer;
  font-size: 12px;
  line-height: 22px;
}

.phrase-link:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.phrase-manage {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.phrase-manage-add {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.phrase-manage-add-row {
  display: flex;
  gap: 8px;
  align-items: center;
  justify-content: flex-end;
}

.phrase-empty {
  margin: 0;
  color: var(--text-tertiary, #94a3b8);
  font-size: 12px;
  line-height: 18px;
}

.phrase-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: 280px;
  margin: 0;
  padding: 0;
  overflow: auto;
  list-style: none;
}

.phrase-list-item {
  display: flex;
  gap: 8px;
  align-items: center;
  min-height: 32px;
  padding: 4px 0;
  border-bottom: 1px solid var(--border-light, #eef2f6);
}

.phrase-list-content {
  display: flex;
  flex: 1;
  min-width: 0;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--text-primary, #0f172a);
  cursor: pointer;
  text-align: left;
}

.phrase-list-content span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.phrase-list-content em {
  flex-shrink: 0;
  margin-left: 8px;
  color: var(--text-tertiary, #94a3b8);
  font-size: 12px;
  font-style: normal;
}

.phrase-list-delete {
  flex-shrink: 0;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--error-color, #d03050);
  cursor: pointer;
  font-size: 12px;
}
</style>
