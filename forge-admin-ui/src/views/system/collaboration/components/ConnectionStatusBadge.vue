<template>
  <span class="connection-status" :class="`connection-status--${status.tone}`">
    <span class="connection-status__dot" aria-hidden="true" />
    {{ status.label }}
  </span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  row: {
    type: Object,
    default: () => ({}),
  },
})

const status = computed(() => {
  const row = props.row || {}
  if (Number(row.status) === 0) {
    return { label: '已停用', tone: 'muted' }
  }
  if (!String(row.connectionName || '').trim() || !String(row.platform || '').trim()) {
    return { label: '待完善', tone: 'warning' }
  }
  if (['WECHAT_ENTERPRISE', 'DINGTALK', 'DINGTALK_ACCOUNT', 'FEISHU'].includes(row.platform)
    && !String(row.enterpriseId || '').trim()) {
    return { label: '缺少企业标识', tone: 'warning' }
  }
  return { label: '基础信息完整', tone: 'success' }
})
</script>

<style scoped>
.connection-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  white-space: nowrap;
  font-size: 12px;
  line-height: 20px;
}

.connection-status__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

.connection-status--success {
  color: #16845b;
}

.connection-status--warning {
  color: #b7791f;
}

.connection-status--muted {
  color: #8b95a5;
}
</style>
