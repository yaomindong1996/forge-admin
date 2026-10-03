<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import HomeActionIcon from './HomeActionIcon.vue'
import HomeSection from './HomeSection.vue'

const props = defineProps({
  todoCount: { type: Number, default: 0 },
  doneCount: { type: Number, default: 0 },
  startedCount: { type: Number, default: 0 },
  pendingStarted: { type: Number, default: 0 },
})
const router = useRouter()
const summaries = computed(() => [
  { title: '我的待办', count: props.todoCount, desc: '待签收 / 待处理', path: '/flow/todo', icon: 'time' },
  { title: '我的已办', count: props.doneCount, desc: '已处理任务', path: '/flow/done', icon: 'completed' },
  {
    title: '我发起的',
    count: props.startedCount,
    desc: `${props.pendingStarted} 个审批中`,
    path: '/flow/started',
    icon: 'started',
  },
])
const actions = [
  { title: '发起流程', path: '/flow/template', icon: 'create' },
  { title: '抄送我的', path: '/flow/cc', icon: 'cc' },
  { title: '流程监控', path: '/flow/monitor', icon: 'monitor' },
]
</script>

<template>
  <!-- 任务统计与流程操作分层，不再把文字动作当数字指标 -->
  <HomeSection title="审批中心" description="任务概览与流程操作" artwork="workflow">
    <template #action>
      <button type="button" @click="router.push('/flow/todo')">
        全部待办 <HomeActionIcon name="arrow" :size="14" />
      </button>
    </template>
    <div class="approval-summary-list">
      <button
        v-for="item in summaries" :key="item.path" type="button" class="approval-summary"
        @click="router.push(item.path)"
      >
        <span class="approval-summary-label">{{ item.title }} <HomeActionIcon :name="item.icon" :size="21" /></span>
        <span class="approval-summary-value"><strong>{{ item.count }}</strong><span>项</span></span>
        <span class="approval-summary-desc">{{ item.desc }}</span>
      </button>
    </div>
    <nav class="approval-action-list" aria-label="流程操作">
      <button
        v-for="item in actions" :key="item.path" type="button" class="approval-action"
        @click="router.push(item.path)"
      >
        <HomeActionIcon :name="item.icon" :size="20" />
        <span>{{ item.title }}</span>
        <HomeActionIcon name="arrow" :size="13" class="approval-action-arrow" />
      </button>
    </nav>
  </HomeSection>
</template>

<style scoped>
.approval-summary-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  padding: 12px 14px;
}
.approval-summary {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 6px;
  padding: 12px;
  border: 1px solid var(--home-border, #e5e7eb);
  border-radius: var(--home-radius-sm, 6px);
  background: var(--home-soft, #f8fafc);
  color: inherit;
  text-align: left;
  cursor: pointer;
  transition:
    background 160ms ease,
    border-color 160ms ease;
}
.approval-summary-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  font-size: 12px;
}
.approval-summary-label :deep(.home-action-icon) {
  color: var(--home-brand, #0e42d2);
}
.approval-summary-value {
  display: flex;
  align-items: baseline;
  gap: 5px;
}
.approval-summary-value strong {
  font-size: 24px;
  font-weight: 600;
  line-height: 28px;
  font-variant-numeric: tabular-nums;
}
.approval-summary-value > span,
.approval-summary-desc {
  color: var(--home-muted, #86909c);
  font-size: 11px;
}
.approval-summary-desc {
  overflow: hidden;
  line-height: 16px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.approval-summary:hover {
  border-color: var(--home-brand-border, #bfd0ff);
  background: var(--home-brand-soft, #f2f6ff);
}
.approval-action-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  padding: 0 14px 10px;
  gap: 10px;
}
.approval-action {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  min-height: 44px;
  padding: 8px 10px;
  border: 0;
  border-radius: var(--home-radius-sm, 6px);
  background: transparent;
  color: var(--home-text-secondary, #4e5969);
  font: inherit;
  font-size: 12px;
  cursor: pointer;
  transition:
    background 160ms ease,
    color 160ms ease;
}
.approval-action > :deep(.home-action-icon:first-child) {
  color: var(--home-brand, #0e42d2);
}
.approval-action-arrow {
  margin-left: auto;
  color: var(--home-muted, #86909c);
}
.approval-action:hover {
  background: var(--home-brand-soft, #f2f6ff);
  color: var(--home-brand, #0e42d2);
}
.approval-summary:focus-visible,
.approval-action:focus-visible {
  outline: 2px solid var(--home-brand, #0e42d2);
}
@media (max-width: 480px) {
  .approval-summary-list {
    gap: 8px;
    padding: 10px;
  }
  .approval-summary {
    gap: 4px;
    padding: 8px;
  }
  .approval-summary-label {
    flex-direction: column-reverse;
    align-items: flex-start;
  }
  .approval-summary-value strong {
    font-size: 22px;
  }
  .approval-action-list {
    gap: 8px;
    padding: 0 10px 10px;
  }
  .approval-action {
    flex-direction: column;
    gap: 5px;
    padding: 8px 4px;
  }
  .approval-action-arrow {
    display: none;
  }
}
</style>
