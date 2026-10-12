<template>
  <section class="workspace-pane">
    <n-spin :show="loading">
      <div v-if="selectedDomain" class="workspace-content">
        <div class="domain-hero">
          <div class="hero-main">
            <div class="domain-code">
              {{ selectedDomain.domainCode }}
            </div>
            <div class="domain-title-row">
              <h2>{{ selectedDomain.domainName }}</h2>
              <n-tag :type="selectedDomain.status === 'ENABLED' ? 'success' : 'warning'" :bordered="false">
                {{ selectedDomain.status === 'ENABLED' ? '启用' : '停用' }}
              </n-tag>
            </div>
            <p>{{ selectedDomain.domainDesc || schema.aiContext?.description || '该领域尚未维护业务说明。' }}</p>
          </div>
          <n-space>
            <n-button @click="$emit('editDomain', selectedDomain)">
              领域规则
            </n-button>
            <n-button type="primary" :disabled="selectedDomain.status !== 'ENABLED'" @click="$emit('createApp', selectedDomain)">
              新建模型
            </n-button>
          </n-space>
        </div>

        <div class="metric-grid">
          <div v-for="metric in metrics" :key="metric.label" class="metric-tile">
            <span>{{ metric.label }}</span>
            <strong>{{ metric.value }}</strong>
            <em>{{ metric.hint }}</em>
          </div>
        </div>

        <div class="workspace-grid">
          <div class="rule-board">
            <div class="board-head">
              <span>领域默认规则</span>
            </div>
            <div class="rule-lines">
              <div class="rule-line">
                <span>表名前缀</span>
                <code>{{ selectedDomain.tablePrefix || schema.naming?.tablePrefix || '-' }}</code>
              </div>
              <div class="rule-line">
                <span>配置键前缀</span>
                <code>{{ selectedDomain.configKeyPrefix || schema.naming?.configKeyPrefix || '-' }}</code>
              </div>
            </div>
          </div>

          <div class="rule-board">
            <div class="board-head">
              <span>AI 领域上下文</span>
            </div>
            <div class="chip-block">
              <n-tag
                v-for="term in aiTerms"
                :key="term"
                size="small"
                :bordered="false"
                type="info"
              >
                {{ term }}
              </n-tag>
              <span v-if="!aiTerms.length" class="empty-line">未维护术语</span>
            </div>
            <div class="constraint-list">
              <div v-for="item in constraints" :key="item" class="constraint-item">
                {{ item }}
              </div>
              <span v-if="!constraints.length" class="empty-line">未维护业务约束</span>
            </div>
          </div>
        </div>

        <div class="object-board">
          <div class="board-head">
            <span>业务对象概览</span>
            <small>{{ objects.length }} 个对象</small>
          </div>
          <div v-if="objects.length" class="object-list">
            <div v-for="object in objects" :key="object.configKey" class="object-row">
              <div>
                <strong>{{ object.objectName || object.appName || object.objectCode }}</strong>
                <span>{{ object.objectCode }} · {{ object.configKey }}</span>
              </div>
              <n-tag size="small" :type="object.publishStatus === 'PUBLISHED' ? 'success' : 'warning'" :bordered="false">
                {{ statusLabel(object.publishStatus) }}
              </n-tag>
            </div>
          </div>
          <n-empty v-else size="small" description="该领域暂无业务对象" />
        </div>
      </div>

      <div v-else class="overview-content">
        <div class="overview-hero">
          <div>
            <div class="domain-code">
              LOWCODE DOMAIN
            </div>
            <h2>领域总览</h2>
            <p>选择左侧业务领域后查看领域规则、对象概览和最近版本；当前展示全部低代码应用。</p>
          </div>
          <n-button type="primary" @click="$emit('createDomain')">
            新增领域
          </n-button>
        </div>
        <div class="metric-grid">
          <div class="metric-tile">
            <span>当前应用</span>
            <strong>{{ totalApps }}</strong>
            <em>当前筛选结果</em>
          </div>
          <div class="metric-tile">
            <span>已发布</span>
            <strong>{{ publishedApps }}</strong>
            <em>可打开运行时</em>
          </div>
          <div class="metric-tile">
            <span>草稿</span>
            <strong>{{ draftApps }}</strong>
            <em>待发布应用</em>
          </div>
        </div>
      </div>
    </n-spin>
  </section>
</template>

<script setup>
import { computed } from 'vue'

defineOptions({ name: 'DomainWorkspacePane' })

const props = defineProps({
  selectedDomain: {
    type: Object,
    default: null,
  },
  workspace: {
    type: Object,
    default: null,
  },
  apps: {
    type: Array,
    default: () => [],
  },
  loading: {
    type: Boolean,
    default: false,
  },
})

defineEmits(['createApp', 'editDomain', 'createDomain'])

const schema = computed(() => props.selectedDomain?.domainSchema || props.workspace?.domain?.domainSchema || {})
const objects = computed(() => props.workspace?.objects || [])
const aiTerms = computed(() => schema.value.aiContext?.terms || [])
const constraints = computed(() => schema.value.aiContext?.constraints || [])
const totalApps = computed(() => props.apps.length)
const publishedApps = computed(() => props.apps.filter(item => item.publishStatus === 'PUBLISHED').length)
const draftApps = computed(() => props.apps.filter(item => item.publishStatus !== 'PUBLISHED').length)
const metrics = computed(() => [
  { label: '模型数', value: props.workspace?.appCount || 0, hint: '领域内模型' },
  { label: '业务对象', value: props.workspace?.objectCount || 0, hint: '对象编码去重' },
  { label: '已发布', value: props.workspace?.publishedCount || 0, hint: '运行中页面' },
  { label: '草稿', value: props.workspace?.draftCount || 0, hint: '待发布' },
])

function statusLabel(status) {
  const map = {
    DRAFT: '草稿',
    PUBLISHED: '已发布',
    STOPPED: '已停用',
  }
  return map[status] || '草稿'
}
</script>

<style scoped>
.workspace-pane {
  min-width: 0;
}

.workspace-content,
.overview-content {
  display: grid;
  gap: 14px;
}

.domain-hero,
.overview-hero {
  display: flex;
  min-height: 132px;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: linear-gradient(135deg, rgba(15, 23, 42, 0.96), rgba(30, 41, 59, 0.92)), #0f172a;
  color: #f8fafc;
  padding: 18px;
}

.hero-main {
  min-width: 0;
}

.domain-code {
  color: #86efac;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  font-weight: 700;
}

.domain-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

h2 {
  margin: 6px 0 0;
  font-size: 22px;
  line-height: 1.25;
}

p {
  max-width: 720px;
  margin: 8px 0 0;
  color: #cbd5e1;
  font-size: 13px;
  line-height: 1.6;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.metric-tile {
  display: grid;
  gap: 3px;
  border: 1px solid #d8dee8;
  border-radius: 8px;
  background: var(--bg-primary);
  padding: 14px;
}

.metric-tile span {
  color: var(--text-secondary);
  font-size: 12px;
}

.metric-tile strong {
  color: var(--text-primary);
  font-size: 24px;
  line-height: 1.1;
}

.metric-tile em {
  color: var(--text-tertiary);
  font-size: 11px;
  font-style: normal;
}

.workspace-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 12px;
}

.rule-board,
.object-board {
  border: 1px solid #d8dee8;
  border-radius: 8px;
  background: var(--bg-primary);
  padding: 14px;
}

.board-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 700;
}

.board-head small {
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 400;
}

.rule-lines {
  display: grid;
  gap: 8px;
}

.rule-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  border-bottom: 1px solid #eef2f7;
  padding-bottom: 8px;
}

.rule-line span {
  color: var(--text-secondary);
  font-size: 12px;
}

code {
  overflow: hidden;
  color: var(--text-primary);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chip-block {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 10px;
}

.constraint-list {
  display: grid;
  gap: 6px;
}

.constraint-item {
  border-left: 3px solid #22c55e;
  background: var(--bg-secondary);
  color: var(--text-primary);
  font-size: 12px;
  line-height: 1.5;
  padding: 7px 9px;
}

.empty-line {
  color: var(--text-tertiary);
  font-size: 12px;
}

.object-list {
  display: grid;
  gap: 8px;
}

.object-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border: 1px solid #eef2f7;
  border-radius: 8px;
  padding: 10px;
}

.object-row div {
  display: grid;
  min-width: 0;
  gap: 3px;
}

.object-row strong {
  overflow: hidden;
  color: var(--text-primary);
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.object-row span {
  overflow: hidden;
  color: var(--text-secondary);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 1280px) {
  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .workspace-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .domain-hero,
  .overview-hero {
    flex-direction: column;
  }

  .metric-grid {
    grid-template-columns: 1fr;
  }
}
</style>
