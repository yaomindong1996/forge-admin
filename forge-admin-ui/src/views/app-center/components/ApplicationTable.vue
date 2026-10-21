<template>
  <div class="application-card-grid" role="list" aria-label="业务应用列表">
    <article
      v-for="application in applications"
      :key="application.id"
      class="application-card"
      :style="applicationTone(application)"
      role="listitem"
      tabindex="0"
      @click="emit('enter', application)"
      @keydown.enter.self.prevent="emit('enter', application)"
    >
      <!-- 身份区：图标 + 名称编码 + 设计状态 -->
      <header class="application-card-head">
        <div class="application-identity">
          <span class="application-icon" aria-hidden="true">
            <IconRenderer v-if="application.icon" :icon="application.icon" :size="18" />
            <i v-else class="i-lucide:app-window" />
          </span>
          <span class="application-copy">
            <strong>{{ application.applicationName || application.applicationCode }}</strong>
            <code>{{ application.applicationCode }}</code>
          </span>
        </div>
        <span
          class="application-design-status"
          :class="{ 'is-draft': isDraftApplication(application) }"
        >
          <DictTag
            dict-type="ai_business_application_design_status"
            :value="application.designStatus"
            :bordered="false"
            force-tag
          />
        </span>
      </header>

      <!-- 标签 + 发布态同一行，避免底栏挤挡 -->
      <div class="application-card-body">
        <div class="application-meta-row">
          <span class="application-badge" :title="application.suiteCode">
            {{ application.suiteName || application.suiteCode || '未分组' }}
          </span>
          <span class="application-badge">{{ application.pageCount || 0 }} 页</span>
          <span
            class="application-badge application-badge--version"
            :class="{
              'is-warning': isUnpublishedApplication(application),
              'is-empty': !application.lastPublishVersion,
            }"
          >
            <template v-if="isUnpublishedApplication(application)">有变更未发布</template>
            <template v-else-if="application.lastPublishVersion">已发布 v{{ application.lastPublishVersion }}</template>
            <template v-else>尚未发布</template>
          </span>
        </div>
        <p class="application-description">
          {{ application.description || '暂无应用说明' }}
        </p>
      </div>

      <!-- 底栏单行：日期 + 操作 -->
      <footer class="application-card-foot">
        <span class="application-date">
          <i class="i-lucide:calendar" />
          {{ formatDate(application.updateTime) }}
        </span>
        <div class="application-actions" @click.stop>
          <button type="button" class="application-action-link is-primary" @click="emit('enter', application)">
            <i class="i-lucide:pen-line" />编辑
          </button>
          <span class="application-action-separator" />
          <button
            v-if="isDraftApplication(application)"
            type="button"
            class="application-action-link"
            @click="emit('publish', application)"
          >
            <i class="i-lucide:upload" />发布
          </button>
          <button
            v-else
            type="button"
            class="application-action-link"
            @click="emit('run', application)"
          >
            <i class="i-lucide:play" />运行
          </button>
          <span class="application-action-separator" />
          <n-dropdown
            trigger="click"
            :options="actionOptions(application)"
            @select="key => handleAction(key, application)"
          >
            <button type="button" class="application-more-action" aria-label="更多应用操作">
              <i class="i-lucide:more-horizontal" />
            </button>
          </n-dropdown>
        </div>
      </footer>
    </article>
  </div>
</template>

<script setup>
import DictTag from '@/components/DictTag.vue'
import IconRenderer from '@/components/IconRenderer.vue'

defineProps({
  applications: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['enter', 'run', 'edit', 'code', 'publish', 'exportConfig', 'toggle', 'delete'])

const APP_TONES = [
  { ink: '#2563eb', wash: '#dbeafe' },
  { ink: '#7c3aed', wash: '#ede9fe' },
  { ink: '#059669', wash: '#d1fae5' },
  { ink: '#ea580c', wash: '#ffedd5' },
  { ink: '#0891b2', wash: '#cffafe' },
  { ink: '#db2777', wash: '#fce7f3' },
]

/** 按应用编码稳定映射色相，对齐应用市场卡片。 */
function applicationTone(application) {
  const key = String(application?.applicationCode || application?.id || '')
  let hash = 0
  for (let i = 0; i < key.length; i += 1)
    hash = (hash + key.charCodeAt(i) * (i + 1)) % APP_TONES.length
  const tone = APP_TONES[hash] || APP_TONES[0]
  return { '--app-ink': tone.ink, '--app-wash': tone.wash }
}

function actionOptions(application) {
  const isDraft = isDraftApplication(application)
  return [
    { label: isDraft ? '运行应用' : '发布应用', key: isDraft ? 'run' : 'publish' },
    { label: '预览与下载代码', key: 'code' },
    { label: '应用设置', key: 'edit' },
    { label: '导出应用配置', key: 'export-config' },
    { label: Number(application.status) === 1 ? '停用应用' : '启用应用', key: 'toggle' },
    { type: 'divider', key: 'divider' },
    { label: '删除应用', key: 'delete' },
  ]
}

function isDraftApplication(application) {
  const status = String(application?.designStatus || '').toUpperCase()
  return !application?.lastPublishVersion || ['DRAFT', 'READY', 'CHANGED'].includes(status)
}

function isUnpublishedApplication(application) {
  const status = String(application?.designStatus || '').toUpperCase()
  return Boolean(application?.lastPublishVersion) && ['DRAFT', 'READY', 'CHANGED'].includes(status)
}

function handleAction(key, application) {
  if (key === 'enter')
    emit('enter', application)
  else if (key === 'run')
    emit('run', application)
  else if (key === 'publish')
    emit('publish', application)
  else if (key === 'code')
    emit('code', application)
  else if (key === 'edit')
    emit('edit', application)
  else if (key === 'export-config')
    emit('exportConfig', application)
  else if (key === 'toggle')
    emit('toggle', application)
  else if (key === 'delete')
    emit('delete', application)
}

function formatDate(value) {
  if (!value)
    return '-'
  const date = new Date(String(value).replace(' ', 'T'))
  if (Number.isNaN(date.getTime()))
    return String(value)
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(date)
}
</script>

<style scoped>
.application-card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(100%, 280px), 1fr));
  align-content: start;
  gap: 12px;
  width: 100%;
  min-width: 0;
  min-height: 100%;
  padding: 8px 16px 16px;
  box-sizing: border-box;
}

.application-card {
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  outline: none;
  background: #fff;
  box-shadow: 0 2px 8px rgb(15 23 42 / 3%);
  cursor: pointer;
  transition:
    transform 0.16s ease,
    box-shadow 0.16s ease;
}

.application-card:hover,
.application-card:focus-visible,
.application-card:focus-within {
  box-shadow: 0 8px 18px rgb(15 23 42 / 8%);
  transform: translateY(-1px);
}

.application-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  padding: 14px 14px 8px;
}

.application-identity {
  display: flex;
  gap: 10px;
  min-width: 0;
}

.application-icon {
  display: grid;
  place-items: center;
  flex: none;
  width: 40px;
  height: 40px;
  border-radius: 4px;
  background: linear-gradient(
    145deg,
    color-mix(in srgb, var(--app-wash) 40%, #fff),
    var(--app-wash)
  );
  color: var(--app-ink);
  font-size: 18px;
  box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--app-ink) 12%, transparent);
}

.application-copy {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.application-copy strong,
.application-copy code {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.application-copy strong {
  color: #0f172a;
  font-size: 15px;
  font-weight: 650;
  line-height: 1.35;
}

.application-copy code {
  color: #94a3b8;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 11px;
  line-height: 1.3;
}

.application-design-status {
  flex: none;
}

.application-card-head :deep(.n-tag) {
  height: 20px;
  padding: 0 7px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
  line-height: 20px;
}

.application-design-status.is-draft :deep(.n-tag) {
  color: #c2410c;
  background-color: #ffedd5;
}

.application-card-body {
  display: grid;
  gap: 8px;
  flex: 1;
  padding: 0 14px 12px;
}

.application-meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  min-width: 0;
}

.application-badge {
  display: inline-flex;
  align-items: center;
  max-width: 140px;
  height: 20px;
  min-width: 0;
  overflow: hidden;
  padding: 0 7px;
  border-radius: 4px;
  background: color-mix(in srgb, var(--app-wash) 70%, #fff);
  color: var(--app-ink);
  font-size: 11px;
  font-weight: 500;
  line-height: 20px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.application-badge--version {
  max-width: 160px;
  background: #f1f5f9;
  color: #64748b;
}

.application-badge--version.is-warning {
  background: #ffedd5;
  color: #c2410c;
}

.application-badge--version.is-empty {
  background: #f1f5f9;
  color: #94a3b8;
}

.application-description {
  display: -webkit-box;
  margin: 0;
  overflow: hidden;
  color: #64748b;
  font-size: 12px;
  line-height: 1.55;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.application-card-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  flex-wrap: nowrap;
  margin-top: auto;
  min-height: 36px;
  min-width: 0;
  padding: 8px 14px;
  border-top: 1px solid #f1f5f9;
  background: #f8fafc;
}

.application-date {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  min-width: 0;
  overflow: hidden;
  color: #94a3b8;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.application-date i {
  flex: none;
  font-size: 12px;
}

.application-actions {
  display: flex;
  align-items: center;
  flex: none;
  flex-wrap: nowrap;
  gap: 6px;
  white-space: nowrap;
}

.application-action-link,
.application-more-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  margin: 0;
  padding: 0 2px;
  border: 0;
  outline: none;
  background: transparent;
  color: #2563eb;
  font: inherit;
  font-size: 12px;
  line-height: 20px;
  white-space: nowrap;
  cursor: pointer;
}

.application-action-link.is-primary {
  font-weight: 650;
}

.application-action-link:hover,
.application-more-action:hover,
.application-action-link:focus-visible,
.application-more-action:focus-visible {
  color: #1d4ed8;
}

.application-action-separator {
  width: 1px;
  height: 12px;
  background: #e2e8f0;
}

.application-more-action {
  width: 18px;
  height: 20px;
  color: #94a3b8;
  font-size: 14px;
  padding: 0;
}

@media (prefers-reduced-motion: reduce) {
  .application-card {
    transition: none;
  }

  .application-card:hover,
  .application-card:focus-visible,
  .application-card:focus-within {
    transform: none;
  }
}

@media (max-width: 620px) {
  .application-card-grid {
    grid-template-columns: minmax(0, 1fr);
    padding: 8px 12px 12px;
  }
}
</style>
