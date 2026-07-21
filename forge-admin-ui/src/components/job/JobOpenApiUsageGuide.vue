<template>
  <section class="job-open-api-guide" :class="{ 'is-compact': compact }">
    <div class="guide-heading">
      <div>
        <strong>调用示例</strong>
        <p>使用服务账号 Token 调用 Forge Admin 开放接口。</p>
      </div>
      <NTag size="small" type="info" :bordered="false">
        Bearer Token
      </NTag>
    </div>

    <label class="base-url-field">
      <span>Admin 服务地址</span>
      <NInput
        v-model:value="baseUrl"
        size="small"
        placeholder="例如：http://localhost:8580"
        aria-label="Forge Admin 服务地址"
      />
    </label>

    <div class="token-instruction">
      <i class="i-material-symbols:info-outline-rounded" />
      <span v-if="usesTokenPlaceholder">
        先执行 <code>export TOKEN=&quot;创建或轮换时保存的明文 Token&quot;</code>，再复制下方命令。
      </span>
      <span v-else>
        下方命令已带入本次生成的 Token；关闭弹窗后系统不会再次展示。
      </span>
    </div>

    <NTabs
      v-if="examples.length"
      v-model:value="activeExample"
      type="line"
      size="small"
      :animated="false"
      class="example-tabs"
    >
      <NTabPane
        v-for="example in examples"
        :key="example.key"
        :name="example.key"
        :tab="example.label"
      >
        <div class="endpoint-summary">
          <NTag
            size="small"
            :type="example.method === 'POST' ? 'success' : 'info'"
            :bordered="false"
          >
            {{ example.method }}
          </NTag>
          <code>{{ example.path }}</code>
          <span>需要 {{ example.scope }}</span>
        </div>
        <p class="example-description">
          {{ example.description }}
        </p>
        <div class="command-block">
          <NButton
            size="tiny"
            secondary
            class="copy-command"
            :aria-label="`复制${example.label} cURL`"
            @click="handleCopy(example.command)"
          >
            <template #icon>
              <i class="i-material-symbols:content-copy-outline-rounded" />
            </template>
            复制 cURL
          </NButton>
          <pre><code>{{ example.command }}</code></pre>
        </div>
      </NTabPane>
    </NTabs>

    <NAlert v-else type="warning" :show-icon="true">
      当前服务账号没有可用于开放 API 的 Scope，请重新创建或轮换服务账号。
    </NAlert>

    <div v-if="!compact" class="guide-notes">
      <div><strong>401</strong><span>Token 缺失、无效、已吊销或已过期</span></div>
      <div><strong>403</strong><span>Scope 不足或任务不在授权资源内</span></div>
      <div><strong>429</strong><span>调用频率超过当前接口限制</span></div>
      <div><strong>503</strong><span>Redis 等安全依赖暂时不可用</span></div>
    </div>

    <p class="security-tip">
      生产环境请从 Secret 或环境变量读取 Token，不要写入前端代码、仓库或应用日志。
    </p>
  </section>
</template>

<script setup>
import { NAlert, NButton, NInput, NTabPane, NTabs, NTag } from 'naive-ui'
import { computed, ref, watch } from 'vue'
import { copy } from '@/utils/clipboard'
import { buildJobOpenApiExamples } from './job-open-api-usage.js'

defineOptions({ name: 'JobOpenApiUsageGuide' })

const props = defineProps({
  token: {
    type: String,
    default: '$' + '{TOKEN}',
  },
  scopes: {
    type: Array,
    default: undefined,
  },
  compact: {
    type: Boolean,
    default: false,
  },
})

const baseUrl = ref('http://localhost:8580')
const activeExample = ref('')
const tokenPlaceholder = '$' + '{TOKEN}'
const usesTokenPlaceholder = computed(() => props.token === tokenPlaceholder)
const examples = computed(() => buildJobOpenApiExamples({
  baseUrl: baseUrl.value,
  token: props.token,
  scopes: props.scopes,
}))

watch(examples, (currentExamples) => {
  if (!currentExamples.some(example => example.key === activeExample.value))
    activeExample.value = currentExamples[0]?.key || ''
}, { immediate: true })

function handleCopy(command) {
  copy(command, 'cURL 示例已复制')
}
</script>

<style scoped>
.job-open-api-guide {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.guide-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.guide-heading strong {
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 650;
}

.guide-heading p,
.example-description,
.security-tip {
  margin: 4px 0 0;
  color: var(--text-tertiary);
  font-size: 12px;
  line-height: 1.6;
}

.base-url-field {
  display: grid;
  grid-template-columns: 112px minmax(0, 1fr);
  align-items: center;
  gap: 12px;
}

.base-url-field > span {
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 600;
}

.token-instruction {
  margin-top: -4px;
  display: flex;
  align-items: flex-start;
  gap: 7px;
  color: var(--text-tertiary);
  font-size: 12px;
  line-height: 1.6;
}

.token-instruction > i {
  margin-top: 3px;
  flex: 0 0 auto;
  color: var(--primary-color);
  font-size: 15px;
}

.token-instruction code {
  color: var(--text-secondary);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

.example-tabs {
  min-width: 0;
}

.endpoint-summary {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.endpoint-summary code {
  min-width: 0;
  overflow-wrap: anywhere;
  color: var(--text-primary);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
}

.endpoint-summary > span {
  margin-left: auto;
  color: var(--text-tertiary);
  font-size: 11px;
  white-space: nowrap;
}

.command-block {
  position: relative;
  margin-top: 10px;
  overflow: hidden;
  background: #111827;
  border: 1px solid rgb(148 163 184 / 24%);
  border-radius: 7px;
}

.command-block pre {
  margin: 0;
  padding: 42px 16px 16px;
  overflow-x: auto;
}

.command-block code {
  color: #dbeafe;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  line-height: 1.7;
  white-space: pre;
}

.copy-command {
  position: absolute;
  z-index: 1;
  top: 8px;
  right: 8px;
}

.guide-notes {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  overflow: hidden;
  background: var(--bg-secondary);
  border: 1px solid var(--border-light);
  border-radius: 7px;
}

.guide-notes > div {
  min-width: 0;
  padding: 10px 12px;
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr);
  align-items: baseline;
  gap: 8px;
}

.guide-notes > div:nth-child(odd) {
  border-right: 1px solid var(--border-light);
}

.guide-notes > div:nth-child(-n + 2) {
  border-bottom: 1px solid var(--border-light);
}

.guide-notes strong {
  color: var(--text-primary);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
}

.guide-notes span {
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.security-tip {
  margin-top: -2px;
}

.is-compact {
  gap: 12px;
}

@media (max-width: 640px) {
  .base-url-field {
    grid-template-columns: minmax(0, 1fr);
    gap: 6px;
  }

  .endpoint-summary {
    align-items: flex-start;
    flex-wrap: wrap;
  }

  .endpoint-summary > span {
    width: 100%;
    margin-left: 0;
  }

  .guide-notes {
    grid-template-columns: minmax(0, 1fr);
  }

  .guide-notes > div:nth-child(odd) {
    border-right: 0;
  }

  .guide-notes > div:not(:last-child) {
    border-bottom: 1px solid var(--border-light);
  }
}
</style>
