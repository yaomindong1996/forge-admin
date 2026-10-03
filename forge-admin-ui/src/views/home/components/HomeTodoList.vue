<script setup>
import { NSkeleton, NSpin } from 'naive-ui'
import { useRouter } from 'vue-router'
import IllustratedEmpty from '@/components/common/IllustratedEmpty.vue'
import DictTag from '@/components/DictTag.vue'
import { formatHomeTaskInitiator, formatHomeTaskTime } from '../homeTaskPresentation'
import HomeActionIcon from './HomeActionIcon.vue'
import HomeSection from './HomeSection.vue'

defineProps({ tasks: { type: Array, default: () => [] }, loading: Boolean })
const emit = defineEmits(['open'])
const router = useRouter()
</script>

<template>
  <HomeSection title="待办任务" description="按任务查看审批节点与发起人" icon="task">
    <template #action>
      <button type="button" @click="router.push('/flow/todo')">
        全部待办 <HomeActionIcon name="arrow" :size="14" />
      </button>
    </template>
    <NSpin :show="loading">
      <!-- 初次加载不给“全部处理完成”的错误反馈 -->
      <div v-if="loading && !tasks.length" class="home-todo-loading" aria-label="正在加载待办任务">
        <NSkeleton v-for="index in 3" :key="index" height="52px" />
      </div>
      <IllustratedEmpty
        v-else-if="!tasks.length" artwork="workflow" description="暂无待办任务"
        class="home-todo-empty" compact
      />
      <div v-else class="home-todo-list">
        <!-- 原生整行按钮保证鼠标和键盘都能进入同一任务，避免嵌套按钮 -->
        <button
          v-for="task in tasks" :key="task.taskId || task.id" type="button" class="home-todo-row"
          :aria-label="`处理${task.title || task.processTitle || task.taskName || '审批任务'}`"
          @click="emit('open', task)"
        >
          <span class="home-task-icon"><HomeActionIcon name="task" :size="24" /></span>
          <span class="home-task-copy">
            <span class="home-task-title">
              <strong :title="task.title || task.processTitle || task.taskName">
                {{ task.title || task.processTitle || task.taskName || '-' }}
              </strong>
              <DictTag v-if="task.priority >= 2" dict-type="flow_priority" :value="task.priority" :bordered="false" />
            </span>
            <span class="home-task-meta">
              <span :title="`审批节点：${task.taskName || '-'}`">
                <HomeActionIcon name="workflow" :size="13" />
                <span class="home-meta-text">{{ task.taskName || '-' }}</span>
              </span>
              <span :title="`发起人：${formatHomeTaskInitiator(task)}`">
                <HomeActionIcon name="user" :size="13" />
                <span class="home-meta-text">
                  {{ formatHomeTaskInitiator(task) }}
                </span>
              </span>
              <span><HomeActionIcon name="time" :size="13" />{{ formatHomeTaskTime(task.createTime) }}</span>
            </span>
          </span>
          <span class="home-task-side">
            <!-- 与原首页保持相同的签收判断，展示文案由流程字典提供 -->
            <DictTag
              dict-type="flow_todo_status" :value="task.status === 0 && !task.assignee ? 0 : 1" :bordered="false"
            />
            <span class="home-task-action">处理<HomeActionIcon name="arrow" :size="14" /></span>
          </span>
        </button>
      </div>
    </NSpin>
  </HomeSection>
</template>

<style scoped>
.home-todo-list {
  padding: 0 14px 4px;
}
.home-todo-row {
  display: grid;
  grid-template-columns: 32px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  width: 100%;
  min-width: 0;
  min-height: 74px;
  padding: 12px 0;
  border: 0;
  border-bottom: 1px solid var(--home-border, #e5e7eb);
  background: transparent;
  color: inherit;
  font: inherit;
  text-align: left;
  cursor: pointer;
  transition: background 160ms ease;
}
.home-todo-row:last-child {
  border-bottom: 0;
}
.home-task-icon {
  display: flex;
  justify-content: center;
  color: var(--home-brand, #0e42d2);
}
.home-task-copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 8px;
}
.home-task-title {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.home-task-title strong {
  min-width: 0;
  overflow: hidden;
  color: var(--home-text, #1f2329);
  font-size: 13px;
  line-height: 20px;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.home-task-title :deep(.n-tag) {
  flex-shrink: 0;
}
.home-task-meta {
  display: flex;
  min-width: 0;
  flex-wrap: wrap;
  gap: 6px 12px;
  color: var(--home-muted, #86909c);
  font-size: 11px;
  line-height: 16px;
  overflow-wrap: anywhere;
}
.home-task-meta > span {
  display: inline-flex;
  min-width: 0;
  max-width: 100%;
  align-items: center;
  gap: 4px;
}
.home-meta-text {
  min-width: 0;
  overflow-wrap: anywhere;
}
.home-task-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}
.home-task-action {
  display: flex;
  align-items: center;
  gap: 2px;
  color: var(--home-brand, #0e42d2);
  font-size: 12px;
}
.home-todo-row:hover {
  background: var(--home-soft, #f8fafc);
}
.home-todo-row:hover strong {
  color: var(--home-brand, #0e42d2);
}
.home-todo-row:focus-visible {
  outline: 2px solid var(--home-brand, #0e42d2);
  outline-offset: -2px;
}
.home-todo-empty {
  padding: 24px 14px;
  min-height: 180px;
}
.home-todo-loading {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px 14px;
}
@media (max-width: 480px) {
  .home-todo-list {
    padding: 0 10px 4px;
  }
  .home-todo-row {
    grid-template-columns: 26px minmax(0, 1fr) auto;
    gap: 8px;
  }
  .home-task-meta {
    gap: 4px 8px;
  }
  .home-task-title strong {
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
    white-space: normal;
  }
}
</style>
