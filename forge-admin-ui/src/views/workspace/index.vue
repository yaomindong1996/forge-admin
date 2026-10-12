<template>
  <div class="workspace-shell-page">
    <header class="workspace-shell-head">
      <div>
        <h1>我的工作台</h1>
        <p>集中处理待办、已办、发起事项和抄送消息。</p>
      </div>
      <BusinessTopNav active="workspace" />
    </header>

    <section class="workspace-shell-layout">
      <aside class="workspace-side">
        <button
          v-for="item in navItems"
          :key="item.key"
          class="workspace-side-item"
          :class="{ active: isActive(item), disabled: item.disabled }"
          type="button"
          :disabled="item.disabled"
          @click="openNav(item)"
        >
          <span class="workspace-side-icon">
            <i :class="item.icon" />
          </span>
          <span>
            <strong>{{ item.label }}</strong>
            <small>{{ item.desc }}</small>
          </span>
        </button>
      </aside>

      <main class="workspace-main">
        <RouterView />
      </main>
    </section>
  </div>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'
import BusinessTopNav from '@/components/business-top-nav/BusinessTopNav.vue'

const route = useRoute()
const router = useRouter()

const navItems = [
  {
    key: 'summary',
    label: '工作台首页',
    desc: '聚合今天要处理的事项',
    path: '/workspace/summary',
    icon: 'i-material-symbols:space-dashboard-outline',
  },
  {
    key: 'todo',
    label: '我的待办',
    desc: '等待我处理的任务',
    path: '/workspace/todo',
    icon: 'i-material-symbols:pending-actions',
  },
  {
    key: 'done',
    label: '我的已办',
    desc: '本周和历史处理记录',
    path: '/workspace/done',
    icon: 'i-material-symbols:task-alt-outline',
  },
  {
    key: 'started',
    label: '我发起的',
    desc: '查看单据流转进度',
    path: '/workspace/started',
    icon: 'i-material-symbols:send-outline',
  },
  {
    key: 'cc',
    label: '抄送我',
    desc: '需要知晓的流程消息',
    path: '/workspace/cc',
    icon: 'i-material-symbols:alternate-email',
  },
  {
    key: 'draft',
    label: '我的草稿',
    desc: '下一轮接入',
    path: '',
    icon: 'i-material-symbols:draft-outline',
    disabled: true,
  },
]

function isActive(item) {
  return item.path && route.path === item.path
}

function openNav(item) {
  if (!item.path || route.path === item.path)
    return
  router.push(item.path)
}
</script>

<style scoped>
.workspace-shell-page {
  min-height: 0;
  flex: 1;
  padding: 0;
  background: transparent;
}

.workspace-shell-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
  padding: 0 0 14px;
  border: 0;
  background: transparent;
}

.workspace-shell-head h1 {
  margin: 0;
  color: var(--text-primary);
  font-size: 22px;
  font-weight: 700;
  line-height: 30px;
}

.workspace-shell-head p {
  margin: 4px 0 0;
  color: #667085;
  font-size: 13px;
}

.workspace-shell-layout {
  display: grid;
  grid-template-columns: minmax(180px, 220px) minmax(0, 1fr);
  gap: 14px;
}

.workspace-side {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-self: start;
  padding: 0;
  border: 0;
  background: transparent;
}

.workspace-side-item {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
  width: 100%;
  min-height: 58px;
  padding: 8px;
  border: 1px solid transparent;
  border-radius: 8px;
  background: transparent;
  color: #344256;
  text-align: left;
  cursor: pointer;
}

.workspace-side-item:hover {
  background: var(--bg-tertiary);
}

.workspace-side-item.active {
  border-color: #c9cdd4;
  background: var(--bg-tertiary);
  color: var(--text-primary);
}

.workspace-side-item.disabled {
  cursor: not-allowed;
  opacity: 0.52;
}

.workspace-side-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 8px;
  background: var(--bg-tertiary);
  color: var(--text-secondary);
  font-size: 18px;
}

.workspace-side-item.active .workspace-side-icon {
  background: #e5e6eb;
  color: var(--text-primary);
}

.workspace-side-item strong,
.workspace-side-item small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.workspace-side-item strong {
  font-size: 14px;
  font-weight: 650;
}

.workspace-side-item small {
  margin-top: 2px;
  color: #667085;
  font-size: 12px;
}

.workspace-main {
  min-width: 0;
}

@media (max-width: 900px) {
  .workspace-shell-head {
    flex-direction: column;
  }

  .workspace-shell-layout {
    grid-template-columns: 1fr;
  }

  .workspace-side {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(145px, 1fr));
  }
}
</style>
