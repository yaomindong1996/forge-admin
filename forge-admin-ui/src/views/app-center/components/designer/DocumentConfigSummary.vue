<template>
  <div class="document-config-summary">
    <section>
      <h4>发布摘要</h4>
      <div class="summary-facts">
        <div>
          <span>状态字段</span>
          <strong>{{ form.statusField || '-' }}</strong>
        </div>
        <div>
          <span>编号规则</span>
          <strong>{{ form.noRuleTemplate || form.documentNoRule || '-' }}</strong>
        </div>
        <div>
          <span>状态映射</span>
          <strong>{{ statusCount }} 项</strong>
        </div>
      </div>
    </section>

    <section>
      <div class="flow-summary-head">
        <h4>主流程</h4>
        <n-button size="tiny" secondary @click="$emit('configureFlow')">
          去配置
        </n-button>
      </div>
      <div class="flow-summary">
        <n-tag :type="mainFlow.configured ? 'success' : 'warning'" :bordered="false">
          {{ mainFlow.configured ? '已配置' : '未配置' }}
        </n-tag>
        <strong>{{ mainFlow.flowModelName || mainFlow.flowModelKey || '-' }}</strong>
        <p v-if="mainFlow.gaps?.length">
          {{ mainFlow.gaps.join('、') }}
        </p>
      </div>
    </section>

    <section>
      <h4>保存检查</h4>
      <ul class="check-list">
        <li :class="{ ok: !!form.statusField }">
          <span />
          状态字段
        </li>
        <li :class="{ ok: !noRulePreview || noRulePreview.valid !== false }">
          <span />
          编号规则
        </li>
        <li :class="{ ok: statusCount > 0 }">
          <span />
          状态映射
        </li>
        <li :class="{ ok: !!mainFlow.configured }">
          <span />
          主流程
        </li>
      </ul>
    </section>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  form: {
    type: Object,
    default: () => ({}),
  },
  noRulePreview: {
    type: Object,
    default: null,
  },
})

defineEmits(['configureFlow'])

const mainFlow = computed(() => props.form.mainFlowSummary || {})
const statusCount = computed(() => (props.form.statusMappingRows || []).filter(row => row.standardStatus && row.statusValue).length)
</script>

<style scoped>
.document-config-summary {
  display: grid;
  gap: 12px;
}

.document-config-summary section {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 14px;
}

.document-config-summary h4,
.flow-summary-head h4 {
  margin: 0;
  color: #111827;
  font-size: 15px;
}

.summary-facts {
  display: grid;
  gap: 10px;
  margin-top: 12px;
}

.summary-facts div {
  display: grid;
  gap: 3px;
}

.summary-facts span {
  color: #64748b;
  font-size: 12px;
}

.summary-facts strong,
.flow-summary strong {
  overflow: hidden;
  color: #111827;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.flow-summary-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.flow-summary {
  display: grid;
  gap: 8px;
  margin-top: 12px;
}

.flow-summary p {
  margin: 0;
  color: #b45309;
  font-size: 12px;
  line-height: 1.5;
}

.check-list {
  display: grid;
  gap: 8px;
  margin: 12px 0 0;
  padding: 0;
}

.check-list li {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #64748b;
  font-size: 13px;
  list-style: none;
}

.check-list span {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: #f59e0b;
}

.check-list li.ok {
  color: #166534;
}

.check-list li.ok span {
  background: #22c55e;
}
</style>
