<template>
  <div class="trace-steps">
    <n-empty v-if="!steps.length" size="small" description="暂无执行步骤" />
    <article
      v-for="(step, index) in steps"
      :key="`${step.fieldCode || 'step'}-${index}`"
      class="trace-step"
      :class="{ failed: step.success === false }"
    >
      <header class="trace-step-head">
        <div>
          <span class="step-index">{{ index + 1 }}</span>
          <strong>{{ step.fieldCode || '未命名字段' }}</strong>
          <em>{{ step.formulaType || 'FORMULA' }}</em>
        </div>
        <div class="step-status">
          <span v-if="step.elapsedMs !== null && step.elapsedMs !== undefined">{{ step.elapsedMs }}ms</span>
          <n-tag size="small" :type="step.success === false ? 'error' : 'success'" :bordered="false">
            {{ step.success === false ? '失败' : '成功' }}
          </n-tag>
        </div>
      </header>

      <pre class="step-expression">{{ step.expression || '无表达式' }}</pre>

      <div class="step-io">
        <section>
          <span>输入</span>
          <pre>{{ formatValue(step.input) }}</pre>
        </section>
        <section>
          <span>输出</span>
          <pre>{{ formatValue(step.output) }}</pre>
        </section>
      </div>

      <div v-if="metadataEntries(step).length" class="step-metadata">
        <span
          v-for="item in metadataEntries(step)"
          :key="`${step.fieldCode}-${item.key}`"
        >
          {{ item.key }}: {{ formatInlineValue(item.value) }}
        </span>
      </div>

      <p v-if="step.errorMessage" class="step-error">
        {{ step.errorMessage }}
      </p>
    </article>

    <div v-if="normalizedErrors.length" class="trace-errors">
      <strong>错误</strong>
      <span v-for="error in normalizedErrors" :key="error">{{ error }}</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  steps: {
    type: Array,
    default: () => [],
  },
  errors: {
    type: [Array, Object],
    default: () => [],
  },
})

const normalizedErrors = computed(() => {
  if (Array.isArray(props.errors))
    return props.errors.map(item => String(item || '')).filter(Boolean)
  if (!props.errors || typeof props.errors !== 'object')
    return []
  return Object.entries(props.errors)
    .flatMap(([fieldCode, errors]) => (Array.isArray(errors) ? errors : [errors])
      .map(item => `${fieldCode}: ${item}`))
    .filter(Boolean)
})

function metadataEntries(step = {}) {
  return Object.entries(step.metadata || {}).map(([key, value]) => ({ key, value }))
}

function formatValue(value) {
  if (value === null || value === undefined || value === '')
    return '-'
  if (typeof value === 'string')
    return value
  try {
    return JSON.stringify(value, null, 2)
  }
  catch {
    return String(value)
  }
}

function formatInlineValue(value) {
  if (value === null || value === undefined || value === '')
    return '-'
  if (typeof value === 'object')
    return JSON.stringify(value)
  return String(value)
}
</script>

<style scoped>
.trace-steps {
  display: grid;
  gap: 10px;
}

.trace-step {
  display: grid;
  gap: 10px;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  background: var(--bg-primary);
  padding: 12px;
}

.trace-step.failed {
  border-color: #fecaca;
  background: #fff7f7;
}

.trace-step-head,
.trace-step-head > div,
.step-status {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.trace-step-head {
  justify-content: space-between;
}

.trace-step-head strong {
  color: var(--text-primary);
  font-size: 13px;
}

.trace-step-head em,
.step-status span {
  color: var(--text-secondary);
  font-size: 12px;
  font-style: normal;
}

.step-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 6px;
  background: #e8eef7;
  color: var(--text-primary);
  font-size: 12px;
  font-weight: 700;
}

.step-expression,
.step-io pre {
  overflow: auto;
  margin: 0;
  border-radius: 6px;
  background: var(--bg-secondary);
  color: var(--text-primary);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  line-height: 1.6;
  padding: 8px;
}

.step-io {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.step-io section {
  min-width: 0;
}

.step-io span {
  display: block;
  margin-bottom: 5px;
  color: var(--text-secondary);
  font-size: 12px;
}

.step-metadata {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.step-metadata span {
  border-radius: 999px;
  background: #eef6ff;
  color: #2563eb;
  font-size: 12px;
  padding: 3px 8px;
}

.step-error {
  margin: 0;
  color: #b91c1c;
  font-size: 12px;
  line-height: 1.6;
}

.trace-errors {
  display: grid;
  gap: 4px;
  border: 1px solid #fecaca;
  border-radius: 8px;
  background: #fef2f2;
  color: #991b1b;
  padding: 10px 12px;
}

.trace-errors strong,
.trace-errors span {
  font-size: 12px;
}

@media (max-width: 720px) {
  .trace-step-head,
  .step-io {
    grid-template-columns: minmax(0, 1fr);
  }

  .trace-step-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .step-io {
    display: grid;
  }
}
</style>
