<template>
  <div class="application-card-grid" role="list" aria-label="业务应用列表">
    <article
      v-for="application in applications"
      :key="application.id"
      class="application-card"
      role="listitem"
      tabindex="0"
      @click="emit('enter', application)"
      @keydown.enter.self.prevent="emit('enter', application)"
    >
      <header class="application-card-head">
        <span
          class="application-icon"
          :class="{
            'is-draft': isDraftApplication(application) && !application.lastPublishVersion,
            'is-changed': isUnpublishedApplication(application),
            'is-published': application.lastPublishVersion && !isUnpublishedApplication(application),
          }"
        >
          <IconRenderer v-if="application.icon" :icon="application.icon" :size="16" />
          <i v-else class="i-lucide:layout-dashboard" />
        </span>
        <span class="application-copy">
          <strong>{{ application.applicationName || application.applicationCode }}</strong>
          <code>{{ application.applicationCode }}</code>
        </span>
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

      <div class="application-meta-row">
        <span class="application-badge" :title="application.suiteCode">
          {{ application.suiteName || application.suiteCode || '未分组' }}
        </span>
        <span class="application-badge">{{ application.pageCount || 0 }} 页</span>
        <span
          class="application-version"
          :class="{ warning: isUnpublishedApplication(application), empty: !application.lastPublishVersion }"
        >
          <i class="i-lucide:git-branch" />
          <span v-if="isUnpublishedApplication(application)" class="problem-text">有变更未发布</span>
          <span v-else-if="application.lastPublishVersion">已发布 v{{ application.lastPublishVersion }}</span>
          <span v-else>尚未发布</span>
        </span>
      </div>

      <p v-if="application.description" class="application-description">
        {{ application.description }}
      </p>

      <footer class="application-card-foot">
        <span class="application-date">
          <i class="i-lucide:calendar" />
          {{ formatDate(application.updateTime) }}
        </span>

        <div class="application-actions" @click.stop>
          <button type="button" class="application-action-link" @click="emit('enter', application)">
            编辑
          </button>
          <span class="application-action-separator" />
          <button
            v-if="isDraftApplication(application)"
            type="button"
            class="application-action-link"
            @click="emit('publish', application)"
          >
            发布
          </button>
          <button
            v-else
            type="button"
            class="application-action-link"
            @click="emit('run', application)"
          >
            运行
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

const emit = defineEmits(['enter', 'run', 'edit', 'code', 'publish', 'toggle', 'delete'])

function actionOptions(application) {
  const isDraft = isDraftApplication(application)
  return [
    { label: isDraft ? '运行应用' : '发布应用', key: isDraft ? 'run' : 'publish' },
    { label: '预览与下载代码', key: 'code' },
    { label: '应用设置', key: 'edit' },
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
  grid-template-columns: repeat(auto-fill, minmax(min(100%, 300px), 1fr));
  align-content: start;
  gap: 14px;
  min-width: 0;
  min-height: 100%;
  padding: 14px;
}

.application-card {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 132px;
  overflow: hidden;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  outline: none;
  color: var(--n-text-color, var(--text-primary, #1d2129));
  background: #fff;
  cursor: pointer;
  transition:
    border-color 0.16s ease,
    box-shadow 0.16s ease,
    transform 0.16s ease;
}

.application-card:hover,
.application-card:focus-visible,
.application-card:focus-within {
  border-color: var(--n-primary-color, var(--primary-color, #165dff));
  box-shadow: 0 2px 6px rgb(22 93 255 / 10%);
  transform: translateY(-1px);
}

.application-card-head {
  display: grid;
  grid-template-columns: 36px minmax(0, 1fr) auto;
  align-items: start;
  gap: 10px;
  min-width: 0;
  padding: 14px 14px 10px;
}

.application-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: 0;
  border-radius: 4px;
  color: #5f6b7a;
  background: #f4f6f8;
  font-size: 16px;
}

.application-icon.is-draft {
  color: #b45309;
  background: #fff7e6;
}

.application-icon.is-changed {
  color: #2563eb;
  background: #eff6ff;
}

.application-icon.is-published {
  color: #15803d;
  background: #edf9f0;
}

.application-copy {
  display: grid;
  gap: 3px;
  min-width: 0;
  padding-top: 2px;
}

.application-copy strong,
.application-copy code,
.application-description,
.application-date {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.application-copy strong {
  color: var(--n-text-color, var(--text-primary, #1d2129));
  font-size: 14px;
  font-weight: 600;
  line-height: 16px;
}

.application-copy code {
  color: var(--n-text-color-3, var(--text-tertiary, #86909c));
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 11px;
  line-height: 14px;
}

.application-card-head :deep(.n-tag) {
  max-width: 84px;
  height: 20px;
  border-radius: 4px;
}

.application-design-status.is-draft :deep(.n-tag) {
  color: var(--warning-color, #d46b08);
  background-color: color-mix(in srgb, var(--warning-color, #ff7d00) 14%, transparent);
}

.application-meta-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  min-width: 0;
  padding: 0 14px 8px;
}

.application-badge {
  display: inline-flex;
  align-items: center;
  max-width: 120px;
  height: 20px;
  min-width: 0;
  overflow: hidden;
  border-radius: 4px;
  background: var(--n-color-embedded, var(--bg-secondary, #f2f3f5));
  color: var(--n-text-color-2, var(--text-secondary, #4e5969));
  font-size: 11px;
  line-height: 20px;
  padding: 0 6px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.application-version {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  min-width: 0;
  color: var(--n-text-color-3, var(--text-tertiary, #6b7280));
  font-size: 11px;
  margin-left: auto;
}

.application-version i {
  flex: 0 0 auto;
  font-size: 12px;
  color: var(--n-text-color-3, var(--text-tertiary, #9ca3af));
}

.application-version span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.application-version.empty {
  color: var(--n-text-color-3, var(--text-tertiary, #9ca3af));
}

.application-version.warning {
  color: var(--warning-color, #d46b08);
}

.application-description {
  display: -webkit-box;
  margin: 0;
  padding: 0 14px 8px;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  color: var(--n-text-color-3, var(--text-tertiary, #6b7280));
  font-size: 12px;
  line-height: 18px;
}

.application-date {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  min-width: 0;
  color: var(--n-text-color-3, var(--text-tertiary, #86909c));
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 11px;
  font-variant-numeric: tabular-nums;
}

.application-date i {
  flex: 0 0 auto;
  color: var(--n-text-color-3, var(--text-tertiary, #9ca3af));
  font-size: 12px;
}

.application-card-foot {
  display: none;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: auto;
  min-width: 0;
  border-top: 1px solid var(--n-primary-color, var(--primary-color, #165dff));
  background: var(--n-primary-color, var(--primary-color, #165dff));
  padding: 8px 14px;
}

.application-card:hover .application-card-foot,
.application-card:focus-visible .application-card-foot,
.application-card:focus-within .application-card-foot {
  display: flex;
}

.application-card:hover .application-date,
.application-card:focus-visible .application-date,
.application-card:focus-within .application-date,
.application-card:hover .application-date i,
.application-card:focus-visible .application-date i,
.application-card:focus-within .application-date i {
  color: rgb(255 255 255 / 86%);
}

.application-actions {
  display: flex;
  align-items: center;
  flex: 0 0 auto;
  justify-content: flex-end;
  gap: 7px;
  min-width: 0;
}

.problem-text {
  overflow: hidden;
  color: var(--warning-color, #d46b08);
  text-overflow: ellipsis;
}

.application-action-link,
.application-more-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 0;
  outline: none;
  background: transparent;
  cursor: pointer;
  transition: color 0.16s ease;
}

.application-action-link {
  height: 20px;
  color: #fff;
  font-size: 12px;
  font-weight: 500;
  line-height: 20px;
  padding: 0;
}

.application-action-link:hover,
.application-more-action:hover,
.application-action-link:focus-visible,
.application-more-action:focus-visible {
  color: rgb(255 255 255 / 76%);
}

.application-action-separator {
  width: 1px;
  height: 12px;
  background: rgb(255 255 255 / 32%);
}

.application-more-action {
  width: 18px;
  height: 20px;
  color: #fff;
  font-size: 14px;
  padding: 0;
}

:global(.dark) .application-icon {
  color: #aab2bf;
  background: #2a2d34;
}

:global(.dark) .application-icon.is-draft {
  color: #f0b45e;
  background: #3b3020;
}

:global(.dark) .application-icon.is-changed {
  color: #7aa7ff;
  background: #213251;
}

:global(.dark) .application-icon.is-published {
  color: #76cb91;
  background: #203a2a;
}

:global(.dark) .application-card {
  background: #1e1e22;
  border-color: #303540;
}

:global(.dark) .application-card:hover,
:global(.dark) .application-card:focus-visible,
:global(.dark) .application-card:focus-within {
  border-color: var(--n-primary-color, var(--primary-color, #4080ff));
  box-shadow: 0 2px 6px rgb(0 0 0 / 25%);
}

@media (max-width: 620px) {
  .application-card-grid {
    grid-template-columns: minmax(0, 1fr);
    padding: 10px;
  }

  .application-card {
    min-height: 136px;
  }
}
</style>
