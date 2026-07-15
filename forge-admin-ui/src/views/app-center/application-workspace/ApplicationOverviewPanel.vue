<template>
  <div class="overview-panel">
    <header class="panel-heading">
      <div>
        <h2>应用概览</h2>
        <p>{{ application?.description || '这个应用尚未补充业务说明。' }}</p>
      </div>
      <span class="readiness-summary" :class="readinessTone">
        {{ readinessText }}
      </span>
    </header>

    <section class="overview-section">
      <div class="section-heading">
        <h3>配置进度</h3>
        <span>对象、入口和扩展共用工作台快照，切换后保留现场</span>
      </div>
      <div class="section-table">
        <button
          v-for="item in workspace?.sections || []"
          :key="item.sectionKey"
          type="button"
          class="section-row"
          @click="emit('navigate', item.sectionKey)"
        >
          <span class="status-dot" :class="`is-${String(item.status || '').toLowerCase()}`" />
          <strong>{{ item.sectionName }}</strong>
          <span>{{ item.assetCount || 0 }} 项资产</span>
          <span :class="{ problem: Number(item.problemCount || 0) > 0 }">
            {{ item.problemCount || 0 }} 项问题
          </span>
          <span class="row-action">打开</span>
        </button>
      </div>
    </section>

    <section class="overview-section">
      <div class="section-heading">
        <h3>待处理事项</h3>
        <span>{{ workspace?.issues?.length || 0 }} 项</span>
      </div>
      <n-empty
        v-if="!(workspace?.issues || []).length"
        size="small"
        description="当前没有阻断项或提醒"
        class="issues-empty"
      />
      <div v-else class="issue-list">
        <button
          v-for="issue in workspace.issues"
          :key="`${issue.issueCode}-${issue.message}`"
          type="button"
          class="issue-row"
          @click="emit('navigate', issue.sectionKey || 'overview')"
        >
          <span class="issue-level" :class="`is-${String(issue.level || '').toLowerCase()}`">
            {{ issue.level === 'BLOCK' ? '阻断' : '提醒' }}
          </span>
          <span class="issue-copy">
            <strong>{{ issue.title }}</strong>
            <small>{{ issue.message }}</small>
          </span>
          <span class="row-action">去处理</span>
        </button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  application: {
    type: Object,
    default: null,
  },
  workspace: {
    type: Object,
    default: null,
  },
})

const emit = defineEmits(['navigate'])

const readinessText = computed(() => {
  if (Number(props.workspace?.blockingCount || 0) > 0)
    return `${props.workspace.blockingCount} 项阻断，暂未就绪`
  if (Number(props.workspace?.warningCount || 0) > 0)
    return `可继续配置，${props.workspace.warningCount} 项提醒`
  return '当前配置已就绪'
})

const readinessTone = computed(() => {
  if (Number(props.workspace?.blockingCount || 0) > 0)
    return 'is-blocked'
  if (Number(props.workspace?.warningCount || 0) > 0)
    return 'is-warning'
  return 'is-ready'
})
</script>

<style scoped>
.overview-panel {
  display: grid;
  gap: 12px;
}

.panel-heading,
.section-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.panel-heading {
  padding: 0 2px 10px;
  border-bottom: 1px solid var(--border-light, #e5e6eb);
}

.panel-heading h2,
.section-heading h3 {
  margin: 0;
  color: var(--text-primary, #1d2129);
}

.panel-heading h2 {
  font-size: 16px;
}

.panel-heading p {
  max-width: 720px;
  margin: 3px 0 0;
  color: var(--text-tertiary, #86909c);
  line-height: 1.6;
}

.readiness-summary {
  padding: 4px 9px;
  border: 1px solid #b7dfc2;
  border-radius: 5px;
  color: #1a7f37;
  background: #dafbe1;
  font-size: 12px;
  white-space: nowrap;
}

.readiness-summary.is-warning {
  border-color: #eed888;
  color: #9a6700;
  background: #fff8c5;
}

.readiness-summary.is-blocked {
  border-color: #ffcecb;
  color: #cf222e;
  background: #ffebe9;
}

.overview-section {
  display: grid;
  gap: 6px;
}

.section-heading {
  align-items: center;
}

.section-heading h3 {
  font-size: 14px;
}

.section-heading span {
  color: var(--text-tertiary, #86909c);
  font-size: 12px;
}

.section-table,
.issue-list {
  overflow: hidden;
  border: 1px solid var(--border-default, #c9cdd4);
  border-radius: 7px;
}

.section-row,
.issue-row {
  display: grid;
  align-items: center;
  width: 100%;
  cursor: pointer;
  border: 0;
  border-bottom: 1px solid var(--border-light, #e5e6eb);
  color: var(--text-secondary, #4e5969);
  background: var(--bg-primary, #fff);
  text-align: left;
}

.section-row {
  grid-template-columns: 10px minmax(140px, 1fr) 110px 110px 52px;
  gap: 10px;
  min-height: 38px;
  padding: 5px 10px;
}

.section-row:last-child,
.issue-row:last-child {
  border-bottom: 0;
}

.section-row:hover,
.issue-row:hover {
  background: var(--bg-hover, #f2f3f5);
}

.section-row strong {
  color: var(--text-primary, #1d2129);
  font-size: 13px;
}

.status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #2da44e;
}

.status-dot.is-warning {
  background: #bf8700;
}

.status-dot.is-blocked {
  background: #cf222e;
}

.problem {
  color: #9a6700;
}

.row-action {
  color: var(--primary-color, #165dff);
  font-size: 12px;
  text-align: right;
}

.issue-row {
  grid-template-columns: 52px minmax(0, 1fr) 60px;
  gap: 10px;
  min-height: 48px;
  padding: 6px 10px;
}

.issue-level {
  width: fit-content;
  padding: 2px 6px;
  border-radius: 4px;
  color: #9a6700;
  background: #fff8c5;
  font-size: 11px;
}

.issue-level.is-block {
  color: #cf222e;
  background: #ffebe9;
}

.issue-copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 2px;
}

.issue-copy strong {
  color: var(--text-primary, #1d2129);
  font-size: 13px;
}

.issue-copy small {
  overflow: hidden;
  color: var(--text-tertiary, #86909c);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.issues-empty {
  padding: 16px;
  border: 1px solid var(--border-default, #c9cdd4);
  border-radius: 7px;
}
</style>
