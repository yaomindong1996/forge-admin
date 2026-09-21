<template>
  <component
    :is="inline ? 'div' : NModal"
    v-bind="inline ? {} : {
      'show': show,
      'maskClosable': !busy,
      'closeOnEsc': !busy,
      'trapFocus': false,
      'autoFocus': false,
      'class': 'flow-task-detail-shell-modal',
      'onUpdate:show': (val) => emit('update:show', val),
    }"
    v-show="!inline || show"
    :class="inline ? 'flow-task-detail-shell inline-panel' : undefined"
  >
    <div class="flow-task-detail-shell" :class="{ fullscreen }">
      <header class="approval-topbar">
        <div class="approval-title-area">
          <div class="approval-status-mark" :class="statusClass">
            <i :class="statusIcon" />
          </div>
          <div class="approval-heading">
            <div class="approval-title-row">
              <span v-if="statusText" class="approval-state-text">{{ statusText }}</span>
              <span v-if="priorityText" class="approval-priority" :class="priorityClass">{{ priorityText }}</span>
              <h3>{{ title || '审批详情' }}</h3>
            </div>
            <div v-if="subtitle" class="approval-subtitle">
              {{ subtitle }}
            </div>
          </div>
        </div>
        <div class="approval-toolbar">
          <slot name="toolbar" />
          <button class="approval-close" :disabled="busy" aria-label="关闭详情" @click="emit('update:show', false)">
            <i class="i-material-symbols:close" />
          </button>
        </div>
      </header>

      <div class="approval-detail-layout">
        <main class="approval-main">
          <slot />
        </main>
        <aside class="approval-aside">
          <div class="approval-record-header">
            <div>
              <div class="approval-record-title">
                {{ recordTitle }}
              </div>
              <div class="approval-record-count">
                {{ records.length > 0 ? `${records.length} 条记录` : '暂无记录' }}
              </div>
            </div>
            <slot name="record-action" />
          </div>
          <div class="approval-records">
            <div v-if="records.length > 0" class="approval-record-list">
              <div
                v-for="(item, index) in records"
                :key="getRecordKey(item, index)"
                class="approval-record-item"
                :class="[getActionClass(item.action), { last: index === records.length - 1 }]"
              >
                <div class="approval-record-rail">
                  <div class="approval-record-icon">
                    <i :class="getActionIcon(item.action)" />
                  </div>
                </div>
                <div class="approval-record-content">
                  <div class="approval-record-line">
                    <div class="approval-record-main">
                      <div class="approval-record-stage">
                        <span class="approval-record-task">{{ item.taskName || getActionText(item.action) }}</span>
                        <span class="approval-record-action">{{ getActionText(item.action) }}</span>
                      </div>
                      <div class="approval-record-user">
                        <UserAvatar :name="getRecordUser(item)" :size="24" />
                        <span>{{ getRecordUser(item) }}</span>
                      </div>
                    </div>
                    <time class="approval-record-time">{{ formatRecordTime(item.completeTime || item.createTime) }}</time>
                  </div>
                  <div v-if="item.comment" class="approval-record-comment">
                    {{ item.comment }}
                  </div>
                  <div v-if="hasApprovalPointResults(item)" class="approval-record-points">
                    <div class="approval-record-points-title">
                      审批要点
                    </div>
                    <p
                      v-for="point in item.approvalPointResults"
                      :key="point.id || point.content"
                      class="approval-record-point"
                    >
                      {{ point.checked ? '已核' : '未核' }} · {{ point.content }}
                    </p>
                  </div>
                  <div v-if="item.signature" class="approval-record-signature">
                    <span>签名</span>
                    <SignatureImage :value="String(item.signature)" compact />
                  </div>
                </div>
              </div>
            </div>
            <n-empty v-else description="暂无审批记录" size="small" />
          </div>
          <slot name="aside-extra" />
        </aside>
      </div>
    </div>
  </component>
</template>

<script setup>
import { NModal } from 'naive-ui'
import UserAvatar from '@/components/common/UserAvatar.vue'
import SignatureImage from '@/components/flow/SignatureImage.vue'

defineProps({
  show: { type: Boolean, default: false },
  inline: { type: Boolean, default: false },
  title: { type: String, default: '' },
  subtitle: { type: String, default: '' },
  statusText: { type: String, default: '' },
  statusClass: { type: String, default: 'default' },
  statusIcon: { type: String, default: 'i-material-symbols:check-circle' },
  priorityText: { type: String, default: '' },
  priorityClass: { type: String, default: '' },
  recordTitle: { type: String, default: '审批记录' },
  records: { type: Array, default: () => [] },
  busy: { type: Boolean, default: false },
  fullscreen: { type: Boolean, default: true },
})

const emit = defineEmits(['update:show'])

function getRecordKey(item, index) {
  return item.id || item.historyId || `${item.taskId || item.taskName || 'record'}-${index}`
}

function hasApprovalPointResults(item) {
  return Array.isArray(item?.approvalPointResults) && item.approvalPointResults.length > 0
}

function normalizeAction(action) {
  return String(action || '').toLowerCase()
}

function getActionClass(action) {
  const value = normalizeAction(action)
  if (['approve', 'approved', 'pass', 'passed', 'start', 'completed'].includes(value))
    return 'success'
  if (['reject', 'rejected', 'terminate', 'terminated', 'cancel'].includes(value))
    return 'error'
  if (['return', 'returned', 'delegate', 'delegated', 'reassign', 'rollback', 'withdraw'].includes(value))
    return 'warning'
  if (['claim', 'pending'].includes(value))
    return 'info'
  return 'default'
}

function getActionIcon(action) {
  const icons = {
    approve: 'i-material-symbols:check',
    approved: 'i-material-symbols:check',
    pass: 'i-material-symbols:check',
    passed: 'i-material-symbols:check',
    start: 'i-material-symbols:add',
    reject: 'i-material-symbols:close',
    rejected: 'i-material-symbols:close',
    return: 'i-material-symbols:keyboard-return',
    returned: 'i-material-symbols:keyboard-return',
    delegate: 'i-material-symbols:arrow-forward',
    delegated: 'i-material-symbols:arrow-forward',
    reassign: 'i-material-symbols:person-edit',
    rollback: 'i-material-symbols:undo',
    terminate: 'i-material-symbols:stop',
    terminated: 'i-material-symbols:stop',
    withdraw: 'i-material-symbols:undo',
    claim: 'i-material-symbols:assignment-ind',
    pending: 'i-material-symbols:schedule',
  }
  return icons[normalizeAction(action)] || 'i-material-symbols:radio-button-unchecked'
}

function getActionText(action) {
  const texts = {
    approve: '已通过',
    approved: '已通过',
    pass: '已通过',
    passed: '已通过',
    start: '提交申请',
    reject: '已驳回',
    rejected: '已驳回',
    return: '已退回',
    returned: '已退回',
    delegate: '已转办',
    delegated: '已转办',
    reassign: '已转派',
    rollback: '已回退',
    terminate: '已终结',
    terminated: '已终结',
    withdraw: '已撤回',
    claim: '已签收',
    pending: '待处理',
  }
  return texts[normalizeAction(action)] || action || '处理中'
}

function getRecordUser(item) {
  return item.assigneeName || item.operatorName || item.userName || item.startUserName || '未知'
}

function formatRecordTime(time) {
  if (!time)
    return '-'
  const value = String(time)
  if (value.includes('T'))
    return value.replace('T', ' ').slice(0, 16)
  return value
}
</script>

<style scoped src="./FlowTaskDetailShell.css"></style>
