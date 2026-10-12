<template>
  <n-drawer
    :show="show"
    :width="drawerWidth"
    placement="right"
    @update:show="emit('update:show', $event)"
  >
    <n-drawer-content closable body-content-style="padding: 0;" title="应用 AI 助理">
      <div class="portal-assistant">
        <header class="assistant-scope">
          <span class="assistant-scope__icon"><n-icon><SparklesOutline /></n-icon></span>
          <div>
            <strong>{{ application?.applicationName || '当前应用' }}</strong>
            <span>仅限已发布页面 · {{ pageTitle || pageId }}</span>
          </div>
          <n-tag size="small" :bordered="false" type="success">
            权限内
          </n-tag>
        </header>

        <n-scrollbar ref="scrollbarRef" class="assistant-messages">
          <div class="assistant-message-list">
            <article class="assistant-message is-assistant">
              <span class="assistant-avatar"><n-icon><SparklesOutline /></n-icon></span>
              <p>你好，我会在当前页面和已发布配置的范围内回答。涉及真实数据查询或提交时，我会明确说明能力边界。</p>
            </article>
            <article
              v-for="messageItem in messages"
              :key="messageItem.id"
              class="assistant-message"
              :class="`is-${messageItem.role}`"
            >
              <span v-if="messageItem.role === 'assistant'" class="assistant-avatar">
                <n-icon><SparklesOutline /></n-icon>
              </span>
              <p>{{ messageItem.content }}</p>
            </article>
            <article v-if="sending" class="assistant-message is-assistant is-loading">
              <span class="assistant-avatar"><n-icon><SparklesOutline /></n-icon></span>
              <p><span /><span /><span /></p>
            </article>
          </div>
        </n-scrollbar>

        <footer class="assistant-composer">
          <div class="assistant-composer__tools">
            <n-select
              v-model:value="capability"
              size="small"
              :options="capabilityOptions"
              :consistent-menu-width="false"
            />
            <span>不会访问未授权页面</span>
          </div>
          <n-input
            v-model:value="question"
            type="textarea"
            :autosize="{ minRows: 3, maxRows: 7 }"
            maxlength="4000"
            placeholder="询问当前页面的字段含义、填写方式或分析思路…"
            @keydown.enter.exact.prevent="send"
          />
          <div class="assistant-composer__actions">
            <small>Enter 发送 · Shift + Enter 换行</small>
            <n-button type="primary" :loading="sending" :disabled="!question.trim()" @click="send">
              <template #icon>
                <n-icon><SendOutline /></n-icon>
              </template>
              发送
            </n-button>
          </div>
        </footer>
      </div>
    </n-drawer-content>
  </n-drawer>
</template>

<script setup>
import { SendOutline, SparklesOutline } from '@vicons/ionicons5'
import { useMessage } from 'naive-ui'
import { computed, nextTick, ref, watch } from 'vue'
import { chatBusinessApplicationAssistant } from '@/api/business-application'
import { parseJsonObject } from './portal-config'

const props = defineProps({
  show: { type: Boolean, default: false },
  identifier: { type: String, required: true },
  application: { type: Object, default: null },
  pageId: { type: String, default: '' },
  pageTitle: { type: String, default: '' },
})
const emit = defineEmits(['update:show'])
const message = useMessage()
const question = ref('')
const capability = ref('query')
const sending = ref(false)
const messages = ref([])
const scrollbarRef = ref(null)
let messageSequence = 0

const drawerWidth = computed(() => typeof window !== 'undefined' && window.innerWidth < 640 ? '100vw' : 430)
const assistantConfig = computed(() => parseJsonObject(props.application?.aiAssistantConfig))
const capabilityOptions = computed(() => (assistantConfig.value.capabilities || [])
  .filter(value => ['query', 'form', 'analysis'].includes(value))
  .map(value => ({
    label: { query: '页面问答', form: '填写指导', analysis: '分析建议' }[value],
    value,
  })))

watch(capabilityOptions, (options) => {
  if (!options.some(option => option.value === capability.value))
    capability.value = options[0]?.value || 'query'
}, { immediate: true })

async function send() {
  const content = question.value.trim()
  if (!content || sending.value)
    return
  messages.value.push({ id: nextMessageId(), role: 'user', content })
  question.value = ''
  sending.value = true
  await scrollToBottom()
  try {
    const response = await chatBusinessApplicationAssistant(props.identifier, {
      pageId: props.pageId,
      capability: capability.value,
      message: content,
    })
    messages.value.push({
      id: nextMessageId(),
      role: 'assistant',
      content: response.data?.content || '助理没有返回有效内容，请稍后重试。',
    })
  }
  catch (error) {
    message.error(error?.message || '应用 AI 助理暂时不可用')
    messages.value.push({
      id: nextMessageId(),
      role: 'assistant',
      content: '这次调用没有成功。请确认应用已经重新发布，并且当前页面在助理授权范围内。',
    })
  }
  finally {
    sending.value = false
    await scrollToBottom()
  }
}

function nextMessageId() {
  messageSequence += 1
  return `${Date.now()}-${messageSequence}`
}

async function scrollToBottom() {
  await nextTick()
  scrollbarRef.value?.scrollTo({ top: 1_000_000, behavior: 'smooth' })
}
</script>

<style scoped>
.portal-assistant {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  height: calc(100vh - 55px);
  background: var(--bg-secondary);
}

.assistant-scope {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  margin: 14px;
  padding: 12px;
  border: 1px solid #dce3ec;
  border-radius: 9px;
  background: var(--bg-primary);
}

.assistant-scope__icon,
.assistant-avatar {
  display: grid;
  place-items: center;
  border-radius: 8px;
  color: #fff;
  background: var(--portal-primary, #3370ff);
}

.assistant-scope__icon {
  width: 34px;
  height: 34px;
}

.assistant-scope > div {
  display: grid;
  min-width: 0;
  gap: 2px;
}

.assistant-scope strong {
  overflow: hidden;
  color: var(--text-primary);
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.assistant-scope span {
  color: #7a8290;
  font-size: 10px;
}

.assistant-messages {
  min-height: 0;
}

.assistant-message-list {
  display: grid;
  gap: 14px;
  padding: 8px 14px 24px;
}

.assistant-message {
  display: flex;
  max-width: 92%;
  align-items: flex-start;
  gap: 8px;
}

.assistant-message p {
  margin: 0;
  padding: 10px 12px;
  border: 1px solid #dce3ec;
  border-radius: 4px 12px 12px;
  color: #313844;
  background: var(--bg-primary);
  font-size: 12px;
  line-height: 1.7;
  white-space: pre-wrap;
}

.assistant-message.is-user {
  justify-self: end;
}

.assistant-message.is-user p {
  border-color: color-mix(in srgb, var(--portal-primary, #3370ff) 32%, transparent);
  border-radius: 12px 4px 12px 12px;
  color: #fff;
  background: var(--portal-primary, #3370ff);
}

.assistant-avatar {
  width: 26px;
  height: 26px;
  flex: 0 0 26px;
  font-size: 13px;
}

.assistant-message.is-loading p {
  display: flex;
  align-items: center;
  gap: 4px;
  min-height: 36px;
}

.assistant-message.is-loading p span {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #9aa3b2;
  animation: assistant-thinking 0.9s infinite alternate;
}

.assistant-message.is-loading p span:nth-child(2) {
  animation-delay: 0.18s;
}

.assistant-message.is-loading p span:nth-child(3) {
  animation-delay: 0.36s;
}

.assistant-composer {
  display: grid;
  gap: 9px;
  padding: 12px 14px 14px;
  border-top: 1px solid #dce3ec;
  background: var(--bg-primary);
  box-shadow: 0 -10px 30px rgb(15 23 42 / 5%);
}

.assistant-composer__tools,
.assistant-composer__actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.assistant-composer__tools :deep(.n-select) {
  width: 116px;
}

.assistant-composer__tools span,
.assistant-composer__actions small {
  color: #8a93a2;
  font-size: 10px;
}

@keyframes assistant-thinking {
  to {
    opacity: 0.25;
    transform: translateY(-2px);
  }
}
</style>
